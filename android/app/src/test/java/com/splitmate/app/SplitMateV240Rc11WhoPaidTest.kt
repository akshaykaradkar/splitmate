package com.splitmate.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.components.CategoryBatchCard
import com.splitmate.app.ui.components.ExpenseUiMappers
import com.splitmate.app.ui.components.MoneyExpenseItem
import com.splitmate.app.ui.formatIndianRupeesFromCents
import com.splitmate.app.ui.screens.TripHubBookingCategory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * v2.4.0 rc11 follow-up: on a mixed-payer batch the "Sam ₹… · Akshay ₹…" line is a real button that
 * opens the "Who paid" sheet (each payer, subtotal and entries), and names are shown in title case
 * however they were typed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SplitMateV240Rc11WhoPaidTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // Names typed in lower / upper case on purpose.
    private val members = listOf(
        GroupMemberEntity("m1", "g", "akshay", "akshay", isCurrentUser = true),
        GroupMemberEntity("m2", "g", "SAM", "SAM"),
        GroupMemberEntity("m3", "g", "anne-marie mcKenzie", "anne")
    )

    private fun expense(id: String, title: String, cents: Long, payer: String, at: Long) = ExpenseEntity(
        expenseId = id, groupId = "g", title = title, payerId = payer,
        baseSubtotalCents = cents, taxCents = 0, tipCents = 0, totalAmountCents = cents,
        lockedMultiplier = 1.0, unassignedBaseCents = 0, currencyCode = "INR", lockedExchangeRate = 1.0,
        expenseCategory = "FOOD", createdAt = at
    )

    private val t0 = 1_790_000_000_000L
    private val meals = listOf(
        expense("f1", "Breakfast Artjuna", 1_320_00, "m1", t0 + 1),
        expense("f2", "Dinner at Thalassa", 4_860_00, "m2", t0 + 2),
        expense("f3", "Lunch Britto's", 2_100_00, "m1", t0 + 3)
    )

    private fun foodBatch() = (ExpenseUiMappers.buildMoneyExpenseItems(
        meals.map { it to TripHubBookingCategory.FOOD }, members, emptyList()
    ).single() as MoneyExpenseItem.Batch)

    @Test
    fun `RC11W_01 names are title case however they were typed`() {
        assertEquals("Akshay", ExpenseUiMappers.displayPersonName("akshay"))
        assertEquals("Sam", ExpenseUiMappers.displayPersonName("SAM"))
        assertEquals("Priya Shah", ExpenseUiMappers.displayPersonName("  priya   shah "))
        assertEquals("Anne-Marie McKenzie", ExpenseUiMappers.displayPersonName("anne-marie McKenzie"))
        val batch = foodBatch().data
        assertEquals(
            "Sam ${formatIndianRupeesFromCents(4_860_00)} · Akshay ${formatIndianRupeesFromCents(3_420_00)}",
            batch.payerSummary
        )
        assertTrue(batch.rows.mapNotNull { it.payerName }.all { it == "Sam" || it == "Akshay" })
        assertEquals(listOf("Sam", "Akshay"), batch.payers.map { it.name })
        assertEquals(listOf("f3", "f1"), batch.payers[1].expenseIds, "a payer's entries, newest first")
    }

    @Test
    fun `RC11W_02 tapping the payer summary opens Who paid, and an entry opens its expense`() {
        val batch = foodBatch().data
        var opened: String? = null
        compose.setContent { CategoryBatchCard(batch = batch, onRowClick = { opened = it.expenseId }) }
        compose.onNodeWithText(batch.payerSummary).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Who paid").assertIsDisplayed()
        compose.onNodeWithText("Paid for 2 meals").assertIsDisplayed()
        compose.onNodeWithText("Paid for 1 meal").assertIsDisplayed()
        // "Dinner at Thalassa" is in the card and in the sheet; the last match is the sheet's.
        compose.onAllNodesWithText("Dinner at Thalassa").let { it[it.fetchSemanticsNodes().lastIndex] }.performClick()
        compose.waitForIdle()
        assertEquals("f2", opened)
    }
}
