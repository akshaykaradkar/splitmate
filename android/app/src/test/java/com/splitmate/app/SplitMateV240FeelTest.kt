package com.splitmate.app

import android.view.HapticFeedbackConstants
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.splitmate.app.ui.feel.Feel
import com.splitmate.app.ui.feel.PaidChipHoldMillis
import com.splitmate.app.ui.feel.SplitMateHaptics
import com.splitmate.app.ui.feel.PaidMorphController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.4.0 D4: haptic vocabulary and the Mark paid -> Paid morph. */
@OptIn(ExperimentalCoroutinesApi::class)
class SplitMateV240FeelTest {

    private fun src(path: String) = java.io.File("src/main/java/com/splitmate/app/$path").readText()

    private fun allUiSources(): Map<String, String> =
        java.io.File("src/main/java/com/splitmate/app/ui").walkTopDown()
            .filter { it.extension == "kt" }
            .associate { it.path to it.readText() }

    @Test
    fun `each feeling maps to the specific platform haptic on new Android and a close fallback on old`() {
        assertEquals(HapticFeedbackConstants.SEGMENT_TICK, SplitMateHaptics.platformConstant(Feel.Tick, 34))
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, SplitMateHaptics.platformConstant(Feel.Tick, 33))
        assertEquals(HapticFeedbackConstants.CONFIRM, SplitMateHaptics.platformConstant(Feel.Confirm, 30))
        assertEquals(HapticFeedbackConstants.LONG_PRESS, SplitMateHaptics.platformConstant(Feel.Confirm, 29))
        assertEquals(HapticFeedbackConstants.REJECT, SplitMateHaptics.platformConstant(Feel.Reject, 30))
        assertEquals(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE, SplitMateHaptics.platformConstant(Feel.Threshold, 34))
        assertEquals(HapticFeedbackConstants.CONTEXT_CLICK, SplitMateHaptics.platformConstant(Feel.Threshold, 26))
        assertEquals(HapticFeedbackConstants.LONG_PRESS, SplitMateHaptics.platformConstant(Feel.LongPress, 26))
        assertEquals(HapticFeedbackType.SegmentTick, SplitMateHaptics.composeType(Feel.Tick))
        assertEquals(HapticFeedbackType.Confirm, SplitMateHaptics.composeType(Feel.Confirm))
        assertEquals(HapticFeedbackType.Reject, SplitMateHaptics.composeType(Feel.Reject))
    }

    @Test
    fun `no tap feels like a text cursor and nothing bypasses the system touch feedback setting`() {
        allUiSources().forEach { (path, code) ->
            assertFalse(code.contains("HapticFeedbackType.TextHandleMove"), "$path: taps use Tick, not TextHandleMove")
            assertFalse(code.contains("FLAG_IGNORE_GLOBAL_SETTING"), "$path: respect the system setting")
        }
        val theme = src("ui/SplitMateTheme.kt")
        assertFalse(theme.contains("VibrationEffect.createOneShot"), "no raw vibrator pulse on top of the view haptic")
        assertTrue(theme.contains("ProvideSplitMateHaptics(content)"), "Compose haptics honour the in-app switch")
    }

    @Test
    fun `money moments use the right words`() {
        val app = src("ui/SplitMateAppComposable.kt")
        val trip = src("ui/screens/TripHomeScreen.kt")
        val quick = src("ui/screens/QuickExpenseAndGuideScreens.kt")
        assertTrue(app.contains("rememberPaidMorphController()") && trip.contains("rememberPaidMorphController()"))
        assertTrue(app.contains("HapticFeedbackType.GestureThresholdActivate"), "swipe threshold")
        assertTrue(quick.contains("!canCommitSplit -> com.splitmate.app.ui.feel.Feel.Reject"), "blocked save")
        assertTrue(trip.contains("feel = com.splitmate.app.ui.feel.Feel.Reject"), "blocked mark paid")
        assertFalse(app.contains("₹0.00 · Settled"))
    }

    @Test
    fun `harmony seal plays once and has no wavy ring`() {
        val app = src("ui/SplitMateAppComposable.kt")
        assertFalse(app.contains("HarmonySealMorphTransition"), "no infinite loop")
        assertTrue(app.contains("harmonySealPlayed"))
        assertFalse(app.contains("CircularWavyProgressIndicator("), "no wavy ring on static money data")
    }

    private class FakeHaptics : HapticFeedback {
        val played = mutableListOf<HapticFeedbackType>()
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) { played += hapticFeedbackType }
    }

    @Test
    fun `mark paid saves at tap time, then shows the chip with one Confirm`() {
        val scope = TestScope(StandardTestDispatcher())
        val haptics = FakeHaptics()
        val controller = PaidMorphController(scope, haptics, PaidChipHoldMillis)
        var saves = 0

        controller.trigger { saves++; true }
        assertEquals(1, saves, "saved immediately, the chip is only visual")
        controller.trigger { saves++; true } // a second tap while the chip shows is ignored
        assertEquals(1, saves)
        assertTrue(controller.isPaid)
        assertEquals(listOf(HapticFeedbackType.Confirm), haptics.played)

        scope.advanceTimeBy(PaidChipHoldMillis + 1)
        scope.runCurrent()
        assertFalse(controller.isPaid, "button returns if the row is still there")
        assertEquals(1, saves)
    }

    @Test
    fun `a refused save plays Reject and never shows the Paid chip`() {
        val scope = TestScope(StandardTestDispatcher())
        val haptics = FakeHaptics()
        val controller = PaidMorphController(scope, haptics, PaidChipHoldMillis)
        controller.trigger { false }
        assertFalse(controller.isPaid)
        assertEquals(listOf(HapticFeedbackType.Reject), haptics.played)
    }

    @Test
    fun `paid morph has no delayed or dispose-time save and settlement ids are unique`() {
        val morph = src("ui/feel/PaidMorph.kt")
        assertFalse(morph.contains("DisposableEffect"), "no save from onDispose")
        assertFalse(morph.contains("pending"), "no held save")
        val vm = src("ui/SplitMateViewModel.kt")
        assertTrue(vm.contains("fun markGreedyTransferSettled(transfer: SplitMateMathEngine.SimplifiedTransfer): Boolean"))
        assertTrue(vm.contains("settlementId = \"settle_\${System.currentTimeMillis()}_\${java.util.UUID.randomUUID().toString().take(8)}\""))
    }
}
