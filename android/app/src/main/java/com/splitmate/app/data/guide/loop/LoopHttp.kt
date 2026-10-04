package com.splitmate.app.data.guide.loop

import com.splitmate.app.data.guide.GuideHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/*
 * Network seams for the smart-loop package. Every networked component takes one of these
 * interfaces so unit tests run fully offline with fakes and recorded fixtures.
 */

/** HTTP method used for a redirect probe. */
enum class ProbeMethod { HEAD, GET }

/**
 * Which identity a [RedirectFetcher] presents (v2.3.5 #3).
 * - [BROWSER_MOBILE]: a mobile Chrome User-Agent. Google serves normal 30x redirects / place pages
 *   to it instead of bot-style responses.
 * - [APP]: the identified SplitMate User-Agent ([GuideHttp.USER_AGENT]), kept as a fallback.
 */
enum class FetchIdentity { BROWSER_MOBILE, APP }

/**
 * Status code and raw `Location` header of a single, non-followed HTTP request.
 * [body] is only set by [RedirectFetcher.fetch] for 2xx responses when a body was requested
 * (size-capped, UTF-8, possibly truncated).
 */
data class RedirectProbe(val statusCode: Int, val location: String?, val body: String? = null)

/**
 * Performs exactly ONE HTTP request without following redirects. Used by [ShortLinkResolver].
 */
interface RedirectFetcher {
    /** Legacy probe: never reads the body. */
    @Throws(IOException::class)
    suspend fun probe(url: String, method: ProbeMethod): RedirectProbe

    /**
     * v2.3.5: one request with an explicit [identity]. For 2xx responses up to [maxBodyBytes]
     * bytes of the body are returned in [RedirectProbe.body] (0 = never read the body).
     * The default implementation delegates to [probe] so older fakes keep working.
     */
    @Throws(IOException::class)
    suspend fun fetch(url: String, method: ProbeMethod, identity: FetchIdentity, maxBodyBytes: Int): RedirectProbe =
        probe(url, method)
}

/** A small text HTTP response (status + UTF-8 body). */
data class HttpTextResponse(val statusCode: Int, val body: String)

/** Performs an HTTPS GET and returns the (size-capped) body as text. */
interface HttpTextFetcher {
    @Throws(IOException::class)
    suspend fun get(url: String, headers: Map<String, String>): HttpTextResponse
}

/**
 * OkHttp implementation of [RedirectFetcher]: `followRedirects(false)`, connect/read 10 s.
 *
 * v2.3.5 (#3): `retryOnConnectionFailure(true)` so OkHttp tries the next route (e.g. IPv4 after a
 * dead IPv6 address on carrier networks) instead of failing the whole hop; redirects are still
 * followed manually by [ShortLinkResolver].
 */
class OkHttpRedirectFetcher(
    private val client: OkHttpClient = defaultClient()
) : RedirectFetcher {

    override suspend fun probe(url: String, method: ProbeMethod): RedirectProbe =
        fetch(url, method, FetchIdentity.APP, maxBodyBytes = 0)

    override suspend fun fetch(
        url: String,
        method: ProbeMethod,
        identity: FetchIdentity,
        maxBodyBytes: Int
    ): RedirectProbe =
        runInterruptible(Dispatchers.IO) {
            if (!url.startsWith("https://", ignoreCase = true)) throw IOException("HTTPS only")
            val builder = Request.Builder().url(url)
            when (identity) {
                FetchIdentity.BROWSER_MOBILE -> builder
                    .header("User-Agent", MOBILE_BROWSER_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                FetchIdentity.APP -> builder.header("User-Agent", GuideHttp.USER_AGENT)
            }
            when (method) {
                ProbeMethod.HEAD -> builder.head()
                ProbeMethod.GET -> {
                    builder.get()
                    if (maxBodyBytes <= 0) builder.header("Range", "bytes=0-0")
                }
            }
            client.newCall(builder.build()).execute().use { response ->
                val body = if (method == ProbeMethod.GET && maxBodyBytes > 0 && response.code in 200..299) {
                    response.body?.byteStream()?.let { readTruncated(it, maxBodyBytes) }
                } else {
                    null
                }
                RedirectProbe(response.code, response.header("Location"), body)
            }
        }

    companion object {
        /** Mobile Chrome identity used for Google hosts (v2.3.5 #3). */
        const val MOBILE_BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Mobile Safari/537.36"

        /** Redirect client: never follows redirects itself; 10 s connect/read (mobile networks). */
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(true)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()

        /** Reads at most [max] bytes and silently truncates the rest (HTML fallback only). */
        internal fun readTruncated(input: InputStream, max: Int): String {
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8 * 1024)
            var total = 0
            while (total < max) {
                val n = input.read(buf, 0, minOf(buf.size, max - total))
                if (n < 0) break
                total += n
                out.write(buf, 0, n)
            }
            return String(out.toByteArray(), Charsets.UTF_8)
        }
    }
}

/**
 * OkHttp implementation of [HttpTextFetcher] for WDQS / Nominatim JSON. HTTPS only; never
 * follows an HTTPS→HTTP downgrade; body capped at [maxBodyBytes].
 */
class OkHttpTextFetcher(
    private val client: OkHttpClient = defaultClient(),
    private val maxBodyBytes: Int = DEFAULT_MAX_BODY_BYTES
) : HttpTextFetcher {

    override suspend fun get(url: String, headers: Map<String, String>): HttpTextResponse =
        runInterruptible(Dispatchers.IO) {
            if (!url.startsWith("https://", ignoreCase = true)) throw IOException("HTTPS only")
            val builder = Request.Builder().url(url).get().header("User-Agent", GuideHttp.USER_AGENT)
            headers.forEach { (k, v) -> builder.header(k, v) }
            client.newCall(builder.build()).execute().use { response ->
                val body = response.body
                val text = if (body == null) "" else readCapped(body.byteStream(), maxBodyBytes)
                HttpTextResponse(response.code, text)
            }
        }

    companion object {
        const val DEFAULT_MAX_BODY_BYTES = 2 * 1024 * 1024

        /** JSON client: 8 s connect, 10 s read (WDQS timeout budget), no HTTPS→HTTP redirects. */
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .followSslRedirects(false)
            .connectTimeout(GuideHttp.CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(GuideHttp.READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .callTimeout(GuideHttp.READ_TIMEOUT_MS.toLong() + 2_000L, TimeUnit.MILLISECONDS)
            .build()

        private fun readCapped(input: InputStream, max: Int): String {
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                if (total > max) throw IOException("Response too large")
                out.write(buf, 0, n)
            }
            return String(out.toByteArray(), Charsets.UTF_8)
        }
    }
}
