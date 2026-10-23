package com.splitmate.app.guide.loop

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.GuideFeatureFlags
import com.splitmate.app.data.guide.StayResolution
import com.splitmate.app.data.guide.loop.FetchIdentity
import com.splitmate.app.data.guide.loop.GoogleDirectEdgeDns
import com.splitmate.app.data.guide.loop.HttpTextFetcher
import com.splitmate.app.data.guide.loop.HttpTextResponse
import com.splitmate.app.data.guide.loop.MapLinkCoordinateParser
import com.splitmate.app.data.guide.loop.MapsPageCoordinateExtractor
import com.splitmate.app.data.guide.loop.MinIntervalRateLimiter
import com.splitmate.app.data.guide.loop.NominatimGeocoder
import com.splitmate.app.data.guide.loop.ProbeMethod
import com.splitmate.app.data.guide.loop.RacingRedirectFetcher
import com.splitmate.app.data.guide.loop.RedirectFetcher
import com.splitmate.app.data.guide.loop.RedirectProbe
import com.splitmate.app.data.guide.loop.ShortLinkResolver
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * v2.4.0 rc16: the user's real link https://maps.app.goo.gl/m18sW89UNW9NqgXM8 (2026-10-21) redirects
 * to a `/maps/place/<address>/data=…!1s0x…:0x…` URL with NO coordinates (place id only; the page
 * has none either), so the stay has to come from a place-name search. And on the user's phone the
 * first hop timed out twice. Fixes: a second HTTP stack races each hop, the error carries the
 * technical trace, and the name search skips "Shop No …"/"PROP <owner>" noise and falls back to the
 * locality / postcode.
 */
@DisplayName("v2.4.0 rc16: stay link - real place-id link, racing stacks, smarter name search")
class StayLinkRc16Test {

    private val hint = "Shop No 504, Hemprabha Bed And Breakfast PROP PRASHANT KOPARDE, near RAILWAY STATION, CORNER, " +
        "Kuwarbav, Ratnagiri, Maharashtra 415639"
    private val redirect = "https://www.google.com/maps/place/Shop+No+504,+Hemprabha+Bed+And+Breakfast+PROP+PRASHANT+KOPARDE," +
        "+near+RAILWAY+STATION,+CORNER,+Kuwarbav,+Ratnagiri,+Maharashtra+415639/data=!4m2!3m1!1s0x3bea0d22e1412a97:0x9395d694c999e8da!18m1!1e1?utm_source=mstt_1"

    @Test
    fun `the real place-id link yields the address as a search hint, not an error`() = runBlocking {
        val fetcher = object : RedirectFetcher {
            override suspend fun probe(url: String, method: ProbeMethod) = fetch(url, method, FetchIdentity.APP, 0)
            override suspend fun fetch(url: String, method: ProbeMethod, identity: FetchIdentity, maxBodyBytes: Int): RedirectProbe =
                if (url.contains("maps.app.goo.gl")) RedirectProbe(302, redirect) else RedirectProbe(200, null, "<html>no coordinates here</html>")
        }
        val r = ShortLinkResolver(fetcher, isOnline = { true }, flags = { GuideFeatureFlags() })
            .resolve("https://maps.app.goo.gl/m18sW89UNW9NqgXM8") as StayResolution.NoCoordinates
        assertTrue(r.placeNameHint!!.contains("Hemprabha Bed And Breakfast"), r.placeNameHint)
        assertTrue(r.placeNameHint!!.contains("Kuwarbav"), r.placeNameHint)
    }

    @Test
    fun `name search skips shop number and owner, then falls back to the locality and postcode`() {
        assertEquals("Hemprabha Bed And Breakfast", NominatimGeocoder.placeName(hint))
        val plan = NominatimGeocoder.fullPlan(hint, null).map { it.text }
        // Query 1 is the whole address (v2.3.5 contract); after that, "Shop No 504" is never treated as the name.
        assertTrue(plan.first().startsWith("Shop No 504, Hemprabha"), "$plan")
        assertTrue(plan.drop(1).none { it.startsWith("Shop No") }, "never searches 'Shop No 504, …' as the name: $plan")
        assertTrue(plan.contains("Hemprabha Bed And Breakfast, Ratnagiri, Maharashtra"), "$plan")
        assertTrue(plan.none { it.contains("PROP", ignoreCase = false) && !it.contains(",") }, "$plan")
        assertTrue(plan.any { it.startsWith("Kuwarbav, Ratnagiri") }, "locality fallback: $plan")
        assertTrue(plan.any { it.startsWith("415639") }, "postcode last resort: $plan")
        assertTrue(plan.size <= NominatimGeocoder.MAX_SMART_QUERIES)
        assertTrue(plan.none { it.contains("CORNER") && it.startsWith("CORNER") }, "position words are not places: $plan")
    }

    @Test
    fun `when the business is not in OpenStreetMap the area is found and marked approximate`() = runBlocking {
        val urls = ArrayList<String>()
        val text = object : HttpTextFetcher {
            override suspend fun get(url: String, headers: Map<String, String>): HttpTextResponse {
                urls += url
                return if (url.contains("Kuwarbav")) HttpTextResponse(200, """[{"lat":"16.9951712","lon":"73.3490283","name":"Kuwarbav"}]""")
                else HttpTextResponse(200, "[]")
            }
        }
        val r = NominatimGeocoder(text, { GuideFeatureFlags() }, MinIntervalRateLimiter(0L)).geocodeSmart(hint, null) as StayResolution.Resolved
        assertEquals(16.9951712, r.location.lat, 1e-9)
        assertEquals(CoordinatePrecision.APPROXIMATE, r.precision, "the preview asks the user to confirm")
        assertTrue(urls.size <= NominatimGeocoder.MAX_SMART_QUERIES)
    }

    private fun scripted(block: suspend () -> RedirectProbe) = object : RedirectFetcher {
        override suspend fun probe(url: String, method: ProbeMethod) = fetch(url, method, FetchIdentity.APP, 0)
        override suspend fun fetch(url: String, method: ProbeMethod, identity: FetchIdentity, maxBodyBytes: Int) = block()
    }

    @Test
    fun `a stalled primary stack is overtaken by the platform stack after the head start`() = runTest {
        var clock = 0L
        val racing = RacingRedirectFetcher(
            primary = scripted { awaitCancellation() },
            secondary = scripted { delay(300); RedirectProbe(302, redirect) },
            clockMs = { currentTime }
        )
        val r = racing.fetch("https://maps.app.goo.gl/x", ProbeMethod.GET, FetchIdentity.BROWSER_MOBILE, 0)
        assertEquals(302, r.statusCode)
        assertEquals(RacingRedirectFetcher.HEAD_START_MS + 300, currentTime, "platform lane starts after the 1.5 s head start")
        assertTrue(racing.trace().any { it.contains("platform 302") }, racing.trace().toString())
        clock++
    }

    @Test
    fun `a fast primary failure starts the platform stack at once, and both failing reports both`() = runTest {
        val racing = RacingRedirectFetcher(
            primary = scripted { delay(100); throw SocketTimeoutException("connect timed out") },
            secondary = scripted { delay(200); RedirectProbe(302, redirect) },
            clockMs = { currentTime }
        )
        assertEquals(302, racing.fetch("https://maps.app.goo.gl/x", ProbeMethod.GET, FetchIdentity.BROWSER_MOBILE, 0).statusCode)
        assertEquals(300, currentTime, "no head-start wait after the primary already failed")

        val bothFail = RacingRedirectFetcher(
            primary = scripted { throw SocketTimeoutException("connect timed out") },
            secondary = scripted { throw IOException("unexpected end of stream") },
            clockMs = { currentTime }
        )
        val e = runCatching { bothFail.fetch("https://maps.app.goo.gl/x", ProbeMethod.GET, FetchIdentity.BROWSER_MOBILE, 0) }.exceptionOrNull()
        assertTrue(e is SocketTimeoutException, "primary's error is thrown: $e")
        assertEquals(1, e!!.suppressed.size, "secondary's error attached")
        assertEquals(2, bothFail.trace().size)
    }

    @Test
    fun `when every attempt fails the error carries a technical detail line`() = runTest {
        val racing = RacingRedirectFetcher(
            primary = scripted { throw SocketTimeoutException("connect timed out") },
            secondary = scripted { throw SocketTimeoutException("timeout") },
            clockMs = { currentTime }
        )
        val r = ShortLinkResolver(racing, isOnline = { true }, flags = { GuideFeatureFlags() }, sleeper = { delay(it) })
            .resolve("https://maps.app.goo.gl/m18sW89UNW9NqgXM8") as StayResolution.Rejected
        assertNotNull(r.detail)
        assertTrue(r.detail!!.contains("maps.app.goo.gl okhttp SocketTimeoutException"), r.detail)
        assertTrue(r.detail!!.contains("platform"), r.detail)
    }

    @Test
    fun `the app wires both stacks and shows the detail under the error`() {
        val services = File("src/main/java/com/splitmate/app/data/guide/TripGuideServices.kt").readText()
        assertTrue(services.contains("RacingRedirectFetcher(") && services.contains("UrlConnectionRedirectFetcher()"))
        assertTrue(services.contains("directGoogleClient()"))
        val sheet = File("src/main/java/com/splitmate/app/ui/screens/plan/StayPinSheet.kt").readText()
        assertTrue(sheet.contains("text = \"Details: \$detail\""))
        assertFalse(sheet.contains("ACCESS_FINE_LOCATION"), "no location permission involved")
    }

    @Test
    fun `rc17 - long 133-char place path keeps Maharashtra 415639 without 120-char truncation`() {
        val extracted = MapLinkCoordinateParser.extractPlaceNameHint(redirect)
        assertEquals(hint, extracted)
        assertTrue(extracted!!.endsWith("Maharashtra 415639"), extracted)
    }

    @Test
    fun `rc17 - maps embed initEmbed response resolves the real place-id link to EXACT coordinates`() = runBlocking {
        val embedHtml = """
            <!DOCTYPE html><html><head><title>Google Maps</title></head><body><script>
            initEmbed([null,null,null,null,null,[[[2,"spotlit"]]],null,null,[["52"],
            ["0x3bea0d22e1412a97:0x9395d694c999e8da","$hint, India",[16.9983921,73.3569574],"10634642029631105242"],
            "Hemprabha Bed And Breakfast PROP PRASHANT KOPARDE",["Shop No 504","Kuwarbav","Ratnagiri, Maharashtra 415639","India"],4.9,86]]);
            </script></body></html>
        """.trimIndent()
        val fetcher = object : RedirectFetcher {
            override suspend fun probe(url: String, method: ProbeMethod) = fetch(url, method, FetchIdentity.APP, 0)
            override suspend fun fetch(url: String, method: ProbeMethod, identity: FetchIdentity, maxBodyBytes: Int): RedirectProbe =
                when {
                    url.contains("maps.app.goo.gl") -> RedirectProbe(302, redirect)
                    url.contains("/maps/embed") -> RedirectProbe(200, null, embedHtml)
                    else -> RedirectProbe(200, null, "<html><script>mapslite=1;</script><img src=\"maps_512dp.png\"></html>")
                }
        }
        val r = ShortLinkResolver(fetcher, isOnline = { true }, flags = { GuideFeatureFlags() })
            .resolve("https://maps.app.goo.gl/m18sW89UNW9NqgXM8") as StayResolution.Resolved
        assertEquals(CoordinatePrecision.EXACT, r.precision)
        assertEquals(16.9983921, r.location.lat, 1e-7)
        assertEquals(73.3569574, r.location.lng, 1e-7)
        assertEquals("Hemprabha Bed And Breakfast", r.label)
        assertTrue(MapsPageCoordinateExtractor.shouldStopReading("<html><script>mapslite=1;</script><img src=\"maps_512dp.png\"></html>"))
    }

    @Test
    fun `rc17 - GoogleDirectEdgeDns resolves Google hosts in-memory and 3rd direct lane overtakes stalled DNS`() = runTest {
        assertTrue(GoogleDirectEdgeDns.isGoogleHost("maps.app.goo.gl"))
        assertTrue(GoogleDirectEdgeDns.isGoogleHost("www.google.com"))
        assertTrue(GoogleDirectEdgeDns.isGoogleHost("maps.google.co.in"))
        assertFalse(GoogleDirectEdgeDns.isGoogleHost("nominatim.openstreetmap.org"))
        val directAddrs = GoogleDirectEdgeDns.lookup("maps.app.goo.gl")
        assertTrue(directAddrs.size >= 6)
        assertTrue(directAddrs.all { it.hostName == "maps.app.goo.gl" })

        val racing = RacingRedirectFetcher(
            primary = scripted { awaitCancellation() },
            secondary = scripted { awaitCancellation() },
            tertiary = scripted { delay(150); RedirectProbe(302, redirect) },
            clockMs = { currentTime }
        )
        val probe = racing.fetch("https://maps.app.goo.gl/m18sW89UNW9NqgXM8", ProbeMethod.GET, FetchIdentity.BROWSER_MOBILE, 0)
        assertEquals(302, probe.statusCode)
        assertEquals(RacingRedirectFetcher.DIRECT_HEAD_START_MS + 150, currentTime)
        assertTrue(racing.trace().any { it.contains("direct 302") }, racing.trace().toString())
    }
}
