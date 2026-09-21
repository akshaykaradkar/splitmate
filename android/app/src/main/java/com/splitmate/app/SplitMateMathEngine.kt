package com.splitmate.app

import java.util.PriorityQueue
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * SplitMate Mathematical Engine (100% Pure Kotlin, Zero-Drift Integer Cents)
 *
 * Implements:
 * 1. Proportional Auxiliary Cost Distribution with Locked Multiplier ($m = T / B$)
 * 2. Real-time Remainder Engine (`unassignedBaseCents` & `unassignedFinalCents`)
 * 3. Largest Remainder Method (`0.00¢ drift` penny reconciliation)
 * 4. Greedy Minimum Cash Flow Debt Simplification (`PriorityQueue` Max-Heaps $V_+$ and $V_-$)
 */
object SplitMateMathEngine {

    data class SplitAllocation(
        val memberId: String,
        val displayName: String,
        val baseClaimedCents: Long,
        val rawExactCents: Double,
        val flooredCents: Long,
        val fractionalRemainder: Double,
        var finalCents: Long,
        var plusOneCent: Boolean = false
    )

    data class ProportionalSplitResult(
        val lockedMultiplier: Double,
        val baseSubtotalCents: Long,
        val taxCents: Long,
        val tipCents: Long,
        val totalFinalCents: Long,
        val unassignedBaseCents: Long,
        val unassignedFinalCents: Long,
        val allocations: List<SplitAllocation>,
        val driftCents: Long = 0L
    )

    data class MemberNetBalance(
        val memberId: String,
        val displayName: String,
        val netCents: Long
    ) : Comparable<MemberNetBalance> {
        override fun compareTo(other: MemberNetBalance): Int {
            // Max-Priority Queue ordering by largest balance first, deterministic tie-breaker by memberId
            val cmp = other.netCents.compareTo(this.netCents)
            return if (cmp != 0) cmp else this.memberId.compareTo(other.memberId)
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
    fun splitEquallyZeroDrift(
        totalCents: Long,
        members: List<Pair<String, String>>
    ): List<SplitAllocation> {
        if (members.isEmpty() || totalCents <= 0L) {
            return members.map { (id, name) ->
                SplitAllocation(id, name, 0L, 0.0, 0L, 0.0, 0L, false)
            }
        }
        val count = members.size
        val exactEach = totalCents.toDouble() / count.toDouble()
        val baseFloor = totalCents / count
        val remainderPennies = (totalCents % count).toInt()

        return members.mapIndexed { idx, (id, name) ->
            val getsExtraPenny = idx < remainderPennies
            val finalShare = if (getsExtraPenny) baseFloor + 1L else baseFloor
            SplitAllocation(
                memberId = id,
                displayName = name,
                baseClaimedCents = finalShare,
                rawExactCents = exactEach,
                flooredCents = baseFloor,
                fractionalRemainder = exactEach - baseFloor,
                finalCents = finalShare,
                plusOneCent = getsExtraPenny
            )
        }
    }

    /**
     * Proportional Auxiliary Splitting & Remainder Engine:
     * Users input base costs of their items; the engine locks $m = T / B$, tracks the real-time
     * unassigned remainder (`unassignedBaseCents`), and distributes tax & tip proportionally
     * using the Largest Remainder Method (`0.00¢ drift`).
     */
    fun calculateProportionalReceiptSplits(
        baseSubtotalCents: Long,
        taxCents: Long,
        tipCents: Long,
        payerId: String,
        memberBaseClaimsCents: List<Triple<String, String, Long>>,
        attributeRemainderToPayer: Boolean = true
    ): ProportionalSplitResult {
        val totalFinalCents = baseSubtotalCents + taxCents + tipCents
        val multiplier = if (baseSubtotalCents > 0L) {
            totalFinalCents.toDouble() / baseSubtotalCents.toDouble()
        } else {
            1.0
        }

        val sumClaimedBase = memberBaseClaimsCents.sumOf { it.third }
        val unassignedBaseCents = (baseSubtotalCents - sumClaimedBase).coerceAtLeast(0L)
        val unassignedFinalCents = (unassignedBaseCents.toDouble() * multiplier).roundToLong()

        // Effective base per member (temporarily holding unassigned remainder on Payer if enabled)
        val effectiveClaims = memberBaseClaimsCents.map { (id, name, claimed) ->
            val effectiveBase = if (attributeRemainderToPayer && id == payerId) {
                claimed + unassignedBaseCents
            } else {
                claimed
            }
            Triple(id, name, effectiveBase)
        }

        val allocations = effectiveClaims.map { (id, name, baseCents) ->
            val exactFinal = baseCents.toDouble() * multiplier
            val fl = floor(exactFinal).toLong()
            SplitAllocation(
                memberId = id,
                displayName = name,
                baseClaimedCents = baseCents,
                rawExactCents = exactFinal,
                flooredCents = fl,
                fractionalRemainder = exactFinal - fl.toDouble(),
                finalCents = fl,
                plusOneCent = false
            )
        }.toMutableList()

        val targetSum = if (attributeRemainderToPayer) {
            totalFinalCents
        } else {
            (totalFinalCents - unassignedFinalCents).coerceAtLeast(0L)
        }

        val currentSum = allocations.sumOf { it.flooredCents }
        val discrepancy = (targetSum - currentSum).toInt()

        if (discrepancy > 0 && allocations.any { it.baseClaimedCents > 0L }) {
            val sortedIndices = allocations.indices
                .filter { allocations[it].baseClaimedCents > 0L }
                .sortedByDescending { allocations[it].fractionalRemainder }
            for (i in 0 until min(discrepancy, sortedIndices.size)) {
                val idx = sortedIndices[i]
                allocations[idx].finalCents += 1L
                allocations[idx].plusOneCent = true
            }
        }

        return ProportionalSplitResult(
            lockedMultiplier = multiplier,
            baseSubtotalCents = baseSubtotalCents,
            taxCents = taxCents,
            tipCents = tipCents,
            totalFinalCents = totalFinalCents,
            unassignedBaseCents = unassignedBaseCents,
            unassignedFinalCents = unassignedFinalCents,
            allocations = allocations,
            driftCents = targetSum - allocations.sumOf { it.finalCents }
        )
    }

    /**
     * Greedy Minimum Cash Flow Debt Simplification Algorithm:
     * Uses two Max-Priority Queues ($V_+$ Creditors and $V_-$ Debtors) to eliminate cyclic debts
     * and reduce $O(N^2)$ cross-debts into at most $N - 1$ optimal transfers.
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
            if (settledAmount > 0L) {
                transfers.add(
                    SimplifiedTransfer(
                        fromMemberId = maxDebtor.memberId,
                        fromName = maxDebtor.displayName,
                        toMemberId = maxCreditor.memberId,
                        toName = maxCreditor.displayName,
                        amountCents = settledAmount
                    )
                )
            }

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
