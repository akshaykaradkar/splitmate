package com.splitmate.app.guide.content

import com.splitmate.app.data.guide.GuideHttp
import com.splitmate.app.data.guide.content.ApiUrl
import com.splitmate.app.data.guide.content.GuideContentException
import com.splitmate.app.data.guide.content.HttpFetcher
import com.splitmate.app.data.guide.content.HttpResponse
import com.splitmate.app.data.guide.content.PoliteHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class PoliteHttpClientTest {

    private val url = "https://www.wikidata.org/w/api.php?action=wbsearchentities&search=x"
    private val sleeps = ArrayList<Long>()
    private fun client(fake: HttpFetcher, now: Long = 0L) =
        PoliteHttpClient(fake, sleeper = { sleeps += it }, nowMs = { now })

    @Test
    fun `429 with Retry-After seconds is retried once after waiting`() {
        val fake = FakeHttpFetcher().on("wikidata", HttpResponse(429, "", mapOf("retry-after" to "2")), Fixtures.ok("{\"ok\":1}"))
        assertEquals("{\"ok\":1}", client(fake).get(url))
        assertEquals(listOf(2000L), sleeps)
        assertEquals(2, fake.requests.size)
    }

    @Test
    fun `503 without Retry-After uses default backoff`() {
        val fake = FakeHttpFetcher().on("wikidata", HttpResponse(503, "busy"), Fixtures.ok("fine"))
        assertEquals("fine", client(fake).get(url))
        assertEquals(listOf(PoliteHttpClient.DEFAULT_BACKOFF_MS), sleeps)
    }

    @Test
    fun `Retry-After HTTP-date is honoured relative to now`() {
        val now = java.time.ZonedDateTime.parse("Wed, 30 Sep 2026 18:00:00 GMT", java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant().toEpochMilli()
        val fake = FakeHttpFetcher().on("wikidata",
            HttpResponse(429, "", mapOf("Retry-After" to "Wed, 30 Sep 2026 18:00:03 GMT")), Fixtures.ok("ok"))
        assertEquals("ok", client(fake, now).get(url))
        assertEquals(listOf(3000L), sleeps)
    }

    @Test
    fun `retries only once - second 429 fails with code`() {
        val fake = FakeHttpFetcher().on("wikidata", HttpResponse(429, "", mapOf("Retry-After" to "1")))
        val e = assertThrows(GuideContentException::class.java) { client(fake).get(url) }
        assertEquals(429, e.httpCode!!)
        assertEquals(2, fake.requests.size)
        assertEquals(listOf(1000L), sleeps)
    }

    @Test
    fun `excessive Retry-After fails fast without sleeping`() {
        val fake = FakeHttpFetcher().on("wikidata", HttpResponse(429, "", mapOf("Retry-After" to "120")))
        val e = assertThrows(GuideContentException::class.java) { client(fake).get(url) }
        assertEquals(429, e.httpCode!!)
        assertEquals(1, fake.requests.size)
        assertTrue(sleeps.isEmpty())
    }

    @Test
    fun `maxlag error body is treated as retryable`() {
        val maxlag = """{"error":{"code":"maxlag","info":"Waiting for 10.64.48.35: 6 seconds lagged.","host":"db","lag":6}}"""
        val fake = FakeHttpFetcher().on("wikidata", HttpResponse(200, maxlag, mapOf("Retry-After" to "5")), Fixtures.ok("ok"))
        assertEquals("ok", client(fake).get(url))
        assertEquals(listOf(5000L), sleeps)
    }

    @Test
    fun `non-retryable errors are not retried`() {
        val fake = FakeHttpFetcher().on("wikidata", HttpResponse(404, "nope"))
        assertEquals(404, assertThrows(GuideContentException::class.java) { client(fake).get(url) }.httpCode!!)
        assertEquals(1, fake.requests.size)
    }

    @Test
    fun `transport failure maps to GuideContentException`() {
        val fake = FakeHttpFetcher().failOn("wikidata", IOException("timeout"))
        val e = assertThrows(GuideContentException::class.java) { client(fake).get(url) }
        assertTrue(e.cause is IOException)
    }

    @Test
    fun `https only and host allow-list enforced before any request`() {
        val fake = FakeHttpFetcher().on("", Fixtures.ok("x"))
        val c = client(fake)
        assertThrows(GuideContentException::class.java) { c.get("http://www.wikidata.org/w/api.php") }
        assertThrows(GuideContentException::class.java) { c.get("https://evil.example.com/w/api.php") }
        assertThrows(GuideContentException::class.java) { c.get("https://www.wikidata.org.evil.com/w/api.php") }
        assertThrows(GuideContentException::class.java) { c.get("not a url") }
        assertTrue(fake.requests.isEmpty())
    }

    @Test
    fun `sends exactly the identified User-Agent`() {
        val fake = FakeHttpFetcher().on("wikidata", Fixtures.ok("x"))
        client(fake).get(url)
        val h = fake.requests.single().headers
        assertEquals(GuideHttp.USER_AGENT, h["User-Agent"])
        assertEquals(1, h.keys.count { it.equals("User-Agent", ignoreCase = true) })
        assertTrue(!h.values.any { it.contains("Mozilla") || it.contains("Chrome") })
    }

    @Test
    fun `never more than two requests in flight`() {
        val inFlight = AtomicInteger(0)
        val maxSeen = AtomicInteger(0)
        val fetcher = HttpFetcher { _, _ ->
            val n = inFlight.incrementAndGet()
            maxSeen.accumulateAndGet(n) { a, b -> maxOf(a, b) }
            Thread.sleep(30)
            inFlight.decrementAndGet()
            HttpResponse(200, "ok")
        }
        val c = PoliteHttpClient(fetcher, sleeper = {}, maxConcurrent = 8) // clamped to 2
        val pool = Executors.newFixedThreadPool(6)
        val done = CountDownLatch(6)
        repeat(6) { pool.execute { try { c.get(url) } finally { done.countDown() } } }
        assertTrue(done.await(10, TimeUnit.SECONDS))
        pool.shutdownNow()
        assertTrue(maxSeen.get() in 1..2, "max in flight = ${maxSeen.get()}")
    }

    @Test
    fun `ApiUrl appends json and formatversion 2 and maxlag 5 with RFC 3986 encoding`() {
        val u = ApiUrl.mediaWiki(ApiUrl.WIKIVOYAGE_API, listOf("page" to "Goa/North Goa", "prop" to "wikitext|revid", "q" to "a b~"))
        assertEquals(
            "https://en.wikivoyage.org/w/api.php?page=Goa%2FNorth%20Goa&prop=wikitext%7Crevid&q=a%20b~&format=json&formatversion=2&maxlag=5",
            u
        )
    }
}
