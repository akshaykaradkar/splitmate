package com.splitmate.app.data.guide.loop

import java.io.ByteArrayOutputStream

/**
 * Small, pure-Kotlin text and URL helpers for the smart-loop package.
 *
 * Everything here is deterministic and free of Android framework types so it can be unit-tested
 * on the JVM. None of these helpers log anything (no PII in logs).
 */
internal object LoopText {

    /** Default maximum length for user-visible labels derived from external text. */
    const val MAX_LABEL_CHARS = 120

    private val CONTROL_OR_FORMAT = Regex("""[\p{Cc}\p{Cf}\u2028\u2029]""")
    private val WHITESPACE_RUN = Regex("""\s+""")
    private val HTML_TAG = Regex("""<[^>]{0,200}>""")

    /**
     * Sanitises external text (Wikidata labels, URL-derived hints, Nominatim names) before it can
     * reach the UI: strips control/format characters (incl. bidi overrides), HTML tags, collapses
     * whitespace, trims surrounding quotes and truncates to [maxChars] with an ellipsis.
     *
     * @return the cleaned text, or null when nothing meaningful is left.
     */
    fun clean(raw: String?, maxChars: Int = MAX_LABEL_CHARS): String? {
        if (raw == null) return null
        var s = raw
            .replace(HTML_TAG, " ")
            .replace(CONTROL_OR_FORMAT, " ")
            .replace(WHITESPACE_RUN, " ")
            .trim()
            .trim('"', '\'', '“', '”', '‘', '’')
            .trim()
        if (s.isEmpty()) return null
        if (s.length > maxChars) {
            s = s.take(maxChars - 1).trimEnd() + "…"
        }
        return s
    }

    /**
     * Lenient percent-decoder (RFC 3986 `%XX` sequences, UTF-8). Unlike [java.net.URLDecoder] it
     * never throws: malformed escapes are kept literally.
     *
     * @param plusAsSpace when true, `+` is decoded to a space (form/query semantics).
     */
    fun percentDecode(s: String, plusAsSpace: Boolean = true): String {
        if (s.indexOf('%') < 0 && (!plusAsSpace || s.indexOf('+') < 0)) return s
        val out = StringBuilder(s.length)
        val bytes = ByteArrayOutputStream()
        fun flush() {
            if (bytes.size() > 0) {
                out.append(String(bytes.toByteArray(), Charsets.UTF_8))
                bytes.reset()
            }
        }
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%' && i + 2 < s.length) {
                val hi = Character.digit(s[i + 1], 16)
                val lo = Character.digit(s[i + 2], 16)
                if (hi >= 0 && lo >= 0) {
                    bytes.write((hi shl 4) or lo)
                    i += 3
                    continue
                }
            }
            flush()
            out.append(if (plusAsSpace && c == '+') ' ' else c)
            i++
        }
        flush()
        return out.toString()
    }

    private val PERCENT_ESCAPE = Regex("""%[0-9A-Fa-f]{2}""")

    /**
     * Decodes [s] repeatedly (at most [maxRounds]) to undo double-encoding that is common in
     * redirect `continue=` parameters and chat-app re-shares. Also normalises `&amp;` and the
     * JSON escapes `\u0026` / `\u003d` that sometimes appear in copied links.
     */
    fun deepDecode(s: String, maxRounds: Int = 3): String {
        var cur = s.replace("&amp;", "&").replace("\\u0026", "&").replace("\\u003d", "=")
        cur = percentDecode(cur, plusAsSpace = true)
        var round = 1
        while (round < maxRounds && PERCENT_ESCAPE.containsMatchIn(cur)) {
            cur = percentDecode(cur, plusAsSpace = false)
            round++
        }
        return cur
    }

    private const val UNRESERVED =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    private const val HEX = "0123456789ABCDEF"

    /**
     * RFC 3986 percent-encoder: every byte outside the unreserved set is encoded as `%XX`
     * (upper-case hex, UTF-8); spaces become `%20`. Safe for query values and URI labels.
     */
    fun percentEncode(s: String): String {
        val sb = StringBuilder(s.length + 16)
        for (b in s.toByteArray(Charsets.UTF_8)) {
            val v = b.toInt() and 0xFF
            val ch = v.toChar()
            if (v < 128 && UNRESERVED.indexOf(ch) >= 0) {
                sb.append(ch)
            } else {
                sb.append('%')
                sb.append(HEX[v shr 4])
                sb.append(HEX[v and 0xF])
            }
        }
        return sb.toString()
    }
}

/**
 * Minimal, non-throwing absolute-URL splitter used for allow-list checks. We intentionally do not
 * use [java.net.URI] because real-world map links frequently contain characters (`|`, spaces,
 * unencoded quotes) that make it throw.
 */
internal data class SimpleUrl(
    val scheme: String,
    val host: String,
    val port: Int?,
    val path: String,
    val query: String?
) {
    companion object {
        private val URL_SHAPE =
            Regex("""^([a-zA-Z][a-zA-Z0-9+.\-]*)://([^/?#]*)([^?#]*)(?:\?([^#]*))?""")

        /**
         * Parses an absolute URL. Returns null for relative URLs, URLs with user-info
         * (`https://maps.google.com@evil.example/` spoofing) or an unparsable port.
         */
        fun parse(url: String): SimpleUrl? {
            val m = URL_SHAPE.find(url.trim()) ?: return null
            val authority = m.groupValues[2]
            if (authority.isEmpty() || authority.contains('@')) return null
            val hostPart = authority.substringBefore(':')
            val portPart = if (authority.contains(':')) authority.substringAfter(':') else null
            val nonEmptyPort: String? = portPart?.takeIf { it.isNotEmpty() }
            val port: Int? = if (nonEmptyPort == null) null else (nonEmptyPort.toIntOrNull() ?: return null)
            val host = hostPart.lowercase().trimEnd('.')
            if (host.isEmpty()) return null
            return SimpleUrl(
                scheme = m.groupValues[1].lowercase(),
                host = host,
                port = port,
                path = m.groupValues[3],
                query = m.groups[4]?.value
            )
        }
    }

    /** Returns the raw (still percent-encoded) value of the first query parameter [name]. */
    fun rawQueryParam(name: String): String? {
        val q = query ?: return null
        for (pair in q.split('&')) {
            val key = pair.substringBefore('=')
            if (key.equals(name, ignoreCase = true) && pair.contains('=')) {
                return pair.substringAfter('=')
            }
        }
        return null
    }

    /** Scheme + authority, e.g. `https://www.google.com`. */
    val origin: String get() = "$scheme://$host" + (port?.let { ":$it" } ?: "")
}
