package com.splitmate.app.ui.components

import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.ExpenseSplitEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.ParsedTravelTicket
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.extractTravelTicketFromTitle
import com.splitmate.app.ui.cleanDisplayExpenseTitle
import com.splitmate.app.ui.formatIndianRupeesFromCents
import com.splitmate.app.ui.screens.TripHubBookingCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ==============================================================================
// v2.4.0 rc10: Money tab category cards — UI models + safe mapper (Phase 1)
// ==============================================================================
//
// Read-only layer: turns existing Room rows (ExpenseEntity / ExpenseSplitEntity /
// GroupMemberEntity) into the display models the category cards render. It never writes,
// never changes split math, and formats money only through `formatIndianRupeesFromCents` and
// the ViewModel's own `resolveExpenseSplitBreakdown`, so every figure matches the rest of the app.
//
// Fields SplitMate does not store (room type, dish notes, operator, check-out without a nights
// count, ride IDs) are nullable and hidden by the cards; nothing is invented.

data class StayExpenseData(
    val id: String,
    val resortName: String,
    /** Booking platform / provider when logged (e.g. "Zostel"); otherwise null. */
    val locationTag: String?,
    val checkInDate: String,
    /** Only when the title carries a nights count ("3 nights"). */
    val checkOutDate: String?,
    val nightsCount: Int?,
    val payerName: String,
    val totalAmountFormatted: String,
    val perPersonFormatted: String,
    val travelerCount: Int,
    val splitSummary: String,
    /** Booking / confirmation reference when logged (travelPnr); otherwise null. */
    val confirmationCode: String?
)

data class TransitHopItem(
    val expenseId: String,
    val timeLabel: String,
    val destination: String,
    val amountFormatted: String,
    val riderCount: Int,
    val isRental: Boolean,
    /** v2.4.0 rc11: who paid this row, set only when the batch has more than one payer. */
    val payerName: String? = null
)

data class TransitBatchData(
    val id: String,
    val batchTitle: String,
    val batchSubtitle: String,
    /** One name when a single person paid every ride, otherwise "N people". */
    val payerName: String,
    val totalAmountFormatted: String,
    val totalRidesCount: Int,
    /** "Paid by Akshay", or per-payer totals when several people paid ("Sam ₹4,860.00 · Akshay ₹1,320.00"). */
    val payerSummary: String = "Paid by $payerName",
    val multiplePayers: Boolean = false,
    /** Every payer with subtotal and entries, biggest first (filled when [multiplePayers]). */
    val payers: List<PayerShare> = emptyList(),
    /** Every ride, newest first; the card shows the first [TransitBatchDefaults.COLLAPSED_HOPS]. */
    val hops: List<TransitHopItem>
)

data class FoodDiningData(
    val id: String,
    val restaurantName: String,
    /** "Breakfast" / "Lunch" / "Dinner" / … only when the title says so. */
    val mealType: String?,
    val locationName: String?,
    val payerName: String,
    val totalAmountFormatted: String,
    val perPersonFormatted: String,
    val splitCount: Int,
    val isEqualSplit: Boolean,
    val dateLabel: String
)

data class ActivityTourData(
    val id: String,
    val activityTitle: String,
    val operatorName: String?,
    val payerName: String,
    val totalAmountFormatted: String,
    val perPersonFormatted: String,
    val peopleCount: Int,
    val dateLabel: String
)

/** Shopping / groceries / other: same card frame, rose family. */
data class SundriesExpenseData(
    val id: String,
    val title: String,
    val categoryLabel: String,
    val payerName: String,
    val totalAmountFormatted: String,
    val perPersonFormatted: String,
    val splitCount: Int,
    val dateLabel: String
)

/**
 * v2.4.0 rc10: the Money tab's compact ticket row (`CompactLedgerTicketStub`, same row the Groups feed uses).
 * [ticket] is parsed from the title; when the title carries no ticket segment, it falls back to the stored
 * PNR / provider so the row is never empty. [openKey] is what the review screen expects: the 10-digit PNR
 * for trains, `EXPENSE:<id>` for flights (same keys the full ticket cards pass).
 */
data class TicketRowData(
    val ticket: ParsedTravelTicket,
    val isFlight: Boolean,
    val totalFormatted: String,
    /** Per-person share for an equal split, else blank (the row then shows no "each" figure). */
    val perPersonShare: String,
    val openKey: String
)

/**
 * v2.4.0 rc11: one card for every expense of a category that has 2+ entries (Food & dining,
 * Activities, Shopping & other), same pattern as the cabs batch, so ten meals are one card with
 * ten short rows instead of ten full cards. A category with a single expense keeps its rich card.
 */
data class CategoryBatchData(
    val id: String,
    val kind: com.splitmate.app.ui.theme.ExpenseCardKind,
    val title: String,
    val subtitle: String,
    /** One name when a single person paid everything, otherwise "N people". */
    val payerName: String,
    val totalAmountFormatted: String,
    val count: Int,
    /** "Paid by Akshay", or per-payer totals when several people paid ("Sam ₹4,860.00 · Akshay ₹1,320.00"). */
    val payerSummary: String = "Paid by $payerName",
    val multiplePayers: Boolean = false,
    /** Every payer with subtotal and entries, biggest first (filled when [multiplePayers]). */
    val payers: List<PayerShare> = emptyList(),
    /** "meal" / "activity" / "item" (used for "View all 6 meals"). */
    val unitSingular: String,
    val unitPlural: String,
    /** Every entry, newest first; the card shows the first [TransitBatchDefaults.COLLAPSED_HOPS]. */
    val rows: List<TransitHopItem>
)

/** v2.4.0 rc11: one payer inside a batch, for the "Who paid" sheet. */
data class PayerShare(
    val name: String,
    val totalFormatted: String,
    val count: Int,
    /** This payer's entries, newest first (same order as the batch rows). */
    val expenseIds: List<String>
)

object TransitBatchDefaults {
    /** Rides visible before "View all" is tapped. */
    const val COLLAPSED_HOPS = 3
}

/** One Money-tab list entry, in display order. [key] is unique even if expense ids collide. */
sealed interface MoneyExpenseItem {
    val key: String

    data class Stay(override val key: String, val data: StayExpenseData, val expense: ExpenseEntity) : MoneyExpenseItem
    data class Transit(override val key: String, val data: TransitBatchData, val expenses: List<ExpenseEntity>) : MoneyExpenseItem
    data class Food(override val key: String, val data: FoodDiningData, val expense: ExpenseEntity) : MoneyExpenseItem
    data class Activity(override val key: String, val data: ActivityTourData, val expense: ExpenseEntity) : MoneyExpenseItem
    data class Sundries(override val key: String, val data: SundriesExpenseData, val expense: ExpenseEntity) : MoneyExpenseItem
    /** 2+ expenses of one category (food / activities / shopping & other) in one card. */
    data class Batch(override val key: String, val data: CategoryBatchData, val expenses: List<ExpenseEntity>) : MoneyExpenseItem
    /** Train / flight: the compact ticket row (the full ticket card lives in Overview and Plan). */
    data class Ticket(
        override val key: String,
        val expense: ExpenseEntity,
        val category: TripHubBookingCategory,
        val data: TicketRowData
    ) : MoneyExpenseItem
}

object ExpenseUiMappers {

    private val NIGHTS = Regex("""\b(\d{1,2})\s*nights?\b""", RegexOption.IGNORE_CASE)
    private val MEAL = Regex("""\b(breakfast|brunch|lunch|dinner|snacks?|drinks|coffee|tea)\b""", RegexOption.IGNORE_CASE)

    private fun dayFormat() = SimpleDateFormat("d MMM", Locale.US)
    private fun dayYearFormat() = SimpleDateFormat("EEE, d MMM", Locale.US)
    private fun timeFormat() = SimpleDateFormat("h:mm a", Locale.US)

    /** The moment the expense happened: scheduled time when logged, else when it was created. */
    fun effectiveMs(expense: ExpenseEntity): Long = expense.scheduledAtEpochMs ?: expense.createdAt

    private fun payerNameOf(expense: ExpenseEntity, members: List<GroupMemberEntity>): String =
        members.find { it.memberId == expense.payerId }?.name?.let(::displayPersonName)?.ifBlank { null } ?: "Member"

    /**
     * v2.4.0 rc11: names as people expect to read them, whatever case they were typed in
     * ("sam" -> "Sam", "PRIYA SHAH" -> "Priya Shah"). A word typed in mixed case keeps its casing
     * ("McKenzie"), so a real name is never broken.
     */
    fun displayPersonName(raw: String): String = com.splitmate.app.ui.PersonNames.display(raw)

    private fun breakdownOf(
        expense: ExpenseEntity,
        members: List<GroupMemberEntity>,
        splits: List<ExpenseSplitEntity>
    ) = SplitMateViewModel.resolveExpenseSplitBreakdown(expense, members, splits)

    private fun perPerson(b: SplitMateViewModel.ExpenseSplitBreakdownSummary): String =
        b.perPersonCaption("${b.perPersonHeadlineShare} / person")

    private fun isEqualSplit(expense: ExpenseEntity, splits: List<ExpenseSplitEntity>): Boolean {
        val owed = splits.filter { it.expenseId == expense.expenseId && it.finalOwedCents > 0L }.map { it.finalOwedCents }
        if (owed.size < 2) return true
        // Largest Remainder leaves at most 1 paise of difference on an equal split.
        return owed.max() - owed.min() <= 1L
    }

    fun toStay(expense: ExpenseEntity, members: List<GroupMemberEntity>, splits: List<ExpenseSplitEntity>): StayExpenseData {
        val b = breakdownOf(expense, members, splits)
        val nights = NIGHTS.find(expense.title)?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..60 }
        val start = effectiveMs(expense)
        return StayExpenseData(
            id = expense.expenseId,
            resortName = cleanDisplayExpenseTitle(expense.title),
            locationTag = expense.providerName.trim().ifBlank { null },
            checkInDate = dayYearFormat().format(Date(start)),
            checkOutDate = nights?.let { dayYearFormat().format(Date(start + TimeUnit.DAYS.toMillis(it.toLong()))) },
            nightsCount = nights,
            payerName = payerNameOf(expense, members),
            totalAmountFormatted = formatIndianRupeesFromCents(expense.totalAmountCents),
            perPersonFormatted = perPerson(b),
            travelerCount = b.splittingMembersCount,
            splitSummary = b.compactDetail(if (isEqualSplit(expense, splits)) "Shared equally" else "Custom split"),
            confirmationCode = expense.travelPnr.trim().ifBlank { null }
        )
    }

    fun toTransitBatch(
        expenses: List<ExpenseEntity>,
        members: List<GroupMemberEntity>,
        splits: List<ExpenseSplitEntity>
    ): TransitBatchData {
        val newestFirst = expenses.sortedWith(ExpenseOrdering.newestFirst({ it }, { effectiveMs(it) }))
        val days = newestFirst.map { dayFormat().format(Date(effectiveMs(it))) }.distinct()
        val multiDay = days.size > 1
        val mixedPayers = expenses.map { it.payerId }.distinct().size > 1
        val hops = newestFirst.map { exp ->
            val ms = effectiveMs(exp)
            TransitHopItem(
                expenseId = exp.expenseId,
                timeLabel = if (multiDay) dayFormat().format(Date(ms)) else timeFormat().format(Date(ms)),
                destination = cleanDisplayExpenseTitle(exp.title),
                amountFormatted = formatIndianRupeesFromCents(exp.totalAmountCents),
                riderCount = breakdownOf(exp, members, splits).splittingMembersCount,
                isRental = exp.expenseCategory.equals("RENTAL", ignoreCase = true),
                payerName = if (mixedPayers) payerNameOf(exp, members) else null
            )
        }
        val payers = expenses.map { it.payerId }.distinct()
        val rides = expenses.size
        val rentals = hops.count { it.isRental }
        val title = when {
            rentals == 0 -> "Cabs & rides"
            rentals == rides -> "Rentals"
            else -> "Cabs, rides & rentals"
        }
        return TransitBatchData(
            id = "transit_batch",
            batchTitle = title,
            batchSubtitle = buildString {
                append(if (rides == 1) "1 ride" else "$rides rides")
                append(" · ")
                append(if (days.size == 1) days.first() else "${days.size} days")
            },
            payerName = if (payers.size == 1) payerNameOf(expenses.first(), members) else "${payers.size} people",
            totalAmountFormatted = formatIndianRupeesFromCents(expenses.sumOf { it.totalAmountCents }),
            totalRidesCount = rides,
            payerSummary = payerSummaryOf(expenses, members),
            multiplePayers = mixedPayers,
            payers = if (mixedPayers) payerSharesOf(newestFirst, members) else emptyList(),
            hops = hops
        )
    }

    /**
     * v2.4.0 rc11: who paid what inside a batch. One payer -> "Paid by Akshay". Several -> each payer's
     * subtotal, biggest first ("Sam ₹4,860.00 · Akshay ₹1,320.00"), at most 3 names then "+N more".
     * Display only: balances still come from the splits / settle-up engine.
     */
    fun payerSummaryOf(expenses: List<ExpenseEntity>, members: List<GroupMemberEntity>): String {
        val shares = payerSharesOf(expenses, members)
        if (shares.size <= 1) return "Paid by ${shares.firstOrNull()?.name ?: "Member"}"
        val shown = shares.take(3).joinToString(" · ") { "${it.name} ${it.totalFormatted}" }
        return if (shares.size > 3) "$shown · +${shares.size - 3} more" else shown
    }

    /** Every payer of [expenses] with subtotal and entries, biggest subtotal first (ties by name). */
    fun payerSharesOf(expenses: List<ExpenseEntity>, members: List<GroupMemberEntity>): List<PayerShare> =
        expenses.groupBy { it.payerId }.values
            .map { list -> Triple(payerNameOf(list.first(), members), list.sumOf { it.totalAmountCents }, list) }
            .sortedWith(compareByDescending<Triple<String, Long, List<ExpenseEntity>>> { it.second }.thenBy { it.first })
            .map { (name, cents, list) -> PayerShare(name, formatIndianRupeesFromCents(cents), list.size, list.map { it.expenseId }) }

    /** Which batch a category belongs to (null = never batched: stays, tickets; cabs use the transit batch). */
    fun batchKindOf(category: TripHubBookingCategory): com.splitmate.app.ui.theme.ExpenseCardKind? = when (category) {
        TripHubBookingCategory.FOOD -> com.splitmate.app.ui.theme.ExpenseCardKind.FOOD
        TripHubBookingCategory.ACTIVITIES -> com.splitmate.app.ui.theme.ExpenseCardKind.ACTIVITY
        TripHubBookingCategory.SHOPPING, TripHubBookingCategory.GENERAL, TripHubBookingCategory.ALL ->
            com.splitmate.app.ui.theme.ExpenseCardKind.SUNDRIES
        else -> null
    }

    fun toCategoryBatch(
        kind: com.splitmate.app.ui.theme.ExpenseCardKind,
        expenses: List<ExpenseEntity>,
        categories: Map<String, TripHubBookingCategory>,
        members: List<GroupMemberEntity>,
        splits: List<ExpenseSplitEntity>
    ): CategoryBatchData {
        val newestFirst = expenses.sortedWith(ExpenseOrdering.newestFirst({ it }, { effectiveMs(it) }))
        val days = newestFirst.map { dayFormat().format(Date(effectiveMs(it))) }.distinct()
        val multiDay = days.size > 1
        val mixedPayers = expenses.map { it.payerId }.distinct().size > 1
        val rows = newestFirst.map { exp ->
            val ms = effectiveMs(exp)
            val name = cleanDisplayExpenseTitle(exp.title)
            val meal = if (kind == com.splitmate.app.ui.theme.ExpenseCardKind.FOOD) {
                MEAL.find(exp.title)?.value?.lowercase(Locale.US)?.replaceFirstChar { it.titlecase(Locale.US) }
            } else null
            TransitHopItem(
                expenseId = exp.expenseId,
                timeLabel = if (multiDay) dayFormat().format(Date(ms)) else timeFormat().format(Date(ms)),
                // Meal type leads only when the title doesn't already start with it ("Dinner · Thalassa").
                destination = if (meal != null && !name.startsWith(meal, ignoreCase = true)) "$meal · $name" else name,
                amountFormatted = formatIndianRupeesFromCents(exp.totalAmountCents),
                riderCount = breakdownOf(exp, members, splits).splittingMembersCount,
                isRental = false,
                payerName = if (mixedPayers) payerNameOf(exp, members) else null
            )
        }
        val (title, singular, plural) = when (kind) {
            com.splitmate.app.ui.theme.ExpenseCardKind.FOOD -> Triple("Food & dining", "meal", "meals")
            com.splitmate.app.ui.theme.ExpenseCardKind.ACTIVITY -> Triple("Activities", "activity", "activities")
            else -> Triple(
                if (expenses.all { categories[it.expenseId] == TripHubBookingCategory.SHOPPING }) "Shopping" else "Shopping & other",
                "item", "items"
            )
        }
        val payers = expenses.map { it.payerId }.distinct()
        val n = expenses.size
        return CategoryBatchData(
            id = "batch_${kind.name.lowercase(Locale.US)}",
            kind = kind,
            title = title,
            subtitle = "${if (n == 1) "1 $singular" else "$n $plural"} · ${if (days.size == 1) days.first() else "${days.size} days"}",
            payerName = if (payers.size == 1) payerNameOf(expenses.first(), members) else "${payers.size} people",
            totalAmountFormatted = formatIndianRupeesFromCents(expenses.sumOf { it.totalAmountCents }),
            count = n,
            payerSummary = payerSummaryOf(expenses, members),
            multiplePayers = mixedPayers,
            payers = if (mixedPayers) payerSharesOf(newestFirst, members) else emptyList(),
            unitSingular = singular,
            unitPlural = plural,
            rows = rows
        )
    }

    fun toFood(expense: ExpenseEntity, members: List<GroupMemberEntity>, splits: List<ExpenseSplitEntity>): FoodDiningData {
        val b = breakdownOf(expense, members, splits)
        return FoodDiningData(
            id = expense.expenseId,
            restaurantName = cleanDisplayExpenseTitle(expense.title),
            mealType = MEAL.find(expense.title)?.value?.lowercase(Locale.US)?.replaceFirstChar { it.titlecase(Locale.US) },
            locationName = expense.providerName.trim().ifBlank { null },
            payerName = payerNameOf(expense, members),
            totalAmountFormatted = formatIndianRupeesFromCents(expense.totalAmountCents),
            perPersonFormatted = perPerson(b),
            splitCount = b.splittingMembersCount,
            isEqualSplit = isEqualSplit(expense, splits),
            dateLabel = dayFormat().format(Date(effectiveMs(expense)))
        )
    }

    fun toActivity(expense: ExpenseEntity, members: List<GroupMemberEntity>, splits: List<ExpenseSplitEntity>): ActivityTourData {
        val b = breakdownOf(expense, members, splits)
        return ActivityTourData(
            id = expense.expenseId,
            activityTitle = cleanDisplayExpenseTitle(expense.title),
            operatorName = expense.providerName.trim().ifBlank { null },
            payerName = payerNameOf(expense, members),
            totalAmountFormatted = formatIndianRupeesFromCents(expense.totalAmountCents),
            perPersonFormatted = perPerson(b),
            peopleCount = b.splittingMembersCount,
            dateLabel = dayFormat().format(Date(effectiveMs(expense)))
        )
    }

    fun toSundries(
        expense: ExpenseEntity,
        category: TripHubBookingCategory,
        members: List<GroupMemberEntity>,
        splits: List<ExpenseSplitEntity>
    ): SundriesExpenseData {
        val b = breakdownOf(expense, members, splits)
        return SundriesExpenseData(
            id = expense.expenseId,
            title = cleanDisplayExpenseTitle(expense.title),
            categoryLabel = if (category == TripHubBookingCategory.SHOPPING) "Shopping" else "Other",
            payerName = payerNameOf(expense, members),
            totalAmountFormatted = formatIndianRupeesFromCents(expense.totalAmountCents),
            perPersonFormatted = perPerson(b),
            splitCount = b.splittingMembersCount,
            dateLabel = dayFormat().format(Date(effectiveMs(expense)))
        )
    }

    private val PNR10 = Regex("""\b(\d{10})\b""")

    fun toTicketRow(
        expense: ExpenseEntity,
        category: TripHubBookingCategory,
        members: List<GroupMemberEntity>,
        splits: List<ExpenseSplitEntity>
    ): TicketRowData {
        val isFlight = category == TripHubBookingCategory.FLIGHT
        val parsed = extractTravelTicketFromTitle(expense.title)
        val ticket = (parsed ?: ParsedTravelTicket(cleanTitle = cleanDisplayExpenseTitle(expense.title))).let { t ->
            t.copy(
                pnr = t.pnr.ifBlank { expense.travelPnr.trim() },
                trainOrCarrierName = t.trainOrCarrierName.ifBlank { expense.providerName.trim() },
                cleanTitle = t.cleanTitle.ifBlank { cleanDisplayExpenseTitle(expense.title) }
            )
        }
        val b = breakdownOf(expense, members, splits)
        val trainPnr = ticket.pnr.takeIf { it.isNotBlank() }
            ?: PNR10.find(expense.title)?.groupValues?.getOrNull(1).orEmpty()
        return TicketRowData(
            ticket = ticket,
            isFlight = isFlight,
            totalFormatted = formatIndianRupeesFromCents(expense.totalAmountCents),
            perPersonShare = if (b.splittingMembersCount > 1 && isEqualSplit(expense, splits)) b.perPersonHeadlineShare else "",
            openKey = if (isFlight) "EXPENSE:${expense.expenseId}" else trainPnr
        )
    }

    /**
     * Builds the Money-tab list in the same order as [classified] (the Trip Hub's chronological
     * order). Cabs and rentals collapse into ONE transit batch placed where the first ride sits.
     * List keys stay unique even if two rows share an expense id (ids are millisecond based).
     */
    fun buildMoneyExpenseItems(
        classified: List<Pair<ExpenseEntity, TripHubBookingCategory>>,
        members: List<GroupMemberEntity>,
        splits: List<ExpenseSplitEntity>
    ): List<MoneyExpenseItem> {
        val transitExpenses = classified
            .filter { it.second == TripHubBookingCategory.CAB || it.second == TripHubBookingCategory.RENTAL }
            .map { it.first }
        val usedKeys = HashSet<String>()
        fun uniqueKey(base: String): String {
            var k = base
            var n = 1
            while (!usedKeys.add(k)) k = "${base}_${n++}"
            return k
        }
        // v2.4.0 rc11: categories with 2+ expenses collapse into one batch card, placed where the first one sits.
        val categoryById = classified.associate { it.first.expenseId to it.second }
        val batchMembers = classified.groupBy({ batchKindOf(it.second) }, { it.first }).filterKeys { it != null }
        val batchedKinds = batchMembers.filterValues { it.size >= 2 }.keys
        val placedBatches = HashSet<com.splitmate.app.ui.theme.ExpenseCardKind>()
        val out = ArrayList<MoneyExpenseItem>()
        var transitPlaced = false
        for ((exp, cat) in classified) {
            val batchKind = batchKindOf(cat)
            if (batchKind != null && batchKind in batchedKinds) {
                if (placedBatches.add(batchKind)) {
                    val group = batchMembers.getValue(batchKind)
                    out += MoneyExpenseItem.Batch(
                        uniqueKey("batch_${batchKind.name.lowercase(Locale.US)}"),
                        toCategoryBatch(batchKind, group, categoryById, members, splits),
                        group
                    )
                }
                continue
            }
            when (cat) {
                TripHubBookingCategory.CAB, TripHubBookingCategory.RENTAL -> if (!transitPlaced) {
                    transitPlaced = true
                    out += MoneyExpenseItem.Transit(uniqueKey("transit_batch"), toTransitBatch(transitExpenses, members, splits), transitExpenses)
                }
                TripHubBookingCategory.STAY -> out += MoneyExpenseItem.Stay(uniqueKey("stay_${exp.expenseId}"), toStay(exp, members, splits), exp)
                TripHubBookingCategory.FOOD -> out += MoneyExpenseItem.Food(uniqueKey("food_${exp.expenseId}"), toFood(exp, members, splits), exp)
                TripHubBookingCategory.ACTIVITIES -> out += MoneyExpenseItem.Activity(uniqueKey("activity_${exp.expenseId}"), toActivity(exp, members, splits), exp)
                TripHubBookingCategory.TRAIN, TripHubBookingCategory.FLIGHT ->
                    out += MoneyExpenseItem.Ticket(uniqueKey("ticket_${exp.expenseId}"), exp, cat, toTicketRow(exp, cat, members, splits))
                TripHubBookingCategory.SHOPPING, TripHubBookingCategory.GENERAL, TripHubBookingCategory.ALL ->
                    out += MoneyExpenseItem.Sundries(uniqueKey("other_${exp.expenseId}"), toSundries(exp, cat, members, splits), exp)
            }
        }
        return out
    }
}
