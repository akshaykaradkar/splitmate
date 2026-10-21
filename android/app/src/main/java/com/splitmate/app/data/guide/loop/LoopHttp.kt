package com.splitmate.app.data.guide.loop

import kotlinx.coroutines.async
import com.splitmate.app.data.guide.GuideHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Orders DNS answers so that OkHttp reaches a working address family quickly.
 *
 * OkHttp 4.12 does not implement Happy Eyeballs (RFC 8305): it dials the returned addresses one
 * after another, each with the full connect timeout. Google hosts answer with many records
 * (`maps.app.goo.gl`: 6 × A + 4 × AAAA on 2026-10-20), so a blackholed family (SYNs dropped, not
 * refused) burns `addresses × connectTimeout` before the other family is ever tried — far more
 * than one call's budget. v2.3.x put every IPv4 record first, which fixed IPv6 blackholes (Indian
 * dual-stack carriers) but made an IPv4 blackhole (IPv6-only APN / Wi-Fi without 464XLAT)
 * unreachable within the call timeout.
 *
 * v2.4.0: addresses are **interleaved by family** (RFC 8305 §4), IPv4 first:
 * `[v4₁, v6₁, v4₂, v6₂, …]`. Whatever family is broken, the other one is dialled on the second
 * attempt, i.e. after one connect timeout. Single-family answers are returned untouched.
 */
object Ipv4PreferredDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> = interleave(Dns.SYSTEM.lookup(hostname))

    /** Pure ordering step (unit-tested): IPv4 first, then alternate families. */
    internal fun interleave(addresses: List<InetAddress>): List<InetAddress> {
        if (addresses.size <= 1) return addresses
        val ipv4 = addresses.filterIsInstance<Inet4Address>()
        if (ipv4.isEmpty() || ipv4.size == addresses.size) return addresses
        val ipv6 = addresses.filter { it !is Inet4Address }
        val out = ArrayList<InetAddress>(addresses.size)
        var i = 0
        while (i < ipv4.size || i < ipv6.size) {
            if (i < ipv4.size) out += ipv4[i]
            if (i < ipv6.size) out += ipv6[i]
            i++
        }
        return out
    }
}

/*
 * Network seams for the smart-loop package. Every networked component takes one of these
 * interfaces so unit tests run fully offline with fakes and recorded fixtures.
 */

/** HTTP method used for a redirect probe. */
enum class ProbeMethod { HEAD, GET }

/**
 * Which identity a [RedirectFetcher] presents (v2.3.5 #3).
 * - [BROWSER_MOBILE]: a browser User-Agent. Google serves normal 30x redirects / place pages
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
 * OkHttp implementation of [RedirectFetcher]: `followRedirects(false)`, family-interleaved DNS,
 * HTTP/1.1 so closing a partially read 2 MB Google Maps page never stalls draining an HTTP/2 stream.
 *
 * v2.4.0: requests are **enqueued, not executed**, and bridged with `suspendCancellableCoroutine`.
 * Cancelling the coroutine (sheet closed, new paste, the resolver's overall budget) cancels the
 * OkHttp call immediately — a blocking `execute()` under `runInterruptible` cannot be interrupted
 * while the socket is connecting or reading, so the old implementation kept running to its own
 * timeout. No thread is parked; the capped body read happens on OkHttp's dispatcher thread.
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
    ): RedirectProbe {
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
        val call = client.newCall(builder.build())
        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { runCatching { call.cancel() } }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val probe = try {
                        response.use { r ->
                            val code = r.code
                            val location = r.header("Location")
                            val body = if (method == ProbeMethod.GET && maxBodyBytes > 0 && code in 200..299) {
                                r.body?.byteStream()?.let { readTruncated(it, maxBodyBytes) }
                            } else {
                                null
                            }
                            // Cancel before closing a 2xx stream so OkHttp does not block draining unread MBs of Maps HTML/JS.
                            if (code in 200..299) runCatching { call.cancel() }
                            RedirectProbe(code, location, body)
                        }
                    } catch (e: IOException) {
                        if (cont.isActive) cont.resumeWithException(e)
                        return
                    } catch (e: RuntimeException) {
                        if (cont.isActive) cont.resumeWithException(IOException(e.message ?: "response failed", e))
                        return
                    } finally {
                        if (!call.isCanceled()) runCatching { call.cancel() }
                    }
                    if (cont.isActive) cont.resume(probe)
                }
            })
        }
    }

    companion object {
        /** Browser identity used for Google hosts (v2.3.5 #3). */
        const val MOBILE_BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Mobile Safari/537.36"

        /**
         * v2.4.0 per-request budget. Measured 2026-10-20: `maps.app.goo.gl` answers a 302 in
         * ~1.1–1.3 s (server time; connect 2 ms, TLS 13 ms), a full place page streams 256 KB in
         * ~0.2 s. One connect attempt gets 3 s so a blackholed address family costs one attempt,
         * not the whole call; the whole call (DNS + connects + headers + capped body) gets 6 s.
         */
        const val CONNECT_TIMEOUT_MS = 3_000L
        const val READ_TIMEOUT_MS = 4_000L
        const val CALL_TIMEOUT_MS = 6_000L

        /** Redirect client: never follows redirects itself; family-interleaved DNS; HTTP/1.1; fast timeouts. */
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .dns(Ipv4PreferredDns)
            .protocols(listOf(Protocol.HTTP_1_1))
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(true)
            .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .callTimeout(CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build()

        /** Earliest point at which [readTruncated] considers stopping before [max] bytes. */
        internal const val EARLY_STOP_MIN_BYTES = 24 * 1024

        /** [readTruncated] re-checks for an EXACT coordinate at most once per this many bytes. */
        internal const val EARLY_STOP_CHECK_INTERVAL_BYTES = 32 * 1024

        /**
         * Reads at most [max] bytes and silently truncates the rest (HTML fallback only).
         *
         * v2.3.5 (#3) rework: it stops before [max] only when [isComplete] says the bytes read so
         * far already contain an EXACT coordinate (default: [MapsPageCoordinateExtractor.hasExactCoordinate]).
         * Reaching `</head>` is not enough: the EXACT `[null,null,lat,lng]` tuples and the
         * `APP_INITIALIZATION_STATE` array usually sit in `<body>`, after the head.
         * If the server stalls or drops the connection after some bytes, those bytes are returned
         * instead of throwing.
         */
        internal fun readTruncated(
            input: InputStream,
            max: Int,
            isComplete: (String) -> Boolean = MapsPageCoordinateExtractor::hasExactCoordinate
        ): String {
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8 * 1024)
            var total = 0
            var lastCheckAt = 0
            try {
                while (total < max) {
                    val n = input.read(buf, 0, minOf(buf.size, max - total))
                    if (n < 0) break
                    total += n
                    out.write(buf, 0, n)
                    if (total >= EARLY_STOP_MIN_BYTES && total < max &&
                        (lastCheckAt == 0 || total - lastCheckAt >= EARLY_STOP_CHECK_INTERVAL_BYTES)
                    ) {
                        lastCheckAt = total
                        val snapshot = String(out.toByteArray(), Charsets.UTF_8)
                        if (isComplete(snapshot)) return snapshot
                    }
                }
            } catch (e: IOException) {
                if (out.size() == 0) throw e
            }
            return String(out.toByteArray(), Charsets.UTF_8)
        }
    }
}

/**
 * rc16: [RedirectFetcher] on Android's platform HTTP stack (`HttpsURLConnection`), independent of
 * our OkHttp client: different connection logic, the platform's own per-network DNS/socket
 * binding. Used as the second lane of [RacingRedirectFetcher]. Never follows redirects.
 * Cancellation disconnects the socket from the cancelling thread.
 */
class UrlConnectionRedirectFetcher(
    private val connectTimeoutMs: Int = 5_000,
    private val readTimeoutMs: Int = 5_000
) : RedirectFetcher {
    override suspend fun probe(url: String, method: ProbeMethod): RedirectProbe =
        fetch(url, method, FetchIdentity.APP, maxBodyBytes = 0)

    override suspend fun fetch(url: String, method: ProbeMethod, identity: FetchIdentity, maxBodyBytes: Int): RedirectProbe {
        if (!url.startsWith("https://", ignoreCase = true)) throw IOException("HTTPS only")
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val conn = java.net.URL(url).openConnection() as javax.net.ssl.HttpsURLConnection
            val handle = coroutineContext[kotlinx.coroutines.Job]?.invokeOnCompletion { cause ->
                if (cause != null) runCatching { conn.disconnect() }
            }
            try {
                conn.instanceFollowRedirects = false
                conn.useCaches = false
                conn.connectTimeout = connectTimeoutMs
                conn.readTimeout = readTimeoutMs
                conn.requestMethod = method.name
                when (identity) {
                    FetchIdentity.BROWSER_MOBILE -> {
                        conn.setRequestProperty("User-Agent", OkHttpRedirectFetcher.MOBILE_BROWSER_USER_AGENT)
                        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                    }
                    FetchIdentity.APP -> conn.setRequestProperty("User-Agent", GuideHttp.USER_AGENT)
                }
                if (method == ProbeMethod.GET && maxBodyBytes <= 0) conn.setRequestProperty("Range", "bytes=0-0")
                val code = conn.responseCode
                val location = conn.getHeaderField("Location")
                val body = if (method == ProbeMethod.GET && maxBodyBytes > 0 && code in 200..299) {
                    conn.inputStream.use { OkHttpRedirectFetcher.readTruncated(it, maxBodyBytes) }
                } else {
                    null
                }
                RedirectProbe(code, location, body)
            } finally {
                handle?.dispose()
                runCatching { conn.disconnect() }
            }
        }
    }
}

/** rc16: a fetcher that can report what its attempts did (for the error's technical detail). */
interface DiagnosticRedirectFetcher {
    fun resetTrace()
    fun trace(): List<String>
}

/**
 * rc16: races two independent HTTP stacks for each hop. [primary] (OkHttp) starts at once; if it
 * has not answered within [headStartMs] (or fails earlier), [secondary] (platform
 * HttpsURLConnection) starts too, and the first successful answer wins (the other is cancelled).
 * Only when both fail does the hop fail, with the primary's error (secondary's attached as
 * suppressed). Every attempt's outcome and timing is recorded for [trace].
 *
 * Why: a phone network can stall one stack (address family, middlebox, DNS path) while the other
 * gets through; one Google redirect is tiny, so a parallel second attempt costs almost nothing.
 */
class RacingRedirectFetcher(
    private val primary: RedirectFetcher,
    private val secondary: RedirectFetcher,
    private val headStartMs: Long = HEAD_START_MS,
    private val clockMs: () -> Long = { System.nanoTime() / 1_000_000 },
    private val primaryName: String = "okhttp",
    private val secondaryName: String = "platform"
) : RedirectFetcher, DiagnosticRedirectFetcher {

    private val traceLines = java.util.Collections.synchronizedList(ArrayList<String>())

    override fun resetTrace() = traceLines.clear()
    override fun trace(): List<String> = synchronized(traceLines) { traceLines.toList() }

    override suspend fun probe(url: String, method: ProbeMethod): RedirectProbe =
        fetch(url, method, FetchIdentity.APP, maxBodyBytes = 0)

    override suspend fun fetch(url: String, method: ProbeMethod, identity: FetchIdentity, maxBodyBytes: Int): RedirectProbe =
        kotlinx.coroutines.coroutineScope {
            val host = runCatching { java.net.URI(url).host }.getOrNull() ?: "?"
            val primaryDone = kotlinx.coroutines.CompletableDeferred<Unit>()
            val a = async {
                try {
                    attempt(primaryName, host) { primary.fetch(url, method, identity, maxBodyBytes) }
                } finally {
                    primaryDone.complete(Unit)
                }
            }
            val b = async {
                kotlinx.coroutines.withTimeoutOrNull(headStartMs) { primaryDone.await() }
                if (a.isCompleted && a.await().isSuccess) return@async Result.failure<RedirectProbe>(IOException("not needed"))
                attempt(secondaryName, host) { secondary.fetch(url, method, identity, maxBodyBytes) }
            }
            // First success wins; otherwise wait for the other lane.
            val first = kotlinx.coroutines.selects.select<Pair<Result<RedirectProbe>, Boolean>> {
                a.onAwait { it to true }
                b.onAwait { it to false }
            }
            val firstResult = first.first
            if (firstResult.isSuccess) {
                (if (first.second) b else a).cancel()
                return@coroutineScope firstResult.getOrThrow()
            }
            val other = (if (first.second) b else a).await()
            if (other.isSuccess) return@coroutineScope other.getOrThrow()
            val primaryError = (if (first.second) firstResult else other).exceptionOrNull()
            val secondaryError = (if (first.second) other else firstResult).exceptionOrNull()
            val error = (primaryError as? IOException) ?: IOException(primaryError?.message ?: "request failed", primaryError)
            secondaryError?.takeIf { it !== error && it.message != "not needed" }?.let { error.addSuppressed(it) }
            throw error
        }

    private suspend fun attempt(name: String, host: String, block: suspend () -> RedirectProbe): Result<RedirectProbe> {
        val start = clockMs()
        return try {
            val r = block()
            traceLines += "$host $name ${r.statusCode} ${clockMs() - start}ms"
            Result.success(r)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            traceLines += "$host $name ${e.javaClass.simpleName}${e.message?.let { ": " + it.take(80) } ?: ""} ${clockMs() - start}ms"
            Result.failure(e)
        }
    }

    companion object {
        /** Google's short-link redirect takes ~1.1 s server-side; give OkHttp 1.5 s alone first. */
        const val HEAD_START_MS = 1_500L
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

        /** JSON client: 8 s connect, 10 s read (WDQS timeout budget), IPv4-first DNS, no HTTPS→HTTP redirects. */
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .dns(Ipv4PreferredDns)
            .followSslRedirects(false)
            .retryOnConnectionFailure(true)
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
