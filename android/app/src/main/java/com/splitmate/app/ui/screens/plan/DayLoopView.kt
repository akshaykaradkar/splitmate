@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.splitmate.app.ui.screens.plan

import androidx.compose.runtime.setValue
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import com.splitmate.app.ui.components.SymbolIcon
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.StayPin
import com.splitmate.app.ui.components.LocalMotionScheme

/**
 * Loop sub-view (audit 5.2 D; rc14 per day): "Day N loop from your stay" as a vertical stepper
 * Stay -> 1 -> 2 -> ... -> back to Stay, with straight-line leg distances, totals, caveats and the
 * hand-offs (whole loop in Google Maps, share). The route is recomputed on every device and is
 * never synced; this view only renders [LoopUi].
 */
@Composable
fun DayLoopView(
    state: TripGuideUiState,
    actions: TripGuideActions,
    onSetStay: () -> Unit,
    modifier: Modifier = Modifier,
    /** rc14: the trip's days (null until there are bookings or trip dates). */
    tripDays: com.splitmate.app.data.guide.TripDayRange? = null,
    /** rc14: put a place on a day (null = "Not planned yet"); the host also freezes the trip dates. */
    onMovePlaceToDay: (placeId: String, day: Int?) -> Unit = { id, d -> actions.movePlaceToDay(id, d) }
) {
    val loop = state.loop
    val stay = state.stay.stay
    val route = loop.route
    val stops = route?.orderedStops.orEmpty()
    val motion = LocalMotionScheme.current

    // rc15: mapcn-style map pane above the stop list (MapLibre GL JS + OpenFreeMap, themed from tokens).
    val showMap = state.mapEnabled && stay != null && route != null && stops.isNotEmpty()
    val mapController = remember { com.splitmate.app.ui.components.map.LoopMapController() }
    var selectedStopId by androidx.compose.runtime.saveable.rememberSaveable(loop.day) { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var mapExpanded by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(selectedStopId) { mapController.select(selectedStopId) }

    Column(modifier = modifier.fillMaxSize()) {
    if (showMap) {
        LoopMapSection(
            stay = stay!!,
            stops = stops,
            offline = state.isOffline,
            expanded = mapExpanded,
            onToggle = { mapExpanded = !mapExpanded },
            selectedStopId = selectedStopId,
            onSelectStop = { selectedStopId = it },
            onOpenDetails = { id -> stops.firstOrNull { it.id == id }?.let { actions.openPlace(it) } },
            controller = mapController
        )
    }
    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(
            start = PlanGuideDefaults.ScreenGutter,
            end = PlanGuideDefaults.ScreenGutter,
            top = 8.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        val offlineLabel = state.offlineSavedLabel
        if (state.isOffline && offlineLabel != null) {
            item(key = "offline") {
                OfflineStatusChip(
                    label = offlineLabel,
                    icon = Icons.Rounded.CloudOff,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                )
            }
        }
        item(key = "title") {
            // rc14: the title cross-fades on the effects spring when the day changes.
            androidx.compose.animation.AnimatedContent(
                targetState = loopTitle(if (tripDays != null) state.loopDay else null),
                transitionSpec = {
                    androidx.compose.animation.fadeIn(motion.defaultEffectsSpec()) togetherWith
                        androidx.compose.animation.fadeOut(motion.fastEffectsSpec())
                },
                label = "LoopDayTitle",
                modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
            ) { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .semantics { heading() }
                )
            }
        }
        if (tripDays != null) {
            // rc14: plan one day at a time; "All days" is the whole-trip view.
            item(key = "day_picker") {
                LoopDayPicker(
                    range = tripDays,
                    selectedDay = state.loopDay,
                    onSelect = { actions.selectLoopDay(it) },
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                )
            }
        }

        when {
            stay == null -> item(key = "no_stay") {
                NoStayCard(onSetStay = onSetStay, modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null))
            }
            route == null || stops.isEmpty() -> {
                item(key = "stay_only") {
                    StayStepCard(
                        stay = stay,
                        isReturn = false,
                        legKm = null,
                        isShared = state.stay.isShared,
                        onChange = onSetStay,
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                    )
                }
                item(key = "no_stops") {
                    EmptyLoopCard(
                        building = state.networkInFlight,
                        onExplore = { actions.openExploreForDay(if (tripDays != null) state.loopDay else null) },
                        modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                    )
                }
            }
            else -> {
                item(key = "totals") {
                    LoopTotals(
                        loop = loop,
                        stopCount = stops.size,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                    )
                }
                item(key = "stay_start") {
                    StayStepCard(
                        stay = stay,
                        isReturn = false,
                        legKm = null,
                        isShared = state.stay.isShared,
                        onChange = onSetStay,
                        modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                    )
                }
                stops.forEachIndexed { index, place ->
                    item(key = "leg_${place.id}") {
                        // rc14: switching days swaps stops: they fade on the effects spring and
                        // re-order on the spatial spring.
                        LegConnector(
                            km = route.legStraightKm.getOrNull(index),
                            modifier = Modifier.animateItem(fadeInSpec = motion.fastEffectsSpec(), placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = motion.fastEffectsSpec())
                        )
                    }
                    item(key = "stop_${place.id}") {
                        LoopStopCard(
                            index = index + 1,
                            total = stops.size,
                            place = place,
                            legKm = route.legStraightKm.getOrNull(index),
                            onOpen = {
                                if (showMap) selectedStopId = place.id
                                actions.openPlace(place)
                            },
                            moveTo = tripDays?.let { range ->
                                { MoveToDayButton(placeName = place.name, range = range, currentDay = loop.day) { d -> onMovePlaceToDay(place.id, d) } }
                            },
                            modifier = Modifier.animateItem(fadeInSpec = motion.fastEffectsSpec(), placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = motion.fastEffectsSpec())
                        )
                    }
                }
                item(key = "leg_return") {
                    LegConnector(
                        km = route.legStraightKm.getOrNull(stops.size),
                        modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                    )
                }
                item(key = "stay_end") {
                    StayStepCard(
                        stay = stay,
                        isReturn = true,
                        legKm = route.legStraightKm.getOrNull(stops.size),
                        isShared = state.stay.isShared,
                        onChange = null,
                        modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                    )
                }
                item(key = "loop_actions") {
                    LoopActions(
                        actions = actions,
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                    )
                }
            }
        }

        val trayDay = loop.day
        if (tripDays != null && loop.perDay && trayDay != null && loop.unplanned.isNotEmpty()) {
            item(key = "unplanned") {
                UnplannedTray(
                    places = loop.unplanned,
                    day = trayDay,
                    onAddToDay = { onMovePlaceToDay(it.id, trayDay) },
                    onOpen = { actions.openPlace(it) },
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .animateItem(fadeInSpec = motion.fastEffectsSpec(), placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = motion.fastEffectsSpec())
                )
            }
        }

        if (loop.stopsWithoutCoordinates.isNotEmpty()) {
            item(key = "no_coordinates") {
                val names = loop.stopsWithoutCoordinates
                    .mapNotNull { PlanGuideFormat.sanitizeDisplay(it.name) }
                    .joinToString(", ")
                PlanInlineBanner(
                    message = "Not in the loop because they have no map location: $names",
                    tone = PlanBannerTone.NEUTRAL,
                    icon = Icons.Rounded.LocationOff,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .animateItem(fadeInSpec = null, placementSpec = motion.defaultSpatialSpec(), fadeOutSpec = null)
                )
            }
        }
    }
    }
}

/**
 * rc15: the collapsible map section. Expanded: the map pane (about 40% of a phone screen).
 * Collapsed: one slim "Show map" row, so the list gets the whole height.
 */
@Composable
private fun LoopMapSection(
    stay: StayPin,
    stops: List<Place>,
    offline: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    selectedStopId: String?,
    onSelectStop: (String?) -> Unit,
    onOpenDetails: (String) -> Unit,
    controller: com.splitmate.app.ui.components.map.LoopMapController
) {
    val palette = com.splitmate.app.ui.DesignSystemBindings.activePalette
    val stayLabel = PlanGuideFormat.sanitizeDisplay(stay.label) ?: "Your stay"
    val model = remember(palette, stay, stops) {
        val colors = com.splitmate.app.ui.theme.MapExtendedColors.of(palette)
        val style = com.splitmate.app.ui.components.map.LoopMapStyle
        com.splitmate.app.ui.components.map.LoopMapModel(
            styleJson = style.style(colors),
            overlayJson = style.overlay(colors),
            dataJson = style.data(stay.location, stayLabel, stops),
            points = listOf(com.splitmate.app.ui.components.map.LoopMapPoint(style.STAY_ID, 0, stay.location.lat, stay.location.lng, isStay = true)) +
                stops.filter { it.location != null }.mapIndexed { i, p ->
                    com.splitmate.app.ui.components.map.LoopMapPoint(p.id, i + 1, p.location!!.lat, p.location.lng, isStay = false)
                }
        )
    }
    val selection = selectedStopId?.let { id ->
        val i = stops.indexOfFirst { it.id == id }
        stops.getOrNull(i)?.let { p ->
            com.splitmate.app.ui.components.map.LoopMapSelection(
                id = p.id, index = i + 1,
                name = PlanGuideFormat.sanitizeDisplay(p.name) ?: p.name,
                kindLabel = PlanGuideFormat.kindLabel(p.kind)
            )
        }
    }
    val motion = LocalMotionScheme.current
    Column(modifier = Modifier.fillMaxWidth().animateContentSize(motion.defaultSpatialSpec())) {
        if (expanded) {
            com.splitmate.app.ui.components.map.LoopMapPane(
                model = model,
                offline = offline,
                selection = selection,
                onSelectStop = onSelectStop,
                onOpenDetails = onOpenDetails,
                controller = controller,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MAP_PANE_HEIGHT)
            )
        }
        TextButton(
            onClick = onToggle,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .heightIn(min = 48.dp)
        ) {
            SymbolIcon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (expanded) "Hide map" else "Show map", style = MaterialTheme.typography.labelLarge)
        }
    }
}

private val MAP_PANE_HEIGHT = 300.dp

@Composable
private fun LoopTotals(loop: LoopUi, stopCount: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = PlanGuideFormat.sanitizeDisplay(loop.totalLabel) ?: "$stopCount stops",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (loop.autoSuggested) {
            Text(
                text = "Suggested from the guide's top places. Add places to your loop in Explore to make it yours.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (loop.truncated) {
            Text(
                text = "Showing the first $stopCount stops. Remove a few to fit the rest.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        loop.caveats.mapNotNull { PlanGuideFormat.sanitizeDisplay(it) }.forEach { caveat ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                SymbolIcon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(16.dp)
                )
                Text(
                    text = caveat,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Stay endpoint (start, or "Back to your stay") in the sage tone. */
@Composable
private fun StayStepCard(
    stay: StayPin,
    isReturn: Boolean,
    legKm: Double?,
    isShared: Boolean,
    onChange: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val label = PlanGuideFormat.sanitizeDisplay(stay.label) ?: "Your stay"
    val a11y = if (isReturn) PlanGuideFormat.returnA11yLabel(legKm) else "Start at your stay: $label"
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepBadge(icon = Icons.Rounded.Hotel)
        // rc13 (audit L-3): Loop cards are borderless ElevatedCards (GEMINI.md v2.3.4 addendum).
        ElevatedCard(
            modifier = Modifier.weight(1f),
            shape = PlanGuideDefaults.CardShape,
            // White card role like every guide card; the hotel badge beside it marks the stay.
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = PlanGuideDefaults.CardElevation)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { contentDescription = a11y },
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isReturn) "Back to your stay" else "Your stay",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isReturn && isShared) {
                        Text("Shared by a trip member", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (onChange != null) {
                    TextButton(
                        onClick = onChange,
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Change", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** One stop: numbered badge plus a tonal card; tapping opens the place sheet. */
@Composable
private fun LoopStopCard(
    index: Int,
    total: Int,
    place: Place,
    legKm: Double?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    /** rc14: trailing "Move to day…" control (null when the trip has no days yet). */
    moveTo: (@Composable () -> Unit)? = null
) {
    val name = PlanGuideFormat.sanitizeDisplay(place.name) ?: place.name
    val a11y = PlanGuideFormat.stepA11yLabel(index, total, name, place.kind, legKm)
    val interaction = remember { MutableInteractionSource() }
    val scale by rememberPressScale(interaction)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepBadge(number = index)
        ElevatedCard(
            onClick = onOpen,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = PlanGuideDefaults.CardShape,
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = PlanGuideDefaults.CardElevation,
                pressedElevation = PlanGuideDefaults.CardPressedElevation
            ),
            interactionSource = interaction
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { contentDescription = a11y },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    PlaceKindChip(kind = place.kind)
                }
                place.image?.let { PlaceThumbnail(image = it, size = 56.dp) }
                moveTo?.invoke()
            }
        }
    }
}

/** Decorative connector between steps with the straight-line leg distance (read via the stop label). */
@Composable
private fun LegConnector(km: Double?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .planDecorative(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.width(STEP_BADGE_SIZE), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(32.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
        if (km != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SymbolIcon(
                    imageVector = Icons.AutoMirrored.Rounded.DirectionsWalk,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = PlanGuideFormat.formatKm(km),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val STEP_BADGE_SIZE = 36.dp

@Composable
private fun StepBadge(number: Int? = null, icon: ImageVector? = null) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (icon != null) scheme.primaryContainer else scheme.primary,
        contentColor = if (icon != null) scheme.onPrimaryContainer else scheme.onPrimary,
        modifier = Modifier
            .widthIn(min = STEP_BADGE_SIZE)
            .heightIn(min = STEP_BADGE_SIZE)
            .planDecorative()
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
            if (icon != null) {
                SymbolIcon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            } else {
                Text(
                    text = number?.toString().orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** One primary action (Google Maps); Reoptimise and Share sit beside it as tonal actions. */
@Composable
private fun LoopActions(actions: TripGuideActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { actions.openWholeLoopInMaps() },
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            SymbolIcon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Open whole loop in Google Maps", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tonal = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
            FilledTonalButton(onClick = { actions.reoptimizeLoop() }, shapes = ButtonDefaults.shapes(), colors = tonal, modifier = Modifier.heightIn(min = 48.dp)) {
                SymbolIcon(Icons.Rounded.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reoptimise", style = MaterialTheme.typography.labelLarge)
            }
            FilledTonalButton(onClick = { actions.shareLoop() }, shapes = ButtonDefaults.shapes(), colors = tonal, modifier = Modifier.heightIn(min = 48.dp)) {
                SymbolIcon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share loop", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun NoStayCard(onSetStay: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = PlanGuideDefaults.CardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = PlanGuideDefaults.CardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                SymbolIcon(
                    imageVector = Icons.Rounded.Hotel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(28.dp)
                )
            }
            Text(
                text = "Set your stay",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Your loop starts and ends where you sleep. Paste a Google Maps link, pick a " +
                    "'Sleep' listing from the guide, or enter coordinates.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onSetStay, shapes = ButtonDefaults.shapes(), modifier = Modifier.heightIn(min = 48.dp)) {
                SymbolIcon(Icons.Rounded.Hotel, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Set your stay", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyLoopCard(building: Boolean, onExplore: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = PlanGuideDefaults.CardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = PlanGuideDefaults.CardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (building) "Building your loop" else "No stops yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Tap 'Add to loop' on places in Explore. We'll order them into a short round trip from your stay.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(onClick = onExplore, shapes = ButtonDefaults.shapes(), modifier = Modifier.heightIn(min = 48.dp)) {
                SymbolIcon(Icons.Rounded.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Go to Explore", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
