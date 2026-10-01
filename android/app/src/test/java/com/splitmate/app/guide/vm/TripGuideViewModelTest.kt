package com.splitmate.app.guide.vm

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.GuideFeatureFlags
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.StayResolution
import com.splitmate.app.data.guide.TripGuideRepository
import com.splitmate.app.data.guide.content.GuideContentException
import com.splitmate.app.data.guide.loop.NominatimGeocoder
import com.splitmate.app.data.guide.nav.MapsHandoff
import com.splitmate.app.data.guide.nav.TravelMode
import com.splitmate.app.data.guide.sync.NtfyPlanMessage
import com.splitmate.app.guide.vm.GuideVmFixtures.DAY
import com.splitmate.app.guide.vm.GuideVmFixtures.GROUP
import com.splitmate.app.guide.vm.GuideVmFixtures.HAMPI_CENTER
import com.splitmate.app.guide.vm.GuideVmFixtures.HAMPI_QID
import com.splitmate.app.guide.vm.GuideVmFixtures.NOW
import com.splitmate.app.guide.vm.GuideVmFixtures.REV_1
import com.splitmate.app.guide.vm.GuideVmFixtures.REV_2
import com.splitmate.app.guide.vm.GuideVmFixtures.STAY_LOC
import com.splitmate.app.guide.vm.GuideVmFixtures.hampiCandidate
import com.splitmate.app.guide.vm.GuideVmFixtures.hemakuta
import com.splitmate.app.guide.vm.GuideVmFixtures.hosapeteCandidate
import com.splitmate.app.guide.vm.GuideVmFixtures.manifest
import com.splitmate.app.guide.vm.GuideVmFixtures.noCoords
import com.splitmate.app.guide.vm.GuideVmFixtures.pack
import com.splitmate.app.guide.vm.GuideVmFixtures.virupaksha
import com.splitmate.app.ui.screens.plan.GuidePhase
import com.splitmate.app.ui.screens.plan.PlanSubView
import com.splitmate.app.ui.screens.plan.StayInputFeedback
import com.splitmate.app.ui.screens.plan.TripGuideEffect
import com.splitmate.app.ui.screens.plan.TripGuidePresenter
import com.splitmate.app.ui.screens.plan.TripGuideUiState
import com.splitmate.app.ui.screens.plan.TripGuideViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("v2.3.4 E1: TripGuideViewModel orchestration")
class TripGuideViewModelTest {

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

    private fun newVm(
        shared: MutableStateFlow<String?> = MutableStateFlow(null),
        planRequests: Flow<String> = emptyFlow()
    ) = TripGuideViewModel(
        groupId = GROUP,
        repository = g.repository,
        network = g.network,
        environment = g.environment,
        sharedStayText = shared,
        planSyncRequests = planRequests,
        clock = { g.now },
        zoneId = ZoneOffset.UTC,
        heartbeatMs = null,
        publishDebounceMs = 1_000L
    )

    private fun TestScope.effectsOf(vm: TripGuideViewModel): MutableList<TripGuideEffect> {
        val out = mutableListOf<TripGuideEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.collect { out += it } }
        return out
    }

    /** Seeds a stored pack + a manifest pointing at it (what a returning user has on disk). */
    private suspend fun seedCachedTrip(fetchedAt: Long = NOW, rev: Long = REV_1) {
        assertNotNull(g.repository.persist(pack(rev = rev, fetchedAt = fetchedAt)))
        assertTrue(g.store.upsertManifest(GROUP, manifest(rev = rev), pendingPush = false))
    }

    private val TripGuideViewModel.s: TripGuideUiState get() = uiState.value

    private fun TripGuideUiState.cardIds(): List<String> = sections.flatMap { sec -> sec.places.map { it.place.id } }

    // ---------------------------------------------------------------------------------------------
    // Lifecycle, search, hydrate
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `new trip starts Empty with booking suggestions and refreshes flags once`() = runTest(dispatcher) {
        val vm = newVm()
        advanceUntilIdle()
        assertEquals(GuidePhase.Empty, vm.s.phase)
        assertEquals(listOf("Hosapete"), vm.s.bookingSuggestions)
        assertEquals(1, g.flags.refreshCalls)
        assertFalse(vm.s.networkInFlight)
        assertEquals(0, g.content.buildCalls)
    }

    @Test
    fun `ambiguous search disambiguates, then hydration is the only time networkInFlight is true`() = runTest(dispatcher) {
        g.content.candidates = listOf(hampiCandidate, hosapeteCandidate)
        g.content.ambiguous = true
        val vm = newVm()
        advanceUntilIdle()

        vm.updateDestinationQuery("Hampi")
        vm.submitDestinationQuery()
        advanceUntilIdle()
        val phase = vm.s.phase
        assertTrue(phase is GuidePhase.Disambiguate)
        assertEquals(listOf(HAMPI_QID, hosapeteCandidate.qid), (phase as GuidePhase.Disambiguate).candidates.map { it.qid })
        assertFalse(vm.s.networkInFlight)

        val gate = CompletableDeferred<Unit>()
        g.content.buildGate = gate
        vm.chooseCandidate(hampiCandidate)
        advanceUntilIdle()
        assertEquals(GuidePhase.Hydrating(hampiCandidate), vm.s.phase)
        assertTrue(vm.s.networkInFlight)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(GuidePhase.Ready, vm.s.phase)
        assertFalse(vm.s.networkInFlight)
        assertEquals(HAMPI_QID, vm.s.pack?.destination?.qid)
        assertEquals(listOf("Must See", "Things to Do", "Local Eats"), vm.s.sections.map { it.title })
        assertTrue(g.local.packs.value.containsKey(HAMPI_QID), "pack saved for offline use")
        val m = g.store.getManifest(GROUP)!!
        assertEquals(HAMPI_QID, m.destinationQid)
        assertEquals(REV_1, m.wikivoyageRevisionId)
        assertTrue(g.nearby.centers.contains(HAMPI_CENTER), "WDQS dragnet around the destination centre")
    }

    @Test
    fun `unambiguous search hydrates directly and the edit is published after the debounce`() = runTest(dispatcher) {
        val vm = newVm()
        advanceUntilIdle()
        vm.updateDestinationQuery("Hampi")
        vm.submitDestinationQuery()
        advanceUntilIdle()
        assertEquals(GuidePhase.Ready, vm.s.phase)
        assertEquals(HAMPI_QID, g.transport.published.last().destinationQid)
        assertFalse(g.local.manifests.value.getValue(GROUP).pendingPush)
    }

    @Test
    fun `query shorter than two letters is refused with a snackbar`() = runTest(dispatcher) {
        val vm = newVm()
        val effects = effectsOf(vm)
        advanceUntilIdle()
        vm.updateDestinationQuery("H")
        vm.submitDestinationQuery()
        advanceUntilIdle()
        assertEquals(0, g.content.resolveCalls)
        assertEquals(TripGuideEffect.Snackbar(TripGuideViewModel.MSG_QUERY_TOO_SHORT), effects.single())
    }

    @Test
    fun `no results is a non-retryable error`() = runTest(dispatcher) {
        g.content.candidates = emptyList()
        val vm = newVm()
        advanceUntilIdle()
        vm.updateDestinationQuery("Atlantis")
        vm.submitDestinationQuery()
        advanceUntilIdle()
        val phase = vm.s.phase as GuidePhase.Error
        assertFalse(phase.retryable)
        assertTrue(phase.message.contains("Atlantis"))
    }

    @Test
    fun `rate-limited build shows retryable error and retry recovers`() = runTest(dispatcher) {
        g.content.buildError = GuideContentException("HTTP 429", httpCode = 429)
        val vm = newVm()
        advanceUntilIdle()
        vm.chooseCandidate(hampiCandidate)
        advanceUntilIdle()
        assertEquals(GuidePhase.Error(TripGuideRepository.MSG_RATE_LIMITED, retryable = true), vm.s.phase)
        assertFalse(vm.s.networkInFlight)

        g.content.buildError = null
        vm.retry()
        advanceUntilIdle()
        assertEquals(GuidePhase.Ready, vm.s.phase)
    }

    @Test
    fun `no curated guide degrades to Partial with Wikidata landmarks`() = runTest(dispatcher) {
        g.content.packFor = { c, _ -> pack(qid = c.qid, rev = null, places = emptyList()) }
        g.nearby.results = listOf(hemakuta)
        val vm = newVm()
        advanceUntilIdle()
        vm.chooseCandidate(hampiCandidate)
        advanceUntilIdle()
        assertEquals(GuidePhase.Partial(TripGuideRepository.MSG_NO_CURATED), vm.s.phase)
        val card = vm.s.sections.single().places.single()
        assertEquals(hemakuta.id, card.place.id)
        assertEquals(TripGuidePresenter.SOURCE_WIKIDATA, card.sourceLabel)
        assertTrue(vm.s.attribution.contains(TripGuidePresenter.WIKIDATA_NEARBY_ATTRIBUTION))
    }

    @Test
    fun `nearby failure keeps curated content as Partial`() = runTest(dispatcher) {
        g.nearby.error = GuideContentException("HTTP 503", httpCode = 503)
        val vm = newVm()
        advanceUntilIdle()
        vm.chooseCandidate(hampiCandidate)
        advanceUntilIdle()
        assertEquals(GuidePhase.Partial(TripGuideRepository.MSG_NEARBY_FAILED), vm.s.phase)
        assertTrue(vm.s.cardIds().contains(virupaksha.id))
    }

    // ---------------------------------------------------------------------------------------------
    // Cache first, offline, soft expiry
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `cache first - a stored pack renders with no network calls`() = runTest(dispatcher) {
        seedCachedTrip()
        val vm = newVm()
        advanceUntilIdle()
        assertEquals(GuidePhase.Ready, vm.s.phase)
        assertEquals(HAMPI_QID, vm.s.pack?.destination?.qid)
        assertFalse(vm.s.networkInFlight)
        assertEquals(0, g.content.buildCalls)
        assertEquals(0, g.content.resolveCalls)
        assertTrue(g.nearby.centers.isEmpty())
        assertNotNull(vm.s.saveOfflineSizeLabel)
    }

    @Test
    fun `offline with a stored pack shows the saved-on label`() = runTest(dispatcher) {
        seedCachedTrip()
        g.network.set(online = false)
        val vm = newVm()
        advanceUntilIdle()
        assertEquals(GuidePhase.Ready, vm.s.phase)
        assertTrue(vm.s.isOffline)
        assertEquals("Offline · guide saved on 29 Sep", vm.s.offlineSavedLabel)
    }

    @Test
    fun `offline without a stored pack errors, then auto-retries when back online`() = runTest(dispatcher) {
        g.store.upsertManifest(GROUP, manifest(), pendingPush = false)
        g.network.set(online = false)
        val vm = newVm()
        advanceUntilIdle()
        assertEquals(GuidePhase.Error(TripGuideViewModel.MSG_OFFLINE_GUIDE, retryable = true), vm.s.phase)
        assertEquals(0, g.content.buildCalls)

        g.network.set(online = true)
        advanceUntilIdle()
        assertEquals(GuidePhase.Ready, vm.s.phase)
        assertFalse(vm.s.isOffline)
        assertEquals(listOf<Long?>(REV_1), g.content.pinnedRevisions, "rebuilt at the group's pinned revision")
    }

    @Test
    fun `soft-expired pack refreshes on unmetered network and holds a new revision behind guideUpdateAvailable`() = runTest(dispatcher) {
        seedCachedTrip(fetchedAt = NOW - 15 * DAY)
        g.content.packFor = { c, _ -> pack(qid = c.qid, rev = REV_2) }
        val vm = newVm()
        val effects = effectsOf(vm)
        advanceUntilIdle()
        assertTrue(vm.s.guideUpdateAvailable)
        assertEquals(REV_1, vm.s.pack?.wikivoyage?.revisionId, "group stays on its pinned revision until accepted")
        assertFalse(vm.s.networkInFlight)

        vm.acceptGuideUpdate()
        advanceUntilIdle()
        assertFalse(vm.s.guideUpdateAvailable)
        assertEquals(REV_2, vm.s.pack?.wikivoyage?.revisionId)
        assertEquals(REV_2, g.store.getManifest(GROUP)?.wikivoyageRevisionId)
        assertTrue(effects.contains(TripGuideEffect.Snackbar(TripGuideViewModel.MSG_GUIDE_UPDATED)))
    }

    @Test
    fun `soft-expired pack is not refreshed on a metered network`() = runTest(dispatcher) {
        seedCachedTrip(fetchedAt = NOW - 15 * DAY)
        g.network.set(online = true, metered = true)
        val vm = newVm()
        advanceUntilIdle()
        assertEquals(GuidePhase.Ready, vm.s.phase)
        assertEquals(0, g.content.buildCalls)
        assertFalse(vm.s.guideUpdateAvailable)
    }

    // ---------------------------------------------------------------------------------------------
    // Flags
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `kill switch hides Explore and Loop and blocks guide network use`() = runTest(dispatcher) {
        g.flags.now = GuideFeatureFlags(guideEnabled = false)
        g.flags.afterRefresh = g.flags.now
        seedCachedTrip()
        val vm = newVm()
        advanceUntilIdle()
        assertFalse(vm.s.guideEnabled)
        vm.selectSubView(PlanSubView.EXPLORE)
        vm.updateDestinationQuery("Hampi")
        vm.submitDestinationQuery()
        advanceUntilIdle()
        assertEquals(PlanSubView.BOOKINGS, vm.s.subView)
        assertEquals(0, g.content.resolveCalls)
        assertTrue(g.transport.fetchCursors.isEmpty(), "no plan sync while disabled")
    }

    @Test
    fun `flags refreshed on first open can switch the guide off`() = runTest(dispatcher) {
        g.flags.afterRefresh = GuideFeatureFlags(guideEnabled = false)
        val vm = newVm()
        vm.selectSubView(PlanSubView.EXPLORE)
        assertEquals(PlanSubView.EXPLORE, vm.s.subView)
        advanceUntilIdle()
        assertFalse(vm.s.guideEnabled)
        assertEquals(PlanSubView.BOOKINGS, vm.s.subView)
    }

    // ---------------------------------------------------------------------------------------------
    // Pin / hide
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `pin, unpin keeps the card visible, hide removes it for the group`() = runTest(dispatcher) {
        seedCachedTrip()
        val vm = newVm()
        val effects = effectsOf(vm)
        advanceUntilIdle()
        fun card() = vm.s.sections.flatMap { it.places }.firstOrNull { it.place.id == virupaksha.id }

        vm.togglePin(virupaksha.id)
        advanceUntilIdle()
        assertTrue(card()!!.isPinned)

        g.now += 1_000
        vm.togglePin(virupaksha.id)
        advanceUntilIdle()
        assertNotNull(card(), "un-pin keeps the place in Explore")
        assertFalse(card()!!.isPinned)

        g.now += 1_000
        vm.hidePlace(virupaksha.id)
        advanceUntilIdle()
        assertNull(card())
        assertTrue(effects.contains(TripGuideEffect.Snackbar(TripGuideViewModel.MSG_HIDDEN)))
        assertTrue(g.transport.published.isNotEmpty())
    }

    // ---------------------------------------------------------------------------------------------
    // Effects
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `place actions emit maps, chooser, clipboard and share effects`() = runTest(dispatcher) {
        val vm = newVm()
        val effects = effectsOf(vm)
        advanceUntilIdle()
        val loc = virupaksha.location!!

        vm.directions(virupaksha)
        vm.navigate(virupaksha, TravelMode.WALKING)
        vm.openInOtherApp(virupaksha)
        vm.copyCoordinates(virupaksha)
        vm.sharePlace(virupaksha)
        vm.directions(noCoords)
        advanceUntilIdle()

        assertEquals(
            listOf(
                TripGuideEffect.OpenMaps(MapsHandoff.geoUri(loc, virupaksha.name), MapsHandoff.searchUrl(loc)),
                TripGuideEffect.OpenMaps(MapsHandoff.navigationUri(loc, TravelMode.WALKING), MapsHandoff.directionsUrl(loc, null, TravelMode.WALKING)),
                TripGuideEffect.OpenMapsChooser(MapsHandoff.geoUri(loc, virupaksha.name), MapsHandoff.searchUrl(loc), TripGuideViewModel.CHOOSER_TITLE),
                TripGuideEffect.CopyToClipboard(TripGuideViewModel.COPY_LABEL, MapsHandoff.copyableCoordinates(loc)),
                TripGuideEffect.ShareText(MapsHandoff.placeShareText(virupaksha.name, loc)),
                TripGuideEffect.Snackbar(TripGuideViewModel.MSG_NO_PLACE_LOCATION)
            ),
            effects
        )
    }

    @Test
    fun `loop needs a stay, then opens Maps and shares a text summary`() = runTest(dispatcher) {
        seedCachedTrip()
        val vm = newVm()
        val effects = effectsOf(vm)
        advanceUntilIdle()
        assertNull(vm.s.loop.route)
        assertNull(vm.s.loop.totalLabel)
        vm.openWholeLoopInMaps()
        vm.shareLoop()
        advanceUntilIdle()
        assertEquals(
            listOf(TripGuideEffect.Snackbar(TripGuideViewModel.MSG_SET_STAY_FIRST), TripGuideEffect.Snackbar(TripGuideViewModel.MSG_SET_STAY_FIRST)),
            effects
        )
        effects.clear()

        vm.submitStayInput("https://maps.app.goo.gl/abcDEF123")
        advanceUntilIdle()
        vm.confirmStayPreview()
        advanceUntilIdle()
        val loop = vm.s.loop
        assertNotNull(loop.route)
        assertTrue(loop.autoSuggested)
        assertTrue(loop.route!!.orderedStops.none { it.kind == PlaceKind.SLEEP })
        assertTrue(Regex("""\d+ stops · ≈ \d+\.\d km""").matches(loop.totalLabel!!), loop.totalLabel)
        assertEquals(TripGuidePresenter.LOOP_CAVEATS, loop.caveats)

        vm.openWholeLoopInMaps()
        vm.shareLoop()
        advanceUntilIdle()
        val open = effects.filterIsInstance<TripGuideEffect.OpenUrl>().single()
        assertTrue(open.url.startsWith("https://www.google.com/maps/dir/"), open.url)
        val share = effects.filterIsInstance<TripGuideEffect.ShareText>().single()
        assertTrue(share.text.contains("Day loop from Hotel Mayura Bhuvaneswari"), share.text)
    }

    // ---------------------------------------------------------------------------------------------
    // Stay
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `pasted link previews, confirm saves a local-only stay and queries nearby around it`() = runTest(dispatcher) {
        seedCachedTrip()
        val vm = newVm()
        val effects = effectsOf(vm)
        advanceUntilIdle()

        vm.submitStayInput("  https://maps.app.goo.gl/abcDEF123  ")
        advanceUntilIdle()
        assertEquals(listOf("https://maps.app.goo.gl/abcDEF123"), g.stayResolver.inputs)
        assertEquals(StayInputFeedback.Preview(STAY_LOC, "Hotel Mayura Bhuvaneswari", approximate = false), vm.s.stayFeedback)
        assertNull(vm.s.stay.attribution)

        vm.confirmStayPreview()
        advanceUntilIdle()
        assertEquals(StayInputFeedback.Idle, vm.s.stayFeedback)
        val stay = vm.s.stay.stay!!
        assertEquals(STAY_LOC, stay.location)
        assertEquals(CoordinatePrecision.EXACT, stay.precision)
        assertFalse(vm.s.stay.shareWithGroup)
        assertFalse(vm.s.stay.isShared)
        assertNull(g.store.getManifest(GROUP)?.stay, "stay is local-only by default")
        assertTrue(effects.contains(TripGuideEffect.Snackbar(TripGuideViewModel.MSG_STAY_SET)))
        assertTrue(g.nearby.centers.contains(STAY_LOC))
        assertTrue(vm.s.stay.sleepListings.any { it.kind == PlaceKind.SLEEP })
    }

    @Test
    fun `name search uses Nominatim and carries the OpenStreetMap attribution`() = runTest(dispatcher) {
        seedCachedTrip()
        val vm = newVm()
        advanceUntilIdle()
        vm.searchStayByName("Hampi Heritage Homestay")
        advanceUntilIdle()
        assertEquals(listOf("Hampi Heritage Homestay" to "Hampi"), g.geocoder.calls)
        assertEquals(StayInputFeedback.Preview(STAY_LOC, "Hampi Heritage Homestay", approximate = true), vm.s.stayFeedback)
        assertEquals(NominatimGeocoder.ATTRIBUTION, vm.s.stay.attribution)

        vm.confirmStayPreview()
        advanceUntilIdle()
        assertEquals(CoordinatePrecision.APPROXIMATE, vm.s.stay.stay?.precision)
        assertEquals(NominatimGeocoder.ATTRIBUTION, vm.s.stay.attribution)
    }

    @Test
    fun `stay input edge cases - empty, no coordinates, offline name search, clear`() = runTest(dispatcher) {
        seedCachedTrip()
        val vm = newVm()
        val effects = effectsOf(vm)
        advanceUntilIdle()

        vm.submitStayInput("   ")
        assertEquals(StayInputFeedback.Rejected(TripGuideViewModel.MSG_STAY_EMPTY), vm.s.stayFeedback)

        g.stayResolver.result = StayResolution.NoCoordinates("https://www.google.com/maps/place/Hotel+X", "Hotel X")
        vm.submitStayInput("https://maps.app.goo.gl/noCoords1")
        advanceUntilIdle()
        assertEquals(StayInputFeedback.NeedsFallback("Hotel X"), vm.s.stayFeedback)

        vm.searchStayByName(" ")
        assertEquals(StayInputFeedback.Rejected(NominatimGeocoder.REASON_EMPTY), vm.s.stayFeedback)

        g.network.set(online = false)
        advanceUntilIdle()
        vm.searchStayByName("Hotel X")
        assertEquals(StayInputFeedback.Rejected(TripGuideViewModel.MSG_STAY_OFFLINE), vm.s.stayFeedback)
        assertTrue(g.geocoder.calls.isEmpty())

        vm.chooseSleepListing(GuideVmFixtures.mayura)
        vm.confirmStayPreview()
        advanceUntilIdle()
        assertNotNull(vm.s.stay.stay)
        vm.clearStay()
        advanceUntilIdle()
        assertNull(vm.s.stay.stay)
        assertTrue(effects.contains(TripGuideEffect.Snackbar(TripGuideViewModel.MSG_STAY_CLEARED)))
    }

    @Test
    fun `sharing the stay publishes it with the manifest`() = runTest(dispatcher) {
        seedCachedTrip()
        val vm = newVm()
        advanceUntilIdle()
        vm.submitStayInput("15.3440, 76.4550")
        advanceUntilIdle()
        vm.confirmStayPreview()
        advanceUntilIdle()
        vm.setShareStay(true)
        advanceUntilIdle()
        assertTrue(vm.s.stay.shareWithGroup)
        val published = g.transport.published.last()
        assertEquals(STAY_LOC, published.stay?.location)
        assertEquals(GuideVmFixtures.MEMBER, published.stay?.setByMemberKey)
    }

    @Test
    fun `map link shared from another app becomes a stay preview exactly once`() = runTest(dispatcher) {
        seedCachedTrip()
        val shared = MutableStateFlow<String?>("Hotel Mayura https://maps.app.goo.gl/abcDEF123")
        val vm = newVm(shared = shared)
        advanceUntilIdle()
        assertNull(shared.value)
        assertTrue(vm.s.stayFeedback is StayInputFeedback.Preview)
        assertEquals(1, g.stayResolver.inputs.size)
    }

    // ---------------------------------------------------------------------------------------------
    // Sync
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `a destination set by another member opens at the group's pinned revision`() = runTest(dispatcher) {
        g.transport.inbox = listOf(NtfyPlanMessage(manifest(rev = REV_2), "msgA1", NOW / 1000))
        val vm = newVm()
        advanceUntilIdle()
        assertEquals(GuidePhase.Ready, vm.s.phase)
        assertEquals(HAMPI_QID, vm.s.pack?.destination?.qid)
        assertEquals(REV_2, vm.s.pack?.wikivoyage?.revisionId)
        assertTrue(g.content.pinnedRevisions.contains(REV_2))
        assertEquals("msgA1", g.local.manifests.value.getValue(GROUP).lastSyncMessageId)
    }

    @Test
    fun `live plan-topic events for this trip trigger a plan sync, other trips are ignored`() = runTest(dispatcher) {
        val requests = MutableSharedFlow<String>(extraBufferCapacity = 4)
        newVm(planRequests = requests)
        advanceUntilIdle()
        val before = g.transport.fetchCursors.size
        requests.tryEmit("g_other_trip")
        advanceUntilIdle()
        assertEquals(before, g.transport.fetchCursors.size)
        requests.tryEmit(GROUP)
        advanceUntilIdle()
        assertEquals(before + 1, g.transport.fetchCursors.size)
    }
}
