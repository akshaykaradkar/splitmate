package com.splitmate.app.guide.loop

import com.splitmate.app.data.guide.GuideFeatureFlags
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.data.guide.loop.HttpTextFetcher
import com.splitmate.app.data.guide.loop.HttpTextResponse
import com.splitmate.app.data.guide.loop.WdqsClasses
import com.splitmate.app.data.guide.loop.WdqsNearbySource
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URLDecoder
import java.util.Locale

@DisplayName("WdqsNearbySource: wikibase:around dragnet with adaptive radius")
class WdqsNearbySourceTest {

    private val hampi = LatLng(15.335, 76.46)
    private val allKinds = setOf(PlaceKind.SEE, PlaceKind.DO, PlaceKind.EAT)

    /** Serves the fixture matching the query's radius; records decoded queries. */
    private class RadiusFixtureFetcher(
        private val statusFor: (String) -> Int = { 200 }
    ) : HttpTextFetcher {
        val queries = mutableListOf<String>()
        val headers = mutableListOf<Map<String, String>>()
        override suspend fun get(url: String, headers: Map<String, String>): HttpTextResponse {
            val q = URLDecoder.decode(url.substringAfter("query="), "UTF-8")
            queries += q
            this.headers += headers
            val radius = Regex("""wikibase:radius "([0-9.]+)"""").find(q)!!.groupValues[1]
            val status = statusFor(radius)
            if (status != 200) return HttpTextResponse(status, "")
            val file = when (radius) {
                "5.0" -> "wdqs_hampi_r5.json"
                "10.0" -> "wdqs_hampi_r10.json"
                "20.0" -> "wdqs_hampi_r20.json"
                else -> error("unexpected radius $radius")
            }
            return HttpTextResponse(200, LoopFixtures.text(file))
        }
    }

    private fun radii(f: RadiusFixtureFetcher) =
        f.queries.map { Regex("""wikibase:radius "([0-9.]+)"""").find(it)!!.groupValues[1] }

    // ------------------------------------------------------------------ SPARQL builder

    @Test
    fun `query uses wikibase around with centre, radius, distance, labels, order and limit`() {
        val q = WdqsNearbySource.buildQuery(hampi, 5.0, WdqsClasses.classesFor(allKinds))
        assertTrue(q.contains("SERVICE wikibase:around"))
        assertTrue(q.contains("wikibase:center \"Point(76.460000 15.335000)\"^^geo:wktLiteral"), q)
        assertTrue(q.contains("wikibase:radius \"5.0\""))
        assertTrue(q.contains("wikibase:distance ?dist"))
        assertTrue(q.contains("?place wdt:P31/wdt:P279* ?class"))
        assertTrue(q.contains("hint:Prior hint:gearing \"forward\""))
        assertTrue(q.contains("SERVICE wikibase:label { bd:serviceParam wikibase:language \"en,hi,kn,mr\" . }"))
        assertTrue(q.contains("ORDER BY ?dist"))
        assertTrue(q.trimEnd().endsWith("LIMIT 60"))
        for (qid in listOf("Q839954", "Q44539", "Q33506", "Q57821", "Q570116", "Q4989906", "Q11707", "Q22698")) {
            assertTrue(q.contains("wd:$qid"), "missing $qid")
        }
    }

    @Test
    fun `query only includes classes for requested kinds`() {
        val eatOnly = WdqsNearbySource.buildQuery(hampi, 5.0, WdqsClasses.classesFor(setOf(PlaceKind.EAT)))
        assertTrue(eatOnly.contains("wd:Q11707"))
        assertFalse(eatOnly.contains("wd:Q44539"))
        assertTrue(WdqsClasses.classesFor(setOf(PlaceKind.SLEEP)).isEmpty())
    }

    @Test
    fun `radius is capped at 25 km`() {
        val q = WdqsNearbySource.buildQuery(hampi, 40.0, listOf("Q44539"))
        assertTrue(q.contains("wikibase:radius \"25.0\""))
    }

    @Test
    fun `query formatting is locale independent`() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val q = WdqsNearbySource.buildQuery(hampi, 5.0, listOf("Q44539"))
            assertTrue(q.contains("Point(76.460000 15.335000)"), q)
            assertTrue(q.contains("\"5.0\""))
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun `malformed class ids are never injected into SPARQL`() {
        val q = WdqsNearbySource.buildQuery(hampi, 5.0, listOf("Q44539", "Q1 } DROP", "wd:Q5", ""))
        assertTrue(q.contains("VALUES ?class { wd:Q44539 }"), q)
    }

    @Test
    fun `url targets the https sparql endpoint and round-trips the query`() {
        val q = WdqsNearbySource.buildQuery(hampi, 5.0, listOf("Q44539"))
        val url = WdqsNearbySource(RadiusFixtureFetcher()).buildUrl(q)
        assertTrue(url.startsWith("https://query.wikidata.org/sparql?format=json&query="))
        assertEquals(q, URLDecoder.decode(url.substringAfter("query="), "UTF-8"))
        assertFalse(url.contains(" "))
    }

    // ------------------------------------------------------------------ Result parsing

    @Test
    fun `parses fixture, dedupes by QID, picks highest-priority kind, skips junk rows`() {
        val places = WdqsNearbySource.parseResults(LoopFixtures.text("wdqs_hampi_r5.json"), allKinds)
        assertEquals(
            listOf("wd:Q1050711", "wd:Q5639020", "wd:Q5710432", "wd:Q111222333", "wd:Q3500123"),
            places.map { it.id }
        )
        val virupaksha = places.first()
        assertEquals("Virupaksha Temple", virupaksha.name)
        assertEquals(PlaceKind.SEE, virupaksha.kind)
        assertEquals(PlaceSource.WIKIDATA, virupaksha.source)
        assertEquals("Q1050711", virupaksha.qid)
        assertEquals(15.335, virupaksha.location!!.lat, 1e-9)
        assertEquals(76.4599, virupaksha.location!!.lng, 1e-9)
        assertEquals(0.012, virupaksha.distanceKmFromCenter!!, 1e-9)

        assertEquals(PlaceKind.SEE, places.first { it.id == "wd:Q5710432" }.kind, "SEE beats DO for multi-class items")
        assertEquals(PlaceKind.EAT, places.first { it.id == "wd:Q111222333" }.kind)
        assertEquals("ಕಮಲಾಪುರ ಕೆರೆ", places.first { it.id == "wd:Q3500123" }.name, "label sanitised (bidi override removed)")
        assertTrue(places.none { it.id == "wd:Q98765432" }, "QID-only labels are skipped")
        assertTrue(places.none { it.id == "wd:Q222333444" }, "non-Earth coordinates are skipped")
    }

    @Test
    fun `parse filters by requested kinds`() {
        val seeOnly = WdqsNearbySource.parseResults(LoopFixtures.text("wdqs_hampi_r5.json"), setOf(PlaceKind.SEE))
        assertTrue(seeOnly.all { it.kind == PlaceKind.SEE })
        assertEquals(3, seeOnly.size)
    }

    @Test
    fun `parse point handles WKT and rejects other globes and garbage`() {
        assertEquals(LatLng(15.335, 76.46), WdqsNearbySource.parsePoint("Point(76.46 15.335)"))
        assertNull(WdqsNearbySource.parsePoint("<http://www.wikidata.org/entity/Q405> Point(10 10)"))
        assertNull(WdqsNearbySource.parsePoint("Point(200 10)"))
        assertNull(WdqsNearbySource.parsePoint("nonsense"))
    }

    @Test
    fun `non-json body throws JSONException from the pure parser`() {
        assertThrows(org.json.JSONException::class.java) {
            WdqsNearbySource.parseResults("<html>rate limited</html>", allKinds)
        }
    }

    // ------------------------------------------------------------------ Adaptive radius

    @Test
    fun `escalates 5 to 10 to 20 km until minResults is met`(): Unit = runBlocking {
        val f = RadiusFixtureFetcher()
        val places = WdqsNearbySource(f).nearby(hampi, allKinds, minResults = 8)
        assertEquals(listOf("5.0", "10.0", "20.0"), radii(f))
        assertEquals(10, places.size)
        val d = places.map { it.distanceKmFromCenter!! }
        assertEquals(d.sorted(), d, "sorted by distance")
        assertEquals(places.map { it.id }.distinct(), places.map { it.id })
    }

    @Test
    fun `stops at the first radius that satisfies minResults`(): Unit = runBlocking {
        val f1 = RadiusFixtureFetcher()
        assertEquals(5, WdqsNearbySource(f1).nearby(hampi, allKinds, minResults = 5).size)
        assertEquals(listOf("5.0"), radii(f1))

        val f2 = RadiusFixtureFetcher()
        assertEquals(7, WdqsNearbySource(f2).nearby(hampi, allKinds, minResults = 7).size)
        assertEquals(listOf("5.0", "10.0"), radii(f2))
    }

    @Test
    fun `never exceeds the 20 km step (25 km cap) even if minResults is unreachable`(): Unit = runBlocking {
        val f = RadiusFixtureFetcher()
        val places = WdqsNearbySource(f).nearby(hampi, allKinds, minResults = 1_000)
        assertEquals(listOf("5.0", "10.0", "20.0"), radii(f))
        assertEquals(10, places.size)
        assertTrue(WdqsNearbySource.RADIUS_STEPS_KM.all { it <= WdqsNearbySource.MAX_RADIUS_KM })
    }

    @Test
    fun `failure at a larger radius returns what was already collected`(): Unit = runBlocking {
        val f = RadiusFixtureFetcher(statusFor = { r -> if (r == "10.0") 429 else 200 })
        val places = WdqsNearbySource(f).nearby(hampi, allKinds, minResults = 8)
        assertEquals(5, places.size)
        assertEquals(listOf("5.0", "10.0"), radii(f))
    }

    @Test
    fun `sends sparql json accept header`(): Unit = runBlocking {
        val f = RadiusFixtureFetcher()
        WdqsNearbySource(f).nearby(hampi, allKinds, minResults = 1)
        assertEquals("application/sparql-results+json", f.headers.first()["Accept"])
    }

    @Test
    fun `kill-switch off or SLEEP-only makes no request`(): Unit = runBlocking {
        val f = RadiusFixtureFetcher()
        val off = WdqsNearbySource(f, { GuideFeatureFlags(wdqsEnabled = false) })
        assertTrue(off.nearby(hampi, allKinds, 8).isEmpty())
        assertTrue(WdqsNearbySource(f).nearby(hampi, setOf(PlaceKind.SLEEP), 8).isEmpty())
        assertTrue(f.queries.isEmpty())
    }
}
