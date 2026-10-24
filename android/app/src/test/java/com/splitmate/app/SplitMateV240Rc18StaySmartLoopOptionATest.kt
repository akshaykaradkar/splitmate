package com.splitmate.app

import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.data.guide.loop.DeterministicLoopOptimizer
import com.splitmate.app.data.guide.loop.LoopStopSelector
import com.splitmate.app.data.guide.nav.TravelMode
import com.splitmate.app.data.guide.sync.PlanManifestMerger
import com.splitmate.app.data.guide.sync.TripGuideStore
import com.splitmate.app.guide.vm.GuideVmFixtures
import com.splitmate.app.ui.screens.plan.TripGuidePresenter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * v2.4.0-rc18 Option A verification suite:
 * - Phase 1: 3-segment [Days | Explore | Loop] ConnectedButtonGroup, Stay-First Auto-Discovery,
 *   explicit Stay distance ("from <Stay>") & "Near your stay" proximity sort.
 * - Phase 2: Manual stop reordering (buildOrderedRoute), real Reoptimise + Undo, optimization
 *   savings badge (savedKm / savedMinutes), and road-multiplied leg connectors + [Drive | Walk].
 * - Phase 3: Per-day smart auto-builder (LoopStopSelector.suggestForDay) & meal-cadence pacing
 *   (DeterministicLoopOptimizer.applyMealCadence).
 * - Phase 4: Cross-tab integration (Day Loop Summary Card in Days timeline, logged STAY booking
 *   chips in StayPinSheet, and 1-tap "Log expense" pre-fill).
 */
class SplitMateV240Rc18StaySmartLoopOptionATest {

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

    private val stayLoc = LatLng(15.3350, 76.4600)

    private fun samplePlace(
        id: String,
        kind: PlaceKind,
        lat: Double,
        lng: Double
    ): Place = Place(
        id = id,
        kind = kind,
        name = "Place $id",
        location = LatLng(lat, lng),
        source = PlaceSource.WIKIVOYAGE
    )

    @Test
    fun `RC18_01 Phase 1 3-segment ConnectedButtonGroup and explicit Stay proximity sort`() {
        val tab = src("ui/screens/plan/TripPlanTab.kt")
        assertTrue(
            tab.contains("listOf(PlanSubView.BOOKINGS, PlanSubView.EXPLORE, PlanSubView.LOOP)"),
            "Loop is promoted to the 3-segment ConnectedButtonGroup"
        )
        assertTrue(
            tab.contains("\"\${view.label} · \${state.loop.activeStopCount}\""),
            "Loop segment displays active stop count badge"
        )

        val farSight = samplePlace("wv:far", PlaceKind.SEE, 15.4000, 76.4600)
        val nearSight = samplePlace("wv:near", PlaceKind.SEE, 15.3360, 76.4600)
        val pack = GuideVmFixtures.pack(places = listOf(farSight, nearSight))
        val manifest = TripGuideStore.emptyManifest()

        val defaultSections = TripGuidePresenter.buildSections(
            pack = pack,
            manifest = manifest,
            origin = stayLoc,
            stayLabel = "Zostel Hampi",
            sortByDistance = false
        )
        assertEquals(listOf("wv:far", "wv:near"), defaultSections.single().places.map { it.place.id })
        assertTrue(
            defaultSections.single().places.first().distanceLabel!!.contains("from Zostel Hampi"),
            "Distance label explicitly names the Stay anchor"
        )

        val nearSortedSections = TripGuidePresenter.buildSections(
            pack = pack,
            manifest = manifest,
            origin = stayLoc,
            stayLabel = "Zostel Hampi",
            sortByDistance = true
        )
        assertEquals(
            listOf("wv:near", "wv:far"),
            nearSortedSections.single().places.map { it.place.id },
            "Near your stay FilterChip sorts places within each section by distance from Stay"
        )
    }

    @Test
    fun `RC18_02 Phase 2 custom stop ordering, optimization savings proof and travel mode`() {
        val s1 = samplePlace("wv:1", PlaceKind.SEE, 15.340, 76.460)
        val s2 = samplePlace("wv:2", PlaceKind.SEE, 15.390, 76.510)
        val s3 = samplePlace("wv:3", PlaceKind.SEE, 15.342, 76.462)
        val s4 = samplePlace("wv:4", PlaceKind.SEE, 15.392, 76.512)
        val pack = GuideVmFixtures.pack(places = listOf(s1, s2, s3, s4))
        var manifest = TripGuideStore.emptyManifest()
        listOf(s1, s2, s3, s4).forEachIndexed { idx, p ->
            manifest = PlanManifestMerger.pin(manifest, p.id, "me", 100L + idx)
        }
        val optimizer = DeterministicLoopOptimizer()

        // Optimized loop should save significant zig-zag distance vs (s1 -> s2 -> s3 -> s4)
        val optimizedUi = TripGuidePresenter.buildLoop(
            pack = pack,
            manifest = manifest,
            stay = stayLoc,
            optimizer = optimizer,
            travelMode = TravelMode.WALKING
        )
        assertFalse(optimizedUi.isCustomOrdered)
        assertEquals(TravelMode.WALKING, optimizedUi.travelMode)
        assertNotNull(optimizedUi.savedKm, "Zig-zag pin order should report savedKm when optimized")
        assertTrue(optimizedUi.savedKm!! >= 0.4)
        assertNotNull(optimizedUi.savedMinutes)

        // Custom manual order should be preserved without Held-Karp overwriting it
        val manualIds = listOf("wv:2", "wv:1", "wv:4", "wv:3")
        val customUi = TripGuidePresenter.buildLoop(
            pack = pack,
            manifest = manifest,
            stay = stayLoc,
            optimizer = optimizer,
            customOrderIds = manualIds,
            canUndoOptimize = true
        )
        assertTrue(customUi.isCustomOrdered)
        assertTrue(customUi.canUndoOptimize)
        assertEquals(manualIds, customUi.route!!.orderedStops.map { it.id })
    }

    @Test
    fun `RC18_03 Phase 3 suggestForDay partitions sectors and applyMealCadence spaces EAT stops`() {
        val places = listOf(
            samplePlace("see_n1", PlaceKind.SEE, 15.350, 76.460),
            samplePlace("see_n2", PlaceKind.DO, 15.355, 76.465),
            samplePlace("eat_n1", PlaceKind.EAT, 15.348, 76.458),
            samplePlace("see_s1", PlaceKind.SEE, 15.320, 76.460),
            samplePlace("see_s2", PlaceKind.DO, 15.315, 76.455),
            samplePlace("eat_s1", PlaceKind.EAT, 15.322, 76.462)
        )
        val day1Suggested = LoopStopSelector.suggestForDay(
            candidates = places,
            pinned = emptyMap(),
            hidden = emptyMap(),
            days = emptyMap(),
            targetDay = 1,
            stay = stayLoc,
            limit = 4
        )
        assertTrue(day1Suggested.isNotEmpty())
        assertTrue(day1Suggested.any { it.kind == PlaceKind.EAT }, "Includes at least 1 EAT stop")
        assertTrue(day1Suggested.any { it.kind == PlaceKind.SEE || it.kind == PlaceKind.DO }, "Includes sights/activities")

        // When Day 1 stops are assigned to day 1, Day 2 suggestions exclude Day 1 stops
        val daysMap = day1Suggested.associate { it.id to 1 }
        val pinnedMap = day1Suggested.associate { it.id to 100L }
        val day2Suggested = LoopStopSelector.suggestForDay(
            candidates = places,
            pinned = pinnedMap,
            hidden = emptyMap(),
            days = daysMap,
            targetDay = 2,
            stay = stayLoc,
            limit = 4
        )
        assertTrue(day2Suggested.none { it.id in daysMap.keys }, "Day 2 never repeats stops already on Day 1")

        // Meal cadence avoids starting at an EAT stop or placing two EAT stops back-to-back
        val backToBackMeals = listOf(
            samplePlace("eat1", PlaceKind.EAT, 15.336, 76.460),
            samplePlace("eat2", PlaceKind.EAT, 15.337, 76.461),
            samplePlace("see1", PlaceKind.SEE, 15.338, 76.462),
            samplePlace("see2", PlaceKind.SEE, 15.339, 76.463),
            samplePlace("see3", PlaceKind.DO, 15.340, 76.464)
        )
        val rawRoute = DeterministicLoopOptimizer.buildOrderedRoute(stayLoc, backToBackMeals)!!
        val pacedRoute = DeterministicLoopOptimizer.applyMealCadence(stayLoc, rawRoute)
        assertNotEquals(PlaceKind.EAT, pacedRoute.orderedStops.first().kind, "Loop does not start with an EAT stop at index 0")
        val adjacentMeals = pacedRoute.orderedStops.zipWithNext().any { (a, b) ->
            a.kind == PlaceKind.EAT && b.kind == PlaceKind.EAT
        }
        assertFalse(adjacentMeals, "Meal cadence separates back-to-back EAT stops")
    }

    @Test
    fun `RC18_04 Phase 4 cross-tab integration in TripHomeScreen, StayPinSheet and PlaceDetailSheet`() {
        val home = src("ui/screens/TripHomeScreen.kt")
        assertTrue(home.contains("day_loop_summary_\$epochDay"), "Days timeline renders per-day loop summary card")
        assertTrue(home.contains("viewModel.setPrefillExpenseTitle(place.name)"), "1-tap Log expense pre-fills place name")

        val staySheet = src("ui/screens/plan/StayPinSheet.kt")
        assertTrue(staySheet.contains("\"From your trip bookings\""), "StayPinSheet surfaces logged STAY booking suggestions")

        val placeSheet = src("ui/screens/plan/PlaceDetailSheet.kt")
        assertTrue(placeSheet.contains("\"Log expense\""), "PlaceDetailSheet offers 1-tap Log expense button")

        val loopView = src("ui/screens/plan/DayLoopView.kt")
        assertTrue(loopView.contains("LoopRoute.ROAD_FACTOR"), "LegConnector uses road-multiplied distance")
    }
}
