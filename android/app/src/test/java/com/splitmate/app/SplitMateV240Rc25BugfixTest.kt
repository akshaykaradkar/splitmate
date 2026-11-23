package com.splitmate.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.data.SettlementEntity
import com.splitmate.app.ui.SplitMateExpressiveTheme
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.components.ExpressiveMenuAction
import com.splitmate.app.ui.components.SplitButtonLayout
import com.splitmate.app.ui.fund.TripFundCalculator
import com.splitmate.app.ui.fund.isTripFundAdvance
import com.splitmate.app.ui.fund.resolveTripFundContributions
import com.splitmate.app.ui.screens.UserSettingsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * v2.4.0-rc25: fixes from the rc24 bug hunt. These tests run the code (ViewModel, pure rules and
 * rendered Compose), they do not check source text.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp")
class SplitMateV240Rc25BugfixTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun settlement(id: String, from: String, to: String, cents: Long, at: Long) = SettlementEntity(
        settlementId = id, groupId = "g1", fromMemberId = from, fromMemberName = from, toMemberId = to,
        toMemberName = to, amountCents = cents, currencyCode = "INR", lockedExchangeRate = 1.0, settledAt = at
    )

    private fun expense(id: String, payer: String, cents: Long) = ExpenseEntity(
        expenseId = id, groupId = "g1", title = id, payerId = payer, baseSubtotalCents = cents, taxCents = 0L,
        tipCents = 0L, totalAmountCents = cents, lockedMultiplier = 1.0, unassignedBaseCents = 0L,
        currencyCode = "INR", lockedExchangeRate = 1.0
    )

    private val members = listOf("ravi", "asha", "bina").map { GroupMemberEntity(it, "g1", it, it) }

    @Test
    fun `RC25_01 two equal Trip Fund top-ups minutes apart are both kept by the duplicate cleanup`() {
        val fiveMin = 5 * 60 * 1000L
        val advances = listOf(
            settlement("advance_1", "asha", "ravi", 500_000L, 1_000L),
            settlement("advance_2", "asha", "ravi", 500_000L, 1_000L + fiveMin)
        )
        assertTrue(SettlementDuplicateGuard.redundantDuplicateIds(emptyList(), emptyList(), advances).isEmpty())

        // The cleanup still removes a real double entry of an ordinary payment.
        val exp = expense("e1", "ravi", 100_000L)
        val splits = listOf(
            ExpenseSplitEntity("s1", "e1", "ravi", 50_000L, 50_000L),
            ExpenseSplitEntity("s2", "e1", "asha", 50_000L, 50_000L)
        )
        val payments = listOf(
            settlement("settle_1", "asha", "ravi", 50_000L, 1_000L),
            settlement("settle_2", "asha", "ravi", 50_000L, 1_000L + fiveMin)
        )
        assertEquals(setOf("settle_2"), SettlementDuplicateGuard.redundantDuplicateIds(listOf(exp), splits, payments))
    }

    @Test
    fun `RC25_02 saving the profile keeps the new name, phone and UPI ID together`() {
        val vm = SplitMateViewModel()
        vm.updateUserProfile("Old Name", "9876543210", "", "old@upi")
        vm.updateUserProfile("New Name", "9123456780", vm.uiState.value.currentUserSeed, "new@upi")
        val state = vm.uiState.value
        assertEquals("New Name", state.currentUserName)
        assertEquals("9123456780", state.userPhone)
        assertEquals("new@upi", state.userUpiId)
    }

    @Test
    fun `RC25_03 Settings Save profile makes one save call with all four values`() {
        val calls = mutableListOf<List<String>>()
        var legacyCalls = 0
        compose.setContent {
            SplitMateExpressiveTheme {
                UserSettingsScreen(
                    userName = "Akshay",
                    userPhone = "9876543210",
                    upiId = "akshay@upi",
                    onUpdateUpiId = { legacyCalls++ },
                    onUpdateUserProfile = { _, _, _ -> legacyCalls++ },
                    onSaveProfile = { n, p, s, u -> calls += listOf(n, p, s, u) }
                )
            }
        }
        compose.onNode(hasText("Save profile")).performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(1, calls.size)
        assertEquals("Akshay", calls[0][0])
        assertEquals("9876543210", calls[0][1])
        assertEquals("akshay@upi", calls[0][3])
        assertEquals(0, legacyCalls, "the two older callbacks must not also fire")
    }

    @Test
    fun `RC25_04 full-width split button keeps a visible menu button`() {
        compose.setContent {
            SplitMateExpressiveTheme {
                Box(Modifier.width(328.dp)) {
                    SplitButtonLayout(
                        leadingText = "Edit expense",
                        leadingIcon = null,
                        onLeadingClick = {},
                        menuItems = listOf(ExpressiveMenuAction(label = "Delete expense", onClick = {})),
                        fillWidth = true
                    )
                }
            }
        }
        compose.waitForIdle()
        val trailing = compose.onNode(hasContentDescription("More actions")).fetchSemanticsNode().boundsInRoot
        val leading = compose.onNode(hasText("Edit expense")).fetchSemanticsNode().boundsInRoot
        val minPx = with(compose.density) { 40.dp.toPx() }
        assertTrue(trailing.width >= minPx, "menu button is ${trailing.width}px wide, needs at least $minPx")
        assertTrue(trailing.left >= leading.right, "menu button must sit to the right of the label")
        assertTrue(trailing.right <= with(compose.density) { 328.dp.toPx() } + 1f, "menu button must stay inside the row")
    }

    @Test
    fun `RC25_05 only the sender, the holder or an organizer may record or remove an advance`() {
        val vm = SplitMateViewModel()
        vm.createNewGroup(name = "Manali", currencyCode = "INR", friendNamesCsv = "Priya, Sam, Raj")
        val real = vm.uiState.value
        val gid = real.activeGroupId
        val priya = real.activeGroupMembers.first { it.name == "Priya" }
        val sam = real.activeGroupMembers.first { it.name == "Sam" }
        val raj = real.activeGroupMembers.first { it.name == "Raj" }
        assertFalse(vm.isMemberGroupOrganizer(gid, sam), "test setup: Sam must not be an organizer")

        // The same trip as seen on Sam's phone.
        val asSam = real.copy(members = real.members.map { it.copy(isCurrentUser = it.memberId == sam.memberId) }, userPhone = "")
        assertTrue(vm.canCurrentUserManageAdvance(gid, sam.memberId, priya.memberId, asSam), "sender")
        assertTrue(vm.canCurrentUserManageAdvance(gid, raj.memberId, sam.memberId, asSam), "holder")
        assertFalse(vm.canCurrentUserManageAdvance(gid, raj.memberId, priya.memberId, asSam), "bystander")
        // The organizer (trip creator) may record for others.
        assertTrue(vm.canCurrentUserManageAdvance(gid, raj.memberId, priya.memberId, real), "organizer")
    }

    @Test
    fun `RC25_06 a double tap records a Trip Fund batch once, and removal goes through the checked path`() {
        val vm = SplitMateViewModel()
        vm.createNewGroup(name = "Goa", currencyCode = "INR", friendNamesCsv = "Priya, Sam")
        val s0 = vm.uiState.value
        val me = s0.activeGroupMembers.first { it.isCurrentUser }
        val priya = s0.activeGroupMembers.first { it.name == "Priya" }
        val batch = mapOf(priya.memberId to 500_000L)
        assertNull(vm.recordTripFundAdvances(me.memberId, batch))
        assertNull(vm.recordTripFundAdvances(me.memberId, batch))
        val advances = vm.uiState.value.settlements.filter { it.isTripFundAdvance() }
        assertEquals(1, advances.size)

        assertNull(vm.removeTripFundAdvance(advances.single().settlementId))
        assertTrue(vm.uiState.value.settlements.none { it.isTripFundAdvance() })
        assertNotNull(vm.removeTripFundAdvance("advance_missing"))
    }

    @Test
    fun `RC25_07 cash in hand counts refunds and returned advances, and the chip never hides a real debt`() {
        val advances = listOf(
            settlement("advance_1", "asha", "ravi", 500_000L, 1L),
            settlement("advance_2", "bina", "ravi", 500_000L, 2L)
        )
        // Ravi refunds Asha 1,000 with an ordinary payment and sends Bina 500 back as an advance.
        val later = advances + settlement("settle_r", "ravi", "asha", 100_000L, 3L) +
            settlement("advance_back", "ravi", "bina", 50_000L, 4L)
        val fund = TripFundCalculator.summarize("g1", members, listOf(expense("e1", "ravi", 300_000L)), later).single()
        assertEquals("ravi", fund.treasurerMemberId)
        assertEquals(950_000L, fund.totalCollectedCents)
        assertEquals(100_000L, fund.returnedCents)
        assertEquals(550_000L, fund.cashInHandCents)

        // Fund of 1,000 plus a separate 10,000 debt: balance is -11,000 and must read as owing.
        val small = TripFundCalculator.summarize("g1", members, emptyList(), listOf(settlement("advance_s", "asha", "ravi", 100_000L, 1L)))
        assertFalse(TripFundCalculator.isCustodialPoolBalance("ravi", -1_100_000L, small))
        assertTrue(TripFundCalculator.isCustodialPoolBalance("ravi", -100_000L, small))
    }

    @Test
    fun `RC25_08 a cleared per-person amount records nothing for that person`() {
        val ids = listOf("asha", "bina")
        assertEquals(
            mapOf("asha" to 500_000L, "bina" to 500_000L),
            resolveTripFundContributions(ids, "5000", emptyMap())
        )
        assertEquals(
            mapOf("bina" to 300_000L),
            resolveTripFundContributions(ids, "5000", mapOf("asha" to "", "bina" to "3000"))
        )
    }

    @Test
    fun `RC25_09 a member with payments cannot be removed, and a member with history cannot leave`() {
        val vm = SplitMateViewModel()
        vm.createNewGroup(name = "Coorg", currencyCode = "INR", friendNamesCsv = "Priya, Sam")
        val s0 = vm.uiState.value
        val gid = s0.activeGroupId
        val me = s0.activeGroupMembers.first { it.isCurrentUser }
        val priya = s0.activeGroupMembers.first { it.name == "Priya" }
        val sam = s0.activeGroupMembers.first { it.name == "Sam" }

        // Priya advances 5,000 to me; Sam has no history.
        assertNull(vm.recordTripFundAdvances(me.memberId, mapOf(priya.memberId to 500_000L)))
        var refusal = ""
        assertFalse(vm.removeMemberFromGroup(groupId = gid, targetMemberId = priya.memberId) { _, msg -> refusal = msg })
        assertTrue(refusal.contains("1 recorded payment"), refusal)
        val after = vm.uiState.value
        assertTrue(after.members.any { it.memberId == priya.memberId }, "Priya is still in the trip")
        assertEquals(1, after.settlements.count { it.groupId == gid }, "her advance is still there")

        // I hold her advance, so I cannot leave either.
        assertFalse(vm.leaveGroup(groupId = gid))
        assertTrue(vm.uiState.value.settlements.any { it.groupId == gid })

        // A member with no history can still be removed.
        assertTrue(vm.removeMemberFromGroup(groupId = gid, targetMemberId = sam.memberId))
        assertTrue(vm.uiState.value.members.none { it.memberId == sam.memberId })
    }

    // ------------------------------------------------------------------------------------------
    // rc25 second batch ("the remaining list")
    // ------------------------------------------------------------------------------------------

    private val lookups = mutableListOf<String>()

    private fun trainController() = com.splitmate.app.ui.screens.PnrReviewSessionController(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
    ) { cleanPnr, _ ->
        lookups += cleanPnr
        com.splitmate.app.data.PnrNetworkRepository.buildUnverifiedManualFallbackSnapshot(cleanPnr = cleanPnr)
    }

    @Test
    fun `RC25_10 the train fare card stays once the user has typed into it`() {
        fun shown(total: Long, typed: Boolean) = com.splitmate.app.ui.screens.shouldShowManualTicketCard(
            isManualEntry = false, isLiveVerified = true, effectiveTotalPaise = total,
            resolvedFromCode = "MMCT", resolvedToCode = "NDLS", hasTypedManualValues = typed
        )
        assertTrue(shown(total = 0L, typed = false), "a live ticket without a fare asks for it")
        assertTrue(shown(total = 200L, typed = true), "after the first digit the card must stay")
        assertFalse(shown(total = 245_000L, typed = false), "a complete live ticket shows no manual card")
    }

    @Test
    fun `RC25_11 editing the PNR in Manual Ticket mode keeps fare, route, time and member picks`() {
        val c = trainController()
        val provider = { members }
        c.syncForGroupOrExistingExpense(members, null, emptySet())
        c.enterExplicitManualMode()
        c.manualTotalFareInput = "2450"
        c.manualFromStationInput = "MMCT"
        c.manualToStationInput = "NDLS"
        c.manualDepartureDateInput = "15 Mar 2026"
        c.manualDepartureTimeInput = "17:00"
        c.onToggleMember("bina")
        val picks = c.selectedMemberIds

        "4521876309".indices.forEach { i ->
            c.onPnrDigitsChanged("4521876309".take(i + 1), provider, { null }, { emptySet() })
        }
        assertEquals("2450", c.manualTotalFareInput)
        assertEquals("MMCT", c.manualFromStationInput)
        assertEquals("NDLS", c.manualToStationInput)
        assertEquals("15 Mar 2026", c.manualDepartureDateInput)
        assertEquals("17:00", c.manualDepartureTimeInput)
        assertEquals(picks, c.selectedMemberIds)
        assertEquals("4521876309", c.liveSnapshot?.pnr, "the typed PNR is stored on the manual ticket")
        assertTrue(c.liveSnapshot?.isManualEntry == true)
        assertTrue(lookups.isEmpty(), "Manual Ticket mode must not start a live lookup on its own")
    }

    @Test
    fun `RC25_12 outside Manual Ticket mode a new PNR still starts clean`() {
        val c = trainController()
        val provider = { members }
        // The lookup falls back to manual entry on its own (server unreachable).
        c.onPnrDigitsChanged("1111111111", provider, { null }, { emptySet() })
        assertEquals(listOf("1111111111"), lookups)
        assertFalse(c.isExplicitManualMode)
        c.manualTotalFareInput = "999"
        c.manualFromStationInput = "MMCT"
        c.onPnrDigitsChanged("222222222", provider, { null }, { emptySet() })
        assertEquals("", c.manualTotalFareInput, "PNR B never inherits PNR A's fare")
        assertEquals("", c.manualFromStationInput)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h420dp")
    fun `RC25_13 the expense detail sheet scrolls so Edit stays reachable on a short screen`() {
        var edited = false
        val exp = expense("Dinner at the beach shack", "ravi", 120_000L)
        compose.setContent {
            SplitMateExpressiveTheme {
                com.splitmate.app.ui.screens.ActivityDetailSheet(
                    expense = exp,
                    payer = members.first(),
                    breakdown = SplitMateViewModel.ExpenseSplitBreakdownSummary(
                        totalMembersInGroup = 3, splittingMembersCount = 1, perPersonHeadlineShare = "₹1,200",
                        headerLabel = "Personal", rows = emptyList(),
                        splitNature = ExpenseSplitNature.PERSONAL
                    ),
                    onDismiss = {},
                    onOpenBoardingPass = { _, _ -> },
                    onEditExpense = { edited = true },
                    onUndoExpense = {}
                )
            }
        }
        compose.waitForIdle()
        compose.onNode(hasText("Edit Expense")).performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(edited, "Edit Expense must be reachable and clickable")
    }

    @Test
    fun `RC25_14 contact picker Select all and Clear act on the rows the search shows`() {
        fun toggle(visible: List<String>, selected: Set<String>) =
            com.splitmate.app.ui.contactPickerSelectAllToggle(visible, selected)
        // One earlier pick that the search hides, two visible rows, one row without a phone.
        val first = toggle(listOf("9000000001", "9000000002", "contact_17"), setOf("9111111111"))
        assertEquals("Select all", first.label)
        assertEquals(setOf("9111111111", "9000000001", "9000000002"), first.nextSelection)

        val second = toggle(listOf("9000000001", "9000000002", "contact_17"), first.nextSelection)
        assertEquals("Clear", second.label, "label and action agree once every visible row is ticked")
        assertEquals(setOf("9111111111"), second.nextSelection, "Clear keeps picks the search is hiding")

        assertEquals("Select all", toggle(listOf("contact_1"), emptySet()).label)
    }

    @Test
    fun `RC25_15 a phone number in a title never overwrites an earlier expense`() {
        val vm = SplitMateViewModel()
        vm.createNewGroup(name = "Hampi", currencyCode = "INR", friendNamesCsv = "Priya, Sam")
        val ids = vm.uiState.value.activeGroupMembers.map { it.memberId }
        vm.commitQuickEqualExpense(title = "Cab driver Ramesh 9876543210", totalAmountCents = 90_000L, selectedMemberIds = ids)
        vm.commitQuickEqualExpense(title = "Guide tip via 9876543210", totalAmountCents = 30_000L, selectedMemberIds = ids)
        val expenses = vm.uiState.value.expenses.filter { it.groupId == vm.uiState.value.activeGroupId }
        assertEquals(2, expenses.size, "both expenses are kept")
        assertEquals(setOf(90_000L, 30_000L), expenses.map { it.totalAmountCents }.toSet())
        assertTrue(expenses.all { it.travelPnr.isBlank() }, "a phone number is not stored as a PNR")
        assertTrue(expenses.none { it.expenseCategory == "TRAIN" }, expenses.map { it.expenseCategory }.toString())

        // A real train title still de-duplicates on its PNR.
        vm.commitQuickEqualExpense(title = "Train PNR 4521876309", totalAmountCents = 200_000L, selectedMemberIds = ids)
        vm.commitQuickEqualExpense(title = "Train PNR 4521876309 final", totalAmountCents = 210_000L, selectedMemberIds = ids)
        val trains = vm.uiState.value.expenses.filter { it.travelPnr == "4521876309" }
        assertEquals(1, trains.size)
        assertEquals(210_000L, trains.single().totalAmountCents)
    }

    @Test
    fun `RC25_16 a duplicate PNR never flattens a custom split onto the earlier expense`() {
        val vm = SplitMateViewModel()
        vm.createNewGroup(name = "Delhi", currencyCode = "INR", friendNamesCsv = "Priya, Sam")
        val ids = vm.uiState.value.activeGroupMembers.map { it.memberId }
        vm.commitQuickEqualExpense(title = "Train PNR 4521876309", totalAmountCents = 300_000L, selectedMemberIds = ids)
        val before = vm.uiState.value
        vm.commitQuickEqualExpense(
            title = "Train PNR 4521876309 upgrade", totalAmountCents = 400_000L, selectedMemberIds = ids,
            customSharesByMemberId = mapOf(ids[0] to 300_000L, ids[1] to 50_000L, ids[2] to 50_000L)
        )
        val after = vm.uiState.value
        assertEquals(before.expenses, after.expenses, "the earlier expense is untouched")
        assertEquals(before.splits, after.splits)
        assertTrue(after.statusBannerMessage.orEmpty().contains("already logged"), after.statusBannerMessage.orEmpty())
    }

    @Test
    fun `RC25_17 a custom split is not called an itemized receipt`() {
        val custom = expense("e1", "ravi", 100_000L)
        val customSplits = listOf(
            ExpenseSplitEntity("s1", "e1", "ravi", 70_000L, 70_000L),
            ExpenseSplitEntity("s2", "e1", "asha", 30_000L, 30_000L)
        )
        val reason = ItemizedExpenseGuard.lockedReasonFor(custom, customSplits)
        assertTrue(reason.startsWith("Custom split"), reason)
        assertFalse(reason.contains("receipt", ignoreCase = true), reason)

        val receipt = custom.copy(taxCents = 5_000L, baseSubtotalCents = 95_000L)
        assertEquals(ItemizedExpenseGuard.LOCKED_REASON, ItemizedExpenseGuard.lockedReasonFor(receipt, customSplits))
    }

    @Test
    fun `RC25_18 Trip Fund advances are not counted or worded as repayments on the Settle screen`() {
        val rows = listOf(
            settlement("advance_1", "asha", "ravi", 500_000L, 1L),
            settlement("settle_1", "bina", "ravi", 20_000L, 2L)
        )
        assertEquals("1 Paid · 1 advance", com.splitmate.app.ui.fund.settleHistoryBadgeLabel(rows))
        assertEquals("0 Paid", com.splitmate.app.ui.fund.settleHistoryBadgeLabel(emptyList()))
        assertEquals("2 advances", com.splitmate.app.ui.fund.settleHistoryBadgeLabel(listOf(rows[0], rows[0].copy(settlementId = "advance_2"))))
        assertEquals(20_000L, com.splitmate.app.ui.fund.repaidCentsExcludingAdvances(rows), "progress counts repayments only")
        assertEquals("asha → ravi's Trip Fund", com.splitmate.app.ui.fund.settleHistoryRowTitle(rows[0], "asha", "ravi"))
        assertEquals("bina paid ravi", com.splitmate.app.ui.fund.settleHistoryRowTitle(rows[1], "bina", "ravi"))
    }
}
