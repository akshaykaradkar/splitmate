package com.splitmate.app

import com.splitmate.app.ui.a11y.SpokenMoney
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.4.0 D2 accessibility basics. */
class SplitMateV240AccessibilityTest {

    private fun src(path: String) =
        java.io.File("src/main/java/com/splitmate/app/$path").readText()

    @Test
    fun `spoken rupees reads like a person would say it`() {
        assertEquals("14 rupees 17 paise", SpokenMoney.rupees(1417))
        assertEquals("1 rupee 1 paisa", SpokenMoney.rupees(101))
        assertEquals("3000 rupees", SpokenMoney.rupees(300_000))
        assertEquals("50 paise", SpokenMoney.rupees(50))
        assertEquals("0 rupees", SpokenMoney.rupees(0))
        assertEquals("14 rupees 17 paise", SpokenMoney.rupees(-1417))
    }

    @Test
    fun `settlement sentence speaks from the user's point of view`() {
        assertEquals("Sam pays you 14 rupees 17 paise", SpokenMoney.settlementSentence("Sam", "Akshay", 1417, false, true))
        assertEquals("You pay Priya 5 rupees", SpokenMoney.settlementSentence("Akshay", "Priya", 500, true, false))
        assertEquals("Sam pays Priya 5 rupees", SpokenMoney.settlementSentence("Sam", "Priya", 500, false, false))
    }

    @Test
    fun `infinite loops respect reduced motion`() {
        listOf("ui/components/AnimatedTransitDeckHeroCard.kt", "ui/SplitMateAppComposable.kt", "ui/SplitMateTheme.kt").forEach { f ->
            val s = src(f)
            val loops = Regex("""rememberInfiniteTransition\(label""").findAll(s).count()
            val guards = Regex("""val reduceMotionForLoops = """).findAll(s).count()
            assertEquals(loops, guards, "$f: every rememberInfiniteTransition needs a reduced-motion guard")
        }
    }

    @Test
    fun `no tiny text below 11sp`() {
        val tiny = Regex("""fontSize\s*=\s*(\d+(?:\.\d+)?)\.sp""")
        java.io.File("src/main/java/com/splitmate/app/ui").walkTopDown().filter { it.extension == "kt" }.forEach { f ->
            tiny.findAll(f.readText()).forEach { m ->
                assertTrue(m.groupValues[1].toDouble() >= 11.0, "${f.name}: ${m.value} is below 11sp")
            }
        }
    }

    @Test
    fun `status banners are announced`() {
        assertTrue(src("ui/screens/TripHomeScreen.kt").contains("liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite"))
        assertTrue(src("ui/SplitMateAppComposable.kt").contains("liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite"))
    }
}
