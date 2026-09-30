package com.splitmate.app.data.guide.content

import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource

/** A Wikivoyage listing converted to a contract [Place], plus parse metadata. */
data class ParsedListing(
    val place: Place,
    /** The blurb was cut to [WikiTextSanitizer.MAX_BLURB_CHARS]; UI labels it "excerpt". */
    val blurbTruncated: Boolean,
    val alt: String?,
    /** Commons file from the `image=` param (normalised, no `File:` prefix). */
    val imageFile: String?,
    /** The level-2 section heading the listing appeared under (e.g. "See"). */
    val section: String?,
    /** The raw template name (see/do/eat/drink/sleep/listing). */
    val template: String
)

/** Output of [WikivoyageListingParser.parse]. */
data class ParsedArticle(
    val title: String,
    val revisionId: Long,
    val listings: List<ParsedListing>,
    /** All section headings in document order (level ≥ 2), sanitised. */
    val headings: List<String>
) {
    val places: List<Place> get() = listings.map { it.place }
    val isOutline: Boolean get() = listings.isEmpty()
}

/**
 * Parses Wikivoyage wikitext into See/Do/Eat/Sleep [Place]s.
 *
 * Pipeline (audit §2.2): strip comments → split by headings (level-2 decides the kind, deeper
 * subsections inherit it) → balanced-brace scan for listing templates → split named params on
 * top-level `|` only (nested `{{…}}` and `[[…|…]]` are respected, params may span lines) →
 * sanitise text → build ids `wv:<kind>:<slug>` (deterministic `-2`, `-3` suffixes on collisions).
 *
 * `{{drink}}` / `==Drink==` map to [PlaceKind.EAT]; `{{buy}}` and `{{go}}` are ignored.
 * SLEEP listings are retained (the stay picker uses them). Never fabricates listings from prose.
 */
object WikivoyageListingParser {

    const val MAX_FIELD_CHARS = 160
    private const val MAX_LISTINGS = 200

    private val HEADING = Regex("^(={2,6})\\s*(.+?)\\s*\\1\\s*$")
    private val LISTING_TEMPLATES = setOf("see", "do", "eat", "drink", "sleep", "buy", "go", "listing")

    fun parse(title: String, revisionId: Long, wikitext: String): ParsedArticle {
        val text = WikiTextSanitizer.stripComments(wikitext.replace("\r\n", "\n"))
        val headings = ArrayList<String>()
        val listings = ArrayList<ParsedListing>()
        val usedIds = HashMap<String, Int>()

        for (section in splitSections(text)) {
            section.heading?.let { headings += it }
            for (tpl in scanTemplates(section.body)) {
                if (listings.size >= MAX_LISTINGS) break
                val listing = toListing(tpl, section, usedIds) ?: continue
                listings += listing
            }
        }
        return ParsedArticle(title, revisionId, listings, headings)
    }

    /** Maps a heading (e.g. "See", " Eat and drink ") to a kind; null for non-listing sections. */
    fun kindForHeading(heading: String): PlaceKind? {
        val h = heading.trim().lowercase()
        return when {
            h == "see" || h.startsWith("see ") || h == "sights" -> PlaceKind.SEE
            h == "do" || h.startsWith("do ") || h == "activities" -> PlaceKind.DO
            h == "eat" || h.startsWith("eat") || h == "drink" || h.startsWith("drink") -> PlaceKind.EAT
            h == "sleep" || h.startsWith("sleep") || h == "accommodation" -> PlaceKind.SLEEP
            else -> null
        }
    }

    fun kindForTemplate(name: String): PlaceKind? = when (name) {
        "see" -> PlaceKind.SEE
        "do" -> PlaceKind.DO
        "eat", "drink" -> PlaceKind.EAT
        "sleep" -> PlaceKind.SLEEP
        else -> null
    }

    // ------------------------------------------------------------------------------------------

    internal data class Section(val heading: String?, val level2: String?, val kind: PlaceKind?, val body: String)

    internal data class Template(val name: String, val params: Map<String, String>)

    internal fun splitSections(text: String): List<Section> {
        val out = ArrayList<Section>()
        var heading: String? = null
        var level2: String? = null
        var kind: PlaceKind? = null
        val body = StringBuilder()
        var depth = 0 // brace depth: headings inside multi-line templates are not headings
        for (line in text.split('\n')) {
            val m = if (depth == 0) HEADING.matchEntire(line) else null
            if (m != null) {
                out += Section(heading, level2, kind, body.toString())
                body.setLength(0)
                val level = m.groupValues[1].length
                val h = WikiTextSanitizer.toPlainText(m.groupValues[2])
                heading = h
                if (level == 2) {
                    level2 = h
                    kind = kindForHeading(h)
                }
            } else {
                body.append(line).append('\n')
                depth = (depth + countOf(line, "{{") - countOf(line, "}}")).coerceAtLeast(0)
            }
        }
        out += Section(heading, level2, kind, body.toString())
        return out
    }

    private fun countOf(s: String, token: String): Int {
        var n = 0
        var i = s.indexOf(token)
        while (i >= 0) {
            n++
            i = s.indexOf(token, i + token.length)
        }
        return n
    }

    /** Balanced-brace scan returning top-level listing templates in document order. */
    internal fun scanTemplates(body: String): List<Template> {
        val out = ArrayList<Template>()
        var i = 0
        while (true) {
            val start = body.indexOf("{{", i)
            if (start < 0) break
            val end = matchingClose(body, start)
            if (end < 0) {
                // Unbalanced: try to salvage up to the end of text.
                parseTemplate(body.substring(start + 2))?.let { if (it.name in LISTING_TEMPLATES) out += it }
                break
            }
            val inner = body.substring(start + 2, end)
            parseTemplate(inner)?.let { if (it.name in LISTING_TEMPLATES) out += it }
            i = end + 2
        }
        return out
    }

    /** Index of the `}}` closing the `{{` at [start], or -1. Tracks nested `{{`/`}}`. */
    private fun matchingClose(s: String, start: Int): Int {
        var depth = 0
        var i = start
        while (i < s.length - 1) {
            if (s[i] == '{' && s[i + 1] == '{') {
                depth++; i += 2; continue
            }
            if (s[i] == '}' && s[i + 1] == '}') {
                depth--
                if (depth == 0) return i
                i += 2; continue
            }
            i++
        }
        return -1
    }

    /** Splits a template body on top-level `|` and returns its lowercase name + named params. */
    internal fun parseTemplate(inner: String): Template? {
        val parts = splitTopLevel(inner)
        if (parts.isEmpty()) return null
        val name = parts[0].trim().lowercase().replace('_', ' ').removePrefix("template:").trim()
        if (name.isEmpty()) return null
        val params = LinkedHashMap<String, String>()
        for (p in parts.drop(1)) {
            val eq = topLevelEquals(p)
            if (eq <= 0) continue
            val key = p.substring(0, eq).trim().lowercase()
            val value = p.substring(eq + 1).trim()
            if (key.isNotEmpty() && value.isNotEmpty()) params[key] = value
        }
        return Template(name, params)
    }

    private fun splitTopLevel(s: String): List<String> {
        val parts = ArrayList<String>()
        var brace = 0
        var bracket = 0
        val cur = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val next = if (i + 1 < s.length) s[i + 1] else '\u0000'
            when {
                c == '{' && next == '{' -> { brace++; cur.append("{{"); i += 2; continue }
                c == '}' && next == '}' -> { brace = (brace - 1).coerceAtLeast(0); cur.append("}}"); i += 2; continue }
                c == '[' && next == '[' -> { bracket++; cur.append("[["); i += 2; continue }
                c == ']' && next == ']' -> { bracket = (bracket - 1).coerceAtLeast(0); cur.append("]]"); i += 2; continue }
                c == '|' && brace == 0 && bracket == 0 -> { parts += cur.toString(); cur.setLength(0) }
                else -> cur.append(c)
            }
            i++
        }
        parts += cur.toString()
        return parts
    }

    private fun topLevelEquals(p: String): Int {
        var brace = 0
        var bracket = 0
        var i = 0
        while (i < p.length) {
            val c = p[i]
            val next = if (i + 1 < p.length) p[i + 1] else '\u0000'
            if (c == '{' && next == '{') { brace++; i += 2; continue }
            if (c == '}' && next == '}') { brace--; i += 2; continue }
            if (c == '[' && next == '[') { bracket++; i += 2; continue }
            if (c == ']' && next == ']') { bracket--; i += 2; continue }
            if (c == '=' && brace == 0 && bracket == 0) return i
            i++
        }
        return -1
    }

    private fun toListing(tpl: Template, section: Section, usedIds: MutableMap<String, Int>): ParsedListing? {
        val kind: PlaceKind = when (tpl.name) {
            "buy", "go" -> return null
            "listing" -> tpl.params["type"]?.trim()?.lowercase()?.let { t ->
                if (t == "buy" || t == "go") return null
                kindForTemplate(t)
            } ?: section.kind ?: return null
            else -> kindForTemplate(tpl.name) ?: return null
        }
        val name = field(tpl.params["name"]) ?: return null
        val rawContent = tpl.params["content"] ?: tpl.params["description"]
        val blurbPlain = WikiTextSanitizer.toPlainText(rawContent)
        val blurb = WikiTextSanitizer.truncate(blurbPlain)
        val qid = tpl.params["wikidata"]?.trim()?.uppercase()?.takeIf { WikidataActionApiClient.QID.matches(it) }
        val imageFile = tpl.params["image"]?.let { WikiTextSanitizer.stripComments(it) }
            ?.let { CommonsImageClient.normalizeFileName(it) }?.takeIf { it.isNotEmpty() && !it.contains('{') && !it.contains('[') }

        return ParsedListing(
            place = Place(
                id = uniqueId(kind, name, usedIds),
                kind = kind,
                name = name,
                location = parseLatLng(tpl.params["lat"], tpl.params["long"] ?: tpl.params["lon"] ?: tpl.params["lng"]),
                qid = qid,
                blurb = blurb.text.ifEmpty { null },
                hours = field(tpl.params["hours"]),
                price = field(tpl.params["price"]),
                address = field(tpl.params["address"]),
                source = PlaceSource.WIKIVOYAGE,
                image = null,
                distanceKmFromCenter = null
            ),
            blurbTruncated = blurb.truncated,
            alt = field(tpl.params["alt"]),
            imageFile = imageFile,
            section = section.level2,
            template = tpl.name
        )
    }

    private fun field(raw: String?): String? {
        val plain = WikiTextSanitizer.toPlainText(raw)
        if (plain.isEmpty()) return null
        return WikiTextSanitizer.truncate(plain, MAX_FIELD_CHARS).text
    }

    /** Parses decimal lat/long; returns null when missing, malformed, out of range or (0,0). */
    fun parseLatLng(latRaw: String?, lngRaw: String?): LatLng? {
        val lat = parseCoordinate(latRaw) ?: return null
        val lng = parseCoordinate(lngRaw) ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        if (lat == 0.0 && lng == 0.0) return null
        return LatLng(lat, lng)
    }

    private fun parseCoordinate(raw: String?): Double? {
        val s = WikiTextSanitizer.stripComments(raw ?: return null).trim()
            .replace('−', '-').replace(" ", "")
        if (s.isEmpty()) return null
        return s.toDoubleOrNull()?.takeIf { !it.isNaN() && !it.isInfinite() }
    }

    private fun uniqueId(kind: PlaceKind, name: String, used: MutableMap<String, Int>): String {
        val slug = WikiTextSanitizer.slugify(name).ifEmpty {
            "listing-" + Integer.toHexString(name.hashCode())
        }
        val base = "wv:${kind.name.lowercase()}:$slug"
        val n = (used[base] ?: 0) + 1
        used[base] = n
        return if (n == 1) base else "$base-$n"
    }
}
