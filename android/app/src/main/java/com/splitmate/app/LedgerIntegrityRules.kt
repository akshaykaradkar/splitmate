package com.splitmate.app

import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.SettlementEntity

/** v2.3.6: parses a typed amount ("3193.96", "₹3,193.96", "500") into integer paise, exactly. */
object ManualPaymentInput {
    /** Returns paise, or null when the text isn't a positive amount with at most 2 decimals. */
    fun parseAmountToCents(text: String): Long? {
        val clean = text.trim().removePrefix("₹").replace(",", "").replace(" ", "")
        if (clean.isEmpty() || !Regex("""\d+(\.\d{0,2})?|\.\d{1,2}""").matches(clean)) return null
        val cents = runCatching { java.math.BigDecimal(clean).movePointRight(2).longValueExact() }.getOrNull()
        return cents?.takeIf { it > 0L }
    }
}

/**
 * v2.3.6: stops the same payment from being recorded twice (double tap, or a stale "Mark paid" row
 * that is still on screen after the payment was recorded).
 */
object SettlementDuplicateGuard {
    /** An identical payment (same payer, receiver and amount) inside this window is a duplicate. */
    const val DuplicateWindowMillis: Long = 2 * 60 * 1000L

    fun isDuplicateOrAlreadySettled(
        fromMemberId: String,
        toMemberId: String,
        amountCents: Long,
        existingSettlements: List<SettlementEntity>,
        netBalances: Map<String, Long>,
        nowMillis: Long
    ): Boolean {
        val recentTwin = existingSettlements.any {
            it.fromMemberId == fromMemberId &&
                it.toMemberId == toMemberId &&
                it.amountCents == amountCents &&
                nowMillis - it.settledAt in 0..DuplicateWindowMillis
        }
        if (recentTwin) return true
        // Net balance: positive = is owed money, negative = owes money. The payer must still owe and
        // the receiver must still be owed, otherwise this transfer was already settled.
        val fromNet = netBalances[fromMemberId]
        val toNet = netBalances[toMemberId]
        if (fromNet != null && toNet != null && (fromNet >= 0L || toNet <= 0L)) return true
        return false
    }

    /**
     * Twins recorded further apart than this are treated as two real payments and never removed.
     * Found in real trip data: the same ₹1,439.13 recorded twice, about 8 minutes apart.
     */
    const val CleanupWindowMillis: Long = 15 * 60 * 1000L

    /**
     * Finds payments that were already recorded twice before [isDuplicateOrAlreadySettled] existed.
     * A later payment is removed only when ALL of these hold:
     * 1. An earlier payment has the same payer, receiver and amount.
     * 2. It was recorded within [CleanupWindowMillis] of that earlier twin.
     * 3. Without it, the payer no longer owes anything (net >= 0), so it only adds an overpayment.
     * Anything else (different amount, far apart, payer still in debt) is left alone.
     */
    fun redundantDuplicateIds(
        expenses: List<ExpenseEntity>,
        splits: List<ExpenseSplitEntity>,
        settlements: List<SettlementEntity>
    ): Set<String> {
        if (settlements.size < 2) return emptySet()
        val removed = mutableSetOf<String>()
        settlements
            .groupBy { Triple(it.fromMemberId, it.toMemberId, it.amountCents) }
            .values
            .filter { it.size > 1 }
            .forEach { twins ->
                val ordered = twins.sortedWith(compareBy({ it.settledAt }, { it.settlementId }))
                for (i in 1 until ordered.size) {
                    val later = ordered[i]
                    val earlier = ordered.subList(0, i).lastOrNull { it.settlementId !in removed } ?: continue
                    if (later.settledAt - earlier.settledAt !in 0..CleanupWindowMillis) continue
                    val payerNet = netBalanceOf(
                        memberId = later.fromMemberId,
                        expenses = expenses,
                        splits = splits,
                        settlements = settlements.filter { it.settlementId != later.settlementId && it.settlementId !in removed }
                    )
                    if (payerNet >= 0L) removed += later.settlementId
                }
            }
        return removed
    }

    /** Positive = is owed money, negative = owes money. */
    private fun netBalanceOf(
        memberId: String,
        expenses: List<ExpenseEntity>,
        splits: List<ExpenseSplitEntity>,
        settlements: List<SettlementEntity>
    ): Long {
        val expenseIds = expenses.map { it.expenseId }.toSet()
        var net = 0L
        expenses.forEach { if (it.payerId == memberId) net += it.totalAmountCents }
        splits.forEach { if (it.memberId == memberId && it.expenseId in expenseIds) net -= it.finalOwedCents }
        settlements.forEach {
            if (it.fromMemberId == memberId) net += it.amountCents
            if (it.toMemberId == memberId) net -= it.amountCents
        }
        return net
    }
}

/**
 * v2.3.6: keeps every expense's total equal to the sum of its split rows.
 *
 * Found in real trip data: a rental edited from ₹7,950 to ₹4,950 (deposit returned). The edit saved
 * the new total and the new split rows (6 × ₹825) together. Another phone with the old copy then
 * won the whole-document merge, and the v2.3.4 merge took the expense row from that copy (₹7,950)
 * while keeping the local split rows (same `_sp_N` ids), so the mixed copy spread to everyone. The
 * ₹3,000 nobody owed sent part of a real payment to the wrong person. See the RCA (Bike Rentals).
 *
 * Valid expenses always satisfy `sum(finalOwedCents) == totalAmountCents` (the payer's row carries
 * any unassigned remainder). In that failure the split rows were the newer data, so the repair
 * trusts them and corrects the stale total. Itemized receipts (tax / tip / remainder / multiplier)
 * are left alone because their total can't be rebuilt from the rows alone.
 */
object ExpenseSplitIntegrity {

    fun isConsistent(expense: ExpenseEntity, splits: List<ExpenseSplitEntity>): Boolean =
        splits.isEmpty() || splits.sumOf { it.finalOwedCents } == expense.totalAmountCents

    /** Returns [expense] with its total (and base subtotal) set to the split sum when it is stale. */
    fun repairStaleTotal(expense: ExpenseEntity, splits: List<ExpenseSplitEntity>): ExpenseEntity {
        val rows = splits.filter { it.expenseId == expense.expenseId }
        if (isConsistent(expense, rows)) return expense
        val isPlainEqualOrExact = expense.taxCents == 0L &&
            expense.tipCents == 0L &&
            expense.unassignedBaseCents == 0L &&
            kotlin.math.abs(expense.lockedMultiplier - 1.0) < 1e-9
        if (!isPlainEqualOrExact) return expense
        val splitSum = rows.sumOf { it.finalOwedCents }
        if (splitSum <= 0L) return expense
        return expense.copy(totalAmountCents = splitSum, baseSubtotalCents = splitSum)
    }

    /** Repairs every expense in a ledger. */
    fun repairStaleTotals(expenses: List<ExpenseEntity>, splits: List<ExpenseSplitEntity>): List<ExpenseEntity> {
        val byExpense = splits.groupBy { it.expenseId }
        return expenses.map { exp -> repairStaleTotal(exp, byExpense[exp.expenseId].orEmpty()) }
    }
}

/**
 * v2.3.6 P0 (RCA audit, risk R2): the sync reads the local rows, merges, then writes. An edit saved
 * in between used to be half-overwritten: the merged expense row (old total) was written, but the
 * split rows were skipped because they still matched the pre-merge snapshot, leaving the edit's new
 * shares under the old total. The sync now re-reads the rows inside one transaction and skips every
 * expense / settlement that changed since its snapshot. The edit stays local (it is PENDING and
 * triggers its own sync), and nothing is ever written half-way.
 */
object ConcurrentEditGuard {

    /** Expense ids whose row or share rows differ between the sync's snapshot and the database now. */
    fun changedExpenseIds(
        snapshotExpenses: List<ExpenseEntity>,
        snapshotSplits: List<ExpenseSplitEntity>,
        currentExpenses: List<ExpenseEntity>,
        currentSplits: List<ExpenseSplitEntity>
    ): Set<String> {
        val before = snapshotExpenses.associateBy { it.expenseId }
        val now = currentExpenses.associateBy { it.expenseId }
        val splitsBefore = snapshotSplits.groupBy { it.expenseId }.mapValues { it.value.toSet() }
        val splitsNow = currentSplits.groupBy { it.expenseId }.mapValues { it.value.toSet() }
        return (before.keys + now.keys).filterTo(mutableSetOf()) { id ->
            before[id] != now[id] || splitsBefore[id].orEmpty() != splitsNow[id].orEmpty()
        }
    }

    /** Settlement ids added, removed or changed since the sync's snapshot. */
    fun changedSettlementIds(snapshot: List<SettlementEntity>, current: List<SettlementEntity>): Set<String> {
        val before = snapshot.associateBy { it.settlementId }
        val now = current.associateBy { it.settlementId }
        return (before.keys + now.keys).filterTo(mutableSetOf()) { before[it] != now[it] }
    }
}
