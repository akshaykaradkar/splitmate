package com.splitmate.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.splitmate.app.R

/**
 * Figtree weight of the text the icons sit next to. Icons follow it (Material Symbols with
 * typography): provide it around a screen or component whose labels are heavier or lighter.
 * This only tunes ICON stroke weight; all text in SplitMate stays Figtree.
 */
val LocalSymbolTextWeight = staticCompositionLocalOf { FontWeight.Medium }

/** How Material Symbols Rounded axes are derived from the nearby Figtree text. */
object MaterialSymbolDefaults {
    /** Symbols `wght` axis (100..700) matched to the label weight; Figtree 800/900 cap at 700. */
    fun weightFor(textWeight: FontWeight): Int = textWeight.weight.coerceIn(100, 700)

    /**
     * Symbols `GRAD` axis. ExtraBold/Black labels need more weight than the 700 cap, so they get
     * the +200 emphasis grade; light glyphs on dark surfaces read heavier, so they get -25.
     */
    fun gradeFor(textWeight: FontWeight, darkSurface: Boolean): Int = when {
        textWeight.weight >= 800 -> 200
        darkSurface -> -25
        else -> 0
    }

    /** Symbols `opsz` axis (20..48) follows the rendered icon size in dp. */
    fun opticalSizeFor(sizeDp: Float): Float = sizeDp.coerceIn(20f, 48f)

    /** Codepoint for a Compose material icon, or null if it is not in the bundled subset. */
    fun codepointFor(imageVector: ImageVector): Int? =
        MaterialSymbolCodepoints[imageVector.name.substringAfterLast('.')]
}

/**
 * Drop-in replacement for `Icon(imageVector, contentDescription, modifier, tint)` that draws the
 * same icon from the Material Symbols Rounded variable font (res/font/material_symbols_rounded.ttf,
 * subset to the icons SplitMate uses). Stroke weight follows [textWeight], [filled] switches the
 * FILL axis (use it for selected states), optical size follows the rendered size, and AutoMirrored
 * icons flip in RTL. Sizing matches `Icon`: 24.dp unless the modifier sets a size. Icons missing
 * from the subset fall back to the original vector, so nothing ever disappears.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
@Composable
fun SymbolIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
    textWeight: FontWeight = LocalSymbolTextWeight.current
) {
    val codepoint = MaterialSymbolDefaults.codepointFor(imageVector)
    if (codepoint == null) {
        Icon(imageVector = imageVector, contentDescription = contentDescription, modifier = modifier, tint = tint)
        return
    }
    val color = if (tint.isSpecified) tint else LocalContentColor.current
    val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val wght = MaterialSymbolDefaults.weightFor(textWeight)
    val grad = MaterialSymbolDefaults.gradeFor(textWeight, darkSurface)
    val fill = if (filled) 1f else 0f
    val mirror = imageVector.autoMirror && LocalLayoutDirection.current == LayoutDirection.Rtl
    val glyph = remember(codepoint) { String(Character.toChars(codepoint)) }
    val textMeasurer = rememberTextMeasurer(cacheSize = 4)
    val semantics = if (contentDescription != null) {
        Modifier.clearAndSetSemantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier.clearAndSetSemantics { }
    }

    Spacer(
        modifier = modifier
            .then(semantics)
            .size(24.dp) // Icon's default; an earlier size in [modifier] wins.
            .drawWithCache {
                val px = size.minDimension
                val opsz = MaterialSymbolDefaults.opticalSizeFor(px / density)
                val family = FontFamily(
                    Font(
                        resId = R.font.material_symbols_rounded,
                        variationSettings = FontVariation.Settings(
                            FontVariation.Setting("FILL", fill),
                            FontVariation.weight(wght),
                            FontVariation.grade(grad),
                            FontVariation.Setting("opsz", opsz)
                        )
                    )
                )
                val layout = textMeasurer.measure(
                    text = glyph,
                    style = TextStyle(
                        fontFamily = family,
                        fontSize = px.toSp(),
                        lineHeight = px.toSp(),
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    ),
                    softWrap = false,
                    maxLines = 1
                )
                val topLeft = Offset(
                    (size.width - layout.size.width) / 2f,
                    (size.height - layout.size.height) / 2f
                )
                onDrawBehind {
                    if (mirror) {
                        scale(scaleX = -1f, scaleY = 1f) { drawText(layout, color = color, topLeft = topLeft) }
                    } else {
                        drawText(layout, color = color, topLeft = topLeft)
                    }
                }
            }
    )
}
