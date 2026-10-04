package com.splitmate.app

import com.splitmate.app.SplitMateMathEngine.MemberNetBalance
import com.splitmate.app.SplitMateMathEngine.SimplifiedTransfer
import com.splitmate.app.ui.share.SettleUpNetDirection
import com.splitmate.app.ui.share.SettleUpShareLayout
import com.splitmate.app.ui.share.SettleUpShareMember
import com.splitmate.app.ui.share.SettleUpShareModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * v2.3.5 (#2): settle-up shared as an image. Tests the pure [SettleUpShareModel] / layout used by
 * the Canvas renderer: integer-cent formatting, row ordering, all-settled state, no UPI / phone
 * text anywhere, long-name ellipsis and height growth.
 */
class SplitMateV235ShareImageTest {

    private val members = listOf(
        SettleUpShareMember("m_me", "You", isCurrentUser = true),
        SettleUpShareMember("m_rohan", "Rohan"),
        SettleUpShareMember("m_priya", "Priya"),
        SettleUpShareMember("m_dev", "Dev")
    )

    private fun greedy(vararg nets: Pair<String, Long>): List<SimplifiedTransfer> =
        SplitMateMathEngine.simplifyDebtsGreedy(
            nets.map { (id, net) -> MemberNetBalance(id, members.first { it.memberId == id }.name, net) }
        )

    @Test
    fun formatsAmountsFromIntegerCentsWithIndianGroupingAndRupee() {
        assertEquals("₹1,240.50", SettleUpShareModel.formatMoney(124_050L))
        assertEquals("₹1,23,456.78", SettleUpShareModel.formatMoney(12_345_678L))
        assertEquals("₹0.01", SettleUpShareModel.formatMoney(1L))
        assertEquals("₹0.00", SettleUpShareModel.formatMoney(0L))
    }

    @Test
    fun buildsRowsFromGreedyOutputOrderedByAmountAndResolvesCurrentUserName() {
        val transfers = greedy("m_me" to 150_000L, "m_rohan" to -100_000L, "m_priya" to -30_000L, "m_dev" to -20_000L)
        val model = SettleUpShareModel.build(
            tripName = "  Goa   Trip ",
            transfers = transfers,
            members = members,
            totalSpentCents = 400_000L,
            currentUserName = "Akshay"
        )
        assertFalse(model.isAllSettled)
        assertEquals("Goa Trip", model.tripName)
        assertEquals("₹4,000.00", model.totalSpentText)
        assertEquals(listOf(100_000L, 30_000L, 20_000L), model.transferRows.map { it.amountCents })
        assertEquals(listOf("Rohan", "Priya", "Dev"), model.transferRows.map { it.fromName })
        assertTrue(model.transferRows.all { it.toName == "Akshay" }, "'You' must become the real name in a shared image")
        assertEquals("₹1,000.00", model.transferRows.first().amountText)
        // Sum of rows equals the greedy output exactly (no drift).
        assertEquals(transfers.sumOf { it.amountCents }, model.transferRows.sumOf { it.amountCents })
        assertTrue(model.transfersTitle.contains("3 payments"))
        assertEquals("4 travellers", model.metaLine)
    }

    @Test
    fun equalAmountsTieBreakDeterministicallyByName() {
        val transfers = listOf(
            SimplifiedTransfer("m_rohan", "Rohan", "m_me", "You", 5_000L),
            SimplifiedTransfer("m_dev", "Dev", "m_me", "You", 5_000L)
        )
        val model = SettleUpShareModel.build("Trip", transfers, members, 10_000L, currentUserName = "Akshay")
        assertEquals(listOf("Dev", "Rohan"), model.transferRows.map { it.fromName })
    }

    @Test
    fun netSummaryIsDerivedFromTransfersAndSortedCreditorsFirst() {
        val transfers = greedy("m_me" to 150_000L, "m_rohan" to -100_000L, "m_priya" to -30_000L, "m_dev" to -20_000L)
        val model = SettleUpShareModel.build("Trip", transfers, members, 400_000L, currentUserName = "Akshay")
        assertEquals(listOf("Akshay", "Dev", "Priya", "Rohan"), model.netRows.map { it.name })
        assertEquals(listOf(150_000L, -20_000L, -30_000L, -100_000L), model.netRows.map { it.netCents })
        assertEquals(SettleUpNetDirection.GETS_BACK, model.netRows.first().direction)
        assertEquals("gets back ₹1,500.00", model.netRows.first().netText)
        assertEquals("pays ₹1,000.00", model.netRows.last().netText)
        assertEquals(0L, model.netRows.sumOf { it.netCents })
    }

    @Test
    fun allSettledStateHasNoRowsAndNoNetSummary() {
        val model = SettleUpShareModel.build("Manali", emptyList(), members, 250_000L)
        assertTrue(model.isAllSettled)
        assertTrue(model.transferRows.isEmpty())
        assertTrue(model.netRows.isEmpty())
        assertTrue(model.allSettledTitle.startsWith("Everyone is settled"))
        assertTrue(model.allVisibleText().contains(model.allSettledTitle))
        assertEquals("Made with SplitMate", model.footerText)
        val layout = SettleUpShareLayout.of(model)
        assertEquals(SettleUpShareLayout.SETTLED_CARD_H, layout.transfersCardHeight)
        assertNull(layout.netCardTop)
    }

    @Test
    fun noUpiOrPhoneTextAnywhereInTheImage() {
        val leaky = listOf(
            SettleUpShareMember("a", "rohan@okaxis"),
            SettleUpShareMember("b", "9876543210"),
            SettleUpShareMember("c", "9876543210@ybl"),
            SettleUpShareMember("d", "Priya (priya.s@oksbi)")
        )
        val transfers = listOf(
            SimplifiedTransfer("a", "rohan@okaxis", "d", "Priya", 1_000L),
            SimplifiedTransfer("b", "9876543210", "d", "Priya", 2_000L),
            SimplifiedTransfer("c", "9876543210@ybl", "d", "Priya", 3_000L)
        )
        val model = SettleUpShareModel.build("Trip", transfers, leaky, 6_000L)
        val all = model.allVisibleText()
        assertTrue(all.none { it.contains("@") }, "No UPI handle may appear: $all")
        assertTrue(all.none { it.contains("UPI", ignoreCase = true) }, "No UPI text may appear: $all")
        assertTrue(all.none { Regex("\\d{10}").containsMatchIn(it) }, "No phone number may appear: $all")
        assertTrue(model.transferRows.any { it.fromName == "rohan" })
        assertTrue(model.transferRows.count { it.fromName == "Member" } == 2)
    }

    @Test
    fun longNamesAreEllipsizedWithoutSplittingEmojiOrDevanagari() {
        val longName = "Venkatanarasimharajuvaripeta Ramachandran"
        val emojiName = "\uD83D\uDE00".repeat(30)
        val hindi = "अक्षय कराडकर बहुत लंबा नाम वाला यात्री सदस्य"
        val ppl = listOf(
            SettleUpShareMember("x", longName),
            SettleUpShareMember("y", emojiName),
            SettleUpShareMember("z", hindi)
        )
        val transfers = listOf(
            SimplifiedTransfer("x", longName, "z", hindi, 10_000L),
            SimplifiedTransfer("y", emojiName, "z", hindi, 5_000L)
        )
        val model = SettleUpShareModel.build("A".repeat(80), transfers, ppl, 15_000L)
        model.transferRows.forEach { row ->
            listOf(row.fromName, row.toName).forEach { n ->
                assertTrue(n.codePointCount(0, n.length) <= SettleUpShareModel.MAX_NAME_CODE_POINTS)
                assertFalse(Character.isHighSurrogate(n[n.length - 2]) && n.last() == '…', "split surrogate: $n")
            }
        }
        assertTrue(model.transferRows.first().fromName.endsWith("…"))
        assertTrue(model.tripName.endsWith("…"))
        val emojiRow = model.transferRows.first { it.fromName.startsWith("\uD83D") }
        val body = emojiRow.fromName.removeSuffix("…")
        assertEquals(0, body.length % 2, "emoji must stay whole surrogate pairs")
    }

    @Test
    fun layoutGrowsWithRowsAndCapsHugeGroups() {
        fun model(n: Int): SettleUpShareModel {
            val ppl = (0..n).map { SettleUpShareMember("p$it", "Person $it") }
            val transfers = (1..n).map { SimplifiedTransfer("p$it", "Person $it", "p0", "Person 0", 100L * it) }
            return SettleUpShareModel.build("Trip", transfers, ppl, 1_000_000L, includeNetSummary = false)
        }
        val h1 = SettleUpShareLayout.of(model(1)).height
        val h5 = SettleUpShareLayout.of(model(5)).height
        assertEquals(4 * SettleUpShareLayout.TRANSFER_ROW_H, h5 - h1)
        assertEquals(1080, SettleUpShareLayout.of(model(1)).width)

        val huge = model(60)
        assertEquals(SettleUpShareModel.MAX_TRANSFER_ROWS, huge.transferRows.size)
        assertEquals(20, huge.hiddenTransferCount)
        assertEquals("+20 more payments", huge.moreTransfersText)
        assertTrue(SettleUpShareLayout.of(huge).height < 10_000)
    }

    @Test
    fun dateRangeAndBadgesFormatCleanly() {
        val utc = ZoneId.of("UTC")
        fun ms(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atStartOfDay(utc).toInstant().toEpochMilli()
        assertEquals("12–18 Mar 2026", SettleUpShareModel.formatDateRange(ms(2026, 3, 12), ms(2026, 3, 18), utc))
        assertEquals("28 Mar – 2 Apr 2026", SettleUpShareModel.formatDateRange(ms(2026, 4, 2), ms(2026, 3, 28), utc))
        assertEquals("30 Dec 2025 – 3 Jan 2026", SettleUpShareModel.formatDateRange(ms(2025, 12, 30), ms(2026, 1, 3), utc))
        assertEquals("As of 4 Oct 2026", SettleUpShareModel.formatAsOf(ms(2026, 10, 4), utc))

        val model = SettleUpShareModel.build(
            "Trip", emptyList(), members, 0L,
            dateRangeLabel = "12–18 Mar 2026", tripEnded = true, asOfLabel = "As of 4 Oct 2026"
        )
        assertEquals("Trip ended", model.tripEndedBadge)
        assertEquals("12–18 Mar 2026 · 4 travellers", model.metaLine)
        assertNull(SettleUpShareModel.build("Trip", emptyList(), members, 0L).tripEndedBadge)
    }

    @Test
    fun fileProviderIsDeclaredWithApplicationIdPlaceholderAndCachePath() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("androidx.core.content.FileProvider"))
        assertTrue(manifest.contains("\${applicationId}.fileprovider"))
        val paths = File("src/main/res/xml/file_paths.xml").readText()
        assertTrue(paths.contains("cache-path") && paths.contains("shared_images/"))
        val renderer = File("src/main/java/com/splitmate/app/ui/share/SettleUpImageRenderer.kt").readText()
        assertFalse(Regex("(?i)\\bupi").containsMatchIn(renderer), "Renderer must not draw UPI data")
    }
}
