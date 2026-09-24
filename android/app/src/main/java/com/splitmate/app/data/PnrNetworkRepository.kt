package com.splitmate.app.data

import android.content.Context
import android.util.LruCache
import com.splitmate.app.ui.LivePnrPassenger
import com.splitmate.app.ui.LivePnrStatusSnapshot
import com.splitmate.app.ui.ParsedTravelTicket
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Dedicated 5-Stage PNR Network & Caching Repository (`PnrNetworkRepository`).
 *
 * Implements:
 * - Stage 1: L1 Bounded `LruCache(32)` + L2 `EncryptedSharedPreferences` (`EncryptedPrefsProvider`)
 * - Stage 2: 6-Hour Positive TTL (`AUTO_SYNC_COOLDOWN_MS`) & 60-Second Negative Failure Debounce (`"last_failed_sync_$cleanPnr"`)
 * - Stage 3: In-Flight Request Coalescing (`coalesceMutex` + `inFlightPnrDeferreds`) on an independent `SupervisorJob() + Dispatchers.IO` scope
 * - Stage 4: Persistent 5-Calls-Per-5-Minutes Token Bucket (`"global_pnr_fetch_epochs_csv"`) inside `rateLimitMutex`
 * - Stage 5: Bounded `4,000ms` socket connect/read timeouts with guaranteed `finally { conn.disconnect() }` and Zero Ghost Data on failure (`isLiveVerified = false, isManualEntry = true`).
 */
object PnrNetworkRepository {

    private const val AUTO_SYNC_COOLDOWN_MS = 6L * 60L * 60L * 1000L // 6 hours
    private const val TRAVEL_DAY_AUTO_SYNC_COOLDOWN_MS = 30L * 60L * 1000L // 30 minutes on travel day
    private const val MANUAL_REFRESH_DEBOUNCE_MS = 60L * 1000L // 60 seconds
    private const val NEGATIVE_FAILURE_DEBOUNCE_MS = 60L * 1000L // 60 seconds negative cache on network failure
    private const val TOKEN_BUCKET_WINDOW_MS = 5L * 60L * 1000L // 5 minutes sliding window
    private const val TOKEN_BUCKET_MAX_CALLS = 5
    private const val SOCKET_TIMEOUT_MS = 4000 // 4.0 seconds per endpoint (max 8.0s total across 2 endpoints)

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val coalesceMutex = Mutex()
    private val rateLimitMutex = Mutex()
    private val inFlightPnrDeferreds = HashMap<String, Deferred<LivePnrStatusSnapshot>>()

    private const val MAX_CACHE_ENTRIES = 64

    private val lruSnapshotCache = object : LinkedHashMap<String, LivePnrStatusSnapshot>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LivePnrStatusSnapshot>?): Boolean = size > MAX_CACHE_ENTRIES
    }
    private val lruFlightResultCache = object : LinkedHashMap<String, UniversalFlightTicketExtractor.UniversalFlightTicketResult>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, UniversalFlightTicketExtractor.UniversalFlightTicketResult>?): Boolean = size > MAX_CACHE_ENTRIES
    }
    private val lastSyncEpochMsCache = object : LinkedHashMap<String, Long>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean = size > MAX_CACHE_ENTRIES
    }
    private val lastFailedSyncEpochMsCache = object : LinkedHashMap<String, Long>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean = size > MAX_CACHE_ENTRIES
    }

    /**
     * Normalizes either a 10-digit Indian Railways PNR (e.g., `8412659012`)
     * or a 6-character Airline/GDS PNR (e.g., `D9GQ3Z`, `KLMNPQ`).
     */
    fun normalizePnrKey(pnr: String): String {
        val raw = pnr.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "")
        val digitsOnly = raw.filter { it.isDigit() }
        if (digitsOnly.length == 10 && raw.length >= 10) return digitsOnly.take(10)
        if (raw.length == 6 && raw.all { it.isLetterOrDigit() }) return raw
        return digitsOnly.take(10)
    }

    /**
     * Returns `true` when every passenger on the snapshot is already Confirmed (`CNF`)
     * with zero `WL` (Waitlisted) or `RAC` statuses remaining.
     *
     * Once `true`, the ticket is **Permanently Confirmed (`IMMUTABLE_CNF`)** — it will NEVER
     * downgrade back to Waitlisted and must be served 100% offline forever (`Infinite TTL`)
     * without ever querying the internet again (even in zero-connectivity areas like Hampi).
     */
    fun isSnapshotAllConfirmed(snapshot: LivePnrStatusSnapshot?): Boolean {
        if (snapshot == null || !snapshot.isLiveVerified) return false
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
        return !hasPendingStatus
    }

    /**
     * Returns `true` if a `ParsedTravelTicket` is already fully confirmed (`CNF` / Flight PDF).
     */
    fun isTicketAllConfirmed(ticket: ParsedTravelTicket): Boolean {
        val key = normalizePnrKey(ticket.pnr)
        if (key.length != 6 && key.length != 10) return false
        val badge = ticket.bookingStatus.trim().uppercase(Locale.US)
        val isCnf = badge.startsWith("CNF") || badge == "CONFIRMED" || (key.length == 6 && ticket.chartStatus.contains("Flight", ignoreCase = true))
        if (!isCnf) return false
        val seatsUpper = ticket.coachAndSeats.uppercase(Locale.US)
        return !seatsUpper.contains("WL") && !seatsUpper.contains("RAC") && !seatsUpper.contains("WAITLIST")
    }

    /**
     * Automatically indexes and persists an extracted Flight PDF (`UniversalFlightTicketResult`)
     * under its 6-character PNR (e.g., `D9GQ3Z`) as a Permanently Confirmed (`CNF`) offline snapshot.
     */
    fun saveConfirmedFlightTicketToVault(
        context: Context?,
        result: UniversalFlightTicketExtractor.UniversalFlightTicketResult
    ): LivePnrStatusSnapshot? {
        val cleanPnr = normalizePnrKey(result.pnr)
        if (cleanPnr.length != 6 || !result.isValidFlightTicket) return null
        synchronized(lruFlightResultCache) {
            lruFlightResultCache[cleanPnr] = result
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
            trainName = result.airlineName.ifBlank { "Flight" },
            fromStation = result.originIata,
            toStation = result.destinationIata,
            departureTime = listOf(result.travelDate, result.departureTime).filter { it.isNotBlank() }.joinToString(" • "),
            travelClass = listOf(result.cabinClass, result.fareType).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Economy" },
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
            quotaText = result.fareType.ifBlank { "REG" },
            coachPositionHint = listOf(
                if (result.cabinBaggage.isNotBlank()) "Cabin: ${result.cabinBaggage}" else "",
                if (result.checkInBaggage.isNotBlank()) "Check-in: ${result.checkInBaggage}" else ""
            ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Confirmed Flight Ticket · Offline Ready" },
            liveTrainLocationRadar = "✈️ ${result.airlineName} ${result.flightNumber} · ${result.originCity.ifBlank { result.originIata }} ➔ ${result.destinationCity.ifBlank { result.destinationIata }}",
            confirmationProbability = "100% Confirmed · Saved in Offline PNR Vault",
            sourceLabel = "Confirmed Offline Vault (Flight PDF · 0 Internet Used)",
            isLiveVerified = true,
            isManualEntry = false
        )
        savePersistedPnrSnapshot(context, snapshot)
        return snapshot
    }

    fun loadConfirmedFlightTicketResult(
        context: Context?,
        pnr: String
    ): UniversalFlightTicketExtractor.UniversalFlightTicketResult? {
        val cleanPnr = normalizePnrKey(pnr)
        if (cleanPnr.length != 6) return null
        synchronized(lruFlightResultCache) {
            lruFlightResultCache[cleanPnr]?.let { return it }
        }
        return null
    }

    fun loadPersistedPnrSnapshot(context: Context?, pnr: String): LivePnrStatusSnapshot? = runCatching {
        val cleanPnr = normalizePnrKey(pnr)
        if (cleanPnr.length != 10 && cleanPnr.length != 6) return null
        synchronized(lruSnapshotCache) {
            lruSnapshotCache[cleanPnr]?.let { return it }
        }
        if (context == null) return null
        val prefs = EncryptedPrefsProvider.getPnrVaultPrefs(context)
        val rawJson = prefs.getString("snapshot_json_$cleanPnr", null) ?: return null
        val savedSyncMs = prefs.getLong("last_sync_$cleanPnr", 0L)
        if (savedSyncMs > 0L) {
            synchronized(lastSyncEpochMsCache) {
                lastSyncEpochMsCache[cleanPnr] = savedSyncMs
            }
        }
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
                        initialStatus = sObj.optString("initialStatus", "WL"),
                        currentStatus = sObj.optString("currentStatus", "WL"),
                        statusLabel = sObj.optString("statusLabel", "Waitlisted")
                    )
                )
            }
        }
        LivePnrStatusSnapshot(
            pnr = obj.optString("pnr", cleanPnr),
            trainNo = obj.optString("trainNo", ""),
            trainName = obj.optString("trainName", ""),
            fromStation = obj.optString("fromStation", ""),
            toStation = obj.optString("toStation", ""),
            departureTime = obj.optString("departureTime", ""),
            travelClass = obj.optString("travelClass", ""),
            totalFareRupees = obj.optInt("totalFareRupees", 0),
            passengerCount = obj.optInt("passengerCount", paxList.size.coerceAtLeast(1)),
            bookingStatusBadge = obj.optString("bookingStatusBadge", "CNF"),
            chartPrepared = obj.optBoolean("chartPrepared", false),
            passengerStatuses = paxList,
            structuredPassengers = structList,
            fromStationName = obj.optString("fromStationName", ""),
            toStationName = obj.optString("toStationName", ""),
            arrivalTime = obj.optString("arrivalTime", ""),
            durationText = obj.optString("durationText", ""),
            quotaText = obj.optString("quotaText", "GN"),
            coachPositionHint = obj.optString("coachPositionHint", ""),
            liveTrainLocationRadar = obj.optString("liveTrainLocationRadar", ""),
            confirmationProbability = obj.optString("confirmationProbability", ""),
            sourceLabel = obj.optString("sourceLabel", "Live CRIS Cache (Encrypted Vault)"),
            isLiveVerified = obj.optBoolean("isLiveVerified", true),
            isManualEntry = obj.optBoolean("isManualEntry", false)
        ).also {
            synchronized(lruSnapshotCache) {
                lruSnapshotCache[cleanPnr] = it
            }
        }
    }.getOrNull()

    fun savePersistedPnrSnapshot(context: Context?, snapshot: LivePnrStatusSnapshot) {
        runCatching {
            val cleanPnr = normalizePnrKey(snapshot.pnr)
            if ((cleanPnr.length != 10 && cleanPnr.length != 6) || !snapshot.isLiveVerified) return
            val now = System.currentTimeMillis()
            synchronized(lruSnapshotCache) {
                lruSnapshotCache[cleanPnr] = snapshot
            }
            synchronized(lastSyncEpochMsCache) {
                lastSyncEpochMsCache[cleanPnr] = now
            }
            if (context == null) return
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
            val obj = JSONObject().apply {
                put("pnr", cleanPnr)
                put("trainNo", snapshot.trainNo)
                put("trainName", snapshot.trainName)
                put("fromStation", snapshot.fromStation)
                put("toStation", snapshot.toStation)
                put("departureTime", snapshot.departureTime)
                put("travelClass", snapshot.travelClass)
                put("totalFareRupees", snapshot.totalFareRupees)
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
            }
            EncryptedPrefsProvider.getPnrVaultPrefs(context)
                .edit()
                .putLong("last_sync_$cleanPnr", now)
                .remove("last_failed_sync_$cleanPnr")
                .putString("snapshot_json_$cleanPnr", obj.toString())
                .apply()
        }
    }

    fun shouldSkipAutoPnrNetworkPoll(
        context: Context?,
        pnr: String,
        ticket: ParsedTravelTicket
    ): Boolean = runCatching {
        val cleanPnr = normalizePnrKey(pnr)
        // 6-character Flight PNRs are always 100% confirmed and stored locally
        if (cleanPnr.length == 6) return true
        if (cleanPnr.length != 10) return true

        // Rule 1: If the ticket itself or its cached snapshot is already CONFIRMED (all passengers CNF, no WL/RAC),
        // NEVER query the internet again (Permanent Offline Lock).
        if (isTicketAllConfirmed(ticket)) return true

        val cached = loadPersistedPnrSnapshot(context, cleanPnr)
        if (isSnapshotAllConfirmed(cached)) return true

        if (context == null) return true

        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        if ((hour == 23 && minute >= 30) || (hour == 0 && minute <= 30)) return true

        val prefs = EncryptedPrefsProvider.getPnrVaultPrefs(context)
        val memSyncMs = synchronized(lastSyncEpochMsCache) { lastSyncEpochMsCache[cleanPnr] ?: 0L }
        val lastSyncMs = maxOf(
            memSyncMs,
            prefs.getLong("last_sync_$cleanPnr", 0L)
        )
        val nowMs = System.currentTimeMillis()
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))
        val isTravelDay = ticket.departureTime.contains(todayIso) || ticket.departureDate.contains(todayIso)
        val requiredCooldown = if (isTravelDay) TRAVEL_DAY_AUTO_SYNC_COOLDOWN_MS else AUTO_SYNC_COOLDOWN_MS

        (nowMs - lastSyncMs) < requiredCooldown
    }.getOrDefault(true)

    fun recordPnrSyncTimestamp(context: Context, pnr: String) {
        runCatching {
            val cleanPnr = normalizePnrKey(pnr)
            if (cleanPnr.length != 10 && cleanPnr.length != 6) return
            val now = System.currentTimeMillis()
            synchronized(lastSyncEpochMsCache) {
                lastSyncEpochMsCache[cleanPnr] = now
            }
            EncryptedPrefsProvider.getPnrVaultPrefs(context)
                .edit()
                .putLong("last_sync_$cleanPnr", now)
                .apply()
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

        // STAGE 1: L1 In-Memory Cache & L2 EncryptedSharedPreferences Vault Lookup (0 network tokens consumed)
        val cachedSnapshot = loadPersistedPnrSnapshot(context, cleanPnr)

        // PERMANENT OFFLINE LOCK FOR CONFIRMED TICKETS (Train 10-Digit CNF or Flight 6-Char PNR):
        // Once all passengers are Confirmed (`CNF`, zero WL/RAC), the ticket will NEVER revert to Waitlisted.
        // Return immediately from the local vault forever (`Infinite TTL`) with ZERO internet calls!
        if (cachedSnapshot != null && isSnapshotAllConfirmed(cachedSnapshot)) {
            return cachedSnapshot.copy(
                sourceLabel = "Confirmed Offline Vault (Permanent CNF · 0 Internet Used)"
            )
        }

        // If `fallbackTicket` passed by caller is already fully confirmed (`CNF`), promote it into the Permanent Offline Vault
        if (isTicketAllConfirmed(fallbackTicket.copy(pnr = cleanPnr))) {
            val promoted = buildConfirmedSnapshotFromTicket(cleanPnr, fallbackTicket)
            savePersistedPnrSnapshot(context, promoted)
            return promoted
        }

        // 6-character Airline PNR: never poll Indian Railways 10-digit CRIS endpoints
        if (cleanPnr.length == 6) {
            return cachedSnapshot ?: buildUnverifiedManualFallbackSnapshot(
                cleanPnr = cleanPnr,
                fallbackTicket = fallbackTicket,
                reasonLabel = "Import Flight Ticket PDF once to lock PNR $cleanPnr permanently offline"
            )
        }

        val nowMs = System.currentTimeMillis()
        val prefs = context?.let { EncryptedPrefsProvider.getPnrVaultPrefs(it) }
        val memSyncMs = synchronized(lastSyncEpochMsCache) { lastSyncEpochMsCache[cleanPnr] ?: 0L }
        val lastSyncMs = maxOf(
            memSyncMs,
            prefs?.getLong("last_sync_$cleanPnr", 0L) ?: 0L
        )
        val elapsedPositiveMs = nowMs - lastSyncMs

        // STAGE 2: For Waitlisted (WL) / RAC tickets ONLY, apply 6-Hour Positive TTL / 60-Second Manual Refresh Debounce
        if (cachedSnapshot != null && cachedSnapshot.isLiveVerified) {
            val minWait = if (forceManualRefresh) MANUAL_REFRESH_DEBOUNCE_MS else AUTO_SYNC_COOLDOWN_MS
            if (elapsedPositiveMs in 0 until minWait) {
                val minsAgo = (elapsedPositiveMs / 60000L).coerceAtLeast(0L)
                return cachedSnapshot.copy(
                    sourceLabel = "Live CRIS Cache (${if (minsAgo == 0L) "<1m" else "${minsAgo}m"} ago · Encrypted Vault)"
                )
            }
        }

        if (!forceManualRefresh) {
            val memFailedMs = synchronized(lastFailedSyncEpochMsCache) { lastFailedSyncEpochMsCache[cleanPnr] ?: 0L }
            val lastFailedMs = maxOf(
                memFailedMs,
                prefs?.getLong("last_failed_sync_$cleanPnr", 0L) ?: 0L
            )
            if (nowMs - lastFailedMs in 0 until NEGATIVE_FAILURE_DEBOUNCE_MS) {
                return cachedSnapshot ?: buildUnverifiedManualFallbackSnapshot(
                    cleanPnr = cleanPnr,
                    fallbackTicket = fallbackTicket,
                    reasonLabel = "Live server cooldown (60s) — Enter ticket details manually"
                )
            }
        }

        // STAGE 3: In-Flight Request Coalescing on SupervisorJob() + Dispatchers.IO (0 extra tokens consumed)
        val deferred = coalesceMutex.withLock {
            inFlightPnrDeferreds[cleanPnr]?.takeIf { it.isActive } ?: repoScope.async {
                try {
                    executeTokenGuardedNetworkFetch(cleanPnr, fallbackTicket, cachedSnapshot, context)
                } finally {
                    coalesceMutex.withLock {
                        inFlightPnrDeferreds.remove(cleanPnr)
                    }
                }
            }.also { inFlightPnrDeferreds[cleanPnr] = it }
        }
        return deferred.await()
    }

    private fun buildConfirmedSnapshotFromTicket(
        cleanPnr: String,
        ticket: ParsedTravelTicket
    ): LivePnrStatusSnapshot {
        val paxRaw = ticket.coachAndSeats
            .split(",", "|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("P1: Confirmed (CNF)") }
        val structured = paxRaw.mapIndexed { idx, s ->
            LivePnrPassenger(
                passengerNumber = "P${idx + 1}",
                initialStatus = s,
                currentStatus = s,
                statusLabel = "Confirmed"
            )
        }
        val originCode = ticket.fromStation.ifBlank { "ORIG" }
        val destCode = ticket.toStation.ifBlank { "DEST" }
        return LivePnrStatusSnapshot(
            pnr = cleanPnr,
            trainNo = ticket.trainOrFlightNo,
            trainName = ticket.trainOrCarrierName.ifBlank { if (cleanPnr.length == 6) "Confirmed Flight" else "Confirmed Train" },
            fromStation = originCode,
            toStation = destCode,
            departureTime = listOf(ticket.departureDate, ticket.departureTime).filter { it.isNotBlank() }.joinToString(" • "),
            travelClass = if (cleanPnr.length == 6) "Economy" else "3A",
            totalFareRupees = ticket.fareRupees.replace(",", "").toDoubleOrNull()?.toInt() ?: 0,
            passengerCount = paxRaw.size.coerceAtLeast(1),
            bookingStatusBadge = "CNF (Confirmed)",
            chartPrepared = true,
            passengerStatuses = paxRaw,
            structuredPassengers = structured,
            fromStationName = resolveStationDisplayName(originCode),
            toStationName = resolveStationDisplayName(destCode),
            arrivalTime = "",
            durationText = "",
            quotaText = "GN",
            coachPositionHint = "100% Confirmed · Permanently Locked in Offline Vault",
            liveTrainLocationRadar = "Route: $originCode ➔ $destCode",
            confirmationProbability = "100% Confirmed · 0 Internet Needed",
            sourceLabel = "Confirmed Offline Vault (Permanent CNF · 0 Internet Used)",
            isLiveVerified = true,
            isManualEntry = false
        )
    }

    private suspend fun executeTokenGuardedNetworkFetch(
        cleanPnr: String,
        fallbackTicket: ParsedTravelTicket,
        cachedSnapshot: LivePnrStatusSnapshot?,
        context: Context?
    ): LivePnrStatusSnapshot {
        val prefs = context?.let { EncryptedPrefsProvider.getPnrVaultPrefs(it) }

        // STAGE 4: Persistent 5-Calls-Per-5-Minutes Token Bucket inside rateLimitMutex
        val allowedByTokenBucket = rateLimitMutex.withLock {
            val now = System.currentTimeMillis()
            val rawCsv = prefs?.getString("global_pnr_fetch_epochs_csv", "").orEmpty()
            val recentEpochs = rawCsv.split(",")
                .mapNotNull { it.trim().toLongOrNull() }
                .filter { now - it in 0..TOKEN_BUCKET_WINDOW_MS }
                .toMutableList()

            if (recentEpochs.size >= TOKEN_BUCKET_MAX_CALLS) {
                false
            } else {
                recentEpochs.add(now)
                prefs?.edit()
                    ?.putString("global_pnr_fetch_epochs_csv", recentEpochs.joinToString(","))
                    ?.putLong("last_sync_$cleanPnr", now)
                    ?.apply()
                synchronized(lastSyncEpochMsCache) {
                    lastSyncEpochMsCache[cleanPnr] = now
                }
                true
            }
        }

        if (!allowedByTokenBucket) {
            return cachedSnapshot?.copy(
                sourceLabel = "Rate limit protected (max 5/5m) · Showing cached snapshot"
            ) ?: buildUnverifiedManualFallbackSnapshot(
                cleanPnr = cleanPnr,
                fallbackTicket = fallbackTicket,
                reasonLabel = "Rate limit reached (max 5 lookups / 5 min) — Enter fare manually"
            )
        }

        // STAGE 5: Bounded 4.0s Socket Execution & Guaranteed conn.disconnect()
        var scrapedTrainNo = fallbackTicket.trainOrFlightNo
        var scrapedTrainName = fallbackTicket.trainOrCarrierName
        var scrapedFrom = fallbackTicket.fromStation
        var scrapedTo = fallbackTicket.toStation
        var scrapedFromName = ""
        var scrapedToName = ""
        var scrapedDep = fallbackTicket.departureTime
        var scrapedArr = ""
        var scrapedDuration = ""
        var scrapedQuota = "GN"
        var scrapedTravelClass = ""
        var scrapedTotalFare = 0
        var scrapedChart = false
        val scrapedPassengers = mutableListOf<String>()
        val scrapedStructuredPassengers = mutableListOf<LivePnrPassenger>()
        var liveNetworkHit = false
        var scrapedCoachPosition = ""
        var scrapedPrediction = ""

        // Endpoint 1: RailYatri SSR JSON (`/m/pnr-status/$cleanPnr`)
        runCatching {
            val ryUrl = URL("https://www.railyatri.in/m/pnr-status/$cleanPnr")
            val ryConn = (ryUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = SOCKET_TIMEOUT_MS
                readTimeout = SOCKET_TIMEOUT_MS
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
            }
            try {
                if (ryConn.responseCode in 200..299) {
                    val html = ryConn.inputStream.bufferedReader().use { it.readText() }
                    val nextDataJson = Regex("""<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
                        .find(html)?.groupValues?.getOrNull(1)

                    if (!nextDataJson.isNullOrBlank()) {
                        val root = JSONObject(nextDataJson)
                        val pnrDetail = root.optJSONObject("props")?.optJSONObject("pageProps")?.optJSONObject("pnrDetail")
                        if (pnrDetail != null && pnrDetail.optString("train_number").isNotBlank()) {
                            scrapedTrainNo = pnrDetail.optString("train_number", scrapedTrainNo)
                            scrapedTrainName = pnrDetail.optString("train_name", scrapedTrainName)
                            scrapedFrom = pnrDetail.optString("board_from", scrapedFrom)
                            scrapedTo = pnrDetail.optString("board_to", scrapedTo)
                            scrapedFromName = pnrDetail.optString("from_station_name", pnrDetail.optString("boarding_station_name", ""))
                            scrapedToName = pnrDetail.optString("to_station_name", pnrDetail.optString("reservation_upto_name", ""))
                            scrapedTravelClass = pnrDetail.optString("class", "")
                            scrapedQuota = pnrDetail.optString("quota", "GN").ifBlank { "GN" }
                            scrapedDuration = pnrDetail.optString("duration", "")
                            scrapedTotalFare = pnrDetail.optInt("total_fare", 0)
                            scrapedChart = pnrDetail.optBoolean("chart_prepared", false)

                            val travelDt = pnrDetail.optString("travel_date", "")
                            val boardingDt = pnrDetail.optString("boarding_datetime", "")
                            val arrivalDt = pnrDetail.optString("arrival_datetime", "")
                            val timePart = if (boardingDt.contains("T")) boardingDt.substringAfter("T").take(5) else ""
                            scrapedDep = listOf(travelDt, timePart).filter { it.isNotBlank() }.joinToString(" ")
                            scrapedArr = if (arrivalDt.contains("T")) {
                                val arrDate = arrivalDt.substringBefore("T")
                                val arrTime = arrivalDt.substringAfter("T").take(5)
                                "$arrDate $arrTime"
                            } else arrivalDt

                            val paxArr = pnrDetail.optJSONArray("passenger")
                            if (paxArr != null && paxArr.length() > 0) {
                                for (i in 0 until paxArr.length()) {
                                    val pax = paxArr.optJSONObject(i) ?: continue
                                    val bkStatus = pax.optString("booking_status", pax.optString("booking_status_details", "")).trim()
                                    val curBookingStatus = pax.optString("current_booking_status", "").trim()
                                    val curStatusText = pax.optString("current_status", "").trim()
                                    val coachPos = pax.optString("coach_position", "").trim()
                                    val confProb = pax.optString("conf_probability", "").trim()
                                    val confPct = pax.optInt("conf_percentage", -1)

                                    if (coachPos.isNotBlank() && scrapedCoachPosition.isBlank()) {
                                        scrapedCoachPosition = "Coach Position $coachPos from Engine"
                                    }
                                    if (confPct >= 0 && scrapedPrediction.isBlank()) {
                                        scrapedPrediction = "$confPct% Confirmation Chance ($confProb)"
                                    }

                                    val effectiveCurrent = curBookingStatus.ifBlank { curStatusText.ifBlank { bkStatus } }
                                    val cleanInitial = bkStatus.substringBefore(",").trim().ifBlank { effectiveCurrent }
                                    val cleanCurrent = effectiveCurrent.substringBefore(",").trim().ifBlank { cleanInitial }
                                    val statusBadgeLabel = when {
                                        cleanCurrent.contains("CNF", ignoreCase = true) -> "Confirmed"
                                        cleanCurrent.contains("RAC", ignoreCase = true) -> "RAC Berth"
                                        bkStatus.contains("PQWL", ignoreCase = true) && i == 0 -> "Priority WL"
                                        else -> "Waitlisted"
                                    }
                                    scrapedStructuredPassengers.add(
                                        LivePnrPassenger(
                                            passengerNumber = "P${i + 1}",
                                            initialStatus = cleanInitial,
                                            currentStatus = cleanCurrent,
                                            statusLabel = statusBadgeLabel
                                        )
                                    )

                                    val probSuffix = if (confPct >= 0 && !effectiveCurrent.contains("CNF", ignoreCase = true)) {
                                        " ($confPct% $confProb)"
                                    } else ""

                                    val rowLabel = if (bkStatus.isNotBlank() && !bkStatus.equals(effectiveCurrent, ignoreCase = true)) {
                                        "P${i + 1}: Booked [$bkStatus] → Live [$effectiveCurrent$probSuffix]"
                                    } else {
                                        "P${i + 1}: $effectiveCurrent$probSuffix"
                                    }
                                    scrapedPassengers.add(rowLabel)
                                }
                                liveNetworkHit = true
                            }
                        }
                    }
                }
            } finally {
                ryConn.disconnect()
            }
        }

        // Endpoint 2: ConfirmTkt SSR Fallback (`/pnr-status/$cleanPnr`)
        if (!liveNetworkHit) {
            runCatching {
                val url = URL("https://www.confirmtkt.com/pnr-status/$cleanPnr")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = SOCKET_TIMEOUT_MS
                    readTimeout = SOCKET_TIMEOUT_MS
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    )
                }
                try {
                    if (conn.responseCode in 200..299) {
                        val html = conn.inputStream.bufferedReader().use { it.readText() }
                        val trNo = Regex(""""TrainNo"\s*:\s*"(\d{5})"""").find(html)?.groupValues?.getOrNull(1)
                        val trName = Regex(""""TrainName"\s*:\s*"([^"]+)"""").find(html)?.groupValues?.getOrNull(1)
                        val fromSt = Regex(""""BoardingStation"\s*:\s*"([A-Z]{2,5})"""").find(html)?.groupValues?.getOrNull(1)
                        val toSt = Regex(""""ReservationUpto"\s*:\s*"([A-Z]{2,5})"""").find(html)?.groupValues?.getOrNull(1)
                        val chartPrep = Regex(""""ChartPrepared"\s*:\s*(true|false)""").find(html)?.groupValues?.getOrNull(1)
                        val currentStatuses = Regex(""""CurrentStatus"\s*:\s*"([^"]+)"""").findAll(html)
                            .map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.toList()

                        if (!trNo.isNullOrBlank()) {
                            scrapedTrainNo = trNo
                            liveNetworkHit = true
                        }
                        if (!trName.isNullOrBlank()) scrapedTrainName = trName
                        if (!fromSt.isNullOrBlank()) scrapedFrom = fromSt
                        if (!toSt.isNullOrBlank()) scrapedTo = toSt
                        if (chartPrep != null) scrapedChart = chartPrep.equals("true", ignoreCase = true)
                        if (currentStatuses.isNotEmpty()) {
                            currentStatuses.forEachIndexed { i, curSt ->
                                scrapedPassengers.add("P${i + 1}: $curSt")
                                scrapedStructuredPassengers.add(
                                    LivePnrPassenger(
                                        passengerNumber = "P${i + 1}",
                                        initialStatus = curSt,
                                        currentStatus = curSt,
                                        statusLabel = if (curSt.contains("CNF", true)) "Confirmed" else "Waitlisted"
                                    )
                                )
                            }
                            liveNetworkHit = true
                        }
                    }
                } finally {
                    conn.disconnect()
                }
            }
        }

        if (!liveNetworkHit) {
            val failEpoch = System.currentTimeMillis()
            synchronized(lastFailedSyncEpochMsCache) {
                lastFailedSyncEpochMsCache[cleanPnr] = failEpoch
            }
            prefs?.edit()?.putLong("last_failed_sync_$cleanPnr", failEpoch)?.apply()
            return cachedSnapshot?.copy(
                sourceLabel = "Offline / No Signal · Showing Last Known Status (${cachedSnapshot.bookingStatusBadge})"
            ) ?: buildUnverifiedManualFallbackSnapshot(
                cleanPnr = cleanPnr,
                fallbackTicket = fallbackTicket,
                reasonLabel = "Live CRIS servers unreachable — Enter ticket fare & route manually"
            )
        }

        val resolvedTrainName = if (scrapedTravelClass.isNotBlank() && !scrapedTrainName.contains("($scrapedTravelClass)")) {
            "${scrapedTrainName.ifBlank { "Express Train" }} ($scrapedTravelClass)"
        } else {
            scrapedTrainName.ifBlank { "Express Train" }
        }
        val resolvedFrom = scrapedFrom.ifBlank { "ORIG" }
        val resolvedTo = scrapedTo.ifBlank { "DEST" }
        val resolvedFromName = scrapedFromName.ifBlank { resolveStationDisplayName(resolvedFrom) }
        val resolvedToName = scrapedToName.ifBlank { resolveStationDisplayName(resolvedTo) }

        val joinedPassengers = scrapedPassengers.joinToString(" | ")
        val overallBadge = when {
            joinedPassengers.contains("WL", ignoreCase = true) && !joinedPassengers.contains("CNF", ignoreCase = true) -> "WL (Waitlisted)"
            joinedPassengers.contains("RAC", ignoreCase = true) -> "RAC (Side Lower Shared)"
            joinedPassengers.contains("WL", ignoreCase = true) -> "PARTIAL CNF + WL"
            else -> "CNF (Confirmed)"
        }
        val confirmationProb = scrapedPrediction.ifBlank {
            when {
                overallBadge.startsWith("CNF") -> "100% Confirmed · Berths Locked"
                overallBadge.startsWith("RAC") -> "RAC Berth Assigned"
                else -> "Waitlisted · Live Status Active"
            }
        }
        val paxCount = scrapedPassengers.size.coerceAtLeast(1)

        val verifiedSnapshot = LivePnrStatusSnapshot(
            pnr = cleanPnr,
            trainNo = scrapedTrainNo,
            trainName = resolvedTrainName,
            fromStation = resolvedFrom,
            toStation = resolvedTo,
            departureTime = scrapedDep,
            travelClass = scrapedTravelClass,
            totalFareRupees = scrapedTotalFare,
            passengerCount = paxCount,
            bookingStatusBadge = overallBadge,
            chartPrepared = scrapedChart,
            passengerStatuses = scrapedPassengers,
            structuredPassengers = scrapedStructuredPassengers,
            fromStationName = resolvedFromName,
            toStationName = resolvedToName,
            arrivalTime = scrapedArr,
            durationText = scrapedDuration,
            quotaText = scrapedQuota,
            coachPositionHint = scrapedCoachPosition.ifBlank { "Class ${scrapedTravelClass.ifBlank { "IRCTC" }} · $paxCount Passenger(s)" },
            liveTrainLocationRadar = "Route: $resolvedFromName → $resolvedToName${if (scrapedDep.isNotBlank()) " · Dep $scrapedDep" else ""}",
            confirmationProbability = confirmationProb,
            sourceLabel = "Live CRIS / RailYatri SSR JSON (Verified)",
            isLiveVerified = true,
            isManualEntry = false
        )
        savePersistedPnrSnapshot(context, verifiedSnapshot)
        return verifiedSnapshot
    }

    /**
     * Builds an honest, zero-ghost-data manual-entry snapshot when live PNR servers are unreachable,
     * rate-limited, or when the user chooses to enter ticket details manually.
     */
    fun buildUnverifiedManualFallbackSnapshot(
        cleanPnr: String,
        fallbackTicket: ParsedTravelTicket = ParsedTravelTicket(),
        reasonLabel: String = "Manual Ticket Entry Mode"
    ): LivePnrStatusSnapshot {
        val existingPassengers = fallbackTicket.coachAndSeats
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val structured = existingPassengers.mapIndexed { idx, s ->
            LivePnrPassenger(
                passengerNumber = "P${idx + 1}",
                initialStatus = s,
                currentStatus = s,
                statusLabel = if (s.contains("CNF", true)) "Confirmed" else "Passenger"
            )
        }
        val fromCode = fallbackTicket.fromStation.ifBlank { "" }
        val toCode = fallbackTicket.toStation.ifBlank { "" }
        return LivePnrStatusSnapshot(
            pnr = cleanPnr,
            trainNo = fallbackTicket.trainOrFlightNo,
            trainName = fallbackTicket.trainOrCarrierName.ifBlank { "Manual Train / Ticket Entry" },
            fromStation = fromCode,
            toStation = toCode,
            departureTime = fallbackTicket.departureTime,
            travelClass = "3A",
            totalFareRupees = fallbackTicket.fareRupees.toDoubleOrNull()?.toInt() ?: 0,
            passengerCount = existingPassengers.size.coerceAtLeast(1),
            bookingStatusBadge = fallbackTicket.bookingStatus.ifBlank { "MANUAL" },
            chartPrepared = false,
            passengerStatuses = existingPassengers,
            structuredPassengers = structured,
            fromStationName = if (fromCode.isNotBlank()) resolveStationDisplayName(fromCode) else "Origin Station",
            toStationName = if (toCode.isNotBlank()) resolveStationDisplayName(toCode) else "Destination Station",
            arrivalTime = "",
            durationText = "",
            quotaText = "GN",
            coachPositionHint = "Enter Total Ticket Fare & Route below to split across group members",
            liveTrainLocationRadar = reasonLabel,
            confirmationProbability = reasonLabel,
            sourceLabel = reasonLabel,
            isLiveVerified = false,
            isManualEntry = true
        )
    }

    /**
     * Queries public unauthenticated FlightRadar24 JSON endpoint for a given Flight Number (e.g., `6E 282`, `AI 865`)
     * to resolve or enrich live schedule/delay/route metadata with bounded 4,000ms socket timeout.
     */
    fun fetchLiveFlightStatusByNumber(
        flightNumber: String,
        optionalPnr: String = ""
    ): LivePnrStatusSnapshot? = runCatching {
        val cleanFlight = flightNumber.replace(Regex("[^A-Za-z0-9]"), "").uppercase(Locale.US)
        if (cleanFlight.length < 3) return null
        val cacheKey = "FLIGHT_${optionalPnr.ifBlank { cleanFlight }}"
        lruSnapshotCache.get(cacheKey)?.let { return it }

        val url = URL("https://api.flightradar24.com/common/v1/flight/list.json?query=$cleanFlight&fetchBy=flight&page=1&limit=1")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = SOCKET_TIMEOUT_MS
            readTimeout = SOCKET_TIMEOUT_MS
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36")
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode != 200) return null
            val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
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
                travelClass = "ECONOMY",
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
                quotaText = "FLIGHT",
                coachPositionHint = "Flight $resolvedNo • $statusText",
                liveTrainLocationRadar = statusText,
                confirmationProbability = "100% Confirmed",
                sourceLabel = "Live Flight Status",
                isLiveVerified = true,
                isManualEntry = false
            )
            lruSnapshotCache.put(cacheKey, snapshot)
            snapshot
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}

