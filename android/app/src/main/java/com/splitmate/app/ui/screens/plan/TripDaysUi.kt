@file:OptIn(ExperimentalMaterial3Api::class)

package com.splitmate.app.ui.screens.plan

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.TripDayRange
import com.splitmate.app.data.guide.TripDays
import com.splitmate.app.ui.components.SymbolIcon

// =================================================================================================
// v2.4.0 rc14: per-day loops. Trip dates chip + picker, the Loop's day picker, "Move to day…",
// and the "Not planned yet" tray. Render-only: callers pass state and callbacks.
// =================================================================================================

/** "15–18 Oct · 4 days" chip on Days; opens the trip-dates picker. Shows "Set trip dates" when unknown. */
@Composable
fun TripDatesChip(range: TripDayRange?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = if (range == null) "Set trip dates" else {
        val n = range.days.size
        "${TripDays.rangeLabel(range)} · ${if (n == 1) "1 day" else "$n days"}"
    }
    ElevatedAssistChip(
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = { SymbolIcon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
        trailingIcon = { SymbolIcon(Icons.Rounded.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp)) },
        colors = AssistChipDefaults.elevatedAssistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = AssistChipDefaults.elevatedAssistChipElevation(elevation = 1.dp),
        modifier = modifier.semantics { contentDescription = "Trip dates: $label. Change" }
    )
}

/**
 * Trip dates as the full-screen M3 date-range picker (Material's pattern for range selection on
 * phones): close, title, Save in the top bar, the scrolling range calendar below. [onConfirm] gets
 * local start-of-first-day and end-of-last-day epoch ms (what the synced trip record stores); the
 * picker itself works in UTC days and is converted here.
 */
@Composable
fun TripDatesPickerDialog(range: TripDayRange?, onConfirm: (startMs: Long, endMs: Long) -> Unit, onDismiss: () -> Unit) {
    val dayMs = 86_400_000L
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = range?.startEpochDay?.times(dayMs),
        initialSelectedEndDateMillis = range?.endEpochDay?.times(dayMs)
    )
    val start = state.selectedStartDateMillis
    val end = state.selectedEndDateMillis ?: start
    val tooLong = start != null && end != null && (end - start) / dayMs + 1 > TripDays.MAX_DAYS
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, shapes = IconButtonDefaults.shapes()) {
                        SymbolIcon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                    Text(
                        text = "Trip dates",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f).semantics { heading() }
                    )
                    TextButton(
                        onClick = {
                            if (start != null && end != null && !tooLong) {
                                onConfirm(TripDays.startOfDayMs(start / dayMs), TripDays.startOfDayMs(end / dayMs + 1) - 1)
                            }
                        },
                        enabled = start != null && !tooLong,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Save", style = MaterialTheme.typography.labelLarge) }
                }
                DateRangePicker(
                    state = state,
                    title = null,
                    headline = {
                        Text(
                            text = if (tooLong) "Trips can be up to ${TripDays.MAX_DAYS} days" else "Pick the first and last day",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (tooLong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp)
                        )
                    },
                    showModeToggle = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * The Loop's day picker: "All days", then "Day 1 · Thu 15", … as M3 Expressive [ToggleButton]s
 * (round -> square shape morph on selection, single-select semantics), scrolled to the chosen day.
 */
@Composable
fun LoopDayPicker(range: TripDayRange, selectedDay: Int?, onSelect: (Int?) -> Unit, modifier: Modifier = Modifier) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reducedMotion = com.splitmate.app.ui.components.rememberReducedMotionEnabled()
    androidx.compose.runtime.LaunchedEffect(selectedDay) {
        // Index 0 is "All days"; day N sits at index N. Keep the previous chip in view for context.
        val target = selectedDay ?: 0
        if (target > 0) {
            val index = (target - 1).coerceAtLeast(0)
            // "Remove animations" on: jump instead of scrolling.
            if (reducedMotion) listState.scrollToItem(index) else listState.animateScrollToItem(index)
        }
    }
    // Unselected days sit on a tonal container so they still read as buttons on the white canvas.
    val colors = ToggleButtonDefaults.toggleButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
    val pick: (Int?) -> Unit = { day ->
        if (day != selectedDay) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.SegmentTick)
            onSelect(day)
        }
    }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth().semantics { selectableGroup() },
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        item(key = "all") {
            ToggleButton(
                checked = selectedDay == null,
                onCheckedChange = { pick(null) },
                shapes = ToggleButtonDefaults.shapes(),
                colors = colors,
                modifier = Modifier.heightIn(min = 48.dp).semantics { role = Role.RadioButton }
            ) { Text("All days", style = MaterialTheme.typography.labelLarge) }
        }
        itemsIndexed(range.days, key = { _, d -> "day_${d.number}" }) { _, d ->
            ToggleButton(
                checked = selectedDay == d.number,
                onCheckedChange = { pick(d.number) },
                shapes = ToggleButtonDefaults.shapes(),
                colors = colors,
                modifier = Modifier.heightIn(min = 48.dp).semantics { role = Role.RadioButton }
            ) { Text("Day ${d.number} · ${TripDays.shortLabel(d.epochDay)}", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/** Title for the Loop: "Day 2 loop from your stay" / "Your loop from your stay". */
fun loopTitle(day: Int?): String = if (day == null) "Your loop from your stay" else "Day $day loop from your stay"

/**
 * "Move to day…" for a stop: a shape-morphing IconButton opening an M3 Expressive menu with two
 * groups, the trip days (current day shown selected with a check) and "Not planned yet".
 */
@Composable
fun MoveToDayButton(
    placeName: String,
    range: TripDayRange,
    currentDay: Int?,
    onMove: (Int?) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val move: (Int?) -> Unit = { day ->
        open = false
        if (day != currentDay) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.SegmentTick)
            onMove(day)
        }
    }
    Box {
        IconButton(
            onClick = { open = true },
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.semantics { contentDescription = "Move $placeName to another day" }
        ) {
            SymbolIcon(Icons.Rounded.EditCalendar, contentDescription = null)
        }
        DropdownMenuPopup(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 2)) {
                range.days.forEachIndexed { i, d ->
                    DropdownMenuItem(
                        selected = d.number == currentDay,
                        onClick = { move(d.number) },
                        text = { Text("Day ${d.number} · ${TripDays.shortLabel(d.epochDay)}") },
                        shapes = MenuDefaults.itemShape(i, range.days.size),
                        selectedLeadingIcon = { SymbolIcon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                }
            }
            Spacer(modifier = Modifier.heightIn(min = MenuDefaults.GroupSpacing))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(1, 2)) {
                DropdownMenuItem(
                    selected = currentDay == null,
                    onClick = { move(null) },
                    text = { Text("Not planned yet") },
                    shapes = MenuDefaults.itemShape(0, 1),
                    leadingIcon = { SymbolIcon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
            }
        }
    }
}

/**
 * "Not planned yet": places added to the loop without a day. They stay out of the route until
 * placed; one tap puts a place on the day being viewed.
 */
@Composable
fun UnplannedTray(
    places: List<Place>,
    day: Int,
    onAddToDay: (Place) -> Unit,
    onOpen: (Place) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = PlanGuideDefaults.CardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = PlanGuideDefaults.CardElevation)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SymbolIcon(Icons.Rounded.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Not planned yet (${places.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
            }
            Text(
                text = "Added to the loop without a day. Put them on a day to route them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // M3 Expressive segmented list: one connected group, outer corners rounded. The group
            // resizes on the spatial spring when a place moves out to a day.
            val motion = com.splitmate.app.ui.components.LocalMotionScheme.current
            Column(
                modifier = Modifier.animateContentSize(motion.defaultSpatialSpec()),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                places.forEachIndexed { index, place ->
                    val name = PlanGuideFormat.sanitizeDisplay(place.name) ?: place.name
                    SegmentedListItem(
                        onClick = { onOpen(place) },
                        shapes = ListItemDefaults.segmentedShapes(index = index, count = places.size),
                        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        content = {
                            Text(text = name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        trailingContent = {
                            FilledTonalButton(
                                onClick = { onAddToDay(place) },
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.heightIn(min = 40.dp).semantics { contentDescription = "Add $name to day $day" }
                            ) { Text("Add to Day $day", style = MaterialTheme.typography.labelLarge) }
                        }
                    )
                }
            }
        }
    }
}
