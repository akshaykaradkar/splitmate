package com.splitmate.app.guide.vm

import com.splitmate.app.data.TripGuidePackEntity
import com.splitmate.app.data.TripPlanManifestEntity
import com.splitmate.app.data.guide.CommonsImage
import com.splitmate.app.data.guide.Destination
import com.splitmate.app.data.guide.DestinationCandidate
import com.splitmate.app.data.guide.GuideContentSource
import com.splitmate.app.data.guide.GuideFeatureFlags
import com.splitmate.app.data.guide.GuideFlagsSource
import com.splitmate.app.data.guide.GuidePack
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.NearbySource
import com.splitmate.app.data.guide.NetworkMonitor
import com.splitmate.app.data.guide.NetworkState
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.data.guide.PlanManifest
import com.splitmate.app.data.guide.PlanSyncTransport
import com.splitmate.app.data.guide.StayGeocoder
import com.splitmate.app.data.guide.StayLocationResolver
import com.splitmate.app.data.guide.StayResolution
import com.splitmate.app.data.guide.TripGuideEnvironment
import com.splitmate.app.data.guide.TripGuideRepository
import com.splitmate.app.data.guide.WikivoyageRef
import com.splitmate.app.data.guide.sync.NtfyPlanMessage
import com.splitmate.app.data.guide.sync.TripGuideLocalSource
import com.splitmate.app.data.guide.sync.TripGuideStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fakes and fixtures for the v2.3.4 Plan-tab orchestration tests (no network, no Room).
 * Places and coordinates are modelled on Hampi (Karnataka) so distances are realistic.
 */
object GuideVmFixtures {
    const val GROUP = "g_hampi"
    const val HAMPI_QID = "Q1066187"
    const val HOSAPETE_QID = "Q1026937"
    const val REV_1 = 6_100_000L
    const val REV_2 = 6_200_000L
    const val MEMBER = "9876543210"

    /** 2024-09-29T12:00:00Z */
    const val NOW = 1_727_611_200_000L
    const val DAY = 24L * 60L * 60L * 1000L

    val HAMPI_CENTER = LatLng(15.3350, 76.4600)
    val STAY_LOC = LatLng(15.3440, 76.4550)

    val hampiCandidate = DestinationCandidate(HAMPI_QID, "Hampi", "Village in Karnataka, India", HAMPI_CENTER, "Hampi", "Q668", 0.9)
    val hosapeteCandidate = DestinationCandidate(HOSAPETE_QID, "Hosapete", "City in Karnataka, India", LatLng(15.2689, 76.3909), "Hosapete", "Q668", 0.6)

    fun place(id: String, kind: PlaceKind, name: String, loc: LatLng?, source: PlaceSource = PlaceSource.WIKIVOYAGE, blurb: String? = null, qid: String? = null) =
        Place(id = id, kind = kind, name = name, location = loc, qid = qid, blurb = blurb, source = source)

    val virupaksha = place("wv:see:virupaksha-temple", PlaceKind.SEE, "Virupaksha Temple", LatLng(15.3350, 76.4590), qid = "Q2531426", blurb = "Active 7th-century temple dedicated to Shiva.")
    val vittala = place("wv:see:vittala-temple", PlaceKind.SEE, "Vittala Temple", LatLng(15.3420, 76.4750), qid = "Q3530547")
    val lotusMahal = place("wv:see:lotus-mahal", PlaceKind.SEE, "Lotus Mahal", LatLng(15.3180, 76.4710))
    val coracle = place("wv:do:coracle-ride", PlaceKind.DO, "Coracle ride", LatLng(15.3380, 76.4700))
    val mango = place("wv:eat:mango-tree", PlaceKind.EAT, "Mango Tree", LatLng(15.3355, 76.4580))
    val noCoords = place("wv:do:bouldering", PlaceKind.DO, "Bouldering", null)
    val mayura = place("wv:sleep:hotel-mayura-bhuvaneswari", PlaceKind.SLEEP, "Hotel Mayura Bhuvaneswari", LatLng(15.3440, 76.4550))
    val hemakuta = place("wd:Q5712063", PlaceKind.SEE, "Hemakuta hill", LatLng(15.3340, 76.4580), source = PlaceSource.WIKIDATA, qid = "Q5712063")
    val virupakshaWd = place("wd:Q2531426", PlaceKind.SEE, "Virupaksha Temple (Wikidata)", LatLng(15.3350, 76.4590), source = PlaceSource.WIKIDATA, qid = "Q2531426")

    val curated = listOf(virupaksha, vittala, lotusMahal, coracle, mango, noCoords, mayura)

    fun pack(
        qid: String = HAMPI_QID,
        label: String = "Hampi",
        rev: Long? = REV_1,
        places: List<Place> = curated,
        nearby: List<Place> = emptyList(),
        fetchedAt: Long = NOW,
        hero: Boolean = true
    ) = GuidePack(
        destination = Destination(
            qid, label, "Village in Karnataka, India", HAMPI_CENTER,
            if (hero) CommonsImage("Hampi_view.jpg", "https://upload.wikimedia.org/x/Hampi_view.jpg", 1280, "A. Photographer", "CC BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/", "https://commons.wikimedia.org/wiki/File:Hampi_view.jpg") else null
        ),
        wikivoyage = rev?.let { WikivoyageRef(label, it, "https://en.wikivoyage.org/wiki/$label?oldid=$it") },
        places = places,
        nearby = nearby,
        attribution = if (rev != null) listOf("Travel content from Wikivoyage, CC BY-SA 4.0") else emptyList(),
        fetchedAtEpochMs = fetchedAt
    )

    fun manifest(
        qid: String? = HAMPI_QID,
        rev: Long? = REV_1,
        pinned: Map<String, Long> = emptyMap(),
        hidden: Map<String, Long> = emptyMap(),
        updatedAt: Long = NOW - DAY,
        by: String? = "9123456789"
    ) = PlanManifest(
        destinationQid = qid, wikivoyageRevisionId = rev, stay = null, pinned = pinned, hidden = hidden,
        days = emptyMap(), updatedAtEpochMs = updatedAt, updatedByMemberKey = by
    )
}

/** Room stand-in (no foreign keys). */
class InMemoryTripGuideLocalSource : TripGuideLocalSource {
    val packs = MutableStateFlow<Map<String, TripGuidePackEntity>>(emptyMap())
    val manifests = MutableStateFlow<Map<String, TripPlanManifestEntity>>(emptyMap())

    override suspend fun getPack(destinationQid: String) = packs.value[destinationQid]
    override fun observePack(destinationQid: String): Flow<TripGuidePackEntity?> = packs.map { it[destinationQid] }
    override suspend fun upsertPack(pack: TripGuidePackEntity) {
        packs.value = packs.value + (pack.destinationQid to pack)
    }
    override suspend fun deleteHardExpiredPacks(nowEpochMs: Long): Int {
        val (expired, kept) = packs.value.values.partition { it.hardExpiryEpochMs <= nowEpochMs }
        packs.value = kept.associateBy { it.destinationQid }
        return expired.size
    }
    override suspend fun getManifest(groupId: String) = manifests.value[groupId]
    override fun observeManifest(groupId: String): Flow<TripPlanManifestEntity?> = manifests.map { it[groupId] }
    override suspend fun upsertManifest(entity: TripPlanManifestEntity) {
        manifests.value = manifests.value + (entity.groupId to entity)
    }
    override suspend fun getPendingManifests() = manifests.value.values.filter { it.pendingPush }
}

class FakeContentSource : GuideContentSource {
    var candidates: List<DestinationCandidate> = listOf(GuideVmFixtures.hampiCandidate)
    var ambiguous = false
    var resolveError: Exception? = null
    var buildError: Exception? = null
    var packFor: (DestinationCandidate, Long?) -> GuidePack = { c, rev ->
        GuideVmFixtures.pack(qid = c.qid, label = c.label, rev = rev ?: GuideVmFixtures.REV_1)
    }
    /** When set, buildPack suspends until completed (lets tests observe Hydrating/networkInFlight). */
    var buildGate: CompletableDeferred<Unit>? = null
    var resolveCalls = 0
    var buildCalls = 0
    val pinnedRevisions = mutableListOf<Long?>()

    override suspend fun resolve(query: String, bookingHint: String?): List<DestinationCandidate> {
        resolveCalls++
        resolveError?.let { throw it }
        return if (query.startsWith("Q")) candidates.filter { it.qid == query } else candidates
    }

    override fun isAmbiguous(candidates: List<DestinationCandidate>) = ambiguous

    override suspend fun buildPack(candidate: DestinationCandidate, pinnedRevisionId: Long?): GuidePack {
        buildCalls++
        pinnedRevisions += pinnedRevisionId
        buildGate?.await()
        buildError?.let { throw it }
        return packFor(candidate, pinnedRevisionId)
    }
}

class FakeNearbySource : NearbySource {
    var results: List<Place> = emptyList()
    var error: Exception? = null
    val centers = mutableListOf<LatLng>()
    override suspend fun nearby(center: LatLng, kinds: Set<PlaceKind>, minResults: Int): List<Place> {
        centers += center
        error?.let { throw it }
        return results
    }
}

class FakeStayResolver : StayLocationResolver {
    var result: StayResolution = StayResolution.Resolved(GuideVmFixtures.STAY_LOC, "Hotel Mayura Bhuvaneswari", com.splitmate.app.data.guide.CoordinatePrecision.EXACT)
    val inputs = mutableListOf<String>()
    override suspend fun resolve(input: String): StayResolution {
        inputs += input
        return result
    }
}

class FakeGeocoder : StayGeocoder {
    var result: StayResolution = StayResolution.Resolved(GuideVmFixtures.STAY_LOC, "Hampi Heritage Homestay", com.splitmate.app.data.guide.CoordinatePrecision.APPROXIMATE)
    val calls = mutableListOf<Pair<String, String?>>()
    override suspend fun geocode(nameHint: String, destinationLabel: String?): StayResolution {
        calls += nameHint to destinationLabel
        return result
    }
}

class FakeTransport : PlanSyncTransport {
    var inbox: List<NtfyPlanMessage> = emptyList()
    val fetchCursors = mutableListOf<String?>()
    val published = mutableListOf<PlanManifest>()
    var publishResult: String? = "abc123XYZ"

    override suspend fun fetchAll(groupId: String, sinceMessageId: String?): List<NtfyPlanMessage> {
        fetchCursors += sinceMessageId
        val out = inbox
        inbox = emptyList()
        return out
    }

    override suspend fun publish(groupId: String, manifest: PlanManifest): String? {
        if (publishResult != null) published += manifest
        return publishResult
    }
}

class FakeFlags(
    var now: GuideFeatureFlags = GuideFeatureFlags(),
    var afterRefresh: GuideFeatureFlags = now
) : GuideFlagsSource {
    var refreshCalls = 0
    override fun current() = now
    override suspend fun refresh(): GuideFeatureFlags {
        refreshCalls++
        now = afterRefresh
        return now
    }
}

class FakeEnvironment(
    var member: String? = GuideVmFixtures.MEMBER,
    var organizers: Set<String> = setOf("9123456789"),
    var bookingNames: List<String> = listOf("HPT (Hosapete Jn)")
) : TripGuideEnvironment {
    override suspend fun memberKey() = member
    override suspend fun organizerKeys(groupId: String) = organizers
    override suspend fun bookingDestinationNames(groupId: String) = bookingNames
}

class FakeNetwork(initial: NetworkState = NetworkState(online = true, metered = false)) : NetworkMonitor {
    private val flow = MutableStateFlow(initial)
    override val state: StateFlow<NetworkState> = flow
    fun set(online: Boolean, metered: Boolean = false) {
        flow.value = NetworkState(online, metered)
    }
}

/** Wires a real [TripGuideRepository] + [TripGuideStore] over the fakes. */
class GuideTestGraph(dispatcher: CoroutineDispatcher, var now: Long = GuideVmFixtures.NOW) {
    val local = InMemoryTripGuideLocalSource()
    val content = FakeContentSource()
    val nearby = FakeNearbySource()
    val stayResolver = FakeStayResolver()
    val geocoder = FakeGeocoder()
    val transport = FakeTransport()
    val flags = FakeFlags()
    val environment = FakeEnvironment()
    val network = FakeNetwork()
    val store = TripGuideStore(local) { now }
    val repository = TripGuideRepository(
        content = content,
        nearby = nearby,
        stayResolver = stayResolver,
        geocoder = geocoder,
        store = store,
        planSync = transport,
        flags = flags,
        clock = { now },
        cpuDispatcher = dispatcher
    )
}
