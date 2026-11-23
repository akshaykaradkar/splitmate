package com.splitmate.app.ui.export

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Pure-Kotlin OOXML Excel (`.xlsx`) workbook writer (zero third-party library dependencies).
 *
 * Produces a 3-sheet workbook compatible with Microsoft Excel, Google Sheets, Apple Numbers,
 * and LibreOffice Calc:
 * 1. `Expenses`: itemized group expenses + one column per member showing their exact share + live `=SUM(...)` totals.
 * 2. `Member Balances`: live `=SUMIF(...)` Paid, live `=SUM(...)` Share, recorded settlements, and live `=B-C+D-E` Net Balance.
 * 3. `Minimum Settlement Plan`: Greedy Minimum Cash Flow transfers, simplification savings summary, and recorded payments.
 *
 * Every formula cell includes both the OOXML `<f>` formula AND its exact pre-calculated `<v>` value
 * so viewers that don't evaluate formulas still display the exact rupee amounts immediately.
 */
object GroupLedgerXlsxWriter {

    private const val STYLE_DEFAULT = 0
    private const val STYLE_HEADER = 1
    private const val STYLE_MONEY = 2
    private const val STYLE_TOTAL_LABEL = 3
    private const val STYLE_TOTAL_MONEY = 4
    private const val STYLE_TITLE = 5
    private const val STYLE_SUBTITLE = 6

    fun writeToBytes(model: GroupLedgerExportModel): ByteArray {
        val baos = ByteArrayOutputStream()
        writeTo(model, baos)
        return baos.toByteArray()
    }

    fun writeTo(model: GroupLedgerExportModel, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", contentTypesXml())
            writeZipEntry(zip, "_rels/.rels", rootRelsXml())
            writeZipEntry(zip, "xl/workbook.xml", workbookXml())
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
            writeZipEntry(zip, "xl/styles.xml", stylesXml())
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", buildExpensesSheetXml(model))
            writeZipEntry(zip, "xl/worksheets/sheet2.xml", buildMemberBalancesSheetXml(model))
            writeZipEntry(zip, "xl/worksheets/sheet3.xml", buildSettlementPlanSheetXml(model))
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private fun contentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

    private fun rootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Expenses" sheetId="1" r:id="rId1"/>
    <sheet name="Member Balances" sheetId="2" r:id="rId2"/>
    <sheet name="Minimum Settlement Plan" sheetId="3" r:id="rId3"/>
  </sheets>
</workbook>"""

    private fun workbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    /** Buckwheat-inspired clean spreadsheet styling: soft sage headers, warm cream totals, tabular money format. */
    private fun stylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="1">
    <numFmt numFmtId="164" formatCode="#,##0.00"/>
  </numFmts>
  <fonts count="4">
    <font><sz val="11"/><color rgb="FF23201E"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FF23201E"/><name val="Calibri"/></font>
    <font><b/><sz val="14"/><color rgb="FF23201E"/><name val="Calibri"/></font>
    <font><sz val="10"/><color rgb="FF6B655F"/><name val="Calibri"/></font>
  </fonts>
  <fills count="4">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFDCE9B9"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF4EFE6"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFEDE7DF"/></left>
      <right style="thin"><color rgb="FFEDE7DF"/></right>
      <top style="thin"><color rgb="FFEDE7DF"/></top>
      <bottom style="thin"><color rgb="FFEDE7DF"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="7">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>
    <xf numFmtId="0" fontId="1" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
    <xf numFmtId="164" fontId="1" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1"/>
    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>"""

    // =========================================================================
    // SHEET 1: EXPENSES + PER-MEMBER SHARES
    // =========================================================================
    private fun buildExpensesSheetXml(model: GroupLedgerExportModel): String {
        val sb = StringBuilder()
        val sym = model.currencySymbol
        val memberCount = model.members.size
        // Base columns: A=Date, B=Expense, C=Category, D=PNR/Travel, E=Paid By, F=Total, G=Split Type, H=Participants
        // Member share columns start at index 8 (column I), followed by Row Check column.
        val firstMemberCol = 8
        val checkCol = firstMemberCol + memberCount

        sb.append(sheetHeader(freezeRows = 1))
        sb.append("<cols>")
        sb.append(colDef(1, 14.0)) // Date
        sb.append(colDef(2, 28.0)) // Expense
        sb.append(colDef(3, 15.0)) // Category
        sb.append(colDef(4, 22.0)) // PNR / Travel
        sb.append(colDef(5, 18.0)) // Paid By
        sb.append(colDef(6, 15.0)) // Total
        sb.append(colDef(7, 15.0)) // Split Type
        sb.append(colDef(8, 28.0)) // Participants
        for (i in 0 until memberCount) {
            sb.append(colDef(firstMemberCol + i + 1, 16.0))
        }
        sb.append(colDef(checkCol + 1, 16.0)) // Row Check
        sb.append("</cols>")

        sb.append("<sheetData>")
        // Row 1: Headers
        sb.append("<row r=\"1\">")
        val headers = buildList {
            add("Date")
            add("Expense")
            add("Category")
            add("PNR / Travel")
            add("Paid By")
            add("Total ($sym)")
            add("Split Type")
            add("Participants")
            model.members.forEach { add("${it.displayName} Share ($sym)") }
            add("Row Drift Check ($sym)")
        }
        headers.forEachIndexed { colIdx, text ->
            sb.append(stringCell(colIdx, 1, text, STYLE_HEADER, sanitizeUserInput = false))
        }
        sb.append("</row>")

        // Rows 2..N+1: Expenses
        model.expenses.forEachIndexed { idx, exp ->
            val rowNum = idx + 2
            sb.append("<row r=\"$rowNum\">")
            sb.append(stringCell(0, rowNum, exp.dateLabel, STYLE_DEFAULT, sanitizeUserInput = false))
            sb.append(stringCell(1, rowNum, exp.title, STYLE_DEFAULT))
            sb.append(stringCell(2, rowNum, exp.category, STYLE_DEFAULT))
            sb.append(stringCell(3, rowNum, exp.travelRef, STYLE_DEFAULT))
            sb.append(stringCell(4, rowNum, exp.payerName, STYLE_DEFAULT))
            sb.append(moneyCell(5, rowNum, exp.totalCents, STYLE_MONEY))
            sb.append(stringCell(6, rowNum, exp.splitTypeLabel, STYLE_DEFAULT, sanitizeUserInput = false))
            sb.append(stringCell(7, rowNum, exp.participantsSummary, STYLE_DEFAULT))

            var rowShareSum = 0L
            model.members.forEachIndexed { mIdx, member ->
                val share = exp.sharesByMemberId[member.memberId] ?: 0L
                rowShareSum += share
                sb.append(moneyCell(firstMemberCol + mIdx, rowNum, share, STYLE_MONEY))
            }

            val drift = rowShareSum - exp.totalCents
            if (memberCount > 0) {
                val startCol = colLetter(firstMemberCol)
                val endCol = colLetter(firstMemberCol + memberCount - 1)
                sb.append(formulaCell(checkCol, rowNum, "SUM($startCol$rowNum:$endCol$rowNum)-F$rowNum", drift, STYLE_MONEY))
            } else {
                sb.append(moneyCell(checkCol, rowNum, 0L, STYLE_MONEY))
            }
            sb.append("</row>")
        }

        // Totals Row
        val lastExpenseRow = model.expenses.size + 1
        val totalRow = lastExpenseRow + 1
        sb.append("<row r=\"$totalRow\">")
        sb.append(stringCell(0, totalRow, "TOTAL", STYLE_TOTAL_LABEL, sanitizeUserInput = false))
        sb.append(stringCell(1, totalRow, "${model.expenses.size} expenses", STYLE_TOTAL_LABEL, sanitizeUserInput = false))
        for (c in 2..4) sb.append(stringCell(c, totalRow, "", STYLE_TOTAL_LABEL, sanitizeUserInput = false))
        if (model.expenses.isNotEmpty()) {
            sb.append(formulaCell(5, totalRow, "SUM(F2:F$lastExpenseRow)", model.totalSpendCents, STYLE_TOTAL_MONEY))
        } else {
            sb.append(moneyCell(5, totalRow, 0L, STYLE_TOTAL_MONEY))
        }
        sb.append(stringCell(6, totalRow, "", STYLE_TOTAL_LABEL, sanitizeUserInput = false))
        sb.append(stringCell(7, totalRow, "", STYLE_TOTAL_LABEL, sanitizeUserInput = false))

        model.members.forEachIndexed { mIdx, member ->
            val colIdx = firstMemberCol + mIdx
            val colL = colLetter(colIdx)
            val memberSum = model.expenses.sumOf { it.sharesByMemberId[member.memberId] ?: 0L }
            if (model.expenses.isNotEmpty()) {
                sb.append(formulaCell(colIdx, totalRow, "SUM(${colL}2:${colL}$lastExpenseRow)", memberSum, STYLE_TOTAL_MONEY))
            } else {
                sb.append(moneyCell(colIdx, totalRow, 0L, STYLE_TOTAL_MONEY))
            }
        }
        val checkColL = colLetter(checkCol)
        if (model.expenses.isNotEmpty()) {
            sb.append(formulaCell(checkCol, totalRow, "SUM(${checkColL}2:${checkColL}$lastExpenseRow)", 0L, STYLE_TOTAL_MONEY))
        } else {
            sb.append(moneyCell(checkCol, totalRow, 0L, STYLE_TOTAL_MONEY))
        }
        sb.append("</row>")

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    // =========================================================================
    // SHEET 2: MEMBER BALANCES (LIVE FORMULAS REFERENCING SHEET 1)
    // =========================================================================
    private fun buildMemberBalancesSheetXml(model: GroupLedgerExportModel): String {
        val sb = StringBuilder()
        val sym = model.currencySymbol
        val lastExpenseRow = model.expenses.size + 1

        sb.append(sheetHeader(freezeRows = 1))
        sb.append("<cols>")
        sb.append(colDef(1, 22.0)) // Member
        sb.append(colDef(2, 18.0)) // Total Paid
        sb.append(colDef(3, 18.0)) // Total Share
        sb.append(colDef(4, 20.0)) // Payments Sent
        sb.append(colDef(5, 22.0)) // Payments Received
        sb.append(colDef(6, 18.0)) // Net Balance
        sb.append(colDef(7, 16.0)) // Status
        sb.append("</cols>")

        sb.append("<sheetData>")
        sb.append("<row r=\"1\">")
        listOf(
            "Member",
            "Total Paid ($sym)",
            "Total Share ($sym)",
            "Payments Sent ($sym)",
            "Payments Received ($sym)",
            "Net Balance ($sym)",
            "Status"
        ).forEachIndexed { idx, text ->
            sb.append(stringCell(idx, 1, text, STYLE_HEADER, sanitizeUserInput = false))
        }
        sb.append("</row>")

        model.memberBalances.forEachIndexed { idx, row ->
            val rowNum = idx + 2
            val shareColL = colLetter(8 + idx)
            sb.append("<row r=\"$rowNum\">")
            sb.append(stringCell(0, rowNum, row.displayName, STYLE_DEFAULT))
            if (model.expenses.isNotEmpty()) {
                sb.append(
                    formulaCell(
                        1,
                        rowNum,
                        "SUMIF(Expenses!\$E\$2:\$E\$$lastExpenseRow,A$rowNum,Expenses!\$F\$2:\$F\$$lastExpenseRow)",
                        row.totalPaidCents,
                        STYLE_MONEY
                    )
                )
                sb.append(
                    formulaCell(
                        2,
                        rowNum,
                        "SUM(Expenses!${shareColL}\$2:${shareColL}\$$lastExpenseRow)",
                        row.totalShareCents,
                        STYLE_MONEY
                    )
                )
            } else {
                sb.append(moneyCell(1, rowNum, row.totalPaidCents, STYLE_MONEY))
                sb.append(moneyCell(2, rowNum, row.totalShareCents, STYLE_MONEY))
            }
            sb.append(moneyCell(3, rowNum, row.settlementsSentCents, STYLE_MONEY))
            sb.append(moneyCell(4, rowNum, row.settlementsReceivedCents, STYLE_MONEY))
            sb.append(formulaCell(5, rowNum, "B$rowNum-C$rowNum+D$rowNum-E$rowNum", row.netBalanceCents, STYLE_MONEY))
            sb.append(stringCell(6, rowNum, row.statusLabel, STYLE_DEFAULT, sanitizeUserInput = false))
            sb.append("</row>")
        }

        val lastMemberRow = model.memberBalances.size + 1
        val totalRow = lastMemberRow + 1
        sb.append("<row r=\"$totalRow\">")
        sb.append(stringCell(0, totalRow, "TOTAL / ZERO-CHECK", STYLE_TOTAL_LABEL, sanitizeUserInput = false))
        if (model.memberBalances.isNotEmpty()) {
            sb.append(formulaCell(1, totalRow, "SUM(B2:B$lastMemberRow)", model.memberBalances.sumOf { it.totalPaidCents }, STYLE_TOTAL_MONEY))
            sb.append(formulaCell(2, totalRow, "SUM(C2:C$lastMemberRow)", model.memberBalances.sumOf { it.totalShareCents }, STYLE_TOTAL_MONEY))
            sb.append(formulaCell(3, totalRow, "SUM(D2:D$lastMemberRow)", model.memberBalances.sumOf { it.settlementsSentCents }, STYLE_TOTAL_MONEY))
            sb.append(formulaCell(4, totalRow, "SUM(E2:E$lastMemberRow)", model.memberBalances.sumOf { it.settlementsReceivedCents }, STYLE_TOTAL_MONEY))
            sb.append(formulaCell(5, totalRow, "SUM(F2:F$lastMemberRow)", model.memberBalances.sumOf { it.netBalanceCents }, STYLE_TOTAL_MONEY))
        } else {
            for (c in 1..5) sb.append(moneyCell(c, totalRow, 0L, STYLE_TOTAL_MONEY))
        }
        sb.append(stringCell(6, totalRow, "Must equal 0.00", STYLE_TOTAL_LABEL, sanitizeUserInput = false))
        sb.append("</row>")

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    // =========================================================================
    // SHEET 3: MINIMUM SETTLEMENT PLAN (GREEDY MINIMUM CASH FLOW)
    // =========================================================================
    private fun buildSettlementPlanSheetXml(model: GroupLedgerExportModel): String {
        val sb = StringBuilder()
        val sym = model.currencySymbol
        val expenseTotalRow = model.expenses.size + 2

        sb.append(sheetHeader(freezeRows = 0))
        sb.append("<cols>")
        sb.append(colDef(1, 28.0))
        sb.append(colDef(2, 24.0))
        sb.append(colDef(3, 24.0))
        sb.append(colDef(4, 18.0))
        sb.append(colDef(5, 16.0))
        sb.append("</cols>")

        sb.append("<sheetData>")
        var r = 1
        sb.append("<row r=\"$r\">")
        sb.append(stringCell(0, r, "${model.groupName} — Minimum Settlement Plan", STYLE_TITLE))
        sb.append("</row>")
        r++

        sb.append("<row r=\"$r\">")
        sb.append(stringCell(0, r, "Generated by SplitMate on ${model.generatedAtLabel} (${model.currencyCode})", STYLE_SUBTITLE, sanitizeUserInput = false))
        sb.append("</row>")
        r += 2

        // Summary KPI table
        sb.append("<row r=\"$r\">")
        sb.append(stringCell(0, r, "Metric", STYLE_HEADER, sanitizeUserInput = false))
        sb.append(stringCell(1, r, "Value", STYLE_HEADER, sanitizeUserInput = false))
        sb.append("</row>")
        r++

        sb.append("<row r=\"$r\">")
        sb.append(stringCell(0, r, "Total Group Spend ($sym)", STYLE_DEFAULT, sanitizeUserInput = false))
        sb.append(formulaCell(1, r, "Expenses!F$expenseTotalRow", model.totalSpendCents, STYLE_MONEY))
        sb.append("</row>")
        r++

        sb.append("<row r=\"$r\">")
        sb.append(stringCell(0, r, "Expenses Logged", STYLE_DEFAULT, sanitizeUserInput = false))
        sb.append(intCell(1, r, model.expenses.size.toLong(), STYLE_DEFAULT))
        sb.append("</row>")
        r++

        sb.append("<row r=\"$r\">")
        sb.append(stringCell(0, r, "Individual Debt Legs Before Simplification", STYLE_DEFAULT, sanitizeUserInput = false))
        sb.append(intCell(1, r, model.naiveDebtLegCount.toLong(), STYLE_DEFAULT))
        sb.append("</row>")
        r++

        sb.append("<row r=\"$r\">")
        sb.append(stringCell(0, r, "Minimum Transfers Needed Now", STYLE_DEFAULT, sanitizeUserInput = false))
        sb.append(intCell(1, r, model.pendingTransfers.size.toLong(), STYLE_DEFAULT))
        sb.append("</row>")
        r++

        sb.append("<row r=\"$r\">")
        sb.append(stringCell(0, r, "Transactions Eliminated", STYLE_DEFAULT, sanitizeUserInput = false))
        sb.append(intCell(1, r, model.transactionsSaved.toLong(), STYLE_DEFAULT))
        sb.append("</row>")
        r++

        sb.append("<row r=\"$r\">")
        sb.append(stringCell(0, r, "How It Works", STYLE_DEFAULT, sanitizeUserInput = false))
        sb.append(
            stringCell(
                1,
                r,
                "Greedy Minimum Cash Flow computes each member's net balance once (see Member Balances sheet), then matches the largest debtor with the largest creditor until every balance reaches 0.00.",
                STYLE_DEFAULT,
                sanitizeUserInput = false
            )
        )
        sb.append("</row>")
        r += 2

        // Pending Minimum Transfers Table
        sb.append("<row r=\"$r\">")
        listOf("Step", "From (Debtor)", "To (Creditor)", "Amount ($sym)", "Status").forEachIndexed { idx, h ->
            sb.append(stringCell(idx, r, h, STYLE_HEADER, sanitizeUserInput = false))
        }
        sb.append("</row>")
        r++

        if (model.pendingTransfers.isEmpty()) {
            sb.append("<row r=\"$r\">")
            sb.append(stringCell(0, r, "All settled", STYLE_TOTAL_LABEL, sanitizeUserInput = false))
            sb.append(stringCell(1, r, "Everyone is square — 0 transfers needed.", STYLE_TOTAL_LABEL, sanitizeUserInput = false))
            sb.append("</row>")
            r += 2
        } else {
            val firstTransferRow = r
            model.pendingTransfers.forEach { t ->
                sb.append("<row r=\"$r\">")
                sb.append(intCell(0, r, t.stepNumber.toLong(), STYLE_DEFAULT))
                sb.append(stringCell(1, r, t.fromName, STYLE_DEFAULT))
                sb.append(stringCell(2, r, t.toName, STYLE_DEFAULT))
                sb.append(moneyCell(3, r, t.amountCents, STYLE_MONEY))
                sb.append(stringCell(4, r, "Pending", STYLE_DEFAULT, sanitizeUserInput = false))
                sb.append("</row>")
                r++
            }
            val lastTransferRow = r - 1
            sb.append("<row r=\"$r\">")
            sb.append(stringCell(0, r, "TOTAL REMAINING", STYLE_TOTAL_LABEL, sanitizeUserInput = false))
            sb.append(stringCell(1, r, "", STYLE_TOTAL_LABEL, sanitizeUserInput = false))
            sb.append(stringCell(2, r, "", STYLE_TOTAL_LABEL, sanitizeUserInput = false))
            sb.append(
                formulaCell(
                    3,
                    r,
                    "SUM(D$firstTransferRow:D$lastTransferRow)",
                    model.pendingTransfers.sumOf { it.amountCents },
                    STYLE_TOTAL_MONEY
                )
            )
            sb.append(stringCell(4, r, "${model.pendingTransfers.size} transfer(s)", STYLE_TOTAL_LABEL, sanitizeUserInput = false))
            sb.append("</row>")
            r += 2
        }

        // Recorded / Paid Settlements Table
        if (model.recordedSettlements.isNotEmpty()) {
            sb.append("<row r=\"$r\">")
            listOf("Recorded Date", "From", "To", "Amount ($sym)", "Status").forEachIndexed { idx, h ->
                sb.append(stringCell(idx, r, h, STYLE_HEADER, sanitizeUserInput = false))
            }
            sb.append("</row>")
            r++
            val firstPaidRow = r
            model.recordedSettlements.forEach { s ->
                sb.append("<row r=\"$r\">")
                sb.append(stringCell(0, r, s.dateLabel, STYLE_DEFAULT, sanitizeUserInput = false))
                sb.append(stringCell(1, r, s.fromName, STYLE_DEFAULT))
                sb.append(stringCell(2, r, s.toName, STYLE_DEFAULT))
                sb.append(moneyCell(3, r, s.amountCents, STYLE_MONEY))
                sb.append(stringCell(4, r, "Paid", STYLE_DEFAULT, sanitizeUserInput = false))
                sb.append("</row>")
                r++
            }
            val lastPaidRow = r - 1
            sb.append("<row r=\"$r\">")
            sb.append(stringCell(0, r, "TOTAL PAID", STYLE_TOTAL_LABEL, sanitizeUserInput = false))
            sb.append(stringCell(1, r, "", STYLE_TOTAL_LABEL, sanitizeUserInput = false))
            sb.append(stringCell(2, r, "", STYLE_TOTAL_LABEL, sanitizeUserInput = false))
            sb.append(
                formulaCell(
                    3,
                    r,
                    "SUM(D$firstPaidRow:D$lastPaidRow)",
                    model.recordedSettlements.sumOf { it.amountCents },
                    STYLE_TOTAL_MONEY
                )
            )
            sb.append(stringCell(4, r, "${model.recordedSettlements.size} payment(s)", STYLE_TOTAL_LABEL, sanitizeUserInput = false))
            sb.append("</row>")
        }

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun sheetHeader(freezeRows: Int): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        if (freezeRows > 0) {
            append("<sheetViews><sheetView workbookViewId=\"0\">")
            append("<pane ySplit=\"$freezeRows\" topLeftCell=\"A${freezeRows + 1}\" activePane=\"bottomLeft\" state=\"frozen\"/>")
            append("</sheetView></sheetViews>")
        }
    }

    private fun colDef(index1Based: Int, width: Double): String =
        "<col min=\"$index1Based\" max=\"$index1Based\" width=\"$width\" customWidth=\"1\"/>"

    private fun stringCell(
        colIndex0Based: Int,
        rowNum: Int,
        text: String,
        style: Int,
        sanitizeUserInput: Boolean = true
    ): String {
        val safe = if (sanitizeUserInput) GroupLedgerExportBuilder.sanitizeSpreadsheetText(text) else text
        val ref = "${colLetter(colIndex0Based)}$rowNum"
        return "<c r=\"$ref\" s=\"$style\" t=\"inlineStr\"><is><t>${escapeXml(safe)}</t></is></c>"
    }

    private fun moneyCell(colIndex0Based: Int, rowNum: Int, cents: Long, style: Int): String {
        val ref = "${colLetter(colIndex0Based)}$rowNum"
        val decimal = GroupLedgerExportBuilder.formatCentsDecimal(cents)
        return "<c r=\"$ref\" s=\"$style\"><v>$decimal</v></c>"
    }

    private fun intCell(colIndex0Based: Int, rowNum: Int, value: Long, style: Int): String {
        val ref = "${colLetter(colIndex0Based)}$rowNum"
        return "<c r=\"$ref\" s=\"$style\"><v>$value</v></c>"
    }

    private fun formulaCell(
        colIndex0Based: Int,
        rowNum: Int,
        formula: String,
        cachedCents: Long,
        style: Int
    ): String {
        val ref = "${colLetter(colIndex0Based)}$rowNum"
        val cached = GroupLedgerExportBuilder.formatCentsDecimal(cachedCents)
        return "<c r=\"$ref\" s=\"$style\"><f>${escapeXml(formula)}</f><v>$cached</v></c>"
    }

    internal fun colLetter(index0Based: Int): String {
        var n = index0Based
        val sb = StringBuilder()
        while (n >= 0) {
            sb.append(('A'.code + (n % 26)).toChar())
            n = (n / 26) - 1
        }
        return sb.reverse().toString()
    }

    private fun escapeXml(input: String): String = buildString(input.length) {
        for (ch in input) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (ch.code >= 0x20 || ch == '\t') append(ch)
            }
        }
    }
}
