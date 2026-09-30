package com.splitmate.app.data.guide.sync

import com.splitmate.app.data.guide.GuideHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Minimal HTTP response (status + UTF-8 body, possibly truncated to the transport's cap). */
data class GuideHttpResponse(val code: Int, val body: String) {
    val isSuccessful: Boolean get() = code in 200..299
}

/**
 * Injectable HTTP seam for guide sync + flags. Tests supply a fake; production uses
 * [UrlConnectionGuideHttpTransport]. Implementations throw [IOException] on transport failure.
 */
interface GuideHttpTransport {
    suspend fun get(url: String, headers: Map<String, String>): GuideHttpResponse
    suspend fun post(url: String, body: String, headers: Map<String, String>): GuideHttpResponse
}

/**
 * `HttpURLConnection` transport (house standard). HTTPS only, identified User-Agent
 * ([GuideHttp.USER_AGENT], no PII), bounded timeouts, bounded response size, no redirects.
 * Never logs URLs or bodies.
 */
class UrlConnectionGuideHttpTransport(
    private val maxResponseBytes: Int = DEFAULT_MAX_RESPONSE_BYTES,
    private val connectTimeoutMs: Int = GuideHttp.CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = GuideHttp.READ_TIMEOUT_MS
) : GuideHttpTransport {

    override suspend fun get(url: String, headers: Map<String, String>): GuideHttpResponse =
        execute("GET", url, null, headers)

    override suspend fun post(url: String, body: String, headers: Map<String, String>): GuideHttpResponse =
        execute("POST", url, body, headers)

    private suspend fun execute(
        method: String,
        url: String,
        body: String?,
        headers: Map<String, String>
    ): GuideHttpResponse = withContext(Dispatchers.IO) {
        requireHttps(url)
        var conn: HttpURLConnection? = null
        try {
            val c = (URL(url).openConnection() as HttpURLConnection)
            conn = c
            c.apply {
                requestMethod = method
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                instanceFollowRedirects = false
                useCaches = false
                setRequestProperty("User-Agent", GuideHttp.USER_AGENT)
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                if (body != null) {
                    doOutput = true
                    val bytes = body.toByteArray(Charsets.UTF_8)
                    setFixedLengthStreamingMode(bytes.size)
                    outputStream.use { it.write(bytes) }
                }
            }
            val code = c.responseCode
            val stream = if (code in 200..399) c.inputStream else c.errorStream
            val text = stream?.use { readCapped(it, maxResponseBytes) } ?: ""
            GuideHttpResponse(code, text)
        } finally {
            conn?.disconnect()
        }
    }

    companion object {
        const val DEFAULT_MAX_RESPONSE_BYTES = 512 * 1024

        fun requireHttps(url: String) {
            if (!url.startsWith("https://")) throw IOException("HTTPS required")
        }

        internal fun readCapped(input: InputStream, cap: Int): String {
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                val take = minOf(n, cap - total)
                if (take > 0) out.write(buf, 0, take)
                total += n
                if (total >= cap) break
            }
            return out.toString(Charsets.UTF_8.name())
        }
    }
}
