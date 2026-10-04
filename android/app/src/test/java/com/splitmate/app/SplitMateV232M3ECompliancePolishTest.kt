package com.splitmate.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

/**
 * SplitMate v2.3.2 (versionCode 50) Material 3 Expressive compliance audit regression suite.
 *
 * Source-inspection contracts for the v2.3.2 bug-fix release:
 * - Phase 1: no double status-bar inset above the Trip Hub LargeTopAppBar; innerPadding consumed.
 * - Phase 2: static financial data (shares, fares, spend) carries no wavy/linear progress lines or dividers.
 * - Phase 3: SplitButtonLayout for Edit Expense and the empty-state Create/Join pair; springs over tween().
 * - Phase 4: Cookie9Sided/SoftBurst avatars and hero icons; SingleChoiceSegmentedButtonRow payer picker;
 *   20.dp card tokens.
 * - Phase 5: editorial displaySmall/headlineLarge totals; borderless surfaceContainer berth cells.
 *
 * Strictly zero Unicode emoji characters appear in this file.
 */
@DisplayName("SplitMate v2.3.2 M3 Expressive compliance audit")
class SplitMateV232M3ECompliancePolishTest {

    private fun resolveProjectSourceRoot(): File {
        val candidates = listOf(
            File("src/main/java/com/splitmate/app"),
            File("app/src/main/java/com/splitmate/app"),
            File("android/app/src/main/java/com/splitmate/app"),
            File("/usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Unable to locate SplitMate source root from ${File(".").absolutePath}")
    }

    private fun resolveBuildGradleFile(): File {
        val candidates = listOf(
            File("build.gradle"),
            File("app/build.gradle"),
            File("android/app/build.gradle"),
            File("/usr/local/google/home/karadkar/splitmate/android/app/build.gradle")
        )
        return candidates.firstOrNull { it.exists() && it.isFile }
            ?: error("Unable to locate app/build.gradle")
    }

    private fun src(relativePath: String): String {
        val file = File(resolveProjectSourceRoot(), relativePath)
        assertTrue(file.exists(), "Expected source file to exist: ${file.path}")
        return file.readText()
    }

    /** Returns the body of the first top-level/private function named [name] (brace-balanced). */
    private fun functionBody(source: String, name: String): String {
        val match = Regex("""fun\s+$name\s*\(""").find(source)
            ?: error("Function $name not found")
        val start = match.range.first
        // Paren-balance the parameter list so lambda types / default `{}` values are skipped.
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

    private val appPath = "ui/SplitMateAppComposable.kt"
    private val tripHomePath = "ui/screens/TripHomeScreen.kt"
    private val sheetPath = "ui/screens/ActivityDetailSheet.kt"
    private val pnrPath = "ui/screens/PnrExpenseReviewScreen.kt"
    private val flightPath = "ui/screens/FlightExpenseReviewScreen.kt"
    private val componentsPath = "ui/components/ExpressiveM3Components.kt"
    private val shapesPath = "ui/components/ExpressiveShapesAndMotion.kt"

    // ------------------------------------------------------------------
    // RELEASE
    // ------------------------------------------------------------------

    @Test
    fun `R_01 build gradle is versionCode 53 versionName 2_3_5`() {
        val gradle = resolveBuildGradleFile().readText()
        assertTrue(gradle.contains("versionCode 53"))
        assertTrue(gradle.contains("versionName \"2.3.5\""))
        assertFalse(gradle.contains("versionName \"2.3.3\""))
    }

    // ------------------------------------------------------------------
    // PHASE 1: INSETS
    // ------------------------------------------------------------------

    @Test
    fun `P1_01 dashboard scaffold skips root top inset when immersive Trip Hub owns the status bar`() {
        val app = src(appPath)
        assertTrue(app.contains("val tripHubOwnsStatusBar = isImmersiveTripHubOpen && !isTwoPaneTabletWithGroups"))
        assertTrue(app.contains("if (tripHubOwnsStatusBar) 0.dp else innerPadding.calculateTopPadding()"))
        assertTrue(app.contains(".consumeWindowInsets(PaddingValues(top = appliedTopInsetPadding))"))
        assertFalse(app.contains(".padding(top = innerPadding.calculateTopPadding())\n                .imePadding()"))
    }

    @Test
    fun `P1_02 Trip Hub, IRCTC and Flight content consume the Scaffold innerPadding they apply`() {
        listOf(tripHomePath, pnrPath, flightPath).forEach { path ->
            val s = src(path)
            assertTrue(
                s.contains(".padding(innerPadding)\n") && s.contains(".consumeWindowInsets(innerPadding)"),
                "$path must apply and consume innerPadding on its content only"
            )
        }
        // The LargeTopAppBar itself must never receive innerPadding.
        val topBar = functionBody(src(tripHomePath), "TripHubTopBar")
        assertTrue(topBar.contains("LargeTopAppBar("))
        assertFalse(topBar.contains("innerPadding"))
    }

    // ------------------------------------------------------------------
    // PHASE 2: NO PROGRESS LINES ON STATIC FINANCIAL DATA
    // ------------------------------------------------------------------

    @Test
    fun `P2_01 ActivityDetailSheet has no wavy indicator and no divider`() {
        val sheet = src(sheetPath)
        assertFalse(sheet.contains("LinearWavyProgressIndicator"))
        assertFalse(sheet.contains("CircularWavyProgressIndicator"))
        assertFalse(sheet.contains("HorizontalDivider"))
        assertFalse(sheet.contains("shareFraction"))
    }

    @Test
    fun `P2_02 IRCTC and Flight review screens have no progress lines or dividers`() {
        listOf(pnrPath, flightPath).forEach { path ->
            val s = src(path)
            assertFalse(s.contains("ExpressiveGapLinearProgressIndicator"), "$path gap indicator")
            assertFalse(s.contains("LinearWavyProgressIndicator"), "$path wavy indicator")
            assertFalse(s.contains("LinearProgressIndicator("), "$path linear indicator")
            assertFalse(s.contains("HorizontalDivider("), "$path divider")
            assertFalse(s.contains("memberShareProgress"), "$path per-member share bar")
        }
        assertFalse(src(pnrPath).contains("confirmedRatio"))
    }

    @Test
    fun `P2_03 Trip spend strip shows no wavy harmony indicators`() {
        val strip = functionBody(src(tripHomePath), "CompactPerspectiveNetBalanceStrip")
        assertFalse(strip.contains("WavyProgressIndicator"))
        assertFalse(strip.contains("harmonyProgress"))
    }

    // ------------------------------------------------------------------
    // PHASE 3: ARCHITECTURE & MOTION
    // ------------------------------------------------------------------

    @Test
    fun `P3_01 Edit Expense is a full-width SplitButtonLayout`() {
        val sheet = src(sheetPath)
        assertTrue(sheet.contains("SplitButtonLayout("))
        assertTrue(sheet.contains("leadingText = \"Edit Expense\""))
        assertTrue(sheet.contains("fillWidth = true"))
        val components = src(componentsPath)
        assertTrue(components.contains("fillWidth: Boolean = false"))
        assertTrue(components.contains("val buttonHeight = if (fillWidth) 48.dp else 40.dp"))
    }

    @Test
    fun `P3_02 empty state condenses Create First Group and Join with Code into one SplitButtonLayout`() {
        val app = src(appPath)
        assertTrue(app.contains("leadingText = \"Create First Group\""))
        assertTrue(app.contains("label = \"Join with Code\""))
        assertFalse(app.contains("Text(\"Create First Group\""))
    }

    @Test
    fun `P3_03 one-shot UI animations use MotionScheme springs not tween`() {
        assertFalse(src(pnrPath).contains("tween("))
        assertFalse(src(flightPath).contains("tween("))
        assertFalse(src(appPath).contains("fadeIn(tween(240))"))
        assertFalse(src(appPath).contains("fadeOut(tween(180))"))
        assertTrue(src(flightPath).contains("animationSpec = SplitMateMotion.fastEffects(),\n        label = \"GateStampAlpha\""))
    }

    // ------------------------------------------------------------------
    // PHASE 4: GEOMETRY
    // ------------------------------------------------------------------

    @Test
    fun `P4_01 toShape extension mirrors the official MaterialShapes API`() {
        assertTrue(src(shapesPath).contains("fun RoundedPolygon.toShape(rotationDegrees: Float = 0f): Shape"))
    }

    @Test
    fun `P4_02 avatars and group hero icons use Cookie9Sided or SoftBurst`() {
        val avatar = functionBody(src(tripHomePath), "TripHubMemberAvatar")
        assertTrue(avatar.contains("MaterialShapes.Cookie9Sided.toShape()"))
        assertFalse(avatar.contains(".clip(CircleShape)\n                .background(presetBg)"))
        val token = functionBody(src("ui/dialogs/GroupAndSettlementDialogs.kt"), "AvatarToken")
        assertTrue(token.contains("MaterialShapes.Cookie9Sided.toShape()"))
        val app = src(appPath)
        assertTrue(Regex("""\.clip\(MaterialShapes\.Cookie9Sided\.toShape\(\)\)""").findAll(app).count() >= 3)
        val theme = src("ui/SplitMateTheme.kt")
        assertTrue(theme.contains("MaterialShapes.SoftBurst"))
    }

    @Test
    fun `P4_03 IRCTC who-paid picker is a SingleChoiceSegmentedButtonRow that scrolls for 4+ members`() {
        val pnr = src(pnrPath)
        val picker = functionBody(pnr, "PayerSegmentedButtonRow")
        assertTrue(picker.contains("SingleChoiceSegmentedButtonRow("))
        assertTrue(picker.contains("SegmentedButton("))
        assertTrue(picker.contains("SegmentedButtonDefaults.itemShape(index = index, count = members.size)"))
        assertTrue(picker.contains(".horizontalScroll(rememberScrollState())"))
        assertTrue(picker.contains("overflow = TextOverflow.Ellipsis"))
        assertFalse(pnr.contains("\"Paid by \${member.name}\""))
    }

    @Test
    fun `P4_04 card tokens use 20dp and button tokens 16dp`() {
        val app = src(appPath)
        assertTrue(app.contains("val RadiusCard = RoundedCornerShape(20.dp)"))
        assertTrue(app.contains("val RadiusButton = RoundedCornerShape(16.dp)"))
        assertTrue(src("ui/screens/OnboardingAndSettingsScreens.kt").contains("val RadiusCard = RoundedCornerShape(20.dp)"))
        assertTrue(src("ui/screens/QuickExpenseAndGuideScreens.kt").contains("val RadiusCard = RoundedCornerShape(20.dp)"))
    }

    // ------------------------------------------------------------------
    // PHASE 5: TYPOGRAPHY & SURFACES
    // ------------------------------------------------------------------

    @Test
    fun `P5_01 financial totals use editorial displaySmall or headlineLarge auto-fit text`() {
        val components = src(componentsPath)
        assertTrue(components.contains("fun EditorialFinancialTotalText("))
        assertTrue(components.contains("result.didOverflowWidth"))
        assertTrue(components.contains("softWrap = false"))

        val strip = functionBody(src(tripHomePath), "CompactPerspectiveNetBalanceStrip")
        assertTrue(strip.contains("EditorialFinancialTotalText("))
        assertTrue(strip.contains("MaterialTheme.typography.headlineLarge"))

        val pnr = src(pnrPath)
        assertTrue(pnr.contains("text = totalFareDisplay,\n                    style = MaterialTheme.typography.displaySmall"))

        val sheet = src(sheetPath)
        assertTrue(sheet.contains("EditorialFinancialTotalText("))
        assertTrue(sheet.contains("MaterialTheme.typography.displaySmall"))
    }

    @Test
    fun `P5_02 train berth cells are borderless surfaceContainerHigh washes`() {
        val card = functionBody(src(tripHomePath), "DeepGreenTrainTicketCard")
        assertTrue(card.contains("MaterialTheme.colorScheme.surfaceContainerHigh.copy("))
        assertFalse(card.contains("TrainBerthCellBorder"))
        assertFalse(card.contains("color = TripHubTokens.TrainBerthCellBg"))
    }

    @Test
    fun `P5_03 money math engine untouched by UI audit`() {
        // Integer-cent largest-remainder split still sums exactly (no drift introduced by UI work).
        val allocations = SplitMateMathEngine.splitEquallyZeroDrift(
            totalCents = 85360L,
            members = listOf("a" to "A", "b" to "B", "c" to "C"),
            payerId = "a"
        )
        assertEquals(85360L, allocations.sumOf { it.finalCents })
        assertEquals(listOf(28454L, 28453L, 28453L), allocations.map { it.finalCents })
    }
}
