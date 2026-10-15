package com.splitmate.app.data.guide

import com.splitmate.app.data.guide.sync.PlanManifestCodec
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One calendar day of the trip. [number] is 1-based ("Day 1") and is what `PlanManifest.days` stores. */
data class TripDay(val number: Int, val epochDay: Long) {
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)
}

/**
 * v2.4.0 rc14: the trip's day range. [explicit] is true when it comes from trip dates someone set
 * (synced `TripLifecycleRecord.startDateEpochMs/endDateEpochMs`), false when it is derived from the
 * first and last booking.
 */
data class TripDayRange(val startEpochDay: Long, val endEpochDay: Long, val explicit: Boolean) {
    val days: List<TripDay> = (startEpochDay..endEpochDay).mapIndexed { i, d -> TripDay(i + 1, d) }

    /** Day number of [epochDay] inside the range, or null when the date is outside it. */
    fun numberOf(epochDay: Long): Int? =
        if (epochDay in startEpochDay..endEpochDay) (epochDay - startEpochDay + 1).toInt() else null

    fun day(number: Int): TripDay? = days.getOrNull(number - 1)
}

/**
 * v2.4.0 rc14: pure trip-day maths (JVM-testable, no Android).
 *
 * - Explicit trip dates win. A missing or earlier end date means a one-day trip.
 * - Otherwise the range runs from the first to the last booking.
 * - Ranges are capped at [MAX_DAYS] (the plan sync stores day numbers 1..30).
 */
object TripDays {
    const val MAX_DAYS = PlanManifestCodec.MAX_DAY

    fun resolve(
        startEpochMs: Long?,
        endEpochMs: Long?,
        bookingEpochMs: List<Long>,
        zone: ZoneId = ZoneId.systemDefault()
    ): TripDayRange? {
        if (startEpochMs != null && startEpochMs > 0L) {
            val start = epochDayOf(startEpochMs, zone)
            val end = endEpochMs?.takeIf { it > 0L }?.let { epochDayOf(it, zone) }?.coerceAtLeast(start) ?: start
            return TripDayRange(start, capEnd(start, end), explicit = true)
        }
        val days = bookingEpochMs.filter { it > 0L }.map { epochDayOf(it, zone) }
        if (days.isEmpty()) return null
        val start = days.min()
        return TripDayRange(start, capEnd(start, days.max()), explicit = false)
    }

    fun epochDayOf(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().toEpochDay()

    /** Start of [epochDay] in [zone], as epoch ms (what trip dates are stored as). */
    fun startOfDayMs(epochDay: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toInstant().toEpochMilli()

    /** "15 Oct 2026": the same format the Days list already uses for its headers. */
    fun fullLabel(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(FULL)

    /** "Thu 15": compact label for the Loop's day picker. */
    fun shortLabel(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(SHORT)

    /** "15–18 Oct" / "30 Oct – 2 Nov" / "15 Oct" for the trip-dates chip. */
    fun rangeLabel(range: TripDayRange): String {
        val a = LocalDate.ofEpochDay(range.startEpochDay)
        val b = LocalDate.ofEpochDay(range.endEpochDay)
        return when {
            a == b -> a.format(DAY_MONTH)
            a.month == b.month && a.year == b.year -> "${a.dayOfMonth}–${b.format(DAY_MONTH)}"
            else -> "${a.format(DAY_MONTH)} – ${b.format(DAY_MONTH)}"
        }
    }

    private fun capEnd(start: Long, end: Long): Long = minOf(end, start + MAX_DAYS - 1)

    private val FULL = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)
    private val SHORT = DateTimeFormatter.ofPattern("EEE d", Locale.US)
    private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.US)
}
