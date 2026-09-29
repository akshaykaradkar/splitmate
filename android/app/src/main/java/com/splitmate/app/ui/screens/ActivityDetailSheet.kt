package com.splitmate.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.ui.SplitMateTnumMonospace
import com.splitmate.app.ui.SplitMateViewModel.ExpenseSplitBreakdownSummary
import com.splitmate.app.ui.cleanDisplayExpenseTitle
import com.splitmate.app.ui.components.ExperimentalMaterial3ExpressiveApi
import com.splitmate.app.ui.components.ExpressiveMenuAction
import com.splitmate.app.ui.components.LinearWavyProgressIndicator
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
    onUndoExpense: (String) -> Unit
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = cleanTitle,
                        fontFamily = SplitMateTheme.FontDisplay,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = SplitMateTheme.PrimaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Paid by ${payer?.name ?: "You"} · ${breakdown.perPersonHeadlineShare}/person",
                        fontFamily = SplitMateTheme.FontRounded,
                        fontSize = 12.sp,
                        color = SplitMateTheme.TextSecondary,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }

                // 2. Typography Hero Moment: Elevate the transaction total using MaterialTheme.typography.displaySmall + SplitMateTnumMonospace
                Text(
                    text = formattedTotal,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = SplitMateTnumMonospace,
                        fontWeight = FontWeight.Black,
                        fontFeatureSettings = "tnum"
                    ),
                    color = SplitMateTheme.PrimaryDark
                )
            }

            if (parsedTravelTicket != null &&
                (parsedTravelTicket.pnr.isNotBlank() || isFlightTicketExpense(expense.title, parsedTravelTicket))
            ) {
                Spacer(modifier = Modifier.height(14.dp))
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
                        .height(46.dp)
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

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = SplitMateTheme.BorderLight)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = breakdown.headerLabel,
                fontFamily = SplitMateTheme.FontDisplay,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SplitMateTheme.PrimaryDark
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 3. M3 Expressive Segmented Island: Each participant row morphs corners based on position + LinearWavyProgressIndicator
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
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
                    val shareFraction = if (expense.totalAmountCents > 0L) {
                        (row.owedCents.toFloat() / expense.totalAmountCents.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = itemShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.displayName,
                                    fontFamily = SplitMateTheme.FontRounded,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = row.formattedShare,
                                    fontFamily = SplitMateTnumMonospace,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = TextStyle(fontFamily = SplitMateTnumMonospace, fontFeatureSettings = "tnum")
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearWavyProgressIndicator(
                                progress = { shareFraction },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.14f),
                                amplitude = 0.75f,
                                wavelength = 18.dp,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. M3 Expressive SplitButtonLayout for Edit Expense (Leading) + Undo Entry (Trailing Menu)
            SplitButtonLayout(
                leadingText = "Edit Expense",
                leadingIcon = Icons.Rounded.Edit,
                onLeadingClick = {
                    onDismiss()
                    onEditExpense(expense)
                },
                menuItems = listOf(
                    ExpressiveMenuAction(
                        label = "Undo Entry",
                        icon = Icons.AutoMirrored.Rounded.Undo,
                        subtitle = "Roll back this expense from the ledger",
                        onClick = {
                            val idToRollback = expense.expenseId
                            onDismiss()
                            onUndoExpense(idToRollback)
                        }
                    )
                ),
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
