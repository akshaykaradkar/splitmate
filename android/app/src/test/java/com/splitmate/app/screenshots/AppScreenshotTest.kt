package com.splitmate.app.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.takahirom.roborazzi.captureRoboImage
import com.splitmate.app.SplitMateApp
import com.splitmate.app.ui.SplitMateMaterial3ExpressiveTheme
import com.splitmate.app.ui.SplitMateThemeMode
import com.splitmate.app.ui.SplitMateViewModel
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * v2.3.6: renders the REAL app UI (seeded in-memory ViewModel, no Room / no account) to PNGs on
 * the JVM so design changes can be reviewed before/after without a device.
 * Opt-in only: `-Psplitmate.screenshots=true` (skipped in the normal unit-test gate).
 * Network images (DiceBear avatars) cannot load on the JVM, so avatars show their placeholder.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class AppScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir: String = System.getProperty("splitmate.screenshots.dir") ?: "build/screenshots"

    @Before
    fun optIn() {
        assumeTrue(System.getProperty("splitmate.screenshots") == "true")
    }

    private fun launch(mode: SplitMateThemeMode? = null): SplitMateViewModel {
        val vm = SplitMateViewModel()
        if (mode != null) vm.setExpressiveThemeMode(mode, compose.activity)
        compose.setContent {
            val ui by vm.uiState.collectAsStateWithLifecycle()
            SplitMateMaterial3ExpressiveTheme(themeMode = ui.activeThemeMode, darkTheme = ui.isDarkTheme) {
                SplitMateApp(vm)
            }
        }
        settle()
        return vm
    }

    private fun settle() {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1500)
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        settle()
        compose.onRoot().captureRoboImage("$outDir/$name.png")
    }

    private fun tap(text: String? = null, desc: String? = null) {
        val matcher = if (text != null) hasText(text, substring = true) else hasContentDescription(desc!!)
        val node = compose.onAllNodes(matcher, useUnmergedTree = false)[0]
        runCatching { node.performScrollTo() }
        // Click near the top edge: the floating bottom toolbar can cover the node's centre.
        node.performTouchInput { click(androidx.compose.ui.geometry.Offset(width / 2f, minOf(height / 2f, 4f))) }
        settle()
    }

    @Test fun s01_ledgers_home() { launch(); shot("01_ledgers_home") }

    @Test fun s02_group_detail() { launch(); tap(text = "Lake Tahoe Cabin"); shot("02_group_detail") }

    @Test fun s08_trip_fab_menu() { launch(); tap(text = "Lake Tahoe Cabin"); tap(desc = "Add Booking"); shot("08_trip_fab_menu") }

    @Test fun s09_trip_money_tab() { launch(); tap(text = "Lake Tahoe Cabin"); tap(text = "Money"); shot("09_trip_money_tab") }

    @Test fun s10_trip_scrolled() {
        launch(); tap(text = "Lake Tahoe Cabin")
        compose.onRoot().performTouchInput { swipeUp(startY = height * 0.85f, endY = height * 0.25f) }
        shot("10_trip_scrolled")
    }

    @Test fun s03_settle_tab() { launch(); tap(desc = "Settle"); shot("03_settle") }

    @Test fun s04_audit_tab() { launch(); tap(desc = "Audit"); shot("04_audit") }

    @Test fun s05_quick_split() { launch(); tap(desc = "Quick Split"); shot("05_quick_split") }

    @Test fun s06_espresso_home() { launch(SplitMateThemeMode.WARM_ESPRESSO_NIGHT); shot("06_ledgers_espresso") }

    @Test fun s07_kyoto_home() { launch(SplitMateThemeMode.KYOTO_MATCHA_YUZU); shot("07_ledgers_kyoto") }
}
