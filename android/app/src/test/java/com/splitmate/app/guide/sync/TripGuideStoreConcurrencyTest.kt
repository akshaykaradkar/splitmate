package com.splitmate.app.guide.sync

import com.splitmate.app.data.TripGuidePackEntity
import com.splitmate.app.data.TripPlanManifestEntity
import com.splitmate.app.data.guide.sync.PlanManifestMerger
import com.splitmate.app.data.guide.sync.TripGuideLocalSource
import com.splitmate.app.data.guide.sync.TripGuideStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

/**
 * v2.4.0 rc13 (audit M-NEW-1): plan edits are an atomic read-modify-write. Before, the repository
 * read the manifest outside the store lock and then replaced it whole, so a double tap on
 * "Add to loop" or a sync merge landing in between silently dropped pins.
 */
@DisplayName("v2.4.0 rc13: no lost pins on concurrent plan edits")
class TripGuideStoreConcurrencyTest {

    /** Room stand-in whose reads are SLOW, to force edits to interleave. */
    private class SlowLocalSource : TripGuideLocalSource {
        val manifests = MutableStateFlow<Map<String, TripPlanManifestEntity>>(emptyMap())
        override suspend fun getPack(destinationQid: String): TripGuidePackEntity? = null
        override fun observePack(destinationQid: String): Flow<TripGuidePackEntity?> = MutableStateFlow(null)
        override suspend fun upsertPack(pack: TripGuidePackEntity) = Unit
        override suspend fun deleteHardExpiredPacks(nowEpochMs: Long): Int = 0
        override suspend fun getManifest(groupId: String): TripPlanManifestEntity? {
            val snapshot = manifests.value[groupId]
            delay(20) // a slow disk read: other edits get scheduled in between
            return snapshot
        }
        override fun observeManifest(groupId: String): Flow<TripPlanManifestEntity?> = manifests.map { it[groupId] }
        override suspend fun upsertManifest(entity: TripPlanManifestEntity) {
            manifests.value = manifests.value + (entity.groupId to entity)
        }
        override suspend fun getPendingManifests() = manifests.value.values.filter { it.pendingPush }
    }

    @Test
    fun `parallel pins and a sync merge all survive`() = runBlocking {
        val local = SlowLocalSource()
        var now = 1_790_000_000_000L
        val store = TripGuideStore(local) { now++ }
        val ids = (1..8).map { "wd:Q10$it" }
        val remote = PlanManifestMerger.pin(TripGuideStore.emptyManifest(), "wd:Q999", "peer", 1_790_000_000_500L)

        val jobs = ids.map { id ->
            async(Dispatchers.Default) { store.editManifest("g1") { m -> PlanManifestMerger.pin(m, id, "me", 1_790_000_000_100L) } }
        } + async(Dispatchers.Default) { store.applyRemoteManifest("g1", remote, messageId = "msg-1") }
        jobs.awaitAll()

        val pinned = store.getManifest("g1")!!.pinned.keys
        assertTrue(pinned.containsAll(ids), "every local pin survives: $pinned")
        assertTrue("wd:Q999" in pinned, "the friend's pin from sync survives: $pinned")
    }

    @Test
    fun `a no-op edit writes nothing and returns the current plan`() = runBlocking {
        val local = SlowLocalSource()
        val store = TripGuideStore(local) { 1L }
        val result = store.editManifest("g1") { null }
        assertEquals(TripGuideStore.emptyManifest().pinned, result!!.pinned)
        assertTrue(local.manifests.value.isEmpty(), "nothing persisted for a no-op")
    }

    @Test
    fun `the repository edits through the atomic store call`() {
        val repo = File("src/main/java/com/splitmate/app/data/guide/TripGuideRepository.kt").readText()
        val edit = repo.substringAfter("private suspend fun editManifest(").substringBefore("\n\n")
        assertTrue(edit.contains("store.editManifest(groupId, edit)"), edit)
        assertTrue(!edit.contains("manifest(groupId)"), "no read outside the lock")
    }
}
