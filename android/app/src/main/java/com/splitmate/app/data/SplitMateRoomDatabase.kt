package com.splitmate.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CurrencyRateEntity::class,
        ExpenseGroupEntity::class,
        GroupMemberEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
        SettlementEntity::class,
        UserProfileEntity::class,
        TripGuidePackEntity::class,
        TripPlanManifestEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class SplitMateRoomDatabase : RoomDatabase() {

    abstract fun dao(): SplitMateDao

    companion object {
        @Volatile
        private var INSTANCE: SplitMateRoomDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys=OFF")

                // 1. Clean up all 8 potential relational orphan paths before enforcing 6 Foreign Keys
                db.execSQL("DELETE FROM `group_members` WHERE `groupId` NOT IN (SELECT `groupId` FROM `expense_groups`)")
                db.execSQL("DELETE FROM `expenses` WHERE `groupId` NOT IN (SELECT `groupId` FROM `expense_groups`) OR `payerId` NOT IN (SELECT `memberId` FROM `group_members`)")
                db.execSQL("DELETE FROM `expense_splits` WHERE `expenseId` NOT IN (SELECT `expenseId` FROM `expenses`) OR `memberId` NOT IN (SELECT `memberId` FROM `group_members`)")
                db.execSQL("DELETE FROM `settlements` WHERE `groupId` NOT IN (SELECT `groupId` FROM `expense_groups`) OR `fromMemberId` NOT IN (SELECT `memberId` FROM `group_members`) OR `toMemberId` NOT IN (SELECT `memberId` FROM `group_members`)")

                // 2. Rebuild `group_members` (Topological Level 1: references `expense_groups`)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `group_members_new` (
                        `memberId` TEXT NOT NULL,
                        `groupId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `avatarSeed` TEXT NOT NULL,
                        `isCurrentUser` INTEGER NOT NULL,
                        `upiId` TEXT NOT NULL,
                        PRIMARY KEY(`memberId`),
                        FOREIGN KEY(`groupId`) REFERENCES `expense_groups`(`groupId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `group_members_new` SELECT `memberId`, `groupId`, `name`, `avatarSeed`, `isCurrentUser`, `upiId` FROM `group_members`")
                db.execSQL("DROP TABLE `group_members`")
                db.execSQL("ALTER TABLE `group_members_new` RENAME TO `group_members`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_group_members_groupId` ON `group_members` (`groupId`)")

                // 3. Rebuild `expenses` (Topological Level 2: references `expense_groups` and `group_members`)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `expenses_new` (
                        `expenseId` TEXT NOT NULL,
                        `groupId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `payerId` TEXT NOT NULL,
                        `baseSubtotalCents` INTEGER NOT NULL,
                        `taxCents` INTEGER NOT NULL,
                        `tipCents` INTEGER NOT NULL,
                        `totalAmountCents` INTEGER NOT NULL,
                        `lockedMultiplier` REAL NOT NULL,
                        `unassignedBaseCents` INTEGER NOT NULL,
                        `currencyCode` TEXT NOT NULL,
                        `lockedExchangeRate` REAL NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`expenseId`),
                        FOREIGN KEY(`groupId`) REFERENCES `expense_groups`(`groupId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`payerId`) REFERENCES `group_members`(`memberId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `expenses_new` SELECT `expenseId`, `groupId`, `title`, `payerId`, `baseSubtotalCents`, `taxCents`, `tipCents`, `totalAmountCents`, `lockedMultiplier`, `unassignedBaseCents`, `currencyCode`, `lockedExchangeRate`, `syncStatus`, `createdAt` FROM `expenses`")
                db.execSQL("DROP TABLE `expenses`")
                db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_groupId` ON `expenses` (`groupId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_payerId` ON `expenses` (`payerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_groupId_createdAt` ON `expenses` (`groupId`, `createdAt`)")

                // 4. Rebuild `expense_splits` (Topological Level 3: references `expenses` and `group_members`)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `expense_splits_new` (
                        `splitId` TEXT NOT NULL,
                        `expenseId` TEXT NOT NULL,
                        `memberId` TEXT NOT NULL,
                        `baseClaimedCents` INTEGER NOT NULL,
                        `finalOwedCents` INTEGER NOT NULL,
                        `plusOneCent` INTEGER NOT NULL,
                        PRIMARY KEY(`splitId`),
                        FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`expenseId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`memberId`) REFERENCES `group_members`(`memberId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `expense_splits_new` SELECT `splitId`, `expenseId`, `memberId`, `baseClaimedCents`, `finalOwedCents`, `plusOneCent` FROM `expense_splits` WHERE `rowid` IN (SELECT MAX(`rowid`) FROM `expense_splits` GROUP BY `expenseId`, `memberId`)")
                db.execSQL("DROP TABLE `expense_splits`")
                db.execSQL("ALTER TABLE `expense_splits_new` RENAME TO `expense_splits`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_splits_expenseId` ON `expense_splits` (`expenseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_splits_memberId` ON `expense_splits` (`memberId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_expense_splits_expenseId_memberId` ON `expense_splits` (`expenseId`, `memberId`)")

                // 5. Rebuild `settlements` (Topological Level 2: references `expense_groups` and `group_members`)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `settlements_new` (
                        `settlementId` TEXT NOT NULL,
                        `groupId` TEXT NOT NULL,
                        `fromMemberId` TEXT NOT NULL,
                        `fromMemberName` TEXT NOT NULL,
                        `toMemberId` TEXT NOT NULL,
                        `toMemberName` TEXT NOT NULL,
                        `amountCents` INTEGER NOT NULL,
                        `currencyCode` TEXT NOT NULL,
                        `lockedExchangeRate` REAL NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `settledAt` INTEGER NOT NULL,
                        PRIMARY KEY(`settlementId`),
                        FOREIGN KEY(`groupId`) REFERENCES `expense_groups`(`groupId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`fromMemberId`) REFERENCES `group_members`(`memberId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`toMemberId`) REFERENCES `group_members`(`memberId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `settlements_new` SELECT `settlementId`, `groupId`, `fromMemberId`, `fromMemberName`, `toMemberId`, `toMemberName`, `amountCents`, `currencyCode`, `lockedExchangeRate`, `syncStatus`, `settledAt` FROM `settlements`")
                db.execSQL("DROP TABLE `settlements`")
                db.execSQL("ALTER TABLE `settlements_new` RENAME TO `settlements`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_settlements_groupId` ON `settlements` (`groupId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_settlements_fromMemberId` ON `settlements` (`fromMemberId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_settlements_toMemberId` ON `settlements` (`toMemberId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_settlements_groupId_settledAt` ON `settlements` (`groupId`, `settledAt`)")

                db.execSQL("PRAGMA foreign_keys=ON")
            }
        }
        
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `group_members` ADD COLUMN `userPhone` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `userPhone` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `isPhoneVerified` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `pinHash` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `expense_groups` ADD COLUMN `isDemoSeed` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `expense_groups` SET `isDemoSeed` = 1 WHERE `name` IN ('Lake Tahoe Cabin', 'Apt 4B')")
                db.execSQL("ALTER TABLE `group_members` ADD COLUMN `inviteStatus` TEXT NOT NULL DEFAULT 'JOINED'")
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `expenseCategory` TEXT NOT NULL DEFAULT 'OTHER'")
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `travelPnr` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `providerName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `scheduledAtEpochMs` INTEGER DEFAULT NULL")
            }
        }

        /**
         * v2.3.4 Trip Guide & Smart Loop: purely additive (two new tables, no ledger table touched,
         * no data rewritten). The SQL mirrors exactly what Room generates for [TripGuidePackEntity]
         * and [TripPlanManifestEntity] so post-migration schema validation passes; a JVM test
         * compares these statements with Room's generated `_Impl` create statements.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Shared offline guide packs (no FK: shared across groups, evicted by hard expiry).
                db.execSQL("CREATE TABLE IF NOT EXISTS `trip_guide_pack` (`destinationQid` TEXT NOT NULL, `schemaVersion` INTEGER NOT NULL, `wikivoyageTitle` TEXT, `wikivoyageRevId` INTEGER, `contentHash` TEXT NOT NULL, `packGz` BLOB NOT NULL, `fetchedAtEpochMs` INTEGER NOT NULL, `softExpiryEpochMs` INTEGER NOT NULL, `hardExpiryEpochMs` INTEGER NOT NULL, PRIMARY KEY(`destinationQid`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_trip_guide_pack_hardExpiryEpochMs` ON `trip_guide_pack` (`hardExpiryEpochMs`)")

                // 2. Per-group plan manifest (FK -> expense_groups, cascade-deleted with the group).
                db.execSQL("CREATE TABLE IF NOT EXISTS `trip_plan_manifest` (`groupId` TEXT NOT NULL, `destinationQid` TEXT, `manifestJson` TEXT NOT NULL, `lastSyncMessageId` TEXT, `stayLocalJson` TEXT, `shareStay` INTEGER NOT NULL DEFAULT 0, `updatedAtEpochMs` INTEGER NOT NULL, `pendingPush` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`groupId`), FOREIGN KEY(`groupId`) REFERENCES `expense_groups`(`groupId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_trip_plan_manifest_destinationQid` ON `trip_plan_manifest` (`destinationQid`)")
            }
        }

        fun getInstance(context: Context): SplitMateRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SplitMateRoomDatabase::class.java,
                    "splitmate_native_room.db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigrationFrom(1, 2, 3)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

