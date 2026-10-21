package com.splitmate.app.guide.loop

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.StayResolution
import com.splitmate.app.data.guide.loop.CoordinatePattern
import com.splitmate.app.data.guide.loop.FetchIdentity
import com.splitmate.app.data.guide.loop.Ipv4PreferredDns
import com.splitmate.app.data.guide.loop.MapLinkCoordinateParser
import com.splitmate.app.data.guide.loop.OkHttpRedirectFetcher
import com.splitmate.app.data.guide.loop.ProbeMethod
import com.splitmate.app.data.guide.loop.RedirectFetcher
import com.splitmate.app.data.guide.loop.RedirectProbe
import com.splitmate.app.data.guide.loop.ShortLinkResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.net.InetAddress
import java.net.SocketTimeoutException

/**
 * v2.4.0 stay-link fix (Plan › Loop › "Set your stay"): the lookup must finish — resolved, with a
 * name fallback, or with an honest error — within [ShortLinkResolver.OVERALL_TIMEOUT_MS], and every
 * URL form Google (and Apple Maps / OSM) hands out today must parse. Redirect fixtures below are the
 * exact `Location` headers recorded from live `maps.app.goo.gl` links on 2026-10-20.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("ShortLinkResolver v2.4.0: real link forms, 12 s budget, cancellation")
class ShortLinkResolverV240Test {

    private class ScriptedFetcher(
        private val script: suspend (url: String, identity: FetchIdentity) -> RedirectProbe?
    ) : RedirectFetcher {
        val calls = mutableListOf<String>()
        var cancelled = 0
        override suspend fun probe(url: String, method: ProbeMethod): RedirectProbe = fetch(url, method, FetchIdentity.APP, 0)
        override suspend fun fetch(url: String, method: ProbeMethod, identity: FetchIdentity, maxBodyBytes: Int): RedirectProbe {
            calls += url
            return try {
                script(url, identity) ?: RedirectProbe(404, null)
            } catch (e: CancellationException) {
                cancelled++
                throw e
            }
        }
    }

    private val sleeps = mutableListOf<Long>()

    private fun resolver(fetcher: RedirectFetcher, online: Boolean? = true) =
        ShortLinkResolver(fetcher, isOnline = { online }, sleeper = { sleeps += it; delay(it) })

    // ---------------------------------------------------------------------------------------------
    // Recorded live redirects (2026-10-20)
    // ---------------------------------------------------------------------------------------------

    private val recorded = mapOf(
        "https://maps.app.goo.gl/fWJUZ1DrXLV5izF6A" to
            "https://www.google.com/maps/place/Taj+Fort+Aguada+Resort+%26+Spa,+Goa/@15.4975365,73.7670868,17z/data=!4m9!3m8!1s0x3bbfc17572c6093d:0xc2b14e0b1a4873ef!5m2!4m1!1i2!8m2!3d15.4975365!4d73.7670868!16s%2Fg%2F1tkks9qr?entry=tts&g_ep=EgoyMDI2MTAxNC4wIPu8ASoASAFQAw%3D%3D&skid=85414f82-9eaf-4dd5-978a-dc3e77721255",
        "https://maps.app.goo.gl/kQhsAWcdCpuWgyfm7" to
            "https://www.google.com/maps/place/Evolve+Back,+Hampi/@15.2744219,76.4793159,17z/data=!4m9!3m8!1s0x3bb77e0aac461def:0x4d3fb548f3abefdf!5m2!4m1!1i2!8m2!3d15.2744219!4d76.4793159!16s%2Fg%2F11cmt0c2mh?entry=tts&g_ep=EgoyMDI2MTAxNC4wIPu8ASoASAFQAw%3D%3D&skid=9130d6e6-3b86-4937-9008-925b2a81f89f",
        "https://maps.app.goo.gl/FYwvSp1uuQhyTHPu5" to
            "https://www.google.com/maps/place/Hampi+Bazaar/@15.3341232,76.4643603,17z/data=!3m1!4b1!4m6!3m5!1s0x3bb77fdba4e7ba67:0xacabb2edc27d80a!8m2!3d15.3341232!4d76.4643603!16s%2Fg%2F11c5_vyzdn?entry=tts&g_ep=EgoyMDI2MTAxNC4wIPu8ASoASAFQAw%3D%3D&skid=7bf5c17f-1084-4f02-bb3f-6da00241ef46",
        "https://maps.app.goo.gl/r8YTZYDoeTmhcoTt5" to
            "https://www.google.com/maps/place/15%C2%B020'06.0%22N+76%C2%B027'36.0%22E/@15.335,76.46,17z/data=!3m1!4b1!4m4!3m3!8m2!3d15.335!4d76.46?entry=tts&g_ep=EgoyMDI2MTAxNC4wIPu8ASoASAFQAw%3D%3D&skid=0ead35ed-6d21-45a9-bff9-db6d9428135e"
    )

    private fun recordedFetcher() = ScriptedFetcher { url, _ -> recorded[url]?.let { RedirectProbe(302, it) } }

    @ParameterizedTest(name = "{0} -> {1},{2} \"{3}\"")
    @CsvSource(
        "https://maps.app.goo.gl/fWJUZ1DrXLV5izF6A, 15.4975365, 73.7670868, 'Taj Fort Aguada Resort & Spa, Goa'",
        "https://maps.app.goo.gl/kQhsAWcdCpuWgyfm7, 15.2744219, 76.4793159, 'Evolve Back, Hampi'",
        "https://maps.app.goo.gl/FYwvSp1uuQhyTHPu5, 15.3341232, 76.4643603, 'Hampi Bazaar'",
        "https://maps.app.goo.gl/r8YTZYDoeTmhcoTt5, 15.335, 76.46, ''"
    )
    fun `live-recorded short link resolves EXACT from the single 302 in one request`(
        link: String, lat: Double, lng: Double, label: String
    ): Unit = runBlocking {
        val f = recordedFetcher()
        val r = resolver(f).resolve(link)
        assertTrue(r is StayResolution.Resolved, "got $r")
        r as StayResolution.Resolved
        assertEquals(lat, r.location.lat, 1e-9)
        assertEquals(lng, r.location.lng, 1e-9)
        assertEquals(CoordinatePrecision.EXACT, r.precision)
        assertEquals(label.ifEmpty { null }, r.label)
        assertEquals(listOf(link), f.calls, "exactly one request, no body fetch of the place page")
    }

    @Test
    fun `share-intent text with the name first keeps the URL-derived label and still needs one request`(): Unit = runBlocking {
        val f = recordedFetcher()
        val r = resolver(f).resolve("Check this out!\nTaj Fort Aguada Resort & Spa, Goa\nhttps://maps.app.goo.gl/fWJUZ1DrXLV5izF6A") as StayResolution.Resolved
        assertEquals("Taj Fort Aguada Resort & Spa, Goa", r.label)
        assertEquals(1, f.calls.size)
    }

    @Test
    fun `requiresNetwork is true for shorteners and place-id links only`() {
        assertTrue(ShortLinkResolver.requiresNetwork("https://maps.app.goo.gl/fWJUZ1DrXLV5izF6A"))
        assertTrue(ShortLinkResolver.requiresNetwork("Hotel X https://maps.app.goo.gl/fWJUZ1DrXLV5izF6A"))
        assertTrue(ShortLinkResolver.requiresNetwork("https://www.google.com/maps?cid=10634806485727930586"))
        assertTrue(ShortLinkResolver.requiresNetwork("https://goo.gl/maps/abc"))
        assertFalse(ShortLinkResolver.requiresNetwork("15.335, 76.46"))
        assertFalse(ShortLinkResolver.requiresNetwork(recorded.getValue("https://maps.app.goo.gl/kQhsAWcdCpuWgyfm7")), "pin in the URL: offline")
        assertFalse(ShortLinkResolver.requiresNetwork("https://maps.apple.com/?ll=15.335,76.46&q=Hampi"))
        assertFalse(ShortLinkResolver.requiresNetwork("https://www.openstreetmap.org/?mlat=15.335&mlon=76.46#map=17/15.335/76.46"))
        assertFalse(ShortLinkResolver.requiresNetwork("https://evil.example/maps.app.goo.gl/x"))
        assertFalse(ShortLinkResolver.requiresNetwork("just a hotel name"))
    }

    // ---------------------------------------------------------------------------------------------
    // Every URL form, offline (no fetcher call)
    // ---------------------------------------------------------------------------------------------

    private fun noNetwork() = ScriptedFetcher { _, _ -> error("network must not be used") }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        delimiter = '|',
        value = [
            "https://www.google.com/maps/place/Evolve+Back,+Hampi/@15.2744219,76.4793159,17z/data=!4m9!3m8!1s0x3bb77e0aac461def:0x4d3fb548f3abefdf!8m2!3d15.2744219!4d76.4793159?entry=ttu | 15.2744219 | 76.4793159 | EXACT | Evolve Back, Hampi",
            "https://www.google.com/maps?q=15.3341232,76.4643603 | 15.3341232 | 76.4643603 | EXACT |",
            "https://www.google.com/maps/@15.3341232,76.4643603,15z | 15.3341232 | 76.4643603 | APPROXIMATE |",
            "https://www.google.com/maps?ll=15.3341232,76.4643603&z=16 | 15.3341232 | 76.4643603 | EXACT |",
            "https://maps.google.com/maps?center=15.3341232,76.4643603&zoom=14 | 15.3341232 | 76.4643603 | EXACT |",
            "https://www.google.com/maps/search/15.3341232,+76.4643603 | 15.3341232 | 76.4643603 | EXACT |",
            "https://www.google.com/maps/place/15%C2%B020'06.0%22N+76%C2%B027'36.0%22E/@15.335,76.46,17z/data=!3m1!4b1!4m4!3m3!8m2!3d15.335!4d76.46 | 15.335 | 76.46 | EXACT |",
            "https://maps.apple.com/?ll=15.3341232,76.4643603&q=Hampi%20Bazaar | 15.3341232 | 76.4643603 | EXACT | Hampi Bazaar",
            "https://maps.apple.com/place?coordinate=15.3341232,76.4643603&q=Dropped%20Pin&ll=15.3341232,76.4643603 | 15.3341232 | 76.4643603 | EXACT | Dropped Pin",
            "https://www.openstreetmap.org/?mlat=15.3341232&mlon=76.4643603#map=17/15.3341232/76.4643603 | 15.3341232 | 76.4643603 | EXACT |",
            "https://www.openstreetmap.org/#map=17/15.3341232/76.4643603 | 15.3341232 | 76.4643603 | APPROXIMATE |",
            "https://osm.org/go/xyz?m=&mlat=15.3341232&mlon=76.4643603 | 15.3341232 | 76.4643603 | EXACT |",
            "geo:15.3341232,76.4643603?q=15.3341232,76.4643603(Hampi%20Bazaar) | 15.3341232 | 76.4643603 | EXACT | Hampi Bazaar",
            "15°20'06.0\"N 76°27'36.0\"E | 15.335 | 76.46 | EXACT |",
            "15.3341232, 76.4643603 | 15.3341232 | 76.4643603 | EXACT |"
        ]
    )
    fun `every supported link form resolves offline without any request`(
        input: String, lat: Double, lng: Double, precision: CoordinatePrecision, label: String?
    ): Unit = runBlocking {
        val f = noNetwork()
        val r = resolver(f).resolve(input)
        assertTrue(r is StayResolution.Resolved, "got $r for $input")
        r as StayResolution.Resolved
        assertEquals(lat, r.location.lat, 1e-7)
        assertEquals(lng, r.location.lng, 1e-7)
        assertEquals(precision, r.precision)
        assertEquals(label?.takeIf { it.isNotBlank() }, r.label)
        assertTrue(f.calls.isEmpty())
    }

    @Test
    fun `maps search by name carries the name as hint`(): Unit = runBlocking {
        val r = resolver(noNetwork()).resolve("https://www.google.com/maps/search/Evolve+Back+Hampi/") as StayResolution.NoCoordinates
        assertEquals("Evolve Back Hampi", r.placeNameHint)
        assertEquals("Evolve Back Hampi", MapLinkCoordinateParser.parse("https://www.google.com/maps/search/Evolve+Back+Hampi/?entry=ttu").placeNameHint)
    }

    @Test
    fun `OSM marker and fragment rungs are classified correctly`() {
        val marker = MapLinkCoordinateParser.extractCoordinate("https://www.openstreetmap.org/?mlon=76.46&mlat=15.335#map=12/15.0/76.0")!!
        assertEquals(CoordinatePattern.QUERY_PARAM, marker.pattern)
        assertEquals(15.335, marker.location.lat, 1e-9)
        val frag = MapLinkCoordinateParser.extractCoordinate("https://www.openstreetmap.org/#map=12/15.335/76.46")!!
        assertEquals(CoordinatePattern.VIEWPORT, frag.pattern)
        assertNull(MapLinkCoordinateParser.extractCoordinate("https://www.openstreetmap.org/?mlat=15.335"), "lat alone is not a coordinate")
    }

    @Test
    fun `Apple Maps and OSM are never followed as redirect targets`(): Unit = runBlocking {
        val f = ScriptedFetcher { url, _ -> if (url == "https://maps.app.goo.gl/x") RedirectProbe(302, "https://maps.apple.com/?ll=15.3,76.4") else null }
        val r = resolver(f).resolve("https://maps.app.goo.gl/x")
        assertEquals(StayResolution.Rejected(ShortLinkResolver.REASON_LEFT_GOOGLE), r)
    }

    @Test
    fun `unsupported host is still rejected even with coordinates in it`(): Unit = runBlocking {
        val r = resolver(noNetwork()).resolve("https://evil.example/maps/@15.3,76.4,17z")
        assertEquals(StayResolution.Rejected(ShortLinkResolver.REASON_UNSUPPORTED_HOST), r)
    }

    // ---------------------------------------------------------------------------------------------
    // Budget and cancellation
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `hanging server - lookup fails within the 12 s budget, cancels the request, and says so honestly`() = runTest {
        val f = ScriptedFetcher { _, _ -> awaitCancellation() }
        val r = resolver(f).resolve("https://maps.app.goo.gl/hang")
        assertEquals(ShortLinkResolver.OVERALL_TIMEOUT_MS, currentTime, "resolved exactly at the cap")
        assertTrue(ShortLinkResolver.OVERALL_TIMEOUT_MS <= 12_000L)
        assertEquals(StayResolution.Rejected(ShortLinkResolver.REASON_TIMEOUT), r)
        assertEquals(1, f.cancelled, "the in-flight request was cancelled by the budget")
        assertTrue(sleeps.isEmpty(), "no retry was started while the first request was still hanging")
    }

    @Test
    fun `hanging server with a name in the share text falls back to the name instead of an error`() = runTest {
        val f = ScriptedFetcher { _, _ -> awaitCancellation() }
        val r = resolver(f).resolve("Evolve Back Hampi https://maps.app.goo.gl/hang")
        assertEquals(ShortLinkResolver.OVERALL_TIMEOUT_MS, currentTime)
        assertEquals(StayResolution.NoCoordinates("https://maps.app.goo.gl/hang", "Evolve Back Hampi"), r)
    }

    @Test
    fun `per-request timeouts twice in a row end well inside the budget with the timeout reason`() = runTest {
        var attempts = 0
        val f = ScriptedFetcher { _, _ ->
            attempts++
            delay(OkHttpRedirectFetcher.CALL_TIMEOUT_MS)
            throw SocketTimeoutException("timeout")
        }
        val r = resolver(f).resolve("https://maps.app.goo.gl/slow")
        assertEquals(StayResolution.Rejected(ShortLinkResolver.REASON_TIMEOUT), r)
        assertEquals(2, attempts, "one retry only")
        assertEquals(listOf(ShortLinkResolver.RETRY_BACKOFF_MS), sleeps)
        assertTrue(currentTime <= ShortLinkResolver.OVERALL_TIMEOUT_MS, "took ${currentTime}ms")
    }

    @Test
    fun `captive portal (not validated) reports no working internet rather than a timeout`() = runTest {
        val f = ScriptedFetcher { _, _ -> throw SocketTimeoutException("timeout") }
        val r = resolver(f, online = false).resolve("https://maps.app.goo.gl/slow")
        assertEquals(StayResolution.Rejected(ShortLinkResolver.REASON_OFFLINE), r)
        assertEquals(1, f.calls.size, "no retry while known offline")
    }

    @Test
    fun `cancelling the caller cancels the in-flight request immediately`() = runTest {
        val f = ScriptedFetcher { _, _ -> awaitCancellation() }
        val job = launch { resolver(f).resolve("https://maps.app.goo.gl/hang") }
        runCurrent()
        assertEquals(1, f.calls.size)
        advanceTimeBy(500)
        job.cancel()
        advanceUntilIdle()
        assertEquals(1, f.cancelled)
        assertTrue(currentTime < 1_000L)
    }

    @Test
    fun `budget constants match the sheet copy`() {
        assertEquals(12_000L, ShortLinkResolver.OVERALL_TIMEOUT_MS)
        assertTrue(ShortLinkResolver.REASON_TIMEOUT.contains("12 seconds"))
        assertTrue(OkHttpRedirectFetcher.CALL_TIMEOUT_MS * 2 + ShortLinkResolver.RETRY_BACKOFF_MS <= ShortLinkResolver.OVERALL_TIMEOUT_MS + 1_000L)
    }

    // ---------------------------------------------------------------------------------------------
    // DNS ordering (the device-side timeout mechanism)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `DNS answers are interleaved by family, IPv4 first, so one broken family costs one connect attempt`() {
        val v4 = (0 until 6).map { InetAddress.getByAddress("maps.app.goo.gl", byteArrayOf(172.toByte(), 253.toByte(), 118.toByte(), (100 + it).toByte())) }
        val v6 = (0 until 4).map {
            InetAddress.getByAddress("maps.app.goo.gl", byteArrayOf(0x24, 0x04, 0x68, 0, 0x40, 0x03, 0x0c, 0x05, 0, 0, 0, 0, 0, 0, 0, (0x64 + it).toByte()))
        }
        val ordered = Ipv4PreferredDns.interleave(v6 + v4)
        assertEquals(v4[0], ordered[0])
        assertEquals(v6[0], ordered[1])
        assertEquals(v4[1], ordered[2])
        assertEquals(v6[1], ordered[3])
        assertEquals(10, ordered.size)
        assertEquals((v4 + v6).toSet(), ordered.toSet())
        assertEquals(v4, Ipv4PreferredDns.interleave(v4), "single family untouched")
        assertEquals(v6, Ipv4PreferredDns.interleave(v6), "single family untouched")
        assertNotNull(Ipv4PreferredDns.interleave(emptyList()))
    }
}
