package com.splitmate.app.data

import android.content.Context
import com.splitmate.app.SplitMateMathEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

data class CloudUserProfileRecord(
    val phone10: String,
    val name: String,
    val handle: String,
    val upiVpa: String,
    val avatarStyle: String,
    val avatarColorPreset: String,
    val pinHash: String,
    val updatedAtEpochMs: Long,
    val avatarSeed: String = ""
)

data class CloudPhoneGroupIndexEntry(
    val groupId: String,
    val groupName: String,
    val inviterName: String,
    val inviterPhone: String,
    val inviteStatus: String,
    val updatedAtEpochMs: Long
)

data class CloudJoinCodeIndexEntry(
    val code6: String,
    val groupId: String,
    val groupName: String,
    val organizerName: String,
    val organizerPhone10: String,
    val updatedAtEpochMs: Long
)

typealias CloudGroupCodePointer = CloudJoinCodeIndexEntry

data class CloudGroupLedgerDocument(
    val group: ExpenseGroupEntity,
    val members: List<GroupMemberEntity>,
    val expenses: List<ExpenseEntity>,
    val splits: List<ExpenseSplitEntity>,
    val settlements: List<SettlementEntity>,
    val deletedExpenseIds: Map<String, Long> = emptyMap(),
    val flightVaultByPnr: Map<String, String> = emptyMap(),
    val trainSnapshotByPnr: Map<String, String> = emptyMap(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val memberPresenceByPhone: Map<String, Long> = emptyMap(),
    val organizerPhone10: String = "",
    val joinCode6: String = "",
    val deletedMemberIds: Map<String, Long> = emptyMap(),
    val removedMemberPhones: Map<String, Long> = emptyMap(),
    val deletedSettlementIds: Map<String, Long> = emptyMap(),
    val organizerRolesByKey: Map<String, Long> = emptyMap(),
    /** v2.3.5 (#5): group-shared custom categories; omitted from JSON + hash when empty. */
    val customCategories: List<CloudCustomCategory> = emptyList(),
    /** v2.3.5 (#1): trip lifecycle with its own LWW clock; omitted from JSON + hash when null. */
    val tripLifecycle: TripLifecycleRecord? = null,
    /**
     * v2.4.0 P4: compact edit history (newest 5 per expense, max 300), member ids only. Omitted from
     * JSON when empty and never part of the structural hash, so it rides along with real changes.
     */
    val revisions: List<ExpenseRevisionEntity> = emptyList()
)

data class CloudRestoreSummary(
    val restoredJoinedGroupsCount: Int,
    val discoveredPendingInvitesCount: Int,
    val memberPresenceByPhone: Map<String, Long> = emptyMap()
)

data class LocalTombstoneStore(
    val deletedExpenseIds: Map<String, Long> = emptyMap(),
    val deletedMemberIds: Map<String, Long> = emptyMap(),
    val removedMemberPhones: Map<String, Long> = emptyMap(),
    val deletedSettlementIds: Map<String, Long> = emptyMap(),
    val organizerPhone10: String = "",
    val joinCode6: String = "",
    val organizerRolesByKey: Map<String, Long> = emptyMap()
)

object CloudGroupSyncRepository {

    const val ONLINE_PRESENCE_TTL_MS: Long = 90_000L
    const val CODE_POINTER_REFRESH_INTERVAL_MS: Long = 2L * 60L * 60L * 1000L

    /**
     * Public ntfy.sh only caches messages for ~12 hours. Discovery records (the per-phone group
     * index `splitmate_v2_idx_<phone10>` and the user profile `splitmate_v2_u_<phone10>`) are
     * re-published at half that window so a fresh install / new phone can always restore trips.
     */
    const val NTFY_CACHE_WINDOW_MS: Long = 12L * 60L * 60L * 1000L
    const val DISCOVERY_REFRESH_INTERVAL_MS: Long = NTFY_CACHE_WINDOW_MS / 2L
    const val PROFILE_REFRESH_CHECK_INTERVAL_MS: Long = 60L * 60L * 1000L

    enum class IndexPushDecision { SKIP, SEED_TOKEN_ONLY, PUSH }

    /**
     * Decides whether a phone-index entry must be (re)published. Pushes on status change when
     * network pushes are allowed, and ALWAYS when the last successful push is older than
     * [DISCOVERY_REFRESH_INTERVAL_MS] (it may have expired from the ntfy cache).
     */
    fun decidePhoneIndexPush(
        nowMs: Long,
        lastPushedEpochMs: Long,
        statusChanged: Boolean,
        forcePush: Boolean,
        networkPushAllowed: Boolean
    ): IndexPushDecision {
        val stale = lastPushedEpochMs <= 0L || (nowMs - lastPushedEpochMs) >= DISCOVERY_REFRESH_INTERVAL_MS
        return when {
            forcePush || stale -> IndexPushDecision.PUSH
            statusChanged && networkPushAllowed -> IndexPushDecision.PUSH
            statusChanged -> IndexPushDecision.SEED_TOKEN_ONLY
            else -> IndexPushDecision.SKIP
        }
    }

    /** A profile is re-published only when the cloud GET succeeded and returned no record. */
    fun shouldRepublishProfile(remoteFetchSucceeded: Boolean, remoteProfileExists: Boolean): Boolean =
        remoteFetchSucceeded && !remoteProfileExists

    /** Extracts (styleId, presetId) from a canonical `seed|gender|style|preset` avatar seed. */
    fun parseAvatarStyleAndPreset(avatarSeed: String): Pair<String, String> {
        val parts = avatarSeed.split("|")
        val style = parts.getOrNull(2)?.trim().orEmpty().ifBlank { "open-peeps" }
        val preset = parts.getOrNull(3)?.trim().orEmpty().ifBlank { "Buckwheat" }
        return style to preset
    }

    private const val TOMBSTONE_PREFS_NAME = "splitmate_v2_tombstones"
    private const val SYNC_META_PREFS_NAME = "splitmate_v2_sync_meta"
    private const val CROCKFORD_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"

    private val syncMutex = Mutex()

    @Volatile
    private var appContext: Context? = null

    private val inMemoryTombstoneStore = ConcurrentHashMap<String, LocalTombstoneStore>()
    private val lastPushedGroupHashByGroup = ConcurrentHashMap<String, String>()
    private val lastPushedIndexStatusByGroupPhone = ConcurrentHashMap<String, String>()
    private val lastCodePublishEpochByGroup = ConcurrentHashMap<String, Long>()
    private val lastPushedIndexEpochByGroupPhone = ConcurrentHashMap<String, Long>()
    @Volatile private var lastProfileRefreshCheckEpochMs: Long = 0L
    private val lastLocalMutationEpochByGroup = ConcurrentHashMap<String, Long>()
    private val pendingCloudPushByGroup = ConcurrentHashMap<String, Boolean>()

    /** v2.3.6 P3: when this phone last confirmed its copy equals the group's cloud copy (this session). */
    private val lastConfirmedInSyncEpochByGroup = ConcurrentHashMap<String, Long>()

    /** v2.3.6 P3 Money check footer: epoch ms of the last confirmed match with the group, or 0. */
    fun lastConfirmedInSyncEpoch(groupId: String): Long = lastConfirmedInSyncEpochByGroup[groupId] ?: 0L

    fun init(context: Context?) {
        if (context != null) {
            appContext = context.applicationContext ?: context
            GroupLedgerExtrasStore.init(appContext)
        }
    }

    private fun resolveContext(context: Context?): Context? {
        if (context != null) {
            init(context)
            return appContext ?: context
        }
        return appContext
    }

    fun resetInMemoryStateForTests() {
        inMemoryTombstoneStore.clear()
        lastPushedGroupHashByGroup.clear()
        lastPushedIndexStatusByGroupPhone.clear()
        lastCodePublishEpochByGroup.clear()
        lastLocalMutationEpochByGroup.clear()
        pendingCloudPushByGroup.clear()
        GroupLedgerExtrasStore.resetForTests()
    }

    fun isPhoneOnlineNow(
        phone10: String,
        presenceMap: Map<String, Long>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Boolean {
        val norm = PhoneIdentityValidator.normalizeIndianPhone10(phone10)
        if (norm.length != 10) return false
        val lastSeen = presenceMap[norm] ?: return false
        return lastSeen > 0L && (nowEpochMs - lastSeen) <= ONLINE_PRESENCE_TTL_MS
    }

    private fun sanitizeTopicKey(key: String): String {
        return key.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    // =========================================================================
    // 6-CHARACTER CROCKFORD BASE32 GROUP JOIN CODE ENGINE (PA-7)
    // =========================================================================

    fun deriveGroupJoinCode6(groupId: String, createdAtEpochMs: Long = 0L): String {
        val trimmed = groupId.trim()
        if (trimmed.isBlank()) return "234567"
        val suffix = trimmed.substringAfterLast("_", "")
        if (suffix.length == 6 && suffix.all { it in CROCKFORD_ALPHABET }) {
            return suffix
        }
        val seedInput = if (createdAtEpochMs > 0L && !trimmed.contains(createdAtEpochMs.toString())) {
            "$trimmed|$createdAtEpochMs"
        } else {
            trimmed
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(seedInput.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(6)
        for (i in 0 until 6) {
            val idx = (digest[i].toInt() and 0xFF) % CROCKFORD_ALPHABET.length
            sb.append(CROCKFORD_ALPHABET[idx])
        }
        return sb.toString()
    }

    fun formatJoinCode6(code6: String): String {
        val norm = normalizeJoinCode6(code6).ifBlank { deriveGroupJoinCode6(code6) }
        return if (norm.length == 6) "${norm.substring(0, 3)}-${norm.substring(3, 6)}" else norm
    }

    fun normalizeJoinCode6(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return ""
        val candidate = if (trimmed.contains("code=", ignoreCase = true)) {
            trimmed.substringAfter("code=", "").substringBefore("&").substringBefore(" ")
        } else {
            trimmed
        }
        val cleaned = candidate.uppercase()
            .replace("-", "")
            .replace(" ", "")
            .filter { it in CROCKFORD_ALPHABET }
        return if (cleaned.length == 6) cleaned else ""
    }

    fun deriveGroupJoinCode(groupId: String, createdAtEpochMs: Long = 0L): String =
        deriveGroupJoinCode6(groupId, createdAtEpochMs)

    fun normalizeJoinCode(rawInput: String): String =
        normalizeJoinCode6(rawInput)


    // =========================================================================
    // DURABLE LOCAL TOMBSTONE STORE (PA-3 & PA-4)
    // =========================================================================

    fun loadLocalTombstones(context: Context?, groupId: String): LocalTombstoneStore {
        if (groupId.isBlank()) return LocalTombstoneStore()
        val mem = inMemoryTombstoneStore[groupId]
        val ctx = resolveContext(context) ?: return (mem ?: LocalTombstoneStore())
        return try {
            val prefs = ctx.getSharedPreferences(TOMBSTONE_PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString("tomb_$groupId", null)
            if (raw.isNullOrBlank()) {
                mem ?: LocalTombstoneStore()
            } else {
                val parsed = decodeLocalTombstoneStore(raw)
                val merged = mergeLocalTombstoneStores(mem ?: LocalTombstoneStore(), parsed)
                inMemoryTombstoneStore[groupId] = merged
                merged
            }
        } catch (_: Exception) {
            mem ?: LocalTombstoneStore()
        }
    }

    fun recordLocalTombstones(
        context: Context?,
        groupId: String,
        deletedExpenseIds: Map<String, Long> = emptyMap(),
        deletedMemberIds: Map<String, Long> = emptyMap(),
        removedMemberPhones: Map<String, Long> = emptyMap(),
        deletedSettlementIds: Map<String, Long> = emptyMap(),
        organizerPhone10: String = "",
        joinCode6: String = "",
        organizerRolesByKey: Map<String, Long> = emptyMap()
    ): LocalTombstoneStore {
        if (groupId.isBlank()) return LocalTombstoneStore()
        val current = loadLocalTombstones(context, groupId)
        val normalizedRemovedPhones = mutableMapOf<String, Long>()
        removedMemberPhones.forEach { (phone, ts) ->
            val norm = PhoneIdentityValidator.normalizeIndianPhone10(phone)
            if (norm.length == 10) {
                normalizedRemovedPhones[norm] = max(normalizedRemovedPhones[norm] ?: 0L, ts)
            }
        }
        val incoming = LocalTombstoneStore(
            deletedExpenseIds = deletedExpenseIds,
            deletedMemberIds = deletedMemberIds,
            removedMemberPhones = normalizedRemovedPhones,
            deletedSettlementIds = deletedSettlementIds,
            organizerPhone10 = PhoneIdentityValidator.normalizeIndianPhone10(organizerPhone10),
            joinCode6 = normalizeJoinCode6(joinCode6),
            organizerRolesByKey = organizerRolesByKey
        )
        val merged = mergeLocalTombstoneStores(current, incoming)
        saveLocalTombstones(context, groupId, merged)
        return merged
    }

    fun setMemberOrganizerRole(
        context: Context?,
        groupId: String,
        memberId: String,
        memberPhone10: String,
        isOrganizer: Boolean,
        timestampMs: Long = System.currentTimeMillis()
    ) {
        if (groupId.isBlank()) return
        val absEpoch = kotlin.math.abs(timestampMs).coerceAtLeast(1L)
        val signedEpoch = if (isOrganizer) absEpoch else -absEpoch
        val updates = mutableMapOf<String, Long>()
        if (memberId.isNotBlank()) {
            updates[memberId] = signedEpoch
        }
        val normPhone = PhoneIdentityValidator.normalizeIndianPhone10(memberPhone10)
        if (normPhone.length == 10) {
            updates[normPhone] = signedEpoch
        }
        if (updates.isNotEmpty()) {
            recordLocalTombstones(
                context = context,
                groupId = groupId,
                organizerRolesByKey = updates
            )
            setLastLocalMutationEpoch(context, groupId, absEpoch)
            setGroupPendingCloudPush(context, groupId, true)
        }
    }

    fun getOrganizerRolesByKey(context: Context?, groupId: String): Map<String, Long> {
        if (groupId.isBlank()) return emptyMap()
        return loadLocalTombstones(context, groupId).organizerRolesByKey
    }

    fun mergeOrganizerRoleMaps(vararg maps: Map<String, Long>): Map<String, Long> {
        val merged = mutableMapOf<String, Long>()
        for (m in maps) {
            for ((k, v) in m) {
                if (k.isBlank() || v == 0L) continue
                val existing = merged[k]
                if (existing == null || kotlin.math.abs(v) >= kotlin.math.abs(existing)) {
                    merged[k] = v
                }
            }
        }
        return merged
    }

    fun unTombstonePhones(
        context: Context?,
        groupId: String,
        phonesToRestore: Collection<String>,
        memberIdsToRestore: Collection<String> = emptyList()
    ) {
        if (groupId.isBlank()) return
        val current = loadLocalTombstones(context, groupId)
        val normPhones = phonesToRestore
            .map { PhoneIdentityValidator.normalizeIndianPhone10(it) }
            .filter { it.length == 10 }
            .toSet()
        val memberIdSet = memberIdsToRestore.filter { it.isNotBlank() }.toSet()
        if (normPhones.isEmpty() && memberIdSet.isEmpty()) return
        val updated = current.copy(
            removedMemberPhones = current.removedMemberPhones.filterKeys { it !in normPhones },
            deletedMemberIds = current.deletedMemberIds.filterKeys { it !in memberIdSet }
        )
        saveLocalTombstones(context, groupId, updated)
    }

    fun setOrganizerPhone10(context: Context?, groupId: String, organizerPhone10: String) {
        val norm = PhoneIdentityValidator.normalizeIndianPhone10(organizerPhone10)
        if (groupId.isBlank() || norm.length != 10) return
        recordLocalTombstones(context = context, groupId = groupId, organizerPhone10 = norm)
    }

    fun setOrganizerPhone10(groupId: String, organizerPhone10: String) =
        setOrganizerPhone10(null, groupId, organizerPhone10)

    fun getOrganizerPhone10(context: Context?, groupId: String): String {
        if (groupId.isBlank()) return ""
        return loadLocalTombstones(context, groupId).organizerPhone10
    }

    fun getOrganizerPhone10(groupId: String): String =
        getOrganizerPhone10(null, groupId)

    fun isPhoneRemovedFromGroup(context: Context?, groupId: String, phone10: String): Boolean {
        val norm = PhoneIdentityValidator.normalizeIndianPhone10(phone10)
        if (groupId.isBlank() || norm.length != 10) return false
        return loadLocalTombstones(context, groupId).removedMemberPhones.containsKey(norm)
    }

    fun isPhoneRemovedFromGroup(groupId: String, phone10: String): Boolean =
        isPhoneRemovedFromGroup(null, groupId, phone10)

    private fun saveLocalTombstones(context: Context?, groupId: String, store: LocalTombstoneStore) {
        inMemoryTombstoneStore[groupId] = store
        val ctx = resolveContext(context) ?: return
        try {
            val prefs = ctx.getSharedPreferences(TOMBSTONE_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("tomb_$groupId", encodeLocalTombstoneStore(store)).apply()
        } catch (_: Exception) {
        }
    }

    private fun mergeLocalTombstoneStores(a: LocalTombstoneStore, b: LocalTombstoneStore): LocalTombstoneStore {
        val exp = mutableMapOf<String, Long>()
        (a.deletedExpenseIds.keys + b.deletedExpenseIds.keys).forEach { k ->
            exp[k] = max(a.deletedExpenseIds[k] ?: 0L, b.deletedExpenseIds[k] ?: 0L)
        }
        val mem = mutableMapOf<String, Long>()
        (a.deletedMemberIds.keys + b.deletedMemberIds.keys).forEach { k ->
            mem[k] = max(a.deletedMemberIds[k] ?: 0L, b.deletedMemberIds[k] ?: 0L)
        }
        val phones = mutableMapOf<String, Long>()
        (a.removedMemberPhones.keys + b.removedMemberPhones.keys).forEach { k ->
            val norm = PhoneIdentityValidator.normalizeIndianPhone10(k)
            if (norm.length == 10) {
                phones[norm] = max(a.removedMemberPhones[k] ?: 0L, b.removedMemberPhones[k] ?: 0L)
            }
        }
        val setts = mutableMapOf<String, Long>()
        (a.deletedSettlementIds.keys + b.deletedSettlementIds.keys).forEach { k ->
            setts[k] = max(a.deletedSettlementIds[k] ?: 0L, b.deletedSettlementIds[k] ?: 0L)
        }
        val orgRoles = mergeOrganizerRoleMaps(a.organizerRolesByKey, b.organizerRolesByKey)
        return LocalTombstoneStore(
            deletedExpenseIds = exp,
            deletedMemberIds = mem,
            removedMemberPhones = phones,
            deletedSettlementIds = setts,
            organizerPhone10 = b.organizerPhone10.ifBlank { a.organizerPhone10 },
            joinCode6 = b.joinCode6.ifBlank { a.joinCode6 },
            organizerRolesByKey = orgRoles
        )
    }

    private fun encodeLocalTombstoneStore(store: LocalTombstoneStore): String {
        return JSONObject().apply {
            put("organizerPhone10", store.organizerPhone10)
            put("joinCode6", store.joinCode6)
            put("deletedExpenseIds", JSONObject().apply {
                store.deletedExpenseIds.forEach { (k, v) -> put(k, v) }
            })
            put("deletedMemberIds", JSONObject().apply {
                store.deletedMemberIds.forEach { (k, v) -> put(k, v) }
            })
            put("removedMemberPhones", JSONObject().apply {
                store.removedMemberPhones.forEach { (k, v) -> put(k, v) }
            })
            put("deletedSettlementIds", JSONObject().apply {
                store.deletedSettlementIds.forEach { (k, v) -> put(k, v) }
            })
            put("organizerRolesByKey", JSONObject().apply {
                store.organizerRolesByKey.forEach { (k, v) -> put(k, v) }
            })
        }.toString()
    }

    private fun decodeLocalTombstoneStore(jsonStr: String): LocalTombstoneStore {
        return try {
            val root = JSONObject(jsonStr)
            LocalTombstoneStore(
                deletedExpenseIds = parseLongMap(root.optJSONObject("deletedExpenseIds")),
                deletedMemberIds = parseLongMap(root.optJSONObject("deletedMemberIds")),
                removedMemberPhones = parseLongMap(root.optJSONObject("removedMemberPhones"), normalizePhones = true),
                deletedSettlementIds = parseLongMap(root.optJSONObject("deletedSettlementIds")),
                organizerPhone10 = PhoneIdentityValidator.normalizeIndianPhone10(root.optString("organizerPhone10", "")),
                joinCode6 = normalizeJoinCode6(root.optString("joinCode6", "")),
                organizerRolesByKey = parseSignedLongMap(root.optJSONObject("organizerRolesByKey"))
            )
        } catch (_: Exception) {
            LocalTombstoneStore()
        }
    }

    private fun parseSignedLongMap(obj: JSONObject?): Map<String, Long> {
        if (obj == null) return emptyMap()
        val result = mutableMapOf<String, Long>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next().trim()
            if (key.isNotBlank()) {
                val v = obj.optLong(key, 0L)
                if (v != 0L) {
                    result[key] = v
                }
            }
        }
        return result
    }

    private fun parseLongMap(obj: JSONObject?, normalizePhones: Boolean = false): Map<String, Long> {
        if (obj == null) return emptyMap()
        val result = mutableMapOf<String, Long>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val rawKey = keys.next()
            val key = if (normalizePhones) {
                PhoneIdentityValidator.normalizeIndianPhone10(rawKey)
            } else {
                rawKey
            }
            if (key.isNotBlank()) {
                result[key] = max(result[key] ?: 0L, obj.optLong(rawKey, 0L))
            }
        }
        return result
    }

    // =========================================================================
    // ZERO-DEPENDENCY JSON SERIALIZATION (`org.json.JSONObject` / `JSONArray`)
    // =========================================================================

    fun encodeUserProfileRecord(record: CloudUserProfileRecord): String {
        return JSONObject().apply {
            put("phone10", record.phone10)
            put("name", record.name)
            put("handle", record.handle)
            put("upiVpa", record.upiVpa)
            put("avatarStyle", record.avatarStyle)
            put("avatarColorPreset", record.avatarColorPreset)
            put("pinHash", record.pinHash)
            put("updatedAtEpochMs", record.updatedAtEpochMs)
            put("avatarSeed", record.avatarSeed)
        }.toString()
    }

    fun decodeUserProfileRecord(jsonStr: String): CloudUserProfileRecord? {
        return try {
            val obj = JSONObject(jsonStr)
            val phone10 = obj.optString("phone10", "")
            if (phone10.isBlank()) return null
            CloudUserProfileRecord(
                phone10 = phone10,
                name = obj.optString("name", ""),
                handle = obj.optString("handle", ""),
                upiVpa = obj.optString("upiVpa", ""),
                avatarStyle = obj.optString("avatarStyle", "open-peeps"),
                avatarColorPreset = obj.optString("avatarColorPreset", "Buckwheat"),
                pinHash = obj.optString("pinHash", ""),
                updatedAtEpochMs = obj.optLong("updatedAtEpochMs", 0L),
                avatarSeed = obj.optString("avatarSeed", "")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun encodePhoneIndexEntry(entry: CloudPhoneGroupIndexEntry): String {
        return JSONObject().apply {
            put("groupId", entry.groupId)
            put("groupName", entry.groupName)
            put("inviterName", entry.inviterName)
            put("inviterPhone", entry.inviterPhone)
            put("inviteStatus", entry.inviteStatus)
            put("updatedAtEpochMs", entry.updatedAtEpochMs)
        }.toString()
    }

    fun decodePhoneIndexEntry(jsonStr: String): CloudPhoneGroupIndexEntry? {
        return try {
            val obj = JSONObject(jsonStr)
            val groupId = obj.optString("groupId", "")
            if (groupId.isBlank()) return null
            CloudPhoneGroupIndexEntry(
                groupId = groupId,
                groupName = obj.optString("groupName", ""),
                inviterName = obj.optString("inviterName", ""),
                inviterPhone = obj.optString("inviterPhone", ""),
                inviteStatus = obj.optString("inviteStatus", "PENDING"),
                updatedAtEpochMs = obj.optLong("updatedAtEpochMs", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun encodeJoinCodeIndexEntry(entry: CloudJoinCodeIndexEntry): String {
        return JSONObject().apply {
            put("code6", normalizeJoinCode6(entry.code6).ifBlank { deriveGroupJoinCode6(entry.groupId) })
            put("groupId", entry.groupId)
            put("groupName", entry.groupName)
            put("organizerName", entry.organizerName)
            put("organizerPhone10", PhoneIdentityValidator.normalizeIndianPhone10(entry.organizerPhone10))
            put("updatedAtEpochMs", entry.updatedAtEpochMs)
        }.toString()
    }

    fun decodeJoinCodeIndexEntry(jsonStr: String): CloudJoinCodeIndexEntry? {
        return try {
            val obj = JSONObject(jsonStr)
            val groupId = obj.optString("groupId", "")
            if (groupId.isBlank()) return null
            val rawCode = obj.optString("code6", "")
            val code6 = normalizeJoinCode6(rawCode).ifBlank { deriveGroupJoinCode6(groupId) }
            CloudJoinCodeIndexEntry(
                code6 = code6,
                groupId = groupId,
                groupName = obj.optString("groupName", "Shared Group"),
                organizerName = obj.optString("organizerName", ""),
                organizerPhone10 = PhoneIdentityValidator.normalizeIndianPhone10(obj.optString("organizerPhone10", "")),
                updatedAtEpochMs = obj.optLong("updatedAtEpochMs", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun encodeGroupLedgerDocument(doc: CloudGroupLedgerDocument): String {
        val root = JSONObject()
        val splitsByExpenseForEncode = doc.splits.groupBy { it.expenseId }
        root.put("updatedAtEpochMs", doc.updatedAtEpochMs)
        val resolvedOrganizerPhone = PhoneIdentityValidator.normalizeIndianPhone10(doc.organizerPhone10)
        val resolvedJoinCode = normalizeJoinCode6(doc.joinCode6).ifBlank { deriveGroupJoinCode6(doc.group.groupId) }
        root.put("organizerPhone10", resolvedOrganizerPhone)
        root.put("joinCode6", resolvedJoinCode)
        root.put("group", JSONObject().apply {
            put("groupId", doc.group.groupId)
            put("name", doc.group.name)
            put("currencyCode", doc.group.currencyCode)
            put("iconName", doc.group.iconName)
            put("isDemoSeed", doc.group.isDemoSeed)
            put("createdAt", doc.group.createdAt)
        })

        val membersArr = JSONArray()
        doc.members.forEach { m ->
            membersArr.put(JSONObject().apply {
                put("memberId", m.memberId)
                put("groupId", m.groupId)
                put("name", m.name)
                put("avatarSeed", m.avatarSeed)
                put("isCurrentUser", m.isCurrentUser)
                put("upiId", m.upiId)
                put("userPhone", m.userPhone)
                put("inviteStatus", m.inviteStatus)
            })
        }
        root.put("members", membersArr)

        val expensesArr = JSONArray()
        doc.expenses.forEach { e ->
            expensesArr.put(JSONObject().apply {
                put("expenseId", e.expenseId)
                put("groupId", e.groupId)
                put("title", e.title)
                put("payerId", e.payerId)
                put("baseSubtotalCents", e.baseSubtotalCents)
                put("taxCents", e.taxCents)
                put("tipCents", e.tipCents)
                put("totalAmountCents", e.totalAmountCents)
                put("lockedMultiplier", e.lockedMultiplier)
                put("unassignedBaseCents", e.unassignedBaseCents)
                put("currencyCode", e.currencyCode)
                put("lockedExchangeRate", e.lockedExchangeRate)
                put("expenseCategory", e.expenseCategory)
                put("travelPnr", e.travelPnr)
                put("providerName", e.providerName)
                if (e.scheduledAtEpochMs != null) {
                    put("scheduledAtEpochMs", e.scheduledAtEpochMs)
                }
                put("syncStatus", e.syncStatus)
                put("createdAt", e.createdAt)
                // v2.3.5: additive, omitted when null so legacy expense JSON is byte-identical.
                e.categoryRef?.takeIf { it.isNotBlank() }?.let { put("categoryRef", it) }
                e.createdByPhone?.takeIf { it.isNotBlank() }?.let { put("createdByPhone", it) }
                // v2.4.0 P1: per-expense version + editor (omitted on unversioned rows).
                if (e.rowVersion > 0L) put("rv", e.rowVersion)
                e.rowUpdatedBy?.takeIf { it.isNotBlank() }?.let { put("rby", it) }
                // v2.4.0 P2: quarantine flag. Never blocks the upload; the Money check shows it.
                if (!com.splitmate.app.ExpenseSplitIntegrity.isConsistent(e, splitsByExpenseForEncode[e.expenseId].orEmpty())) put("q", true)
            })
        }
        root.put("expenses", expensesArr)

        val splitsArr = JSONArray()
        doc.splits.forEach { s ->
            splitsArr.put(JSONObject().apply {
                put("splitId", s.splitId)
                put("expenseId", s.expenseId)
                put("memberId", s.memberId)
                put("baseClaimedCents", s.baseClaimedCents)
                put("finalOwedCents", s.finalOwedCents)
                put("plusOneCent", s.plusOneCent)
            })
        }
        root.put("splits", splitsArr)

        val settlementsArr = JSONArray()
        doc.settlements.forEach { st ->
            settlementsArr.put(JSONObject().apply {
                put("settlementId", st.settlementId)
                put("groupId", st.groupId)
                put("fromMemberId", st.fromMemberId)
                put("fromMemberName", st.fromMemberName)
                put("toMemberId", st.toMemberId)
                put("toMemberName", st.toMemberName)
                put("amountCents", st.amountCents)
                put("currencyCode", st.currencyCode)
                put("lockedExchangeRate", st.lockedExchangeRate)
                put("syncStatus", st.syncStatus)
                put("settledAt", st.settledAt)
            })
        }
        root.put("settlements", settlementsArr)

        val deletedObj = JSONObject()
        doc.deletedExpenseIds.forEach { (expId, ts) -> deletedObj.put(expId, ts) }
        root.put("deletedExpenseIds", deletedObj)

        val deletedMembersObj = JSONObject()
        doc.deletedMemberIds.forEach { (memId, ts) -> deletedMembersObj.put(memId, ts) }
        root.put("deletedMemberIds", deletedMembersObj)

        val removedPhonesObj = JSONObject()
        doc.removedMemberPhones.forEach { (phone, ts) ->
            val norm = PhoneIdentityValidator.normalizeIndianPhone10(phone)
            if (norm.length == 10) {
                removedPhonesObj.put(norm, ts)
            }
        }
        root.put("removedMemberPhones", removedPhonesObj)

        val deletedSettlementsObj = JSONObject()
        doc.deletedSettlementIds.forEach { (settleId, ts) -> deletedSettlementsObj.put(settleId, ts) }
        root.put("deletedSettlementIds", deletedSettlementsObj)

        val orgRolesObj = JSONObject()
        doc.organizerRolesByKey.forEach { (k, v) -> if (k.isNotBlank() && v != 0L) orgRolesObj.put(k, v) }
        root.put("organizerRolesByKey", orgRolesObj)

        val flightObj = JSONObject()
        doc.flightVaultByPnr.forEach { (pnr, json) -> flightObj.put(pnr, json) }
        root.put("flightVaultByPnr", flightObj)

        val trainObj = JSONObject()
        doc.trainSnapshotByPnr.forEach { (pnr, json) -> trainObj.put(pnr, json) }
        root.put("trainSnapshotByPnr", trainObj)

        val presenceObj = JSONObject()
        doc.memberPresenceByPhone.forEach { (phone10, ts) -> presenceObj.put(phone10, ts) }
        root.put("memberPresenceByPhone", presenceObj)

        // v2.3.5 additive keys: only written when non-empty / present.
        if (doc.customCategories.isNotEmpty()) {
            root.put(GroupLedgerExtrasCodec.KEY_CUSTOM_CATEGORIES, GroupLedgerExtrasCodec.encodeCustomCategories(doc.customCategories))
        }
        doc.tripLifecycle?.let {
            root.put(GroupLedgerExtrasCodec.KEY_TRIP_LIFECYCLE, GroupLedgerExtrasCodec.encodeTripLifecycle(it))
        }
        // v2.4.0 P4: additive; older apps ignore it.
        if (doc.revisions.isNotEmpty()) {
            root.put(ExpenseVersionSync.KEY_REVISIONS, ExpenseVersionSync.encodeRevisions(doc.revisions))
        }

        return root.toString()
    }

    fun encodeLedgerDocumentToJson(doc: CloudGroupLedgerDocument): String =
        encodeGroupLedgerDocument(doc)

    fun decodeLedgerDocumentFromJson(jsonStr: String): CloudGroupLedgerDocument? =
        decodeGroupLedgerDocument(jsonStr)

    fun decodeGroupLedgerDocument(jsonStr: String): CloudGroupLedgerDocument? {
        return try {
            val root = JSONObject(jsonStr)
            val grpObj = root.optJSONObject("group") ?: return null
            val groupId = grpObj.optString("groupId", "")
            if (groupId.isBlank()) return null

            val group = ExpenseGroupEntity(
                groupId = groupId,
                name = grpObj.optString("name", "Shared Group"),
                currencyCode = grpObj.optString("currencyCode", "INR"),
                iconName = grpObj.optString("iconName", "Flight"),
                isDemoSeed = grpObj.optBoolean("isDemoSeed", false),
                createdAt = grpObj.optLong("createdAt", System.currentTimeMillis())
            )

            val members = mutableListOf<GroupMemberEntity>()
            val membersArr = root.optJSONArray("members") ?: JSONArray()
            for (i in 0 until membersArr.length()) {
                val m = membersArr.optJSONObject(i) ?: continue
                val rawUpi = m.optString("upiId", "")
                val rawPhone = m.optString("userPhone", "")
                val extractedPhone = PhoneIdentityValidator.extractMemberPhone10(rawPhone, rawUpi)
                members.add(
                    GroupMemberEntity(
                        memberId = m.optString("memberId", ""),
                        groupId = m.optString("groupId", groupId),
                        name = m.optString("name", ""),
                        avatarSeed = m.optString("avatarSeed", ""),
                        isCurrentUser = m.optBoolean("isCurrentUser", false),
                        upiId = rawUpi,
                        userPhone = extractedPhone.ifBlank { rawPhone },
                        inviteStatus = m.optString("inviteStatus", "JOINED")
                    )
                )
            }

            val expenses = mutableListOf<ExpenseEntity>()
            val expensesArr = root.optJSONArray("expenses") ?: JSONArray()
            for (i in 0 until expensesArr.length()) {
                val e = expensesArr.optJSONObject(i) ?: continue
                val scheduledAt = if (e.has("scheduledAtEpochMs") && !e.isNull("scheduledAtEpochMs")) {
                    e.optLong("scheduledAtEpochMs")
                } else {
                    null
                }
                expenses.add(
                    ExpenseEntity(
                        expenseId = e.optString("expenseId", ""),
                        groupId = e.optString("groupId", groupId),
                        title = e.optString("title", ""),
                        payerId = e.optString("payerId", ""),
                        baseSubtotalCents = e.optLong("baseSubtotalCents", 0L),
                        taxCents = e.optLong("taxCents", 0L),
                        tipCents = e.optLong("tipCents", 0L),
                        totalAmountCents = e.optLong("totalAmountCents", 0L),
                        lockedMultiplier = e.optDouble("lockedMultiplier", 1.0),
                        unassignedBaseCents = e.optLong("unassignedBaseCents", 0L),
                        currencyCode = e.optString("currencyCode", "INR"),
                        lockedExchangeRate = e.optDouble("lockedExchangeRate", 1.0),
                        expenseCategory = e.optString("expenseCategory", "OTHER"),
                        travelPnr = e.optString("travelPnr", ""),
                        providerName = e.optString("providerName", ""),
                        scheduledAtEpochMs = scheduledAt,
                        syncStatus = e.optString("syncStatus", "SYNCED"),
                        createdAt = e.optLong("createdAt", System.currentTimeMillis()),
                        categoryRef = e.optString("categoryRef", "").trim().take(80).takeIf { it.isNotEmpty() },
                        createdByPhone = PhoneIdentityValidator.normalizeIndianPhone10(e.optString("createdByPhone", ""))
                            .takeIf { it.length == 10 },
                        // v2.4.0 P1: absent on apps older than v2.4.0 (reads as unversioned).
                        rowVersion = e.optLong("rv", 0L).coerceAtLeast(0L),
                        rowUpdatedBy = e.optString("rby", "").trim().take(80).takeIf { it.isNotEmpty() }
                    )
                )
            }

            val splits = mutableListOf<ExpenseSplitEntity>()
            val splitsArr = root.optJSONArray("splits") ?: JSONArray()
            for (i in 0 until splitsArr.length()) {
                val s = splitsArr.optJSONObject(i) ?: continue
                splits.add(
                    ExpenseSplitEntity(
                        splitId = s.optString("splitId", ""),
                        expenseId = s.optString("expenseId", ""),
                        memberId = s.optString("memberId", ""),
                        baseClaimedCents = s.optLong("baseClaimedCents", 0L),
                        finalOwedCents = s.optLong("finalOwedCents", 0L),
                        plusOneCent = s.optBoolean("plusOneCent", false)
                    )
                )
            }

            val settlements = mutableListOf<SettlementEntity>()
            val settlementsArr = root.optJSONArray("settlements") ?: JSONArray()
            for (i in 0 until settlementsArr.length()) {
                val st = settlementsArr.optJSONObject(i) ?: continue
                settlements.add(
                    SettlementEntity(
                        settlementId = st.optString("settlementId", ""),
                        groupId = st.optString("groupId", groupId),
                        fromMemberId = st.optString("fromMemberId", ""),
                        fromMemberName = st.optString("fromMemberName", ""),
                        toMemberId = st.optString("toMemberId", ""),
                        toMemberName = st.optString("toMemberName", ""),
                        amountCents = st.optLong("amountCents", 0L),
                        currencyCode = st.optString("currencyCode", "INR"),
                        lockedExchangeRate = st.optDouble("lockedExchangeRate", 1.0),
                        syncStatus = st.optString("syncStatus", "SYNCED"),
                        settledAt = st.optLong("settledAt", System.currentTimeMillis())
                    )
                )
            }

            val deletedExpenseIds = parseLongMap(root.optJSONObject("deletedExpenseIds"))
            val deletedMemberIds = parseLongMap(root.optJSONObject("deletedMemberIds"))
            val removedMemberPhones = parseLongMap(root.optJSONObject("removedMemberPhones"), normalizePhones = true)
            val deletedSettlementIds = parseLongMap(root.optJSONObject("deletedSettlementIds"))
            val organizerRolesByKey = parseSignedLongMap(root.optJSONObject("organizerRolesByKey"))

            val flightVaultByPnr = mutableMapOf<String, String>()
            val flightObj = root.optJSONObject("flightVaultByPnr")
            if (flightObj != null) {
                val keys = flightObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    flightVaultByPnr[k] = flightObj.optString(k, "")
                }
            }

            val trainSnapshotByPnr = mutableMapOf<String, String>()
            val trainObj = root.optJSONObject("trainSnapshotByPnr")
            if (trainObj != null) {
                val keys = trainObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    trainSnapshotByPnr[k] = trainObj.optString(k, "")
                }
            }

            val memberPresenceByPhone = parseLongMap(root.optJSONObject("memberPresenceByPhone"), normalizePhones = true)
            val rawOrganizerPhone = root.optString("organizerPhone10", "")
            val inferredOrganizerPhone = PhoneIdentityValidator.normalizeIndianPhone10(rawOrganizerPhone).ifBlank {
                val orgMember = members.firstOrNull { it.memberId.endsWith("_me") }
                    ?: members.firstOrNull { it.isCurrentUser }
                    ?: members.firstOrNull()
                orgMember?.let { PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) }.orEmpty()
            }
            val rawJoinCode = root.optString("joinCode6", "")
            val resolvedJoinCode = normalizeJoinCode6(rawJoinCode).ifBlank { deriveGroupJoinCode6(groupId) }

            CloudGroupLedgerDocument(
                group = group,
                members = members,
                expenses = expenses,
                splits = splits,
                settlements = settlements,
                deletedExpenseIds = deletedExpenseIds,
                flightVaultByPnr = flightVaultByPnr,
                trainSnapshotByPnr = trainSnapshotByPnr,
                updatedAtEpochMs = root.optLong("updatedAtEpochMs", System.currentTimeMillis()),
                memberPresenceByPhone = memberPresenceByPhone,
                organizerPhone10 = inferredOrganizerPhone,
                joinCode6 = resolvedJoinCode,
                deletedMemberIds = deletedMemberIds,
                removedMemberPhones = removedMemberPhones,
                deletedSettlementIds = deletedSettlementIds,
                organizerRolesByKey = organizerRolesByKey,
                customCategories = GroupLedgerExtrasCodec.decodeCustomCategories(
                    root.optJSONArray(GroupLedgerExtrasCodec.KEY_CUSTOM_CATEGORIES)
                ),
                tripLifecycle = GroupLedgerExtrasCodec.decodeTripLifecycle(
                    root.optJSONObject(GroupLedgerExtrasCodec.KEY_TRIP_LIFECYCLE)
                ),
                revisions = ExpenseVersionSync.decodeRevisions(root.optJSONArray(ExpenseVersionSync.KEY_REVISIONS), groupId)
            )
        } catch (_: Exception) {
            null
        }
    }

    // =========================================================================
    // STRUCTURAL LEDGER HASH DEDUPLICATION & DELTA INDEX CACHE (F2, F3, PA-5)
    // =========================================================================

    fun computeStructuralLedgerHash(doc: CloudGroupLedgerDocument): String {
        val canonical = JSONObject().apply {
            put("groupId", doc.group.groupId)
            put("name", doc.group.name)
            put("currencyCode", doc.group.currencyCode)
            put("iconName", doc.group.iconName)
            put("isDemoSeed", doc.group.isDemoSeed)
            put("organizerPhone10", PhoneIdentityValidator.normalizeIndianPhone10(doc.organizerPhone10))
            put("joinCode6", normalizeJoinCode6(doc.joinCode6).ifBlank { deriveGroupJoinCode6(doc.group.groupId) })
            val orgRolesArr = JSONArray()
            doc.organizerRolesByKey.entries.sortedBy { it.key }.forEach { (k, v) ->
                if (k.isNotBlank() && v != 0L) {
                    orgRolesArr.put("$k:${if (v > 0L) 1 else -1}")
                }
            }
            put("organizerRolesByKey", orgRolesArr)

            val membersArr = JSONArray()
            doc.members.sortedWith(compareBy<GroupMemberEntity> {
                PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId).ifBlank { it.memberId }
            }.thenBy { it.memberId }).forEach { m ->
                membersArr.put(JSONObject().apply {
                    put("memberId", m.memberId)
                    put("name", m.name)
                    put("avatarSeed", m.avatarSeed)
                    put("upiId", m.upiId)
                    put("userPhone", PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId).ifBlank { m.userPhone })
                    put("inviteStatus", m.inviteStatus)
                })
            }
            put("members", membersArr)

            val expensesArr = JSONArray()
            doc.expenses.sortedBy { it.expenseId }.forEach { e ->
                expensesArr.put(JSONObject().apply {
                    put("expenseId", e.expenseId)
                    put("title", e.title)
                    put("payerId", e.payerId)
                    put("baseSubtotalCents", e.baseSubtotalCents)
                    put("taxCents", e.taxCents)
                    put("tipCents", e.tipCents)
                    put("totalAmountCents", e.totalAmountCents)
                    put("lockedMultiplier", e.lockedMultiplier)
                    put("unassignedBaseCents", e.unassignedBaseCents)
                    put("currencyCode", e.currencyCode)
                    put("lockedExchangeRate", e.lockedExchangeRate)
                    put("expenseCategory", e.expenseCategory)
                    put("travelPnr", e.travelPnr)
                    put("providerName", e.providerName)
                    put("scheduledAtEpochMs", e.scheduledAtEpochMs ?: -1L)
                    // v2.3.5: only when present, so pre-v2.3.5 hashes stay byte-identical.
                    e.categoryRef?.takeIf { it.isNotBlank() }?.let { put("categoryRef", it) }
                    e.createdByPhone?.takeIf { it.isNotBlank() }?.let { put("createdByPhone", it) }
                    // v2.4.0 P1: only when versioned, so unversioned hashes stay byte-identical.
                    if (e.rowVersion > 0L) put("rv", e.rowVersion)
                })
            }
            put("expenses", expensesArr)

            val splitsArr = JSONArray()
            doc.splits.sortedWith(compareBy<ExpenseSplitEntity> { it.expenseId }.thenBy { it.memberId }.thenBy { it.splitId }).forEach { s ->
                splitsArr.put(JSONObject().apply {
                    put("splitId", s.splitId)
                    put("expenseId", s.expenseId)
                    put("memberId", s.memberId)
                    put("baseClaimedCents", s.baseClaimedCents)
                    put("finalOwedCents", s.finalOwedCents)
                    put("plusOneCent", s.plusOneCent)
                })
            }
            put("splits", splitsArr)

            val settlementsArr = JSONArray()
            doc.settlements.sortedBy { it.settlementId }.forEach { st ->
                settlementsArr.put(JSONObject().apply {
                    put("settlementId", st.settlementId)
                    put("fromMemberId", st.fromMemberId)
                    put("toMemberId", st.toMemberId)
                    put("amountCents", st.amountCents)
                    put("currencyCode", st.currencyCode)
                    put("settledAt", st.settledAt)
                })
            }
            put("settlements", settlementsArr)

            put("deletedExpenseIds", JSONArray(doc.deletedExpenseIds.keys.sorted()))
            put("deletedMemberIds", JSONArray(doc.deletedMemberIds.keys.sorted()))
            put("removedMemberPhones", JSONArray(doc.removedMemberPhones.keys.sorted()))
            put("deletedSettlementIds", JSONArray(doc.deletedSettlementIds.keys.sorted()))
            put("flightPnrs", JSONArray(doc.flightVaultByPnr.entries.sortedBy { it.key }.map { "${it.key}:${it.value}" }))
            put("trainPnrs", JSONArray(doc.trainSnapshotByPnr.entries.sortedBy { it.key }.map { "${it.key}:${it.value}" }))
            // v2.3.5 additive keys: folded in ONLY when non-empty / present (legacy hashes unchanged).
            if (doc.customCategories.isNotEmpty()) {
                put(GroupLedgerExtrasCodec.KEY_CUSTOM_CATEGORIES, GroupLedgerExtrasCodec.canonicalCustomCategories(doc.customCategories))
            }
            doc.tripLifecycle?.let {
                put(GroupLedgerExtrasCodec.KEY_TRIP_LIFECYCLE, GroupLedgerExtrasCodec.canonicalTripLifecycle(it))
            }
        }.toString()

        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun getLastPushedGroupHash(context: Context?, groupId: String): String {
        val mem = lastPushedGroupHashByGroup[groupId]
        if (!mem.isNullOrBlank()) return mem
        val ctx = resolveContext(context) ?: return ""
        return try {
            ctx.getSharedPreferences(SYNC_META_PREFS_NAME, Context.MODE_PRIVATE)
                .getString("hash_$groupId", "")
                .orEmpty()
                .also { if (it.isNotBlank()) lastPushedGroupHashByGroup[groupId] = it }
        } catch (_: Exception) {
            ""
        }
    }

    private fun setLastPushedGroupHash(context: Context?, groupId: String, hash: String) {
        lastPushedGroupHashByGroup[groupId] = hash
        val ctx = resolveContext(context) ?: return
        try {
            ctx.getSharedPreferences(SYNC_META_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("hash_$groupId", hash)
                .apply()
        } catch (_: Exception) {
        }
    }

    private fun getLastPushedIndexStatus(context: Context?, groupId: String, phone10: String): String {
        val key = "${groupId}_$phone10"
        val mem = lastPushedIndexStatusByGroupPhone[key]
        if (!mem.isNullOrBlank()) return mem
        val ctx = resolveContext(context) ?: return ""
        return try {
            ctx.getSharedPreferences(SYNC_META_PREFS_NAME, Context.MODE_PRIVATE)
                .getString("idx_$key", "")
                .orEmpty()
                .also { if (it.isNotBlank()) lastPushedIndexStatusByGroupPhone[key] = it }
        } catch (_: Exception) {
            ""
        }
    }

    private fun setLastPushedIndexStatus(context: Context?, groupId: String, phone10: String, status: String) {
        val key = "${groupId}_$phone10"
        lastPushedIndexStatusByGroupPhone[key] = status
        val ctx = resolveContext(context) ?: return
        try {
            ctx.getSharedPreferences(SYNC_META_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("idx_$key", status)
                .apply()
        } catch (_: Exception) {
        }
    }

    private fun getLastPushedIndexEpoch(context: Context?, groupId: String, phone10: String): Long {
        val key = "${groupId}_$phone10"
        val mem = lastPushedIndexEpochByGroupPhone[key]
        if (mem != null && mem > 0L) return mem
        val ctx = resolveContext(context) ?: return 0L
        return try {
            ctx.getSharedPreferences(SYNC_META_PREFS_NAME, Context.MODE_PRIVATE)
                .getLong("idx_ts_$key", 0L)
                .also { if (it > 0L) lastPushedIndexEpochByGroupPhone[key] = it }
        } catch (_: Exception) {
            0L
        }
    }

    private fun setLastPushedIndexEpoch(context: Context?, groupId: String, phone10: String, epochMs: Long) {
        val key = "${groupId}_$phone10"
        lastPushedIndexEpochByGroupPhone[key] = epochMs
        val ctx = resolveContext(context) ?: return
        try {
            ctx.getSharedPreferences(SYNC_META_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong("idx_ts_$key", epochMs)
                .apply()
        } catch (_: Exception) {
        }
    }

    private fun getLastCodePublishEpoch(context: Context?, groupId: String): Long {
        val mem = lastCodePublishEpochByGroup[groupId]
        if (mem != null && mem > 0L) return mem
        val ctx = resolveContext(context) ?: return 0L
        return try {
            ctx.getSharedPreferences(SYNC_META_PREFS_NAME, Context.MODE_PRIVATE)
                .getLong("code_ts_$groupId", 0L)
                .also { if (it > 0L) lastCodePublishEpochByGroup[groupId] = it }
        } catch (_: Exception) {
            0L
        }
    }

    private fun setLastCodePublishEpoch(context: Context?, groupId: String, epochMs: Long) {
        lastCodePublishEpochByGroup[groupId] = epochMs
        val ctx = resolveContext(context) ?: return
        try {
            ctx.getSharedPreferences(SYNC_META_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong("code_ts_$groupId", epochMs)
                .apply()
        } catch (_: Exception) {
        }
    }

    private fun getLastLocalMutationEpoch(context: Context?, groupId: String): Long {
        val mem = lastLocalMutationEpochByGroup[groupId]
        if (mem != null && mem > 0L) return mem
        val ctx = resolveContext(context) ?: return 0L
        return try {
            ctx.getSharedPreferences(SYNC_META_PREFS_NAME, Context.MODE_PRIVATE)
                .getLong("local_mut_$groupId", 0L)
                .also { if (it > 0L) lastLocalMutationEpochByGroup[groupId] = it }
        } catch (_: Exception) {
            0L
        }
    }

    private fun setLastLocalMutationEpoch(context: Context?, groupId: String, epochMs: Long) {
        lastLocalMutationEpochByGroup[groupId] = epochMs
        val ctx = resolveContext(context) ?: return
        try {
            ctx.getSharedPreferences(SYNC_META_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong("local_mut_$groupId", epochMs)
                .apply()
        } catch (_: Exception) {
        }
    }

    fun isGroupPendingCloudPush(context: Context?, groupId: String): Boolean {
        val mem = pendingCloudPushByGroup[groupId]
        if (mem != null) return mem
        val ctx = resolveContext(context) ?: return false
        return try {
            ctx.getSharedPreferences(SYNC_META_PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("pending_push_$groupId", false)
                .also { pendingCloudPushByGroup[groupId] = it }
        } catch (_: Exception) {
            false
        }
    }

    fun setGroupPendingCloudPush(context: Context?, groupId: String, pending: Boolean) {
        pendingCloudPushByGroup[groupId] = pending
        val ctx = resolveContext(context) ?: return
        try {
            ctx.getSharedPreferences(SYNC_META_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("pending_push_$groupId", pending)
                .apply()
        } catch (_: Exception) {
        }
    }

    fun isInternetAvailable(context: Context?): Boolean {
        val ctx = resolveContext(context) ?: return true
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                ?: return true
            val activeNet = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNet) ?: return false
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    // =========================================================================
    // CLOUD NTFY JSON CHANNEL TRANSPORT + EXPONENTIAL BACKOFF (F4)
    // =========================================================================

    private const val GZIP_PREFIX = "SMGZ:"

    internal fun compressPayloadIfNeeded(rawJson: String): String {
        val rawBytes = rawJson.toByteArray(Charsets.UTF_8)
        if (rawBytes.size <= 3000) return rawJson
        return try {
            val bos = java.io.ByteArrayOutputStream()
            java.util.zip.GZIPOutputStream(bos).use { gzip ->
                gzip.write(rawBytes)
            }
            GZIP_PREFIX + java.util.Base64.getEncoder().encodeToString(bos.toByteArray())
        } catch (_: Exception) {
            rawJson
        }
    }

    internal fun decompressPayloadIfNeeded(payload: String): String {
        val trimmed = payload.trim()
        if (!trimmed.startsWith(GZIP_PREFIX)) return trimmed
        return try {
            val compressedBytes = java.util.Base64.getDecoder().decode(trimmed.removePrefix(GZIP_PREFIX))
            java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(compressedBytes)).use { gis ->
                InputStreamReader(gis, Charsets.UTF_8).readText()
            }
        } catch (_: Exception) {
            trimmed
        }
    }

    private fun downloadAttachmentText(attachmentUrl: String): String? {
        return try {
            val conn = (URL(attachmentUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }
            if (conn.responseCode == 200) {
                InputStreamReader(conn.inputStream, Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractNtfyEventPayload(eventJson: JSONObject): String? {
        val attachmentUrl = eventJson.optJSONObject("attachment")?.optString("url", "").orEmpty()
        val rawContent = if (attachmentUrl.isNotBlank()) {
            downloadAttachmentText(attachmentUrl) ?: eventJson.optString("message", "")
        } else {
            eventJson.optString("message", "")
        }
        if (rawContent.isBlank()) return null
        return decompressPayloadIfNeeded(rawContent).takeIf { it.isNotBlank() }
    }

    private suspend fun executeNtfyGetWithBackoff(urlStr: String): List<String>? = withContext(Dispatchers.IO) {
        val backoffDelaysMs = longArrayOf(800L, 2000L, 4000L)
        for (attempt in 0 until 4) {
            var retryDelayMs = if (attempt < backoffDelaysMs.size) backoffDelaysMs[attempt] else 4000L
            try {
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                }
                val code = conn.responseCode
                if (code == 200) {
                    return@withContext InputStreamReader(conn.inputStream, Charsets.UTF_8).use { it.readLines() }
                }
                if (code != 429 && code !in 500..599) {
                    return@withContext null
                }
                val retryAfterSec = conn.getHeaderField("Retry-After")?.trim()?.toLongOrNull()
                if (retryAfterSec != null && retryAfterSec > 0L) {
                    retryDelayMs = (retryAfterSec * 1000L).coerceAtMost(5500L)
                }
            } catch (_: IOException) {
                // Retry transient network IOException on attempts 0..2
            } catch (_: Exception) {
                return@withContext null
            }
            if (attempt < 3) {
                delay(retryDelayMs)
            }
        }
        return@withContext null
    }

    private suspend fun executeNtfyPostWithBackoff(topic: String, wirePayload: String): Boolean = withContext(Dispatchers.IO) {
        val urlStr = "https://ntfy.sh/$topic"
        val payloadBytes = wirePayload.toByteArray(Charsets.UTF_8)
        val backoffDelaysMs = longArrayOf(800L, 2000L, 4200L)
        for (attempt in 0 until 4) {
            var retryDelayMs = if (attempt < backoffDelaysMs.size) backoffDelaysMs[attempt] else 4200L
            try {
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Title", "SplitMateSync")
                    setRequestProperty("Cache", "yes")
                    if (payloadBytes.size > 3800) {
                        setRequestProperty("Filename", "ledger.txt")
                    }
                    doOutput = true
                    connectTimeout = 6000
                    readTimeout = 6000
                }
                conn.outputStream.use { os ->
                    os.write(payloadBytes)
                }
                val code = conn.responseCode
                if (code == 200) {
                    return@withContext true
                }
                if (code != 429 && code !in 500..599) {
                    return@withContext false
                }
                val retryAfterSec = conn.getHeaderField("Retry-After")?.trim()?.toLongOrNull()
                if (retryAfterSec != null && retryAfterSec > 0L) {
                    retryDelayMs = (retryAfterSec * 1000L).coerceAtMost(5500L)
                }
            } catch (_: IOException) {
                // Retry transient network IOException on attempts 0..2
            } catch (_: Exception) {
                return@withContext false
            }
            if (attempt < 3) {
                delay(retryDelayMs)
            }
        }
        return@withContext false
    }

    private suspend fun fetchNtfySnapshot(topic: String): String? = withContext(Dispatchers.IO) {
        val lines = executeNtfyGetWithBackoff("https://ntfy.sh/$topic/json?poll=1&since=all")
            ?: return@withContext null
        var bestEventJson: JSONObject? = null
        var bestTime = 0L
        for (line in lines) {
            if (line.isBlank()) continue
            try {
                val json = JSONObject(line)
                if (json.optString("event") == "message") {
                    val time = json.optLong("time", 0L)
                    if (time >= bestTime) {
                        bestTime = time
                        bestEventJson = json
                    }
                }
            } catch (_: Exception) {
            }
        }
        return@withContext bestEventJson?.let { extractNtfyEventPayload(it) }
    }

    /**
     * Fetches the group ledger topic (`splitmate_v2_grp_<groupId>`) and folds the last 6 valid
     * `CloudGroupLedgerDocument` snapshots in chronological order using [mergeGroupLedgerDocuments].
     *
     * Returns `Pair<CloudGroupLedgerDocument?, Boolean>` where the second element (`fetchSucceeded`)
     * is `true` iff the HTTP GET succeeded (`200 OK`, even if 0 messages exist yet) and `false`
     * if the HTTP GET failed due to network error or rate-limiting (`429`).
     *
     * Folding the recent message window guarantees that even if two phones push concurrently
     * within the same second, no expense, split, member, or tombstone in the recent log is ever lost.
     */
    private suspend fun fetchRemoteGroupLedgerOutcome(
        topic: String,
        localUserPhone10: String,
        localUserAvatarSeed: String,
        versionHistory: com.splitmate.app.ExpenseVersioning.History? = null
    ): Pair<CloudGroupLedgerDocument?, Boolean> = withContext(Dispatchers.IO) {
        val lines = executeNtfyGetWithBackoff("https://ntfy.sh/$topic/json?poll=1&since=all")
            ?: return@withContext (null to false)
        val messageEvents = mutableListOf<Pair<Long, JSONObject>>()
        for (line in lines) {
            if (line.isBlank()) continue
            try {
                val json = JSONObject(line)
                if (json.optString("event") == "message") {
                    messageEvents.add(json.optLong("time", 0L) to json)
                }
            } catch (_: Exception) {
            }
        }
        if (messageEvents.isEmpty()) return@withContext (null to true)

        val recentEvents = messageEvents
            .sortedBy { it.first }
            .takeLast(6)
            .map { it.second }

        var foldedDoc: CloudGroupLedgerDocument? = null
        for (eventJson in recentEvents) {
            val payload = extractNtfyEventPayload(eventJson) ?: continue
            val doc = decodeGroupLedgerDocument(payload) ?: continue
            foldedDoc = if (foldedDoc == null) {
                doc
            } else {
                mergeGroupLedgerDocuments(
                    localDoc = foldedDoc,
                    remoteDoc = doc,
                    localUserPhone10 = localUserPhone10,
                    localUserAvatarSeed = localUserAvatarSeed,
                    versionHistory = versionHistory
                )
            }
        }
        return@withContext (foldedDoc to true)
    }

    private suspend fun fetchAllNtfyMessages(topic: String): List<String> = withContext(Dispatchers.IO) {
        val lines = executeNtfyGetWithBackoff("https://ntfy.sh/$topic/json?poll=1&since=all")
            ?: return@withContext emptyList()
        val messages = mutableListOf<String>()
        for (line in lines) {
            if (line.isBlank()) continue
            try {
                val json = JSONObject(line)
                if (json.optString("event") == "message") {
                    val extracted = extractNtfyEventPayload(json)
                    if (!extracted.isNullOrBlank()) {
                        messages.add(extracted)
                    }
                }
            } catch (_: Exception) {
            }
        }
        return@withContext messages
    }

    fun groupTopicForGroupId(groupId: String): String =
        "splitmate_v2_grp_${sanitizeTopicKey(groupId)}"

    /** v2.3.4: dedicated ntfy topic for the group's Trip plan manifest (never carries ledger data). */
    fun groupPlanTopicForGroupId(groupId: String): String =
        "splitmate_v2_plan_${sanitizeTopicKey(groupId)}"

    /**
     * Opens a single real-time HTTP pub/sub stream on ntfy.sh across [topics] and suspends until
     * any member on another device publishes a "message" event, returning the changed topic name
     * in < 1 second (or null on keepalive timeout / disconnect).
     */
    suspend fun awaitLiveCloudTopicChange(topics: List<String>): String? = withContext(Dispatchers.IO) {
        val cleanTopics = topics.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(12)
        if (cleanTopics.isEmpty()) return@withContext null
        val urlStr = "https://ntfy.sh/${cleanTopics.joinToString(",")}/json"
        var conn: HttpURLConnection? = null
        return@withContext try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 55000
            }
            if (conn.responseCode != 200) {
                return@withContext null
            }
            java.io.BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue
                    try {
                        val json = JSONObject(line)
                        if (json.optString("event") == "message") {
                            val changedTopic = json.optString("topic", "").trim()
                            if (changedTopic.isNotEmpty()) {
                                return@withContext changedTopic
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    private suspend fun pushNtfySnapshot(topic: String, jsonPayload: String): Boolean = withContext(Dispatchers.IO) {
        val wirePayload = compressPayloadIfNeeded(jsonPayload)
        return@withContext executeNtfyPostWithBackoff(topic, wirePayload)
    }

    // =========================================================================
    // ATOMIC READ-BEFORE-WRITE UNION MERGE & CRDT TOMBSTONE ENGINE (F5, PA-1, PA-6, PA-8)
    // =========================================================================

    fun mergeDocuments(
        localDoc: CloudGroupLedgerDocument?,
        remoteDoc: CloudGroupLedgerDocument?,
        localUserPhone10: String,
        localUserAvatarSeed: String = "",
        localPresenceEpochMs: Long = System.currentTimeMillis()
    ): CloudGroupLedgerDocument = mergeGroupLedgerDocuments(
        localDoc = localDoc,
        remoteDoc = remoteDoc,
        localUserPhone10 = localUserPhone10,
        localUserAvatarSeed = localUserAvatarSeed,
        localPresenceEpochMs = localPresenceEpochMs
    )

    fun mergeGroupLedgerDocuments(
        localDoc: CloudGroupLedgerDocument?,
        remoteDoc: CloudGroupLedgerDocument?,
        localUserPhone10: String,
        localUserAvatarSeed: String = "",
        localPresenceEpochMs: Long = System.currentTimeMillis(),
        /** v2.4.0 P1: versions this phone already knows (null = no history, e.g. folding cloud snapshots). */
        versionHistory: com.splitmate.app.ExpenseVersioning.History? = null
    ): CloudGroupLedgerDocument {
        val normLocalPhone = PhoneIdentityValidator.normalizeIndianPhone10(localUserPhone10)
        if (localDoc == null && remoteDoc == null) throw IllegalArgumentException("Both docs null")

        val primaryDoc = localDoc ?: remoteDoc!!
        val secondaryDoc = if (localDoc != null) remoteDoc else null
        val groupId = primaryDoc.group.groupId
        val storedTombstones = loadLocalTombstones(null, groupId)

        // 1. Union all 4 CRDT tombstone maps across localDoc, remoteDoc, and durable LocalTombstoneStore
        val mergedDeletedExpenseIds = mutableMapOf<String, Long>()
        (primaryDoc.deletedExpenseIds.keys +
            secondaryDoc?.deletedExpenseIds?.keys.orEmpty() +
            storedTombstones.deletedExpenseIds.keys).forEach { id ->
            mergedDeletedExpenseIds[id] = max(
                max(primaryDoc.deletedExpenseIds[id] ?: 0L, secondaryDoc?.deletedExpenseIds?.get(id) ?: 0L),
                storedTombstones.deletedExpenseIds[id] ?: 0L
            )
        }

        val mergedDeletedMemberIds = mutableMapOf<String, Long>()
        (primaryDoc.deletedMemberIds.keys +
            secondaryDoc?.deletedMemberIds?.keys.orEmpty() +
            storedTombstones.deletedMemberIds.keys).forEach { id ->
            mergedDeletedMemberIds[id] = max(
                max(primaryDoc.deletedMemberIds[id] ?: 0L, secondaryDoc?.deletedMemberIds?.get(id) ?: 0L),
                storedTombstones.deletedMemberIds[id] ?: 0L
            )
        }

        val mergedRemovedMemberPhones = mutableMapOf<String, Long>()
        (primaryDoc.removedMemberPhones.keys +
            secondaryDoc?.removedMemberPhones?.keys.orEmpty() +
            storedTombstones.removedMemberPhones.keys).forEach { rawPhone ->
            val normP = PhoneIdentityValidator.normalizeIndianPhone10(rawPhone)
            if (normP.length == 10) {
                val ts = max(
                    max(
                        primaryDoc.removedMemberPhones[rawPhone] ?: primaryDoc.removedMemberPhones[normP] ?: 0L,
                        secondaryDoc?.removedMemberPhones?.get(rawPhone) ?: secondaryDoc?.removedMemberPhones?.get(normP) ?: 0L
                    ),
                    storedTombstones.removedMemberPhones[normP] ?: 0L
                )
                mergedRemovedMemberPhones[normP] = max(mergedRemovedMemberPhones[normP] ?: 0L, ts)
            }
        }

        val mergedDeletedSettlementIds = mutableMapOf<String, Long>()
        (primaryDoc.deletedSettlementIds.keys +
            secondaryDoc?.deletedSettlementIds?.keys.orEmpty() +
            storedTombstones.deletedSettlementIds.keys).forEach { id ->
            mergedDeletedSettlementIds[id] = max(
                max(primaryDoc.deletedSettlementIds[id] ?: 0L, secondaryDoc?.deletedSettlementIds?.get(id) ?: 0L),
                storedTombstones.deletedSettlementIds[id] ?: 0L
            )
        }

        val mergedOrganizerRolesByKey = mergeOrganizerRoleMaps(
            localDoc?.organizerRolesByKey.orEmpty(),
            remoteDoc?.organizerRolesByKey.orEmpty(),
            storedTombstones.organizerRolesByKey
        )

        // v2.3.5: union/LWW merge of group custom categories + trip lifecycle across local, remote and the
        // durable local copy (old clients drop these keys when they push; the durable copy heals that).
        val storedExtras = GroupLedgerExtrasStore.load(null, groupId)
        val mergedCustomCategories = GroupLedgerExtrasCodec.mergeCustomCategories(
            localDoc?.customCategories.orEmpty(),
            remoteDoc?.customCategories.orEmpty(),
            storedExtras.customCategories
        )
        val mergedTripLifecycle = GroupLedgerExtrasCodec.mergeTripLifecycle(
            localDoc?.tripLifecycle,
            remoteDoc?.tripLifecycle,
            storedExtras.tripLifecycle
        )

        // Un-tombstone phone if a genuinely new member row (memberId !in mergedDeletedMemberIds)
        // was re-invited with PENDING/JOINED at a strictly newer document timestamp
        val allMembersWithDocTime = buildList {
            if (localDoc != null) addAll(localDoc.members.map { it to localDoc.updatedAtEpochMs })
            if (remoteDoc != null) addAll(remoteDoc.members.map { it to remoteDoc.updatedAtEpochMs })
        }
        for ((m, docTime) in allMembersWithDocTime) {
            if (m.memberId in mergedDeletedMemberIds) continue
            val normPhone = PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId)
            if (normPhone.length == 10 && m.inviteStatus.uppercase() in setOf("PENDING", "JOINED")) {
                val removedAt = mergedRemovedMemberPhones[normPhone]
                if (removedAt != null && docTime > removedAt) {
                    mergedRemovedMemberPhones.remove(normPhone)
                }
            }
        }

        // Single-doc fast path when no tombstones or orphaned references exist
        if (localDoc == null || remoteDoc == null) {
            val singleDoc = localDoc ?: remoteDoc!!
            val hasTombstonedEntities = mergedDeletedExpenseIds.isNotEmpty() ||
                mergedDeletedMemberIds.isNotEmpty() ||
                mergedRemovedMemberPhones.isNotEmpty() ||
                mergedDeletedSettlementIds.isNotEmpty()
            val resolvedOrgPhone = PhoneIdentityValidator.normalizeIndianPhone10(
                singleDoc.organizerPhone10.ifBlank { storedTombstones.organizerPhone10 }
            ).ifBlank {
                val orgCandidate = singleDoc.members.firstOrNull { it.memberId.endsWith("_me") }
                    ?: singleDoc.members.firstOrNull { it.isCurrentUser }
                    ?: singleDoc.members.firstOrNull()
                orgCandidate?.let { PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) }.orEmpty()
            }
            val resolvedJoinCode = normalizeJoinCode6(singleDoc.joinCode6)
                .ifBlank { normalizeJoinCode6(storedTombstones.joinCode6) }
                .ifBlank { deriveGroupJoinCode6(groupId) }

            if (!hasTombstonedEntities) {
                // v2.4.0 P2: no blind repair (it trusted the shares and could undo an edit). A mismatch is
                // kept as-is, flagged on upload and shown by the Money check.
                val repairedSingleExpenses = singleDoc.expenses
                val (singleSettlements, singleDeletedSettlementIds) = dropRedundantDuplicateSettlements(
                    repairedSingleExpenses, singleDoc.splits, singleDoc.settlements, singleDoc.deletedSettlementIds
                )
                return adjustMembersForLocalUser(
                    doc = singleDoc.copy(
                        expenses = repairedSingleExpenses,
                        settlements = singleSettlements,
                        deletedSettlementIds = singleDeletedSettlementIds,
                        organizerPhone10 = resolvedOrgPhone,
                        joinCode6 = resolvedJoinCode,
                        organizerRolesByKey = mergedOrganizerRolesByKey,
                        customCategories = mergedCustomCategories,
                        tripLifecycle = mergedTripLifecycle,
                        revisions = ExpenseVersionSync.compactForSync(singleDoc.revisions, mergedDeletedExpenseIds.keys)
                    ),
                    localUserPhone10 = normLocalPhone,
                    localUserAvatarSeed = localUserAvatarSeed,
                    isRemoteOnly = (localDoc == null),
                    localPresenceEpochMs = localPresenceEpochMs
                )
            }
        }

        // 2. Member union & filtering against deletedMemberIds and removedMemberPhones
        val memberMap = linkedMapOf<String, GroupMemberEntity>()
        val docTimeByKey = mutableMapOf<String, Long>()
        val keyByMemberId = mutableMapOf<String, String>()
        val memberIdRemap = mutableMapOf<String, String>()
        for ((member, docTime) in allMembersWithDocTime) {
            if (member.memberId in mergedDeletedMemberIds) continue
            val normPhone = PhoneIdentityValidator.extractMemberPhone10(member.userPhone, member.upiId)
            if (normPhone.length == 10 && normPhone in mergedRemovedMemberPhones) continue

            val normalizedMember = if (normPhone.isNotBlank() && member.userPhone != normPhone) {
                member.copy(userPhone = normPhone)
            } else {
                member
            }
            val candidateKey = if (normPhone.isNotBlank()) normPhone else normalizedMember.memberId
            val existingKeyForMemberId = keyByMemberId[normalizedMember.memberId]
            val effectiveKey = existingKeyForMemberId ?: candidateKey

            // Also match phoneless member row with phone-bearing member row of the exact same normalized name
            val cleanMemberName = normalizedMember.name.trim().lowercase()
            val nameMatchedKey = if (!memberMap.containsKey(effectiveKey) && !memberMap.containsKey(candidateKey) && cleanMemberName.isNotEmpty()) {
                memberMap.entries.firstOrNull { (_, existingM) ->
                    val exPhone = PhoneIdentityValidator.extractMemberPhone10(existingM.userPhone, existingM.upiId)
                    val oneHasPhoneOneDoesNot = (normPhone.length == 10 && exPhone.isEmpty()) ||
                        (normPhone.isEmpty() && exPhone.length == 10)
                    oneHasPhoneOneDoesNot && existingM.name.trim().lowercase() == cleanMemberName
                }?.key
            } else null

            val existing = memberMap[effectiveKey]
                ?: memberMap[candidateKey]
                ?: (if (nameMatchedKey != null) memberMap[nameMatchedKey] else null)
            if (existing == null) {
                memberMap[candidateKey] = normalizedMember
                docTimeByKey[candidateKey] = docTime
                keyByMemberId[normalizedMember.memberId] = candidateKey
                memberIdRemap[normalizedMember.memberId] = normalizedMember.memberId
            } else {
                val existingActualKey = when {
                    memberMap.containsKey(effectiveKey) -> effectiveKey
                    memberMap.containsKey(candidateKey) -> candidateKey
                    nameMatchedKey != null -> nameMatchedKey
                    else -> candidateKey
                }
                val existingDocTime = docTimeByKey[existingActualKey] ?: 0L
                val existingPhone = PhoneIdentityValidator.extractMemberPhone10(existing.userPhone, existing.upiId)
                val mergedPhone = when {
                    docTime >= existingDocTime && normPhone.length == 10 -> normPhone
                    existingPhone.length == 10 -> existingPhone
                    else -> normPhone.ifBlank { existing.userPhone }
                }
                val isDeclinedLatest = (docTime >= existingDocTime && normalizedMember.inviteStatus.equals("DECLINED", ignoreCase = true)) ||
                    (existingDocTime > docTime && existing.inviteStatus.equals("DECLINED", ignoreCase = true) && !normalizedMember.inviteStatus.equals("JOINED", ignoreCase = true))
                val mergedInviteStatus = when {
                    isDeclinedLatest -> "DECLINED"
                    existing.inviteStatus.equals("JOINED", ignoreCase = true) &&
                        existingPhone.isEmpty() &&
                        normPhone.length == 10 &&
                        docTime > existingDocTime -> normalizedMember.inviteStatus
                    existing.inviteStatus.equals("JOINED", ignoreCase = true) ||
                        normalizedMember.inviteStatus.equals("JOINED", ignoreCase = true) -> "JOINED"
                    docTime >= existingDocTime -> normalizedMember.inviteStatus
                    else -> existing.inviteStatus
                }
                val winnerBase = if (docTime >= existingDocTime) normalizedMember else existing
                val mergedSeed = when {
                    winnerBase.avatarSeed.count { it == '|' } >= 3 -> winnerBase.avatarSeed
                    existing.avatarSeed.count { it == '|' } >= 3 -> existing.avatarSeed
                    normalizedMember.avatarSeed.count { it == '|' } >= 3 -> normalizedMember.avatarSeed
                    else -> winnerBase.avatarSeed.ifBlank { existing.avatarSeed }
                }
                val mergedWinner = winnerBase.copy(
                    memberId = existing.memberId,
                    avatarSeed = mergedSeed,
                    userPhone = mergedPhone,
                    upiId = if (mergedPhone.length == 10 && winnerBase.upiId.isBlank()) "${mergedPhone}@upi" else winnerBase.upiId.ifBlank { existing.upiId },
                    inviteStatus = mergedInviteStatus
                )
                val finalKey = if (mergedPhone.length == 10) mergedPhone else existing.memberId
                if (existingActualKey != finalKey) {
                    memberMap.remove(existingActualKey)
                    docTimeByKey.remove(existingActualKey)
                }
                memberMap[finalKey] = mergedWinner
                docTimeByKey[finalKey] = max(existingDocTime, docTime)
                keyByMemberId[existing.memberId] = finalKey
                keyByMemberId[normalizedMember.memberId] = finalKey
                memberIdRemap[normalizedMember.memberId] = existing.memberId
                memberIdRemap[existing.memberId] = existing.memberId
            }
        }

        // Resolve Organizer phone & Organizer member ID for PA-1 referential integrity fallback
        val resolvedOrganizerPhone = PhoneIdentityValidator.normalizeIndianPhone10(
            localDoc?.organizerPhone10?.takeIf { it.isNotBlank() }
                ?: remoteDoc?.organizerPhone10?.takeIf { it.isNotBlank() }
                ?: storedTombstones.organizerPhone10
        ).ifBlank {
            val orgCandidate = memberMap.values.firstOrNull { it.memberId.endsWith("_me") }
                ?: memberMap.values.firstOrNull { it.isCurrentUser }
                ?: memberMap.values.firstOrNull()
            orgCandidate?.let { PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) }.orEmpty()
        }

        val resolvedJoinCode = normalizeJoinCode6(
            localDoc?.joinCode6?.takeIf { it.isNotBlank() }
                ?: remoteDoc?.joinCode6?.takeIf { it.isNotBlank() }
                ?: storedTombstones.joinCode6
                ?: ""
        ).ifBlank { deriveGroupJoinCode6(groupId) }

        val survivingMemberIds = memberMap.values.map { it.memberId }.toSet()
        val activeSurvivingMemberIds = memberMap.values
            .filter { !it.inviteStatus.equals("DECLINED", ignoreCase = true) }
            .map { it.memberId }
        val organizerMemberId = memberMap.values.firstOrNull {
            resolvedOrganizerPhone.length == 10 &&
                PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == resolvedOrganizerPhone
        }?.memberId
            ?: memberMap.values.firstOrNull { it.memberId.endsWith("_me") }?.memberId
            ?: memberMap.values.firstOrNull()?.memberId
            ?: ""

        // 3. Merge expenses, remap cross-device payerId, and enforce PA-1 (reassign orphaned payerId to organizerMemberId)
        // v2.4.0 P1: each expense present on both sides is decided on its own version
        // (ExpenseVersioning.decide), not by which whole document is newer.
        val localDocWins = localDoc != null && remoteDoc != null && localDoc.updatedAtEpochMs >= remoteDoc.updatedAtEpochMs
        val localExpenseById = localDoc?.expenses.orEmpty().associateBy { it.expenseId }
        val remoteExpenseById = remoteDoc?.expenses.orEmpty().associateBy { it.expenseId }
        val localSplitsByExpense = localDoc?.splits.orEmpty().groupBy { it.expenseId }
        val remoteSplitsByExpense = remoteDoc?.splits.orEmpty().groupBy { it.expenseId }
        // v2.4.0 P4: a version recorded by ANY phone (synced history) counts as a known copy.
        val effectiveHistory = ExpenseVersionSync.withKnown(versionHistory, localDoc?.revisions.orEmpty() + remoteDoc?.revisions.orEmpty())
        val versionDecisions = mutableMapOf<String, com.splitmate.app.ExpenseVersioning.Decision>()
        // Review F4: compare copies by device-independent member keys (phone number; member id mapped
        // through the merge's remap only when there is no phone), so the same copy hashes the same on
        // every phone, matching the fingerprints recorded anywhere.
        // Review R5: one resolver over both documents' members, so a member without a phone on one side
        // resolves through the merge's remap to the matched member's phone.
        val allDocMembers = localDoc?.members.orEmpty() + remoteDoc?.members.orEmpty()
        val localMemberKey = com.splitmate.app.ExpenseVersioning.memberKeyOf(allDocMembers) { memberIdRemap[it] ?: it }
        val remoteMemberKey = localMemberKey
        for ((id, localExp) in localExpenseById) {
            val remoteExp = remoteExpenseById[id] ?: continue
            versionDecisions[id] = com.splitmate.app.ExpenseVersioning.decide(
                local = localExp,
                localSplits = localSplitsByExpense[id].orEmpty(),
                remote = remoteExp,
                remoteSplits = remoteSplitsByExpense[id].orEmpty(),
                localDocWins = localDocWins,
                history = effectiveHistory,
                localKey = localMemberKey,
                remoteKey = remoteMemberKey
            )
        }
        val localWinsExpense: (String) -> Boolean = { id ->
            versionDecisions[id]?.side?.let { it == com.splitmate.app.ExpenseVersioning.Side.LOCAL } ?: localDocWins
        }
        val combinedExpenses = (localDoc?.expenses.orEmpty() + remoteDoc?.expenses.orEmpty())
            .filter { it.expenseId !in mergedDeletedExpenseIds }
            .groupBy { it.expenseId }
            .map { (expenseId, _) ->
                val fromLocal = localExpenseById[expenseId]
                val fromRemote = remoteExpenseById[expenseId]
                val decision = versionDecisions[expenseId]
                val chosenRaw = if (fromLocal != null && fromRemote != null) {
                    val winner = if (localWinsExpense(expenseId)) fromLocal else fromRemote
                    val stamp = decision?.stampVersion
                    if (stamp != null) {
                        winner.copy(rowVersion = stamp, rowUpdatedBy = com.splitmate.app.ExpenseVersioning.LEGACY_EDITOR)
                    } else {
                        winner
                    }
                } else {
                    fromLocal ?: fromRemote!!
                }
                // v2.3.5: an old client that edits an expense drops categoryRef/createdByPhone; keep the
                // other side's value instead of erasing it (never touches any money field).
                val loser = when {
                    fromLocal == null || fromRemote == null -> null
                    localWinsExpense(expenseId) -> fromRemote
                    else -> fromLocal
                }
                val chosen = if (loser != null &&
                    ((chosenRaw.categoryRef == null && loser.categoryRef != null) ||
                        (chosenRaw.createdByPhone == null && loser.createdByPhone != null))
                ) {
                    chosenRaw.copy(
                        categoryRef = chosenRaw.categoryRef ?: loser.categoryRef,
                        createdByPhone = chosenRaw.createdByPhone ?: loser.createdByPhone
                    )
                } else {
                    chosenRaw
                }
                val remappedPayerId = memberIdRemap[chosen.payerId] ?: chosen.payerId
                val finalPayerId = if (remappedPayerId !in survivingMemberIds && organizerMemberId.isNotBlank()) {
                    organizerMemberId
                } else {
                    remappedPayerId
                }
                if (finalPayerId != chosen.payerId) {
                    chosen.copy(payerId = finalPayerId)
                } else {
                    chosen
                }
            }

        // 4. Merge splits (remapping cross-device memberId) and enforce PA-6 (subset-only zero-drift redistribution when a participant was removed)
        val survivingExpenseMap = combinedExpenses.associateBy { it.expenseId }.toMutableMap()
        // v2.3.5 (#4 guard c): for an expense present on both sides, keep only the winning side's split
        // rows (same LWW choice as the expense row). The old union resurrected a removed participant's
        // stale row from the losing side, so the splits no longer summed to the expense total.
        // v2.4.0 P1: per expense, all or nothing, following the expense row's winner.
        val (winnerLocalSplits, winnerRemoteSplits) = com.splitmate.app.ExpenseSplitMergeRules.keepWinningSideSplits(
            localSplits = localDoc?.splits.orEmpty(),
            remoteSplits = remoteDoc?.splits.orEmpty(),
            localExpenseIds = localExpenseById.keys,
            remoteExpenseIds = remoteExpenseById.keys,
            localWinsFor = localWinsExpense
        )
        val rawMergedSplits = (winnerLocalSplits + winnerRemoteSplits)
            .filter { it.expenseId in survivingExpenseMap }
            .map { sp ->
                val remappedMid = memberIdRemap[sp.memberId] ?: sp.memberId
                if (remappedMid != sp.memberId) sp.copy(memberId = remappedMid) else sp
            }
            .groupBy { "${it.expenseId}|${it.memberId}" }
            // v2.4.0 P1: only the winning side's rows reach this point for any expense, so the old
            // `find { it.splitId in remoteSplitKeySet }` (which found the LOCAL row again when ids
            // matched: the RCA root cause) is gone. Duplicate member rows within one side keep the first.
            .map { (_, group) -> group.first() }

        val finalSplits = mutableListOf<ExpenseSplitEntity>()
        val splitsByExpenseId = rawMergedSplits.groupBy { it.expenseId }
        for (exp in combinedExpenses) {
            val expSplits = splitsByExpenseId[exp.expenseId].orEmpty()
            val survivingExpSplits = expSplits.filter { it.memberId in survivingMemberIds }
            val removedExpSplits = expSplits.filter { it.memberId !in survivingMemberIds }

            if (removedExpSplits.isEmpty()) {
                // Deduplicate by memberId in case both local and remote created different splitIds for same (expenseId, memberId)
                val dedupedByMember = survivingExpSplits
                    .groupBy { it.memberId }
                    .map { (_, list) -> list.first() }
                finalSplits.addAll(dedupedByMember)
            } else {
                // PA-6: At least one participant of this expense was removed.
                // Redistribute strictly across the remaining active participants of THIS expense (or fallback to active group members).
                val remainingSubsetIds = survivingExpSplits
                    .map { it.memberId }
                    .distinct()
                    .filter { mid ->
                        val mem = memberMap.values.find { it.memberId == mid }
                        mem != null && !mem.inviteStatus.equals("DECLINED", ignoreCase = true)
                    }
                    .ifEmpty {
                        activeSurvivingMemberIds.ifEmpty { survivingMemberIds.toList() }
                    }

                if (remainingSubsetIds.isNotEmpty()) {
                    val mergedMemberById = memberMap.values.associateBy { it.memberId }
                    val subsetMemberPairs = remainingSubsetIds.map { id ->
                        id to (mergedMemberById[id]?.name ?: id)
                    }
                    val equalTotalAllocations = SplitMateMathEngine.splitEquallyZeroDrift(
                        totalCents = exp.totalAmountCents,
                        members = subsetMemberPairs,
                        payerId = exp.payerId
                    ).associateBy { it.memberId }
                    val equalBaseAllocations = SplitMateMathEngine.splitEquallyZeroDrift(
                        totalCents = exp.baseSubtotalCents,
                        members = subsetMemberPairs,
                        payerId = exp.payerId
                    ).associateBy { it.memberId }
                    val existingByMember = survivingExpSplits.associateBy { it.memberId }
                    for (memberId in remainingSubsetIds) {
                        val totalAlloc = equalTotalAllocations[memberId]
                        val baseAlloc = equalBaseAllocations[memberId]
                        val preservedSplitId = existingByMember[memberId]?.splitId ?: "${exp.expenseId}_$memberId"
                        finalSplits.add(
                            ExpenseSplitEntity(
                                splitId = preservedSplitId,
                                expenseId = exp.expenseId,
                                memberId = memberId,
                                baseClaimedCents = baseAlloc?.baseClaimedCents ?: 0L,
                                finalOwedCents = totalAlloc?.finalCents ?: 0L,
                                plusOneCent = totalAlloc?.plusOneCent == true
                            )
                        )
                    }
                    if (exp.unassignedBaseCents != 0L) {
                        survivingExpenseMap[exp.expenseId] = exp.copy(unassignedBaseCents = 0L)
                    }
                }
            }
        }
        val finalExpenses = combinedExpenses.map { survivingExpenseMap[it.expenseId] ?: it }

        // 5. Merge settlements, remap cross-device memberIds, and prune tombstoned or orphaned settlements (FK safety)
        val localSettleIdSet = localDoc?.settlements.orEmpty().map { it.settlementId }.toSet()
        val remoteSettleIdSet = remoteDoc?.settlements.orEmpty().map { it.settlementId }.toSet()
        val allSettlements = (localDoc?.settlements.orEmpty() + remoteDoc?.settlements.orEmpty())
            .map { st ->
                val mappedFrom = memberIdRemap[st.fromMemberId] ?: st.fromMemberId
                val mappedTo = memberIdRemap[st.toMemberId] ?: st.toMemberId
                if (mappedFrom != st.fromMemberId || mappedTo != st.toMemberId) {
                    st.copy(fromMemberId = mappedFrom, toMemberId = mappedTo)
                } else {
                    st
                }
            }
            .filter {
                it.settlementId !in mergedDeletedSettlementIds &&
                    it.fromMemberId in survivingMemberIds &&
                    it.toMemberId in survivingMemberIds
            }
            .groupBy { it.settlementId }
            .map { (_, group) ->
                val fromLocal = if (localDoc != null) group.find { it.settlementId in localSettleIdSet } else null
                val fromRemote = if (remoteDoc != null) group.find { it.settlementId in remoteSettleIdSet } else null
                if (fromLocal != null && fromRemote != null) {
                    if (localDoc!!.updatedAtEpochMs >= remoteDoc!!.updatedAtEpochMs) fromLocal else fromRemote
                } else {
                    fromLocal ?: fromRemote ?: group.first()
                }
            }

        // 6. Bind local perspective & avatar seed (PA-8)
        val localPreferredSeed = localUserAvatarSeed.takeIf { it.isNotBlank() }
            ?: localDoc?.members?.firstOrNull { m ->
                val normPhone = PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId)
                (normLocalPhone.length == 10 && normPhone == normLocalPhone) ||
                    (normPhone.isEmpty() && m.isCurrentUser && m.memberId.endsWith("_me"))
            }?.avatarSeed?.takeIf { it.contains("|") }.orEmpty()

        val localMeMemberIds = localDoc?.members?.filter { it.isCurrentUser }?.map { it.memberId }?.toSet().orEmpty()
        val hasPhoneMatchedLocalUser = normLocalPhone.length == 10 && memberMap.values.any {
            PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == normLocalPhone
        }
        val mergedMembers = memberMap.values.map { m ->
            val normPhone = PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId)
            val isMe = if (hasPhoneMatchedLocalUser) {
                normPhone == normLocalPhone
            } else if (localDoc != null) {
                (normPhone == normLocalPhone && normLocalPhone.length == 10) ||
                    (normPhone.isEmpty() && m.memberId in localMeMemberIds)
            } else {
                false
            }
            val shouldApplyLocalSeed = (normLocalPhone.length == 10 && normPhone == normLocalPhone) ||
                (normPhone.isEmpty() && isMe && m.memberId.endsWith("_me"))
            m.copy(
                userPhone = normPhone.ifBlank { m.userPhone },
                isCurrentUser = isMe,
                avatarSeed = if (shouldApplyLocalSeed && localPreferredSeed.isNotBlank()) localPreferredSeed else m.avatarSeed
            )
        }

        val mergedFlights = localDoc?.flightVaultByPnr.orEmpty().toMutableMap()
        remoteDoc?.flightVaultByPnr?.forEach { (k, v) -> mergedFlights[k] = v }
        val mergedTrains = localDoc?.trainSnapshotByPnr.orEmpty().toMutableMap()
        remoteDoc?.trainSnapshotByPnr?.forEach { (k, v) -> mergedTrains[k] = v }

        val mergedPresence = mutableMapOf<String, Long>()
        (localDoc?.memberPresenceByPhone?.keys.orEmpty() + remoteDoc?.memberPresenceByPhone?.keys.orEmpty()).forEach { p ->
            val normP = PhoneIdentityValidator.normalizeIndianPhone10(p)
            if (normP.length == 10 && normP !in mergedRemovedMemberPhones) {
                mergedPresence[normP] = max(
                    localDoc?.memberPresenceByPhone?.get(p) ?: localDoc?.memberPresenceByPhone?.get(normP) ?: 0L,
                    remoteDoc?.memberPresenceByPhone?.get(p) ?: remoteDoc?.memberPresenceByPhone?.get(normP) ?: 0L
                )
            }
        }
        if (normLocalPhone.length == 10 && normLocalPhone !in mergedRemovedMemberPhones) {
            mergedPresence[normLocalPhone] = max(mergedPresence[normLocalPhone] ?: 0L, localPresenceEpochMs)
        }

        val chosenGroup = when {
            localDoc != null && remoteDoc != null ->
                if (localDoc.updatedAtEpochMs >= remoteDoc.updatedAtEpochMs) localDoc.group else remoteDoc.group
            localDoc != null -> localDoc.group
            else -> remoteDoc!!.group
        }

        // v2.4.0 P2: no blind repair after a merge (RCA risk R4: it trusted the shares, and in the mirror
        // case that undoes an edit). With P1 each expense's row and shares come from one copy; if that
        // copy is itself inconsistent it is quarantined (flagged on upload, shown by the Money check).
        val repairedExpenses = finalExpenses
        val (cleanSettlements, cleanDeletedSettlementIds) = dropRedundantDuplicateSettlements(
            repairedExpenses, finalSplits, allSettlements, mergedDeletedSettlementIds
        )

        return CloudGroupLedgerDocument(
            group = chosenGroup,
            members = mergedMembers,
            expenses = repairedExpenses,
            splits = finalSplits,
            settlements = cleanSettlements,
            deletedExpenseIds = mergedDeletedExpenseIds,
            flightVaultByPnr = mergedFlights,
            trainSnapshotByPnr = mergedTrains,
            updatedAtEpochMs = max(localDoc?.updatedAtEpochMs ?: 0L, remoteDoc?.updatedAtEpochMs ?: 0L),
            memberPresenceByPhone = mergedPresence,
            organizerPhone10 = resolvedOrganizerPhone,
            joinCode6 = resolvedJoinCode,
            deletedMemberIds = mergedDeletedMemberIds,
            removedMemberPhones = mergedRemovedMemberPhones,
            deletedSettlementIds = cleanDeletedSettlementIds,
            organizerRolesByKey = mergedOrganizerRolesByKey,
            customCategories = mergedCustomCategories,
            tripLifecycle = mergedTripLifecycle,
            revisions = ExpenseVersionSync.mergeSyncedRevisions(
                localDoc?.revisions.orEmpty(), remoteDoc?.revisions.orEmpty(), mergedDeletedExpenseIds.keys
            )
        )
    }

    /**
     * v2.3.6: removes payments recorded twice (see [com.splitmate.app.SettlementDuplicateGuard.redundantDuplicateIds])
     * and tombstones them. The tombstone time is the duplicate's own time + 1ms, so every phone builds
     * the same document, and older builds that honour `deletedSettlementIds` also drop it.
     */
    private fun dropRedundantDuplicateSettlements(
        expenses: List<ExpenseEntity>,
        splits: List<ExpenseSplitEntity>,
        settlements: List<SettlementEntity>,
        deletedSettlementIds: Map<String, Long>
    ): Pair<List<SettlementEntity>, Map<String, Long>> {
        val duplicateIds = com.splitmate.app.SettlementDuplicateGuard.redundantDuplicateIds(expenses, splits, settlements)
        if (duplicateIds.isEmpty()) return settlements to deletedSettlementIds
        val tombstones = deletedSettlementIds.toMutableMap()
        settlements.filter { it.settlementId in duplicateIds }.forEach { dup ->
            tombstones[dup.settlementId] = max(tombstones[dup.settlementId] ?: 0L, dup.settledAt + 1L)
        }
        return settlements.filter { it.settlementId !in duplicateIds } to tombstones
    }

    private fun adjustMembersForLocalUser(
        doc: CloudGroupLedgerDocument,
        localUserPhone10: String,
        localUserAvatarSeed: String = "",
        isRemoteOnly: Boolean = false,
        localPresenceEpochMs: Long = System.currentTimeMillis()
    ): CloudGroupLedgerDocument {
        val normLocalPhone = PhoneIdentityValidator.normalizeIndianPhone10(localUserPhone10)
        val updatedPresence = doc.memberPresenceByPhone.toMutableMap()
        if (normLocalPhone.length == 10 && normLocalPhone !in doc.removedMemberPhones) {
            updatedPresence[normLocalPhone] = max(updatedPresence[normLocalPhone] ?: 0L, localPresenceEpochMs)
        }
        if (normLocalPhone.length != 10 && localUserAvatarSeed.isBlank() && !isRemoteOnly) {
            return doc.copy(memberPresenceByPhone = updatedPresence)
        }
        val hasPhoneMatch = normLocalPhone.length == 10 && doc.members.any {
            PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == normLocalPhone
        }
        val adjustedMembers = doc.members.map { m ->
            val normPhone = PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId)
            val isMe = when {
                hasPhoneMatch -> normPhone == normLocalPhone
                !isRemoteOnly -> normPhone.isEmpty() && m.isCurrentUser
                else -> false
            }
            val shouldApplyLocalSeed = (normLocalPhone.length == 10 && normPhone == normLocalPhone) ||
                (normPhone.isEmpty() && isMe && m.memberId.endsWith("_me"))
            m.copy(
                userPhone = normPhone.ifBlank { m.userPhone },
                isCurrentUser = isMe,
                avatarSeed = if (shouldApplyLocalSeed && localUserAvatarSeed.isNotBlank()) localUserAvatarSeed else m.avatarSeed
            )
        }
        return doc.copy(
            members = adjustedMembers,
            memberPresenceByPhone = updatedPresence
        )
    }

    suspend fun pushUserProfileToCloud(
        profile: UserProfileEntity,
        avatarStyle: String = "open-peeps",
        avatarColorPreset: String = "Buckwheat"
    ): Boolean {
        val phone10 = PhoneIdentityValidator.normalizeIndianPhone10(profile.userPhone)
        if (phone10.isEmpty()) return false
        val topic = "splitmate_v2_u_$phone10"
        val record = CloudUserProfileRecord(
            phone10 = phone10,
            name = profile.name,
            handle = profile.name.lowercase().replace(" ", "_"),
            upiVpa = profile.upiId,
            avatarStyle = avatarStyle,
            avatarColorPreset = avatarColorPreset,
            pinHash = profile.pinHash,
            updatedAtEpochMs = System.currentTimeMillis(),
            avatarSeed = profile.avatarSeed
        )
        return pushNtfySnapshot(topic, encodeUserProfileRecord(record))
    }

    /**
     * Re-publishes the local user profile when it has expired from the ntfy cache, so that
     * signing in on a new device with the same phone number finds the account. Never overwrites
     * an existing cloud profile (another device may have newer data). Throttled to one GET per
     * [PROFILE_REFRESH_CHECK_INTERVAL_MS].
     */
    suspend fun refreshUserProfileInCloudIfExpired(profile: UserProfileEntity?, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (profile == null) return false
        val phone10 = PhoneIdentityValidator.normalizeIndianPhone10(profile.userPhone)
        if (phone10.length != 10) return false
        if (nowMs - lastProfileRefreshCheckEpochMs < PROFILE_REFRESH_CHECK_INTERVAL_MS) return false
        lastProfileRefreshCheckEpochMs = nowMs
        val lines = executeNtfyGetWithBackoff("https://ntfy.sh/splitmate_v2_u_$phone10/json?poll=1&since=all")
        val fetchSucceeded = lines != null
        val exists = lines.orEmpty().any { line ->
            line.isNotBlank() && runCatching { JSONObject(line).optString("event") == "message" }.getOrDefault(false)
        }
        if (!shouldRepublishProfile(fetchSucceeded, exists)) return false
        val (style, preset) = parseAvatarStyleAndPreset(profile.avatarSeed)
        return pushUserProfileToCloud(profile, avatarStyle = style, avatarColorPreset = preset)
    }

    suspend fun fetchUserProfileFromCloud(phone10: String): CloudUserProfileRecord? {
        val normPhone = PhoneIdentityValidator.normalizeIndianPhone10(phone10)
        if (normPhone.isEmpty()) return null
        val topic = "splitmate_v2_u_$normPhone"
        val json = fetchNtfySnapshot(topic) ?: return null
        return decodeUserProfileRecord(json)
    }

    suspend fun pushCrossDeviceOtpChallengeToVerifiedPrimary(
        phone10: String,
        encryptedChallengeJson: String
    ): Boolean {
        val normPhone = PhoneIdentityValidator.normalizeIndianPhone10(phone10)
        if (normPhone.isEmpty() || encryptedChallengeJson.isBlank()) return false
        val existingProfile = fetchUserProfileFromCloud(normPhone) ?: return false
        if (existingProfile.pinHash.isBlank()) return false
        val topic = "splitmate_v2_otp_push_$normPhone"
        return pushNtfySnapshot(topic, encryptedChallengeJson)
    }

    suspend fun pushPhoneIndexEntry(entry: CloudPhoneGroupIndexEntry, targetPhone10: String): Boolean {
        val normPhone = PhoneIdentityValidator.normalizeIndianPhone10(targetPhone10)
        if (normPhone.isEmpty()) return false
        val idxTopic = "splitmate_v2_idx_$normPhone"
        return pushNtfySnapshot(idxTopic, encodePhoneIndexEntry(entry))
    }

    suspend fun pushJoinCodePointer(entry: CloudJoinCodeIndexEntry): Boolean {
        val normCode = normalizeJoinCode6(entry.code6).ifBlank { deriveGroupJoinCode6(entry.groupId) }
        if (normCode.length != 6 || entry.groupId.isBlank()) return false
        val codeTopic = "splitmate_v2_code_$normCode"
        return pushNtfySnapshot(codeTopic, encodeJoinCodeIndexEntry(entry.copy(code6 = normCode)))
    }

    suspend fun fetchJoinCodePointer(code6: String): CloudJoinCodeIndexEntry? {
        val normCode = normalizeJoinCode6(code6)
        if (normCode.length != 6) return null
        val codeTopic = "splitmate_v2_code_$normCode"
        val raw = fetchNtfySnapshot(codeTopic) ?: return null
        return decodeJoinCodeIndexEntry(raw)
    }

    suspend fun joinGroupByCodeFromCloud(
        context: Context?,
        dao: SplitMateDao,
        rawCode6: String,
        explicitGroupId: String = "",
        localUserPhone10: String,
        localUserName: String,
        localUserAvatarSeed: String = ""
    ): CloudGroupLedgerDocument? = syncMutex.withLock {
        val ctx = resolveContext(context)
        val normCode = normalizeJoinCode6(rawCode6)
        val pointer = if (normCode.length == 6) fetchJoinCodePointer(normCode) else null
        val targetGroupId = explicitGroupId.trim().ifBlank { pointer?.groupId.orEmpty() }
        if (targetGroupId.isBlank()) return@withLock null

        val normPhone = PhoneIdentityValidator.normalizeIndianPhone10(localUserPhone10)
            .ifBlank {
                PhoneIdentityValidator.normalizeIndianPhone10(dao.getUserProfile()?.userPhone.orEmpty())
            }
        val cleanName = localUserName.trim().ifBlank { dao.getUserProfile()?.name?.trim().orEmpty() }.ifBlank { "Member" }
        val cleanSeed = localUserAvatarSeed.trim().ifBlank {
            dao.getUserProfile()?.avatarSeed?.trim().orEmpty()
        }.ifBlank { "$cleanName|Neutral|open-peeps|Buckwheat" }

        // Un-tombstone local phone if user is explicitly re-joining via Join Code
        if (normPhone.length == 10) {
            unTombstonePhones(ctx, targetGroupId, listOf(normPhone))
        }

        val localProfile = dao.getUserProfile()
        val localAvatarSeed = cleanSeed.ifBlank { localProfile?.avatarSeed?.trim().orEmpty() }
        val topic = "splitmate_v2_grp_${sanitizeTopicKey(targetGroupId)}"
        val (remoteDoc, _) = fetchRemoteGroupLedgerOutcome(
            topic = topic,
            localUserPhone10 = normPhone,
            localUserAvatarSeed = localAvatarSeed
        )
        if (remoteDoc == null && dao.getGroupById(targetGroupId) == null) {
            return@withLock null
        }

        if (remoteDoc != null) {
            val nowMs = System.currentTimeMillis()
            val cleanedRemovedPhones = if (normPhone.length == 10) {
                remoteDoc.removedMemberPhones - normPhone
            } else {
                remoteDoc.removedMemberPhones
            }
            val existingByPhone = if (normPhone.length == 10) {
                remoteDoc.members.firstOrNull {
                    PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == normPhone
                }
            } else null
            val existingByName = if (existingByPhone == null) {
                remoteDoc.members.firstOrNull { m ->
                    PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId).isEmpty() &&
                        m.name.trim().equals(cleanName, ignoreCase = true)
                }
            } else null

            val updatedRemoteMembers = when {
                existingByPhone != null -> remoteDoc.members.map { m ->
                    if (m.memberId == existingByPhone.memberId) {
                        m.copy(
                            name = cleanName.ifBlank { m.name },
                            userPhone = normPhone,
                            inviteStatus = "JOINED",
                            isCurrentUser = true,
                            avatarSeed = cleanSeed.ifBlank { m.avatarSeed }
                        )
                    } else {
                        m.copy(isCurrentUser = false)
                    }
                }
                existingByName != null -> remoteDoc.members.map { m ->
                    if (m.memberId == existingByName.memberId) {
                        m.copy(
                            userPhone = normPhone.ifBlank { m.userPhone },
                            inviteStatus = "JOINED",
                            isCurrentUser = true,
                            avatarSeed = cleanSeed.ifBlank { m.avatarSeed }
                        )
                    } else {
                        m.copy(isCurrentUser = false)
                    }
                }
                else -> {
                    val newMemberId = "m_${nowMs}_${normPhone.takeLast(4).ifBlank { "join" }}"
                    remoteDoc.members.map { it.copy(isCurrentUser = false) } + GroupMemberEntity(
                        memberId = newMemberId,
                        groupId = targetGroupId,
                        name = cleanName,
                        avatarSeed = cleanSeed,
                        isCurrentUser = true,
                        upiId = "",
                        userPhone = normPhone,
                        inviteStatus = "JOINED"
                    )
                }
            }

            // Persist initial joined group & members locally; syncGroupWithCloudInternal will merge & persist expenses/splits/settlements in topological FK order
            dao.insertGroup(remoteDoc.group)
            dao.insertMembers(updatedRemoteMembers)
            setLastLocalMutationEpoch(ctx, targetGroupId, nowMs)
            recordLocalTombstones(
                context = ctx,
                groupId = targetGroupId,
                deletedExpenseIds = remoteDoc.deletedExpenseIds,
                deletedMemberIds = remoteDoc.deletedMemberIds,
                removedMemberPhones = cleanedRemovedPhones,
                deletedSettlementIds = remoteDoc.deletedSettlementIds,
                organizerPhone10 = remoteDoc.organizerPhone10,
                joinCode6 = remoteDoc.joinCode6.ifBlank { normCode }
            )
        }

        return@withLock syncGroupWithCloudInternal(
            context = ctx,
            dao = dao,
            groupId = targetGroupId,
            localUserPhone10 = normPhone,
            localUserName = cleanName,
            additionalTombstones = emptyMap(),
            additionalDeletedMemberIds = emptyMap(),
            additionalRemovedMemberPhones = emptyMap(),
            additionalDeletedSettlementIds = emptyMap(),
            forceIndexPush = true,
            isLocalMutation = true
        )
    }

    suspend fun syncGroupWithCloud(
        context: Context?,
        dao: SplitMateDao,
        groupId: String,
        localUserPhone10: String,
        localUserName: String,
        additionalTombstones: Map<String, Long> = emptyMap(),
        additionalDeletedMemberIds: Map<String, Long> = emptyMap(),
        additionalRemovedMemberPhones: Map<String, Long> = emptyMap(),
        additionalDeletedSettlementIds: Map<String, Long> = emptyMap(),
        forceIndexPush: Boolean = false,
        isLocalMutation: Boolean = true
    ): CloudGroupLedgerDocument? = syncMutex.withLock {
        syncGroupWithCloudInternal(
            context = context,
            dao = dao,
            groupId = groupId,
            localUserPhone10 = localUserPhone10,
            localUserName = localUserName,
            additionalTombstones = additionalTombstones,
            additionalDeletedMemberIds = additionalDeletedMemberIds,
            additionalRemovedMemberPhones = additionalRemovedMemberPhones,
            additionalDeletedSettlementIds = additionalDeletedSettlementIds,
            forceIndexPush = forceIndexPush,
            isLocalMutation = isLocalMutation
        )
    }

    private suspend fun syncGroupWithCloudInternal(
        context: Context?,
        dao: SplitMateDao,
        groupId: String,
        localUserPhone10: String,
        localUserName: String,
        additionalTombstones: Map<String, Long>,
        additionalDeletedMemberIds: Map<String, Long>,
        additionalRemovedMemberPhones: Map<String, Long>,
        additionalDeletedSettlementIds: Map<String, Long>,
        forceIndexPush: Boolean,
        isLocalMutation: Boolean = true
    ): CloudGroupLedgerDocument? {
        val ctx = resolveContext(context)
        val localProfile = dao.getUserProfile()
        val normLocalPhone = PhoneIdentityValidator.normalizeIndianPhone10(localUserPhone10)
            .ifBlank {
                PhoneIdentityValidator.normalizeIndianPhone10(localProfile?.userPhone.orEmpty())
            }
        val localAvatarSeed = localProfile?.avatarSeed?.takeIf { it.isNotBlank() }.orEmpty()

        // Persist any newly supplied tombstones into durable LocalTombstoneStore before network I/O (PA-3)
        val durableTombstones = recordLocalTombstones(
            context = ctx,
            groupId = groupId,
            deletedExpenseIds = additionalTombstones,
            deletedMemberIds = additionalDeletedMemberIds,
            removedMemberPhones = additionalRemovedMemberPhones,
            deletedSettlementIds = additionalDeletedSettlementIds
        )

        val nowMs = System.currentTimeMillis()
        val rawExplicitMutation = isLocalMutation ||
            forceIndexPush ||
            additionalTombstones.isNotEmpty() ||
            additionalDeletedMemberIds.isNotEmpty() ||
            additionalRemovedMemberPhones.isNotEmpty() ||
            additionalDeletedSettlementIds.isNotEmpty()

        // v2.4.0 review N3: the trip's last local change BEFORE this sync re-stamps it (dates PENDING rows).
        val priorLocalChangeMs = getLastLocalMutationEpoch(ctx, groupId).takeIf { it > 0L }

        // Mark group as having a pending cloud push BEFORE network I/O so if the device is offline,
        // the pending mutation is durably remembered and automatically pushed the instant internet returns!
        if (rawExplicitMutation) {
            setLastLocalMutationEpoch(ctx, groupId, nowMs)
            setGroupPendingCloudPush(ctx, groupId, true)
        }

        val topic = "splitmate_v2_grp_${sanitizeTopicKey(groupId)}"
        // v2.4.0 P1: versions this phone already knows, used while folding the recent cloud snapshots
        // (an older app's stale copy among them loses to a newer version instead of winning by time).
        val preMergeHistory = ExpenseVersionSync.loadHistory(dao, groupId)
        val (remoteDoc, remoteFetchSucceeded) = fetchRemoteGroupLedgerOutcome(
            topic = topic,
            localUserPhone10 = normLocalPhone,
            localUserAvatarSeed = localAvatarSeed,
            versionHistory = preMergeHistory
        )
        var mergeHistory: com.splitmate.app.ExpenseVersioning.History? = preMergeHistory

        val localGroup = dao.getGroupById(groupId)
        var rawGroupMembers: List<GroupMemberEntity>? = null
        var localDoc: CloudGroupLedgerDocument? = null
        var hasPendingLocalEntities = false

        if (localGroup != null) {
            val loadedMembers = dao.getMembersForGroup(groupId)
            rawGroupMembers = loadedMembers
            val groupAlreadyHasLocalPhone = normLocalPhone.length == 10 && loadedMembers.any {
                PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == normLocalPhone
            }
            val members = loadedMembers.map { m ->
                val extractedPhone = PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId)
                val isTrueLocalOwner = !groupAlreadyHasLocalPhone &&
                    m.isCurrentUser &&
                    extractedPhone.isBlank() &&
                    normLocalPhone.length == 10 &&
                    (m.memberId.endsWith("_me") || (localUserName.isNotBlank() && m.name.trim().equals(localUserName.trim(), ignoreCase = true)))
                val finalPhone = if (isTrueLocalOwner) {
                    normLocalPhone
                } else {
                    extractedPhone.ifBlank { m.userPhone }
                }
                if (finalPhone != m.userPhone) m.copy(userPhone = finalPhone) else m
            }
            val rawExpenses = dao.getExpensesForGroup(groupId)
            val splits = dao.getSplitsForGroup(groupId)
            // v2.4.0 P1: give every expense edited on this phone since its last known version a new
            // hybrid-clock version (whichever screen made the edit) before merging.
            val prepared = ExpenseVersionSync.prepareLocalExpenses(
                dao = dao,
                groupId = groupId,
                expenses = rawExpenses,
                splits = splits,
                remoteDoc = remoteDoc,
                editorId = loadedMembers.firstOrNull { it.isCurrentUser }?.memberId ?: normLocalPhone,
                nowMs = nowMs,
                context = ctx,
                // Review R1: the same phone-patched member list the local document and merge use.
                members = members,
                lastLocalChangeMs = priorLocalChangeMs
            )
            val expenses = prepared.expenses
            mergeHistory = prepared.history
            val settlements = dao.getSettlementsForGroup(groupId)
            hasPendingLocalEntities = expenses.any { it.syncStatus.equals("PENDING", ignoreCase = true) } ||
                settlements.any { it.syncStatus.equals("PENDING", ignoreCase = true) }

            val flights = ctx?.let { PnrNetworkRepository.exportAllFlightVaultJsonByPnr(it) } ?: emptyMap()
            val trains = ctx?.let { PnrNetworkRepository.exportAllTrainSnapshotJsonByPnr(it) } ?: emptyMap()

            val initialPresence = if (normLocalPhone.length == 10) mapOf(normLocalPhone to nowMs) else emptyMap()
            val inferredOrgPhone = durableTombstones.organizerPhone10.ifBlank {
                val orgM = members.firstOrNull { it.memberId.endsWith("_me") }
                    ?: members.firstOrNull { it.isCurrentUser }
                    ?: members.firstOrNull()
                orgM?.let { PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) }.orEmpty()
            }
            val inferredCode6 = durableTombstones.joinCode6.ifBlank { deriveGroupJoinCode6(groupId) }

            val hadPendingPush = isGroupPendingCloudPush(ctx, groupId) || hasPendingLocalEntities
            val hasExplicitMutation = rawExplicitMutation || hadPendingPush

            // Crucial: During passive background polls (!hasExplicitMutation), do NOT stamp localDoc with nowMs > remoteDoc.updatedAtEpochMs,
            // otherwise local stale metadata would win LWW over remote updates and trigger a ping-pong POST storm across devices!
            val storedLocalMut = getLastLocalMutationEpoch(ctx, groupId)
            val effectiveLocalEpochMs = when {
                hasExplicitMutation -> max(nowMs, storedLocalMut)
                remoteDoc != null -> if (storedLocalMut > remoteDoc.updatedAtEpochMs) {
                    storedLocalMut
                } else {
                    (remoteDoc.updatedAtEpochMs - 1L).coerceAtLeast(1L)
                }
                storedLocalMut > 0L -> storedLocalMut
                else -> nowMs
            }

            localDoc = CloudGroupLedgerDocument(
                group = localGroup,
                members = members,
                expenses = expenses,
                splits = splits,
                settlements = settlements,
                deletedExpenseIds = durableTombstones.deletedExpenseIds,
                flightVaultByPnr = flights,
                trainSnapshotByPnr = trains,
                updatedAtEpochMs = effectiveLocalEpochMs,
                memberPresenceByPhone = initialPresence,
                organizerPhone10 = inferredOrgPhone,
                joinCode6 = inferredCode6,
                deletedMemberIds = durableTombstones.deletedMemberIds,
                removedMemberPhones = durableTombstones.removedMemberPhones,
                deletedSettlementIds = durableTombstones.deletedSettlementIds,
                organizerRolesByKey = durableTombstones.organizerRolesByKey,
                customCategories = GroupLedgerExtrasStore.load(ctx, groupId).customCategories,
                tripLifecycle = GroupLedgerExtrasStore.load(ctx, groupId).tripLifecycle,
                // v2.4.0 P4: this phone's history travels with the document (compact).
                revisions = ExpenseVersionSync.compactForSync(
                    runCatching { dao.getExpenseRevisionsForGroup(groupId) }.getOrDefault(emptyList()),
                    durableTombstones.deletedExpenseIds.keys
                )
            )
        }

        val hasExplicitMutation = rawExplicitMutation || isGroupPendingCloudPush(ctx, groupId) || hasPendingLocalEntities

        if (localDoc == null && remoteDoc == null) return null

        val mergedDoc = mergeGroupLedgerDocuments(
            localDoc = localDoc,
            remoteDoc = remoteDoc,
            localUserPhone10 = normLocalPhone,
            localUserAvatarSeed = localAvatarSeed,
            versionHistory = mergeHistory
        )

        // Persist merged tombstones durably (PA-3)
        recordLocalTombstones(
            context = ctx,
            groupId = groupId,
            deletedExpenseIds = mergedDoc.deletedExpenseIds,
            deletedMemberIds = mergedDoc.deletedMemberIds,
            removedMemberPhones = mergedDoc.removedMemberPhones,
            deletedSettlementIds = mergedDoc.deletedSettlementIds,
            organizerPhone10 = mergedDoc.organizerPhone10,
            joinCode6 = mergedDoc.joinCode6,
            organizerRolesByKey = mergedDoc.organizerRolesByKey
        )
        // v2.3.5: persist merged custom categories + trip lifecycle durably and publish to the UI.
        GroupLedgerExtrasStore.mergeAndSave(
            ctx,
            groupId,
            GroupLedgerExtras(customCategories = mergedDoc.customCategories, tripLifecycle = mergedDoc.tripLifecycle)
        )

        // PA-4 (3-Layer Removed-User Local Purge):
        // If localUserPhone10 is in mergedDoc.removedMemberPhones (and not an active surviving member),
        // push the merged tombstone state if needed, then purge the group from local Room DB and return null.
        val isLocalUserSurviving = normLocalPhone.length == 10 && mergedDoc.members.any {
            PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == normLocalPhone
        }
        val isLocalUserRemoved = normLocalPhone.length == 10 &&
            normLocalPhone in mergedDoc.removedMemberPhones &&
            !isLocalUserSurviving

        if (isLocalUserRemoved) {
            if (additionalDeletedMemberIds.isNotEmpty() || additionalRemovedMemberPhones.isNotEmpty()) {
                val pushMs = System.currentTimeMillis()
                val docToPush = mergedDoc.copy(updatedAtEpochMs = pushMs)
                pushNtfySnapshot(topic, encodeGroupLedgerDocument(docToPush))
                val removedIdxEntry = CloudPhoneGroupIndexEntry(
                    groupId = groupId,
                    groupName = mergedDoc.group.name,
                    inviterName = localUserName,
                    inviterPhone = normLocalPhone,
                    inviteStatus = "REMOVED",
                    updatedAtEpochMs = pushMs
                )
                pushPhoneIndexEntry(removedIdxEntry, normLocalPhone)
                setLastPushedIndexStatus(ctx, groupId, normLocalPhone, "REMOVED")
            }
            setGroupPendingCloudPush(ctx, groupId, false)
            dao.deleteGroupCascade(groupId)
            return null
        }

        // v2.3.6 P0 (RCA audit R2): steps 1-6 run in ONE Room transaction. Inside it the current rows
        // are re-read; any expense or settlement changed on this phone since the snapshot above (an
        // edit saved while this sync was merging) is skipped, so it can never be half-overwritten
        // (old total + new shares). The skipped edit stays local and is pushed by the next sync.
        val skippedExpenseIds = mutableSetOf<String>()
        val skippedSettlementIds = mutableSetOf<String>()
        val snapshotDoc = localDoc
        dao.runInLedgerTransaction {
            if (snapshotDoc != null) {
                skippedExpenseIds += com.splitmate.app.ConcurrentEditGuard.changedExpenseIds(
                    snapshotExpenses = snapshotDoc.expenses,
                    snapshotSplits = snapshotDoc.splits,
                    currentExpenses = dao.getExpensesForGroup(groupId),
                    currentSplits = dao.getSplitsForGroup(groupId)
                )
                skippedSettlementIds += com.splitmate.app.ConcurrentEditGuard.changedSettlementIds(
                    snapshot = snapshotDoc.settlements,
                    current = dao.getSettlementsForGroup(groupId)
                )
            }

            // Strict Topological Persistence Order into Room (PA-1 & PA-2):
            // Step 1: Upsert group & surviving members first so new payer/member FKs exist
            if (localGroup == null || localGroup != mergedDoc.group) {
                dao.insertGroup(mergedDoc.group)
            }
            if (rawGroupMembers == null || rawGroupMembers != mergedDoc.members) {
                dao.insertMembers(mergedDoc.members)
            }

            // Step 2: Delete tombstoned or non-surviving expenses & their splits (a delete always wins)
            val localExpIds = localDoc?.expenses?.map { it.expenseId }?.toSet().orEmpty()
            val survivingExpIds = mergedDoc.expenses.map { it.expenseId }.toSet()
            val expIdsToDelete = (mergedDoc.deletedExpenseIds.keys + localExpIds.filter { it !in survivingExpIds }).toSet()
            expIdsToDelete.forEach { deletedExpId ->
                if (localDoc == null || deletedExpId in localExpIds || additionalTombstones.containsKey(deletedExpId)) {
                    dao.deleteSplitsForExpense(deletedExpId)
                    dao.deleteExpense(deletedExpId)
                }
            }

            // Step 3: Upsert surviving expenses (including any payerId remapped or reassigned to Organizer per PA-1)
            val localExpMap = localDoc?.expenses?.associateBy { it.expenseId }.orEmpty()
            mergedDoc.expenses.filter { localExpMap[it.expenseId] != it && it.expenseId !in skippedExpenseIds }.forEach { exp ->
                runCatching { dao.insertExpense(exp) }
            }

            // Step 4: Atomically replace splits per expense when changed (PA-2)
            val localSplitsByExp = localDoc?.splits.orEmpty().groupBy { it.expenseId }
            val mergedSplitsByExp = mergedDoc.splits.groupBy { it.expenseId }
            for (exp in mergedDoc.expenses) {
                if (exp.expenseId in skippedExpenseIds) continue
                val oldSplits = localSplitsByExp[exp.expenseId].orEmpty()
                val newSplits = mergedSplitsByExp[exp.expenseId].orEmpty()
                if (localDoc == null || oldSplits.toSet() != newSplits.toSet()) {
                    runCatching { dao.replaceExpenseSplits(exp.expenseId, newSplits) }
                }
            }

            // Step 5: Delete tombstoned or orphaned settlements, then upsert surviving settlements
            val survivingSettleIds = mergedDoc.settlements.map { it.settlementId }.toSet()
            localDoc?.settlements.orEmpty().forEach { oldSettle ->
                if (oldSettle.settlementId !in survivingSettleIds || oldSettle.settlementId in mergedDoc.deletedSettlementIds) {
                    dao.deleteSettlementById(oldSettle.settlementId)
                }
            }
            mergedDoc.deletedSettlementIds.keys.forEach { delSettleId ->
                dao.deleteSettlementById(delSettleId)
            }
            val localSettleMap = localDoc?.settlements?.associateBy { it.settlementId }.orEmpty()
            mergedDoc.settlements.filter { localSettleMap[it.settlementId] != it && it.settlementId !in skippedSettlementIds }.forEach { settle ->
                runCatching { dao.insertSettlement(settle) }
            }

            // Step 6: Finally delete any tombstoned or remapped/removed members from Room (safe now that expenses/splits/settlements no longer reference them)
            val survivingMemberIdSet = mergedDoc.members.map { it.memberId }.toSet()
            val memberIdsToDelete = (rawGroupMembers.orEmpty().map { it.memberId }.filter { it !in survivingMemberIdSet } +
                mergedDoc.deletedMemberIds.keys).toSet()
            for (deadMemberId in memberIdsToDelete) {
                if (deadMemberId !in survivingMemberIdSet) {
                    runCatching {
                        dao.deleteSettlementsForMember(groupId, deadMemberId)
                        dao.deleteMemberById(deadMemberId)
                    }
                }
            }

            // v2.4.0 P1 + P4: remember every version this merge produced or saw (edit history + stale
            // copies) in the SAME transaction as the rows (review F5).
            ExpenseVersionSync.recordMergeOutcome(
                dao = dao,
                localDoc = localDoc,
                remoteDoc = remoteDoc,
                mergedDoc = mergedDoc,
                history = mergeHistory,
                skippedExpenseIds = skippedExpenseIds,
                nowMs = nowMs
            )
        }

        if (ctx != null) {
            val newFlights = mergedDoc.flightVaultByPnr.filter { (k, v) -> localDoc?.flightVaultByPnr?.get(k) != v }
            if (newFlights.isNotEmpty()) {
                PnrNetworkRepository.importFlightVaultJsonByPnr(ctx, newFlights)
            }
            val newTrains = mergedDoc.trainSnapshotByPnr.filter { (k, v) -> localDoc?.trainSnapshotByPnr?.get(k) != v }
            if (newTrains.isNotEmpty()) {
                PnrNetworkRepository.importTrainSnapshotJsonByPnr(ctx, newTrains)
            }
        }

        val postMergeMs = System.currentTimeMillis()
        val updatedPresence = mergedDoc.memberPresenceByPhone.toMutableMap()
        if (normLocalPhone.length == 10) {
            updatedPresence[normLocalPhone] = postMergeMs
        }
        val syncedExpenses = mergedDoc.expenses.map {
            if (it.syncStatus != "SYNCED") it.copy(syncStatus = "SYNCED") else it
        }
        val syncedSettlements = mergedDoc.settlements.map {
            if (it.syncStatus != "SYNCED") it.copy(syncStatus = "SYNCED") else it
        }
        val mergedDocUpdated = mergedDoc.copy(
            expenses = syncedExpenses,
            settlements = syncedSettlements,
            updatedAtEpochMs = if (hasExplicitMutation) postMergeMs else max(mergedDoc.updatedAtEpochMs, postMergeMs),
            memberPresenceByPhone = updatedPresence
        )

        // F2: Structural SHA-256 hash deduplication — never overwrite cloud when a passive GET failed (!remoteFetchSucceeded)
        val newStructuralHash = computeStructuralLedgerHash(mergedDocUpdated)
        val remoteStructuralHash = remoteDoc?.let { computeStructuralLedgerHash(it) }.orEmpty()
        val lastLocalPushedHash = getLastPushedGroupHash(ctx, groupId)
        val shouldPushLedger = when {
            forceIndexPush -> true
            !remoteFetchSucceeded && !hasExplicitMutation -> false
            remoteDoc == null -> hasExplicitMutation || remoteFetchSucceeded
            newStructuralHash != remoteStructuralHash -> true
            hasExplicitMutation && newStructuralHash != lastLocalPushedHash -> true
            else -> false
        }

        var cloudSyncedConfirmed = false
        if (shouldPushLedger) {
            val pushedOk = pushNtfySnapshot(topic, encodeGroupLedgerDocument(mergedDocUpdated))
            if (pushedOk) {
                setLastPushedGroupHash(ctx, groupId, newStructuralHash)
                setGroupPendingCloudPush(ctx, groupId, false)
                cloudSyncedConfirmed = true
            } else {
                // Network push failed (device offline or rate-limited); keep pending flag true so it auto-flushes when internet returns
                setGroupPendingCloudPush(ctx, groupId, true)
            }
        } else if (remoteDoc != null && newStructuralHash == remoteStructuralHash) {
            setLastPushedGroupHash(ctx, groupId, newStructuralHash)
            setGroupPendingCloudPush(ctx, groupId, false)
            cloudSyncedConfirmed = true
        }
        if (skippedExpenseIds.isNotEmpty() || skippedSettlementIds.isNotEmpty()) {
            // v2.3.6 P0: an edit made during this sync was kept locally; make sure the next sync pushes it.
            setGroupPendingCloudPush(ctx, groupId, true)
        } else if (cloudSyncedConfirmed) {
            lastConfirmedInSyncEpochByGroup[groupId] = postMergeMs
        }

        if (cloudSyncedConfirmed && hasPendingLocalEntities) {
            // v2.3.6 (real-data bug): only flip PENDING -> SYNCED on rows nobody changed while this sync
            // was on the network. Writing back the snapshot copy used to overwrite an edit made in the
            // meantime: a total edited ₹7,950 -> ₹4,950 came back as ₹7,950 while its new split rows
            // (₹825 each) stayed, and an undone settlement could be re-inserted.
            val currentExpenses = dao.getExpensesForGroup(groupId).associateBy { it.expenseId }
            syncedExpenses.filter { exp ->
                val mergedRow = mergedDoc.expenses.find { it.expenseId == exp.expenseId }
                mergedRow?.syncStatus != "SYNCED" && currentExpenses[exp.expenseId] == mergedRow
            }.forEach { exp ->
                runCatching { dao.insertExpense(exp) }
            }
            val currentSettlements = dao.getSettlementsForGroup(groupId).associateBy { it.settlementId }
            syncedSettlements.filter { settle ->
                val mergedRow = mergedDoc.settlements.find { it.settlementId == settle.settlementId }
                mergedRow?.syncStatus != "SYNCED" && currentSettlements[settle.settlementId] == mergedRow
            }.forEach { settle ->
                runCatching { dao.insertSettlement(settle) }
            }
        }

        // PA-7: Publish / refresh 6-character Join Code pointer on splitmate_v2_code_<CODE6>
        val lastCodeTs = getLastCodePublishEpoch(ctx, groupId)
        if (mergedDocUpdated.joinCode6.length == 6 &&
            (forceIndexPush || (remoteDoc == null && remoteFetchSucceeded) || (postMergeMs - lastCodeTs) >= CODE_POINTER_REFRESH_INTERVAL_MS)
        ) {
            val orgMember = mergedDocUpdated.members.firstOrNull {
                PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == mergedDocUpdated.organizerPhone10
            } ?: mergedDocUpdated.members.firstOrNull()
            val pointer = CloudJoinCodeIndexEntry(
                code6 = mergedDocUpdated.joinCode6,
                groupId = groupId,
                groupName = mergedDocUpdated.group.name,
                organizerName = orgMember?.name ?: localUserName,
                organizerPhone10 = mergedDocUpdated.organizerPhone10.ifBlank { normLocalPhone },
                updatedAtEpochMs = postMergeMs
            )
            if (pushJoinCodePointer(pointer)) {
                setLastCodePublishEpoch(ctx, groupId, postMergeMs)
            }
        }

        // F3: Delta Phone-Index Push — only push to splitmate_v2_idx_<phone10> on explicit mutations or initial creation
        val allowIndexNetworkPush = forceIndexPush || hasExplicitMutation || (remoteDoc == null && remoteFetchSucceeded)
        val pushedPhones = HashSet<String>()
        mergedDocUpdated.members.forEach { m ->
            val p = PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId)
            if (p.length == 10 && pushedPhones.add(p)) {
                val statusToken = "${m.inviteStatus.uppercase()}|${mergedDocUpdated.group.name}"
                val prevToken = getLastPushedIndexStatus(ctx, groupId, p)
                // v2.3.6: also re-publish when the last push may have expired from the ntfy cache,
                // otherwise a fresh install / new phone cannot discover its trips.
                when (
                    decidePhoneIndexPush(
                        nowMs = System.currentTimeMillis(),
                        lastPushedEpochMs = getLastPushedIndexEpoch(ctx, groupId, p),
                        statusChanged = statusToken != prevToken,
                        forcePush = forceIndexPush,
                        networkPushAllowed = allowIndexNetworkPush
                    )
                ) {
                    IndexPushDecision.PUSH -> {
                        val idxEntry = CloudPhoneGroupIndexEntry(
                            groupId = mergedDocUpdated.group.groupId,
                            groupName = mergedDocUpdated.group.name,
                            inviterName = localUserName,
                            inviterPhone = normLocalPhone,
                            inviteStatus = m.inviteStatus,
                            updatedAtEpochMs = mergedDocUpdated.updatedAtEpochMs
                        )
                        if (pushPhoneIndexEntry(idxEntry, p)) {
                            setLastPushedIndexStatus(ctx, groupId, p, statusToken)
                            setLastPushedIndexEpoch(ctx, groupId, p, System.currentTimeMillis())
                        }
                    }
                    IndexPushDecision.SEED_TOKEN_ONLY -> {
                        // Seed local token cache during passive poll so we don't redundantly POST on every poll
                        setLastPushedIndexStatus(ctx, groupId, p, statusToken)
                    }
                    IndexPushDecision.SKIP -> Unit
                }
            }
        }

        // Also push "REMOVED" index entry for newly tombstoned member phones so their devices purge immediately
        mergedDocUpdated.removedMemberPhones.keys.forEach { remPhone ->
            val p = PhoneIdentityValidator.normalizeIndianPhone10(remPhone)
            if (p.length == 10 && p !in pushedPhones) {
                val statusToken = "REMOVED|${mergedDocUpdated.group.name}"
                val prevToken = getLastPushedIndexStatus(ctx, groupId, p)
                if (forceIndexPush || additionalRemovedMemberPhones.containsKey(p) || (allowIndexNetworkPush && statusToken != prevToken)) {
                    val remEntry = CloudPhoneGroupIndexEntry(
                        groupId = mergedDocUpdated.group.groupId,
                        groupName = mergedDocUpdated.group.name,
                        inviterName = localUserName,
                        inviterPhone = normLocalPhone,
                        inviteStatus = "REMOVED",
                        updatedAtEpochMs = mergedDocUpdated.updatedAtEpochMs
                    )
                    if (pushPhoneIndexEntry(remEntry, p)) {
                        setLastPushedIndexStatus(ctx, groupId, p, statusToken)
                    }
                }
            }
        }

        return mergedDocUpdated
    }

    suspend fun restoreAndSyncAllForVerifiedPhone(
        context: Context?,
        dao: SplitMateDao,
        phone10: String
    ): CloudRestoreSummary = syncMutex.withLock {
        val ctx = resolveContext(context)
        val localProfile = dao.getUserProfile()
        val normPhone = PhoneIdentityValidator.normalizeIndianPhone10(phone10)
            .ifBlank {
                PhoneIdentityValidator.normalizeIndianPhone10(localProfile?.userPhone.orEmpty())
            }
        val localName = localProfile?.name ?: "You"

        if (localProfile != null &&
            PhoneIdentityValidator.normalizeIndianPhone10(localProfile.userPhone) == normPhone
        ) {
            runCatching { refreshUserProfileInCloudIfExpired(localProfile) }
        }

        // Purge untouched demo seed groups (using structured isDemoSeed column)
        if (normPhone.isNotEmpty()) {
            val allGroups = dao.getAllGroups()
            allGroups.filter { it.isDemoSeed }.forEach { g ->
                val exps = dao.getExpensesForGroup(g.groupId)
                if (exps.all { it.syncStatus == "SYNCED" && it.travelPnr.isBlank() } && exps.size <= 3) {
                    dao.deleteGroupCascade(g.groupId)
                }
            }
        }

        val uniqueEntries = if (normPhone.length == 10) {
            val topic = "splitmate_v2_idx_$normPhone"
            val messages = fetchAllNtfyMessages(topic)
            val entries = messages.mapNotNull { decodePhoneIndexEntry(it) }
            entries.groupBy { entry -> entry.groupId }
                .mapNotNull { (_, list) -> list.maxByOrNull { item -> item.updatedAtEpochMs } }
        } else {
            emptyList()
        }

        var joined = 0
        var pending = 0
        val aggregatedPresence = mutableMapOf<String, Long>()
        if (normPhone.length == 10) {
            aggregatedPresence[normPhone] = System.currentTimeMillis()
        }

        for (entry in uniqueEntries) {
            // Layer 1 of PA-4: If the latest index entry for this phone is REMOVED/LEFT or local tombstone marks phone removed, purge & skip
            if (entry.inviteStatus.uppercase() in setOf("REMOVED", "LEFT") ||
                (normPhone.length == 10 && isPhoneRemovedFromGroup(ctx, entry.groupId, normPhone))
            ) {
                if (normPhone.length == 10) {
                    recordLocalTombstones(
                        context = ctx,
                        groupId = entry.groupId,
                        removedMemberPhones = mapOf(normPhone to max(entry.updatedAtEpochMs, System.currentTimeMillis()))
                    )
                }
                dao.deleteGroupCascade(entry.groupId)
                continue
            }

            val syncedDoc = runCatching {
                syncGroupWithCloudInternal(
                    context = ctx,
                    dao = dao,
                    groupId = entry.groupId,
                    localUserPhone10 = normPhone,
                    localUserName = localName,
                    additionalTombstones = emptyMap(),
                    additionalDeletedMemberIds = emptyMap(),
                    additionalRemovedMemberPhones = emptyMap(),
                    additionalDeletedSettlementIds = emptyMap(),
                    forceIndexPush = false,
                    isLocalMutation = false
                )
            }.getOrNull() ?: continue

            syncedDoc.memberPresenceByPhone.forEach { (p, ts) ->
                val normP = PhoneIdentityValidator.normalizeIndianPhone10(p)
                if (normP.length == 10) {
                    aggregatedPresence[normP] = max(aggregatedPresence[normP] ?: 0L, ts)
                }
            }

            val members = dao.getMembersForGroup(entry.groupId)
            val myMember = members.find {
                normPhone.length == 10 && PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) == normPhone
            } ?: members.find { it.isCurrentUser }
            if (myMember != null) {
                when (myMember.inviteStatus.uppercase()) {
                    "JOINED" -> joined++
                    "PENDING" -> pending++
                }
            }
        }

        // Also sync any existing local non-seed groups (including groups joined via 6-char Join Code even if phone index had not arrived yet)
        val remainingLocalGroups = dao.getAllGroups().filter { !it.isDemoSeed }
        for (localGroup in remainingLocalGroups) {
            if (normPhone.length == 10 && isPhoneRemovedFromGroup(ctx, localGroup.groupId, normPhone)) {
                dao.deleteGroupCascade(localGroup.groupId)
                continue
            }
            if (uniqueEntries.none { it.groupId == localGroup.groupId }) {
                val syncedDoc = runCatching {
                    syncGroupWithCloudInternal(
                        context = ctx,
                        dao = dao,
                        groupId = localGroup.groupId,
                        localUserPhone10 = normPhone,
                        localUserName = localName,
                        additionalTombstones = emptyMap(),
                        additionalDeletedMemberIds = emptyMap(),
                        additionalRemovedMemberPhones = emptyMap(),
                        additionalDeletedSettlementIds = emptyMap(),
                        forceIndexPush = false,
                        isLocalMutation = false
                    )
                }.getOrNull() ?: continue

                syncedDoc.memberPresenceByPhone.forEach { (p, ts) ->
                    val normP = PhoneIdentityValidator.normalizeIndianPhone10(p)
                    if (normP.length == 10) {
                        aggregatedPresence[normP] = max(aggregatedPresence[normP] ?: 0L, ts)
                    }
                }
                joined++
            }
        }

        return@withLock CloudRestoreSummary(
            restoredJoinedGroupsCount = joined,
            discoveredPendingInvitesCount = pending,
            memberPresenceByPhone = aggregatedPresence
        )
    }
}


