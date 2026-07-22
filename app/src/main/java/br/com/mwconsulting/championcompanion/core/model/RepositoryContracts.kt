package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

sealed interface RepositoryResult<out T> {
    data class Success<T>(val value: T) : RepositoryResult<T>
    data object Conflict : RepositoryResult<Nothing>
    data object UnexpectedState : RepositoryResult<Nothing>
    data object NotFound : RepositoryResult<Nothing>
    data object StorageUnavailable : RepositoryResult<Nothing>
}

interface DeviceRepository {
    fun observeAssociatedDevice(): Flow<Device?>
    suspend fun associateDevice(device: Device): RepositoryResult<Device>
    suspend fun updateLastSuccessfulConnection(deviceId: DeviceId, connectedAt: Instant): RepositoryResult<Device>
    suspend fun updateKnownMetadata(deviceId: DeviceId, name: String, firmware: String?): RepositoryResult<Device>
    suspend fun forgetAssociatedDevice(): RepositoryResult<Unit>
    suspend fun findByPlatformIdentifier(platformIdentifier: String): RepositoryResult<Device>
}

interface DailySummaryRepository {
    fun observeSummaries(from: LocalDate, to: LocalDate): Flow<List<DailySummary>>
    suspend fun save(summary: DailySummary): RepositoryResult<Unit>
}

interface HeartRateRepository {
    suspend fun import(measurements: List<HeartRateMeasurement>): RepositoryResult<Unit>
    suspend fun findPendingSync(): RepositoryResult<List<HeartRateMeasurement>>
}

interface WorkoutActivityRepository {
    suspend fun persistImported(activity: WorkoutActivity, importRecord: WatchWorkoutImport): RepositoryResult<Unit>
    fun observePendingReview(): Flow<List<WorkoutActivity>>
    suspend fun review(activityId: WorkoutActivityId, type: WorkoutType, reviewedAt: Instant): RepositoryResult<WorkoutActivity>
    suspend fun discard(activityId: WorkoutActivityId): RepositoryResult<Unit>
}

interface TreadmillCalibrationRepository {
    suspend fun register(calibration: TreadmillCalibration): RepositoryResult<Unit>
}

interface AppWorkoutSessionRepository {
    fun observeRecoverable(): Flow<List<AppWorkoutSession>>
    suspend fun recover(sessionId: AppWorkoutSessionId, at: Instant): RepositoryResult<AppWorkoutSession>
    suspend fun finish(sessionId: AppWorkoutSessionId, activityId: WorkoutActivityId, at: Instant): RepositoryResult<AppWorkoutSession>
}

interface HealthSyncRepository {
    suspend fun findFailed(): RepositoryResult<List<HealthSyncOperation>>
    suspend fun markStarted(operationId: HealthSyncOperationId, at: Instant): RepositoryResult<Unit>
    suspend fun markSynced(operationId: HealthSyncOperationId): RepositoryResult<Unit>
    suspend fun markFailed(operationId: HealthSyncOperationId, error: SafeSyncError): RepositoryResult<Unit>
}

interface DiagnosticPayloadRepository {
    suspend fun store(payload: DiagnosticPayload): RepositoryResult<Unit>
    suspend fun reprocess(payloadId: DiagnosticPayloadId): RepositoryResult<Unit>
    suspend fun clearExpired(now: Instant): RepositoryResult<Int>
}

interface AppPreferencesRepository {
    fun observe(): Flow<AppPreferences>
    suspend fun update(preferences: AppPreferences): RepositoryResult<Unit>
}
