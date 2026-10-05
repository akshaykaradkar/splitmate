package com.splitmate.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * v2.3.6 Step C: a full-window "Reading your ticket…" state while a flight PDF is parsed.
 *
 * Uses the real material3 1.5 `ContainedLoadingIndicator` (a 7-shape morph). This is a short local
 * wait, not money data, so a loading indicator is right here: the wavy bar stays reserved for
 * network jobs. Under reduced motion the local indicator, which freezes on its first shape, is
 * used instead. Touches are blocked so nothing can be tapped while the parse is running.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FlightPdfParsingOverlay(visible: Boolean, label: String = "Reading your ticket…") {
    val reducedMotion = rememberReducedMotionEnabled()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier
                    .animateEnterExit(enter = scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = 0.85f))
                    .semantics {
                        contentDescription = label
                        liveRegion = LiveRegionMode.Polite
                    }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (reducedMotion) {
                        ContainedLoadingIndicator(containerSize = 64.dp)
                    } else {
                        androidx.compose.material3.ContainedLoadingIndicator(modifier = Modifier.size(64.dp))
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
