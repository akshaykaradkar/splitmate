package com.splitmate.app

import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.data.SettlementEntity
import com.splitmate.app.ui.export.GroupExportFormat
import com.splitmate.app.ui.export.GroupLedgerCsvWriter
import com.splitmate.app.ui.export.GroupLedgerExportBuilder
import com.splitmate.app.ui.export.GroupLedgerExportHelper
import com.splitmate.app.ui.export.GroupLedgerXlsxWriter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.ZoneOffset
import java.util.zip.ZipInputStream

/**
 * SplitMate v2.4.0-rc23 — Per-Group Excel (.xlsx) & CSV (.csv) Ledger Export verification suite.
 *
 * Verifies:
 * - Exact 0.00c-drift per-member share reconciliation across equal, custom/unequal, and recorded settlements.
 * - Greedy Minimum Cash Flow simplification metrics (naive debt legs -> minimum transfers -> transactions saved).
 * - Multi-sheet OOXML .xlsx ZIP validity, 3 worksheets, live =SUM / =SUMIF / =B-C+D-E formulas, and cached values.
 * - UTF-8 BOM CSV output, RFC 4180 escaping, and spreadsheet formula-injection neutralization.
 * - Group isolation (expenses/members from other groups are never leaked into an exported group).
 * - FileProvider path registration (`shared_exports/`) and UI wiring in TripHomeScreen & Settle screen.
 */
@DisplayName("SplitMate v2.4.0-rc23 Per-Group Excel & CSV Ledger Export")
class SplitMateV240Rc23GroupExportTest {

    private fun sampleModel() = GroupLedgerExportBuilder.build(
        groupId = "grp_goa",
        groupName = "Goa Trip 2026",
        currencyCode = "INR",
        currencySymbol = "₹",
        members = listOf(
            GroupMemberEntity("m_ak", "grp_goa", "Akshay", "seed1", isCurrentUser = true),
            GroupMemberEntity("m_pr", "grp_goa", "Priya", "seed2"),
            GroupMemberEntity("m_sm", "grp_goa", "Sam", "seed3"),
            GroupMemberEntity("m_other", "grp_other", "Intruder", "seed4")
        ),
        expenses = listOf(
            ExpenseEntity(
                expenseId = "e1",
                groupId = "grp_goa",
                title = "Beach Shack Dinner",
                payerId = "m_ak",
                baseSubtotalCents = 10_000L, // ₹100.00 split among 3 (33.34 + 33.33 + 33.33)
                taxCents = 0L,
                tipCents = 0L,
                totalAmountCents = 10_000L,
                lockedMultiplier = 1.0,
                unassignedBaseCents = 0L,
                currencyCode = "INR",
                lockedExchangeRate = 1.0,
                expenseCategory = "DINING",
                createdAt = 1_760_000_000_000L
            ),
            ExpenseEntity(
                expenseId = "e2",
                groupId = "grp_goa",
                title = "=SUM(A1:A2) Ferry Tickets", // Formula injection payload test
                payerId = "m_pr",
                baseSubtotalCents = 6_000L, // ₹60.00 custom split (10 + 20 + 30)
                taxCents = 0L,
                tipCents = 0L,
                totalAmountCents = 6_000L,
                lockedMultiplier = 1.0,
                unassignedBaseCents = 0L,
                currencyCode = "INR",
                lockedExchangeRate = 1.0,
                expenseCategory = "TRANSPORT",
                travelPnr = "8753634406",
                createdAt = 1_760_086_400_000L
            ),
            ExpenseEntity(
                expenseId = "e_leak",
                groupId = "grp_other",
                title = "Other Group Secret",
                payerId = "m_other",
                baseSubtotalCents = 99_900L,
                taxCents = 0L,
                tipCents = 0L,
                totalAmountCents = 99_900L,
                lockedMultiplier = 1.0,
                unassignedBaseCents = 0L,
                currencyCode = "INR",
                lockedExchangeRate = 1.0,
                createdAt = 1_760_000_000_000L
            )
        ),
        splits = listOf(
            ExpenseSplitEntity("s1", "e1", "m_ak", 3334L, 3334L, true),
            ExpenseSplitEntity("s2", "e1", "m_pr", 3333L, 3333L, false),
            ExpenseSplitEntity("s3", "e1", "m_sm", 3333L, 3333L, false),
            ExpenseSplitEntity("s4", "e2", "m_ak", 1000L, 1000L, false),
            ExpenseSplitEntity("s5", "e2", "m_pr", 2000L, 2000L, false),
            ExpenseSplitEntity("s6", "e2", "m_sm", 3000L, 3000L, false)
        ),
        settlements = listOf(
            SettlementEntity(
                settlementId = "set1",
                groupId = "grp_goa",
                fromMemberId = "m_sm",
                fromMemberName = "Sam",
                toMemberId = "m_ak",
                toMemberName = "Akshay",
                amountCents = 2_000L, // ₹20.00 already paid
                currencyCode = "INR",
                lockedExchangeRate = 1.0,
                settledAt = 1_760_100_000_000L
            )
        ),
        generatedAtEpochMs = 1_760_200_000_000L,
        zoneId = ZoneOffset.UTC
    )

    @Test
    fun `RC23_01 export model reconciles balances with 0_00c drift and isolates group data`() {
        val model = sampleModel()

        // Group isolation
        assertEquals(3, model.members.size)
        assertEquals(2, model.expenses.size)
        assertFalse(model.members.any { it.displayName == "Intruder" })
        assertEquals(16_000L, model.totalSpendCents)

        // Exact balances:
        // Akshay: paid 10000, share 4334, sent 0, received 2000 -> net = +3666
        // Priya:  paid 6000,  share 5333, sent 0, received 0    -> net = +667
        // Sam:    paid 0,     share 6333, sent 2000, received 0 -> net = -4333
        val byId = model.memberBalances.associateBy { it.memberId }
        assertEquals(3666L, byId["m_ak"]!!.netBalanceCents)
        assertEquals(667L, byId["m_pr"]!!.netBalanceCents)
        assertEquals(-4333L, byId["m_sm"]!!.netBalanceCents)
        assertEquals(0L, model.memberBalances.sumOf { it.netBalanceCents }, "Sum of all net balances must be 0.00c")

        // Greedy Minimum Cash Flow: 4 naive debt legs simplified to 2 transfers (Sam->Akshay 36.66, Sam->Priya 6.67)
        assertEquals(4, model.naiveDebtLegCount)
        assertEquals(2, model.pendingTransfers.size)
        assertEquals(2, model.transactionsSaved)
        assertEquals(3666L, model.pendingTransfers[0].amountCents)
        assertEquals("Sam", model.pendingTransfers[0].fromName)
        assertEquals("Akshay", model.pendingTransfers[0].toName)
        assertEquals(667L, model.pendingTransfers[1].amountCents)
        assertEquals("Sam", model.pendingTransfers[1].fromName)
        assertEquals("Priya", model.pendingTransfers[1].toName)
        assertEquals(1, model.recordedSettlements.size)
    }

    @Test
    fun `RC23_02 XLSX writer produces valid 3-sheet OOXML zip with live formulas and injection safety`() {
        val model = sampleModel()
        val bytes = GroupLedgerXlsxWriter.writeToBytes(model)
        assertTrue(bytes.size > 1000, "XLSX archive should be non-empty")

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes().toString(StandardCharsets.UTF_8)
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // All required OOXML parts exist
        listOf(
            "[Content_Types].xml",
            "_rels/.rels",
            "xl/workbook.xml",
            "xl/_rels/workbook.xml.rels",
            "xl/styles.xml",
            "xl/worksheets/sheet1.xml",
            "xl/worksheets/sheet2.xml",
            "xl/worksheets/sheet3.xml"
        ).forEach { part ->
            assertTrue(entries.containsKey(part), "Missing OOXML part: $part")
        }

        val workbook = entries["xl/workbook.xml"]!!
        assertTrue(workbook.contains("name=\"Expenses\""))
        assertTrue(workbook.contains("name=\"Member Balances\""))
        assertTrue(workbook.contains("name=\"Minimum Settlement Plan\""))

        val sheet1 = entries["xl/worksheets/sheet1.xml"]!!
        assertTrue(sheet1.contains("Beach Shack Dinner"))
        assertTrue(sheet1.contains("&apos;=SUM(A1:A2) Ferry Tickets"), "Formula-injection payload must be prefixed with apostrophe and XML-escaped")
        assertTrue(sheet1.contains("<f>SUM(F2:F3)</f><v>160.00</v>"), "Expenses total formula + cached value")
        assertTrue(sheet1.contains("<f>SUM(I2:K2)-F2</f><v>0.00</v>"), "Per-row 0.00 drift check formula")
        assertTrue(sheet1.contains("PNR: 8753634406"))

        val sheet2 = entries["xl/worksheets/sheet2.xml"]!!
        assertTrue(sheet2.contains("SUMIF(Expenses!\$E\$2:\$E\$3,A2,Expenses!\$F\$2:\$F\$3)"), "Live SUMIF for Paid column")
        assertTrue(sheet2.contains("<f>B2-C2+D2-E2</f><v>36.66</v>"), "Live Net Balance formula + cached value")
        assertTrue(sheet2.contains("<f>SUM(F2:F4)</f><v>0.00</v>"), "Zero-sum net balance check")

        val sheet3 = entries["xl/worksheets/sheet3.xml"]!!
        assertTrue(sheet3.contains("Greedy Minimum Cash Flow"))
        assertTrue(sheet3.contains("<v>36.66</v>"))
        assertTrue(sheet3.contains("<v>6.67</v>"))
        assertTrue(sheet3.contains("<v>20.00</v>"))
        assertFalse(sheet3.contains("Intruder"))
    }

    @Test
    fun `RC23_03 CSV writer outputs UTF-8 BOM, all 4 sections, sanitized text and safe filenames`() {
        val model = sampleModel()
        val csv = GroupLedgerCsvWriter.writeToString(model)

        assertTrue(csv.startsWith("\uFEFF"), "CSV must start with UTF-8 BOM for clean ₹ rendering in Excel")
        assertTrue(csv.contains("SECTION 1: EXPENSES & PER-MEMBER SHARES"))
        assertTrue(csv.contains("SECTION 2: MEMBER BALANCES"))
        assertTrue(csv.contains("SECTION 3: MINIMUM SETTLEMENT PLAN (GREEDY MINIMUM CASH FLOW)"))
        assertTrue(csv.contains("SECTION 4: RECORDED / PAID SETTLEMENTS"))
        assertTrue(csv.contains("'=SUM(A1:A2) Ferry Tickets"))
        assertTrue(csv.contains("36.66"))
        assertTrue(csv.contains("6.67"))
        assertFalse(csv.contains("Other Group Secret"))

        assertEquals("Goa_Trip_2026_SplitMate_Ledger.xlsx", GroupLedgerExportHelper.buildSafeFileName("Goa Trip 2026!", GroupExportFormat.EXCEL_XLSX))
        assertEquals("Goa_Trip_2026_SplitMate_Ledger.csv", GroupLedgerExportHelper.buildSafeFileName("Goa Trip 2026!", GroupExportFormat.CSV))
        assertEquals("AA", GroupLedgerXlsxWriter.colLetter(26))
    }

    @Test
    fun `RC23_04 FileProvider registers shared_exports and TripHub + Settle screens expose export entrypoints`() {
        val filePaths = File("src/main/res/xml/file_paths.xml").readText()
        assertTrue(filePaths.contains("path=\"shared_exports/\""), "FileProvider must expose shared_exports/")

        val tripHub = File("src/main/java/com/splitmate/app/ui/screens/TripHomeScreen.kt").readText()
        assertTrue(tripHub.contains("Export Excel / CSV"))
        assertTrue(tripHub.contains("GroupLedgerExportSheet("))
        assertTrue(tripHub.contains("GroupLedgerExportButton("))

        val app = File("src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt").readText()
        assertTrue(app.contains("GroupLedgerExportButton("))
        assertTrue(app.contains("GroupLedgerExportSheet("))
    }
}
