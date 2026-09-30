package com.splitmate.app.guide.loop

import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.data.guide.loop.LoopStopSelector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("LoopStopSelector: pinned first, else top-ranked <= 8")
class LoopStopSelectorTest {

    private val stay = LatLng(15.335, 76.46)

    private fun p(
        id: String,
        kind: PlaceKind,
        dist: Double?,
        loc: LatLng? = LatLng(15.335 + (dist ?: 1.0) / 111.0, 76.46),
        source: PlaceSource = PlaceSource.WIKIVOYAGE,
        qid: String? = null
    ) = Place(id = id, kind = kind, name = id, location = loc, qid = qid, source = source, distanceKmFromCenter = dist)

    @Test
    fun `pinned stops win and are the only stops`() {
        val c = listOf(p("a", PlaceKind.SEE, 1.0), p("b", PlaceKind.EAT, 2.0), p("c", PlaceKind.SEE, 0.5))
        val s = LoopStopSelector.select(c, pinned = mapOf("b" to 10L, "a" to 11L), hidden = emptyMap())
        assertFalse(s.autoSuggested)
        assertEquals(setOf("a", "b"), s.stops.map { it.id }.toSet())
    }

    @Test
    fun `a later hide overrides an earlier pin (OR-set semantics)`() {
        val c = listOf(p("a", PlaceKind.SEE, 1.0), p("b", PlaceKind.SEE, 2.0))
        assertTrue(LoopStopSelector.isPinned("a", mapOf("a" to 5L), mapOf("a" to 4L)))
        assertFalse(LoopStopSelector.isPinned("a", mapOf("a" to 5L), mapOf("a" to 5L)))
        assertTrue(LoopStopSelector.isHidden("a", mapOf("a" to 5L), mapOf("a" to 5L)))
        val s = LoopStopSelector.select(c, pinned = mapOf("a" to 1L), hidden = mapOf("a" to 2L))
        assertTrue(s.autoSuggested, "no effective pins => auto mode")
        assertEquals(listOf("b"), s.stops.map { it.id }, "hidden place excluded from auto-suggest")
    }

    @Test
    fun `pinned places without coordinates are reported, not routed`() {
        val c = listOf(p("a", PlaceKind.SEE, 1.0), p("nocoord", PlaceKind.SEE, null, loc = null))
        val s = LoopStopSelector.select(c, pinned = mapOf("a" to 1L, "nocoord" to 1L), hidden = emptyMap())
        assertEquals(listOf("a"), s.stops.map { it.id })
        assertEquals(listOf("nocoord"), s.pinnedWithoutCoordinates.map { it.id })
    }

    @Test
    fun `auto-suggest ranks SEE then DO then EAT, then distance, capped at 8`() {
        val c = listOf(
            p("eat1", PlaceKind.EAT, 0.1),
            p("do1", PlaceKind.DO, 0.2),
            p("see-far", PlaceKind.SEE, 9.0),
            p("see-near", PlaceKind.SEE, 0.3),
            p("sleep", PlaceKind.SLEEP, 0.05),
            p("nocoord", PlaceKind.SEE, 0.01, loc = null)
        ) + (1..10).map { p("see%02d".format(it), PlaceKind.SEE, 1.0 + it) }
        val s = LoopStopSelector.select(c, emptyMap(), emptyMap())
        assertTrue(s.autoSuggested)
        assertEquals(LoopStopSelector.AUTO_SUGGEST_LIMIT, s.stops.size)
        assertEquals("see-near", s.stops.first().id)
        assertTrue(s.stops.all { it.kind == PlaceKind.SEE }, "12 SEE candidates fill all 8 slots")
        assertTrue(s.stops.none { it.id == "sleep" || it.id == "nocoord" })
    }

    @Test
    fun `kind priority applies before distance when slots remain`() {
        val c = listOf(p("eat1", PlaceKind.EAT, 0.1), p("do1", PlaceKind.DO, 0.2), p("see1", PlaceKind.SEE, 5.0))
        val s = LoopStopSelector.select(c, emptyMap(), emptyMap())
        assertEquals(listOf("see1", "do1", "eat1"), s.stops.map { it.id })
    }

    @Test
    fun `distance from the stay is used when provided`() {
        val near = p("z-near", PlaceKind.SEE, dist = 50.0, loc = LatLng(15.336, 76.461))
        val far = p("a-far", PlaceKind.SEE, dist = 0.1, loc = LatLng(15.60, 76.90))
        val s = LoopStopSelector.select(listOf(far, near), emptyMap(), emptyMap(), stay = stay)
        assertEquals(listOf("z-near", "a-far"), s.stops.map { it.id })
    }

    @Test
    fun `wikidata duplicates of wikivoyage listings are collapsed by QID`() {
        val wv = p("wv:see:virupaksha", PlaceKind.SEE, 0.1, qid = "Q1050711", source = PlaceSource.WIKIVOYAGE)
        val wd = p("wd:Q1050711", PlaceKind.SEE, 0.1, qid = "Q1050711", source = PlaceSource.WIKIDATA)
        val s = LoopStopSelector.select(listOf(wd, wv), emptyMap(), emptyMap())
        assertEquals(listOf("wv:see:virupaksha"), s.stops.map { it.id })
    }

    @Test
    fun `a pin on the wikidata duplicate is honoured`() {
        val wv = p("wv:see:virupaksha", PlaceKind.SEE, 0.1, qid = "Q1050711", source = PlaceSource.WIKIVOYAGE)
        val wd = p("wd:Q1050711", PlaceKind.SEE, 0.1, qid = "Q1050711", source = PlaceSource.WIKIDATA)
        val s = LoopStopSelector.select(listOf(wd, wv), mapOf("wd:Q1050711" to 1L), emptyMap())
        assertEquals(listOf("wd:Q1050711"), s.stops.map { it.id })
    }

    @Test
    fun `more than 60 pins are truncated and flagged`() {
        val c = (0 until 70).map { p("p%02d".format(it), PlaceKind.SEE, it.toDouble() + 0.1) }
        val s = LoopStopSelector.select(c, c.associate { it.id to 1L }, emptyMap())
        assertEquals(LoopStopSelector.MAX_LOOP_STOPS, s.stops.size)
        assertTrue(s.truncated)
    }

    @Test
    fun `selection is independent of candidate order`() {
        val c = (0 until 20).map { p("id%02d".format(it), if (it % 3 == 0) PlaceKind.DO else PlaceKind.SEE, (it * 7 % 11).toDouble()) }
        val a = LoopStopSelector.select(c, emptyMap(), emptyMap()).stops.map { it.id }
        val b = LoopStopSelector.select(c.reversed(), emptyMap(), emptyMap()).stops.map { it.id }
        assertEquals(a, b)
    }

    @Test
    fun `empty candidates yield an empty auto selection`() {
        val s = LoopStopSelector.select(emptyList(), emptyMap(), emptyMap())
        assertTrue(s.stops.isEmpty())
        assertTrue(s.autoSuggested)
    }
}
