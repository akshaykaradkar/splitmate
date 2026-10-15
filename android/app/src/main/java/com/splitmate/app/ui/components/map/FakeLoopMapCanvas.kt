package com.splitmate.app.ui.components.map

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.tan

/**
 * v2.4.0 rc15: a static stand-in for the WebView map (tests, screenshots, previews): the land
 * colour, the closed route and numbered markers, projected with the same Web Mercator maths.
 */
@Composable
fun FakeLoopMapCanvas(model: LoopMapModel, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val label = MaterialTheme.typography.labelMedium
    Canvas(modifier = modifier.semantics { contentDescription = "Map preview of the loop" }) {
        drawRect(scheme.surfaceContainerLow)
        val pts = model.points
        if (pts.isEmpty()) return@Canvas
        fun merc(lat: Double) = ln(tan(PI / 4 + Math.toRadians(lat) / 2))
        // Web Mercator: x and y both in radians, so the map keeps its true aspect ratio.
        fun lngRad(lng: Double) = Math.toRadians(lng)
        val xs = pts.map { lngRad(it.lng) }
        val ys = pts.map { merc(it.lat) }
        // Same padding as the live map's fitBounds: room for the toolbar on the right.
        val padH = 32.dp.toPx()
        val padRight = 88.dp.toPx()
        val padTop = 56.dp.toPx()
        val padBottom = 100.dp.toPx()
        val spanX = max(xs.max() - xs.min(), 1e-9)
        val spanY = max(ys.max() - ys.min(), 1e-9)
        val usableW = size.width - padH - padRight
        val usableH = size.height - padTop - padBottom
        val scale = minOf(usableW / spanX, usableH / spanY)
        val cx = (xs.max() + xs.min()) / 2
        val cy = (ys.max() + ys.min()) / 2
        val centerX = padH + usableW / 2
        fun at(p: LoopMapPoint) = Offset(
            (centerX + (lngRad(p.lng) - cx) * scale).toFloat(),
            (padTop + usableH / 2 - (merc(p.lat) - cy) * scale).toFloat()
        )
        val stay = pts.firstOrNull { it.isStay }
        val stops = pts.filter { !it.isStay }.sortedBy { it.index }
        val path = androidx.compose.ui.graphics.Path()
        val ordered = listOfNotNull(stay) + stops + listOfNotNull(stay)
        ordered.forEachIndexed { i, p -> val o = at(p); if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }
        drawPath(path, scheme.surfaceContainerLowest, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, scheme.primary, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        stops.forEach { p ->
            val o = at(p)
            drawCircle(scheme.surfaceContainerLowest, radius = 14.dp.toPx(), center = o)
            drawCircle(scheme.primary, radius = 11.dp.toPx(), center = o)
            val t = measurer.measure(p.index.toString(), TextStyle(color = scheme.onPrimary, fontSize = label.fontSize, fontWeight = label.fontWeight))
            drawText(t, topLeft = Offset(o.x - t.size.width / 2f, o.y - t.size.height / 2f))
        }
        stay?.let {
            val o = at(it)
            drawCircle(scheme.surfaceContainerLowest, radius = 15.dp.toPx(), center = o)
            drawCircle(scheme.onSurface, radius = 12.dp.toPx(), center = o)
            val t = measurer.measure("H", TextStyle(color = scheme.surfaceContainerLowest, fontSize = label.fontSize, fontWeight = label.fontWeight))
            drawText(t, topLeft = Offset(o.x - t.size.width / 2f, o.y - t.size.height / 2f))
        }
    }
}
