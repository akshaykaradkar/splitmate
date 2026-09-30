package com.splitmate.app.data.guide.content

import com.splitmate.app.data.guide.CommonsImage
import com.splitmate.app.data.guide.Destination
import com.splitmate.app.data.guide.GuidePack
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.data.guide.WikivoyageRef
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Thrown when a stored pack cannot be decoded (corrupt, too large, or unknown schema). */
class GuidePackFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Output of [GuidePackCodec.encode]: the (possibly trimmed) pack and its stored forms. */
data class EncodedGuidePack(
    val pack: GuidePack,
    val canonicalJson: String,
    val gzip: ByteArray,
    /** Lowercase hex sha256 of the canonical JSON UTF-8 bytes. */
    val contentHash: String,
    val rawBytes: Int,
    /** True when blurbs/places were trimmed to meet [GuidePackCodec.MAX_RAW_BYTES]. */
    val trimmed: Boolean,
    /** Non-PII diagnostics (e.g. "raw 70123 B > budget; trimmed blurbs to 140"). */
    val warnings: List<String>
) {
    override fun equals(other: Any?): Boolean =
        other is EncodedGuidePack && other.contentHash == contentHash && other.trimmed == trimmed
    override fun hashCode(): Int = contentHash.hashCode()
}

/**
 * Canonical JSON (schema `splitmate.guide/1`) + gzip codec for [GuidePack].
 *
 * Canonical form: fixed key order (writer below, NOT org.json's unordered JSONObject), null
 * fields omitted, doubles via `Double.toString` (round-trip exact), UTF-8, no insignificant
 * whitespace. Therefore equal packs ⇒ byte-identical JSON ⇒ identical [contentHash], and
 * `decode(encode(p)) == p` (lossless). Gzip output is deterministic (JDK writes mtime 0).
 *
 * Size budget (audit §3.3): raw JSON ≤ 64 KB. [encode] trims progressively when over budget:
 * blurbs → 140 chars, then SLEEP blurbs dropped, then all blurbs dropped, then trailing
 * listings dropped. Decoding is bounded ([MAX_DECOMPRESSED_BYTES]) against gzip bombs.
 */
object GuidePackCodec {

    const val MAX_RAW_BYTES = 64 * 1024
    const val TARGET_RAW_BYTES = 30 * 1024
    const val MAX_DECOMPRESSED_BYTES = 1024 * 1024
    private const val TRIMMED_BLURB_CHARS = 140

    // ---- public API -----------------------------------------------------------------------------

    /** Canonical JSON with stable key order. */
    fun toCanonicalJson(pack: GuidePack): String {
        val w = JsonWriter()
        w.obj {
            field("schema", pack.schema)
            key("destination"); destination(pack.destination)
            pack.wikivoyage?.let { key("wikivoyage"); wikivoyage(it) }
            key("places"); places(pack.places)
            key("nearby"); places(pack.nearby)
            key("attribution"); array(pack.attribution) { string(it) }
            field("fetchedAt", pack.fetchedAtEpochMs)
        }
        return w.toString()
    }

    /** Parses canonical (or any key-order) JSON. @throws GuidePackFormatException */
    fun fromJson(json: String): GuidePack {
        try {
            val o = JSONObject(json)
            val schema = o.getString("schema")
            if (schema != GuidePack.SCHEMA_V1) throw GuidePackFormatException("Unsupported schema: ${schema.take(40)}")
            return GuidePack(
                schema = schema,
                destination = readDestination(o.getJSONObject("destination")),
                wikivoyage = o.optJSONObject("wikivoyage")?.let { readWikivoyage(it) },
                places = readPlaces(o.optJSONArray("places")),
                nearby = readPlaces(o.optJSONArray("nearby")),
                attribution = o.optJSONArray("attribution")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
                fetchedAtEpochMs = o.getLong("fetchedAt")
            )
        } catch (e: GuidePackFormatException) {
            throw e
        } catch (e: Exception) {
            throw GuidePackFormatException("Corrupt guide pack JSON", e)
        }
    }

    fun gzip(json: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(json.toByteArray(Charsets.UTF_8)) }
        return bos.toByteArray()
    }

    /** Bounded gunzip. @throws GuidePackFormatException when corrupt or over [MAX_DECOMPRESSED_BYTES]. */
    fun gunzip(bytes: ByteArray, maxBytes: Int = MAX_DECOMPRESSED_BYTES): String {
        return try {
            GZIPInputStream(ByteArrayInputStream(bytes)).use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > maxBytes) throw GuidePackFormatException("Guide pack exceeds $maxBytes bytes")
                    out.write(buf, 0, n)
                }
                String(out.toByteArray(), Charsets.UTF_8)
            }
        } catch (e: GuidePackFormatException) {
            throw e
        } catch (e: Exception) {
            throw GuidePackFormatException("Corrupt gzip guide pack", e)
        }
    }

    fun encodeGzip(pack: GuidePack): ByteArray = gzip(toCanonicalJson(pack))

    fun decodeGzip(bytes: ByteArray): GuidePack = fromJson(gunzip(bytes))

    fun contentHash(pack: GuidePack): String = sha256Hex(toCanonicalJson(pack))

    fun sha256Hex(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    fun rawSize(pack: GuidePack): Int = toCanonicalJson(pack).toByteArray(Charsets.UTF_8).size

    /** Returns the pack trimmed to fit [maxRawBytes] (unchanged if it already fits). */
    fun enforceBudget(pack: GuidePack, maxRawBytes: Int = MAX_RAW_BYTES): Pair<GuidePack, List<String>> {
        val warnings = ArrayList<String>()
        var size = rawSize(pack)
        if (size <= maxRawBytes) return pack to warnings
        warnings += "raw $size B > budget $maxRawBytes B"

        var p = pack.copy(
            places = pack.places.map { it.copy(blurb = it.blurb?.let { b -> WikiTextSanitizer.truncate(b, TRIMMED_BLURB_CHARS).text }) },
            nearby = pack.nearby.map { it.copy(blurb = it.blurb?.let { b -> WikiTextSanitizer.truncate(b, TRIMMED_BLURB_CHARS).text }) }
        )
        size = rawSize(p)
        warnings += "trimmed blurbs to $TRIMMED_BLURB_CHARS chars -> $size B"
        if (size <= maxRawBytes) return p to warnings

        p = p.copy(places = p.places.map { if (it.kind == PlaceKind.SLEEP) it.copy(blurb = null) else it })
        size = rawSize(p)
        warnings += "dropped SLEEP blurbs -> $size B"
        if (size <= maxRawBytes) return p to warnings

        p = p.copy(places = p.places.map { it.copy(blurb = null) }, nearby = p.nearby.map { it.copy(blurb = null) })
        size = rawSize(p)
        warnings += "dropped all blurbs -> $size B"
        if (size <= maxRawBytes) return p to warnings

        var places = p.places
        var nearby = p.nearby
        while (size > maxRawBytes && (places.isNotEmpty() || nearby.isNotEmpty())) {
            if (nearby.isNotEmpty()) nearby = nearby.dropLast(1) else places = places.dropLast(1)
            p = p.copy(places = places, nearby = nearby)
            size = rawSize(p)
        }
        warnings += "dropped trailing places -> ${p.places.size} places, ${p.nearby.size} nearby, $size B"
        return p to warnings
    }

    /** Budget-enforced canonical JSON + gzip + hash. */
    fun encode(pack: GuidePack, maxRawBytes: Int = MAX_RAW_BYTES): EncodedGuidePack {
        val (fitted, warnings) = enforceBudget(pack, maxRawBytes)
        val json = toCanonicalJson(fitted)
        return EncodedGuidePack(
            pack = fitted,
            canonicalJson = json,
            gzip = gzip(json),
            contentHash = sha256Hex(json),
            rawBytes = json.toByteArray(Charsets.UTF_8).size,
            trimmed = fitted != pack,
            warnings = warnings
        )
    }

    // ---- writing ----------------------------------------------------------------------------------

    private fun JsonWriter.destination(d: Destination) = obj {
        field("qid", d.qid)
        field("label", d.label)
        field("description", d.description)
        field("lat", d.location.lat)
        field("lng", d.location.lng)
        d.hero?.let { key("hero"); image(it) }
    }

    private fun JsonWriter.wikivoyage(r: WikivoyageRef) = obj {
        field("title", r.title)
        field("revid", r.revisionId)
        field("url", r.url)
        field("license", r.license)
    }

    private fun JsonWriter.image(i: CommonsImage) = obj {
        field("file", i.fileName)
        field("thumbUrl", i.thumbUrl)
        field("width", i.width.toLong())
        field("artist", i.artist)
        field("license", i.license)
        field("licenseUrl", i.licenseUrl)
        field("sourceUrl", i.sourceUrl)
    }

    private fun JsonWriter.places(list: List<Place>) = array(list) { place(it) }

    private fun JsonWriter.place(p: Place) = obj {
        field("id", p.id)
        field("kind", p.kind.name)
        field("name", p.name)
        p.location?.let { field("lat", it.lat); field("lng", it.lng) }
        field("qid", p.qid)
        field("blurb", p.blurb)
        field("hours", p.hours)
        field("price", p.price)
        field("address", p.address)
        field("source", p.source.name)
        p.image?.let { key("image"); image(it) }
        p.distanceKmFromCenter?.takeIf { !it.isNaN() && !it.isInfinite() }?.let { field("distKm", it) }
    }

    // ---- reading ----------------------------------------------------------------------------------

    private fun readDestination(o: JSONObject) = Destination(
        qid = o.getString("qid"),
        label = o.getString("label"),
        description = o.optStringOrNull("description"),
        location = LatLng(o.getDouble("lat"), o.getDouble("lng")),
        hero = o.optJSONObject("hero")?.let { readImage(it) }
    )

    private fun readWikivoyage(o: JSONObject) = WikivoyageRef(
        title = o.getString("title"),
        revisionId = o.getLong("revid"),
        url = o.getString("url"),
        license = o.optStringOrNull("license") ?: "CC BY-SA 4.0"
    )

    private fun readImage(o: JSONObject) = CommonsImage(
        fileName = o.getString("file"),
        thumbUrl = o.getString("thumbUrl"),
        width = o.getInt("width"),
        artist = o.optStringOrNull("artist"),
        license = o.optStringOrNull("license"),
        licenseUrl = o.optStringOrNull("licenseUrl"),
        sourceUrl = o.optStringOrNull("sourceUrl")
    )

    private fun readPlaces(a: JSONArray?): List<Place> {
        if (a == null) return emptyList()
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            Place(
                id = o.getString("id"),
                kind = PlaceKind.valueOf(o.getString("kind")),
                name = o.getString("name"),
                location = if (o.has("lat") && o.has("lng")) LatLng(o.getDouble("lat"), o.getDouble("lng")) else null,
                qid = o.optStringOrNull("qid"),
                blurb = o.optStringOrNull("blurb"),
                hours = o.optStringOrNull("hours"),
                price = o.optStringOrNull("price"),
                address = o.optStringOrNull("address"),
                source = PlaceSource.valueOf(o.getString("source")),
                image = o.optJSONObject("image")?.let { readImage(it) },
                distanceKmFromCenter = if (o.has("distKm")) o.getDouble("distKm") else null
            )
        }
    }

    // ---- minimal ordered JSON writer ----------------------------------------------------------------

    private class JsonWriter {
        private val sb = StringBuilder()
        private val needComma = ArrayList<Boolean>()

        fun obj(body: JsonWriter.() -> Unit) {
            beforeValue()
            sb.append('{'); needComma.add(false)
            body()
            needComma.removeAt(needComma.size - 1); sb.append('}')
        }

        fun <T> array(items: List<T>, each: JsonWriter.(T) -> Unit) {
            beforeValue()
            sb.append('['); needComma.add(false)
            items.forEach { each(it) }
            needComma.removeAt(needComma.size - 1); sb.append(']')
        }

        private var pendingKey = false

        fun key(k: String) {
            if (needComma.isNotEmpty() && needComma[needComma.size - 1]) sb.append(',')
            writeString(k); sb.append(':')
            pendingKey = true
        }

        private fun beforeValue() {
            if (pendingKey) {
                pendingKey = false
            } else if (needComma.isNotEmpty() && needComma[needComma.size - 1]) {
                sb.append(',')
            }
            if (needComma.isNotEmpty()) needComma[needComma.size - 1] = true
        }

        fun string(v: String) { beforeValue(); writeString(v) }

        fun field(k: String, v: String?) { if (v != null) { key(k); string(v) } }

        fun field(k: String, v: Long) { key(k); beforeValue(); sb.append(v) }

        fun field(k: String, v: Double) {
            require(!v.isNaN() && !v.isInfinite()) { "non-finite number for $k" }
            key(k); beforeValue(); sb.append(v.toString())
        }

        private fun writeString(s: String) {
            sb.append('"')
            for (c in s) {
                when {
                    c == '"' -> sb.append("\\\"")
                    c == '\\' -> sb.append("\\\\")
                    c == '\n' -> sb.append("\\n")
                    c == '\r' -> sb.append("\\r")
                    c == '\t' -> sb.append("\\t")
                    c < ' ' || c == '\u2028' || c == '\u2029' -> sb.append(String.format("\\u%04x", c.code))
                    else -> sb.append(c)
                }
            }
            sb.append('"')
        }

        override fun toString(): String = sb.toString()
    }
}
