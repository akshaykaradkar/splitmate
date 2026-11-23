package com.splitmate.app.ui.fund

import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.data.SettlementEntity

/**
 * Pure domain model & calculator for SplitMate's **Trip Fund** (pre-trip advances / pooled cash).
 *
 * Advances are stored as first-class [SettlementEntity] records with `settlementId` prefixed by
 * [ADVANCE_ID_PREFIX] (`"advance_"`). Because they are directed member-to-member transfers rather
 * than [ExpenseEntity] rows:
 * - They NEVER inflate `Total Trip Spend` or per-person averages.
 * - They reconcile identically with `0.00¢` drift in `SplitMateMathEngine.simplifyDebtsGreedy`.
 * - They sync losslessly across all devices (including older clients) with zero Room schema bump.
 */
const val ADVANCE_ID_PREFIX = "advance_"

fun SettlementEntity.isTripFundAdvance(): Boolean =
    settlementId.startsWith(ADVANCE_ID_PREFIX)

/** Money actually repaid so far: Trip Fund advances are money handed over up front, not repayments. */
fun repaidCentsExcludingAdvances(settlements: List<SettlementEntity>): Long =
    settlements.filterNot { it.isTripFundAdvance() }.sumOf { it.amountCents }.coerceAtLeast(0L)

/** Badge on the Settle history card, e.g. "2 Paid" or "2 Paid · 1 advance". */
fun settleHistoryBadgeLabel(settlements: List<SettlementEntity>): String {
    val advances = settlements.count { it.isTripFundAdvance() }
    val paid = settlements.size - advances
    return when {
        advances == 0 -> "$paid Paid"
        paid == 0 -> if (advances == 1) "1 advance" else "$advances advances"
        else -> "$paid Paid · " + if (advances == 1) "1 advance" else "$advances advances"
    }
}

/** Row title in the Settle history: an advance is never worded as a repayment. */
fun settleHistoryRowTitle(settlement: SettlementEntity, fromShortName: String, toShortName: String): String =
    if (settlement.isTripFundAdvance()) "$fromShortName → $toShortName's Trip Fund" else "$fromShortName paid $toShortName"

data class TripFundHolderSummary(
    val treasurerMemberId: String,
    val treasurerName: String,
    /** Advances received by the holder minus advances the holder sent back. */
    val totalCollectedCents: Long,
    val contributorCount: Int,
    /** Every expense the holder paid in this trip. */
    val treasurerSpentCents: Long,
    /** Advances to the holder, and advances the holder sent back, newest first. */
    val advances: List<SettlementEntity>,
    /** Ordinary payments (Mark paid / Record a payment) from the holder to contributors. */
    val returnedCents: Long = 0L
) {
    /** Positive = cash still held; 0 = fund used exactly; negative = holder paid from their own pocket. */
    val cashInHandCents: Long
        get() = totalCollectedCents - treasurerSpentCents - returnedCents

    val isPoolDepleted: Boolean
        get() = cashInHandCents < 0L
}

object TripFundCalculator {

    fun summarize(
        groupId: String,
        members: List<GroupMemberEntity>,
        expenses: List<ExpenseEntity>,
        settlements: List<SettlementEntity>
    ): List<TripFundHolderSummary> {
        val groupSettlements = settlements.filter { it.groupId == groupId && it.amountCents > 0L }
        val groupAdvances = groupSettlements.filter { it.isTripFundAdvance() }
        if (groupAdvances.isEmpty()) return emptyList()

        val groupMembers = members.filter { it.groupId == groupId }
        val memberNameById = groupMembers.associate { it.memberId to it.name }
        val groupExpenses = expenses.filter { it.groupId == groupId }

        return groupAdvances
            .groupBy { it.toMemberId }
            .mapNotNull { (holderId, receivedList) ->
                // An advance the holder sent back reduces this fund; it does not start a second one.
                val sentBack = groupAdvances.filter { it.fromMemberId == holderId }
                val collected = receivedList.sumOf { it.amountCents } - sentBack.sumOf { it.amountCents }
                if (collected <= 0L) return@mapNotNull null
                val contributorIds = receivedList.map { it.fromMemberId }.toSet()
                val returned = groupSettlements
                    .filter { !it.isTripFundAdvance() && it.fromMemberId == holderId && it.toMemberId in contributorIds }
                    .sumOf { it.amountCents }
                TripFundHolderSummary(
                    treasurerMemberId = holderId,
                    treasurerName = memberNameById[holderId]
                        ?: receivedList.first().toMemberName.ifBlank { "Member" },
                    totalCollectedCents = collected,
                    contributorCount = contributorIds.size,
                    treasurerSpentCents = groupExpenses.filter { it.payerId == holderId }.sumOf { it.totalAmountCents },
                    advances = (receivedList + sentBack).sortedByDescending { it.settledAt },
                    returnedCents = returned
                )
            }
            .sortedByDescending { it.totalCollectedCents }
    }

    /**
     * True when [memberId]'s balance is negative only because they hold other people's Trip Fund
     * money: without the advances they would owe nothing. Someone who owes money for expenses on
     * top of the fund gets the normal "You owe" wording, so a real debt is never hidden.
     */
    fun isCustodialPoolBalance(
        memberId: String?,
        netBalanceCents: Long,
        summaries: List<TripFundHolderSummary>
    ): Boolean {
        if (memberId == null || netBalanceCents >= 0L) return false
        val holder = summaries.firstOrNull { it.treasurerMemberId == memberId } ?: return false
        return netBalanceCents + holder.totalCollectedCents >= 0L
    }
}

/**
 * Amounts to record for the ticked [contributorIds]. A person with an entry in
 * [perMemberAmountText] uses exactly that text, so a cleared field means no amount for them (it
 * does not fall back to the default). Unparseable or zero amounts are left out.
 */
fun resolveTripFundContributions(
    contributorIds: List<String>,
    defaultAmountText: String,
    perMemberAmountText: Map<String, String>
): Map<String, Long> =
    contributorIds.mapNotNull { id ->
        val raw = perMemberAmountText[id] ?: defaultAmountText
        val cents = com.splitmate.app.ui.parseRupeesToPaiseExact(raw)?.takeIf { it > 0L } ?: return@mapNotNull null
        id to cents
    }.toMap()
