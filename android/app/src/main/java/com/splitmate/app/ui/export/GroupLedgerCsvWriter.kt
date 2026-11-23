package com.splitmate.app.ui.export

import java.nio.charset.StandardCharsets

/**
 * Pure-Kotlin RFC 4180 CSV exporter for a single group's complete SplitMate ledger.
 *
 * Prepends a UTF-8 BOM (`\uFEFF`) so Microsoft Excel on desktop/mobile and Google Sheets
 * render the `₹` symbol cleanly without encoding glitches.
 */
object GroupLedgerCsvWriter {

    private const val UTF8_BOM = "\uFEFF"

    fun writeToString(model: GroupLedgerExportModel): String {
        val sb = StringBuilder()
        val sym = model.currencySymbol

        fun row(vararg cols: String) {
            sb.append(cols.joinToString(",") { escapeCsv(it) })
            sb.append("\r\n")
        }

        // Header banner
        row("SplitMate Group Ledger Export", model.groupName)
        row("Generated On", model.generatedAtLabel, "Currency", "${model.currencyCode} ($sym)")
        row()

        // Section 1: Itemized Expenses & Per-Member Shares
        row("SECTION 1: EXPENSES & PER-MEMBER SHARES")
        val expenseHeaders = buildList {
            add("Date")
            add("Expense")
            add("Category")
            add("PNR / Travel")
            add("Paid By")
            add("Total ($sym)")
            add("Split Type")
            add("Participants")
            model.members.forEach { add("${it.displayName} Share ($sym)") }
        }
        row(*expenseHeaders.toTypedArray())

        model.expenses.forEach { exp ->
            val values = buildList {
                add(exp.dateLabel)
                add(GroupLedgerExportBuilder.sanitizeSpreadsheetText(exp.title))
                add(GroupLedgerExportBuilder.sanitizeSpreadsheetText(exp.category))
                add(GroupLedgerExportBuilder.sanitizeSpreadsheetText(exp.travelRef))
                add(GroupLedgerExportBuilder.sanitizeSpreadsheetText(exp.payerName))
                add(GroupLedgerExportBuilder.formatCentsDecimal(exp.totalCents))
                add(exp.splitTypeLabel)
                add(GroupLedgerExportBuilder.sanitizeSpreadsheetText(exp.participantsSummary))
                model.members.forEach { member ->
                    add(GroupLedgerExportBuilder.formatCentsDecimal(exp.sharesByMemberId[member.memberId] ?: 0L))
                }
            }
            row(*values.toTypedArray())
        }

        val totalRow = buildList {
            add("TOTAL")
            add("${model.expenses.size} expenses")
            add("")
            add("")
            add("")
            add(GroupLedgerExportBuilder.formatCentsDecimal(model.totalSpendCents))
            add("")
            add("")
            model.members.forEach { member ->
                val sum = model.expenses.sumOf { it.sharesByMemberId[member.memberId] ?: 0L }
                add(GroupLedgerExportBuilder.formatCentsDecimal(sum))
            }
        }
        row(*totalRow.toTypedArray())
        row()

        // Section 2: Member Balances
        row("SECTION 2: MEMBER BALANCES")
        row(
            "Member",
            "Total Paid ($sym)",
            "Total Share ($sym)",
            "Payments Sent ($sym)",
            "Payments Received ($sym)",
            "Net Balance ($sym)",
            "Status"
        )
        model.memberBalances.forEach { b ->
            row(
                GroupLedgerExportBuilder.sanitizeSpreadsheetText(b.displayName),
                GroupLedgerExportBuilder.formatCentsDecimal(b.totalPaidCents),
                GroupLedgerExportBuilder.formatCentsDecimal(b.totalShareCents),
                GroupLedgerExportBuilder.formatCentsDecimal(b.settlementsSentCents),
                GroupLedgerExportBuilder.formatCentsDecimal(b.settlementsReceivedCents),
                GroupLedgerExportBuilder.formatCentsDecimal(b.netBalanceCents),
                b.statusLabel
            )
        }
        row(
            "TOTAL / ZERO-CHECK",
            GroupLedgerExportBuilder.formatCentsDecimal(model.memberBalances.sumOf { it.totalPaidCents }),
            GroupLedgerExportBuilder.formatCentsDecimal(model.memberBalances.sumOf { it.totalShareCents }),
            GroupLedgerExportBuilder.formatCentsDecimal(model.memberBalances.sumOf { it.settlementsSentCents }),
            GroupLedgerExportBuilder.formatCentsDecimal(model.memberBalances.sumOf { it.settlementsReceivedCents }),
            GroupLedgerExportBuilder.formatCentsDecimal(model.memberBalances.sumOf { it.netBalanceCents }),
            "Must equal 0.00"
        )
        row()

        // Section 3: Greedy Minimum Cash Flow Settlement Plan
        row("SECTION 3: MINIMUM SETTLEMENT PLAN (GREEDY MINIMUM CASH FLOW)")
        row("Individual Debt Legs Before Simplification", model.naiveDebtLegCount.toString())
        row("Minimum Transfers Needed Now", model.pendingTransfers.size.toString())
        row("Transactions Eliminated", model.transactionsSaved.toString())
        row("Step", "From (Debtor)", "To (Creditor)", "Amount ($sym)", "Status")
        if (model.pendingTransfers.isEmpty()) {
            row("All settled", "Everyone is square — 0 transfers needed.", "", "0.00", "Settled")
        } else {
            model.pendingTransfers.forEach { t ->
                row(
                    t.stepNumber.toString(),
                    GroupLedgerExportBuilder.sanitizeSpreadsheetText(t.fromName),
                    GroupLedgerExportBuilder.sanitizeSpreadsheetText(t.toName),
                    GroupLedgerExportBuilder.formatCentsDecimal(t.amountCents),
                    "Pending"
                )
            }
            row(
                "TOTAL REMAINING",
                "",
                "",
                GroupLedgerExportBuilder.formatCentsDecimal(model.pendingTransfers.sumOf { it.amountCents }),
                "${model.pendingTransfers.size} transfer(s)"
            )
        }

        if (model.recordedSettlements.isNotEmpty()) {
            row()
            row("SECTION 4: RECORDED / PAID SETTLEMENTS")
            row("Date", "From", "To", "Amount ($sym)", "Status")
            model.recordedSettlements.forEach { s ->
                row(
                    s.dateLabel,
                    GroupLedgerExportBuilder.sanitizeSpreadsheetText(s.fromName),
                    GroupLedgerExportBuilder.sanitizeSpreadsheetText(s.toName),
                    GroupLedgerExportBuilder.formatCentsDecimal(s.amountCents),
                    "Paid"
                )
            }
        }

        return UTF8_BOM + sb.toString()
    }

    fun writeToBytes(model: GroupLedgerExportModel): ByteArray =
        writeToString(model).toByteArray(StandardCharsets.UTF_8)

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
