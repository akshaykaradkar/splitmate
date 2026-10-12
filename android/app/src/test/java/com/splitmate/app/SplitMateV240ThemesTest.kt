package com.splitmate.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.splitmate.app.ui.KyotoMatchaYuzuPalette
import com.splitmate.app.ui.SplitMateThemeMode
import com.splitmate.app.ui.WarmEspressoNightPalette
import com.splitmate.app.ui.theme.HubExtendedColors
import com.splitmate.app.ui.theme.appShellExtendedFor
import com.splitmate.app.ui.theme.transitExtendedColorsFor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * v2.4.0 D6 themes: Jaipur Rani (light, persisted id KYOTO_MATCHA_YUZU) and Obsidian Gold
 * (dark, persisted id WARM_ESPRESSO_NIGHT).
 */
class SplitMateV240ThemesTest {

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /** HSV hue in degrees. */
    private fun hue(c: Color): Float {
        val hsv = FloatArray(3)
        val argb = c.toArgb()
        val r = (argb shr 16 and 0xFF) / 255f
        val g = (argb shr 8 and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val d = max - min
        if (d == 0f) return 0f
        val h = when (max) {
            r -> 60f * (((g - b) / d) % 6f)
            g -> 60f * (((b - r) / d) + 2f)
            else -> 60f * (((r - g) / d) + 4f)
        }
        hsv[0] = if (h < 0) h + 360f else h
        return hsv[0]
    }

    private fun hueDistance(a: Color, b: Color): Float {
        val d = kotlin.math.abs(hue(a) - hue(b))
        return minOf(d, 360f - d)
    }

    private fun isGreen(c: Color): Boolean = hue(c) in 75f..165f

    @Test
    fun `D6_01 persisted ids never change and old names still resolve`() {
        assertEquals("WARM_ESPRESSO_NIGHT", SplitMateThemeMode.WARM_ESPRESSO_NIGHT.id)
        assertEquals("KYOTO_MATCHA_YUZU", SplitMateThemeMode.KYOTO_MATCHA_YUZU.id)
        assertEquals("Obsidian Gold", SplitMateThemeMode.WARM_ESPRESSO_NIGHT.displayName)
        assertEquals("Jaipur Rani", SplitMateThemeMode.KYOTO_MATCHA_YUZU.displayName)
        assertEquals(SplitMateThemeMode.WARM_ESPRESSO_NIGHT, SplitMateThemeMode.fromId("Warm Espresso Night"))
        assertEquals(SplitMateThemeMode.KYOTO_MATCHA_YUZU, SplitMateThemeMode.fromId("kyoto matcha & yuzu"))
        assertEquals(SplitMateThemeMode.WARM_ESPRESSO_NIGHT, SplitMateThemeMode.fromId("obsidian gold"))
        assertEquals(SplitMateThemeMode.KYOTO_MATCHA_YUZU, SplitMateThemeMode.fromId("Jaipur Rani"))
    }

    @Test
    fun `D6_02 gets back and owes read apart in both new themes`() {
        listOf(WarmEspressoNightPalette, KyotoMatchaYuzuPalette).forEach { p ->
            assertTrue(hueDistance(p.primary, p.secondary) >= 60f, "${p.mode} primary vs secondary hue too close")
        }
        // Obsidian: gold "gets back" is warm, rose "owes" is cool-leaning (pink side of red).
        assertTrue(hue(WarmEspressoNightPalette.primary) in 35f..55f, "Obsidian primary should be gold")
        assertTrue(hue(WarmEspressoNightPalette.secondary) >= 320f, "Obsidian secondary should be rose")
    }

    @Test
    fun `D6_03 Obsidian large fills are calm graphite, not saturated dark gold`() {
        val c = WarmEspressoNightPalette.primaryContainer
        val argb = c.toArgb()
        val r = argb shr 16 and 0xFF
        val g = argb shr 8 and 0xFF
        val b = argb and 0xFF
        assertTrue(maxOf(r, g, b) - minOf(r, g, b) <= 16, "primaryContainer must be low-chroma graphite")
        assertTrue(contrast(c, WarmEspressoNightPalette.surfaceContainerLow) >= 1.15, "card must separate from canvas")
        assertTrue(contrast(WarmEspressoNightPalette.primary, c) >= 3.0, "gold accent border must be visible on the card")
        assertTrue(contrast(WarmEspressoNightPalette.onPrimaryContainer, c) >= 4.5)
    }

    @Test
    fun `D6_04 train cards and Link phone button follow the theme, not Buckwheat green`() {
        listOf(SplitMateThemeMode.WARM_ESPRESSO_NIGHT, SplitMateThemeMode.KYOTO_MATCHA_YUZU).forEach { mode ->
            val transit = transitExtendedColorsFor(mode)
            listOf(transit.trainTicket, transit.trainDeck, transit.trainHub).forEach { pass ->
                assertTrue(!isGreen(pass.color) && !isGreen(pass.colorDeep), "$mode train pass is still green")
                assertTrue(contrast(pass.onColor, pass.color) >= 4.5, "$mode train headline ink")
                assertTrue(contrast(pass.onContainer, pass.container) >= 4.5, "$mode train pill text")
            }
            val hub = HubExtendedColors.trainPass(mode)
            assertTrue(!isGreen(hub.forestTop), "$mode hub train card is still green")
            assertTrue(contrast(hub.primaryCtaText, hub.primaryCtaBg) >= 4.5)

            val sage = appShellExtendedFor(mode).sage
            assertTrue(!isGreen(sage.color), "$mode Link phone button is still green")
            assertTrue(contrast(mode.toPaletteForTest().surfaceContainerLow, sage.color) >= 4.5, "$mode Link button text")
        }
    }

    @Test
    fun `D6_05 Jaipur train and flight passes stay distinct`() {
        val t = transitExtendedColorsFor(SplitMateThemeMode.KYOTO_MATCHA_YUZU)
        assertTrue(hueDistance(t.trainDeck.color, t.flight.color) >= 50f)
    }

    private fun SplitMateThemeMode.toPaletteForTest() = when (this) {
        SplitMateThemeMode.WARM_ESPRESSO_NIGHT -> WarmEspressoNightPalette
        SplitMateThemeMode.KYOTO_MATCHA_YUZU -> KyotoMatchaYuzuPalette
        SplitMateThemeMode.SUNLIT_BUCKWHEAT -> com.splitmate.app.ui.SunlitBuckwheatPalette
    }
}
