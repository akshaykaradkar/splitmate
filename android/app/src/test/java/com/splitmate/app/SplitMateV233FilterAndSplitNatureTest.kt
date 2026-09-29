package com.splitmate.app

import com.splitmate.app.SplitMateMathEngine.MemberPaymentLeg
import com.splitmate.app.SplitMateMathEngine.MemberSettlementSummary
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * SplitMate v2.3.3 (versionCode 51) bug-fix regression suite.
 *
 * - Bug 1: "Your Settlements" Gets Back / Owes filters must never show a false "all settled" state.
 * - Bug 2: personal (non-split) expenses read as "Personal expense", never "1 of N splitting" / "₹x/person".
 *
 * Pure UI-copy / presentation logic only; money math is asserted unchanged.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SplitMate v2.3.3 settlement filter + split nature")
class SplitMateV233FilterAndSplitNatureTest {

    // ------------------------------------------------------------------
    // BUG 1: SettlementFilterPresentation
    // ------------------------------------------------------------------

    private fun leg(id: String, cents: Long) = MemberPaymentLeg(
        counterpartyMemberId = id,
        counterpartyName = id,
        amountCents = cents,
        formattedAmount = SplitMateMathEngine.formatCurrencyCents(cents)
    )

    private fun summary(
        id: String,
        isMe: Boolean = false,
        outgoing: List<MemberPaymentLeg> = emptyList(),
        incoming: List<MemberPaymentLeg> = emptyList()
    ) = MemberSettlementSummary(
        memberId = id,
        memberName = id,
        isCurrentUser = isMe,
        outgoingPayments = outgoing,
        incomingPayments = incoming,
        formattedTotalOutgoing = SplitMateMathEngine.formatCurrencyCents(outgoing.sumOf { it.amountCents }),
        formattedTotalIncoming = SplitMateMathEngine.formatCurrencyCents(incoming.sumOf { it.amountCents })
    )

    /** Current user only owes: me -> priya 1500.00, me -> sam 500.00; priya/sam only get back. */
    private val iOnlyOwe = listOf(
        summary("me", isMe = true, outgoing = listOf(leg("priya", 150_000L), leg("sam", 50_000L))),
        summary("priya", incoming = listOf(leg("me", 150_000L))),
        summary("sam", incoming = listOf(leg("me", 50_000L)))
    )

    /** Current user only gets back. */
    private val iOnlyGetBack = listOf(
        summary("me", isMe = true, incoming = listOf(leg("priya", 80_000L))),
        summary("priya", outgoing = listOf(leg("me", 80_000L)))
    )

    @Nested
    @DisplayName("Bug 1: Your Settlements filter")
    inner class SettlementFilter {

        @Test
        fun `Gets Back while I only owe is NOT settled and offers a jump to Owes`() {
            val p = SettlementFilterPresentation.resolve(iOnlyOwe, SettlementFilterPresentation.FILTER_GETS_BACK)
            assertFalse(p.isTrulySettled)
            assertFalse(p.showSettledCard, "Must not show 'You're all settled up' when I owe money")
            assertNotNull(p.myFilterEmptyState)
            val empty = p.myFilterEmptyState!!
            assertEquals("Nothing to get back", empty.title)
            assertEquals("You owe ₹2,000.00 in this trip · see Owes", empty.subtitle)
            assertEquals(SettlementFilterPresentation.FILTER_OWES, empty.switchToFilterIndex)
            // Others (priya, sam) do get back, so no empty state for them.
            assertNull(p.othersFilterEmptyState)
        }

        @Test
        fun `Owes while I only get back offers a jump to Gets Back`() {
            val p = SettlementFilterPresentation.resolve(iOnlyGetBack, SettlementFilterPresentation.FILTER_OWES)
            assertFalse(p.showSettledCard)
            val empty = p.myFilterEmptyState!!
            assertEquals("You don't owe anyone", empty.title)
            assertEquals("You get back ₹800.00 · see Gets Back", empty.subtitle)
            assertEquals(SettlementFilterPresentation.FILTER_GETS_BACK, empty.switchToFilterIndex)
        }

        @Test
        fun `matching filter and All Balances show neither settled card nor empty state`() {
            listOf(SettlementFilterPresentation.FILTER_ALL, SettlementFilterPresentation.FILTER_OWES).forEach { idx ->
                val p = SettlementFilterPresentation.resolve(iOnlyOwe, idx)
                assertFalse(p.showSettledCard)
                assertNull(p.myFilterEmptyState)
            }
        }

        @Test
        fun `truly settled user sees settled card under every filter`() {
            // Current user absent from summaries (no transfers involve them) => truly settled.
            val others = listOf(
                summary("priya", outgoing = listOf(leg("sam", 10_000L))),
                summary("sam", incoming = listOf(leg("priya", 10_000L)))
            )
            listOf(0, 1, 2).forEach { idx ->
                val p = SettlementFilterPresentation.resolve(others, idx)
                assertTrue(p.isTrulySettled)
                assertTrue(p.showSettledCard)
                assertNull(p.myFilterEmptyState)
            }
            // Present with zero legs is also settled.
            val meZero = listOf(summary("me", isMe = true)) + others
            assertTrue(SettlementFilterPresentation.isCurrentUserTrulySettled(meZero))
        }

        @Test
        fun `other travelers empty under a filter get a graceful empty state`() {
            // Under Owes, priya/sam only get back -> nobody else owes.
            val p = SettlementFilterPresentation.resolve(iOnlyOwe, SettlementFilterPresentation.FILTER_OWES)
            val others = p.othersFilterEmptyState!!
            assertEquals("No other traveler owes money", others.title)
            assertEquals(SettlementFilterPresentation.FILTER_ALL, others.switchToFilterIndex)
            // Under All Balances there is never an others-empty state.
            assertNull(SettlementFilterPresentation.resolve(iOnlyOwe, SettlementFilterPresentation.FILTER_ALL).othersFilterEmptyState)
        }

        @Test
        fun `settlement screen derives settled state from unfiltered summaries`() {
            val app = java.io.File(
                listOf(
                    "src/main/java/com/splitmate/app",
                    "app/src/main/java/com/splitmate/app",
                    "android/app/src/main/java/com/splitmate/app"
                ).map { java.io.File(it) }.first { it.isDirectory },
                "ui/SplitMateAppComposable.kt"
            ).readText()
            assertTrue(app.contains("SettlementFilterPresentation.resolve(memberSummaries, selectedBalanceFilterIndex)"))
            assertFalse(app.contains("if (myMemberSummaries.isEmpty()) \"All settled\""))
            assertFalse(app.contains("if (myMemberSummaries.isEmpty()) {"))
        }
    }

    // ------------------------------------------------------------------
    // BUG 2: ExpenseSplitNature classifier + copy
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Bug 2: ExpenseSplitNature classifier")
    inner class Classifier {

        @Test
        fun `single participant who is the payer is PERSONAL`() {
            assertEquals(
                ExpenseSplitNature.PERSONAL,
                ExpenseSplitClassifier.classify("priyanka", mapOf("priyanka" to 1_039_100L))
            )
        }

        @Test
        fun `zero-cent excluded participants are ignored`() {
            val shares = mapOf("priyanka" to 1_039_100L, "akshay" to 0L, "sam" to 0L, "maya" to -0L)
            assertEquals(ExpenseSplitNature.PERSONAL, ExpenseSplitClassifier.classify("priyanka", shares))
            assertEquals(setOf("priyanka"), ExpenseSplitClassifier.activeParticipantIds(shares))
        }

        @Test
        fun `single participant who is not the payer is ON_BEHALF`() {
            val shares = mapOf("akshay" to 50_000L, "priyanka" to 0L)
            assertEquals(ExpenseSplitNature.ON_BEHALF, ExpenseSplitClassifier.classify("priyanka", shares))
            assertEquals("akshay", ExpenseSplitClassifier.soleParticipantId(shares))
        }

        @Test
        fun `two or more participants and empty shares are SHARED`() {
            assertEquals(ExpenseSplitNature.SHARED, ExpenseSplitClassifier.classify("a", mapOf("a" to 1L, "b" to 1L)))
            assertEquals(ExpenseSplitNature.SHARED, ExpenseSplitClassifier.classify("a", emptyMap()))
            assertEquals(ExpenseSplitNature.SHARED, ExpenseSplitClassifier.classify("a", mapOf("a" to 0L, "b" to 0L)))
        }

        @Test
        fun `selection classifier mirrors review screens`() {
            assertEquals(ExpenseSplitNature.PERSONAL, ExpenseSplitClassifier.classifySelection("p", setOf("p")))
            assertEquals(ExpenseSplitNature.ON_BEHALF, ExpenseSplitClassifier.classifySelection("p", setOf("a")))
            assertEquals(ExpenseSplitNature.SHARED, ExpenseSplitClassifier.classifySelection("p", setOf("p", "a")))
        }

        @Test
        fun `copy reads naturally per nature and SHARED is verbatim`() {
            assertEquals(
                "Paid by Priyanka · Personal expense",
                ExpenseSplitCopy.paidBySubtitle(ExpenseSplitNature.PERSONAL, "Priyanka", "Priyanka", false, "₹10,391.00", "ignored")
            )
            assertEquals(
                "Paid by Priyanka for Akshay · Akshay owes ₹500.00",
                ExpenseSplitCopy.paidBySubtitle(ExpenseSplitNature.ON_BEHALF, "Priyanka", "Akshay Karadkar", false, "₹500.00", "ignored")
            )
            assertEquals(
                "Paid by Priyanka for you · you owe ₹500.00",
                ExpenseSplitCopy.paidBySubtitle(ExpenseSplitNature.ON_BEHALF, "Priyanka", "Akshay", true, "₹500.00", "ignored")
            )
            assertEquals(
                "Paid by Priyanka · ₹250.00/person",
                ExpenseSplitCopy.paidBySubtitle(ExpenseSplitNature.SHARED, "Priyanka", null, false, "", "₹250.00/person")
            )
            assertEquals(
                "Paid by Priyanka · Personal · not split",
                ExpenseSplitCopy.historyRowSubtitle(ExpenseSplitNature.PERSONAL, "Priyanka", "Priyanka", false, "", "1 splitting (₹x/ea)")
            )
            assertEquals("Personal · not split", ExpenseSplitCopy.perPersonCaption(ExpenseSplitNature.PERSONAL, null, false, "", "x"))
            assertEquals("Akshay owes ₹500.00", ExpenseSplitCopy.perPersonCaption(ExpenseSplitNature.ON_BEHALF, "Akshay", false, "₹500.00", "x"))
            assertEquals("Personal expense", ExpenseSplitCopy.splitModeLabel(ExpenseSplitNature.PERSONAL, null, false, "x"))
            assertEquals("Paid for Akshay", ExpenseSplitCopy.splitModeLabel(ExpenseSplitNature.ON_BEHALF, "Akshay", false, "x"))
            assertEquals("Equal split · 4 members", ExpenseSplitCopy.splitModeLabel(ExpenseSplitNature.SHARED, null, false, "Equal split · 4 members"))
        }

        @Test
        fun `no UI surface hard-codes the old per-person templates any more`() {
            val root = listOf(
                "src/main/java/com/splitmate/app",
                "app/src/main/java/com/splitmate/app",
                "android/app/src/main/java/com/splitmate/app"
            ).map { java.io.File(it) }.first { it.isDirectory }
            val app = java.io.File(root, "ui/SplitMateAppComposable.kt").readText()
            val sheet = java.io.File(root, "ui/screens/ActivityDetailSheet.kt").readText()
            val trip = java.io.File(root, "ui/screens/TripHomeScreen.kt").readText()
            assertTrue(app.contains("breakdown.paidBySubtitle("))
            assertTrue(app.contains("breakdown.historyRowSubtitle("))
            assertTrue(sheet.contains("breakdown.paidBySubtitle("))
            assertTrue(sheet.contains("ExpenseSplitNature.PERSONAL"))
            assertFalse(trip.contains("text = \"\${splitBreakdown.perPersonHeadlineShare} / traveler\""))
        }
    }

    // ------------------------------------------------------------------
    // BUG 2: end-to-end via the ViewModel (breakdown + balances unchanged)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Bug 2: breakdown + balance invariants")
    inner class BreakdownAndBalances {

        private val testDispatcher = StandardTestDispatcher()

        @BeforeEach
        fun setup() = Dispatchers.setMain(testDispatcher)

        @AfterEach
        fun tearDown() = Dispatchers.resetMain()

        private fun nets(state: com.splitmate.app.ui.SplitMateUiState, groupId: String): Map<String, Long> {
            val members = state.members.filter { it.groupId == groupId }
            val expenses = state.expenses.filter { it.groupId == groupId }
            val ids = expenses.map { it.expenseId }.toSet()
            val net = members.associate { it.memberId to 0L }.toMutableMap()
            expenses.forEach { net[it.payerId] = (net[it.payerId] ?: 0L) + it.totalAmountCents }
            state.splits.filter { it.expenseId in ids }.forEach { net[it.memberId] = (net[it.memberId] ?: 0L) - it.finalOwedCents }
            return net
        }

        private fun transfers(state: com.splitmate.app.ui.SplitMateUiState, groupId: String) =
            SplitMateMathEngine.simplifyDebtsGreedy(
                nets(state, groupId).map { (id, cents) -> SplitMateMathEngine.MemberNetBalance(id, id, cents) }
            )

        @Test
        fun `personal expense classifies PERSONAL and never changes anyone's balance`() = runTest(testDispatcher) {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.createNewGroup(name = "Kashmir", currencyCode = "INR", friendNamesCsv = "Priyanka, Sam, Maya")
            testDispatcher.scheduler.advanceUntilIdle()
            val members = vm.uiState.value.activeGroupMembers
            val groupId = members.first().groupId
            val me = members.first { it.isCurrentUser }

            // Shared dinner so there are real balances to protect.
            vm.commitQuickEqualExpense("Dinner", 400_000L, members.map { it.memberId })
            testDispatcher.scheduler.advanceUntilIdle()
            val before = vm.uiState.value
            val netsBefore = nets(before, groupId)
            val transfersBefore = transfers(before, groupId)

            // Personal flight ticket: only the payer participates.
            Thread.sleep(5L) // expenseId is time-based; keep the two commits distinct.
            vm.commitQuickEqualExpense("Solo Flight", 1_039_100L, listOf(me.memberId), payerMemberId = me.memberId)
            testDispatcher.scheduler.advanceUntilIdle()
            val after = vm.uiState.value

            assertEquals(netsBefore, nets(after, groupId), "Personal expense must net to zero for every member")
            assertEquals(transfersBefore, transfers(after, groupId), "Settlement plan must be identical")

            val personal = after.expenses.first { it.title.contains("Solo Flight") }
            val breakdown = SplitMateViewModel.resolveExpenseSplitBreakdown(
                expense = personal,
                groupMembers = members,
                allSplits = after.splits,
                headerPrefix = "Individual Share Breakdown"
            )
            assertEquals(ExpenseSplitNature.PERSONAL, breakdown.splitNature)
            assertEquals("Personal expense · not split", breakdown.headerLabel)
            assertFalse(breakdown.headerLabel.contains("of ${members.size}"))
            assertEquals("Paid by Akshay · Personal expense", breakdown.paidBySubtitle("Akshay", "₹10,391.00/person"))
            assertEquals("Paid by Akshay · Personal · not split", breakdown.historyRowSubtitle("Akshay", "1 splitting (₹10,391.00/ea)"))
            assertEquals("Personal · not split", breakdown.perPersonCaption("₹10,391.00 / traveler"))
            // Rows are untouched: the payer still carries the full cost in cents.
            assertEquals(1_039_100L, breakdown.rows.filter { it.isIncludedInSplit }.sumOf { it.owedCents })

            // A 0-cent split row for someone else must not turn it into SHARED.
            val personalSplit = after.splits.first { it.expenseId == personal.expenseId }
            val other = members.first { !it.isCurrentUser }
            val withZero = after.splits + personalSplit.copy(memberId = other.memberId, finalOwedCents = 0L)
            val breakdownZero = SplitMateViewModel.resolveExpenseSplitBreakdown(personal, members, withZero)
            assertEquals(ExpenseSplitNature.PERSONAL, breakdownZero.splitNature)

            // The shared dinner reads exactly as before.
            val dinner = after.expenses.first { it.title.contains("Dinner") }
            val shared = SplitMateViewModel.resolveExpenseSplitBreakdown(dinner, members, after.splits)
            assertEquals(ExpenseSplitNature.SHARED, shared.splitNature)
            assertEquals("Split Breakdown (4 members)", shared.headerLabel)
            assertEquals("Paid by Akshay · ₹1,000.00/person", shared.paidBySubtitle("Akshay", "${shared.perPersonHeadlineShare}/person"))
        }

        @Test
        fun `on-behalf expense names the beneficiary and their share`() = runTest(testDispatcher) {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.createNewGroup(name = "Goa", currencyCode = "INR", friendNamesCsv = "Priyanka, Sam")
            testDispatcher.scheduler.advanceUntilIdle()
            val members = vm.uiState.value.activeGroupMembers
            val me = members.first { it.isCurrentUser }
            val priyanka = members.first { it.name.startsWith("Priyanka") }

            vm.commitQuickEqualExpense("Train for Priyanka", 50_000L, listOf(priyanka.memberId), payerMemberId = me.memberId)
            testDispatcher.scheduler.advanceUntilIdle()
            val state = vm.uiState.value
            val exp = state.expenses.first { it.title.contains("Train for Priyanka") }
            val b = SplitMateViewModel.resolveExpenseSplitBreakdown(exp, members, state.splits)
            assertEquals(ExpenseSplitNature.ON_BEHALF, b.splitNature)
            assertEquals("Paid by You for Priyanka · Priyanka owes ₹500.00", b.paidBySubtitle("You", "x"))
            assertEquals("Priyanka owes ₹500.00", b.perPersonCaption("x"))
        }
    }
}
