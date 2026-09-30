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
    val truncated: Boolean
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
        autoLimit: Int = AUTO_SUGGEST_LIMIT
    ): LoopStopSelection {
        val unique = dedupe(candidates)

        // Pins are matched against every candidate id (not the QID-deduped list) so a pin on a
        // Wikidata duplicate of a Wikivoyage listing is never lost.
        val pinnedPlaces = candidates
            .sortedWith(compareBy<Place>({ it.id }, { if (it.location != null) 0 else 1 }))
            .distinctBy { it.id }
            .filter { isPinned(it.id, pinned, hidden) }
        if (pinnedPlaces.isNotEmpty()) {
            val (withCoords, withoutCoords) = pinnedPlaces.partition { it.location != null }
            val ranked = withCoords.sortedWith(rankComparator(stay))
            return LoopStopSelection(
                stops = ranked.take(MAX_LOOP_STOPS),
                autoSuggested = false,
                pinnedWithoutCoordinates = withoutCoords.sortedBy { it.id },
                truncated = ranked.size > MAX_LOOP_STOPS
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
