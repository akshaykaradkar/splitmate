package com.splitmate.app

import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.ui.screens.TripHubBookingCategory
import com.splitmate.app.ui.screens.TripHubLayoutRules
import com.splitmate.app.ui.screens.TripHubSectionTab
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.4.0 Trip Hub step 1: one home for each thing. */
class SplitMateV240TripHubTest {

    private val t0 = 1_791_000_000_000L
    private val hour = 3_600_000L

    private fun exp(id: String, createdAt: Long, scheduled: Long? = null) = ExpenseEntity(
        expenseId = id, groupId = "g", title = id, payerId = "m1", baseSubtotalCents = 100, taxCents = 0, tipCents = 0,
        totalAmountCents = 100, lockedMultiplier = 1.0, unassignedBaseCents = 0, currencyCode = "INR", lockedExchangeRate = 1.0,
        createdAt = createdAt, scheduledAtEpochMs = scheduled
    )

    @Test
    fun `overview shows the next ticket then the three newest other expenses`() {
        val pastTrain = exp("train_past", t0, t0 - 48 * hour) to TripHubBookingCategory.TRAIN
        val nextFlight = exp("flight_next", t0 + 1, t0 + 5 * hour) to TripHubBookingCategory.FLIGHT
        val laterTrain = exp("train_later", t0 + 2, t0 + 50 * hour) to TripHubBookingCategory.TRAIN
        val food = (1..5).map { exp("food_$it", t0 + 10 + it) to TripHubBookingCategory.FOOD }
        val all = listOf(pastTrain, nextFlight, laterTrain) + food
        val items = TripHubLayoutRules.overviewItems(all, { it.scheduledAtEpochMs ?: it.createdAt }, nowMs = t0)
        assertEquals("flight_next", items.first().first.expenseId, "soonest upcoming ticket first")
        assertEquals(listOf("food_5", "food_4", "food_3"), items.drop(1).map { it.first.expenseId })
        assertEquals(4, items.size)
    }

    @Test
    fun `with every ticket in the past the latest one is shown, and a trip with no tickets shows recent only`() {
        val a = exp("t1", t0, t0 - 72 * hour) to TripHubBookingCategory.TRAIN
        val b = exp("t2", t0 + 1, t0 - 24 * hour) to TripHubBookingCategory.TRAIN
        val items = TripHubLayoutRules.overviewItems(listOf(a, b), { it.scheduledAtEpochMs ?: 0 }, nowMs = t0)
        assertEquals("t2", items.first().first.expenseId)
        assertEquals(2, items.size, "no duplicate of the ticket in the recent list")

        val onlyFood = listOf(exp("f", t0) to TripHubBookingCategory.FOOD)
        assertEquals(listOf("f"), TripHubLayoutRules.overviewItems(onlyFood, { 0 }, t0).map { it.first.expenseId })
        assertTrue(TripHubLayoutRules.overviewItems(emptyList(), { 0 }, t0).isEmpty())
    }

    @Test
    fun `the spend card shows on Money only and the chip speaks plainly`() {
        assertTrue(TripHubLayoutRules.showsSpendCard(TripHubSectionTab.MONEY))
        TripHubSectionTab.entries.filter { it != TripHubSectionTab.MONEY }.forEach {
            assertFalse(TripHubLayoutRules.showsSpendCard(it), "$it must show the balance chip instead")
        }
        assertEquals("You get back ₹28.33", TripHubLayoutRules.balanceChipLabel(2833, "₹28.33"))
        assertEquals("You owe ₹5.00", TripHubLayoutRules.balanceChipLabel(-500, "₹5.00"))
        assertEquals("All settled", TripHubLayoutRules.balanceChipLabel(0, "₹0.00"))
    }
}
