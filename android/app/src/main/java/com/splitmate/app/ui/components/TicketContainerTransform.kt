package com.splitmate.app.ui.components

import android.os.SystemClock
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * v2.3.6 Step C1: Material 3 **container transform** from a booking card into its ticket/detail
 * screen, with a **predictive back** gesture that previews the return (the detail shrinks back
 * into the card while the user drags, and springs back open if the gesture is cancelled).
 *
 * How it fits together:
 * - [TicketContainerTransformHost] wraps the dashboard. It keeps the dashboard composed underneath
 *   (so the Trip Hub tab, scroll position and collapsing header survive opening a ticket), and draws
 *   the ticket overlay above it, driven by one [SeekableTransitionState].
 * - [TicketContainerSource] wraps a booking card. Both sides register the same shared-bounds key, so
 *   the card's bounds morph into the full-screen detail (fade-through + scale-to-width content).
 * - The card marks itself on the [TicketContainerTransformController] right before it asks the app to
 *   open a ticket; the host reads that mark when the ticket opens. Tickets opened from anywhere else
 *   (FAB menu, Audit, a PDF pick) have no source and use a plain fade + scale enter/exit instead.
 * - Reduced motion: every spec comes from `MaterialTheme.motionScheme`, which SplitMate swaps for an
 *   instant scheme when `ANIMATOR_DURATION_SCALE == 0`, so the transform snaps.
 */
interface TicketContainerTarget {
    /** Stable id of the card that opened this ticket (e.g. an expense id), or null when sourceless. */
    val sourceKey: String?
}

/**
 * Remembers which card was tapped, for at most [SOURCE_TTL_MILLIS], so a stale mark (for example
 * a tap that did not end up opening a ticket) can never make an unrelated ticket morph from it.
 */
@Stable
class TicketContainerTransformController(
    private val clockMillis: () -> Long = { SystemClock.uptimeMillis() }
) {
    private var pendingSourceKey: String? = null
    private var pendingAtMillis: Long = 0L

    fun markSource(sourceKey: String) {
        pendingSourceKey = sourceKey
        pendingAtMillis = clockMillis()
    }

    /** Returns the fresh source mark (if any) and clears it. */
    fun takeSource(): String? {
        val key = pendingSourceKey
        val isFresh = key != null && clockMillis() - pendingAtMillis <= SOURCE_TTL_MILLIS
        pendingSourceKey = null
        return if (isFresh) key else null
    }

    companion object {
        const val SOURCE_TTL_MILLIS: Long = 1_000L
    }
}

object TicketContainerTransformDefaults {
    /** Card corner the container starts from (feed cards use the 20dp tier). */
    val SourceCornerRadius: Dp = 20.dp

    /** Scrim over the dashboard while a ticket is open (visible during the morph and the back preview). */
    const val ScrimAlpha: Float = 0.32f

    /** Scale used by the sourceless (no matching card) enter/exit. */
    const val SourcelessInitialScale: Float = 0.92f

    /**
     * Length of the linear timeline used while a back gesture scrubs the transform. Seeking maps
     * gesture progress proportionally onto the transition's timeline, so every participating
     * animation shares this one linear spec while scrubbing (springs would finish their short fades
     * early and the ticket would vanish halfway through the gesture).
     */
    const val BackSeekDurationMillis: Int = 400

    /** The furthest a back gesture scrubs (1 = all the way back into the card) before release. */
    const val BackSeekMaxFraction: Float = 0.85f
}

/**
 * Picks the theme's spring while the transform plays normally, and the shared linear timeline
 * while a back gesture is scrubbing it (see [TicketContainerTransformDefaults.BackSeekDurationMillis]).
 */
private fun <T> ticketTransformSpec(
    isBackSeeking: Boolean,
    spring: () -> FiniteAnimationSpec<T>
): FiniteAnimationSpec<T> = if (isBackSeeking) {
    tween(durationMillis = TicketContainerTransformDefaults.BackSeekDurationMillis, easing = LinearEasing)
} else {
    spring()
}

/** Shared-bounds key used by both the card and the ticket overlay. */
fun ticketContainerSharedKey(sourceKey: String): String = "splitmate-ticket-container:$sourceKey"

@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
class TicketContainerTransformScope internal constructor(
    val sharedTransitionScope: SharedTransitionScope,
    val transition: Transition<TicketContainerTarget?>,
    val controller: TicketContainerTransformController,
    internal val cornerRadius: State<Dp>,
    internal val isBackSeeking: State<Boolean>
)

val LocalTicketContainerTransform = compositionLocalOf<TicketContainerTransformScope?> { null }

/** Rounded-rect overlay clip whose radius is read at draw time, so it morphs every frame. */
@OptIn(ExperimentalSharedTransitionApi::class)
private class MorphingCornerOverlayClip(
    private val cornerRadius: State<Dp>
) : SharedTransitionScope.OverlayClip {
    override fun getClipPath(
        sharedContentState: SharedTransitionScope.SharedContentState,
        bounds: Rect,
        layoutDirection: LayoutDirection,
        density: Density
    ): Path {
        val radiusPx = with(density) { cornerRadius.value.toPx() }
        return Path().apply { addRoundRect(RoundRect(bounds, CornerRadius(radiusPx, radiusPx))) }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TicketContainerTransformHost(
    target: TicketContainerTarget?,
    onDismiss: () -> Unit,
    controller: TicketContainerTransformController,
    modifier: Modifier = Modifier,
    overlay: @Composable (TicketContainerTarget) -> Unit,
    content: @Composable () -> Unit
) {
    val seekableState = remember { SeekableTransitionState(target) }
    // True from the first back-gesture event until the transform settles again (commit or cancel).
    val isBackSeekingState = remember { mutableStateOf(false) }
    var isBackSeeking by isBackSeekingState
    LaunchedEffect(target) {
        seekableState.animateTo(target)
        isBackSeeking = false
    }
    val transition = rememberTransition(seekableState, label = "TicketContainerTransform")
    val coroutineScope = rememberCoroutineScope()

    val motion = MaterialTheme.motionScheme
    val cornerRadius = transition.animateDp(
        transitionSpec = { ticketTransformSpec(isBackSeeking) { motion.defaultSpatialSpec() } },
        label = "TicketContainerCorner"
    ) { state -> if (state != null) 0.dp else TicketContainerTransformDefaults.SourceCornerRadius }
    val scrimAlpha by transition.animateFloat(
        transitionSpec = { ticketTransformSpec(isBackSeeking) { motion.defaultEffectsSpec() } },
        label = "TicketContainerScrim"
    ) { state -> if (state != null) TicketContainerTransformDefaults.ScrimAlpha else 0f }
    val scrimColor = MaterialTheme.colorScheme.scrim
    val overlayBackground = MaterialTheme.colorScheme.background

    SharedTransitionLayout(modifier = modifier) {
        val hostScope = remember(this, transition, controller) {
            TicketContainerTransformScope(this, transition, controller, cornerRadius, isBackSeekingState)
        }
        CompositionLocalProvider(LocalTicketContainerTransform provides hostScope) {
            // Once the ticket fully covers the dashboard, hide the dashboard from accessibility
            // services (it used to be removed from composition entirely).
            val isTicketSettledOpen = transition.currentState != null &&
                transition.targetState != null && !transition.isRunning
            Box(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isTicketSettledOpen) Modifier.clearAndSetSemantics { } else Modifier)
                ) {
                    content()
                }

                if (scrimAlpha > 0f) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawBehind { drawRect(scrimColor, alpha = scrimAlpha) }
                    )
                }

                val shown = transition.targetState ?: transition.currentState
                val hasSource = shown?.sourceKey != null
                transition.AnimatedVisibility(
                    visible = { it != null },
                    enter = if (hasSource) EnterTransition.None else {
                        fadeIn(motion.defaultEffectsSpec()) +
                            scaleIn(motion.defaultSpatialSpec(), TicketContainerTransformDefaults.SourcelessInitialScale)
                    },
                    exit = if (hasSource) ExitTransition.None else {
                        fadeOut(motion.fastEffectsSpec()) +
                            scaleOut(motion.defaultSpatialSpec(), TicketContainerTransformDefaults.SourcelessInitialScale)
                    }
                ) {
                    val rendered = transition.targetState ?: transition.currentState
                    if (rendered != null) {
                        val sourceKey = rendered.sourceKey
                        val sharedModifier = if (sourceKey != null) {
                            Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(ticketContainerSharedKey(sourceKey)),
                                animatedVisibilityScope = this,
                                enter = fadeIn(ticketTransformSpec(isBackSeeking) { motion.defaultEffectsSpec() }),
                                exit = fadeOut(ticketTransformSpec(isBackSeeking) { motion.fastEffectsSpec() }),
                                boundsTransform = { _, _ ->
                                    ticketTransformSpec(isBackSeekingState.value) { motion.defaultSpatialSpec() }
                                },
                                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                                    ContentScale.FillWidth,
                                    Alignment.TopCenter
                                ),
                                clipInOverlayDuringTransition = MorphingCornerOverlayClip(cornerRadius)
                            )
                        } else {
                            Modifier
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(sharedModifier)
                                .background(overlayBackground)
                                // Blocks touches from reaching the dashboard underneath.
                                .pointerInput(Unit) {
                                    awaitPointerEventScope { while (true) awaitPointerEvent() }
                                }
                        ) {
                            overlay(rendered)
                        }
                    }
                }
            }

            // Composed last (and only while a ticket is open) so it outranks every BackHandler in the
            // dashboard. Dragging seeks the transform toward the card; releasing commits, cancelling
            // springs the ticket back open.
            if (target != null) {
                PredictiveBackHandler { backEvents ->
                    try {
                        backEvents.collect { event ->
                            isBackSeeking = true
                            val fraction = event.progress.coerceIn(0f, 1f) *
                                TicketContainerTransformDefaults.BackSeekMaxFraction
                            seekableState.seekTo(fraction, targetState = null)
                        }
                        onDismiss()
                    } catch (cancelled: CancellationException) {
                        coroutineScope.launch {
                            seekableState.animateTo(seekableState.currentState)
                            isBackSeeking = false
                        }
                        throw cancelled
                    }
                }
            }
        }
    }
}

/**
 * Wraps a booking card so it can morph into its ticket. While its own ticket is fully open the card
 * leaves composition, but its last measured size is kept so the list never jumps.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TicketContainerSource(
    sourceKey: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val host = LocalTicketContainerTransform.current
    if (host == null) {
        Box(modifier) { content() }
        return
    }
    val motion = MaterialTheme.motionScheme
    val lastSize = remember { IntArray(2) }
    Box(
        modifier = modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            if (placeable.width > 0 && placeable.height > 0) {
                lastSize[0] = placeable.width
                lastSize[1] = placeable.height
            }
            val keepLast = placeable.height == 0 && lastSize[1] > 0
            val width = if (keepLast) lastSize[0].coerceIn(constraints.minWidth, constraints.maxWidth) else placeable.width
            val height = if (keepLast) lastSize[1].coerceIn(constraints.minHeight, constraints.maxHeight) else placeable.height
            layout(width, height) { placeable.place(0, 0) }
        }
    ) {
        host.transition.AnimatedVisibility(
            visible = { it?.sourceKey != sourceKey },
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            with(host.sharedTransitionScope) {
                Box(
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(ticketContainerSharedKey(sourceKey)),
                        animatedVisibilityScope = this@AnimatedVisibility,
                        enter = fadeIn(ticketTransformSpec(host.isBackSeeking.value) { motion.fastEffectsSpec() }),
                        exit = fadeOut(ticketTransformSpec(host.isBackSeeking.value) { motion.fastEffectsSpec() }),
                        boundsTransform = { _, _ ->
                            ticketTransformSpec(host.isBackSeeking.value) { motion.defaultSpatialSpec() }
                        },
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                            ContentScale.FillWidth,
                            Alignment.TopCenter
                        ),
                        clipInOverlayDuringTransition = MorphingCornerOverlayClip(host.cornerRadius)
                    )
                ) {
                    content()
                }
            }
        }
    }
}
