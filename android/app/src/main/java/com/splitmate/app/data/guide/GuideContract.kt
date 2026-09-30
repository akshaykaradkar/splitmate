package com.splitmate.app.data.guide

/**
 * v2.3.4 Trip Guide & Smart Loop — SHARED CONTRACT (owned by the integration lead).
 *
 * Every v2.3.4 workstream (content engine, smart loop, persistence/sync, UI) codes against the
 * types and interfaces in this file. Do NOT change existing signatures without coordinating with
 * the integration lead; additive, backwards-compatible members are allowed.
 *
 * Rules:
 * - Pure Kotlin only (no Android framework types) so everything is JVM unit-testable.
 * - JSON is handled with org.json (house standard); no new serialization libraries.
 * - This is the invisible data model. It is never rendered as raw JSON in the UI.
 */

/** WGS84 coordinate in decimal degrees. */
data class LatLng(val lat: Double, val lng: Double) {
    init {
        require(lat in -90.0..90.0) { "lat out of range: $lat" }
        require(lng in -180.0..180.0) { "lng out of range: $lng" }
    }
}

/** Editorial category of a place. Colour mapping: SEE=olive, EAT=terracotta, DO=periwinkle, SLEEP=neutral. */
enum class PlaceKind { SEE, DO, EAT, SLEEP }

/** Where a record came from (drives attribution). */
enum class PlaceSource { WIKIVOYAGE, WIKIDATA, USER }

/** Wikimedia Commons image with licence credit (required for CC BY-SA compliance). */
data class CommonsImage(
    val fileName: String,
    val thumbUrl: String,
    val width: Int,
    val artist: String?,
    val license: String?,
    val licenseUrl: String?,
    val sourceUrl: String?
)

/** A candidate destination returned by entity resolution (used for disambiguation chips). */
data class DestinationCandidate(
    val qid: String,
    val label: String,
    val description: String?,
    val location: LatLng?,
    val wikivoyageTitle: String?,
    val countryQid: String?,
    val score: Double
)

/** A resolved destination with its hero image. */
data class Destination(
    val qid: String,
    val label: String,
    val description: String?,
    val location: LatLng,
    val hero: CommonsImage?
)

/**
 * A place in the guide or the loop.
 * id conventions: "wv:<kind>:<slug>" for Wikivoyage listings, "wd:<QID>" for Wikidata items,
 * "stay" for the user's stay.
 */
data class Place(
    val id: String,
    val kind: PlaceKind,
    val name: String,
    val location: LatLng?,
    val qid: String? = null,
    val blurb: String? = null,          // plain text, sanitised, <= 280 chars
    val hours: String? = null,
    val price: String? = null,
    val address: String? = null,
    val source: PlaceSource,
    val image: CommonsImage? = null,
    val distanceKmFromCenter: Double? = null
)

/** Wikivoyage article reference for CC BY-SA 4.0 attribution. */
data class WikivoyageRef(
    val title: String,
    val revisionId: Long,
    val url: String,
    val license: String = "CC BY-SA 4.0"
)

/** The complete offline pack (schema "splitmate.guide/1"). Stored gzip-compressed in Room. */
data class GuidePack(
    val schema: String = SCHEMA_V1,
    val destination: Destination,
    val wikivoyage: WikivoyageRef?,
    val places: List<Place>,            // curated Wikivoyage listings (SEE/DO/EAT/SLEEP)
    val nearby: List<Place>,            // Wikidata spatial dragnet results
    val attribution: List<String>,
    val fetchedAtEpochMs: Long
) {
    companion object { const val SCHEMA_V1 = "splitmate.guide/1" }
}

/** The user's stay (hotel). Local-only unless the planner opts in to share it. */
data class StayPin(
    val location: LatLng,
    val label: String,
    val precision: CoordinatePrecision,
    val setByMemberKey: String?,
    val setAtEpochMs: Long
)

enum class CoordinatePrecision { EXACT, APPROXIMATE }

/**
 * Synced plan manifest (<= 3 KB serialized). "Sync intents, recompute derivations":
 * the route is NEVER synced; every device recomputes it with the deterministic TSP.
 * pinned/hidden are OR-sets with timestamps: visible iff pinnedAt > hiddenAt.
 */
data class PlanManifest(
    val version: Int = 1,
    val destinationQid: String?,
    val wikivoyageRevisionId: Long?,
    val stay: StayPin?,                 // present only when sharing is opted in
    val pinned: Map<String, Long>,      // placeId -> epochMs
    val hidden: Map<String, Long>,      // placeId -> epochMs
    val days: Map<String, Int>,         // placeId -> day number (reserved; single-day in v2.3.4)
    val updatedAtEpochMs: Long,
    val updatedByMemberKey: String?
)

/** Ordered loop result. Distances are straight-line (Haversine) plus a road estimate. */
data class LoopRoute(
    val start: LatLng,
    val orderedStops: List<Place>,      // excludes the start/end stay point
    val legStraightKm: List<Double>,    // size = orderedStops.size + 1 (includes return leg)
    val totalStraightKm: Double,
    val totalRoadEstimateKm: Double     // totalStraightKm * ROAD_FACTOR
) {
    companion object { const val ROAD_FACTOR = 1.35 }
}

/** Outcome of resolving a shared/pasted map link or text into coordinates. */
sealed class StayResolution {
    data class Resolved(val location: LatLng, val label: String?, val precision: CoordinatePrecision) : StayResolution()
    /** Link valid but carried no coordinates (e.g. ftid-only); UI should offer fallbacks. */
    data class NoCoordinates(val finalUrl: String?, val placeNameHint: String?) : StayResolution()
    data class Rejected(val reason: String) : StayResolution()
}

/** Remote kill-switches (static JSON on GitHub Pages). Defaults are "enabled". */
data class GuideFeatureFlags(
    val guideEnabled: Boolean = true,
    val shortLinkParsingEnabled: Boolean = true,
    val wdqsEnabled: Boolean = true,
    val nominatimEnabled: Boolean = true
)

// ---------------------------------------------------------------------------------------------
// Interfaces (implementations live in their workstream packages)
// ---------------------------------------------------------------------------------------------

/** Content engine: Wikidata Action API resolution + Commons credits + Wikivoyage listing parse. */
interface GuideSource {
    suspend fun resolve(query: String): List<DestinationCandidate>
    suspend fun buildPack(candidate: DestinationCandidate, pinnedRevisionId: Long? = null): GuidePack
}

/** Spatial dragnet (Wikidata WDQS wikibase:around). Adaptive radius 5 -> 10 -> 20 km, cap 25 km. */
interface NearbySource {
    suspend fun nearby(center: LatLng, kinds: Set<PlaceKind>, minResults: Int = 8): List<Place>
}

/** Resolves map links / pasted text into a stay location. */
interface StayLocationResolver {
    suspend fun resolve(input: String): StayResolution
}

/** Route cost provider (Haversine now; road-distance providers later). */
interface RouteCostProvider {
    fun matrixKm(points: List<LatLng>): Array<DoubleArray>
}

/** Deterministic loop optimiser: identical input set (any order) => identical output. */
interface LoopOptimizer {
    fun optimize(start: LatLng, stops: List<Place>): LoopRoute
}

/** Plan manifest transport over the group's dedicated ntfy plan topic. */
interface PlanManifestSync {
    suspend fun publish(groupId: String, manifest: PlanManifest): Boolean
    suspend fun fetchLatest(groupId: String, sinceMessageId: String?): Pair<PlanManifest, String>?
}

/** Identified User-Agent required by the Wikimedia User-Agent policy (no PII). */
object GuideHttp {
    const val USER_AGENT =
        "SplitMate/2.3.4 (Android; https://github.com/akshaykaradkar/splitmate; group-travel planner)"
    const val CONNECT_TIMEOUT_MS = 8_000
    const val READ_TIMEOUT_MS = 10_000
}
