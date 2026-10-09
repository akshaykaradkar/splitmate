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

    const val KEEP_PER_EXPENSE = 20

    data class Prepared(
        val expenses: List<ExpenseEntity>,
        val history: ExpenseVersioning.History
    )

    fun historyOf(revisions: List<ExpenseRevisionEntity>, extraVersions: Sequence<Long> = emptySequence()): ExpenseVersioning.History {
        val known = revisions.groupBy { it.expenseId }.mapValues { (_, list) -> list.map { it.contentHash }.toSet() }
        val maxSeen = (revisions.asSequence().map { it.rowVersion } + extraVersions).maxOrNull() ?: 0L
        return ExpenseVersioning.History(knownHashesByExpense = known, maxSeenVersion = maxSeen)
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
        revisionId = "${expense.expenseId}_${expense.rowVersion}_${hash.take(12)}",
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
        nowMs: Long
    ): Pair<Map<String, ExpenseEntity>, List<ExpenseRevisionEntity>> {
        if (revisions.isEmpty()) {
            // First sync after the upgrade: remember today's content as the starting version.
            return emptyMap<String, ExpenseEntity>() to expenses.map { e ->
                revisionOf(e, ExpenseVersioning.contentHash(e, splits), "seed", nowMs)
            }
        }
        var maxSeen = (revisions.asSequence().map { it.rowVersion } +
            expenses.asSequence().map { it.rowVersion } + remoteVersions).maxOrNull() ?: 0L
        val byExpense = revisions.groupBy { it.expenseId }
        val stamped = linkedMapOf<String, ExpenseEntity>()
        val newRevisions = mutableListOf<ExpenseRevisionEntity>()
        for (e in expenses) {
            val hash = ExpenseVersioning.contentHash(e, splits)
            val known = byExpense[e.expenseId]
            when {
                known != null && known.any { it.rowVersion == e.rowVersion && it.contentHash == hash } -> Unit
                known == null && e.rowVersion > 0L ->
                    // Arrived already versioned (joined from the cloud): remember it, don't re-stamp.
                    newRevisions += revisionOf(e, hash, "remote", nowMs)
                else -> {
                    val version = ExpenseVersioning.nextVersion(nowMs, maxSeen)
                    maxSeen = version
                    val next = e.copy(rowVersion = version, rowUpdatedBy = editorId.ifBlank { null })
                    stamped[e.expenseId] = next
                    newRevisions += revisionOf(next, hash, "local", nowMs)
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
        nowMs: Long
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
            nowMs = nowMs
        )
        val applied = mutableMapOf<String, ExpenseEntity>()
        dao.runInLedgerTransaction {
            // Only stamp rows nobody changed since we read them (an edit saved meanwhile is stamped next sync).
            val current = dao.getExpensesForGroup(groupId).associateBy { it.expenseId }
            val original = expenses.associateBy { it.expenseId }
            for ((id, next) in plannedStamps) {
                if (current[id] == original[id]) {
                    dao.insertExpense(next)
                    applied[id] = next
                }
            }
            val keptRevisions = plannedRevisions.filter { it.kind != "local" || it.expenseId in applied }
            if (keptRevisions.isNotEmpty()) dao.insertExpenseRevisions(keptRevisions)
        }
        val allRevisions = revisions + plannedRevisions.filter { it.kind != "local" || it.expenseId in applied }
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
        for (merged in mergedDoc.expenses) {
            if (merged.expenseId in skippedExpenseIds) continue
            val hash = ExpenseVersioning.contentHash(merged, mergedDoc.splits)
            val local = localById[merged.expenseId]
            val remote = remoteById[merged.expenseId]
            val localHash = local?.let { ExpenseVersioning.contentHash(it, localSplits) }
            val remoteHash = remote?.let { ExpenseVersioning.contentHash(it, remoteSplits) }
            val changedHere = localHash != null && localHash != hash
            val kind = when {
                merged.rowUpdatedBy == ExpenseVersioning.LEGACY_EDITOR && local?.rowVersion != merged.rowVersion -> "legacy"
                // The content went back to a copy this phone had already seen: show a heads-up.
                changedHere && history?.knows(merged.expenseId, hash) == true -> "revert"
                local != null && remote != null && local.rowVersion > 0L && remote.rowVersion > 0L &&
                    localHash != remoteHash -> "concurrent"
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
        runCatching {
            val existing = dao.getExpenseRevisionsForGroup(mergedDoc.group.groupId).map { it.revisionId }.toSet()
            val revisions = planMergeRevisions(localDoc, remoteDoc, mergedDoc, history, existing, skippedExpenseIds, nowMs)
            if (revisions.isEmpty()) return
            dao.insertExpenseRevisions(revisions)
            revisions.map { it.expenseId }.distinct().forEach { dao.pruneExpenseRevisions(it, KEEP_PER_EXPENSE) }
        }
    }
}
