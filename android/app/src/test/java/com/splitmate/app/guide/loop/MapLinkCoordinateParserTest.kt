package com.splitmate.app.guide.loop

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.loop.CoordinatePattern
import com.splitmate.app.data.guide.loop.MapLinkCoordinateParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

@DisplayName("MapLinkCoordinateParser: offline precision ladder")
class MapLinkCoordinateParserTest {

    private data class Case(val input: String, val pattern: String, val lat: Double?, val lng: Double?)

    private fun cases(): List<Case> = LoopFixtures.text("map_links.tsv")
        .lines()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .map { line ->
            val cols = line.split('\t')
            require(cols.size >= 2) { "Bad fixture line: $line" }
            Case(cols[0], cols[1], cols.getOrNull(2)?.toDouble(), cols.getOrNull(3)?.toDouble())
        }

    @Test
    fun `fixture file has at least 40 link cases`() {
        assertTrue(cases().size >= 40, "expected >= 40 fixtures, got ${cases().size}")
    }

    @TestFactory
    fun `every fixture resolves to the expected rung and coordinate`(): List<DynamicTest> =
        cases().map { c ->
            DynamicTest.dynamicTest("${c.pattern}: ${c.input.take(90)}") {
                val parsed = MapLinkCoordinateParser.extractCoordinate(c.input)
                if (c.pattern == "NONE") {
                    assertNull(parsed, "expected no coordinate for ${c.input}")
                } else {
                    assertNotNull(parsed, "expected a coordinate for ${c.input}")
                    val expected = CoordinatePattern.valueOf(c.pattern)
                    assertEquals(expected, parsed!!.pattern)
                    assertEquals(expected.precision, parsed.precision)
                    assertEquals(c.lat!!, parsed.location.lat, 1e-5)
                    assertEquals(c.lng!!, parsed.location.lng, 1e-5)
                }
            }
        }

    @Test
    fun `last !3d pair wins over earlier pairs and viewport`() {
        val url = "https://www.google.com/maps/place/A/@10.0,70.0,12z/data=!3d11.11!4d71.11!3d12.345!4d77.654"
        val p = MapLinkCoordinateParser.extractCoordinate(url)!!
        assertEquals(12.345, p.location.lat, 1e-9)
        assertEquals(77.654, p.location.lng, 1e-9)
        assertEquals(CoordinatePrecision.EXACT, p.precision)
    }

    @Test
    fun `viewport only is approximate`() {
        val p = MapLinkCoordinateParser.extractCoordinate("https://www.google.com/maps/@15.335,76.46,15z")!!
        assertEquals(CoordinatePrecision.APPROXIMATE, p.precision)
    }

    @Test
    fun `validate rejects out of range, non-finite and null island`() {
        assertNull(MapLinkCoordinateParser.validate(0.0, 0.0))
        assertNull(MapLinkCoordinateParser.validate(90.0001, 10.0))
        assertNull(MapLinkCoordinateParser.validate(10.0, -180.0001))
        assertNull(MapLinkCoordinateParser.validate(Double.NaN, 10.0))
        assertNull(MapLinkCoordinateParser.validate(10.0, Double.POSITIVE_INFINITY))
        assertNotNull(MapLinkCoordinateParser.validate(90.0, 180.0))
        assertNotNull(MapLinkCoordinateParser.validate(0.0, 1.0))
    }

    @Test
    fun `place name hint from place path is decoded and cleaned`() {
        val r = MapLinkCoordinateParser.parse(
            "https://www.google.com/maps/place/Hotel+Mayura+Bhuvaneshwari/@15.3345,76.4562,17z/data=!3d15.335!4d76.46"
        )
        assertEquals("Hotel Mayura Bhuvaneshwari", r.placeNameHint)
        assertNotNull(r.coordinate)
    }

    @Test
    fun `place name hint from percent-encoded unicode path`() {
        val hint = MapLinkCoordinateParser.extractPlaceNameHint(
            "https://www.google.com/maps/place/%E0%A4%B9%E0%A4%82%E0%A4%AA%E0%A5%80/@15.3,76.4,12z"
        )
        assertEquals("हंपी", hint)
    }

    @Test
    fun `hint strips control and bidi characters`() {
        val hint = MapLinkCoordinateParser.extractPlaceNameHint(
            "https://www.google.com/maps/place/Evil%E2%80%AEName%0AInjected/"
        )
        assertEquals("Evil Name Injected", hint)
    }

    @Test
    fun `hint from ftid-only q text`() {
        val r = MapLinkCoordinateParser.parse(
            "https://maps.google.com/?q=Hotel+Mayura+Bhuvaneshwari,+Kamalapur&ftid=0x3bb77e1ec1f4b2a5:0x6b1b5f4b0c8e1a2d"
        )
        assertNull(r.coordinate)
        assertEquals("Hotel Mayura Bhuvaneshwari, Kamalapur", r.placeNameHint)
    }

    @Test
    fun `hint from geo label`() {
        val r = MapLinkCoordinateParser.parse("geo:0,0?q=15.335,76.46(Hotel%20Mayura)")
        assertEquals("Hotel Mayura", r.placeNameHint)
    }

    @Test
    fun `hint from share-intent text preceding the url`() {
        val r = MapLinkCoordinateParser.parse("Hotel Mayura Bhuvaneshwari\nhttps://maps.app.goo.gl/xyz123")
        assertEquals("Hotel Mayura Bhuvaneshwari", r.placeNameHint)
        assertNull(r.coordinate)
    }

    @Test
    fun `coordinate-looking place path is not used as a hint`() {
        assertNull(MapLinkCoordinateParser.extractPlaceNameHint("https://www.google.com/maps/place/15.335,76.46"))
        assertNull(
            MapLinkCoordinateParser.extractPlaceNameHint(
                "https://www.google.com/maps/place/15%C2%B020'06.0%22N+76%C2%B027'36.0%22E/"
            )
        )
    }

    @Test
    fun `garbage, empty and huge inputs never throw`() {
        assertNull(MapLinkCoordinateParser.extractCoordinate(""))
        assertNull(MapLinkCoordinateParser.extractCoordinate("   "))
        assertNull(MapLinkCoordinateParser.extractCoordinate("%%%ZZ%E0%A4"))
        assertNull(MapLinkCoordinateParser.extractCoordinate("!3d!4d@,,z geo: q="))
        val huge = "a".repeat(100_000) + " 15.335, 76.46"
        // Beyond MAX_INPUT_CHARS the tail is ignored; must not throw or hang.
        assertNull(MapLinkCoordinateParser.extractCoordinate(huge))
    }

    @Test
    fun `html-escaped ampersands in copied links are handled`() {
        val p = MapLinkCoordinateParser.extractCoordinate("https://maps.google.com/maps?z=15&amp;q=15.335,76.46")
        assertEquals(CoordinatePattern.QUERY_PARAM, p!!.pattern)
    }
}
