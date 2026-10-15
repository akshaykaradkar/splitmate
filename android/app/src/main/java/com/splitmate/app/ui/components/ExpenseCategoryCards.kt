package com.splitmate.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.LocalActivity
import androidx.compose.material.icons.rounded.LocalMall
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.splitmate.app.ui.SplitMateTnumMonospace
import com.splitmate.app.ui.extractInitialsFromNameOrSeed
import com.splitmate.app.ui.theme.ExpenseCardKind
import com.splitmate.app.ui.theme.ExpenseCategoryCardColors
import com.splitmate.app.ui.theme.HubExtendedColors

// ==============================================================================
// v2.4.0 rc10: Money tab category cards (Stitch design, Phase 2)
// ==============================================================================
//
// One card family per category instead of the old "card sandwich" (full ticket art + a
// separate Edit/Delete row + in-card delete). Shared rules:
//  - 1dp category outline, no shadow (GEMINI.md card rule), 26dp master radius.
//  - Category colours come from [HubExtendedColors.expenseCategoryCard] (light + dark); body
//    text uses theme roles, so cards follow all three themes.
//  - Type uses the existing M3E roles; amounts use the same tabular style as the Trip Hub cards.
//  - Icons are Material Symbols ([SymbolIcon]); actions are official AssistChips (48dp target).
//  - Tapping the card opens the shared expense sheet (split details, edit, history) via
//    [onOpenDetails]; swipe-to-manage is applied by the caller around single-expense cards.

// Shapes come from the theme's approved corner hierarchy: extraLarge (28dp, closest to the
// Stitch 26dp master radius) for the card, medium (16dp) for inner wells.
private val RadiusCardMaster @Composable get() = MaterialTheme.shapes.extraLarge
private val RadiusInnerWell @Composable get() = MaterialTheme.shapes.medium

/** Tabular money style (Figtree tnum, ExtraBold) on the titleMedium role. */
private val MoneyAmountStyle: TextStyle
    @Composable get() = tnum(MaterialTheme.typography.titleMedium, FontWeight.ExtraBold)

@Composable
private fun tnum(style: TextStyle, weight: FontWeight): TextStyle =
    style.merge(TextStyle(fontFamily = SplitMateTnumMonospace, fontWeight = weight, fontFeatureSettings = "tnum"))

// ------------------------------------------------------------------ shared frame

@Composable
private fun CategoryCardFrame(
    kind: ExpenseCardKind,
    icon: ImageVector,
    categoryLabel: String,
    headerSubtitle: String,
    accessibilitySummary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    headerTrailing: (@Composable () -> Unit)? = null,
    body: @Composable ColumnScope.(ExpenseCategoryCardColors) -> Unit
) {
    val colors = HubExtendedColors.expenseCategoryCard(kind)
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onClick()
        },
        shape = RadiusCardMaster,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.outline),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilitySummary }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(colors.headerContainer, colors.headerContainerEnd)))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    SymbolIcon(imageVector = icon, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(19.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.accent,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = headerSubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (headerTrailing != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    headerTrailing()
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                body(colors)
            }
        }
    }
}

/** Title + subtitle on the left, total + per-person on the right. */
@Composable
private fun TitleAndAmountRow(
    title: String,
    subtitle: String?,
    total: String,
    perPerson: String?,
    subtitleIcon: ImageVector? = null,
    subtitleIconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.padding(top = 2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (subtitleIcon != null) {
                        SymbolIcon(imageVector = subtitleIcon, contentDescription = null, tint = subtitleIconTint, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(text = total, style = MoneyAmountStyle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            if (!perPerson.isNullOrBlank()) {
                Spacer(modifier = Modifier.padding(top = 2.dp))
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Text(
                        text = perPerson,
                        style = tnum(MaterialTheme.typography.labelSmall, FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/** Payer avatar + "Paid by …" line. */
@Composable
private fun PayerLine(text: String, payerName: String, modifier: Modifier = Modifier, multiplePayers: Boolean = false) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (multiplePayers) {
                // Several people paid: a group glyph, the names and amounts follow in the text.
                SymbolIcon(imageVector = Icons.Rounded.Group, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
            } else {
                Text(
                    text = extractInitialsFromNameOrSeed(payerName).take(1),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Official M3 AssistChip with a Material Symbol (min 48dp touch target from the library). */
@Composable
private fun CardActionChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    AssistChip(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onClick()
        },
        label = { Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) },
        leadingIcon = { SymbolIcon(imageVector = icon, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            labelColor = MaterialTheme.colorScheme.onSurface,
            leadingIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outlineVariant)
    )
}

/** Small tonal status pill (e.g. "Booked", "Equal split"). */
@Composable
private fun StatusPill(text: String, colors: ExpenseCategoryCardColors) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLowest, border = BorderStroke(1.dp, colors.outline)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = colors.accent,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

// ------------------------------------------------------------------ 1. Stays & lodging

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StaysLodgingCard(
    stay: StayExpenseData,
    modifier: Modifier = Modifier,
    onOpenDetails: () -> Unit = {},
    onViewLocationClick: () -> Unit = {},
    onSplitDetailsClick: () -> Unit = {}
) {
    val nightsText = stay.nightsCount?.let { if (it == 1) "1 night" else "$it nights" }
    CategoryCardFrame(
        kind = ExpenseCardKind.STAY,
        icon = Icons.Rounded.Hotel,
        categoryLabel = "Stays",
        headerSubtitle = listOfNotNull(nightsText, "${stay.travelerCount} sharing", stay.confirmationCode?.let { "Ref $it" }).joinToString(" · "),
        accessibilitySummary = "Stay, ${stay.resortName}, ${stay.totalAmountFormatted}, paid by ${stay.payerName}",
        onClick = onOpenDetails,
        modifier = modifier
    ) { colors ->
        TitleAndAmountRow(
            title = stay.resortName,
            subtitle = stay.locationTag,
            total = stay.totalAmountFormatted,
            perPerson = stay.perPersonFormatted,
            subtitleIcon = Icons.Rounded.Place,
            subtitleIconTint = colors.accent
        )
        Spacer(modifier = Modifier.padding(top = 14.dp))
        // Check-in / check-out strip (check-out only when the nights count is known).
        Surface(
            shape = RadiusInnerWell,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StayDateColumn(label = "Check-in", value = stay.checkInDate, alignEnd = false)
                if (nightsText != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            SymbolIcon(imageVector = Icons.Rounded.Bedtime, contentDescription = null, tint = colors.accent, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = nightsText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                if (stay.checkOutDate != null) {
                    StayDateColumn(label = "Check-out", value = stay.checkOutDate, alignEnd = true)
                }
            }
        }
        Spacer(modifier = Modifier.padding(top = 12.dp))
        PayerLine(text = "Paid by ${stay.payerName} · ${stay.splitSummary}", payerName = stay.payerName)
        Spacer(modifier = Modifier.padding(top = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardActionChip(label = "Maps", icon = Icons.Rounded.Map, onClick = onViewLocationClick)
            CardActionChip(label = "Split details", icon = Icons.AutoMirrored.Rounded.ReceiptLong, onClick = onSplitDetailsClick)
        }
    }
}

@Composable
private fun StayDateColumn(label: String, value: String, alignEnd: Boolean) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = tnum(MaterialTheme.typography.labelLarge, FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface)
    }
}

// ------------------------------------------------------------------ 2. Cabs, rides & rentals batch

/**
 * All cab and rental expenses of the trip in ONE card: total, ride count and a micro-timeline.
 * Shows [TransitBatchDefaults.COLLAPSED_HOPS] rides; "View all" expands the rest in place
 * (spatial spring for size, effects spring for the fade). Each ride opens its own expense sheet.
 */
@Composable
fun CabsTransitBatchCard(
    batch: TransitBatchData,
    modifier: Modifier = Modifier,
    onHopClick: (TransitHopItem) -> Unit = {}
) {
    val allRentals = batch.hops.isNotEmpty() && batch.hops.all { it.isRental }
    BatchCard(
        stateKey = batch.id,
        kind = ExpenseCardKind.TRANSIT,
        icon = if (allRentals) Icons.Rounded.TwoWheeler else Icons.Rounded.DirectionsCar,
        title = batch.batchTitle,
        subtitle = batch.batchSubtitle,
        payerName = batch.payerName,
        payerSummary = batch.payerSummary,
        multiplePayers = batch.multiplePayers,
        payers = batch.payers,
        totalAmountFormatted = batch.totalAmountFormatted,
        count = batch.totalRidesCount,
        unitSingular = "ride",
        unitPlural = "rides",
        rows = batch.hops,
        modifier = modifier,
        onRowClick = onHopClick
    )
}

/**
 * v2.4.0 rc11: Food & dining / Activities / Shopping & other with 2+ expenses, in ONE card (same
 * layout as the cabs batch). Each row opens its own expense sheet (edit, undo, history).
 */
@Composable
fun CategoryBatchCard(
    batch: CategoryBatchData,
    modifier: Modifier = Modifier,
    onRowClick: (TransitHopItem) -> Unit = {}
) {
    BatchCard(
        stateKey = batch.id,
        kind = batch.kind,
        icon = when (batch.kind) {
            ExpenseCardKind.FOOD -> Icons.Rounded.Restaurant
            ExpenseCardKind.ACTIVITY -> Icons.Rounded.LocalActivity
            else -> if (batch.title == "Shopping") Icons.Rounded.LocalMall else Icons.AutoMirrored.Rounded.ReceiptLong
        },
        title = batch.title,
        subtitle = batch.subtitle,
        payerName = batch.payerName,
        payerSummary = batch.payerSummary,
        multiplePayers = batch.multiplePayers,
        payers = batch.payers,
        totalAmountFormatted = batch.totalAmountFormatted,
        count = batch.count,
        unitSingular = batch.unitSingular,
        unitPlural = batch.unitPlural,
        rows = batch.rows,
        modifier = modifier,
        onRowClick = onRowClick
    )
}

/**
 * Shared batch layout: category total in the header, payer line, then a micro-timeline of rows.
 * Shows [TransitBatchDefaults.COLLAPSED_HOPS] rows; "View all" expands the rest in place
 * (spatial spring for size, effects spring for the fade).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BatchCard(
    stateKey: String,
    kind: ExpenseCardKind,
    icon: ImageVector,
    title: String,
    subtitle: String,
    payerName: String,
    payerSummary: String,
    multiplePayers: Boolean,
    payers: List<PayerShare>,
    totalAmountFormatted: String,
    count: Int,
    unitSingular: String,
    unitPlural: String,
    rows: List<TransitHopItem>,
    modifier: Modifier = Modifier,
    onRowClick: (TransitHopItem) -> Unit
) {
    var isExpanded by rememberSaveable(stateKey) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val visibleRows = rows.take(TransitBatchDefaults.COLLAPSED_HOPS)
    val extraRows = rows.drop(TransitBatchDefaults.COLLAPSED_HOPS)
    val countText = if (count == 1) "1 $unitSingular" else "$count $unitPlural"

    CategoryCardFrame(
        kind = kind,
        icon = icon,
        categoryLabel = title,
        headerSubtitle = subtitle,
        accessibilitySummary = "$title, $countText, $totalAmountFormatted",
        // The batch has no single expense: a card tap toggles the list instead.
        onClick = { if (extraRows.isNotEmpty()) isExpanded = !isExpanded },
        modifier = modifier,
        headerTrailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(text = totalAmountFormatted, style = MoneyAmountStyle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(text = "Category total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    ) { colors ->
        if (multiplePayers && payers.isNotEmpty()) {
            // v2.4.0 rc11: several payers -> the summary is a button that opens "Who paid".
            var showPayers by rememberSaveable(stateKey) { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .heightIn(min = 48.dp)
                    .clickable(onClickLabel = "See who paid what") {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        showPayers = true
                    }
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically
            ) {
                PayerLine(text = payerSummary, payerName = payerName, multiplePayers = true, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(4.dp))
                SymbolIcon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            if (showPayers) {
                WhoPaidSheet(
                    title = title,
                    totalAmountFormatted = totalAmountFormatted,
                    unitSingular = unitSingular,
                    unitPlural = unitPlural,
                    payers = payers,
                    rows = rows,
                    accent = colors.accent,
                    onDismiss = { showPayers = false },
                    onRowClick = { row ->
                        showPayers = false
                        onRowClick(row)
                    }
                )
            }
        } else {
            PayerLine(text = payerSummary, payerName = payerName, multiplePayers = multiplePayers)
        }
        Spacer(modifier = Modifier.padding(top = 6.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        visibleRows.forEachIndexed { index, row ->
            TransitHopRow(hop = row, colors = colors, onClick = { onRowClick(row) })
            if (index < visibleRows.lastIndex || (isExpanded && extraRows.isNotEmpty())) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), modifier = Modifier.padding(start = 20.dp))
            }
        }
        AnimatedVisibility(
            visible = isExpanded && extraRows.isNotEmpty(),
            enter = expandVertically(animationSpec = SplitMateMotion.defaultSpatial()) + fadeIn(animationSpec = SplitMateMotion.defaultEffects()),
            exit = shrinkVertically(animationSpec = SplitMateMotion.defaultSpatial()) + fadeOut(animationSpec = SplitMateMotion.defaultEffects())
        ) {
            Column {
                extraRows.forEachIndexed { index, row ->
                    TransitHopRow(hop = row, colors = colors, onClick = { onRowClick(row) })
                    if (index < extraRows.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), modifier = Modifier.padding(start = 20.dp))
                    }
                }
            }
        }
        if (extraRows.isNotEmpty()) {
            Spacer(modifier = Modifier.padding(top = 8.dp))
            FilledTonalButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    isExpanded = !isExpanded
                },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(
                    text = if (isExpanded) "Show fewer $unitPlural" else "View all $count $unitPlural",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * v2.4.0 rc11: "Who paid" for a batch with several payers: each person's subtotal and their entries.
 * Display only (balances still come from the splits). Tapping an entry opens its expense sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhoPaidSheet(
    title: String,
    totalAmountFormatted: String,
    unitSingular: String,
    unitPlural: String,
    payers: List<PayerShare>,
    rows: List<TransitHopItem>,
    accent: androidx.compose.ui.graphics.Color,
    onDismiss: () -> Unit,
    onRowClick: (TransitHopItem) -> Unit
) {
    val rowById = remember(rows) { rows.associateBy { it.expenseId } }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            item(key = "who_paid_header") {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Text(
                        text = "Who paid",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "$title · $totalAmountFormatted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            payers.forEachIndexed { index, payer ->
                item(key = "payer_$index") {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = extractInitialsFromNameOrSeed(payer.name).take(1),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = payer.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Paid for ${if (payer.count == 1) "1 $unitSingular" else "${payer.count} $unitPlural"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(text = payer.totalFormatted, style = MoneyAmountStyle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                            }
                            payer.expenseIds.mapNotNull { rowById[it] }.forEach { row ->
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), modifier = Modifier.padding(top = 4.dp))
                                // The payer is the section header, so the row doesn't repeat "Paid by".
                                TransitHopRow(hop = row.copy(payerName = null), colors = null, accent = accent, onClick = { onRowClick(row) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransitHopRow(
    hop: TransitHopItem,
    colors: ExpenseCategoryCardColors?,
    onClick: () -> Unit,
    accent: androidx.compose.ui.graphics.Color = colors?.accent ?: MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accent))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = hop.timeLabel,
            style = tnum(MaterialTheme.typography.labelSmall, FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = hop.destination,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // v2.4.0 rc11: mixed-payer batches name the payer on every row.
            if (hop.payerName != null) {
                Text(
                    text = "Paid by ${hop.payerName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = hop.amountFormatted, style = tnum(MaterialTheme.typography.bodyMedium, FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface)
    }
}

// ------------------------------------------------------------------ 3. Food & dining

@Composable
fun FoodDiningCard(
    food: FoodDiningData,
    modifier: Modifier = Modifier,
    onOpenDetails: () -> Unit = {},
    onSplitReceiptClick: () -> Unit = {}
) {
    CategoryCardFrame(
        kind = ExpenseCardKind.FOOD,
        icon = Icons.Rounded.Restaurant,
        categoryLabel = "Food & dining",
        headerSubtitle = listOfNotNull(food.mealType, food.locationName, food.dateLabel).joinToString(" · "),
        accessibilitySummary = "Food, ${food.restaurantName}, ${food.totalAmountFormatted}, paid by ${food.payerName}",
        onClick = onOpenDetails,
        modifier = modifier,
        headerTrailing = { StatusPill(text = if (food.isEqualSplit) "Equal split" else "Custom split", colors = HubExtendedColors.expenseCategoryCard(ExpenseCardKind.FOOD)) }
    ) { _ ->
        TitleAndAmountRow(title = food.restaurantName, subtitle = null, total = food.totalAmountFormatted, perPerson = food.perPersonFormatted)
        Spacer(modifier = Modifier.padding(top = 12.dp))
        PayerLine(text = "Paid by ${food.payerName} · ${food.splitCount} sharing", payerName = food.payerName)
        Spacer(modifier = Modifier.padding(top = 8.dp))
        CardActionChip(label = "Split details", icon = Icons.AutoMirrored.Rounded.ReceiptLong, onClick = onSplitReceiptClick)
    }
}

// ------------------------------------------------------------------ 4. Activities & tours

@Composable
fun ActivityTourCard(
    activity: ActivityTourData,
    modifier: Modifier = Modifier,
    onOpenDetails: () -> Unit = {},
    onSplitDetailsClick: () -> Unit = {}
) {
    CategoryCardFrame(
        kind = ExpenseCardKind.ACTIVITY,
        icon = Icons.Rounded.LocalActivity,
        categoryLabel = "Activities",
        headerSubtitle = listOfNotNull(activity.operatorName, activity.dateLabel).joinToString(" · "),
        accessibilitySummary = "Activity, ${activity.activityTitle}, ${activity.totalAmountFormatted}, paid by ${activity.payerName}",
        onClick = onOpenDetails,
        modifier = modifier,
        headerTrailing = {
            Text(
                text = if (activity.peopleCount == 1) "1 person" else "${activity.peopleCount} people",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = HubExtendedColors.expenseCategoryCard(ExpenseCardKind.ACTIVITY).accent
            )
        }
    ) { _ ->
        TitleAndAmountRow(title = activity.activityTitle, subtitle = null, total = activity.totalAmountFormatted, perPerson = activity.perPersonFormatted)
        Spacer(modifier = Modifier.padding(top = 12.dp))
        PayerLine(text = "Paid by ${activity.payerName}", payerName = activity.payerName)
        Spacer(modifier = Modifier.padding(top = 8.dp))
        CardActionChip(label = "Split details", icon = Icons.AutoMirrored.Rounded.ReceiptLong, onClick = onSplitDetailsClick)
    }
}

// ------------------------------------------------------------------ 5. Shopping & other

@Composable
fun SundriesExpenseCard(
    item: SundriesExpenseData,
    modifier: Modifier = Modifier,
    onOpenDetails: () -> Unit = {},
    onSplitDetailsClick: () -> Unit = {}
) {
    CategoryCardFrame(
        kind = ExpenseCardKind.SUNDRIES,
        icon = if (item.categoryLabel == "Shopping") Icons.Rounded.LocalMall else Icons.AutoMirrored.Rounded.ReceiptLong,
        categoryLabel = item.categoryLabel,
        headerSubtitle = item.dateLabel,
        accessibilitySummary = "${item.categoryLabel}, ${item.title}, ${item.totalAmountFormatted}, paid by ${item.payerName}",
        onClick = onOpenDetails,
        modifier = modifier
    ) { _ ->
        TitleAndAmountRow(title = item.title, subtitle = null, total = item.totalAmountFormatted, perPerson = item.perPersonFormatted)
        Spacer(modifier = Modifier.padding(top = 12.dp))
        PayerLine(text = "Paid by ${item.payerName} · ${item.splitCount} sharing", payerName = item.payerName)
        Spacer(modifier = Modifier.padding(top = 8.dp))
        CardActionChip(label = "Split details", icon = Icons.AutoMirrored.Rounded.ReceiptLong, onClick = onSplitDetailsClick)
    }
}
