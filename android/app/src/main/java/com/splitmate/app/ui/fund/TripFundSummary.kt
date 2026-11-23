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

data class TripFundHolderSummary(
    val treasurerMemberId: String,
    val treasurerName: String,
    val totalCollectedCents: Long,
    val contributorCount: Int,
    val treasurerSpentCents: Long,
    val advances: List<SettlementEntity>
) {
    /** Positive = cash still held in hand; 0 = pool spent exactly; negative = treasurer spent out of pocket. */
    val cashInHandCents: Long
        get() = totalCollectedCents - treasurerSpentCents

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
        val groupAdvances = settlements
            .filter { it.groupId == groupId && it.isTripFundAdvance() && it.amountCents > 0L }
            .sortedByDescending { it.settledAt }
        if (groupAdvances.isEmpty()) return emptyList()

        val groupMembers = members.filter { it.groupId == groupId }
        val memberNameById = groupMembers.associate { it.memberId to it.name }
        val groupExpenses = expenses.filter { it.groupId == groupId }

        return groupAdvances
            .groupBy { it.toMemberId }
            .map { (treasurerId, receivedList) ->
                val treasurerName = memberNameById[treasurerId]
                    ?: receivedList.first().toMemberName.ifBlank { "Member" }
                val totalCollected = receivedList.sumOf { it.amountCents }
                val contributors = receivedList.map { it.fromMemberId }.distinct().size
                val spentByTreasurer = groupExpenses
                    .filter { it.payerId == treasurerId }
                    .sumOf { it.totalAmountCents }
                TripFundHolderSummary(
                    treasurerMemberId = treasurerId,
                    treasurerName = treasurerName,
                    totalCollectedCents = totalCollected,
                    contributorCount = contributors,
                    treasurerSpentCents = spentByTreasurer,
                    advances = receivedList
                )
            }
            .sortedByDescending { it.totalCollectedCents }
    }

    /**
     * True when [memberId] currently has a negative net balance ONLY because they are holding
     * unspent Trip Fund cash from other members (so the UI can frame it calmly as
     * `"Holding ₹X Trip Fund cash"` instead of alarming `"You owe ₹X"`).
     */
    fun isCustodialPoolBalance(
        memberId: String?,
        netBalanceCents: Long,
        summaries: List<TripFundHolderSummary>
    ): Boolean {
        if (memberId == null || netBalanceCents >= 0L) return false
        val holder = summaries.firstOrNull { it.treasurerMemberId == memberId } ?: return false
        return holder.cashInHandCents > 0L
    }
}
