package com.splitmate.app.screenshots

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
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
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.abs
import kotlin.math.pow

/**
 * v2.4.0 rc9: dark-mode readability sweep. Renders the REAL app screens in light (Sunlit
 * Buckwheat) and dark (Warm Espresso Night), saves `<theme>/<screen>.png` for Light | Dark boards,
 * and measures every on-screen text against the pixels actually drawn behind it.
 *
 * Opt-in like [AppScreenshotTest]: `-Psplitmate.screenshots=true`. Each screen appends its
 * findings to `<dir>/<theme>/contrast_report.tsv` (screen, text, ratio, fg, bg, bounds).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class DarkModeSweepTest(private val themeName: String) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes(): List<Array<Any>> = listOf(arrayOf("light"), arrayOf("dark"))
    }

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val baseDir: String = System.getProperty("splitmate.screenshots.dir") ?: "build/screenshots"
    private val outDir: String get() = "$baseDir/$themeName"
    private val mode: SplitMateThemeMode
        get() = if (themeName == "dark") SplitMateThemeMode.WARM_ESPRESSO_NIGHT else SplitMateThemeMode.SUNLIT_BUCKWHEAT

    @Before
    fun optIn() {
        assumeTrue(System.getProperty("splitmate.screenshots") == "true")
        File(outDir).mkdirs()
    }

    // ---------------------------------------------------------------- harness

    private fun launch(): SplitMateViewModel {
        val vm = SplitMateViewModel()
        vm.setExpressiveThemeMode(mode, compose.activity)
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

    private fun tap(text: String? = null, desc: String? = null, exact: Boolean = false) {
        val matcher = if (text != null) hasText(text, substring = !exact) else hasContentDescription(desc!!)
        val node = compose.onAllNodes(matcher, useUnmergedTree = false)[0]
        runCatching { node.performScrollTo() }
        node.performTouchInput { click(androidx.compose.ui.geometry.Offset(width / 2f, minOf(height / 2f, 4f))) }
        settle()
    }

    private fun scrollDown(fraction: Float = 0.6f) {
        compose.onRoot().performTouchInput { swipeUp(startY = height * 0.85f, endY = height * (0.85f - fraction)) }
        settle()
    }

    /** Same seed as AppScreenshotTest: a real flight booking on the demo trip. */
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

    /** Screenshot + readability audit of every text node currently on screen. */
    private fun shot(name: String) {
        settle()
        compose.onRoot().captureRoboImage("$outDir/$name.png")
        val severe = audit(name).filter { f ->
            f.ratio < 3.0 && f.text.length > 1 && KNOWN_OCCLUDED.none { (screen, text) -> name == screen && f.text.startsWith(text) }
        }
        org.junit.Assert.assertTrue(
            "Unreadable text on $name ($themeName): " + severe.joinToString { "\"${it.text}\" %.2f:1".format(it.ratio) },
            severe.isEmpty()
        )
    }

    private data class Finding(val text: String, val ratio: Double)

    /**
     * Texts that exist in the semantics tree but are covered by something else on screen, so the
     * pixels under their bounds belong to the covering layer (not a real contrast problem):
     * the Trip Hub behind the open e-ticket, and the flight card peeking behind the train card.
     */
    private val KNOWN_OCCLUDED = listOf(
        "f04_flight_eticket_open" to "You get back", "f04_flight_eticket_open" to "People",
        "f04_flight_eticket_open" to "Edit", "f04_flight_eticket_open" to "Delete",
        "f04_flight_eticket_open" to "Overview", "f04_flight_eticket_open" to "Assigned at Check-in",
        "f04_flight_eticket_open" to "₹", "f05_flight_eticket_scrolled" to "₹",
        "f05_flight_eticket_scrolled" to "Overview", "f05_flight_eticket_scrolled" to "People",
        "f05_flight_eticket_scrolled" to "1", "f04_flight_eticket_open" to "1",
        "s01_home" to "Upload the airline PDF", "s02_home_scrolled" to "Upload the airline PDF"
    )

    // ---------------------------------------------------------------- readability audit

    private fun luminance(argb: Int): Double {
        fun ch(c: Int): Double {
            val s = c / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * ch((argb shr 16) and 0xFF) + 0.7152 * ch((argb shr 8) and 0xFF) + 0.0722 * ch(argb and 0xFF)
    }

    private fun ratio(a: Double, b: Double) = (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)

    private fun hex(argb: Int) = String.format("#%06X", argb and 0xFFFFFF)

    private fun audit(screen: String): List<Finding> {
        val findings = ArrayList<Finding>()
        // Read back the PNG Roborazzi just wrote (a second window capture times out under Robolectric).
        val bitmap: Bitmap = android.graphics.BitmapFactory.decodeFile("$outDir/$screen.png") ?: return findings
        val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
        val report = File(outDir, "contrast_report.tsv")
        if (!report.exists()) report.writeText("screen\ttext\tratio\tfg\tbg\tbounds\n")
        val seen = HashSet<String>()
        for (node in nodes) {
            val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }?.trim().orEmpty()
            if (text.isBlank()) continue
            val r = node.boundsInRoot
            val left = r.left.toInt().coerceIn(0, bitmap.width - 1)
            val top = r.top.toInt().coerceIn(0, bitmap.height - 1)
            val right = r.right.toInt().coerceIn(0, bitmap.width)
            val bottom = r.bottom.toInt().coerceIn(0, bitmap.height)
            if (right - left < 6 || bottom - top < 6) continue
            val key = "$text@$left,$top"
            if (!seen.add(key)) continue

            // Background = most common colour in the text box; text ink = the colour furthest in
            // luminance from it, ignoring the most extreme 2% (anti-aliasing / stray pixels).
            val counts = HashMap<Int, Int>()
            val pixels = ArrayList<Int>((right - left) * (bottom - top))
            for (y in top until bottom) for (x in left until right) {
                val p = bitmap.getPixel(x, y) or (0xFF shl 24)
                pixels.add(p)
                val q = p and 0xF8F8F8
                counts[q] = (counts[q] ?: 0) + 1
            }
            val bgKey = counts.maxByOrNull { it.value }?.key ?: continue
            val bg = pixels.first { (it and 0xF8F8F8) == bgKey }
            val bgL = luminance(bg)
            val sorted = pixels.sortedByDescending { abs(luminance(it) - bgL) }
            val distinct = sorted.count { abs(luminance(it) - bgL) > 0.004 }
            if (distinct < 8) continue // no visible ink (e.g. clipped or off-screen text)
            val fg = sorted[(distinct * 0.02).toInt().coerceAtMost(distinct - 1)]
            val cr = ratio(luminance(fg), bgL)
            if (cr < 4.5) {
                findings.add(Finding(text, cr))
                report.appendText(
                    "$screen\t${text.replace('\t', ' ').take(60)}\t${"%.2f".format(cr)}\t${hex(fg)}\t${hex(bg)}\t$left,$top,$right,$bottom\n"
                )
            }
        }
        return findings
    }

    // ---------------------------------------------------------------- screens (flight tickets first)

    /** A real train booking on the demo trip (deep-green train pass card). */
    private fun seedTrainBooking(vm: SplitMateViewModel) {
        vm.selectActiveGroup("g_tahoe")
        val memberIds = vm.uiState.value.members.filter { it.groupId == "g_tahoe" }.map { it.memberId }
        vm.logExpense(
            title = "Train 12051 Jan Shatabdi CSMT-MAO (PNR 4521367890)",
            totalAmountCents = 486_000L,
            selectedMemberIds = memberIds,
            expenseCategory = "TRAIN",
            travelPnr = "4521367890",
            providerName = "IRCTC"
        )
        settle()
    }

    @Test fun f06_trip_overview_train() {
        val vm = launch(); tap(text = "Lake Tahoe Cabin"); seedTrainBooking(vm)
        runCatching { compose.onAllNodes(hasText("12051", substring = true))[0].performScrollTo() }
        shot("f06_trip_overview_train")
    }

    /** A realistic busy trip: tickets + stay + cab + rental + food + shopping, as people log them. */
    private fun seedBusyTrip(vm: SplitMateViewModel) {
        seedFlightBooking(vm)
        seedTrainBooking(vm)
        val memberIds = vm.uiState.value.members.filter { it.groupId == "g_tahoe" }.map { it.memberId }
        listOf(
            Triple("Zostel Goa Anjuna (3 nights)", 1_860_000L, "STAY"),
            Triple("Uber BOM airport to Andheri", 64_000L, "CAB"),
            Triple("Scooty rental Anjuna 3 days", 210_000L, "RENTAL"),
            Triple("Dinner at Thalassa", 486_000L, "FOOD"),
            Triple("Breakfast Artjuna", 132_000L, "FOOD"),
            Triple("Flea market shopping", 95_000L, "SHOPPING"),
            Triple("Parasailing Calangute", 360_000L, "ACTIVITIES")
        ).forEach { (title, cents, cat) ->
            vm.logExpense(title = title, totalAmountCents = cents, selectedMemberIds = memberIds, expenseCategory = cat)
        }
        settle()
    }

    /** Overview of a busy trip, captured top to bottom. */
    @Test fun t01_overview_busy_trip() {
        val vm = launch(); tap(text = "Lake Tahoe Cabin"); seedBusyTrip(vm)
        tap(text = "Overview", exact = true)
        for (i in 1..6) {
            shotNoAudit("t01_overview_busy_$i")
            scrollDown(0.55f)
        }
    }

    /** Money -> All expenses of the same busy trip. */
    @Test fun t02_money_all_expenses_busy_trip() {
        val vm = launch(); tap(text = "Lake Tahoe Cabin"); seedBusyTrip(vm)
        tap(text = "Money")
        runCatching { compose.onAllNodes(hasText("All expenses", substring = true))[0].performScrollTo() }
        for (i in 1..8) {
            shotNoAudit("t02_money_all_expenses_busy_$i")
            scrollDown(0.55f)
        }
    }

    private fun shotNoAudit(name: String) {
        settle()
        compose.onRoot().captureRoboImage("$outDir/$name.png")
    }

    @Test fun f01_trip_overview_flight() {
        val vm = launch(); tap(text = "Lake Tahoe Cabin"); seedFlightBooking(vm)
        val card = compose.onAllNodes(hasText("6E 2134", substring = true), useUnmergedTree = false)[0]
        runCatching { card.performScrollTo() }
        shot("f01_trip_overview_flight")
    }

    @Test fun f02_trip_plan_flight() {
        val vm = launch(); tap(text = "Lake Tahoe Cabin"); seedFlightBooking(vm)
        tap(text = "Plan", exact = true)
        runCatching { compose.onAllNodes(hasText("6E 2134", substring = true))[0].performScrollTo() }
        shot("f02_trip_plan_flight")
    }

    @Test fun f03_trip_money_flight() {
        val vm = launch(); tap(text = "Lake Tahoe Cabin"); seedFlightBooking(vm)
        tap(text = "Money")
        runCatching { compose.onAllNodes(hasText("6E 2134", substring = true))[0].performScrollTo() }
        shot("f03_trip_money_flight")
    }

    @Test fun f04_flight_eticket_open() {
        val vm = launch(); tap(text = "Lake Tahoe Cabin"); seedFlightBooking(vm)
        val card = compose.onAllNodes(hasText("6E 2134", substring = true), useUnmergedTree = true)[0]
        runCatching { card.performScrollTo() }
        card.performTouchInput { click(androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)) }
        compose.mainClock.advanceTimeBy(2500)
        shot("f04_flight_eticket_open")
        scrollDown()
        shot("f05_flight_eticket_scrolled")
    }

    @Test fun s01_home() { launch(); shot("s01_home") }
    @Test fun s02_home_scrolled() { launch(); scrollDown(); shot("s02_home_scrolled") }
    @Test fun s03_trip_overview() { launch(); tap(text = "Lake Tahoe Cabin"); shot("s03_trip_overview") }
    @Test fun s04_trip_plan() { launch(); tap(text = "Lake Tahoe Cabin"); tap(text = "Plan", exact = true); shot("s04_trip_plan") }
    @Test fun s05_trip_money() { launch(); tap(text = "Lake Tahoe Cabin"); tap(text = "Money"); shot("s05_trip_money") }
    @Test fun s06_trip_money_scrolled() { launch(); tap(text = "Lake Tahoe Cabin"); tap(text = "Money"); scrollDown(0.65f); shot("s06_trip_money_scrolled") }
    @Test fun s07_trip_people() { launch(); tap(text = "Lake Tahoe Cabin"); tap(text = "People"); shot("s07_trip_people") }
    @Test fun s08_add_booking_sheet() { launch(); tap(text = "Lake Tahoe Cabin"); tap(desc = "Add booking"); shot("s08_add_booking_sheet") }
    @Test fun s09_settle_up_sheet() { launch(); tap(text = "Lake Tahoe Cabin"); tap(text = "Money"); tap(text = "Settle up"); shot("s09_settle_up_sheet") }
    @Test fun s10_settle_tab() { launch(); tap(desc = "Settle"); shot("s10_settle_tab") }
    @Test fun s11_settle_tab_scrolled() { launch(); tap(desc = "Settle"); scrollDown(0.65f); shot("s11_settle_tab_scrolled") }
    @Test fun s12_audit_tab() { launch(); tap(desc = "Audit"); shot("s12_audit_tab") }
    @Test fun s13_quick_split() { launch(); tap(desc = "Quick Split"); shot("s13_quick_split") }
    @Test fun s14_shared_expense_form() { launch(); tap(text = "Lake Tahoe Cabin"); tap(desc = "Add booking"); tap(text = "Split a bill"); shot("s14_shared_expense_form") }
    @Test fun s15_settings() { launch(); tap(desc = "Open settings"); shot("s15_settings") }
    @Test fun s16_settings_scrolled() { launch(); tap(desc = "Open settings"); scrollDown(0.7f); shot("s16_settings_scrolled") }
}
