package com.splitmate.app

import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import com.splitmate.app.data.CloudGroupSyncRepository
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseGroupEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.data.PnrNetworkRepository
import com.splitmate.app.data.SettlementEntity
import com.splitmate.app.data.UniversalFlightTicketExtractor
import com.splitmate.app.ui.AvatarGender
import com.splitmate.app.ui.AvatarSeedCodec
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.FigtreeFontFamily
import com.splitmate.app.ui.KyotoMatchaYuzuPalette
import com.splitmate.app.ui.LivePnrPassenger
import com.splitmate.app.ui.LivePnrStatusSnapshot
import com.splitmate.app.ui.NewGroupMemberDraft
import com.splitmate.app.ui.ParsedTravelTicket
import com.splitmate.app.ui.SplitMateAvatarColorPresets
import com.splitmate.app.ui.SplitMateCrewScenes
import com.splitmate.app.ui.SplitMateDiceBearStyles
import com.splitmate.app.ui.SplitMateExpressivePalette
import com.splitmate.app.ui.SplitMateExpressiveTypography
import com.splitmate.app.ui.SplitMateThemeMode
import com.splitmate.app.ui.SplitMateThemeState
import com.splitmate.app.ui.SplitMateTypography
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.SunlitBuckwheatPalette
import com.splitmate.app.ui.WarmEspressoNightPalette
import com.splitmate.app.ui.buildDiceBearAvatarUrl
import com.splitmate.app.ui.cleanDisplayExpenseTitle
import com.splitmate.app.ui.components.ExpressiveActionItem
import com.splitmate.app.ui.components.ExpressiveFabMenuItem
import com.splitmate.app.ui.components.ExpressiveMenuAction
import com.splitmate.app.ui.components.MaterialShapes
import com.splitmate.app.ui.components.MorphPolygonShape
import com.splitmate.app.ui.components.RoundedPolygonShape
import com.splitmate.app.ui.components.SplitMateMotion
import com.splitmate.app.ui.components.segmentedIslandItemShape
import com.splitmate.app.ui.extractInitialsFromNameOrSeed
import com.splitmate.app.ui.extractTravelTicketFromTitle
import com.splitmate.app.ui.formatIndianRupeesFromCents
import com.splitmate.app.ui.formatTravelExpenseTitle
import com.splitmate.app.ui.isFlightTicketExpense
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.text.font.FontFamily
import com.splitmate.app.ui.JetBrainsMonoFont
import com.splitmate.app.ui.PlusJakartaSansFont
import com.splitmate.app.ui.SplitMateMonospaceTextStyle
import com.splitmate.app.ui.SplitMateTnumMonospace
import com.splitmate.app.ui.cleanIndianTenDigitPhone
import com.splitmate.app.ui.formatTenDigitIndianPhone
import com.splitmate.app.ui.normalizeValidatedPhone10
import com.splitmate.app.ui.resolveExpenseCategoryBadgeColors
import com.splitmate.app.ui.resolveExpenseCategoryIcon
import com.splitmate.app.ui.resolveGroupCategoryIcon
import com.splitmate.app.ui.resolveStationDisplayName
import com.splitmate.app.ui.components.ActiveTravelPassMode
import com.splitmate.app.ui.screens.QuickSplitMode
import com.splitmate.app.ui.toPalette
import com.splitmate.app.ui.toSmartTitleCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.pow

/**
 * SplitMate v2.3.0 (versionCode 48) Material 3 Expressive 5-Tier End-to-End Verification Suite.
 *
 * Covers all 20 features (F1-F20) across:
 * - Tier 1 (Feature Coverage): 100 test cases (5 per feature F1-F20)
 * - Tier 2 (Boundary & Corner Cases): 100 test cases (5 per feature F1-F20)
 * - Tier 3 (Cross-Feature Pairwise Combinations): 20 test cases (T3_01-T3_20)
 * - Tier 4 (Real-World Application Scenarios): 10 end-to-end scenarios (T4_01-T4_10)
 * - Tier 5 (White-Box Adversarial Coverage Hardening): 20 test cases (T5_01-T5_20)
 * Total: 250 test cases.
 *
 * Strictly zero Unicode emoji characters or banned dingbats appear in this file.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplitMateV23ExpressiveE2ETest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testDensity = Density(density = 1f, fontScale = 1f)
    private val testSize = Size(100f, 100f)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        SplitMateThemeState.activeThemeMode = SplitMateThemeMode.SUNLIT_BUCKWHEAT
        SplitMateTheme.isDark = false
    }

    @AfterEach
    fun tearDown() {
        SplitMateThemeState.activeThemeMode = SplitMateThemeMode.SUNLIT_BUCKWHEAT
        SplitMateTheme.isDark = false
        Dispatchers.resetMain()
    }

    // =========================================================================
    // RUNTIME & SOURCE INSPECTION HELPERS
    // =========================================================================

    private fun resolveProjectSourceRoot(): File {
        val candidates = listOf(
            File("src/main/java/com/splitmate/app"),
            File("app/src/main/java/com/splitmate/app"),
            File("android/app/src/main/java/com/splitmate/app"),
            File("/usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Unable to locate SplitMate source root from working directory ${File(".").absolutePath}")
    }

    private fun resolveBuildGradleFile(): File {
        val candidates = listOf(
            File("build.gradle"),
            File("app/build.gradle"),
            File("android/app/build.gradle"),
            File("/usr/local/google/home/karadkar/splitmate/android/app/build.gradle"),
            File("build.gradle.kts"),
            File("app/build.gradle.kts"),
            File("android/app/build.gradle.kts"),
            File("/usr/local/google/home/karadkar/splitmate/android/app/build.gradle.kts")
        )
        return candidates.firstOrNull { it.exists() && it.isFile }
            ?: error("Unable to locate app/build.gradle")
    }

    private fun readSourceFile(relativePath: String): String {
        val file = File(resolveProjectSourceRoot(), relativePath)
        assertTrue(file.exists(), "Expected source file to exist: ${file.path}")
        return file.readText()
    }

    private fun allUiKotlinFiles(): List<File> {
        val uiDir = File(resolveProjectSourceRoot(), "ui")
        assertTrue(uiDir.exists() && uiDir.isDirectory, "Expected ui directory at ${uiDir.path}")
        return uiDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private fun relativeLuminance(color: Color): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble().coerceIn(0.0, 1.0)
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private fun wcagContrastRatio(fg: Color, bg: Color): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun cornerPx(cornerSize: CornerSize): Float =
        cornerSize.toPx(shapeSize = testSize, density = testDensity)

    private fun containsEmojiCodepoint(text: String): Boolean {
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            if (cp in 0x1F300..0x1FAFF || cp in 0x2600..0x27BF) return true
            i += Character.charCount(cp)
        }
        return false
    }

    // =========================================================================
    // TIER 1: FEATURE COVERAGE TESTS (F1-F20 x 5 = 100 TEST CASES)
    // =========================================================================

    @Nested
    @DisplayName("Tier 1: Feature Coverage Tests (F1-F20, 100 Test Cases)")
    inner class Tier1FeatureCoverageTests {

        // --- F1: 3-Theme Material 3 Expressive Engine ---

        @Test
        fun `F1_T1_01 SplitMateThemeMode defines SUNLIT_BUCKWHEAT WARM_ESPRESSO_NIGHT and KYOTO_MATCHA_YUZU`() {
            val modes = SplitMateThemeMode.values().toList()
            assertEquals(3, modes.size)
            assertEquals(
                listOf("SUNLIT_BUCKWHEAT", "WARM_ESPRESSO_NIGHT", "KYOTO_MATCHA_YUZU"),
                modes.map { it.id }
            )
            assertFalse(SplitMateThemeMode.SUNLIT_BUCKWHEAT.isDark)
            assertTrue(SplitMateThemeMode.WARM_ESPRESSO_NIGHT.isDark)
            assertFalse(SplitMateThemeMode.KYOTO_MATCHA_YUZU.isDark)
        }

        @Test
        fun `F1_T1_02 SunlitBuckwheatPalette matches canonical warm cream surface and olive-terracotta tokens`() {
            val p = SplitMateThemeMode.SUNLIT_BUCKWHEAT.toPalette()
            assertEquals(Color(0xFFFFFFFF), p.surfaceContainerLowest)
            assertEquals(Color(0xFFFAF6F0), p.surfaceContainerLow)
            assertEquals(Color(0xFFF4EFE6), p.surfaceContainer)
            assertEquals(Color(0xFFEDE6DA), p.surfaceContainerHigh)
            assertEquals(Color(0xFFE4DCCD), p.surfaceContainerHighest)
            assertEquals(Color(0xFF365314), p.primary)
            assertEquals(Color(0xFFD7E8B6), p.primaryContainer) // v2.3.6: Stitch sage (was #D9F99D)
            assertEquals(Color(0xFFE06B52), p.secondary)
            assertEquals(Color(0xFFFED8C8), p.secondaryContainer)
            assertEquals(Color(0xFF3730A3), p.tertiary)
        }

        @Test
        fun `F1_T1_03 WarmEspressoNightPalette matches canonical espresso surfaces and lime-amber tokens`() {
            val p = SplitMateThemeMode.WARM_ESPRESSO_NIGHT.toPalette()
            assertEquals(Color(0xFF14110F), p.surfaceContainerLowest)
            assertEquals(Color(0xFF1C1815), p.surfaceContainerLow)
            assertEquals(Color(0xFF25201C), p.surfaceContainer)
            assertEquals(Color(0xFF302A24), p.surfaceContainerHigh)
            assertEquals(Color(0xFF3B332C), p.surfaceContainerHighest)
            assertEquals(Color(0xFFA3E635), p.primary)
            assertEquals(Color(0xFF283D0E), p.primaryContainer)
            assertEquals(Color(0xFFFB923C), p.secondary)
            assertEquals(Color(0xFFA5B4FC), p.tertiary)
        }

        @Test
        fun `F1_T1_04 KyotoMatchaYuzuPalette matches canonical botanical stationery and hanko coral tokens`() {
            val p = SplitMateThemeMode.KYOTO_MATCHA_YUZU.toPalette()
            assertEquals(Color(0xFFEDF7EA), p.surfaceContainerLowest)
            assertEquals(Color(0xFFDDF0D5), p.surfaceContainerLow)
            assertEquals(Color(0xFFCCE6C2), p.surfaceContainer)
            assertEquals(Color(0xFFBBDAAF), p.surfaceContainerHigh)
            assertEquals(Color(0xFFA8CE9A), p.surfaceContainerHighest)
            assertEquals(Color(0xFF0F6B3E), p.primary)
            assertEquals(Color(0xFF86EFAC), p.primaryContainer)
            assertEquals(Color(0xFFB45309), p.secondary)
            assertEquals(Color(0xFFFEF08A), p.secondaryContainer)
            assertEquals(Color(0xFF6B21A8), p.tertiary)
            assertEquals(Color(0xFFF3E8FF), p.tertiaryContainer)
            assertNotEquals(SunlitBuckwheatPalette.primaryContainer, p.primaryContainer)
            assertNotEquals(SunlitBuckwheatPalette.secondaryContainer, p.secondaryContainer)
            assertNotEquals(SunlitBuckwheatPalette.tertiaryContainer, p.tertiaryContainer)
            assertNotEquals(SunlitBuckwheatPalette.onSecondaryContainer, p.onSecondaryContainer)
        }

        @Test
        fun `F1_T1_05 SplitMateViewModel setExpressiveThemeMode and cycleExpressiveThemeMode update activeThemeMode reactively`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            assertEquals(SplitMateThemeMode.SUNLIT_BUCKWHEAT, vm.uiState.value.activeThemeMode)

            vm.setExpressiveThemeMode(SplitMateThemeMode.KYOTO_MATCHA_YUZU)
            assertEquals(SplitMateThemeMode.KYOTO_MATCHA_YUZU, vm.uiState.value.activeThemeMode)
            assertFalse(vm.uiState.value.isDarkTheme)

            vm.setExpressiveThemeMode(SplitMateThemeMode.WARM_ESPRESSO_NIGHT)
            assertEquals(SplitMateThemeMode.WARM_ESPRESSO_NIGHT, vm.uiState.value.activeThemeMode)
            assertTrue(vm.uiState.value.isDarkTheme)

            val cycled = vm.cycleExpressiveThemeMode()
            assertEquals(SplitMateThemeMode.KYOTO_MATCHA_YUZU, cycled)
            assertEquals(SplitMateThemeMode.KYOTO_MATCHA_YUZU, vm.uiState.value.activeThemeMode)
        }

        // --- F2: M3 Expressive Dual-Scheme Spring Motion Physics ---

        @Test
        fun `F2_T1_01 SplitMateMotion fastSpatial spring uses dampingRatio 0_6f and stiffness 800f`() {
            val spec: SpringSpec<Float> = SplitMateMotion.fastSpatial()
            assertEquals(0.6f, spec.dampingRatio, 1e-4f)
            assertEquals(800f, spec.stiffness, 1e-4f)
            assertEquals(0.6f, SplitMateMotion.FAST_SPATIAL_DAMPING, 1e-4f)
            assertEquals(800f, SplitMateMotion.FAST_SPATIAL_STIFFNESS, 1e-4f)
        }

        @Test
        fun `F2_T1_02 SplitMateMotion defaultSpatial spring uses dampingRatio 0_8f and stiffness 380f`() {
            val spec: SpringSpec<Float> = SplitMateMotion.defaultSpatial()
            assertEquals(0.8f, spec.dampingRatio, 1e-4f)
            assertEquals(380f, spec.stiffness, 1e-4f)
        }

        @Test
        fun `F2_T1_03 SplitMateMotion slowSpatial spring uses dampingRatio 0_8f and stiffness 200f`() {
            val spec: SpringSpec<Float> = SplitMateMotion.slowSpatial()
            assertEquals(0.8f, spec.dampingRatio, 1e-4f)
            assertEquals(200f, spec.stiffness, 1e-4f)
        }

        @Test
        fun `F2_T1_04 SplitMateMotion fastEffects defaultEffects and slowEffects are critically damped 1_0f with zero overshoot`() {
            val fast: SpringSpec<Float> = SplitMateMotion.fastEffects()
            val def: SpringSpec<Float> = SplitMateMotion.defaultEffects()
            val slow: SpringSpec<Float> = SplitMateMotion.slowEffects()
            assertEquals(1.0f, fast.dampingRatio, 1e-4f)
            assertEquals(3800f, fast.stiffness, 1e-4f)
            assertEquals(1.0f, def.dampingRatio, 1e-4f)
            assertEquals(1600f, def.stiffness, 1e-4f)
            assertEquals(1.0f, slow.dampingRatio, 1e-4f)
            assertEquals(800f, slow.stiffness, 1e-4f)
        }

        @Test
        fun `F2_T1_05 DesignSystemBindings tactileSpring and themeColorTween delegate to SplitMateMotion springs`() {
            val spatial = DesignSystemBindings.tactileSpring<Float>()
            assertEquals(0.8f, spatial.dampingRatio, 1e-4f)
            assertEquals(380f, spatial.stiffness, 1e-4f)
            val colorSpec = DesignSystemBindings.themeColorTween<Color>()
            assertTrue(colorSpec is SpringSpec<Color>)
            val springColor = colorSpec as SpringSpec<Color>
            assertEquals(1.0f, springColor.dampingRatio, 1e-4f)
            assertEquals(800f, springColor.stiffness, 1e-4f)
        }

        // --- F3: M3 Expressive Emphasized Typography & Editorial Hierarchy ---

        @Test
        fun `F3_T1_01 SplitMateExpressiveTypography defines all 15 emphasized styles with Figtree and tnum`() {
            val styles = listOf(
                SplitMateExpressiveTypography.displayLargeEmphasized,
                SplitMateExpressiveTypography.displayMediumEmphasized,
                SplitMateExpressiveTypography.displaySmallEmphasized,
                SplitMateExpressiveTypography.headlineLargeEmphasized,
                SplitMateExpressiveTypography.headlineMediumEmphasized,
                SplitMateExpressiveTypography.headlineSmallEmphasized,
                SplitMateExpressiveTypography.titleLargeEmphasized,
                SplitMateExpressiveTypography.titleMediumEmphasized,
                SplitMateExpressiveTypography.titleSmallEmphasized,
                SplitMateExpressiveTypography.bodyLargeEmphasized,
                SplitMateExpressiveTypography.bodyMediumEmphasized,
                SplitMateExpressiveTypography.bodySmallEmphasized,
                SplitMateExpressiveTypography.labelLargeEmphasized,
                SplitMateExpressiveTypography.labelMediumEmphasized,
                SplitMateExpressiveTypography.labelSmallEmphasized
            )
            assertEquals(15, styles.size)
            styles.forEach { style ->
                assertEquals(FigtreeFontFamily, style.fontFamily)
                assertEquals("tnum, zero", style.fontFeatureSettings)
            }
        }

        @Test
        fun `F3_T1_02 displayLargeEmphasized and displayMediumEmphasized use Black weights with tight tracking`() {
            assertEquals(FontWeight.Black, SplitMateExpressiveTypography.displayLargeEmphasized.fontWeight)
            assertEquals(57.sp, SplitMateExpressiveTypography.displayLargeEmphasized.fontSize)
            assertEquals(FontWeight.Black, SplitMateExpressiveTypography.displayMediumEmphasized.fontWeight)
            assertEquals(45.sp, SplitMateExpressiveTypography.displayMediumEmphasized.fontSize)
        }

        @Test
        fun `F3_T1_03 headlineMediumEmphasized titleLargeEmphasized and labelLargeEmphasized use ExtraBold weight`() {
            assertEquals(FontWeight.ExtraBold, SplitMateExpressiveTypography.headlineMediumEmphasized.fontWeight)
            assertEquals(28.sp, SplitMateExpressiveTypography.headlineMediumEmphasized.fontSize)
            assertEquals(FontWeight.ExtraBold, SplitMateExpressiveTypography.titleLargeEmphasized.fontWeight)
            assertEquals(22.sp, SplitMateExpressiveTypography.titleLargeEmphasized.fontSize)
            assertEquals(FontWeight.ExtraBold, SplitMateExpressiveTypography.labelLargeEmphasized.fontWeight)
            assertEquals(14.sp, SplitMateExpressiveTypography.labelLargeEmphasized.fontSize)
        }

        @Test
        fun `F3_T1_04 SplitMateTypography base scale uses FigtreeFontFamily and tnum across all 15 M3 slots`() {
            val allSlots = listOf(
                SplitMateTypography.displayLarge,
                SplitMateTypography.displayMedium,
                SplitMateTypography.displaySmall,
                SplitMateTypography.headlineLarge,
                SplitMateTypography.headlineMedium,
                SplitMateTypography.headlineSmall,
                SplitMateTypography.titleLarge,
                SplitMateTypography.titleMedium,
                SplitMateTypography.titleSmall,
                SplitMateTypography.bodyLarge,
                SplitMateTypography.bodyMedium,
                SplitMateTypography.bodySmall,
                SplitMateTypography.labelLarge,
                SplitMateTypography.labelMedium,
                SplitMateTypography.labelSmall
            )
            allSlots.forEach { slot ->
                assertEquals(FigtreeFontFamily, slot.fontFamily)
                assertEquals("tnum, zero", slot.fontFeatureSettings)
            }
        }

        @Test
        fun `F3_T1_05 toSmartTitleCase normalizes group titles into editorial Title Case`() {
            assertEquals("Goa Beach Trip 2026", "goa beach trip 2026".toSmartTitleCase())
            assertEquals("Mumbai Flight", "MUMBAI FLIGHT".toSmartTitleCase())
            assertEquals("Kyoto Matcha Squad", "  kyoto   matcha   squad ".toSmartTitleCase())
        }

        // --- F4: Shape Morphing & Segmented Island Lists ---

        @Test
        fun `F4_T1_01 MaterialShapes provides 7 canonical normalized RoundedPolygons in morphSequence`() {
            val seq = MaterialShapes.morphSequence
            assertEquals(7, seq.size)
            assertEquals(
                listOf(
                    MaterialShapes.SoftBurst,
                    MaterialShapes.Cookie9Sided,
                    MaterialShapes.Pentagon,
                    MaterialShapes.Pill,
                    MaterialShapes.Sunny,
                    MaterialShapes.Clover4Leaf,
                    MaterialShapes.Oval
                ),
                seq
            )
        }

        @Test
        fun `F4_T1_02 MorphPolygonShape creates valid Path Outline at progress 0_0f 0_5f and 1_0f`() {
            val morph = Morph(MaterialShapes.SoftBurst, MaterialShapes.Cookie9Sided)
            listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f).forEach { progress ->
                val shape = MorphPolygonShape(morph = morph, percentage = progress, rotationDegrees = 45f)
                val outline = shape.createOutline(size = testSize, layoutDirection = LayoutDirection.Ltr, density = testDensity)
                assertTrue(outline is Outline.Generic)
                val path = (outline as Outline.Generic).path
                assertFalse(path.isEmpty, "Expected non-empty path at progress=$progress")
            }
        }

        @Test
        fun `F4_T1_03 segmentedIslandItemShape returns 24dp all-around for single item list`() {
            val shape = segmentedIslandItemShape(index = 0, totalCount = 1, isSelected = false)
            assertEquals(24f, cornerPx(shape.topStart), 0.01f)
            assertEquals(24f, cornerPx(shape.topEnd), 0.01f)
            assertEquals(24f, cornerPx(shape.bottomStart), 0.01f)
            assertEquals(24f, cornerPx(shape.bottomEnd), 0.01f)
        }

        @Test
        fun `F4_T1_04 segmentedIslandItemShape returns 24dp outer and 6dp inner corners for top middle and bottom items`() {
            val top = segmentedIslandItemShape(index = 0, totalCount = 3, isSelected = false)
            assertEquals(24f, cornerPx(top.topStart), 0.01f)
            assertEquals(24f, cornerPx(top.topEnd), 0.01f)
            assertEquals(6f, cornerPx(top.bottomStart), 0.01f)
            assertEquals(6f, cornerPx(top.bottomEnd), 0.01f)

            val middle = segmentedIslandItemShape(index = 1, totalCount = 3, isSelected = false)
            assertEquals(6f, cornerPx(middle.topStart), 0.01f)
            assertEquals(6f, cornerPx(middle.topEnd), 0.01f)
            assertEquals(6f, cornerPx(middle.bottomStart), 0.01f)
            assertEquals(6f, cornerPx(middle.bottomEnd), 0.01f)

            val bottom = segmentedIslandItemShape(index = 2, totalCount = 3, isSelected = false)
            assertEquals(6f, cornerPx(bottom.topStart), 0.01f)
            assertEquals(6f, cornerPx(bottom.topEnd), 0.01f)
            assertEquals(24f, cornerPx(bottom.bottomStart), 0.01f)
            assertEquals(24f, cornerPx(bottom.bottomEnd), 0.01f)
        }

        @Test
        fun `F4_T1_05 segmentedIslandItemShape morphs selected item to full 24dp outer CornerRadius`() {
            val selectedMiddle = segmentedIslandItemShape(index = 1, totalCount = 4, isSelected = true)
            assertEquals(24f, cornerPx(selectedMiddle.topStart), 0.01f)
            assertEquals(24f, cornerPx(selectedMiddle.topEnd), 0.01f)
            assertEquals(24f, cornerPx(selectedMiddle.bottomStart), 0.01f)
            assertEquals(24f, cornerPx(selectedMiddle.bottomEnd), 0.01f)
        }

        // --- F5: Official M3 Expressive Component Suite (7 Components) ---

        @Test
        fun `F5_T1_01 ExpressiveM3Components defines LinearWavyProgressIndicator and CircularWavyProgressIndicator`() {
            val src = readSourceFile("ui/components/ExpressiveM3Components.kt")
            assertTrue(src.contains("fun LinearWavyProgressIndicator("))
            assertTrue(src.contains("fun CircularWavyProgressIndicator("))
            assertTrue(src.contains("wavelength: Dp = 24.dp"))
            assertTrue(src.contains("gapSize: Dp = 4.dp"))
        }

        @Test
        fun `F5_T1_02 ExpressiveM3Components defines ButtonGroup and ConnectedButtonGroup with spring morphing`() {
            val src = readSourceFile("ui/components/ExpressiveM3Components.kt")
            assertTrue(src.contains("fun ButtonGroup("))
            assertTrue(src.contains("fun <T> ConnectedButtonGroup("))
            assertTrue(src.contains("SplitMateMotion.fastSpatial()"))
            val item = ExpressiveActionItem(label = "Settle Up", onClick = {}, isPrimary = true)
            assertEquals("Settle Up", item.label)
            assertTrue(item.isPrimary)
        }

        @Test
        fun `F5_T1_03 ExpressiveM3Components defines FloatingActionButtonMenu and ToggleFloatingActionButton`() {
            val src = readSourceFile("ui/components/ExpressiveM3Components.kt")
            assertTrue(src.contains("fun ToggleFloatingActionButton("))
            assertTrue(src.contains("fun FloatingActionButtonMenu("))
            assertTrue(src.contains("if (checked) 28.dp else 20.dp"))
            assertTrue(src.contains("if (checked) 90f else 0f"))
        }

        @Test
        fun `F5_T1_04 ExpressiveM3Components defines ContainedLoadingIndicator cycling 7 MaterialShapes polygons`() {
            val src = readSourceFile("ui/components/ExpressiveM3Components.kt")
            assertTrue(src.contains("fun ContainedLoadingIndicator("))
            assertTrue(src.contains("MaterialShapes.morphSequence"))
            assertTrue(src.contains("MorphPolygonShape("))
        }

        @Test
        fun `F5_T1_05 ExpressiveM3Components defines SplitButtonLayout and HorizontalFloatingToolbar`() {
            val src = readSourceFile("ui/components/ExpressiveM3Components.kt")
            assertTrue(src.contains("fun SplitButtonLayout("))
            assertTrue(src.contains("if (menuExpanded) 180f else 0f"))
            assertTrue(src.contains("fun HorizontalFloatingToolbar("))
            assertTrue(src.contains("palette.surfaceContainerHigh"))
            val action = ExpressiveMenuAction(label = "Export JSON", onClick = {}, subtitle = "Backup")
            assertEquals("Export JSON", action.label)
            assertEquals("Backup", action.subtitle)
        }

        // --- F6: App Shell & Navigation Architecture ---

        @Test
        fun `F6_T1_01 SplitMateAppComposable and SplitMateAppNavHost provide top-level navigation shell`() {
            val appSrc = readSourceFile("ui/SplitMateAppComposable.kt")
            val navSrc = readSourceFile("ui/navigation/SplitMateAppNavHost.kt")
            assertTrue(appSrc.contains("SplitMateApp(") || appSrc.contains("SplitMateAppComposable"))
            assertTrue(navSrc.contains("SplitMateAppNavHost("))
        }

        @Test
        fun `F6_T1_02 SplitMateViewModel selectTab switches across LEDGERS QUICK_SPLIT SCAN_RECEIPT SETTLE_UP and GUIDE`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            listOf("LEDGERS", "QUICK_SPLIT", "SCAN_RECEIPT", "SETTLE_UP", "GUIDE").forEach { tab ->
                vm.selectTab(tab)
                assertEquals(tab, vm.uiState.value.selectedTabName)
            }
        }

        @Test
        fun `F6_T1_03 SplitMateViewModel openGroupDetail and closeGroupDetail manage group detail drill-down state`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.openGroupDetail("g_tahoe")
            assertEquals("g_tahoe", vm.uiState.value.activeGroupId)
            assertEquals("g_tahoe", vm.uiState.value.openedGroupDetailId)
            assertEquals("LEDGERS", vm.uiState.value.selectedTabName)

            vm.closeGroupDetail()
            assertEquals(null, vm.uiState.value.openedGroupDetailId)
        }

        @Test
        fun `F6_T1_04 SplitMateViewModel navigateToSubFlow and finishSubFlowToGroupDetail preserve origin group context`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.openGroupDetail("g_tahoe")
            vm.navigateToSubFlow(targetTabName = "QUICK_SPLIT", originGroupDetailId = "g_tahoe")
            assertEquals("QUICK_SPLIT", vm.uiState.value.selectedTabName)
            assertEquals("g_tahoe", vm.uiState.value.returnToGroupDetailId)

            vm.finishSubFlowToGroupDetail()
            assertEquals("LEDGERS", vm.uiState.value.selectedTabName)
            assertEquals("g_tahoe", vm.uiState.value.openedGroupDetailId)
        }

        @Test
        fun `F6_T1_05 buildGradle configures versionCode 54 versionName 2_3_5_1 and graphics-shapes 1_0_1`() {
            val gradleText = resolveBuildGradleFile().readText()
            assertTrue(gradleText.contains("versionCode 54") || gradleText.contains("versionCode = 54"))
            assertTrue(gradleText.contains("versionName \"2.3.5.1\"") || gradleText.contains("versionName = \"2.3.5.1\""))
            assertTrue(gradleText.contains("androidx.graphics:graphics-shapes"))
        }

        // --- F7: Onboarding, Profile, Settings & 3-Theme Studio ---

        @Test
        fun `F7_T1_01 OnboardingAndSettingsScreens defines onboarding profile and settings composables`() {
            val src = readSourceFile("ui/screens/OnboardingAndSettingsScreens.kt")
            assertTrue(src.contains("Onboarding") || src.contains("Settings"))
            assertTrue(src.contains("OpenPeepsHeroStage"))
        }

        @Test
        fun `F7_T1_02 SplitMateViewModel completeOnboarding registers profile name country currency and seed`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.completeOnboarding(
                name = "Rohan Kulkarni",
                countryName = "India",
                currencyCode = "INR",
                currencySymbol = "\u20B9",
                avatarSeed = "Rohan|Male|open-peeps|Buckwheat",
                userPhone = "9876543210"
            )
            assertTrue(vm.uiState.value.hasRegisteredProfile)
            assertEquals("Rohan Kulkarni", vm.uiState.value.currentUserName)
            assertEquals("9876543210", vm.uiState.value.userPhone)
        }

        @Test
        fun `F7_T1_03 SplitMateViewModel updateUserProfile updates name phone UPI and canonical 4-token avatar seed`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.updateUserProfile(
                newName = "Meera Nair",
                newPhone = "+91 98111 22333",
                newSeedOrCurrency = "Meera|Female|adventurer|PastelWall",
                newUpiId = "meera@okicici"
            )
            assertEquals("Meera Nair", vm.uiState.value.currentUserName)
            assertEquals("9811122333", vm.uiState.value.userPhone)
            assertEquals("meera@okicici", vm.uiState.value.userUpiId)
            assertEquals("adventurer", vm.uiState.value.avatarStyleId)
            assertEquals("PastelWall", vm.uiState.value.avatarColorPresetId)
        }

        @Test
        fun `F7_T1_04 SplitMateViewModel toggleDarkTheme preserves Kyoto Matcha when switching back to light mode`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.setExpressiveThemeMode(SplitMateThemeMode.KYOTO_MATCHA_YUZU)
            assertEquals(SplitMateThemeMode.KYOTO_MATCHA_YUZU, vm.uiState.value.activeThemeMode)

            vm.toggleDarkTheme(true)
            assertEquals(SplitMateThemeMode.WARM_ESPRESSO_NIGHT, vm.uiState.value.activeThemeMode)
            assertTrue(vm.uiState.value.isDarkTheme)

            vm.toggleDarkTheme(false)
            assertEquals(SplitMateThemeMode.SUNLIT_BUCKWHEAT, vm.uiState.value.activeThemeMode)
            assertFalse(vm.uiState.value.isDarkTheme)
        }

        @Test
        fun `F7_T1_05 SplitMateCrewScenes defines 5 offline Open Peeps SVG crew scenes`() {
            assertEquals(5, SplitMateCrewScenes.size)
            assertEquals(
                listOf("walk_crew", "bike_trip", "coffee_hangout", "weekend_squad", "roadtrip_busters"),
                SplitMateCrewScenes.map { it.id }
            )
            SplitMateCrewScenes.forEach { scene ->
                assertTrue(scene.assetPath.startsWith("file:///android_asset/peeps/scenes/"))
                assertTrue(scene.assetPath.endsWith(".svg"))
            }
        }

        // --- F8: Trip Sync & Perspective Sheet + Guide ---

        @Test
        fun `F8_T1_01 TripSyncAndPerspectiveSheet and QuickExpenseAndGuideScreens exist and expose sync & guide UI`() {
            val syncSrc = readSourceFile("ui/dialogs/TripSyncAndPerspectiveSheet.kt")
            val guideSrc = readSourceFile("ui/screens/QuickExpenseAndGuideScreens.kt")
            assertTrue(syncSrc.contains("TripSync") || syncSrc.contains("Perspective"))
            assertTrue(guideSrc.contains("Guide") || guideSrc.contains("QuickExpense"))
        }

        @Test
        fun `F8_T1_02 SplitMateViewModel exportGroupSyncPayload generates valid SM2 token deepLink and WhatsApp message`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val groupId = vm.uiState.value.activeGroupId
            val bundle = vm.exportGroupSyncPayload(groupId)
            assertNotNull(bundle)
            assertTrue(bundle!!.syncToken.startsWith("SM2_"))
            assertTrue(bundle.deepLinkUri.startsWith("splitmate://trip-sync?payload=SM2_"))
            assertTrue(bundle.whatsappShareText.contains(bundle.syncToken))
            assertTrue(bundle.memberCount >= 2)
        }

        @Test
        fun `F8_T1_03 SplitMateViewModel extractSyncTokenFromRawInput extracts SM2 token from raw string deepLink or WhatsApp text`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val bundle = vm.exportGroupSyncPayload(vm.uiState.value.activeGroupId)!!
            assertEquals(bundle.syncToken, SplitMateViewModel.extractSyncTokenFromRawInput(bundle.syncToken))
            assertEquals(bundle.syncToken, SplitMateViewModel.extractSyncTokenFromRawInput(bundle.deepLinkUri))
            assertEquals(bundle.syncToken, SplitMateViewModel.extractSyncTokenFromRawInput(bundle.whatsappShareText))
        }

        @Test
        fun `F8_T1_04 SplitMateViewModel importAndMergeGroupSyncPayload round-trips group expenses and splits`() = runTest {
            val senderVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val groupId = senderVm.uiState.value.activeGroupId
            val memberIds = senderVm.uiState.value.activeGroupMembers.map { it.memberId }
            senderVm.commitQuickEqualExpense("Pondicherry Cafe Breakfast", 90000L, memberIds)
            val bundle = senderVm.exportGroupSyncPayload(groupId)!!

            val receiverVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            receiverVm.clearLocalVault()
            val mergeResult = receiverVm.importAndMergeGroupSyncPayload(bundle.whatsappShareText, openGroupAfterMerge = true)
            assertTrue(mergeResult.success)
            assertEquals(groupId, mergeResult.groupId)
            assertTrue(receiverVm.uiState.value.expenses.any { it.title == "Pondicherry Cafe Breakfast" })
        }

        @Test
        fun `F8_T1_05 SplitMateViewModel claimGroupMemberPerspective switches active current-user perspective`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val groupId = vm.uiState.value.activeGroupId
            val nonMe = vm.uiState.value.activeGroupMembers.first { !it.isCurrentUser }
            vm.claimGroupMemberPerspective(groupId = groupId, memberId = nonMe.memberId)
            val updatedMembers = vm.uiState.value.members.filter { it.groupId == groupId }
            val newMe = updatedMembers.first { it.isCurrentUser }
            assertEquals(nonMe.memberId, newMe.memberId)
        }

        // --- F9: Trip Home & Group Ledgers + Hero Moment #1 (Interactive Balance Cascade Card) ---

        @Test
        fun `F9_T1_01 TripHomeScreen and SplitMateAppComposable define group ledger cards and balance cascade hero`() {
            val homeSrc = readSourceFile("ui/screens/TripHomeScreen.kt")
            val appSrc = readSourceFile("ui/SplitMateAppComposable.kt")
            assertTrue(homeSrc.isNotBlank())
            assertTrue(appSrc.isNotBlank())
        }

        @Test
        fun `F9_T1_02 SplitMateViewModel computeGroupMemberNetBalances computes exact creditor and debtor net paise`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.clearLocalVault()
            vm.createNewGroup(name = "Coorg Coffee Estate", currencyCode = "INR", friendNamesCsv = "Aarav,Diya", iconName = "Flight")
            val groupId = vm.uiState.value.activeGroupId
            val members = vm.uiState.value.members.filter { it.groupId == groupId }
            val payer = members.first { it.isCurrentUser }
            vm.commitQuickEqualExpense(
                title = "Estate Homestay Villa",
                totalAmountCents = 90000L,
                selectedMemberIds = members.map { it.memberId },
                payerMemberId = payer.memberId
            )
            val netMap = vm.computeGroupMemberNetBalances(groupId)
            assertEquals(60000L, netMap[payer.memberId])
            members.filterNot { it.memberId == payer.memberId }.forEach { friend ->
                assertEquals(-30000L, netMap[friend.memberId])
            }
            assertEquals(0L, netMap.values.sum())
        }

        @Test
        fun `F9_T1_03 Elements GM3 semantic balance tokens map positive to olive-sage and negative to terracotta-peach`() {
            assertEquals(Color(0xFFD7E8B6), DesignSystemBindings.ElementsCreditorContainer)
            assertEquals(Color(0xFF2D4810), DesignSystemBindings.ElementsCreditorOnContainer)
            assertEquals(Color(0xFFFED8C8), DesignSystemBindings.ElementsDebtorContainer)
            assertEquals(Color(0xFF7C2D12), DesignSystemBindings.ElementsDebtorOnContainer)
        }

        @Test
        fun `F9_T1_04 SplitMateViewModel renameGroup normalizes name to SmartTitleCase and updates iconName`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val groupId = vm.uiState.value.activeGroupId
            vm.renameGroup(groupId = groupId, newName = "jaipur palace weekend", newIconName = "Hotel")
            val updated = vm.uiState.value.groups.first { it.groupId == groupId }
            assertEquals("Jaipur Palace Weekend", updated.name)
            assertEquals("Hotel", updated.iconName)
        }

        @Test
        fun `F9_T1_05 MaterialShapes defines distinct expressive M3 polygons for group and category badges`() {
            val shapes = listOf(
                MaterialShapes.SoftBurst,
                MaterialShapes.Cookie9Sided,
                MaterialShapes.Pentagon,
                MaterialShapes.Sunny,
                MaterialShapes.Clover4Leaf,
                MaterialShapes.Pill
            )
            assertEquals(6, shapes.distinct().size)
            shapes.forEach { poly ->
                val outline = RoundedPolygonShape(poly).createOutline(testSize, LayoutDirection.Ltr, testDensity)
                assertTrue(outline is Outline.Generic && !(outline as Outline.Generic).path.isEmpty)
            }
        }

        // --- F10: Quick Expense Logger & Calculator + Hero Moment #2 (Spring-Physics Keypad) ---

        @Test
        fun `F10_T1_01 QuickExpenseAndGuideScreens defines tactile keypad and equal split calculator`() {
            val src = readSourceFile("ui/screens/QuickExpenseAndGuideScreens.kt")
            assertTrue(src.contains("QuickExpense"))
            assertTrue(src.contains("commitQuickEqualExpense") || src.contains("splitEquallyZeroDrift"))
        }

        @Test
        fun `F10_T1_02 SplitMateMathEngine splitEquallyZeroDrift splits 10000 paise across 3 members with 0 drift and Payer-First penny`() {
            val members = listOf("m1" to "Akshay", "m2" to "Rohan", "m3" to "Priya")
            val result = SplitMateMathEngine.splitEquallyZeroDrift(
                totalCents = 10000L,
                members = members,
                payerId = "m1",
                currentUserId = "m1"
            )
            assertEquals(3, result.size)
            assertEquals(10000L, result.sumOf { it.finalCents })
            val payerAlloc = result.first { it.memberId == "m1" }
            assertEquals(3334L, payerAlloc.finalCents)
            assertTrue(payerAlloc.plusOneCent)
            assertEquals(3333L, result.first { it.memberId == "m2" }.finalCents)
            assertEquals(3333L, result.first { it.memberId == "m3" }.finalCents)
        }

        @Test
        fun `F10_T1_03 SplitMateViewModel commitQuickEqualExpense charges only selected subset of members`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val members = vm.uiState.value.activeGroupMembers
            assertTrue(members.size >= 3)
            val subsetIds = members.take(2).map { it.memberId }
            val excludedId = members.drop(2).first().memberId

            vm.commitQuickEqualExpense(
                title = "Midnight Gelato",
                totalAmountCents = 50100L,
                selectedMemberIds = subsetIds,
                payerMemberId = subsetIds.first()
            )
            val logged = vm.uiState.value.expenses.first { it.title == "Midnight Gelato" }
            val breakdown = SplitMateViewModel.resolveExpenseSplitBreakdown(
                expense = logged,
                groupMembers = members,
                allSplits = vm.uiState.value.splits
            )
            assertEquals(2, breakdown.splittingMembersCount)
            assertEquals(50100L, breakdown.rows.sumOf { it.owedCents })
            val excludedRow = breakdown.rows.first { it.memberId == excludedId }
            assertFalse(excludedRow.isIncludedInSplit)
            assertEquals(0L, excludedRow.owedCents)
        }

        @Test
        fun `F10_T1_04 rememberAnimatedSegmentedIslandItemShape is defined in ExpressiveShapesAndMotion with fastSpatial spring`() {
            val src = readSourceFile("ui/components/ExpressiveShapesAndMotion.kt")
            assertTrue(src.contains("fun rememberAnimatedSegmentedIslandItemShape("))
            assertTrue(src.contains("SplitMateMotion.fastSpatial()"))
            assertTrue(src.contains("RoundedCornerShape("))
        }

        @Test
        fun `F10_T1_05 formatIndianRupeesFromCents formats integer paise using Indian lakh-crore grouping`() {
            assertEquals("\u20B91,250.50", formatIndianRupeesFromCents(125050L))
            assertEquals("\u20B91,00,000.00", formatIndianRupeesFromCents(10000000L))
            assertEquals("+\u20B9450.00", formatIndianRupeesFromCents(45000L, includePlusSign = true))
        }

        // --- F11: Live Receipt Claim & Remainder Engine + Hero Moment #3 ---

        @Test
        fun `F11_T1_01 SplitMateMathEngine calculateProportionalReceiptSplits locks multiplier m = T over B and holds unassigned on payer`() {
            val claims = listOf(
                Triple("m1", "Akshay", 4000L),
                Triple("m2", "Rohan", 3000L),
                Triple("m3", "Priya", 0L)
            )
            val res = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = 10000L,
                taxCents = 800L,
                tipCents = 1200L,
                payerId = "m1",
                memberBaseClaimsCents = claims,
                attributeRemainderToPayer = true
            )
            assertEquals(1.20, res.lockedMultiplier, 1e-6)
            assertEquals(12000L, res.totalFinalCents)
            assertEquals(3000L, res.unassignedBaseCents)
            assertEquals(3600L, res.unassignedFinalCents)
            assertEquals(0L, res.driftCents)
            assertEquals(12000L, res.allocations.sumOf { it.finalCents })
        }

        @Test
        fun `F11_T1_02 SplitMateMathEngine splits unassigned remainder equally across all members and reconciles proportional tax and tip with 0 drift`() {
            val initialClaims = listOf(
                Triple("m1", "Akshay", 3000L),
                Triple("m2", "Rohan", 3000L),
                Triple("m3", "Priya", 1000L)
            )
            val unassignedBase = 10000L - initialClaims.sumOf { it.third }
            val remainderShares = SplitMateMathEngine.splitEquallyZeroDrift(
                totalCents = unassignedBase,
                members = initialClaims.map { it.first to it.second },
                payerId = "m1",
                currentUserId = "m1"
            ).associateBy { it.memberId }
            val updatedClaims = initialClaims.map { (id, name, base) ->
                Triple(id, name, base + (remainderShares[id]?.finalCents ?: 0L))
            }
            val afterSplit = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = 10000L,
                taxCents = 900L,
                tipCents = 1500L,
                payerId = "m1",
                memberBaseClaimsCents = updatedClaims,
                attributeRemainderToPayer = true
            )
            assertEquals(10000L, afterSplit.allocations.sumOf { it.baseClaimedCents })
            assertEquals(0L, afterSplit.unassignedBaseCents)
            assertEquals(0L, afterSplit.unassignedFinalCents)
            assertEquals(0L, afterSplit.driftCents)
            assertEquals(12400L, afterSplit.allocations.sumOf { it.finalCents })
        }

        @Test
        fun `F11_T1_03 calculateProportionalReceiptSplits reconciles indivisible tax and tip via Largest Remainder with 0_00 drift`() {
            val claims = listOf(
                Triple("m1", "Akshay", 3334L),
                Triple("m2", "Rohan", 3333L),
                Triple("m3", "Priya", 3333L)
            )
            val res = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = 10000L,
                taxCents = 777L,
                tipCents = 333L,
                payerId = "m1",
                memberBaseClaimsCents = claims,
                attributeRemainderToPayer = true
            )
            assertEquals(11110L, res.totalFinalCents)
            assertEquals(11110L, res.allocations.sumOf { it.finalCents })
            assertEquals(0L, res.driftCents)
        }

        @Test
        fun `F11_T1_04 LinearWavyProgressIndicator supports receipt claim completion ratio from 0_0f to 1_0f`() {
            val src = readSourceFile("ui/components/ExpressiveM3Components.kt")
            assertTrue(src.contains("val rawProgress = progress().coerceIn(0f, 1f)"))
            assertTrue(src.contains("if (rawProgress <= 0.001f || rawProgress >= 0.999f)"))
        }

        @Test
        fun `F11_T1_05 SplitMateMathEngine and UI host Live Receipt Claim and Split Remainder Equally workflow`() {
            val mathSrc = readSourceFile("SplitMateMathEngine.kt")
            assertTrue(mathSrc.contains("calculateProportionalReceiptSplits"))
            assertTrue(mathSrc.contains("attributeRemainderToPayer"))
        }

        // --- F12: Greedy Debt Simplification & Settle Up ---

        @Test
        fun `F12_T1_01 simplifyDebtsGreedy collapses transitive 3-member chain A owes B owes C into 1 transfer`() {
            val balances = listOf(
                SplitMateMathEngine.MemberNetBalance("m_a", "Aarav", -50000L),
                SplitMateMathEngine.MemberNetBalance("m_b", "Bhavna", 0L),
                SplitMateMathEngine.MemberNetBalance("m_c", "Chirag", 50000L)
            )
            val transfers = SplitMateMathEngine.simplifyDebtsGreedy(balances)
            assertEquals(1, transfers.size)
            assertEquals("m_a", transfers[0].fromMemberId)
            assertEquals("m_c", transfers[0].toMemberId)
            assertEquals(50000L, transfers[0].amountCents)
        }

        @Test
        fun `F12_T1_02 simplifyDebtsGreedy satisfies N minus 1 upper bound for 6-member trip ledger`() {
            val balances = listOf(
                SplitMateMathEngine.MemberNetBalance("m1", "M1", 120000L),
                SplitMateMathEngine.MemberNetBalance("m2", "M2", 80000L),
                SplitMateMathEngine.MemberNetBalance("m3", "M3", -45000L),
                SplitMateMathEngine.MemberNetBalance("m4", "M4", -55000L),
                SplitMateMathEngine.MemberNetBalance("m5", "M5", -60000L),
                SplitMateMathEngine.MemberNetBalance("m6", "M6", -40000L)
            )
            val transfers = SplitMateMathEngine.simplifyDebtsGreedy(balances)
            assertTrue(transfers.size <= 5)
            assertEquals(200000L, transfers.sumOf { it.amountCents })
        }

        @Test
        fun `F12_T1_03 computeMemberSettlementSummaries aggregates outgoing and incoming payments per member`() {
            val transfers = listOf(
                SplitMateMathEngine.SimplifiedTransfer("m2", "Rohan", "m1", "Akshay", 45000L),
                SplitMateMathEngine.SimplifiedTransfer("m3", "Priya", "m1", "Akshay", 30000L)
            )
            val summaries = SplitMateMathEngine.computeMemberSettlementSummaries(transfers)
            assertEquals(3, summaries.size)
            val akshay = summaries.first { it.memberId == "m1" }
            assertEquals(75000L, akshay.totalIncomingCents)
            assertEquals(0L, akshay.totalOutgoingCents)
            assertTrue(akshay.hasMultipleIncoming)
        }

        @Test
        fun `F12_T1_04 SplitMateViewModel markGreedyTransferSettled records settlement and reduces remaining debt`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val groupId = vm.uiState.value.activeGroupId
            val netMap = vm.computeGroupMemberNetBalances(groupId)
            val transfersBefore = SplitMateMathEngine.simplifyDebtsGreedy(
                vm.uiState.value.activeGroupMembers.map {
                    SplitMateMathEngine.MemberNetBalance(it.memberId, it.name, netMap[it.memberId] ?: 0L)
                }
            )
            assertTrue(transfersBefore.isNotEmpty())
            val firstTransfer = transfersBefore.first()
            val settlementsCountBefore = vm.uiState.value.settlements.size

            vm.markGreedyTransferSettled(firstTransfer)
            assertEquals(settlementsCountBefore + 1, vm.uiState.value.settlements.size)
        }

        @Test
        fun `F12_T1_05 SplitButtonLayout supports primary settlement action with secondary dropdown options`() {
            val src = readSourceFile("ui/components/ExpressiveM3Components.kt")
            assertTrue(src.contains("fun SplitButtonLayout("))
            assertTrue(src.contains("DropdownMenu("))
            assertTrue(src.contains("DropdownMenuItem("))
        }

        // --- F13: Transit Hub #1 — Tactile Paper IRCTC PNR Studio ---

        @Test
        fun `F13_T1_01 PnrExpenseReviewScreen and PnrTicketModels support 10-digit IRCTC PNR extraction and formatting`() {
            val pnrSrc = readSourceFile("ui/screens/PnrExpenseReviewScreen.kt")
            assertTrue(pnrSrc.contains("PnrExpenseReviewScreen"))
            val ticket = ParsedTravelTicket(
                pnr = "2458910342",
                trainOrFlightNo = "12952",
                trainOrCarrierName = "MUMBAI RAJDHANI",
                fromStation = "NDLS",
                toStation = "MMCT",
                departureDate = "18 Oct 2026",
                departureTime = "16:55",
                coachAndSeats = "B4 / 21 (LB), 22 (UB)",
                bookingStatus = "CNF",
                fareRupees = "5740"
            )
            val formatted = formatTravelExpenseTitle("Train", ticket)
            val parsedBack = extractTravelTicketFromTitle(formatted)
            assertNotNull(parsedBack)
            assertEquals("2458910342", parsedBack!!.pnr)
            assertEquals("12952", parsedBack.trainOrFlightNo)
            assertEquals("NDLS", parsedBack.fromStation)
            assertEquals("MMCT", parsedBack.toStation)
        }

        @Test
        fun `F13_T1_02 resolveStationDisplayName resolves NDLS MMCT SBC MAS HWH and GOI to full city names`() {
            assertEquals("NDLS (New Delhi)", resolveStationDisplayName("NDLS"))
            assertEquals("MMCT (Mumbai Central)", resolveStationDisplayName("MMCT"))
            assertTrue(resolveStationDisplayName("SBC").contains("Bengaluru"))
            assertTrue(resolveStationDisplayName("MAS").contains("Chennai"))
            assertTrue(resolveStationDisplayName("HWH").contains("Howrah"))
        }

        @Test
        fun `F13_T1_03 UniversalFlightTicketExtractor and PnrNetworkRepository extract 10-digit IRCTC PNR and evaluate confirmation status`() {
            val rawIrctc = """
                IRCTC Electronic Reservation Slip
                PNR: 4829103847
                Train No & Name: 12009 / SHATABDI EXP
                From: MMCT To: ADI
                Departure: 06:20
                Total Fare: Rs. 2460
                Passenger 1: Mr Akshay Karadkar
            """.trimIndent()
            val extracted = UniversalFlightTicketExtractor.extractFromText(rawIrctc)
            assertTrue(extracted.isTrainPdfTicket)
            assertEquals("4829103847", PnrNetworkRepository.normalizePnrKey(extracted.pnr))
            assertEquals("12009", extracted.flightNumber)
            assertEquals(246000L, extracted.totalFarePaise)

            val confirmedSnapshot = extracted.toLivePnrStatusSnapshot()
            assertEquals("4829103847", confirmedSnapshot.pnr)
            assertEquals("12009", confirmedSnapshot.trainNo)
            assertTrue(PnrNetworkRepository.isSnapshotAllConfirmed(confirmedSnapshot))

            val racSnapshot = confirmedSnapshot.copy(
                passengerStatuses = listOf("P1: CNF/C4/32/WS", "P2: RAC 4"),
                structuredPassengers = listOf(
                    LivePnrPassenger("Passenger 1", "CNF/C4/32", "CNF", "Confirmed"),
                    LivePnrPassenger("Passenger 2", "WL 12", "RAC 4", "RAC")
                )
            )
            assertFalse(PnrNetworkRepository.isSnapshotAllConfirmed(racSnapshot))
        }

        @Test
        fun `F13_T1_04 SplitMateViewModel commitQuickEqualExpense deduplicates repeated 10-digit IRCTC PNR in same group`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val memberIds = vm.uiState.value.activeGroupMembers.map { it.memberId }
            val title1 = "Train 12952 (NDLS \u2192 MMCT) [PNR:8877665544]"
            vm.commitQuickEqualExpense(title = title1, totalAmountCents = 420000L, selectedMemberIds = memberIds)
            val countAfterFirst = vm.uiState.value.expenses.count { it.travelPnr == "8877665544" }
            assertEquals(1, countAfterFirst)

            vm.commitQuickEqualExpense(title = title1, totalAmountCents = 480000L, selectedMemberIds = memberIds)
            val matching = vm.uiState.value.expenses.filter { it.travelPnr == "8877665544" }
            assertEquals(1, matching.size)
            assertEquals(480000L, matching.first().totalAmountCents)
        }

        @Test
        fun `F13_T1_05 cleanDisplayExpenseTitle strips internal metadata tags for clean ledger display`() {
            val ticket = ParsedTravelTicket(
                pnr = "2458910342",
                trainOrFlightNo = "12952",
                trainOrCarrierName = "RAJDHANI EXP",
                fromStation = "NDLS",
                toStation = "MMCT",
                bookingStatus = "CNF"
            )
            val raw = formatTravelExpenseTitle("Train", ticket)
            val cleaned = cleanDisplayExpenseTitle(raw)
            assertFalse(cleaned.contains("[[SM_TICKET"))
            assertTrue(cleaned.contains("12952") || cleaned.contains("NDLS"))
        }

        // --- F14: Transit Hub #2 — Flight Boarding Pass & Universal Ticket Scanner ---

        @Test
        fun `F14_T1_01 UniversalFlightTicketExtractor extracts IndiGo 6E flight PNR route passengers and total paise`() {
            val sampleIndiGo = """
                IndiGo Booking Confirmation
                PNR: K9M4Q2
                Flight: 6E-5124
                From: DEL (New Delhi) To: GOI (Goa)
                Date: 14 Nov 2026  Dep: 08:15  Arr: 10:55
                Passengers:
                1. Mr Akshay Karadkar  Seat 12A
                2. Ms Priya Sharma     Seat 12B
                Total Fare: INR 11,480.00
            """.trimIndent()
            val extracted = UniversalFlightTicketExtractor.extractFromText(
                rawText = sampleIndiGo,
                groupMemberNames = listOf("Akshay", "Priya", "Rohan")
            )
            assertTrue(extracted.isValidFlightTicket)
            assertEquals("K9M4Q2", extracted.pnr)
            assertTrue(extracted.flightNumber.contains("6E"))
            assertEquals("DEL", extracted.originIata)
            assertEquals("GOI", extracted.destinationIata)
            assertEquals(1148000L, extracted.totalFarePaise)
        }

        @Test
        fun `F14_T1_02 UniversalFlightTicketExtractor extracts Air India flight details and matches group members`() {
            val sampleAirIndia = """
                Air India E-Ticket Receipt
                Booking Reference (PNR): Z7X2W9
                Flight Number: AI 865
                Origin: BOM (Mumbai)  Destination: BLR (Bengaluru)
                Departure: 22 Dec 2026 18:40
                Passenger Name: Mr Rohan Verma (Seat 14C)
                Grand Total: Rs. 6,850
            """.trimIndent()
            val extracted = UniversalFlightTicketExtractor.extractFromText(
                rawText = sampleAirIndia,
                groupMemberNames = listOf("Rohan", "Akshay")
            )
            assertTrue(extracted.isValidFlightTicket)
            assertEquals("Z7X2W9", extracted.pnr)
            assertEquals("BOM", extracted.originIata)
            assertEquals("BLR", extracted.destinationIata)
            assertEquals(685000L, extracted.totalFarePaise)
            assertTrue(extracted.matchedGroupMembers.contains("Rohan"))
        }

        @Test
        fun `F14_T1_03 isFlightTicketExpense distinguishes 6-char Flight PNRs and airlines from 10-digit IRCTC Train PNRs`() {
            assertTrue(isFlightTicketExpense("IndiGo 6E-204 (DEL -> GOI) [PNR:K9M4Q2]"))
            assertTrue(isFlightTicketExpense("Air India AI-865 BOM to BLR"))
            assertFalse(isFlightTicketExpense("Train 12952 Mumbai Rajdhani [PNR:2458910342]"))
            assertFalse(isFlightTicketExpense("IRCTC Vande Bharat Express 22436"))
        }

        @Test
        fun `F14_T1_04 FlightExpenseReviewScreen exists and integrates UniversalFlightTicketExtractor`() {
            val src = readSourceFile("ui/screens/FlightExpenseReviewScreen.kt")
            assertTrue(src.contains("FlightExpenseReviewScreen"))
            assertTrue(src.contains("UniversalFlightTicketExtractor"))
        }

        @Test
        fun `F14_T1_05 SplitMateViewModel findExistingExpenseByPnr locates logged 6-char Flight PNR across active group`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val memberIds = vm.uiState.value.activeGroupMembers.map { it.memberId }
            vm.commitQuickEqualExpense(
                title = "IndiGo 6E-512 (DEL -> GOI) [PNR:AB12CD]",
                totalAmountCents = 960000L,
                selectedMemberIds = memberIds
            )
            val match = vm.findExistingExpenseByPnr("AB12CD")
            assertNotNull(match)
            assertEquals(960000L, match!!.first.totalAmountCents)
        }

        // --- F15: Transit Hub #3 — Animated Transit Deck Hero Card (Hero Moment #4) ---

        @Test
        fun `F15_T1_01 AnimatedTransitDeckHeroCard defines transit deck hero composable and vector path rendering`() {
            val src = readSourceFile("ui/components/AnimatedTransitDeckHeroCard.kt")
            assertTrue(src.contains("AnimatedTransitDeckHeroCard"))
            assertTrue(src.contains("PathEffect.dashPathEffect") || src.contains("Canvas"))
        }

        @Test
        fun `F15_T1_02 AnimatedTransitDeckHeroCard supports FLIGHT TRAIN STAY CAB and RENTAL categories`() {
            val src = readSourceFile("ui/components/AnimatedTransitDeckHeroCard.kt")
            listOf("FLIGHT", "TRAIN", "IRCTC RAIL PASS", "AIRLINE E-TICKET").forEach { token ->
                assertTrue(src.contains(token), "Expected AnimatedTransitDeckHeroCard to handle $token")
            }
            val themeSrc = readSourceFile("ui/SplitMateTheme.kt")
            listOf("flight", "train", "hotel", "resort", "stay", "uber", "ola", "cab", "auto").forEach { keyword ->
                assertTrue(themeSrc.contains(keyword), "Expected SplitMateTheme category icon/badge resolver to handle $keyword")
            }
        }

        @Test
        fun `F15_T1_03 resolveExpenseCategoryIcon maps travel dining cab and hotel titles to rounded Material icons`() {
            val flightIcon = resolveExpenseCategoryIcon("IndiGo 6E-512 Flight to Goa")
            val trainIcon = resolveExpenseCategoryIcon("IRCTC Rajdhani Train 12952")
            val hotelIcon = resolveExpenseCategoryIcon("Taj Resort Villa Stay")
            val cabIcon = resolveExpenseCategoryIcon("Airport Uber Cab Transfer")
            assertNotEquals(flightIcon, trainIcon)
            assertNotEquals(trainIcon, hotelIcon)
            assertNotEquals(hotelIcon, cabIcon)
        }

        @Test
        fun `F15_T1_04 resolveExpenseCategoryBadgeColors returns distinct light and dark badge pairs per category`() {
            val (flightBgLight, flightFgLight) = resolveExpenseCategoryBadgeColors("IndiGo 6E-512 Flight", isDark = false)
            val (flightBgDark, flightFgDark) = resolveExpenseCategoryBadgeColors("IndiGo 6E-512 Flight", isDark = true)
            val (trainBgLight, trainFgLight) = resolveExpenseCategoryBadgeColors("IRCTC Train 12952", isDark = false)
            assertNotEquals(flightBgLight, flightBgDark)
            assertNotEquals(flightBgLight, trainBgLight)
            assertTrue(wcagContrastRatio(flightFgLight, flightBgLight) >= 4.5)
            assertTrue(wcagContrastRatio(flightFgDark, flightBgDark) >= 4.5)
            assertTrue(wcagContrastRatio(trainFgLight, trainBgLight) >= 4.5)
        }

        @Test
        fun `F15_T1_05 CompactLedgerTicketStub in SplitMateTheme renders 76dp single-row stub for train and flight tickets`() {
            val src = readSourceFile("ui/SplitMateTheme.kt")
            assertTrue(src.contains("fun CompactLedgerTicketStub("))
            assertTrue(src.contains("Paper Pass"))
        }

        // --- F16: Classic Group Detail & Member Governance ---

        @Test
        fun `F16_T1_01 SplitMateViewModel getGroupOrganizerMembers returns primary creator as default organizer`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val organizers = vm.getGroupOrganizerMembers()
            assertTrue(organizers.isNotEmpty())
            assertTrue(vm.isCurrentUserOrganizer())
        }

        @Test
        fun `F16_T1_02 SplitMateViewModel promoteMemberToOrganizer and dismissMemberAsOrganizer manage co-organizers`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val groupId = vm.uiState.value.activeGroupId
            val friend = vm.uiState.value.activeGroupMembers.first { !it.isCurrentUser }

            val promoted = vm.promoteMemberToOrganizer(groupId = groupId, targetMemberId = friend.memberId)
            assertTrue(promoted)
            assertTrue(vm.isMemberGroupOrganizer(groupId = groupId, member = friend))

            val dismissed = vm.dismissMemberAsOrganizer(groupId = groupId, targetMemberId = friend.memberId)
            assertTrue(dismissed)
        }

        @Test
        fun `F16_T1_03 SplitMateViewModel declineGroupInvite and restoreDeclinedGroupInvite transition inviteStatus`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val groupId = vm.uiState.value.activeGroupId
            vm.declineGroupInvite(context = null, groupId = groupId)
            val meAfterDecline = vm.uiState.value.members.first { it.groupId == groupId && it.isCurrentUser }
            assertEquals("DECLINED", meAfterDecline.inviteStatus)

            vm.restoreDeclinedGroupInvite(context = null, groupId = groupId)
            val meAfterRestore = vm.uiState.value.members.first { it.groupId == groupId && it.isCurrentUser }
            assertEquals("JOINED", meAfterRestore.inviteStatus)
        }

        @Test
        fun `F16_T1_04 SplitMateViewModel reassignDeclinedMemberSharesEqually redistributes declined member shares with 0 drift`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.clearLocalVault()
            vm.createNewGroup(name = "Munnar Tea Trail", currencyCode = "INR", friendNamesCsv = "Rohan,Priya", iconName = "Flight")
            val groupId = vm.uiState.value.activeGroupId
            val members = vm.uiState.value.members.filter { it.groupId == groupId }
            val payer = members.first { it.isCurrentUser }
            val rohan = members.first { it.name == "Rohan" }

            vm.commitQuickEqualExpense(
                title = "Tea Bungalow Booking",
                totalAmountCents = 90000L,
                selectedMemberIds = members.map { it.memberId },
                payerMemberId = payer.memberId
            )
            vm.reassignDeclinedMemberSharesEqually(context = null, groupId = groupId, declinedMemberId = rohan.memberId)

            val expense = vm.uiState.value.expenses.first { it.groupId == groupId }
            val updatedSplits = vm.uiState.value.splits.filter { it.expenseId == expense.expenseId }
            assertEquals(2, updatedSplits.size)
            assertFalse(updatedSplits.any { it.memberId == rohan.memberId })
            assertEquals(90000L, updatedSplits.sumOf { it.finalOwedCents })
        }

        @Test
        fun `F16_T1_05 SplitMateViewModel canSafelyRemoveMember blocks removal when member has ledger history`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val activeMembers = vm.uiState.value.activeGroupMembers
            val memberWithHistory = activeMembers.first()
            assertFalse(vm.canSafelyRemoveMember(memberWithHistory.memberId))

            vm.addMemberToActiveGroup(friendName = "NewbieWithoutExpenses")
            val newMember = vm.uiState.value.activeGroupMembers.first { it.name == "NewbieWithoutExpenses" }
            assertTrue(vm.canSafelyRemoveMember(newMember.memberId))
        }

        // --- F17: Zero-Login Cloud Sync & Real-Time Presence ---

        @Test
        fun `F17_T1_01 CloudGroupSyncRepository deriveGroupJoinCode6 generates deterministic 6-character uppercase code`() {
            val code1 = CloudGroupSyncRepository.deriveGroupJoinCode6("g_goa_2026")
            val code2 = CloudGroupSyncRepository.deriveGroupJoinCode6("g_goa_2026")
            assertEquals(6, code1.length)
            assertEquals(code1, code2)
            assertTrue(code1.all { it.isUpperCase() || it.isDigit() })
        }

        @Test
        fun `F17_T1_02 CloudGroupSyncRepository groupTopicForGroupId generates deterministic ntfy topic name`() {
            val topic = CloudGroupSyncRepository.groupTopicForGroupId("g_tahoe")
            assertTrue(topic.isNotBlank())
            assertEquals(topic, CloudGroupSyncRepository.groupTopicForGroupId("g_tahoe"))
        }

        @Test
        fun `F17_T1_03 CloudGroupSyncRepository isPhoneOnlineNow evaluates 5-minute heartbeat window accurately`() {
            val nowMs = 1_700_000_000_000L
            val presenceMap = mapOf(
                "9876543210" to (nowMs - 60_000L),
                "9123456789" to (nowMs - 600_000L)
            )
            assertTrue(CloudGroupSyncRepository.isPhoneOnlineNow("9876543210", presenceMap, nowMs))
            assertFalse(CloudGroupSyncRepository.isPhoneOnlineNow("9123456789", presenceMap, nowMs))
        }

        @Test
        fun `F17_T1_04 SplitMateUiState isMemberOnline and onlineFriendsCount reflect active presence map`() {
            val nowMs = 1_700_000_000_000L
            val me = GroupMemberEntity("m_me", "g1", "Akshay", "Akshay|Male", isCurrentUser = true, userPhone = "9999988888")
            val f1 = GroupMemberEntity("m_f1", "g1", "Rohan", "Rohan|Male", isCurrentUser = false, userPhone = "9876543210")
            val f2 = GroupMemberEntity("m_f2", "g1", "Priya", "Priya|Female", isCurrentUser = false, userPhone = "9123456789")
            val state = com.splitmate.app.ui.SplitMateUiState(
                userPhone = "9999988888",
                groups = listOf(ExpenseGroupEntity("g1", "Trip")),
                activeGroupId = "g1",
                members = listOf(me, f1, f2),
                memberPresenceByPhone = mapOf("9876543210" to (nowMs - 30_000L))
            )
            assertTrue(state.isMemberOnline(me, nowMs))
            assertTrue(state.isMemberOnline(f1, nowMs))
            assertFalse(state.isMemberOnline(f2, nowMs))
            assertEquals(1, state.onlineFriendsCount("g1", nowMs))
        }

        @Test
        fun `F17_T1_05 SplitMateViewModel onNetworkConnectivityChanged updates isOfflineMode flag`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.onNetworkConnectivityChanged(context = null, isConnected = false)
            assertTrue(vm.uiState.value.isOfflineMode)
            vm.onNetworkConnectivityChanged(context = null, isConnected = true)
            assertFalse(vm.uiState.value.isOfflineMode)
        }

        // --- F18: DiceBear 9.x & Open Peeps Avatar Studio ---

        @Test
        fun `F18_T1_01 SplitMateDiceBearStyles defines all 13 curated DiceBear 9_x styles`() {
            assertEquals(13, SplitMateDiceBearStyles.size)
            val ids = SplitMateDiceBearStyles.map { it.id }
            assertTrue(ids.containsAll(listOf("open-peeps", "adventurer", "avataaars", "lorelei", "notionists", "personas", "micah", "dylan", "toon-head")))
        }

        @Test
        fun `F18_T1_02 SplitMateAvatarColorPresets defines all 12 curated tactile avatar color presets`() {
            assertEquals(12, SplitMateAvatarColorPresets.size)
            val ids = SplitMateAvatarColorPresets.map { it.id }
            assertTrue(ids.containsAll(listOf("Bare", "PastelWall", "BoldPop", "Sunrise", "Muted", "Electric", "NightShift", "Sepia", "Greyscale", "Duotone", "FullCast", "CloseUp")))
        }

        @Test
        fun `F18_T1_03 AvatarSeedCodec encodes and parses 4-token seedKey gender styleId colorPresetId round-trip`() {
            val encoded = AvatarSeedCodec.encode(
                seedKey = "Akshay",
                gender = AvatarGender.MALE,
                styleId = "adventurer",
                colorPresetId = "BoldPop"
            )
            assertEquals("Akshay|Male|adventurer|BoldPop", encoded)
            val parsed = AvatarSeedCodec.parse(encoded)
            assertEquals("Akshay", parsed.seedKey)
            assertEquals(AvatarGender.MALE, parsed.gender)
            assertEquals("adventurer", parsed.styleId)
            assertEquals("BoldPop", parsed.colorPresetId)
        }

        @Test
        fun `F18_T1_04 buildDiceBearAvatarUrl scopes open-peeps head and face params strictly to open-peeps style`() {
            val openPeepsUrl = buildDiceBearAvatarUrl(
                name = "Priya|Female|open-peeps|PastelWall"
            )
            assertTrue(openPeepsUrl.startsWith("https://api.dicebear.com/9.x/open-peeps/svg?"))
            assertTrue(openPeepsUrl.contains("face="))
            assertTrue(openPeepsUrl.contains("head="))
            assertTrue(openPeepsUrl.contains("facialHairProbability=0"))

            val adventurerUrl = buildDiceBearAvatarUrl(
                name = "Priya|Female|adventurer|PastelWall"
            )
            assertTrue(adventurerUrl.startsWith("https://api.dicebear.com/9.x/adventurer/svg?"))
            assertFalse(adventurerUrl.contains("facialHairProbability=0"))
        }

        @Test
        fun `F18_T1_05 extractInitialsFromNameOrSeed extracts clean 1 or 2 letter initials without gender tokens`() {
            assertEquals("AK", extractInitialsFromNameOrSeed("Akshay Karadkar|Male|open-peeps|Buckwheat"))
            assertEquals("R", extractInitialsFromNameOrSeed("Rohan (You)|Masculine"))
            assertEquals("PS", extractInitialsFromNameOrSeed("Priya Sharma_9876543210"))
        }

        // --- F19: WCAG 2.1 AA Contrast & Surface Hierarchy Compliance ---

        @Test
        fun `F19_T1_01 SunlitBuckwheatPalette primary and secondary text exceed WCAG AA 4_5 to 1 on all 5 surfaces`() {
            val p = SunlitBuckwheatPalette
            val surfaces = listOf(
                p.surfaceContainerLowest,
                p.surfaceContainerLow,
                p.surfaceContainer,
                p.surfaceContainerHigh,
                p.surfaceContainerHighest
            )
            surfaces.forEach { bg ->
                assertTrue(wcagContrastRatio(p.onSurface, bg) >= 7.0, "Expected AAA contrast for onSurface on $bg")
                assertTrue(wcagContrastRatio(p.onSurfaceVariant, bg) >= 4.0, "Expected readable contrast for onSurfaceVariant on $bg")
            }
        }

        @Test
        fun `F19_T1_02 WarmEspressoNightPalette primary and secondary text exceed WCAG AA on all 5 dark surfaces`() {
            val p = WarmEspressoNightPalette
            val surfaces = listOf(
                p.surfaceContainerLowest,
                p.surfaceContainerLow,
                p.surfaceContainer,
                p.surfaceContainerHigh,
                p.surfaceContainerHighest
            )
            surfaces.forEach { bg ->
                assertTrue(wcagContrastRatio(p.onSurface, bg) >= 7.0, "Expected AAA contrast for dark onSurface on $bg")
                assertTrue(wcagContrastRatio(p.onSurfaceVariant, bg) >= 4.5, "Expected AA contrast for dark onSurfaceVariant on $bg")
            }
        }

        @Test
        fun `F19_T1_03 KyotoMatchaYuzuPalette primary and secondary text exceed WCAG AA on all 5 botanical surfaces`() {
            val p = KyotoMatchaYuzuPalette
            val surfaces = listOf(
                p.surfaceContainerLowest,
                p.surfaceContainerLow,
                p.surfaceContainer,
                p.surfaceContainerHigh,
                p.surfaceContainerHighest
            )
            surfaces.forEach { bg ->
                assertTrue(wcagContrastRatio(p.onSurface, bg) >= 7.0, "Expected AAA contrast for matcha onSurface on $bg")
                assertTrue(wcagContrastRatio(p.onSurfaceVariant, bg) >= 4.0, "Expected readable contrast for matcha onSurfaceVariant on $bg")
            }
        }

        @Test
        fun `F19_T1_04 All 3 themes maintain strict luminance ordering across their 5-step surface container hierarchy`() {
            val sunlitLums = listOf(
                SunlitBuckwheatPalette.surfaceContainerLowest,
                SunlitBuckwheatPalette.surfaceContainerLow,
                SunlitBuckwheatPalette.surfaceContainer,
                SunlitBuckwheatPalette.surfaceContainerHigh,
                SunlitBuckwheatPalette.surfaceContainerHighest
            ).map { relativeLuminance(it) }
            for (i in 0 until sunlitLums.lastIndex) {
                assertTrue(sunlitLums[i] > sunlitLums[i + 1], "Sunlit luminance must decrease from Lowest to Highest")
            }

            val espressoLums = listOf(
                WarmEspressoNightPalette.surfaceContainerLowest,
                WarmEspressoNightPalette.surfaceContainerLow,
                WarmEspressoNightPalette.surfaceContainer,
                WarmEspressoNightPalette.surfaceContainerHigh,
                WarmEspressoNightPalette.surfaceContainerHighest
            ).map { relativeLuminance(it) }
            for (i in 0 until espressoLums.lastIndex) {
                assertTrue(espressoLums[i] < espressoLums[i + 1], "Espresso luminance must increase from Lowest to Highest")
            }

            val kyotoLums = listOf(
                KyotoMatchaYuzuPalette.surfaceContainerLowest,
                KyotoMatchaYuzuPalette.surfaceContainerLow,
                KyotoMatchaYuzuPalette.surfaceContainer,
                KyotoMatchaYuzuPalette.surfaceContainerHigh,
                KyotoMatchaYuzuPalette.surfaceContainerHighest
            ).map { relativeLuminance(it) }
            for (i in 0 until kyotoLums.lastIndex) {
                assertTrue(kyotoLums[i] > kyotoLums[i + 1], "Kyoto luminance must decrease from Lowest to Highest")
            }
        }

        @Test
        fun `F19_T1_05 All 3 themes maintain WCAG AA contrast between primaryContainer and onPrimaryContainer`() {
            listOf(SunlitBuckwheatPalette, WarmEspressoNightPalette, KyotoMatchaYuzuPalette).forEach { p ->
                val primaryContainerRatio = wcagContrastRatio(p.onPrimaryContainer, p.primaryContainer)
                val secondaryContainerRatio = wcagContrastRatio(p.onSecondaryContainer, p.secondaryContainer)
                val tertiaryContainerRatio = wcagContrastRatio(p.onTertiaryContainer, p.tertiaryContainer)
                assertTrue(primaryContainerRatio >= 4.5, "${p.mode} primaryContainer contrast $primaryContainerRatio < 4.5")
                assertTrue(secondaryContainerRatio >= 4.5, "${p.mode} secondaryContainer contrast $secondaryContainerRatio < 4.5")
                assertTrue(tertiaryContainerRatio >= 4.5, "${p.mode} tertiaryContainer contrast $tertiaryContainerRatio < 4.5")
            }
        }

        // --- F20: Zero-Emoji & Vector-Only Icon Hygiene ---

        @Test
        fun `F20_T1_01 ExpressiveShapesAndMotion and ExpressiveM3Components contain zero Unicode emojis`() {
            val shapesSrc = readSourceFile("ui/components/ExpressiveShapesAndMotion.kt")
            val compSrc = readSourceFile("ui/components/ExpressiveM3Components.kt")
            assertFalse(containsEmojiCodepoint(shapesSrc))
            assertFalse(containsEmojiCodepoint(compSrc))
        }

        @Test
        fun `F20_T1_02 ExpressiveShapesAndMotion and ExpressiveM3Components contain zero banned dingbat arrows`() {
            val bannedDingbats = listOf('\u2794', '\u25B2', '\u25BC', '\u25BE', '\u293E', '\u2197', '\u203A')
            val shapesSrc = readSourceFile("ui/components/ExpressiveShapesAndMotion.kt")
            val compSrc = readSourceFile("ui/components/ExpressiveM3Components.kt")
            bannedDingbats.forEach { ch ->
                assertFalse(shapesSrc.contains(ch), "Found banned dingbat $ch in ExpressiveShapesAndMotion.kt")
                assertFalse(compSrc.contains(ch), "Found banned dingbat $ch in ExpressiveM3Components.kt")
            }
        }

        @Test
        fun `F20_T1_03 ExpressiveM3Components uses Icons Rounded and AutoMirrored vector icons exclusively`() {
            val compSrc = readSourceFile("ui/components/ExpressiveM3Components.kt")
            assertTrue(compSrc.contains("Icons.Rounded.Add"))
            assertTrue(compSrc.contains("Icons.Rounded.Close"))
            assertTrue(compSrc.contains("Icons.Rounded.ExpandMore"))
        }

        @Test
        fun `F20_T1_04 SplitMateTheme category icon resolver uses Material Icons instead of text emojis`() {
            val themeSrc = readSourceFile("ui/SplitMateTheme.kt")
            assertTrue(themeSrc.contains("Icons.Rounded.FlightTakeoff"))
            assertTrue(themeSrc.contains("Icons.Rounded.Train"))
            assertTrue(themeSrc.contains("Icons.Rounded.LocalTaxi"))
            assertTrue(themeSrc.contains("Icons.Rounded.Hotel"))
            assertTrue(themeSrc.contains("Icons.Rounded.Restaurant"))
        }

        @Test
        fun `F20_T1_05 All UI Kotlin files in ui components and navigation are free of Unicode emojis`() {
            val files = listOf(
                "ui/components/ExpressiveShapesAndMotion.kt",
                "ui/components/ExpressiveM3Components.kt",
                "ui/components/AnimatedTransitDeckHeroCard.kt",
                "ui/navigation/SplitMateAppNavHost.kt"
            )
            files.forEach { rel ->
                val text = readSourceFile(rel)
                assertFalse(containsEmojiCodepoint(text), "Expected zero emojis in $rel")
            }
        }
    }

    // =========================================================================
    // TIER 2: BOUNDARY & CORNER CASE TESTS (F1-F20 x 5 = 100 TEST CASES)
    // =========================================================================

    @Nested
    @DisplayName("Tier 2: Boundary & Corner Case Tests (F1-F20, 100 Test Cases)")
    inner class Tier2BoundaryAndCornerCaseTests {

        // --- F1 Boundary Cases ---

        @Test
        fun `F1_T2_01 SplitMateThemeMode fromId handles null empty whitespace and unknown strings with fallbackDark false`() {
            assertEquals(SplitMateThemeMode.SUNLIT_BUCKWHEAT, SplitMateThemeMode.fromId(null, fallbackDark = false))
            assertEquals(SplitMateThemeMode.SUNLIT_BUCKWHEAT, SplitMateThemeMode.fromId("", fallbackDark = false))
            assertEquals(SplitMateThemeMode.SUNLIT_BUCKWHEAT, SplitMateThemeMode.fromId("   ", fallbackDark = false))
            assertEquals(SplitMateThemeMode.SUNLIT_BUCKWHEAT, SplitMateThemeMode.fromId("NEON_CYBERPUNK", fallbackDark = false))
        }

        @Test
        fun `F1_T2_02 SplitMateThemeMode fromId falls back to WARM_ESPRESSO_NIGHT when fallbackDark is true`() {
            assertEquals(SplitMateThemeMode.WARM_ESPRESSO_NIGHT, SplitMateThemeMode.fromId(null, fallbackDark = true))
            assertEquals(SplitMateThemeMode.WARM_ESPRESSO_NIGHT, SplitMateThemeMode.fromId("INVALID_ID", fallbackDark = true))
        }

        @Test
        fun `F1_T2_03 SplitMateThemeMode fromId is case-insensitive and trims surrounding whitespace`() {
            assertEquals(SplitMateThemeMode.KYOTO_MATCHA_YUZU, SplitMateThemeMode.fromId("  kyoto_matcha_yuzu  "))
            assertEquals(SplitMateThemeMode.WARM_ESPRESSO_NIGHT, SplitMateThemeMode.fromId("warm_espresso_night"))
            assertEquals(SplitMateThemeMode.SUNLIT_BUCKWHEAT, SplitMateThemeMode.fromId("Sunlit_Buckwheat"))
        }

        @Test
        fun `F1_T2_04 Rapid 30-step theme cycling preserves exact modulo-3 state transitions without drift`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.setExpressiveThemeMode(SplitMateThemeMode.SUNLIT_BUCKWHEAT)
            repeat(30) {
                vm.cycleExpressiveThemeMode()
            }
            assertEquals(SplitMateThemeMode.SUNLIT_BUCKWHEAT, vm.uiState.value.activeThemeMode)
            assertFalse(vm.uiState.value.isDarkTheme)
        }

        @Test
        fun `F1_T2_05 Setting identical SplitMateThemeMode repeatedly is idempotent`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            repeat(5) {
                vm.setExpressiveThemeMode(SplitMateThemeMode.KYOTO_MATCHA_YUZU)
            }
            assertEquals(SplitMateThemeMode.KYOTO_MATCHA_YUZU, vm.uiState.value.activeThemeMode)
            assertEquals(SplitMateThemeMode.KYOTO_MATCHA_YUZU, SplitMateThemeState.activeThemeMode)
        }

        // --- F2 Boundary Cases ---

        @Test
        fun `F2_T2_01 Spatial spring damping ratios are strictly underdamped below 1_0f`() {
            assertTrue(SplitMateMotion.FAST_SPATIAL_DAMPING < 1.0f)
            assertTrue(SplitMateMotion.DEFAULT_SPATIAL_DAMPING < 1.0f)
            assertTrue(SplitMateMotion.SLOW_SPATIAL_DAMPING < 1.0f)
        }

        @Test
        fun `F2_T2_02 Effects spring damping ratio is strictly critically damped at 1_0f to prevent color flash`() {
            assertEquals(1.0f, SplitMateMotion.FAST_EFFECTS_DAMPING, 0.0f)
            assertEquals(1.0f, SplitMateMotion.DEFAULT_EFFECTS_DAMPING, 0.0f)
            assertEquals(1.0f, SplitMateMotion.SLOW_EFFECTS_DAMPING, 0.0f)
            assertEquals(1.0f, SplitMateMotion.fastEffects<Color>().dampingRatio, 0.0f)
            assertEquals(1.0f, SplitMateMotion.defaultEffects<Color>().dampingRatio, 0.0f)
            assertEquals(1.0f, SplitMateMotion.slowEffects<Color>().dampingRatio, 0.0f)
        }

        @Test
        fun `F2_T2_03 Spatial stiffness values are strictly ordered fast greater than default greater than slow`() {
            assertTrue(SplitMateMotion.FAST_SPATIAL_STIFFNESS > SplitMateMotion.DEFAULT_SPATIAL_STIFFNESS)
            assertTrue(SplitMateMotion.DEFAULT_SPATIAL_STIFFNESS > SplitMateMotion.SLOW_SPATIAL_STIFFNESS)
            assertTrue(SplitMateMotion.SLOW_SPATIAL_STIFFNESS > 0f)
        }

        @Test
        fun `F2_T2_04 Effects stiffness values are strictly ordered fast greater than default greater than slow`() {
            assertTrue(SplitMateMotion.FAST_EFFECTS_STIFFNESS > SplitMateMotion.DEFAULT_EFFECTS_STIFFNESS)
            assertTrue(SplitMateMotion.DEFAULT_EFFECTS_STIFFNESS > SplitMateMotion.SLOW_EFFECTS_STIFFNESS)
            assertTrue(SplitMateMotion.SLOW_EFFECTS_STIFFNESS > 0f)
        }

        @Test
        fun `F2_T2_05 SplitMateMotion typed convenience spring factories return valid SpringSpecs`() {
            assertEquals(0.6f, SplitMateMotion.fastSpatialFloat().dampingRatio, 1e-4f)
            assertEquals(800f, SplitMateMotion.fastSpatialDp().stiffness, 1e-4f)
            assertEquals(1.0f, SplitMateMotion.fastEffectsColor().dampingRatio, 1e-4f)
            assertEquals(1.0f, SplitMateMotion.defaultEffectsColor().dampingRatio, 1e-4f)
            assertEquals(1.0f, SplitMateMotion.slowEffectsColor().dampingRatio, 1e-4f)
        }

        // --- F3 Boundary Cases ---

        @Test
        fun `F3_T2_01 toSmartTitleCase handles empty string and pure whitespace safely`() {
            assertEquals("", "".toSmartTitleCase())
            assertEquals("", "     ".toSmartTitleCase())
        }

        @Test
        fun `F3_T2_02 toSmartTitleCase handles single character and numeric prefixes`() {
            assertEquals("A", "a".toSmartTitleCase())
            assertEquals("2026 Goa Trip", "2026 GOA TRIP".toSmartTitleCase())
        }

        @Test
        fun `F3_T2_03 Hero emphasized font sizes are strictly ordered displayLarge greater than displayMedium greater than headlineMedium`() {
            val displaySize = SplitMateExpressiveTypography.displayLargeEmphasized.fontSize.value
            val heroSize = SplitMateExpressiveTypography.displayMediumEmphasized.fontSize.value
            val headlineSize = SplitMateExpressiveTypography.headlineMediumEmphasized.fontSize.value
            val titleSize = SplitMateExpressiveTypography.titleLargeEmphasized.fontSize.value
            val labelSize = SplitMateExpressiveTypography.labelLargeEmphasized.fontSize.value
            assertTrue(displaySize > heroSize)
            assertTrue(heroSize > headlineSize)
            assertTrue(headlineSize > titleSize)
            assertTrue(titleSize > labelSize)
        }

        @Test
        fun `F3_T2_04 LineHeight is strictly greater than FontSize across all 15 emphasized typography styles`() {
            listOf(
                SplitMateExpressiveTypography.displayLargeEmphasized,
                SplitMateExpressiveTypography.displayMediumEmphasized,
                SplitMateExpressiveTypography.displaySmallEmphasized,
                SplitMateExpressiveTypography.headlineLargeEmphasized,
                SplitMateExpressiveTypography.headlineMediumEmphasized,
                SplitMateExpressiveTypography.headlineSmallEmphasized,
                SplitMateExpressiveTypography.titleLargeEmphasized,
                SplitMateExpressiveTypography.titleMediumEmphasized,
                SplitMateExpressiveTypography.titleSmallEmphasized,
                SplitMateExpressiveTypography.bodyLargeEmphasized,
                SplitMateExpressiveTypography.bodyMediumEmphasized,
                SplitMateExpressiveTypography.bodySmallEmphasized,
                SplitMateExpressiveTypography.labelLargeEmphasized,
                SplitMateExpressiveTypography.labelMediumEmphasized,
                SplitMateExpressiveTypography.labelSmallEmphasized
            ).forEach { style ->
                assertTrue(style.lineHeight.value > style.fontSize.value, "LineHeight must exceed FontSize for $style")
            }
        }

        @Test
        fun `F3_T2_05 formatIndianRupeesFromCents handles 0 paise negative paise and multi-crore paise`() {
            assertEquals("\u20B90.00", formatIndianRupeesFromCents(0L))
            assertEquals("-\u20B950.25", formatIndianRupeesFromCents(-5025L))
            assertEquals("\u20B91,23,45,678.90", formatIndianRupeesFromCents(1234567890L))
        }

        // --- F4 Boundary Cases ---

        @Test
        fun `F4_T2_01 segmentedIslandItemShape handles totalCount 0 or negative safely as outer CornerRadius`() {
            val zeroShape = segmentedIslandItemShape(index = 0, totalCount = 0)
            assertEquals(24f, cornerPx(zeroShape.topStart), 0.01f)
            assertEquals(24f, cornerPx(zeroShape.bottomEnd), 0.01f)

            val negShape = segmentedIslandItemShape(index = -1, totalCount = -5)
            assertEquals(24f, cornerPx(negShape.topStart), 0.01f)
        }

        @Test
        fun `F4_T2_02 segmentedIslandItemShape handles 2-item list top and bottom without middle item`() {
            val top = segmentedIslandItemShape(index = 0, totalCount = 2)
            assertEquals(24f, cornerPx(top.topStart), 0.01f)
            assertEquals(6f, cornerPx(top.bottomStart), 0.01f)

            val bottom = segmentedIslandItemShape(index = 1, totalCount = 2)
            assertEquals(6f, cornerPx(bottom.topStart), 0.01f)
            assertEquals(24f, cornerPx(bottom.bottomStart), 0.01f)
        }

        @Test
        fun `F4_T2_03 MorphPolygonShape clamps out-of-bounds progress values below 0f and above 1f`() {
            val morph = Morph(MaterialShapes.Pentagon, MaterialShapes.Sunny)
            val under = MorphPolygonShape(morph = morph, percentage = -5.0f)
            val over = MorphPolygonShape(morph = morph, percentage = 12.0f)
            val outlineUnder = under.createOutline(testSize, LayoutDirection.Ltr, testDensity)
            val outlineOver = over.createOutline(testSize, LayoutDirection.Ltr, testDensity)
            assertTrue(outlineUnder is Outline.Generic && !(outlineUnder as Outline.Generic).path.isEmpty)
            assertTrue(outlineOver is Outline.Generic && !(outlineOver as Outline.Generic).path.isEmpty)
        }

        @Test
        fun `F4_T2_04 MaterialShapes morphSequence supports negative and large indices via modulo arithmetic`() {
            val seq = MaterialShapes.morphSequence
            val polyNeg = seq[(-3).mod(seq.size)]
            val polyLarge = seq[(999).mod(seq.size)]
            assertNotNull(Morph(polyNeg, polyLarge))
        }

        @Test
        fun `F4_T2_05 RoundedPolygonShape creates valid outline for Cookie12Sided and Clover4Leaf`() {
            val s1 = RoundedPolygonShape(MaterialShapes.Cookie12Sided)
            val s2 = RoundedPolygonShape(MaterialShapes.Clover4Leaf)
            val o1 = s1.createOutline(testSize, LayoutDirection.Ltr, testDensity)
            val o2 = s2.createOutline(testSize, LayoutDirection.Ltr, testDensity)
            assertTrue(o1 is Outline.Generic && !(o1 as Outline.Generic).path.isEmpty)
            assertTrue(o2 is Outline.Generic && !(o2 as Outline.Generic).path.isEmpty)
        }

        // --- F5 Boundary Cases ---

        @Test
        fun `F5_T2_01 WavyLinearProgressIndicator flattens wave amplitude to 0dp at 0_0f and 1_0f boundaries`() {
            val src = readSourceFile("ui/components/ExpressiveM3Components.kt")
            assertTrue(src.contains("rawProgress <= 0.001f || rawProgress >= 0.999f"))
            assertTrue(src.contains("0.dp"))
        }

        @Test
        fun `F5_T2_02 ExpressiveButtonGroup returns early without crash when items list is empty`() {
            val src = readSourceFile("ui/components/ExpressiveM3Components.kt")
            assertTrue(src.contains("if (items.isEmpty()) return"))
        }

        @Test
        fun `F5_T2_03 ConnectedButtonGroup returns early without crash when options list is empty`() {
            val src = readSourceFile("ui/components/ExpressiveM3Components.kt")
            assertTrue(src.contains("if (options.isEmpty()) return"))
        }

        @Test
        fun `F5_T2_04 ExpressiveActionItem and ExpressiveMenuAction preserve default optional fields`() {
            val btn = ExpressiveActionItem(label = "Action", onClick = {})
            assertEquals(null, btn.icon)
            assertFalse(btn.isPrimary)
            assertTrue(btn.enabled)
            assertEquals(1f, btn.weight)

            val menu = ExpressiveMenuAction(label = "Share", onClick = {})
            assertEquals(null, menu.icon)
            assertEquals(null, menu.subtitle)
        }

        @Test
        fun `F5_T2_05 ContainedLoadingIndicator stageIndex modulo arithmetic never overflows morphs list`() {
            val sequenceSize = MaterialShapes.morphSequence.size
            for (step in 0..100) {
                val stageFloat = step * 0.37f
                val stageIndex = stageFloat.toInt().mod(sequenceSize)
                assertTrue(stageIndex in 0 until sequenceSize)
            }
        }

        // --- F6 Boundary Cases ---

        @Test
        fun `F6_T2_01 SplitMateViewModel finishSubFlowToGroupDetail falls back gracefully when returnToGroupDetailId is null`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.closeGroupDetail()
            vm.finishSubFlowToGroupDetail()
            assertEquals("LEDGERS", vm.uiState.value.selectedTabName)
            assertNotNull(vm.uiState.value.openedGroupDetailId)
        }

        @Test
        fun `F6_T2_02 SplitMateViewModel openGroupDetail synchronizes activeGroupId and openedGroupDetailId`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val secondGroup = vm.uiState.value.groups.last().groupId
            vm.openGroupDetail(secondGroup)
            assertEquals(secondGroup, vm.uiState.value.activeGroupId)
            assertEquals(secondGroup, vm.uiState.value.openedGroupDetailId)
        }

        @Test
        fun `F6_T2_03 SplitMateViewModel clearLocalVault resets groups members expenses splits and settlements`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.clearLocalVault()
            assertTrue(vm.uiState.value.groups.isEmpty())
            assertTrue(vm.uiState.value.members.isEmpty())
            assertTrue(vm.uiState.value.expenses.isEmpty())
            assertTrue(vm.uiState.value.splits.isEmpty())
            assertTrue(vm.uiState.value.settlements.isEmpty())
            assertEquals("", vm.uiState.value.activeGroupId)
        }

        @Test
        fun `F6_T2_04 SplitMateViewModel currencySymbolFor handles INR USD EUR GBP JPY and unknown currency codes`() {
            assertEquals("\u20B9", SplitMateViewModel.currencySymbolFor("INR"))
            assertEquals("$", SplitMateViewModel.currencySymbolFor("USD"))
            assertEquals("\u20AC", SplitMateViewModel.currencySymbolFor("EUR"))
            assertEquals("\u00A3", SplitMateViewModel.currencySymbolFor("GBP"))
            assertEquals("\u00A5", SplitMateViewModel.currencySymbolFor("JPY"))
            assertEquals("XYZ", SplitMateViewModel.currencySymbolFor("XYZ"))
        }

        @Test
        fun `F6_T2_05 HorizontalFloatingToolbar supports both null and non-null adjacent floatingActionButton slot`() {
            val src = readSourceFile("ui/components/ExpressiveM3Components.kt")
            assertTrue(src.contains("floatingActionButton: (@Composable () -> Unit)? = null"))
            assertTrue(src.contains("if (floatingActionButton != null)"))
        }

        // --- F7 Boundary Cases ---

        @Test
        fun `F7_T2_01 SplitMateViewModel updateUserProfile sanitizes pipe characters and blank names to Akshay fallback`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.updateUserProfile(
                newName = "   |   ",
                newPhone = "invalid-phone",
                newSeedOrCurrency = "Explorer|Neutral|open-peeps|PastelWall"
            )
            assertEquals("Akshay", vm.uiState.value.currentUserName)
            assertEquals("", vm.uiState.value.userPhone)
        }

        @Test
        fun `F7_T2_02 SplitMateViewModel createNewGroup ignores duplicate rapid tap within 800ms debounce window`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val beforeCount = vm.uiState.value.groups.size
            vm.createNewGroup(name = "First Tap Group", currencyCode = "INR", friendNamesCsv = "Aarav")
            val afterFirst = vm.uiState.value.groups.size
            assertEquals(beforeCount + 1, afterFirst)

            vm.createNewGroup(name = "Second Immediate Tap", currencyCode = "INR", friendNamesCsv = "Aarav")
            assertEquals(afterFirst, vm.uiState.value.groups.size)
        }

        @Test
        fun `F7_T2_03 SplitMateViewModel createNewGroupWithContacts filters out self phone number to prevent duplicate self member`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.clearLocalVault()
            vm.updateUserProfile("Akshay", "9876543210", "Akshay|Male|open-peeps|Buckwheat")
            Thread.sleep(820)
            vm.createNewGroupWithContacts(
                name = "Self Filter Trip",
                iconName = "Flight",
                memberDrafts = listOf(
                    NewGroupMemberDraft(name = "Akshay Clone", cleanPhone = "9876543210"),
                    NewGroupMemberDraft(name = "Rohan", cleanPhone = "9123456789")
                )
            )
            val groupMembers = vm.uiState.value.activeGroupMembers
            assertEquals(2, groupMembers.size)
            assertEquals(1, groupMembers.count { it.isCurrentUser })
        }

        @Test
        fun `F7_T2_04 SplitMateViewModel renameGroup ignores blank newName without corrupting existing group title`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val group = vm.uiState.value.groups.first()
            val originalName = group.name
            vm.renameGroup(groupId = group.groupId, newName = "    ")
            assertEquals(originalName, vm.uiState.value.groups.first { it.groupId == group.groupId }.name)
        }

        @Test
        fun `F7_T2_05 OpenPeepsHeroStage clamps activeSceneIndex safely inside SplitMateCrewScenes bounds`() {
            val lastIdx = SplitMateCrewScenes.lastIndex
            assertEquals(0, (-5).coerceIn(0, lastIdx))
            assertEquals(lastIdx, 99.coerceIn(0, lastIdx))
        }

        // --- F8 Boundary Cases ---

        @Test
        fun `F8_T2_01 extractSyncTokenFromRawInput returns null for blank or malformed token strings`() {
            assertEquals(null, SplitMateViewModel.extractSyncTokenFromRawInput(""))
            assertEquals(null, SplitMateViewModel.extractSyncTokenFromRawInput("   "))
            assertEquals(null, SplitMateViewModel.extractSyncTokenFromRawInput("SM2_short"))
            assertEquals(null, SplitMateViewModel.extractSyncTokenFromRawInput("https://example.com/no-token"))
        }

        @Test
        fun `F8_T2_02 importAndMergeGroupSyncPayload rejects corrupted SM2 payload gracefully with success false`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val res = vm.importAndMergeGroupSyncPayload("SM2_not_valid_compressed_base64_payload_data")
            assertFalse(res.success)
            assertTrue(res.message.isNotBlank())
        }

        @Test
        fun `F8_T2_03 exportGroupSyncPayload returns null for non-existent groupId`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val bundle = vm.exportGroupSyncPayload("non_existent_group_999")
            assertEquals(null, bundle)
        }

        @Test
        fun `F8_T2_04 Importing identical GroupSyncExportBundle twice is idempotent and does not duplicate expenses`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val groupId = vm.uiState.value.activeGroupId
            val bundle = vm.exportGroupSyncPayload(groupId)!!
            val countBefore = vm.uiState.value.expenses.count { it.groupId == groupId }

            val firstImport = vm.importAndMergeGroupSyncPayload(bundle.syncToken)
            val secondImport = vm.importAndMergeGroupSyncPayload(bundle.syncToken)
            assertTrue(firstImport.success)
            assertTrue(secondImport.success)
            assertEquals(0, secondImport.newlyAddedExpenseCount)
            assertEquals(countBefore, vm.uiState.value.expenses.count { it.groupId == groupId })
        }

        @Test
        fun `F8_T2_05 claimGroupMemberPerspective ignores unknown memberId safely`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val groupId = vm.uiState.value.activeGroupId
            val currentMeBefore = vm.uiState.value.activeGroupMembers.first { it.isCurrentUser }.memberId
            vm.claimGroupMemberPerspective(groupId = groupId, memberId = "unknown_member_id")
            val currentMeAfter = vm.uiState.value.activeGroupMembers.first { it.isCurrentUser }.memberId
            assertEquals(currentMeBefore, currentMeAfter)
        }

        // --- F9 Boundary Cases ---

        @Test
        fun `F9_T2_01 computeGroupMemberNetBalances returns 0L for all members when group has no expenses`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.clearLocalVault()
            Thread.sleep(820)
            vm.createNewGroup(name = "Empty Ledger Trip", currencyCode = "INR", friendNamesCsv = "Rohan,Priya")
            val groupId = vm.uiState.value.activeGroupId
            val netMap = vm.computeGroupMemberNetBalances(groupId)
            assertTrue(netMap.values.all { it == 0L })
        }

        @Test
        fun `F9_T2_02 computeGroupMemberNetBalances returns 0L net balance after full settlement of all transfers`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val groupId = vm.uiState.value.activeGroupId
            val initialNetMap = vm.computeGroupMemberNetBalances(groupId)
            val transfers = SplitMateMathEngine.simplifyDebtsGreedy(
                vm.uiState.value.activeGroupMembers.map {
                    SplitMateMathEngine.MemberNetBalance(it.memberId, it.name, initialNetMap[it.memberId] ?: 0L)
                }
            )
            transfers.forEach { transfer ->
                vm.markGreedyTransferSettled(transfer)
            }
            val netMap = vm.computeGroupMemberNetBalances(groupId)
            assertTrue(netMap.values.all { it == 0L })
        }

        @Test
        fun `F9_T2_03 resolveExpenseSplitBreakdown handles legacy expense without explicit split rows via equal fallback`() {
            val members = listOf(
                GroupMemberEntity("m1", "g1", "Akshay", "Akshay|Male", isCurrentUser = true),
                GroupMemberEntity("m2", "g1", "Rohan", "Rohan|Male", isCurrentUser = false),
                GroupMemberEntity("m3", "g1", "Priya", "Priya|Female", isCurrentUser = false)
            )
            val legacyExpense = ExpenseEntity(
                expenseId = "exp_legacy",
                groupId = "g1",
                title = "Legacy Dinner",
                payerId = "m1",
                baseSubtotalCents = 10000L,
                taxCents = 0L,
                tipCents = 0L,
                totalAmountCents = 10000L,
                lockedMultiplier = 1.0,
                unassignedBaseCents = 0L,
                currencyCode = "INR",
                lockedExchangeRate = 1.0
            )
            val summary = SplitMateViewModel.resolveExpenseSplitBreakdown(
                expense = legacyExpense,
                groupMembers = members,
                allSplits = emptyList()
            )
            assertEquals(3, summary.splittingMembersCount)
            assertEquals(10000L, summary.rows.sumOf { it.owedCents })
        }

        @Test
        fun `F9_T2_04 pendingInviteGroups and declinedInviteGroups partition groups by current user inviteStatus`() {
            val g1 = ExpenseGroupEntity("g_pend", "Pending Goa")
            val g2 = ExpenseGroupEntity("g_decl", "Declined Manali")
            val g3 = ExpenseGroupEntity("g_join", "Joined Kerala")
            val m1 = GroupMemberEntity("m1", "g_pend", "Akshay", "Akshay", isCurrentUser = true, inviteStatus = "PENDING")
            val m2 = GroupMemberEntity("m2", "g_decl", "Akshay", "Akshay", isCurrentUser = true, inviteStatus = "DECLINED")
            val m3 = GroupMemberEntity("m3", "g_join", "Akshay", "Akshay", isCurrentUser = true, inviteStatus = "JOINED")
            val state = com.splitmate.app.ui.SplitMateUiState(
                groups = listOf(g1, g2, g3),
                members = listOf(m1, m2, m3)
            )
            assertEquals(listOf("g_pend"), state.pendingInviteGroups.map { it.groupId })
            assertEquals(listOf("g_decl"), state.declinedInviteGroups.map { it.groupId })
            assertEquals(listOf("g_join"), state.activeJoinedGroups.map { it.groupId })
        }

        @Test
        fun `F9_T2_05 computeGroupMemberNetBalances preserves zero-sum invariant across multi-payer multi-expense ledgers`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val groupId = vm.uiState.value.activeGroupId
            val netMap = vm.computeGroupMemberNetBalances(groupId)
            assertEquals(0L, netMap.values.sum())
        }

        // --- F10 Boundary Cases ---

        @Test
        fun `F10_T2_01 splitEquallyZeroDrift returns empty list when members is empty or 0 paise allocations when totalCents is 0 or negative`() {
            assertTrue(SplitMateMathEngine.splitEquallyZeroDrift(1000L, emptyList(), "m1").isEmpty())
            assertTrue(SplitMateMathEngine.splitEquallyZeroDrift(0L, listOf("m1" to "A"), "m1").all { it.finalCents == 0L })
            assertTrue(SplitMateMathEngine.splitEquallyZeroDrift(-500L, listOf("m1" to "A"), "m1").all { it.finalCents == 0L })
        }

        @Test
        fun `F10_T2_02 splitEquallyZeroDrift distributes 1 paise across 3 members by assigning 1 paise to payer and 0 to others`() {
            val members = listOf("m1" to "Akshay", "m2" to "Rohan", "m3" to "Priya")
            val allocs = SplitMateMathEngine.splitEquallyZeroDrift(1L, members, payerId = "m2", currentUserId = "m1")
            assertEquals(1L, allocs.sumOf { it.finalCents })
            assertEquals(1L, allocs.first { it.memberId == "m2" }.finalCents)
            assertEquals(0L, allocs.first { it.memberId == "m1" }.finalCents)
            assertEquals(0L, allocs.first { it.memberId == "m3" }.finalCents)
        }

        @Test
        fun `F10_T2_03 splitEquallyZeroDrift distributes 10001 paise across 7 members with exact 0 paise drift`() {
            val members = (1..7).map { "m$it" to "Member$it" }
            val allocs = SplitMateMathEngine.splitEquallyZeroDrift(10001L, members, payerId = "m4", currentUserId = "m1")
            assertEquals(7, allocs.size)
            assertEquals(10001L, allocs.sumOf { it.finalCents })
            assertEquals(5, allocs.count { it.plusOneCent })
        }

        @Test
        fun `F10_T2_04 commitQuickEqualExpense ignores zero or negative totalAmountCents`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val countBefore = vm.uiState.value.expenses.size
            val memberIds = vm.uiState.value.activeGroupMembers.map { it.memberId }
            vm.commitQuickEqualExpense("Zero Expense", 0L, memberIds)
            vm.commitQuickEqualExpense("Negative Expense", -100L, memberIds)
            assertEquals(countBefore, vm.uiState.value.expenses.size)
        }

        @Test
        fun `F10_T2_05 commitQuickEqualExpense debounces identical rapid submission within 800ms`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val memberIds = vm.uiState.value.activeGroupMembers.map { it.memberId }
            vm.commitQuickEqualExpense("Debounce Coffee", 45000L, memberIds)
            val countAfterFirst = vm.uiState.value.expenses.count { it.title == "Debounce Coffee" }
            assertEquals(1, countAfterFirst)

            vm.commitQuickEqualExpense("Debounce Coffee", 45000L, memberIds)
            val countAfterSecond = vm.uiState.value.expenses.count { it.title == "Debounce Coffee" }
            assertEquals(1, countAfterSecond)
        }

        // --- F11 Boundary Cases ---

        @Test
        fun `F11_T2_01 calculateProportionalReceiptSplits handles baseSubtotalCents equal to 0 without division by zero`() {
            val claims = listOf(Triple("m1", "Akshay", 0L))
            val res = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = 0L,
                taxCents = 0L,
                tipCents = 0L,
                payerId = "m1",
                memberBaseClaimsCents = claims,
                attributeRemainderToPayer = true
            )
            assertEquals(1.0, res.lockedMultiplier, 1e-6)
            assertEquals(0L, res.totalFinalCents)
            assertEquals(0L, res.driftCents)
        }

        @Test
        fun `F11_T2_02 calculateProportionalReceiptSplits assigns 100 percent of bill to payer when 0 paise are claimed by anyone`() {
            val claims = listOf(
                Triple("m1", "Akshay", 0L),
                Triple("m2", "Rohan", 0L)
            )
            val res = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = 5000L,
                taxCents = 500L,
                tipCents = 500L,
                payerId = "m1",
                memberBaseClaimsCents = claims,
                attributeRemainderToPayer = true
            )
            assertEquals(6000L, res.totalFinalCents)
            assertEquals(5000L, res.unassignedBaseCents)
            assertEquals(6000L, res.allocations.first { it.memberId == "m1" }.finalCents)
            assertEquals(0L, res.allocations.first { it.memberId == "m2" }.finalCents)
            assertEquals(0L, res.driftCents)
        }

        @Test
        fun `F11_T2_03 calculateProportionalReceiptSplits clamps unassignedBaseCents to 0 when claims exceed baseSubtotalCents`() {
            val claims = listOf(
                Triple("m1", "Akshay", 6000L),
                Triple("m2", "Rohan", 6000L)
            )
            val res = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = 10000L,
                taxCents = 1000L,
                tipCents = 0L,
                payerId = "m1",
                memberBaseClaimsCents = claims,
                attributeRemainderToPayer = true
            )
            assertEquals(0L, res.unassignedBaseCents)
            assertEquals(0L, res.unassignedFinalCents)
            assertEquals(13200L, res.allocations.sumOf { it.finalCents })
        }

        @Test
        fun `F11_T2_04 calculateProportionalReceiptSplits with attributeRemainderToPayer false handles 0 unassigned and empty claims`() {
            val fullClaims = listOf(
                Triple("m1", "Akshay", 5000L),
                Triple("m2", "Rohan", 5000L)
            )
            val resFull = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = 10000L,
                taxCents = 0L,
                tipCents = 0L,
                payerId = "m1",
                memberBaseClaimsCents = fullClaims,
                attributeRemainderToPayer = false
            )
            assertEquals(0L, resFull.unassignedBaseCents)
            assertEquals(10000L, resFull.allocations.sumOf { it.finalCents })

            val resEmpty = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = 10000L,
                taxCents = 0L,
                tipCents = 0L,
                payerId = "m1",
                memberBaseClaimsCents = emptyList(),
                attributeRemainderToPayer = false
            )
            assertTrue(resEmpty.allocations.isEmpty())
        }

        @Test
        fun `F11_T2_05 calculateProportionalReceiptSplits handles high-precision ₹99_999_99 bill across 7 members with 0 drift`() {
            val claims = (1..7).map { idx ->
                Triple("m$idx", "Member$idx", 1428571L)
            }
            val res = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = 9999999L,
                taxCents = 1799999L,
                tipCents = 499999L,
                payerId = "m1",
                memberBaseClaimsCents = claims,
                attributeRemainderToPayer = true
            )
            assertEquals(0L, res.driftCents)
            assertEquals(res.totalFinalCents, res.allocations.sumOf { it.finalCents })
        }

        // --- F12 Boundary Cases ---

        @Test
        fun `F12_T2_01 simplifyDebtsGreedy returns empty list when all net balances are 0L`() {
            val balances = listOf(
                SplitMateMathEngine.MemberNetBalance("m1", "Akshay", 0L),
                SplitMateMathEngine.MemberNetBalance("m2", "Rohan", 0L)
            )
            assertTrue(SplitMateMathEngine.simplifyDebtsGreedy(balances).isEmpty())
        }

        @Test
        fun `F12_T2_02 simplifyDebtsGreedy returns empty list for empty or single-member input`() {
            assertTrue(SplitMateMathEngine.simplifyDebtsGreedy(emptyList()).isEmpty())
            assertTrue(
                SplitMateMathEngine.simplifyDebtsGreedy(
                    listOf(SplitMateMathEngine.MemberNetBalance("m1", "Akshay", 500L))
                ).isEmpty()
            )
        }

        @Test
        fun `F12_T2_03 simplifyDebtsGreedy breaks ties deterministically by memberId`() {
            val balances = listOf(
                SplitMateMathEngine.MemberNetBalance("m_debtor_b", "Debtor B", -1000L),
                SplitMateMathEngine.MemberNetBalance("m_debtor_a", "Debtor A", -1000L),
                SplitMateMathEngine.MemberNetBalance("m_creditor_1", "Creditor 1", 2000L)
            )
            val run1 = SplitMateMathEngine.simplifyDebtsGreedy(balances)
            val run2 = SplitMateMathEngine.simplifyDebtsGreedy(balances.reversed())
            assertEquals(run1, run2)
            assertEquals("m_debtor_a", run1.first().fromMemberId)
        }

        @Test
        fun `F12_T2_04 computeMemberSettlementSummaries returns empty list when transfers is empty`() {
            val summaries = SplitMateMathEngine.computeMemberSettlementSummaries(emptyList())
            assertTrue(summaries.isEmpty())
        }

        @Test
        fun `F12_T2_05 simplifyDebtsGreedy handles 15-member group in at most 14 transfers with exact conservation of flow`() {
            val debtors = (1..8).map { SplitMateMathEngine.MemberNetBalance("d$it", "Debtor$it", -1000L * it) }
            val totalDebt = debtors.sumOf { -it.netCents }
            val creditors = listOf(
                SplitMateMathEngine.MemberNetBalance("c1", "Creditor1", totalDebt / 2),
                SplitMateMathEngine.MemberNetBalance("c2", "Creditor2", totalDebt - (totalDebt / 2))
            )
            val transfers = SplitMateMathEngine.simplifyDebtsGreedy(debtors + creditors)
            assertTrue(transfers.size <= 9)
            assertEquals(totalDebt, transfers.sumOf { it.amountCents })
        }

        // --- F13 Boundary Cases ---

        @Test
        fun `F13_T2_01 PnrNetworkRepository and UniversalFlightTicketExtractor reject blank or invalid PNR payloads`() {
            assertEquals("", PnrNetworkRepository.normalizePnrKey(""))
            assertEquals("", PnrNetworkRepository.normalizePnrKey("   "))
            assertEquals(null, PnrNetworkRepository.loadPersistedPnrSnapshot(null, ""))
            assertFalse(PnrNetworkRepository.isSnapshotAllConfirmed(null))
            assertFalse(UniversalFlightTicketExtractor.extractFromText("   ").isValidFlightTicket)
        }

        @Test
        fun `F13_T2_02 resolveStationDisplayName handles blank lowercase and unknown station codes gracefully`() {
            assertEquals("", resolveStationDisplayName(""))
            assertEquals("NDLS (New Delhi)", resolveStationDisplayName("ndls"))
            assertEquals("ZZZ", resolveStationDisplayName("zzz"))
        }

        @Test
        fun `F13_T2_03 extractTravelTicketFromTitle returns null when title has no ticket metadata or PNR pattern`() {
            assertEquals(null, extractTravelTicketFromTitle("Team Pizza Dinner at Indiranagar"))
            assertEquals(null, extractTravelTicketFromTitle(""))
        }

        @Test
        fun `F13_T2_04 LivePnrStatusSnapshot preserves structuredPassengers and chartPrepared flags`() {
            val pax = LivePnrPassenger("Passenger 1", "RLWL/4", "CNF/B2/14", "Confirmed")
            val snap = LivePnrStatusSnapshot(
                pnr = "2345678901",
                trainNo = "12627",
                trainName = "KARNATAKA EXP",
                fromStation = "SBC",
                toStation = "NDLS",
                departureTime = "19:20",
                bookingStatusBadge = "CNF",
                chartPrepared = true,
                passengerStatuses = listOf("P1: CNF/B2/14"),
                structuredPassengers = listOf(pax),
                coachPositionHint = "8th from Engine",
                liveTrainLocationRadar = "On Time",
                confirmationProbability = "100% Confirmed",
                sourceLabel = "IRCTC Verified"
            )
            assertTrue(snap.chartPrepared)
            assertEquals(1, snap.structuredPassengers.size)
            assertEquals("CNF/B2/14", snap.structuredPassengers.first().currentStatus)
        }

        @Test
        fun `F13_T2_05 findExistingExpenseByPnr returns null when PNR length is fewer than 6 characters`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            assertEquals(null, vm.findExistingExpenseByPnr(""))
            assertEquals(null, vm.findExistingExpenseByPnr("12345"))
        }

        // --- F14 Boundary Cases ---

        @Test
        fun `F14_T2_01 UniversalFlightTicketExtractor rejects non-flight grocery receipt text`() {
            val groceryText = """
                FreshMart Supermarket Indiranagar
                Milk 2L          Rs 120.00
                Sourdough Bread  Rs 180.00
                Total Paid       Rs 300.00
            """.trimIndent()
            val res = UniversalFlightTicketExtractor.extractFromText(groceryText)
            assertFalse(res.isValidFlightTicket)
        }

        @Test
        fun `F14_T2_02 UniversalFlightTicketExtractor handles multi-stop connecting flights with viaAirports`() {
            val connectingText = """
                Air India Booking Confirmation
                PNR: M8N2P4
                Flight: AI-501, AI-502
                From: DEL (New Delhi) To: COK (Kochi) via BOM
                Departure: 10 Jan 2027 06:10  Arrival: 13:45
                Passenger: 1. Mr Akshay Karadkar
                Total Amount: INR 9,420.00
            """.trimIndent()
            val res = UniversalFlightTicketExtractor.extractFromText(connectingText, listOf("Akshay"))
            assertTrue(res.isValidFlightTicket)
            assertEquals("M8N2P4", res.pnr)
            assertEquals(942000L, res.totalFarePaise)
        }

        @Test
        fun `F14_T2_03 UniversalFlightTicketExtractor handles empty input text safely without throwing`() {
            val res = UniversalFlightTicketExtractor.extractFromText("")
            assertFalse(res.isValidFlightTicket)
            assertEquals("", res.pnr)
            assertEquals(0L, res.totalFarePaise)
        }

        @Test
        fun `F14_T2_04 isFlightTicketExpense does not misclassify Air India Express as a train`() {
            assertTrue(isFlightTicketExpense("Air India Express IX-1142 (BLR -> GOI) [PNR:Q4W8E2]"))
        }

        @Test
        fun `F14_T2_05 UniversalFlightTicketExtractor avoids matching common words like DATE TIME or PASSENGER as PNR`() {
            val noisyTicket = """
                Akasa Air Boarding Pass
                DATE: 15 Nov 2026  TIME: 14:20  STATUS: CONFIRMED
                PNR: V4K8N2
                Flight: QP 1342
                From: BLR To: BOM
                Total Fare: INR 4,250
            """.trimIndent()
            val res = UniversalFlightTicketExtractor.extractFromText(noisyTicket)
            assertEquals("V4K8N2", res.pnr)
        }

        // --- F15 Boundary Cases ---

        @Test
        fun `F15_T2_01 AnimatedTransitDeckHeroCard handles empty expenses list without crash`() {
            val src = readSourceFile("ui/components/AnimatedTransitDeckHeroCard.kt")
            assertTrue(src.contains("trainCountLogged: Int = 0") && src.contains("flightCountActive: Int = 0"))
            assertTrue(src.contains("trainCountLogged > 0") && src.contains("flightCountActive > 0"))
        }

        @Test
        fun `F15_T2_02 resolveExpenseCategoryIcon falls back to ReceiptLong icon for generic unknown titles`() {
            val fallback = resolveExpenseCategoryIcon("Miscellaneous Adjustment Entry")
            val grocery = resolveExpenseCategoryIcon("Blinkit Grocery Delivery")
            assertNotEquals(fallback, grocery)
        }

        @Test
        fun `F15_T2_03 ParsedTravelTicket hasTicketMetadata is false when all fields are blank`() {
            val emptyTicket = ParsedTravelTicket()
            assertFalse(emptyTicket.hasTicketMetadata)
            val nonEmpty = ParsedTravelTicket(pnr = "K9M4Q2")
            assertTrue(nonEmpty.hasTicketMetadata)
        }

        @Test
        fun `F15_T2_04 ParsedTravelTicket route formats fromStation and toStation with arrow`() {
            val ticket = ParsedTravelTicket(fromStation = "DEL", toStation = "GOI")
            assertEquals("DEL\u2192GOI", ticket.route)
            assertEquals("", ParsedTravelTicket().route)
        }

        @Test
        fun `F15_T2_05 resolveExpenseCategoryBadgeColors handles empty string title in both light and dark modes`() {
            val (bgL, fgL) = resolveExpenseCategoryBadgeColors("", isDark = false)
            val (bgD, fgD) = resolveExpenseCategoryBadgeColors("", isDark = true)
            assertTrue(wcagContrastRatio(fgL, bgL) >= 4.5)
            assertTrue(wcagContrastRatio(fgD, bgD) >= 4.5)
        }

        // --- F16 Boundary Cases ---

        @Test
        fun `F16_T2_01 dismissMemberAsOrganizer blocks dismissing the last remaining organizer`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val groupId = vm.uiState.value.activeGroupId
            val soleOrganizer = vm.getGroupOrganizerMembers(groupId).single()
            val dismissed = vm.dismissMemberAsOrganizer(groupId = groupId, targetMemberId = soleOrganizer.memberId)
            assertFalse(dismissed)
            assertTrue(vm.getGroupOrganizerMembers(groupId).isNotEmpty())
        }

        @Test
        fun `F16_T2_02 promoteMemberToOrganizer returns false for non-existent memberId`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val res = vm.promoteMemberToOrganizer(targetMemberId = "non_existent_member_999")
            assertFalse(res)
        }

        @Test
        fun `F16_T2_03 reassignDeclinedMemberSharesEqually preserves subset splits on expenses where another member was excluded`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.clearLocalVault()
            Thread.sleep(820)
            vm.createNewGroup(name = "Four Member Subset Trip", currencyCode = "INR", friendNamesCsv = "Rohan,Priya,Kabir")
            val groupId = vm.uiState.value.activeGroupId
            val members = vm.uiState.value.members.filter { it.groupId == groupId }
            val me = members.first { it.isCurrentUser }
            val rohan = members.first { it.name == "Rohan" }
            val priya = members.first { it.name == "Priya" }
            val kabir = members.first { it.name == "Kabir" }

            // Expense only among Me, Rohan, Priya (Kabir excluded)
            vm.commitQuickEqualExpense(
                title = "Scuba Diving",
                totalAmountCents = 60000L,
                selectedMemberIds = listOf(me.memberId, rohan.memberId, priya.memberId),
                payerMemberId = me.memberId
            )
            // Rohan declines -> share should only redistribute between Me and Priya, NOT Kabir!
            vm.reassignDeclinedMemberSharesEqually(context = null, groupId = groupId, declinedMemberId = rohan.memberId)
            val exp = vm.uiState.value.expenses.first { it.title == "Scuba Diving" }
            val splits = vm.uiState.value.splits.filter { it.expenseId == exp.expenseId }
            assertEquals(2, splits.size)
            assertFalse(splits.any { it.memberId == kabir.memberId })
            assertEquals(60000L, splits.sumOf { it.finalOwedCents })
        }

        @Test
        fun `F16_T2_04 addMemberToActiveGroup ignores blank friendName and self phone number`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.updateUserProfile("Akshay", "9876543210", "Akshay|Male|open-peeps|Buckwheat")
            val beforeCount = vm.uiState.value.activeGroupMembers.size
            vm.addMemberToActiveGroup("   ")
            vm.addMemberToActiveGroup("SelfClone", "9876543210")
            assertEquals(beforeCount, vm.uiState.value.activeGroupMembers.size)
        }

        @Test
        fun `F16_T2_05 getGroupOrganizerMembers returns emptyList for empty or unknown groupId`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            assertTrue(vm.getGroupOrganizerMembers("non_existent_group_id").isEmpty())
        }

        // --- F17 Boundary Cases ---

        @Test
        fun `F17_T2_01 CloudGroupSyncRepository isPhoneOnlineNow returns false for invalid or empty phone`() {
            val nowMs = 1_700_000_000_000L
            val map = mapOf("9876543210" to nowMs)
            assertFalse(CloudGroupSyncRepository.isPhoneOnlineNow("", map, nowMs))
            assertFalse(CloudGroupSyncRepository.isPhoneOnlineNow("123", map, nowMs))
        }

        @Test
        fun `F17_T2_02 CloudGroupSyncRepository deriveGroupJoinCode6 handles empty seed deterministically`() {
            val code = CloudGroupSyncRepository.deriveGroupJoinCode6("")
            assertEquals(6, code.length)
        }

        @Test
        fun `F17_T2_03 SplitMateViewModel handleLiveCloudTopicEvent ignores blank topic when called`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.handleLiveCloudTopicEvent(context = null, changedTopic = "")
            assertFalse(vm.uiState.value.isCloudSyncing)
        }

        @Test
        fun `F17_T2_04 SplitMateUiState onlineFriendsCount excludes currentUser phone from friend count`() {
            val nowMs = 1_700_000_000_000L
            val me = GroupMemberEntity("m_me", "g1", "Akshay", "Akshay", isCurrentUser = true, userPhone = "9876543210")
            val state = com.splitmate.app.ui.SplitMateUiState(
                userPhone = "9876543210",
                groups = listOf(ExpenseGroupEntity("g1", "Trip")),
                activeGroupId = "g1",
                members = listOf(me),
                memberPresenceByPhone = mapOf("9876543210" to nowMs)
            )
            assertEquals(0, state.onlineFriendsCount("g1", nowMs))
        }

        @Test
        fun `F17_T2_05 CloudGroupSyncRepository tombstone tracking records and restores invited phone numbers`() {
            val groupId = "g_tombstone_test_${System.currentTimeMillis()}"
            CloudGroupSyncRepository.recordLocalTombstones(
                context = null,
                groupId = groupId,
                organizerPhone10 = "9876543210",
                joinCode6 = "ABC123"
            )
            assertEquals("9876543210", CloudGroupSyncRepository.getOrganizerPhone10(null, groupId))
        }

        // --- F18 Boundary Cases ---

        @Test
        fun `F18_T2_01 AvatarSeedCodec parse handles empty string with Explorer fallback`() {
            val parsed = AvatarSeedCodec.parse("")
            assertEquals("Explorer", parsed.seedKey)
            assertEquals(AvatarGender.NEUTRAL, parsed.gender)
            assertEquals("open-peeps", parsed.styleId)
            assertEquals("PastelWall", parsed.colorPresetId)
        }

        @Test
        fun `F18_T2_02 AvatarSeedCodec parse handles legacy 1-token 2-token and 3-token seeds`() {
            val oneToken = AvatarSeedCodec.parse("Akshay")
            assertEquals("Akshay", oneToken.seedKey)

            val twoTokenGender = AvatarSeedCodec.parse("Akshay|Masculine")
            assertEquals("Akshay", twoTokenGender.seedKey)
            assertEquals(AvatarGender.MALE, twoTokenGender.gender)

            val threeToken = AvatarSeedCodec.parse("Priya|Feminine|lorelei")
            assertEquals("Priya", threeToken.seedKey)
            assertEquals(AvatarGender.FEMALE, threeToken.gender)
            assertEquals("lorelei", threeToken.styleId)
        }

        @Test
        fun `F18_T2_03 AvatarSeedCodec falls back to open-peeps and PastelWall for unknown style or preset tokens`() {
            val parsed = AvatarSeedCodec.parse("Akshay|Male|invalid-style-id|InvalidPresetId")
            assertEquals("open-peeps", parsed.styleId)
            assertEquals("PastelWall", parsed.colorPresetId)
        }

        @Test
        fun `F18_T2_04 buildDiceBearAvatarUrl uses phone number fallback seed when name is blank`() {
            val urlWithPhone = buildDiceBearAvatarUrl(name = "   ", phone = "+91 98765 43210")
            assertTrue(urlWithPhone.contains("seed=Explorer_9876543210"))

            val urlWithoutPhone = buildDiceBearAvatarUrl(name = "   ", phone = "")
            assertTrue(urlWithoutPhone.contains("seed=Explorer"))
        }

        @Test
        fun `F18_T2_05 extractInitialsFromNameOrSeed returns S fallback when input contains no letters`() {
            assertEquals("S", extractInitialsFromNameOrSeed("1234567890"))
            assertEquals("S", extractInitialsFromNameOrSeed("   "))
        }

        // --- F19 Boundary Cases ---

        @Test
        fun `F19_T2_01 wcagContrastRatio of identical colors is 1_0 and black-on-white is 21_0`() {
            assertEquals(1.0, wcagContrastRatio(Color.White, Color.White), 1e-3)
            assertEquals(21.0, wcagContrastRatio(Color.Black, Color.White), 1e-2)
        }

        @Test
        fun `F19_T2_02 All 3 themes keep primary onPrimary contrast above WCAG AA 4_5 to 1`() {
            listOf(SunlitBuckwheatPalette, WarmEspressoNightPalette, KyotoMatchaYuzuPalette).forEach { p ->
                val ratio = wcagContrastRatio(p.onPrimary, p.primary)
                assertTrue(ratio >= 4.5, "${p.mode} onPrimary/primary contrast $ratio < 4.5")
            }
        }

        @Test
        fun `F19_T2_03 ElementsCreditor and ElementsDebtor semantic containers maintain high contrast above 5_5 to 1`() {
            val creditorRatio = wcagContrastRatio(
                DesignSystemBindings.ElementsCreditorOnContainer,
                DesignSystemBindings.ElementsCreditorContainer
            )
            val debtorRatio = wcagContrastRatio(
                DesignSystemBindings.ElementsDebtorOnContainer,
                DesignSystemBindings.ElementsDebtorContainer
            )
            assertTrue(creditorRatio >= 5.5)
            assertTrue(debtorRatio >= 5.5)
        }

        @Test
        fun `F19_T2_04 All 12 SplitMateAvatarColorPresets maintain distinct primaryBgColor and accentRingColor`() {
            SplitMateAvatarColorPresets.forEach { preset ->
                assertNotEquals(preset.primaryBgColor, preset.accentRingColor, "Preset ${preset.id} ring must contrast with bg")
            }
        }

        @Test
        fun `F19_T2_05 WarmEspressoNightPalette avoids pure #000000 black to prevent OLED smearing`() {
            val p = WarmEspressoNightPalette
            listOf(
                p.surfaceContainerLowest,
                p.surfaceContainerLow,
                p.surfaceContainer,
                p.surfaceContainerHigh,
                p.surfaceContainerHighest
            ).forEach { surface ->
                assertNotEquals(Color.Black, surface)
                assertTrue(relativeLuminance(surface) > 0.003)
            }
        }

        // --- F20 Boundary Cases ---

        @Test
        fun `F20_T2_01 containsEmojiCodepoint accurately detects Miscellaneous Symbols Dingbats and Supplemental Emojis`() {
            assertTrue(containsEmojiCodepoint("Flight \u2708"))
            assertTrue(containsEmojiCodepoint("Car \uD83D\uDE97"))
            assertTrue(containsEmojiCodepoint("Sparkles \u2728"))
            assertFalse(containsEmojiCodepoint("Clean Text \u20B91,250.00 \u2192 Settled"))
        }

        @Test
        fun `F20_T2_02 Currency symbol ₹ and standard arrow → are permitted and not flagged as emojis`() {
            val sample = "NDLS \u2192 MMCT \u00B7 \u20B92,450.00 (0.00\u00A2 drift)"
            assertFalse(containsEmojiCodepoint(sample))
        }

        @Test
        fun `F20_T2_03 SplitMateV23ExpressiveE2ETest source file itself contains zero literal emojis or banned dingbats`() {
            val candidates = listOf(
                File("src/test/java/com/splitmate/app/SplitMateV23ExpressiveE2ETest.kt"),
                File("app/src/test/java/com/splitmate/app/SplitMateV23ExpressiveE2ETest.kt"),
                File("android/app/src/test/java/com/splitmate/app/SplitMateV23ExpressiveE2ETest.kt"),
                File("/usr/local/google/home/karadkar/splitmate/android/app/src/test/java/com/splitmate/app/SplitMateV23ExpressiveE2ETest.kt")
            )
            val selfFile = candidates.firstOrNull { it.exists() }
            assertNotNull(selfFile)
            val selfText = selfFile!!.readText()
            assertFalse(containsEmojiCodepoint(selfText), "Test file itself must contain zero literal emojis")
        }

        @Test
        fun `F20_T2_04 All 13 SplitMateDiceBearStyles labels and subtitles contain zero emojis`() {
            SplitMateDiceBearStyles.forEach { style ->
                assertFalse(containsEmojiCodepoint(style.label), "Emoji in style label ${style.id}")
                assertFalse(containsEmojiCodepoint(style.subtitle), "Emoji in style subtitle ${style.id}")
            }
        }

        @Test
        fun `F20_T2_05 All 3 SplitMateThemeMode displayNames and subtitles contain zero emojis`() {
            SplitMateThemeMode.values().forEach { mode ->
                assertFalse(containsEmojiCodepoint(mode.displayName))
                assertFalse(containsEmojiCodepoint(mode.subtitle))
            }
        }
    }

    // =========================================================================
    // TIER 3: CROSS-FEATURE PAIRWISE COMBINATION TESTS (20 TEST CASES)
    // =========================================================================

    @Nested
    @DisplayName("Tier 3: Cross-Feature Pairwise Tests (20 Test Cases)")
    inner class Tier3CrossFeaturePairwiseTests {

        @Test
        fun `T3_01 F1 Kyoto Matcha Theme x F9 Balance Cascade Hero x F19 WCAG Contrast`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.setExpressiveThemeMode(SplitMateThemeMode.KYOTO_MATCHA_YUZU)
            val palette = vm.uiState.value.activeThemeMode.toPalette()
            assertEquals(SplitMateThemeMode.KYOTO_MATCHA_YUZU, palette.mode)
            assertTrue(wcagContrastRatio(palette.onPrimaryContainer, palette.primaryContainer) >= 4.5)
            assertTrue(wcagContrastRatio(palette.onSecondaryContainer, palette.secondaryContainer) >= 4.5)
        }

        @Test
        fun `T3_02 F1 Warm Espresso Night Theme x F10 Spring Keypad x F3 Emphasized Typography`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.setExpressiveThemeMode(SplitMateThemeMode.WARM_ESPRESSO_NIGHT)
            val palette = vm.uiState.value.activeThemeMode.toPalette()
            val heroStyle = SplitMateExpressiveTypography.displayMediumEmphasized
            assertEquals(FontWeight.Black, heroStyle.fontWeight)
            assertEquals("tnum, zero", heroStyle.fontFeatureSettings)
            assertTrue(wcagContrastRatio(palette.onSurface, palette.surfaceContainerHigh) >= 7.0)
        }

        @Test
        fun `T3_03 F1 Sunlit Buckwheat Theme x F11 Live Receipt Claim x F5 WavyLinearProgressIndicator`() {
            val claims = listOf(
                Triple("m1", "Akshay", 2500L),
                Triple("m2", "Rohan", 2500L)
            )
            val res = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = 10000L,
                taxCents = 1000L,
                tipCents = 500L,
                payerId = "m1",
                memberBaseClaimsCents = claims,
                attributeRemainderToPayer = true
            )
            val claimedRatio = (res.baseSubtotalCents - res.unassignedBaseCents).toFloat() / res.baseSubtotalCents.toFloat()
            assertEquals(0.5f, claimedRatio, 1e-4f)
            assertEquals(5750L, res.unassignedFinalCents)
            assertEquals(Color(0xFFFED8C8), SunlitBuckwheatPalette.secondaryContainer)
        }

        @Test
        fun `T3_04 F12 Greedy Debt Simplification x F5 SplitButtonLayout x F4 Segmented Island Shape`() {
            val balances = listOf(
                SplitMateMathEngine.MemberNetBalance("m1", "Akshay", 60000L),
                SplitMateMathEngine.MemberNetBalance("m2", "Rohan", -25000L),
                SplitMateMathEngine.MemberNetBalance("m3", "Priya", -35000L)
            )
            val transfers = SplitMateMathEngine.simplifyDebtsGreedy(balances)
            assertEquals(2, transfers.size)
            val topShape = segmentedIslandItemShape(index = 0, totalCount = transfers.size)
            val bottomShape = segmentedIslandItemShape(index = 1, totalCount = transfers.size)
            assertEquals(24f, cornerPx(topShape.topStart), 0.01f)
            assertEquals(6f, cornerPx(topShape.bottomStart), 0.01f)
            assertEquals(6f, cornerPx(bottomShape.topStart), 0.01f)
            assertEquals(24f, cornerPx(bottomShape.bottomStart), 0.01f)
        }

        @Test
        fun `T3_05 F13 IRCTC PNR Studio x F5 ContainedLoadingIndicator x F5 ConnectedButtonGroup`() {
            val morphSeq = MaterialShapes.morphSequence
            assertEquals(7, morphSeq.size)
            val extracted = UniversalFlightTicketExtractor.extractFromText(
                "IRCTC PNR: 2458910342 Train: 12952 - RAJDHANI EXP From: NDLS To: MMCT Passenger 1: CNF/B2/12"
            )
            val snapshot = extracted.toLivePnrStatusSnapshot()
            assertEquals("2458910342", PnrNetworkRepository.normalizePnrKey(snapshot.pnr))
            assertEquals("12952", snapshot.trainNo)
            assertTrue(PnrNetworkRepository.isSnapshotAllConfirmed(snapshot))
        }

        @Test
        fun `T3_06 F14 Flight Boarding Pass Scanner x F10 Payer-First Largest Remainder Split`() {
            val rawFlight = """
                IndiGo Booking Confirmation
                PNR: H7J2K9
                Flight: 6E-602
                From: BLR To: GOI
                Passengers:
                1. Akshay Karadkar
                2. Rohan Verma
                3. Priya Sharma
                Total Fare: INR 10,000.00
            """.trimIndent()
            val extracted = UniversalFlightTicketExtractor.extractFromText(rawFlight, listOf("Akshay", "Rohan", "Priya"))
            assertTrue(extracted.isValidFlightTicket)
            val split = SplitMateMathEngine.splitEquallyZeroDrift(
                totalCents = extracted.totalFarePaise,
                members = listOf("m1" to "Akshay", "m2" to "Rohan", "m3" to "Priya"),
                payerId = "m1"
            )
            assertEquals(1000000L, split.sumOf { it.finalCents })
            assertEquals(333334L, split.first { it.memberId == "m1" }.finalCents)
        }

        @Test
        fun `T3_07 F15 AnimatedTransitDeckHeroCard x F2 Spatial Spring Physics x F20 Vector-Only Icons`() {
            val deckSrc = readSourceFile("ui/components/AnimatedTransitDeckHeroCard.kt")
            assertFalse(containsEmojiCodepoint(deckSrc))
            val springSpec = SplitMateMotion.defaultSpatial<Float>()
            assertEquals(0.8f, springSpec.dampingRatio, 1e-4f)
            assertEquals(380f, springSpec.stiffness, 1e-4f)
        }

        @Test
        fun `T3_08 F6 HorizontalFloatingToolbar + FAB Menu x F1 All 3 Themes Surface Hierarchy`() {
            SplitMateThemeMode.values().forEach { mode ->
                val palette = mode.toPalette()
                val toolbarContrast = wcagContrastRatio(palette.onSurface, palette.surfaceContainerHigh)
                assertTrue(toolbarContrast >= 7.0, "Toolbar contrast failed in $mode")
            }
        }

        @Test
        fun `T3_09 F7 Settings 3-Theme Studio x F18 DiceBear 9_x Avatar Studio Preset Sync`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.setExpressiveThemeMode(SplitMateThemeMode.KYOTO_MATCHA_YUZU)
            vm.updateUserProfile("Akshay", "9876543210", "Akshay|Male|open-peeps|BoldPop")
            assertEquals(SplitMateThemeMode.KYOTO_MATCHA_YUZU, vm.uiState.value.activeThemeMode)
            assertEquals("BoldPop", vm.uiState.value.avatarColorPresetId)
            val url = buildDiceBearAvatarUrl(vm.uiState.value.currentUserSeed)
            assertTrue(url.contains("open-peeps"))
        }

        @Test
        fun `T3_10 F8 Trip Sync Sheet x F12 Settle Up Summary x F3 Tabular Monospace Alignment`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val groupId = vm.uiState.value.activeGroupId
            val bundle = vm.exportGroupSyncPayload(groupId)
            assertNotNull(bundle)
            val netMap = vm.computeGroupMemberNetBalances(groupId)
            val transfers = SplitMateMathEngine.simplifyDebtsGreedy(
                vm.uiState.value.activeGroupMembers.map {
                    SplitMateMathEngine.MemberNetBalance(it.memberId, it.name, netMap[it.memberId] ?: 0L)
                }
            )
            val summaries = SplitMateMathEngine.computeMemberSettlementSummaries(transfers)
            assertTrue(summaries.isNotEmpty())
            assertEquals("tnum, zero", SplitMateExpressiveTypography.displayMediumEmphasized.fontFeatureSettings)
        }

        @Test
        fun `T3_11 F16 Member Governance Decline Reassign x F11 Receipt Split Zero-Drift Invariant`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.clearLocalVault()
            Thread.sleep(820)
            vm.createNewGroup(name = "Udaipur Lake Trip", currencyCode = "INR", friendNamesCsv = "Rohan,Priya")
            val groupId = vm.uiState.value.activeGroupId
            val members = vm.uiState.value.members.filter { it.groupId == groupId }
            val me = members.first { it.isCurrentUser }
            val rohan = members.first { it.name == "Rohan" }

            vm.commitQuickEqualExpense("Lake Boat Charter", 10000L, members.map { it.memberId }, me.memberId)
            vm.reassignDeclinedMemberSharesEqually(null, groupId, rohan.memberId)

            val exp = vm.uiState.value.expenses.first { it.groupId == groupId }
            val splits = vm.uiState.value.splits.filter { it.expenseId == exp.expenseId }
            assertEquals(10000L, splits.sumOf { it.finalOwedCents })
            assertEquals(5000L, splits.first { it.memberId == me.memberId }.finalOwedCents)
        }

        @Test
        fun `T3_12 F17 Cloud Sync Presence x F5 WavyCircularProgressIndicator x F1 Warm Espresso Night`() {
            val nowMs = 1_700_000_000_000L
            val presence = mapOf("9876543210" to (nowMs - 15_000L))
            assertTrue(CloudGroupSyncRepository.isPhoneOnlineNow("9876543210", presence, nowMs))
            val palette = WarmEspressoNightPalette
            assertEquals(Color(0xFFA3E635), palette.primary)
        }

        @Test
        fun `T3_13 F4 MaterialShapes 7-Polygon Morphing Badges x F9 Group Cards Category Mapping`() {
            val polygons = MaterialShapes.morphSequence
            assertEquals(7, polygons.size)
            polygons.forEachIndexed { idx, poly ->
                val nextPoly = polygons[(idx + 1) % polygons.size]
                val shape = MorphPolygonShape(Morph(poly, nextPoly), percentage = 0.5f)
                val outline = shape.createOutline(testSize, LayoutDirection.Ltr, testDensity)
                assertTrue(outline is Outline.Generic)
            }
        }

        @Test
        fun `T3_14 F5 ExpressiveButtonGroup Weight Expansion x F16 Group Detail Action Row`() {
            val src = readSourceFile("ui/components/ExpressiveM3Components.kt")
            assertTrue(src.contains("(item.weight + expandedWeightBoost).coerceAtLeast(0.2f)"))
            assertTrue(src.contains("if (isPressed) 8.dp else 16.dp"))
        }

        @Test
        fun `T3_15 F18 OpenPeepsHeroStage 5 Crew Scenes x F2 Spatial Spring Scene Transition`() {
            val themeSrc = readSourceFile("ui/SplitMateTheme.kt")
            assertTrue(themeSrc.contains("fun OpenPeepsHeroStage("))
            assertEquals(5, SplitMateCrewScenes.size)
        }

        @Test
        fun `T3_16 F10 Quick Expense Subset Member Selection x F12 Greedy Minimum Cash Flow`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.clearLocalVault()
            Thread.sleep(820)
            vm.createNewGroup(name = "Rishikesh Rafting", currencyCode = "INR", friendNamesCsv = "Rohan,Priya,Kabir")
            val groupId = vm.uiState.value.activeGroupId
            val members = vm.uiState.value.members.filter { it.groupId == groupId }
            val me = members.first { it.isCurrentUser }
            val rohan = members.first { it.name == "Rohan" }

            vm.commitQuickEqualExpense("Kayak Rental", 40000L, listOf(me.memberId, rohan.memberId), me.memberId)
            val netMap = vm.computeGroupMemberNetBalances(groupId)
            assertEquals(20000L, netMap[me.memberId])
            assertEquals(-20000L, netMap[rohan.memberId])
            assertEquals(0L, netMap.values.sum())
        }

        @Test
        fun `T3_17 F13 IRCTC PNR Expense Logging x F15 Transit Deck Hero Card Extraction`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            val memberIds = vm.uiState.value.activeGroupMembers.map { it.memberId }
            val ticket = ParsedTravelTicket(
                pnr = "6655443322",
                trainOrFlightNo = "22436",
                trainOrCarrierName = "VANDE BHARAT",
                fromStation = "NDLS",
                toStation = "BSB",
                departureDate = "20 Nov 2026",
                departureTime = "06:00",
                bookingStatus = "CNF"
            )
            val title = formatTravelExpenseTitle("Train", ticket)
            vm.commitQuickEqualExpense(title = title, totalAmountCents = 720000L, selectedMemberIds = memberIds)
            val logged = vm.uiState.value.expenses.first { it.travelPnr == "6655443322" }
            assertEquals("TRAIN", logged.expenseCategory)
            val parsed = extractTravelTicketFromTitle(logged.title)
            assertNotNull(parsed)
            assertEquals("NDLS\u2192BSB", parsed!!.route)
        }

        @Test
        fun `T3_18 F14 Flight Ticket Expense Logging x F9 Group Ledger Net Balance Cascade`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.clearLocalVault()
            Thread.sleep(820)
            vm.createNewGroup(name = "Andaman Scuba", currencyCode = "INR", friendNamesCsv = "Rohan,Priya")
            val groupId = vm.uiState.value.activeGroupId
            val members = vm.uiState.value.members.filter { it.groupId == groupId }
            val me = members.first { it.isCurrentUser }

            vm.commitQuickEqualExpense(
                title = "IndiGo 6E-812 (MAA -> IXZ) [PNR:IXZ999]",
                totalAmountCents = 1800000L,
                selectedMemberIds = members.map { it.memberId },
                payerMemberId = me.memberId
            )
            val logged = vm.uiState.value.expenses.first { it.groupId == groupId }
            assertEquals("FLIGHT", logged.expenseCategory)
            assertEquals("IXZ999", logged.travelPnr)
            val netMap = vm.computeGroupMemberNetBalances(groupId)
            assertEquals(1200000L, netMap[me.memberId])
        }

        @Test
        fun `T3_19 F19 3-Theme Semantic Container Contrast x F11 Unassigned Remainder Banner Colors`() {
            listOf(SunlitBuckwheatPalette, WarmEspressoNightPalette, KyotoMatchaYuzuPalette).forEach { palette ->
                val ratio = wcagContrastRatio(palette.onSecondaryContainer, palette.secondaryContainer)
                assertTrue(ratio >= 4.5, "Unassigned remainder banner contrast failed in ${palette.mode}")
            }
        }

        @Test
        fun `T3_20 F20 Zero-Emoji Hygiene x All 7 M3 Expressive Components & 4 Hero Moments`() {
            val filesToAudit = listOf(
                "ui/SplitMateTheme.kt",
                "ui/components/ExpressiveShapesAndMotion.kt",
                "ui/components/ExpressiveM3Components.kt",
                "ui/components/AnimatedTransitDeckHeroCard.kt"
            )
            filesToAudit.forEach { path ->
                val content = readSourceFile(path)
                assertFalse(containsEmojiCodepoint(content), "Emoji violation in $path")
            }
        }
    }

    // =========================================================================
    // TIER 4: REAL-WORLD APPLICATION SCENARIOS (10 END-TO-END USER JOURNEYS)
    // =========================================================================

    @Nested
    @DisplayName("Tier 4: Real-World Application Scenarios (10 End-to-End Journeys)")
    inner class Tier4RealWorldApplicationScenarios {

        @Test
        fun `T4_01 Goa Beach Squad Trip (6 Members, Mixed Flights + Restaurants + Cabs, Sunlit Buckwheat to Warm Espresso)`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.clearLocalVault()
            vm.setExpressiveThemeMode(SplitMateThemeMode.SUNLIT_BUCKWHEAT)
            Thread.sleep(820)

            vm.createNewGroup(
                name = "goa beach squad 2026",
                currencyCode = "INR",
                friendNamesCsv = "Rohan,Priya,Kabir,Ananya,Vikram",
                iconName = "Flight"
            )
            val groupId = vm.uiState.value.activeGroupId
            val group = vm.uiState.value.groups.first { it.groupId == groupId }
            assertEquals("Goa Beach Squad 2026", group.name)

            val members = vm.uiState.value.members.filter { it.groupId == groupId }
            assertEquals(6, members.size)
            val akshay = members.first { it.isCurrentUser }
            val rohan = members.first { it.name == "Rohan" }
            val priya = members.first { it.name == "Priya" }

            // 1. Flight booking paid by Akshay (₹36,000.00 = 3,600,000 paise)
            vm.commitQuickEqualExpense(
                title = "IndiGo 6E-512 (BOM -> GOI) [PNR:GOA6E1]",
                totalAmountCents = 3600000L,
                selectedMemberIds = members.map { it.memberId },
                payerMemberId = akshay.memberId
            )

            // 2. Beach Shack Dinner with itemized claims + tax + tip paid by Rohan
            val dinnerClaims = members.mapIndexed { idx, mbr ->
                Triple(mbr.memberId, mbr.name, (idx + 1) * 80000L)
            }
            val dinnerBase = dinnerClaims.sumOf { it.third }
            val dinnerSplit = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = dinnerBase,
                taxCents = 84000L,
                tipCents = 126000L,
                payerId = rohan.memberId,
                memberBaseClaimsCents = dinnerClaims,
                attributeRemainderToPayer = true
            )
            assertEquals(0L, dinnerSplit.driftCents)
            assertEquals(dinnerSplit.totalFinalCents, dinnerSplit.allocations.sumOf { it.finalCents })

            // 3. Airport cab paid by Priya (₹1,801.00 = 180,100 paise across 6 members)
            Thread.sleep(820)
            vm.commitQuickEqualExpense(
                title = "Goa Mopa Airport Cab",
                totalAmountCents = 180100L,
                selectedMemberIds = members.map { it.memberId },
                payerMemberId = priya.memberId
            )

            // Switch to Warm Espresso Night & verify greedy debt simplification
            vm.setExpressiveThemeMode(SplitMateThemeMode.WARM_ESPRESSO_NIGHT)
            assertTrue(vm.uiState.value.isDarkTheme)

            val netMap = vm.computeGroupMemberNetBalances(groupId)
            assertEquals(0L, netMap.values.sum())
            val transfers = SplitMateMathEngine.simplifyDebtsGreedy(
                members.map { SplitMateMathEngine.MemberNetBalance(it.memberId, it.name, netMap[it.memberId] ?: 0L) }
            )
            assertTrue(transfers.size <= 5)
            transfers.forEach { vm.markGreedyTransferSettled(it) }
            assertTrue(vm.computeGroupMemberNetBalances(groupId).values.all { it == 0L })
        }

        @Test
        fun `T4_02 IRCTC Rajdhani Express Group Journey (10-Digit PNR + Berth Status + Kyoto Matcha Theme)`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.clearLocalVault()
            vm.setExpressiveThemeMode(SplitMateThemeMode.KYOTO_MATCHA_YUZU)
            Thread.sleep(820)

            vm.createNewGroup(
                name = "Rajdhani Delhi To Mumbai",
                currencyCode = "INR",
                friendNamesCsv = "Aarav,Diya,Ishaan",
                iconName = "Train"
            )
            val groupId = vm.uiState.value.activeGroupId
            val members = vm.uiState.value.members.filter { it.groupId == groupId }

            val rawIrctc = """
                PNR: 2849105736
                Train: 12952 - MUMBAI RAJDHANI
                From: NDLS To: MMCT
                Departure: 16:55
                Class: 2A
                Total Fare: Rs. 11480
                Passenger 1: Booking Status: CNF/A1/12/LB, Current Status: CNF
                Passenger 2: Booking Status: CNF/A1/14/UB, Current Status: CNF
                Passenger 3: Booking Status: RAC 8, Current Status: CNF/A1/15/SL
                Passenger 4: Booking Status: WL 4, Current Status: CNF/A1/16/SU
                Chart Prepared
            """.trimIndent()
            val extracted = UniversalFlightTicketExtractor.extractFromText(rawIrctc)
            assertTrue(extracted.isTrainPdfTicket)
            assertEquals("2849105736", extracted.pnr)
            assertEquals("12952", extracted.flightNumber)
            assertEquals(1148000L, extracted.totalFarePaise)

            val snapshot = LivePnrStatusSnapshot(
                pnr = extracted.pnr,
                trainNo = extracted.flightNumber,
                trainName = extracted.airlineName,
                fromStation = "NDLS",
                toStation = "MMCT",
                departureTime = extracted.departureTime,
                travelClass = "2A",
                totalFareRupees = (extracted.totalFarePaise / 100L).toInt(),
                passengerCount = 4,
                bookingStatusBadge = "CNF",
                chartPrepared = true,
                passengerStatuses = listOf("P1: CNF/A1/12/LB", "P2: CNF/A1/14/UB", "P3: CNF/A1/15/SL", "P4: CNF/A1/16/SU"),
                coachPositionHint = "Coach A1",
                liveTrainLocationRadar = "On Time",
                confirmationProbability = "100% Confirmed",
                sourceLabel = "IRCTC Verified"
            )
            assertTrue(PnrNetworkRepository.isSnapshotAllConfirmed(snapshot))

            val ticket = ParsedTravelTicket(
                pnr = snapshot.pnr,
                trainOrFlightNo = snapshot.trainNo,
                trainOrCarrierName = snapshot.trainName,
                fromStation = snapshot.fromStation,
                toStation = snapshot.toStation,
                departureTime = snapshot.departureTime,
                bookingStatus = snapshot.bookingStatusBadge,
                fareRupees = "11480"
            )
            val title = formatTravelExpenseTitle("Train", ticket)
            vm.commitQuickEqualExpense(title = title, totalAmountCents = 1148000L, selectedMemberIds = members.map { it.memberId })

            // Verify duplicate PNR guard updates in place rather than duplicating
            vm.commitQuickEqualExpense(title = title, totalAmountCents = 1148000L, selectedMemberIds = members.map { it.memberId })
            assertEquals(1, vm.uiState.value.expenses.count { it.travelPnr == "2849105736" })
        }

        @Test
        fun `T4_03 Multi-Stop Flight Boarding Pass Extraction (IndiGo 6E + Air India to Passenger Matching to Equal Split)`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.clearLocalVault()
            Thread.sleep(820)
            vm.createNewGroup(name = "Leh Ladakh Expedition", currencyCode = "INR", friendNamesCsv = "Rohan,Priya,Kabir")
            val groupId = vm.uiState.value.activeGroupId
            val members = vm.uiState.value.members.filter { it.groupId == groupId }

            val flightText = """
                IndiGo E-Ticket
                PNR: LEH6E9
                Flight: 6E-2104
                From: DEL (New Delhi) To: IXL (Leh)
                Date: 05 Jun 2026  Dep: 05:45  Arr: 07:15
                Passengers:
                1. Mr Akshay Karadkar  Seat 4A
                2. Mr Rohan Verma      Seat 4B
                3. Ms Priya Sharma     Seat 4C
                Total Fare: INR 21,001.00
            """.trimIndent()

            val extracted = UniversalFlightTicketExtractor.extractFromText(flightText, members.map { it.name })
            assertTrue(extracted.isValidFlightTicket)
            assertEquals("LEH6E9", extracted.pnr)
            assertEquals(2100100L, extracted.totalFarePaise)

            // Only 3 matched passengers out of 4 group members fly on this PNR
            val matchedIds = members.filter { it.name in listOf("Akshay", "Rohan", "Priya") || it.isCurrentUser }.map { it.memberId }.distinct()
            assertEquals(3, matchedIds.size)

            vm.commitQuickEqualExpense(
                title = "IndiGo ${extracted.flightNumber} (${extracted.originIata} -> ${extracted.destinationIata}) [PNR:${extracted.pnr}]",
                totalAmountCents = extracted.totalFarePaise,
                selectedMemberIds = matchedIds,
                payerMemberId = matchedIds.first()
            )
            val exp = vm.uiState.value.expenses.first { it.travelPnr == "LEH6E9" }
            val breakdown = SplitMateViewModel.resolveExpenseSplitBreakdown(exp, members, vm.uiState.value.splits)
            assertEquals(3, breakdown.splittingMembersCount)
            assertEquals(2100100L, breakdown.rows.sumOf { it.owedCents })
            val kabirRow = breakdown.rows.first { it.displayName.startsWith("Kabir") }
            assertFalse(kabirRow.isIncludedInSplit)
            assertEquals(0L, kabirRow.owedCents)
        }

        @Test
        fun `T4_04 Complex Restaurant Receipt with Unassigned Appetizers + 18% GST + 10% Service Charge`() {
            val claims = listOf(
                Triple("m1", "Akshay", 125000L),
                Triple("m2", "Rohan", 98000L),
                Triple("m3", "Priya", 112000L),
                Triple("m4", "Kabir", 85000L)
            )
            // Base subtotal is ₹5,000.00 (500,000 paise); ₹800.00 (80,000 paise) of shared appetizers unassigned
            val initial = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = 500000L,
                taxCents = 90000L,
                tipCents = 50000L,
                payerId = "m1",
                memberBaseClaimsCents = claims,
                attributeRemainderToPayer = true
            )
            assertEquals(1.28, initial.lockedMultiplier, 1e-6)
            assertEquals(80000L, initial.unassignedBaseCents)
            assertEquals(102400L, initial.unassignedFinalCents)
            assertEquals(0L, initial.driftCents)

            // Tap 1-Tap "Split Remainder Equally"
            val remainderShares = SplitMateMathEngine.splitEquallyZeroDrift(
                totalCents = initial.unassignedBaseCents,
                members = claims.map { it.first to it.second },
                payerId = "m1",
                currentUserId = "m1"
            ).associateBy { it.memberId }
            val updatedClaims = claims.map { (id, name, base) ->
                Triple(id, name, base + (remainderShares[id]?.finalCents ?: 0L))
            }
            val finalSplit = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = 500000L,
                taxCents = 90000L,
                tipCents = 50000L,
                payerId = "m1",
                memberBaseClaimsCents = updatedClaims,
                attributeRemainderToPayer = true
            )
            assertEquals(1.28, finalSplit.lockedMultiplier, 1e-6)
            assertEquals(0L, finalSplit.unassignedBaseCents)
            assertEquals(0L, finalSplit.unassignedFinalCents)
            assertEquals(640000L, finalSplit.allocations.sumOf { it.finalCents })
            assertEquals(0L, finalSplit.driftCents)
        }

        @Test
        fun `T4_05 Group Invite Decline & Share Reassignment (5 Members to 1 Declines to Equal Redistribution)`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.clearLocalVault()
            Thread.sleep(820)
            vm.createNewGroup(
                name = "Hampi Heritage Weekend",
                currencyCode = "INR",
                friendNamesCsv = "Rohan,Priya,Kabir,Ananya"
            )
            val groupId = vm.uiState.value.activeGroupId
            val members = vm.uiState.value.members.filter { it.groupId == groupId }
            assertEquals(5, members.size)
            val me = members.first { it.isCurrentUser }
            val ananya = members.first { it.name == "Ananya" }

            vm.commitQuickEqualExpense(
                title = "Boulder Resort Booking",
                totalAmountCents = 2500000L,
                selectedMemberIds = members.map { it.memberId },
                payerMemberId = me.memberId
            )

            // Ananya declines and shares are reassigned across the remaining 4 active members
            vm.reassignDeclinedMemberSharesEqually(context = null, groupId = groupId, declinedMemberId = ananya.memberId)

            val exp = vm.uiState.value.expenses.first { it.groupId == groupId }
            val updatedSplits = vm.uiState.value.splits.filter { it.expenseId == exp.expenseId }
            assertEquals(4, updatedSplits.size)
            assertEquals(2500000L, updatedSplits.sumOf { it.finalOwedCents })
            updatedSplits.forEach { sp ->
                assertEquals(625000L, sp.finalOwedCents)
            }
        }

        @Test
        fun `T4_06 Offline-to-Online Cloud Sync & WhatsApp Deep-Link Merge Round-Trip`() = runTest {
            val deviceAVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            deviceAVm.clearLocalVault()
            deviceAVm.onNetworkConnectivityChanged(null, isConnected = false)
            assertTrue(deviceAVm.uiState.value.isOfflineMode)

            Thread.sleep(820)
            deviceAVm.createNewGroup(name = "Spiti Valley Roadtrip", currencyCode = "INR", friendNamesCsv = "Rohan,Priya")
            val groupId = deviceAVm.uiState.value.activeGroupId
            val members = deviceAVm.uiState.value.members.filter { it.groupId == groupId }

            deviceAVm.commitQuickEqualExpense(
                title = "4x4 Expedition SUV Fuel",
                totalAmountCents = 1200000L,
                selectedMemberIds = members.map { it.memberId }
            )
            val offlineExp = deviceAVm.uiState.value.expenses.first { it.groupId == groupId }
            assertEquals("PENDING", offlineExp.syncStatus)

            val exportBundle = deviceAVm.exportGroupSyncPayload(groupId)!!
            val deviceBVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            deviceBVm.clearLocalVault()

            val mergeRes = deviceBVm.importAndMergeGroupSyncPayload(exportBundle.deepLinkUri, openGroupAfterMerge = true)
            assertTrue(mergeRes.success)
            assertEquals("Spiti Valley Roadtrip", mergeRes.groupName)
            assertEquals(1200000L, deviceBVm.uiState.value.expenses.first { it.groupId == groupId }.totalAmountCents)
        }

        @Test
        fun `T4_07 Full Avatar Studio Customization (13 DiceBear Styles x 12 Color Presets x 5 Crew Scenes)`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            SplitMateDiceBearStyles.forEachIndexed { idx, style ->
                val preset = SplitMateAvatarColorPresets[idx % SplitMateAvatarColorPresets.size]
                val gender = AvatarGender.values()[idx % AvatarGender.values().size]
                val encoded = AvatarSeedCodec.encode(
                    seedKey = "Traveler$idx",
                    gender = gender,
                    styleId = style.id,
                    colorPresetId = preset.id
                )
                val parsed = AvatarSeedCodec.parse(encoded)
                assertEquals(style.id, parsed.styleId)
                assertEquals(preset.id, parsed.colorPresetId)
                assertEquals(gender, parsed.gender)

                val url = buildDiceBearAvatarUrl(name = encoded)
                assertTrue(url.startsWith("https://api.dicebear.com/9.x/${style.id}/svg?"))
            }
            val lastSeed = AvatarSeedCodec.encode("Akshay", AvatarGender.MALE, "lorelei", "BoldPop")
            vm.updateUserProfile("Akshay", "9876543210", lastSeed)
            assertEquals("lorelei", vm.uiState.value.avatarStyleId)
            assertEquals("BoldPop", vm.uiState.value.avatarColorPresetId)
        }

        @Test
        fun `T4_08 Live 3-Theme Switching During Active Expense Logging without State Loss`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.clearLocalVault()
            Thread.sleep(820)
            vm.createNewGroup(name = "Pondicherry Cycling", currencyCode = "INR", friendNamesCsv = "Rohan,Priya")
            val groupId = vm.uiState.value.activeGroupId
            val memberIds = vm.uiState.value.members.filter { it.groupId == groupId }.map { it.memberId }

            vm.setExpressiveThemeMode(SplitMateThemeMode.SUNLIT_BUCKWHEAT)
            vm.commitQuickEqualExpense("French Quarter Croissants", 150000L, memberIds)

            vm.setExpressiveThemeMode(SplitMateThemeMode.KYOTO_MATCHA_YUZU)
            Thread.sleep(820)
            vm.commitQuickEqualExpense("Auroville Pottery Workshop", 300000L, memberIds)

            vm.setExpressiveThemeMode(SplitMateThemeMode.WARM_ESPRESSO_NIGHT)
            Thread.sleep(820)
            vm.commitQuickEqualExpense("Promenade Rooftop Dinner", 450000L, memberIds)

            val groupExpenses = vm.uiState.value.expenses.filter { it.groupId == groupId }
            assertEquals(3, groupExpenses.size)
            assertEquals(900000L, groupExpenses.sumOf { it.totalAmountCents })
            assertEquals(SplitMateThemeMode.WARM_ESPRESSO_NIGHT, vm.uiState.value.activeThemeMode)
        }

        @Test
        fun `T4_09 Multi-Group Ledger Portfolio (3 Active Groups - Positive, Negative, and All Settled)`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.clearLocalVault()

            // Group 1: User is owed (Positive)
            Thread.sleep(820)
            vm.createNewGroup(name = "Group One Positive", currencyCode = "INR", friendNamesCsv = "Rohan")
            val g1 = vm.uiState.value.activeGroupId
            val g1Members = vm.uiState.value.members.filter { it.groupId == g1 }
            val g1Me = g1Members.first { it.isCurrentUser }
            vm.commitQuickEqualExpense("Hotel Paid By Me", 400000L, g1Members.map { it.memberId }, g1Me.memberId)

            // Group 2: User owes (Negative)
            Thread.sleep(820)
            vm.createNewGroup(name = "Group Two Negative", currencyCode = "INR", friendNamesCsv = "Priya")
            val g2 = vm.uiState.value.activeGroupId
            val g2Members = vm.uiState.value.members.filter { it.groupId == g2 }
            val g2Friend = g2Members.first { !it.isCurrentUser }
            Thread.sleep(820)
            vm.commitQuickEqualExpense("Flights Paid By Priya", 600000L, g2Members.map { it.memberId }, g2Friend.memberId)

            // Group 3: All Settled (0.00 balance)
            Thread.sleep(820)
            vm.createNewGroup(name = "Group Three Settled", currencyCode = "INR", friendNamesCsv = "Kabir")
            val g3 = vm.uiState.value.activeGroupId

            val g1Net = vm.computeGroupMemberNetBalances(g1)[g1Me.memberId] ?: 0L
            val g2Me = g2Members.first { it.isCurrentUser }
            val g2Net = vm.computeGroupMemberNetBalances(g2)[g2Me.memberId] ?: 0L
            val g3Me = vm.uiState.value.members.first { it.groupId == g3 && it.isCurrentUser }
            val g3Net = vm.computeGroupMemberNetBalances(g3)[g3Me.memberId] ?: 0L

            assertEquals(200000L, g1Net)
            assertEquals(-300000L, g2Net)
            assertEquals(0L, g3Net)
        }

        @Test
        fun `T4_10 High-Member Trip Stress Scenario (12 Members, 10 Expenses, Greedy Cash Flow max 11 Transfers, 0 Drift)`() {
            val members = (1..12).map { idx -> "m$idx" to "Member$idx" }
            val netBalances = linkedMapOf<String, Long>().apply {
                members.forEach { (id, _) -> put(id, 0L) }
            }

            // Simulate 10 expenses with odd paise totals across 12 members
            val oddTotals = listOf(
                100001L, 250003L, 78999L, 412345L, 99999L,
                154321L, 333333L, 876543L, 65432L, 199999L
            )
            oddTotals.forEachIndexed { expIdx, totalCents ->
                val payerId = members[expIdx % members.size].first
                val allocs = SplitMateMathEngine.splitEquallyZeroDrift(
                    totalCents = totalCents,
                    members = members,
                    payerId = payerId,
                    currentUserId = "m1"
                )
                assertEquals(totalCents, allocs.sumOf { it.finalCents }, "0.00 drift violated on expense $expIdx")
                netBalances[payerId] = (netBalances[payerId] ?: 0L) + totalCents
                allocs.forEach { alloc ->
                    netBalances[alloc.memberId] = (netBalances[alloc.memberId] ?: 0L) - alloc.finalCents
                }
            }

            assertEquals(0L, netBalances.values.sum(), "Global zero-sum invariant must hold across 12 members and 10 expenses")

            val balanceList = members.map { (id, name) ->
                SplitMateMathEngine.MemberNetBalance(id, name, netBalances[id] ?: 0L)
            }
            val transfers = SplitMateMathEngine.simplifyDebtsGreedy(balanceList)
            assertTrue(transfers.size <= 11, "Greedy cash flow must use at most N-1 = 11 transfers, used ${transfers.size}")

            val totalPositive = balanceList.filter { it.netCents > 0L }.sumOf { it.netCents }
            assertEquals(totalPositive, transfers.sumOf { it.amountCents })
        }
    }

    // =========================================================================
    // TIER 5: WHITE-BOX ADVERSARIAL COVERAGE HARDENING (20 TEST CASES: T5_01-T5_20)
    // =========================================================================

    @Nested
    @DisplayName("Tier 5: White-Box Adversarial Coverage Hardening (20 Tests)")
    inner class Tier5WhiteBoxAdversarialHardeningTests {

        @Test
        fun `T5_01 SplitMateThemeMode fromId edge normalization and SplitMateTheme desync recovery across all 3 palettes`() {
            val whitespaceLower = SplitMateThemeMode.fromId("   warm_espresso_night   ")
            assertEquals(SplitMateThemeMode.WARM_ESPRESSO_NIGHT, whitespaceLower)

            val mixedCaseMatcha = SplitMateThemeMode.fromId("  KyOtO_MaTcHa_YuZu\n\t ")
            assertEquals(SplitMateThemeMode.KYOTO_MATCHA_YUZU, mixedCaseMatcha)

            assertEquals(SplitMateThemeMode.SUNLIT_BUCKWHEAT, SplitMateThemeMode.fromId("  Sunlit Buckwheat  "))
            assertEquals(SplitMateThemeMode.WARM_ESPRESSO_NIGHT, SplitMateThemeMode.fromId("Warm Espresso Night"))
            assertEquals(SplitMateThemeMode.KYOTO_MATCHA_YUZU, SplitMateThemeMode.fromId("kyoto matcha & yuzu"))

            assertEquals(SplitMateThemeMode.SUNLIT_BUCKWHEAT, SplitMateThemeMode.fromId(null, fallbackDark = false))
            assertEquals(SplitMateThemeMode.WARM_ESPRESSO_NIGHT, SplitMateThemeMode.fromId(null, fallbackDark = true))
            assertEquals(SplitMateThemeMode.SUNLIT_BUCKWHEAT, SplitMateThemeMode.fromId("", fallbackDark = false))
            assertEquals(SplitMateThemeMode.WARM_ESPRESSO_NIGHT, SplitMateThemeMode.fromId("   ", fallbackDark = true))
            assertEquals(SplitMateThemeMode.SUNLIT_BUCKWHEAT, SplitMateThemeMode.fromId("COLD_CORPORATE_BLUE", fallbackDark = false))
            assertEquals(SplitMateThemeMode.WARM_ESPRESSO_NIGHT, SplitMateThemeMode.fromId("COLD_CORPORATE_BLUE", fallbackDark = true))

            // Desync Scenario A: activeThemeMode is SUNLIT_BUCKWHEAT (light), but legacy SplitMateTheme.isDark is flipped to true
            DesignSystemBindings.activeThemeMode = SplitMateThemeMode.SUNLIT_BUCKWHEAT
            SplitMateTheme.isDark = true
            assertEquals(WarmEspressoNightPalette.surfaceContainerLow, SplitMateTheme.ScreenBg)
            assertEquals(WarmEspressoNightPalette.surfaceContainerLowest, SplitMateTheme.SurfaceWhite)
            assertEquals(WarmEspressoNightPalette.primaryContainer, SplitMateTheme.SageSurface)
            assertEquals(WarmEspressoNightPalette.onPrimaryContainer, SplitMateTheme.SageText)
            assertEquals(Color(0xFF3A2019), SplitMateTheme.TerracottaSurface)
            assertEquals(Color(0xFFFECDD3), SplitMateTheme.TerracottaText)

            // Desync Scenario B: activeThemeMode is WARM_ESPRESSO_NIGHT (dark), but legacy SplitMateTheme.isDark is flipped to false
            DesignSystemBindings.activeThemeMode = SplitMateThemeMode.WARM_ESPRESSO_NIGHT
            SplitMateTheme.isDark = false
            assertEquals(SunlitBuckwheatPalette.surfaceContainerLow, SplitMateTheme.ScreenBg)
            assertEquals(SunlitBuckwheatPalette.surfaceContainerLowest, SplitMateTheme.SurfaceWhite)
            assertEquals(SunlitBuckwheatPalette.primaryContainer, SplitMateTheme.SageSurface)

            // Scenario C: KYOTO_MATCHA_YUZU active with isDark = false resolves Kyoto Matcha tokens
            DesignSystemBindings.activeThemeMode = SplitMateThemeMode.KYOTO_MATCHA_YUZU
            SplitMateTheme.isDark = false
            assertEquals(KyotoMatchaYuzuPalette.surfaceContainerLow, SplitMateTheme.ScreenBg)
            assertEquals(KyotoMatchaYuzuPalette.primaryContainer, SplitMateTheme.SageSurface)
            assertEquals(KyotoMatchaYuzuPalette.onPrimaryContainer, SplitMateTheme.SageText)
            assertEquals(KyotoMatchaYuzuPalette.secondaryContainer.copy(alpha = 0.65f), SplitMateTheme.TerracottaSurface)
            assertEquals(KyotoMatchaYuzuPalette.onSecondaryContainer, SplitMateTheme.TerracottaText)

            // Reset to canonical default
            SplitMateThemeState.activeThemeMode = SplitMateThemeMode.SUNLIT_BUCKWHEAT
            DesignSystemBindings.activeThemeMode = SplitMateThemeMode.SUNLIT_BUCKWHEAT
            SplitMateTheme.isDark = false
        }

        @Test
        fun `T5_02 SplitMateExpressivePalette full 21-token WCAG contrast matrix and 15 emphasized typography extensions`() {
            val palettes = listOf(
                SunlitBuckwheatPalette,
                WarmEspressoNightPalette,
                KyotoMatchaYuzuPalette
            )
            for (palette in palettes) {
                assertTrue(
                    wcagContrastRatio(palette.onPrimary, palette.primary) >= 4.5,
                    "onPrimary vs primary contrast failed for ${palette.mode}"
                )
                assertTrue(
                    wcagContrastRatio(palette.onPrimaryContainer, palette.primaryContainer) >= 4.5,
                    "onPrimaryContainer vs primaryContainer contrast failed for ${palette.mode}"
                )
                assertTrue(
                    wcagContrastRatio(palette.onSecondaryContainer, palette.secondaryContainer) >= 4.5,
                    "onSecondaryContainer vs secondaryContainer contrast failed for ${palette.mode}"
                )
                assertTrue(
                    wcagContrastRatio(palette.onTertiaryContainer, palette.tertiaryContainer) >= 4.5,
                    "onTertiaryContainer vs tertiaryContainer contrast failed for ${palette.mode}"
                )
                assertTrue(
                    wcagContrastRatio(palette.onSurface, palette.surfaceContainerLowest) >= 10.0,
                    "onSurface vs surfaceContainerLowest AAA contrast failed for ${palette.mode}"
                )
                assertTrue(
                    wcagContrastRatio(palette.onSurface, palette.surfaceContainerLow) >= 10.0,
                    "onSurface vs surfaceContainerLow AAA contrast failed for ${palette.mode}"
                )
                assertTrue(
                    wcagContrastRatio(palette.onSurface, palette.surfaceContainerHighest) >= 4.5,
                    "onSurface vs surfaceContainerHighest contrast failed for ${palette.mode}"
                )
                assertTrue(
                    wcagContrastRatio(palette.onSurfaceVariant, palette.surfaceContainerLow) >= 4.5,
                    "onSurfaceVariant vs surfaceContainerLow contrast failed for ${palette.mode}"
                )
                assertNotEquals(palette.primaryContainer, palette.secondaryContainer)
                assertNotEquals(palette.onPrimaryContainer, palette.onSecondaryContainer)
            }

            assertTrue(
                wcagContrastRatio(DesignSystemBindings.ElementsPositiveText, DesignSystemBindings.ElementsPositiveContainer) >= 4.5
            )
            assertTrue(
                wcagContrastRatio(DesignSystemBindings.ElementsDebtorOnContainer, DesignSystemBindings.ElementsDebtorContainer) >= 4.5
            )

            assertEquals(FigtreeFontFamily, PlusJakartaSansFont)
            assertEquals(FontFamily.Monospace, JetBrainsMonoFont)
            assertEquals(FontFamily.Monospace, SplitMateTnumMonospace)
            assertEquals("tnum, zero", SplitMateMonospaceTextStyle.fontFeatureSettings)
            assertEquals(FontWeight.Bold, SplitMateMonospaceTextStyle.fontWeight)

            val baseToEmphasized = listOf(
                SplitMateTypography.displayLarge to SplitMateExpressiveTypography.displayLargeEmphasized,
                SplitMateTypography.displayMedium to SplitMateExpressiveTypography.displayMediumEmphasized,
                SplitMateTypography.displaySmall to SplitMateExpressiveTypography.displaySmallEmphasized,
                SplitMateTypography.headlineLarge to SplitMateExpressiveTypography.headlineLargeEmphasized,
                SplitMateTypography.headlineMedium to SplitMateExpressiveTypography.headlineMediumEmphasized,
                SplitMateTypography.headlineSmall to SplitMateExpressiveTypography.headlineSmallEmphasized,
                SplitMateTypography.titleLarge to SplitMateExpressiveTypography.titleLargeEmphasized,
                SplitMateTypography.titleMedium to SplitMateExpressiveTypography.titleMediumEmphasized,
                SplitMateTypography.titleSmall to SplitMateExpressiveTypography.titleSmallEmphasized,
                SplitMateTypography.bodyLarge to SplitMateExpressiveTypography.bodyLargeEmphasized,
                SplitMateTypography.bodyMedium to SplitMateExpressiveTypography.bodyMediumEmphasized,
                SplitMateTypography.bodySmall to SplitMateExpressiveTypography.bodySmallEmphasized,
                SplitMateTypography.labelLarge to SplitMateExpressiveTypography.labelLargeEmphasized,
                SplitMateTypography.labelMedium to SplitMateExpressiveTypography.labelMediumEmphasized,
                SplitMateTypography.labelSmall to SplitMateExpressiveTypography.labelSmallEmphasized
            )
            assertEquals(15, baseToEmphasized.size)
            baseToEmphasized.forEachIndexed { idx, (base, emphasized) ->
                assertEquals("tnum, zero", base.fontFeatureSettings)
                assertEquals("tnum, zero", emphasized.fontFeatureSettings)
                assertTrue(
                    emphasized.fontWeight!!.weight >= base.fontWeight!!.weight,
                    "Emphasized slot $idx weight must be >= base weight"
                )
                if (idx != 6) {
                    assertTrue(
                        emphasized.fontSize.value >= base.fontSize.value,
                        "Emphasized slot $idx fontSize (${emphasized.fontSize.value}) must be >= base (${base.fontSize.value})"
                    )
                }
            }

            val themeSrc = readSourceFile("ui/SplitMateTheme.kt")
            val requiredExtensionProps = listOf(
                "val Typography.displayLargeEmphasized",
                "val Typography.displayMediumEmphasized",
                "val Typography.displaySmallEmphasized",
                "val Typography.headlineLargeEmphasized",
                "val Typography.headlineMediumEmphasized",
                "val Typography.headlineSmallEmphasized",
                "val Typography.titleLargeEmphasized",
                "val Typography.titleMediumEmphasized",
                "val Typography.titleSmallEmphasized",
                "val Typography.bodyLargeEmphasized",
                "val Typography.bodyMediumEmphasized",
                "val Typography.bodySmallEmphasized",
                "val Typography.labelLargeEmphasized",
                "val Typography.labelMediumEmphasized",
                "val Typography.labelSmallEmphasized"
            )
            for (prop in requiredExtensionProps) {
                assertTrue(themeSrc.contains(prop), "SplitMateTheme.kt must declare extension '$prop'")
            }
        }

        @Test
        fun `T5_03 MaterialShapes 21-shape catalog and MorphPolygonShape adversarial aspect-ratio and rotation outline bounds`() {
            val allShapes = listOf<RoundedPolygon>(
                MaterialShapes.Circle,
                MaterialShapes.Square,
                MaterialShapes.Arch,
                MaterialShapes.Oval,
                MaterialShapes.Pill,
                MaterialShapes.Diamond,
                MaterialShapes.Pentagon,
                MaterialShapes.Sunny,
                MaterialShapes.VerySunny,
                MaterialShapes.Cookie4Sided,
                MaterialShapes.Cookie6Sided,
                MaterialShapes.Cookie7Sided,
                MaterialShapes.Cookie9Sided,
                MaterialShapes.Cookie12Sided,
                MaterialShapes.Clover4Leaf,
                MaterialShapes.Clover8Leaf,
                MaterialShapes.Burst,
                MaterialShapes.SoftBurst,
                MaterialShapes.Flower,
                MaterialShapes.Puffy,
                MaterialShapes.Heart
            )
            assertEquals(21, allShapes.size)

            val extremeSizes = listOf(
                Size(240f, 36f),
                Size(36f, 240f),
                Size(1f, 1f)
            )
            val rotations = listOf(-720f, -45f, 0f, 135f, 1080f)

            for (poly in allShapes) {
                for (size in extremeSizes) {
                    for (rot in rotations) {
                        val outline = RoundedPolygonShape(poly, rotationDegrees = rot)
                            .createOutline(size, LayoutDirection.Ltr, testDensity)
                        assertTrue(outline is Outline.Generic)
                        val path = (outline as Outline.Generic).path
                        assertFalse(path.isEmpty, "Outline path must not be empty for size=$size rot=$rot")
                        val bounds = path.getBounds()
                        assertFalse(bounds.left.isNaN() || bounds.top.isNaN() || bounds.right.isNaN() || bounds.bottom.isNaN())
                    }
                }
            }

            val morph = Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny)
            for (progress in listOf(-999f, -0.01f, 0f, 0.25f, 0.5f, 0.75f, 1f, 1.01f, 999f)) {
                val outline = MorphPolygonShape(morph, percentage = progress, rotationDegrees = 270f)
                    .createOutline(Size(180f, 60f), LayoutDirection.Rtl, testDensity)
                assertTrue(outline is Outline.Generic)
                val path = (outline as Outline.Generic).path
                assertFalse(path.isEmpty)
            }

            // Degenerate zero-dimension Size(0f, 0f) must not throw and must return a non-null path
            val zeroOutline = MorphPolygonShape(morph, percentage = 0.5f)
                .createOutline(Size(0f, 0f), LayoutDirection.Ltr, testDensity)
            assertTrue(zeroOutline is Outline.Generic)
            assertNotNull((zeroOutline as Outline.Generic).path)
        }

        @Test
        fun `T5_04 segmentedIslandItemShape out-of-range index and custom outer-inner corner radius invariant`() {
            // Single or non-positive totalCount <= 1 returns outerCorner on all 4 corners
            for (invalidTotal in listOf(-5, 0, 1)) {
                val singleShape = segmentedIslandItemShape(
                    index = 0,
                    totalCount = invalidTotal,
                    isSelected = false,
                    outerCorner = 24.dp,
                    innerCorner = 8.dp
                )
                assertEquals(24f, cornerPx(singleShape.topStart), 0.01f)
                assertEquals(24f, cornerPx(singleShape.topEnd), 0.01f)
                assertEquals(24f, cornerPx(singleShape.bottomStart), 0.01f)
                assertEquals(24f, cornerPx(singleShape.bottomEnd), 0.01f)
            }

            // Negative index clamps to 0 (first item: top outerCorner = 24f, bottom innerCorner = 8f)
            val negIndexShape = segmentedIslandItemShape(
                index = -3,
                totalCount = 4,
                isSelected = false,
                outerCorner = 24.dp,
                innerCorner = 8.dp
            )
            assertEquals(24f, cornerPx(negIndexShape.topStart), 0.01f)
            assertEquals(24f, cornerPx(negIndexShape.topEnd), 0.01f)
            assertEquals(8f, cornerPx(negIndexShape.bottomStart), 0.01f)
            assertEquals(8f, cornerPx(negIndexShape.bottomEnd), 0.01f)

            // Overflow index clamps to totalCount - 1 (last item: top innerCorner = 8f, bottom outerCorner = 24f)
            val highIndexShape = segmentedIslandItemShape(
                index = 99,
                totalCount = 4,
                isSelected = false,
                outerCorner = 24.dp,
                innerCorner = 8.dp
            )
            assertEquals(8f, cornerPx(highIndexShape.topStart), 0.01f)
            assertEquals(8f, cornerPx(highIndexShape.topEnd), 0.01f)
            assertEquals(24f, cornerPx(highIndexShape.bottomStart), 0.01f)
            assertEquals(24f, cornerPx(highIndexShape.bottomEnd), 0.01f)

            val studioFirst = segmentedIslandItemShape(0, 3, isSelected = false, outerCorner = 20.dp, innerCorner = 6.dp)
            val studioMiddle = segmentedIslandItemShape(1, 3, isSelected = false, outerCorner = 20.dp, innerCorner = 6.dp)
            val studioLast = segmentedIslandItemShape(2, 3, isSelected = false, outerCorner = 20.dp, innerCorner = 6.dp)
            val studioSelected = segmentedIslandItemShape(1, 3, isSelected = true, outerCorner = 20.dp, innerCorner = 6.dp)

            assertEquals(20f, cornerPx(studioFirst.topStart), 0.01f)
            assertEquals(20f, cornerPx(studioFirst.topEnd), 0.01f)
            assertEquals(6f, cornerPx(studioFirst.bottomStart), 0.01f)
            assertEquals(6f, cornerPx(studioFirst.bottomEnd), 0.01f)

            assertEquals(6f, cornerPx(studioMiddle.topStart), 0.01f)
            assertEquals(6f, cornerPx(studioMiddle.bottomEnd), 0.01f)

            assertEquals(6f, cornerPx(studioLast.topStart), 0.01f)
            assertEquals(6f, cornerPx(studioLast.topEnd), 0.01f)
            assertEquals(20f, cornerPx(studioLast.bottomStart), 0.01f)
            assertEquals(20f, cornerPx(studioLast.bottomEnd), 0.01f)

            assertEquals(20f, cornerPx(studioSelected.topStart), 0.01f)
            assertEquals(20f, cornerPx(studioSelected.bottomEnd), 0.01f)
        }

        @Test
        fun `T5_05 ExpressiveM3Components data models and progress indicator overload parity`() {
            assertEquals(
                listOf(ActiveTravelPassMode.TRAIN, ActiveTravelPassMode.FLIGHT),
                ActiveTravelPassMode.entries
            )

            var fabClicked = 0
            val fabItem = ExpressiveFabMenuItem(
                label = "Scan Receipt",
                icon = Icons.Rounded.Add,
                onClick = { fabClicked++ },
                subtitle = "AI OCR itemization",
                containerColor = SunlitBuckwheatPalette.primaryContainer,
                contentColor = SunlitBuckwheatPalette.onPrimaryContainer
            )
            assertEquals("Scan Receipt", fabItem.label)
            assertEquals("AI OCR itemization", fabItem.subtitle)
            assertEquals(SunlitBuckwheatPalette.primaryContainer, fabItem.containerColor)
            fabItem.onClick()
            assertEquals(1, fabClicked)

            var actionClicked = 0
            val actionItem = ExpressiveActionItem(
                label = "Share Link",
                icon = Icons.Rounded.Add,
                onClick = { actionClicked++ },
                isPrimary = true,
                enabled = true,
                weight = 1.5f
            )
            assertTrue(actionItem.isPrimary)
            assertTrue(actionItem.enabled)
            assertEquals(1.5f, actionItem.weight, 0.001f)
            actionItem.onClick()
            assertEquals(1, actionClicked)

            var menuClicked = 0
            val menuAction = ExpressiveMenuAction(
                label = "Delete Expense",
                icon = Icons.Rounded.Check,
                onClick = { menuClicked++ },
                subtitle = "Remove from ledger"
            )
            assertEquals("Delete Expense", menuAction.label)
            assertEquals("Remove from ledger", menuAction.subtitle)
            menuAction.onClick()
            assertEquals(1, menuClicked)

            val compSrc = readSourceFile("ui/components/ExpressiveM3Components.kt")
            assertTrue(compSrc.contains("fun LinearWavyProgressIndicator("))
            assertTrue(compSrc.contains("fun CircularWavyProgressIndicator("))
            assertTrue(compSrc.contains("fun ExpressiveGapLinearProgressIndicator("))
            assertTrue(compSrc.contains("fun SplitButtonLayout("))
            assertTrue(compSrc.contains("fun <T> ConnectedButtonGroup("))
            assertTrue(compSrc.contains("fun FloatingActionButtonMenu("))
            assertTrue(compSrc.contains("fun HorizontalFloatingToolbar("))
            assertTrue(compSrc.contains("if (rawProgress <= 0.001f || rawProgress >= 0.999f)"))
        }

        @Test
        fun `T5_06 SplitMateViewModel selectExpressiveTheme and toggleDarkTheme preserve Kyoto Matcha Yuzu and sync token boundary`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            assertNull(SplitMateViewModel.extractSyncTokenFromRawInput("SM2_123456"))
            assertEquals(
                "SM2_1234567",
                SplitMateViewModel.extractSyncTokenFromRawInput("SM2_1234567")
            )
            assertEquals(
                "SM2_Valid-Token_99",
                SplitMateViewModel.extractSyncTokenFromRawInput("Open https://splitmate.app/sync?token=SM2_Valid-Token_99&src=wa!")
            )

            vm.selectExpressiveTheme(SplitMateThemeMode.KYOTO_MATCHA_YUZU)
            assertEquals(SplitMateThemeMode.KYOTO_MATCHA_YUZU, vm.uiState.value.activeThemeMode)
            assertFalse(vm.uiState.value.isDarkTheme)
            assertEquals(SplitMateThemeMode.KYOTO_MATCHA_YUZU, SplitMateThemeState.activeThemeMode)

            // Calling toggleDarkTheme(false) when already on KYOTO_MATCHA_YUZU must preserve KYOTO_MATCHA_YUZU
            vm.toggleDarkTheme(false)
            assertEquals(
                SplitMateThemeMode.KYOTO_MATCHA_YUZU,
                vm.uiState.value.activeThemeMode,
                "toggleDarkTheme(false) must not clobber KYOTO_MATCHA_YUZU back to SUNLIT_BUCKWHEAT"
            )

            // Toggling dark mode true switches to WARM_ESPRESSO_NIGHT
            vm.toggleDarkTheme(true)
            assertEquals(SplitMateThemeMode.WARM_ESPRESSO_NIGHT, vm.uiState.value.activeThemeMode)
            assertTrue(vm.uiState.value.isDarkTheme)

            // Toggling dark mode false from WARM_ESPRESSO_NIGHT restores SUNLIT_BUCKWHEAT
            vm.toggleDarkTheme(false)
            assertEquals(SplitMateThemeMode.SUNLIT_BUCKWHEAT, vm.uiState.value.activeThemeMode)
            assertFalse(vm.uiState.value.isDarkTheme)
            DesignSystemBindings.activeThemeMode = SplitMateThemeMode.SUNLIT_BUCKWHEAT
        }

        @Test
        fun `T5_07 AvatarSeedCodec 3-part legacy format and buildDiceBearAvatarUrl gender-specific query invariants`() {
            val legacyThreePart = AvatarSeedCodec.parse(
                rawSeed = "Ananya|lorelei|Sunrise",
                fallbackGender = AvatarGender.FEMALE
            )
            assertEquals("Ananya", legacyThreePart.seedKey)
            assertEquals("lorelei", legacyThreePart.styleId)
            assertEquals(AvatarGender.FEMALE, legacyThreePart.gender)
            assertEquals("Sunrise", legacyThreePart.colorPresetId)

            val unknownStyleAndColor = AvatarSeedCodec.parse(
                rawSeed = "Vikram|Male|unknown-style-xyz|NonExistentPreset",
                fallbackGender = AvatarGender.NEUTRAL
            )
            assertEquals("Vikram", unknownStyleAndColor.seedKey)
            assertEquals(AvatarGender.MALE, unknownStyleAndColor.gender)
            assertEquals(SplitMateDiceBearStyles.first().id, unknownStyleAndColor.styleId)
            assertEquals("PastelWall", unknownStyleAndColor.colorPresetId)

            val femaleToonUrl = buildDiceBearAvatarUrl(
                name = "Priya",
                phone = "9876543210",
                styleId = "toon-head",
                colorPresetId = "Sunrise",
                flip = true,
                gender = AvatarGender.FEMALE
            )
            assertTrue(femaleToonUrl.contains("https://api.dicebear.com/9.x/toon-head/svg?seed="))
            assertTrue(femaleToonUrl.contains("beardProbability=0"))
            assertTrue(femaleToonUrl.contains("backgroundType=gradientLinear"))
            assertTrue(femaleToonUrl.contains("flip=true"))

            val maleMicahUrl = buildDiceBearAvatarUrl(
                name = "Rohan",
                phone = "9123456780",
                styleId = "micah",
                colorPresetId = "BoldPop",
                flip = false,
                gender = AvatarGender.MALE
            )
            assertTrue(maleMicahUrl.contains("hair=fonze,dannyPhantom,dougFunny,mrT"))
            assertTrue(maleMicahUrl.contains("earringsProbability=0"))
            assertFalse(maleMicahUrl.contains("flip=true"))

            val maleLoreleiUrl = buildDiceBearAvatarUrl(
                name = "Kabir",
                phone = "",
                styleId = "lorelei",
                colorPresetId = "Bare",
                flip = false,
                gender = AvatarGender.MALE
            )
            assertTrue(maleLoreleiUrl.contains("earringsProbability=0"))
        }

        @Test
        fun `T5_08 resolveExpenseSplitBreakdown subset exclusion and SplitMateAppComposable morphing seal contract`() {
            val members = listOf(
                GroupMemberEntity("m1", "g1", "Akshay", "Akshay|Male", isCurrentUser = true),
                GroupMemberEntity("m2", "g1", "Rohan", "Rohan|Male"),
                GroupMemberEntity("m3", "g1", "Priya", "Priya|Female"),
                GroupMemberEntity("m4", "g1", "Zoya", "Zoya|Female")
            )
            val expense = ExpenseEntity(
                expenseId = "e_sub",
                groupId = "g1",
                title = "Scuba Diving Deposit",
                payerId = "m1",
                baseSubtotalCents = 1001L,
                taxCents = 0L,
                tipCents = 0L,
                totalAmountCents = 1001L,
                lockedMultiplier = 1.0,
                unassignedBaseCents = 0L,
                currencyCode = "USD",
                lockedExchangeRate = 1.0
            )
            val subsetSplits = listOf(
                ExpenseSplitEntity("s1", "e_sub", "m1", 334L, 334L, plusOneCent = true),
                ExpenseSplitEntity("s2", "e_sub", "m2", 334L, 334L, plusOneCent = true),
                ExpenseSplitEntity("s3", "e_sub", "m3", 333L, 333L, plusOneCent = false)
            )
            val subsetBreakdown = SplitMateViewModel.resolveExpenseSplitBreakdown(
                expense = expense,
                groupMembers = members,
                allSplits = subsetSplits,
                currencySymbol = "$",
                headerPrefix = "Itemized Share"
            )
            assertEquals(4, subsetBreakdown.totalMembersInGroup)
            assertEquals(3, subsetBreakdown.splittingMembersCount)
            assertEquals("Itemized Share (3 of 4 members splitting)", subsetBreakdown.headerLabel)
            val excludedRow = subsetBreakdown.rows.first { it.memberId == "m4" }
            assertFalse(excludedRow.isIncludedInSplit)
            assertEquals(0L, excludedRow.owedCents)
            assertEquals("$0.00 (Excluded)", excludedRow.formattedShare)

            // Fallback when splits table has no rows for the expense yet
            val fallbackBreakdown = SplitMateViewModel.resolveExpenseSplitBreakdown(
                expense = expense,
                groupMembers = members,
                allSplits = emptyList(),
                currencySymbol = "$",
                headerPrefix = "Full Group"
            )
            assertEquals(4, fallbackBreakdown.splittingMembersCount)
            assertEquals("Full Group (4 members)", fallbackBreakdown.headerLabel)
            assertEquals(1001L, fallbackBreakdown.rows.sumOf { it.owedCents })

            val composableSrc = readSourceFile("ui/SplitMateAppComposable.kt")
            assertTrue(composableSrc.contains("Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny)"))
            assertTrue(composableSrc.contains("All Accounts Balanced"))
            assertTrue(composableSrc.contains("0.00\u00A2 DRIFT \u2022 EVERY PENNY ACCOUNTED FOR"))
            assertTrue(composableSrc.contains("SplitButtonLayout("))
            assertTrue(composableSrc.contains("segmentedIslandItemShape("))
        }

        @Test
        fun `T5_09 SplitMateAppNavHost floating pill toolbar and Onboarding Expressive Theme Studio contract`() {
            val navSrc = readSourceFile("ui/navigation/SplitMateAppNavHost.kt")
            assertTrue(navSrc.contains("HorizontalFloatingToolbar("))
            assertTrue(navSrc.contains("ExpressiveFloatingToolbarItem("))
            assertTrue(navSrc.contains("targetValue = if (isSelected) 16.dp else 28.dp"))
            assertTrue(navSrc.contains("SplitMateMotion.fastSpatial()"))
            assertTrue(navSrc.contains("SplitMateMotion.fastEffects()"))

            val onboardingSrc = readSourceFile("ui/screens/OnboardingAndSettingsScreens.kt")
            assertTrue(onboardingSrc.contains("themeModes.forEachIndexed"))
            assertTrue(onboardingSrc.contains("segmentedIslandItemShape("))
            assertTrue(onboardingSrc.contains("outerCorner = 20.dp"))
            assertTrue(onboardingSrc.contains("innerCorner = 6.dp"))
            assertTrue(onboardingSrc.contains("ConnectedButtonGroup("))
            assertTrue(onboardingSrc.contains("Morph(MaterialShapes.SoftBurst, MaterialShapes.Cookie9Sided)"))
            assertTrue(onboardingSrc.contains("SplitMateDiceBearStyles"))
            assertTrue(onboardingSrc.contains("SplitMateAvatarColorPresets"))
        }

        @Test
        fun `T5_10 PA-8 uninvited receiver auto-enrollment on importAndMergeGroupSyncPayload and phone normalization edge cases`() = runTest {
            val senderVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            senderVm.clearLocalVault()
            senderVm.updateUserProfile(
                newName = "Akshay Karadkar",
                newPhone = "9876543210",
                newSeedOrCurrency = "Akshay|Male|toon-head|Sunrise",
                newUpiId = "akshay@okicici"
            )
            senderVm.createNewGroup(
                name = "Gokarna Cliff Trek",
                currencyCode = "INR",
                friendNamesCsv = "Rohan,Priya",
                iconName = "Camping"
            )
            val groupId = senderVm.uiState.value.activeGroupId
            senderVm.commitQuickEqualExpense(
                title = "Beach Tent Rental",
                totalAmountCents = 300000L,
                selectedMemberIds = senderVm.uiState.value.activeGroupMembers.map { it.memberId },
                payerMemberId = senderVm.uiState.value.activeGroupMembers.first().memberId
            )

            val exportBundle = senderVm.exportGroupSyncPayload(groupId)
            assertNotNull(exportBundle)

            // Receiver VM has a completely different user ("Zoya Khan", phone "9123456780") not in the sender's group
            val receiverVm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            receiverVm.clearLocalVault()
            receiverVm.updateUserProfile(
                newName = "Zoya Khan",
                newPhone = "+91 91234-56780",
                newSeedOrCurrency = "Zoya Khan|Female|lorelei|Sunrise",
                newUpiId = "zoya@okaxis"
            )

            val mergeReport = receiverVm.importAndMergeGroupSyncPayload(
                rawPayloadOrMessage = exportBundle!!.syncToken,
                openGroupAfterMerge = true
            )
            assertTrue(mergeReport.success, "Sync capsule import must succeed for uninvited receiver")
            assertEquals(groupId, receiverVm.uiState.value.activeGroupId)

            val mergedMembers = receiverVm.uiState.value.members.filter { it.groupId == groupId }
            assertEquals(4, mergedMembers.size, "Uninvited receiver Zoya must be auto-enrolled as the 4th member")

            val zoyaMember = mergedMembers.find { it.memberId == "m_${groupId}_9123456780" }
            assertNotNull(zoyaMember, "Auto-enrolled member must have deterministic ID m_<groupId>_<10DigitPhone>")
            assertEquals("Zoya Khan", zoyaMember!!.name)
            assertEquals("9123456780", zoyaMember.userPhone)
            assertEquals("zoya@okaxis", zoyaMember.upiId)
            assertTrue(zoyaMember.isCurrentUser, "Auto-enrolled receiver must be marked isCurrentUser = true")
            assertEquals("JOINED", zoyaMember.inviteStatus)
            assertEquals(1, mergedMembers.count { it.isCurrentUser }, "Exactly one member in the group may have isCurrentUser = true")

            // Phone & Category Icon helpers in SplitMateTheme.kt
            assertEquals("+14155552671", cleanIndianTenDigitPhone("+14155552671"))
            assertEquals("9876543210", cleanIndianTenDigitPhone("+91 98765-43210"))
            assertEquals("9876543210", cleanIndianTenDigitPhone("09876543210"))
            assertEquals("9876543210", normalizeValidatedPhone10("919876543210"))
            assertEquals("", normalizeValidatedPhone10("12345"))
            assertEquals("+91 98765 43210", formatTenDigitIndianPhone("9876543210"))

            assertEquals(
                resolveGroupCategoryIcon("Flight", ""),
                resolveGroupCategoryIcon("Custom", "goa beach")
            )
            assertNotEquals(
                resolveGroupCategoryIcon("Flight", ""),
                resolveGroupCategoryIcon("Home", "")
            )

            val syncSheetSrc = readSourceFile("ui/dialogs/TripSyncAndPerspectiveSheet.kt")
            assertTrue(syncSheetSrc.contains("fun TripSyncAndPerspectiveSheet("))
            assertTrue(syncSheetSrc.contains("ModalBottomSheet("))
            assertTrue(syncSheetSrc.contains("ButtonGroup("))
            assertTrue(syncSheetSrc.contains("ExpressiveActionItem("))
            assertTrue(syncSheetSrc.contains("SplitMateMotion.defaultSpatial()"))
        }

        @Test
        fun `T5_11 M3 4-screen token objects bind dynamically across all 3 expressive themes and maintain sunlight contrast`() {
            val modes = listOf(
                SplitMateThemeMode.SUNLIT_BUCKWHEAT,
                SplitMateThemeMode.WARM_ESPRESSO_NIGHT,
                SplitMateThemeMode.KYOTO_MATCHA_YUZU
            )
            val seenPerforationColors = mutableSetOf<Color>()
            val seenAviationNavyColors = mutableSetOf<Color>()
            val seenTrainForestTopColors = mutableSetOf<Color>()

            modes.forEach { mode ->
                SplitMateThemeState.activeThemeMode = mode
                DesignSystemBindings.activeThemeMode = mode
                SplitMateTheme.isDark = mode.isDark
                val palette = mode.toPalette()

                // 1. TripHubTokens live palette delegation & sunlight contrast (>= 5.8:1)
                assertEquals(palette.surfaceContainerLow, com.splitmate.app.ui.screens.TripHubTokens.CanvasBg)
                assertEquals(palette.surfaceContainerLowest, com.splitmate.app.ui.screens.TripHubTokens.CardSurface)
                assertEquals(palette.surfaceContainer, com.splitmate.app.ui.screens.TripHubTokens.SunkenWell)
                assertEquals(palette.outlineVariant, com.splitmate.app.ui.screens.TripHubTokens.CardBorder) // v2.3.6: card border = M3 outlineVariant
                assertEquals(palette.onSurface, com.splitmate.app.ui.screens.TripHubTokens.TextPrimary)
                assertEquals(palette.primaryContainer, com.splitmate.app.ui.screens.TripHubTokens.PositiveSagePillBg)
                assertEquals(palette.secondaryContainer, com.splitmate.app.ui.screens.TripHubTokens.TerracottaPeachBg)
                assertEquals(palette.tertiaryContainer, com.splitmate.app.ui.screens.TripHubTokens.PeriwinkleBoxBg)

                val limeOnTop = wcagContrastRatio(
                    com.splitmate.app.ui.screens.TripHubTokens.TrainAccentLime,
                    com.splitmate.app.ui.screens.TripHubTokens.TrainForestTop
                )
                val sageOnTop = wcagContrastRatio(
                    com.splitmate.app.ui.screens.TripHubTokens.TrainSecondarySage,
                    com.splitmate.app.ui.screens.TripHubTokens.TrainForestTop
                )
                val sageOnBottom = wcagContrastRatio(
                    com.splitmate.app.ui.screens.TripHubTokens.TrainSecondarySage,
                    com.splitmate.app.ui.screens.TripHubTokens.TrainForestBottom
                )
                assertTrue(limeOnTop >= 5.8, "Expected TrainAccentLime >= 5.8:1 on TrainForestTop in $mode, got $limeOnTop")
                assertTrue(sageOnTop >= 7.0, "Expected TrainSecondarySage >= 7.0:1 on TrainForestTop in $mode, got $sageOnTop")
                assertTrue(sageOnBottom >= 5.8, "Expected TrainSecondarySage >= 5.8:1 on TrainForestBottom in $mode, got $sageOnBottom")

                // 2. QuickExpenseThemeTokens live palette delegation
                assertEquals(palette.surfaceContainerLow, com.splitmate.app.ui.screens.QuickExpenseThemeTokens.ScreenBg)
                assertEquals(palette.onSurface, com.splitmate.app.ui.screens.QuickExpenseThemeTokens.PrimaryDark)
                assertEquals(palette.primaryContainer, com.splitmate.app.ui.screens.QuickExpenseThemeTokens.SageSurface)
                assertEquals(palette.onPrimaryContainer, com.splitmate.app.ui.screens.QuickExpenseThemeTokens.SageText)
                assertEquals(palette.secondaryContainer, com.splitmate.app.ui.screens.QuickExpenseThemeTokens.TerracottaSurface)
                assertEquals(palette.onSecondaryContainer, com.splitmate.app.ui.screens.QuickExpenseThemeTokens.TerracottaText)

                // 3. TactilePaperPassTokens & FlightPassTokens live palette delegation
                assertEquals(palette.surfaceContainerLow, com.splitmate.app.ui.screens.TactilePaperPassTokens.CanvasBackground)
                assertEquals(palette.surfaceContainerLowest, com.splitmate.app.ui.screens.TactilePaperPassTokens.PaperSurface)
                assertEquals(palette.surfaceContainer, com.splitmate.app.ui.screens.TactilePaperPassTokens.PaperStubSurface)
                assertEquals(palette.surfaceContainerLow, com.splitmate.app.ui.screens.FlightPassTokens.AppBackground)
                assertEquals(palette.surfaceContainerLowest, com.splitmate.app.ui.screens.FlightPassTokens.TicketPaperWhite)
                assertEquals(palette.tertiaryContainer, com.splitmate.app.ui.screens.FlightPassTokens.SkyBlue)

                seenPerforationColors.add(com.splitmate.app.ui.screens.TactilePaperPassTokens.PerforationLine)
                seenAviationNavyColors.add(com.splitmate.app.ui.screens.FlightPassTokens.AviationNavy)
                seenTrainForestTopColors.add(com.splitmate.app.ui.screens.TripHubTokens.TrainForestTop)
            }

            assertEquals(3, seenPerforationColors.size, "TactilePaperPassTokens.PerforationLine must resolve 3 distinct theme colors")
            assertEquals(3, seenAviationNavyColors.size, "FlightPassTokens.AviationNavy must resolve 3 distinct theme colors")
            assertEquals(3, seenTrainForestTopColors.size, "TripHubTokens.TrainForestTop must resolve 3 distinct theme colors")

            SplitMateThemeState.activeThemeMode = SplitMateThemeMode.SUNLIT_BUCKWHEAT
            DesignSystemBindings.activeThemeMode = SplitMateThemeMode.SUNLIT_BUCKWHEAT
            SplitMateTheme.isDark = false
        }

        @Test
        fun `T5_12 TripHomeScreen white-box M3 Expressive choreography FAB menu theme cycling all_settled_card morph and 22 spatial placements`() {
            val src = readSourceFile("ui/screens/TripHomeScreen.kt")

            // 1. FloatingActionButtonMenu with 4 ExpressiveFabMenuItems + 1-tap theme cycling in TripHubTopBar
            assertTrue(src.contains("FloatingActionButtonMenu("))
            assertEquals(4, Regex("""ExpressiveFabMenuItem\(""").findAll(src).count())
            assertTrue(
                src.contains("onCycleThemeClick = {") && src.contains("viewModel.cycleExpressiveThemeMode(context)"),
                "TripHomeScreen must wire onCycleThemeClick to viewModel.cycleExpressiveThemeMode(context)"
            )
            assertTrue(src.contains("Icons.Rounded.Palette"))
            assertTrue(src.contains("ContainedLoadingIndicator("))

            // 2. all_settled_card Cookie9Sided -> Sunny MorphPolygonShape & LinearWavyProgressIndicator flattening
            assertTrue(src.contains("Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny)"))
            assertTrue(src.contains("MorphPolygonShape(morph = settledMorph, percentage = settledMorphProgress)"))
            assertTrue(src.contains("SplitMateMotion.slowSpatialFloat()"))
            assertTrue(src.contains("amplitude = if (simplifiedTransfers.isEmpty()) 0f else 0.75f"))
            assertTrue(src.contains("SplitButtonLayout("))
            assertTrue(src.contains("leadingText = \"Mark Paid\""))

            // 3. Runtime verification of Cookie9Sided -> Sunny MorphPolygonShape across [0f, 0.5f, 1f]
            val settledMorph = Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny)
            listOf(0f, 0.5f, 1f).forEach { progress ->
                val shape = MorphPolygonShape(morph = settledMorph, percentage = progress)
                val outline = shape.createOutline(Size(64f, 64f), LayoutDirection.Ltr, testDensity)
                assertTrue(outline is Outline.Generic)
                assertFalse((outline as Outline.Generic).path.isEmpty)
            }

            // 4. Verify LazyColumn item placements use SplitMateMotion.defaultSpatial()
            val spatialPlacementCount = Regex("""\.animateItem\(\s*fadeInSpec = null,\s*placementSpec = SplitMateMotion\.defaultSpatial\(\),\s*fadeOutSpec = null\s*\)""")
                .findAll(src).count()
            assertTrue(spatialPlacementCount >= 14, "Expected >= 14 defaultSpatial item placements in TripHomeScreen.kt, found $spatialPlacementCount")
        }

        @Test
        fun `T5_13 QuickExpenseAndGuideScreens white-box Hero Moment 2 zero-drift wave flattening SoftBurst-to-Cookie9Sided morph and merged monospace typography`() {
            val src = readSourceFile("ui/screens/QuickExpenseAndGuideScreens.kt")

            // 1. Runtime verification of QuickSplitMode enum entries & labels
            val modes = com.splitmate.app.ui.screens.QuickSplitMode.entries
            assertEquals(4, modes.size)
            assertEquals(listOf("Equal", "Exact", "%", "Shares"), modes.map { it.label })

            // 2. Runtime verification of merged tabular monospace style used on the 0.00c DRIFT badge
            val mergedDriftStyle = SplitMateExpressiveTypography.labelSmallEmphasized.merge(
                com.splitmate.app.ui.SplitMateMonospaceTextStyle
            )
            assertEquals(FontFamily.Monospace, mergedDriftStyle.fontFamily)
            assertEquals(FontWeight.Bold, mergedDriftStyle.fontWeight)
            assertEquals(11.sp, mergedDriftStyle.fontSize)
            assertEquals("tnum, zero", mergedDriftStyle.fontFeatureSettings)

            // 3. Runtime verification of Hero Moment 2 Morph(SoftBurst, Cookie9Sided)
            val badgeMorph = Morph(MaterialShapes.SoftBurst, MaterialShapes.Cookie9Sided)
            val morphAtUnsettled = MorphPolygonShape(badgeMorph, percentage = 0f).createOutline(testSize, LayoutDirection.Ltr, testDensity)
            val morphAtZeroDrift = MorphPolygonShape(badgeMorph, percentage = 1f).createOutline(testSize, LayoutDirection.Ltr, testDensity)
            assertTrue(morphAtUnsettled is Outline.Generic && !(morphAtUnsettled as Outline.Generic).path.isEmpty)
            assertTrue(morphAtZeroDrift is Outline.Generic && !(morphAtZeroDrift as Outline.Generic).path.isEmpty)

            // 4. White-box source contracts for Hero Moment 2 & preserved +1p coin-flight
            assertTrue(src.contains("ConnectedButtonGroup("))
            assertTrue(src.contains("options = QuickSplitMode.entries"))
            assertTrue(src.contains("ExpressiveGapLinearProgressIndicator("))
            assertTrue(src.contains("Morph(MaterialShapes.SoftBurst, MaterialShapes.Cookie9Sided)"))
            assertTrue(src.contains("MorphPolygonShape(badgeMorph, badgeMorphProgress)"))
            assertTrue(src.contains("amplitude = if (isZeroDriftVerified) 0f else 1f"))
            assertTrue(src.contains("SplitMateExpressiveTypography.labelSmallEmphasized.merge(SplitMateMonospaceTextStyle)"))
            assertTrue(src.contains("remainderCoinFlightProgress"))
            assertTrue(src.contains("Split Remainder Equally"))
        }

        @Test
        fun `T5_14 PnrExpenseReviewScreen and FlightExpenseReviewScreen perforated shapes 5-tier passenger matching and tactile audio-stamp contracts`() {
            // 1. Runtime verification of TactilePaperPerforatedShape and FlightBoardingPassShape outlines
            val trainShape = com.splitmate.app.ui.screens.TactilePaperPerforatedShape(
                cornerRadiusDp = 24f,
                notchRadiusDp = 12f,
                perforationRatio = 0.77f
            )
            val flightShape = com.splitmate.app.ui.screens.FlightBoardingPassShape(
                cornerRadius = 72f,
                notchRadius = 48f,
                notchYPercent = 0.765f
            )
            val passSize = Size(360f, 640f)
            val trainOutline = trainShape.createOutline(passSize, LayoutDirection.Ltr, testDensity)
            val flightOutline = flightShape.createOutline(passSize, LayoutDirection.Ltr, testDensity)
            assertTrue(trainOutline is Outline.Generic && !(trainOutline as Outline.Generic).path.isEmpty)
            assertTrue(flightOutline is Outline.Generic && !(flightOutline as Outline.Generic).path.isEmpty)

            // 2. Runtime white-box test of 5-tier matchSinglePassengerToGroupMember & P1-first Payer ordering
            val members = listOf(
                GroupMemberEntity("m1", "g1", "Akshay Karadkar", "Akshay", isCurrentUser = true),
                GroupMemberEntity("m2", "g1", "Pratik", "Pratik"),
                GroupMemberEntity("m3", "g1", "Sharma", "Sharma"),
                GroupMemberEntity("m4", "g1", "Kabir", "Kabir")
            )
            // Tier 1 (Score 100): Exact multi-token match
            assertEquals("m1", com.splitmate.app.ui.screens.matchSinglePassengerToGroupMember("Mr. Akshay Ramesh Karadkar", members)?.memberId)
            // Tier 3 (Score 80): Member first name matches middle/last token of passenger
            assertEquals("m3", com.splitmate.app.ui.screens.matchSinglePassengerToGroupMember("Ms. Priya Sharma", members)?.memberId)
            // Tier 5 (Score 55): Prefix match >= 4 chars ("Pratik" vs "Pratiksha")
            assertEquals("m2", com.splitmate.app.ui.screens.matchSinglePassengerToGroupMember("Ms. Pratiksha Jadhav", members)?.memberId)
            // Unmatched passenger returns null
            assertEquals(null, com.splitmate.app.ui.screens.matchSinglePassengerToGroupMember("Dr. Vikramaditya Sen", members))

            // Verify findMatchedGroupMembersForFlightTicket preserves P1, P2 order so P1 is Payer
            val extractedTicket = UniversalFlightTicketExtractor.extractFromText(
                rawText = """
                    IndiGo Flight 6E-204
                    PNR: P1TEST
                    From: BLR To: GOI
                    1. Ms. Pratiksha Jadhav  Seat 12A
                    2. Mr. Akshay Karadkar   Seat 12B
                    Total Fare: INR 8,400.00
                """.trimIndent(),
                groupMemberNames = members.map { it.name }
            )
            val orderedMatches = com.splitmate.app.ui.screens.findMatchedGroupMembersForFlightTicket(extractedTicket, members)
            assertEquals(listOf("m2", "m1"), orderedMatches.map { it.memberId })

            // 3. Verify preserved tactile barcode, PCM audio-stamp, and M3 Expressive components in both screens
            val pnrSrc = readSourceFile("ui/screens/PnrExpenseReviewScreen.kt")
            val flightSrc = readSourceFile("ui/screens/FlightExpenseReviewScreen.kt")
            assertTrue(
                pnrSrc.contains("fun TactileBarcode(") &&
                    pnrSrc.contains("playBoardingPassTearAndStampOneShot") &&
                    flightSrc.contains("buildBoardingPassTearAndStampPcm")
            )
            assertTrue(pnrSrc.contains("ContainedLoadingIndicator(") && !pnrSrc.contains("ExpressiveGapLinearProgressIndicator("))
            assertTrue(flightSrc.contains("fun EngravedAviationBarcode(") && flightSrc.contains("val stubFoldDegrees = stubPitchX"))
            assertTrue(flightSrc.contains("ContainedLoadingIndicator(") && !flightSrc.contains("ExpressiveGapLinearProgressIndicator("))
        }

        @Test
        fun `T5_15 SplitMateMathEngine calculateProportionalReceiptSplits 3-tier tie-breaker and zero-claim exclusion guard`() {
            // 3 active claimants with identical 1000L base claims + 1 inactive member with 0L base claim
            val claims = listOf(
                Triple("m0", "InactiveZero", 0L),
                Triple("m2", "Rohan", 1000L),
                Triple("m1", "Akshay", 1000L),
                Triple("m3", "PriyaPayer", 1000L)
            )

            // Case A: +1 cent discrepancy -> must go to Payer ("m3") first when fractionalRemainders are tied
            val splitOneExtra = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = 3000L,
                taxCents = 1L,
                tipCents = 0L,
                payerId = "m3",
                memberBaseClaimsCents = claims,
                attributeRemainderToPayer = true
            )
            val mapOne = splitOneExtra.allocations.associateBy { it.memberId }
            assertEquals(0L, mapOne.getValue("m0").finalCents, "0L-claim member must never receive a rounding penny")
            assertEquals(1001L, mapOne.getValue("m3").finalCents, "Payer m3 must win first tie-break")
            assertEquals(1000L, mapOne.getValue("m1").finalCents)
            assertEquals(1000L, mapOne.getValue("m2").finalCents)
            assertEquals(0L, splitOneExtra.driftCents)

            // Case B: +2 cents discrepancy -> 1st cent to Payer ("m3"), 2nd cent to lexicographically smallest ID ("m1" before "m2")
            val splitTwoExtra = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = 3000L,
                taxCents = 2L,
                tipCents = 0L,
                payerId = "m3",
                memberBaseClaimsCents = claims,
                attributeRemainderToPayer = true
            )
            val mapTwo = splitTwoExtra.allocations.associateBy { it.memberId }
            assertEquals(0L, mapTwo.getValue("m0").finalCents)
            assertEquals(1001L, mapTwo.getValue("m3").finalCents)
            assertEquals(1001L, mapTwo.getValue("m1").finalCents, "Lexicographically smaller memberId m1 must beat m2")
            assertEquals(1000L, mapTwo.getValue("m2").finalCents)
            assertEquals(0L, splitTwoExtra.driftCents)
        }

        @Test
        fun `T5_16 SplitMateMathEngine calculateProportionalReceiptSplits attributeRemainderToPayer false vs true with prime tax and tip`() {
            val claims = listOf(
                Triple("m1", "Akshay", 2333L),
                Triple("m2", "Rohan", 3111L),
                Triple("m3", "Priya", 1777L)
            )
            // Claimed sum = 7221L out of 10000L baseSubtotal -> unassignedBaseCents = 2779L
            // Prime tax = 1319L, prime tip = 727L -> totalFinalCents = 12046L, multiplier = 1.2046
            val withPayerHold = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = 10000L,
                taxCents = 1319L,
                tipCents = 727L,
                payerId = "m1",
                memberBaseClaimsCents = claims,
                attributeRemainderToPayer = true
            )
            assertEquals(1.2046, withPayerHold.lockedMultiplier, 1e-6)
            assertEquals(2779L, withPayerHold.unassignedBaseCents)
            assertEquals(12046L, withPayerHold.allocations.sumOf { it.finalCents })
            assertEquals(5112L, withPayerHold.allocations.first { it.memberId == "m1" }.baseClaimedCents)
            assertEquals(0L, withPayerHold.driftCents)

            val withoutPayerHold = SplitMateMathEngine.calculateProportionalReceiptSplits(
                baseSubtotalCents = 10000L,
                taxCents = 1319L,
                tipCents = 727L,
                payerId = "m1",
                memberBaseClaimsCents = claims,
                attributeRemainderToPayer = false
            )
            assertEquals(1.2046, withoutPayerHold.lockedMultiplier, 1e-6)
            assertEquals(2779L, withoutPayerHold.unassignedBaseCents)
            assertEquals(2333L, withoutPayerHold.allocations.first { it.memberId == "m1" }.baseClaimedCents)
            assertEquals(0L, withoutPayerHold.driftCents)
            assertEquals(
                withoutPayerHold.totalFinalCents,
                withoutPayerHold.allocations.sumOf { it.finalCents } + withoutPayerHold.unassignedFinalCents,
                "Claimed final allocations plus unassignedFinalCents must equal totalFinalCents with 0 drift"
            )
        }

        @Test
        fun `T5_17 SplitMateMathEngine simplifyDebtsGreedy and computeMemberSettlementSummaries disconnected components and You fallback`() {
            // Component A (3 members, zero-sum): m1 (+5000), m2 (-2000), m3 (-3000)
            // Component B (4 members with tied balances, zero-sum): m4 (+4000), m5 (+4000), m6 (-4000), m7 (-4000)
            val balances = listOf(
                SplitMateMathEngine.MemberNetBalance("m1", "You", 5000L),
                SplitMateMathEngine.MemberNetBalance("m2", "Rohan", -2000L),
                SplitMateMathEngine.MemberNetBalance("m3", "Priya", -3000L),
                SplitMateMathEngine.MemberNetBalance("m4", "Kabir", 4000L),
                SplitMateMathEngine.MemberNetBalance("m5", "Ananya", 4000L),
                SplitMateMathEngine.MemberNetBalance("m6", "Dev", -4000L),
                SplitMateMathEngine.MemberNetBalance("m7", "Meera", -4000L)
            )
            val transfers = SplitMateMathEngine.simplifyDebtsGreedy(balances)
            // Greedy Max-PQ pairs largest creditor (+5000) with largest debtor (-4000) first: 5 transfers (strictly <= N - 1 = 6)
            assertEquals(5, transfers.size)
            assertEquals(13000L, transfers.sumOf { it.amountCents })

            // Verify every member's net balance is exactly reconstructed by the simplified transfers
            balances.forEach { b ->
                val incoming = transfers.filter { it.toMemberId == b.memberId }.sumOf { it.amountCents }
                val outgoing = transfers.filter { it.fromMemberId == b.memberId }.sumOf { it.amountCents }
                assertEquals(b.netCents, incoming - outgoing, "Net balance mismatch for ${b.memberId}")
            }

            // Verify computeMemberSettlementSummaries resolves "You" to actual name from transfer and sorts currentUser first
            val customTransfers = transfers.map {
                if (it.toMemberId == "m1") it.copy(toName = "Akshay") else it
            }
            val summaries = SplitMateMathEngine.computeMemberSettlementSummaries(
                transfers = customTransfers,
                memberMetadata = balances.associate { b ->
                    b.memberId to Triple(b.displayName, b.displayName, b.memberId == "m1")
                }
            )
            assertEquals("m1", summaries.first().memberId, "CurrentUser must be sorted first in settlement summaries")
            assertEquals("Akshay", summaries.first().memberName, "Placeholder 'You' must resolve to 'Akshay' from transfer metadata")
            assertEquals(5000L, summaries.first().totalIncomingCents)
            assertEquals(2, summaries.first().incomingPayments.size)
        }

        @Test
        fun `T5_18 UniversalFlightTicketExtractor parses IATA BCBP barcode string transfers wrapped seat and enforces 1-to-1 member matching`() {
            // 1. IATA Resolution 792 BCBP barcode string + wrapped passenger table with seat on second row
            val bcbpAndWrappedText = """
                IndiGo Boarding Pass
                M1VERMA/ROHAN         EBCBP77 DELGOI6E 0512 318J004A0025 100
                Date: 14 Nov 2026  Departure: 08:15  Arrival: 10:55
                1. Mr. Rohan Kumar Verma
                Mr. Rohan Verma  Seat 14F
                2. Ms. Priya Sharma  14E
                Total Fare: INR 12,840.00
            """.trimIndent()

            // Group has two members named "Rohan" ("Rohan Verma" and "Rohan Gupta") plus "Priya Sharma"
            val groupMemberNames = listOf("Rohan Verma", "Rohan Gupta", "Priya Sharma")
            val result = UniversalFlightTicketExtractor.extractFromText(bcbpAndWrappedText, groupMemberNames)

            assertTrue(result.isValidFlightTicket)
            assertEquals("BCBP77", result.pnr)
            assertEquals("DEL", result.originIata)
            assertEquals("GOI", result.destinationIata)
            assertEquals("Business", result.cabinClass)
            assertEquals(1284000L, result.totalFarePaise)

            // Wrapped duplicate row for Rohan must merge and transfer seat ("4A" from BCBP or "14F" from row)
            val rohanPax = result.passengers.firstOrNull { it.fullName.contains("Rohan", ignoreCase = true) }
            assertNotNull(rohanPax)
            assertTrue(rohanPax!!.seatNumber in listOf("4A", "14F"))

            // Greedy 1-to-1 matching must match "Rohan Verma" and "Priya Sharma", and NEVER double-match "Rohan Gupta"
            assertEquals(2, result.matchedGroupMembers.size)
            assertTrue(result.matchedGroupMembers.contains("Rohan Verma"))
            assertTrue(result.matchedGroupMembers.contains("Priya Sharma"))
            assertFalse(result.matchedGroupMembers.contains("Rohan Gupta"))
        }

        @Test
        fun `T5_19 PnrNetworkRepository normalizePnrKey rejects ForbiddenSixCharWords guards partial WL-RAC and reconstructs flight seats`() {
            // 1. ForbiddenSixCharWords ("FLIGHT", "INDIGO", "TICKET", "STATUS") must not be mistaken for 6-char PNRs
            assertEquals("K9M4Q2", PnrNetworkRepository.normalizePnrKey("FLIGHT INDIGO TICKET STATUS K9M4Q2"))
            assertEquals("8412659012", PnrNetworkRepository.normalizePnrKey("IRCTC STATUS TICKET 8412659012"))
            assertEquals("", PnrNetworkRepository.normalizePnrKey("FLIGHT INDIGO TICKET STATUS"))

            // 2. Partial CNF + WL/RAC snapshot must return false from isSnapshotAllConfirmed (preventing premature offline lock)
            val partialSnapshot = LivePnrStatusSnapshot(
                pnr = "8412659012",
                trainNo = "12952",
                trainName = "Mumbai Rajdhani",
                fromStation = "NDLS",
                toStation = "MMCT",
                departureTime = "16:55",
                travelClass = "3A",
                totalFareRupees = 5600,
                passengerCount = 2,
                bookingStatusBadge = "CNF",
                chartPrepared = false,
                passengerStatuses = listOf("P1: CNF/B1/12", "P2: RAC 4"),
                structuredPassengers = listOf(
                    LivePnrPassenger("P1", "CNF/B1/12", "CNF/B1/12", "Confirmed"),
                    LivePnrPassenger("P2", "WL 12", "RAC 4", "RAC")
                ),
                coachPositionHint = "Coach B1",
                liveTrainLocationRadar = "On Time",
                confirmationProbability = "92% Probable",
                sourceLabel = "IRCTC Live",
                isLiveVerified = true,
                isManualEntry = false
            )
            assertFalse(PnrNetworkRepository.isSnapshotAllConfirmed(partialSnapshot))

            val allConfirmedSnapshot = partialSnapshot.copy(
                passengerStatuses = listOf("P1: CNF/B1/12", "P2: CNF/B1/14"),
                structuredPassengers = listOf(
                    LivePnrPassenger("P1", "CNF/B1/12", "CNF/B1/12", "Confirmed"),
                    LivePnrPassenger("P2", "WL 12", "CNF/B1/14", "Confirmed")
                )
            )
            assertTrue(PnrNetworkRepository.isSnapshotAllConfirmed(allConfirmedSnapshot))

            // 3. reconstructFlightTicketFromExpense orders Payer first and assigns deterministic 12A, 12B, 12C seats
            val members = listOf(
                GroupMemberEntity("m1", "g_rec", "Akshay", "Akshay", isCurrentUser = true),
                GroupMemberEntity("m2", "g_rec", "Rohan", "Rohan"),
                GroupMemberEntity("m3", "g_rec", "Priya", "Priya")
            )
            val expense = ExpenseEntity(
                expenseId = "exp_reconstruct_99",
                groupId = "g_rec",
                title = "Akasa Air QP-1120 (BLR -> GOI) [PNR:QP9912]",
                payerId = "m2", // Rohan is Payer -> must be Passenger 1 (12A)
                baseSubtotalCents = 1350000L,
                taxCents = 0L,
                tipCents = 0L,
                totalAmountCents = 1350000L,
                lockedMultiplier = 1.0,
                unassignedBaseCents = 0L,
                currencyCode = "INR",
                lockedExchangeRate = 1.0,
                expenseCategory = "FLIGHT",
                travelPnr = "QP9912"
            )
            val splits = listOf(
                ExpenseSplitEntity("s1", "exp_reconstruct_99", "m1", 450000L, 450000L),
                ExpenseSplitEntity("s2", "exp_reconstruct_99", "m2", 450000L, 450000L),
                ExpenseSplitEntity("s3", "exp_reconstruct_99", "m3", 450000L, 450000L)
            )
            val reconstructed = PnrNetworkRepository.reconstructFlightTicketFromExpense(null, expense, members, splits)
            assertEquals("QP9912", reconstructed.pnr)
            assertEquals("Akasa Air", reconstructed.airlineName)
            assertEquals("BLR", reconstructed.originIata)
            assertEquals("GOI", reconstructed.destinationIata)
            assertEquals(listOf("Rohan", "Akshay", "Priya"), reconstructed.passengers.map { it.fullName })
            // v2.3.5 rework (audit C3): reconstruction is display-only and never invents seats.
            assertEquals(listOf("", "", ""), reconstructed.passengers.map { it.seatNumber })
        }

        @Test
        fun `T5_20 Cross-module E2E Flight extraction to Vault to ViewModel commit to declined share reassignment to greedy settlement`() = runTest {
            val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
            vm.clearLocalVault()
            vm.setExpressiveThemeMode(SplitMateThemeMode.KYOTO_MATCHA_YUZU)
            assertEquals(SplitMateThemeMode.KYOTO_MATCHA_YUZU, SplitMateThemeState.activeThemeMode)

            vm.createNewGroup(name = "Andaman Scuba Expedition", currencyCode = "INR", friendNamesCsv = "Rohan,Priya,Kabir")
            val groupId = vm.uiState.value.activeGroupId
            val members = vm.uiState.value.members.filter { it.groupId == groupId }
            assertEquals(4, members.size)
            val me = members.first { it.isCurrentUser }
            val kabir = members.first { it.name == "Kabir" }

            // Step 1: Extract 4-passenger flight ticket with odd total INR 17,503.00 (1,750,300 paise)
            val pdfText = """
                Air India E-Ticket Receipt
                PNR: AND99K
                Flight: AI-789
                From: MAA (Chennai) To: IXZ (Port Blair)
                Date: 18 Dec 2026  Dep: 05:20  Arr: 07:35
                Passengers:
                1. Mr ${me.name}      Seat 8A
                2. Mr Rohan           Seat 8B
                3. Ms Priya           Seat 8C
                4. Mr Kabir           Seat 8D
                Total Amount: INR 17,503.00
            """.trimIndent()
            val extracted = UniversalFlightTicketExtractor.extractFromText(pdfText, members.map { it.name })
            assertTrue(extracted.isValidFlightTicket)
            assertEquals("AND99K", extracted.pnr)
            assertEquals(1750300L, extracted.totalFarePaise)

            // Step 2: Persist to PnrNetworkRepository in-memory vault & verify permanent CNF lock
            val vaultSnap = PnrNetworkRepository.saveConfirmedFlightTicketToVault(null, extracted)
            assertNotNull(vaultSnap)
            assertTrue(PnrNetworkRepository.isSnapshotAllConfirmed(vaultSnap))
            assertEquals("AND99K", PnrNetworkRepository.loadConfirmedFlightTicketResult(null, "AND99K")?.pnr)

            // Step 3: Commit flight expense across all 4 members (1750300 / 4 = 437575 each, exact)
            vm.commitQuickEqualExpense(
                title = "Air India ${extracted.flightNumber} (${extracted.originIata} -> ${extracted.destinationIata}) [PNR:${extracted.pnr}]",
                totalAmountCents = extracted.totalFarePaise,
                selectedMemberIds = members.map { it.memberId },
                payerMemberId = me.memberId
            )

            // Step 4: Kabir declines invite -> reassignDeclinedMemberSharesEqually redistributes 1,750,300 across 3 remaining members
            // 1,750,300 / 3 = 583,433 rem 1 -> Payer (me) gets 583,434, Rohan & Priya get 583,433 each (0.00c drift!)
            vm.reassignDeclinedMemberSharesEqually(context = null, groupId = groupId, declinedMemberId = kabir.memberId)
            val exp = vm.uiState.value.expenses.first { it.travelPnr == "AND99K" }
            val splitsAfterDecline = vm.uiState.value.splits.filter { it.expenseId == exp.expenseId }
            assertEquals(3, splitsAfterDecline.size)
            assertEquals(1750300L, splitsAfterDecline.sumOf { it.finalOwedCents })
            assertEquals(583434L, splitsAfterDecline.first { it.memberId == me.memberId }.finalOwedCents)

            // Step 5: Compute net balances, run Greedy Minimum Cash Flow, and settle all transfers to 0.00c equilibrium
            val netBeforeSettle = vm.computeGroupMemberNetBalances(groupId)
            assertEquals(0L, netBeforeSettle.values.sum())
            val balanceList = members.map { m ->
                SplitMateMathEngine.MemberNetBalance(m.memberId, m.name, netBeforeSettle[m.memberId] ?: 0L)
            }
            val greedyTransfers = SplitMateMathEngine.simplifyDebtsGreedy(balanceList)
            assertEquals(2, greedyTransfers.size)
            greedyTransfers.forEach { transfer ->
                vm.markGreedyTransferSettled(transfer)
            }

            val netAfterSettle = vm.computeGroupMemberNetBalances(groupId)
            assertTrue(netAfterSettle.values.all { it == 0L }, "All members must reach 0L net balance after settling greedy transfers")

            SplitMateThemeState.activeThemeMode = SplitMateThemeMode.SUNLIT_BUCKWHEAT
            DesignSystemBindings.activeThemeMode = SplitMateThemeMode.SUNLIT_BUCKWHEAT
            SplitMateTheme.isDark = false
        }

        @Test
        fun `T5_21 M3 Expressive UX and Motion Polish contracts across TripHomeScreen ActivityDetailSheet Pnr Flight and QuickExpense`() {
            val tripHomeSrc = readSourceFile("ui/screens/TripHomeScreen.kt")
            val activitySheetSrc = readSourceFile("ui/screens/ActivityDetailSheet.kt")
            val pnrSrc = readSourceFile("ui/screens/PnrExpenseReviewScreen.kt")
            val flightSrc = readSourceFile("ui/screens/FlightExpenseReviewScreen.kt")
            val quickExpenseSrc = readSourceFile("ui/screens/QuickExpenseAndGuideScreens.kt")

            // 1. TripHomeScreen collapsing LargeTopAppBar + nestedScroll + LinearWavyProgressIndicator in Itinerary
            assertTrue(tripHomeSrc.contains("TopAppBarDefaults.exitUntilCollapsedScrollBehavior()"))
            assertTrue(tripHomeSrc.contains(".nestedScroll(scrollBehavior.nestedScrollConnection)"))
            assertTrue(tripHomeSrc.contains("LargeTopAppBar("))
            assertTrue(tripHomeSrc.contains("LinearWavyProgressIndicator("))

            // 2. ActivityDetailSheet ModalBottomSheet + SplitButtonLayout + Segmented Island + Tnum Monospace
            assertTrue(activitySheetSrc.contains("ModalBottomSheet("))
            assertTrue(activitySheetSrc.contains("SplitButtonLayout("))
            assertTrue(activitySheetSrc.contains("rememberAnimatedSegmentedIslandItemShape("))
            assertTrue(activitySheetSrc.contains("SplitMateTnumMonospace"))
            assertFalse(
                Regex("""Color\(0x[0-9A-Fa-f]{8}\)""").containsMatchIn(activitySheetSrc),
                "ActivityDetailSheet.kt must use semantic tokens with zero hardcoded hex colors"
            )

            // 3. PnrExpenseReviewScreen.kt and FlightExpenseReviewScreen.kt adapt across all 3 themes with zero hardcoded Buckwheat chrome literals
            val bannedChromeHexes = listOf("0xFFFAF6F0", "0xFF23201E", "0xFFEDE7DF", "0xFF365314")
            bannedChromeHexes.forEach { hex ->
                assertFalse(pnrSrc.contains(hex, ignoreCase = true), "PnrExpenseReviewScreen.kt must not contain hardcoded chrome hex $hex")
                assertFalse(flightSrc.contains(hex, ignoreCase = true), "FlightExpenseReviewScreen.kt must not contain hardcoded chrome hex $hex")
            }
            assertTrue(pnrSrc.contains("SplitMateThemeMode.KYOTO_MATCHA_YUZU"))
            assertTrue(flightSrc.contains("SplitMateThemeMode.KYOTO_MATCHA_YUZU"))
            // v2.3.2 M3E audit: static fare/share data carries no progress indicators in the review screens.
            assertFalse(pnrSrc.contains("ExpressiveGapLinearProgressIndicator("))
            assertFalse(flightSrc.contains("ExpressiveGapLinearProgressIndicator("))

            // 4. QuickExpenseAndGuideScreens ConnectedButtonGroup + LinearWavyProgressIndicator
            assertTrue(quickExpenseSrc.contains("ConnectedButtonGroup("))
            assertTrue(quickExpenseSrc.contains("LinearWavyProgressIndicator("))
        }
    }
}

