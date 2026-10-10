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

    /**
     * Overview items: the next ticket (the soonest one not yet departed, allowing a few hours'
     * grace; otherwise the latest ticket) followed by the newest [maxRecent] other expenses.
     */
    fun overviewItems(
        all: List<Pair<ExpenseEntity, TripHubBookingCategory>>,
        scheduleMsOf: (ExpenseEntity) -> Long,
        nowMs: Long,
        maxRecent: Int = OVERVIEW_RECENT_COUNT
    ): List<Pair<ExpenseEntity, TripHubBookingCategory>> {
        val tickets = all.filter { isTicket(it.second) }
        val next = tickets
            .filter { scheduleMsOf(it.first) >= nowMs - RECENTLY_DEPARTED_GRACE_MS }
            .minByOrNull { scheduleMsOf(it.first) }
            ?: tickets.maxByOrNull { scheduleMsOf(it.first) }
        val recent = all
            .filter { it.first.expenseId != next?.first?.expenseId }
            .sortedWith(compareByDescending<Pair<ExpenseEntity, TripHubBookingCategory>> { it.first.createdAt }.thenBy { it.first.expenseId })
            .take(maxRecent)
        return listOfNotNull(next) + recent
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
