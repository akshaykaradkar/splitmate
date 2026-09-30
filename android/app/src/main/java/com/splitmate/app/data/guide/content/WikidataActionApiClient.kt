package com.splitmate.app.data.guide.content

import com.splitmate.app.data.guide.LatLng
import org.json.JSONArray
import org.json.JSONObject

/** One `wbsearchentities` hit, in API relevance order ([rank] 0 = best). */
data class WikidataSearchHit(
    val qid: String,
    val label: String,
    val description: String?,
    val rank: Int
)

/** The subset of a Wikidata item the guide needs. */
data class WikidataEntity(
    val qid: String,
    val label: String?,
    val labelLanguage: String?,
    val description: String?,
    val location: LatLng?,
    /** P18 Commons file name, without the `File:` prefix. */
    val imageFile: String?,
    /** P17 country QID. */
    val countryQid: String?,
    /** P31 instance-of QIDs (non-deprecated, preferred first). */
    val instanceOf: List<String>,
    val enwikivoyageTitle: String?,
    val enwikiTitle: String?,
    val sitelinkCount: Int
)

/**
 * Wikidata **Action API** client for destination resolution (never SPARQL for name lookup).
 *
 * - [search]: `wbsearchentities` (language=en, uselang=en, type=item, limit=7).
 * - [getEntities]: `wbgetentities` (props=labels|descriptions|claims|sitelinks,
 *   languages=en|hi|kn|mr, languagefallback, sitefilter=enwikivoyage|enwiki).
 *
 * Label/description fall back en → hi → kn → mr (decision #26).
 * Parsing is exposed separately ([parseSearch], [parseEntities]) for fixture tests.
 */
class WikidataActionApiClient(private val http: PoliteHttpClient) {

    fun search(query: String, limit: Int = SEARCH_LIMIT): List<WikidataSearchHit> {
        val q = query.trim().take(MAX_QUERY_CHARS)
        if (q.isEmpty()) return emptyList()
        return parseSearch(http.get(searchUrl(q, limit)))
    }

    fun getEntities(qids: List<String>): Map<String, WikidataEntity> {
        val ids = qids.filter { QID.matches(it) }.distinct().take(MAX_IDS)
        if (ids.isEmpty()) return emptyMap()
        return parseEntities(http.get(entitiesUrl(ids)))
    }

    companion object {
        const val SEARCH_LIMIT = 7
        const val MAX_IDS = 50
        const val MAX_QUERY_CHARS = 100
        val LANGUAGES = listOf("en", "hi", "kn", "mr")
        val QID = Regex("^Q[1-9][0-9]*$")

        fun searchUrl(query: String, limit: Int = SEARCH_LIMIT): String = ApiUrl.mediaWiki(
            ApiUrl.WIKIDATA_API,
            listOf(
                "action" to "wbsearchentities",
                "search" to query,
                "language" to "en",
                "uselang" to "en",
                "type" to "item",
                "limit" to limit.coerceIn(1, 50).toString()
            )
        )

        fun entitiesUrl(ids: List<String>): String = ApiUrl.mediaWiki(
            ApiUrl.WIKIDATA_API,
            listOf(
                "action" to "wbgetentities",
                "ids" to ids.joinToString("|"),
                "props" to "labels|descriptions|claims|sitelinks",
                "languages" to LANGUAGES.joinToString("|"),
                "languagefallback" to "1",
                "sitefilter" to "enwikivoyage|enwiki"
            )
        )

        /** Parses a `wbsearchentities` response. Throws [GuideContentException] on API error. */
        fun parseSearch(json: String): List<WikidataSearchHit> {
            val root = parseRoot(json)
            val arr = root.optJSONArray("search") ?: return emptyList()
            val out = ArrayList<WikidataSearchHit>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id", "")
                if (!QID.matches(id)) continue
                val label = WikiTextSanitizer.toPlainText(
                    o.optStringOrNull("label") ?: o.optJSONObject("display")?.optJSONObject("label")?.optStringOrNull("value") ?: id
                )
                val desc = (o.optStringOrNull("description")
                    ?: o.optJSONObject("display")?.optJSONObject("description")?.optStringOrNull("value"))
                    ?.let { WikiTextSanitizer.toPlainText(it) }?.ifEmpty { null }
                out += WikidataSearchHit(id, label.ifEmpty { id }, desc, out.size)
            }
            return out
        }

        /** Parses a `wbgetentities` response; missing/redirected-away items are skipped. */
        fun parseEntities(json: String): Map<String, WikidataEntity> {
            val root = parseRoot(json)
            val entities = root.optJSONObject("entities") ?: return emptyMap()
            val out = LinkedHashMap<String, WikidataEntity>()
            val keys = entities.keys().asSequence().toList().sorted()
            for (key in keys) {
                val e = entities.optJSONObject(key) ?: continue
                if (e.has("missing")) continue
                val qid = e.optString("id", key)
                if (!QID.matches(qid)) continue
                out[qid] = parseEntity(qid, e)
            }
            return out
        }

        private fun parseEntity(qid: String, e: JSONObject): WikidataEntity {
            val (label, labelLang) = pickTerm(e.optJSONObject("labels"))
            val (desc, _) = pickTerm(e.optJSONObject("descriptions"))
            val claims = e.optJSONObject("claims") ?: JSONObject()
            val sitelinks = e.optJSONObject("sitelinks") ?: JSONObject()
            return WikidataEntity(
                qid = qid,
                label = label?.let { WikiTextSanitizer.toPlainText(it) }?.ifEmpty { null },
                labelLanguage = labelLang,
                description = desc?.let { WikiTextSanitizer.toPlainText(it) }?.ifEmpty { null },
                location = bestValues(claims, "P625").firstNotNullOfOrNull { coordOf(it) },
                imageFile = bestValues(claims, "P18").firstNotNullOfOrNull { (it as? String)?.trim()?.ifEmpty { null } },
                countryQid = bestValues(claims, "P17").firstNotNullOfOrNull { entityIdOf(it) },
                instanceOf = bestValues(claims, "P31").mapNotNull { entityIdOf(it) }.distinct(),
                enwikivoyageTitle = sitelinks.optJSONObject("enwikivoyage")?.optStringOrNull("title"),
                enwikiTitle = sitelinks.optJSONObject("enwiki")?.optStringOrNull("title"),
                sitelinkCount = sitelinks.length()
            )
        }

        /** Returns (value, actualLanguage) using en → hi → kn → mr. */
        private fun pickTerm(terms: JSONObject?): Pair<String?, String?> {
            if (terms == null) return null to null
            for (lang in LANGUAGES) {
                val t = terms.optJSONObject(lang) ?: continue
                val v = t.optStringOrNull("value")?.trim()
                if (!v.isNullOrEmpty()) return v to (t.optStringOrNull("language") ?: lang)
            }
            return null to null
        }

        /** Datavalue payloads of non-deprecated statements, preferred-rank first, document order. */
        private fun bestValues(claims: JSONObject, pid: String): List<Any> {
            val arr: JSONArray = claims.optJSONArray(pid) ?: return emptyList()
            val preferred = ArrayList<Any>()
            val normal = ArrayList<Any>()
            for (i in 0 until arr.length()) {
                val st = arr.optJSONObject(i) ?: continue
                val rank = st.optString("rank", "normal")
                if (rank == "deprecated") continue
                val snak = st.optJSONObject("mainsnak") ?: continue
                if (snak.optString("snaktype", "value") != "value") continue
                val value = snak.optJSONObject("datavalue")?.opt("value") ?: continue
                if (rank == "preferred") preferred += value else normal += value
            }
            return preferred + normal
        }

        private fun coordOf(v: Any): LatLng? {
            val o = v as? JSONObject ?: return null
            val globe = o.optString("globe", "")
            if (globe.isNotEmpty() && !globe.endsWith("/Q2")) return null // Earth only
            if (!o.has("latitude") || !o.has("longitude")) return null
            val lat = o.optDouble("latitude", Double.NaN)
            val lng = o.optDouble("longitude", Double.NaN)
            if (lat.isNaN() || lng.isNaN() || lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
            return LatLng(lat, lng)
        }

        private fun entityIdOf(v: Any): String? {
            val o = v as? JSONObject ?: return null
            val id = o.optStringOrNull("id") ?: o.optLong("numeric-id", -1L).takeIf { it > 0 }?.let { "Q$it" }
            return id?.takeIf { QID.matches(it) }
        }

        internal fun parseRoot(json: String): JSONObject {
            val root = try {
                JSONObject(json)
            } catch (e: Exception) {
                throw GuideContentException("Malformed Wikidata response", null, e)
            }
            root.optJSONObject("error")?.let {
                throw GuideContentException("Wikidata API error: ${it.optString("code", "unknown").take(40)}")
            }
            return root
        }
    }
}

/** `optString` that maps absent/JSON-null to Kotlin null. */
internal fun JSONObject.optStringOrNull(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key, "").let { it }
