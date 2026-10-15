package com.splitmate.app.ui.components.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.splitmate.app.ui.components.LocalMotionScheme
import com.splitmate.app.ui.components.SymbolIcon
import com.splitmate.app.ui.components.VerticalFloatingToolbar
import com.splitmate.app.ui.components.rememberReducedMotionEnabled

/** The stop shown in the card when a marker (or a list stop) is selected. */
data class LoopMapSelection(val id: String, val index: Int, val name: String, val kindLabel: String)

/**
 * v2.4.0 rc15: the Loop map pane (mapcn-style): the map, an edge-docked M3 Expressive vertical
 * floating toolbar (+ / − / Fit loop), the always-visible OpenStreetMap credit, and a borderless
 * card for the selected stop. Offline or on a map error it shows a calm card instead; the stop
 * list below keeps working either way.
 */
@Composable
fun LoopMapPane(
    model: LoopMapModel,
    offline: Boolean,
    selection: LoopMapSelection?,
    onSelectStop: (String?) -> Unit,
    onOpenDetails: (String) -> Unit,
    controller: LoopMapController,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotionEnabled()
    val motion = LocalMotionScheme.current
    var failed by remember { mutableStateOf(false) }
    val a11y = "Map of the loop, ${model.points.count { !it.isStay }} stops. The list below has every stop."

    Box(modifier = modifier.semantics { contentDescription = a11y }) {
        if (offline || failed) {
            MapUnavailableCard(offline = offline, modifier = Modifier.fillMaxSize())
            return@Box
        }
        LoopMapCanvas(
            model = model,
            controller = controller,
            reducedMotion = reducedMotion,
            onStopTap = { id ->
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onSelectStop(if (id == LoopMapStyle.STAY_ID) null else id)
            },
            onError = { failed = true },
            modifier = Modifier.fillMaxSize()
        )
        VerticalFloatingToolbar(
            expanded = true,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            MapToolButton(Icons.Rounded.Add, "Zoom in") { controller.zoomIn() }
            MapToolButton(Icons.Rounded.Remove, "Zoom out") { controller.zoomOut() }
            MapToolButton(Icons.Rounded.FitScreen, "Fit the whole loop") {
                onSelectStop(null)
                controller.fitLoop()
            }
        }
        MapAttributionChip(modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
        AnimatedVisibility(
            visible = selection != null,
            enter = slideInVertically(motion.defaultSpatialSpec()) { it / 2 } + fadeIn(motion.defaultEffectsSpec()),
            exit = slideOutVertically(motion.fastSpatialSpec()) { it / 2 } + fadeOut(motion.fastEffectsSpec()),
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 76.dp, bottom = 12.dp)
        ) {
            selection?.let { MapStopCard(it, onOpenDetails = { onOpenDetails(it.id) }) }
        }
    }
}

@Composable
private fun MapToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onClick()
        },
        shapes = IconButtonDefaults.shapes(),
        modifier = Modifier.semantics { contentDescription = label }
    ) { SymbolIcon(icon, contentDescription = null) }
}

/** OSMF attribution: always visible, links to the OpenStreetMap copyright page. */
@Composable
fun MapAttributionChip(modifier: Modifier = Modifier) {
    val uri = LocalUriHandler.current
    Surface(
        onClick = { runCatching { uri.openUri(LoopMapStyle.ATTRIBUTION_URL) } },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f),
        modifier = modifier.heightIn(min = 32.dp)
    ) {
        Text(
            text = LoopMapStyle.ATTRIBUTION,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

/** Borderless card for the selected stop: number, name, kind, and "Details". */
@Composable
private fun MapStopCard(selection: LoopMapSelection, onOpenDetails: () -> Unit) {
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = selection.index.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = selection.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = selection.kindLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onOpenDetails, shapes = ButtonDefaults.shapes(), modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Details", style = MaterialTheme.typography.labelLarge)
                SymbolIcon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Offline / map error: a calm tonal card; the list below still has every stop. */
@Composable
private fun MapUnavailableCard(offline: Boolean, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SymbolIcon(Icons.Rounded.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.heightIn(min = 8.dp))
            Text(
                text = if (offline) "The map needs internet for now" else "The map couldn't load",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (offline) "Saving maps offline is coming. Your stops are listed below." else "Your stops are listed below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
