package com.splitmate.app.ui.category

import java.security.MessageDigest

enum class SpendBucket(val label: String) {
    FOOD("Food & Drinks"),
    LOCAL_TRANSPORT("Local Transport"),
    STAY("Stay"),
    TRAVEL_TICKETS("Travel Tickets"),
    ACTIVITIES("Activities"),
    SHOPPING("Shopping"),
    OTHER("Other");
    
    companion object {
        fun fromNameOrNull(name: String?): SpendBucket? = 
            values().find { it.name.equals(name, ignoreCase = true) }
    }
}

object ExpenseCategoryRefs {
    const val PREFIX_BUILTIN = "builtin:"
    const val PREFIX_CUSTOM = "custom:"

    fun builtin(title: String): String = "$PREFIX_BUILTIN$title"
    fun custom(id: String): String = "$PREFIX_CUSTOM$id"

    fun isCustom(ref: String?): Boolean = ref?.startsWith(PREFIX_CUSTOM) == true
    fun isBuiltin(ref: String?): Boolean = ref?.startsWith(PREFIX_BUILTIN) == true

    /** categoryRef to persist when [cat] is picked (custom id, or built-in title; PNR flow = null). */
    fun refFor(cat: ExpenseCategory): String? = when {
        cat.opensPnrFlow -> null
        cat.isCustom -> cat.customId?.takeIf { it.isNotBlank() }?.let(::custom)
        else -> builtin(cat.title)
    }

    /**
     * categoryRef for a title chosen in Quick Expense: catalog / custom exact match only. Legacy preset
     * titles ("Dinner & Food" ...) and free text return null and keep resolving from the title as before.
     */
    fun refForTitle(title: String, custom: List<ExpenseCategory>): String? =
        ExpenseCategoryCatalog.exactMatch(title, custom)?.let(::refFor)

    fun customIdForTitle(cleanTitle: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(cleanTitle.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }.take(12)
        return "c_$hex"
    }
}
