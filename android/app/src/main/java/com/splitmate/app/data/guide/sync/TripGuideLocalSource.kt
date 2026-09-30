package com.splitmate.app.data.guide.sync

import com.splitmate.app.data.SplitMateDao
import com.splitmate.app.data.TripGuidePackEntity
import com.splitmate.app.data.TripPlanManifestEntity
import kotlinx.coroutines.flow.Flow

/**
 * Narrow persistence seam used by [TripGuideStore] (keeps the store unit-testable without Room).
 * Production: [DaoTripGuideLocalSource] over [SplitMateDao].
 */
interface TripGuideLocalSource {
    suspend fun getPack(destinationQid: String): TripGuidePackEntity?
    fun observePack(destinationQid: String): Flow<TripGuidePackEntity?>
    suspend fun upsertPack(pack: TripGuidePackEntity)
    suspend fun deleteHardExpiredPacks(nowEpochMs: Long): Int

    suspend fun getManifest(groupId: String): TripPlanManifestEntity?
    fun observeManifest(groupId: String): Flow<TripPlanManifestEntity?>
    suspend fun upsertManifest(entity: TripPlanManifestEntity)
    suspend fun getPendingManifests(): List<TripPlanManifestEntity>
}

/** [TripGuideLocalSource] backed by the Room DAO. */
class DaoTripGuideLocalSource(private val dao: SplitMateDao) : TripGuideLocalSource {
    override suspend fun getPack(destinationQid: String) = dao.getGuidePack(destinationQid)
    override fun observePack(destinationQid: String) = dao.observeGuidePack(destinationQid)
    override suspend fun upsertPack(pack: TripGuidePackEntity) = dao.upsertGuidePack(pack)
    override suspend fun deleteHardExpiredPacks(nowEpochMs: Long) = dao.deleteHardExpiredGuidePacks(nowEpochMs)

    override suspend fun getManifest(groupId: String) = dao.getTripPlanManifest(groupId)
    override fun observeManifest(groupId: String) = dao.observeTripPlanManifest(groupId)
    override suspend fun upsertManifest(entity: TripPlanManifestEntity) = dao.upsertTripPlanManifest(entity)
    override suspend fun getPendingManifests() = dao.getPendingTripPlanManifests()
}
