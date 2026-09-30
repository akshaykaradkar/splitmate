package com.splitmate.app.data.guide.content

import org.json.JSONObject

/** Raw article wikitext at a specific revision. */
data class WikivoyageArticle(val title: String, val revisionId: Long, val wikitext: String)

/**
 * Fetches Wikivoyage article wikitext through the MediaWiki Action API (`action=parse`).
 * The REST `mobile-sections` endpoint was decommissioned in 2023 (audit §2.2).
 *
 * - Latest: `action=parse&page=<title>&prop=wikitext|revid&redirects=1`.
 * - Pinned (decision #19): `action=parse&oldid=<rev>&prop=wikitext|revid` (`page` and `oldid`
 *   are mutually exclusive in API:Parse).
 */
class WikivoyageClient(private val http: PoliteHttpClient) {

    /** Returns null when the article (or pinned revision) does not exist. */
    fun fetch(title: String, pinnedRevisionId: Long? = null): WikivoyageArticle? {
        val url = if (pinnedRevisionId != null && pinnedRevisionId > 0) revisionUrl(pinnedRevisionId) else pageUrl(title)
        return parseResponse(http.get(url), title)
    }

    companion object {
        const val ARTICLE_BASE = "https://en.wikivoyage.org/wiki/"
        private val MISSING_CODES = setOf("missingtitle", "nosuchrevid", "invalidtitle", "missingcontent", "nosuchpageid")

        fun pageUrl(title: String): String = ApiUrl.mediaWiki(
            ApiUrl.WIKIVOYAGE_API,
            listOf("action" to "parse", "page" to title, "prop" to "wikitext|revid", "redirects" to "1")
        )

        fun revisionUrl(revisionId: Long): String = ApiUrl.mediaWiki(
            ApiUrl.WIKIVOYAGE_API,
            listOf("action" to "parse", "oldid" to revisionId.toString(), "prop" to "wikitext|revid")
        )

        fun articleUrl(title: String): String = ARTICLE_BASE + ApiUrl.encode(title.replace(' ', '_')).replace("%2F", "/")

        /** Permanent link to a revision (used in CC BY-SA attribution). */
        fun permalink(title: String, revisionId: Long): String =
            "https://en.wikivoyage.org/w/index.php?title=" + ApiUrl.encode(title.replace(' ', '_')) + "&oldid=" + revisionId

        /** Parses an `action=parse` response (formatversion 2 string or v1 `{"*": …}` wikitext). */
        fun parseResponse(json: String, requestedTitle: String): WikivoyageArticle? {
            val root = try {
                JSONObject(json)
            } catch (e: Exception) {
                throw GuideContentException("Malformed Wikivoyage response", null, e)
            }
            root.optJSONObject("error")?.let { err ->
                val code = err.optString("code", "")
                if (code in MISSING_CODES) return null
                throw GuideContentException("Wikivoyage API error: ${code.take(40)}")
            }
            val parse = root.optJSONObject("parse") ?: return null
            val wikitext = when (val w = parse.opt("wikitext")) {
                is String -> w
                is JSONObject -> w.optString("*", "")
                else -> ""
            }
            val title = parse.optStringOrNull("title") ?: requestedTitle
            val rev = parse.optLong("revid", 0L)
            return WikivoyageArticle(title, rev, wikitext)
        }
    }
}
