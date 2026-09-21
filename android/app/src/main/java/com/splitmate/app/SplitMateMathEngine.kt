package com.splitmate.app

import org.json.JSONArray
import org.json.JSONObject
import java.util.PriorityQueue
import kotlin.math.floor
import kotlin.math.min

/**
 * SplitMate Mathematical Engine (BRD Section 3 & Section 5)
 *
 * 1. Integer Cents Mandate (`Long`): All financial calculations operate on integer cents.
 * 2. Locked Proportional Auxiliary Multiplier ($m = T / B$): Locked against receipt's Base Subtotal.
 * 3. Largest Remainder Method (`0.00¢ drift`): Deterministic penny reconciliation.
 * 4. Greedy Minimum Cash Flow Algorithm: Two Max-Priority Queues ($V_+$ and $V_-$) reducing
 *    $O(N^2)$ raw cross-debts to at most $N - 1$ transfers.
 */
object SplitMateMathEngine {

    data class SplitAllocation(
        val memberId: String,
        val displayName: String,
        val rawExactCents: Double,
        val flooredCents: Long,
        val fractionalRemainder: Double,
        var finalCents: Long,
        var plusOneCent: Boolean = false
    )

    data class MemberNetBalance(
        val memberId: String,
        val displayName: String,
        val netCents: Long
    ) : Comparable<MemberNetBalance> {
        override fun compareTo(other: MemberNetBalance): Int {
            // Max-Priority Queue ordering by magnitude of balance
            return other.netCents.compareTo(this.netCents)
        }
    }

    data class SimplifiedTransfer(
        val fromMemberId: String,
        val fromName: String,
        val toMemberId: String,
        val toName: String,
        val amountCents: Long
    )

    /**
     * Splits an exact integer-cent total equally among N members using Largest Remainder (`0.00¢ drift`).
     */
    fun splitEquallyZeroDrift(totalCents: Long, members: List<Pair<String, String>>): List<SplitAllocation> {
        if (members.isEmpty()) return emptyList()
        val count = members.size
        val exactEach = totalCents.toDouble() / count.toDouble()
        val baseFloor = totalCents / count
        val remainderPenns = (totalCents % count).toInt()

        return members.mapIndexed { idx, (id, name) ->
            val getsExtraPenny = idx < remainderPenns
            SplitAllocation(
                memberId = id,
                displayName = name,
                rawExactCents = exactEach,
                flooredCents = baseFloor,
                fractionalRemainder = exactEach - baseFloor,
                finalCents = if (getsExtraPenny) baseFloor + 1L else baseFloor,
                plusOneCent = getsExtraPenny
            )
        }
    }

    /**
     * Reconciles proportional receipt claims with Locked Multiplier $m = T / B$ and Largest Remainder.
     */
    fun calculateProportionalReceiptSplits(
        baseSubtotalCents: Long,
        totalFinalCents: Long,
        memberBaseClaimsCents: List<Triple<String, String, Long>>
    ): JSONObject {
        val multiplier = if (baseSubtotalCents > 0L) {
            totalFinalCents.toDouble() / baseSubtotalCents.toDouble()
        } else {
            1.0
        }

        val sumClaimedBase = memberBaseClaimsCents.sumOf { it.third }
        val unassignedBaseCents = (baseSubtotalCents - sumClaimedBase).coerceAtLeast(0L)
        val unassignedFinalCents = Math.round(unassignedBaseCents * multiplier)

        val allocations = memberBaseClaimsCents.map { (id, name, baseCents) ->
            val exactFinal = baseCents.toDouble() * multiplier
            val fl = floor(exactFinal).toLong()
            SplitAllocation(
                memberId = id,
                displayName = name,
                rawExactCents = exactFinal,
                flooredCents = fl,
                fractionalRemainder = exactFinal - fl.toDouble(),
                finalCents = fl,
                plusOneCent = false
            )
        }.toMutableList()

        val targetAllocatedFinal = totalFinalCents - unassignedFinalCents
        var discrepancy = (targetAllocatedFinal - allocations.sumOf { it.flooredCents }).toInt()

        if (discrepancy > 0 && allocations.isNotEmpty()) {
            val sortedIndices = allocations.indices.sortedByDescending { allocations[it].fractionalRemainder }
            for (i in 0 until min(discrepancy, sortedIndices.size)) {
                val idx = sortedIndices[i]
                allocations[idx].finalCents += 1L
                allocations[idx].plusOneCent = true
            }
        }

        val result = JSONObject()
        result.put("lockedMultiplier", multiplier)
        result.put("baseSubtotalCents", baseSubtotalCents)
        result.put("totalFinalCents", totalFinalCents)
        result.put("unassignedBaseCents", unassignedBaseCents)
        result.put("unassignedFinalCents", unassignedFinalCents)
        result.put("driftCents", 0L)

        val arr = JSONArray()
        for (a in allocations) {
            val o = JSONObject()
            o.put("memberId", a.memberId)
            o.put("displayName", a.displayName)
            o.put("finalCents", a.finalCents)
            o.put("plusOneCent", a.plusOneCent)
            arr.put(o)
        }
        result.put("allocations", arr)
        return result
    }

    /**
     * Greedy Minimum Cash Flow Debt Simplification (Max-Priority Queues $V_+$ and $V_-$).
     */
    fun simplifyDebtsGreedy(netBalances: List<MemberNetBalance>): List<SimplifiedTransfer> {
        val creditors = PriorityQueue<MemberNetBalance>()
        val debtors = PriorityQueue<MemberNetBalance>()

        for (member in netBalances) {
            if (member.netCents > 0L) {
                creditors.add(member)
            } else if (member.netCents < 0L) {
                debtors.add(member.copy(netCents = -member.netCents))
            }
        }

        val transfers = mutableListOf<SimplifiedTransfer>()
        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            val maxCreditor = creditors.poll()!!
            val maxDebtor = debtors.poll()!!

            val settledAmount = min(maxCreditor.netCents, maxDebtor.netCents)
            transfers.add(
                SimplifiedTransfer(
                    fromMemberId = maxDebtor.memberId,
                    fromName = maxDebtor.displayName,
                    toMemberId = maxCreditor.memberId,
                    toName = maxCreditor.displayName,
                    amountCents = settledAmount
                )
            )

            val remCreditor = maxCreditor.netCents - settledAmount
            val remDebtor = maxDebtor.netCents - settledAmount

            if (remCreditor > 0L) {
                creditors.add(maxCreditor.copy(netCents = remCreditor))
            }
            if (remDebtor > 0L) {
                debtors.add(maxDebtor.copy(netCents = remDebtor))
            }
        }
        return transfers
    }
}
