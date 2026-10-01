package com.splitmate.app.guide.vm

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.LoopRoute
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.StayPin
import com.splitmate.app.data.guide.StoredPack
import com.splitmate.app.data.guide.loop.DeterministicLoopOptimizer
import com.splitmate.app.guide.vm.GuideVmFixtures.NOW
import com.splitmate.app.guide.vm.GuideVmFixtures.STAY_LOC
import com.splitmate.app.guide.vm.GuideVmFixtures.coracle
import com.splitmate.app.guide.vm.GuideVmFixtures.curated
import com.splitmate.app.guide.vm.GuideVmFixtures.hemakuta
import com.splitmate.app.guide.vm.GuideVmFixtures.lotusMahal
import com.splitmate.app.guide.vm.GuideVmFixtures.manifest
import com.splitmate.app.guide.vm.GuideVmFixtures.mango
import com.splitmate.app.guide.vm.GuideVmFixtures.noCoords
import com.splitmate.app.guide.vm.GuideVmFixtures.pack
import com.splitmate.app.guide.vm.GuideVmFixtures.place
import com.splitmate.app.guide.vm.GuideVmFixtures.virupaksha
import com.splitmate.app.guide.vm.GuideVmFixtures.virupakshaWd
import com.splitmate.app.guide.vm.GuideVmFixtures.vittala
import com.splitmate.app.ui.screens.plan.TripGuidePresenter
import com.splitmate.app.ui.screens.plan.TripGuideViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.ZoneOffset

@DisplayName("v2.3.4 E1: TripGuidePresenter (domain -> UI models)")
class TripGuidePresenterTest {

    @Test
    fun `sections are ordered, skip SLEEP and empty kinds, and drop hidden places`() {
        val m = manifest(pinned = mapOf(vittala.id to 10L), hidden = mapOf(lotusMahal.id to 5L))
        val sections = TripGuidePresenter.buildSections(pack(), m, GuideVmFixtures.HAMPI_CENTER)
        assertEquals(listOf("Must See", "Things to Do", "Local Eats"), sections.map { it.title })
        assertEquals(listOf(PlaceKind.SEE, PlaceKind.DO, PlaceKind.EAT), sections.map { it.kind })
        val see = sections.first().places
        assertEquals(listOf(virupaksha.id, vittala.id), see.map { it.place.id })
        assertTrue(see.single { it.place.id == vittala.id }.isPinned)
        assertFalse(see.single { it.place.id == virupaksha.id }.isPinned)
        assertTrue(sections.all { it.initiallyVisible == TripGuidePresenter.INITIALLY_VISIBLE })
        assertTrue(sections.flatMap { it.places }.none { it.place.kind == PlaceKind.SLEEP })

        val onlySee = TripGuidePresenter.buildSections(pack(places = listOf(virupaksha)), manifest(), null)
        assertEquals(listOf("Must See"), onlySee.map { it.title })
        assertNull(onlySee.single().places.single().distanceLabel)
    }

    @Test
    fun `nearby Wikidata duplicates of curated listings are merged away, others labelled`() {
        val p = pack(places = listOf(virupaksha), nearby = listOf(virupakshaWd, hemakuta))
        val cards = TripGuidePresenter.buildSections(p, manifest(), null).single().places
        assertEquals(listOf(virupaksha.id, hemakuta.id), cards.map { it.place.id })
        assertNull(cards[0].sourceLabel)
        assertEquals(TripGuidePresenter.SOURCE_WIKIDATA, cards[1].sourceLabel)
    }

    @Test
    fun `distance label is a road estimate`() {
        assertNull(TripGuidePresenter.distanceLabel(null, STAY_LOC))
        assertNull(TripGuidePresenter.distanceLabel(STAY_LOC, null))
        assertEquals("0.0 km ≈", TripGuidePresenter.distanceLabel(STAY_LOC, STAY_LOC))
        assertEquals("1.5 km ≈", TripGuidePresenter.distanceLabel(LatLng(15.0, 76.0), LatLng(15.01, 76.0)))
        assertEquals("150 km ≈", TripGuidePresenter.distanceLabel(LatLng(15.0, 76.0), LatLng(16.0, 76.0)))
    }

    @Test
    fun `excerpt heuristic`() {
        assertFalse(TripGuidePresenter.isExcerpt(noCoords))
        assertFalse(TripGuidePresenter.isExcerpt(virupaksha))
        assertTrue(TripGuidePresenter.isExcerpt(virupaksha.copy(blurb = "a".repeat(280))))
        assertTrue(TripGuidePresenter.isExcerpt(virupaksha.copy(blurb = "Built in the 14th century…")))
    }

    @Test
    fun `loop is deterministic for any input order and needs a stay`() {
        val optimizer = DeterministicLoopOptimizer()
        val a = TripGuidePresenter.buildLoop(pack(), manifest(), STAY_LOC, optimizer)
        val b = TripGuidePresenter.buildLoop(pack(places = curated.reversed()), manifest(), STAY_LOC, optimizer)
        val idsA = a.route!!.orderedStops.map { it.id }
        assertEquals(idsA, b.route!!.orderedStops.map { it.id })
        assertEquals(setOf(virupaksha.id, vittala.id, lotusMahal.id, coracle.id, mango.id), idsA.toSet())
        assertTrue(a.autoSuggested)
        assertEquals(TripGuidePresenter.LOOP_CAVEATS, a.caveats)
        assertEquals("5 stops · ≈ " + String.format(java.util.Locale.ROOT, "%.1f km", a.route!!.totalRoadEstimateKm), a.totalLabel)

        val noStay = TripGuidePresenter.buildLoop(pack(), manifest(), null, optimizer)
        assertNull(noStay.route)
        assertNull(noStay.totalLabel)
        assertTrue(noStay.caveats.isEmpty())
    }

    @Test
    fun `pinned stops drive the loop and pinned places without coordinates are reported`() {
        val m = manifest(pinned = mapOf(vittala.id to 10L, noCoords.id to 10L))
        val loop = TripGuidePresenter.buildLoop(pack(), m, STAY_LOC, DeterministicLoopOptimizer())
        assertFalse(loop.autoSuggested)
        assertEquals(listOf(vittala.id), loop.route!!.orderedStops.map { it.id })
        assertEquals(listOf(noCoords.id), loop.stopsWithoutCoordinates.map { it.id })
        assertEquals("1 stop · ≈ " + String.format(java.util.Locale.ROOT, "%.1f km", loop.route!!.totalRoadEstimateKm), loop.totalLabel)
    }

    @Test
    fun `total label formats stops and road km`() {
        val route = LoopRoute(STAY_LOC, listOf(virupaksha, vittala), listOf(1.0, 2.0, 3.0), 10.5, 14.2)
        assertEquals("2 stops · ≈ 14.2 km", TripGuidePresenter.totalLabel(route))
    }

    @Test
    fun `offline size estimate and saved-on label`() {
        val small = StoredPack(pack(hero = true), gzipBytes = 10_000, fetchedAtEpochMs = NOW, softExpired = false)
        assertEquals("0.1 MB", TripGuidePresenter.sizeLabel(small))
        val big = StoredPack(pack(hero = true), gzipBytes = 1_000_000, fetchedAtEpochMs = NOW, softExpired = false)
        assertEquals("1.0 MB", TripGuidePresenter.sizeLabel(big))
        assertEquals("Offline · guide saved on 29 Sep", TripGuidePresenter.offlineLabel(NOW, ZoneOffset.UTC))
    }

    @Test
    fun `Wikidata attribution is added only when Wikidata places are present`() {
        assertEquals(pack().attribution, TripGuidePresenter.attribution(pack()))
        val withWd = pack(nearby = listOf(hemakuta))
        assertEquals(pack().attribution + TripGuidePresenter.WIKIDATA_NEARBY_ATTRIBUTION, TripGuidePresenter.attribution(withWd))
        assertEquals(listOf(GuideVmFixtures.mayura.id), TripGuidePresenter.sleepListings(pack()).map { it.id })
    }

    @Test
    fun `effective stay is the newer of local and shared, local wins ties`() {
        fun stay(at: Long, label: String) = StayPin(STAY_LOC, label, CoordinatePrecision.EXACT, null, at)
        val local = stay(100, "local")
        val shared = stay(200, "shared")
        assertSame(shared, TripGuideViewModel.effectiveOf(local, shared))
        assertSame(local, TripGuideViewModel.effectiveOf(local, stay(100, "tie")))
        assertSame(local, TripGuideViewModel.effectiveOf(local, null))
        assertSame(shared, TripGuideViewModel.effectiveOf(null, shared))
        assertNull(TripGuideViewModel.effectiveOf(null, null))
        assertEquals("x", place("wv:see:x", PlaceKind.SEE, "x", null).name)
    }
}
