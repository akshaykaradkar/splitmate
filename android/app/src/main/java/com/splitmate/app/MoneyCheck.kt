package com.splitmate.app

import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.data.SettlementEntity

/**
 * v2.3.6 P3 "Money check": a read-only audit of one trip's money, recomputed from the exact rows
 * the balances use (payer += total, member -= share, payer of a settlement += amount, receiver -=).
 *
 * Why it exists (RCA, Bike Rentals): an expense showed a ₹7,950 total while its shares added up to
 * ₹4,950. Balances credit the payer with the total and charge members their shares, so ₹3,000 of
 * credit had no matching debt and the settle plan routed a real payment to the wrong person. Nothing
 * checked that the numbers add up, so the gap stayed invisible for days. This check makes every
 * such gap visible the moment it appears.
 *
 * Pure and O(expenses + splits + settlements): safe to recompute whenever the lists change. It
 * never writes anything.
 */
object MoneyCheck {

    enum class IssueKind {
        /** The expense total and the sum of its shares differ (the RCA bug). */
        SHARES_DONT_MATCH_TOTAL,
        /** An expense with a total but no share rows: the payer is credited and nobody owes. */
        NO_SHARES,
        /** An expense total of zero or less. */
        NON_POSITIVE_TOTAL,
        /** A share below zero. */
        NEGATIVE_SHARE,
        /** The payer or someone sharing the expense is not in the trip. */
        PERSON_NOT_IN_TRIP,
        /** The same payment saved twice (exact twin recorded after the debt was already cleared). */
        PAYMENT_COUNTED_TWICE,
        /** A payment of zero or less, to oneself, or to / from someone not in the trip. */
        INVALID_PAYMENT
    }

    data class Issue(
        val kind: IssueKind,
        val expenseId: String? = null,
        val settlementId: String? = null,
        val title: String,
        val totalCents: Long = 0L,
        val sharesCents: Long = 0L
    ) {
        /** Positive when the total is larger than the shares. */
        val gapCents: Long get() = totalCents - sharesCents
    }

    data class Report(
        val expenseCount: Int,
        val expensesAddingUp: Int,
        val paymentCount: Int,
        /** Σ expense totals. */
        val totalPaidCents: Long,
        /** Σ shares. */
        val totalSharedCents: Long,
        /** Σ of positive balances ("to receive"). */
        val toReceiveCents: Long,
        /** Σ of |negative balances| ("owed"). */
        val owedCents: Long,
        val issues: List<Issue>
    ) {
        val everyExpenseAddsUp: Boolean get() = issues.none { it.expenseId != null }
        val balancesEvenOut: Boolean get() = toReceiveCents == owedCents
        val noPaymentCountedTwice: Boolean get() = issues.none { it.kind == IssueKind.PAYMENT_COUNTED_TWICE }
        val paymentsValid: Boolean get() = issues.none { it.kind == IssueKind.INVALID_PAYMENT }
        val passed: Boolean get() = issues.isEmpty() && balancesEvenOut

        /** Expense ids that fail, in display order (largest gap first). */
        val failingExpenseIds: List<String>
            get() = issues.mapNotNull { it.expenseId }.distinct()

        /** Total size of the money gap across failing expenses (absolute paise). */
        val totalGapCents: Long get() = kotlin.math.abs(toReceiveCents - owedCents)
    }

    fun run(
        members: List<GroupMemberEntity>,
        expenses: List<ExpenseEntity>,
        splits: List<ExpenseSplitEntity>,
        settlements: List<SettlementEntity>
    ): Report {
        val memberIds = members.map { it.memberId }.toSet()
        val expenseIds = expenses.map { it.expenseId }.toSet()
        val splitsByExpense = splits.filter { it.expenseId in expenseIds }.groupBy { it.expenseId }
        val issues = mutableListOf<Issue>()
        var addingUp = 0

        for (exp in expenses) {
            val rows = splitsByExpense[exp.expenseId].orEmpty()
            val shares = rows.sumOf { it.finalOwedCents }
            val name = exp.title.substringBefore(" · ").trim().ifBlank { "An expense" }
            val issue = when {
                exp.totalAmountCents <= 0L ->
                    Issue(IssueKind.NON_POSITIVE_TOTAL, exp.expenseId, title = name, totalCents = exp.totalAmountCents, sharesCents = shares)
                rows.isEmpty() ->
                    Issue(IssueKind.NO_SHARES, exp.expenseId, title = name, totalCents = exp.totalAmountCents, sharesCents = 0L)
                rows.any { it.finalOwedCents < 0L } ->
                    Issue(IssueKind.NEGATIVE_SHARE, exp.expenseId, title = name, totalCents = exp.totalAmountCents, sharesCents = shares)
                shares != exp.totalAmountCents ->
                    Issue(IssueKind.SHARES_DONT_MATCH_TOTAL, exp.expenseId, title = name, totalCents = exp.totalAmountCents, sharesCents = shares)
                memberIds.isNotEmpty() && (exp.payerId !in memberIds || rows.any { it.memberId !in memberIds }) ->
                    Issue(IssueKind.PERSON_NOT_IN_TRIP, exp.expenseId, title = name, totalCents = exp.totalAmountCents, sharesCents = shares)
                else -> null
            }
            if (issue == null) addingUp++ else issues += issue
        }

        val validSettlements = mutableListOf<SettlementEntity>()
        for (st in settlements) {
            val invalid = st.amountCents <= 0L ||
                st.fromMemberId == st.toMemberId ||
                (memberIds.isNotEmpty() && (st.fromMemberId !in memberIds || st.toMemberId !in memberIds))
            if (invalid) {
                issues += Issue(
                    IssueKind.INVALID_PAYMENT,
                    settlementId = st.settlementId,
                    title = "${st.fromMemberName} → ${st.toMemberName}",
                    totalCents = st.amountCents
                )
            } else {
                validSettlements += st
            }
        }
        SettlementDuplicateGuard.redundantDuplicateIds(expenses, splits, validSettlements).forEach { id ->
            val st = validSettlements.first { it.settlementId == id }
            issues += Issue(
                IssueKind.PAYMENT_COUNTED_TWICE,
                settlementId = id,
                title = "${st.fromMemberName} → ${st.toMemberName}",
                totalCents = st.amountCents
            )
        }

        // Same formula as SplitMateViewModel.computeGroupNetBalances, including ghost keys, so a
        // share or payment pointing at someone outside the trip still shows up as an uneven total.
        val net = HashMap<String, Long>()
        memberIds.forEach { net[it] = 0L }
        expenses.forEach { net[it.payerId] = (net[it.payerId] ?: 0L) + it.totalAmountCents }
        splitsByExpense.values.flatten().forEach { net[it.memberId] = (net[it.memberId] ?: 0L) - it.finalOwedCents }
        settlements.forEach {
            net[it.fromMemberId] = (net[it.fromMemberId] ?: 0L) + it.amountCents
            net[it.toMemberId] = (net[it.toMemberId] ?: 0L) - it.amountCents
        }

        val sortedIssues = issues.sortedWith(
            compareByDescending<Issue> { it.expenseId != null }
                .thenByDescending { kotlin.math.abs(it.gapCents) }
        )
        return Report(
            expenseCount = expenses.size,
            expensesAddingUp = addingUp,
            paymentCount = settlements.size,
            totalPaidCents = expenses.sumOf { it.totalAmountCents },
            totalSharedCents = splitsByExpense.values.sumOf { list -> list.sumOf { it.finalOwedCents } },
            toReceiveCents = net.values.filter { it > 0L }.sum(),
            owedCents = -net.values.filter { it < 0L }.sum(),
            issues = sortedIssues
        )
    }

    /**
     * True when the 1-tap "Use shares total" fix is safe: a plain (non-itemized) expense whose
     * shares are present and positive. Itemized receipts (tax / tip / remainder / multiplier /
     * unequal item claims) must be fixed by editing their items instead.
     */
    fun canUseSharesTotal(expense: ExpenseEntity, splits: List<ExpenseSplitEntity>): Boolean {
        val rows = splits.filter { it.expenseId == expense.expenseId }
        val shares = rows.sumOf { it.finalOwedCents }
        if (rows.isEmpty() || shares <= 0L || shares == expense.totalAmountCents) return false
        if (rows.any { it.finalOwedCents < 0L }) return false
        return !ItemizedExpenseGuard.isItemized(expense, rows)
    }
}

/** v2.3.6 P3: what the Money check footer knows about the group copy. */
data class MoneyCheckSyncStatus(
    val isSyncing: Boolean = false,
    val isOffline: Boolean = false,
    val hasPendingPush: Boolean = false,
    /** When this phone last confirmed its copy equals the group's, or 0 if not yet this session. */
    val lastConfirmedEpochMs: Long = 0L
)

/**
 * v2.3.6 P3: plain-language copy (audited). No "ledger", "books", "drift" or "cloud" on screen;
 * amounts use whole rupees when the paise are zero.
 */
object MoneyCheckCopy {
    const val HOLD_OFF = "These payments may change. Hold off until it's fixed."
    const val ASK_TO_FIX = "Ask the organizer, the payer or whoever added it to fix it."
    const val MARK_PAID_WARNING = "Some amounts in this trip don't add up, so this payment may change."

    private fun rs(cents: Long) = com.splitmate.app.ui.formatIndianRupeesFromCents(cents, trimZeroDecimals = true)
    private fun plural(n: Int, one: String, many: String = one + "s") = if (n == 1) "$n $one" else "$n $many"

    private fun failingExpenses(r: MoneyCheck.Report) = r.failingExpenseIds.size
    private fun thingsToFix(r: MoneyCheck.Report): Int =
        (r.failingExpenseIds.size + r.issues.count { it.expenseId == null }).coerceAtLeast(if (r.passed) 0 else 1)

    fun statusTitle(r: MoneyCheck.Report, sync: MoneyCheckSyncStatus): String = when {
        !r.passed -> "${plural(thingsToFix(r), "thing")} to fix"
        sync.isSyncing -> "Checking with the group…"
        sync.isOffline -> "Checked on this phone · offline"
        r.expenseCount == 0 -> "All amounts add up"
        else -> "All amounts add up · ${plural(r.expenseCount, "expense")}"
    }

    fun sheetTitle(r: MoneyCheck.Report): String =
        if (r.passed) "Everything adds up" else "${plural(thingsToFix(r), "thing")} to fix"

    fun failureTitle(r: MoneyCheck.Report, titleOf: (String) -> String): String {
        val n = failingExpenses(r)
        return when {
            n == 1 -> "${titleOf(r.failingExpenseIds.first())} doesn't add up"
            n > 1 -> "$n expenses don't add up"
            r.issues.any { it.kind == MoneyCheck.IssueKind.PAYMENT_COUNTED_TWICE } -> "A payment was counted twice"
            r.issues.isNotEmpty() -> "A payment needs a look"
            else -> "Balances don't even out"
        }
    }

    fun failureBody(r: MoneyCheck.Report): String {
        val expenseIssues = r.issues.filter { it.expenseId != null }
        return when {
            expenseIssues.size == 1 -> issueSentence(expenseIssues.first()) +
                if (r.totalGapCents > 0L) " Balances are off by ${rs(r.totalGapCents)}." else ""
            expenseIssues.size > 1 -> if (r.totalGapCents > 0L) "Balances are off by ${rs(r.totalGapCents)}. Review them before anyone pays."
            else "Review them before anyone pays."
            r.issues.isNotEmpty() -> issueSentence(r.issues.first())
            else -> "${rs(r.owedCents)} owed but ${rs(r.toReceiveCents)} to receive."
        }
    }

    /** One full sentence for the failure card. */
    fun issueSentence(i: MoneyCheck.Issue): String = when (i.kind) {
        MoneyCheck.IssueKind.SHARES_DONT_MATCH_TOTAL ->
            "The total says ${rs(i.totalCents)} but the shares add up to ${rs(i.sharesCents)}."
        MoneyCheck.IssueKind.NO_SHARES -> "It has a total of ${rs(i.totalCents)} but nobody is sharing it."
        MoneyCheck.IssueKind.NON_POSITIVE_TOTAL -> "Its total is ${rs(i.totalCents)}."
        MoneyCheck.IssueKind.NEGATIVE_SHARE -> "One of its shares is below zero."
        MoneyCheck.IssueKind.PERSON_NOT_IN_TRIP -> "Someone in it is no longer in the trip."
        MoneyCheck.IssueKind.PAYMENT_COUNTED_TWICE -> "${i.title} ${rs(i.totalCents)} was saved twice."
        MoneyCheck.IssueKind.INVALID_PAYMENT -> "${i.title} ${rs(i.totalCents)} can't be right."
    }

    /** Short line under an issue in the sheet. */
    fun issueLine(i: MoneyCheck.Issue): String = when (i.kind) {
        MoneyCheck.IssueKind.SHARES_DONT_MATCH_TOTAL ->
            "Total ${rs(i.totalCents)} · shares ${rs(i.sharesCents)} · off by ${rs(kotlin.math.abs(i.gapCents))}"
        else -> issueSentence(i)
    }

    fun expensesDetail(r: MoneyCheck.Report): String = when {
        r.expenseCount == 0 -> "No expenses yet"
        r.everyExpenseAddsUp -> "${plural(r.expenseCount, "expense")}, shares match each total"
        else -> "${failingExpenses(r)} of ${r.expenseCount} don't add up"
    }

    fun balancesDetail(r: MoneyCheck.Report): String =
        if (r.balancesEvenOut) "${rs(r.owedCents)} owed = ${rs(r.toReceiveCents)} to receive"
        else "${rs(r.owedCents)} owed · ${rs(r.toReceiveCents)} to receive"

    fun paymentsDetail(r: MoneyCheck.Report): String {
        val twice = r.issues.count { it.kind == MoneyCheck.IssueKind.PAYMENT_COUNTED_TWICE }
        return when {
            twice > 0 -> "${plural(twice, "payment")} saved twice"
            r.paymentCount == 0 -> "No payments yet"
            else -> "${plural(r.paymentCount, "payment")} checked"
        }
    }

    fun syncLine(s: MoneyCheckSyncStatus, nowMs: Long): String = when {
        s.isSyncing -> "Checking with the group…"
        s.isOffline -> "Checked on this phone · offline"
        s.hasPendingPush -> "Checked on this phone · your changes are waiting to upload"
        s.lastConfirmedEpochMs > 0L -> "Up to date with the group · ${ago(nowMs - s.lastConfirmedEpochMs)}"
        else -> "Checked on this phone"
    }

    private fun ago(ms: Long): String {
        val min = (ms.coerceAtLeast(0L) / 60_000L)
        return when {
            min < 1 -> "just now"
            min < 60 -> "$min min ago"
            min < 24 * 60 -> "${min / 60} h ago"
            else -> plural((min / (24 * 60)).toInt(), "day") + " ago"
        }
    }
}
