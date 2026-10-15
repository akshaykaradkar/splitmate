package com.splitmate.app

import com.splitmate.app.ui.toPalette
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.data.guide.TripDays
import com.splitmate.app.data.guide.loop.LoopStopSelector
import com.splitmate.app.data.guide.sync.PlanManifestMerger
import com.splitmate.app.data.guide.sync.TripGuideStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * v2.4.0 rc14: proper per-day loops. Trip dates (set, else first..last booking) define the days;
 * places are put on days through the synced PlanManifest.days; unplanned places wait in a tray;
 * existing trips (no days yet) keep the single whole-trip loop.
 */
class SplitMateV240Rc14PerDayLoopsTest {

    private fun src(path: String) = File("src/main/java/com/splitmate/app/$path").readText()
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun ms(date: String, hour: Int = 10) = LocalDate.parse(date).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    private fun day(date: String) = LocalDate.parse(date).toEpochDay()

    @Test
    fun `RC14_01 trip days come from trip dates, else from the first and last booking`() {
        val explicit = TripDays.resolve(ms("2026-10-15"), ms("2026-10-18", 23), listOf(ms("2026-10-20")), zone)!!
        assertTrue(explicit.explicit)
        assertEquals(4, explicit.days.size, "set dates win over bookings")
        assertEquals(2, explicit.numberOf(day("2026-10-16")))
        assertNull(explicit.numberOf(day("2026-10-20")), "a booking outside the dates has no day number")

        val auto = TripDays.resolve(null, null, listOf(ms("2026-10-17"), ms("2026-10-15"), ms("2026-10-15", 20)), zone)!!
        assertFalse(auto.explicit)
        assertEquals(day("2026-10-15"), auto.startEpochDay)
        assertEquals(3, auto.days.size, "days in between with nothing booked are still days")

        assertNull(TripDays.resolve(null, null, emptyList(), zone), "no dates, no bookings: no days yet")
        val long = TripDays.resolve(ms("2026-10-01"), ms("2026-12-31"), emptyList(), zone)!!
        assertEquals(TripDays.MAX_DAYS, long.days.size, "capped at the 30 days the plan sync can store")
        assertEquals("15–18 Oct", TripDays.rangeLabel(explicit))
        assertEquals("15 Oct 2026", TripDays.fullLabel(day("2026-10-15")))
        assertEquals("Fri 16", TripDays.shortLabel(day("2026-10-16")))
    }

    private fun place(id: String, lat: Double) =
        Place(id = id, kind = PlaceKind.SEE, name = id, location = LatLng(lat, 76.4), source = PlaceSource.WIKIDATA)

    private val places = listOf(place("wd:Q1", 15.31), place("wd:Q2", 15.32), place("wd:Q3", 15.33), place("wd:Q4", 15.34))

    @Test
    fun `RC14_02 existing trips keep one whole-trip loop until a place gets a day`() {
        val pinned = places.associate { it.id to 1L }
        val sel = LoopStopSelector.select(places, pinned, emptyMap(), day = 2)
        assertFalse(sel.perDay)
        assertEquals(4, sel.stops.size, "no days yet: every day shows every pinned place")
    }

    @Test
    fun `RC14_03 each day routes only its own places, unplanned ones wait in the tray`() {
        val pinned = places.associate { it.id to 1L }
        val days = mapOf("wd:Q1" to 1, "wd:Q2" to 2, "wd:Q3" to 2)
        val day2 = LoopStopSelector.select(places, pinned, emptyMap(), days = days, day = 2)
        assertTrue(day2.perDay)
        assertEquals(setOf("wd:Q2", "wd:Q3"), day2.stops.map { it.id }.toSet())
        assertEquals(listOf("wd:Q4"), day2.unplanned.map { it.id }, "no day yet -> tray, not the route")
        val day3 = LoopStopSelector.select(places, pinned, emptyMap(), days = days, day = 3)
        assertTrue(day3.stops.isEmpty() && !day3.autoSuggested, "an empty day stays empty (no surprise suggestions)")
        val whole = LoopStopSelector.select(places, pinned, emptyMap(), days = days, day = null)
        assertEquals(4, whole.stops.size, "All days = the whole trip")
        val none = LoopStopSelector.select(places, emptyMap(), emptyMap(), days = emptyMap(), day = 1)
        assertTrue(none.autoSuggested, "nothing added yet: suggestions as before")
    }

    @Test
    fun `RC14_04 moving a place keeps it pinned and syncs the day by pin time`() {
        val m0 = TripGuideStore.emptyManifest()
        val onDay2 = PlanManifestMerger.pin(m0, "wd:Q1", "me", 100L, day = 2)
        assertEquals(2, onDay2.days["wd:Q1"])
        val moved = PlanManifestMerger.pin(onDay2, "wd:Q1", "me", 200L, day = 3)
        assertEquals(3, moved.days["wd:Q1"])
        assertTrue(LoopStopSelector.isPinned("wd:Q1", moved.pinned, moved.hidden))
        val unplanned = PlanManifestMerger.pin(moved, "wd:Q1", "me", 300L, day = null)
        assertNull(unplanned.days["wd:Q1"], "back to Not planned yet, still in the loop")
        // A peer's newer move wins (LWW by pin time).
        val peer = PlanManifestMerger.pin(onDay2, "wd:Q1", "peer", 250L, day = 1)
        assertEquals(1, PlanManifestMerger.merge(moved, peer).days["wd:Q1"])
        val repo = src("data/guide/TripGuideRepository.kt")
        assertTrue(repo.contains("editManifest(groupId) { m -> PlanManifestMerger.pin(m, placeId, memberKey, now, day).withHeaderOf(m) }"))
    }

    @Test
    fun `RC14_05 Days lists every trip day and each Make a loop opens that day`() {
        val trip = src("ui/screens/TripHomeScreen.kt")
        val timeline = trip.substringAfter("private fun TripHubPlanTimelineView(").substringBefore("\n}\n")
        assertTrue(timeline.contains("(groupedByDay.keys + tripDays?.days?.map { it.epochDay }.orEmpty()).distinct().sorted()"))
        assertTrue(timeline.contains("\"Nothing booked yet\""))
        assertTrue(timeline.contains("onClick = { onMakeLoop(dayNumber) }"))
        assertTrue(timeline.contains("TripDatesChip("))
        assertTrue(trip.contains("TripDays.resolve("), "trip days resolved from trip dates / bookings")
        assertTrue(trip.contains("viewModel.setTripDates(resolvedGroupId, start, end)"))
        val tab = src("ui/screens/plan/TripPlanTab.kt")
        assertTrue(tab.contains("actions.selectLoopDay(day)"))
        assertTrue(tab.contains("if (day != null) onFreezeTripDates()"), "first day assignment freezes the dates")
        val vm = src("ui/SplitMateViewModel.kt")
        assertTrue(vm.contains("fun setTripDates(groupId: String, startEpochMs: Long, endEpochMs: Long)"))
    }

    @Test
    fun `RC14_06 Loop has a day picker, Move to day and the Not planned yet tray`() {
        val loop = src("ui/screens/plan/DayLoopView.kt")
        assertTrue(loop.contains("LoopDayPicker("))
        assertTrue(loop.contains("MoveToDayButton("))
        assertTrue(loop.contains("UnplannedTray("))
        assertTrue(loop.contains("targetState = loopTitle("))
        assertFalse(loop.contains("Today's loop"), "no false 'Today' promise")
        val ui = src("ui/screens/plan/TripDaysUi.kt")
        assertTrue(ui.contains("DateRangePicker(") && ui.contains("\"Not planned yet\""))
        assertFalse(Regex("""RoundedCornerShape\(|Color\(0x|fontSize\s*=""").containsMatchIn(ui), "theme tokens only")
        val vm = src("ui/screens/plan/TripGuideViewModel.kt")
        assertTrue(vm.contains("val key = listOf(pack, manifest.pinned, manifest.hidden, manifest.days, stay, day)"), "loop cache keyed by day")
        assertTrue(vm.contains("movePlaceToDay(placeId, targetDay)"), "Explore opened for a day adds onto that day")
        assertTrue(src("ui/screens/plan/PlanGuideComponents.kt").contains("targetDay != null -> \"Add to Day \$targetDay\""))
    }

    @Test
    fun `RC14_07 per-day UI uses the Material 3 Expressive components`() {
        val ui = src("ui/screens/plan/TripDaysUi.kt")
        val picker = ui.substringAfter("fun LoopDayPicker(").substringBefore("\n}\n")
        assertTrue(picker.contains("ToggleButton(") && picker.contains("shapes = ToggleButtonDefaults.shapes()"), "day picker: shape-morphing toggle buttons")
        assertTrue(picker.contains("selectableGroup()") && picker.contains("Role.RadioButton"), "single-select semantics for TalkBack")
        assertTrue(picker.contains("HapticFeedbackType.SegmentTick"))
        val move = ui.substringAfter("fun MoveToDayButton(").substringBefore("\n}\n")
        assertTrue(move.contains("shapes = IconButtonDefaults.shapes()"), "shape-morphing icon button")
        assertTrue(move.contains("DropdownMenuPopup(") && move.contains("DropdownMenuGroup(shapes = MenuDefaults.groupShape("), "expressive grouped menu")
        assertTrue(move.contains("selected = d.number == currentDay") && move.contains("shapes = MenuDefaults.itemShape("))
        val tray = ui.substringAfter("fun UnplannedTray(")
        assertTrue(tray.contains("SegmentedListItem(") && tray.contains("ListItemDefaults.segmentedShapes(index = index, count = places.size)"), "segmented list")
        val dates = ui.substringAfter("fun TripDatesPickerDialog(").substringBefore("\n}\n")
        assertTrue(dates.contains("DialogProperties(usePlatformDefaultWidth = false)") && dates.contains("DateRangePicker("), "full-screen range picker")
        assertFalse(dates.contains("DatePickerDialog("), "range selection is full-screen on phones, not a cramped dialog")
    }

    @Test
    fun `RC14_08 per-day motion uses the expressive motion scheme and respects reduced motion`() {
        val ui = src("ui/screens/plan/TripDaysUi.kt")
        assertTrue(ui.contains("if (reducedMotion) listState.scrollToItem(index) else listState.animateScrollToItem(index)"))
        assertTrue(ui.contains("Modifier.animateContentSize(motion.defaultSpatialSpec())"), "tray resizes on the spatial spring")
        assertFalse(Regex("""\btween\(|spring\(""").containsMatchIn(ui), "no hand-tuned curves: motion comes from the scheme")
        val loop = src("ui/screens/plan/DayLoopView.kt")
        assertTrue(loop.contains("fadeIn(motion.defaultEffectsSpec()) togetherWith"), "title cross-fades on the effects spring")
        assertTrue(loop.contains("fadeInSpec = motion.fastEffectsSpec(), placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = motion.fastEffectsSpec()"),
            "stops fade on effects and move on spatial springs when the day changes")
        val theme = src("ui/SplitMateTheme.kt")
        assertTrue(theme.contains("androidx.compose.material3.MotionScheme.expressive()"), "toggle/menu/icon-button morphs run on the expressive scheme")
        assertTrue(theme.contains("SplitMateReducedMaterialMotionScheme"), "and snap under Remove animations")
    }

    @Test
    fun `RC14_09 muted text meets WCAG AA 4_5 to 1 on every card and canvas surface`() {
        fun lum(c: androidx.compose.ui.graphics.Color) = c.luminance().toDouble()
        fun cr(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color): Double {
            val la = lum(a); val lb = lum(b)
            return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
        }
        val alpha = com.splitmate.app.ui.screens.TripHubTokens.TEXT_MUTED_ALPHA
        com.splitmate.app.ui.SplitMateThemeMode.entries.forEach { mode ->
            val p = mode.toPalette()
            listOf(p.surfaceContainerLowest, p.surfaceContainerLow, p.surfaceContainer).forEach { bg ->
                val fg = p.onSurfaceVariant.copy(alpha = alpha).compositeOver(bg)
                assertTrue(cr(fg, bg) >= 4.5, "$mode muted text ${"%.2f".format(cr(fg, bg))}:1")
            }
        }
    }

    @Test
    fun `RC14_10 direct motion tokens also snap under Remove animations`() {
        val motion = com.splitmate.app.ui.components.SplitMateMotion
        try {
            motion.reducedMotion = false
            assertEquals(com.splitmate.app.ui.components.SplitMateMotion.DEFAULT_SPATIAL_STIFFNESS, motion.defaultSpatial<Float>().stiffness, 1f)
            motion.reducedMotion = true
            listOf(motion.fastSpatial<Float>(), motion.defaultSpatial(), motion.slowSpatial(), motion.fastEffects(), motion.defaultEffects(), motion.slowEffects())
                .forEach {
                    assertEquals(motion.INSTANT_STIFFNESS, it.stiffness)
                    assertEquals(1f, it.dampingRatio)
                }
        } finally {
            motion.reducedMotion = false
        }
        assertTrue(src("ui/SplitMateTheme.kt").contains("com.splitmate.app.ui.components.SplitMateMotion.reducedMotion = reducedMotion"))
    }
}
