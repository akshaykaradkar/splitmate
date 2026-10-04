package com.splitmate.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.data.TripLifecycleRecord
import com.splitmate.app.data.TripLifecycleResolver
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.FigtreeFontFamily
import com.splitmate.app.ui.formatIndianRupeesFromCents
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v2.3.5 (#1) trip lifecycle UI for Trip Hub.
 *
 * - ENDED: "Trip wrapped up" summary (total, per-person, days, top categories, settle-up state).
 * - ENDED_SUGGESTED: organizer-only nudge to wrap up (never auto-ends).
 * - ACTIVE: organizer-only compact "Trip over? Wrap it up" row.
 * Only organizers can wrap up / reopen; everyone else sees read-only info.
 */
data class TripWrapUpStats(
    val totalSpentCents: Long,
    val memberCount: Int,
    val days: Int,
    val topCategories: List<Pair<String, Long>>,
    val isAllSettled: Boolean
) {
    /** Display-only average; never used for money math. */
    val perPersonCents: Long get() = if (memberCount > 0) totalSpentCents / memberCount else 0L
}

/** Pure: top spend buckets (label, cents) for the wrap-up summary. */
fun computeTripTopCategories(
    classified: List<Pair<com.splitmate.app.data.ExpenseEntity, TripHubBookingCategory>>,
    limit: Int = 3
): List<Pair<String, Long>> =
    classified
        .groupBy { it.second }
        .map { (cat, rows) -> cat.filterTitle to rows.sumOf { it.first.totalAmountCents } }
        .filter { it.second > 0L }
        .sortedWith(compareByDescending<Pair<String, Long>> { it.second }.thenBy { it.first })
        .take(limit)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripLifecycleCard(
    state: TripLifecycleResolver.State,
    record: TripLifecycleRecord?,
    endedByName: String?,
    isOrganizer: Boolean,
    stats: TripWrapUpStats,
    onWrapUp: () -> Unit,
    onReopen: () -> Unit,
    onSettleUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = DesignSystemBindings.activePalette
    var confirmWrapUp by remember { mutableStateOf(false) }
    var confirmReopen by remember { mutableStateOf(false) }
    var suggestionDismissed by rememberSaveable(state) { mutableStateOf(false) }

    when (state) {
        TripLifecycleResolver.State.ENDED -> {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.primaryContainer,
                modifier = modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Flag, contentDescription = null, tint = palette.onPrimaryContainer, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Trip wrapped up",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = palette.onPrimaryContainer,
                                modifier = Modifier.semantics { heading() }
                            )
                            val endedLine = buildString {
                                if ((record?.endedAtEpochMs ?: 0L) > 0L) {
                                    append("Ended ")
                                    append(SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(record!!.endedAtEpochMs)))
                                }
                                if (!endedByName.isNullOrBlank()) append(" by $endedByName")
                            }
                            if (endedLine.isNotBlank()) {
                                Text(endedLine, fontFamily = FigtreeFontFamily, fontSize = 12.sp, color = palette.onPrimaryContainer.copy(alpha = 0.8f))
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        WrapUpStat("Total spent", formatIndianRupeesFromCents(stats.totalSpentCents, includePlusSign = false), Modifier.weight(1f))
                        WrapUpStat("Per person", formatIndianRupeesFromCents(stats.perPersonCents, includePlusSign = false), Modifier.weight(1f))
                        WrapUpStat("Days", stats.days.toString(), Modifier.weight(0.6f))
                    }
                    if (stats.topCategories.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            stats.topCategories.forEach { (label, cents) ->
                                Surface(shape = RoundedCornerShape(50), color = palette.surfaceContainerLowest) {
                                    Text(
                                        "$label · ${formatIndianRupeesFromCents(cents, includePlusSign = false)}",
                                        fontFamily = FigtreeFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = palette.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (stats.isAllSettled) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                            Text("Everyone is settled", fontFamily = FigtreeFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = palette.onPrimaryContainer, modifier = Modifier.weight(1f))
                        } else {
                            Surface(
                                onClick = onSettleUp,
                                shape = RoundedCornerShape(50),
                                color = palette.onSurface,
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Text(
                                    "Settle up →",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = palette.surfaceContainerLowest,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                        }
                        if (TripLifecycleResolver.canToggle(isOrganizer)) {
                            TextButton(onClick = { confirmReopen = true }) {
                                Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(16.dp), tint = palette.onPrimaryContainer)
                                Spacer(Modifier.width(4.dp))
                                Text("Reopen", fontFamily = FigtreeFontFamily, fontWeight = FontWeight.Bold, color = palette.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
        }

        TripLifecycleResolver.State.ENDED_SUGGESTED, TripLifecycleResolver.State.ENDING_SOON -> {
            if (TripLifecycleResolver.canToggle(isOrganizer) && !suggestionDismissed) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.tertiaryContainer,
                    modifier = modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Flag, contentDescription = null, tint = palette.onTertiaryContainer, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "No new expenses for a few days. Is the trip over?",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = palette.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { suggestionDismissed = true }) {
                            Text("Not yet", fontFamily = FigtreeFontFamily, color = palette.onTertiaryContainer)
                        }
                        TextButton(onClick = { confirmWrapUp = true }) {
                            Text("Wrap up", fontFamily = FigtreeFontFamily, fontWeight = FontWeight.ExtraBold, color = palette.onTertiaryContainer)
                        }
                    }
                }
            }
        }

        TripLifecycleResolver.State.ACTIVE -> {
            if (TripLifecycleResolver.canToggle(isOrganizer) && stats.totalSpentCents > 0L) {
                Surface(
                    onClick = { confirmWrapUp = true },
                    shape = RoundedCornerShape(50),
                    color = palette.surfaceContainer,
                    border = BorderStroke(1.dp, palette.outlineVariant),
                    modifier = modifier.heightIn(min = 40.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Flag, contentDescription = null, tint = palette.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Trip over? Wrap it up",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = palette.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (confirmWrapUp) {
        AlertDialog(
            onDismissRequest = { confirmWrapUp = false },
            title = { Text("Wrap up this trip?", fontFamily = FigtreeFontFamily, fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    "Everyone will see the trip as ended with a final summary. Nothing is deleted, balances stay the same, and late expenses can still be added. You can reopen it any time.",
                    fontFamily = FigtreeFontFamily,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmWrapUp = false; onWrapUp() }) {
                    Text("Wrap up", fontWeight = FontWeight.ExtraBold, color = palette.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmWrapUp = false }) { Text("Cancel", color = palette.onSurface) }
            },
            containerColor = palette.surfaceContainerLowest
        )
    }
    if (confirmReopen) {
        AlertDialog(
            onDismissRequest = { confirmReopen = false },
            title = { Text("Reopen this trip?", fontFamily = FigtreeFontFamily, fontWeight = FontWeight.ExtraBold) },
            text = { Text("The trip goes back to active for everyone.", fontFamily = FigtreeFontFamily, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { confirmReopen = false; onReopen() }) {
                    Text("Reopen", fontWeight = FontWeight.ExtraBold, color = palette.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReopen = false }) { Text("Cancel", color = palette.onSurface) }
            },
            containerColor = palette.surfaceContainerLowest
        )
    }
}

@Composable
private fun WrapUpStat(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = DesignSystemBindings.activePalette
    Surface(shape = RoundedCornerShape(16.dp), color = palette.surfaceContainerLowest, modifier = modifier) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, fontFamily = FigtreeFontFamily, fontSize = 11.sp, color = palette.onSurfaceVariant, maxLines = 1)
            Text(
                value,
                fontFamily = FigtreeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = palette.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * v2.3.5 (#1): confirmation shown before logging an expense on a wrapped-up trip.
 * Late expenses are allowed (approved), they just need an explicit "Add anyway".
 */
@Composable
fun LateExpenseConfirmDialog(
    tripName: String,
    onAddAnyway: () -> Unit,
    onCancel: () -> Unit
) {
    val palette = DesignSystemBindings.activePalette
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Rounded.Flag, contentDescription = null, tint = palette.secondary) },
        title = { Text("$tripName is wrapped up", fontFamily = FigtreeFontFamily, fontWeight = FontWeight.ExtraBold) },
        text = {
            Text(
                "This trip has ended. You can still add a late expense and balances will update for everyone.",
                fontFamily = FigtreeFontFamily,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onAddAnyway) { Text("Add anyway", fontWeight = FontWeight.ExtraBold, color = palette.secondary) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Go back", color = palette.onSurface) }
        },
        containerColor = palette.surfaceContainerLowest
    )
}
