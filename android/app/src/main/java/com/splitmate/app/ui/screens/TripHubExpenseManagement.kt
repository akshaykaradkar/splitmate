package com.splitmate.app.ui.screens

import androidx.compose.material3.Surface
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.EditLoggedExpenseDialog
import com.splitmate.app.ExpenseEditPermission
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.ui.FigtreeFontFamily
import com.splitmate.app.ui.SplitMateUiState
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.cleanDisplayExpenseTitle
import com.splitmate.app.ui.components.ExpressiveSwipeAction
import com.splitmate.app.ui.components.ExpressiveSwipeActionsBox
import com.splitmate.app.ui.formatIndianRupeesFromCents

/**
 * v2.3.5 (issue #4): edit / delete for Trip Hub 2.0 expense cards.
 *
 * Trip Hub cards look up [LocalTripHubExpenseActions] to (a) open the shared
 * [ActivityDetailSheet] -> [EditLoggedExpenseDialog] flow already used by Audit Vault and
 * (b) ask whether the device user may modify an expense (payer, creator or organizer).
 * The ViewModel enforces the same rule again in `editExistingExpense` / `rollbackExpense`.
 */
class TripHubExpenseActions(
    val canModify: (ExpenseEntity) -> Boolean,
    val openDetails: (ExpenseEntity) -> Unit
)

/** Null outside [TripHubExpenseManagementHost]; cards then keep their legacy behaviour. */
val LocalTripHubExpenseActions = staticCompositionLocalOf<TripHubExpenseActions?> { null }

@Composable
fun TripHubExpenseManagementHost(
    viewModel: SplitMateViewModel,
    uiState: SplitMateUiState,
    onOpenTrainPnrReviewClick: (pnr: String) -> Unit,
    onOpenFlightReviewClick: (pnrOrTrigger: String) -> Unit,
    content: @Composable () -> Unit
) {
    var detailExpenseId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingExpenseId by rememberSaveable { mutableStateOf<String?>(null) }

    // Latest uiState is read through the lambdas so the static local never needs to change.
    val latestState by androidx.compose.runtime.rememberUpdatedState(uiState)
    val actions = remember(viewModel) {
        TripHubExpenseActions(
            canModify = { exp -> viewModel.canCurrentUserModifyExpense(exp, latestState) },
            openDetails = { exp -> detailExpenseId = exp.expenseId }
        )
    }

    CompositionLocalProvider(LocalTripHubExpenseActions provides actions) {
        content()
    }

    // If the expense was deleted (locally or by sync) while open, nothing renders.
    val detailExpense = detailExpenseId?.let { id -> uiState.expenses.find { it.expenseId == id } }
    detailExpense?.let { exp ->
        val payer = uiState.members.find { it.memberId == exp.payerId }
        val groupMembers = uiState.members.filter { it.groupId == exp.groupId }
        val breakdown = remember(exp, groupMembers, uiState.splits) {
            SplitMateViewModel.resolveExpenseSplitBreakdown(
                expense = exp,
                groupMembers = groupMembers,
                allSplits = uiState.splits,
                currencySymbol = "₹",
                headerPrefix = "Individual Share Breakdown"
            )
        }
        ActivityDetailSheet(
            expense = exp,
            payer = payer,
            breakdown = breakdown,
            currencySymbol = "₹",
            onDismiss = { detailExpenseId = null },
            onOpenBoardingPass = { _, pnrCode ->
                detailExpenseId = null
                if (classifyGroupExpenseForTripHub(exp) == TripHubBookingCategory.FLIGHT) {
                    onOpenFlightReviewClick(pnrCode)
                } else {
                    onOpenTrainPnrReviewClick(pnrCode)
                }
            },
            onEditExpense = { expToEdit -> editingExpenseId = expToEdit.expenseId },
            onUndoExpense = { idToRollback -> viewModel.rollbackExpense(idToRollback) },
            canModify = viewModel.canCurrentUserModifyExpense(exp, uiState),
            readOnlyReason = ExpenseEditPermission.READ_ONLY_REASON,
            // v2.3.6 P3 Money check: per-expense shares check + 1-tap fix (organizer / payer / creator).
            sharesCheck = {
                ExpenseSharesCheckRow(
                    expense = exp,
                    splits = uiState.splits,
                    canModify = viewModel.canCurrentUserModifyExpense(exp, uiState),
                    onUseSharesTotal = { viewModel.useSharesTotalForExpense(exp.expenseId) },
                    onEdit = { editingExpenseId = exp.expenseId }
                )
            },
            // v2.4.0 P4: edit history + revert heads-up with one-tap restore.
            editHistory = {
                val revisions by viewModel.observeExpenseRevisions(exp.expenseId)
                    .collectAsState(initial = emptyList())
                ExpenseEditHistorySection(
                    revisions = revisions,
                    memberNames = groupMembers.associate { it.memberId to it.name },
                    canRestore = viewModel.canCurrentUserModifyExpense(exp, uiState),
                    onRestoreTotal = { cents -> viewModel.restoreExpenseTotal(exp.expenseId, cents) }
                )
            }
        )
    }

    val editingExpense = editingExpenseId?.let { id -> uiState.expenses.find { it.expenseId == id } }
    editingExpense?.let { exp ->
        val expGroupMembers = uiState.members.filter { it.groupId == exp.groupId }
        val existingSplitIds = uiState.splits
            .filter { it.expenseId == exp.expenseId && it.finalOwedCents > 0L }
            .map { it.memberId }
            .toSet()
        EditLoggedExpenseDialog(
            expense = exp,
            groupMembers = expGroupMembers,
            initialSplitMemberIds = existingSplitIds.ifEmpty { expGroupMembers.map { it.memberId }.toSet() },
            onDismiss = { editingExpenseId = null },
            onSave = { newTitle, newRupees, newPayerId, updatedSplitMemberIds, newCategoryRef ->
                viewModel.editExistingExpense(
                    expenseId = exp.expenseId,
                    newTitle = newTitle,
                    newTotalRupees = newRupees,
                    newPayerId = newPayerId,
                    selectedMemberIds = updatedSplitMemberIds,
                    categoryRef = newCategoryRef
                )
                editingExpenseId = null
            },
            lockAmountAndSplit = viewModel.isExpenseItemized(exp, uiState)
        )
    }
}

/**
 * v2.3.5 (#4): "Edit" + "Delete" row used by the split drawers and by Train / Flight cards.
 * Delete always asks for confirmation. When the device user is not payer / creator / organizer
 * both actions are disabled and [ExpenseEditPermission.READ_ONLY_REASON] is shown.
 *
 * Outside [TripHubExpenseManagementHost] (no actions) only Delete-with-confirmation is shown.
 */
@Composable
fun TripHubManageExpenseRow(
    expense: ExpenseEntity?,
    onDeleteExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = LocalTripHubExpenseActions.current
    val canModify = expense == null || actions == null || actions.canModify(expense)
    var showDeleteConfirm by remember(expense?.expenseId) { mutableStateOf(false) }

    if (showDeleteConfirm) {
        TripHubDeleteExpenseDialog(
            expense = expense,
            onConfirm = {
                showDeleteConfirm = false
                onDeleteExpense()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (actions != null && expense != null) {
                TextButton(
                    onClick = { actions.openDetails(expense) },
                    enabled = canModify,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = if (canModify) TripHubTokens.TextPrimary else TripHubTokens.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Edit",
                        fontFamily = FigtreeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (canModify) TripHubTokens.TextPrimary else TripHubTokens.TextMuted
                    )
                }
            }
            TextButton(
                onClick = { showDeleteConfirm = true },
                enabled = canModify,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = if (canModify) TripHubTokens.TerracottaIconTint else TripHubTokens.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Delete",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (canModify) TripHubTokens.TerracottaIconTint else TripHubTokens.TextMuted
                )
            }
        }
        if (!canModify) {
            Text(
                text = ExpenseEditPermission.READ_ONLY_REASON,
                fontFamily = FigtreeFontFamily,
                fontSize = 11.sp,
                color = TripHubTokens.TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/** Shared "Delete this expense?" confirmation (manage row and swipe-to-delete). */
@Composable
private fun TripHubDeleteExpenseDialog(
    expense: ExpenseEntity?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SplitMateTheme.SurfaceWhite,
        title = {
            Text(
                text = "Delete this expense?",
                fontFamily = FigtreeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = TripHubTokens.TextPrimary
            )
        },
        text = {
            Text(
                text = expense?.let {
                    "\"${cleanDisplayExpenseTitle(it.title)}\" (${formatIndianRupeesFromCents(it.totalAmountCents)}) " +
                        "will be removed for everyone in the trip and balances will be recalculated."
                } ?: "This expense will be removed for everyone in the trip and balances will be recalculated.",
                fontFamily = FigtreeFontFamily,
                fontSize = 13.sp,
                color = TripHubTokens.TextSecondary
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    text = "Delete",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = SplitMateTheme.TerracottaText
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = TripHubTokens.TextSecondary
                )
            }
        }
    )
}

/** v2.3.6 Step C: confirmation behind the settle-row "Mark paid" swipe. */
@Composable
fun TripHubMarkPaidConfirmDialog(
    fromName: String,
    toName: String,
    formattedAmount: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    /** v2.3.6 P3: shown while a Money check fails. Paying is still allowed (cash may have moved). */
    warning: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = {
            Text(
                text = "Mark $formattedAmount as paid?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "$fromName paid $toName $formattedAmount. This is saved as a settlement receipt for everyone in the trip.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (warning != null) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(text = warning, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Mark paid", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/**
 * v2.3.6 Step C: swipe a Trip Hub booking card right to **edit** or left to **delete**.
 * The card never leaves on its own: after the swipe it springs back (theme motion) and the action
 * opens the existing details/edit sheet or the delete confirmation, so money data is never removed
 * by a stray gesture. Only offered when the device user may modify the expense; the Edit / Delete
 * buttons stay as the visible, accessible alternative, and both actions are also exposed as
 * TalkBack custom actions.
 */
@Composable
fun TripHubSwipeToManage(
    expense: ExpenseEntity,
    onDeleteExpense: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val actions = LocalTripHubExpenseActions.current
    if (actions == null || !actions.canModify(expense)) {
        Box(modifier = modifier) { content() }
        return
    }
    var showDeleteConfirm by remember(expense.expenseId) { mutableStateOf(false) }

    if (showDeleteConfirm) {
        TripHubDeleteExpenseDialog(
            expense = expense,
            onConfirm = {
                showDeleteConfirm = false
                onDeleteExpense()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    ExpressiveSwipeActionsBox(
        modifier = modifier,
        startAction = ExpressiveSwipeAction(
            label = "Edit",
            icon = Icons.Rounded.Edit,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onTrigger = { actions.openDetails(expense) }
        ),
        endAction = ExpressiveSwipeAction(
            label = "Delete",
            icon = Icons.Rounded.DeleteOutline,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onTrigger = { showDeleteConfirm = true }
        ),
        content = content
    )
}
