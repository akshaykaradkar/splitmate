package com.splitmate.app.data

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Pure (no Android dependencies) parser for the date / time strings that appear on train and
 * flight tickets, PNR snapshots and SplitMate travel expense titles.
 *
 * Design rules (v2.3.5 audit H6 / M5):
 * - Every pattern is a **full match** of the cleaned token (java.time `parse(CharSequence)` rejects
 *   trailing text), so `2026-10-03 17:25` is never mis-read as midnight by a prefix match.
 * - Case-insensitive, [Locale.ENGLISH] month / weekday names, [ResolverStyle.STRICT] calendar checks.
 * - Wall-clock values are interpreted in [IST] (`Asia/Kolkata`) unless the text carries an offset.
 * - Date-only values resolve to the start of that day in the zone with `hasTime = false`; callers
 *   that need "journey over" semantics should treat them as the end of the day.
 * - Year-less values (`14 Nov, 19:20`) take the year of `referenceEpochMs`; if the result is more
 *   than 180 days before the reference it rolls forward to the next year, and if it is more than
 *   180 days after the reference it rolls back to the previous year (nearest occurrence).
 * - Anything that does not parse returns `null`; nothing is ever guessed or fabricated.
 */
object TicketDateTimeParser {

    val IST: ZoneId = ZoneId.of("Asia/Kolkata")

    data class ParsedTicketInstant(val epochMs: Long, val hasTime: Boolean)

    private const val YEAR_ROLL_WINDOW_DAYS = 180L

    private fun formatter(pattern: String): DateTimeFormatter =
        DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern(pattern)
            .toFormatter(Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT)

    private val DATE_WITH_YEAR_FORMATTERS: List<DateTimeFormatter> = listOf(
        "uuuu-M-d",
        "uuuu/M/d",
        "d-M-uuuu",
        "d/M/uuuu",
        "d.M.uuuu",
        "d-MMM-uuuu",
        "d-MMMM-uuuu",
        "d MMM uuuu",
        "d MMMM uuuu",
        "d MMM, uuuu",
        "d MMMM, uuuu",
        "MMM d, uuuu",
        "MMMM d, uuuu",
        "MMM d uuuu",
        "MMMM d uuuu",
        "d-MMM-uu",
        "d MMM uu"
    ).map { formatter(it) }

    private val DATE_WITHOUT_YEAR_FORMATTERS: List<DateTimeFormatter> = listOf(
        "d MMM",
        "d MMMM",
        "d-MMM",
        "d-MMMM",
        "MMM d",
        "MMMM d"
    ).map { formatter(it) }

    private val TIME_FORMATTERS: List<DateTimeFormatter> = listOf(
        "H:mm",
        "H:mm:ss",
        "h:mm a",
        "h:mm:ss a",
        "h a"
    ).map { formatter(it) }

    private val EPOCH_MILLIS_REGEX = Regex("""^\d{12,13}$""")
    private val LEADING_WEEKDAY_REGEX = Regex(
        """^(mon|monday|tue|tues|tuesday|wed|weds|wednesday|thu|thur|thurs|thursday|fri|friday|sat|saturday|sun|sunday)\.?,?\s+""",
        RegexOption.IGNORE_CASE
    )
    private val TIME_TOKEN = """(\d{1,2}(?::\d{2}){1,2}(?:\s*[AP]M)?|\d{1,2}\s*[AP]M)"""
    private val TRAILING_TIME_REGEX = Regex("""^(.*?)[\s,]*(?:at\s+)?$TIME_TOKEN$""", RegexOption.IGNORE_CASE)
    private val LEADING_TIME_REGEX = Regex("""^$TIME_TOKEN[\s,]+(.+)$""", RegexOption.IGNORE_CASE)

    /** Parses [text] into an epoch instant, or `null` when it is not a recognisable ticket date/time. */
    fun parse(text: String?, referenceEpochMs: Long, zone: ZoneId = IST): ParsedTicketInstant? =
        parseDetailed(text, referenceEpochMs, zone)?.let { ParsedTicketInstant(it.epochMs, it.hasTime) }

    /** Internal detail: also reports whether the text carried a calendar date (vs. a bare time). */
    internal data class Detailed(val epochMs: Long, val hasTime: Boolean, val hasDate: Boolean)

    internal fun parseDetailed(text: String?, referenceEpochMs: Long, zone: ZoneId = IST): Detailed? {
        val cleaned = clean(text) ?: return null
        return runCatching { parseCleaned(cleaned, referenceEpochMs, zone) }.getOrNull()
    }

    private fun clean(text: String?): String? {
        if (text == null) return null
        var s = text
            .replace('\u2022', ' ') // bullet
            .replace('\u00B7', ' ') // middle dot
            .replace('|', ' ')
            .replace('\u00A0', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trimEnd(',', ';')
            .trim()
        if (s.isEmpty()) return null
        // Normalise suffixes / spellings that java.time does not know
        s = s.replace(Regex("""\s*\b(hrs|hr|ist)\.?$""", RegexOption.IGNORE_CASE), "").trim()
        s = s.replace(Regex("""\bsept\b""", RegexOption.IGNORE_CASE), "Sep")
        s = s.replace(Regex("""(\d)(st|nd|rd|th)\b""", RegexOption.IGNORE_CASE), "$1")
        s = s.replace(Regex("""\b([ap])\.\s?m\.?""", RegexOption.IGNORE_CASE), "$1m")
        s = s.replace(Regex("""(\d)\s*([ap]m)\b""", RegexOption.IGNORE_CASE)) { m ->
            "${m.groupValues[1]} ${m.groupValues[2].uppercase(Locale.ENGLISH)}"
        }
        return s.ifEmpty { null }
    }

    private fun parseCleaned(s: String, referenceEpochMs: Long, zone: ZoneId): Detailed? {
        if (EPOCH_MILLIS_REGEX.matches(s)) {
            val ms = s.toLongOrNull() ?: return null
            return Detailed(ms, hasTime = true, hasDate = true)
        }
        parseIso(s, zone)?.let { return it }

        val withoutWeekday = s.replace(LEADING_WEEKDAY_REGEX, "").trim()

        // Date only
        parseDate(withoutWeekday, referenceEpochMs, zone)?.let { date ->
            return Detailed(date.atStartOfDay(zone).toInstant().toEpochMilli(), hasTime = false, hasDate = true)
        }

        // Date + trailing time, e.g. "14 Nov, 19:20", "03 Oct 2026 5:25 PM", "2026-10-03 17:25"
        TRAILING_TIME_REGEX.find(withoutWeekday)?.let { m ->
            val datePart = m.groupValues[1].trim().trimEnd(',').trim()
            val time = parseTime(m.groupValues[2].trim())
            if (time != null) {
                if (datePart.isEmpty()) {
                    val refDate = Instant.ofEpochMilli(referenceEpochMs).atZone(zone).toLocalDate()
                    return Detailed(toEpoch(refDate, time, zone), hasTime = true, hasDate = false)
                }
                val date = parseDate(datePart.replace(LEADING_WEEKDAY_REGEX, "").trim(), referenceEpochMs, zone)
                if (date != null) return Detailed(toEpoch(date, time, zone), hasTime = true, hasDate = true)
            }
        }

        // Leading time + date, e.g. "17:25, 03 Oct 2026"
        LEADING_TIME_REGEX.find(withoutWeekday)?.let { m ->
            val time = parseTime(m.groupValues[1].trim())
            val datePart = m.groupValues[2].trim().replace(LEADING_WEEKDAY_REGEX, "").trim()
            val date = parseDate(datePart, referenceEpochMs, zone)
            if (time != null && date != null) return Detailed(toEpoch(date, time, zone), hasTime = true, hasDate = true)
        }
        return null
    }

    private fun toEpoch(date: LocalDate, time: LocalTime, zone: ZoneId): Long =
        LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()

    private fun parseIso(s: String, zone: ZoneId): Detailed? {
        if (!s.firstOrNull().let { it != null && it.isDigit() } || s.length < 10 || s[4] != '-') return null
        runCatching { ZonedDateTime.parse(s, DateTimeFormatter.ISO_ZONED_DATE_TIME) }.getOrNull()?.let {
            return Detailed(it.toInstant().toEpochMilli(), hasTime = true, hasDate = true)
        }
        runCatching { OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME) }.getOrNull()?.let {
            return Detailed(it.toInstant().toEpochMilli(), hasTime = true, hasDate = true)
        }
        runCatching { Instant.parse(s) }.getOrNull()?.let {
            return Detailed(it.toEpochMilli(), hasTime = true, hasDate = true)
        }
        runCatching { LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }.getOrNull()?.let {
            return Detailed(it.atZone(zone).toInstant().toEpochMilli(), hasTime = true, hasDate = true)
        }
        return null
    }

    private fun parseTime(s: String): LocalTime? {
        for (f in TIME_FORMATTERS) {
            runCatching { LocalTime.parse(s, f) }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun parseDate(s: String, referenceEpochMs: Long, zone: ZoneId): LocalDate? {
        if (s.isEmpty()) return null
        for (f in DATE_WITH_YEAR_FORMATTERS) {
            runCatching { LocalDate.parse(s, f) }.getOrNull()?.let { return it }
        }
        for (f in DATE_WITHOUT_YEAR_FORMATTERS) {
            val accessor = runCatching { f.parse(s) }.getOrNull() ?: continue
            val month = runCatching { accessor.get(ChronoField.MONTH_OF_YEAR) }.getOrNull() ?: continue
            val day = runCatching { accessor.get(ChronoField.DAY_OF_MONTH) }.getOrNull() ?: continue
            return resolveYearLess(month, day, referenceEpochMs, zone)
        }
        return null
    }

    private fun resolveYearLess(month: Int, day: Int, referenceEpochMs: Long, zone: ZoneId): LocalDate? {
        val refDate = Instant.ofEpochMilli(referenceEpochMs).atZone(zone).toLocalDate()
        val refYear = refDate.year
        val candidate = runCatching { LocalDate.of(refYear, month, day) }.getOrNull()
            ?: return listOf(refYear + 1, refYear - 1, refYear + 2, refYear + 3)
                .firstNotNullOfOrNull { y -> runCatching { LocalDate.of(y, month, day) }.getOrNull() }
        val deltaDays = ChronoUnit.DAYS.between(refDate, candidate)
        return when {
            deltaDays < -YEAR_ROLL_WINDOW_DAYS -> runCatching { LocalDate.of(refYear + 1, month, day) }.getOrNull()
            deltaDays > YEAR_ROLL_WINDOW_DAYS -> runCatching { LocalDate.of(refYear - 1, month, day) }.getOrNull()
            else -> candidate
        }
    }

    private val CLOCK_DURATION_REGEX = Regex("""^(\d{1,3}):([0-5]\d)$""")
    private val UNIT_DURATION_REGEX = Regex(
        """^(?:(\d{1,3})\s*(?:d|day|days)\s*)?(?:(\d{1,3})\s*(?:h|hr|hrs|hour|hours)\s*)?(?:(\d{1,4})\s*(?:m|min|mins|minute|minutes)\s*)?$"""
    )

    /**
     * Parses a journey duration such as `2h 35m`, `2 hr 35 min`, `02:35`, `14h`, `45m` or ISO `PT2H35M`
     * into milliseconds. Returns `null` for blank / unrecognised / zero durations.
     */
    fun parseDurationMs(text: String?): Long? {
        val s = text?.trim()?.lowercase(Locale.ENGLISH)
            ?.replace(",", " ")
            ?.replace(Regex("""\band\b"""), " ")
            ?.replace(Regex("""\s+"""), " ")
            ?.trim()
            ?: return null
        if (s.isEmpty()) return null
        if (s.startsWith("p")) {
            return runCatching { Duration.parse(s.uppercase(Locale.ENGLISH)).toMillis() }
                .getOrNull()?.takeIf { it > 0L }
        }
        CLOCK_DURATION_REGEX.matchEntire(s)?.let { m ->
            val minutes = m.groupValues[1].toLong() * 60L + m.groupValues[2].toLong()
            return (minutes * 60_000L).takeIf { it > 0L }
        }
        val m = UNIT_DURATION_REGEX.matchEntire(s) ?: return null
        val (d, h, min) = Triple(m.groupValues[1], m.groupValues[2], m.groupValues[3])
        if (d.isEmpty() && h.isEmpty() && min.isEmpty()) return null
        val totalMinutes = (d.toLongOrNull() ?: 0L) * 24L * 60L +
            (h.toLongOrNull() ?: 0L) * 60L +
            (min.toLongOrNull() ?: 0L)
        return (totalMinutes * 60_000L).takeIf { it > 0L }
    }
}
