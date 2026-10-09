package com.splitmate.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitMateDao {

    // --- Currency Rates Cache ---
    @Query("SELECT * FROM currency_rates ORDER BY currencyCode ASC")
    fun observeCurrencyRates(): Flow<List<CurrencyRateEntity>>

    @Query("SELECT * FROM currency_rates WHERE currencyCode = :code LIMIT 1")
    suspend fun getCurrencyRate(code: String): CurrencyRateEntity?

    @Upsert
    suspend fun upsertCurrencyRates(rates: List<CurrencyRateEntity>)

    // --- Groups & Members ---
    @Query("SELECT * FROM expense_groups ORDER BY createdAt DESC, groupId ASC")
    fun observeGroups(): Flow<List<ExpenseGroupEntity>>

    @Query("SELECT * FROM expense_groups ORDER BY createdAt DESC, groupId ASC")
    suspend fun getAllGroups(): List<ExpenseGroupEntity>

    @Query("SELECT * FROM expense_groups WHERE groupId = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): ExpenseGroupEntity?

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    fun observeGroupMembers(groupId: String): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members")
    fun observeAllMembers(): Flow<List<GroupMemberEntity>>

    @Upsert
    suspend fun insertGroup(group: ExpenseGroupEntity)

    @Query("UPDATE expense_groups SET name = :newName, iconName = :newIconName WHERE groupId = :groupId")
    suspend fun updateGroupDetails(groupId: String, newName: String, newIconName: String)

    @Upsert
    suspend fun insertMembers(members: List<GroupMemberEntity>)

    @Query("UPDATE group_members SET name = :name, upiId = :upiId, avatarSeed = :avatarSeed WHERE memberId = :memberId")
    suspend fun updateMemberProfile(memberId: String, name: String, upiId: String, avatarSeed: String)

    @Query("UPDATE group_members SET name = :name, userPhone = :userPhone, inviteStatus = :inviteStatus WHERE memberId = :memberId")
    suspend fun updateMemberPhoneAndInviteStatus(
        memberId: String,
        name: String,
        userPhone: String,
        inviteStatus: String
    )

    @Query("DELETE FROM group_members WHERE memberId = :memberId")
    suspend fun deleteMemberById(memberId: String)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun deleteMembersForGroup(groupId: String)

    // --- Expenses & Splits ---
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC, expenseId DESC")
    fun observeAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expense_splits")
    fun observeAllSplits(): Flow<List<ExpenseSplitEntity>>

    @Upsert
    suspend fun insertExpense(expense: ExpenseEntity)

    @Upsert
    suspend fun insertExpenseSplits(splits: List<ExpenseSplitEntity>)

    @Transaction
    suspend fun replaceExpenseSplits(
        expenseId: String,
        splits: List<ExpenseSplitEntity>
    ) {
        deleteSplitsForExpense(expenseId)
        insertExpenseSplits(splits)
    }

    @Transaction
    suspend fun insertExpenseWithSplits(
        expense: ExpenseEntity,
        splits: List<ExpenseSplitEntity>
    ) {
        insertExpense(expense)
        replaceExpenseSplits(expense.expenseId, splits)
    }

    /**
     * v2.3.6 P0 (RCA Bike Rentals): runs [block] in ONE Room transaction. The cloud sync uses it to
     * re-read the current rows and write the merged ledger atomically, so an edit saved while the
     * sync was merging can never be half-overwritten (old total with new shares).
     */
    @Transaction
    suspend fun runInLedgerTransaction(block: suspend () -> Unit) {
        block()
    }

    @Query("DELETE FROM expenses WHERE expenseId = :expenseId")
    suspend fun deleteExpense(expenseId: String)

    @Query("DELETE FROM expenses WHERE groupId = :groupId")
    suspend fun deleteExpensesForGroup(groupId: String)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: String)

    @Query("DELETE FROM expense_groups WHERE groupId = :groupId")
    suspend fun deleteGroupById(groupId: String)

    @Query("DELETE FROM settlements WHERE groupId = :groupId")
    suspend fun deleteSettlementsForGroup(groupId: String)

    @Transaction
    suspend fun deleteGroupCascade(groupId: String) {
        val exps = getExpensesForGroup(groupId)
        exps.forEach { deleteSplitsForExpense(it.expenseId) }
        deleteExpensesForGroup(groupId)
        deleteSettlementsForGroup(groupId)
        deleteMembersForGroup(groupId)
        // v2.3.4: the group's plan manifest (incl. its local-only stay) goes with the group. Guide
        // packs are shared across groups and are evicted by hard expiry instead.
        deleteTripPlanManifestForGroup(groupId)
        deleteExpenseRevisionsForGroup(groupId)
        deleteGroupById(groupId)
    }

    // --- Settlements ---
    @Query("SELECT * FROM settlements ORDER BY settledAt DESC")
    fun observeAllSettlements(): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY createdAt DESC, expenseId DESC")
    suspend fun getExpensesForGroup(groupId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expense_splits WHERE expenseId IN (SELECT expenseId FROM expenses WHERE groupId = :groupId)")
    suspend fun getSplitsForGroup(groupId: String): List<ExpenseSplitEntity>

    @Query("SELECT * FROM settlements WHERE groupId = :groupId")
    suspend fun getSettlementsForGroup(groupId: String): List<SettlementEntity>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    suspend fun getMembersForGroup(groupId: String): List<GroupMemberEntity>

    @Upsert
    suspend fun insertSettlement(settlement: SettlementEntity)

    @Query("DELETE FROM settlements WHERE settlementId = :settlementId")
    suspend fun deleteSettlementById(settlementId: String)

    @Query("DELETE FROM settlements WHERE groupId = :groupId AND (fromMemberId = :memberId OR toMemberId = :memberId)")
    suspend fun deleteSettlementsForMember(groupId: String, memberId: String)

    @Query("UPDATE expenses SET payerId = :newPayerId WHERE groupId = :groupId AND payerId = :oldPayerId")
    suspend fun reassignExpensePayer(groupId: String, oldPayerId: String, newPayerId: String)

    @Query("UPDATE expenses SET syncStatus = 'SYNCED' WHERE syncStatus = 'PENDING'")
    suspend fun markPendingExpensesSynced()

    // --- User Profile & Onboarding ---
    @Query("SELECT * FROM user_profile WHERE profileId = 'me' LIMIT 1")
    fun observeUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE profileId = 'me' LIMIT 1")
    suspend fun getUserProfile(): UserProfileEntity?

    @Upsert
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
        // v2.3.4: plan manifests are children of groups (FK CASCADE); clear them explicitly too.
        deleteAllTripPlanManifests()
        deleteAllGroups()
        deleteAllExpenseRevisions()
    }

    // --- v2.4.0 P1 + P4: per-expense version history (local only) ---
    @Query("SELECT * FROM expense_revisions WHERE groupId = :groupId ORDER BY observedAtEpochMs ASC, rowVersion ASC")
    suspend fun getExpenseRevisionsForGroup(groupId: String): List<ExpenseRevisionEntity>

    @Query("SELECT * FROM expense_revisions WHERE expenseId = :expenseId ORDER BY observedAtEpochMs DESC, rowVersion DESC")
    fun observeExpenseRevisions(expenseId: String): Flow<List<ExpenseRevisionEntity>>

    @Upsert
    suspend fun insertExpenseRevisions(revisions: List<ExpenseRevisionEntity>)

    /**
     * Keeps only the newest [keep] change entries of an expense. Older-copy fingerprints ("seed",
     * "superseded") are kept separately ([keepCopies]) so a stale upload stays recognisable (review F3).
     */
    @Query(
        "DELETE FROM expense_revisions WHERE expenseId = :expenseId AND kind NOT IN ('seed', 'superseded') AND revisionId NOT IN " +
            "(SELECT revisionId FROM expense_revisions WHERE expenseId = :expenseId AND kind NOT IN ('seed', 'superseded') " +
            "ORDER BY observedAtEpochMs DESC, rowVersion DESC LIMIT :keep)"
    )
    suspend fun pruneExpenseRevisionChanges(expenseId: String, keep: Int)

    @Query(
        "DELETE FROM expense_revisions WHERE expenseId = :expenseId AND kind IN ('seed', 'superseded') AND revisionId NOT IN " +
            "(SELECT revisionId FROM expense_revisions WHERE expenseId = :expenseId AND kind IN ('seed', 'superseded') " +
            "ORDER BY observedAtEpochMs DESC, rowVersion DESC LIMIT :keepCopies)"
    )
    suspend fun pruneExpenseRevisionCopies(expenseId: String, keepCopies: Int)

    @Transaction
    suspend fun pruneExpenseRevisions(expenseId: String, keep: Int, keepCopies: Int = 64) {
        pruneExpenseRevisionChanges(expenseId, keep)
        pruneExpenseRevisionCopies(expenseId, keepCopies)
    }

    @Query("DELETE FROM expense_revisions WHERE groupId = :groupId")
    suspend fun deleteExpenseRevisionsForGroup(groupId: String)

    @Query("DELETE FROM expense_revisions")
    suspend fun deleteAllExpenseRevisions()

    // --- v2.3.4 Trip Guide packs (shared across groups; evicted by hard expiry) ---
    @Query("SELECT * FROM trip_guide_pack WHERE destinationQid = :destinationQid LIMIT 1")
    suspend fun getGuidePack(destinationQid: String): TripGuidePackEntity?

    @Query("SELECT * FROM trip_guide_pack WHERE destinationQid = :destinationQid LIMIT 1")
    fun observeGuidePack(destinationQid: String): Flow<TripGuidePackEntity?>

    @Upsert
    suspend fun upsertGuidePack(pack: TripGuidePackEntity)

    @Query("DELETE FROM trip_guide_pack WHERE destinationQid = :destinationQid")
    suspend fun deleteGuidePack(destinationQid: String)

    @Query("DELETE FROM trip_guide_pack WHERE hardExpiryEpochMs <= :nowEpochMs")
    suspend fun deleteHardExpiredGuidePacks(nowEpochMs: Long): Int

    // --- v2.3.4 Trip plan manifests (one per group; cascade-deleted with the group) ---
    @Query("SELECT * FROM trip_plan_manifest WHERE groupId = :groupId LIMIT 1")
    suspend fun getTripPlanManifest(groupId: String): TripPlanManifestEntity?

    @Query("SELECT * FROM trip_plan_manifest WHERE groupId = :groupId LIMIT 1")
    fun observeTripPlanManifest(groupId: String): Flow<TripPlanManifestEntity?>

    @Query("SELECT * FROM trip_plan_manifest WHERE pendingPush = 1 ORDER BY updatedAtEpochMs ASC, groupId ASC")
    suspend fun getPendingTripPlanManifests(): List<TripPlanManifestEntity>

    @Upsert
    suspend fun upsertTripPlanManifest(manifest: TripPlanManifestEntity)

    @Query("DELETE FROM trip_plan_manifest WHERE groupId = :groupId")
    suspend fun deleteTripPlanManifestForGroup(groupId: String)

    @Query("DELETE FROM trip_plan_manifest")
    suspend fun deleteAllTripPlanManifests()
}

