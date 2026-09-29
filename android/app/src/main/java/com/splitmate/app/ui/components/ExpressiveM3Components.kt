package com.splitmate.app.ui.components

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import com.splitmate.app.ui.FigtreeFontFamily
import com.splitmate.app.ui.LocalSplitMatePalette
import com.splitmate.app.ui.SplitMateExpressiveTypography
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

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
}

/**
 * Material 3 Expressive `LinearWavyProgressIndicator` with sinusoidal wave animation,
 * rounded stroke caps (`StrokeCap.Round`), `4.dp` active-to-track gap, and `4.dp` end-of-track stop dot.
 * Wave amplitude smoothly dampens to `0f` via [SplitMateMotion.defaultSpatial] when
 * progress reaches `1f` (`100%` settled or `0.00c` receipt equilibrium) or `0f`.
 */
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
 * Material 3 Expressive `CircularWavyProgressIndicator` for Trip Harmony settlement meters
 * and per-traveler contribution rings.
 */
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                shape = CircleShape
            )
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, option ->
            val checked = index == selectedIndex
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val targetStartCorner = when {
                checked || index == 0 -> 22.dp
                isPressed -> 4.dp
                else -> 8.dp
            }
            val targetEndCorner = when {
                checked || index == options.lastIndex -> 22.dp
                isPressed -> 4.dp
                else -> 8.dp
            }

            val startCorner by animateDpAsState(
                targetValue = targetStartCorner,
                animationSpec = SplitMateMotion.fastSpatial(),
                label = "ConnectedStartCorner_$index"
            )
            val endCorner by animateDpAsState(
                targetValue = targetEndCorner,
                animationSpec = SplitMateMotion.fastSpatial(),
                label = "ConnectedEndCorner_$index"
            )
            val animatedWeight by animateFloatAsState(
                targetValue = when {
                    isPressed -> 1.12f
                    checked -> 1.06f
                    else -> 1f
                },
                animationSpec = SplitMateMotion.fastSpatial(),
                label = "ConnectedWeight_$index"
            )

            val containerColor by animateColorAsState(
                targetValue = if (checked) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                animationSpec = SplitMateMotion.fastEffects(),
                label = "ConnectedBg_$index"
            )
            val contentColor by animateColorAsState(
                targetValue = if (checked) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = SplitMateMotion.fastEffects(),
                label = "ConnectedFg_$index"
            )

            val segmentShape = RoundedCornerShape(
                topStart = startCorner,
                bottomStart = startCorner,
                topEnd = endCorner,
                bottomEnd = endCorner
            )

            Surface(
                shape = segmentShape,
                color = containerColor,
                contentColor = contentColor,
                modifier = Modifier
                    .weight(animatedWeight)
                    .height(40.dp)
                    .clip(segmentShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onSelect(index, option) }
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
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
 * Material 3 Expressive `FloatingActionButtonMenu` that fans out staggered pill items
 * above a morphing [ToggleFloatingActionButton] using [SplitMateMotion.defaultSpatial].
 */
@Composable
fun FloatingActionButtonMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    items: List<ExpressiveFabMenuItem>,
    modifier: Modifier = Modifier,
    toggleIcon: ImageVector = Icons.Rounded.Add,
    toggleLabel: String? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = SplitMateMotion.fastEffects()) +
                scaleIn(
                    initialScale = 0.82f,
                    animationSpec = SplitMateMotion.defaultSpatial()
                ),
            exit = fadeOut(animationSpec = SplitMateMotion.fastEffects()) +
                scaleOut(
                    targetScale = 0.82f,
                    animationSpec = SplitMateMotion.defaultSpatial()
                )
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                items.forEach { item ->
                    val pillBg = item.containerColor ?: MaterialTheme.colorScheme.primaryContainer
                    val pillFg = item.contentColor ?: MaterialTheme.colorScheme.onPrimaryContainer
                    Surface(
                        onClick = {
                            onToggle()
                            item.onClick()
                        },
                        shape = CircleShape,
                        color = pillBg,
                        contentColor = pillFg,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = pillFg,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = item.label,
                                    style = SplitMateExpressiveTypography.labelLargeEmphasized,
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = pillFg
                                )
                                if (!item.subtitle.isNullOrBlank()) {
                                    Text(
                                        text = item.subtitle,
                                        fontFamily = FigtreeFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        color = pillFg.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ToggleFloatingActionButton(
            checked = expanded,
            onCheckedChange = { onToggle() },
            icon = toggleIcon,
            label = toggleLabel
        )
    }
}

// ==============================================================================
// 5. CONTAINED LOADING INDICATOR (7-POLYGON MORPHING LOADER)
// ==============================================================================

/**
 * Material 3 Expressive `ContainedLoadingIndicator` that cycles smoothly through
 * [MaterialShapes.morphSequence] (`SoftBurst` -> `Cookie9Sided` -> `Pentagon` -> `Pill`
 * -> `Sunny` -> `Clover4Leaf` -> `Oval`) inside a tonal `CircleShape` container.
 */
@Composable
fun ContainedLoadingIndicator(
    modifier: Modifier = Modifier,
    containerSize: Dp = 48.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    indicatorColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val sequence = MaterialShapes.morphSequence
    val morphs = remember(sequence) {
        sequence.indices.map { idx ->
            val start = sequence[idx]
            val end = sequence[(idx + 1) % sequence.size]
            Morph(start, end)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ContainedLoadingTransition")
    val stageFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = sequence.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = sequence.size * 650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ContainedLoadingMorphStage"
    )
    val rotationDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ContainedLoadingRotation"
    )

    val stageIndex = stageFloat.toInt().mod(morphs.size)
    val localProgress = (stageFloat - stageFloat.toInt()).coerceIn(0f, 1f)
    val activeMorph = morphs[stageIndex]

    Box(
        modifier = modifier
            .size(containerSize)
            .clip(CircleShape)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(containerSize * 0.62f)
                .clip(
                    MorphPolygonShape(
                        morph = activeMorph,
                        percentage = localProgress,
                        rotationDegrees = rotationDegrees
                    )
                )
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
                border = BorderStroke(1.dp, palette.outline.copy(alpha = 0.65f)),
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
                border = BorderStroke(1.dp, palette.outline.copy(alpha = 0.65f)),
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
 * [MaterialShapes.morphSequence] without an outer circular container.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    ContainedLoadingIndicator(
        modifier = modifier,
        containerSize = size,
        containerColor = Color.Transparent,
        indicatorColor = color
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
