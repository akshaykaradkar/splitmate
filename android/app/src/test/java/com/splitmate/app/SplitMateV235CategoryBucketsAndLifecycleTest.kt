package com.splitmate.app

import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.TripLifecycleRecord
import com.splitmate.app.data.TripLifecycleResolver
import com.splitmate.app.data.TripLifecycleState
import com.splitmate.app.ui.category.CustomExpenseCategoryStore
import com.splitmate.app.ui.category.ExpenseBucketResolver
import com.splitmate.app.ui.category.ExpenseCategoryCatalog
import com.splitmate.app.ui.category.ExpenseCategoryRefs
import com.splitmate.app.ui.category.SpendBucket
import com.splitmate.app.ui.screens.TripHubBookingCategory
import com.splitmate.app.ui.screens.classifyGroupExpenseForTripHub
import com.splitmate.app.ui.screens.computeTripTopCategories
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * v2.3.5 (#5 + #1) integration checks written by the release lead:
 *  - real-world trip titles (from the user's Ratnagiri trip) no longer collapse into "Other",
 *  - every legacy structured category / ticket card keeps its old archetype,
 *  - categoryRef + group-shared custom categories bucket correctly,
 *  - lifecycle suggestion never auto-ends a trip.
 */
class SplitMateV235CategoryBucketsAndLifecycleTest {

    private fun exp(title: String, category: String = "OTHER", ref: String? = null, cents: Long = 10_000L) = ExpenseEntity(
        expenseId = "e_${title.hashCode()}_$category",
        groupId = "g",
        title = title,
        payerId = "m1",
        baseSubtotalCents = cents,
        taxCents = 0L,
        tipCents = 0L,
        totalAmountCents = cents,
        lockedMultiplier = 1.0,
        unassignedBaseCents = 0L,
        currencyCode = "INR",
        lockedExchangeRate = 1.0,
        expenseCategory = category,
        categoryRef = ref
    )

    @Test
    @DisplayName("L01 Ratnagiri titles: food / fuel / sightseeing no longer land in Other")
    fun realTripTitlesBucketed() {
        val food = listOf("Dinner & Food", "Dinner", "Lunch", "Breakfast", "Party & Drinks", "Water & Beverages")
        food.forEach { assertEquals(TripHubBookingCategory.FOOD, classifyGroupExpenseForTripHub(exp(it)), it) }
        listOf("Fuel & Petrol", "Fuel").forEach {
            assertEquals(TripHubBookingCategory.CAB, classifyGroupExpenseForTripHub(exp(it)), it)
        }
        listOf("Sightseeing", "Entry Tickets").forEach {
            assertEquals(TripHubBookingCategory.ACTIVITIES, classifyGroupExpenseForTripHub(exp(it)), it)
        }
    }

    @Test
    @DisplayName("L02 legacy structured categories and ticket cards keep their archetype")
    fun legacyArchetypesUnchanged() {
        assertEquals(TripHubBookingCategory.CAB, classifyGroupExpenseForTripHub(exp("Cab & Local", "CAB")))
        assertEquals(TripHubBookingCategory.CAB, classifyGroupExpenseForTripHub(exp("Auto Rickshaw", "CAB")))
        assertEquals(TripHubBookingCategory.RENTAL, classifyGroupExpenseForTripHub(exp("Bike Rentals", "RENTAL")))
        assertEquals(TripHubBookingCategory.STAY, classifyGroupExpenseForTripHub(exp("Hotel & Stay", "STAY")))
        val train = "Train 12052 Jan Shatabdi Express (2S) | PNR: 8252894179 | Status: CNF (Confirmed) | RN->CSMT | Dep: 2026-10-03 17:25 | Seats: Class 2S · 4 Pax"
        assertEquals(TripHubBookingCategory.TRAIN, classifyGroupExpenseForTripHub(exp(train, "TRAIN")))
        assertEquals(TripHubBookingCategory.TRAIN, classifyGroupExpenseForTripHub(exp(train)))
        // keyword fallbacks from v2.0 still win over the new food pass
        assertEquals(TripHubBookingCategory.STAY, classifyGroupExpenseForTripHub(exp("Hotel room dinner")))
        assertEquals(TripHubBookingCategory.GENERAL, classifyGroupExpenseForTripHub(exp("Misc")))
    }

    @Test
    @DisplayName("L03 categoryRef: custom category with parent bucket, built-in ref, unknown ref falls back")
    fun categoryRefRouting() {
        val paragliding = ExpenseCategoryCatalog.custom("Paragliding", "paragliding", parentBucket = SpendBucket.ACTIVITIES)
        val ref = ExpenseCategoryRefs.refFor(paragliding)!!
        assertTrue(ref.startsWith("custom:"))
        assertEquals(
            TripHubBookingCategory.ACTIVITIES,
            classifyGroupExpenseForTripHub(exp("Paragliding", ref = ref), listOf(paragliding))
        )
        // Same expense on a phone that hasn't synced the category yet: still not a crash, title fallback.
        assertEquals(TripHubBookingCategory.GENERAL, classifyGroupExpenseForTripHub(exp("Paragliding", ref = ref)))
        // A ticket ref never hijacks a real train card.
        val train = "Train 12051 Jan Shatabdi Express | PNR: 8252916692"
        assertEquals(TripHubBookingCategory.TRAIN, classifyGroupExpenseForTripHub(exp(train, ref = "builtin:Train Ticket")))
    }

    @Test
    @DisplayName("L04 refForTitle keeps legacy preset titles title-based (null ref)")
    fun refForTitleLegacy() {
        assertNull(ExpenseCategoryRefs.refForTitle("Dinner & Food", emptyList()))
        val custom = ExpenseCategoryCatalog.custom("Kayak Fun", "kayak")
        assertEquals(ExpenseCategoryRefs.custom(custom.customId!!), ExpenseCategoryRefs.refForTitle("Kayak Fun", listOf(custom)))
        assertNull(ExpenseCategoryRefs.refForTitle("some free text", emptyList()))
    }

    @Test
    @DisplayName("L05 suggestBucket for custom categories")
    fun suggestBucket() {
        assertEquals(SpendBucket.FOOD, ExpenseBucketResolver.suggestBucket("Chai & Snacks"))
        assertEquals(SpendBucket.LOCAL_TRANSPORT, ExpenseBucketResolver.suggestBucket("Scooty Petrol"))
        assertEquals(SpendBucket.STAY, ExpenseBucketResolver.suggestBucket("Beach Camp Tent"))
        assertEquals(SpendBucket.ACTIVITIES, ExpenseBucketResolver.suggestBucket("Scuba Diving"))
        assertEquals(SpendBucket.SHOPPING, ExpenseBucketResolver.suggestBucket("Souvenirs Gift"))
        assertEquals(SpendBucket.OTHER, ExpenseBucketResolver.suggestBucket("Xyz"))
        // Custom without explicit parent resolves through the suggestion, not "Other".
        val c = ExpenseCategoryCatalog.custom("Chai Stop", "custom_star")
        assertEquals(SpendBucket.FOOD, ExpenseBucketResolver.resolveBucket(c))
    }

    @Test
    @DisplayName("L06 merged view: shared trip categories win over local duplicates")
    fun mergedView() {
        val shared = ExpenseCategoryCatalog.custom("Paragliding", "paragliding", parentBucket = SpendBucket.ACTIVITIES, isGroupShared = true)
        val localDup = ExpenseCategoryCatalog.custom("paragliding", "custom_star")
        val localOnly = ExpenseCategoryCatalog.custom("Toll", "toll")
        val merged = CustomExpenseCategoryStore.mergeViews(listOf(shared), listOf(localDup, localOnly))
        assertEquals(listOf("Paragliding", "Toll"), merged.map { it.title })
        assertTrue(merged.first().isGroupShared)
    }

    @Test
    @DisplayName("L07 lifecycle: explicit end only; quiet trip is just a suggestion; organizer-only toggle")
    fun lifecycle() {
        val day = TripLifecycleResolver.ONE_DAY_MS
        val now = 100 * day
        assertEquals(TripLifecycleResolver.State.ACTIVE, TripLifecycleResolver.resolve(null, now, now - day))
        assertEquals(TripLifecycleResolver.State.ENDED_SUGGESTED, TripLifecycleResolver.resolve(null, now, now - 4 * day))
        assertEquals(TripLifecycleResolver.State.ACTIVE, TripLifecycleResolver.resolve(null, now, null))
        val ended = TripLifecycleRecord(state = TripLifecycleState.ENDED, endedAtEpochMs = now, updatedAtEpochMs = now)
        assertEquals(TripLifecycleResolver.State.ENDED, TripLifecycleResolver.resolve(ended, now, now))
        assertEquals(TripLifecycleResolver.State.ACTIVE, TripLifecycleResolver.resolve(ended.copy(state = TripLifecycleState.ACTIVE), now, now))
        assertTrue(TripLifecycleResolver.canToggle(true))
        assertFalse(TripLifecycleResolver.canToggle(false))
        assertEquals(3, TripLifecycleResolver.tripDays(now, now + 2 * day))
        assertEquals(1, TripLifecycleResolver.tripDays(null, null))
    }

    @Test
    @DisplayName("L08 wrap-up summary: top categories by spend, integer cents only")
    fun topCategories() {
        val rows = listOf(
            exp("Dinner", cents = 300_000L) to TripHubBookingCategory.FOOD,
            exp("Lunch", cents = 165_000L) to TripHubBookingCategory.FOOD,
            exp("Hotel & Stay", "STAY", cents = 798_000L) to TripHubBookingCategory.STAY,
            exp("Fuel", cents = 60_000L) to TripHubBookingCategory.CAB,
            exp("Misc", cents = 1_000L) to TripHubBookingCategory.GENERAL
        )
        val top = computeTripTopCategories(rows)
        assertEquals(3, top.size)
        assertEquals(TripHubBookingCategory.STAY.filterTitle to 798_000L, top[0])
        assertEquals(TripHubBookingCategory.FOOD.filterTitle to 465_000L, top[1])
    }
}
