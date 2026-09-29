package com.splitmate.app

/**
 * v2.3.3 Split Nature Classifier (100% pure Kotlin, UI-copy only).
 *
 * Derives how an expense is shared purely from existing persisted data — the payer id and the
 * participants that carry a non-zero owed share (in integer cents). It NEVER alters splits,
 * balances, the remainder engine or debt simplification; it only drives the wording shown in the UI.
 *
 * - [PERSONAL]  : exactly one participant and that participant IS the payer (solo ticket / own purchase).
 *                 Nets to zero for everyone else, so there is nothing to split.
 * - [ON_BEHALF] : exactly one participant who is NOT the payer (paid fully for someone else).
 * - [SHARED]    : two or more participants (or no resolvable participants) — existing behaviour.
 */
enum class ExpenseSplitNature { PERSONAL, ON_BEHALF, SHARED }

object ExpenseSplitClassifier {

    /** Participants that actually carry a share (0-cent / excluded entries are ignored). */
    fun activeParticipantIds(participantShareCents: Map<String, Long>): Set<String> =
        participantShareCents.filterValues { it > 0L }.keys

    fun classify(payerId: String?, participantShareCents: Map<String, Long>): ExpenseSplitNature {
        val sole = activeParticipantIds(participantShareCents).singleOrNull()
            ?: return ExpenseSplitNature.SHARED
        return if (!payerId.isNullOrBlank() && sole == payerId) {
            ExpenseSplitNature.PERSONAL
        } else {
            ExpenseSplitNature.ON_BEHALF
        }
    }

    /** The single participant for PERSONAL / ON_BEHALF expenses, otherwise `null`. */
    fun soleParticipantId(participantShareCents: Map<String, Long>): String? =
        activeParticipantIds(participantShareCents).singleOrNull()

    /**
     * Review-screen variant (before any split rows exist): every selected member gets a non-zero
     * equal share of a positive total, so the selection alone decides the nature.
     */
    fun classifySelection(payerId: String?, selectedMemberIds: Collection<String>): ExpenseSplitNature =
        classify(payerId, selectedMemberIds.associateWith { 1L })
}

/**
 * Human copy for [ExpenseSplitNature]. SHARED always returns the caller-provided legacy text so
 * shared expenses read exactly as before.
 */
object ExpenseSplitCopy {
    const val PERSONAL_EXPENSE = "Personal expense"
    const val PERSONAL_NOT_SPLIT = "Personal · not split"
    const val PERSONAL_EXPENSE_NOT_SPLIT = "Personal expense · not split"

    /** "you" for the current user, otherwise the first name (falls back to the full name). */
    fun beneficiaryLabel(name: String?, isCurrentUser: Boolean): String {
        if (isCurrentUser) return "you"
        val clean = name.orEmpty().substringBefore(" (").trim()
        return clean.substringBefore(" ").ifBlank { clean.ifBlank { "member" } }
    }

    private fun owesPhrase(beneficiary: String, isCurrentUser: Boolean, share: String): String =
        if (isCurrentUser) "you owe $share" else "$beneficiary owes $share"

    /**
     * Full "Paid by …" subtitle (activity sheet / group expense rows).
     * PERSONAL  -> "Paid by Priyanka · Personal expense"
     * ON_BEHALF -> "Paid by Priyanka for Akshay · Akshay owes ₹Z"
     * SHARED    -> "Paid by Priyanka · <sharedDetail>"
     */
    fun paidBySubtitle(
        nature: ExpenseSplitNature,
        payerName: String,
        beneficiaryName: String?,
        beneficiaryIsCurrentUser: Boolean,
        beneficiaryShare: String,
        sharedDetail: String
    ): String = when (nature) {
        ExpenseSplitNature.PERSONAL -> "Paid by $payerName · $PERSONAL_EXPENSE"
        ExpenseSplitNature.ON_BEHALF -> {
            val who = beneficiaryLabel(beneficiaryName, beneficiaryIsCurrentUser)
            "Paid by $payerName for $who · ${owesPhrase(who, beneficiaryIsCurrentUser, beneficiaryShare)}"
        }
        ExpenseSplitNature.SHARED -> "Paid by $payerName · $sharedDetail"
    }

    /**
     * Compact history-row subtitle.
     * PERSONAL -> "Paid by Priyanka · Personal · not split"; ON_BEHALF as [paidBySubtitle].
     */
    fun historyRowSubtitle(
        nature: ExpenseSplitNature,
        payerName: String,
        beneficiaryName: String?,
        beneficiaryIsCurrentUser: Boolean,
        beneficiaryShare: String,
        sharedDetail: String
    ): String = when (nature) {
        ExpenseSplitNature.PERSONAL -> "Paid by $payerName · $PERSONAL_NOT_SPLIT"
        else -> paidBySubtitle(nature, payerName, beneficiaryName, beneficiaryIsCurrentUser, beneficiaryShare, sharedDetail)
    }

    /**
     * Short detail used where a per-person figure used to sit (booking cards, review chips):
     * PERSONAL -> "Personal · not split"; ON_BEHALF -> "Paid for Akshay"; SHARED -> [sharedDetail].
     */
    fun compactDetail(
        nature: ExpenseSplitNature,
        beneficiaryName: String?,
        beneficiaryIsCurrentUser: Boolean,
        sharedDetail: String
    ): String = when (nature) {
        ExpenseSplitNature.PERSONAL -> PERSONAL_NOT_SPLIT
        ExpenseSplitNature.ON_BEHALF -> "Paid for ${beneficiaryLabel(beneficiaryName, beneficiaryIsCurrentUser)}"
        ExpenseSplitNature.SHARED -> sharedDetail
    }

    /**
     * Caption under a total where "₹x / traveler" used to sit:
     * PERSONAL -> "Personal · not split"; ON_BEHALF -> "Akshay owes ₹Z" (or "you owe ₹Z");
     * SHARED -> [sharedDetail].
     */
    fun perPersonCaption(
        nature: ExpenseSplitNature,
        beneficiaryName: String?,
        beneficiaryIsCurrentUser: Boolean,
        beneficiaryShare: String,
        sharedDetail: String
    ): String = when (nature) {
        ExpenseSplitNature.PERSONAL -> PERSONAL_NOT_SPLIT
        ExpenseSplitNature.ON_BEHALF -> {
            val who = beneficiaryLabel(beneficiaryName, beneficiaryIsCurrentUser)
            owesPhrase(who, beneficiaryIsCurrentUser, beneficiaryShare).replaceFirstChar { it.uppercase() }
        }
        ExpenseSplitNature.SHARED -> sharedDetail
    }

    /**
     * Split-mode label where "Equal split · N members" / "N-way split" used to sit (paired with
     * [perPersonCaption], so PERSONAL avoids repeating "not split"):
     * PERSONAL -> "Personal expense"; ON_BEHALF -> "Paid for Akshay"; SHARED -> [sharedDetail].
     */
    fun splitModeLabel(
        nature: ExpenseSplitNature,
        beneficiaryName: String?,
        beneficiaryIsCurrentUser: Boolean,
        sharedDetail: String
    ): String = when (nature) {
        ExpenseSplitNature.PERSONAL -> PERSONAL_EXPENSE
        ExpenseSplitNature.ON_BEHALF -> "Paid for ${beneficiaryLabel(beneficiaryName, beneficiaryIsCurrentUser)}"
        ExpenseSplitNature.SHARED -> sharedDetail
    }
}
