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

            // Enable offline mode
            viewModel.setOfflineMode(true)
            val offlineState = awaitItem()
            assertTrue(offlineState.isOfflineMode)

            // Commit a collaborative expense while offline
            viewModel.commitCollaborativeExpense(title = "Offline Mountain Dinner")
            val afterCommit = awaitItem()

            val newestExpense = afterCommit.expenses.first()
            assertEquals("Offline Mountain Dinner", newestExpense.title)
            assertEquals("PENDING", newestExpense.syncStatus, "Offline expense must be queued as PENDING")
            assertEquals(83.95, newestExpense.lockedExchangeRate, 0.001, "Exchange rate must be locked at creation time")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Turbine StateFlow test: Splitting unassigned remainder equally assigns all unclaimed receipt items")
    fun testSplitUnassignedRemainderEquallyFlow() = runTest(testDispatcher) {
        val viewModel = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.receiptItems.any { it.claimedByMemberIds.isEmpty() })

            viewModel.splitUnassignedRemainderEqually()
            val afterSplit = awaitItem()

            assertTrue(
                afterSplit.receiptItems.all { it.claimedByMemberIds.isNotEmpty() },
                "All receipt items must be assigned after Split Remainder Equally"
            )
            cancelAndIgnoreRemainingEvents()
        }
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
}

