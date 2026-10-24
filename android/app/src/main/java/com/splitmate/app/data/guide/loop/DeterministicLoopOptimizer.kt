package com.splitmate.app.data.guide.loop

import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.LoopOptimizer
import com.splitmate.app.data.guide.LoopRoute
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.RouteCostProvider
import kotlin.math.abs

/**
 * Deterministic closed-loop optimiser (audit §4.3, decisions #22 and #23).
 *
 * Determinism ("sync intents, recompute derivations"): every device must compute the same route
 * from the same stop set, whatever order the stops arrive in. So:
 * 1. Input is canonicalised: stops without coordinates are dropped, the rest are sorted by `id`
 *    (then name/lat/lng as a total order), and duplicate ids are removed (first after sort wins).
 * 2. No randomness, no hash-order dependence. All loops scan in fixed index order and ties are
 *    broken by the canonical index, i.e. lexicographic `id`.
 * 3. For symmetric cost matrices the tour is oriented so that the first stop's id sorts before
 *    the last stop's id (a tour and its reverse have equal cost).
 *
 * Algorithms:
 * - n ≤ [exactMaxStops] (default 12): Held-Karp exact DP, O(n²·2ⁿ).
 * - otherwise: nearest-neighbour from the stay, then first-improvement 2-opt in fixed scan order
 *   until no improving move remains. 2-opt assumes a symmetric matrix (true for Haversine).
 *   v2.4.0 rc13: then Or-opt (move a run of 1-3 consecutive stops, either direction, to a cheaper
 *   spot), alternating with 2-opt until neither improves. Still deterministic, never worse than
 *   2-opt alone. It narrows the gap to the exact optimum but can't guarantee it, so the UI says
 *   "a short round trip", not "the shortest".
 *
 * The route starts and ends at [start] (the stay). [LoopRoute.legStraightKm] has
 * `orderedStops.size + 1` entries (the last one is the return leg), and
 * [LoopRoute.totalRoadEstimateKm] = total × [LoopRoute.ROAD_FACTOR] (1.35, labelled "≈" in UI).
 */
class DeterministicLoopOptimizer(
    private val costProvider: RouteCostProvider = HaversineCostProvider(),
    private val exactMaxStops: Int = DEFAULT_EXACT_MAX_STOPS
) : LoopOptimizer {

    init {
        require(exactMaxStops in 0..MAX_EXACT_STOPS) { "exactMaxStops must be in 0..$MAX_EXACT_STOPS" }
    }

    override fun optimize(start: LatLng, stops: List<Place>): LoopRoute {
        val canonical = canonicalize(stops)
        val points = ArrayList<LatLng>(canonical.size + 1)
        points += start
        canonical.forEach { points += it.location!! }
        val d = costProvider.matrixKm(points)
        require(d.size == points.size && d.all { it.size == points.size }) { "cost matrix size mismatch" }

        val n = canonical.size
        val raw: IntArray = when {
            n == 0 -> IntArray(0)
            n <= exactMaxStops -> heldKarp(d, n)
            else -> improve(nearestNeighbour(d, n), d)
        }
        val order = orient(raw, d)

        val legs = ArrayList<Double>(n + 1)
        var prev = 0
        for (idx in order) {
            legs += d[prev][idx]
            prev = idx
        }
        legs += d[prev][0]
        val total = legs.sum()
        return LoopRoute(
            start = start,
            orderedStops = order.map { canonical[it - 1] },
            legStraightKm = legs,
            totalStraightKm = total,
            totalRoadEstimateKm = total * LoopRoute.ROAD_FACTOR
        )
    }

    companion object {
        const val DEFAULT_EXACT_MAX_STOPS = 12
        /** Hard upper bound for Held-Karp (2^16 × 16 states ≈ 1M doubles). */
        const val MAX_EXACT_STOPS = 16

        private const val IMPROVEMENT_EPS = 1e-9
        private const val TIE_EPS = 1e-12
        private const val MAX_TWO_OPT_PASSES = 1_000
        private const val MAX_OR_OPT_MOVES = 10_000
        private const val MAX_IMPROVE_ROUNDS = 100
        private const val OR_OPT_MAX_SEGMENT = 3

        private val CANONICAL_ORDER: Comparator<Place> = compareBy<Place>(
            { it.id }, { it.name }, { it.location?.lat }, { it.location?.lng }
        )

        /** Drops stops without coordinates, sorts by id (total order), removes duplicate ids. */
        internal fun canonicalize(stops: List<Place>): List<Place> =
            stops.filter { it.location != null }
                .sortedWith(CANONICAL_ORDER)
                .distinctBy { it.id }

        /**
         * Held-Karp exact TSP over matrix indices 1..n with fixed depot 0.
         * @return visiting order as matrix indices (1-based stop indices).
         */
        internal fun heldKarp(d: Array<DoubleArray>, n: Int): IntArray {
            if (n == 1) return intArrayOf(1)
            val full = (1 shl n) - 1
            val size = (1 shl n) * n
            val dp = DoubleArray(size) { Double.POSITIVE_INFINITY }
            val parent = IntArray(size) { -1 }
            for (j in 0 until n) dp[(1 shl j) * n + j] = d[0][j + 1]
            for (mask in 1..full) {
                for (j in 0 until n) {
                    if (mask and (1 shl j) == 0) continue
                    val cur = dp[mask * n + j]
                    if (cur == Double.POSITIVE_INFINITY) continue
                    for (k in 0 until n) {
                        if (mask and (1 shl k) != 0) continue
                        val next = mask or (1 shl k)
                        val c = cur + d[j + 1][k + 1]
                        val slot = next * n + k
                        if (c < dp[slot]) {
                            dp[slot] = c
                            parent[slot] = j
                        }
                    }
                }
            }
            var bestJ = -1
            var best = Double.POSITIVE_INFINITY
            for (j in 0 until n) {
                val c = dp[full * n + j] + d[j + 1][0]
                if (c < best) {
                    best = c
                    bestJ = j
                }
            }
            val order = IntArray(n)
            var mask = full
            var j = bestJ
            for (pos in n - 1 downTo 0) {
                order[pos] = j + 1
                val p = parent[mask * n + j]
                mask = mask and (1 shl j).inv()
                j = p
            }
            return order
        }

        /** Nearest-neighbour tour from depot 0; ties go to the smaller (canonical) index. */
        internal fun nearestNeighbour(d: Array<DoubleArray>, n: Int): IntArray {
            val visited = BooleanArray(n + 1)
            visited[0] = true
            val order = IntArray(n)
            var cur = 0
            for (step in 0 until n) {
                var best = -1
                var bestD = Double.POSITIVE_INFINITY
                for (k in 1..n) {
                    if (visited[k]) continue
                    val dk = d[cur][k]
                    if (best == -1 || dk < bestD - TIE_EPS) {
                        best = k
                        bestD = dk
                    }
                }
                order[step] = best
                visited[best] = true
                cur = best
            }
            return order
        }

        /**
         * First-improvement 2-opt on the closed tour `0 → order… → 0`, fixed scan order.
         * Returns a new array. Never returns a tour costlier than the input.
         */
        internal fun twoOpt(order: IntArray, d: Array<DoubleArray>): IntArray {
            val n = order.size
            if (n < 3) return order.copyOf()
            val r = IntArray(n + 2)
            for (i in 0 until n) r[i + 1] = order[i]
            // r[0] = r[n + 1] = 0 (depot)
            var improved = true
            var passes = 0
            while (improved && passes < MAX_TWO_OPT_PASSES) {
                improved = false
                passes++
                for (i in 1 until n) {
                    for (k in i + 1..n) {
                        val a = r[i - 1]
                        val b = r[i]
                        val c = r[k]
                        val e = r[k + 1]
                        val delta = d[a][c] + d[b][e] - d[a][b] - d[c][e]
                        if (delta < -IMPROVEMENT_EPS) {
                            reverse(r, i, k)
                            improved = true
                        }
                    }
                }
            }
            return r.copyOfRange(1, n + 1)
        }

        /** 2-opt, then alternate Or-opt and 2-opt until neither improves the closed tour. */
        internal fun improve(order: IntArray, d: Array<DoubleArray>): IntArray {
            var best = twoOpt(order, d)
            var bestCost = tourCost(best, d)
            var rounds = 0
            while (rounds++ < MAX_IMPROVE_ROUNDS) {
                val moved = orOpt(best, d)
                val candidate = twoOpt(moved, d)
                val cost = tourCost(candidate, d)
                if (cost < bestCost - IMPROVEMENT_EPS) {
                    best = candidate
                    bestCost = cost
                } else {
                    break
                }
            }
            return best
        }

        /**
         * First-improvement Or-opt on the closed tour `0 → order… → 0`: takes a run of 1..3
         * consecutive stops and re-inserts it (as is or reversed) between two other neighbours when
         * that shortens the tour. Fixed scan order (segment length, then position, then slot), so
         * it is deterministic. Never returns a costlier tour than the input.
         */
        internal fun orOpt(order: IntArray, d: Array<DoubleArray>): IntArray {
            val n = order.size
            if (n < 3) return order.copyOf()
            var tour = order.toMutableList()
            var moves = 0
            var improved = true
            while (improved && moves < MAX_OR_OPT_MOVES) {
                improved = false
                search@ for (len in 1..minOf(OR_OPT_MAX_SEGMENT, n - 1)) {
                    for (i in 0..n - len) {
                        val s0 = tour[i]
                        val sLast = tour[i + len - 1]
                        val prev = if (i == 0) 0 else tour[i - 1]
                        val next = if (i + len == n) 0 else tour[i + len]
                        val removeGain = d[prev][s0] + d[sLast][next] - d[prev][next]
                        val rest = ArrayList<Int>(n - len).apply {
                            addAll(tour.subList(0, i)); addAll(tour.subList(i + len, n))
                        }
                        for (p in 0..rest.size) {
                            if (p == i) continue // same place
                            val a = if (p == 0) 0 else rest[p - 1]
                            val b = if (p == rest.size) 0 else rest[p]
                            val forward = d[a][s0] + d[sLast][b] - d[a][b]
                            val reversed = d[a][sLast] + d[s0][b] - d[a][b]
                            val useReversed = reversed < forward - TIE_EPS
                            val add = if (useReversed) reversed else forward
                            if (add - removeGain < -IMPROVEMENT_EPS) {
                                val seg = tour.subList(i, i + len).toList().let { if (useReversed) it.reversed() else it }
                                rest.addAll(p, seg)
                                tour = rest
                                improved = true
                                moves++
                                break@search
                            }
                        }
                    }
                }
            }
            return tour.toIntArray()
        }

        /** Cost of the closed tour `0 → order… → 0`. */
        internal fun tourCost(order: IntArray, d: Array<DoubleArray>): Double {
            var prev = 0
            var total = 0.0
            for (idx in order) {
                total += d[prev][idx]
                prev = idx
            }
            return total + d[prev][0]
        }

        private fun reverse(a: IntArray, from: Int, to: Int) {
            var i = from
            var j = to
            while (i < j) {
                val t = a[i]; a[i] = a[j]; a[j] = t
                i++; j--
            }
        }

        /** For symmetric matrices, orient the tour so the first stop's canonical index < last's. */
        private fun orient(order: IntArray, d: Array<DoubleArray>): IntArray {
            if (order.size < 2 || !isSymmetric(d)) return order
            return if (order.first() > order.last()) order.reversedArray() else order
        }

        private fun isSymmetric(d: Array<DoubleArray>): Boolean {
            for (i in d.indices) for (j in i + 1 until d.size) {
                if (abs(d[i][j] - d[j][i]) > IMPROVEMENT_EPS) return false
            }
            return true
        }

        /**
         * v2.4.0 rc18: Builds a [LoopRoute] that preserves [orderedStops]'s exact visiting order
         * (used for manual user reordering and for computing baseline unoptimized tour distance).
         */
        fun buildOrderedRoute(
            start: LatLng,
            orderedStops: List<Place>,
            costProvider: RouteCostProvider = HaversineCostProvider()
        ): LoopRoute {
            val valid = orderedStops.filter { it.location != null }.distinctBy { it.id }
            if (valid.isEmpty()) {
                return LoopRoute(
                    start = start,
                    orderedStops = emptyList(),
                    legStraightKm = listOf(0.0),
                    totalStraightKm = 0.0,
                    totalRoadEstimateKm = 0.0
                )
            }
            val points = ArrayList<LatLng>(valid.size + 1)
            points += start
            valid.forEach { points += it.location!! }
            val d = costProvider.matrixKm(points)
            val legs = ArrayList<Double>(valid.size + 1)
            var prev = 0
            for (idx in 1..valid.size) {
                legs += d[prev][idx]
                prev = idx
            }
            legs += d[prev][0]
            val total = legs.sum()
            return LoopRoute(
                start = start,
                orderedStops = valid,
                legStraightKm = legs,
                totalStraightKm = total,
                totalRoadEstimateKm = total * LoopRoute.ROAD_FACTOR
            )
        }

        /**
         * v2.4.0 rc18: Meal-cadence post-pass for day loops.
         *
         * When a loop has >= 4 stops with both sights (SEE/DO) and restaurants/cafes (EAT), pure
         * geometric TSP can schedule a restaurant as Stop #1 in the morning or put two EAT stops
         * back-to-back. This post-pass keeps the Held-Karp relative order of sights intact and
         * slots EAT stops into natural midday (lunch) and evening (dinner) slots when the added
         * detour is <= 35% + 1.5 km straight-line.
         */
        fun applyMealCadence(
            start: LatLng,
            route: LoopRoute,
            costProvider: RouteCostProvider = HaversineCostProvider()
        ): LoopRoute {
            val stops = route.orderedStops
            if (stops.size < 4) return route
            val sights = stops.filter { it.kind != PlaceKind.EAT }
            val eats = stops.filter { it.kind == PlaceKind.EAT }
            if (eats.isEmpty() || sights.size < 2) return route

            val startsWithEat = stops.first().kind == PlaceKind.EAT
            val hasAdjacentEats = (0 until stops.size - 1).any {
                stops[it].kind == PlaceKind.EAT && stops[it + 1].kind == PlaceKind.EAT
            }
            if (!startsWithEat && !hasAdjacentEats) return route

            val paced = ArrayList<Place>(stops.size)
            val remainingEats = eats.toMutableList()
            val lunchSlotAfterSightIndex = (sights.size / 2).coerceAtLeast(1) - 1

            for ((idx, sight) in sights.withIndex()) {
                paced += sight
                if (idx == lunchSlotAfterSightIndex && remainingEats.isNotEmpty()) {
                    val anchor = sight.location ?: start
                    val nextAnchor = sights.getOrNull(idx + 1)?.location ?: start
                    val bestLunch = remainingEats.minByOrNull { eat ->
                        val loc = eat.location ?: start
                        Haversine.km(anchor, loc) + Haversine.km(loc, nextAnchor)
                    }!!
                    paced += bestLunch
                    remainingEats.remove(bestLunch)
                }
            }
            while (remainingEats.isNotEmpty()) {
                val anchor = paced.lastOrNull()?.location ?: start
                val bestNext = remainingEats.minByOrNull { eat ->
                    val loc = eat.location ?: start
                    Haversine.km(anchor, loc) + Haversine.km(loc, start)
                }!!
                paced += bestNext
                remainingEats.remove(bestNext)
            }

            val candidate = buildOrderedRoute(start, paced, costProvider)
            val maxAllowedKm = route.totalStraightKm * 1.35 + 1.5
            return if (candidate.totalStraightKm <= maxAllowedKm) candidate else route
        }
    }
}
