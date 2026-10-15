package com.splitmate.app

import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.components.ExpenseUiMappers
import com.splitmate.app.ui.components.MoneyExpenseItem
import com.splitmate.app.ui.formatIndianRupeesFromCents
import com.splitmate.app.ui.screens.TripHubBookingCategory
import com.splitmate.app.ui.theme.ExpenseCardKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * v2.4.0 rc11: a busy trip logs many meals. Categories with 2+ expenses (food, activities,
 * shopping & other) collapse into ONE batch card like the cabs batch, so the Money list never
 * turns into a wall of identical "Food & dining" cards. Totals and membership are preserved.
 */
class SplitMateV240Rc11CategoryBatchTest {

    private fun src(path: String) = File("src/main/java/com/splitmate/app/$path").readText()

    private val members = listOf(
        GroupMemberEntity("m1", "g", "Akshay", "Akshay", isCurrentUser = true),
        GroupMemberEntity("m2", "g", "Sam", "Sam")
    )

    private fun expense(id: String, title: String, cents: Long, category: String, payer: String = "m1", at: Long) = ExpenseEntity(
        expenseId = id, groupId = "g", title = title, payerId = payer,
        baseSubtotalCents = cents, taxCents = 0, tipCents = 0, totalAmountCents = cents,
        lockedMultiplier = 1.0, unassignedBaseCents = 0, currencyCode = "INR", lockedExchangeRate = 1.0,
        expenseCategory = category, createdAt = at
    )

    private val t0 = 1_790_000_000_000L
    private val day = 86_400_000L
    private val stay = expense("s1", "Zostel Goa 3 nights", 18_600_00, "STAY", at = t0)
    private val meals = listOf(
        expense("f1", "Breakfast Artjuna", 1_320_00, "FOOD", at = t0 + 1_000),
        expense("f2", "Dinner at Thalassa", 4_860_00, "FOOD", payer = "m2", at = t0 + 2_000),
        expense("f3", "Lunch Britto's", 2_100_00, "FOOD", at = t0 + day),
        expense("f4", "Fish thali", 900_00, "FOOD", at = t0 + day + 1_000),
        expense("f5", "Gelato", 300_00, "FOOD", at = t0 + 2 * day)
    )
    private val oneActivity = expense("a1", "Scuba Grande Island", 9_000_00, "ACTIVITIES", at = t0 + 3_000)
    private val shopping = listOf(
        expense("o1", "Cashew nuts", 800_00, "SHOPPING", at = t0 + 4_000),
        expense("o2", "Sunscreen", 450_00, "OTHER", at = t0 + day + 5_000)
    )
    private val classified = buildList {
        add(stay to TripHubBookingCategory.STAY)
        add(meals[0] to TripHubBookingCategory.FOOD)
        add(meals[1] to TripHubBookingCategory.FOOD)
        add(oneActivity to TripHubBookingCategory.ACTIVITIES)
        add(shopping[0] to TripHubBookingCategory.SHOPPING)
        add(meals[2] to TripHubBookingCategory.FOOD)
        add(meals[3] to TripHubBookingCategory.FOOD)
        add(shopping[1] to TripHubBookingCategory.GENERAL)
        add(meals[4] to TripHubBookingCategory.FOOD)
    }

    @Test
    fun `RC11_01 many meals become one food card placed at the first meal`() {
        val items = ExpenseUiMappers.buildMoneyExpenseItems(classified, members, emptyList())
        assertEquals(listOf("Stay", "Batch", "Activity", "Batch"), items.map { it::class.simpleName })
        val food = items[1] as MoneyExpenseItem.Batch
        assertEquals(ExpenseCardKind.FOOD, food.data.kind)
        assertEquals("Food & dining", food.data.title)
        assertEquals(5, food.data.count)
        assertEquals("5 meals · 3 days", food.data.subtitle)
        assertEquals("2 people", food.data.payerName)
        assertEquals(formatIndianRupeesFromCents(meals.sumOf { it.totalAmountCents }), food.data.totalAmountFormatted)
        assertEquals("f5", food.data.rows.first().expenseId, "newest meal first")
        assertEquals(meals.map { it.expenseId }.sorted(), food.data.rows.map { it.expenseId }.sorted())
        assertTrue(food.data.rows.any { it.destination == "Fish thali" }, "no meal prefix invented when the title has none")
    }

    @Test
    fun `RC11_02 a single expense keeps its rich card, shopping and other batch together`() {
        val items = ExpenseUiMappers.buildMoneyExpenseItems(classified, members, emptyList())
        assertTrue(items[2] is MoneyExpenseItem.Activity, "one activity keeps the full card")
        val other = items[3] as MoneyExpenseItem.Batch
        assertEquals(ExpenseCardKind.SUNDRIES, other.data.kind)
        assertEquals("Shopping & other", other.data.title)
        assertEquals("2 items · 2 days", other.data.subtitle)
    }

    @Test
    fun `RC11_03 every expense appears exactly once and the money adds up`() {
        val items = ExpenseUiMappers.buildMoneyExpenseItems(classified, members, emptyList())
        val ids = items.flatMap { item ->
            when (item) {
                is MoneyExpenseItem.Stay -> listOf(item.expense.expenseId)
                is MoneyExpenseItem.Transit -> item.expenses.map { it.expenseId }
                is MoneyExpenseItem.Food -> listOf(item.expense.expenseId)
                is MoneyExpenseItem.Activity -> listOf(item.expense.expenseId)
                is MoneyExpenseItem.Sundries -> listOf(item.expense.expenseId)
                is MoneyExpenseItem.Ticket -> listOf(item.expense.expenseId)
                is MoneyExpenseItem.Batch -> item.expenses.map { it.expenseId }
            }
        }
        assertEquals(classified.map { it.first.expenseId }.sorted(), ids.sorted())
        assertEquals(items.size, items.map { it.key }.toSet().size, "list keys stay unique")
    }

    @Test
    fun `RC11_04 Money tab renders batches with the shared batch layout`() {
        val host = src("ui/screens/TripHomeScreen.kt")
            .substringAfter("private fun MoneyCategoryExpenseItem(").substringBefore("private data class BerthCellDisplayModel(")
        assertTrue(host.contains("MoneyExpenseItem.Batch -> com.splitmate.app.ui.components.CategoryBatchCard("))
        assertTrue(host.contains("onRowClick = { row -> item.expenses.firstOrNull { it.expenseId == row.expenseId }?.let(openDetails) }"),
            "each row opens its own expense sheet")
        val cards = src("ui/components/ExpenseCategoryCards.kt")
        assertTrue(cards.contains("fun CategoryBatchCard(") && cards.contains("private fun BatchCard("))
        assertTrue(cards.substringAfter("fun CabsTransitBatchCard(").substringBefore("fun CategoryBatchCard(").contains("BatchCard("),
            "cabs and category batches share one layout")
    }

    @Test
    fun `RC11_05 when different people paid, the card says who paid what`() {
        val items = ExpenseUiMappers.buildMoneyExpenseItems(classified, members, emptyList())
        val food = (items[1] as MoneyExpenseItem.Batch).data
        assertTrue(food.multiplePayers)
        // Akshay paid 1,320 + 2,100 + 900 + 300 = 4,620; Sam paid 4,860 -> Sam first (biggest).
        assertEquals(
            "Sam ${formatIndianRupeesFromCents(4_860_00)} · Akshay ${formatIndianRupeesFromCents(4_620_00)}",
            food.payerSummary
        )
        assertEquals("Sam", food.rows.first { it.expenseId == "f2" }.payerName)
        assertEquals("Akshay", food.rows.first { it.expenseId == "f1" }.payerName)
        val other = (items[3] as MoneyExpenseItem.Batch).data
        assertEquals(false, other.multiplePayers)
        assertEquals("Paid by Akshay", other.payerSummary)
        assertTrue(other.rows.all { it.payerName == null }, "single payer: no repeated name on every row")
    }

    @Test
    fun `RC11_06 more than three payers are summarised`() {
        val four = listOf(
            GroupMemberEntity("m1", "g", "Akshay", "Akshay", isCurrentUser = true),
            GroupMemberEntity("m2", "g", "Sam", "Sam"),
            GroupMemberEntity("m3", "g", "Priya", "Priya"),
            GroupMemberEntity("m4", "g", "Maya", "Maya")
        )
        val rows = (1..4).map { expense("x$it", "Meal $it", it * 100_00L, "FOOD", payer = "m$it", at = t0 + it) }
        assertEquals(
            "Maya ${formatIndianRupeesFromCents(400_00)} · Priya ${formatIndianRupeesFromCents(300_00)} · Sam ${formatIndianRupeesFromCents(200_00)} · +1 more",
            ExpenseUiMappers.payerSummaryOf(rows, four)
        )
        val cards = src("ui/components/ExpenseCategoryCards.kt")
        assertTrue(cards.contains("PayerLine(text = payerSummary, payerName = payerName, multiplePayers = multiplePayers)"))
        assertTrue(cards.contains("text = \"Paid by \${hop.payerName}\""), "rows name the payer when mixed")
    }
}
