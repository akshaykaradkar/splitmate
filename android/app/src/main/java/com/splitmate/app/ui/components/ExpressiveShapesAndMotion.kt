package com.splitmate.app.ui.components

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * Material 3 Expressive 6-Token Motion Physics System (`MotionScheme`).
 *
 * - Spatial springs (`dampingRatio < 1.0f`) govern geometry, position, bounds,
 *   corner radii, press-width expansion, and polygon morphing.
 * - Effects springs (`dampingRatio == 1.0f`, critically damped) govern color,
 *   alpha, elevation, and scrim transitions with zero overshoot.
 */
object SplitMateMotion {
    const val FAST_SPATIAL_DAMPING = 0.6f
    const val FAST_SPATIAL_STIFFNESS = 800f

    const val DEFAULT_SPATIAL_DAMPING = 0.8f
    const val DEFAULT_SPATIAL_STIFFNESS = 380f

    const val SLOW_SPATIAL_DAMPING = 0.8f
    const val SLOW_SPATIAL_STIFFNESS = 200f

    const val FAST_EFFECTS_DAMPING = 1.0f
    const val FAST_EFFECTS_STIFFNESS = 3800f

    const val DEFAULT_EFFECTS_DAMPING = 1.0f
    const val DEFAULT_EFFECTS_STIFFNESS = 1600f

    const val SLOW_EFFECTS_DAMPING = 1.0f
    const val SLOW_EFFECTS_STIFFNESS = 800f

    fun <T> fastSpatial(): SpringSpec<T> =
        spring(dampingRatio = FAST_SPATIAL_DAMPING, stiffness = FAST_SPATIAL_STIFFNESS)

    fun <T> defaultSpatial(): SpringSpec<T> =
        spring(dampingRatio = DEFAULT_SPATIAL_DAMPING, stiffness = DEFAULT_SPATIAL_STIFFNESS)

    fun <T> slowSpatial(): SpringSpec<T> =
        spring(dampingRatio = SLOW_SPATIAL_DAMPING, stiffness = SLOW_SPATIAL_STIFFNESS)

    fun <T> fastEffects(): SpringSpec<T> =
        spring(dampingRatio = FAST_EFFECTS_DAMPING, stiffness = FAST_EFFECTS_STIFFNESS)

    fun <T> defaultEffects(): SpringSpec<T> =
        spring(dampingRatio = DEFAULT_EFFECTS_DAMPING, stiffness = DEFAULT_EFFECTS_STIFFNESS)

    fun <T> slowEffects(): SpringSpec<T> =
        spring(dampingRatio = SLOW_EFFECTS_DAMPING, stiffness = SLOW_EFFECTS_STIFFNESS)

    fun fastSpatialFloat(): SpringSpec<Float> = fastSpatial()
    fun defaultSpatialFloat(): SpringSpec<Float> = defaultSpatial()
    fun slowSpatialFloat(): SpringSpec<Float> = slowSpatial()

    fun fastSpatialDp(): SpringSpec<Dp> = fastSpatial()
    fun defaultSpatialDp(): SpringSpec<Dp> = defaultSpatial()
    fun slowSpatialDp(): SpringSpec<Dp> = slowSpatial()

    fun fastEffectsColor(): SpringSpec<Color> = fastEffects()
    fun defaultEffectsColor(): SpringSpec<Color> = defaultEffects()
    fun slowEffectsColor(): SpringSpec<Color> = slowEffects()
}

/**
 * Material 3 Expressive Normalized Polygon Catalog (`androidx.graphics:graphics-shapes:1.0.1`).
 * Every polygon is normalized to the `[0f, 1f] x [0f, 1f]` unit square so any pair of
 * polygons can be passed directly into `Morph(start, end)` without off-center distortion.
 */
object MaterialShapes {
    val Circle: RoundedPolygon by lazy {
        RoundedPolygon.circle(numVertices = 8).normalized()
    }

    val Square: RoundedPolygon by lazy {
        RoundedPolygon.rectangle(
            width = 2f,
            height = 2f,
            rounding = CornerRounding(radius = 0.32f, smoothing = 0.5f)
        ).normalized()
    }

    val Arch: RoundedPolygon by lazy {
        RoundedPolygon(
            numVertices = 4,
            rounding = CornerRounding(radius = 0.22f, smoothing = 0.4f),
            perVertexRounding = listOf(
                CornerRounding(radius = 1.0f, smoothing = 0.5f),
                CornerRounding(radius = 1.0f, smoothing = 0.5f),
                CornerRounding(radius = 0.22f, smoothing = 0.4f),
                CornerRounding(radius = 0.22f, smoothing = 0.4f)
            )
        ).normalized()
    }

    val Oval: RoundedPolygon by lazy {
        RoundedPolygon.rectangle(
            width = 2f,
            height = 1.38f,
            rounding = CornerRounding(radius = 0.69f, smoothing = 0.9f)
        ).normalized()
    }

    val Pill: RoundedPolygon by lazy {
        RoundedPolygon.rectangle(
            width = 2f,
            height = 1.12f,
            rounding = CornerRounding(radius = 0.56f, smoothing = 0.35f)
        ).normalized()
    }

    val Diamond: RoundedPolygon by lazy {
        RoundedPolygon(
            vertices = floatArrayOf(0f, -1f, 0.88f, 0f, 0f, 1f, -0.88f, 0f),
            rounding = CornerRounding(radius = 0.24f, smoothing = 0.45f)
        ).normalized()
    }

    val Pentagon: RoundedPolygon by lazy {
        RoundedPolygon(
            numVertices = 5,
            rounding = CornerRounding(radius = 0.32f, smoothing = 0.5f)
        ).normalized()
    }

    val Sunny: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            innerRadius = 0.78f,
            rounding = CornerRounding(radius = 0.22f, smoothing = 0.45f),
            innerRounding = CornerRounding(radius = 0.14f, smoothing = 0.3f)
        ).normalized()
    }

    val VerySunny: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            innerRadius = 0.64f,
            rounding = CornerRounding(radius = 0.16f, smoothing = 0.35f),
            innerRounding = CornerRounding(radius = 0.12f, smoothing = 0.25f)
        ).normalized()
    }

    val Cookie4Sided: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 4,
            innerRadius = 0.68f,
            rounding = CornerRounding(radius = 0.38f, smoothing = 0.55f),
            innerRounding = CornerRounding(radius = 0.38f, smoothing = 0.55f)
        ).normalized()
    }

    val Cookie6Sided: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 6,
            innerRadius = 0.72f,
            rounding = CornerRounding(radius = 0.35f, smoothing = 0.55f),
            innerRounding = CornerRounding(radius = 0.35f, smoothing = 0.55f)
        ).normalized()
    }

    val Cookie7Sided: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 7,
            innerRadius = 0.74f,
            rounding = CornerRounding(radius = 0.34f, smoothing = 0.55f),
            innerRounding = CornerRounding(radius = 0.34f, smoothing = 0.55f)
        ).normalized()
    }

    val Cookie9Sided: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 9,
            innerRadius = 0.78f,
            rounding = CornerRounding(radius = 0.32f, smoothing = 0.55f),
            innerRounding = CornerRounding(radius = 0.32f, smoothing = 0.55f)
        ).normalized()
    }

    val Cookie12Sided: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            innerRadius = 0.82f,
            rounding = CornerRounding(radius = 0.28f, smoothing = 0.55f),
            innerRounding = CornerRounding(radius = 0.28f, smoothing = 0.55f)
        ).normalized()
    }

    val Clover4Leaf: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 4,
            innerRadius = 0.52f,
            rounding = CornerRounding(radius = 0.42f, smoothing = 0.6f),
            innerRounding = CornerRounding(radius = 0.12f, smoothing = 0.25f)
        ).normalized()
    }

    val Clover8Leaf: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            innerRadius = 0.62f,
            rounding = CornerRounding(radius = 0.36f, smoothing = 0.55f),
            innerRounding = CornerRounding(radius = 0.12f, smoothing = 0.25f)
        ).normalized()
    }

    val Burst: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            innerRadius = 0.70f,
            rounding = CornerRounding(radius = 0.06f, smoothing = 0.1f),
            innerRounding = CornerRounding(radius = 0.06f, smoothing = 0.1f)
        ).normalized()
    }

    val SoftBurst: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 10,
            innerRadius = 0.76f,
            rounding = CornerRounding(radius = 0.26f, smoothing = 0.5f),
            innerRounding = CornerRounding(radius = 0.26f, smoothing = 0.5f)
        ).normalized()
    }

    val Flower: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 6,
            innerRadius = 0.58f,
            rounding = CornerRounding(radius = 0.44f, smoothing = 0.55f),
            innerRounding = CornerRounding(radius = 0.16f, smoothing = 0.3f)
        ).normalized()
    }

    val Puffy: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            innerRadius = 0.84f,
            rounding = CornerRounding(radius = 0.42f, smoothing = 0.6f),
            innerRounding = CornerRounding(radius = 0.32f, smoothing = 0.5f)
        ).normalized()
    }

    val Heart: RoundedPolygon by lazy {
        RoundedPolygon(
            vertices = floatArrayOf(
                0f, 0.95f,
                -0.95f, 0.05f,
                -0.60f, -0.80f,
                0f, -0.35f,
                0.60f, -0.80f,
                0.95f, 0.05f
            ),
            rounding = CornerRounding(radius = 0.35f, smoothing = 0.5f),
            perVertexRounding = listOf(
                CornerRounding(radius = 0.12f, smoothing = 0.2f),
                CornerRounding(radius = 0.45f, smoothing = 0.6f),
                CornerRounding(radius = 0.45f, smoothing = 0.6f),
                CornerRounding(radius = 0.08f, smoothing = 0.1f),
                CornerRounding(radius = 0.45f, smoothing = 0.6f),
                CornerRounding(radius = 0.45f, smoothing = 0.6f)
            )
        ).normalized()
    }

    val morphSequence: List<RoundedPolygon> by lazy {
        listOf(
            SoftBurst,
            Cookie9Sided,
            Pentagon,
            Pill,
            Sunny,
            Clover4Leaf,
            Oval
        )
    }
}

/**
 * Compose `Shape` wrapper that interpolates between two normalized `RoundedPolygon` instances
 * via `androidx.graphics.shapes.Morph` at `percentage` (`0f..1f`) with optional `rotationDegrees`.
 */
class MorphPolygonShape(
    private val morph: Morph,
    private val percentage: Float,
    private val rotationDegrees: Float = 0f
) : Shape {
    private val matrix = Matrix()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        matrix.reset()
        matrix.scale(size.width, size.height)
        if (rotationDegrees != 0f) {
            matrix.translate(0.5f, 0.5f)
            matrix.rotateZ(rotationDegrees)
            matrix.translate(-0.5f, -0.5f)
        }
        val composePath = morph.toPath(progress = percentage.coerceIn(0f, 1f)).asComposePath()
        composePath.transform(matrix)
        return Outline.Generic(composePath)
    }
}

/**
 * Static Compose `Shape` wrapper for any normalized `RoundedPolygon` in [MaterialShapes].
 */
class RoundedPolygonShape(
    private val polygon: RoundedPolygon,
    private val rotationDegrees: Float = 0f
) : Shape {
    private val matrix = Matrix()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        matrix.reset()
        matrix.scale(size.width, size.height)
        if (rotationDegrees != 0f) {
            matrix.translate(0.5f, 0.5f)
            matrix.rotateZ(rotationDegrees)
            matrix.translate(-0.5f, -0.5f)
        }
        val composePath = polygon.toPath().asComposePath()
        composePath.transform(matrix)
        return Outline.Generic(composePath)
    }
}

/**
 * Computes the M3 Expressive Segmented Contained Island item shape (`24.dp` outer island
 * corners, `6.dp` inner item corners, morphing to `24.dp` all-around when selected/expanded).
 */
fun segmentedIslandItemShape(
    index: Int,
    totalCount: Int,
    isSelected: Boolean = false,
    outerCorner: Dp = 24.dp,
    innerCorner: Dp = 6.dp
): RoundedCornerShape {
    if (isSelected || totalCount <= 1) {
        return RoundedCornerShape(outerCorner)
    }
    return when {
        index <= 0 -> RoundedCornerShape(
            topStart = outerCorner,
            topEnd = outerCorner,
            bottomStart = innerCorner,
            bottomEnd = innerCorner
        )
        index >= totalCount - 1 -> RoundedCornerShape(
            topStart = innerCorner,
            topEnd = innerCorner,
            bottomStart = outerCorner,
            bottomEnd = outerCorner
        )
        else -> RoundedCornerShape(innerCorner)
    }
}

/**
 * Spring-animated variant of [segmentedIslandItemShape] using [SplitMateMotion.fastSpatial]
 * so list rows smoothly morph their inner corners from `6.dp` to `24.dp` pills when tapped.
 */
@Composable
fun rememberAnimatedSegmentedIslandItemShape(
    index: Int,
    totalCount: Int,
    isSelected: Boolean = false,
    outerCorner: Dp = 24.dp,
    innerCorner: Dp = 6.dp
): RoundedCornerShape {
    val targetTop = if (isSelected || totalCount <= 1 || index <= 0) outerCorner else innerCorner
    val targetBottom = if (isSelected || totalCount <= 1 || index >= totalCount - 1) outerCorner else innerCorner
    val topCorner by animateDpAsState(
        targetValue = targetTop,
        animationSpec = SplitMateMotion.fastSpatial(),
        label = "SegmentedIslandTopCorner"
    )
    val bottomCorner by animateDpAsState(
        targetValue = targetBottom,
        animationSpec = SplitMateMotion.fastSpatial(),
        label = "SegmentedIslandBottomCorner"
    )
    return remember(topCorner, bottomCorner) {
        RoundedCornerShape(
            topStart = topCorner,
            topEnd = topCorner,
            bottomStart = bottomCorner,
            bottomEnd = bottomCorner
        )
    }
}
