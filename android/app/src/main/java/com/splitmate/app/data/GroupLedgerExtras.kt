package com.splitmate.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * v2.3.5 additive ledger extensions (decision log: #5 group categories, #1 trip lifecycle).
 *
 * WIRE CONTRACT (backward compatible):
 *  - Both keys are OPTIONAL in the group ledger JSON. They are only written when non-empty / present,
 *    and only folded into `computeStructuralLedgerHash` when non-empty / present, so the hash of every
 *    pre-v2.3.5 ledger is byte-for-byte unchanged.
 *  - Old clients ignore unknown keys and drop them when they push. Newer clients keep a durable local
 *    copy ([GroupLedgerExtrasStore]) and merge it back on every sync, so the data self-heals.
 *  - No money data lives here. Nothing in this file touches cents, splits or settlements.
 */
data class CloudCustomCategory(
    /** Stable id (`c_<hash>`), referenced by `ExpenseEntity.categoryRef = "custom:<id>"`. */
    val id: String,
    val title: String,
    val iconKey: String,
    /** Parent spend bucket name (e.g. `FOOD`, `LOCAL_TRANSPORT`); blank = auto-suggest from title/icon. */
    val parentBucket: String = "",
    val createdByPhone: String = "",
    /** LWW clock for this entry. */
    val updatedAtEpochMs: Long = 0L,
    /** Tombstone: a delete wins over any stale (older) re-add. */
    val deleted: Boolean = false
)

object TripLifecycleState {
    const val ACTIVE = "ACTIVE"
    const val ENDED = "ENDED"
}

data class TripLifecycleRecord(
    val state: String = TripLifecycleState.ACTIVE,
    val endedAtEpochMs: Long = 0L,
    val endedByPhone: String = "",
    val startDateEpochMs: Long? = null,
    val endDateEpochMs: Long? = null,
    /** Own LWW clock (independent of the ledger document clock). */
    val updatedAtEpochMs: Long = 0L,
    /**
     * v2.3.5 (#1): when an organizer last reopened the trip (0 = never). Travel legs departing
     * before this and return arrivals before this no longer produce a wrap-up suggestion.
     * Optional in JSON (omitted when 0) so older peers/records stay compatible.
     */
    val reopenedAtEpochMs: Long = 0L
) {
    val isEnded: Boolean get() = state == TripLifecycleState.ENDED
}

data class GroupLedgerExtras(
    val customCategories: List<CloudCustomCategory> = emptyList(),
    val tripLifecycle: TripLifecycleRecord? = null
)

/** Pure merge + codec helpers (JVM-testable; org.json only). */
object GroupLedgerExtrasCodec {

    const val KEY_CUSTOM_CATEGORIES = "customCategories"
    const val KEY_TRIP_LIFECYCLE = "tripLifecycle"

    /** Hard cap so a hostile/buggy peer can't bloat every ledger push. */
    const val MAX_CUSTOM_CATEGORY_ENTRIES = 200
    private const val MAX_TITLE = 28
    private const val MAX_ICON_KEY = 32

    private val KNOWN_BUCKETS = setOf(
        "FOOD", "LOCAL_TRANSPORT", "STAY", "TRAVEL_TICKETS", "ACTIVITIES", "SHOPPING", "OTHER"
    )

    fun sanitizeTitle(raw: String): String =
        raw.replace(Regex("[\\p{Cntrl}|]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(MAX_TITLE)
            .trim()

    private fun sanitizeIconKey(raw: String): String =
        raw.trim().filter { it.isLetterOrDigit() || it == '_' }.take(MAX_ICON_KEY)

    private fun sanitizeBucket(raw: String): String =
        raw.trim().uppercase(java.util.Locale.US).takeIf { it in KNOWN_BUCKETS }.orEmpty()

    /** Deterministic winner between two versions of the same id. */
    private fun pickWinner(a: CloudCustomCategory, b: CloudCustomCategory): CloudCustomCategory = when {
        a.updatedAtEpochMs != b.updatedAtEpochMs -> if (a.updatedAtEpochMs > b.updatedAtEpochMs) a else b
        a.deleted != b.deleted -> if (a.deleted) a else b // equal clocks: delete wins
        else -> if ("${a.title}|${a.iconKey}|${a.parentBucket}" >= "${b.title}|${b.iconKey}|${b.parentBucket}") a else b
    }

    /** Union by id, LWW on [CloudCustomCategory.updatedAtEpochMs], tombstones win ties. Sorted by id. */
    fun mergeCustomCategories(vararg lists: List<CloudCustomCategory>): List<CloudCustomCategory> {
        val byId = LinkedHashMap<String, CloudCustomCategory>()
        for (list in lists) {
            for (c in list) {
                if (c.id.isBlank()) continue
                val existing = byId[c.id]
                byId[c.id] = if (existing == null) c else pickWinner(existing, c)
            }
        }
        return byId.values.sortedBy { it.id }.take(MAX_CUSTOM_CATEGORY_ENTRIES)
    }

    /** LWW on the lifecycle's own clock. Equal clocks: ENDED wins, then the later endedAt. */
    fun mergeTripLifecycle(vararg records: TripLifecycleRecord?): TripLifecycleRecord? {
        var best: TripLifecycleRecord? = null
        for (r in records) {
            if (r == null) continue
            val cur = best
            best = when {
                cur == null -> r
                r.updatedAtEpochMs != cur.updatedAtEpochMs -> if (r.updatedAtEpochMs > cur.updatedAtEpochMs) r else cur
                r.isEnded != cur.isEnded -> if (r.isEnded) r else cur
                r.endedAtEpochMs > cur.endedAtEpochMs -> r
                else -> cur
            }
        }
        return best
    }

    fun mergeExtras(vararg extras: GroupLedgerExtras?): GroupLedgerExtras {
        val present = extras.filterNotNull()
        return GroupLedgerExtras(
            customCategories = mergeCustomCategories(*present.map { it.customCategories }.toTypedArray()),
            tripLifecycle = mergeTripLifecycle(*present.map { it.tripLifecycle }.toTypedArray())
        )
    }

    // ------------------------------------------------------------------------------------------
    // JSON codec
    // ------------------------------------------------------------------------------------------

    fun encodeCustomCategories(list: List<CloudCustomCategory>): JSONArray {
        val arr = JSONArray()
        list.sortedBy { it.id }.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("title", c.title)
                put("iconKey", c.iconKey)
                if (c.parentBucket.isNotBlank()) put("parent", c.parentBucket)
                if (c.createdByPhone.isNotBlank()) put("createdByPhone", c.createdByPhone)
                put("updatedAtEpochMs", c.updatedAtEpochMs)
                if (c.deleted) put("deleted", true)
            })
        }
        return arr
    }

    fun decodeCustomCategories(arr: JSONArray?): List<CloudCustomCategory> {
        if (arr == null) return emptyList()
        val out = ArrayList<CloudCustomCategory>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id", "").trim().take(64)
            if (id.isBlank()) continue
            val title = sanitizeTitle(o.optString("title", ""))
            val deleted = o.optBoolean("deleted", false)
            if (title.length < 2 && !deleted) continue
            out += CloudCustomCategory(
                id = id,
                title = title,
                iconKey = sanitizeIconKey(o.optString("iconKey", "")),
                parentBucket = sanitizeBucket(o.optString("parent", "")),
                createdByPhone = PhoneIdentityValidator.normalizeIndianPhone10(o.optString("createdByPhone", "")),
                updatedAtEpochMs = o.optLong("updatedAtEpochMs", 0L).coerceAtLeast(0L),
                deleted = deleted
            )
        }
        return mergeCustomCategories(out)
    }

    fun encodeTripLifecycle(rec: TripLifecycleRecord): JSONObject = JSONObject().apply {
        put("state", rec.state)
        if (rec.endedAtEpochMs > 0L) put("endedAtEpochMs", rec.endedAtEpochMs)
        if (rec.endedByPhone.isNotBlank()) put("endedByPhone", rec.endedByPhone)
        rec.startDateEpochMs?.let { put("startDateEpochMs", it) }
        rec.endDateEpochMs?.let { put("endDateEpochMs", it) }
        put("updatedAtEpochMs", rec.updatedAtEpochMs)
        if (rec.reopenedAtEpochMs > 0L) put("reopenedAtEpochMs", rec.reopenedAtEpochMs)
    }

    fun decodeTripLifecycle(obj: JSONObject?): TripLifecycleRecord? {
        if (obj == null) return null
        val rawState = obj.optString("state", TripLifecycleState.ACTIVE).trim().uppercase(java.util.Locale.US)
        val state = if (rawState == TripLifecycleState.ENDED) TripLifecycleState.ENDED else TripLifecycleState.ACTIVE
        fun optNullableLong(key: String): Long? =
            if (obj.has(key) && !obj.isNull(key)) obj.optLong(key, 0L).takeIf { it > 0L } else null
        return TripLifecycleRecord(
            state = state,
            endedAtEpochMs = obj.optLong("endedAtEpochMs", 0L).coerceAtLeast(0L),
            endedByPhone = PhoneIdentityValidator.normalizeIndianPhone10(obj.optString("endedByPhone", "")),
            startDateEpochMs = optNullableLong("startDateEpochMs"),
            endDateEpochMs = optNullableLong("endDateEpochMs"),
            updatedAtEpochMs = obj.optLong("updatedAtEpochMs", 0L).coerceAtLeast(0L),
            reopenedAtEpochMs = obj.optLong("reopenedAtEpochMs", 0L).coerceAtLeast(0L)
        )
    }

    /** Canonical, order-independent fragment for the structural ledger hash. */
    fun canonicalCustomCategories(list: List<CloudCustomCategory>): JSONArray =
        JSONArray(list.sortedBy { it.id }.map { c ->
            "${c.id}:${c.title}:${c.iconKey}:${c.parentBucket}:${if (c.deleted) 1 else 0}:${c.updatedAtEpochMs}"
        })

    fun canonicalTripLifecycle(rec: TripLifecycleRecord): String =
        "${rec.state}:${rec.endedAtEpochMs}:${rec.endedByPhone}:${rec.startDateEpochMs ?: -1L}:" +
            "${rec.endDateEpochMs ?: -1L}:${rec.updatedAtEpochMs}" +
            // Appended only when set, so pre-existing records keep their exact structural hash.
            (if (rec.reopenedAtEpochMs > 0L) ":r${rec.reopenedAtEpochMs}" else "")

    fun encodeExtras(extras: GroupLedgerExtras): String = JSONObject().apply {
        if (extras.customCategories.isNotEmpty()) put(KEY_CUSTOM_CATEGORIES, encodeCustomCategories(extras.customCategories))
        extras.tripLifecycle?.let { put(KEY_TRIP_LIFECYCLE, encodeTripLifecycle(it)) }
    }.toString()

    fun decodeExtras(raw: String?): GroupLedgerExtras {
        if (raw.isNullOrBlank()) return GroupLedgerExtras()
        return try {
            val root = JSONObject(raw)
            GroupLedgerExtras(
                customCategories = decodeCustomCategories(root.optJSONArray(KEY_CUSTOM_CATEGORIES)),
                tripLifecycle = decodeTripLifecycle(root.optJSONObject(KEY_TRIP_LIFECYCLE))
            )
        } catch (_: Exception) {
            GroupLedgerExtras()
        }
    }
}

/**
 * Durable per-group local copy of the v2.3.5 ledger extensions (same pattern as `LocalTombstoneStore`):
 * in-memory map backed by SharedPreferences. Exposed as a [StateFlow] so every screen sees the
 * group's shared categories / lifecycle immediately after a sync.
 */
object GroupLedgerExtrasStore {
    private const val PREFS_NAME = "splitmate_v235_ledger_extras"

    @Volatile
    private var appContext: Context? = null

    private val mem = ConcurrentHashMap<String, GroupLedgerExtras>()
    private val _extrasByGroup = MutableStateFlow<Map<String, GroupLedgerExtras>>(emptyMap())
    val extrasByGroup: StateFlow<Map<String, GroupLedgerExtras>> = _extrasByGroup.asStateFlow()

    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()

    fun init(context: Context?) {
        if (context != null) appContext = context.applicationContext ?: context
    }

    fun addListener(listener: (groupId: String) -> Unit) {
        if (listener !in listeners) listeners += listener
    }

    fun removeListener(listener: (groupId: String) -> Unit) {
        listeners -= listener
    }

    fun resetForTests() {
        mem.clear()
        _extrasByGroup.value = emptyMap()
    }

    private fun ctxOrNull(context: Context?): Context? {
        if (context != null) init(context)
        return appContext
    }

    fun load(context: Context?, groupId: String): GroupLedgerExtras {
        if (groupId.isBlank()) return GroupLedgerExtras()
        mem[groupId]?.let { return it }
        val ctx = ctxOrNull(context) ?: return GroupLedgerExtras()
        val loaded = try {
            GroupLedgerExtrasCodec.decodeExtras(
                ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("x_$groupId", null)
            )
        } catch (_: Exception) {
            GroupLedgerExtras()
        }
        mem[groupId] = loaded
        publish(groupId, loaded, notify = false)
        return loaded
    }

    fun customCategories(groupId: String): List<CloudCustomCategory> = load(null, groupId).customCategories

    fun tripLifecycle(groupId: String): TripLifecycleRecord? = load(null, groupId).tripLifecycle

    /** Merges [incoming] into the durable copy (union/LWW); persists + notifies only on change. */
    fun mergeAndSave(context: Context?, groupId: String, incoming: GroupLedgerExtras): GroupLedgerExtras {
        if (groupId.isBlank()) return GroupLedgerExtras()
        val current = load(context, groupId)
        val merged = GroupLedgerExtrasCodec.mergeExtras(current, incoming)
        if (merged != current) {
            mem[groupId] = merged
            val ctx = ctxOrNull(context)
            if (ctx != null) {
                try {
                    ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putString("x_$groupId", GroupLedgerExtrasCodec.encodeExtras(merged))
                        .apply()
                } catch (_: Exception) {
                }
            }
            publish(groupId, merged, notify = true)
        }
        return merged
    }

    fun upsertCustomCategory(context: Context?, groupId: String, category: CloudCustomCategory): GroupLedgerExtras =
        mergeAndSave(context, groupId, GroupLedgerExtras(customCategories = listOf(category)))

    fun setTripLifecycle(context: Context?, groupId: String, record: TripLifecycleRecord): GroupLedgerExtras =
        mergeAndSave(context, groupId, GroupLedgerExtras(tripLifecycle = record))

    private fun publish(groupId: String, extras: GroupLedgerExtras, notify: Boolean) {
        _extrasByGroup.update { it + (groupId to extras) }
        if (notify) listeners.forEach { runCatching { it(groupId) } }
    }
}
