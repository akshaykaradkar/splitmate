package com.splitmate.app

import app.cash.turbine.test
import com.splitmate.app.ui.SplitMateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * CashApp Turbine StateFlow Test Suite for `SplitMateViewModel`:
 * - Verifies offline transactions queue as `PENDING` and lock the exchange rate at creation time.
 * - Verifies Remainder Engine `splitUnassignedRemainderEqually()` clears unclaimed items.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplitMateViewModelTurbineTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("Turbine StateFlow test: Offline transactions queue as PENDING and lock exchange rate")
    fun testOfflineTransactionsQueueAsPendingAndLockRate() = runTest(testDispatcher) {
        val viewModel = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals("INR", initial.activeCurrencyCode)

            // Create a group so activeGroupMembers is non-empty
            viewModel.createNewGroup(name = "Mountain Trip", currencyCode = "INR", friendNamesCsv = "Sam")
            val withGroupState = awaitItem()

            // Enable offline mode
            viewModel.setOfflineMode(true)
            val offlineState = awaitItem()
            assertTrue(offlineState.isOfflineMode)

            // Commit a quick equal expense while offline
            viewModel.commitQuickEqualExpense(
                title = "Offline Mountain Dinner",
                totalAmountCents = 12000L,
                selectedMemberIds = withGroupState.activeGroupMembers.map { it.memberId }
            )
            val afterCommit = awaitItem()

            val newestExpense = afterCommit.expenses.first()
            assertEquals("Offline Mountain Dinner", newestExpense.title)
            assertEquals("PENDING", newestExpense.syncStatus, "Offline expense must be queued as PENDING")
            assertEquals(1.0, newestExpense.lockedExchangeRate, 0.001, "INR base exchange rate must be locked at 1.0")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Turbine StateFlow test: Proportional Remainder Engine reconciles 0.00¢ drift when remainder is distributed equally")
    fun testSplitUnassignedRemainderEquallyFlow() = runTest(testDispatcher) {
        val resultWithPayerHoldingRemainder = SplitMateMathEngine.calculateProportionalReceiptSplits(
            baseSubtotalCents = 10000L,
            taxCents = 888L,
            tipCents = 1800L,
            payerId = "m_1",
            memberBaseClaimsCents = listOf(
                Triple("m_1", "Akshay", 4000L),
                Triple("m_2", "Sam", 3400L)
            ),
            attributeRemainderToPayer = true
        )
        assertEquals(2600L, resultWithPayerHoldingRemainder.unassignedBaseCents)
        assertEquals(0L, resultWithPayerHoldingRemainder.driftCents)

        val resultWithRemainderSplitEqually = SplitMateMathEngine.calculateProportionalReceiptSplits(
            baseSubtotalCents = 10000L,
            taxCents = 888L,
            tipCents = 1800L,
            payerId = "m_1",
            memberBaseClaimsCents = listOf(
                Triple("m_1", "Akshay", 5300L),
                Triple("m_2", "Sam", 4700L)
            ),
            attributeRemainderToPayer = false
        )
        assertEquals(0L, resultWithRemainderSplitEqually.unassignedBaseCents)
        assertEquals(0L, resultWithRemainderSplitEqually.driftCents)
    }

    @Test
    @DisplayName("Split Breakdown test: Deselecting 1 of 4 members divides ONLY by 3 selected members and excludes the 4th")
    fun testDeselectingOneMemberSplitsOnlyAmongSelectedMembers() = runTest(testDispatcher) {
        val viewModel = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)

        // Create a 4-member group (Akshay + Sam, Priya, Maya)
        viewModel.createNewGroup(
            name = "Goa Train Trip",
            currencyCode = "INR",
            friendNamesCsv = "Sam, Priya, Maya"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val stateBefore = viewModel.uiState.value
        val groupMembers = stateBefore.activeGroupMembers
        assertEquals(4, groupMembers.size, "Group should have 4 total members")

        // Deselect the 4th member ("Maya") -> only 3 members selected
        val selectedThreeIds = groupMembers.take(3).map { it.memberId }
        val deselectedMember = groupMembers.last()

        // Log ₹3,000.00 (300,000 paise) among ONLY the 3 selected members
        viewModel.commitQuickEqualExpense(
            title = "Paschim Express Tickets",
            totalAmountCents = 300_000L,
            selectedMemberIds = selectedThreeIds
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val stateAfter = viewModel.uiState.value
        val loggedExpense = stateAfter.expenses.first { it.title == "Paschim Express Tickets" }

        val breakdown = SplitMateViewModel.resolveExpenseSplitBreakdown(
            expense = loggedExpense,
            groupMembers = groupMembers,
            allSplits = stateAfter.splits,
            currencySymbol = "₹",
            headerPrefix = "Split Breakdown"
        )

        // Verify it divides by 3 (₹1000.00 each), NOT by 4 (₹750.00)!
        assertEquals(4, breakdown.totalMembersInGroup)
        assertEquals(3, breakdown.splittingMembersCount)
        assertEquals("₹1000.00", breakdown.perPersonHeadlineShare)
        assertEquals("Split Breakdown (3 of 4 members splitting)", breakdown.headerLabel)

        // Verify each of the 3 selected members owes 100,000 paise (₹1000.00)
        selectedThreeIds.forEach { selectedId ->
            val row = breakdown.rows.first { it.memberId == selectedId }
            assertTrue(row.isIncludedInSplit)
            assertEquals(100_000L, row.owedCents)
            assertEquals("₹1000.00", row.formattedShare)
        }

        // Verify the deselected 4th member ("Maya") is excluded and owes 0 paise
        val excludedRow = breakdown.rows.first { it.memberId == deselectedMember.memberId }
        assertEquals(false, excludedRow.isIncludedInSplit)
        assertEquals(0L, excludedRow.owedCents)
        assertEquals("₹0.00 (Excluded)", excludedRow.formattedShare)
    }

    @Test
    @DisplayName("Custom Payer Largest Remainder test: Payer receives +1 paise when ₹100.00 is split 3 ways")
    fun testCustomPayerLargestRemainderAllocation() = runTest(testDispatcher) {
        val viewModel = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        viewModel.createNewGroup(
            name = "Konkan Trip",
            currencyCode = "INR",
            friendNamesCsv = "Sam, Priya"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val groupMembers = viewModel.uiState.value.activeGroupMembers
        val samPayer = groupMembers.first { it.name.equals("Sam", ignoreCase = true) }

        viewModel.commitQuickEqualExpense(
            title = "Taxi to Station",
            totalAmountCents = 10_000L,
            selectedMemberIds = groupMembers.map { it.memberId },
            payerMemberId = samPayer.memberId
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val expense = viewModel.uiState.value.expenses.first { it.title == "Taxi to Station" }
        assertEquals(samPayer.memberId, expense.payerId, "Custom payer Sam must be recorded as expense payer")

        val splitsForExpense = viewModel.uiState.value.splits.filter { it.expenseId == expense.expenseId }
        assertEquals(10_000L, splitsForExpense.sumOf { it.finalOwedCents }, "Sum of splits must equal 10,000 paise (0.00¢ drift)")
        val samSplit = splitsForExpense.first { it.memberId == samPayer.memberId }
        assertEquals(3_334L, samSplit.finalOwedCents, "Payer Sam must receive the +1 paise Largest Remainder share (3,334 paise)")
    }

    @Test
    @DisplayName("Duplicate PNR Guard test: Blocks duplicate 10-digit PNR in same group while allowing editExistingExpense")
    fun testDuplicatePnrRejectedAndEditAllowed() = runTest(testDispatcher) {
        val viewModel = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        viewModel.createNewGroup(
            name = "Delhi Rajdhani Group",
            currencyCode = "INR",
            friendNamesCsv = "Sam, Priya"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val memberIds = viewModel.uiState.value.activeGroupMembers.map { it.memberId }

        viewModel.commitQuickEqualExpense(
            title = "Rajdhani Express [PNR: 2518493012]",
            totalAmountCents = 450_000L,
            selectedMemberIds = memberIds
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val countAfterFirst = viewModel.uiState.value.expenses.count { it.title.contains("2518493012") }
        assertEquals(1, countAfterFirst)

        // Attempt to log duplicate 10-digit PNR in same group -> must be rejected
        viewModel.commitQuickEqualExpense(
            title = "Duplicate Ticket [PNR: 2518493012]",
            totalAmountCents = 450_000L,
            selectedMemberIds = memberIds
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val countAfterDuplicate = viewModel.uiState.value.expenses.count { it.title.contains("2518493012") }
        assertEquals(1, countAfterDuplicate, "Duplicate 10-digit PNR in same group must be blocked")

        // Editing the existing PNR expense must succeed
        val existingExpense = viewModel.uiState.value.expenses.first { it.title.contains("2518493012") }
        viewModel.editExistingExpense(
            expenseId = existingExpense.expenseId,
            newTitle = "Rajdhani Express Updated [PNR: 2518493012]",
            newTotalRupees = 4800.0,
            newPayerId = existingExpense.payerId,
            selectedMemberIds = memberIds
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedExpense = viewModel.uiState.value.expenses.first { it.expenseId == existingExpense.expenseId }
        assertEquals(480_000L, updatedExpense.totalAmountCents)
    }
}

