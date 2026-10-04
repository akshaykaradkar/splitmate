package com.splitmate.app.ui.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils

/**
 * v2.3.5 (#2): draws the settle-up share card with plain android.graphics (Compose 1.6 has no
 * GraphicsLayer capture). Always uses the Buckwheat light palette, even in dark mode, so the image
 * looks the same in every chat. All text/ordering/heights come from [SettleUpShareModel] and
 * [SettleUpShareLayout]; this class only measures widths (for ellipsis) and draws.
 *
 * Fonts: system sans-serif typefaces, so the platform fallback chain renders ₹, Devanagari and
 * emoji. Must be called off the main thread (Dispatchers.Default).
 */
object SettleUpImageRenderer {

    private val CANVAS = Color.parseColor("#FAF6F0")
    private val CARD = Color.parseColor("#FFFFFF")
    private val CARD_BORDER = Color.parseColor("#EDE7DF")
    private val SUNKEN = Color.parseColor("#F4EFE6")
    private val OLIVE = Color.parseColor("#416913")
    private val OLIVE_DEEP = Color.parseColor("#365314")
    private val SAGE = Color.parseColor("#DCE9B9")
    private val TERRACOTTA = Color.parseColor("#E06B52")
    private val PEACH = Color.parseColor("#FCE3D7")
    private val TERRACOTTA_TEXT = Color.parseColor("#7C2D12")
    private val CHARCOAL = Color.parseColor("#23201E")
    private val MUTED = Color.parseColor("#6B625A")

    private val BOLD: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    private val REGULAR: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

    fun render(model: SettleUpShareModel): Bitmap {
        val layout = SettleUpShareLayout.of(model)
        val bitmap = Bitmap.createBitmap(layout.width, layout.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(CANVAS)

        val left = layout.padding.toFloat()
        val right = (layout.width - layout.padding).toFloat()
        val contentW = right - left

        drawBrand(canvas, model, layout, left, right)
        drawTrip(canvas, model, layout, left, right, contentW)
        drawTotal(canvas, model, layout, left, right)
        drawSectionTitle(canvas, model.transfersTitle, left, layout.transfersTitleTop, contentW)
        drawCard(canvas, left, layout.transfersCardTop.toFloat(), right, (layout.transfersCardTop + layout.transfersCardHeight).toFloat())
        if (model.isAllSettled) {
            drawAllSettled(canvas, model, layout, left, right, contentW)
        } else {
            drawTransfers(canvas, model, layout, left, right)
        }
        val netTitleTop = layout.netTitleTop
        val netCardTop = layout.netCardTop
        if (netTitleTop != null && netCardTop != null) {
            drawSectionTitle(canvas, model.netSummaryTitle, left, netTitleTop, contentW)
            drawCard(canvas, left, netCardTop.toFloat(), right, (netCardTop + layout.netCardHeight).toFloat())
            drawNetRows(canvas, model, netCardTop, left, right)
        }
        drawFooter(canvas, model, layout, left, right, contentW)
        return bitmap
    }

    private fun paint(color: Int, sizePx: Float, bold: Boolean): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.color = color
            textSize = sizePx
            typeface = if (bold) BOLD else REGULAR
        }

    private fun fit(text: String, p: TextPaint, maxW: Float): String =
        if (maxW <= 0f) "" else TextUtils.ellipsize(text, p, maxW, TextUtils.TruncateAt.END).toString()

    /** Draws [text] vertically centred in a box starting at [top] with height [h]. */
    private fun drawCentered(canvas: Canvas, text: String, x: Float, top: Float, h: Float, p: TextPaint) {
        val fm = p.fontMetrics
        val baseline = top + h / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, x, baseline, p)
    }

    private fun drawCard(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val rect = RectF(l, t, r, b)
        canvas.drawRoundRect(rect, 36f, 36f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD })
        canvas.drawRoundRect(
            RectF(l + 1f, t + 1f, r - 1f, b - 1f), 36f, 36f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD_BORDER; style = Paint.Style.STROKE; strokeWidth = 2f }
        )
    }

    private fun drawPill(canvas: Canvas, text: String, x: Float, top: Float, h: Float, bg: Int, fg: Int, sizePx: Float, maxW: Float, alignRight: Boolean = false): Float {
        val p = paint(fg, sizePx, bold = true)
        val padH = 24f
        val label = fit(text, p, maxW - padH * 2)
        val w = p.measureText(label) + padH * 2
        val l = if (alignRight) x - w else x
        canvas.drawRoundRect(RectF(l, top, l + w, top + h), h / 2f, h / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg })
        drawCentered(canvas, label, l + padH, top, h, p)
        return w
    }

    private fun drawBrand(canvas: Canvas, model: SettleUpShareModel, layout: SettleUpShareLayout, left: Float, right: Float) {
        val top = layout.brandTop.toFloat()
        val h = SettleUpShareLayout.BRAND_H.toFloat()
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = OLIVE }
        canvas.drawRoundRect(RectF(left, top, left + h, top + h), 22f, 22f, dot)
        val mark = paint(Color.WHITE, 40f, bold = true).apply { textAlign = Paint.Align.CENTER }
        drawCentered(canvas, "S", left + h / 2f, top, h, mark)
        val brand = paint(CHARCOAL, 40f, bold = true)
        drawCentered(canvas, model.brandName, left + h + 20f, top, h, brand)
        val brandEnd = left + h + 20f + brand.measureText(model.brandName) + 32f
        val asOf = model.asOfLabel
        if (asOf != null) {
            drawPill(canvas, asOf, right, top + 10f, h - 20f, SUNKEN, MUTED, 28f, right - brandEnd, alignRight = true)
        }
    }

    private fun drawTrip(canvas: Canvas, model: SettleUpShareModel, layout: SettleUpShareLayout, left: Float, right: Float, contentW: Float) {
        val tripP = paint(CHARCOAL, 60f, bold = true)
        drawCentered(canvas, fit(model.tripName, tripP, contentW), left, layout.tripTop.toFloat(), SettleUpShareLayout.TRIP_H.toFloat(), tripP)
        val metaTop = layout.metaTop.toFloat()
        val metaH = SettleUpShareLayout.META_H.toFloat()
        var x = left
        val badge = model.tripEndedBadge
        if (badge != null) {
            x += drawPill(canvas, badge, x, metaTop + 6f, metaH - 12f, PEACH, TERRACOTTA_TEXT, 28f, contentW / 2f) + 20f
        }
        if (model.metaLine.isNotEmpty()) {
            val metaP = paint(MUTED, 32f, bold = false)
            drawCentered(canvas, fit(model.metaLine, metaP, right - x), x, metaTop, metaH, metaP)
        }
    }

    private fun drawTotal(canvas: Canvas, model: SettleUpShareModel, layout: SettleUpShareLayout, left: Float, right: Float) {
        val top = layout.totalCardTop.toFloat()
        val h = SettleUpShareLayout.TOTAL_CARD_H.toFloat()
        canvas.drawRoundRect(RectF(left, top, right, top + h), 40f, 40f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SAGE })
        val labelP = paint(OLIVE_DEEP, 32f, bold = true)
        drawCentered(canvas, model.totalSpentLabel, left + 40f, top + 28f, 48f, labelP)
        val amountP = paint(OLIVE_DEEP, 76f, bold = true)
        drawCentered(canvas, fit(model.totalSpentText, amountP, right - left - 80f), left + 40f, top + 80f, 92f, amountP)
    }

    private fun drawSectionTitle(canvas: Canvas, text: String, left: Float, top: Int, contentW: Float) {
        val p = paint(CHARCOAL, 38f, bold = true)
        drawCentered(canvas, fit(text, p, contentW), left, top.toFloat(), SettleUpShareLayout.SECTION_TITLE_H.toFloat(), p)
    }

    private fun drawTransfers(canvas: Canvas, model: SettleUpShareModel, layout: SettleUpShareLayout, left: Float, right: Float) {
        val rowH = SettleUpShareLayout.TRANSFER_ROW_H.toFloat()
        val innerL = left + 36f
        val innerR = right - 36f
        val divider = Paint().apply { color = CARD_BORDER; strokeWidth = 2f }
        val nameP = paint(CHARCOAL, 40f, bold = true)
        val arrowP = paint(TERRACOTTA, 40f, bold = true)
        val chipP = paint(TERRACOTTA_TEXT, 40f, bold = true)
        val arrow = "  →  "
        var top = (layout.transfersCardTop + SettleUpShareLayout.CARD_V_PAD).toFloat()
        model.transferRows.forEachIndexed { index, row ->
            if (index > 0) canvas.drawLine(innerL, top, innerR, top, divider)
            // Amount chip (right, peach) is measured first so names get the remaining width.
            val chipPad = 28f
            val chipH = 76f
            val chipW = chipP.measureText(row.amountText) + chipPad * 2
            val chipL = innerR - chipW
            val chipT = top + (rowH - chipH) / 2f
            canvas.drawRoundRect(RectF(chipL, chipT, innerR, chipT + chipH), chipH / 2f, chipH / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PEACH })
            drawCentered(canvas, row.amountText, chipL + chipPad, chipT, chipH, chipP)

            val namesW = chipL - 24f - innerL
            val arrowW = arrowP.measureText(arrow)
            val eachW = ((namesW - arrowW) / 2f).coerceAtLeast(0f)
            val fromFull = nameP.measureText(row.fromName)
            val toFull = nameP.measureText(row.toName)
            // Give unused width of a short name to the longer one.
            val fromW = if (fromFull < eachW) fromFull else (namesW - arrowW - minOf(toFull, eachW))
            val from = fit(row.fromName, nameP, fromW)
            var x = innerL
            drawCentered(canvas, from, x, top, rowH, nameP)
            x += nameP.measureText(from)
            drawCentered(canvas, arrow, x, top, rowH, arrowP)
            x += arrowW
            drawCentered(canvas, fit(row.toName, nameP, chipL - 24f - x), x, top, rowH, nameP)
            top += rowH
        }
        val more = model.moreTransfersText
        if (more != null) {
            canvas.drawLine(innerL, top, innerR, top, divider)
            drawCentered(canvas, more, innerL, top, SettleUpShareLayout.MORE_ROW_H.toFloat(), paint(MUTED, 32f, bold = true))
        }
    }

    private fun drawAllSettled(canvas: Canvas, model: SettleUpShareModel, layout: SettleUpShareLayout, left: Float, right: Float, contentW: Float) {
        val top = layout.transfersCardTop.toFloat()
        val cx = (left + right) / 2f
        val titleP = paint(OLIVE_DEEP, 52f, bold = true).apply { textAlign = Paint.Align.CENTER }
        drawCentered(canvas, fit(model.allSettledTitle, titleP, contentW - 64f), cx, top + 50f, 80f, titleP)
        val subP = paint(MUTED, 32f, bold = false).apply { textAlign = Paint.Align.CENTER }
        drawCentered(canvas, fit(model.allSettledSubtitle, subP, contentW - 64f), cx, top + 136f, 56f, subP)
    }

    private fun drawNetRows(canvas: Canvas, model: SettleUpShareModel, cardTop: Int, left: Float, right: Float) {
        val rowH = SettleUpShareLayout.NET_ROW_H.toFloat()
        val innerL = left + 36f
        val innerR = right - 36f
        val nameP = paint(CHARCOAL, 36f, bold = true)
        val divider = Paint().apply { color = CARD_BORDER; strokeWidth = 2f }
        var top = (cardTop + SettleUpShareLayout.CARD_V_PAD).toFloat()
        model.netRows.forEachIndexed { index, row ->
            if (index > 0) canvas.drawLine(innerL, top, innerR, top, divider)
            val color = when (row.direction) {
                SettleUpNetDirection.GETS_BACK -> OLIVE
                SettleUpNetDirection.PAYS -> TERRACOTTA
                SettleUpNetDirection.SETTLED -> MUTED
            }
            val valueP = paint(color, 36f, bold = true).apply { textAlign = Paint.Align.RIGHT }
            val valueW = valueP.measureText(row.netText)
            drawCentered(canvas, row.netText, innerR, top, rowH, valueP)
            drawCentered(canvas, fit(row.name, nameP, innerR - valueW - 32f - innerL), innerL, top, rowH, nameP)
            top += rowH
        }
        val more = model.moreNetText
        if (more != null) {
            canvas.drawLine(innerL, top, innerR, top, divider)
            drawCentered(canvas, more, innerL, top, SettleUpShareLayout.MORE_ROW_H.toFloat(), paint(MUTED, 32f, bold = true))
        }
    }

    private fun drawFooter(canvas: Canvas, model: SettleUpShareModel, layout: SettleUpShareLayout, left: Float, right: Float, contentW: Float) {
        val top = layout.footerTop.toFloat()
        canvas.drawLine(left, top, right, top, Paint().apply { color = CARD_BORDER; strokeWidth = 2f })
        val cx = (left + right) / 2f
        val main = paint(OLIVE_DEEP, 34f, bold = true).apply { textAlign = Paint.Align.CENTER }
        drawCentered(canvas, fit(model.footerText, main, contentW), cx, top + 16f, 48f, main)
        val sub = paint(MUTED, 26f, bold = false).apply { textAlign = Paint.Align.CENTER }
        drawCentered(canvas, fit(model.footerSubText, sub, contentW), cx, top + 64f, 40f, sub)
    }
}
