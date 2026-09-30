package com.splitmate.app.data.guide.nav

import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.LoopRoute
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/** Travel modes supported by Google Maps handoffs. */
enum class TravelMode(
    /** `google.navigation:` `mode=` code. */
    val navigationCode: String,
    /** Maps URLs `travelmode=` value. */
    val urlValue: String
) {
    WALKING("w", "walking"),
    DRIVING("d", "driving"),
    BICYCLING("b", "bicycling"),
    TWO_WHEELER("l", "two-wheeler")
}

/**
 * Pure string builders for map handoffs (audit §2.4 / §5.8, decisions #24 and #25). No Android
 * dependencies: callers wrap the strings in `Intent(ACTION_VIEW, Uri.parse(...))`, catch
 * `ActivityNotFoundException`, and fall back to [searchUrl] in a browser.
 *
 * Coordinates are formatted with [Locale.ROOT] and ≤ 6 decimals (≈ 11 cm), trailing zeros
 * stripped, so output never depends on the device locale (no `15,335` decimal commas).
 * Free text (labels) is sanitised (control characters removed) and RFC 3986 percent-encoded.
 */
object MapsHandoff {

    /** Maps URLs support at most 9 waypoints per link (decision #25: chunk above that). */
    const val MAX_WAYPOINTS_PER_URL = 9

    private const val DIR_BASE = "https://www.google.com/maps/dir/?api=1"
    private const val SEARCH_BASE = "https://www.google.com/maps/search/?api=1"
    private const val MAX_LABEL_CHARS = 100

    // ---------------------------------------------------------------------------------------
    // Single place
    // ---------------------------------------------------------------------------------------

    /**
     * Primary "Directions" intent URI: `geo:0,0?q=lat,lng(Label)`. Works with Google Maps,
     * OsmAnd, Organic Maps, HERE. The label is only honoured inside `q=` and is percent-encoded
     * (including parentheses) so it cannot break the URI.
     */
    fun geoUri(location: LatLng, label: String?): String {
        val q = coord(location)
        val clean = cleanLabel(label)
        return if (clean == null) "geo:0,0?q=$q" else "geo:0,0?q=$q(${encode(clean)})"
    }

    /** Google Maps turn-by-turn: `google.navigation:q=lat,lng&mode=w|d|b|l` (Google Maps only). */
    fun navigationUri(destination: LatLng, mode: TravelMode = TravelMode.WALKING): String =
        "google.navigation:q=${coord(destination)}&mode=${mode.navigationCode}"

    /**
     * Universal cross-platform directions URL (no API key):
     * `https://www.google.com/maps/dir/?api=1&origin=…&destination=…&travelmode=…`.
     * Origin omitted ⇒ Maps uses the device location.
     */
    fun directionsUrl(destination: LatLng, origin: LatLng? = null, mode: TravelMode? = null): String =
        buildDirUrl(origin, destination, emptyList(), mode)

    /** Browser fallback that shows a pin: `https://www.google.com/maps/search/?api=1&query=lat%2Clng`. */
    fun searchUrl(location: LatLng): String = "$SEARCH_BASE&query=${encode(coord(location))}"

    /** Copyable coordinates, e.g. `15.335, 76.46`. */
    fun copyableCoordinates(location: LatLng): String =
        "${formatDegrees(location.lat)}, ${formatDegrees(location.lng)}"

    /** WhatsApp-friendly single place share: name, coordinates and a universal link. */
    fun placeShareText(name: String?, location: LatLng): String = buildString {
        cleanLabel(name)?.let { append('*').append(it).append("*\n") }
        append(copyableCoordinates(location)).append('\n')
        append(searchUrl(location))
    }

    // ---------------------------------------------------------------------------------------
    // Whole loop
    // ---------------------------------------------------------------------------------------

    /**
     * "Open whole loop in Google Maps" (decision #25). The closed sequence
     * `stay → s1 … sn → stay` is split into consecutive segments with at most [maxWaypoints]
     * intermediate waypoints each. Every segment starts exactly where the previous one ended.
     * Returns an empty list when the loop has no stops.
     */
    fun loopDirectionsUrls(
        route: LoopRoute,
        mode: TravelMode? = null,
        maxWaypoints: Int = MAX_WAYPOINTS_PER_URL
    ): List<String> {
        val stopPoints = route.orderedStops.mapNotNull { it.location }
        return loopDirectionsUrls(route.start, stopPoints, mode, maxWaypoints)
    }

    /** Same as the [LoopRoute] overload, from raw points. */
    fun loopDirectionsUrls(
        stay: LatLng,
        orderedStops: List<LatLng>,
        mode: TravelMode? = null,
        maxWaypoints: Int = MAX_WAYPOINTS_PER_URL
    ): List<String> {
        require(maxWaypoints >= 0) { "maxWaypoints must be >= 0" }
        if (orderedStops.isEmpty()) return emptyList()
        val seq = ArrayList<LatLng>(orderedStops.size + 2)
        seq += stay
        seq += orderedStops
        seq += stay
        val last = seq.size - 1
        val urls = ArrayList<String>()
        var s = 0
        while (s < last) {
            val e = minOf(s + maxWaypoints + 1, last)
            urls += buildDirUrl(seq[s], seq[e], seq.subList(s + 1, e), mode)
            s = e
        }
        return urls
    }

    /**
     * WhatsApp-friendly loop summary (plain text, `*bold*` markup, no raw JSON), e.g.:
     * ```
     * *Day loop from Hotel Mayura* (3 stops)
     * 1. Virupaksha Temple (1.2 km)
     * 2. Vittala Temple (2.9 km)
     * 3. Hampi Bazaar (1.1 km)
     * Back to Hotel Mayura (2.0 km)
     * Total ≈ 9.7 km by road (7.2 km straight-line estimate)
     * Open in Google Maps:
     * https://www.google.com/maps/dir/?api=1&…
     * ```
     */
    fun loopShareText(
        route: LoopRoute,
        stayLabel: String?,
        mode: TravelMode? = null,
        appSignature: String? = "Planned with SplitMate"
    ): String = buildString {
        val stay = cleanLabel(stayLabel) ?: "our stay"
        val n = route.orderedStops.size
        append("*Day loop from ").append(stay).append("* (")
            .append(n).append(if (n == 1) " stop" else " stops").append(")\n")
        route.orderedStops.forEachIndexed { i, place ->
            append(i + 1).append(". ").append(cleanLabel(place.name) ?: "Stop ${i + 1}")
            route.legStraightKm.getOrNull(i)?.let { append(" (").append(km(it)).append(')') }
            append('\n')
        }
        if (n > 0) {
            append("Back to ").append(stay)
            route.legStraightKm.getOrNull(n)?.let { append(" (").append(km(it)).append(')') }
            append('\n')
        }
        append("Total ≈ ").append(km(route.totalRoadEstimateKm)).append(" by road (")
            .append(km(route.totalStraightKm)).append(" straight-line estimate)\n")
        val urls = loopDirectionsUrls(route, mode)
        if (urls.isNotEmpty()) {
            append("Open in Google Maps:\n")
            urls.forEachIndexed { i, u ->
                if (urls.size > 1) append("Part ").append(i + 1).append(": ")
                append(u).append('\n')
            }
        }
        appSignature?.let { append(it) }
    }.trimEnd()

    // ---------------------------------------------------------------------------------------
    // Formatting helpers
    // ---------------------------------------------------------------------------------------

    /** `lat,lng` with ≤ 6 decimals, Locale.ROOT, no trailing zeros. */
    fun coord(location: LatLng): String = "${formatDegrees(location.lat)},${formatDegrees(location.lng)}"

    /** Formats a degree value with ≤ 6 decimals, HALF_UP, trailing zeros stripped, no exponent. */
    fun formatDegrees(value: Double): String {
        val bd = BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros()
        val s = bd.toPlainString()
        return if (s == "-0") "0" else s
    }

    private fun km(value: Double): String = String.format(Locale.ROOT, "%.1f km", value)

    private fun buildDirUrl(origin: LatLng?, destination: LatLng, waypoints: List<LatLng>, mode: TravelMode?): String =
        buildString {
            append(DIR_BASE)
            if (origin != null) append("&origin=").append(encode(coord(origin)))
            append("&destination=").append(encode(coord(destination)))
            if (waypoints.isNotEmpty()) {
                append("&waypoints=").append(encode(waypoints.joinToString("|") { coord(it) }))
            }
            if (mode != null) append("&travelmode=").append(mode.urlValue)
        }

    private val CONTROL = Regex("""[\p{Cc}\p{Cf}\u2028\u2029]""")
    private val SPACES = Regex("""\s+""")

    private fun cleanLabel(label: String?): String? {
        if (label == null) return null
        var s = label.replace(CONTROL, " ").replace(SPACES, " ").trim()
        if (s.isEmpty()) return null
        if (s.length > MAX_LABEL_CHARS) s = s.take(MAX_LABEL_CHARS - 1).trimEnd() + "…"
        return s
    }

    private const val UNRESERVED =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    private const val HEX = "0123456789ABCDEF"

    /** RFC 3986 percent-encoding (UTF-8; space ⇒ `%20`; `,` ⇒ `%2C`; `|` ⇒ `%7C`; `(` ⇒ `%28`). */
    fun encode(s: String): String {
        val sb = StringBuilder(s.length + 16)
        for (b in s.toByteArray(Charsets.UTF_8)) {
            val v = b.toInt() and 0xFF
            val ch = v.toChar()
            if (v < 128 && UNRESERVED.indexOf(ch) >= 0) {
                sb.append(ch)
            } else {
                sb.append('%').append(HEX[v shr 4]).append(HEX[v and 0xF])
            }
        }
        return sb.toString()
    }
}
