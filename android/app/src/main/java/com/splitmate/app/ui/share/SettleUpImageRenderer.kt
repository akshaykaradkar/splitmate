package com.splitmate.app.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.splitmate.app.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * v2.3.5 (#2): renders the settle-up share card with android.graphics using the Stitch
 * "Organic Tactile Financial" (Buckwheat) Material 3 Expressive palette, bundled Figtree
 * typography (when [Context] is supplied), and the vector SplitMate brand mark from
 * `ic_launcher_foreground.xml` + `ic_launcher_background.xml`.
 *
 * Always uses the Buckwheat light palette so the exported PNG reads with high contrast in any
 * chat app. Must be called off the main thread (Dispatchers.Default).
 *
 * Every string drawn here comes from [SettleUpShareModel] (see [SettleUpShareModel.allVisibleText])
 * and every colour from [ShareCardPalette]; this object only measures and draws.
 */
object SettleUpImageRenderer {

    // Canonical Stitch "Organic Tactile Financial" (Buckwheat) palette tokens
    private const val CANVAS = ShareCardPalette.CANVAS
    private const val CANVAS_WARM = ShareCardPalette.CANVAS_WARM
    private const val SUNKEN = ShareCardPalette.SUNKEN
    private const val CARD = ShareCardPalette.CARD
    private const val CARD_BORDER = ShareCardPalette.CARD_BORDER
    private const val OLIVE = ShareCardPalette.OLIVE
    private const val OLIVE_DEEP = ShareCardPalette.OLIVE_DEEP
    private const val SAGE = ShareCardPalette.SAGE
    private const val SAGE_BRIGHT = ShareCardPalette.SAGE_BRIGHT
    private const val TERRACOTTA = ShareCardPalette.TERRACOTTA
    private const val PEACH = ShareCardPalette.PEACH
    private const val PEACH_DEEP = ShareCardPalette.PEACH_DEEP
    private const val TERRACOTTA_TEXT = ShareCardPalette.TERRACOTTA_TEXT
    private const val CHARCOAL = ShareCardPalette.CHARCOAL
    private const val MUTED = ShareCardPalette.MUTED
    private const val LOGO_INNER_RING = ShareCardPalette.LOGO_INNER_RING

    /** Number of scallops on the member avatar (Material `Cookie9Sided`). */
    private const val COOKIE_LOBES = 9
    /** Scallop depth as a fraction of the outer radius. */
    private const val COOKIE_DEPTH = 0.10f
    private const val COOKIE_SAMPLES = COOKIE_LOBES * 16

    private data class FontSet(
        val regular: Typeface,
        val medium: Typeface,
        val semiBold: Typeface,
        val bold: Typeface,
        val extraBold: Typeface
    )

    private fun resolveFonts(context: Context?): FontSet {
        val fallbackRegular = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        val fallbackBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        if (context == null) {
            return FontSet(
                regular = fallbackRegular,
                medium = fallbackRegular,
                semiBold = fallbackBold,
                bold = fallbackBold,
                extraBold = fallbackBold
            )
        }
        val reg = runCatching { ResourcesCompat.getFont(context, R.font.figtree_regular) }.getOrNull() ?: fallbackRegular
        val med = runCatching { ResourcesCompat.getFont(context, R.font.figtree_medium) }.getOrNull() ?: reg
        val semi = runCatching { ResourcesCompat.getFont(context, R.font.figtree_semibold) }.getOrNull() ?: fallbackBold
        val bld = runCatching { ResourcesCompat.getFont(context, R.font.figtree_bold) }.getOrNull() ?: fallbackBold
        val xbld = runCatching { ResourcesCompat.getFont(context, R.font.figtree_extrabold) }.getOrNull() ?: bld
        return FontSet(regular = reg, medium = med, semiBold = semi, bold = bld, extraBold = xbld)
    }

    fun render(model: SettleUpShareModel, context: Context? = null): Bitmap {
        val fonts = resolveFonts(context)
        val layout = SettleUpShareLayout.of(model)
        // The model caps rows so layout.height < MAX_HEIGHT; the clamp is belt-and-braces only.
        val bitmapHeight = layout.height.coerceAtMost(SettleUpShareLayout.MAX_HEIGHT)
        // A very tall card (~1080 x 9600 ARGB_8888 = ~41 MB) can fail on low-memory devices;
        // fall back to RGB_565 (half the memory, the card is fully opaque) instead of crashing.
        val bitmap = try {
            Bitmap.createBitmap(layout.width, bitmapHeight, Bitmap.Config.ARGB_8888)
        } catch (oom: OutOfMemoryError) {
            Bitmap.createBitmap(layout.width, bitmapHeight, Bitmap.Config.RGB_565)
        }
        val canvas = Canvas(bitmap)
        canvas.drawColor(CANVAS)

        // Outer tactile sheet frame with warm rounded border so the card pops in any chat bubble
        val frameInset = 24f
        val frameRect = RectF(frameInset, frameInset, layout.width - frameInset, bitmap.height - frameInset)
        canvas.drawRoundRect(frameRect, 52f, 52f, fill(CANVAS_WARM))
        canvas.drawRoundRect(frameRect, 52f, 52f, stroke(CARD_BORDER, 3f))

        val left = layout.padding.toFloat()
        val right = (layout.width - layout.padding).toFloat()
        val contentW = right - left

        drawBrand(canvas, model, layout, left, right, fonts)
        drawTrip(canvas, model, layout, left, right, contentW, fonts)
        drawTotal(canvas, model, layout, left, right, fonts)
        drawSectionTitle(canvas, model.transfersTitle, left, layout.transfersTitleTop, contentW, fonts)
        drawCard(canvas, left, layout.transfersCardTop.toFloat(), right, (layout.transfersCardTop + layout.transfersCardHeight).toFloat())
        if (model.isAllSettled) {
            drawAllSettled(canvas, model, layout, left, right, contentW, fonts)
        } else {
            drawTransfers(canvas, model, layout, left, right, fonts)
        }
        val netTitleTop = layout.netTitleTop
        val netCardTop = layout.netCardTop
        if (netTitleTop != null && netCardTop != null) {
            drawSectionTitle(canvas, model.netSummaryTitle, left, netTitleTop, contentW, fonts)
            drawCard(canvas, left, netCardTop.toFloat(), right, (netCardTop + layout.netCardHeight).toFloat())
            drawNetRows(canvas, model, netCardTop, left, right, fonts)
        }
        drawFooter(canvas, model, layout, left, right, contentW, fonts)
        return bitmap
    }

    private fun fill(color: Int): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }

    private fun stroke(color: Int, width: Float): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = width }

    /** [color] with its alpha replaced by [alpha] (0..255); keeps the palette as the only source. */
    private fun withAlpha(color: Int, alpha: Int): Int = (alpha.coerceIn(0, 255) shl 24) or (color and 0x00FFFFFF)

    private fun paint(
        color: Int,
        sizePx: Float,
        typeface: Typeface,
        tabularNums: Boolean = false,
        letterSpacingEm: Float = 0f
    ): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.color = color
            this.textSize = sizePx
            this.typeface = typeface
            if (tabularNums) {
                this.fontFeatureSettings = "tnum"
            }
            if (letterSpacingEm != 0f) {
                this.letterSpacing = letterSpacingEm
            }
        }

    private fun fit(text: String, p: TextPaint, maxW: Float): String =
        if (maxW <= 0f) "" else TextUtils.ellipsize(text, p, maxW, TextUtils.TruncateAt.END).toString()

    /**
     * Shrinks [p] (from [maxSize] down to [minSize]) until [text] fits [availW].
     * Returns true when it fits without ellipsis.
     */
    private fun scaleToFit(text: String, p: TextPaint, availW: Float, maxSize: Float, minSize: Float): Boolean {
        p.textSize = maxSize
        val w = p.measureText(text)
        if (w <= availW) return true
        if (availW <= 0f) return false
        p.textSize = (maxSize * availW / w).coerceIn(minSize, maxSize)
        // Glyph advances don't scale perfectly linearly (hinting); nudge down until it fits.
        while (p.measureText(text) > availW && p.textSize > minSize) {
            p.textSize = (p.textSize - 1f).coerceAtLeast(minSize)
        }
        return p.measureText(text) <= availW
    }

    /** Draws [text] vertically centred in a box starting at [top] with height [h]. */
    private fun drawCentered(canvas: Canvas, text: String, x: Float, top: Float, h: Float, p: TextPaint) {
        val fm = p.fontMetrics
        val baseline = top + h / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, x, baseline, p)
    }

    /**
     * Draws the authentic SplitMate brand mark exactly as the adaptive launcher icon:
     *  - `ic_launcher_background.xml`: Soft Charcoal (`#23201E`) base with the `#2C2825`
     *    r=42 inner ring centred at (54, 54), clipped to the squircle mask;
     *  - `ic_launcher_foreground.xml`: Upper-Left Organic Split Hemisphere in Sage Olive
     *    (`#D7E8B6`), Lower-Right Organic Split Hemisphere in Terracotta Coral (`#E06B52`),
     *    Upper Equilibrium Dot (`#365314`) and Lower Equilibrium Dot (`#FAF6F0`).
     */
    private fun drawSplitMateLogo(canvas: Canvas, left: Float, top: Float, size: Float) {
        val cornerRadius = size * 0.28f
        val bgRect = RectF(left, top, left + size, top + size)
        val saveCount = canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(bgRect, cornerRadius, cornerRadius, Path.Direction.CW) })
        canvas.drawRect(bgRect, fill(CHARCOAL))

        // Both vectors use a 108x108 viewport; the foreground paths occupy roughly [32..78].
        // Map the viewport window [20..88] (68 units, centred on 54) into [0..size], like the
        // adaptive-icon mask crop, so the mark fills the squircle without clipping.
        val scale = size / 68f
        canvas.translate(left - 20f * scale, top - 20f * scale)
        canvas.scale(scale, scale)

        // Subtle Inner Warm Ring (#2C2825) from ic_launcher_background.xml: M54,54 r=42
        canvas.drawCircle(54f, 54f, 42f, fill(LOGO_INNER_RING))

        // Upper-Left Organic Split Hemisphere (Sage Olive #D7E8B6)
        // M33,56 C30,42 41,30 56,32 L68,32 C70,32 71,35 69,37 L37,69 C35,71 32,70 32,68 Z
        val upperPath = Path().apply {
            moveTo(33f, 56f)
            cubicTo(30f, 42f, 41f, 30f, 56f, 32f)
            lineTo(68f, 32f)
            cubicTo(70f, 32f, 71f, 35f, 69f, 37f)
            lineTo(37f, 69f)
            cubicTo(35f, 71f, 32f, 70f, 32f, 68f)
            close()
        }
        canvas.drawPath(upperPath, fill(SAGE_BRIGHT))

        // Lower-Right Organic Split Hemisphere (Terracotta Coral #E06B52)
        // M75,52 C78,66 67,78 52,76 L40,76 C38,76 37,73 39,71 L71,39 C73,37 76,38 76,40 Z
        val lowerPath = Path().apply {
            moveTo(75f, 52f)
            cubicTo(78f, 66f, 67f, 78f, 52f, 76f)
            lineTo(40f, 76f)
            cubicTo(38f, 76f, 37f, 73f, 39f, 71f)
            lineTo(71f, 39f)
            cubicTo(73f, 37f, 76f, 38f, 76f, 40f)
            close()
        }
        canvas.drawPath(lowerPath, fill(TERRACOTTA))

        // Upper Equilibrium Dot (Deep Olive #365314) at (45, 45) r=4.5
        canvas.drawCircle(45f, 45f, 4.5f, fill(OLIVE_DEEP))

        // Lower Equilibrium Dot (Warm Cream #FAF6F0) at (63, 63) r=4.5
        canvas.drawCircle(63f, 63f, 4.5f, fill(CANVAS))

        canvas.restoreToCount(saveCount)
    }

    private fun drawCard(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val rect = RectF(l, t, r, b)
        canvas.drawRoundRect(rect, 40f, 40f, fill(CARD))
        canvas.drawRoundRect(RectF(l + 1.25f, t + 1.25f, r - 1.25f, b - 1.25f), 40f, 40f, stroke(CARD_BORDER, 2.5f))
    }

    private fun drawPill(
        canvas: Canvas,
        text: String,
        x: Float,
        top: Float,
        h: Float,
        bg: Int,
        fg: Int,
        sizePx: Float,
        maxW: Float,
        typeface: Typeface,
        alignRight: Boolean = false,
        borderColor: Int? = null
    ): Float {
        val p = paint(fg, sizePx, typeface)
        val padH = 26f
        val label = fit(text, p, maxW - padH * 2)
        val w = p.measureText(label) + padH * 2
        val l = if (alignRight) x - w else x
        val rect = RectF(l, top, l + w, top + h)
        canvas.drawRoundRect(rect, h / 2f, h / 2f, fill(bg))
        if (borderColor != null) {
            canvas.drawRoundRect(RectF(l + 1f, top + 1f, l + w - 1f, top + h - 1f), h / 2f, h / 2f, stroke(borderColor, 2f))
        }
        drawCentered(canvas, label, l + padH, top, h, p)
        return w
    }

    /**
     * 9-scallop "cookie" outline (Material 3 Expressive `MaterialShapes.Cookie9Sided`), with one
     * lobe pointing straight up. Radius varies smoothly between [outerR] and
     * `outerR * (1 - COOKIE_DEPTH)`.
     */
    private fun cookiePath(cx: Float, cy: Float, outerR: Float): Path = Path().apply {
        for (i in 0 until COOKIE_SAMPLES) {
            val theta = 2.0 * PI * i / COOKIE_SAMPLES - PI / 2.0
            val wave = (1.0 - cos(COOKIE_LOBES * (theta + PI / 2.0))) / 2.0 // 0 at lobe tip, 1 in valley
            val r = outerR * (1.0 - COOKIE_DEPTH * wave)
            val px = (cx + r * cos(theta)).toFloat()
            val py = (cy + r * sin(theta)).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }

    private fun drawAvatarBadge(
        canvas: Canvas,
        initials: String,
        left: Float,
        centerY: Float,
        size: Float,
        bgColor: Int,
        borderColor: Int,
        textColor: Int,
        fonts: FontSet
    ) {
        val cx = left + size / 2f
        val outerR = size / 2f
        canvas.drawPath(cookiePath(cx, centerY, outerR), fill(bgColor))
        canvas.drawPath(cookiePath(cx, centerY, outerR - 1f), stroke(borderColor, 2f))
        // Keep initials inside the cookie's inner (valley) circle.
        val innerDiameter = size * (1f - COOKIE_DEPTH) * 0.78f
        val p = paint(textColor, size * 0.38f, fonts.extraBold).apply { textAlign = Paint.Align.CENTER }
        scaleToFit(initials, p, innerDiameter, size * 0.38f, size * 0.22f)
        drawCentered(canvas, fit(initials, p, innerDiameter), cx, centerY - size / 2f, size, p)
    }

    private fun drawBrand(
        canvas: Canvas,
        model: SettleUpShareModel,
        layout: SettleUpShareLayout,
        left: Float,
        right: Float,
        fonts: FontSet
    ) {
        val top = layout.brandTop.toFloat()
        val h = SettleUpShareLayout.BRAND_H.toFloat()
        drawSplitMateLogo(canvas, left, top, h)

        val textLeft = left + h + 22f
        val brandP = paint(CHARCOAL, 40f, fonts.extraBold)
        drawCentered(canvas, model.brandName, textLeft, top + 4f, 46f, brandP)

        val subP = paint(OLIVE, 20f, fonts.bold, letterSpacingEm = 0.08f)
        drawCentered(canvas, model.brandSubtitle, textLeft, top + 48f, 30f, subP)

        val brandEnd = textLeft + maxOf(brandP.measureText(model.brandName), subP.measureText(model.brandSubtitle)) + 32f
        val asOf = model.asOfLabel
        if (asOf != null) {
            drawPill(
                canvas = canvas,
                text = asOf,
                x = right,
                top = top + 14f,
                h = h - 28f,
                bg = SUNKEN,
                fg = MUTED,
                sizePx = 26f,
                maxW = right - brandEnd,
                typeface = fonts.semiBold,
                alignRight = true,
                borderColor = CARD_BORDER
            )
        }
    }

    private fun drawTrip(
        canvas: Canvas,
        model: SettleUpShareModel,
        layout: SettleUpShareLayout,
        left: Float,
        right: Float,
        contentW: Float,
        fonts: FontSet
    ) {
        val tripP = paint(CHARCOAL, 62f, fonts.extraBold)
        drawCentered(canvas, fit(model.tripName, tripP, contentW), left, layout.tripTop.toFloat(), SettleUpShareLayout.TRIP_H.toFloat(), tripP)
        val metaTop = layout.metaTop.toFloat()
        val metaH = SettleUpShareLayout.META_H.toFloat()
        var x = left
        val badge = model.tripEndedBadge
        if (badge != null) {
            x += drawPill(
                canvas = canvas,
                text = badge,
                x = x,
                top = metaTop + 4f,
                h = metaH - 8f,
                bg = PEACH,
                fg = TERRACOTTA_TEXT,
                sizePx = 26f,
                maxW = contentW / 2f,
                typeface = fonts.bold,
                borderColor = PEACH_DEEP
            ) + 20f
        }
        if (model.metaLine.isNotEmpty()) {
            val metaP = paint(MUTED, 30f, fonts.medium)
            drawCentered(canvas, fit(model.metaLine, metaP, right - x), x, metaTop, metaH, metaP)
        }
    }

    private fun drawTotal(
        canvas: Canvas,
        model: SettleUpShareModel,
        layout: SettleUpShareLayout,
        left: Float,
        right: Float,
        fonts: FontSet
    ) {
        val top = layout.totalCardTop.toFloat()
        val h = SettleUpShareLayout.TOTAL_CARD_H.toFloat()
        val cardRect = RectF(left, top, right, top + h)

        // Tonal Sage Hero Card with M3 Expressive organic decorative arcs clipped inside
        val save = canvas.save()
        val clipPath = Path().apply { addRoundRect(cardRect, 44f, 44f, Path.Direction.CW) }
        canvas.clipPath(clipPath)
        canvas.drawColor(SAGE)

        // Subtle decorative equilibrium rings on right side
        val decoPaint = fill(withAlpha(OLIVE_DEEP, 26))
        canvas.drawCircle(right - 70f, top + 30f, 130f, decoPaint)
        canvas.drawCircle(right - 180f, top + h + 20f, 110f, decoPaint)
        canvas.restoreToCount(save)

        canvas.drawRoundRect(RectF(left + 1.5f, top + 1.5f, right - 1.5f, top + h - 1.5f), 44f, 44f, stroke(SAGE_BRIGHT, 2.5f))

        val innerL = left + 40f
        val innerR = right - 40f
        val innerW = innerR - innerL
        val gap = 28f

        // Right-side status badge inside hero card
        val badgeW = drawPill(
            canvas = canvas,
            text = model.heroBadgeText,
            x = right - 36f,
            top = top + 34f,
            h = 54f,
            bg = OLIVE_DEEP,
            fg = CANVAS,
            sizePx = 24f,
            maxW = 320f,
            typeface = fonts.extraBold,
            alignRight = true
        )

        val labelP = paint(OLIVE_DEEP, 25f, fonts.bold, letterSpacingEm = 0.08f)
        drawCentered(canvas, fit(model.totalSpentHeroLabel, labelP, innerW - badgeW - gap), innerL, top + 30f, 42f, labelP)

        // Hero amount vs "0.00 drift" sub-badge: both share the band top+78..top+178. Measure
        // first; if the amount can't sit beside the sub-badge even at a reduced size, drop the
        // sub-badge (the footer repeats the drift line) and give the amount the full width.
        val subP = paint(OLIVE_DEEP, 23f, fonts.semiBold).apply { textAlign = Paint.Align.RIGHT }
        val subW = subP.measureText(model.heroDriftText)
        val besideW = innerW - maxOf(badgeW, subW) - gap
        val amountP = paint(CHARCOAL, 76f, fonts.extraBold, tabularNums = true)
        val showSub = scaleToFit(model.totalSpentText, amountP, besideW, 76f, 56f)
        val amountMaxW = if (showSub) {
            besideW
        } else {
            scaleToFit(model.totalSpentText, amountP, innerW, 76f, 44f)
            innerW
        }
        if (showSub) {
            drawCentered(canvas, model.heroDriftText, right - 40f, top + 102f, 40f, subP)
        }
        drawCentered(canvas, fit(model.totalSpentText, amountP, amountMaxW), innerL, top + 78f, 100f, amountP)
    }

    private fun drawSectionTitle(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Int,
        contentW: Float,
        fonts: FontSet
    ) {
        val topF = top.toFloat()
        val h = SettleUpShareLayout.SECTION_TITLE_H.toFloat()
        val barH = 32f
        val barTop = topF + (h - barH) / 2f
        canvas.drawRoundRect(RectF(left, barTop, left + 8f, barTop + barH), 4f, 4f, fill(OLIVE))
        val p = paint(CHARCOAL, 36f, fonts.extraBold)
        drawCentered(canvas, fit(text, p, contentW - 24f), left + 22f, topF, h, p)
    }

    private fun drawTransfers(
        canvas: Canvas,
        model: SettleUpShareModel,
        layout: SettleUpShareLayout,
        left: Float,
        right: Float,
        fonts: FontSet
    ) {
        val rowH = SettleUpShareLayout.TRANSFER_ROW_H.toFloat()
        val innerL = left + 32f
        val innerR = right - 32f
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SUNKEN; strokeWidth = 2.5f }
        val nameP = paint(CHARCOAL, 34f, fonts.bold)
        val arrowBadgeP = paint(TERRACOTTA_TEXT, 22f, fonts.extraBold)
        val chipP = paint(TERRACOTTA_TEXT, 36f, fonts.extraBold, tabularNums = true)
        val avatarSize = 56f
        val arrowLabel = model.paysLabel
        val arrowPadH = 18f
        val arrowH = 42f
        val arrowW = arrowBadgeP.measureText(arrowLabel) + arrowPadH * 2

        var top = (layout.transfersCardTop + SettleUpShareLayout.CARD_V_PAD).toFloat()
        model.transferRows.forEachIndexed { index, row ->
            if (index > 0) canvas.drawLine(innerL, top, innerR, top, divider)
            val centerY = top + rowH / 2f

            // 1. Right-aligned amount pill (Peach container with Peach-deep border & Terracotta text)
            val chipPad = 26f
            val chipH = 74f
            val chipW = chipP.measureText(row.amountText) + chipPad * 2
            val chipL = innerR - chipW
            val chipT = centerY - chipH / 2f
            canvas.drawRoundRect(RectF(chipL, chipT, innerR, chipT + chipH), chipH / 2f, chipH / 2f, fill(PEACH))
            canvas.drawRoundRect(
                RectF(chipL + 1f, chipT + 1f, innerR - 1f, chipT + chipH - 1f),
                chipH / 2f,
                chipH / 2f,
                stroke(PEACH_DEEP, 2f)
            )
            drawCentered(canvas, row.amountText, chipL + chipPad, chipT, chipH, chipP)

            // 2. Left/Center: [FromAvatar] [FromName] [PAYS ->] [ToAvatar] [ToName]
            val availableW = chipL - 20f - innerL
            val fixedElementsW = avatarSize + 12f + 14f + arrowW + 14f + avatarSize + 12f
            val namesBudget = (availableW - fixedElementsW).coerceAtLeast(80f)
            val halfBudget = namesBudget / 2f
            val fromFull = nameP.measureText(row.fromName)
            val toFull = nameP.measureText(row.toName)
            val fromMaxW = if (fromFull <= halfBudget) fromFull else (namesBudget - minOf(toFull, halfBudget))
            val fromLabel = fit(row.fromName, nameP, fromMaxW)
            val fromActualW = nameP.measureText(fromLabel)
            val toMaxW = (namesBudget - fromActualW).coerceAtLeast(40f)
            val toLabel = fit(row.toName, nameP, toMaxW)

            var x = innerL
            // Sender avatar (Peach cookie)
            drawAvatarBadge(
                canvas = canvas,
                initials = row.fromInitials,
                left = x,
                centerY = centerY,
                size = avatarSize,
                bgColor = PEACH,
                borderColor = PEACH_DEEP,
                textColor = TERRACOTTA_TEXT,
                fonts = fonts
            )
            x += avatarSize + 12f
            drawCentered(canvas, fromLabel, x, top, rowH, nameP)
            x += fromActualW + 14f

            // Directional "PAYS ->" pill
            val arrowTop = centerY - arrowH / 2f
            canvas.drawRoundRect(RectF(x, arrowTop, x + arrowW, arrowTop + arrowH), arrowH / 2f, arrowH / 2f, fill(SUNKEN))
            drawCentered(canvas, arrowLabel, x + arrowPadH, arrowTop, arrowH, arrowBadgeP)
            x += arrowW + 14f

            // Receiver avatar (Sage cookie)
            drawAvatarBadge(
                canvas = canvas,
                initials = row.toInitials,
                left = x,
                centerY = centerY,
                size = avatarSize,
                bgColor = SAGE,
                borderColor = SAGE_BRIGHT,
                textColor = OLIVE_DEEP,
                fonts = fonts
            )
            x += avatarSize + 12f
            drawCentered(canvas, toLabel, x, top, rowH, nameP)

            top += rowH
        }
        val more = model.moreTransfersText
        if (more != null) {
            canvas.drawLine(innerL, top, innerR, top, divider)
            drawCentered(
                canvas,
                more,
                innerL,
                top,
                SettleUpShareLayout.MORE_ROW_H.toFloat(),
                paint(MUTED, 30f, fonts.bold)
            )
        }
    }

    private fun drawAllSettled(
        canvas: Canvas,
        model: SettleUpShareModel,
        layout: SettleUpShareLayout,
        left: Float,
        right: Float,
        contentW: Float,
        fonts: FontSet
    ) {
        val top = layout.transfersCardTop.toFloat()
        val cx = (left + right) / 2f
        val titleP = paint(OLIVE_DEEP, 50f, fonts.extraBold).apply { textAlign = Paint.Align.CENTER }
        drawCentered(canvas, fit(model.allSettledTitle, titleP, contentW - 64f), cx, top + 52f, 80f, titleP)
        val subP = paint(MUTED, 32f, fonts.medium).apply { textAlign = Paint.Align.CENTER }
        drawCentered(canvas, fit(model.allSettledSubtitle, subP, contentW - 64f), cx, top + 140f, 56f, subP)
    }

    private fun drawNetRows(
        canvas: Canvas,
        model: SettleUpShareModel,
        cardTop: Int,
        left: Float,
        right: Float,
        fonts: FontSet
    ) {
        val rowH = SettleUpShareLayout.NET_ROW_H.toFloat()
        val innerL = left + 32f
        val innerR = right - 32f
        val nameP = paint(CHARCOAL, 34f, fonts.bold)
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SUNKEN; strokeWidth = 2.5f }
        val avatarSize = 48f
        var top = (cardTop + SettleUpShareLayout.CARD_V_PAD).toFloat()

        model.netRows.forEachIndexed { index, row ->
            if (index > 0) canvas.drawLine(innerL, top, innerR, top, divider)
            val centerY = top + rowH / 2f

            val (pillBg, pillBorder, pillFg) = when (row.direction) {
                SettleUpNetDirection.GETS_BACK -> Triple(SAGE, SAGE_BRIGHT, OLIVE_DEEP)
                SettleUpNetDirection.PAYS -> Triple(PEACH, PEACH_DEEP, TERRACOTTA_TEXT)
                SettleUpNetDirection.SETTLED -> Triple(SUNKEN, CARD_BORDER, MUTED)
            }
            val pillW = drawPill(
                canvas = canvas,
                text = row.netText,
                x = innerR,
                top = centerY - 28f,
                h = 56f,
                bg = pillBg,
                fg = pillFg,
                sizePx = 28f,
                maxW = (innerR - innerL) * 0.55f,
                typeface = fonts.bold,
                alignRight = true,
                borderColor = pillBorder
            )

            drawAvatarBadge(
                canvas = canvas,
                initials = row.initials,
                left = innerL,
                centerY = centerY,
                size = avatarSize,
                bgColor = pillBg,
                borderColor = pillBorder,
                textColor = pillFg,
                fonts = fonts
            )
            val nameLeft = innerL + avatarSize + 16f
            val maxNameW = innerR - pillW - 24f - nameLeft
            drawCentered(canvas, fit(row.name, nameP, maxNameW), nameLeft, top, rowH, nameP)
            top += rowH
        }
        val more = model.moreNetText
        if (more != null) {
            if (model.netRows.isNotEmpty()) canvas.drawLine(innerL, top, innerR, top, divider)
            drawCentered(
                canvas,
                more,
                innerL,
                top,
                SettleUpShareLayout.MORE_ROW_H.toFloat(),
                paint(MUTED, 30f, fonts.bold)
            )
        }
    }

    private fun drawFooter(
        canvas: Canvas,
        model: SettleUpShareModel,
        layout: SettleUpShareLayout,
        left: Float,
        right: Float,
        contentW: Float,
        fonts: FontSet
    ) {
        val top = layout.footerTop.toFloat()
        canvas.drawLine(left, top, right, top, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD_BORDER; strokeWidth = 2f })
        val cx = (left + right) / 2f
        val mainP = paint(OLIVE_DEEP, 32f, fonts.extraBold)
        val footerLabel = fit(model.footerText, mainP, contentW - 60f)
        val labelW = mainP.measureText(footerLabel)
        val logoSize = 38f
        val gap = 14f
        val totalHeaderW = logoSize + gap + labelW
        val startX = cx - totalHeaderW / 2f

        drawSplitMateLogo(canvas, startX, top + 18f, logoSize)
        drawCentered(canvas, footerLabel, startX + logoSize + gap, top + 14f, 46f, mainP)

        val subP = paint(MUTED, 25f, fonts.semiBold).apply { textAlign = Paint.Align.CENTER }
        drawCentered(canvas, fit(model.footerSubText, subP, contentW), cx, top + 66f, 40f, subP)
    }
}
