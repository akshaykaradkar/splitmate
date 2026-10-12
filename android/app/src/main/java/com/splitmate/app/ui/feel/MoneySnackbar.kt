package com.splitmate.app.ui.feel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * v2.4.0 D5: one app-wide snackbar for money moments.
 *
 * - [offerUndo]: "Marked paid · ₹14.17 from Sam to Akshay" with an Undo action (about 4 seconds).
 * - [say]: a plain message, e.g. why a payment could not be recorded.
 * - [announce] (v2.4.0 D7): a calm, non-urgent line such as "Priya joined Goa".
 *
 * It lives at the app root, so it survives leaving the screen where the payment was made.
 * Money messages always win: [offerUndo] and [say] cancel any waiting or showing announcement,
 * so an Undo is never pushed back behind "joined" lines.
 */
@Stable
class MoneySnackbar internal constructor(
    val hostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    private val announceLock = Mutex()
    private val announcements = mutableListOf<Job>()

    fun offerUndo(message: String, onUndo: () -> Unit) {
        cancelAnnouncements()
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                withDismissAction = false,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    fun say(message: String) {
        cancelAnnouncements()
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    /**
     * v2.4.0 D7: shows [message] once nothing else is on screen, one at a time. It never dismisses
     * what is showing (an open Undo stays usable). [onShown] runs right as the line appears, so a
     * haptic lands with the text, not seconds earlier.
     */
    fun announce(message: String, onShown: () -> Unit = {}) {
        val job = scope.launch {
            announceLock.withLock {
                snapshotFlow { hostState.currentSnackbarData }.first { it == null }
                onShown()
                hostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            }
        }
        announcements += job
        job.invokeOnCompletion { scope.launch { announcements.remove(job) } }
    }

    private fun cancelAnnouncements() {
        announcements.toList().forEach { it.cancel() }
        announcements.clear()
    }
}

/** Null outside [ProvideMoneySnackbar] (previews, isolated tests): callers then skip the snackbar. */
val LocalMoneySnackbar = staticCompositionLocalOf<MoneySnackbar?> { null }

/** Provides [LocalMoneySnackbar] and draws its host above the bottom toolbar and FAB. */
@Composable
fun ProvideMoneySnackbar(content: @Composable BoxScope.() -> Unit) {
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackbar = remember(hostState, scope) { MoneySnackbar(hostState, scope) }
    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalMoneySnackbar provides snackbar) {
            content()
        }
        SnackbarHost(
            hostState = hostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 96.dp)
        )
    }
}
