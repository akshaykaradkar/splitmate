package com.splitmate.app.data.guide.sync

import com.splitmate.app.data.CloudGroupSyncRepository
import com.splitmate.app.data.guide.PlanManifest
import com.splitmate.app.data.guide.PlanManifestSync
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

/** A decoded manifest together with its ntfy message id and server time (epoch seconds). */
data class NtfyPlanMessage(val manifest: PlanManifest, val messageId: String, val timeEpochSec: Long)

/**
 * [PlanManifestSync] over the group's dedicated ntfy plan topic
 * (`splitmate_v2_plan_<sanitised groupId>`, see `CloudGroupSyncRepository.groupPlanTopicForGroupId`).
 *
 * - publish: `POST https://ntfy.sh/<topic>` with the compact manifest JSON (<= 3,000 B, so it is
 *   always an inline ntfy message and never an attachment that expires after 3 h).
 * - fetch: `GET https://ntfy.sh/<topic>/json?poll=1&since=<lastId | 12h>` (never `since=all`),
 *   tolerating keepalives, foreign/garbage messages, attachments and malformed lines.
 * - The stay is included only when [shareStayForGroup] says the planner opted in (default: never).
 * - No logging (payloads may contain a location); HTTPS only; identified User-Agent via transport.
 *
 * Durability: ntfy caches messages for 12 h, so callers should republish per [shouldRepublish].
 */
class NtfyPlanManifestSync(
    private val http: GuideHttpTransport = UrlConnectionGuideHttpTransport(),
    private val topicForGroupId: (String) -> String = { CloudGroupSyncRepository.groupPlanTopicForGroupId(it) },
    private val shareStayForGroup: suspend (String) -> Boolean = { false },
    private val baseUrl: String = DEFAULT_BASE_URL
) : PlanManifestSync {

    override suspend fun publish(groupId: String, manifest: PlanManifest): Boolean =
        publishForMessageId(groupId, manifest) != null

    /**
     * Publishes and returns ntfy's message id (useful as the next `since=` cursor so a device does
     * not re-download its own message), or null on any failure (including an oversize manifest).
     * Returns an empty string when the publish succeeded but ntfy's response carried no usable id.
     */
    suspend fun publishForMessageId(groupId: String, manifest: PlanManifest): String? {
        return try {
            val shareStay = shareStayForGroup(groupId)
            val encoded = PlanManifestCodec.encode(manifest, shareStay)
            val response = http.post(
                url = "$baseUrl/${topicForGroupId(groupId)}",
                body = encoded.json,
                headers = mapOf("Content-Type" to "application/json; charset=utf-8")
            )
            if (!response.isSuccessful) return null
            parsePublishResponseId(response.body) ?: ""
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun fetchLatest(groupId: String, sinceMessageId: String?): Pair<PlanManifest, String>? =
        fetchAll(groupId, sinceMessageId).lastOrNull()?.let { it.manifest to it.messageId }

    /**
     * All valid manifests after [sinceMessageId] (or from the last 12 h), oldest first.
     * Callers that want full convergence can fold these with [PlanManifestMerger.mergeAll].
     * Returns an empty list on any HTTP/network failure.
     */
    suspend fun fetchAll(groupId: String, sinceMessageId: String?): List<NtfyPlanMessage> {
        return try {
            val topic = topicForGroupId(groupId)
            val response = http.get(buildPollUrl(baseUrl, topic, sinceMessageId), emptyMap())
            if (!response.isSuccessful) return emptyList()
            parsePollResponse(response.body, topic)
                .filter { it.messageId != sinceMessageId }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://ntfy.sh"
        const val TOPIC_PREFIX = "splitmate_v2_plan_"

        /** Fallback replay window when no cursor is known (matches ntfy's 12 h cache). */
        const val FALLBACK_SINCE = "12h"

        /** Republish at least this often so the manifest survives ntfy's 12 h message cache. */
        const val REPUBLISH_INTERVAL_MS: Long = 6L * 60L * 60L * 1000L

        /** ntfy message limit; larger payloads are not ours (ours are <= 3,000 B). */
        const val MAX_MESSAGE_BYTES = 4_096

        private val MESSAGE_ID_REGEX = Regex("^[A-Za-z0-9]{1,32}$")

        /**
         * Republish policy: publish when there is no record of a previous publish, when the
         * content hash changed, when more than 6 h passed since the last publish (to survive the
         * 12 h ntfy cache), or when the clock moved backwards.
         */
        fun shouldRepublish(
            lastPublishedHash: String?,
            currentHash: String,
            lastPublishedAt: Long?,
            now: Long
        ): Boolean {
            if (lastPublishedHash == null || lastPublishedAt == null) return true
            if (lastPublishedHash != currentHash) return true
            if (now < lastPublishedAt) return true
            return now - lastPublishedAt > REPUBLISH_INTERVAL_MS
        }

        /** Builds the poll URL; an invalid or missing cursor falls back to `since=12h`. */
        fun buildPollUrl(baseUrl: String, topic: String, sinceMessageId: String?): String {
            val since = sinceMessageId?.takeIf { MESSAGE_ID_REGEX.matches(it) } ?: FALLBACK_SINCE
            return "$baseUrl/$topic/json?poll=1&since=$since"
        }

        /**
         * Parses ntfy's newline-delimited JSON poll response. Keeps only `event == "message"`
         * lines for [expectedTopic] (when the line carries a topic) whose body decodes as a valid
         * manifest. Result is sorted oldest -> newest by (server time, response order).
         */
        fun parsePollResponse(body: String, expectedTopic: String?): List<NtfyPlanMessage> {
            val out = mutableListOf<Pair<Int, NtfyPlanMessage>>()
            var index = 0
            for (line in body.lineSequence()) {
                index++
                val trimmed = line.trim()
                if (trimmed.isEmpty() || !trimmed.startsWith("{")) continue
                val msg = parsePollLine(trimmed, expectedTopic) ?: continue
                out += index to msg
            }
            return out.sortedWith(compareBy<Pair<Int, NtfyPlanMessage>>({ it.second.timeEpochSec }, { it.first }))
                .map { it.second }
        }

        private fun parsePollLine(line: String, expectedTopic: String?): NtfyPlanMessage? {
            return try {
                val json = JSONObject(line)
                if (json.optString("event") != "message") return null
                val topic = json.optString("topic", "")
                if (expectedTopic != null && topic.isNotEmpty() && topic != expectedTopic) return null
                if (json.has("attachment")) return null
                val id = json.optString("id", "")
                if (!MESSAGE_ID_REGEX.matches(id)) return null
                val message = json.opt("message") as? String ?: return null
                if (PlanManifestCodec.utf8Size(message) > MAX_MESSAGE_BYTES) return null
                val manifest = PlanManifestCodec.decode(message) ?: return null
                val time = PlanManifestCodec.strictLong(json.opt("time")) ?: 0L
                NtfyPlanMessage(manifest, id, time)
            } catch (_: Exception) {
                null
            }
        }

        /** ntfy answers a publish with the message JSON; returns its id when well-formed. */
        fun parsePublishResponseId(body: String?): String? {
            if (body.isNullOrBlank()) return null
            return try {
                JSONObject(body.trim()).optString("id", "").takeIf { MESSAGE_ID_REGEX.matches(it) }
            } catch (_: Exception) {
                null
            }
        }
    }
}
