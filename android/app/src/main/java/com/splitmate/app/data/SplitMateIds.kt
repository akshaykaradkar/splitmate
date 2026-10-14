package com.splitmate.app.data

import java.security.SecureRandom
import java.util.UUID

/**
 * v2.4.0 rc10: collision-safe IDs for every newly created row (expenses, members, settlements, groups).
 *
 * Before rc10, IDs were `"exp_" + System.currentTimeMillis()`, so two rows created in the same
 * millisecond (a fast double-tap, a batch import, or two phones in the same group) got the same
 * primary key and one silently overwrote the other. The fix follows RFC 9562 UUIDv7:
 *  - 48-bit Unix-millisecond timestamp first, so IDs stay roughly time-ordered (readable in logs,
 *    friendly to B-tree inserts),
 *  - 74 bits from [SecureRandom], so same-millisecond IDs never clash, on one device or across devices.
 *
 * Format: `<prefix>_<uuidv7>`, e.g. `exp_0192f0c4-8a3e-7b1d-9c2f-5e6a7b8c9d0e`.
 *
 * Compatibility: IDs are opaque strings everywhere (nothing parses a timestamp back out of them),
 * so old `exp_<ms>` rows and new rows coexist with no Room schema change and no migration.
 * Ordering must never rely on the ID; use `createdAt` / schedule time and only use the ID as a
 * final tie-breaker for a stable order.
 */
object SplitMateIds {
    private val random = SecureRandom()

    fun newId(prefix: String, nowMs: Long = System.currentTimeMillis()): String =
        "${prefix}_${uuidV7(nowMs)}"

    fun uuidV7(nowMs: Long = System.currentTimeMillis()): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        val ts = nowMs and 0xFFFF_FFFF_FFFFL
        for (i in 0 until 6) {
            bytes[i] = (ts ushr (40 - 8 * i)).toByte()
        }
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x70).toByte() // version 7
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte() // IETF variant
        var msb = 0L
        var lsb = 0L
        for (i in 0 until 8) msb = (msb shl 8) or (bytes[i].toLong() and 0xFF)
        for (i in 8 until 16) lsb = (lsb shl 8) or (bytes[i].toLong() and 0xFF)
        return UUID(msb, lsb).toString()
    }
}
