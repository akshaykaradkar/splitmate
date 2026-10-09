package com.splitmate.app

import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseSplitEntity
import java.security.MessageDigest

/**
 * v2.4.0 P1 (RCA Bike Rentals): every expense keeps its own version.
 *
 * Before: a whole trip document won or lost as one unit, stamped with "now" by any phone that had a
 * pending change. An old, untouched copy of an expense on that phone could therefore undo a real
 * edit made elsewhere (₹4,950 back to ₹7,950).
 *
 * Now, per expense:
 *  - [ExpenseEntity.rowVersion] is a hybrid logical clock: `max(now, newest version seen + 1)`, so a
 *    phone whose clock runs days ahead can't win forever; the next edit anywhere goes past it.
 *  - Higher version wins. Ties go to the editor id, then to the content hash, so every phone picks
 *    the same winner.
 *  - Apps older than v2.4.0 drop the version when they re-upload (it reads as 0). Such a row is a
 *    stale copy if its content matches a version this phone already knows, and loses. If its content
 *    is new, it is a real edit made on an old app, and wins (and is given the next version).
 *  - Without version information on either side, the old document-level rule is kept unchanged.
 *  - Shares always follow the winning expense, all or nothing ([ExpenseSplitMergeRules]).
 *  - Repairs never create a version, so a repair can't beat a real edit.
 */
object ExpenseVersioning {

    /** Editor id used when an old app's genuine edit is adopted. */
    const val LEGACY_EDITOR = "legacy"

    /**
     * Content fingerprint of an expense and its shares. Only fields every app version uploads are
     * included (not `categoryRef` / `createdByPhone`, which apps older than v2.3.5 drop), and never
     * sync bookkeeping (status, version, editor).
     */
    fun contentHash(expense: ExpenseEntity, splits: List<ExpenseSplitEntity>): String {
        val rows = splits
            .filter { it.expenseId == expense.expenseId }
            .sortedWith(compareBy<ExpenseSplitEntity> { it.memberId }.thenBy { it.finalOwedCents })
            .joinToString(";") { "${it.memberId}=${it.finalOwedCents}/${it.baseClaimedCents}" }
        val canonical = listOf(
            expense.expenseId,
            expense.title.trim(),
            expense.payerId,
            expense.totalAmountCents,
            expense.baseSubtotalCents,
            expense.taxCents,
            expense.tipCents,
            expense.unassignedBaseCents,
            String.format(java.util.Locale.ROOT, "%.6f", expense.lockedMultiplier),
            expense.currencyCode,
            expense.expenseCategory,
            expense.scheduledAtEpochMs ?: -1L,
            rows
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        return digest.take(16).joinToString("") { "%02x".format(it) }
    }

    /** Hybrid logical clock: never below wall time, always above every version already seen. */
    fun nextVersion(nowMs: Long, maxSeenVersion: Long): Long = maxOf(nowMs, maxSeenVersion + 1)

    /** What this phone knows: hashes of every version it has seen, per expense. */
    data class History(
        val knownHashesByExpense: Map<String, Set<String>> = emptyMap(),
        val maxSeenVersion: Long = 0L,
        /** Highest version of each expense this phone has a record of (its history is complete up to it). */
        val maxKnownVersionByExpense: Map<String, Long> = emptyMap()
    ) {
        fun knows(expenseId: String, hash: String): Boolean =
            knownHashesByExpense[expenseId]?.contains(hash) == true

        fun isCompleteUpTo(expenseId: String, version: Long): Boolean =
            (maxKnownVersionByExpense[expenseId] ?: -1L) >= version
    }

    enum class Side { LOCAL, REMOTE }

    /**
     * @property stampVersion set when an old app's genuine edit wins: the winner is given this
     * version (and [LEGACY_EDITOR]) so later merges keep it.
     * @property concurrent both sides carried different, versioned content (two edits from the same
     * starting point); P4 records it.
     */
    data class Decision(val side: Side, val stampVersion: Long? = null, val concurrent: Boolean = false)

    fun decide(
        local: ExpenseEntity,
        localSplits: List<ExpenseSplitEntity>,
        remote: ExpenseEntity,
        remoteSplits: List<ExpenseSplitEntity>,
        localDocWins: Boolean,
        history: History?
    ): Decision {
        val lv = local.rowVersion
        val rv = remote.rowVersion
        if (lv <= 0L && rv <= 0L) {
            return Decision(if (localDocWins) Side.LOCAL else Side.REMOTE)
        }
        val localHash = contentHash(local, localSplits)
        val remoteHash = contentHash(remote, remoteSplits)
        if (lv > 0L && rv > 0L) {
            val side = when {
                lv != rv -> if (lv > rv) Side.LOCAL else Side.REMOTE
                (local.rowUpdatedBy.orEmpty()) != (remote.rowUpdatedBy.orEmpty()) ->
                    if (local.rowUpdatedBy.orEmpty() > remote.rowUpdatedBy.orEmpty()) Side.LOCAL else Side.REMOTE
                else -> if (localHash >= remoteHash) Side.LOCAL else Side.REMOTE
            }
            return Decision(side, concurrent = localHash != remoteHash)
        }
        // Exactly one side is unversioned (an app older than v2.4.0, or a row not yet re-stamped).
        val versionedSide = if (lv > 0L) Side.LOCAL else Side.REMOTE
        val unversioned = if (lv > 0L) remote else local
        val unversionedHash = if (lv > 0L) remoteHash else localHash
        val versionedHash = if (lv > 0L) localHash else remoteHash
        val versionedVersion = maxOf(lv, rv)
        if (unversionedHash == versionedHash) return Decision(versionedSide)
        if (history == null) {
            // No history available (pure fold of cloud snapshots): keep the old rule.
            return Decision(if (localDocWins) Side.LOCAL else Side.REMOTE)
        }
        if (history.knows(unversioned.expenseId, unversionedHash)) return Decision(versionedSide)
        // Review F3: only adopt an unknown unversioned copy when this phone's history of the expense is
        // complete up to the versioned side. A phone that joined late (or pruned old entries) can't
        // tell a stale copy from a real edit, so it keeps the versioned side; a phone with the full
        // history adopts a genuine old-app edit and its legacy version then reaches everyone.
        if (!history.isCompleteUpTo(unversioned.expenseId, versionedVersion)) return Decision(versionedSide)
        val unversionedSide = if (versionedSide == Side.LOCAL) Side.REMOTE else Side.LOCAL
        return Decision(unversionedSide, stampVersion = versionedVersion + 1)
    }
}
