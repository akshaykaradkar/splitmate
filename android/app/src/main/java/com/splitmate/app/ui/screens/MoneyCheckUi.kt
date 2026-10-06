package com.splitmate.app.ui.screens

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.splitmate.app.MoneyCheck
import com.splitmate.app.MoneyCheckCopy
import com.splitmate.app.MoneyCheckSyncStatus
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.ui.SplitMateTnumMonospace
import com.splitmate.app.ui.components.LocalMotionScheme
import com.splitmate.app.ui.components.MaterialShapes
import com.splitmate.app.ui.components.RoundedPolygonShape
import com.splitmate.app.ui.components.SettledCelebrationBadge
import com.splitmate.app.ui.components.rememberAnimatedSegmentedIslandItemShape
import com.splitmate.app.ui.formatIndianRupeesFromCents

/*
 * v2.3.6 P3 "Money check" (RCA: Bike Rentals ₹7,950 total vs ₹4,950 shares).
 *
 * Design (audited, M3 Expressive + Stitch "low-arousal financial feedback"):
 * - Passed: no extra row on the Money tab. One quiet status row lives inside the existing
 *   "Smart settle up" card ([MoneyCheckStatusRow]); tapping it opens [MoneyCheckSheet].
 * - Failed: one calm, non-dismissable errorContainer card at the top of Money
 *   ([MoneyCheckFailureCard]), a one-line hold-off note above the settle plan, an error dot on the
 *   Money tab, and a warning inside every "Mark paid" confirmation.
 * - Expense sheet: [ExpenseSharesCheckRow] shows "Shares add up" or the gap with a 1-tap fix.
 * - Icon + text for every state (never colour alone); spring motion via LocalMotionScheme, which
 *   snaps under reduced motion. No wavy progress near money; sync state is text only.
 */

private val tnum = TextStyle(fontFamily = SplitMateTnumMonospace, fontFeatureSettings = "tnum")

/** Sage "verified" or error chip on the avatar-family Cookie shape. */
@Composable
private fun MoneyCheckGlyph(passed: Boolean, size: androidx.compose.ui.unit.Dp = 32.dp) {
    val cookie = remember { RoundedPolygonShape(MaterialShapes.Cookie9Sided) }
    val container = if (passed) TripHubTokens.PositiveSagePillBg else MaterialTheme.colorScheme.errorContainer
    val tint = if (passed) TripHubTokens.PositiveSageText else MaterialTheme.colorScheme.onErrorContainer
    Box(
        modifier = Modifier
            .size(size)
            .clip(cookie)
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (passed) Icons.Rounded.Verified else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

/**
 * The quiet status row inside the "Smart settle up" card. When the check turns from failing to
 * passing, the badge morphs Cookie9Sided → SoftBurst once on a bouncy spring (with a confirm haptic).
 */
@Composable
fun MoneyCheckStatusRow(
    report: MoneyCheck.Report,
    sync: MoneyCheckSyncStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = MoneyCheckCopy.statusTitle(report, sync)
    var wasFailing by remember { mutableStateOf(!report.passed) }
    var celebrate by remember { mutableStateOf(false) }
    LaunchedEffect(report.passed) {
        if (report.passed && wasFailing) celebrate = true
        wasFailing = !report.passed
    }
    val morph = remember { Morph(MaterialShapes.Cookie9Sided, MaterialShapes.SoftBurst) }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                role = Role.Button
                liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                contentDescription = "$title. Double tap for details."
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (report.passed && celebrate) {
                SettledCelebrationBadge(
                    celebrate = true,
                    containerColor = TripHubTokens.PositiveSagePillBg,
                    iconTint = TripHubTokens.PositiveSageText,
                    size = 32.dp,
                    morph = morph
                )
            } else {
                MoneyCheckGlyph(passed = report.passed)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The calm, non-dismissable card shown at the top of Money while any check fails.
 * [canFix] is true for the organizer / payer / creator of the first failing expense.
 */
@Composable
fun MoneyCheckFailureCard(
    report: MoneyCheck.Report,
    titleOf: (String) -> String,
    canFix: Boolean,
    onReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = MoneyCheckCopy.failureTitle(report, titleOf)
    val body = MoneyCheckCopy.failureBody(report)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(text = body, style = MaterialTheme.typography.bodyMedium.merge(tnum))
            if (!canFix) {
                Text(
                    text = MoneyCheckCopy.ASK_TO_FIX,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                FilledTonalButton(onClick = onReview, shape = MaterialTheme.shapes.medium) {
                    Text(text = if (canFix) "Review" else "View", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** One line above the settle plan while a check fails. */
@Composable
fun MoneyCheckSettleHoldNote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = MoneyCheckCopy.HOLD_OFF,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class CheckRow(val passed: Boolean, val title: String, val detail: String)

/** The full checklist (segmented list), opened from [MoneyCheckStatusRow] or the failure card. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoneyCheckSheet(
    report: MoneyCheck.Report,
    sync: MoneyCheckSyncStatus,
    titleOf: (String) -> String,
    onOpenExpense: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val main = listOf(
        CheckRow(report.everyExpenseAddsUp, "Every expense adds up", MoneyCheckCopy.expensesDetail(report)),
        CheckRow(report.balancesEvenOut, "Balances even out", MoneyCheckCopy.balancesDetail(report)),
        CheckRow(report.noPaymentCountedTwice, "No payment counted twice", MoneyCheckCopy.paymentsDetail(report))
    )
    val details = listOf(
        CheckRow(
            report.totalPaidCents == report.totalSharedCents,
            "Total paid matches total shared",
            if (report.totalPaidCents == report.totalSharedCents) formatIndianRupeesFromCents(report.totalPaidCents)
            else "${formatIndianRupeesFromCents(report.totalPaidCents)} paid · ${formatIndianRupeesFromCents(report.totalSharedCents)} shared"
        ),
        CheckRow(
            report.issues.none { it.kind == MoneyCheck.IssueKind.PERSON_NOT_IN_TRIP },
            "Everyone is in the trip",
            "Every payer and share belongs to a member"
        ),
        CheckRow(
            report.paymentsValid && report.issues.none {
                it.kind == MoneyCheck.IssueKind.NON_POSITIVE_TOTAL || it.kind == MoneyCheck.IssueKind.NEGATIVE_SHARE
            },
            "Amounts look right",
            "No zero or negative amounts"
        )
    )
    val motion = LocalMotionScheme.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MoneyCheckGlyph(passed = report.passed, size = 40.dp)
                Column {
                    Text(
                        text = MoneyCheckCopy.sheetTitle(report),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Money check",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (!report.passed) {
                val fixable = report.issues.filter { it.expenseId != null }
                fixable.forEachIndexed { idx, issue ->
                    val shape = rememberAnimatedSegmentedIslandItemShape(
                        index = idx, totalCount = fixable.size, outerCorner = 16.dp, innerCorner = 4.dp
                    )
                    Surface(
                        shape = shape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { issue.expenseId?.let(onOpenExpense) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp).defaultMinSize(minHeight = 40.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = issue.expenseId?.let(titleOf) ?: issue.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = MoneyCheckCopy.issueLine(issue),
                                    style = MaterialTheme.typography.bodySmall.merge(tnum)
                                )
                            }
                            Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null)
                        }
                    }
                }
                if (fixable.isNotEmpty()) Spacer(modifier = Modifier.height(14.dp))
            }

            CheckList(rows = main)
            TextButton(
                onClick = { showDetails = !showDetails },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(text = if (showDetails) "Hide details" else "Show details", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = if (showDetails) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
            }
            AnimatedVisibility(
                visible = showDetails,
                enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
                exit = shrinkVertically(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec())
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    CheckList(rows = details)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            SyncFooter(sync)
        }
    }
}

@Composable
private fun ColumnScope.CheckList(rows: List<CheckRow>) {
    rows.forEachIndexed { idx, row ->
        val shape = rememberAnimatedSegmentedIslandItemShape(
            index = idx, totalCount = rows.size, outerCorner = 16.dp, innerCorner = 4.dp
        )
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "${row.title}. ${if (row.passed) "Passed" else "Needs a look"}. ${row.detail}"
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MoneyCheckGlyph(passed = row.passed, size = 28.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = row.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = row.detail,
                        style = MaterialTheme.typography.bodySmall.merge(tnum),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncFooter(sync: MoneyCheckSyncStatus) {
    val icon = when {
        sync.isOffline -> Icons.Rounded.CloudOff
        sync.hasPendingPush || sync.isSyncing -> Icons.Rounded.CloudUpload
        else -> Icons.Rounded.CloudDone
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(
            text = MoneyCheckCopy.syncLine(sync, System.currentTimeMillis()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Inside the expense sheet: a quiet "Shares add up" line, or the gap with the 1-tap fix.
 * [canModify] = organizer, payer or creator (the same rule as editing).
 */
@Composable
fun ExpenseSharesCheckRow(
    expense: ExpenseEntity,
    splits: List<ExpenseSplitEntity>,
    canModify: Boolean,
    onUseSharesTotal: () -> Unit,
    onEdit: () -> Unit
) {
    val rows = remember(expense.expenseId, splits) { splits.filter { it.expenseId == expense.expenseId } }
    val shares = rows.sumOf { it.finalOwedCents }
    if (rows.isNotEmpty() && shares == expense.totalAmountCents) {
        if (rows.count { it.finalOwedCents > 0L } <= 1) return
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Verified,
                contentDescription = null,
                tint = TripHubTokens.PositiveSageText,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Shares add up · ${formatIndianRupeesFromCents(shares)}",
                style = MaterialTheme.typography.labelMedium.merge(tnum),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    val canUseShares = MoneyCheck.canUseSharesTotal(expense, rows)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = if (rows.isEmpty()) "Nobody is sharing this expense"
                    else "Shares ${formatIndianRupeesFromCents(shares)} · Total ${formatIndianRupeesFromCents(expense.totalAmountCents)}",
                    style = MaterialTheme.typography.titleSmall.merge(tnum)
                )
            }
            if (rows.isNotEmpty()) {
                Text(
                    text = "Off by ${formatIndianRupeesFromCents(kotlin.math.abs(expense.totalAmountCents - shares))}. Balances use both numbers, so they are wrong until this is fixed.",
                    style = MaterialTheme.typography.bodySmall.merge(tnum)
                )
            }
            if (canModify) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (canUseShares) {
                        Button(onClick = onUseSharesTotal, shape = MaterialTheme.shapes.medium) {
                            Text(text = "Use shares total ${formatIndianRupeesFromCents(shares)}", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    OutlinedButton(onClick = onEdit, shape = MaterialTheme.shapes.medium) {
                        Text(text = "Edit expense", style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                Text(text = MoneyCheckCopy.ASK_TO_FIX, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
