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
    fun LI_01_realTripDrift_isRepairedToTotalWithEqualShares() {
        // Real data: total ₹7,950 but six rows of ₹825 (= ₹4,950).
        val exp = expense("e1", 795_000)
        val rows = (0 until 6).map { split("e1", it, "m$it", 82_500) }
        assertFalse(ExpenseSplitIntegrity.isConsistent(exp, rows))
        val fixed = ExpenseSplitIntegrity.reconcile(exp, rows)
        assertEquals(795_000L, fixed.sumOf { it.finalOwedCents })
        assertTrue(fixed.all { it.finalOwedCents == 132_500L })
        assertEquals(795_000L, fixed.sumOf { it.baseClaimedCents })
        assertEquals(rows.map { it.splitId }, fixed.map { it.splitId })
    }

    @Test
    fun LI_02_unequalProportionsKeptAndPenniesReconciled() {
        val exp = expense("e2", 1_000)
        val rows = listOf(split("e2", 0, "a", 200), split("e2", 1, "b", 100), split("e2", 2, "c", 100))
        val fixed = ExpenseSplitIntegrity.reconcile(exp, rows)
        assertEquals(1_000L, fixed.sumOf { it.finalOwedCents })
        assertEquals(500L, fixed[0].finalOwedCents)
        assertEquals(listOf(500L, 250L, 250L), fixed.map { it.finalOwedCents })

        val odd = expense("e3", 1_000)
        val three = (0 until 3).map { split("e3", it, "m$it", 100) }
        val fixedOdd = ExpenseSplitIntegrity.reconcile(odd, three)
        assertEquals(1_000L, fixedOdd.sumOf { it.finalOwedCents })
        assertEquals(1, fixedOdd.count { it.plusOneCent })
    }

    @Test
    fun LI_03_consistentSplitsAreReturnedUntouched() {
        val exp = expense("e4", 300)
        val rows = (0 until 3).map { split("e4", it, "m$it", 100) }
        assertSame(rows, ExpenseSplitIntegrity.reconcile(exp, rows))
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
}
