@file:OptIn(ExperimentalMaterial3Api::class)

package com.splitmate.app.ui.screens.plan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.Place
import com.splitmate.app.data.guide.PlaceKind
import com.splitmate.app.data.guide.PlaceSource
import com.splitmate.app.data.guide.loop.NominatimGeocoder
import com.splitmate.app.ui.components.InFlightWavyProgressIndicator
import com.splitmate.app.ui.components.LocalMotionScheme

/** Input modes of the stay sheet (audit 5.2 E). */
private enum class StayTab(val label: String) {
    LINK("Paste link"),
    SLEEP("From guide's 'Sleep'"),
    COORDINATES("Coordinates")
}

/**
 * Set-stay sheet: paste a Maps link, pick a Wikivoyage "Sleep" listing, or type coordinates.
 * A preview line confirms "lat, lng · Exact/Approximate" before anything is saved. The
 * Nominatim fallback runs when the user taps "Search by name" (decision #12) or, since v2.3.5
 * (#3), automatically once when a Google link resolves to a place name without coordinates; its
 * result is always an Approximate preview the user confirms and carries the OpenStreetMap credit.
 * Sharing the stay with the group is OFF by default (decision #14).
 */
@Composable
fun StayPinSheet(
    state: TripGuideUiState,
    actions: TripGuideActions,
    onDismiss: () -> Unit
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val motion = LocalMotionScheme.current
    val stayUi = state.stay
    val feedback = state.stayFeedback
    var tab by rememberSaveable { mutableIntStateOf(StayTab.LINK.ordinal) }
    var searchedByName by rememberSaveable { mutableStateOf(false) }
    var awaitingNewStay by rememberSaveable { mutableStateOf(false) }
    val stayAtOpen = remember { stayUi.stay }

    val destinationName = remember(state.pack) {
        val dest = state.pack?.destination
        if (dest != null && dest.location != null) dest.label else null
    }

    // Close once a confirmed or chosen stay lands in state.
    LaunchedEffect(stayUi.stay, awaitingNewStay) {
        val current = stayUi.stay
        if (awaitingNewStay && current != null && current != stayAtOpen) currentOnDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = { currentOnDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        BackHandler { currentOnDismiss() }
        InFlightWavyProgressIndicator(
            inFlight = state.networkInFlight,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentDescription = "Finding location"
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding()
                .animateContentSize(motion.defaultSpatialSpec()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Set your stay",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "Your day loop starts and ends here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { currentOnDismiss() }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            CurrentStay(state = state, actions = actions)

            TabRow(
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                StayTab.entries.forEach { t ->
                    Tab(
                        selected = tab == t.ordinal,
                        onClick = { tab = t.ordinal },
                        modifier = Modifier.heightIn(min = 48.dp),
                        text = {
                            Text(
                                text = t.label,
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            when (StayTab.entries[tab]) {
                StayTab.LINK -> PasteLinkTab(
                    working = feedback == StayInputFeedback.Working,
                    onSubmit = { text ->
                        searchedByName = false
                        actions.submitStayInput(text)
                    }
                )
                StayTab.SLEEP -> SleepListingsTab(
                    listings = stayUi.sleepListings,
                    destinationName = destinationName,
                    onChoose = { place ->
                        searchedByName = false
                        awaitingNewStay = true
                        actions.chooseStay(TripGuideActions.StaySelection.SleepListing(place))
                    },
                    onUseDestinationCenter = {
                        searchedByName = false
                        actions.chooseStay(TripGuideActions.StaySelection.DestinationCenter)
                    }
                )
                StayTab.COORDINATES -> CoordinatesTab(
                    working = feedback == StayInputFeedback.Working,
                    onSubmit = { text ->
                        searchedByName = false
                        actions.submitStayInput(text)
                    }
                )
            }

            StayFeedbackPanel(
                feedback = feedback,
                showOsmCredit = searchedByName || stayUi.attribution != null,
                destinationName = destinationName,
                onSearchByName = { hint ->
                    searchedByName = true
                    actions.searchStayByName(hint)
                },
                onUseDestinationCenter = {
                    searchedByName = false
                    actions.chooseStay(TripGuideActions.StaySelection.DestinationCenter)
                },
                onUseSleep = { tab = StayTab.SLEEP.ordinal },
                onUseCoordinates = { tab = StayTab.COORDINATES.ordinal },
                onConfirm = {
                    awaitingNewStay = true
                    actions.confirmStayPreview()
                }
            )

            ShareStayToggle(
                checked = stayUi.shareWithGroup,
                onCheckedChange = { actions.setShareStay(it) }
            )
        }
    }
}

@Composable
private fun CurrentStay(state: TripGuideUiState, actions: TripGuideActions) {
    val stay = state.stay.stay ?: return
    val label = PlanGuideFormat.sanitizeDisplay(stay.label) ?: "Your stay"
    Surface(
        shape = PlanGuideDefaults.CardShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.Hotel, contentDescription = null, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = PlanGuideFormat.stayPreviewLine(stay.location, stay.precision == CoordinatePrecision.APPROXIMATE),
                    style = MaterialTheme.typography.bodySmall
                )
                if (state.stay.isShared) {
                    Text("Shared by a trip member", style = MaterialTheme.typography.bodySmall)
                }
                PlanGuideFormat.sanitizeDisplay(state.stay.attribution)?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall)
                }
            }
            TextButton(
                onClick = { actions.clearStay() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Clear stay", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PasteLinkTab(working: Boolean, onSubmit: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val submit = {
        val value = text.trim()
        if (value.isNotEmpty()) {
            focusManager.clearFocus()
            onSubmit(value)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(MAX_INPUT_CHARS) },
            label = { Text("Google Maps link or place") },
            placeholder = { Text("maps.app.goo.gl/... or hotel / area name") },
            supportingText = {
                Text("Paste a Google Maps share link, or type your hotel or area name")
            },
            trailingIcon = {
                IconButton(onClick = { clipboard.getText()?.text?.let { text = it.take(MAX_INPUT_CHARS) } }) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste from clipboard")
                }
            },
            maxLines = 3,
            shape = RoundedCornerShape(20.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = submit,
            enabled = text.isNotBlank() && !working,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text("Find location", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CoordinatesTab(working: Boolean, onSubmit: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val parsed = remember(text) { PlanGuideFormat.parseCoordinates(text) }
    val invalid = text.isNotBlank() && parsed == null
    val focusManager = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(64) },
            label = { Text("Latitude, longitude") },
            placeholder = { Text("15.3350, 76.4600") },
            isError = invalid,
            supportingText = {
                Text(
                    if (invalid) "Use decimal degrees, e.g. 15.3350, 76.4600" else "Decimal degrees, latitude first"
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (parsed != null) {
                    focusManager.clearFocus()
                    onSubmit(text.trim())
                }
            }),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                focusManager.clearFocus()
                onSubmit(text.trim())
            },
            enabled = parsed != null && !working,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text("Use coordinates", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SleepListingsTab(
    listings: List<Place>,
    destinationName: String?,
    onChoose: (Place) -> Unit,
    onUseDestinationCenter: () -> Unit
) {
    if (listings.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "This guide has no curated 'Sleep' listings yet. Type your hotel or area name in the first tab, or start from the destination center below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (destinationName != null) {
                val destStr = PlanGuideFormat.sanitizeDisplay(destinationName) ?: "Destination"
                FilledTonalButton(
                    onClick = onUseDestinationCenter,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Use $destStr center", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listings.forEach { place ->
            val name = PlanGuideFormat.sanitizeDisplay(place.name) ?: return@forEach
            val detail = PlanGuideFormat.sanitizeDisplay(place.address)
                ?: PlanGuideFormat.sanitizeDisplay(place.blurb)?.let { PlanGuideFormat.truncate(it, 80) }
            val usable = place.location != null
            Surface(
                onClick = { onChoose(place) },
                enabled = usable,
                shape = PlanGuideDefaults.CardShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Hotel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = if (usable) detail ?: "Wikivoyage listing" else "No map location",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (usable) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StayFeedbackPanel(
    feedback: StayInputFeedback,
    showOsmCredit: Boolean,
    destinationName: String?,
    onSearchByName: (String) -> Unit,
    onUseDestinationCenter: () -> Unit,
    onUseSleep: () -> Unit,
    onUseCoordinates: () -> Unit,
    onConfirm: () -> Unit
) {
    when (feedback) {
        StayInputFeedback.Idle -> Unit
        StayInputFeedback.Working -> Text(
            text = "Finding location...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        is StayInputFeedback.Preview -> Surface(
            shape = PlanGuideDefaults.CardShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = PlanGuideFormat.sanitizeDisplay(feedback.label) ?: "Location found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = PlanGuideFormat.stayPreviewLine(feedback.location, feedback.approximate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                if (showOsmCredit) {
                    Text(
                        text = NominatimGeocoder.ATTRIBUTION,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = onConfirm, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Use this stay", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
        is StayInputFeedback.NeedsFallback -> Surface(
            shape = PlanGuideDefaults.CardShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.LinkOff, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        text = if (feedback.placeNameHint != null) {
                            "Google didn't share a map pin for this place."
                        } else {
                            "That link doesn't include a map pin."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                val hint = PlanGuideFormat.sanitizeDisplay(feedback.placeNameHint)
                InlineStayNameSearch(
                    destinationName = destinationName,
                    initialQuery = hint.orEmpty(),
                    onSearchByName = onSearchByName
                )
                if (destinationName != null) {
                    val destStr = PlanGuideFormat.sanitizeDisplay(destinationName) ?: "Destination"
                    FilledTonalButton(
                        onClick = onUseDestinationCenter,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Use $destStr center", style = MaterialTheme.typography.labelLarge)
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = onUseSleep,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) { Text("Pick a Sleep listing") }
                    TextButton(
                        onClick = onUseCoordinates,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) { Text("Enter coordinates") }
                }
            }
        }
        // v2.3.5 (#3): the reason now says what really failed; always offer a way forward.
        is StayInputFeedback.Rejected -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PlanInlineBanner(
                message = PlanGuideFormat.sanitizeDisplay(feedback.message) ?: "That didn't look like a location.",
                tone = PlanBannerTone.ERROR,
                icon = Icons.Rounded.ErrorOutline
            )
            InlineStayNameSearch(
                destinationName = destinationName,
                initialQuery = "",
                onSearchByName = onSearchByName
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (destinationName != null) {
                    val destStr = PlanGuideFormat.sanitizeDisplay(destinationName) ?: "Destination"
                    FilledTonalButton(
                        onClick = onUseDestinationCenter,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Use $destStr center", style = MaterialTheme.typography.labelLarge)
                    }
                }
                TextButton(onClick = onUseSleep, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Pick a Sleep listing")
                }
                TextButton(onClick = onUseCoordinates, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Enter coordinates")
                }
            }
        }
    }
}

@Composable
private fun InlineStayNameSearch(
    destinationName: String?,
    initialQuery: String,
    onSearchByName: (String) -> Unit
) {
    var nameQuery by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    val focusManager = LocalFocusManager.current
    val submitSearch = {
        val q = nameQuery.trim()
        if (q.isNotEmpty()) {
            focusManager.clearFocus()
            onSearchByName(q)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val destStr = PlanGuideFormat.sanitizeDisplay(destinationName)
        OutlinedTextField(
            value = nameQuery,
            onValueChange = { nameQuery = it.take(200) },
            label = { Text("Search hotel or area by name") },
            placeholder = { 
                Text(if (destStr != null) "e.g. hotel name, area, $destStr" else "Hotel name or area") 
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
            colors = sheetFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        FilledTonalButton(
            onClick = submitSearch,
            enabled = nameQuery.isNotBlank(),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (nameQuery.isNotBlank()) {
                    "Search by name: ${PlanGuideFormat.truncate(nameQuery.trim(), 32)}"
                } else {
                    "Search by name"
                },
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "Searches OpenStreetMap for this name. ${NominatimGeocoder.ATTRIBUTION}",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/** "Share stay with group": OFF by default; the whole row toggles (TalkBack role Switch). */
@Composable
private fun ShareStayToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Share stay with group",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = SHARE_STAY_PRIVACY_BLURB,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun sheetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
)

private const val MAX_INPUT_CHARS = 2_000

/** Privacy copy shown under the share toggle (decision #14). */
const val SHARE_STAY_PRIVACY_BLURB = "Visible to everyone in this trip. Off by default."
