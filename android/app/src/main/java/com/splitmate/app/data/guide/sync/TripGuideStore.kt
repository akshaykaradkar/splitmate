package com.splitmate.app.data.guide.sync

import com.splitmate.app.data.SplitMateDao
import com.splitmate.app.data.TripGuidePackEntity
import com.splitmate.app.data.TripPlanManifestEntity
import com.splitmate.app.data.guide.GuidePack
import com.splitmate.app.data.guide.PlanManifest
import com.splitmate.app.data.guide.StayPin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Thin repository over Room for v2.3.4 guide packs and per-group plan manifests.
 *
 * - Packs are opaque gzip bytes (the content workstream owns the pack codec) plus metadata, shared
 *   across groups and evicted by hard expiry (+90 d). Soft expiry (+14 d) triggers a background
 *   refresh; the pinned Wikivoyage revision stays until the planner accepts an update (decision #19).
 * - The manifest row stores the merged group manifest (`manifestJson`) and, separately, this
 *   device's LOCAL-only stay (`stayLocalJson`). The local stay only enters the synced manifest
 *   while `shareStay` is true (decision #14, default off).
 * - All writes are serialised by a mutex (read-modify-write safety). Manifest writes return false
 *   instead of throwing (e.g. FK failure when the group row does not exist): guide features must
 *   never break money flows.
 */
class TripGuideStore(
    private val local: TripGuideLocalSource,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val mutex = Mutex()

    // ---------------------------------------------------------------------------------------------
    // Packs
    // ---------------------------------------------------------------------------------------------

    /**
     * Stores a gzip-compressed pack as given. Expiries are derived from [fetchedAtEpochMs].
     * @throws IllegalArgumentException for an invalid QID, empty/non-gzip/oversize bytes.
     */
    suspend fun savePack(
        destinationQid: String,
        packGz: ByteArray,
        contentHash: String,
        wikivoyageTitle: String?,
        wikivoyageRevId: Long?,
        schemaVersion: Int = PACK_SCHEMA_VERSION,
        fetchedAtEpochMs: Long = clock()
    ): TripGuidePackEntity {
        require(PlanManifestCodec.isValidQid(destinationQid)) { "invalid destination QID" }
        require(packGz.size >= 2 && packGz[0] == GZIP_MAGIC_0 && packGz[1] == GZIP_MAGIC_1) { "packGz must be gzip data" }
        require(packGz.size <= MAX_PACK_GZ_BYTES) { "packGz exceeds $MAX_PACK_GZ_BYTES bytes" }
        require(contentHash.isNotBlank()) { "contentHash required" }
        require(schemaVersion >= 1) { "schemaVersion must be >= 1" }
        val entity = TripGuidePackEntity(
            destinationQid = destinationQid,
            schemaVersion = schemaVersion,
            wikivoyageTitle = wikivoyageTitle?.take(MAX_TITLE_LENGTH),
            wikivoyageRevId = wikivoyageRevId,
            contentHash = contentHash,
            packGz = packGz.copyOf(),
            fetchedAtEpochMs = fetchedAtEpochMs,
            softExpiryEpochMs = fetchedAtEpochMs + SOFT_TTL_MS,
            hardExpiryEpochMs = fetchedAtEpochMs + HARD_TTL_MS
        )
        mutex.withLock { local.upsertPack(entity) }
        return entity
    }

    /** The stored pack (even if expired; use [isSoftExpired]/[isHardExpired] to decide). */
    suspend fun loadPack(destinationQid: String): TripGuidePackEntity? = local.getPack(destinationQid)

    /** The stored pack only if it is not hard-expired. */
    suspend fun loadUsablePack(destinationQid: String, now: Long = clock()): TripGuidePackEntity? =
        local.getPack(destinationQid)?.takeUnless { isHardExpired(it, now) }

    fun observePack(destinationQid: String): Flow<TripGuidePackEntity?> = local.observePack(destinationQid)

    /** Deletes packs whose hard expiry has passed. Returns the number removed. */
    suspend fun evictHardExpiredPacks(now: Long = clock()): Int = mutex.withLock { local.deleteHardExpiredPacks(now) }

    // ---------------------------------------------------------------------------------------------
    // Manifests
    // ---------------------------------------------------------------------------------------------

    suspend fun getManifest(groupId: String): PlanManifest? =
        local.getManifest(groupId)?.let { PlanManifestCodec.decode(it.manifestJson) }

    fun observeManifest(groupId: String): Flow<PlanManifest?> =
        local.observeManifest(groupId)
            .map { row -> row?.let { PlanManifestCodec.decode(it.manifestJson) } }
            .distinctUntilChanged()

    suspend fun getManifestEntity(groupId: String): TripPlanManifestEntity? = local.getManifest(groupId)

    fun observeManifestEntity(groupId: String): Flow<TripPlanManifestEntity?> = local.observeManifest(groupId)

    /** Replaces the group's manifest after a LOCAL edit; marks it pending push. */
    suspend fun upsertManifest(groupId: String, manifest: PlanManifest, pendingPush: Boolean = true): Boolean =
        mutate(groupId) { row -> row.withManifest(manifest).copy(pendingPush = pendingPush) }

    /**
     * Merges a manifest received from the plan topic into the local one and records the cursor.
     * The row stays pending push when the local state has something the remote lacks.
     * @return the merged manifest, or null if the write failed.
     */
    suspend fun applyRemoteManifest(
        groupId: String,
        remote: PlanManifest,
        messageId: String?,
        organizerKeys: Set<String> = emptySet()
    ): PlanManifest? {
        var merged: PlanManifest? = null
        val ok = mutate(groupId) { row ->
            val localManifest = PlanManifestCodec.decode(row.manifestJson) ?: emptyManifest()
            val m = PlanManifestMerger.merge(localManifest, remote, organizerKeys)
            merged = m
            val needsPush = m != PlanManifestMerger.normalize(remote)
            row.withManifest(m).copy(
                lastSyncMessageId = messageId?.takeIf { it.isNotEmpty() } ?: row.lastSyncMessageId,
                pendingPush = needsPush
            )
        }
        return if (ok) merged else null
    }

    /** Records a successful publish (clears pending push, advances the cursor if an id is known). */
    suspend fun markPushed(groupId: String, messageId: String?): Boolean =
        mutate(groupId, createIfMissing = false) { row ->
            row.copy(
                pendingPush = false,
                lastSyncMessageId = messageId?.takeIf { it.isNotEmpty() } ?: row.lastSyncMessageId
            )
        }

    suspend fun pendingPushGroupIds(): List<String> = local.getPendingManifests().map { it.groupId }

    // ---------------------------------------------------------------------------------------------
    // Stay (local-only by default)
    // ---------------------------------------------------------------------------------------------

    suspend fun getLocalStay(groupId: String): StayPin? =
        local.getManifest(groupId)?.stayLocalJson?.let { PlanManifestCodec.decodeStay(it) }

    suspend fun isShareStay(groupId: String): Boolean = local.getManifest(groupId)?.shareStay ?: false

    /**
     * Sets this device's stay. It is written to the synced manifest only when sharing is on.
     */
    suspend fun setLocalStay(groupId: String, stay: StayPin, byMemberKey: String?): Boolean {
        val now = clock()
        return mutate(groupId) { row ->
            val withLocal = row.copy(stayLocalJson = PlanManifestCodec.stayToJson(stay))
            if (!row.shareStay) return@mutate withLocal
            val m = PlanManifestCodec.decode(row.manifestJson) ?: emptyManifest()
            withLocal.withManifest(PlanManifestMerger.setStay(m, stay, byMemberKey, now)).copy(pendingPush = true)
        }
    }

    /**
     * Clears this device's stay. If this member had shared it, the shared copy is retracted too
     * (stay tombstone), so peers stop showing it.
     */
    suspend fun clearLocalStay(groupId: String, byMemberKey: String?, organizerKeys: Set<String> = emptySet()): Boolean {
        val now = clock()
        return mutate(groupId, createIfMissing = false) { row ->
            val cleared = row.copy(stayLocalJson = null)
            val m = PlanManifestCodec.decode(row.manifestJson) ?: return@mutate cleared
            val shared = m.stay ?: return@mutate cleared
            if (byMemberKey == null || shared.setByMemberKey != byMemberKey) return@mutate cleared
            val next = PlanManifestMerger.clearStay(m, byMemberKey, organizerKeys, now) ?: return@mutate cleared
            cleared.withManifest(next).copy(pendingPush = true)
        }
    }

    /**
     * Clears the group's SHARED stay (whoever set it). Allowed only for its setter or an
     * organizer; returns false (and changes nothing) otherwise. The local stay is untouched.
     */
    suspend fun clearSharedStay(groupId: String, byMemberKey: String?, organizerKeys: Set<String>): Boolean {
        val now = clock()
        var allowed = false
        val ok = mutate(groupId, createIfMissing = false) { row ->
            val m = PlanManifestCodec.decode(row.manifestJson) ?: return@mutate row
            if (m.stay == null) {
                allowed = true
                return@mutate row
            }
            val next = PlanManifestMerger.clearStay(m, byMemberKey, organizerKeys, now) ?: return@mutate row
            allowed = true
            row.withManifest(next).copy(pendingPush = true)
        }
        return ok && allowed
    }

    /**
     * Toggles stay sharing for the group. Turning it on shares the current local stay; turning it
     * off retracts a stay this member shared (tombstone), so peers stop showing it.
     */
    suspend fun setShareStay(groupId: String, share: Boolean, byMemberKey: String?, organizerKeys: Set<String> = emptySet()): Boolean {
        val now = clock()
        return mutate(groupId) { row ->
            val base = row.copy(shareStay = share)
            val m = PlanManifestCodec.decode(row.manifestJson) ?: emptyManifest()
            val localStay = row.stayLocalJson?.let { PlanManifestCodec.decodeStay(it) }
            when {
                share && localStay != null ->
                    base.withManifest(PlanManifestMerger.setStay(m, localStay, byMemberKey, now)).copy(pendingPush = true)
                !share && m.stay != null && byMemberKey != null && m.stay.setByMemberKey == byMemberKey -> {
                    val next = PlanManifestMerger.clearStay(m, byMemberKey, organizerKeys, now)
                    if (next != null) base.withManifest(next).copy(pendingPush = true) else base
                }
                else -> base
            }
        }
    }

    /** The stay the loop should start from: the newer of the local stay and the shared stay. */
    suspend fun effectiveStay(groupId: String): StayPin? {
        val row = local.getManifest(groupId) ?: return null
        val localStay = row.stayLocalJson?.let { PlanManifestCodec.decodeStay(it) }
        val shared = PlanManifestCodec.decode(row.manifestJson)?.stay
        return when {
            localStay == null -> shared
            shared == null -> localStay
            shared.setAtEpochMs > localStay.setAtEpochMs -> shared
            else -> localStay
        }
    }

    /**
     * The manifest to publish. The stay is kept only when sharing is on (the codec strips it
     * again as defence in depth when the sync layer is wired with `shareStayForGroup`).
     */
    suspend fun manifestForPublish(groupId: String): PlanManifest? {
        val row = local.getManifest(groupId) ?: return null
        val m = PlanManifestCodec.decode(row.manifestJson) ?: return null
        return if (row.shareStay) m else m.copy(stay = null)
    }

    // ---------------------------------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------------------------------

    private suspend fun mutate(
        groupId: String,
        createIfMissing: Boolean = true,
        transform: (TripPlanManifestEntity) -> TripPlanManifestEntity
    ): Boolean {
        return try {
            mutex.withLock {
                val existing = local.getManifest(groupId)
                val row = existing ?: if (createIfMissing) newRow(groupId) else return@withLock false
                val next = transform(row).copy(groupId = groupId, updatedAtEpochMs = clock())
                local.upsertManifest(next)
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    private fun newRow(groupId: String) = TripPlanManifestEntity(
        groupId = groupId,
        destinationQid = null,
        manifestJson = PlanManifestCodec.toJson(emptyManifest(), includeStay = true),
        lastSyncMessageId = null,
        stayLocalJson = null,
        shareStay = false,
        updatedAtEpochMs = clock(),
        pendingPush = false
    )

    private fun TripPlanManifestEntity.withManifest(m: PlanManifest) = copy(
        destinationQid = m.destinationQid,
        manifestJson = PlanManifestCodec.toJson(m, includeStay = true)
    )

    companion object {
        const val PACK_SCHEMA_VERSION = 1
        const val SOFT_TTL_MS: Long = 14L * 24L * 60L * 60L * 1000L
        const val HARD_TTL_MS: Long = 90L * 24L * 60L * 60L * 1000L
        const val MAX_PACK_GZ_BYTES = 1024 * 1024
        private const val MAX_TITLE_LENGTH = 256
        private val GZIP_MAGIC_0: Byte = 0x1f.toByte()
        private val GZIP_MAGIC_1: Byte = 0x8b.toByte()

        fun fromDao(dao: SplitMateDao, clock: () -> Long = { System.currentTimeMillis() }) =
            TripGuideStore(DaoTripGuideLocalSource(dao), clock)

        fun emptyManifest(): PlanManifest = PlanManifest(
            destinationQid = null,
            wikivoyageRevisionId = null,
            stay = null,
            pinned = emptyMap(),
            hidden = emptyMap(),
            days = emptyMap(),
            updatedAtEpochMs = 0L,
            updatedByMemberKey = null
        )

        fun isSoftExpired(pack: TripGuidePackEntity, now: Long): Boolean = now >= pack.softExpiryEpochMs

        fun isHardExpired(pack: TripGuidePackEntity, now: Long): Boolean = now >= pack.hardExpiryEpochMs

        /** Sanity helper for callers holding a decoded pack. */
        fun isSchemaSupported(pack: GuidePack): Boolean = pack.schema == GuidePack.SCHEMA_V1
    }
}
