package com.splitmate.app.data.guide.loop

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.GuideFeatureFlags
import com.splitmate.app.data.guide.StayResolution
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONException

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

    /** Builds the Nominatim `/search` URL (jsonv2, limit=1). Exposed for tests. */
    fun buildUrl(query: String): String =
        "$endpoint?q=${LoopText.percentEncode(query)}&format=jsonv2&limit=1&addressdetails=0"

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

        /** Parses a jsonv2 response array. Pure; exposed for fixture tests. */
        fun parse(body: String, fallbackLabel: String?): StayResolution {
            val arr = try {
                JSONArray(body)
            } catch (e: JSONException) {
                return StayResolution.Rejected(REASON_FAILED)
            }
            if (arr.length() == 0) return StayResolution.NoCoordinates(null, fallbackLabel)
            val first = arr.optJSONObject(0) ?: return StayResolution.NoCoordinates(null, fallbackLabel)
            val lat = first.optString("lat").toDoubleOrNull()
            val lng = first.optString("lon").toDoubleOrNull()
            if (lat == null || lng == null) return StayResolution.NoCoordinates(null, fallbackLabel)
            val ll = MapLinkCoordinateParser.validate(lat, lng)
                ?: return StayResolution.NoCoordinates(null, fallbackLabel)
            val label = LoopText.clean(first.optString("name").takeIf { it.isNotBlank() })
                ?: fallbackLabel
                ?: LoopText.clean(first.optString("display_name").substringBefore(',').takeIf { it.isNotBlank() })
            return StayResolution.Resolved(ll, label, CoordinatePrecision.APPROXIMATE)
        }
    }
}
