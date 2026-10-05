package com.splitmate.app

import com.splitmate.app.ui.screens.TripHubCollapsingHeaderState
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * v2.3.6 B2: regression locks for user-reported layout bugs and Trip Hub UX changes.
 *  - Names must never be crushed into one-letter-per-line columns (weighted name, capped status).
 *  - Trip Hub chips/hero/banners collapse on scroll so bookings get the screen.
 *  - No progress line on settlement money data; "New Group" label has a single plus (icon).
 */
class SplitMateV236LayoutAndMotionTest {

    private fun src(rel: String): String {
        val candidates = listOf("src/main/java/com/splitmate/app/", "app/src/main/java/com/splitmate/app/")
        val f = candidates.map { File(it + rel) }.firstOrNull { it.isFile } ?: error("missing $rel")
        return f.readText()
    }

    private fun block(text: String, anchor: String, length: Int = 2600): String {
        val i = text.indexOf(anchor)
        assertTrue(i >= 0, "anchor not found: $anchor")
        return text.substring(i, minOf(text.length, i + length))
    }

    @Test
    fun `LM_01 PNR passenger name column is weighted and the status side is capped`() {
        val pnr = src("ui/screens/PnrExpenseReviewScreen.kt")
        val row = block(pnr, "passengers.forEachIndexed { index, pax ->", 6000)
        val nameIdx = row.indexOf("text = pax.name,")
        val weightIdx = row.lastIndexOf("Column(modifier = Modifier.weight(1f))", nameIdx)
        assertTrue(weightIdx in 0 until nameIdx, "name Column must take the remaining width")
        assertTrue(row.contains("modifier = Modifier.widthIn(max = 148.dp)"), "status side must be capped")
    }

    @Test
    fun `LM_02 payer and traveler names in booking cards ellipsize instead of squeezing`() {
        val home = src("ui/screens/TripHomeScreen.kt")
        var from = 0
        var checked = 0
        while (true) {
            val i = home.indexOf("text = \"Paid by \${payer?.name ?: \"Member\"}\",", from)
            if (i < 0) break
            val call = home.substring(home.lastIndexOf("Text(", i), home.indexOf("\n", i + 400).coerceAtLeast(i))
            if (call.contains("maxLines")) checked++
            from = i + 10
        }
        assertTrue(checked >= 4, "at least the 4 card payer rows must cap lines (found $checked)")
        val dialog = block(home, "\"\${cell.travelerName} (YOU)\" else cell.travelerName,", 700)
        assertTrue(dialog.contains("modifier = Modifier.weight(1f)"))
    }

    @Test
    fun `LM_03 Trip Hub header collapses with nested scroll and springs back`() {
        val home = src("ui/screens/TripHomeScreen.kt")
        assertTrue(home.contains(".nestedScroll(collapsingHeaderState.nestedScrollConnection)"))
        assertTrue(home.contains("TripHubCollapsingHeader(state = collapsingHeaderState) {"))
        val header = home.indexOf("TripHubCollapsingHeader(state = collapsingHeaderState) {")
        val strip = home.indexOf("CompactPerspectiveNetBalanceStrip(", header)
        val body = home.indexOf("SECTION BODY CONTENT", header)
        assertTrue(strip in header..body, "hero strip must live inside the collapsing header")
        assertTrue(home.contains("collapsingHeaderState.expand(headerResetSpec)"))
    }

    @Test
    fun `LM_04 collapsing header state consumes upward scroll first and clamps to its height`() {
        val state = TripHubCollapsingHeaderState()
        state.heightPx = 300f // normally set by layout
        assertEquals(-120f, state.consume(-120f))
        assertEquals(-120f, state.offsetPx)
        assertEquals(-180f, state.consume(-500f), "clamped at full height")
        assertEquals(-300f, state.offsetPx)
        assertEquals(300f, state.consume(1000f), "expands back fully, no further")
        assertEquals(0f, state.offsetPx)
    }

    @Test
    fun `LM_05 settlement card shows no progress line and New Group has a single plus`() {
        val home = src("ui/screens/TripHomeScreen.kt")
        assertFalse(home.contains("progress = { settlementProgress }"))
        val app = src("ui/SplitMateAppComposable.kt")
        assertFalse(app.contains("\"+ New Group\""))
        assertTrue(app.contains("leadingText = \"New Group\""))
    }

    @Test
    fun `LM_06 booking feeds use expressive press scale`() {
        val home = src("ui/screens/TripHomeScreen.kt")
        assertTrue(Regex("""\.expressivePressScale\(\)""").findAll(home).count() >= 2)
        val mod = src("ui/components/ExpressivePressScale.kt")
        assertTrue(mod.contains("MaterialTheme.motionScheme.fastSpatialSpec"))
        assertTrue(mod.contains("requireUnconsumed = false"), "must not steal clicks from inner targets")
    }
}
