package com.splitmate.app.data

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * v2.3.5 (#1): pure trip lifecycle derivation (JVM-testable, no Android types).
 *
 * Decisions (binding):
 * - Only an organizer's explicit "Wrap up" ([TripLifecycleRecord.isEnded]) produces [State.ENDED].
 *   Nothing is ever ended automatically.
 * - Return-ticket evidence only SUGGESTS: once the last travel leg arrives back in the first leg's
 *   origin city ([resolveReturnArrivalEpochMs]), the trip resolves to [State.ENDED_SUGGESTED]
 *   ("Return journey completed - wrap up?", organizers only). In the final day before that arrival
 *   it resolves to [State.ENDING_SOON] ("Heading home", informational, no wrap-up CTA).
 * - Quiet trips (> [QUIET_DAYS_BEFORE_SUGGEST] days after the last activity) also resolve to
 *   [State.ENDED_SUGGESTED].
 * - An organizer "Reopen" stamps [TripLifecycleRecord.reopenedAtEpochMs]; return arrivals and
 *   travel legs before that stamp no longer produce suggestions (also handles reused groups).
 */
object TripLifecycleResolver {
    enum class State { ACTIVE, ENDING_SOON, ENDED_SUGGESTED, ENDED }

    /** Pure schedule summary of one train/flight booking in a trip. */
    data class TravelLegSchedule(
        /** Departure instant. For date-only tickets this is the start of that day (see [departureHasTime]). */
        val departureEpochMs: Long,
        /** Real arrival instant if known (may be before departure for overnight clocks; rolled over). */
        val arrivalEpochMs: Long? = null,
        val fromCode: String = "",
        val toCode: String = "",
        /** False when the ticket only had a date; the departure is then treated as the end of that day. */
        val departureHasTime: Boolean = true,
        /** Journey duration if known (used when [arrivalEpochMs] is missing). */
        val durationMs: Long? = null
    )

    const val ONE_HOUR_MS = 3_600_000L
    const val ONE_DAY_MS = 86_400_000L
    /**
     * Conservative transit allowance when neither the arrival time nor the duration is known.
     * Deliberately long: a late suggestion is harmless, an early one nags people still on board.
     */
    const val DEFAULT_RETURN_TRANSIT_MS = 12 * ONE_HOUR_MS
    /** No new expense for this long (after the last scheduled booking) -> suggest wrapping up. */
    const val QUIET_DAYS_BEFORE_SUGGEST = 3
    private const val MAX_ROLLOVER_DAYS = 2

    fun resolve(
        record: TripLifecycleRecord?,
        currentEpochMs: Long,
        /** Latest expense createdAt / scheduled time in the trip (null = no expenses). */
        lastActivityEpochMs: Long? = null,
        /** Arrival of the leg that brings the group back to its origin (null = not a round trip). */
        returnArrivalEpochMs: Long? = null
    ): State {
        if (record?.isEnded == true) return State.ENDED
        val reopenedAt = record?.reopenedAtEpochMs ?: 0L
        val returnArrival = effectiveReturnArrival(record, returnArrivalEpochMs)
        if (returnArrival != null) {
            return when {
                currentEpochMs >= returnArrival -> State.ENDED_SUGGESTED
                currentEpochMs >= returnArrival - ONE_DAY_MS -> State.ENDING_SOON
                else -> State.ACTIVE
            }
        }
        val end = record?.endDateEpochMs
        if (end != null) {
            return when {
                currentEpochMs > end + ONE_DAY_MS -> State.ENDED_SUGGESTED
                currentEpochMs > end - ONE_DAY_MS -> State.ENDING_SOON
                else -> State.ACTIVE
            }
        }
        val quietBaseline = maxOf(lastActivityEpochMs ?: 0L, reopenedAt)
        if (quietBaseline > 0L && currentEpochMs - quietBaseline > QUIET_DAYS_BEFORE_SUGGEST * ONE_DAY_MS) {
            return State.ENDED_SUGGESTED
        }
        return State.ACTIVE
    }

    /** True when the current ENDED_SUGGESTED comes from a completed return journey (vs a quiet trip). */
    fun isReturnJourneySuggestion(
        record: TripLifecycleRecord?,
        currentEpochMs: Long,
        returnArrivalEpochMs: Long?
    ): Boolean {
        if (record?.isEnded == true) return false
        val arrival = effectiveReturnArrival(record, returnArrivalEpochMs) ?: return false
        return currentEpochMs >= arrival
    }

    private fun effectiveReturnArrival(record: TripLifecycleRecord?, returnArrivalEpochMs: Long?): Long? {
        val reopenedAt = record?.reopenedAtEpochMs ?: 0L
        return returnArrivalEpochMs?.takeIf { it > 0L && it > reopenedAt }
    }

    /** Departure used for ordering / comparisons: date-only tickets count as the end of that day. */
    fun effectiveDepartureEpochMs(leg: TravelLegSchedule): Long =
        if (leg.departureHasTime) leg.departureEpochMs else leg.departureEpochMs + ONE_DAY_MS - 1L

    /**
     * Best arrival estimate for one leg: explicit arrival (rolled forward by whole days if its clock
     * is before the departure, e.g. 22:10 -> 06:05), else departure + duration, else departure +
     * [DEFAULT_RETURN_TRANSIT_MS].
     */
    fun effectiveArrivalEpochMs(leg: TravelLegSchedule): Long {
        val departure = effectiveDepartureEpochMs(leg)
        val explicit = leg.arrivalEpochMs?.takeIf { it > 0L }
        if (explicit != null) {
            var arrival = explicit
            var rolled = 0
            while (arrival < departure && rolled < MAX_ROLLOVER_DAYS) {
                arrival += ONE_DAY_MS
                rolled++
            }
            if (arrival >= departure) return arrival
        }
        val duration = leg.durationMs?.takeIf { it > 0L }
        if (duration != null) return departure + duration
        return departure + DEFAULT_RETURN_TRANSIT_MS
    }

    /**
     * Arrival time of the leg that brings the group back to where the trip started, or `null`.
     *
     * Legs are sorted by departure. The trip has returned when the LAST leg's destination
     * normalises ([TravelPlaceAliases]) to the FIRST leg's origin, and they are different legs.
     * Multi-city loops (BLR -> GOI -> BOM -> BLR) count; one-way trips never do. Two people flying out
     * on different days (BOM -> GOI twice) never count, because the last leg does not end at the origin.
     *
     * [ignoreLegsDepartingBeforeEpochMs] (the organizer's reopen stamp) drops older legs, so a reused
     * group's previous trip cannot trigger a suggestion for the new one.
     */
    fun resolveReturnArrivalEpochMs(
        legs: List<TravelLegSchedule>,
        ignoreLegsDepartingBeforeEpochMs: Long = 0L
    ): Long? {
        val valid = legs
            .filter { it.departureEpochMs > 0L && it.fromCode.isNotBlank() && it.toCode.isNotBlank() }
            .filter { effectiveDepartureEpochMs(it) >= ignoreLegsDepartingBeforeEpochMs }
            .sortedBy { effectiveDepartureEpochMs(it) }
        if (valid.size < 2) return null
        val first = valid.first()
        val last = valid.last()
        val origin = TravelPlaceAliases.canonicalCity(first.fromCode)
        // A leg that starts and ends in the same city carries no "went somewhere" signal.
        if (origin.isEmpty() || origin == TravelPlaceAliases.canonicalCity(first.toCode)) return null
        if (TravelPlaceAliases.canonicalCity(last.toCode) != origin) return null
        return effectiveArrivalEpochMs(last)
    }

    private val CLOCK_ONLY = Regex("""^\s*(\d{1,2})[:.](\d{2})\s*(AM|PM)?\s*(?:hrs?)?\s*$""", RegexOption.IGNORE_CASE)

    /**
     * Builds an arrival instant from ticket text. A bare clock ("06:05", "6:05 AM") is placed on the
     * departure's calendar day in [zone] (rollover is applied later by [effectiveArrivalEpochMs]);
     * text with a date is parsed by [TicketDateTimeParser]. Returns null when nothing usable is found.
     */
    fun arrivalFromTicketText(
        departureEpochMs: Long,
        arrivalText: String?,
        zone: ZoneId = TicketDateTimeParser.IST
    ): Long? {
        if (arrivalText.isNullOrBlank() || departureEpochMs <= 0L) return null
        val clock = CLOCK_ONLY.find(arrivalText)
        if (clock != null) {
            var hour = clock.groupValues[1].toIntOrNull() ?: return null
            val minute = clock.groupValues[2].toIntOrNull() ?: return null
            val ampm = clock.groupValues[3].uppercase()
            if (ampm == "PM" && hour < 12) hour += 12
            if (ampm == "AM" && hour == 12) hour = 0
            if (hour !in 0..23 || minute !in 0..59) return null
            val day = Instant.ofEpochMilli(departureEpochMs).atZone(zone).toLocalDate()
            return day.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()
        }
        val parsed = TicketDateTimeParser.parse(arrivalText, departureEpochMs, zone) ?: return null
        return if (parsed.hasTime) parsed.epochMs else null
    }

    /** Organizer "Wrap up": the only way to reach [State.ENDED]. LWW clock strictly advances. */
    fun buildWrapUpRecord(existing: TripLifecycleRecord?, nowEpochMs: Long, endedByPhone: String): TripLifecycleRecord =
        (existing ?: TripLifecycleRecord()).copy(
            state = TripLifecycleState.ENDED,
            endedAtEpochMs = nowEpochMs,
            endedByPhone = endedByPhone,
            updatedAtEpochMs = maxOf(nowEpochMs, (existing?.updatedAtEpochMs ?: 0L) + 1L)
        )

    /**
     * Organizer "Reopen". Works with or without an existing record (a return-ticket or quiet-trip
     * suggestion has no record), and stamps [TripLifecycleRecord.reopenedAtEpochMs] so the same
     * suggestion does not immediately reappear.
     */
    fun buildReopenRecord(existing: TripLifecycleRecord?, nowEpochMs: Long): TripLifecycleRecord {
        val clock = maxOf(nowEpochMs, (existing?.updatedAtEpochMs ?: 0L) + 1L)
        return (existing ?: TripLifecycleRecord()).copy(
            state = TripLifecycleState.ACTIVE,
            endedAtEpochMs = 0L,
            endedByPhone = "",
            updatedAtEpochMs = clock,
            reopenedAtEpochMs = clock
        )
    }

    /** Inclusive calendar-ish day count between first and last activity (min 1). */
    fun tripDays(firstEpochMs: Long?, lastEpochMs: Long?): Int {
        if (firstEpochMs == null || lastEpochMs == null || lastEpochMs < firstEpochMs) return 1
        return ((lastEpochMs - firstEpochMs) / ONE_DAY_MS).toInt() + 1
    }

    /** Only organizers may wrap up / reopen (UI + ViewModel both check this). */
    fun canToggle(isOrganizer: Boolean): Boolean = isOrganizer
}
