package com.splitmate.app.ui.feel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import com.splitmate.app.ui.components.SplitMateMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long the "Paid" chip stays if the row is still on screen after the save. */
const val PaidChipHoldMillis: Long = 1_800L

/**
 * v2.4.0 D4: "Mark paid" becomes a "Paid" chip with a Confirm haptic.
 *
 * Money safety (review B1-B4): the payment is saved at tap time, exactly as before D4; the chip is
 * purely visual. [trigger] calls [commit] immediately and only shows the chip and plays Confirm
 * when the save was accepted. A refused save (not allowed, already recorded) plays Reject and the
 * button stays. The row then fades out of the list as the balances update.
 */
@Stable
class PaidMorphController internal constructor(
    private val scope: CoroutineScope,
    private val haptic: HapticFeedback,
    private val chipHoldMillis: Long
) {
    var isPaid by mutableStateOf(false)
        private set

    /** Saves now via [commit] (true = saved). A second tap while the chip shows is ignored. */
    fun trigger(commit: () -> Boolean) {
        if (isPaid) return
        if (!commit()) {
            haptic.feel(Feel.Reject)
            return
        }
        isPaid = true
        haptic.feel(Feel.Confirm)
        scope.launch {
            // If the row is still here (e.g. a partial plan update), offer the button again.
            delay(chipHoldMillis)
            isPaid = false
        }
    }
}

@Composable
fun rememberPaidMorphController(chipHoldMillis: Long = PaidChipHoldMillis): PaidMorphController {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    return remember { PaidMorphController(scope, haptic, chipHoldMillis) }
}

/**
 * Shows [button] until [isPaid], then morphs (spring scale + fade) into a "Paid" chip.
 * Under "Remove animations" the springs finish instantly, so it snaps to the chip.
 */
@Composable
fun PaidMorph(
    isPaid: Boolean,
    modifier: Modifier = Modifier,
    label: String = "Paid",
    contentAlignment: Alignment = Alignment.CenterEnd,
    button: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = isPaid,
        modifier = modifier,
        contentAlignment = contentAlignment,
        transitionSpec = {
            (fadeIn(SplitMateMotion.fastEffects()) + scaleIn(SplitMateMotion.defaultSpatial(), initialScale = 0.8f))
                .togetherWith(fadeOut(SplitMateMotion.fastEffects()))
                .using(SizeTransform(clip = false) { _, _ -> SplitMateMotion.defaultSpatial() })
        },
        label = "PaidMorph"
    ) { paid ->
        if (paid) PaidChip(label = label) else button()
    }
}

@Composable
fun PaidChip(label: String = "Paid", modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
            .heightIn(min = 40.dp)
            .clearAndSetSemantics {
                contentDescription = "Marked as paid"
                liveRegion = LiveRegionMode.Polite
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
