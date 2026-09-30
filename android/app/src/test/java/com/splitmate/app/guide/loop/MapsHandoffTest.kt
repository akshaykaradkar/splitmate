package com.splitmate.app.guide.loop

import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.LoopRoute
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.data.guide.loop.DeterministicLoopOptimizer
import com.splitmate.app.data.guide.nav.MapsHandoff
import com.splitmate.app.data.guide.nav.TravelMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URLDecoder
import java.util.Locale

@DisplayName("MapsHandoff: pure URI builders")
class MapsHandoffTest {

    private val stay = LatLng(15.335, 76.46)
    private val temple = LatLng(15.3350123, 76.4600456)

    private fun stops(n: Int): List<LatLng> = (1..n).map { LatLng(15.30 + it * 0.001, 76.40 + it * 0.001) }

    private fun params(url: String): Map<String, String> =
        url.substringAfter('?').split('&').associate {
            it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), "UTF-8")
        }

    @Test
    fun `geo uri with encoded label`() {
        assertEquals(
            "geo:0,0?q=15.335,76.46(Hotel%20Mayura%20%28Kamalapur%29%20%26%20Spa)",
            MapsHandoff.geoUri(stay, "Hotel Mayura (Kamalapur) & Spa")
        )
    }

    @Test
    fun `geo uri without label and with blank or unsafe label`() {
        assertEquals("geo:0,0?q=15.335,76.46", MapsHandoff.geoUri(stay, null))
        assertEquals("geo:0,0?q=15.335,76.46", MapsHandoff.geoUri(stay, "  \n "))
        assertEquals("geo:0,0?q=15.335,76.46(A%20B)", MapsHandoff.geoUri(stay, "A\u202E\nB"))
    }

    @Test
    fun `geo uri encodes non-ascii labels as utf-8`() {
        assertEquals("geo:0,0?q=15.335,76.46(%E0%A4%B9%E0%A4%82%E0%A4%AA%E0%A5%80)", MapsHandoff.geoUri(stay, "हंपी"))
    }

    @Test
    fun `navigation uri walking and driving`() {
        assertEquals("google.navigation:q=15.335012,76.460046&mode=w", MapsHandoff.navigationUri(temple))
        assertEquals("google.navigation:q=15.335012,76.460046&mode=d", MapsHandoff.navigationUri(temple, TravelMode.DRIVING))
    }

    @Test
    fun `universal directions url`() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&origin=15.335%2C76.46&destination=15.335012%2C76.460046&travelmode=walking",
            MapsHandoff.directionsUrl(temple, origin = stay, mode = TravelMode.WALKING)
        )
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=15.335%2C76.46",
            MapsHandoff.directionsUrl(stay)
        )
    }

    @Test
    fun `search url browser fallback and copyable coordinates`() {
        assertEquals("https://www.google.com/maps/search/?api=1&query=15.335%2C76.46", MapsHandoff.searchUrl(stay))
        assertEquals("15.335, 76.46", MapsHandoff.copyableCoordinates(stay))
        assertEquals("-33.856784, 151.215297", MapsHandoff.copyableCoordinates(LatLng(-33.8567844, 151.2152967)))
    }

    @Test
    fun `coordinate formatting is locale independent and never uses exponents`() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("geo:0,0?q=15.335,76.46", MapsHandoff.geoUri(stay, null))
            assertEquals("0.0000001".let { MapsHandoff.formatDegrees(it.toDouble()) }, "0")
            assertEquals("0.000001", MapsHandoff.formatDegrees(1e-6))
            assertEquals("15", MapsHandoff.formatDegrees(15.0))
            assertEquals("15.123457", MapsHandoff.formatDegrees(15.1234567))
            assertEquals("0", MapsHandoff.formatDegrees(-0.0000001))
        } finally {
            Locale.setDefault(saved)
        }
    }

    // ------------------------------------------------------------------ Loop chunking

    @Test
    fun `no stops produce no loop urls`() {
        assertTrue(MapsHandoff.loopDirectionsUrls(stay, emptyList()).isEmpty())
    }

    @Test
    fun `up to 9 stops fit in one url that starts and ends at the stay`() {
        for (n in listOf(1, 5, 9)) {
            val urls = MapsHandoff.loopDirectionsUrls(stay, stops(n), TravelMode.DRIVING)
            assertEquals(1, urls.size, "n=$n")
            val p = params(urls.single())
            assertEquals("15.335,76.46", p["origin"])
            assertEquals("15.335,76.46", p["destination"])
            assertEquals(n, p["waypoints"]!!.split('|').size)
            assertEquals("driving", p["travelmode"])
            assertTrue(urls.single().contains("%7C") || n == 1, "pipes must be encoded")
        }
    }

    @Test
    fun `more than 9 stops are chunked into continuous segments`() {
        for (n in listOf(10, 18, 19, 20, 27, 60)) {
            val pts = stops(n)
            val urls = MapsHandoff.loopDirectionsUrls(stay, pts)
            val segs = urls.map { params(it) }
            // Continuity: each segment starts where the previous one ended.
            assertEquals("15.335,76.46", segs.first()["origin"])
            assertEquals("15.335,76.46", segs.last()["destination"])
            for (i in 1 until segs.size) assertEquals(segs[i - 1]["destination"], segs[i]["origin"], "n=$n seg=$i")
            // Cap and coverage: every stop visited once, in order.
            val visited = mutableListOf<String>()
            for (s in segs) {
                val wps = s["waypoints"]?.split('|') ?: emptyList()
                assertTrue(wps.size <= MapsHandoff.MAX_WAYPOINTS_PER_URL, "n=$n has ${wps.size} waypoints")
                visited += wps
                visited += s["destination"]!!
            }
            visited.removeAt(visited.lastIndex) // the final stay
            assertEquals(pts.map { MapsHandoff.coord(it) }, visited, "n=$n")
            assertFalse(urls.any { it.contains(" ") })
        }
    }

    @Test
    fun `chunk count follows ceil((n + 1) divided by 10)`() {
        assertEquals(2, MapsHandoff.loopDirectionsUrls(stay, stops(10)).size)
        assertEquals(2, MapsHandoff.loopDirectionsUrls(stay, stops(19)).size)
        assertEquals(3, MapsHandoff.loopDirectionsUrls(stay, stops(20)).size)
    }

    @Test
    fun `smaller waypoint cap (mobile) is honoured`() {
        val urls = MapsHandoff.loopDirectionsUrls(stay, stops(7), maxWaypoints = 3)
        assertEquals(2, urls.size)
        assertTrue(urls.all { (params(it)["waypoints"]?.split('|')?.size ?: 0) <= 3 })
    }

    @Test
    fun `loop urls from an optimised LoopRoute`() {
        val places = stops(12).mapIndexed { i, ll ->
            Place(id = "s$i", kind = PlaceKind.SEE, name = "Stop $i", location = ll, source = PlaceSource.WIKIDATA)
        }
        val route = DeterministicLoopOptimizer().optimize(stay, places)
        val urls = MapsHandoff.loopDirectionsUrls(route, TravelMode.WALKING)
        assertEquals(2, urls.size)
        assertTrue(urls.all { it.endsWith("&travelmode=walking") })
    }

    // ------------------------------------------------------------------ Share text

    @Test
    fun `whatsapp loop share text is readable, estimated and json-free`() {
        val a = Place(id = "a", kind = PlaceKind.SEE, name = "Virupaksha Temple", location = LatLng(15.3350, 76.4600), source = PlaceSource.WIKIVOYAGE)
        val b = Place(id = "b", kind = PlaceKind.SEE, name = "Vittala Temple", location = LatLng(15.3430, 76.4750), source = PlaceSource.WIKIVOYAGE)
        val route = LoopRoute(
            start = LatLng(15.3180, 76.4705),
            orderedStops = listOf(a, b),
            legStraightKm = listOf(1.94, 1.87, 3.0),
            totalStraightKm = 6.81,
            totalRoadEstimateKm = 6.81 * LoopRoute.ROAD_FACTOR
        )
        val text = MapsHandoff.loopShareText(route, "Hotel Mayura\u0000")
        assertTrue(text.startsWith("*Day loop from Hotel Mayura* (2 stops)"), text)
        assertTrue(text.contains("1. Virupaksha Temple (1.9 km)"), text)
        assertTrue(text.contains("2. Vittala Temple (1.9 km)"), text)
        assertTrue(text.contains("Back to Hotel Mayura (3.0 km)"), text)
        assertTrue(text.contains("Total ≈ 9.2 km by road (6.8 km straight-line estimate)"), text)
        assertTrue(text.contains("https://www.google.com/maps/dir/?api=1&origin="), text)
        assertFalse(text.contains("{") || text.contains("}"), "no raw JSON")
        assertFalse(text.contains("\u0000"))
    }

    @Test
    fun `share text lists parts when the loop is chunked`() {
        val places = stops(12).mapIndexed { i, ll ->
            Place(id = "s%02d".format(i), kind = PlaceKind.SEE, name = "Stop $i", location = ll, source = PlaceSource.WIKIDATA)
        }
        val route = DeterministicLoopOptimizer().optimize(stay, places)
        val text = MapsHandoff.loopShareText(route, null)
        assertTrue(text.contains("Part 1: https://"), text)
        assertTrue(text.contains("Part 2: https://"), text)
        assertTrue(text.contains("from our stay"), text)
    }

    @Test
    fun `place share text`() {
        assertEquals(
            "*Hampi Bazaar*\n15.3365, 76.461\nhttps://www.google.com/maps/search/?api=1&query=15.3365%2C76.461",
            MapsHandoff.placeShareText("Hampi Bazaar", LatLng(15.3365, 76.461))
        )
    }
}
