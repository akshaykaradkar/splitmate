package com.splitmate.app.ui.a11y

/**
 * v2.4.0 D2: screen-reader friendly money.
 *
 * TalkBack reads "+₹14.17" as "plus rupee fourteen point one seven". These helpers turn integer
 * paise into words a person would say ("14 rupees 17 paise") and build one sentence per
 * settlement row ("Sam pays you 14 rupees 17 paise"). Pure Kotlin, no money math beyond
 * splitting paise into rupees and paise.
 */
object SpokenMoney {

    fun rupees(amountCents: Long): String {
        val abs = kotlin.math.abs(amountCents)
        val rupees = abs / 100
        val paise = abs % 100
        val rupeePart = when (rupees) {
            0L -> null
            1L -> "1 rupee"
            else -> "$rupees rupees"
        }
        val paisePart = when (paise) {
            0L -> null
            1L -> "1 paisa"
            else -> "$paise paise"
        }
        return listOfNotNull(rupeePart, paisePart).joinToString(" ").ifEmpty { "0 rupees" }
    }

    /** One sentence for a settlement row, from the current user's point of view when involved. */
    fun settlementSentence(
        fromName: String,
        toName: String,
        amountCents: Long,
        currentUserIsPayer: Boolean,
        currentUserIsReceiver: Boolean
    ): String {
        val amount = rupees(amountCents)
        return when {
            currentUserIsPayer -> "You pay $toName $amount"
            currentUserIsReceiver -> "$fromName pays you $amount"
            else -> "$fromName pays $toName $amount"
        }
    }
}
