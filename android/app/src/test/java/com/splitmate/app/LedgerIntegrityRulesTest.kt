package com.splitmate.app

import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.SettlementEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** v2.3.6: guards found from real trip data (split total drift, duplicate settlement). */
class LedgerIntegrityRulesTest {

    private fun expense(id: String, total: Long, base: Long = total) = ExpenseEntity(
        expenseId = id, groupId = "g", title = "Bike Rentals", payerId = "m2",
        baseSubtotalCents = base, taxCents = 0, tipCents = 0, totalAmountCents = total,
        lockedMultiplier = 1.0, unassignedBaseCents = 0, currencyCode = "INR", lockedExchangeRate = 1.0
    )

    private fun split(exp: String, idx: Int, member: String, cents: Long) =
        ExpenseSplitEntity("${exp}_sp_$idx", exp, member, cents, cents)

    @Test
    fun LI_01_realTripDrift_staleTotalIsCorrectedFromSplits() {
        // Real data: edited to ₹4,950 (6 × ₹825) but the total came back as ₹7,950.
        val exp = expense("e1", 795_000)
        val rows = (0 until 6).map { split("e1", it, "m$it", 82_500) }
        assertFalse(ExpenseSplitIntegrity.isConsistent(exp, rows))
        val fixed = ExpenseSplitIntegrity.repairStaleTotal(exp, rows)
        assertEquals(495_000L, fixed.totalAmountCents)
        assertEquals(495_000L, fixed.baseSubtotalCents)
        assertTrue(ExpenseSplitIntegrity.isConsistent(fixed, rows))
    }

    @Test
    fun LI_02_itemizedReceiptsAreNeverRewritten() {
        val itemized = expense("e2", 1_100, base = 1_000).copy(taxCents = 100, lockedMultiplier = 1.1)
        val rows = listOf(split("e2", 0, "a", 500), split("e2", 1, "b", 400))
        assertSame(itemized, ExpenseSplitIntegrity.repairStaleTotal(itemized, rows))
    }

    @Test
    fun LI_03_consistentExpensesAreReturnedUntouched() {
        val exp = expense("e4", 300)
        val rows = (0 until 3).map { split("e4", it, "m$it", 100) }
        assertSame(exp, ExpenseSplitIntegrity.repairStaleTotal(exp, rows))
        assertEquals(listOf(exp), ExpenseSplitIntegrity.repairStaleTotals(listOf(exp), rows))
    }

    private fun settle(id: String, from: String, to: String, amt: Long, at: Long) =
        SettlementEntity(id, "g", from, from, to, to, amt, "INR", 1.0, settledAt = at)

    @Test
    fun LI_04_duplicateTapOrStaleRowIsRefused() {
        val now = 10_000_000L
        val existing = listOf(settle("s1", "akshay", "nikhil", 143_913, now - 5_000))
        // Same payment again within the window.
        assertTrue(
            SettlementDuplicateGuard.isDuplicateOrAlreadySettled(
                "akshay", "nikhil", 143_913, existing,
                mapOf("akshay" to -1L, "nikhil" to 1L), now
            )
        )
        // Payer no longer owes anything: the transfer was already settled.
        assertTrue(
            SettlementDuplicateGuard.isDuplicateOrAlreadySettled(
                "akshay", "nikhil", 143_913, emptyList(),
                mapOf("akshay" to 0L, "nikhil" to 143_913L), now
            )
        )
        // A genuine outstanding transfer is allowed.
        assertFalse(
            SettlementDuplicateGuard.isDuplicateOrAlreadySettled(
                "gaurii", "siddhesh", 319_396, existing,
                mapOf("gaurii" to -319_396L, "siddhesh" to 319_396L), now
            )
        )
    }

    // Akshay owes Nikhil ₹1,439.13 for one shared expense.
    private val owedExpense = ExpenseEntity(
        expenseId = "dinner", groupId = "g", title = "Dinner", payerId = "nikhil",
        baseSubtotalCents = 287_826, taxCents = 0, tipCents = 0, totalAmountCents = 287_826,
        lockedMultiplier = 1.0, unassignedBaseCents = 0, currencyCode = "INR", lockedExchangeRate = 1.0
    )
    private val owedSplits = listOf(split("dinner", 0, "akshay", 143_913), split("dinner", 1, "nikhil", 143_913))
    private val t0 = 1_791_200_244_664L

    @Test
    fun LI_05_realTripDuplicate_laterTwinIsRemoved() {
        // Real data: the same ₹1,439.13 recorded twice, ~8 minutes apart.
        val first = settle("settle_1791200244664", "akshay", "nikhil", 143_913, t0)
        val dup = settle("settle_1791200732303", "akshay", "nikhil", 143_913, t0 + 488_000)
        assertEquals(
            setOf(dup.settlementId),
            SettlementDuplicateGuard.redundantDuplicateIds(listOf(owedExpense), owedSplits, listOf(first, dup))
        )
    }

    @Test
    fun LI_06_twinsFarApartAreTwoRealPayments() {
        val first = settle("a", "akshay", "nikhil", 143_913, t0)
        val later = settle("b", "akshay", "nikhil", 143_913, t0 + SettlementDuplicateGuard.CleanupWindowMillis + 1)
        assertTrue(SettlementDuplicateGuard.redundantDuplicateIds(listOf(owedExpense), owedSplits, listOf(first, later)).isEmpty())
    }

    @Test
    fun LI_07_twinIsKeptWhilePayerStillOwes() {
        // Akshay owed twice as much, so two identical payments are both genuine.
        val bigger = owedSplits.map { if (it.memberId == "akshay") it.copy(finalOwedCents = 287_826) else it }
        val expense = owedExpense.copy(totalAmountCents = 431_739, baseSubtotalCents = 431_739)
        val first = settle("a", "akshay", "nikhil", 143_913, t0)
        val second = settle("b", "akshay", "nikhil", 143_913, t0 + 60_000)
        assertTrue(SettlementDuplicateGuard.redundantDuplicateIds(listOf(expense), bigger, listOf(first, second)).isEmpty())
    }

    @Test
    fun LI_08_differentAmountsOrPeopleAreNeverTouched() {
        val first = settle("a", "akshay", "nikhil", 143_913, t0)
        val other = settle("b", "gaurii", "siddhesh", 319_396, t0 + 1_000)
        val near = settle("c", "akshay", "nikhil", 143_912, t0 + 2_000)
        assertTrue(SettlementDuplicateGuard.redundantDuplicateIds(listOf(owedExpense), owedSplits, listOf(first, other, near)).isEmpty())
    }

    @Test
    fun LI_09_manualAmountParsesExactlyToPaise() {
        assertEquals(319_396L, ManualPaymentInput.parseAmountToCents("3193.96"))
        assertEquals(319_396L, ManualPaymentInput.parseAmountToCents("₹3,193.96"))
        assertEquals(50_000L, ManualPaymentInput.parseAmountToCents("500"))
        assertEquals(50_050L, ManualPaymentInput.parseAmountToCents("500.5"))
        assertEquals(null, ManualPaymentInput.parseAmountToCents("0"))
        assertEquals(null, ManualPaymentInput.parseAmountToCents("12.345"))
        assertEquals(null, ManualPaymentInput.parseAmountToCents("-5"))
        assertEquals(null, ManualPaymentInput.parseAmountToCents("abc"))
    }
}
