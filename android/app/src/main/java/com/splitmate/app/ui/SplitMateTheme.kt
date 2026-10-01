package com.splitmate.app.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.ui.components.ContainedLoadingIndicator
import com.splitmate.app.ui.components.LocalMotionScheme
import com.splitmate.app.ui.components.LocalReducedMotion
import com.splitmate.app.ui.components.SplitMateMotion
import com.splitmate.app.ui.components.SplitMateMotionScheme
import com.splitmate.app.ui.components.rememberReducedMotionEnabled
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.R
import java.net.URLEncoder
import java.util.Locale

// ==============================================================================
// 3-THEME MATERIAL 3 EXPRESSIVE ENGINE (v2.3.0)
// ==============================================================================
enum class SplitMateThemeMode(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val isDark: Boolean
) {
    SUNLIT_BUCKWHEAT(
        id = "SUNLIT_BUCKWHEAT",
        displayName = "Sunlit Buckwheat",
        subtitle = "Warm Cream Expressive Daylight",
        isDark = false
    ),
    WARM_ESPRESSO_NIGHT(
        id = "WARM_ESPRESSO_NIGHT",
        displayName = "Warm Espresso Night",
        subtitle = "Tactile Amber-on-Espresso Dark",
        isDark = true
    ),
    KYOTO_MATCHA_YUZU(
        id = "KYOTO_MATCHA_YUZU",
        displayName = "Kyoto Matcha & Yuzu",
        subtitle = "Botanical Stationery & Hanko Coral",
        isDark = false
    );

    companion object {
        @JvmOverloads
        fun fromId(id: String?, fallbackDark: Boolean = false): SplitMateThemeMode {
            val clean = id?.trim().orEmpty()
            if (clean.isEmpty()) {
                return if (fallbackDark) WARM_ESPRESSO_NIGHT else SUNLIT_BUCKWHEAT
            }
            return values().firstOrNull {
                it.id.equals(clean, ignoreCase = true) ||
                    it.displayName.equals(clean, ignoreCase = true) ||
                    it.name.equals(clean.replace(" ", "_"), ignoreCase = true)
            } ?: if (fallbackDark) WARM_ESPRESSO_NIGHT else SUNLIT_BUCKWHEAT
        }
    }
}

data class SplitMateExpressivePalette(
    val mode: SplitMateThemeMode,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val outline: Color,
    val outlineVariant: Color
)

val SunlitBuckwheatPalette = SplitMateExpressivePalette(
    mode = SplitMateThemeMode.SUNLIT_BUCKWHEAT,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF6F0),
    surfaceContainer = Color(0xFFF4EFE6),
    surfaceContainerHigh = Color(0xFFEDE6DA),
    surfaceContainerHighest = Color(0xFFE4DCCD),
    onSurface = Color(0xFF23201E),
    onSurfaceVariant = Color(0xFF6E675F),
    primary = Color(0xFF365314),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9F99D),
    onPrimaryContainer = Color(0xFF1A2E05),
    secondary = Color(0xFFE06B52),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFED8C8),
    onSecondaryContainer = Color(0xFF7C2D12),
    tertiary = Color(0xFF3730A3),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCE3FD),
    onTertiaryContainer = Color(0xFF312E81),
    outline = Color(0xFFEDE7DF),
    outlineVariant = Color(0xFFE2D9CC)
)

val WarmEspressoNightPalette = SplitMateExpressivePalette(
    mode = SplitMateThemeMode.WARM_ESPRESSO_NIGHT,
    surfaceContainerLowest = Color(0xFF14110F),
    surfaceContainerLow = Color(0xFF1C1815),
    surfaceContainer = Color(0xFF25201C),
    surfaceContainerHigh = Color(0xFF302A24),
    surfaceContainerHighest = Color(0xFF3B332C),
    onSurface = Color(0xFFF5F0E6),
    onSurfaceVariant = Color(0xFFB5ACA2),
    primary = Color(0xFFA3E635),
    onPrimary = Color(0xFF1A2E05),
    primaryContainer = Color(0xFF283D0E),
    onPrimaryContainer = Color(0xFFD9F99D),
    secondary = Color(0xFFFB923C),
    onSecondary = Color(0xFF431A08),
    secondaryContainer = Color(0xFF431A08),
    onSecondaryContainer = Color(0xFFFED8C8),
    tertiary = Color(0xFFA5B4FC),
    onTertiary = Color(0xFF1E1B4B),
    tertiaryContainer = Color(0xFF1E1B4B),
    onTertiaryContainer = Color(0xFFE0E7FF),
    outline = Color(0xFF38312B),
    outlineVariant = Color(0xFF2E2924)
)

val KyotoMatchaYuzuPalette = SplitMateExpressivePalette(
    mode = SplitMateThemeMode.KYOTO_MATCHA_YUZU,
    surfaceContainerLowest = Color(0xFFEDF7EA),
    surfaceContainerLow = Color(0xFFDDF0D5),
    surfaceContainer = Color(0xFFCCE6C2),
    surfaceContainerHigh = Color(0xFFBBDAAF),
    surfaceContainerHighest = Color(0xFFA8CE9A),
    onSurface = Color(0xFF0A1F12),
    onSurfaceVariant = Color(0xFF1E3F29),
    primary = Color(0xFF0F6B3E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF86EFAC),
    onPrimaryContainer = Color(0xFF062E19),
    secondary = Color(0xFFB45309),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEF08A),
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = Color(0xFF6B21A8),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E8FF),
    onTertiaryContainer = Color(0xFF3B0764),
    outline = Color(0xFF86B87A),
    outlineVariant = Color(0xFFA3CC97)
)

fun SplitMateThemeMode.toPalette(): SplitMateExpressivePalette = when (this) {
    SplitMateThemeMode.SUNLIT_BUCKWHEAT -> SunlitBuckwheatPalette
    SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> WarmEspressoNightPalette
    SplitMateThemeMode.KYOTO_MATCHA_YUZU -> KyotoMatchaYuzuPalette
}

val LocalSplitMatePalette = staticCompositionLocalOf { SunlitBuckwheatPalette }

/**
 * v2.3.4 (Decision #6): the M3 tonal surface roles resolved from a [SplitMateExpressivePalette].
 *
 * Before v2.3.4 `SplitMateExpressiveTheme` never set `colorScheme.surfaceContainer*`, so any
 * component reading them (ModalBottomSheet, DropdownMenu, AlertDialog, ElevatedCard, ...) fell
 * back to the cool M3 baseline greys. These roles map the Buckwheat ladder instead:
 * Lowest `#FFFFFF` card, Low `#FAF6F0` canvas, default `#F4EFE6` band, High `#EDE6DA` wells,
 * Highest `#E4DCCD` (Espresso Night / Kyoto Matcha use their own ladders).
 *
 * `bright` / `dim` follow M3 semantics: light schemes are brightest at the canvas and dimmest at
 * Highest; dark schemes are brightest at Highest and dimmest at Lowest.
 */
data class SplitMateSurfaceRoles(
    val lowest: Color,
    val low: Color,
    val container: Color,
    val high: Color,
    val highest: Color,
    val bright: Color,
    val dim: Color
)

/** Pure mapping from a palette to the M3 tonal surface roles (see [SplitMateSurfaceRoles]). */
fun SplitMateExpressivePalette.surfaceContainerRoles(): SplitMateSurfaceRoles = SplitMateSurfaceRoles(
    lowest = surfaceContainerLowest,
    low = surfaceContainerLow,
    container = surfaceContainer,
    high = surfaceContainerHigh,
    highest = surfaceContainerHighest,
    bright = if (mode.isDark) surfaceContainerHighest else surfaceContainerLow,
    dim = if (mode.isDark) surfaceContainerLowest else surfaceContainerHighest
)

object SplitMateThemeState {
    var activeThemeMode by mutableStateOf(SplitMateThemeMode.SUNLIT_BUCKWHEAT)
    val activePalette: SplitMateExpressivePalette
        get() = activeThemeMode.toPalette()
}

// ==============================================================================
// CANONICAL BINDINGS TO THE 4 GOOGLE DESIGN SYSTEMS (`design_systems/`)
// ==============================================================================
object DesignSystemBindings {
    var activeThemeMode: SplitMateThemeMode
        get() = SplitMateThemeState.activeThemeMode
        set(value) {
            SplitMateThemeState.activeThemeMode = value
        }

    val activePalette: SplitMateExpressivePalette
        get() = SplitMateThemeState.activePalette

    // 1. Google Material 3 (`design_systems/google-material-3/DESIGN.md`)
    val GM3LightBackground = Color(0xFFFAF7F2)
    val GM3LightCardSurface = Color(0xFFFFFFFF)
    val GM3LightKeypadSurface = Color(0xFFF3EFEA)
    val GM3LightPrimaryText = Color(0xFF23201E)
    val GM3LightSubtitleText = Color(0xFF6E6863)

    // Warm Espresso Night Theme (#181512 Leather-Journal Dark Palette)
    val GM3DarkBackground = Color(0xFF181512)
    val GM3DarkCardSurface = Color(0xFF24201C)
    val GM3DarkKeypadSurface = Color(0xFF2E2823)
    val GM3DarkPrimaryText = Color(0xFFFAF6F0)
    val GM3DarkSubtitleText = Color(0xFFB5ACA2)
    val GM3DarkBorder = Color(0xFF38312B)

    val GM3ShapeExtraLarge = RoundedCornerShape(28.dp)
    val GM3ShapeLarge = RoundedCornerShape(24.dp)
    val GM3ShapePill = RoundedCornerShape(50)

    // 2. Android Motion (`design_systems/android-motion/DESIGN.md`) — Upgraded to M3 Expressive Springs
    fun <T> themeColorTween(): FiniteAnimationSpec<T> = SplitMateMotion.slowEffects()
    fun <T> tactileSpring(): SpringSpec<T> = SplitMateMotion.defaultSpatial()

    // 3. Elements GM3 (`design_systems/elements-3/DESIGN.md`)
    val ElementsCreditorContainer = Color(0xFFD7E8B6)
    val ElementsCreditorOnContainer = Color(0xFF2D4810)
    val ElementsDebtorContainer = Color(0xFFFED8C8)
    val ElementsDebtorOnContainer = Color(0xFF7C2D12)
    val ElementsInfoPeriwinkle = Color(0xFFDCE3FD)

    val ElementsPositiveContainer = Color(0xFFD7E8B6)
    val ElementsPositiveText = Color(0xFF416913)
    val ElementsNegativeText = Color(0xFFE06B52)

    // 4. Android Pixel Design System (`design_systems/android-pixel-design-system/DESIGN.md`)
    val PixelCardInternalPadding = 14.dp
    val PixelSectionSpacing = 12.dp
    val PixelCompactItemSpacing = 8.dp
}

// Stitch "Organic Tactile Financial" (Buckwheat) + HCT Expressive Tokens (Reactive to Kyoto Matcha & Yuzu)
val BuckwheatCanvas: Color
    get() = if (DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) KyotoMatchaYuzuPalette.surfaceContainerLow else Color(0xFFFAF6F0)
val BuckwheatSurface: Color
    get() = if (DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) KyotoMatchaYuzuPalette.surfaceContainerLowest else Color(0xFFFFFFFF)
val BuckwheatSunken: Color
    get() = if (DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) KyotoMatchaYuzuPalette.surfaceContainer else Color(0xFFF4EFE6)
val BuckwheatCharcoal: Color
    get() = if (DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) KyotoMatchaYuzuPalette.onSurface else Color(0xFF23201E)
val BuckwheatSecondaryText: Color
    get() = if (DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) KyotoMatchaYuzuPalette.onSurfaceVariant else Color(0xFF6E675F)
val BuckwheatBorder: Color
    get() = if (DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) KyotoMatchaYuzuPalette.outline else Color(0xFFEDE7DF)

val BuckwheatOlivePrimary: Color
    get() = if (DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) KyotoMatchaYuzuPalette.primary else Color(0xFF365314)
val BuckwheatSageContainer: Color
    get() = if (DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) KyotoMatchaYuzuPalette.primaryContainer else Color(0xFFD7E8B6)
val BuckwheatTerracotta: Color
    get() = if (DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) KyotoMatchaYuzuPalette.secondary else Color(0xFFE06B52)
val BuckwheatPeachContainer: Color
    get() = if (DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) KyotoMatchaYuzuPalette.secondaryContainer else Color(0xFFFED8C8)
val BuckwheatTerracottaDark: Color
    get() = if (DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) KyotoMatchaYuzuPalette.onSecondaryContainer else Color(0xFF7C2D12)
val BuckwheatLavenderContainer: Color
    get() = if (DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) KyotoMatchaYuzuPalette.tertiaryContainer else Color(0xFFDCE3FD)
val BuckwheatLavenderText: Color
    get() = if (DesignSystemBindings.activeThemeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU) KyotoMatchaYuzuPalette.tertiary else Color(0xFF3730A3)

private val SplitMateLightColorScheme = lightColorScheme(
    primary = SunlitBuckwheatPalette.primary,
    onPrimary = SunlitBuckwheatPalette.onPrimary,
    primaryContainer = SunlitBuckwheatPalette.primaryContainer,
    onPrimaryContainer = SunlitBuckwheatPalette.onPrimaryContainer,
    secondary = SunlitBuckwheatPalette.secondary,
    onSecondary = SunlitBuckwheatPalette.onSecondary,
    secondaryContainer = SunlitBuckwheatPalette.secondaryContainer,
    onSecondaryContainer = SunlitBuckwheatPalette.onSecondaryContainer,
    tertiary = SunlitBuckwheatPalette.tertiary,
    onTertiary = SunlitBuckwheatPalette.onTertiary,
    tertiaryContainer = SunlitBuckwheatPalette.tertiaryContainer,
    onTertiaryContainer = SunlitBuckwheatPalette.onTertiaryContainer,
    background = SunlitBuckwheatPalette.surfaceContainerLow,
    onBackground = SunlitBuckwheatPalette.onSurface,
    surface = SunlitBuckwheatPalette.surfaceContainerLowest,
    onSurface = SunlitBuckwheatPalette.onSurface,
    surfaceVariant = SunlitBuckwheatPalette.surfaceContainer,
    onSurfaceVariant = SunlitBuckwheatPalette.onSurfaceVariant,
    outline = SunlitBuckwheatPalette.outline,
    outlineVariant = SunlitBuckwheatPalette.outlineVariant
)

private val SplitMateDarkColorScheme = darkColorScheme(
    primary = WarmEspressoNightPalette.primary,
    onPrimary = WarmEspressoNightPalette.onPrimary,
    primaryContainer = WarmEspressoNightPalette.primaryContainer,
    onPrimaryContainer = WarmEspressoNightPalette.onPrimaryContainer,
    secondary = WarmEspressoNightPalette.secondary,
    onSecondary = WarmEspressoNightPalette.onSecondary,
    secondaryContainer = WarmEspressoNightPalette.secondaryContainer,
    onSecondaryContainer = WarmEspressoNightPalette.onSecondaryContainer,
    tertiary = WarmEspressoNightPalette.tertiary,
    onTertiary = WarmEspressoNightPalette.onTertiary,
    tertiaryContainer = WarmEspressoNightPalette.tertiaryContainer,
    onTertiaryContainer = WarmEspressoNightPalette.onTertiaryContainer,
    background = WarmEspressoNightPalette.surfaceContainerLow,
    onBackground = WarmEspressoNightPalette.onSurface,
    surface = WarmEspressoNightPalette.surfaceContainerLowest,
    onSurface = WarmEspressoNightPalette.onSurface,
    surfaceVariant = WarmEspressoNightPalette.surfaceContainer,
    onSurfaceVariant = WarmEspressoNightPalette.onSurfaceVariant,
    outline = WarmEspressoNightPalette.outline,
    outlineVariant = WarmEspressoNightPalette.outlineVariant
)

private val SplitMateKyotoMatchaColorScheme = lightColorScheme(
    primary = KyotoMatchaYuzuPalette.primary,
    onPrimary = KyotoMatchaYuzuPalette.onPrimary,
    primaryContainer = KyotoMatchaYuzuPalette.primaryContainer,
    onPrimaryContainer = KyotoMatchaYuzuPalette.onPrimaryContainer,
    secondary = KyotoMatchaYuzuPalette.secondary,
    onSecondary = KyotoMatchaYuzuPalette.onSecondary,
    secondaryContainer = KyotoMatchaYuzuPalette.secondaryContainer,
    onSecondaryContainer = KyotoMatchaYuzuPalette.onSecondaryContainer,
    tertiary = KyotoMatchaYuzuPalette.tertiary,
    onTertiary = KyotoMatchaYuzuPalette.onTertiary,
    tertiaryContainer = KyotoMatchaYuzuPalette.tertiaryContainer,
    onTertiaryContainer = KyotoMatchaYuzuPalette.onTertiaryContainer,
    background = KyotoMatchaYuzuPalette.surfaceContainerLow,
    onBackground = KyotoMatchaYuzuPalette.onSurface,
    surface = KyotoMatchaYuzuPalette.surfaceContainerLowest,
    onSurface = KyotoMatchaYuzuPalette.onSurface,
    surfaceVariant = KyotoMatchaYuzuPalette.surfaceContainer,
    onSurfaceVariant = KyotoMatchaYuzuPalette.onSurfaceVariant,
    outline = KyotoMatchaYuzuPalette.outline,
    outlineVariant = KyotoMatchaYuzuPalette.outlineVariant
)

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val figtreeGoogleFont = GoogleFont("Figtree")

// Bundled TrueType static Figtree fonts (400 Regular, 500 Medium, 600 SemiBold, 700 Bold, 800 ExtraBold)
// Guarantees 100% deterministic offline Figtree typography on every screen, dialog, and sheet with zero Roboto fallback
val FigtreeFontFamily = FontFamily(
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_regular, weight = FontWeight.Light),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_regular, weight = FontWeight.Normal),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_medium, weight = FontWeight.Medium),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_semibold, weight = FontWeight.SemiBold),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_bold, weight = FontWeight.Bold),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_extrabold, weight = FontWeight.ExtraBold),
    androidx.compose.ui.text.font.Font(resId = R.font.figtree_extrabold, weight = FontWeight.Black)
)

/**
 * Formats any group title into editorial Title Case regardless of how the user typed it
 * (e.g., "goa beach trip 2026" -> "Goa Beach Trip 2026", "MUMBAI FLIGHT" -> "Mumbai Flight").
 */
fun String.toSmartTitleCase(): String {
    val trimmed = this.trim()
    if (trimmed.isEmpty()) return ""
    return trimmed
        .split(Regex("\\s+"))
        .joinToString(" ") { word ->
            if (word.isEmpty()) ""
            else word.lowercase(java.util.Locale.US).replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(java.util.Locale.US) else ch.toString()
            }
        }
}

// 3-Voice Typography:
// 1. Editorial Display (`PlusJakartaSansFont` backed by deterministic offline Figtree ExtraBold/Black)
// 2. Conversational UI (`FigtreeFontFamily`)
// 3. True Tabular Monospace (`JetBrainsMonoFont` / `SplitMateTnumMonospace` backed by `FontFamily.Monospace` with `"tnum, zero"`)
val PlusJakartaSansFont: FontFamily = FigtreeFontFamily
val JetBrainsMonoFont: FontFamily = FontFamily.Monospace
val SplitMateTnumMonospace: FontFamily = FontFamily.Monospace

val SplitMateMonospaceTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum, zero"
)

val SplitMateTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = "tnum, zero"
    ),
    displayMedium = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = "tnum, zero"
    ),
    displaySmall = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-1.0).sp,
        fontFeatureSettings = "tnum, zero"
    ),
    headlineLarge = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum, zero"
    ),
    headlineMedium = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp,
        fontFeatureSettings = "tnum, zero"
    ),
    headlineSmall = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontFeatureSettings = "tnum, zero"
    ),
    titleLarge = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontFeatureSettings = "tnum, zero"
    ),
    titleMedium = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "tnum, zero"
    ),
    titleSmall = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum, zero"
    ),
    bodyLarge = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum, zero"
    ),
    bodyMedium = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum, zero"
    ),
    bodySmall = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum, zero"
    ),
    labelLarge = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        fontFeatureSettings = "tnum, zero"
    ),
    labelMedium = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum, zero"
    ),
    labelSmall = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum, zero"
    )
)

/**
 * All 15 Material 3 Expressive `*Emphasized` Typography Tokens (`displayLargeEmphasized` through `labelSmallEmphasized`).
 */
object SplitMateExpressiveTypography {
    val displayLargeEmphasized = SplitMateTypography.displayLarge.copy(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Black,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-1.8).sp,
        fontFeatureSettings = "tnum, zero"
    )
    val displayMediumEmphasized = SplitMateTypography.displayMedium.copy(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Black,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1.6).sp,
        fontFeatureSettings = "tnum, zero"
    )
    val displaySmallEmphasized = SplitMateTypography.displaySmall.copy(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Black,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.2).sp,
        fontFeatureSettings = "tnum, zero"
    )
    val headlineLargeEmphasized = SplitMateTypography.headlineLarge.copy(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.7).sp,
        fontFeatureSettings = "tnum, zero"
    )
    val headlineMediumEmphasized = SplitMateTypography.headlineMedium.copy(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
        fontFeatureSettings = "tnum, zero"
    )
    val headlineSmallEmphasized = SplitMateTypography.headlineSmall.copy(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
        fontFeatureSettings = "tnum, zero"
    )
    val titleLargeEmphasized = SplitMateTypography.titleLarge.copy(
        fontFamily = PlusJakartaSansFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp,
        fontFeatureSettings = "tnum, zero"
    )
    val titleMediumEmphasized = SplitMateTypography.titleMedium.copy(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = "tnum, zero"
    )
    val titleSmallEmphasized = SplitMateTypography.titleSmall.copy(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp,
        fontFeatureSettings = "tnum, zero"
    )
    val bodyLargeEmphasized = SplitMateTypography.bodyLarge.copy(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontFeatureSettings = "tnum, zero"
    )
    val bodyMediumEmphasized = SplitMateTypography.bodyMedium.copy(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "tnum, zero"
    )
    val bodySmallEmphasized = SplitMateTypography.bodySmall.copy(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum, zero"
    )
    val labelLargeEmphasized = SplitMateTypography.labelLarge.copy(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
        fontFeatureSettings = "tnum, zero"
    )
    val labelMediumEmphasized = SplitMateTypography.labelMedium.copy(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        fontFeatureSettings = "tnum, zero"
    )
    val labelSmallEmphasized = SplitMateTypography.labelSmall.copy(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        fontFeatureSettings = "tnum, zero"
    )
}

val Typography.displayLargeEmphasized: TextStyle get() = SplitMateExpressiveTypography.displayLargeEmphasized
val Typography.displayMediumEmphasized: TextStyle get() = SplitMateExpressiveTypography.displayMediumEmphasized
val Typography.displaySmallEmphasized: TextStyle get() = SplitMateExpressiveTypography.displaySmallEmphasized
val Typography.headlineLargeEmphasized: TextStyle get() = SplitMateExpressiveTypography.headlineLargeEmphasized
val Typography.headlineMediumEmphasized: TextStyle get() = SplitMateExpressiveTypography.headlineMediumEmphasized
val Typography.headlineSmallEmphasized: TextStyle get() = SplitMateExpressiveTypography.headlineSmallEmphasized
val Typography.titleLargeEmphasized: TextStyle get() = SplitMateExpressiveTypography.titleLargeEmphasized
val Typography.titleMediumEmphasized: TextStyle get() = SplitMateExpressiveTypography.titleMediumEmphasized
val Typography.titleSmallEmphasized: TextStyle get() = SplitMateExpressiveTypography.titleSmallEmphasized
val Typography.bodyLargeEmphasized: TextStyle get() = SplitMateExpressiveTypography.bodyLargeEmphasized
val Typography.bodyMediumEmphasized: TextStyle get() = SplitMateExpressiveTypography.bodyMediumEmphasized
val Typography.bodySmallEmphasized: TextStyle get() = SplitMateExpressiveTypography.bodySmallEmphasized
val Typography.labelLargeEmphasized: TextStyle get() = SplitMateExpressiveTypography.labelLargeEmphasized
val Typography.labelMediumEmphasized: TextStyle get() = SplitMateExpressiveTypography.labelMediumEmphasized
val Typography.labelSmallEmphasized: TextStyle get() = SplitMateExpressiveTypography.labelSmallEmphasized

/**
 * Extracts strictly the first 1 or 2 uppercase letters of the person's ACTUAL NAME.
 * Never renders "Masculine", "Feminine", "Neutral", "Male", "Female", or numeric suffixes.
 */
fun extractInitialsFromNameOrSeed(rawNameOrSeed: String): String {
    val basePart = rawNameOrSeed
        .substringBefore("|")
        .substringBefore("_")
        .replace("(You)", "", ignoreCase = true)
        .replace(Regex("\\b(Masculine|Feminine|Neutral|Male|Female)\\b", RegexOption.IGNORE_CASE), "")
        .trim()

    val words = basePart.split(Regex("\\s+")).filter { it.isNotBlank() && it.first().isLetter() }
    return when {
        words.size >= 2 -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
        words.size == 1 -> words[0].take(1).uppercase(Locale.US)
        else -> basePart.firstOrNull { it.isLetter() }?.uppercaseChar()?.toString() ?: "S"
    }
}

// ==============================================================================
// BLUSH OPEN-PEEPS GROUP SCENES & OFFICIAL DICEBEAR 9.x AVATAR STUDIO ENGINE
// ==============================================================================

data class DiceBearStyleSpec(
    val id: String,
    val label: String,
    val subtitle: String,
    val creator: String = ""
)

val SplitMateDiceBearStyles: List<DiceBearStyleSpec> = listOf(
    DiceBearStyleSpec("open-peeps", "Open Peeps", "Hand-Drawn Ink", "Pablo Stanley"),
    DiceBearStyleSpec("adventurer", "Adventurer", "Travel Crew", "Lisa Wischofsky"),
    DiceBearStyleSpec("avataaars", "Avataaars", "Classic Studio", "Pablo Stanley"),
    DiceBearStyleSpec("big-ears", "Big Ears", "Playful Portrait", "The Visual Team"),
    DiceBearStyleSpec("big-smile", "Big Smile", "Cheerful Grin", "Ashley Seo"),
    DiceBearStyleSpec("croodles", "Croodles", "Sketch Doodle", "Vijay Verma"),
    DiceBearStyleSpec("dylan", "Dylan", "Editorial Pop", "Natalia Spivak"),
    DiceBearStyleSpec("lorelei", "Lorelei", "Expressive", "Lisa Wischofsky"),
    DiceBearStyleSpec("micah", "Micah", "Warm Modern", "Micah Lanier"),
    DiceBearStyleSpec("miniavs", "Miniavs", "Compact Minimal", "Webpixels"),
    DiceBearStyleSpec("notionists", "Notionists", "Minimalist", "Zoish"),
    DiceBearStyleSpec("personas", "Personas", "Modern Avatar", "Draftbit"),
    DiceBearStyleSpec("toon-head", "Toon Head", "2026 Cartoon", "Johan Melin")
)

val AVATAR_STYLE_CATALOG: List<DiceBearStyleSpec> = SplitMateDiceBearStyles
val DiceBearStyleCatalog: List<DiceBearStyleSpec> = SplitMateDiceBearStyles

data class AvatarColorPresetSpec(
    val id: String,
    val label: String,
    val hexCsv: String,
    val primaryBgColor: Color,
    val accentRingColor: Color,
    val openPeepsExtras: String = "",
    val universalExtras: String = "",
    val secondarySwatchColor: Color = primaryBgColor
)

val SplitMateAvatarColorPresets: List<AvatarColorPresetSpec> = listOf(
    AvatarColorPresetSpec(
        id = "Bare",
        label = "Bare",
        hexCsv = "transparent",
        primaryBgColor = Color(0xFFFAF6F0),
        accentRingColor = Color(0xFF365314),
        openPeepsExtras = "accessoriesProbability=0&maskProbability=0&facialHairProbability=0",
        universalExtras = "",
        secondarySwatchColor = Color(0xFFEDE7DF)
    ),
    AvatarColorPresetSpec(
        id = "PastelWall",
        label = "Pastel Wall",
        hexCsv = "ffe3ea,e3edff,e2f5e9,fdf1d4,efe6ff",
        primaryBgColor = Color(0xFFFFE3EA),
        accentRingColor = Color(0xFF365314),
        openPeepsExtras = "",
        universalExtras = "backgroundColor=ffe3ea,e3edff,e2f5e9,fdf1d4,efe6ff",
        secondarySwatchColor = Color(0xFFE3EDFF)
    ),
    AvatarColorPresetSpec(
        id = "BoldPop",
        label = "Bold Pop",
        hexCsv = "ff8fab,ffb703,4cc9a7,4d96ff,b57bff",
        primaryBgColor = Color(0xFFFF8FAB),
        accentRingColor = Color(0xFF4D96FF),
        openPeepsExtras = "",
        universalExtras = "backgroundColor=ff8fab,ffb703,4cc9a7,4d96ff,b57bff",
        secondarySwatchColor = Color(0xFFFFB703)
    ),
    AvatarColorPresetSpec(
        id = "Sunrise",
        label = "Sunrise",
        hexCsv = "ffd9b0,ffa8bf",
        primaryBgColor = Color(0xFFFFD9B0),
        accentRingColor = Color(0xFFE06B52),
        openPeepsExtras = "",
        universalExtras = "backgroundColor=ffd9b0,ffa8bf&backgroundType=gradientLinear&backgroundRotation=135",
        secondarySwatchColor = Color(0xFFFFA8BF)
    ),
    AvatarColorPresetSpec(
        id = "Muted",
        label = "Muted",
        hexCsv = "ece7de",
        primaryBgColor = Color(0xFFECE7DE),
        accentRingColor = Color(0xFF6B705C),
        openPeepsExtras = "clothingColor=6b705c,a5a58d,b98b73,7c9082,8e9aaf,9c6b58,8a7f6d&headContrastColor=2c1b18,4a312c,724133,a55728,b58143",
        universalExtras = "backgroundColor=ece7de",
        secondarySwatchColor = Color(0xFFA5A58D)
    ),
    AvatarColorPresetSpec(
        id = "Electric",
        label = "Electric",
        hexCsv = "101216",
        primaryBgColor = Color(0xFF101216),
        accentRingColor = Color(0xFF00E5FF),
        openPeepsExtras = "clothingColor=ff2e88,00e5ff,ffe600,7cff00,ff6a00,b400ff&headContrastColor=ff2e88,00e5ff,ffe600,7cff00,ff6a00,b400ff",
        universalExtras = "backgroundColor=101216",
        secondarySwatchColor = Color(0xFFFF2E88)
    ),
    AvatarColorPresetSpec(
        id = "NightShift",
        label = "Night Shift",
        hexCsv = "262b36",
        primaryBgColor = Color(0xFF262B36),
        accentRingColor = Color(0xFF78E185),
        openPeepsExtras = "clothingColor=fdea6b,78e185,9ddadb,ffcf77,e78276&headContrastColor=e8e1e1,ecdcbf,d6b370,f59797,b58143",
        universalExtras = "backgroundColor=262b36",
        secondarySwatchColor = Color(0xFFFDEA6B)
    ),
    AvatarColorPresetSpec(
        id = "Sepia",
        label = "Sepia",
        hexCsv = "e3d2b4",
        primaryBgColor = Color(0xFFE3D2B4),
        accentRingColor = Color(0xFF7A5C43),
        openPeepsExtras = "skinColor=d8b48c,c19a70,a37e58&headContrastColor=4a3526,5f4531,7a5c43&clothingColor=8a6a48,9c7c58,6f5433",
        universalExtras = "backgroundColor=e3d2b4",
        secondarySwatchColor = Color(0xFFC19A70)
    ),
    AvatarColorPresetSpec(
        id = "Greyscale",
        label = "Greyscale",
        hexCsv = "ececee",
        primaryBgColor = Color(0xFFECECEE),
        accentRingColor = Color(0xFF48484A),
        openPeepsExtras = "skinColor=dcdcde,b8b8bc,8e8e93&headContrastColor=2c2c2e,48484a,636366&clothingColor=6e6e73,8e8e93,aeaeb2",
        universalExtras = "backgroundColor=ececee",
        secondarySwatchColor = Color(0xFF8E8E93)
    ),
    AvatarColorPresetSpec(
        id = "Duotone",
        label = "Duotone",
        hexCsv = "dfe3f5",
        primaryBgColor = Color(0xFFDFE3F5),
        accentRingColor = Color(0xFF3D4272),
        openPeepsExtras = "skinColor=9aa2d2&headContrastColor=3d4272&clothingColor=6a71a8",
        universalExtras = "backgroundColor=dfe3f5",
        secondarySwatchColor = Color(0xFF6A71A8)
    ),
    AvatarColorPresetSpec(
        id = "FullCast",
        label = "Full Cast",
        hexCsv = "faf5ee",
        primaryBgColor = Color(0xFFFAF5EE),
        accentRingColor = Color(0xFF365314),
        openPeepsExtras = "accessoriesProbability=50&maskProbability=0",
        universalExtras = "backgroundColor=faf5ee",
        secondarySwatchColor = Color(0xFFD7E8B6)
    ),
    AvatarColorPresetSpec(
        id = "CloseUp",
        label = "Close Up",
        hexCsv = "f2ede4",
        primaryBgColor = Color(0xFFF2EDE4),
        accentRingColor = Color(0xFF365314),
        openPeepsExtras = "maskProbability=0",
        universalExtras = "backgroundColor=f2ede4&scale=120",
        secondarySwatchColor = Color(0xFFFED8C8)
    )
)

val AVATAR_COLOR_PRESETS: List<AvatarColorPresetSpec> = SplitMateAvatarColorPresets

fun findAvatarColorPreset(
    idOrLabel: String?,
    fallbackIdOrLabel: String = "PastelWall"
): AvatarColorPresetSpec {
    val defaultPreset = SplitMateAvatarColorPresets.first { it.id == "PastelWall" }
    val raw = idOrLabel?.trim().orEmpty()
    if (raw.isEmpty()) {
        return if (fallbackIdOrLabel != "PastelWall") findAvatarColorPreset(fallbackIdOrLabel, "PastelWall") else defaultPreset
    }
    val normalized = raw.replace(" ", "").replace("_", "").lowercase(Locale.US)
    if (normalized in setOf("buckwheat", "classic", "terracotta", "periwinkle", "terracottasunset", "periwinkledream")) {
        return defaultPreset
    }
    val matched = SplitMateAvatarColorPresets.find {
        it.id.equals(raw, ignoreCase = true) ||
            it.label.equals(raw, ignoreCase = true) ||
            it.id.lowercase(Locale.US) == normalized
    }
    return matched ?: if (fallbackIdOrLabel != raw && fallbackIdOrLabel.isNotBlank()) {
        SplitMateAvatarColorPresets.find {
            it.id.equals(fallbackIdOrLabel, ignoreCase = true) ||
                it.label.equals(fallbackIdOrLabel, ignoreCase = true)
        } ?: defaultPreset
    } else {
        defaultPreset
    }
}

enum class AvatarGender(val id: String, val label: String) {
    MALE("Male", "Male"),
    FEMALE("Female", "Female"),
    NEUTRAL("Neutral", "Neutral");

    companion object {
        fun fromId(raw: String?): AvatarGender {
            return when (raw?.trim()?.lowercase(Locale.US)) {
                "male", "masculine", "m" -> MALE
                "female", "feminine", "f" -> FEMALE
                else -> NEUTRAL
            }
        }
    }
}

private val KNOWN_MALE_FIRST_NAMES = setOf(
    "akshay", "rohan", "kabir", "rahul", "aarav", "vikram", "aditya", "arjun", "siddharth",
    "karan", "amit", "raj", "dev", "sam", "alex", "john", "michael", "david", "aryan",
    "nikhil", "varun", "manish", "suresh", "ramesh", "ankit", "abhishek", "pranav", "harsh",
    "yash", "vishal", "kunal", "gaurav", "deepak", "sachin", "virat", "dhruv", "ishaan",
    "reyansh", "vivaan", "krishna", "sai", "mohammed", "ali", "omar", "james", "robert",
    "william", "daniel", "matthew", "joseph", "andrew", "ryan", "kevin", "brian", "jason",
    "liam", "noah", "oliver", "lucas", "ethan", "logan", "jay", "neil", "tarun", "ashish",
    "mayank", "tushar", "piyush", "naveen", "prashant", "sandeep", "sumit", "vivek", "alok",
    "anand", "hemant", "kartik", "mohit", "nitin", "pankaj", "rajesh", "ravi", "sanjay",
    "shashank", "shubham", "sourabh", "uday", "vaibhav", "vinay", "yogesh"
)

private val KNOWN_FEMALE_FIRST_NAMES = setOf(
    "priya", "neha", "sneha", "ananya", "gauri", "maya", "pooja", "kavya", "divya",
    "aishwarya", "riya", "kriti", "shreya", "nisha", "aditi", "sarah", "emma", "jessica",
    "anya", "diya", "ishita", "meera", "nandini", "pallavi", "radhika", "sakshi", "tanvi",
    "urvashi", "vidya", "zoya", "simran", "komal", "swati", "anjali", "deepika", "kareena",
    "alia", "kiara", "shraddha", "kritika", "mansi", "nikita", "payal", "rachna", "richa",
    "roshni", "sonali", "sunita", "rekha", "sushma", "archana", "bhavna", "chaitra", "damini",
    "ekta", "fatima", "geeta", "hema", "indira", "janhvi", "kiran", "lakshmi", "madhuri",
    "namrata", "ojaswi", "parul", "prachi", "preeti", "rani", "rashmi", "rupali", "sana",
    "shilpa", "smita", "sonam", "supriya", "tara", "trisha", "uma", "vandana", "varsha",
    "yamini", "olivia", "ava", "sophia", "isabella", "mia", "charlotte", "amelia", "harper",
    "evelyn", "abigail", "emily", "elizabeth", "sofia", "avery", "ella", "scarlett", "grace",
    "chloe", "victoria", "riley", "aria", "lily", "hannah", "layla", "zoe", "nora", "stella",
    "natalie", "lucy", "anna", "samantha", "rachel", "lauren", "ashley", "megan", "amanda",
    "melissa", "stephanie", "rebecca", "laura", "amy", "claire", "elena", "nina", "vera"
)

fun inferGenderFromFirstName(rawName: String): AvatarGender? {
    val firstWord = rawName
        .substringBefore("|")
        .substringBefore("_")
        .trim()
        .split(Regex("\\s+"))
        .firstOrNull()
        ?.filter { it.isLetter() }
        ?.lowercase(Locale.US)
        .orEmpty()
    if (firstWord.isEmpty()) return null
    return when {
        firstWord in KNOWN_FEMALE_FIRST_NAMES -> AvatarGender.FEMALE
        firstWord in KNOWN_MALE_FIRST_NAMES -> AvatarGender.MALE
        else -> null
    }
}

data class ValidatedAvatarDescriptor(
    val seedKey: String,
    val gender: AvatarGender,
    val styleId: String,
    val colorPresetId: String
) {
    fun encode(): String = AvatarSeedCodec.encode(seedKey, gender.id, styleId, colorPresetId)
}

object AvatarSeedCodec {
    private val validStyleIdsLower: Map<String, String> =
        SplitMateDiceBearStyles.associate { it.id.lowercase(Locale.US) to it.id }

    private val genderTokenSet = setOf("male", "female", "neutral", "masculine", "feminine")

    private val presetTokenSet: Set<String> = buildSet {
        SplitMateAvatarColorPresets.forEach {
            add(it.id.lowercase(Locale.US))
            add(it.label.lowercase(Locale.US))
            add(it.label.replace(" ", "").lowercase(Locale.US))
        }
        addAll(listOf("buckwheat", "classic", "terracotta", "periwinkle", "bold_pop", "terracotta_sunset", "periwinkle_dream"))
    }

    fun sanitizeSeedKey(raw: String): String =
        raw.replace("|", " ").trim().ifBlank { "Explorer" }

    private fun resolveStyleId(candidate: String?, fallback: String = "open-peeps"): String {
        val clean = candidate?.trim()?.lowercase(Locale.US).orEmpty()
        return validStyleIdsLower[clean]
            ?: validStyleIdsLower[fallback.trim().lowercase(Locale.US)]
            ?: "open-peeps"
    }

    private fun isGenderToken(token: String): Boolean =
        token.trim().lowercase(Locale.US) in genderTokenSet

    private fun isStyleToken(token: String): Boolean =
        validStyleIdsLower.containsKey(token.trim().lowercase(Locale.US))

    private fun isPresetToken(token: String): Boolean =
        presetTokenSet.contains(token.trim().lowercase(Locale.US)) ||
            presetTokenSet.contains(token.trim().replace(" ", "").lowercase(Locale.US))

    fun encode(
        seedKey: String,
        gender: String = "Neutral",
        styleId: String = "open-peeps",
        colorPresetId: String = "PastelWall"
    ): String {
        val cleanSeed = sanitizeSeedKey(seedKey)
        val cleanGender = AvatarGender.fromId(gender).id
        val cleanStyle = resolveStyleId(styleId)
        val cleanPreset = findAvatarColorPreset(colorPresetId).id
        return "$cleanSeed|$cleanGender|$cleanStyle|$cleanPreset"
    }

    fun encode(
        seedKey: String,
        gender: AvatarGender,
        styleId: String = "open-peeps",
        colorPresetId: String = "PastelWall"
    ): String = encode(seedKey, gender.id, styleId, colorPresetId)

    fun parse(
        rawSeed: String,
        fallbackStyleId: String = "open-peeps",
        fallbackColorPresetId: String = "PastelWall",
        fallbackGender: String = "Neutral"
    ): ValidatedAvatarDescriptor {
        val tokens = rawSeed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        val defaultStyle = resolveStyleId(fallbackStyleId)
        val defaultPreset = findAvatarColorPreset(fallbackColorPresetId).id
        val defaultGender = AvatarGender.fromId(fallbackGender)

        if (tokens.isEmpty()) {
            return ValidatedAvatarDescriptor(
                seedKey = "Explorer",
                gender = defaultGender,
                styleId = defaultStyle,
                colorPresetId = defaultPreset
            )
        }

        val seedKey = sanitizeSeedKey(tokens[0])
        return when {
            tokens.size >= 4 -> {
                val g = if (isGenderToken(tokens[1])) AvatarGender.fromId(tokens[1]) else defaultGender
                val s = resolveStyleId(tokens[2], defaultStyle)
                val p = findAvatarColorPreset(tokens[3], defaultPreset).id
                ValidatedAvatarDescriptor(seedKey, g, s, p)
            }
            tokens.size == 3 -> {
                val t1 = tokens[1]
                val t2 = tokens[2]
                when {
                    isGenderToken(t1) && isStyleToken(t2) -> ValidatedAvatarDescriptor(
                        seedKey = seedKey,
                        gender = AvatarGender.fromId(t1),
                        styleId = resolveStyleId(t2, defaultStyle),
                        colorPresetId = defaultPreset
                    )
                    isGenderToken(t1) && isPresetToken(t2) -> ValidatedAvatarDescriptor(
                        seedKey = seedKey,
                        gender = AvatarGender.fromId(t1),
                        styleId = defaultStyle,
                        colorPresetId = findAvatarColorPreset(t2, defaultPreset).id
                    )
                    else -> ValidatedAvatarDescriptor(
                        seedKey = seedKey,
                        gender = defaultGender,
                        styleId = resolveStyleId(t1, defaultStyle),
                        colorPresetId = findAvatarColorPreset(t2, defaultPreset).id
                    )
                }
            }
            tokens.size == 2 -> {
                val t1 = tokens[1]
                when {
                    isGenderToken(t1) -> ValidatedAvatarDescriptor(
                        seedKey = seedKey,
                        gender = AvatarGender.fromId(t1),
                        styleId = defaultStyle,
                        colorPresetId = defaultPreset
                    )
                    isStyleToken(t1) -> ValidatedAvatarDescriptor(
                        seedKey = seedKey,
                        gender = defaultGender,
                        styleId = resolveStyleId(t1, defaultStyle),
                        colorPresetId = defaultPreset
                    )
                    isPresetToken(t1) -> ValidatedAvatarDescriptor(
                        seedKey = seedKey,
                        gender = defaultGender,
                        styleId = defaultStyle,
                        colorPresetId = findAvatarColorPreset(t1, defaultPreset).id
                    )
                    else -> ValidatedAvatarDescriptor(
                        seedKey = seedKey,
                        gender = defaultGender,
                        styleId = defaultStyle,
                        colorPresetId = defaultPreset
                    )
                }
            }
            else -> ValidatedAvatarDescriptor(
                seedKey = seedKey,
                gender = defaultGender,
                styleId = defaultStyle,
                colorPresetId = defaultPreset
            )
        }
    }

    fun parse(
        rawSeed: String,
        fallbackStyleId: String = "open-peeps",
        fallbackColorPresetId: String = "PastelWall",
        fallbackGender: AvatarGender
    ): ValidatedAvatarDescriptor = parse(rawSeed, fallbackStyleId, fallbackColorPresetId, fallbackGender.id)
}

fun buildDiceBearAvatarUrl(
    name: String,
    phone: String = "",
    styleId: String = "open-peeps",
    colorPresetId: String = "PastelWall",
    flip: Boolean = false,
    gender: String = ""
): String {
    val cleanPhone10 = phone.filter { it.isDigit() }.takeLast(10)
    val effectiveRawSeed = name.trim().ifEmpty {
        if (cleanPhone10.length == 10) "Explorer_$cleanPhone10" else "Explorer"
    }
    val parsed = AvatarSeedCodec.parse(
        rawSeed = effectiveRawSeed,
        fallbackStyleId = styleId,
        fallbackColorPresetId = colorPresetId,
        fallbackGender = gender.ifBlank { "Neutral" }
    )
    val effectiveGender = if (gender.isNotBlank() && effectiveRawSeed.count { it == '|' } < 3) {
        AvatarGender.fromId(gender)
    } else {
        parsed.gender
    }
    val preset = findAvatarColorPreset(parsed.colorPresetId)
    val encodedSeed = URLEncoder.encode(parsed.seedKey, "UTF-8")

    val queryPairs = linkedMapOf<String, String>()
    queryPairs["seed"] = encodedSeed
    queryPairs["radius"] = "50"
    if (flip) {
        queryPairs["flip"] = "true"
    }

    fun appendRawQueryPairs(rawQuery: String) {
        if (rawQuery.isBlank()) return
        rawQuery.split("&").forEach { pair ->
            val key = pair.substringBefore("=").trim()
            val value = pair.substringAfter("=", "").trim()
            if (key.isNotEmpty() && value.isNotEmpty()) {
                queryPairs[key] = value
            }
        }
    }

    // 1. Universal preset query parameters (backgroundColor, backgroundType, backgroundRotation, scale)
    appendRawQueryPairs(preset.universalExtras)

    // 2. Style-specific preset parameters strictly gated to open-peeps
    if (parsed.styleId == "open-peeps") {
        appendRawQueryPairs(preset.openPeepsExtras)
    }

    // 3. Schema-validated style-specific gender & happy expression parameters
    when (parsed.styleId) {
        "open-peeps" -> {
            queryPairs["face"] = "smile,smileBig,smileLOL,smileTeethGap,lovingGrin1,lovingGrin2,cheeky,cute,calm,eatingHappy"
            if (!queryPairs.containsKey("maskProbability")) {
                queryPairs["maskProbability"] = "0"
            }
            when (effectiveGender) {
                AvatarGender.MALE -> {
                    queryPairs["head"] = "short1,short2,short3,short4,short5,pomp,flatTop,twists,dreads1"
                }
                AvatarGender.FEMALE -> {
                    queryPairs["head"] = "long,longBangs,longCurly,bun,bun2,buns,medium1,medium2,medium3,mediumBangs,mediumBangs2,mediumBangs3,mediumStraight"
                    queryPairs["facialHairProbability"] = "0"
                }
                AvatarGender.NEUTRAL -> {
                    queryPairs["head"] = "medium1,medium2,medium3,afro,twists,hatBeanie,hatHip,short1,short2,bun,longCurly"
                    if (!queryPairs.containsKey("facialHairProbability")) {
                        queryPairs["facialHairProbability"] = "0"
                    }
                }
            }
        }
        "adventurer" -> {
            when (effectiveGender) {
                AvatarGender.MALE -> {
                    queryPairs["hair"] = "short01,short02,short03,short04,short05,short06,short07,short08,short09,short10,short11,short12,short13,short14,short15,short16,short17,short18,short19"
                    queryPairs["earringsProbability"] = "0"
                }
                AvatarGender.FEMALE -> {
                    queryPairs["hair"] = "long01,long02,long03,long04,long05,long06,long07,long08,long09,long10,long11,long12,long13,long14,long15,long16,long17,long18,long19,long20,long21,long22,long23,long24,long25,long26"
                }
                AvatarGender.NEUTRAL -> Unit
            }
        }
        "avataaars" -> {
            when (effectiveGender) {
                AvatarGender.MALE -> {
                    queryPairs["top"] = "shortFlat,shortRound,shortWaved,theCaesar,shaggy"
                }
                AvatarGender.FEMALE -> {
                    queryPairs["top"] = "longButNotTooLong,straight01,straight02,curvy,bob,bun,miaWallace"
                    queryPairs["facialHairProbability"] = "0"
                }
                AvatarGender.NEUTRAL -> {
                    queryPairs["facialHairProbability"] = "0"
                }
            }
        }
        "personas" -> {
            when (effectiveGender) {
                AvatarGender.MALE -> {
                    queryPairs["hair"] = "shortCombover,fade,buzzcut,curlyHighTop"
                }
                AvatarGender.FEMALE -> {
                    queryPairs["hair"] = "long,extraLong,bobBangs,bobCut,curlyBun,straightBun,pigtails"
                    queryPairs["facialHairProbability"] = "0"
                }
                AvatarGender.NEUTRAL -> {
                    queryPairs["facialHairProbability"] = "0"
                }
            }
        }
        "micah" -> {
            when (effectiveGender) {
                AvatarGender.MALE -> {
                    queryPairs["hair"] = "fonze,dannyPhantom,dougFunny,mrT"
                    queryPairs["earringsProbability"] = "0"
                }
                AvatarGender.FEMALE -> {
                    queryPairs["hair"] = "full,pixie"
                    queryPairs["facialHairProbability"] = "0"
                }
                AvatarGender.NEUTRAL -> {
                    queryPairs["facialHairProbability"] = "0"
                }
            }
        }
        "toon-head" -> {
            when (effectiveGender) {
                AvatarGender.MALE -> {
                    queryPairs["hair"] = "spiky,undercut,sideComed"
                    queryPairs["rearHairProbability"] = "0"
                }
                AvatarGender.FEMALE -> {
                    queryPairs["hair"] = "bun"
                    queryPairs["rearHairProbability"] = "100"
                    queryPairs["beardProbability"] = "0"
                }
                AvatarGender.NEUTRAL -> {
                    queryPairs["beardProbability"] = "0"
                }
            }
        }
        "dylan" -> {
            if (effectiveGender == AvatarGender.FEMALE) {
                queryPairs["facialHairProbability"] = "0"
            }
        }
        "lorelei" -> {
            if (effectiveGender == AvatarGender.FEMALE) {
                queryPairs["beardProbability"] = "0"
            } else if (effectiveGender == AvatarGender.MALE) {
                queryPairs["earringsProbability"] = "0"
            }
        }
        "notionists", "croodles" -> {
            if (effectiveGender == AvatarGender.FEMALE) {
                queryPairs["beardProbability"] = "0"
            }
        }
    }

    val queryString = queryPairs.entries.joinToString("&") { "${it.key}=${it.value}" }
    return "https://api.dicebear.com/9.x/${parsed.styleId}/svg?$queryString"
}

fun buildDiceBearAvatarUrl(
    name: String,
    phone: String = "",
    styleId: String = "open-peeps",
    colorPresetId: String = "PastelWall",
    flip: Boolean = false,
    gender: AvatarGender
): String = buildDiceBearAvatarUrl(
    name = name,
    phone = phone,
    styleId = styleId,
    colorPresetId = colorPresetId,
    flip = flip,
    gender = gender.id
)

/**
 * Unified DiceBear SVG URL builder that delegates directly to [buildDiceBearAvatarUrl]
 * so every avatar call site across the app renders the exact same 4-token character SVG.
 */
fun buildDiceBearOpenPeepsUrl(rawSeed: String, styleOverride: String? = null): String {
    return buildDiceBearAvatarUrl(
        name = rawSeed,
        gender = styleOverride.orEmpty()
    )
}

fun buildOpenPeepsAvatarSvgUrl(seed: String): String {
    return buildDiceBearAvatarUrl(name = seed)
}

@Composable
fun SplitMateCharacterAvatar(
    name: String,
    phone: String = "",
    size: androidx.compose.ui.unit.Dp = 48.dp,
    styleId: String = "open-peeps",
    colorPresetId: String = "PastelWall",
    highlighted: Boolean = false,
    flip: Boolean = false,
    gender: String = "",
    modifier: Modifier = Modifier
) {
    val parsed = remember(name, styleId, colorPresetId, gender) {
        AvatarSeedCodec.parse(
            rawSeed = name,
            fallbackStyleId = styleId,
            fallbackColorPresetId = colorPresetId,
            fallbackGender = gender.ifBlank { "Neutral" }
        )
    }
    val preset = remember(parsed.colorPresetId) {
        findAvatarColorPreset(parsed.colorPresetId)
    }
    val url = remember(name, phone, styleId, colorPresetId, flip, gender) {
        buildDiceBearAvatarUrl(
            name = name,
            phone = phone,
            styleId = styleId,
            colorPresetId = colorPresetId,
            flip = flip,
            gender = gender
        )
    }
    val initials = remember(parsed.seedKey) {
        extractInitialsFromNameOrSeed(parsed.seedKey)
    }

    val ringColor = if (highlighted) preset.accentRingColor else SplitMateTheme.BorderLight
    val ringWidth = if (highlighted) 3.dp else 1.dp
    // M3 Expressive avatar geometry: Cookie9Sided by default, SoftBurst for the highlighted/active user.
    val avatarShape = remember(highlighted) {
        if (highlighted) {
            com.splitmate.app.ui.components.RoundedPolygonShape(com.splitmate.app.ui.components.MaterialShapes.SoftBurst)
        } else {
            com.splitmate.app.ui.components.RoundedPolygonShape(com.splitmate.app.ui.components.MaterialShapes.Cookie9Sided)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(avatarShape)
            .background(preset.primaryBgColor)
            .border(ringWidth, ringColor, avatarShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontFamily = FigtreeFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.34f).sp,
            color = preset.accentRingColor.copy(alpha = 0.78f)
        )
        coil.compose.AsyncImage(
            model = coil.request.ImageRequest.Builder(LocalContext.current)
                .data(url)
                .decoderFactory(coil.decode.SvgDecoder.Factory())
                .diskCacheKey("dicebear_avatar_$url")
                .memoryCacheKey("dicebear_avatar_$url")
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .crossfade(true)
                .build(),
            contentDescription = "Avatar for ${parsed.seedKey}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(avatarShape)
        )
    }
}

@Composable
fun SplitMateCharacterAvatar(
    name: String,
    phone: String = "",
    size: androidx.compose.ui.unit.Dp = 48.dp,
    styleId: String = "open-peeps",
    colorPresetId: String = "PastelWall",
    gender: AvatarGender,
    highlighted: Boolean = false,
    flip: Boolean = false,
    modifier: Modifier = Modifier
) {
    SplitMateCharacterAvatar(
        name = name,
        phone = phone,
        size = size,
        styleId = styleId,
        colorPresetId = colorPresetId,
        highlighted = highlighted,
        flip = flip,
        gender = gender.id,
        modifier = modifier
    )
}

data class OpenPeepsCrewSceneSpec(
    val id: String,
    val title: String,
    val assetPath: String
)

val SplitMateCrewScenes: List<OpenPeepsCrewSceneSpec> = listOf(
    OpenPeepsCrewSceneSpec(
        id = "walk_crew",
        title = "The Travel Walkers",
        assetPath = "file:///android_asset/peeps/scenes/scene_walk_crew.svg"
    ),
    OpenPeepsCrewSceneSpec(
        id = "bike_trip",
        title = "The Bike Trip Crew",
        assetPath = "file:///android_asset/peeps/scenes/scene_bike_trip.svg"
    ),
    OpenPeepsCrewSceneSpec(
        id = "coffee_hangout",
        title = "The Coffee & Hoodie Crew",
        assetPath = "file:///android_asset/peeps/scenes/scene_coffee_hangout.svg"
    ),
    OpenPeepsCrewSceneSpec(
        id = "weekend_squad",
        title = "The Weekend Hangout",
        assetPath = "file:///android_asset/peeps/scenes/scene_weekend_squad.svg"
    ),
    OpenPeepsCrewSceneSpec(
        id = "roadtrip_busters",
        title = "The Roadtrip Squad",
        assetPath = "file:///android_asset/peeps/scenes/scene_roadtrip_busters.svg"
    )
)

@Composable
fun OpenPeepsHeroStage(
    @Suppress("UNUSED_PARAMETER") name: String = "",
    @Suppress("UNUSED_PARAMETER") phone: String = "",
    @Suppress("UNUSED_PARAMETER") selectedStyleId: String = "open-peeps",
    @Suppress("UNUSED_PARAMETER") selectedColorPresetId: String = "PastelWall",
    isCompactMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localView = androidx.compose.ui.platform.LocalView.current
    var activeSceneIndex by rememberSaveable { mutableIntStateOf(0) }
    val activeScene = SplitMateCrewScenes[activeSceneIndex.coerceIn(0, SplitMateCrewScenes.lastIndex)]

    val stageHeight by animateDpAsState(
        targetValue = if (isCompactMode) 84.dp else 184.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "hero_stage_height"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "hero_scene_breathing")
    val floatOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scene_float_y"
    )

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFFF4EFE6),
        border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
        modifier = modifier
            .fillMaxWidth()
            .height(stageHeight)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Layer 1: Warm Buckwheat organic backdrop blobs
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                drawOval(
                    color = Color(0xFFEDE7DF),
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.84f),
                    size = androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.14f)
                )

                val pathSage = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.14f, h * 0.86f)
                    quadraticBezierTo(w * 0.06f, h * 0.35f, w * 0.28f, h * 0.18f)
                    quadraticBezierTo(w * 0.48f, h * 0.08f, w * 0.44f, h * 0.86f)
                    close()
                }
                drawPath(pathSage, Color(0xFFD7E8B6).copy(alpha = 0.55f))

                val pathPeach = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.52f, h * 0.88f)
                    quadraticBezierTo(w * 0.82f, h * 0.24f, w * 0.72f, h * 0.14f)
                    quadraticBezierTo(w * 0.92f, h * 0.48f, w * 0.86f, h * 0.86f)
                    close()
                }
                drawPath(pathPeach, Color(0xFFFED8C8).copy(alpha = 0.55f))

                drawCircle(
                    color = Color(0xFFDCE3FD).copy(alpha = 0.48f),
                    radius = w * 0.16f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.48f)
                )
            }

            // Layer 2: Unobstructed Multi-Person Open Peeps Group Scene with M3 Spatial Spring Transition
            AnimatedContent(
                targetState = activeScene,
                transitionSpec = {
                    (fadeIn(animationSpec = com.splitmate.app.ui.components.SplitMateMotion.defaultEffects()) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )).togetherWith(
                        fadeOut(animationSpec = com.splitmate.app.ui.components.SplitMateMotion.fastEffects()) +
                            scaleOut(targetScale = 0.95f)
                    )
                },
                label = "crew_scene_transition",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = if (isCompactMode) 4.dp else 8.dp)
            ) { scene ->
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(scene.assetPath)
                        .decoderFactory(coil.decode.SvgDecoder.Factory())
                        .diskCacheKey("crew_scene_${scene.id}")
                        .memoryCacheKey("crew_scene_${scene.id}")
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .crossfade(true)
                        .build(),
                    contentDescription = scene.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = floatOffsetY }
                )
            }

            // Layer 3: Top-Right M3 Expressive "Shuffle Crew" Pill (Zero Emojis)
            Surface(
                onClick = {
                    performCrispTactileHaptic(context, localView, heavy = false)
                    activeSceneIndex = (activeSceneIndex + 1) % SplitMateCrewScenes.size
                },
                shape = RoundedCornerShape(50),
                color = Color(0xFFFAF6F0).copy(alpha = 0.94f),
                border = BorderStroke(1.dp, Color(0xFF365314).copy(alpha = 0.28f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle crew scene",
                        tint = Color(0xFF365314),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Shuffle Crew",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF23201E)
                    )
                    Text(
                        text = "${activeSceneIndex + 1}/${SplitMateCrewScenes.size}",
                        style = TextStyle(
                            fontFamily = SplitMateTnumMonospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = Color(0xFF365314)
                    )
                }
            }
        }
    }
}

/**
 * 8 Expressive Material 3 Category Icons for Group Creation & Group Cards.
 */
data class GroupCategoryIconOption(
    val id: String,
    val label: String,
    val icon: ImageVector
) {
    val key: String get() = id
}

val GroupCategoryIcons: List<GroupCategoryIconOption> = listOf(
    GroupCategoryIconOption("Flight", "Trip", Icons.Rounded.Flight),
    GroupCategoryIconOption("Cabin", "Cabin", Icons.Rounded.Cabin),
    GroupCategoryIconOption("Home", "Home", Icons.Rounded.Home),
    GroupCategoryIconOption("Restaurant", "Dining", Icons.Rounded.Restaurant),
    GroupCategoryIconOption("LocalCafe", "Coffee", Icons.Rounded.LocalCafe),
    GroupCategoryIconOption("LocalBar", "Party", Icons.Rounded.LocalBar),
    GroupCategoryIconOption("ShoppingCart", "Groceries", Icons.Rounded.ShoppingCart),
    GroupCategoryIconOption("Celebration", "Event", Icons.Rounded.Celebration)
)

fun resolveGroupCategoryIcon(iconName: String, fallbackGroupName: String = ""): ImageVector {
    val matched = GroupCategoryIcons.find { it.id.equals(iconName, ignoreCase = true) }
    if (matched != null) return matched.icon
    return when {
        fallbackGroupName.contains("Cabin", ignoreCase = true) -> Icons.Rounded.Cabin
        fallbackGroupName.contains("Apt", ignoreCase = true) ||
            fallbackGroupName.contains("Room", ignoreCase = true) ||
            fallbackGroupName.contains("Home", ignoreCase = true) -> Icons.Rounded.Home
        fallbackGroupName.contains("Cafe", ignoreCase = true) ||
            fallbackGroupName.contains("Coffee", ignoreCase = true) -> Icons.Rounded.LocalCafe
        fallbackGroupName.contains("Party", ignoreCase = true) ||
            fallbackGroupName.contains("Drinks", ignoreCase = true) -> Icons.Rounded.LocalBar
        fallbackGroupName.contains("Grocer", ignoreCase = true) -> Icons.Rounded.ShoppingCart
        fallbackGroupName.contains("Event", ignoreCase = true) -> Icons.Rounded.Celebration
        fallbackGroupName.contains("Trip", ignoreCase = true) ||
            fallbackGroupName.contains("Goa", ignoreCase = true) -> Icons.Rounded.Flight
        else -> Icons.Rounded.Restaurant
    }
}

// ==============================================================================
// IN-APP DEVICE CONTACT MODEL, PHONE CLEANER & MULTI-SELECT MODAL BOTTOM SHEET
// ==============================================================================

data class DeviceContact(
    val name: String,
    val cleanPhone: String,
    val formattedPhone: String
)

fun cleanIndianTenDigitPhone(rawNumber: String): String {
    val trimmed = rawNumber.trim()
    val digitsOnly = trimmed.replace(Regex("[^0-9]"), "")
    if (trimmed.startsWith("+") && !digitsOnly.startsWith("91") && digitsOnly.length in 10..15) {
        return "+$digitsOnly"
    }
    return when {
        digitsOnly.length >= 12 && digitsOnly.startsWith("91") -> digitsOnly.substring(2).takeLast(10)
        digitsOnly.length == 11 && digitsOnly.startsWith("0") -> digitsOnly.substring(1)
        digitsOnly.length > 10 -> digitsOnly.takeLast(10)
        else -> digitsOnly
    }
}

fun normalizeValidatedPhone10(raw: String): String {
    val clean = cleanIndianTenDigitPhone(raw)
    return if (clean.length < 10) "" else clean.takeLast(10)
}

fun formatTenDigitIndianPhone(cleanPhone: String): String {
    return when {
        cleanPhone.startsWith("+") -> cleanPhone
        cleanPhone.length == 10 -> "+91 ${cleanPhone.substring(0, 5)} ${cleanPhone.substring(5)}"
        else -> cleanPhone
    }
}

fun queryAllDeviceContacts(context: Context): List<DeviceContact> {
    val contactsMap = linkedMapOf<String, DeviceContact>()
    val seenNames = HashSet<String>()
    try {
        // 1. Query all Phone rows across all synced accounts (Google, Device, SIM, WhatsApp, Exchange) without 500-row cap
        val phoneProjection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            phoneProjection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val primaryNameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
            val altNameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val primaryName = if (primaryNameIdx >= 0) cursor.getString(primaryNameIdx).orEmpty().trim() else ""
                val altName = if (altNameIdx >= 0) cursor.getString(altNameIdx).orEmpty().trim() else ""
                val rawName = primaryName.ifBlank { altName }
                val rawNumber = if (numberIdx >= 0) cursor.getString(numberIdx).orEmpty().trim() else ""
                val cleanPhone = cleanIndianTenDigitPhone(rawNumber).ifBlank {
                    rawNumber.filter { it.isDigit() }
                }
                if (rawName.isNotEmpty() && cleanPhone.isNotEmpty()) {
                    val dedupKey = "${rawName.lowercase()}|$cleanPhone"
                    seenNames.add(rawName.lowercase())
                    if (!contactsMap.containsKey(dedupKey)) {
                        contactsMap[dedupKey] = DeviceContact(
                            name = rawName,
                            cleanPhone = cleanPhone,
                            formattedPhone = formatTenDigitIndianPhone(cleanPhone)
                        )
                    }
                }
            }
        }

        // 2. Also include any Contacts from ContactsContract.Contacts.CONTENT_URI that had no phone row above
        val contactsProjection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.DISPLAY_NAME
        )
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            contactsProjection,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC"
        )?.use { cCursor ->
            val idIdx = cCursor.getColumnIndex(ContactsContract.Contacts._ID)
            val pNameIdx = cCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val dNameIdx = cCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            while (cCursor.moveToNext()) {
                val cid = if (idIdx >= 0) cCursor.getString(idIdx).orEmpty().trim() else ""
                val pName = if (pNameIdx >= 0) cCursor.getString(pNameIdx).orEmpty().trim() else ""
                val dName = if (dNameIdx >= 0) cCursor.getString(dNameIdx).orEmpty().trim() else ""
                val rawName = pName.ifBlank { dName }
                if (rawName.isNotEmpty() && rawName.lowercase() !in seenNames) {
                    seenNames.add(rawName.lowercase())
                    val syntheticPhoneKey = "contact_$cid"
                    contactsMap["${rawName.lowercase()}|$syntheticPhoneKey"] = DeviceContact(
                        name = rawName,
                        cleanPhone = syntheticPhoneKey,
                        formattedPhone = "No phone number · Tap to add +91 mobile"
                    )
                }
            }
        }
    } catch (_: SecurityException) {
    } catch (_: Exception) {
    }
    return contactsMap.values.sortedBy { it.name.lowercase() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerBottomSheet(
    contacts: List<DeviceContact>,
    isLoading: Boolean = false,
    multiSelect: Boolean = true,
    preSelectedPhones: Set<String> = emptySet(),
    initialSelectedPhones: Set<String> = preSelectedPhones,
    title: String = "Add Members from Contacts",
    subtitle: String = "Select friends from your phonebook or add +91 mobile",
    onDismissRequest: () -> Unit = {},
    onDismiss: () -> Unit = onDismissRequest,
    onConfirmSelected: (List<DeviceContact>) -> Unit = {},
    onConfirmSelection: (List<DeviceContact>) -> Unit = onConfirmSelected
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedPhones by remember(initialSelectedPhones) {
        mutableStateOf(initialSelectedPhones.toSet())
    }
    var customAddedContacts by remember { mutableStateOf(listOf<DeviceContact>()) }
    var manualNameInput by remember { mutableStateOf("") }
    var manualPhoneInput by remember { mutableStateOf("") }
    var promptingPhonelessContact by remember { mutableStateOf<DeviceContact?>(null) }

    val allContacts = remember(contacts, customAddedContacts) {
        customAddedContacts + contacts
    }

    val filteredContacts = remember(allContacts, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            allContacts
        } else {
            allContacts.filter {
                it.name.lowercase().contains(q) || it.cleanPhone.contains(q)
            }
        }
    }

    // Auto-populate manual fields when user types a 10-digit phone or name with no matching contacts
    LaunchedEffect(searchQuery, filteredContacts.size) {
        if (filteredContacts.isEmpty() && searchQuery.isNotBlank()) {
            val digits = com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(searchQuery)
            val letters = searchQuery.replace(Regex("[0-9+\\-()\\s]"), " ").trim()
            if (digits.isNotEmpty() && manualPhoneInput.isBlank()) {
                manualPhoneInput = digits
            }
            if (letters.isNotEmpty() && manualNameInput.isBlank()) {
                manualNameInput = letters
            }
        }
    }

    val manualPhone10 = remember(manualPhoneInput) {
        com.splitmate.app.data.PhoneIdentityValidator.normalizeIndianPhone10(manualPhoneInput)
    }
    val isManualEntryValid = remember(manualNameInput, manualPhone10) {
        manualNameInput.trim().isNotBlank() &&
            com.splitmate.app.data.PhoneIdentityValidator.isValidIndianMobile10(manualPhone10)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SplitMateTheme.ScreenBg,
        contentColor = SplitMateTheme.PrimaryDark,
        scrimColor = Color.Black.copy(alpha = 0.72f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Surface(
            color = SplitMateTheme.ScreenBg,
            contentColor = SplitMateTheme.PrimaryDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SplitMateTheme.ScreenBg)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = SplitMateTheme.PrimaryDark
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SplitMateTheme.TextSecondary
                        )
                    }
                    if (multiSelect && allContacts.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                val selectablePhones = filteredContacts
                                    .filterNot { it.cleanPhone.startsWith("contact_") }
                                    .map { it.cleanPhone }
                                selectedPhones = if (selectablePhones.isNotEmpty() && selectedPhones.containsAll(selectablePhones)) {
                                    emptySet()
                                } else {
                                    selectedPhones + selectablePhones
                                }
                            }
                        ) {
                            Text(
                                text = if (selectedPhones.size == filteredContacts.size && filteredContacts.isNotEmpty()) {
                                    "Clear"
                                } else {
                                    "Select All"
                                },
                                fontWeight = FontWeight.Bold,
                                color = SplitMateTheme.SageText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search by name or 10-digit phone...",
                            color = SplitMateTheme.TextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search contacts",
                            tint = SplitMateTheme.TextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    tint = SplitMateTheme.TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SplitMateTheme.PrimaryDark,
                        unfocusedTextColor = SplitMateTheme.PrimaryDark,
                        focusedBorderColor = SplitMateTheme.PrimaryDark,
                        unfocusedBorderColor = SplitMateTheme.BorderLight,
                        focusedContainerColor = SplitMateTheme.SurfaceWhite,
                        unfocusedContainerColor = SplitMateTheme.SurfaceWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Inline Buckwheat "Add by Mobile Number (+91)" Card (F9 / RCA-2B / RCA-2C)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (SplitMateTheme.isDark) Color(0xFF24201C) else Color(0xFFF7F3EC),
                    border = BorderStroke(
                        width = if (promptingPhonelessContact != null) 1.5.dp else 1.dp,
                        color = if (promptingPhonelessContact != null) Color(0xFFE06B52) else Color(0xFFEDE7DF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (promptingPhonelessContact != null) {
                                    "Enter 10-digit mobile for ${promptingPhonelessContact?.name} to send cloud invite"
                                } else {
                                    "Add by Mobile Number (+91)"
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (promptingPhonelessContact != null) Color(0xFF7C2D12) else SplitMateTheme.PrimaryDark,
                                modifier = Modifier.weight(1f)
                            )
                            if (promptingPhonelessContact != null) {
                                TextButton(
                                    onClick = {
                                        val offlineContact = promptingPhonelessContact
                                        if (offlineContact != null) {
                                            selectedPhones = if (multiSelect) {
                                                selectedPhones + offlineContact.cleanPhone
                                            } else {
                                                setOf(offlineContact.cleanPhone)
                                            }
                                        }
                                        promptingPhonelessContact = null
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Add Offline Without Number",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF7C2D12)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualNameInput,
                                onValueChange = { manualNameInput = it },
                                placeholder = {
                                    Text("Friend's Name", fontSize = 12.sp, color = SplitMateTheme.TextSecondary)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = TextStyle(
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = SplitMateTheme.PrimaryDark
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = SplitMateTheme.PrimaryDark,
                                    unfocusedTextColor = SplitMateTheme.PrimaryDark,
                                    focusedBorderColor = BuckwheatOlivePrimary,
                                    unfocusedBorderColor = SplitMateTheme.BorderLight,
                                    focusedContainerColor = SplitMateTheme.SurfaceWhite,
                                    unfocusedContainerColor = SplitMateTheme.SurfaceWhite
                                ),
                                modifier = Modifier.weight(0.95f)
                            )

                            OutlinedTextField(
                                value = manualPhoneInput,
                                onValueChange = { raw ->
                                    manualPhoneInput = raw.filter { it.isDigit() }.takeLast(10)
                                },
                                prefix = {
                                    Text(
                                        text = "+91 ",
                                        style = TextStyle(
                                            fontFamily = SplitMateTnumMonospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = BuckwheatOlivePrimary,
                                            fontFeatureSettings = "tnum"
                                        )
                                    )
                                },
                                placeholder = {
                                    Text("9876543210", fontSize = 12.sp, color = SplitMateTheme.TextSecondary)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = TextStyle(
                                    fontFamily = SplitMateTnumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SplitMateTheme.PrimaryDark,
                                    fontFeatureSettings = "tnum"
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = SplitMateTheme.PrimaryDark,
                                    unfocusedTextColor = SplitMateTheme.PrimaryDark,
                                    focusedBorderColor = BuckwheatOlivePrimary,
                                    unfocusedBorderColor = SplitMateTheme.BorderLight,
                                    focusedContainerColor = SplitMateTheme.SurfaceWhite,
                                    unfocusedContainerColor = SplitMateTheme.SurfaceWhite
                                ),
                                modifier = Modifier.weight(1.05f)
                            )
                        }

                        Button(
                            onClick = {
                                val resolvedName = manualNameInput.trim().ifBlank { "Friend (${manualPhone10.takeLast(4)})" }
                                val newContact = DeviceContact(
                                    name = resolvedName,
                                    cleanPhone = manualPhone10,
                                    formattedPhone = formatTenDigitIndianPhone(manualPhone10)
                                )
                                customAddedContacts = (listOf(newContact) + customAddedContacts)
                                    .distinctBy { "${it.name.lowercase()}|${it.cleanPhone}" }
                                val nextSelectedPhones = if (multiSelect) {
                                    selectedPhones + manualPhone10
                                } else {
                                    setOf(manualPhone10)
                                }
                                selectedPhones = nextSelectedPhones
                                promptingPhonelessContact = null
                                manualNameInput = ""
                                manualPhoneInput = ""
                                val combinedSelected = (listOf(newContact) + allContacts.filter { nextSelectedPhones.contains(it.cleanPhone) })
                                    .distinctBy { "${it.name.lowercase()}|${it.cleanPhone}" }
                                onConfirmSelection(combinedSelected)
                                onDismiss()
                            },
                            enabled = isManualEntryValid,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BuckwheatOlivePrimary,
                                contentColor = Color(0xFFFAF6F0),
                                disabledContainerColor = SplitMateTheme.BorderLight,
                                disabledContentColor = SplitMateTheme.TextSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+ Add & Send Invite",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ExpressiveMorphingLoader(
                            containerColor = SplitMateTheme.SageSurface,
                            indicatorColor = SplitMateTheme.PrimaryDark
                        )
                    }
                } else if (filteredContacts.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = SplitMateTheme.SurfaceWhite,
                        border = BorderStroke(1.dp, SplitMateTheme.BorderLight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Contacts,
                                contentDescription = null,
                                tint = SplitMateTheme.TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = if (allContacts.isEmpty()) {
                                    "No device contacts found — enter Name & +91 mobile number above"
                                } else {
                                    "No contacts matching \"$searchQuery\" — use + Add & Send Invite above"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SplitMateTheme.TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp, max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            filteredContacts,
                            key = { idx, c -> "${c.name}|${c.cleanPhone}|$idx" }
                        ) { _, contact ->
                            val isPhoneless = contact.cleanPhone.startsWith("contact_")
                            val isChecked = selectedPhones.contains(contact.cleanPhone)
                            val toggleSelection = {
                                if (isPhoneless && !isChecked) {
                                    promptingPhonelessContact = contact
                                    manualNameInput = contact.name
                                } else {
                                    selectedPhones = if (multiSelect) {
                                        if (isChecked) selectedPhones - contact.cleanPhone else selectedPhones + contact.cleanPhone
                                    } else {
                                        setOf(contact.cleanPhone)
                                    }
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { toggleSelection() },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isChecked) {
                                    SplitMateTheme.SageSurface
                                } else {
                                    SplitMateTheme.SurfaceWhite
                                },
                                border = BorderStroke(
                                    width = if (isChecked) 1.5.dp else 1.dp,
                                    color = if (isChecked) BuckwheatOlivePrimary else SplitMateTheme.BorderLight
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                           modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isChecked) BuckwheatSageContainer else SplitMateTheme.SurfaceMuted
                                                )
                                                .border(1.dp, SplitMateTheme.BorderLight, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = extractInitialsFromNameOrSeed(contact.name),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = if (isChecked) BuckwheatOlivePrimary else SplitMateTheme.PrimaryDark
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = contact.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = SplitMateTheme.PrimaryDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = contact.formattedPhone,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isPhoneless) Color(0xFF7C2D12) else SplitMateTheme.TextSecondary
                                            )
                                        }
                                    }
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { toggleSelection() },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = BuckwheatOlivePrimary,
                                            checkmarkColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val selectedCount = selectedPhones.size
                Button(
                    onClick = {
                        val selectedList = allContacts.filter { selectedPhones.contains(it.cleanPhone) }
                        if (selectedList.isNotEmpty()) {
                            onConfirmSelection(selectedList)
                        }
                        onDismiss()
                    },
                    enabled = selectedCount > 0,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SplitMateTheme.PrimaryDark,
                        contentColor = SplitMateTheme.ScreenBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = if (multiSelect) {
                            "Add Selected ($selectedCount)"
                        } else {
                            "Link Selected Contact"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = SplitMateTheme.ScreenBg
                    )
                }
            }
        }
    }
}

@Composable
fun ExpressiveMorphingLoader(
    modifier: Modifier = Modifier,
    containerSize: Dp = 48.dp,
    containerColor: Color = SplitMateTheme.SageSurface,
    indicatorColor: Color = SplitMateTheme.PrimaryDark
) {
    ContainedLoadingIndicator(
        modifier = modifier,
        containerSize = containerSize,
        containerColor = containerColor,
        indicatorColor = indicatorColor
    )
}

@Composable
fun SplitMateMaterial3ExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: SplitMateThemeMode = if (darkTheme) SplitMateThemeMode.WARM_ESPRESSO_NIGHT else SplitMateThemeMode.SUNLIT_BUCKWHEAT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SplitMateExpressiveTheme(
        darkTheme = darkTheme,
        themeMode = themeMode,
        dynamicColor = dynamicColor,
        content = content
    )
}

@Composable
fun SplitMateExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: SplitMateThemeMode = if (darkTheme) SplitMateThemeMode.WARM_ESPRESSO_NIGHT else SplitMateThemeMode.SUNLIT_BUCKWHEAT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val effectiveMode = when {
        themeMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU && !darkTheme -> SplitMateThemeMode.KYOTO_MATCHA_YUZU
        darkTheme || themeMode == SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> SplitMateThemeMode.WARM_ESPRESSO_NIGHT
        else -> SplitMateThemeMode.SUNLIT_BUCKWHEAT
    }

    SplitMateTheme.isDark = effectiveMode.isDark
    SplitMateThemeState.activeThemeMode = effectiveMode
    DesignSystemBindings.activeThemeMode = effectiveMode

    val targetPalette = effectiveMode.toPalette()

    val animSurfaceLowest by animateColorAsState(
        targetValue = targetPalette.surfaceContainerLowest,
        animationSpec = SplitMateMotion.slowEffects(),
        label = "ThemeSurfaceLowest"
    )
    val animSurfaceLow by animateColorAsState(
        targetValue = targetPalette.surfaceContainerLow,
        animationSpec = SplitMateMotion.slowEffects(),
        label = "ThemeSurfaceLow"
    )
    val animSurfaceContainer by animateColorAsState(
        targetValue = targetPalette.surfaceContainer,
        animationSpec = SplitMateMotion.slowEffects(),
        label = "ThemeSurfaceContainer"
    )
    val animSurfaceHigh by animateColorAsState(
        targetValue = targetPalette.surfaceContainerHigh,
        animationSpec = SplitMateMotion.slowEffects(),
        label = "ThemeSurfaceHigh"
    )
    val animSurfaceHighest by animateColorAsState(
        targetValue = targetPalette.surfaceContainerHighest,
        animationSpec = SplitMateMotion.slowEffects(),
        label = "ThemeSurfaceHighest"
    )
    val animOnSurface by animateColorAsState(
        targetValue = targetPalette.onSurface,
        animationSpec = SplitMateMotion.slowEffects(),
        label = "ThemeOnSurface"
    )
    val animOnSurfaceVariant by animateColorAsState(
        targetValue = targetPalette.onSurfaceVariant,
        animationSpec = SplitMateMotion.slowEffects(),
        label = "ThemeOnSurfaceVariant"
    )
    val animPrimary by animateColorAsState(
        targetValue = targetPalette.primary,
        animationSpec = SplitMateMotion.slowEffects(),
        label = "ThemePrimary"
    )
    val animPrimaryContainer by animateColorAsState(
        targetValue = targetPalette.primaryContainer,
        animationSpec = SplitMateMotion.slowEffects(),
        label = "ThemePrimaryContainer"
    )
    val animSecondary by animateColorAsState(
        targetValue = targetPalette.secondary,
        animationSpec = SplitMateMotion.slowEffects(),
        label = "ThemeSecondary"
    )
    val animSecondaryContainer by animateColorAsState(
        targetValue = targetPalette.secondaryContainer,
        animationSpec = SplitMateMotion.slowEffects(),
        label = "ThemeSecondaryContainer"
    )
    val animTertiary by animateColorAsState(
        targetValue = targetPalette.tertiary,
        animationSpec = SplitMateMotion.slowEffects(),
        label = "ThemeTertiary"
    )
    val animTertiaryContainer by animateColorAsState(
        targetValue = targetPalette.tertiaryContainer,
        animationSpec = SplitMateMotion.slowEffects(),
        label = "ThemeTertiaryContainer"
    )
    val animOutline by animateColorAsState(
        targetValue = targetPalette.outline,
        animationSpec = SplitMateMotion.slowEffects(),
        label = "ThemeOutline"
    )

    val animatedPalette = targetPalette.copy(
        surfaceContainerLowest = animSurfaceLowest,
        surfaceContainerLow = animSurfaceLow,
        surfaceContainer = animSurfaceContainer,
        surfaceContainerHigh = animSurfaceHigh,
        surfaceContainerHighest = animSurfaceHighest,
        onSurface = animOnSurface,
        onSurfaceVariant = animOnSurfaceVariant,
        primary = animPrimary,
        primaryContainer = animPrimaryContainer,
        secondary = animSecondary,
        secondaryContainer = animSecondaryContainer,
        tertiary = animTertiary,
        tertiaryContainer = animTertiaryContainer,
        outline = animOutline
    )

    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (effectiveMode.isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        effectiveMode == SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> SplitMateDarkColorScheme
        effectiveMode == SplitMateThemeMode.KYOTO_MATCHA_YUZU -> SplitMateKyotoMatchaColorScheme
        else -> SplitMateLightColorScheme
    }

    // v2.3.4 (Decision #6): map every surfaceContainer role to the (animated) Buckwheat ladder.
    val surfaceRoles = animatedPalette.surfaceContainerRoles()

    val colorScheme = baseScheme.copy(
        primary = animPrimary,
        onPrimary = targetPalette.onPrimary,
        primaryContainer = animPrimaryContainer,
        onPrimaryContainer = targetPalette.onPrimaryContainer,
        secondary = animSecondary,
        onSecondary = targetPalette.onSecondary,
        secondaryContainer = animSecondaryContainer,
        onSecondaryContainer = targetPalette.onSecondaryContainer,
        tertiary = animTertiary,
        onTertiary = targetPalette.onTertiary,
        tertiaryContainer = animTertiaryContainer,
        onTertiaryContainer = targetPalette.onTertiaryContainer,
        background = animSurfaceLow,
        onBackground = animOnSurface,
        surface = animSurfaceLowest,
        onSurface = animOnSurface,
        surfaceVariant = animSurfaceContainer,
        onSurfaceVariant = animOnSurfaceVariant,
        outline = animOutline,
        outlineVariant = targetPalette.outlineVariant,
        surfaceBright = surfaceRoles.bright,
        surfaceDim = surfaceRoles.dim,
        surfaceContainerLowest = surfaceRoles.lowest,
        surfaceContainerLow = surfaceRoles.low,
        surfaceContainer = surfaceRoles.container,
        surfaceContainerHigh = surfaceRoles.high,
        surfaceContainerHighest = surfaceRoles.highest
    )

    // v2.3.4: MotionScheme shim + reduced-motion flag (ANIMATOR_DURATION_SCALE == 0 => snap).
    val reducedMotion = rememberReducedMotionEnabled()
    val motionScheme = if (reducedMotion) SplitMateMotionScheme.reduced() else SplitMateMotionScheme.expressive()

    CompositionLocalProvider(
        LocalSplitMatePalette provides animatedPalette,
        LocalMotionScheme provides motionScheme,
        LocalReducedMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SplitMateTypography
        ) {
            ProvideTextStyle(
                value = TextStyle(fontFamily = FigtreeFontFamily),
                content = content
            )
        }
    }
}

val SplitMateBrandFontFamily: FontFamily = FigtreeFontFamily
val SplitMateDisplayFontFamily: FontFamily = FigtreeFontFamily

// ==============================================================================
// EXPENSE CATEGORY ICON & COLOR RESOLVER (Tab 1 Recent Activity, Group View & Tab 4 Audit)
// ==============================================================================
fun isFlightTicketExpense(
    title: String,
    parsedTicket: ParsedTravelTicket? = extractTravelTicketFromTitle(title)
): Boolean {
    val cleanPnr = parsedTicket?.pnr?.trim().orEmpty()
    if ((cleanPnr.length == 10 && cleanPnr.all { it.isDigit() }) ||
        Regex("""\b\d{10}\b""").containsMatchIn(title) ||
        Regex("""^\d{5}\b""").containsMatchIn(parsedTicket?.trainOrFlightNo?.trim().orEmpty())
    ) {
        return false
    }
    if (cleanPnr.length == 6 && cleanPnr.all { it.isLetterOrDigit() } && cleanPnr.any { it.isLetter() }) {
        return true
    }
    val hasExplicitAirlineOrFlightNo = Regex("""\b(indigo|air india|akasa|spicejet|vistara|airasia|alliance air|star air|fly91|emirates|qatar|lufthansa)\b""", RegexOption.IGNORE_CASE).containsMatchIn(title) ||
        Regex("""\b(6e|ai|ix|qp|sg|uk|i5|9i|s5)[\s\-]?\d{2,4}\b""", RegexOption.IGNORE_CASE).containsMatchIn(title)
    if (hasExplicitAirlineOrFlightNo) {
        return true
    }
    val sanitizedForTrain = title.replace("Air India Express", "Air India", ignoreCase = true)
    if (Regex("""\b(train|irctc|express|rajdhani|shatabdi|vande|duronto|sleeper|berth|3a|2a|1a|3e)\b""", RegexOption.IGNORE_CASE).containsMatchIn(sanitizedForTrain)) {
        return false
    }
    val lower = title.replace(Regex("""train/flight|flight/train""", RegexOption.IGNORE_CASE), "train").lowercase()
    return lower.contains("flight") || lower.contains("airfare") || lower.contains("airport")
}

fun resolveExpenseCategoryIcon(title: String): ImageVector {
    // v2.3.4: new built-in / user-created categories match by exact title first.
    // Legacy preset titles never match here, so existing expenses keep their icons.
    com.splitmate.app.ui.category.exactExpenseCategoryFor(title)?.let {
        return com.splitmate.app.ui.category.ExpenseCategoryIcons.forKey(it.iconKey)
    }
    val lower = title.lowercase()
    return when {
        isFlightTicketExpense(title) -> Icons.Rounded.FlightTakeoff
        lower.contains("train") || lower.contains("pnr") || lower.contains("irctc") ||
            lower.contains("express") || lower.contains("rajdhani") || lower.contains("shatabdi") ||
            lower.contains("vande") || lower.contains("rail") || lower.contains("berth") -> Icons.Rounded.Train
        lower.contains("cab") || lower.contains("auto") || lower.contains("uber") ||
            lower.contains("ola") || lower.contains("rapido") || lower.contains("taxi") ||
            lower.contains("fuel") || lower.contains("petrol") || lower.contains("toll") -> Icons.Rounded.LocalTaxi
        lower.contains("hotel") || lower.contains("stay") || lower.contains("resort") ||
            lower.contains("villa") || lower.contains("airbnb") || lower.contains("hostel") ||
            lower.contains("room") -> Icons.Rounded.Hotel
        lower.contains("grocer") || lower.contains("mart") || lower.contains("blinkit") ||
            lower.contains("zepto") || lower.contains("instamart") || lower.contains("supermarket") -> Icons.Rounded.ShoppingCart
        lower.contains("drink") || lower.contains("outing") || lower.contains("bar") ||
            lower.contains("pub") || lower.contains("club") || lower.contains("party") ||
            lower.contains("beer") -> Icons.Rounded.LocalBar
        lower.contains("coffee") || lower.contains("cafe") || lower.contains("tea") ||
            lower.contains("chai") || lower.contains("bakery") || lower.contains("snack") ||
            lower.contains("breakfast") -> Icons.Rounded.LocalCafe
        lower.contains("movie") || lower.contains("cinema") || lower.contains("concert") ||
            lower.contains("show") || lower.contains("event") || lower.contains("museum") -> Icons.Rounded.ConfirmationNumber
        lower.contains("shop") || lower.contains("gift") || lower.contains("clothes") ||
            lower.contains("souvenir") -> Icons.Rounded.ShoppingBag
        lower.contains("dinner") || lower.contains("food") || lower.contains("lunch") ||
            lower.contains("restaurant") || lower.contains("pizza") || lower.contains("biryani") ||
            lower.contains("zomato") || lower.contains("swiggy") || lower.contains("meal") -> Icons.Rounded.Restaurant
        else -> Icons.AutoMirrored.Rounded.ReceiptLong
    }
}

fun resolveExpenseCategoryBadgeColors(title: String, isDark: Boolean): Pair<Color, Color> {
    com.splitmate.app.ui.category.exactExpenseCategoryFor(title)?.let {
        return com.splitmate.app.ui.category.ExpenseCategoryIcons.toneColors(it.tone, isDark)
    }
    val lower = title.lowercase()
    return when {
        isFlightTicketExpense(title) ->
            if (isDark) Color(0xFF282552) to Color(0xFFDCE3FD) else Color(0xFFEEF2FF) to Color(0xFF2B2768)
        lower.contains("train") || lower.contains("pnr") || lower.contains("irctc") || lower.contains("rail") ->
            if (isDark) Color(0xFF283A18) to Color(0xFFD7E8B6) else Color(0xFFDCE9B9) to Color(0xFF365314)
        lower.contains("hotel") || lower.contains("stay") ->
            if (isDark) Color(0xFF222A4A) to Color(0xFFC7D2FE) else Color(0xFFE0E7FF) to Color(0xFF3730A3)
        lower.contains("cab") || lower.contains("auto") || lower.contains("uber") || lower.contains("taxi") || lower.contains("fuel") ->
            if (isDark) Color(0xFF3D2E14) to Color(0xFFFDE68A) else Color(0xFFFEF3C7) to Color(0xFF92400E)
        lower.contains("grocer") || lower.contains("mart") || lower.contains("shop") ->
            if (isDark) Color(0xFF1F3833) to Color(0xFFA7F3D0) else Color(0xFFD1FAE5) to Color(0xFF065F46)
        lower.contains("drink") || lower.contains("outing") || lower.contains("bar") || lower.contains("party") ->
            if (isDark) Color(0xFF3B1D2E) to Color(0xFFFBCFE8) else Color(0xFFFCE7F3) to Color(0xFF9D174D)
        else ->
            if (isDark) Color(0xFF3D231B) to Color(0xFFFED8C8) else Color(0xFFFCE3D7) to Color(0xFF7C2D12)
    }
}

// ==============================================================================
// CRISP HARDWARE VIBRATOR & TACTILE KEYPAD HAPTIC ENGINE
// ==============================================================================
fun performCrispTactileHaptic(
    context: Context,
    view: android.view.View? = null,
    heavy: Boolean = false
) {
    val prefs = com.splitmate.app.data.EncryptedPrefsProvider.get(context)
    if (!prefs.getBoolean("pref_haptics", true)) return

    runCatching {
        @Suppress("DEPRECATION")
        view?.performHapticFeedback(
            if (heavy) android.view.HapticFeedbackConstants.LONG_PRESS
            else android.view.HapticFeedbackConstants.KEYBOARD_TAP,
            android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    runCatching {
        val vibrator: android.os.Vibrator? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val durationMs = if (heavy) 28L else 16L
                val amplitude = if (heavy) 245 else 195
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (heavy) 28L else 16L)
            }
        }
    }
}

@Composable
fun SplitMateCircularLogoBadge(
    sizeDp: Int = 44,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(SplitMateTheme.SurfaceWhite, CircleShape)
            .border(2.dp, SplitMateTheme.PrimaryDark.copy(alpha = 0.28f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(CircleShape)
        ) {
            drawArc(
                color = Color(0xFFD7E8B6),
                startAngle = 135f,
                sweepAngle = 180f,
                useCenter = true
            )
            drawArc(
                color = Color(0xFFE06B52),
                startAngle = 315f,
                sweepAngle = 180f,
                useCenter = true
            )
            drawCircle(
                color = Color(0xFF365314),
                radius = size.minDimension * 0.48f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF365314),
                radius = size.minDimension * 0.10f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.36f, size.height * 0.36f)
            )
            drawCircle(
                color = Color(0xFFFAF6F0),
                radius = size.minDimension * 0.10f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.64f, size.height * 0.64f)
            )
        }
    }
}

/**
 * Compact 76dp Single-Row Buckwheat Ticket Stub Pill (`CompactLedgerTicketStub`).
 * Designed for Group Overview & Activity Audit feeds so multiple logged train PNRs never
 * clutter the vertical scroll with 380dp+ multi-section boarding passes.
 * Tapping opens the full Tactile Paper PNR Studio (`PnrExpenseReviewScreen`) for that PNR.
 */
@Composable
fun CompactLedgerTicketStub(
    ticket: ParsedTravelTicket,
    @Suppress("UNUSED_PARAMETER") totalAmountDisplay: String = "",
    perPersonShareDisplay: String = "",
    onInspectTactilePass: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val localView = androidx.compose.ui.platform.LocalView.current
    val isDark = SplitMateTheme.isDark
    val isFlight = isFlightTicketExpense(ticket.cleanTitle, ticket)
    val stubBg = when {
        isFlight && isDark -> Color(0xFF1B1936)
        isFlight -> Color(0xFFF3F5FF)
        isDark -> Color(0xFF212B18)
        else -> Color(0xFFF3F7EA)
    }
    val stubBorder = when {
        isFlight && isDark -> Color(0xFF3E397A)
        isFlight -> Color(0xFFC7D2FE)
        isDark -> Color(0xFF3B5224)
        else -> Color(0xFFCDE0A8)
    }
    val badgeBg = when {
        isFlight && isDark -> Color(0xFF2B2768)
        isFlight -> Color(0xFFEEF2FF)
        isDark -> Color(0xFF2D401B)
        else -> Color(0xFFDCE9B9)
    }
    val badgeText = when {
        isFlight && isDark -> Color(0xFFDCE3FD)
        isFlight -> Color(0xFF2B2768)
        isDark -> Color(0xFFD7E8B6)
        else -> Color(0xFF365314)
    }

    val fromCode = ticket.fromStation.ifBlank { "ORG" }.uppercase(java.util.Locale.US)
    val toCode = ticket.toStation.ifBlank { "DST" }.uppercase(java.util.Locale.US)
    val statusBadge = ticket.bookingStatus.ifBlank { "CNF" }

    Surface(
        onClick = {
            performCrispTactileHaptic(context, localView, heavy = false)
            onInspectTactilePass?.invoke()
        },
        enabled = onInspectTactilePass != null,
        shape = RoundedCornerShape(14.dp),
        color = stubBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, stubBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFlight) Icons.Rounded.FlightTakeoff else Icons.Rounded.Train,
                        contentDescription = if (isFlight) "Flight Ticket Stub" else "IRCTC Ticket Stub",
                        tint = badgeText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = fromCode,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = SplitMateTheme.PrimaryDark
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = SplitMateTheme.PrimaryDark,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = toCode,
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = SplitMateTheme.PrimaryDark
                            )
                        }
                        if (ticket.pnr.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeBg
                            ) {
                                Text(
                                    text = "PNR ${ticket.pnr}",
                                    style = TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = badgeText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            if (ticket.trainOrFlightNo.isNotBlank()) append(ticket.trainOrFlightNo).append(" · ")
                            append(statusBadge)
                            if (perPersonShareDisplay.isNotBlank()) append(" · ").append(perPersonShareDisplay).append("/pax")
                        },
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = SplitMateTheme.TextSecondary,
                        maxLines = 1
                    )
                }
            }

            if (onInspectTactilePass != null) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = badgeBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Paper Pass",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = badgeText
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = badgeText,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}


