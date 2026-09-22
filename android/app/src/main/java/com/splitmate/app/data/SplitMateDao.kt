package com.splitmate.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitMateDao {

    // --- Currency Rates Cache ---
    @Query("SELECT * FROM currency_rates ORDER BY currencyCode ASC")
    fun observeCurrencyRates(): Flow<List<CurrencyRateEntity>>

    @Query("SELECT * FROM currency_rates WHERE currencyCode = :code LIMIT 1")
    suspend fun getCurrencyRate(code: String): CurrencyRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCurrencyRates(rates: List<CurrencyRateEntity>)

    // --- Groups & Members ---
    @Query("SELECT * FROM expense_groups ORDER BY createdAt DESC")
    fun observeGroups(): Flow<List<ExpenseGroupEntity>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    fun observeGroupMembers(groupId: String): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members")
    fun observeAllMembers(): Flow<List<GroupMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: ExpenseGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<GroupMemberEntity>)

    @Query("UPDATE group_members SET name = :name, upiId = :upiId, avatarSeed = :avatarSeed WHERE memberId = :memberId")
    suspend fun updateMemberProfile(memberId: String, name: String, upiId: String, avatarSeed: String)

    // --- Expenses & Splits ---
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun observeAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expense_splits")
    fun observeAllSplits(): Flow<List<ExpenseSplitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseSplits(splits: List<ExpenseSplitEntity>)

    @Transaction
    suspend fun insertExpenseWithSplits(
        expense: ExpenseEntity,
        splits: List<ExpenseSplitEntity>
    ) {
        insertExpense(expense)
        insertExpenseSplits(splits)
    }

    @Query("DELETE FROM expenses WHERE expenseId = :expenseId")
    suspend fun deleteExpense(expenseId: String)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: String)

    // --- Settlements ---
    @Query("SELECT * FROM settlements ORDER BY settledAt DESC")
    fun observeAllSettlements(): Flow<List<SettlementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity)

    @Query("DELETE FROM settlements WHERE settlementId = :settlementId")
    suspend fun deleteSettlementById(settlementId: String)

    @Query("UPDATE expenses SET syncStatus = 'SYNCED' WHERE syncStatus = 'PENDING'")
    suspend fun markPendingExpensesSynced()

    // --- User Profile & Onboarding ---
    @Query("SELECT * FROM user_profile WHERE profileId = 'me' LIMIT 1")
    fun observeUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE profileId = 'me' LIMIT 1")
    suspend fun getUserProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profile")
    suspend fun deleteUserProfile()

    @Query("DELETE FROM expense_groups")
    suspend fun deleteAllGroups()

    @Query("DELETE FROM group_members")
    suspend fun deleteAllMembers()

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    @Query("DELETE FROM expense_splits")
    suspend fun deleteAllSplits()

    @Query("DELETE FROM settlements")
    suspend fun deleteAllSettlements()

    @Transaction
    suspend fun clearAllLedgerData() {
        deleteAllSplits()
        deleteAllExpenses()
        deleteAllSettlements()
        deleteAllMembers()
        deleteAllGroups()
    }
}

