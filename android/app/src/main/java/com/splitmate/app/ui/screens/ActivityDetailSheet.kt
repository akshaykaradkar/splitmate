package com.splitmate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ExpenseSplitCopy
import com.splitmate.app.ExpenseSplitNature
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.ui.SplitMateTnumMonospace
import com.splitmate.app.ui.SplitMateViewModel.ExpenseSplitBreakdownSummary
import com.splitmate.app.ui.cleanDisplayExpenseTitle
import com.splitmate.app.ui.components.EditorialFinancialTotalText
import com.splitmate.app.ui.components.ExperimentalMaterial3ExpressiveApi
import com.splitmate.app.ui.components.ExpressiveMenuAction
import com.splitmate.app.ui.components.SplitButtonLayout
import com.splitmate.app.ui.components.rememberAnimatedSegmentedIslandItemShape
import com.splitmate.app.ui.extractTravelTicketFromTitle
import com.splitmate.app.ui.formatIndianRupeesFromCents
import com.splitmate.app.ui.isFlightTicketExpense

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActivityDetailSheet(
    expense: ExpenseEntity,
    payer: GroupMemberEntity?,
    breakdown: ExpenseSplitBreakdownSummary,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onOpenBoardingPass: (groupId: String, pnrOrExpenseCode: String) -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    onUndoExpense: (String) -> Unit,
    /** v2.3.5 (#4): false -> Edit/Delete are hidden and [readOnlyReason] is shown instead. */
    canModify: Boolean = true,
    readOnlyReason: String = com.splitmate.app.ExpenseEditPermission.READ_ONLY_REASON,
    /** v2.3.6 P3 Money check: "Shares add up" line or the gap with its 1-tap fix. */
    sharesCheck: (@Composable () -> Unit)? = null
) {
    // v2.3.5 (#4): deleting an expense always asks for confirmation first.
    var showDeleteConfirm by remember(expense.expenseId) { mutableStateOf(false) }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Delete this expense?",
                    fontFamily = SplitMateTheme.FontDisplay,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = "\"${cleanDisplayExpenseTitle(expense.title)}\" will be removed for everyone in the trip and balances will be recalculated.",
                    fontFamily = SplitMateTheme.FontRounded
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        val idToRollback = expense.expenseId
                        onDismiss()
                        onUndoExpense(idToRollback)
                    }
                ) {
                    Text("Delete", fontWeight = FontWeight.ExtraBold, color = SplitMateTheme.TerracottaText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", fontWeight = FontWeight.Bold, color = SplitMateTheme.TextSecondary)
                }
            },
            containerColor = SplitMateTheme.SurfaceWhite
        )
    }
    val parsedTravelTicket = remember(expense.title) {
        extractTravelTicketFromTitle(expense.title)
    }
    val cleanTitle = remember(expense.title) {
        cleanDisplayExpenseTitle(expense.title)
    }
    val formattedTotal = remember(expense.totalAmountCents, currencySymbol) {
        formatIndianRupeesFromCents(
            cents = expense.totalAmountCents,
            includePlusSign = false,
            currencySymbol = currencySymbol
        )
    }

    // 1. Data Filtering (Clarity First): NEVER render users with a ₹0.00 or "Excluded" share.
    val activeParticipantRows = remember(breakdown.rows) {
        breakdown.rows.filter { row ->
            row.isIncludedInSplit &&
                row.owedCents > 0L &&
                !row.formattedShare.contains("Excluded", ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SplitMateTheme.SurfaceWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = cleanTitle,
                fontFamily = SplitMateTheme.FontDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = SplitMateTheme.PrimaryDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                // v2.3.3: "Paid by X · Personal expense" / "Paid by X for Y · Y owes ₹Z"; SHARED unchanged.
                text = breakdown.paidBySubtitle(
                    payerName = payer?.name ?: "You",
                    sharedDetail = "${breakdown.perPersonHeadlineShare}/person"
                ),
                fontFamily = SplitMateTheme.FontRounded,
                fontSize = 12.sp,
                color = SplitMateTheme.TextSecondary,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 2. Typography Hero Moment: Elevate the transaction total using MaterialTheme.typography.displaySmall + SplitMateTnumMonospace.
            // The figure sits on its own line and auto-fits so large totals never overflow the header.
            EditorialFinancialTotalText(
                text = formattedTotal,
                style = MaterialTheme.typography.displaySmall,
                fontFamily = SplitMateTnumMonospace,
                fontWeight = FontWeight.Black,
                color = SplitMateTheme.PrimaryDark,
                modifier = Modifier.fillMaxWidth()
            )

            if (parsedTravelTicket != null &&
                (parsedTravelTicket.pnr.isNotBlank() || isFlightTicketExpense(expense.title, parsedTravelTicket))
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val gId = expense.groupId
                        val isFlightSel = isFlightTicketExpense(expense.title, parsedTravelTicket) ||
                            expense.expenseCategory.equals("FLIGHT", ignoreCase = true)
                        val pnrCode = if (isFlightSel) "EXPENSE:${expense.expenseId}" else parsedTravelTicket.pnr
                        onDismiss()
                        onOpenBoardingPass(gId, pnrCode)
                    },
                    shape = SplitMateTheme.RadiusButton,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SplitMateTheme.PrimaryDark,
                        contentColor = SplitMateTheme.ScreenBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = if (isFlightTicketExpense(expense.title, parsedTravelTicket)) {
                            Icons.Rounded.FlightTakeoff
                        } else {
                            Icons.Rounded.Train
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (parsedTravelTicket.pnr.isNotBlank()) {
                            "Open Full Boarding Pass (PNR ${parsedTravelTicket.pnr})"
                        } else {
                            "Open Full Boarding Pass"
                        },
                        fontFamily = SplitMateTheme.FontRounded,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(fontFamily = SplitMateTnumMonospace, fontFeatureSettings = "tnum")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // M3E spacing (no divider): the section break is carried by whitespace alone.
            Spacer(modifier = Modifier.height(24.dp))

            if (breakdown.splitNature == ExpenseSplitNature.PERSONAL) {
                // v2.3.3: a personal expense has nothing to split — one calm row, no "1 of N" count
                // and no per-person math.
                PersonalExpenseNotSplitRow(payerName = payer?.name ?: "You")
            } else {
            Text(
                text = breakdown.headerLabel,
                fontFamily = SplitMateTheme.FontDisplay,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SplitMateTheme.PrimaryDark
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 3. M3 Expressive Segmented Island: Each participant row morphs corners based on position.
            // Static share amounts carry no progress/wavy indicator (v2.3.2 M3E audit).
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(
                    items = activeParticipantRows,
                    key = { _, row -> row.memberId }
                ) { index, row ->
                    val itemShape = rememberAnimatedSegmentedIslandItemShape(
                        index = index,
                        totalCount = activeParticipantRows.size,
                        isSelected = false
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = itemShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = row.displayName,
                                fontFamily = SplitMateTheme.FontRounded,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            )
                            Text(
                                text = row.formattedShare,
                                fontFamily = SplitMateTnumMonospace,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = TextStyle(fontFamily = SplitMateTnumMonospace, fontFeatureSettings = "tnum")
                            )
                        }
                    }
                }
            }
            }
            sharesCheck?.invoke()

            Spacer(modifier = Modifier.height(24.dp))

            // 4. M3 Expressive SplitButtonLayout for Edit Expense (Leading) + Delete (Trailing Menu)
            if (canModify) {
                SplitButtonLayout(
                    leadingText = "Edit Expense",
                    leadingIcon = Icons.Rounded.Edit,
                    onLeadingClick = {
                        onDismiss()
                        onEditExpense(expense)
                    },
                    menuItems = listOf(
                        ExpressiveMenuAction(
                            label = "Delete Expense",
                            icon = Icons.AutoMirrored.Rounded.Undo,
                            subtitle = "Remove this expense from the trip ledger",
                            onClick = { showDeleteConfirm = true }
                        )
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    fillWidth = true
                )
            } else {
                // v2.3.5 (#4): read-only for members who are not payer / creator / organizer.
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = SplitMateTheme.TextSecondary,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = readOnlyReason,
                        fontFamily = SplitMateTheme.FontRounded,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SplitMateTheme.TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * v2.3.3: Single tasteful row for PERSONAL expenses (sole participant == payer). Replaces the
 * "Individual Share Breakdown (1 of N members splitting)" list — there is nothing to split.
 */
@Composable
private fun PersonalExpenseNotSplitRow(payerName: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ExpenseSplitCopy.PERSONAL_EXPENSE_NOT_SPLIT,
                    fontFamily = SplitMateTheme.FontDisplay,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = if (payerName == "You") {
                        "Your own cost · no one else owes anything"
                    } else {
                        "$payerName's own cost · no one else owes anything"
                    },
                    fontFamily = SplitMateTheme.FontRounded,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
