package com.splitmate.app.data.guide.flags

import com.splitmate.app.data.guide.GuideFeatureFlags
import com.splitmate.app.data.guide.sync.GuideHttpTransport
import com.splitmate.app.data.guide.sync.UrlConnectionGuideHttpTransport
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/** Tiny key-value seam so the flags cache does not depend on SharedPreferences. */
interface GuideKeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

/** In-memory store (tests, or when no cache directory is available). */
class InMemoryGuideKeyValueStore : GuideKeyValueStore {
    private val map = java.util.concurrent.ConcurrentHashMap<String, String>()
    override fun getString(key: String): String? = map[key]
    override fun putString(key: String, value: String) {
        map[key] = value
    }
}

/**
 * Plain-file store: one JSON object per file, written atomically (temp file + rename).
 * Suggested location: `File(context.filesDir, "guide_flags_cache.json")`. Call from a background
 * dispatcher (the provider does).
 */
class FileGuideKeyValueStore(private val file: File) : GuideKeyValueStore {
    private val lock = Any()

    override fun getString(key: String): String? = synchronized(lock) {
        readAll()?.optString(key, "")?.takeIf { it.isNotEmpty() }
    }

    override fun putString(key: String, value: String) {
        synchronized(lock) {
            val all = readAll() ?: JSONObject()
            all.put(key, value)
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(all.toString(), Charsets.UTF_8)
            if (!tmp.renameTo(file)) {
                file.writeText(all.toString(), Charsets.UTF_8)
                tmp.delete()
            }
        }
    }

    private fun readAll(): JSONObject? = try {
        if (file.exists()) JSONObject(file.readText(Charsets.UTF_8)) else null
    } catch (_: Exception) {
        null
    }
}

/**
 * Remote kill-switches for the Trip Guide (decision #30), served as static JSON from GitHub Pages
 * (`docs/flags/guide.json` -> https://akshaykaradkar.github.io/splitmate/flags/guide.json).
 *
 * Behaviour:
 * - [flags] is a hot [StateFlow] that starts at all-enabled defaults; reading it never blocks.
 * - [refresh] (call off the UI path, e.g. when the Plan tab opens) loads the cached value, and if it
 *   is older than 6 h fetches the JSON. Never throws.
 * - Failure semantics: any fetch/parse failure keeps the last-known cached flags; with no cache the
 *   result is the all-enabled default. Missing or non-boolean keys default to enabled.
 * - After a failure the network is not retried for [failureRetryMs] (no hammering when offline).
 * - Unknown keys are ignored, so the JSON can grow without breaking old clients.
 */
class GuideFeatureFlagsProvider(
    private val http: GuideHttpTransport = UrlConnectionGuideHttpTransport(maxResponseBytes = 16 * 1024),
    private val store: GuideKeyValueStore = InMemoryGuideKeyValueStore(),
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val url: String = DEFAULT_URL,
    private val ttlMs: Long = TTL_MS,
    private val failureRetryMs: Long = FAILURE_RETRY_MS
) {
    private val mutex = Mutex()
    private val state = MutableStateFlow(GuideFeatureFlags())
    private var cacheLoaded = false
    private var cachedAtEpochMs: Long? = null
    private var lastFailureAtEpochMs: Long? = null

    /** Current flags (defaults until the first [refresh]). */
    val flags: StateFlow<GuideFeatureFlags> = state.asStateFlow()

    /** Non-blocking snapshot. */
    fun current(): GuideFeatureFlags = state.value

    /**
     * Loads cache and refreshes from the network when stale (or when [force] is true).
     * Returns the effective flags. Never throws (except coroutine cancellation).
     */
    suspend fun refresh(force: Boolean = false): GuideFeatureFlags = withContext(ioDispatcher) {
        mutex.withLock {
            val now = clock()
            if (!cacheLoaded) {
                loadCache()
                cacheLoaded = true
            }
            val cachedAt = cachedAtEpochMs
            val fresh = cachedAt != null && now >= cachedAt && now - cachedAt < ttlMs
            if (fresh && !force) return@withLock state.value
            val lastFailure = lastFailureAtEpochMs
            if (!force && lastFailure != null && now >= lastFailure && now - lastFailure < failureRetryMs) {
                return@withLock state.value
            }
            val fetched = fetch()
            if (fetched != null) {
                state.value = fetched
                cachedAtEpochMs = now
                lastFailureAtEpochMs = null
                try {
                    store.putString(CACHE_KEY, encodeCache(fetched, now))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Cache write failures are non-fatal.
                }
            } else {
                lastFailureAtEpochMs = now
            }
            state.value
        }
    }

    private fun loadCache() {
        val raw = try {
            store.getString(CACHE_KEY)
        } catch (_: Exception) {
            null
        } ?: return
        val (flags, fetchedAt) = decodeCache(raw) ?: return
        state.value = flags
        cachedAtEpochMs = fetchedAt
    }

    private suspend fun fetch(): GuideFeatureFlags? = try {
        val response = http.get(url, mapOf("Accept" to "application/json"))
        if (response.isSuccessful) parse(response.body) else null
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    companion object {
        const val DEFAULT_URL = "https://akshaykaradkar.github.io/splitmate/flags/guide.json"
        const val TTL_MS: Long = 6L * 60L * 60L * 1000L
        const val FAILURE_RETRY_MS: Long = 15L * 60L * 1000L
        const val CACHE_KEY = "guide_flags_v1"

        const val KEY_GUIDE = "guideEnabled"
        const val KEY_SHORT_LINK = "shortLinkParsingEnabled"
        const val KEY_WDQS = "wdqsEnabled"
        const val KEY_NOMINATIM = "nominatimEnabled"
        /** v2.3.5 (#3): HTML body fallback in ShortLinkResolver. */
        const val KEY_SHORT_LINK_BODY = "shortLinkBodyParseEnabled"

        /**
         * Parses the published flags JSON. Null when the body is not a JSON object (treated as a
         * failure). Missing or non-boolean keys default to `true` (enabled).
         */
        fun parse(body: String?): GuideFeatureFlags? {
            if (body.isNullOrBlank() || body.length > 16 * 1024) return null
            return try {
                val o = JSONObject(body.trim())
                GuideFeatureFlags(
                    guideEnabled = booleanOrTrue(o, KEY_GUIDE),
                    shortLinkParsingEnabled = booleanOrTrue(o, KEY_SHORT_LINK),
                    wdqsEnabled = booleanOrTrue(o, KEY_WDQS),
                    nominatimEnabled = booleanOrTrue(o, KEY_NOMINATIM),
                    shortLinkBodyParseEnabled = booleanOrTrue(o, KEY_SHORT_LINK_BODY)
                )
            } catch (_: Exception) {
                null
            }
        }

        private fun booleanOrTrue(o: JSONObject, key: String): Boolean = (o.opt(key) as? Boolean) ?: true

        internal fun encodeCache(flags: GuideFeatureFlags, fetchedAt: Long): String =
            JSONObject()
                .put("fetchedAt", fetchedAt)
                .put(KEY_GUIDE, flags.guideEnabled)
                .put(KEY_SHORT_LINK, flags.shortLinkParsingEnabled)
                .put(KEY_WDQS, flags.wdqsEnabled)
                .put(KEY_NOMINATIM, flags.nominatimEnabled)
                .put(KEY_SHORT_LINK_BODY, flags.shortLinkBodyParseEnabled)
                .toString()

        internal fun decodeCache(raw: String): Pair<GuideFeatureFlags, Long>? = try {
            val o = JSONObject(raw)
            val fetchedAt = (o.opt("fetchedAt") as? Number)?.toLong()
            val flags = parse(raw)
            if (fetchedAt == null || flags == null) null else flags to fetchedAt
        } catch (_: Exception) {
            null
        }
    }
}
