package com.splitmate.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * v2.3.6 Step C: one action revealed behind a row while it is swiped.
 *
 * [onTrigger] should open a sheet or a confirmation, never change money data directly, so a
 * stray swipe can't delete a booking or record a payment.
 */
data class ExpressiveSwipeAction(
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val onTrigger: () -> Unit
)

/** Which side's action a released swipe resolves to. */
enum class ExpressiveSwipeSide { Start, End }

object ExpressiveSwipeActionsDefaults {
    /** Drag distance that arms an action (capped at [ThresholdWidthFraction] of the row). */
    val ActionThreshold = 96.dp
    const val ThresholdWidthFraction = 0.4f
    /** Beyond the threshold the row follows the finger at this rate (rubber band). */
    const val OverdragResistance = 0.35f
    /** The row can never travel further than this fraction of its width. */
    const val MaxTravelWidthFraction = 0.6f

    /** Effective threshold in px for a row [widthPx] wide. */
    fun thresholdPx(actionThresholdPx: Float, widthPx: Int): Float =
        if (widthPx <= 0) actionThresholdPx else minOf(actionThresholdPx, widthPx * ThresholdWidthFraction)

    /**
     * Next offset for a drag [delta] (positive = towards the end edge). Sides without an action
     * don't move; past the threshold the drag is damped, and travel is capped.
     */
    fun applyDrag(
        current: Float,
        delta: Float,
        thresholdPx: Float,
        maxTravelPx: Float,
        hasStartAction: Boolean,
        hasEndAction: Boolean
    ): Float {
        val damped = if (abs(current) >= thresholdPx && abs(current + delta) > abs(current)) {
            delta * OverdragResistance
        } else {
            delta
        }
        val lower = if (hasEndAction) -maxTravelPx else 0f
        val upper = if (hasStartAction) maxTravelPx else 0f
        return (current + damped).coerceIn(lower, upper)
    }

    /**
     * The action a release at [offset] triggers. Swiping towards the end edge reveals the start
     * action and vice versa; null when the threshold wasn't reached.
     */
    fun resolve(offset: Float, thresholdPx: Float): ExpressiveSwipeSide? = when {
        thresholdPx <= 0f -> null
        offset >= thresholdPx -> ExpressiveSwipeSide.Start
        offset <= -thresholdPx -> ExpressiveSwipeSide.End
        else -> null
    }
}

/**
 * Swipe-to-reveal row.
 *
 * - The row follows the finger and reveals the action behind it.
 * - Crossing the threshold gives a haptic tick, and the action icon pops to a larger size.
 * - On release the row always springs back using the theme's spatial spring. If the threshold
 *   was crossed, the action then runs.
 *
 * Under reduced motion `MaterialTheme.motionScheme` snaps, so the return is instant. Both
 * actions are also TalkBack custom actions, so the gesture is never the only way in.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveSwipeActionsBox(
    modifier: Modifier = Modifier,
    startAction: ExpressiveSwipeAction? = null,
    endAction: ExpressiveSwipeAction? = null,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit
) {
    if (startAction == null && endAction == null) {
        Box(modifier = modifier) { content() }
        return
    }
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val returnSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val offset = remember { Animatable(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    val actionThresholdPx = with(density) { ExpressiveSwipeActionsDefaults.ActionThreshold.toPx() }
    val thresholdPx = ExpressiveSwipeActionsDefaults.thresholdPx(actionThresholdPx, widthPx)
    val maxTravelPx = if (widthPx > 0) widthPx * ExpressiveSwipeActionsDefaults.MaxTravelWidthFraction else actionThresholdPx * 2f
    val currentStart by rememberUpdatedState(startAction)
    val currentEnd by rememberUpdatedState(endAction)
    val armedSide by remember(thresholdPx) {
        derivedStateOf { ExpressiveSwipeActionsDefaults.resolve(offset.value, thresholdPx) }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { armedSide }
            .drop(1)
            .collect { side ->
                if (side != null) haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            }
    }

    val draggableState = rememberDraggableState { delta ->
        val next = ExpressiveSwipeActionsDefaults.applyDrag(
            current = offset.value,
            delta = delta,
            thresholdPx = thresholdPx,
            maxTravelPx = maxTravelPx,
            hasStartAction = currentStart != null,
            hasEndAction = currentEnd != null
        )
        scope.launch { offset.snapTo(next) }
    }

    Box(
        modifier = modifier
            .onSizeChanged { widthPx = it.width }
            .semantics {
                customActions = listOfNotNull(
                    startAction?.let { action -> CustomAccessibilityAction(action.label) { action.onTrigger(); true } },
                    endAction?.let { action -> CustomAccessibilityAction(action.label) { action.onTrigger(); true } }
                )
            }
    ) {
        val revealed = when {
            offset.value > 0f -> startAction?.let { it to ExpressiveSwipeSide.Start }
            offset.value < 0f -> endAction?.let { it to ExpressiveSwipeSide.End }
            else -> null
        }
        if (revealed != null) {
            ExpressiveSwipeActionBackground(
                action = revealed.first,
                side = revealed.second,
                isArmed = armedSide == revealed.second,
                revealFraction = if (thresholdPx > 0f) (abs(offset.value) / thresholdPx).coerceIn(0f, 1f) else 0f,
                shape = shape,
                modifier = Modifier.matchParentSize()
            )
        }
        Box(
            modifier = Modifier
                .graphicsLayer { translationX = if (isRtl) -offset.value else offset.value }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    reverseDirection = isRtl,
                    onDragStopped = { velocity ->
                        val side = ExpressiveSwipeActionsDefaults.resolve(offset.value, thresholdPx)
                        scope.launch {
                            offset.animateTo(0f, returnSpec, initialVelocity = if (isRtl) -velocity else velocity)
                        }
                        when (side) {
                            ExpressiveSwipeSide.Start -> currentStart?.onTrigger?.invoke()
                            ExpressiveSwipeSide.End -> currentEnd?.onTrigger?.invoke()
                            null -> Unit
                        }
                    }
                )
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveSwipeActionBackground(
    action: ExpressiveSwipeAction,
    side: ExpressiveSwipeSide,
    isArmed: Boolean,
    revealFraction: Float,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    val iconScale by animateFloatAsState(
        targetValue = if (isArmed) 1.25f else 0.9f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "ExpressiveSwipeIconScale"
    )
    Row(
        modifier = modifier
            .clip(shape)
            .background(action.containerColor)
            .padding(horizontal = 24.dp)
            .graphicsLayer { alpha = 0.4f + 0.6f * revealFraction },
        horizontalArrangement = if (side == ExpressiveSwipeSide.Start) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon: @Composable () -> Unit = {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = action.contentColor,
                modifier = Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale }
            )
        }
        val label: @Composable () -> Unit = {
            Text(text = action.label, style = MaterialTheme.typography.labelLarge, color = action.contentColor)
        }
        if (side == ExpressiveSwipeSide.Start) {
            icon(); Spacer(Modifier.width(8.dp)); label()
        } else {
            label(); Spacer(Modifier.width(8.dp)); icon()
        }
    }
}
