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
 * v2.3.6: keeps every expense's split rows summing exactly to the expense total.
 *
 * Found in real trip data: an expense whose total said ₹7,950 while its six split rows still added up
 * to the old ₹4,950. The ₹3,000 nobody owed left two members "owed" money with no transfer that could
 * ever pay them, so the trip could never reach "all settled". Valid expenses always satisfy
 * `sum(finalOwedCents) == totalAmountCents` (the payer's row carries any unassigned remainder), so a
 * mismatch is always corruption. The repair keeps everyone's proportions and fixes the pennies with
 * Largest Remainder (0.00 drift).
 */
object ExpenseSplitIntegrity {

    fun isConsistent(expense: ExpenseEntity, splits: List<ExpenseSplitEntity>): Boolean =
        splits.isEmpty() || splits.sumOf { it.finalOwedCents } == expense.totalAmountCents

    /** Returns [splits] rescaled to the expense total, or [splits] unchanged when already consistent. */
    fun reconcile(expense: ExpenseEntity, splits: List<ExpenseSplitEntity>): List<ExpenseSplitEntity> {
        if (isConsistent(expense, splits)) return splits
        val ordered = splits.sortedBy { it.splitId }
        val finals = largestRemainder(expense.totalAmountCents, ordered.map { it.finalOwedCents })
        val bases = largestRemainder(expense.baseSubtotalCents, ordered.map { it.baseClaimedCents.coerceAtLeast(0L) })
        val repaired = ordered.mapIndexed { i, sp ->
            sp.copy(
                finalOwedCents = finals.first[i],
                baseClaimedCents = bases.first[i],
                plusOneCent = finals.second[i]
            )
        }
        val byId = repaired.associateBy { it.splitId }
        return splits.map { byId[it.splitId] ?: it }
    }

    /** Repairs every expense in a ledger; splits of unknown expenses are passed through. */
    fun reconcileAll(expenses: List<ExpenseEntity>, splits: List<ExpenseSplitEntity>): List<ExpenseSplitEntity> {
        val byExpense = splits.groupBy { it.expenseId }
        val expenseById = expenses.associateBy { it.expenseId }
        return byExpense.flatMap { (expId, rows) ->
            val exp = expenseById[expId]
            if (exp == null) rows else reconcile(exp, rows)
        }
    }

    /**
     * Distributes [total] proportionally to [weights] (equal shares when all weights are 0). Returns the
     * allocations and, per row, whether it received one of the leftover pennies.
     */
    private fun largestRemainder(total: Long, weights: List<Long>): Pair<List<Long>, List<Boolean>> {
        val n = weights.size
        if (n == 0) return emptyList<Long>() to emptyList()
        val w = if (weights.all { it <= 0L }) List(n) { 1L } else weights.map { it.coerceAtLeast(0L) }
        val weightSum = w.sum()
        val floors = LongArray(n)
        val remainders = DoubleArray(n)
        for (i in 0 until n) {
            val exact = total.toDouble() * w[i].toDouble() / weightSum.toDouble()
            floors[i] = kotlin.math.floor(exact).toLong()
            remainders[i] = exact - floors[i]
        }
        var leftover = total - floors.sum()
        val bonus = BooleanArray(n)
        val order = (0 until n).sortedWith(compareByDescending<Int> { remainders[it] }.thenBy { it })
        var k = 0
        while (leftover > 0 && n > 0) {
            val i = order[k % n]
            floors[i] += 1
            bonus[i] = true
            leftover--
            k++
        }
        return floors.toList() to bonus.toList()
    }
}
