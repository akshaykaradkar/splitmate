package com.splitmate.app

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.splitmate.app.ui.KyotoMatchaYuzuPalette
import com.splitmate.app.ui.SplitMateExpressivePalette
import com.splitmate.app.ui.SunlitBuckwheatPalette
import com.splitmate.app.ui.WarmEspressoNightPalette
import com.splitmate.app.ui.components.ExperimentalMaterial3ExpressiveApi
import com.splitmate.app.ui.components.GuideShapeTokens
import com.splitmate.app.ui.components.LoadingIndicatorDefaults
import com.splitmate.app.ui.components.MaterialShapes
import com.splitmate.app.ui.components.SplitButtonDefaults
import com.splitmate.app.ui.components.SplitMateMotion
import com.splitmate.app.ui.components.SplitMateMotionScheme
import com.splitmate.app.ui.components.SplitMateStandardMotionTokens
import com.splitmate.app.ui.components.WavyProgressIndicatorDefaults
import com.splitmate.app.ui.components.indeterminateWavySegments
import com.splitmate.app.ui.components.isReducedMotionScale
import com.splitmate.app.ui.components.loadingIndicatorFrame
import com.splitmate.app.ui.components.remainingMinVisibleMillis
import com.splitmate.app.ui.surfaceContainerRoles
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

/**
 * SplitMate v2.3.4 (Trip Guide & Smart Loop) design-system foundations.
 *
 * Source-inspection contracts (house style) plus pure-logic checks for:
 * - Decision #6: every M3 surfaceContainer role is mapped to the Buckwheat ladder.
 * - Decision #3: indeterminate LinearWavyProgressIndicator, polygon-driven loaders, the slot
 *   SplitButtonLayout and the MotionScheme shim over SplitMateMotion.
 * - Decision #4: the module-wide -opt-in flag and the annotated look-alike surface.
 * - Decision #5: member avatars stay Cookie9Sided (P4_02); 16.dp squircles for POI thumbnails.
 * - Decisions #7/#8: GEMINI.md addendum (borderless guide cards, in-flight-only wavy rule).
 * - Audit 5.7 / 6: reduced motion and the 400ms minimum visibility rule.
 *
 * Strictly zero Unicode emoji characters appear in this file.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@DisplayName("SplitMate v2.3.4 design-system foundations")
class SplitMateV234DesignSystemTest {

    private fun resolveProjectSourceRoot(): File {
        val candidates = listOf(
            File("src/main/java/com/splitmate/app"),
            File("app/src/main/java/com/splitmate/app"),
            File("android/app/src/main/java/com/splitmate/app")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Unable to locate SplitMate source root from ${File(".").absolutePath}")
    }

    private fun resolveFile(vararg candidates: String): File =
        candidates.map { File(it) }.firstOrNull { it.exists() && it.isFile }
            ?: error("Unable to locate any of ${candidates.toList()} from ${File(".").absolutePath}")

    private fun src(relativePath: String): String {
        val file = File(resolveProjectSourceRoot(), relativePath)
        assertTrue(file.exists(), "Expected source file to exist: ${file.path}")
        return file.readText()
    }

    /** Returns the body of the first function named [name] (brace-balanced). */
    private fun functionBody(source: String, name: String): String {
        val match = Regex("""fun\s+$name\s*\(""").find(source)
            ?: error("Function $name not found")
        val start = match.range.first
        var parenDepth = 0
        var paramsEnd = -1
        for (i in match.range.last until source.length) {
            when (source[i]) {
                '(' -> parenDepth++
                ')' -> {
                    parenDepth--
                    if (parenDepth == 0) { paramsEnd = i; break }
                }
            }
        }
        check(paramsEnd > 0) { "Unbalanced parameter list in $name" }
        val open = source.indexOf('{', paramsEnd)
        var depth = 0
        for (i in open until source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(start, i + 1)
                }
            }
        }
        error("Unbalanced braces in $name")
    }

    private fun occurrences(haystack: String, needle: String): Int =
        Regex(Regex.escape(needle)).findAll(haystack).count()

    private fun springOf(spec: Any): SpringSpec<*> {
        assertTrue(spec is SpringSpec<*>, "Expected a SpringSpec but was ${spec::class.java.simpleName}")
        return spec as SpringSpec<*>
    }

    private fun Color.brightness(): Float = red + green + blue

    private val themePath = "ui/SplitMateTheme.kt"
    private val componentsPath = "ui/components/ExpressiveM3Components.kt"
    private val shapesPath = "ui/components/ExpressiveShapesAndMotion.kt"
    private val tripHomePath = "ui/screens/TripHomeScreen.kt"

    // ------------------------------------------------------------------
    // DECISION #6: THEME SURFACE ROLES
    // ------------------------------------------------------------------

    @Test
    fun `DS_01 theme maps every surfaceContainer role into the colorScheme copy`() {
        val theme = src(themePath)
        assertTrue(theme.contains("val surfaceRoles = animatedPalette.surfaceContainerRoles()"))
        listOf(
            "surfaceBright = surfaceRoles.bright",
            "surfaceDim = surfaceRoles.dim",
            "surfaceContainerLowest = surfaceRoles.lowest",
            "surfaceContainerLow = surfaceRoles.low",
            "surfaceContainer = surfaceRoles.container",
            "surfaceContainerHigh = surfaceRoles.high",
            "surfaceContainerHighest = surfaceRoles.highest"
        ).forEach { mapping ->
            assertTrue(theme.contains(mapping), "SplitMateExpressiveTheme colorScheme must set $mapping")
        }
        // The mapping must live inside the colorScheme.copy(...) block of the theme composable.
        val themeBody = functionBody(theme, "SplitMateExpressiveTheme")
        assertTrue(themeBody.contains("surfaceContainerHighest = surfaceRoles.highest"))
    }

    @Test
    fun `DS_02 Buckwheat roles resolve to the canonical warm tokens`() {
        val roles = SunlitBuckwheatPalette.surfaceContainerRoles()
        assertEquals(Color(0xFFFFFFFF), roles.lowest)
        assertEquals(Color(0xFFFAF6F0), roles.low)
        assertEquals(Color(0xFFF4EFE6), roles.container)
        assertEquals(Color(0xFFEDE6DA), roles.high)
        assertEquals(Color(0xFFE4DCCD), roles.highest)
        assertEquals(roles.low, roles.bright)
        assertEquals(roles.highest, roles.dim)
    }

    @Test
    fun `DS_03 every palette variant maps a monotonic tonal ladder and correct bright and dim`() {
        val lightPalettes: List<SplitMateExpressivePalette> = listOf(SunlitBuckwheatPalette, KyotoMatchaYuzuPalette)
        lightPalettes.forEach { p ->
            val r = p.surfaceContainerRoles()
            val ladder = listOf(r.lowest, r.low, r.container, r.high, r.highest).map { it.brightness() }
            ladder.zipWithNext().forEach { (a, b) -> assertTrue(a > b, "${p.mode} light ladder must darken") }
            assertEquals(p.surfaceContainerLow, r.bright)
            assertEquals(p.surfaceContainerHighest, r.dim)
        }
        val dark = WarmEspressoNightPalette.surfaceContainerRoles()
        val darkLadder = listOf(dark.lowest, dark.low, dark.container, dark.high, dark.highest).map { it.brightness() }
        darkLadder.zipWithNext().forEach { (a, b) -> assertTrue(a < b, "Espresso ladder must lighten") }
        assertEquals(WarmEspressoNightPalette.surfaceContainerHighest, dark.bright)
        assertEquals(WarmEspressoNightPalette.surfaceContainerLowest, dark.dim)
    }

    // ------------------------------------------------------------------
    // DECISION #3: INDETERMINATE WAVY BAR
    // ------------------------------------------------------------------

    @Test
    fun `DS_04 indeterminate LinearWavyProgressIndicator overload exists without a progress parameter`() {
        val comp = src(componentsPath)
        assertEquals(3, occurrences(comp, "fun LinearWavyProgressIndicator("))
        assertTrue(comp.contains("fun LinearWavyProgressIndicator(\n    modifier: Modifier = Modifier,"))
        assertTrue(comp.contains("if (reducedMotion) 0f else amplitude.coerceIn(0f, 1f)"))
        assertTrue(comp.contains("indeterminateWavySegments(cycle)"))
        assertTrue(comp.contains(".progressSemantics()"))
        assertTrue(comp.contains("liveRegion = LiveRegionMode.Polite"))
        // Existing determinate contracts stay intact.
        assertTrue(comp.contains("val rawProgress = progress().coerceIn(0f, 1f)"))
        assertTrue(comp.contains("if (rawProgress <= 0.001f || rawProgress >= 0.999f)"))
    }

    @Test
    fun `DS_05 indeterminate segments travel edge to edge with eased head and tail`() {
        assertTrue(indeterminateWavySegments(0f).isEmpty())
        assertTrue(indeterminateWavySegments(1f).isEmpty())

        val mid = indeterminateWavySegments(0.375f)
        assertEquals(1, mid.size)
        assertEquals(0.25f, mid[0].start, 1e-4f)
        assertEquals(0.75f, mid[0].end, 1e-4f)
        assertEquals(0.5f, mid[0].length, 1e-4f)

        val overlap = indeterminateWavySegments(0.6f)
        assertEquals(2, overlap.size)
        assertEquals(0.0744f, overlap[0].start, 1e-3f)
        assertEquals(0.96f, overlap[1].end, 1e-3f)

        var minStart = 1f
        var maxEnd = 0f
        (0..1000).forEach { step ->
            indeterminateWavySegments(step / 1000f).forEach { s ->
                minStart = minOf(minStart, s.start)
                maxEnd = maxOf(maxEnd, s.end)
            }
        }
        assertTrue(minStart <= 0.01f, "segments must start at the leading edge")
        assertTrue(maxEnd >= 0.99f, "segments must reach the trailing edge")
    }

    @Test
    fun `DS_06 indeterminate segments stay in range, sorted and never overlap`() {
        (0..2000).forEach { step ->
            val segments = indeterminateWavySegments(step / 2000f)
            assertTrue(segments.size <= 2)
            segments.forEach { s ->
                assertTrue(s.start in 0f..1f && s.end in 0f..1f, "segment out of range at $step")
                assertTrue(s.start <= s.end)
            }
            segments.zipWithNext().forEach { (a, b) ->
                assertTrue(a.end <= b.start, "segments overlap at step $step: $a $b")
            }
        }
    }

    @Test
    fun `DS_07 indeterminate segments wrap negative and large cycles and reject non-finite input`() {
        assertEquals(indeterminateWavySegments(0.375f), indeterminateWavySegments(-0.625f))
        assertEquals(indeterminateWavySegments(0.375f), indeterminateWavySegments(3.375f), "wraps by whole cycles")
        assertTrue(indeterminateWavySegments(Float.NaN).isEmpty())
        assertTrue(indeterminateWavySegments(Float.POSITIVE_INFINITY).isEmpty())
        val reduced = WavyProgressIndicatorDefaults.ReducedMotionSegment
        assertTrue(reduced.start >= 0f && reduced.end <= 1f && reduced.length > 0f)
    }

    // ------------------------------------------------------------------
    // DECISION #3: POLYGON LOADERS
    // ------------------------------------------------------------------

    @Test
    fun `DS_08 ContainedLoadingIndicator and LoadingIndicator accept polygons and a11y label`() {
        val comp = src(componentsPath)
        assertEquals(
            2,
            occurrences(comp, "polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons")
        )
        assertTrue(comp.contains("containerShape: Shape = CircleShape"))
        val contained = functionBody(comp, "ContainedLoadingIndicator")
        assertTrue(contained.contains("contentDescription: String? = null"))
        assertTrue(contained.contains("LoadingIndicatorDefaults.morphSpring()"))
        assertTrue(contained.contains("MorphPolygonShape("))
        assertFalse(contained.contains("tween("), "morph loop must be spring-driven")
        assertFalse(contained.contains("rememberInfiniteTransition"))
        // Existing default sequence preserved for current call sites.
        assertTrue(comp.contains("get() = MaterialShapes.morphSequence"))
        assertEquals(
            listOf(MaterialShapes.Circle, MaterialShapes.Square, MaterialShapes.Cookie9Sided, MaterialShapes.SoftBurst),
            LoadingIndicatorDefaults.GuideHeroPolygons
        )
        assertEquals(7, LoadingIndicatorDefaults.IndeterminateIndicatorPolygons.size)
    }

    @Test
    fun `DS_09 loadingIndicatorFrame sequences morphs, clamps overshoot and wraps seamlessly`() {
        val initial = loadingIndicatorFrame(animatedStage = 0f, targetStage = 0, polygonCount = 4)
        assertEquals(0, initial.morphIndex)
        assertEquals(0f, initial.morphProgress, 0f)
        assertEquals(0f, initial.rotationDegrees, 0f)

        val half = loadingIndicatorFrame(0.5f, 1, 4)
        assertEquals(0, half.morphIndex)
        assertEquals(0.5f, half.morphProgress, 1e-5f)
        assertEquals(45f, half.rotationDegrees, 1e-3f)

        val overshoot = loadingIndicatorFrame(1.08f, 1, 4)
        assertEquals(1f, overshoot.morphProgress, 0f)
        val undershoot = loadingIndicatorFrame(-0.05f, 1, 4)
        assertEquals(0f, undershoot.morphProgress, 0f)

        val secondCycle = loadingIndicatorFrame(4.3f, 5, 4)
        assertEquals(0, secondCycle.morphIndex)
        assertEquals(0.3f, secondCycle.morphProgress, 1e-4f)
        assertEquals(3, loadingIndicatorFrame(3.5f, 4, 4).morphIndex)

        val wrap = 4 * LoadingIndicatorDefaults.STAGE_WRAP_CYCLES
        val beforeWrap = loadingIndicatorFrame(wrap + 0.25f, wrap + 1, 4)
        val afterWrap = loadingIndicatorFrame(0.25f, 1, 4)
        assertEquals(afterWrap.morphIndex, beforeWrap.morphIndex)
        assertEquals(afterWrap.morphProgress, beforeWrap.morphProgress, 1e-3f)
        assertEquals(afterWrap.rotationDegrees, beforeWrap.rotationDegrees, 1e-2f)
        assertEquals(
            0f,
            (LoadingIndicatorDefaults.STAGE_WRAP_CYCLES * LoadingIndicatorDefaults.DEGREES_PER_STAGE) % 360f,
            0f
        )

        val single = loadingIndicatorFrame(2.5f, 3, 1)
        assertEquals(0, single.morphIndex)
        assertEquals(0f, single.morphProgress, 0f)
        assertEquals(0, loadingIndicatorFrame(Float.NaN, 3, 4).morphIndex)
    }

    // ------------------------------------------------------------------
    // AUDIT 5.7 / 6: REDUCED MOTION + MIN VISIBILITY
    // ------------------------------------------------------------------

    @Test
    fun `DS_10 reduced motion rule reads ANIMATOR_DURATION_SCALE and freezes loaders`() {
        assertTrue(isReducedMotionScale(0f))
        assertTrue(isReducedMotionScale(-1f))
        assertFalse(isReducedMotionScale(0.5f))
        assertFalse(isReducedMotionScale(1f))
        val shapes = src(shapesPath)
        assertTrue(shapes.contains("Settings.Global.ANIMATOR_DURATION_SCALE"))
        assertTrue(shapes.contains("fun rememberReducedMotionEnabled(): Boolean"))
        val contained = functionBody(src(componentsPath), "ContainedLoadingIndicator")
        assertTrue(contained.contains("if (reducedMotion || sequence.size < 2) return@LaunchedEffect"))
        assertTrue(contained.contains("sequence.first().toShape()"))
    }

    @Test
    fun `DS_11 in-flight wavy bar honours the 400ms minimum and animates out`() {
        assertEquals(400L, WavyProgressIndicatorDefaults.MIN_VISIBLE_MILLIS)
        assertEquals(300L, remainingMinVisibleMillis(shownAtMillis = 1_000L, nowMillis = 1_100L, minVisibleMillis = 400L))
        assertEquals(0L, remainingMinVisibleMillis(1_000L, 1_500L, 400L))
        assertEquals(0L, remainingMinVisibleMillis(1_000L, 1_400L, 400L))
        assertEquals(400L, remainingMinVisibleMillis(1_000L, 900L, 400L), "clock skew never exceeds the minimum")
        assertEquals(0L, remainingMinVisibleMillis(0L, 0L, -5L))
        val comp = src(componentsPath)
        val inFlight = functionBody(comp, "InFlightWavyProgressIndicator")
        assertTrue(inFlight.contains("rememberMinimumVisibility(active = inFlight, minVisibleMillis = minVisibleMillis)"))
        assertTrue(inFlight.contains("shrinkVertically(animationSpec = SplitMateMotion.fastEffects())"))
        assertTrue(inFlight.contains("fadeOut(animationSpec = SplitMateMotion.fastEffects())"))
    }

    // ------------------------------------------------------------------
    // DECISION #3: SLOT SPLIT BUTTON
    // ------------------------------------------------------------------

    @Test
    fun `DS_12 slot-based SplitButtonLayout overload mirrors the official API`() {
        val comp = src(componentsPath)
        assertEquals(2, occurrences(comp, "fun SplitButtonLayout("))
        assertTrue(comp.contains("    leadingButton: @Composable () -> Unit,\n    trailingButton: @Composable () -> Unit,"))
        assertTrue(comp.contains("spacing: Dp = SplitButtonDefaults.Spacing"))
        assertTrue(comp.contains("menuContent: (@Composable ColumnScope.() -> Unit)? = null"))
        assertTrue(comp.contains("object SplitButtonDefaults"))
        assertTrue(comp.contains("fun LeadingButton("))
        assertTrue(comp.contains("fun TrailingButton(\n        checked: Boolean,\n        onCheckedChange: (Boolean) -> Unit,"))
        assertTrue(comp.contains("semantics { stateDescription = stateText }"))
        assertEquals(2.dp, SplitButtonDefaults.Spacing)
        assertEquals(4.dp, SplitButtonDefaults.InnerCornerSize)
        // Opinionated overload (Edit Expense, Create First Group) untouched.
        assertTrue(comp.contains("fillWidth: Boolean = false"))
        assertTrue(comp.contains("val buttonHeight = if (fillWidth) SplitButtonDefaults.MediumContainerHeight else SplitButtonDefaults.ContainerHeight"))
        assertTrue(comp.contains("if (checked) 180f else 0f"))
        // v2.4.0: delegates to the official M3 Expressive split button.
        assertTrue(comp.contains("androidx.compose.material3.SplitButtonLayout("))
    }

    // ------------------------------------------------------------------
    // DECISION #3: MOTIONSCHEME SHIM
    // ------------------------------------------------------------------

    @Test
    fun `DS_13 expressive MotionScheme is backed 1 to 1 by SplitMateMotion springs`() {
        val scheme = SplitMateMotionScheme.expressive()
        val fast = springOf(scheme.fastSpatialSpec<Float>())
        assertEquals(SplitMateMotion.FAST_SPATIAL_DAMPING, fast.dampingRatio, 0f)
        assertEquals(SplitMateMotion.FAST_SPATIAL_STIFFNESS, fast.stiffness, 0f)
        val default = springOf(scheme.defaultSpatialSpec<Float>())
        assertEquals(SplitMateMotion.DEFAULT_SPATIAL_DAMPING, default.dampingRatio, 0f)
        assertEquals(SplitMateMotion.DEFAULT_SPATIAL_STIFFNESS, default.stiffness, 0f)
        val slow = springOf(scheme.slowSpatialSpec<Float>())
        assertEquals(SplitMateMotion.SLOW_SPATIAL_STIFFNESS, slow.stiffness, 0f)
        assertEquals(SplitMateMotion.FAST_EFFECTS_STIFFNESS, springOf(scheme.fastEffectsSpec<Float>()).stiffness, 0f)
        assertEquals(SplitMateMotion.DEFAULT_EFFECTS_STIFFNESS, springOf(scheme.defaultEffectsSpec<Float>()).stiffness, 0f)
        assertEquals(SplitMateMotion.SLOW_EFFECTS_STIFFNESS, springOf(scheme.slowEffectsSpec<Float>()).stiffness, 0f)
        listOf(scheme.fastEffectsSpec<Float>(), scheme.defaultEffectsSpec<Float>(), scheme.slowEffectsSpec<Float>())
            .forEach { assertEquals(1f, springOf(it).dampingRatio, 0f, "effects springs never overshoot") }
        // Short aliases resolve to the same tokens.
        assertEquals(fast.stiffness, springOf(scheme.fastSpatial<Float>()).stiffness, 0f)
        assertEquals(default.stiffness, springOf(scheme.defaultSpatial<Float>()).stiffness, 0f)
        assertEquals(slow.stiffness, springOf(scheme.slowSpatial<Float>()).stiffness, 0f)
    }

    @Test
    fun `DS_14 standard and reduced MotionSchemes plus LocalMotionScheme in the theme`() {
        val standard = SplitMateMotionScheme.standard()
        assertEquals(SplitMateStandardMotionTokens.FAST_SPATIAL_STIFFNESS, springOf(standard.fastSpatialSpec<Float>()).stiffness, 0f)
        assertEquals(SplitMateStandardMotionTokens.DEFAULT_SPATIAL_STIFFNESS, springOf(standard.defaultSpatialSpec<Float>()).stiffness, 0f)
        assertEquals(SplitMateStandardMotionTokens.SLOW_SPATIAL_STIFFNESS, springOf(standard.slowSpatialSpec<Float>()).stiffness, 0f)
        assertEquals(0.9f, springOf(standard.defaultSpatialSpec<Float>()).dampingRatio, 0f)
        assertEquals(SplitMateMotion.FAST_EFFECTS_STIFFNESS, springOf(standard.fastEffectsSpec<Float>()).stiffness, 0f)

        val reduced = SplitMateMotionScheme.reduced()
        listOf(
            reduced.fastSpatialSpec<Float>(), reduced.defaultSpatialSpec<Float>(), reduced.slowSpatialSpec<Float>(),
            reduced.fastEffectsSpec<Float>(), reduced.defaultEffectsSpec<Float>(), reduced.slowEffectsSpec<Float>()
        ).forEach { assertTrue(it is SnapSpec<*>, "reduced motion must snap") }

        val shapes = src(shapesPath)
        assertTrue(shapes.contains("interface SplitMateMotionScheme"))
        assertTrue(shapes.contains("val LocalMotionScheme = staticCompositionLocalOf { SplitMateMotionScheme.expressive() }"))
        val theme = src(themePath)
        assertTrue(theme.contains("LocalMotionScheme provides motionScheme"))
        assertTrue(theme.contains("if (reducedMotion) SplitMateMotionScheme.reduced() else SplitMateMotionScheme.expressive()"))
    }

    // ------------------------------------------------------------------
    // DECISION #4: OPT-IN
    // ------------------------------------------------------------------

    @Test
    fun `DS_15 module-wide opt-in flag and annotated look-alike surface`() {
        val gradle = resolveFile("build.gradle", "app/build.gradle", "android/app/build.gradle").readText()
        // v2.3.6 Wave 4: both the local look-alike opt-in and the real material3 Expressive opt-in
        assertTrue(gradle.contains("\"-opt-in=com.splitmate.app.ui.components.ExperimentalMaterial3ExpressiveApi\""))
        assertTrue(gradle.contains("\"-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi\""))
        assertTrue(gradle.contains("jvmTarget = '17'"))

        val comp = src(componentsPath)
        assertTrue(comp.contains("@RequiresOptIn(message = \"This Material 3 Expressive API is experimental.\")"))
        assertTrue(comp.contains("annotation class ExperimentalMaterial3ExpressiveApi"))
        listOf(
            "LinearWavyProgressIndicator",
            "CircularWavyProgressIndicator",
            "ButtonGroup",
            "<T> ConnectedButtonGroup",
            "FloatingActionButtonMenu",
            "ContainedLoadingIndicator",
            "LoadingIndicator",
            "SplitButtonLayout",
            "HorizontalFloatingToolbar",
            "VerticalFloatingToolbar",
            "InFlightWavyProgressIndicator"
        ).forEach { name ->
            val declared = occurrences(comp, "@Composable\nfun $name(")
            val annotated = occurrences(comp, "@ExperimentalMaterial3ExpressiveApi\n@Composable\nfun $name(")
            assertTrue(declared > 0, "$name must be declared")
            assertEquals(declared, annotated, "every $name overload must carry @ExperimentalMaterial3ExpressiveApi")
        }
    }

    // ------------------------------------------------------------------
    // DECISION #5: GEOMETRY
    // ------------------------------------------------------------------

    @Test
    fun `DS_16 Cookie9Sided member avatar contract P4_02 still intact`() {
        val avatar = functionBody(src(tripHomePath), "TripHubMemberAvatar")
        assertTrue(avatar.contains("MaterialShapes.Cookie9Sided.toShape()"))
        assertTrue(src(themePath).contains("MaterialShapes.SoftBurst"))
        assertSame(MaterialShapes.Cookie9Sided, GuideShapeTokens.MemberAvatarPolygon)
        assertSame(MaterialShapes.SoftBurst, GuideShapeTokens.MemberAvatarAltPolygon)
        assertEquals(16.dp, GuideShapeTokens.PlaceThumbnailCorner)
    }

    // ------------------------------------------------------------------
    // DECISIONS #7 / #8: GEMINI.md ADDENDUM
    // ------------------------------------------------------------------

    @Test
    fun `DS_17 GEMINI md documents the v2_3_4 design-system addendum`() {
        val gemini = resolveFile("../../GEMINI.md", "../GEMINI.md", "GEMINI.md").readText()
        assertTrue(gemini.contains("v2.3.4 Addendum"))
        assertTrue(gemini.contains("borderless `ElevatedCard`"))
        assertTrue(gemini.contains("explicit exception"))
        assertTrue(gemini.contains("400ms"))
        assertTrue(gemini.contains("Never show it under static, cached or offline data"))
        assertTrue(gemini.contains("Cookie9Sided"))
        assertTrue(gemini.contains("place/POI thumbnails"))
        assertTrue(gemini.contains("surfaceContainerLowest"))
        // The original integer-cent mandate is preserved.
        assertTrue(gemini.contains("Store all monetary amounts in integer cents (`Long`)"))
    }

    @Test
    fun `DS_18 money math engine untouched by design-system work`() {
        val allocations = SplitMateMathEngine.splitEquallyZeroDrift(
            totalCents = 85360L,
            members = listOf("a" to "A", "b" to "B", "c" to "C"),
            payerId = "a"
        )
        assertEquals(85360L, allocations.sumOf { it.finalCents })
        assertEquals(listOf(28454L, 28453L, 28453L), allocations.map { it.finalCents })
    }
}
