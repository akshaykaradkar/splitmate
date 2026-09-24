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

    @Test
    @DisplayName("6. Multi-payment settlement summary verifies Akshay/Gaurav/Gauri/Ninad exact totals without altering simplified transfers")
    fun testMultiPaymentMemberSettlementSummaryExactVerification() {
        // Net balances producing the exact simplified transfers:
        // Akshay: +₹3,630.73 (363073c), Gauri: +₹2,145.28 (214528c),
        // Gaurav: -₹3,894.66 (-389466c), Ninad: -₹1,881.35 (-188135c)
        val balances = listOf(
            SplitMateMathEngine.MemberNetBalance("m_akshay", "Akshay", 363073L),
            SplitMateMathEngine.MemberNetBalance("m_gaurav", "Gaurav Gadhave", -389466L),
            SplitMateMathEngine.MemberNetBalance("m_gauri", "Gauri", 214528L),
            SplitMateMathEngine.MemberNetBalance("m_ninad", "Ninad", -188135L)
        )

        val transfers = SplitMateMathEngine.simplifyDebtsGreedy(balances)
        assertEquals(3, transfers.size, "Must produce exactly 3 simplified settlements")

        val gauravToAkshay = transfers.first { it.fromMemberId == "m_gaurav" && it.toMemberId == "m_akshay" }
        val gauravToGauri = transfers.first { it.fromMemberId == "m_gaurav" && it.toMemberId == "m_gauri" }
        val ninadToGauri = transfers.first { it.fromMemberId == "m_ninad" && it.toMemberId == "m_gauri" }

        assertEquals(363073L, gauravToAkshay.amountCents)
        assertEquals(26393L, gauravToGauri.amountCents)
        assertEquals(188135L, ninadToGauri.amountCents)

        val summaries = SplitMateMathEngine.computeMemberSettlementSummaries(
            transfers = transfers,
            memberMetadata = mapOf(
                "m_akshay" to Triple("Akshay", "Akshay", true),
                "m_gaurav" to Triple("Gaurav Gadhave", "Gaurav", false),
                "m_gauri" to Triple("Gauri", "Gauri", false),
                "m_ninad" to Triple("Ninad", "Ninad", false)
            )
        ).associateBy { it.memberId }

        // Gaurav total to pay: ₹3,894.66 (2 payments: -> Akshay ₹3,630.73, -> Gauri ₹263.93)
        val gauravSummary = summaries.getValue("m_gaurav")
        assertEquals(389466L, gauravSummary.totalOutgoingCents)
        assertEquals("₹3,894.66", gauravSummary.formattedTotalOutgoing)
        assertTrue(gauravSummary.hasMultipleOutgoing)
        assertEquals(2, gauravSummary.outgoingPayments.size)
        assertEquals("₹3,630.73", gauravSummary.outgoingPayments.first { it.counterpartyMemberId == "m_akshay" }.formattedAmount)
        assertEquals("₹263.93", gauravSummary.outgoingPayments.first { it.counterpartyMemberId == "m_gauri" }.formattedAmount)

        // Ninad total to pay: ₹1,881.35 (1 payment: -> Gauri ₹1,881.35, hasMultipleOutgoing == false)
        val ninadSummary = summaries.getValue("m_ninad")
        assertEquals(188135L, ninadSummary.totalOutgoingCents)
        assertEquals("₹1,881.35", ninadSummary.formattedTotalOutgoing)
        assertEquals(false, ninadSummary.hasMultipleOutgoing)
        assertEquals(1, ninadSummary.outgoingPayments.size)
        assertEquals("Gauri", ninadSummary.outgoingPayments.single().counterpartyName)

        // Akshay receives: ₹3,630.73
        val akshaySummary = summaries.getValue("m_akshay")
        assertEquals(363073L, akshaySummary.totalIncomingCents)
        assertEquals("₹3,630.73", akshaySummary.formattedTotalIncoming)
        assertEquals(0L, akshaySummary.totalOutgoingCents)

        // Gauri receives: ₹2,145.28
        val gauriSummary = summaries.getValue("m_gauri")
        assertEquals(214528L, gauriSummary.totalIncomingCents)
        assertEquals("₹2,145.28", gauriSummary.formattedTotalIncoming)
        assertEquals(0L, gauriSummary.totalOutgoingCents)
        assertEquals(2, gauriSummary.incomingPayments.size)
    }

    @Test
    @DisplayName("7. Partial settlement of one leg updates only that specific transfer and transitions multi-payment payer to single-payment")
    fun testPartialSettlementAndDistinctDualDirections() {
        val remainingTransfersAfterGauravPaysGauri = listOf(
            SplitMateMathEngine.SimplifiedTransfer("m_gaurav", "Gaurav Gadhave", "m_akshay", "Akshay", 363073L),
            SplitMateMathEngine.SimplifiedTransfer("m_ninad", "Ninad", "m_gauri", "Gauri", 188135L)
        )

        val summaries = SplitMateMathEngine.computeMemberSettlementSummaries(remainingTransfersAfterGauravPaysGauri)
            .associateBy { it.memberId }

        val gauravAfter = summaries.getValue("m_gaurav")
        assertEquals(363073L, gauravAfter.totalOutgoingCents)
        assertEquals("₹3,630.73", gauravAfter.formattedTotalOutgoing)
        assertEquals(false, gauravAfter.hasMultipleOutgoing, "After settling 1 of 2 payments, hasMultipleOutgoing must become false")

        // Also verify that if a member has both incoming and outgoing transfers, they are kept distinct and never combined
        val dualTransfers = listOf(
            SplitMateMathEngine.SimplifiedTransfer("m_a", "Member A", "m_b", "Member B", 50000L),
            SplitMateMathEngine.SimplifiedTransfer("m_b", "Member B", "m_c", "Member C", 20000L)
        )
        val dualSummaryB = SplitMateMathEngine.computeMemberSettlementSummaries(dualTransfers)
            .first { it.memberId == "m_b" }
        assertTrue(dualSummaryB.hasBothDirections)
        assertEquals(50000L, dualSummaryB.totalIncomingCents)
        assertEquals(20000L, dualSummaryB.totalOutgoingCents)
    }
}
