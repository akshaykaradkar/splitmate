package com.splitmate.app.ui.category

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Device-local store for user-created expense categories.
 *
 * Persisted as a single string in the app's encrypted prefs (no Room migration, no sync payload change).
 * Deleting a custom category never touches any expense: the expense keeps its title and its icon simply
 * falls back to the keyword engine.
 */
object CustomExpenseCategoryStore {
    private const val PREF_KEY = "custom_expense_categories_v1"

    private val _categories = MutableStateFlow<List<ExpenseCategory>>(emptyList())
    val categories: StateFlow<List<ExpenseCategory>> = _categories.asStateFlow()

    @Volatile private var prefs: SharedPreferences? = null

    /** Current snapshot; safe to call from non-composable code (icon resolver). */
    val current: List<ExpenseCategory> get() = _categories.value

    fun ensureLoaded(context: Context) {
        if (prefs != null) return
        synchronized(this) {
            if (prefs != null) return
            val p = runCatching { com.splitmate.app.data.EncryptedPrefsProvider.get(context.applicationContext) }
                .getOrNull() ?: return
            prefs = p
            _categories.value = ExpenseCategoryCatalog.decodeCustom(runCatching { p.getString(PREF_KEY, null) }.getOrNull())
        }
    }

    /** Returns the created category, or a user-facing error message. */
    fun add(context: Context, title: String, iconKey: String): Result<ExpenseCategory> {
        ensureLoaded(context)
        return when (val v = ExpenseCategoryCatalog.validateNew(title, current)) {
            is ExpenseCategoryCatalog.Validation.Error -> Result.failure(IllegalArgumentException(v.message))
            is ExpenseCategoryCatalog.Validation.Ok -> {
                val created = ExpenseCategoryCatalog.custom(v.cleanTitle, iconKey)
                persist(current + created)
                Result.success(created)
            }
        }
    }

    fun remove(context: Context, title: String) {
        ensureLoaded(context)
        persist(current.filterNot { it.title.equals(title, ignoreCase = true) })
    }

    private fun persist(list: List<ExpenseCategory>) {
        _categories.value = list
        runCatching { prefs?.edit()?.putString(PREF_KEY, ExpenseCategoryCatalog.encodeCustom(list))?.apply() }
    }
}
