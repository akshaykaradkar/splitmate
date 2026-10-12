package com.splitmate.app.ui.feel

import com.splitmate.app.ui.SplitMateViewModel

/**
 * v2.4.0 D5: the one Mark paid flow used everywhere (Money tab button, menu and swipe, organizer
 * rows, and the Settle screen).
 *
 * 1. [save] records the payment straight away and returns its settlement id (null = refused).
 * 2. On success the button turns into the "Paid" chip with a Confirm haptic ([PaidMorphController])
 *    and a snackbar offers Undo for about 4 seconds. Undo removes exactly that settlement through
 *    [SplitMateViewModel.undoSettlement] (tombstoned, so it stays removed after sync).
 * 3. When refused, the Reject haptic plays and the snackbar says why.
 */
fun markPaidWithUndo(
    morph: PaidMorphController,
    snackbar: MoneySnackbar?,
    viewModel: SplitMateViewModel,
    summary: String,
    save: () -> String?
) {
    var attempted = false
    var savedId: String? = null
    morph.trigger {
        attempted = true
        savedId = save()
        savedId != null
    }
    if (!attempted) return // the chip is already showing for this row
    val id = savedId
    if (id != null) {
        // The save used the active trip; remember it so Undo targets the same trip (review U1).
        val savedInGroupId = viewModel.uiState.value.activeGroupId
        snackbar?.offerUndo("Marked paid · $summary") {
            viewModel.undoSettlement(id, groupIdHint = savedInGroupId)
            snackbar.say("Payment undone")
        }
    } else {
        viewModel.uiState.value.statusBannerMessage?.let { reason -> snackbar?.say(reason) }
    }
}
