package com.splitmate.app.ui.export

import com.splitmate.app.SplitMateMathEngine
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.data.SettlementEntity
import com.splitmate.app.ui.cleanDisplayExpenseTitle
import com.splitmate.app.ui.extractTravelTicketFromTitle
import com.splitmate.app.ui.fund.isTripFundAdvance
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pure, deterministic per-group export model used by both [GroupLedgerXlsxWriter] and
 * [GroupLedgerCsvWriter]. All monetary values are integer paise/cents (`Long`) so totals,
 * per-member shares, net balances, and Greedy Minimum Cash Flow transfers reconcile with 0.00c drift.
 */
data class GroupLedgerExportModel(
    val groupId: String,
    val groupName: String,
    val currencyCode: String,
    val currencySymbol: String,
    val generatedAtLabel: String,
    val members: List<ExportMemberColumn>,
    val expenses: List<ExportExpenseRow>,
    val memberBalances: List<ExportMemberBalanceRow>,
    val pendingTransfers: List<ExportTransferRow>,
    val tripFundAdvances: List<ExportRecordedSettlementRow>,
    val recordedSettlements: List<ExportRecordedSettlementRow>,
    val totalSpendCents: Long,
    val naiveDebtLegCount: Int
) {
    val transactionsSaved: Int
        get() = (naiveDebtLegCount - pendingTransfers.size).coerceAtLeast(0)
}

data class ExportMemberColumn(
    val memberId: String,
    val displayName: String
)

data class ExportExpenseRow(
    val expenseId: String,
    val dateLabel: String,
    val title: String,
    val category: String,
    val travelRef: String,
    val payerMemberId: String,
    val payerName: String,
    val totalCents: Long,
    val splitTypeLabel: String,
    val participantsSummary: String,
    val sharesByMemberId: Map<String, Long>
)

data class ExportMemberBalanceRow(
    val memberId: String,
    val displayName: String,
    val totalPaidCents: Long,
    val totalShareCents: Long,
    val settlementsSentCents: Long,
    val settlementsReceivedCents: Long,
    val netBalanceCents: Long
) {
    val statusLabel: String
        get() = when {
            netBalanceCents > 0L -> "Gets back"
            netBalanceCents < 0L -> "Owes"
            else -> "Settled"
        }
}

data class ExportTransferRow(
    val stepNumber: Int,
    val fromMemberId: String,
    val fromName: String,
    val toMemberId: String,
    val toName: String,
    val amountCents: Long
)

data class ExportRecordedSettlementRow(
    val dateLabel: String,
    val fromName: String,
    val toName: String,
    val amountCents: Long
)

object GroupLedgerExportBuilder {

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)

    private val dateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.US)

    /**
     * Builds a self-consistent [GroupLedgerExportModel] for [groupId].
     *
     * Every expense's per-member shares sum to `expense.totalAmountCents`, every member's
     * `netBalanceCents == totalPaidCents - totalShareCents + settlementsSentCents - settlementsReceivedCents`,
     * the sum of all `netBalanceCents` is `0L`, and `pendingTransfers` is produced directly from
     * [SplitMateMathEngine.simplifyDebtsGreedy] on those exact net balances.
     */
    fun build(
        groupId: String,
        groupName: String,
        currencyCode: String = "INR",
        currencySymbol: String = "₹",
        members: List<GroupMemberEntity>,
        expenses: List<ExpenseEntity>,
        splits: List<ExpenseSplitEntity>,
        settlements: List<SettlementEntity>,
        generatedAtEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): GroupLedgerExportModel {
        val groupMembers = members.filter { it.groupId == groupId }
        val groupExpenses = expenses
            .filter { it.groupId == groupId }
            .sortedBy { it.scheduledAtEpochMs ?: it.createdAt }
        val groupExpenseIds = groupExpenses.map { it.expenseId }.toSet()
        val splitsByExpense = splits
            .filter { it.expenseId in groupExpenseIds }
            .groupBy { it.expenseId }
        val groupSettlements = settlements
            .filter { it.groupId == groupId }
            .sortedBy { it.settledAt }

        // Disambiguate duplicate member display names so SUMIF formulas in Excel stay 100% exact.
        val nameCounts = groupMembers.groupingBy { it.name.trim().ifBlank { "Member" } }.eachCount()
        val seenNames = mutableMapOf<String, Int>()
        val exportMembers = groupMembers.map { m ->
            val base = m.name.trim().ifBlank { "Member" }
            val unique = if ((nameCounts[base] ?: 0) > 1) {
                val n = (seenNames[base] ?: 0) + 1
                seenNames[base] = n
                "$base ($n)"
            } else {
                base
            }
            ExportMemberColumn(memberId = m.memberId, displayName = unique)
        }
        val memberNameById = exportMembers.associate { it.memberId to it.displayName }

        val paidByMember = mutableMapOf<String, Long>()
        val shareByMember = mutableMapOf<String, Long>()
        var naiveDebtLegs = 0

        val expenseRows = groupExpenses.map { exp ->
            val expSplits = splitsByExpense[exp.expenseId].orEmpty()
            val resolvedShares: Map<String, Long> = if (expSplits.isNotEmpty()) {
                val rawMap = expSplits.associate { it.memberId to it.finalOwedCents }
                exportMembers.associate { col -> col.memberId to (rawMap[col.memberId] ?: 0L) }
            } else if (exportMembers.isNotEmpty()) {
                // Legacy fallback: equal split across all group members with 0.00c drift.
                val equal = SplitMateMathEngine.splitEquallyZeroDrift(
                    totalCents = exp.totalAmountCents,
                    members = exportMembers.map { it.memberId to it.displayName },
                    payerId = exp.payerId
                )
                equal.associate { it.memberId to it.finalCents }
            } else {
                emptyMap()
            }

            paidByMember[exp.payerId] = (paidByMember[exp.payerId] ?: 0L) + exp.totalAmountCents
            resolvedShares.forEach { (mid, share) ->
                shareByMember[mid] = (shareByMember[mid] ?: 0L) + share
                if (share > 0L && mid != exp.payerId) {
                    naiveDebtLegs++
                }
            }

            val nonZeroParticipants = exportMembers.filter { (resolvedShares[it.memberId] ?: 0L) > 0L }
            val distinctNonZeroShares = nonZeroParticipants
                .map { resolvedShares[it.memberId] ?: 0L }
                .distinct()
            val splitType = when {
                nonZeroParticipants.size <= 1 -> {
                    val only = nonZeroParticipants.firstOrNull()
                    if (only?.memberId == exp.payerId) "Personal / Solo" else "Full transfer"
                }
                distinctNonZeroShares.size == 1 ||
                    (distinctNonZeroShares.size == 2 && kotlin.math.abs(distinctNonZeroShares[0] - distinctNonZeroShares[1]) == 1L) ->
                    "Equal (${nonZeroParticipants.size})"
                else -> "Custom (${nonZeroParticipants.size})"
            }

            val parsedTicket = extractTravelTicketFromTitle(exp.title)
            val travelRef = buildList {
                val pnr = exp.travelPnr.ifBlank { parsedTicket?.pnr.orEmpty() }
                if (pnr.isNotBlank()) add("PNR: $pnr")
                val route = listOfNotNull(
                    parsedTicket?.fromStation?.takeIf { it.isNotBlank() },
                    parsedTicket?.toStation?.takeIf { it.isNotBlank() }
                ).joinToString(" -> ")
                if (route.isNotBlank()) add(route)
            }.joinToString(" | ")

            val epochMs = exp.scheduledAtEpochMs ?: exp.createdAt
            val dateLabel = Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate().format(dateFormatter)
            val payerName = memberNameById[exp.payerId]
                ?: groupMembers.find { it.memberId == exp.payerId }?.name
                ?: "Unknown"

            ExportExpenseRow(
                expenseId = exp.expenseId,
                dateLabel = dateLabel,
                title = cleanDisplayExpenseTitle(exp.title).ifBlank { exp.title },
                category = formatCategoryLabel(exp.expenseCategory),
                travelRef = travelRef,
                payerMemberId = exp.payerId,
                payerName = payerName,
                totalCents = exp.totalAmountCents,
                splitTypeLabel = splitType,
                participantsSummary = nonZeroParticipants.joinToString(", ") { it.displayName },
                sharesByMemberId = resolvedShares
            )
        }

        val sentByMember = mutableMapOf<String, Long>()
        val receivedByMember = mutableMapOf<String, Long>()
        val tripFundAdvanceRows = mutableListOf<ExportRecordedSettlementRow>()
        val recordedSettlementRows = mutableListOf<ExportRecordedSettlementRow>()
        groupSettlements.forEach { s ->
            sentByMember[s.fromMemberId] = (sentByMember[s.fromMemberId] ?: 0L) + s.amountCents
            receivedByMember[s.toMemberId] = (receivedByMember[s.toMemberId] ?: 0L) + s.amountCents
            val row = ExportRecordedSettlementRow(
                dateLabel = Instant.ofEpochMilli(s.settledAt).atZone(zoneId).toLocalDate().format(dateFormatter),
                fromName = memberNameById[s.fromMemberId] ?: s.fromMemberName,
                toName = memberNameById[s.toMemberId] ?: s.toMemberName,
                amountCents = s.amountCents
            )
            if (s.isTripFundAdvance()) {
                tripFundAdvanceRows += row
            } else {
                recordedSettlementRows += row
            }
        }

        val balanceRows = exportMembers.map { col ->
            val paid = paidByMember[col.memberId] ?: 0L
            val share = shareByMember[col.memberId] ?: 0L
            val sent = sentByMember[col.memberId] ?: 0L
            val received = receivedByMember[col.memberId] ?: 0L
            val net = paid - share + sent - received
            ExportMemberBalanceRow(
                memberId = col.memberId,
                displayName = col.displayName,
                totalPaidCents = paid,
                totalShareCents = share,
                settlementsSentCents = sent,
                settlementsReceivedCents = received,
                netBalanceCents = net
            )
        }

        val simplified = SplitMateMathEngine.simplifyDebtsGreedy(
            balanceRows.map { row ->
                SplitMateMathEngine.MemberNetBalance(
                    memberId = row.memberId,
                    displayName = row.displayName,
                    netCents = row.netBalanceCents
                )
            }
        )
        val pendingTransfers = simplified.mapIndexed { idx, transfer ->
            ExportTransferRow(
                stepNumber = idx + 1,
                fromMemberId = transfer.fromMemberId,
                fromName = transfer.fromName,
                toMemberId = transfer.toMemberId,
                toName = transfer.toName,
                amountCents = transfer.amountCents
            )
        }

        val generatedAtLabel = Instant.ofEpochMilli(generatedAtEpochMs)
            .atZone(zoneId)
            .format(dateTimeFormatter)

        return GroupLedgerExportModel(
            groupId = groupId,
            groupName = groupName.trim().ifBlank { "SplitMate Group" },
            currencyCode = currencyCode,
            currencySymbol = currencySymbol,
            generatedAtLabel = generatedAtLabel,
            members = exportMembers,
            expenses = expenseRows,
            memberBalances = balanceRows,
            pendingTransfers = pendingTransfers,
            tripFundAdvances = tripFundAdvanceRows,
            recordedSettlements = recordedSettlementRows,
            totalSpendCents = expenseRows.sumOf { it.totalCents },
            naiveDebtLegCount = naiveDebtLegs
        )
    }

    private fun formatCategoryLabel(raw: String): String =
        raw.trim()
            .replace('_', ' ')
            .lowercase(Locale.US)
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase(Locale.US) } }
            .ifBlank { "General" }

    /** Formats integer paise/cents as exact decimal string (e.g. `1417` -> `"14.17"`, `-500` -> `"-5.00"`). */
    fun formatCentsDecimal(cents: Long): String {
        val sign = if (cents < 0L) "-" else ""
        val abs = kotlin.math.abs(cents)
        return String.format(Locale.US, "%s%d.%02d", sign, abs / 100L, abs % 100L)
    }

    /** Neutralizes spreadsheet formula injection in user-supplied text cells (`=`, `+`, `-`, `@`). */
    fun sanitizeSpreadsheetText(raw: String): String {
        val trimmed = raw.replace("\r", " ").replace("\n", " ").trim()
        if (trimmed.isEmpty()) return ""
        return when (trimmed.first()) {
            '=', '+', '-', '@' -> "'$trimmed"
            else -> trimmed
        }
    }
}
