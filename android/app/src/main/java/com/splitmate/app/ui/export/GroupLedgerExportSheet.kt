package com.splitmate.app.ui.export

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.splitmate.app.ui.components.ExpressivePlainTooltipBox
import com.splitmate.app.ui.components.LoadingIndicator
import com.splitmate.app.ui.components.SymbolIcon
import com.splitmate.app.ui.feel.Feel
import com.splitmate.app.ui.feel.LocalMoneySnackbar
import com.splitmate.app.ui.feel.feel
import kotlinx.coroutines.launch

/**
 * GM3 Expressive modal bottom sheet for exporting a single group's complete itemized ledger
 * (Expenses + per-member shares, Member Balances, and Greedy Minimum Settlement Plan) as either
 * a multi-sheet Excel `.xlsx` workbook with live formulas or a universal UTF-8 `.csv` file.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupLedgerExportSheet(
    model: GroupLedgerExportModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val moneySnackbar = LocalMoneySnackbar.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var busyFormat by remember { mutableStateOf<GroupExportFormat?>(null) }

    val closeSheet: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Export group ledger",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = model.groupName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ExpressivePlainTooltipBox(tooltipText = "Close") {
                    androidx.compose.material3.IconButton(
                        onClick = closeSheet,
                        shapes = androidx.compose.material3.IconButtonDefaults.shapes()
                    ) {
                        SymbolIcon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Summary strip: expense count, total spend, minimum transfers
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                val totalLabel = "${model.currencySymbol}${GroupLedgerExportBuilder.formatCentsDecimal(model.totalSpendCents)}"
                val transferLabel = if (model.pendingTransfers.isEmpty()) {
                    "All settled (0 transfers)"
                } else {
                    val n = model.pendingTransfers.size
                    "$n min ${if (n == 1) "transfer" else "transfers"}"
                }
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${model.expenses.size} expenses · $totalLabel · $transferLabel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Includes every expense, payer, per-person share, net balances, and the simplified minimum-transaction settlement plan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                val options = listOf(
                    Triple(
                        GroupExportFormat.EXCEL_XLSX,
                        "Excel Workbook (.xlsx)",
                        "3 sheets (Expenses, Member Balances, Minimum Settlement Plan) with live =SUM & =SUMIF formulas"
                    ),
                    Triple(
                        GroupExportFormat.CSV,
                        "CSV Spreadsheet (.csv)",
                        "Single UTF-8 file for Google Sheets, Excel, or quick sharing on WhatsApp & Email"
                    )
                )
                options.forEachIndexed { idx, (format, title, subtitle) ->
                    val isBusy = busyFormat == format
                    SegmentedListItem(
                        onClick = {
                            if (busyFormat != null) return@SegmentedListItem
                            haptics.feel(Feel.Tick)
                            busyFormat = format
                            scope.launch {
                                val result = GroupLedgerExportHelper.exportAndShare(context, model, format)
                                busyFormat = null
                                if (result.isSuccess) {
                                    closeSheet()
                                } else {
                                    val msg = "Couldn't export ${format.extension.uppercase()}. Try again."
                                    moneySnackbar?.say(msg)
                                        ?: Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = busyFormat == null,
                        shapes = ListItemDefaults.segmentedShapes(index = idx, count = options.size),
                        colors = ListItemDefaults.segmentedColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                        leadingContent = {
                            if (isBusy) {
                                LoadingIndicator(size = 22.dp, color = MaterialTheme.colorScheme.primary)
                            } else {
                                SymbolIcon(
                                    imageVector = if (format == GroupExportFormat.EXCEL_XLSX) {
                                        Icons.Rounded.ViewAgenda
                                    } else {
                                        Icons.Rounded.ReceiptLong
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        supportingContent = {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            SymbolIcon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/** Compact Expressive tonal trigger button to open [GroupLedgerExportSheet] for a group. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupLedgerExportButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    FilledTonalButton(
        onClick = {
            haptics.feel(Feel.Tick)
            onClick()
        },
        shapes = ButtonDefaults.shapes(),
        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    ) {
        SymbolIcon(
            imageVector = Icons.Rounded.DownloadForOffline,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        Text(
            text = "Export Excel / CSV",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
