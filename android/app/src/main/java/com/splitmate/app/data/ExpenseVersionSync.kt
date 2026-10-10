package com.splitmate.app.data

import com.splitmate.app.ExpenseVersioning

/**
 * v2.4.0 P1 + P4: connects [ExpenseVersioning] to the cloud sync.
 *
 * - [prepareLocalExpenses] runs before a merge: it gives every expense edited on this phone since its
 *   last known version a new hybrid-clock version (whatever screen made the edit), and returns the
 *   history of versions this phone knows. On the first sync after the upgrade it only records the
 *   current content as version 0 ("seed"), so no data changes.
 * - [recordMergeOutcome] runs after the merged ledger is written: it stores each new version as an
 *   [ExpenseRevisionEntity] (the P4 edit history) and remembers losing copies as known, so an older
 *   app re-uploading them later loses.
 */
object ExpenseVersionSync {

    /** Review F3/N1: this phone's own history is effectively never pruned (fingerprints must survive). */
    const val KEEP_PER_EXPENSE = 200
    /** Entries received from other phones' synced history are stored with this kind prefix. */
    const val RECEIVED_PREFIX = "rx-"
    private const val FINGERPRINT_SUFFIX = "_fp"

    fun baseKind(kind: String): String = kind.removePrefix(RECEIVED_PREFIX)
    private val OWN_HELD_KINDS = setOf("seed", "local", "remote", "legacy", "revert", "concurrent")
    fun isReceived(rev: ExpenseRevisionEntity): Boolean = rev.kind.startsWith(RECEIVED_PREFIX)
    /** P4: synced history is compact: the newest 5 entries per expense, at most 300 per trip. */
    const val SYNC_PER_EXPENSE = 5
    const val SYNC_PER_TRIP = 300
    const val KEY_REVISIONS = "rev"

    private val revisionOrder = compareByDescending<ExpenseRevisionEntity> { it.observedAtEpochMs }
        .thenByDescending { it.rowVersion }
        .thenBy { it.revisionId }

    /** Kinds that mark an older copy (needed to recognise stale uploads), never shown as changes. */
    val KNOWN_COPY_KINDS = setOf("seed", "superseded")
    const val SYNC_KNOWN_COPIES_PER_EXPENSE = 8

    /**
     * The compact, deterministic history list that travels with the trip document: per expense the
     * newest [SYNC_PER_EXPENSE] changes plus up to [SYNC_KNOWN_COPIES_PER_EXPENSE] older-copy
     * fingerprints. Review F3: when the trip cap bites, display entries are dropped before the
     * older-copy fingerprints, which are what keeps a stale old-app upload from winning.
     */
    fun compactForSync(revisions: List<ExpenseRevisionEntity>, deletedExpenseIds: Set<String> = emptySet()): List<ExpenseRevisionEntity> {
        val perExpense = revisions.asSequence()
            .filter { it.expenseId !in deletedExpenseIds }
            .groupBy { it.revisionId }
            // Same revision observed by two phones: keep the earliest observation (deterministic).
            .map { (_, same) -> same.minWith(compareBy<ExpenseRevisionEntity> { it.observedAtEpochMs }.thenBy { it.kind }) }
            .groupBy { it.expenseId }
            .flatMap { (_, list) ->
                val (copies, changes) = list.partition { baseKind(it.kind) in KNOWN_COPY_KINDS }
                changes.sortedWith(revisionOrder).take(SYNC_PER_EXPENSE) +
                    copies.sortedWith(revisionOrder).take(SYNC_KNOWN_COPIES_PER_EXPENSE)
            }
        val (copies, changes) = perExpense.partition { baseKind(it.kind) in KNOWN_COPY_KINDS }
        val keptCopies = copies.sortedWith(revisionOrder).take(SYNC_PER_TRIP)
        val keptChanges = changes.sortedWith(revisionOrder).take((SYNC_PER_TRIP - keptCopies.size).coerceAtLeast(0))
        return (keptChanges + keptCopies).sortedWith(revisionOrder)
    }

    /** Union of two documents' history lists (P4), compacted. */
    fun mergeSyncedRevisions(
        local: List<ExpenseRevisionEntity>,
        remote: List<ExpenseRevisionEntity>,
        deletedExpenseIds: Set<String>
    ): List<ExpenseRevisionEntity> = compactForSync(local + remote, deletedExpenseIds)

    /** [base] plus every content hash in [revisions] (a recorded version anywhere is a known copy). */
    fun withKnown(base: ExpenseVersioning.History?, revisions: List<ExpenseRevisionEntity>): ExpenseVersioning.History? {
        if (base == null || revisions.isEmpty()) return base
        val known = base.knownHashesByExpense.mapValues { it.value.toMutableSet() }.toMutableMap()
        revisions.forEach { known.getOrPut(it.expenseId) { mutableSetOf() } += it.contentHash }
        // Received entries add known copies but never make this phone's own record "complete" (N1).
        return base.copy(
            knownHashesByExpense = known.mapValues { it.value.toSet() },
            maxSeenVersion = maxOf(base.maxSeenVersion, revisions.maxOf { it.rowVersion })
        )
    }

    /** Member ids, amounts and hashes only: never phone numbers (ntfy topics are public). */
    fun encodeRevisions(revisions: List<ExpenseRevisionEntity>): org.json.JSONArray {
        val arr = org.json.JSONArray()
        revisions.forEach { r ->
            arr.put(org.json.JSONObject().apply {
                put("e", r.expenseId)
                put("v", r.rowVersion)
                put("h", r.contentHash)
                put("t", r.totalAmountCents)
                put("p", r.payerId)
                r.editedBy?.let { put("b", it) }
                put("a", r.observedAtEpochMs)
                put("k", baseKind(r.kind))
            })
        }
        return arr
    }

    fun decodeRevisions(arr: org.json.JSONArray?, groupId: String): List<ExpenseRevisionEntity> {
        arr ?: return emptyList()
        val out = mutableListOf<ExpenseRevisionEntity>()
        for (i in 0 until minOf(arr.length(), SYNC_PER_TRIP * 2)) {
            val o = arr.optJSONObject(i) ?: continue
            val expenseId = o.optString("e", "").trim().take(120)
            val hash = o.optString("h", "").trim().take(64)
            val kind = o.optString("k", "").trim().take(16)
            if (expenseId.isEmpty() || hash.isEmpty() || kind.isEmpty()) continue
            val version = o.optLong("v", 0L).coerceAtLeast(0L)
            out += ExpenseRevisionEntity(
                revisionId = "${expenseId}_${version}_${hash.take(12)}" + if (baseKind(kind) == "superseded") FINGERPRINT_SUFFIX else "",
                expenseId = expenseId,
                groupId = groupId,
                rowVersion = version,
                contentHash = hash,
                title = "",
                totalAmountCents = o.optLong("t", 0L),
                payerId = o.optString("p", "").take(120),
                editedBy = o.optString("b", "").trim().take(80).takeIf { it.isNotEmpty() },
                observedAtEpochMs = o.optLong("a", 0L),
                kind = kind
            )
        }
        return out
    }

    data class Prepared(
        val expenses: List<ExpenseEntity>,
        val history: ExpenseVersioning.History
    )

    fun historyOf(revisions: List<ExpenseRevisionEntity>, extraVersions: Sequence<Long> = emptySequence()): ExpenseVersioning.History {
        val known = revisions.groupBy { it.expenseId }.mapValues { (_, list) -> list.map { it.contentHash }.toSet() }
        val maxSeen = (revisions.asSequence().map { it.rowVersion } + extraVersions).maxOrNull() ?: 0L
        // Review R2: only copies this phone actually HELD count (a fingerprint of a rejected copy doesn't).
        val firstOwn = revisions.filter { !isReceived(it) && it.kind in OWN_HELD_KINDS }.groupBy { it.expenseId }
            .mapValues { (_, list) -> list.minOf { it.rowVersion } }
        return ExpenseVersioning.History(knownHashesByExpense = known, maxSeenVersion = maxSeen, firstOwnVersionByExpense = firstOwn)
    }

    suspend fun loadHistory(dao: SplitMateDao, groupId: String): ExpenseVersioning.History =
        historyOf(runCatching { dao.getExpenseRevisionsForGroup(groupId) }.getOrDefault(emptyList()))

    private fun revisionOf(
        expense: ExpenseEntity,
        hash: String,
        kind: String,
        nowMs: Long,
        editedBy: String? = expense.rowUpdatedBy
    ) = ExpenseRevisionEntity(
        // Fingerprints of replaced copies get their own id, so they never collide with (or hide) the
        // displayed change entry of the same version (review N1).
        revisionId = "${expense.expenseId}_${expense.rowVersion}_${hash.take(12)}" + if (kind == "superseded") FINGERPRINT_SUFFIX else "",
        expenseId = expense.expenseId,
        groupId = expense.groupId,
        rowVersion = expense.rowVersion,
        contentHash = hash,
        title = expense.title,
        totalAmountCents = expense.totalAmountCents,
        payerId = expense.payerId,
        editedBy = editedBy,
        observedAtEpochMs = nowMs,
        kind = kind
    )

    /** Pure planning step (unit-tested): which local rows are edits that need a new version. */
    fun planLocalStamps(
        expenses: List<ExpenseEntity>,
        splits: List<ExpenseSplitEntity>,
        revisions: List<ExpenseRevisionEntity>,
        remoteVersions: Sequence<Long>,
        editorId: String,
        nowMs: Long,
        /** When each edit was actually made on this phone (null = unknown). */
        editTimeOf: (String) -> Long? = { null },
        /** Review F4: device-independent member key, see [ExpenseVersioning.memberKeyOf]. */
        memberKey: (String) -> String = { it },
        /**
         * When the phone last changed anything in this trip before this sync (kept by every app
         * version). Used for PENDING rows that have no edit time of their own.
         */
        lastLocalChangeMs: Long? = null
    ): Pair<Map<String, ExpenseEntity>, List<ExpenseRevisionEntity>> {
        val maxSeen = (revisions.asSequence().map { it.rowVersion } +
            expenses.asSequence().map { it.rowVersion } + remoteVersions).maxOrNull() ?: 0L
        val byExpense = revisions.groupBy { it.expenseId }
        val firstRun = revisions.isEmpty()
        val stamped = linkedMapOf<String, ExpenseEntity>()
        val newRevisions = mutableListOf<ExpenseRevisionEntity>()
        // Review F1/F5: an edit made on this phone always leaves a trace: the row is PENDING until it is
        // pushed, and the edit clock remembers when it happened.
        fun editedHere(e: ExpenseEntity) =
            e.syncStatus.equals("PENDING", ignoreCase = true) || editTimeOf(e.expenseId) != null
        for (e in expenses) {
            val hash = ExpenseVersioning.contentHash(e, splits, memberKey)
            val known = byExpense[e.expenseId]
            when {
                known != null && known.any { it.rowVersion == e.rowVersion && it.contentHash == hash } -> Unit
                // First sync after the upgrade: untouched rows become the starting version (no data change).
                firstRun && !editedHere(e) -> newRevisions += revisionOf(e, hash, "seed", nowMs)
                // Arrived already versioned (joined from the cloud): remember it, don't re-stamp.
                known == null && e.rowVersion > 0L && !editedHere(e) -> newRevisions += revisionOf(e, hash, "remote", nowMs)
                // Differs from what we recorded but was not edited here (e.g. the app stopped after a
                // merge was written but before its history was): record it, never give it a new version.
                known != null && !editedHere(e) -> newRevisions += revisionOf(e, hash, "remote", nowMs)
                else -> {
                    // Review N3 (+F1): a PENDING row with no recorded edit time (an older app's offline edit,
                    // or its push that was delivered but never confirmed) must not get the strongest
                    // version "now". The trip's last local change time (which older apps also record)
                    // dates it: a newer edit made elsewhere still wins, a later offline edit here wins.
                    // Known limit (review R4): older apps keep that time per trip, not per expense, so on
                    // the first sync after upgrading, an unsynced offline edit is dated by the trip's last
                    // unsynced change. Choosing an earlier time instead would make genuine offline edits
                    // lose, which is the more common case; it only applies once, before any 2.4 edit.
                    val editedAt = editTimeOf(e.expenseId)?.takeIf { it in 1..nowMs }
                        ?: maxOf(e.createdAt, lastLocalChangeMs ?: 0L).coerceIn(1L, nowMs)
                    // Above every version seen in earlier syncs, but not pushed by other expenses stamped
                    // in this same pass: each edit keeps its own edit time.
                    val version = ExpenseVersioning.nextVersion(editedAt, maxSeen)
                    val next = e.copy(rowVersion = version, rowUpdatedBy = editorId.ifBlank { null })
                    stamped[e.expenseId] = next
                    newRevisions += revisionOf(next, hash, "local", nowMs)
                    // Review N1: the copy this edit replaced becomes a synced fingerprint, so any phone
                    // recognises an older app's later re-upload of it as stale.
                    // Review R3: the source may be an entry this phone only holds as received.
                    known?.firstOrNull { it.rowVersion == e.rowVersion && baseKind(it.kind) != "superseded" }?.let { previous ->
                        newRevisions += previous.copy(
                            revisionId = previous.revisionId.removeSuffix(FINGERPRINT_SUFFIX) + FINGERPRINT_SUFFIX,
                            kind = "superseded",
                            observedAtEpochMs = nowMs
                        )
                    }
                }
            }
        }
        return stamped to newRevisions
    }

    suspend fun prepareLocalExpenses(
        dao: SplitMateDao,
        groupId: String,
        expenses: List<ExpenseEntity>,
        splits: List<ExpenseSplitEntity>,
        remoteDoc: CloudGroupLedgerDocument?,
        editorId: String,
        nowMs: Long,
        context: android.content.Context? = null,
        members: List<GroupMemberEntity> = emptyList(),
        lastLocalChangeMs: Long? = null
    ): Prepared {
        val revisions = runCatching { dao.getExpenseRevisionsForGroup(groupId) }.getOrDefault(emptyList())
        val (plannedStamps, plannedRevisions) = planLocalStamps(
            expenses = expenses,
            splits = splits,
            revisions = revisions,
            // Versions first seen in THIS sync are concurrent with the local edit, so they are left out:
            // concurrent edits resolve by wall clock ("the newest one wins"). Versions seen in earlier
            // syncs (in the revisions) still push the clock forward, which beats a skewed phone clock.
            remoteVersions = emptySequence(),
            editorId = editorId,
            nowMs = nowMs,
            editTimeOf = { id -> ExpenseEditClock.editTimeOf(context, id) },
            memberKey = ExpenseVersioning.memberKeyOf(members),
            lastLocalChangeMs = lastLocalChangeMs
        )
        val applied = mutableMapOf<String, ExpenseEntity>()
        dao.runInLedgerTransaction {
            // Only stamp rows nobody changed since we read them (an edit saved meanwhile is stamped next sync).
            val current = dao.getExpensesForGroup(groupId).associateBy { it.expenseId }
            val original = expenses.associateBy { it.expenseId }
            // Review F6: shares too, so a share-only edit saved meanwhile keeps its own edit time.
            val currentSplits = dao.getSplitsForGroup(groupId).groupBy { it.expenseId }.mapValues { it.value.toSet() }
            val originalSplits = splits.groupBy { it.expenseId }.mapValues { it.value.toSet() }
            for ((id, next) in plannedStamps) {
                if (current[id] == original[id] && currentSplits[id].orEmpty() == originalSplits[id].orEmpty()) {
                    dao.insertExpense(next)
                    applied[id] = next
                }
            }
            val keptRevisions = plannedRevisions.filter { (it.kind != "local" && it.kind != "superseded") || it.expenseId in applied }
            if (keptRevisions.isNotEmpty()) dao.insertExpenseRevisions(keptRevisions)
        }
        // Versioned edits no longer need their edit time, and rows that turned out unchanged (a save
        // with identical content) must not keep a stale one; edits not stamped yet keep theirs.
        ExpenseEditClock.clear(
            context,
            applied.keys + expenses.map { it.expenseId }.filter { id ->
                id !in plannedStamps && (ExpenseEditClock.editTimeOf(context, id) ?: 0L) < nowMs
            }
        )
        val allRevisions = revisions + plannedRevisions.filter { (it.kind != "local" && it.kind != "superseded") || it.expenseId in applied }
        val finalExpenses = expenses.map { applied[it.expenseId] ?: it }
        return Prepared(
            expenses = finalExpenses,
            history = historyOf(
                allRevisions,
                finalExpenses.asSequence().map { it.rowVersion } + remoteDoc?.expenses.orEmpty().asSequence().map { it.rowVersion }
            )
        )
    }

    /** Pure planning step (unit-tested): revisions to store after a merge. */
    fun planMergeRevisions(
        localDoc: CloudGroupLedgerDocument?,
        remoteDoc: CloudGroupLedgerDocument?,
        mergedDoc: CloudGroupLedgerDocument,
        history: ExpenseVersioning.History?,
        existingRevisionIds: Set<String>,
        skippedExpenseIds: Set<String>,
        nowMs: Long
    ): List<ExpenseRevisionEntity> {
        val out = linkedMapOf<String, ExpenseRevisionEntity>()
        val localById = localDoc?.expenses.orEmpty().associateBy { it.expenseId }
        val remoteById = remoteDoc?.expenses.orEmpty().associateBy { it.expenseId }
        val localSplits = localDoc?.splits.orEmpty()
        val remoteSplits = remoteDoc?.splits.orEmpty()
        fun add(rev: ExpenseRevisionEntity) {
            if (rev.revisionId !in existingRevisionIds && rev.revisionId !in out) out[rev.revisionId] = rev
        }
        // Review Q1: one resolver over every document's members (any known phone wins).
        val sharedKey = ExpenseVersioning.memberKeyOf(
            localDoc?.members.orEmpty() + remoteDoc?.members.orEmpty() + mergedDoc.members
        )
        val mergedKey = sharedKey
        val localKey = sharedKey
        val remoteKey = sharedKey
        for (merged in mergedDoc.expenses) {
            if (merged.expenseId in skippedExpenseIds) continue
            val hash = ExpenseVersioning.contentHash(merged, mergedDoc.splits, mergedKey)
            val local = localById[merged.expenseId]
            val remote = remoteById[merged.expenseId]
            val localHash = local?.let { ExpenseVersioning.contentHash(it, localSplits, localKey) }
            val remoteHash = remote?.let { ExpenseVersioning.contentHash(it, remoteSplits, remoteKey) }
            val changedHere = localHash != null && localHash != hash
            val kind = when {
                merged.rowUpdatedBy == ExpenseVersioning.LEGACY_EDITOR && local?.rowVersion != merged.rowVersion -> "legacy"
                // The content went back to a copy this phone had already seen: show a heads-up.
                changedHere && history?.knows(merged.expenseId, hash) == true -> "revert"
                // Review F7: only a real conflict: this phone had an unsynced edit of its own and the
                // other copy is a different versioned edit. A plain newer edit from elsewhere is "remote".
                local != null && remote != null && local.rowVersion > 0L && remote.rowVersion > 0L &&
                    localHash != remoteHash && local.syncStatus.equals("PENDING", ignoreCase = true) -> "concurrent"
                else -> "remote"
            }
            add(revisionOf(merged, hash, kind, nowMs))
            // Remember the losing copy so an older app re-uploading it later is recognised as stale.
            if (local != null && localHash != hash) add(revisionOf(local, localHash!!, "superseded", nowMs))
            if (remote != null && remoteHash != hash) add(revisionOf(remote, remoteHash!!, "superseded", nowMs))
        }
        return out.values.toList()
    }

    suspend fun recordMergeOutcome(
        dao: SplitMateDao,
        localDoc: CloudGroupLedgerDocument?,
        remoteDoc: CloudGroupLedgerDocument?,
        mergedDoc: CloudGroupLedgerDocument,
        history: ExpenseVersioning.History?,
        skippedExpenseIds: Set<String>,
        nowMs: Long
    ) {
        // Review F5: called INSIDE the ledger transaction and errors are not swallowed, so the merged rows
        // and their history are written together (or not at all).
        run {
            val existing = dao.getExpenseRevisionsForGroup(mergedDoc.group.groupId).map { it.revisionId }.toSet()
            // P4: history entries other phones recorded (synced list), plus what this merge saw.
            val received = mergedDoc.revisions
                .filter { it.revisionId !in existing }
                .map { if (isReceived(it)) it else it.copy(kind = RECEIVED_PREFIX + it.kind) }
            // Review R3: this phone's own observations are planned against what it already stored only,
            // and written AFTER the received entries, so an own entry replaces "rx-" for the same id.
            val observed = planMergeRevisions(localDoc, remoteDoc, mergedDoc, history, existing, skippedExpenseIds, nowMs)
            val revisions = (received + observed).distinctBy { it.revisionId }
            if (revisions.isEmpty()) return
            val observedIds = observed.map { it.revisionId }.toSet()
            dao.insertExpenseRevisions(received.filter { it.revisionId !in observedIds })
            dao.insertExpenseRevisions(observed)
            revisions.map { it.expenseId }.distinct().forEach { dao.pruneExpenseRevisions(it, KEEP_PER_EXPENSE, KEEP_PER_EXPENSE) }
        }
    }
}
