package com.splitmate.app

import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.data.SettlementEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** v2.3.6 P3: the read-only Money check (RCA: Bike Rentals ₹7,950 total vs ₹4,950 shares). */
class MoneyCheckTest {

    private val members = (0 until 6).map { GroupMemberEntity("m$it", "g", "P$it", "P$it") }

    private fun expense(id: String, total: Long, payer: String = "m4", base: Long = total, tax: Long = 0) = ExpenseEntity(
        expenseId = id, groupId = "g", title = "Bike Rentals", payerId = payer,
        baseSubtotalCents = base, taxCents = tax, tipCents = 0, totalAmountCents = total,
        lockedMultiplier = 1.0, unassignedBaseCents = 0, currencyCode = "INR", lockedExchangeRate = 1.0
    )

    private fun equalSplits(exp: String, each: Long, who: List<String> = members.map { it.memberId }) =
        who.mapIndexed { i, m -> ExpenseSplitEntity("${exp}_sp_$i", exp, m, each, each) }

    private fun pay(id: String, from: String, to: String, cents: Long, at: Long = 1_000L) =
        SettlementEntity(id, "g", from, from, to, to, cents, "INR", 1.0, settledAt = at)

    @Test
    fun MC_01_consistentTripPasses() {
        val r = MoneyCheck.run(members, listOf(expense("e1", 495_000)), equalSplits("e1", 82_500), emptyList())
        assertTrue(r.passed)
        assertEquals(1, r.expensesAddingUp)
        assertEquals(r.toReceiveCents, r.owedCents)
    }

    @Test
    fun MC_02_rcaBikeRentalsDriftFailsWithExactGap() {
        val r = MoneyCheck.run(members, listOf(expense("e1", 795_000)), equalSplits("e1", 82_500), emptyList())
        assertFalse(r.passed)
        assertFalse(r.everyExpenseAddsUp)
        assertFalse(r.balancesEvenOut)
        val issue = r.issues.single()
        assertEquals(MoneyCheck.IssueKind.SHARES_DONT_MATCH_TOTAL, issue.kind)
        assertEquals(300_000L, issue.gapCents)
        assertEquals(300_000L, r.totalGapCents)
    }

    @Test
    fun MC_03_expenseWithNoSharesFails() {
        val r = MoneyCheck.run(members, listOf(expense("e1", 10_000)), emptyList(), emptyList())
        assertEquals(MoneyCheck.IssueKind.NO_SHARES, r.issues.single().kind)
        assertFalse(r.passed)
    }

    @Test
    fun MC_04_shareForSomeoneOutsideTheTripFails() {
        val splits = equalSplits("e1", 50_000, listOf("m0", "ghost"))
        val r = MoneyCheck.run(members, listOf(expense("e1", 100_000)), splits, emptyList())
        assertEquals(MoneyCheck.IssueKind.PERSON_NOT_IN_TRIP, r.issues.single().kind)
    }

    @Test
    fun MC_05_invalidPaymentsFail() {
        val ok = expense("e1", 60_000)
        val splits = equalSplits("e1", 10_000)
        val r = MoneyCheck.run(members, listOf(ok), splits, listOf(pay("s1", "m0", "m0", 100), pay("s2", "m1", "m4", 0)))
        assertEquals(2, r.issues.count { it.kind == MoneyCheck.IssueKind.INVALID_PAYMENT })
        assertFalse(r.paymentsValid)
    }

    @Test
    fun MC_06_paymentCountedTwiceFails_singlePaymentPasses() {
        val exp = expense("e1", 60_000, payer = "m4")
        val splits = equalSplits("e1", 10_000)
        val once = MoneyCheck.run(members, listOf(exp), splits, listOf(pay("s1", "m0", "m4", 10_000, at = 1_000)))
        assertTrue(once.passed)
        val twice = MoneyCheck.run(
            members, listOf(exp), splits,
            listOf(pay("s1", "m0", "m4", 10_000, at = 1_000), pay("s2", "m0", "m4", 10_000, at = 61_000))
        )
        assertFalse(twice.noPaymentCountedTwice)
        assertFalse(twice.passed)
    }

    @Test
    fun MC_07_settlementsKeepBalancesEven() {
        val exp = expense("e1", 60_000, payer = "m4")
        val r = MoneyCheck.run(members, listOf(exp), equalSplits("e1", 10_000), listOf(pay("s1", "m0", "m4", 10_000)))
        assertTrue(r.balancesEvenOut)
        assertEquals(40_000L, r.toReceiveCents)
    }

    @Test
    fun MC_08_useSharesTotalOnlyForPlainMismatches() {
        val rows = equalSplits("e1", 82_500)
        assertTrue(MoneyCheck.canUseSharesTotal(expense("e1", 795_000), rows))
        assertFalse(MoneyCheck.canUseSharesTotal(expense("e1", 495_000), rows)) // already adds up
        assertFalse(MoneyCheck.canUseSharesTotal(expense("e1", 795_000, tax = 5_000), rows)) // itemized
        assertFalse(MoneyCheck.canUseSharesTotal(expense("e1", 795_000), emptyList()))
    }

    @Test
    fun MC_09_largestGapListedFirst() {
        val exps = listOf(expense("a", 10_100), expense("b", 795_000))
        val splits = equalSplits("a", 1_000).take(1).map { it.copy(finalOwedCents = 10_000) } + equalSplits("b", 82_500)
        val r = MoneyCheck.run(members, exps, splits, emptyList())
        assertEquals(listOf("b", "a"), r.failingExpenseIds)
    }
}
