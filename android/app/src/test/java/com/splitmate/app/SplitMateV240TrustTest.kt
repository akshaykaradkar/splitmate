package com.splitmate.app

import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.SettlementEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.4.0 D5: money trust moments (display-only maths and the Mark paid + Undo flow). */
class SplitMateV240TrustTest {

    private fun exp(id: String, payer: String, total: Long, tax: Long = 0, tip: Long = 0) = ExpenseEntity(
        expenseId = id, groupId = "g", title = id, payerId = payer, baseSubtotalCents = total - tax - tip, taxCents = tax,
        tipCents = tip, totalAmountCents = total, lockedMultiplier = 1.0, unassignedBaseCents = 0, currencyCode = "INR",
        lockedExchangeRate = 1.0, createdAt = 1L
    )

    private fun split(exp: String, member: String, cents: Long) =
        ExpenseSplitEntity(splitId = "${exp}_$member", expenseId = exp, memberId = member, baseClaimedCents = cents, finalOwedCents = cents)

    private fun paid(from: String, to: String, cents: Long) = SettlementEntity(
        settlementId = "s_${from}_$to", groupId = "g", fromMemberId = from, fromMemberName = from, toMemberId = to,
        toMemberName = to, amountCents = cents, currencyCode = "INR", lockedExchangeRate = 1.0
    )

    // A pays 300 split 3 ways, B pays 300 split 3 ways, C pays 300 split 3 ways: everyone is even,
    // but pairwise each traveler owes the other two 100 each way, which nets to zero per pair.
    @Test
    fun `pairwise debts net per pair and drop pairs that cancel out`() {
        val expenses = listOf(exp("e1", "A", 300), exp("e2", "B", 300), exp("e3", "C", 300))
        val splits = listOf("e1", "e2", "e3").flatMap { e -> listOf("A", "B", "C").map { m -> split(e, m, 100) } }
        assertEquals(0, MoneyTrustRules.pairwiseDebtCount(expenses, splits, emptyList()))
    }

    @Test
    fun `a chain of debts counts every pair, and payments between a pair reduce it`() {
        // A paid 200 for A+B, B paid 200 for B+C, C paid 200 for C+D: B owes A, C owes B, D owes C (3 debts).
        val expenses = listOf(exp("e1", "A", 200), exp("e2", "B", 200), exp("e3", "C", 200))
        val splits = listOf(
            split("e1", "A", 100), split("e1", "B", 100),
            split("e2", "B", 100), split("e2", "C", 100),
            split("e3", "C", 100), split("e3", "D", 100)
        )
        assertEquals(3, MoneyTrustRules.pairwiseDebtCount(expenses, splits, emptyList()))
        // D pays C back in full: that pair is closed.
        assertEquals(2, MoneyTrustRules.pairwiseDebtCount(expenses, splits, listOf(paid("D", "C", 100))))
    }

    @Test
    fun `the simplification line only claims a simplification when it is true`() {
        assertEquals("We simplified 7 debts into 2 payments", MoneyTrustRules.simplificationLine(7, 2))
        assertEquals("We simplified 3 debts into 1 payment", MoneyTrustRules.simplificationLine(3, 1))
        assertEquals("2 payments to settle everyone", MoneyTrustRules.simplificationLine(2, 2))
        assertEquals("1 payment to settle everyone", MoneyTrustRules.simplificationLine(1, 1))
        assertEquals("Everyone is settled", MoneyTrustRules.simplificationLine(0, 0))
    }

    @Test
    fun `traveler standing agrees with the trip balance formula`() {
        val expenses = listOf(exp("e1", "A", 4250))
        val splits = listOf(split("e1", "A", 1417), split("e1", "B", 1417), split("e1", "C", 1416))
        val settlements = listOf(paid("B", "A", 1417))
        val s = MoneyTrustRules.standings(listOf("A", "B", "C"), expenses, splits, settlements)

        assertEquals(4250, s.getValue("A").paidCents)
        assertEquals(1417, s.getValue("A").shareCents)
        assertEquals(4250 - 1417 - 1417, s.getValue("A").netCents, "A still gets back C's share")
        assertEquals(0, s.getValue("B").netCents, "B paid A back")
        assertEquals(-1416, s.getValue("C").netCents)
        // Sum of all nets is zero: no money created or lost.
        assertEquals(0, s.values.sumOf { it.netCents })
    }

    @Test
    fun `paid-vs-share bar is full when nothing is owed and partial while owing`() {
        val owing = MoneyTrustRules.TravelerStanding("C", paidCents = 500, shareCents = 2000, sentCents = 500, receivedCents = 0)
        assertEquals(0.5f, owing.coveredFraction, 1e-6f)
        val creditor = MoneyTrustRules.TravelerStanding("A", paidCents = 4000, shareCents = 1000, sentCents = 0, receivedCents = 0)
        assertEquals(1f, creditor.coveredFraction)
        val repaid = MoneyTrustRules.TravelerStanding("A", paidCents = 4000, shareCents = 1000, sentCents = 0, receivedCents = 3000)
        assertEquals(0, repaid.netCents)
        assertEquals(1f, repaid.coveredFraction)
        val nothing = MoneyTrustRules.TravelerStanding("Z", 0, 0, 0, 0)
        assertEquals(1f, nothing.coveredFraction)
    }

    @Test
    fun `tax and tip hint shows only when the bill has tax or tip`() {
        assertTrue(MoneyTrustRules.includesTaxOrTip(exp("e", "A", 1180, tax = 180)))
        assertTrue(MoneyTrustRules.includesTaxOrTip(exp("e", "A", 1100, tip = 100)))
        assertFalse(MoneyTrustRules.includesTaxOrTip(exp("e", "A", 1000)))
    }

    private fun src(path: String) = java.io.File("src/main/java/com/splitmate/app/$path").readText()

    @Test
    fun `mark paid saves at once with Undo everywhere, and asks first only when the money check fails`() {
        val trip = src("ui/screens/TripHomeScreen.kt")
        val app = src("ui/SplitMateAppComposable.kt")
        val flow = src("ui/feel/MarkPaidFlow.kt")
        assertEquals(2, Regex("""markPaidWithUndo\(""").findAll(trip).count(), "my rows + organizer rows")
        assertEquals(2, Regex("""markPaidWithUndo\(""").findAll(app).count(), "settle screen in + out")
        assertTrue(trip.contains("if (markPaidWarning != null) pendingMarkPaidConfirm = true else commitMarkPaid()"))
        assertTrue(trip.contains("onTrigger = requestMarkPaid"), "swipe uses the same flow as the button")
        assertTrue(flow.contains("viewModel.undoSettlement(id, groupIdHint = savedInGroupId)"), "Undo removes exactly the saved settlement, in its own trip")
        val vm = src("ui/SplitMateViewModel.kt")
        assertTrue(vm.contains("fun markTransferPaid(transfer: SplitMateMathEngine.SimplifiedTransfer): String?"))
        assertTrue(src("ui/SplitMateTheme.kt").contains("ProvideMoneySnackbar"))
    }

    @Test
    fun `paid-vs-share bar is the flat M3 indicator, never wavy`() {
        val bar = src("ui/components/TravelerStandingLine.kt")
        assertTrue(bar.contains("LinearProgressIndicator("))
        assertFalse(bar.contains("Wavy"))
        assertTrue(bar.contains("ProgressIndicatorDefaults.ProgressAnimationSpec"))
    }
}
