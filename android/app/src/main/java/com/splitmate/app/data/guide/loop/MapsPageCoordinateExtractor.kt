package com.splitmate.app.data.guide.loop

import com.splitmate.app.data.guide.CoordinatePrecision

/**
 * v2.3.5 (#3) HTML fallback for [ShortLinkResolver]: extracts a coordinate (and optionally a place
 * name) from the first ~256 KB of a Google Maps page when none of the redirect URLs carried one.
 *
 * Patterns only (no HTML/JS parsing, no script execution), most precise first:
 * 1. Static-map `markers=lat,lng` (og:image / itemprop=image): the place marker ⇒ EXACT.
 * 2. `[null,null,lat,lng]` place tuples in the inline app state ⇒ EXACT.
 * 3. `!3d<lat>!4d<lng>` inside embedded place URLs ⇒ EXACT.
 * 4. Static-map `center=lat,lng` ⇒ APPROXIMATE.
 * 5. `APP_INITIALIZATION_STATE=[[[zoom,lng,lat]` viewport (note lng before lat) ⇒ APPROXIMATE.
 * 6. `ll=lat,lng` in embedded links ⇒ APPROXIMATE.
 * 7. `@lat,lng,<zoom>z` viewport ⇒ APPROXIMATE.
 *
 * Place name: og:title / itemprop=name / `<title>` first; when those are generic ("Google Maps"),
 * only proper URL fields (og:url, canonical link, an embedded `/maps/place/<name>` URL) are run
 * through the URL hint parser. Raw HTML is never handed to the hint parser.
 *
 * Gated by `GuideFeatureFlags.shortLinkBodyParseEnabled`. Never throws; never logs page content.
 */
object MapsPageCoordinateExtractor {

    /** Maximum number of body bytes the resolver reads for this fallback. */
    const val MAX_BODY_BYTES = 256 * 1024

    private const val LAT = """(-?\d{1,2}\.\d{3,})"""
    private const val LNG = """(-?\d{1,3}\.\d{3,})"""

    private val MARKERS = Regex("""markers=(?:[^&"'|<>\s]*\|)*$LAT,\s*$LNG""")
    /**
     * v2.4.0 rc17: Google Maps `/maps/embed?origin=mfe&pb=!1m2!2m1!1s…` `initEmbed` place tuple:
     * `["0x3bea0d22e1412a97:0x9395d694c999e8da","Shop No 504, …",[16.9983921,73.3569574],"1063…"],"Hemprabha …"`
     */
    private val EMBED_ENTITY = Regex(
        """\[\s*"0x[0-9a-fA-F]+:0x[0-9a-fA-F]+"\s*,\s*"(?:[^"\\]|\\.)*"\s*,\s*\[\s*$LAT\s*,\s*$LNG\s*](?:\s*,\s*"[0-9]+")?\s*]\s*(?:,\s*"([^"\\]{1,200})")?"""
    )
    private val NULL_TUPLE = Regex("""\[\s*null\s*,\s*null\s*,\s*$LAT\s*,\s*$LNG\s*]""")
    private val PIN = Regex("""!3d$LAT!4d$LNG""")
    private val CENTER = Regex("""[?&;]center=$LAT,\s*$LNG""")
    private val APP_STATE = Regex("""APP_INITIALIZATION_STATE\s*=\s*\[\s*\[\s*\[\s*-?\d+(?:\.\d+)?\s*,\s*$LNG\s*,\s*$LAT""")
    private val LL = Regex("""[?&;]s?ll=$LAT,\s*$LNG""")
    private val VIEWPORT = Regex("""@$LAT,$LNG,\d+(?:\.\d+)?z""")

    private val OG_TITLE_A = Regex("""<meta[^>]{0,200}?content="([^"]{1,300})"[^>]{0,200}?property="og:title"""", RegexOption.IGNORE_CASE)
    private val OG_TITLE_B = Regex("""<meta[^>]{0,200}?property="og:title"[^>]{0,200}?content="([^"]{1,300})"""", RegexOption.IGNORE_CASE)
    private val ITEMPROP_NAME = Regex("""<meta[^>]{0,200}?content="([^"]{1,300})"[^>]{0,200}?itemprop="name"""", RegexOption.IGNORE_CASE)
    private val TITLE = Regex("""<title>([^<]{1,300})</title>""", RegexOption.IGNORE_CASE)

    // URL-valued fields only (v2.3.5 #3): each match is a single https URL token.
    private val OG_URL_A = Regex("""<meta[^>]{0,200}?content="(https://[^"<>\s]{1,600})"[^>]{0,200}?property="og:url"""", RegexOption.IGNORE_CASE)
    private val OG_URL_B = Regex("""<meta[^>]{0,200}?property="og:url"[^>]{0,200}?content="(https://[^"<>\s]{1,600})"""", RegexOption.IGNORE_CASE)
    private val CANONICAL_A = Regex("""<link[^>]{0,200}?rel="canonical"[^>]{0,200}?href="(https://[^"<>\s]{1,600})"""", RegexOption.IGNORE_CASE)
    private val CANONICAL_B = Regex("""<link[^>]{0,200}?href="(https://[^"<>\s]{1,600})"[^>]{0,200}?rel="canonical"""", RegexOption.IGNORE_CASE)
    private val EMBEDDED_PLACE_URL =
        Regex("""https://(?:www\.)?google\.[a-z.]{2,10}/maps/place/[^\s"'<>\\]{1,300}""", RegexOption.IGNORE_CASE)
    private val HINT_JUNK = Regex("""[<>="\\{}\[\]]""")
    private const val MAX_EMBEDDED_URLS = 5

    /** Coordinate (or null) and an optional page-derived place name. */
    data class Result(val coordinate: ParsedCoordinate?, val placeName: String?)

    fun extract(html: String?): Result {
        if (html.isNullOrBlank()) return Result(null, null)
        return try {
            val s = normalize(if (html.length > MAX_BODY_BYTES) html.take(MAX_BODY_BYTES) else html)
            Result(extractCoordinate(s), extractName(s))
        } catch (_: Exception) {
            Result(null, null)
        }
    }

    /**
     * True when [html] already yields an EXACT coordinate. The body reader may stop early only
     * then; APPROXIMATE hints in `<head>` are not enough because the EXACT tuples and
     * `APP_INITIALIZATION_STATE` usually live further down in `<body>`.
     */
    fun hasExactCoordinate(html: String?): Boolean {
        if (html.isNullOrBlank()) return false
        return try {
            val s = normalize(if (html.length > MAX_BODY_BYTES) html.take(MAX_BODY_BYTES) else html)
            extractCoordinate(s)?.precision == CoordinatePrecision.EXACT
        } catch (_: Exception) {
            false
        }
    }

    /**
     * v2.4.0 rc17: true when [html] either already contains an EXACT coordinate OR is Google's
     * coordinate-free mobile `mapslite` SPA shell (`<title></title>`, `maps_512dp.png`, and no
     * `staticmap` / `APP_INITIALIZATION_STATE` in the header). Stopping early on `mapslite` avoids
     * downloading 256 KB of JS/font boilerplate before the `/maps/embed` pin probe runs.
     */
    fun shouldStopReading(html: String?): Boolean {
        if (html.isNullOrBlank()) return false
        if (hasExactCoordinate(html)) return true
        return isCoordinateFreeMapsLite(html)
    }

    internal fun isCoordinateFreeMapsLite(html: String): Boolean =
        html.contains("mapslite") &&
            html.contains("maps_512dp.png") &&
            !html.contains("staticmap") &&
            !html.contains("APP_INITIALIZATION_STATE") &&
            !html.contains("initEmbed")

    private fun extractCoordinate(s: String): ParsedCoordinate? {
        firstValid(MARKERS, s, CoordinatePrecision.EXACT, CoordinatePattern.QUERY_PARAM)?.let { return it }
        firstValid(EMBED_ENTITY, s, CoordinatePrecision.EXACT, CoordinatePattern.PLACE_PIN)?.let { return it }
        firstValid(NULL_TUPLE, s, CoordinatePrecision.EXACT, CoordinatePattern.PLACE_PIN)?.let { return it }
        firstValid(PIN, s, CoordinatePrecision.EXACT, CoordinatePattern.PLACE_PIN)?.let { return it }
        firstValid(CENTER, s, CoordinatePrecision.APPROXIMATE, CoordinatePattern.QUERY_PARAM)?.let { return it }
        APP_STATE.findAll(s).forEach { m ->
            // Group 1 is the longitude, group 2 the latitude.
            coord(m.groupValues[2], m.groupValues[1], CoordinatePrecision.APPROXIMATE, CoordinatePattern.VIEWPORT)?.let { return it }
        }
        firstValid(LL, s, CoordinatePrecision.APPROXIMATE, CoordinatePattern.QUERY_PARAM)?.let { return it }
        firstValid(VIEWPORT, s, CoordinatePrecision.APPROXIMATE, CoordinatePattern.VIEWPORT)?.let { return it }
        return null
    }

    private fun firstValid(
        regex: Regex,
        s: String,
        precision: CoordinatePrecision,
        pattern: CoordinatePattern
    ): ParsedCoordinate? {
        regex.findAll(s).forEach { m ->
            coord(m.groupValues[1], m.groupValues[2], precision, pattern)?.let { return it }
        }
        return null
    }

    private fun coord(latText: String, lngText: String, precision: CoordinatePrecision, pattern: CoordinatePattern): ParsedCoordinate? {
        val lat = latText.toDoubleOrNull() ?: return null
        val lng = lngText.toDoubleOrNull() ?: return null
        val ll = MapLinkCoordinateParser.validate(lat, lng) ?: return null
        return ParsedCoordinate(ll, precision, pattern)
    }

    private fun extractName(s: String): String? {
        for (r in listOf(OG_TITLE_A, OG_TITLE_B, ITEMPROP_NAME, TITLE)) {
            val raw = r.find(s)?.groupValues?.get(1) ?: continue
            val name = cleanTitle(raw) ?: continue
            return name
        }
        EMBED_ENTITY.find(s)?.groupValues?.getOrNull(3)?.takeIf { it.isNotBlank() }?.let { raw ->
            (NominatimGeocoder.placeName(raw) ?: cleanTitle(raw))?.let { return it }
        }
        return urlFieldHint(s)
    }

    /**
     * Generic-title fallback: a place-name hint from proper URL fields only (og:url, canonical
     * link, an embedded `google.<tld>/maps/place/<name>` URL). Each candidate is one https URL
     * token, so markup such as `<link href=` can never become a hint.
     */
    private fun urlFieldHint(s: String): String? {
        val fields = sequenceOf(OG_URL_A, OG_URL_B, CANONICAL_A, CANONICAL_B)
            .mapNotNull { it.find(s)?.groupValues?.get(1) }
        val embedded = EMBEDDED_PLACE_URL.findAll(s).take(MAX_EMBEDDED_URLS).map { it.value }
        for (candidate in fields + embedded) {
            val url = candidate.trim()
            if (!url.startsWith("https://", ignoreCase = true)) continue
            if (url.any { it.isWhitespace() || it == '<' || it == '>' || it == '"' }) continue
            val hint = MapLinkCoordinateParser.extractPlaceNameHint(url) ?: continue
            if (HINT_JUNK.containsMatchIn(hint) || hint.contains("://")) continue
            if (hint.none { it.isLetter() } || hint.equals("Google Maps", ignoreCase = true)) continue
            return hint
        }
        return null
    }

    private fun cleanTitle(raw: String): String? {
        val unescaped = raw
            .replace("&amp;", "&").replace("&#39;", "'").replace("&quot;", "\"")
            .replace("&lt;", "<").replace("&gt;", ">")
        val trimmed = unescaped
            .removeSuffix(" - Google Maps").removeSuffix(" – Google Maps")
            .trim()
        if (trimmed.isEmpty() || trimmed.equals("Google Maps", ignoreCase = true)) return null
        val cleaned = LoopText.clean(trimmed, maxChars = 200) ?: return null
        if (cleaned.none { it.isLetter() }) return null
        return cleaned
    }

    /** Cheap, targeted unescaping (the page is too big for a full multi-round decode). */
    private fun normalize(s: String): String = s
        .replace("\\u0026", "&").replace("\\u003d", "=").replace("\\u003c", "<").replace("\\u003e", ">")
        .replace("\\/", "/")
        .replace("&amp;", "&")
        .replace("%2C", ",").replace("%2c", ",")
        .replace("%21", "!")
        .replace("%7C", "|").replace("%7c", "|")
        .replace("%40", "@")
}
