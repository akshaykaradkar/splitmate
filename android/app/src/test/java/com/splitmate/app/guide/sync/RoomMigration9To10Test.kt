package com.splitmate.app.guide.sync

import androidx.sqlite.db.SupportSQLiteDatabase
import com.splitmate.app.data.ExpenseRevisionEntity
import com.splitmate.app.data.SplitMateRoomDatabase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy

/**
 * v2.4.0 P1 + P4: Room 9 -> 10 migration checks at JVM level (same harness as [RoomMigration7To8Test]).
 * Additive only: two columns on `expenses` (existing rows read as version 0) and the local
 * `expense_revisions` table.
 */
@DisplayName("v2.4.0: Room migration 9 -> 10 (per-expense versions + edit history)")
class RoomMigration9To10Test {

    private fun runMigration(): List<String> {
        val statements = mutableListOf<String>()
        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> { statements += args!![0] as String; null }
                "toString" -> "RecordingSupportSQLiteDatabase"
                "hashCode" -> 0
                "equals" -> false
                else -> throw UnsupportedOperationException("migration must only use execSQL, called ${method.name}")
            }
        } as SupportSQLiteDatabase
        val migration = SplitMateRoomDatabase.MIGRATION_9_10
        assertEquals(9, migration.startVersion)
        assertEquals(10, migration.endVersion)
        migration.migrate(db)
        return statements
    }

    private fun normalize(sql: String) = sql.replace(Regex("\\s+"), " ").replace("( ", "(").replace(" )", ")").trim()

    @Test
    fun `existing expenses only gain columns with safe defaults - no money value is rewritten`() {
        val sql = runMigration().map(::normalize)
        assertTrue(sql.contains("ALTER TABLE `expenses` ADD COLUMN `rowVersion` INTEGER NOT NULL DEFAULT 0"))
        assertTrue(sql.contains("ALTER TABLE `expenses` ADD COLUMN `rowUpdatedBy` TEXT DEFAULT NULL"))
        assertTrue(sql.none { it.startsWith("UPDATE") || it.startsWith("DELETE") || it.startsWith("DROP") }, sql.joinToString("\n"))
    }

    @Test
    fun `revision table columns follow the entity`() {
        val create = runMigration().map(::normalize).single { it.startsWith("CREATE TABLE IF NOT EXISTS `expense_revisions`") }
        val columnDefs = create.substringAfter("(").split(", ").filter { it.startsWith("`") }
        val columns = columnDefs.associate { def -> def.substringAfter("`").substringBefore("`") to def }
        val fields = ExpenseRevisionEntity::class.java.declaredFields.filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
        assertEquals(fields.map { it.name }, columns.keys.toList())
        for (f in fields) {
            val def = columns.getValue(f.name)
            val affinity = if (f.type == String::class.java) "TEXT" else "INTEGER"
            assertTrue(def.startsWith("`${f.name}` $affinity"), def)
            assertEquals(f.name != "editedBy", def.contains("NOT NULL"), def)
        }
        assertTrue(create.contains("PRIMARY KEY(`revisionId`)"))
    }

    @Test
    fun `migration DDL matches what Room generated`() {
        val impl = listOf(
            "build/generated/ksp/debug/java/com/splitmate/app/data/SplitMateRoomDatabase_Impl.java",
            "app/build/generated/ksp/debug/java/com/splitmate/app/data/SplitMateRoomDatabase_Impl.java",
            "build/generated/ksp/debug/kotlin/com/splitmate/app/data/SplitMateRoomDatabase_Impl.kt",
            "app/build/generated/ksp/debug/kotlin/com/splitmate/app/data/SplitMateRoomDatabase_Impl.kt"
        ).map(::File).firstOrNull { it.exists() }
        assumeTrue(impl != null, "Room KSP output not found; skipped (run after compileDebugKotlin)")
        val source = impl!!.readText()
        val generated = Regex("\"(CREATE (?:UNIQUE )?(?:TABLE|INDEX) IF NOT EXISTS `(?:expense_revisions|index_expense_revisions_[A-Za-z_]+)`[^\"]*)\"")
            .findAll(source).map { normalize(it.groupValues[1]) }.toSet()
        assertEquals(3, generated.size, "generated: $generated")
        val migrated = runMigration().map(::normalize).filter { it.startsWith("CREATE") }.toSet()
        assertEquals(generated, migrated)
        val expensesCreate = Regex("\"(CREATE TABLE IF NOT EXISTS `expenses`[^\"]*)\"").find(source)!!.groupValues[1]
        assertTrue(expensesCreate.contains("`rowVersion` INTEGER NOT NULL"))
        assertTrue(expensesCreate.contains("`rowUpdatedBy` TEXT"))
    }

    @Test
    fun `database wiring - version 10, migration added, backup before upgrade, history cleared with the group`() {
        val srcMain = listOf(File("src/main"), File("app/src/main")).firstOrNull { it.isDirectory }
        assumeTrue(srcMain != null)
        val db = File(srcMain, "java/com/splitmate/app/data/SplitMateRoomDatabase.kt").readText()
        assertTrue(db.contains("version = 10"))
        assertTrue(db.contains("ExpenseRevisionEntity::class"))
        assertTrue(db.contains("MIGRATION_8_9, MIGRATION_9_10)"))
        assertTrue(db.contains("\"before_v2.4.0\""))
        val dao = File(srcMain, "java/com/splitmate/app/data/SplitMateDao.kt").readText()
        val cascade = dao.substringAfter("suspend fun deleteGroupCascade(groupId: String) {").substringBefore("\n    }")
        assertTrue(cascade.contains("deleteExpenseRevisionsForGroup(groupId)"))
    }
}
