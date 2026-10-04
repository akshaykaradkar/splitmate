package com.splitmate.app

import com.splitmate.app.data.TripLifecycleRecord
import com.splitmate.app.data.TripLifecycleResolver
import com.splitmate.app.data.TripLifecycleResolver.State
import com.splitmate.app.data.TripLifecycleResolver.TravelLegSchedule
import com.splitmate.app.data.TripLifecycleState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SplitMateV235TripLifecycleReturnTest {

    private val day = TripLifecycleResolver.ONE_DAY_MS

    @Test
    @DisplayName("Delhi->Jaipur train out, flight JAI->DEL back => suggests after arrival")
    fun testDelhiJaipurReturn() {
        val outDep = 10 * day
        val outArr = 10 * day + 6 * TripLifecycleResolver.ONE_HOUR_MS
        val retDep = 15 * day
        val retArr = 15 * day + 1 * TripLifecycleResolver.ONE_HOUR_MS
        
        val legs = listOf(
            TravelLegSchedule(departureEpochMs = outDep, arrivalEpochMs = outArr, fromCode = "NDLS", toCode = "JP"),
            TravelLegSchedule(departureEpochMs = retDep, arrivalEpochMs = retArr, fromCode = "JAI", toCode = "DEL")
        )
        val returnArrival = TripLifecycleResolver.resolveReturnArrivalEpochMs(legs)
        assertEquals(retArr, returnArrival)

        // Suggests after arrival
        assertEquals(State.ENDED_SUGGESTED, TripLifecycleResolver.resolve(null, retArr + 1000L, outDep, returnArrival))
    }

    @Test
    @DisplayName("Bengaluru->Goa->Mumbai->Bengaluru multi-city")
    fun testMultiCityReturn() {
        val legs = listOf(
            TravelLegSchedule(departureEpochMs = 1 * day, fromCode = "BLR", toCode = "GOI"),
            TravelLegSchedule(departureEpochMs = 3 * day, fromCode = "GOX", toCode = "BOM"),
            TravelLegSchedule(departureEpochMs = 5 * day, fromCode = "BOM", toCode = "SBC", arrivalEpochMs = 5 * day + 2 * TripLifecycleResolver.ONE_HOUR_MS)
        )
        val returnArrival = TripLifecycleResolver.resolveReturnArrivalEpochMs(legs)
        assertEquals(5 * day + 2 * TripLifecycleResolver.ONE_HOUR_MS, returnArrival)
    }

    @Test
    @DisplayName("one-way Pune->Goa never suggests")
    fun testOneWayNeverSuggests() {
        val legs = listOf(
            TravelLegSchedule(departureEpochMs = 1 * day, fromCode = "PUNE", toCode = "GOI")
        )
        val returnArrival = TripLifecycleResolver.resolveReturnArrivalEpochMs(legs)
        assertNull(returnArrival)
    }

    @Test
    @DisplayName("overnight train with arrival before departure clock (rollover)")
    fun testOvernightTrainRollover() {
        // Departure 22:00, arrival 06:00
        val dep = 10 * day + 22 * TripLifecycleResolver.ONE_HOUR_MS
        val arrRaw = 10 * day + 6 * TripLifecycleResolver.ONE_HOUR_MS // 06:00 on the same day

        val leg = TravelLegSchedule(departureEpochMs = dep, arrivalEpochMs = arrRaw, fromCode = "A", toCode = "B")
        val effectiveArr = TripLifecycleResolver.effectiveArrivalEpochMs(leg)
        
        // Rolled over by 1 day
        assertEquals(11 * day + 6 * TripLifecycleResolver.ONE_HOUR_MS, effectiveArr)
    }

    @Test
    @DisplayName("LTT out, CSMT back (alias match)")
    fun testAliasMatchMumbai() {
        val legs = listOf(
            TravelLegSchedule(departureEpochMs = 1 * day, fromCode = "LTT", toCode = "RN"),
            TravelLegSchedule(departureEpochMs = 5 * day, fromCode = "RN", toCode = "CSMT")
        )
        val returnArrival = TripLifecycleResolver.resolveReturnArrivalEpochMs(legs)
        assertEquals(5 * day + TripLifecycleResolver.DEFAULT_RETURN_TRANSIT_MS, returnArrival)
    }

    @Test
    @DisplayName("date-only return date (end of day)")
    fun testDateOnlyReturn() {
        val legs = listOf(
            TravelLegSchedule(departureEpochMs = 1 * day, fromCode = "DEL", toCode = "BOM"),
            TravelLegSchedule(departureEpochMs = 5 * day, fromCode = "BOM", toCode = "DEL", departureHasTime = false)
        )
        val returnArrival = TripLifecycleResolver.resolveReturnArrivalEpochMs(legs)
        val effectiveDep = 5 * day + day - 1L
        assertEquals(effectiveDep + TripLifecycleResolver.DEFAULT_RETURN_TRANSIT_MS, returnArrival)
    }

    @Test
    @DisplayName("return in future => ACTIVE/ENDING_SOON")
    fun testFutureReturnActiveEndingSoon() {
        val returnArrival = 10 * day
        // Future return (> 1 day away) -> ACTIVE
        assertEquals(State.ACTIVE, TripLifecycleResolver.resolve(null, 5 * day, 1 * day, returnArrival))
        // Ending soon (<= 1 day away) -> ENDING_SOON
        assertEquals(State.ENDING_SOON, TripLifecycleResolver.resolve(null, 9 * day + 1 * TripLifecycleResolver.ONE_HOUR_MS, 1 * day, returnArrival))
    }

    @Test
    @DisplayName("organizer wrap-up => ENDED")
    fun testOrganizerWrapUp() {
        val record = TripLifecycleRecord(state = TripLifecycleState.ENDED)
        assertEquals(State.ENDED, TripLifecycleResolver.resolve(record, 100 * day, 0, null))
    }

    @Test
    @DisplayName("reopen with no record => ACTIVE and suggestion suppressed")
    fun testReopenSuppressesSuggestion() {
        val outDep = 10 * day
        val retDep = 15 * day
        val returnArrival = retDep + TripLifecycleResolver.DEFAULT_RETURN_TRANSIT_MS
        
        // Without record, it's ended suggested after arrival
        assertEquals(State.ENDED_SUGGESTED, TripLifecycleResolver.resolve(null, 20 * day, 10 * day, returnArrival))
        
        // Organizer reopens it at 18 * day
        val reopenedRecord = TripLifecycleResolver.buildReopenRecord(null, 18 * day)
        
        // The return arrival is 15 * day + 12 hrs, which is BEFORE 18 * day.
        // So the return arrival is ignored, and it falls back to quiet trip detection.
        // Quiet trip uses max(lastActivity, reopenedAt).
        // current = 20 * day. reopenedAt = 18 * day. Difference is 2 days.
        // 2 days <= 3 days (QUIET_DAYS_BEFORE_SUGGEST), so it should be ACTIVE.
        assertEquals(State.ACTIVE, TripLifecycleResolver.resolve(reopenedRecord, 20 * day, 10 * day, returnArrival))
    }

    @Test
    @DisplayName("reused group across two trips (old legs before a reopen marker ignored)")
    fun testReusedGroupOldLegsIgnored() {
        // Trip 1
        val legs = listOf(
            TravelLegSchedule(departureEpochMs = 1 * day, fromCode = "DEL", toCode = "BOM"),
            TravelLegSchedule(departureEpochMs = 5 * day, fromCode = "BOM", toCode = "DEL")
        )
        // Group reused on day 20. Organizer reopens group.
        val reopenStamp = 20 * day
        
        // The old legs should be ignored for computing the return arrival
        val returnArrival = TripLifecycleResolver.resolveReturnArrivalEpochMs(legs, ignoreLegsDepartingBeforeEpochMs = reopenStamp)
        assertNull(returnArrival)
    }
}
