package com.splitmate.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 1. Locally cached exchange rates fetched from `https://api.frankfurter.dev/v1/latest`.
 */
@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey val currencyCode: String,
    val currencyName: String,
    val symbol: String,
    val rateFromBase: Double,
    val baseCurrency: String = "USD",
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 2. Expense Group Entity.
 */
@Entity(tableName = "expense_groups")
data class ExpenseGroupEntity(
    @PrimaryKey val groupId: String,
    val name: String,
    val currencyCode: String = "INR",
    val iconName: String = "Flight",
    val isDemoSeed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 3. Group Member Entity (supports Dynamic DiceBear Open-Peeps SVG seeds).
 */
@Entity(
    tableName = "group_members",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseGroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class GroupMemberEntity(
    @PrimaryKey val memberId: String,
    val groupId: String,
    val name: String,
    val avatarSeed: String,
    val isCurrentUser: Boolean = false,
    val upiId: String = "",
    val userPhone: String = "",
    val inviteStatus: String = "JOINED"
) {
    val diceBearSvgUrl: String
        get() = com.splitmate.app.ui.buildDiceBearOpenPeepsUrl(avatarSeed)
}

/**
 * 4. Expense Entity with Transaction-Locked Exchange Rate & Offline Sync Status (`PENDING` / `SYNCED`).
 *
 * - `lockedExchangeRate`: Locked at the exact instant the expense is created so historical debts remain immutable.
 * - `lockedMultiplier`: Proportional auxiliary multiplier ($m = T / B$) locked against `baseSubtotalCents`.
 * - `unassignedBaseCents`: Real-time Remainder Engine balance temporarily held on the Payer.
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseGroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GroupMemberEntity::class,
            parentColumns = ["memberId"],
            childColumns = ["payerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["payerId"]),
        Index(value = ["groupId", "createdAt"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey val expenseId: String,
    val groupId: String,
    val title: String,
    val payerId: String,
    val baseSubtotalCents: Long,
    val taxCents: Long,
    val tipCents: Long,
    val totalAmountCents: Long,
    val lockedMultiplier: Double,
    val unassignedBaseCents: Long,
    val currencyCode: String,
    val lockedExchangeRate: Double,
    val expenseCategory: String = "OTHER",
    val travelPnr: String = "",
    val providerName: String = "",
    val scheduledAtEpochMs: Long? = null,
    val syncStatus: String = "SYNCED", // "PENDING" when created offline, "SYNCED" when online
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 5. Individual Itemized / Proportional Split Allocation per Member (`0.00¢ drift`).
 */
@Entity(
    tableName = "expense_splits",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["expenseId"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GroupMemberEntity::class,
            parentColumns = ["memberId"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["expenseId"]),
        Index(value = ["memberId"]),
        Index(value = ["expenseId", "memberId"], unique = true)
    ]
)
data class ExpenseSplitEntity(
    @PrimaryKey val splitId: String,
    val expenseId: String,
    val memberId: String,
    val baseClaimedCents: Long,
    val finalOwedCents: Long,
    val plusOneCent: Boolean = false
)

/**
 * 6. Settlement Entity recording completed Greedy Minimum Cash Flow transfers.
 */
@Entity(
    tableName = "settlements",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseGroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GroupMemberEntity::class,
            parentColumns = ["memberId"],
            childColumns = ["fromMemberId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GroupMemberEntity::class,
            parentColumns = ["memberId"],
            childColumns = ["toMemberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["fromMemberId"]),
        Index(value = ["toMemberId"]),
        Index(value = ["groupId", "settledAt"])
    ]
)
data class SettlementEntity(
    @PrimaryKey val settlementId: String,
    val groupId: String,
    val fromMemberId: String,
    val fromMemberName: String,
    val toMemberId: String,
    val toMemberName: String,
    val amountCents: Long,
    val currencyCode: String,
    val lockedExchangeRate: Double,
    val syncStatus: String = "SYNCED",
    val settledAt: Long = System.currentTimeMillis()
)

/**
 * 7. User Profile Entity persisted in local Room SQLite for Onboarding & Settings state.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val profileId: String = "me",
    val name: String,
    val avatarSeed: String,
    val countryName: String,
    val currencyCode: String,
    val currencySymbol: String,
    val upiId: String,
    val userPhone: String = "",
    val isPhoneVerified: Boolean = false,
    val pinHash: String = "",
    val isDarkTheme: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 8. (v2.3.4, Room v8) Offline Trip Guide pack: gzip-compressed JSON (schema `splitmate.guide/1`).
 *
 * - Keyed by Wikidata QID and SHARED across groups (two trips to Hampi reuse one pack), so it is
 *   intentionally NOT foreign-keyed to `expense_groups` and is NOT removed by `deleteGroupCascade`.
 *   Packs are evicted by [hardExpiryEpochMs] instead (see `SplitMateDao.deleteHardExpiredGuidePacks`).
 * - `packGz` is produced by the content workstream's codec; persistence treats it as opaque bytes.
 * - Soft expiry = fetchedAt + 14 d (background refresh), hard expiry = fetchedAt + 90 d (must refetch).
 * - Holds no money data; the integer-cent ledger tables are untouched by this entity.
 */
@Entity(
    tableName = "trip_guide_pack",
    indices = [Index(value = ["hardExpiryEpochMs"])]
)
data class TripGuidePackEntity(
    @PrimaryKey val destinationQid: String,
    val schemaVersion: Int,
    val wikivoyageTitle: String?,
    val wikivoyageRevId: Long?,
    val contentHash: String,
    val packGz: ByteArray,
    val fetchedAtEpochMs: Long,
    val softExpiryEpochMs: Long,
    val hardExpiryEpochMs: Long
) {
    // ByteArray uses identity equality by default; compare by content so the entity behaves like a
    // value type (Flow distinctness, tests).
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TripGuidePackEntity) return false
        return destinationQid == other.destinationQid &&
            schemaVersion == other.schemaVersion &&
            wikivoyageTitle == other.wikivoyageTitle &&
            wikivoyageRevId == other.wikivoyageRevId &&
            contentHash == other.contentHash &&
            packGz.contentEquals(other.packGz) &&
            fetchedAtEpochMs == other.fetchedAtEpochMs &&
            softExpiryEpochMs == other.softExpiryEpochMs &&
            hardExpiryEpochMs == other.hardExpiryEpochMs
    }

    override fun hashCode(): Int {
        var result = destinationQid.hashCode()
        result = 31 * result + schemaVersion
        result = 31 * result + (wikivoyageTitle?.hashCode() ?: 0)
        result = 31 * result + (wikivoyageRevId?.hashCode() ?: 0)
        result = 31 * result + contentHash.hashCode()
        result = 31 * result + packGz.contentHashCode()
        result = 31 * result + fetchedAtEpochMs.hashCode()
        result = 31 * result + softExpiryEpochMs.hashCode()
        result = 31 * result + hardExpiryEpochMs.hashCode()
        return result
    }
}

/**
 * 9. (v2.3.4, Room v8) Per-group synced trip plan manifest ("sync intents, recompute derivations").
 *
 * - `manifestJson`: the merged group manifest (compact JSON, see `PlanManifestCodec`). Its `stay`
 *   is only ever a stay that somebody explicitly shared.
 * - `stayLocalJson`: this device's OWN stay pin. LOCAL-ONLY: it is never synced unless
 *   [shareStay] is true (decision #14: sharing is off by default, opt-in per trip).
 * - `lastSyncMessageId`: last ntfy message id consumed from the group's plan topic (`since=` cursor).
 * - `pendingPush`: local changes not yet published to the plan topic.
 * - Cascade-deleted with its group (FK ON DELETE CASCADE + explicit delete in `deleteGroupCascade`).
 */
@Entity(
    tableName = "trip_plan_manifest",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseGroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["destinationQid"])]
)
data class TripPlanManifestEntity(
    @PrimaryKey val groupId: String,
    val destinationQid: String?,
    val manifestJson: String,
    val lastSyncMessageId: String?,
    val stayLocalJson: String?,
    @ColumnInfo(defaultValue = "0") val shareStay: Boolean = false,
    val updatedAtEpochMs: Long,
    @ColumnInfo(defaultValue = "0") val pendingPush: Boolean = false
)

