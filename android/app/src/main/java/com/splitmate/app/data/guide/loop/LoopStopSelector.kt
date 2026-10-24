package com.splitmate.app.data.guide.loop

import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource

/** Which stops go into the loop, and why. */
data class LoopStopSelection(
    /** Stops to optimise (always with coordinates, never hidden, ≤ [LoopStopSelector.MAX_LOOP_STOPS]). */
    val stops: List<Place>,
    /** True when nothing was pinned and stops were auto-suggested. */
    val autoSuggested: Boolean,
    /** Pinned places that could not be routed because they have no coordinates (UI may explain). */
    val pinnedWithoutCoordinates: List<Place>,
    /** True when more than [LoopStopSelector.MAX_LOOP_STOPS] pinned stops were truncated. */
    val truncated: Boolean,
    /** rc14: pinned places with no day yet ("Not planned yet"); filled only in per-day mode. */
    val unplanned: List<Place> = emptyList(),
    /** rc14: true once any pinned place has a day, so each day routes only its own places. */
    val perDay: Boolean = false
)

/**
 * Chooses loop stops (decision #22): user-pinned stops first; if none are pinned, auto-suggest the
 * top-ranked ≤ [AUTO_SUGGEST_LIMIT] places.
 *
 * Pin/hide semantics follow the synced PlanManifest OR-sets: a place counts as pinned iff
 * `pinnedAt > hiddenAt` (or it has never been hidden), and as hidden iff `hiddenAt >= pinnedAt`
 * (or it has never been pinned).
 *
 * Auto-suggest ranking (deterministic): kind priority SEE > DO > EAT (SLEEP excluded), then
 * places with coordinates only, then distance (from the stay when given, else
 * [Place.distanceKmFromCenter]), then id. Hidden places and places without coordinates are
 * excluded. Candidates are de-duplicated by id, and by QID across sources (Wikivoyage preferred
 * over Wikidata).
 */
object LoopStopSelector {
    const val AUTO_SUGGEST_LIMIT = 8
    /** Upper bound on stops in one loop (audit §4.3: > 60 is refused, so we cap). */
    const val MAX_LOOP_STOPS = 60

    fun select(
        candidates: List<Place>,
        pinned: Map<String, Long>,
        hidden: Map<String, Long>,
        stay: LatLng? = null,
        autoLimit: Int = AUTO_SUGGEST_LIMIT,
        /** rc14: placeId -> day number (PlanManifest.days). */
        days: Map<String, Int> = emptyMap(),
        /** rc14: the day being planned; null = the whole trip (legacy single loop). */
        day: Int? = null
    ): LoopStopSelection {
        val unique = dedupe(candidates)

        // Pins are matched against every candidate id (not the QID-deduped list) so a pin on a
        // Wikidata duplicate of a Wikivoyage listing is never lost.
        val pinnedPlaces = candidates
            .sortedWith(compareBy<Place>({ it.id }, { if (it.location != null) 0 else 1 }))
            .distinctBy { it.id }
            .filter { isPinned(it.id, pinned, hidden) }
        if (pinnedPlaces.isNotEmpty()) {
            // rc14: per-day mode starts once any pinned place has a day. Until then (existing trips)
            // every day shows every pinned place, exactly as before.
            val perDay = pinnedPlaces.any { days[it.id] != null }
            if (perDay && day != null) {
                val onDay = pinnedPlaces.filter { days[it.id] == day }
                val (dayCoords, dayNoCoords) = onDay.partition { it.location != null }
                val ranked = dayCoords.sortedWith(rankComparator(stay))
                return LoopStopSelection(
                    stops = ranked.take(MAX_LOOP_STOPS),
                    autoSuggested = false,
                    pinnedWithoutCoordinates = dayNoCoords.sortedBy { it.id },
                    truncated = ranked.size > MAX_LOOP_STOPS,
                    unplanned = pinnedPlaces.filter { days[it.id] == null }.sortedWith(rankComparator(stay)),
                    perDay = true
                )
            }
            val (withCoords, withoutCoords) = pinnedPlaces.partition { it.location != null }
            val ranked = withCoords.sortedWith(rankComparator(stay))
            return LoopStopSelection(
                stops = ranked.take(MAX_LOOP_STOPS),
                autoSuggested = false,
                pinnedWithoutCoordinates = withoutCoords.sortedBy { it.id },
                truncated = ranked.size > MAX_LOOP_STOPS,
                perDay = perDay
            )
        }

        val auto = unique
            .asSequence()
            .filter { it.location != null }
            .filter { it.kind != PlaceKind.SLEEP }
            .filter { !isHidden(it.id, pinned, hidden) }
            .sortedWith(rankComparator(stay))
            .take(autoLimit.coerceIn(0, MAX_LOOP_STOPS))
            .toList()
        return LoopStopSelection(auto, autoSuggested = true, pinnedWithoutCoordinates = emptyList(), truncated = false)
    }

    /**
     * v2.4.0 rc18: Suggests up to [limit] stops for [targetDay] when that day has no stops yet.
     *
     * Rules:
     * 1. Excludes SLEEP, places without coordinates, hidden places, and places already assigned
     *    to a different day (`days[id] != null && days[id] != targetDay`).
     * 2. Prioritises unplanned pinned places (`isPinned && days[id] == null`) first so the user's
     *    saved shortlist gets scheduled before unpinned places.
     * 3. Partitions candidates around [stay] into `totalDays.coerceIn(1, 6)` angular sectors so
     *    Day 1, Day 2, Day 3 explore distinct geographic zones around the Stay.
     * 4. Balances categories (up to 3 SEE/DO sights + up to 2 EAT spots for a 5-stop day) and
     *    applies a 250 m anti-clustering preference when alternatives exist.
     */
    fun suggestForDay(
        candidates: List<Place>,
        pinned: Map<String, Long>,
        hidden: Map<String, Long>,
        days: Map<String, Int>,
        targetDay: Int?,
        stay: LatLng?,
        totalDays: Int = 3,
        limit: Int = 5
    ): List<Place> {
        val cap = limit.coerceIn(1, MAX_LOOP_STOPS)
        val unique = dedupe(candidates)
        val eligible = unique.filter { p ->
            p.location != null &&
                p.kind != PlaceKind.SLEEP &&
                !isHidden(p.id, pinned, hidden) &&
                (targetDay == null || days[p.id] == null || days[p.id] == targetDay)
        }
        if (eligible.isEmpty()) return emptyList()

        val origin = stay ?: eligible.first().location
        val sectorCount = totalDays.coerceIn(1, 6)
        val targetSector = if (targetDay != null && targetDay >= 1) (targetDay - 1) % sectorCount else 0

        fun sectorDistance(p: Place): Int {
            if (origin == null || sectorCount <= 1 || targetDay == null) return 0
            val loc = p.location ?: return sectorCount
            val dLat = loc.lat - origin.lat
            val dLng = loc.lng - origin.lng
            if (kotlin.math.abs(dLat) < 1e-6 && kotlin.math.abs(dLng) < 1e-6) return 0
            val angle = (kotlin.math.atan2(dLat, dLng) + Math.PI * 2.0) % (Math.PI * 2.0)
            val sector = ((angle / (Math.PI * 2.0)) * sectorCount).toInt().coerceIn(0, sectorCount - 1)
            val diff = kotlin.math.abs(sector - targetSector)
            return minOf(diff, sectorCount - diff)
        }

        val comparator = compareBy<Place>(
            { if (isPinned(it.id, pinned, hidden) && days[it.id] == null) 0 else 1 },
            { sectorDistance(it) },
            { kindPriority(it.kind) },
            { distanceKey(it, stay) },
            { it.id }
        )
        val sorted = eligible.sortedWith(comparator)

        val maxEats = (cap / 2).coerceAtLeast(1)
        val maxSights = (cap - 1).coerceAtLeast(1)
        val picked = ArrayList<Place>(cap)
        var eatCount = 0
        var sightCount = 0

        // Pass 1: category-balanced & 250m anti-clustered selection
        for (p in sorted) {
            if (picked.size >= cap) break
            val isEat = p.kind == PlaceKind.EAT
            if (isEat && eatCount >= maxEats) continue
            if (!isEat && sightCount >= maxSights && sorted.any { it.kind == PlaceKind.EAT && it !in picked }) continue
            val tooClose = p.location != null && picked.any { existing ->
                existing.location != null && Haversine.km(existing.location, p.location) < 0.25
            }
            if (tooClose && eligible.size > cap) continue
            picked += p
            if (isEat) eatCount++ else sightCount++
        }

        // Pass 2: fill any remaining slots up to [cap]
        if (picked.size < cap) {
            for (p in sorted) {
                if (picked.size >= cap) break
                if (p !in picked) picked += p
            }
        }
        return picked
    }

    /** True iff [id] is pinned and the pin is newer than any hide. */
    fun isPinned(id: String, pinned: Map<String, Long>, hidden: Map<String, Long>): Boolean {
        val p = pinned[id] ?: return false
        val h = hidden[id] ?: return true
        return p > h
    }

    /** True iff [id] is hidden and the hide is at least as new as any pin. */
    fun isHidden(id: String, pinned: Map<String, Long>, hidden: Map<String, Long>): Boolean {
        val h = hidden[id] ?: return false
        val p = pinned[id] ?: return true
        return h >= p
    }

    private fun kindPriority(kind: PlaceKind): Int = when (kind) {
        PlaceKind.SEE -> 0
        PlaceKind.DO -> 1
        PlaceKind.EAT -> 2
        PlaceKind.SLEEP -> 3
    }

    private fun rankComparator(stay: LatLng?): Comparator<Place> = compareBy<Place>(
        { kindPriority(it.kind) },
        { if (it.location != null) 0 else 1 },
        { distanceKey(it, stay) },
        { it.id }
    )

    private fun distanceKey(p: Place, stay: LatLng?): Double {
        val loc = p.location
        if (stay != null && loc != null) return Haversine.km(stay, loc)
        return p.distanceKmFromCenter ?: Double.MAX_VALUE
    }

    /** Dedupe by id, then by QID (Wikivoyage > User > Wikidata), deterministically. */
    private fun dedupe(candidates: List<Place>): List<Place> {
        val sourceRank = { s: PlaceSource ->
            when (s) {
                PlaceSource.WIKIVOYAGE -> 0
                PlaceSource.USER -> 1
                PlaceSource.WIKIDATA -> 2
            }
        }
        val sorted = candidates.sortedWith(
            compareBy<Place>({ sourceRank(it.source) }, { if (it.location != null) 0 else 1 }, { it.id })
        )
        val seenIds = HashSet<String>()
        val seenQids = HashSet<String>()
        val out = ArrayList<Place>()
        for (p in sorted) {
            if (!seenIds.add(p.id)) continue
            val q = p.qid
            if (q != null && !seenQids.add(q)) continue
            out += p
        }
        return out
    }
}
