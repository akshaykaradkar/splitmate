package com.splitmate.app

import com.splitmate.app.data.PnrNetworkRepository
import com.splitmate.app.data.TicketDateTimeParser
import com.splitmate.app.ui.ParsedTravelTicket
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class SplitMateV235PnrOfflineVaultTest {

    @Test
    fun `test TicketDateTimeParser formats`() {
        // We test with a reference epoch of 2026-10-04T12:00:00 IST
        val refMs = ZonedDateTime.of(2026, 10, 4, 12, 0, 0, 0, TicketDateTimeParser.IST).toInstant().toEpochMilli()

        // ISO format with time
        val p1 = TicketDateTimeParser.parse("2026-10-03 17:25", refMs)
        assertNotNull(p1)
        assertEquals(true, p1!!.hasTime)
        assertEquals(ZonedDateTime.of(2026, 10, 3, 17, 25, 0, 0, TicketDateTimeParser.IST).toInstant().toEpochMilli(), p1.epochMs)

        // dd-MM-yyyy format
        val p2 = TicketDateTimeParser.parse("03-10-2026", refMs)
        assertNotNull(p2)
        assertEquals(false, p2!!.hasTime)
        assertEquals(ZonedDateTime.of(2026, 10, 3, 0, 0, 0, 0, TicketDateTimeParser.IST).toInstant().toEpochMilli(), p2.epochMs)

        // IST vs UTC boundary e.g. 00:30 IST
        val p3 = TicketDateTimeParser.parse("04 Oct 2026 00:30", refMs)
        assertNotNull(p3)
        assertEquals(true, p3!!.hasTime)
        assertEquals(ZonedDateTime.of(2026, 10, 4, 0, 30, 0, 0, TicketDateTimeParser.IST).toInstant().toEpochMilli(), p3.epochMs)

        // Year-less Dec->Jan roll forward
        // Reference is 2026-10-04. A ticket for "05 Jan" should be assumed 2027-01-05 since it's within 180 days in the future.
        val p4 = TicketDateTimeParser.parse("05 Jan", refMs)
        assertNotNull(p4)
        assertEquals(false, p4!!.hasTime)
        assertEquals(ZonedDateTime.of(2027, 1, 5, 0, 0, 0, 0, TicketDateTimeParser.IST).toInstant().toEpochMilli(), p4.epochMs)

        // Year-less past date
        // Reference 2026-10-04. A ticket for "05 May" should be assumed 2026-05-05 since it's within 180 days in the past.
        val p5 = TicketDateTimeParser.parse("05 May", refMs)
        assertNotNull(p5)
        assertEquals(ZonedDateTime.of(2026, 5, 5, 0, 0, 0, 0, TicketDateTimeParser.IST).toInstant().toEpochMilli(), p5!!.epochMs)

        // Garbage text
        assertNull(TicketDateTimeParser.parse("Not a date", refMs))
        assertNull(TicketDateTimeParser.parse("12345678", refMs)) // Not epoch ms, not valid date

        // Duration tests
        assertEquals(9300000L, TicketDateTimeParser.parseDurationMs("2h 35m")) // 2h * 3600_000 + 35m * 60_000 = 7200000 + 2100000 = 9300000
        assertEquals(9300000L, TicketDateTimeParser.parseDurationMs("2 hr 35 min"))
        assertEquals(9300000L, TicketDateTimeParser.parseDurationMs("02:35"))
        assertEquals(50400000L, TicketDateTimeParser.parseDurationMs("14h")) // 14 * 3600_000
        assertEquals(2700000L, TicketDateTimeParser.parseDurationMs("45m"))
        assertEquals(9300000L, TicketDateTimeParser.parseDurationMs("PT2H35M"))
        assertNull(TicketDateTimeParser.parseDurationMs("garbage"))
    }

    @Test
    fun `test normalizePnrKey ignores free-text titles`() {
        assertEquals("", PnrNetworkRepository.extractPnrFromFreeText("Dinner"))
        assertEquals("", PnrNetworkRepository.extractPnrFromFreeText("Lunch"))
        assertEquals("", PnrNetworkRepository.extractPnrFromFreeText("Fuel & Petrol"))
        assertEquals("", PnrNetworkRepository.extractPnrFromFreeText("Hotel & Stay"))

        // Titles that are plain words never become PNRs, even as a single 6-letter word
        assertEquals("", PnrNetworkRepository.extractPnrFromFreeText("Hostel"))
        assertEquals("", PnrNetworkRepository.extractPnrFromFreeText("Snacks"))
        // A value from a PNR field keeps letter-only airline PNRs
        assertEquals("QAZWSX", PnrNetworkRepository.normalizePnrKey("qazwsx"))
        assertEquals("QAZWSX", PnrNetworkRepository.normalizePnrKey(" QAZ-WSX "))
        assertEquals("4419025583", PnrNetworkRepository.normalizePnrKey("4419025583"))
        // Explicit airline PNR
        assertEquals("A1B2C3", PnrNetworkRepository.normalizePnrKey("PNR: A1B2C3"))
        // Implicit airline PNR (must contain a digit)
        assertEquals("AB3XYZ", PnrNetworkRepository.extractPnrFromFreeText("Flight ticket AB3XYZ"))
        // 10-digit rail PNR accepted
        assertEquals("1234567890", PnrNetworkRepository.extractPnrFromFreeText("Ticket 1234567890 for Delhi"))
    }

    @Test
    fun `test isTicketAllConfirmed`() {
        // Bare ticket not confirmed
        val bare = ParsedTravelTicket(pnr = "A1B2C3", statusExplicit = false)
        assertFalse(PnrNetworkRepository.isTicketAllConfirmed(bare))

        // WL ticket stays WL and not confirmed
        val wl = ParsedTravelTicket(pnr = "A1B2C3", bookingStatus = "WL", statusExplicit = true)
        assertFalse(PnrNetworkRepository.isTicketAllConfirmed(wl))

        // Explicit CNF confirmed
        val cnf = ParsedTravelTicket(
            pnr = "4419025583", trainOrFlightNo = "12951", fromStation = "MMCT", toStation = "NDLS",
            coachAndSeats = "B2/33", bookingStatus = "CNF", statusExplicit = true
        )
        assertTrue(PnrNetworkRepository.isTicketAllConfirmed(cnf))
    }

    @Test
    fun `test isTicketJourneyInPast`() {
        val nowMs = ZonedDateTime.of(2026, 10, 4, 12, 0, 0, 0, TicketDateTimeParser.IST).toInstant().toEpochMilli()

        // Past journey
        val past = ParsedTravelTicket(departureDate = "01 Oct 2026", departureTime = "10:00")
        assertTrue(PnrNetworkRepository.isTicketJourneyInPast(past, nowMs = nowMs))

        // Future journey
        val future = ParsedTravelTicket(departureDate = "10 Oct 2026", departureTime = "10:00")
        assertFalse(PnrNetworkRepository.isTicketJourneyInPast(future, nowMs = nowMs))
    }
}
