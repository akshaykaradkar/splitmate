package com.splitmate.app.ui

import android.content.Context
import com.splitmate.app.data.PnrNetworkRepository
import java.util.Locale
import kotlin.math.abs

data class ParsedTravelTicket(
    val pnr: String = "",
    val trainOrFlightNo: String = "",
    val trainOrCarrierName: String = "",
    val fromStation: String = "",
    val toStation: String = "",
    val departureDate: String = "",
    val departureTime: String = "",
    val coachAndSeats: String = "",
    val bookingStatus: String = "", // CNF, WL, RAC, UNVERIFIED, or "" when unknown
    val chartStatus: String = "",
    val liveTrainRadar: String = "",
    val fareRupees: String = "",
    val cleanTitle: String = "",
    /**
     * `true` only when [bookingStatus] came from a real source (an explicit `Status:` segment in a
     * reviewed expense title, a live response, or the user's confirmed review).
     */
    val statusExplicit: Boolean = false
) {
    val hasTicketMetadata: Boolean
        get() = pnr.isNotBlank() || trainOrFlightNo.isNotBlank() || coachAndSeats.isNotBlank() || (fromStation.isNotBlank() && toStation.isNotBlank())

    val route: String
        get() = if (fromStation.isNotBlank() || toStation.isNotBlank()) "${fromStation}→${toStation}" else ""

    val departureInfo: String
        get() = listOf(departureDate, departureTime).filter { it.isNotBlank() }.joinToString(" ")
}

data class LivePnrPassenger(
    val passengerNumber: String,
    val initialStatus: String,
    val currentStatus: String,
    val statusLabel: String
)

private val AcIndianRailClasses = setOf(
    "1A", "2A", "3A", "3E", "CC", "EC", "EA", "FC", "1AC", "2AC", "3AC", "HA", "HB"
)

data class LivePnrStatusSnapshot(
    val pnr: String,
    val trainNo: String,
    val trainName: String,
    val fromStation: String,
    val toStation: String,
    val departureTime: String,
    val travelClass: String = "",
    val totalFareRupees: Int = 0,
    val passengerCount: Int = 1,
    val bookingStatusBadge: String, // "CNF", "WL", "RAC", or "MANUAL"
    val chartPrepared: Boolean,
    val passengerStatuses: List<String>,
    val structuredPassengers: List<LivePnrPassenger> = emptyList(),
    val fromStationName: String = "",
    val toStationName: String = "",
    val arrivalTime: String = "",
    val durationText: String = "",
    val quotaText: String = "",
    val coachPositionHint: String,
    val liveTrainLocationRadar: String,
    val confirmationProbability: String,
    val sourceLabel: String,
    val isLiveVerified: Boolean = true,
    val isManualEntry: Boolean = false,
    val baseFarePaiseOverride: Long? = null,
    val totalPayablePaiseOverride: Long? = null,
    val irctcPaymentMode: String = "UPI",
    val includeInsurance: Boolean? = null,
    val departureDate: String = "",
    val arrivalDate: String = "",
    val platformNumber: String = "",
    val fetchedAtEpochMs: Long = 0L,
    val upstreamCacheEpochMs: Long = 0L,
    val isInvalidOrFlushedPnr: Boolean = false,
    val cooldownRemainingSeconds: Int = 0
) {
    /**
     * Official Indian Railways / IRCTC Tariff Classification:
     * Explicit AC classes ("1A", "2A", "3A", "3E", "CC", "EC", "EA", "FC") are charged
     * ₹20+18% GST (₹23.60 UPI) / ₹30+18% GST (₹35.40 Card).
     * Non-AC classes ("SL", "2S", "II", "GN", "UR") or unspecified classes are never overcharged AC rates.
     */
    val isAcClass: Boolean
        get() = travelClass.trim().uppercase(Locale.US) in AcIndianRailClasses

    val effectivePassengerCount: Int
        get() = passengerCount.coerceAtLeast(
            structuredPassengers.size.coerceAtLeast(passengerStatuses.size.coerceAtLeast(1))
        )

    /**
     * Pure integer-paise base ticket fare (before IRCTC convenience fee & optional travel insurance).
     * Never overwritten by post-fee payable totals so re-entry never compounds +₹25.00.
     */
    val baseFarePaise: Long
        get() = baseFarePaiseOverride ?: (totalFareRupees.toLong() * 100L)

    /**
     * IRCTC does not levy optional travel insurance (₹0.45/pax) on purely waitlisted bookings at time of booking.
     */
    val isPurelyWaitlistedAtBooking: Boolean
        get() {
            if (structuredPassengers.isNotEmpty()) {
                return structuredPassengers.all { pax ->
                    val statusRef = pax.initialStatus.ifBlank { pax.currentStatus }.uppercase(Locale.US)
                    statusRef.contains("WL") && !statusRef.contains("CNF") && !statusRef.contains("RAC")
                }
            }
            if (passengerStatuses.isNotEmpty()) {
                return passengerStatuses.all { entry ->
                    val upper = entry.uppercase(Locale.US)
                    upper.contains("WL") && !upper.contains("CNF") && !upper.contains("RAC")
                }
            }
            val badgeUpper = bookingStatusBadge.uppercase(Locale.US)
            return badgeUpper.contains("WL") && !badgeUpper.contains("CNF") && !badgeUpper.contains("RAC")
        }

    val defaultIncludeInsurance: Boolean
        get() = includeInsurance ?: !isPurelyWaitlistedAtBooking

    // IRCTC Convenience Fee per PNR (inclusive of 18% GST)
    val irctcConvenienceFeeUpiPaise: Long
        get() = if (baseFarePaise > 0L) (if (isAcClass) 2360L else 1180L) else 0L

    val irctcConvenienceFeeCardPaise: Long
        get() = if (baseFarePaise > 0L) (if (isAcClass) 3540L else 1770L) else 0L

    // IRCTC Optional Travel Insurance Premium: ₹0.45 (45 paise) per passenger (inclusive of 18% GST)
    val travelInsurancePaise: Long
        get() = if (baseFarePaise > 0L) effectivePassengerCount * 45L else 0L

    // Dynamic All-Inclusive IRCTC Totals in Paise
    val allInclusiveUpiPaise: Long
        get() = baseFarePaise + irctcConvenienceFeeUpiPaise + travelInsurancePaise

    val allInclusiveCardPaise: Long
        get() = baseFarePaise + irctcConvenienceFeeCardPaise + travelInsurancePaise

    val effectivePayablePaise: Long
        get() = totalPayablePaiseOverride
            ?: computeCustomTotalPaise(paymentMode = irctcPaymentMode, includeInsurance = defaultIncludeInsurance)

    fun computeCustomTotalPaise(
        paymentMode: String = irctcPaymentMode,
        includeInsurance: Boolean = defaultIncludeInsurance
    ): Long {
        if (baseFarePaise <= 0L) return 0L
        val convFee = when (paymentMode.trim().uppercase(Locale.US)) {
            "UPI" -> irctcConvenienceFeeUpiPaise
            "CARD" -> irctcConvenienceFeeCardPaise
            else -> 0L
        }
        val insFee = if (includeInsurance) travelInsurancePaise else 0L
        return baseFarePaise + convFee + insFee
    }

    fun formatPaiseAsDecimalRupees(paise: Long): String {
        val sign = if (paise < 0L) "-" else ""
        val absPaise = abs(paise)
        val whole = absPaise / 100L
        val rem = absPaise % 100L
        return if (rem == 0L) "$sign$whole" else String.format(Locale.US, "%s%d.%02d", sign, whole, rem)
    }
}

/**
 * Maps Indian Railways class codes accurately without conflating AC 3-Tier (3A) with Non-AC Sleeper (SL).
 */
fun formatIndianRailClassLabel(rawClass: String): String {
    val clean = rawClass.trim().uppercase(Locale.US)
    return when (clean) {
        "" -> ""
        "1A", "1AC" -> "First AC (1A)"
        "2A", "2AC" -> "AC 2-Tier (2A)"
        "3A", "3AC" -> "AC 3-Tier (3A)"
        "3E" -> "AC 3-Economy (3E)"
        "CC" -> "AC Chair Car (CC)"
        "EC" -> "Exec. Chair Car (EC)"
        "EA" -> "Exec. Anubhuti (EA)"
        "FC" -> "First Class (FC)"
        "SL" -> "Sleeper (SL)"
        "2S" -> "Second Sitting (2S)"
        "II", "GN", "UR" -> "Unreserved ($clean)"
        else -> rawClass.trim()
    }
}

/**
 * Sanitizes a user-typed decimal rupee input so it allows digits and at most ONE decimal point
 * with at most 2 fractional paise digits (preventing "2450..50" silent parse failures).
 */
fun sanitizeDecimalRupeesInput(raw: String): String {
    if (raw.isEmpty()) return ""
    val sb = StringBuilder()
    var seenDot = false
    var decimals = 0
    for (ch in raw) {
        when {
            ch.isDigit() -> {
                if (!seenDot) {
                    if (sb.length < 9) sb.append(ch)
                } else if (decimals < 2) {
                    sb.append(ch)
                    decimals++
                }
            }
            ch == '.' && !seenDot -> {
                seenDot = true
                if (sb.isEmpty()) sb.append('0')
                sb.append('.')
            }
        }
    }
    return sb.toString()
}

/**
 * Parses a decimal rupee string directly into exact Long integer paise without IEEE-754 Double drift.
 * Returns null if blank, negative, or malformed (e.g., multiple decimal points).
 */
fun parseRupeesToPaiseExact(raw: String): Long? {
    val cleaned = raw.replace("₹", "").replace(",", "").trim()
    if (cleaned.isEmpty()) return null
    if (cleaned.count { it == '.' } > 1) return null
    val match = Regex("""^(\d{1,10})(?:\.(\d{0,2}))?$""").matchEntire(cleaned) ?: return null
    val wholeRupees = match.groupValues[1].toLongOrNull() ?: return null
    val fracRaw = match.groupValues[2]
    val paisePart = when (fracRaw.length) {
        0 -> 0L
        1 -> (fracRaw[0] - '0').toLong() * 10L
        else -> fracRaw.take(2).toLongOrNull() ?: return null
    }
    return wholeRupees * 100L + paisePart
}

private val OfflineStationNames = mapOf(
    "BDTS" to "Mumbai Bandra Terminus",
    "CDG" to "Chandigarh",
    "SBC" to "KSR Bengaluru",
    "HPT" to "Hosapete (Hampi)",
    "NDLS" to "New Delhi",
    "MMCT" to "Mumbai Central",
    "CSMT" to "Mumbai CSMT",
    "LTT" to "Lokmanya Tilak",
    "MAO" to "Madgaon (Goa)",
    "PUNE" to "Pune Jn",
    "HYB" to "Hyderabad",
    "MAS" to "MGR Chennai",
    "HWH" to "Howrah Jn",
    "BSB" to "Varanasi Jn",
    "RKMP" to "Rani Kamalapati",
    "JP" to "Jaipur Jn",
    "ADI" to "Ahmedabad Jn",
    "GOI" to "Goa Airport",
    "DEL" to "Delhi Airport",
    "BOM" to "Mumbai Airport",
    "BLR" to "Bengaluru Airport"
)

fun resolveStationDisplayName(code: String): String {
    val clean = code.trim().uppercase()
    val fullName = OfflineStationNames[clean]
    return if (fullName != null) "$clean ($fullName)" else clean
}

fun loadPersistedPnrSnapshot(context: Context, pnr: String): LivePnrStatusSnapshot? =
    PnrNetworkRepository.loadPersistedPnrSnapshot(context, pnr)

suspend fun fetchLivePnrAndTrainStatus(
    pnr: String,
    fallbackTicket: ParsedTravelTicket = ParsedTravelTicket(),
    forceManualRefresh: Boolean = false,
    context: Context? = null
): LivePnrStatusSnapshot = PnrNetworkRepository.fetchLivePnrStatus(
    pnr = pnr,
    fallbackTicket = fallbackTicket,
    forceManualRefresh = forceManualRefresh,
    context = context
)

fun formatTravelExpenseTitle(baseCategory: String, ticket: ParsedTravelTicket): String {
    val cleanBase = baseCategory
        .replace("\uD83D\uDE86", "")
        .replace("\u2708\uFE0F", "")
        .replace("\u2708", "")
        .trim()
    if (!ticket.hasTicketMetadata) return cleanBase.ifBlank { "Travel Ticket" }

    val pnrTrim = ticket.pnr.trim()
    val combinedTextWithoutAirIndiaExpress = "$cleanBase ${ticket.trainOrFlightNo} ${ticket.trainOrCarrierName} ${ticket.coachAndSeats}"
        .replace("Air India Express", "Air India", ignoreCase = true)
    val hasExplicitAirlineOrFlightNo = Regex("""^(6E|AI|IX|QP|SG|UK|I5|9I|S5)[\s\-]?\d{2,4}$""", RegexOption.IGNORE_CASE).matches(ticket.trainOrFlightNo.trim()) ||
        cleanBase.contains("IndiGo", ignoreCase = true) ||
        cleanBase.contains("Air India", ignoreCase = true) ||
        cleanBase.contains("Akasa", ignoreCase = true) ||
        cleanBase.contains("SpiceJet", ignoreCase = true) ||
        cleanBase.contains("Vistara", ignoreCase = true)
    val isExplicitTrain = (pnrTrim.length == 10 && pnrTrim.all { it.isDigit() }) ||
        Regex("""^\d{5}\b""").containsMatchIn(ticket.trainOrFlightNo.trim()) ||
        (!hasExplicitAirlineOrFlightNo && Regex("""\b(train|irctc|express|rajdhani|shatabdi|vande|duronto|sleeper|berth|3a|2a|1a|3e)\b""", RegexOption.IGNORE_CASE)
            .containsMatchIn(combinedTextWithoutAirIndiaExpress))
    val isFlightTicket = !isExplicitTrain && (
        (pnrTrim.length == 6 && pnrTrim.any { it.isLetter() }) ||
            hasExplicitAirlineOrFlightNo ||
            (!cleanBase.contains("Train", ignoreCase = true) && cleanBase.contains("Flight", ignoreCase = true))
    )

    val parts = mutableListOf<String>()
    val labelPrefix = when {
        isFlightTicket && ticket.trainOrCarrierName.isNotBlank() && ticket.trainOrFlightNo.isNotBlank() ->
            "Flight ${ticket.trainOrFlightNo} ${ticket.trainOrCarrierName}"
        isFlightTicket && ticket.trainOrFlightNo.isNotBlank() ->
            "Flight ${ticket.trainOrFlightNo}"
        isFlightTicket && cleanBase.isNotBlank() ->
            cleanBase
        ticket.trainOrCarrierName.isNotBlank() && ticket.trainOrFlightNo.isNotBlank() ->
            "Train ${ticket.trainOrFlightNo} ${ticket.trainOrCarrierName}"
        ticket.trainOrFlightNo.isNotBlank() ->
            "Train ${ticket.trainOrFlightNo}"
        ticket.trainOrCarrierName.isNotBlank() ->
            ticket.trainOrCarrierName
        else -> cleanBase.ifBlank { if (isFlightTicket) "Flight Ticket" else "Train Ticket" }
    }
    parts.add(labelPrefix)
    if (ticket.pnr.isNotBlank()) parts.add("PNR: ${ticket.pnr}")
    if (ticket.bookingStatus.isNotBlank()) parts.add("Status: ${ticket.bookingStatus}")
    if (ticket.fromStation.isNotBlank() && ticket.toStation.isNotBlank()) {
        parts.add("${ticket.fromStation.uppercase()}->${ticket.toStation.uppercase()}")
    }
    if (ticket.departureTime.isNotBlank() || ticket.departureDate.isNotBlank()) {
        val dt = listOf(ticket.departureDate, ticket.departureTime).filter { it.isNotBlank() }.joinToString(" ")
        parts.add("Dep: $dt")
    }
    if (ticket.coachAndSeats.isNotBlank()) {
        parts.add("Seats: ${ticket.coachAndSeats}")
    }
    return parts.joinToString(" | ")
}

fun extractTravelTicketFromTitle(title: String): ParsedTravelTicket? {
    if (!title.contains("PNR:", ignoreCase = true) &&
        !title.contains("Seats:", ignoreCase = true) &&
        !title.contains("Train", ignoreCase = true) &&
        !title.contains("Flight", ignoreCase = true) &&
        !title.contains("->") &&
        !title.contains("→")
    ) {
        return null
    }
    val segments = title.split("|").map { it.trim() }
    var pnr = ""
    var trainNo = ""
    var trainName = ""
    var fromSt = ""
    var toSt = ""
    var dep = ""
    var seats = ""
    var status = ""
    var isExplicitStatus = false

    segments.forEachIndexed { idx, seg ->
        when {
            seg.startsWith("PNR:", ignoreCase = true) -> pnr = seg.substringAfter(":").trim()
            seg.startsWith("Status:", ignoreCase = true) -> {
                status = seg.substringAfter(":").trim()
                isExplicitStatus = status.isNotBlank()
            }
            seg.startsWith("Dep:", ignoreCase = true) -> dep = seg.substringAfter(":").trim()
            seg.startsWith("Seats:", ignoreCase = true) || seg.startsWith("Coach", ignoreCase = true) ->
                seats = seg.substringAfter(":").trim()
            seg.contains("->") -> {
                fromSt = seg.substringBefore("->").trim()
                toSt = seg.substringAfter("->").trim()
            }
            seg.contains("→") -> {
                fromSt = seg.substringBefore("→").trim()
                toSt = seg.substringAfter("→").trim()
            }
            idx == 0 -> {
                val cleanFirst = seg
                    .replace("\uD83D\uDE86", "")
                    .replace("\u2708\uFE0F", "")
                    .replace("\u2708", "")
                    .replace("Train/Flight", "")
                    .replace("Flight", "")
                    .replace("Train", "")
                    .trim()
                val numMatch = Regex("""^(\d{5}|[A-Z0-9]{2}[\s\-]?\d{2,4})\s*(.*)$""", RegexOption.IGNORE_CASE).find(cleanFirst)
                if (numMatch != null) {
                    trainNo = numMatch.groupValues[1].trim()
                    trainName = numMatch.groupValues[2].trim()
                } else {
                    trainName = cleanFirst
                }
            }
        }
    }
    if (status.isBlank() || (!isExplicitStatus && status.equals("CNF", ignoreCase = true))) {
        when {
            seats.contains("WL", ignoreCase = true) -> status = "WL"
            seats.contains("RAC", ignoreCase = true) -> status = "RAC"
            seats.contains("CNF", ignoreCase = true) -> {
                status = "CNF"
                isExplicitStatus = true
            }
        }
    }

    val hasFlightIndicator = (pnr.length == 6 && pnr.any { it.isLetter() }) ||
        Regex("""^(6E|AI|IX|QP|SG|UK|I5|9I|S5)[\s\-]?\d{2,4}$""", RegexOption.IGNORE_CASE).matches(trainNo) ||
        Regex("""\b(indigo|air india|akasa|spicejet|vistara)\b""", RegexOption.IGNORE_CASE).containsMatchIn(title)
    val sanitizedTitleForTrainCheck = "$title $seats".replace("Air India Express", "Air India", ignoreCase = true)
    val isExplicitTrain = (pnr.length == 10 && pnr.all { it.isDigit() }) ||
        Regex("""^\d{5}\b""").containsMatchIn(trainNo) ||
        (!hasFlightIndicator && Regex("""\b(train|irctc|express|rajdhani|shatabdi|vande|duronto|sleeper|berth|3a|2a|1a|3e)\b""", RegexOption.IGNORE_CASE)
            .containsMatchIn(sanitizedTitleForTrainCheck))
    val isFlight = !isExplicitTrain && (
        hasFlightIndicator ||
            Regex("""^[A-Z0-9]{2}[\s\-]?\d{2,4}$""", RegexOption.IGNORE_CASE).matches(trainNo)
    )

    val rawFirstSegment = segments.firstOrNull()
        ?.replace("\uD83D\uDE86", "")
        ?.replace("\u2708\uFE0F", "")
        ?.replace("\u2708", "")
        ?.trim()
        .orEmpty()

    val strippedVehiclePrefix = rawFirstSegment
        .replace(Regex("""^(Train/Flight|Flight/Train)\s+""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""^Train\s+(?=\d{5}\b)""", RegexOption.IGNORE_CASE), "")
        .replace(" Express", " Exp")
        .trim()

    val normalizedCleanTitle = when {
        strippedVehiclePrefix.isNotBlank() -> strippedVehiclePrefix
        isFlight -> "Flight Ticket"
        else -> "Train Ticket"
    }

    val timeMatch = Regex("""\b([01]?\d|2[0-3]):[0-5]\d(?:\s*(?:AM|PM|hrs))?\b""", RegexOption.IGNORE_CASE).find(dep)
    val extractedDepTime = timeMatch?.value?.trim().orEmpty()
    val extractedDepDate = if (timeMatch != null) {
        dep.removeRange(timeMatch.range).replace("•", " ").replace(Regex("""\s+"""), " ").trim()
    } else {
        dep
    }

    val resolvedChartStatus = when {
        status.contains("WL", ignoreCase = true) || status.contains("RAC", ignoreCase = true) -> "Chart Not Prepared"
        status.contains("CNF", ignoreCase = true) && isExplicitStatus -> "Chart Prepared"
        else -> ""
    }

    val parsed = ParsedTravelTicket(
        pnr = pnr,
        trainOrFlightNo = trainNo,
        trainOrCarrierName = trainName,
        fromStation = fromSt,
        toStation = toSt,
        departureDate = extractedDepDate,
        departureTime = if (extractedDepDate.isNotBlank() && extractedDepTime.isNotBlank()) extractedDepTime else dep,
        coachAndSeats = seats,
        bookingStatus = status,
        chartStatus = resolvedChartStatus,
        cleanTitle = normalizedCleanTitle,
        statusExplicit = isExplicitStatus
    )
    return if (parsed.hasTicketMetadata) parsed else null
}

/**
 * Strips database concatenation prefixes like "Train/Flight " and normalizes display titles
 * e.g. "Train/Flight 11058 Amritsar - Mumbai CSMT Express (3E)" -> "11058 Amritsar - Mumbai CSMT Exp (3E)"
 */
fun cleanDisplayExpenseTitle(rawTitle: String): String {
    val parsedTicket = extractTravelTicketFromTitle(rawTitle)
    val base = parsedTicket?.cleanTitle?.takeIf { it.isNotBlank() } ?: rawTitle.substringBefore("[PNR:").trim()
    return base
        .replace("\uD83D\uDE86", "")
        .replace("\u2708\uFE0F", "")
        .replace("\u2708", "")
        .replace(Regex("""^(Train/Flight|Flight/Train)\s+""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""^Train\s+(?=\d{5}\b)""", RegexOption.IGNORE_CASE), "")
        .replace(" Express", " Exp")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

/**
 * Formats integer paise/cents using standard Indian numbering system (en-IN) -> ₹10,391.00
 */
fun formatIndianRupeesFromCents(
    cents: Long,
    includePlusSign: Boolean = false,
    currencySymbol: String = "₹",
    trimZeroDecimals: Boolean = false
): String {
    val sign = when {
        cents < 0L -> "-"
        cents > 0L && includePlusSign -> "+"
        else -> ""
    }
    val absCents = kotlin.math.abs(cents)
    val rupees = absCents / 100L
    val paise = (absCents % 100L).toInt()
    val formattedRupees = formatIndianIntegerGrouping(rupees)
    return if (trimZeroDecimals && paise == 0) {
        "$sign$currencySymbol$formattedRupees"
    } else {
        String.format(java.util.Locale.US, "%s%s%s.%02d", sign, currencySymbol, formattedRupees, paise)
    }
}

fun formatIndianIntegerGrouping(number: Long): String {
    val absNum = kotlin.math.abs(number)
    val prefix = if (number < 0) "-" else ""
    if (absNum < 1000L) return "$prefix$absNum"
    val lastThree = (absNum % 1000L).toString().padStart(3, '0')
    var remaining = absNum / 1000L
    val groups = mutableListOf<String>()
    while (remaining > 0L) {
        if (remaining >= 100L) {
            groups.add(0, (remaining % 100L).toString().padStart(2, '0'))
            remaining /= 100L
        } else {
            groups.add(0, remaining.toString())
            remaining = 0L
        }
    }
    return "$prefix${groups.joinToString(",")},$lastThree"
}

