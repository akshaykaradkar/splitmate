@file:OptIn(ExperimentalMaterial3Api::class)

package com.splitmate.app.ui.screens.plan

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
    bookingsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = LocalMotionScheme.current
    val snackbarHostState = remember { SnackbarHostState() }
    PlanEffectsCollector(effects = effects, snackbarHostState = snackbarHostState)

    // Remote kill-switch: without the guide only Bookings remains, so the switch disappears.
    val options = if (state.guideEnabled) PlanSubView.entries.toList() else listOf(PlanSubView.BOOKINGS)
    val current = if (state.subView in options) state.subView else PlanSubView.BOOKINGS

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

    BackHandler(enabled = staySheetVisible || state.selectedPlace != null || current != PlanSubView.BOOKINGS) {
        when {
            staySheetVisible -> dismissStaySheet()
            state.selectedPlace != null -> actions.dismissPlace()
            current != PlanSubView.BOOKINGS -> actions.selectSubView(PlanSubView.BOOKINGS)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (options.size > 1) {
                ConnectedButtonGroup(
                    options = options,
                    selectedIndex = options.indexOf(current),
                    onSelect = { _, view -> actions.selectSubView(view) },
                    labelProvider = { it.label },
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
                    PlanSubView.BOOKINGS -> bookingsContent()
                    PlanSubView.EXPLORE -> ExploreGuideView(
                        state = state,
                        actions = actions,
                        onOpenStaySheet = openStaySheet
                    )
                    PlanSubView.LOOP -> DayLoopView(
                        state = state,
                        actions = actions,
                        onSetStay = openStaySheet
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
        PlaceDetailSheet(
            place = selected,
            card = card,
            actions = actions,
            onDismiss = { actions.dismissPlace() }
        )
    }

    if (staySheetVisible) {
        StayPinSheet(
            state = state,
            actions = actions,
            onDismiss = dismissStaySheet
        )
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
