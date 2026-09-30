package com.splitmate.app.data.guide.loop

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.LatLng
import kotlin.math.abs

/** Which rung of the precision ladder produced a coordinate (useful for diagnostics & tests). */
enum class CoordinatePattern(val precision: CoordinatePrecision) {
    /** `!3d<lat>!4d<lng>` inside Google Maps `data=` blobs: the place pin itself. */
    PLACE_PIN(CoordinatePrecision.EXACT),
    /** `q=`, `query=`, `ll=`, `sll=`, `center=`, `daddr=`, `destination=` holding `lat,lng`. */
    QUERY_PARAM(CoordinatePrecision.EXACT),
    /** `/place/lat,lng`, `/search/lat,+lng`, or the last `/dir/…/lat,lng` segment. */
    PATH(CoordinatePrecision.EXACT),
    /** `geo:lat,lng` URI (RFC 5870). */
    GEO_URI(CoordinatePrecision.EXACT),
    /** `@lat,lng,17z`: the map viewport centre, NOT the place, so only approximate. */
    VIEWPORT(CoordinatePrecision.APPROXIMATE),
    /** Degrees-minutes-seconds, e.g. `15°20'06"N 76°27'36"E`. */
    DMS(CoordinatePrecision.EXACT),
    /** Decimal degrees with hemisphere letters, e.g. `15.335° N, 76.46° E`. */
    HEMISPHERE_DECIMAL(CoordinatePrecision.EXACT),
    /** A plain decimal pair in pasted text, e.g. `15.335, 76.46`. */
    DECIMAL_PAIR(CoordinatePrecision.EXACT)
}

/** A validated coordinate plus how it was found. */
data class ParsedCoordinate(
    val location: LatLng,
    val precision: CoordinatePrecision,
    val pattern: CoordinatePattern
)

/** Result of parsing a link or pasted text: an optional coordinate and an optional place name. */
data class MapLinkParseResult(
    val coordinate: ParsedCoordinate?,
    val placeNameHint: String?
)

/**
 * Pure, offline regex ladder that extracts a coordinate from a map link or pasted text.
 *
 * Rungs, tried in order of precision. The first rung that yields a valid coordinate wins:
 * 1. `!3d<lat>!4d<lng>` ([CoordinatePattern.PLACE_PIN]). The **last** pair is taken because
 *    Google puts the place pin last, after any intermediate/viewport pairs.
 * 2. Query parameters `q|query|ll|sll|center|daddr|destination = lat,lng` (optionally `loc:`).
 * 3. Path coordinates `/place/lat,lng`, `/search/lat,+lng`, last `/dir/…/lat,lng` segment.
 * 4. `geo:lat,lng` URIs (`geo:0,0?q=…` falls through to rung 2 because 0,0 is rejected).
 * 5. DMS `15°20'06"N 76°27'36"E` (seconds optional). This also covers Google's
 *    `/place/15°20'06.0"N+76°27'36.0"E/` URLs.
 * 6. Hemisphere decimals `15.335° N, 76.46° E`.
 * 7. `@lat,lng,<zoom>z` viewport centre ⇒ [CoordinatePrecision.APPROXIMATE]. It is ranked below
 *    every exact rung except rung 8, which must come after it so viewport numbers are never
 *    mistaken for a pasted pair.
 * 8. Plain decimal pairs `15.335, 76.46` (≥ 2 decimals each, to avoid matching prices/versions).
 *
 * Input is percent-decoded first (`%2C`, `%40`, `+` ⇒ space; double-encoding tolerated).
 * Validation: lat ∈ [-90, 90], lng ∈ [-180, 180], never (0, 0), finite numbers only.
 *
 * The parser never performs I/O and never throws for any input.
 */
object MapLinkCoordinateParser {

    /** Inputs longer than this are truncated before matching (defensive against huge pastes). */
    const val MAX_INPUT_CHARS = 8_192

    // Number fragments. "_D" variants require a fractional part.
    private const val LAT_D = """([+-]?\d{1,2}\.\d+)"""
    private const val LNG_D = """([+-]?\d{1,3}\.\d+)"""
    private const val LAT_I = """([+-]?\d{1,2}(?:\.\d+)?)"""
    private const val LNG_I = """([+-]?\d{1,3}(?:\.\d+)?)"""
    private const val SEP = """\s*,\s*"""
    private const val NUM_END = """(?![\d.])"""

    private val PLACE_PIN = Regex("""!3d${LAT_D}!4d${LNG_D}""")

    private val QUERY_PARAM = Regex(
        """(?:^|[?&#;\s])(?:q|query|ll|sll|center|daddr|destination)=(?:loc:)?\s*${LAT_D}${SEP}${LNG_D}${NUM_END}""",
        RegexOption.IGNORE_CASE
    )

    private val PATH_PLACE_SEARCH =
        Regex("""/(?:place|search)/(?:[^/?#]*/)*?\s*${LAT_D}${SEP}${LNG_D}${NUM_END}""")

    private val DIR_PATH = Regex("""/dir/([^?#]*)""")
    private val WHOLE_PAIR = Regex("""^\s*${LAT_D}${SEP}${LNG_D}\s*$""")

    private val GEO_URI = Regex("""geo:\s*${LAT_I}\s*,\s*${LNG_I}${NUM_END}""", RegexOption.IGNORE_CASE)

    private val VIEWPORT = Regex("""@${LAT_D}${SEP}${LNG_D}(?:,\s*\d+(?:\.\d+)?[a-z])?""")

    private val DMS = Regex(
        """(\d{1,3})\s*°\s*(\d{1,2}(?:\.\d+)?)\s*['′’]\s*(?:(\d{1,2}(?:\.\d+)?)\s*(?:"|″|”|''|′′)\s*)?([NSns])""" +
            """[\s,;]*""" +
            """(\d{1,3})\s*°\s*(\d{1,2}(?:\.\d+)?)\s*['′’]\s*(?:(\d{1,2}(?:\.\d+)?)\s*(?:"|″|”|''|′′)\s*)?([EWew])"""
    )

    private val HEMISPHERE_DECIMAL = Regex(
        """(?<![\w.])(\d{1,2}(?:\.\d+)?)\s*(°?)\s*([NS])[\s,;]+(\d{1,3}(?:\.\d+)?)\s*(°?)\s*([EW])(?![A-Za-z])"""
    )

    private val DECIMAL_PAIR = Regex(
        """(?<![\w.])([+-]?\d{1,2}\.\d{2,})(?:\s*[,;]\s*|\s+)([+-]?\d{1,3}\.\d{2,})${NUM_END}"""
    )

    // Place-name hint sources (matched against the RAW input, then decoded individually).
    private val PLACE_NAME = Regex("""/maps/place/([^/?#@]+)""")
    private val GEO_LABEL = Regex("""geo:[^\s?]*\?q=[^(\s]*\(([^)]{1,200})\)""", RegexOption.IGNORE_CASE)
    private val QUERY_TEXT = Regex("""[?&](?:q|query)=([^&#\s]+)""", RegexOption.IGNORE_CASE)
    private val FIRST_URL = Regex("""(?i)(?:https?://|geo:|\b(?:maps\.app\.goo\.gl|goo\.gl|g\.co|maps\.google\.|www\.google\.)\S*)""")

    /** Parses [input] (a link or free text) into an optional coordinate and optional name hint. */
    fun parse(input: String): MapLinkParseResult {
        val raw = if (input.length > MAX_INPUT_CHARS) input.take(MAX_INPUT_CHARS) else input
        return MapLinkParseResult(
            coordinate = extractCoordinateInternal(raw),
            placeNameHint = extractPlaceNameHint(raw)
        )
    }

    /** Runs only the coordinate ladder. */
    fun extractCoordinate(input: String): ParsedCoordinate? {
        val raw = if (input.length > MAX_INPUT_CHARS) input.take(MAX_INPUT_CHARS) else input
        return extractCoordinateInternal(raw)
    }

    private fun extractCoordinateInternal(raw: String): ParsedCoordinate? {
        if (raw.isBlank()) return null
        val s = LoopText.deepDecode(raw)

        // 1. Place pin: last valid pair wins.
        PLACE_PIN.findAll(s).toList().asReversed().forEach { m ->
            pair(m.groupValues[1], m.groupValues[2], CoordinatePattern.PLACE_PIN)?.let { return it }
        }
        // 2. Explicit query parameters.
        QUERY_PARAM.findAll(s).forEach { m ->
            pair(m.groupValues[1], m.groupValues[2], CoordinatePattern.QUERY_PARAM)?.let { return it }
        }
        // 3a. /place/… and /search/… path coordinates.
        PATH_PLACE_SEARCH.findAll(s).forEach { m ->
            pair(m.groupValues[1], m.groupValues[2], CoordinatePattern.PATH)?.let { return it }
        }
        // 3b. /dir/…: the destination is the last meaningful segment.
        DIR_PATH.find(s)?.let { m ->
            val last = m.groupValues[1].split('/')
                .map { it.trim() }
                .lastOrNull { it.isNotEmpty() && !it.startsWith("@") && !it.startsWith("data=") }
            if (last != null) {
                WHOLE_PAIR.find(last)?.let { pm ->
                    pair(pm.groupValues[1], pm.groupValues[2], CoordinatePattern.PATH)?.let { return it }
                }
            }
        }
        // 4. geo: URIs.
        GEO_URI.findAll(s).forEach { m ->
            pair(m.groupValues[1], m.groupValues[2], CoordinatePattern.GEO_URI)?.let { return it }
        }
        // 5. DMS.
        DMS.findAll(s).forEach { m -> dms(m)?.let { return it } }
        // 6. Hemisphere decimals.
        HEMISPHERE_DECIMAL.findAll(s).forEach { m -> hemisphereDecimal(m)?.let { return it } }
        // 7. Viewport centre (approximate).
        VIEWPORT.findAll(s).forEach { m ->
            pair(m.groupValues[1], m.groupValues[2], CoordinatePattern.VIEWPORT)?.let { return it }
        }
        // 8. Plain decimal pairs.
        DECIMAL_PAIR.findAll(s).forEach { m ->
            pair(m.groupValues[1], m.groupValues[2], CoordinatePattern.DECIMAL_PAIR)?.let { return it }
        }
        return null
    }

    /**
     * Extracts a human place-name hint (used for labels and the Nominatim fallback), in order:
     * `/maps/place/<name>/`, a `geo:…?q=lat,lng(<label>)` label, a non-coordinate `q=`/`query=`
     * value, or the first line of text preceding the URL in a share-intent payload.
     */
    fun extractPlaceNameHint(input: String): String? {
        val raw = if (input.length > MAX_INPUT_CHARS) input.take(MAX_INPUT_CHARS) else input
        PLACE_NAME.find(raw)?.let { m -> usableHint(m.groupValues[1])?.let { return it } }
        GEO_LABEL.find(raw)?.let { m -> usableHint(m.groupValues[1])?.let { return it } }
        QUERY_TEXT.findAll(raw).forEach { m -> usableHint(m.groupValues[1])?.let { return it } }
        val urlStart = FIRST_URL.find(raw)?.range?.first
        if (urlStart != null && urlStart > 0) {
            val prefixLine = raw.substring(0, urlStart)
                .lines()
                .map { it.trim().trimEnd(':', '-', '–', '—', '|', ',').trim() }
                .firstOrNull { it.isNotEmpty() }
            if (prefixLine != null) usableHint(prefixLine, decode = false)?.let { return it }
        }
        return null
    }

    /**
     * Hard validation shared by every rung and by other components.
     * @return a [LatLng] or null when out of range, non-finite, or the (0, 0) "null island".
     */
    fun validate(lat: Double, lng: Double): LatLng? {
        if (!lat.isFinite() || !lng.isFinite()) return null
        if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) return null
        if (abs(lat) < 1e-9 && abs(lng) < 1e-9) return null
        return LatLng(lat, lng)
    }

    // ------------------------------------------------------------------------------------------

    private fun pair(latText: String, lngText: String, pattern: CoordinatePattern): ParsedCoordinate? {
        val lat = latText.toDoubleOrNull() ?: return null
        val lng = lngText.toDoubleOrNull() ?: return null
        val ll = validate(lat, lng) ?: return null
        return ParsedCoordinate(ll, pattern.precision, pattern)
    }

    private fun dms(m: MatchResult): ParsedCoordinate? {
        val g = m.groupValues
        val lat = dmsToDecimal(g[1], g[2], g[3], maxDegrees = 90) ?: return null
        val lng = dmsToDecimal(g[5], g[6], g[7], maxDegrees = 180) ?: return null
        val signedLat = if (g[4].equals("S", ignoreCase = true)) -lat else lat
        val signedLng = if (g[8].equals("W", ignoreCase = true)) -lng else lng
        val ll = validate(signedLat, signedLng) ?: return null
        return ParsedCoordinate(ll, CoordinatePattern.DMS.precision, CoordinatePattern.DMS)
    }

    private fun dmsToDecimal(deg: String, min: String, sec: String, maxDegrees: Int): Double? {
        val d = deg.toIntOrNull() ?: return null
        val mi = min.toDoubleOrNull() ?: return null
        val se = if (sec.isEmpty()) 0.0 else sec.toDoubleOrNull() ?: return null
        if (d > maxDegrees || mi >= 60.0 || se >= 60.0) return null
        return d + mi / 60.0 + se / 3600.0
    }

    private fun hemisphereDecimal(m: MatchResult): ParsedCoordinate? {
        val g = m.groupValues
        // Require either a fractional part or a degree sign so "12 N, 45 E" style noise is ignored.
        val latOk = g[1].contains('.') || g[2].isNotEmpty()
        val lngOk = g[4].contains('.') || g[5].isNotEmpty()
        if (!latOk || !lngOk) return null
        val lat = g[1].toDoubleOrNull() ?: return null
        val lng = g[4].toDoubleOrNull() ?: return null
        val ll = validate(if (g[3] == "S") -lat else lat, if (g[6] == "W") -lng else lng) ?: return null
        return ParsedCoordinate(ll, CoordinatePattern.HEMISPHERE_DECIMAL.precision, CoordinatePattern.HEMISPHERE_DECIMAL)
    }

    private fun usableHint(rawValue: String, decode: Boolean = true): String? {
        val decoded = if (decode) LoopText.deepDecode(rawValue) else rawValue
        val cleaned = LoopText.clean(decoded) ?: return null
        // Reject values that are themselves coordinates or have no letters at all.
        if (extractCoordinateInternal(cleaned) != null) return null
        if (cleaned.none { it.isLetter() }) return null
        return cleaned
    }
}
