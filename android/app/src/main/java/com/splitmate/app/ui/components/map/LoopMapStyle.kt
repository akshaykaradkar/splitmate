package com.splitmate.app.ui.components.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.Place
import com.splitmate.app.ui.theme.MapColorRoles
import org.json.JSONArray
import org.json.JSONObject

/**
 * v2.4.0 rc15: pure-Kotlin builders for the Loop map (JVM-testable).
 *
 * - [style]: a calm, mapcn-like MapLibre style over OpenFreeMap vector tiles (OpenMapTiles schema),
 *   every colour taken from [MapColorRoles] (which derives from the theme palette).
 * - [overlay]: the colours the page uses for the route, stop and stay markers.
 * - [data]: GeoJSON for the closed loop (stay -> stops -> stay) and its points.
 */
object LoopMapStyle {
    const val TILE_HOST = "tiles.openfreemap.org"
    const val TILES_URL = "https://$TILE_HOST/planet"
    const val GLYPHS_URL = "https://$TILE_HOST/fonts/{fontstack}/{range}.pbf"
    /** Shown as a Compose chip (OSMF attribution guidelines: visible, linked). */
    const val ATTRIBUTION = "© OpenStreetMap contributors · OpenFreeMap"
    const val ATTRIBUTION_URL = "https://www.openstreetmap.org/copyright"

    private val FONT_REGULAR = JSONArray().put("Noto Sans Regular")
    private val FONT_BOLD = JSONArray().put("Noto Sans Bold")
    private val FONT_ITALIC = JSONArray().put("Noto Sans Italic")

    fun hex(c: Color): String = String.format("#%06X", c.toArgb() and 0xFFFFFF)

    fun style(c: MapColorRoles): String {
        val layers = JSONArray()
        fun layer(id: String, type: String, sourceLayer: String?, block: JSONObject.() -> Unit) {
            val o = JSONObject().put("id", id).put("type", type)
            if (sourceLayer != null) o.put("source", "openmaptiles").put("source-layer", sourceLayer)
            o.block()
            layers.put(o)
        }
        fun j(vararg pairs: Pair<String, Any>) = JSONObject().apply { pairs.forEach { put(it.first, it.second) } }
        fun a(vararg items: Any) = JSONArray().apply { items.forEach { put(it) } }
        fun zoomRamp(vararg stops: Number): JSONArray = a("interpolate", a("exponential", 1.5), a("zoom"), *stops)
        val name = a("coalesce", a("get", "name:latin"), a("get", "name"))

        layer("background", "background", null) { put("paint", j("background-color" to hex(c.land))) }
        layer("residential", "fill", "landuse") {
            put("filter", a("==", a("get", "class"), "residential"))
            put("paint", j("fill-color" to hex(c.residential), "fill-opacity" to 0.7))
        }
        layer("wood", "fill", "landcover") {
            put("filter", a("match", a("get", "class"), a("wood", "grass"), true, false))
            put("paint", j("fill-color" to hex(c.park), "fill-opacity" to 0.6))
        }
        layer("park", "fill", "park") { put("paint", j("fill-color" to hex(c.park))) }
        layer("water", "fill", "water") { put("paint", j("fill-color" to hex(c.water))) }
        layer("waterway", "line", "waterway") {
            put("paint", j("line-color" to hex(c.water), "line-width" to zoomRamp(10, 1, 16, 3)))
        }
        layer("building", "fill", "building") {
            put("minzoom", 14)
            put("paint", j("fill-color" to hex(c.building), "fill-opacity" to 0.8))
        }
        val minor = a("match", a("get", "class"), a("minor", "service", "track"), true, false)
        val major = a("match", a("get", "class"), a("primary", "secondary", "tertiary", "trunk"), true, false)
        val motorway = a("==", a("get", "class"), "motorway")
        layer("road-minor-casing", "line", "transportation") {
            put("minzoom", 12); put("filter", minor)
            put("layout", j("line-join" to "round", "line-cap" to "round"))
            put("paint", j("line-color" to hex(c.roadCasing), "line-width" to zoomRamp(12, 1.5, 18, 14)))
        }
        layer("road-major-casing", "line", "transportation") {
            put("minzoom", 8); put("filter", a("any", major, motorway))
            put("layout", j("line-join" to "round", "line-cap" to "round"))
            put("paint", j("line-color" to hex(c.roadCasing), "line-width" to zoomRamp(8, 1.5, 18, 22)))
        }
        layer("road-path", "line", "transportation") {
            put("minzoom", 14); put("filter", a("match", a("get", "class"), a("path", "pedestrian"), true, false))
            put("paint", j("line-color" to hex(c.roadCasing), "line-width" to 1, "line-dasharray" to a(2, 2)))
        }
        layer("road-minor", "line", "transportation") {
            put("minzoom", 12); put("filter", minor)
            put("layout", j("line-join" to "round", "line-cap" to "round"))
            put("paint", j("line-color" to hex(c.roadMinor), "line-width" to zoomRamp(12, 0.5, 18, 12)))
        }
        layer("road-major", "line", "transportation") {
            put("minzoom", 8); put("filter", a("any", major, motorway))
            put("layout", j("line-join" to "round", "line-cap" to "round"))
            put("paint", j("line-color" to hex(c.roadMajor), "line-width" to zoomRamp(8, 0.75, 18, 18)))
        }
        layer("rail", "line", "transportation") {
            put("minzoom", 11); put("filter", a("==", a("get", "class"), "rail"))
            put("paint", j("line-color" to hex(c.rail), "line-width" to 1, "line-opacity" to 0.6))
        }
        layer("boundary", "line", "boundary") {
            put("filter", a("all", a("<=", a("get", "admin_level"), 4), a("!=", a("get", "maritime"), 1)))
            put("paint", j("line-color" to hex(c.boundary), "line-width" to 1, "line-dasharray" to a(3, 2), "line-opacity" to 0.5))
        }
        layer("water-label", "symbol", "water_name") {
            put("layout", j("text-field" to name, "text-font" to FONT_ITALIC, "text-size" to 12))
            put("paint", j("text-color" to hex(c.waterLabel), "text-halo-color" to hex(c.water), "text-halo-width" to 1))
        }
        layer("road-label", "symbol", "transportation_name") {
            put("minzoom", 13)
            put("layout", j("symbol-placement" to "line", "text-field" to name, "text-font" to FONT_REGULAR, "text-size" to 11))
            put("paint", j("text-color" to hex(c.label), "text-halo-color" to hex(c.labelHalo), "text-halo-width" to 1.5))
        }
        layer("place-minor-label", "symbol", "place") {
            put("minzoom", 12)
            put("filter", a("match", a("get", "class"), a("suburb", "neighbourhood", "quarter", "hamlet"), true, false))
            put("layout", j("text-field" to name, "text-font" to FONT_REGULAR, "text-size" to 12, "text-max-width" to 8))
            put("paint", j("text-color" to hex(c.label), "text-halo-color" to hex(c.labelHalo), "text-halo-width" to 1.5))
        }
        layer("place-label", "symbol", "place") {
            put("filter", a("match", a("get", "class"), a("city", "town", "village"), true, false))
            put("layout", j("text-field" to name, "text-font" to FONT_BOLD, "text-size" to zoomRamp(6, 11, 14, 16), "text-max-width" to 8))
            put("paint", j("text-color" to hex(c.label), "text-halo-color" to hex(c.labelHalo), "text-halo-width" to 2))
        }

        return JSONObject()
            .put("version", 8)
            .put("name", "SplitMate Loop")
            .put("glyphs", GLYPHS_URL)
            .put("sources", JSONObject().put("openmaptiles", JSONObject().put("type", "vector").put("url", TILES_URL)))
            .put("layers", layers)
            .toString()
    }

    fun overlay(c: MapColorRoles): String = JSONObject()
        .put("route", hex(c.route))
        .put("routeCasing", hex(c.routeCasing))
        .put("stop", hex(c.stop))
        .put("onStop", hex(c.onStop))
        .put("stopRing", hex(c.stopRing))
        .put("stay", hex(c.stay))
        .put("onStay", hex(c.onStay))
        .toString()

    /** GeoJSON: the closed loop line (stay, stops…, stay) and the points (stay + numbered stops). */
    fun data(stay: LatLng, stayLabel: String, stops: List<Place>): String {
        val located = stops.filter { it.location != null }
        val line = JSONArray().put(coord(stay))
        located.forEach { line.put(coord(it.location!!)) }
        line.put(coord(stay))
        val route = JSONObject().put("type", "FeatureCollection").put(
            "features",
            JSONArray().apply {
                if (located.isNotEmpty()) put(
                    JSONObject().put("type", "Feature").put("properties", JSONObject())
                        .put("geometry", JSONObject().put("type", "LineString").put("coordinates", line))
                )
            }
        )
        val points = JSONArray().put(point(stay, JSONObject().put("id", STAY_ID).put("kind", "STAY").put("name", stayLabel).put("index", 0)))
        located.forEachIndexed { i, p ->
            points.put(point(p.location!!, JSONObject().put("id", p.id).put("kind", p.kind.name).put("name", p.name).put("index", i + 1)))
        }
        return JSONObject()
            .put("route", route)
            .put("points", JSONObject().put("type", "FeatureCollection").put("features", points))
            .toString()
    }

    const val STAY_ID = "__stay__"

    private fun coord(l: LatLng) = JSONArray().put(l.lng).put(l.lat)
    private fun point(l: LatLng, props: JSONObject) = JSONObject().put("type", "Feature").put("properties", props)
        .put("geometry", JSONObject().put("type", "Point").put("coordinates", coord(l)))

    /**
     * Network allow-list for the map WebView: only the bundled page (served from [ASSET_HOST]) and
     * OpenFreeMap tiles/fonts. Everything else is refused, so the map can't phone anywhere else.
     */
    fun isAllowedHost(host: String?): Boolean = host == ASSET_HOST || host == TILE_HOST

    /** Same host WebViewAssetLoader uses by convention; served by the app, never the network. */
    const val ASSET_HOST = "appassets.androidplatform.net"
    const val PAGE_URL = "https://$ASSET_HOST/map/map.html"
}
