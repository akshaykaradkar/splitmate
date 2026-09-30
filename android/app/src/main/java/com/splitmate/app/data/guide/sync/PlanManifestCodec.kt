package com.splitmate.app.data.guide.sync

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.PlanManifest
import com.splitmate.app.data.guide.StayPin
import org.json.JSONObject
import java.security.MessageDigest

/** Thrown when a manifest cannot be brought under [PlanManifestCodec.MAX_BYTES] by trimming. */
class PlanManifestTooLargeException(val sizeBytes: Int, val limitBytes: Int) :
    IllegalStateException("Plan manifest is $sizeBytes bytes after trimming; limit is $limitBytes bytes")

/**
 * Result of [PlanManifestCodec.encode].
 *
 * @property json compact canonical JSON, at most [PlanManifestCodec.MAX_BYTES] UTF-8 bytes.
 * @property manifest the manifest that was actually serialised (after any tombstone trimming, and
 *   with `stay` removed when sharing is off).
 * @property droppedHiddenIds hidden tombstones removed to satisfy the size limit (oldest first).
 */
data class EncodedPlanManifest(
    val json: String,
    val sizeBytes: Int,
    val manifest: PlanManifest,
    val droppedHiddenIds: List<String>
) {
    /** SHA-256 hex of [json]; stable for equal manifests (canonical key order). */
    val contentHash: String get() = PlanManifestCodec.sha256Hex(json)
}

/**
 * [PlanManifest] <-> compact JSON for the group's ntfy plan topic.
 *
 * Wire format (schema `v = 1`, keys sorted, empty/nullable members omitted):
 * ```
 * {"by":"9876543210","days":{"wv:see:x":1},"destQid":"Q1","hidden":{"wd:Q2":1727600000000},
 *  "pinned":{"wv:see:x":1727600000000},"stay":{"at":0,"by":"..","label":"Our stay","lat":15.33,
 *  "lng":76.46,"prec":"EXACT"},"updatedAt":1727600000000,"v":1,"wvRev":123}
 * ```
 *
 * Guarantees:
 * - Output is canonical (sorted keys, no whitespace), so equal manifests hash identically.
 * - `stay` is emitted ONLY when `shareStay` is true (decision #14, privacy).
 * - The encoded size is at most [MAX_BYTES] UTF-8 bytes. If larger, the oldest `hidden` tombstones
 *   are dropped first (never the reserved `stay` tombstone); if that is not enough,
 *   [PlanManifestTooLargeException] is thrown.
 * - [decode] never throws: foreign, malformed, oversized or future-version payloads return null.
 *   All strings are sanitised (control/bidi characters removed, lengths capped).
 */
object PlanManifestCodec {

    const val MAX_BYTES = 3_000
    const val SCHEMA_VERSION = 1

    /** Reserved place id: the stay pin. `hidden["stay"]` is the stay-clear tombstone. */
    const val STAY_ID = "stay"

    /** Upper bound for raw payloads accepted by [decode] (ntfy message limit is 4,096 bytes). */
    const val MAX_DECODE_CHARS = 8_192
    const val MAX_ENTRIES_PER_MAP = 400
    const val MAX_PLACE_ID_LENGTH = 128
    const val MAX_MEMBER_KEY_LENGTH = 64
    const val MAX_LABEL_LENGTH = 80
    const val MAX_DAY = 30
    const val DEFAULT_STAY_LABEL = "Stay"

    private val QID_REGEX = Regex("^Q[1-9][0-9]{0,11}$")
    private val PLACE_ID_REGEX = Regex("^[\\p{L}\\p{N}:._~\\-]{1,$MAX_PLACE_ID_LENGTH}$")
    private val MEMBER_KEY_REGEX = Regex("^[\\p{L}\\p{N}_.:@+\\-]{1,$MAX_MEMBER_KEY_LENGTH}$")

    // ---------------------------------------------------------------------------------------------
    // Encoding
    // ---------------------------------------------------------------------------------------------

    /**
     * Serialises [manifest] for the wire, enforcing [MAX_BYTES].
     *
     * @param shareStay when false the `stay` member is never emitted, regardless of [manifest].
     * @throws PlanManifestTooLargeException if trimming hidden tombstones cannot reach the limit.
     */
    fun encode(manifest: PlanManifest, shareStay: Boolean, maxBytes: Int = MAX_BYTES): EncodedPlanManifest {
        var current = if (shareStay) manifest else manifest.copy(stay = null)
        var json = toJson(current, includeStay = shareStay)
        var size = utf8Size(json)
        if (size <= maxBytes) return EncodedPlanManifest(json, size, current, emptyList())

        // Oldest tombstones first; ties broken by id for determinism. Never trim the stay tombstone
        // (dropping it could resurrect a cleared stay from an older peer).
        val trimOrder = current.hidden.entries
            .filter { it.key != STAY_ID }
            .sortedWith(compareBy<Map.Entry<String, Long>>({ it.value }, { it.key }))
            .map { it.key }
        val dropped = mutableListOf<String>()
        for (id in trimOrder) {
            current = dropHiddenTombstone(current, id)
            dropped += id
            json = toJson(current, includeStay = shareStay)
            size = utf8Size(json)
            if (size <= maxBytes) return EncodedPlanManifest(json, size, current, dropped.toList())
        }
        throw PlanManifestTooLargeException(size, maxBytes)
    }

    /**
     * Removes a hidden tombstone. If the same id is also pinned with an older-or-equal timestamp
     * (i.e. the item is currently hidden), the stale pin and its day assignment are removed too so
     * trimming never makes a hidden pin visible again.
     */
    private fun dropHiddenTombstone(m: PlanManifest, id: String): PlanManifest {
        val hiddenAt = m.hidden[id] ?: return m
        val pinnedAt = m.pinned[id]
        val removePin = pinnedAt != null && pinnedAt <= hiddenAt
        return m.copy(
            hidden = m.hidden - id,
            pinned = if (removePin) m.pinned - id else m.pinned,
            days = if (removePin) m.days - id else m.days
        )
    }

    /**
     * Canonical JSON without the size limit (used for local Room storage and hashing).
     * `stay` is included only when [includeStay] is true.
     */
    fun toJson(manifest: PlanManifest, includeStay: Boolean): String {
        val fields = sortedMapOf<String, String>()
        fields["v"] = manifest.version.toString()
        manifest.destinationQid?.let { fields["destQid"] = JSONObject.quote(it) }
        manifest.wikivoyageRevisionId?.let { fields["wvRev"] = it.toString() }
        if (includeStay) manifest.stay?.let { fields["stay"] = stayToJson(it) }
        if (manifest.pinned.isNotEmpty()) fields["pinned"] = longMapToJson(manifest.pinned)
        if (manifest.hidden.isNotEmpty()) fields["hidden"] = longMapToJson(manifest.hidden)
        if (manifest.days.isNotEmpty()) fields["days"] = intMapToJson(manifest.days)
        fields["updatedAt"] = manifest.updatedAtEpochMs.toString()
        manifest.updatedByMemberKey?.let { fields["by"] = JSONObject.quote(it) }
        return objectOf(fields)
    }

    /** Canonical JSON of a stay pin (also used for the LOCAL-only `stayLocalJson` column). */
    fun stayToJson(stay: StayPin): String {
        val fields = sortedMapOf<String, String>()
        fields["lat"] = JSONObject.numberToString(stay.location.lat)
        fields["lng"] = JSONObject.numberToString(stay.location.lng)
        fields["label"] = JSONObject.quote(stay.label)
        fields["prec"] = JSONObject.quote(stay.precision.name)
        stay.setByMemberKey?.let { fields["by"] = JSONObject.quote(it) }
        fields["at"] = stay.setAtEpochMs.toString()
        return objectOf(fields)
    }

    /** SHA-256 of the wire encoding (what peers would receive). */
    fun contentHash(manifest: PlanManifest, shareStay: Boolean): String =
        encode(manifest, shareStay).contentHash

    // ---------------------------------------------------------------------------------------------
    // Decoding (tolerant; never throws)
    // ---------------------------------------------------------------------------------------------

    /** Parses a wire/local manifest. Returns null for anything that is not a valid v1 manifest. */
    fun decode(raw: String?): PlanManifest? {
        if (raw.isNullOrBlank() || raw.length > MAX_DECODE_CHARS) return null
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{")) return null
        return try {
            val o = JSONObject(trimmed)
            val version = strictLong(o.opt("v")) ?: return null
            if (version != SCHEMA_VERSION.toLong()) return null
            val updatedAt = strictLong(o.opt("updatedAt")) ?: return null
            if (updatedAt < 0) return null

            val destQid: String? = when (val d = o.opt("destQid")) {
                null, JSONObject.NULL -> null
                is String -> if (QID_REGEX.matches(d)) d else return null
                else -> return null
            }
            val wvRev: Long? = when (val w = o.opt("wvRev")) {
                null, JSONObject.NULL -> null
                else -> strictLong(w)?.takeIf { it > 0 } ?: return null
            }
            val stay = (o.opt("stay") as? JSONObject)?.let { decodeStayObject(it) }
            PlanManifest(
                version = SCHEMA_VERSION,
                destinationQid = destQid,
                wikivoyageRevisionId = wvRev,
                stay = stay,
                pinned = decodeTimestampMap(o.opt("pinned")),
                hidden = decodeTimestampMap(o.opt("hidden")),
                days = decodeDayMap(o.opt("days")),
                updatedAtEpochMs = updatedAt,
                updatedByMemberKey = sanitizeMemberKey(o.opt("by"))
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Parses a stay produced by [stayToJson]; null when malformed or out of range. */
    fun decodeStay(raw: String?): StayPin? {
        if (raw.isNullOrBlank() || raw.length > MAX_DECODE_CHARS) return null
        return try {
            decodeStayObject(JSONObject(raw.trim()))
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeStayObject(o: JSONObject): StayPin? {
        val lat = (o.opt("lat") as? Number)?.toDouble() ?: return null
        val lng = (o.opt("lng") as? Number)?.toDouble() ?: return null
        if (lat.isNaN() || lng.isNaN()) return null
        val location = try {
            LatLng(lat, lng)
        } catch (_: IllegalArgumentException) {
            return null
        }
        val at = strictLong(o.opt("at"))?.takeIf { it >= 0 } ?: return null
        val precision = when (o.opt("prec")) {
            CoordinatePrecision.APPROXIMATE.name -> CoordinatePrecision.APPROXIMATE
            else -> CoordinatePrecision.EXACT
        }
        val label = sanitizeLabel(o.opt("label") as? String) ?: DEFAULT_STAY_LABEL
        return StayPin(
            location = location,
            label = label,
            precision = precision,
            setByMemberKey = sanitizeMemberKey(o.opt("by")),
            setAtEpochMs = at
        )
    }

    private fun decodeTimestampMap(value: Any?): Map<String, Long> {
        val o = value as? JSONObject ?: return emptyMap()
        val out = sortedMapOf<String, Long>()
        val keys = o.keys()
        while (keys.hasNext() && out.size < MAX_ENTRIES_PER_MAP) {
            val key = keys.next()
            if (!isValidPlaceId(key)) continue
            val ts = strictLong(o.opt(key)) ?: continue
            if (ts < 0) continue
            out[key] = ts
        }
        return out
    }

    private fun decodeDayMap(value: Any?): Map<String, Int> {
        val o = value as? JSONObject ?: return emptyMap()
        val out = sortedMapOf<String, Int>()
        val keys = o.keys()
        while (keys.hasNext() && out.size < MAX_ENTRIES_PER_MAP) {
            val key = keys.next()
            if (!isValidPlaceId(key)) continue
            val day = strictLong(o.opt(key)) ?: continue
            if (day < 1 || day > MAX_DAY) continue
            out[key] = day.toInt()
        }
        return out
    }

    // ---------------------------------------------------------------------------------------------
    // Validation / sanitisation helpers (public for reuse by the store and tests)
    // ---------------------------------------------------------------------------------------------

    fun isValidQid(qid: String?): Boolean = qid != null && QID_REGEX.matches(qid)

    fun isValidPlaceId(id: String?): Boolean = id != null && PLACE_ID_REGEX.matches(id)

    /** Returns a safe member key or null (keys are opaque identifiers; never rendered raw). */
    fun sanitizeMemberKey(value: Any?): String? {
        val s = value as? String ?: return null
        return s.trim().takeIf { MEMBER_KEY_REGEX.matches(it) }
    }

    /**
     * Strips control, format (incl. bidi overrides) and private-use characters, collapses
     * whitespace, and caps the length at [MAX_LABEL_LENGTH] code points. Null when nothing is left.
     */
    fun sanitizeLabel(value: String?): String? {
        if (value == null) return null
        val sb = StringBuilder()
        var i = 0
        var lastWasSpace = true
        var count = 0
        while (i < value.length && count < MAX_LABEL_LENGTH) {
            val cp = value.codePointAt(i)
            i += Character.charCount(cp)
            val type = Character.getType(cp)
            val drop = type == Character.CONTROL.toInt() ||
                type == Character.FORMAT.toInt() ||
                type == Character.PRIVATE_USE.toInt() ||
                type == Character.SURROGATE.toInt() ||
                type == Character.UNASSIGNED.toInt()
            if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) {
                if (!lastWasSpace) {
                    sb.append(' ')
                    count++
                    lastWasSpace = true
                }
                continue
            }
            if (drop) continue
            sb.appendCodePoint(cp)
            count++
            lastWasSpace = false
        }
        return sb.toString().trim().takeIf { it.isNotEmpty() }
    }

    /** Accepts only integral JSON numbers (Int/Long); rejects strings, doubles and big numbers. */
    internal fun strictLong(value: Any?): Long? = when (value) {
        is Int -> value.toLong()
        is Long -> value
        is Short -> value.toLong()
        is Byte -> value.toLong()
        else -> null
    }

    fun utf8Size(s: String): Int = s.toByteArray(Charsets.UTF_8).size

    fun sha256Hex(s: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            val v = b.toInt() and 0xFF
            sb.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
        }
        return sb.toString()
    }

    private val HEX = "0123456789abcdef".toCharArray()

    private fun longMapToJson(map: Map<String, Long>): String =
        objectOf(map.toSortedMap().mapValues { it.value.toString() })

    private fun intMapToJson(map: Map<String, Int>): String =
        objectOf(map.toSortedMap().mapValues { it.value.toString() })

    /** Joins pre-rendered JSON values into an object with keys in iteration (sorted) order. */
    private fun objectOf(fields: Map<String, String>): String {
        val sb = StringBuilder("{")
        var first = true
        for ((k, v) in fields) {
            if (!first) sb.append(',')
            first = false
            sb.append(JSONObject.quote(k)).append(':').append(v)
        }
        return sb.append('}').toString()
    }
}
