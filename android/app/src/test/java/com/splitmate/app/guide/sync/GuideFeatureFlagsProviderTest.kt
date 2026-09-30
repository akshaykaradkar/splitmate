package com.splitmate.app.guide.sync

import com.splitmate.app.data.guide.GuideFeatureFlags
import com.splitmate.app.data.guide.flags.FileGuideKeyValueStore
import com.splitmate.app.data.guide.flags.GuideFeatureFlagsProvider
import com.splitmate.app.data.guide.flags.InMemoryGuideKeyValueStore
import com.splitmate.app.data.guide.sync.GuideHttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@DisplayName("v2.3.4 WS-D: GuideFeatureFlagsProvider (remote kill-switches)")
class GuideFeatureFlagsProviderTest {

    private val allOn = GuideFeatureFlags()
    private val killGuide = """{"schema":1,"guideEnabled":false,"shortLinkParsingEnabled":true,"wdqsEnabled":false,"nominatimEnabled":true}"""
    private val hour = 60L * 60L * 1000L

    private class Clock(var now: Long = 1_727_600_000_000L) {
        val fn: () -> Long = { now }
    }

    private fun provider(
        http: FakeGuideHttpTransport,
        clock: Clock,
        store: com.splitmate.app.data.guide.flags.GuideKeyValueStore = InMemoryGuideKeyValueStore()
    ) = GuideFeatureFlagsProvider(
        http = http,
        store = store,
        clock = clock.fn,
        ioDispatcher = Dispatchers.Unconfined
    )

    @Test
    fun `defaults are all enabled and available without blocking`() {
        val p = provider(FakeGuideHttpTransport.failing(), Clock())
        assertEquals(allOn, p.current())
        assertEquals(allOn, p.flags.value)
        assertTrue(allOn.guideEnabled && allOn.shortLinkParsingEnabled && allOn.wdqsEnabled && allOn.nominatimEnabled)
    }

    @Test
    fun `successful fetch applies remote flags`() = runBlocking {
        val http = FakeGuideHttpTransport.ok(killGuide)
        val p = provider(http, Clock())
        val flags = p.refresh()
        assertEquals(GuideFeatureFlags(guideEnabled = false, wdqsEnabled = false), flags)
        assertEquals(flags, p.current())
        assertEquals(GuideFeatureFlagsProvider.DEFAULT_URL, http.requests.single().url)
        assertTrue(http.requests.single().url.startsWith("https://"))
    }

    @Test
    fun `cached flags are reused within the 6h TTL and refetched after it`() = runBlocking {
        val http = FakeGuideHttpTransport.ok(killGuide)
        val clock = Clock()
        val p = provider(http, clock)
        p.refresh()
        clock.now += 6 * hour - 1
        p.refresh()
        assertEquals(1, http.requests.size, "fresh cache must not hit the network")
        clock.now += 1
        http.handler = { _, _, _ -> GuideHttpResponse(200, "{}") }
        assertEquals(allOn, p.refresh())
        assertEquals(2, http.requests.size)
    }

    @Test
    fun `force refresh bypasses the TTL`() = runBlocking {
        val http = FakeGuideHttpTransport.ok(killGuide)
        val p = provider(http, Clock())
        p.refresh()
        p.refresh(force = true)
        assertEquals(2, http.requests.size)
    }

    @Test
    fun `any failure without cache yields all-enabled defaults`() = runBlocking {
        assertEquals(allOn, provider(FakeGuideHttpTransport.failing(), Clock()).refresh())
        assertEquals(allOn, provider(FakeGuideHttpTransport { _, _, _ -> GuideHttpResponse(404, "") }, Clock()).refresh())
        assertEquals(allOn, provider(FakeGuideHttpTransport.ok("<html>oops</html>"), Clock()).refresh())
        assertEquals(allOn, provider(FakeGuideHttpTransport.ok("[true,false]"), Clock()).refresh())
        assertEquals(allOn, provider(FakeGuideHttpTransport.ok(""), Clock()).refresh())
    }

    @Test
    fun `failure after a successful fetch keeps the last-known flags`() = runBlocking {
        val http = FakeGuideHttpTransport.ok(killGuide)
        val clock = Clock()
        val p = provider(http, clock)
        p.refresh()
        clock.now += 7 * hour
        http.handler = { _, _, _ -> throw java.io.IOException("offline") }
        val flags = p.refresh()
        assertFalse(flags.guideEnabled, "kill-switch must stay engaged while offline")
    }

    @Test
    fun `failures back off before retrying the network`() = runBlocking {
        val http = FakeGuideHttpTransport.failing()
        val clock = Clock()
        val p = provider(http, clock)
        p.refresh()
        clock.now += GuideFeatureFlagsProvider.FAILURE_RETRY_MS - 1
        p.refresh()
        assertEquals(1, http.requests.size)
        clock.now += 1
        p.refresh()
        assertEquals(2, http.requests.size)
    }

    @Test
    fun `missing or non-boolean keys default to enabled, unknown keys ignored`() {
        assertEquals(allOn, GuideFeatureFlagsProvider.parse("{}"))
        assertEquals(
            GuideFeatureFlags(nominatimEnabled = false),
            GuideFeatureFlagsProvider.parse("""{"nominatimEnabled":false,"guideEnabled":"false","wdqsEnabled":0,"futureFlag":false}""")
        )
        assertNull(GuideFeatureFlagsProvider.parse("not json"))
        assertNull(GuideFeatureFlagsProvider.parse(null))
    }

    @Test
    fun `the published default flags file parses to all enabled`() {
        val candidates = listOf(File("../../docs/flags/guide.json"), File("../docs/flags/guide.json"), File("docs/flags/guide.json"))
        val file = candidates.firstOrNull { it.exists() } ?: return
        assertEquals(allOn, GuideFeatureFlagsProvider.parse(file.readText()))
    }

    @Test
    fun `persisted cache is loaded on a cold start and respected within TTL`(@TempDir dir: File) = runBlocking {
        val store = FileGuideKeyValueStore(File(dir, "guide_flags_cache.json"))
        val clock = Clock()
        provider(FakeGuideHttpTransport.ok(killGuide), clock, store).refresh()

        clock.now += hour
        val offline = FakeGuideHttpTransport.failing()
        val coldStart = provider(offline, clock, store)
        assertEquals(allOn, coldStart.current(), "no disk I/O before refresh")
        val flags = coldStart.refresh()
        assertFalse(flags.guideEnabled)
        assertTrue(offline.requests.isEmpty(), "fresh disk cache must not hit the network")
    }

    @Test
    fun `stale persisted cache is used when the refetch fails`(@TempDir dir: File) = runBlocking {
        val store = FileGuideKeyValueStore(File(dir, "flags.json"))
        val clock = Clock()
        provider(FakeGuideHttpTransport.ok(killGuide), clock, store).refresh()
        clock.now += 30 * 24 * hour
        val offline = FakeGuideHttpTransport.failing()
        val flags = provider(offline, clock, store).refresh()
        assertEquals(1, offline.requests.size)
        assertFalse(flags.guideEnabled)
    }

    @Test
    fun `corrupt cache file is ignored`(@TempDir dir: File) = runBlocking {
        val file = File(dir, "flags.json")
        file.writeText("{{{ corrupt")
        val flags = provider(FakeGuideHttpTransport.failing(), Clock(), FileGuideKeyValueStore(file)).refresh()
        assertEquals(allOn, flags)
    }
}
