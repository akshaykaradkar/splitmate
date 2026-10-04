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
        com.splitmate.app.data.PhoneOtpAuthManager.clearPendingOtpChallenge(null)
        com.splitmate.app.data.CloudGroupSyncRepository.resetInMemoryStateForTests()
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
        phoneBViewModel.completeOnboarding(
            name = "Rohan",
            countryName = "India",
            currencyCode = "INR",
            currencySymbol = "₹",
            avatarSeed = "Rohan",
            userPhone = "9765432109"
        )
        val firstMerge = phoneBViewModel.importAndMergeGroupSyncPayload(
            rawPayloadOrMessage = capsuleFromA.syncToken
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
            "Automatic local perspective claim (Rohan) must be preserved on re-import"
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
    @DisplayName("3. Subtask 5.1.1: Automatic backend identity resolution via registered 10-digit phone number, UPI VPA, and name")
    fun testFourStageIdentityResolutionAndPerspectiveSwitching() = runTest(testDispatcher) {
        val hostVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        hostVm.completeOnboarding(
            name = "Akshay",
            countryName = "India",
            currencyCode = "INR",
            currencySymbol = "₹",
            avatarSeed = "Akshay",
            userPhone = "9811122233"
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
        hostVm.updateFriendUpi(priya.memberId, "Priya", "9123456789", "Priya")
        hostVm.commitQuickEqualExpense(
            title = "Lake Pichola Boat Ride",
            totalAmountCents = 100_001L,
            selectedMemberIds = members.map { it.memberId },
            payerMemberId = akshay.memberId
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val capsule = hostVm.exportGroupSyncPayload(groupId)!!

        // Stage 1: Direct registered 10-digit phone number match from Onboarding (Priya's phone 9123456789)
        val directPhoneVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        directPhoneVm.completeOnboarding("P. Sharma", "India", "INR", "₹", "Priya", userPhone = "+91 91234-56789")
        val resStage1 = directPhoneVm.importAndMergeGroupSyncPayload(rawPayloadOrMessage = capsule.syncToken)
        assertEquals(priya.memberId, resStage1.claimedMemberId, "Registered userPhone must automatically match Priya's 10-digit phone")

        // Stage 2: 10-digit Indian mobile number match in UPI VPA (ignoring +91 prefix and different @handle)
        val phoneMatchVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        phoneMatchVm.completeOnboarding("Rohan K", "India", "INR", "₹", "Rohan")
        phoneMatchVm.updateUpiId("+919765432109@paytm")
        val resStage3 = phoneMatchVm.importAndMergeGroupSyncPayload(capsule.syncToken)
        assertEquals(rohan.memberId, resStage3.claimedMemberId, "Must match 10-digit Indian mobile number")

        // Stage 3: Exact case-insensitive UPI VPA match
        val vpaMatchVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        vpaMatchVm.completeOnboarding("Traveler", "India", "INR", "₹", "Traveler")
        vpaMatchVm.updateUpiId("SNEHA.PAY@OKSBI")
        val resStage4 = vpaMatchVm.importAndMergeGroupSyncPayload(capsule.syncToken)
        assertEquals(sneha.memberId, resStage4.claimedMemberId, "Must match case-insensitive UPI VPA")

        // Stage 4: Case-insensitive name match + collision-safe completeOnboarding after pre-onboarding import
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

        // Verify perspective resolution across all 4 members keeps net balances invariant
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
                "Perspective for ${targetMember.name} must never alter group member net balances"
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

    @Test
    @DisplayName("5. Confirmed Train Ticket & PDF Never Misclassified as Flight in Trip Hub")
    fun testConfirmedTrainNeverMisclassifiedAsFlightInTripHub() {
        // 1. Bare ParsedTravelTicket(pnr = "2841957364") has default bookingStatus = "CNF"
        //    but blank trainOrFlightNo/stations/seats -> must NOT short-circuit live network lookup!
        val bareFallback = com.splitmate.app.ui.ParsedTravelTicket(pnr = "2841957364")
        assertEquals(
            false,
            com.splitmate.app.data.PnrNetworkRepository.isTicketAllConfirmed(bareFallback),
            "Bare ParsedTravelTicket(pnr = 10-digit) must not short-circuit live PNR network fetch"
        )

        // 2. Confirmed train tickets (including legacy 'Train/Flight' prefix or empty-airline PDF fallback)
        //    must classify strictly as TRAIN and never FLIGHT
        val confirmedTrainCases = listOf(
            "Train 12627 Karnataka Exp (NDLS → SBC) | PNR: 2841957364 | Dep: 14 Nov, 19:20 | Coach/Seat: B2/34 | Status: CNF",
            "Train/Flight 12952 Mumbai Rajdhani (NDLS → MMCT) | PNR: 8421905631 | Status: CNF",
            "Flight  (NDLS → MMCT) | PNR: 8421905631 | Dep: 15 Nov 2026 | Seats: B1/12 (CNF)",
            "Flight  (CSMT → MAO) | PNR: 4521983746 | Status: CNF"
        )
        for (rawTitle in confirmedTrainCases) {
            val exp = com.splitmate.app.data.ExpenseEntity(
                expenseId = "exp_train_${rawTitle.hashCode()}",
                groupId = "g_1",
                title = rawTitle,
                payerId = "m_1",
                baseSubtotalCents = 245_000L,
                taxCents = 0L,
                tipCents = 0L,
                totalAmountCents = 245_000L,
                lockedMultiplier = 1.0,
                unassignedBaseCents = 0L,
                currencyCode = "INR",
                lockedExchangeRate = 1.0
            )
            assertEquals(
                com.splitmate.app.ui.screens.TripHubBookingCategory.TRAIN,
                com.splitmate.app.ui.screens.classifyGroupExpenseForTripHub(exp),
                "Confirmed train expense '$rawTitle' must classify as TRAIN, never FLIGHT"
            )
        }

        // 3. Real 6-character Flight PNR boarding passes must still classify as FLIGHT
        val realFlightCases = listOf(
            "Flight 6E-204 (DEL → GOI) | PNR: K9M2QX | Dep: 18 Nov, 07:15 | Seats: 12A (Confirmed)",
            "Flight AI 803 (DEL → BLR) | PNR: R4T8LP | Status: Confirmed"
        )
        for (flightTitle in realFlightCases) {
            val exp = com.splitmate.app.data.ExpenseEntity(
                expenseId = "exp_flight_${flightTitle.hashCode()}",
                groupId = "g_1",
                title = flightTitle,
                payerId = "m_1",
                baseSubtotalCents = 580_000L,
                taxCents = 0L,
                tipCents = 0L,
                totalAmountCents = 580_000L,
                lockedMultiplier = 1.0,
                unassignedBaseCents = 0L,
                currencyCode = "INR",
                lockedExchangeRate = 1.0
            )
            assertEquals(
                com.splitmate.app.ui.screens.TripHubBookingCategory.FLIGHT,
                com.splitmate.app.ui.screens.classifyGroupExpenseForTripHub(exp),
                "Real flight expense '$flightTitle' must classify as FLIGHT"
            )
        }
    }

    @Test
    @DisplayName("6. Task 5.1.1: PhoneIdentityValidator & Shortcode Rejection")
    fun testPhoneIdentityValidatorAndShortcodeRejection() {
        assertEquals(
            "9876543210",
            com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10("+91 98765 43210"),
            "+91 98765 43210 must normalize to 9876543210"
        )
        assertEquals(
            "9876543210",
            com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10("09876543210"),
            "09876543210 must normalize to 9876543210"
        )
        assertEquals(
            "",
            com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10("12345"),
            "Shortcode 12345 must be rejected with empty string"
        )
        assertFalse(
            com.splitmate.app.data.PhoneIdentityValidator.isValidIndianMobile10("12345"),
            "Shortcode 12345 must not be valid Indian mobile"
        )
        assertFalse(
            com.splitmate.app.data.PhoneIdentityValidator.isValidIndianMobile10("5876543210"),
            "10-digit number starting with 5 must be rejected"
        )
        assertTrue(
            com.splitmate.app.data.PhoneIdentityValidator.isValidIndianMobile10("9876543210"),
            "9876543210 must be valid Indian mobile"
        )
    }

    @Test
    @DisplayName("7. Task 5.1.2: PhoneOtpAuthManager 6-Digit OTP, Expiry & Salted SHA-256 PIN")
    fun testPhoneOtpAuthManagerSixDigitOtpExpiryAndSha256Pin() {
        com.splitmate.app.data.PhoneOtpAuthManager.clearPendingOtpChallenge(null)
        // Reject shortcode OTP dispatch
        assertEquals(
            null,
            com.splitmate.app.data.PhoneOtpAuthManager.sendOtp(null, "12345"),
            "sendOtp must return null for invalid shortcode"
        )

        val dispatch = com.splitmate.app.data.PhoneOtpAuthManager.sendOtp(null, "+91 98765 43210")
        assertNotNull(dispatch, "sendOtp must succeed for +91 98765 43210")
        val otpResult = dispatch!!
        assertEquals("9876543210", otpResult.phone10)
        val generatedCode = com.splitmate.app.data.PhoneOtpAuthManager.peekLastGeneratedOtpForTestOnly()
        assertNotNull(generatedCode, "Test-only OTP peek must return generated 6-digit code")
        assertEquals(6, generatedCode!!.length, "OTP code must be 6 digits")
        assertTrue(generatedCode.all { it.isDigit() }, "OTP code must be numeric")

        // Verify failure after expiresAtEpochMs + 1
        assertFalse(
            com.splitmate.app.data.PhoneOtpAuthManager.verifyOtp(
                rawPhone = "+91 98765 43210",
                enteredCode = generatedCode,
                nowEpochMs = otpResult.expiresAtEpochMs + 1L
            ),
            "verifyOtp must fail when nowEpochMs > expiresAtEpochMs"
        )

        // Verify failure with wrong OTP before expiry
        assertFalse(
            com.splitmate.app.data.PhoneOtpAuthManager.verifyOtp(
                rawPhone = "9876543210",
                enteredCode = "000000",
                nowEpochMs = otpResult.expiresAtEpochMs - 1_000L
            ),
            "verifyOtp must fail with wrong code"
        )

        // Verify success with valid 6-digit OTP before expiry
        assertTrue(
            com.splitmate.app.data.PhoneOtpAuthManager.verifyOtp(
                rawPhone = "09876543210",
                enteredCode = generatedCode,
                nowEpochMs = otpResult.expiresAtEpochMs - 1_000L
            ),
            "verifyOtp must succeed with matching 6-digit OTP before expiry"
        )

        // One-time consumption: replay must fail
        assertFalse(
            com.splitmate.app.data.PhoneOtpAuthManager.verifyOtp(
                rawPhone = "9876543210",
                enteredCode = generatedCode,
                nowEpochMs = otpResult.expiresAtEpochMs - 500L
            ),
            "Consumed OTP must not be reusable"
        )

        // Salted SHA-256 PIN hashing & verification
        val pinHash = com.splitmate.app.data.PhoneOtpAuthManager.hashPin("9876543210", "4829")
        assertEquals(64, pinHash.length, "SHA-256 PIN hash must be 64 hex chars")
        assertTrue(
            com.splitmate.app.data.PhoneOtpAuthManager.verifyPin("9876543210", "4829", pinHash),
            "verifyPin must succeed for matching phone10 and 4-digit PIN"
        )
        assertFalse(
            com.splitmate.app.data.PhoneOtpAuthManager.verifyPin("9876543210", "9999", pinHash),
            "verifyPin must fail for wrong 4-digit PIN"
        )
        assertFalse(
            com.splitmate.app.data.PhoneOtpAuthManager.verifyPin("9123456789", "4829", pinHash),
            "Salted verifyPin must fail if phone10 differs"
        )
    }

    @Test
    @DisplayName("8. Task 5.1.3: CloudGroupSyncRepository Round-Trip JSON & CRDT Tombstone Union Merge")
    fun testCloudGroupSyncRepositoryRoundTripJsonAndCrdtTombstoneMerge() {
        val group = com.splitmate.app.data.ExpenseGroupEntity(
            groupId = "g_cloud_goa",
            name = "Goa Monsoon Escape",
            currencyCode = "INR",
            iconName = "Flight",
            isDemoSeed = false,
            createdAt = 1760000000000L
        )
        val members = listOf(
            com.splitmate.app.data.GroupMemberEntity(
                memberId = "m_akshay",
                groupId = "g_cloud_goa",
                name = "Akshay",
                avatarSeed = "Akshay|open-peeps|Buckwheat",
                isCurrentUser = true,
                upiId = "9876543210@upi",
                userPhone = "9876543210",
                inviteStatus = "JOINED"
            ),
            com.splitmate.app.data.GroupMemberEntity(
                memberId = "m_rohan",
                groupId = "g_cloud_goa",
                name = "Rohan",
                avatarSeed = "Rohan|adventurer|Terracotta",
                isCurrentUser = false,
                upiId = "9123456789@upi",
                userPhone = "9123456789",
                inviteStatus = "PENDING"
            )
        )
        val scheduledAt = 1765003200000L
        val expSurviving = com.splitmate.app.data.ExpenseEntity(
            expenseId = "exp_flight_1",
            groupId = "g_cloud_goa",
            title = "IndiGo 6E-204 • DEL → GOI • PNR: K9M2QX",
            payerId = "m_akshay",
            baseSubtotalCents = 840_000L,
            taxCents = 0L,
            tipCents = 0L,
            totalAmountCents = 840_000L,
            lockedMultiplier = 1.0,
            unassignedBaseCents = 0L,
            currencyCode = "INR",
            lockedExchangeRate = 1.0,
            expenseCategory = "FLIGHT",
            travelPnr = "K9M2QX",
            providerName = "IndiGo 6E-204",
            scheduledAtEpochMs = scheduledAt,
            syncStatus = "SYNCED",
            createdAt = 1760000100000L
        )
        val expDeletedOnRemote = com.splitmate.app.data.ExpenseEntity(
            expenseId = "exp_deleted_cab",
            groupId = "g_cloud_goa",
            title = "Duplicate Airport Cab",
            payerId = "m_akshay",
            baseSubtotalCents = 120_000L,
            taxCents = 0L,
            tipCents = 0L,
            totalAmountCents = 120_000L,
            lockedMultiplier = 1.0,
            unassignedBaseCents = 0L,
            currencyCode = "INR",
            lockedExchangeRate = 1.0,
            expenseCategory = "CAB",
            travelPnr = "",
            providerName = "Uber",
            scheduledAtEpochMs = null,
            syncStatus = "SYNCED",
            createdAt = 1760000200000L
        )
        val splits = listOf(
            com.splitmate.app.data.ExpenseSplitEntity("sp_f1", "exp_flight_1", "m_akshay", 420_000L, 420_000L, false),
            com.splitmate.app.data.ExpenseSplitEntity("sp_f2", "exp_flight_1", "m_rohan", 420_000L, 420_000L, false),
            com.splitmate.app.data.ExpenseSplitEntity("sp_del1", "exp_deleted_cab", "m_akshay", 60_000L, 60_000L, false),
            com.splitmate.app.data.ExpenseSplitEntity("sp_del2", "exp_deleted_cab", "m_rohan", 60_000L, 60_000L, false)
        )

        val localDoc = com.splitmate.app.data.CloudGroupLedgerDocument(
            group = group,
            members = members,
            expenses = listOf(expSurviving, expDeletedOnRemote),
            splits = splits,
            settlements = emptyList(),
            deletedExpenseIds = emptyMap(),
            flightVaultByPnr = mapOf("K9M2QX" to """{"pnr":"K9M2QX","airline":"IndiGo"}"""),
            trainSnapshotByPnr = mapOf("2841957364" to """{"pnrNumber":"2841957364","trainNo":"12627"}"""),
            updatedAtEpochMs = 1760000500000L
        )

        // 1. Round-trip JSON encode/decode verification
        val encodedJson = com.splitmate.app.data.CloudGroupSyncRepository.encodeGroupLedgerDocument(localDoc)
        val decodedDoc = com.splitmate.app.data.CloudGroupSyncRepository.decodeGroupLedgerDocument(encodedJson)
        assertNotNull(decodedDoc, "Decoded CloudGroupLedgerDocument must not be null")
        val rt = decodedDoc!!
        assertEquals(false, rt.group.isDemoSeed)
        assertEquals("PENDING", rt.members.first { it.memberId == "m_rohan" }.inviteStatus)
        val rtFlightExp = rt.expenses.first { it.expenseId == "exp_flight_1" }
        assertEquals("FLIGHT", rtFlightExp.expenseCategory)
        assertEquals("K9M2QX", rtFlightExp.travelPnr)
        assertEquals("IndiGo 6E-204", rtFlightExp.providerName)
        assertEquals(scheduledAt, rtFlightExp.scheduledAtEpochMs)
        assertEquals("""{"pnr":"K9M2QX","airline":"IndiGo"}""", rt.flightVaultByPnr["K9M2QX"])
        assertEquals("""{"pnrNumber":"2841957364","trainNo":"12627"}""", rt.trainSnapshotByPnr["2841957364"])

        // 2. CRDT Tombstone Union Merge + localUserPhone10 perspective binding (Rohan's phone: 9123456789)
        val remoteDocWithTombstone = rt.copy(
            members = rt.members.map {
                if (it.memberId == "m_rohan") it.copy(inviteStatus = "JOINED") else it
            },
            expenses = listOf(expSurviving),
            splits = splits.filter { it.expenseId == "exp_flight_1" },
            deletedExpenseIds = mapOf("exp_deleted_cab" to 1760000900000L),
            updatedAtEpochMs = 1760000900000L
        )

        val mergedOnRohanPhone = com.splitmate.app.data.CloudGroupSyncRepository.mergeGroupLedgerDocuments(
            localDoc = localDoc,
            remoteDoc = remoteDocWithTombstone,
            localUserPhone10 = "9123456789"
        )

        assertTrue(
            mergedOnRohanPhone.deletedExpenseIds.containsKey("exp_deleted_cab"),
            "CRDT tombstone for exp_deleted_cab must be preserved in merged document"
        )
        assertEquals(
            listOf("exp_flight_1"),
            mergedOnRohanPhone.expenses.map { it.expenseId },
            "Tombstoned expense exp_deleted_cab must never resurrect during union merge"
        )
        assertTrue(
            mergedOnRohanPhone.splits.none { it.expenseId == "exp_deleted_cab" },
            "Splits belonging to tombstoned expense must be pruned"
        )
        val rohanMerged = mergedOnRohanPhone.members.first { it.memberId == "m_rohan" }
        val akshayMerged = mergedOnRohanPhone.members.first { it.memberId == "m_akshay" }
        assertTrue(rohanMerged.isCurrentUser, "Rohan must be bound as isCurrentUser = true on localUserPhone10 = 9123456789")
        assertFalse(akshayMerged.isCurrentUser, "Akshay must have isCurrentUser = false on Rohan's phone")
        assertEquals("JOINED", rohanMerged.inviteStatus, "Newer inviteStatus = JOINED must win in union merge")
    }

    @Test
    @DisplayName("9. Task 5.1.4: Structured classifyGroupExpenseForTripHub & resolveExpenseSchedule O(1) Priority")
    fun testStructuredClassifyGroupExpenseAndResolveScheduleO1Priority() {
        val scheduledEpoch = 1763541000000L // Explicit travel/schedule epoch
        val createdEpoch = 1760000000000L

        // Expense with title mentioning "train station" and a conflicting date string in title,
        // but structured expenseCategory = "FOOD" and explicit scheduledAtEpochMs
        val foodNearTrainStation = com.splitmate.app.data.ExpenseEntity(
            expenseId = "exp_food_station",
            groupId = "g_1",
            title = "Dinner near train station on 25 Dec 2026",
            payerId = "m_1",
            baseSubtotalCents = 180_000L,
            taxCents = 0L,
            tipCents = 0L,
            totalAmountCents = 180_000L,
            lockedMultiplier = 1.0,
            unassignedBaseCents = 0L,
            currencyCode = "INR",
            lockedExchangeRate = 1.0,
            expenseCategory = "FOOD",
            travelPnr = "",
            providerName = "",
            scheduledAtEpochMs = scheduledEpoch,
            createdAt = createdEpoch
        )

        assertEquals(
            com.splitmate.app.ui.screens.TripHubBookingCategory.FOOD,
            com.splitmate.app.ui.screens.classifyGroupExpenseForTripHub(foodNearTrainStation),
            "Expense with structured expenseCategory = FOOD must NOT be misclassified as TRAIN despite 'train' in title"
        )

        val resolvedSchedule = com.splitmate.app.ui.screens.resolveExpenseSchedule(
            context = null,
            expense = foodNearTrainStation
        )
        assertEquals(
            scheduledEpoch,
            resolvedSchedule.effectiveEpochMs,
            "scheduledAtEpochMs must take O(1) precedence over any date substring in title"
        )
        assertTrue(resolvedSchedule.hasExplicitTicketDate)
    }

    @Test
    @DisplayName("10. Task 5.1.5: Soft-Decline (declineGroupInvite) + 1-Tap Share Reassignment (0.00c Drift)")
    fun testSoftDeclineAndOneTapShareReassignmentZeroDrift() = runTest(testDispatcher) {
        val viewModel = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        viewModel.completeOnboarding(
            name = "Akshay",
            countryName = "India",
            currencyCode = "INR",
            currencySymbol = "₹",
            avatarSeed = "Akshay",
            userPhone = "9876543210"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Create 3-person group: Akshay (JOINED), Rohan (JOINED), Sneha (PENDING with phone 9988776655)
        viewModel.createNewGroupWithContacts(
            name = "Alibaug Villa Weekend",
            iconName = "Hotel",
            memberDrafts = listOf(
                com.splitmate.app.ui.NewGroupMemberDraft(name = "Rohan", cleanPhone = ""),
                com.splitmate.app.ui.NewGroupMemberDraft(name = "Sneha", cleanPhone = "9988776655")
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val groupId = viewModel.uiState.value.activeGroupId
        val membersBefore = viewModel.uiState.value.members.filter { it.groupId == groupId }
        assertEquals(3, membersBefore.size, "Group must have 3 members")

        val akshay = membersBefore.first { it.name == "Akshay" }
        val rohan = membersBefore.first { it.name == "Rohan" }
        val sneha = membersBefore.first { it.name == "Sneha" }

        // Split Rs 3,000.00 (300_000 paise) across all 3 members (Rs 1,000.00 = 100_000 paise each)
        viewModel.commitQuickEqualExpense(
            title = "Beachfront Villa Deposit",
            totalAmountCents = 300_000L,
            selectedMemberIds = listOf(akshay.memberId, rohan.memberId, sneha.memberId),
            payerMemberId = akshay.memberId
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val expense = viewModel.uiState.value.expenses.single { it.groupId == groupId }
        val initialSplits = viewModel.uiState.value.splits.filter { it.expenseId == expense.expenseId }
        assertEquals(3, initialSplits.size)
        assertEquals(300_000L, initialSplits.sumOf { it.finalOwedCents })
        assertEquals(100_000L, initialSplits.first { it.memberId == sneha.memberId }.finalOwedCents)

        // Switch perspective to Sneha and invoke declineGroupInvite -> inviteStatus becomes DECLINED
        viewModel.claimGroupMemberPerspective(groupId, sneha.memberId)
        viewModel.declineGroupInvite(context = null, groupId = groupId)
        testDispatcher.scheduler.advanceUntilIdle()

        val snehaAfterDecline = viewModel.uiState.value.members.first { it.memberId == sneha.memberId }
        assertEquals("DECLINED", snehaAfterDecline.inviteStatus, "Member 3 must be soft-declined (inviteStatus = DECLINED)")

        // Switch back to Akshay and invoke 1-tap reassignDeclinedMemberSharesEqually for Sneha
        viewModel.claimGroupMemberPerspective(groupId, akshay.memberId)
        viewModel.reassignDeclinedMemberSharesEqually(
            context = null,
            groupId = groupId,
            declinedMemberId = sneha.memberId
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val reassignedSplits = viewModel.uiState.value.splits.filter { it.expenseId == expense.expenseId }
        assertEquals(2, reassignedSplits.size, "Splits must now be assigned only to the 2 active JOINED members")
        assertEquals(
            300_000L,
            reassignedSplits.sumOf { it.finalOwedCents },
            "Reassigned splits must sum to exact Rs 3,000.00 (300,000 paise) with 0.00c drift"
        )
        assertEquals(
            150_000L,
            reassignedSplits.first { it.memberId == akshay.memberId }.finalOwedCents,
            "Akshay's reassigned share must be Rs 1,500.00 (150,000 paise)"
        )
        assertEquals(
            150_000L,
            reassignedSplits.first { it.memberId == rohan.memberId }.finalOwedCents,
            "Rohan's reassigned share must be Rs 1,500.00 (150,000 paise)"
        )
        assertTrue(
            reassignedSplits.none { it.memberId == sneha.memberId },
            "Declined member Sneha must have 0 splits remaining on the expense"
        )

        val finalNetBalances = viewModel.computeGroupMemberNetBalances(groupId)
        assertEquals(0L, finalNetBalances[sneha.memberId], "Declined member net balance must be 0L")
        assertEquals(150_000L, finalNetBalances[akshay.memberId], "Akshay net balance must be +Rs 1,500.00")
        assertEquals(-150_000L, finalNetBalances[rohan.memberId], "Rohan net balance must be -Rs 1,500.00")
        assertEquals(0L, finalNetBalances.values.sum(), "Conservation of flow must hold with 0.00c drift")
    }

    @Test
    @DisplayName("11. End-to-End Registration, OTP, 4-Digit PIN Unlock, Forgot-PIN Reset & >6KB GZIP Cloud Sync")
    fun testLiveRegistrationOtpPinVerificationAndLargePayloadCompression() = runTest(testDispatcher) {
        val viewModel = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)

        // 1. Request OTP for +91 9876543210
        val dispatch = viewModel.requestPhoneOtp(context = null, rawPhone = "+91 98765 43210")
        assertNotNull(dispatch, "OTP dispatch must succeed for valid 10-digit Indian phone")
        assertEquals("9876543210", dispatch!!.phone10)
        val generatedCode = com.splitmate.app.data.PhoneOtpAuthManager.peekLastGeneratedOtpForTestOnly()!!
        assertEquals(6, generatedCode.length)
        assertTrue(viewModel.uiState.value.isOtpChallengeActive)

        // 2. Verify OTP and register with 4-digit Recovery PIN "4829", style "adventurer", preset "Electric"
        var otpResultOk = false
        var otpResultMsg = ""
        viewModel.verifyPhoneOtpAndSyncCloud(
            context = null,
            rawPhone = "9876543210",
            enteredOtp = generatedCode,
            userName = "Akshay Karadkar",
            upiId = "akshay@okaxis",
            avatarStyleId = "adventurer",
            avatarColorPresetId = "Electric",
            optionalPin4 = "4829"
        ) { ok, msg ->
            otpResultOk = ok
            otpResultMsg = msg
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(otpResultOk, "OTP verification must succeed: $otpResultMsg")
        val stateAfterReg = viewModel.uiState.value
        assertTrue(stateAfterReg.isPhoneVerified)
        assertEquals("9876543210", stateAfterReg.userPhone)
        assertEquals("Akshay Karadkar", stateAfterReg.currentUserName)
        assertEquals("adventurer", stateAfterReg.avatarStyleId)
        assertEquals("Electric", stateAfterReg.avatarColorPresetId)
        assertTrue(
            com.splitmate.app.data.PhoneOtpAuthManager.verifyPin("9876543210", "4829", stateAfterReg.pinHash),
            "Stored pinHash must validate against PIN 4829"
        )

        // 3. Returning User 4-Digit PIN Verification (Reject wrong PIN "0000", Accept "4829")
        var wrongPinOk = true
        viewModel.verifyPinAndRestoreCloud(
            context = null,
            rawPhone = "9876543210",
            enteredPin4 = "0000"
        ) { ok, _ -> wrongPinOk = ok }
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(wrongPinOk, "Wrong 4-digit PIN must be rejected")

        var rightPinOk = false
        viewModel.verifyPinAndRestoreCloud(
            context = null,
            rawPhone = "9876543210",
            enteredPin4 = "4829"
        ) { ok, _ -> rightPinOk = ok }
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(rightPinOk, "Correct 4-digit PIN 4829 must unlock and restore account")

        // 4. Forgot-PIN Reset after OTP verification -> overwrite PIN to "7391"
        viewModel.resetPinAfterOtpVerified("9876543210", "7391")
        testDispatcher.scheduler.advanceUntilIdle()
        val updatedPinHash = viewModel.uiState.value.pinHash
        assertFalse(com.splitmate.app.data.PhoneOtpAuthManager.verifyPin("9876543210", "4829", updatedPinHash))
        assertTrue(com.splitmate.app.data.PhoneOtpAuthManager.verifyPin("9876543210", "7391", updatedPinHash))

        // 5. >6KB Group Ledger GZIP+Base64 Compression & Lossless Round-Trip
        val largeExpenses = (1..20).map { idx ->
            com.splitmate.app.data.ExpenseEntity(
                expenseId = "exp_large_$idx",
                groupId = "g_goa_2026",
                title = "Goa Beach Villa & Scuba Package Day $idx",
                payerId = "m_1",
                baseSubtotalCents = 250_000L,
                taxCents = 0L,
                tipCents = 0L,
                totalAmountCents = 250_000L,
                lockedMultiplier = 1.0,
                unassignedBaseCents = 0L,
                currencyCode = "INR",
                lockedExchangeRate = 1.0,
                expenseCategory = "STAY",
                travelPnr = "PNR$idx",
                providerName = "Taj Exotica Goa Resort & Spa",
                scheduledAtEpochMs = 1790450000000L + idx * 86_400_000L
            )
        }
        val largeDoc = com.splitmate.app.data.CloudGroupLedgerDocument(
            group = com.splitmate.app.data.ExpenseGroupEntity("g_goa_2026", "Goa Mega Trip", "INR"),
            members = listOf(
                com.splitmate.app.data.GroupMemberEntity("m_1", "g_goa_2026", "Akshay", "Akshay|Neutral|adventurer|Electric", isCurrentUser = true, userPhone = "9876543210", inviteStatus = "JOINED")
            ),
            expenses = largeExpenses,
            splits = emptyList(),
            settlements = emptyList(),
            deletedExpenseIds = emptyMap(),
            flightVaultByPnr = mapOf("6E204" to "{\"airline\":\"IndiGo\",\"passengers\":[\"Akshay\",\"Rohan\",\"Sneha\"],\"notes\":\"${"X".repeat(3000)}\"}"),
            trainSnapshotByPnr = mapOf("8241902341" to "{\"trainNo\":\"12952\",\"coach\":\"B2\",\"berth\":\"34\"}"),
            updatedAtEpochMs = 1790453000000L
        )
        val rawJson = com.splitmate.app.data.CloudGroupSyncRepository.encodeGroupLedgerDocument(largeDoc)
        assertTrue(rawJson.length > 6000, "Raw JSON should exceed 6KB to trigger GZIP compression")

        val compressed = com.splitmate.app.data.CloudGroupSyncRepository.compressPayloadIfNeeded(rawJson)
        assertTrue(compressed.startsWith("SMGZ:"), "Payloads >3KB must be GZIP+Base64 compressed with SMGZ: prefix")
        assertTrue(compressed.length < 3500, "Compressed payload must stay under 3.5KB for inline ntfy.sh delivery")

        val decompressed = com.splitmate.app.data.CloudGroupSyncRepository.decompressPayloadIfNeeded(compressed)
        assertEquals(rawJson, decompressed, "Decompressed JSON must match original raw JSON byte-for-byte")
        val decodedDoc = com.splitmate.app.data.CloudGroupSyncRepository.decodeGroupLedgerDocument(decompressed)
        assertNotNull(decodedDoc)
        assertEquals(20, decodedDoc!!.expenses.size)
        assertEquals("Taj Exotica Goa Resort & Spa", decodedDoc.expenses.first().providerName)
    }

    private fun resolveSrcMainDir(): java.io.File {
        val candidates = listOf(
            java.io.File("src/main"),
            java.io.File("app/src/main"),
            java.io.File("/usr/local/google/home/karadkar/splitmate/android/app/src/main")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Could not locate src/main directory from working directory ${java.io.File(".").absolutePath}")
    }

    @Test
    @DisplayName("12. Phase 3 Guard 1: Zero Unicode Emojis & Zero Inline Dingbats Across All .kt & .svg Files")
    fun testPhase3Guard1ZeroEmojisAndZeroInlineDingbatsAcrossCodebaseAndAssets() {
        val srcMain = resolveSrcMainDir()
        val kotlinDir = java.io.File(srcMain, "java")
        val assetsDir = java.io.File(srcMain, "assets")
        assertTrue(kotlinDir.exists(), "src/main/java must exist")
        assertTrue(assetsDir.exists(), "src/main/assets must exist")

        val forbiddenDingbats = setOf('➔', '▲', '▼', '▾', '⤾', '↗', '›')
        val ktFiles = kotlinDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        val svgFiles = assetsDir.walkTopDown().filter { it.isFile && it.extension == "svg" }.toList()
        assertTrue(ktFiles.isNotEmpty(), "Must scan non-empty list of .kt source files")
        assertTrue(svgFiles.isNotEmpty(), "Must scan non-empty list of .svg asset files")

        val violations = mutableListOf<String>()
        for (file in ktFiles + svgFiles) {
            val content = file.readText(Charsets.UTF_8)
            var idx = 0
            while (idx < content.length) {
                val cp = content.codePointAt(idx)
                val ch = content[idx]
                if (cp in 0x1F300..0x1FAFF || cp in 0x2600..0x27BF || ch in forbiddenDingbats) {
                    violations.add("${file.name}@offset($idx): U+${cp.toString(16).uppercase()}")
                    break
                }
                idx += Character.charCount(cp)
            }
        }
        assertTrue(
            violations.isEmpty(),
            "Zero emojis and zero inline dingbats allowed in src/main/java or src/main/assets, found: $violations"
        )
    }

    @Test
    @DisplayName("13. Phase 3 Guard 2: 5 Happy Open Peeps Group Scenes Present & Wheelchair/Prosthetic SVGs Deleted")
    fun testPhase3Guard2HappyOpenPeepsGroupScenesAndDeletedSadPoses() {
        val peepsDir = java.io.File(resolveSrcMainDir(), "assets/peeps")
        val scenesDir = java.io.File(peepsDir, "scenes")
        assertTrue(scenesDir.exists() && scenesDir.isDirectory, "assets/peeps/scenes directory must exist")

        val deletedFiles = listOf(
            "peep_sitting_14.svg",
            "peep_standing_8.svg",
            "peep_standing_4.svg"
        )
        for (deletedName in deletedFiles) {
            val f = java.io.File(peepsDir, deletedName)
            assertFalse(f.exists(), "Deprecated single-person SVG $deletedName must be deleted")
        }

        val requiredScenes = listOf(
            "scene_walk_crew.svg",
            "scene_bike_trip.svg",
            "scene_coffee_hangout.svg",
            "scene_weekend_squad.svg",
            "scene_roadtrip_busters.svg"
        )
        for (sceneName in requiredScenes) {
            val sceneFile = java.io.File(scenesDir, sceneName)
            assertTrue(sceneFile.exists() && sceneFile.length() > 10_000L, "Scene SVG $sceneName must exist and be non-trivial")
            val svgText = sceneFile.readText(Charsets.UTF_8)
            val groupCount = Regex("<g\\b").findAll(svgText).count()
            assertTrue(groupCount >= 4, "Multi-person scene $sceneName must contain multiple character <g> groups (found $groupCount)")
            assertFalse(svgText.contains("wheelchair", ignoreCase = true), "$sceneName must not contain wheelchair pose")
            assertFalse(svgText.contains("prosthetic", ignoreCase = true), "$sceneName must not contain prosthetic pose")
        }
    }

    @Test
    @DisplayName("14. Phase 3 Guard 3: 13 Official DiceBear Styles x 12 Presets x 3 Genders (468 URLs) Schema Safety & Name Inference")
    fun testPhase3Guard3Official13Styles12Presets3GendersAnd468UrlSchemaSafety() {
        val styles = com.splitmate.app.ui.SplitMateDiceBearStyles
        val presets = com.splitmate.app.ui.SplitMateAvatarColorPresets
        val genders = com.splitmate.app.ui.AvatarGender.entries

        assertEquals(13, styles.size, "Must have exactly 13 official DiceBear 9.x character styles")
        assertEquals(12, presets.size, "Must have exactly 12 official DiceBear color presets")
        assertEquals(3, genders.size, "Must have exactly 3 AvatarGender options (Male, Female, Neutral)")

        val expectedStyleIds = setOf(
            "open-peeps", "adventurer", "avataaars", "big-ears", "big-smile",
            "croodles", "dylan", "lorelei", "micah", "miniavs",
            "notionists", "personas", "toon-head"
        )
        assertEquals(expectedStyleIds, styles.map { it.id }.toSet())

        val openPeepsExclusiveKeys = listOf(
            "face=",
            "head=",
            "clothingColor=",
            "skinColor=",
            "headContrastColor=",
            "maskProbability="
        )

        var testedCombinations = 0
        for (style in styles) {
            for (preset in presets) {
                for (gender in genders) {
                    val encodedSeed = com.splitmate.app.ui.AvatarSeedCodec.encode(
                        seedKey = "Akshay Karadkar",
                        gender = gender,
                        styleId = style.id,
                        colorPresetId = preset.id
                    )
                    val url = com.splitmate.app.ui.buildDiceBearAvatarUrl(name = encodedSeed)
                    assertTrue(
                        url.startsWith("https://api.dicebear.com/9.x/${style.id}/svg?"),
                        "URL must target DiceBear 9.x ${style.id} endpoint: $url"
                    )
                    if (style.id != "open-peeps") {
                        for (forbiddenParam in openPeepsExclusiveKeys) {
                            assertFalse(
                                url.contains(forbiddenParam),
                                "Non-open-peeps style '${style.id}' (preset=${preset.id}, gender=${gender.id}) must NEVER leak '$forbiddenParam' in URL: $url"
                            )
                        }
                    } else {
                        assertTrue(url.contains("face=smile"), "open-peeps URL must enforce smiling face whitelist: $url")
                        assertTrue(url.contains("maskProbability=0"), "open-peeps URL must enforce maskProbability=0: $url")
                    }
                    if (gender == com.splitmate.app.ui.AvatarGender.FEMALE &&
                        style.id in setOf("open-peeps", "avataaars", "personas", "micah", "dylan")
                    ) {
                        assertTrue(
                            url.contains("facialHairProbability=0"),
                            "Female avatar on ${style.id} must enforce facialHairProbability=0: $url"
                        )
                    }
                    if (gender == com.splitmate.app.ui.AvatarGender.FEMALE &&
                        style.id in setOf("toon-head", "lorelei", "notionists", "croodles")
                    ) {
                        assertTrue(
                            url.contains("beardProbability=0"),
                            "Female avatar on ${style.id} must enforce beardProbability=0: $url"
                        )
                    }
                    testedCombinations++
                }
            }
        }
        assertEquals(468, testedCombinations, "Must validate all 13 x 12 x 3 = 468 combinations")

        // Verify smart first-name gender inference
        assertEquals(com.splitmate.app.ui.AvatarGender.MALE, com.splitmate.app.ui.inferGenderFromFirstName("Akshay Karadkar"))
        assertEquals(com.splitmate.app.ui.AvatarGender.MALE, com.splitmate.app.ui.inferGenderFromFirstName("  rohan_482 "))
        assertEquals(com.splitmate.app.ui.AvatarGender.FEMALE, com.splitmate.app.ui.inferGenderFromFirstName("Priya Sharma"))
        assertEquals(com.splitmate.app.ui.AvatarGender.FEMALE, com.splitmate.app.ui.inferGenderFromFirstName("Sneha"))
        assertEquals(null, com.splitmate.app.ui.inferGenderFromFirstName(""))
        assertEquals(null, com.splitmate.app.ui.inferGenderFromFirstName("   "))
        assertEquals(null, com.splitmate.app.ui.inferGenderFromFirstName("Explorer"))
    }

    @Test
    @DisplayName("15. Phase 3 Guard 4: AvatarSeedCodec Pipe Injection Defense, 1-to-4 Token Parsing & URL Unification")
    fun testPhase3Guard4AvatarSeedCodecPipeInjectionAnd4TokenUrlUnification() {
        // 1. Pipe character injection defense + URL-encoding of seedKey
        val maliciousName = "Akshay|Female|lorelei|Electric&evilParam=1"
        val encoded4Token = com.splitmate.app.ui.AvatarSeedCodec.encode(
            seedKey = maliciousName,
            gender = com.splitmate.app.ui.AvatarGender.MALE,
            styleId = "adventurer",
            colorPresetId = "Sunrise"
        )
        assertEquals(
            3,
            encoded4Token.count { it == '|' },
            "Encoded 4-token seed must contain strictly 3 pipe delimiters even when input name contains pipes: $encoded4Token"
        )
        val parsedMalicious = com.splitmate.app.ui.AvatarSeedCodec.parse(encoded4Token)
        assertFalse(parsedMalicious.seedKey.contains("|"), "Sanitized seedKey must never contain pipe delimiter")
        assertEquals(com.splitmate.app.ui.AvatarGender.MALE, parsedMalicious.gender)
        assertEquals("adventurer", parsedMalicious.styleId)
        assertEquals("Sunrise", parsedMalicious.colorPresetId)

        val safeUrl = com.splitmate.app.ui.buildDiceBearAvatarUrl(name = encoded4Token)
        assertFalse(safeUrl.contains("&evilParam=1"), "URL-encoding of seedKey must neutralize query parameter injection: $safeUrl")
        assertTrue(safeUrl.contains("%26evilParam%3D1"), "Injected '&evilParam=1' must be percent-encoded inside seed=: $safeUrl")

        // 2. Safe parsing of 1-token, 2-token, 3-token, and 4-token strings
        val p1 = com.splitmate.app.ui.AvatarSeedCodec.parse("Rohan")
        assertEquals("Rohan", p1.seedKey)
        assertEquals(com.splitmate.app.ui.AvatarGender.NEUTRAL, p1.gender)
        assertEquals("open-peeps", p1.styleId)
        assertEquals("PastelWall", p1.colorPresetId)

        val p2Gender = com.splitmate.app.ui.AvatarSeedCodec.parse("Priya|Female")
        assertEquals("Priya", p2Gender.seedKey)
        assertEquals(com.splitmate.app.ui.AvatarGender.FEMALE, p2Gender.gender)

        val p2Style = com.splitmate.app.ui.AvatarSeedCodec.parse("Rohan|adventurer")
        assertEquals("Rohan", p2Style.seedKey)
        assertEquals("adventurer", p2Style.styleId)

        val p3Legacy = com.splitmate.app.ui.AvatarSeedCodec.parse("Akshay|micah|Electric")
        assertEquals("Akshay", p3Legacy.seedKey)
        assertEquals("micah", p3Legacy.styleId)
        assertEquals("Electric", p3Legacy.colorPresetId)

        val p4Canonical = com.splitmate.app.ui.AvatarSeedCodec.parse("Sneha_777|Female|lorelei|BoldPop")
        assertEquals("Sneha_777", p4Canonical.seedKey)
        assertEquals(com.splitmate.app.ui.AvatarGender.FEMALE, p4Canonical.gender)
        assertEquals("lorelei", p4Canonical.styleId)
        assertEquals("BoldPop", p4Canonical.colorPresetId)

        // 3. Unification between buildDiceBearAvatarUrl and buildDiceBearOpenPeepsUrl
        val canonicalSeed = com.splitmate.app.ui.AvatarSeedCodec.encode(
            seedKey = "Akshay_2026",
            gender = com.splitmate.app.ui.AvatarGender.MALE,
            styleId = "toon-head",
            colorPresetId = "NightShift"
        )
        assertEquals(
            com.splitmate.app.ui.buildDiceBearAvatarUrl(name = canonicalSeed),
            com.splitmate.app.ui.buildDiceBearOpenPeepsUrl(rawSeed = canonicalSeed),
            "buildDiceBearOpenPeepsUrl and buildDiceBearAvatarUrl must return identical SVG URLs for any 4-token seed"
        )
    }

    @Test
    @DisplayName("16. Phase 3 Guard 5: PhD OTP Security (Phone-Swap & 5-Attempt Lockout, DLT SMS, Zero Leak Banner) & 8-Field Cloud Profile Sync")
    fun testPhase3Guard5PhdOtpHmacSecurityBruteForceLockoutDltSmsAnd8FieldCloudProfileSync() {
        // 1. Verify OtpDispatchResult does not expose generatedOtpCode field
        val dispatchFields = com.splitmate.app.data.OtpDispatchResult::class.java.declaredFields.map { it.name }
        assertFalse(
            "generatedOtpCode" in dispatchFields,
            "OtpDispatchResult must not expose plaintext generatedOtpCode"
        )

        // 2. Verify pendingOtpCodeForBanner is completely removed from SplitMateViewModel.kt & SplitMateAppComposable.kt
        val srcMain = resolveSrcMainDir()
        val vmSource = java.io.File(srcMain, "java/com/splitmate/app/ui/SplitMateViewModel.kt").readText(Charsets.UTF_8)
        val appComposableSource = java.io.File(srcMain, "java/com/splitmate/app/ui/SplitMateAppComposable.kt").readText(Charsets.UTF_8)
        assertFalse(vmSource.contains("pendingOtpCodeForBanner"), "SplitMateViewModel.kt must not contain pendingOtpCodeForBanner")
        assertFalse(appComposableSource.contains("pendingOtpCodeForBanner"), "SplitMateAppComposable.kt must not contain pendingOtpCodeForBanner")

        // 3. Verify TRAI DLT-safe P2P SMS payload formatting
        val dltSms = com.splitmate.app.data.PhoneOtpAuthManager.formatDltSafeSyncSmsMessage("482910")
        assertEquals("SplitMate trip sync key: 482-910 (valid 5m)", dltSms)
        assertFalse(dltSms.contains("OTP", ignoreCase = true), "DLT-safe SMS must not contain 'OTP' A2P trigger keyword")
        assertFalse(dltSms.contains("verification code", ignoreCase = true), "DLT-safe SMS must not contain 'verification code'")

        // 4. Verify phone-number-swap attack rejection
        com.splitmate.app.data.PhoneOtpAuthManager.clearPendingOtpChallenge(null)
        val dispatch = com.splitmate.app.data.PhoneOtpAuthManager.sendOtp(null, "9876543210")
        assertNotNull(dispatch)
        val validCode = com.splitmate.app.data.PhoneOtpAuthManager.peekLastGeneratedOtpForTestOnly()!!
        assertFalse(
            com.splitmate.app.data.PhoneOtpAuthManager.verifyOtp(
                rawPhone = "9123456789",
                enteredCode = validCode,
                nowEpochMs = dispatch!!.expiresAtEpochMs - 10_000L
            ),
            "HMAC-SHA256 OTP bound to 9876543210 must be rejected if caller swaps phone to 9123456789"
        )

        // 5. Verify 5-attempt brute-force lockout destroys the active challenge
        repeat(5) { attemptIdx ->
            assertFalse(
                com.splitmate.app.data.PhoneOtpAuthManager.verifyOtp(
                    rawPhone = "9876543210",
                    enteredCode = "00000$attemptIdx",
                    nowEpochMs = dispatch.expiresAtEpochMs - 10_000L
                ),
                "Wrong OTP attempt #${attemptIdx + 1} must fail"
            )
        }
        assertFalse(
            com.splitmate.app.data.PhoneOtpAuthManager.verifyOtp(
                rawPhone = "9876543210",
                enteredCode = validCode,
                nowEpochMs = dispatch.expiresAtEpochMs - 5_000L
            ),
            "After 5 failed attempts, even the genuine OTP code must be rejected due to brute-force lockout"
        )

        // 6. Verify 8-field CloudUserProfileRecord round-trip preserving 4-token avatarSeed + local member avatar preservation
        val canonical4Token = com.splitmate.app.ui.AvatarSeedCodec.encode(
            seedKey = "Akshay_999",
            gender = com.splitmate.app.ui.AvatarGender.MALE,
            styleId = "adventurer",
            colorPresetId = "Electric"
        )
        val profileRecord = com.splitmate.app.data.CloudUserProfileRecord(
            phone10 = "9876543210",
            name = "Akshay Karadkar",
            handle = "akshaykaradkar",
            upiVpa = "9876543210@upi",
            avatarStyle = "adventurer",
            avatarColorPreset = "Electric",
            pinHash = com.splitmate.app.data.PhoneOtpAuthManager.getOrCreateDeviceOwnershipToken(null, "9876543210"),
            updatedAtEpochMs = 1790000000000L,
            avatarSeed = canonical4Token
        )
        val encodedProfileJson = com.splitmate.app.data.CloudGroupSyncRepository.encodeUserProfileRecord(profileRecord)
        val decodedProfile = com.splitmate.app.data.CloudGroupSyncRepository.decodeUserProfileRecord(encodedProfileJson)
        assertNotNull(decodedProfile)
        assertEquals(canonical4Token, decodedProfile!!.avatarSeed, "CloudUserProfileRecord must preserve full 4-token avatarSeed")
        assertEquals("adventurer", decodedProfile.avatarStyle)
        assertEquals("Electric", decodedProfile.avatarColorPreset)
    }

    @Test
    @DisplayName("17. Phase 3 Guard 6: Adversarial Cooldown & Lockout Enforcement")
    fun testPhase3Guard6AdversarialCooldownAndLockoutEnforcement() {
        // 1. Verify 30-second cooldown on sendOtp
        com.splitmate.app.data.PhoneOtpAuthManager.clearPendingOtpChallenge(null)
        val initialDispatch = com.splitmate.app.data.PhoneOtpAuthManager.sendOtp(null, "9876543210")
        assertNotNull(initialDispatch, "Initial sendOtp should succeed")

        val immediateResend = com.splitmate.app.data.PhoneOtpAuthManager.sendOtp(null, "9876543210")
        assertEquals(null, immediateResend, "Immediate resend within 30s cooldown must fail and return null")

        // 2. Clear challenge to simulate cooldown expiry
        com.splitmate.app.data.PhoneOtpAuthManager.clearPendingOtpChallenge(null)

        // 3. Verify brute-force lockout prevents new OTP generation
        val secondDispatch = com.splitmate.app.data.PhoneOtpAuthManager.sendOtp(null, "9876543210")
        assertNotNull(secondDispatch)

        // Simulate 5 failed attempts
        repeat(5) {
            com.splitmate.app.data.PhoneOtpAuthManager.verifyOtp(
                rawPhone = "9876543210",
                enteredCode = "00000$it",
                nowEpochMs = secondDispatch!!.expiresAtEpochMs - 10_000L
            )
        }

        // Now the user is locked out, the challenge hash was destroyed but attempts/expiry remain
        val lockoutResend = com.splitmate.app.data.PhoneOtpAuthManager.sendOtp(null, "9876543210")
        assertEquals(null, lockoutResend, "sendOtp must fail when user is in 5-attempt brute-force lockout")
        com.splitmate.app.data.PhoneOtpAuthManager.clearPendingOtpChallenge(null)
    }

    @Test
    @DisplayName("18. Phase 3 Guard 7: Google Play Protect Enhanced Fraud Protection Compliance (Pixel 9a Sideload Safety)")
    fun testPhase3Guard7GooglePlayProtectEnhancedFraudProtectionComplianceOnPixel9a() {
        val srcMain = resolveSrcMainDir()
        val manifestText = java.io.File(srcMain, "AndroidManifest.xml").readText(Charsets.UTF_8)
        val forbiddenPlayProtectPermissions = listOf(
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_SMS",
            "android.permission.READ_PHONE_NUMBERS",
            "android.permission.READ_PHONE_STATE",
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
        )
        for (perm in forbiddenPlayProtectPermissions) {
            assertFalse(
                manifestText.contains(perm),
                "AndroidManifest.xml must NEVER declare '$perm' because Google Play Protect Enhanced Fraud Protection on Pixel 9a (Android 15) blocks sideloaded APKs with restricted permissions"
            )
        }
    }

    @Test
    @DisplayName("19. Phase 3 Guard 8: Zero Local Notification OTP Leak, Google SIM Verification Token Binding & End-to-End Avatar Sync")
    fun testPhase3Guard8ZeroNotificationOtpLeakGoogleSimTokenAndEndToEndAvatarSync() {
        val srcMain = resolveSrcMainDir()
        val authSource = java.io.File(srcMain, "java/com/splitmate/app/data/PhoneOtpAuthManager.kt").readText(Charsets.UTF_8)
        assertFalse(
            authSource.contains("dispatchSystemOtpNotification"),
            "PhoneOtpAuthManager.kt must NEVER display OTPs in a local system notification on an unverified device"
        )
        assertTrue(
            authSource.contains("GetPhoneNumberHintIntentRequest"),
            "PhoneOtpAuthManager.kt must integrate Google Play Services PhoneNumberHint OS SIM verification"
        )

        // Verify hardware SIM token binds strictly to the OS-verified SIM number
        com.splitmate.app.data.PhoneOtpAuthManager.clearPendingOtpChallenge(null)
        val token = com.splitmate.app.data.PhoneOtpAuthManager.issueHardwareSimVerifiedToken(null, "+91 98765 43210")
        assertNotNull(token, "issueHardwareSimVerifiedToken must succeed for valid OS-verified SIM number")
        assertFalse(
            com.splitmate.app.data.PhoneOtpAuthManager.verifyOtp("9123456789", token!!),
            "Hardware SIM token must fail if phone number is swapped"
        )
        assertTrue(
            com.splitmate.app.data.PhoneOtpAuthManager.verifyOtp("9876543210", token),
            "Hardware SIM token must verify cleanly for the OS-verified SIM number"
        )

        // Verify Option 1 is4DigitPinHash distinguishes 4-digit PIN hashes from legacy 256-bit random tokens
        val fourDigitPinHash = com.splitmate.app.data.PhoneOtpAuthManager.hashPin("9876543210", "4829")
        val legacyRandomTokenHash = com.splitmate.app.data.PhoneOtpAuthManager.getOrCreateDeviceOwnershipToken(null, "9876543210")
        assertTrue(
            com.splitmate.app.data.PhoneOtpAuthManager.is4DigitPinHash("9876543210", fourDigitPinHash),
            "is4DigitPinHash must return true for a 4-digit PIN hash"
        )
        assertFalse(
            com.splitmate.app.data.PhoneOtpAuthManager.is4DigitPinHash("9876543210", legacyRandomTokenHash),
            "is4DigitPinHash must return false for a legacy 256-bit random device ownership token hash"
        )

        // Verify end-to-end 4-token avatar sync across mergeGroupLedgerDocuments even when member userPhone is blank
        val customSeed = com.splitmate.app.ui.AvatarSeedCodec.encode(
            seedKey = "Explorer_888",
            gender = com.splitmate.app.ui.AvatarGender.MALE,
            styleId = "toon-head",
            colorPresetId = "Sunrise"
        )
        val localDoc = com.splitmate.app.data.CloudGroupLedgerDocument(
            group = com.splitmate.app.data.ExpenseGroupEntity("g_sync", "Sync Crew", "INR"),
            members = listOf(
                com.splitmate.app.data.GroupMemberEntity(
                    memberId = "m_me",
                    groupId = "g_sync",
                    name = "Akshay",
                    avatarSeed = "Akshay|open-peeps|PastelWall",
                    isCurrentUser = true,
                    userPhone = ""
                ),
                com.splitmate.app.data.GroupMemberEntity(
                    memberId = "m_friend",
                    groupId = "g_sync",
                    name = "Rohan",
                    avatarSeed = "Rohan|Male|adventurer|Electric",
                    isCurrentUser = false,
                    userPhone = "9123456789"
                )
            ),
            expenses = emptyList(),
            splits = emptyList(),
            settlements = emptyList(),
            deletedExpenseIds = emptyMap(),
            flightVaultByPnr = emptyMap(),
            trainSnapshotByPnr = emptyMap(),
            updatedAtEpochMs = 1790000000000L
        )
        val mergedDoc = com.splitmate.app.data.CloudGroupSyncRepository.mergeGroupLedgerDocuments(
            localDoc = localDoc,
            remoteDoc = null,
            localUserPhone10 = "9876543210",
            localUserAvatarSeed = customSeed
        )
        val meMember = mergedDoc.members.first { it.memberId == "m_me" }
        assertTrue(meMember.isCurrentUser, "Local current user member must remain isCurrentUser=true")
        assertEquals(customSeed, meMember.avatarSeed, "Current user member must sync the exact 4-token signup avatarSeed")
    }

    @Test
    @DisplayName("20. Phase 3 Guard 9: Flight Ticket Boarding Pass Reconstruction & Full DeepGreenTrainTicketCard for All Train Legs")
    fun testPhase3Guard9FlightTicketReconstructionAndFullGreenTrainCardForAllLegs() {
        // 1. Verify normalizePnrKey extracts explicit PNR and never matches English words like FLIGHT or INDIGO
        assertEquals(
            "K8M2XP",
            com.splitmate.app.data.PnrNetworkRepository.normalizePnrKey(
                "Flight 6E 5124 IndiGo (BOM-GOI | 28 Sep | 14:30 | 2A, 3A | PNR: K8M2XP)"
            )
        )
        val noPnrTitleNormalized = com.splitmate.app.data.PnrNetworkRepository.normalizePnrKey(
            "Flight IndiGo Mumbai to Goa"
        )
        assertFalse(
            noPnrTitleNormalized == "FLIGHT" || noPnrTitleNormalized == "INDIGO" || noPnrTitleNormalized == "MUMBAI",
            "normalizePnrKey must never return 6-letter English words like FLIGHT/INDIGO/MUMBAI as a PNR"
        )

        // 2. Verify flight expenses with aircraft seats (1A, 2A, 3A) or compact flight numbers (6E5124) are classified as FLIGHT
        val compactFlightTitle = com.splitmate.app.ui.formatTravelExpenseTitle(
            baseCategory = "Flight",
            ticket = com.splitmate.app.ui.ParsedTravelTicket(
                pnr = "K8M2XP",
                trainOrFlightNo = "6E5124",
                trainOrCarrierName = "IndiGo",
                fromStation = "BOM",
                toStation = "GOI",
                departureDate = "28 Sep",
                departureTime = "14:30",
                coachAndSeats = "2A, 3A"
            )
        )
        assertTrue(compactFlightTitle.startsWith("Flight "), "6E5124 must format with 'Flight ' prefix: $compactFlightTitle")
        val parsedCompactFlight = com.splitmate.app.ui.extractTravelTicketFromTitle(compactFlightTitle)
        assertNotNull(parsedCompactFlight, "extractTravelTicketFromTitle must parse compact flight title")
        assertTrue(
            com.splitmate.app.ui.isFlightTicketExpense(compactFlightTitle, parsedCompactFlight),
            "Flight with seats 2A, 3A must be classified as a Flight ticket, never a 2A/3A AC Train coach"
        )

        val flightExpense = com.splitmate.app.data.ExpenseEntity(
            expenseId = "exp_flight_cloud_1",
            groupId = "g_goa",
            title = compactFlightTitle,
            payerId = "m_akshay",
            baseSubtotalCents = 940_000L,
            taxCents = 0L,
            tipCents = 0L,
            totalAmountCents = 940_000L,
            lockedMultiplier = 1.0,
            unassignedBaseCents = 0L,
            currencyCode = "INR",
            lockedExchangeRate = 1.0,
            expenseCategory = "FLIGHT",
            travelPnr = "K8M2XP",
            providerName = "IndiGo"
        )
        assertEquals(
            com.splitmate.app.ui.screens.TripHubBookingCategory.FLIGHT,
            com.splitmate.app.ui.screens.classifyGroupExpenseForTripHub(flightExpense),
            "Flight expense must be classified as TripHubBookingCategory.FLIGHT"
        )

        // 3. Reconstruct UniversalFlightTicketResult from Cloud-synced ExpenseEntity without local SharedPreferences vault
        val groupMembers = listOf(
            com.splitmate.app.data.GroupMemberEntity("m_akshay", "g_goa", "Akshay", "Akshay", true),
            com.splitmate.app.data.GroupMemberEntity("m_rohan", "g_goa", "Rohan", "Rohan", false)
        )
        val splits = listOf(
            com.splitmate.app.data.ExpenseSplitEntity("s_1", "exp_flight_cloud_1", "m_akshay", 470_000L, 470_000L),
            com.splitmate.app.data.ExpenseSplitEntity("s_2", "exp_flight_cloud_1", "m_rohan", 470_000L, 470_000L)
        )
        val reconstructed = com.splitmate.app.data.PnrNetworkRepository.reconstructFlightTicketFromExpense(
            context = null,
            expense = flightExpense,
            groupMembers = groupMembers,
            allSplits = splits
        )
        assertEquals("K8M2XP", reconstructed.pnr)
        assertEquals("IndiGo", reconstructed.airlineName)
        assertEquals("BOM", reconstructed.originIata)
        assertEquals("Mumbai", reconstructed.originCity)
        assertEquals("GOI", reconstructed.destinationIata)
        assertEquals(940_000L, reconstructed.totalFarePaise)
        assertEquals(2, reconstructed.passengers.size)
        assertEquals("2A", reconstructed.passengers[0].seatNumber)
        assertEquals("3A", reconstructed.passengers[1].seatNumber)

        // 4. Verify TripHomeScreen renders DeepGreenTrainTicketCard for all train legs (no collapsed second train card)
        val srcMain = resolveSrcMainDir()
        val tripHomeSource = java.io.File(srcMain, "java/com/splitmate/app/ui/screens/TripHomeScreen.kt").readText(Charsets.UTF_8)
        assertFalse(
            tripHomeSource.contains("if (isPrimaryLeg)"),
            "TripHomeScreen.kt must render DeepGreenTrainTicketCard for all train legs without hiding leg 2+ behind ReturnTransitTrainCard"
        )
    }

    @Test
    @DisplayName("21. v2.1.3 Guard: 4 Phone Discovery Fixes, Remote-Only Perspective Attribution, Live Online Presence (90s TTL) & Zero UPI UI")
    fun testV213PhoneDiscoveryFixesRemoteOnlyPerspectivePresenceAndZeroUpiUi() = runTest(testDispatcher) {
        // 1. PhoneIdentityValidator.extractMemberPhone10 extracts valid 10-digit mobile from userPhone OR legacy upiId
        assertEquals(
            "9876543210",
            com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10("+91 98765-43210", ""),
            "Must normalize userPhone to 10-digit mobile"
        )
        assertEquals(
            "9123456789",
            com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10("", "9123456789@okaxis"),
            "Must extract 10-digit mobile from legacy upiId when userPhone is blank"
        )
        assertEquals(
            "",
            com.splitmate.app.data.PhoneIdentityValidator.extractMemberPhone10("", "sneha.kulkarni@oksbi"),
            "Non-numeric handle without 10-digit mobile must return empty string"
        )

        // 2. Remote-only group discovery (localDoc == null on Friend B's phone):
        //    Inviter (Akshay, isCurrentUser=true in cloud doc) must NEVER remain isCurrentUser=true on Friend B's phone!
        val nowMs = 1_795_000_000_000L
        val remoteDocFromAkshay = com.splitmate.app.data.CloudGroupLedgerDocument(
            group = com.splitmate.app.data.ExpenseGroupEntity("g_ladakh", "Ladakh Bike Expedition", "INR"),
            members = listOf(
                com.splitmate.app.data.GroupMemberEntity(
                    memberId = "m_akshay",
                    groupId = "g_ladakh",
                    name = "Akshay",
                    avatarSeed = "Akshay|Male|adventurer|Buckwheat",
                    isCurrentUser = true,
                    upiId = "9876543210@upi",
                    userPhone = "9876543210",
                    inviteStatus = "JOINED"
                ),
                com.splitmate.app.data.GroupMemberEntity(
                    memberId = "m_rohan",
                    groupId = "g_ladakh",
                    name = "Rohan",
                    avatarSeed = "Rohan|Male|open-peeps|Terracotta",
                    isCurrentUser = false,
                    upiId = "9123456789@ybl",
                    userPhone = "", // Stored only in legacy upiId before fix
                    inviteStatus = "PENDING"
                )
            ),
            expenses = emptyList(),
            splits = emptyList(),
            settlements = emptyList(),
            deletedExpenseIds = emptyMap(),
            flightVaultByPnr = emptyMap(),
            trainSnapshotByPnr = emptyMap(),
            updatedAtEpochMs = nowMs,
            memberPresenceByPhone = mapOf(
                "9876543210" to (nowMs - 15_000L), // Akshay active 15s ago -> ONLINE
                "9988776655" to (nowMs - 120_000L) // Expired 120s ago -> OFFLINE
            )
        )

        val mergedOnRohanNewPhone = com.splitmate.app.data.CloudGroupSyncRepository.mergeGroupLedgerDocuments(
            localDoc = null,
            remoteDoc = remoteDocFromAkshay,
            localUserPhone10 = "9123456789",
            localPresenceEpochMs = nowMs
        )

        val akshayOnRohanPhone = mergedOnRohanNewPhone.members.first { it.memberId == "m_akshay" }
        val rohanOnRohanPhone = mergedOnRohanNewPhone.members.first { it.memberId == "m_rohan" }
        assertFalse(
            akshayOnRohanPhone.isCurrentUser,
            "Remote inviter Akshay must NOT be marked isCurrentUser=true when imported onto Rohan's phone (localDoc == null)"
        )
        assertTrue(
            rohanOnRohanPhone.isCurrentUser,
            "Invited friend Rohan must be bound as isCurrentUser=true via extractMemberPhone10 even if phone was in upiId"
        )
        assertEquals(
            "9123456789",
            rohanOnRohanPhone.userPhone,
            "Rohan's userPhone must be normalized to 10-digit mobile during merge"
        )

        // 3. Verify Live Online Presence 90s TTL and JSON round-trip
        val encodedJson = com.splitmate.app.data.CloudGroupSyncRepository.encodeGroupLedgerDocument(mergedOnRohanNewPhone)
        val roundTripped = com.splitmate.app.data.CloudGroupSyncRepository.decodeGroupLedgerDocument(encodedJson)!!
        assertTrue(
            com.splitmate.app.data.CloudGroupSyncRepository.isPhoneOnlineNow(
                phone10 = "9876543210",
                presenceMap = roundTripped.memberPresenceByPhone,
                nowEpochMs = nowMs
            ),
            "Akshay (heartbeat 15s ago) must be reported online within 90s TTL"
        )
        assertTrue(
            com.splitmate.app.data.CloudGroupSyncRepository.isPhoneOnlineNow(
                phone10 = "9123456789",
                presenceMap = roundTripped.memberPresenceByPhone,
                nowEpochMs = nowMs
            ),
            "Rohan (just synced at nowMs) must be reported online"
        )
        assertFalse(
            com.splitmate.app.data.CloudGroupSyncRepository.isPhoneOnlineNow(
                phone10 = "9988776655",
                presenceMap = roundTripped.memberPresenceByPhone,
                nowEpochMs = nowMs
            ),
            "Member with heartbeat 120s ago (>90s TTL) must be reported offline"
        )

        // 4. Verify Offline-First Onboarding & Member Phone Editing in SplitMateViewModel
        val offlineVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        offlineVm.completeOnboarding(
            name = "Akshay",
            countryName = "India",
            currencyCode = "INR",
            currencySymbol = "₹",
            avatarSeed = "Akshay|Male|open-peeps|Buckwheat",
            userPhone = "9876543210"
        )
        offlineVm.createNewGroup(
            name = "Spiti Offline Circuit",
            currencyCode = "INR",
            friendNamesCsv = "Rohan, 9123456789"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val spitiGroupId = offlineVm.uiState.value.activeGroupId
        val spitiMembers = offlineVm.uiState.value.members.filter { it.groupId == spitiGroupId }
        val rohanMember = spitiMembers.first { it.name == "Rohan" }
        offlineVm.updateFriendUpi(rohanMember.memberId, "Rohan", "+91 97654 32109", "Rohan|Male|adventurer|Terracotta")
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedRohan = offlineVm.uiState.value.members.first { it.memberId == rohanMember.memberId }
        assertEquals(
            "9765432109",
            updatedRohan.userPhone,
            "Editing a member's phone number must populate normalized 10-digit userPhone for cloud group discovery"
        )
        assertEquals(
            "PENDING",
            updatedRohan.inviteStatus,
            "Adding a 10-digit phone to a member must set inviteStatus to PENDING so their invite appears on join"
        )

        // 5. Verify UPI UI strings are completely removed from UI screens
        val srcMain = resolveSrcMainDir()
        val onboardingScreens = java.io.File(srcMain, "java/com/splitmate/app/ui/screens/OnboardingAndSettingsScreens.kt").readText(Charsets.UTF_8)
        val quickExpenseScreens = java.io.File(srcMain, "java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt").readText(Charsets.UTF_8)
        val tripHomeScreens = java.io.File(srcMain, "java/com/splitmate/app/ui/screens/TripHomeScreen.kt").readText(Charsets.UTF_8)
        assertFalse(onboardingScreens.contains("Your UPI ID"), "OnboardingAndSettingsScreens.kt must not contain 'Your UPI ID'")
        assertFalse(quickExpenseScreens.contains("\"+UPI\""), "QuickExpenseAndGuideScreens.kt must not render '+UPI' badge")
        assertFalse(tripHomeScreens.contains("Pay via UPI"), "TripHomeScreen.kt must not render 'Pay via UPI' button")
    }

    @Test
    @DisplayName("22. v2.1.4 Guard: People Tab Identity Preservation, Online Status Accuracy, O(1) Schedule Cache & Deterministic Activity Order")
    fun testV214RcaBugFixesPeopleIdentityScrollPerfAndDeterministicOrder() = runTest(testDispatcher) {
        val srcMain = resolveSrcMainDir()
        val tripHomeSource = java.io.File(srcMain, "java/com/splitmate/app/ui/screens/TripHomeScreen.kt").readText(Charsets.UTF_8)
        val daoSource = java.io.File(srcMain, "java/com/splitmate/app/data/SplitMateDao.kt").readText(Charsets.UTF_8)

        // 1. Bug 1 Guard: TripHomeScreen.kt (TripHubPeoplePerspectiveView) must NOT call claimGroupMemberPerspective on row click
        assertFalse(
            tripHomeSource.contains("claimGroupMemberPerspective"),
            "TripHomeScreen.kt must NEVER call claimGroupMemberPerspective when clicking a member row in the People tab"
        )

        // 2. Bug 1 Guard: SplitMateUiState.isMemberOnline must NEVER mark an offline friend online just because isCurrentUser == true
        val nowMs = 1_800_000_000_000L
        val uiState = com.splitmate.app.ui.SplitMateUiState(
            userPhone = "9876543210",
            memberPresenceByPhone = mapOf(
                "9123456789" to (nowMs - 10_000L), // Active 10s ago -> ONLINE
                "9988776655" to (nowMs - 150_000L) // Active 150s ago -> OFFLINE (>90s TTL)
            )
        )
        val ownerMember = com.splitmate.app.data.GroupMemberEntity(
            memberId = "g1_me",
            groupId = "g1",
            name = "Akshay",
            avatarSeed = "Akshay|Male|open-peeps|Buckwheat",
            isCurrentUser = false, // Even if perspective temporarily inspected someone else
            userPhone = "9876543210"
        )
        val inspectedOfflineFriendNoPhone = com.splitmate.app.data.GroupMemberEntity(
            memberId = "g1_f0",
            groupId = "g1",
            name = "Vikram",
            avatarSeed = "Vikram|Male|adventurer|Terracotta",
            isCurrentUser = true, // Temporarily inspected perspective!
            userPhone = ""
        )
        val inspectedOfflineFriendExpiredPhone = com.splitmate.app.data.GroupMemberEntity(
            memberId = "g1_f1",
            groupId = "g1",
            name = "Priya",
            avatarSeed = "Priya|Female|lorelei|Sage",
            isCurrentUser = true, // Temporarily inspected perspective!
            userPhone = "9988776655"
        )
        val onlineFriend = com.splitmate.app.data.GroupMemberEntity(
            memberId = "g1_f2",
            groupId = "g1",
            name = "Rohan",
            avatarSeed = "Rohan|Male|micah|Periwinkle",
            isCurrentUser = false,
            userPhone = "9123456789"
        )

        assertTrue(
            uiState.isMemberOnline(ownerMember, nowMs),
            "True device owner (matching 10-digit userPhone) must always be reported online"
        )
        assertFalse(
            uiState.isMemberOnline(inspectedOfflineFriendNoPhone, nowMs),
            "Offline friend without phone must NEVER be reported online even when isCurrentUser == true"
        )
        assertFalse(
            uiState.isMemberOnline(inspectedOfflineFriendExpiredPhone, nowMs),
            "Friend with expired presence (>90s TTL) must NEVER be reported online even when isCurrentUser == true"
        )
        assertTrue(
            uiState.isMemberOnline(onlineFriend, nowMs),
            "Friend with active presence heartbeat within 90s TTL must be reported online"
        )

        // 3. Bug 1 Guard: Perspective inspection + profile update must NEVER overwrite friend's avatarSeed or userPhone
        val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        vm.completeOnboarding(
            name = "Akshay",
            countryName = "India",
            currencyCode = "INR",
            currencySymbol = "₹",
            avatarSeed = "Akshay|Male|open-peeps|Buckwheat",
            userPhone = "9876543210"
        )
        vm.createNewGroup(
            name = "Gokarna Trek",
            currencyCode = "INR",
            friendNamesCsv = "Sneha, Rohan"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val gId = vm.uiState.value.activeGroupId
        val snehaBefore = vm.uiState.value.members.first { it.groupId == gId && it.name == "Sneha" }
        val originalSnehaSeed = snehaBefore.avatarSeed

        // Simulate temporary perspective switch via TripSyncAndPerspectiveSheet
        vm.claimGroupMemberPerspective(gId, snehaBefore.memberId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Now update local owner's profile & avatar settings
        vm.updateUserProfile(
            newName = "Akshay K",
            newPhone = "9876543210",
            newSeedOrCurrency = "adventurer|Terracotta|Male|AkshayK",
            newUpiId = "9876543210@upi"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val snehaAfter = vm.uiState.value.members.first { it.memberId == snehaBefore.memberId }
        val akshayAfter = vm.uiState.value.members.first { it.groupId == gId && it.memberId.endsWith("_me") }
        assertEquals(
            "Sneha",
            snehaAfter.name,
            "Friend Sneha's name must NEVER be overwritten with local user's name after perspective inspection"
        )
        assertEquals(
            originalSnehaSeed,
            snehaAfter.avatarSeed,
            "Friend Sneha's avatarSeed must NEVER be overwritten with local user's avatarSeed"
        )
        assertEquals(
            "",
            snehaAfter.userPhone,
            "Friend Sneha's blank userPhone must NEVER be overwritten with local user's phone"
        )
        assertEquals(
            "Akshay K",
            akshayAfter.name,
            "True device owner's member row must receive the updated profile name"
        )

        // 4. Bug 2 Guard: O(1) memoization of resolveExpenseSchedule & reconstructFlightTicketFromExpense
        val sampleFlightExpense = com.splitmate.app.data.ExpenseEntity(
            expenseId = "exp_flight_1",
            groupId = gId,
            title = "Flight 6E 204 (BOM -> GOI) [PNR:A1B2C3] | 28 Sep 2026 09:30 AM",
            payerId = akshayAfter.memberId,
            baseSubtotalCents = 840_000L,
            taxCents = 0L,
            tipCents = 0L,
            totalAmountCents = 840_000L,
            lockedMultiplier = 1.0,
            unassignedBaseCents = 0L,
            currencyCode = "INR",
            lockedExchangeRate = 1.0,
            createdAt = 1_790_000_000_000L,
            expenseCategory = "FLIGHT",
            travelPnr = "A1B2C3"
        )
        val sched1 = com.splitmate.app.ui.screens.resolveExpenseSchedule(null, sampleFlightExpense)
        val sched2 = com.splitmate.app.ui.screens.resolveExpenseSchedule(null, sampleFlightExpense)
        org.junit.jupiter.api.Assertions.assertSame(
            sched1,
            sched2,
            "resolveExpenseSchedule must return the memoized ResolvedExpenseSchedule instance in O(1) without re-running Regex/SimpleDateFormat"
        )

        val groupMembers = vm.uiState.value.members.filter { it.groupId == gId }
        val flight1 = com.splitmate.app.data.PnrNetworkRepository.reconstructFlightTicketFromExpense(
            context = null,
            expense = sampleFlightExpense,
            groupMembers = groupMembers,
            allSplits = emptyList()
        )
        val flight2 = com.splitmate.app.data.PnrNetworkRepository.reconstructFlightTicketFromExpense(
            context = null,
            expense = sampleFlightExpense,
            groupMembers = groupMembers,
            allSplits = emptyList()
        )
        org.junit.jupiter.api.Assertions.assertSame(
            flight1,
            flight2,
            "reconstructFlightTicketFromExpense must return the memoized UniversalFlightTicketResult instance on repeated scroll frames"
        )

        // 5. Bug 3 Guard: Deterministic Chronological Expense Ordering (ORDER BY createdAt DESC, expenseId DESC)
        assertTrue(
            daoSource.contains("SELECT * FROM expenses ORDER BY createdAt DESC, expenseId DESC"),
            "SplitMateDao.observeAllExpenses() must enforce ORDER BY createdAt DESC, expenseId DESC"
        )
        assertTrue(
            daoSource.contains("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY createdAt DESC, expenseId DESC"),
            "SplitMateDao.getExpensesForGroup() must enforce ORDER BY createdAt DESC, expenseId DESC"
        )
        assertTrue(
            daoSource.contains("SELECT * FROM expense_groups ORDER BY createdAt DESC, groupId ASC"),
            "SplitMateDao.getAllGroups() must enforce ORDER BY createdAt DESC, groupId ASC"
        )
    }

    @Test
    @DisplayName("23. Tier 1 (F1 & F5): PhoneIdentityValidator [6-9] validation and 6-Character Crockford Base32 Group Join Code algebra")
    fun testPhoneIdentityValidatorAndSixCharJoinCodeAlgebra() = runTest(testDispatcher) {
        val validator = com.splitmate.app.data.PhoneIdentityValidator
        val syncRepo = com.splitmate.app.data.CloudGroupSyncRepository

        // F1: Indian 10-digit normalization & [6-9] validation
        assertEquals("9876543210", validator.normalizeTo10DigitIndianMobile("+91 98765-43210"))
        assertEquals("9876543210", validator.normalizeTo10DigitIndianMobile("09876543210"))
        assertEquals("9876543210", validator.normalizeTo10DigitIndianMobile("919876543210"))
        assertEquals("9876543210", validator.normalizeTo10DigitIndianMobile("9876543210@okaxis"))
        assertEquals("", validator.normalizeTo10DigitIndianMobile("1234567890"), "Numbers starting with 1-5 must be rejected")
        assertEquals("", validator.normalizeTo10DigitIndianMobile("98765"), "Short numbers must be rejected")
        assertEquals("", validator.normalizeTo10DigitIndianMobile("akshay.karadkar@okicici"), "Non-phone VPAs must return empty phone")

        // F1: extractMemberPhone10 must validate primary userPhone via [6-9] before returning, falling back to upiId
        assertEquals(
            "9876543210",
            validator.extractMemberPhone10("1234567890", "9876543210@okicici"),
            "Invalid primary userPhone must fall back to valid 10-digit phone in upiId"
        )
        assertEquals(
            "",
            validator.extractMemberPhone10("1234567890", "akshay@okicici"),
            "Both invalid primary and non-numeric VPA must yield empty phone"
        )

        // F5 / PA-7: 6-character Crockford Base32 Join Code generation & formatting
        val code6 = syncRepo.deriveGroupJoinCode6("grp_goa_test", 1_790_000_000_000L)
        assertEquals(6, code6.length)
        val allowedAlphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toSet()
        assertTrue(code6.all { it in allowedAlphabet }, "Join code must only contain unambiguous Crockford Base32 chars: $code6")
        assertEquals("${code6.substring(0, 3)}-${code6.substring(3, 6)}", syncRepo.formatJoinCode6(code6))
        assertEquals(code6, syncRepo.normalizeJoinCode6(syncRepo.formatJoinCode6(code6).lowercase()))

        // Verify createNewGroupWithContacts embeds _<CODE6> suffix in groupId (PA-7)
        val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        vm.completeOnboarding(
            name = "Akshay",
            countryName = "India",
            currencyCode = "INR",
            currencySymbol = "₹",
            avatarSeed = "Akshay",
            userPhone = "9876543210"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.createNewGroupWithContacts(
            name = "Goa Beach Villa",
            iconName = "Flight",
            memberDrafts = listOf(com.splitmate.app.ui.NewGroupMemberDraft(name = "Rahul", cleanPhone = "9876543211"))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val gId = vm.uiState.value.activeGroupId
        val embeddedCode = vm.getGroupJoinCode(gId)
        assertEquals(6, embeddedCode.length)
        assertTrue(gId.endsWith("_$embeddedCode"), "Created groupId ($gId) must embed _$embeddedCode suffix for PA-7 discovery")
        assertEquals(syncRepo.formatJoinCode6(embeddedCode), vm.getFormattedGroupJoinCode(gId))

        val bundle = vm.exportGroupSyncPayload(gId)
        assertNotNull(bundle)
        assertEquals(embeddedCode, bundle!!.joinCode6)
        assertTrue(bundle.whatsappShareText.contains("Trip Join Code: ${syncRepo.formatJoinCode6(embeddedCode)}"))
        assertFalse(containsUnicodeEmoji(bundle.whatsappShareText))
    }

    @Test
    @DisplayName("24. Tier 2 (F2, F3, F5, PA-1, PA-3, PA-6): Structural ledger hash invariance and CRDT tombstone merge with subset-only split redistribution")
    fun testStructuralLedgerHashAndCrdtTombstoneMerge() = runTest(testDispatcher) {
        val syncRepo = com.splitmate.app.data.CloudGroupSyncRepository
        val group = com.splitmate.app.data.ExpenseGroupEntity(
            groupId = "grp_manali_K7M9P2",
            name = "Manali Snow Expedition",
            currencyCode = "INR",
            iconName = "Flight",
            createdAt = 1_790_000_000_000L
        )
        val mAkshay = com.splitmate.app.data.GroupMemberEntity(
            memberId = "m_akshay",
            groupId = group.groupId,
            name = "Akshay",
            avatarSeed = "Akshay",
            upiId = "9876543210@okaxis",
            isCurrentUser = true,
            userPhone = "9876543210",
            inviteStatus = "JOINED"
        )
        val mRahul = com.splitmate.app.data.GroupMemberEntity(
            memberId = "m_rahul",
            groupId = group.groupId,
            name = "Rahul",
            avatarSeed = "Rahul",
            upiId = "9876543211@ybl",
            isCurrentUser = false,
            userPhone = "9876543211",
            inviteStatus = "PENDING"
        )
        val mPriya = com.splitmate.app.data.GroupMemberEntity(
            memberId = "m_priya",
            groupId = group.groupId,
            name = "Priya",
            avatarSeed = "Priya",
            upiId = "9876543212@okicici",
            isCurrentUser = false,
            userPhone = "9876543212",
            inviteStatus = "JOINED"
        )
        val mKaran = com.splitmate.app.data.GroupMemberEntity(
            memberId = "m_karan",
            groupId = group.groupId,
            name = "Karan",
            avatarSeed = "Karan",
            upiId = "9876543213@upi",
            isCurrentUser = false,
            userPhone = "9876543213",
            inviteStatus = "JOINED"
        )

        // Expense 1: 10,000 cents paid by Rahul, split ONLY among Akshay, Rahul, Priya (Karan excluded!)
        val exp1 = com.splitmate.app.data.ExpenseEntity(
            expenseId = "exp_1",
            groupId = group.groupId,
            title = "Ski Gear Rental",
            payerId = "m_rahul",
            baseSubtotalCents = 10_000L,
            taxCents = 0L,
            tipCents = 0L,
            totalAmountCents = 10_000L,
            lockedMultiplier = 1.0,
            unassignedBaseCents = 0L,
            currencyCode = "INR",
            lockedExchangeRate = 1.0,
            createdAt = 1_790_000_100_000L
        )
        val splits1 = listOf(
            com.splitmate.app.data.ExpenseSplitEntity("sp_1_a", "exp_1", "m_akshay", 3334L, 3334L, true),
            com.splitmate.app.data.ExpenseSplitEntity("sp_1_r", "exp_1", "m_rahul", 3333L, 3333L, false),
            com.splitmate.app.data.ExpenseSplitEntity("sp_1_p", "exp_1", "m_priya", 3333L, 3333L, false)
        )

        val docA = com.splitmate.app.data.CloudGroupLedgerDocument(
            group = group,
            members = listOf(mAkshay, mRahul, mPriya, mKaran),
            expenses = listOf(exp1),
            splits = splits1,
            settlements = emptyList(),
            updatedAtEpochMs = 1000L,
            organizerPhone10 = "9876543210",
            joinCode6 = "K7M9P2"
        )
        val docAReordered = docA.copy(
            members = listOf(mKaran, mPriya, mRahul, mAkshay),
            updatedAtEpochMs = 999999L
        )

        // F2: Structural hash must be identical regardless of updatedAtEpochMs or list order
        assertEquals(
            syncRepo.computeStructuralLedgerHash(docA),
            syncRepo.computeStructuralLedgerHash(docAReordered),
            "Structural hash must be invariant to updatedAtEpochMs and member list ordering"
        )

        // Changing inviteStatus must change structural hash
        val docAJoined = docA.copy(
            members = listOf(mAkshay, mRahul.copy(inviteStatus = "JOINED"), mPriya, mKaran)
        )
        org.junit.jupiter.api.Assertions.assertNotEquals(
            syncRepo.computeStructuralLedgerHash(docA),
            syncRepo.computeStructuralLedgerHash(docAJoined),
            "Structural hash must detect inviteStatus transitions"
        )

        // F5: Wire JSON round-trip with tombstones, organizerPhone10, and joinCode6
        val docRemoteWithRemoval = docA.copy(
            members = listOf(mAkshay, mPriya, mKaran),
            deletedMemberIds = mapOf("m_rahul" to 2000L),
            removedMemberPhones = mapOf("9876543211" to 2000L),
            deletedSettlementIds = mapOf("st_old" to 2000L),
            updatedAtEpochMs = 2000L
        )
        val json = syncRepo.encodeGroupLedgerDocument(docRemoteWithRemoval)
        val decoded = syncRepo.decodeGroupLedgerDocument(json)
        assertNotNull(decoded)
        assertEquals("9876543210", decoded!!.organizerPhone10)
        assertEquals("K7M9P2", decoded.joinCode6)
        assertEquals(setOf("m_rahul"), decoded.deletedMemberIds.keys)
        assertEquals(setOf("9876543211"), decoded.removedMemberPhones.keys)
        assertEquals(setOf("st_old"), decoded.deletedSettlementIds.keys)

        // Merge docA (stale local still having Rahul) with decoded (remote having tombstone for Rahul)
        val merged = syncRepo.mergeGroupLedgerDocuments(
            localDoc = docA,
            remoteDoc = decoded,
            localUserPhone10 = "9876543210"
        )
        assertEquals(3, merged.members.size, "Tombstoned member m_rahul must be purged during CRDT merge")
        assertFalse(merged.members.any { it.memberId == "m_rahul" })

        // PA-1: Expense 1 payerId (was m_rahul) must be reassigned to Organizer (m_akshay)
        val mergedExp1 = merged.expenses.single()
        assertEquals("m_akshay", mergedExp1.payerId, "Orphaned expense payerId must fall back to Organizer m_akshay (PA-1)")

        // PA-6: Splits on Expense 1 must be redistributed ONLY between Akshay and Priya (5000 + 5000 = 10000), NOT Karan!
        val mergedExp1Splits = merged.splits.filter { it.expenseId == "exp_1" }
        assertEquals(2, mergedExp1Splits.size, "Karan was not in Expense 1 and must NOT be added to its splits (PA-6)")
        assertEquals(setOf("m_akshay", "m_priya"), mergedExp1Splits.map { it.memberId }.toSet())
        assertEquals(10_000L, mergedExp1Splits.sumOf { it.finalOwedCents }, "Redistributed splits must have 0.00c drift")
        assertTrue(mergedExp1Splits.all { it.finalOwedCents == 5_000L })
    }

    @Test
    @DisplayName("25. Tier 3 (F7, F8, PA-1, PA-3, PA-4, PA-6): Organizer Remove Member, RBAC guard, Leave Group, 3-layer purge, and Re-Invite un-tombstoning")
    fun testOrganizerRemoveMemberAndLeaveGroupLifecycle() = runTest(testDispatcher) {
        val syncRepo = com.splitmate.app.data.CloudGroupSyncRepository
        val orgVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        orgVm.completeOnboarding(
            name = "Akshay",
            countryName = "India",
            currencyCode = "INR",
            currencySymbol = "₹",
            avatarSeed = "Akshay",
            userPhone = "9876543210"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        orgVm.createNewGroupWithContacts(
            name = "Coorg Coffee Estate",
            iconName = "Flight",
            memberDrafts = listOf(
                com.splitmate.app.ui.NewGroupMemberDraft("Rahul", "9876543211"),
                com.splitmate.app.ui.NewGroupMemberDraft("Priya", "9876543212"),
                com.splitmate.app.ui.NewGroupMemberDraft("Karan", "9876543213")
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val gId = orgVm.uiState.value.activeGroupId
        val membersBefore = orgVm.uiState.value.members.filter { it.groupId == gId }
        assertEquals(4, membersBefore.size)
        val akshay = membersBefore.first { it.name == "Akshay" }
        val rahul = membersBefore.first { it.name == "Rahul" }
        val priya = membersBefore.first { it.name == "Priya" }
        val karan = membersBefore.first { it.name == "Karan" }

        // Verify Organizer detection
        assertTrue(orgVm.isUserGroupOrganizer(gId, "9876543210"))
        assertFalse(orgVm.isUserGroupOrganizer(gId, "9876543211"))
        assertEquals(akshay.memberId, orgVm.getGroupOrganizerMember(gId)?.memberId)

        // Log an expense paid by Rahul and split ONLY among Akshay, Rahul, Priya (subset of 3 out of 4)
        orgVm.commitQuickEqualExpense(
            title = "Plantation Jeep Safari",
            totalAmountCents = 9_000L,
            selectedMemberIds = listOf(akshay.memberId, rahul.memberId, priya.memberId),
            payerMemberId = rahul.memberId
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Log a settlement involving Rahul
        orgVm.markGreedyTransferSettled(
            SplitMateMathEngine.SimplifiedTransfer(
                fromMemberId = priya.memberId,
                fromName = priya.name,
                toMemberId = rahul.memberId,
                toName = rahul.name,
                amountCents = 1_000L
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, orgVm.uiState.value.settlements.count { it.groupId == gId })

        // Organizer removes Rahul
        orgVm.removeMemberFromGroup(gId, rahul.memberId)
        testDispatcher.scheduler.advanceUntilIdle()

        val stateAfterRemove = orgVm.uiState.value
        val remainingMembers = stateAfterRemove.members.filter { it.groupId == gId }
        assertEquals(3, remainingMembers.size)
        assertFalse(remainingMembers.any { it.memberId == rahul.memberId })

        // PA-1: Expense paid by Rahul must now be reassigned to Organizer Akshay
        val updatedExpense = stateAfterRemove.expenses.single { it.groupId == gId }
        assertEquals(akshay.memberId, updatedExpense.payerId, "Removed payer must fall back to Organizer Akshay (PA-1)")

        // PA-6: Splits must be redistributed ONLY between Akshay and Priya (4500 + 4500 = 9000), excluding Karan
        val updatedSplits = stateAfterRemove.splits.filter { it.expenseId == updatedExpense.expenseId }
        assertEquals(2, updatedSplits.size)
        assertEquals(setOf(akshay.memberId, priya.memberId), updatedSplits.map { it.memberId }.toSet())
        assertFalse(updatedSplits.any { it.memberId == karan.memberId }, "Non-participating member Karan must not be added to subset expense")
        assertEquals(9_000L, updatedSplits.sumOf { it.finalOwedCents })

        // Settlements involving Rahul must be purged
        assertEquals(0, stateAfterRemove.settlements.count { it.groupId == gId })

        // PA-3 & PA-4: Durable tombstone recorded for Rahul's phone (9876543211)
        assertTrue(syncRepo.isPhoneRemovedFromGroup(gId, "9876543211"))

        // Re-adding Rahul via addMemberToActiveGroup must un-tombstone 9876543211
        orgVm.addMemberToActiveGroup("Rahul Rejoined", "9876543211")
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(
            syncRepo.isPhoneRemovedFromGroup(gId, "9876543211"),
            "Re-inviting a previously removed phone must clear its local tombstone"
        )
        assertEquals(4, orgVm.uiState.value.members.count { it.groupId == gId })

        // Non-organizer (Karan, 0.00c balance) leaves the trip on his own device
        val capsule = orgVm.exportGroupSyncPayload(gId)!!.syncToken
        val karanVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        karanVm.completeOnboarding(
            name = "Karan",
            countryName = "India",
            currencyCode = "INR",
            currencySymbol = "₹",
            avatarSeed = "Karan",
            userPhone = "9876543213"
        )
        testDispatcher.scheduler.advanceUntilIdle()
        karanVm.importAndMergeGroupSyncPayload(capsule, openGroupAfterMerge = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(karanVm.uiState.value.groups.any { it.groupId == gId })
        val leaveOk = karanVm.leaveGroup(gId)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(leaveOk, "Member with 0.00c net balance must be allowed to leave group")

        assertFalse(
            karanVm.uiState.value.groups.any { it.groupId == gId },
            "Leaving group must immediately purge group from local state (PA-4)"
        )
        assertFalse(karanVm.uiState.value.activeJoinedGroups.any { it.groupId == gId })
        assertTrue(syncRepo.isPhoneRemovedFromGroup(gId, "9876543213"))
    }

    @Test
    @DisplayName("26. Tier 4 (F6 & PA-8): Edit Member Phone & Resend Invite, Optional PIN Onboarding, and Uninvited Joiner Perspective Isolation")
    fun testUpdateMemberPhoneAndUninvitedJoinerPerspectiveIsolation() = runTest(testDispatcher) {
        val orgVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        orgVm.completeOnboarding(
            name = "Akshay",
            countryName = "India",
            currencyCode = "INR",
            currencySymbol = "₹",
            avatarSeed = "Akshay",
            userPhone = "9876543210"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Optional PIN on first-time account setup: verifyPinAndRestoreCloud with blank PIN must succeed when hasExisting4DigitPin is false
        var pinCallbackSuccess = false
        orgVm.verifyPinAndRestoreCloud(
            context = null,
            rawPhone = "9876543210",
            enteredPin4 = "",
            fallbackUserName = "Akshay"
        ) { ok, _ -> pinCallbackSuccess = ok }
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(pinCallbackSuccess, "Blank PIN must be accepted for first-time account without existing PIN")
        assertTrue(orgVm.uiState.value.isPhoneVerified)

        orgVm.createNewGroupWithContacts(
            name = "Udaipur Palace Retreat",
            iconName = "Flight",
            memberDrafts = listOf(
                com.splitmate.app.ui.NewGroupMemberDraft("Rohan", "9876543211")
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val gId = orgVm.uiState.value.activeGroupId
        val rohanBefore = orgVm.uiState.value.members.first { it.groupId == gId && it.name == "Rohan" }

        // F6: Edit Rohan's phone number and resend invite
        orgVm.updateMemberPhoneAndResendInvite(gId, rohanBefore.memberId, "+91 99887-76655")
        testDispatcher.scheduler.advanceUntilIdle()

        val rohanAfter = orgVm.uiState.value.members.first { it.memberId == rohanBefore.memberId }
        assertEquals("9988776655", rohanAfter.userPhone)
        assertEquals("9988776655@upi", rohanAfter.upiId)

        // Invalid phone edit must be rejected without corrupting existing userPhone
        orgVm.updateMemberPhoneAndResendInvite(gId, rohanBefore.memberId, "12345")
        testDispatcher.scheduler.advanceUntilIdle()
        val rohanStillValid = orgVm.uiState.value.members.first { it.memberId == rohanBefore.memberId }
        assertEquals("9988776655", rohanStillValid.userPhone)

        // PA-8: Uninvited user (Vikram, 9123456789) imports capsule — must NOT hijack Organizer Akshay's row!
        val exportBundle = orgVm.exportGroupSyncPayload(gId)!!
        val vikramVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        vikramVm.completeOnboarding(
            name = "Vikram",
            countryName = "India",
            currencyCode = "INR",
            currencySymbol = "₹",
            avatarSeed = "Vikram",
            userPhone = "9123456789"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val mergeResult = vikramVm.importAndMergeGroupSyncPayload(exportBundle.syncToken, openGroupAfterMerge = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(mergeResult.success)
        assertEquals(3, mergeResult.mergedMemberCount, "Uninvited joiner Vikram must be appended as a 3rd member (PA-8)")
        val vikramGroupMembers = vikramVm.uiState.value.members.filter { it.groupId == gId }
        assertEquals(3, vikramGroupMembers.size)

        val currentUserRows = vikramGroupMembers.filter { it.isCurrentUser }
        assertEquals(1, currentUserRows.size, "Exactly one member row must have isCurrentUser = true")
        assertEquals("Vikram", currentUserRows.single().name, "Vikram must own the local perspective row")
        assertEquals("9123456789", currentUserRows.single().userPhone)
        assertEquals("JOINED", currentUserRows.single().inviteStatus)

        val akshayRowOnVikramDevice = vikramGroupMembers.first { it.name == "Akshay" }
        assertFalse(akshayRowOnVikramDevice.isCurrentUser, "Organizer Akshay's row must have isCurrentUser = false on Vikram's device")
        assertEquals(
            akshayRowOnVikramDevice.memberId,
            vikramVm.getGroupOrganizerMember(gId)?.memberId,
            "Akshay must still be recognized as the Trip Organizer on Vikram's device"
        )
    }

    @Test
    @DisplayName("27. Tier 5 (F9–F14): Milestone 2 Buckwheat UI Surfaces, Contact Picker Manual +91 Entry, Join with Code Modal, People Tab RBAC & Sync Sheet Compliance")
    fun testMilestone2BuckwheatUiSurfacesAndCallbackWiring() = runTest(testDispatcher) {
        val srcMain = resolveSrcMainDir()

        val themeFile = java.io.File(srcMain, "java/com/splitmate/app/ui/SplitMateTheme.kt")
        val themeSource = themeFile.readText()
        assertTrue(
            themeSource.contains("Add by Mobile Number (+91)"),
            "SplitMateTheme.kt ContactPickerBottomSheet must render inline 'Add by Mobile Number (+91)' card (F9)"
        )
        assertTrue(
            themeSource.contains("contact.cleanPhone.startsWith(\"contact_\")"),
            "SplitMateTheme.kt must guard phoneless contacts with contact_ prefix from silent offline addition (F9)"
        )
        assertTrue(
            themeSource.contains("itemsIndexed("),
            "SplitMateTheme.kt must use itemsIndexed with composite key to prevent duplicate phone LazyColumn crashes (F9)"
        )

        val appComposableFile = java.io.File(srcMain, "java/com/splitmate/app/ui/SplitMateAppComposable.kt")
        val appSource = appComposableFile.readText()
        assertTrue(
            appSource.contains("CloudGroupSyncRepository.init(context.applicationContext)"),
            "SplitMateApp must initialize CloudGroupSyncRepository at startup (F10)"
        )
        assertTrue(
            appSource.contains("Save Profile & Find My Trips ->"),
            "SplitMateCloudOtpOnboardingScreen must display unified single-gate primary CTA (F10)"
        )
        assertTrue(
            appSource.contains("Continue in Offline Mode without Cloud Sync"),
            "SplitMateCloudOtpOnboardingScreen must clearly distinguish offline mode secondary action (F10)"
        )
        assertTrue(
            appSource.contains("Join with Code") && appSource.contains("fun JoinGroupByCodeDialog("),
            "LedgersDashboardScreen must expose 'Join with Code' buttons and JoinGroupByCodeDialog (F11)"
        )

        val tripHomeFile = java.io.File(srcMain, "java/com/splitmate/app/ui/screens/TripHomeScreen.kt")
        val tripHomeSource = tripHomeFile.readText()
        assertTrue(
            tripHomeSource.contains("Trip Code: \$formattedJoinCode · Invite"),
            "TripHubPeoplePerspectiveView must display 6-character Trip Code in the top action bar (F12)"
        )
        assertTrue(
            tripHomeSource.contains("\"ORGANIZER\"") &&
                tripHomeSource.contains("+ Add Phone & Invite") &&
                tripHomeSource.contains("Edit Name & Phone") &&
                tripHomeSource.contains("Share Invite on WhatsApp") &&
                tripHomeSource.contains("Your Settlements") &&
                tripHomeSource.contains("Other Travelers' Settlements") &&
                tripHomeSource.contains("Confirmed by each recipient once received") &&
                !tripHomeSource.contains("· Invite sent") &&
                !tripHomeSource.contains("View as \${member.name") &&
                !tripHomeSource.contains("switchActivePerspectiveMember") &&
                tripHomeSource.contains("Remove Member") &&
                tripHomeSource.contains("Leave Trip") &&
                tripHomeSource.contains("selectedMemberForActions") &&
                tripHomeSource.contains("Recipients confirm payments once received · Organizers can settle for offline members") &&
                !tripHomeSource.contains("containerColor = Color(0xFFFAF6F0)") &&
                !tripHomeSource.contains("\"Online now\"") &&
                !tripHomeSource.contains("· Auto-Sync"),
            "TripHubPeoplePerspectiveView and TripHubMoneySettlementView must use M3 Expressive Segmented Contained List, Me-First hierarchy, non-overlapping action sheet rows, and adaptive TripHubTokens (F12)"
        )
        assertTrue(
            appSource.contains("GETS BACK (") &&
                appSource.contains("OWES (") &&
                appSource.contains("Show top 3 rows") &&
                appSource.contains("Your Settlements") &&
                appSource.contains("Other Travelers' Settlements") &&
                !appSource.contains("confirms incoming payments once received"),
            "SettleUpTab must render compact 2-column GETS BACK | OWES split board with 3-row accordion, Me-First settlements, and zero repetitive per-card confirmation footers (F12)"
        )

        val syncSheetFile = java.io.File(srcMain, "java/com/splitmate/app/ui/dialogs/TripSyncAndPerspectiveSheet.kt")
        val syncSheetSource = syncSheetFile.readText()
        assertTrue(
            syncSheetSource.contains("6-CHARACTER TRIP JOIN CODE") &&
                syncSheetSource.contains("Copy Code") &&
                syncSheetSource.contains("Share Invite on WhatsApp") &&
                syncSheetSource.contains("Join Another Trip by Code or Link") &&
                !syncSheetSource.contains("Viewing as (Switch Perspective)") &&
                !syncSheetSource.contains("claimGroupMemberPerspective"),
            "TripSyncAndPerspectiveSheet must render 6-character Trip Join Code card and universal Join by Code/Link without any perspective switcher (F13)"
        )

        // Verify ViewModel callback overload for updateMemberPhoneAndResendInvite & sendOrResendDirectMemberInvite
        val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        vm.completeOnboarding(
            name = "Organizer A",
            countryName = "India",
            currencyCode = "INR",
            currencySymbol = "₹",
            avatarSeed = "OrganizerA",
            userPhone = "9876543210"
        )
        testDispatcher.scheduler.advanceUntilIdle()
        vm.createNewGroupWithContacts(
            name = "Munnar Tea Trail",
            iconName = "Hotel",
            memberDrafts = listOf(com.splitmate.app.ui.NewGroupMemberDraft("Offline Friend", ""))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val gId = vm.uiState.value.activeGroupId
        val friend = vm.uiState.value.members.first { it.groupId == gId && !it.isCurrentUser }
        var callbackOk = false
        var callbackMsg = ""
        vm.updateMemberPhoneAndResendInvite(
            context = null,
            groupId = gId,
            memberId = friend.memberId,
            rawPhone = "+91 91234 56780",
            newName = "Arjun Nair"
        ) { ok, msg ->
            callbackOk = ok
            callbackMsg = msg
        }
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(callbackOk, "updateMemberPhoneAndResendInvite callback must succeed: $callbackMsg")
        val updatedFriend = vm.uiState.value.members.first { it.memberId == friend.memberId }
        assertEquals("Arjun Nair", updatedFriend.name)
        assertEquals("9123456780", updatedFriend.userPhone)
        assertEquals("PENDING", updatedFriend.inviteStatus)

        var directInviteOk = false
        var directInviteMsg = ""
        vm.sendOrResendDirectMemberInvite(
            context = null,
            groupId = gId,
            memberId = friend.memberId,
            launchWhatsAppShare = false
        ) { ok, msg ->
            directInviteOk = ok
            directInviteMsg = msg
        }
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(directInviteOk && directInviteMsg.contains("9123456780"), "sendOrResendDirectMemberInvite must succeed: $directInviteMsg")
    }

    @Test
    @DisplayName("28. Cross-Device Expense Sync: MemberId Remapping (Phone & Name Match) & Monotonic JOINED Status")
    fun testCrossDeviceExpenseMemberIdRemappingAndMonotonicJoinedSync() {
        val group = com.splitmate.app.data.ExpenseGroupEntity(
            groupId = "g_sync_cross_device",
            name = "Gokarna Beach Trek",
            currencyCode = "INR",
            iconName = "Flight",
            isDemoSeed = false,
            createdAt = 1760000000000L
        )

        // Phone A (Akshay Organizer): Rohan has memberId = "m_rohan_local" and inviteStatus = "PENDING"
        val localDocOnPhoneA = com.splitmate.app.data.CloudGroupLedgerDocument(
            group = group,
            members = listOf(
                com.splitmate.app.data.GroupMemberEntity(
                    memberId = "m_akshay_me",
                    groupId = group.groupId,
                    name = "Akshay",
                    avatarSeed = "Akshay|Male|open-peeps|Buckwheat",
                    isCurrentUser = true,
                    upiId = "9876543210@upi",
                    userPhone = "9876543210",
                    inviteStatus = "JOINED"
                ),
                com.splitmate.app.data.GroupMemberEntity(
                    memberId = "m_rohan_local",
                    groupId = group.groupId,
                    name = "Rohan",
                    avatarSeed = "Rohan|Neutral",
                    isCurrentUser = false,
                    upiId = "9123456789@upi",
                    userPhone = "9123456789",
                    inviteStatus = "PENDING"
                )
            ),
            expenses = emptyList(),
            splits = emptyList(),
            settlements = emptyList(),
            updatedAtEpochMs = 1760000500000L,
            organizerPhone10 = "9876543210"
        )

        // Phone B (Rohan): joined with a different generated memberId ("m_rohan_remote_join") and logged a ₹1,500 dinner expense
        val rohanExpense = com.splitmate.app.data.ExpenseEntity(
            expenseId = "exp_rohan_dinner_1",
            groupId = group.groupId,
            title = "Namaste Cafe Beach Dinner",
            payerId = "m_rohan_remote_join",
            baseSubtotalCents = 150_000L,
            taxCents = 0L,
            tipCents = 0L,
            totalAmountCents = 150_000L,
            lockedMultiplier = 1.0,
            unassignedBaseCents = 0L,
            currencyCode = "INR",
            lockedExchangeRate = 1.0,
            expenseCategory = "FOOD",
            syncStatus = "SYNCED",
            createdAt = 1760000400000L
        )
        val rohanSplits = listOf(
            com.splitmate.app.data.ExpenseSplitEntity(
                splitId = "exp_rohan_dinner_1_sp_0",
                expenseId = "exp_rohan_dinner_1",
                memberId = "m_akshay_me",
                baseClaimedCents = 75_000L,
                finalOwedCents = 75_000L,
                plusOneCent = false
            ),
            com.splitmate.app.data.ExpenseSplitEntity(
                splitId = "exp_rohan_dinner_1_sp_1",
                expenseId = "exp_rohan_dinner_1",
                memberId = "m_rohan_remote_join",
                baseClaimedCents = 75_000L,
                finalOwedCents = 75_000L,
                plusOneCent = false
            )
        )
        val remoteDocFromPhoneB = com.splitmate.app.data.CloudGroupLedgerDocument(
            group = group,
            members = listOf(
                com.splitmate.app.data.GroupMemberEntity(
                    memberId = "m_akshay_me",
                    groupId = group.groupId,
                    name = "Akshay",
                    avatarSeed = "Akshay|Male|open-peeps|Buckwheat",
                    isCurrentUser = false,
                    upiId = "9876543210@upi",
                    userPhone = "9876543210",
                    inviteStatus = "JOINED"
                ),
                com.splitmate.app.data.GroupMemberEntity(
                    memberId = "m_rohan_remote_join",
                    groupId = group.groupId,
                    name = "Rohan Sharma",
                    avatarSeed = "Rohan Sharma|Male|adventurer|Terracotta",
                    isCurrentUser = true,
                    upiId = "9123456789@upi",
                    userPhone = "9123456789",
                    inviteStatus = "JOINED"
                )
            ),
            expenses = listOf(rohanExpense),
            splits = rohanSplits,
            settlements = emptyList(),
            updatedAtEpochMs = 1760000450000L,
            organizerPhone10 = "9876543210"
        )

        val mergedOnPhoneA = com.splitmate.app.data.CloudGroupSyncRepository.mergeGroupLedgerDocuments(
            localDoc = localDocOnPhoneA,
            remoteDoc = remoteDocFromPhoneB,
            localUserPhone10 = "9876543210"
        )

        // 1. Rohan's expense must NOT be dropped even though his remote memberId differed from Phone A's local memberId
        assertEquals(1, mergedOnPhoneA.expenses.size, "Rohan's remote expense must be preserved after cross-device memberId remapping")
        val mergedExp = mergedOnPhoneA.expenses.single()
        val survivingRohan = mergedOnPhoneA.members.first { it.userPhone == "9123456789" }
        assertEquals(
            survivingRohan.memberId,
            mergedExp.payerId,
            "Expense payerId must be remapped to the canonical surviving memberId for Rohan"
        )

        // 2. Both splits must survive and sum to 150_000L paise (0.00c drift)
        assertEquals(2, mergedOnPhoneA.splits.size, "Both expense splits must survive after memberId remapping")
        assertEquals(
            150_000L,
            mergedOnPhoneA.splits.sumOf { it.finalOwedCents },
            "Remapped expense splits must preserve exact 0.00c drift total"
        )
        assertTrue(
            mergedOnPhoneA.splits.any { it.memberId == survivingRohan.memberId && it.finalOwedCents == 75_000L },
            "Rohan's split must be remapped to his canonical surviving memberId"
        )

        // 3. Monotonic JOINED status and 4-part canonical avatarSeed must be preserved even when localDoc had a higher timestamp
        assertEquals("JOINED", survivingRohan.inviteStatus, "JOINED status must be monotonic over PENDING")
        assertEquals(
            "Rohan Sharma|Male|adventurer|Terracotta",
            survivingRohan.avatarSeed,
            "4-part canonical avatarSeed from joined member must win over bare placeholder seed"
        )
    }

    @Test
    @DisplayName("PA-11: Automatic Offline-to-Online Pending Cloud Push Queue & Live Topic Resolution")
    fun testAutomaticOfflineToOnlinePendingCloudPushQueue() = kotlinx.coroutines.test.runTest {
        val vm = SplitMateViewModel(dao = null, ioDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        vm.createNewGroup(name = "Coorg Roadtrip", currencyCode = "INR", friendNamesCsv = "Rohan")
        testScheduler.advanceUntilIdle()

        val groupId = vm.uiState.value.activeGroupId
        assertTrue(groupId.isNotBlank())

        // Clear any initial flag, then set offline mode and log an expense
        com.splitmate.app.data.CloudGroupSyncRepository.setGroupPendingCloudPush(null, groupId, false)
        assertFalse(com.splitmate.app.data.CloudGroupSyncRepository.isGroupPendingCloudPush(null, groupId))

        vm.setOfflineMode(true)
        vm.commitQuickEqualExpense(
            title = "Coffee Stop while Offline",
            totalAmountCents = 48_000L,
            selectedMemberIds = vm.uiState.value.activeGroupMembers.map { it.memberId }
        )
        testScheduler.advanceUntilIdle()

        // Must automatically queue group for pending cloud push and mark expense as PENDING
        assertTrue(
            com.splitmate.app.data.CloudGroupSyncRepository.isGroupPendingCloudPush(null, groupId),
            "Adding an expense must immediately mark the group for automatic pending cloud push"
        )
        val loggedExpense = vm.uiState.value.expenses.first { it.title == "Coffee Stop while Offline" }
        assertEquals("PENDING", loggedExpense.syncStatus)

        // Verify live topic resolution for real-time ntfy stream subscription
        val topic = com.splitmate.app.data.CloudGroupSyncRepository.groupTopicForGroupId(groupId)
        assertTrue(topic.startsWith("splitmate_v2_grp_"))

        // When network connectivity returns, onNetworkConnectivityChanged clears offline mode automatically
        vm.onNetworkConnectivityChanged(null, isConnected = true)
        assertFalse(vm.uiState.value.isOfflineMode)
    }

    @Test
    @DisplayName("PA-12: WhatsApp-Style Multi-Organizer Governance & Recipient/Unjoined-Organizer Mark Paid Authorization")
    fun testMultiOrganizerGovernanceAndRecipientMarkPaidAuthorization() = kotlinx.coroutines.test.runTest {
        val dispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        val vm = SplitMateViewModel(dao = null, ioDispatcher = dispatcher)
        vm.completeOnboarding(
            name = "Akshay",
            countryName = "India",
            currencyCode = "INR",
            currencySymbol = "₹",
            avatarSeed = "Akshay",
            userPhone = "9876543210"
        )
        testScheduler.advanceUntilIdle()

        // Create group with Rohan (JOINED 10-digit phone) and Sneha (offline / no phone)
        vm.createNewGroupWithContacts(
            name = "Pondicherry Escape",
            iconName = "Flight",
            memberDrafts = listOf(
                com.splitmate.app.ui.NewGroupMemberDraft(name = "Rohan", cleanPhone = "9123456789"),
                com.splitmate.app.ui.NewGroupMemberDraft(name = "Sneha", cleanPhone = "")
            )
        )
        testScheduler.advanceUntilIdle()

        val groupId = vm.uiState.value.activeGroupId
        val membersInit = vm.uiState.value.members.filter { it.groupId == groupId }
        val akshay = membersInit.first { it.name == "Akshay" }
        val rohan = membersInit.first { it.name == "Rohan" }
        val sneha = membersInit.first { it.name == "Sneha" }

        // Mark Rohan as JOINED (active in trip group) while Sneha has no phone (offline/not joined via phone)
        vm.acceptGroupInvite(context = null, groupId = groupId)
        vm.claimGroupMemberPerspective(groupId, rohan.memberId)
        vm.acceptGroupInvite(context = null, groupId = groupId)
        vm.claimGroupMemberPerspective(groupId, akshay.memberId)
        testScheduler.advanceUntilIdle()

        // 1. Initially only Akshay (creator) is Organizer
        assertEquals(listOf(akshay.memberId), vm.getGroupOrganizerMembers(groupId).map { it.memberId })
        assertTrue(vm.isMemberGroupOrganizer(groupId, akshay))
        assertFalse(vm.isMemberGroupOrganizer(groupId, rohan))

        // 2. Akshay promotes Rohan to Organizer ("Make Organizer")
        var promoteOk = false
        vm.promoteMemberToOrganizer(context = null, groupId = groupId, targetMemberId = rohan.memberId) { ok, _ -> promoteOk = ok }
        testScheduler.advanceUntilIdle()
        assertTrue(promoteOk, "Organizer must be able to promote another member to Organizer")
        assertTrue(vm.isMemberGroupOrganizer(groupId, rohan), "Rohan must now be a co-organizer")
        assertEquals(2, vm.getGroupOrganizerMembers(groupId).size)

        // 3. Verify Mark Paid Authorization:
        //    - Transfer where Rohan (JOINED with phone) is the recipient (toMemberId = rohan.memberId):
        //      * Akshay (payer / co-organizer) CANNOT mark paid because Rohan is JOINED in the group!
        //      * Only Rohan (the recipient receiving the money) CAN mark paid.
        assertFalse(
            vm.canCurrentUserMarkTransferPaid(groupId, toMemberId = rohan.memberId),
            "Even an Organizer cannot Mark Paid when the recipient (Rohan) is an active JOINED member in the trip group"
        )
        vm.claimGroupMemberPerspective(groupId, rohan.memberId)
        assertTrue(
            vm.canCurrentUserMarkTransferPaid(groupId, toMemberId = rohan.memberId),
            "Recipient (Rohan) must be able to Mark Paid when receiving money"
        )

        //    - Transfer where Sneha (unjoined / offline member without 10-digit phone) is the recipient:
        //      * Organizer (Akshay or Rohan) CAN mark paid on behalf of Sneha!
        assertTrue(
            vm.canCurrentUserMarkTransferPaid(groupId, toMemberId = sneha.memberId),
            "Organizer must be able to Mark Paid when recipient (Sneha) is not an active JOINED phone member in the trip group"
        )

        // 4. Switch back to Akshay and dismiss Rohan as Organizer ("Dismiss as Organizer")
        vm.claimGroupMemberPerspective(groupId, akshay.memberId)
        var dismissOk = false
        vm.dismissMemberAsOrganizer(context = null, groupId = groupId, targetMemberId = rohan.memberId) { ok, _ -> dismissOk = ok }
        testScheduler.advanceUntilIdle()
        assertTrue(dismissOk, "Co-organizer dismissal must succeed when at least 1 organizer remains")
        assertFalse(vm.isMemberGroupOrganizer(groupId, rohan))
        assertTrue(vm.isMemberGroupOrganizer(groupId, akshay))

        // 5. Now that Rohan is no longer an Organizer, Rohan cannot mark paid for unjoined member Sneha
        vm.claimGroupMemberPerspective(groupId, rohan.memberId)
        assertFalse(
            vm.canCurrentUserMarkTransferPaid(groupId, toMemberId = sneha.memberId),
            "Non-organizer non-recipient (Rohan) must NOT be able to Mark Paid for unjoined member Sneha"
        )
    }
}





