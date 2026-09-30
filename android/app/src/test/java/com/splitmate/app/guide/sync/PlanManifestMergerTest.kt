package com.splitmate.app.guide.sync

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.PlanManifest
import com.splitmate.app.data.guide.StayPin
import com.splitmate.app.data.guide.sync.PlanManifestCodec
import com.splitmate.app.data.guide.sync.PlanManifestMerger
import com.splitmate.app.guide.sync.GuideSyncFixtures.manifest
import com.splitmate.app.guide.sync.GuideSyncFixtures.stay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.random.Random

@DisplayName("v2.3.4 WS-D: PlanManifestMerger (LWW + OR-set lattice laws)")
class PlanManifestMergerTest {

    private val organizers = setOf("org")

    // ---------------------------------------------------------------------------------------------
    // Property-style lattice laws over a small, collision-heavy domain (seeded, deterministic)
    // ---------------------------------------------------------------------------------------------

    private val ids = listOf("wv:see:a", "wv:eat:b", "wd:Q1", "wd:Q2")
    private val members = listOf(null, "m1", "m2", "org")

    private fun Random.maybe(p: Double = 0.5) = nextDouble() < p

    private fun Random.genStay(): StayPin? = if (maybe(0.6)) {
        StayPin(
            location = LatLng(listOf(15.0, 15.5)[nextInt(2)], listOf(76.0, 76.5)[nextInt(2)]),
            label = listOf("A", "B")[nextInt(2)],
            precision = CoordinatePrecision.values()[nextInt(2)],
            setByMemberKey = members[nextInt(members.size)],
            setAtEpochMs = nextLong(0, 6)
        )
    } else null

    private fun Random.genManifest(): PlanManifest {
        val pinned = ids.filter { maybe() }.associateWith { nextLong(0, 6) }
        val hidden = (ids + PlanManifestCodec.STAY_ID).filter { maybe(0.35) }.associateWith { nextLong(0, 6) }
        val days = ids.filter { maybe(0.4) }.associateWith { nextInt(1, 4) }
        return PlanManifest(
            version = 1,
            destinationQid = listOf(null, "Q1", "Q2")[nextInt(3)],
            wikivoyageRevisionId = listOf(null, 1L, 2L)[nextInt(3)],
            stay = genStay(),
            pinned = pinned,
            hidden = hidden,
            days = days,
            updatedAtEpochMs = nextLong(0, 4),
            updatedByMemberKey = members[nextInt(members.size)]
        )
    }

    private fun merge(a: PlanManifest, b: PlanManifest) = PlanManifestMerger.merge(a, b, organizers)

    @Test
    fun `merge is commutative`() {
        val rnd = Random(20260930)
        repeat(2_000) {
            val a = PlanManifestMerger.normalize(rnd.genManifest())
            val b = PlanManifestMerger.normalize(rnd.genManifest())
            assertEquals(merge(a, b), merge(b, a), "a=$a\nb=$b")
        }
    }

    @Test
    fun `merge is associative`() {
        val rnd = Random(1234)
        repeat(2_000) {
            val a = PlanManifestMerger.normalize(rnd.genManifest())
            val b = PlanManifestMerger.normalize(rnd.genManifest())
            val c = PlanManifestMerger.normalize(rnd.genManifest())
            assertEquals(merge(merge(a, b), c), merge(a, merge(b, c)), "a=$a\nb=$b\nc=$c")
        }
    }

    @Test
    fun `merge is idempotent`() {
        val rnd = Random(99)
        repeat(2_000) {
            val raw = rnd.genManifest()
            val a = PlanManifestMerger.normalize(raw)
            assertEquals(a, merge(a, a))
            assertEquals(a, PlanManifestMerger.normalize(a), "normalize must be idempotent")
            val b = PlanManifestMerger.normalize(rnd.genManifest())
            val ab = merge(a, b)
            assertEquals(ab, merge(ab, b), "absorbing an already-merged input changes nothing")
            assertEquals(ab, merge(ab, a))
        }
    }

    @Test
    fun `mergeAll is order independent`() {
        val rnd = Random(7)
        repeat(300) {
            val list = List(5) { rnd.genManifest() }
            val expected = PlanManifestMerger.mergeAll(list, organizers)
            assertEquals(expected, PlanManifestMerger.mergeAll(list.reversed(), organizers))
            assertEquals(expected, PlanManifestMerger.mergeAll(list.shuffled(Random(it)), organizers))
        }
        assertNull(PlanManifestMerger.mergeAll(emptyList(), organizers))
    }

    @Test
    fun `merged manifests survive the wire codec`() {
        val rnd = Random(5)
        repeat(300) {
            val m = merge(PlanManifestMerger.normalize(rnd.genManifest()), PlanManifestMerger.normalize(rnd.genManifest()))
            val wire = PlanManifestCodec.encode(m, shareStay = true).json
            assertEquals(m, PlanManifestCodec.decode(wire))
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Field rules
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `destination and revision are last-writer-wins by updatedAt`() {
        val older = manifest(destQid = "Q1", wvRev = 1, updatedAt = 100, by = "m1")
        val newer = manifest(destQid = "Q2", wvRev = 2, updatedAt = 200, by = "m2")
        val m = merge(older, newer)
        assertEquals("Q2", m.destinationQid)
        assertEquals(2L, m.wikivoyageRevisionId)
        assertEquals(200L, m.updatedAtEpochMs)
        assertEquals("m2", m.updatedByMemberKey)
    }

    @Test
    fun `organizer wins updatedAt ties`() {
        val member = manifest(destQid = "Q9", updatedAt = 100, by = "zz-member")
        val organizer = manifest(destQid = "Q1", updatedAt = 100, by = "org")
        assertEquals("Q1", merge(member, organizer).destinationQid)
        assertEquals("Q1", merge(organizer, member).destinationQid)
        // Without the organizer set, the tie falls back to the deterministic key order.
        assertEquals("Q9", PlanManifestMerger.merge(member, organizer, emptySet()).destinationQid)
    }

    @Test
    fun `pins and hides are an OR-set with timestamps`() {
        val a = manifest(pinned = mapOf("wv:see:a" to 10L), hidden = mapOf("wd:Q1" to 5L))
        val b = manifest(pinned = mapOf("wd:Q1" to 3L, "wv:see:a" to 7L), hidden = mapOf("wv:see:a" to 12L))
        val m = merge(a, b)
        assertEquals(mapOf("wv:see:a" to 10L, "wd:Q1" to 3L), m.pinned)
        assertEquals(mapOf("wd:Q1" to 5L, "wv:see:a" to 12L), m.hidden)
        assertTrue(PlanManifestMerger.visiblePinnedIds(m).isEmpty(), "both hidden after their pin")
        assertTrue(PlanManifestMerger.isHidden(m, "wv:see:a"))

        // Re-pin after hide makes it visible again, and survives merging with the stale peer.
        val repinned = PlanManifestMerger.pin(m, "wv:see:a", "m1", now = 20)
        assertEquals(setOf("wv:see:a"), PlanManifestMerger.visiblePinnedIds(merge(repinned, b)))
    }

    @Test
    fun `equal pin and hide timestamps mean hidden`() {
        val m = manifest(pinned = mapOf("wd:Q1" to 5L), hidden = mapOf("wd:Q1" to 5L))
        assertFalse(PlanManifestMerger.visiblePinnedIds(m).contains("wd:Q1"))
        val hidden = PlanManifestMerger.hide(manifest(pinned = mapOf("wd:Q1" to 50L)), "wd:Q1", "m1", now = 10)
        assertTrue(PlanManifestMerger.isHidden(hidden, "wd:Q1"), "hide must not lose to a pin from the future")
    }

    @Test
    fun `days are last-writer-wins per key using the pin timestamp`() {
        val a = manifest(pinned = mapOf("wv:see:a" to 10L), days = mapOf("wv:see:a" to 1))
        val b = manifest(pinned = mapOf("wv:see:a" to 20L), days = mapOf("wv:see:a" to 2))
        assertEquals(mapOf("wv:see:a" to 2), merge(a, b).days)
        // Newer re-pin without a day clears the older day assignment.
        val c = manifest(pinned = mapOf("wv:see:a" to 30L))
        assertTrue(merge(merge(a, b), c).days.isEmpty())
        // Same pin time: deterministic max.
        val d = manifest(pinned = mapOf("wv:see:a" to 20L), days = mapOf("wv:see:a" to 3))
        assertEquals(mapOf("wv:see:a" to 3), merge(b, d).days)
    }

    @Test
    fun `stay is last-writer-wins with organizer tie-break`() {
        val s1 = stay(lat = 15.0, by = "m1", at = 100)
        val s2 = stay(lat = 16.0, by = "m2", at = 200)
        assertEquals(s2, merge(manifest(stay = s1), manifest(stay = s2)).stay)
        val sOrg = stay(lat = 10.0, by = "org", at = 100)
        assertEquals(sOrg, merge(manifest(stay = s1), manifest(stay = sOrg)).stay)
        assertEquals(sOrg, merge(manifest(stay = sOrg), manifest(stay = s1)).stay)
    }

    @Test
    fun `only the setter or an organizer may clear the stay`() {
        val shared = manifest(stay = stay(by = "m1", at = 100))
        assertNull(PlanManifestMerger.clearStay(shared, "m2", organizers, now = 200), "other member cannot clear")
        assertNull(PlanManifestMerger.clearStay(shared, null, organizers, now = 200))

        val bySetter = PlanManifestMerger.clearStay(shared, "m1", organizers, now = 200)
        assertNotNull(bySetter)
        assertNull(bySetter!!.stay)
        assertEquals(200L, bySetter.hidden[PlanManifestCodec.STAY_ID])

        val byOrganizer = PlanManifestMerger.clearStay(shared, "org", organizers, now = 200)
        assertNotNull(byOrganizer)
        assertNull(byOrganizer!!.stay)
    }

    @Test
    fun `stay tombstone prevents resurrection from a stale peer, newer stay wins again`() {
        val stale = manifest(stay = stay(by = "m1", at = 100), updatedAt = 100)
        val cleared = PlanManifestMerger.clearStay(stale, "m1", organizers, now = 200)!!
        assertNull(merge(cleared, stale).stay)
        assertNull(merge(stale, cleared).stay)

        val newer = PlanManifestMerger.setStay(stale, stay(lat = 20.0, by = "m1", at = 50), "m1", now = 300)
        val m = merge(cleared, newer)
        assertNotNull(m.stay)
        assertEquals(20.0, m.stay!!.location.lat)
        assertTrue(m.stay!!.setAtEpochMs > m.hidden[PlanManifestCodec.STAY_ID]!!)
    }

    @Test
    fun `local mutations advance updatedAt monotonically`() {
        val m = manifest(updatedAt = 1_000)
        val pinned = PlanManifestMerger.pin(m, "wv:see:a", "m1", now = 10) // device clock behind
        assertTrue(pinned.updatedAtEpochMs > m.updatedAtEpochMs)
        val dest = PlanManifestMerger.setDestination(pinned, "Q42", 7, "m1", now = 5)
        assertTrue(dest.updatedAtEpochMs > pinned.updatedAtEpochMs)
        assertEquals("Q42", merge(m, dest).destinationQid)
    }
}
