package com.splitmate.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.splitmate.app.ui.SplitMateThemeMode
import com.splitmate.app.ui.theme.HubCategoryHue
import com.splitmate.app.ui.theme.HubExtendedColors
import com.splitmate.app.ui.theme.appShellExtendedFor
import com.splitmate.app.ui.theme.transitExtendedColorsFor
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * v2.3.6 Wave 3: hardcoded-colour migration guard.
 *  - No `Color(0x…)` / `Color.White` / `Color.Black` literal in UI code outside the palette file
 *    and the dedicated extended-colour files (M3 "static/custom colors").
 *  - Every extended colour group (color/onColor/container/onContainer) reaches 4.5:1 in every theme.
 * Iterates all [SplitMateThemeMode]s and walks the whole `ui/` tree, so new files/themes are covered.
 */
class SplitMateV236ColourMigrationTest {

    private val uiRoot = File("src/main/java/com/splitmate/app/ui")
    private val allowedFiles = setOf(
        "SplitMateTheme.kt",
        "AppShellExtendedColors.kt",
        "HubExtendedColors.kt",
        "TransitExtendedColors.kt"
    )

    private fun contrast(a: Color, b: Color): Double {
        val fg = if (a.alpha < 1f) a.compositeOver(b) else a
        val la = fg.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Test
    fun `W3_01 no hardcoded colour literals outside palette and extended-colour files`() {
        val literal = Regex("""Color\(0x[0-9A-Fa-f]+\)|Color\.(White|Black)\b""")
        val offenders = uiRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name !in allowedFiles }
            .flatMap { f ->
                f.readLines().mapIndexedNotNull { i, line ->
                    val code = line.substringBefore("//")
                    if (literal.containsMatchIn(code)) "${f.name}:${i + 1}" else null
                }
            }.toList()
        assertTrue(offenders.isEmpty(), "hardcoded colours found: $offenders")
    }

    @Test
    fun `W3_02 hub extended colour groups reach 4_5 to 1 in every theme`() {
        SplitMateThemeMode.values().forEach { mode ->
            val groups = buildList {
                add("presence" to HubExtendedColors.presence(mode))
                add("warning" to HubExtendedColors.warning(mode))
                HubExtendedColors.participantPalette(mode).forEachIndexed { i, g -> add("participant$i" to g) }
                HubCategoryHue.values().forEach { add("category_$it" to HubExtendedColors.categoryHue(it, mode)) }
            }
            groups.forEach { (name, g) ->
                val c = contrast(g.onColorContainer, g.colorContainer)
                assertTrue(c >= 4.5, "$mode $name onColorContainer/colorContainer = $c")
            }
        }
    }

    @Test
    fun `W3_03 app-shell extended colour groups reach 4_5 to 1 in every theme`() {
        SplitMateThemeMode.values().forEach { mode ->
            val x = appShellExtendedFor(mode)
            listOf(
                "sage" to x.sage, "terracotta" to x.terracotta, "online" to x.online,
                "trainPass" to x.trainPass, "flightPass" to x.flightPass, "flightChip" to x.flightChip,
                "flightTicket" to x.flightTicket, "settleSummary" to x.settleSummary,
                "settleReceive" to x.settleReceive, "settlePay" to x.settlePay
            ).forEach { (name, g) ->
                val c = contrast(g.onContainer, g.container)
                assertTrue(c >= 4.5, "$mode $name onContainer/container = $c")
            }
        }
    }

    @Test
    fun `W3_04 transit pass colours reach 4_5 to 1 in every theme`() {
        SplitMateThemeMode.values().forEach { mode ->
            val t = transitExtendedColorsFor(mode)
            listOf("trainTicket" to t.trainTicket, "trainDeck" to t.trainDeck, "flight" to t.flight).forEach { (name, p) ->
                val onColor = contrast(p.onColor, p.color)
                val onContainer = contrast(p.onContainer, p.container)
                assertTrue(onColor >= 4.5, "$mode $name onColor/color = $onColor")
                assertTrue(onContainer >= 4.5, "$mode $name onContainer/container = $onContainer")
            }
        }
    }

    @Test
    fun `W3_05 extended colours differ between light and dark themes`() {
        val sunlit = appShellExtendedFor(SplitMateThemeMode.SUNLIT_BUCKWHEAT)
        val espresso = appShellExtendedFor(SplitMateThemeMode.WARM_ESPRESSO_NIGHT)
        assertTrue(sunlit.sage.container != espresso.sage.container, "sage container identical in light and dark")
        assertTrue(
            HubExtendedColors.warning(SplitMateThemeMode.SUNLIT_BUCKWHEAT).colorContainer !=
                HubExtendedColors.warning(SplitMateThemeMode.WARM_ESPRESSO_NIGHT).colorContainer,
            "warning container identical in light and dark"
        )
    }
}
