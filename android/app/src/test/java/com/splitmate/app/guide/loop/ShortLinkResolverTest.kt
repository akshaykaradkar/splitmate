package com.splitmate.app.guide.loop

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.GuideFeatureFlags
import com.splitmate.app.data.guide.StayResolution
import com.splitmate.app.data.guide.loop.ProbeMethod
import com.splitmate.app.data.guide.loop.RedirectFetcher
import com.splitmate.app.data.guide.loop.RedirectProbe
import com.splitmate.app.data.guide.loop.ShortLinkResolver
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException

@DisplayName("ShortLinkResolver: allow-listed manual redirects, no bodies")
class ShortLinkResolverTest {

    /** Scripted fetcher: (method, url) -> probe. Unknown URLs return 404. Records every call. */
    private class FakeRedirectFetcher(
        private val script: (ProbeMethod, String) -> RedirectProbe?
    ) : RedirectFetcher {
        val calls = mutableListOf<Pair<ProbeMethod, String>>()
        override suspend fun probe(url: String, method: ProbeMethod): RedirectProbe {
            calls += method to url
            return script(method, url) ?: RedirectProbe(404, null)
        }
    }

    private fun redirects(vararg hops: Pair<String, String>): FakeRedirectFetcher {
        val map = hops.toMap()
        return FakeRedirectFetcher { _, url -> map[url]?.let { RedirectProbe(302, it) } ?: RedirectProbe(200, null) }
    }

    private fun resolver(fetcher: RedirectFetcher, flags: GuideFeatureFlags = GuideFeatureFlags()) =
        ShortLinkResolver(fetcher) { flags }

    private val placeWithPin =
        "https://www.google.com/maps/place/Hotel+Mayura+Bhuvaneshwari/@15.3345,76.4562,17z/data=!3m1!4b1!4m6!3m5!1s0x0:0x0!8m2!3d15.3350123!4d76.4600456"

    @Test
    fun `short link redirecting to place pin resolves EXACT with name label`(): Unit = runBlocking {
        val f = redirects("https://maps.app.goo.gl/abc123" to placeWithPin)
        val r = resolver(f).resolve("https://maps.app.goo.gl/abc123") as StayResolution.Resolved
        assertEquals(15.3350123, r.location.lat, 1e-9)
        assertEquals(76.4600456, r.location.lng, 1e-9)
        assertEquals(CoordinatePrecision.EXACT, r.precision)
        assertEquals("Hotel Mayura Bhuvaneshwari", r.label)
        assertEquals(listOf(ProbeMethod.HEAD to "https://maps.app.goo.gl/abc123"), f.calls)
    }

    @Test
    fun `ftid-only final url yields NoCoordinates with q-text hint`(): Unit = runBlocking {
        val finalUrl =
            "https://maps.google.com/?q=Hotel+Mayura+Bhuvaneshwari,+Kamalapur&ftid=0x3bb77e1ec1f4b2a5:0x6b1b5f4b0c8e1a2d"
        val f = redirects("https://maps.app.goo.gl/ftid1" to finalUrl)
        val r = resolver(f).resolve("https://maps.app.goo.gl/ftid1") as StayResolution.NoCoordinates
        assertEquals(finalUrl, r.finalUrl)
        assertEquals("Hotel Mayura Bhuvaneshwari, Kamalapur", r.placeNameHint)
    }

    @Test
    fun `share text name is used as hint when the link has no coordinates`(): Unit = runBlocking {
        val f = redirects("https://maps.app.goo.gl/xyz" to "https://maps.google.com/?cid=1234567890")
        val r = resolver(f).resolve("Hotel Mayura Bhuvaneshwari\nhttps://maps.app.goo.gl/xyz") as StayResolution.NoCoordinates
        assertEquals("Hotel Mayura Bhuvaneshwari", r.placeNameHint)
        assertEquals("https://maps.google.com/?cid=1234567890", r.finalUrl)
    }

    @Test
    fun `multi-hop chain ending in viewport-only url resolves APPROXIMATE`(): Unit = runBlocking {
        val f = redirects(
            "https://maps.app.goo.gl/v1" to "https://maps.google.com/?cid=42",
            "https://maps.google.com/?cid=42" to "https://www.google.com/maps/place/Hampi/@15.335,76.46,14z"
        )
        val r = resolver(f).resolve("https://maps.app.goo.gl/v1") as StayResolution.Resolved
        assertEquals(CoordinatePrecision.APPROXIMATE, r.precision)
        assertEquals(15.335, r.location.lat, 1e-9)
        assertEquals("Hampi", r.label)
    }

    @Test
    fun `consent interstitial is unwrapped via continue param without being fetched`(): Unit = runBlocking {
        val consent = "https://consent.google.com/ml?continue=https%3A%2F%2Fmaps.google.com%2F%3Fcid%3D777&gl=IN&hl=en"
        val f = redirects(
            "https://maps.app.goo.gl/eu1" to consent,
            "https://maps.google.com/?cid=777" to placeWithPin
        )
        val r = resolver(f).resolve("https://maps.app.goo.gl/eu1") as StayResolution.Resolved
        assertEquals(CoordinatePrecision.EXACT, r.precision)
        assertFalse(f.calls.any { it.second.contains("consent.google.com") }, "consent page must not be fetched")
        assertEquals(2, f.calls.size)
    }

    @Test
    fun `consent continue carrying coordinates resolves without further requests`(): Unit = runBlocking {
        val consent = "https://consent.google.com/ml?continue=https%3A%2F%2Fwww.google.com%2Fmaps%2Fplace%2FX%2Fdata%3D!3d15.335!4d76.46&gl=DE"
        val f = redirects("https://maps.app.goo.gl/eu2" to consent)
        val r = resolver(f).resolve("https://maps.app.goo.gl/eu2") as StayResolution.Resolved
        assertEquals(15.335, r.location.lat, 1e-9)
        assertEquals(1, f.calls.size)
    }

    @Test
    fun `HEAD refused with 405 falls back to GET`(): Unit = runBlocking {
        val f = FakeRedirectFetcher { method, url ->
            when {
                url == "https://maps.app.goo.gl/h405" && method == ProbeMethod.HEAD -> RedirectProbe(405, null)
                url == "https://maps.app.goo.gl/h405" && method == ProbeMethod.GET -> RedirectProbe(302, placeWithPin)
                else -> RedirectProbe(200, null)
            }
        }
        val r = resolver(f).resolve("https://maps.app.goo.gl/h405")
        assertTrue(r is StayResolution.Resolved)
        assertEquals(
            listOf(ProbeMethod.HEAD to "https://maps.app.goo.gl/h405", ProbeMethod.GET to "https://maps.app.goo.gl/h405"),
            f.calls
        )
    }

    @Test
    fun `more than 5 redirects is Rejected`(): Unit = runBlocking {
        // l0 -> l1 -> ... forever
        val f = FakeRedirectFetcher { _, url ->
            val n = url.substringAfterLast("/l").toInt()
            RedirectProbe(301, "https://maps.app.goo.gl/l${n + 1}")
        }
        val r = resolver(f).resolve("https://maps.app.goo.gl/l0") as StayResolution.Rejected
        assertEquals(ShortLinkResolver.REASON_TOO_MANY_REDIRECTS, r.reason)
        assertEquals(ShortLinkResolver.MAX_REDIRECTS + 1, f.calls.size)
    }

    @Test
    fun `exactly 5 redirects is still followed`(): Unit = runBlocking {
        val f = FakeRedirectFetcher { _, url ->
            if (!url.contains("/l")) {
                RedirectProbe(200, null)
            } else {
                val n = url.substringAfterLast("/l").toInt()
                if (n < 4) RedirectProbe(302, "https://maps.app.goo.gl/l${n + 1}") else RedirectProbe(302, placeWithPin)
            }
        }
        val r = resolver(f).resolve("https://maps.app.goo.gl/l0")
        assertTrue(r is StayResolution.Resolved, "got $r")
        assertEquals(5, f.calls.size)
    }

    @Test
    fun `http input is Rejected without network`(): Unit = runBlocking {
        val f = redirects()
        val r = resolver(f).resolve("http://maps.app.goo.gl/abc") as StayResolution.Rejected
        assertEquals(ShortLinkResolver.REASON_INSECURE, r.reason)
        assertTrue(f.calls.isEmpty())
    }

    @Test
    fun `redirect to http is Rejected`(): Unit = runBlocking {
        val f = redirects("https://maps.app.goo.gl/down" to "http://maps.google.com/?q=15.335,76.46")
        val r = resolver(f).resolve("https://maps.app.goo.gl/down") as StayResolution.Rejected
        assertEquals(ShortLinkResolver.REASON_INSECURE, r.reason)
    }

    @Test
    fun `non-Google input host is Rejected without network`(): Unit = runBlocking {
        val f = redirects()
        for (input in listOf(
            "https://bit.ly/3abcd",
            "https://evil.example/maps/place/X/data=!3d15.335!4d76.46",
            "https://www.google.com.evil.example/maps",
            "https://maps.google.com@evil.example/x",
            "https://www.google.com/search?q=hampi",
            "https://goo.gl/abc"
        )) {
            val r = resolver(f).resolve(input)
            assertTrue(r is StayResolution.Rejected, "expected Rejected for $input, got $r")
        }
        assertTrue(f.calls.isEmpty())
    }

    @Test
    fun `redirect leaving Google is Rejected`(): Unit = runBlocking {
        val f = redirects("https://maps.app.goo.gl/evil" to "https://evil.example/landing?q=15.335,76.46")
        val r = resolver(f).resolve("https://maps.app.goo.gl/evil") as StayResolution.Rejected
        assertEquals(ShortLinkResolver.REASON_LEFT_GOOGLE, r.reason)
    }

    @Test
    fun `relative Location header is resolved against the current origin`(): Unit = runBlocking {
        val f = redirects("https://maps.google.com/?cid=9" to "/maps/place/Hampi/data=!3d15.335!4d76.46")
        val r = resolver(f).resolve("https://maps.google.com/?cid=9") as StayResolution.Resolved
        assertEquals(76.46, r.location.lng, 1e-9)
    }

    @Test
    fun `g co kgs link following to google search ends as NoCoordinates`(): Unit = runBlocking {
        val f = redirects("https://g.co/kgs/AbCdEf" to "https://www.google.com/search?kgmid=/g/11abc&hl=en")
        val r = resolver(f).resolve("https://g.co/kgs/AbCdEf")
        assertTrue(r is StayResolution.NoCoordinates, "got $r")
    }

    @Test
    fun `dead first hop is Rejected`(): Unit = runBlocking {
        val f = FakeRedirectFetcher { _, _ -> RedirectProbe(404, null) }
        val r = resolver(f).resolve("https://goo.gl/maps/oldLink") as StayResolution.Rejected
        assertEquals(ShortLinkResolver.REASON_DEAD_LINK, r.reason)
    }

    @Test
    fun `network failure is Rejected with a friendly reason`(): Unit = runBlocking {
        val f = object : RedirectFetcher {
            override suspend fun probe(url: String, method: ProbeMethod): RedirectProbe = throw IOException("boom")
        }
        val r = ShortLinkResolver(f) { GuideFeatureFlags() }.resolve("https://maps.app.goo.gl/x") as StayResolution.Rejected
        assertEquals(ShortLinkResolver.REASON_NETWORK, r.reason)
    }

    @Test
    fun `kill-switch off parses pasted text only`(): Unit = runBlocking {
        val f = redirects("https://maps.app.goo.gl/abc123" to placeWithPin)
        val off = GuideFeatureFlags(shortLinkParsingEnabled = false)
        val r1 = resolver(f, off).resolve("Hotel Mayura\nhttps://maps.app.goo.gl/abc123") as StayResolution.NoCoordinates
        assertEquals("https://maps.app.goo.gl/abc123", r1.finalUrl)
        assertEquals("Hotel Mayura", r1.placeNameHint)
        val r2 = resolver(f, off).resolve("15.335, 76.46") as StayResolution.Resolved
        assertEquals(CoordinatePrecision.EXACT, r2.precision)
        assertTrue(f.calls.isEmpty(), "no network when the kill-switch is off")
    }

    @Test
    fun `pasted coordinates and full urls with pins never touch the network`(): Unit = runBlocking {
        val f = redirects()
        val inputs = listOf(
            "15.335, 76.46",
            "15°20'06\"N 76°27'36\"E",
            "geo:15.335,76.46",
            placeWithPin,
            "https://maps.google.com/?q=15.335,76.46"
        )
        for (input in inputs) {
            val r = resolver(f).resolve(input)
            assertTrue(r is StayResolution.Resolved && r.precision == CoordinatePrecision.EXACT, "for $input got $r")
        }
        assertTrue(f.calls.isEmpty())
    }

    @Test
    fun `full google maps url with viewport only is parsed offline as APPROXIMATE`(): Unit = runBlocking {
        val f = redirects()
        val r = resolver(f).resolve("https://www.google.com/maps/place/Hampi/@15.335,76.46,14z") as StayResolution.Resolved
        assertEquals(CoordinatePrecision.APPROXIMATE, r.precision)
        assertTrue(f.calls.isEmpty())
    }

    @Test
    fun `bare short link without scheme is upgraded to https`(): Unit = runBlocking {
        val f = redirects("https://maps.app.goo.gl/bare1" to placeWithPin)
        val r = resolver(f).resolve("Stay here: maps.app.goo.gl/bare1")
        assertTrue(r is StayResolution.Resolved, "got $r")
        assertEquals("https://maps.app.goo.gl/bare1", f.calls.first().second)
    }

    @Test
    fun `empty and nonsense input is Rejected`(): Unit = runBlocking {
        val f = redirects()
        assertTrue(resolver(f).resolve("   ") is StayResolution.Rejected)
        assertTrue(resolver(f).resolve("the hotel near the temple") is StayResolution.Rejected)
    }

    @Test
    fun `allow-list helpers`() {
        assertEquals("https://maps.app.goo.gl/a", ShortLinkResolver.findFirstUrl("see https://maps.app.goo.gl/a)."))
        assertNull(ShortLinkResolver.findFirstUrl("no link here"))
    }
}
