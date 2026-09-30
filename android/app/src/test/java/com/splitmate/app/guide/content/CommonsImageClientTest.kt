package com.splitmate.app.guide.content

import com.splitmate.app.data.guide.GuideHttp
import com.splitmate.app.data.guide.content.CommonsImageClient
import com.splitmate.app.data.guide.content.PoliteHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommonsImageClientTest {

    @Test
    fun `hero credit extraction strips HTML from Artist`() {
        val map = CommonsImageClient.parseImageInfo(Fixtures.text("commons_imageinfo_hampi_hero.json"), 1280)
        val img = map.getValue("Virupaksha Temple, Hampi.jpg")
        assertEquals("Virupaksha Temple, Hampi.jpg", img.fileName)
        assertTrue(img.thumbUrl.startsWith("https://upload.wikimedia.org/"))
        assertTrue(img.thumbUrl.contains("1280px-"))
        assertEquals(1280, img.width)
        assertEquals("Example Photographer & R. Rao", img.artist)
        assertEquals("CC BY-SA 4.0", img.license)
        assertEquals("https://creativecommons.org/licenses/by-sa/4.0", img.licenseUrl)
        assertEquals("https://commons.wikimedia.org/wiki/File:Virupaksha_Temple,_Hampi.jpg", img.sourceUrl)
        assertFalse(img.artist!!.contains("<"))
    }

    @Test
    fun `batched thumbs - protocol-relative upgraded - missing skipped - PD without artist`() {
        val map = CommonsImageClient.parseImageInfo(Fixtures.text("commons_imageinfo_hampi_thumbs.json"), 320)
        assertEquals(setOf("Hemakuta hill temples.jpg", "Evolve Back Hampi.jpg"), map.keys)
        val h = map.getValue("Hemakuta hill temples.jpg")
        assertTrue(h.thumbUrl.startsWith("https://upload.wikimedia.org/"))
        assertEquals(320, h.width)
        assertEquals("Anonymous", h.artist)
        assertEquals("CC BY 2.0", h.license)
        assertEquals("https://creativecommons.org/licenses/by/2.0", h.licenseUrl)
        assertEquals("https://commons.wikimedia.org/wiki/File:Hemakuta_hill_temples.jpg", h.sourceUrl)
        val e = map.getValue("Evolve Back Hampi.jpg")
        assertNull(e.artist)
        assertEquals("Public domain", e.license)
        assertNull(e.licenseUrl)
    }

    @Test
    fun `url uses imageinfo params and standard widths`() {
        val u = CommonsImageClient.imageInfoUrl(listOf("A b.jpg", "C.png"), 1280)
        assertTrue(u.startsWith("https://commons.wikimedia.org/w/api.php?action=query"))
        listOf("titles=File%3AA%20b.jpg%7CFile%3AC.png", "prop=imageinfo", "iiprop=url%7Cextmetadata", "iiurlwidth=1280", "maxlag=5")
            .forEach { assertTrue(u.contains(it), "missing $it in $u") }
        assertEquals(640, CommonsImageClient.standardWidth(700))
        assertEquals(1280, CommonsImageClient.standardWidth(5000))
        assertEquals(320, CommonsImageClient.standardWidth(100))
        assertTrue(CommonsImageClient.imageInfoUrl(listOf("x.jpg"), 999).contains("iiurlwidth=1280"))
    }

    @Test
    fun `file name normalisation`() {
        assertEquals("Foo bar.jpg", CommonsImageClient.normalizeFileName("File:foo_bar.jpg"))
        assertEquals("X.png", CommonsImageClient.normalizeFileName(" image:X.png "))
        assertEquals("Hampi 2.jpg", CommonsImageClient.normalizeFileName("Hampi  2.jpg"))
        assertEquals("", CommonsImageClient.normalizeFileName("   "))
    }

    @Test
    fun `only https URLs survive`() {
        assertEquals("https://a.org/x", CommonsImageClient.httpsUrl("//a.org/x"))
        assertEquals("https://a.org/x", CommonsImageClient.httpsUrl("http://a.org/x"))
        assertNull(CommonsImageClient.httpsUrl("javascript:alert(1)"))
        assertNull(CommonsImageClient.httpsUrl("ftp://a.org/x"))
        assertNull(CommonsImageClient.httpsUrl(""))
    }

    @Test
    fun `client fetch goes through polite http with UA and requested width`() {
        val fake = FakeHttpFetcher().on("iiurlwidth=640", Fixtures.ok(Fixtures.text("commons_imageinfo_hampi_hero.json")))
        val client = CommonsImageClient(PoliteHttpClient(fake, sleeper = {}))
        val img = client.fetch("File:Virupaksha_Temple,_Hampi.jpg", CommonsImageClient.WIDTH_METERED)
        assertEquals("CC BY-SA 4.0", img!!.license)
        assertEquals(1, fake.requests.size)
        assertEquals(GuideHttp.USER_AGENT, fake.requests[0].headers["User-Agent"])
        assertNull(client.fetch("Nope.jpg", 640), "file not in response -> null")
        assertTrue(client.fetchMany(emptyList(), 320).isEmpty())
    }
}
