package com.splitmate.app.data

import com.splitmate.app.ui.LivePnrPassenger
import com.splitmate.app.ui.LivePnrStatusSnapshot
import com.splitmate.app.ui.parseRupeesToPaiseExact
import com.splitmate.app.ui.resolveStationDisplayName
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Zero-permission on-device parser for official IRCTC SMS notifications, WhatsApp PNR shares,
 * and IRCTC Electronic Reservation Slip (ERS) text payloads.
 *
 * Produces pure integer-paise [LivePnrStatusSnapshot] instances without fabricating
 * placeholder station codes, train names, or fares.
 */
object IrctcTicketTextExtractor {

    private val PnrRegex = Regex(
        """(?:\bPNR\s*(?:No\.?|Number)?\s*[:\-=]?\s*|\b)([2468]\d{9})\b""",
        RegexOption.IGNORE_CASE
    )
    private val SmsTrnRegex = Regex(
        """\b(?:TRN|TRAIN(?:\s*NO\.?)?)\s*[:\-=]?\s*(\d{5})(?:\s*[/\-–]\s*([A-Za-z][A-Za-z0-9\s\-]{2,34}))?""",
        RegexOption.IGNORE_CASE
    )
    private val SmsDojRegex = Regex(
        """\b(?:DOJ|DATE(?:\s*OF\s*JOURNEY)?)\s*[:\-=]?\s*(\d{1,2}[\-/][A-Za-z0-9]{2,3}[\-/]\d{2,4}|\d{1,2}\s+[A-Za-z]{3,9}\s+\d{2,4})""",
        RegexOption.IGNORE_CASE
    )
    private val SmsRouteDashRegex = Regex(
        """(?:,|\s|^)([A-Z]{2,5})\s*(?:->|→|-|\s+TO\s+)\s*([A-Z]{2,5})(?:,|\s|$)""",
        RegexOption.IGNORE_CASE
    )
    private val FromToExplicitRegex = Regex(
        """\bFrom\s*[:\-]?\s*(?:[A-Za-z\s]+\()?([A-Z]{2,5})\)?.*?\bTo\s*[:\-]?\s*(?:[A-Za-z\s]+\()?([A-Z]{2,5})\)?""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val DepTimeRegex = Regex(
        """\b(?:Dep(?:arture)?|Sch\.?\s*Dep)\s*[:\-=]?\s*([01]?\d|2[0-3]):([0-5]\d)\b""",
        RegexOption.IGNORE_CASE
    )
    private val ArrTimeRegex = Regex(
        """\b(?:Arr(?:ival)?|Sch\.?\s*Arr)\s*[:\-=]?\s*([01]?\d|2[0-3]):([0-5]\d)\b""",
        RegexOption.IGNORE_CASE
    )
    private val ClassTokenRegex = Regex(
        """(?:Class\s*[:\-=]?\s*|,\s*)(1A|2A|3A|3E|CC|EC|EA|FC|SL|2S)\b""",
        RegexOption.IGNORE_CASE
    )
    private val QuotaRegex = Regex(
        """\bQuota\s*[:\-=]?\s*(GN|GENERAL|TQ|TATKAL|PT|LD|SS|PQ|RL|HP)\b""",
        RegexOption.IGNORE_CASE
    )
    private val FareRegex = Regex(
        """\b(?:Total\s*(?:Ticket\s*)?Fare|Ticket\s*Fare|Base\s*Fare|Fare)\s*[:\-=]?\s*(?:Rs\.?|INR|₹)?\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val SmsPassengerTokenRegex = Regex(
        """\bP(\d+)\s*[-:]?\s*([A-Z0-9/\s]{2,22}?)(?=,|\s+Fare|\s+C\.\s*Fee|\n|$)""",
        RegexOption.IGNORE_CASE
    )
    private val ErsPassengerLineRegex = Regex(
        """Passenger\s*(\d+)\s*[:\-]?\s*(.*?)(?=Passenger\s*\d+|Total\s*Fare|Chart|$)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private val ForbiddenRouteTokens = setOf(
        "PNR", "TRN", "DOJ", "DEP", "ARR", "CNF", "RAC", "FARE", "INR", "UPI",
        "GST", "SMS", "PDF", "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN",
        "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
    )

    fun extractFromText(rawText: String, nowEpochMs: Long = System.currentTimeMillis()): LivePnrStatusSnapshot? {
        val text = rawText.trim()
        if (text.isBlank()) return null

        val pnr = PnrRegex.find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (pnr.length != 10 || !pnr.all { it.isDigit() }) return null

        val trnMatch = SmsTrnRegex.find(text)
        val trainNo = trnMatch?.groupValues?.getOrNull(1)?.trim()
            ?: Regex("""\b(1\d{4}|2\d{4}|0\d{4})\b""").find(text)?.groupValues?.getOrNull(1).orEmpty()

        val rawTrainName = trnMatch?.groupValues?.getOrNull(2)?.trim()
            ?.replace(Regex("""\s+(?:From|Class|Quota|DOJ|Dep)\b.*$""", RegexOption.IGNORE_CASE), "")
            ?.trim()
            .orEmpty()

        var fromCode = ""
        var toCode = ""
        val explicitFromTo = FromToExplicitRegex.find(text)
        if (explicitFromTo != null) {
            fromCode = explicitFromTo.groupValues[1].uppercase(Locale.US)
            toCode = explicitFromTo.groupValues[2].uppercase(Locale.US)
        } else {
            for (m in SmsRouteDashRegex.findAll(text)) {
                val c1 = m.groupValues[1].uppercase(Locale.US)
                val c2 = m.groupValues[2].uppercase(Locale.US)
                if (c1 !in ForbiddenRouteTokens && c2 !in ForbiddenRouteTokens && c1 != c2) {
                    fromCode = c1
                    toCode = c2
                    break
                }
            }
        }

        val rawDoj = SmsDojRegex.find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val formattedDoj = normalizeIndianRailDate(rawDoj)

        val depMatch = DepTimeRegex.find(text)
        val depTime = if (depMatch != null) {
            "${depMatch.groupValues[1].padStart(2, '0')}:${depMatch.groupValues[2]}"
        } else ""

        val arrMatch = ArrTimeRegex.find(text)
        val arrTime = if (arrMatch != null) {
            "${arrMatch.groupValues[1].padStart(2, '0')}:${arrMatch.groupValues[2]}"
        } else ""

        val travelClass = ClassTokenRegex.find(text)?.groupValues?.getOrNull(1)?.uppercase(Locale.US).orEmpty()
        val rawQuota = QuotaRegex.find(text)?.groupValues?.getOrNull(1)?.uppercase(Locale.US).orEmpty()
        val quota = when (rawQuota) {
            "GENERAL" -> "GN"
            "TATKAL" -> "TQ"
            else -> rawQuota
        }

        val rawFare = FareRegex.find(text)?.groupValues?.getOrNull(1).orEmpty()
        val baseFarePaise = parseRupeesToPaiseExact(rawFare) ?: 0L

        val structuredPassengers = mutableListOf<LivePnrPassenger>()
        for (m in SmsPassengerTokenRegex.findAll(text)) {
            val idx = m.groupValues[1].trim()
            val statusToken = m.groupValues[2].trim().replace(Regex("""\s+"""), " ")
            if (statusToken.isNotBlank()) {
                structuredPassengers.add(
                    buildStructuredPassenger("Passenger $idx", statusToken, statusToken)
                )
            }
        }
        if (structuredPassengers.isEmpty()) {
            for (m in ErsPassengerLineRegex.findAll(text)) {
                val idx = m.groupValues[1].trim()
                val body = m.groupValues[2].trim().replace(Regex("""\s+"""), " ")
                val bookingMatch = Regex("""Booking\s*Status\s*[:\-]?\s*([A-Za-z0-9/\s\-]+?)(?:,|Current|$)""", RegexOption.IGNORE_CASE).find(body)
                val currentMatch = Regex("""Current\s*Status\s*[:\-]?\s*([A-Za-z0-9/\s\-]+?)(?:,|$)""", RegexOption.IGNORE_CASE).find(body)
                val directStatus = Regex("""\b((?:CNF|RAC|[A-Z]*WL)[A-Za-z0-9/\s\-]{0,18})""", RegexOption.IGNORE_CASE).find(body)
                val initial = bookingMatch?.groupValues?.getOrNull(1)?.trim()
                    ?: directStatus?.groupValues?.getOrNull(1)?.trim()
                    ?: "CNF"
                val current = currentMatch?.groupValues?.getOrNull(1)?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: initial
                val resolvedCurrent = if (current.equals("CNF", ignoreCase = true) && initial.contains("/")) {
                    initial
                } else {
                    current
                }
                structuredPassengers.add(
                    buildStructuredPassenger("Passenger $idx", initial, resolvedCurrent)
                )
            }
        }

        val chartNotPrepared = text.contains("Chart Not Prepared", ignoreCase = true)
        val chartPreparedExplicit = text.contains("Chart Prepared", ignoreCase = true) && !chartNotPrepared

        val allUpperStatuses = structuredPassengers.map { it.currentStatus.uppercase(Locale.US) }
        val hasWl = allUpperStatuses.any { it.contains("WL") }
        val hasRac = allUpperStatuses.any { it.contains("RAC") }
        val hasCnf = allUpperStatuses.any { it.contains("CNF") || Regex("""^[A-Z]{1,2}\d+\s*[,/]\s*\d+""").containsMatchIn(it) }

        val badge = when {
            hasWl && hasCnf -> "PARTIAL CNF + WL"
            hasWl -> "WL"
            hasRac -> "RAC"
            structuredPassengers.isNotEmpty() -> "CNF"
            else -> ""
        }

        val passengerStatuses = structuredPassengers.mapIndexed { i, p ->
            "P${i + 1}: ${p.currentStatus}"
        }

        val combinedDepDisplay = listOf(formattedDoj, depTime).filter { it.isNotBlank() }.joinToString(" • ")

        return LivePnrStatusSnapshot(
            pnr = pnr,
            trainNo = trainNo,
            trainName = rawTrainName,
            fromStation = fromCode,
            toStation = toCode,
            departureTime = combinedDepDisplay.ifBlank { depTime },
            travelClass = travelClass,
            totalFareRupees = (baseFarePaise / 100L).toInt(),
            passengerCount = structuredPassengers.size.coerceAtLeast(1),
            bookingStatusBadge = badge,
            chartPrepared = chartPreparedExplicit || (!hasWl && !hasRac && structuredPassengers.isNotEmpty() && !chartNotPrepared && text.contains("Reservation Slip", ignoreCase = true)),
            passengerStatuses = passengerStatuses,
            structuredPassengers = structuredPassengers,
            fromStationName = if (fromCode.isNotBlank()) resolveStationDisplayName(fromCode) else "",
            toStationName = if (toCode.isNotBlank()) resolveStationDisplayName(toCode) else "",
            arrivalTime = arrTime,
            durationText = "",
            quotaText = quota,
            coachPositionHint = travelClass,
            liveTrainLocationRadar = if (fromCode.isNotBlank() && toCode.isNotBlank()) "$fromCode → $toCode" else "",
            confirmationProbability = when {
                hasWl -> "Waitlisted"
                hasRac -> "RAC"
                badge == "CNF" -> "Confirmed"
                else -> ""
            },
            sourceLabel = "IRCTC Ticket Text",
            isLiveVerified = true,
            isManualEntry = false,
            baseFarePaiseOverride = if (baseFarePaise > 0L) baseFarePaise else null,
            departureDate = formattedDoj,
            fetchedAtEpochMs = nowEpochMs
        )
    }

    internal fun normalizeIndianRailDate(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        val dmyMatch = Regex("""^(\d{1,2})[\-/](\d{1,2})[\-/](\d{2,4})$""").matchEntire(trimmed)
        if (dmyMatch != null) {
            val day = dmyMatch.groupValues[1].toIntOrNull() ?: return trimmed
            val month = dmyMatch.groupValues[2].toIntOrNull() ?: return trimmed
            val rawYear = dmyMatch.groupValues[3].toIntOrNull() ?: return trimmed
            val year = if (rawYear < 100) 2000 + rawYear else rawYear
            return runCatching {
                LocalDate.of(year, month, day).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))
            }.getOrDefault(trimmed)
        }
        return trimmed
    }

    private fun buildStructuredPassenger(label: String, initial: String, current: String): LivePnrPassenger {
        val normalizedCurrent = if (Regex("""^[A-Z]{1,2}\d+\s*[,/]\s*\d+""", RegexOption.IGNORE_CASE).containsMatchIn(current) &&
            !current.contains("CNF", ignoreCase = true) &&
            !current.contains("WL", ignoreCase = true) &&
            !current.contains("RAC", ignoreCase = true)
        ) {
            "CNF/${current.replace(",", "/").replace(" ", "")}"
        } else {
            current
        }
        val upper = normalizedCurrent.uppercase(Locale.US)
        val statusLabel = when {
            upper.contains("WL") -> "Waitlisted"
            upper.contains("RAC") -> "RAC"
            else -> "Confirmed"
        }
        return LivePnrPassenger(
            passengerNumber = label,
            initialStatus = initial,
            currentStatus = normalizedCurrent,
            statusLabel = statusLabel
        )
    }
}
