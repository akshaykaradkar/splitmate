package com.splitmate.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.splitmate.app.ui.WarmEspressoNightPalette
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** v2.4.0 rc3: the open issues found after rc2 (dark cards, avatars, pills, styles, Leave trip, chips). */
class SplitMateV240Rc3Test {

    private fun src(path: String) = File("src/main/java/com/splitmate/app/$path").readText()
    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble(); val lb = b.luminance().toDouble()
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Test
    fun `RC3_01 dark cards sit above the canvas, never darker`() {
        val p = WarmEspressoNightPalette
        assertTrue(p.surfaceContainerLowest.luminance() > p.surfaceContainerLow.luminance(), "card lighter than canvas")
        assertTrue(contrast(p.surfaceContainerLowest, p.surfaceContainerLow) >= 1.1, "visible step")
        assertTrue(contrast(p.onSurface, p.surfaceContainerLowest) >= 10.0, "body text stays AAA on cards")
        val shell = src("ui/theme/AppShellExtendedColors.kt")
            .substringAfter("val WarmEspressoNightAppShellExtendedColors").substringBefore("heroGradientStart")
        assertFalse(shell.contains("0xFF0E0E11"), "settle boards use the card tone, not a hole")
    }

    @Test
    fun `RC3_02 settle From and To rows show the person, not a group icon`() {
        val app = src("ui/SplitMateAppComposable.kt")
        assertTrue(app.contains("initials = fromRoomMember?.avatarSeed ?: leg.counterpartyName"))
        assertTrue(app.contains("initials = toRoomMember?.avatarSeed ?: leg.counterpartyName"))
    }

    @Test
    fun `RC3_04 no grey pills that look like disabled buttons`() {
        val app = src("ui/SplitMateAppComposable.kt")
        assertFalse(app.contains("\"Priority view\""))
        val trip = src("ui/screens/TripHomeScreen.kt")
        assertFalse(trip.contains("color = if (myTransfers.isEmpty()) TripHubTokens.PositiveSagePillBg else TripHubTokens.SunkenWell"))
        val life = src("ui/screens/TripLifecycleUi.kt")
        assertTrue(life.contains("androidx.compose.material3.AssistChip("), "Wrap it up is an official assist chip")
    }

    @Test
    fun `RC3_05 one style per action`() {
        val app = src("ui/SplitMateAppComposable.kt")
        val hero = app.substringAfter("if (totalBalance != \"₹0.00\") {").substringBefore("} else {")
        assertTrue(hero.contains("Icons.Rounded.SwapHoriz") && hero.contains("shapes = ButtonDefaults.shapes()"))
        assertFalse(hero.contains("ArrowForward"), "no trailing arrow on Settle up")
        assertTrue(src("ui/screens/TripHomeScreen.kt").contains("imageVector = Icons.Outlined.Info"))
        val form = src("ui/screens/QuickExpenseAndGuideScreens.kt")
        assertFalse(form.contains("val selectedBg = if (isDark) activePalette.primary else activePalette.onSurface"), "selected chips are sage, like Paid by")
        assertFalse(form.contains("targetValue = if (isChosen) 10.dp else 20.dp"), "no squarer selected chip")
    }

    @Test
    fun `RC3_06 Leave trip is quiet and the invite button is an official icon button`() {
        val trip = src("ui/screens/TripHomeScreen.kt")
        val footer = trip.substringAfter("item(key = \"people_leave_trip_footer\")").substringBefore("/**")
        assertTrue(footer.contains("TextButton(") && footer.contains("MaterialTheme.colorScheme.error"))
        assertFalse(footer.contains("fillMaxWidth()\n                    .defaultMinSize"), "not a full-width button")
        assertTrue(trip.contains("imageVector = if (isMissingPhone) Icons.Rounded.AddIcCall"))
    }

    @Test
    fun `RC3_07 chip rows scroll to the card edge`() {
        val form = src("ui/screens/QuickExpenseAndGuideScreens.kt")
        assertTrue(form.contains("private fun Modifier.bleedHorizontally("))
        assertTrue(Regex("bleedHorizontally\\(").findAll(form).count() >= 3)
    }
}
