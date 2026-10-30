package com.splitmate.app

import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.data.IrctcTicketTextExtractor
import com.splitmate.app.data.PnrNetworkRepository
import com.splitmate.app.data.TicketDateTimeParser
import com.splitmate.app.data.UniversalFlightTicketExtractor
import com.splitmate.app.ui.LivePnrPassenger
import com.splitmate.app.ui.LivePnrStatusSnapshot
import com.splitmate.app.ui.ParsedTravelTicket
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.screens.PnrReviewSessionController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicInteger

/**
 * SplitMate v2.4.0-rc20 Engineering & Model Benchmark Verification Suite (`G1`–`G10`):
 *
 * Verifies:
 * - G1: Live 3-tier PNR cascade parsers (ConfirmTkt v2 JSON, RailMitra SSR HTML, RailYatri SSR HTML, IRCTC SMS/ERS).
 * - G2: 5-Layer Anti-Ban Rate Limiter, Rage-Tap `60s` per-PNR cooldown, `livePnr=false` cache-first + `1/5m` live cap,
 *       `3/1m` burst & `8/10m` sustained token bucket, `15m` circuit breaker on HTTP 429/403, `hasDate` guard, and pre-chart CNF refreshability.
 * - G3: Zero paise truncation & zero `+₹25.00` IRCTC convenience fee compounding across Vault save/reload/delete,
 *       plus v1.8.5 AC class whitelist (`1A`..`FC`) and pure-WL `defaultIncludeInsurance == false`.
 * - G4: Largest-Remainder (`0.00¢` drift) per-passenger ticket shares & `~₹X.XX/person` headline caption.
 * - G5: Cross-group foreign `payerId` rejection on `editExistingExpense` preserving `sum(netBalance) == 0`.
 * - G6: Mid-edit & out-of-order PNR lookup race protection in `PnrReviewSessionController` + exact `paxCount` selection.
 * - G7: Cross-PNR manual station/fare input isolation in `PnrReviewSessionController`.
 * - G8: Cloud Sync `pickNewerTrainSnapshotJson` (`CNF` monotonicity + timestamp LWW) & group-scoped Vault PII isolation.
 * - G9: 6-char Flight PNR preservation + Vault sync on edit & IRCTC Train PDF routing (`isTrainPdfTicket == true`, real class).
 * - G10: Zero hardcoded PNRs (`8753634406`), zero `padEnd(10, '0')`, zero `"6E 204"`, zero `"3A Sleeper"`, zero `KnownIndianTrainRoutes` in `src/main`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplitMateV240Rc20PnrLiveMathAndRaceBenchmarkTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        PnrNetworkRepository.clearAllCaches(null)
        PnrNetworkRepository.httpTransportOverride = null
        PnrNetworkRepository.clockMsProvider = { System.currentTimeMillis() }
        PnrNetworkRepository.bypassCrisNightBlackoutForTest = true
        com.splitmate.app.data.CloudGroupSyncRepository.resetInMemoryStateForTests()
    }

    @AfterEach
    fun tearDown() {
        PnrNetworkRepository.httpTransportOverride = null
        PnrNetworkRepository.clockMsProvider = { System.currentTimeMillis() }
        PnrNetworkRepository.bypassCrisNightBlackoutForTest = false
        PnrNetworkRepository.clearAllCaches(null)
        Dispatchers.resetMain()
    }

    private fun resolveSrcMainDir(): File {
        val candidates = listOf(
            File("src/main"),
            File("app/src/main"),
            File("android/app/src/main")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate android/app/src/main from ${File(".").absolutePath}")
    }

    private fun testTrainSnapshot(
        pnr: String,
        trainNo: String = "12925",
        trainName: String = "PASCHIM EXPRESS",
        fromStation: String = "BDTS",
        toStation: String = "CDG",
        departureTime: String = "11:30",
        travelClass: String = "3A",
        baseFarePaise: Long = 750_000L,
        passengerCount: Int = 1,
        bookingStatusBadge: String = "CNF (Confirmed)",
        chartPrepared: Boolean = false,
        passengerStatuses: List<String> = listOf("P1: CNF/B1/12"),
        structuredPassengers: List<LivePnrPassenger> = emptyList(),
        isLiveVerified: Boolean = true,
        fetchedAtEpochMs: Long = 0L
    ): LivePnrStatusSnapshot = LivePnrStatusSnapshot(
        pnr = pnr,
        trainNo = trainNo,
        trainName = trainName,
        fromStation = fromStation,
        toStation = toStation,
        departureTime = departureTime,
        travelClass = travelClass,
        totalFareRupees = (baseFarePaise / 100L).toInt(),
        passengerCount = passengerCount,
        bookingStatusBadge = bookingStatusBadge,
        chartPrepared = chartPrepared,
        passengerStatuses = passengerStatuses,
        structuredPassengers = structuredPassengers,
        coachPositionHint = "",
        liveTrainLocationRadar = "$fromStation → $toStation",
        confirmationProbability = if (bookingStatusBadge.startsWith("CNF")) "Confirmed" else "Waitlisted",
        sourceLabel = "Test Snapshot",
        isLiveVerified = isLiveVerified,
        baseFarePaiseOverride = baseFarePaise,
        fetchedAtEpochMs = fetchedAtEpochMs
    )

    private fun testFlightResult(
        pnr: String,
        airlineCode: String,
        airlineName: String,
        flightNumber: String,
        originIata: String,
        destinationIata: String,
        totalFarePaise: Long
    ): UniversalFlightTicketExtractor.UniversalFlightTicketResult =
        UniversalFlightTicketExtractor.UniversalFlightTicketResult(
            pnr = pnr,
            otaBookingId = "PNR-$pnr",
            airlineCode = airlineCode,
            airlineName = airlineName,
            flightNumber = flightNumber,
            originIata = originIata,
            originCity = originIata,
            originAirportName = originIata,
            destinationIata = destinationIata,
            destinationCity = destinationIata,
            destinationAirportName = destinationIata,
            travelDate = "12 Nov 2026",
            bookingDate = "",
            departureTime = "09:15",
            arrivalTime = "10:30",
            durationText = "1h 15m",
            cabinClass = "Economy",
            fareType = "",
            cabinBaggage = "",
            checkInBaggage = "",
            passengers = listOf(
                UniversalFlightTicketExtractor.ExtractedFlightPassenger(
                    fullName = "Akshay",
                    seatNumber = "12A",
                    eTicketOrPnr = pnr
                )
            ),
            matchedGroupMembers = emptyList(),
            totalFarePaise = totalFarePaise,
            discountSavedPaise = 0L,
            paymentMethod = "",
            extractionDurationMs = 1L
        )

    // =========================================================================
    // GATE G1: LivePnrCascadeParserTest
    // =========================================================================
    @Test
    @DisplayName("G1: Live 3-tier PNR cascade parses ConfirmTkt v2 JSON, errorCode 201, RailMitra HTML, RailYatri HTML & IRCTC SMS")
    fun gateG1_livePnrCascadeParserTest() = runTest(testDispatcher) {
        val nowMs = ZonedDateTime.of(2026, 10, 30, 14, 0, 0, 0, TicketDateTimeParser.IST).toInstant().toEpochMilli()
        val confirmTktPayload8753634406 = """
            {
              "data": {
                "pnrResponse": {
                  "pnr": "8753634406",
                  "trainNo": "12925",
                  "trainName": "PASCHIM EXPRESS",
                  "doj": "15-11-2026",
                  "destinationDoj": "16-11-2026",
                  "boardingPoint": "BDTS",
                  "boardingStationName": "Mumbai Bandra Terminus",
                  "reservationUpto": "CDG",
                  "reservationUptoName": "Chandigarh",
                  "departureTime": "11:30",
                  "arrivalTime": "15:23",
                  "duration": "27:53",
                  "class": "3A",
                  "quota": "GN",
                  "ticketFare": "7500",
                  "chartPrepared": false,
                  "expectedPlatformNo": "3",
                  "cacheTime": "2026-10-30T13:55:00",
                  "error": null,
                  "errorCode": 0,
                  "passengerCount": 4,
                  "passengerStatus": [
                    {"number": 1, "bookingStatus": "PQWL/14", "currentStatus": "PQWL/2", "coach": "", "berth": 0, "predictionPercentage": "84"},
                    {"number": 2, "bookingStatus": "PQWL/15", "currentStatus": "PQWL/3", "coach": "", "berth": 0, "predictionPercentage": "81"},
                    {"number": 3, "bookingStatus": "PQWL/16", "currentStatus": "PQWL/4", "coach": "", "berth": 0, "predictionPercentage": "78"},
                    {"number": 4, "bookingStatus": "PQWL/17", "currentStatus": "PQWL/5", "coach": "", "berth": 0, "predictionPercentage": "75"}
                  ]
                }
              }
            }
        """.trimIndent()

        val outcome = PnrNetworkRepository.parseConfirmTktApiJson("8753634406", confirmTktPayload8753634406, nowMs)
        assertTrue(outcome is PnrNetworkRepository.ConfirmTktParseOutcome.Success)
        val snap = (outcome as PnrNetworkRepository.ConfirmTktParseOutcome.Success).snapshot
        assertEquals("12925", snap.trainNo)
        assertEquals("BDTS", snap.fromStation)
        assertEquals("CDG", snap.toStation)
        assertEquals("11:30", snap.departureTime)
        assertEquals("15:23", snap.arrivalTime)
        assertEquals("27h 53m", snap.durationText)
        assertEquals("3A", snap.travelClass)
        assertEquals("GN", snap.quotaText)
        assertEquals(750_000L, snap.baseFarePaise)
        assertEquals(4, snap.passengerCount)
        assertEquals(4, snap.structuredPassengers.size)
        assertEquals("PQWL/2", snap.structuredPassengers[0].currentStatus)
        assertEquals("PQWL/5", snap.structuredPassengers[3].currentStatus)

        // errorCode == 201 ("Flushed PNR / PNR not yet generated") must NOT trigger Tier 2/3 scrapers
        val tierCallCount = AtomicInteger(0)
        PnrNetworkRepository.clockMsProvider = { nowMs }
        PnrNetworkRepository.httpTransportOverride = PnrNetworkRepository.PnrHttpTransport { req ->
            tierCallCount.incrementAndGet()
            assertTrue(req.url.contains("confirmtkt.com"), "Invalid PNR must not hit Tier 2/3 scrapers")
            PnrNetworkRepository.PnrHttpResponse(
                statusCode = 200,
                body = """{"error":"Flushed PNR / PNR not yet generated","errorCode":201}"""
            )
        }
        val flushedSnap = PnrNetworkRepository.fetchLivePnrStatus(
            pnr = "8111111111",
            forceManualRefresh = true,
            context = null
        )
        assertTrue(flushedSnap.isInvalidOrFlushedPnr)
        assertFalse(flushedSnap.isLiveVerified)
        assertEquals(1, tierCallCount.get(), "Flushed PNR must stop at Tier 1 and enter 30m negative cache")

        // Second call within 30m hits negative cache with 0 additional HTTP calls
        val cachedFlushed = PnrNetworkRepository.fetchLivePnrStatus(
            pnr = "8111111111",
            forceManualRefresh = true,
            context = null
        )
        assertTrue(cachedFlushed.isInvalidOrFlushedPnr)
        assertEquals(1, tierCallCount.get(), "30-minute negative cache must prevent repeat upstream calls")

        // Tier 2 RailMitra SSR HTML parser
        val railMitraHtml = """
            <html><body>
              <div>12925 - PASCHIM EXPRESS  Class: 3A</div>
              <div>Mumbai Bandra Terminus (BDTS) to Chandigarh (CDG) | 15-11-2026</div>
              <div>Passenger 1 PQWL/14 CNF/B2/21</div>
              <div>Passenger 2 PQWL/15 CNF/B2/22</div>
              <div>Chart Prepared</div>
            </body></html>
        """.trimIndent()
        val rmSnap = PnrNetworkRepository.parseRailMitraHtml("8753634406", railMitraHtml, null, nowMs)
        assertNotNull(rmSnap)
        assertEquals("12925", rmSnap!!.trainNo)
        assertEquals("BDTS", rmSnap.fromStation)
        assertEquals("CDG", rmSnap.toStation)
        assertEquals(2, rmSnap.structuredPassengers.size)
        assertEquals("CNF/B2/21", rmSnap.structuredPassengers[0].currentStatus)
        assertTrue(rmSnap.chartPrepared)

        // Tier 3 RailYatri SSR HTML parser
        val railYatriHtml = """
            <div class="pnr-search-result">
              <h2>12951 - MUMBAI RAJDHANI EXPRESS</h2>
              <span>MMCT -> NDLS | 3A | Chart Not Prepared</span>
              <ul class="pasListUL">
                <li>Passenger 1 <span>B1/14</span> <span>CNF/B1/14</span></li>
                <li>Passenger 2 <span>WL 4</span> <span>RAC 2</span></li>
              </ul>
            </div>
        """.trimIndent()
        val rySnap = PnrNetworkRepository.parseRailYatriHtml("4419025583", railYatriHtml, null, nowMs)
        assertNotNull(rySnap)
        assertEquals("12951", rySnap!!.trainNo)
        assertEquals("MMCT", rySnap.fromStation)
        assertEquals("NDLS", rySnap.toStation)
        assertEquals(2, rySnap.structuredPassengers.size)

        // On-device IRCTC SMS parser (`IrctcTicketTextExtractor`)
        val smsText = "PNR:2648193052,TRAIN:12925,DOJ:15-11-2026,3A,BDTS-CDG,Dep:11:30,P1 B3 41,P2 B3 42,Fare:3760,Chg:23.60+PG"
        val smsParsed = IrctcTicketTextExtractor.extractFromText(smsText)
        assertNotNull(smsParsed)
        assertEquals("2648193052", smsParsed!!.pnr)
        assertEquals("12925", smsParsed.trainNo)
        assertEquals("BDTS", smsParsed.fromStation)
        assertEquals("CDG", smsParsed.toStation)
        assertEquals("3A", smsParsed.travelClass)
        assertEquals(376_000L, smsParsed.baseFarePaise)
        assertEquals(2, smsParsed.structuredPassengers.size)
    }

    // =========================================================================
    // GATE G2: PnrAntiBanRateLimitAndLockTest
    // =========================================================================
    @Test
    @DisplayName("G2: 5-Layer Anti-Ban Shield blocks 20x rage-taps, caps livePnr=true, trips 15m 429 breaker & guards bare time / pre-chart CNF")
    fun gateG2_pnrAntiBanRateLimitAndLockTest() = runTest(testDispatcher) {
        var fakeClockMs = ZonedDateTime.of(2026, 10, 30, 14, 0, 0, 0, TicketDateTimeParser.IST).toInstant().toEpochMilli()
        PnrNetworkRepository.clockMsProvider = { fakeClockMs }

        val networkHits = AtomicInteger(0)
        val livePnrTrueHits = AtomicInteger(0)
        val staleConfirmTktBody = """
            {
              "trainNo": "12925",
              "trainName": "PASCHIM EXPRESS",
              "boardingPoint": "BDTS",
              "reservationUpto": "CDG",
              "doj": "30-10-2026",
              "departureTime": "23:55",
              "class": "3A",
              "ticketFare": "7500",
              "chartPrepared": false,
              "cacheTime": "2026-10-30T11:00:00",
              "passengerStatus": [
                {"number": 1, "bookingStatus": "PQWL/10", "currentStatus": "PQWL/2"}
              ]
            }
        """.trimIndent()

        PnrNetworkRepository.httpTransportOverride = PnrNetworkRepository.PnrHttpTransport { req ->
            networkHits.incrementAndGet()
            if (req.url.contains("livePnr=true")) {
                livePnrTrueHits.incrementAndGet()
            }
            PnrNetworkRepository.PnrHttpResponse(statusCode = 200, body = staleConfirmTktBody)
        }

        // 1. Auto-lookup uses `livePnr=false` cache-first (never `livePnr=true` on initial auto-fetch!)
        val initial = PnrNetworkRepository.fetchLivePnrStatus(
            pnr = "8753634406",
            forceManualRefresh = false,
            context = null
        )
        assertTrue(initial.isLiveVerified)
        assertEquals(1, networkHits.get())
        assertEquals(0, livePnrTrueHits.get(), "Initial auto-lookup must strictly use livePnr=false")

        // 2. 20x continuous rapid manual refreshes ("rage-tap") within 60s dispatch ZERO network calls!
        fakeClockMs += 5_000L
        repeat(20) {
            val cooled = PnrNetworkRepository.fetchLivePnrStatus(
                pnr = "8753634406",
                forceManualRefresh = true,
                context = null
            )
            assertTrue(cooled.cooldownRemainingSeconds > 0, "Cooldown countdown must be surfaced to UI")
            assertTrue(cooled.sourceLabel.contains("Next live check in"))
        }
        assertEquals(1, networkHits.get(), "All 20 rage-tap refreshes within 60s must be absorbed with 0 HTTP calls")
        assertTrue(PnrNetworkRepository.getRemainingManualRefreshCooldownSeconds(null, "8753634406", fakeClockMs) in 50..56)

        // 3. Advance past 60s manual cooldown -> manual refresh sees stale upstream cache (>15m on travel day) and escalates ONCE to livePnr=true
        fakeClockMs += 61_000L
        val escalated = PnrNetworkRepository.fetchLivePnrStatus(
            pnr = "8753634406",
            forceManualRefresh = true,
            context = null
        )
        assertTrue(escalated.isLiveVerified)
        assertEquals(1, livePnrTrueHits.get(), "Stale upstream cache on manual refresh escalates once to livePnr=true")

        // Advance another 65s (still within 5m `FORCE_LIVE_UPSTREAM_COOLDOWN_MS`): manual refresh must NOT send `livePnr=true` again
        fakeClockMs += 65_000L
        PnrNetworkRepository.fetchLivePnrStatus(
            pnr = "8753634406",
            forceManualRefresh = true,
            context = null
        )
        assertEquals(1, livePnrTrueHits.get(), "livePnr=true must be capped at max 1 call per 5 minutes per PNR")

        // 4. Global Token Bucket (`3` calls / `1 min` burst + `8` calls / `10 min` sustained)
        PnrNetworkRepository.clearAllCaches(null)
        networkHits.set(0)
        val distinctPnrs = listOf("2111111111", "2222222222", "2333333333", "2444444444")
        val results = distinctPnrs.map { p ->
            PnrNetworkRepository.fetchLivePnrStatus(pnr = p, forceManualRefresh = true, context = null)
        }
        assertEquals(3, networkHits.get(), "Global burst limiter must cap total network calls at 3 per minute")
        assertFalse(results[3].isLiveVerified, "4th distinct PNR within 1 minute must be throttled locally")
        assertTrue(results[3].cooldownRemainingSeconds > 0)

        // Advance +2 minutes (3 more calls = 6 total in 10m), then +2 minutes (2 more calls = 8 total, 9th blocked by 8/10m cap!)
        fakeClockMs += 125_000L
        listOf("2555555555", "2666666666", "2777777777").forEach { p ->
            assertTrue(PnrNetworkRepository.fetchLivePnrStatus(pnr = p, forceManualRefresh = true, context = null).isLiveVerified)
        }
        assertEquals(6, networkHits.get())
        fakeClockMs += 125_000L
        assertTrue(PnrNetworkRepository.fetchLivePnrStatus(pnr = "2888888888", forceManualRefresh = true, context = null).isLiveVerified)
        assertTrue(PnrNetworkRepository.fetchLivePnrStatus(pnr = "2999999999", forceManualRefresh = true, context = null).isLiveVerified)
        val ninthIn10m = PnrNetworkRepository.fetchLivePnrStatus(pnr = "4111111111", forceManualRefresh = true, context = null)
        assertEquals(8, networkHits.get(), "Sustained 10-minute limiter must cap total network calls at 8 per 10 minutes")
        assertFalse(ninthIn10m.isLiveVerified, "9th distinct PNR within 10 minutes must be throttled locally")

        // 5. HTTP 429 on ConfirmTkt trips 15-minute circuit breaker and fails over seamlessly to Tier 2 (RailMitra)
        PnrNetworkRepository.clearAllCaches(null)
        val confirmTktAttempts = AtomicInteger(0)
        val railMitraAttempts = AtomicInteger(0)
        PnrNetworkRepository.httpTransportOverride = PnrNetworkRepository.PnrHttpTransport { req ->
            when {
                req.url.contains("confirmtkt.com") -> {
                    confirmTktAttempts.incrementAndGet()
                    PnrNetworkRepository.PnrHttpResponse(statusCode = 429, body = "Too Many Requests")
                }
                req.url.contains("railmitra.com") -> {
                    railMitraAttempts.incrementAndGet()
                    PnrNetworkRepository.PnrHttpResponse(
                        statusCode = 200,
                        body = "<div>12951 - RAJDHANI EXP Class: 2A</div><div>(MMCT) (NDLS)</div><div>Passenger 1 WL/3 RAC/1</div>"
                    )
                }
                else -> PnrNetworkRepository.PnrHttpResponse(statusCode = 503, body = "")
            }
        }
        val failover1 = PnrNetworkRepository.fetchLivePnrStatus("4511111111", forceManualRefresh = true, context = null)
        assertTrue(failover1.isLiveVerified)
        assertEquals(1, confirmTktAttempts.get())
        assertEquals(1, railMitraAttempts.get())

        // Next distinct PNR within 15 minutes skips ConfirmTkt completely (0 ConfirmTkt calls while breaker is open!)
        val failover2 = PnrNetworkRepository.fetchLivePnrStatus("4522222222", forceManualRefresh = true, context = null)
        assertTrue(failover2.isLiveVerified)
        assertEquals(1, confirmTktAttempts.get(), "15m circuit breaker must skip ConfirmTkt after HTTP 429")
        assertEquals(2, railMitraAttempts.get())

        // 6. R-01: Bare time `"11:25"` without explicit calendar date must NEVER be classified as past journey!
        val bareTimeTicket = ParsedTravelTicket(pnr = "8753634406", departureDate = "", departureTime = "11:25")
        val eveningTodayMs = ZonedDateTime.of(2026, 10, 30, 20, 0, 0, 0, TicketDateTimeParser.IST).toInstant().toEpochMilli()
        assertFalse(
            PnrNetworkRepository.isTicketJourneyInPast(bareTimeTicket, null, eveningTodayMs),
            "Bare departureTime '11:25' without calendar date must never freeze future tickets as 'Trip Completed'"
        )

        // 7. R-04: Pre-chart `CNF` without coach/berth (`chartPrepared = false`, `currentStatus = "CNF"`) stays refreshable!
        val preChartFirstAc = testTrainSnapshot(
            pnr = "6123456789",
            trainNo = "12951",
            trainName = "MUMBAI RAJDHANI",
            fromStation = "MMCT",
            toStation = "NDLS",
            departureTime = "17:00",
            travelClass = "1A",
            baseFarePaise = 520_000L,
            passengerCount = 1,
            bookingStatusBadge = "CNF (Confirmed)",
            chartPrepared = false,
            passengerStatuses = listOf("P1: CNF"),
            structuredPassengers = listOf(LivePnrPassenger("P1", "CNF", "CNF", "Confirmed"))
        )
        assertFalse(
            PnrNetworkRepository.isSnapshotAllConfirmed(preChartFirstAc),
            "Pre-chart 1A CNF without coach/berth must remain refreshable until chart preparation assigns cabin/coupe"
        )
    }

    // =========================================================================
    // GATE G3: PnrVaultZeroCompoundingTest (`M-01` & `M-04`)
    // =========================================================================
    @Test
    @DisplayName("G3: Vault preserves exact paise (₹7,525.40 = 752,540L) with 0 compounding + enforces AC whitelist & pure-WL insurance default")
    fun gateG3_pnrVaultZeroCompoundingAndIrctcFeeMathTest() {
        val baseAcWaitlistSnap = testTrainSnapshot(
            pnr = "8753634406",
            trainNo = "12925",
            trainName = "PASCHIM EXPRESS",
            fromStation = "BDTS",
            toStation = "CDG",
            departureTime = "11:30",
            travelClass = "3A",
            baseFarePaise = 750_000L,
            passengerCount = 4,
            bookingStatusBadge = "WL (Waitlisted)",
            chartPrepared = false,
            passengerStatuses = listOf("P1: PQWL/2", "P2: PQWL/3", "P3: PQWL/4", "P4: PQWL/5"),
            structuredPassengers = listOf(
                LivePnrPassenger("P1", "PQWL/14", "PQWL/2", "Waitlisted"),
                LivePnrPassenger("P2", "PQWL/15", "PQWL/3", "Waitlisted"),
                LivePnrPassenger("P3", "PQWL/16", "PQWL/4", "Waitlisted"),
                LivePnrPassenger("P4", "PQWL/17", "PQWL/5", "Waitlisted")
            )
        )

        // M-04: Purely waitlisted ticket defaults `defaultIncludeInsurance` to false (IRCTC does not charge insurance on pure WL)
        assertTrue(baseAcWaitlistSnap.isAcClass)
        assertTrue(baseAcWaitlistSnap.isPurelyWaitlistedAtBooking)
        assertFalse(baseAcWaitlistSnap.defaultIncludeInsurance)
        // UPI AC fee = Rs 20 + 18% GST = Rs 23.60 (2,360p) -> 750,000 + 2,360 = 752,360p when insurance is off
        assertEquals(752_360L, baseAcWaitlistSnap.effectivePayablePaise)

        // Toggle insurance ON (4 pax * 45p = 180p) -> 750,000 + 2,360 + 180 = 752,540p (₹7,525.40)
        val totalWithIns = baseAcWaitlistSnap.computeCustomTotalPaise(paymentMode = "UPI", includeInsurance = true)
        assertEquals(752_540L, totalWithIns)

        // M-04 AC Class Whitelist check: "3E" & "EA" (Anubhuti) are AC; "SL" & "2S" are Non-AC
        assertTrue(baseAcWaitlistSnap.copy(travelClass = "3E").isAcClass)
        assertTrue(baseAcWaitlistSnap.copy(travelClass = "EA").isAcClass)
        assertFalse(baseAcWaitlistSnap.copy(travelClass = "SL").isAcClass)
        assertFalse(baseAcWaitlistSnap.copy(travelClass = "2S").isAcClass)
        // Non-AC UPI fee = Rs 10 + 18% GST = Rs 11.80 (1,180p)
        assertEquals(1_180L, baseAcWaitlistSnap.copy(travelClass = "SL").irctcConvenienceFeeUpiPaise)

        // M-01: Persist to Vault JSON & L1 cache, reload multiple times -> base stays 750,000L and payable stays 752,540L (ZERO compounding!)
        val savedSnap = baseAcWaitlistSnap.copy(
            baseFarePaiseOverride = 750_000L,
            totalPayablePaiseOverride = 752_540L,
            irctcPaymentMode = "UPI",
            includeInsurance = true
        )
        val json = PnrNetworkRepository.serializeSnapshotJson("8753634406", savedSnap, 1_700_000_000_000L)
        val reloaded = PnrNetworkRepository.deserializeSnapshotJson("8753634406", json)
        assertNotNull(reloaded)
        assertEquals(750_000L, reloaded!!.baseFarePaise, "Base fare must remain 750,000p, never overwritten with payable total")
        assertEquals(752_540L, reloaded.effectivePayablePaise, "Exact 40 paise must never be truncated")
        assertEquals(752_540L, reloaded.computeCustomTotalPaise("UPI", true), "Recomputing on reload must never double-add ₹25.40")

        // Save in L1 Vault, evict on delete, and verify clean eviction
        PnrNetworkRepository.savePersistedPnrSnapshot(null, reloaded)
        assertNotNull(PnrNetworkRepository.loadPersistedPnrSnapshot(null, "8753634406"))
        PnrNetworkRepository.evictPnrFromVault(null, "8753634406")
        assertEquals(null, PnrNetworkRepository.loadPersistedPnrSnapshot(null, "8753634406"))
    }

    // =========================================================================
    // GATE G4: LargestRemainderTicketDisplayTest (`M-03`)
    // =========================================================================
    @Test
    @DisplayName("G4: Largest-Remainder 3-way split of ₹7,525.40 (752,540p) reconciles to [250,847p, 250,847p, 250,846p] with ~₹2,508.47/person caption")
    fun gateG4_largestRemainderTicketDisplayTest() {
        val totalPaise = 752_540L
        val claims = listOf(
            Triple("m1", "Akshay", 100L),
            Triple("m2", "Rohan", 100L),
            Triple("m3", "Sneha", 100L)
        )
        val split = SplitMateMathEngine.calculateProportionalReceiptSplits(
            baseSubtotalCents = 300L,
            taxCents = totalPaise - 300L,
            tipCents = 0L,
            payerId = "m1",
            memberBaseClaimsCents = claims,
            attributeRemainderToPayer = true
        )
        val shares = split.allocations.map { it.finalCents }.sortedDescending()
        assertEquals(listOf(250_847L, 250_847L, 250_846L), shares)
        assertEquals(totalPaise, shares.sum(), "0.00c drift invariant")
        assertEquals("~₹2,508.47/person", ExpenseSplitCopy.perPersonCaption(totalPaise, 3))
        assertEquals("₹2,500.00/person", ExpenseSplitCopy.perPersonCaption(750_000L, 3))
    }

    // =========================================================================
    // GATE G5: CrossGroupPayerIntegrityTest (`M-05`)
    // =========================================================================
    @Test
    @DisplayName("G5: editExistingExpense rejects foreign Group B payerId on Group A expense, keeping sum(netBalance) == 0")
    fun gateG5_crossGroupPayerIntegrityTest() = runTest(testDispatcher) {
        val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        vm.completeOnboarding("Akshay", "India", "INR", "₹", "Akshay")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.createNewGroup("Group A Goa", "INR", "Rohan")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupAId = vm.uiState.value.activeGroupId
        val membersA = vm.uiState.value.members.filter { it.groupId == groupAId }
        val akshayA = membersA.first { it.name == "Akshay" }

        vm.commitQuickEqualExpense(
            title = "Train 12925 Paschim [PNR:8753634406]",
            totalAmountCents = 400_000L,
            selectedMemberIds = membersA.map { it.memberId },
            payerMemberId = akshayA.memberId,
            explicitCategory = "TRAIN",
            explicitTravelPnr = "8753634406"
        )
        testDispatcher.scheduler.advanceUntilIdle()
        val expenseA = vm.uiState.value.expenses.first { it.groupId == groupAId }

        // Create Group B (after 800ms group-creation double-tap debounce) and obtain a foreign memberId from Group B
        Thread.sleep(820)
        vm.createNewGroup("Group B Manali", "INR", "Vikram")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupBId = vm.uiState.value.activeGroupId
        val foreignVikramB = vm.uiState.value.members.first { it.groupId == groupBId && it.name == "Vikram" }

        // Attempt to corrupt Group A's expense with Group B's foreign payerId
        vm.selectActiveGroup(groupAId)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.editExistingExpense(
            expenseId = expenseA.expenseId,
            newTitle = expenseA.title,
            newTotalRupees = 4000.0,
            newPayerId = foreignVikramB.memberId,
            selectedMemberIds = membersA.map { it.memberId },
            newTotalCentsOverride = 400_000L
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedExpenseA = vm.uiState.value.expenses.first { it.expenseId == expenseA.expenseId }
        assertTrue(
            membersA.any { it.memberId == updatedExpenseA.payerId },
            "Foreign payerId from Group B must be rejected; payerId must belong to Group A"
        )

        val splitsA = vm.uiState.value.splits.filter { it.expenseId == updatedExpenseA.expenseId }
        val netSum = membersA.sumOf { m ->
            val paid = if (updatedExpenseA.payerId == m.memberId) updatedExpenseA.totalAmountCents else 0L
            val owed = splitsA.filter { it.memberId == m.memberId }.sumOf { it.finalOwedCents }
            paid - owed
        }
        assertEquals(0L, netSum, "Group A sum(netBalance) must remain strictly 0L")
    }

    // =========================================================================
    // GATE G6: PnrMidEditAndLookupRaceTest (`C-01`, `C-02`, `M-06`)
    // =========================================================================
    @Test
    @DisplayName("G6: Slow PNR A never overwrites fast PNR B; mid-fetch user split edits survive; 2-pax ticket in 6-member group selects 2")
    fun gateG6_pnrMidEditAndLookupRaceTest() = runTest(testDispatcher) {
        val sixMembers = (1..6).map { idx ->
            GroupMemberEntity("m$idx", "g1", "Member $idx", "m$idx", isCurrentUser = idx == 1)
        }

        val controller = PnrReviewSessionController(scope = this) { cleanPnr, _ ->
            if (cleanPnr == "1111111111") {
                delay(5_000L)
                testTrainSnapshot(
                    pnr = "1111111111",
                    trainNo = "12951",
                    trainName = "SLOW TRAIN A",
                    fromStation = "MMCT",
                    toStation = "NDLS",
                    departureTime = "17:00",
                    travelClass = "2A",
                    baseFarePaise = 600_000L,
                    passengerCount = 4,
                    chartPrepared = true,
                    passengerStatuses = listOf("P1: CNF/A1/1")
                )
            } else {
                delay(50L)
                testTrainSnapshot(
                    pnr = cleanPnr,
                    trainNo = "12009",
                    trainName = "FAST SHATABDI B",
                    fromStation = "MMCT",
                    toStation = "ADI",
                    departureTime = "06:20",
                    travelClass = "CC",
                    baseFarePaise = 180_000L,
                    passengerCount = 2,
                    chartPrepared = true,
                    passengerStatuses = listOf("P1: CNF/C1/12", "P2: CNF/C1/13")
                )
            }
        }
        controller.syncForGroupOrExistingExpense(sixMembers, null, emptySet())

        // 1. Out-of-order race: type PNR A (`1111111111`, 5000ms), then after 10ms switch to PNR B (`2222222222`, 50ms)
        controller.onPnrDigitsChanged("1111111111", { sixMembers }, { null }, { emptySet() })
        testDispatcher.scheduler.advanceTimeBy(10L)
        controller.onPnrDigitsChanged("2222222222", { sixMembers }, { null }, { emptySet() })
        testDispatcher.scheduler.advanceTimeBy(6_000L)
        testDispatcher.scheduler.runCurrent()

        assertEquals("2222222222", controller.liveSnapshot?.pnr, "Cancelled slow PNR A must never overwrite fast PNR B")
        assertEquals("12009", controller.liveSnapshot?.trainNo)
        // M-06: 2-passenger ticket in a 6-member group selects 2 members (NOT all 6!)
        assertEquals(2, controller.selectedMemberIds.size, "2-pax ticket in 6-member group must auto-select 2 members, not 6")

        // 2. Mid-fetch user edit preservation (`C-02`): start lookup for PNR C (`3333333333`), customize split & payer mid-flight
        controller.onPnrDigitsChanged("3333333333", { sixMembers }, { null }, { emptySet() })
        testDispatcher.scheduler.advanceTimeBy(10L)
        val customSet = setOf("m3", "m5", "m6")
        controller.selectedMemberIds = customSet
        controller.hasUserCustomizedSplit = true
        controller.onSelectPayer("m4")

        testDispatcher.scheduler.advanceTimeBy(200L)
        testDispatcher.scheduler.runCurrent()

        assertEquals("3333333333", controller.liveSnapshot?.pnr)
        assertEquals(customSet, controller.selectedMemberIds, "User's mid-fetch member selection must never be clobbered")
        assertEquals("m4", controller.selectedPayerId, "User's mid-fetch payer selection must never be clobbered")
    }

    // =========================================================================
    // GATE G7: CrossPnrInputIsolationTest (`C-03`)
    // =========================================================================
    @Test
    @DisplayName("G7: Switching from PNR 1 with manual overrides to PNR 2 resets manualFromStationInput, manualToStationInput & manualTotalFareInput")
    fun gateG7_crossPnrInputIsolationTest() = runTest(testDispatcher) {
        val members = listOf(GroupMemberEntity("m1", "g1", "Akshay", "Akshay", true))
        val controller = PnrReviewSessionController(scope = this) { cleanPnr, _ ->
            PnrNetworkRepository.buildUnverifiedManualFallbackSnapshot(cleanPnr)
        }
        controller.syncForGroupOrExistingExpense(members, null, emptySet())

        controller.onPnrDigitsChanged("8753634406", { members }, { null }, { emptySet() }, autoTriggerWhen10Digits = false)
        controller.manualFromStationInput = "BDTS"
        controller.manualToStationInput = "CDG"
        controller.manualTotalFareInput = "7525.40"

        // Switch to PNR 2
        controller.onPnrDigitsChanged("4521876390", { members }, { null }, { emptySet() }, autoTriggerWhen10Digits = false)
        assertEquals("", controller.manualFromStationInput, "PNR 2 must not inherit PNR 1's manualFromStationInput")
        assertEquals("", controller.manualToStationInput, "PNR 2 must not inherit PNR 1's manualToStationInput")
        assertEquals("", controller.manualTotalFareInput, "PNR 2 must not inherit PNR 1's manualTotalFareInput")
    }

    // =========================================================================
    // GATE G8: CloudSyncPnrLwwAndPrivacyTest (`D-03`, `D-04`)
    // =========================================================================
    @Test
    @DisplayName("G8: Cloud Sync keeps newer local CNF over stale remote WL & scopes Vault exports strictly to group PNRs")
    fun gateG8_cloudSyncPnrLwwAndPrivacyTest() {
        val localConfirmed = testTrainSnapshot(
            pnr = "8753634406",
            trainNo = "12925",
            trainName = "PASCHIM EXPRESS",
            fromStation = "BDTS",
            toStation = "CDG",
            departureTime = "11:30",
            travelClass = "3A",
            baseFarePaise = 750_000L,
            passengerCount = 2,
            bookingStatusBadge = "CNF (Confirmed)",
            chartPrepared = true,
            passengerStatuses = listOf("P1: CNF/B2/21", "P2: CNF/B2/22"),
            structuredPassengers = listOf(
                LivePnrPassenger("P1", "PQWL/14", "CNF/B2/21", "Confirmed"),
                LivePnrPassenger("P2", "PQWL/15", "CNF/B2/22", "Confirmed")
            ),
            fetchedAtEpochMs = 2_000L
        )
        val staleRemoteWaitlist = localConfirmed.copy(
            bookingStatusBadge = "WL (Waitlisted)",
            chartPrepared = false,
            passengerStatuses = listOf("P1: PQWL/14", "P2: PQWL/15"),
            structuredPassengers = listOf(
                LivePnrPassenger("P1", "PQWL/14", "PQWL/14", "Waitlisted"),
                LivePnrPassenger("P2", "PQWL/15", "PQWL/15", "Waitlisted")
            ),
            fetchedAtEpochMs = 1_000L
        )

        val localJson = PnrNetworkRepository.serializeSnapshotJson("8753634406", localConfirmed, 2_000L)
        val remoteJson = PnrNetworkRepository.serializeSnapshotJson("8753634406", staleRemoteWaitlist, 1_000L)
        val winningJson = PnrNetworkRepository.pickNewerTrainSnapshotJson(localJson, remoteJson)
        val winner = PnrNetworkRepository.deserializeSnapshotJson("8753634406", winningJson)
        assertNotNull(winner)
        assertTrue(winner!!.chartPrepared, "Stale remote WL snapshot must NEVER overwrite confirmed local CNF snapshot")
        assertEquals("CNF/B2/21", winner.structuredPassengers[0].currentStatus)

        // D-04 Privacy Scoping: Group A (`8753634406`, `A1B2C3`) must NEVER export Group B's (`9988776655`, `Z9Y8X7`) Vault PII
        PnrNetworkRepository.savePersistedPnrSnapshot(null, localConfirmed)
        PnrNetworkRepository.savePersistedPnrSnapshot(null, localConfirmed.copy(pnr = "9988776655"))
        PnrNetworkRepository.saveConfirmedFlightTicketToVault(
            null,
            testFlightResult("A1B2C3", "6E", "IndiGo", "6E 512", "BOM", "GOI", 540_000L)
        )
        PnrNetworkRepository.saveConfirmedFlightTicketToVault(
            null,
            testFlightResult("Z9Y8X7", "UK", "Vistara", "UK 811", "DEL", "BLR", 890_000L)
        )

        val groupATrainExport = PnrNetworkRepository.exportGroupScopedTrainSnapshotJsonByPnr(null, setOf("8753634406"))
        val groupAFlightExport = PnrNetworkRepository.exportGroupScopedFlightVaultJsonByPnr(null, setOf("A1B2C3"))
        assertEquals(setOf("8753634406"), groupATrainExport.keys)
        assertFalse("9988776655" in groupATrainExport.keys, "Group B train PNR must not leak into Group A sync capsule")
        assertEquals(setOf("A1B2C3"), groupAFlightExport.keys)
        assertFalse("Z9Y8X7" in groupAFlightExport.keys, "Group B flight PNR must not leak into Group A sync capsule")
    }

    // =========================================================================
    // GATE G9: FlightPnrEditAndIrctcPdfRoutingTest (`D-01`, `D-06`, `#8`)
    // =========================================================================
    @Test
    @DisplayName("G9: Editing Flight expense preserves 6-char PNR A7K92B & syncs Vault; IRCTC ERS PDF extracts real rail class (never 'Economy')")
    fun gateG9_flightPnrEditAndIrctcPdfRoutingTest() = runTest(testDispatcher) {
        val vm = SplitMateViewModel(dao = null, ioDispatcher = testDispatcher)
        vm.completeOnboarding("Akshay", "India", "INR", "₹", "Akshay")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.createNewGroup("Goa Flights", "INR", "Rohan")
        testDispatcher.scheduler.advanceUntilIdle()

        val gid = vm.uiState.value.activeGroupId
        val members = vm.uiState.value.members.filter { it.groupId == gid }
        val akshay = members.first { it.name == "Akshay" }

        PnrNetworkRepository.saveConfirmedFlightTicketToVault(
            null,
            testFlightResult("A7K92B", "6E", "IndiGo", "6E 501", "BOM", "GOI", 800_000L)
        )

        val flightTitle = com.splitmate.app.ui.formatTravelExpenseTitle(
            baseCategory = "Flight",
            ticket = ParsedTravelTicket(
                pnr = "A7K92B",
                trainOrFlightNo = "6E 501",
                trainOrCarrierName = "IndiGo",
                fromStation = "BOM",
                toStation = "GOI",
                departureDate = "12 Nov 2026",
                departureTime = "09:15",
                coachAndSeats = "12A, 12B"
            )
        )
        vm.commitQuickEqualExpense(
            title = flightTitle,
            totalAmountCents = 800_000L,
            selectedMemberIds = members.map { it.memberId },
            payerMemberId = akshay.memberId,
            explicitCategory = "FLIGHT",
            explicitTravelPnr = "A7K92B",
            explicitProviderName = "IndiGo"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val savedFlightExp = vm.uiState.value.expenses.first { it.groupId == gid }
        assertEquals("A7K92B", savedFlightExp.travelPnr)

        // Edit total fare to ₹8,450.75 (845,075 paise) while preserving 6-char alphanumeric PNR A7K92B
        val updatedTitle = com.splitmate.app.ui.formatTravelExpenseTitle(
            baseCategory = "Flight",
            ticket = ParsedTravelTicket(
                pnr = "A7K92B",
                trainOrFlightNo = "6E 501",
                trainOrCarrierName = "IndiGo",
                fromStation = "BOM",
                toStation = "GOI",
                departureDate = "12 Nov 2026",
                departureTime = "09:15",
                coachAndSeats = "12A, 12B",
                fareRupees = "8450.75"
            )
        )
        vm.editExistingExpense(
            expenseId = savedFlightExp.expenseId,
            newTitle = updatedTitle,
            newTotalRupees = 8450.75,
            newPayerId = akshay.memberId,
            selectedMemberIds = members.map { it.memberId },
            newTotalCentsOverride = 845_075L
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val afterEdit = vm.uiState.value.expenses.first { it.expenseId == savedFlightExp.expenseId }
        assertEquals("A7K92B", afterEdit.travelPnr, "6-char alphanumeric Flight PNR must be preserved on edit")
        assertEquals(845_075L, afterEdit.totalAmountCents, "Exact decimal paise must be preserved on edit")
        val syncedVaultFlight = PnrNetworkRepository.loadConfirmedFlightTicketResult(null, "A7K92B")
        assertEquals(845_075L, syncedVaultFlight?.totalFarePaise, "Flight Vault totalFarePaise must stay in sync after edit")

        // IRCTC Train ERS PDF extraction: sets `isTrainPdfTicket == true` and extracts real rail class (`3A`), NEVER `"Economy"` or `"IRCTC Express"`
        val irctcErsText = """
            Electronic Reservation Slip (ERS)
            Indian Railways - IRCTC
            PNR: 2648193052
            Train No./Name: 12925 / PASCHIM EXPRESS
            Class: AC 3 Tier (3A)
            Quota: GENERAL (GN)
            From: MUMBAI BANDRA TERMINUS (BDTS)   To: CHANDIGARH (CDG)
            Date of Journey: 15-Nov-2026
            Scheduled Departure: 11:30
            Ticket Fare: Rs. 7,500.00
            IRCTC Convenience Fee: Rs. 23.60
            Passenger 1: Akshay Karadkar  CNF/B2/21/LB
        """.trimIndent()
        val extractedTrainPdf = UniversalFlightTicketExtractor.extractFromText(irctcErsText)
        assertTrue(extractedTrainPdf.isTrainPdfTicket, "IRCTC ERS PDF must be flagged isTrainPdfTicket == true")
        assertEquals("3A", extractedTrainPdf.cabinClass, "IRCTC ERS PDF must extract real rail class 3A, never 'Economy'")
        assertFalse(extractedTrainPdf.airlineName.equals("IRCTC Express", ignoreCase = true))
    }

    // =========================================================================
    // GATE G10: ZeroPlaceholderSourceRatchetTest (`#1`–`#10`)
    // =========================================================================
    @Test
    @DisplayName("G10: Static scan over android/app/src/main confirms ZERO hardcoded PNRs or banned placeholders")
    fun gateG10_zeroPlaceholderSourceRatchetTest() {
        val srcMain = resolveSrcMainDir()
        val bannedPatterns = listOf(
            "8753634406",
            "padEnd(10, '0')",
            "\"6E 204\"",
            "\"3A Sleeper\"",
            "resolveKnownVerifiedTrainPnrSnapshot",
            "KnownIndianTrainRoutes"
        )
        val violations = mutableListOf<String>()

        srcMain.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java" || it.extension == "xml") }
            .forEach { file ->
                val text = file.readText(Charsets.UTF_8)
                for (pattern in bannedPatterns) {
                    if (text.contains(pattern)) {
                        violations.add("${file.relativeTo(srcMain).path} contains banned placeholder `$pattern`")
                    }
                }
            }

        assertTrue(
            violations.isEmpty(),
            "Zero-Placeholder Ratchet failed:\n" + violations.joinToString("\n")
        )
    }
}
