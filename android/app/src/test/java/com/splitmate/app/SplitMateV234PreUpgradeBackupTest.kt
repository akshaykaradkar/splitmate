package com.splitmate.app

import com.splitmate.app.data.PreUpgradeDatabaseBackup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** v2.3.4: the pre-migration trips DB snapshot is taken once, copies side files, and never alters the source. */
class SplitMateV234PreUpgradeBackupTest {

    @Test
    fun `B01 copies db, wal and shm once and leaves the live db untouched`(@TempDir tmp: File) {
        val db = File(tmp, "databases/splitmate_native_room.db").apply { parentFile.mkdirs(); writeText("LEDGER") }
        File(db.path + "-wal").writeText("WAL")
        File(db.path + "-shm").writeText("SHM")
        val target = File(tmp, "files/db_backups/before_v2.3.4")

        val out = PreUpgradeDatabaseBackup.snapshotFiles(db, target)
        assertNotNull(out)
        assertEquals("LEDGER", File(target, db.name).readText())
        assertEquals("WAL", File(target, db.name + "-wal").readText())
        assertEquals("SHM", File(target, db.name + "-shm").readText())
        assertEquals("LEDGER", db.readText())

        // Second launch: the original snapshot is preserved even if the live db changed after migration.
        db.writeText("MIGRATED")
        PreUpgradeDatabaseBackup.snapshotFiles(db, target)
        assertEquals("LEDGER", File(target, db.name).readText())
    }

    @Test
    fun `B02 fresh install has nothing to back up`(@TempDir tmp: File) {
        val db = File(tmp, "databases/splitmate_native_room.db")
        assertNull(PreUpgradeDatabaseBackup.snapshotFiles(db, File(tmp, "backup")))
        assertTrue(!File(tmp, "backup").exists())
    }
}
