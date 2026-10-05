package com.splitmate.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.LocalSplitMatePalette
import com.splitmate.app.ui.SplitMateThemeMode

// ==============================================================================
// v2.3.6 APP-SHELL EXTENDED (CUSTOM / STATIC) COLOURS
// ==============================================================================
// Brand and semantic accents used by `SplitMateAppComposable.kt` that have no M3 colour-role
// equivalent (transit passes, online presence, settlement boards, hero gradient, ...).
//
// Following the M3 "custom colors" rule every family exposes 4 roles: `color`, `onColor`,
// `container`, `onContainer`. Families that draw a visible card/chip boundary additionally
// expose `outline`, and the flight pass exposes `onContainerVariant` for its subtitle line.
//
// Hex literals are allowed ONLY in this file. Sunlit Buckwheat values equal the literals that
// previously lived in `SplitMateAppComposable.kt`, so the default theme renders unchanged.
// Every text/background pair below was checked against WCAG 2.x (4.5:1 text, 3:1 icons and
// boundaries) in all three themes.

/** One extended colour family (M3 custom colour: color / onColor / container / onContainer). */
@Immutable
data class AppShellExtendedColor(
    val color: Color,
    val onColor: Color,
    val container: Color,
    val onContainer: Color,
    /** Boundary of a [container] surface (1dp card / chip border). */
    val outline: Color = onContainer.copy(alpha = 0.4f),
    /** Lower-emphasis text on [container] (subtitles). */
    val onContainerVariant: Color = onContainer
)

/** All extended app-shell colours for one [SplitMateThemeMode]. */
@Immutable
data class AppShellExtendedColors(
    /** Soft sage "positive" family behind `SplitMateTheme.SageSurface` / `SageText`. */
    val sage: AppShellExtendedColor,
    /** Terracotta "you owe / declined" family behind `SplitMateTheme.TerracottaSurface` / `TerracottaText`. */
    val terracotta: AppShellExtendedColor,
    /** Live presence ("N Online") pill: `color` is the status dot, text uses `onContainer`. */
    val online: AppShellExtendedColor,
    /** Train Pass face of the transit deck (deep-olive badge, sage card). */
    val trainPass: AppShellExtendedColor,
    /** Flight Pass face of the transit deck (indigo badge, periwinkle card). */
    val flightPass: AppShellExtendedColor,
    /** Periwinkle flip / edit chips on the Flight Pass. */
    val flightChip: AppShellExtendedColor,
    /** Logged flight ticket chips and flight accents on expense rows. */
    val flightTicket: AppShellExtendedColor,
    /** Settle-tab trip summary board (GETS BACK | OWES). */
    val settleSummary: AppShellExtendedColor,
    /** Settle-tab RECEIVES member card. */
    val settleReceive: AppShellExtendedColor,
    /** Settle-tab PAYS peach breakdown box. */
    val settlePay: AppShellExtendedColor,
    /** Ledgers hero balance card two-stop gradient. */
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    /** Dark ink for text on light avatar-preset pastels (static: identical in every theme). */
    val pastelInkDark: Color,
    /** Light ink for text on dark avatar-preset swatches (static: identical in every theme). */
    val pastelInkLight: Color
) {
    /** Picks the pastel ink with the higher contrast against [background] (avatar preset chips). */
    fun pastelInkOn(background: Color): Color =
        if (background.luminance() > 0.179f) pastelInkDark else pastelInkLight
}

// ------------------------------------------------------------------------------
// Sunlit Buckwheat (light) - identical to the pre-v2.3.6 literals.
// ------------------------------------------------------------------------------
val SunlitBuckwheatAppShellExtendedColors = AppShellExtendedColors(
    sage = AppShellExtendedColor(
        color = Color(0xFF416913),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFEAF3DC),
        onContainer = Color(0xFF416913)
    ),
    terracotta = AppShellExtendedColor(
        color = Color(0xFFE06B52),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFFED8C8),
        onContainer = Color(0xFF7C2D12)
    ),
    online = AppShellExtendedColor(
        color = Color(0xFF22C55E),
        onColor = Color(0xFF052E16),
        container = Color(0xFFDCFCE7),
        // v2.3.6: #15803D only reached 4.38:1 on the #F4EFE6 member chip; #166534 reaches 6.2:1.
        onContainer = Color(0xFF166534)
    ),
    trainPass = AppShellExtendedColor(
        color = Color(0xFF264010),
        onColor = Color(0xFFD7E8B6),
        container = Color(0xFFEAF3D5),
        onContainer = Color(0xFF264010),
        outline = Color(0xFFC5DCA0)
    ),
    flightPass = AppShellExtendedColor(
        color = Color(0xFF2B2768),
        onColor = Color(0xFFEEF2FF),
        container = Color(0xFFEEF2FF),
        onContainer = Color(0xFF1F1C4D),
        outline = Color(0xFFC7D2FE),
        onContainerVariant = Color(0xFF433E85)
    ),
    flightChip = AppShellExtendedColor(
        color = Color(0xFF3730A3),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFEEF2FF),
        onContainer = Color(0xFF3730A3),
        outline = Color(0xFFA5B4FC)
    ),
    flightTicket = AppShellExtendedColor(
        color = Color(0xFF3730A3),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFFFFFFF),
        onContainer = Color(0xFF1F1C4D),
        outline = Color(0xFFA5B4FC)
    ),
    settleSummary = AppShellExtendedColor(
        color = Color(0xFF416913),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFF3F7EB),
        onContainer = Color(0xFF23201E),
        outline = Color(0xFFDCE6C8)
    ),
    settleReceive = AppShellExtendedColor(
        color = Color(0xFF416913),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFF1F7E8),
        onContainer = Color(0xFF23201E),
        outline = Color(0xFFD8E5C2)
    ),
    settlePay = AppShellExtendedColor(
        color = Color(0xFFE06B52),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFFDF2EE),
        onContainer = Color(0xFF7C2D12),
        outline = Color(0xFFF7E0D7)
    ),
    heroGradientStart = Color(0xFFF5F8EC),
    heroGradientEnd = Color(0xFFFDF1EC),
    pastelInkDark = Color(0xFF23201E),
    pastelInkLight = Color(0xFFFAF6F0)
)

// ------------------------------------------------------------------------------
// Warm Espresso Night (dark) - the former `if (SplitMateTheme.isDark)` literals; the
// settlement boards fall back to the Espresso surface ladder exactly as before.
// ------------------------------------------------------------------------------
val WarmEspressoNightAppShellExtendedColors = AppShellExtendedColors(
    sage = AppShellExtendedColor(
        color = Color(0xFFA3E635),
        onColor = Color(0xFF1A2E05),
        container = Color(0xFF283D0E),
        onContainer = Color(0xFFD9F99D)
    ),
    terracotta = AppShellExtendedColor(
        color = Color(0xFFFB923C),
        onColor = Color(0xFF431A08),
        container = Color(0xFF3A2019),
        onContainer = Color(0xFFFECDD3)
    ),
    online = AppShellExtendedColor(
        color = Color(0xFF4ADE80),
        onColor = Color(0xFF052E16),
        container = Color(0xFF14532D),
        onContainer = Color(0xFFBBF7D0)
    ),
    trainPass = AppShellExtendedColor(
        color = Color(0xFF264010),
        onColor = Color(0xFFD7E8B6),
        container = Color(0xFF1F2B16),
        onContainer = Color(0xFFD9F99D),
        outline = Color(0xFF3D5428)
    ),
    flightPass = AppShellExtendedColor(
        color = Color(0xFF282552),
        onColor = Color(0xFFDCE3FD),
        container = Color(0xFF1B1936),
        onContainer = Color(0xFFE6EAFF),
        outline = Color(0xFF3F3A82),
        onContainerVariant = Color(0xFFB5BEEC)
    ),
    flightChip = AppShellExtendedColor(
        color = Color(0xFFA5B4FC),
        onColor = Color(0xFF1E1B4B),
        container = Color(0xFF24214A),
        onContainer = Color(0xFFDCE3FD),
        outline = Color(0xFF4E48A6)
    ),
    flightTicket = AppShellExtendedColor(
        color = Color(0xFFA5B4FC),
        onColor = Color(0xFF1E1B4B),
        container = Color(0xFF24214A),
        onContainer = Color(0xFFE6EAFF),
        outline = Color(0xFF4E48A6)
    ),
    settleSummary = AppShellExtendedColor(
        color = Color(0xFFA3E635),
        onColor = Color(0xFF1A2E05),
        container = Color(0xFF14110F),
        onContainer = Color(0xFFF5F0E6),
        outline = Color(0xFF38312B)
    ),
    settleReceive = AppShellExtendedColor(
        color = Color(0xFFA3E635),
        onColor = Color(0xFF1A2E05),
        container = Color(0xFF14110F),
        onContainer = Color(0xFFF5F0E6),
        outline = Color(0xFF38312B)
    ),
    settlePay = AppShellExtendedColor(
        color = Color(0xFFFB923C),
        onColor = Color(0xFF431A08),
        container = Color(0xFF25201C),
        onContainer = Color(0xFFFED8C8),
        outline = Color(0xFF38312B)
    ),
    heroGradientStart = Color(0xFF233216),
    heroGradientEnd = Color(0xFF24201C),
    pastelInkDark = Color(0xFF23201E),
    pastelInkLight = Color(0xFFFAF6F0)
)

// ------------------------------------------------------------------------------
// Kyoto Matcha & Yuzu (light) - transit passes keep the Sunlit values they always used
// (the old code only branched on dark); Kyoto-specific settlement/hero tints preserved.
// ------------------------------------------------------------------------------
val KyotoMatchaYuzuAppShellExtendedColors = AppShellExtendedColors(
    sage = AppShellExtendedColor(
        color = Color(0xFF0F6B3E),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFF86EFAC),
        onContainer = Color(0xFF062E19)
    ),
    terracotta = AppShellExtendedColor(
        color = Color(0xFFB45309),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFFEF08A),
        onContainer = Color(0xFF451A03)
    ),
    online = AppShellExtendedColor(
        color = Color(0xFF22C55E),
        onColor = Color(0xFF052E16),
        container = Color(0xFFDCFCE7),
        // #15803D only reaches 3.7:1 on Kyoto #CCE6C2 chips; #14532D reaches 6.8:1.
        onContainer = Color(0xFF14532D)
    ),
    trainPass = SunlitBuckwheatAppShellExtendedColors.trainPass,
    flightPass = SunlitBuckwheatAppShellExtendedColors.flightPass,
    flightChip = SunlitBuckwheatAppShellExtendedColors.flightChip,
    flightTicket = SunlitBuckwheatAppShellExtendedColors.flightTicket,
    settleSummary = AppShellExtendedColor(
        color = Color(0xFF0F6B3E),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFD2F4DC),
        onContainer = Color(0xFF0A1F12),
        outline = Color(0xFF75C993)
    ),
    settleReceive = AppShellExtendedColor(
        color = Color(0xFF0F6B3E),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFD2F4DC),
        onContainer = Color(0xFF0A1F12),
        outline = Color(0xFF75C993)
    ),
    settlePay = AppShellExtendedColor(
        color = Color(0xFFB45309),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFFEF3C7),
        onContainer = Color(0xFF451A03),
        outline = Color(0xFFF59E0B).copy(alpha = 0.45f)
    ),
    heroGradientStart = Color(0xFFC4ECCB),
    heroGradientEnd = Color(0xFFFEF08A),
    pastelInkDark = Color(0xFF23201E),
    pastelInkLight = Color(0xFFFAF6F0)
)

/** Pure mapping from a theme mode to its extended app-shell colours. */
fun appShellExtendedFor(mode: SplitMateThemeMode): AppShellExtendedColors = when (mode) {
    SplitMateThemeMode.SUNLIT_BUCKWHEAT -> SunlitBuckwheatAppShellExtendedColors
    SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> WarmEspressoNightAppShellExtendedColors
    SplitMateThemeMode.KYOTO_MATCHA_YUZU -> KyotoMatchaYuzuAppShellExtendedColors
}

/** Composable resolver: extended colours for the palette provided by `SplitMateExpressiveTheme`. */
@Composable
fun appShellExtended(): AppShellExtendedColors = appShellExtendedFor(LocalSplitMatePalette.current.mode)

/** Non-composable resolver for getter-style token objects (reads the active palette state). */
val activeAppShellExtended: AppShellExtendedColors
    get() = appShellExtendedFor(DesignSystemBindings.activePalette.mode)
