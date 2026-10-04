package com.splitmate.app.ui.category

import android.content.Context
import android.content.SharedPreferences
import com.splitmate.app.data.CloudCustomCategory
import com.splitmate.app.data.GroupLedgerExtrasStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User-created expense categories.
 *
 * v2.3.4: device-local list persisted in the app's encrypted prefs.
 * v2.3.5 (#5): categories are also shared with the active trip through the synced ledger
 * (`customCategories` in [GroupLedgerExtrasStore]). [categories] exposes the merged view:
 * the active trip's shared categories first, then this phone's own ones (deduped by id / title).
 * Creating a category while a trip is active publishes it to that trip so every member sees the
 * same name + icon + parent bucket. Deleting a shared one writes a tombstone (expenses untouched).
 */
object CustomExpenseCategoryStore {
    private const val PREF_KEY = "custom_expense_categories_v1"

    /** This phone's own categories (v2.3.4 format). */
    private val _local = MutableStateFlow<List<ExpenseCategory>>(emptyList())

    private val _categories = MutableStateFlow<List<ExpenseCategory>>(emptyList())
    /** Merged view: active trip's shared categories + device-local ones. */
    val categories: StateFlow<List<ExpenseCategory>> = _categories.asStateFlow()

    @Volatile private var prefs: SharedPreferences? = null
    @Volatile private var appContext: Context? = null
    @Volatile var activeGroupId: String = ""
        private set
    @Volatile private var listenerInstalled = false

    /** Invoked after a shared category changes locally so the caller can push the ledger. */
    @Volatile var onGroupCategoriesChanged: ((groupId: String) -> Unit)? = null

    /** Current snapshot; safe to call from non-composable code (icon resolver). */
    val current: List<ExpenseCategory> get() = _categories.value

    fun ensureLoaded(context: Context) {
        if (appContext == null) appContext = context.applicationContext ?: context
        if (prefs != null) return
        synchronized(this) {
            if (prefs != null) return
            val p = runCatching { com.splitmate.app.data.EncryptedPrefsProvider.get(context.applicationContext) }
                .getOrNull() ?: return
            prefs = p
            _local.value = ExpenseCategoryCatalog.decodeCustom(runCatching { p.getString(PREF_KEY, null) }.getOrNull())
            recompute()
        }
    }

    /** Binds the merged view to a trip (call when the active / opened trip changes). */
    fun bindGroup(context: Context?, groupId: String?) {
        if (context != null) ensureLoaded(context)
        installListener()
        val gid = groupId.orEmpty()
        if (gid.isNotBlank()) GroupLedgerExtrasStore.load(context, gid)
        activeGroupId = gid
        recompute()
    }

    private fun installListener() {
        if (listenerInstalled) return
        synchronized(this) {
            if (listenerInstalled) return
            GroupLedgerExtrasStore.addListener { gid -> if (gid == activeGroupId) recompute() }
            listenerInstalled = true
        }
    }

    /** Shared (non-deleted) categories of [groupId] as UI models. */
    fun sharedFor(groupId: String): List<ExpenseCategory> =
        if (groupId.isBlank()) emptyList()
        else GroupLedgerExtrasStore.customCategories(groupId).filter { !it.deleted }.map(::toUi)

    private fun toUi(c: CloudCustomCategory): ExpenseCategory = ExpenseCategoryCatalog.custom(
        title = c.title,
        iconKey = c.iconKey,
        parentBucket = SpendBucket.fromNameOrNull(c.parentBucket),
        customId = c.id,
        createdByPhone = c.createdByPhone,
        isGroupShared = true
    )

    /** Pure merge used by [recompute]; shared entries win over local duplicates. */
    fun mergeViews(shared: List<ExpenseCategory>, local: List<ExpenseCategory>): List<ExpenseCategory> {
        val seenIds = HashSet<String>()
        val seenTitles = HashSet<String>()
        val out = ArrayList<ExpenseCategory>()
        for (c in shared + local) {
            val id = c.customId.orEmpty()
            val t = c.title.trim().lowercase()
            if ((id.isNotEmpty() && id in seenIds) || t in seenTitles) continue
            if (id.isNotEmpty()) seenIds += id
            seenTitles += t
            out += c
        }
        return out
    }

    private fun recompute() {
        _categories.value = mergeViews(sharedFor(activeGroupId), _local.value)
    }

    /** Returns the created category, or a user-facing error message. */
    fun add(context: Context, title: String, iconKey: String, parentBucket: SpendBucket? = null): Result<ExpenseCategory> {
        ensureLoaded(context)
        return when (val v = ExpenseCategoryCatalog.validateNew(title, current)) {
            is ExpenseCategoryCatalog.Validation.Error -> Result.failure(IllegalArgumentException(v.message))
            is ExpenseCategoryCatalog.Validation.Ok -> {
                val bucket = parentBucket ?: ExpenseBucketResolver.suggestBucket(v.cleanTitle, iconKey)
                val created = ExpenseCategoryCatalog.custom(v.cleanTitle, iconKey, parentBucket = bucket)
                persistLocal(_local.value + created)
                publishToGroup(context, activeGroupId, created)
                recompute()
                Result.success(current.firstOrNull { it.customId == created.customId } ?: created)
            }
        }
    }

    /**
     * v2.3.5: if [categoryRef] points at a phone-local custom category that the trip doesn't know yet,
     * publish it so other members can render the icon / bucket. No-op for built-ins.
     */
    fun ensurePublished(context: Context?, groupId: String, categoryRef: String?) {
        if (groupId.isBlank() || !ExpenseCategoryRefs.isCustom(categoryRef)) return
        val id = categoryRef!!.removePrefix(ExpenseCategoryRefs.PREFIX_CUSTOM)
        if (GroupLedgerExtrasStore.customCategories(groupId).any { it.id == id && !it.deleted }) return
        val local = _local.value.firstOrNull { it.customId == id } ?: return
        publishToGroup(context, groupId, local)
    }

    private fun publishToGroup(context: Context?, groupId: String, cat: ExpenseCategory) {
        if (groupId.isBlank()) return
        val id = cat.customId ?: return
        GroupLedgerExtrasStore.upsertCustomCategory(
            context ?: appContext,
            groupId,
            CloudCustomCategory(
                id = id,
                title = cat.title,
                iconKey = cat.iconKey,
                parentBucket = (cat.parentBucket ?: ExpenseBucketResolver.suggestBucket(cat.title, cat.iconKey)).name,
                createdByPhone = cat.createdByPhone,
                updatedAtEpochMs = System.currentTimeMillis(),
                deleted = false
            )
        )
        onGroupCategoriesChanged?.invoke(groupId)
    }

    fun remove(context: Context, title: String) {
        ensureLoaded(context)
        val target = current.firstOrNull { it.title.equals(title, ignoreCase = true) }
        persistLocal(_local.value.filterNot { it.title.equals(title, ignoreCase = true) })
        val gid = activeGroupId
        val id = target?.customId
        if (gid.isNotBlank() && id != null &&
            GroupLedgerExtrasStore.customCategories(gid).any { it.id == id && !it.deleted }
        ) {
            GroupLedgerExtrasStore.upsertCustomCategory(
                context,
                gid,
                CloudCustomCategory(
                    id = id,
                    title = target.title,
                    iconKey = target.iconKey,
                    parentBucket = target.parentBucket?.name.orEmpty(),
                    createdByPhone = target.createdByPhone,
                    updatedAtEpochMs = System.currentTimeMillis(),
                    deleted = true
                )
            )
            onGroupCategoriesChanged?.invoke(gid)
        }
        recompute()
    }

    private fun persistLocal(list: List<ExpenseCategory>) {
        _local.value = list
        runCatching { prefs?.edit()?.putString(PREF_KEY, ExpenseCategoryCatalog.encodeCustom(list))?.apply() }
    }
}
