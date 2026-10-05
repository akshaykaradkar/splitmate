package com.splitmate.app.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * v2.3.6 Step C: list entrance. The first few cards of a screen rise and fade in one after another
 * each time the user opens that screen (e.g. every time a trip is opened from Ledgers). Switching
 * back to a tab inside the same visit, closing a ticket overlay, or scrolling an item back into view
 * never replays it.
 *
 * Device feedback (preview4): the first version played only once per app process and ran during the
 * Ledgers -> Trip Hub container transform, so on a real phone it was either already used up or
 * hidden behind the morph. It now plays once per visit and waits for the screen's enter transition
 * to settle before the cards rise.
 */
object ExpressiveEntranceDefaults {
    /** Delay between consecutive items. */
    const val StaggerMillis: Long = 55L

    /** Only the first items stagger; the rest are below the fold on open anyway. */
    const val MaxStaggeredItems: Int = 6

    /** Items composed later than this after the screen opened appear without an entrance. */
    const val EntranceWindowMillis: Long = 1_200L

    /** Distance each item rises while fading in. */
    val InitialOffset: Dp = 36.dp

    /** Starting scale of each item (it grows to full size while rising). */
    const val InitialScale: Float = 0.96f
}

/** Record of which screens already played their entrance. */
object ExpressiveEntranceRegistry {
    private val playedScreenKeys = mutableSetOf<String>()

    /** Returns true exactly once per [screenKey] until its visit restarts (see [beginVisit]). */
    @Synchronized
    fun claim(screenKey: String): Boolean = playedScreenKeys.add(screenKey)

    /**
     * Starts a new visit of [visitId] (e.g. a trip being opened from Ledgers): every screen key
     * ending in `:visitId` may play its entrance again.
     */
    @Synchronized
    fun beginVisit(visitId: String) {
        if (visitId.isBlank()) return
        playedScreenKeys.removeAll { it.endsWith(":$visitId") }
    }

    @Synchronized
    internal fun resetForTest() = playedScreenKeys.clear()
}

/**
 * Returns the uptime at which [screenKey]'s entrance started, or null when it already played in
 * this visit (or reduced motion is on).
 */
@Composable
fun rememberFirstOpenEntrance(screenKey: String): Long? {
    val reducedMotion = rememberReducedMotionEnabled()
    return remember(screenKey) {
        if (!reducedMotion && ExpressiveEntranceRegistry.claim(screenKey)) SystemClock.uptimeMillis() else null
    }
}

/** Pure rule: whether item [index] should animate, given when the entrance started. */
fun shouldPlayStaggeredEntrance(entranceStartedAtMillis: Long?, index: Int, nowMillis: Long): Boolean =
    entranceStartedAtMillis != null &&
        index in 0 until ExpressiveEntranceDefaults.MaxStaggeredItems &&
        nowMillis - entranceStartedAtMillis in 0..ExpressiveEntranceDefaults.EntranceWindowMillis

/**
 * Rise + fade + slight grow entrance for list item [index], driven by the theme's spatial spring (so
 * it snaps when system animations are off). [entranceStartedAtMillis] comes from
 * [rememberFirstOpenEntrance]. When the screen is entering through the group container transform
 * ([LocalGroupNavAnimatedScope]), the items wait for that transition to finish first.
 */
fun Modifier.staggeredEntrance(index: Int, entranceStartedAtMillis: Long?): Modifier = composed {
    val shouldAnimate = remember {
        shouldPlayStaggeredEntrance(entranceStartedAtMillis, index, SystemClock.uptimeMillis())
    }
    if (!shouldAnimate) return@composed this
    val progress = remember { Animatable(0f) }
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val enteringScope = LocalGroupNavAnimatedScope.current
    LaunchedEffect(Unit) {
        if (enteringScope != null) {
            snapshotFlow { enteringScope.transition.isRunning }.first { running -> !running }
        }
        delay(index * ExpressiveEntranceDefaults.StaggerMillis)
        progress.animateTo(1f, spatialSpec)
    }
    val offsetPx = with(LocalDensity.current) { ExpressiveEntranceDefaults.InitialOffset.toPx() }
    this.graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * offsetPx
        val s = ExpressiveEntranceDefaults.InitialScale + (1f - ExpressiveEntranceDefaults.InitialScale) * p
        scaleX = s
        scaleY = s
    }
}
