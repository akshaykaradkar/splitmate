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
}
