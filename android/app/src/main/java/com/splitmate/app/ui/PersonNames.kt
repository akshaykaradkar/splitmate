package com.splitmate.app.ui

import com.splitmate.app.data.GroupMemberEntity
import java.util.Locale

/**
 * v2.4.0 rc12: one rule for how people's names are shown, app-wide.
 *
 * Names arrive typed any way ("sam", "PRIYA SHAH", "  rohan  "), from the keyboard, contacts,
 * invite links and other phones via cloud sync. They are normalised where they ENTER the app
 * (local entry, Room reads, cloud decode), so every screen, the split engine's labels and the
 * settle-up text all read the same name without each screen having to remember a formatter.
 *
 * - All-lowercase or ALL-CAPS words become Title Case ("sam" -> "Sam", "PRIYA SHAH" -> "Priya Shah"),
 *   including hyphenated parts ("anne-marie" -> "Anne-Marie").
 * - A word typed in mixed case keeps its casing ("McKenzie", "DeSouza"), so real names never break.
 * - Extra spaces collapse to one. Non-letter tokens ("2", "(You)") are left alone.
 *
 * Matching by name elsewhere is already case-insensitive, so normalising is safe for joins and dedupe.
 */
object PersonNames {
    private val SPACES = Regex("""\s+""")

    fun display(raw: String): String {
        val words = raw.trim().split(SPACES).filter { it.isNotEmpty() }
        if (words.isEmpty()) return ""
        val allUpper = words.all { w -> w.none { it.isLowerCase() } }
        return words.joinToString(" ") { w ->
            when {
                w.none { it.isLetter() } -> w
                w.all { !it.isLetter() || it.isLowerCase() } || allUpper ->
                    w.lowercase(Locale.ROOT).split("-").joinToString("-") { part ->
                        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    }
                else -> w
            }
        }
    }
}

/** This member with its name in display form (same instance when already normalised). */
fun GroupMemberEntity.withDisplayName(): GroupMemberEntity {
    val shown = PersonNames.display(name)
    return if (shown == name || shown.isEmpty()) this else copy(name = shown)
}

fun List<GroupMemberEntity>.withDisplayNames(): List<GroupMemberEntity> = map { it.withDisplayName() }
