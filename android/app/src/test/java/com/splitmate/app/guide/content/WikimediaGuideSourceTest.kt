package com.splitmate.app.guide.content

import com.splitmate.app.data.guide.DestinationCandidate
import com.splitmate.app.data.guide.GuideHttp
import com.splitmate.app.data.guide.GuidePack
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.content.GuideContentException
import com.splitmate.app.data.guide.content.GuidePackCodec
import com.splitmate.app.data.guide.content.HttpResponse
import com.splitmate.app.data.guide.content.PoliteHttpClient
import com.splitmate.app.data.guide.content.WikimediaGuideSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WikimediaGuideSourceTest {

    private val fixedNow = 1_727_600_000_000L

    private fun hampiFake(): FakeHttpFetcher = FakeHttpFetcher()
        .on("action=wbsearchentities", Fixtures.ok(Fixtures.text("wikidata_search_hampi.json")))
        .on("action=wbgetentities", Fixtures.ok(Fixtures.text("wikidata_entities_hampi.json")))
        .on("iiurlwidth=1280", Fixtures.ok(Fixtures.text("commons_imageinfo_hampi_hero.json")))
        .on("iiurlwidth=320", Fixtures.ok(Fixtures.text("commons_imageinfo_hampi_thumbs.json")))
        .on(listOf("action=parse", "page=Hampi"), Fixtures.ok(Fixtures.parseResponse("Hampi", 4500101L, "wikivoyage_hampi.wikitext")))

    private fun source(fake: FakeHttpFetcher) = WikimediaGuideSource(
        fetcher = fake,
        clock = { fixedNow },
        ioDispatcher = Dispatchers.Unconfined,
        http = PoliteHttpClient(fake, sleeper = {})
    )

    private fun candidate(qid: String, label: String, wv: String?, loc: LatLng? = LatLng(15.335, 76.46)) =
        DestinationCandidate(qid, label, null, loc, wv, "Q668", 0.0)

    @Test
    fun `resolve then buildPack assembles a complete Hampi pack`() = runBlocking {
        val fake = hampiFake()
        val src = source(fake)
        val candidates = src.resolve("Hampi")
        assertEquals("Q9000101", candidates.first().qid)
        assertFalse(src.isAmbiguous(candidates))

        val pack: GuidePack = src.buildPack(candidates.first())
        assertEquals(GuidePack.SCHEMA_V1, pack.schema)
        assertEquals("Hampi", pack.destination.label)
        assertEquals("village in Vijayanagara district, Karnataka, India", pack.destination.description)
        assertEquals(LatLng(15.335, 76.46), pack.destination.location)
        assertEquals("Example Photographer & R. Rao", pack.destination.hero!!.artist)
        assertEquals(1280, pack.destination.hero!!.width)

        val wv = pack.wikivoyage!!
        assertEquals("Hampi", wv.title)
        assertEquals(4500101L, wv.revisionId)
        assertEquals("https://en.wikivoyage.org/wiki/Hampi", wv.url)
        assertEquals("CC BY-SA 4.0", wv.license)

        assertEquals(17, pack.places.size)
        assertEquals(3, pack.places.count { it.kind == PlaceKind.SLEEP })
        assertTrue(pack.nearby.isEmpty(), "nearby is filled by the loop workstream")
        assertEquals(fixedNow, pack.fetchedAtEpochMs)

        val byId = pack.places.associateBy { it.id }
        assertEquals(0.0, byId.getValue("wv:see:virupaksha-temple").distanceKmFromCenter!!, 1e-9)
        assertNull(byId.getValue("wv:see:virupaksha-temple").image, "file missing on Commons -> no image")
        assertEquals(320, byId.getValue("wv:see:hemakuta-hill").image!!.width)
        assertEquals("Public domain", byId.getValue("wv:sleep:evolve-back-kamalapura-palace").image!!.license)
        assertEquals(4.77, byId.getValue("wv:sleep:evolve-back-kamalapura-palace").distanceKmFromCenter!!, 0.02)
        assertNull(byId.getValue("wv:see:hampi-archaeological-museum").distanceKmFromCenter)

        assertEquals(5, pack.attribution.size, "wikivoyage + wikidata + hero + 2 thumbs")
        assertTrue(pack.attribution[0].startsWith("Text from the Wikivoyage article \"Hampi\" (revision 4500101), CC BY-SA 4.0"))
        assertTrue(pack.attribution[0].contains("https://en.wikivoyage.org/w/index.php?title=Hampi&oldid=4500101"))
        assertTrue(pack.attribution[1].contains("Wikidata (Q9000101), CC0"))
        assertTrue(pack.attribution[2].startsWith("Photo \"Virupaksha Temple, Hampi.jpg\" by Example Photographer & R. Rao, CC BY-SA 4.0"))
        assertTrue(pack.attribution.any { it.contains("Hemakuta hill temples.jpg") && it.contains("CC BY 2.0") })

        // Politeness: 5 requests total, identified UA, maxlag, https, no duplicate wbgetentities.
        val urls = fake.urls()
        assertEquals(5, urls.size, urls.joinToString("\n"))
        assertEquals(1, urls.count { it.contains("action=wbgetentities") })
        assertTrue(urls.all { it.startsWith("https://") && it.contains("maxlag=5") })
        assertTrue(fake.requests.all { it.headers["User-Agent"] == GuideHttp.USER_AGENT })

        // Pack is codec-stable.
        assertEquals(pack, GuidePackCodec.decodeGzip(GuidePackCodec.encodeGzip(pack)))
    }

    @Test
    fun `booking hint reorders ambiguous Ratnagiri`() = runBlocking {
        val fake = FakeHttpFetcher()
            .on("action=wbsearchentities", Fixtures.ok(Fixtures.text("wikidata_search_ratnagiri.json")))
            .on("action=wbgetentities", Fixtures.ok(Fixtures.text("wikidata_entities_ratnagiri.json")))
        val src = source(fake)
        assertTrue(src.isAmbiguous(src.resolve("Ratnagiri")))
        val hinted = src.resolve("Ratnagiri", bookingHint = "Ratnagiri")
        assertEquals("Q9000201", hinted.first().qid)
        assertFalse(src.isAmbiguous(hinted))
    }

    @Test
    fun `candidate without Wikivoyage sitelink yields partial pack with no places`() = runBlocking {
        val fake = hampiFake()
        val src = source(fake)
        val monuments = src.resolve("Hampi").first { it.qid == "Q9000102" }
        val pack = src.buildPack(monuments)
        assertNull(pack.wikivoyage)
        assertTrue(pack.places.isEmpty())
        assertEquals(LatLng(15.3144, 76.4712), pack.destination.location)
        assertFalse(fake.urls().any { it.contains("action=parse") }, "no Wikivoyage call without a title")
        assertFalse(pack.attribution.any { it.contains("Wikivoyage") })
        assertTrue(pack.attribution.any { it.contains("CC0") })
    }

    @Test
    fun `missing Wikivoyage article degrades gracefully`() = runBlocking {
        val fake = FakeHttpFetcher()
            .on("action=wbgetentities", Fixtures.ok("""{"entities":{}}"""))
            .on("action=parse", Fixtures.ok(Fixtures.text("wikivoyage_parse_missing.json")))
        val pack = source(fake).buildPack(candidate("Q9000999", "Nowhere", "Nowhere"))
        assertNull(pack.wikivoyage)
        assertTrue(pack.places.isEmpty())
        assertNull(pack.destination.hero)
        assertEquals("Nowhere", pack.destination.label)
    }

    @Test
    fun `outline article keeps the Wikivoyage reference but has no places`() = runBlocking {
        val fake = FakeHttpFetcher()
            .on("action=wbgetentities", Fixtures.ok("""{"entities":{}}"""))
            .on("action=parse", Fixtures.ok(Fixtures.text("wikivoyage_parse_outline_dapoli.json")))
        val pack = source(fake).buildPack(candidate("Q9000501", "Dapoli", "Dapoli", LatLng(17.76, 73.19)))
        assertEquals(4500301L, pack.wikivoyage!!.revisionId)
        assertTrue(pack.places.isEmpty())
        assertTrue(pack.attribution[0].contains("revision 4500301"))
    }

    @Test
    fun `pinned revision is fetched by oldid`() = runBlocking {
        val fake = hampiFake()
            .on("oldid=4400000", Fixtures.ok(Fixtures.parseResponse("Hampi", 4400000L, "wikivoyage_gokarna.wikitext")))
        val src = source(fake)
        val c = src.resolve("Hampi").first()
        val pack = src.buildPack(c, pinnedRevisionId = 4400000L)
        assertEquals(4400000L, pack.wikivoyage!!.revisionId)
        val parseUrls = fake.urls().filter { it.contains("action=parse") }
        assertEquals(1, parseUrls.size)
        assertTrue(parseUrls[0].contains("oldid=4400000") && !parseUrls[0].contains("page="))
        assertEquals(9, pack.places.size, "content of the pinned revision")
    }

    @Test
    fun `deleted pinned revision falls back to latest`() = runBlocking {
        val fake = hampiFake()
            .on("oldid=1", Fixtures.ok("""{"error":{"code":"nosuchrevid","info":"There is no revision with ID 1."}}"""))
        val src = source(fake)
        val pack = src.buildPack(src.resolve("Hampi").first(), pinnedRevisionId = 1L)
        assertEquals(4500101L, pack.wikivoyage!!.revisionId)
        assertEquals(17, pack.places.size)
    }

    @Test
    fun `hero failure does not block the pack and 429 on parse is retried`() = runBlocking {
        val fake = FakeHttpFetcher()
            .on("action=wbsearchentities", Fixtures.ok(Fixtures.text("wikidata_search_hampi.json")))
            .on("action=wbgetentities", Fixtures.ok(Fixtures.text("wikidata_entities_hampi.json")))
            .on("iiurlwidth=1280", HttpResponse(500, "oops"))
            .on("iiurlwidth=320", Fixtures.ok(Fixtures.text("commons_imageinfo_hampi_thumbs.json")))
            .on(listOf("action=parse", "page=Hampi"),
                HttpResponse(429, "", mapOf("Retry-After" to "1")),
                Fixtures.ok(Fixtures.parseResponse("Hampi", 4500101L, "wikivoyage_hampi.wikitext")))
        val sleeps = ArrayList<Long>()
        val src = WikimediaGuideSource(fetcher = fake, clock = { fixedNow }, ioDispatcher = Dispatchers.Unconfined,
            http = PoliteHttpClient(fake, sleeper = { synchronized(sleeps) { sleeps += it } }))
        val pack = src.buildPack(src.resolve("Hampi").first())
        assertNull(pack.destination.hero)
        assertEquals(17, pack.places.size)
        assertEquals(listOf(1000L), sleeps)
        assertEquals(2, fake.urls().count { it.contains("action=parse") })
    }

    @Test
    fun `no coordinates anywhere is an error`() {
        val fake = FakeHttpFetcher().on("action=wbgetentities", Fixtures.ok("""{"entities":{}}"""))
        assertThrows(GuideContentException::class.java) {
            runBlocking { source(fake).buildPack(candidate("Q1", "Nowhere", null, loc = null)) }
        }
    }

    @Test
    fun `blank query makes no request and empty search returns empty`() = runBlocking {
        val fake = FakeHttpFetcher().on("action=wbsearchentities", Fixtures.ok("""{"searchinfo":{"search":"zzz"},"search":[],"success":1}"""))
        val src = source(fake)
        assertTrue(src.resolve("   ").isEmpty())
        assertTrue(fake.requests.isEmpty())
        assertTrue(src.resolve("zzzqqq").isEmpty())
        assertEquals(1, fake.requests.size, "no wbgetentities when search is empty")
    }

    @Test
    fun `listing thumbnails can be disabled`() = runBlocking {
        val fake = hampiFake()
        val src = WikimediaGuideSource(fetcher = fake, fetchListingThumbs = false, clock = { fixedNow },
            ioDispatcher = Dispatchers.Unconfined, http = PoliteHttpClient(fake, sleeper = {}))
        val pack = src.buildPack(src.resolve("Hampi").first())
        assertNotNull(pack.destination.hero)
        assertTrue(pack.places.all { it.image == null })
        assertFalse(fake.urls().any { it.contains("iiurlwidth=320") })
    }
}
