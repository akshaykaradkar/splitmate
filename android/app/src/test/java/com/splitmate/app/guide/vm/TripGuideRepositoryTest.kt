package com.splitmate.app.guide.vm

import com.splitmate.app.data.guide.BuildOutcome
import com.splitmate.app.data.guide.ResolveOutcome
import com.splitmate.app.data.guide.TripGuideRepository
import com.splitmate.app.data.guide.content.GuideContentException
import com.splitmate.app.data.guide.sync.NtfyPlanManifestSync
import com.splitmate.app.data.guide.sync.NtfyPlanMessage
import com.splitmate.app.data.guide.sync.PlanManifestMerger
import com.splitmate.app.guide.vm.GuideVmFixtures.DAY
import com.splitmate.app.guide.vm.GuideVmFixtures.GROUP
import com.splitmate.app.guide.vm.GuideVmFixtures.HAMPI_CENTER
import com.splitmate.app.guide.vm.GuideVmFixtures.HAMPI_QID
import com.splitmate.app.guide.vm.GuideVmFixtures.MEMBER
import com.splitmate.app.guide.vm.GuideVmFixtures.NOW
import com.splitmate.app.guide.vm.GuideVmFixtures.REV_1
import com.splitmate.app.guide.vm.GuideVmFixtures.REV_2
import com.splitmate.app.guide.vm.GuideVmFixtures.STAY_LOC
import com.splitmate.app.guide.vm.GuideVmFixtures.hampiCandidate
import com.splitmate.app.guide.vm.GuideVmFixtures.hemakuta
import com.splitmate.app.guide.vm.GuideVmFixtures.manifest
import com.splitmate.app.guide.vm.GuideVmFixtures.pack
import com.splitmate.app.guide.vm.GuideVmFixtures.virupaksha
import com.splitmate.app.guide.vm.GuideVmFixtures.virupakshaWd
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("v2.3.4 E1: TripGuideRepository (packs, manifest edits, plan sync policy)")
class TripGuideRepositoryTest {

    private val dispatcher = StandardTestDispatcher()
    private val g = GuideTestGraph(dispatcher)
    private val repo = g.repository

    private fun gzip(text: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(text.toByteArray(Charsets.UTF_8)) }
        return bos.toByteArray()
    }

    // ---- destination ----------------------------------------------------------------------------

    @Test
    fun `resolve maps results, ambiguity, empty and failures to user-safe outcomes`() = runTest(dispatcher) {
        g.content.ambiguous = true
        assertEquals(ResolveOutcome.Candidates(listOf(hampiCandidate), ambiguous = true), repo.resolveDestination("Hampi", "Hosapete"))
        g.content.candidates = emptyList()
        assertEquals(ResolveOutcome.NoResults, repo.resolveDestination("Hampi", null))
        assertEquals(ResolveOutcome.NoResults, repo.resolveDestination("   ", null))
        g.content.resolveError = IOException("timeout contacting host")
        assertEquals(ResolveOutcome.Failure(TripGuideRepository.MSG_NETWORK, true), repo.resolveDestination("Hampi", null))
    }

    @Test
    fun `candidateForQid rejects invalid QIDs and falls back to a minimal candidate`() = runTest(dispatcher) {
        assertNull(repo.candidateForQid("hampi"))
        assertEquals(hampiCandidate, repo.candidateForQid(HAMPI_QID))
        g.content.resolveError = IOException("offline")
        val fallback = repo.candidateForQid("Q42")!!
        assertEquals("Q42", fallback.qid)
        assertNull(fallback.location)
    }

    @Test
    fun `failureMessage never leaks details and marks retryability`() {
        assertEquals(TripGuideRepository.MSG_RATE_LIMITED to true, TripGuideRepository.failureMessage(GuideContentException("x", 429)))
        assertEquals(TripGuideRepository.MSG_RATE_LIMITED to true, TripGuideRepository.failureMessage(GuideContentException("x", 503)))
        assertEquals(TripGuideRepository.MSG_NO_COORDINATES to false, TripGuideRepository.failureMessage(GuideContentException("Destination has no coordinates")))
        assertEquals(TripGuideRepository.MSG_NETWORK to true, TripGuideRepository.failureMessage(IOException("secret host 10.0.0.1")))
    }

    // ---- packs ----------------------------------------------------------------------------------

    @Test
    fun `buildPack queries nearby for centre and stay and merges by id`() = runTest(dispatcher) {
        g.nearby.results = listOf(hemakuta, virupakshaWd)
        val out = repo.buildPack(hampiCandidate, REV_1, STAY_LOC) as BuildOutcome.Built
        assertEquals(listOf(HAMPI_CENTER, STAY_LOC), g.nearby.centers)
        assertEquals(listOf(hemakuta.id, virupakshaWd.id), out.pack.nearby.map { it.id })
        assertNull(out.partialMessage)
        assertEquals(listOf<Long?>(REV_1), g.content.pinnedRevisions)
    }

    @Test
    fun `buildPack failure is a Failure outcome, not an exception`() = runTest(dispatcher) {
        g.content.buildError = GuideContentException("HTTP 503", 503)
        assertEquals(BuildOutcome.Failure(TripGuideRepository.MSG_RATE_LIMITED, true), repo.buildPack(hampiCandidate, null, null))
    }

    @Test
    fun `persist then loadCachedPack round-trips and flags soft expiry`() = runTest(dispatcher) {
        val saved = repo.persist(pack(fetchedAt = NOW - 15 * DAY))!!
        assertTrue(saved.gzipBytes > 0)
        val loaded = repo.loadCachedPack(HAMPI_QID)!!
        assertEquals(HAMPI_QID, loaded.pack.destination.qid)
        assertEquals(virupaksha.id, loaded.pack.places.first().id)
        assertTrue(loaded.softExpired)
        assertEquals(NOW - 15 * DAY, loaded.fetchedAtEpochMs)
    }

    @Test
    fun `hard-expired or corrupt packs are treated as missing`() = runTest(dispatcher) {
        repo.persist(pack(fetchedAt = NOW - 91 * DAY))
        assertNull(repo.loadCachedPack(HAMPI_QID))
        g.store.savePack("Q42", gzip("not json at all"), "hash", null, null)
        assertNull(repo.loadCachedPack("Q42"))
        assertNull(repo.loadCachedPack("Q7"))
    }

    @Test
    fun `fetchNearbyInto returns null when nothing new`() = runTest(dispatcher) {
        val p = pack(nearby = listOf(hemakuta))
        g.nearby.results = listOf(hemakuta)
        assertNull(repo.fetchNearbyInto(p, STAY_LOC))
        g.nearby.results = listOf(hemakuta, virupakshaWd)
        assertEquals(2, repo.fetchNearbyInto(p, STAY_LOC)!!.nearby.size)
    }

    // ---- manifest edits -------------------------------------------------------------------------

    @Test
    fun `setDestination is a no-op when unchanged`() = runTest(dispatcher) {
        val first = repo.setDestination(GROUP, HAMPI_QID, REV_1, MEMBER)!!
        g.now += 5_000
        val second = repo.setDestination(GROUP, HAMPI_QID, REV_1, MEMBER)!!
        assertEquals(first.updatedAtEpochMs, second.updatedAtEpochMs)
        val third = repo.setDestination(GROUP, HAMPI_QID, REV_2, MEMBER)!!
        assertEquals(REV_2, third.wikivoyageRevisionId)
    }

    @Test
    fun `togglePin pins then un-pins with an equal tombstone that Explore still shows`() = runTest(dispatcher) {
        val pinned = repo.togglePin(GROUP, virupaksha.id, MEMBER)!!
        assertTrue(virupaksha.id in PlanManifestMerger.visiblePinnedIds(pinned))

        g.now += 60_000
        val unpinned = repo.togglePin(GROUP, virupaksha.id, MEMBER)!!
        assertFalse(virupaksha.id in PlanManifestMerger.visiblePinnedIds(unpinned))
        assertEquals(unpinned.pinned[virupaksha.id], unpinned.hidden[virupaksha.id])
        assertFalse(TripGuideRepository.isHiddenForExplore(unpinned, virupaksha.id))

        val hidden = repo.hide(GROUP, virupaksha.id, MEMBER)!!
        assertTrue(hidden.hidden.getValue(virupaksha.id) > hidden.pinned.getValue(virupaksha.id))
        assertTrue(TripGuideRepository.isHiddenForExplore(hidden, virupaksha.id))
    }

    @Test
    fun `hide is strictly newer than a pin even when the clock lags`() = runTest(dispatcher) {
        g.now = NOW + 10_000
        repo.togglePin(GROUP, virupaksha.id, MEMBER)
        g.now = NOW // clock moved backwards
        val hidden = repo.hide(GROUP, virupaksha.id, MEMBER)!!
        assertTrue(hidden.hidden.getValue(virupaksha.id) > hidden.pinned.getValue(virupaksha.id))
    }

    @Test
    fun `invalid place ids and the reserved stay id are refused`() = runTest(dispatcher) {
        assertNull(repo.togglePin(GROUP, "bad id with spaces", MEMBER))
        assertNull(repo.togglePin(GROUP, "stay", MEMBER))
        assertNull(repo.hide(GROUP, "", MEMBER))
        assertNull(g.local.manifests.value[GROUP])
    }

    @Test
    fun `explore visibility rule`() {
        val m = manifest(pinned = mapOf("a" to 10L, "b" to 10L, "c" to 10L), hidden = mapOf("a" to 10L, "b" to 11L, "d" to 1L))
        assertFalse(TripGuideRepository.isHiddenForExplore(m, "a"))
        assertTrue(TripGuideRepository.isHiddenForExplore(m, "b"))
        assertFalse(TripGuideRepository.isHiddenForExplore(m, "c"))
        assertTrue(TripGuideRepository.isHiddenForExplore(m, "d"))
        assertFalse(TripGuideRepository.isHiddenForExplore(m, "e"))
    }

    // ---- sync -----------------------------------------------------------------------------------

    @Test
    fun `publishIfNeeded skips a missing or empty manifest`() = runTest(dispatcher) {
        assertFalse(repo.publishIfNeeded(GROUP))
        g.store.upsertManifest(GROUP, manifest(qid = null, rev = null))
        assertFalse(repo.publishIfNeeded(GROUP))
        assertTrue(g.transport.published.isEmpty())
    }

    @Test
    fun `publish clears pending push but never advances the poll cursor`() = runTest(dispatcher) {
        g.store.applyRemoteManifest(GROUP, manifest(), "peerMsg1")
        repo.togglePin(GROUP, virupaksha.id, MEMBER)
        assertTrue(g.local.manifests.value.getValue(GROUP).pendingPush)

        assertTrue(repo.publishIfNeeded(GROUP))
        val row = g.local.manifests.value.getValue(GROUP)
        assertFalse(row.pendingPush)
        assertEquals("peerMsg1", row.lastSyncMessageId)
        assertTrue(virupaksha.id in g.transport.published.single().pinned)
    }

    @Test
    fun `unchanged manifest is republished only after the 6 h window`() = runTest(dispatcher) {
        repo.setDestination(GROUP, HAMPI_QID, REV_1, MEMBER)
        assertTrue(repo.publishIfNeeded(GROUP))
        g.now += 60_000
        assertFalse(repo.publishIfNeeded(GROUP))
        g.now += NtfyPlanManifestSync.REPUBLISH_INTERVAL_MS
        assertTrue(repo.publishIfNeeded(GROUP))
        assertEquals(2, g.transport.published.size)
    }

    @Test
    fun `failed publish keeps the row pending`() = runTest(dispatcher) {
        repo.setDestination(GROUP, HAMPI_QID, REV_1, MEMBER)
        g.transport.publishResult = null
        assertFalse(repo.publishIfNeeded(GROUP))
        assertTrue(g.local.manifests.value.getValue(GROUP).pendingPush)
    }

    @Test
    fun `syncPlan polls from the stored cursor, merges, and does not echo an in-sync manifest`() = runTest(dispatcher) {
        g.transport.inbox = listOf(
            NtfyPlanMessage(manifest(rev = REV_1), "m1", NOW / 1000 - 60),
            NtfyPlanMessage(manifest(rev = REV_1, pinned = mapOf(virupaksha.id to NOW - 1_000)), "m2", NOW / 1000)
        )
        val merged = repo.syncPlan(GROUP, emptySet())!!
        assertEquals(listOf<String?>(null), g.transport.fetchCursors)
        assertEquals(HAMPI_QID, merged.destinationQid)
        assertTrue(virupaksha.id in merged.pinned)
        assertEquals("m2", g.local.manifests.value.getValue(GROUP).lastSyncMessageId)
        assertFalse(repo.publishIfNeeded(GROUP), "local == latest remote: nothing to echo")

        repo.syncPlan(GROUP, emptySet())
        assertEquals(listOf(null, "m2"), g.transport.fetchCursors)
    }

    @Test
    fun `local edits not yet on the topic are still published after a sync`() = runTest(dispatcher) {
        repo.togglePin(GROUP, virupaksha.id, MEMBER)
        g.transport.inbox = listOf(NtfyPlanMessage(manifest(rev = REV_1), "m1", NOW / 1000))
        repo.syncPlan(GROUP, emptySet())
        assertTrue(g.local.manifests.value.getValue(GROUP).pendingPush)
        assertTrue(repo.publishIfNeeded(GROUP))
        val published = g.transport.published.single()
        assertEquals(HAMPI_QID, published.destinationQid)
        assertTrue(virupaksha.id in published.pinned)
    }

    @Test
    fun `a pin made after a peer's destination change does not revert that destination`() = runTest(dispatcher) {
        g.now = NOW - 2 * DAY
        repo.setDestination(GROUP, GuideVmFixtures.HOSAPETE_QID, null, MEMBER)
        repo.publishIfNeeded(GROUP)
        g.now = NOW
        val pinned = repo.togglePin(GROUP, virupaksha.id, MEMBER)!!
        assertEquals(NOW - 2 * DAY, pinned.updatedAtEpochMs, "place edits keep the LWW header")

        // A peer switched the trip to Hampi a day ago (newer than our destination choice).
        g.transport.inbox = listOf(NtfyPlanMessage(manifest(rev = REV_1, updatedAt = NOW - DAY), "m9", NOW / 1000))
        val merged = repo.syncPlan(GROUP, emptySet())!!
        assertEquals(HAMPI_QID, merged.destinationQid)
        assertEquals(REV_1, merged.wikivoyageRevisionId)
        assertTrue(virupaksha.id in PlanManifestMerger.visiblePinnedIds(merged), "our pin survives the merge")
        assertTrue(repo.publishIfNeeded(GROUP))
        assertEquals(HAMPI_QID, g.transport.published.last().destinationQid)
    }

    @Test
    fun `stay text resolution failures degrade to a Rejected result`() = runTest(dispatcher) {
        val throwing = object : com.splitmate.app.data.guide.StayLocationResolver {
            override suspend fun resolve(input: String) = throw IOException("boom")
        }
        val r = TripGuideRepository(g.content, g.nearby, throwing, g.geocoder, g.store, g.transport, g.flags, clock = { g.now }, cpuDispatcher = dispatcher)
        assertEquals(
            com.splitmate.app.data.guide.StayResolution.Rejected(TripGuideRepository.MSG_STAY_FAILED),
            r.resolveStayText("https://maps.app.goo.gl/x")
        )
        assertNotNull(r.currentFlags())
    }
}
