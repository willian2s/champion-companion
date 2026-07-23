package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

const val DEFAULT_DAILY_STEPS_GOAL = 8_000

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
    suspend fun associateDeviceAndUpdateLastSuccessfulConnection(
        device: Device,
        connectedAt: Instant,
    ): RepositoryResult<Device>
    suspend fun updateLastSuccessfulConnection(deviceId: DeviceId, connectedAt: Instant): RepositoryResult<Device>
    suspend fun updateImportMetadata(
        deviceId: DeviceId,
        importedAt: Instant,
        parserVersion: ParserVersion,
    ): RepositoryResult<Device> = RepositoryResult.UnexpectedState
    suspend fun updateKnownMetadata(deviceId: DeviceId, name: String, firmware: String?): RepositoryResult<Device>
    suspend fun forgetAssociatedDevice(): RepositoryResult<Unit>
    suspend fun findByPlatformIdentifier(platformIdentifier: String): RepositoryResult<Device>
}

interface DailySummaryRepository {
    fun observeSummaries(from: LocalDate, to: LocalDate): Flow<List<DailySummary>>
    fun observeSummaryForDate(localDate: LocalDate): Flow<DailySummary?> =
        observeSummaries(localDate, localDate).map { it.firstOrNull() }
    suspend fun save(summary: DailySummary): RepositoryResult<Unit>
    suspend fun mergeWatchDailySummary(input: MergeWatchDailySummaryInput): RepositoryResult<DailySummary> =
        RepositoryResult.UnexpectedState
}

data class MergeWatchDailySummaryInput(
    val deviceId: DeviceId,
    val localDate: LocalDate,
    val incomingSteps: Long,
    val collectedAt: Instant,
    val updatedAt: Instant,
    val estimatedDistanceMeters: Double?,
    val estimatedCaloriesKilocalories: Double?,
    val parserVersion: ParserVersion,
)

interface PersonalMetricsRepository {
    fun observeSnapshot(): Flow<PersonalMetricsSnapshot>
}

data class PersonalMetricsSnapshot(
    val heightCentimeters: Double? = null,
    val weightKilograms: Double? = null,
    val latestWeightMeasuredAt: Instant? = null,
    val latestWeightOrigin: DataOrigin? = null,
    val manualStrideLengthMeters: Double? = null,
)

interface DailyStepsSyncCoordinator {
    suspend fun refresh(request: DailyStepsRefreshRequest): DailyStepsRefreshOutcome
}

sealed interface DailyStepsRefreshOutcome {
    data class Success(val value: DailyStepsRefreshResult) : DailyStepsRefreshOutcome
    data class Failure(val reason: DailyStepsRefreshFailure) : DailyStepsRefreshOutcome
}

enum class DailyStepsRefreshFailure {
    DEVICE_NOT_FOUND,
    CONNECTION_FAILED,
    TIMEOUT,
    INVALID_PACKET,
    CANCELLED,
    INTERNAL,
}

data class DailyStepsRefreshRequest(
    val attempt: DailySyncAttempt,
    val associatedDeviceId: DeviceId,
    val associatedDevicePlatformIdentifier: String,
    val zoneId: ZoneId,
)

data class DailySyncAttempt(
    val refreshAttemptId: String,
    val sessionId: String? = null,
    val startedAt: Instant,
) {
    fun accepts(packet: FreshPacketCandidate): Boolean =
        sessionId != null && packet.sessionId == sessionId && !packet.observedAt.isBefore(startedAt)
}

data class FreshPacketCandidate(
    val sessionId: String,
    val observedAt: Instant,
    val channelName: String,
)

data class DailyStepsRefreshResult(
    val attempt: DailySyncAttempt,
    val deviceId: DeviceId,
    val localDate: LocalDate,
    val steps: Long,
    val collectedAt: Instant,
    val parserVersion: ParserVersion,
)

enum class ForegroundBleOperationOwner {
    DAILY_STEPS_REFRESH,
    MANUAL_HEART_RATE,
    WATCH_D1_HISTORY,
    WATCH_WORKOUT_IMPORT,
}

data class ForegroundBleOperationRequest(
    val owner: ForegroundBleOperationOwner,
    val operationId: String,
    val requestedAt: Instant,
)

data class ForegroundBleOperationPermit(
    val owner: ForegroundBleOperationOwner,
    val operationId: String,
    val acquiredAt: Instant,
)

data class ForegroundBleOperationSnapshot(
    val owner: ForegroundBleOperationOwner,
    val operationId: String,
    val acquiredAt: Instant,
)

sealed interface ForegroundBleOperationAcquireResult {
    data class Acquired(
        val permit: ForegroundBleOperationPermit,
    ) : ForegroundBleOperationAcquireResult

    data class Rejected(
        val activeOperation: ForegroundBleOperationSnapshot,
    ) : ForegroundBleOperationAcquireResult
}

interface ForegroundBleOperationGate {
    fun observeActiveOperation(): Flow<ForegroundBleOperationSnapshot?> = flowOf(null)

    suspend fun tryAcquire(request: ForegroundBleOperationRequest): ForegroundBleOperationAcquireResult

    suspend fun release(permit: ForegroundBleOperationPermit) {}
}

data class SaveManualHeartRateMeasurementInput(
    val measurementId: HeartRateMeasurementId,
    val sessionId: HeartRateMeasurementSessionId,
    val deviceId: DeviceId?,
    val bpm: Int,
    val measuredAt: Instant,
    val parserVersion: ParserVersion?,
)

data class SaveD1HeartRateMeasurementsInput(
    val measurements: List<HeartRateMeasurement>,
    val metadata: D1HeartRateImportMetadata,
)

interface HeartRateRepository {
    suspend fun import(measurements: List<HeartRateMeasurement>): RepositoryResult<Unit>
    suspend fun findPendingSync(): RepositoryResult<List<HeartRateMeasurement>>
    suspend fun saveManualMeasurement(input: SaveManualHeartRateMeasurementInput): RepositoryResult<HeartRateMeasurement> =
        RepositoryResult.UnexpectedState

    fun observeLatestValidMeasurement(): Flow<HeartRateMeasurement?> = flowOf(null)

    fun observeRecentManualMeasurements(limit: Int): Flow<List<HeartRateMeasurement>> = flowOf(emptyList())

    suspend fun importD1Measurements(
        input: SaveD1HeartRateMeasurementsInput,
    ): RepositoryResult<D1HeartRateImportPersistenceResult> = RepositoryResult.UnexpectedState

    fun observeHistory(query: HeartRateHistoryQuery): Flow<HeartRateHistorySnapshot> = flowOf(
        HeartRateHistorySnapshot(period = query.period),
    )

    fun observeD1ImportMetadata(deviceId: DeviceId): Flow<D1HeartRateImportMetadata?> = flowOf(null)
}

interface WorkoutActivityRepository {
    suspend fun persistImported(activity: WorkoutActivity, importRecord: WatchWorkoutImport): RepositoryResult<Unit>
    fun observePendingReview(): Flow<List<WorkoutActivity>>
    fun observeWatchWorkoutImportMetadata(deviceId: DeviceId): Flow<WatchWorkoutImportMetadata?> = flowOf(null)
    suspend fun updateWatchWorkoutImportMetadata(metadata: WatchWorkoutImportMetadata): RepositoryResult<Unit> = RepositoryResult.UnexpectedState
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
    fun observeDailyStepsGoal(): Flow<Int>
    suspend fun update(preferences: AppPreferences): RepositoryResult<Unit>
    suspend fun updateDailyStepsGoal(goal: Int): RepositoryResult<Unit>
}
