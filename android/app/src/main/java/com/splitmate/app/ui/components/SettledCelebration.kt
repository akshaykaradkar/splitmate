package com.splitmate.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SettledCelebrationDefaults {
    /** Bouncy spatial spring for the burst: it overshoots, then settles. */
    const val BurstDampingRatio = 0.45f
    const val BurstStiffness = Spring.StiffnessMediumLow
    const val InitialScale = 0.4f
    const val InitialRotationDegrees = -120f
    /** The check pops in once the burst is mostly open. */
    const val CheckDelayMillis = 140L

    /** Celebrate only the moment the last payment clears, never just for opening a settled trip. */
    fun shouldCelebrate(hadTransfers: Boolean, hasTransfersNow: Boolean): Boolean =
        hadTransfers && !hasTransfersNow
}

/**
 * v2.3.6 Step C: the small "all settled" moment.
 *
 * When [celebrate] is true, a circle morphs into the M3 Expressive soft-burst shape on a bouncy
 * spring (scale, rotation and shape together) with one confirm haptic. A check then pops in.
 * When [celebrate] is false, or under reduced motion, the final badge is drawn statically, so
 * opening an already-settled trip stays calm.
 */
@Composable
fun SettledCelebrationBadge(
    celebrate: Boolean,
    containerColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    morph: Morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.SoftBurst) }
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val haptic = LocalHapticFeedback.current
    val animate = celebrate && !reducedMotion
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    val checkScale = remember { Animatable(if (animate) 0f else 1f) }

    LaunchedEffect(celebrate) {
        if (!celebrate) return@LaunchedEffect
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        if (reducedMotion) return@LaunchedEffect
        val burst = spring<Float>(
            dampingRatio = SettledCelebrationDefaults.BurstDampingRatio,
            stiffness = SettledCelebrationDefaults.BurstStiffness
        )
        coroutineScope {
            // The trigger usually lands a frame after the badge first composes, so restart from
            // the small circle explicitly.
            progress.snapTo(0f)
            checkScale.snapTo(0f)
            launch { progress.animateTo(1f, burst) }
            launch {
                delay(SettledCelebrationDefaults.CheckDelayMillis)
                checkScale.animateTo(1f, burst)
            }
        }
    }

    val p = progress.value
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val scale = SettledCelebrationDefaults.InitialScale + (1f - SettledCelebrationDefaults.InitialScale) * p
                scaleX = scale
                scaleY = scale
            }
            .clip(
                MorphPolygonShape(
                    morph = morph,
                    percentage = p,
                    rotationDegrees = SettledCelebrationDefaults.InitialRotationDegrees * (1f - p)
                )
            )
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .size(size * 0.6f)
                .graphicsLayer {
                    scaleX = checkScale.value
                    scaleY = checkScale.value
                }
        )
    }
}
