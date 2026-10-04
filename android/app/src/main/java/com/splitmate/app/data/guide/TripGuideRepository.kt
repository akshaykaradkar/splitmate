package com.splitmate.app.data.guide

import com.splitmate.app.data.guide.content.GuideContentException
import com.splitmate.app.data.guide.content.GuidePackCodec
import com.splitmate.app.data.guide.content.GuidePackFormatException
import com.splitmate.app.data.guide.content.WikimediaGuideSource
import com.splitmate.app.data.guide.flags.GuideFeatureFlagsProvider
import com.splitmate.app.data.guide.loop.DeterministicLoopOptimizer
import com.splitmate.app.data.guide.loop.NominatimGeocoder
import com.splitmate.app.data.guide.sync.NtfyPlanManifestSync
import com.splitmate.app.data.guide.sync.NtfyPlanMessage
import com.splitmate.app.data.guide.sync.PlanManifestCodec
import com.splitmate.app.data.guide.sync.PlanManifestMerger
import com.splitmate.app.data.guide.sync.TripGuideStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import com.splitmate.app.data.TripPlanManifestEntity
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

/*
 * v2.3.4 Trip Guide orchestration (Wave 2 stream E1).
 *
 * Everything in this file is pure Kotlin (no Android framework types) so the whole Plan-tab
 * pipeline is JVM unit-testable with fakes. Android-backed implementations of the seams below
 * live in TripGuideServices.kt and are created lazily on first Plan-tab use (zero cold-start cost).
 *
 * Rules honoured here:
 * - Guide work never touches money flows; every failure degrades to a user-safe message.
 * - No logging at all (payloads may contain a stay location).
 * - "Sync intents, recompute derivations": only the PlanManifest travels; packs are rebuilt
 *   locally (pinned Wikivoyage revision) and loops are recomputed on every device.
 */

// -------------------------------------------------------------------------------------------------
// Seams
// -------------------------------------------------------------------------------------------------

/** Connectivity snapshot. `metered` drives the 640 px hero width and blocks background refresh. */
data class NetworkState(val online: Boolean, val metered: Boolean)

/** Observable connectivity (Android: ConnectivityManager default-network callback). */
interface NetworkMonitor {
    val state: StateFlow<NetworkState>
}

/**
 * Content engine seam: [WikimediaGuideSource] exposes `resolve(query, bookingHint)` and
 * `isAmbiguous` outside the shared [GuideSource] contract, so the repository codes against this.
 */
interface GuideContentSource {
    suspend fun resolve(query: String, bookingHint: String?): List<DestinationCandidate>
    fun isAmbiguous(candidates: List<DestinationCandidate>): Boolean
    suspend fun buildPack(candidate: DestinationCandidate, pinnedRevisionId: Long?): GuidePack
}

/** User-initiated name -> coordinate fallback (Nominatim). */
interface StayGeocoder {
    suspend fun geocode(nameHint: String, destinationLabel: String?): StayResolution
}

/** Plan-topic transport (ntfy). */
interface PlanSyncTransport {
    /** Valid manifests after [sinceMessageId] (or the last 12 h), oldest first; empty on failure. */
    suspend fun fetchAll(groupId: String, sinceMessageId: String?): List<NtfyPlanMessage>

    /** Publishes; returns the ntfy message id ("" when unknown) or null on failure. */
    suspend fun publish(groupId: String, manifest: PlanManifest): String?
}

/** Remote kill-switches. */
interface GuideFlagsSource {
    fun current(): GuideFeatureFlags
    suspend fun refresh(): GuideFeatureFlags
}

/** App/ledger facts the Plan tab needs (Android: Room + CloudGroupSyncRepository prefs). */
interface TripGuideEnvironment {
    /** Current user's member key (normalised 10-digit phone), or null when unknown. */
    suspend fun memberKey(): String?

    /** Ledger organizer keys for the group (same set on every device; used for merge tie-breaks). */
    suspend fun organizerKeys(groupId: String): Set<String>

    /** Raw arrival station/city names from the group's train/flight bookings, chronological. */
    suspend fun bookingDestinationNames(groupId: String): List<String>
}

// -------------------------------------------------------------------------------------------------
// Adapters over the Wave-1 implementations
// -------------------------------------------------------------------------------------------------

/**
 * [GuideContentSource] over [WikimediaGuideSource]. Two instances are kept (lazily): a default one
 * and a metered one with 640 px heroes; [isMetered] picks per call.
 */
class WikimediaGuideContentSource(
    private val unmetered: () -> WikimediaGuideSource,
    private val metered: () -> WikimediaGuideSource,
    private val isMetered: () -> Boolean
) : GuideContentSource {
    private fun pick(): WikimediaGuideSource = if (isMetered()) metered() else unmetered()
    override suspend fun resolve(query: String, bookingHint: String?) = pick().resolve(query, bookingHint)
    override fun isAmbiguous(candidates: List<DestinationCandidate>) = pick().isAmbiguous(candidates)
    override suspend fun buildPack(candidate: DestinationCandidate, pinnedRevisionId: Long?) =
        pick().buildPack(candidate, pinnedRevisionId)
}

class NominatimStayGeocoder(private val geocoder: NominatimGeocoder) : StayGeocoder {
    // v2.3.5 (#3): full name -> name + locality -> name + destination, limit 3, max 3 requests.
    override suspend fun geocode(nameHint: String, destinationLabel: String?) = geocoder.geocodeSmart(nameHint, destinationLabel)
}

class NtfyPlanSyncTransport(private val sync: NtfyPlanManifestSync) : PlanSyncTransport {
    override suspend fun fetchAll(groupId: String, sinceMessageId: String?) = sync.fetchAll(groupId, sinceMessageId)
    override suspend fun publish(groupId: String, manifest: PlanManifest) = sync.publishForMessageId(groupId, manifest)
}

class ProviderGuideFlagsSource(private val provider: GuideFeatureFlagsProvider) : GuideFlagsSource {
    override fun current() = provider.current()
    override suspend fun refresh() = provider.refresh()
}

// -------------------------------------------------------------------------------------------------
// Outcomes
// -------------------------------------------------------------------------------------------------

sealed interface ResolveOutcome {
    data class Candidates(val candidates: List<DestinationCandidate>, val ambiguous: Boolean) : ResolveOutcome
    data object NoResults : ResolveOutcome
    data class Failure(val message: String, val retryable: Boolean) : ResolveOutcome
}

/** A pack as rendered: decoded content plus storage facts. */
data class StoredPack(
    val pack: GuidePack,
    val gzipBytes: Int,
    val fetchedAtEpochMs: Long,
    val softExpired: Boolean
)

sealed interface BuildOutcome {
    /** [partialMessage] is non-null when some sources failed or returned nothing (user-safe). */
    data class Built(val pack: GuidePack, val partialMessage: String?) : BuildOutcome
    data class Failure(val message: String, val retryable: Boolean) : BuildOutcome
}

// -------------------------------------------------------------------------------------------------
// Repository
// -------------------------------------------------------------------------------------------------

/**
 * Orchestrates [GuideContentSource] + [NearbySource] + [TripGuideStore] + [GuidePackCodec] +
 * plan sync + flags for the Plan tab. One app-scoped instance; all methods are main-safe and
 * never throw (except coroutine cancellation).
 */
class TripGuideRepository(
    private val content: GuideContentSource,
    private val nearby: NearbySource,
    private val stayResolver: StayLocationResolver,
    private val geocoder: StayGeocoder,
    private val store: TripGuideStore,
    private val planSync: PlanSyncTransport,
    private val flags: GuideFlagsSource,
    val optimizer: LoopOptimizer = DeterministicLoopOptimizer(),
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val cpuDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    /** Last successful publish per group (hash, time): republish policy memory (process-wide). */
    private val lastPublished = ConcurrentHashMap<String, Pair<String, Long>>()

    // ---- flags ----------------------------------------------------------------------------------

    fun currentFlags(): GuideFeatureFlags = flags.current()

    suspend fun refreshFlags(): GuideFeatureFlags = safely(flags.current()) { flags.refresh() }

    // ---- destination ----------------------------------------------------------------------------

    suspend fun resolveDestination(query: String, bookingHint: String?): ResolveOutcome {
        val q = PlanManifestCodec.sanitizeLabel(query) ?: return ResolveOutcome.NoResults
        return try {
            val candidates = content.resolve(q, bookingHint?.let { PlanManifestCodec.sanitizeLabel(it) })
            if (candidates.isEmpty()) ResolveOutcome.NoResults
            else ResolveOutcome.Candidates(candidates, content.isAmbiguous(candidates))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val (msg, retry) = failureMessage(e)
            ResolveOutcome.Failure(msg, retry)
        }
    }

    /**
     * Best-effort candidate for a QID known only from the manifest (e.g. set by another member):
     * searches the QID and keeps the exact match; falls back to a minimal candidate whose missing
     * fields the content engine fills from Wikidata. Null only when [qid] is invalid.
     */
    suspend fun candidateForQid(qid: String): DestinationCandidate? {
        if (!PlanManifestCodec.isValidQid(qid)) return null
        val exact = try {
            content.resolve(qid, null).firstOrNull { it.qid == qid }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        return exact ?: DestinationCandidate(qid, qid, null, null, null, null, 0.0)
    }

    /** Candidate equivalent of a stored pack (used for background refresh). */
    fun candidateFor(pack: GuidePack): DestinationCandidate = DestinationCandidate(
        qid = pack.destination.qid,
        label = pack.destination.label,
        description = pack.destination.description,
        location = pack.destination.location,
        wikivoyageTitle = pack.wikivoyage?.title,
        countryQid = null,
        score = 0.0
    )

    // ---- packs ----------------------------------------------------------------------------------

    /** Cached, not hard-expired, decodable pack; null means "needs a network build". */
    suspend fun loadCachedPack(destinationQid: String): StoredPack? {
        val now = clock()
        return try {
            val entity = store.loadUsablePack(destinationQid, now) ?: return null
            val pack = withContext(cpuDispatcher) { GuidePackCodec.decodeGzip(entity.packGz) }
            if (!TripGuideStore.isSchemaSupported(pack)) return null
            StoredPack(pack, entity.packGz.size, entity.fetchedAtEpochMs, TripGuideStore.isSoftExpired(entity, now))
        } catch (e: CancellationException) {
            throw e
        } catch (_: GuidePackFormatException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Network build: content engine pack (pinned revision when given) + WDQS nearby for the
     * destination centre and, when set, the stay (merged into `pack.nearby` by id). Not persisted:
     * call [persist] (the caller decides, e.g. a changed revision waits for acceptGuideUpdate()).
     */
    suspend fun buildPack(candidate: DestinationCandidate, pinnedRevisionId: Long?, stay: LatLng?): BuildOutcome {
        val base = try {
            content.buildPack(candidate, pinnedRevisionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val (msg, retry) = failureMessage(e)
            return BuildOutcome.Failure(msg, retry)
        }
        var nearbyFailed = false
        val found = LinkedHashMap<String, Place>()
        val centers = listOfNotNull(base.destination.location, stay).distinct()
        for (center in centers) {
            val batch = try {
                nearby.nearby(center, NEARBY_KINDS, NEARBY_MIN_RESULTS)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                nearbyFailed = true
                emptyList()
            }
            batch.forEach { p -> found.putIfAbsent(p.id, p) }
        }
        val pack = mergeNearby(base, found.values.toList())
        val partial = when {
            pack.wikivoyage == null && pack.places.isEmpty() && pack.nearby.isEmpty() ->
                MSG_NOTHING_FOUND
            pack.wikivoyage == null || pack.places.isEmpty() -> MSG_NO_CURATED
            nearbyFailed -> MSG_NEARBY_FAILED
            else -> null
        }
        return BuildOutcome.Built(pack, partial)
    }

    /**
     * Encodes (budget + hash) and saves the pack. Returns the stored form (the codec may have
     * trimmed it) or null when saving failed (the in-memory pack is still usable).
     */
    suspend fun persist(pack: GuidePack): StoredPack? = try {
        val encoded = withContext(cpuDispatcher) { GuidePackCodec.encode(pack) }
        val entity = store.savePack(
            destinationQid = encoded.pack.destination.qid,
            packGz = encoded.gzip,
            contentHash = encoded.contentHash,
            wikivoyageTitle = encoded.pack.wikivoyage?.title,
            wikivoyageRevId = encoded.pack.wikivoyage?.revisionId,
            fetchedAtEpochMs = encoded.pack.fetchedAtEpochMs
        )
        StoredPack(encoded.pack, entity.packGz.size, entity.fetchedAtEpochMs, softExpired = false)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    /** WDQS dragnet around [center] (e.g. a newly set stay) merged into [pack]; null if nothing new. */
    suspend fun fetchNearbyInto(pack: GuidePack, center: LatLng): GuidePack? {
        val batch = try {
            nearby.nearby(center, NEARBY_KINDS, NEARBY_MIN_RESULTS)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
        val merged = mergeNearby(pack, batch)
        return if (merged.nearby.size == pack.nearby.size) null else merged
    }

    // ---- manifest -------------------------------------------------------------------------------

    fun observeManifestRow(groupId: String): Flow<TripPlanManifestEntity?> = store.observeManifestEntity(groupId)

    suspend fun manifest(groupId: String): PlanManifest = store.getManifest(groupId) ?: TripGuideStore.emptyManifest()

    suspend fun effectiveStay(groupId: String): StayPin? = safely(null) { store.effectiveStay(groupId) }

    /** Records the group's destination + pinned revision (no-op when unchanged). */
    suspend fun setDestination(groupId: String, qid: String, revisionId: Long?, memberKey: String?): PlanManifest? =
        editManifest(groupId) { m ->
            if (m.destinationQid == qid && m.wikivoyageRevisionId == revisionId) null
            else PlanManifestMerger.setDestination(m, qid, revisionId, memberKey, clock())
        }

    /**
     * Pin, or un-pin when currently pinned. Un-pin writes a hide tombstone EQUAL to the pin time:
     * the place stops being pinned (pinnedAt > hiddenAt is false) but stays visible in Explore,
     * whose filter is "hidden strictly newer than pinned" ([isHiddenForExplore]).
     *
     * Place edits keep the manifest header ([withHeaderOf]): see there for why.
     */
    suspend fun togglePin(groupId: String, placeId: String, memberKey: String?): PlanManifest? {
        if (!PlanManifestCodec.isValidPlaceId(placeId) || placeId == PlanManifestCodec.STAY_ID) return null
        val now = clock()
        return editManifest(groupId) { m ->
            val edited = if (PlanManifestMerger.visiblePinnedIds(m).contains(placeId)) {
                val pinnedAt = m.pinned.getValue(placeId)
                m.copy(hidden = m.hidden + (placeId to pinnedAt))
            } else {
                PlanManifestMerger.pin(m, placeId, memberKey, now)
            }
            edited.withHeaderOf(m)
        }
    }

    /** Hides a place for the whole group (always strictly newer than any pin). Keeps the header. */
    suspend fun hide(groupId: String, placeId: String, memberKey: String?): PlanManifest? {
        if (!PlanManifestCodec.isValidPlaceId(placeId) || placeId == PlanManifestCodec.STAY_ID) return null
        return editManifest(groupId) { m ->
            val hidden = PlanManifestMerger.hide(m, placeId, memberKey, clock())
            val pinnedAt = hidden.pinned[placeId]
            val hiddenAt = hidden.hidden.getValue(placeId)
            val edited = if (pinnedAt != null && hiddenAt <= pinnedAt) hidden.copy(hidden = hidden.hidden + (placeId to pinnedAt + 1))
            else hidden
            edited.withHeaderOf(m)
        }
    }

    /**
     * Restores [source]'s LWW header (version, updatedAt, updatedBy) on a place-only edit.
     *
     * [PlanManifestMerger.merge] resolves `destinationQid`/`wikivoyageRevisionId` as ONE
     * last-writer-wins header keyed by `updatedAt`, while pins/hides are OR-sets that carry their
     * own per-id timestamps. If a pin bumped `updatedAt`, a member pinning offline would make their
     * (possibly stale or still-empty) destination win the header on every peer, reverting another
     * member's newer destination choice or erasing it. Keeping the header means only
     * [setDestination] competes for the destination, and pin/hide still merge by their timestamps.
     * The edit is still published because the content hash changes and the row is pending push.
     */
    private fun PlanManifest.withHeaderOf(source: PlanManifest): PlanManifest = copy(
        version = source.version,
        updatedAtEpochMs = source.updatedAtEpochMs,
        updatedByMemberKey = source.updatedByMemberKey
    )

    private suspend fun editManifest(groupId: String, edit: (PlanManifest) -> PlanManifest?): PlanManifest? =
        safely(null) {
            val current = manifest(groupId)
            val next = edit(current) ?: return@safely current
            if (store.upsertManifest(groupId, next)) next else null
        }

    // ---- stay -----------------------------------------------------------------------------------

    suspend fun resolveStayText(text: String): StayResolution =
        safely<StayResolution>(StayResolution.Rejected(MSG_STAY_FAILED)) { stayResolver.resolve(text) }

    suspend fun geocodeStay(nameHint: String, destinationLabel: String?): StayResolution =
        safely<StayResolution>(StayResolution.Rejected(MSG_STAY_FAILED)) { geocoder.geocode(nameHint, destinationLabel) }

    suspend fun setLocalStay(groupId: String, stay: StayPin, memberKey: String?): Boolean =
        safely(false) { store.setLocalStay(groupId, stay, memberKey) }

    suspend fun clearLocalStay(groupId: String, memberKey: String?, organizerKeys: Set<String>): Boolean =
        safely(false) { store.clearLocalStay(groupId, memberKey, organizerKeys) }

    suspend fun setShareStay(groupId: String, share: Boolean, memberKey: String?, organizerKeys: Set<String>): Boolean =
        safely(false) { store.setShareStay(groupId, share, memberKey, organizerKeys) }

    // ---- sync -----------------------------------------------------------------------------------

    /**
     * Polls the plan topic from the stored cursor and merges every message. Returns the merged
     * manifest. When the merge leaves nothing to push (local == latest remote), the latest message
     * is recorded as our "last publish" so a freshly started process does not echo a peer's
     * manifest straight back (the 6 h republish rule still applies from that message's time).
     */
    suspend fun syncPlan(groupId: String, organizerKeys: Set<String>): PlanManifest? = safely(null) {
        val since = store.getManifestEntity(groupId)?.lastSyncMessageId
        val messages = planSync.fetchAll(groupId, since)
        var merged: PlanManifest? = null
        for (msg in messages) {
            merged = store.applyRemoteManifest(groupId, msg.manifest, msg.messageId, organizerKeys) ?: merged
        }
        val latest = messages.lastOrNull()
        val row = if (merged != null && latest != null) store.getManifestEntity(groupId) else null
        if (row != null && latest != null && !row.pendingPush) {
            val publishable = store.manifestForPublish(groupId)
            val sentAt = latest.timeEpochSec * 1000L
            val previous = lastPublished[groupId]
            if (publishable != null && (previous == null || previous.second < sentAt)) {
                lastPublished[groupId] = PlanManifestCodec.contentHash(publishable, row.shareStay) to sentAt
            }
        }
        merged
    }

    /**
     * Publishes when the row is pending push or [NtfyPlanManifestSync.shouldRepublish] says so
     * (content changed / > 6 h since the last publish, to survive ntfy's 12 h cache). Empty
     * manifests are never published. The poll cursor is NOT advanced to our own message id, so a
     * peer message published just before ours is never skipped. Returns true when published.
     */
    suspend fun publishIfNeeded(groupId: String): Boolean = safely(false) {
        val row = store.getManifestEntity(groupId) ?: return@safely false
        val m = store.manifestForPublish(groupId) ?: return@safely false
        if (m.destinationQid == null && m.pinned.isEmpty() && m.hidden.isEmpty() && m.stay == null) return@safely false
        val hash = PlanManifestCodec.contentHash(m, row.shareStay)
        val now = clock()
        val last = lastPublished[groupId]
        val needed = row.pendingPush || NtfyPlanManifestSync.shouldRepublish(last?.first, hash, last?.second, now)
        if (!needed) return@safely false
        planSync.publish(groupId, m) ?: return@safely false
        store.markPushed(groupId, null)
        lastPublished[groupId] = hash to now
        true
    }

    // ---- helpers --------------------------------------------------------------------------------

    private suspend inline fun <T> safely(fallback: T, crossinline block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        fallback
    }

    companion object {
        val NEARBY_KINDS: Set<PlaceKind> = setOf(PlaceKind.SEE, PlaceKind.DO, PlaceKind.EAT)
        const val NEARBY_MIN_RESULTS = 8

        const val MSG_NO_CURATED = "No curated guide yet. Showing nearby landmarks from Wikidata"
        const val MSG_NOTHING_FOUND = "No curated guide or nearby landmarks found for this place yet."
        const val MSG_NEARBY_FAILED = "Some nearby places couldn't be loaded. Pull to retry."
        const val MSG_RATE_LIMITED = "Wikimedia is busy right now. Try again in a minute."
        const val MSG_NO_COORDINATES = "This place has no map location. Try a nearby town or city."
        const val MSG_NETWORK = "Couldn't load the guide. Check your connection and retry."
        const val MSG_STAY_FAILED = "Couldn't read that location. Try again or paste coordinates."

        /** Adds [extra] places into `pack.nearby` by id (existing entries win; stable order). */
        fun mergeNearby(pack: GuidePack, extra: List<Place>): GuidePack {
            if (extra.isEmpty()) return pack
            val byId = LinkedHashMap<String, Place>()
            pack.nearby.forEach { byId.putIfAbsent(it.id, it) }
            extra.forEach { byId.putIfAbsent(it.id, it) }
            return if (byId.size == pack.nearby.size) pack else pack.copy(nearby = byId.values.toList())
        }

        /** User-safe message + retryability for a content/transport failure (never leaks details). */
        fun failureMessage(e: Throwable): Pair<String, Boolean> = when {
            e is GuideContentException && (e.httpCode == 429 || e.httpCode == 503) -> MSG_RATE_LIMITED to true
            e is GuideContentException && e.message?.contains("no coordinates", ignoreCase = true) == true ->
                MSG_NO_COORDINATES to false
            else -> MSG_NETWORK to true
        }

        /** Explore visibility rule: hidden only when the hide is strictly newer than any pin. */
        fun isHiddenForExplore(m: PlanManifest, placeId: String): Boolean {
            val hiddenAt = m.hidden[placeId] ?: return false
            val pinnedAt = m.pinned[placeId] ?: return true
            return hiddenAt > pinnedAt
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Pure helpers used by MainActivity / the ViewModel
// -------------------------------------------------------------------------------------------------

/**
 * Detects share-sheet text that carries a map location (Google Maps share, Knowledge-panel link,
 * `geo:` URI). Used by MainActivity to route ACTION_SEND text to the "Set trip stay" flow AFTER
 * the existing SM2_/join handling has declined it.
 */
object MapShareTextDetector {
    private val PATTERNS = listOf(
        Regex("""maps\.app\.goo\.gl/""", RegexOption.IGNORE_CASE),
        Regex("""(^|[^a-z0-9.])goo\.gl/maps""", RegexOption.IGNORE_CASE),
        Regex("""(^|[^a-z0-9.])g\.co/kgs""", RegexOption.IGNORE_CASE),
        Regex("""(^|[/.\s])google\.[a-z]{2,3}(\.[a-z]{2})?/maps""", RegexOption.IGNORE_CASE),
        Regex("""(^|[/\s])maps\.google\.[a-z]{2,3}(\.[a-z]{2})?([/?]|$)""", RegexOption.IGNORE_CASE),
        Regex("""(^|\s)geo:[-+0-9.]""", RegexOption.IGNORE_CASE)
    )

    /** Longest shared text we forward (the resolver caps input again). */
    const val MAX_SHARED_CHARS = 2_000

    fun isMapShareText(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val t = text.take(MAX_SHARED_CHARS)
        if (t.contains("SM2_")) return false
        return PATTERNS.any { it.containsMatchIn(t) }
    }
}

/**
 * Turns raw booking arrival names (PNR `toStationName`, flight "IATA (City)", offline station
 * names like "HPT (Hosapete (Hampi))") into short destination search suggestions:
 * "Hosapete Jn" -> "Hosapete", "Madgaon (Goa)" -> "Goa", "Madgaon"; codes and placeholders dropped.
 */
object BookingDestinationSuggestions {
    const val DEFAULT_LIMIT = 3

    private val CODE_WRAPPED = Regex("""^([A-Z0-9]{2,5})\s*\((.+)\)$""")
    private val TRAILING_PAREN = Regex("""^(.*?)\s*\(([^()]*)\)\s*$""")
    private val BARE_CODE = Regex("""^[A-Z0-9]{2,5}$""")
    private val SUFFIXES = listOf(
        "international airport", "intl airport", "intl. airport", "domestic airport", "airport",
        "railway station", "rly station", "rly stn", "station", "stn",
        "junction", "jn.", "jn", "jct", "terminus", "terminal", "cantonment", "cantt", "halt"
    )
    private val PREFIXES = listOf("ksr ", "mgr ", "chhatrapati shivaji maharaj ")
    private val PLACEHOLDERS = setOf("destination station", "destination", "dest", "dst", "org", "origin")

    fun normalise(rawNames: List<String>, limit: Int = DEFAULT_LIMIT): List<String> {
        val out = LinkedHashMap<String, String>()
        for (raw in rawNames) {
            for (name in candidatesFor(raw)) {
                out.putIfAbsent(name.lowercase(), name)
                if (out.size >= limit) return out.values.toList()
            }
        }
        return out.values.toList()
    }

    /** Suggestions derived from one raw name (tourist hint in parentheses first). */
    fun candidatesFor(raw: String?): List<String> {
        var s = PlanManifestCodec.sanitizeLabel(raw) ?: return emptyList()
        CODE_WRAPPED.matchEntire(s)?.let { s = it.groupValues[2].trim() }
        val (base, hint) = TRAILING_PAREN.matchEntire(s)?.let { it.groupValues[1] to it.groupValues[2] } ?: (s to null)
        return listOfNotNull(hint?.let { clean(it) }, clean(base)).distinct()
    }

    private fun clean(value: String): String? {
        var s = value.trim().trim(',', '-', '.', ' ')
        var changed = true
        while (changed && s.isNotEmpty()) {
            changed = false
            val lower = s.lowercase()
            for (suffix in SUFFIXES) {
                if (lower.endsWith(" $suffix") && s.length > suffix.length + 1) {
                    s = s.dropLast(suffix.length).trimEnd().trimEnd(',', '-', '.').trimEnd()
                    changed = true
                    break
                }
            }
        }
        val lower = s.lowercase()
        PREFIXES.firstOrNull { lower.startsWith(it) && s.length > it.length + 1 }?.let { s = s.drop(it.length).trim() }
        if (s.length < 2) return null
        if (BARE_CODE.matches(s)) return null
        if (s.lowercase() in PLACEHOLDERS) return null
        return s
    }
}
