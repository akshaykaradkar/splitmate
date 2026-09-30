package com.splitmate.app.guide.content

import com.splitmate.app.data.guide.CommonsImage
import com.splitmate.app.data.guide.Destination
import com.splitmate.app.data.guide.GuidePack
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.data.guide.WikivoyageRef
import com.splitmate.app.data.guide.content.GuidePackCodec
import com.splitmate.app.data.guide.content.GuidePackFormatException
import com.splitmate.app.data.guide.content.WikivoyageListingParser
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class GuidePackCodecTest {

    private val hero = CommonsImage(
        fileName = "Virupaksha Temple, Hampi.jpg",
        thumbUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/V.jpg/1280px-V.jpg",
        width = 1280, artist = "Example Photographer", license = "CC BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
        sourceUrl = "https://commons.wikimedia.org/wiki/File:V.jpg"
    )

    private fun hampiPack(): GuidePack {
        val places = WikivoyageListingParser.parse("Hampi", 4500101L, Fixtures.text("wikivoyage_hampi.wikitext")).places
            .mapIndexed { i, p -> if (i == 1) p.copy(image = hero.copy(width = 320, artist = null), distanceKmFromCenter = 0.12) else p }
        return GuidePack(
            destination = Destination("Q9000101", "Hampi", "village in Karnataka, India", LatLng(15.335, 76.46), hero),
            wikivoyage = WikivoyageRef("Hampi", 4500101L, "https://en.wikivoyage.org/wiki/Hampi"),
            places = places,
            nearby = listOf(
                Place(id = "wd:Q123", kind = PlaceKind.SEE, name = "Achyutaraya Temple", location = LatLng(15.3401, 76.4672),
                    qid = "Q123", source = PlaceSource.WIKIDATA, distanceKmFromCenter = 1.2345678901234)
            ),
            attribution = listOf("Wikivoyage (CC BY-SA 4.0)", "Wikidata (CC0)", "Photo \"V.jpg\" by X"),
            fetchedAtEpochMs = 1_727_600_000_000L
        )
    }

    @Test
    fun `round trip is lossless through JSON and gzip`() {
        val p = hampiPack()
        assertEquals(p, GuidePackCodec.fromJson(GuidePackCodec.toCanonicalJson(p)))
        assertEquals(p, GuidePackCodec.decodeGzip(GuidePackCodec.encodeGzip(p)))
    }

    @Test
    fun `canonical JSON has stable key order and schema first`() {
        val json = GuidePackCodec.toCanonicalJson(hampiPack())
        assertTrue(json.startsWith("{\"schema\":\"splitmate.guide/1\",\"destination\":{\"qid\":\"Q9000101\",\"label\":\"Hampi\""), json.take(120))
        val order = listOf("\"destination\":", "\"wikivoyage\":", "\"places\":", "\"nearby\":", "\"attribution\":", "\"fetchedAt\":")
        val idx = order.map { json.indexOf(it) }
        assertTrue(idx.all { it > 0 } && idx == idx.sorted(), "top-level key order $idx")
        assertTrue(json.contains("{\"id\":\"wv:see:virupaksha-temple\",\"kind\":\"SEE\",\"name\":\"Virupaksha Temple\",\"lat\":15.335,\"lng\":76.46"))
        assertFalse(json.contains("\n"), "no insignificant whitespace")
    }

    @Test
    fun `decoding tolerates any key order`() {
        val p = hampiPack()
        val shuffled = JSONObject(GuidePackCodec.toCanonicalJson(p)).toString(2) // org.json: hash order + whitespace
        assertEquals(p, GuidePackCodec.fromJson(shuffled))
    }

    @Test
    fun `content hash is stable and sensitive`() {
        val p = hampiPack()
        val h1 = GuidePackCodec.contentHash(p)
        assertEquals(h1, GuidePackCodec.contentHash(hampiPack()))
        assertTrue(Regex("^[0-9a-f]{64}$").matches(h1))
        assertNotEquals(h1, GuidePackCodec.contentHash(p.copy(fetchedAtEpochMs = p.fetchedAtEpochMs + 1)))
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            GuidePackCodec.sha256Hex(""), "sha256 known vector"
        )
    }

    @Test
    fun `gzip is deterministic and smaller than raw and within 20 KB budget`() {
        val p = hampiPack()
        val a = GuidePackCodec.encode(p)
        val b = GuidePackCodec.encode(p)
        assertArrayEquals(a.gzip, b.gzip)
        assertTrue(a.gzip.size < a.rawBytes, "gz=${a.gzip.size} raw=${a.rawBytes}")
        assertTrue(a.gzip.size <= 20 * 1024)
        assertTrue(a.rawBytes <= GuidePackCodec.TARGET_RAW_BYTES)
        assertFalse(a.trimmed)
        assertTrue(a.warnings.isEmpty())
        assertEquals(GuidePackCodec.sha256Hex(a.canonicalJson), a.contentHash)
        assertEquals(p, a.pack)
    }

    @Test
    fun `null fields are omitted and restored as null`() {
        val p = hampiPack().copy(wikivoyage = null, destination = hampiPack().destination.copy(hero = null, description = null))
        val json = GuidePackCodec.toCanonicalJson(p)
        assertFalse(json.contains("\"wikivoyage\""))
        assertFalse(json.contains("\"hero\""))
        val back = GuidePackCodec.fromJson(json)
        assertNull(back.wikivoyage)
        assertNull(back.destination.hero)
        assertNull(back.destination.description)
        assertEquals(p, back)
    }

    @Test
    fun `special characters escape and round trip`() {
        val tricky = "Quote \" backslash \\ newline \n tab \t ctrl \u0001 sep \u2028 ₹ ಹಂಪೆ 😀"
        val p = hampiPack().copy(attribution = listOf(tricky))
        val json = GuidePackCodec.toCanonicalJson(p)
        assertTrue(json.contains("\\u0001") && json.contains("\\u2028"))
        assertEquals(tricky, GuidePackCodec.fromJson(json).attribution.single())
    }

    @Test
    fun `size budget trims oversize packs below 64 KB`() {
        val blurb = "Lorem ipsum dolor sit amet, ".repeat(10).take(280)
        val many = (0 until 260).map { i ->
            Place(id = "wv:see:p$i", kind = if (i % 5 == 0) PlaceKind.SLEEP else PlaceKind.SEE, name = "Place number $i",
                location = LatLng(15.0 + i / 1000.0, 76.0), blurb = blurb, hours = "9AM–5PM", price = "₹40",
                address = "Somewhere road", source = PlaceSource.WIKIVOYAGE)
        }
        val p = hampiPack().copy(places = many)
        assertTrue(GuidePackCodec.rawSize(p) > GuidePackCodec.MAX_RAW_BYTES)
        val enc = GuidePackCodec.encode(p)
        assertTrue(enc.trimmed)
        assertTrue(enc.rawBytes <= GuidePackCodec.MAX_RAW_BYTES, "raw=${enc.rawBytes}")
        assertTrue(enc.warnings.isNotEmpty())
        assertEquals(p.destination, enc.pack.destination)
        assertEquals(enc.pack, GuidePackCodec.decodeGzip(enc.gzip))
        assertTrue(enc.pack.places.all { (it.blurb?.length ?: 0) <= 140 })
    }

    @Test
    fun `explicit tiny budget drops trailing places`() {
        val p = hampiPack()
        val (fitted, warnings) = GuidePackCodec.enforceBudget(p, maxRawBytes = 1_500)
        assertTrue(GuidePackCodec.rawSize(fitted) <= 1_500)
        assertTrue(fitted.places.size < p.places.size)
        assertTrue(fitted.nearby.isEmpty())
        assertTrue(warnings.last().startsWith("dropped trailing places"))
    }

    @Test
    fun `corrupt and wrong schema and oversize inputs are rejected`() {
        assertThrows(GuidePackFormatException::class.java) { GuidePackCodec.decodeGzip(byteArrayOf(1, 2, 3)) }
        assertThrows(GuidePackFormatException::class.java) { GuidePackCodec.fromJson("{\"schema\":\"splitmate.guide/9\"}") }
        assertThrows(GuidePackFormatException::class.java) { GuidePackCodec.fromJson("[]") }
        val bomb = ByteArrayOutputStream().also { bos -> GZIPOutputStream(bos).use { it.write(ByteArray(2_000_000)) } }.toByteArray()
        assertTrue(bomb.size < 10_000)
        assertThrows(GuidePackFormatException::class.java) { GuidePackCodec.gunzip(bomb) }
    }
}
