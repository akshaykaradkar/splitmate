package com.splitmate.app.screenshots

import androidx.activity.BackEventCompat
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

    /** Step C: the "Reading your ticket…" loading overlay (real M3E ContainedLoadingIndicator). */
    @Test fun s15_pdf_parsing_overlay() {
        val vm = SplitMateViewModel()
        compose.setContent {
            val ui by vm.uiState.collectAsStateWithLifecycle()
            SplitMateMaterial3ExpressiveTheme(themeMode = ui.activeThemeMode, darkTheme = ui.isDarkTheme) {
                androidx.compose.foundation.layout.Box {
                    SplitMateApp(vm)
                    com.splitmate.app.ui.components.FlightPdfParsingOverlay(visible = true)
                }
            }
        }
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(900)
        frame("15_pdf_parsing_overlay")
        compose.mainClock.autoAdvance = true
    }

    /** Step C: mark both payments paid -> the "all settled" burst plays (mid + rest frames). */
    @Test fun s16_settled_celebration() {
        launch(); tap(text = "Lake Tahoe Cabin"); tap(text = "Money")
        tap(text = "Mark Paid")
        compose.mainClock.autoAdvance = false
        val last = compose.onAllNodes(hasText("Mark Paid", substring = true))[0]
        last.performTouchInput { click(androidx.compose.ui.geometry.Offset(width / 2f, minOf(height / 2f, 4f))) }
        compose.mainClock.advanceTimeBy(64); frame("16a_settled_burst_early")
        compose.mainClock.advanceTimeBy(96); frame("16a_settled_burst_mid")
        compose.mainClock.advanceTimeBy(2000); frame("16b_settled_burst_rest")
        compose.mainClock.autoAdvance = true
    }

    /** Step C: Ledgers card -> Trip Hub, group name + balance fly across (shared elements). */
    @Test fun s17_group_shared_elements() {
        launch()
        compose.mainClock.autoAdvance = false
        val node = compose.onAllNodes(hasText("Lake Tahoe Cabin", substring = true))[0]
        node.performTouchInput { click(androidx.compose.ui.geometry.Offset(width / 2f, minOf(height / 2f, 4f))) }
        compose.mainClock.advanceTimeBy(64); frame("17a_shared_early")
        compose.mainClock.advanceTimeBy(96); frame("17b_shared_mid")
        compose.mainClock.advanceTimeBy(2000); frame("17c_shared_done")
        compose.mainClock.autoAdvance = true
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

    // Full-app design audit coverage (every main surface).
    @Test fun s18_trip_plan_tab() { launch(); tap(text = "Lake Tahoe Cabin"); tap(text = "Plan"); shot("18_trip_plan_tab") }

    @Test fun s19_trip_travel_tab() { launch(); tap(text = "Lake Tahoe Cabin"); tap(text = "Travel"); shot("19_trip_travel_tab") }

    @Test fun s20_trip_people_tab() { launch(); tap(text = "Lake Tahoe Cabin"); tap(text = "People"); shot("20_trip_people_tab") }

    @Test fun s21_settle_up_sheet() { launch(); tap(text = "Lake Tahoe Cabin"); tap(text = "Money"); tap(text = "Settle Up"); shot("21_settle_up_sheet") }

    @Test fun s22_money_check_sheet() {
        launch(); tap(text = "Lake Tahoe Cabin"); tap(text = "Money")
        val row = compose.onAllNodes(hasContentDescription("Double tap for details", substring = true))[0]
        runCatching { row.performScrollTo() }
        row.performTouchInput { click(androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)) }
        shot("22_money_check_sheet")
    }

    @Test fun s23_shared_expense_form() {
        launch(); tap(text = "Lake Tahoe Cabin"); tap(desc = "Add Booking"); tap(text = "Shared Expense"); shot("23_shared_expense_form")
    }

    @Test fun s24_more_booking_options() {
        launch(); tap(text = "Lake Tahoe Cabin"); tap(desc = "Add Booking"); tap(text = "More Booking Options"); shot("24_more_booking_options")
    }

    @Test fun s25_money_tab_scrolled() {
        launch(); tap(text = "Lake Tahoe Cabin"); tap(text = "Money")
        compose.onRoot().performTouchInput { swipeUp(startY = height * 0.85f, endY = height * 0.2f) }
        shot("25_money_tab_scrolled")
    }

    @Test fun s26_espresso_trip_money() {
        launch(SplitMateThemeMode.WARM_ESPRESSO_NIGHT); tap(text = "Lake Tahoe Cabin"); tap(text = "Money"); shot("26_espresso_trip_money")
    }

    @Test fun s27_kyoto_trip_overview() {
        launch(SplitMateThemeMode.KYOTO_MATCHA_YUZU); tap(text = "Lake Tahoe Cabin"); shot("27_kyoto_trip_overview")
    }

    @Test fun s10_trip_scrolled() {
        launch(); tap(text = "Lake Tahoe Cabin")
        compose.onRoot().performTouchInput { swipeUp(startY = height * 0.85f, endY = height * 0.25f) }
        shot("10_trip_scrolled")
    }

    /** Adds a real flight booking to the demo trip (the seed has no travel tickets). */
    private fun seedFlightBooking(vm: SplitMateViewModel) {
        vm.selectActiveGroup("g_tahoe")
        val memberIds = vm.uiState.value.members.filter { it.groupId == "g_tahoe" }.map { it.memberId }
        vm.logExpense(
            title = "Flight IndiGo 6E 2134 BOM-GOI (PNR QX7K2M)",
            totalAmountCents = 1_248_000L,
            selectedMemberIds = memberIds,
            expenseCategory = "FLIGHT",
            travelPnr = "QX7K2M",
            providerName = "IndiGo"
        )
        settle()
    }

    private fun frame(name: String) {
        compose.onRoot().captureRoboImage("$outDir/$name.png")
    }

    /** Step C1: booking card -> ticket container transform, then a predictive-back preview. */
    @Test fun s11_ticket_container_transform() {
        val vm = launch(); tap(text = "Lake Tahoe Cabin"); seedFlightBooking(vm)
        shot("11a_trip_with_flight")
        compose.mainClock.autoAdvance = false
        val card = compose.onAllNodes(hasText("6E 2134", substring = true), useUnmergedTree = true)[0]
        card.performTouchInput { click(androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)) }
        compose.mainClock.advanceTimeBy(96); frame("11b_transform_early")
        compose.mainClock.advanceTimeBy(96); frame("11c_transform_mid")
        compose.mainClock.advanceTimeBy(2000); frame("11d_ticket_open")
        val dispatcher = compose.activity.onBackPressedDispatcher
        compose.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(8f, 900f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(160f, 900f, 0.55f, BackEventCompat.EDGE_LEFT))
        }
        compose.mainClock.advanceTimeBy(400); frame("11e_back_gesture_preview")
        compose.runOnUiThread { dispatcher.onBackPressed() }
        compose.mainClock.advanceTimeBy(2500); frame("11f_back_to_trip")
        compose.mainClock.autoAdvance = true
    }

    /** Step C: swipe a booking left -> Delete action revealed; release -> springs back + confirmation. */
    @Test fun s12_swipe_to_manage() {
        val vm = launch(); tap(text = "Lake Tahoe Cabin"); seedFlightBooking(vm)
        val card = compose.onAllNodes(hasText("6E 2134", substring = true), useUnmergedTree = false)[0]
        runCatching { card.performScrollTo() }
        settle()
        compose.mainClock.autoAdvance = false
        card.performTouchInput {
            val y = height * 0.6f
            down(androidx.compose.ui.geometry.Offset(width * 0.9f, y))
            repeat(12) { moveBy(androidx.compose.ui.geometry.Offset(-width * 0.04f, 0f)) }
        }
        compose.mainClock.advanceTimeBy(300); frame("12a_swipe_delete_armed")
        card.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(2000); frame("12b_swipe_released_confirm")
        compose.mainClock.autoAdvance = true
        settle()
        compose.onNode(hasText("Delete this expense?")).assertExists()
        compose.onNode(androidx.compose.ui.test.isDialog()).captureRoboImage("$outDir/12c_delete_dialog.png")
    }

    /** Step C: swipe a Money-tab settle row right -> "Mark paid" revealed; release -> confirmation. */
    @Test fun s14_mark_paid_swipe() {
        launch(); tap(text = "Lake Tahoe Cabin"); tap(text = "Money")
        val row = compose.onAllNodes(hasText("YOU RECEIVE", substring = true), useUnmergedTree = true)[0]
        runCatching { row.performScrollTo() }
        settle()
        compose.mainClock.autoAdvance = false
        row.performTouchInput {
            down(androidx.compose.ui.geometry.Offset(4f, height / 2f))
            repeat(14) { moveBy(androidx.compose.ui.geometry.Offset(30f, 0f)) }
        }
        compose.mainClock.advanceTimeBy(300); frame("14a_mark_paid_armed")
        row.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(1500)
        compose.mainClock.autoAdvance = true
        settle()
        compose.onNode(hasText("as paid?", substring = true)).assertExists()
        compose.onNode(androidx.compose.ui.test.isDialog()).captureRoboImage("$outDir/14b_mark_paid_dialog.png")
    }

    /** Step C: first-open entrance stagger, captured mid-way. */
    @Test fun s13_first_open_entrance() {
        com.splitmate.app.ui.components.ExpressiveEntranceRegistry.resetForTest()
        launch()
        compose.mainClock.autoAdvance = false
        val node = compose.onAllNodes(hasText("Lake Tahoe Cabin", substring = true))[0]
        node.performTouchInput { click(androidx.compose.ui.geometry.Offset(width / 2f, minOf(height / 2f, 4f))) }
        compose.mainClock.advanceTimeBy(120); frame("13a_entrance_mid")
        compose.mainClock.advanceTimeBy(2000); frame("13b_entrance_done")
        compose.mainClock.autoAdvance = true
    }

    @Test fun s03_settle_tab() { launch(); tap(desc = "Settle"); shot("03_settle") }

    @Test fun s04_audit_tab() { launch(); tap(desc = "Audit"); shot("04_audit") }

    @Test fun s05_quick_split() { launch(); tap(desc = "Quick Split"); shot("05_quick_split") }

    @Test fun s06_espresso_home() { launch(SplitMateThemeMode.WARM_ESPRESSO_NIGHT); shot("06_ledgers_espresso") }

    @Test fun s07_kyoto_home() { launch(SplitMateThemeMode.KYOTO_MATCHA_YUZU); shot("07_ledgers_kyoto") }
}
