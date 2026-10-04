package com.splitmate.app

import com.splitmate.app.data.CloudCustomCategory
import com.splitmate.app.data.CloudGroupLedgerDocument
import com.splitmate.app.data.CloudGroupSyncRepository
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseGroupEntity
import com.splitmate.app.data.GroupLedgerExtrasCodec
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.data.TripLifecycleRecord
import com.splitmate.app.data.TripLifecycleState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * v2.3.5 ledger extensions (#5 group categories, #1 trip lifecycle, categoryRef / createdByPhone).
 * JUnit 5 (the project runs on the JUnit Platform; JUnit 4 `org.junit.Test` classes are not executed).
 */
class SplitMateV235LedgerCategoriesLifecycleTest {

    private val gId = "g_v235_ledger"
    private val group = ExpenseGroupEntity(groupId = gId, name = "Ratnagiri", createdAt = 1_790_000_000_000L)
    private val member = GroupMemberEntity(
        memberId = "${gId}_me", groupId = gId, name = "Akshay", avatarSeed = "Akshay|Male|dylan|Sunrise",
        isCurrentUser = true, upiId = "9876543210@upi", userPhone = "9876543210"
    )

    private fun expense(id: String, ref: String? = null, creator: String? = null) = ExpenseEntity(
        expenseId = id, groupId = gId, title = "Lunch", payerId = member.memberId,
        baseSubtotalCents = 52_500L, taxCents = 0L, tipCents = 0L, totalAmountCents = 52_500L,
        lockedMultiplier = 1.0, unassignedBaseCents = 0L, currencyCode = "INR", lockedExchangeRate = 1.0,
        createdAt = 1_790_000_100_000L, categoryRef = ref, createdByPhone = creator
    )

    private fun doc(
        expenses: List<ExpenseEntity> = listOf(expense("e1")),
        categories: List<CloudCustomCategory> = emptyList(),
        lifecycle: TripLifecycleRecord? = null,
        updatedAt: Long = 1_790_000_200_000L
    ) = CloudGroupLedgerDocument(
        group = group, members = listOf(member), expenses = expenses, splits = emptyList(),
        settlements = emptyList(), updatedAtEpochMs = updatedAt,
        customCategories = categories, tripLifecycle = lifecycle
    )

    @Test
    @DisplayName("X01 legacy ledger: no new keys are written; decode gives null refs and no extras")
    fun legacyLedgerUnchanged() {
        val json = CloudGroupSyncRepository.encodeGroupLedgerDocument(doc())
        assertFalse(json.contains("customCategories"))
        assertFalse(json.contains("tripLifecycle"))
        assertFalse(json.contains("categoryRef"))
        assertFalse(json.contains("createdByPhone"))
        val back = CloudGroupSyncRepository.decodeGroupLedgerDocument(json)!!
        assertNull(back.expenses.single().categoryRef)
        assertNull(back.expenses.single().createdByPhone)
        assertTrue(back.customCategories.isEmpty())
        assertNull(back.tripLifecycle)
    }

    @Test
    @DisplayName("X02 categoryRef / createdByPhone / extras survive an encode-decode round trip")
    fun roundTrip() {
        val cat = CloudCustomCategory("c_abc", "Paragliding", "paragliding", "ACTIVITIES", "9876543210", 10L)
        val life = TripLifecycleRecord(TripLifecycleState.ENDED, 500L, "9876543210", null, null, 600L)
        val d = doc(listOf(expense("e1", "custom:c_abc", "9876543210")), listOf(cat), life)
        val back = CloudGroupSyncRepository.decodeGroupLedgerDocument(CloudGroupSyncRepository.encodeGroupLedgerDocument(d))!!
        assertEquals("custom:c_abc", back.expenses.single().categoryRef)
        assertEquals("9876543210", back.expenses.single().createdByPhone)
        assertEquals("Paragliding", back.customCategories.single().title)
        assertEquals("ACTIVITIES", back.customCategories.single().parentBucket)
        assertTrue(back.tripLifecycle!!.isEnded)
        assertEquals("9876543210", back.tripLifecycle!!.endedByPhone)
    }

    @Test
    @DisplayName("X03 structural hash: unchanged without extras, changes on wrap-up / new category (so it syncs)")
    fun structuralHash() {
        val base = CloudGroupSyncRepository.computeStructuralLedgerHash(doc())
        assertEquals(base, CloudGroupSyncRepository.computeStructuralLedgerHash(doc(categories = emptyList(), lifecycle = null)))
        val ended = TripLifecycleRecord(TripLifecycleState.ENDED, 1L, "9876543210", null, null, 2L)
        assertNotEquals(base, CloudGroupSyncRepository.computeStructuralLedgerHash(doc(lifecycle = ended)))
        val cat = CloudCustomCategory("c_1", "Toll", "toll", "LOCAL_TRANSPORT", "", 3L)
        assertNotEquals(base, CloudGroupSyncRepository.computeStructuralLedgerHash(doc(categories = listOf(cat))))
    }

    @Test
    @DisplayName("X04 customCategories merge: union by id, LWW, tombstone beats stale re-add")
    fun categoryMerge() {
        val a1 = CloudCustomCategory("1", "Food", "food", "FOOD", "", 100L)
        val a2 = CloudCustomCategory("1", "Food Edit", "food", "FOOD", "", 200L)
        val b = CloudCustomCategory("2", "Cab", "taxi", "LOCAL_TRANSPORT", "", 50L)
        val merged = GroupLedgerExtrasCodec.mergeCustomCategories(listOf(a1, b), listOf(a2))
        assertEquals(2, merged.size)
        assertEquals("Food Edit", merged.first { it.id == "1" }.title)

        val tomb = b.copy(deleted = true, updatedAtEpochMs = 300L)
        val staleReAdd = b.copy(updatedAtEpochMs = 60L)
        val afterDelete = GroupLedgerExtrasCodec.mergeCustomCategories(listOf(tomb), listOf(staleReAdd))
        assertTrue(afterDelete.first { it.id == "2" }.deleted)
    }

    @Test
    @DisplayName("X05 lifecycle merge: own LWW clock; a newer reopen beats an older wrap-up")
    fun lifecycleMerge() {
        val ended = TripLifecycleRecord(TripLifecycleState.ENDED, 100L, "9876543210", null, null, 100L)
        val reopened = ended.copy(state = TripLifecycleState.ACTIVE, endedAtEpochMs = 0L, updatedAtEpochMs = 200L)
        assertFalse(GroupLedgerExtrasCodec.mergeTripLifecycle(ended, reopened)!!.isEnded)
        assertTrue(GroupLedgerExtrasCodec.mergeTripLifecycle(ended, null)!!.isEnded)
        assertNull(GroupLedgerExtrasCodec.mergeTripLifecycle(null, null))
    }

    @Test
    @DisplayName("X06 ledger merge keeps extras from either side and categoryRef from an old-client edit")
    fun ledgerMergeKeepsExtras() {
        val cat = CloudCustomCategory("c_abc", "Paragliding", "paragliding", "ACTIVITIES", "", 10L)
        val life = TripLifecycleRecord(TripLifecycleState.ENDED, 500L, "9876543210", null, null, 600L)
        val newClient = doc(listOf(expense("e1", "custom:c_abc", "9876543210")), listOf(cat), life, updatedAt = 1_000L)
        // An old client re-pushed the ledger later and dropped every unknown key.
        val oldClient = doc(listOf(expense("e1")), updatedAt = 2_000L)
        val merged = CloudGroupSyncRepository.mergeGroupLedgerDocuments(newClient, oldClient, "9876543210")
        assertNotNull(merged.tripLifecycle)
        assertTrue(merged.tripLifecycle!!.isEnded)
        assertEquals(listOf("c_abc"), merged.customCategories.map { it.id })
        assertEquals("custom:c_abc", merged.expenses.single().categoryRef)
        assertEquals("9876543210", merged.expenses.single().createdByPhone)
        assertEquals(52_500L, merged.expenses.single().totalAmountCents)
    }
}
