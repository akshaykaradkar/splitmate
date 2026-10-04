package com.splitmate.app.data.guide.loop

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.GuideFeatureFlags
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.StayResolution
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONException
import java.util.Locale

/**
 * Serialises calls and enforces a minimum interval between them (Nominatim: ≤ 1 request/second).
 * Clock and sleeper are injectable for deterministic tests.
 */
class MinIntervalRateLimiter(
    private val minIntervalMs: Long,
    private val clockMs: () -> Long = { System.nanoTime() / 1_000_000L },
    private val sleeper: suspend (Long) -> Unit = { delay(it) }
) {
    private val mutex = Mutex()
    private var lastStartMs: Long? = null

    /** Runs [block] once at least [minIntervalMs] has passed since the previous call started. */
    suspend fun <T> throttle(block: suspend () -> T): T = mutex.withLock {
        val last = lastStartMs
        if (last != null) {
            val waitMs = last + minIntervalMs - clockMs()
            if (waitMs > 0) sleeper(waitMs)
        }
        lastStartMs = clockMs()
        block()
    }
}

/**
 * Name → coordinate fallback for stays whose link carried no coordinate (decision #12).
 *
 * OSMF Nominatim usage policy (audit §2.5 / §2.3):
 * - Only **single, user-initiated** searches. No autocomplete, no bulk, no background retries.
 * - At most **1 request per second** per app process ([SHARED_LIMITER]).
 * - Identified User-Agent (added by [HttpTextFetcher] implementations).
 * - Attribution "© OpenStreetMap contributors" must be shown wherever the result is displayed
 *   ([ATTRIBUTION]).
 * - Behind the [GuideFeatureFlags.nominatimEnabled] kill-switch.
 *
 * Results are always [CoordinatePrecision.APPROXIMATE] (a geocoded name, not a pin).
 */
class NominatimGeocoder(
    private val fetcher: HttpTextFetcher = OkHttpTextFetcher(),
    private val flags: () -> GuideFeatureFlags = { GuideFeatureFlags() },
    private val limiter: MinIntervalRateLimiter = SHARED_LIMITER,
    private val endpoint: String = DEFAULT_ENDPOINT
) {

    /**
     * Geocodes "<nameHint>, <destination>" (destination appended only if not already present).
     *
     * @return Resolved (APPROXIMATE) on a hit; NoCoordinates(null, nameHint) when Nominatim finds
     * nothing; Rejected when disabled, the query is empty, or the request fails.
     */
    suspend fun geocode(nameHint: String, destination: String?): StayResolution {
        if (!flags().nominatimEnabled) return StayResolution.Rejected(REASON_DISABLED)
        val query = buildQuery(nameHint, destination) ?: return StayResolution.Rejected(REASON_EMPTY)
        val url = buildUrl(query)
        return try {
            val response = limiter.throttle { fetcher.get(url, HEADERS) }
            if (response.statusCode !in 200..299) return StayResolution.Rejected(REASON_FAILED)
            parse(response.body, fallbackLabel = LoopText.clean(nameHint))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            StayResolution.Rejected(REASON_FAILED)
        }
    }

    /**
     * v2.3.5 (#3) automatic fallback for a coordinate-less Google link (and typed stay names):
     * runs the candidates from [smartPlan] (full name/address, name + locality, then the
     * street/locality or stripped-name fallbacks), each asking for [SMART_LIMIT] results, and
     * returns the first valid hit as APPROXIMATE. Same policy as [geocode]: one user action,
     * shared 1 req/s limiter, kill-switch, identified UA.
     *
     * Request budget: at most [MAX_SMART_QUERIES] Nominatim requests in total per call, fallbacks
     * included (OSMF usage policy; also keeps the worst case at ~3 s plus network time).
     *
     * @param destinationCenter optional centre of the trip destination. When present, the
     * stripped-name query (e.g. just the core name without "Hotel"/"Homestay") is restricted to a
     * viewbox around it (`bounded=1`). Without a destination the stripped-name query is skipped.
     * @return Resolved on a hit; NoCoordinates(null, name) when every query came back empty;
     * Rejected when disabled, the hint is empty, or every request failed.
     */
    suspend fun geocodeSmart(nameHint: String, destination: String?, destinationCenter: LatLng? = null): StayResolution {
        if (!flags().nominatimEnabled) return StayResolution.Rejected(REASON_DISABLED)
        val plan = smartPlan(nameHint, destination)
        if (plan.isEmpty()) return StayResolution.Rejected(REASON_EMPTY)
        val label = placeName(nameHint)
        var failures = 0
        for (q in plan) {
            val result = try {
                val response = limiter.throttle { fetcher.get(buildSmartUrl(q, destinationCenter), HEADERS) }
                if (response.statusCode !in 200..299) null else parse(response.body, fallbackLabel = label)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            when (result) {
                is StayResolution.Resolved -> return result
                is StayResolution.NoCoordinates -> Unit
                else -> failures++
            }
        }
        return if (failures == plan.size) {
            StayResolution.Rejected(REASON_FAILED)
        } else {
            StayResolution.NoCoordinates(null, label)
        }
    }

    /** Builds the Nominatim `/search` URL (jsonv2, default limit=1). Exposed for tests. */
    fun buildUrl(query: String, limit: Int = 1): String =
        "$endpoint?q=${LoopText.percentEncode(query)}&format=jsonv2&limit=${limit.coerceIn(1, 5)}&addressdetails=0"

    /** URL for one [SmartQuery]; a bounded query gets a viewbox around [center] (when known). */
    internal fun buildSmartUrl(query: SmartQuery, center: LatLng?): String {
        val base = buildUrl(query.text, SMART_LIMIT)
        if (!query.boundedToDestination || center == null) return base
        val d = VIEWBOX_HALF_SPAN_DEG
        val left = (center.lng - d).coerceIn(-180.0, 180.0)
        val right = (center.lng + d).coerceIn(-180.0, 180.0)
        val top = (center.lat + d).coerceIn(-90.0, 90.0)
        val bottom = (center.lat - d).coerceIn(-90.0, 90.0)
        val box = String.format(Locale.ROOT, "%.4f,%.4f,%.4f,%.4f", left, top, right, bottom)
        return "$base&viewbox=${LoopText.percentEncode(box)}&bounded=1"
    }

    /** One planned smart query. [boundedToDestination] = must stay near the trip destination. */
    data class SmartQuery(val text: String, val boundedToDestination: Boolean = false)

    companion object {
        const val DEFAULT_ENDPOINT = "https://nominatim.openstreetmap.org/search"
        const val ATTRIBUTION = "© OpenStreetMap contributors"
        const val MIN_INTERVAL_MS = 1_000L

        const val REASON_DISABLED = "Place search is turned off right now."
        const val REASON_EMPTY = "Enter a place name to search."
        const val REASON_FAILED = "Place search failed. Try again or paste coordinates."

        /** Process-wide limiter so multiple geocoder instances still respect 1 req/s. */
        val SHARED_LIMITER = MinIntervalRateLimiter(MIN_INTERVAL_MS)

        private val HEADERS = mapOf(
            "Accept" to "application/json",
            "Accept-Language" to "en"
        )

        /** Combines the hint with the destination, de-duplicating and sanitising. */
        fun buildQuery(nameHint: String, destination: String?): String? {
            val name = LoopText.clean(nameHint, maxChars = 200) ?: return null
            val dest = LoopText.clean(destination, maxChars = 100)
            return if (dest == null || name.contains(dest, ignoreCase = true)) name else "$name, $dest"
        }

        /** v2.3.5: results requested per smart query. */
        const val SMART_LIMIT = 3

        /** v2.3.5: maximum Nominatim requests per automatic fallback. */
        const val MAX_SMART_QUERIES = 3

        private val POSTCODE = Regex("""\b\d{5,6}\b""")
        private val COUNTRY_NOISE = setOf("india", "bharat")
        private val PLOT_PREFIX = Regex("""^(?i)(?:plot|shop|flat|house|h\.?\s*no\.?|survey|sr\.?\s*no\.?|ward|near|opp\.?|opposite|behind)\b.*$""")
        private val HOSPITALITY_NOISE = Regex(
            """(?i)\b(?:bed\s+and\s+breakfast|bed\s*&\s*breakfast|b\s*&\s*b|homestay|home\s+stay|guest\s*house|guesthouse|hotel|resort|resorts|villa|villas|lodge|cottage|cottages|inn|suites|stay|rooms)\b"""
        )

        /** v2.3.5: half the side of the viewbox (degrees, ~25 km) around the destination centre. */
        const val VIEWBOX_HALF_SPAN_DEG = 0.25

        /** Address/name tokens shorter than this at the end of a truncated hint are dropped. */
        private const val MIN_TAIL_TOKEN_CHARS = 3

        /**
         * Splits a place hint into meaningful comma parts, removing truncation garbage:
         * - a hint cut with "…"/"..." loses its partial last part (or, for a single part, its
         *   partial trailing word and any trailing token shorter than [MIN_TAIL_TOKEN_CHARS]);
         * - a last part that is only the beginning of [destination] (cut upstream without an
         *   ellipsis, e.g. `"Panj"` for `"Panjim"`) is dropped.
         */
        private fun hintParts(nameHint: String, destination: String?): List<String> {
            val raw = nameHint.trim()
            val truncated = raw.endsWith("…") || raw.endsWith("...")
            val base = LoopText.clean(raw.removeSuffix("…").removeSuffix("..."), maxChars = 200)
                ?.trim()?.trimEnd(',', ';', ' ')
                ?: return emptyList()
            var parts = base.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            if (truncated) {
                parts = if (parts.size > 1) parts.dropLast(1) else listOfNotNull(parts.firstOrNull()?.let(::dropPartialTail))
            }
            val dest = LoopText.clean(destination, maxChars = 100)
            if (parts.size > 1 && dest != null) {
                val last = parts.last()
                if (last.length < dest.length && dest.startsWith(last, ignoreCase = true)) parts = parts.dropLast(1)
            }
            return parts
        }

        /** Drops the (partial) trailing word of a truncated single-part hint, then short tail tokens. */
        private fun dropPartialTail(part: String): String? {
            val words = part.split(' ').filter { it.isNotEmpty() }.toMutableList()
            if (words.size <= 1) return null
            words.removeAt(words.lastIndex)
            while (words.size > 1 && words.last().count { it.isLetterOrDigit() } < MIN_TAIL_TOKEN_CHARS) {
                words.removeAt(words.lastIndex)
            }
            return words.joinToString(" ").trim(' ', '-', '·').takeIf { it.isNotEmpty() }
        }

        /**
         * v2.3.5 (#3): query candidates for a Google place hint like
         * `"Sea Breeze Homestay, Plot 4, Beach Road, Hilltown, State 400001"`:
         * 1. the full hint (+ destination), 2. `<name>, <locality>` (the last two meaningful
         * address parts, postcode/country stripped), 3. `<name>, <destination>`.
         * A hint already cut with "…" (labels are capped at 120 chars upstream) loses its partial
         * last part instead of sending a half word to Nominatim. De-duplicated, at most
         * [MAX_SMART_QUERIES].
         */
        fun smartQueries(nameHint: String, destination: String?): List<String> {
            val parts = hintParts(nameHint, destination)
            val name = parts.firstOrNull() ?: return emptyList()
            val full = parts.joinToString(", ")
            val locality = parts.drop(1)
                .map { it.replace(POSTCODE, "").trim() }
                .filter { p -> p.any { it.isLetter() } && p.lowercase() !in COUNTRY_NOISE }
                .takeLast(2)
                .joinToString(", ")
            val out = LinkedHashSet<String>()
            buildQuery(full, destination)?.let { out += it }
            if (locality.isNotEmpty()) buildQuery("$name, $locality", null)?.let { out += it }
            buildQuery(name, destination)?.let { out += it }
            return out.take(MAX_SMART_QUERIES)
        }

        /**
         * Secondary queries when a small homestay/B&B name is absent from OpenStreetMap:
         * the street/locality (e.g. `"Beach Road, Hilltown"`) and, only when a destination is
         * known, the core name stripped of hospitality words (e.g. `"Sea Breeze, <destination>"`).
         * A lone street without a city or destination is never searched worldwide.
         */
        internal fun fallbackLocalityQueries(nameHint: String, destination: String?): List<String> =
            fallbackPlan(nameHint, destination).map { it.text }

        private fun fallbackPlan(nameHint: String, destination: String?): List<SmartQuery> {
            val parts = hintParts(nameHint, destination)
            val name = parts.firstOrNull() ?: return emptyList()
            val dest = LoopText.clean(destination, maxChars = 100)
            val addressParts = parts.drop(1)
                .map { it.replace(POSTCODE, "").trim() }
                .filter { p ->
                    p.count { it.isLetter() } >= MIN_TAIL_TOKEN_CHARS &&
                        p.lowercase() !in COUNTRY_NOISE &&
                        !PLOT_PREFIX.matches(p)
                }
            val out = LinkedHashMap<String, SmartQuery>()
            if (addressParts.size >= 2 || (addressParts.isNotEmpty() && dest != null)) {
                val streetAndCity = addressParts.take(2).joinToString(", ")
                buildQuery(streetAndCity, dest)?.let { out.putIfAbsent(it.lowercase(), SmartQuery(it)) }
            }
            if (dest != null) {
                val strippedName = name.replace(HOSPITALITY_NOISE, " ")
                    .replace(Regex("""\s+"""), " ")
                    .trim(' ', '-', ',', '·')
                    .takeIf { it.count { c -> c.isLetter() } >= MIN_TAIL_TOKEN_CHARS && !it.equals(name, ignoreCase = true) }
                if (strippedName != null) {
                    buildQuery(strippedName, dest)?.let {
                        out.putIfAbsent(it.lowercase(), SmartQuery(it, boundedToDestination = true))
                    }
                }
            }
            return out.values.toList()
        }

        /**
         * The full, ordered request plan for [geocodeSmart], capped at [MAX_SMART_QUERIES]:
         * the first two primary candidates (full hint, name + locality), then the locality /
         * stripped-name fallbacks, then the remaining primary candidate. De-duplicated
         * (case-insensitive). The viewbox for bounded queries is added by [buildSmartUrl].
         */
        internal fun smartPlan(nameHint: String, destination: String?): List<SmartQuery> {
            val primary = smartQueries(nameHint, destination).map { SmartQuery(it) }
            val ordered = primary.take(2) + fallbackPlan(nameHint, destination) + primary.drop(2)
            val out = LinkedHashMap<String, SmartQuery>()
            for (q in ordered) out.putIfAbsent(q.text.lowercase(), q)
            return out.values.take(MAX_SMART_QUERIES)
        }

        /** The place name part of a hint (before the first comma), for labels. */
        fun placeName(nameHint: String): String? =
            LoopText.clean(nameHint.removeSuffix("…"), maxChars = 200)
                ?.substringBefore(',')?.trim()?.takeIf { it.isNotEmpty() }
                ?.let { LoopText.clean(it) }

        /**
         * Parses a jsonv2 response array and returns the first entry with valid coordinates.
         * Pure; exposed for fixture tests.
         */
        fun parse(body: String, fallbackLabel: String?): StayResolution {
            val arr = try {
                JSONArray(body)
            } catch (e: JSONException) {
                return StayResolution.Rejected(REASON_FAILED)
            }
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val lat = item.optString("lat").toDoubleOrNull() ?: continue
                val lng = item.optString("lon").toDoubleOrNull() ?: continue
                val ll = MapLinkCoordinateParser.validate(lat, lng) ?: continue
                val label = LoopText.clean(item.optString("name").takeIf { it.isNotBlank() })
                    ?: fallbackLabel
                    ?: LoopText.clean(item.optString("display_name").substringBefore(',').takeIf { it.isNotBlank() })
                return StayResolution.Resolved(ll, label, CoordinatePrecision.APPROXIMATE)
            }
            return StayResolution.NoCoordinates(null, fallbackLabel)
        }
    }
}
