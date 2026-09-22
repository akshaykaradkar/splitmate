package com.splitmate.app.data

import androidx.room.Entity
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
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 3. Group Member Entity (supports Dynamic DiceBear Open-Peeps SVG seeds).
 */
@Entity(
    tableName = "group_members",
    indices = [Index("groupId")]
)
data class GroupMemberEntity(
    @PrimaryKey val memberId: String,
    val groupId: String,
    val name: String,
    val avatarSeed: String,
    val isCurrentUser: Boolean = false,
    val upiId: String = ""
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
    indices = [Index("groupId")]
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
    val syncStatus: String = "SYNCED", // "PENDING" when created offline, "SYNCED" when online
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 5. Individual Itemized / Proportional Split Allocation per Member (`0.00¢ drift`).
 */
@Entity(
    tableName = "expense_splits",
    indices = [Index("expenseId"), Index("memberId")]
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
    indices = [Index("groupId")]
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
    val isDarkTheme: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

