package com.splitmate.app.ui.share

import com.splitmate.app.SplitMateMathEngine
import com.splitmate.app.ui.formatIndianRupeesFromCents
import java.text.BreakIterator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * v2.3.5 (#2): pure, JVM-testable model for the "Share settle-up as image" card.
 *
 * Everything the bitmap shows is pre-formatted here from integer cents (paise), so the Canvas
 * renderer ([SettleUpImageRenderer]) only measures and draws. Amounts come straight from the
 * same [SplitMateMathEngine.simplifyDebtsGreedy] output the screen uses; nothing is recomputed
 * with floating point.
 *
 * Privacy (user decision): no UPI IDs, no phone numbers. Names that look like a UPI handle or a
 * phone number are sanitised before they reach the image.
 */
data class SettleUpShareMember(
    val memberId: String,
    val name: String,
    val isCurrentUser: Boolean = false
)

data class SettleUpShareTransferRow(
    val fromName: String,
    val toName: String,
    val amountCents: Long,
    val amountText: String,
    /** Avatar initials, grapheme-safe (never half a surrogate pair). */
    val fromInitials: String = SettleUpShareModel.initialsFor(fromName),
    val toInitials: String = SettleUpShareModel.initialsFor(toName)
)

enum class SettleUpNetDirection { GETS_BACK, PAYS, SETTLED }

data class SettleUpShareNetRow(
    val name: String,
    val netCents: Long,
    val direction: SettleUpNetDirection,
    val netText: String,
    val initials: String = SettleUpShareModel.initialsFor(name)
)

data class SettleUpShareModel(
    val brandName: String,
    val headline: String,
    val tripName: String,
    val metaLine: String,
    val tripEndedBadge: String?,
    val asOfLabel: String?,
    val totalSpentLabel: String,
    val totalSpentText: String,
    val transfersTitle: String,
    val transferRows: List<SettleUpShareTransferRow>,
    val hiddenTransferCount: Int,
    val isAllSettled: Boolean,
    val allSettledTitle: String,
    val allSettledSubtitle: String,
    val netSummaryTitle: String,
    val netRows: List<SettleUpShareNetRow>,
    val hiddenNetCount: Int,
    val footerText: String,
    val footerSubText: String,
    /** Small caps line under the brand name in the header. */
    val brandSubtitle: String = "SETTLEMENT SUMMARY",
    /** Exact-math sub-badge inside the hero card (dropped by the renderer if it would collide). */
    val heroDriftText: String = "0.00\u00A2 drift \u00B7 Exact math",
    /** Directional pill between payer and receiver in each transfer row. */
    val paysLabel: String = "PAYS \u2192"
) {
    val moreTransfersText: String?
        get() = if (hiddenTransferCount > 0) {
            "+$hiddenTransferCount more ${if (hiddenTransferCount == 1) "payment" else "payments"}"
        } else null

    val moreNetText: String?
        get() = if (hiddenNetCount > 0) "+$hiddenNetCount more" else null

    /** Upper-cased label drawn above the hero amount. */
    val totalSpentHeroLabel: String
        get() = totalSpentLabel.uppercase(Locale.ROOT)

    /** Status pill in the hero card: "ALL SETTLED" or "N PAYMENTS". */
    val heroBadgeText: String
        get() {
            val total = transferRows.size + hiddenTransferCount
            return if (isAllSettled) "ALL SETTLED" else "$total ${if (total == 1) "PAYMENT" else "PAYMENTS"}"
        }

    /** True when the net-summary section (rows and/or its "+N more" row) is drawn. */
    val hasNetSection: Boolean
        get() = netRows.isNotEmpty() || hiddenNetCount > 0

    /** Every string that can end up on the bitmap (used by tests to assert privacy rules). */
    fun allVisibleText(): List<String> = buildList {
        add(brandName); add(brandSubtitle); add(headline); add(tripName); add(metaLine)
        tripEndedBadge?.let { add(it) }
        asOfLabel?.let { add(it) }
        add(totalSpentLabel); add(totalSpentHeroLabel); add(totalSpentText)
        add(heroBadgeText); add(heroDriftText)
        add(transfersTitle)
        if (transferRows.isNotEmpty()) add(paysLabel)
        transferRows.forEach {
            add(it.fromName); add(it.fromInitials); add(it.toName); add(it.toInitials); add(it.amountText)
        }
        moreTransfersText?.let { add(it) }
        if (isAllSettled) { add(allSettledTitle); add(allSettledSubtitle) }
        if (hasNetSection) add(netSummaryTitle)
        netRows.forEach { add(it.name); add(it.initials); add(it.netText) }
        moreNetText?.let { add(it) }
        add(footerText); add(footerSubText)
    }

    companion object {
        const val BRAND = "SplitMate"
        const val MAX_TRANSFER_ROWS = 40
        const val MAX_NET_ROWS = 40
        const val MAX_NAME_CODE_POINTS = 24
        const val MAX_TRIP_NAME_CODE_POINTS = 48
        private const val FALLBACK_MEMBER_NAME = "Member"

        fun build(
            tripName: String,
            transfers: List<SplitMateMathEngine.SimplifiedTransfer>,
            members: List<SettleUpShareMember>,
            totalSpentCents: Long,
            currencySymbol: String = "₹",
            currentUserName: String? = null,
            dateRangeLabel: String? = null,
            tripEnded: Boolean = false,
            asOfLabel: String? = null,
            includeNetSummary: Boolean = true
        ): SettleUpShareModel {
            val membersById = members.associateBy { it.memberId }

            fun resolveName(memberId: String, fallback: String): String {
                val member = membersById[memberId]
                val raw = member?.name?.takeIf { it.isNotBlank() } ?: fallback
                val isMe = member?.isCurrentUser == true || raw.trim().equals("You", ignoreCase = true)
                val chosen = if (isMe && !currentUserName.isNullOrBlank()) currentUserName else raw
                return ellipsizeCodePoints(sanitizeDisplayName(chosen), MAX_NAME_CODE_POINTS)
            }

            val positive = transfers.filter { it.amountCents > 0L }
            val orderedRows = positive
                .map { tr ->
                    SettleUpShareTransferRow(
                        fromName = resolveName(tr.fromMemberId, tr.fromName),
                        toName = resolveName(tr.toMemberId, tr.toName),
                        amountCents = tr.amountCents,
                        amountText = formatMoney(tr.amountCents, currencySymbol)
                    )
                }
                .sortedWith(
                    compareByDescending<SettleUpShareTransferRow> { it.amountCents }
                        .thenBy { it.fromName.lowercase(Locale.ROOT) }
                        .thenBy { it.toName.lowercase(Locale.ROOT) }
                )
            val visibleRows = orderedRows.take(MAX_TRANSFER_ROWS)
            val isAllSettled = orderedRows.isEmpty()

            // Net per person derived from the transfers themselves (incoming - outgoing). Greedy
            // simplification settles every balance exactly, so this equals each member's net.
            val netById = linkedMapOf<String, Long>()
            members.forEach { netById[it.memberId] = 0L }
            positive.forEach { tr ->
                netById[tr.toMemberId] = (netById[tr.toMemberId] ?: 0L) + tr.amountCents
                netById[tr.fromMemberId] = (netById[tr.fromMemberId] ?: 0L) - tr.amountCents
            }
            val fallbackNames = HashMap<String, String>()
            positive.forEach { tr ->
                fallbackNames.putIfAbsent(tr.fromMemberId, tr.fromName)
                fallbackNames.putIfAbsent(tr.toMemberId, tr.toName)
            }
            val orderedNet = if (!includeNetSummary || isAllSettled) {
                emptyList()
            } else {
                netById.map { (id, net) ->
                    val direction = when {
                        net > 0L -> SettleUpNetDirection.GETS_BACK
                        net < 0L -> SettleUpNetDirection.PAYS
                        else -> SettleUpNetDirection.SETTLED
                    }
                    val abs = if (net < 0L) -net else net
                    SettleUpShareNetRow(
                        name = resolveName(id, fallbackNames[id] ?: FALLBACK_MEMBER_NAME),
                        netCents = net,
                        direction = direction,
                        netText = when (direction) {
                            SettleUpNetDirection.GETS_BACK -> "gets back ${formatMoney(abs, currencySymbol)}"
                            SettleUpNetDirection.PAYS -> "pays ${formatMoney(abs, currencySymbol)}"
                            SettleUpNetDirection.SETTLED -> "settled"
                        }
                    )
                }.sortedWith(
                    compareByDescending<SettleUpShareNetRow> { it.netCents }
                        .thenBy { it.name.lowercase(Locale.ROOT) }
                )
            }
            // Keep the bitmap under SettleUpShareLayout.MAX_HEIGHT: whatever height the (already
            // capped) transfer card leaves is given to net rows, with a "+N more" row for the rest.
            val netCap = SettleUpShareLayout.maxNetRows(
                totalNetRows = orderedNet.size,
                transferRowCount = visibleRows.size,
                hasMoreTransfers = orderedRows.size > visibleRows.size,
                isAllSettled = isAllSettled
            )
            val visibleNet = orderedNet.take(netCap)

            val travellerCount = members.size
            val metaParts = buildList {
                dateRangeLabel?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
                if (travellerCount > 0) {
                    add("$travellerCount ${if (travellerCount == 1) "traveller" else "travellers"}")
                }
            }
            val cleanTrip = tripName.trim().replace(WHITESPACE, " ").ifEmpty { "Our trip" }

            return SettleUpShareModel(
                brandName = BRAND,
                headline = "Settle up",
                tripName = ellipsizeCodePoints(cleanTrip, MAX_TRIP_NAME_CODE_POINTS),
                metaLine = metaParts.joinToString(" · "),
                tripEndedBadge = if (tripEnded) "Trip ended" else null,
                asOfLabel = asOfLabel?.trim()?.takeIf { it.isNotEmpty() },
                totalSpentLabel = "Total spent",
                totalSpentText = formatMoney(totalSpentCents.coerceAtLeast(0L), currencySymbol),
                transfersTitle = if (isAllSettled) {
                    "Who pays whom"
                } else {
                    "Who pays whom · ${orderedRows.size} ${if (orderedRows.size == 1) "payment" else "payments"}"
                },
                transferRows = visibleRows,
                hiddenTransferCount = orderedRows.size - visibleRows.size,
                isAllSettled = isAllSettled,
                allSettledTitle = "Everyone is settled \uD83C\uDF89",
                allSettledSubtitle = "No payments needed. Nobody owes anything.",
                netSummaryTitle = "Where everyone stands",
                netRows = visibleNet,
                hiddenNetCount = orderedNet.size - visibleNet.size,
                footerText = "Made with SplitMate",
                footerSubText = "Exact to the last paisa · 0.00¢ drift"
            )
        }

        /**
         * Avatar initials that never split a surrogate pair or grapheme cluster:
         *  - "Rohan Mehta" -> "RM", "priya" -> "P", Devanagari / CJK -> first grapheme of each word;
         *  - emoji-only / symbol-only names -> their first whole grapheme (e.g. one emoji);
         *  - blank -> "?".
         */
        fun initialsFor(name: String): String {
            val clean = name.trim()
            if (clean.isEmpty()) return "?"
            val words = clean.split(WHITESPACE).filter { it.isNotEmpty() }
            fun letterGrapheme(word: String): String? {
                var i = 0
                while (i < word.length) {
                    val cp = word.codePointAt(i)
                    if (Character.isLetterOrDigit(cp)) return firstGrapheme(word, i)
                    i += Character.charCount(cp)
                }
                return null
            }
            val first = letterGrapheme(words.first())
            val second = if (words.size > 1) letterGrapheme(words.last()) else null
            val raw = when {
                first != null && second != null -> first + second
                first != null -> first
                second != null -> second
                else -> firstGrapheme(clean, 0)
            }
            return raw.uppercase(Locale.ROOT)
        }

        /** The grapheme cluster starting at [start]; always at least one whole code point. */
        internal fun firstGrapheme(text: String, start: Int): String {
            if (start >= text.length) return ""
            val minEnd = start + Character.charCount(text.codePointAt(start))
            val end = runCatching {
                val it = BreakIterator.getCharacterInstance(Locale.ROOT)
                it.setText(text)
                it.following(start)
            }.getOrNull()
            val safeEnd = if (end == null || end == BreakIterator.DONE || end < minEnd) minEnd else end
            return text.substring(start, minOf(safeEnd, text.length))
        }

        private val WHITESPACE = Regex("\\s+")
        private val UPI_HANDLE = Regex("[A-Za-z0-9._\\-]+@[A-Za-z0-9._\\-]+")
        private val PHONE_LIKE = Regex("^[+]?[0-9][0-9 \\-]{8,}$")

        /** Integer-cent formatting only (never Double math). */
        fun formatMoney(cents: Long, currencySymbol: String = "₹"): String =
            formatIndianRupeesFromCents(cents, includePlusSign = false, currencySymbol = currencySymbol)

        /**
         * Removes anything that could leak a UPI ID or phone number: `name@bank` handles are cut to
         * the part before `@`, and phone-number-only names become "Member".
         */
        fun sanitizeDisplayName(raw: String): String {
            var s = raw.trim().replace(WHITESPACE, " ")
            s = UPI_HANDLE.replace(s) { m -> m.value.substringBefore('@') }
            s = s.replace("@", "").trim()
            if (s.isEmpty() || PHONE_LIKE.matches(s)) return FALLBACK_MEMBER_NAME
            return s
        }

        /** Code-point aware ellipsis so emoji / Devanagari surrogate pairs are never split. */
        fun ellipsizeCodePoints(text: String, maxCodePoints: Int): String {
            val count = text.codePointCount(0, text.length)
            if (count <= maxCodePoints || maxCodePoints < 2) return text
            val end = text.offsetByCodePoints(0, maxCodePoints - 1)
            return text.substring(0, end).trimEnd() + "…"
        }

        /** "12–18 Mar 2026", "28 Mar – 2 Apr 2026" or "30 Dec 2025 – 3 Jan 2026". */
        fun formatDateRange(startMs: Long, endMs: Long, zone: ZoneId = ZoneId.systemDefault()): String {
            val lo = minOf(startMs, endMs)
            val hi = maxOf(startMs, endMs)
            val a = Instant.ofEpochMilli(lo).atZone(zone).toLocalDate()
            val b = Instant.ofEpochMilli(hi).atZone(zone).toLocalDate()
            val full = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
            return when {
                a == b -> a.format(full)
                a.year == b.year && a.month == b.month -> "${a.dayOfMonth}–${b.format(full)}"
                a.year == b.year -> "${a.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))} – ${b.format(full)}"
                else -> "${a.format(full)} – ${b.format(full)}"
            }
        }

        fun formatAsOf(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
            "As of " + Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
                .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    }
}

/**
 * Pure vertical layout for the 1080px share card. Kept free of android.graphics so the height
 * growth for many rows can be unit tested; [SettleUpImageRenderer] just draws at these offsets.
 */
data class SettleUpShareLayout(
    val width: Int,
    val padding: Int,
    val brandTop: Int,
    val tripTop: Int,
    val metaTop: Int,
    val totalCardTop: Int,
    val transfersTitleTop: Int,
    val transfersCardTop: Int,
    val transfersCardHeight: Int,
    val netTitleTop: Int?,
    val netCardTop: Int?,
    val netCardHeight: Int,
    val footerTop: Int,
    val height: Int
) {
    companion object {
        const val WIDTH = 1080
        const val PADDING = 64
        const val BRAND_H = 84
        const val TRIP_H = 84
        const val META_H = 56
        const val TOTAL_CARD_H = 212
        const val SECTION_TITLE_H = 64
        const val CARD_V_PAD = 20
        const val TRANSFER_ROW_H = 136
        const val MORE_ROW_H = 76
        const val SETTLED_CARD_H = 248
        const val NET_ROW_H = 96
        const val FOOTER_H = 120
        const val GAP_S = 16
        const val GAP_M = 32
        const val GAP_L = 44

        /**
         * Hard ceiling for the bitmap height (well below the 10,000px many chat apps and
         * decoders choke on; ~41 MB ARGB at 1080px wide).
         */
        const val MAX_HEIGHT = 9_600

        private fun transfersCardHeight(rowCount: Int, hasMore: Boolean, isAllSettled: Boolean): Int =
            if (isAllSettled) SETTLED_CARD_H
            else CARD_V_PAD * 2 + rowCount * TRANSFER_ROW_H + (if (hasMore) MORE_ROW_H else 0)

        /**
         * How many net-summary rows fit under [MAX_HEIGHT] once the transfer card is placed,
         * assuming the worst-case header (meta line present). Returns at most
         * [SettleUpShareModel.MAX_NET_ROWS]; if not every row fits, room is kept for "+N more".
         */
        fun maxNetRows(
            totalNetRows: Int,
            transferRowCount: Int,
            hasMoreTransfers: Boolean,
            isAllSettled: Boolean
        ): Int {
            if (totalNetRows <= 0) return 0
            val fixed = PADDING + BRAND_H + GAP_L + TRIP_H + META_H + GAP_M +
                TOTAL_CARD_H + GAP_L + SECTION_TITLE_H + GAP_S +
                transfersCardHeight(transferRowCount, hasMoreTransfers, isAllSettled) +
                GAP_L + FOOTER_H + PADDING
            val netOverhead = GAP_L + SECTION_TITLE_H + GAP_S + CARD_V_PAD * 2
            val available = MAX_HEIGHT - fixed - netOverhead
            if (totalNetRows <= SettleUpShareModel.MAX_NET_ROWS && totalNetRows * NET_ROW_H <= available) {
                return totalNetRows
            }
            val withMoreRow = (available - MORE_ROW_H) / NET_ROW_H
            return withMoreRow.coerceIn(0, minOf(totalNetRows, SettleUpShareModel.MAX_NET_ROWS))
        }

        fun of(model: SettleUpShareModel): SettleUpShareLayout {
            var y = PADDING
            val brandTop = y
            y += BRAND_H + GAP_L
            val tripTop = y
            y += TRIP_H
            val metaTop = y
            val hasMeta = model.metaLine.isNotEmpty() || model.tripEndedBadge != null
            if (hasMeta) y += META_H
            y += GAP_M
            val totalTop = y
            y += TOTAL_CARD_H + GAP_L
            val transfersTitleTop = y
            y += SECTION_TITLE_H + GAP_S
            val transfersCardTop = y
            val transfersCardH = transfersCardHeight(
                rowCount = model.transferRows.size,
                hasMore = model.moreTransfersText != null,
                isAllSettled = model.isAllSettled
            )
            y += transfersCardH
            var netTitleTop: Int? = null
            var netCardTop: Int? = null
            var netCardH = 0
            if (model.hasNetSection) {
                y += GAP_L
                netTitleTop = y
                y += SECTION_TITLE_H + GAP_S
                netCardTop = y
                netCardH = CARD_V_PAD * 2 + model.netRows.size * NET_ROW_H +
                    (if (model.moreNetText != null) MORE_ROW_H else 0)
                y += netCardH
            }
            y += GAP_L
            val footerTop = y
            y += FOOTER_H + PADDING
            return SettleUpShareLayout(
                width = WIDTH,
                padding = PADDING,
                brandTop = brandTop,
                tripTop = tripTop,
                metaTop = metaTop,
                totalCardTop = totalTop,
                transfersTitleTop = transfersTitleTop,
                transfersCardTop = transfersCardTop,
                transfersCardHeight = transfersCardH,
                netTitleTop = netTitleTop,
                netCardTop = netCardTop,
                netCardHeight = netCardH,
                footerTop = footerTop,
                height = y
            )
        }
    }
}
