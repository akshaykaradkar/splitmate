package com.splitmate.app.ui.category

object ExpenseBucketResolver {
    fun resolveRef(
        ref: String?,
        title: String,
        customCategories: List<ExpenseCategory>
    ): ExpenseCategory? {
        if (ref != null) {
            if (ExpenseCategoryRefs.isBuiltin(ref)) {
                val key = ref.removePrefix(ExpenseCategoryRefs.PREFIX_BUILTIN)
                return ExpenseCategoryCatalog.BUILT_IN.find { it.title.equals(key, ignoreCase = true) }
            } else if (ExpenseCategoryRefs.isCustom(ref)) {
                val id = ref.removePrefix(ExpenseCategoryRefs.PREFIX_CUSTOM)
                return customCategories.find { it.customId == id }
            }
        }
        return ExpenseCategoryCatalog.exactMatch(title, customCategories)
    }

    fun resolveBucket(category: ExpenseCategory?): SpendBucket {
        if (category == null) return SpendBucket.OTHER
        if (category.parentBucket != null) return category.parentBucket
        if (category.isCustom) return suggestBucket(category.title, category.iconKey)

        return when (category.group) {
            ExpenseCategoryGroup.FOOD -> SpendBucket.FOOD
            ExpenseCategoryGroup.TRANSPORT -> {
                if (category.opensPnrFlow || category.title.contains("Flight", ignoreCase = true) || category.title.contains("Train", ignoreCase = true)) {
                    SpendBucket.TRAVEL_TICKETS
                } else {
                    SpendBucket.LOCAL_TRANSPORT
                }
            }
            ExpenseCategoryGroup.STAY -> SpendBucket.STAY
            ExpenseCategoryGroup.ACTIVITIES -> SpendBucket.ACTIVITIES
            ExpenseCategoryGroup.ESSENTIALS -> SpendBucket.SHOPPING
            ExpenseCategoryGroup.OTHER, ExpenseCategoryGroup.CUSTOM -> SpendBucket.OTHER
        }
    }

    private val FOOD_WORDS = Regex("""\b(breakfast|lunch|dinner|brunch|snack|snacks|food|meal|restaurant|cafe|café|dhaba|tea|chai|coffee|drink|drinks|juice|bar|beer|biryani|thali|seafood|fish|sweets|ice ?cream|dessert)\b""", RegexOption.IGNORE_CASE)
    private val TRANSPORT_WORDS = Regex("""\b(auto|rickshaw|cab|taxi|uber|ola|rapido|bike|scooter|scooty|rental|fuel|petrol|diesel|toll|parking|ferry|boat|bus|metro|local)\b""", RegexOption.IGNORE_CASE)
    private val TICKET_WORDS = Regex("""\b(train|flight|irctc|airfare|boarding)\b""", RegexOption.IGNORE_CASE)
    private val STAY_WORDS = Regex("""\b(hotel|stay|room|resort|homestay|hostel|villa|airbnb|lodge|cottage|camp|tent)\b""", RegexOption.IGNORE_CASE)
    private val ACTIVITY_WORDS = Regex("""\b(ticket|entry|museum|fort|temple|beach|trek|paragliding|scuba|snorkel|rafting|show|movie|park|safari|tour|guide|activity|adventure|water ?sports)\b""", RegexOption.IGNORE_CASE)
    private val SHOPPING_WORDS = Regex("""\b(shopping|souvenir|gift|grocer(y|ies)|market|medicine|pharmacy|essentials|clothes|toiletries)\b""", RegexOption.IGNORE_CASE)

    /** v2.3.5: auto-suggested parent bucket for a custom category (title first, then icon key). */
    fun suggestBucket(title: String, iconKey: String = ""): SpendBucket {
        val t = title.trim()
        return when {
            FOOD_WORDS.containsMatchIn(t) -> SpendBucket.FOOD
            TICKET_WORDS.containsMatchIn(t) -> SpendBucket.TRAVEL_TICKETS
            STAY_WORDS.containsMatchIn(t) -> SpendBucket.STAY
            TRANSPORT_WORDS.containsMatchIn(t) -> SpendBucket.LOCAL_TRANSPORT
            SHOPPING_WORDS.containsMatchIn(t) -> SpendBucket.SHOPPING
            ACTIVITY_WORDS.containsMatchIn(t) -> SpendBucket.ACTIVITIES
            else -> bucketForIconKey(iconKey)
        }
    }

    private fun bucketForIconKey(iconKey: String): SpendBucket {
        val k = iconKey.lowercase()
        return when {
            listOf("restaurant", "food", "coffee", "cafe", "bar", "breakfast", "lunch", "dinner", "drink", "icecream", "bakery", "fastfood").any { k.contains(it) } -> SpendBucket.FOOD
            listOf("train", "flight").any { k.contains(it) } -> SpendBucket.TRAVEL_TICKETS
            listOf("hotel", "stay", "bed", "home", "camp").any { k.contains(it) } -> SpendBucket.STAY
            listOf("taxi", "car", "auto", "bike", "two_wheeler", "scooter", "fuel", "gas", "bus", "boat", "ferry", "parking", "toll").any { k.contains(it) } -> SpendBucket.LOCAL_TRANSPORT
            listOf("shop", "cart", "mall", "gift", "pharmacy", "medical").any { k.contains(it) } -> SpendBucket.SHOPPING
            listOf("ticket", "activity", "beach", "hike", "museum", "attraction", "pool", "surf", "kayak", "park", "movie", "celebration").any { k.contains(it) } -> SpendBucket.ACTIVITIES
            else -> SpendBucket.OTHER
        }
    }
}
