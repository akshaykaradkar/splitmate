package com.splitmate.app.data.guide

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.splitmate.app.data.CloudGroupSyncRepository
import com.splitmate.app.data.PhoneIdentityValidator
import com.splitmate.app.data.SplitMateDao
import com.splitmate.app.data.SplitMateRoomDatabase
import com.splitmate.app.data.guide.content.CommonsImageClient
import com.splitmate.app.data.guide.content.WikimediaGuideSource
import com.splitmate.app.data.guide.flags.FileGuideKeyValueStore
import com.splitmate.app.data.guide.flags.GuideFeatureFlagsProvider
import com.splitmate.app.data.guide.loop.NominatimGeocoder
import com.splitmate.app.data.guide.loop.OkHttpRedirectFetcher
import com.splitmate.app.data.guide.loop.OkHttpTextFetcher
import com.splitmate.app.data.guide.loop.ShortLinkResolver
import com.splitmate.app.data.guide.loop.WdqsNearbySource
import com.splitmate.app.data.guide.sync.NtfyPlanManifestSync
import com.splitmate.app.data.guide.sync.TripGuideStore
import com.splitmate.app.ui.extractTravelTicketFromTitle
import com.splitmate.app.ui.loadPersistedPnrSnapshot
import com.splitmate.app.ui.resolveStationDisplayName
import com.splitmate.app.ui.screens.plan.TripGuideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * App-scoped, lazily created v2.3.4 Trip Guide singletons (no DI in this repo).
 *
 * Nothing here runs at app start: the object only holds two tiny flows until the Plan tab host
 * first asks for a ViewModel ([createViewModel]); only then are the Wikimedia/WDQS/Nominatim
 * clients, the Room-backed [TripGuideStore], the ntfy plan sync, the flags provider and the
 * connectivity monitor built (each once per process).
 */
object TripGuideServices {

    // ---- share-intent intake (MainActivity -> ViewModel) ----------------------------------------

    private val _pendingSharedStayText = MutableStateFlow<String?>(null)

    /**
     * Map text shared into the app (ACTION_SEND). The opened trip's Plan-tab ViewModel consumes it
     * (compare-and-set to null) and auto-submits it to the stay sheet.
     */
    val pendingSharedStayText: StateFlow<String?> = _pendingSharedStayText.asStateFlow()

    /** Called by MainActivity for share text accepted by [MapShareTextDetector]. */
    fun offerSharedStayText(text: String) {
        val clean = text.trim().take(MapShareTextDetector.MAX_SHARED_CHARS)
        if (clean.isNotEmpty()) _pendingSharedStayText.value = clean
    }

    // ---- live-stream plan events (SplitMateAppComposable -> ViewModel) ---------------------------

    private val _planSyncRequests = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Group ids whose plan topic changed on the live stream. */
    val planSyncRequests: SharedFlow<String> = _planSyncRequests.asSharedFlow()

    /**
     * Live-stream hook: a `splitmate_v2_plan_*` topic fired for the opened trip. Triggers a plan
     * fetch/merge in that trip's Plan ViewModel (if the Plan tab has been opened; otherwise the
     * next open polls anyway). Never touches the ledger sync.
     */
    fun onPlanTopicChanged(groupId: String) {
        if (groupId.isNotBlank()) _planSyncRequests.tryEmit(groupId)
    }

    /** True when [topic] is a plan-manifest topic (not a ledger topic). */
    fun isPlanTopic(topic: String?): Boolean = topic?.startsWith(NtfyPlanManifestSync.TOPIC_PREFIX) == true

    // ---- lazy graph -----------------------------------------------------------------------------

    @Volatile private var graph: Graph? = null

    private class Graph(appContext: Context) {
        val dao: SplitMateDao = SplitMateRoomDatabase.getInstance(appContext).dao()
        val network: ConnectivityNetworkMonitor = ConnectivityNetworkMonitor(appContext)
        val store: TripGuideStore = TripGuideStore.fromDao(dao)
        val flagsProvider = GuideFeatureFlagsProvider(
            store = FileGuideKeyValueStore(File(appContext.filesDir, "guide_flags_cache.json"))
        )
        private val flagsNow = { flagsProvider.current() }
        private val unmeteredSource by lazy { WikimediaGuideSource() }
        private val meteredSource by lazy { WikimediaGuideSource(heroWidth = CommonsImageClient.WIDTH_METERED) }
        private val textFetcher by lazy { OkHttpTextFetcher() }
        val shortLinks by lazy {
            ShortLinkResolver(OkHttpRedirectFetcher(), isOnline = { network.state.value.online }, flags = flagsNow)
        }
        val nominatim by lazy { NominatimGeocoder(textFetcher, flagsNow) }
        val wdqs by lazy { WdqsNearbySource(textFetcher, flagsNow) }
        val planSync = NtfyPlanManifestSync(shareStayForGroup = { gid -> store.isShareStay(gid) })
        val environment = AndroidTripGuideEnvironment(appContext, dao)

        val repository = TripGuideRepository(
            content = WikimediaGuideContentSource(
                unmetered = { unmeteredSource },
                metered = { meteredSource },
                isMetered = { network.state.value.metered }
            ),
            nearby = wdqs,
            stayResolver = shortLinks,
            geocoder = NominatimStayGeocoder(nominatim),
            store = store,
            planSync = NtfyPlanSyncTransport(planSync),
            flags = ProviderGuideFlagsSource(flagsProvider)
        )
    }

    private fun graph(context: Context): Graph =
        graph ?: synchronized(this) {
            graph ?: Graph(context.applicationContext).also { graph = it }
        }

    /** The app-scoped repository (builds the graph on first use). */
    fun repository(context: Context): TripGuideRepository = graph(context).repository

    /** Builds a Plan-tab ViewModel for [groupId]; used by [TripGuideViewModel.factory]. */
    fun createViewModel(context: Context, groupId: String): TripGuideViewModel {
        val g = graph(context)
        return TripGuideViewModel(
            groupId = groupId,
            repository = g.repository,
            network = g.network,
            environment = g.environment,
            sharedStayText = _pendingSharedStayText,
            planSyncRequests = planSyncRequests
        )
    }
}

/**
 * [NetworkMonitor] over the default-network callback. Metered-aware (hero 640 px, no background
 * refresh on metered). Registered once per process; failures fall back to "online, unmetered"
 * like CloudGroupSyncRepository.isInternetAvailable.
 */
internal class ConnectivityNetworkMonitor(context: Context) : NetworkMonitor {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val _state = MutableStateFlow(readNow())
    override val state: StateFlow<NetworkState> = _state.asStateFlow()

    init {
        runCatching {
            cm?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _state.value = readNow()
                }

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _state.value = NetworkState(
                        online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                        metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    )
                }

                override fun onLost(network: Network) {
                    _state.value = readNow()
                }
            })
        }
    }

    private fun readNow(): NetworkState {
        val manager = cm ?: return NetworkState(online = true, metered = false)
        return try {
            val active = manager.activeNetwork ?: return NetworkState(online = false, metered = false)
            val caps = manager.getNetworkCapabilities(active) ?: return NetworkState(online = false, metered = false)
            NetworkState(
                online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                metered = manager.isActiveNetworkMetered
            )
        } catch (_: Exception) {
            NetworkState(online = true, metered = false)
        }
    }
}

/**
 * [TripGuideEnvironment] over Room (profile, bookings), the persisted PNR snapshots and the
 * ledger's organizer roles (CloudGroupSyncRepository prefs). Organizer keys are the keys whose
 * signed role epoch is positive (negative = demoted), matching the ledger's own semantics.
 */
internal class AndroidTripGuideEnvironment(
    private val context: Context,
    private val dao: SplitMateDao
) : TripGuideEnvironment {

    override suspend fun memberKey(): String? = withContext(Dispatchers.IO) {
        val phone = dao.getUserProfile()?.userPhone.orEmpty()
        PhoneIdentityValidator.normalizeIndianPhone10(phone).takeIf { it.length == 10 }
    }

    override suspend fun organizerKeys(groupId: String): Set<String> = withContext(Dispatchers.IO) {
        CloudGroupSyncRepository.getOrganizerRolesByKey(context, groupId)
            .filter { (key, role) -> key.isNotBlank() && role > 0L }
            .keys
            .toSortedSet()
    }

    override suspend fun bookingDestinationNames(groupId: String): List<String> = withContext(Dispatchers.IO) {
        dao.getExpensesForGroup(groupId)
            .sortedWith(compareBy({ it.scheduledAtEpochMs ?: it.createdAt }, { it.expenseId }))
            .mapNotNull { expense ->
                val ticket = runCatching { extractTravelTicketFromTitle(expense.title) }.getOrNull()
                val pnr = expense.travelPnr.filter { it.isDigit() }.ifBlank { ticket?.pnr.orEmpty() }
                if (pnr.isBlank() && ticket?.hasTicketMetadata != true) return@mapNotNull null
                val snapshot = if (pnr.length == 10) runCatching { loadPersistedPnrSnapshot(context, pnr) }.getOrNull() else null
                snapshot?.toStationName?.takeIf { it.isNotBlank() }
                    ?: (snapshot?.toStation?.takeIf { it.isNotBlank() } ?: ticket?.toStation?.takeIf { it.isNotBlank() })
                        ?.let { resolveStationDisplayName(it) }
            }
    }
}
