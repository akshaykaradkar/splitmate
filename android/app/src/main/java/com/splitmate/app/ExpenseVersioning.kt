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
    fun contentHash(
        expense: ExpenseEntity,
        splits: List<ExpenseSplitEntity>,
        /** Review F4: device-independent member key (phone number when known); see [memberKeyOf]. */
        memberKey: (String) -> String = { it }
    ): String {
        val rows = splits
            .filter { it.expenseId == expense.expenseId }
            .map { memberKey(it.memberId) to it }
            .sortedWith(compareBy<Pair<String, ExpenseSplitEntity>> { it.first }.thenBy { it.second.finalOwedCents })
            .joinToString(";") { (key, sp) -> "$key=${sp.finalOwedCents}/${sp.baseClaimedCents}" }
        val canonical = listOf(
            expense.expenseId,
            expense.title.trim(),
            memberKey(expense.payerId),
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

    /**
     * Review F4: the same person can have a different member id on each phone (placeholder rows,
     * `_me` ids, older apps). Keying members by their 10-digit phone (member id only when there is
     * none) makes a copy hash the same on every phone. [fallback] maps an id without a phone (e.g.
     * through the merge's member remap) before it is used as the key.
     */
    fun memberKeyOf(
        members: List<com.splitmate.app.data.GroupMemberEntity>,
        fallback: (String) -> String = { it }
    ): (String) -> String {
        // Review Q1: the same member id can appear in several documents, with a phone in one and not in
        // another (e.g. an owner row patched locally but phoneless in the cloud). Any known phone wins,
        // so every resolver built from any mix of documents gives the same key.
        val phoneById = members.groupBy { it.memberId }.mapValues { (_, rows) ->
            rows.map { com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10(it.userPhone, it.upiId) }
                .firstOrNull { it.length == 10 }
        }
        return { id ->
            val mapped = fallback(id)
            (phoneById[id]?.takeIf { it.length == 10 } ?: phoneById[mapped]?.takeIf { it.length == 10 })
                ?.let { "p:$it" } ?: "m:$mapped"
        }
    }

    /** Hybrid logical clock: never below wall time, always above every version already seen. */
    fun nextVersion(nowMs: Long, maxSeenVersion: Long): Long = maxOf(nowMs, maxSeenVersion + 1)

    /** What this phone knows: hashes of every version it has seen, per expense. */
    data class History(
        val knownHashesByExpense: Map<String, Set<String>> = emptyMap(),
        val maxSeenVersion: Long = 0L,
        /**
         * Lowest version of each expense this phone observed ITSELF (entries received from other
         * phones' synced history don't count). Review N1: a phone that first saw an expense at version
         * T (joined or reinstalled later) can't tell a stale copy from a real edit below T.
         */
        val firstOwnVersionByExpense: Map<String, Long> = emptyMap()
    ) {
        fun knows(expenseId: String, hash: String): Boolean =
            knownHashesByExpense[expenseId]?.contains(hash) == true

        /** True when this phone itself saw this expense before [version] (so its record covers it). */
        fun isCompleteUpTo(expenseId: String, version: Long): Boolean =
            firstOwnVersionByExpense[expenseId]?.let { it < version } == true
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
        history: History?,
        localKey: (String) -> String = { it },
        remoteKey: (String) -> String = { it }
    ): Decision {
        val lv = local.rowVersion
        val rv = remote.rowVersion
        if (lv <= 0L && rv <= 0L) {
            return Decision(if (localDocWins) Side.LOCAL else Side.REMOTE)
        }
        val localHash = contentHash(local, localSplits, localKey)
        val remoteHash = contentHash(remote, remoteSplits, remoteKey)
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
        // Review F3/N1: only adopt an unknown unversioned copy when this phone itself saw the expense
        // before the versioned side (its own, never-pruned record covers it). A phone that joined or
        // reinstalled later can't tell a stale copy from a real edit, so it keeps the versioned side;
        // a phone with the full record adopts a genuine old-app edit and its legacy version spreads.
        if (!history.isCompleteUpTo(unversioned.expenseId, versionedVersion)) return Decision(versionedSide)
        val unversionedSide = if (versionedSide == Side.LOCAL) Side.REMOTE else Side.LOCAL
        return Decision(unversionedSide, stampVersion = versionedVersion + 1)
    }
}
