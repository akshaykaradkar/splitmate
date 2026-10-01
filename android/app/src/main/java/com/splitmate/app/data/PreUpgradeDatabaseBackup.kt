package com.splitmate.app.data

import android.content.Context
import java.io.File

/**
 * One-time, local safety snapshot of the Room database taken BEFORE Room opens (and therefore before
 * any schema migration runs) on the first launch of a new app version.
 *
 * - Copies `splitmate_native_room.db` plus its `-wal` / `-shm` side files into
 *   `files/db_backups/<tag>/` while the database is still closed, so the copy is consistent.
 * - Runs once per tag (a marker file is written last). Never touches the live database.
 * - Every failure is swallowed: a backup problem must never block the app from starting.
 */
object PreUpgradeDatabaseBackup {
    const val BACKUP_DIR = "db_backups"
    private const val MARKER = "snapshot.ok"
    private val SIDE_SUFFIXES = listOf("", "-wal", "-shm")

    /** @return the backup directory if a snapshot exists (new or previous), else null. */
    fun snapshotOnce(context: Context, dbName: String, tag: String): File? = runCatching {
        val dbFile = context.getDatabasePath(dbName)
        snapshotFiles(dbFile, File(context.filesDir, "$BACKUP_DIR/$tag"))
    }.getOrNull()

    /** Pure file logic (unit-testable without Android). */
    fun snapshotFiles(dbFile: File, targetDir: File): File? {
        if (File(targetDir, MARKER).exists()) return targetDir
        if (!dbFile.exists() || dbFile.length() == 0L) return null // fresh install: nothing to protect
        targetDir.mkdirs()
        for (suffix in SIDE_SUFFIXES) {
            val src = File(dbFile.path + suffix)
            if (src.exists()) src.copyTo(File(targetDir, dbFile.name + suffix), overwrite = true)
        }
        File(targetDir, MARKER).writeText(System.currentTimeMillis().toString())
        return targetDir
    }
}
