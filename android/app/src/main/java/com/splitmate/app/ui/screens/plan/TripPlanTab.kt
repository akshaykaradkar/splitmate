@file:OptIn(ExperimentalMaterial3Api::class)

package com.splitmate.app.ui.screens.plan

import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Map
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.splitmate.app.ui.components.ConnectedButtonGroup
import com.splitmate.app.ui.components.InFlightWavyProgressIndicator
import com.splitmate.app.ui.components.LocalMotionScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * v2.3.4 Plan tab host: the `[Bookings | Explore | Loop]` switch (decision #9), the in-flight wavy
 * bar pinned right under it (audit 5.3), the active sub-view, the place/stay sheets, and the
 * one-shot [TripGuideEffect]s that need an Android `Context`.
 *
 * Renders [state] and calls [actions] only; it never touches repositories. [bookingsContent] is the
 * existing day timeline, rendered unchanged.
 */
@Composable
fun TripPlanTab(
    state: TripGuideUiState,
    actions: TripGuideActions,
    effects: Flow<TripGuideEffect>,
    bookingsContent: @Composable (onMakeLoop: ((day: Int?) -> Unit)?) -> Unit,
    modifier: Modifier = Modifier,
    /** rc14: the trip's days; null until there are bookings or trip dates. */
    tripDays: com.splitmate.app.data.guide.TripDayRange? = null,
    onFreezeTripDates: () -> Unit = {},
    onLogExpenseForPlace: ((com.splitmate.app.data.guide.Place) -> Unit)? = null
) {
    val motion = LocalMotionScheme.current
    val snackbarHostState = remember { SnackbarHostState() }
    // rc14: "Add to loop" in Explore/the place sheet puts the stop on a day when Explore was opened
    // from a day's loop; that first day assignment also freezes the trip dates.
    val latestFreeze by rememberUpdatedState(onFreezeTripDates)
    val targetDay = state.exploreTargetDay
    val dayAwareActions: TripGuideActions = remember(actions, targetDay) {
        if (targetDay == null) actions else object : TripGuideActions by actions {
            override fun togglePin(placeId: String) {
                latestFreeze()
                actions.togglePin(placeId)
            }
        }
    }
    PlanEffectsCollector(effects = effects, snackbarHostState = snackbarHostState)

    // Remote kill-switch: without the guide only Bookings remains, so the switch disappears.
    // v2.4.0-rc18: Days | Explore | Loop in the connected 3-segment M3 Expressive switcher.
    val options = if (state.guideEnabled) {
        listOf(PlanSubView.BOOKINGS, PlanSubView.EXPLORE, PlanSubView.LOOP)
    } else {
        listOf(PlanSubView.BOOKINGS)
    }
    val reachable = if (state.guideEnabled) PlanSubView.entries.toList() else listOf(PlanSubView.BOOKINGS)
    val current = if (state.subView in reachable) state.subView else PlanSubView.BOOKINGS

    // Stay sheet: opened manually (Stay chip / Loop CTA) or automatically whenever the ViewModel
    // reports stay feedback (e.g. a Maps link shared into the app). A dismissed feedback value
    // doesn't reopen the sheet; the next new feedback does.
    var staySheetRequested by rememberSaveable { mutableStateOf(false) }
    var dismissedFeedbackId by remember { mutableStateOf(0L) }
    val latestStayFeedback by rememberUpdatedState(state.stayFeedback)
    val latestFeedbackRequestId by rememberUpdatedState(state.stayFeedbackRequestId)
    val feedbackWantsSheet = state.guideEnabled &&
        state.stayFeedback != StayInputFeedback.Idle &&
        state.stayFeedbackRequestId != dismissedFeedbackId
    val staySheetVisible = state.guideEnabled && (staySheetRequested || feedbackWantsSheet)
    val openStaySheet = {
        dismissedFeedbackId = 0L
        staySheetRequested = true
    }
    val dismissStaySheet = {
        staySheetRequested = false
        if (latestStayFeedback != StayInputFeedback.Idle) dismissedFeedbackId = latestFeedbackRequestId
        actions.dismissStayFeedback()
    }

    // rc13 (audit H-2): Explore opened from the Loop ("Go to Explore") goes Back to the Loop, not Days.
    var exploreOpenedFromLoop by rememberSaveable { mutableStateOf(false) }
    var lastView by remember { mutableStateOf(current) }
    LaunchedEffect(current) {
        when (current) {
            PlanSubView.EXPLORE -> if (lastView == PlanSubView.LOOP) exploreOpenedFromLoop = true
            PlanSubView.BOOKINGS -> exploreOpenedFromLoop = false
            PlanSubView.LOOP -> exploreOpenedFromLoop = false
        }
        lastView = current
    }

    BackHandler(enabled = staySheetVisible || state.selectedPlace != null || current != PlanSubView.BOOKINGS) {
        when {
            staySheetVisible -> dismissStaySheet()
            state.selectedPlace != null -> actions.dismissPlace()
            current == PlanSubView.EXPLORE && exploreOpenedFromLoop -> actions.selectSubView(PlanSubView.LOOP)
            current != PlanSubView.BOOKINGS -> actions.selectSubView(PlanSubView.BOOKINGS)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (options.size > 1) {
                ConnectedButtonGroup(
                    options = options,
                    selectedIndex = options.indexOf(current).coerceAtLeast(0),
                    onSelect = { _, view -> actions.selectSubView(view) },
                    labelProvider = { view ->
                        if (view == PlanSubView.LOOP && state.loop.activeStopCount > 0) {
                            "${view.label} · ${state.loop.activeStopCount}"
                        } else {
                            view.label
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            // Driven ONLY by an in-flight network job; it keeps a 400ms minimum and animates out itself.
            InFlightWavyProgressIndicator(
                inFlight = state.networkInFlight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentDescription = "Loading guide"
            )
            Crossfade(
                targetState = current,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                animationSpec = motion.defaultEffectsSpec(),
                label = "PlanSubViewCrossfade"
            ) { view ->
                when (view) {
                    PlanSubView.BOOKINGS -> bookingsContent(
                        // rc14: each day's "Make a loop" opens that day's loop.
                        if (state.guideEnabled) ({ day ->
                            actions.selectLoopDay(day)
                            actions.selectSubView(PlanSubView.LOOP)
                        }) else null
                    )
                    PlanSubView.EXPLORE -> androidx.compose.runtime.CompositionLocalProvider(LocalExploreTargetDay provides targetDay) {
                        ExploreGuideView(
                            state = state,
                            actions = dayAwareActions,
                            onOpenStaySheet = openStaySheet
                        )
                    }
                    PlanSubView.LOOP -> DayLoopView(
                        state = state,
                        actions = actions,
                        onSetStay = openStaySheet,
                        tripDays = tripDays,
                        onMovePlaceToDay = { placeId, day ->
                            if (day != null) onFreezeTripDates()
                            actions.movePlaceToDay(placeId, day)
                        },
                        onLogExpenseForPlace = onLogExpenseForPlace
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    val selected = state.selectedPlace
    if (selected != null && state.guideEnabled) {
        val card = remember(selected, state.sections) {
            state.sections.asSequence().flatMap { it.places.asSequence() }.firstOrNull { it.place.id == selected.id }
        }
        androidx.compose.runtime.CompositionLocalProvider(LocalExploreTargetDay provides targetDay) {
            PlaceDetailSheet(
                place = selected,
                card = card,
                actions = dayAwareActions,
                onDismiss = { actions.dismissPlace() },
                onLogExpense = if (onLogExpenseForPlace != null) ({
                    actions.dismissPlace()
                    onLogExpenseForPlace(selected)
                }) else null
            )
        }
    }

    if (staySheetVisible) {
        StayPinSheet(
            state = state,
            actions = actions,
            onDismiss = dismissStaySheet
        )
    }

    if (state.loopMapsParts.isNotEmpty()) {
        LoopMapsPartsSheet(
            parts = state.loopMapsParts,
            onOpen = { actions.openLoopMapsPart(it) },
            onDismiss = { actions.dismissLoopMapsParts() }
        )
    }
}

/**
 * rc13 (audit M-1): Google Maps accepts 9 stops per link, so a longer loop is opened part by part.
 * Each part starts where the previous one ended; the sheet stays open so the next part is one tap away.
 */
@Composable
private fun LoopMapsPartsSheet(
    parts: List<com.splitmate.app.data.guide.nav.MapsHandoff.LoopMapsPart>,
    onOpen: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val opened = remember(parts) { androidx.compose.runtime.mutableStateListOf<Int>() }
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
        ) {
            androidx.compose.material3.Text(
                text = "Open in Google Maps",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                modifier = Modifier.semantics { heading() }
            )
            androidx.compose.material3.Text(
                text = "Google Maps takes up to 9 stops at a time, so your loop opens in ${parts.size} parts. Open them in order.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
            parts.forEachIndexed { index, part ->
                androidx.compose.material3.FilledTonalButton(
                    onClick = {
                        if (index !in opened) opened += index
                        onOpen(index)
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                ) {
                    com.splitmate.app.ui.components.SymbolIcon(
                        imageVector = if (index in opened) androidx.compose.material.icons.Icons.Rounded.CheckCircle
                        else androidx.compose.material.icons.Icons.Rounded.Map,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    androidx.compose.material3.Text(
                        text = part.label,
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Collects one-shot effects for the lifetime of the Plan tab. */
@Composable
private fun PlanEffectsCollector(effects: Flow<TripGuideEffect>, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(effects, context) {
        effects.collect { effect ->
            handlePlanEffect(context, effect) { message -> scope.showSnack(snackbarHostState, message) }
        }
    }
}

private fun CoroutineScope.showSnack(host: SnackbarHostState, message: String) {
    val text = PlanGuideFormat.sanitizeDisplay(message) ?: return
    launch { host.showSnackbar(text) }
}

/** Performs one effect. Never throws: every failure becomes a short, user-safe snackbar. */
internal fun handlePlanEffect(context: Context, effect: TripGuideEffect, snack: (String) -> Unit) {
    when (effect) {
        is TripGuideEffect.OpenMaps ->
            openMaps(context, effect.primaryUri, effect.fallbackUrl, chooserTitle = null, snack = snack)
        is TripGuideEffect.OpenMapsChooser ->
            openMaps(context, effect.primaryUri, effect.fallbackUrl, chooserTitle = effect.chooserTitle, snack = snack)
        is TripGuideEffect.OpenUrl -> openHttpsUrl(context, effect.url, snack)
        is TripGuideEffect.CopyToClipboard -> {
            val copied = runCatching {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard?.setPrimaryClip(ClipData.newPlainText(effect.label, effect.text))
                clipboard != null
            }.getOrDefault(false)
            // Android 13+ shows its own clipboard confirmation.
            if (!copied) snack("Couldn't copy") else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) snack("Copied")
        }
        is TripGuideEffect.ShareText -> {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, effect.text)
            }
            startSafely(context, Intent.createChooser(send, "Share with your group")) { snack("No app available to share") }
        }
        is TripGuideEffect.Snackbar -> snack(effect.message)
    }
}

/** `geo:` / `google.navigation:` intent; on ActivityNotFoundException fall back to the https Maps URL. */
private fun openMaps(
    context: Context,
    primaryUri: String,
    fallbackUrl: String,
    chooserTitle: String?,
    snack: (String) -> Unit
) {
    val view = Intent(Intent.ACTION_VIEW, Uri.parse(primaryUri))
    val intent = if (chooserTitle != null) Intent.createChooser(view, chooserTitle) else view
    try {
        context.startActivity(intent.withNewTaskIfNeeded(context))
    } catch (_: ActivityNotFoundException) {
        openHttpsUrl(context, fallbackUrl, snack)
    } catch (_: SecurityException) {
        openHttpsUrl(context, fallbackUrl, snack)
    }
}

/** Opens an https URL in the browser. Anything that isn't https is refused. */
private fun openHttpsUrl(context: Context, url: String, snack: (String) -> Unit) {
    if (!PlanGuideFormat.isSafeHttpsUrl(url)) {
        snack("Couldn't open that link")
        return
    }
    startSafely(context, Intent(Intent.ACTION_VIEW, Uri.parse(url))) { snack("No app available to open the link") }
}

private fun startSafely(context: Context, intent: Intent, onFailure: () -> Unit) {
    try {
        context.startActivity(intent.withNewTaskIfNeeded(context))
    } catch (_: ActivityNotFoundException) {
        onFailure()
    } catch (_: SecurityException) {
        onFailure()
    }
}

private fun Intent.withNewTaskIfNeeded(context: Context): Intent =
    if (context.findActivity() == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) else this

private tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
