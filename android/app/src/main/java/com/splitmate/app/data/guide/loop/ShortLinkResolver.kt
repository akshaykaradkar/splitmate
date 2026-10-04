package com.splitmate.app.data.guide.loop

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.GuideFeatureFlags
import com.splitmate.app.data.guide.StayLocationResolver
import com.splitmate.app.data.guide.StayResolution
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Resolves a pasted/shared Google Maps link (or plain pasted coordinates) into a stay location.
 *
 * Policy (audit §2.3 / §4.1, decisions #12 and #13; hardened in v2.3.5 #3):
 * - **Offline first:** the text is always run through [MapLinkCoordinateParser]. An exact
 *   coordinate found locally is returned without any network call. Only shorteners
 *   (`maps.app.goo.gl`, `goo.gl`, `g.co`), legacy `maps.google.<tld>` links and
 *   `google.<tld>/maps?cid=` links trigger network probing. Other full `www.google.<tld>/maps/…`
 *   URLs are parsed offline only.
 * - **Host allow-list** for any URL in the input: `maps.app.goo.gl`, `goo.gl/maps…`,
 *   `maps.google.<tld>`, `(www.)google.<tld>/maps…`, `g.co/kgs…`. Any other host is Rejected,
 *   even when the URL happens to contain numbers.
 * - **HTTPS only.** `http://` input or an `http://` redirect target is Rejected.
 * - **Manual redirects (v2.3.5):** GET first with a mobile-browser identity (HEAD only when GET
 *   is refused with 405/501), at most [MAX_REDIRECTS] redirects, one retry with a short backoff
 *   on I/O errors/timeouts (from the second hop on only while no coordinate or place-name hint is
 *   known). When a shortener answers a browser with a page instead of a redirect,
 *   it is asked once more with the app identity.
 * - **consent.google.\*** interstitials are not fetched. Their `continue=` parameter is decoded
 *   and followed instead. **google.\*\/sorry** (rate-limit) pages are not fetched either: their
 *   `continue=` URL is parsed offline and the chain stops there.
 * - **HTML fallback (v2.3.5, approved):** when no redirect URL carries a coordinate and
 *   [GuideFeatureFlags.shortLinkBodyParseEnabled] is on, up to 256 KB of the final Google page is
 *   scanned by [MapsPageCoordinateExtractor] (patterns only).
 * - Every redirect target must stay on a Google host (allow-list plus `consent.` / `www.google.`).
 * - **Kill-switch:** when [GuideFeatureFlags.shortLinkParsingEnabled] is false, only the pasted
 *   text is parsed (no network). A coordinate-less Google link then yields
 *   [StayResolution.NoCoordinates] so the UI can offer the manual/Nominatim fallbacks.
 *
 * Results:
 * - [StayResolution.Resolved]: EXACT for pin/query/path/geo/DMS/decimal rungs, APPROXIMATE for a
 *   viewport-only (`@lat,lng,z`) coordinate.
 * - [StayResolution.NoCoordinates]: valid Google link with no coordinate (e.g. `q=<name>&ftid=`).
 *   Carries the final URL and a place-name hint for the (automatic) Nominatim fallback.
 * - [StayResolution.Rejected]: unsupported host, insecure link, too many redirects, dead link,
 *   Google refusing the request, or a network failure. Network failures are classified
 *   ([classifyFailure]) so "offline" is only claimed when the device really is offline.
 */
class ShortLinkResolver(
    private val fetcher: RedirectFetcher = OkHttpRedirectFetcher(),
    /** Device connectivity: true/false when known, null when unknown (JVM tests). */
    private val isOnline: () -> Boolean? = { null },
    /** Backoff sleeper between the first attempt and the single retry (injectable for tests). */
    private val sleeper: suspend (Long) -> Unit = { delay(it) },
    private val flags: () -> GuideFeatureFlags = { GuideFeatureFlags() }
) : StayLocationResolver {

    override suspend fun resolve(input: String): StayResolution {
        val text = input.trim().let {
            if (it.length > MapLinkCoordinateParser.MAX_INPUT_CHARS) it.take(MapLinkCoordinateParser.MAX_INPUT_CHARS) else it
        }
        if (text.isEmpty()) return StayResolution.Rejected(REASON_EMPTY)

        val url = findFirstUrl(text)
        if (url != null) {
            val parsedUrl = SimpleUrl.parse(url) ?: return StayResolution.Rejected(REASON_UNSUPPORTED_HOST)
            if (parsedUrl.scheme != "https") return StayResolution.Rejected(REASON_INSECURE)
            if (!isAllowedInputUrl(parsedUrl)) return StayResolution.Rejected(REASON_UNSUPPORTED_HOST)
        }

        val local = MapLinkCoordinateParser.parse(text)
        val localCoord = local.coordinate
        if (localCoord != null && localCoord.precision == CoordinatePrecision.EXACT) {
            return StayResolution.Resolved(localCoord.location, local.placeNameHint, CoordinatePrecision.EXACT)
        }

        if (url == null) {
            return if (localCoord != null) {
                StayResolution.Resolved(localCoord.location, local.placeNameHint, localCoord.precision)
            } else {
                StayResolution.Rejected(REASON_NOTHING_FOUND)
            }
        }

        if (!flags().shortLinkParsingEnabled || !needsNetwork(SimpleUrl.parse(url))) {
            return if (localCoord != null) {
                StayResolution.Resolved(localCoord.location, local.placeNameHint, localCoord.precision)
            } else {
                StayResolution.NoCoordinates(url, local.placeNameHint)
            }
        }

        var lastApprox: ParsedCoordinate? = localCoord
        var lastHint: String? = local.placeNameHint
        var lastUrl: String = url

        return try {
            withTimeoutOrNull(OVERALL_TIMEOUT_MS) {
                follow(
                    startUrl = url,
                    initialApprox = localCoord,
                    initialHint = local.placeNameHint,
                    onProgress = { curUrl, curApprox, curHint ->
                        lastUrl = curUrl
                        if (curApprox != null) lastApprox = curApprox
                        if (curHint != null) lastHint = curHint
                    }
                )
            } ?: when {
                lastApprox != null -> StayResolution.Resolved(lastApprox!!.location, lastHint, lastApprox!!.precision)
                lastHint != null -> StayResolution.NoCoordinates(lastUrl, lastHint)
                else -> StayResolution.Rejected(if (isOnline() == false) REASON_OFFLINE else REASON_TIMEOUT)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            when {
                lastApprox != null -> StayResolution.Resolved(lastApprox!!.location, lastHint, lastApprox!!.precision)
                lastHint != null && isOnline() != false -> StayResolution.NoCoordinates(lastUrl, lastHint)
                else -> StayResolution.Rejected(classifyFailure(e, isOnline()))
            }
        }
    }

    private suspend fun follow(
        startUrl: String,
        initialApprox: ParsedCoordinate?,
        initialHint: String?,
        onProgress: (String, ParsedCoordinate?, String?) -> Unit = { _, _, _ -> }
    ): StayResolution {
        var current = startUrl
        var approx = initialApprox
        // URL-derived names (/place/<name>, q=<text>) beat free share-text prefixes.
        var urlHint: String? = null
        var redirects = 0
        var unwraps = 0
        // Network hops performed so far (the retry policy differs for the first hop).
        var fetches = 0
        // Set after a google.*/sorry unwrap: parse the continue URL offline, then stop.
        var stopAfterParse = false
        val bodyParse = flags().shortLinkBodyParseEnabled

        while (true) {
            val hop = MapLinkCoordinateParser.parse(current)
            // consent/sorry URLs: their own `q=` is an opaque token, never a place name.
            val interstitial = SimpleUrl.parse(current)?.let { CONSENT_HOST.matches(it.host) || isSorryPage(it) } == true
            if (urlHint == null && !interstitial) urlHint = hop.placeNameHint
            val hint = urlHint ?: initialHint
            hop.coordinate?.let { c ->
                if (c.precision == CoordinatePrecision.EXACT) {
                    return StayResolution.Resolved(c.location, hint, CoordinatePrecision.EXACT)
                }
                if (approx == null) approx = c
            }
            onProgress(current, approx, hint)
            if (stopAfterParse) return refused(approx, hint, current)

            val parsed = SimpleUrl.parse(current) ?: return StayResolution.Rejected(REASON_INVALID_REDIRECT)

            val consent = CONSENT_HOST.matches(parsed.host)
            val sorry = isSorryPage(parsed)
            if (consent || sorry) {
                if (++unwraps > MAX_REDIRECTS) return StayResolution.Rejected(REASON_TOO_MANY_REDIRECTS)
                val cont = parsed.rawQueryParam("continue")
                if (cont == null) {
                    return if (sorry) refused(approx, hint, current) else finish(approx, hint, current, 200, redirects)
                }
                val next = LoopText.percentDecode(cont, plusAsSpace = false)
                checkHop(next)?.let { return it }
                current = next
                // A rate-limit page would just come back if the continue URL were fetched again.
                if (sorry) stopAfterParse = true
                continue
            }

            // v2.3.5 (#3): the first hop always gets its single retry. From the second hop on, a
            // retry (backoff + another full timeout) only runs when nothing usable is known yet;
            // with a coordinate or place-name hint in hand we fail fast and use the hint instead.
            val allowRetry = fetches == 0 || (approx == null && hint == null)
            fetches++
            val probe = try {
                request(current, parsed, bodyParse, allowRetry)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                val resolvedApprox = approx
                val resolvedHint = hint
                if (resolvedApprox != null) {
                    return StayResolution.Resolved(resolvedApprox.location, resolvedHint, resolvedApprox.precision)
                }
                if (resolvedHint != null && isOnline() != false) {
                    return StayResolution.NoCoordinates(current, resolvedHint)
                }
                throw e
            }

            val location = probe.location
            if (probe.statusCode in 300..399 && !location.isNullOrBlank()) {
                if (redirects >= MAX_REDIRECTS) return StayResolution.Rejected(REASON_TOO_MANY_REDIRECTS)
                val next = resolveLocation(parsed, location.trim())
                    ?: return StayResolution.Rejected(REASON_INVALID_REDIRECT)
                checkHop(next)?.let { return it }
                redirects++
                current = next
                continue
            }

            // Terminal response. Approved HTML fallback (flag-gated, 2xx only, size-capped).
            val body = probe.body
            if (bodyParse && probe.statusCode in 200..299 && body != null) {
                val page = MapsPageCoordinateExtractor.extract(body)
                val pageHint = hint ?: page.placeName
                val c = page.coordinate
                if (c != null && (c.precision == CoordinatePrecision.EXACT || approx == null)) {
                    return StayResolution.Resolved(c.location, pageHint, c.precision)
                }
                return finish(approx, pageHint, current, probe.statusCode, redirects)
            }
            return finish(approx, hint, current, probe.statusCode, redirects)
        }
    }

    /**
     * One hop: GET first (browser identity, optional capped body), HEAD only if GET is refused
     * with 405/501, and, for shorteners that answered with a page instead of a redirect, one
     * more GET with the app identity (which Google answers with a plain 30x).
     * [allowRetry] = whether an I/O failure may be retried once (see [follow]).
     */
    private suspend fun request(url: String, parsed: SimpleUrl, bodyParse: Boolean, allowRetry: Boolean = true): RedirectProbe {
        val maxBody = if (bodyParse) MapsPageCoordinateExtractor.MAX_BODY_BYTES else 0
        var probe = withRetry(allowRetry) { fetcher.fetch(url, ProbeMethod.GET, FetchIdentity.BROWSER_MOBILE, maxBody) }
        if (probe.statusCode in GET_REFUSED) {
            probe = withRetry(allowRetry) { fetcher.fetch(url, ProbeMethod.HEAD, FetchIdentity.BROWSER_MOBILE, 0) }
        }
        if (isShortener(parsed) && probe.statusCode !in 300..399 && probe.statusCode !in GONE) {
            val alt = withRetry(allowRetry) { fetcher.fetch(url, ProbeMethod.GET, FetchIdentity.APP, maxBody) }
            if (alt.statusCode in 300..399 && !alt.location.isNullOrBlank()) return alt
        }
        return probe
    }

    /**
     * Runs [block]; on an I/O error or timeout waits [RETRY_BACKOFF_MS] and tries exactly once
     * more, unless [allowRetry] is false or the device is known to be offline.
     */
    private suspend fun withRetry(allowRetry: Boolean, block: suspend () -> RedirectProbe): RedirectProbe =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            if (!allowRetry || isOnline() == false) throw e
            sleeper(RETRY_BACKOFF_MS)
            block()
        }

    private fun finish(
        approx: ParsedCoordinate?,
        hint: String?,
        finalUrl: String,
        terminalStatus: Int,
        redirects: Int
    ): StayResolution {
        if (approx != null) return StayResolution.Resolved(approx.location, hint, approx.precision)
        if (terminalStatus == 429 || terminalStatus == 403) return refused(null, hint, finalUrl)
        if (terminalStatus in 400..499 && redirects == 0) return StayResolution.Rejected(REASON_DEAD_LINK)
        if (terminalStatus >= 500 && hint == null) return StayResolution.Rejected(REASON_GOOGLE_UNAVAILABLE)
        return StayResolution.NoCoordinates(finalUrl, hint)
    }

    /**
     * Google refused/rate-limited us. A known coordinate still wins; a known place name becomes
     * NoCoordinates so the UI can run the automatic name search; otherwise an accurate refusal.
     */
    private fun refused(approx: ParsedCoordinate?, hint: String?, finalUrl: String): StayResolution = when {
        approx != null -> StayResolution.Resolved(approx.location, hint, approx.precision)
        hint != null -> StayResolution.NoCoordinates(finalUrl, hint)
        else -> StayResolution.Rejected(REASON_GOOGLE_REFUSED)
    }

    /** Returns a rejection when [next] is not an acceptable redirect target, else null. */
    private fun checkHop(next: String): StayResolution.Rejected? {
        val u = SimpleUrl.parse(next) ?: return StayResolution.Rejected(REASON_INVALID_REDIRECT)
        if (u.scheme != "https") return StayResolution.Rejected(REASON_INSECURE)
        if (!isAllowedHopUrl(u)) return StayResolution.Rejected(REASON_LEFT_GOOGLE)
        return null
    }

    companion object {
        /** Maximum number of HTTP redirects followed (audit §4.1; raised to 8 in v2.3.5). */
        const val MAX_REDIRECTS = 8

        /** Wait before the single retry of a failed hop. */
        const val RETRY_BACKOFF_MS = 600L

        /** Upper bound for the whole chain (hops x retries) so the sheet never spins forever. */
        const val OVERALL_TIMEOUT_MS = 45_000L

        const val REASON_EMPTY = "Paste a Google Maps link or coordinates."
        const val REASON_NOTHING_FOUND = "No location found in the pasted text."
        const val REASON_UNSUPPORTED_HOST = "Only Google Maps links are supported."
        const val REASON_INSECURE = "Only secure (https) links are supported."
        const val REASON_TOO_MANY_REDIRECTS = "This link redirects too many times."
        const val REASON_INVALID_REDIRECT = "This link could not be followed."
        const val REASON_LEFT_GOOGLE = "This link leaves Google Maps, so it was not followed."
        const val REASON_DEAD_LINK = "This link no longer opens."

        // v2.3.5 (#3): failure messages that say what actually happened. Only REASON_OFFLINE and
        // REASON_CANT_CONNECT talk about connectivity, and only when that is the real cause.
        const val REASON_OFFLINE = "You're offline. Connect and try again, or paste coordinates."
        const val REASON_CANT_CONNECT =
            "Couldn't connect to Google Maps on this network. Try again, or paste coordinates or search by name."
        const val REASON_TIMEOUT =
            "Google Maps took too long to answer. Try again, or paste coordinates or search by name."
        const val REASON_SECURE_CONNECTION =
            "This network blocked the secure link to Google Maps. Paste coordinates or search by name."
        const val REASON_GOOGLE_REFUSED =
            "Google Maps refused the request for now. Paste coordinates or search by name."
        const val REASON_GOOGLE_UNAVAILABLE =
            "Google Maps isn't responding right now. Paste coordinates or search by name."
        const val REASON_NETWORK =
            "Couldn't open this Google Maps link. Try again, or paste coordinates or search by name."

        /** GET responses that mean "try HEAD instead". */
        private val GET_REFUSED = setOf(405, 501)

        /** Statuses that mean the short link itself is gone (no identity fallback). */
        private val GONE = setOf(404, 410)

        /**
         * Maps a thrown failure to a user-facing reason. "Offline" is only claimed when the device
         * reports no network; DNS/connect failures say we couldn't connect; TLS failures point at
         * the network blocking us; timeouts say Google was slow.
         */
        internal fun classifyFailure(e: Throwable, online: Boolean?): String = when {
            online == false -> REASON_OFFLINE
            e is UnknownHostException || e is ConnectException || e is NoRouteToHostException -> REASON_CANT_CONNECT
            e is SSLException -> REASON_SECURE_CONNECTION
            e is InterruptedIOException -> REASON_TIMEOUT
            e is IOException -> REASON_NETWORK
            else -> REASON_INVALID_REDIRECT
        }

        /** `www.google.<tld>/sorry/...` rate-limit interstitial. */
        internal fun isSorryPage(u: SimpleUrl): Boolean =
            GOOGLE_WWW.matches(u.host) && (u.path == "/sorry" || u.path.startsWith("/sorry/"))

        private fun isShortener(u: SimpleUrl): Boolean =
            u.host == "maps.app.goo.gl" || u.host == "goo.gl" || u.host == "g.co"

        private const val GOOGLE_TLD = """(?:com|[a-z]{2}|co\.[a-z]{2}|com\.[a-z]{2})"""
        private val GOOGLE_WWW = Regex("""^(?:www\.)?google\.$GOOGLE_TLD$""")
        private val MAPS_GOOGLE = Regex("""^maps\.google\.$GOOGLE_TLD$""")
        private val CONSENT_HOST = Regex("""^consent\.google\.$GOOGLE_TLD$""")

        private val HTTP_URL = Regex("""(?i)\bhttps?://[^\s<>"'`]+""")
        private val BARE_URL = Regex(
            """(?i)(?<![\w./@-])(?:maps\.app\.goo\.gl|goo\.gl/maps|g\.co/kgs|maps\.google\.[a-z.]{2,10}|(?:www\.)?google\.[a-z.]{2,10}/maps)[^\s<>"'`]*"""
        )

        /**
         * Finds the first http(s) URL in free text (share-intent payloads put the name first).
         * A bare `maps.app.goo.gl/…` style link without a scheme is upgraded to `https://`.
         */
        internal fun findFirstUrl(text: String): String? {
            HTTP_URL.find(text)?.let { return trimTrailingPunctuation(it.value) }
            BARE_URL.find(text)?.let { return "https://" + trimTrailingPunctuation(it.value) }
            return null
        }

        private fun trimTrailingPunctuation(s: String): String = s.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}', '\'', '"')

        /** Allow-list for URLs the user pasted (see class docs). */
        internal fun isAllowedInputUrl(u: SimpleUrl): Boolean {
            if (u.port != null && u.port != 443) return false
            val path = u.path
            return when {
                u.host == "maps.app.goo.gl" -> true
                u.host == "goo.gl" -> path == "/maps" || path.startsWith("/maps/")
                u.host == "g.co" -> path == "/kgs" || path.startsWith("/kgs/")
                MAPS_GOOGLE.matches(u.host) -> true
                GOOGLE_WWW.matches(u.host) -> path == "/maps" || path.startsWith("/maps/")
                else -> false
            }
        }

        /**
         * Only link shorteners and legacy `maps.google.*` URLs are worth a network round-trip.
         * A full `www.google.<tld>/maps/...` URL already carries everything we are allowed to read,
         * so it is parsed offline (less traffic to Google, see ToS note in audit §2.3).
         * v2.3.5: the exception is a `google.<tld>/maps?cid=…` / `…&ftid=…` place-id link, which
         * never carries coordinates and must be followed to find the pin.
         */
        internal fun needsNetwork(u: SimpleUrl?): Boolean {
            if (u == null) return false
            if (u.host == "maps.app.goo.gl" || u.host == "goo.gl" || u.host == "g.co" || MAPS_GOOGLE.matches(u.host)) {
                return true
            }
            return GOOGLE_WWW.matches(u.host) && (u.rawQueryParam("cid") != null || u.rawQueryParam("ftid") != null)
        }

        /** Allow-list for redirect targets: input allow-list plus consent and any google.<tld> path. */
        internal fun isAllowedHopUrl(u: SimpleUrl): Boolean {
            if (u.port != null && u.port != 443) return false
            return isAllowedInputUrl(u) || CONSENT_HOST.matches(u.host) || GOOGLE_WWW.matches(u.host)
        }

        /** Resolves a (possibly relative) `Location` header against the current URL. */
        internal fun resolveLocation(current: SimpleUrl, location: String): String? {
            return when {
                location.contains("://") -> location
                location.startsWith("//") -> "${current.scheme}:$location"
                location.startsWith("/") -> current.origin + location
                location.startsWith("?") -> current.origin + current.path + location
                location.isEmpty() -> null
                else -> {
                    val dir = current.path.substringBeforeLast('/', missingDelimiterValue = "")
                    "${current.origin}$dir/$location"
                }
            }
        }
    }
}
