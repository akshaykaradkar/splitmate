package com.splitmate.app.ui.category

import java.util.Locale

/**
 * v2.3.4 Expense Category Catalog (pure Kotlin, no Android / Compose dependency so it is unit-testable).
 *
 * DESIGN CONTRACT (non-destructive):
 *  - A "category" in SplitMate has always been the expense *title* string (`ExpenseEntity.title`).
 *    This catalog does NOT add a Room column, does NOT migrate data and does NOT touch
 *    `ExpenseEntity.expenseCategory` or any money math. Already-logged expenses keep their exact title.
 *  - The 7 legacy preset titles are kept byte-for-byte (see [LEGACY_PRESET_TITLES]) so every existing
 *    expense, chip-selection check and keyword classifier keeps behaving identically.
 *  - New built-ins and user-created categories are only ever *added*. An icon is resolved by an exact
 *    (case-insensitive) title match first, then the pre-existing keyword engine as fallback.
 */
enum class ExpenseCategoryGroup(val label: String) {
    FOOD("Food & Drinks"),
    TRANSPORT("Getting Around"),
    STAY("Stay"),
    ACTIVITIES("Fun & Activities"),
    ESSENTIALS("Shopping & Essentials"),
    OTHER("Other"),
    CUSTOM("Your Categories")
}

/** Badge tone family; mapped to Buckwheat colour pairs in `ExpenseCategoryIcons.kt`. */
enum class ExpenseCategoryTone { SAGE, PEACH, PERIWINKLE, AMBER, MINT, ROSE }

data class ExpenseCategory(
    /** Exact string written into `ExpenseEntity.title` when picked. */
    val title: String,
    /** Short label for compact chips. */
    val shortLabel: String,
    /** Stable key into [ExpenseCategoryIconKeys]; persisted for custom categories. */
    val iconKey: String,
    val group: ExpenseCategoryGroup,
    val isCustom: Boolean = false,
    /** Tapping this entry launches the IRCTC PNR direct-split flow instead of setting a title. */
    val opensPnrFlow: Boolean = false,
    /** v2.3.5: stable id for custom categories (`ExpenseEntity.categoryRef = "custom:<id>"`). */
    val customId: String? = null,
    /** v2.3.5: parent spend bucket chosen for a custom category (null = auto-suggest). */
    val parentBucket: SpendBucket? = null,
    /** v2.3.5: 10-digit phone of the member who created this custom category (blank = this device). */
    val createdByPhone: String = "",
    /** v2.3.5: true when the category lives in the active trip's synced ledger. */
    val isGroupShared: Boolean = false
) {
    val tone: ExpenseCategoryTone
        get() = when (group) {
            ExpenseCategoryGroup.FOOD -> ExpenseCategoryTone.PEACH
            ExpenseCategoryGroup.TRANSPORT -> ExpenseCategoryTone.AMBER
            ExpenseCategoryGroup.STAY -> ExpenseCategoryTone.PERIWINKLE
            ExpenseCategoryGroup.ACTIVITIES -> ExpenseCategoryTone.ROSE
            ExpenseCategoryGroup.ESSENTIALS -> ExpenseCategoryTone.MINT
            ExpenseCategoryGroup.OTHER -> ExpenseCategoryTone.PEACH
            ExpenseCategoryGroup.CUSTOM -> ExpenseCategoryTone.SAGE
        }
}

/** Every icon key the app knows. Custom categories may pick any of these. */
object ExpenseCategoryIconKeys {
    val ALL: List<String> = listOf(
        // food & drink
        "restaurant", "breakfast", "lunch", "dinner", "cafe", "coffee", "ramen", "pizza",
        "bakery", "icecream", "fastfood", "water", "bar", "liquor",
        // transport
        "train", "flight", "taxi", "auto_rickshaw", "two_wheeler", "scooter", "bicycle", "car_rental",
        "fuel", "parking", "toll", "bus", "metro", "boat", "shuttle", "walk",
        // stay
        "hotel", "house", "cottage", "bed", "camping",
        // activities
        "ticket", "attractions", "tour", "museum", "temple", "hiking", "kayaking", "paragliding",
        "surfing", "scuba", "spa", "movie", "beach", "camera", "celebration", "sports",
        // essentials
        "groceries", "shopping", "gift", "pharmacy", "hospital", "soap", "sim", "wifi", "laundry",
        "luggage",
        // other
        "tips", "atm", "payments", "savings", "work", "school", "pets", "receipt", "more"
    )

    const val DEFAULT_CUSTOM = "receipt"

    fun isKnown(key: String): Boolean = key in ALL
}

object ExpenseCategoryCatalog {

    /** The 7 preset titles that shipped before v2.3.4. Never rename or remove these. */
    val LEGACY_PRESET_TITLES: List<String> = listOf(
        "Train / PNR Ticket",
        "Dinner & Food",
        "Travel & Flight",
        "Stay & Hotel",
        "Cab & Local",
        "Groceries",
        "Party & Drinks"
    )

    const val MAX_CUSTOM_CATEGORIES = 40
    const val MAX_TITLE_LENGTH = 28

    private fun b(title: String, short: String, icon: String, group: ExpenseCategoryGroup, pnr: Boolean = false) =
        ExpenseCategory(title = title, shortLabel = short, iconKey = icon, group = group, opensPnrFlow = pnr)

    /** Legacy presets first (unchanged order + titles), then the v2.3.4 additions from user interviews. */
    val BUILT_IN: List<ExpenseCategory> = listOf(
        // ---- legacy (pre-v2.3.4) — exact titles preserved ----
        b("Train / PNR Ticket", "Train / PNR", "train", ExpenseCategoryGroup.TRANSPORT, pnr = true),
        b("Dinner & Food", "Food", "restaurant", ExpenseCategoryGroup.FOOD),
        b("Travel & Flight", "Flight", "flight", ExpenseCategoryGroup.TRANSPORT),
        b("Stay & Hotel", "Stay", "hotel", ExpenseCategoryGroup.STAY),
        b("Cab & Local", "Cab", "taxi", ExpenseCategoryGroup.TRANSPORT),
        b("Groceries", "Groceries", "groceries", ExpenseCategoryGroup.ESSENTIALS),
        b("Party & Drinks", "Drinks", "bar", ExpenseCategoryGroup.FOOD),

        // ---- v2.3.4 Food & Drinks ----
        b("Breakfast", "Breakfast", "breakfast", ExpenseCategoryGroup.FOOD),
        b("Lunch", "Lunch", "lunch", ExpenseCategoryGroup.FOOD),
        b("Dinner", "Dinner", "dinner", ExpenseCategoryGroup.FOOD),
        b("Snacks & Chai", "Snacks", "cafe", ExpenseCategoryGroup.FOOD),
        b("Coffee", "Coffee", "coffee", ExpenseCategoryGroup.FOOD),
        b("Street Food", "Street Food", "fastfood", ExpenseCategoryGroup.FOOD),
        b("Desserts", "Desserts", "icecream", ExpenseCategoryGroup.FOOD),
        b("Water & Beverages", "Water", "water", ExpenseCategoryGroup.FOOD),

        // ---- v2.3.4 Getting Around ----
        b("Auto Rickshaw", "Auto", "auto_rickshaw", ExpenseCategoryGroup.TRANSPORT),
        b("Bike Rental", "Bike Rental", "two_wheeler", ExpenseCategoryGroup.TRANSPORT),
        b("Scooter Rental", "Scooter", "scooter", ExpenseCategoryGroup.TRANSPORT),
        b("Car Rental", "Car Rental", "car_rental", ExpenseCategoryGroup.TRANSPORT),
        b("Fuel & Petrol", "Fuel", "fuel", ExpenseCategoryGroup.TRANSPORT),
        b("Tolls & Parking", "Tolls", "parking", ExpenseCategoryGroup.TRANSPORT),
        b("Bus", "Bus", "bus", ExpenseCategoryGroup.TRANSPORT),
        b("Metro", "Metro", "metro", ExpenseCategoryGroup.TRANSPORT),
        b("Ferry & Boat", "Ferry", "boat", ExpenseCategoryGroup.TRANSPORT),
        b("Bicycle", "Bicycle", "bicycle", ExpenseCategoryGroup.TRANSPORT),

        // ---- v2.3.4 Stay ----
        b("Homestay", "Homestay", "cottage", ExpenseCategoryGroup.STAY),
        b("Hostel", "Hostel", "bed", ExpenseCategoryGroup.STAY),
        b("Camping", "Camping", "camping", ExpenseCategoryGroup.STAY),

        // ---- v2.3.4 Fun & Activities ----
        b("Entry Tickets", "Tickets", "ticket", ExpenseCategoryGroup.ACTIVITIES),
        b("Sightseeing", "Sightseeing", "attractions", ExpenseCategoryGroup.ACTIVITIES),
        b("Tour & Guide", "Tour Guide", "tour", ExpenseCategoryGroup.ACTIVITIES),
        b("Adventure Sports", "Adventure", "paragliding", ExpenseCategoryGroup.ACTIVITIES),
        b("Water Sports", "Water Sports", "kayaking", ExpenseCategoryGroup.ACTIVITIES),
        b("Spa & Wellness", "Spa", "spa", ExpenseCategoryGroup.ACTIVITIES),
        b("Movies & Shows", "Movies", "movie", ExpenseCategoryGroup.ACTIVITIES),

        // ---- v2.3.4 Shopping & Essentials ----
        b("Shopping", "Shopping", "shopping", ExpenseCategoryGroup.ESSENTIALS),
        b("Souvenirs & Gifts", "Gifts", "gift", ExpenseCategoryGroup.ESSENTIALS),
        b("Medicines", "Medicines", "pharmacy", ExpenseCategoryGroup.ESSENTIALS),
        b("Toiletries", "Toiletries", "soap", ExpenseCategoryGroup.ESSENTIALS),
        b("SIM & Recharge", "SIM", "sim", ExpenseCategoryGroup.ESSENTIALS),
        b("Laundry", "Laundry", "laundry", ExpenseCategoryGroup.ESSENTIALS),

        // ---- v2.3.4 Other ----
        b("Tips", "Tips", "tips", ExpenseCategoryGroup.OTHER),
        b("Cash Withdrawal", "Cash", "atm", ExpenseCategoryGroup.OTHER),
        b("Fees & Charges", "Fees", "payments", ExpenseCategoryGroup.OTHER),
        b("Miscellaneous", "Misc", "more", ExpenseCategoryGroup.OTHER)
    )

    private fun norm(s: String) = s.trim().lowercase(Locale.US)

    private val builtInByTitle: Map<String, ExpenseCategory> = BUILT_IN.associateBy { norm(it.title) }

    /** Built-ins + customs, legacy first. Customs never shadow a built-in title. */
    fun all(custom: List<ExpenseCategory>): List<ExpenseCategory> =
        BUILT_IN + custom.filter { norm(it.title) !in builtInByTitle }

    /**
     * Exact (trimmed, case-insensitive) match of an expense title to a category, or null.
     * Legacy presets are deliberately excluded so their icon/tone keep coming from the original
     * keyword engine — zero visual change for already-logged expenses.
     */
    fun exactMatch(title: String, custom: List<ExpenseCategory>): ExpenseCategory? {
        val key = norm(title)
        if (key.isEmpty()) return null
        val builtIn = builtInByTitle[key]
        if (builtIn != null) {
            return if (LEGACY_PRESET_TITLES.any { norm(it) == key }) null else builtIn
        }
        return custom.firstOrNull { norm(it.title) == key }
    }

    /** Case-insensitive search across title, short label and group label. */
    fun search(query: String, custom: List<ExpenseCategory>): List<ExpenseCategory> {
        val q = norm(query)
        val everything = all(custom)
        if (q.isEmpty()) return everything
        return everything.filter {
            norm(it.title).contains(q) || norm(it.shortLabel).contains(q) || norm(it.group.label).contains(q)
        }
    }

    // ------------------------------------------------------------------------------------------
    // Custom category validation + persistence codec
    // ------------------------------------------------------------------------------------------

    sealed class Validation {
        data class Ok(val cleanTitle: String) : Validation()
        data class Error(val message: String) : Validation()
    }

    /** Strips control/separator characters and collapses whitespace. */
    fun sanitizeTitle(raw: String): String =
        raw.replace(Regex("[\\p{Cntrl}|]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(MAX_TITLE_LENGTH)
            .trim()

    fun validateNew(raw: String, custom: List<ExpenseCategory>): Validation {
        val clean = sanitizeTitle(raw)
        if (clean.length < 2) return Validation.Error("Name must be at least 2 characters")
        val key = norm(clean)
        if (builtInByTitle.containsKey(key)) return Validation.Error("\"$clean\" already exists")
        if (custom.any { norm(it.title) == key }) return Validation.Error("You already created \"$clean\"")
        if (custom.size >= MAX_CUSTOM_CATEGORIES) {
            return Validation.Error("You can keep up to $MAX_CUSTOM_CATEGORIES custom categories")
        }
        return Validation.Ok(clean)
    }

    fun custom(
        title: String,
        iconKey: String,
        parentBucket: SpendBucket? = null,
        customId: String? = null,
        createdByPhone: String = "",
        isGroupShared: Boolean = false
    ): ExpenseCategory {
        val clean = sanitizeTitle(title)
        return ExpenseCategory(
            title = clean,
            shortLabel = clean,
            iconKey = if (ExpenseCategoryIconKeys.isKnown(iconKey)) iconKey else ExpenseCategoryIconKeys.DEFAULT_CUSTOM,
            group = ExpenseCategoryGroup.CUSTOM,
            isCustom = true,
            customId = customId?.takeIf { it.isNotBlank() } ?: ExpenseCategoryRefs.customIdForTitle(clean),
            parentBucket = parentBucket,
            createdByPhone = createdByPhone,
            isGroupShared = isGroupShared
        )
    }

    internal val ICON_HINTS: List<Pair<List<String>, String>> = listOf(
        listOf("breakfast") to "breakfast",
        listOf("lunch") to "lunch",
        listOf("dinner") to "dinner",
        listOf("coffee") to "coffee",
        listOf("chai", "tea", "snack") to "cafe",
        listOf("pizza") to "pizza",
        listOf("noodle", "ramen", "momo") to "ramen",
        listOf("cake", "bakery", "bread") to "bakery",
        listOf("ice cream", "icecream", "dessert", "sweet") to "icecream",
        listOf("water", "juice", "beverage") to "water",
        listOf("beer", "bar", "pub", "drink") to "bar",
        listOf("wine", "liquor", "alcohol") to "liquor",
        listOf("food", "meal", "restaurant", "thali") to "restaurant",
        listOf("auto", "rickshaw", "tuk") to "auto_rickshaw",
        listOf("bike", "motorcycle", "royal enfield") to "two_wheeler",
        listOf("scooter", "scooty", "activa") to "scooter",
        listOf("cycle", "bicycle") to "bicycle",
        listOf("fuel", "petrol", "diesel", "gas", "cng") to "fuel",
        listOf("parking") to "parking",
        listOf("toll", "fastag") to "toll",
        listOf("bus") to "bus",
        listOf("metro", "subway", "local train") to "metro",
        listOf("train", "rail") to "train",
        listOf("flight", "airport", "airline") to "flight",
        listOf("ferry", "boat", "cruise", "shikara") to "boat",
        listOf("cab", "taxi", "uber") to "taxi",
        listOf("rent", "car") to "car_rental",
        listOf("hotel", "resort") to "hotel",
        listOf("homestay", "villa", "cottage") to "cottage",
        listOf("hostel", "dorm") to "bed",
        listOf("camp", "tent") to "camping",
        listOf("ticket", "entry", "pass") to "ticket",
        listOf("museum", "fort", "palace") to "museum",
        listOf("temple", "darshan", "puja") to "temple",
        listOf("trek", "hike") to "hiking",
        listOf("scuba", "snorkel", "dive") to "scuba",
        listOf("surf") to "surfing",
        listOf("kayak", "raft") to "kayaking",
        listOf("paraglid", "bungee", "zipline") to "paragliding",
        listOf("spa", "massage") to "spa",
        listOf("movie", "cinema", "show") to "movie",
        listOf("beach") to "beach",
        listOf("photo", "camera") to "camera",
        listOf("party", "birthday", "celebrat") to "celebration",
        listOf("sport", "game", "match") to "sports",
        listOf("grocer", "mart") to "groceries",
        listOf("gift", "souvenir") to "gift",
        listOf("shop", "clothes") to "shopping",
        listOf("medic", "pharma", "tablet") to "pharmacy",
        listOf("doctor", "hospital", "clinic") to "hospital",
        listOf("soap", "toiletr", "shampoo") to "soap",
        listOf("sim", "recharge", "data pack") to "sim",
        listOf("wifi", "internet") to "wifi",
        listOf("laundry", "wash") to "laundry",
        listOf("luggage", "porter", "bag") to "luggage",
        listOf("tip") to "tips",
        listOf("atm", "cash") to "atm",
        listOf("fee", "charge", "fine") to "payments",
        listOf("pet", "dog") to "pets"
    )

    /** Best-guess icon for a name the user is typing; falls back to the receipt icon. */
    fun suggestIconKey(name: String): String {
        val lower = norm(name)
        if (lower.isEmpty()) return ExpenseCategoryIconKeys.DEFAULT_CUSTOM
        return ICON_HINTS.firstOrNull { (words, _) -> words.any { lower.contains(it) } }?.second
            ?: ExpenseCategoryIconKeys.DEFAULT_CUSTOM
    }

    /** One category per line: `iconKey|title`. Titles are sanitised so they can't contain `|` or newlines. */
    fun encodeCustom(list: List<ExpenseCategory>): String =
        list.filter { it.isCustom }.joinToString("\n") { "${it.iconKey}|${sanitizeTitle(it.title)}" }

    /** Tolerant decoder: skips blank/garbled lines, dedupes, drops built-in collisions, caps the count. */
    fun decodeCustom(encoded: String?): List<ExpenseCategory> {
        if (encoded.isNullOrBlank()) return emptyList()
        val seen = HashSet<String>()
        val out = ArrayList<ExpenseCategory>()
        for (line in encoded.lineSequence()) {
            val sep = line.indexOf('|')
            if (sep <= 0) continue
            val cat = custom(line.substring(sep + 1), line.substring(0, sep).trim())
            val key = norm(cat.title)
            if (cat.title.length < 2 || key in builtInByTitle || !seen.add(key)) continue
            out += cat
            if (out.size >= MAX_CUSTOM_CATEGORIES) break
        }
        return out
    }
}
