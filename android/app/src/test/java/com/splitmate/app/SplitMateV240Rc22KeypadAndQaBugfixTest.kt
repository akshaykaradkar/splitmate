package com.splitmate.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.splitmate.app.ui.NewGroupMemberDraft
import com.splitmate.app.ui.SplitMateExpressiveTheme
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.screens.QuickExpenseScreen
import com.splitmate.app.ui.screens.QuickSplitMode
import com.splitmate.app.ui.screens.computeQuickSplitSharesPaise
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.math.abs

/**
 * SplitMate v2.4.0-rc22 — Real rendered Compose UI & QA bugfix regression suite.
 *
 * Renders QuickExpenseScreen in Robolectric at realistic compact phone dimensions so layout bugs
 * like TooltipBox crushing weighted '.' and '0' keypad buttons to 0dp can never ship undetected.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp")
class SplitMateV240Rc22KeypadAndQaBugfixTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun src(path: String): String =
        File("src/main/java/com/splitmate/app/$path").readText()

    @Test
    fun `RC22_01 QuickSplit keypad renders 0, dot, 1-9 and Backspace with real width and live typing`() {
        val vm = SplitMateViewModel()
        vm.createNewGroup(
            name = "Goa Trip",
            currencyCode = "INR",
            friendNamesCsv = "Priya, Sam"
        )
        var backClicks = 0
        compose.setContent {
            SplitMateExpressiveTheme {
                QuickExpenseScreen(
                    viewModel = vm,
                    onBackClick = { backClicks++ },
                    onSaveSplit = { _, _ -> }
                )
            }
        }
        compose.waitForIdle()

        val minKeyWidthPx = with(compose.density) { 48.dp.toPx() }
        // Verify every digit 0..9, dot, and Backspace is rendered with >= 48dp width.
        val dotBounds = compose.onNodeWithText(".").fetchSemanticsNode().boundsInRoot
        val zeroBounds = compose.onNodeWithText("0").fetchSemanticsNode().boundsInRoot
        val backBounds = compose.onNode(hasContentDescription("Backspace")).fetchSemanticsNode().boundsInRoot

        assertTrue(dotBounds.width >= minKeyWidthPx, "'.' key must be >= 48dp wide, was ${dotBounds.width}px")
        assertTrue(zeroBounds.width >= minKeyWidthPx, "'0' key must be >= 48dp wide, was ${zeroBounds.width}px")
        assertTrue(backBounds.width >= minKeyWidthPx, "'Backspace' key must be >= 48dp wide, was ${backBounds.width}px")
        assertTrue(
            abs(dotBounds.width - zeroBounds.width) <= 3f && abs(zeroBounds.width - backBounds.width) <= 3f,
            "Bottom row keys must share equal 1:1:1 width: dot=${dotBounds.width}, zero=${zeroBounds.width}, back=${backBounds.width}"
        )

        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9").forEach { digit ->
            val bounds = compose.onNodeWithText(digit).fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.width >= minKeyWidthPx, "Digit '$digit' must be >= 48dp wide, was ${bounds.width}px")
        }

        // Actually tap 1 -> 0 -> 5 -> . -> 5 -> 0 -> Backspace and assert the hero display updates!
        compose.onNodeWithText("1").performScrollTo().performClick()
        compose.onNodeWithText("0").performScrollTo().performClick()
        compose.onNodeWithText("5").performScrollTo().performClick()
        compose.onNodeWithText(".").performScrollTo().performClick()
        compose.onNodeWithText("5").performScrollTo().performClick()
        compose.onNodeWithText("0").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNode(hasText("₹ 105.50")).performScrollTo().assertIsDisplayed()

        compose.onNode(hasContentDescription("Backspace")).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNode(hasText("₹ 105.5")).performScrollTo().assertIsDisplayed()

        // Verify visible TopAppBar Back button exists and triggers onBackClick.
        compose.onNode(hasContentDescription("Back")).performClick()
        assertEquals(1, backClicks, "TopAppBar Back button must invoke onBackClick")
    }

    @Test
    fun `RC22_02 QuickSplit Exact, Percentage and Shares modes compute 0_00c drift shares`() {
        val members = listOf("m1", "m2", "m3")

        val exactOk = computeQuickSplitSharesPaise(
            mode = QuickSplitMode.EXACT,
            totalAmountPaise = 10_000L,
            memberIds = members,
            rawInputs = mapOf("m1" to "40", "m2" to "35.50", "m3" to "24.50")
        )
        assertTrue(exactOk.isValid)
        assertEquals(mapOf("m1" to 4000L, "m2" to 3550L, "m3" to 2450L), exactOk.sharesByMemberId)

        val exactBad = computeQuickSplitSharesPaise(
            mode = QuickSplitMode.EXACT,
            totalAmountPaise = 10_000L,
            memberIds = members,
            rawInputs = mapOf("m1" to "40", "m2" to "30", "m3" to "20")
        )
        assertFalse(exactBad.isValid)
        assertEquals(1000L, exactBad.remainingPaise)

        val pctOk = computeQuickSplitSharesPaise(
            mode = QuickSplitMode.PERCENTAGE,
            totalAmountPaise = 10_000L,
            memberIds = members,
            rawInputs = mapOf("m1" to "33.33", "m2" to "33.33", "m3" to "33.34")
        )
        assertTrue(pctOk.isValid)
        assertEquals(10_000L, pctOk.sharesByMemberId.values.sum())

        val sharesOk = computeQuickSplitSharesPaise(
            mode = QuickSplitMode.SHARES,
            totalAmountPaise = 10_000L,
            memberIds = members,
            rawInputs = mapOf("m1" to "1", "m2" to "2", "m3" to "0")
        )
        assertTrue(sharesOk.isValid)
        assertEquals(10_000L, sharesOk.sharesByMemberId.values.sum())
        assertEquals(3333L, sharesOk.sharesByMemberId["m1"])
        assertEquals(6667L, sharesOk.sharesByMemberId["m2"])
        assertEquals(0L, sharesOk.sharesByMemberId["m3"])
    }

    @Test
    fun `RC22_03 QA audit P1 and P2 fixes are wired across Settings, Dialogs, Explore, Stay and Audit`() {
        val settings = src("ui/screens/OnboardingAndSettingsScreens.kt")
        assertTrue(settings.contains("UPI ID (optional, for receiving payments)"))
        assertTrue(settings.contains("onUpdateUpiId(editedUpiId.trim())"))
        assertTrue(settings.contains("containerColor = MaterialTheme.colorScheme.error"))

        val dialogs = src("ui/dialogs/GroupAndSettlementDialogs.kt")
        assertTrue(dialogs.contains("routeFromTo.isNotBlank()"))
        assertTrue(dialogs.contains("KeyboardType.Decimal"))
        assertTrue(dialogs.contains("Enter an amount greater than 0"))

        val record = src("ui/screens/RecordPaymentDialog.kt")
        assertTrue(record.contains(".verticalScroll(rememberScrollState())"))

        val explore = src("ui/screens/plan/ExploreGuideView.kt")
        assertTrue(explore.contains("val isDropdownExpanded = searchExpanded && sanitizedSuggestions.isNotEmpty()"))
        assertTrue(explore.contains("tooltipText = \"Search destination\""))

        val stay = src("ui/screens/plan/StayPinSheet.kt")
        assertTrue(
            Regex("""onUseDestinationCenter\s*=\s*\{\s*searchedByName = false\s*awaitingNewStay = true""").findAll(stay).count() >= 2,
            "Both DestinationCenter handlers must set awaitingNewStay = true"
        )

        val app = src("ui/SplitMateAppComposable.kt")
        assertTrue(app.contains("val signedCents = if (isMePayer) expense.totalAmountCents else -myExpenseShareCents"))
    }
}
