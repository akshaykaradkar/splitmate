package com.splitmate.app.data.guide.content

import com.splitmate.app.data.guide.CommonsImage
import com.splitmate.app.data.guide.Destination
import com.splitmate.app.data.guide.DestinationCandidate
import com.splitmate.app.data.guide.GuidePack
import com.splitmate.app.data.guide.GuideSource
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.WikivoyageRef
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * [GuideSource] backed by the Wikimedia Action APIs (Wikidata, Commons, Wikivoyage).
 *
 * resolve(): `wbsearchentities` → `wbgetentities` → [DestinationScorer] (2 requests).
 * buildPack(): hero `imageinfo` ‖ Wikivoyage `parse` (at most 2 in flight), then one batched
 * `imageinfo` for listing thumbnails (320 px). ≤ 5 requests per hydrate, all through
 * [PoliteHttpClient] (UA, maxlag, retry-once, HTTPS allow-list).
 *
 * Partial results: a missing/failed Wikivoyage article yields a pack with `places = []` and
 * `wikivoyage = null` (UI shows "No curated guide yet"); a failed hero yields `hero = null`.
 * `nearby` is always empty here; the loop workstream fills it via NearbySource.
 */
class WikimediaGuideSource(
    fetcher: HttpFetcher = OkHttpFetcher(),
    private val scorer: DestinationScorer = DestinationScorer(),
    private val heroWidth: Int = CommonsImageClient.WIDTH_HERO,
    private val fetchListingThumbs: Boolean = true,
    private val maxListingThumbs: Int = DEFAULT_MAX_LISTING_THUMBS,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    http: PoliteHttpClient = PoliteHttpClient(fetcher)
) : GuideSource {

    private val wikidata = WikidataActionApiClient(http)
    private val commons = CommonsImageClient(http)
    private val wikivoyage = WikivoyageClient(http)
    private val entityCache = ConcurrentHashMap<String, WikidataEntity>()

    override suspend fun resolve(query: String): List<DestinationCandidate> = resolve(query, null)

    /**
     * Resolves free text to ranked candidates. [bookingHint] (e.g. the arrival station/city from
     * a PNR) boosts matching labels (decision #10). Use [DestinationScorer.isAmbiguous] via
     * [isAmbiguous] to decide whether to show disambiguation chips.
     */
    suspend fun resolve(query: String, bookingHint: String?): List<DestinationCandidate> = withContext(ioDispatcher) {
        val hits = wikidata.search(query)
        if (hits.isEmpty()) return@withContext emptyList()
        val entities = wikidata.getEntities(hits.map { it.qid })
        if (entityCache.size > MAX_CACHE) entityCache.clear()
        entityCache.putAll(entities)
        scorer.rank(hits, entities, bookingHint)
    }

    fun isAmbiguous(candidates: List<DestinationCandidate>): Boolean = scorer.isAmbiguous(candidates)

    override suspend fun buildPack(candidate: DestinationCandidate, pinnedRevisionId: Long?): GuidePack =
        withContext(ioDispatcher) {
            val entity = entityCache[candidate.qid]
                ?: softly { wikidata.getEntities(listOf(candidate.qid))[candidate.qid] }?.also { entityCache[it.qid] = it }
            val center = candidate.location ?: entity?.location
                ?: throw GuideContentException("Destination has no coordinates")
            val wvTitle = candidate.wikivoyageTitle ?: entity?.enwikivoyageTitle

            val (hero, article) = coroutineScope {
                val heroJob = async { entity?.imageFile?.let { f -> softly { commons.fetch(f, heroWidth) } } }
                val articleJob = async { fetchArticle(wvTitle, pinnedRevisionId) }
                heroJob.await() to articleJob.await()
            }

            val parsed = article?.let { WikivoyageListingParser.parse(it.title, it.revisionId, it.wikitext) }
            val thumbs: Map<String, CommonsImage> =
                if (fetchListingThumbs && parsed != null) {
                    val files = parsed.listings.mapNotNull { it.imageFile }.distinct().take(maxListingThumbs)
                    if (files.isEmpty()) emptyMap()
                    else softly { commons.fetchMany(files, CommonsImageClient.WIDTH_THUMB) } ?: emptyMap()
                } else emptyMap()

            val places: List<Place> = parsed?.listings?.map { l ->
                l.place.copy(
                    image = l.imageFile?.let { thumbs[it] },
                    distanceKmFromCenter = l.place.location?.let { round2(haversineKm(center, it)) }
                )
            } ?: emptyList()

            val ref = article?.let {
                WikivoyageRef(title = it.title, revisionId = it.revisionId, url = WikivoyageClient.articleUrl(it.title))
            }
            val pack = GuidePack(
                destination = Destination(
                    qid = candidate.qid,
                    label = candidate.label,
                    description = candidate.description ?: entity?.description,
                    location = center,
                    hero = hero
                ),
                wikivoyage = ref,
                places = places,
                nearby = emptyList(),
                attribution = attributionFor(candidate.qid, ref, hero, places),
                fetchedAtEpochMs = clock()
            )
            GuidePackCodec.enforceBudget(pack).first
        }

    private fun fetchArticle(title: String?, pinned: Long?): WikivoyageArticle? {
        if (pinned != null && pinned > 0) {
            val pinnedArticle = softly { wikivoyage.fetch(title ?: "", pinned) }
            if (pinnedArticle != null) return pinnedArticle
        }
        if (title.isNullOrBlank()) return null
        return softly { wikivoyage.fetch(title) }
    }

    /** Runs [block], mapping content/transport failures to null (never swallows cancellation). */
    private inline fun <T> softly(block: () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: GuideContentException) {
        null
    } catch (e: org.json.JSONException) {
        null
    }

    companion object {
        const val DEFAULT_MAX_LISTING_THUMBS = 30
        private const val MAX_CACHE = 256
        const val WIKIDATA_ITEM_BASE = "https://www.wikidata.org/wiki/"
        const val CC_BY_SA_4_URL = "https://creativecommons.org/licenses/by-sa/4.0/"

        /** Human-readable attribution lines (CC BY-SA text, CC0 courtesy, per-file Commons credits). */
        fun attributionFor(qid: String, ref: WikivoyageRef?, hero: CommonsImage?, places: List<Place>): List<String> {
            val lines = ArrayList<String>()
            if (ref != null) {
                lines += "Text from the Wikivoyage article \"${ref.title}\" (revision ${ref.revisionId}), " +
                    "${ref.license}, excerpted and modified: ${WikivoyageClient.permalink(ref.title, ref.revisionId)} " +
                    "(licence: $CC_BY_SA_4_URL)"
            }
            lines += "Destination data from Wikidata ($qid), CC0 1.0: $WIKIDATA_ITEM_BASE$qid"
            val images = LinkedHashMap<String, CommonsImage>()
            hero?.let { images[it.fileName] = it }
            places.forEach { p -> p.image?.let { images.putIfAbsent(it.fileName, it) } }
            images.values.forEach { lines += commonsCredit(it) }
            return lines
        }

        fun commonsCredit(img: CommonsImage): String {
            val by = img.artist?.let { " by $it" } ?: ""
            val lic = when {
                img.license != null && img.licenseUrl != null -> ", ${img.license} (${img.licenseUrl})"
                img.license != null -> ", ${img.license}"
                else -> ""
            }
            val src = img.sourceUrl?.let { ": $it" } ?: ""
            return "Photo \"${img.fileName}\"$by$lic, via Wikimedia Commons$src"
        }

        /** Great-circle distance in km (mean Earth radius 6371.0088 km). */
        fun haversineKm(a: LatLng, b: LatLng): Double {
            val r = 6371.0088
            val dLat = Math.toRadians(b.lat - a.lat)
            val dLng = Math.toRadians(b.lng - a.lng)
            val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
            return 2 * r * asin(sqrt(h.coerceIn(0.0, 1.0)))
        }

        private fun round2(d: Double): Double = Math.round(d * 100.0) / 100.0
    }
}
