package com.splitmate.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.splitmate.app.ui.SplitMateExpressivePalette
import com.splitmate.app.ui.SplitMateThemeMode
import com.splitmate.app.ui.toPalette
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * v2.3.6 Wave 2: M3 colour-role compliance for all three themes, checked against the live
 * m3.material.io rules (Styles > Color > Roles, Foundations > Accessibility > Color contrast):
 *  - text/icon pairs >= 4.5:1
 *  - `outline` = important boundary, >= 3:1 against EVERY surface container
 *  - `outlineVariant` = decorative card border / divider (never `outline` for cards)
 *  - containers must not equal the on-colour of their accent (Espresso regression)
 *  - error / inverse / scrim roles exist and are mapped into the Material ColorScheme
 * Iterates every [SplitMateThemeMode] so a newly added theme is checked automatically.
 */
class SplitMateV236ThemeRolesTest {

    private val palettes: List<SplitMateExpressivePalette> = SplitMateThemeMode.values().map { it.toPalette() }

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun SplitMateExpressivePalette.surfaces() = listOf(
        surfaceContainerLowest, surfaceContainerLow, surfaceContainer, surfaceContainerHigh, surfaceContainerHighest
    )

    @Test
    fun `W2_01 onSurface and onSurfaceVariant reach 4_5 to 1 on every surface container`() {
        palettes.forEach { p ->
            p.surfaces().forEach { s ->
                assertTrue(contrast(p.onSurface, s) >= 4.5, "${p.mode} onSurface on $s")
                assertTrue(contrast(p.onSurfaceVariant, s) >= 4.5, "${p.mode} onSurfaceVariant on $s = ${contrast(p.onSurfaceVariant, s)}")
            }
        }
    }

    @Test
    fun `W2_02 outline is a real 3 to 1 boundary on every surface container`() {
        palettes.forEach { p ->
            p.surfaces().forEach { s ->
                assertTrue(contrast(p.outline, s) >= 3.0, "${p.mode} outline on $s = ${contrast(p.outline, s)}")
            }
        }
    }

    @Test
    fun `W2_03 container and on-container pairs reach 4_5 to 1`() {
        palettes.forEach { p ->
            listOf(
                p.onPrimaryContainer to p.primaryContainer,
                p.onSecondaryContainer to p.secondaryContainer,
                p.onTertiaryContainer to p.tertiaryContainer,
                p.onErrorContainer to p.errorContainer,
                p.onError to p.error,
                p.inverseOnSurface to p.inverseSurface
            ).forEach { (fg, bg) ->
                assertTrue(contrast(fg, bg) >= 4.5, "${p.mode} $fg on $bg = ${contrast(fg, bg)}")
            }
            assertTrue(contrast(p.inversePrimary, p.inverseSurface) >= 3.0, "${p.mode} inversePrimary on inverseSurface")
        }
    }

    @Test
    fun `W2_04 accent containers never collapse onto their on-colour`() {
        palettes.forEach { p ->
            assertNotEquals(p.onSecondary, p.secondaryContainer, "${p.mode} secondaryContainer == onSecondary")
            assertNotEquals(p.onTertiary, p.tertiaryContainer, "${p.mode} tertiaryContainer == onTertiary")
            assertNotEquals(p.onPrimary, p.primaryContainer, "${p.mode} primaryContainer == onPrimary")
        }
    }

    @Test
    fun `W2_05 error text stays readable on every surface`() {
        palettes.forEach { p ->
            p.surfaces().forEach { s ->
                assertTrue(contrast(p.error, s) >= 4.5, "${p.mode} error on $s = ${contrast(p.error, s)}")
            }
        }
    }

    @Test
    fun `W2_06 outline and outlineVariant are distinct roles`() {
        palettes.forEach { p -> assertNotEquals(p.outline, p.outlineVariant, "${p.mode}") }
    }

    @Test
    fun `W2_07 Sunlit Buckwheat keeps the Stitch card border and sage container`() {
        val p = SplitMateThemeMode.SUNLIT_BUCKWHEAT.toPalette()
        assertEquals(Color(0xFFEDE7DF), p.outlineVariant)
        assertEquals(Color(0xFFD7E8B6), p.primaryContainer)
    }

    @Test
    fun `W2_08 ColorScheme maps error inverse scrim and animated outlineVariant`() {
        val src = File("src/main/java/com/splitmate/app/ui/SplitMateTheme.kt").readText()
        listOf(
            "error = targetPalette.error", "onError = targetPalette.onError",
            "errorContainer = targetPalette.errorContainer", "onErrorContainer = targetPalette.onErrorContainer",
            "inverseSurface = targetPalette.inverseSurface", "inverseOnSurface = targetPalette.inverseOnSurface",
            "inversePrimary = targetPalette.inversePrimary", "scrim = targetPalette.scrim",
            "outlineVariant = animOutlineVariant"
        ).forEach { assertTrue(src.contains(it), "missing mapping: $it") }
    }

    @Test
    fun `W2_09 Buckwheat aliases resolve from the active palette in every theme`() {
        val src = File("src/main/java/com/splitmate/app/ui/SplitMateTheme.kt").readText()
        val aliasBlock = src.substringAfter("val BuckwheatCanvas: Color").substringBefore("private val SplitMateLightColorScheme")
        assertTrue(!aliasBlock.contains("Color(0x"), "Buckwheat aliases must not hardcode hex values")
        assertTrue(!aliasBlock.contains("KYOTO_MATCHA_YUZU"), "Buckwheat aliases must not special-case one theme")
    }

    @Test
    fun `W2_10 card-border tokens use outlineVariant, never outline`() {
        val root = File("src/main/java/com/splitmate/app/ui")
        val offenders = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, line ->
                // Only the M3 palette/colour-scheme `outline` role is restricted; extended colour groups
                // (e.g. `Extended.trainPass.outline`) carry their own decorative border colour.
                val role = """(?:[Pp]alette|colorScheme)\.outline\b(?!Variant)"""
                val isBorderToken = Regex("""(CardBorder|Border|Divider)\b.*get\(\)\s*=.*$role""").containsMatchIn(line) ||
                    Regex("""BorderStroke\([^)]*$role""").containsMatchIn(line)
                if (isBorderToken) "${f.name}:${i + 1}" else null
            }
        }.toList()
        assertTrue(offenders.isEmpty(), "border tokens using outline: $offenders")
    }
}
