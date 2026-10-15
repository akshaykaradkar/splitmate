package com.splitmate.app.guide.loop

import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.data.guide.loop.DeterministicLoopOptimizer
import com.splitmate.app.data.guide.loop.HaversineCostProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.random.Random

/**
 * v2.4.0 rc13 (audit L-NEW-1): for 13+ stops the heuristic now runs Or-opt after 2-opt. It must
 * never be worse than 2-opt alone, should close most of the gap to the exact optimum, stay
 * deterministic, and the UI must not promise "the shortest" route.
 */
@DisplayName("v2.4.0 rc13: Or-opt routing")
class LoopOrOptTest {

    private fun places(seed: Int, n: Int): Pair<LatLng, List<Place>> {
        val r = Random(seed)
        val stay = LatLng(15.55 + r.nextDouble() * 0.02, 73.75 + r.nextDouble() * 0.02)
        val stops = (1..n).map { i ->
            Place(
                id = "p${seed}_$i", kind = PlaceKind.SEE, name = "Stop $i",
                location = LatLng(15.50 + r.nextDouble() * 0.12, 73.70 + r.nextDouble() * 0.12),
                source = PlaceSource.WIKIDATA
            )
        }
        return stay to stops
    }

    @Test
    fun `Or-opt is never worse than 2-opt and closes most of the gap to the exact optimum`() {
        var sumOld = 0.0
        var sumNew = 0.0
        var maxNewGap = 0.0
        repeat(30) { seed ->
            val n = 13 + seed % 2 // 13 or 14 stops: still small enough for the exact answer
            val (stay, stops) = places(seed, n)
            val canonical = DeterministicLoopOptimizer.canonicalize(stops)
            val d = HaversineCostProvider().matrixKm(listOf(stay) + canonical.map { it.location!! })
            val exact = DeterministicLoopOptimizer.tourCost(DeterministicLoopOptimizer.heldKarp(d, n), d)
            val nn = DeterministicLoopOptimizer.nearestNeighbour(d, n)
            val old = DeterministicLoopOptimizer.tourCost(DeterministicLoopOptimizer.twoOpt(nn, d), d)
            val improvedOrder = DeterministicLoopOptimizer.improve(nn, d)
            val new = DeterministicLoopOptimizer.tourCost(improvedOrder, d)
            assertEquals(n, improvedOrder.toSet().size, "every stop visited exactly once")
            assertTrue(new <= old + 1e-9, "seed $seed: Or-opt made it worse ($new > $old)")
            assertTrue(new >= exact - 1e-9)
            sumOld += old / exact - 1
            sumNew += new / exact - 1
            maxNewGap = maxOf(maxNewGap, new / exact - 1)
        }
        val meanOld = sumOld / 30
        val meanNew = sumNew / 30
        println("LoopOrOptTest: mean gap 2-opt=${"%.2f".format(meanOld * 100)}% -> Or-opt=${"%.2f".format(meanNew * 100)}%, max ${"%.2f".format(maxNewGap * 100)}%")
        assertTrue(meanNew <= meanOld, "Or-opt narrows the average gap")
        assertTrue(maxNewGap < 0.06, "worst case within 6% of the exact optimum (was up to 8.6%)")
    }

    @Test
    fun `route stays deterministic and still starts and ends at the stay`() {
        val (stay, stops) = places(7, 20)
        val opt = DeterministicLoopOptimizer()
        val a = opt.optimize(stay, stops)
        val b = opt.optimize(stay, stops.shuffled(Random(99)))
        assertEquals(a.orderedStops.map { it.id }, b.orderedStops.map { it.id }, "same stops, same route, any input order")
        assertEquals(stops.size, a.orderedStops.size)
        assertEquals(stay, a.start)
        assertEquals(a.orderedStops.size + 1, a.legStraightKm.size, "includes the return leg to the stay")
    }

    @Test
    fun `the Loop no longer promises the shortest route`() {
        val view = File("src/main/java/com/splitmate/app/ui/screens/plan/DayLoopView.kt").readText()
        assertFalse(view.contains("shortest", ignoreCase = true))
        assertTrue(view.contains("a short round trip from your stay"))
    }
}
