package com.splitmate.app

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

/**
 * SplitMate v2.4.0-rc21 — Material 3 Expressive (`material_compose_components` skill) P0 + P1 + P2
 * verification suite:
 * - Theme setup: `LocalTonalElevationEnabled provides false` & global `"tnum, zero"` tabular lining numerals.
 * - Adaptive shell: `NavigationSuiteScaffoldLayout` (`ShortNavigationBar` on compact + `WideNavigationRail`
 *   with header FAB on medium/expanded) + `MoneyScaffoldSnackbarHost` (`Snackbar` slot customization).
 * - Expressive shared components: `ButtonGroup` with `OverflowIndicator`, `ConnectedButtonGroup` with
 *   `FlowRow` + `selectableGroup()`, `FloatingActionButtonMenu` `BackHandler`, `TooltipBox` + `PlainTooltip`.
 * - Sheets, dialogs & pickers: zero manual `BackHandler` inside `PlaceDetailSheet` / `StayPinSheet`,
 *   `DateRangePicker(showModeToggle = true)`, full-screen `EditLoggedExpenseDialog`, `Cookie9Sided` avatars.
 * - Discovery, transit & lists: `PullToRefreshBox`, `HorizontalMultiBrowseCarousel`,
 *   `HorizontalUncontainedCarousel`, `DockedSearchBar`, `SegmentedListItem`, and zero static
 *   `LinearWavyProgressIndicator` on money data.
 */
@DisplayName("SplitMate v2.4.0-rc21 GM3 Expressive Skill Audit Verification")
class SplitMateV240Rc21Gm3ExpressiveSkillAuditTest {

    private fun resolveProjectSourceRoot(): File {
        val candidates = listOf(
            File("src/main/java/com/splitmate/app"),
            File("app/src/main/java/com/splitmate/app"),
            File("android/app/src/main/java/com/splitmate/app"),
            File("/usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Unable to locate SplitMate source root")
    }

    private fun src(relativePath: String): String =
        File(resolveProjectSourceRoot(), relativePath).readText()

    private fun stripComments(source: String): String =
        source.replace(Regex("""/\*[\s\S]*?\*/"""), "")
            .replace(Regex("""(?m)^\s*//.*$"""), "")

    @Test
    fun `RC21_01 SplitMateExpressiveTheme disables M2 tonal elevation overlays and preserves tabular figures`() {
        val theme = stripComments(src("ui/SplitMateTheme.kt"))
        assertTrue(
            theme.contains("LocalTonalElevationEnabled provides false"),
            "MaterialExpressiveTheme must disable legacy M2 tonal elevation overlays per SKILL.md Step 2"
        )
        assertTrue(
            theme.contains("fontFeatureSettings = \"tnum, zero\""),
            "ProvideTextStyle must preserve global tabular lining numerals + slashed zero"
        )
    }

    @Test
    fun `RC21_02 Shared ExpressiveM3Components provide OverflowIndicator, FlowRow reflow, FAB BackHandler and Tooltips`() {
        val comp = stripComments(src("ui/components/ExpressiveM3Components.kt"))
        assertTrue(
            comp.contains("ButtonGroupDefaults.OverflowIndicator"),
            "ButtonGroup must wire ButtonGroupDefaults.OverflowIndicator for 200% font-scale overflow"
        )
        assertTrue(
            comp.contains("FlowRow(") && comp.contains(".selectableGroup()"),
            "ConnectedButtonGroup must use FlowRow + Modifier.selectableGroup() for accessible multi-line reflow"
        )
        assertTrue(
            comp.contains("BackHandler(enabled = expanded)"),
            "FloatingActionButtonMenu must collapse on Back press when expanded"
        )
        assertTrue(
            comp.contains("TooltipBox(") && comp.contains("PlainTooltip"),
            "ExpressivePlainTooltipBox must wrap icon actions with official M3 TooltipBox + PlainTooltip"
        )
    }

    @Test
    fun `RC21_03 Adaptive NavigationSuiteScaffold uses ShortNavigationBar on compact and WideNavigationRail on expanded`() {
        val app = stripComments(src("ui/SplitMateAppComposable.kt"))
        assertTrue(
            app.contains("NavigationSuiteScaffold") && app.contains("ShortNavigationBar(") && app.contains("WideNavigationRail("),
            "Main dashboard shell must use NavigationSuiteScaffold with ShortNavigationBar + WideNavigationRail"
        )
        assertTrue(
            app.contains("MoneyScaffoldSnackbarHost("),
            "Main dashboard Scaffold must claim LocalMoneySnackbar via MoneyScaffoldSnackbarHost"
        )
        assertFalse(
            app.contains("LinearWavyProgressIndicator("),
            "No static LinearWavyProgressIndicator on money/ledger cards (GEMINI.md Rule #4)"
        )
    }

    @Test
    fun `RC21_04 ModalBottomSheets preserve Predictive Back and DateRangePicker enables keyboard input mode`() {
        val placeSheet = stripComments(src("ui/screens/plan/PlaceDetailSheet.kt"))
        val staySheet = stripComments(src("ui/screens/plan/StayPinSheet.kt"))
        assertFalse(
            placeSheet.contains("BackHandler"),
            "PlaceDetailSheet must not intercept BackHandler over ModalBottomSheet Predictive Back"
        )
        assertFalse(
            staySheet.contains("BackHandler"),
            "StayPinSheet must not intercept BackHandler over ModalBottomSheet Predictive Back"
        )
        val daysUi = stripComments(src("ui/screens/plan/TripDaysUi.kt"))
        assertTrue(
            daysUi.contains("showModeToggle = true"),
            "DateRangePicker must enable keyboard input mode (showModeToggle = true) for accessibility"
        )
    }

    @Test
    fun `RC21_05 Explore Guide and Transit PNR adopt PullToRefreshBox, Carousels, DockedSearchBar and Full-Screen Edit`() {
        val explore = stripComments(src("ui/screens/plan/ExploreGuideView.kt"))
        assertTrue(explore.contains("PullToRefreshBox("), "ExploreGuideView supports PullToRefreshBox")
        assertTrue(explore.contains("HorizontalMultiBrowseCarousel("), "ExploreGuideView renders HorizontalMultiBrowseCarousel")
        assertTrue(explore.contains("DockedSearchBar("), "DestinationSearch uses official DockedSearchBar")

        val pnr = stripComments(src("ui/screens/PnrExpenseReviewScreen.kt"))
        assertTrue(pnr.contains("PullToRefreshBox("), "PnrExpenseReviewScreen supports PullToRefreshBox")
        assertTrue(pnr.contains("HorizontalUncontainedCarousel("), "PnrExpenseReviewScreen renders HorizontalUncontainedCarousel for logged tickets")
        assertTrue(pnr.contains("ConnectedButtonGroup("), "PnrExpenseReviewScreen uses ConnectedButtonGroup for Live vs Manual mode")

        val dialogs = stripComments(src("ui/dialogs/GroupAndSettlementDialogs.kt"))
        val editDialog = dialogs.substringAfter("fun EditLoggedExpenseDialog(")
        assertTrue(
            editDialog.contains("DialogProperties(usePlatformDefaultWidth = false)"),
            "EditLoggedExpenseDialog uses a full-screen M3 Dialog instead of a cramped AlertDialog"
        )
    }
}
