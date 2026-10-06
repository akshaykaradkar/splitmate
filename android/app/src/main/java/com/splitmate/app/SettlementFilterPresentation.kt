package com.splitmate.app

/**
 * v2.3.3 "Your settlements" filter presentation (100% pure Kotlin, no money math).
 *
 * Bug fixed: the "All settled" badge and "You're all settled up (₹0.00)" card were driven by the
 * FILTERED member list, so tapping "Gets Back" while the current user only owes money hid their row
 * and falsely claimed they were settled. The truly-settled state is now computed from the UNFILTERED
 * summaries; a filter that merely hides rows gets a context-aware empty state instead.
 */
object SettlementFilterPresentation {

    const val FILTER_ALL = 0
    const val FILTER_GETS_BACK = 1
    const val FILTER_OWES = 2

    /** A context-aware empty state. Tapping it switches to [switchToFilterIndex] (if non-null). */
    data class FilterEmptyState(
        val title: String,
        val subtitle: String,
        val switchToFilterIndex: Int?
    )

    data class YourSettlementsPresentation(
        /** Current user has no incoming and no outgoing payments (from UNFILTERED data). */
        val isTrulySettled: Boolean,
        /** Show the "You're all settled up (₹0.00)" card + "All settled" badge. */
        val showSettledCard: Boolean,
        /** Current user's row is hidden by the active filter even though they are NOT settled. */
        val myFilterEmptyState: FilterEmptyState?,
        /** Other travelers exist with balances but none match the active filter. */
        val othersFilterEmptyState: FilterEmptyState?
    )

    fun matchesFilter(summary: SplitMateMathEngine.MemberSettlementSummary, filterIndex: Int): Boolean =
        when (filterIndex) {
            FILTER_GETS_BACK -> summary.hasIncoming
            FILTER_OWES -> summary.hasOutgoing
            else -> true
        }

    fun isCurrentUserTrulySettled(allSummaries: List<SplitMateMathEngine.MemberSettlementSummary>): Boolean {
        val me = allSummaries.firstOrNull { it.isCurrentUser } ?: return true
        return !me.hasIncoming && !me.hasOutgoing
    }

    fun resolve(
        allSummaries: List<SplitMateMathEngine.MemberSettlementSummary>,
        filterIndex: Int
    ): YourSettlementsPresentation {
        val me = allSummaries.firstOrNull { it.isCurrentUser }
        val trulySettled = isCurrentUserTrulySettled(allSummaries)

        val myFilterEmpty = if (!trulySettled && me != null && !matchesFilter(me, filterIndex)) {
            when (filterIndex) {
                FILTER_GETS_BACK -> FilterEmptyState(
                    title = "Nothing to get back",
                    subtitle = "You owe ${me.formattedTotalOutgoing} in this trip · see Owes",
                    switchToFilterIndex = FILTER_OWES
                )
                FILTER_OWES -> FilterEmptyState(
                    title = "You don't owe anyone",
                    subtitle = "You get back ${me.formattedTotalIncoming} · see Gets Back",
                    switchToFilterIndex = FILTER_GETS_BACK
                )
                else -> null
            }
        } else {
            null
        }

        val others = allSummaries.filter { !it.isCurrentUser }
        val othersEmpty = if (filterIndex != FILTER_ALL && others.isNotEmpty() && others.none { matchesFilter(it, filterIndex) }) {
            when (filterIndex) {
                FILTER_GETS_BACK -> FilterEmptyState(
                    title = "No other traveler gets money back",
                    subtitle = "Everyone else only owes · see All Balances",
                    switchToFilterIndex = FILTER_ALL
                )
                else -> FilterEmptyState(
                    title = "No other traveler owes money",
                    subtitle = "Everyone else only gets back · see All Balances",
                    switchToFilterIndex = FILTER_ALL
                )
            }
        } else {
            null
        }

        return YourSettlementsPresentation(
            isTrulySettled = trulySettled,
            showSettledCard = trulySettled,
            myFilterEmptyState = myFilterEmpty,
            othersFilterEmptyState = othersEmpty
        )
    }
}
