package com.splitmate.app.data

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/**
 * v2.4.0 P1: remembers WHEN an expense was edited on this phone, so its version reflects the edit,
 * not the (possibly much later) sync. Two edits made offline at the same time then resolve by which
 * edit was actually newer. Survives an app restart (SharedPreferences); cleared once the edit is
 * versioned.
 */
object ExpenseEditClock {

    private const val PREFS = "splitmate_expense_edit_clock"
    private val memory = ConcurrentHashMap<String, Long>()

    private fun prefs(context: Context?) =
        context?.applicationContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Keeps the LATEST edit time since the last sync (review F2: the newest edit is what competes). */
    fun record(context: Context?, expenseIds: Collection<String>, atEpochMs: Long = System.currentTimeMillis()) {
        if (expenseIds.isEmpty()) return
        val p = prefs(context)
        val editor = p?.edit()
        for (id in expenseIds) {
            val existing = memory[id] ?: p?.getLong(id, 0L)?.takeIf { it > 0L }
            val value = maxOf(existing ?: 0L, atEpochMs)
            memory[id] = value
            editor?.putLong(id, value)
        }
        // Review N2: written synchronously, so an edit committed to Room never loses its edit time.
        editor?.commit()
    }

    fun editTimeOf(context: Context?, expenseId: String): Long? =
        memory[expenseId] ?: prefs(context)?.getLong(expenseId, 0L)?.takeIf { it > 0L }

    fun clear(context: Context?, expenseIds: Collection<String>) {
        if (expenseIds.isEmpty()) return
        expenseIds.forEach { memory.remove(it) }
        prefs(context)?.edit()?.apply { expenseIds.forEach { remove(it) } }?.apply()
    }
}
