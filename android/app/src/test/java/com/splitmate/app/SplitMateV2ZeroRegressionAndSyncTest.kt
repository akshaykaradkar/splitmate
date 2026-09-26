package com.splitmate.app

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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * SplitMate V2 Zero-Regression & $0.00-Server-Cost P2P Sync Test Suite (Phase 5: Tasks 5.1.1 & 5.1.2):
 * 1. `SM2_` GZIP+Base64url Sync Capsule export/import round-trip across separate devices.
 * 2. Idempotent double-merge and bidirectional CRDT-style union merge convergence.
 * 3. Multi-stage local perspective identity resolution (override, existing claim, 10-digit mobile, UPI VPA, name)
 *    and 1-tap `claimGroupMemberPerspective` switching.
 * 4. Multi-device `+1p` Largest-Remainder invariance across all 4 device perspectives (`0.00c drift`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplitMateV2ZeroRegressionAndSyncTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun containsUnicodeEmoji(text: String): Boolean {
        var idx = 0
        while (idx < text.length) {
            val codePoint = text.codePointAt(idx)
            if (codePoint in 0x1F300..0x1FAFF || codePoint in 0x2600..0x27BF) {
                return true
            }
            idx += Character.charCount(codePoint)
        }
        return false
    }

    @Test
    @DisplayName("1. Subtask 5.1.1: SM2_ GZIP+Base64url export/import round-trip across devices with zero emojis")
    fun testSm2ExportAndImportRoundTripAcrossDevices() = runTest(testDispatcher) {
        val phoneAViewModel = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        phoneAViewModel.completeOnboarding(
            name = "Akshay",
            countryName = "India",
            currencyCode = "INR",
            currencySymbol = "₹",
            avatarSeed = "Akshay"
        )
        phoneAViewModel.updateUpiId("9876543210@okaxis")
        testDispatcher.scheduler.advanceUntilIdle()

        phoneAViewModel.createNewGroup(
            name = "Hampi Expedition",
            currencyCode = "INR",
            friendNamesCsv = "Rohan, Sneha, Priya"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val groupId = phoneAViewModel.uiState.value.activeGroupId
        val membersA = phoneAViewModel.uiState.value.members.filter { it.groupId == groupId }
        assertEquals(4, membersA.size, "Hampi Expedition must have 4 members")

        val akshay = membersA.first { it.name.equals("Akshay", ignoreCase = true) }
        val rohan = membersA.first { it.name.equals("Rohan", ignoreCase = true) }
        val sneha = membersA.first { it.name.equals("Sneha", ignoreCase = true) }
        val priya = membersA.first { it.name.equals("Priya", ignoreCase = true) }

        // Assign distinct UPI IDs so 4-stage resolution can also be verified
        phoneAViewModel.updateFriendUpi(akshay.memberId, "Akshay", "9876543210@okaxis", "Akshay")
        phoneAViewModel.updateFriendUpi(rohan.memberId, "Rohan", "9123456789@ybl", "Rohan")
        phoneAViewModel.updateFriendUpi(sneha.memberId, "Sneha", "sneha.kulkarni@oksbi", "Sneha")
        phoneAViewModel.updateFriendUpi(priya.memberId, "Priya", "9988776655@icici", "Priya")
        testDispatcher.scheduler.advanceUntilIdle()

        val allMemberIds = membersA.map { it.memberId }

        // Log 3 bookings across different payers and uneven amounts
        phoneAViewModel.commitQuickEqualExpense(
            title = "IRCTC Hampi Express [PNR:8419203746]",
            totalAmountCents = 342_000L, // Rs 3,420.00
            selectedMemberIds = allMemberIds,
            payerMemberId = akshay.memberId
        )
        Thread.sleep(5)
        phoneAViewModel.commitQuickEqualExpense(
            title = "Boulders Resort Hampi Stay",
            totalAmountCents = 1_250_001L, // Rs 12,500.01 (uneven +1p)
            selectedMemberIds = allMemberIds,
            payerMemberId = rohan.memberId
        )
        Thread.sleep(5)
        phoneAViewModel.commitQuickEqualExpense(
            title = "Coracle Crossing & Scooter Rental",
            totalAmountCents = 100_000L, // Rs 1,000.00 across 3 members (uneven +1p)
            selectedMemberIds = listOf(akshay.memberId, rohan.memberId, sneha.memberId),
            payerMemberId = sneha.memberId
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Record one partial settlement from Priya to Rohan
        phoneAViewModel.markGreedyTransferSettled(
            SplitMateMathEngine.SimplifiedTransfer(
                fromMemberId = priya.memberId,
                fromName = priya.name,
                toMemberId = rohan.memberId,
                toName = rohan.name,
                amountCents = 150_000L
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val exportBundle = phoneAViewModel.exportGroupSyncPayload(groupId)
        assertNotNull(exportBundle, "Export bundle must not be null")
        val bundle = exportBundle!!

        assertTrue(bundle.syncToken.startsWith("SM2_"), "Sync token must start with SM2_")
        assertTrue(
            bundle.deepLinkUri.startsWith("splitmate://trip-sync?payload=SM2_"),
            "Deep link URI must use splitmate://trip-sync?payload=SM2_"
        )
        assertFalse(
            containsUnicodeEmoji(bundle.whatsappShareText),
            "WhatsApp share text must contain strictly zero Unicode emojis"
        )
        assertEquals(4, bundle.memberCount)
        assertEquals(3, bundle.expenseCount)
        assertEquals(1, bundle.settlementCount)
        assertTrue(bundle.compressedBytesSize in 100..2500, "Compressed GZIP capsule must be compact")

        // Verify token extraction from raw token, deep link URI, and full WhatsApp message
        assertEquals(bundle.syncToken, SplitMateViewModel.extractSyncTokenFromRawInput(bundle.syncToken))
        assertEquals(bundle.syncToken, SplitMateViewModel.extractSyncTokenFromRawInput(bundle.deepLinkUri))
        assertEquals(bundle.syncToken, SplitMateViewModel.extractSyncTokenFromRawInput(bundle.whatsappShareText))

        // Import into Phone B (owned by Rohan, matched by 10-digit Indian phone number)
        val phoneBViewModel = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        phoneBViewModel.completeOnboarding(
            name = "Rohan Sharma",
            countryName = "India",
            currencyCode = "INR",
            currencySymbol = "₹",
            avatarSeed = "Rohan"
        )
        phoneBViewModel.updateUpiId("+919123456789@okicici")
        testDispatcher.scheduler.advanceUntilIdle()

        val mergeResult = phoneBViewModel.importAndMergeGroupSyncPayload(
            rawPayloadOrMessage = bundle.whatsappShareText,
            openGroupAfterMerge = true
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(mergeResult.success, "Phone B import must succeed")
        assertEquals(groupId, mergeResult.groupId)
        assertEquals(4, mergeResult.mergedMemberCount)
        assertEquals(3, mergeResult.mergedExpenseCount)
        assertEquals(3, mergeResult.newlyAddedExpenseCount)
        assertEquals(1, mergeResult.newlyAddedSettlementCount)
        assertEquals(rohan.memberId, mergeResult.claimedMemberId, "Phone B must auto-resolve Rohan via 10-digit mobile match")

        val balancesA = phoneAViewModel.computeGroupMemberNetBalances(groupId)
        val balancesB = phoneBViewModel.computeGroupMemberNetBalances(groupId)
        assertEquals(balancesA, balancesB, "Phone A and Phone B must compute identical integer paise net balances")
        assertEquals(0L, balancesB.values.sum(), "Conservation of flow: sum of group net balances must equal 0L")
    }

    @Test
    @DisplayName("2. Subtask 5.1.1: Idempotent double-merge and bidirectional CRDT union merge convergence")
    fun testIdempotentDoubleMergeAndBidirectionalUnionMerge() = runTest(testDispatcher) {
        val phoneAViewModel = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        phoneAViewModel.createNewGroup(
            name = "Coorg Coffee Trail",
            currencyCode = "INR",
            friendNamesCsv = "Rohan, Sneha, Priya"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val groupId = phoneAViewModel.uiState.value.activeGroupId
        val membersA = phoneAViewModel.uiState.value.members.filter { it.groupId == groupId }
        val akshay = membersA.first { it.isCurrentUser }
        val rohan = membersA.first { it.name.equals("Rohan", ignoreCase = true) }

        phoneAViewModel.commitQuickEqualExpense(
            title = "Plantation Homestay Deposit",
            totalAmountCents = 800_000L,
            selectedMemberIds = membersA.map { it.memberId },
            payerMemberId = akshay.memberId
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val capsuleFromA = phoneAViewModel.exportGroupSyncPayload(groupId)!!

        val phoneBViewModel = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        val firstMerge = phoneBViewModel.importAndMergeGroupSyncPayload(
            rawPayloadOrMessage = capsuleFromA.syncToken,
            claimedMemberIdOverride = rohan.memberId
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(firstMerge.success)
        assertEquals(1, firstMerge.newlyAddedExpenseCount)

        // Import the exact same capsule a second time -> must be 100% idempotent with 0 duplicates
        val secondMerge = phoneBViewModel.importAndMergeGroupSyncPayload(
            rawPayloadOrMessage = capsuleFromA.whatsappShareText
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(secondMerge.success)
        assertEquals(0, secondMerge.newlyAddedExpenseCount, "Re-importing identical capsule must add 0 duplicate expenses")
        assertEquals(0, secondMerge.newlyAddedSettlementCount, "Re-importing identical capsule must add 0 duplicate settlements")
        assertEquals(
            4,
            phoneBViewModel.uiState.value.members.count { it.groupId == groupId },
            "Member count must remain 4 after idempotent re-import"
        )
        assertEquals(
            1,
            phoneBViewModel.uiState.value.expenses.count { it.groupId == groupId },
            "Expense count must remain 1 after idempotent re-import"
        )
        assertEquals(
            rohan.memberId,
            phoneBViewModel.uiState.value.members.first { it.groupId == groupId && it.isCurrentUser }.memberId,
            "Existing local perspective claim (Rohan) must be preserved on re-import"
        )

        // Now Phone B (Rohan) logs a new offline expense and exports an updated capsule back to Phone A
        Thread.sleep(5)
        phoneBViewModel.commitQuickEqualExpense(
            title = "Jeep Safari Dubare",
            totalAmountCents = 240_000L,
            selectedMemberIds = membersA.map { it.memberId },
            payerMemberId = rohan.memberId
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val capsuleFromB = phoneBViewModel.exportGroupSyncPayload(groupId)!!
        assertEquals(2, capsuleFromB.expenseCount)

        val mergeBackOnA = phoneAViewModel.importAndMergeGroupSyncPayload(
            rawPayloadOrMessage = capsuleFromB.syncToken
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(mergeBackOnA.success)
        assertEquals(1, mergeBackOnA.newlyAddedExpenseCount, "Phone A must merge the 1 new expense added by Phone B")
        assertEquals(
            akshay.memberId,
            phoneAViewModel.uiState.value.members.first { it.groupId == groupId && it.isCurrentUser }.memberId,
            "Phone A must preserve Akshay as its local perspective after merging Phone B's capsule"
        )

        val finalBalancesA = phoneAViewModel.computeGroupMemberNetBalances(groupId)
        val finalBalancesB = phoneBViewModel.computeGroupMemberNetBalances(groupId)
        assertEquals(finalBalancesA, finalBalancesB, "Bidirectional union merge must converge to identical net balances")
    }

    @Test
    @DisplayName("3. Subtask 5.1.1: Multi-stage local perspective resolution & 1-tap claimGroupMemberPerspective")
    fun testFourStageIdentityResolutionAndPerspectiveSwitching() = runTest(testDispatcher) {
        val hostVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        hostVm.completeOnboarding(
            name = "Akshay",
            countryName = "India",
            currencyCode = "INR",
            currencySymbol = "₹",
            avatarSeed = "Akshay"
        )
        hostVm.updateUpiId("9811122233@okaxis")
        hostVm.createNewGroup(
            name = "Udaipur Palace Trip",
            currencyCode = "INR",
            friendNamesCsv = "Rohan, Sneha, Priya"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val groupId = hostVm.uiState.value.activeGroupId
        val members = hostVm.uiState.value.members.filter { it.groupId == groupId }
        val akshay = members.first { it.name == "Akshay" }
        val rohan = members.first { it.name == "Rohan" }
        val sneha = members.first { it.name == "Sneha" }
        val priya = members.first { it.name == "Priya" }

        hostVm.updateFriendUpi(rohan.memberId, "Rohan", "9765432109@ybl", "Rohan")
        hostVm.updateFriendUpi(sneha.memberId, "Sneha", "sneha.pay@oksbi", "Sneha")
        hostVm.updateFriendUpi(priya.memberId, "Priya", "", "Priya")
        hostVm.commitQuickEqualExpense(
            title = "Lake Pichola Boat Ride",
            totalAmountCents = 100_001L,
            selectedMemberIds = members.map { it.memberId },
            payerMemberId = akshay.memberId
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val capsule = hostVm.exportGroupSyncPayload(groupId)!!

        // Stage 1: Explicit claimedMemberIdOverride
        val overrideVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        val resStage1 = overrideVm.importAndMergeGroupSyncPayload(
            rawPayloadOrMessage = capsule.syncToken,
            claimedMemberIdOverride = priya.memberId
        )
        assertEquals(priya.memberId, resStage1.claimedMemberId, "Stage 1 must honor claimedMemberIdOverride")

        // Stage 3: 10-digit Indian mobile number match (ignoring +91 prefix and different @handle)
        val phoneMatchVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        phoneMatchVm.completeOnboarding("Rohan K", "India", "INR", "₹", "Rohan")
        phoneMatchVm.updateUpiId("+919765432109@paytm")
        val resStage3 = phoneMatchVm.importAndMergeGroupSyncPayload(capsule.syncToken)
        assertEquals(rohan.memberId, resStage3.claimedMemberId, "Stage 3 must match 10-digit Indian mobile number")

        // Stage 4: Exact case-insensitive UPI VPA match
        val vpaMatchVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        vpaMatchVm.completeOnboarding("Traveler", "India", "INR", "₹", "Traveler")
        vpaMatchVm.updateUpiId("SNEHA.PAY@OKSBI")
        val resStage4 = vpaMatchVm.importAndMergeGroupSyncPayload(capsule.syncToken)
        assertEquals(sneha.memberId, resStage4.claimedMemberId, "Stage 4 must match case-insensitive UPI VPA")

        // Stage 5: Case-insensitive name match + collision-safe completeOnboarding after pre-onboarding import
        val nameMatchVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        nameMatchVm.importAndMergeGroupSyncPayload(capsule.syncToken)
        nameMatchVm.completeOnboarding("Priya", "India", "INR", "₹", "Priya")
        nameMatchVm.updateUpiId("priya@okaxis")
        testDispatcher.scheduler.advanceUntilIdle()
        val priyaAfterOnboarding = nameMatchVm.uiState.value.members
            .filter { it.groupId == groupId }
            .first { it.isCurrentUser }
        assertEquals(priya.memberId, priyaAfterOnboarding.memberId, "Post-import onboarding as Priya must claim existing Priya row without collision")
        assertEquals(4, nameMatchVm.uiState.value.members.count { it.groupId == groupId }, "Member count must remain 4")

        // Verify 1-tap perspective switching across all 4 members keeps net balances invariant
        val baselineBalances = hostVm.computeGroupMemberNetBalances(groupId)
        for (targetMember in listOf(akshay, rohan, sneha, priya)) {
            hostVm.claimGroupMemberPerspective(groupId, targetMember.memberId)
            testDispatcher.scheduler.advanceUntilIdle()

            val currentClaimed = hostVm.uiState.value.members
                .filter { it.groupId == groupId }
                .single { it.isCurrentUser }
            assertEquals(targetMember.memberId, currentClaimed.memberId)
            assertEquals(
                baselineBalances,
                hostVm.computeGroupMemberNetBalances(groupId),
                "Switching perspective to ${targetMember.name} must never alter group member net balances"
            )
        }
    }

    @Test
    @DisplayName("4. Subtask 5.1.2: Multi-device +1p Largest-Remainder invariance across all 4 perspectives (0.00c drift)")
    fun testMultiDevicePlusOnePennyLargestRemainderInvarianceAcrossAllFourPerspectives() {
        val members = listOf(
            "m_1" to "Akshay",
            "m_2" to "Rohan",
            "m_3" to "Sneha",
            "m_4" to "Priya"
        )
        val perspectiveIds = listOf("m_1", "m_2", "m_3", "m_4")

        // 4A. Verify Equal Splits with uneven totals across all 4 device perspectives
        val unevenCases = listOf(
            Triple(10_000L, "m_3", members.take(3)), // Rs 100.00 across 3 members, paid by Sneha (m_3)
            Triple(100_001L, "m_2", members),        // Rs 1,000.01 across 4 members, paid by Rohan (m_2)
            Triple(250_002L, "m_4", members)         // Rs 2,500.02 across 4 members, paid by Priya (m_4)
        )

        for ((totalCents, payerId, participants) in unevenCases) {
            val referenceSplits = SplitMateMathEngine.splitEquallyZeroDrift(
                totalCents = totalCents,
                members = participants,
                payerId = payerId,
                currentUserId = perspectiveIds.first()
            )
            assertEquals(totalCents, referenceSplits.sumOf { it.finalCents }, "Must have 0.00c drift")
            assertTrue(
                referenceSplits.first { it.memberId == payerId }.plusOneCent,
                "Payer $payerId must deterministically receive the first +1p remainder cent"
            )

            for (viewerId in perspectiveIds) {
                // Shuffle input order to simulate different device insertion orders while keeping payerId fixed
                val reorderedInput = participants.reversed()
                val viewerSplits = SplitMateMathEngine.splitEquallyZeroDrift(
                    totalCents = totalCents,
                    members = reorderedInput,
                    payerId = payerId,
                    currentUserId = viewerId
                ).associateBy { it.memberId }

                for (refAlloc in referenceSplits) {
                    val viewerAlloc = viewerSplits.getValue(refAlloc.memberId)
                    assertEquals(
                        refAlloc.finalCents,
                        viewerAlloc.finalCents,
                        "Member ${refAlloc.memberId} finalCents must be identical when viewed by $viewerId"
                    )
                    assertEquals(
                        refAlloc.plusOneCent,
                        viewerAlloc.plusOneCent,
                        "Member ${refAlloc.memberId} plusOneCent flag must be identical when viewed by $viewerId"
                    )
                }
            }
        }

        // 4B. Verify Proportional Receipt Splits across reversed member input orders
        val claims = listOf(
            Triple("m_1", "Akshay", 3_333L),
            Triple("m_2", "Rohan", 3_333L),
            Triple("m_3", "Sneha", 3_334L)
        )
        val referenceProp = SplitMateMathEngine.calculateProportionalReceiptSplits(
            baseSubtotalCents = 10_000L,
            taxCents = 500L,
            tipCents = 200L,
            payerId = "m_3",
            memberBaseClaimsCents = claims,
            attributeRemainderToPayer = true
        )
        assertEquals(0L, referenceProp.driftCents)
        assertEquals(10_700L, referenceProp.allocations.sumOf { it.finalCents })

        val reorderedProp = SplitMateMathEngine.calculateProportionalReceiptSplits(
            baseSubtotalCents = 10_000L,
            taxCents = 500L,
            tipCents = 200L,
            payerId = "m_3",
            memberBaseClaimsCents = claims.reversed(),
            attributeRemainderToPayer = true
        )
        assertEquals(0L, reorderedProp.driftCents)
        assertEquals(
            referenceProp.allocations.associate { it.memberId to it.finalCents },
            reorderedProp.allocations.associate { it.memberId to it.finalCents },
            "Proportional receipt allocations must be 100% order-invariant and perspective-invariant"
        )
    }
}
