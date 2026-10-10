package com.splitmate.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.splitmate.app.ExpenseVersioning
import com.splitmate.app.data.ExpenseRevisionEntity
import com.splitmate.app.ui.a11y.SpokenMoney
import com.splitmate.app.ui.formatIndianRupeesFromCents
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v2.4.0 P4: what changed on an expense, in plain words. Pure (unit-tested); no money math.
 * The word "version" never appears on screen.
 */
object ExpenseHistoryPresentation {

    data class Change(
        val atEpochMs: Long,
        val byMemberId: String?,
        val oldTotalCents: Long?,
        val newTotalCents: Long,
        val oldPayerId: String?,
        val newPayerId: String,
        val kind: String
    ) {
        val isRevert: Boolean get() = kind == "revert"
        val isConcurrent: Boolean get() = kind == "concurrent"
    }

    private val shownKinds = setOf("local", "remote", "legacy", "revert", "concurrent")

    /** Newest first. Seeds and losing copies are not shown, but they are the "before" amounts. */
    fun changes(revisions: List<ExpenseRevisionEntity>): List<Change> {
        // Entries received from other phones carry a storage prefix; they display like any other.
        val ordered = revisions
            .map { it.copy(kind = com.splitmate.app.data.ExpenseVersionSync.baseKind(it.kind)) }
            .distinctBy { "${it.rowVersion}|${it.contentHash}|${it.kind}" }
            .sortedWith(compareBy<ExpenseRevisionEntity> { it.observedAtEpochMs }.thenBy { it.rowVersion })
        val baseline = ordered.filter { it.kind != "superseded" }
        val out = mutableListOf<Change>()
        var previous: ExpenseRevisionEntity? = null
        for (rev in baseline) {
            if (rev.kind in shownKinds && previous != null &&
                (previous.totalAmountCents != rev.totalAmountCents || previous.payerId != rev.payerId || rev.kind == "revert")
            ) {
                out += Change(
                    atEpochMs = rev.observedAtEpochMs,
                    byMemberId = rev.editedBy,
                    oldTotalCents = previous.totalAmountCents,
                    newTotalCents = rev.totalAmountCents,
                    oldPayerId = previous.payerId,
                    newPayerId = rev.payerId,
                    kind = rev.kind
                )
            }
            previous = rev
        }
        return out.reversed()
    }

    fun whoLabel(memberId: String?, names: Map<String, String>): String = when {
        memberId == null -> "Someone"
        memberId == ExpenseVersioning.LEGACY_EDITOR -> "An older app"
        else -> names[memberId] ?: "Someone"
    }

    fun whenLabel(epochMs: Long): String =
        SimpleDateFormat("d MMM, h:mm a", Locale.US).format(Date(epochMs)).replace("AM", "am").replace("PM", "pm")
}

/**
 * v2.4.0 P4: edit history inside the expense sheet.
 * - Collapsed: "Edited 2 Oct, 2:10 pm · Akshay   ₹7,950 → ₹4,950 >".
 * - Expanded: the last 5 changes, then "Show all".
 * - A change back to an older amount shows as a periwinkle heads-up with a one-tap restore.
 */
@Composable
fun ExpenseEditHistorySection(
    revisions: List<ExpenseRevisionEntity>,
    memberNames: Map<String, String>,
    canRestore: Boolean,
    onRestoreTotal: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val changes = ExpenseHistoryPresentation.changes(revisions)
    if (changes.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val latest = changes.first()
    val amountStyle = MaterialTheme.typography.labelLarge.merge(TextStyle(fontFeatureSettings = "tnum"))

    Column(modifier = modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (latest.isRevert && latest.oldTotalCents != null) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Changed back to ${formatIndianRupeesFromCents(latest.newTotalCents)} by " +
                            "${ExpenseHistoryPresentation.whoLabel(latest.byMemberId, memberNames)}'s phone · " +
                            "${ExpenseHistoryPresentation.whenLabel(latest.atEpochMs).substringBefore(",")}. " +
                            "This may be from an older copy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    if (canRestore) {
                        FilledTonalButton(onClick = { onRestoreTotal(latest.oldTotalCents) }) {
                            Text("Restore ${formatIndianRupeesFromCents(latest.oldTotalCents)}", style = amountStyle)
                        }
                    }
                }
            }
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { expanded = !expanded }
                        .semantics {
                            stateDescription = if (expanded) "Expanded" else "Collapsed"
                            contentDescription = "Edited ${ExpenseHistoryPresentation.whenLabel(latest.atEpochMs)} by " +
                                "${ExpenseHistoryPresentation.whoLabel(latest.byMemberId, memberNames)}. " +
                                (latest.oldTotalCents?.let { "From ${SpokenMoney.rupees(it)} to " } ?: "") +
                                "${SpokenMoney.rupees(latest.newTotalCents)}. ${changes.size} " +
                                (if (changes.size == 1) "change" else "changes")
                        }
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Edited ${ExpenseHistoryPresentation.whenLabel(latest.atEpochMs)} · " +
                                ExpenseHistoryPresentation.whoLabel(latest.byMemberId, memberNames),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        ChangeAmountLine(latest, amountStyle)
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (expanded) {
                    val visible = if (showAll) changes else changes.take(5)
                    visible.forEach { change ->
                        Column(Modifier.fillMaxWidth().padding(start = 30.dp)) {
                            Text(
                                text = "${ExpenseHistoryPresentation.whenLabel(change.atEpochMs)} · " +
                                    ExpenseHistoryPresentation.whoLabel(change.byMemberId, memberNames) +
                                    when {
                                        change.isRevert -> " · changed back"
                                        change.isConcurrent -> " · 2 edits at the same time, kept this one"
                                        else -> ""
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ChangeAmountLine(change, amountStyle)
                        }
                    }
                    if (!showAll && changes.size > 5) {
                        Text(
                            text = "Show all ${changes.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 30.dp)
                                .clickable(role = Role.Button) { showAll = true }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeAmountLine(change: ExpenseHistoryPresentation.Change, style: TextStyle) {
    val old = change.oldTotalCents
    Text(
        text = buildAnnotatedString {
            if (old != null && old != change.newTotalCents) {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    append(formatIndianRupeesFromCents(old))
                }
                append("  →  ")
            }
            append(formatIndianRupeesFromCents(change.newTotalCents))
            if (change.oldPayerId != null && change.oldPayerId != change.newPayerId) append(" · payer changed")
        },
        style = style,
        color = MaterialTheme.colorScheme.onSurface
    )
}
