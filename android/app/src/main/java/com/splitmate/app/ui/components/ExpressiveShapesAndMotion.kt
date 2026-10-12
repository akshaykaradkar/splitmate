package com.splitmate.app.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
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
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

/**
 * Material 3 Expressive 6-Token Motion Physics System (`MotionScheme`).
 *
 * - Spatial springs (`dampingRatio < 1.0f`) govern geometry, position, bounds,
 *   corner radii, press-width expansion, and polygon morphing.
 * - Effects springs (`dampingRatio == 1.0f`, critically damped) govern color,
 *   alpha, elevation, and scrim transitions with zero overshoot.
 *
 * v2.4.0 rc3: every spec now comes from the official `androidx.compose.material3.MotionScheme.expressive()`
 * (fastSpatial / defaultSpatial / slowSpatial / fastEffects / defaultEffects / slowEffects). The
 * constants below mirror the official expressive tokens and are only the fallback if a future
 * library version stops returning a `SpringSpec`. Reduced motion still snaps through
 * [SplitMateMotionScheme.reduced] / [LocalMotionScheme], exactly as before.
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

    /** The official Material 3 Expressive motion scheme (plain object, no composition needed). */
    private val expressive: MotionScheme = MotionScheme.expressive()

    private fun <T> springFrom(spec: FiniteAnimationSpec<T>, damping: Float, stiffness: Float): SpringSpec<T> =
        (spec as? SpringSpec<T>) ?: spring(dampingRatio = damping, stiffness = stiffness)

    fun <T> fastSpatial(): SpringSpec<T> =
        springFrom(expressive.fastSpatialSpec(), FAST_SPATIAL_DAMPING, FAST_SPATIAL_STIFFNESS)

    fun <T> defaultSpatial(): SpringSpec<T> =
        springFrom(expressive.defaultSpatialSpec(), DEFAULT_SPATIAL_DAMPING, DEFAULT_SPATIAL_STIFFNESS)

    fun <T> slowSpatial(): SpringSpec<T> =
        springFrom(expressive.slowSpatialSpec(), SLOW_SPATIAL_DAMPING, SLOW_SPATIAL_STIFFNESS)

    fun <T> fastEffects(): SpringSpec<T> =
        springFrom(expressive.fastEffectsSpec(), FAST_EFFECTS_DAMPING, FAST_EFFECTS_STIFFNESS)

    fun <T> defaultEffects(): SpringSpec<T> =
        springFrom(expressive.defaultEffectsSpec(), DEFAULT_EFFECTS_DAMPING, DEFAULT_EFFECTS_STIFFNESS)

    fun <T> slowEffects(): SpringSpec<T> =
        springFrom(expressive.slowEffectsSpec(), SLOW_EFFECTS_DAMPING, SLOW_EFFECTS_STIFFNESS)

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

// ==============================================================================
// v2.3.4 MOTIONSCHEME SHIM (mirrors the material3 1.4 `MotionScheme` member names)
// ==============================================================================

/**
 * Local stand-in for the official Material 3 Expressive `MotionScheme` interface, which does not
 * exist in material3 1.2.1. Member names match the official API (`fastSpatialSpec()` ...
 * `slowEffectsSpec()`) so the v3.0 upgrade is a near-mechanical swap to
 * `MaterialTheme.motionScheme`. The short aliases (`fastSpatial()` ...) mirror [SplitMateMotion].
 *
 * - Spatial specs animate geometry (position, size, corner radii, polygon morphs) and may overshoot.
 * - Effects specs animate colour, alpha and elevation and are critically damped (no overshoot).
 *
 * Read the active scheme through [LocalMotionScheme], which `SplitMateExpressiveTheme` provides.
 */
@ExperimentalMaterial3ExpressiveApi
@Stable
interface SplitMateMotionScheme {
    fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T>
    fun <T> fastSpatialSpec(): FiniteAnimationSpec<T>
    fun <T> slowSpatialSpec(): FiniteAnimationSpec<T>
    fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T>
    fun <T> fastEffectsSpec(): FiniteAnimationSpec<T>
    fun <T> slowEffectsSpec(): FiniteAnimationSpec<T>

    fun <T> defaultSpatial(): FiniteAnimationSpec<T> = defaultSpatialSpec()
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = fastSpatialSpec()
    fun <T> slowSpatial(): FiniteAnimationSpec<T> = slowSpatialSpec()
    fun <T> defaultEffects(): FiniteAnimationSpec<T> = defaultEffectsSpec()
    fun <T> fastEffects(): FiniteAnimationSpec<T> = fastEffectsSpec()
    fun <T> slowEffects(): FiniteAnimationSpec<T> = slowEffectsSpec()

    companion object {
        /** Expressive scheme: bouncy spatial springs, backed 1:1 by [SplitMateMotion]. */
        fun expressive(): SplitMateMotionScheme = ExpressiveSplitMateMotionScheme

        /** Standard scheme: M3 standard tokens (spatial damping 0.9, no visible bounce). */
        fun standard(): SplitMateMotionScheme = StandardSplitMateMotionScheme

        /** Reduced-motion scheme: every spec snaps (used when the animator duration scale is 0). */
        fun reduced(): SplitMateMotionScheme = ReducedSplitMateMotionScheme
    }
}

/** Spatial spring tokens for [SplitMateMotionScheme.standard] (M3 standard motion scheme). */
object SplitMateStandardMotionTokens {
    const val FAST_SPATIAL_DAMPING = 0.9f
    const val FAST_SPATIAL_STIFFNESS = 1400f

    const val DEFAULT_SPATIAL_DAMPING = 0.9f
    const val DEFAULT_SPATIAL_STIFFNESS = 700f

    const val SLOW_SPATIAL_DAMPING = 0.9f
    const val SLOW_SPATIAL_STIFFNESS = 300f
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private object ExpressiveSplitMateMotionScheme : SplitMateMotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = SplitMateMotion.defaultSpatial()
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = SplitMateMotion.fastSpatial()
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = SplitMateMotion.slowSpatial()
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = SplitMateMotion.defaultEffects()
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = SplitMateMotion.fastEffects()
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = SplitMateMotion.slowEffects()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private object StandardSplitMateMotionScheme : SplitMateMotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = spring(
        dampingRatio = SplitMateStandardMotionTokens.DEFAULT_SPATIAL_DAMPING,
        stiffness = SplitMateStandardMotionTokens.DEFAULT_SPATIAL_STIFFNESS
    )

    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = spring(
        dampingRatio = SplitMateStandardMotionTokens.FAST_SPATIAL_DAMPING,
        stiffness = SplitMateStandardMotionTokens.FAST_SPATIAL_STIFFNESS
    )

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = spring(
        dampingRatio = SplitMateStandardMotionTokens.SLOW_SPATIAL_DAMPING,
        stiffness = SplitMateStandardMotionTokens.SLOW_SPATIAL_STIFFNESS
    )

    // Effects tokens are identical in the M3 standard and expressive schemes.
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = SplitMateMotion.defaultEffects()
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = SplitMateMotion.fastEffects()
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = SplitMateMotion.slowEffects()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private object ReducedSplitMateMotionScheme : SplitMateMotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = snap()
}

/**
 * CompositionLocal carrying the active [SplitMateMotionScheme]. `SplitMateExpressiveTheme`
 * provides [SplitMateMotionScheme.expressive] (or [SplitMateMotionScheme.reduced] when the
 * system animator duration scale is 0). Mirrors the official `MaterialTheme.motionScheme`.
 */
@ExperimentalMaterial3ExpressiveApi
val LocalMotionScheme = staticCompositionLocalOf { SplitMateMotionScheme.expressive() }

// ==============================================================================
// v2.3.4 REDUCED MOTION (Settings.Global.ANIMATOR_DURATION_SCALE == 0)
// ==============================================================================

/**
 * Optional override for the reduced-motion flag (previews, screenshot tests). `null` means
 * "read the system setting". `SplitMateExpressiveTheme` provides the resolved system value.
 */
val LocalReducedMotion = compositionLocalOf<Boolean?> { null }

/** Pure rule: animations count as disabled when the animator duration scale is 0 (or below). */
fun isReducedMotionScale(animatorDurationScale: Float): Boolean = animatorDurationScale <= 0f

/**
 * Reads `Settings.Global.ANIMATOR_DURATION_SCALE` (the "Remove animations" accessibility toggle
 * and the developer option). Returns `false` if the setting cannot be read.
 */
fun isReducedMotionEnabled(context: Context): Boolean = try {
    isReducedMotionScale(
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    )
} catch (_: Exception) {
    false
}

/**
 * Composable accessor for the reduced-motion flag. [LocalReducedMotion] wins when set; otherwise
 * the system animator duration scale is read once per context. When `true`, look-alike loaders
 * render a static shape, wavy bars render flat (amplitude 0) and [LocalMotionScheme] snaps.
 */
@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    val systemValue = remember(context) { isReducedMotionEnabled(context) }
    return LocalReducedMotion.current ?: systemValue
}

// ==============================================================================
// v2.3.4 TRIP GUIDE GEOMETRY TOKENS (Decision #5)
// ==============================================================================

/**
 * Trip Guide geometry tokens. Member avatars stay [MaterialShapes.Cookie9Sided] /
 * [MaterialShapes.SoftBurst] (v2.3.2 P4_02); 16.dp squircles are ONLY for place/POI thumbnails.
 */
object GuideShapeTokens {
    val PlaceThumbnailCorner: Dp = 16.dp
    val PlaceThumbnail: RoundedCornerShape = RoundedCornerShape(PlaceThumbnailCorner)
    val MemberAvatarPolygon: RoundedPolygon get() = MaterialShapes.Cookie9Sided
    val MemberAvatarAltPolygon: RoundedPolygon get() = MaterialShapes.SoftBurst
}

/**
 * Material 3 Expressive Normalized Polygon Catalog.
 *
 * v2.4.0 rc3: every named shape delegates to the official `androidx.compose.material3.MaterialShapes`
 * catalog (same names, official geometry). This object stays so existing call sites
 * (`MaterialShapes.Cookie9Sided.toShape()`, `Morph(MaterialShapes.SoftBurst, ...)`) compile unchanged.
 * Official polygons are normalized to the `[0f, 1f] x [0f, 1f]` unit square and cached by the
 * library, so any pair can be passed straight into `Morph(start, end)` and identity is stable.
 * Member avatars stay [Cookie9Sided] / [SoftBurst] (test P4_02).
 */
object MaterialShapes {
    val Circle: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Circle
    val Square: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Square
    val Arch: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Arch
    val Oval: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Oval
    val Pill: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Pill
    val Diamond: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Diamond
    val Pentagon: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Pentagon
    val Sunny: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Sunny
    val VerySunny: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.VerySunny
    val Cookie4Sided: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Cookie4Sided
    val Cookie6Sided: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Cookie6Sided
    val Cookie7Sided: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Cookie7Sided
    val Cookie9Sided: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Cookie9Sided
    val Cookie12Sided: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Cookie12Sided
    val Clover4Leaf: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Clover4Leaf
    val Clover8Leaf: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Clover8Leaf
    val Burst: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Burst
    val SoftBurst: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.SoftBurst
    val Flower: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Flower
    val Puffy: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Puffy
    val Heart: RoundedPolygon get() = androidx.compose.material3.MaterialShapes.Heart

    /** House loader sequence (official shapes): SoftBurst -> Cookie9Sided -> Pentagon -> Pill -> Sunny -> Clover4Leaf -> Oval. */
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
 * Mirrors the official Material 3 Expressive `RoundedPolygon.toShape()` extension
 * (e.g. `MaterialShapes.Cookie9Sided.toShape()`), backed by [RoundedPolygonShape].
 *
 * v2.4.0 rc3: kept local on purpose. The official `androidx.compose.material3.toShape(startAngle: Int)`
 * is `@Composable`, while call sites build avatar shapes inside `remember { ... }` and other
 * non-composable code. The polygons themselves are the official `MaterialShapes` now.
 */
fun RoundedPolygon.toShape(rotationDegrees: Float = 0f): Shape =
    RoundedPolygonShape(polygon = this, rotationDegrees = rotationDegrees)

/**
 * Computes the M3 Expressive Segmented Contained Island item shape (`24.dp` outer island
 * corners, `6.dp` inner item corners, morphing to `24.dp` all-around when selected/expanded).
 */
fun segmentedIslandItemShape(
    index: Int,
    totalCount: Int,
    isSelected: Boolean = false,
    // v2.4.0 D3: house segmented tokens are 16/4 (was 24/6).
    outerCorner: Dp = 16.dp,
    innerCorner: Dp = 4.dp
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
    // v2.4.0 D3: house segmented tokens are 16/4 (was 24/6).
    outerCorner: Dp = 16.dp,
    innerCorner: Dp = 4.dp
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
