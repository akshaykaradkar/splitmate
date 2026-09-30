package com.splitmate.app.data.guide.loop

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.GuideFeatureFlags
import com.splitmate.app.data.guide.StayLocationResolver
import com.splitmate.app.data.guide.StayResolution
import kotlinx.coroutines.CancellationException

/**
 * Resolves a pasted/shared Google Maps link (or plain pasted coordinates) into a stay location.
 *
 * Policy (audit §2.3 / §4.1, decisions #12 and #13):
 * - **Offline first:** the text is always run through [MapLinkCoordinateParser]. An exact
 *   coordinate found locally is returned without any network call. Only shorteners
 *   (`maps.app.goo.gl`, `goo.gl`, `g.co`) and legacy `maps.google.<tld>` links trigger network
 *   probing. Full `www.google.<tld>/maps/…` URLs are parsed offline only.
 * - **Host allow-list** for any URL in the input: `maps.app.goo.gl`, `goo.gl/maps…`,
 *   `maps.google.<tld>`, `(www.)google.<tld>/maps…`, `g.co/kgs…`. Any other host is Rejected,
 *   even when the URL happens to contain numbers.
 * - **HTTPS only.** `http://` input or an `http://` redirect target is Rejected.
 * - **Manual redirects:** HEAD first, GET (`Range: bytes=0-0`) fallback when HEAD is refused.
 *   At most [MAX_REDIRECTS] redirects are followed. Response bodies are never read or parsed.
 *   Only `Location` headers are inspected, and every hop URL goes through the parser.
 * - **consent.google.\*** interstitials are not fetched. Their `continue=` parameter is decoded
 *   and followed instead.
 * - Every redirect target must stay on a Google host (allow-list plus `consent.` / `www.google.`).
 * - **Kill-switch:** when [GuideFeatureFlags.shortLinkParsingEnabled] is false, only the pasted
 *   text is parsed (no network). A coordinate-less Google link then yields
 *   [StayResolution.NoCoordinates] so the UI can offer the manual/Nominatim fallbacks.
 *
 * Results:
 * - [StayResolution.Resolved]: EXACT for pin/query/path/geo/DMS/decimal rungs, APPROXIMATE for a
 *   viewport-only (`@lat,lng,z`) coordinate.
 * - [StayResolution.NoCoordinates]: valid Google link with no coordinate (e.g. `q=<name>&ftid=`).
 *   Carries the final URL and a place-name hint for the Nominatim fallback.
 * - [StayResolution.Rejected]: unsupported host, insecure link, too many redirects, dead link,
 *   network failure or nothing parseable. The reason is short, user-safe text.
 */
class ShortLinkResolver(
    private val fetcher: RedirectFetcher = OkHttpRedirectFetcher(),
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

        return try {
            follow(url, localCoord, local.placeNameHint)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // IOException, timeouts, malformed URLs rejected by OkHttp, etc. No details logged.
            StayResolution.Rejected(REASON_NETWORK)
        }
    }

    private suspend fun follow(
        startUrl: String,
        initialApprox: ParsedCoordinate?,
        initialHint: String?
    ): StayResolution {
        var current = startUrl
        var approx = initialApprox
        // URL-derived names (/place/<name>, q=<text>) beat free share-text prefixes.
        var urlHint: String? = null
        var redirects = 0
        var consentUnwraps = 0

        while (true) {
            val hop = MapLinkCoordinateParser.parse(current)
            if (urlHint == null) urlHint = hop.placeNameHint
            val hint = urlHint ?: initialHint
            hop.coordinate?.let { c ->
                if (c.precision == CoordinatePrecision.EXACT) {
                    return StayResolution.Resolved(c.location, hint, CoordinatePrecision.EXACT)
                }
                if (approx == null) approx = c
            }

            val parsed = SimpleUrl.parse(current) ?: return StayResolution.Rejected(REASON_INVALID_REDIRECT)

            if (CONSENT_HOST.matches(parsed.host)) {
                if (++consentUnwraps > MAX_REDIRECTS) return StayResolution.Rejected(REASON_TOO_MANY_REDIRECTS)
                val cont = parsed.rawQueryParam("continue")
                    ?: return finish(approx, hint, current, terminalStatus = 200, redirects = redirects)
                val next = LoopText.percentDecode(cont, plusAsSpace = false)
                checkHop(next)?.let { return it }
                current = next
                continue
            }

            var probe = fetcher.probe(current, ProbeMethod.HEAD)
            if (probe.statusCode in HEAD_REFUSED) {
                probe = fetcher.probe(current, ProbeMethod.GET)
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
            return finish(approx, hint, current, probe.statusCode, redirects)
        }
    }

    private fun finish(
        approx: ParsedCoordinate?,
        hint: String?,
        finalUrl: String,
        terminalStatus: Int,
        redirects: Int
    ): StayResolution {
        if (approx != null) return StayResolution.Resolved(approx.location, hint, approx.precision)
        if (terminalStatus >= 400 && redirects == 0) return StayResolution.Rejected(REASON_DEAD_LINK)
        return StayResolution.NoCoordinates(finalUrl, hint)
    }

    /** Returns a rejection when [next] is not an acceptable redirect target, else null. */
    private fun checkHop(next: String): StayResolution.Rejected? {
        val u = SimpleUrl.parse(next) ?: return StayResolution.Rejected(REASON_INVALID_REDIRECT)
        if (u.scheme != "https") return StayResolution.Rejected(REASON_INSECURE)
        if (!isAllowedHopUrl(u)) return StayResolution.Rejected(REASON_LEFT_GOOGLE)
        return null
    }

    companion object {
        /** Maximum number of HTTP redirects followed (audit §4.1). */
        const val MAX_REDIRECTS = 5

        const val REASON_EMPTY = "Paste a Google Maps link or coordinates."
        const val REASON_NOTHING_FOUND = "No location found in the pasted text."
        const val REASON_UNSUPPORTED_HOST = "Only Google Maps links are supported."
        const val REASON_INSECURE = "Only secure (https) links are supported."
        const val REASON_TOO_MANY_REDIRECTS = "This link redirects too many times."
        const val REASON_INVALID_REDIRECT = "This link could not be followed."
        const val REASON_LEFT_GOOGLE = "This link leaves Google Maps, so it was not followed."
        const val REASON_DEAD_LINK = "This link no longer opens."
        const val REASON_NETWORK = "Couldn't reach Google Maps. Check your connection."

        /** HEAD responses that mean "try GET instead". */
        private val HEAD_REFUSED = setOf(400, 403, 405, 501)

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
         */
        internal fun needsNetwork(u: SimpleUrl?): Boolean {
            if (u == null) return false
            return u.host == "maps.app.goo.gl" || u.host == "goo.gl" || u.host == "g.co" ||
                MAPS_GOOGLE.matches(u.host)
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
