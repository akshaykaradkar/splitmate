package com.splitmate.app

import androidx.compose.ui.text.font.FontWeight
import com.splitmate.app.ui.components.MaterialSymbolDefaults
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** v2.4.0 rc8: Screens 1-3 draw icons from Material Symbols Rounded; text stays Figtree. */
class SplitMateV240Rc8SymbolsTest {

    private fun src(path: String) = File("src/main/java/com/splitmate/app/$path").readText()

    @Test
    fun `RC8_01 icon weight follows the nearby Figtree text weight`() {
        assertEquals(500, MaterialSymbolDefaults.weightFor(FontWeight.Medium))
        assertEquals(600, MaterialSymbolDefaults.weightFor(FontWeight.SemiBold))
        assertEquals(700, MaterialSymbolDefaults.weightFor(FontWeight.Bold))
        assertEquals(700, MaterialSymbolDefaults.weightFor(FontWeight.ExtraBold), "wght axis caps at 700")
        assertEquals(200, MaterialSymbolDefaults.gradeFor(FontWeight.ExtraBold, darkSurface = false), "emphasis grade past the cap")
        assertEquals(-25, MaterialSymbolDefaults.gradeFor(FontWeight.SemiBold, darkSurface = true), "light-on-dark pulls back")
        assertEquals(0, MaterialSymbolDefaults.gradeFor(FontWeight.SemiBold, darkSurface = false))
        assertEquals(20f, MaterialSymbolDefaults.opticalSizeFor(13f))
        assertEquals(24f, MaterialSymbolDefaults.opticalSizeFor(24f))
        assertEquals(48f, MaterialSymbolDefaults.opticalSizeFor(64f))
    }

    @Test
    fun `RC8_02 every material icon used in the app has a bundled symbol`() {
        val map = src("ui/components/MaterialSymbolCodepoints.kt")
        val iconRef = Regex("Icons\\.(?:AutoMirrored\\.)?(?:Rounded|Outlined|Filled|Default)\\.([A-Za-z0-9_]+)")
        val used = File("src/main/java").walkTopDown().filter { it.extension == "kt" }
            .flatMap { f -> iconRef.findAll(f.readText()).map { it.groupValues[1] } }
            .toSet()
        val missing = used.filter { !map.contains("\"$it\" to 0x") }
        assertTrue(missing.isEmpty(), "icons without a symbol: $missing")
        val font = File("src/main/res/font/material_symbols_rounded.ttf")
        assertTrue(font.exists(), "subset icon font is bundled")
        assertTrue(font.length() < 600_000, "subset stays small (was ${font.length()} bytes)")
    }

    @Test
    fun `RC8_03 Screens 1-3 use SymbolIcon, other screens are untouched, text stays Figtree`() {
        val app = src("ui/SplitMateAppComposable.kt")
        val iconCall = Regex("\\bIcon\\((\\s*\\n\\s*)?(imageVector =|Icons\\.)")
        listOf(
            "fun SplitMateBottomNavigationBar(" to "fun LedgersDashboardScreen(",
            "fun LedgersDashboardScreen(" to "fun JoinGroupByCodeDialog(",
            "fun GreedySettlementScreen(" to "fun AuditVaultScreen("
        ).forEach { (start, end) ->
            val body = app.substringAfter(start).substringBefore(end)
            assertFalse(iconCall.containsMatchIn(body), "$start still has vector Icon calls")
            assertTrue(body.contains("SymbolIcon("), "$start uses SymbolIcon")
        }
        val trip = src("ui/screens/TripHomeScreen.kt")
        assertFalse(iconCall.containsMatchIn(trip), "Trip Hub still has vector Icon calls")
        // Selected nav tab: filled and as heavy as its ExtraBold label.
        val bar = app.substringAfter("fun SplitMateBottomNavigationBar(").substringBefore("fun LedgersDashboardScreen(")
        assertTrue(bar.contains("filled = isSelected"))
        assertTrue(app.contains("LocalSymbolTextWeight provides FontWeight.SemiBold"))
        // Figtree is still the text font.
        val theme = src("ui/SplitMateTheme.kt")
        assertTrue(theme.contains("R.font.figtree"), "text font is Figtree")
        assertFalse(theme.contains("R.font.material_symbols_rounded"), "icon font is never used for text")
    }
}
