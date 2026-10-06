package com.splitmate.app

import com.splitmate.app.data.CloudGroupLedgerDocument
import com.splitmate.app.data.CloudGroupSyncRepository
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseGroupEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.AvatarGender
import com.splitmate.app.ui.AvatarSeedCodec
import com.splitmate.app.ui.SplitMateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * v2.3.5 issue #7 (Edit members not saved) and issue #4 (edit / delete expenses from Trip Hub 2.0).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplitMateV235EditMembersAndExpensePermissionTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------------------------------------------------------------------------------------------
    // Issue #7: Edit members
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("#7 RC2: gender pills use stored ids, so a saved Female member re-opens with F selected")
    fun genderPillsMatchStoredSeedIds() {
        val seed = "Gaurii|Female|open-peeps|PastelWall"
        val initial = MemberProfileEditRules.initialGenderIdOf(seed, "Gaurii")
        assertEquals(AvatarGender.FEMALE.id, initial)

        val selectedPills = MemberProfileEditRules.GENDER_PILLS.filter { (id, _) ->
            MemberProfileEditRules.isGenderSelected(initial, id)
        }
        assertEquals(listOf("F"), selectedPills.map { it.second }, "Exactly the F pill must be selected")

        // Legacy labels still normalise.
        assertTrue(MemberProfileEditRules.isGenderSelected("Feminine", AvatarGender.FEMALE.id))
        assertTrue(MemberProfileEditRules.isGenderSelected("Masculine", AvatarGender.MALE.id))
        assertFalse(MemberProfileEditRules.isGenderSelected("Female", AvatarGender.MALE.id))
    }

    @Test
    @DisplayName("#7: avatar seed keeps style + preset, swaps gender, and keeps the seed key when name is unchanged")
    fun avatarSeedPreservesStyleAndPreset() {
        val existing = "Gaurii|Female|open-peeps|PastelWall"
        val updated = MemberProfileEditRules.resolveMemberAvatarSeed(existing, "Gaurii", "Gaurii", AvatarGender.MALE.id)
        val before = AvatarSeedCodec.parse(existing)
        val after = AvatarSeedCodec.parse(updated)
        assertEquals(AvatarGender.MALE, after.gender)
        assertEquals(before.styleId, after.styleId)
        assertEquals(before.colorPresetId, after.colorPresetId)
        assertEquals(before.seedKey, after.seedKey)

        val renamed = AvatarSeedCodec.parse(
            MemberProfileEditRules.resolveMemberAvatarSeed(existing, "Gaurii", "Gauri K", AvatarGender.FEMALE.id)
        )
        assertEquals("Gauri K", renamed.seedKey)
        assertEquals(AvatarGender.FEMALE, renamed.gender)
    }

    @Test
    @DisplayName("#7: a real VPA is never clobbered by '<phone>@upi'; derived handles are refreshed")
    fun resolveUpiIdPreservesRealVpa() {
        // Real VPA + phone edit -> VPA kept.
        assertEquals("rohan@okaxis", MemberProfileEditRules.resolveUpiId("rohan@okaxis", "9123456789"))
        // Blank existing -> derive.
        assertEquals("9123456789@upi", MemberProfileEditRules.resolveUpiId("", "9123456789"))
        // Previously derived handle follows the new phone.
        assertEquals("9988776655@upi", MemberProfileEditRules.resolveUpiId("9123456789@upi", "9988776655"))
        // Explicit VPA field wins.
        assertEquals("rohan@ybl", MemberProfileEditRules.resolveUpiId("rohan@okaxis", "9123456789", "rohan@ybl"))
        // User cleared the VPA field -> fall back to derived phone handle.
        assertEquals("9123456789@upi", MemberProfileEditRules.resolveUpiId("rohan@okaxis", "9123456789", ""))
        // Legacy: VPA typed into the phone field is still honoured.
        assertEquals("sam@oksbi", MemberProfileEditRules.resolveUpiId("", "sam@oksbi"))

        assertTrue(MemberProfileEditRules.isDerivedPhoneUpi("9123456789@upi"))
        assertFalse(MemberProfileEditRules.isRealVpa("9123456789@upi"))
        assertEquals("", MemberProfileEditRules.editableVpaOf("9123456789@upi"))
        assertEquals("rohan@okaxis", MemberProfileEditRules.editableVpaOf("rohan@okaxis"))
    }

    @Test
    @DisplayName("#7 VM: updateAllGroupMembers saves name + gender, keeps a real VPA, and only touches the target member")
    fun updateAllGroupMembersPersistsEditsWithoutClobbering() = runTest(testDispatcher) {
        val viewModel = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        viewModel.createNewGroup(name = "Goa Crew", currencyCode = "INR", friendNamesCsv = "Rohan, Gaurii")
        testDispatcher.scheduler.advanceUntilIdle()

        val members = viewModel.uiState.value.activeGroupMembers
        val rohan = members.first { it.name.equals("Rohan", ignoreCase = true) }
        val gaurii = members.first { it.name.equals("Gaurii", ignoreCase = true) }
        val untouched = members.first { it.memberId != rohan.memberId && it.memberId != gaurii.memberId }

        // Rohan first gets a real VPA.
        viewModel.updateAllGroupMembers(
            listOf(
                SplitMateViewModel.BatchMemberUpdate(
                    memberId = rohan.memberId,
                    name = rohan.name,
                    upiId = "9123456789",
                    avatarSeed = rohan.avatarSeed,
                    vpaOverride = "rohan@okaxis"
                )
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("rohan@okaxis", viewModel.uiState.value.members.first { it.memberId == rohan.memberId }.upiId)

        // Second save edits only phone + gender (VPA field untouched) -> VPA must survive.
        val rohanNow = viewModel.uiState.value.members.first { it.memberId == rohan.memberId }
        val gauriiSeed = MemberProfileEditRules.resolveMemberAvatarSeed(
            gaurii.avatarSeed, gaurii.name, gaurii.name, AvatarGender.FEMALE.id
        )
        viewModel.updateAllGroupMembers(
            listOf(
                SplitMateViewModel.BatchMemberUpdate(
                    memberId = rohanNow.memberId,
                    name = "Rohan M",
                    upiId = "9988776655",
                    avatarSeed = MemberProfileEditRules.resolveMemberAvatarSeed(
                        rohanNow.avatarSeed, rohanNow.name, "Rohan M", AvatarGender.MALE.id
                    )
                ),
                SplitMateViewModel.BatchMemberUpdate(
                    memberId = gaurii.memberId,
                    name = gaurii.name,
                    upiId = "",
                    avatarSeed = gauriiSeed
                )
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val after = viewModel.uiState.value.members
        val rohanAfter = after.first { it.memberId == rohan.memberId }
        val gauriiAfter = after.first { it.memberId == gaurii.memberId }
        val untouchedAfter = after.first { it.memberId == untouched.memberId }

        assertEquals("rohan@okaxis", rohanAfter.upiId, "Real VPA must not be overwritten by <phone>@upi")
        assertEquals("9988776655", rohanAfter.userPhone)
        assertEquals("Rohan M", rohanAfter.name)
        assertEquals(AvatarGender.MALE, AvatarSeedCodec.parse(rohanAfter.avatarSeed).gender)

        // Re-opening the dialog derives the pill from the stored seed -> F for Gaurii.
        assertEquals(
            AvatarGender.FEMALE.id,
            MemberProfileEditRules.initialGenderIdOf(gauriiAfter.avatarSeed, gauriiAfter.name)
        )

        // RC3: the untouched member (e.g. member 1) keeps its name and seed.
        assertEquals(untouched.name, untouchedAfter.name)
        assertEquals(untouched.avatarSeed, untouchedAfter.avatarSeed)
    }

    // ---------------------------------------------------------------------------------------------
    // Issue #4: permission
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("#4: ExpenseEditPermission allows organizer, payer and creator only")
    fun permissionMatrix() {
        val me = "9123456789"
        // Organizer.
        assertTrue(ExpenseEditPermission.canModify(me, true, "9000000001", null))
        // Payer by phone.
        assertTrue(ExpenseEditPermission.canModify(me, false, "+91 91234 56789", null))
        // Payer by member row (no phone).
        assertTrue(ExpenseEditPermission.canModify("", false, null, null, isCurrentUserPayer = true))
        // Creator.
        assertTrue(ExpenseEditPermission.canModify(me, false, "9000000001", "9123456789"))
        // Anyone else.
        assertFalse(ExpenseEditPermission.canModify(me, false, "9000000001", "9000000002"))
        // Legacy row without creator stamp -> organizer + payer only.
        assertFalse(ExpenseEditPermission.canModify(me, false, "9000000001", null))
        // No phone at all -> no phone-based match.
        assertFalse(ExpenseEditPermission.canModify("", false, "", ""))

        assertEquals("9123456789", ExpenseEditPermission.creatorStampOf("+91-9123456789"))
        assertNull(ExpenseEditPermission.creatorStampOf(""))
    }

    @Test
    @DisplayName("#4 VM: canCurrentUserModifyExpense gates non-payer / non-creator / non-organizer members")
    fun viewModelPermissionUsesPayerCreatorOrganizer() = runTest(testDispatcher) {
        val viewModel = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        val gId = "g_v235_perm"
        // First member is the primary organizer (no "_me" / "m_1" ids, no stored organizer roles).
        val organizer = member("m_org", gId, "Organizer", phone = "9000000001", isCurrentUser = false)
        val you = member("m_you", gId, "You", phone = "9123456789", isCurrentUser = true)
        val payer = member("m_payer", gId, "Payer", phone = "9000000003", isCurrentUser = false)
        val expByPayer = expense("exp_perm_1", gId, payerId = payer.memberId, total = 90_000L, createdByPhone = null)
        val base = viewModel.uiState.value.copy(
            userPhone = "",
            activeGroupId = gId,
            members = listOf(organizer, you, payer),
            expenses = listOf(expByPayer),
            splits = equalSplits(expByPayer, listOf(organizer, you, payer))
        )

        assertFalse(viewModel.canCurrentUserModifyExpense(expByPayer, base), "Plain member must be read-only")
        assertTrue(
            viewModel.canCurrentUserModifyExpense(expByPayer.copy(createdByPhone = "9123456789"), base),
            "Creator may edit"
        )
        assertTrue(
            viewModel.canCurrentUserModifyExpense(expByPayer.copy(payerId = you.memberId), base),
            "Payer may edit"
        )

        val organizerPerspective = base.copy(
            members = listOf(organizer.copy(isCurrentUser = true), you.copy(isCurrentUser = false), payer)
        )
        assertTrue(viewModel.canCurrentUserModifyExpense(expByPayer, organizerPerspective), "Organizer may edit")
    }

    @Test
    @DisplayName("#4 VM: new expenses are stamped with createdByPhone and the creator (organizer) can still edit")
    fun commitQuickEqualExpenseStampsCreator() = runTest(testDispatcher) {
        val viewModel = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        viewModel.createNewGroup(name = "Pune Weekend", currencyCode = "INR", friendNamesCsv = "Sam, Priya")
        testDispatcher.scheduler.advanceUntilIdle()

        val members = viewModel.uiState.value.activeGroupMembers
        viewModel.commitQuickEqualExpense(
            title = "Misal Breakfast",
            totalAmountCents = 30_000L,
            selectedMemberIds = members.map { it.memberId }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val exp = viewModel.uiState.value.expenses.first { it.title == "Misal Breakfast" }
        val expectedStamp = ExpenseEditPermission.creatorStampOf(viewModel.currentUserPhone10(exp.groupId))
        assertEquals(expectedStamp, exp.createdByPhone)
        assertTrue(viewModel.canCurrentUserModifyExpense(exp))
    }

    // ---------------------------------------------------------------------------------------------
    // Issue #4: itemized guard
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("#4 guard a: itemized receipts lock amount + participants, title/payer edits allowed")
    fun itemizedGuard() {
        val members = listOf(
            member("m_a", "g", "A", "9000000001", true),
            member("m_b", "g", "B", "9000000002", false)
        )
        val equal = expense("exp_eq", "g", "m_a", 10_001L, null)
        val equalSplits = listOf(
            ExpenseSplitEntity("s1", "exp_eq", "m_a", 5_001L, 5_001L, false),
            ExpenseSplitEntity("s2", "exp_eq", "m_b", 5_000L, 5_000L, false)
        )
        assertFalse(ItemizedExpenseGuard.isItemized(equal, equalSplits), "1-paisa Largest Remainder diff is still equal")

        val receipt = expense("exp_rcpt", "g", "m_a", 12_688L, null).copy(
            baseSubtotalCents = 10_000L,
            taxCents = 888L,
            tipCents = 1_800L,
            lockedMultiplier = 1.2688
        )
        val receiptSplits = listOf(
            ExpenseSplitEntity("r1", "exp_rcpt", "m_a", 6_000L, 7_613L, false),
            ExpenseSplitEntity("r2", "exp_rcpt", "m_b", 4_000L, 5_075L, false)
        )
        assertTrue(ItemizedExpenseGuard.isItemized(receipt, receiptSplits))
        assertTrue(ItemizedExpenseGuard.isBlockedEdit(receipt, receiptSplits, 13_000L, members.map { it.memberId }))
        assertTrue(ItemizedExpenseGuard.isBlockedEdit(receipt, receiptSplits, 12_688L, listOf("m_a")))
        assertFalse(ItemizedExpenseGuard.isBlockedEdit(receipt, receiptSplits, 12_688L, members.map { it.memberId }))
        assertFalse(ItemizedExpenseGuard.isBlockedEdit(equal, equalSplits, 20_000L, listOf("m_a")))
    }

    // ---------------------------------------------------------------------------------------------
    // Issue #4: sync merge (guard c)
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("#4 guard c: pruneLosingSideSplits keeps only the winning side's rows for shared expenses")
    fun pruneLosingSideSplitsRule() {
        val local = listOf(
            ExpenseSplitEntity("l1", "exp_1", "m_a", 45_000L, 45_000L, false),
            ExpenseSplitEntity("l2", "exp_1", "m_b", 45_000L, 45_000L, false),
            ExpenseSplitEntity("l3", "exp_local_only", "m_a", 1_000L, 1_000L, false)
        )
        val remote = listOf(
            ExpenseSplitEntity("r1", "exp_1", "m_a", 30_000L, 30_000L, false),
            ExpenseSplitEntity("r2", "exp_1", "m_b", 30_000L, 30_000L, false),
            ExpenseSplitEntity("r3", "exp_1", "m_c", 30_000L, 30_000L, false),
            ExpenseSplitEntity("r4", "exp_remote_only", "m_c", 2_000L, 2_000L, false)
        )
        val (keptLocal, keptRemote) = ExpenseSplitMergeRules.pruneLosingSideSplits(
            local, remote, setOf("exp_1", "exp_local_only"), setOf("exp_1", "exp_remote_only"), localWins = true
        )
        assertEquals(setOf("l1", "l2", "l3"), keptLocal.map { it.splitId }.toSet())
        assertEquals(setOf("r4"), keptRemote.map { it.splitId }.toSet())

        val (keptLocal2, keptRemote2) = ExpenseSplitMergeRules.pruneLosingSideSplits(
            local, remote, setOf("exp_1", "exp_local_only"), setOf("exp_1", "exp_remote_only"), localWins = false
        )
        assertEquals(setOf("l3"), keptLocal2.map { it.splitId }.toSet())
        assertEquals(setOf("r1", "r2", "r3", "r4"), keptRemote2.map { it.splitId }.toSet())
    }

    @Test
    @DisplayName("#4 guard c: merge round-trip never resurrects a removed member's split; splits sum to the total")
    fun mergeDoesNotResurrectRemovedMemberSplit() {
        val gId = "g_v235_merge"
        val group = ExpenseGroupEntity(
            groupId = gId,
            name = "Manali",
            currencyCode = "INR",
            iconName = "Flight",
            isDemoSeed = false,
            createdAt = 1760000000000L
        )
        val a = member("m_a", gId, "Akshay", "9876543210", true)
        val b = member("m_b", gId, "Priya", "9123456780", false)
        val c = member("m_c", gId, "Rohan", "9123456789", false)
        val members = listOf(a, b, c)
        val original = expense("exp_dinner", gId, a.memberId, 90_000L, "9876543210")
        val remoteSplits = equalSplits(original, members) // a, b, c = 30,000 each
        // Local edit removed Rohan: a, b = 45,000 each.
        val localSplits = listOf(
            ExpenseSplitEntity("sp_exp_dinner_m_a", original.expenseId, a.memberId, 45_000L, 45_000L, false),
            ExpenseSplitEntity("sp_exp_dinner_m_b", original.expenseId, b.memberId, 45_000L, 45_000L, false)
        )

        val remoteDoc = CloudGroupLedgerDocument(
            group = group,
            members = members,
            expenses = listOf(original),
            splits = remoteSplits,
            settlements = emptyList(),
            deletedExpenseIds = emptyMap(),
            flightVaultByPnr = emptyMap(),
            trainSnapshotByPnr = emptyMap(),
            updatedAtEpochMs = 1760000500000L
        )
        val localDoc = remoteDoc.copy(splits = localSplits, updatedAtEpochMs = 1760000900000L)

        val merged = CloudGroupSyncRepository.mergeGroupLedgerDocuments(
            localDoc = localDoc,
            remoteDoc = remoteDoc,
            localUserPhone10 = "9876543210"
        )
        val mergedForExpense = merged.splits.filter { it.expenseId == original.expenseId }
        assertTrue(mergedForExpense.none { it.memberId == c.memberId }, "Removed member's split must not resurrect")
        assertEquals(90_000L, mergedForExpense.sumOf { it.finalOwedCents }, "Splits must sum to the expense total")

        // Opposite direction: the remote edit is newer -> remote rows win completely.
        val newerRemote = remoteDoc.copy(splits = localSplits, updatedAtEpochMs = 1760001000000L)
        val olderLocal = remoteDoc.copy(splits = remoteSplits, updatedAtEpochMs = 1760000100000L)
        val merged2 = CloudGroupSyncRepository.mergeGroupLedgerDocuments(
            localDoc = olderLocal,
            remoteDoc = newerRemote,
            localUserPhone10 = "9876543210"
        )
        val merged2ForExpense = merged2.splits.filter { it.expenseId == original.expenseId }
        assertTrue(merged2ForExpense.none { it.memberId == c.memberId })
        assertEquals(90_000L, merged2ForExpense.sumOf { it.finalOwedCents })
    }

    // ---------------------------------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------------------------------

    private fun member(
        id: String,
        groupId: String,
        name: String,
        phone: String,
        isCurrentUser: Boolean
    ) = GroupMemberEntity(
        memberId = id,
        groupId = groupId,
        name = name,
        avatarSeed = "$name|Neutral|open-peeps|Buckwheat",
        isCurrentUser = isCurrentUser,
        upiId = "$phone@upi",
        userPhone = phone,
        inviteStatus = "JOINED"
    )

    private fun expense(
        id: String,
        groupId: String,
        payerId: String,
        total: Long,
        createdByPhone: String?
    ) = ExpenseEntity(
        expenseId = id,
        groupId = groupId,
        title = "Dinner",
        payerId = payerId,
        baseSubtotalCents = total,
        taxCents = 0L,
        tipCents = 0L,
        totalAmountCents = total,
        lockedMultiplier = 1.0,
        unassignedBaseCents = 0L,
        currencyCode = "INR",
        lockedExchangeRate = 1.0,
        syncStatus = "SYNCED",
        createdAt = 1760000000000L,
        createdByPhone = createdByPhone
    )

    private fun equalSplits(exp: ExpenseEntity, members: List<GroupMemberEntity>): List<ExpenseSplitEntity> {
        val share = exp.totalAmountCents / members.size
        var leftover = exp.totalAmountCents - share * members.size
        return members.map { m ->
            val extra = if (leftover > 0) 1L.also { leftover-- } else 0L
            ExpenseSplitEntity("sp_${exp.expenseId}_${m.memberId}", exp.expenseId, m.memberId, share + extra, share + extra, false)
        }
    }
}
