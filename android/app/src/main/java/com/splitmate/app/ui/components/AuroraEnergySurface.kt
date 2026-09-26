package com.splitmate.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.ui.DesignSystemBindings
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Canonical 5-state Google Material 3 (`gBreeze Energy` / `md-gb-energy`) lifecycle
 * from `google3/javascript/materialdesign/lib/aurora/energy/types.ts`.
 *
 * Strictly uses `type="accents"` (deriving colors from SplitMate's M3 Expressive
 * `--md-sys-color-*` tokens: Olive/Sage, Terracotta/Peach, and Periwinkle/Indigo)
 * rather than cold neon or rainbow gradients.
 */
enum class Gm3EnergyState(val label: String) {
    IDLE("idle"),
    ANTICIPATING("anticipating"),
    RECEIVING("receiving"),
    PROCESSING("processing"),
    RESPONDING("responding")
}

/**
 * Palette accent presets matching SplitMate's M3 Expressive tonal system.
 *
 * Light-mode tokens (`baseContainer`, `borderTint`) are the canonical Buckwheat values.
 * `darkBorderTint` is a lightened accent that keeps >= 4.5:1 contrast against the
 * GM3 dark card surface (`#24201C`).
 */
enum class Gm3EnergyAccentPalette(
    val baseContainer: Color,
    val primaryBlob: Color,
    val secondaryBlob: Color,
    val tertiaryBlob: Color,
    val borderTint: Color,
    val darkBorderTint: Color
) {
    /** Warm Olive + Sage + Peach for IRCTC Train PNR & Receipt Claim Engine */
    BUCKWHEAT_SAGE(
        baseContainer = Color(0xFFF7F3EC),
        primaryBlob = Color(0xFFD7E8B6),    // Soft Sage Container
        secondaryBlob = Color(0xFFFED8C8),  // Warm Peach Container
        tertiaryBlob = Color(0xFFE5F2D0),   // Light Olive Tonal
        borderTint = Color(0xFF416913),     // Deep Olive
        darkBorderTint = Color(0xFFB5DC86)  // Light Sage accent for dark surfaces
    ),

    /** Periwinkle + Lavender + Peach for Airline E-Ticket PDF & Boarding Pass Engine */
    AVIATION_PERIWINKLE(
        baseContainer = Color(0xFFF7F3EC),
        primaryBlob = Color(0xFFDCE3FD),    // Soft Periwinkle Container
        secondaryBlob = Color(0xFFEDE9FE),  // Lavender Tonal
        tertiaryBlob = Color(0xFFFED8C8),   // Warm Peach Accent
        borderTint = Color(0xFF3730A3),     // Deep Indigo
        darkBorderTint = Color(0xFFC7D2FE)  // Light Periwinkle accent for dark surfaces
    );

    /** Base container resolved against the current theme (dark: GM3 dark card surface `#24201C`). */
    val resolvedBaseContainer: Color
        get() = if (SplitMateTheme.isDark) DesignSystemBindings.GM3DarkCardSurface else baseContainer

    /** Resting (idle) border resolved against the current theme (dark: `#38312B`). */
    val resolvedRestingBorder: Color
        get() = if (SplitMateTheme.isDark) DesignSystemBindings.GM3DarkBorder else Color(0xFFEDE7DF)

    /** Accent tint (status dot, active border, state label) resolved against the current theme. */
    val resolvedAccentTint: Color
        get() = if (SplitMateTheme.isDark) darkBorderTint else borderTint

    /** Blob alpha multiplier: pastel blobs are dimmed on dark surfaces so light text stays legible. */
    val resolvedBlobAlphaScale: Float
        get() = if (SplitMateTheme.isDark) 0.12f else 1f
}

/**
 * Google Material 3 Expressive (`md-gb-energy`) Surface Container for Jetpack Compose.
 *
 * Guidelines enforced from `gbreeze_energy/SKILL.md`:
 * - At most 1 active energy surface per screen.
 * - `ANTICIPATING` automatically transitions to `IDLE` after `1800ms` so it never loops forever.
 * - Base container color is preserved (`--color: var(--md-sys-color-surface-container)`), while
 *   tonal accent blobs softly orbit only when `active && state != IDLE`.
 */
@Composable
fun Gm3AuroraEnergySurface(
    state: Gm3EnergyState,
    onStateAutoTransition: ((Gm3EnergyState) -> Unit)? = null,
    palette: Gm3EnergyAccentPalette = Gm3EnergyAccentPalette.BUCKWHEAT_SAGE,
    intensity: Float = 0.65f,
    dynamicIntensity: Float = 0f,
    shape: Shape = RoundedCornerShape(24.dp),
    borderWidth: Dp = 1.dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // Auto-transition rule from gbreeze_energy/SKILL.md:
    // When starting in state="anticipating", auto-transition to state="idle" after 1800ms.
    LaunchedEffect(state) {
        if (state == Gm3EnergyState.ANTICIPATING && onStateAutoTransition != null) {
            delay(1800L)
            onStateAutoTransition(Gm3EnergyState.IDLE)
        } else if (state == Gm3EnergyState.RESPONDING && onStateAutoTransition != null) {
            delay(1200L)
            onStateAutoTransition(Gm3EnergyState.IDLE)
        }
    }

    val targetAlpha = when (state) {
        Gm3EnergyState.IDLE -> 0f
        Gm3EnergyState.ANTICIPATING -> (0.55f * intensity).coerceIn(0f, 0.85f)
        Gm3EnergyState.RECEIVING -> (0.60f * (intensity + dynamicIntensity * 0.35f)).coerceIn(0f, 0.9f)
        Gm3EnergyState.PROCESSING -> (0.85f * intensity).coerceIn(0.35f, 0.95f)
        Gm3EnergyState.RESPONDING -> (0.72f * (intensity + dynamicIntensity * 0.25f)).coerceIn(0f, 0.9f)
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "gm3EnergyAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "gm3EnergyShader")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    Gm3EnergyState.PROCESSING -> 2200
                    Gm3EnergyState.RESPONDING -> 2800
                    else -> 3600
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "gm3EnergyPhase"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gm3EnergyPulse"
    )

    val baseContainer = palette.resolvedBaseContainer
    val restingBorder = palette.resolvedRestingBorder
    val accentTint = palette.resolvedAccentTint
    val blobAlphaScale = palette.resolvedBlobAlphaScale

    Box(
        modifier = modifier
            .clip(shape)
            .background(baseContainer)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val radius = maxOf(w, h) * 0.68f * pulse

                val c1 = Offset(
                    x = w * (0.28f + 0.25f * cos(phase)),
                    y = h * (0.32f + 0.22f * sin(phase))
                )
                val c2 = Offset(
                    x = w * (0.72f + 0.24f * cos(phase + 2.1f)),
                    y = h * (0.65f + 0.24f * sin(phase + 2.1f))
                )
                val c3 = Offset(
                    x = w * (0.50f + 0.28f * sin(phase * 0.8f + 4.2f)),
                    y = h * (0.45f + 0.20f * cos(phase * 0.8f + 4.2f))
                )

                onDrawBehind {
                    val blobAlpha = animatedAlpha * blobAlphaScale
                    if (blobAlpha > 0.01f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    palette.primaryBlob.copy(alpha = blobAlpha * 0.85f),
                                    palette.primaryBlob.copy(alpha = 0f)
                                ),
                                center = c1,
                                radius = radius
                            ),
                            radius = radius,
                            center = c1
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    palette.secondaryBlob.copy(alpha = blobAlpha * 0.78f),
                                    palette.secondaryBlob.copy(alpha = 0f)
                                ),
                                center = c2,
                                radius = radius * 0.9f
                            ),
                            radius = radius * 0.9f,
                            center = c2
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    palette.tertiaryBlob.copy(alpha = blobAlpha * 0.72f),
                                    palette.tertiaryBlob.copy(alpha = 0f)
                                ),
                                center = c3,
                                radius = radius * 0.8f
                            ),
                            radius = radius * 0.8f,
                            center = c3
                        )
                    }
                }
            }
            .border(
                width = borderWidth,
                color = if (animatedAlpha > 0.05f) {
                    accentTint.copy(alpha = 0.22f + animatedAlpha * 0.28f)
                } else {
                    restingBorder
                },
                shape = shape
            ),
        content = content
    )
}

/**
 * Non-interactive read-only GM3 AI Status Pill (`role="status"` equivalent from `gbreeze_energy/SKILL.md`).
 * Never uses a clickable `<Button>` host so screen readers and users don't get false button affordances.
 */
@Composable
fun Gm3EnergyStatusPill(
    state: Gm3EnergyState,
    statusText: String,
    palette: Gm3EnergyAccentPalette = Gm3EnergyAccentPalette.BUCKWHEAT_SAGE,
    modifier: Modifier = Modifier
) {
    Gm3AuroraEnergySurface(
        state = state,
        palette = palette,
        intensity = 0.8f,
        shape = CircleShape,
        borderWidth = 1.dp,
        modifier = modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = "AI state ${state.label}: $statusText"
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(palette.resolvedAccentTint)
            )
            Text(
                text = statusText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (SplitMateTheme.isDark) DesignSystemBindings.GM3DarkPrimaryText else Color(0xFF23201E)
            )
            Text(
                text = "• ${state.label}",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.resolvedAccentTint
            )
        }
    }
}
