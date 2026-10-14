package com.splitmate.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.SplitMateThemeMode
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.components.ExpenseUiMappers
import com.splitmate.app.ui.components.MoneyExpenseItem
import com.splitmate.app.ui.formatIndianRupeesFromCents
import com.splitmate.app.ui.screens.TripHubBookingCategory
import com.splitmate.app.ui.theme.ExpenseCardKind
import com.splitmate.app.ui.theme.HubExtendedColors
import com.splitmate.app.ui.toPalette
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * v2.4.0 rc10: Money tab category cards. The mapper is read-only (no math, no writes); these
 * tests pin that every amount still comes from the same formatter / split helper, totals are
 * preserved, cabs and rentals collapse into one batch, nothing is invented, and the cards follow
 * the colour, type, icon and component rules.
 */
class SplitMateV240Rc10MoneyCardsTest {

    private fun src(path: String) = File("src/main/java/com/splitmate/app/$path").readText()

    private val members = listOf(
        GroupMemberEntity("m1", "g", "Akshay", "Akshay", isCurrentUser = true),
        GroupMemberEntity("m2", "g", "Sam", "Sam"),
        GroupMemberEntity("m3", "g", "Priya", "Priya"),
        GroupMemberEntity("m4", "g", "Maya", "Maya")
    )

    private fun expense(
        id: String,
        title: String,
        cents: Long,
        category: String,
        payer: String = "m1",
        at: Long = 1_790_000_000_000L,
        provider: String = "",
        pnr: String = ""
    ) = ExpenseEntity(
        expenseId = id, groupId = "g", title = title, payerId = payer,
        baseSubtotalCents = cents, taxCents = 0, tipCents = 0, totalAmountCents = cents,
        lockedMultiplier = 1.0, unassignedBaseCents = 0, currencyCode = "INR", lockedExchangeRate = 1.0,
        expenseCategory = category, travelPnr = pnr, providerName = provider, createdAt = at
    )

    /** Largest-remainder equal split across all four members. */
    private fun equalSplits(e: ExpenseEntity): List<ExpenseSplitEntity> {
        val base = e.totalAmountCents / 4
        val extra = (e.totalAmountCents % 4).toInt()
        return members.mapIndexed { i, m ->
            val owed = base + if (i < extra) 1 else 0
            ExpenseSplitEntity("s_${e.expenseId}_$i", e.expenseId, m.memberId, owed, owed, i < extra)
        }
    }

    private val stay = expense("e1", "Zostel Goa Anjuna (3 nights)", 1_860_000, "STAY", at = 1_790_000_000_000L, provider = "Zostel")
    private val cab1 = expense("e2", "Uber BOM airport to Andheri", 64_000, "CAB", at = 1_790_000_100_000L)
    private val food = expense("e3", "Dinner at Thalassa", 486_001, "FOOD", payer = "m2", at = 1_790_000_200_000L)
    private val rental = expense("e4", "Scooty rental Anjuna 3 days", 210_000, "RENTAL", payer = "m3", at = 1_790_090_000_000L)
    private val activity = expense("e5", "Parasailing Calangute", 360_000, "ACTIVITIES", at = 1_790_000_300_000L)
    private val shopping = expense("e6", "Flea market shopping", 95_000, "SHOPPING", at = 1_790_000_400_000L)
    private val cab2 = expense("e7", "Auto to Baga", 25_000, "CAB", payer = "m2", at = 1_790_000_500_000L)
    private val flight = expense("e8", "Flight IndiGo 6E 2134 BOM-GOI (PNR QX7K2M)", 1_248_000, "FLIGHT", pnr = "QX7K2M")

    private val all = listOf(stay, cab1, food, rental, activity, shopping, cab2, flight)
    private val splits = all.flatMap { equalSplits(it) }
    private val classified = listOf(
        stay to TripHubBookingCategory.STAY,
        cab1 to TripHubBookingCategory.CAB,
        food to TripHubBookingCategory.FOOD,
        rental to TripHubBookingCategory.RENTAL,
        activity to TripHubBookingCategory.ACTIVITIES,
        shopping to TripHubBookingCategory.SHOPPING,
        cab2 to TripHubBookingCategory.CAB,
        flight to TripHubBookingCategory.FLIGHT
    )

    @Test
    fun `RC10_01 cabs and rentals collapse into one batch placed at the first ride, order otherwise kept`() {
        val items = ExpenseUiMappers.buildMoneyExpenseItems(classified, members, splits)
        assertEquals(
            listOf("Stay", "Transit", "Food", "Activity", "Sundries", "Ticket"),
            items.map { it::class.simpleName }
        )
        val batch = items[1] as MoneyExpenseItem.Transit
        assertEquals(setOf("e2", "e4", "e7"), batch.expenses.map { it.expenseId }.toSet())
        assertEquals(3, batch.data.totalRidesCount)
        assertEquals("Cabs, rides & rentals", batch.data.batchTitle)
        assertEquals("3 people", batch.data.payerName, "three different payers")
        assertEquals(listOf("e7", "e4", "e2").sorted(), batch.data.hops.map { it.expenseId }.sorted())
        assertEquals("e4", batch.data.hops.first().expenseId, "newest ride first")
    }

    @Test
    fun `RC10_02 every figure is the app's own formatting and totals are preserved`() {
        val items = ExpenseUiMappers.buildMoneyExpenseItems(classified, members, splits)
        val s = (items[0] as MoneyExpenseItem.Stay).data
        assertEquals(formatIndianRupeesFromCents(stay.totalAmountCents), s.totalAmountFormatted)
        val b = SplitMateViewModel.resolveExpenseSplitBreakdown(stay, members, splits)
        assertEquals(b.perPersonCaption("${b.perPersonHeadlineShare} / person"), s.perPersonFormatted)
        val batch = (items[1] as MoneyExpenseItem.Transit).data
        assertEquals(formatIndianRupeesFromCents(cab1.totalAmountCents + rental.totalAmountCents + cab2.totalAmountCents), batch.totalAmountFormatted)
        // Every expense appears exactly once across the list (nothing dropped, nothing duplicated).
        val ids = items.flatMap { item ->
            when (item) {
                is MoneyExpenseItem.Stay -> listOf(item.expense.expenseId)
                is MoneyExpenseItem.Transit -> item.expenses.map { it.expenseId }
                is MoneyExpenseItem.Food -> listOf(item.expense.expenseId)
                is MoneyExpenseItem.Activity -> listOf(item.expense.expenseId)
                is MoneyExpenseItem.Sundries -> listOf(item.expense.expenseId)
                is MoneyExpenseItem.Ticket -> listOf(item.expense.expenseId)
            }
        }
        assertEquals(all.map { it.expenseId }.sorted(), ids.sorted())
        // The food split is uneven by 1 paise (largest remainder): still an equal split.
        assertTrue((items[2] as MoneyExpenseItem.Food).data.isEqualSplit)
    }

    @Test
    fun `RC10_03 nothing is invented when the data is not stored`() {
        val plain = expense("p1", "Homestay near beach", 500_000, "STAY")
        val d = ExpenseUiMappers.toStay(plain, members, equalSplits(plain))
        assertNull(d.nightsCount)
        assertNull(d.checkOutDate, "no check-out without a nights count")
        assertNull(d.confirmationCode)
        assertNull(d.locationTag)
        val withNights = ExpenseUiMappers.toStay(stay, members, splits)
        assertEquals(3, withNights.nightsCount)
        assertEquals("Zostel", withNights.locationTag)
        val lunch = ExpenseUiMappers.toFood(expense("f1", "Thali place", 90_000, "FOOD"), members, emptyList())
        assertNull(lunch.mealType, "meal only when the title names it")
        assertEquals("Dinner", ExpenseUiMappers.toFood(food, members, splits).mealType)
        val cards = src("ui/components/ExpenseCategoryCards.kt")
        listOf("#RES-", "\"AK\"", "Category 1", "passes").forEach { assertFalse(cards.contains(it), "placeholder '$it' must not ship") }
    }

    @Test
    fun `RC10_04 list keys stay unique when expense ids collide`() {
        val a = expense("dup", "Breakfast A", 10_000, "FOOD")
        val b = expense("dup", "Breakfast B", 20_000, "FOOD")
        val items = ExpenseUiMappers.buildMoneyExpenseItems(
            listOf(a to TripHubBookingCategory.FOOD, b to TripHubBookingCategory.FOOD), members, emptyList()
        )
        assertEquals(2, items.map { it.key }.toSet().size)
    }

    @Test
    fun `RC10_05 category colours are readable in every theme`() {
        fun cr(a: Color, b: Color): Double {
            val la = a.luminance().toDouble(); val lb = b.luminance().toDouble()
            return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
        }
        SplitMateThemeMode.values().forEach { mode ->
            val card = mode.toPalette().surfaceContainerLowest
            ExpenseCardKind.values().forEach { kind ->
                val c = HubExtendedColors.expenseCategoryCard(kind, mode)
                assertTrue(cr(c.accent, c.headerContainer) >= 4.5, "$mode $kind accent on header ${cr(c.accent, c.headerContainer)}")
                assertTrue(cr(c.accent, c.headerContainerEnd) >= 4.5, "$mode $kind accent on header end")
                assertTrue(cr(c.accent, card) >= 4.5, "$mode $kind accent on card")
                assertTrue(cr(c.onAccent, c.accent) >= 4.5, "$mode $kind badge glyph")
            }
        }
    }

    @Test
    fun `RC10_06 Money tab uses the mapper and cards follow the component and token rules`() {
        val trip = src("ui/screens/TripHomeScreen.kt")
        val money = trip.substringAfter("item(key = \"money_all_expenses_header\")").substringBefore("TripHubSectionTab.PEOPLE ->")
        assertTrue(money.contains("ExpenseUiMappers.buildMoneyExpenseItems("))
        assertFalse(money.contains("items(items = classifiedExpenses"), "no more raw card sandwich list")
        val host = trip.substringAfter("private fun MoneyCategoryExpenseItem(").substringBefore("private data class BerthCellDisplayModel(")
        assertTrue(host.contains("actions?.openDetails"), "split details open the shared expense sheet")
        assertTrue(host.contains("geo:0,0?q="), "Maps uses the same intent as before")
        assertTrue(Regex("TripHubSwipeToManage\\(").findAll(host).count() >= 4, "swipe edit/delete kept on single-expense cards")
        val cards = src("ui/components/ExpenseCategoryCards.kt")
        assertFalse(Regex("""Color\(0x|Color\.(White|Black)\b""").containsMatchIn(cards), "colours come from theme files")
        assertFalse(cards.contains("shadowElevation"), "cards keep the 1dp border, no shadow")
        assertFalse(Regex("""\bIcon\(""").containsMatchIn(cards), "Material Symbols only")
        assertTrue(cards.contains("AssistChip(") && cards.contains("shapes = ButtonDefaults.shapes()"))
        assertTrue(cards.contains("SplitMateMotion.defaultSpatial()") && cards.contains("SplitMateMotion.defaultEffects()"))
        assertFalse(Regex("""fontSize = \d+(\.\d+)?\.sp""").containsMatchIn(cards.substringAfter("private val MoneyAmountStyle").substringAfter(")")),
            "type uses MaterialTheme roles (only the shared amount style sets a size)")
    }

    @Test
    fun `RC10_07 new ids never collide, even when created in the same millisecond`() {
        val sameMs = 1_790_000_000_000L
        val ids = (1..50_000).map { com.splitmate.app.data.SplitMateIds.newId("exp", sameMs) }
        assertEquals(ids.size, ids.toSet().size, "same-millisecond ids must all be unique")
        val v7 = Regex("""^exp_[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$""")
        assertTrue(ids.all { v7.matches(it) }, "format is <prefix>_<RFC 9562 UUIDv7>: ${ids.first()}")
        val earlier = com.splitmate.app.data.SplitMateIds.newId("exp", sameMs)
        val later = com.splitmate.app.data.SplitMateIds.newId("exp", sameMs + 1)
        assertTrue(earlier < later, "ids from a later millisecond sort after earlier ones")
        val uuid = java.util.UUID.fromString(earlier.removePrefix("exp_"))
        assertEquals(7, uuid.version())
        assertEquals(2, uuid.variant())
        assertEquals(sameMs, uuid.mostSignificantBits ushr 16, "timestamp is the leading 48 bits")
    }

    @Test
    fun `RC10_08 no row id is built from the clock alone any more`() {
        val vm = src("ui/SplitMateViewModel.kt")
        val cloud = src("data/CloudGroupSyncRepository.kt")
        val clockId = Regex(""""(exp|m|g|settle)_\$\{?(System\.currentTimeMillis\(\)|nowMs)\b""")
        assertFalse(clockId.containsMatchIn(vm), "ViewModel ids must use SplitMateIds")
        assertFalse(clockId.containsMatchIn(cloud), "sync ids must use SplitMateIds")
        listOf("newId(\"exp\")", "newId(\"g\", nowMs)", "newId(\"m\")", "newId(\"settle\")").forEach {
            assertTrue(vm.contains("SplitMateIds.$it"), "missing SplitMateIds.$it")
        }
    }

    @Test
    fun `RC10_09 lists with identical timestamps always render in the same order`() {
        val at = 1_790_000_000_000L
        val rows = listOf(
            expense("exp_c", "Dinner", 4_860_00, "FOOD", at = at),
            expense("exp_a", "Breakfast", 1_320_00, "FOOD", at = at),
            expense("exp_b", "Lunch", 2_000_00, "FOOD", at = at),
            expense("exp_d", "Coffee", 300_00, "FOOD", at = at - 1)
        )
        val chrono = com.splitmate.app.ui.components.ExpenseOrdering.chronological<ExpenseEntity>({ it }, { at })
        val newest = com.splitmate.app.ui.components.ExpenseOrdering.newestFirst<ExpenseEntity>({ it }, { it.createdAt })
        val expectedChrono = listOf("exp_d", "exp_a", "exp_b", "exp_c")
        val expectedNewest = listOf("exp_a", "exp_b", "exp_c", "exp_d")
        repeat(20) { seed ->
            val shuffled = rows.shuffled(kotlin.random.Random(seed))
            assertEquals(expectedChrono, shuffled.sortedWith(chrono).map { it.expenseId })
            assertEquals(expectedNewest, shuffled.sortedWith(newest).map { it.expenseId })
        }
        val trip = src("ui/screens/TripHomeScreen.kt")
        assertFalse(trip.contains(".sortedBy { resolveExpenseSchedule(context, it).effectiveEpochMs }"), "time-only sort left in Trip Hub")
        assertFalse(trip.contains(".sortedBy { it.third.effectiveEpochMs }"), "time-only sort left in Plan")
        assertTrue(src("ui/components/ExpenseUiMappers.kt").contains("ExpenseOrdering.newestFirst"))
        assertTrue(src("ui/screens/TripHubLayoutRules.kt").contains("ExpenseOrdering"))
    }

    @Test
    fun `RC10_10 Money tickets are the compact row with the total, opening the same review screens`() {
        val train = expense("exp_tr", "Train 12951 Rajdhani PNR 4521876390", 6_400_00, "TRAIN")
        val flight = expense("exp_fl", "IndiGo Goa", 12_480_00, "FLIGHT", provider = "IndiGo", pnr = "QX7K2M")
        val splits = equalSplits(train) + equalSplits(flight)
        val items = ExpenseUiMappers.buildMoneyExpenseItems(
            listOf(train to TripHubBookingCategory.TRAIN, flight to TripHubBookingCategory.FLIGHT), members, splits
        ).filterIsInstance<MoneyExpenseItem.Ticket>()
        assertEquals(2, items.size)
        val tr = items.first { it.expense.expenseId == "exp_tr" }.data
        val fl = items.first { it.expense.expenseId == "exp_fl" }.data
        assertFalse(tr.isFlight)
        assertEquals("4521876390", tr.openKey, "train opens the PNR review")
        assertTrue(fl.isFlight)
        assertEquals("EXPENSE:exp_fl", fl.openKey, "flight opens the flight review by expense")
        assertEquals("QX7K2M", fl.ticket.pnr, "stored PNR is used when the title has none")
        assertEquals("IndiGo", fl.ticket.trainOrCarrierName)
        assertEquals(formatIndianRupeesFromCents(12_480_00), fl.totalFormatted)
        assertEquals(formatIndianRupeesFromCents(3_120_00), fl.perPersonShare)

        val host = src("ui/screens/TripHomeScreen.kt")
            .substringAfter("private fun MoneyCategoryExpenseItem(").substringBefore("private data class BerthCellDisplayModel(")
        assertTrue(host.contains("CompactLedgerTicketStub("), "Money uses the shared compact ticket row")
        assertFalse(host.contains("TripHubFeedCard("), "no full ticket card in the Money list")
        assertTrue(host.contains("trailingTotalDisplay = row.totalFormatted"))
        assertTrue(host.contains("onOpenFlightReviewClick(row.openKey)") && host.contains("onOpenTrainPnrReviewClick(row.openKey)"))
    }
}
