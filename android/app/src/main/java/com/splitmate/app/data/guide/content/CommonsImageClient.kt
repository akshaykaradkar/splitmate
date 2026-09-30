package com.splitmate.app.data.guide.content

import com.splitmate.app.data.guide.CommonsImage
import org.json.JSONObject

/**
 * Wikimedia Commons `imageinfo` client: one call returns the scaled thumb URL plus the per-file
 * credit (Artist, LicenseShortName, LicenseUrl) and the file description page.
 *
 * `action=query&prop=imageinfo&iiprop=url|extmetadata&iiurlwidth=<w>` with standard widths only
 * ([WIDTH_HERO] 1280, [WIDTH_METERED] 640, [WIDTH_THUMB] 320) to stay on cached thumbnail steps.
 * Up to [MAX_TITLES_PER_CALL] files are batched per request.
 */
class CommonsImageClient(private val http: PoliteHttpClient) {

    fun fetch(fileName: String, width: Int = WIDTH_HERO): CommonsImage? =
        fetchMany(listOf(fileName), width)[normalizeFileName(fileName)]

    /** Returns images keyed by normalised file name (spaces, no `File:` prefix). Missing files are absent. */
    fun fetchMany(fileNames: List<String>, width: Int): Map<String, CommonsImage> {
        val names = fileNames.map { normalizeFileName(it) }.filter { it.isNotEmpty() }.distinct()
        if (names.isEmpty()) return emptyMap()
        val out = LinkedHashMap<String, CommonsImage>()
        names.chunked(MAX_TITLES_PER_CALL).forEach { chunk ->
            out.putAll(parseImageInfo(http.get(imageInfoUrl(chunk, width)), width))
        }
        return out
    }

    companion object {
        const val WIDTH_HERO = 1280
        const val WIDTH_METERED = 640
        const val WIDTH_THUMB = 320
        val STANDARD_WIDTHS = setOf(WIDTH_THUMB, WIDTH_METERED, WIDTH_HERO)
        const val MAX_TITLES_PER_CALL = 50
        private const val MAX_CREDIT_CHARS = 200

        /** Snap any requested width to the nearest standard step (never above 1280). */
        fun standardWidth(requested: Int): Int = STANDARD_WIDTHS.minByOrNull { kotlin.math.abs(it - requested) }!!

        fun normalizeFileName(raw: String): String {
            var s = raw.trim().replace('_', ' ')
            val colon = s.indexOf(':')
            if (colon in 1..6) {
                val ns = s.substring(0, colon).trim().lowercase()
                if (ns == "file" || ns == "image") s = s.substring(colon + 1).trim()
            }
            s = s.replace(Regex("\\s+"), " ")
            return if (s.isEmpty()) "" else s[0].uppercaseChar() + s.substring(1)
        }

        fun imageInfoUrl(fileNames: List<String>, width: Int): String = ApiUrl.mediaWiki(
            ApiUrl.COMMONS_API,
            listOf(
                "action" to "query",
                "titles" to fileNames.joinToString("|") { "File:$it" },
                "prop" to "imageinfo",
                "iiprop" to "url|extmetadata",
                "iiurlwidth" to standardWidth(width).toString(),
                "iiextmetadatafilter" to "Artist|LicenseShortName|LicenseUrl|Credit"
            )
        )

        /** Parses an imageinfo response (formatversion 2, tolerant of v1 `pages` objects). */
        fun parseImageInfo(json: String, requestedWidth: Int): Map<String, CommonsImage> {
            val root = WikidataActionApiClient.parseRoot(json)
            val query = root.optJSONObject("query") ?: return emptyMap()
            val pages = ArrayList<JSONObject>()
            query.optJSONArray("pages")?.let { arr -> for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { pages += it } }
            query.optJSONObject("pages")?.let { obj -> obj.keys().forEach { k -> obj.optJSONObject(k)?.let { pages += it } } }
            val out = LinkedHashMap<String, CommonsImage>()
            for (page in pages) {
                if (page.has("missing") || page.has("invalid")) continue
                val title = page.optStringOrNull("title") ?: continue
                val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                val file = normalizeFileName(title)
                val thumb = httpsUrl(info.optStringOrNull("thumburl") ?: info.optStringOrNull("url")) ?: continue
                val width = info.optInt("thumbwidth", info.optInt("width", standardWidth(requestedWidth)))
                val meta = info.optJSONObject("extmetadata")
                out[file] = CommonsImage(
                    fileName = file,
                    thumbUrl = thumb,
                    width = width,
                    artist = metaText(meta, "Artist") ?: metaText(meta, "Credit"),
                    license = metaText(meta, "LicenseShortName"),
                    licenseUrl = httpsUrl(meta?.optJSONObject("LicenseUrl")?.optStringOrNull("value")),
                    sourceUrl = httpsUrl(info.optStringOrNull("descriptionurl"))
                        ?: "https://commons.wikimedia.org/wiki/File:" + ApiUrl.encode(file.replace(' ', '_'))
                )
            }
            return out
        }

        private fun metaText(meta: JSONObject?, key: String): String? {
            val raw = meta?.optJSONObject(key)?.optStringOrNull("value") ?: return null
            val plain = WikiTextSanitizer.toPlainText(raw)
            if (plain.isEmpty()) return null
            return WikiTextSanitizer.truncate(plain, MAX_CREDIT_CHARS).text
        }

        /** Protocol-relative → https; anything not https is rejected. */
        internal fun httpsUrl(raw: String?): String? {
            val s = raw?.trim().orEmpty()
            if (s.isEmpty()) return null
            val u = if (s.startsWith("//")) "https:$s" else if (s.startsWith("http://")) "https://" + s.removePrefix("http://") else s
            return if (u.startsWith("https://") && !u.any { it.isWhitespace() }) u else null
        }
    }
}
