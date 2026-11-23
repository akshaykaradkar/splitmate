package com.splitmate.app

import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.data.SettlementEntity
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.export.GroupLedgerCsvWriter
import com.splitmate.app.ui.export.GroupLedgerExportBuilder
import com.splitmate.app.ui.export.GroupLedgerXlsxWriter
import com.splitmate.app.ui.fund.ADVANCE_ID_PREFIX
import com.splitmate.app.ui.fund.TripFundCalculator
import com.splitmate.app.ui.fund.isTripFundAdvance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.ZoneOffset
import java.util.zip.ZipInputStream

/**
 * SplitMate v2.4.0-rc24 — **Trip Fund** (pre-trip advances & cash-in-hand) verification suite.
 */
@DisplayName("SplitMate v2.4.0-rc24 Trip Fund (Pre-Trip Advances & Cash in Hand)")
class SplitMateV240Rc24TripFundTest {

    private val members = listOf(
        GroupMemberEntity("m_ak", "g1", "Akshay", "s1", isCurrentUser = true),
        GroupMemberEntity("m_pr", "g1", "Priya", "s2"),
        GroupMemberEntity("m_sm", "g1", "Sam", "s3"),
        GroupMemberEntity("m_rj", "g1", "Raj", "s4")
    )

    private fun advance(id: String, fromId: String, fromName: String, cents: Long) = SettlementEntity(
        settlementId = "${ADVANCE_ID_PREFIX}$id",
        groupId = "g1",
        fromMemberId = fromId,
        fromMemberName = fromName,
        toMemberId = "m_ak",
        toMemberName = "Akshay",
        amountCents = cents,
        currencyCode = "INR",
        lockedExchangeRate = 1.0,
        settledAt = 1_760_000_000_000L
    )

    private fun expense(id: String, payerId: String, totalCents: Long) = ExpenseEntity(
        expenseId = id,
        groupId = "g1",
        title = "Trip Expense $id",
        payerId = payerId,
        baseSubtotalCents = totalCents,
        taxCents = 0L,
        tipCents = 0L,
        totalAmountCents = totalCents,
        lockedMultiplier = 1.0,
        unassignedBaseCents = 0L,
        currencyCode = "INR",
        lockedExchangeRate = 1.0,
        createdAt = 1_760_050_000_000L
    )

    @Test
    fun `RC24_01 under-spend vs over-spend Trip Fund cash-in-hand and custodial balance math`() {
        val advances = listOf(
            advance("1", "m_pr", "Priya", 500_000L), // ₹5,000
            advance("2", "m_sm", "Sam", 500_000L),   // ₹5,000
            advance("3", "m_rj", "Raj", 500_000L)    // ₹5,000
        )
        assertTrue(advances.all { it.isTripFundAdvance() })

        // Case A: Day 0 (₹0 spent) -> Holding ₹15,000 Trip Fund cash (custodial, not alarming debt)
        val day0 = TripFundCalculator.summarize("g1", members, emptyList(), advances).single()
        assertEquals(1_500_000L, day0.totalCollectedCents)
        assertEquals(0L, day0.treasurerSpentCents)
        assertEquals(1_500_000L, day0.cashInHandCents)
        assertFalse(day0.isPoolDepleted)
        assertTrue(TripFundCalculator.isCustodialPoolBalance("m_ak", -1_500_000L, listOf(day0)))

        // Case B: Under-spend (Akshay spends ₹12,000 total -> ₹3,000 cash left in hand)
        val underSpend = TripFundCalculator.summarize("g1", members, listOf(expense("e1", "m_ak", 1_200_000L)), advances).single()
        assertEquals(300_000L, underSpend.cashInHandCents)
        assertFalse(underSpend.isPoolDepleted)
        assertTrue(TripFundCalculator.isCustodialPoolBalance("m_ak", -600_000L, listOf(underSpend)))

        // Case C: Over-spend (Akshay spends ₹24,000 total -> pool depleted, ₹9,000 out of pocket)
        val overSpend = TripFundCalculator.summarize("g1", members, listOf(expense("e2", "m_ak", 2_400_000L)), advances).single()
        assertEquals(-900_000L, overSpend.cashInHandCents)
        assertTrue(overSpend.isPoolDepleted)
        assertFalse(TripFundCalculator.isCustodialPoolBalance("m_ak", 300_000L, listOf(overSpend)))
    }

    @Test
    fun `RC24_02 anyone in group can batch-record Trip Fund advances without inflating Total Trip Spend`() {
        val vm = SplitMateViewModel()
        vm.createNewGroup(name = "Manali Trip", currencyCode = "INR", friendNamesCsv = "Priya, Sam, Raj")
        val stateBefore = vm.uiState.value
        val activeMembers = stateBefore.activeGroupMembers
        val priya = activeMembers.first { it.name == "Priya" }
        val sam = activeMembers.first { it.name == "Sam" }
        val raj = activeMembers.first { it.name == "Raj" }

        // Even when Priya (not the current user) is the Treasurer, anyone can record Trip Fund advances.
        val err = vm.recordTripFundAdvances(
            treasurerMemberId = priya.memberId,
            contributionsByMemberId = mapOf(
                sam.memberId to 500_000L,
                raj.memberId to 300_000L
            )
        )
        assertNull(err)

        val stateAfter = vm.uiState.value
        val groupId = stateAfter.activeGroupId
        assertEquals(0, stateAfter.expenses.count { it.groupId == groupId }, "Trip Fund advances must NEVER create ExpenseEntity rows")
        val recordedAdvances = stateAfter.settlements.filter { it.groupId == groupId && it.isTripFundAdvance() }
        assertEquals(2, recordedAdvances.size)
        assertEquals(800_000L, recordedAdvances.sumOf { it.amountCents })

        // Undo removes the advance cleanly
        vm.undoSettlement(recordedAdvances.first().settlementId, groupIdHint = groupId)
        val remainingAdvances = vm.uiState.value.settlements.filter { it.groupId == groupId && it.isTripFundAdvance() }
        assertEquals(1, remainingAdvances.size)
    }

    @Test
    fun `RC24_03 Excel and CSV exports separate Trip Fund advances from recorded settlements with 0_00c drift`() {
        val exp = expense("e1", "m_ak", 1_200_000L) // ₹12,000 equal among 4 (₹3,000 each)
        val splits = members.mapIndexed { idx, m ->
            ExpenseSplitEntity("sp_$idx", "e1", m.memberId, 300_000L, 300_000L, false)
        }
        val adv = advance("1", "m_pr", "Priya", 500_000L) // ₹5,000 Trip Fund advance
        val settle = SettlementEntity(
            settlementId = "settle_1",
            groupId = "g1",
            fromMemberId = "m_sm",
            fromMemberName = "Sam",
            toMemberId = "m_ak",
            toMemberName = "Akshay",
            amountCents = 100_000L,
            currencyCode = "INR",
            lockedExchangeRate = 1.0,
            settledAt = 1_760_100_000_000L
        )
        val model = GroupLedgerExportBuilder.build(
            groupId = "g1",
            groupName = "Spiti Trip",
            members = members,
            expenses = listOf(exp),
            splits = splits,
            settlements = listOf(adv, settle),
            zoneId = ZoneOffset.UTC
        )
        assertEquals(1_200_000L, model.totalSpendCents, "Total Trip Spend excludes Trip Fund advances")
        assertEquals(1, model.tripFundAdvances.size)
        assertEquals(1, model.recordedSettlements.size)
        assertEquals(0L, model.memberBalances.sumOf { it.netBalanceCents })

        val csv = GroupLedgerCsvWriter.writeToString(model)
        assertTrue(csv.contains("TRIP FUND ADVANCES (EXCLUDED FROM TOTAL TRIP SPEND)"))
        assertTrue(csv.contains("Trip Fund Advance"))
        assertTrue(csv.contains("RECORDED / PAID SETTLEMENTS"))

        val xlsxBytes = GroupLedgerXlsxWriter.writeToBytes(model)
        val sheet3 = buildString {
            ZipInputStream(ByteArrayInputStream(xlsxBytes)).use { zis ->
                var e = zis.nextEntry
                while (e != null) {
                    if (e.name == "xl/worksheets/sheet3.xml") {
                        append(zis.readBytes().toString(StandardCharsets.UTF_8))
                    }
                    zis.closeEntry()
                    e = zis.nextEntry
                }
            }
        }
        assertTrue(sheet3.contains("TOTAL TRIP FUND"))
        assertTrue(sheet3.contains("Trip Fund Advance"))
        assertTrue(sheet3.contains("TOTAL PAID"))
    }

    @Test
    fun `RC24_04 TripHomeScreen wires Trip Fund in FAB menu, Money tab card, and custodial balance chip`() {
        val trip = File("src/main/java/com/splitmate/app/ui/screens/TripHomeScreen.kt").readText()
        assertTrue(trip.contains("label = \"Trip Fund\""))
        assertTrue(trip.contains("TripFundSummaryCard("))
        assertTrue(trip.contains("CollectTripFundButton("))
        assertTrue(trip.contains("CollectTripFundSheet("))
        assertTrue(trip.contains("Holding \$formattedAbs Trip Fund"))
    }
}
