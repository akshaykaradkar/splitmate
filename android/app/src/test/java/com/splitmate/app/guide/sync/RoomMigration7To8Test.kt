package com.splitmate.app.guide.sync

import androidx.sqlite.db.SupportSQLiteDatabase
import com.splitmate.app.data.SplitMateRoomDatabase
import com.splitmate.app.data.TripGuidePackEntity
import com.splitmate.app.data.TripPlanManifestEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy

/**
 * Room 7 -> 8 migration checks at JVM level.
 *
 * The repo has no Robolectric / room-testing / instrumentation migration harness, so instead of
 * `MigrationTestHelper` this test:
 *  1. runs `MIGRATION_7_8` against a recording `SupportSQLiteDatabase` proxy and asserts the DDL;
 *  2. checks every entity field maps to a column with the right SQLite affinity/nullability;
 *  3. compares the migration DDL with the CREATE statements Room generated into
 *     `SplitMateRoomDatabase_Impl` (KSP output), which is what Room validates against at runtime;
 *  4. asserts wiring in source (version 8, entities registered, migration added, cascade delete).
 */
@DisplayName("v2.3.4 WS-D: Room migration 7 -> 8 (JVM schema assertions)")
class RoomMigration7To8Test {

    private fun runMigration(): List<String> {
        val statements = mutableListOf<String>()
        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    statements += args!![0] as String
                    null
                }
                "toString" -> "RecordingSupportSQLiteDatabase"
                "hashCode" -> 0
                "equals" -> false
                else -> throw UnsupportedOperationException("migration must only use execSQL, called ${method.name}")
            }
        } as SupportSQLiteDatabase
        val migration = SplitMateRoomDatabase.MIGRATION_7_8
        assertEquals(7, migration.startVersion)
        assertEquals(8, migration.endVersion)
        migration.migrate(db)
        return statements
    }

    private fun normalize(sql: String) = sql.replace(Regex("\\s+"), " ")
        .replace("( ", "(").replace(" )", ")").trim()

    @Test
    fun `migration only creates the two new tables and their indices`() {
        val sql = runMigration().map(::normalize)
        assertEquals(4, sql.size, sql.joinToString("\n"))
        assertTrue(sql.all { it.startsWith("CREATE TABLE IF NOT EXISTS") || it.startsWith("CREATE INDEX IF NOT EXISTS") })
        val createdTables = sql.filter { it.startsWith("CREATE TABLE") }
            .map { it.substringAfter("`").substringBefore("`") }.toSet()
        assertEquals(setOf("trip_guide_pack", "trip_plan_manifest"), createdTables, "no ledger table may be touched")
        val indexedTables = sql.filter { it.startsWith("CREATE INDEX") }
            .map { it.substringAfter(" ON `").substringBefore("`") }.toSet()
        assertEquals(setOf("trip_guide_pack", "trip_plan_manifest"), indexedTables)
        assertTrue(sql.any { it.startsWith("CREATE TABLE IF NOT EXISTS `trip_guide_pack`") })
        assertTrue(sql.any { it.startsWith("CREATE TABLE IF NOT EXISTS `trip_plan_manifest`") })
        assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS `index_trip_guide_pack_hardExpiryEpochMs` ON `trip_guide_pack` (`hardExpiryEpochMs`)"))
        assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS `index_trip_plan_manifest_destinationQid` ON `trip_plan_manifest` (`destinationQid`)"))
    }

    @Test
    fun `manifest table cascades with its group, pack table is shared`() {
        val sql = runMigration().map(::normalize)
        val manifest = sql.single { it.startsWith("CREATE TABLE IF NOT EXISTS `trip_plan_manifest`") }
        assertTrue(manifest.contains("PRIMARY KEY(`groupId`)"))
        assertTrue(manifest.contains("FOREIGN KEY(`groupId`) REFERENCES `expense_groups`(`groupId`) ON UPDATE NO ACTION ON DELETE CASCADE"))
        assertTrue(manifest.contains("`shareStay` INTEGER NOT NULL DEFAULT 0"), "stay sharing must default to off")
        assertTrue(manifest.contains("`pendingPush` INTEGER NOT NULL DEFAULT 0"))
        val pack = sql.single { it.startsWith("CREATE TABLE IF NOT EXISTS `trip_guide_pack`") }
        assertTrue(pack.contains("PRIMARY KEY(`destinationQid`)"))
        assertTrue(pack.contains("`packGz` BLOB NOT NULL"))
        assertFalse(pack.contains("FOREIGN KEY"), "packs are shared across groups")
    }

    @Test
    fun `every entity field has a matching column with the right affinity and nullability`() {
        val sql = runMigration().map(::normalize)
        val nullable = mapOf(
            TripGuidePackEntity::class.java to setOf("wikivoyageTitle", "wikivoyageRevId"),
            TripPlanManifestEntity::class.java to setOf("destinationQid", "lastSyncMessageId", "stayLocalJson")
        )
        val tables = mapOf(
            TripGuidePackEntity::class.java to "trip_guide_pack",
            TripPlanManifestEntity::class.java to "trip_plan_manifest"
        )
        for ((cls, table) in tables) {
            val create = sql.single { it.startsWith("CREATE TABLE IF NOT EXISTS `$table`") }
            val columnDefs = create.substringAfter("(").split(", ").filter { it.startsWith("`") }
            val columns = columnDefs.associate { def -> def.substringAfter("`").substringBefore("`") to def }
            val fields = cls.declaredFields.filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
            assertEquals(fields.map { it.name }, columns.keys.toList(), "column order must follow the entity for $table")
            for (f in fields) {
                val def = columns.getValue(f.name)
                val affinity = when (f.type) {
                    String::class.java -> "TEXT"
                    ByteArray::class.java -> "BLOB"
                    Int::class.javaPrimitiveType, Long::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
                    Int::class.javaObjectType, Long::class.javaObjectType, Boolean::class.javaObjectType -> "INTEGER"
                    else -> error("unexpected field type ${f.type} for ${f.name}")
                }
                assertTrue(def.startsWith("`${f.name}` $affinity"), "$table.${f.name}: $def")
                val isNullable = f.name in nullable.getValue(cls)
                assertEquals(!isNullable, def.contains("NOT NULL"), "$table.${f.name} nullability: $def")
            }
        }
    }

    @Test
    fun `migration DDL matches the statements Room generated for the entities`() {
        val implCandidates = listOf(
            "build/generated/ksp/debug/java/com/splitmate/app/data/SplitMateRoomDatabase_Impl.java",
            "app/build/generated/ksp/debug/java/com/splitmate/app/data/SplitMateRoomDatabase_Impl.java",
            "build/generated/ksp/debug/kotlin/com/splitmate/app/data/SplitMateRoomDatabase_Impl.kt",
            "app/build/generated/ksp/debug/kotlin/com/splitmate/app/data/SplitMateRoomDatabase_Impl.kt"
        ).map(::File)
        val impl = implCandidates.firstOrNull { it.exists() }
        assumeTrue(impl != null, "Room KSP output not found; skipped (run after compileDebugKotlin)")
        val source = impl!!.readText()
        val generated = Regex("\"(CREATE (?:UNIQUE )?(?:TABLE|INDEX) IF NOT EXISTS `(?:trip_guide_pack|trip_plan_manifest|index_trip_guide_pack_[A-Za-z_]+|index_trip_plan_manifest_[A-Za-z_]+)`[^\"]*)\"")
            .findAll(source).map { normalize(it.groupValues[1]) }.toSet()
        assertEquals(4, generated.size, "generated: $generated")
        assertEquals(generated, runMigration().map(::normalize).toSet())
    }

    @Test
    fun `database wiring registers version 8, both entities, the migration and cascade delete`() {
        val srcMain = listOf(File("src/main"), File("app/src/main")).firstOrNull { it.isDirectory }
        assumeTrue(srcMain != null, "src/main not found from ${File(".").absolutePath}")
        val db = File(srcMain, "java/com/splitmate/app/data/SplitMateRoomDatabase.kt").readText()
        assertTrue(db.contains("version = 8"))
        assertTrue(db.contains("TripGuidePackEntity::class"))
        assertTrue(db.contains("TripPlanManifestEntity::class"))
        assertTrue(db.contains(".addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)"))

        val dao = File(srcMain, "java/com/splitmate/app/data/SplitMateDao.kt").readText()
        val cascade = dao.substringAfter("suspend fun deleteGroupCascade(groupId: String) {").substringBefore("\n    }")
        assertTrue(cascade.contains("deleteTripPlanManifestForGroup(groupId)"), "group delete must clear the manifest")
        assertTrue(
            cascade.indexOf("deleteTripPlanManifestForGroup(groupId)") < cascade.indexOf("deleteGroupById(groupId)"),
            "manifest (child) must be deleted before the group (parent)"
        )
        assertFalse(cascade.contains("GuidePack"), "packs are shared across groups and must survive a group delete")
        val clearAll = dao.substringAfter("suspend fun clearAllLedgerData() {").substringBefore("\n    }")
        assertTrue(clearAll.indexOf("deleteAllTripPlanManifests()") in 0 until clearAll.indexOf("deleteAllGroups()"))
    }
}
