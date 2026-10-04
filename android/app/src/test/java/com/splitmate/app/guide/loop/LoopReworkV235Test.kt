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
import com.splitmate.app.data.guide.loop.OkHttpRedirectFetcher
import com.splitmate.app.data.guide.loop.ProbeMethod
import com.splitmate.app.data.guide.loop.RedirectFetcher
import com.splitmate.app.data.guide.loop.RedirectProbe
import com.splitmate.app.data.guide.loop.ShortLinkResolver
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.net.SocketTimeoutException

class LoopReworkV235Test {

    @Test
    fun `a_readTruncated stops after head when exact coordinate found`() {
        val pad = "a".repeat(40 * 1024)
        val html = "<html><head></head><body>$pad [null,null,26.9124,75.7873] " + "b".repeat(60 * 1024)
        
        val stream = ByteArrayInputStream(html.toByteArray())
        val result = OkHttpRedirectFetcher.readTruncated(stream, 100 * 1024)
        
        assertTrue(result.contains("[null,null,26.9124,75.7873]"))
        assertTrue(result.length < 90 * 1024, "Should stop early, but got ${result.length}")
        
        val extracted = MapsPageCoordinateExtractor.extract(result)
        assertEquals(CoordinatePrecision.EXACT, extracted.coordinate?.precision)
    }

    @Test
    fun `b_generic title extractor rejects junk html`() {
        val html = """<link href="https://example.com/junk"><meta property="og:title" content="Google Maps">"""
        val result = MapsPageCoordinateExtractor.extract(html)
        assertNull(result.placeName)
    }

    @Test
    fun `c_fallbackLocalityQueries drops partial fragments`() {
        val queries = NominatimGeocoder.fallbackLocalityQueries("Oceanview Inn, Beach Road, Malv…", null)
        assertTrue(queries.none { it.contains("Malv") })
        assertTrue(queries.none { it.contains("…") })
    }

    @Test
    fun `d_geocodeSmart respects MAX_SMART_QUERIES budget`() = runBlocking {
        var fetches = 0
        val fetcher = object : HttpTextFetcher {
            override suspend fun get(url: String, headers: Map<String, String>): HttpTextResponse {
                fetches++
                return HttpTextResponse(200, "[]")
            }
        }
        val geocoder = NominatimGeocoder(fetcher, { GuideFeatureFlags() }, MinIntervalRateLimiter(0L))
        val result = geocoder.geocodeSmart("Oceanview Inn, Beach Road, Malvan, Maharashtra", "Malvan")
        
        assertTrue(fetches <= NominatimGeocoder.MAX_SMART_QUERIES, "Issued $fetches requests, expected <= 3")
        assertTrue(result is StayResolution.NoCoordinates)
    }

    @Test
    fun `e_null destination skips global stripped name query`() {
        val hint = "Lakeview Homestay"
        // With a destination, the hospitality-stripped name ("Lakeview") is planned as a bounded query.
        val withDest = NominatimGeocoder.smartPlan(hint, "Udaipur")
        assertTrue(withDest.any { it.boundedToDestination && it.text.startsWith("Lakeview,") }, "plan=$withDest")
        // Without a destination, the stripped name must never be searched globally.
        val noDest = NominatimGeocoder.smartPlan(hint, null)
        assertTrue(noDest.none { it.boundedToDestination }, "plan=$noDest")
        assertTrue(noDest.none { it.text.equals("Lakeview", ignoreCase = true) || it.text.startsWith("Lakeview,", ignoreCase = true) }, "plan=$noDest")
    }

    @Test
    fun `f_ShortLinkResolver hop 2 retry policy`() = runBlocking {
        var fetchCount = 0
        val fetcher = object : RedirectFetcher {
            override suspend fun probe(url: String, method: ProbeMethod) = fetch(url, method, FetchIdentity.APP, 0)
            override suspend fun fetch(url: String, method: ProbeMethod, identity: FetchIdentity, maxBodyBytes: Int): RedirectProbe {
                fetchCount++
                if (fetchCount == 1) {
                    return RedirectProbe(302, "https://www.google.com/maps/place/Oceanview+Inn")
                } else {
                    throw SocketTimeoutException("timeout")
                }
            }
        }
        val resolver = ShortLinkResolver(fetcher, sleeper = { }, flags = { GuideFeatureFlags() })
        val result = resolver.resolve("https://maps.app.goo.gl/abcd")
        
        assertEquals(2, fetchCount, "Should fetch exactly twice (no retry on hop 2)")
        assertTrue(result is StayResolution.NoCoordinates)
        assertEquals("Oceanview Inn", (result as StayResolution.NoCoordinates).placeNameHint)
    }
}
