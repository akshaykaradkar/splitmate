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

    private val utc = java.time.ZoneOffset.UTC

    @Test
    fun `trip phase - before, during and after`() {
        val day = 24 * hour
        val start = t0 + 2 * day
        val end = t0 + 4 * day
        assertEquals(TripHubLayoutRules.TripPhase.BEFORE, TripHubLayoutRules.phaseOf(false, listOf(start, end), t0, utc))
        assertEquals(TripHubLayoutRules.TripPhase.DURING, TripHubLayoutRules.phaseOf(false, listOf(start, end), t0 + 3 * day, utc))
        assertEquals(TripHubLayoutRules.TripPhase.AFTER, TripHubLayoutRules.phaseOf(false, listOf(start, end), t0 + 6 * day, utc))
        assertEquals(TripHubLayoutRules.TripPhase.AFTER, TripHubLayoutRules.phaseOf(true, listOf(start, end), t0, utc), "wrapped up")
        assertEquals(TripHubLayoutRules.TripPhase.BEFORE, TripHubLayoutRules.phaseOf(false, emptyList(), t0, utc))
    }

    @Test
    fun `during the trip Overview leads with today, after it only recent expenses`() {
        val now = t0
        val todayCab = exp("cab_today", t0 - 10, now + 2 * hour) to TripHubBookingCategory.CAB
        val todayTrain = exp("train_today", t0 - 20, now + 5 * hour) to TripHubBookingCategory.TRAIN
        val tomorrowFlight = exp("flight_tomorrow", t0 - 30, now + 26 * hour) to TripHubBookingCategory.FLIGHT
        val oldFood = exp("food_old", t0 - 1_000, now - 30 * hour) to TripHubBookingCategory.FOOD
        val all = listOf(todayCab, todayTrain, tomorrowFlight, oldFood)
        val schedule: (ExpenseEntity) -> Long = { it.scheduledAtEpochMs ?: it.createdAt }
        val during = TripHubLayoutRules.overviewItems(all, schedule, now, phase = TripHubLayoutRules.TripPhase.DURING, zone = utc)
        assertEquals(listOf("train_today", "cab_today"), during.take(2).map { it.first.expenseId }, "today's plan first, tickets first")
        assertEquals(4, during.size)
        assertEquals(during.size, during.map { it.first.expenseId }.toSet().size, "no duplicates")

        val after = TripHubLayoutRules.overviewItems(all, schedule, now, phase = TripHubLayoutRules.TripPhase.AFTER, zone = utc)
        assertEquals(listOf("cab_today", "train_today", "flight_tomorrow"), after.map { it.first.expenseId }, "newest first, no ticket lead")
    }
}
