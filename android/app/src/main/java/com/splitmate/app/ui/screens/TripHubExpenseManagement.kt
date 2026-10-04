package com.splitmate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
            readOnlyReason = ExpenseEditPermission.READ_ONLY_REASON
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
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
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
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteExpense()
                    }
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
                TextButton(onClick = { showDeleteConfirm = false }) {
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
