package com.splitmate.app

import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.LoopRoute
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.data.guide.nav.MapsHandoff
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * v2.4.0 rc13: Plan › Explore / Loop fixes from the audit (H-2, M-1, M-2, L-3). The race (M-NEW-1)
 * and routing (L-NEW-1) have their own tests in guide/sync and guide/loop.
 */
class SplitMateV240Rc13PlanFixesTest {

    private fun src(path: String) = File("src/main/java/com/splitmate/app/$path").readText()

    private fun route(stops: Int): LoopRoute {
        val places = (1..stops).map { i ->
            Place(id = "s$i", kind = PlaceKind.SEE, name = "Stop $i", location = LatLng(15.3 + i * 0.001, 76.4), source = PlaceSource.WIKIDATA)
        }
        return LoopRoute(
            start = LatLng(15.3, 76.4), orderedStops = places,
            legStraightKm = List(stops + 1) { 1.0 }, totalStraightKm = stops + 1.0, totalRoadEstimateKm = (stops + 1) * 1.35
        )
    }

    @Test
    fun `RC13_01 a long loop opens in labelled parts that chain end to start`() {
        val parts = MapsHandoff.loopDirectionsParts(route(12))
        assertEquals(listOf("Part 1 · your stay → Stop 10", "Part 2 · Stop 10 → your stay"), parts.map { it.label })
        assertEquals(MapsHandoff.loopDirectionsUrls(route(12)), parts.map { it.url }, "same links as before, now all reachable")
        assertEquals(listOf("Part 1 · your stay → your stay"), MapsHandoff.loopDirectionsParts(route(5)).map { it.label })
        val vm = src("ui/screens/plan/TripGuideViewModel.kt")
        assertTrue(vm.contains("else -> _uiState.update { it.copy(loopMapsParts = parts) }"), "more than one part -> picker, not part 1 only")
        assertTrue(src("ui/screens/plan/TripPlanTab.kt").contains("LoopMapsPartsSheet("))
    }

    @Test
    fun `RC13_02 the Loop is reachable from Explore and from an empty Days screen, and Back returns to it`() {
        val explore = src("ui/screens/plan/ExploreGuideView.kt")
        assertTrue(explore.contains("\"View loop (\$loopStops)\""))
        assertTrue(explore.contains("actions.selectSubView(PlanSubView.LOOP)"))
        val tab = src("ui/screens/plan/TripPlanTab.kt")
        assertTrue(tab.contains("current == PlanSubView.EXPLORE && exploreOpenedFromLoop -> actions.selectSubView(PlanSubView.LOOP)"))
        val trip = src("ui/screens/TripHomeScreen.kt")
        val empty = trip.substringAfter("if (classifiedExpenses.isEmpty()) {").substringBefore("return")
        assertTrue(empty.contains("onClick = onMakeLoop"), "empty Days still offers Make a loop")
    }

    @Test
    fun `RC13_03 Save offline stays hidden until it works`() {
        val explore = src("ui/screens/plan/ExploreGuideView.kt")
        assertTrue(explore.contains("internal const val SAVE_OFFLINE_READY = false"))
        assertTrue(explore.contains("if (SAVE_OFFLINE_READY && (state.saveOfflineEnabled || size != null))"))
    }

    @Test
    fun `RC13_04 Loop cards are borderless ElevatedCards and Plan icons use Material Symbols`() {
        val loop = src("ui/screens/plan/DayLoopView.kt")
        assertFalse(Regex("""(?<![\w.])Card\(""").containsMatchIn(loop), "no flat Card left in the Loop")
        assertTrue(Regex("""ElevatedCard\(""").findAll(loop).count() >= 4)
        listOf("DayLoopView.kt", "ExploreGuideView.kt", "PlaceDetailSheet.kt", "PlanGuideComponents.kt", "StayPinSheet.kt", "TripPlanTab.kt").forEach { f ->
            val s = src("ui/screens/plan/$f")
            assertFalse(Regex("""(?<![\w.])Icon\(""").containsMatchIn(s), "$f still draws a raw Icon(")
        }
    }
}
