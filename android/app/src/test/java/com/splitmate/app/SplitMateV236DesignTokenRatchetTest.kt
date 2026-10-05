package com.splitmate.app

import androidx.compose.ui.unit.sp
import com.splitmate.app.ui.SplitMateTypography
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * v2.3.6 Step A4: design-token "ratchet".
 *
 * Every UI file may contain at most the number of hand-written `fontSize = N.sp`, `tween(` and
 * `RoundedCornerShape(N.dp)` literals recorded in [BASELINE]. When a screen migrates to
 * `MaterialTheme.typography`, `MaterialTheme.motionScheme` and `MaterialTheme.shapes`, lower its
 * numbers here; they may never go up, and new files must start at zero.
 */
class SplitMateV236DesignTokenRatchetTest {

    private val patterns = listOf(
        "fontSize literal" to Regex("""fontSize\s*=\s*[0-9.]+\.sp"""),
        "tween(" to Regex("""\btween\("""),
        "RoundedCornerShape(N.dp)" to Regex("""RoundedCornerShape\(\s*[0-9.]+\.dp\s*\)""")
    )

    /** path relative to `ui/` -> (fontSize literals, tween calls, corner literals). */
    private val BASELINE: Map<String, IntArray> = mapOf(
        "Components.kt" to intArrayOf(0, 0, 1),
        "SplitMateAppComposable.kt" to intArrayOf(196, 2, 40),
        "category/ExpenseCategoryPicker.kt" to intArrayOf(17, 0, 6),
        "components/AnimatedTransitDeckHeroCard.kt" to intArrayOf(12, 4, 1),
        "components/ExpressiveM3Components.kt" to intArrayOf(5, 4, 0),
        "components/UpiExpressPaymentSheet.kt" to intArrayOf(22, 0, 11),
        "dialogs/GroupAndSettlementDialogs.kt" to intArrayOf(24, 0, 0),
        "dialogs/TripSyncAndPerspectiveSheet.kt" to intArrayOf(19, 0, 12),
        "navigation/SplitMateAppNavHost.kt" to intArrayOf(1, 0, 0),
        "screens/ActivityDetailSheet.kt" to intArrayOf(9, 0, 2),
        "screens/FlightExpenseReviewScreen.kt" to intArrayOf(54, 0, 7),
        "screens/OnboardingAndSettingsScreens.kt" to intArrayOf(30, 0, 9),
        "screens/PnrExpenseReviewScreen.kt" to intArrayOf(49, 0, 21),
        "screens/QuickExpenseAndGuideScreens.kt" to intArrayOf(50, 0, 11),
        "screens/TripHomeScreen.kt" to intArrayOf(103, 0, 47),
        "screens/TripHubExpenseManagement.kt" to intArrayOf(4, 0, 0),
        "screens/TripLifecycleUi.kt" to intArrayOf(13, 0, 3),
        "screens/plan/ExploreGuideView.kt" to intArrayOf(0, 0, 5),
        "screens/plan/PlanGuideComponents.kt" to intArrayOf(0, 0, 3),
        "screens/plan/StayPinSheet.kt" to intArrayOf(0, 0, 3),
        "share/SettleUpShareButton.kt" to intArrayOf(1, 0, 0)
    )

    private fun uiRoot(): File {
        val candidates = listOf(
            File("src/main/java/com/splitmate/app/ui"),
            File("app/src/main/java/com/splitmate/app/ui"),
            File("android/app/src/main/java/com/splitmate/app/ui")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("UI source root not found from ${File(".").absolutePath}")
    }

    @Test
    fun `DT_01 no UI file adds hand-written font size, tween or corner literals beyond its baseline`() {
        val root = uiRoot()
        val violations = mutableListOf<String>()
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "SplitMateTheme.kt" }
            .forEach { file ->
                val rel = file.relativeTo(root).invariantSeparatorsPath
                val text = file.readText()
                val limits = BASELINE[rel] ?: IntArray(patterns.size)
                patterns.forEachIndexed { i, (label, regex) ->
                    val count = regex.findAll(text).count()
                    if (count > limits[i]) {
                        violations += "$rel: $count x $label (baseline ${limits[i]}). " +
                            "Use MaterialTheme.typography / motionScheme / shapes instead."
                    }
                }
            }
        assertTrue(violations.isEmpty(), violations.joinToString("\n"))
    }

    @Test
    fun `DT_02 baseline only lists files that still exist`() {
        val root = uiRoot()
        val missing = BASELINE.keys.filterNot { File(root, it).isFile }
        assertTrue(missing.isEmpty(), "Remove stale ratchet entries: $missing")
    }

    @Test
    fun `DT_03 type scale follows the official Material 3 sizes`() {
        val t = SplitMateTypography
        val expected = listOf(
            t.displayLarge to 57, t.displayMedium to 45, t.displaySmall to 36,
            t.headlineLarge to 32, t.headlineMedium to 28, t.headlineSmall to 24,
            t.titleLarge to 22, t.titleMedium to 16, t.titleSmall to 14,
            t.bodyLarge to 16, t.bodyMedium to 14, t.bodySmall to 12,
            t.labelLarge to 14, t.labelMedium to 12, t.labelSmall to 11
        )
        expected.forEachIndexed { idx, (style, size) ->
            assertEquals(size.sp, style.fontSize, "Type role $idx")
        }
        // Hierarchy must be strictly ordered inside each group (catches titleLarge > headlineSmall bugs).
        assertTrue(t.headlineSmall.fontSize.value > t.titleLarge.fontSize.value)
        assertTrue(t.titleLarge.fontSize.value > t.titleMedium.fontSize.value)
    }
}
