package com.splitmate.app.ui.components

import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.role
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.drawWithContent
import androidx.graphics.shapes.Morph
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.graphics.shapes.RoundedPolygon
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.splitmate.app.ui.FigtreeFontFamily
import com.splitmate.app.ui.LocalSplitMatePalette
import com.splitmate.app.ui.SplitMateExpressiveTypography
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

// v2.4.0 rc3: the local `ExperimentalMaterial3ExpressiveApi` look-alike marker is gone. material3 is
// on 1.5.0-alpha14, so every wrapper in this file carries the official
// `androidx.compose.material3.ExperimentalMaterial3ExpressiveApi` (opted in module-wide through
// `-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi` in android/app/build.gradle)
// and delegates to the official Material 3 Expressive implementation.

// ==============================================================================
// 1. M3 EXPRESSIVE WAVY & GAP PROGRESS INDICATORS
// ==============================================================================

object WavyProgressIndicatorDefaults {
    val indicatorColor: Color
        @Composable get() = MaterialTheme.colorScheme.tertiary

    val trackColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant

    /** Duration of one indeterminate cycle (two travelling segments), in milliseconds. */
    const val INDETERMINATE_CYCLE_MILLIS: Int = 1750

    /** Cycle window, as fractions of one cycle, during which each indeterminate segment travels. */
    const val FIRST_SEGMENT_WINDOW_END: Float = 0.75f
    const val SECOND_SEGMENT_WINDOW_START: Float = 0.45f

    /** Segments shorter than this fraction of the track are not drawn. */
    const val MIN_SEGMENT_FRACTION: Float = 0.001f

    /** Minimum on-screen time for an in-flight wavy bar, to avoid flicker (audit 5.3). */
    const val MIN_VISIBLE_MILLIS: Long = 400L

    /** Static, flat segment drawn by the indeterminate bar when reduced motion is on. */
    val ReducedMotionSegment: WavySegment = WavySegment(start = 0f, end = 0.4f)
}

/** Round-capped [Stroke] of [width] in px, as the official wavy indicators expect. */
@Composable
private fun rememberRoundStroke(width: Dp): Stroke {
    val density = LocalDensity.current
    val widthPx = with(density) { width.toPx() }.coerceAtLeast(2f)
    return remember(widthPx) { Stroke(width = widthPx, cap = StrokeCap.Round) }
}

/**
 * Material 3 Expressive `LinearWavyProgressIndicator` with sinusoidal wave animation,
 * rounded stroke caps (`StrokeCap.Round`), `4.dp` active-to-track gap, and `4.dp` end-of-track stop dot.
 * Wave amplitude smoothly dampens to `0f` when progress reaches `1f` (`100%` settled or `0.00c`
 * receipt equilibrium) or `0f`, and stays flat under reduced motion.
 *
 * v2.4.0 rc3: delegates to the official `androidx.compose.material3.LinearWavyProgressIndicator`
 * (the library draws the wave, gap, stop dot and animates amplitude changes). This wrapper keeps the
 * SplitMate signature, the spring-animated progress/colour and the reduced-motion flattening.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun LinearWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    amplitude: Float = 1f,
    wavelength: Dp = 24.dp,
    waveSpeed: Dp = 24.dp,
    gapSize: Dp = 4.dp,
    stopSize: Dp = 4.dp,
    strokeWidth: Dp = 6.dp
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val rawProgress = progress().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = SplitMateMotion.defaultSpatial(),
        label = "LinearWavyProgress"
    )
    // The official indicator animates amplitude changes itself (Increasing/DecreasingAmplitude specs).
    val targetAmplitude = if (rawProgress <= 0.001f || rawProgress >= 0.999f) {
        0f
    } else if (reducedMotion) {
        0f
    } else {
        amplitude.coerceIn(0f, 1f)
    }
    val animatedActiveColor by animateColorAsState(
        targetValue = color,
        animationSpec = SplitMateMotion.defaultEffects(),
        label = "LinearWavyActiveColor"
    )
    val animatedTrackColor by animateColorAsState(
        targetValue = trackColor,
        animationSpec = SplitMateMotion.defaultEffects(),
        label = "LinearWavyTrackColor"
    )
    val stroke = rememberRoundStroke(strokeWidth)

    androidx.compose.material3.LinearWavyProgressIndicator(
        progress = { animatedProgress },
        // Same footprint as before: full width, stroke + 8.dp tall (the wave uses the spare height).
        modifier = modifier
            .fillMaxWidth()
            .height(strokeWidth + 8.dp),
        color = animatedActiveColor,
        trackColor = animatedTrackColor,
        stroke = stroke,
        trackStroke = stroke,
        gapSize = gapSize,
        stopSize = stopSize,
        amplitude = { targetAmplitude },
        wavelength = wavelength,
        waveSpeed = waveSpeed
    )
}

@ExperimentalMaterial3ExpressiveApi
@Composable
fun LinearWavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    amplitude: Float = 1f,
    wavelength: Dp = 24.dp,
    waveSpeed: Dp = 24.dp,
    gapSize: Dp = 4.dp,
    stopSize: Dp = 4.dp,
    strokeWidth: Dp = 6.dp
) {
    LinearWavyProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        amplitude = amplitude,
        wavelength = wavelength,
        waveSpeed = waveSpeed,
        gapSize = gapSize,
        stopSize = stopSize,
        strokeWidth = strokeWidth
    )
}

/**
 * INDETERMINATE Material 3 Expressive `LinearWavyProgressIndicator` (no `progress` parameter).
 *
 * v2.4.0 rc3: delegates to the official indeterminate
 * `androidx.compose.material3.LinearWavyProgressIndicator` (the library's travelling segments and
 * wave). When reduced motion is enabled (`ANIMATOR_DURATION_SCALE == 0`) nothing moves: a static,
 * flat [WavyProgressIndicatorDefaults.ReducedMotionSegment] is drawn by the official determinate bar
 * (amplitude 0, no stop dot) under indeterminate progress semantics.
 *
 * Usage rule (GEMINI.md v2.3.4 addendum): ONLY for in-flight network work. Prefer
 * [InFlightWavyProgressIndicator], which adds the 400ms minimum visibility and the exit animation.
 *
 * @param contentDescription TalkBack label (e.g. "Loading guide"); announced politely when set.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun LinearWavyProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    amplitude: Float = 1f,
    wavelength: Dp = 24.dp,
    waveSpeed: Dp = 24.dp,
    gapSize: Dp = 4.dp,
    strokeWidth: Dp = 6.dp,
    contentDescription: String? = null
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val effectiveAmplitude by animateFloatAsState(
        targetValue = if (reducedMotion) 0f else amplitude.coerceIn(0f, 1f),
        animationSpec = SplitMateMotion.defaultSpatial(),
        label = "LinearWavyIndeterminateAmplitude"
    )
    val animatedActiveColor by animateColorAsState(
        targetValue = color,
        animationSpec = SplitMateMotion.defaultEffects(),
        label = "LinearWavyIndeterminateActiveColor"
    )
    val animatedTrackColor by animateColorAsState(
        targetValue = trackColor,
        animationSpec = SplitMateMotion.defaultEffects(),
        label = "LinearWavyIndeterminateTrackColor"
    )
    val stroke = rememberRoundStroke(strokeWidth)
    val description = contentDescription

    if (reducedMotion) {
        // Static frame: the official determinate bar, flat, frozen at the reduced-motion segment.
        // The wrapper owns the semantics so TalkBack still hears an indeterminate loader.
        val segment = WavyProgressIndicatorDefaults.ReducedMotionSegment
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clearAndSetSemantics {
                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                    if (description != null) {
                        this.contentDescription = description
                        liveRegion = LiveRegionMode.Polite
                    }
                }
        ) {
            androidx.compose.material3.LinearWavyProgressIndicator(
                progress = { segment.end },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth + 8.dp),
                color = animatedActiveColor,
                trackColor = animatedTrackColor,
                stroke = stroke,
                trackStroke = stroke,
                gapSize = gapSize,
                stopSize = 0.dp,
                amplitude = { 0f },
                wavelength = wavelength,
                waveSpeed = 0.dp
            )
        }
        return
    }

    val labelModifier = if (description != null) {
        Modifier.semantics {
            this.contentDescription = description
            liveRegion = LiveRegionMode.Polite
        }
    } else {
        Modifier
    }
    androidx.compose.material3.LinearWavyProgressIndicator(
        modifier = modifier
            .progressSemantics()
            .then(labelModifier)
            .fillMaxWidth()
            .height(strokeWidth + 8.dp),
        color = animatedActiveColor,
        trackColor = animatedTrackColor,
        stroke = stroke,
        trackStroke = stroke,
        gapSize = gapSize,
        amplitude = effectiveAmplitude,
        wavelength = wavelength,
        waveSpeed = waveSpeed
    )
}

/** One active segment of an indeterminate bar, as fractions `[start, end]` of the track (0..1). */
data class WavySegment(val start: Float, val end: Float) {
    val length: Float get() = end - start
}

/**
 * Pure, deterministic geometry of the indeterminate bar at [cycleFraction] (wrapped into 0..1).
 *
 * v2.4.0 rc3: the running bar is now drawn by the official indeterminate
 * `LinearWavyProgressIndicator`; this pure model is kept as the documented reference for the
 * edge-to-edge segment behaviour (and its unit tests).
 *
 * Two segments travel edge-to-edge in overlapping windows of the cycle (`[0, 0.75]` and
 * `[0.45, 1]`). Inside its window a segment's head follows `1 - (1 - u)^2` (decelerating) and its
 * tail follows `u^2` (accelerating), so it grows from the start edge, peaks at half the track and
 * shrinks into the end edge. Result: at most 2 segments, sorted by start, never overlapping,
 * all within 0..1. Non-finite input returns an empty list.
 */
fun indeterminateWavySegments(cycleFraction: Float): List<WavySegment> {
    if (cycleFraction.isNaN() || cycleFraction.isInfinite()) return emptyList()
    val t = cycleFraction.mod(1f)
    return listOfNotNull(
        indeterminateWindowSegment(t, 0f, WavyProgressIndicatorDefaults.FIRST_SEGMENT_WINDOW_END),
        indeterminateWindowSegment(t, WavyProgressIndicatorDefaults.SECOND_SEGMENT_WINDOW_START, 1f)
    ).sortedBy { it.start }
}

private fun indeterminateWindowSegment(t: Float, windowStart: Float, windowEnd: Float): WavySegment? {
    if (windowEnd <= windowStart || t < windowStart || t > windowEnd) return null
    val u = ((t - windowStart) / (windowEnd - windowStart)).coerceIn(0f, 1f)
    val head = 1f - (1f - u) * (1f - u)
    val tail = u * u
    if (head - tail < WavyProgressIndicatorDefaults.MIN_SEGMENT_FRACTION) return null
    return WavySegment(start = tail.coerceIn(0f, 1f), end = head.coerceIn(0f, 1f))
}

/**
 * Pure rule behind [rememberMinimumVisibility]: how much longer (ms) an indicator shown at
 * [shownAtMillis] must stay visible at [nowMillis] to honour [minVisibleMillis]. Clamped to
 * `0..minVisibleMillis` (a clock that runs backwards never extends the wait beyond the minimum).
 */
fun remainingMinVisibleMillis(shownAtMillis: Long, nowMillis: Long, minVisibleMillis: Long): Long {
    val minimum = minVisibleMillis.coerceAtLeast(0L)
    val elapsed = nowMillis - shownAtMillis
    return (minimum - elapsed).coerceIn(0L, minimum)
}

/**
 * Returns `true` while [active] is true, and keeps returning `true` after [active] turns false
 * until the indicator has been visible for at least [minVisibleMillis] (anti-flicker rule).
 */
@Composable
fun rememberMinimumVisibility(
    active: Boolean,
    minVisibleMillis: Long = WavyProgressIndicatorDefaults.MIN_VISIBLE_MILLIS
): Boolean {
    var visible by remember { mutableStateOf(active) }
    var shownAtMillis by remember { mutableLongStateOf(if (active) SystemClock.uptimeMillis() else 0L) }
    LaunchedEffect(active, minVisibleMillis) {
        if (active) {
            if (!visible) {
                shownAtMillis = SystemClock.uptimeMillis()
                visible = true
            }
        } else if (visible) {
            val wait = remainingMinVisibleMillis(shownAtMillis, SystemClock.uptimeMillis(), minVisibleMillis)
            if (wait > 0L) delay(wait)
            visible = false
        }
    }
    return visible
}

/**
 * The sanctioned Trip Guide wait-state bar: an indeterminate [LinearWavyProgressIndicator] that is
 * visible while [inFlight] (an in-flight network job), stays up for at least [minVisibleMillis]
 * and animates out with `shrinkVertically + fadeOut` ([SplitMateMotion.fastEffects]).
 * Never drive [inFlight] from cached or offline data.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun InFlightWavyProgressIndicator(
    inFlight: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    minVisibleMillis: Long = WavyProgressIndicatorDefaults.MIN_VISIBLE_MILLIS,
    contentDescription: String? = "Loading"
) {
    val visible = rememberMinimumVisibility(active = inFlight, minVisibleMillis = minVisibleMillis)
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = SplitMateMotion.fastEffects()) +
            expandVertically(animationSpec = SplitMateMotion.fastEffects()),
        exit = shrinkVertically(animationSpec = SplitMateMotion.fastEffects()) +
            fadeOut(animationSpec = SplitMateMotion.fastEffects()),
        label = "InFlightWavyProgressVisibility"
    ) {
        LinearWavyProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = trackColor,
            contentDescription = contentDescription
        )
    }
}

/**
 * Material 3 Expressive `CircularWavyProgressIndicator` for Trip Harmony settlement meters
 * and per-traveler contribution rings.
 *
 * v2.4.0 rc3: delegates to the official `androidx.compose.material3.CircularWavyProgressIndicator`
 * (48.dp container, as before). The wave flattens at 0% / 100% and under reduced motion.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun CircularWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    amplitude: Float = 1f,
    wavelength: Dp = 18.dp,
    strokeWidth: Dp = 5.dp,
    gapSize: Dp = 4.dp
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val rawProgress = progress().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = SplitMateMotion.defaultSpatial(),
        label = "CircularWavyProgress"
    )
    val targetAmplitude = if (rawProgress <= 0.001f || rawProgress >= 0.999f) {
        0f
    } else if (reducedMotion) {
        0f
    } else {
        amplitude.coerceIn(0f, 1f)
    }
    val animatedActiveColor by animateColorAsState(
        targetValue = color,
        animationSpec = SplitMateMotion.defaultEffects(),
        label = "CircularWavyActiveColor"
    )
    val animatedTrackColor by animateColorAsState(
        targetValue = trackColor,
        animationSpec = SplitMateMotion.defaultEffects(),
        label = "CircularWavyTrackColor"
    )
    val stroke = rememberRoundStroke(strokeWidth)

    androidx.compose.material3.CircularWavyProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier.size(48.dp),
        color = animatedActiveColor,
        trackColor = animatedTrackColor,
        stroke = stroke,
        trackStroke = stroke,
        gapSize = gapSize,
        amplitude = { targetAmplitude },
        wavelength = wavelength
    )
}

@ExperimentalMaterial3ExpressiveApi
@Composable
fun CircularWavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    amplitude: Float = 1f,
    wavelength: Dp = 18.dp,
    strokeWidth: Dp = 5.dp,
    gapSize: Dp = 4.dp
) {
    CircularWavyProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        amplitude = amplitude,
        wavelength = wavelength,
        strokeWidth = strokeWidth,
        gapSize = gapSize
    )
}

/**
 * Material 3 Expressive flat linear progress indicator with rounded stroke caps,
 * `4.dp` active-to-track gap, and `4.dp` end-of-track stop dot.
 *
 * v2.4.0 rc3: delegates to the official
 * `androidx.compose.material3.LinearProgressIndicator(progress, gapSize, drawStopIndicator)`.
 * The bar keeps its previous footprint (stroke + 8.dp tall, centred stroke).
 */
@Composable
fun ExpressiveGapLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    gapSize: Dp = 4.dp,
    stopSize: Dp = 4.dp,
    strokeWidth: Dp = 6.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress().coerceIn(0f, 1f),
        animationSpec = SplitMateMotion.defaultSpatial(),
        label = "GapLinearProgress"
    )
    val animatedActiveColor by animateColorAsState(
        targetValue = color,
        animationSpec = SplitMateMotion.defaultEffects(),
        label = "GapLinearActiveColor"
    )
    val animatedTrackColor by animateColorAsState(
        targetValue = trackColor,
        animationSpec = SplitMateMotion.defaultEffects(),
        label = "GapLinearTrackColor"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(strokeWidth + 8.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(strokeWidth),
            color = animatedActiveColor,
            trackColor = animatedTrackColor,
            strokeCap = StrokeCap.Round,
            gapSize = gapSize,
            drawStopIndicator = {
                if (stopSize > 0.dp) {
                    androidx.compose.material3.ProgressIndicatorDefaults.drawStopIndicator(
                        drawScope = this,
                        stopSize = stopSize,
                        color = animatedActiveColor,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        )
    }
}

@Composable
fun ExpressiveGapLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    gapSize: Dp = 4.dp,
    stopSize: Dp = 4.dp,
    strokeWidth: Dp = 6.dp
) {
    ExpressiveGapLinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        gapSize = gapSize,
        stopSize = stopSize,
        strokeWidth = strokeWidth
    )
}

// ==============================================================================
// 2. TYPE 1 BUTTON GROUP (REACTIVE PRESS-WIDTH EXPANSION)
// ==============================================================================

data class ExpressiveActionItem(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
    val isPrimary: Boolean = false,
    val enabled: Boolean = true,
    val containerColor: Color? = null,
    val contentColor: Color? = null,
    val weight: Float = 1f
)

/**
 * Material 3 Expressive Type 1 `ButtonGroup`: pressing any action pill expands its
 * horizontal weight by [expandedWeightBoost] via [SplitMateMotion.fastSpatial] while
 * sibling buttons gently compress.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun ButtonGroup(
    items: List<ExpressiveActionItem>,
    modifier: Modifier = Modifier,
    expandedWeightBoost: Float = 0.15f
) {
    if (items.isEmpty()) return
    // v2.4.0 Trip Hub step 3: the official M3 Expressive ButtonGroup. Pressing a button widens it
    // (animateWidth on the theme's spring) while its neighbours give way; no hand-built weights.
    androidx.compose.material3.ButtonGroup(
        modifier = modifier.fillMaxWidth(),
        expandedRatio = expandedWeightBoost,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            val interactionSource = remember { MutableInteractionSource() }
            val resolvedContainerColor = item.containerColor ?: if (item.isPrimary) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
            val resolvedContentColor = item.contentColor ?: if (item.isPrimary) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
            FilledTonalButton(
                onClick = item.onClick,
                enabled = item.enabled,
                interactionSource = interactionSource,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = resolvedContainerColor,
                    contentColor = resolvedContentColor
                ),
                modifier = Modifier
                    .animateWidth(interactionSource)
                    .weight(item.weight.coerceAtLeast(0.2f))
                    .heightIn(min = 48.dp)
            ) {
                if (item.icon != null) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = item.label,
                    style = if (item.isPrimary) {
                        SplitMateExpressiveTypography.labelLargeEmphasized
                    } else {
                        MaterialTheme.typography.labelLarge
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ==============================================================================
// 3. TYPE 2 CONNECTED BUTTON GROUP (MORPHING INNER CORNERS)
// ==============================================================================

/**
 * Material 3 Expressive Type 2 `ConnectedButtonGroup`:
 * - Outer ends remain `50%` (`CircleShape` / `22.dp` pill).
 * - Unselected inner corners are `8.dp` with a `2.dp` gap between segments.
 * - Pressed inner corners sharpen to `4.dp`.
 * - Selected (`index == selectedIndex`, `checked = true`) morphs both start & end corners
 *   to `50%` (`CircleShape`) via [SplitMateMotion.fastSpatial] and applies
 *   [SplitMateExpressiveTypography.labelLargeEmphasized].
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun <T> ConnectedButtonGroup(
    options: List<T>,
    selectedIndex: Int,
    onSelect: (Int, T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier,
    iconProvider: ((T, Boolean) -> ImageVector?)? = null
) {
    if (options.isEmpty()) return
    // v2.3.6 Wave 4: real Material 3 Expressive connected button group — official ToggleButton +
    // ButtonGroupDefaults connected leading/middle/trailing shapes (asymmetric inner corners, press
    // squish, checked morph to full pill) with ButtonGroupDefaults.ConnectedSpaceBetween gaps.
    // Brand colours: checked = sage primaryContainer, unchecked = surfaceContainerHigh.
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(androidx.compose.material3.ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, option ->
            val checked = index == selectedIndex
            val shapes = when {
                options.size == 1 -> androidx.compose.material3.ToggleButtonDefaults.shapes()
                index == 0 -> androidx.compose.material3.ButtonGroupDefaults.connectedLeadingButtonShapes()
                index == options.lastIndex -> androidx.compose.material3.ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> androidx.compose.material3.ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            val contentColor = if (checked) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            androidx.compose.material3.ToggleButton(
                checked = checked,
                onCheckedChange = { onSelect(index, option) },
                shapes = shapes,
                colors = androidx.compose.material3.ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .semantics { role = androidx.compose.ui.semantics.Role.RadioButton }
            ) {
                val icon = iconProvider?.invoke(option, checked)
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = labelProvider(option),
                    style = if (checked) {
                        SplitMateExpressiveTypography.labelLargeEmphasized
                    } else {
                        MaterialTheme.typography.labelMedium
                    },
                    fontFamily = FigtreeFontFamily,
                    fontWeight = if (checked) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ==============================================================================
// 4. FLOATING ACTION BUTTON MENU & TOGGLE FLOATING ACTION BUTTON
// ==============================================================================

data class ExpressiveFabMenuItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val subtitle: String? = null,
    val containerColor: Color? = null,
    val contentColor: Color? = null
)

/**
 * SplitMate entry point for the Material 3 Expressive FAB menu.
 *
 * v2.3.6: delegates to the real `androidx.compose.material3.FloatingActionButtonMenu` and
 * `ToggleFloatingActionButton`: the toggle morphs from a 56dp rounded square into a circle and the
 * `+` icon turns into `x`; menu items expand with the theme's fast spatial spring
 * (`MaterialTheme.motionScheme`, snapped under reduced motion). The call-site API (labels, icons,
 * [toggleLabel] used as the accessibility label) is unchanged.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun FloatingActionButtonMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    items: List<ExpressiveFabMenuItem>,
    modifier: Modifier = Modifier,
    toggleIcon: ImageVector = Icons.Rounded.Add,
    toggleLabel: String? = null
) {
    androidx.compose.material3.FloatingActionButtonMenu(
        expanded = expanded,
        modifier = modifier,
        button = {
            androidx.compose.material3.ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { onToggle() },
                modifier = Modifier.semantics {
                    contentDescription = if (expanded) "Close menu" else (toggleLabel ?: "Open menu")
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                }
            ) {
                val progress = { checkedProgress }
                Icon(
                    imageVector = if (checkedProgress > 0.5f) Icons.Rounded.Close else toggleIcon,
                    contentDescription = null,
                    modifier = with(androidx.compose.material3.ToggleFloatingActionButtonDefaults) {
                        Modifier.animateIcon(progress)
                    }
                )
            }
        }
    ) {
        items.forEach { item ->
            FloatingActionButtonMenuItem(
                onClick = {
                    onToggle()
                    item.onClick()
                },
                text = { Text(text = item.label, maxLines = 1) },
                icon = { Icon(imageVector = item.icon, contentDescription = null) },
                containerColor = item.containerColor ?: MaterialTheme.colorScheme.primaryContainer,
                contentColor = item.contentColor ?: MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ==============================================================================
// 5. CONTAINED LOADING INDICATOR (7-POLYGON MORPHING LOADER)
// ==============================================================================

/**
 * Default polygon sets and spring tokens for [ContainedLoadingIndicator] and [LoadingIndicator].
 *
 * v2.4.0 rc3: both loaders now delegate to the official material3 loaders, which own their morph and
 * rotation motion. The polygon sets below are still passed through; the morph spring tokens and
 * [loadingIndicatorFrame] are kept as the documented reference model (and for their unit tests).
 */
object LoadingIndicatorDefaults {
    /**
     * The house 7-shape sequence (`MaterialShapes.morphSequence`: `SoftBurst` -> `Cookie9Sided`
     * -> `Pentagon` -> `Pill` -> `Sunny` -> `Clover4Leaf` -> `Oval`). Default for every loader.
     */
    val IndeterminateIndicatorPolygons: List<RoundedPolygon>
        get() = MaterialShapes.morphSequence

    /** Trip Guide hero-image loader shapes (audit 5.3): Circle -> Square -> Cookie9Sided -> SoftBurst. */
    val GuideHeroPolygons: List<RoundedPolygon> by lazy {
        listOf(
            MaterialShapes.Circle,
            MaterialShapes.Square,
            MaterialShapes.Cookie9Sided,
            MaterialShapes.SoftBurst
        )
    }

    /** Spatial spring for each shape-to-shape morph (slight overshoot, settles in ~0.5s). */
    const val MORPH_DAMPING: Float = 0.6f
    const val MORPH_STIFFNESS: Float = 200f
    const val MORPH_VISIBILITY_THRESHOLD: Float = 0.002f

    /** Pause on each settled shape before the next morph starts. */
    const val MORPH_HOLD_MILLIS: Long = 90L

    /** Rotation added per morph stage. */
    const val DEGREES_PER_STAGE: Float = 90f

    /**
     * The stage counter is wrapped every `polygons.size * STAGE_WRAP_CYCLES` stages to keep float
     * precision. `STAGE_WRAP_CYCLES * DEGREES_PER_STAGE` is a multiple of 360, so the wrap is
     * visually seamless.
     */
    const val STAGE_WRAP_CYCLES: Int = 64

    fun morphSpring(): SpringSpec<Float> = spring(
        dampingRatio = MORPH_DAMPING,
        stiffness = MORPH_STIFFNESS,
        visibilityThreshold = MORPH_VISIBILITY_THRESHOLD
    )
}

/** One rendered frame of a morphing loader: which `Morph` to draw, its progress and rotation. */
data class LoadingIndicatorFrame(
    val morphIndex: Int,
    val morphProgress: Float,
    val rotationDegrees: Float
)

/**
 * Pure mapping from the spring-driven stage value to a loader frame. The loader animates
 * [animatedStage] towards the integer [targetStage]; morph `targetStage - 1` (polygon
 * `(targetStage - 1) % n` -> the next polygon) is drawn at progress
 * `animatedStage - (targetStage - 1)`, clamped to 0..1 so spring overshoot never distorts the
 * outline. Rotation follows the unclamped stage, so the overshoot reads as a lively spin.
 */
fun loadingIndicatorFrame(
    animatedStage: Float,
    targetStage: Int,
    polygonCount: Int,
    degreesPerStage: Float = LoadingIndicatorDefaults.DEGREES_PER_STAGE
): LoadingIndicatorFrame {
    if (polygonCount <= 1 || targetStage <= 0 || animatedStage.isNaN() || animatedStage.isInfinite()) {
        return LoadingIndicatorFrame(morphIndex = 0, morphProgress = 0f, rotationDegrees = 0f)
    }
    val fromStage = targetStage - 1
    return LoadingIndicatorFrame(
        morphIndex = fromStage.mod(polygonCount),
        morphProgress = (animatedStage - fromStage).coerceIn(0f, 1f),
        rotationDegrees = (animatedStage * degreesPerStage).mod(360f)
    )
}

/**
 * Material 3 Expressive `ContainedLoadingIndicator`: morphs sequentially through [polygons]
 * (default [LoadingIndicatorDefaults.IndeterminateIndicatorPolygons]) inside a tonal
 * [containerShape]. With reduced motion (`ANIMATOR_DURATION_SCALE == 0`) the first polygon is drawn
 * once, statically. Pass [contentDescription] (e.g. "Loading guide") to expose a TalkBack label on
 * a polite live region.
 *
 * v2.4.0 rc3: delegates to the official `androidx.compose.material3.ContainedLoadingIndicator`
 * (library morph + rotation springs, 38/48 indicator-to-container ratio). [containerSize] still
 * sizes the container; a smaller `Modifier.size(...)` from the caller wins, as before.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun ContainedLoadingIndicator(
    modifier: Modifier = Modifier,
    containerSize: Dp = 48.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    indicatorColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    containerShape: Shape = CircleShape,
    polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons,
    contentDescription: String? = null
) {
    val sequence = remember(polygons) { polygons.ifEmpty { MaterialShapes.morphSequence } }
    val reducedMotion = rememberReducedMotionEnabled()
    val labelModifier = loadingLabelModifier(contentDescription)

    if (reducedMotion || sequence.size < 2) {
        // The official loader always animates and needs >= 2 polygons: draw the first shape once.
        val staticShape = remember(sequence) { sequence.first().toShape() }
        StaticLoadingShape(
            modifier = modifier.then(labelModifier).size(containerSize),
            shape = staticShape,
            indicatorColor = indicatorColor,
            containerColor = containerColor,
            containerShape = containerShape
        )
        return
    }

    androidx.compose.material3.ContainedLoadingIndicator(
        modifier = modifier.then(labelModifier).size(containerSize),
        containerColor = containerColor,
        indicatorColor = indicatorColor,
        containerShape = containerShape,
        polygons = sequence
    )
}

/** TalkBack label for the loaders: a polite live region announcing [contentDescription]. */
private fun loadingLabelModifier(contentDescription: String?): Modifier =
    if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            liveRegion = LiveRegionMode.Polite
        }
    } else {
        Modifier
    }

/**
 * Reduced-motion frame shared by [ContainedLoadingIndicator] and [LoadingIndicator]: one official
 * `MaterialShapes` polygon, drawn once at the official indicator-to-container ratio (38/48).
 */
@Composable
private fun StaticLoadingShape(
    modifier: Modifier,
    shape: Shape,
    indicatorColor: Color,
    containerColor: Color,
    containerShape: Shape
) {
    val ratio = androidx.compose.material3.LoadingIndicatorDefaults.IndicatorSize /
        androidx.compose.material3.LoadingIndicatorDefaults.ContainerWidth
    Box(
        modifier = modifier
            .progressSemantics()
            .clip(containerShape)
            .background(containerColor, containerShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(ratio)
                .clip(shape)
                .background(indicatorColor)
        )
    }
}

// ==============================================================================
// 6. SPLIT BUTTON LAYOUT (LEADING BUTTON + MORPHING TRAILING CHEVRON CAP)
// ==============================================================================

data class ExpressiveMenuAction(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
    val subtitle: String? = null
)

/**
 * Material 3 Expressive `SplitButtonLayout` pairing a pill-start `LeadingButton` with
 * a `2.dp` gap and a D-shaped `TrailingButton` that morphs into a full `CircleShape`
 * and rotates `Icons.Rounded.ExpandMore` by `180deg` via [SplitMateMotion.fastSpatial]
 * when its dropdown menu opens.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun SplitButtonLayout(
    leadingText: String,
    leadingIcon: ImageVector?,
    onLeadingClick: () -> Unit,
    menuItems: List<ExpressiveMenuAction>,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    fillWidth: Boolean = false
) {
    var menuExpanded by remember { mutableStateOf(false) }
    // v2.4.0 Trip Hub step 3: the official M3 Expressive split button (SplitButtonLayout +
    // SplitButtonDefaults.LeadingButton / TrailingButton): the trailing button's inner corners
    // morph to a pill while its menu is open, with the library's own press squish and motion.
    val buttonHeight = if (fillWidth) SplitButtonDefaults.MediumContainerHeight else SplitButtonDefaults.ContainerHeight
    val colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor)
    Box(modifier = modifier) {
        androidx.compose.material3.SplitButtonLayout(
            leadingButton = {
                androidx.compose.material3.SplitButtonDefaults.LeadingButton(
                    onClick = onLeadingClick,
                    colors = colors,
                    modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier).heightIn(min = buttonHeight)
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(if (fillWidth) 18.dp else 16.dp)
                        )
                        Spacer(modifier = Modifier.width(if (fillWidth) 8.dp else 6.dp))
                    }
                    Text(
                        text = leadingText,
                        style = SplitMateExpressiveTypography.labelLargeEmphasized,
                        maxLines = 1
                    )
                }
            },
            trailingButton = {
                // v2.4.0 D3: same trailing wrapper as the slot overload (min width keeps the
                // pill end from being clipped when the label is long).
                SplitButtonDefaults.TrailingButton(
                    checked = menuExpanded,
                    onCheckedChange = { menuExpanded = it },
                    containerColor = containerColor,
                    contentColor = contentColor,
                    height = buttonHeight,
                    width = SplitButtonDefaults.MediumContainerHeight
                ) {
                    SplitButtonDefaults.TrailingIcon(
                        checked = menuExpanded,
                        contentDescription = "More actions",
                        iconSize = if (fillWidth) 22.dp else 18.dp
                    )
                }
            },
            modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier,
            spacing = SplitButtonDefaults.Spacing
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            menuItems.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(text = item.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            if (!item.subtitle.isNullOrBlank()) {
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    leadingIcon = item.icon?.let { iconVec ->
                        { Icon(imageVector = iconVec, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    },
                    onClick = {
                        menuExpanded = false
                        item.onClick()
                    }
                )
            }
        }
    }
}

/**
 * Slot-based Material 3 Expressive `SplitButtonLayout`, mirroring the official material3 1.4
 * signature `SplitButtonLayout(leadingButton, trailingButton, modifier, spacing)` so the v3.0
 * upgrade is an import swap (used by the Trip Guide "Directions" button, audit 5.8).
 *
 * Pair it with [SplitButtonDefaults.LeadingButton] and [SplitButtonDefaults.TrailingButton]. The
 * trailing button owns the menu expanded state (`checked`) and morphs its inner corners into a
 * full pill while the menu is open. The optional [menuContent] renders a `DropdownMenu`
 * anchored to the whole split button, shown while [menuExpanded] is true:
 *
 * ```
 * var menuOpen by remember { mutableStateOf(false) }
 * SplitButtonLayout(
 *     leadingButton = {
 *         SplitButtonDefaults.LeadingButton(onClick = onDirections) { Text("Directions") }
 *     },
 *     trailingButton = {
 *         SplitButtonDefaults.TrailingButton(checked = menuOpen, onCheckedChange = { menuOpen = it })
 *     },
 *     menuExpanded = menuOpen,
 *     onMenuDismissRequest = { menuOpen = false },
 *     menuContent = { DropdownMenuItem(text = { Text("Walk") }, onClick = onWalk) }
 * )
 * ```
 *
 * @param fillWidth when true the layout fills the width and the leading slot takes the remaining
 *   space (its min width is propagated, so the leading button stretches).
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun SplitButtonLayout(
    leadingButton: @Composable () -> Unit,
    trailingButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = SplitButtonDefaults.Spacing,
    fillWidth: Boolean = false,
    menuExpanded: Boolean = false,
    onMenuDismissRequest: () -> Unit = {},
    menuContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    // v2.4.0 Trip Hub step 3: lays out through the official M3 Expressive SplitButtonLayout.
    Box(modifier = modifier) {
        androidx.compose.material3.SplitButtonLayout(
            leadingButton = {
                if (fillWidth) {
                    Box(modifier = Modifier.fillMaxWidth(), propagateMinConstraints = true) { leadingButton() }
                } else {
                    leadingButton()
                }
            },
            trailingButton = trailingButton,
            modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier,
            spacing = spacing
        )
        if (menuContent != null) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = onMenuDismissRequest,
                content = menuContent
            )
        }
    }
}

/**
 * Defaults and building blocks for the slot-based [SplitButtonLayout], mirroring the official
 * `SplitButtonDefaults.LeadingButton` / `TrailingButton`. Visible height defaults to 40.dp; the
 * M3 minimum interactive size still guarantees a 48.dp touch target.
 */
object SplitButtonDefaults {
    /** Gap between the leading and trailing buttons. */
    val Spacing: Dp = 2.dp

    /** Default visible container height (M3E "small" split button). */
    val ContainerHeight: Dp = 40.dp

    /** Full-width sheet actions use the M3E "medium" height. */
    val MediumContainerHeight: Dp = 48.dp

    /** Inner (facing) corner radius of both buttons while the menu is closed. */
    val InnerCornerSize: Dp = 4.dp

    /** Default width of the trailing (menu) button. */
    val TrailingButtonWidth: Dp = 40.dp

    /**
     * Leading action of a split button: pill-shaped outer start corners, [InnerCornerSize]
     * inner end corners.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun LeadingButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        containerColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = MaterialTheme.colorScheme.onPrimary,
        height: Dp = ContainerHeight,
        contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        content: @Composable RowScope.() -> Unit
    ) {
        // v2.4.0: the official M3 Expressive leading button (library shapes, press squish, motion).
        androidx.compose.material3.SplitButtonDefaults.LeadingButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = height),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
            contentPadding = contentPadding,
            content = content
        )
    }

    /**
     * Trailing menu toggle of a split button. [checked] is the menu expanded state: when true the
     * inner corners spring ([SplitMateMotion.fastSpatial]) from [InnerCornerSize] to a full pill.
     * Exposes the expanded/collapsed state to TalkBack via `stateDescription`.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun TrailingButton(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        containerColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = MaterialTheme.colorScheme.onPrimary,
        height: Dp = ContainerHeight,
        width: Dp = TrailingButtonWidth,
        expandedStateDescription: String = "Menu expanded",
        collapsedStateDescription: String = "Menu collapsed",
        content: @Composable () -> Unit = { TrailingIcon(checked = checked) }
    ) {
        // v2.4.0: the official M3 Expressive trailing toggle (inner corners morph to a pill while open).
        val stateText = if (checked) expandedStateDescription else collapsedStateDescription
        androidx.compose.material3.SplitButtonDefaults.TrailingButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier
                .heightIn(min = height)
                .widthIn(min = width)
                .semantics { stateDescription = stateText },
            enabled = enabled,
            // v2.4.0 D3: explicit shapes with dp outer corners (half the height): the alpha library drew
            // percent-based outer corners square on first frame, leaving a flat trailing end.
            shapes = androidx.compose.material3.SplitButtonShapes(
                shape = RoundedCornerShape(
                    topStart = CornerSize(InnerCornerSize),
                    bottomStart = CornerSize(InnerCornerSize),
                    topEnd = CornerSize(height / 2),
                    bottomEnd = CornerSize(height / 2)
                ),
                pressedShape = RoundedCornerShape(
                    topStart = CornerSize(InnerCornerSize * 2),
                    bottomStart = CornerSize(InnerCornerSize * 2),
                    topEnd = CornerSize(height / 2),
                    bottomEnd = CornerSize(height / 2)
                ),
                checkedShape = CircleShape
            ),
            colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor)
        ) {
            content()
        }
    }

    /** Chevron for [TrailingButton] that rotates 180 degrees while the menu is open. */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun TrailingIcon(
        checked: Boolean,
        modifier: Modifier = Modifier,
        contentDescription: String? = "More options",
        iconSize: Dp = 18.dp
    ) {
        val rotation by animateFloatAsState(
            targetValue = if (checked) 180f else 0f,
            animationSpec = SplitMateMotion.fastSpatial(),
            label = "SplitButtonSlotChevronRotation"
        )
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = contentDescription,
            modifier = modifier
                .size(iconSize)
                .graphicsLayer { rotationZ = rotation }
        )
    }
}

// ==============================================================================
// 7. HORIZONTAL FLOATING TOOLBAR (WITH ADJACENT FAB & NESTED SCROLL)
// ==============================================================================

object FloatingToolbarDefaults {
    @Composable
    fun StandardFloatingActionButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        containerColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = MaterialTheme.colorScheme.onPrimary,
        content: @Composable () -> Unit
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor,
            shadowElevation = 6.dp,
            modifier = modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

/**
 * Automatically toggles a [HorizontalFloatingToolbar]'s expanded/collapsed state
 * as the user scrolls vertically in the parent list.
 */
fun Modifier.floatingToolbarVerticalNestedScroll(
    expanded: Boolean = true,
    onExpand: () -> Unit = {},
    onCollapse: () -> Unit = {}
): Modifier = this.nestedScroll(
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y < -6f && expanded) {
                onCollapse()
            } else if (available.y > 6f && !expanded) {
                onExpand()
            }
            return Offset.Zero
        }
    }
)

/**
 * Material 3 Expressive `HorizontalFloatingToolbar` hovering above the system navigation
 * bar in a `CircleShape` pill container (`LocalSplitMatePalette.current.surfaceContainerHigh`,
 * `6.dp` shadow elevation), paired side-by-side with an optional adjacent [floatingActionButton].
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun HorizontalFloatingToolbar(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val palette = LocalSplitMatePalette.current
    val horizontalPad by animateDpAsState(
        targetValue = if (expanded) 12.dp else 8.dp,
        animationSpec = SplitMateMotion.defaultSpatial(),
        label = "FloatingToolbarPadding"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 72.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = SplitMateMotion.fastEffects()) +
                expandHorizontally(animationSpec = SplitMateMotion.defaultSpatial()),
            exit = fadeOut(animationSpec = SplitMateMotion.fastEffects()) +
                shrinkHorizontally(animationSpec = SplitMateMotion.defaultSpatial())
        ) {
            Surface(
                shape = CircleShape,
                color = palette.surfaceContainerHigh,
                contentColor = palette.onSurface,
                border = BorderStroke(1.dp, palette.outlineVariant.copy(alpha = 0.65f)),
                modifier = Modifier
                    .height(60.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = horizontalPad, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }

        if (floatingActionButton != null) {
            if (expanded) {
                Spacer(modifier = Modifier.width(10.dp))
            }
            floatingActionButton.invoke()
        }
    }
}

/**
 * Material 3 Expressive `VerticalFloatingToolbar` for edge-docked quick actions
 * and foldable/tablet vertical toolbars, paired with an optional [floatingActionButton].
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun VerticalFloatingToolbar(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalSplitMatePalette.current
    val verticalPad by animateDpAsState(
        targetValue = if (expanded) 12.dp else 8.dp,
        animationSpec = SplitMateMotion.defaultSpatial(),
        label = "VerticalFloatingToolbarPadding"
    )
    Column(
        modifier = modifier
            .widthIn(max = 72.dp)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = SplitMateMotion.fastEffects()) +
                expandVertically(animationSpec = SplitMateMotion.defaultSpatial()),
            exit = fadeOut(animationSpec = SplitMateMotion.fastEffects()) +
                shrinkVertically(animationSpec = SplitMateMotion.defaultSpatial())
        ) {
            Surface(
                shape = CircleShape,
                color = palette.surfaceContainerHigh,
                contentColor = palette.onSurface,
                border = BorderStroke(1.dp, palette.outlineVariant.copy(alpha = 0.65f)),
                modifier = Modifier
                    .width(60.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = verticalPad),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content
                )
            }
        }

        if (floatingActionButton != null) {
            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
            }
            floatingActionButton.invoke()
        }
    }
}

/**
 * Material 3 Expressive uncontained `LoadingIndicator` cycling through
 * [polygons] (default [LoadingIndicatorDefaults.IndeterminateIndicatorPolygons]) without an
 * outer container. Honours reduced motion and [contentDescription] like [ContainedLoadingIndicator].
 *
 * v2.4.0 rc3: delegates to the official `androidx.compose.material3.LoadingIndicator`; [size] is the
 * container size (the official shape fills 38/48 of it).
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons,
    contentDescription: String? = null
) {
    val sequence = remember(polygons) { polygons.ifEmpty { MaterialShapes.morphSequence } }
    val reducedMotion = rememberReducedMotionEnabled()
    val labelModifier = loadingLabelModifier(contentDescription)

    if (reducedMotion || sequence.size < 2) {
        val staticShape = remember(sequence) { sequence.first().toShape() }
        StaticLoadingShape(
            modifier = modifier.then(labelModifier).size(size),
            shape = staticShape,
            indicatorColor = color,
            containerColor = Color.Transparent,
            containerShape = CircleShape
        )
        return
    }

    androidx.compose.material3.LoadingIndicator(
        modifier = modifier.then(labelModifier).size(size),
        color = color,
        polygons = sequence
    )
}

// ==============================================================================
// 8. EDITORIAL FINANCIAL TOTAL (HERO NUMERALS THAT NEVER OVERFLOW)
// ==============================================================================

/**
 * Material 3 Expressive editorial hero figure for financial totals (e.g. `₹33,365.35`).
 *
 * Renders with a large type role (default [androidx.compose.material3.Typography.displaySmall])
 * and tabular numerals, on a single line. When the figure is wider than the available
 * width it steps the font size down (never below [minFontScale] of the base size) instead
 * of wrapping or pushing sibling header content off-screen. Pure presentation: the text
 * passed in is already formatted from integer cents by the caller.
 */
@Composable
fun EditorialFinancialTotalText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displaySmall,
    color: Color = Color.Unspecified,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight = FontWeight.Black,
    textAlign: TextAlign? = null,
    minFontScale: Float = 0.55f
) {
    var fontScale by remember(text, style) { mutableFloatStateOf(1f) }
    var readyToDraw by remember(text, style) { mutableStateOf(false) }
    val baseFontSize = if (style.fontSize.isSpecified) style.fontSize else 36.sp
    val scaledLineHeight = if (style.lineHeight.isSpecified) style.lineHeight * fontScale else TextUnit.Unspecified

    Text(
        text = text,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        style = style.copy(
            fontSize = baseFontSize * fontScale,
            lineHeight = scaledLineHeight,
            fontFamily = fontFamily ?: style.fontFamily,
            fontWeight = fontWeight,
            fontFeatureSettings = "tnum"
        ),
        color = color,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontScale > minFontScale) {
                fontScale = (fontScale * 0.9f).coerceAtLeast(minFontScale)
            } else {
                readyToDraw = true
            }
        }
    )
}
