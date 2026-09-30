package com.splitmate.app.guide.content

import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.content.DestinationScorer
import com.splitmate.app.data.guide.content.GuideContentException
import com.splitmate.app.data.guide.content.WikidataActionApiClient
import com.splitmate.app.data.guide.content.WikidataEntity
import com.splitmate.app.data.guide.content.WikidataSearchHit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DestinationScorerTest {

    private val scorer = DestinationScorer()

    private fun hampiHits() = WikidataActionApiClient.parseSearch(Fixtures.text("wikidata_search_hampi.json"))
    private fun hampiEntities() = WikidataActionApiClient.parseEntities(Fixtures.text("wikidata_entities_hampi.json"))

    // ---- Wikidata parsing -----------------------------------------------------------------------

    @Test
    fun `parseSearch keeps API order - skips non-QIDs - assigns ranks`() {
        val hits = hampiHits()
        assertEquals(listOf("Q9000101", "Q9000102", "Q9000103", "Q9000104", "Q9000105"), hits.map { it.qid })
        assertEquals(listOf(0, 1, 2, 3, 4), hits.map { it.rank })
        assertEquals("Hampi", hits[0].label)
        assertEquals("village in Vijayanagara district, Karnataka, India", hits[0].description)
    }

    @Test
    fun `parseEntities extracts P625 P18 P17 P31 and enwikivoyage sitelink`() {
        val e = hampiEntities()
        assertEquals(5, e.size, "missing item is skipped")
        val village = e.getValue("Q9000101")
        assertEquals(LatLng(15.335, 76.46), village.location, "deprecated (0,0) statement ignored")
        assertEquals("Virupaksha Temple, Hampi.jpg", village.imageFile)
        assertEquals("Q668", village.countryQid)
        assertEquals(listOf("Q532"), village.instanceOf)
        assertEquals("Hampi", village.enwikivoyageTitle)
        assertEquals("Hampi", village.enwikiTitle)
        assertEquals(2, village.sitelinkCount)
        assertEquals("en", village.labelLanguage)
        assertNull(e.getValue("Q9000102").enwikivoyageTitle)
    }

    @Test
    fun `label falls back en - hi - kn - mr - preferred rank wins - numeric-id country`() {
        val u = hampiEntities().getValue("Q9000105")
        assertEquals("ಕನ್ನಡ ವಿಶ್ವವಿದ್ಯಾಲಯ", u.label)
        assertEquals("kn", u.labelLanguage)
        assertEquals(LatLng(15.3, 76.42), u.location)
        assertEquals("Q668", u.countryQid)
        assertNull(u.imageFile, "novalue snak ignored")
        assertNull(u.description)
    }

    @Test
    fun `API error body raises GuideContentException`() {
        assertThrows(GuideContentException::class.java) {
            WikidataActionApiClient.parseSearch("""{"error":{"code":"badvalue","info":"x"}}""")
        }
        assertThrows(GuideContentException::class.java) { WikidataActionApiClient.parseEntities("not json") }
    }

    @Test
    fun `request URLs use Action API params`() {
        val s = WikidataActionApiClient.searchUrl("Hampi")
        assertTrue(s.startsWith("https://www.wikidata.org/w/api.php?action=wbsearchentities"))
        listOf("search=Hampi", "language=en", "uselang=en", "type=item", "limit=7", "maxlag=5", "format=json").forEach {
            assertTrue(s.contains(it), "missing $it in $s")
        }
        val g = WikidataActionApiClient.entitiesUrl(listOf("Q1", "Q2"))
        listOf("action=wbgetentities", "ids=Q1%7CQ2", "props=labels%7Cdescriptions%7Cclaims%7Csitelinks",
            "languages=en%7Chi%7Ckn%7Cmr", "languagefallback=1", "sitefilter=enwikivoyage%7Cenwiki", "maxlag=5").forEach {
            assertTrue(g.contains(it), "missing $it in $g")
        }
    }

    // ---- scoring --------------------------------------------------------------------------------

    @Test
    fun `hampi ranks the Wikivoyage village first and is not ambiguous`() {
        val c = scorer.rank(hampiHits(), hampiEntities())
        assertEquals(listOf("Q9000101", "Q9000102", "Q9000105", "Q9000103", "Q9000104"), c.map { it.qid })
        assertEquals(108.099, c[0].score, 1e-9)
        assertEquals(49.693, c[1].score, 1e-9)
        assertEquals("Hampi", c[0].wikivoyageTitle)
        assertEquals(LatLng(15.335, 76.46), c[0].location)
        assertEquals("Q668", c[0].countryQid)
        assertTrue(c.last().score < 0, "disambiguation page is penalised")
        assertFalse(scorer.isAmbiguous(c))
    }

    @Test
    fun `ratnagiri city vs district is ambiguous until a booking hint breaks the tie`() {
        val hits = WikidataActionApiClient.parseSearch(Fixtures.text("wikidata_search_ratnagiri.json"))
        val ents = WikidataActionApiClient.parseEntities(Fixtures.text("wikidata_entities_ratnagiri.json"))
        val plain = scorer.rank(hits, ents)
        assertEquals(listOf("Q9000201", "Q9000202", "Q9000203"), plain.map { it.qid })
        assertEquals(11.0, plain[0].score - plain[1].score, 1e-9)
        assertTrue(scorer.isAmbiguous(plain))

        val hinted = scorer.rank(hits, ents, bookingHint = "RATNAGIRI")
        assertEquals("Q9000201", hinted[0].qid)
        assertEquals(24.0, hinted[0].score - hinted[1].score, 1e-9)
        assertFalse(scorer.isAmbiguous(hinted))
    }

    @Test
    fun `aurangabad twins are ambiguous and tie-break by search rank`() {
        val f = "wikidata_ambiguous_aurangabad.json"
        val c = scorer.rank(
            WikidataActionApiClient.parseSearch(Fixtures.part(f, "search_response")),
            WikidataActionApiClient.parseEntities(Fixtures.part(f, "entities_response"))
        )
        assertEquals(listOf("Q9000401", "Q9000402"), c.map { it.qid })
        assertEquals("city in Maharashtra, India", c[0].description)
        assertTrue(scorer.isAmbiguous(c))
        assertEquals("Aurangabad (Bihar)", c[1].wikivoyageTitle)
    }

    @Test
    fun `gokarna India town beats higher-ranked Nepal village and country is configurable`() {
        val f = "wikidata_gokarna.json"
        val hits = WikidataActionApiClient.parseSearch(Fixtures.part(f, "search_response"))
        val ents = WikidataActionApiClient.parseEntities(Fixtures.part(f, "entities_response"))
        val india = scorer.rank(hits, ents)
        assertEquals("Q9000301", india[0].qid)
        assertEquals("Gokarna", india[0].wikivoyageTitle)

        val nepalScorer = DestinationScorer(preferredCountryQid = "Q837")
        val nepal = nepalScorer.rank(hits, ents)
        val nepalItemIndia = india.first { it.qid == "Q9000302" }.score
        val nepalItemNepal = nepal.first { it.qid == "Q9000302" }.score
        assertEquals(20.0, nepalItemNepal - nepalItemIndia, 1e-9)
        assertEquals("Q9000301", nepal[0].qid, "Wikivoyage sitelink still dominates")

        val noCountry = DestinationScorer(preferredCountryQid = null).rank(hits, ents)
        assertEquals(india.map { it.qid }, noCountry.map { it.qid })
    }

    @Test
    fun `ranking is deterministic regardless of input order`() {
        val hits = hampiHits()
        val a = scorer.rank(hits, hampiEntities())
        val b = scorer.rank(hits.reversed(), hampiEntities())
        val c = scorer.rank(hits.shuffled(java.util.Random(42)), hampiEntities())
        assertEquals(a, b)
        assertEquals(a, c)
    }

    @Test
    fun `booking hint exact vs partial vs none`() {
        val hit = WikidataSearchHit("Q1", "Hosapete", "city in Karnataka, India", 0)
        val e = entity("Q1", "Hosapete")
        val none = scorer.score(hit, e, null)
        assertEquals(25.0, scorer.score(hit, e, "Hosapete Junction") - none, 1e-9, "junction noise stripped")
        assertEquals(12.0, scorer.score(hit, e, "Karnataka") - none, 1e-9, "description word match")
        assertEquals(0.0, scorer.score(hit, e, "Goa") - none, 1e-9)
        assertEquals(0.0, scorer.score(hit, e, "  ") - none, 1e-9)
        assertEquals("hospet", DestinationScorer.normalize("Hospet Jn."))
    }

    @Test
    fun `missing entity still produces a candidate from the search hit`() {
        val c = scorer.rank(listOf(WikidataSearchHit("Q77", "Lonely", null, 0)), emptyMap())
        assertEquals(1, c.size)
        assertEquals("Lonely", c[0].label)
        assertNull(c[0].location)
        assertEquals(7.0, c[0].score, 1e-9)
    }

    @Test
    fun `isAmbiguous edge cases and custom margin`() {
        assertFalse(scorer.isAmbiguous(emptyList()))
        val single = scorer.rank(listOf(WikidataSearchHit("Q77", "Lonely", null, 0)), emptyMap())
        assertFalse(scorer.isAmbiguous(single))
        val hits = WikidataActionApiClient.parseSearch(Fixtures.text("wikidata_search_ratnagiri.json"))
        val ents = WikidataActionApiClient.parseEntities(Fixtures.text("wikidata_entities_ratnagiri.json"))
        assertFalse(DestinationScorer(ambiguityMargin = 5.0).let { it.isAmbiguous(it.rank(hits, ents)) })
    }

    private fun entity(qid: String, label: String) = WikidataEntity(
        qid = qid, label = label, labelLanguage = "en", description = null, location = LatLng(15.27, 76.39),
        imageFile = null, countryQid = "Q668", instanceOf = listOf("Q515"), enwikivoyageTitle = null,
        enwikiTitle = null, sitelinkCount = 0
    )
}
