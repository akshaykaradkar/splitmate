package com.splitmate.app.data.guide.loop

import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.RouteCostProvider
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Great-circle distance helpers (mean Earth radius R = 6,371.0088 km, IUGG). */
object Haversine {
    const val EARTH_RADIUS_KM = 6371.0088

    /** Straight-line (great-circle) distance in km between [a] and [b]. */
    fun km(a: LatLng, b: LatLng): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val s1 = sin(dLat / 2)
        val s2 = sin(dLng / 2)
        val h = s1 * s1 + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * s2 * s2
        return 2.0 * EARTH_RADIUS_KM * asin(min(1.0, sqrt(h)))
    }
}

/**
 * [RouteCostProvider] using straight-line Haversine distances. The matrix is symmetric with a
 * zero diagonal; each pair is computed once.
 */
class HaversineCostProvider : RouteCostProvider {
    override fun matrixKm(points: List<LatLng>): Array<DoubleArray> {
        val n = points.size
        val m = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val d = Haversine.km(points[i], points[j])
                m[i][j] = d
                m[j][i] = d
            }
        }
        return m
    }
}
