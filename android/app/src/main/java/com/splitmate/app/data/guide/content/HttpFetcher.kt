package com.splitmate.app.data.guide.content

import com.splitmate.app.data.guide.GuideHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Minimal blocking HTTP GET seam used by the content engine.
 *
 * Production uses [OkHttpFetcher]; unit tests inject a fake that serves recorded fixtures so no
 * test ever touches the network. Implementations must be thread-safe.
 */
fun interface HttpFetcher {
    /**
     * Performs a GET. Returns the response for ANY HTTP status (callers decide what is an error).
     * @throws IOException on transport failure (DNS, timeout, TLS, ...).
     */
    @Throws(IOException::class)
    fun get(url: String, headers: Map<String, String>): HttpResponse
}

/** An HTTP response with a decoded UTF-8 body. Header lookup is case-insensitive. */
data class HttpResponse(
    val code: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap()
) {
    fun header(name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
}

/**
 * OkHttp-backed [HttpFetcher]. Timeouts come from [GuideHttp]; HTTPS→HTTP redirect downgrades are
 * refused (`followSslRedirects(false)`); bodies are capped at [maxBodyBytes] to bound memory.
 */
class OkHttpFetcher(
    private val client: OkHttpClient = defaultClient(),
    private val maxBodyBytes: Int = DEFAULT_MAX_BODY_BYTES
) : HttpFetcher {

    override fun get(url: String, headers: Map<String, String>): HttpResponse {
        val builder = Request.Builder().url(url).get()
        headers.forEach { (k, v) -> builder.header(k, v) }
        return client.newCall(builder.build()).execute().use { response ->
            val bytes = response.body?.byteStream()?.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > maxBodyBytes) throw IOException("Response body exceeds $maxBodyBytes bytes")
                    out.write(buf, 0, n)
                }
                out.toByteArray()
            } ?: ByteArray(0)
            val headerMap = LinkedHashMap<String, String>()
            for (name in response.headers.names()) {
                response.header(name)?.let { headerMap[name] = it }
            }
            HttpResponse(response.code, String(bytes, Charsets.UTF_8), headerMap)
        }
    }

    companion object {
        const val DEFAULT_MAX_BODY_BYTES = 4 * 1024 * 1024

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(GuideHttp.CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(GuideHttp.READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(false)
            .build()
    }
}
