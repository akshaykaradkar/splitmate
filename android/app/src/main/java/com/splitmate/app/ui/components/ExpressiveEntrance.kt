package com.splitmate.app.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * v2.3.6 Step C: first-open list entrance. The first few cards of a screen rise and fade in one
 * after another the first time that screen is opened in this app session; reopening it (or
 * scrolling an item back into view) never replays the entrance.
 */
object ExpressiveEntranceDefaults {
    /** Delay between consecutive items. */
    const val StaggerMillis: Long = 40L

    /** Only the first items stagger; the rest are below the fold on open anyway. */
    const val MaxStaggeredItems: Int = 6

    /** Items composed later than this after the screen opened appear without an entrance. */
    const val EntranceWindowMillis: Long = 1_200L

    /** Distance each item rises while fading in. */
    val InitialOffset: Dp = 24.dp
}

/** Process-wide record of which screens already played their entrance this session. */
object ExpressiveEntranceRegistry {
    private val playedScreenKeys = mutableSetOf<String>()

    /** Returns true exactly once per [screenKey] per process. */
    @Synchronized
    fun claim(screenKey: String): Boolean = playedScreenKeys.add(screenKey)

    @Synchronized
    internal fun resetForTest() = playedScreenKeys.clear()
}

/**
 * Returns the uptime at which [screenKey]'s entrance started, or null when it already played
 * earlier in this session (or reduced motion is on).
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
 * Rise + fade entrance for list item [index], driven by the theme's spatial spring (so it snaps
 * when system animations are off). [entranceStartedAtMillis] comes from [rememberFirstOpenEntrance].
 */
fun Modifier.staggeredEntrance(index: Int, entranceStartedAtMillis: Long?): Modifier = composed {
    val shouldAnimate = remember {
        shouldPlayStaggeredEntrance(entranceStartedAtMillis, index, SystemClock.uptimeMillis())
    }
    if (!shouldAnimate) return@composed this
    val progress = remember { Animatable(0f) }
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    LaunchedEffect(Unit) {
        delay(index * ExpressiveEntranceDefaults.StaggerMillis)
        progress.animateTo(1f, spatialSpec)
    }
    val offsetPx = with(LocalDensity.current) { ExpressiveEntranceDefaults.InitialOffset.toPx() }
    this.graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * offsetPx
    }
}
