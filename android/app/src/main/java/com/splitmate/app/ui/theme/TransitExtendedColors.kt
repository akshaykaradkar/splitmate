package com.splitmate.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.KyotoMatchaYuzuPalette
import com.splitmate.app.ui.LocalSplitMatePalette
import com.splitmate.app.ui.SplitMateExpressivePalette
import com.splitmate.app.ui.SplitMateThemeMode
import com.splitmate.app.ui.SunlitBuckwheatPalette
import com.splitmate.app.ui.WarmEspressoNightPalette

// ==============================================================================
// v2.3.6 Wave 3: TRANSIT EXTENDED COLOURS (M3 "custom / static colours")
// ==============================================================================
//
// Colours with no Material 3 role equivalent (transit brand passes, boarding-pass
// stubs, IRCTC status pills) are defined ONCE here, per theme. Following the
// m3.material.io custom-colour rule, every extended colour exposes four roles:
// `color`, `onColor`, `container`, `onContainer`. Surface-specific tones that a
// physical pass needs (gradient end, raised badge, hairlines, foil accents) are
// carried alongside the four roles.
//
// Hex literals are ONLY allowed in this file. UI code must read colours through
// [transitExtendedColors] (composable, reads LocalSplitMatePalette) or
// [activeTransitExtendedColors] (non-composable, reads DesignSystemBindings).
//
// Sunlit Buckwheat values are the exact v2.3.5 values so the default theme looks
// unchanged. Every text/background pair was checked to reach WCAG 4.5:1
// (3:1 for large/bold text, icons and boundaries) in all three themes.

/** The four M3 custom-colour roles. */
@Immutable
data class ExtendedColorRoles(
    val color: Color,
    val onColor: Color,
    val container: Color,
    val onContainer: Color
)

/**
 * A transit brand pass (train / flight). [color] is the hero surface (gradient start)
 * and [onColor] the headline ink on it; [container] / [onContainer] form the light
 * action pill that sits on the hero.
 */
@Immutable
data class TransitPassColors(
    val color: Color,
    val onColor: Color,
    val container: Color,
    val onContainer: Color,
    /** Gradient end of the hero surface. */
    val colorDeep: Color,
    /** Raised tone on the hero (badges, wallet sleeve, dark-theme CTA). */
    val colorRaised: Color,
    /** Hairline around [colorRaised] elements. */
    val colorRaisedOutline: Color,
    /** Secondary text / icon ink on the hero (>= 4.5:1 on color, colorDeep, colorRaised). */
    val onColorVariant: Color,
    /** Tertiary captions on the hero (>= 4.5:1 on color, colorDeep, colorRaised). */
    val onColorSubtle: Color,
    /** Least-emphasis captions on the hero (>= 4.5:1 on color, colorDeep, colorRaised). */
    val onColorMuted: Color,
    /** Hairline boundary drawn on/around hero-coloured elements. */
    val outline: Color,
    /** Hairline around [container] pills. */
    val containerOutline: Color,
    /** Foil / headlight highlight on the hero (text-safe, >= 4.5:1 on the hero tones). */
    val highlight: Color,
    /** Status beacon / dot on the hero (graphic, >= 3:1 on [color]). */
    val signal: Color,
    /** Purely decorative strokes (dashed route lines, stitching). No contrast requirement. */
    val decoration: Color
) {
    /** The four M3 custom-colour roles of this pass. */
    val roles: ExtendedColorRoles
        get() = ExtendedColorRoles(color, onColor, container, onContainer)
}

/** Physical boarding-pass details shared by the IRCTC and flight review screens. */
@Immutable
data class BoardingPassColors(
    /** Dashed perforation line between the pass body and the stub. */
    val perforation: Color,
    /**
     * Commit stamp: [ExtendedColorRoles.color] = gold foil spark, [ExtendedColorRoles.container]
     * = stamp paper, [ExtendedColorRoles.onContainer] = stamp sub-label ink.
     */
    val stamp: ExtendedColorRoles,
    /** Leading dot of the tear sweep line. */
    val tearCursor: Color,
    /** Tear-cut gradient start / end (decorative). */
    val tearGradientStart: Color,
    val tearGradientEnd: Color
)

/** A status pill (IRCTC chart / booking status): the four roles plus its 1dp outline. */
@Immutable
data class StatusPillColors(
    val roles: ExtendedColorRoles,
    val outline: Color
)

@Immutable
data class TransitExtendedColors(
    val mode: SplitMateThemeMode,
    /** IRCTC review boarding pass (PnrExpenseReviewScreen). */
    val trainTicket: TransitPassColors,
    /** Animated Transit Deck hero card, train pass. */
    val trainDeck: TransitPassColors,
    /** Trip Hub train ticket card (`TripHubTokens.Train*`), exposed for adoption. */
    val trainHub: TransitPassColors,
    /** Flight review boarding pass + Animated Transit Deck flight pass. */
    val flight: TransitPassColors,
    /** Trip Hub flight ticket card (`TripHubTokens.Flight*`), exposed for adoption. */
    val flightHub: TransitPassColors,
    val boardingPass: BoardingPassColors,
    /** Chart not prepared / pending (amber). */
    val statusPending: StatusPillColors,
    /** Confirmed / chart prepared (palette primary roles + sage outline). */
    val statusConfirmed: StatusPillColors,
    /** Waitlist / RAC (palette secondary roles + peach outline). */
    val statusWaitlist: StatusPillColors,
    /** Categorical member avatar (container to onContainer) pairs. */
    val memberAvatars: List<Pair<Color, Color>>
)

// ------------------------------------------------------------------------------
// Per-theme definitions
// ------------------------------------------------------------------------------

private val White = Color(0xFFFFFFFF)

private val SunlitTrainTicket = TransitPassColors(
    color = Color(0xFF264010),
    onColor = White,
    container = Color(0xFFD7E8B6),
    onContainer = Color(0xFF1B2E0B),
    colorDeep = Color(0xFF1B2E0B),
    colorRaised = Color(0xFF345418),
    colorRaisedOutline = Color(0xFF4A7325),
    onColorVariant = Color(0xFFD7E8B6),
    onColorSubtle = Color(0xFFC5D6A7),
    onColorMuted = Color(0xFFAEC48A),
    outline = Color(0xFF4A7325),
    containerOutline = Color(0xFFC2E0A3),
    highlight = Color(0xFFD7E8B6),
    signal = Color(0xFFD7E8B6),
    decoration = Color(0xFF6B9440)
)

// Espresso Night keeps the deep forest pass (it already reads as a dark card).
private val EspressoTrainTicket = SunlitTrainTicket

private val KyotoTrainTicket = SunlitTrainTicket.copy(
    color = Color(0xFF1F3D1C),
    onContainer = Color(0xFF142812),
    colorDeep = Color(0xFF142812),
    colorRaised = Color(0xFF2B5427),
    colorRaisedOutline = Color(0xFF437A3D),
    outline = Color(0xFF437A3D)
)

private val SunlitTrainDeck = TransitPassColors(
    color = Color(0xFF32571F),
    onColor = White,
    container = Color(0xFFEAF5DC),
    onContainer = Color(0xFF254212),
    colorDeep = Color(0xFF223E13),
    colorRaised = Color(0xFF223E13),
    colorRaisedOutline = Color(0xFFC3E29C),
    onColorVariant = Color(0xFFEAF5DC),
    onColorSubtle = Color(0xFFD7E8B6),
    onColorMuted = Color(0xFFC5D6A7),
    outline = Color(0xFFC3E29C),
    containerOutline = Color(0xFFC3E29C),
    highlight = Color(0xFFFFF59D),
    // 4CAF50 measured 2.996:1 on 32571F; nudged to 4EB152 (3.07:1) to clear the 3:1 icon minimum.
    signal = Color(0xFF4EB152),
    decoration = Color(0xFFEAF5DC)
)

private val EspressoTrainDeck = SunlitTrainDeck

private val KyotoTrainDeck = SunlitTrainDeck.copy(
    color = Color(0xFF2B5427),
    colorDeep = Color(0xFF1F3D1C),
    colorRaised = Color(0xFF1F3D1C),
    onContainer = Color(0xFF1F3D1C)
)

private val SunlitTrainHub = TransitPassColors(
    color = Color(0xFF2D4F12),
    onColor = White,
    container = Color(0xFF80AD47),
    onContainer = Color(0xFF132604),
    colorDeep = Color(0xFF213B0C),
    colorRaised = Color(0xFF223D0D),
    colorRaisedOutline = Color(0xFF3E651E),
    onColorVariant = Color(0xFFD7E8B6),
    onColorSubtle = Color(0xFFC5D6A7),
    onColorMuted = Color(0xFFAEC48A),
    outline = Color(0xFF3E651E),
    containerOutline = Color(0xFF84A950),
    highlight = Color(0xFFB5DC86),
    signal = Color(0xFF84A950),
    decoration = Color(0xFF6B9440)
)

private val EspressoTrainHub = SunlitTrainHub.copy(
    color = Color(0xFF264210),
    container = Color(0xFF8BB85A),
    colorDeep = Color(0xFF1B3009),
    colorRaised = Color(0xFF1A300A),
    colorRaisedOutline = Color(0xFF345718),
    outline = Color(0xFF345718),
    containerOutline = Color(0xFF759943),
    signal = Color(0xFF759943)
)

private val KyotoTrainHub = SunlitTrainHub.copy(
    color = Color(0xFF1E3F24),
    colorDeep = Color(0xFF142E19)
)

private val SunlitFlight = TransitPassColors(
    color = Color(0xFF2B2768),
    onColor = White,
    container = Color(0xFFEEF2FF),
    onContainer = Color(0xFF2B2768),
    colorDeep = Color(0xFF1B1849),
    colorRaised = Color(0xFF282552),
    colorRaisedOutline = Color(0xFF5650B8),
    onColorVariant = Color(0xFFEEF2FF),
    onColorSubtle = Color(0xFFC7D2FE),
    onColorMuted = Color(0xFFC7D2FE),
    outline = Color(0xFFC7D2FE),
    containerOutline = Color(0xFFC7D2FE),
    highlight = Color(0xFFFDE68A),
    signal = Color(0xFF818CF8),
    decoration = Color(0xFFEAB308)
)

private val EspressoFlight = SunlitFlight.copy(
    color = Color(0xFF1F1C52),
    colorDeep = Color(0xFF141236),
    outline = Color(0xFF4E48A6)
)

private val KyotoFlight = SunlitFlight.copy(
    color = Color(0xFF23383B),
    container = Color(0xFFDDF0F2),
    onContainer = Color(0xFF23383B),
    colorDeep = Color(0xFF162527),
    colorRaised = Color(0xFF2D4A4E),
    colorRaisedOutline = Color(0xFF4F7A7E),
    onColorSubtle = Color(0xFFB4D4D8),
    onColorMuted = Color(0xFFB4D4D8),
    outline = Color(0xFFB4D4D8),
    containerOutline = Color(0xFFB4D4D8),
    signal = Color(0xFF8CC7CC)
)

private val SunlitFlightHub = SunlitFlight.copy(
    onColorVariant = Color(0xFFDCE3FD),
    onColorSubtle = Color(0xFFC7D2FE)
)

private val EspressoFlightHub = SunlitFlightHub.copy(
    color = Color(0xFF242059),
    colorDeep = Color(0xFF16133B),
    outline = Color(0xFF4E48A6)
)

private val KyotoFlightHub = KyotoFlight.copy(
    onColorVariant = Color(0xFFDCE3FD)
)

private val SunlitBoardingPass = BoardingPassColors(
    perforation = Color(0xFFD6CEBE),
    stamp = ExtendedColorRoles(
        color = Color(0xFFF59E0B),
        onColor = Color(0xFF451A03),
        container = Color(0xFFFFFCF7),
        onContainer = Color(0xFF4A443E)
    ),
    tearCursor = Color(0xFFD7E8B6),
    tearGradientStart = Color(0xFF6366F1),
    tearGradientEnd = Color(0xFF4F46E5)
)

private val EspressoBoardingPass = SunlitBoardingPass.copy(perforation = Color(0xFF4A443C))

private val KyotoBoardingPass = SunlitBoardingPass.copy(perforation = Color(0xFFB5CCA8))

private val SunlitStatusPending = StatusPillColors(
    roles = ExtendedColorRoles(
        color = Color(0xFF9E5808),
        onColor = White,
        container = Color(0xFFFEF3D6),
        onContainer = Color(0xFF9E5808)
    ),
    outline = Color(0xFFF7D788)
)

private val EspressoStatusPending = StatusPillColors(
    roles = ExtendedColorRoles(
        color = Color(0xFFFCD34D),
        onColor = Color(0xFF332610),
        container = Color(0xFF332610),
        onContainer = Color(0xFFFCD34D)
    ),
    outline = Color(0xFF6B4E1B)
)

private val KyotoStatusPending = SunlitStatusPending.copy(
    roles = SunlitStatusPending.roles.copy(container = Color(0xFFFEF9C3))
)

private val SunlitMemberAvatars = listOf(
    Color(0xFFD7E8B6) to Color(0xFF2D4810),
    Color(0xFFFFD8CC) to Color(0xFF8A2E1A),
    Color(0xFFD0E2FF) to Color(0xFF143E82),
    Color(0xFFD3D7FD) to Color(0xFF343B80),
    Color(0xFFFCE3D7) to Color(0xFF7C2D12),
    Color(0xFFE0F2FE) to Color(0xFF075985)
)

private val EspressoMemberAvatars = listOf(
    Color(0xFF283D0E) to Color(0xFFD9F99D),
    Color(0xFF6B3418) to Color(0xFFFED8C8),
    Color(0xFF1E3A66) to Color(0xFFD0E2FF),
    Color(0xFF3A3580) to Color(0xFFE0E7FF),
    Color(0xFF5A2A14) to Color(0xFFFCE3D7),
    Color(0xFF0C4A6E) to Color(0xFFE0F2FE)
)

// Kyoto Matcha is a light theme: the Sunlit pastel avatar pairs read correctly on its surfaces.
private val KyotoMemberAvatars = SunlitMemberAvatars

/**
 * Builds the extended colours for [palette]. Confirmed / waitlist pills reuse the palette's
 * primary / secondary roles (so they track the theme) and only add their 1dp outline here.
 */
private fun buildTransitExtendedColors(palette: SplitMateExpressivePalette): TransitExtendedColors {
    val confirmedText = if (palette.mode.isDark) palette.onPrimaryContainer else palette.primary
    val confirmedOnText = if (palette.mode.isDark) palette.primaryContainer else palette.onPrimary
    return when (palette.mode) {
        SplitMateThemeMode.SUNLIT_BUCKWHEAT -> TransitExtendedColors(
            mode = palette.mode,
            trainTicket = SunlitTrainTicket,
            trainDeck = SunlitTrainDeck,
            trainHub = SunlitTrainHub,
            flight = SunlitFlight,
            flightHub = SunlitFlightHub,
            boardingPass = SunlitBoardingPass,
            statusPending = SunlitStatusPending,
            statusConfirmed = StatusPillColors(
                ExtendedColorRoles(confirmedText, confirmedOnText, palette.primaryContainer, confirmedText),
                outline = Color(0xFFC2E0A3)
            ),
            statusWaitlist = StatusPillColors(
                ExtendedColorRoles(palette.secondary, palette.onSecondary, palette.secondaryContainer, palette.onSecondaryContainer),
                outline = Color(0xFFF7C6B5)
            ),
            memberAvatars = SunlitMemberAvatars
        )
        SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> TransitExtendedColors(
            mode = palette.mode,
            trainTicket = EspressoTrainTicket,
            trainDeck = EspressoTrainDeck,
            trainHub = EspressoTrainHub,
            flight = EspressoFlight,
            flightHub = EspressoFlightHub,
            boardingPass = EspressoBoardingPass,
            statusPending = EspressoStatusPending,
            statusConfirmed = StatusPillColors(
                ExtendedColorRoles(confirmedText, confirmedOnText, palette.primaryContainer, confirmedText),
                outline = Color(0xFF426128)
            ),
            statusWaitlist = StatusPillColors(
                ExtendedColorRoles(palette.secondary, palette.onSecondary, palette.secondaryContainer, palette.onSecondaryContainer),
                outline = Color(0xFF7C3725)
            ),
            memberAvatars = EspressoMemberAvatars
        )
        SplitMateThemeMode.KYOTO_MATCHA_YUZU -> TransitExtendedColors(
            mode = palette.mode,
            trainTicket = KyotoTrainTicket,
            trainDeck = KyotoTrainDeck,
            trainHub = KyotoTrainHub,
            flight = KyotoFlight,
            flightHub = KyotoFlightHub,
            boardingPass = KyotoBoardingPass,
            statusPending = KyotoStatusPending,
            statusConfirmed = StatusPillColors(
                ExtendedColorRoles(confirmedText, confirmedOnText, palette.primaryContainer, confirmedText),
                outline = Color(0xFFA8C6A3)
            ),
            statusWaitlist = StatusPillColors(
                ExtendedColorRoles(palette.secondary, palette.onSecondary, palette.secondaryContainer, palette.onSecondaryContainer),
                outline = Color(0xFFF7C6B5)
            ),
            memberAvatars = KyotoMemberAvatars
        )
    }
}

private val SunlitTransitExtendedColors by lazy { buildTransitExtendedColors(SunlitBuckwheatPalette) }
private val EspressoTransitExtendedColors by lazy { buildTransitExtendedColors(WarmEspressoNightPalette) }
private val KyotoTransitExtendedColors by lazy { buildTransitExtendedColors(KyotoMatchaYuzuPalette) }

/** Pure lookup of the extended colours for [mode]. */
fun transitExtendedColorsFor(mode: SplitMateThemeMode): TransitExtendedColors = when (mode) {
    SplitMateThemeMode.SUNLIT_BUCKWHEAT -> SunlitTransitExtendedColors
    SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> EspressoTransitExtendedColors
    SplitMateThemeMode.KYOTO_MATCHA_YUZU -> KyotoTransitExtendedColors
}

/**
 * Composable resolver: extended colours for the palette provided by `SplitMateExpressiveTheme`
 * through [LocalSplitMatePalette].
 */
@Composable
@ReadOnlyComposable
fun transitExtendedColors(): TransitExtendedColors =
    transitExtendedColorsFor(LocalSplitMatePalette.current.mode)

/**
 * Non-composable resolver for token objects (`TactilePaperPassTokens`, `FlightPassTokens`,
 * `TripHubTokens`, ...). Reads [DesignSystemBindings.activePalette], which is snapshot state,
 * so composables reading it still recompose on a theme switch.
 */
val activeTransitExtendedColors: TransitExtendedColors
    get() = transitExtendedColorsFor(DesignSystemBindings.activePalette.mode)
