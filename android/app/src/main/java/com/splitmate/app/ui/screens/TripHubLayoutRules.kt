package com.splitmate.app.ui.screens

import com.splitmate.app.data.ExpenseEntity

/**
 * v2.4.0 Trip Hub step 1 (approved plan "B"): one home for each thing.
 * - The Trip spend card lives on Money only; other tabs show a small balance chip that opens Money.
 * - Overview shows "what's next": the next ticket plus the most recent expenses, with a link to the
 *   one full expense list on Money (no more duplicate lists).
 * Pure rules (unit-tested); no money math.
 */
object TripHubLayoutRules {

    const val OVERVIEW_RECENT_COUNT = 3
    private const val RECENTLY_DEPARTED_GRACE_MS = 6L * 60 * 60 * 1000

    fun isTicket(category: TripHubBookingCategory): Boolean =
        category == TripHubBookingCategory.TRAIN || category == TripHubBookingCategory.FLIGHT

    /** v2.4.0 Trip Hub step 3: where the trip is, so Overview can lead with what matters now. */
    enum class TripPhase { BEFORE, DURING, AFTER }

    private fun dayOf(ms: Long, zone: java.time.ZoneId): java.time.LocalDate =
        java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    /**
     * AFTER once the trip is wrapped up (or its last scheduled day has passed); DURING from the first
     * scheduled day to the last; otherwise BEFORE.
     */
    fun phaseOf(
        isEnded: Boolean,
        scheduledMs: List<Long>,
        nowMs: Long,
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault()
    ): TripPhase {
        if (isEnded) return TripPhase.AFTER
        if (scheduledMs.isEmpty()) return TripPhase.BEFORE
        val today = dayOf(nowMs, zone)
        val first = dayOf(scheduledMs.min(), zone)
        val last = dayOf(scheduledMs.max(), zone)
        return when {
            today.isBefore(first) -> TripPhase.BEFORE
            today.isAfter(last) -> TripPhase.AFTER
            else -> TripPhase.DURING
        }
    }

    /**
     * Overview items, ordered by trip phase:
     * - BEFORE: the next ticket (soonest not yet departed, a few hours' grace; else the latest),
     *   then the newest [maxRecent] other expenses.
     * - DURING: today's plan first (up to 3, tickets first, in time order), then the next ticket,
     *   then recent expenses.
     * - AFTER: the newest expenses only (the wrap-up card above already leads to settling up).
     */
    fun overviewItems(
        all: List<Pair<ExpenseEntity, TripHubBookingCategory>>,
        scheduleMsOf: (ExpenseEntity) -> Long,
        nowMs: Long,
        maxRecent: Int = OVERVIEW_RECENT_COUNT,
        phase: TripPhase = TripPhase.BEFORE,
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault()
    ): List<Pair<ExpenseEntity, TripHubBookingCategory>> {
        val newestFirst = compareByDescending<Pair<ExpenseEntity, TripHubBookingCategory>> { it.first.createdAt }
            .thenBy { it.first.expenseId }
        if (phase == TripPhase.AFTER) return all.sortedWith(newestFirst).take(maxRecent)

        val tickets = all.filter { isTicket(it.second) }
        val next = tickets
            .filter { scheduleMsOf(it.first) >= nowMs - RECENTLY_DEPARTED_GRACE_MS }
            .minByOrNull { scheduleMsOf(it.first) }
            ?: tickets.maxByOrNull { scheduleMsOf(it.first) }
        val today = if (phase == TripPhase.DURING) {
            val day = dayOf(nowMs, zone)
            all.filter { dayOf(scheduleMsOf(it.first), zone) == day }
                .sortedWith(compareBy<Pair<ExpenseEntity, TripHubBookingCategory>> { if (isTicket(it.second)) 0 else 1 }.thenBy { scheduleMsOf(it.first) })
                .take(3)
        } else {
            emptyList()
        }
        val lead = (today + listOfNotNull(next)).distinctBy { it.first.expenseId }
        val leadIds = lead.map { it.first.expenseId }.toSet()
        val recent = all.filter { it.first.expenseId !in leadIds }.sortedWith(newestFirst).take(maxRecent)
        return lead + recent
    }

    /** Label for the balance chip shown on every tab except Money. */
    fun balanceChipLabel(netCents: Long, formattedAbs: String): String = when {
        netCents > 0L -> "You get back $formattedAbs"
        netCents < 0L -> "You owe $formattedAbs"
        else -> "All settled"
    }

    /** The spend hero card shows only on Money; everywhere else the compact chip. */
    fun showsSpendCard(tab: TripHubSectionTab): Boolean = tab == TripHubSectionTab.MONEY
}
