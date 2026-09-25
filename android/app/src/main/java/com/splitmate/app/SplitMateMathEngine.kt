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
     * Canonical Payer-First participant ordering for Largest Remainder (`0.00¢` drift) reconciliation.
     * Guarantees that the Payer (`payerId`, falling back to `currentUserId`) absorbs the first `+1` paise
     * remainder penny, followed by remaining participants in deterministic `memberId` ascending order.
     */
    fun orderParticipantsPayerFirst(
        memberIds: Collection<String>,
        payerId: String?,
        currentUserId: String? = null
    ): List<String> {
        val primaryPayerId = payerId?.takeIf { it.isNotBlank() } ?: currentUserId
        return memberIds.distinct().sortedWith(
            compareByDescending<String> { it == primaryPayerId }
                .thenByDescending { it == currentUserId }
                .thenBy { it }
        )
    }

    /**
     * Splits an exact integer-cent total equally among N members using Largest Remainder (`0.00¢ drift`).
     */
    fun splitEquallyZeroDrift(
        totalCents: Long,
        members: List<Pair<String, String>>,
        payerId: String? = null,
        currentUserId: String? = null
    ): List<SplitAllocation> {
        val orderedMembers = if (payerId != null || currentUserId != null) {
            val memberMap = members.toMap()
            orderParticipantsPayerFirst(members.map { it.first }, payerId, currentUserId)
                .map { id -> id to (memberMap[id] ?: id) }
        } else {
            members
        }

        if (orderedMembers.isEmpty() || totalCents <= 0L) {
            return orderedMembers.map { (id, name) ->
                SplitAllocation(id, name, 0L, 0.0, 0L, 0.0, 0L, false)
            }
        }
        val count = orderedMembers.size
        val exactEach = totalCents.toDouble() / count.toDouble()
        val baseFloor = totalCents / count
        val remainderPennies = (totalCents % count).toInt()

        return orderedMembers.mapIndexed { idx, (id, name) ->
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

    data class MemberPaymentLeg(
        val counterpartyMemberId: String,
        val counterpartyName: String,
        val amountCents: Long,
        val formattedAmount: String
    )

    data class MemberSettlementSummary(
        val memberId: String,
        val memberName: String,
        val isCurrentUser: Boolean = false,
        val avatarSeed: String = memberName,
        val outgoingPayments: List<MemberPaymentLeg>,
        val incomingPayments: List<MemberPaymentLeg>,
        val totalOutgoingCents: Long = outgoingPayments.sumOf { it.amountCents },
        val totalIncomingCents: Long = incomingPayments.sumOf { it.amountCents },
        val formattedTotalOutgoing: String,
        val formattedTotalIncoming: String
    ) {
        val hasOutgoing: Boolean get() = totalOutgoingCents > 0L
        val hasIncoming: Boolean get() = totalIncomingCents > 0L
        val hasMultipleOutgoing: Boolean get() = outgoingPayments.size > 1
        val hasMultipleIncoming: Boolean get() = incomingPayments.size > 1
        val hasBothDirections: Boolean get() = hasOutgoing && hasIncoming
    }

    fun formatCurrencyCents(cents: Long, currencySymbol: String = "₹"): String {
        return com.splitmate.app.ui.formatIndianRupeesFromCents(
            cents = cents,
            includePlusSign = false,
            currencySymbol = currencySymbol
        )
    }

    /**
     * Dynamically aggregates unsettled simplified transfers by member without modifying any
     * underlying debt edges or settlement amounts.
     *
     * - `totalOutgoingCents` = sum of unsettled payments where this member is the payer (`fromMemberId`).
     * - `totalIncomingCents` = sum of unsettled payments where this member is the receiver (`toMemberId`).
     * - Keeps `incomingPayments` (`RECEIVES +₹X`) and `outgoingPayments` (`PAYS −₹Y`) strictly separate.
     */
    fun computeMemberSettlementSummaries(
        transfers: List<SimplifiedTransfer>,
        memberMetadata: Map<String, Triple<String, String, Boolean>> = emptyMap(),
        currencySymbol: String = "₹"
    ): List<MemberSettlementSummary> {
        if (transfers.isEmpty()) return emptyList()

        fun resolveName(memberId: String, fallbackName: String): String {
            val metaName = memberMetadata[memberId]?.first?.trim().orEmpty()
            return if (metaName.isNotBlank() && !metaName.equals("You", ignoreCase = true)) {
                metaName
            } else {
                fallbackName
            }
        }

        val participantIds = linkedSetOf<String>()
        transfers.forEach { tr ->
            participantIds.add(tr.fromMemberId)
            participantIds.add(tr.toMemberId)
        }

        val summaries = participantIds.mapNotNull { memberId ->
            val outgoing = transfers
                .filter { it.fromMemberId == memberId && it.amountCents > 0L }
                .sortedWith(compareByDescending<SimplifiedTransfer> { it.amountCents }.thenBy { it.toName })
                .map { tr ->
                    MemberPaymentLeg(
                        counterpartyMemberId = tr.toMemberId,
                        counterpartyName = resolveName(tr.toMemberId, tr.toName),
                        amountCents = tr.amountCents,
                        formattedAmount = formatCurrencyCents(tr.amountCents, currencySymbol)
                    )
                }

            val incoming = transfers
                .filter { it.toMemberId == memberId && it.amountCents > 0L }
                .map { tr ->
                    MemberPaymentLeg(
                        counterpartyMemberId = tr.fromMemberId,
                        counterpartyName = resolveName(tr.fromMemberId, tr.fromName),
                        amountCents = tr.amountCents,
                        formattedAmount = formatCurrencyCents(tr.amountCents, currencySymbol)
                    )
                }
                .sortedWith(compareBy<MemberPaymentLeg> { it.counterpartyName }.thenByDescending { it.amountCents })

            val totalOut = outgoing.sumOf { it.amountCents }
            val totalIn = incoming.sumOf { it.amountCents }
            if (totalOut <= 0L && totalIn <= 0L) {
                null
            } else {
                val fallbackName = transfers.firstOrNull { it.fromMemberId == memberId }?.fromName
                    ?: transfers.firstOrNull { it.toMemberId == memberId }?.toName
                    ?: memberId
                val resolvedMemberName = resolveName(memberId, fallbackName)
                val meta = memberMetadata[memberId]
                MemberSettlementSummary(
                    memberId = memberId,
                    memberName = resolvedMemberName,
                    isCurrentUser = meta?.third == true,
                    avatarSeed = meta?.second?.takeIf { it.isNotBlank() } ?: resolvedMemberName,
                    outgoingPayments = outgoing,
                    incomingPayments = incoming,
                    totalOutgoingCents = totalOut,
                    totalIncomingCents = totalIn,
                    formattedTotalOutgoing = formatCurrencyCents(totalOut, currencySymbol),
                    formattedTotalIncoming = formatCurrencyCents(totalIn, currencySymbol)
                )
            }
        }

        return summaries.sortedWith(
            compareByDescending<MemberSettlementSummary> { it.isCurrentUser }
                .thenByDescending { it.hasMultipleOutgoing }
                .thenByDescending { it.hasIncoming && !it.hasOutgoing }
                .thenByDescending { maxOf(it.totalOutgoingCents, it.totalIncomingCents) }
                .thenBy { it.memberName }
        )
    }
}
