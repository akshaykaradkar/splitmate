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

/** Status code and raw `Location` header of a single, non-followed HTTP request. */
data class RedirectProbe(val statusCode: Int, val location: String?)

/**
 * Performs exactly ONE HTTP request without following redirects and without reading the
 * response body. Used by [ShortLinkResolver].
 */
interface RedirectFetcher {
    @Throws(IOException::class)
    suspend fun probe(url: String, method: ProbeMethod): RedirectProbe
}

/** A small text HTTP response (status + UTF-8 body). */
data class HttpTextResponse(val statusCode: Int, val body: String)

/** Performs an HTTPS GET and returns the (size-capped) body as text. */
interface HttpTextFetcher {
    @Throws(IOException::class)
    suspend fun get(url: String, headers: Map<String, String>): HttpTextResponse
}

/**
 * OkHttp implementation of [RedirectFetcher]: `followRedirects(false)`, identified User-Agent,
 * connect/read 5 s. The body is never read: the response is closed immediately.
 */
class OkHttpRedirectFetcher(
    private val client: OkHttpClient = defaultClient()
) : RedirectFetcher {

    override suspend fun probe(url: String, method: ProbeMethod): RedirectProbe =
        runInterruptible(Dispatchers.IO) {
            if (!url.startsWith("https://", ignoreCase = true)) throw IOException("HTTPS only")
            val builder = Request.Builder()
                .url(url)
                .header("User-Agent", GuideHttp.USER_AGENT)
            when (method) {
                ProbeMethod.HEAD -> builder.head()
                ProbeMethod.GET -> builder.get().header("Range", "bytes=0-0")
            }
            client.newCall(builder.build()).execute().use { response ->
                RedirectProbe(response.code, response.header("Location"))
            }
        }

    companion object {
        /** Redirect-probing client: never follows redirects itself; short timeouts. */
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .build()
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
