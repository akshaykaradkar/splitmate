package com.splitmate.app

import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.SettlementEntity

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
}

/**
 * v2.3.6: keeps every expense's total equal to the sum of its split rows.
 *
 * Found in real trip data: a rental edited from ₹7,950 to ₹4,950 (deposit returned). The edit saved
 * the new total and the new split rows (6 × ₹825), but a sync that was already on the network wrote
 * its older copy of the expense row back, so the total went back to ₹7,950 while the splits stayed
 * at ₹4,950. The ₹3,000 nobody owed left two members "owed" money that no transfer could ever pay.
 *
 * Valid expenses always satisfy `sum(finalOwedCents) == totalAmountCents` (the payer's row carries
 * any unassigned remainder). In this failure the split rows are the newer data, so the repair trusts
 * them and corrects the stale total. Itemized receipts (tax / tip / remainder / multiplier) are left
 * alone because their total can't be rebuilt from the rows alone.
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
