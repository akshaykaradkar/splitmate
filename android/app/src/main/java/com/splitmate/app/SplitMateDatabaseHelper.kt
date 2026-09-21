package com.splitmate.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Local Offline SQLite Vault implementing the 6-table integer-cent schema from BRD Section 5.
 */
class SplitMateDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "splitmate_vault.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users (
                user_id TEXT PRIMARY KEY,
                display_name TEXT NOT NULL,
                avatar_emoji TEXT NOT NULL,
                is_guest INTEGER NOT NULL DEFAULT 1,
                default_currency TEXT NOT NULL DEFAULT 'USD'
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE expense_groups (
                group_id TEXT PRIMARY KEY,
                group_name TEXT NOT NULL,
                category_icon TEXT NOT NULL,
                currency_code TEXT NOT NULL DEFAULT 'USD',
                net_balance_cents INTEGER NOT NULL DEFAULT 0,
                member_count INTEGER NOT NULL DEFAULT 4,
                subtitle TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE group_member_cross_ref (
                group_id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                PRIMARY KEY (group_id, user_id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE expenses (
                expense_id TEXT PRIMARY KEY,
                group_id TEXT NOT NULL,
                payer_id TEXT NOT NULL,
                title TEXT NOT NULL,
                category TEXT NOT NULL,
                base_subtotal_cents INTEGER NOT NULL,
                tax_cents INTEGER NOT NULL,
                tip_cents INTEGER NOT NULL,
                total_amount_cents INTEGER NOT NULL,
                locked_multiplier REAL NOT NULL,
                unassigned_base_cents INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE expense_splits (
                split_id TEXT PRIMARY KEY,
                expense_id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                base_claimed_cents INTEGER NOT NULL,
                owed_amount_cents INTEGER NOT NULL,
                plus_one_cent_flag INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE settlements (
                settlement_id TEXT PRIMARY KEY,
                group_id TEXT NOT NULL,
                debtor_id TEXT NOT NULL,
                debtor_name TEXT NOT NULL,
                creditor_id TEXT NOT NULL,
                creditor_name TEXT NOT NULL,
                amount_cents INTEGER NOT NULL,
                method TEXT NOT NULL,
                settled_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        seedInitialBuckwheatData(db)
    }

    private fun seedInitialBuckwheatData(db: SQLiteDatabase) {
        // Seed Users (including Instant Guest & Shadow Guest Personas)
        val users = listOf(
            arrayOf("u_alex", "Alex", "🌱", 0, "USD"),
            arrayOf("u_sam", "Sam", "🦊", 1, "USD"),
            arrayOf("u_priya", "Priya (Guest)", "🐱", 1, "USD"),
            arrayOf("u_maya", "Maya", "🦔", 1, "USD"),
            arrayOf("u_kai", "Kai", "☕", 1, "USD"),
            arrayOf("u_liam", "Liam", "🦊", 1, "USD")
        )
        for (u in users) {
            val cv = ContentValues().apply {
                put("user_id", u[0] as String)
                put("display_name", u[1] as String)
                put("avatar_emoji", u[2] as String)
                put("is_guest", u[3] as Int)
                put("default_currency", u[4] as String)
            }
            db.insert("users", null, cv)
        }

        // Seed Active Groups matching Stitch prototype
        val groups = listOf(
            arrayOf("g_tahoe", "Lake Tahoe Cabin", "cabin", "USD", 8600L, 6, "6 members · 4 active splits"),
            arrayOf("g_mission", "Mission Apt Roommates", "apartment", "USD", 5650L, 4, "4 members · Monthly utilities"),
            arrayOf("g_tokyo", "Tokyo Food Odyssey", "ramen_dining", "USD", 0L, 3, "3 members · All 14 receipts reconciled")
        )
        for (g in groups) {
            val cv = ContentValues().apply {
                put("group_id", g[0] as String)
                put("group_name", g[1] as String)
                put("category_icon", g[2] as String)
                put("currency_code", g[3] as String)
                put("net_balance_cents", g[4] as Long)
                put("member_count", g[5] as Int)
                put("subtitle", g[6] as String)
            }
            db.insert("expense_groups", null, cv)
        }

        // Seed flagship receipts & expenses
        val now = System.currentTimeMillis()
        val exp1 = ContentValues().apply {
            put("expense_id", "exp_osteria")
            put("group_id", "g_tokyo")
            put("payer_id", "u_alex")
            put("title", "Osteria Del Sole · Table 14")
            put("category", "Dining")
            put("base_subtotal_cents", 12000L)
            put("tax_cents", 1065L)
            put("tip_cents", 1785L)
            put("total_amount_cents", 14850L)
            put("locked_multiplier", 1.2375)
            put("unassigned_base_cents", 1131L)
            put("created_at", now - 7200_000L)
        }
        db.insert("expenses", null, exp1)

        val exp2 = ContentValues().apply {
            put("expense_id", "exp_groceries")
            put("group_id", "g_tahoe")
            put("payer_id", "u_alex")
            put("title", "Mountain Sunrise Groceries")
            put("category", "Groceries")
            put("base_subtotal_cents", 4250L)
            put("tax_cents", 0L)
            put("tip_cents", 0L)
            put("total_amount_cents", 4250L)
            put("locked_multiplier", 1.0)
            put("unassigned_base_cents", 0L)
            put("created_at", now - 3600_000L)
        }
        db.insert("expenses", null, exp2)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS settlements")
        db.execSQL("DROP TABLE IF EXISTS expense_splits")
        db.execSQL("DROP TABLE IF EXISTS expenses")
        db.execSQL("DROP TABLE IF EXISTS group_member_cross_ref")
        db.execSQL("DROP TABLE IF EXISTS expense_groups")
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    fun updateActiveUserProfile(alias: String, emoji: String, currency: String, isGuest: Boolean) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("display_name", alias)
            put("avatar_emoji", emoji)
            put("default_currency", currency)
            put("is_guest", if (isGuest) 1 else 0)
        }
        db.update("users", cv, "user_id = ?", arrayOf("u_alex"))
    }

    fun insertQuickExpense(
        title: String,
        category: String,
        groupId: String,
        payerId: String,
        totalCents: Long
    ): JSONObject {
        val db = writableDatabase
        val expId = "exp_" + System.currentTimeMillis()
        val cv = ContentValues().apply {
            put("expense_id", expId)
            put("group_id", groupId)
            put("payer_id", payerId)
            put("title", title)
            put("category", category)
            put("base_subtotal_cents", totalCents)
            put("tax_cents", 0L)
            put("tip_cents", 0L)
            put("total_amount_cents", totalCents)
            put("locked_multiplier", 1.0)
            put("unassigned_base_cents", 0L)
            put("created_at", System.currentTimeMillis())
        }
        db.insert("expenses", null, cv)

        val splits = SplitMateMathEngine.splitEquallyZeroDrift(
            totalCents,
            listOf("u_alex" to "Alex", "u_sam" to "Sam", "u_priya" to "Priya")
        )
        for ((idx, sp) in splits.withIndex()) {
            val scv = ContentValues().apply {
                put("split_id", "${expId}_$idx")
                put("expense_id", expId)
                put("user_id", sp.memberId)
                put("base_claimed_cents", sp.finalCents)
                put("owed_amount_cents", sp.finalCents)
                put("plus_one_cent_flag", if (sp.plusOneCent) 1 else 0)
            }
            db.insert("expense_splits", null, scv)
        }

        val out = JSONObject()
        out.put("expenseId", expId)
        out.put("totalCents", totalCents)
        out.put("auditHash", computeAuditHash())
        return out
    }

    fun recordSettlement(debtorName: String, creditorName: String, amountCents: Long, method: String): String {
        val db = writableDatabase
        val id = "settle_" + System.currentTimeMillis()
        val cv = ContentValues().apply {
            put("settlement_id", id)
            put("group_id", "g_tahoe")
            put("debtor_id", "u_${debtorName.lowercase()}")
            put("debtor_name", debtorName)
            put("creditor_id", "u_${creditorName.lowercase()}")
            put("creditor_name", creditorName)
            put("amount_cents", amountCents)
            put("method", method)
            put("settled_at", System.currentTimeMillis())
        }
        db.insert("settlements", null, cv)
        return computeAuditHash()
    }

    fun computeAuditHash(): String {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT expense_id, total_amount_cents FROM expenses ORDER BY created_at DESC", null)
        val sb = StringBuilder("splitmate-zero-drift:")
        cursor.use {
            while (it.moveToNext()) {
                sb.append(it.getString(0)).append("=").append(it.getLong(1)).append(";")
            }
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(sb.toString().toByteArray())
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "#${hex.take(4)}...${hex.takeLast(4)}"
    }

    fun exportVaultSnapshotJson(): String {
        val db = readableDatabase
        val root = JSONObject()
        root.put("engine", "SplitMate SQLite Vault (0.00c Drift)")
        root.put("auditHash", computeAuditHash())

        val expensesArr = JSONArray()
        db.rawQuery("SELECT expense_id, title, category, total_amount_cents, locked_multiplier FROM expenses ORDER BY created_at DESC", null).use { c ->
            while (c.moveToNext()) {
                val o = JSONObject()
                o.put("id", c.getString(0))
                o.put("title", c.getString(1))
                o.put("category", c.getString(2))
                o.put("totalCents", c.getLong(3))
                o.put("lockedMultiplier", c.getDouble(4))
                expensesArr.put(o)
            }
        }
        root.put("expenses", expensesArr)

        val settlementsArr = JSONArray()
        db.rawQuery("SELECT debtor_name, creditor_name, amount_cents, method FROM settlements ORDER BY settled_at DESC", null).use { c ->
            while (c.moveToNext()) {
                val o = JSONObject()
                o.put("debtor", c.getString(0))
                o.put("creditor", c.getString(1))
                o.put("amountCents", c.getLong(2))
                o.put("method", c.getString(3))
                settlementsArr.put(o)
            }
        }
        root.put("settlements", settlementsArr)
        return root.toString()
    }
}
