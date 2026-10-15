package com.splitmate.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.data.guide.flags.GuideFeatureFlagsProvider
import com.splitmate.app.ui.SplitMateThemeMode
import com.splitmate.app.ui.components.map.LoopMapController
import com.splitmate.app.ui.components.map.LoopMapStyle
import com.splitmate.app.ui.theme.MapExtendedColors
import com.splitmate.app.ui.toPalette
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * v2.4.0 rc15 (branch feature/rc15-map): the Loop map. MapLibre GL JS inside the system WebView
 * (no native code: the APK barely grows), OpenFreeMap tiles, every colour from the theme tokens,
 * only one network host allowed, and the stop list stays the accessible path.
 */
class SplitMateV240Rc15LoopMapTest {

    private fun src(path: String) = File("src/main/java/com/splitmate/app/$path").readText()
    private fun asset(path: String) = File("src/main/assets/$path")

    private fun cr(a: Color, b: Color): Double {
        val la = a.luminance().toDouble(); val lb = b.luminance().toDouble()
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Test
    fun `RC15_01 the style is valid MapLibre JSON on OpenFreeMap, coloured only from theme tokens`() {
        SplitMateThemeMode.entries.forEach { mode ->
            val colors = MapExtendedColors.of(mode.toPalette())
            val style = JSONObject(LoopMapStyle.style(colors))
            assertEquals(8, style.getInt("version"))
            assertEquals(LoopMapStyle.GLYPHS_URL, style.getString("glyphs"))
            val sources = style.getJSONObject("sources")
            assertEquals(setOf("openmaptiles"), sources.keys().asSequence().toSet())
            assertEquals(LoopMapStyle.TILES_URL, sources.getJSONObject("openmaptiles").getString("url"))
            val text = style.toString()
            assertFalse(text.contains("{{"), "no unfilled template tokens")
            assertTrue(text.contains(LoopMapStyle.hex(colors.land)) && text.contains(LoopMapStyle.hex(colors.water)) && text.contains(LoopMapStyle.hex(colors.park)))
            val hexes = Regex("#[0-9A-F]{6}").findAll(text).map { it.value }.toSet()
            val roleHexes = listOf(colors.land, colors.residential, colors.park, colors.water, colors.waterLabel, colors.building, colors.roadMinor,
                colors.roadMajor, colors.roadCasing, colors.rail, colors.boundary, colors.label, colors.labelHalo).map { LoopMapStyle.hex(it) }.toSet()
            assertTrue(roleHexes.containsAll(hexes), "$mode: every colour comes from MapColorRoles ($hexes)")
        }
    }

    @Test
    fun `RC15_02 route and labels stay readable on the basemap in every theme`() {
        SplitMateThemeMode.entries.forEach { mode ->
            val c = MapExtendedColors.of(mode.toPalette())
            assertTrue(cr(c.route, c.land) >= 3.0, "$mode route vs land ${cr(c.route, c.land)}")
            assertTrue(cr(c.route, c.roadMajor) >= 3.0, "$mode route vs roads ${cr(c.route, c.roadMajor)}")
            assertTrue(cr(c.label, c.labelHalo) >= 4.5, "$mode labels ${cr(c.label, c.labelHalo)}")
            assertTrue(cr(c.onStop, c.stop) >= 4.5, "$mode stop numbers ${cr(c.onStop, c.stop)}")
            assertTrue(cr(c.onStay, c.stay) >= 4.5, "$mode stay marker ${cr(c.onStay, c.stay)}")
        }
    }

    @Test
    fun `RC15_03 the loop GeoJSON is closed at the stay with numbered stops`() {
        val stay = LatLng(15.30, 76.46)
        val stops = (1..3).map { Place(id = "wd:Q$it", kind = PlaceKind.SEE, name = "Stop $it", location = LatLng(15.30 + it * 0.01, 76.46), source = PlaceSource.WIKIDATA) } +
            Place(id = "x", kind = PlaceKind.SEE, name = "No coords", location = null, source = PlaceSource.USER)
        val d = JSONObject(LoopMapStyle.data(stay, "Hotel \"Mayura\" </script>", stops))
        val line = d.getJSONObject("route").getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
        assertEquals(5, line.length(), "stay + 3 stops + stay")
        assertEquals(line.getJSONArray(0).toString(), line.getJSONArray(4).toString(), "closed at the stay")
        val points = d.getJSONObject("points").getJSONArray("features")
        assertEquals(4, points.length())
        assertEquals(LoopMapStyle.STAY_ID, points.getJSONObject(0).getJSONObject("properties").getString("id"))
        assertEquals(3, points.getJSONObject(3).getJSONObject("properties").getInt("index"))
        assertEquals("Hotel \"Mayura\" </script>", points.getJSONObject(0).getJSONObject("properties").getString("name"), "names travel as data, unchanged")
    }

    @Test
    fun `RC15_04 the map can only reach OpenFreeMap and its own bundled page`() {
        assertTrue(LoopMapStyle.isAllowedHost("tiles.openfreemap.org"))
        assertTrue(LoopMapStyle.isAllowedHost(LoopMapStyle.ASSET_HOST))
        listOf("evil.example", "tiles.openfreemap.org.evil.example", null, "unpkg.com", "api.maptiler.com").forEach {
            assertFalse(LoopMapStyle.isAllowedHost(it), "blocked: $it")
        }
        val html = asset("map/map.html").readText()
        assertTrue(html.contains("connect-src https://tiles.openfreemap.org;"), "CSP limits network to OpenFreeMap")
        assertTrue(html.contains("script-src 'self'"), "only bundled scripts run")
        val canvas = src("ui/components/map/LoopMapCanvas.kt")
        assertTrue(canvas.contains("settings.allowFileAccess = false") && canvas.contains("setGeolocationEnabled(false)"))
        assertTrue(canvas.contains("override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true"), "no navigation away")
        assertTrue(canvas.contains("if (!path.startsWith(\"map/\") || path.contains(\"..\")) return blocked()"), "asset path traversal blocked")
    }

    @Test
    fun `RC15_05 commands and data cross the bridge as quoted JSON`() {
        val sent = ArrayList<String>()
        val c = LoopMapController()
        c.select("wd:Q1\"); alert(1); (\"")
        c.zoomIn()
        c.attach { sent += it }
        assertEquals(2, sent.size, "commands before the page is ready are queued, then replayed")
        assertEquals("splitmate.select(" + JSONObject.quote("wd:Q1\"); alert(1); (\"") + ",true)", sent[0])
        c.reducedMotion = true
        c.fitLoop()
        assertEquals("splitmate.fit(false)", sent.last(), "Remove animations: camera jumps")
        val canvas = src("ui/components/map/LoopMapCanvas.kt")
        assertFalse(Regex("""evaluateJavascript\("[^"]*\$\{model\.""").containsMatchIn(canvas), "model JSON is never spliced in unquoted")
        assertTrue(canvas.contains("JSONObject.quote(model.styleJson)") && canvas.contains("JSONObject.quote(model.dataJson)"))
    }

    @Test
    fun `RC15_06 almost no APK growth - one bundled JS library, no native map code`() {
        val js = asset("map/maplibre-gl.js")
        assertTrue(js.exists() && asset("map/maplibre-gl.css").exists() && asset("map/LICENSE-maplibre-gl.txt").exists())
        assertTrue(js.readText().take(200).contains("3-Clause BSD"), "MapLibre GL JS licence kept")
        val total = File("src/main/assets/map").listFiles()!!.sumOf { it.length() }
        assertTrue(total < 1_300_000, "map assets stay small (${total / 1024} KB raw, ~0.3 MB compressed in the APK)")
        val gradle = File("build.gradle").readText()
        assertFalse(gradle.contains("org.maplibre.gl:android-sdk"), "no native MapLibre (+11 MB per ABI)")
    }

    @Test
    fun `RC15_07 the map pane is mapcn-style M3 Expressive, accessible, behind a kill switch`() {
        val pane = src("ui/components/map/LoopMapPane.kt")
        assertTrue(pane.contains("VerticalFloatingToolbar(") && pane.contains("shapes = IconButtonDefaults.shapes()"), "edge-docked expressive toolbar")
        assertTrue(pane.contains("\"Zoom in\"") && pane.contains("\"Zoom out\"") && pane.contains("\"Fit the whole loop\""), "labelled controls")
        assertTrue(pane.contains("MapAttributionChip(") && src("ui/components/map/LoopMapStyle.kt").contains("© OpenStreetMap contributors"), "OSM credit always visible")
        assertTrue(pane.contains("ElevatedCard(") && pane.contains("surfaceContainerLowest"), "borderless card on the white role")
        assertTrue(pane.contains("The list below has every stop."), "TalkBack is pointed at the list")
        listOf("LoopMapPane.kt", "LoopMapCanvas.kt", "FakeLoopMapCanvas.kt", "LoopMapStyle.kt").forEach { f ->
            val s = src("ui/components/map/$f")
            assertFalse(Regex("""Color\(0x|RoundedCornerShape\(|fontSize\s*=\s*\d|\btween\(""").containsMatchIn(s), "$f: tokens only")
        }
        assertFalse(src("ui/theme/MapExtendedColors.kt").contains("Color(0x"), "map colours derive from the palette (no new hex)")
        val loop = src("ui/screens/plan/DayLoopView.kt")
        assertTrue(loop.contains("val showMap = state.mapEnabled && stay != null && route != null && stops.isNotEmpty()"))
        assertTrue(GuideFeatureFlagsProvider.parse("""{"mapEnabled": false}""")!!.mapEnabled.not(), "remote kill switch")
        assertTrue(GuideFeatureFlagsProvider.parse("""{}""")!!.mapEnabled, "on by default")
    }
}
