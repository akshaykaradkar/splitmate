package com.splitmate.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CurrencyRateEntity::class,
        ExpenseGroupEntity::class,
        GroupMemberEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
        SettlementEntity::class,
        UserProfileEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class SplitMateRoomDatabase : RoomDatabase() {

    abstract fun dao(): SplitMateDao

    companion object {
        @Volatile
        private var INSTANCE: SplitMateRoomDatabase? = null

        fun getInstance(context: Context): SplitMateRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SplitMateRoomDatabase::class.java,
                    "splitmate_native_room.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
