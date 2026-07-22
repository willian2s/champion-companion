package br.com.mwconsulting.championcompanion.data

import androidx.room3.withWriteTransaction
import br.com.mwconsulting.championcompanion.core.common.IoDispatcher
import br.com.mwconsulting.championcompanion.core.database.ChampionDatabase
import br.com.mwconsulting.championcompanion.core.database.DeviceEntity
import br.com.mwconsulting.championcompanion.core.database.DiagnosticDerivedOutputEntity
import br.com.mwconsulting.championcompanion.core.database.HealthSyncOperationEntity
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSession
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionId
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionRepository
import br.com.mwconsulting.championcompanion.core.model.DailySummary
import br.com.mwconsulting.championcompanion.core.model.DailySummaryRepository
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayload
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadId
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadRepository
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperation
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationId
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRepository
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.HeartRateRepository
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.SafeSyncError
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibration
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibrationRepository
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImport
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi

@Singleton
class RoomRepositories @Inject constructor(
    private val database: ChampionDatabase,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DeviceRepository, DailySummaryRepository, HeartRateRepository, WorkoutActivityRepository,
    TreadmillCalibrationRepository, AppWorkoutSessionRepository, HealthSyncRepository, DiagnosticPayloadRepository {
    private val ids = UuidV7Generator()

    override fun observeAssociatedDevice(): Flow<Device?> = database.deviceAssociationDao().observeAssociated()
        .map { it?.toDomain() }
        .flowOn(ioDispatcher)

    override suspend fun associateDevice(device: Device): RepositoryResult<Device> = write {
        val deviceDao = database.deviceDao()
        val associationDao = database.deviceAssociationDao()
        val existing = deviceDao.findByPlatformIdentifier(device.platformIdentifier)
        val entity = device.toEntity()
        val persisted = if (existing == null) {
            deviceDao.insert(entity)
            entity
        } else {
            val merged = entity.copy(
                id = existing.id,
                lastSuccessfulConnectionAtEpochMillis = entity.lastSuccessfulConnectionAtEpochMillis
                    ?: existing.lastSuccessfulConnectionAtEpochMillis,
                lastImportedAtEpochMillis = entity.lastImportedAtEpochMillis ?: existing.lastImportedAtEpochMillis,
                importParserVersion = entity.importParserVersion ?: existing.importParserVersion,
            )
            deviceDao.update(merged)
            merged
        }
        associationDao.upsert(
            persisted.toAssociationEntity(
                associatedAtEpochMillis = device.associatedAt.toEpochMilli(),
                updatedAtEpochMillis = device.associatedAt.toEpochMilli(),
            ),
        )
        val mapped = persisted.toDomain(associatedAtEpochMillis = device.associatedAt.toEpochMilli())
            ?: return@write RepositoryResult.UnexpectedState
        RepositoryResult.Success(mapped)
    }

    override suspend fun associateDeviceAndUpdateLastSuccessfulConnection(
        device: Device,
        connectedAt: Instant,
    ): RepositoryResult<Device> = write {
        val associatedAtEpochMillis = device.associatedAt.toEpochMilli()
        val persisted = upsertAssociatedDevice(
            device = device,
            associatedAtEpochMillis = associatedAtEpochMillis,
            connectedAtEpochMillis = connectedAt.toEpochMilli(),
        ) ?: return@write RepositoryResult.UnexpectedState
        val mapped = persisted.toDomain(associatedAtEpochMillis = associatedAtEpochMillis)
            ?: return@write RepositoryResult.UnexpectedState
        RepositoryResult.Success(mapped)
    }

    override suspend fun updateLastSuccessfulConnection(
        deviceId: DeviceId,
        connectedAt: Instant,
    ): RepositoryResult<Device> = write {
        val existing = database.deviceDao().findById(deviceId.value.toString()) ?: return@write RepositoryResult.NotFound
        val updated = existing.copy(lastSuccessfulConnectionAtEpochMillis = connectedAt.toEpochMilli())
        database.deviceDao().update(updated)
        val associatedAt = database.deviceAssociationDao().findById(PRIMARY_DEVICE_ASSOCIATION_ID)
            ?.takeIf { it.deviceId == updated.id }
            ?.associatedAtEpochMillis
            ?: updated.associatedAtEpochMillis
        val mapped = updated.toDomain(associatedAtEpochMillis = associatedAt)
            ?: return@write RepositoryResult.UnexpectedState
        RepositoryResult.Success(mapped)
    }

    override suspend fun updateKnownMetadata(
        deviceId: DeviceId,
        name: String,
        firmware: String?,
    ): RepositoryResult<Device> = write {
        val existing = database.deviceDao().findById(deviceId.value.toString()) ?: return@write RepositoryResult.NotFound
        val updated = existing.copy(name = name, firmware = firmware)
        database.deviceDao().update(updated)
        val associatedAt = database.deviceAssociationDao().findById(PRIMARY_DEVICE_ASSOCIATION_ID)
            ?.takeIf { it.deviceId == updated.id }
            ?.associatedAtEpochMillis
            ?: updated.associatedAtEpochMillis
        val mapped = updated.toDomain(associatedAtEpochMillis = associatedAt)
            ?: return@write RepositoryResult.UnexpectedState
        RepositoryResult.Success(mapped)
    }

    override suspend fun forgetAssociatedDevice(): RepositoryResult<Unit> = write {
        database.deviceAssociationDao().deleteById(PRIMARY_DEVICE_ASSOCIATION_ID)
        RepositoryResult.Success(Unit)
    }

    override suspend fun findByPlatformIdentifier(platformIdentifier: String): RepositoryResult<Device> = withContext(ioDispatcher) {
        val mapped = database.deviceDao().findByPlatformIdentifier(platformIdentifier)
            ?.toDomain()
            ?: return@withContext RepositoryResult.NotFound
        RepositoryResult.Success(mapped)
    }

    override fun observeSummaries(from: LocalDate, to: LocalDate): Flow<List<DailySummary>> =
        database.dailySummaryDao().observeBetween(from.toString(), to.toString())
            .map { entities -> entities.mapNotNull { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun save(summary: DailySummary): RepositoryResult<Unit> = write {
        val entity = summary.toEntity()
        val existing = database.dailySummaryDao().findByNaturalKey(entity.deviceId, entity.origin, entity.localDateIso)
        if (existing == null) database.dailySummaryDao().insert(entity)
        else database.dailySummaryDao().update(entity.copy(id = existing.id))
        RepositoryResult.Success(Unit)
    }

    override suspend fun import(measurements: List<HeartRateMeasurement>): RepositoryResult<Unit> = write {
        for (measurement in measurements) {
            val entity = measurement.toEntity()
            if (database.heartRateDao().findByDeduplicationKey(entity.deduplicationKey) == null) {
                database.heartRateDao().insert(entity)
                if (measurement.isEligibleForSync) {
                    database.healthSyncOperationDao().insert(HealthSyncOperationEntity(
                        id = ids.next().toString(), aggregateId = entity.id, aggregateType = "HEART_RATE_MEASUREMENT",
                        recordType = "HEART_RATE", state = "PENDING", attemptCount = 0,
                        lastAttemptAtEpochMillis = null, lastError = null,
                    ))
                }
            }
        }
        RepositoryResult.Success(Unit)
    }

    override suspend fun findPendingSync(): RepositoryResult<List<HeartRateMeasurement>> = withContext(ioDispatcher) {
        RepositoryResult.Success(database.heartRateDao().findValidPendingForSync().mapNotNull { it.toDomain() })
    }

    override suspend fun persistImported(activity: WorkoutActivity, importRecord: WatchWorkoutImport): RepositoryResult<Unit> = write {
        val activityEntity = activity.toEntity()
        val importEntity = importRecord.toEntity()
        val existingImport = database.watchWorkoutImportDao().findByNaturalKey(importEntity.deviceId, importEntity.origin, importEntity.crc)
        if (existingImport == null) {
            database.workoutActivityDao().insert(activityEntity)
            database.watchWorkoutImportDao().insert(importEntity)
        } else {
            val existingActivity = database.workoutActivityDao().findById(existingImport.activityId) ?: return@write RepositoryResult.NotFound
            // Reimport replaces parser output only; user decisions and calibration remain authoritative.
            val refreshed = activityEntity.copy(
                id = existingActivity.id,
                confirmedType = existingActivity.confirmedType,
                workflowState = existingActivity.workflowState,
                reviewedAtEpochMillis = existingActivity.reviewedAtEpochMillis,
                calibrationId = existingActivity.calibrationId,
                calibratedDistanceMeters = existingActivity.calibratedDistanceMeters,
                calibratedAveragePaceSecondsPerKilometer = existingActivity.calibratedAveragePaceSecondsPerKilometer,
                calibratedAverageSpeedMetersPerSecond = existingActivity.calibratedAverageSpeedMetersPerSecond,
            )
            database.workoutActivityDao().update(refreshed)
            database.watchWorkoutImportDao().update(importEntity.copy(id = existingImport.id, activityId = existingActivity.id))
        }
        val persistedId = existingImport?.activityId ?: activityEntity.id
        database.workoutDetailDao().deleteIntervalsForActivity(persistedId)
        database.workoutDetailDao().deleteHeartRateSamplesForActivity(persistedId)
        database.workoutDetailDao().insertIntervals(activity.toIntervals(persistedId, generateIds(activity.intervals.size)))
        database.workoutDetailDao().insertHeartRateSamples(activity.toHeartRateSamples(persistedId, generateIds(activity.heartRateSamples.size)))
        RepositoryResult.Success(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePendingReview(): Flow<List<WorkoutActivity>> = database.workoutActivityDao().observePendingReview()
        .transformLatest { activities -> emit(activities.mapNotNull { activity ->
            val importRecord = database.watchWorkoutImportDao().findByActivityId(activity.id)
            importRecord?.let { activity.toPendingReviewDomain(it, database.workoutDetailDao().findIntervalsForActivity(activity.id), database.workoutDetailDao().findHeartRateSamplesForActivity(activity.id)) }
        }) }
        .flowOn(ioDispatcher)

    override suspend fun review(activityId: WorkoutActivityId, type: WorkoutType, reviewedAt: Instant): RepositoryResult<WorkoutActivity> = write {
        val entity = database.workoutActivityDao().findById(activityId.value.toString()) ?: return@write RepositoryResult.NotFound
        val importRecord = database.watchWorkoutImportDao().findByActivityId(entity.id) ?: return@write RepositoryResult.NotFound
        val activity = entity.toPendingReviewDomain(importRecord, database.workoutDetailDao().findIntervalsForActivity(entity.id), database.workoutDetailDao().findHeartRateSamplesForActivity(entity.id))
            ?: return@write RepositoryResult.UnexpectedState
        val reviewed = when (val result = activity.confirmReview(type, reviewedAt)) {
            is br.com.mwconsulting.championcompanion.core.model.DomainResult.Success -> result.value
            is br.com.mwconsulting.championcompanion.core.model.DomainResult.Failure -> return@write RepositoryResult.UnexpectedState
        }
        database.workoutActivityDao().update(reviewed.toEntity())
        RepositoryResult.Success(reviewed)
    }

    override suspend fun discard(activityId: WorkoutActivityId): RepositoryResult<Unit> = write {
        when (database.workoutActivityDao().transitionState(activityId.value.toString(), listOf("PENDING_REVIEW", "PENDING_CALIBRATION", "READY_TO_SYNC"), "DISCARDED")) {
            1 -> RepositoryResult.Success(Unit)
            else -> if (database.workoutActivityDao().findById(activityId.value.toString()) == null) RepositoryResult.NotFound else RepositoryResult.UnexpectedState
        }
    }

    override suspend fun register(calibration: TreadmillCalibration): RepositoryResult<Unit> = write {
        val entity = calibration.toEntity() ?: return@write RepositoryResult.UnexpectedState
        val activity = database.workoutActivityDao().findById(entity.activityId) ?: return@write RepositoryResult.NotFound
        if (activity.workflowState != "PENDING_CALIBRATION") return@write RepositoryResult.UnexpectedState
        database.treadmillCalibrationDao().insert(entity)
        database.workoutActivityDao().update(activity.copy(
            calibratedDistanceMeters = entity.calibratedDistanceMeters,
            calibratedAveragePaceSecondsPerKilometer = entity.averagePaceSecondsPerKilometer,
            calibratedAverageSpeedMetersPerSecond = entity.averageSpeedMetersPerSecond,
            calibrationId = entity.id,
            workflowState = "READY_TO_SYNC",
        ))
        RepositoryResult.Success(Unit)
    }

    override fun observeRecoverable(): Flow<List<AppWorkoutSession>> = database.appWorkoutSessionDao().observeRecoverable()
        .map { it.mapNotNull { entity -> entity.toDomain() } }
        .flowOn(ioDispatcher)

    override suspend fun recover(sessionId: AppWorkoutSessionId, at: Instant): RepositoryResult<AppWorkoutSession> = write {
        val entity = database.appWorkoutSessionDao().findById(sessionId.value.toString()) ?: return@write RepositoryResult.NotFound
        if (entity.state != "AWAITING_RECOVERY") return@write RepositoryResult.UnexpectedState
        val updated = entity.copy(state = "ACTIVE", activeSinceEpochMillis = at.toEpochMilli())
        database.appWorkoutSessionDao().update(updated)
        updated.toDomain()?.let { RepositoryResult.Success(it) } ?: RepositoryResult.UnexpectedState
    }

    override suspend fun finish(sessionId: AppWorkoutSessionId, activityId: WorkoutActivityId, at: Instant): RepositoryResult<AppWorkoutSession> = write {
        val entity = database.appWorkoutSessionDao().findById(sessionId.value.toString()) ?: return@write RepositoryResult.NotFound
        if (database.workoutActivityDao().findById(activityId.value.toString()) == null) return@write RepositoryResult.NotFound
        if (entity.state !in setOf("ACTIVE", "PAUSED", "AWAITING_RECOVERY")) return@write RepositoryResult.UnexpectedState
        val updated = entity.copy(state = "FINISHED", endedAtEpochMillis = at.toEpochMilli(), activeSinceEpochMillis = null, pausedSinceEpochMillis = null, workoutActivityId = activityId.value.toString())
        database.appWorkoutSessionDao().update(updated)
        updated.toDomain()?.let { RepositoryResult.Success(it) } ?: RepositoryResult.UnexpectedState
    }

    override suspend fun findFailed(): RepositoryResult<List<HealthSyncOperation>> = withContext(ioDispatcher) {
        RepositoryResult.Success(database.healthSyncOperationDao().findFailed().mapNotNull { it.toDomain() })
    }

    override suspend fun markStarted(operationId: HealthSyncOperationId, at: Instant): RepositoryResult<Unit> = transitionSync(operationId, listOf("PENDING", "FAILED"), "SYNCING", at, null, 1)
    override suspend fun markSynced(operationId: HealthSyncOperationId): RepositoryResult<Unit> = transitionSync(operationId, listOf("SYNCING"), "SYNCED", null, null)
    override suspend fun markFailed(operationId: HealthSyncOperationId, error: SafeSyncError): RepositoryResult<Unit> = transitionSync(operationId, listOf("SYNCING"), "FAILED", null, error.name)

    override suspend fun store(payload: DiagnosticPayload): RepositoryResult<Unit> = write {
        database.diagnosticPayloadDao().insert(payload.toEntity())
        RepositoryResult.Success(Unit)
    }

    override suspend fun reprocess(payloadId: DiagnosticPayloadId): RepositoryResult<Unit> = write {
        if (database.diagnosticPayloadDao().findById(payloadId.value.toString()) == null) RepositoryResult.NotFound
        else RepositoryResult.Success(Unit) // Parser execution belongs to future protocol phase; persisted outputs remain idempotent.
    }

    override suspend fun clearExpired(now: Instant): RepositoryResult<Int> = write {
        val payloadDao = database.diagnosticPayloadDao()
        val expired = payloadDao.findExpired(now.minus(br.com.mwconsulting.championcompanion.core.model.DiagnosticRetention.maximumAge).toEpochMilli())
        val retained = payloadDao.findOldestFirst().filterNot { candidate -> expired.any { it.id == candidate.id } }.toMutableList()
        var bytes = retained.sumOf { it.byteCount }
        val overLimit = buildList {
            for (payload in retained) {
                if (bytes <= br.com.mwconsulting.championcompanion.core.model.DiagnosticRetention.maximumBytes) break
                add(payload.id)
                bytes -= payload.byteCount
            }
        }
        val ids = (expired.map { it.id } + overLimit).distinct()
        RepositoryResult.Success(if (ids.isEmpty()) 0 else payloadDao.deleteByIds(ids))
    }

    private suspend fun upsertAssociatedDevice(
        device: Device,
        associatedAtEpochMillis: Long,
        connectedAtEpochMillis: Long? = null,
    ): DeviceEntity = with(database) {
        val deviceDao = deviceDao()
        val associationDao = deviceAssociationDao()
        val existing = deviceDao.findByPlatformIdentifier(device.platformIdentifier)
        val entity = device.toEntity()
        val persisted = if (existing == null) {
            val inserted = entity.copy(lastSuccessfulConnectionAtEpochMillis = connectedAtEpochMillis ?: entity.lastSuccessfulConnectionAtEpochMillis)
            deviceDao.insert(inserted)
            inserted
        } else {
            val merged = entity.copy(
                id = existing.id,
                lastSuccessfulConnectionAtEpochMillis = connectedAtEpochMillis
                    ?: entity.lastSuccessfulConnectionAtEpochMillis
                    ?: existing.lastSuccessfulConnectionAtEpochMillis,
                lastImportedAtEpochMillis = entity.lastImportedAtEpochMillis ?: existing.lastImportedAtEpochMillis,
                importParserVersion = entity.importParserVersion ?: existing.importParserVersion,
            )
            deviceDao.update(merged)
            merged
        }
        associationDao.upsert(
            persisted.toAssociationEntity(
                associatedAtEpochMillis = associatedAtEpochMillis,
                updatedAtEpochMillis = associatedAtEpochMillis,
            ),
        )
        persisted
    }

    private suspend fun transitionSync(id: HealthSyncOperationId, expected: List<String>, next: String, at: Instant?, error: String?, increment: Int = 0): RepositoryResult<Unit> = write {
        val operation = database.healthSyncOperationDao().findById(id.value.toString())
        if (operation == null) return@write RepositoryResult.NotFound
        if (operation.aggregateType != "HEART_RATE_MEASUREMENT") return@write RepositoryResult.UnexpectedState
        // Sync aggregate identity has no FK. Confirm subject exists before every transition.
        if (database.heartRateDao().findById(operation.aggregateId) == null) return@write RepositoryResult.NotFound
        if (database.healthSyncOperationDao().transitionState(operation.id, expected, next, at?.toEpochMilli(), error, increment) == 1) RepositoryResult.Success(Unit)
        else RepositoryResult.UnexpectedState
    }

    private suspend fun <T> write(block: suspend () -> RepositoryResult<T>): RepositoryResult<T> = withContext(ioDispatcher) {
        database.withWriteTransaction { block() }
    }

    private fun generateIds(count: Int): Iterator<java.util.UUID> = List(count) { ids.next() }.iterator()
}
