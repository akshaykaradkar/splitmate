package com.splitmate.app

import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.SettlementEntity

/**
 * v2.4.0 D5: pure, display-only maths for the "money trust" moments. Nothing here writes money;
 * every figure is derived from the same expenses, splits and settlements the balances use, so
 * the numbers on screen always agree with the net balance (paid - share + sent - received).
 */
object MoneyTrustRules {

    /** One traveler's position in a trip. All values in integer cents. */
    data class TravelerStanding(
        val memberId: String,
        /** Total of the expenses this traveler paid for. */
        val paidCents: Long,
        /** This traveler's share of all expenses (sum of their split rows). */
        val shareCents: Long,
        /** Payments this traveler has sent to others (settlements from them). */
        val sentCents: Long,
        /** Payments this traveler has received (settlements to them). */
        val receivedCents: Long
    ) {
        /** Same formula as the trip balances: positive = gets back, negative = owes. */
        val netCents: Long get() = paidCents - shareCents + sentCents - receivedCents

        /**
         * How much of their part this traveler has covered, 0..1, for the paid-vs-share bar.
         * Covered = paid + sent, their part = share + received. 1 when they owe nothing.
         */
        val coveredFraction: Float
            get() {
                val covered = paidCents + sentCents
                val part = shareCents + receivedCents
                if (part <= 0L) return if (covered > 0L || netCents >= 0L) 1f else 0f
                return (covered.toDouble() / part.toDouble()).coerceIn(0.0, 1.0).toFloat()
            }
    }

    fun standings(
        memberIds: List<String>,
        expenses: List<ExpenseEntity>,
        splits: List<ExpenseSplitEntity>,
        settlements: List<SettlementEntity>
    ): Map<String, TravelerStanding> {
        val expenseIds = expenses.map { it.expenseId }.toSet()
        return memberIds.associateWith { id ->
            TravelerStanding(
                memberId = id,
                paidCents = expenses.filter { it.payerId == id }.sumOf { it.totalAmountCents },
                shareCents = splits.filter { it.memberId == id && it.expenseId in expenseIds }.sumOf { it.finalOwedCents },
                sentCents = settlements.filter { it.fromMemberId == id }.sumOf { it.amountCents },
                receivedCents = settlements.filter { it.toMemberId == id }.sumOf { it.amountCents }
            )
        }
    }

    /**
     * How many separate debts the trip would have without simplification: for every pair of
     * travelers, what one owes the other across all expenses (minus payments between them),
     * netted per pair. Counts pairs that still owe something.
     */
    fun pairwiseDebtCount(
        expenses: List<ExpenseEntity>,
        splits: List<ExpenseSplitEntity>,
        settlements: List<SettlementEntity>
    ): Int {
        val payerByExpense = expenses.associate { it.expenseId to it.payerId }
        // owes[a to b] > 0 means a owes b. Keyed on an ordered pair (smaller id first).
        val net = HashMap<Pair<String, String>, Long>()
        fun add(debtor: String, creditor: String, cents: Long) {
            if (debtor == creditor || cents == 0L) return
            if (debtor < creditor) {
                val k = debtor to creditor
                net[k] = (net[k] ?: 0L) + cents
            } else {
                val k = creditor to debtor
                net[k] = (net[k] ?: 0L) - cents
            }
        }
        splits.forEach { sp ->
            val payer = payerByExpense[sp.expenseId] ?: return@forEach
            add(debtor = sp.memberId, creditor = payer, cents = sp.finalOwedCents)
        }
        // A payment from -> to reduces what `from` owes `to`.
        settlements.forEach { st -> add(debtor = st.toMemberId, creditor = st.fromMemberId, cents = st.amountCents) }
        return net.values.count { it != 0L }
    }

    /**
     * Subtitle under "Smart settle up". Says how many debts were simplified only when that is
     * true (the pairwise count is larger than the plan); otherwise the plain count.
     */
    fun simplificationLine(pairwiseDebts: Int, payments: Int): String = when {
        payments <= 0 -> "Everyone is settled"
        pairwiseDebts > payments ->
            "We simplified $pairwiseDebts debts into $payments ${if (payments == 1) "payment" else "payments"}"
        payments == 1 -> "1 payment to settle everyone"
        else -> "$payments payments to settle everyone"
    }

    /** True when an expense carries tax or tip that is shared in proportion to each base share. */
    fun includesTaxOrTip(expense: ExpenseEntity): Boolean = expense.taxCents + expense.tipCents > 0L
}
