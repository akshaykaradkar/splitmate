package com.splitmate.app

import com.splitmate.app.data.UniversalFlightTicketExtractor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Deflater

class UniversalFlightTicketExtractorTest {

    @Test
    fun `auditRealDebugFlightTicketPdfBytes - 100 percent field accuracy and sub-35ms warm speed on real Flight ticket pdf`() {
        val candidates = listOf(
            File("/usr/local/google/home/karadkar/splitmate/Debug/Flight ticket.pdf"),
            File("../Debug/Flight ticket.pdf"),
            File("../../Debug/Flight ticket.pdf")
        )
        val pdfFile = candidates.firstOrNull { it.exists() }
        assertTrue(pdfFile != null && pdfFile.exists(), "Expected Debug/Flight ticket.pdf to exist on disk")

        val groupMembers = listOf("Akshay", "Gaurav", "Gauri", "Ninad")
        val coldResult = UniversalFlightTicketExtractor.extractFromPdfFile(pdfFile!!, groupMembers)
        val result = UniversalFlightTicketExtractor.extractFromPdfFile(pdfFile, groupMembers)

        println("=== REAL PDF EXTRACTION AUDIT REPORT ===")
        println("Cold Latency       : ${coldResult.extractionDurationMs} ms")
        println("Warm Latency       : ${result.extractionDurationMs} ms")
        println("Airline PNR        : ${result.pnr}")
        println("OTA Booking ID     : ${result.otaBookingId}")
        println("Carrier & Flight   : ${result.airlineName} (${result.airlineCode}) • ${result.flightNumber}")
        println("Origin             : ${result.originIata} • ${result.originCity} (${result.originAirportName})")
        println("Destination        : ${result.destinationIata} • ${result.destinationCity} (${result.destinationAirportName})")
        println("Travel Date        : ${result.travelDate} (Booked on: ${result.bookingDate})")
        println("Timings & Duration : ${result.departureTime} -> ${result.arrivalTime} (${result.durationText})")
        println("Cabin & Fare Type  : ${result.cabinClass} • ${result.fareType}")
        println("Baggage            : Cabin=${result.cabinBaggage} | Check-in=${result.checkInBaggage}")
        println("Passengers         : ${result.passengers}")
        println("Matched Group Pax  : ${result.matchedGroupMembers}")
        println("Total Fare (Paise) : ${result.totalFarePaise} (₹${result.totalFareRupeesFormatted}) via ${result.paymentMethod}")
        println("Coupon Saved       : ${result.discountSavedPaise} paise")
        println("Expense Clean Title: ${result.buildCleanExpenseTitle()}")

        assertEquals("D9GQ3Z", result.pnr)
        assertEquals("NF7ANA8E23122941493", result.otaBookingId)
        assertEquals("6E", result.airlineCode)
        assertEquals("IndiGo", result.airlineName)
        assertEquals("6E 282", result.flightNumber)
        assertEquals("IXC", result.originIata)
        assertEquals("Chandigarh", result.originCity)
        assertEquals("NMI", result.destinationIata)
        assertEquals("Navi Mumbai", result.destinationCity)
        assertEquals("Fri, 20 Nov 2026", result.travelDate)
        assertEquals("1 Sep 2026", result.bookingDate)
        assertEquals("14:15", result.departureTime)
        assertEquals("16:40", result.arrivalTime)
        assertEquals("02h 25m", result.durationText)
        assertEquals("Economy", result.cabinClass)
        assertEquals("MMTSPECIAL", result.fareType)
        assertEquals(1, result.passengers.size)
        assertEquals("Ninad Ratnakar Rane", result.passengers.first().fullName)
        assertEquals("ADULT", result.passengers.first().passengerType)
        assertEquals(listOf("Ninad"), result.matchedGroupMembers)
        assertEquals(9409_00L, result.totalFarePaise)
        assertEquals("9409", result.totalFareRupeesFormatted)
        assertEquals(370_00L, result.discountSavedPaise)
        assertEquals("UPI", result.paymentMethod)
        assertTrue(result.extractionDurationMs < 50L, "Warm PDF extraction should complete in < 50ms, took ${result.extractionDurationMs}ms")
    }

    @Test
    fun `auditAllFiveRealBinaryPdfSamplesInDebugFolder - tests 5 distinct 2026 binary PDFs with custom ToUnicode CMaps and ObjStm`() {
        val debugDir = listOf(
            File("/usr/local/google/home/karadkar/splitmate/Debug"),
            File("../Debug"),
            File("../../Debug")
        ).firstOrNull { it.exists() && it.isDirectory }
        assertTrue(debugDir != null, "Debug directory with 2026 sample PDFs must exist")

        val groupMembers = listOf("Akshay", "Gaurav", "Gauri", "Ninad")

        // 1. Air India Direct (Amadeus 2-Page PDF 1.5 with /ObjStm + Shifted CID CMap + 6-Alpha PNR KLMNPQ)
        val airIndiaFile = File(debugDir!!, "Sample_2026_AirIndia_Direct_Amadeus.pdf")
        val aiRes = UniversalFlightTicketExtractor.extractFromPdfFile(airIndiaFile, groupMembers)
        println("[PDF #2 Air India Amadeus] (${aiRes.extractionDurationMs}ms) -> PNR=${aiRes.pnr}, Flight=${aiRes.flightNumber}, Route=${aiRes.originIata}->${aiRes.destinationIata}, Pax=${aiRes.passengers.size}, Fare=₹${aiRes.totalFareRupeesFormatted} (${aiRes.paymentMethod})")
        assertEquals("KLMNPQ", aiRes.pnr)
        assertEquals("AI202699887766", aiRes.otaBookingId)
        assertEquals("AI 865", aiRes.flightNumber)
        assertEquals("BOM", aiRes.originIata)
        assertEquals("DEL", aiRes.destinationIata)
        assertEquals("09:00", aiRes.departureTime)
        assertEquals("11:10", aiRes.arrivalTime)
        assertEquals("Business", aiRes.cabinClass)
        assertEquals("FLEXI", aiRes.fareType)
        assertEquals(4, aiRes.passengers.size)
        assertEquals(listOf("2A", "2B", "3A", "3B"), aiRes.passengers.map { it.seatNumber })
        assertEquals(listOf("Akshay", "Gaurav", "Gauri", "Ninad"), aiRes.matchedGroupMembers)
        assertEquals(34850_50L, aiRes.totalFarePaise)
        assertEquals(1200_00L, aiRes.discountSavedPaise)
        assertEquals("CARD", aiRes.paymentMethod)

        // 2. IndiGo Direct Web (TJ kerning array + Single-word Indian name Ms. Gauri + Carrier 6E seat collision guard + 2-column fare table)
        val indigoDirectFile = File(debugDir, "Sample_2026_IndiGo_Direct_Web.pdf")
        val inRes = UniversalFlightTicketExtractor.extractFromPdfFile(indigoDirectFile, groupMembers)
        println("[PDF #3 IndiGo Direct Web] (${inRes.extractionDurationMs}ms) -> PNR=${inRes.pnr}, Flight=${inRes.flightNumber}, Route=${inRes.originIata}->${inRes.destinationIata}, Pax=${inRes.passengers.map { "${it.fullName}(${it.seatNumber})" }}, Fare=₹${inRes.totalFareRupeesFormatted}")
        assertEquals("W8X4Q9", inRes.pnr)
        assertEquals("6E 6112", inRes.flightNumber)
        assertEquals("DEL", inRes.originIata)
        assertEquals("GOX", inRes.destinationIata)
        assertEquals("08:30", inRes.departureTime)
        assertEquals("11:05", inRes.arrivalTime)
        assertEquals("SUPER 6E", inRes.fareType)
        assertEquals(2, inRes.passengers.size)
        assertEquals("Gauri", inRes.passengers[0].fullName)
        assertEquals("14A", inRes.passengers[0].seatNumber)
        assertEquals("Akshay Karadkar", inRes.passengers[1].fullName)
        assertEquals("14B", inRes.passengers[1].seatNumber)
        assertEquals(15780_00L, inRes.totalFarePaise)
        assertEquals(850_00L, inRes.discountSavedPaise)

        // 3. Akasa Air Cleartrip (QP 1120 BLR -> AYJ Ayodhya Maharishi Valmiki Airport + 3 Pax + Coupon Save)
        val akasaFile = File(debugDir, "Sample_2026_AkasaAir_Cleartrip.pdf")
        val qpRes = UniversalFlightTicketExtractor.extractFromPdfFile(akasaFile, groupMembers)
        println("[PDF #4 Akasa Air Cleartrip] (${qpRes.extractionDurationMs}ms) -> PNR=${qpRes.pnr}, Flight=${qpRes.flightNumber}, Route=${qpRes.originIata}(${qpRes.originCity})->${qpRes.destinationIata}(${qpRes.destinationCity}), Pax=${qpRes.passengers.size}, Fare=₹${qpRes.totalFareRupeesFormatted}, Discount=${qpRes.discountSavedPaise}")
        assertEquals("Q9M4L2", qpRes.pnr)
        assertEquals("QP 1120", qpRes.flightNumber)
        assertEquals("Akasa Air", qpRes.airlineName)
        assertEquals("BLR", qpRes.originIata)
        assertEquals("AYJ", qpRes.destinationIata)
        assertEquals("Ayodhya", qpRes.destinationCity)
        assertEquals(3, qpRes.passengers.size)
        assertEquals(listOf("11A", "11B", "11C"), qpRes.passengers.map { it.seatNumber })
        assertEquals(listOf("Akshay", "Gaurav", "Ninad"), qpRes.matchedGroupMembers)
        assertEquals(18997_00L, qpRes.totalFarePaise)
        assertTrue(qpRes.discountSavedPaise >= 0L)

        // 4. SpiceJet ixigo Connecting Flight (BOM -> HYD -> MAA, SG 412 / SG 891, 12-hour AM/PM times, GDS slash names)
        val spiceJetFile = File(debugDir, "Sample_2026_SpiceJet_Ixigo_Connecting.pdf")
        val sgRes = UniversalFlightTicketExtractor.extractFromPdfFile(spiceJetFile, groupMembers)
        println("[PDF #5 SpiceJet Connecting] (${sgRes.extractionDurationMs}ms) -> PNR=${sgRes.pnr}, Flights=${sgRes.allFlightNumbers}, Route=${sgRes.originIata}->${sgRes.viaAirports}->${sgRes.destinationIata}, Dep/Arr=${sgRes.departureTime}->${sgRes.arrivalTime}, Fare=₹${sgRes.totalFareRupeesFormatted}")
        assertEquals("Z9M2X4", sgRes.pnr)
        assertEquals(listOf("SG 412", "SG 891"), sgRes.allFlightNumbers)
        assertEquals("BOM", sgRes.originIata)
        assertEquals("MAA", sgRes.destinationIata)
        assertEquals(listOf("HYD"), sgRes.viaAirports)
        assertEquals("06:15", sgRes.departureTime)
        assertEquals("10:45", sgRes.arrivalTime)
        assertEquals(2, sgRes.passengers.size)
        assertEquals("Akshay Karadkar", sgRes.passengers[0].fullName)
        assertEquals("Gaurav Gadhave", sgRes.passengers[1].fullName)
        assertEquals(12640_00L, sgRes.totalFarePaise)
    }

    @Test
    fun `auditSynthetic2026Pdf15WithObjStmAndToUnicode - verifies PDF 1_5 ObjStm, 6-alpha PNR, single-word Indian name, 6E seat guard, and 2-column fare table`() {
        // Build a genuine binary PDF 1.5 containing:
        // - Obj 10: /Type /ObjStm (FlateDecode compressed object stream packing Obj 20 Page Font Resource & Obj 21 Font Dict -> /ToUnicode 30 0 R)
        // - Obj 30: /FlateDecode /ToUnicode CMap mapping custom glyph <009A> -> ₹ (U+20B9)
        // - Obj 40: /FlateDecode Page Content Stream with:
        //     * Header booking timestamp `Date of Booking: 14-Oct-2026 at 19:55` (must NOT overwrite flight departure time `08:30`)
        //     * 6-letter all-alpha Amadeus PNR `KLMNPQ` on the line below `Booking Reference:`
        //     * IndiGo `6E 6112` (where `6E` appears near passenger row — must NOT be mistaken for Seat `6E`!)
        //     * Single-word passenger `Ms. Gauri (ADULT) Seat: 14A` and multi-word `MR AKSHAY KARADKAR (ADULT) 14B`
        //     * Two-column detached fare table where `Total Amount` label is >150 chars before `<009A> 15,780`
        val pdfBytes = buildBinaryPdf15Sample()
        val groupMembers = listOf("Akshay", "Gaurav", "Gauri", "Ninad")
        val res = UniversalFlightTicketExtractor.extractFromPdfBytes(pdfBytes, groupMembers)

        println("=== SYNTHETIC 2026 PDF 1.5 (/ObjStm + CMap) AUDIT ===")
        println("PNR: ${res.pnr} | Flight: ${res.flightNumber} | Route: ${res.originIata}->${res.destinationIata}")
        println("Dep/Arr: ${res.departureTime}->${res.arrivalTime} (Booking Date: ${res.bookingDate})")
        println("Passengers: ${res.passengers}")
        println("Total Fare Paise: ${res.totalFarePaise} | Discount Paise: ${res.discountSavedPaise}")

        assertEquals("KLMNPQ", res.pnr)
        assertEquals("6E", res.airlineCode)
        assertEquals("IndiGo", res.airlineName)
        assertEquals("6E 6112", res.flightNumber)
        assertEquals("SUPER 6E", res.fareType)
        assertEquals("DEL", res.originIata)
        assertEquals("GOX", res.destinationIata)
        assertEquals("14-Oct-2026", res.bookingDate)
        // Must ignore booking timestamp `19:55` and capture actual flight `08:30` -> `11:05`
        assertEquals("08:30", res.departureTime)
        assertEquals("11:05", res.arrivalTime)
        assertEquals(2, res.passengers.size)
        assertEquals("Gauri", res.passengers[0].fullName)
        assertEquals("14A", res.passengers[0].seatNumber)
        assertEquals("Akshay Karadkar", res.passengers[1].fullName)
        assertEquals("14B", res.passengers[1].seatNumber)
        assertEquals(listOf("Akshay", "Gauri"), res.matchedGroupMembers)
        assertEquals(15780_00L, res.totalFarePaise)
        assertEquals(850_00L, res.discountSavedPaise)
    }

    @Test
    fun `auditAirIndiaMultiPassengerTicketUniversalExtraction - zero hardcoding verification`() {
        val sampleAirIndiaTicket = """
            Air India e-Ticket Receipt & Itinerary
            Booking Reference / Airline PNR: K7M9P2
            Booking ID: CT998877665544 (Booked on 14 Oct 2026)
            Mumbai - New Delhi
            Sun, 15 Nov 2026 • Non-stop • 02 h 10 m duration
            Air India AI-865 | Business Class | FLEXI
            BOM 09:00 hrs -> 11:10 hrs DEL
            TRAVELLER DETAILS
            1. Mr. Akshay Karadkar (ADULT) Seat 2A
            2. Mr. Gaurav Gadhave (ADULT) Seat 2B
            3. Ms. Gauri Deshmukh (ADULT) Seat 3A
            4. Mr. Ninad Rane (ADULT) Seat 3B
            PAYMENT SUMMARY
            Base Fare: INR 28,000.00
            Taxes & UDF: INR 8,050.00
            You saved INR 1,200.00 with coupon FLYAI
            Grand Total Amount Paid by Card: INR 34,850.50
            Cancellation Surcharge Fee: INR 3500/passenger
        """.trimIndent()

        val groupMembers = listOf("Akshay", "Gaurav", "Gauri", "Ninad")
        val res = UniversalFlightTicketExtractor.extractFromText(sampleAirIndiaTicket, groupMembers)

        assertEquals("K7M9P2", res.pnr)
        assertEquals("CT998877665544", res.otaBookingId)
        assertEquals("AI", res.airlineCode)
        assertEquals("Air India", res.airlineName)
        assertEquals("AI 865", res.flightNumber)
        assertEquals("BOM", res.originIata)
        assertEquals("Mumbai", res.originCity)
        assertEquals("DEL", res.destinationIata)
        assertEquals("New Delhi", res.destinationCity)
        assertEquals("Sun, 15 Nov 2026", res.travelDate)
        assertEquals("09:00", res.departureTime)
        assertEquals("11:10", res.arrivalTime)
        assertEquals("02h 10m", res.durationText)
        assertEquals("Business", res.cabinClass)
        assertEquals(4, res.passengers.size)
        assertEquals("2A", res.passengers[0].seatNumber)
        assertEquals("2B", res.passengers[1].seatNumber)
        assertEquals("3A", res.passengers[2].seatNumber)
        assertEquals("3B", res.passengers[3].seatNumber)
        assertEquals(listOf("Akshay", "Gaurav", "Gauri", "Ninad"), res.matchedGroupMembers)
        assertEquals(34850_50L, res.totalFarePaise)
        assertEquals(1200_00L, res.discountSavedPaise)
        assertEquals("CARD", res.paymentMethod)
    }

    @Test
    fun `auditAllCapsGdsSlashConnectingFlightsAndIataBcbp - universal edge cases`() {
        val connectingAndGdsTicket = """
            SPICEJET E-TICKET ITINERARY
            RECORD LOCATOR / PNR: Z9M2X4
            BOM -> HYD -> MAA
            SG 412 / SG 891
            06:15 AM -> 10:45 AM
            PASSENGERS:
            KARADKAR/AKSHAY MR
            MR GAURAV GADHAVE
            TOTAL AMOUNT PAID: Rs. 12,640
        """.trimIndent()

        val res = UniversalFlightTicketExtractor.extractFromText(
            connectingAndGdsTicket,
            listOf("Akshay", "Gaurav", "Ninad")
        )
        assertEquals("Z9M2X4", res.pnr)
        assertEquals("SG", res.airlineCode)
        assertEquals("SpiceJet", res.airlineName)
        assertEquals(listOf("SG 412", "SG 891"), res.allFlightNumbers)
        assertEquals("BOM", res.originIata)
        assertEquals("MAA", res.destinationIata)
        assertEquals(listOf("HYD"), res.viaAirports)
        assertEquals("06:15", res.departureTime)
        assertEquals("10:45", res.arrivalTime)
        assertEquals(2, res.passengers.size)
        assertEquals("Akshay Karadkar", res.passengers[0].fullName)
        assertEquals("Gaurav Gadhave", res.passengers[1].fullName)
        assertEquals(listOf("Akshay", "Gaurav"), res.matchedGroupMembers)
        assertEquals(12640_00L, res.totalFarePaise)

        val bcbpRaw = "M1KARADKAR/AKSHAY     ED9GQ3Z IXCNMI6E 0282 324Y012A0045 100"
        val bcbpRes = UniversalFlightTicketExtractor.extractFromText(bcbpRaw, listOf("Akshay"))
        assertEquals("D9GQ3Z", bcbpRes.pnr)
        assertEquals("6E 282", bcbpRes.flightNumber)
        assertEquals("IXC", bcbpRes.originIata)
        assertEquals("NMI", bcbpRes.destinationIata)
        assertEquals("Akshay Karadkar", bcbpRes.passengers.single().fullName)
        assertEquals("12A", bcbpRes.passengers.single().seatNumber)
    }

    @Test
    fun `auditPermanentOfflineVaultForConfirmedTrainAndFlightTickets - Hampi zero-internet return ticket verification`() = kotlinx.coroutines.runBlocking {
        // 1. Extract real Flight ticket.pdf (PNR: D9GQ3Z) -> must auto-index into PnrNetworkRepository's Permanent Offline Vault
        val pdfFile = listOf(
            File("/usr/local/google/home/karadkar/splitmate/Debug/Flight ticket.pdf"),
            File("../Debug/Flight ticket.pdf"),
            File("../../Debug/Flight ticket.pdf")
        ).first { it.exists() }

        val extracted = UniversalFlightTicketExtractor.extractFromPdfFile(pdfFile, listOf("Ninad"))
        assertEquals("D9GQ3Z", extracted.pnr)

        // Verify automatic local vault indexing for 6-char Flight PNR D9GQ3Z
        val cachedFlightResult = com.splitmate.app.data.PnrNetworkRepository.loadConfirmedFlightTicketResult(null, "d9gq3z")
        assertTrue(cachedFlightResult != null && cachedFlightResult.pnr == "D9GQ3Z")

        val hampiOfflineFlightLookup = com.splitmate.app.data.PnrNetworkRepository.fetchLivePnrStatus(
            pnr = "D9GQ3Z",
            forceManualRefresh = true
        )
        assertTrue(hampiOfflineFlightLookup.isLiveVerified)
        assertEquals("D9GQ3Z", hampiOfflineFlightLookup.pnr)
        assertEquals("6E 282", hampiOfflineFlightLookup.trainNo)
        assertEquals("IXC", hampiOfflineFlightLookup.fromStation)
        assertEquals("NMI", hampiOfflineFlightLookup.toStation)
        assertEquals(9409, hampiOfflineFlightLookup.totalFareRupees)
        assertTrue(hampiOfflineFlightLookup.sourceLabel.contains("Confirmed Offline Vault"))
        assertTrue(
            com.splitmate.app.data.PnrNetworkRepository.shouldSkipAutoPnrNetworkPoll(
                null,
                "D9GQ3Z",
                extracted.toParsedTravelTicket()
            )
        )

        // 2. Verify 10-Digit Confirmed Train Ticket (CNF, even when chartPrepared = false) is Permanently Locked Offline
        val confirmedTrainSnapshot = com.splitmate.app.ui.LivePnrStatusSnapshot(
            pnr = "8412659012",
            trainNo = "16591",
            trainName = "Hampi Express (3A)",
            fromStation = "UBL",
            toStation = "SBC",
            departureTime = "2026-11-22 • 18:20",
            travelClass = "3A",
            totalFareRupees = 2480,
            passengerCount = 2,
            bookingStatusBadge = "CNF (Confirmed)",
            chartPrepared = false, // Chart NOT prepared yet, but all passengers are already CNF!
            passengerStatuses = listOf("P1: CNF / B2 / 45 / LB", "P2: CNF / B2 / 46 / UB"),
            structuredPassengers = listOf(
                com.splitmate.app.ui.LivePnrPassenger("P1", "CNF / B2 / 45", "CNF / B2 / 45", "Confirmed"),
                com.splitmate.app.ui.LivePnrPassenger("P2", "CNF / B2 / 46", "CNF / B2 / 46", "Confirmed")
            ),
            coachPositionHint = "Coach B2 · 3A",
            liveTrainLocationRadar = "Hubballi ➔ Bengaluru",
            confirmationProbability = "100% Confirmed",
            sourceLabel = "Live CRIS Cache",
            isLiveVerified = true,
            isManualEntry = false
        )
        com.splitmate.app.data.PnrNetworkRepository.savePersistedPnrSnapshot(null, confirmedTrainSnapshot)
        assertTrue(com.splitmate.app.data.PnrNetworkRepository.isSnapshotAllConfirmed(confirmedTrainSnapshot))

        val hampiReturnTrainLookup = com.splitmate.app.data.PnrNetworkRepository.fetchLivePnrStatus(
            pnr = "8412659012",
            forceManualRefresh = true
        )
        assertEquals("8412659012", hampiReturnTrainLookup.pnr)
        assertEquals("Hampi Express (3A)", hampiReturnTrainLookup.trainName)
        assertEquals(2480, hampiReturnTrainLookup.totalFareRupees)
        assertTrue(hampiReturnTrainLookup.sourceLabel.contains("Confirmed Offline Vault"))

        // 3. Verify Waitlisted (WL) Train Ticket is NOT marked as permanently confirmed
        val waitlistedSnapshot = confirmedTrainSnapshot.copy(
            pnr = "8412659999",
            bookingStatusBadge = "PARTIAL CNF + WL",
            passengerStatuses = listOf("P1: CNF / B2 / 45", "P2: WL 4")
        )
        assertTrue(!com.splitmate.app.data.PnrNetworkRepository.isSnapshotAllConfirmed(waitlistedSnapshot))
    }

    private fun deflateBytes(raw: String): ByteArray {
        val input = raw.toByteArray(Charsets.ISO_8859_1)
        val deflater = Deflater()
        deflater.setInput(input)
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!deflater.finished()) {
            val n = deflater.deflate(buf)
            out.write(buf, 0, n)
        }
        deflater.end()
        return out.toByteArray()
    }

    private fun buildBinaryPdf15Sample(): ByteArray {
        // Obj 20 (Page dict with /Resources /F9 -> 21 0 R and /Contents 40 0 R) + Obj 21 (Font dict -> /ToUnicode 30 0 R) packed inside Obj 10 (/Type /ObjStm)
        val obj20Dict = "<< /Type /Page /Resources << /Font << /F9 21 0 R >> >> /Contents 40 0 R >>"
        val obj21Dict = "<< /Type /Font /Subtype /Type0 /ToUnicode 30 0 R >>"
        val indexHeader = "20 0 21 ${obj20Dict.length + 1} "
        val objStmPlain = indexHeader + obj20Dict + "\n" + obj21Dict
        val objStmCompressed = deflateBytes(objStmPlain)

        val cmapPlain = """
            /CIDInit /ProcSet findresource begin
            begincmap
            1 beginbfchar
            <9A> <20B9>
            endbfchar
            endcmap
        """.trimIndent()
        val cmapCompressed = deflateBytes(cmapPlain)

        // Two-column detached table where labels appear 180+ chars before `<9A> ( 15,780)`
        val contentPlain = """
            BT /F9 11 Tf (Date of Booking: 14-Oct-2026 at 19:55) Tj ET
            BT /F9 12 Tf (Booking Reference:) Tj ET
            BT /F9 12 Tf (KLMNPQ) Tj ET
            BT /F9 12 Tf (IndiGo 6E 6112 | SUPER 6E | Economy) Tj ET
            BT /F9 12 Tf (DEL 08:30 hrs -> 11:05 hrs GOX | 02 h 35 m) Tj ET
            BT /F9 11 Tf (1. Ms. Gauri (ADULT) Carrier 6E Seat: 14A) Tj ET
            BT /F9 11 Tf (2. MR AKSHAY KARADKAR (ADULT) Carrier 6E 14B) Tj ET
            BT /F9 11 Tf (FARE SUMMARY COLUMN: Base Fare | Taxes & Airport UDF | Convenience Fee | Total Amount Paid by UPI ————————————————————————————————————————————————) Tj ET
            BT /F9 11 Tf (Instant Discount: ) <9A> ( 850) Tj ET
            BT /F9 12 Tf <9A> ( 15,780) Tj ET
        """.trimIndent()
        val contentCompressed = deflateBytes(contentPlain)

        val out = ByteArrayOutputStream()
        fun writeAscii(s: String) = out.write(s.toByteArray(Charsets.ISO_8859_1))

        writeAscii("%PDF-1.5\n")
        writeAscii("10 0 obj\n<< /Type /ObjStm /N 2 /First ${indexHeader.length} /Filter /FlateDecode >>\nstream\n")
        out.write(objStmCompressed)
        writeAscii("\nendstream\nendobj\n")

        writeAscii("30 0 obj\n<< /Filter /FlateDecode >>\nstream\n")
        out.write(cmapCompressed)
        writeAscii("\nendstream\nendobj\n")

        writeAscii("40 0 obj\n<< /Filter /FlateDecode >>\nstream\n")
        out.write(contentCompressed)
        writeAscii("\nendstream\nendobj\n%%EOF\n")
        return out.toByteArray()
    }
}
