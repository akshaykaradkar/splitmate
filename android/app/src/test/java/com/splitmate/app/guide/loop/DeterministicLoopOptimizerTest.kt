package com.splitmate.app.guide.loop

import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.LoopRoute
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.data.guide.loop.DeterministicLoopOptimizer
import com.splitmate.app.data.guide.loop.Haversine
import com.splitmate.app.data.guide.loop.HaversineCostProvider
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@DisplayName("DeterministicLoopOptimizer: Held-Karp / NN + 2-opt, deterministic")
class DeterministicLoopOptimizerTest {

    private val stay = LatLng(15.335, 76.46)
    private val optimizer = DeterministicLoopOptimizer()
    private val costs = HaversineCostProvider()

    private fun place(id: String, lat: Double, lng: Double, kind: PlaceKind = PlaceKind.SEE) =
        Place(id = id, kind = kind, name = "Place $id", location = LatLng(lat, lng), source = PlaceSource.WIKIDATA)

    /** Seeded random stops within ~15 km of the stay. */
    private fun randomStops(n: Int, seed: Int): List<Place> {
        val rnd = Random(seed)
        return (0 until n).map { i ->
            place(
                id = "p%03d".format(i),
                lat = stay.lat + rnd.nextDouble(-0.13, 0.13),
                lng = stay.lng + rnd.nextDouble(-0.13, 0.13)
            )
        }
    }

    private fun matrixFor(stops: List<Place>): Array<DoubleArray> =
        costs.matrixKm(listOf(stay) + stops.map { it.location!! })

    private fun routeCost(route: LoopRoute): Double {
        val pts = listOf(stay) + route.orderedStops.map { it.location!! } + stay
        return pts.zipWithNext { a, b -> Haversine.km(a, b) }.sum()
    }

    /** Exhaustive optimum for small n (reference oracle). */
    private fun bruteForceOptimum(stops: List<Place>): Double {
        val d = matrixFor(stops)
        val idx = (1..stops.size).toMutableList()
        var best = Double.POSITIVE_INFINITY
        fun permute(k: Int) {
            if (k == idx.size) {
                best = minOf(best, DeterministicLoopOptimizer.tourCost(idx.toIntArray(), d))
                return
            }
            for (i in k until idx.size) {
                java.util.Collections.swap(idx, k, i)
                permute(k + 1)
                java.util.Collections.swap(idx, k, i)
            }
        }
        permute(0)
        return best
    }

    // ------------------------------------------------------------------ Haversine

    @Test
    fun `haversine matches known distances`() {
        // 1 degree of longitude on the equator = 2*pi*R/360.
        assertEquals(2 * PI * Haversine.EARTH_RADIUS_KM / 360.0, Haversine.km(LatLng(0.0, 10.0), LatLng(0.0, 11.0)), 1e-6)
        assertEquals(0.0, Haversine.km(stay, stay), 0.0)
        val m = costs.matrixKm(listOf(stay, LatLng(15.3, 76.4), LatLng(15.2, 76.5)))
        for (i in 0..2) {
            assertEquals(0.0, m[i][i], 0.0)
            for (j in 0..2) assertEquals(m[i][j], m[j][i], 0.0)
        }
    }

    // ------------------------------------------------------------------ Trivial sizes

    @Test
    fun `zero stops gives an empty loop with a single zero leg`() {
        val r = optimizer.optimize(stay, emptyList())
        assertTrue(r.orderedStops.isEmpty())
        assertEquals(listOf(0.0), r.legStraightKm)
        assertEquals(0.0, r.totalStraightKm, 0.0)
        assertEquals(0.0, r.totalRoadEstimateKm, 0.0)
        assertEquals(stay, r.start)
    }

    @Test
    fun `one stop is out and back`() {
        val p = place("a", 15.30, 76.47)
        val r = optimizer.optimize(stay, listOf(p))
        val d = Haversine.km(stay, p.location!!)
        assertEquals(listOf(p), r.orderedStops)
        assertEquals(2, r.legStraightKm.size)
        assertEquals(d, r.legStraightKm[0], 1e-12)
        assertEquals(d, r.legStraightKm[1], 1e-12)
        assertEquals(2 * d, r.totalStraightKm, 1e-12)
        assertEquals(2 * d * LoopRoute.ROAD_FACTOR, r.totalRoadEstimateKm, 1e-12)
    }

    @Test
    fun `two stops are oriented canonically (first id sorts before last)`() {
        val a = place("a", 15.30, 76.47)
        val b = place("b", 15.36, 76.50)
        val r1 = optimizer.optimize(stay, listOf(b, a))
        val r2 = optimizer.optimize(stay, listOf(a, b))
        assertEquals(listOf("a", "b"), r1.orderedStops.map { it.id })
        assertEquals(r1, r2)
        assertEquals(3, r1.legStraightKm.size)
    }

    @Test
    fun `stops without coordinates are dropped and duplicate ids collapse`() {
        val a = place("a", 15.30, 76.47)
        val aDup = place("a", 15.30, 76.47)
        val noLoc = Place(id = "z", kind = PlaceKind.SEE, name = "No coords", location = null, source = PlaceSource.USER)
        val r = optimizer.optimize(stay, listOf(a, noLoc, aDup))
        assertEquals(listOf("a"), r.orderedStops.map { it.id })
    }

    @Test
    fun `co-located stops with different ids are both visited with a zero leg between them`() {
        val a = place("a", 15.30, 76.47)
        val b = place("b", 15.30, 76.47)
        val c = place("c", 15.36, 76.50)
        val r = optimizer.optimize(stay, listOf(c, b, a))
        assertEquals(3, r.orderedStops.size)
        val ia = r.orderedStops.indexOfFirst { it.id == "a" }
        val ib = r.orderedStops.indexOfFirst { it.id == "b" }
        assertEquals(1, kotlin.math.abs(ia - ib), "co-located stops should be adjacent")
        assertEquals(0.0, r.legStraightKm[maxOf(ia, ib)], 1e-12)
    }

    // ------------------------------------------------------------------ Optimality

    @Test
    fun `Held-Karp matches brute force on random instances n = 1 to 8`() {
        for (n in 1..8) for (seed in 1..5) {
            val stops = randomStops(n, seed * 100 + n)
            val r = optimizer.optimize(stay, stops)
            assertEquals(bruteForceOptimum(stops), r.totalStraightKm, 1e-9, "n=$n seed=$seed")
            assertEquals(n, r.orderedStops.size)
        }
    }

    @Test
    fun `collinear stops are visited in line order`() {
        // Stay at the west end; stops strung out eastwards. Optimal loop = out and back.
        val stops = listOf(place("c", 15.335, 76.52), place("a", 15.335, 76.48), place("b", 15.335, 76.50))
        val r = optimizer.optimize(stay, stops)
        assertEquals(listOf("a", "b", "c"), r.orderedStops.map { it.id })
        assertEquals(2 * Haversine.km(stay, stops[0].location!!), r.totalStraightKm, 1e-6)
    }

    @Test
    fun `points on a circle are visited in angular order (Held-Karp and 2-opt)`() {
        for (n in listOf(10, 20)) {
            val center = LatLng(15.335, 76.46)
            val radiusDeg = 0.05
            // Stay sits on the circle at angle 0; stops at the other angles, ids scrambled.
            val start = LatLng(center.lat, center.lng + radiusDeg)
            val stops = (1 until n + 1).map { k ->
                val theta = 2 * PI * k / (n + 1)
                place("s" + ((k * 7) % 97).toString().padStart(2, '0'), center.lat + radiusDeg * sin(theta), center.lng + radiusDeg * cos(theta))
            }
            val angularIds = stops.map { it.id }
            val r = optimizer.optimize(start, stops.shuffled(Random(n)))
            val ids = r.orderedStops.map { it.id }
            assertTrue(ids == angularIds || ids == angularIds.reversed(), "n=$n got $ids")
        }
    }

    @Test
    fun `2-opt is never worse than nearest neighbour`() {
        for (n in listOf(13, 20, 30, 45, 60)) for (seed in 1..6) {
            val stops = DeterministicLoopOptimizer.canonicalize(randomStops(n, seed * 31 + n))
            val d = matrixFor(stops)
            val nn = DeterministicLoopOptimizer.nearestNeighbour(d, n)
            val opt = DeterministicLoopOptimizer.twoOpt(nn, d)
            val nnCost = DeterministicLoopOptimizer.tourCost(nn, d)
            val optCost = DeterministicLoopOptimizer.tourCost(opt, d)
            assertTrue(optCost <= nnCost + 1e-9, "n=$n seed=$seed nn=$nnCost 2opt=$optCost")
            assertEquals((1..n).toList(), opt.sorted(), "2-opt output must be a permutation")
        }
    }

    @Test
    fun `Held-Karp is never worse than NN plus 2-opt for n up to 12`() {
        for (n in 3..12) {
            val stops = DeterministicLoopOptimizer.canonicalize(randomStops(n, 7 * n))
            val d = matrixFor(stops)
            val hk = DeterministicLoopOptimizer.tourCost(DeterministicLoopOptimizer.heldKarp(d, n), d)
            val heur = DeterministicLoopOptimizer.tourCost(
                DeterministicLoopOptimizer.twoOpt(DeterministicLoopOptimizer.nearestNeighbour(d, n), d), d
            )
            assertTrue(hk <= heur + 1e-9, "n=$n hk=$hk heuristic=$heur")
        }
    }

    // ------------------------------------------------------------------ Determinism

    @Test
    fun `identical output under input shuffles (exact and heuristic paths)`() {
        for (n in listOf(5, 12, 13, 30, 60)) {
            val stops = randomStops(n, 4242 + n)
            val reference = optimizer.optimize(stay, stops)
            repeat(15) { k ->
                val shuffled = stops.shuffled(Random(k * 13 + n))
                val r = optimizer.optimize(stay, shuffled)
                assertEquals(reference.orderedStops.map { it.id }, r.orderedStops.map { it.id }, "n=$n shuffle=$k")
                assertEquals(reference.totalStraightKm, r.totalStraightKm, 0.0)
                assertEquals(reference.legStraightKm, r.legStraightKm)
            }
        }
    }

    @Test
    fun `nearest neighbour ties break by lexicographic id (lower canonical index)`() {
        // Exact matrix: stops 1 and 2 are both 1.0 from the stay, 2.0 apart, 3 is far.
        val d = arrayOf(
            doubleArrayOf(0.0, 1.0, 1.0, 5.0),
            doubleArrayOf(1.0, 0.0, 2.0, 4.0),
            doubleArrayOf(1.0, 2.0, 0.0, 4.0),
            doubleArrayOf(5.0, 4.0, 4.0, 0.0)
        )
        assertArrayEquals(intArrayOf(1, 2, 3), DeterministicLoopOptimizer.nearestNeighbour(d, 3))
    }

    @Test
    fun `equal-cost alternatives resolve identically with a custom cost provider`() {
        // Every pair costs the same: all tours tie, so the result must still be stable.
        val flat = object : com.splitmate.app.data.guide.RouteCostProvider {
            override fun matrixKm(points: List<LatLng>): Array<DoubleArray> =
                Array(points.size) { i -> DoubleArray(points.size) { j -> if (i == j) 0.0 else 1.0 } }
        }
        val opt = DeterministicLoopOptimizer(flat)
        for (n in listOf(6, 20)) {
            val stops = randomStops(n, n)
            val ref = opt.optimize(stay, stops).orderedStops.map { it.id }
            repeat(5) { k ->
                assertEquals(ref, opt.optimize(stay, stops.shuffled(Random(k))).orderedStops.map { it.id })
            }
            assertEquals((n + 1).toDouble(), opt.optimize(stay, stops).totalStraightKm, 1e-12)
        }
    }

    // ------------------------------------------------------------------ Output shape & perf

    @Test
    fun `legs sum to total and road estimate uses factor 1_35`() {
        for (n in listOf(4, 12, 25)) {
            val r = optimizer.optimize(stay, randomStops(n, n))
            assertEquals(n + 1, r.legStraightKm.size)
            assertEquals(r.legStraightKm.sum(), r.totalStraightKm, 1e-9)
            assertEquals(routeCost(r), r.totalStraightKm, 1e-9)
            assertEquals(r.totalStraightKm * 1.35, r.totalRoadEstimateKm, 1e-9)
            assertEquals(n, r.orderedStops.map { it.id }.toSet().size)
        }
    }

    @Test
    fun `n = 60 optimises in under 50 ms`() {
        val stops = randomStops(60, 60)
        repeat(5) { optimizer.optimize(stay, stops) } // JIT warm-up
        var best = Long.MAX_VALUE
        repeat(5) {
            val t0 = System.nanoTime()
            optimizer.optimize(stay, stops.shuffled(Random(it)))
            best = minOf(best, System.nanoTime() - t0)
        }
        val ms = best / 1_000_000.0
        assertTrue(ms < 50.0, "n=60 took $ms ms")
    }

    @Test
    fun `n = 12 exact path is fast`() {
        val stops = randomStops(12, 12)
        repeat(3) { optimizer.optimize(stay, stops) }
        val t0 = System.nanoTime()
        optimizer.optimize(stay, stops)
        val ms = (System.nanoTime() - t0) / 1_000_000.0
        assertTrue(ms < 500.0, "n=12 Held-Karp took $ms ms")
    }
}
