package com.splitmate.app.ui.share

/**
 * v2.3.5 (#2): the single source of colour for the settle-up share bitmap.
 *
 * Every value is a canonical Stitch "Organic Tactile Financial" (Buckwheat) token from GEMINI.md,
 * except [LOGO_INNER_RING], which is copied verbatim from `res/drawable/ic_launcher_background.xml`
 * so the brand mark matches the launcher icon. Plain ARGB ints (no android.graphics) so JVM tests
 * can assert the image never drifts off-palette.
 */
object ShareCardPalette {
    // Canvas / surfaces
    const val CANVAS: Int = 0xFFFAF6F0.toInt()
    const val CANVAS_WARM: Int = 0xFFF7F3EC.toInt()
    const val SUNKEN: Int = 0xFFF4EFE6.toInt()
    const val CARD: Int = 0xFFFFFFFF.toInt()
    const val CARD_BORDER: Int = 0xFFEDE7DF.toInt()

    // Primary olive / sage
    const val OLIVE: Int = 0xFF416913.toInt()
    const val OLIVE_DEEP: Int = 0xFF365314.toInt()
    const val SAGE: Int = 0xFFDCE9B9.toInt()
    const val SAGE_BRIGHT: Int = 0xFFD7E8B6.toInt()

    // Secondary terracotta / peach
    const val TERRACOTTA: Int = 0xFFE06B52.toInt()
    const val PEACH: Int = 0xFFFCE3D7.toInt()
    const val PEACH_DEEP: Int = 0xFFFED8C8.toInt()
    const val TERRACOTTA_TEXT: Int = 0xFF7C2D12.toInt()

    // Typography
    const val CHARCOAL: Int = 0xFF23201E.toInt()
    /** Warm secondary text (charcoal family) for meta lines and "+N more" rows. */
    const val MUTED: Int = 0xFF6B625A.toInt()

    /** `#2C2825` r=42 inner ring from ic_launcher_background.xml (logo only). */
    const val LOGO_INNER_RING: Int = 0xFF2C2825.toInt()

    /** Every colour the renderer is allowed to use (opaque RGB, alpha stripped). */
    val ALL: List<Int> = listOf(
        CANVAS, CANVAS_WARM, SUNKEN, CARD, CARD_BORDER,
        OLIVE, OLIVE_DEEP, SAGE, SAGE_BRIGHT,
        TERRACOTTA, PEACH, PEACH_DEEP, TERRACOTTA_TEXT,
        CHARCOAL, MUTED, LOGO_INNER_RING
    )
}
