package com.splitmate.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * JUnit 5 Test Suite for SplitMate's Mathematical Engine:
 * - Greedy Minimum Cash Flow Debt Simplification (Zero-Balance, Cyclic Debts, Multi-Node Reduction)
 * - Largest Remainder Float Rounding & Proportional Auxiliary Splitting (`0.00¢ drift`)
 */
class SplitMateDebtSimplificationTest {

    @Test
    @DisplayName("1. Zero-balance inputs produce zero simplified transfers")
    fun testZeroBalanceInputsProduceEmptyTransfers() {
        val zeroBalances = listOf(
            SplitMateMathEngine.MemberNetBalance("m_1", "Akshay", 0L),
            SplitMateMathEngine.MemberNetBalance("m_2", "Sam", 0L),
            SplitMateMathEngine.MemberNetBalance("m_3", "Priya", 0L)
        )

        val transfers = SplitMateMathEngine.simplifyDebtsGreedy(zeroBalances)
        assertTrue(transfers.isEmpty(), "Expected 0 transfers when all member net balances are 0")
    }

    @Test
    @DisplayName("2. Pure 3-way cyclic debt (A owes B $50, B owes C $50, C owes A $50) cancels out to 0 transfers")
    fun testCyclicDebtsCancelOutCompletely() {
        // A paid $50 for B, B paid $50 for C, C paid $50 for A -> everyone's net balance is 0L
        val cyclicNetBalances = listOf(
            SplitMateMathEngine.MemberNetBalance("m_a", "Alice", 5000L - 5000L),
            SplitMateMathEngine.MemberNetBalance("m_b", "Bob", 5000L - 5000L),
            SplitMateMathEngine.MemberNetBalance("m_c", "Charlie", 5000L - 5000L)
        )

        val transfers = SplitMateMathEngine.simplifyDebtsGreedy(cyclicNetBalances)
        assertEquals(0, transfers.size, "Pure cyclic debts must simplify to 0 transfers")
    }

    @Test
    @DisplayName("3. Complex 6-member cross-debt graph simplifies to at most N-1 direct transfers with conservation of flow")
    fun testGreedyMultiMemberReductionAndConservation() {
        val balances = listOf(
            SplitMateMathEngine.MemberNetBalance("m_alex", "Alex", 26370L),   // +$263.70
            SplitMateMathEngine.MemberNetBalance("m_maya", "Maya", -16520L),  // -$165.20
            SplitMateMathEngine.MemberNetBalance("m_kai", "Kai", -9850L),     // -$98.50
            SplitMateMathEngine.MemberNetBalance("m_sam", "Sam", 0L),
            SplitMateMathEngine.MemberNetBalance("m_priya", "Priya", 0L)
        )

        val transfers = SplitMateMathEngine.simplifyDebtsGreedy(balances)

        assertEquals(2, transfers.size, "Expected exactly 2 direct transfers")
        assertEquals(26370L, transfers.sumOf { it.amountCents }, "Total transferred cents must equal total positive credit")
        assertEquals("m_maya", transfers[0].fromMemberId)
        assertEquals("m_alex", transfers[0].toMemberId)
        assertEquals(16520L, transfers[0].amountCents)
        assertEquals("m_kai", transfers[1].fromMemberId)
        assertEquals("m_alex", transfers[1].toMemberId)
        assertEquals(9850L, transfers[1].amountCents)
    }

    @Test
    @DisplayName("4. Float rounding & Largest Remainder reconciliation guarantees 0.00c drift ($42.50 / 3)")
    fun testLargestRemainderFloatRoundingZeroDrift() {
        val splits = SplitMateMathEngine.splitEquallyZeroDrift(
            totalCents = 4250L,
            members = listOf("m_1" to "Alex", "m_2" to "Sam", "m_3" to "Priya")
        )

        assertEquals(3, splits.size)
        assertEquals(4250L, splits.sumOf { it.finalCents }, "Sum of splits must equal exact total (4250 cents) with 0.00c drift")
        assertEquals(1417L, splits[0].finalCents)
        assertTrue(splits[0].plusOneCent)
        assertEquals(1417L, splits[1].finalCents)
        assertTrue(splits[1].plusOneCent)
        assertEquals(1416L, splits[2].finalCents)
    }

    @Test
    @DisplayName("5. Proportional Auxiliary Splitting locks m = T/B and tracks Unassigned Remainder with 0.00c drift")
    fun testProportionalAuxiliaryMultiplierAndRemainderEngine() {
        // Subtotal = $120.00 (12000c), Tax = $10.65 (1065c), Tip = $17.85 (1785c) -> Total = $148.50 (14850c)
        // Multiplier m = 14850 / 12000 = 1.2375
        val result = SplitMateMathEngine.calculateProportionalReceiptSplits(
            baseSubtotalCents = 12000L,
            taxCents = 1065L,
            tipCents = 1785L,
            payerId = "m_alex",
            memberBaseClaimsCents = listOf(
                Triple("m_alex", "Alex", 4200L),
                Triple("m_sam", "Sam", 3800L),
                Triple("m_priya", "Priya", 2600L)
                // Sum claimed = 10600c -> Unassigned Base = 1400c ($14.00)
            ),
            attributeRemainderToPayer = true
        )

        assertEquals(1.2375, result.lockedMultiplier, 0.00001)
        assertEquals(1400L, result.unassignedBaseCents)
        assertEquals(14850L, result.allocations.sumOf { it.finalCents })
        assertEquals(0L, result.driftCents)
    }
}
