package com.splitmate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.splitmate.app.ManualPaymentInput
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.formatIndianRupeesFromCents

/**
 * v2.3.6: record a payment that is not one of the suggested transfers (e.g. Gaurii paid siddhesh
 * directly). Pick who paid, who received it, and the amount; the balances and the plan update from
 * it like any other settlement. [allowedReceiverIds] limits receivers to the people the device user
 * may confirm for (themselves, or everyone when they are an organizer).
 *
 * [onRecord] returns null on success or the reason the payment was refused (shown inline).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordPaymentDialog(
    members: List<GroupMemberEntity>,
    allowedReceiverIds: Set<String>,
    onRecord: (fromMemberId: String, toMemberId: String, amountCents: Long) -> String?,
    onDismiss: () -> Unit
) {
    var fromId by rememberSaveable { mutableStateOf<String?>(null) }
    var toId by rememberSaveable {
        mutableStateOf(allowedReceiverIds.singleOrNull())
    }
    var amountText by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val amountCents = ManualPaymentInput.parseAmountToCents(amountText)
    val from = members.find { it.memberId == fromId }
    val to = members.find { it.memberId == toId }
    val canRecord = from != null && to != null && from.memberId != to.memberId && amountCents != null
    fun label(m: GroupMemberEntity) = if (m.isCurrentUser) "You" else m.name.trim().substringBefore(" ")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = {
            Text(
                text = "Record a payment",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "For money that changed hands outside the suggested plan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MemberPicker(
                    title = "Who paid",
                    members = members,
                    selectedId = fromId,
                    enabledIds = members.map { it.memberId }.filter { it != toId }.toSet(),
                    label = ::label,
                    onSelect = { fromId = it; error = null }
                )
                MemberPicker(
                    title = "Who received it",
                    members = members,
                    selectedId = toId,
                    enabledIds = allowedReceiverIds.filter { it != fromId }.toSet(),
                    label = ::label,
                    onSelect = { toId = it; error = null }
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("Amount") },
                    prefix = { Text("₹") },
                    singleLine = true,
                    isError = amountText.isNotBlank() && amountCents == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(fontFeatureSettings = "tnum")),
                    modifier = Modifier.fillMaxWidth()
                )
                val summary = when {
                    error != null -> error
                    canRecord -> "${label(from!!)} paid ${label(to!!)} ${formatIndianRupeesFromCents(amountCents!!)}"
                    else -> null
                }
                if (summary != null) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canRecord,
                onClick = {
                    val result = onRecord(fromId!!, toId!!, amountCents!!)
                    if (result == null) onDismiss() else error = result
                }
            ) {
                Text(text = "Record", style = MaterialTheme.typography.labelLarge)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberPicker(
    title: String,
    members: List<GroupMemberEntity>,
    selectedId: String?,
    enabledIds: Set<String>,
    label: (GroupMemberEntity) -> String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            members.forEach { m ->
                FilterChip(
                    selected = m.memberId == selectedId,
                    enabled = m.memberId in enabledIds || m.memberId == selectedId,
                    onClick = { onSelect(m.memberId) },
                    label = { Text(label(m), maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}
