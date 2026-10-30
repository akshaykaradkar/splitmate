package com.splitmate.app.data

import android.content.Context
import com.splitmate.app.ui.LivePnrPassenger
import com.splitmate.app.ui.LivePnrStatusSnapshot
import com.splitmate.app.ui.ParsedTravelTicket
import com.splitmate.app.ui.extractTravelTicketFromTitle
import com.splitmate.app.ui.isFlightTicketExpense
import com.splitmate.app.ui.parseRupeesToPaiseExact
import com.splitmate.app.ui.resolveStationDisplayName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.random.Random

/**
 * 5-Layer Anti-Ban Live PNR Cascade & Encrypted Integer-Paise Vault (`PnrNetworkRepository`).
 *
 * Architecture:
 * - Vault Schema v2: Pure `Long` integer-paise (`baseFarePaise`, `totalPayablePaise`, `irctcPaymentMode`,
 *   `includeInsurance`, `fetchedAtEpochMs`) with zero `.40` paise truncation and zero `+₹25.00` compounding.
 * - 3-Tier Live Network Cascade (`5,000ms` connect / `10,000ms` read timeout):
 *   1. Tier 1: ConfirmTkt MWeb JSON REST API (`POST https://cttrainsapi.confirmtkt.com/api/v2/ctpro/mweb/{pnr}`)
 *      with two-speed `livePnr=false` cache-first strategy and `5m` cap on `livePnr=true`.
 *   2. Tier 2: RailMitra SSR HTML (`GET https://www.railmitra.com/pnr-status?pnr={pnr}`).
 *   3. Tier 3: RailYatri Rails SSR HTML (`GET https://www.railyatri.in/pnr-status/{pnr}`).
 * - 5-Layer Anti-Ban Rate Limiter:
 *   1. `60s` per-PNR manual refresh cooldown (`0` network calls while cooling down, returns `cooldownRemainingSeconds`).
 *   2. `5m` per-PNR cap on `livePnr=true` upstream CRIS forces.
 *   3. Single-flight request coalescing (`coalesceMutex` + `inFlightPnrDeferreds`).
 *   4. Persisted dual sliding-window token bucket (`max 3 / 1 min` burst + `max 8 / 10 min` sustained),
 *      `30m` negative cache on `errorCode == 201` (flushed/invalid PNR), exponential failure backoff
 *      (`15s -> 30s -> 60s -> 120s`), and CRIS nightly maintenance blackout (`23:30–00:30 IST`).
 *   5. `15-minute` (+ jitter) circuit breaker on `HTTP 429 / 403` failing over smoothly to Tier 2/3.
 */
object PnrNetworkRepository {

    internal const val VAULT_SCHEMA_VERSION = 2
    private const val KEY_VAULT_SCHEMA_VERSION = "pnr_vault_schema_version"

    internal const val AUTO_SYNC_COOLDOWN_MS = 6L * 60L * 60L * 1000L // 6 hours
    internal const val TRAVEL_DAY_AUTO_SYNC_COOLDOWN_MS = 30L * 60L * 1000L // 30 minutes on travel day
    internal const val MANUAL_REFRESH_COOLDOWN_MS = 60L * 1000L // 60 seconds per-PNR manual cooldown
    internal const val LIVE_UPSTREAM_FORCE_COOLDOWN_MS = 5L * 60L * 1000L // max 1 livePnr=true per 5 min per PNR
    internal const val UPSTREAM_STALE_TRAVEL_DAY_MS = 15L * 60L * 1000L // 15m upstream cache threshold on travel day
    internal const val UPSTREAM_STALE_DEFAULT_MS = 60L * 60L * 1000L // 60m upstream cache threshold off travel day

    internal const val BURST_WINDOW_MS = 60L * 1000L // 1 minute burst window
    internal const val BURST_MAX_CALLS = 3 // max 3 calls / 1 minute globally
    internal const val SUSTAINED_WINDOW_MS = 10L * 60L * 1000L // 10 minute sustained window
    internal const val SUSTAINED_MAX_CALLS = 8 // max 8 calls / 10 minutes globally

    internal const val INVALID_PNR_NEGATIVE_TTL_MS = 30L * 60L * 1000L // 30 minutes negative cache for errorCode 201
    internal const val CIRCUIT_BREAKER_BASE_MS = 15L * 60L * 1000L // 15 minutes circuit breaker on 429/403
    internal const val CIRCUIT_BREAKER_MAX_JITTER_MS = 180L * 1000L // 0..180s jitter

    internal const val CONNECT_TIMEOUT_MS = 5_000
    internal const val READ_TIMEOUT_MS = 10_000

    data class PnrHttpRequest(
        val url: String,
        val method: String = "GET",
        val headers: Map<String, String> = emptyMap(),
        val body: String? = null,
        val connectTimeoutMs: Int = CONNECT_TIMEOUT_MS,
        val readTimeoutMs: Int = READ_TIMEOUT_MS
    )

    data class PnrHttpResponse(
        val statusCode: Int,
        val body: String,
        val headers: Map<String, String> = emptyMap()
    )

    fun interface PnrHttpTransport {
        fun execute(request: PnrHttpRequest): PnrHttpResponse
    }

    private val defaultHttpTransport = PnrHttpTransport { req ->
        val conn = (URL(req.url).openConnection() as HttpURLConnection).apply {
            requestMethod = req.method
            connectTimeout = req.connectTimeoutMs
            readTimeout = req.readTimeoutMs
            req.headers.forEach { (k, v) -> setRequestProperty(k, v) }
            if (req.body != null) {
                doOutput = true
            }
        }
        try {
            if (req.body != null) {
                conn.outputStream.use { out ->
                    out.write(req.body.toByteArray(Charsets.UTF_8))
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val bodyText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            PnrHttpResponse(statusCode = code, body = bodyText)
        } finally {
            conn.disconnect()
        }
    }

    @Volatile
    internal var httpTransportOverride: PnrHttpTransport? = null

    @Volatile
    internal var clockMsProvider: () -> Long = { System.currentTimeMillis() }

    @Volatile
    internal var bypassCrisNightBlackoutForTest: Boolean = false

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val coalesceMutex = Mutex()
    private val rateLimitMutex = Mutex()
    private val vaultDiskLock = Any()
    private val inFlightPnrDeferreds = HashMap<String, Deferred<LivePnrStatusSnapshot>>()

    private const val MAX_CACHE_ENTRIES = 64

    private val lruSnapshotCache = object : LinkedHashMap<String, LivePnrStatusSnapshot>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LivePnrStatusSnapshot>?): Boolean = size > MAX_CACHE_ENTRIES
    }
    private val lruFlightResultCache = object : LinkedHashMap<String, UniversalFlightTicketExtractor.UniversalFlightTicketResult>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, UniversalFlightTicketExtractor.UniversalFlightTicketResult>?): Boolean = size > MAX_CACHE_ENTRIES
    }
    private val reconstructedFlightByExpenseCache = object : LinkedHashMap<String, UniversalFlightTicketExtractor.UniversalFlightTicketResult>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, UniversalFlightTicketExtractor.UniversalFlightTicketResult>?): Boolean = size > MAX_CACHE_ENTRIES
    }
    private val missingFlightResultPnrs = java.util.Collections.synchronizedSet(HashSet<String>())
    private val missingSnapshotPnrs = java.util.Collections.synchronizedSet(HashSet<String>())
    private val lastLiveNetSyncEpochMsCache = object : LinkedHashMap<String, Long>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean = size > MAX_CACHE_ENTRIES
    }
    private val lastForceLiveUpstreamMsCache = object : LinkedHashMap<String, Long>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean = size > MAX_CACHE_ENTRIES
    }
    private val failureStateByPnrCache = HashMap<String, Pair<Int, Long>>() // count to backoffUntilMs
    private val invalidPnrUntilCache = HashMap<String, Pair<Long, String>>() // untilMs to message
    private val globalFetchEpochsMemory = ArrayList<Long>()

    @Volatile
    private var confirmTktCircuitOpenUntilMsMemory: Long = 0L

    @Volatile
    private var installationDeviceIdMemory: String = ""

    private const val MAX_VAULT_ENTRIES = 75
    private const val KEY_LRU_INDEX = "vault_pnr_lru_index"

    private fun ensureVaultSchemaMigrated(prefs: android.content.SharedPreferences) {
        synchronized(vaultDiskLock) {
            val savedVersion = prefs.getInt(KEY_VAULT_SCHEMA_VERSION, 1)
            if (savedVersion >= VAULT_SCHEMA_VERSION) return
            val editor = prefs.edit()
            prefs.all.keys.forEach { key ->
                if (key.startsWith("snapshot_json_") ||
                    key.startsWith("last_sync_") ||
                    key.startsWith("last_failed_sync_")
                ) {
                    editor.remove(key)
                }
            }
            editor.putInt(KEY_VAULT_SCHEMA_VERSION, VAULT_SCHEMA_VERSION)
            editor.apply()
        }
    }

    private fun getOrCreateDeviceId(prefs: android.content.SharedPreferences?): String {
        val currentMem = installationDeviceIdMemory
        if (currentMem.isNotBlank()) return currentMem
        synchronized(vaultDiskLock) {
            if (installationDeviceIdMemory.isNotBlank()) return installationDeviceIdMemory
            val fromDisk = prefs?.getString("ct_installation_device_id", null).orEmpty()
            if (fromDisk.isNotBlank()) {
                installationDeviceIdMemory = fromDisk
                return fromDisk
            }
            val generated = UUID.randomUUID().toString()
            installationDeviceIdMemory = generated
            prefs?.edit()?.putString("ct_installation_device_id", generated)?.apply()
            return generated
        }
    }

    /**
     * Clears all in-memory and persisted Vault caches (called on `clearLocalVault()` and test teardown).
     */
    fun clearAllCaches(context: Context? = null) {
        synchronized(lruSnapshotCache) { lruSnapshotCache.clear() }
        synchronized(lruFlightResultCache) { lruFlightResultCache.clear() }
        synchronized(reconstructedFlightByExpenseCache) { reconstructedFlightByExpenseCache.clear() }
        missingFlightResultPnrs.clear()
        missingSnapshotPnrs.clear()
        synchronized(lastLiveNetSyncEpochMsCache) { lastLiveNetSyncEpochMsCache.clear() }
        synchronized(lastForceLiveUpstreamMsCache) { lastForceLiveUpstreamMsCache.clear() }
        synchronized(failureStateByPnrCache) { failureStateByPnrCache.clear() }
        synchronized(invalidPnrUntilCache) { invalidPnrUntilCache.clear() }
        synchronized(globalFetchEpochsMemory) { globalFetchEpochsMemory.clear() }
        confirmTktCircuitOpenUntilMsMemory = 0L
        if (context != null) {
            runCatching {
                synchronized(vaultDiskLock) {
                    val prefs = EncryptedPrefsProvider.getPnrVaultPrefs(context)
                    prefs.edit().clear().putInt(KEY_VAULT_SCHEMA_VERSION, VAULT_SCHEMA_VERSION).apply()
                }
            }
        }
    }

    /**
     * Evicts a single PNR from both L1 memory and L2 EncryptedSharedPreferences when an expense is
     * deleted or edited so stale fares/routes never resurrect from Vault (`D-01`, `D-05`).
     */
    fun evictPnrFromVault(context: Context? = null, pnr: String) {
        val cleanPnr = normalizePnrKey(pnr).ifBlank { pnr.trim().uppercase(Locale.US) }
        if (cleanPnr.isBlank()) return
        synchronized(lruSnapshotCache) { lruSnapshotCache.remove(cleanPnr) }
        synchronized(lruFlightResultCache) { lruFlightResultCache.remove(cleanPnr) }
        synchronized(reconstructedFlightByExpenseCache) { reconstructedFlightByExpenseCache.clear() }
        synchronized(lastLiveNetSyncEpochMsCache) { lastLiveNetSyncEpochMsCache.remove(cleanPnr) }
        synchronized(lastForceLiveUpstreamMsCache) { lastForceLiveUpstreamMsCache.remove(cleanPnr) }
        missingSnapshotPnrs.add(cleanPnr)
        missingFlightResultPnrs.add(cleanPnr)
        if (context != null) {
            runCatching {
                synchronized(vaultDiskLock) {
                    val prefs = EncryptedPrefsProvider.getPnrVaultPrefs(context)
                    ensureVaultSchemaMigrated(prefs)
                    val updatedQueue = prefs.getString(KEY_LRU_INDEX, "").orEmpty()
                        .split(",")
                        .filter { it.isNotBlank() && it != cleanPnr }
                    prefs.edit()
                        .remove("snapshot_json_$cleanPnr")
                        .remove("flight_result_json_$cleanPnr")
                        .remove("last_sync_$cleanPnr")
                        .remove("last_live_net_sync_$cleanPnr")
                        .remove("last_failed_sync_$cleanPnr")
                        .putString(KEY_LRU_INDEX, updatedQueue.joinToString(","))
                        .apply()
                }
            }
        }
    }

    fun invalidateReconstructedFlightCacheForExpense(expenseId: String) {
        if (expenseId.isBlank()) return
        synchronized(reconstructedFlightByExpenseCache) {
            val keysToRemove = reconstructedFlightByExpenseCache.keys.filter { it.startsWith("${expenseId}_") }
            keysToRemove.forEach { reconstructedFlightByExpenseCache.remove(it) }
        }
    }

    fun exportAllFlightVaultJsonByPnr(context: Context): Map<String, String> {
        val prefs = EncryptedPrefsProvider.getPnrVaultPrefs(context)
        ensureVaultSchemaMigrated(prefs)
        return prefs.all
            .filterKeys { it.startsWith("flight_result_json_") }
            .mapKeys { it.key.removePrefix("flight_result_json_") }
            .mapValues { it.value.toString() }
    }

    /**
     * Group-scoped Flight Vault export (`D-04`): exports ONLY flight tickets referenced by the
     * target group's expenses so personal PII/tickets from other groups never leak over sync.
     */
    internal fun filterVaultMapForAllowedPnrs(
        allEntries: Map<String, String>,
        allowedPnrs: Set<String>
    ): Map<String, String> {
        if (allEntries.isEmpty() || allowedPnrs.isEmpty()) return emptyMap()
        val normalizedAllowed = allowedPnrs.map { normalizePnrKey(it).ifBlank { it.trim().uppercase(Locale.US) } }
            .filter { it.isNotBlank() }
            .toSet()
        if (normalizedAllowed.isEmpty()) return emptyMap()
        return allEntries.filterKeys { key ->
            val normKey = normalizePnrKey(key).ifBlank { key.trim().uppercase(Locale.US) }
            normKey in normalizedAllowed
        }
    }

    fun exportGroupScopedFlightVaultJsonByPnr(
        context: Context?,
        allowedPnrs: Set<String>
    ): Map<String, String> {
        val rawMap = if (context != null) {
            exportAllFlightVaultJsonByPnr(context)
        } else {
            synchronized(lruFlightResultCache) {
                lruFlightResultCache.mapValues { (pnr, res) ->
                    JSONObject().apply {
                        put("pnr", pnr)
                        put("flightNumber", res.flightNumber)
                        put("airlineName", res.airlineName)
                        put("originIata", res.originIata)
                        put("destinationIata", res.destinationIata)
                        put("totalFarePaise", res.totalFarePaise)
                    }.toString()
                }
            }
        }
        return filterVaultMapForAllowedPnrs(rawMap, allowedPnrs)
    }

    fun importFlightVaultJsonByPnr(context: Context, map: Map<String, String>) {
        if (map.isEmpty()) return
        synchronized(vaultDiskLock) {
            val prefs = EncryptedPrefsProvider.getPnrVaultPrefs(context)
            ensureVaultSchemaMigrated(prefs)
            var hasChanges = false
            val editor = prefs.edit()
            map.forEach { (pnr, json) ->
                val cleanPnr = normalizePnrKey(pnr).ifBlank { pnr }
                missingFlightResultPnrs.remove(cleanPnr)
                if (prefs.getString("flight_result_json_$cleanPnr", null) != json) {
                    synchronized(lruFlightResultCache) {
                        lruFlightResultCache.remove(cleanPnr)
                    }
                    editor.putString("flight_result_json_$cleanPnr", json)
                    recordAndEvictLruIfNeededLocked(prefs, editor, cleanPnr)
                    hasChanges = true
                }
            }
            if (hasChanges) {
                editor.apply()
            }
        }
    }

    fun exportAllTrainSnapshotJsonByPnr(context: Context): Map<String, String> {
        val prefs = EncryptedPrefsProvider.getPnrVaultPrefs(context)
        ensureVaultSchemaMigrated(prefs)
        return prefs.all
            .filterKeys { it.startsWith("snapshot_json_") }
            .mapKeys { it.key.removePrefix("snapshot_json_") }
            .mapValues { it.value.toString() }
    }

    /**
     * Group-scoped Train Snapshot export (`D-04`): exports ONLY train PNRs referenced by the
     * target group's expenses.
     */
    fun exportGroupScopedTrainSnapshotJsonByPnr(
        context: Context?,
        allowedPnrs: Set<String>
    ): Map<String, String> {
        val rawMap = if (context != null) {
            exportAllTrainSnapshotJsonByPnr(context)
        } else {
            val now = clockMsProvider()
            synchronized(lruSnapshotCache) {
                lruSnapshotCache.mapValues { (pnr, snap) ->
                    serializeSnapshotJson(pnr, snap, if (snap.fetchedAtEpochMs > 0L) snap.fetchedAtEpochMs else now)
                }
            }
        }
        return filterVaultMapForAllowedPnrs(rawMap, allowedPnrs)
    }

    /**
     * Monotonic CNF + Timestamp Last-Writer-Wins comparator (`D-03`):
     * Never allows a stale remote `WL`/`RAC` snapshot to overwrite a newer or already fully-confirmed
     * (`CNF` with berth/chart) local snapshot.
     */
    internal fun pickNewerTrainSnapshotJson(localJson: String?, remoteJson: String): String {
        if (localJson.isNullOrBlank()) return remoteJson
        if (remoteJson.isBlank()) return localJson
        val localSnap = deserializeSnapshotJson("0000000000", localJson) ?: return remoteJson
        val remoteSnap = deserializeSnapshotJson(localSnap.pnr, remoteJson) ?: return localJson

        val localConfirmed = isSnapshotAllConfirmed(localSnap)
        val remoteConfirmed = isSnapshotAllConfirmed(remoteSnap)
        if (localConfirmed && !remoteConfirmed) return localJson
        if (remoteConfirmed && !localConfirmed) return remoteJson

        if (!remoteSnap.isLiveVerified && localSnap.isLiveVerified) return localJson
        if (remoteSnap.isLiveVerified && !localSnap.isLiveVerified) return remoteJson

        return if (remoteSnap.fetchedAtEpochMs > localSnap.fetchedAtEpochMs) {
            remoteJson
        } else {
            localJson
        }
    }

    fun importTrainSnapshotJsonByPnr(context: Context, map: Map<String, String>) {
        if (map.isEmpty()) return
        synchronized(vaultDiskLock) {
            val prefs = EncryptedPrefsProvider.getPnrVaultPrefs(context)
            ensureVaultSchemaMigrated(prefs)
            var hasChanges = false
            val editor = prefs.edit()
            map.forEach { (pnr, remoteJson) ->
                val cleanPnr = normalizePnrKey(pnr).ifBlank { pnr }
                val existingLocalJson = prefs.getString("snapshot_json_$cleanPnr", null)
                val winningJson = pickNewerTrainSnapshotJson(existingLocalJson, remoteJson)
                missingSnapshotPnrs.remove(cleanPnr)
                if (existingLocalJson != winningJson) {
                    synchronized(lruSnapshotCache) {
                        lruSnapshotCache.remove(cleanPnr)
                    }
                    editor.putString("snapshot_json_$cleanPnr", winningJson)
                    recordAndEvictLruIfNeededLocked(prefs, editor, cleanPnr)
                    hasChanges = true
                }
            }
            if (hasChanges) {
                editor.apply()
            }
        }
    }

    private fun recordAndEvictLruIfNeededLocked(
        prefs: android.content.SharedPreferences,
        editor: android.content.SharedPreferences.Editor,
        currentPnr: String
    ) {
        val existingQueue = prefs.getString(KEY_LRU_INDEX, "").orEmpty()
            .split(",")
            .filter { it.isNotBlank() && it != currentPnr }
            .toMutableList()
        existingQueue.add(currentPnr)
        while (existingQueue.size > MAX_VAULT_ENTRIES) {
            val oldest = existingQueue.removeAt(0)
            editor.remove("snapshot_json_$oldest")
                .remove("last_sync_$oldest")
                .remove("last_live_net_sync_$oldest")
                .remove("last_failed_sync_$oldest")
                .remove("flight_result_json_$oldest")
        }
        editor.putString(KEY_LRU_INDEX, existingQueue.joinToString(","))
    }

    /**
     * Normalizes either a 10-digit Indian Railways PNR (e.g., `8412659012`)
     * or a 6-character Airline/GDS PNR (e.g., `D9GQ3Z`).
     */
    fun normalizePnrKey(pnr: String): String {
        val trimmed = pnr.trim()
        if (trimmed.isBlank()) return ""
        val compact = trimmed.replace(Regex("""[\s\-]"""), "")
        if (compact.length == 6 && compact.all { it.isLetterOrDigit() && it.code < 128 }) {
            return compact.uppercase(Locale.US)
        }
        return extractPnrFromFreeText(trimmed)
    }

    /**
     * Extracts a PNR from free text such as an expense title. Plain English words are rejected.
     */
    fun extractPnrFromFreeText(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return ""

        Regex("""PNR[:\s\-]+([A-Za-z0-9]{6,10})\b""", RegexOption.IGNORE_CASE)
            .find(trimmed)?.groupValues?.getOrNull(1)?.uppercase(Locale.US)?.let { tagged ->
                if (tagged.length == 10 && tagged.all { it.isDigit() }) return tagged
                if (tagged.length == 6) return tagged
            }

        Regex("""\b(\d{10})\b""").find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }

        Regex("""\b([A-Za-z0-9]{6})\b""").findAll(trimmed).forEach { match ->
            val token = match.value.uppercase(Locale.US)
            if (token.any { it.isLetter() } && token.any { it.isDigit() }) {
                return token
            }
        }

        return ""
    }

    private fun hasConcreteCoachAndBerth(statusText: String): Boolean {
        val clean = statusText.substringAfter(":").trim().uppercase(Locale.US)
        if (clean.isBlank()) return false
        if (clean == "CNF" || clean == "CONFIRMED" || clean == "CNF/-" || clean == "CNF/0" || clean == "CNF / 0" || clean == "CNF / -") {
            return false
        }
        // Matches e.g. "CNF/B2/45", "CNF / A1 / 12", "CNF (Seat 12A)", "CNF (2A)", "B1/14", "H1/A"
        return Regex("""(?:CNF\s*[/,(]\s*[A-Z0-9]{1,8}|[A-Z]{1,3}\d*\s*[/,]\s*[A-Z0-9]{1,4})""").containsMatchIn(clean)
    }

    /**
     * Returns `true` when every passenger on the snapshot is Confirmed (`CNF`) with zero `WL`/`RAC`
     * AND (either `chartPrepared == true`, it is a 6-char Flight PNR, OR every passenger already has a
     * concrete coach/berth assignment). Pre-chart `CNF` without coach/berth (e.g., `1A` First AC or
     * dynamic Vande Bharat quotas) stays refreshable (`R-04`).
     */
    fun isSnapshotAllConfirmed(snapshot: LivePnrStatusSnapshot?): Boolean {
        if (snapshot == null || !snapshot.isLiveVerified || snapshot.isInvalidOrFlushedPnr) return false
        val badge = snapshot.bookingStatusBadge.trim().uppercase(Locale.US)
        val badgeIsCnf = badge.startsWith("CNF") || badge == "CONFIRMED"
        if (!badgeIsCnf) return false

        val hasPendingStatus = snapshot.passengerStatuses.any { status ->
            val u = status.uppercase(Locale.US)
            u.contains("WL") || u.contains("RAC") || u.contains("WAITLIST")
        } || snapshot.structuredPassengers.any { sp ->
            val u = "${sp.currentStatus} ${sp.statusLabel}".uppercase(Locale.US)
            u.contains("WL") || u.contains("RAC") || u.contains("WAITLIST")
        }
        if (hasPendingStatus) return false

        val cleanKey = normalizePnrKey(snapshot.pnr)
        if (cleanKey.length == 6 || snapshot.chartPrepared) return true

        val paxEntries = if (snapshot.structuredPassengers.isNotEmpty()) {
            snapshot.structuredPassengers.map { it.currentStatus.ifBlank { it.initialStatus } }
        } else {
            snapshot.passengerStatuses
        }
        if (paxEntries.isEmpty()) return false
        return paxEntries.all { hasConcreteCoachAndBerth(it) }
    }

    /**
     * Returns `true` if a `ParsedTravelTicket` is already fully confirmed (`CNF` with concrete seats or Flight PDF).
     */
    fun isTicketAllConfirmed(ticket: ParsedTravelTicket): Boolean {
        if (!ticket.statusExplicit) return false
        val key = normalizePnrKey(ticket.pnr)
        if (key.length != 6 && key.length != 10) return false
        if (ticket.trainOrFlightNo.isBlank() && ticket.fromStation.isBlank() && ticket.coachAndSeats.isBlank()) {
            return false
        }
        val badge = ticket.bookingStatus.trim().uppercase(Locale.US)
        val isCnf = badge.startsWith("CNF") || badge == "CONFIRMED" || (key.length == 6 && ticket.chartStatus.contains("Flight", ignoreCase = true))
        if (!isCnf) return false
        if (badge.contains("WL") || badge.contains("RAC") || badge.contains("WAITLIST")) return false
        val seatsUpper = ticket.coachAndSeats.uppercase(Locale.US)
        if (seatsUpper.contains("WL") || seatsUpper.contains("RAC") || seatsUpper.contains("WAITLIST")) return false
        if (key.length == 10 && ticket.chartStatus != "Chart Prepared") {
            if (seatsUpper.isBlank() || (!hasConcreteCoachAndBerth(seatsUpper) && !Regex("""[A-Z]\d+\s*[/,]\s*\d+""").containsMatchIn(seatsUpper))) {
                return false
            }
        }
        return true
    }

    /**
     * Returns `true` ONLY when an explicit calendar date (`parsed.hasDate == true`) is present and
     * the journey's completion window is strictly before [nowMs]. Bare departure times like `"11:25"`
     * NEVER lock a future ticket as completed (`R-05`).
     */
    fun isTicketJourneyInPast(
        ticket: ParsedTravelTicket,
        cachedSnapshot: LivePnrStatusSnapshot? = null,
        nowMs: Long = clockMsProvider()
    ): Boolean {
        val snapshotDateAndTime = cachedSnapshot?.let {
            listOf(it.departureDate, it.departureTime).filter { s -> s.isNotBlank() }.joinToString(" ")
        }?.takeIf { it.isNotBlank() }

        val candidates = listOfNotNull(
            ticket.departureInfo.takeIf { it.isNotBlank() },
            ticket.departureDate.takeIf { it.isNotBlank() },
            snapshotDateAndTime,
            cachedSnapshot?.departureDate?.takeIf { it.isNotBlank() },
            cachedSnapshot?.departureTime?.takeIf { it.isNotBlank() }
        )
        val durationMs = TicketDateTimeParser.parseDurationMs(cachedSnapshot?.durationText) ?: (12 * 3600_000L)

        for (raw in candidates) {
            val parsed = TicketDateTimeParser.parseDetailed(raw, nowMs, TicketDateTimeParser.IST) ?: continue
            if (!parsed.hasDate) continue
            val effectiveEpochMs = if (parsed.hasTime) {
                parsed.epochMs + durationMs
            } else {
                parsed.epochMs + 24 * 3600_000L
            }
            if (effectiveEpochMs < nowMs) {
                return true
            }
        }
        return false
    }

    /**
     * CRIS nightly maintenance window (`23:30–00:30 IST`).
     */
    fun isWithinCrisNightlyMaintenance(nowMs: Long = clockMsProvider()): Boolean {
        if (bypassCrisNightBlackoutForTest) return false
        val istNow = Instant.ofEpochMilli(nowMs).atZone(TicketDateTimeParser.IST)
        val hour = istNow.hour
        val minute = istNow.minute
        return (hour == 23 && minute >= 30) || (hour == 0 && minute <= 30)
    }

    internal fun isTicketOnTravelDayIst(
        ticket: ParsedTravelTicket,
        cachedSnapshot: LivePnrStatusSnapshot?,
        nowMs: Long = clockMsProvider()
    ): Boolean {
        val istToday = Instant.ofEpochMilli(nowMs).atZone(TicketDateTimeParser.IST).toLocalDate()
        val candidates = listOfNotNull(
            ticket.departureDate.takeIf { it.isNotBlank() },
            ticket.departureInfo.takeIf { it.isNotBlank() },
            cachedSnapshot?.departureDate?.takeIf { it.isNotBlank() },
            cachedSnapshot?.departureTime?.takeIf { it.isNotBlank() }
        )
        for (raw in candidates) {
            val parsed = TicketDateTimeParser.parseDetailed(raw, nowMs, TicketDateTimeParser.IST) ?: continue
            if (!parsed.hasDate) continue
            val parsedDate = Instant.ofEpochMilli(parsed.epochMs).atZone(TicketDateTimeParser.IST).toLocalDate()
            if (parsedDate == istToday) return true
        }
        return false
    }

    suspend fun seedConfirmedTicketsFromExpenses(
        context: Context?,
        expenses: List<ExpenseEntity>,
        members: List<GroupMemberEntity> = emptyList(),
        splits: List<ExpenseSplitEntity> = emptyList()
    ) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            for (expense in expenses) {
                val parsed = extractTravelTicketFromTitle(expense.title)
                val rawPnr = expense.travelPnr.trim().ifBlank { parsed?.pnr?.trim().orEmpty() }
                val cleanPnr = normalizePnrKey(rawPnr)
                if (cleanPnr.length == 10 && parsed != null) {
                    val existing = loadPersistedPnrSnapshot(context, cleanPnr)
                    if (existing != null && isSnapshotAllConfirmed(existing)) continue
                    val enriched = if (parsed.fareRupees.isBlank() && expense.totalAmountCents > 0L) {
                        val exactDecimal = formatCentsPlainDecimal(expense.totalAmountCents)
                        parsed.copy(pnr = cleanPnr, fareRupees = exactDecimal)
                    } else {
                        parsed.copy(pnr = cleanPnr)
                    }
                    if (isTicketAllConfirmed(enriched) || isTicketJourneyInPast(enriched, existing)) {
                        val promoted = buildConfirmedSnapshotFromTicket(cleanPnr, enriched, expense.totalAmountCents)
                        savePersistedPnrSnapshot(context, promoted)
                    }
                } else if (cleanPnr.length == 6) {
                    if (parsed != null) {
                        val existing = loadPersistedPnrSnapshot(context, cleanPnr)
                        if (existing != null && isSnapshotAllConfirmed(existing)) continue
                        val enriched = parsed.copy(pnr = cleanPnr)
                        if (isTicketAllConfirmed(enriched) || isTicketJourneyInPast(enriched, existing)) {
                            val promoted = buildConfirmedSnapshotFromTicket(cleanPnr, enriched, expense.totalAmountCents)
                            savePersistedPnrSnapshot(context, promoted)
                        }
                    }
                }
            }
        }
    }

    private fun formatCentsPlainDecimal(paise: Long): String {
        val whole = paise / 100L
        val rem = kotlin.math.abs(paise % 100L)
        return if (rem == 0L) "$whole" else String.format(Locale.US, "%d.%02d", whole, rem)
    }

    fun saveConfirmedFlightTicketToVault(
        context: Context?,
        result: UniversalFlightTicketExtractor.UniversalFlightTicketResult
    ): LivePnrStatusSnapshot? {
        val cleanPnr = normalizePnrKey(result.pnr)
        if ((cleanPnr.length != 6 && cleanPnr.length != 10) || !result.isValidFlightTicket) return null
        missingFlightResultPnrs.remove(cleanPnr)
        synchronized(lruFlightResultCache) {
            lruFlightResultCache[cleanPnr] = result
        }
        if (context != null) {
            runCatching {
                val flightObj = JSONObject().apply {
                    put("pnr", cleanPnr)
                    put("otaBookingId", result.otaBookingId)
                    put("airlineCode", result.airlineCode)
                    put("airlineName", result.airlineName)
                    put("flightNumber", result.flightNumber)
                    put("originIata", result.originIata)
                    put("originCity", result.originCity)
                    put("originAirportName", result.originAirportName)
                    put("destinationIata", result.destinationIata)
                    put("destinationCity", result.destinationCity)
                    put("destinationAirportName", result.destinationAirportName)
                    put("travelDate", result.travelDate)
                    put("bookingDate", result.bookingDate)
                    put("departureTime", result.departureTime)
                    put("arrivalTime", result.arrivalTime)
                    put("durationText", result.durationText)
                    put("cabinClass", result.cabinClass)
                    put("fareType", result.fareType)
                    put("cabinBaggage", result.cabinBaggage)
                    put("checkInBaggage", result.checkInBaggage)
                    put("totalFarePaise", result.totalFarePaise)
                    put("discountSavedPaise", result.discountSavedPaise)
                    put("paymentMethod", result.paymentMethod)
                    put("viaAirports", JSONArray().apply { result.viaAirports.forEach { put(it) } })
                    put("matchedGroupMembers", JSONArray().apply { result.matchedGroupMembers.forEach { put(it) } })
                    put("passengers", JSONArray().apply {
                        result.passengers.forEach { pax ->
                            put(JSONObject().apply {
                                put("fullName", pax.fullName)
                                put("seatNumber", pax.seatNumber)
                                put("eTicketOrPnr", pax.eTicketOrPnr)
                                put("passengerType", pax.passengerType)
                            })
                        }
                    })
                }
                val newJson = flightObj.toString()
                synchronized(vaultDiskLock) {
                    val prefs = EncryptedPrefsProvider.getPnrVaultPrefs(context)
                    ensureVaultSchemaMigrated(prefs)
                    if (prefs.getString("flight_result_json_$cleanPnr", null) != newJson) {
                        val editor = prefs.edit().putString("flight_result_json_$cleanPnr", newJson)
                        recordAndEvictLruIfNeededLocked(prefs, editor, cleanPnr)
                        editor.apply()
                    }
                }
            }
        }
        val paxStatuses = if (result.passengers.isNotEmpty()) {
            result.passengers.mapIndexed { idx, pax ->
                val seatPart = if (pax.seatNumber.isNotBlank() && pax.seatNumber != "-") "Seat ${pax.seatNumber}" else "CNF"
                "P${idx + 1}: ${pax.fullName} ($seatPart · ${pax.passengerType})"
            }
        } else {
            listOf("P1: Confirmed Passenger (CNF)")
        }
        val structuredPax = if (result.passengers.isNotEmpty()) {
            result.passengers.mapIndexed { idx, pax ->
                val seatCode = if (pax.seatNumber.isNotBlank() && pax.seatNumber != "-") "CNF / ${pax.seatNumber}" else "CNF"
                LivePnrPassenger(
                    passengerNumber = pax.fullName.ifBlank { "P${idx + 1}" },
                    initialStatus = seatCode,
                    currentStatus = seatCode,
                    statusLabel = "Confirmed (${pax.passengerType})"
                )
            }
        } else {
            listOf(LivePnrPassenger("P1", "CNF", "CNF", "Confirmed"))
        }
        val snapshot = LivePnrStatusSnapshot(
            pnr = cleanPnr,
            trainNo = result.flightNumber,
            trainName = result.airlineName,
            fromStation = result.originIata,
            toStation = result.destinationIata,
            departureTime = listOf(result.travelDate, result.departureTime).filter { it.isNotBlank() }.joinToString(" • "),
            travelClass = listOf(result.cabinClass, result.fareType).filter { it.isNotBlank() }.joinToString(" • "),
            totalFareRupees = (result.totalFarePaise / 100L).toInt(),
            passengerCount = result.passengers.size.coerceAtLeast(1),
            bookingStatusBadge = "CNF (Confirmed)",
            chartPrepared = true,
            passengerStatuses = paxStatuses,
            structuredPassengers = structuredPax,
            fromStationName = result.originCity.ifBlank { result.originIata },
            toStationName = result.destinationCity.ifBlank { result.destinationIata },
            arrivalTime = result.arrivalTime,
            durationText = result.durationText,
            quotaText = result.fareType,
            coachPositionHint = listOf(
                if (result.cabinBaggage.isNotBlank()) "Cabin: ${result.cabinBaggage}" else "",
                if (result.checkInBaggage.isNotBlank()) "Check-in: ${result.checkInBaggage}" else ""
            ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Confirmed Ticket · Offline Ready" },
            liveTrainLocationRadar = listOf(
                "${result.airlineName} ${result.flightNumber}".trim(),
                if (result.originIata.isNotBlank() && result.destinationIata.isNotBlank()) {
                    "${result.originCity.ifBlank { result.originIata }} -> ${result.destinationCity.ifBlank { result.destinationIata }}"
                } else ""
            ).filter { it.isNotBlank() }.joinToString(" · "),
            confirmationProbability = "100% Confirmed · Saved in Offline PNR Vault",
            sourceLabel = "Confirmed Offline Vault (E-Ticket PDF · 0 Internet Used)",
            isLiveVerified = true,
            isManualEntry = false,
            baseFarePaiseOverride = result.totalFarePaise,
            totalPayablePaiseOverride = result.totalFarePaise,
            departureDate = result.travelDate,
            fetchedAtEpochMs = clockMsProvider()
        )
        savePersistedPnrSnapshot(context, snapshot)
        return snapshot
    }

    fun loadConfirmedFlightTicketResult(
        context: Context?,
        pnr: String
    ): UniversalFlightTicketExtractor.UniversalFlightTicketResult? {
        val cleanPnr = normalizePnrKey(pnr)
        if (cleanPnr.length != 6 && cleanPnr.length != 10) return null
        synchronized(lruFlightResultCache) {
            lruFlightResultCache[cleanPnr]?.let { return it }
        }
        if (missingFlightResultPnrs.contains(cleanPnr)) return null
        if (context == null) return null
        return runCatching {
            val prefs = EncryptedPrefsProvider.getPnrVaultPrefs(context)
            ensureVaultSchemaMigrated(prefs)
            val rawJson = prefs.getString("flight_result_json_$cleanPnr", null)
            if (rawJson == null) {
                missingFlightResultPnrs.add(cleanPnr)
                return@runCatching null
            }
            val obj = JSONObject(rawJson)
            val paxArr = obj.optJSONArray("passengers") ?: JSONArray()
            val passengers = (0 until paxArr.length()).mapNotNull { i ->
                val pObj = paxArr.optJSONObject(i) ?: return@mapNotNull null
                UniversalFlightTicketExtractor.ExtractedFlightPassenger(
                    fullName = pObj.optString("fullName"),
                    seatNumber = pObj.optString("seatNumber", ""),
                    eTicketOrPnr = pObj.optString("eTicketOrPnr", cleanPnr),
                    passengerType = pObj.optString("passengerType", "ADULT")
                )
            }
            val viaArr = obj.optJSONArray("viaAirports") ?: JSONArray()
            val viaAirports = (0 until viaArr.length()).map { viaArr.optString(it) }.filter { it.isNotBlank() }
            val matchedArr = obj.optJSONArray("matchedGroupMembers") ?: JSONArray()
            val matchedMembers = (0 until matchedArr.length()).map { matchedArr.optString(it) }.filter { it.isNotBlank() }
            val restored = UniversalFlightTicketExtractor.UniversalFlightTicketResult(
                pnr = cleanPnr,
                otaBookingId = obj.optString("otaBookingId"),
                airlineCode = obj.optString("airlineCode"),
                airlineName = obj.optString("airlineName"),
                flightNumber = obj.optString("flightNumber"),
                originIata = obj.optString("originIata"),
                originCity = obj.optString("originCity"),
                originAirportName = obj.optString("originAirportName"),
                destinationIata = obj.optString("destinationIata"),
                destinationCity = obj.optString("destinationCity"),
                destinationAirportName = obj.optString("destinationAirportName"),
                viaAirports = viaAirports,
                travelDate = obj.optString("travelDate"),
                bookingDate = obj.optString("bookingDate"),
                departureTime = obj.optString("departureTime"),
                arrivalTime = obj.optString("arrivalTime"),
                durationText = obj.optString("durationText"),
                cabinClass = obj.optString("cabinClass", ""),
                fareType = obj.optString("fareType", ""),
                cabinBaggage = obj.optString("cabinBaggage"),
                checkInBaggage = obj.optString("checkInBaggage"),
                passengers = passengers,
                matchedGroupMembers = matchedMembers,
                totalFarePaise = obj.optLong("totalFarePaise", 0L),
                discountSavedPaise = obj.optLong("discountSavedPaise", 0L),
                paymentMethod = obj.optString("paymentMethod"),
                extractionDurationMs = 0L
            )
            synchronized(lruFlightResultCache) {
                lruFlightResultCache[cleanPnr] = restored
            }
            restored
        }.getOrNull()
    }

    fun reconstructFlightTicketFromExpense(
        context: Context?,
        expense: ExpenseEntity,
        groupMembers: List<GroupMemberEntity>,
        allSplits: List<ExpenseSplitEntity>
    ): UniversalFlightTicketExtractor.UniversalFlightTicketResult {
        val expenseCacheKey = "${expense.expenseId}_${expense.title.hashCode()}_${expense.totalAmountCents}_${groupMembers.size}"
        synchronized(reconstructedFlightByExpenseCache) {
            reconstructedFlightByExpenseCache[expenseCacheKey]?.let { return it }
        }

        val parsed = extractTravelTicketFromTitle(expense.title)
        val rawPnr = expense.travelPnr.trim().takeIf { it.isNotBlank() }
            ?: parsed?.pnr?.trim()?.takeIf { it.isNotBlank() }
            ?: extractPnrFromFreeText(expense.title).takeIf { it.length == 6 }
            ?: ""

        if (rawPnr.length == 6) {
            loadConfirmedFlightTicketResult(context, rawPnr)?.let { vaultFlight ->
                val reconciled = if (expense.totalAmountCents > 0L && vaultFlight.totalFarePaise != expense.totalAmountCents) {
                    vaultFlight.copy(totalFarePaise = expense.totalAmountCents)
                } else {
                    vaultFlight
                }
                synchronized(reconstructedFlightByExpenseCache) {
                    reconstructedFlightByExpenseCache[expenseCacheKey] = reconciled
                }
                return reconciled
            }
            loadPersistedPnrSnapshot(context, rawPnr)?.let { snap ->
                val fromSnap = snapshotToFlightTicketResult(rawPnr, snap).let { built ->
                    if (expense.totalAmountCents > 0L && built.totalFarePaise != expense.totalAmountCents) {
                        built.copy(totalFarePaise = expense.totalAmountCents)
                    } else built
                }
                synchronized(reconstructedFlightByExpenseCache) {
                    reconstructedFlightByExpenseCache[expenseCacheKey] = fromSnap
                }
                return fromSnap
            }
        }

        // Honest PNR: never fabricate "F$hashBase" when PNR is absent (#6)
        val effectivePnr = if (rawPnr.length == 6) rawPnr.uppercase(Locale.US) else ""

        val rawFlightNo = parsed?.trainOrFlightNo?.trim()?.takeIf { it.isNotBlank() }
            ?: Regex("""\b(6E|AI|IX|QP|SG|UK|I5|9I|S5|EK|EY|QR|SQ|TG|MH|BA|LH)[\s\-]?\d{2,4}\b""", RegexOption.IGNORE_CASE)
                .find(expense.title)?.value?.uppercase(Locale.US)
            ?: ""
        val airlineCode = rawFlightNo.takeWhile { it.isLetterOrDigit() }.take(2).uppercase(Locale.US)
            .takeIf { it.length == 2 && it.any { ch -> ch.isLetter() } } ?: ""
        val flightNumber = if (rawFlightNo.isNotBlank()) rawFlightNo.uppercase(Locale.US) else airlineCode

        val airlineName = UniversalFlightTicketExtractor.resolveAirlineName(airlineCode)
            ?: parsed?.trainOrCarrierName?.trim()?.takeIf {
                it.isNotBlank() && !it.equals("Flight", ignoreCase = true) && !it.contains("(")
            }
            ?: expense.providerName.trim().takeIf { it.isNotBlank() }
            ?: when {
                expense.title.contains("Air India Express", ignoreCase = true) -> "Air India Express"
                expense.title.contains("Air India", ignoreCase = true) -> "Air India"
                expense.title.contains("IndiGo", ignoreCase = true) -> "IndiGo"
                expense.title.contains("Akasa", ignoreCase = true) -> "Akasa Air"
                expense.title.contains("SpiceJet", ignoreCase = true) -> "SpiceJet"
                expense.title.contains("Vistara", ignoreCase = true) -> "Vistara"
                else -> ""
            }

        val parenRouteMatch = Regex("""\(([A-Z]{3})\s*(?:->|[-–])\s*([A-Z]{3})\)""").find(expense.title.uppercase(Locale.US))
        val arrowRouteMatch = Regex("""\b([A-Z]{3})\s*->\s*([A-Z]{3})\b""").find(expense.title.uppercase(Locale.US))
        val originIata = parsed?.fromStation?.trim()?.uppercase(Locale.US)?.takeIf { it.length == 3 }
            ?: parenRouteMatch?.groupValues?.getOrNull(1)
            ?: arrowRouteMatch?.groupValues?.getOrNull(1)
            ?: ""
        val destIata = parsed?.toStation?.trim()?.uppercase(Locale.US)?.takeIf { it.length == 3 }
            ?: parenRouteMatch?.groupValues?.getOrNull(2)
            ?: arrowRouteMatch?.groupValues?.getOrNull(2)
            ?: ""

        val originInfo = UniversalFlightTicketExtractor.resolveAirportInfo(originIata)
        val destInfo = UniversalFlightTicketExtractor.resolveAirportInfo(destIata)
        val originCity = originInfo?.city ?: originIata
        val originAirportName = originInfo?.airportName ?: if (originCity.isNotBlank()) "$originCity Airport" else ""
        val destCity = destInfo?.city ?: destIata
        val destAirportName = destInfo?.airportName ?: if (destCity.isNotBlank()) "$destCity Airport" else ""

        val fallbackEpoch = expense.scheduledAtEpochMs?.takeIf { it > 0L }
        val travelDate = parsed?.departureDate?.trim()?.takeIf { it.isNotBlank() }
            ?: fallbackEpoch?.let { SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(it)) }
            ?: ""
        val departureTime = parsed?.departureTime?.trim()?.takeIf { it.isNotBlank() } ?: ""

        val rawSeatsField = parsed?.coachAndSeats?.trim().orEmpty()
        val cabinClass = rawSeatsField.substringBefore("·").trim()
            .takeIf { it.isNotBlank() && !it.contains("(") && !it.endsWith("Pax", ignoreCase = true) }
            ?: ""

        val expenseGroupMembers = groupMembers.filter { it.groupId == expense.groupId }.ifEmpty { groupMembers }
        val expenseSplits = allSplits.filter { it.expenseId == expense.expenseId && it.finalOwedCents > 0L }
        val splittingMemberIds = expenseSplits.map { it.memberId }.toSet()
        val orderedSplittingMembers = buildList {
            expenseGroupMembers.find { it.memberId == expense.payerId }?.let { add(it) }
            expenseGroupMembers.filter {
                (splittingMemberIds.isEmpty() || it.memberId in splittingMemberIds) && it.memberId != expense.payerId
            }.forEach { add(it) }
        }.ifEmpty { expenseGroupMembers }

        val seatRegex = Regex("""([^,·|]+?)\s*\(([0-9]{1,2}[A-K])\)""")
        val explicitSeatMatches = seatRegex.findAll(rawSeatsField).toList()
        val bareSeatTokens = Regex("""\b([0-9]{1,2}[A-K])\b""", RegexOption.IGNORE_CASE)
            .findAll(rawSeatsField)
            .map { it.groupValues[1].uppercase(Locale.US) }
            .toList()
        val passengers = if (explicitSeatMatches.isNotEmpty()) {
            explicitSeatMatches.map { match ->
                UniversalFlightTicketExtractor.ExtractedFlightPassenger(
                    fullName = match.groupValues[1].trim(),
                    seatNumber = match.groupValues[2].trim().uppercase(Locale.US),
                    eTicketOrPnr = effectivePnr
                )
            }
        } else if (orderedSplittingMembers.isNotEmpty()) {
            orderedSplittingMembers.mapIndexed { idx, member ->
                val assignedSeat = bareSeatTokens.getOrNull(idx) ?: ""
                UniversalFlightTicketExtractor.ExtractedFlightPassenger(
                    fullName = member.name,
                    seatNumber = assignedSeat,
                    eTicketOrPnr = effectivePnr
                )
            }
        } else {
            val fallbackSeat = bareSeatTokens.firstOrNull() ?: ""
            listOf(
                UniversalFlightTicketExtractor.ExtractedFlightPassenger(
                    fullName = "Passenger",
                    seatNumber = fallbackSeat,
                    eTicketOrPnr = effectivePnr
                )
            )
        }

        val reconstructed = UniversalFlightTicketExtractor.UniversalFlightTicketResult(
            pnr = effectivePnr,
            otaBookingId = if (effectivePnr.isNotBlank()) "PNR-$effectivePnr" else "",
            airlineCode = airlineCode,
            airlineName = airlineName,
            flightNumber = flightNumber,
            originIata = originIata,
            originCity = originCity,
            originAirportName = originAirportName,
            destinationIata = destIata,
            destinationCity = destCity,
            destinationAirportName = destAirportName,
            travelDate = travelDate,
            bookingDate = "",
            departureTime = departureTime,
            arrivalTime = "",
            durationText = "",
            cabinClass = cabinClass,
            fareType = "",
            cabinBaggage = "",
            checkInBaggage = "",
            passengers = passengers,
            matchedGroupMembers = orderedSplittingMembers.map { it.name },
            totalFarePaise = expense.totalAmountCents,
            discountSavedPaise = 0L,
            paymentMethod = "",
            extractionDurationMs = 1L
        )
        synchronized(reconstructedFlightByExpenseCache) {
            reconstructedFlightByExpenseCache[expenseCacheKey] = reconstructed
        }
        return reconstructed
    }

    /**
     * Converts a persisted [LivePnrStatusSnapshot] into a [UniversalFlightTicketExtractor.UniversalFlightTicketResult]
     * without fabricating IndiGo, BOM->GOI, 7 Kgs / 15 Kgs, or truncating paise (#5, M-02).
     */
    fun snapshotToFlightTicketResult(
        cleanPnr: String,
        persistedSnap: LivePnrStatusSnapshot
    ): UniversalFlightTicketExtractor.UniversalFlightTicketResult {
        val originIata = persistedSnap.fromStation.trim()
        val destIata = persistedSnap.toStation.trim()
        val originInfo = UniversalFlightTicketExtractor.resolveAirportInfo(originIata)
        val destInfo = UniversalFlightTicketExtractor.resolveAirportInfo(destIata)
        val resolvedTotalPaise = persistedSnap.totalPayablePaiseOverride
            ?: persistedSnap.baseFarePaise
        val resolvedDate = persistedSnap.departureDate.ifBlank {
            if (persistedSnap.departureTime.contains("•")) {
                persistedSnap.departureTime.substringBefore("•").trim()
            } else ""
        }
        val resolvedTime = if (persistedSnap.departureTime.contains("•")) {
            persistedSnap.departureTime.substringAfter("•").trim()
        } else {
            persistedSnap.departureTime.trim()
        }
        return UniversalFlightTicketExtractor.UniversalFlightTicketResult(
            pnr = cleanPnr,
            otaBookingId = if (cleanPnr.isNotBlank()) "PNR-$cleanPnr" else "",
            airlineCode = persistedSnap.trainNo.substringBefore(" ").trim(),
            airlineName = persistedSnap.trainName.trim(),
            flightNumber = persistedSnap.trainNo.trim(),
            originIata = originIata,
            originCity = originInfo?.city ?: persistedSnap.fromStationName.ifBlank { originIata },
            originAirportName = originInfo?.airportName ?: persistedSnap.fromStationName.ifBlank { originIata },
            destinationIata = destIata,
            destinationCity = destInfo?.city ?: persistedSnap.toStationName.ifBlank { destIata },
            destinationAirportName = destInfo?.airportName ?: persistedSnap.toStationName.ifBlank { destIata },
            travelDate = resolvedDate,
            bookingDate = "",
            departureTime = resolvedTime,
            arrivalTime = persistedSnap.arrivalTime,
            durationText = persistedSnap.durationText,
            cabinClass = persistedSnap.travelClass.substringBefore("•").trim(),
            fareType = persistedSnap.quotaText,
            cabinBaggage = "",
            checkInBaggage = "",
            passengers = persistedSnap.structuredPassengers.map { sp ->
                val rawSeat = if (sp.currentStatus.contains("/")) {
                    sp.currentStatus.substringAfter("/").trim()
                } else ""
                UniversalFlightTicketExtractor.ExtractedFlightPassenger(
                    fullName = sp.passengerNumber,
                    seatNumber = rawSeat,
                    eTicketOrPnr = cleanPnr
                )
            },
            matchedGroupMembers = emptyList(),
            totalFarePaise = resolvedTotalPaise,
            discountSavedPaise = 0L,
            paymentMethod = "",
            extractionDurationMs = 0L
        )
    }

    fun resolveOrReconstructFlightTicket(
        context: Context?,
        pnrOrExpenseKey: String,
        expenses: List<ExpenseEntity>,
        members: List<GroupMemberEntity>,
        splits: List<ExpenseSplitEntity>,
        preferredGroupId: String? = null
    ): UniversalFlightTicketExtractor.UniversalFlightTicketResult {
        val trimmedKey = pnrOrExpenseKey.trim()
        if (trimmedKey.startsWith("EXPENSE:", ignoreCase = true)) {
            val targetExpenseId = trimmedKey.substringAfter(":").trim()
            val matchedById = expenses.find { it.expenseId == targetExpenseId }
            if (matchedById != null) {
                return reconstructFlightTicketFromExpense(context, matchedById, members, splits)
            }
        }

        val cleanPnr = normalizePnrKey(trimmedKey)
        val candidateExpenses = if (!preferredGroupId.isNullOrBlank()) {
            val inGroup = expenses.filter { it.groupId == preferredGroupId }
            val others = expenses.filter { it.groupId != preferredGroupId }
            inGroup + others
        } else {
            expenses
        }

        val matchedFlightExpense = candidateExpenses.firstOrNull { exp ->
            val expPnr = exp.travelPnr.trim().ifBlank { extractTravelTicketFromTitle(exp.title)?.pnr?.trim().orEmpty() }
            (cleanPnr.isNotBlank() && (expPnr.equals(cleanPnr, ignoreCase = true) || exp.title.contains(cleanPnr, ignoreCase = true))) ||
                exp.expenseId == trimmedKey
        }

        if (cleanPnr.length == 6) {
            loadConfirmedFlightTicketResult(context, cleanPnr)?.let { vaultFlight ->
                return if (matchedFlightExpense != null && matchedFlightExpense.totalAmountCents > 0L && vaultFlight.totalFarePaise != matchedFlightExpense.totalAmountCents) {
                    vaultFlight.copy(totalFarePaise = matchedFlightExpense.totalAmountCents)
                } else {
                    vaultFlight
                }
            }
            loadPersistedPnrSnapshot(context, cleanPnr)?.let { snap ->
                val converted = snapshotToFlightTicketResult(cleanPnr, snap)
                return if (matchedFlightExpense != null && matchedFlightExpense.totalAmountCents > 0L && converted.totalFarePaise != matchedFlightExpense.totalAmountCents) {
                    converted.copy(totalFarePaise = matchedFlightExpense.totalAmountCents)
                } else {
                    converted
                }
            }
        }

        if (matchedFlightExpense != null) {
            return reconstructFlightTicketFromExpense(context, matchedFlightExpense, members, splits)
        }

        val fallbackPnr = if (cleanPnr.length == 6) cleanPnr else ""
        return UniversalFlightTicketExtractor.UniversalFlightTicketResult(
            pnr = fallbackPnr,
            otaBookingId = if (fallbackPnr.isNotBlank()) "PNR-$fallbackPnr" else "",
            airlineCode = "",
            airlineName = "",
            flightNumber = "",
            originIata = "",
            originCity = "",
            originAirportName = "",
            destinationIata = "",
            destinationCity = "",
            destinationAirportName = "",
            travelDate = "",
            bookingDate = "",
            departureTime = "",
            arrivalTime = "",
            durationText = "",
            cabinClass = "",
            fareType = "",
            cabinBaggage = "",
            checkInBaggage = "",
            passengers = members
                .filter { preferredGroupId == null || it.groupId == preferredGroupId }
                .ifEmpty { members }
                .map { m ->
                    UniversalFlightTicketExtractor.ExtractedFlightPassenger(
                        fullName = m.name,
                        seatNumber = "",
                        eTicketOrPnr = fallbackPnr
                    )
                },
            matchedGroupMembers = emptyList(),
            totalFarePaise = 0L,
            discountSavedPaise = 0L,
            paymentMethod = "",
            extractionDurationMs = 0L
        )
    }

    internal fun deserializeSnapshotJson(defaultPnr: String, rawJson: String): LivePnrStatusSnapshot? = runCatching {
        val obj = JSONObject(rawJson)
        val paxArr = obj.optJSONArray("passengerStatuses")
        val paxList = mutableListOf<String>()
        if (paxArr != null) {
            for (i in 0 until paxArr.length()) {
                paxList.add(paxArr.optString(i))
            }
        }
        val structArr = obj.optJSONArray("structuredPassengers")
        val structList = mutableListOf<LivePnrPassenger>()
        if (structArr != null) {
            for (i in 0 until structArr.length()) {
                val sObj = structArr.optJSONObject(i) ?: continue
                structList.add(
                    LivePnrPassenger(
                        passengerNumber = sObj.optString("passengerNumber", "P${i + 1}"),
                        initialStatus = sObj.optString("initialStatus", ""),
                        currentStatus = sObj.optString("currentStatus", ""),
                        statusLabel = sObj.optString("statusLabel", "")
                    )
                )
            }
        }
        val savedRupees = obj.optInt("totalFareRupees", 0)
        val baseFarePaise = if (obj.has("baseFarePaise")) {
            obj.optLong("baseFarePaise", savedRupees.toLong() * 100L)
        } else {
            savedRupees.toLong() * 100L
        }
        val payablePaiseRaw = if (obj.has("totalPayablePaise")) obj.optLong("totalPayablePaise", -1L) else -1L
        val hasExplicitIns = obj.optBoolean("hasExplicitInsurance", false)
        val explicitInsurance: Boolean? = if (hasExplicitIns) obj.optBoolean("includeInsurance", false) else null

        LivePnrStatusSnapshot(
            pnr = obj.optString("pnr", defaultPnr),
            trainNo = obj.optString("trainNo", ""),
            trainName = obj.optString("trainName", ""),
            fromStation = obj.optString("fromStation", ""),
            toStation = obj.optString("toStation", ""),
            departureTime = obj.optString("departureTime", ""),
            travelClass = obj.optString("travelClass", ""),
            totalFareRupees = (baseFarePaise / 100L).toInt(),
            passengerCount = obj.optInt("passengerCount", paxList.size.coerceAtLeast(1)),
            bookingStatusBadge = obj.optString("bookingStatusBadge", ""),
            chartPrepared = obj.optBoolean("chartPrepared", false),
            passengerStatuses = paxList,
            structuredPassengers = structList,
            fromStationName = obj.optString("fromStationName", ""),
            toStationName = obj.optString("toStationName", ""),
            arrivalTime = obj.optString("arrivalTime", ""),
            durationText = obj.optString("durationText", ""),
            quotaText = obj.optString("quotaText", ""),
            coachPositionHint = obj.optString("coachPositionHint", ""),
            liveTrainLocationRadar = obj.optString("liveTrainLocationRadar", ""),
            confirmationProbability = obj.optString("confirmationProbability", ""),
            sourceLabel = obj.optString("sourceLabel", "Live CRIS Cache (Encrypted Vault)"),
            isLiveVerified = obj.optBoolean("isLiveVerified", true),
            isManualEntry = obj.optBoolean("isManualEntry", false),
            baseFarePaiseOverride = baseFarePaise,
            totalPayablePaiseOverride = payablePaiseRaw.takeIf { it >= 0L },
            irctcPaymentMode = obj.optString("irctcPaymentMode", "UPI").ifBlank { "UPI" },
            includeInsurance = explicitInsurance,
            departureDate = obj.optString("departureDate", ""),
            arrivalDate = obj.optString("arrivalDate", ""),
            platformNumber = obj.optString("platformNumber", ""),
            fetchedAtEpochMs = obj.optLong("fetchedAtEpochMs", 0L),
            upstreamCacheEpochMs = obj.optLong("upstreamCacheEpochMs", 0L),
            isInvalidOrFlushedPnr = obj.optBoolean("isInvalidOrFlushedPnr", false)
        )
    }.getOrNull()

    fun loadPersistedPnrSnapshot(context: Context?, pnr: String): LivePnrStatusSnapshot? = runCatching {
        val cleanPnr = normalizePnrKey(pnr)
        if (cleanPnr.length != 10 && cleanPnr.length != 6) return null
        synchronized(lruSnapshotCache) {
            lruSnapshotCache[cleanPnr]?.let { return it }
        }
        if (missingSnapshotPnrs.contains(cleanPnr)) return null
        if (context == null) return null
        val prefs = EncryptedPrefsProvider.getPnrVaultPrefs(context)
        ensureVaultSchemaMigrated(prefs)
        val rawJson = prefs.getString("snapshot_json_$cleanPnr", null)
        if (rawJson == null) {
            missingSnapshotPnrs.add(cleanPnr)
            return null
        }
        val savedNetSyncMs = prefs.getLong("last_live_net_sync_$cleanPnr", 0L)
        if (savedNetSyncMs > 0L) {
            synchronized(lastLiveNetSyncEpochMsCache) {
                lastLiveNetSyncEpochMsCache[cleanPnr] = savedNetSyncMs
            }
        }
        val parsed = deserializeSnapshotJson(cleanPnr, rawJson) ?: return null
        synchronized(lruSnapshotCache) {
            lruSnapshotCache[cleanPnr] = parsed
        }
        parsed
    }.getOrNull()

    internal fun serializeSnapshotJson(cleanPnr: String, snapshot: LivePnrStatusSnapshot, effectiveFetchedMs: Long): String {
        val structJsonArr = JSONArray()
        snapshot.structuredPassengers.forEach { sp ->
            structJsonArr.put(
                JSONObject().apply {
                    put("passengerNumber", sp.passengerNumber)
                    put("initialStatus", sp.initialStatus)
                    put("currentStatus", sp.currentStatus)
                    put("statusLabel", sp.statusLabel)
                }
            )
        }
        val pureBasePaise = snapshot.baseFarePaise
        val obj = JSONObject().apply {
            put("schemaVersion", VAULT_SCHEMA_VERSION)
            put("pnr", cleanPnr)
            put("trainNo", snapshot.trainNo)
            put("trainName", snapshot.trainName)
            put("fromStation", snapshot.fromStation)
            put("toStation", snapshot.toStation)
            put("departureTime", snapshot.departureTime)
            put("departureDate", snapshot.departureDate)
            put("arrivalDate", snapshot.arrivalDate)
            put("platformNumber", snapshot.platformNumber)
            put("travelClass", snapshot.travelClass)
            put("baseFarePaise", pureBasePaise)
            put("totalFareRupees", (pureBasePaise / 100L).toInt())
            put("totalPayablePaise", snapshot.totalPayablePaiseOverride ?: -1L)
            put("irctcPaymentMode", snapshot.irctcPaymentMode)
            put("hasExplicitInsurance", snapshot.includeInsurance != null)
            put("includeInsurance", snapshot.includeInsurance ?: false)
            put("passengerCount", snapshot.effectivePassengerCount)
            put("bookingStatusBadge", snapshot.bookingStatusBadge)
            put("chartPrepared", snapshot.chartPrepared)
            put("passengerStatuses", JSONArray(snapshot.passengerStatuses))
            put("structuredPassengers", structJsonArr)
            put("fromStationName", snapshot.fromStationName)
            put("toStationName", snapshot.toStationName)
            put("arrivalTime", snapshot.arrivalTime)
            put("durationText", snapshot.durationText)
            put("quotaText", snapshot.quotaText)
            put("coachPositionHint", snapshot.coachPositionHint)
            put("liveTrainLocationRadar", snapshot.liveTrainLocationRadar)
            put("confirmationProbability", snapshot.confirmationProbability)
            put("sourceLabel", snapshot.sourceLabel)
            put("isLiveVerified", snapshot.isLiveVerified)
            put("isManualEntry", snapshot.isManualEntry)
            put("fetchedAtEpochMs", effectiveFetchedMs)
            put("upstreamCacheEpochMs", snapshot.upstreamCacheEpochMs)
        }
        return obj.toString()
    }

    /**
     * Persists a verified [LivePnrStatusSnapshot] into L1 memory and L2 EncryptedSharedPreferences.
     * Note (`R-03`): Only stamps `last_live_net_sync_$cleanPnr` when [markAsLiveNetworkSync] is `true`,
     * so UI fee-mode adjustments never spoof live network refresh timestamps.
     */
    fun savePersistedPnrSnapshot(
        context: Context?,
        snapshot: LivePnrStatusSnapshot,
        markAsLiveNetworkSync: Boolean = false
    ) {
        runCatching {
            val cleanPnr = normalizePnrKey(snapshot.pnr)
            if ((cleanPnr.length != 10 && cleanPnr.length != 6) || !snapshot.isLiveVerified || snapshot.isInvalidOrFlushedPnr) return
            missingSnapshotPnrs.remove(cleanPnr)
            val now = clockMsProvider()
            val effectiveFetchedMs = if (snapshot.fetchedAtEpochMs > 0L) snapshot.fetchedAtEpochMs else now
            val stamped = snapshot.copy(
                pnr = cleanPnr,
                baseFarePaiseOverride = snapshot.baseFarePaise,
                totalFareRupees = (snapshot.baseFarePaise / 100L).toInt(),
                fetchedAtEpochMs = effectiveFetchedMs,
                cooldownRemainingSeconds = 0
            )
            synchronized(lruSnapshotCache) {
                lruSnapshotCache[cleanPnr] = stamped
            }
            if (markAsLiveNetworkSync) {
                synchronized(lastLiveNetSyncEpochMsCache) {
                    lastLiveNetSyncEpochMsCache[cleanPnr] = now
                }
            }
            if (context == null) return
            val serialized = serializeSnapshotJson(cleanPnr, stamped, effectiveFetchedMs)
            synchronized(vaultDiskLock) {
                val prefs = EncryptedPrefsProvider.getPnrVaultPrefs(context)
                ensureVaultSchemaMigrated(prefs)
                val editor = prefs.edit()
                    .putString("snapshot_json_$cleanPnr", serialized)
                    .remove("last_failed_sync_$cleanPnr")
                if (markAsLiveNetworkSync) {
                    editor.putLong("last_live_net_sync_$cleanPnr", now)
                        .putLong("last_sync_$cleanPnr", now)
                }
                recordAndEvictLruIfNeededLocked(prefs, editor, cleanPnr)
                editor.apply()
            }
        }
    }

    fun getRemainingManualRefreshCooldownSeconds(
        context: Context?,
        pnr: String,
        nowMs: Long = clockMsProvider()
    ): Int {
        val cleanPnr = normalizePnrKey(pnr)
        if (cleanPnr.length != 10) return 0
        val prefs = context?.let {
            EncryptedPrefsProvider.getPnrVaultPrefs(it).also { p -> ensureVaultSchemaMigrated(p) }
        }
        val memSyncMs = synchronized(lastLiveNetSyncEpochMsCache) { lastLiveNetSyncEpochMsCache[cleanPnr] ?: 0L }
        val lastNetSyncMs = maxOf(
            memSyncMs,
            prefs?.getLong("last_live_net_sync_$cleanPnr", 0L) ?: 0L
        )
        if (lastNetSyncMs <= 0L) return 0
        val elapsed = nowMs - lastNetSyncMs
        if (elapsed !in 0 until MANUAL_REFRESH_COOLDOWN_MS) return 0
        return (((MANUAL_REFRESH_COOLDOWN_MS - elapsed) + 999L) / 1000L).toInt().coerceAtLeast(1)
    }

    fun shouldSkipAutoPnrNetworkPoll(
        context: Context?,
        pnr: String,
        ticket: ParsedTravelTicket,
        nowMs: Long = clockMsProvider()
    ): Boolean = runCatching {
        val cleanPnr = normalizePnrKey(pnr)
        if (cleanPnr.length == 6) return true
        if (cleanPnr.length != 10) return true

        if (isTicketAllConfirmed(ticket)) return true

        val cached = loadPersistedPnrSnapshot(context, cleanPnr)
        if (isSnapshotAllConfirmed(cached)) return true
        if (isTicketJourneyInPast(ticket, cached, nowMs)) return true
        if (isWithinCrisNightlyMaintenance(nowMs)) return true

        val prefs = context?.let {
            EncryptedPrefsProvider.getPnrVaultPrefs(it).also { p -> ensureVaultSchemaMigrated(p) }
        }
        val memSyncMs = synchronized(lastLiveNetSyncEpochMsCache) { lastLiveNetSyncEpochMsCache[cleanPnr] ?: 0L }
        val lastSyncMs = maxOf(
            memSyncMs,
            prefs?.getLong("last_live_net_sync_$cleanPnr", 0L) ?: 0L,
            prefs?.getLong("last_sync_$cleanPnr", 0L) ?: 0L
        )
        if (lastSyncMs <= 0L) return context == null

        val isTravelDay = isTicketOnTravelDayIst(ticket, cached, nowMs)
        val requiredCooldown = if (isTravelDay) TRAVEL_DAY_AUTO_SYNC_COOLDOWN_MS else AUTO_SYNC_COOLDOWN_MS
        (nowMs - lastSyncMs) < requiredCooldown
    }.getOrDefault(true)

    fun recordPnrSyncTimestamp(context: Context, pnr: String) {
        runCatching {
            val cleanPnr = normalizePnrKey(pnr)
            if (cleanPnr.length != 10 && cleanPnr.length != 6) return
            val now = clockMsProvider()
            synchronized(lastLiveNetSyncEpochMsCache) {
                lastLiveNetSyncEpochMsCache[cleanPnr] = now
            }
            synchronized(vaultDiskLock) {
                val prefs = EncryptedPrefsProvider.getPnrVaultPrefs(context)
                ensureVaultSchemaMigrated(prefs)
                prefs.edit()
                    .putLong("last_live_net_sync_$cleanPnr", now)
                    .putLong("last_sync_$cleanPnr", now)
                    .apply()
            }
        }
    }

    suspend fun fetchLivePnrStatus(
        pnr: String,
        fallbackTicket: ParsedTravelTicket = ParsedTravelTicket(),
        forceManualRefresh: Boolean = false,
        context: Context? = null
    ): LivePnrStatusSnapshot {
        val cleanPnr = normalizePnrKey(pnr)
        if (cleanPnr.length != 10 && cleanPnr.length != 6) {
            return buildUnverifiedManualFallbackSnapshot(
                cleanPnr = cleanPnr,
                fallbackTicket = fallbackTicket,
                reasonLabel = "Enter a valid 10-digit Train PNR or 6-char Flight PNR"
            )
        }

        val nowMs = clockMsProvider()
        val prefs = context?.let {
            EncryptedPrefsProvider.getPnrVaultPrefs(it).also { p -> ensureVaultSchemaMigrated(p) }
        }

        // STAGE 1: L1 In-Memory Cache & L2 EncryptedSharedPreferences Vault Lookup
        val cachedSnapshot = loadPersistedPnrSnapshot(context, cleanPnr)

        // Permanent Offline Lock ONLY when all passengers are CNF AND (chartPrepared || concrete coach+berth || 6-char Flight)
        if (cachedSnapshot != null && isSnapshotAllConfirmed(cachedSnapshot)) {
            return cachedSnapshot.copy(
                sourceLabel = "Confirmed Offline Vault (Permanent CNF · 0 Internet Used)",
                cooldownRemainingSeconds = 0
            )
        }

        if (isTicketAllConfirmed(fallbackTicket.copy(pnr = cleanPnr))) {
            val promoted = buildConfirmedSnapshotFromTicket(cleanPnr, fallbackTicket)
            savePersistedPnrSnapshot(context, promoted)
            return promoted
        }

        if (!forceManualRefresh && isTicketJourneyInPast(fallbackTicket, cachedSnapshot, nowMs)) {
            if (cachedSnapshot != null) {
                return cachedSnapshot.copy(
                    sourceLabel = "Trip Completed · Saved in Offline Vault (0 Internet Used)",
                    cooldownRemainingSeconds = 0
                )
            }
            if (fallbackTicket.hasTicketMetadata) {
                val pastSnapshot = buildConfirmedSnapshotFromTicket(cleanPnr, fallbackTicket).copy(
                    sourceLabel = "Trip Completed · Saved in Offline Vault (0 Internet Used)"
                )
                savePersistedPnrSnapshot(context, pastSnapshot)
                return pastSnapshot
            }
        }

        if (cleanPnr.length == 6) {
            return cachedSnapshot ?: buildUnverifiedManualFallbackSnapshot(
                cleanPnr = cleanPnr,
                fallbackTicket = fallbackTicket,
                reasonLabel = "Import Flight Ticket PDF once to lock PNR $cleanPnr permanently offline"
            )
        }

        // Check 30-minute Negative Cache for Invalid / Flushed PNR (`errorCode == 201`)
        val negativeEntry = getNegativeInvalidPnrEntry(prefs, cleanPnr, nowMs)
        if (negativeEntry != null) {
            return buildInvalidOrFlushedPnrSnapshot(cleanPnr, negativeEntry)
        }

        // Check Per-PNR Manual Refresh Cooldown (`60s` anti-ban shield) & Auto-Sync TTL (`6h` / `30m`)
        val memSyncMs = synchronized(lastLiveNetSyncEpochMsCache) { lastLiveNetSyncEpochMsCache[cleanPnr] ?: 0L }
        val lastLiveNetSyncMs = maxOf(
            memSyncMs,
            prefs?.getLong("last_live_net_sync_$cleanPnr", 0L) ?: 0L
        )
        if (cachedSnapshot != null && cachedSnapshot.isLiveVerified && lastLiveNetSyncMs > 0L) {
            val elapsedMs = nowMs - lastLiveNetSyncMs
            val isTravelDay = isTicketOnTravelDayIst(fallbackTicket, cachedSnapshot, nowMs)
            val minWaitMs = when {
                forceManualRefresh -> MANUAL_REFRESH_COOLDOWN_MS
                isTravelDay -> TRAVEL_DAY_AUTO_SYNC_COOLDOWN_MS
                else -> AUTO_SYNC_COOLDOWN_MS
            }
            if (elapsedMs in 0 until minWaitMs) {
                val remSec = if (forceManualRefresh) {
                    (((MANUAL_REFRESH_COOLDOWN_MS - elapsedMs) + 999L) / 1000L).toInt().coerceAtLeast(1)
                } else 0
                val minsAgo = (elapsedMs / 60_000L).coerceAtLeast(0L)
                val label = if (forceManualRefresh) {
                    "Updated just now · Next live check in ${remSec}s"
                } else {
                    "Live CRIS Cache (${if (minsAgo == 0L) "<1m" else "${minsAgo}m"} ago · Encrypted Vault)"
                }
                return cachedSnapshot.copy(
                    sourceLabel = label,
                    cooldownRemainingSeconds = remSec
                )
            }
        }

        // Check Exponential Failure Backoff (`15s -> 30s -> 60s -> 120s`)
        val backoffUntilMs = getFailureBackoffUntilMs(prefs, cleanPnr)
        if (nowMs < backoffUntilMs) {
            val remSec = (((backoffUntilMs - nowMs) + 999L) / 1000L).toInt().coerceAtLeast(1)
            return cachedSnapshot?.copy(
                sourceLabel = "Server retry cooldown (${remSec}s) · Showing last known status",
                cooldownRemainingSeconds = remSec
            ) ?: buildUnverifiedManualFallbackSnapshot(
                cleanPnr = cleanPnr,
                fallbackTicket = fallbackTicket,
                reasonLabel = "Server retry cooldown (${remSec}s) — Enter ticket details manually or wait",
                cooldownRemainingSeconds = remSec
            )
        }

        // Check CRIS Nightly Maintenance Window (`23:30–00:30 IST`) — 0 network calls
        if (isWithinCrisNightlyMaintenance(nowMs)) {
            return cachedSnapshot?.copy(
                sourceLabel = "CRIS nightly maintenance (23:30–00:30 IST) · Showing cached status",
                cooldownRemainingSeconds = 0
            ) ?: buildUnverifiedManualFallbackSnapshot(
                cleanPnr = cleanPnr,
                fallbackTicket = fallbackTicket,
                reasonLabel = "CRIS nightly maintenance (23:30–00:30 IST) — Enter ticket details or paste IRCTC SMS"
            )
        }

        // STAGE 3: Single-Flight Request Coalescing on SupervisorJob() + Dispatchers.IO (`C-05` persistence-safe)
        var joinedExisting = false
        val deferred = coalesceMutex.withLock {
            val active = inFlightPnrDeferreds[cleanPnr]?.takeIf { it.isActive }
            if (active != null) {
                joinedExisting = true
                active
            } else {
                repoScope.async {
                    try {
                        executeTokenGuardedNetworkFetch(
                            cleanPnr = cleanPnr,
                            fallbackTicket = fallbackTicket,
                            cachedSnapshot = cachedSnapshot,
                            forceManualRefresh = forceManualRefresh,
                            context = context
                        )
                    } finally {
                        coalesceMutex.withLock {
                            inFlightPnrDeferreds.remove(cleanPnr)
                        }
                    }
                }.also { inFlightPnrDeferreds[cleanPnr] = it }
            }
        }
        val result = deferred.await()
        // C-05 Fix: If a `context != null` caller coalesced onto an in-flight call started with `context == null`,
        // ensure the verified snapshot is persisted to disk!
        if (joinedExisting && context != null && result.isLiveVerified && !result.isInvalidOrFlushedPnr) {
            savePersistedPnrSnapshot(context, result, markAsLiveNetworkSync = true)
        }
        return result
    }

    private suspend fun tryAcquireGlobalRateLimitToken(
        prefs: android.content.SharedPreferences?,
        nowMs: Long
    ): Int {
        return rateLimitMutex.withLock {
            val merged = synchronized(globalFetchEpochsMemory) {
                if (globalFetchEpochsMemory.isEmpty() && prefs != null) {
                    val persistedEpochs = prefs.getString("global_pnr_fetch_epochs_csv", "").orEmpty()
                        .split(",")
                        .mapNotNull { it.trim().toLongOrNull() }
                    globalFetchEpochsMemory.addAll(persistedEpochs)
                }
                globalFetchEpochsMemory
                    .sorted()
                    .filter { nowMs - it in 0 until SUSTAINED_WINDOW_MS }
                    .toMutableList()
            }

            val recent1m = merged.filter { nowMs - it in 0 until BURST_WINDOW_MS }
            if (recent1m.size >= BURST_MAX_CALLS) {
                val oldest1m = recent1m.first()
                val waitMs = (BURST_WINDOW_MS - (nowMs - oldest1m)).coerceAtLeast(1000L)
                return@withLock (((waitMs + 999L) / 1000L).toInt()).coerceAtLeast(1)
            }
            if (merged.size >= SUSTAINED_MAX_CALLS) {
                val oldest10m = merged.first()
                val waitMs = (SUSTAINED_WINDOW_MS - (nowMs - oldest10m)).coerceAtLeast(1000L)
                return@withLock (((waitMs + 999L) / 1000L).toInt()).coerceAtLeast(1)
            }

            merged.add(nowMs)
            synchronized(globalFetchEpochsMemory) {
                globalFetchEpochsMemory.clear()
                globalFetchEpochsMemory.addAll(merged)
            }
            if (prefs != null) {
                synchronized(vaultDiskLock) {
                    prefs.edit().putString("global_pnr_fetch_epochs_csv", merged.joinToString(",")).apply()
                }
            }
            0
        }
    }

    private fun getNegativeInvalidPnrEntry(
        prefs: android.content.SharedPreferences?,
        cleanPnr: String,
        nowMs: Long
    ): String? {
        synchronized(invalidPnrUntilCache) {
            invalidPnrUntilCache[cleanPnr]?.let { (untilMs, msg) ->
                if (nowMs < untilMs) return msg else invalidPnrUntilCache.remove(cleanPnr)
            }
        }
        if (prefs != null) {
            val untilMs = prefs.getLong("invalid_pnr_until_$cleanPnr", 0L)
            if (nowMs < untilMs) {
                val msg = prefs.getString("invalid_pnr_msg_$cleanPnr", null)
                    ?: "Flushed PNR / PNR not yet generated"
                synchronized(invalidPnrUntilCache) {
                    invalidPnrUntilCache[cleanPnr] = untilMs to msg
                }
                return msg
            }
        }
        return null
    }

    private fun recordNegativeInvalidPnr(
        prefs: android.content.SharedPreferences?,
        cleanPnr: String,
        errorMessage: String,
        nowMs: Long
    ) {
        val untilMs = nowMs + INVALID_PNR_NEGATIVE_TTL_MS
        synchronized(invalidPnrUntilCache) {
            invalidPnrUntilCache[cleanPnr] = untilMs to errorMessage
        }
        if (prefs != null) {
            synchronized(vaultDiskLock) {
                prefs.edit()
                    .putLong("invalid_pnr_until_$cleanPnr", untilMs)
                    .putString("invalid_pnr_msg_$cleanPnr", errorMessage)
                    .apply()
            }
        }
    }

    private fun getFailureBackoffUntilMs(
        prefs: android.content.SharedPreferences?,
        cleanPnr: String
    ): Long {
        val memUntil = synchronized(failureStateByPnrCache) { failureStateByPnrCache[cleanPnr]?.second ?: 0L }
        val diskUntil = prefs?.getLong("backoff_until_$cleanPnr", 0L) ?: 0L
        return maxOf(memUntil, diskUntil)
    }

    private fun recordNetworkFailureBackoff(
        prefs: android.content.SharedPreferences?,
        cleanPnr: String,
        nowMs: Long
    ) {
        val prevCount = synchronized(failureStateByPnrCache) {
            failureStateByPnrCache[cleanPnr]?.first ?: (prefs?.getInt("fail_count_$cleanPnr", 0) ?: 0)
        }
        val nextCount = (prevCount + 1).coerceAtMost(8)
        val backoffMs = when (nextCount) {
            1 -> 15_000L
            2 -> 30_000L
            3 -> 60_000L
            else -> 120_000L
        }
        val untilMs = nowMs + backoffMs
        synchronized(failureStateByPnrCache) {
            failureStateByPnrCache[cleanPnr] = nextCount to untilMs
        }
        if (prefs != null) {
            synchronized(vaultDiskLock) {
                prefs.edit()
                    .putInt("fail_count_$cleanPnr", nextCount)
                    .putLong("backoff_until_$cleanPnr", untilMs)
                    .apply()
            }
        }
    }

    private fun clearNetworkFailureBackoff(
        prefs: android.content.SharedPreferences?,
        cleanPnr: String
    ) {
        synchronized(failureStateByPnrCache) {
            failureStateByPnrCache.remove(cleanPnr)
        }
        if (prefs != null) {
            synchronized(vaultDiskLock) {
                prefs.edit()
                    .remove("fail_count_$cleanPnr")
                    .remove("backoff_until_$cleanPnr")
                    .apply()
            }
        }
    }

    private fun isConfirmTktCircuitOpen(
        prefs: android.content.SharedPreferences?,
        nowMs: Long
    ): Boolean {
        val openUntil = maxOf(
            confirmTktCircuitOpenUntilMsMemory,
            prefs?.getLong("ct_circuit_open_until_ms", 0L) ?: 0L
        )
        return nowMs < openUntil
    }

    private fun tripConfirmTktCircuitBreaker(
        prefs: android.content.SharedPreferences?,
        nowMs: Long
    ) {
        val jitterMs = Random.nextLong(0L, CIRCUIT_BREAKER_MAX_JITTER_MS + 1L)
        val openUntil = nowMs + CIRCUIT_BREAKER_BASE_MS + jitterMs
        confirmTktCircuitOpenUntilMsMemory = openUntil
        if (prefs != null) {
            synchronized(vaultDiskLock) {
                prefs.edit().putLong("ct_circuit_open_until_ms", openUntil).apply()
            }
        }
    }

    private fun shouldUseLiveUpstreamFlag(
        prefs: android.content.SharedPreferences?,
        cleanPnr: String,
        fallbackTicket: ParsedTravelTicket,
        cachedSnapshot: LivePnrStatusSnapshot?,
        forceManualRefresh: Boolean,
        upstreamCacheEpochMs: Long,
        nowMs: Long
    ): Boolean {
        if (!forceManualRefresh) return false
        val memLastForce = synchronized(lastForceLiveUpstreamMsCache) { lastForceLiveUpstreamMsCache[cleanPnr] ?: 0L }
        val lastForceMs = maxOf(
            memLastForce,
            prefs?.getLong("last_force_live_ms_$cleanPnr", 0L) ?: 0L
        )
        if (lastForceMs > 0L && (nowMs - lastForceMs) < LIVE_UPSTREAM_FORCE_COOLDOWN_MS) {
            return false
        }
        if (upstreamCacheEpochMs <= 0L) return false
        val isTravelDay = isTicketOnTravelDayIst(fallbackTicket, cachedSnapshot, nowMs)
        val staleThresholdMs = if (isTravelDay) UPSTREAM_STALE_TRAVEL_DAY_MS else UPSTREAM_STALE_DEFAULT_MS
        return (nowMs - upstreamCacheEpochMs) > staleThresholdMs
    }

    private fun recordLiveUpstreamForced(
        prefs: android.content.SharedPreferences?,
        cleanPnr: String,
        nowMs: Long
    ) {
        synchronized(lastForceLiveUpstreamMsCache) {
            lastForceLiveUpstreamMsCache[cleanPnr] = nowMs
        }
        if (prefs != null) {
            synchronized(vaultDiskLock) {
                prefs.edit().putLong("last_force_live_ms_$cleanPnr", nowMs).apply()
            }
        }
    }

    private suspend fun executeTokenGuardedNetworkFetch(
        cleanPnr: String,
        fallbackTicket: ParsedTravelTicket,
        cachedSnapshot: LivePnrStatusSnapshot?,
        forceManualRefresh: Boolean,
        context: Context?
    ): LivePnrStatusSnapshot {
        val nowMs = clockMsProvider()
        val prefs = context?.let {
            EncryptedPrefsProvider.getPnrVaultPrefs(it).also { p -> ensureVaultSchemaMigrated(p) }
        }

        // STAGE 4: Dual Sliding-Window Global Token Bucket (3 / 1m burst + 8 / 10m sustained)
        val waitForTokenSec = tryAcquireGlobalRateLimitToken(prefs, nowMs)
        if (waitForTokenSec > 0) {
            return cachedSnapshot?.copy(
                sourceLabel = "Rate limit protected · Next live check in ${waitForTokenSec}s",
                cooldownRemainingSeconds = waitForTokenSec
            ) ?: buildUnverifiedManualFallbackSnapshot(
                cleanPnr = cleanPnr,
                fallbackTicket = fallbackTicket,
                reasonLabel = "Rate limit protected (wait ${waitForTokenSec}s) — Enter fare manually or paste IRCTC SMS",
                cooldownRemainingSeconds = waitForTokenSec
            )
        }

        val transport = httpTransportOverride ?: defaultHttpTransport
        val deviceId = getOrCreateDeviceId(prefs)

        // TIER 1: ConfirmTkt MWeb JSON REST API (`cttrainsapi.confirmtkt.com`)
        if (!isConfirmTktCircuitOpen(prefs, nowMs)) {
            val initialUseLive = shouldUseLiveUpstreamFlag(
                prefs = prefs,
                cleanPnr = cleanPnr,
                fallbackTicket = fallbackTicket,
                cachedSnapshot = cachedSnapshot,
                forceManualRefresh = forceManualRefresh,
                upstreamCacheEpochMs = cachedSnapshot?.upstreamCacheEpochMs ?: 0L,
                nowMs = nowMs
            )
            if (initialUseLive) {
                recordLiveUpstreamForced(prefs, cleanPnr, nowMs)
            }

            val tier1Outcome = invokeConfirmTktApi(
                transport = transport,
                cleanPnr = cleanPnr,
                deviceId = deviceId,
                livePnr = initialUseLive,
                nowMs = nowMs
            )

            when (tier1Outcome) {
                is ConfirmTktParseOutcome.InvalidOrFlushed -> {
                    clearNetworkFailureBackoff(prefs, cleanPnr)
                    recordNegativeInvalidPnr(prefs, cleanPnr, tier1Outcome.errorMessage, nowMs)
                    return buildInvalidOrFlushedPnrSnapshot(cleanPnr, tier1Outcome.errorMessage)
                }
                is ConfirmTktParseOutcome.RateLimitedOrBlocked -> {
                    tripConfirmTktCircuitBreaker(prefs, nowMs)
                }
                is ConfirmTktParseOutcome.Success -> {
                    var finalSnap = tier1Outcome.snapshot
                    // Smart Two-Speed Escalation: if initial call used `livePnr=false` on an explicit manual refresh
                    // of an already-cached PNR AND the returned `cacheTime` is still stale, escalate ONCE (`5m` capped).
                    if (!initialUseLive && cachedSnapshot != null && shouldUseLiveUpstreamFlag(
                            prefs = prefs,
                            cleanPnr = cleanPnr,
                            fallbackTicket = fallbackTicket,
                            cachedSnapshot = finalSnap,
                            forceManualRefresh = forceManualRefresh,
                            upstreamCacheEpochMs = finalSnap.upstreamCacheEpochMs,
                            nowMs = nowMs
                        )
                    ) {
                        if (tryAcquireGlobalRateLimitToken(prefs, nowMs) == 0) {
                            recordLiveUpstreamForced(prefs, cleanPnr, nowMs)
                            val escalated = invokeConfirmTktApi(
                                transport = transport,
                                cleanPnr = cleanPnr,
                                deviceId = deviceId,
                                livePnr = true,
                                nowMs = nowMs
                            )
                            if (escalated is ConfirmTktParseOutcome.Success) {
                                finalSnap = escalated.snapshot
                            } else if (escalated is ConfirmTktParseOutcome.RateLimitedOrBlocked) {
                                tripConfirmTktCircuitBreaker(prefs, nowMs)
                            }
                        }
                    }
                    val preservedSnap = preserveUserFeePreferencesIfSameBase(finalSnap, cachedSnapshot)
                    clearNetworkFailureBackoff(prefs, cleanPnr)
                    savePersistedPnrSnapshot(context, preservedSnap, markAsLiveNetworkSync = true)
                    return preservedSnap
                }
                is ConfirmTktParseOutcome.Unreachable -> {
                    // Fall through to Tier 2
                }
            }
        }

        // TIER 2: RailMitra SSR HTML (`https://www.railmitra.com/pnr-status?pnr=$cleanPnr`)
        val tier2Snap = invokeRailMitraSsr(transport, cleanPnr, cachedSnapshot, nowMs)
        if (tier2Snap != null) {
            val preserved = preserveUserFeePreferencesIfSameBase(tier2Snap, cachedSnapshot)
            clearNetworkFailureBackoff(prefs, cleanPnr)
            savePersistedPnrSnapshot(context, preserved, markAsLiveNetworkSync = true)
            return preserved
        }

        // TIER 3: RailYatri Rails SSR HTML (`https://www.railyatri.in/pnr-status/$cleanPnr`)
        val tier3Snap = invokeRailYatriSsr(transport, cleanPnr, cachedSnapshot, nowMs)
        if (tier3Snap != null) {
            val preserved = preserveUserFeePreferencesIfSameBase(tier3Snap, cachedSnapshot)
            clearNetworkFailureBackoff(prefs, cleanPnr)
            savePersistedPnrSnapshot(context, preserved, markAsLiveNetworkSync = true)
            return preserved
        }

        // All 3 live tiers unreachable -> apply exponential failure backoff (`R-02`: do NOT stamp positive `last_live_net_sync`!)
        recordNetworkFailureBackoff(prefs, cleanPnr, nowMs)

        if (cachedSnapshot != null && cachedSnapshot.fromStation.isNotBlank()) {
            return cachedSnapshot.copy(
                sourceLabel = "Offline / No Signal · Showing Last Known Status (${cachedSnapshot.bookingStatusBadge})"
            )
        }

        return buildUnverifiedManualFallbackSnapshot(
            cleanPnr = cleanPnr,
            fallbackTicket = fallbackTicket,
            reasonLabel = "Live CRIS servers unreachable — Enter ticket fare & route manually or paste IRCTC SMS"
        )
    }

    private fun preserveUserFeePreferencesIfSameBase(
        freshSnapshot: LivePnrStatusSnapshot,
        cachedSnapshot: LivePnrStatusSnapshot?
    ): LivePnrStatusSnapshot {
        if (cachedSnapshot == null) return freshSnapshot
        val effectiveBase = if (freshSnapshot.baseFarePaise > 0L) {
            freshSnapshot.baseFarePaise
        } else {
            cachedSnapshot.baseFarePaise
        }
        val mode = cachedSnapshot.irctcPaymentMode.ifBlank { freshSnapshot.irctcPaymentMode }
        val explicitIns = cachedSnapshot.includeInsurance
        val recomputedPayable = if (cachedSnapshot.totalPayablePaiseOverride != null && effectiveBase > 0L) {
            freshSnapshot.copy(
                baseFarePaiseOverride = effectiveBase,
                irctcPaymentMode = mode,
                includeInsurance = explicitIns
            ).computeCustomTotalPaise(mode, explicitIns ?: freshSnapshot.defaultIncludeInsurance)
        } else null
        return freshSnapshot.copy(
            baseFarePaiseOverride = effectiveBase.takeIf { it > 0L },
            totalFareRupees = (effectiveBase / 100L).toInt(),
            irctcPaymentMode = mode,
            includeInsurance = explicitIns,
            totalPayablePaiseOverride = recomputedPayable
        )
    }

    internal sealed class ConfirmTktParseOutcome {
        data class Success(val snapshot: LivePnrStatusSnapshot) : ConfirmTktParseOutcome()
        data class InvalidOrFlushed(val errorCode: Int, val errorMessage: String) : ConfirmTktParseOutcome()
        data class RateLimitedOrBlocked(val httpStatus: Int) : ConfirmTktParseOutcome()
        data object Unreachable : ConfirmTktParseOutcome()
    }

    private fun invokeConfirmTktApi(
        transport: PnrHttpTransport,
        cleanPnr: String,
        deviceId: String,
        livePnr: Boolean,
        nowMs: Long
    ): ConfirmTktParseOutcome {
        return runCatching {
            val endpoint = "https://cttrainsapi.confirmtkt.com/api/v2/ctpro/mweb/$cleanPnr" +
                "?querysource=ct-web&locale=en&getHighChanceText=false&livePnr=$livePnr"
            val req = PnrHttpRequest(
                url = endpoint,
                method = "POST",
                headers = mapOf(
                    "Accept" to "application/json, text/plain, */*",
                    "Content-Type" to "application/json",
                    "clientid" to "ct-web",
                    "apikey" to "ct-web!2$",
                    "deviceid" to deviceId,
                    "Origin" to "https://www.confirmtkt.com",
                    "Referer" to "https://www.confirmtkt.com/",
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                ),
                body = """{"proPlanName":"CP1","emailId":"","tempToken":""}"""
            )
            val resp = transport.execute(req)
            if (resp.statusCode == 429 || resp.statusCode == 403) {
                return ConfirmTktParseOutcome.RateLimitedOrBlocked(resp.statusCode)
            }
            if (resp.statusCode !in 200..299 || resp.body.isBlank()) {
                return ConfirmTktParseOutcome.Unreachable
            }
            parseConfirmTktApiJson(cleanPnr, resp.body, nowMs)
        }.getOrElse { ConfirmTktParseOutcome.Unreachable }
    }

    internal fun parseConfirmTktApiJson(
        cleanPnr: String,
        rawJson: String,
        nowMs: Long = clockMsProvider()
    ): ConfirmTktParseOutcome {
        return runCatching {
            val root = JSONObject(rawJson)
            val dataObj = root.optJSONObject("data") ?: root
            val pnrResp = dataObj.optJSONObject("pnrResponse") ?: dataObj

            val errorCode = pnrResp.optInt("errorCode", 0)
            val errorStr = pnrResp.optString("error", "").takeIf { !it.equals("null", ignoreCase = true) }.orEmpty().trim()
            if (errorCode != 0 || errorStr.isNotBlank()) {
                return ConfirmTktParseOutcome.InvalidOrFlushed(
                    errorCode = if (errorCode != 0) errorCode else 201,
                    errorMessage = errorStr.ifBlank { "Flushed PNR / PNR not yet generated" }
                )
            }

            val trainNo = pnrResp.optString("trainNo", "").ifBlank { pnrResp.optString("trainNumber", "") }.trim()
            val trainName = pnrResp.optString("trainName", "").trim()
            val fromCode = pnrResp.optString("boardingPoint", "")
                .ifBlank { pnrResp.optString("boardingStation", pnrResp.optString("from", "")) }
                .trim().uppercase(Locale.US)
            val toCode = pnrResp.optString("reservationUpto", pnrResp.optString("to", "")).trim().uppercase(Locale.US)
            if (trainNo.isBlank() && fromCode.isBlank()) {
                return ConfirmTktParseOutcome.Unreachable
            }

            val fromStationFull = pnrResp.optString("boardingStationName", "").trim()
            val toStationFull = pnrResp.optString("reservationUptoName", "").trim()
            val doj = IrctcTicketTextExtractor.normalizeIndianRailDate(pnrResp.optString("doj", ""))
            val destDoj = IrctcTicketTextExtractor.normalizeIndianRailDate(pnrResp.optString("destinationDoj", ""))
            val depTime = pnrResp.optString("departureTime", "").trim()
            val arrTime = pnrResp.optString("arrivalTime", "").trim()
            val rawDuration = pnrResp.optString("duration", "").trim()
            val formattedDuration = formatColonDuration(rawDuration)

            val travelClass = pnrResp.optString("class", "")
                .ifBlank { pnrResp.optString("journeyClass", "") }
                .trim().uppercase(Locale.US)
            val quota = pnrResp.optString("quota", "").trim().uppercase(Locale.US)
            val chartPrepared = pnrResp.optBoolean("chartPrepared", false)
            val platformNo = pnrResp.optString("expectedPlatformNo", "").takeIf { !it.equals("null", true) }.orEmpty().trim()
            val coachPosRaw = pnrResp.optString("coachPosition", "").takeIf { !it.equals("null", true) }.orEmpty().trim()

            val ticketFareStr = pnrResp.optString("ticketFare", "").trim()
                .ifBlank { pnrResp.optString("bookingFare", "").trim() }
            val baseFarePaise = parseRupeesToPaiseExact(ticketFareStr) ?: 0L

            val cacheTimeStr = pnrResp.optString("cacheTime", "").takeIf { !it.equals("null", true) }.orEmpty().trim()
            val upstreamCacheMs = parseConfirmTktCacheTimeEpochMs(cacheTimeStr) ?: nowMs

            val paxArr = pnrResp.optJSONArray("passengerStatus")
                ?: pnrResp.optJSONArray("passengerList")
                ?: JSONArray()
            val structuredList = mutableListOf<LivePnrPassenger>()
            val displayRows = mutableListOf<String>()
            var topProbability = ""

            for (i in 0 until paxArr.length()) {
                val pObj = paxArr.optJSONObject(i) ?: continue
                val num = pObj.optInt("number", pObj.optInt("passengerSerialNumber", i + 1))
                val bkStatus = pObj.optString("bookingStatus", "").trim()
                val curStatusRaw = pObj.optString("currentStatus", "").trim()
                val coach = pObj.optString("coach", "").takeIf { !it.equals("null", true) }.orEmpty().trim()
                val berthVal = pObj.optString("berth", "").takeIf { !it.equals("null", true) }.orEmpty().trim()
                val predText = pObj.optString("prediction", "").takeIf { !it.equals("null", true) }.orEmpty().trim()
                val predPct = pObj.optString("predictionPercentage", "").takeIf { !it.equals("null", true) }.orEmpty().trim()

                val enrichedCurrent = buildConfirmTktCurrentStatus(curStatusRaw, coach, berthVal)
                val upperCur = enrichedCurrent.uppercase(Locale.US)
                val statusLabel = when {
                    upperCur.contains("WL") -> if (predPct.isNotBlank()) "WL · $predPct% Chance" else "Waitlisted"
                    upperCur.contains("RAC") -> "RAC"
                    upperCur.contains("CAN") -> "Cancelled"
                    else -> "Confirmed"
                }

                if (topProbability.isBlank() && (predPct.isNotBlank() || predText.isNotBlank())) {
                    topProbability = if (predPct.isNotBlank()) "$predPct% Confirmation Chance" else predText
                }

                structuredList.add(
                    LivePnrPassenger(
                        passengerNumber = "P$num",
                        initialStatus = bkStatus.ifBlank { enrichedCurrent },
                        currentStatus = enrichedCurrent,
                        statusLabel = statusLabel
                    )
                )

                val chanceSuffix = if (predPct.isNotBlank() && (upperCur.contains("WL") || upperCur.contains("RAC"))) {
                    " ($predPct% Chance)"
                } else ""
                val row = if (bkStatus.isNotBlank() && !bkStatus.equals(enrichedCurrent, ignoreCase = true)) {
                    "P$num: Booked [$bkStatus] → Live [$enrichedCurrent$chanceSuffix]"
                } else {
                    "P$num: $enrichedCurrent$chanceSuffix"
                }
                displayRows.add(row)
            }

            val paxCount = pnrResp.optInt("passengerCount", structuredList.size).coerceAtLeast(structuredList.size.coerceAtLeast(1))
            val badge = computeOverallStatusBadge(structuredList, displayRows)

            val resolvedFromName = if (fromStationFull.isNotBlank()) "$fromCode ($fromStationFull)" else resolveStationDisplayName(fromCode)
            val resolvedToName = if (toStationFull.isNotBlank()) "$toCode ($toStationFull)" else resolveStationDisplayName(toCode)

            val probSummary = topProbability.ifBlank {
                when {
                    badge.startsWith("CNF") && chartPrepared -> "Chart Prepared · Confirmed"
                    badge.startsWith("CNF") -> "Confirmed"
                    badge.startsWith("RAC") -> "RAC"
                    else -> "Waitlisted · Live CRIS Status"
                }
            }

            val radarParts = buildList {
                if (fromCode.isNotBlank() && toCode.isNotBlank()) add("$fromCode → $toCode")
                if (doj.isNotBlank()) add(doj)
                if (depTime.isNotBlank()) add("Dep $depTime")
                if (platformNo.isNotBlank() && platformNo != "0") add("PF $platformNo")
            }

            val snapshot = LivePnrStatusSnapshot(
                pnr = cleanPnr,
                trainNo = trainNo,
                trainName = trainName,
                fromStation = fromCode,
                toStation = toCode,
                departureTime = depTime,
                travelClass = travelClass,
                totalFareRupees = (baseFarePaise / 100L).toInt(),
                passengerCount = paxCount,
                bookingStatusBadge = badge,
                chartPrepared = chartPrepared,
                passengerStatuses = displayRows,
                structuredPassengers = structuredList,
                fromStationName = resolvedFromName,
                toStationName = resolvedToName,
                arrivalTime = arrTime,
                durationText = formattedDuration,
                quotaText = quota,
                coachPositionHint = coachPosRaw,
                liveTrainLocationRadar = radarParts.joinToString(" · "),
                confirmationProbability = probSummary,
                sourceLabel = "Live CRIS Verified",
                isLiveVerified = true,
                isManualEntry = false,
                baseFarePaiseOverride = baseFarePaise,
                departureDate = doj,
                arrivalDate = destDoj,
                platformNumber = platformNo,
                fetchedAtEpochMs = nowMs,
                upstreamCacheEpochMs = upstreamCacheMs
            )
            ConfirmTktParseOutcome.Success(snapshot)
        }.getOrElse { ConfirmTktParseOutcome.Unreachable }
    }

    private fun buildConfirmTktCurrentStatus(curStatusRaw: String, coach: String, berth: String): String {
        val cleanCur = curStatusRaw.trim()
        val upper = cleanCur.uppercase(Locale.US)
        if (upper.startsWith("CNF") && !cleanCur.contains("/")) {
            val validBerth = berth.takeIf { it.isNotBlank() && it != "0" && it != "-" }
            if (coach.isNotBlank() && validBerth != null) {
                return "CNF/$coach/$validBerth"
            }
            if (coach.isNotBlank()) {
                return "CNF/$coach"
            }
        }
        return cleanCur
    }

    private fun formatColonDuration(raw: String): String {
        val m = Regex("""^(\d{1,3}):(\d{1,2})$""").matchEntire(raw.trim()) ?: return raw.trim()
        val hrs = m.groupValues[1].toIntOrNull() ?: return raw.trim()
        val mins = m.groupValues[2].toIntOrNull() ?: return raw.trim()
        return when {
            hrs > 0 && mins > 0 -> "${hrs}h ${mins}m"
            hrs > 0 -> "${hrs}h"
            mins > 0 -> "${mins}m"
            else -> ""
        }
    }

    private fun parseConfirmTktCacheTimeEpochMs(raw: String): Long? {
        if (raw.isBlank()) return null
        return runCatching {
            val trimmed = raw.substringBefore(".").substringBefore("+").trim()
            val dt = LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US))
            dt.atZone(TicketDateTimeParser.IST).toInstant().toEpochMilli()
        }.getOrNull() ?: TicketDateTimeParser.parseDetailed(raw, clockMsProvider(), TicketDateTimeParser.IST)?.epochMs
    }

    private fun computeOverallStatusBadge(
        structured: List<LivePnrPassenger>,
        rows: List<String>
    ): String {
        val tokens = if (structured.isNotEmpty()) {
            structured.map { it.currentStatus.uppercase(Locale.US) }
        } else {
            rows.map { it.uppercase(Locale.US) }
        }
        val hasWl = tokens.any { it.contains("WL") }
        val hasRac = tokens.any { it.contains("RAC") }
        val hasCnf = tokens.any { it.contains("CNF") }
        return when {
            hasWl && hasCnf -> "PARTIAL CNF + WL"
            hasWl -> "WL (Waitlisted)"
            hasRac -> "RAC"
            else -> "CNF (Confirmed)"
        }
    }

    private fun invokeRailMitraSsr(
        transport: PnrHttpTransport,
        cleanPnr: String,
        cachedSnapshot: LivePnrStatusSnapshot?,
        nowMs: Long
    ): LivePnrStatusSnapshot? = runCatching {
        val resp = transport.execute(
            PnrHttpRequest(
                url = "https://www.railmitra.com/pnr-status?pnr=$cleanPnr",
                method = "GET",
                headers = mapOf(
                    "Accept" to "text/html,application/xhtml+xml",
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                )
            )
        )
        if (resp.statusCode !in 200..299 || resp.body.isBlank()) return null
        parseRailMitraHtml(cleanPnr, resp.body, cachedSnapshot, nowMs)
    }.getOrNull()

    internal fun parseRailMitraHtml(
        cleanPnr: String,
        html: String,
        cachedSnapshot: LivePnrStatusSnapshot? = null,
        nowMs: Long = clockMsProvider()
    ): LivePnrStatusSnapshot? = runCatching {
        val plain = stripHtmlTags(html)
        val trainHeaderMatch = Regex("""\b(\d{5})\s*[-–/]\s*([A-Z][A-Za-z0-9\s]{2,36}?)(?:\s{2,}|\n|\||Class|From)""")
            .find(plain)
        val trainNo = trainHeaderMatch?.groupValues?.getOrNull(1)?.trim()
            ?: Regex("""\b(1\d{4}|2\d{4}|0\d{4})\b""").find(plain)?.groupValues?.getOrNull(1).orEmpty()
        val trainName = trainHeaderMatch?.groupValues?.getOrNull(2)?.trim().orEmpty()

        val stationCodeMatches = Regex("""\(([A-Z]{2,5})\)""").findAll(plain).map { it.groupValues[1] }.toList()
        val fromCode = stationCodeMatches.getOrNull(0) ?: cachedSnapshot?.fromStation.orEmpty()
        val toCode = stationCodeMatches.getOrNull(1) ?: cachedSnapshot?.toStation.orEmpty()

        val paxRowRegex = Regex(
            """Passenger\s*(\d+)\s+((?:CNF|RAC|[A-Z]*WL)[A-Za-z0-9/\s\-]{0,18}?)\s+((?:CNF|RAC|[A-Z]*WL)[A-Za-z0-9/\s\-]{0,18}?)(?=\s+Passenger|\s+Chart|\n|$)""",
            RegexOption.IGNORE_CASE
        )
        val structured = mutableListOf<LivePnrPassenger>()
        val rows = mutableListOf<String>()
        for (m in paxRowRegex.findAll(plain)) {
            val num = m.groupValues[1].trim()
            val bk = m.groupValues[2].trim().replace(Regex("""\s+"""), " ")
            val cur = m.groupValues[3].trim().replace(Regex("""\s+"""), " ")
            val upper = cur.uppercase(Locale.US)
            structured.add(
                LivePnrPassenger(
                    passengerNumber = "P$num",
                    initialStatus = bk,
                    currentStatus = cur,
                    statusLabel = when {
                        upper.contains("WL") -> "Waitlisted"
                        upper.contains("RAC") -> "RAC"
                        else -> "Confirmed"
                    }
                )
            )
            rows.add(if (!bk.equals(cur, true)) "P$num: Booked [$bk] → Live [$cur]" else "P$num: $cur")
        }

        if (trainNo.isBlank() || structured.isEmpty()) return null

        val travelClass = Regex("""\bClass\s*[:\-]?\s*(1A|2A|3A|3E|CC|EC|SL|2S)\b""", RegexOption.IGNORE_CASE)
            .find(plain)?.groupValues?.getOrNull(1)?.uppercase(Locale.US)
            ?: cachedSnapshot?.travelClass.orEmpty()
        val dojRaw = Regex("""\b(\d{1,2}[\-/]\d{1,2}[\-/]\d{4}|\d{1,2}\s+[A-Za-z]{3}\s+\d{4})\b""")
            .find(plain)?.groupValues?.getOrNull(1).orEmpty()
        val doj = IrctcTicketTextExtractor.normalizeIndianRailDate(dojRaw).ifBlank { cachedSnapshot?.departureDate.orEmpty() }
        val chartPrepared = plain.contains("Chart Prepared", ignoreCase = true) &&
            !plain.contains("Chart Not Prepared", ignoreCase = true)

        val preservedBasePaise = cachedSnapshot?.baseFarePaise ?: 0L
        val badge = computeOverallStatusBadge(structured, rows)

        LivePnrStatusSnapshot(
            pnr = cleanPnr,
            trainNo = trainNo,
            trainName = trainName.ifBlank { cachedSnapshot?.trainName.orEmpty() },
            fromStation = fromCode,
            toStation = toCode,
            departureTime = cachedSnapshot?.departureTime.orEmpty(),
            travelClass = travelClass,
            totalFareRupees = (preservedBasePaise / 100L).toInt(),
            passengerCount = structured.size,
            bookingStatusBadge = badge,
            chartPrepared = chartPrepared,
            passengerStatuses = rows,
            structuredPassengers = structured,
            fromStationName = if (fromCode.isNotBlank()) resolveStationDisplayName(fromCode) else "",
            toStationName = if (toCode.isNotBlank()) resolveStationDisplayName(toCode) else "",
            arrivalTime = cachedSnapshot?.arrivalTime.orEmpty(),
            durationText = cachedSnapshot?.durationText.orEmpty(),
            quotaText = cachedSnapshot?.quotaText.orEmpty(),
            coachPositionHint = cachedSnapshot?.coachPositionHint.orEmpty(),
            liveTrainLocationRadar = listOf(
                if (fromCode.isNotBlank() && toCode.isNotBlank()) "$fromCode → $toCode" else "",
                doj
            ).filter { it.isNotBlank() }.joinToString(" · "),
            confirmationProbability = if (badge.startsWith("CNF")) "Confirmed" else "Live Status Verified",
            sourceLabel = "Live RailMitra Fallback",
            isLiveVerified = true,
            isManualEntry = false,
            baseFarePaiseOverride = preservedBasePaise.takeIf { it > 0L },
            departureDate = doj,
            fetchedAtEpochMs = nowMs,
            upstreamCacheEpochMs = nowMs
        )
    }.getOrNull()

    private fun invokeRailYatriSsr(
        transport: PnrHttpTransport,
        cleanPnr: String,
        cachedSnapshot: LivePnrStatusSnapshot?,
        nowMs: Long
    ): LivePnrStatusSnapshot? = runCatching {
        val resp = transport.execute(
            PnrHttpRequest(
                url = "https://www.railyatri.in/pnr-status/$cleanPnr",
                method = "GET",
                headers = mapOf(
                    "Accept" to "text/html,application/xhtml+xml",
                    "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                )
            )
        )
        if (resp.statusCode !in 200..299 || resp.body.isBlank()) return null
        parseRailYatriHtml(cleanPnr, resp.body, cachedSnapshot, nowMs)
    }.getOrNull()

    internal fun parseRailYatriHtml(
        cleanPnr: String,
        html: String,
        cachedSnapshot: LivePnrStatusSnapshot? = null,
        nowMs: Long = clockMsProvider()
    ): LivePnrStatusSnapshot? = runCatching {
        if (!html.contains("pnr-search-result", ignoreCase = true) && !html.contains("pasListUL", ignoreCase = true)) {
            return null
        }
        val plain = stripHtmlTags(html)
        val trainMatch = Regex("""\b(\d{5})\s*[-–]?\s*([A-Z][A-Za-z\s]{2,34}?(?:EXPRESS|EXP|RAJDHANI|SHATABDI|DURONTO|VANDE\s*BHARAT|MAIL|SF))""", RegexOption.IGNORE_CASE)
            .find(plain)
        val trainNo = trainMatch?.groupValues?.getOrNull(1)?.trim()
            ?: Regex("""\b(1\d{4}|2\d{4}|0\d{4})\b""").find(plain)?.groupValues?.getOrNull(1).orEmpty()
        val trainName = trainMatch?.groupValues?.getOrNull(2)?.trim().orEmpty()

        val liBlocks = Regex("""<li[^>]*>(.*?)</li>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .findAll(html)
            .map { stripHtmlTags(it.groupValues[1]) }
            .toList()

        val structured = mutableListOf<LivePnrPassenger>()
        val rows = mutableListOf<String>()
        var paxIdx = 1
        for (liText in liBlocks) {
            val statusTokens = Regex("""\b((?:CNF|RAC|[A-Z]*WL)(?:\s*[/,\-]?\s*[A-Z0-9]+){0,3})\b""", RegexOption.IGNORE_CASE)
                .findAll(liText)
                .map { it.groupValues[1].trim().replace(Regex("""\s+"""), " ") }
                .toList()
            if (statusTokens.size >= 2 || (liText.contains("Passenger", true) && statusTokens.isNotEmpty())) {
                val bk = statusTokens.first()
                val cur = statusTokens.getOrNull(1) ?: bk
                val upper = cur.uppercase(Locale.US)
                structured.add(
                    LivePnrPassenger(
                        passengerNumber = "P$paxIdx",
                        initialStatus = bk,
                        currentStatus = cur,
                        statusLabel = when {
                            upper.contains("WL") -> "Waitlisted"
                            upper.contains("RAC") -> "RAC"
                            else -> "Confirmed"
                        }
                    )
                )
                rows.add(if (!bk.equals(cur, true)) "P$paxIdx: Booked [$bk] → Live [$cur]" else "P$paxIdx: $cur")
                paxIdx++
            }
        }
        if (trainNo.isBlank() || structured.isEmpty()) return null

        val stationCodes = Regex("""\b([A-Z]{2,5})\s*(?:->|→|\|)\s*([A-Z]{2,5})\b""").find(plain)
        val fromCode = stationCodes?.groupValues?.getOrNull(1)
            ?: Regex("""\(([A-Z]{2,5})\)""").findAll(plain).map { it.groupValues[1] }.firstOrNull()
            ?: cachedSnapshot?.fromStation.orEmpty()
        val toCode = stationCodes?.groupValues?.getOrNull(2)
            ?: Regex("""\(([A-Z]{2,5})\)""").findAll(plain).map { it.groupValues[1] }.drop(1).firstOrNull()
            ?: cachedSnapshot?.toStation.orEmpty()

        val travelClass = Regex("""\b(1A|2A|3A|3E|CC|EC|SL|2S)\b""").find(plain)?.groupValues?.getOrNull(1)
            ?: cachedSnapshot?.travelClass.orEmpty()
        val chartPrepared = plain.contains("Chart Prepared", ignoreCase = true) &&
            !plain.contains("Chart Not Prepared", ignoreCase = true)
        val preservedBasePaise = cachedSnapshot?.baseFarePaise ?: 0L
        val badge = computeOverallStatusBadge(structured, rows)

        LivePnrStatusSnapshot(
            pnr = cleanPnr,
            trainNo = trainNo,
            trainName = trainName.ifBlank { cachedSnapshot?.trainName.orEmpty() },
            fromStation = fromCode,
            toStation = toCode,
            departureTime = cachedSnapshot?.departureTime.orEmpty(),
            travelClass = travelClass,
            totalFareRupees = (preservedBasePaise / 100L).toInt(),
            passengerCount = structured.size,
            bookingStatusBadge = badge,
            chartPrepared = chartPrepared,
            passengerStatuses = rows,
            structuredPassengers = structured,
            fromStationName = if (fromCode.isNotBlank()) resolveStationDisplayName(fromCode) else "",
            toStationName = if (toCode.isNotBlank()) resolveStationDisplayName(toCode) else "",
            arrivalTime = cachedSnapshot?.arrivalTime.orEmpty(),
            durationText = cachedSnapshot?.durationText.orEmpty(),
            quotaText = cachedSnapshot?.quotaText.orEmpty(),
            coachPositionHint = cachedSnapshot?.coachPositionHint.orEmpty(),
            liveTrainLocationRadar = if (fromCode.isNotBlank() && toCode.isNotBlank()) "$fromCode → $toCode" else "",
            confirmationProbability = if (badge.startsWith("CNF")) "Confirmed" else "Live Status Verified",
            sourceLabel = "Live RailYatri Fallback",
            isLiveVerified = true,
            isManualEntry = false,
            baseFarePaiseOverride = preservedBasePaise.takeIf { it > 0L },
            departureDate = cachedSnapshot?.departureDate.orEmpty(),
            fetchedAtEpochMs = nowMs,
            upstreamCacheEpochMs = nowMs
        )
    }.getOrNull()

    private fun stripHtmlTags(html: String): String {
        return html
            .replace(Regex("""<script[^>]*>.*?</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("""<style[^>]*>.*?</style>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("""<[^>]+>"""), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&rarr;", "->")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun buildInvalidOrFlushedPnrSnapshot(
        cleanPnr: String,
        errorMessage: String
    ): LivePnrStatusSnapshot {
        return LivePnrStatusSnapshot(
            pnr = cleanPnr,
            trainNo = "",
            trainName = "",
            fromStation = "",
            toStation = "",
            departureTime = "",
            travelClass = "",
            totalFareRupees = 0,
            passengerCount = 1,
            bookingStatusBadge = "INVALID / FLUSHED",
            chartPrepared = false,
            passengerStatuses = emptyList(),
            structuredPassengers = emptyList(),
            fromStationName = "",
            toStationName = "",
            arrivalTime = "",
            durationText = "",
            quotaText = "",
            coachPositionHint = "",
            liveTrainLocationRadar = errorMessage,
            confirmationProbability = errorMessage,
            sourceLabel = errorMessage,
            isLiveVerified = false,
            isManualEntry = true,
            isInvalidOrFlushedPnr = true
        )
    }

    internal fun parsePassengerEntriesFromCoachAndSeats(
        coachAndSeats: String,
        defaultStatus: String,
        populateDefaultWhenEmpty: Boolean
    ): Pair<Int, List<String>> {
        val explicitPaxCount = Regex("""\b(\d+)\s*Pax\b""", RegexOption.IGNORE_CASE)
            .find(coachAndSeats)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        val strippedShareSuffix = coachAndSeats.replace(
            Regex("""\([^)]*(?:/person|₹|INR|Rs)[^)]*\)""", RegexOption.IGNORE_CASE),
            ""
        )

        val seatTokens = strippedShareSuffix
            .split(",", "|", "·")
            .map { it.trim() }
            .filter { token ->
                token.isNotBlank() &&
                    !token.matches(Regex("""^Class\s+.*$""", RegexOption.IGNORE_CASE)) &&
                    !token.matches(Regex("""^\d+\s*Pax$""", RegexOption.IGNORE_CASE)) &&
                    !token.equals("Personal expense", ignoreCase = true) &&
                    !token.startsWith("for ", ignoreCase = true)
            }

        val statusFallback = defaultStatus.ifBlank { "MANUAL" }
        if (seatTokens.isNotEmpty()) {
            val targetCount = maxOf(seatTokens.size, explicitPaxCount ?: seatTokens.size)
            val normalized = (0 until targetCount).map { idx ->
                val raw = seatTokens.getOrNull(idx)
                if (!raw.isNullOrBlank()) {
                    raw
                } else {
                    "P${idx + 1}: $statusFallback"
                }
            }
            return targetCount to normalized
        }

        if (explicitPaxCount != null && explicitPaxCount > 0) {
            val generated = (1..explicitPaxCount).map { idx -> "P$idx: $statusFallback" }
            return explicitPaxCount to generated
        }

        return if (populateDefaultWhenEmpty) {
            1 to listOf("P1: $statusFallback")
        } else {
            1 to emptyList()
        }
    }

    private fun buildConfirmedSnapshotFromTicket(
        cleanPnr: String,
        ticket: ParsedTravelTicket,
        exactExpensePaise: Long = 0L
    ): LivePnrStatusSnapshot {
        val resolvedStatus = ticket.bookingStatus.ifBlank { "CNF" }
        val (resolvedPaxCount, paxRaw) = parsePassengerEntriesFromCoachAndSeats(
            coachAndSeats = ticket.coachAndSeats,
            defaultStatus = resolvedStatus,
            populateDefaultWhenEmpty = true
        )
        val structured = paxRaw.mapIndexed { idx, s ->
            val cleanSeat = s.substringAfter("P${idx + 1}:", s).trim().ifBlank { s }
            LivePnrPassenger(
                passengerNumber = "P${idx + 1}",
                initialStatus = cleanSeat,
                currentStatus = cleanSeat,
                statusLabel = resolvedStatus
            )
        }
        val originCode = ticket.fromStation
            .takeIf { it.isNotBlank() && !it.equals("ORG", ignoreCase = true) && !it.equals("ORIG", ignoreCase = true) }
            .orEmpty()
        val destCode = ticket.toStation
            .takeIf { it.isNotBlank() && !it.equals("DST", ignoreCase = true) && !it.equals("DEST", ignoreCase = true) }
            .orEmpty()
        val isCnf = resolvedStatus.startsWith("CNF", ignoreCase = true) || resolvedStatus.equals("CONFIRMED", ignoreCase = true)
        val parsedFarePaise = if (exactExpensePaise > 0L) {
            exactExpensePaise
        } else {
            parseRupeesToPaiseExact(ticket.fareRupees) ?: 0L
        }
        val extractedClass = Regex("""\bClass\s+(1A|2A|3A|3E|CC|EC|SL|2S)\b""", RegexOption.IGNORE_CASE)
            .find(ticket.coachAndSeats)?.groupValues?.getOrNull(1)?.uppercase(Locale.US).orEmpty()

        return LivePnrStatusSnapshot(
            pnr = cleanPnr,
            trainNo = ticket.trainOrFlightNo,
            trainName = ticket.trainOrCarrierName,
            fromStation = originCode,
            toStation = destCode,
            departureTime = ticket.departureTime,
            travelClass = extractedClass,
            totalFareRupees = (parsedFarePaise / 100L).toInt(),
            passengerCount = resolvedPaxCount.coerceAtLeast(1),
            bookingStatusBadge = resolvedStatus,
            chartPrepared = isCnf || ticket.chartStatus == "Chart Prepared",
            passengerStatuses = paxRaw,
            structuredPassengers = structured,
            fromStationName = if (originCode.isNotBlank()) resolveStationDisplayName(originCode) else "",
            toStationName = if (destCode.isNotBlank()) resolveStationDisplayName(destCode) else "",
            arrivalTime = "",
            durationText = "",
            quotaText = "",
            coachPositionHint = if (isCnf) "Confirmed · Offline Vault" else "Offline Vault",
            liveTrainLocationRadar = if (originCode.isNotBlank() && destCode.isNotBlank()) "$originCode → $destCode" else "",
            confirmationProbability = if (isCnf) "100% Confirmed" else "",
            sourceLabel = "Saved in Offline Vault (0 Internet Used)",
            isLiveVerified = true,
            isManualEntry = false,
            baseFarePaiseOverride = parsedFarePaise.takeIf { it > 0L },
            totalPayablePaiseOverride = parsedFarePaise.takeIf { it > 0L },
            departureDate = ticket.departureDate,
            fetchedAtEpochMs = clockMsProvider()
        )
    }

    /**
     * Builds an honest, zero-placeholder manual-entry snapshot when live PNR servers are unreachable,
     * rate-limited, or when the user chooses to enter ticket details manually.
     */
    fun buildUnverifiedManualFallbackSnapshot(
        cleanPnr: String,
        fallbackTicket: ParsedTravelTicket = ParsedTravelTicket(),
        reasonLabel: String = "Manual Ticket Entry Mode",
        cooldownRemainingSeconds: Int = 0
    ): LivePnrStatusSnapshot {
        val (resolvedPaxCount, existingPassengers) = parsePassengerEntriesFromCoachAndSeats(
            coachAndSeats = fallbackTicket.coachAndSeats,
            defaultStatus = fallbackTicket.bookingStatus.ifBlank { "MANUAL" },
            populateDefaultWhenEmpty = false
        )
        val structured = existingPassengers.mapIndexed { idx, s ->
            val cleanSeat = s.substringAfter("P${idx + 1}:", s).trim().ifBlank { s }
            LivePnrPassenger(
                passengerNumber = "P${idx + 1}",
                initialStatus = cleanSeat,
                currentStatus = cleanSeat,
                statusLabel = if (cleanSeat.contains("CNF", true)) "Confirmed" else "Passenger"
            )
        }
        val fromCode = fallbackTicket.fromStation
            .takeIf { it.isNotBlank() && !it.equals("ORG", ignoreCase = true) && !it.equals("ORIG", ignoreCase = true) && it != "---" }
            .orEmpty()
        val toCode = fallbackTicket.toStation
            .takeIf { it.isNotBlank() && !it.equals("DST", ignoreCase = true) && !it.equals("DEST", ignoreCase = true) && it != "---" }
            .orEmpty()
        val parsedFarePaise = parseRupeesToPaiseExact(fallbackTicket.fareRupees) ?: 0L
        val extractedClass = Regex("""\bClass\s+(1A|2A|3A|3E|CC|EC|SL|2S)\b""", RegexOption.IGNORE_CASE)
            .find(fallbackTicket.coachAndSeats)?.groupValues?.getOrNull(1)?.uppercase(Locale.US).orEmpty()

        return LivePnrStatusSnapshot(
            pnr = cleanPnr,
            trainNo = fallbackTicket.trainOrFlightNo,
            trainName = fallbackTicket.trainOrCarrierName,
            fromStation = fromCode,
            toStation = toCode,
            departureTime = fallbackTicket.departureTime,
            travelClass = extractedClass,
            totalFareRupees = (parsedFarePaise / 100L).toInt(),
            passengerCount = maxOf(resolvedPaxCount, existingPassengers.size, 1),
            bookingStatusBadge = fallbackTicket.bookingStatus.ifBlank { "MANUAL" },
            chartPrepared = false,
            passengerStatuses = existingPassengers,
            structuredPassengers = structured,
            fromStationName = if (fromCode.isNotBlank()) resolveStationDisplayName(fromCode) else "",
            toStationName = if (toCode.isNotBlank()) resolveStationDisplayName(toCode) else "",
            arrivalTime = "",
            durationText = "",
            quotaText = "",
            coachPositionHint = "Enter Total Ticket Fare & Route below or paste IRCTC SMS",
            liveTrainLocationRadar = reasonLabel,
            confirmationProbability = reasonLabel,
            sourceLabel = reasonLabel,
            isLiveVerified = false,
            isManualEntry = true,
            baseFarePaiseOverride = parsedFarePaise.takeIf { it > 0L },
            departureDate = fallbackTicket.departureDate,
            cooldownRemainingSeconds = cooldownRemainingSeconds
        )
    }

    fun fetchLiveFlightStatusByNumber(
        flightNumber: String,
        optionalPnr: String = "",
        context: Context? = null,
        forceManualRefresh: Boolean = false
    ): LivePnrStatusSnapshot? = runCatching {
        val cleanFlight = flightNumber.replace(Regex("[^A-Za-z0-9]"), "").uppercase(Locale.US)
        if (cleanFlight.length < 3) return null
        val cleanPnr = normalizePnrKey(optionalPnr)
        if (!forceManualRefresh && (cleanPnr.length == 6 || cleanPnr.length == 10)) {
            loadPersistedPnrSnapshot(context, cleanPnr)?.let { cached ->
                val stubTicket = ParsedTravelTicket(pnr = cleanPnr, statusExplicit = true)
                if (isSnapshotAllConfirmed(cached) || isTicketJourneyInPast(stubTicket, cached)) return cached
            }
        }
        val cacheKey = "FLIGHT_${optionalPnr.ifBlank { cleanFlight }}"
        if (!forceManualRefresh) {
            synchronized(lruSnapshotCache) {
                lruSnapshotCache[cacheKey]?.let { return it }
            }
        }

        val transport = httpTransportOverride ?: defaultHttpTransport
        val resp = transport.execute(
            PnrHttpRequest(
                url = "https://api.flightradar24.com/common/v1/flight/list.json?query=$cleanFlight&fetchBy=flight&page=1&limit=1",
                method = "GET",
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36",
                    "Accept" to "application/json"
                )
            )
        )
        if (resp.statusCode != 200 || resp.body.isBlank()) return null
        val root = JSONObject(resp.body)
        val item = root.optJSONObject("result")
            ?.optJSONObject("response")
            ?.optJSONArray("data")
            ?.optJSONObject(0) ?: return null

        val resolvedNo = item.optJSONObject("identification")
            ?.optJSONObject("number")
            ?.optString("default", cleanFlight) ?: cleanFlight
        val statusText = item.optJSONObject("status")?.optString("text", "Scheduled") ?: "Scheduled"
        val aircraftCode = item.optJSONObject("aircraft")?.optJSONObject("model")?.optString("code", "").orEmpty()

        val originObj = item.optJSONObject("airport")?.optJSONObject("origin")
        val destObj = item.optJSONObject("airport")?.optJSONObject("destination")
        val fromIata = originObj?.optJSONObject("code")?.optString("iata", "").orEmpty()
        val toIata = destObj?.optJSONObject("code")?.optString("iata", "").orEmpty()
        val fromCity = originObj?.optJSONObject("region")?.optString("city", fromIata).orEmpty()
        val toCity = destObj?.optJSONObject("region")?.optString("city", toIata).orEmpty()

        val schedObj = item.optJSONObject("time")?.optJSONObject("scheduled")
        val depEpochSec = schedObj?.optLong("departure", 0L) ?: 0L
        val arrEpochSec = schedObj?.optLong("arrival", 0L) ?: 0L
        val istFormat = SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        val depTime = if (depEpochSec > 0L) istFormat.format(Date(depEpochSec * 1000L)) else ""
        val arrTime = if (arrEpochSec > 0L) istFormat.format(Date(arrEpochSec * 1000L)) else ""

        val snapshot = LivePnrStatusSnapshot(
            pnr = optionalPnr.ifBlank { resolvedNo },
            trainNo = resolvedNo,
            trainName = if (aircraftCode.isNotBlank()) "Flight $resolvedNo ($aircraftCode)" else "Flight $resolvedNo",
            fromStation = fromIata,
            toStation = toIata,
            departureTime = depTime,
            travelClass = "",
            totalFareRupees = 0,
            passengerCount = 1,
            bookingStatusBadge = "CNF",
            chartPrepared = true,
            passengerStatuses = listOf("CNF • $statusText"),
            structuredPassengers = listOf(
                LivePnrPassenger(
                    passengerNumber = "P1",
                    initialStatus = "CNF",
                    currentStatus = statusText,
                    statusLabel = statusText
                )
            ),
            fromStationName = if (fromCity.isNotBlank()) "$fromIata ($fromCity)" else fromIata,
            toStationName = if (toCity.isNotBlank()) "$toIata ($toCity)" else toIata,
            arrivalTime = arrTime,
            durationText = "",
            quotaText = "",
            coachPositionHint = "Flight $resolvedNo • $statusText",
            liveTrainLocationRadar = statusText,
            confirmationProbability = "100% Confirmed",
            sourceLabel = "Live Flight Status",
            isLiveVerified = true,
            isManualEntry = false,
            fetchedAtEpochMs = clockMsProvider()
        )
        synchronized(lruSnapshotCache) {
            lruSnapshotCache[cacheKey] = snapshot
        }
        snapshot
    }.getOrNull()
}
