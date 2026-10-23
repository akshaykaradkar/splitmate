package com.splitmate.app.guide.loop

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.StayResolution
import com.splitmate.app.data.guide.loop.FetchIdentity
import com.splitmate.app.data.guide.loop.OkHttpRedirectFetcher
import com.splitmate.app.data.guide.loop.ProbeMethod
import com.splitmate.app.data.guide.loop.RacingRedirectFetcher
import com.splitmate.app.data.guide.loop.RedirectFetcher
import com.splitmate.app.data.guide.loop.RedirectProbe
import com.splitmate.app.data.guide.loop.ShortLinkResolver
import com.splitmate.app.data.guide.loop.UrlConnectionRedirectFetcher
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

/**
 * v2.4.0 stay-link fix: LIVE probe of real Google Maps links through the production
 * [OkHttpRedirectFetcher] + [ShortLinkResolver]. Opt-in only (real network, non-deterministic):
 *
 *   gradle testDebugUnitTest -Psplitmate.liveNetwork=true --tests '*LiveNetwork*'
 *
 * Prints one trace line per HTTP hop (method, identity, status, Location, body bytes, ms) and the
 * final resolution + wall time per link so regressions in Google's redirect behaviour are visible.
 * Skipped (not failed) in the normal suite.
 */
@EnabledIfSystemProperty(named = "splitmate.liveNetwork", matches = "true")
@DisplayName("ShortLinkResolver LIVE network probe (opt-in)")
class ShortLinkResolverLiveNetworkTest {

    private class TracingFetcher(private val delegate: RedirectFetcher) : RedirectFetcher {
        val trace = mutableListOf<String>()
        override suspend fun probe(url: String, method: ProbeMethod): RedirectProbe =
            fetch(url, method, FetchIdentity.APP, 0)

        override suspend fun fetch(url: String, method: ProbeMethod, identity: FetchIdentity, maxBodyBytes: Int): RedirectProbe {
            val t0 = System.nanoTime()
            return try {
                val p = delegate.fetch(url, method, identity, maxBodyBytes)
                val ms = (System.nanoTime() - t0) / 1_000_000
                trace += "  $method $identity ${url.take(90)} -> ${p.statusCode} loc=${p.location?.take(160)} body=${p.body?.length ?: 0}B ${ms}ms"
                p
            } catch (e: Exception) {
                val ms = (System.nanoTime() - t0) / 1_000_000
                trace += "  $method $identity ${url.take(90)} -> EXC ${e.javaClass.simpleName}: ${e.message} ${ms}ms"
                throw e
            }
        }
    }

    private val links = listOf(
        // Short links created in Google Maps web on 2026-10-20 (Share -> Copy link).
        "https://maps.app.goo.gl/fWJUZ1DrXLV5izF6A", // Taj Fort Aguada Resort & Spa, Goa (hotel)
        "https://maps.app.goo.gl/kQhsAWcdCpuWgyfm7", // Evolve Back, Hampi (hotel)
        "https://maps.app.goo.gl/FYwvSp1uuQhyTHPu5", // Hampi Bazaar (landmark)
        "https://maps.app.goo.gl/r8YTZYDoeTmhcoTt5", // dropped pin 15.335,76.46 (no place id)
        "https://maps.app.goo.gl/dkQxdkL37eLmLwiG9", // Hemprabha B&B, Ratnagiri (v2.3.5 fixture)
        "https://maps.app.goo.gl/m18sW89UNW9NqgXM8", // Hemprabha B&B place-id short link (user's live link)
        // Share-text payload (name first).
        "Taj Fort Aguada Resort & Spa, Goa\nhttps://maps.app.goo.gl/fWJUZ1DrXLV5izF6A",
        // Legacy forms.
        "https://maps.google.com/?cid=10634806485727930586",
        "https://www.google.com/maps?q=Hemprabha+Bed+And+Breakfast,+Mirya+Road,+Ratnagiri&ftid=0x3bea0d22e1412a97:0x9395d694c999e8da",
        // Dead short link (should be rejected quickly, not time out).
        "https://maps.app.goo.gl/thisLinkDoesNotExist0"
    )

    @Test
    fun `live - every recorded link resolves or fails honestly within the budget`(): Unit = runBlocking {
        val report = StringBuilder("\n===== LIVE SHORT LINK PROBE =====\n")
        var slow = 0
        for (link in links) {
            val fetcher = TracingFetcher(OkHttpRedirectFetcher())
            val resolver = ShortLinkResolver(fetcher, isOnline = { true })
            val t0 = System.nanoTime()
            val result = runCatching { resolver.resolve(link) }
            val ms = (System.nanoTime() - t0) / 1_000_000
            report.append("LINK: ${link.replace("\n", "\\n")}\n")
            fetcher.trace.forEach { report.append(it).append('\n') }
            report.append("  => ${result.getOrElse { "EXC $it" }} in ${ms}ms\n")
            if (ms > ShortLinkResolver.OVERALL_TIMEOUT_MS) slow++
            val r = result.getOrNull()
            if (r is StayResolution.Rejected) report.append("  (rejected: ${r.reason})\n")
        }
        println(report)
        System.err.println(report)
        assertTrue(slow == 0, "some links exceeded the overall budget:\n$report")
    }

    @Test
    fun `live - user link m18sW89UNW9NqgXM8 resolves EXACT via direct Google edge DNS and embed probe`(): Unit = runBlocking {
        val racing = RacingRedirectFetcher(
            primary = OkHttpRedirectFetcher(),
            secondary = UrlConnectionRedirectFetcher(),
            tertiary = OkHttpRedirectFetcher(OkHttpRedirectFetcher.directGoogleClient())
        )
        val resolver = ShortLinkResolver(racing, isOnline = { true })
        val t0 = System.nanoTime()
        val res = resolver.resolve("https://maps.app.goo.gl/m18sW89UNW9NqgXM8")
        val ms = (System.nanoTime() - t0) / 1_000_000
        System.err.println("m18sW89UNW9NqgXM8 racing result: $res in ${ms}ms | trace=${racing.trace()}")
        assertTrue(res is StayResolution.Resolved, "Expected Resolved, got $res (trace=${racing.trace()})")
        res as StayResolution.Resolved
        assertEquals(CoordinatePrecision.EXACT, res.precision)
        assertEquals(16.9983921, res.location.lat, 1e-4)
        assertEquals(73.3569574, res.location.lng, 1e-4)
        assertEquals("Hemprabha Bed And Breakfast", res.label)

        // Also verify the direct-edge DNS client on its own (simulating blocked/sinkholed Dns.SYSTEM).
        val directFetcher = TracingFetcher(OkHttpRedirectFetcher(OkHttpRedirectFetcher.directGoogleClient()))
        val directResolver = ShortLinkResolver(directFetcher, isOnline = { true })
        val t1 = System.nanoTime()
        val directRes = directResolver.resolve("https://maps.app.goo.gl/m18sW89UNW9NqgXM8")
        val directMs = (System.nanoTime() - t1) / 1_000_000
        System.err.println("m18sW89UNW9NqgXM8 direct-edge DNS result: $directRes in ${directMs}ms\n${directFetcher.trace.joinToString("\n")}")
        assertTrue(directRes is StayResolution.Resolved, "Expected direct-edge Resolved, got $directRes")
        directRes as StayResolution.Resolved
        assertEquals(CoordinatePrecision.EXACT, directRes.precision)
        assertEquals(16.9983921, directRes.location.lat, 1e-4)
        assertEquals(73.3569574, directRes.location.lng, 1e-4)
    }
}
