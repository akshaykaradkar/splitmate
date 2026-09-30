package com.splitmate.app.data.guide.sync

import com.splitmate.app.data.guide.PlanManifest
import com.splitmate.app.data.guide.StayPin

/**
 * Pure, deterministic state-based merge (join-semilattice) for [PlanManifest].
 *
 * Every rule is a lexicographic max over a TOTAL order, so [merge] is commutative, associative and
 * idempotent (on normalised inputs; merge output is always normalised, see [normalize]):
 *
 * | Field             | Rule                                                                              |
 * |-------------------|-----------------------------------------------------------------------------------|
 * | destQid, wvRev    | LWW header by (updatedAt, author is organizer, author key, destQid, wvRev).       |
 * |                   | `updatedAt`, `by`, `version` travel with the winning header.                     |
 * | pinned, hidden    | OR-set with timestamps: per-id max. Visible iff pinnedAt > hiddenAt.              |
 * | days[id]          | LWW per key, timestamped by the id's pin time (re-pin to reassign a day): the     |
 * |                   | entry of the input with the greatest (pinnedAt, day) wins; absent day < any day.  |
 * | stay              | LWW register by (setAt, setter is organizer, setter key, lat, lng, label, prec).  |
 * |                   | Cleared via the reserved tombstone `hidden["stay"]`; a stay is kept only if       |
 * |                   | setAt > hidden["stay"]. Only the setter or an organizer may create that tombstone |
 * |                   | (enforced by [clearStay]; payloads are unauthenticated until v2.4 encryption).    |
 * | route             | Never synced; every device recomputes it deterministically.                      |
 *
 * [organizerKeys] must be the same set on every device for a given merge (e.g. the ledger's
 * `organizerRolesByKey.keys`), otherwise tie-breaks could differ between devices.
 */
object PlanManifestMerger {

    private const val STAY_ID = PlanManifestCodec.STAY_ID

    /** Joins two manifests. Commutative, associative and idempotent. */
    fun merge(a: PlanManifest, b: PlanManifest, organizerKeys: Set<String> = emptySet()): PlanManifest {
        val headerWinner = if (compareHeaders(a, b, organizerKeys) >= 0) a else b

        val pinned = maxMerge(a.pinned, b.pinned)
        val hidden = maxMerge(a.hidden, b.hidden)
        val days = mergeDays(a, b)

        val stayCandidate = when {
            a.stay == null -> b.stay
            b.stay == null -> a.stay
            compareStays(a.stay, b.stay, organizerKeys) >= 0 -> a.stay
            else -> b.stay
        }
        val stayTombstone = hidden[STAY_ID]
        val stay = stayCandidate?.takeIf { stayTombstone == null || it.setAtEpochMs > stayTombstone }

        return PlanManifest(
            version = headerWinner.version,
            destinationQid = headerWinner.destinationQid,
            wikivoyageRevisionId = headerWinner.wikivoyageRevisionId,
            stay = stay,
            pinned = pinned,
            hidden = hidden,
            days = days,
            updatedAtEpochMs = headerWinner.updatedAtEpochMs,
            updatedByMemberKey = headerWinner.updatedByMemberKey
        )
    }

    /** Folds any number of manifests (order-independent). Null for an empty list. */
    fun mergeAll(manifests: Iterable<PlanManifest>, organizerKeys: Set<String> = emptySet()): PlanManifest? =
        manifests.fold(null as PlanManifest?) { acc, m -> if (acc == null) normalize(m) else merge(acc, m, organizerKeys) }

    /** Canonical form: drops a stay that is superseded by the stay tombstone; sorts maps. */
    fun normalize(m: PlanManifest): PlanManifest = merge(m, m)

    // ---------------------------------------------------------------------------------------------
    // Derived views
    // ---------------------------------------------------------------------------------------------

    /** Ids currently pinned: pinnedAt > hiddenAt (the reserved `stay` id is excluded). */
    fun visiblePinnedIds(m: PlanManifest): Set<String> =
        m.pinned.filter { (id, pinnedAt) -> id != STAY_ID && pinnedAt > (m.hidden[id] ?: Long.MIN_VALUE) }
            .keys.toSortedSet()

    /** True when [placeId] is hidden (hiddenAt >= pinnedAt, or hidden without any pin). */
    fun isHidden(m: PlanManifest, placeId: String): Boolean {
        val hiddenAt = m.hidden[placeId] ?: return false
        val pinnedAt = m.pinned[placeId] ?: return true
        return hiddenAt >= pinnedAt
    }

    // ---------------------------------------------------------------------------------------------
    // Local mutations (produce a new manifest; caller persists + publishes)
    // ---------------------------------------------------------------------------------------------

    fun pin(m: PlanManifest, placeId: String, byMemberKey: String?, now: Long, day: Int? = null): PlanManifest {
        require(PlanManifestCodec.isValidPlaceId(placeId) && placeId != STAY_ID) { "invalid place id" }
        val at = maxOf(now, (m.hidden[placeId] ?: -1L) + 1, (m.pinned[placeId] ?: -1L) + 1)
        val days = if (day != null && day in 1..PlanManifestCodec.MAX_DAY) m.days + (placeId to day) else m.days - placeId
        return m.copy(
            pinned = m.pinned + (placeId to at),
            days = days,
            updatedAtEpochMs = maxOf(m.updatedAtEpochMs + 1, now),
            updatedByMemberKey = byMemberKey
        )
    }

    fun hide(m: PlanManifest, placeId: String, byMemberKey: String?, now: Long): PlanManifest {
        require(PlanManifestCodec.isValidPlaceId(placeId) && placeId != STAY_ID) { "invalid place id" }
        val at = maxOf(now, m.pinned[placeId] ?: -1L, (m.hidden[placeId] ?: -1L) + 1)
        return m.copy(
            hidden = m.hidden + (placeId to at),
            updatedAtEpochMs = maxOf(m.updatedAtEpochMs + 1, now),
            updatedByMemberKey = byMemberKey
        )
    }

    fun setDestination(m: PlanManifest, destinationQid: String?, wikivoyageRevisionId: Long?, byMemberKey: String?, now: Long): PlanManifest {
        require(destinationQid == null || PlanManifestCodec.isValidQid(destinationQid)) { "invalid QID" }
        return m.copy(
            destinationQid = destinationQid,
            wikivoyageRevisionId = wikivoyageRevisionId,
            updatedAtEpochMs = maxOf(m.updatedAtEpochMs + 1, now),
            updatedByMemberKey = byMemberKey
        )
    }

    /** Shares [stay] into the manifest (only call when the planner opted in to sharing). */
    fun setStay(m: PlanManifest, stay: StayPin, byMemberKey: String?, now: Long): PlanManifest {
        val tomb = m.hidden[STAY_ID] ?: -1L
        val at = maxOf(now, stay.setAtEpochMs, tomb + 1)
        return m.copy(
            stay = stay.copy(setAtEpochMs = at, setByMemberKey = byMemberKey ?: stay.setByMemberKey),
            updatedAtEpochMs = maxOf(m.updatedAtEpochMs + 1, now),
            updatedByMemberKey = byMemberKey
        )
    }

    /** True when [byMemberKey] may clear the current shared stay (its setter or an organizer). */
    fun canClearStay(m: PlanManifest, byMemberKey: String?, organizerKeys: Set<String>): Boolean {
        val stay = m.stay ?: return true
        if (byMemberKey == null) return false
        return byMemberKey == stay.setByMemberKey || byMemberKey in organizerKeys
    }

    /**
     * Clears the shared stay by writing the `hidden["stay"]` tombstone. Returns null when
     * [byMemberKey] is neither the setter nor an organizer (the manifest must not change).
     */
    fun clearStay(m: PlanManifest, byMemberKey: String?, organizerKeys: Set<String>, now: Long): PlanManifest? {
        if (!canClearStay(m, byMemberKey, organizerKeys)) return null
        val at = maxOf(now, m.stay?.setAtEpochMs ?: -1L, m.hidden[STAY_ID] ?: -1L)
        return m.copy(
            stay = null,
            hidden = m.hidden + (STAY_ID to at),
            updatedAtEpochMs = maxOf(m.updatedAtEpochMs + 1, now),
            updatedByMemberKey = byMemberKey
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Total orders
    // ---------------------------------------------------------------------------------------------

    internal fun compareHeaders(a: PlanManifest, b: PlanManifest, organizerKeys: Set<String>): Int {
        compareValues(a.updatedAtEpochMs, b.updatedAtEpochMs).let { if (it != 0) return it }
        compareValues(isOrganizer(a.updatedByMemberKey, organizerKeys), isOrganizer(b.updatedByMemberKey, organizerKeys))
            .let { if (it != 0) return it }
        compareValues(a.updatedByMemberKey, b.updatedByMemberKey).let { if (it != 0) return it }
        compareValues(a.destinationQid, b.destinationQid).let { if (it != 0) return it }
        compareValues(a.wikivoyageRevisionId, b.wikivoyageRevisionId).let { if (it != 0) return it }
        return compareValues(a.version, b.version)
    }

    internal fun compareStays(a: StayPin, b: StayPin, organizerKeys: Set<String>): Int {
        compareValues(a.setAtEpochMs, b.setAtEpochMs).let { if (it != 0) return it }
        compareValues(isOrganizer(a.setByMemberKey, organizerKeys), isOrganizer(b.setByMemberKey, organizerKeys))
            .let { if (it != 0) return it }
        compareValues(a.setByMemberKey, b.setByMemberKey).let { if (it != 0) return it }
        a.location.lat.compareTo(b.location.lat).let { if (it != 0) return it }
        a.location.lng.compareTo(b.location.lng).let { if (it != 0) return it }
        a.label.compareTo(b.label).let { if (it != 0) return it }
        return a.precision.ordinal.compareTo(b.precision.ordinal)
    }

    private fun isOrganizer(key: String?, organizerKeys: Set<String>): Boolean =
        key != null && key in organizerKeys

    private fun maxMerge(a: Map<String, Long>, b: Map<String, Long>): Map<String, Long> {
        val out = sortedMapOf<String, Long>()
        out.putAll(a)
        for ((k, v) in b) {
            val cur = out[k]
            if (cur == null || v > cur) out[k] = v
        }
        return out
    }

    /**
     * Per key: state = (pinnedAt or MIN, day or absent). Lexicographic max; absent day < any day.
     * The winning state's day (if present) is kept.
     */
    private fun mergeDays(a: PlanManifest, b: PlanManifest): Map<String, Int> {
        val out = sortedMapOf<String, Int>()
        for (key in (a.days.keys + b.days.keys)) {
            val pa = a.pinned[key] ?: Long.MIN_VALUE
            val pb = b.pinned[key] ?: Long.MIN_VALUE
            val da = a.days[key]
            val db = b.days[key]
            val day = when {
                pa > pb -> da
                pb > pa -> db
                else -> when {
                    da == null -> db
                    db == null -> da
                    else -> maxOf(da, db)
                }
            }
            if (day != null) out[key] = day
        }
        return out
    }
}
