package com.splitmate.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** Scale applied while a card is pressed (M3 Expressive "squish" feedback). */
const val EXPRESSIVE_PRESS_SCALE: Float = 0.97f

/**
 * v2.3.6: M3 Expressive press feedback for whole cards.
 *
 * Observes the pointer without consuming it (Initial/Final passes), so inner click targets,
 * expanders and swipe actions keep working. The press cancels when a parent scroll consumes the
 * gesture. The scale follows `MaterialTheme.motionScheme.fastSpatialSpec()`, which snaps under
 * reduced motion (`SplitMateReducedMaterialMotionScheme`).
 */
fun Modifier.expressivePressScale(pressedScale: Float = EXPRESSIVE_PRESS_SCALE): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    LaunchedEffect(pressed) {
        scale.animateTo(if (pressed) pressedScale else 1f, spec)
    }
    this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pressed = true
                waitForUpOrCancellation(pass = PointerEventPass.Final)
                pressed = false
            }
        }
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
}
