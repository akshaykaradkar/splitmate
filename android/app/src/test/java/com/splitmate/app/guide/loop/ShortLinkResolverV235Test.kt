package com.splitmate.app.guide.loop

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.GuideFeatureFlags
import com.splitmate.app.data.guide.StayResolution
import com.splitmate.app.data.guide.loop.FetchIdentity
import com.splitmate.app.data.guide.loop.HttpTextFetcher
import com.splitmate.app.data.guide.loop.HttpTextResponse
import com.splitmate.app.data.guide.loop.MapsPageCoordinateExtractor
import com.splitmate.app.data.guide.loop.MinIntervalRateLimiter
import com.splitmate.app.data.guide.loop.NominatimGeocoder
import com.splitmate.app.data.guide.loop.ProbeMethod
import com.splitmate.app.data.guide.loop.RedirectFetcher
import com.splitmate.app.data.guide.loop.RedirectProbe
import com.splitmate.app.data.guide.loop.ShortLinkResolver
import com.splitmate.app.data.guide.loop.SimpleUrl
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

/**
 * v2.3.5 issue #3: hotel Maps short link failing on a phone with "Couldn't reach Google Maps.
 * Check your connection." Covers GET-first hops, consent/sorry unwrapping, the flag-gated HTML
 * fallback, the automatic name search and accurate failure classification.
 */
@DisplayName("ShortLinkResolver v2.3.5: GET-first, unwrap, body fallback, honest errors")
class ShortLinkResolverV235Test {

    private data class Call(val method: ProbeMethod, val url: String, val identity: FetchIdentity, val maxBodyBytes: Int)

    /** Scripted v2.3.5 fetcher: implements [RedirectFetcher.fetch]; unknown URLs return 404. */
    private class FakeFetcher(
        private val script: (Call) -> RedirectProbe?
    ) : RedirectFetcher {
        val calls = mutableListOf<Call>()
        override suspend fun probe(url: String, method: ProbeMethod): RedirectProbe =
            fetch(url, method, FetchIdentity.APP, 0)

        override suspend fun fetch(url: String, method: ProbeMethod, identity: FetchIdentity, maxBodyBytes: Int): RedirectProbe {
            val call = Call(method, url, identity, maxBodyBytes)
            calls += call
            return script(call) ?: RedirectProbe(404, null)
        }
    }

    private val sleeps = mutableListOf<Long>()

    private fun resolver(
        fetcher: RedirectFetcher,
        flags: GuideFeatureFlags = GuideFeatureFlags(),
        online: Boolean? = null
    ) = ShortLinkResolver(fetcher, isOnline = { online }, sleeper = { sleeps += it }) { flags }

    private val shortLink = "https://maps.app.goo.gl/dkQxdkL37eLmLwiG9"

    /** The coordinate-less redirect Google sends some phones/mobile networks. */
    private val ftidUrl =
        "https://www.google.com/maps?q=Hemprabha+Bed+And+Breakfast,+Mirya+Road,+Ratnagiri&ftid=0x3bea5f0c:0x9a1b2c3d"

    private val placePageHtml = """
        <!DOCTYPE html><html><head>
        <title>Hemprabha Bed And Breakfast · Ratnagiri - Google Maps</title>
        <meta content="https://maps.google.com/maps/api/staticmap?center=16.9983921%2C73.3569574&amp;zoom=16&amp;size=900x900&amp;markers=16.9983921%2C73.3569574&amp;sensor=false" property="og:image">
        <meta content="Hemprabha Bed And Breakfast · Ratnagiri" property="og:title">
        </head><body><script>window.APP_INITIALIZATION_STATE=[[[3283.4,73.35,16.99],[0,0,0]]];</script></body></html>
    """.trimIndent()

    // ---------------------------------------------------------------------------------------------
    // GET first
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `first hop is a GET with the mobile browser identity and a 256 KB body budget`(): Unit = runBlocking {
        val pin = "https://www.google.com/maps/place/Hemprabha/data=!3d16.9983921!4d73.3569574"
        val f = FakeFetcher { c -> if (c.url == shortLink) RedirectProbe(302, pin) else null }
        val r = resolver(f).resolve(shortLink) as StayResolution.Resolved
        assertEquals(16.9983921, r.location.lat, 1e-9)
        assertEquals(73.3569574, r.location.lng, 1e-9)
        assertEquals(CoordinatePrecision.EXACT, r.precision)
        val first = f.calls.single()
        assertEquals(ProbeMethod.GET, first.method)
        assertEquals(FetchIdentity.BROWSER_MOBILE, first.identity)
        assertEquals(MapsPageCoordinateExtractor.MAX_BODY_BYTES, first.maxBodyBytes)
        assertTrue(f.calls.none { it.method == ProbeMethod.HEAD }, "no HEAD-first")
    }

    @Test
    fun `shortener answering the browser with a page is retried once with the app identity`(): Unit = runBlocking {
        val pin = "https://www.google.com/maps/place/X/data=!3d16.9983921!4d73.3569574"
        val f = FakeFetcher { c ->
            when {
                c.url == shortLink && c.identity == FetchIdentity.BROWSER_MOBILE -> RedirectProbe(200, null, "<html>open in app</html>")
                c.url == shortLink && c.identity == FetchIdentity.APP -> RedirectProbe(302, pin)
                else -> null
            }
        }
        val r = resolver(f).resolve(shortLink)
        assertTrue(r is StayResolution.Resolved, "got $r")
        assertEquals(listOf(FetchIdentity.BROWSER_MOBILE, FetchIdentity.APP), f.calls.map { it.identity })
    }

    @Test
    fun `transient IOException is retried once with a backoff`(): Unit = runBlocking {
        var attempts = 0
        val pin = "https://www.google.com/maps/place/X/data=!3d16.9983921!4d73.3569574"
        val f = FakeFetcher { c ->
            if (c.url == shortLink) {
                attempts++
                if (attempts == 1) throw SocketTimeoutException("read timed out")
                RedirectProbe(302, pin)
            } else {
                null
            }
        }
        val r = resolver(f).resolve(shortLink)
        assertTrue(r is StayResolution.Resolved, "got $r")
        assertEquals(2, attempts)
        assertEquals(listOf(ShortLinkResolver.RETRY_BACKOFF_MS), sleeps)
    }

    @Test
    fun `maps google cid link is followed and www google maps cid link now needs network`(): Unit = runBlocking {
        val cid = "https://maps.google.com/?cid=1234567890"
        val f = FakeFetcher { c -> if (c.url == cid) RedirectProbe(302, "https://www.google.com/maps/place/H/@16.99,73.35,17z/data=!3d16.9983921!4d73.3569574") else null }
        val r = resolver(f).resolve(cid) as StayResolution.Resolved
        assertEquals(CoordinatePrecision.EXACT, r.precision)
        assertTrue(ShortLinkResolver.needsNetwork(SimpleUrl.parse("https://www.google.com/maps?cid=42")))
        assertFalse(ShortLinkResolver.needsNetwork(SimpleUrl.parse("https://www.google.com/maps/place/Hampi/@15.3,76.4,14z")))
    }

    // ---------------------------------------------------------------------------------------------
    // consent / sorry unwrapping
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `consent continue is unwrapped without fetching the consent page`(): Unit = runBlocking {
        val consent = "https://consent.google.com/m?continue=https%3A%2F%2Fmaps.google.com%2F%3Fcid%3D777&gl=DE"
        val f = FakeFetcher { c ->
            when (c.url) {
                shortLink -> RedirectProbe(302, consent)
                "https://maps.google.com/?cid=777" -> RedirectProbe(302, "https://www.google.com/maps/place/H/data=!3d16.9983921!4d73.3569574")
                else -> null
            }
        }
        val r = resolver(f).resolve(shortLink) as StayResolution.Resolved
        assertEquals(73.3569574, r.location.lng, 1e-9)
        assertTrue(f.calls.none { it.url.contains("consent.google") })
    }

    @Test
    fun `google sorry page is unwrapped offline and its place name goes to the name search`(): Unit = runBlocking {
        val sorry = "https://www.google.com/sorry/index?continue=" +
            "https%3A%2F%2Fwww.google.com%2Fmaps%3Fq%3DHemprabha%2BBed%2BAnd%2BBreakfast%26ftid%3D0x1%3A0x2&q=EgQ"
        val f = FakeFetcher { c -> if (c.url == shortLink) RedirectProbe(302, sorry) else null }
        val r = resolver(f).resolve(shortLink) as StayResolution.NoCoordinates
        assertEquals("Hemprabha Bed And Breakfast", r.placeNameHint)
        assertEquals(1, f.calls.size, "neither the sorry page nor its continue URL is fetched")
    }

    @Test
    fun `google sorry continue with coordinates resolves`(): Unit = runBlocking {
        val sorry = "https://www.google.com/sorry/index?continue=" +
            "https%3A%2F%2Fwww.google.com%2Fmaps%2Fplace%2FH%2Fdata%3D!3d16.9983921!4d73.3569574"
        val f = FakeFetcher { c -> if (c.url == shortLink) RedirectProbe(302, sorry) else null }
        val r = resolver(f).resolve(shortLink) as StayResolution.Resolved
        assertEquals(16.9983921, r.location.lat, 1e-9)
    }

    @Test
    fun `google sorry without any hint is an accurate refusal, not a connection error`(): Unit = runBlocking {
        val sorry = "https://www.google.com/sorry/index?continue=https%3A%2F%2Fmaps.google.com%2F%3Fcid%3D9"
        val f = FakeFetcher { c -> if (c.url == shortLink) RedirectProbe(302, sorry) else null }
        val r = resolver(f).resolve(shortLink) as StayResolution.Rejected
        assertEquals(ShortLinkResolver.REASON_GOOGLE_REFUSED, r.reason)
        assertTrue(ShortLinkResolver.isSorryPage(SimpleUrl.parse(sorry)!!))
    }

    // ---------------------------------------------------------------------------------------------
    // HTML body fallback (flag-gated)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `ftid redirect page body yields the pin when body parsing is on`(): Unit = runBlocking {
        val f = FakeFetcher { c ->
            when (c.url) {
                shortLink -> RedirectProbe(302, ftidUrl)
                ftidUrl -> RedirectProbe(200, null, if (c.maxBodyBytes > 0) placePageHtml else null)
                else -> null
            }
        }
        val r = resolver(f).resolve(shortLink) as StayResolution.Resolved
        assertEquals(16.9983921, r.location.lat, 1e-9)
        assertEquals(73.3569574, r.location.lng, 1e-9)
        assertEquals(CoordinatePrecision.EXACT, r.precision, "static-map marker is the place pin")
        assertEquals("Hemprabha Bed And Breakfast, Mirya Road, Ratnagiri", r.label)
    }

    @Test
    fun `flag off disables body parsing and asks for no body`(): Unit = runBlocking {
        val f = FakeFetcher { c ->
            when (c.url) {
                shortLink -> RedirectProbe(302, ftidUrl)
                ftidUrl -> RedirectProbe(200, null, placePageHtml) // even if a body came back
                else -> null
            }
        }
        val off = GuideFeatureFlags(shortLinkBodyParseEnabled = false)
        val r = resolver(f, off).resolve(shortLink) as StayResolution.NoCoordinates
        assertEquals("Hemprabha Bed And Breakfast, Mirya Road, Ratnagiri", r.placeNameHint)
        assertTrue(f.calls.all { it.maxBodyBytes == 0 }, "no body budget when the flag is off")
    }

    @Test
    fun `extractor patterns - markers, null tuple, pin, center, app state, ll, viewport`() {
        fun ex(html: String) = MapsPageCoordinateExtractor.extract(html).coordinate

        val markers = ex("""<meta content="https://maps.google.com/maps/api/staticmap?center=1.5%2C2.5&amp;markers=color:red%7C16.9983921%2C73.3569574">""")
        assertEquals(16.9983921, markers!!.location.lat, 1e-9)
        assertEquals(CoordinatePrecision.EXACT, markers.precision)

        val tuple = ex("""foo,[null,null,16.9983921,73.3569574],bar""")
        assertEquals(73.3569574, tuple!!.location.lng, 1e-9)
        assertEquals(CoordinatePrecision.EXACT, tuple.precision)

        val pin = ex("""href="/maps/place/H/data=\u00213m1\u00214b1!3d16.9983921!4d73.3569574"""")
        assertEquals(CoordinatePrecision.EXACT, pin!!.precision)

        val center = ex("""staticmap?center=16.9983921%2C73.3569574&amp;zoom=15""")
        assertEquals(CoordinatePrecision.APPROXIMATE, center!!.precision)

        val state = ex("""window.APP_INITIALIZATION_STATE=[[[3283.4,73.3569574,16.9983921],[0,0,0]]]""")
        assertEquals(16.9983921, state!!.location.lat, 1e-9, "app state is [zoom, lng, lat]")
        assertEquals(73.3569574, state.location.lng, 1e-9)
        assertEquals(CoordinatePrecision.APPROXIMATE, state.precision)

        val ll = ex("""<a href="https://maps.google.com/maps?ll=16.9983921,73.3569574&amp;z=16">""")
        assertEquals(CoordinatePrecision.APPROXIMATE, ll!!.precision)

        val viewport = ex("""/maps/@16.9983921,73.3569574,17z""")
        assertEquals(CoordinatePrecision.APPROXIMATE, viewport!!.precision)

        assertNull(ex("<html><title>Google Maps</title></html>"))
        assertNull(ex("""[null,null,0.0000,0.0000]"""), "null island rejected")
        assertEquals(
            "Hemprabha Bed And Breakfast · Ratnagiri",
            MapsPageCoordinateExtractor.extract(placePageHtml).placeName
        )
    }

    @Test
    fun `page name is used as hint when the URLs carry none`(): Unit = runBlocking {
        val cidUrl = "https://maps.google.com/?cid=55"
        val html = """<meta content="Sea View Homestay" property="og:title"><p>no coordinates</p>"""
        val f = FakeFetcher { c ->
            when (c.url) {
                shortLink -> RedirectProbe(302, cidUrl)
                cidUrl -> RedirectProbe(200, null, html)
                else -> null
            }
        }
        val r = resolver(f).resolve(shortLink) as StayResolution.NoCoordinates
        assertEquals("Sea View Homestay", r.placeNameHint)
    }

    // ---------------------------------------------------------------------------------------------
    // ftid / name-only -> geocode fallback
    // ---------------------------------------------------------------------------------------------

    private class ScriptedText(private val bodies: List<HttpTextResponse>) : HttpTextFetcher {
        val urls = mutableListOf<String>()
        override suspend fun get(url: String, headers: Map<String, String>): HttpTextResponse {
            urls += url
            return bodies.getOrElse(urls.size - 1) { HttpTextResponse(200, "[]") }
        }
    }

    private fun fastLimiter(): MinIntervalRateLimiter {
        var now = 0L
        return MinIntervalRateLimiter(1_000L, clockMs = { now }, sleeper = { now += it })
    }

    @Test
    fun `name-only link falls back to the smart Nominatim search`(): Unit = runBlocking {
        // 1) the resolver ends with the place name (no pin, body parsing off for this check)
        val f = FakeFetcher { c -> if (c.url == shortLink) RedirectProbe(302, ftidUrl) else RedirectProbe(200, null) }
        val noCoords = resolver(f, GuideFeatureFlags(shortLinkBodyParseEnabled = false))
            .resolve(shortLink) as StayResolution.NoCoordinates
        val hint = noCoords.placeNameHint!!

        // 2) the geocoder tries the full hint first (empty), then name + destination (hit);
        //    name + locality equals the full hint here and is de-duplicated.
        val text = ScriptedText(
            listOf(
                HttpTextResponse(200, "[]"),
                HttpTextResponse(200, """[{"lat":"16.99812","lon":"73.35701","name":"Hemprabha B&B"},{"lat":"17.1","lon":"73.4"}]""")
            )
        )
        val g = NominatimGeocoder(text, { GuideFeatureFlags() }, fastLimiter())
        val r = g.geocodeSmart(hint, "Ratnagiri") as StayResolution.Resolved
        assertEquals(16.99812, r.location.lat, 1e-9)
        assertEquals(CoordinatePrecision.APPROXIMATE, r.precision)
        assertEquals("Hemprabha B&B", r.label)
        assertEquals(2, text.urls.size)
        assertTrue(text.urls.all { it.contains("&limit=3&") }, "limit 3 per query")
        assertTrue(text.urls[0].contains("Mirya%20Road"), "full hint first: ${text.urls[0]}")
    }

    @Test
    fun `smart queries - full, name plus locality, name plus destination - no truncation garbage`() {
        val q = NominatimGeocoder.smartQueries(
            "Hemprabha Bed And Breakfast, Plot 12, Mirya Road, Ratnagiri, Maharashtra 415612, India",
            "Ratnagiri"
        )
        assertEquals(
            listOf(
                "Hemprabha Bed And Breakfast, Plot 12, Mirya Road, Ratnagiri, Maharashtra 415612, India",
                "Hemprabha Bed And Breakfast, Ratnagiri, Maharashtra",
                "Hemprabha Bed And Breakfast, Ratnagiri"
            ),
            q
        )
        val cut = NominatimGeocoder.smartQueries("Hemprabha Bed And Breakfast, Mirya Road, Ratnag…", null)
        assertTrue(cut.none { it.contains("…") || it.contains("Ratnag") }, "partial last part dropped: $cut")
        assertEquals("Hemprabha Bed And Breakfast, Mirya Road", cut.first())
        assertTrue(cut.size <= NominatimGeocoder.MAX_SMART_QUERIES)
    }

    @Test
    fun `smart search with nothing found is NoCoordinates and all failures are Rejected`(): Unit = runBlocking {
        val empty = NominatimGeocoder(ScriptedText(emptyList()), { GuideFeatureFlags() }, fastLimiter())
        val r = empty.geocodeSmart("Unknown Guest House, Some Road, Ratnagiri", null) as StayResolution.NoCoordinates
        assertEquals("Unknown Guest House", r.placeNameHint)

        val failing = object : HttpTextFetcher {
            override suspend fun get(url: String, headers: Map<String, String>): HttpTextResponse = throw IOException("x")
        }
        val g = NominatimGeocoder(failing, { GuideFeatureFlags() }, fastLimiter())
        assertTrue(g.geocodeSmart("A, B", null) is StayResolution.Rejected)

        val off = NominatimGeocoder(ScriptedText(emptyList()), { GuideFeatureFlags(nominatimEnabled = false) }, fastLimiter())
        assertEquals(NominatimGeocoder.REASON_DISABLED, (off.geocodeSmart("A", null) as StayResolution.Rejected).reason)
    }

    // ---------------------------------------------------------------------------------------------
    // error classification
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `failures are classified and only real connectivity problems mention it`() {
        assertEquals(ShortLinkResolver.REASON_OFFLINE, ShortLinkResolver.classifyFailure(IOException("x"), online = false))
        assertEquals(ShortLinkResolver.REASON_CANT_CONNECT, ShortLinkResolver.classifyFailure(UnknownHostException("maps.app.goo.gl"), null))
        assertEquals(ShortLinkResolver.REASON_CANT_CONNECT, ShortLinkResolver.classifyFailure(ConnectException("refused"), true))
        assertEquals(ShortLinkResolver.REASON_TIMEOUT, ShortLinkResolver.classifyFailure(SocketTimeoutException("t"), true))
        assertEquals(ShortLinkResolver.REASON_SECURE_CONNECTION, ShortLinkResolver.classifyFailure(SSLHandshakeException("h"), true))
        assertEquals(ShortLinkResolver.REASON_NETWORK, ShortLinkResolver.classifyFailure(IOException("reset"), true))
        assertEquals(ShortLinkResolver.REASON_INVALID_REDIRECT, ShortLinkResolver.classifyFailure(IllegalArgumentException("bad url"), true))

        val all = listOf(
            ShortLinkResolver.REASON_CANT_CONNECT, ShortLinkResolver.REASON_TIMEOUT,
            ShortLinkResolver.REASON_SECURE_CONNECTION, ShortLinkResolver.REASON_GOOGLE_REFUSED,
            ShortLinkResolver.REASON_GOOGLE_UNAVAILABLE, ShortLinkResolver.REASON_NETWORK
        )
        assertTrue(all.none { it.contains("check your connection", ignoreCase = true) })
        assertTrue(all.all { it.contains("paste coordinates", ignoreCase = true) }, "every failure is actionable")
    }

    @Test
    fun `timeouts on both attempts end as a timeout, not a connection error`(): Unit = runBlocking {
        val f = FakeFetcher { throw SocketTimeoutException("connect timed out") }
        val r = resolver(f, online = true).resolve(shortLink) as StayResolution.Rejected
        assertEquals(ShortLinkResolver.REASON_TIMEOUT, r.reason)
        assertEquals(2, f.calls.size, "one retry")
    }

    @Test
    fun `device offline gives the offline message without a retry`(): Unit = runBlocking {
        val f = FakeFetcher { throw UnknownHostException("maps.app.goo.gl") }
        val r = resolver(f, online = false).resolve(shortLink) as StayResolution.Rejected
        assertEquals(ShortLinkResolver.REASON_OFFLINE, r.reason)
        assertEquals(1, f.calls.size)
        assertTrue(sleeps.isEmpty())
    }

    @Test
    fun `429 from Google is a refusal or a name-search handoff, never a dead link`(): Unit = runBlocking {
        val cid = "https://maps.google.com/?cid=77"
        val noHint = FakeFetcher { c -> if (c.url == shortLink) RedirectProbe(302, cid) else RedirectProbe(429, null) }
        assertEquals(
            ShortLinkResolver.REASON_GOOGLE_REFUSED,
            (resolver(noHint).resolve(shortLink) as StayResolution.Rejected).reason
        )
        val withHint = FakeFetcher { c -> if (c.url == shortLink) RedirectProbe(302, ftidUrl) else RedirectProbe(429, null) }
        val r = resolver(withHint).resolve(shortLink) as StayResolution.NoCoordinates
        assertEquals("Hemprabha Bed And Breakfast, Mirya Road, Ratnagiri", r.placeNameHint)
    }

    @Test
    fun `hop 2 timeout after hop 1 redirect preserves place name hint for automatic geocoding`(): Unit = runBlocking {
        val f = FakeFetcher { c ->
            when (c.url) {
                shortLink -> RedirectProbe(302, ftidUrl)
                ftidUrl -> throw SocketTimeoutException("read timed out on www.google.com")
                else -> null
            }
        }
        val r = resolver(f, online = true).resolve(shortLink) as StayResolution.NoCoordinates
        assertEquals("Hemprabha Bed And Breakfast, Mirya Road, Ratnagiri", r.placeNameHint)
    }

    @Test
    fun `fallback locality queries resolve street and city when small B&B name is not in OSM`(): Unit = runBlocking {
        val text = ScriptedText(
            listOf(
                HttpTextResponse(200, "[]"), // full
                HttpTextResponse(200, "[]"), // name + locality
                HttpTextResponse(200, """[{"lat":"17.0052","lon":"73.2814","name":"Mirya Road"}]""") // locality fallback
            )
        )
        val g = NominatimGeocoder(text, { GuideFeatureFlags() }, fastLimiter())
        val r = g.geocodeSmart("Hemprabha Bed And Breakfast, Mirya Road, Ratnagiri", "Ratnagiri") as StayResolution.Resolved
        assertEquals(17.0052, r.location.lat, 1e-9)
        assertEquals(73.2814, r.location.lng, 1e-9)
        assertEquals(CoordinatePrecision.APPROXIMATE, r.precision)
    }
}

