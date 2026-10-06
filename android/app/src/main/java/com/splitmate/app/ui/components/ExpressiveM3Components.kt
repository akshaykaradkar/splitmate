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

/**
 * Opt-in marker for SplitMate's local Material 3 Expressive look-alike components.
 *
 * material3 is frozen at 1.2.1, which has none of the official Expressive APIs, so this package
 * ships look-alikes with matching names. Every look-alike is annotated with this marker so the
 * v3.0 swap surface is explicit. The module opts in globally via
 * `-opt-in=com.splitmate.app.ui.components.ExperimentalMaterial3ExpressiveApi` in
 * `android/app/build.gradle`; after the upgrade, point the flag at
 * `androidx.compose.material3.ExperimentalMaterial3ExpressiveApi`.
 */
@RequiresOptIn(message = "This Material 3 Expressive API is experimental.")
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalMaterial3ExpressiveApi

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

/**
 * Material 3 Expressive `LinearWavyProgressIndicator` with sinusoidal wave animation,
 * rounded stroke caps (`StrokeCap.Round`), `4.dp` active-to-track gap, and `4.dp` end-of-track stop dot.
 * Wave amplitude smoothly dampens to `0f` via [SplitMateMotion.defaultSpatial] when
 * progress reaches `1f` (`100%` settled or `0.00c` receipt equilibrium) or `0f`.
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
    val rawProgress = progress().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = SplitMateMotion.defaultSpatial(),
        label = "LinearWavyProgress"
    )
    val targetAmplitude = if (rawProgress <= 0.001f || rawProgress >= 0.999f) {
        0f
    } else {
        amplitude.coerceIn(0f, 1f)
    }
    val effectiveAmplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = SplitMateMotion.defaultSpatial(),
        label = "LinearWavyAmplitude"
    )
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

    val phaseDurationMs = remember(wavelength, waveSpeed) {
        val speedRatio = if (waveSpeed.value > 0f) wavelength.value / waveSpeed.value else 1f
        (speedRatio * 1000f).toInt().coerceIn(450, 4000)
    }
    val infiniteTransition = rememberInfiniteTransition(label = "LinearWavyPhaseTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = phaseDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LinearWavyPhase"
    )

    val wavePath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(strokeWidth + 8.dp)
    ) {
        val strokePx = strokeWidth.toPx().coerceAtLeast(2f)
        val halfStroke = strokePx / 2f
        val centerY = size.height / 2f
        val startX = halfStroke
        val endX = (size.width - halfStroke).coerceAtLeast(startX)
        val totalSpan = (endX - startX).coerceAtLeast(0f)
        val activeEndX = startX + totalSpan * animatedProgress.coerceIn(0f, 1f)
        val gapPx = gapSize.toPx()
        val maxWaveHeightPx = ((size.height - strokePx) / 2f).coerceAtLeast(0f)
        val waveAmpPx = maxWaveHeightPx * effectiveAmplitude
        val wavelengthPx = wavelength.toPx().coerceAtLeast(8f)

        // 1. Active Sinusoidal Wave Segment
        if (animatedProgress > 0.002f && activeEndX > startX) {
            if (waveAmpPx <= 0.2f) {
                drawLine(
                    color = animatedActiveColor,
                    start = Offset(startX, centerY),
                    end = Offset(activeEndX, centerY),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            } else {
                wavePath.reset()
                val stepPx = 2.dp.toPx().coerceAtLeast(1.5f)
                var x = startX
                val startY = centerY + waveAmpPx * sin((2f * PI.toFloat() * x / wavelengthPx) + phase)
                wavePath.moveTo(x, startY)
                while (x < activeEndX) {
                    x = (x + stepPx).coerceAtMost(activeEndX)
                    val y = centerY + waveAmpPx * sin((2f * PI.toFloat() * x / wavelengthPx) + phase)
                    wavePath.lineTo(x, y)
                }
                drawPath(
                    path = wavePath,
                    color = animatedActiveColor,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // 2. Inactive Track Segment (separated by 4.dp gapSize)
        val trackStartX = if (animatedProgress <= 0.002f) {
            startX
        } else {
            activeEndX + gapPx + strokePx * 0.5f
        }
        if (trackStartX < endX) {
            drawLine(
                color = animatedTrackColor,
                start = Offset(trackStartX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }

        // 3. End-of-Track Stop Indicator Dot (4.dp)
        if (animatedProgress < 0.98f && stopSize > 0.dp) {
            val stopRadius = (stopSize.toPx() / 2f).coerceAtMost(halfStroke)
            drawCircle(
                color = animatedActiveColor,
                radius = stopRadius,
                center = Offset(endX, centerY)
            )
        }
    }
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
 * INDETERMINATE Material 3 Expressive `LinearWavyProgressIndicator` (no `progress` parameter),
 * mirroring the official material3 1.4 indeterminate overload.
 *
 * Two phase-shifted segments travel from the start edge to the end edge of the track (see
 * [indeterminateWavySegments]); each segment carries a travelling sine wave. The wave amplitude
 * is constant while running and animates to `0f` (a flat bar) when reduced motion is enabled
 * (`ANIMATOR_DURATION_SCALE == 0`), in which case a static segment is drawn instead.
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

    val phaseDurationMs = remember(wavelength, waveSpeed) {
        val speedRatio = if (waveSpeed.value > 0f) wavelength.value / waveSpeed.value else 1f
        (speedRatio * 1000f).toInt().coerceIn(450, 4000)
    }
    // Continuous loaders legitimately run on a linear clock: the per-segment easing is applied
    // by indeterminateWavySegments(), so the raw cycle fraction must advance uniformly.
    val infiniteTransition = rememberInfiniteTransition(label = "LinearWavyIndeterminateTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = phaseDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LinearWavyIndeterminatePhase"
    )
    val cycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = WavyProgressIndicatorDefaults.INDETERMINATE_CYCLE_MILLIS,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "LinearWavyIndeterminateCycle"
    )

    val wavePath = remember { Path() }
    val description = contentDescription
    val labelModifier = if (description != null) {
        Modifier.semantics {
            this.contentDescription = description
            liveRegion = LiveRegionMode.Polite
        }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .progressSemantics()
            .then(labelModifier)
            .fillMaxWidth()
            .height(strokeWidth + 8.dp)
    ) {
        val strokePx = strokeWidth.toPx().coerceAtLeast(2f)
        val halfStroke = strokePx / 2f
        val centerY = size.height / 2f
        val startX = halfStroke
        val endX = (size.width - halfStroke).coerceAtLeast(startX)
        val totalSpan = (endX - startX).coerceAtLeast(0f)
        val gapPx = gapSize.toPx()
        val maxWaveHeightPx = ((size.height - strokePx) / 2f).coerceAtLeast(0f)
        val waveAmpPx = maxWaveHeightPx * effectiveAmplitude
        val wavelengthPx = wavelength.toPx().coerceAtLeast(8f)
        // State reads happen in the draw phase only: the running loader redraws, never recomposes.
        val segments = if (reducedMotion) {
            listOf(WavyProgressIndicatorDefaults.ReducedMotionSegment)
        } else {
            indeterminateWavySegments(cycle)
        }
        val currentPhase = if (reducedMotion) 0f else phase

        var trackCursor = startX
        segments.forEach { segment ->
            val segmentStartX = startX + totalSpan * segment.start
            val segmentEndX = startX + totalSpan * segment.end
            val trackEndX = segmentStartX - gapPx - strokePx
            if (trackEndX > trackCursor) {
                drawLine(
                    color = animatedTrackColor,
                    start = Offset(trackCursor, centerY),
                    end = Offset(trackEndX, centerY),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
            drawWavySegment(
                path = wavePath,
                startX = segmentStartX,
                endX = segmentEndX,
                centerY = centerY,
                amplitudePx = waveAmpPx,
                wavelengthPx = wavelengthPx,
                phase = currentPhase,
                color = animatedActiveColor,
                strokePx = strokePx
            )
            trackCursor = max(trackCursor, segmentEndX + gapPx + strokePx)
        }
        if (trackCursor < endX) {
            drawLine(
                color = animatedTrackColor,
                start = Offset(trackCursor, centerY),
                end = Offset(endX, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }
    }
}

/** Draws one active segment as a sine wave (or a flat rounded line when the amplitude is ~0). */
private fun DrawScope.drawWavySegment(
    path: Path,
    startX: Float,
    endX: Float,
    centerY: Float,
    amplitudePx: Float,
    wavelengthPx: Float,
    phase: Float,
    color: Color,
    strokePx: Float
) {
    if (endX <= startX) return
    if (amplitudePx <= 0.2f) {
        drawLine(
            color = color,
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
        return
    }
    path.reset()
    val stepPx = 2.dp.toPx().coerceAtLeast(1.5f)
    var x = startX
    path.moveTo(x, centerY + amplitudePx * sin((2f * PI.toFloat() * x / wavelengthPx) + phase))
    while (x < endX) {
        x = (x + stepPx).coerceAtMost(endX)
        path.lineTo(x, centerY + amplitudePx * sin((2f * PI.toFloat() * x / wavelengthPx) + phase))
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
    )
}

/** One active segment of an indeterminate bar, as fractions `[start, end]` of the track (0..1). */
data class WavySegment(val start: Float, val end: Float) {
    val length: Float get() = end - start
}

/**
 * Pure, deterministic geometry of the indeterminate bar at [cycleFraction] (wrapped into 0..1).
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
    val rawProgress = progress().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = SplitMateMotion.defaultSpatial(),
        label = "CircularWavyProgress"
    )
    val targetAmplitude = if (rawProgress <= 0.001f || rawProgress >= 0.999f) {
        0f
    } else {
        amplitude.coerceIn(0f, 1f)
    }
    val effectiveAmplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = SplitMateMotion.defaultSpatial(),
        label = "CircularWavyAmplitude"
    )
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

    val infiniteTransition = rememberInfiniteTransition(label = "CircularWavyPhaseTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CircularWavyPhase"
    )

    val wavePath = remember { Path() }

    Canvas(modifier = modifier.size(48.dp)) {
        val strokePx = strokeWidth.toPx().coerceAtLeast(2f)
        val maxAmpPx = (strokePx * 0.42f) * effectiveAmplitude
        val baseRadius = (size.minDimension / 2f) - strokePx - maxAmpPx
        if (baseRadius <= 2f) return@Canvas

        val center = Offset(size.width / 2f, size.height / 2f)
        val circumference = (2f * PI.toFloat() * baseRadius).coerceAtLeast(1f)
        val numWaves = max(4, (circumference / wavelength.toPx().coerceAtLeast(8f)).toInt())
        val gapAngleDeg = ((gapSize.toPx() + strokePx) / circumference) * 360f
        val activeSweepDeg = (animatedProgress * 360f).coerceIn(0f, 360f)

        // 1. Active Wavy Arc
        if (activeSweepDeg > 1f) {
            if (maxAmpPx <= 0.15f) {
                drawArc(
                    color = animatedActiveColor,
                    startAngle = -90f,
                    sweepAngle = activeSweepDeg,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
                    size = Size(baseRadius * 2f, baseRadius * 2f),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            } else {
                wavePath.reset()
                val steps = max(24, (activeSweepDeg * 0.6f).toInt())
                for (i in 0..steps) {
                    val fraction = i.toFloat() / steps.toFloat()
                    val angleDeg = -90f + activeSweepDeg * fraction
                    val angleRad = angleDeg * (PI.toFloat() / 180f)
                    val r = baseRadius + maxAmpPx * sin(numWaves * angleRad + phase)
                    val x = center.x + r * cos(angleRad)
                    val y = center.y + r * sin(angleRad)
                    if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                }
                drawPath(
                    path = wavePath,
                    color = animatedActiveColor,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // 2. Inactive Track Arc (with gapSize separation)
        val trackStartDeg = -90f + activeSweepDeg + if (activeSweepDeg > 1f) gapAngleDeg else 0f
        val trackSweepDeg = (360f - activeSweepDeg - if (activeSweepDeg > 1f) gapAngleDeg * 2f else 0f)
            .coerceAtLeast(0f)
        if (trackSweepDeg > 2f) {
            drawArc(
                color = animatedTrackColor,
                startAngle = trackStartDeg,
                sweepAngle = trackSweepDeg,
                useCenter = false,
                topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
                size = Size(baseRadius * 2f, baseRadius * 2f),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
    }
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
    LinearWavyProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        amplitude = 0f,
        gapSize = gapSize,
        stopSize = stopSize,
        strokeWidth = strokeWidth
    )
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val animatedWeight by animateFloatAsState(
                targetValue = if (isPressed) {
                    (item.weight + expandedWeightBoost).coerceAtLeast(0.2f)
                } else {
                    item.weight.coerceAtLeast(0.2f)
                },
                animationSpec = SplitMateMotion.fastSpatial(),
                label = "ButtonGroupWeight_$index"
            )
            val innerCorner by animateDpAsState(
                targetValue = if (isPressed) 8.dp else 16.dp,
                animationSpec = SplitMateMotion.fastSpatial(),
                label = "ButtonGroupCorner_$index"
            )
            val shape = when {
                items.size == 1 -> CircleShape
                index == 0 -> RoundedCornerShape(
                    topStart = 24.dp,
                    bottomStart = 24.dp,
                    topEnd = innerCorner,
                    bottomEnd = innerCorner
                )
                index == items.lastIndex -> RoundedCornerShape(
                    topStart = innerCorner,
                    bottomStart = innerCorner,
                    topEnd = 24.dp,
                    bottomEnd = 24.dp
                )
                else -> RoundedCornerShape(innerCorner)
            }

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
                shape = shape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = resolvedContainerColor,
                    contentColor = resolvedContentColor
                ),
                modifier = Modifier
                    .weight(animatedWeight)
                    .height(44.dp)
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
                    fontFamily = FigtreeFontFamily,
                    fontWeight = if (item.isPrimary) FontWeight.ExtraBold else FontWeight.Bold,
                    fontSize = 12.sp,
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
 * Material 3 Expressive `ToggleFloatingActionButton` that morphs from a `20.dp` squircle/pill
 * into a full `CircleShape` (`28.dp`) close button when [checked] is true.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun ToggleFloatingActionButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Add,
    label: String? = null
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (checked) 28.dp else 20.dp,
        animationSpec = SplitMateMotion.fastSpatial(),
        label = "ToggleFabCorner"
    )
    val iconRotation by animateFloatAsState(
        targetValue = if (checked) 90f else 0f,
        animationSpec = SplitMateMotion.fastSpatial(),
        label = "ToggleFabRotation"
    )
    val containerColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = SplitMateMotion.fastEffects(),
        label = "ToggleFabContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = SplitMateMotion.fastEffects(),
        label = "ToggleFabContentColor"
    )

    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = if (checked && cornerRadius >= 27.dp) CircleShape else RoundedCornerShape(cornerRadius),
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 6.dp,
        modifier = modifier.height(56.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (!checked && !label.isNullOrBlank()) 20.dp else 16.dp,
                vertical = 14.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (checked) Icons.Rounded.Close else icon,
                contentDescription = label ?: if (checked) "Close menu" else "Open menu",
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { rotationZ = iconRotation }
            )
            if (!checked && !label.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = SplitMateExpressiveTypography.labelLargeEmphasized,
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

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
 * [containerShape]. Each morph is driven by a spatial spring ([LoadingIndicatorDefaults.morphSpring])
 * rather than a linear tween. With reduced motion (`ANIMATOR_DURATION_SCALE == 0`) the first
 * polygon is drawn statically. Pass [contentDescription] (e.g. "Loading guide") to expose a
 * TalkBack label on a polite live region.
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
    val morphs = remember(sequence) {
        sequence.indices.map { idx ->
            val start = sequence[idx]
            val end = sequence[(idx + 1) % sequence.size]
            Morph(start, end)
        }
    }
    val reducedMotion = rememberReducedMotionEnabled()
    val stage = remember(sequence) { Animatable(0f) }
    var targetStage by remember(sequence) { mutableIntStateOf(0) }

    LaunchedEffect(sequence, reducedMotion) {
        if (reducedMotion || sequence.size < 2) return@LaunchedEffect
        val wrapStage = sequence.size * LoadingIndicatorDefaults.STAGE_WRAP_CYCLES
        while (isActive) {
            var next = targetStage + 1
            if (next > wrapStage) {
                // Seamless wrap: same polygon and same rotation (wrapStage * 90deg % 360 == 0).
                stage.snapTo(stage.value - wrapStage)
                next -= wrapStage
            }
            targetStage = next
            stage.animateTo(
                targetValue = next.toFloat(),
                animationSpec = LoadingIndicatorDefaults.morphSpring()
            )
            delay(LoadingIndicatorDefaults.MORPH_HOLD_MILLIS)
        }
    }

    val description = contentDescription
    val labelModifier = if (description != null) {
        Modifier.semantics {
            this.contentDescription = description
            liveRegion = LiveRegionMode.Polite
        }
    } else {
        Modifier
    }
    val indicatorShape: Shape = if (reducedMotion || sequence.size < 2) {
        remember(sequence) { sequence.first().toShape() }
    } else {
        val frame = loadingIndicatorFrame(stage.value, targetStage, sequence.size)
        MorphPolygonShape(
            morph = morphs[frame.morphIndex],
            percentage = frame.morphProgress,
            rotationDegrees = frame.rotationDegrees
        )
    }

    Box(
        modifier = modifier
            .then(labelModifier)
            .size(containerSize)
            .clip(containerShape)
            .background(containerColor, containerShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(containerSize * 0.62f)
                .clip(indicatorShape)
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
    // Full-width mode (primary sheet actions) uses the M3E "Medium" 48.dp split-button size.
    val buttonHeight = if (fillWidth) 48.dp else 40.dp
    val outerCorner = buttonHeight / 2
    val trailingInnerCorner by animateDpAsState(
        targetValue = if (menuExpanded) outerCorner else 4.dp,
        animationSpec = SplitMateMotion.fastSpatial(),
        label = "SplitButtonTrailingInnerCorner"
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (menuExpanded) 180f else 0f,
        animationSpec = SplitMateMotion.fastSpatial(),
        label = "SplitButtonChevronRotation"
    )

    Box(modifier = modifier) {
        Row(
            modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // 1. Leading Action Chamber (50% outer start / 4.dp inner end)
            Button(
                onClick = onLeadingClick,
                shape = RoundedCornerShape(
                    topStart = outerCorner,
                    bottomStart = outerCorner,
                    topEnd = 4.dp,
                    bottomEnd = 4.dp
                ),
                contentPadding = PaddingValues(start = 14.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                modifier = if (fillWidth) {
                    Modifier
                        .weight(1f)
                        .height(buttonHeight)
                } else {
                    Modifier.height(buttonHeight)
                }
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
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (fillWidth) 14.sp else 12.sp,
                    maxLines = 1
                )
            }

            // 2. Morphing Trailing Menu Cap (D-shape morphing to CircleShape when menuExpanded)
            Surface(
                onClick = { menuExpanded = !menuExpanded },
                shape = if (menuExpanded && trailingInnerCorner >= outerCorner - 1.dp) {
                    CircleShape
                } else {
                    RoundedCornerShape(
                        topStart = trailingInnerCorner,
                        bottomStart = trailingInnerCorner,
                        topEnd = outerCorner,
                        bottomEnd = outerCorner
                    )
                },
                color = containerColor,
                contentColor = contentColor,
                modifier = Modifier
                    .height(buttonHeight)
                    .width(if (fillWidth) 48.dp else 36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = "More actions",
                        modifier = Modifier
                            .size(if (fillWidth) 22.dp else 18.dp)
                            .graphicsLayer { rotationZ = chevronRotation }
                    )
                }
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            menuItems.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = item.label,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (!item.subtitle.isNullOrBlank()) {
                                Text(
                                    text = item.subtitle,
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    leadingIcon = item.icon?.let { iconVec ->
                        {
                            Icon(
                                imageVector = iconVec,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
    Box(modifier = modifier) {
        Row(
            modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            if (fillWidth) {
                Box(modifier = Modifier.weight(1f), propagateMinConstraints = true) {
                    leadingButton()
                }
            } else {
                leadingButton()
            }
            trailingButton()
        }
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
        val outerCorner = height / 2
        Button(
            onClick = onClick,
            modifier = modifier.height(height),
            enabled = enabled,
            shape = RoundedCornerShape(
                topStart = outerCorner,
                bottomStart = outerCorner,
                topEnd = InnerCornerSize,
                bottomEnd = InnerCornerSize
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
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
        val outerCorner = height / 2
        val innerCorner by animateDpAsState(
            targetValue = if (checked) outerCorner else InnerCornerSize,
            animationSpec = SplitMateMotion.fastSpatial(),
            label = "SplitButtonSlotTrailingInnerCorner"
        )
        val stateText = if (checked) expandedStateDescription else collapsedStateDescription
        Surface(
            onClick = { onCheckedChange(!checked) },
            modifier = modifier
                .minimumInteractiveComponentSize() // v2.4.0 D2: 48dp touch area for the Mark paid chevron
                .height(height)
                .width(width)
                .semantics { stateDescription = stateText },
            enabled = enabled,
            shape = RoundedCornerShape(
                topStart = innerCorner,
                bottomStart = innerCorner,
                topEnd = outerCorner,
                bottomEnd = outerCorner
            ),
            color = containerColor,
            contentColor = contentColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                content()
            }
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
    ContainedLoadingIndicator(
        modifier = modifier,
        containerSize = size,
        containerColor = Color.Transparent,
        indicatorColor = color,
        polygons = polygons,
        contentDescription = contentDescription
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
