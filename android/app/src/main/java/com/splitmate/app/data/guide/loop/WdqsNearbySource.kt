package com.splitmate.app.data.guide.loop

import com.splitmate.app.data.guide.GuideFeatureFlags
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.NearbySource
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import kotlinx.coroutines.CancellationException
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

/**
 * Wikidata classes used by the spatial dragnet. Matching uses `wdt:P31/wdt:P279*`, so every
 * subclass is included automatically (e.g. Hindu temple Q842402 ⊂ temple, fort Q1785071 ⊂
 * fortification, fast-food restaurant ⊂ restaurant).
 *
 * The QIDs come from audit §4.2 plus a few well-known additions. Order matters: when an item
 * matches several classes, the earliest kind (SEE > DO > EAT) wins.
 */
object WdqsClasses {
    /** SEE: sights and heritage. */
    val SEE: Map<String, String> = linkedMapOf(
        "Q839954" to "archaeological site",
        "Q4989906" to "monument",
        "Q44539" to "temple",
        "Q16970" to "church building",
        "Q32815" to "mosque",
        "Q33506" to "museum",
        "Q57821" to "fortification",
        "Q16560" to "palace",
        "Q23413" to "castle",
        "Q570116" to "tourist attraction",
        "Q358" to "heritage site"
    )

    /** DO: outdoor and leisure. */
    val DO: Map<String, String> = linkedMapOf(
        "Q22698" to "park",
        "Q40080" to "beach",
        "Q34038" to "waterfall",
        "Q43501" to "zoo",
        "Q167346" to "botanical garden",
        "Q194195" to "amusement park"
    )

    /** EAT: expect sparse coverage in India (decision #21: Wikivoyage Eat listings are primary). */
    val EAT: Map<String, String> = linkedMapOf(
        "Q11707" to "restaurant",
        "Q30022" to "coffeehouse"
    )

    /** Class QIDs for the requested kinds (SLEEP is never queried), in priority order. */
    fun classesFor(kinds: Set<PlaceKind>): List<String> {
        val out = ArrayList<String>()
        if (PlaceKind.SEE in kinds) out += SEE.keys
        if (PlaceKind.DO in kinds) out += DO.keys
        if (PlaceKind.EAT in kinds) out += EAT.keys
        return out
    }

    /** Maps a matched class QID back to its kind, or null if unknown. */
    fun kindOf(classQid: String): PlaceKind? = when (classQid) {
        in SEE -> PlaceKind.SEE
        in DO -> PlaceKind.DO
        in EAT -> PlaceKind.EAT
        else -> null
    }
}

/**
 * Spatial dragnet around a centre using the WDQS `wikibase:around` service (audit §4.2,
 * decision #20).
 *
 * - Adaptive radius: [RADIUS_STEPS_KM] (5 → 10 → 20 km), capped at [MAX_RADIUS_KM], stopping as
 *   soon as at least `minResults` distinct places are found.
 * - `LIMIT 60`, `ORDER BY ?dist`, labels via `SERVICE wikibase:label` with `en,hi,kn,mr` fallback.
 * - Endpoint `https://query.wikidata.org/sparql`, `Accept: application/sparql-results+json`,
 *   identified UA (via [HttpTextFetcher]), 10 s read timeout.
 * - Kill-switch [GuideFeatureFlags.wdqsEnabled]: when off, returns an empty list without I/O.
 * - Failures (network, HTTP 429/5xx, malformed JSON) stop escalation and return what has been
 *   collected so far (possibly empty). This source never throws except for cancellation.
 *
 * Output: `Place(id = "wd:Q…", source = WIKIDATA, distanceKmFromCenter = …)`, de-duplicated by
 * QID, sorted by distance then id (deterministic).
 */
class WdqsNearbySource(
    private val fetcher: HttpTextFetcher = OkHttpTextFetcher(),
    private val flags: () -> GuideFeatureFlags = { GuideFeatureFlags() },
    private val endpoint: String = DEFAULT_ENDPOINT
) : NearbySource {

    override suspend fun nearby(center: LatLng, kinds: Set<PlaceKind>, minResults: Int): List<Place> {
        if (!flags().wdqsEnabled) return emptyList()
        val classes = WdqsClasses.classesFor(kinds)
        if (classes.isEmpty()) return emptyList()

        val merged = LinkedHashMap<String, Place>()
        for (radius in RADIUS_STEPS_KM) {
            val r = radius.coerceAtMost(MAX_RADIUS_KM)
            val batch = try {
                val response = fetcher.get(buildUrl(buildQuery(center, r, classes)), HEADERS)
                if (response.statusCode !in 200..299) break
                parseResults(response.body, kinds)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                break
            }
            for (p in batch) {
                val existing = merged[p.id]
                if (existing == null || (p.distanceKmFromCenter ?: Double.MAX_VALUE) <
                    (existing.distanceKmFromCenter ?: Double.MAX_VALUE)
                ) {
                    merged[p.id] = p
                }
            }
            if (merged.size >= minResults) break
        }
        return sortPlaces(merged.values)
    }

    /** Full GET URL for a query. */
    fun buildUrl(sparql: String): String =
        "$endpoint?format=json&query=${LoopText.percentEncode(sparql)}"

    companion object {
        const val DEFAULT_ENDPOINT = "https://query.wikidata.org/sparql"
        const val RESULT_LIMIT = 60
        const val MAX_RADIUS_KM = 25.0
        val RADIUS_STEPS_KM: List<Double> = listOf(5.0, 10.0, 20.0)
        const val LABEL_LANGUAGES = "en,hi,kn,mr"

        private val HEADERS = mapOf("Accept" to "application/sparql-results+json")

        private val POINT = Regex("""^\s*Point\(\s*([-+0-9.eE]+)\s+([-+0-9.eE]+)\s*\)\s*$""")
        private val QID = Regex("""^Q[1-9]\d*$""")

        /**
         * Builds the SPARQL for one radius. The centre is formatted with [Locale.ROOT] (WKT is
         * `Point(lng lat)`), and the radius is clamped to [MAX_RADIUS_KM]. `hint:Prior
         * hint:gearing "forward"` makes Blazegraph walk the subclass path from each nearby item
         * rather than expanding every subclass of every class.
         */
        fun buildQuery(center: LatLng, radiusKm: Double, classQids: List<String>): String {
            val r = radiusKm.coerceIn(0.1, MAX_RADIUS_KM)
            val values = classQids.filter { QID.matches(it) }.joinToString(" ") { "wd:$it" }
            val lng = String.format(Locale.ROOT, "%.6f", center.lng)
            val lat = String.format(Locale.ROOT, "%.6f", center.lat)
            val radius = String.format(Locale.ROOT, "%.1f", r)
            return """
                |SELECT ?place ?placeLabel ?coord ?dist ?class WHERE {
                |  SERVICE wikibase:around {
                |    ?place wdt:P625 ?coord .
                |    bd:serviceParam wikibase:center "Point($lng $lat)"^^geo:wktLiteral ;
                |                    wikibase:radius "$radius" ;
                |                    wikibase:distance ?dist .
                |  }
                |  VALUES ?class { $values }
                |  ?place wdt:P31/wdt:P279* ?class .
                |  hint:Prior hint:gearing "forward" .
                |  SERVICE wikibase:label { bd:serviceParam wikibase:language "$LABEL_LANGUAGES" . }
                |}
                |ORDER BY ?dist
                |LIMIT $RESULT_LIMIT
            """.trimMargin()
        }

        /**
         * Parses `application/sparql-results+json` into places (pure; fixture-tested).
         * Rows without a usable label (the label service falls back to the bare QID), with a
         * non-Earth / malformed coordinate, or with a kind outside [kinds] are skipped. Multiple
         * rows for one item (several matched classes) collapse to the highest-priority kind.
         *
         * @throws JSONException if the body is not a SPARQL JSON result.
         */
        fun parseResults(body: String, kinds: Set<PlaceKind>): List<Place> {
            val bindings = JSONObject(body).getJSONObject("results").getJSONArray("bindings")
            val byId = LinkedHashMap<String, Place>()
            for (i in 0 until bindings.length()) {
                val row = bindings.optJSONObject(i) ?: continue
                val qid = row.value("place")?.substringAfterLast('/')?.takeIf { QID.matches(it) } ?: continue
                val kind = row.value("class")?.substringAfterLast('/')?.let { WdqsClasses.kindOf(it) } ?: continue
                if (kind !in kinds) continue
                val rawLabel = row.value("placeLabel") ?: continue
                if (QID.matches(rawLabel.trim())) continue
                val name = LoopText.clean(rawLabel) ?: continue
                val location = row.value("coord")?.let { parsePoint(it) } ?: continue
                val dist = row.value("dist")?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
                val id = "wd:$qid"
                val candidate = Place(
                    id = id,
                    kind = kind,
                    name = name,
                    location = location,
                    qid = qid,
                    source = PlaceSource.WIKIDATA,
                    distanceKmFromCenter = dist
                )
                val existing = byId[id]
                if (existing == null || kindRank(kind) < kindRank(existing.kind)) byId[id] = candidate
            }
            return sortPlaces(byId.values)
        }

        /** Parses a WKT `Point(lng lat)`. Returns null for other globes (`<http://…> Point(…)`). */
        fun parsePoint(wkt: String): LatLng? {
            val m = POINT.find(wkt) ?: return null
            val lng = m.groupValues[1].toDoubleOrNull() ?: return null
            val lat = m.groupValues[2].toDoubleOrNull() ?: return null
            return MapLinkCoordinateParser.validate(lat, lng)
        }

        private fun kindRank(kind: PlaceKind): Int = when (kind) {
            PlaceKind.SEE -> 0
            PlaceKind.DO -> 1
            PlaceKind.EAT -> 2
            PlaceKind.SLEEP -> 3
        }

        private fun sortPlaces(places: Collection<Place>): List<Place> =
            places.sortedWith(
                compareBy<Place>({ it.distanceKmFromCenter ?: Double.MAX_VALUE }, { it.id })
            )

        private fun JSONObject.value(key: String): String? =
            optJSONObject(key)?.optString("value")?.takeIf { it.isNotEmpty() }
    }
}
