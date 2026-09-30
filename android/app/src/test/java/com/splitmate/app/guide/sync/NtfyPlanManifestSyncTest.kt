package com.splitmate.app.guide.sync

import com.splitmate.app.data.CloudGroupSyncRepository
import com.splitmate.app.data.guide.sync.GuideHttpResponse
import com.splitmate.app.data.guide.sync.NtfyPlanManifestSync
import com.splitmate.app.data.guide.sync.PlanManifestCodec
import com.splitmate.app.guide.sync.GuideSyncFixtures.manifest
import com.splitmate.app.guide.sync.GuideSyncFixtures.stay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("v2.3.4 WS-D: NtfyPlanManifestSync (plan topic transport, fixtures, republish policy)")
class NtfyPlanManifestSyncTest {

    private val groupId = "g_hampi"
    private val topic = "splitmate_v2_plan_g_hampi"
    private val fixture by lazy { GuideSyncFixtures.resource("guide/sync/ntfy_plan_poll_mixed.jsonl") }

    // ---------------------------------------------------------------------------------------------
    // Topic
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `plan topic is separate from the ledger topic and sanitised`() {
        assertEquals(topic, CloudGroupSyncRepository.groupPlanTopicForGroupId(groupId))
        assertTrue(CloudGroupSyncRepository.groupPlanTopicForGroupId("x").startsWith(NtfyPlanManifestSync.TOPIC_PREFIX))
        assertNotEquals(
            CloudGroupSyncRepository.groupTopicForGroupId(groupId),
            CloudGroupSyncRepository.groupPlanTopicForGroupId(groupId)
        )
        assertEquals("splitmate_v2_plan_g_1_2__x", CloudGroupSyncRepository.groupPlanTopicForGroupId("g 1/2?&x"))
    }

    // ---------------------------------------------------------------------------------------------
    // Parsing (fixtures)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `poll parsing keeps only valid manifests for our topic, oldest first`() {
        val parsed = NtfyPlanManifestSync.parsePollResponse(fixture, topic)
        assertEquals(listOf("msgOld00001", "msgNew00001"), parsed.map { it.messageId })
        val newest = parsed.last().manifest
        assertEquals("Q1066187", newest.destinationQid)
        assertEquals(4567890L, newest.wikivoyageRevisionId)
        assertEquals(setOf("wv:see:virupaksha-temple", "wv:eat:mango-tree"), newest.pinned.keys)
        assertEquals(mapOf("wd:Q12345" to 1_727_600_700_000L), newest.hidden)
        assertEquals(mapOf("wv:see:virupaksha-temple" to 1), newest.days)
        assertEquals("Our stay", newest.stay!!.label)
        assertEquals(1_727_600_800L, parsed.last().timeEpochSec)
    }

    @Test
    fun `poll parsing without a topic filter still rejects garbage`() {
        val parsed = NtfyPlanManifestSync.parsePollResponse(fixture, expectedTopic = null)
        assertEquals(listOf("msgOld00001", "msgNew00001", "msgOther0001"), parsed.map { it.messageId })
    }

    @Test
    fun `empty, garbage and binary-ish bodies parse to nothing`() {
        assertTrue(NtfyPlanManifestSync.parsePollResponse("", topic).isEmpty())
        assertTrue(NtfyPlanManifestSync.parsePollResponse("\n\n  \n", topic).isEmpty())
        assertTrue(NtfyPlanManifestSync.parsePollResponse("<html>502 Bad Gateway</html>", topic).isEmpty())
        assertTrue(NtfyPlanManifestSync.parsePollResponse("{\u0000\u0001}", topic).isEmpty())
    }

    @Test
    fun `same-second messages keep server order`() {
        val m1 = PlanManifestCodec.encode(manifest(destQid = "Q1"), false).json
        val m2 = PlanManifestCodec.encode(manifest(destQid = "Q2"), false).json
        val body = listOf(
            JSONObject().put("id", "aaa1").put("time", 100).put("event", "message").put("topic", topic).put("message", m1),
            JSONObject().put("id", "aaa2").put("time", 100).put("event", "message").put("topic", topic).put("message", m2)
        ).joinToString("\n") { it.toString() }
        assertEquals(listOf("Q1", "Q2"), NtfyPlanManifestSync.parsePollResponse(body, topic).map { it.manifest.destinationQid })
    }

    @Test
    fun `oversized message bodies are ignored`() {
        val padded = "{\"v\":1,\"updatedAt\":1,\"pad\":\"" + "x".repeat(5_000) + "\"}"
        val line = JSONObject().put("id", "big1").put("time", 1).put("event", "message").put("topic", topic).put("message", padded)
        assertTrue(NtfyPlanManifestSync.parsePollResponse(line.toString(), topic).isEmpty())
    }

    // ---------------------------------------------------------------------------------------------
    // Fetch
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `fetchLatest without a cursor polls since=12h, never since=all`() = runBlocking {
        val http = FakeGuideHttpTransport.ok(fixture)
        val sync = NtfyPlanManifestSync(http = http)
        val result = sync.fetchLatest(groupId, null)
        assertNotNull(result)
        assertEquals("msgNew00001", result!!.second)
        assertEquals("Q1066187", result.first.destinationQid)
        val url = http.requests.single().url
        assertEquals("https://ntfy.sh/$topic/json?poll=1&since=12h", url)
        assertFalse(url.contains("since=all"))
        assertEquals("GET", http.requests.single().method)
    }

    @Test
    fun `fetchLatest with a cursor polls since=id and skips the cursor message itself`() = runBlocking {
        val http = FakeGuideHttpTransport.ok(fixture)
        val sync = NtfyPlanManifestSync(http = http)
        val result = sync.fetchLatest(groupId, "msgNew00001")
        assertEquals("https://ntfy.sh/$topic/json?poll=1&since=msgNew00001", http.requests.single().url)
        assertEquals("msgOld00001", result!!.second)
    }

    @Test
    fun `invalid cursor falls back to the 12h window`() {
        assertEquals(
            "https://ntfy.sh/t/json?poll=1&since=12h",
            NtfyPlanManifestSync.buildPollUrl("https://ntfy.sh", "t", "all&x=1")
        )
        assertEquals(
            "https://ntfy.sh/t/json?poll=1&since=12h",
            NtfyPlanManifestSync.buildPollUrl("https://ntfy.sh", "t", "")
        )
    }

    @Test
    fun `fetch failures return null instead of throwing`() = runBlocking {
        assertNull(NtfyPlanManifestSync(http = FakeGuideHttpTransport.failing()).fetchLatest(groupId, null))
        assertNull(NtfyPlanManifestSync(http = FakeGuideHttpTransport { _, _, _ -> GuideHttpResponse(429, "") }).fetchLatest(groupId, null))
        assertNull(NtfyPlanManifestSync(http = FakeGuideHttpTransport.ok("")).fetchLatest(groupId, null))
        assertTrue(NtfyPlanManifestSync(http = FakeGuideHttpTransport.failing()).fetchAll(groupId, null).isEmpty())
    }

    // ---------------------------------------------------------------------------------------------
    // Publish
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `publish posts compact json to the plan topic and omits the stay by default`() = runBlocking {
        val http = FakeGuideHttpTransport { _, _, _ ->
            GuideHttpResponse(200, "{\"id\":\"pub123\",\"time\":1727600000,\"event\":\"message\",\"topic\":\"$topic\"}")
        }
        val sync = NtfyPlanManifestSync(http = http)
        val m = manifest(stay = stay(), pinned = mapOf("wv:see:a" to 1L))
        assertEquals("pub123", sync.publishForMessageId(groupId, m))
        val req = http.requests.single()
        assertEquals("POST", req.method)
        assertEquals("https://ntfy.sh/$topic", req.url)
        assertFalse(JSONObject(req.body!!).has("stay"), "stay must not be published unless shared")
        assertTrue(req.body!!.toByteArray(Charsets.UTF_8).size <= PlanManifestCodec.MAX_BYTES)
        assertEquals(m.copy(stay = null), PlanManifestCodec.decode(req.body))
    }

    @Test
    fun `publish includes the stay only when sharing is opted in`() = runBlocking {
        val http = FakeGuideHttpTransport.ok("{\"id\":\"pub1\"}")
        val sync = NtfyPlanManifestSync(http = http, shareStayForGroup = { it == groupId })
        assertTrue(sync.publish(groupId, manifest(stay = stay())))
        assertTrue(JSONObject(http.requests.single().body!!).has("stay"))
    }

    @Test
    fun `publish returns false on http errors, network errors and oversize manifests`() = runBlocking {
        assertFalse(NtfyPlanManifestSync(http = FakeGuideHttpTransport { _, _, _ -> GuideHttpResponse(500, "") }).publish(groupId, manifest()))
        assertFalse(NtfyPlanManifestSync(http = FakeGuideHttpTransport.failing()).publish(groupId, manifest()))
        val http = FakeGuideHttpTransport.ok("{}")
        val huge = manifest(pinned = (0 until 200).associate { "wv:see:place-number-$it" to it.toLong() })
        assertFalse(NtfyPlanManifestSync(http = http).publish(groupId, huge))
        assertTrue(http.requests.isEmpty(), "oversize manifests are never sent")
    }

    @Test
    fun `publish succeeds even if ntfy returns no id`() = runBlocking {
        val sync = NtfyPlanManifestSync(http = FakeGuideHttpTransport.ok("not json"))
        assertEquals("", sync.publishForMessageId(groupId, manifest()))
        assertTrue(sync.publish(groupId, manifest()))
    }

    // ---------------------------------------------------------------------------------------------
    // Republish policy
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `republish when never published, hash changed, over 6h old or clock skew`() {
        val h = "abc"
        val t0 = 1_727_600_000_000L
        val sixHours = NtfyPlanManifestSync.REPUBLISH_INTERVAL_MS
        assertTrue(NtfyPlanManifestSync.shouldRepublish(null, h, null, t0))
        assertTrue(NtfyPlanManifestSync.shouldRepublish(h, h, null, t0))
        assertTrue(NtfyPlanManifestSync.shouldRepublish("old", h, t0, t0 + 1))
        assertFalse(NtfyPlanManifestSync.shouldRepublish(h, h, t0, t0 + 1))
        assertFalse(NtfyPlanManifestSync.shouldRepublish(h, h, t0, t0 + sixHours))
        assertTrue(NtfyPlanManifestSync.shouldRepublish(h, h, t0, t0 + sixHours + 1))
        assertTrue(NtfyPlanManifestSync.shouldRepublish(h, h, t0, t0 - 1))
        assertEquals(6L * 60 * 60 * 1000, sixHours)
    }
}
