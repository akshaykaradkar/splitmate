package com.splitmate.app

import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.PhoneIdentityValidator
import kotlin.math.abs

/**
 * v2.3.5 (issue #4): who may edit or delete an expense (pure Kotlin, unit-testable).
 *
 * Allowed: any trip ORGANIZER, the PAYER, or the CREATOR (`ExpenseEntity.createdByPhone`).
 * Legacy rows with a null/blank `createdByPhone` fall back to organizer + payer only.
 * Everyone else sees the expense read-only. Enforced in the ViewModel AND in the UI.
 */
object ExpenseEditPermission {

    const val READ_ONLY_REASON =
        "Only the payer, the person who added it, or a trip organizer can edit or delete this expense."

    /**
     * @param currentPhone the device user's phone (any format; normalised to 10 digits).
     * @param isOrganizer whether the device user is an organizer of the expense's group.
     * @param payerPhone the payer member's phone (any format), if known.
     * @param createdByPhone `ExpenseEntity.createdByPhone`; null/blank on legacy rows.
     * @param isCurrentUserPayer phone-less fallback: the payer IS the device user's member row.
     */
    fun canModify(
        currentPhone: String?,
        isOrganizer: Boolean,
        payerPhone: String?,
        createdByPhone: String?,
        isCurrentUserPayer: Boolean = false
    ): Boolean {
        if (isOrganizer || isCurrentUserPayer) return true
        val me = normalize(currentPhone)
        if (me.length != 10) return false
        if (normalize(payerPhone) == me) return true
        val creator = normalize(createdByPhone)
        return creator.length == 10 && creator == me
    }

    /** Value to stamp into `ExpenseEntity.createdByPhone`; null when the user has no valid phone. */
    fun creatorStampOf(rawPhone: String?): String? = normalize(rawPhone).takeIf { it.length == 10 }

    private fun normalize(raw: String?): String =
        if (raw.isNullOrBlank()) "" else PhoneIdentityValidator.normalizeIndianPhone10(raw)
}

/**
 * v2.3.5 (issue #4, guard a): detects itemized / non-equal expenses whose amount or participants
 * must not be re-split equally by the quick edit dialog (that would silently flatten tax, tip,
 * remainder and item claims). For these, only title and payer edits are allowed and the existing
 * split rows are kept untouched.
 */
object ItemizedExpenseGuard {

    const val LOCKED_REASON =
        "Itemized receipt: amount and shares are locked so tax, tip and item claims stay intact. " +
            "You can still change the title or payer."

    fun isItemized(expense: ExpenseEntity, splits: List<ExpenseSplitEntity>): Boolean {
        if (expense.taxCents != 0L || expense.tipCents != 0L) return true
        if (expense.unassignedBaseCents != 0L) return true
        if (expense.baseSubtotalCents != expense.totalAmountCents) return true
        if (abs(expense.lockedMultiplier - 1.0) > 1e-9) return true
        val owed = splits
            .filter { it.expenseId == expense.expenseId && it.finalOwedCents > 0L }
            .map { it.finalOwedCents }
        // Equal splits from the Largest Remainder engine differ by at most 1 paisa.
        return owed.size >= 2 && (owed.maxOrNull()!! - owed.minOrNull()!!) > 1L
    }

    /**
     * True when the requested edit would change money or shares on an itemized expense.
     * Title / payer-only edits return false.
     */
    fun isBlockedEdit(
        expense: ExpenseEntity,
        splits: List<ExpenseSplitEntity>,
        newTotalCents: Long,
        newParticipantIds: Collection<String>?
    ): Boolean {
        if (!isItemized(expense, splits)) return false
        if (newTotalCents != expense.totalAmountCents) return true
        if (newParticipantIds == null) return false
        val current = splits
            .filter { it.expenseId == expense.expenseId && it.finalOwedCents > 0L }
            .map { it.memberId }
            .toSet()
        return newParticipantIds.toSet() != current
    }
}

/**
 * v2.3.5 (issue #4, guard c): split-row merge rule for cloud sync.
 *
 * The old merge unioned split rows from both documents by `(expenseId, memberId)`. When an edit
 * removed a participant on one side, the other side's stale row for that participant was
 * resurrected, so the splits no longer added up to the expense total.
 *
 * Rule: for an expense present in BOTH documents, keep only the split rows of the side whose
 * expense version wins (the same last-writer-wins choice used for the expense row). If the
 * winning side has no split rows for that expense, fall back to the other side's rows.
 * Expenses present on one side only keep that side's rows unchanged.
 */
object ExpenseSplitMergeRules {

    fun pruneLosingSideSplits(
        localSplits: List<ExpenseSplitEntity>,
        remoteSplits: List<ExpenseSplitEntity>,
        localExpenseIds: Set<String>,
        remoteExpenseIds: Set<String>,
        localWins: Boolean
    ): Pair<List<ExpenseSplitEntity>, List<ExpenseSplitEntity>> {
        val onBothSides = localExpenseIds intersect remoteExpenseIds
        if (onBothSides.isEmpty()) return localSplits to remoteSplits
        val localHasRows = localSplits.map { it.expenseId }.toSet()
        val remoteHasRows = remoteSplits.map { it.expenseId }.toSet()

        val keptLocal = localSplits.filter { sp ->
            val id = sp.expenseId
            id !in onBothSides || localWins || id !in remoteHasRows
        }
        val keptRemote = remoteSplits.filter { sp ->
            val id = sp.expenseId
            id !in onBothSides || !localWins || id !in localHasRows
        }
        return keptLocal to keptRemote
    }

    /**
     * v2.4.0 P1: for an expense present on both sides, keep ONLY the split rows of the side whose
     * expense row won ([localWinsFor]), even when that side has no rows (an expense with no shares
     * then fails the Money check instead of borrowing another copy's shares). Expenses present on
     * one side keep that side's rows.
     */
    fun keepWinningSideSplits(
        localSplits: List<ExpenseSplitEntity>,
        remoteSplits: List<ExpenseSplitEntity>,
        localExpenseIds: Set<String>,
        remoteExpenseIds: Set<String>,
        localWinsFor: (String) -> Boolean
    ): Pair<List<ExpenseSplitEntity>, List<ExpenseSplitEntity>> {
        val onBothSides = localExpenseIds intersect remoteExpenseIds
        val keptLocal = localSplits.filter { sp ->
            if (sp.expenseId in onBothSides) localWinsFor(sp.expenseId) else sp.expenseId in localExpenseIds
        }
        val keptRemote = remoteSplits.filter { sp ->
            if (sp.expenseId in onBothSides) !localWinsFor(sp.expenseId) else sp.expenseId in remoteExpenseIds
        }
        return keptLocal to keptRemote
    }
}
