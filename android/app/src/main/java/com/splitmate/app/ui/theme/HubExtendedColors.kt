package com.splitmate.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.SplitMateThemeMode

// ==============================================================================
// v2.3.6 HUB EXTENDED COLOURS (M3 custom / static colour groups)
// ==============================================================================
//
// The M3 baseline roles (primary / secondary / tertiary / error / surface*) live in
// `SplitMateExpressivePalette`. Colours that have no baseline role (participant avatar hues,
// category pastels, presence dot, RAC/WL warning, train/flight pass art) are declared here as
// M3 "custom colour" groups: 4 roles each (color, onColor, colorContainer, onColorContainer),
// resolved per theme (Sunlit Buckwheat, Warm Espresso Night, Kyoto Matcha & Yuzu).
//
// This is the ONLY file in the hub/quick-expense/category surface that may contain hex
// literals. Sunlit Buckwheat values are the pre-v2.3.6 literals so the default theme looks the
// same. Every on/container pair is >= 4.5:1 (see the WCAG script referenced in the v2.3.6
// wave-3 report).
//
// Train/flight pass tokens are hub-local on purpose (no coupling to other transit token files).

/** One M3 custom colour group (4 roles). */
@Immutable
data class HubColorRoles(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)

/** Hue families used by the expense category picker badges. */
enum class HubCategoryHue { SAGE, PERIWINKLE, AMBER, MINT, ROSE, PEACH }

/** Deep-forest train pass art (Trip Hub card + Quick Expense PNR promo tile). */
@Immutable
data class HubTrainPassColors(
    val forestTop: Color,
    val forestBottom: Color,
    /** Primary text / icons on the forest gradient. */
    val onPass: Color,
    val accentLime: Color,
    val secondarySage: Color,
    val nextUpPillBg: Color,
    val nextUpPillText: Color,
    val berthCellBg: Color,
    val berthCellBorder: Color,
    val primaryCtaBg: Color,
    val primaryCtaText: Color,
    val promoContainer: Color,
    val promoBorder: Color,
    val promoSubtitle: Color,
    val promoPill: Color,
    val onPromoPill: Color
)

/** Aviation navy flight pass art (Trip Hub boarding pass card). */
@Immutable
data class HubFlightPassColors(
    val navyTop: Color,
    val navyBottom: Color,
    /** Primary text / icons on the navy gradient. */
    val onPass: Color,
    val secondaryLavender: Color,
    val accentPeriwinkle: Color
)

object HubExtendedColors {

    private val activeMode: SplitMateThemeMode
        get() = DesignSystemBindings.activeThemeMode

    /**
     * Mirrors the `resolvedPalette` rule used by `SplitMateTheme` tokens: when the legacy
     * `isDark` flag disagrees with the active mode, fall back to the matching default theme.
     */
    fun resolveMode(isDark: Boolean, mode: SplitMateThemeMode = activeMode): SplitMateThemeMode = when {
        isDark && !mode.isDark -> SplitMateThemeMode.WARM_ESPRESSO_NIGHT
        !isDark && mode.isDark -> SplitMateThemeMode.SUNLIT_BUCKWHEAT
        else -> mode
    }

    // --------------------------------------------------------------------------
    // Static (theme-independent) colours
    // --------------------------------------------------------------------------

    /** Text/icons drawn over a photo darkened by the `scrim` role (photo is theme-independent). */
    val OnImageScrim: Color = Color(0xFFFFFFFF)

    /** Ink for labels placed on a fixed light avatar backdrop swatch. */
    val SwatchInkOnLight: Color = Color(0xFF23201E)

    /** Ink for labels placed on a fixed dark avatar backdrop swatch. */
    val SwatchInkOnDark: Color = Color(0xFFFFFFFF)

    // --------------------------------------------------------------------------
    // Presence (online dot)
    // --------------------------------------------------------------------------

    private val PresenceSunlit = HubColorRoles(
        color = Color(0xFF16A34A),
        onColor = Color(0xFFFFFFFF),
        colorContainer = Color(0xFFDCFCE7),
        onColorContainer = Color(0xFF14532D)
    )
    private val PresenceEspresso = HubColorRoles(
        color = Color(0xFF4ADE80),
        onColor = Color(0xFF052E16),
        colorContainer = Color(0xFF14532D),
        onColorContainer = Color(0xFFDCFCE7)
    )
    private val PresenceKyoto = HubColorRoles(
        color = Color(0xFF15803D),
        onColor = Color(0xFFFFFFFF),
        colorContainer = Color(0xFFDCFCE7),
        onColorContainer = Color(0xFF14532D)
    )

    fun presence(mode: SplitMateThemeMode = activeMode): HubColorRoles = when (mode) {
        SplitMateThemeMode.SUNLIT_BUCKWHEAT -> PresenceSunlit
        SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> PresenceEspresso
        SplitMateThemeMode.KYOTO_MATCHA_YUZU -> PresenceKyoto
    }

    // --------------------------------------------------------------------------
    // Warning (RAC / WL ticket status)
    // --------------------------------------------------------------------------

    private val WarningLight = HubColorRoles(
        color = Color(0xFF9A3412),
        onColor = Color(0xFFFFFFFF),
        colorContainer = Color(0xFFFFEDD5),
        onColorContainer = Color(0xFF7C2D12)
    )
    private val WarningEspresso = HubColorRoles(
        color = Color(0xFFFDBA74),
        onColor = Color(0xFF431407),
        colorContainer = Color(0xFF7C2D12),
        onColorContainer = Color(0xFFFFEDD5)
    )

    fun warning(mode: SplitMateThemeMode = activeMode): HubColorRoles = when (mode) {
        SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> WarningEspresso
        SplitMateThemeMode.SUNLIT_BUCKWHEAT, SplitMateThemeMode.KYOTO_MATCHA_YUZU -> WarningLight
    }

    // --------------------------------------------------------------------------
    // Quick Expense participant avatar hues (6 per theme)
    // --------------------------------------------------------------------------

    private val ParticipantSunlit = listOf(
        HubColorRoles(Color(0xFF416913), Color(0xFFFFFFFF), Color(0xFFD7E8B6), Color(0xFF23201E)),
        HubColorRoles(Color(0xFF9A3412), Color(0xFFFFFFFF), Color(0xFFFFD8CC), Color(0xFF8A2E1A)),
        HubColorRoles(Color(0xFF1D4ED8), Color(0xFFFFFFFF), Color(0xFFD0E2FF), Color(0xFF143E82)),
        HubColorRoles(Color(0xFFBE185D), Color(0xFFFFFFFF), Color(0xFFFFD5E5), Color(0xFF801844)),
        HubColorRoles(Color(0xFF6D28D9), Color(0xFFFFFFFF), Color(0xFFE5DCFF), Color(0xFF452285)),
        HubColorRoles(Color(0xFF15803D), Color(0xFFFFFFFF), Color(0xFFD2F5DC), Color(0xFF1B6331))
    )
    private val ParticipantEspresso = listOf(
        HubColorRoles(Color(0xFFA3E635), Color(0xFF1A2E05), Color(0xFF2F4417), Color(0xFFD7E8B6)),
        HubColorRoles(Color(0xFFFB923C), Color(0xFF431A08), Color(0xFF5A2618), Color(0xFFFFD8CC)),
        HubColorRoles(Color(0xFF93C5FD), Color(0xFF0B2550), Color(0xFF1E3A6E), Color(0xFFD0E2FF)),
        HubColorRoles(Color(0xFFF9A8D4), Color(0xFF500724), Color(0xFF5C1835), Color(0xFFFFD5E5)),
        HubColorRoles(Color(0xFFC4B5FD), Color(0xFF2E1065), Color(0xFF3B2470), Color(0xFFE5DCFF)),
        HubColorRoles(Color(0xFF86EFAC), Color(0xFF052E16), Color(0xFF1D4A2C), Color(0xFFD2F5DC))
    )
    private val ParticipantKyoto = listOf(
        // Yuzu instead of sage so the first avatar does not melt into the matcha canvas.
        HubColorRoles(Color(0xFFB45309), Color(0xFFFFFFFF), Color(0xFFFEF08A), Color(0xFF451A03)),
        HubColorRoles(Color(0xFF9A3412), Color(0xFFFFFFFF), Color(0xFFFFD8CC), Color(0xFF8A2E1A)),
        HubColorRoles(Color(0xFF1D4ED8), Color(0xFFFFFFFF), Color(0xFFD0E2FF), Color(0xFF143E82)),
        HubColorRoles(Color(0xFFBE185D), Color(0xFFFFFFFF), Color(0xFFFFD5E5), Color(0xFF801844)),
        HubColorRoles(Color(0xFF6D28D9), Color(0xFFFFFFFF), Color(0xFFE5DCFF), Color(0xFF452285)),
        HubColorRoles(Color(0xFF15803D), Color(0xFFFFFFFF), Color(0xFFD2F5DC), Color(0xFF1B6331))
    )

    fun participantPalette(mode: SplitMateThemeMode = activeMode): List<HubColorRoles> = when (mode) {
        SplitMateThemeMode.SUNLIT_BUCKWHEAT -> ParticipantSunlit
        SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> ParticipantEspresso
        SplitMateThemeMode.KYOTO_MATCHA_YUZU -> ParticipantKyoto
    }

    // --------------------------------------------------------------------------
    // Expense category badge hues
    // --------------------------------------------------------------------------

    private val CategoryLight: Map<HubCategoryHue, HubColorRoles> = mapOf(
        HubCategoryHue.SAGE to HubColorRoles(Color(0xFF365314), Color(0xFFFFFFFF), Color(0xFFDCE9B9), Color(0xFF365314)),
        HubCategoryHue.PERIWINKLE to HubColorRoles(Color(0xFF3730A3), Color(0xFFFFFFFF), Color(0xFFE0E7FF), Color(0xFF3730A3)),
        HubCategoryHue.AMBER to HubColorRoles(Color(0xFF92400E), Color(0xFFFFFFFF), Color(0xFFFEF3C7), Color(0xFF92400E)),
        HubCategoryHue.MINT to HubColorRoles(Color(0xFF065F46), Color(0xFFFFFFFF), Color(0xFFD1FAE5), Color(0xFF065F46)),
        HubCategoryHue.ROSE to HubColorRoles(Color(0xFF9D174D), Color(0xFFFFFFFF), Color(0xFFFCE7F3), Color(0xFF9D174D)),
        HubCategoryHue.PEACH to HubColorRoles(Color(0xFF7C2D12), Color(0xFFFFFFFF), Color(0xFFFCE3D7), Color(0xFF7C2D12))
    )
    private val CategoryEspresso: Map<HubCategoryHue, HubColorRoles> = mapOf(
        HubCategoryHue.SAGE to HubColorRoles(Color(0xFFD7E8B6), Color(0xFF283A18), Color(0xFF283A18), Color(0xFFD7E8B6)),
        HubCategoryHue.PERIWINKLE to HubColorRoles(Color(0xFFC7D2FE), Color(0xFF222A4A), Color(0xFF222A4A), Color(0xFFC7D2FE)),
        HubCategoryHue.AMBER to HubColorRoles(Color(0xFFFDE68A), Color(0xFF3D2E14), Color(0xFF3D2E14), Color(0xFFFDE68A)),
        HubCategoryHue.MINT to HubColorRoles(Color(0xFFA7F3D0), Color(0xFF1F3833), Color(0xFF1F3833), Color(0xFFA7F3D0)),
        HubCategoryHue.ROSE to HubColorRoles(Color(0xFFFBCFE8), Color(0xFF3B1D2E), Color(0xFF3B1D2E), Color(0xFFFBCFE8)),
        HubCategoryHue.PEACH to HubColorRoles(Color(0xFFFED8C8), Color(0xFF3D231B), Color(0xFF3D231B), Color(0xFFFED8C8))
    )
    private val CategoryKyoto: Map<HubCategoryHue, HubColorRoles> = CategoryLight + mapOf(
        // Matcha canvas is already sage-green; lift the sage badge to a fresher leaf tone.
        HubCategoryHue.SAGE to HubColorRoles(Color(0xFF0F6B3E), Color(0xFFFFFFFF), Color(0xFFD9F5C5), Color(0xFF14532D))
    )

    fun categoryHue(hue: HubCategoryHue, mode: SplitMateThemeMode = activeMode): HubColorRoles = when (mode) {
        SplitMateThemeMode.SUNLIT_BUCKWHEAT -> CategoryLight.getValue(hue)
        SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> CategoryEspresso.getValue(hue)
        SplitMateThemeMode.KYOTO_MATCHA_YUZU -> CategoryKyoto.getValue(hue)
    }

    // --------------------------------------------------------------------------
    // Train pass (deep forest)
    // --------------------------------------------------------------------------

    private val TrainSunlit = HubTrainPassColors(
        forestTop = Color(0xFF2D4F12),
        forestBottom = Color(0xFF213B0C),
        onPass = Color(0xFFFFFFFF),
        accentLime = Color(0xFFB5DC86),
        secondarySage = Color(0xFFD7E8B6),
        nextUpPillBg = Color(0xFF84A950),
        nextUpPillText = Color(0xFF132604),
        berthCellBg = Color(0xFF223D0D),
        berthCellBorder = Color(0xFF3E651E),
        primaryCtaBg = Color(0xFF80AD47),
        primaryCtaText = Color(0xFF132604),
        promoContainer = Color(0xFF264010),
        promoBorder = Color(0xFF4A7325),
        promoSubtitle = Color(0xFFC5D6A7),
        promoPill = Color(0xFFD7E8B6),
        onPromoPill = Color(0xFF1B2E0B)
    )
    private val TrainEspresso = TrainSunlit.copy(
        forestTop = Color(0xFF264210),
        forestBottom = Color(0xFF1B3009),
        nextUpPillBg = Color(0xFF759943),
        nextUpPillText = Color(0xFF0F1F03),
        berthCellBg = Color(0xFF1A300A),
        berthCellBorder = Color(0xFF345718),
        primaryCtaBg = Color(0xFF8BB85A)
    )
    private val TrainKyoto = TrainSunlit.copy(
        forestTop = Color(0xFF1E3F24),
        forestBottom = Color(0xFF142E19),
        promoContainer = Color(0xFF1E3F24),
        promoBorder = Color(0xFF3F6638)
    )

    fun trainPass(mode: SplitMateThemeMode = activeMode): HubTrainPassColors = when (mode) {
        SplitMateThemeMode.SUNLIT_BUCKWHEAT -> TrainSunlit
        SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> TrainEspresso
        SplitMateThemeMode.KYOTO_MATCHA_YUZU -> TrainKyoto
    }

    // --------------------------------------------------------------------------
    // Flight pass (aviation navy)
    // --------------------------------------------------------------------------

    private val FlightSunlit = HubFlightPassColors(
        navyTop = Color(0xFF2B2768),
        navyBottom = Color(0xFF1B1849),
        onPass = Color(0xFFFFFFFF),
        secondaryLavender = Color(0xFFDCE3FD),
        accentPeriwinkle = Color(0xFFC7D2FE)
    )
    private val FlightEspresso = FlightSunlit.copy(
        navyTop = Color(0xFF242059),
        navyBottom = Color(0xFF16133B)
    )
    private val FlightKyoto = FlightSunlit.copy(
        navyTop = Color(0xFF23383B),
        navyBottom = Color(0xFF162527)
    )

    fun flightPass(mode: SplitMateThemeMode = activeMode): HubFlightPassColors = when (mode) {
        SplitMateThemeMode.SUNLIT_BUCKWHEAT -> FlightSunlit
        SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> FlightEspresso
        SplitMateThemeMode.KYOTO_MATCHA_YUZU -> FlightKyoto
    }
}
