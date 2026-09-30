package com.splitmate.app.guide.loop

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.GuideFeatureFlags
import com.splitmate.app.data.guide.StayResolution
import com.splitmate.app.data.guide.loop.HttpTextFetcher
import com.splitmate.app.data.guide.loop.HttpTextResponse
import com.splitmate.app.data.guide.loop.MinIntervalRateLimiter
import com.splitmate.app.data.guide.loop.NominatimGeocoder
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException

@DisplayName("NominatimGeocoder: single user-initiated lookup, 1 req/s")
class NominatimGeocoderTest {

    private class FakeFetcher(private val response: () -> HttpTextResponse) : HttpTextFetcher {
        val urls = mutableListOf<String>()
        val headers = mutableListOf<Map<String, String>>()
        override suspend fun get(url: String, headers: Map<String, String>): HttpTextResponse {
            urls += url
            this.headers += headers
            return response()
        }
    }

    /** Fake monotonic clock + sleeper that advances the clock instead of sleeping. */
    private class FakeTime {
        var now = 10_000L
        val sleeps = mutableListOf<Long>()
        fun limiter() = MinIntervalRateLimiter(1_000L, clockMs = { now }, sleeper = { ms -> sleeps += ms; now += ms })
    }

    private fun geocoder(fetcher: HttpTextFetcher, flags: GuideFeatureFlags = GuideFeatureFlags(), time: FakeTime = FakeTime()) =
        NominatimGeocoder(fetcher, { flags }, time.limiter())

    @Test
    fun `hit returns APPROXIMATE with OSM name`(): Unit = runBlocking {
        val f = FakeFetcher { HttpTextResponse(200, LoopFixtures.text("nominatim_hotel_mayura.json")) }
        val r = geocoder(f).geocode("Hotel Mayura Bhuvaneshwari", "Hampi") as StayResolution.Resolved
        assertEquals(15.3179821, r.location.lat, 1e-9)
        assertEquals(76.4705134, r.location.lng, 1e-9)
        assertEquals(CoordinatePrecision.APPROXIMATE, r.precision)
        assertEquals("Hotel Mayura Bhuvaneshwari", r.label)
    }

    @Test
    fun `request url is https jsonv2 limit 1 and fully encoded`(): Unit = runBlocking {
        val f = FakeFetcher { HttpTextResponse(200, "[]") }
        geocoder(f).geocode("Hotel Mayura & Spa", "Hampi")
        assertEquals(
            "https://nominatim.openstreetmap.org/search?q=Hotel%20Mayura%20%26%20Spa%2C%20Hampi&format=jsonv2&limit=1&addressdetails=0",
            f.urls.single()
        )
        assertEquals("application/json", f.headers.single()["Accept"])
    }

    @Test
    fun `empty result is NoCoordinates carrying the hint`(): Unit = runBlocking {
        val f = FakeFetcher { HttpTextResponse(200, LoopFixtures.text("nominatim_empty.json")) }
        val r = geocoder(f).geocode("Unknown Guest House", "Hampi") as StayResolution.NoCoordinates
        assertNull(r.finalUrl)
        assertEquals("Unknown Guest House", r.placeNameHint)
    }

    @Test
    fun `kill-switch off makes no request`(): Unit = runBlocking {
        val f = FakeFetcher { HttpTextResponse(200, "[]") }
        val r = geocoder(f, GuideFeatureFlags(nominatimEnabled = false)).geocode("Hotel", "Hampi")
        assertTrue(r is StayResolution.Rejected)
        assertTrue(f.urls.isEmpty())
    }

    @Test
    fun `http error, malformed json and io failure are Rejected`(): Unit = runBlocking {
        assertTrue(geocoder(FakeFetcher { HttpTextResponse(429, "") }).geocode("A", null) is StayResolution.Rejected)
        assertTrue(geocoder(FakeFetcher { HttpTextResponse(200, "<html>") }).geocode("A", null) is StayResolution.Rejected)
        assertTrue(geocoder(FakeFetcher { throw IOException("x") }).geocode("A", null) is StayResolution.Rejected)
    }

    @Test
    fun `blank hint is Rejected without a request`(): Unit = runBlocking {
        val f = FakeFetcher { HttpTextResponse(200, "[]") }
        assertTrue(geocoder(f).geocode("   ", "Hampi") is StayResolution.Rejected)
        assertTrue(f.urls.isEmpty())
    }

    @Test
    fun `back-to-back calls are spaced by at least 1 second`(): Unit = runBlocking {
        val time = FakeTime()
        val f = FakeFetcher { HttpTextResponse(200, "[]") }
        val g = geocoder(f, time = time)
        g.geocode("A", null)
        time.now += 250
        g.geocode("B", null)
        assertEquals(listOf(750L), time.sleeps)
        time.now += 5_000
        g.geocode("C", null)
        assertEquals(listOf(750L), time.sleeps, "no extra wait after the interval has passed")
        assertEquals(3, f.urls.size)
    }

    @Test
    fun `query builder de-duplicates destination and sanitises`() {
        assertEquals("Hotel Mayura, Hampi", NominatimGeocoder.buildQuery("Hotel Mayura", "Hampi"))
        assertEquals("Hotel Mayura Hampi", NominatimGeocoder.buildQuery("Hotel Mayura Hampi", "hampi"))
        assertEquals("Hotel Mayura", NominatimGeocoder.buildQuery("Hotel\u0000 Mayura\n", "  "))
        assertNull(NominatimGeocoder.buildQuery("\n\t", "Hampi"))
    }

    @Test
    fun `parse rejects out-of-range coordinates`() {
        val r = NominatimGeocoder.parse("""[{"lat":"123.0","lon":"76.0","name":"X"}]""", "hint")
        assertTrue(r is StayResolution.NoCoordinates)
    }
}
