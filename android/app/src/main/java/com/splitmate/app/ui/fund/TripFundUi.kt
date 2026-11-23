package com.splitmate.app.ui.fund

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.components.ExpressivePlainTooltipBox
import com.splitmate.app.ui.components.SymbolIcon
import com.splitmate.app.ui.feel.Feel
import com.splitmate.app.ui.feel.feel
import com.splitmate.app.ui.formatIndianRupeesFromCents
import com.splitmate.app.ui.parseRupeesToPaiseExact
import com.splitmate.app.ui.sanitizeDecimalRupeesInput
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Auto-surfacing **Trip Fund / Cash in Hand** card rendered at the top of Trip Hub's Money tab
 * whenever at least one Trip Fund advance has been recorded for the group.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TripFundSummaryCard(
    summaries: List<TripFundHolderSummary>,
    currencySymbol: String,
    onAddAdvanceClick: () -> Unit,
    onRemoveAdvance: (settlementId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (summaries.isEmpty()) return
    val haptics = LocalHapticFeedback.current
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd MMM", Locale.US) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        summaries.forEach { summary ->
            var showAdvancesList by remember(summary.treasurerMemberId) { mutableStateOf(false) }
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                    SymbolIcon(
                                        imageVector = Icons.Rounded.Savings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Trip Fund",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Held by ${summary.treasurerName} · ${summary.contributorCount} ${if (summary.contributorCount == 1) "contributor" else "contributors"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = {
                                haptics.feel(Feel.Tick)
                                onAddAdvanceClick()
                            },
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = ButtonDefaults.SmallContentPadding,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            SymbolIcon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Top up",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 3-column tabular KPI strip: Collected | Spent | Cash in Hand (or Out of Pocket)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TripFundMetricTile(
                            label = "Collected",
                            amount = formatIndianRupeesFromCents(summary.totalCollectedCents, currencySymbol = currencySymbol),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        TripFundMetricTile(
                            label = "Spent",
                            amount = formatIndianRupeesFromCents(summary.treasurerSpentCents, currencySymbol = currencySymbol),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        val isDepleted = summary.isPoolDepleted
                        TripFundMetricTile(
                            label = if (isDepleted) "Out of pocket" else "Cash in hand",
                            amount = formatIndianRupeesFromCents(
                                kotlin.math.abs(summary.cashInHandCents),
                                currencySymbol = currencySymbol
                            ),
                            containerColor = if (isDepleted) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            contentColor = if (isDepleted) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    TextButton(
                        onClick = {
                            haptics.feel(Feel.Tick)
                            showAdvancesList = !showAdvancesList
                        },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = if (showAdvancesList) {
                                "Hide ${summary.advances.size} ${if (summary.advances.size == 1) "advance" else "advances"}"
                            } else {
                                "View ${summary.advances.size} ${if (summary.advances.size == 1) "advance" else "advances"}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        SymbolIcon(
                            imageVector = if (showAdvancesList) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(visible = showAdvancesList) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                        ) {
                            summary.advances.forEachIndexed { idx, adv ->
                                val dateStr = Instant.ofEpochMilli(adv.settledAt)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                    .format(dateFmt)
                                Surface(
                                    shape = ListItemDefaults.segmentedShapes(idx, summary.advances.size).shape,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${adv.fromMemberName} → ${adv.toMemberName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Trip Fund advance · $dateStr",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = formatIndianRupeesFromCents(adv.amountCents, currencySymbol = currencySymbol),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        ExpressivePlainTooltipBox(tooltipText = "Remove advance") {
                                            IconButton(
                                                onClick = {
                                                    haptics.feel(Feel.Tick)
                                                    onRemoveAdvance(adv.settlementId)
                                                },
                                                shapes = IconButtonDefaults.shapes()
                                            ) {
                                                SymbolIcon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = "Remove advance",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripFundMetricTile(
    label: String,
    amount: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Batch **Collect Trip Fund** modal bottom sheet (2-tap Indian UPI workflow).
 *
 * Anyone in the group can record upfront advances:
 * 1. Choose who is holding the Trip Fund (Treasurer).
 * 2. Enter the default amount per person (with optional per-member override).
 * 3. Check off who has already transferred (supports partial contributors cleanly).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun CollectTripFundSheet(
    members: List<GroupMemberEntity>,
    currencySymbol: String,
    onRecordAdvances: (treasurerMemberId: String, contributionsByMemberId: Map<String, Long>) -> String?,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultTreasurerId = remember(members) {
        members.firstOrNull { it.isCurrentUser }?.memberId ?: members.firstOrNull()?.memberId.orEmpty()
    }
    var treasurerId by rememberSaveable(members.map { it.memberId }.joinToString(",")) {
        mutableStateOf(defaultTreasurerId)
    }
    var defaultAmountInput by rememberSaveable { mutableStateOf("") }
    var selectedContributorIds by remember(treasurerId, members) {
        mutableStateOf(members.filter { it.memberId != treasurerId }.map { it.memberId }.toSet())
    }
    var customAmountByMember by remember(treasurerId) {
        mutableStateOf<Map<String, String>>(emptyMap())
    }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val otherMembers = remember(members, treasurerId) {
        members.filter { it.memberId != treasurerId }
    }

    val resolvedContributions: Map<String, Long> = remember(
        otherMembers,
        selectedContributorIds,
        defaultAmountInput,
        customAmountByMember
    ) {
        otherMembers
            .filter { it.memberId in selectedContributorIds }
            .mapNotNull { member ->
                val raw = customAmountByMember[member.memberId]?.takeIf { it.isNotBlank() } ?: defaultAmountInput
                val cents = parseRupeesToPaiseExact(raw)?.takeIf { it > 0L } ?: return@mapNotNull null
                member.memberId to cents
            }
            .toMap()
    }
    val totalBatchCents = resolvedContributions.values.sum()

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Collect Trip Fund",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "Record upfront cash or UPI advances. Never inflates Total Trip Spend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ExpressivePlainTooltipBox(tooltipText = "Close") {
                    IconButton(
                        onClick = closeSheet,
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        SymbolIcon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 1. Choose Treasurer
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Who is holding the Trip Fund?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.selectableGroup()
                ) {
                    members.forEach { member ->
                        val isSelected = member.memberId == treasurerId
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptics.feel(Feel.Tick)
                                treasurerId = member.memberId
                                errorMessage = null
                            },
                            shapes = FilterChipDefaults.shapes(),
                            leadingIcon = if (isSelected) {
                                {
                                    SymbolIcon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else {
                                null
                            },
                            label = {
                                Text(
                                    text = if (member.isCurrentUser) "${member.name} (You)" else member.name,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        )
                    }
                }
            }

            // 2. Default Amount per person
            OutlinedTextField(
                value = defaultAmountInput,
                onValueChange = {
                    defaultAmountInput = sanitizeDecimalRupeesInput(it)
                    errorMessage = null
                },
                label = { Text("Amount per person ($currencySymbol)") },
                placeholder = { Text("e.g. 5000") },
                prefix = { Text("$currencySymbol ") },
                singleLine = true,
                isError = errorMessage != null,
                supportingText = errorMessage?.let { msg -> { Text(msg) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        errorMessage?.let { error(it) }
                    }
            )

            // 3. Multi-select contributors (supports partial contributors & per-person overrides)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Who has transferred? (${selectedContributorIds.size} of ${otherMembers.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                ) {
                    otherMembers.forEachIndexed { idx, member ->
                        val isChecked = member.memberId in selectedContributorIds
                        val effectiveAmountText = customAmountByMember[member.memberId] ?: defaultAmountInput
                        SegmentedListItem(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                haptics.feel(Feel.Tick)
                                selectedContributorIds = if (checked) {
                                    selectedContributorIds + member.memberId
                                } else {
                                    selectedContributorIds - member.memberId
                                }
                                errorMessage = null
                            },
                            shapes = ListItemDefaults.segmentedShapes(index = idx, count = otherMembers.size),
                            colors = ListItemDefaults.segmentedColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            ),
                            leadingContent = {
                                Checkbox(checked = isChecked, onCheckedChange = null)
                            },
                            trailingContent = {
                                if (isChecked) {
                                    OutlinedTextField(
                                        value = effectiveAmountText,
                                        onValueChange = { raw ->
                                            customAmountByMember = customAmountByMember +
                                                (member.memberId to sanitizeDecimalRupeesInput(raw))
                                            errorMessage = null
                                        },
                                        singleLine = true,
                                        prefix = { Text(currencySymbol, style = MaterialTheme.typography.labelSmall) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(104.dp)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (member.isCurrentUser) "${member.name} (You)" else member.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 4. Primary CTA
            Button(
                onClick = {
                    val resultError = onRecordAdvances(treasurerId, resolvedContributions)
                    if (resultError != null) {
                        haptics.feel(Feel.Reject)
                        errorMessage = resultError
                    } else {
                        haptics.feel(Feel.Confirm)
                        closeSheet()
                    }
                },
                enabled = resolvedContributions.isNotEmpty() && totalBatchCents > 0L,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                val count = resolvedContributions.size
                val label = if (count > 0 && totalBatchCents > 0L) {
                    "Add $count ${if (count == 1) "advance" else "advances"} (${formatIndianRupeesFromCents(totalBatchCents, currencySymbol = currencySymbol)})"
                } else {
                    "Add Trip Fund advances"
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/** Compact Expressive button to open [CollectTripFundSheet] from Trip Hub Money tab. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CollectTripFundButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    OutlinedButton(
        onClick = {
            haptics.feel(Feel.Tick)
            onClick()
        },
        shapes = ButtonDefaults.shapes(),
        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
        modifier = modifier
    ) {
        SymbolIcon(
            imageVector = Icons.Rounded.Savings,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        Text(
            text = "Collect Trip Fund",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
