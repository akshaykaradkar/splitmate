package com.splitmate.app.ui.screens.plan

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.splitmate.app.data.TripPlanManifestEntity
import com.splitmate.app.data.guide.BookingDestinationSuggestions
import com.splitmate.app.data.guide.BuildOutcome
import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.DestinationCandidate
import com.splitmate.app.data.guide.GuideFeatureFlags
import com.splitmate.app.data.guide.GuidePack
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.LoopOptimizer
import com.splitmate.app.data.guide.LoopRoute
import com.splitmate.app.data.guide.NetworkMonitor
import com.splitmate.app.data.guide.NetworkState
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.data.guide.PlanManifest
import com.splitmate.app.data.guide.ResolveOutcome
import com.splitmate.app.data.guide.StayPin
import com.splitmate.app.data.guide.StayResolution
import com.splitmate.app.data.guide.StoredPack
import com.splitmate.app.data.guide.TripGuideEnvironment
import com.splitmate.app.data.guide.TripGuideRepository
import com.splitmate.app.data.guide.TripGuideServices
import com.splitmate.app.data.guide.content.WikimediaGuideSource
import com.splitmate.app.data.guide.loop.LoopStopSelector
import com.splitmate.app.data.guide.loop.NominatimGeocoder
import com.splitmate.app.data.guide.nav.MapsHandoff
import com.splitmate.app.data.guide.nav.TravelMode
import com.splitmate.app.data.guide.sync.PlanManifestCodec
import com.splitmate.app.data.guide.sync.PlanManifestMerger
import com.splitmate.app.data.guide.sync.TripGuideStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

/**
 * v2.3.4 Plan tab ViewModel (decision #27). Implements [TripGuideActions] and exposes
 * [uiState] + one-shot [effects] per the contract in TripGuideContract.kt.
 *
 * Lifecycle & cost:
 * - Created only when [TripPlanTabHost] is composed (lazy; zero cold-start cost). Flags are
 *   refreshed on first open; `guideEnabled = false` hides Explore/Loop and stops guide network use.
 * - Cache first: the stored pack for the manifest's destination renders instantly with
 *   `networkInFlight = false`. A soft-expired pack is refreshed in the background only when online
 *   AND unmetered; a changed Wikivoyage revision is held back behind `guideUpdateAvailable` until
 *   [acceptGuideUpdate] (the group stays pinned to one revision, decision #19).
 * - `networkInFlight` is true ONLY while Resolving / Hydrating / Refreshing / LoopFetching network
 *   calls run (never for cached or offline data, never for plan sync or stay parsing).
 *
 * Pin / hide semantics (manifest OR-sets, visible iff pinnedAt > hiddenAt):
 * - [togglePin] on an un-pinned place pins it ([PlanManifestMerger.pin]).
 * - [togglePin] on a pinned place UN-PINS it by writing a hide tombstone EQUAL to the pin time.
 *   The merger then no longer treats it as pinned, but Explore keeps showing the card, because
 *   Explore hides a place only when its hide is STRICTLY newer than its pin
 *   ([TripGuideRepository.isHiddenForExplore]).
 * - [hidePlace] always writes a hide strictly newer than any pin, so the card disappears for the
 *   whole group. Every device applies the same rule, so the result is deterministic after merge.
 *
 * Sync: the plan topic is polled on open, on live-stream plan events ([planSyncRequests]) and on a
 * heartbeat while the host is visible; edits are persisted then published (debounced) when the row
 * is pending push or the republish policy says so. The loop is recomputed locally, never synced.
 *
 * Failures never block money flows: they degrade to Partial / Error (with Retry) or a Snackbar.
 */
class TripGuideViewModel(
    private val groupId: String,
    private val repository: TripGuideRepository,
    private val network: NetworkMonitor,
    private val environment: TripGuideEnvironment,
    private val sharedStayText: MutableStateFlow<String?> = MutableStateFlow(null),
    private val planSyncRequests: Flow<String> = emptyFlow(),
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val heartbeatMs: Long? = HEARTBEAT_MS,
    private val publishDebounceMs: Long = PUBLISH_DEBOUNCE_MS,
    startImmediately: Boolean = true
) : ViewModel(), TripGuideActions {

    private val _uiState = MutableStateFlow(
        TripGuideUiState(
            guideEnabled = repository.currentFlags().guideEnabled,
            isOffline = !network.state.value.online
        )
    )
    val uiState: StateFlow<TripGuideUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TripGuideEffect>(Channel.BUFFERED)
    val effects: Flow<TripGuideEffect> = _effects.receiveAsFlow()

    // ---- internal state (main thread only) --------------------------------------------------------

    private var flags: GuideFeatureFlags = repository.currentFlags()
    private var manifest: PlanManifest = TripGuideStore.emptyManifest()
    private var localStay: StayPin? = null
    private var effectiveStay: StayPin? = null
    private var shareStay: Boolean = false
    private var stored: StoredPack? = null
    private var partialMessage: String? = null
    private var pendingUpdate: GuidePack? = null
    private var hydratingQid: String? = null
    private var revisionRebuildAttempted: Pair<String, Long>? = null
    private var inFlight = 0
    private var lastAction: RetryAction? = null
    private var lastErrorWasOffline = false
    private var previewFromNominatim = false
    private var stayFromNominatim = false
    private var memberKeyLoaded = false
    private var memberKeyValue: String? = null
    private var started = false
    private var visible = true
    private var loopCacheKey: Any? = null
    private var loopCache: LoopUi = EMPTY_LOOP

    private var guideJob: Job? = null
    private var guideJobIsBackground = false
    private var publishJob: Job? = null
    private var heartbeatJob: Job? = null
    /** v2.3.4 QA: only the latest stay lookup (link parse / Nominatim) may update stayFeedback. */
    private var stayJob: Job? = null
    /** v2.3.4 QA: only the latest stay-centred WDQS dragnet may merge into the pack. */
    private var stayNearbyJob: Job? = null

    private sealed interface RetryAction {
        data class Resolve(val query: String, val hint: String?) : RetryAction
        data class Hydrate(val candidate: DestinationCandidate) : RetryAction
        data class Open(val qid: String, val revisionId: Long?) : RetryAction
    }

    init {
        if (startImmediately) start()
    }

    /** Starts observation/sync (idempotent). Called from init unless a test defers it. */
    fun start() {
        if (started) return
        started = true
        viewModelScope.launch { applyFlags(repository.refreshFlags()) }
        viewModelScope.launch {
            val raw = quietly(emptyList<String>()) { environment.bookingDestinationNames(groupId) }
            _uiState.update { it.copy(bookingSuggestions = BookingDestinationSuggestions.normalise(raw)) }
        }
        viewModelScope.launch { network.state.collect { onNetwork(it) } }
        viewModelScope.launch { repository.observeManifestRow(groupId).collect { onManifestRow(it) } }
        viewModelScope.launch { syncNow() }
        viewModelScope.launch { sharedStayText.collect { consumeSharedStayText() } }
        viewModelScope.launch { planSyncRequests.collect { if (it == groupId) syncNow() } }
        startHeartbeat()
    }

    /** Host visibility (Plan tab shown + lifecycle started). Heartbeat and shared-link intake run only while visible. */
    fun onHostVisibilityChanged(isVisible: Boolean) {
        if (visible == isVisible) return
        visible = isVisible
        if (isVisible) {
            startHeartbeat()
            consumeSharedStayText()
            viewModelScope.launch { syncNow() }
        } else {
            heartbeatJob?.cancel()
            heartbeatJob = null
        }
    }

    // =============================================================================================
    // TripGuideActions
    // =============================================================================================

    override fun selectSubView(view: PlanSubView) {
        if (!flags.guideEnabled && view != PlanSubView.BOOKINGS) return
        _uiState.update { it.copy(subView = view) }
    }

    override fun updateDestinationQuery(query: String) {
        _uiState.update { it.copy(destinationQuery = query.take(MAX_QUERY_CHARS)) }
    }

    override fun submitDestinationQuery() {
        if (!flags.guideEnabled) return
        val q = _uiState.value.destinationQuery.trim()
        if (q.length < MIN_QUERY_CHARS) {
            emit(TripGuideEffect.Snackbar(MSG_QUERY_TOO_SHORT))
            return
        }
        resolveAndChoose(q, _uiState.value.bookingSuggestions.firstOrNull())
    }

    override fun chooseCandidate(candidate: DestinationCandidate) {
        if (!flags.guideEnabled) return
        lastAction = RetryAction.Hydrate(candidate)
        launchGuideJob(background = false) {
            val cached = repository.loadCachedPack(candidate.qid)
            if (cached != null) {
                showStored(cached, cachedPartialMessage(cached.pack))
                repository.setDestination(groupId, candidate.qid, cached.pack.wikivoyage?.revisionId, memberKey())
                schedulePublish()
                refreshInBackgroundIfDue(cached)
                return@launchGuideJob
            }
            hydrate(candidate, pinnedRevisionFor(candidate.qid))
        }
    }

    override fun useBookingSuggestion(suggestion: String) {
        if (!flags.guideEnabled) return
        val s = PlanManifestCodec.sanitizeLabel(suggestion) ?: return
        _uiState.update { it.copy(destinationQuery = s) }
        resolveAndChoose(s, s)
    }

    override fun retry() {
        if (!flags.guideEnabled) return
        viewModelScope.launch { syncNow() }
        when (val action = lastAction) {
            is RetryAction.Resolve -> resolveAndChoose(action.query, action.hint)
            is RetryAction.Hydrate -> launchGuideJob(background = false) {
                hydrate(action.candidate, pinnedRevisionFor(action.candidate.qid))
            }
            is RetryAction.Open -> openDestination(action.qid, action.revisionId, forceNetwork = stored != null)
            null -> manifest.destinationQid?.let {
                openDestination(it, manifest.wikivoyageRevisionId, forceNetwork = stored != null)
            }
        }
    }

    override fun acceptGuideUpdate() {
        val update = pendingUpdate ?: return
        pendingUpdate = null
        _uiState.update { it.copy(guideUpdateAvailable = false) }
        viewModelScope.launch {
            val saved = repository.persist(update) ?: transientStored(update)
            showStored(saved, cachedPartialMessage(saved.pack))
            repository.setDestination(groupId, saved.pack.destination.qid, saved.pack.wikivoyage?.revisionId, memberKey())
            schedulePublish()
            emit(TripGuideEffect.Snackbar(MSG_GUIDE_UPDATED))
        }
    }

    override fun setSaveOffline(enabled: Boolean) {
        _uiState.update { it.copy(saveOfflineEnabled = enabled) }
    }

    override fun togglePin(placeId: String) {
        if (!flags.guideEnabled) return
        viewModelScope.launch {
            val updated = repository.togglePin(groupId, placeId, memberKey())
            if (updated == null) {
                emit(TripGuideEffect.Snackbar(MSG_PLAN_EDIT_FAILED))
                return@launch
            }
            manifest = updated
            render()
            schedulePublish()
        }
    }

    override fun hidePlace(placeId: String) {
        if (!flags.guideEnabled) return
        viewModelScope.launch {
            val updated = repository.hide(groupId, placeId, memberKey())
            if (updated == null) {
                emit(TripGuideEffect.Snackbar(MSG_PLAN_EDIT_FAILED))
                return@launch
            }
            manifest = updated
            if (_uiState.value.selectedPlace?.id == placeId) _uiState.update { it.copy(selectedPlace = null) }
            render()
            schedulePublish()
            emit(TripGuideEffect.Snackbar(MSG_HIDDEN))
        }
    }

    override fun openPlace(place: Place) {
        _uiState.update { it.copy(selectedPlace = place) }
    }

    override fun dismissPlace() {
        _uiState.update { it.copy(selectedPlace = null) }
    }

    override fun directions(place: Place) {
        val loc = locationOrWarn(place) ?: return
        emit(TripGuideEffect.OpenMaps(MapsHandoff.geoUri(loc, place.name), MapsHandoff.searchUrl(loc)))
    }

    override fun navigate(place: Place, mode: TravelMode) {
        val loc = locationOrWarn(place) ?: return
        emit(TripGuideEffect.OpenMaps(MapsHandoff.navigationUri(loc, mode), MapsHandoff.directionsUrl(loc, null, mode)))
    }

    override fun openInOtherApp(place: Place) {
        val loc = locationOrWarn(place) ?: return
        emit(TripGuideEffect.OpenMapsChooser(MapsHandoff.geoUri(loc, place.name), MapsHandoff.searchUrl(loc), CHOOSER_TITLE))
    }

    override fun copyCoordinates(place: Place) {
        val loc = locationOrWarn(place) ?: return
        emit(TripGuideEffect.CopyToClipboard(COPY_LABEL, MapsHandoff.copyableCoordinates(loc)))
    }

    override fun sharePlace(place: Place) {
        val loc = locationOrWarn(place) ?: return
        emit(TripGuideEffect.ShareText(MapsHandoff.placeShareText(place.name, loc)))
    }

    override fun submitStayInput(text: String) {
        if (!flags.guideEnabled) return
        val input = text.trim().take(MAX_STAY_INPUT_CHARS)
        if (input.isEmpty()) {
            setStayFeedback(StayInputFeedback.Rejected(MSG_STAY_EMPTY))
            return
        }
        setStayFeedback(StayInputFeedback.Working)
        stayJob?.cancel()
        stayJob = viewModelScope.launch { handleStayResolution(repository.resolveStayText(input), fromNominatim = false) }
    }

    override fun searchStayByName(nameHint: String) {
        if (!flags.guideEnabled) return
        val name = nameHint.trim()
        if (name.isEmpty()) {
            setStayFeedback(StayInputFeedback.Rejected(NominatimGeocoder.REASON_EMPTY))
            return
        }
        if (!network.state.value.online) {
            setStayFeedback(StayInputFeedback.Rejected(MSG_STAY_OFFLINE))
            return
        }
        setStayFeedback(StayInputFeedback.Working)
        stayJob?.cancel()
        stayJob = viewModelScope.launch {
            val result = repository.geocodeStay(name, stored?.pack?.destination?.label)
            handleStayResolution(result, fromNominatim = true)
        }
    }

    override fun chooseSleepListing(place: Place) {
        val loc = place.location
        if (loc == null) {
            setStayFeedback(StayInputFeedback.Rejected(MSG_LISTING_NO_LOCATION))
            return
        }
        stayJob?.cancel()
        previewFromNominatim = false
        setStayFeedback(StayInputFeedback.Preview(loc, PlanManifestCodec.sanitizeLabel(place.name), approximate = false))
    }

    override fun confirmStayPreview() {
        val preview = _uiState.value.stayFeedback as? StayInputFeedback.Preview ?: return
        viewModelScope.launch {
            val key = memberKey()
            val stay = StayPin(
                location = preview.location,
                label = PlanManifestCodec.sanitizeLabel(preview.label) ?: DEFAULT_STAY_LABEL,
                precision = if (preview.approximate) CoordinatePrecision.APPROXIMATE else CoordinatePrecision.EXACT,
                setByMemberKey = key,
                setAtEpochMs = clock()
            )
            if (!repository.setLocalStay(groupId, stay, key)) {
                setStayFeedback(StayInputFeedback.Rejected(MSG_STAY_SAVE_FAILED))
                return@launch
            }
            stayFromNominatim = previewFromNominatim
            previewFromNominatim = false
            localStay = stay
            effectiveStay = effectiveOf(localStay, manifest.stay)
            _uiState.update { it.copy(stayFeedback = StayInputFeedback.Idle) }
            render()
            emit(TripGuideEffect.Snackbar(MSG_STAY_SET))
            schedulePublish()
            fetchNearbyAroundStay(stay)
        }
    }

    override fun clearStay() {
        viewModelScope.launch {
            val ok = repository.clearLocalStay(groupId, memberKey(), organizerKeys())
            stayFromNominatim = false
            previewFromNominatim = false
            _uiState.update { it.copy(stayFeedback = StayInputFeedback.Idle) }
            emit(TripGuideEffect.Snackbar(if (ok) MSG_STAY_CLEARED else MSG_STAY_CLEAR_FAILED))
            schedulePublish()
        }
    }

    override fun setShareStay(share: Boolean) {
        viewModelScope.launch {
            if (!repository.setShareStay(groupId, share, memberKey(), organizerKeys())) {
                emit(TripGuideEffect.Snackbar(MSG_PLAN_EDIT_FAILED))
                return@launch
            }
            schedulePublish()
        }
    }

    override fun reoptimizeLoop() {
        loopCacheKey = null
        render()
        emit(TripGuideEffect.Snackbar(MSG_LOOP_OPTIMISED))
    }

    override fun openWholeLoopInMaps() {
        val route = _uiState.value.loop.route
        if (route == null) {
            emit(TripGuideEffect.Snackbar(MSG_SET_STAY_FIRST))
            return
        }
        val urls = MapsHandoff.loopDirectionsUrls(route)
        if (urls.isEmpty()) {
            emit(TripGuideEffect.Snackbar(MSG_LOOP_EMPTY))
            return
        }
        emit(TripGuideEffect.OpenUrl(urls[0]))
        if (urls.size > 1) emit(TripGuideEffect.Snackbar(loopPartsMessage(urls.size)))
    }

    override fun shareLoop() {
        val route = _uiState.value.loop.route
        if (route == null || route.orderedStops.isEmpty()) {
            emit(TripGuideEffect.Snackbar(if (route == null) MSG_SET_STAY_FIRST else MSG_LOOP_EMPTY))
            return
        }
        emit(TripGuideEffect.ShareText(MapsHandoff.loopShareText(route, effectiveStay?.label)))
    }

    // =============================================================================================
    // Destination pipeline
    // =============================================================================================

    private fun resolveAndChoose(query: String, hint: String?) {
        lastAction = RetryAction.Resolve(query, hint)
        launchGuideJob(background = false) {
            if (!network.state.value.online) {
                failSoft(MSG_OFFLINE_SEARCH, retryable = true, offline = true)
                return@launchGuideJob
            }
            setPhase(GuidePhase.Resolving)
            when (val outcome = withNetwork { repository.resolveDestination(query, hint) }) {
                ResolveOutcome.NoResults -> failSoft(noResultsMessage(query), retryable = false)
                is ResolveOutcome.Failure -> failSoft(outcome.message, outcome.retryable)
                is ResolveOutcome.Candidates ->
                    if (outcome.ambiguous) {
                        setPhase(GuidePhase.Disambiguate(outcome.candidates.take(MAX_DISAMBIGUATION)))
                    } else {
                        val chosen = outcome.candidates.first()
                        lastAction = RetryAction.Hydrate(chosen)
                        val cached = repository.loadCachedPack(chosen.qid)
                        if (cached != null) {
                            showStored(cached, cachedPartialMessage(cached.pack))
                            repository.setDestination(groupId, chosen.qid, cached.pack.wikivoyage?.revisionId, memberKey())
                            schedulePublish()
                            refreshInBackgroundIfDue(cached)
                        } else {
                            hydrate(chosen, pinnedRevisionFor(chosen.qid))
                        }
                    }
            }
        }
    }

    /** Opens the manifest's destination: cache first, then network (unless offline). */
    private fun openDestination(qid: String, revisionId: Long?, forceNetwork: Boolean) {
        lastAction = RetryAction.Open(qid, revisionId)
        launchGuideJob(background = false) {
            if (!forceNetwork) {
                val cached = repository.loadCachedPack(qid)
                if (cached != null) {
                    showStored(cached, cachedPartialMessage(cached.pack))
                    val cachedRev = cached.pack.wikivoyage?.revisionId
                    if (revisionId != null && cachedRev != null && cachedRev != revisionId) {
                        rebuildForPinnedRevision(qid, revisionId)
                    } else {
                        refreshInBackgroundIfDue(cached)
                    }
                    return@launchGuideJob
                }
            }
            if (!network.state.value.online) {
                failSoft(MSG_OFFLINE_GUIDE, retryable = true, offline = true)
                return@launchGuideJob
            }
            hydratingQid = qid
            val current = stored?.pack
            val candidate = if (current != null && current.destination.qid == qid) {
                repository.candidateFor(current)
            } else {
                setPhase(GuidePhase.Resolving)
                withNetwork { repository.candidateForQid(qid) }
            }
            if (candidate == null) {
                hydratingQid = null
                failSoft(TripGuideRepository.MSG_NETWORK, retryable = false)
                return@launchGuideJob
            }
            hydrate(candidate, revisionId)
        }
    }

    /** The group moved to another Wikivoyage revision (planner accepted an update): rebuild once. */
    private suspend fun rebuildForPinnedRevision(qid: String, revisionId: Long) {
        if (revisionRebuildAttempted == (qid to revisionId)) return
        val ns = network.state.value
        if (!ns.online) return
        revisionRebuildAttempted = qid to revisionId
        val pack = stored?.pack ?: return
        val outcome = withNetwork { repository.buildPack(repository.candidateFor(pack), revisionId, effectiveStay?.location) }
        if (outcome is BuildOutcome.Built && outcome.pack.wikivoyage?.revisionId == revisionId) {
            val saved = repository.persist(outcome.pack) ?: transientStored(outcome.pack)
            if (stored?.pack?.destination?.qid == qid) showStored(saved, outcome.partialMessage)
        }
    }

    /** Network build + persist + manifest update. Must run inside the guide job. */
    private suspend fun hydrate(candidate: DestinationCandidate, pinnedRevisionId: Long?) {
        if (!network.state.value.online) {
            failSoft(MSG_OFFLINE_GUIDE, retryable = true, offline = true)
            return
        }
        hydratingQid = candidate.qid
        try {
            if (stored?.pack?.destination?.qid != candidate.qid) {
                stored = null
                partialMessage = null
                pendingUpdate = null
                _uiState.update { it.copy(guideUpdateAvailable = false) }
            }
            setPhase(GuidePhase.Hydrating(candidate))
            render()
            when (val outcome = withNetwork { repository.buildPack(candidate, pinnedRevisionId, effectiveStay?.location) }) {
                is BuildOutcome.Failure -> failSoft(outcome.message, outcome.retryable)
                is BuildOutcome.Built -> {
                    val saved = repository.persist(outcome.pack)
                    val partial = outcome.partialMessage ?: if (saved == null) MSG_SAVE_FAILED else null
                    showStored(saved ?: transientStored(outcome.pack), partial)
                    repository.setDestination(groupId, candidate.qid, outcome.pack.wikivoyage?.revisionId, memberKey())
                    schedulePublish()
                }
            }
        } finally {
            hydratingQid = null
        }
    }

    /** Soft-expired + online + unmetered => background refresh (never replaces a better pack). */
    private suspend fun refreshInBackgroundIfDue(cached: StoredPack) {
        if (!cached.softExpired) return
        val ns = network.state.value
        if (!ns.online || ns.metered) return
        guideJobIsBackground = true
        val outcome = withNetwork { repository.buildPack(repository.candidateFor(cached.pack), null, effectiveStay?.location) }
        if (outcome !is BuildOutcome.Built) return
        if (stored?.pack?.destination?.qid != cached.pack.destination.qid) return
        val oldRev = cached.pack.wikivoyage?.revisionId
        val newRev = outcome.pack.wikivoyage?.revisionId
        when {
            oldRev != null && newRev != null && newRev != oldRev -> {
                pendingUpdate = outcome.pack
                _uiState.update { it.copy(guideUpdateAvailable = true) }
            }
            outcome.pack.places.isEmpty() && cached.pack.places.isNotEmpty() -> Unit // degraded: keep cache
            else -> {
                val saved = repository.persist(outcome.pack) ?: return
                showStored(saved, outcome.partialMessage)
                if (oldRev == null && newRev != null) {
                    repository.setDestination(groupId, saved.pack.destination.qid, newRev, memberKey())
                    schedulePublish()
                }
            }
        }
    }

    /** LoopFetching: WDQS around a newly set stay, merged into the pack. */
    private fun fetchNearbyAroundStay(stay: StayPin) {
        val pack = stored?.pack ?: return
        if (!network.state.value.online || !flags.wdqsEnabled) return
        stayNearbyJob?.cancel()
        stayNearbyJob = viewModelScope.launch {
            val merged = withNetwork { repository.fetchNearbyInto(pack, stay.location) } ?: return@launch
            val saved = repository.persist(merged) ?: transientStored(merged)
            if (hydratingQid == null && stored?.pack?.destination?.qid == merged.destination.qid) {
                showStored(saved, partialMessage)
            }
        }
    }

    // =============================================================================================
    // Manifest / network / sync
    // =============================================================================================

    private suspend fun onManifestRow(row: TripPlanManifestEntity?) {
        manifest = row?.manifestJson?.let { PlanManifestCodec.decode(it) } ?: TripGuideStore.emptyManifest()
        localStay = row?.stayLocalJson?.let { PlanManifestCodec.decodeStay(it) }
        shareStay = row?.shareStay ?: false
        effectiveStay = effectiveOf(localStay, manifest.stay)
        if (effectiveStay == null) stayFromNominatim = false
        render()

        if (!flags.guideEnabled) return
        val qid = manifest.destinationQid ?: return
        val displayed = stored?.pack?.destination?.qid
        val jobActive = guideJob?.isActive == true
        if (jobActive && !guideJobIsBackground) return
        if (qid == hydratingQid) return
        if (qid != displayed) {
            openDestination(qid, manifest.wikivoyageRevisionId, forceNetwork = false)
            return
        }
        val pinnedRev = manifest.wikivoyageRevisionId ?: return
        val shownRev = stored?.pack?.wikivoyage?.revisionId ?: return
        if (pinnedRev != shownRev && pendingUpdate?.wikivoyage?.revisionId != pinnedRev && !jobActive) {
            launchGuideJob(background = true) { rebuildForPinnedRevision(qid, pinnedRev) }
        }
    }

    private fun onNetwork(state: NetworkState) {
        val wasOffline = _uiState.value.isOffline
        _uiState.update { it.copy(isOffline = !state.online) }
        render()
        if (wasOffline && state.online) {
            val phase = _uiState.value.phase
            if (phase is GuidePhase.Error && lastErrorWasOffline) retry()
            viewModelScope.launch { syncNow() }
        }
    }

    private fun applyFlags(newFlags: GuideFeatureFlags) {
        flags = newFlags
        _uiState.update {
            it.copy(
                guideEnabled = newFlags.guideEnabled,
                subView = if (newFlags.guideEnabled) it.subView else PlanSubView.BOOKINGS
            )
        }
        if (!newFlags.guideEnabled) {
            guideJob?.cancel()
            publishJob?.cancel()
        }
    }

    private suspend fun syncNow() {
        if (!flags.guideEnabled || !network.state.value.online) return
        repository.syncPlan(groupId, organizerKeys())
        repository.publishIfNeeded(groupId)
    }

    private fun schedulePublish() {
        if (!flags.guideEnabled) return
        publishJob?.cancel()
        publishJob = viewModelScope.launch {
            delay(publishDebounceMs)
            if (network.state.value.online) repository.publishIfNeeded(groupId)
        }
    }

    private fun startHeartbeat() {
        val period = heartbeatMs ?: return
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = viewModelScope.launch {
            while (true) {
                delay(period)
                if (visible) syncNow()
            }
        }
    }

    private fun consumeSharedStayText() {
        if (!visible || !flags.guideEnabled) return
        val text = sharedStayText.value ?: return
        if (!sharedStayText.compareAndSet(text, null)) return
        submitStayInput(text)
    }

    // =============================================================================================
    // Rendering
    // =============================================================================================

    private fun showStored(sp: StoredPack, partial: String?) {
        stored = sp
        partialMessage = partial
        lastErrorWasOffline = false
        setPhase(partial?.let { GuidePhase.Partial(it) } ?: GuidePhase.Ready)
        render()
    }

    private fun render() {
        val sp = stored
        val pack = sp?.pack
        val stayLoc = effectiveStay?.location
        val origin = stayLoc ?: pack?.destination?.location
        val offline = !network.state.value.online
        val loop = loopFor(pack, stayLoc)
        val feedback = _uiState.value.stayFeedback
        val showOsm = stayFromNominatim || (previewFromNominatim && feedback is StayInputFeedback.Preview)
        val eff = effectiveStay
        val isShared = eff != null && eff == manifest.stay && eff.setByMemberKey != memberKeyValue
        _uiState.update { s ->
            s.copy(
                networkInFlight = inFlight > 0,
                isOffline = offline,
                offlineSavedLabel = if (offline && sp != null) TripGuidePresenter.offlineLabel(sp.fetchedAtEpochMs, zoneId) else null,
                pack = pack,
                sections = if (pack == null) emptyList() else TripGuidePresenter.buildSections(pack, manifest, origin),
                attribution = if (pack == null) emptyList() else TripGuidePresenter.attribution(pack),
                saveOfflineSizeLabel = sp?.let { TripGuidePresenter.sizeLabel(it) },
                stay = StayUi(
                    stay = eff,
                    shareWithGroup = shareStay,
                    isShared = isShared,
                    attribution = if (showOsm) NominatimGeocoder.ATTRIBUTION else null,
                    sleepListings = pack?.let { TripGuidePresenter.sleepListings(it) } ?: emptyList()
                ),
                loop = loop
            )
        }
    }

    private fun loopFor(pack: GuidePack?, stay: LatLng?): LoopUi {
        if (pack == null) return EMPTY_LOOP
        val key = listOf(pack, manifest.pinned, manifest.hidden, stay)
        if (key == loopCacheKey) return loopCache
        val loop = TripGuidePresenter.buildLoop(pack, manifest, stay, repository.optimizer)
        loopCacheKey = key
        loopCache = loop
        return loop
    }

    private fun setPhase(phase: GuidePhase) {
        _uiState.update { it.copy(phase = phase) }
    }

    private fun setStayFeedback(feedback: StayInputFeedback) {
        _uiState.update { it.copy(stayFeedback = feedback) }
        render()
    }

    private fun handleStayResolution(result: StayResolution, fromNominatim: Boolean) {
        when (result) {
            is StayResolution.Resolved -> {
                previewFromNominatim = fromNominatim
                setStayFeedback(
                    StayInputFeedback.Preview(
                        location = result.location,
                        label = PlanManifestCodec.sanitizeLabel(result.label),
                        approximate = result.precision == CoordinatePrecision.APPROXIMATE
                    )
                )
            }
            is StayResolution.NoCoordinates -> {
                previewFromNominatim = false
                setStayFeedback(StayInputFeedback.NeedsFallback(PlanManifestCodec.sanitizeLabel(result.placeNameHint)))
            }
            is StayResolution.Rejected -> {
                previewFromNominatim = false
                setStayFeedback(StayInputFeedback.Rejected(PlanManifestCodec.sanitizeLabel(result.reason) ?: MSG_STAY_GENERIC))
            }
        }
    }

    /** Error with nothing to show => Error phase; otherwise keep the content and Snackbar. */
    private fun failSoft(message: String, retryable: Boolean, offline: Boolean = false) {
        if (stored != null) {
            setPhase(partialMessage?.let { GuidePhase.Partial(it) } ?: GuidePhase.Ready)
            emit(TripGuideEffect.Snackbar(message))
        } else {
            lastErrorWasOffline = offline
            setPhase(GuidePhase.Error(message, retryable))
        }
    }

    // =============================================================================================
    // Helpers
    // =============================================================================================

    private fun launchGuideJob(background: Boolean, block: suspend () -> Unit) {
        guideJob?.cancel()
        guideJobIsBackground = background
        guideJob = viewModelScope.launch { block() }
    }

    /** Runs a network call while flagging `networkInFlight` (nested calls are counted). */
    private suspend fun <T> withNetwork(block: suspend () -> T): T {
        inFlight++
        _uiState.update { it.copy(networkInFlight = true) }
        try {
            return block()
        } finally {
            inFlight--
            _uiState.update { it.copy(networkInFlight = inFlight > 0) }
        }
    }

    private suspend fun memberKey(): String? {
        if (!memberKeyLoaded) {
            memberKeyValue = quietly(null) { environment.memberKey() }?.let { PlanManifestCodec.sanitizeMemberKey(it) }
            memberKeyLoaded = true
        }
        return memberKeyValue
    }

    private suspend fun organizerKeys(): Set<String> = quietly(emptySet()) { environment.organizerKeys(groupId) }

    private fun pinnedRevisionFor(qid: String): Long? =
        if (manifest.destinationQid == qid) manifest.wikivoyageRevisionId else null

    private fun cachedPartialMessage(pack: GuidePack): String? = when {
        pack.wikivoyage == null && pack.places.isEmpty() && pack.nearby.isEmpty() -> TripGuideRepository.MSG_NOTHING_FOUND
        pack.wikivoyage == null || pack.places.isEmpty() -> TripGuideRepository.MSG_NO_CURATED
        else -> null
    }

    private fun transientStored(pack: GuidePack) = StoredPack(pack, 0, pack.fetchedAtEpochMs, softExpired = false)

    private fun locationOrWarn(place: Place): LatLng? {
        val loc = place.location
        if (loc == null) emit(TripGuideEffect.Snackbar(MSG_NO_PLACE_LOCATION))
        return loc
    }

    private fun emit(effect: TripGuideEffect) {
        _effects.trySend(effect)
    }

    private suspend fun <T> quietly(fallback: T, block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        fallback
    }

    companion object {
        const val HEARTBEAT_MS = 45_000L
        const val PUBLISH_DEBOUNCE_MS = 1_500L
        const val MIN_QUERY_CHARS = 2
        const val MAX_QUERY_CHARS = 100
        const val MAX_STAY_INPUT_CHARS = 2_000
        const val MAX_DISAMBIGUATION = 3
        const val DEFAULT_STAY_LABEL = "Our stay"
        const val CHOOSER_TITLE = "Open with"
        const val COPY_LABEL = "Coordinates"

        const val MSG_QUERY_TOO_SHORT = "Type at least 2 letters of a place name."
        const val MSG_OFFLINE_SEARCH = "You're offline. Connect to the internet to search for a destination."
        const val MSG_OFFLINE_GUIDE = "You're offline. Connect to the internet to load this guide."
        const val MSG_SAVE_FAILED = "Couldn't save this guide for offline use."
        const val MSG_GUIDE_UPDATED = "Guide updated"
        const val MSG_PLAN_EDIT_FAILED = "Couldn't update the trip plan. Try again."
        const val MSG_HIDDEN = "Hidden from this trip's guide"
        const val MSG_NO_PLACE_LOCATION = "This place has no map location."
        const val MSG_STAY_EMPTY = "Paste a map link or coordinates."
        const val MSG_STAY_OFFLINE = "You're offline. Paste coordinates instead."
        const val MSG_STAY_GENERIC = "Couldn't read that location."
        const val MSG_LISTING_NO_LOCATION = "This listing has no map location. Try another option."
        const val MSG_STAY_SAVE_FAILED = "Couldn't save your stay. Try again."
        const val MSG_STAY_SET = "Stay set"
        const val MSG_STAY_CLEARED = "Stay cleared"
        const val MSG_STAY_CLEAR_FAILED = "Couldn't clear the stay. Try again."
        const val MSG_LOOP_OPTIMISED = "Loop optimised"
        const val MSG_SET_STAY_FIRST = "Set your stay to build a loop."
        const val MSG_LOOP_EMPTY = "Pin a few places to build a loop."

        internal val EMPTY_LOOP = LoopUi(null, false, emptyList(), false, null, emptyList())

        fun noResultsMessage(query: String): String =
            "No places found for \"${PlanManifestCodec.sanitizeLabel(query) ?: ""}\". Try another spelling."

        fun loopPartsMessage(parts: Int): String =
            "Opened part 1 of $parts in Maps. Share the loop to get every part."

        /** The loop start: the newer of this device's stay and the group's shared stay. */
        fun effectiveOf(local: StayPin?, shared: StayPin?): StayPin? = when {
            local == null -> shared
            shared == null -> local
            shared.setAtEpochMs > local.setAtEpochMs -> shared
            else -> local
        }

        /** ViewModelProvider factory backed by the app-scoped [TripGuideServices]. */
        fun factory(context: Context, groupId: String): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TripGuideServices.createViewModel(appContext, groupId) as T
            }
        }
    }
}

/** Pure mapping from domain data to contract UI models (deterministic; JVM-testable). */
internal object TripGuidePresenter {
    const val SOURCE_WIKIDATA = "From Wikidata"
    const val INITIALLY_VISIBLE = 3
    const val WIKIDATA_NEARBY_ATTRIBUTION = "Nearby landmarks from Wikidata, CC0 1.0: https://www.wikidata.org/"
    private const val THUMB_ESTIMATE_BYTES = 25_000L
    private const val HERO_ESTIMATE_BYTES = 90_000L

    val SECTIONS: List<Pair<PlaceKind, String>> = listOf(
        PlaceKind.SEE to "Must See",
        PlaceKind.DO to "Things to Do",
        PlaceKind.EAT to "Local Eats"
    )

    val LOOP_CAVEATS: List<String> = listOf(
        "Straight-line estimate · ≈ road ×1.35",
        "River crossings / ferries not considered",
        "Monuments often close at sunset"
    )

    private val SAVED_ON: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    /** Curated places then nearby, de-duplicated by id and by QID (Wikivoyage wins). */
    fun mergedPlaces(pack: GuidePack): List<Place> {
        val out = LinkedHashMap<String, Place>()
        val qids = HashSet<String>()
        for (p in pack.places + pack.nearby) {
            if (out.containsKey(p.id)) continue
            val q = p.qid
            if (q != null && !qids.add(q)) continue
            out[p.id] = p
        }
        return out.values.toList()
    }

    fun buildSections(pack: GuidePack, manifest: PlanManifest, origin: LatLng?): List<GuideSection> {
        val pinned = PlanManifestMerger.visiblePinnedIds(manifest)
        val visible = mergedPlaces(pack).filter { !TripGuideRepository.isHiddenForExplore(manifest, it.id) }
        return SECTIONS.mapNotNull { (kind, title) ->
            val cards = visible.filter { it.kind == kind }.map { p ->
                PlaceCardUi(
                    place = p,
                    distanceLabel = distanceLabel(origin, p.location),
                    isPinned = p.id in pinned,
                    isExcerpt = isExcerpt(p),
                    sourceLabel = if (p.source == PlaceSource.WIKIDATA) SOURCE_WIKIDATA else null
                )
            }
            if (cards.isEmpty()) null else GuideSection(kind, title, cards, INITIALLY_VISIBLE)
        }
    }

    /** "1.2 km ≈" (road estimate = straight line × 1.35), or null without both points. */
    fun distanceLabel(origin: LatLng?, target: LatLng?): String? {
        if (origin == null || target == null) return null
        val road = WikimediaGuideSource.haversineKm(origin, target) * LoopRoute.ROAD_FACTOR
        return if (road >= 100.0) String.format(Locale.ROOT, "%.0f km ≈", road)
        else String.format(Locale.ROOT, "%.1f km ≈", road)
    }

    /** Heuristic: the content engine caps blurbs at 280 chars and marks cuts with an ellipsis. */
    fun isExcerpt(place: Place): Boolean {
        val b = place.blurb ?: return false
        return b.length >= 279 || b.endsWith("…") || b.endsWith("...")
    }

    fun sleepListings(pack: GuidePack): List<Place> = pack.places.filter { it.kind == PlaceKind.SLEEP }

    fun attribution(pack: GuidePack): List<String> =
        if (pack.nearby.any { it.source == PlaceSource.WIKIDATA }) pack.attribution + WIKIDATA_NEARBY_ATTRIBUTION
        else pack.attribution

    fun buildLoop(pack: GuidePack, manifest: PlanManifest, stay: LatLng?, optimizer: LoopOptimizer): LoopUi {
        val selection = LoopStopSelector.select(pack.places + pack.nearby, manifest.pinned, manifest.hidden, stay)
        val route = stay?.let { optimizer.optimize(it, selection.stops) }
        val hasStops = route != null && route.orderedStops.isNotEmpty()
        return LoopUi(
            route = route,
            autoSuggested = selection.autoSuggested,
            stopsWithoutCoordinates = selection.pinnedWithoutCoordinates,
            truncated = selection.truncated,
            totalLabel = if (hasStops) totalLabel(route!!) else null,
            caveats = if (hasStops) LOOP_CAVEATS else emptyList()
        )
    }

    /** "6 stops · ≈ 14.2 km" (road estimate). */
    fun totalLabel(route: LoopRoute): String {
        val n = route.orderedStops.size
        val stops = if (n == 1) "1 stop" else "$n stops"
        return stops + " · ≈ " + String.format(Locale.ROOT, "%.1f km", route.totalRoadEstimateKm)
    }

    /** Offline download size estimate: pack + thumbnails (+ hero), e.g. "0.9 MB". */
    fun sizeLabel(stored: StoredPack): String {
        val images = (stored.pack.places + stored.pack.nearby).mapNotNull { it.image?.fileName }.distinct().size
        val hero = if (stored.pack.destination.hero != null) HERO_ESTIMATE_BYTES else 0L
        val bytes = stored.gzipBytes + images * THUMB_ESTIMATE_BYTES + hero
        val mb = maxOf(0.1, bytes / (1024.0 * 1024.0))
        return String.format(Locale.ROOT, "%.1f MB", mb)
    }

    fun offlineLabel(fetchedAtEpochMs: Long, zoneId: ZoneId): String =
        "Offline · guide saved on " + SAVED_ON.format(Instant.ofEpochMilli(fetchedAtEpochMs).atZone(zoneId))
}
