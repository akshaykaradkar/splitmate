package com.splitmate.app.data.guide.content

import com.splitmate.app.data.guide.GuideHttp
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Semaphore

/** Failure raised by the content engine. Messages never contain user data (no PII). */
class GuideContentException(
    message: String,
    val httpCode: Int? = null,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Wikimedia politeness wrapper around an [HttpFetcher].
 *
 * - HTTPS only, host allow-list ([ALLOWED_HOSTS]).
 * - A single identified `User-Agent` ([GuideHttp.USER_AGENT]); no spoofed browser UA, no PII.
 * - At most [maxConcurrent] (default 2) requests in flight process-wide per client instance.
 * - Retry ONCE on 429/503 (or a MediaWiki `maxlag` error body), honouring `Retry-After`
 *   (delta-seconds or HTTP-date). If the server asks us to wait longer than [maxRetryAfterMs]
 *   we fail fast instead of blocking the user.
 *
 * `maxlag=5` is added by the API URL builders ([ApiUrl.mediaWiki]), not here.
 */
class PoliteHttpClient(
    private val fetcher: HttpFetcher,
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) },
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    maxConcurrent: Int = DEFAULT_MAX_CONCURRENT,
    private val defaultBackoffMs: Long = DEFAULT_BACKOFF_MS,
    private val maxRetryAfterMs: Long = DEFAULT_MAX_RETRY_AFTER_MS,
    private val allowedHosts: Set<String> = ALLOWED_HOSTS
) {
    private val permits = Semaphore(maxConcurrent.coerceIn(1, DEFAULT_MAX_CONCURRENT))

    /** GETs [url] and returns the 2xx body, or throws [GuideContentException]. */
    fun get(url: String): String {
        checkUrl(url)
        val first = attempt(url)
        if (!isRetryable(first)) return successBodyOrThrow(first)
        val waitMs = retryAfterMs(first) ?: defaultBackoffMs
        if (waitMs > maxRetryAfterMs) {
            throw GuideContentException("Rate limited; retry after ${waitMs}ms", first.code)
        }
        sleeper(waitMs.coerceAtLeast(0L))
        val second = attempt(url)
        if (isRetryable(second)) throw GuideContentException("Rate limited after retry", second.code)
        return successBodyOrThrow(second)
    }

    private fun attempt(url: String): HttpResponse {
        permits.acquire()
        try {
            return fetcher.get(url, REQUEST_HEADERS)
        } catch (e: IOException) {
            throw GuideContentException("Network error", null, e)
        } finally {
            permits.release()
        }
    }

    private fun successBodyOrThrow(r: HttpResponse): String {
        if (r.code !in 200..299) throw GuideContentException("HTTP ${r.code}", r.code)
        return r.body
    }

    private fun isRetryable(r: HttpResponse): Boolean =
        r.code == 429 || r.code == 503 || (r.code in 200..299 && isMaxlagBody(r.body))

    private fun isMaxlagBody(body: String): Boolean {
        val head = body.trimStart()
        return head.startsWith("{\"error\"") && MAXLAG_CODE.containsMatchIn(head.take(400))
    }

    /** Parses `Retry-After` as delta-seconds or an RFC 1123 HTTP-date. */
    internal fun retryAfterMs(r: HttpResponse): Long? {
        val raw = r.header("Retry-After")?.trim().orEmpty()
        if (raw.isEmpty()) return null
        raw.toLongOrNull()?.let { return (it.coerceAtLeast(0L)) * 1000L }
        return try {
            val at = ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
            (at - nowMs()).coerceAtLeast(0L)
        } catch (e: Exception) {
            null
        }
    }

    private fun checkUrl(url: String) {
        val uri = try {
            URI(url)
        } catch (e: Exception) {
            throw GuideContentException("Malformed URL")
        }
        if (!"https".equals(uri.scheme, ignoreCase = true)) throw GuideContentException("HTTPS required")
        val host = uri.host?.lowercase() ?: throw GuideContentException("Missing host")
        if (host !in allowedHosts) throw GuideContentException("Host not allowed: $host")
    }

    companion object {
        const val DEFAULT_MAX_CONCURRENT = 2
        const val DEFAULT_BACKOFF_MS = 1_000L
        const val DEFAULT_MAX_RETRY_AFTER_MS = 10_000L

        val ALLOWED_HOSTS: Set<String> = setOf(
            "www.wikidata.org",
            "commons.wikimedia.org",
            "en.wikivoyage.org"
        )

        /** Exactly one identifying header set; `User-Agent` is the Wikimedia-policy UA. */
        val REQUEST_HEADERS: Map<String, String> = mapOf(
            "User-Agent" to GuideHttp.USER_AGENT,
            "Accept" to "application/json"
        )

        private val MAXLAG_CODE = Regex("\"code\"\\s*:\\s*\"maxlag\"")
    }
}

/** Deterministic MediaWiki Action API URL builder (stable parameter order, RFC 3986 encoding). */
object ApiUrl {
    const val WIKIDATA_API = "https://www.wikidata.org/w/api.php"
    const val COMMONS_API = "https://commons.wikimedia.org/w/api.php"
    const val WIKIVOYAGE_API = "https://en.wikivoyage.org/w/api.php"
    const val MAXLAG_SECONDS = 5

    /** Appends `format=json&formatversion=2&maxlag=5` after the caller's params. */
    fun mediaWiki(base: String, params: List<Pair<String, String>>): String {
        val all = params + listOf(
            "format" to "json",
            "formatversion" to "2",
            "maxlag" to MAXLAG_SECONDS.toString()
        )
        return base + "?" + all.joinToString("&") { (k, v) -> "${encode(k)}=${encode(v)}" }
    }

    fun encode(s: String): String =
        URLEncoder.encode(s, "UTF-8").replace("+", "%20").replace("%7E", "~")
}
