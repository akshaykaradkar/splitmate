package com.splitmate.app.guide.sync

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.sync.PlanManifestCodec
import com.splitmate.app.data.guide.sync.PlanManifestTooLargeException
import com.splitmate.app.guide.sync.GuideSyncFixtures.manifest
import com.splitmate.app.guide.sync.GuideSyncFixtures.stay
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("v2.3.4 WS-D: PlanManifestCodec (compact JSON, 3 KB cap, stay privacy)")
class PlanManifestCodecTest {

    private val full = manifest(
        stay = stay(),
        pinned = mapOf("wv:see:virupaksha-temple" to 1_727_600_000_000L, "wd:Q12345" to 1_727_600_000_500L),
        hidden = mapOf("wd:Q999" to 1_727_600_000_100L),
        days = mapOf("wv:see:virupaksha-temple" to 1)
    )

    @Test
    fun `round trip preserves every field when stay is shared`() {
        val encoded = PlanManifestCodec.encode(full, shareStay = true)
        assertEquals(full, PlanManifestCodec.decode(encoded.json))
        assertTrue(encoded.droppedHiddenIds.isEmpty())
        assertEquals(PlanManifestCodec.utf8Size(encoded.json), encoded.sizeBytes)
    }

    @Test
    fun `uses the compact wire keys`() {
        val o = JSONObject(PlanManifestCodec.encode(full, shareStay = true).json)
        assertEquals(
            setOf("v", "destQid", "wvRev", "stay", "pinned", "hidden", "days", "updatedAt", "by"),
            o.keySet()
        )
        assertEquals(1, o.getInt("v"))
        assertEquals("Q1066187", o.getString("destQid"))
        assertEquals(4567890L, o.getLong("wvRev"))
    }

    @Test
    fun `stay is absent when shareStay is false`() {
        val encoded = PlanManifestCodec.encode(full, shareStay = false)
        val o = JSONObject(encoded.json)
        assertFalse(o.has("stay"))
        assertFalse(encoded.json.contains("15.335"))
        assertFalse(encoded.json.contains("76.46"))
        assertFalse(encoded.json.contains("Our stay"))
        assertNull(encoded.manifest.stay)
        assertNull(PlanManifestCodec.decode(encoded.json)!!.stay)
    }

    @Test
    fun `encoding is canonical regardless of map insertion order`() {
        val a = manifest(pinned = linkedMapOf("b" to 2L, "a" to 1L), hidden = linkedMapOf("z" to 5L, "y" to 4L))
        val b = manifest(pinned = linkedMapOf("a" to 1L, "b" to 2L), hidden = linkedMapOf("y" to 4L, "z" to 5L))
        val ea = PlanManifestCodec.encode(a, true)
        val eb = PlanManifestCodec.encode(b, true)
        assertEquals(ea.json, eb.json)
        assertEquals(ea.contentHash, eb.contentHash)
        assertEquals(64, ea.contentHash.length)
        assertFalse(ea.json.contains(" "))
    }

    @Test
    fun `null and empty members are omitted`() {
        val m = manifest(destQid = null, wvRev = null, by = null)
        val json = PlanManifestCodec.encode(m, true).json
        assertEquals("{\"updatedAt\":1727600000000,\"v\":1}", json)
        assertEquals(m, PlanManifestCodec.decode(json))
    }

    @Test
    fun `oversize manifest trims oldest hidden tombstones first and fits 3000 bytes`() {
        val hidden = (0 until 120).associate { "wd:Q${100000 + it}" to (1_727_000_000_000L + it) } +
            mapOf(PlanManifestCodec.STAY_ID to 1L) // oldest of all, but must never be trimmed
        val m = manifest(hidden = hidden, pinned = mapOf("wv:see:keep-me" to 1_727_700_000_000L))
        assertTrue(PlanManifestCodec.utf8Size(PlanManifestCodec.toJson(m, true)) > PlanManifestCodec.MAX_BYTES)

        val encoded = PlanManifestCodec.encode(m, shareStay = true)
        assertTrue(encoded.sizeBytes <= PlanManifestCodec.MAX_BYTES, "size=${encoded.sizeBytes}")
        assertTrue(encoded.droppedHiddenIds.isNotEmpty())
        // Dropped ids are exactly the oldest ones, in order.
        val expectedOrder = (0 until encoded.droppedHiddenIds.size).map { "wd:Q${100000 + it}" }
        assertEquals(expectedOrder, encoded.droppedHiddenIds)
        assertTrue(encoded.manifest.hidden.containsKey(PlanManifestCodec.STAY_ID))
        assertTrue(encoded.manifest.pinned.containsKey("wv:see:keep-me"))
        // Newest tombstone survives.
        assertTrue(encoded.manifest.hidden.containsKey("wd:Q100119"))
        assertEquals(encoded.manifest, PlanManifestCodec.decode(encoded.json))
    }

    @Test
    fun `trimming a tombstone also drops the stale pin it was hiding`() {
        val hidden = (0 until 200).associate { "wd:Q${200000 + it}" to (1_000L + it) }
        // wd:Q200000 was pinned at 500 then hidden at 1000 -> currently hidden.
        val m = manifest(hidden = hidden, pinned = mapOf("wd:Q200000" to 500L), days = mapOf("wd:Q200000" to 2))
        val encoded = PlanManifestCodec.encode(m, true)
        assertEquals("wd:Q200000", encoded.droppedHiddenIds.first())
        assertFalse(encoded.manifest.pinned.containsKey("wd:Q200000"), "hidden pin must not resurface")
        assertFalse(encoded.manifest.days.containsKey("wd:Q200000"))
    }

    @Test
    fun `fails loudly when trimming cannot reach the limit`() {
        val pinned = (0 until 150).associate { "wv:see:place-number-$it" to (1_727_600_000_000L + it) }
        val m = manifest(pinned = pinned, hidden = mapOf("wd:Q1" to 1L))
        val ex = assertThrows(PlanManifestTooLargeException::class.java) {
            PlanManifestCodec.encode(m, shareStay = true)
        }
        assertTrue(ex.sizeBytes > PlanManifestCodec.MAX_BYTES)
        assertEquals(PlanManifestCodec.MAX_BYTES, ex.limitBytes)
    }

    @Test
    fun `exactly-at-limit payload is accepted`() {
        val m = manifest()
        val size = PlanManifestCodec.utf8Size(PlanManifestCodec.toJson(m, true))
        val encoded = PlanManifestCodec.encode(m, true, maxBytes = size)
        assertEquals(size, encoded.sizeBytes)
        assertThrows(PlanManifestTooLargeException::class.java) { PlanManifestCodec.encode(m, true, maxBytes = size - 1) }
    }

    @Test
    fun `decode rejects foreign, malformed and future payloads`() {
        val rejects = listOf(
            null, "", "   ", "hello", "[1,2]", "{", "{\"hello\":1}",
            "{\"v\":2,\"updatedAt\":1}",
            "{\"v\":1}",
            "{\"v\":\"1\",\"updatedAt\":1}",
            "{\"v\":1,\"updatedAt\":\"1\"}",
            "{\"v\":1,\"updatedAt\":1.5}",
            "{\"v\":1,\"updatedAt\":-1}",
            "{\"v\":1,\"updatedAt\":1,\"destQid\":\"<b>x</b>\"}",
            "{\"v\":1,\"updatedAt\":1,\"destQid\":\"Q0\"}",
            "{\"v\":1,\"updatedAt\":1,\"wvRev\":\"12\"}",
            "{\"v\":1,\"updatedAt\":1,\"wvRev\":-3}",
            "{\"v\":1,\"updatedAt\":1,\"pad\":\"" + "x".repeat(PlanManifestCodec.MAX_DECODE_CHARS) + "\"}"
        )
        for (raw in rejects) assertNull(PlanManifestCodec.decode(raw), "should reject: ${raw?.take(60)}")
    }

    @Test
    fun `decode skips invalid entries but keeps the manifest`() {
        val raw = """
            {"v":1,"updatedAt":10,
             "pinned":{"wv:see:ok":5,"bad key with spaces":6,"wv:see:neg":-1,"wv:see:str":"7","<script>":8},
             "hidden":{"wd:Q2":3,"wd:Q3":1.5},
             "days":{"wv:see:ok":1,"wv:see:zero":0,"wv:see:big":31,"wv:see:str":"2"},
             "by":"has spaces not allowed"}
        """.trimIndent()
        val m = PlanManifestCodec.decode(raw)
        assertNotNull(m)
        assertEquals(mapOf("wv:see:ok" to 5L), m!!.pinned)
        assertEquals(mapOf("wd:Q2" to 3L), m.hidden)
        assertEquals(mapOf("wv:see:ok" to 1), m.days)
        assertNull(m.updatedByMemberKey)
        assertNull(m.destinationQid)
    }

    @Test
    fun `invalid stay is dropped without rejecting the manifest`() {
        val raw = "{\"v\":1,\"updatedAt\":10,\"stay\":{\"lat\":123.0,\"lng\":10,\"label\":\"x\",\"at\":1}}"
        val m = PlanManifestCodec.decode(raw)
        assertNotNull(m)
        assertNull(m!!.stay)
    }

    @Test
    fun `stay labels are sanitised`() {
        val raw = "{\"v\":1,\"updatedAt\":10,\"stay\":{\"lat\":15.3,\"lng\":76.4,\"label\":\"  Hotel\\u202E\\u0000  \\n\\tMayura  \",\"at\":1,\"prec\":\"APPROXIMATE\"}}"
        val s = PlanManifestCodec.decode(raw)!!.stay!!
        assertEquals("Hotel Mayura", s.label)
        assertEquals(CoordinatePrecision.APPROXIMATE, s.precision)
        val blank = "{\"v\":1,\"updatedAt\":10,\"stay\":{\"lat\":15.3,\"lng\":76.4,\"label\":\"\\u0000\",\"at\":1}}"
        assertEquals(PlanManifestCodec.DEFAULT_STAY_LABEL, PlanManifestCodec.decode(blank)!!.stay!!.label)
        val long = PlanManifestCodec.sanitizeLabel("a".repeat(500))!!
        assertEquals(PlanManifestCodec.MAX_LABEL_LENGTH, long.length)
    }

    @Test
    fun `local stay json round trips`() {
        val s = stay(lat = -33.8688, lng = 151.2093, label = "Harbour flat", precision = CoordinatePrecision.APPROXIMATE)
        assertEquals(s, PlanManifestCodec.decodeStay(PlanManifestCodec.stayToJson(s)))
        assertNull(PlanManifestCodec.decodeStay("garbage"))
        assertNull(PlanManifestCodec.decodeStay(null))
    }

    @Test
    fun `unicode labels count UTF-8 bytes toward the limit`() {
        val m = manifest(stay = stay(label = "\u0939\u0902\u092A\u0940 \u0939\u094B\u091F\u0932"))
        val e = PlanManifestCodec.encode(m, true)
        assertEquals(e.json.toByteArray(Charsets.UTF_8).size, e.sizeBytes)
        assertEquals(m, PlanManifestCodec.decode(e.json))
    }
}
