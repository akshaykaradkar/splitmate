package com.splitmate.app.ui.feel

import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * v2.4.0 D4: SplitMate's haptic vocabulary (Design Bible section 9).
 *
 * Every haptic in the app is one of these words, so the same kind of moment always feels the same:
 * - [Tick]: tabs, chips, segment and filter switches, FAB menu items, keypad keys, navigation taps.
 * - [Confirm]: a money action succeeded (mark paid, save an expense, split the remainder, wrap up).
 * - [Reject]: the action was blocked (form not ready, not allowed, money check warning).
 * - [Threshold]: a swipe crossed its commit point.
 * - [LongPress]: real long presses only.
 *
 * Haptics go through `View.performHapticFeedback` (crisp, tuned per device, and it respects the
 * system "Touch feedback" setting) and the in-app "Haptics" switch (`pref_haptics`).
 */
enum class Feel { Tick, Confirm, Reject, Threshold, LongPress }

object SplitMateHaptics {
    const val PREF_KEY = "pref_haptics"

    /** In-app switch (Settings > Haptics). Defaults to on. */
    fun isEnabled(context: Context): Boolean = runCatching {
        com.splitmate.app.data.EncryptedPrefsProvider.get(context).getBoolean(PREF_KEY, true)
    }.getOrDefault(true)

    /**
     * Platform constant for [feel] on [sdkInt]. Newer, more specific constants are used when the
     * OS has them (CONFIRM/REJECT on 30+, SEGMENT_TICK/GESTURE_THRESHOLD_ACTIVATE on 34+), with
     * the closest older constant as fallback. Pure so it can be unit-tested.
     */
    fun platformConstant(feel: Feel, sdkInt: Int = Build.VERSION.SDK_INT): Int = when (feel) {
        Feel.Tick -> if (sdkInt >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK
        Feel.Confirm -> if (sdkInt >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
        Feel.Reject -> if (sdkInt >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        Feel.Threshold -> if (sdkInt >= 34) HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE else HapticFeedbackConstants.CONTEXT_CLICK
        Feel.LongPress -> HapticFeedbackConstants.LONG_PRESS
    }

    /** Compose equivalent of [feel] (Compose applies the same SDK fallbacks internally). */
    fun composeType(feel: Feel): HapticFeedbackType = when (feel) {
        Feel.Tick -> HapticFeedbackType.SegmentTick
        Feel.Confirm -> HapticFeedbackType.Confirm
        Feel.Reject -> HapticFeedbackType.Reject
        Feel.Threshold -> HapticFeedbackType.GestureThresholdActivate
        Feel.LongPress -> HapticFeedbackType.LongPress
    }

    /** Plays [feel] on [view] if haptics are on. Never throws. */
    fun perform(context: Context, view: View?, feel: Feel) {
        if (!isEnabled(context)) return
        val target = view ?: return
        runCatching { target.performHapticFeedback(platformConstant(feel)) }
    }
}

/** Compose [HapticFeedback] that honours the in-app Haptics switch before delegating. */
private class PrefGatedHapticFeedback(
    private val context: Context,
    private val delegate: HapticFeedback
) : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        if (SplitMateHaptics.isEnabled(context)) delegate.performHapticFeedback(hapticFeedbackType)
    }
}

/** Installs the pref-gated [LocalHapticFeedback] for everything inside [content]. */
@Composable
fun ProvideSplitMateHaptics(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val platform = LocalHapticFeedback.current
    val gated = remember(context, platform) { PrefGatedHapticFeedback(context.applicationContext, platform) }
    CompositionLocalProvider(LocalHapticFeedback provides gated, content = content)
}

/** `haptic.feel(Feel.Confirm)`: vocabulary-first call for Compose sites. */
fun HapticFeedback.feel(feel: Feel) = performHapticFeedback(SplitMateHaptics.composeType(feel))
