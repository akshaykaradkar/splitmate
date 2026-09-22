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
    val bookingStatus: String = "CNF", // CNF, WL, RAC
    val chartStatus: String = "Chart Prepared",
    val liveTrainRadar: String = "",
    val fareRupees: String = "",
    val cleanTitle: String = ""
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
    val quotaText: String = "GN",
    val coachPositionHint: String,
    val liveTrainLocationRadar: String,
    val confirmationProbability: String,
    val sourceLabel: String,
    val isLiveVerified: Boolean = true,
    val isManualEntry: Boolean = false
) {
    /**
     * Official Indian Railways / IRCTC Tariff Classification:
     * Non-AC classes ("SL", "2S", "II", "GN", "UR") are charged ₹10+18% GST (₹11.80 UPI) / ₹15+18% GST (₹17.70 Card).
     * AC classes ("1A", "2A", "3A", "3E", "CC", "EC", "EA", "FC") are charged ₹20+18% GST (₹23.60 UPI) / ₹30+18% GST (₹35.40 Card).
     */
    val isAcClass: Boolean
        get() = travelClass.trim().uppercase() !in setOf("SL", "2S", "II", "GN", "UR")

    val effectivePassengerCount: Int
        get() = passengerCount.coerceAtLeast(passengerStatuses.size.coerceAtLeast(1))

    val baseFarePaise: Long
        get() = totalFareRupees.toLong() * 100L

    // IRCTC Convenience Fee per PNR (inclusive of 18% GST)
    val irctcConvenienceFeeUpiPaise: Long
        get() = if (totalFareRupees > 0) (if (isAcClass) 2360L else 1180L) else 0L

    val irctcConvenienceFeeCardPaise: Long
        get() = if (totalFareRupees > 0) (if (isAcClass) 3540L else 1770L) else 0L

    // IRCTC Optional Travel Insurance Premium: ₹0.45 (45 paise) per passenger (inclusive of 18% GST)
    val travelInsurancePaise: Long
        get() = if (totalFareRupees > 0) effectivePassengerCount * 45L else 0L

    // Dynamic All-Inclusive IRCTC Totals in Paise
    val allInclusiveUpiPaise: Long
        get() = baseFarePaise + irctcConvenienceFeeUpiPaise + travelInsurancePaise

    val allInclusiveCardPaise: Long
        get() = baseFarePaise + irctcConvenienceFeeCardPaise + travelInsurancePaise

    fun computeCustomTotalPaise(paymentMode: String = "UPI", includeInsurance: Boolean = true): Long {
        if (totalFareRupees <= 0) return 0L
        val convFee = when (paymentMode.uppercase()) {
            "UPI" -> irctcConvenienceFeeUpiPaise
            "CARD" -> irctcConvenienceFeeCardPaise
            else -> 0L
        }
        val insFee = if (includeInsurance) travelInsurancePaise else 0L
        return baseFarePaise + convFee + insFee
    }

    fun formatPaiseAsDecimalRupees(paise: Long): String {
        val whole = paise / 100L
        val rem = abs(paise % 100L)
        return if (rem == 0L) "$whole" else String.format(Locale.US, "%d.%02d", whole, rem)
    }
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

fun shouldSkipAutoPnrNetworkPoll(
    context: Context,
    pnr: String,
    ticket: ParsedTravelTicket
): Boolean = PnrNetworkRepository.shouldSkipAutoPnrNetworkPoll(context, pnr, ticket)

fun recordPnrSyncTimestamp(context: Context, pnr: String) =
    PnrNetworkRepository.recordPnrSyncTimestamp(context, pnr)

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
    if (!ticket.hasTicketMetadata) return baseCategory.ifBlank { "Travel & Ticket" }
    val parts = mutableListOf<String>()
    val labelPrefix = when {
        ticket.trainOrCarrierName.isNotBlank() && ticket.trainOrFlightNo.isNotBlank() ->
            "Train ${ticket.trainOrFlightNo} ${ticket.trainOrCarrierName}"
        ticket.trainOrFlightNo.isNotBlank() -> "Train/Flight ${ticket.trainOrFlightNo}"
        ticket.trainOrCarrierName.isNotBlank() -> ticket.trainOrCarrierName
        else -> baseCategory.ifBlank { "Train / Travel Ticket" }
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
    var status = "CNF"

    segments.forEachIndexed { idx, seg ->
        when {
            seg.startsWith("PNR:", ignoreCase = true) -> pnr = seg.substringAfter(":").trim()
            seg.startsWith("Status:", ignoreCase = true) -> status = seg.substringAfter(":").trim()
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
                val cleanFirst = seg.replace("Train/Flight", "").replace("Train", "").trim()
                val numMatch = Regex("""^(\d{5}|[A-Z0-9]{2}-\d{3,4})\s*(.*)$""").find(cleanFirst)
                if (numMatch != null) {
                    trainNo = numMatch.groupValues[1]
                    trainName = numMatch.groupValues[2]
                } else {
                    trainName = cleanFirst
                }
            }
        }
    }
    if (seats.contains("WL", ignoreCase = true) && status == "CNF") status = "WL"
    if (seats.contains("RAC", ignoreCase = true) && status == "CNF") status = "RAC"

    val parsed = ParsedTravelTicket(
        pnr = pnr,
        trainOrFlightNo = trainNo,
        trainOrCarrierName = trainName,
        fromStation = fromSt,
        toStation = toSt,
        departureTime = dep,
        coachAndSeats = seats,
        bookingStatus = status,
        chartStatus = if (status.contains("WL", ignoreCase = true)) "Chart Not Prepared" else "Chart Prepared",
        cleanTitle = segments.firstOrNull()?.trim().orEmpty().ifBlank { "Train / PNR Ticket" }
    )
    return if (parsed.hasTicketMetadata) parsed else null
}
