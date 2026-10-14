package com.splitmate.app.ui.components

import com.splitmate.app.data.ExpenseEntity

/**
 * v2.4.0 rc10: one total order for expense lists, so the same data always renders in the same order.
 *
 * Kotlin's sorts are stable, so sorting by time alone keeps whatever order the input happened to
 * arrive in for rows that share a timestamp (common for imported tickets or items logged together).
 * That made the Money / Plan / Overview lists shuffle between runs. Every list now breaks ties the
 * same way: time first, then `createdAt`, then `expenseId` (unique primary key, so the order is total).
 */
object ExpenseOrdering {
    /** Oldest first: [timeOf] ascending, then createdAt, then expenseId. */
    fun <T> chronological(expenseOf: (T) -> ExpenseEntity, timeOf: (T) -> Long): Comparator<T> =
        compareBy<T> { timeOf(it) }
            .thenBy { expenseOf(it).createdAt }
            .thenBy { expenseOf(it).expenseId }

    /** Newest first: [timeOf] descending, then createdAt descending, then expenseId (ascending, for a fixed order). */
    fun <T> newestFirst(expenseOf: (T) -> ExpenseEntity, timeOf: (T) -> Long): Comparator<T> =
        compareByDescending<T> { timeOf(it) }
            .thenByDescending { expenseOf(it).createdAt }
            .thenBy { expenseOf(it).expenseId }
}
