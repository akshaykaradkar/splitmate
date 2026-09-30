package com.splitmate.app.guide.sync

import com.splitmate.app.data.TripGuidePackEntity
import com.splitmate.app.data.TripPlanManifestEntity
import com.splitmate.app.data.guide.sync.PlanManifestCodec
import com.splitmate.app.data.guide.sync.PlanManifestMerger
import com.splitmate.app.data.guide.sync.TripGuideLocalSource
import com.splitmate.app.data.guide.sync.TripGuideStore
import com.splitmate.app.guide.sync.GuideSyncFixtures.manifest
import com.splitmate.app.guide.sync.GuideSyncFixtures.stay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

@DisplayName("v2.3.4 WS-D: TripGuideStore (packs, manifests, local-only stay)")
class TripGuideStoreTest {

    /** In-memory stand-in for Room, including the manifest -> group foreign key. */
    private class FakeLocalSource(val existingGroups: MutableSet<String> = mutableSetOf("g1")) : TripGuideLocalSource {
        val packs = MutableStateFlow<Map<String, TripGuidePackEntity>>(emptyMap())
        val manifests = MutableStateFlow<Map<String, TripPlanManifestEntity>>(emptyMap())

        override suspend fun getPack(destinationQid: String) = packs.value[destinationQid]
        override fun observePack(destinationQid: String): Flow<TripGuidePackEntity?> = packs.map { it[destinationQid] }
        override suspend fun upsertPack(pack: TripGuidePackEntity) {
            packs.value = packs.value + (pack.destinationQid to pack)
        }
        override suspend fun deleteHardExpiredPacks(nowEpochMs: Long): Int {
            val (expired, kept) = packs.value.values.partition { it.hardExpiryEpochMs <= nowEpochMs }
            packs.value = kept.associateBy { it.destinationQid }
            return expired.size
        }
        override suspend fun getManifest(groupId: String) = manifests.value[groupId]
        override fun observeManifest(groupId: String): Flow<TripPlanManifestEntity?> = manifests.map { it[groupId] }
        override suspend fun upsertManifest(entity: TripPlanManifestEntity) {
            if (entity.groupId !in existingGroups) throw IllegalStateException("FOREIGN KEY constraint failed")
            manifests.value = manifests.value + (entity.groupId to entity)
        }
        override suspend fun getPendingManifests() = manifests.value.values.filter { it.pendingPush }
    }

    private var now = 1_727_600_000_000L
    private val local = FakeLocalSource()
    private val store = TripGuideStore(local) { now }
    private val day = 24L * 60L * 60L * 1000L

    private fun gzip(text: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(text.toByteArray(Charsets.UTF_8)) }
        return bos.toByteArray()
    }

    // ---------------------------------------------------------------------------------------------
    // Packs
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `savePack stores bytes as given and derives 14d soft and 90d hard expiry`() = runBlocking {
        val bytes = gzip("{\"schema\":\"splitmate.guide/1\"}")
        val saved = store.savePack("Q1066187", bytes, "hash1", "Hampi", 4567890L)
        assertEquals(now + 14 * day, saved.softExpiryEpochMs)
        assertEquals(now + 90 * day, saved.hardExpiryEpochMs)
        val loaded = store.loadPack("Q1066187")!!
        assertArrayEquals(bytes, loaded.packGz)
        assertEquals(saved, loaded)
        assertEquals("Hampi", loaded.wikivoyageTitle)
        assertEquals(1, loaded.schemaVersion)
        assertEquals(saved, store.observePack("Q1066187").first())
    }

    @Test
    fun `savePack validates input`() {
        val bytes = gzip("{}")
        assertThrows(IllegalArgumentException::class.java) { runBlocking { store.savePack("hampi", bytes, "h", null, null) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { store.savePack("Q1", "{}".toByteArray(), "h", null, null) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { store.savePack("Q1", ByteArray(0), "h", null, null) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { store.savePack("Q1", bytes, " ", null, null) } }
    }

    @Test
    fun `soft and hard expiry helpers`() = runBlocking {
        val p = store.savePack("Q1", gzip("{}"), "h", null, null)
        assertFalse(TripGuideStore.isSoftExpired(p, now + 14 * day - 1))
        assertTrue(TripGuideStore.isSoftExpired(p, now + 14 * day))
        assertFalse(TripGuideStore.isHardExpired(p, now + 90 * day - 1))
        assertTrue(TripGuideStore.isHardExpired(p, now + 90 * day))
        assertNotNull(store.loadUsablePack("Q1", now + 89 * day))
        assertNull(store.loadUsablePack("Q1", now + 90 * day))
    }

    @Test
    fun `hard-expired packs are evicted, fresh ones kept`() = runBlocking {
        store.savePack("Q1", gzip("{}"), "h", null, null, fetchedAtEpochMs = now - 91 * day)
        store.savePack("Q2", gzip("{}"), "h", null, null, fetchedAtEpochMs = now - 10 * day)
        assertEquals(1, store.evictHardExpiredPacks())
        assertNull(store.loadPack("Q1"))
        assertNotNull(store.loadPack("Q2"))
    }

    // ---------------------------------------------------------------------------------------------
    // Manifests
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `manifest upsert, get and observe round trip`() = runBlocking {
        val m = manifest(pinned = mapOf("wv:see:a" to 1L))
        assertTrue(store.upsertManifest("g1", m))
        assertEquals(m, store.getManifest("g1"))
        assertEquals(m, store.observeManifest("g1").first())
        val row = store.getManifestEntity("g1")!!
        assertEquals("Q1066187", row.destinationQid)
        assertTrue(row.pendingPush)
        assertFalse(row.shareStay, "stay sharing is off by default")
        assertEquals(listOf("g1"), store.pendingPushGroupIds())
    }

    @Test
    fun `manifest writes for an unknown group fail softly`() = runBlocking {
        assertFalse(store.upsertManifest("ghost", manifest()))
        assertNull(store.getManifest("ghost"))
    }

    @Test
    fun `applyRemoteManifest merges, records the cursor and computes pending push`() = runBlocking {
        store.upsertManifest("g1", manifest(pinned = mapOf("wv:see:a" to 5L), updatedAt = 10))
        store.markPushed("g1", "m0")

        // Remote already contains everything we have -> nothing to push.
        val superset = manifest(pinned = mapOf("wv:see:a" to 5L, "wd:Q2" to 6L), updatedAt = 20)
        val merged = store.applyRemoteManifest("g1", superset, "m1")
        assertEquals(PlanManifestMerger.normalize(superset), merged)
        var row = store.getManifestEntity("g1")!!
        assertEquals("m1", row.lastSyncMessageId)
        assertFalse(row.pendingPush)

        // Local-only pin + an older remote -> merged differs from remote -> push needed.
        store.upsertManifest("g1", PlanManifestMerger.pin(merged!!, "wv:eat:b", "me", now = 30))
        store.markPushed("g1", null)
        val older = manifest(pinned = mapOf("wv:see:a" to 5L), updatedAt = 1)
        store.applyRemoteManifest("g1", older, "m2")
        row = store.getManifestEntity("g1")!!
        assertTrue(row.pendingPush)
        assertEquals("m2", row.lastSyncMessageId)
        assertTrue(store.getManifest("g1")!!.pinned.containsKey("wv:eat:b"))
    }

    @Test
    fun `markPushed clears pending and keeps the cursor when no id is known`() = runBlocking {
        store.upsertManifest("g1", manifest())
        store.markPushed("g1", "abc")
        store.upsertManifest("g1", manifest(updatedAt = 5))
        store.markPushed("g1", "")
        val row = store.getManifestEntity("g1")!!
        assertFalse(row.pendingPush)
        assertEquals("abc", row.lastSyncMessageId)
        assertFalse(store.markPushed("g2-missing", "x"))
    }

    // ---------------------------------------------------------------------------------------------
    // Stay privacy
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `local stay never reaches the synced manifest while sharing is off`() = runBlocking {
        store.upsertManifest("g1", manifest())
        val s = stay(by = "me")
        assertTrue(store.setLocalStay("g1", s, "me"))
        assertEquals(s, store.getLocalStay("g1"))
        assertNull(store.getManifest("g1")!!.stay)
        assertNull(store.manifestForPublish("g1")!!.stay)
        assertFalse(store.getManifestEntity("g1")!!.manifestJson.contains("15.335"))
        assertEquals(s, store.effectiveStay("g1"))
    }

    @Test
    fun `opting in shares the local stay, opting out retracts it with a tombstone`() = runBlocking {
        store.setLocalStay("g1", stay(by = "me", at = 1), "me")
        now += 1_000
        assertTrue(store.setShareStay("g1", true, "me"))
        assertTrue(store.isShareStay("g1"))
        val shared = store.manifestForPublish("g1")!!.stay
        assertNotNull(shared)
        assertEquals("me", shared!!.setByMemberKey)
        assertTrue(store.getManifestEntity("g1")!!.pendingPush)

        now += 1_000
        assertTrue(store.setShareStay("g1", false, "me"))
        val m = store.getManifest("g1")!!
        assertNull(m.stay)
        assertTrue(m.hidden.containsKey(PlanManifestCodec.STAY_ID))
        assertNull(store.manifestForPublish("g1")!!.stay)
        assertNotNull(store.getLocalStay("g1"), "local stay itself is kept")
    }

    @Test
    fun `members cannot clear someone else's shared stay, organizers can`() = runBlocking {
        store.upsertManifest("g1", manifest(stay = stay(by = "alice", at = 100)))
        assertFalse(store.clearSharedStay("g1", "bob", setOf("org")))
        assertNotNull(store.getManifest("g1")!!.stay)
        // Clearing my own local stay does not touch alice's shared stay.
        store.clearLocalStay("g1", "bob")
        assertNotNull(store.getManifest("g1")!!.stay)
        assertTrue(store.clearSharedStay("g1", "org", setOf("org")))
        assertNull(store.getManifest("g1")!!.stay)
    }

    @Test
    fun `effective stay prefers the newer of local and shared`() = runBlocking {
        store.upsertManifest("g1", manifest(stay = stay(lat = 10.0, by = "alice", at = 500)))
        store.setLocalStay("g1", stay(lat = 20.0, by = "me", at = 100), "me")
        assertEquals(10.0, store.effectiveStay("g1")!!.location.lat)
        store.setLocalStay("g1", stay(lat = 30.0, by = "me", at = 900), "me")
        assertEquals(30.0, store.effectiveStay("g1")!!.location.lat)
        assertNull(store.effectiveStay("unknown"))
    }
}
