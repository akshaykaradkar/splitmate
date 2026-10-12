package com.splitmate.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** v2.4.0 rc2: fixes from on-device review (toolbar shadow, Mark paid shape, Settings, visual sweep). */
class SplitMateV240Rc2Test {

    private fun src(path: String) = File("src/main/java/com/splitmate/app/$path").readText()

    @Test
    fun `RC2_01 home toolbar is the official M3 Expressive floating toolbar with nothing clipping its shadow`() {
        val app = src("ui/SplitMateAppComposable.kt")
        val bar = app.substringAfter("fun SplitMateBottomNavigationBar(").substringBefore("\n// ====")
        assertTrue(bar.contains("androidx.compose.material3.HorizontalFloatingToolbar("))
        assertTrue(bar.contains("androidx.compose.material3.FloatingToolbarDefaults.VibrantFloatingActionButton("))
        assertFalse(bar.contains(".shadow("), "the official toolbar owns its shadow")
        val wiring = app.substringAfter("bottomBar = {").substringBefore("SplitMateBottomNavigationBar(")
        assertFalse(wiring.contains("animateContentSize"), "animateContentSize clips the toolbar shadow")
        assertFalse(wiring.contains("fadeOut("), "a fade layer clips the shadow to a rectangle")
    }

    @Test
    fun `RC2_02 Mark paid is one pill everywhere, never a split button`() {
        val app = src("ui/SplitMateAppComposable.kt")
        val trip = src("ui/screens/TripHomeScreen.kt")
        listOf(app, trip).forEach { s ->
            assertFalse(Regex("SplitButtonLayout\\(\\s*leadingText = \"Mark paid\"").containsMatchIn(s))
        }
        assertEquals(4, Regex("MarkPaidButton\\(").findAll(app + trip).count(), "Settle in/out, Money mine, Money organizer")
        val button = src("ui/components/MarkPaidButton.kt")
        assertTrue(button.contains("shapes = ButtonDefaults.shapes()"), "official press morph")
        assertTrue(button.contains("onClick = onRemind,") && button.contains("shapes = IconButtonDefaults.shapes()"))
        assertTrue(button.contains("containerColor = MaterialTheme.colorScheme.primaryContainer"), "remind is sage, not peach")
        // New group keeps the official split button: its menu has real choices.
        assertTrue(app.contains("leadingText = \"New group\""))
    }

    @Test
    fun `RC2_03 Settings shows no hex codes, plain words and the real version`() {
        val settings = src("ui/screens/OnboardingAndSettingsScreens.kt")
        assertFalse(Regex("#[0-9A-Fa-f]{6} ·").containsMatchIn(settings), "no colour codes shown to people")
        assertFalse(settings.contains("paletteHexSummary"))
        assertFalse(settings.contains("v2.3.1"), "version comes from the installed package")
        assertTrue(settings.contains("context.packageManager.getPackageInfo(context.packageName, 0).versionName"))
        listOf("Expressive Theme Studio", "Curated tactile", "Dark Mode (Warm Espresso)", "Tactile Keypad Haptics", "Reset App Data")
            .forEach { assertFalse(Regex("\"[^\"\n]*" + Regex.escape(it)).containsMatchIn(settings), it) }
        assertTrue(src("ui/SplitMateAppComposable.kt").contains("onClickLabel = \"Open settings\""))
        assertTrue(Regex("ConnectedButtonGroup\\(\\s*options = themeModes").find(settings) == null, "one theme picker, not two")
    }

    @Test
    fun `RC2_04 visual sweep fixes stay fixed`() {
        val trip = src("ui/screens/TripHomeScreen.kt")
        assertFalse(trip.contains("segmentedIslandItemShape("), "separate cards 12dp apart use uniform corners")
        assertTrue(trip.contains("\"travelers\""), "sentence case: 4 travelers")
        val deck = src("ui/components/AnimatedTransitDeckHeroCard.kt")
        assertTrue(deck.contains("private val BackCardPeek = 46.dp"), "back card header is never sliced")
        val form = src("ui/screens/QuickExpenseAndGuideScreens.kt")
        val logSplit = form.substringAfter("val canCommitSplit =").substringBefore("Text(\n")
        assertFalse(Regex("shadowElevation = [1-9]").containsMatchIn(logSplit), "Log & split key is flat")
        val app = src("ui/SplitMateAppComposable.kt")
        assertTrue(app.contains("contentPadding = PaddingValues(top = 12.dp, bottom = 160.dp)"), "Settle list clears the toolbar")
    }
}
