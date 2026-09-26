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
        assertEquals(6, otpResult.generatedOtpCode.length, "OTP code must be 6 digits")
        assertTrue(otpResult.generatedOtpCode.all { it.isDigit() }, "OTP code must be numeric")

        // Verify failure after expiresAtEpochMs + 1
        assertFalse(
            com.splitmate.app.data.PhoneOtpAuthManager.verifyOtp(
                rawPhone = "+91 98765 43210",
                enteredCode = otpResult.generatedOtpCode,
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
                enteredCode = otpResult.generatedOtpCode,
                nowEpochMs = otpResult.expiresAtEpochMs - 1_000L
            ),
            "verifyOtp must succeed with matching 6-digit OTP before expiry"
        )

        // One-time consumption: replay must fail
        assertFalse(
            com.splitmate.app.data.PhoneOtpAuthManager.verifyOtp(
                rawPhone = "9876543210",
                enteredCode = otpResult.generatedOtpCode,
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
            com.splitmate.app.ui.screens.TripHubBookingCategory.GENERAL,
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
        assertEquals(6, dispatch.generatedOtpCode.length)
        assertEquals(dispatch.generatedOtpCode, viewModel.uiState.value.pendingOtpCodeForBanner)

        // 2. Verify OTP and register with 4-digit Recovery PIN "4829", style "adventurer", preset "Electric"
        var otpResultOk = false
        var otpResultMsg = ""
        viewModel.verifyPhoneOtpAndSyncCloud(
            context = null,
            rawPhone = "9876543210",
            enteredOtp = dispatch.generatedOtpCode,
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
}
