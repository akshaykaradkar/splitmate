package com.splitmate.app.data

/**
 * v2.3.5 (#1): pure trip lifecycle derivation. Never auto-ends a trip: ENDED only comes from an
 * organizer's explicit "Wrap up" (synced [TripLifecycleRecord]); everything else is a suggestion.
 */
object TripLifecycleResolver {
    enum class State { ACTIVE, ENDING_SOON, ENDED_SUGGESTED, ENDED }

    const val ONE_DAY_MS = 86_400_000L
    /** No new expense for this long (after the last scheduled booking) -> suggest wrapping up. */
    const val QUIET_DAYS_BEFORE_SUGGEST = 3

    fun resolve(
        record: TripLifecycleRecord?,
        currentEpochMs: Long,
        /** Latest expense createdAt / scheduled time in the trip (null = no expenses). */
        lastActivityEpochMs: Long? = null
    ): State {
        if (record?.isEnded == true) return State.ENDED
        val end = record?.endDateEpochMs
        if (end != null) {
            return when {
                currentEpochMs > end + ONE_DAY_MS -> State.ENDED_SUGGESTED
                currentEpochMs > end - ONE_DAY_MS -> State.ENDING_SOON
                else -> State.ACTIVE
            }
        }
        if (lastActivityEpochMs != null && lastActivityEpochMs > 0L &&
            currentEpochMs - lastActivityEpochMs > QUIET_DAYS_BEFORE_SUGGEST * ONE_DAY_MS
        ) {
            return State.ENDED_SUGGESTED
        }
        return State.ACTIVE
    }

    /** Inclusive calendar-ish day count between first and last activity (min 1). */
    fun tripDays(firstEpochMs: Long?, lastEpochMs: Long?): Int {
        if (firstEpochMs == null || lastEpochMs == null || lastEpochMs < firstEpochMs) return 1
        return ((lastEpochMs - firstEpochMs) / ONE_DAY_MS).toInt() + 1
    }

    /** Only organizers may wrap up / reopen (UI + ViewModel both check this). */
    fun canToggle(isOrganizer: Boolean): Boolean = isOrganizer
}
