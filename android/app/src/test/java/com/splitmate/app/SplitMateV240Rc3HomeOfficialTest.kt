package com.splitmate.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * v2.4.0 rc3: home / Ledgers controls are the official Material 3 Expressive components.
 * Source contracts plus one Robolectric check that the avatar's official IconButton still tells
 * TalkBack "Open settings" (role Button, click label and content description).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SplitMateV240Rc3HomeOfficialTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun src(path: String) = File("src/main/java/com/splitmate/app/$path").readText()

    @Test
    fun `RC3H_01 avatar IconButton keeps the Open settings role, label and description`() {
        var clicks = 0
        compose.setContent {
            // Same composition as the Ledgers top bar avatar.
            IconButton(
                onClick = { clicks++ },
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Open settings" }
            ) {
                Box(Modifier.size(38.dp).semantics { onClick(label = "Open settings", action = null) })
            }
        }
        val node = compose.onNode(hasContentDescription("Open settings"))
        val config = node.fetchSemanticsNode().config
        assertEquals(Role.Button, config.getOrNull(SemanticsProperties.Role))
        val action = config.getOrNull(SemanticsActions.OnClick)
        assertEquals("Open settings", action?.label)
        assertTrue(action?.action != null, "the merged click action must still run the button's onClick")
        node.performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `RC3H_02 floating toolbar wrappers delegate to the official components`() {
        val comp = src("ui/components/ExpressiveM3Components.kt")
        val toolbar = comp.substringAfter("object FloatingToolbarDefaults {").substringBefore("fun LoadingIndicator(")
        assertTrue(toolbar.contains("androidx.compose.material3.FloatingToolbarDefaults.StandardFloatingActionButton("))
        assertTrue(toolbar.contains("androidx.compose.material3.HorizontalFloatingToolbar("))
        assertTrue(toolbar.contains("androidx.compose.material3.VerticalFloatingToolbar("))
        assertTrue(toolbar.contains("base.floatingToolbarVerticalNestedScroll("))
        assertFalse(toolbar.contains(".shadow("), "the official toolbar owns its elevation")
        assertFalse(toolbar.contains("Surface("), "no hand-built pill container")
        val nav = src("ui/navigation/SplitMateAppNavHost.kt")
        val bar = nav.substringAfter("fun SplitMateGlobalBottomBar(").substringBefore("fun ExpressiveFloatingToolbarItem(")
        assertTrue(bar.contains("androidx.compose.material3.FloatingToolbarDefaults.VibrantFloatingActionButton("))
        assertFalse(bar.contains("shadowElevation"))
    }

    @Test
    fun `RC3H_03 Ledgers controls use official components with press-morph shapes`() {
        val app = src("ui/SplitMateAppComposable.kt")
        // Tablet: official expanded FAB with default shape, official WideNavigationRail.
        val fab = app.substringBefore("bottomBar = {").substringAfterLast("ExtendedFloatingActionButton(")
        assertTrue(fab.contains("expanded = isToolbarExpanded"))
        assertFalse(fab.substringBefore("modifier = Modifier").contains("shape = "))
        assertTrue(app.contains("androidx.compose.material3.WideNavigationRail("))
        // Every Button-family call in the file uses the official press-morph shapes.
        val fixedShapeButton = Regex(
            "\\b(Button|FilledTonalButton|OutlinedButton|TextButton)\\(\\s*\\n(?:(?!\\n\\s*\\) \\{)[\\s\\S])*?\\n\\s*shape = "
        )
        assertFalse(fixedShapeButton.containsMatchIn(app), "no Button with a fixed shape and no press morph")
        // Top bar: theme cycling and avatar are official icon buttons.
        val pill = app.substringAfter("fun ExpressiveThemeModePill(").substringBefore("\n// ====")
        assertTrue(pill.contains("OutlinedIconButton(") && pill.contains("shapes = IconButtonDefaults.shapes()"))
        assertTrue(pill.contains("testTag(\"TopBarExpressiveThemePill\")"))
        // New group: official dialog container and toggles instead of a hand scale bounce.
        assertTrue(app.contains("BasicAlertDialog(onDismissRequest = { showNewGroupDialog = false })"))
        assertTrue(app.contains("shapes = ToggleButtonDefaults.shapes()"))
        assertTrue(app.contains("shapes = IconButtonDefaults.toggleableShapes()"))
        assertFalse(app.contains("shadowElevation = 18.dp"))
        // Edit / Delete pair is an official ButtonGroup.
        assertTrue(app.contains("androidx.compose.material3.ButtonGroup("))
    }

    @Test
    fun `RC3H_04 Live Sync shows a loader only while a sync is in flight`() {
        val app = src("ui/SplitMateAppComposable.kt")
        val row = app.substringAfter("if (uiState.isOfflineMode) {").substringBefore("text = if (uiState.isOfflineMode) \"Offline\" else \"Live Sync\"")
        assertTrue(row.contains("} else if (uiState.isCloudSyncing) {"))
        val loaderBranch = row.substringAfter("} else if (uiState.isCloudSyncing) {").substringBefore("} else {")
        assertTrue(loaderBranch.contains("ContainedLoadingIndicator("))
        val idleBranch = row.substringAfter("} else if (uiState.isCloudSyncing) {").substringAfter("} else {")
        assertFalse(idleBranch.contains("LoadingIndicator("), "idle state is a static icon")
        assertTrue(idleBranch.contains("Icons.Rounded.CloudDone"))
    }

    @Test
    fun `RC3H_05 Home Ledgers top bar, active groups and recent activity use official M3 Expressive components`() {
        val app = src("ui/SplitMateAppComposable.kt")
        assertTrue(app.contains("TopAppBar("))
        assertTrue(app.contains("TopAppBarDefaults.pinnedScrollBehavior("))
        assertTrue(app.contains("SegmentedListItem("))
        assertTrue(app.contains("ListItemDefaults.segmentedShapes("))
        assertTrue(app.contains("ListItemDefaults.SegmentedGap"))
        assertTrue(app.contains("FloatingToolbarDefaults.ScreenOffset"))
    }

    @Test
    fun `RC3H_06 Collapsed floating toolbar centers the circular Quick Split button and remains clickable`() {
        val app = src("ui/SplitMateAppComposable.kt")
        val bar = app.substringAfter("fun SplitMateBottomNavigationBar(").substringBefore("\n// ====")
        assertTrue(bar.contains("shape = CircleShape"), "Quick Split FAB in floating toolbar must be circular")
        assertTrue(bar.contains("wrapContentWidth(align = Alignment.Start, unbounded = true)"), "Toolbar tabs must not squash during collapse")
        assertTrue(bar.contains("placeRelative(x = -shiftLeftToCenterPx, y = 0)"), "Collapsed single button must shift to horizontal center")

        var quickSplitClicks = 0
        compose.setContent {
            Box(modifier = Modifier.size(width = 400.dp, height = 120.dp)) {
                SplitMateBottomNavigationBar(
                    selectedTab = SplitMateTab.LEDGERS,
                    expanded = false,
                    onTabSelected = {},
                    onQuickSplitClick = { quickSplitClicks++ }
                )
            }
        }
        compose.waitForIdle()
        val fabNode = compose.onNode(hasContentDescription("Quick Split"))
        val bounds = fabNode.fetchSemanticsNode().boundsInRoot
        val rootWidthPx = with(compose.density) { 400.dp.toPx() }
        val expectedCenterPx = rootWidthPx / 2f
        assertTrue(
            kotlin.math.abs(bounds.center.x - expectedCenterPx) <= 2f,
            "Collapsed Quick Split button should be centered at $expectedCenterPx px, but was at ${bounds.center.x} px (bounds=$bounds)"
        )
        fabNode.performClick()
        assertEquals(1, quickSplitClicks)
    }
}
