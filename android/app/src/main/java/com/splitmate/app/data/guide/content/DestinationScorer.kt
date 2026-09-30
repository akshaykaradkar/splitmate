package com.splitmate.app.data.guide.content

import com.splitmate.app.data.guide.DestinationCandidate
import java.text.Normalizer
import kotlin.math.ln

/**
 * Deterministic destination ranking for `wbsearchentities` + `wbgetentities` results.
 *
 * Signals (audit §2.1, decision #10):
 * - +[Weights.wikivoyage] when an `enwikivoyage` sitelink exists (it's the article we need);
 * - +[Weights.country] when P17 equals [preferredCountryQid] (India, Q668, by default; `null` disables);
 * - +[Weights.coordinates] when P625 is present; +[Weights.image] when P18 is present;
 * - +[Weights.settlement] / +[Weights.touristDestination] for P31 classes in the configured sets;
 * - [Weights.disambiguation] (negative) for Wikimedia disambiguation pages;
 * - booking hint (e.g. station/city from a PNR): +[Weights.hintExact] for an exact label match,
 *   +[Weights.hintPartial] when label/description contains the hint (or vice versa);
 * - + ln(1 + sitelinks) and a small bonus preserving the API's own relevance rank.
 *
 * Ordering: score desc, then search rank asc, then numeric QID asc — identical inputs always
 * produce identical output.
 */
class DestinationScorer(
    val preferredCountryQid: String? = INDIA_QID,
    val weights: Weights = Weights(),
    val settlementClasses: Set<String> = DEFAULT_SETTLEMENT_CLASSES,
    val touristClasses: Set<String> = DEFAULT_TOURIST_CLASSES,
    val ambiguityMargin: Double = DEFAULT_AMBIGUITY_MARGIN
) {
    data class Weights(
        val wikivoyage: Double = 50.0,
        val country: Double = 20.0,
        val coordinates: Double = 10.0,
        val image: Double = 5.0,
        val settlement: Double = 15.0,
        val touristDestination: Double = 8.0,
        val disambiguation: Double = -100.0,
        val hintExact: Double = 25.0,
        val hintPartial: Double = 12.0,
        val rankBonusPerStep: Double = 1.0
    )

    /** Scores one hit/entity pair. [entity] may be null when `wbgetentities` omitted the item. */
    fun score(hit: WikidataSearchHit, entity: WikidataEntity?, bookingHint: String? = null): Double {
        var s = 0.0
        if (entity != null) {
            if (!entity.enwikivoyageTitle.isNullOrBlank()) s += weights.wikivoyage
            if (preferredCountryQid != null && entity.countryQid == preferredCountryQid) s += weights.country
            if (entity.location != null) s += weights.coordinates
            if (!entity.imageFile.isNullOrBlank()) s += weights.image
            if (entity.instanceOf.any { it in settlementClasses }) s += weights.settlement
            if (entity.instanceOf.any { it in touristClasses }) s += weights.touristDestination
            if (DISAMBIGUATION_QID in entity.instanceOf) s += weights.disambiguation
            s += ln(1.0 + entity.sitelinkCount.coerceAtLeast(0))
        }
        s += hintScore(hit, entity, bookingHint)
        s += (MAX_RANK_BONUS_STEPS - hit.rank).coerceAtLeast(0) * weights.rankBonusPerStep
        return round3(s)
    }

    /** Builds, scores and sorts candidates. */
    fun rank(
        hits: List<WikidataSearchHit>,
        entities: Map<String, WikidataEntity>,
        bookingHint: String? = null
    ): List<DestinationCandidate> {
        data class Scored(val c: DestinationCandidate, val rank: Int)
        return hits.distinctBy { it.qid }.map { hit ->
            val e = entities[hit.qid]
            Scored(
                DestinationCandidate(
                    qid = hit.qid,
                    label = e?.label ?: hit.label,
                    description = hit.description ?: e?.description,
                    location = e?.location,
                    wikivoyageTitle = e?.enwikivoyageTitle,
                    countryQid = e?.countryQid,
                    score = score(hit, e, bookingHint)
                ),
                hit.rank
            )
        }.sortedWith(
            compareByDescending<Scored> { it.c.score }
                .thenBy { it.rank }
                .thenBy { it.c.qid.drop(1).toLongOrNull() ?: Long.MAX_VALUE }
        ).map { it.c }
    }

    /**
     * True when the user should pick via disambiguation chips: at least two candidates and the
     * top two scores are within [ambiguityMargin] (candidates must be sorted, as from [rank]).
     */
    fun isAmbiguous(candidates: List<DestinationCandidate>): Boolean {
        if (candidates.size < 2) return false
        return candidates[0].score - candidates[1].score < ambiguityMargin
    }

    private fun hintScore(hit: WikidataSearchHit, entity: WikidataEntity?, hint: String?): Double {
        val h = normalize(hint ?: return 0.0)
        if (h.length < 3) return 0.0
        val label = normalize(entity?.label ?: hit.label)
        val desc = normalize(hit.description ?: entity?.description ?: "")
        return when {
            label == h -> weights.hintExact
            label.containsWord(h) || h.containsWord(label) || desc.containsWord(h) -> weights.hintPartial
            else -> 0.0
        }
    }

    private fun String.containsWord(other: String): Boolean =
        other.isNotEmpty() && (" $this ").contains(" $other ")

    companion object {
        const val INDIA_QID = "Q668"
        const val DISAMBIGUATION_QID = "Q4167410"
        const val DEFAULT_AMBIGUITY_MARGIN = 15.0
        const val MAX_RANK_BONUS_STEPS = 7

        /** Human settlement, city, big city, town, village, capital, metropolis, municipality, city/town. */
        val DEFAULT_SETTLEMENT_CLASSES: Set<String> = setOf(
            "Q486972", "Q515", "Q1549591", "Q3957", "Q532", "Q5119", "Q200250", "Q15284", "Q7930989"
        )

        /** Tourist destination, tourist attraction, World Heritage Site, beach. */
        val DEFAULT_TOURIST_CLASSES: Set<String> = setOf("Q1200957", "Q570116", "Q9259", "Q40080")

        /** Lowercase, accent-free, punctuation → space, collapsed. Also strips "railway station" noise. */
        fun normalize(s: String): String {
            val ascii = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
            return ascii.lowercase()
                .replace(Regex("[^a-z0-9]+"), " ")
                .replace(Regex("\\b(railway station|junction|jn|station|airport|city|town)\\b"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        private fun round3(d: Double): Double = Math.round(d * 1000.0) / 1000.0
    }
}
