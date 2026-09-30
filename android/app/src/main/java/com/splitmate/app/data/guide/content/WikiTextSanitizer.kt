package com.splitmate.app.data.guide.content

import java.text.Normalizer

/**
 * Converts MediaWiki wikitext fragments into safe plain text for Compose `Text`.
 *
 * Handles: HTML comments, `<ref>` footnotes, nested templates (a few inline ones are rendered,
 * everything else is dropped), `[[File:…]]`/`[[Category:…]]` removal, `[[target|label]]` → label,
 * `[https://x label]` → label, bold/italic apostrophes, HTML tags, HTML entities, control
 * characters and whitespace. The output never contains `<` `>` tag markup.
 *
 * Pure and deterministic; no Android dependencies.
 */
object WikiTextSanitizer {

    const val MAX_BLURB_CHARS = 280
    const val ELLIPSIS = "…"

    /** Result of [truncate]. */
    data class Truncated(val text: String, val truncated: Boolean)

    private val COMMENT = Regex("<!--.*?(-->|$)", RegexOption.DOT_MATCHES_ALL)
    private val SCRIPT_STYLE = Regex("<(script|style)\\b[^>]*>.*?(</\\1\\s*>|$)", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val REF_SELF = Regex("<ref\\b[^>]*/\\s*>", RegexOption.IGNORE_CASE)
    private val REF_PAIRED = Regex("<ref\\b(?:[^>]*[^/>])?>.*?</ref\\s*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val REF_UNCLOSED = Regex("<ref\\b(?:[^>]*[^/>])?>.*$", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val BR = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val TAG = Regex("</?[A-Za-z][^<>]*>")
    private val STRAY_ANGLE = Regex("[<>]")
    private val APOSTROPHES = Regex("'{2,}")
    private val EXT_LINK_LABELLED = Regex("\\[(?:https?:)?//[^\\s\\]]+\\s+([^\\]]*)]")
    private val EXT_LINK_BARE = Regex("\\[(?:https?:)?//[^\\s\\]]+]")
    private val CONTROL = Regex("[\\p{Cntrl}&&[^\\n\\t]]")
    private val WHITESPACE = Regex("\\s+")
    private val NUMERIC_ENTITY = Regex("&#(\\d{1,7});")
    private val HEX_ENTITY = Regex("&#[xX]([0-9A-Fa-f]{1,6});")
    private val NAMED_ENTITY = Regex("&([A-Za-z]{2,8});")
    private val NAMED = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
        "nbsp" to " ", "ndash" to "–", "mdash" to "—", "hellip" to "…", "rsquo" to "’",
        "lsquo" to "‘", "rdquo" to "”", "ldquo" to "“", "middot" to "·", "times" to "×",
        "deg" to "°", "rarr" to "→", "larr" to "←", "euro" to "€", "pound" to "£",
        "copy" to "©", "reg" to "®", "minus" to "−", "thinsp" to " ", "ensp" to " ", "emsp" to " "
    )
    private val LINK_DROP_NAMESPACES = setOf("file", "image", "category", "media")

    /** Removes `<!-- … -->` comments (an unterminated comment runs to the end of the text). */
    fun stripComments(text: String): String = COMMENT.replace(text, "")

    /** Full sanitisation to single-line plain text. */
    fun toPlainText(wikitext: String?): String {
        if (wikitext.isNullOrEmpty()) return ""
        var s = stripComments(wikitext)
        s = SCRIPT_STYLE.replace(s, "")
        s = REF_SELF.replace(s, "")
        s = REF_PAIRED.replace(s, "")
        s = REF_UNCLOSED.replace(s, "")
        s = replaceWikiLinks(s)
        s = expandTemplates(s)
        s = EXT_LINK_LABELLED.replace(s) { it.groupValues[1] }
        s = EXT_LINK_BARE.replace(s, "")
        s = APOSTROPHES.replace(s, "")
        s = BR.replace(s, " ")
        s = TAG.replace(s, "")
        s = decodeEntities(s)
        // Entities may have produced markup ("&lt;script&gt;"): strip again, then any stray angle.
        s = TAG.replace(s, "")
        s = STRAY_ANGLE.replace(s, "")
        s = CONTROL.replace(s, "")
        s = WHITESPACE.replace(s, " ").trim()
        return s
    }

    /** Word-boundary truncation to [max] chars (including the ellipsis). */
    fun truncate(text: String, max: Int = MAX_BLURB_CHARS): Truncated {
        if (text.length <= max) return Truncated(text, false)
        val hardCut = text.substring(0, (max - ELLIPSIS.length).coerceAtLeast(0))
        val lastSpace = hardCut.lastIndexOf(' ')
        val cut = if (lastSpace >= hardCut.length / 2) hardCut.substring(0, lastSpace) else hardCut
        return Truncated(cut.trimEnd(' ', ',', ';', ':', '-', '–') + ELLIPSIS, true)
    }

    /** Stable URL-ish slug: ASCII, lowercase, `-` separated, max [maxLen] chars. */
    fun slugify(text: String, maxLen: Int = 60): String {
        val ascii = Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        val slug = ascii.lowercase()
            .replace('&', ' ')
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return if (slug.length <= maxLen) slug else slug.substring(0, maxLen).trimEnd('-')
    }

    // ------------------------------------------------------------------------------------------

    /** Repeatedly rewrites innermost `{{…}}` until none remain (bounded). */
    internal fun expandTemplates(input: String): String {
        var s = input
        var guard = 0
        while (guard++ < 64) {
            val close = s.indexOf("}}")
            if (close < 0) break
            val open = s.lastIndexOf("{{", close)
            if (open < 0) {
                s = s.removeRange(close, close + 2)
                continue
            }
            val body = s.substring(open + 2, close)
            s = s.substring(0, open) + renderInlineTemplate(body) + s.substring(close + 2)
        }
        return s.replace("{{", "")
    }

    /** Renders common inline templates; unknown templates are dropped. */
    private fun renderInlineTemplate(body: String): String {
        val parts = body.split('|').map { it.trim() }
        val name = parts.firstOrNull()?.lowercase()?.replace('_', ' ')?.trim().orEmpty()
        val args = parts.drop(1).filter { !it.contains('=') }
        return when (name) {
            "km", "mi", "m", "ft", "kg" -> if (args.isNotEmpty()) "${args[0]} $name" else ""
            "convert", "cvt" -> if (args.size >= 2) "${args[0]} ${args[1]}" else args.firstOrNull().orEmpty()
            "lang", "lang-hi", "lang-kn", "lang-mr" -> args.lastOrNull().orEmpty()
            "nowrap", "nobr", "small", "big", "smallcaps", "sic" -> args.firstOrNull().orEmpty()
            "w", "wikipedia" -> args.lastOrNull().orEmpty()
            "inr", "rupee", "rs", "₹" -> "₹" + args.firstOrNull().orEmpty()
            "usd" -> "US$" + args.firstOrNull().orEmpty()
            "!" -> "|"
            "=" -> "="
            else -> ""
        }
    }

    /** `[[target|label]]` → label, `[[target]]` → target, drops File/Category links (nested-safe). */
    internal fun replaceWikiLinks(input: String): String {
        var s = input
        var guard = 0
        while (guard++ < 256) {
            val close = s.indexOf("]]")
            if (close < 0) break
            val open = s.lastIndexOf("[[", close)
            if (open < 0) {
                s = s.removeRange(close, close + 2)
                continue
            }
            val inner = s.substring(open + 2, close)
            val target = inner.substringBefore('|').trim()
            val ns = target.substringBefore(':', "").trim().lowercase()
            val replacement = when {
                ns in LINK_DROP_NAMESPACES -> ""
                inner.contains('|') -> inner.substringAfterLast('|').trim().ifEmpty { target.substringAfter(':') }
                else -> target.removePrefix(":").substringBefore('#').ifEmpty { target }
            }
            s = s.substring(0, open) + replacement + s.substring(close + 2)
        }
        return s.replace("[[", "")
    }

    private fun decodeEntities(input: String): String {
        var s = NUMERIC_ENTITY.replace(input) { m -> codePointOrEmpty(m.groupValues[1].toIntOrNull()) }
        s = HEX_ENTITY.replace(s) { m -> codePointOrEmpty(m.groupValues[1].toIntOrNull(16)) }
        s = NAMED_ENTITY.replace(s) { m -> NAMED[m.groupValues[1].lowercase()] ?: m.value }
        return s
    }

    private fun codePointOrEmpty(cp: Int?): String =
        if (cp == null || !Character.isValidCodePoint(cp) || Character.isISOControl(cp)) ""
        else String(Character.toChars(cp))
}
