package com.splitmate.app

import com.splitmate.app.data.CloudGroupSyncRepository
import com.splitmate.app.data.CloudGroupSyncRepository.IndexPushDecision
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * v2.3.6 sync durability: ntfy.sh only caches messages for ~12h, so discovery records
 * (per-phone group index, user profile, join-code pointer) must be re-published periodically,
 * otherwise a fresh install / new phone signing in with the same number finds no trips.
 */
class SplitMateV236SyncDurabilityTest {

    private val interval = CloudGroupSyncRepository.DISCOVERY_REFRESH_INTERVAL_MS
    private val now = 1_800_000_000_000L

    @Test
    fun `SD_01 refresh interval is strictly inside the ntfy cache window`() {
        assertTrue(interval > 0L)
        assertTrue(interval < CloudGroupSyncRepository.NTFY_CACHE_WINDOW_MS)
        assertTrue(CloudGroupSyncRepository.CODE_POINTER_REFRESH_INTERVAL_MS < CloudGroupSyncRepository.NTFY_CACHE_WINDOW_MS)
        assertTrue(CloudGroupSyncRepository.PROFILE_REFRESH_CHECK_INTERVAL_MS < CloudGroupSyncRepository.NTFY_CACHE_WINDOW_MS)
    }

    @Test
    fun `SD_02 stale or never-pushed index entry is always re-published even on passive polls`() {
        listOf(0L, now - interval, now - interval - 1L, now - CloudGroupSyncRepository.NTFY_CACHE_WINDOW_MS).forEach { last ->
            listOf(true, false).forEach { changed ->
                listOf(true, false).forEach { allowed ->
                    assertEquals(
                        IndexPushDecision.PUSH,
                        CloudGroupSyncRepository.decidePhoneIndexPush(now, last, changed, forcePush = false, networkPushAllowed = allowed),
                        "last=$last changed=$changed allowed=$allowed"
                    )
                }
            }
        }
    }

    @Test
    fun `SD_03 fresh index entry keeps the original delta-push behaviour`() {
        val fresh = now - interval / 2
        assertEquals(IndexPushDecision.SKIP, CloudGroupSyncRepository.decidePhoneIndexPush(now, fresh, false, false, true))
        assertEquals(IndexPushDecision.SKIP, CloudGroupSyncRepository.decidePhoneIndexPush(now, fresh, false, false, false))
        assertEquals(IndexPushDecision.PUSH, CloudGroupSyncRepository.decidePhoneIndexPush(now, fresh, true, false, true))
        assertEquals(IndexPushDecision.SEED_TOKEN_ONLY, CloudGroupSyncRepository.decidePhoneIndexPush(now, fresh, true, false, false))
        assertEquals(IndexPushDecision.PUSH, CloudGroupSyncRepository.decidePhoneIndexPush(now, fresh, false, true, false))
    }

    @Test
    fun `SD_04 profile is re-published only when the cloud GET succeeded and found nothing`() {
        assertTrue(CloudGroupSyncRepository.shouldRepublishProfile(remoteFetchSucceeded = true, remoteProfileExists = false))
        assertFalse(CloudGroupSyncRepository.shouldRepublishProfile(remoteFetchSucceeded = true, remoteProfileExists = true))
        assertFalse(CloudGroupSyncRepository.shouldRepublishProfile(remoteFetchSucceeded = false, remoteProfileExists = false))
        assertFalse(CloudGroupSyncRepository.shouldRepublishProfile(remoteFetchSucceeded = false, remoteProfileExists = true))
    }

    @Test
    fun `SD_05 avatar style and preset are parsed from any canonical seed with safe defaults`() {
        val styles = listOf("open-peeps", "adventurer", "avataaars", "notionists")
        val presets = listOf("Buckwheat", "BoldPop", "PastelWall")
        styles.forEach { st ->
            presets.forEach { pr ->
                val seed = "Any Name_${st.length}|Female|$st|$pr"
                assertEquals(st to pr, CloudGroupSyncRepository.parseAvatarStyleAndPreset(seed))
            }
        }
        assertEquals("open-peeps" to "Buckwheat", CloudGroupSyncRepository.parseAvatarStyleAndPreset(""))
        assertEquals("open-peeps" to "Buckwheat", CloudGroupSyncRepository.parseAvatarStyleAndPreset("OnlyName"))
        assertEquals("open-peeps" to "Buckwheat", CloudGroupSyncRepository.parseAvatarStyleAndPreset("n|g| | "))
    }

    @Test
    fun `SD_06 restore flow refreshes the profile and passive polls refresh index and join code`() {
        val src = File("src/main/java/com/splitmate/app/data/CloudGroupSyncRepository.kt").readText()
        val restoreBody = src.substringAfter("suspend fun restoreAndSyncAllForVerifiedPhone(")
        assertTrue(restoreBody.contains("refreshUserProfileInCloudIfExpired("), "restore must refresh expired profile")
        assertTrue(src.contains("decidePhoneIndexPush("), "index push must use the staleness-aware decision")
        assertFalse(
            src.contains("hasExplicitMutation && (postMergeMs - lastCodeTs) >= CODE_POINTER_REFRESH_INTERVAL_MS"),
            "join-code pointer must refresh on interval even during passive polls"
        )
    }
}
