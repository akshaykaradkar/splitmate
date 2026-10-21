package com.splitmate.app.guide.vm

import com.splitmate.app.data.guide.StayResolution
import com.splitmate.app.data.guide.loop.ShortLinkResolver
import com.splitmate.app.guide.vm.GuideVmFixtures.GROUP
import com.splitmate.app.guide.vm.GuideVmFixtures.REV_1
import com.splitmate.app.guide.vm.GuideVmFixtures.STAY_LOC
import com.splitmate.app.guide.vm.GuideVmFixtures.manifest
import com.splitmate.app.guide.vm.GuideVmFixtures.pack
import com.splitmate.app.ui.screens.plan.StayInputFeedback
import com.splitmate.app.ui.screens.plan.TripGuideViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
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
import java.time.ZoneOffset

/**
 * v2.4.0 stay-link fix, ViewModel side: the StayPinSheet's wavy "Finding location" bar is driven
 * by `networkInFlight`, so the link lookup and the name search must be counted while they run
 * (and only then); a new attempt must wipe the previous outcome; closing the sheet must cancel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("v2.4.0: stay lookup drives networkInFlight and never leaves stale feedback")
class TripGuideViewModelStayLookupTest {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private lateinit var g: GuideTestGraph

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        g = GuideTestGraph(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newVm() = TripGuideViewModel(
        groupId = GROUP,
        repository = g.repository,
        network = g.network,
        environment = g.environment,
        sharedStayText = MutableStateFlow(null),
        planSyncRequests = emptyFlow(),
        clock = { g.now },
        zoneId = ZoneOffset.UTC,
        heartbeatMs = null,
        publishDebounceMs = 1_000L
    )

    private suspend fun seedCachedTrip() {
        assertNotNull(g.repository.persist(pack(rev = REV_1)))
        assertTrue(g.store.upsertManifest(GROUP, manifest(rev = REV_1), pendingPush = false))
    }

    private val SHORT_LINK = "https://maps.app.goo.gl/kQhsAWcdCpuWgyfm7"

    @Test
    fun `short link lookup - networkInFlight is true while resolving and false afterwards`() = runTest(dispatcher) {
        seedCachedTrip()
        val vm = newVm()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.networkInFlight)

        val gate = CompletableDeferred<Unit>()
        g.stayResolver.gate = gate
        vm.submitStayInput(SHORT_LINK)
        assertEquals(StayInputFeedback.Working, vm.uiState.value.stayFeedback, "Working is set synchronously on submit")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.networkInFlight, "the wavy bar must reflect THIS lookup")
        assertEquals(StayInputFeedback.Working, vm.uiState.value.stayFeedback)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.networkInFlight)
        assertTrue(vm.uiState.value.stayFeedback is StayInputFeedback.Preview)
    }

    @Test
    fun `offline parse of plain coordinates never flags networkInFlight`() = runTest(dispatcher) {
        seedCachedTrip()
        val vm = newVm()
        advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        g.stayResolver.gate = gate
        vm.submitStayInput("15.3440, 76.4550")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.networkInFlight, "no network: no bar")
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.stayFeedback is StayInputFeedback.Preview)
    }

    @Test
    fun `name fallback search is counted too and ends with the bar off`() = runTest(dispatcher) {
        seedCachedTrip()
        val vm = newVm()
        advanceUntilIdle()
        g.stayResolver.result = StayResolution.NoCoordinates("https://www.google.com/maps?q=Evolve+Back&ftid=0x1:0x2", "Evolve Back, Hampi")
        val geoGate = CompletableDeferred<Unit>()
        g.geocoder.gate = geoGate
        vm.submitStayInput(SHORT_LINK)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.networkInFlight, "Nominatim leg is in flight")
        assertEquals(listOf("Evolve Back, Hampi" to "Hampi"), g.geocoder.calls)
        geoGate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.networkInFlight)
        assertEquals(StayInputFeedback.Preview(STAY_LOC, "Hampi Heritage Homestay", approximate = true), vm.uiState.value.stayFeedback)

        // Explicit "Search by name" from the sheet.
        val gate2 = CompletableDeferred<Unit>()
        g.geocoder.gate = gate2
        vm.searchStayByName("Hampi Heritage Homestay")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.networkInFlight)
        gate2.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.networkInFlight)
    }

    @Test
    fun `a new attempt clears the previous error immediately and the error never comes back`() = runTest(dispatcher) {
        seedCachedTrip()
        val vm = newVm()
        advanceUntilIdle()

        g.stayResolver.result = StayResolution.Rejected(ShortLinkResolver.REASON_TIMEOUT)
        vm.submitStayInput("https://maps.app.goo.gl/slowOne")
        advanceUntilIdle()
        assertEquals(StayInputFeedback.Rejected(ShortLinkResolver.REASON_TIMEOUT), vm.uiState.value.stayFeedback)
        assertFalse(vm.uiState.value.networkInFlight)
        val firstRequestId = vm.uiState.value.stayFeedbackRequestId

        val gate = CompletableDeferred<Unit>()
        g.stayResolver.gate = gate
        g.stayResolver.result = StayResolution.Resolved(STAY_LOC, "Evolve Back, Hampi", com.splitmate.app.data.guide.CoordinatePrecision.EXACT)
        vm.submitStayInput(SHORT_LINK)
        assertEquals(StayInputFeedback.Working, vm.uiState.value.stayFeedback, "stale error gone before the request starts")
        assertTrue(vm.uiState.value.stayFeedbackRequestId > firstRequestId, "new attempt gets a new request id")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.networkInFlight)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(StayInputFeedback.Preview(STAY_LOC, "Evolve Back, Hampi", approximate = false), vm.uiState.value.stayFeedback)
        assertFalse(vm.uiState.value.networkInFlight)
    }

    @Test
    fun `a second paste while the first is in flight cancels the first and only the latest result lands`() = runTest(dispatcher) {
        seedCachedTrip()
        val vm = newVm()
        advanceUntilIdle()

        val gate1 = CompletableDeferred<Unit>()
        g.stayResolver.gate = gate1
        g.stayResolver.result = StayResolution.Rejected(ShortLinkResolver.REASON_TIMEOUT)
        vm.submitStayInput("https://maps.app.goo.gl/first")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.networkInFlight)

        val gate2 = CompletableDeferred<Unit>()
        g.stayResolver.gate = gate2
        g.stayResolver.result = StayResolution.Resolved(STAY_LOC, "Evolve Back, Hampi", com.splitmate.app.data.guide.CoordinatePrecision.EXACT)
        vm.submitStayInput(SHORT_LINK)
        advanceUntilIdle()
        assertEquals(1, g.stayResolver.cancelled, "first lookup cancelled")
        assertTrue(vm.uiState.value.networkInFlight, "second lookup still in flight")
        assertEquals(StayInputFeedback.Working, vm.uiState.value.stayFeedback)

        gate1.complete(Unit) // late completion of the cancelled job must be ignored
        gate2.complete(Unit)
        advanceUntilIdle()
        assertEquals(StayInputFeedback.Preview(STAY_LOC, "Evolve Back, Hampi", approximate = false), vm.uiState.value.stayFeedback)
        assertFalse(vm.uiState.value.networkInFlight)
    }

    @Test
    fun `dismissing the sheet mid-lookup cancels the request and switches the bar off`() = runTest(dispatcher) {
        seedCachedTrip()
        val vm = newVm()
        advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        g.stayResolver.gate = gate
        vm.submitStayInput(SHORT_LINK)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.networkInFlight)

        vm.dismissStayFeedback()
        advanceUntilIdle()
        assertEquals(1, g.stayResolver.cancelled)
        assertFalse(vm.uiState.value.networkInFlight)
        assertEquals(StayInputFeedback.Idle, vm.uiState.value.stayFeedback)
    }
}
