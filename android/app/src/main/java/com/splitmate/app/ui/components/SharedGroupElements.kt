package com.splitmate.app.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * v2.3.6 Step C: the group's identity carries over between Ledgers home and its Trip Hub.
 *
 * The Ledgers -> Trip Hub switch is an `AnimatedContent`. Its scope is published here, and the
 * dashboard-wide `SharedTransitionLayout` from [TicketContainerTransformHost] supplies the shared
 * scope. Elements tagged with the same [SharedGroupKeys] key then fly and scale between the two
 * screens (the group name and the "you get back / you owe" figure). When either scope is missing
 * (two-pane tablet layout, previews) or reduced motion is on, the modifier is a no-op.
 */
val LocalGroupNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

object SharedGroupKeys {
    fun title(groupId: String) = "group-title-$groupId"
    fun net(groupId: String) = "group-net-$groupId"
}

object SharedGroupElementDefaults {
    const val DampingRatio = Spring.DampingRatioLowBouncy
    const val Stiffness = Spring.StiffnessMediumLow
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedGroupElement(key: String, alignment: Alignment = Alignment.CenterStart): Modifier {
    val shared = LocalTicketContainerTransform.current?.sharedTransitionScope ?: return this
    val animated = LocalGroupNavAnimatedScope.current ?: return this
    if (rememberReducedMotionEnabled()) return this
    return with(shared) {
        this@sharedGroupElement.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animated,
            boundsTransform = { _, _ ->
                spring(
                    dampingRatio = SharedGroupElementDefaults.DampingRatio,
                    stiffness = SharedGroupElementDefaults.Stiffness
                )
            },
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.FillHeight, alignment)
        )
    }
}
