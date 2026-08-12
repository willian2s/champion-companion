package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

const val DEFAULT_DAILY_STEPS_GOAL = 8_000
const val DEFAULT_WORKOUT_COUNTDOWN_SECONDS = 5

object WorkoutCountdownPolicy {
    val allowedSeconds: Set<Int> = setOf(0, 3, 5, 10)

    fun sanitize(seconds: Int): Int = seconds.takeIf(allowedSeconds::contains)
        ?: DEFAULT_WORKOUT_COUNTDOWN_SECONDS
}

sealed interface RepositoryResult<out T> {
    data class Success<T>(val value: T) : RepositoryResult<T>
    data object Conflict : RepositoryResult<Nothing>
    data object UnexpectedState : RepositoryResult<Nothing>
    data object NotFound : RepositoryResult<Nothing>
    data object StorageUnavailable : RepositoryResult<Nothing>
}

/** Motivos seguros para bloquear reprocessamento local de treino do relógio. */
enum class WatchWorkoutReprocessFailureReason {
    ACTIVITY_NOT_FOUND,
    NOT_WATCH_WORKOUT,
    IMPORT_METADATA_MISSING,
    UNKNOWN_PARSER_VERSION,
    RAW_INTERVALS_MISSING,
    RAW_INTERVAL_INVALID,
    MEASURED_STEPS_MISSING,
    INDEX_INELIGIBLE,
    STEP_LENGTH_PROFILE_INVALID,
    TEMPORAL_INCONSISTENCY,
    SYNC_IN_PROGRESS,
    SYNCED_REQUIRES_FUTURE_PROJECTION,
    DERIVATION_INCONSISTENT,
    STORAGE_UNAVAILABLE,
}

sealed interface WatchWorkoutReprocessResult {
    data class Success(
        val snapshot: WorkoutDerivationSnapshot,
    ) : WatchWorkoutReprocessResult

    data class Blocked(
        val reason: WatchWorkoutReprocessFailureReason,
    ) : WatchWorkoutReprocessResult
}

enum class WatchWorkoutPlausibilityAcceptanceFailureReason {
    ACTIVITY_NOT_FOUND,
    NOT_WATCH_WORKOUT,
    DERIVATION_NOT_MATERIALIZED,
    SERIES_INELIGIBLE,
    INDICATORS_PRESENT,
    SYNC_IN_PROGRESS,
    SYNCED_BLOCKED,
    INVALID_WORKFLOW_STATE,
    STORAGE_UNAVAILABLE,
}

sealed interface WatchWorkoutPlausibilityAcceptanceResult {
    data class Accepted(
        val activity: WorkoutActivity,
    ) : WatchWorkoutPlausibilityAcceptanceResult

    data class Blocked(
        val reason: WatchWorkoutPlausibilityAcceptanceFailureReason,
    ) : WatchWorkoutPlausibilityAcceptanceResult
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
    /** API aditiva semiaberta; contrato inclusivo antigo permanece disponível. */
    fun observeSummariesBetween(range: CalendarDateRange): Flow<List<DailySummary>> =
        observeSummaries(range.fromInclusive, range.untilExclusive.minusDays(1))
    fun observeSummaryForDate(localDate: LocalDate): Flow<DailySummary?> =
        observeSummaries(localDate, localDate).map { it.firstOrNull() }
    suspend fun findById(summaryId: DailySummaryId): RepositoryResult<DailySummary> = RepositoryResult.UnexpectedState
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
    val zoneId: ZoneId = ZoneId.systemDefault(),
)

interface PersonalMetricsRepository {
    fun observeSnapshot(): Flow<PersonalMetricsSnapshot>
    /** Pesos cujo measuredAt pertence ao intervalo semiaberto informado. */
    fun observeWeightsBetween(range: CalendarInstantRange): Flow<List<CalendarWeightMeasurementSummary>> =
        flowOf(emptyList())
    /** Histórico persistido, da medição mais recente para a mais antiga. */
    fun observeWeightHistory(): Flow<List<WeightMeasurement>> = flowOf(emptyList())
    /** Lista pontual do mesmo histórico observado, para consumidores não reativos. */
    suspend fun listWeightHistory(): RepositoryResult<List<WeightMeasurement>> =
        RepositoryResult.UnexpectedState
    suspend fun saveManualWeightMeasurement(
        input: SaveManualWeightMeasurementInput,
    ): RepositoryResult<WeightMeasurement> = RepositoryResult.UnexpectedState
    suspend fun updateHeight(input: UpdateHeightInput): RepositoryResult<UserProfile> =
        RepositoryResult.UnexpectedState
    suspend fun updateStepLength(input: UpdateStepLengthInput): RepositoryResult<UserProfile> =
        RepositoryResult.UnexpectedState
    suspend fun findProfileById(profileId: UserProfileId): RepositoryResult<UserProfile> = RepositoryResult.UnexpectedState
    suspend fun findWeightById(measurementId: WeightMeasurementId): RepositoryResult<WeightMeasurement> = RepositoryResult.UnexpectedState

    fun observeStepLengthCalibrations(workoutType: WorkoutType): Flow<List<StepLengthCalibrationSummary>> =
        flowOf(emptyList())

    fun observeLearnedStepLengthProfiles(): Flow<List<LearnedStepLengthProfile>> = flowOf(emptyList())

    suspend fun saveStepLengthCalibration(
        input: SaveStepLengthCalibrationInput,
    ): RepositoryResult<StepLengthCalibration> = RepositoryResult.UnexpectedState

    suspend fun removeStepLengthCalibration(activityId: WorkoutActivityId): RepositoryResult<Unit> =
        RepositoryResult.UnexpectedState

    /**
     * Resolve perfil para cálculos baseados em passos. [WorkoutType.CYCLING]
     * retorna [RepositoryResult.NotFound], pois bicicleta nunca usa distância
     * derivada de passos.
     */
    suspend fun resolveEffectiveStepLength(workoutType: WorkoutType): RepositoryResult<StepLengthProfile> =
        RepositoryResult.UnexpectedState
}

data class SaveStepLengthCalibrationInput(
    val profileId: UserProfileId,
    val activityId: WorkoutActivityId,
    val workoutType: WorkoutType,
    val referenceDistanceMeters: Double,
    /**
     * Valor informado pelo consumidor é somente uma pré-condição de contrato;
     * repository valida contra soma canônica persistida da série D4.
     */
    val decodedSteps: Long,
    val acceptedAt: Instant,
    val updatedAt: Instant = acceptedAt,
)

data class UpdateStepLengthInput(
    val stepLengthMeters: Double,
    val source: StepLengthSource,
    val updatedAt: Instant,
)

data class UpdateHeightInput(
    val heightCentimeters: Double?,
    val updatedAt: Instant,
)

data class SaveManualWeightMeasurementInput(
    val valueKilograms: Double,
    val measuredAt: Instant = Instant.now(),
)

data class PersonalMetricsSnapshot(
    val heightCentimeters: Double? = null,
    val weightKilograms: Double? = null,
    val latestWeightMeasuredAt: Instant? = null,
    val latestWeightOrigin: DataOrigin? = null,
    val manualStrideLengthMeters: Double? = null,
    /** Perfil novo; [manualStrideLengthMeters] permanece somente como ponte legada. */
    val stepLengthProfile: StepLengthProfile? = null,
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
    APP_WORKOUT_SESSION,
    DEVICE_CONNECTION,
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
    suspend fun findById(measurementId: HeartRateMeasurementId): RepositoryResult<HeartRateMeasurement> = RepositoryResult.UnexpectedState
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

    fun observeMeasurementsBetween(
        range: CalendarInstantRange,
        sources: Set<HeartRateSource> = CALENDAR_HEART_RATE_SOURCES,
    ): Flow<List<HeartRateMeasurement>> = flowOf(emptyList())

    fun observeD1ImportMetadata(deviceId: DeviceId): Flow<D1HeartRateImportMetadata?> = flowOf(null)
}

interface WorkoutActivityRepository {
    suspend fun persistImported(activity: WorkoutActivity, importRecord: WatchWorkoutImport): RepositoryResult<Unit>
    fun observePendingReview(): Flow<List<WorkoutActivity>> = observeActivities(WorkoutActivityFilter.PENDING_REVIEW)
    fun observeActivities(filter: WorkoutActivityFilter): Flow<List<WorkoutActivity>> = flowOf(emptyList())
    /** Atividades não descartadas cujo startedAt pertence ao intervalo semiaberto. */
    fun observeActivitiesBetween(range: CalendarInstantRange): Flow<List<CalendarWorkoutSummary>> =
        flowOf(emptyList())
    fun observeActivity(activityId: WorkoutActivityId): Flow<WorkoutActivity?> = flowOf(null)
    suspend fun findById(activityId: WorkoutActivityId): RepositoryResult<WorkoutActivity> = RepositoryResult.UnexpectedState
    fun observeWorkflowSummary(): Flow<WorkoutWorkflowSummary> = flowOf(WorkoutWorkflowSummary())
    suspend fun findWatchWorkoutImport(deviceId: DeviceId, crc: Int): RepositoryResult<WatchWorkoutImportLookup?> = RepositoryResult.UnexpectedState
    fun observeWatchWorkoutImportMetadata(deviceId: DeviceId): Flow<WatchWorkoutImportMetadata?> = flowOf(null)
    suspend fun updateWatchWorkoutImportMetadata(metadata: WatchWorkoutImportMetadata): RepositoryResult<Unit> = RepositoryResult.UnexpectedState
    /** Lê somente derivação ativa de atividade originada no relógio. */
    suspend fun findWatchWorkoutDerivation(
        activityId: WorkoutActivityId,
    ): RepositoryResult<WorkoutDerivationSnapshot?> = RepositoryResult.UnexpectedState
    /** Derivação inicial explícita; snapshot ativo existente nunca é substituído. */
    suspend fun deriveWatchWorkout(
        activityId: WorkoutActivityId,
        derivedAt: Instant = Instant.now(),
    ): RepositoryResult<WorkoutDerivationSnapshot?> = RepositoryResult.UnexpectedState
    /** Ação explícita para substituir snapshot quando fatos ou perfil mudarem. */
    suspend fun rederiveWatchWorkout(
        activityId: WorkoutActivityId,
        derivedAt: Instant = Instant.now(),
    ): RepositoryResult<WorkoutDerivationSnapshot?> = RepositoryResult.UnexpectedState
    /** Reprocessamento explícito com motivo seguro quando a derivação falha fechada. */
    suspend fun reprocessWatchWorkout(
        activityId: WorkoutActivityId,
        derivedAt: Instant = Instant.now(),
    ): WatchWorkoutReprocessResult = when (val result = rederiveWatchWorkout(activityId, derivedAt)) {
        is RepositoryResult.Success -> result.value?.let(WatchWorkoutReprocessResult::Success)
            ?: WatchWorkoutReprocessResult.Blocked(WatchWorkoutReprocessFailureReason.DERIVATION_INCONSISTENT)
        RepositoryResult.NotFound -> WatchWorkoutReprocessResult.Blocked(
            WatchWorkoutReprocessFailureReason.ACTIVITY_NOT_FOUND,
        )
        RepositoryResult.StorageUnavailable -> WatchWorkoutReprocessResult.Blocked(
            WatchWorkoutReprocessFailureReason.STORAGE_UNAVAILABLE,
        )
        RepositoryResult.Conflict,
        RepositoryResult.UnexpectedState,
        -> WatchWorkoutReprocessResult.Blocked(WatchWorkoutReprocessFailureReason.DERIVATION_INCONSISTENT)
    }
    suspend fun submitReview(input: SubmitWorkoutReviewInput): RepositoryResult<WorkoutActivity> = RepositoryResult.UnexpectedState
    /** Persiste aceite no snapshot ativo; não recalcula nem prepara fila remota. */
    suspend fun acceptWatchWorkoutPlausibility(
        activityId: WorkoutActivityId,
    ): WatchWorkoutPlausibilityAcceptanceResult = WatchWorkoutPlausibilityAcceptanceResult.Blocked(
        WatchWorkoutPlausibilityAcceptanceFailureReason.INVALID_WORKFLOW_STATE,
    )
    suspend fun review(activityId: WorkoutActivityId, type: WorkoutType, reviewedAt: Instant): RepositoryResult<WorkoutActivity>
    suspend fun startSync(activityId: WorkoutActivityId): RepositoryResult<WorkoutActivity> = RepositoryResult.UnexpectedState
    suspend fun markSynced(activityId: WorkoutActivityId): RepositoryResult<WorkoutActivity> = RepositoryResult.UnexpectedState
    suspend fun markSyncFailed(activityId: WorkoutActivityId): RepositoryResult<WorkoutActivity> = RepositoryResult.UnexpectedState
    suspend fun resetInterruptedSyncing(): RepositoryResult<Int> = RepositoryResult.UnexpectedState
    suspend fun discard(activityId: WorkoutActivityId): RepositoryResult<Unit>
    suspend fun findPhoneCorrelationCandidates(
        fromInclusive: Instant,
        untilExclusive: Instant,
    ): RepositoryResult<List<PhoneWorkoutCorrelationCandidate>> = RepositoryResult.UnexpectedState
    suspend fun mergeCorrelatedWatchImport(
        input: WorkoutCorrelationMergeInput,
    ): RepositoryResult<WorkoutCorrelationMergeResult> = RepositoryResult.UnexpectedState
}

enum class WorkoutActivityFilter {
    ALL,
    PENDING_REVIEW,
    PENDING_CALIBRATION,
    READY_TO_SYNC,
    SYNCED,
    SYNC_FAILED,
    DISCARDED,
}

data class WorkoutWorkflowSummary(
    val pendingReviewCount: Int = 0,
    val pendingCalibrationCount: Int = 0,
    val readyToSyncCount: Int = 0,
    val syncedCount: Int = 0,
    val syncFailedCount: Int = 0,
    val discardedCount: Int = 0,
)

data class SubmitWorkoutReviewInput(
    val activityId: WorkoutActivityId,
    val confirmedType: WorkoutType,
    val title: String?,
    val notes: String?,
    val reviewedAt: Instant,
    /** Distância total manual em metros; aplicável somente a CYCLING. */
    val manualDistanceMeters: Double? = null,
    /** Aceite explícito da série sem indicadores antes da projeção remota. */
    val acceptPlausibility: Boolean = false,
)

data class WatchWorkoutImportLookup(
    val activityId: WorkoutActivityId,
    val importId: WatchWorkoutImportId,
    val deviceId: DeviceId,
    val crc: Int,
    val parserVersion: ParserVersion? = null,
    val parserValidity: ParserValidity,
    val workflowState: ActivityWorkflowState,
    val importedAt: Instant,
) {
    val canSkipOnResume: Boolean
        get() = parserValidity == ParserValidity.VALID && isSupportedWatchWorkoutD4ParserVersion(parserVersion)

    val shouldRetryOnResume: Boolean
        get() = !canSkipOnResume
}

interface TreadmillCalibrationRepository {
    suspend fun register(calibration: TreadmillCalibration): RepositoryResult<Unit>
    suspend fun calibrateImportedActivity(input: CalibrateImportedActivityInput): RepositoryResult<TreadmillCalibrationSnapshot> = RepositoryResult.UnexpectedState
    fun observeLatestCalibration(activityId: WorkoutActivityId): Flow<TreadmillCalibrationSnapshot?> = flowOf(null)
}

data class CalibrateImportedActivityInput(
    val activityId: WorkoutActivityId,
    val reportedMeters: Double,
    val appliedAt: Instant,
)

data class TreadmillCalibrationSnapshot(
    val activityId: WorkoutActivityId,
    val revision: Int,
    val originalDistanceMeters: DomainValue<Double>,
    val estimatedDistanceMeters: DomainValue<Double>,
    val reportedDistanceMeters: DomainValue<Double>,
    val calibratedDistanceMeters: DomainValue<Double>,
    val factor: DomainValue<Double>,
    val appliedAt: Instant,
    val averagePaceSecondsPerKilometer: DomainValue<Double>,
    val averageSpeedMetersPerSecond: DomainValue<Double>,
    val metricsProvenance: MetricProvenance,
)

data class AppWorkoutSessionCheckpoint(
    val sessionId: AppWorkoutSessionId,
    val expectedRevision: Long,
    val runtimeGeneration: Long,
    val at: Instant,
    val aggregates: AppWorkoutSessionAggregates,
    val activeDuration: Duration? = null,
    val pausedDuration: Duration? = null,
) {
    init {
        require(expectedRevision >= 0)
        require(runtimeGeneration >= 0)
        require((activeDuration == null) == (pausedDuration == null))
        require(activeDuration == null || !activeDuration.isNegative)
        require(pausedDuration == null || !pausedDuration.isNegative)
    }
}

data class AppWorkoutSessionMutation(
    val sessionId: AppWorkoutSessionId,
    val expectedRevision: Long,
    val at: Instant,
    val activeDuration: Duration? = null,
    val pausedDuration: Duration? = null,
) {
    init {
        require(expectedRevision >= 0)
        require((activeDuration == null) == (pausedDuration == null))
        require(activeDuration == null || !activeDuration.isNegative)
        require(pausedDuration == null || !pausedDuration.isNegative)
    }
}

data class AssociateWorkoutActivityInput(
    val sessionId: AppWorkoutSessionId,
    val activityId: WorkoutActivityId,
    val expectedRevision: Long,
) {
    init {
        require(expectedRevision >= 0)
    }
}

/**
 * Escritas usam expectedRevision; divergência retorna [RepositoryResult.Conflict].
 * Defaults CAS falham fechados até implementação Room da migration v6.
 */
interface AppWorkoutSessionRepository {
    suspend fun create(session: AppWorkoutSession): RepositoryResult<AppWorkoutSession> = RepositoryResult.Conflict
    suspend fun createStarted(session: AppWorkoutSession, startedAt: Instant): RepositoryResult<AppWorkoutSession> =
        RepositoryResult.Conflict
    fun observeActive(): Flow<AppWorkoutSession?> = flowOf(null)
    /**
     * Superfície persistida para recuperação no próximo app open. Inclui somente
     * `CREATED`, `ACTIVE`, `PAUSED`, `AWAITING_RECOVERY` e `FINISHING`.
     */
    fun observeOpen(): Flow<AppWorkoutSession?> = observeActive()
    fun observeSession(sessionId: AppWorkoutSessionId): Flow<AppWorkoutSession?> = flowOf(null)
    /** Confirma por leitura pontual que a sessão ainda pertence à superfície de recuperação. */
    suspend fun loadOpenForRecovery(sessionId: AppWorkoutSessionId): RepositoryResult<AppWorkoutSession> =
        RepositoryResult.NotFound
    fun observeRecoverable(): Flow<List<AppWorkoutSession>>
    suspend fun start(input: AppWorkoutSessionMutation): RepositoryResult<AppWorkoutSession> = RepositoryResult.Conflict
    suspend fun checkpoint(input: AppWorkoutSessionCheckpoint): RepositoryResult<AppWorkoutSession> = RepositoryResult.Conflict
    suspend fun pause(input: AppWorkoutSessionMutation): RepositoryResult<AppWorkoutSession> = RepositoryResult.Conflict
    suspend fun resume(input: AppWorkoutSessionMutation): RepositoryResult<AppWorkoutSession> = RepositoryResult.Conflict
    suspend fun interrupt(input: AppWorkoutSessionMutation): RepositoryResult<AppWorkoutSession> = RepositoryResult.Conflict
    suspend fun recover(
        sessionId: AppWorkoutSessionId,
        expectedRevision: Long,
        at: Instant,
    ): RepositoryResult<AppWorkoutSession> = RepositoryResult.Conflict
    suspend fun beginFinishing(input: AppWorkoutSessionMutation): RepositoryResult<AppWorkoutSession> =
        RepositoryResult.Conflict
    suspend fun associateActivity(input: AssociateWorkoutActivityInput): RepositoryResult<AppWorkoutSession> =
        RepositoryResult.Conflict
    suspend fun finish(
        sessionId: AppWorkoutSessionId,
        expectedRevision: Long,
        at: Instant,
    ): RepositoryResult<AppWorkoutSession> = RepositoryResult.Conflict
    suspend fun discard(input: AppWorkoutSessionMutation): RepositoryResult<AppWorkoutSession> = RepositoryResult.Conflict

    @Deprecated(
        message = "Escrita sem expectedRevision é proibida; use recover(sessionId, expectedRevision, at)",
        level = DeprecationLevel.ERROR,
    )
    suspend fun recover(sessionId: AppWorkoutSessionId, at: Instant): RepositoryResult<AppWorkoutSession>

    @Deprecated(
        message = "Escrita sem expectedRevision é proibida; use beginFinishing/associateActivity/finish CAS",
        level = DeprecationLevel.ERROR,
    )
    suspend fun finish(
        sessionId: AppWorkoutSessionId,
        activityId: WorkoutActivityId,
        at: Instant,
    ): RepositoryResult<AppWorkoutSession>
}

data class WorkoutRouteAppendCursor(
    val routeId: WorkoutRouteId,
    val lastSequence: Long?,
    val lastSampledAt: Instant?,
    val currentSegmentIndex: Int?,
) {
    init {
        require((lastSequence == null) == (lastSampledAt == null))
        require((lastSequence == null) == (currentSegmentIndex == null))
        require(lastSequence == null || lastSequence >= 0)
        require(currentSegmentIndex == null || currentSegmentIndex >= 0)
    }
}

data class WorkoutRouteRecoverySnapshot(
    val route: WorkoutRoute,
    val cursor: WorkoutRouteAppendCursor,
    val segments: List<WorkoutRouteSegment>,
    val lastAcceptedPoint: WorkoutRoutePoint?,
) {
    init {
        require(cursor.routeId == route.id)
        require((lastAcceptedPoint == null) == (cursor.lastSequence == null))
        require(lastAcceptedPoint == null || lastAcceptedPoint.routeId == route.id)
        require(lastAcceptedPoint?.sequence == cursor.lastSequence)
        require(lastAcceptedPoint?.sampledAt == cursor.lastSampledAt)
        require(lastAcceptedPoint?.segmentIndex == cursor.currentSegmentIndex)
        require(segments.size == route.metrics.segmentCount)
        require(segments.all { it.routeId == route.id })
        require(segments.map { it.index } == segments.indices.toList())
        require(
            if (segments.isEmpty()) {
                cursor.currentSegmentIndex == null
            } else {
                segments.last().index == cursor.currentSegmentIndex &&
                    segments.last().lastSequence == cursor.lastSequence &&
                    segments.last().endedAt == cursor.lastSampledAt
            },
        )
    }
}

data class AppendWorkoutRoutePointsInput(
    val routeId: WorkoutRouteId,
    val sessionId: AppWorkoutSessionId,
    val expectedRevision: Long,
    val runtimeGeneration: Long,
    val cursor: WorkoutRouteAppendCursor,
    val points: List<WorkoutRoutePoint>,
    val segments: List<WorkoutRouteSegment>,
    val metrics: WorkoutRouteMetrics,
) {
    init {
        require(expectedRevision >= 0)
        require(runtimeGeneration >= 0)
        require(points.isNotEmpty())
        require(points.size <= MAX_ROUTE_POINT_BATCH_SIZE)
        require(cursor.routeId == routeId)
        require(points.all { it.routeId == routeId })
        require(points.first().sequence == (cursor.lastSequence ?: -1L) + 1L)
        require(cursor.lastSampledAt == null || points.first().sampledAt.isAfter(cursor.lastSampledAt))
        require(points.zipWithNext().all { (first, second) ->
            second.sequence == first.sequence + 1L && second.sampledAt.isAfter(first.sampledAt) &&
                second.segmentIndex in first.segmentIndex..(first.segmentIndex + 1)
        })
        require(segments.all { it.routeId == routeId })
        require(segments.zipWithNext().all { (first, second) -> second.index == first.index + 1 })
        val currentSegmentIndex = cursor.currentSegmentIndex
        require(
            if (currentSegmentIndex == null) {
                points.first().segmentIndex == 0
            } else {
                points.first().segmentIndex in currentSegmentIndex..(currentSegmentIndex + 1)
            },
        )
        val pointSegmentIndices = points.map { it.segmentIndex }.distinct()
        require(segments.map { it.index } == pointSegmentIndices)
        require(segments.all { segment ->
            val segmentPoints = points.filter { it.segmentIndex == segment.index }
            segmentPoints.isNotEmpty() && segment.firstSequence <= segmentPoints.first().sequence &&
                segment.lastSequence == segmentPoints.last().sequence &&
                !segment.startedAt.isAfter(segmentPoints.first().sampledAt) &&
                segment.endedAt == segmentPoints.last().sampledAt
        })
        require(metrics.acceptedPointCount >= points.size)
        require(metrics.segmentCount >= segments.size)
        require(metrics.segmentCount > points.maxOf { it.segmentIndex })
    }

    companion object {
        const val MAX_ROUTE_POINT_BATCH_SIZE = 32
    }
}

interface WorkoutRouteRepository {
    suspend fun create(route: WorkoutRoute): RepositoryResult<WorkoutRoute>
    fun observeBySession(sessionId: AppWorkoutSessionId): Flow<WorkoutRoute?>

    /**
     * Carrega estado lossless necessário para continuar rota única da sessão.
     * Ausência de rota produz sucesso nulo; persistência inconsistente produz
     * [RepositoryResult.UnexpectedState].
     */
    suspend fun loadRecoverySnapshot(
        sessionId: AppWorkoutSessionId,
    ): RepositoryResult<WorkoutRouteRecoverySnapshot?>
    suspend fun appendAcceptedPoints(input: AppendWorkoutRoutePointsInput): RepositoryResult<WorkoutRoute>
    suspend fun finalize(
        routeId: WorkoutRouteId,
        expectedRevision: Long,
        endedAt: Instant,
    ): RepositoryResult<WorkoutRoute>
    suspend fun deleteBeforeActivity(sessionId: AppWorkoutSessionId): RepositoryResult<Unit>
}

data class HealthSyncSummary(
    val pendingCount: Int = 0,
    val syncingCount: Int = 0,
    val failedCount: Int = 0,
    val syncedCount: Int = 0,
    val lastSyncedAt: Instant? = null,
)

/**
 * Leitura segura da fila para superfícies locais. Flags de integridade nunca
 * carregam identificadores, payloads ou horários de sincronização.
 */
data class HealthSyncQueueObservation(
    val operations: List<HealthSyncOperation> = emptyList(),
    val hasInvalidOperation: Boolean = false,
    val hasUnresolvedAggregate: Boolean = false,
)

data class HealthSyncBacklogReconciliationResult(
    val ensuredOperationCount: Int = 0,
    val repairedOperationCount: Int = 0,
    val dailySummaryCount: Int = 0,
    val heartRateCount: Int = 0,
    val weightCount: Int = 0,
    val heightCount: Int = 0,
)

/** Fila processável com agregados locais que exigem bloqueio fail-closed. */
data class HealthSyncProcessableQueue(
    val operations: List<HealthSyncOperation> = emptyList(),
    val blockedAggregates: Set<HealthSyncAggregate> = emptySet(),
)

interface HealthSyncRepository {
    suspend fun ensureOperation(key: HealthSyncOperationKey): RepositoryResult<HealthSyncOperation>
    /** Persiste um filho congelado sem executar qualquer efeito remoto. */
    suspend fun prepareProjectionChild(
        key: HealthSyncOperationKey,
        action: HealthSyncOperationAction,
        frozenPayload: HealthSyncFrozenPayload,
        clientRecordVersion: Long,
        projectionOrigin: HealthProjectionOrigin,
    ): RepositoryResult<HealthSyncOperation> = RepositoryResult.UnexpectedState
    suspend fun findProcessable(limit: Int = 100): RepositoryResult<List<HealthSyncOperation>>
    suspend fun findProcessableWithIntegrity(
        limit: Int = 100,
    ): RepositoryResult<HealthSyncProcessableQueue> = when (val result = findProcessable(limit)) {
        is RepositoryResult.Success -> RepositoryResult.Success(
            HealthSyncProcessableQueue(operations = result.value),
        )

        RepositoryResult.Conflict -> RepositoryResult.Conflict
        RepositoryResult.UnexpectedState -> RepositoryResult.UnexpectedState
        RepositoryResult.NotFound -> RepositoryResult.NotFound
        RepositoryResult.StorageUnavailable -> RepositoryResult.StorageUnavailable
    }
    fun observeSyncSummary(): Flow<HealthSyncSummary>
    suspend fun findOperationsForAggregate(aggregate: HealthSyncAggregate): RepositoryResult<List<HealthSyncOperation>>

    /** Inspeciona somente identidade local do agregado legado de distância. */
    suspend fun inspectLegacyWorkoutDistanceProjection(
        activityId: WorkoutActivityId,
        activityOrigin: WorkoutActivityOrigin,
    ): RepositoryResult<HealthLegacyDistanceProjectionStatus> = RepositoryResult.UnexpectedState

    /** Persiste tombstone DELETE do agregado legado sem efeito remoto. */
    suspend fun prepareLegacyWorkoutDistanceDeletion(
        activityId: WorkoutActivityId,
    ): RepositoryResult<HealthSyncOperation> = RepositoryResult.UnexpectedState

    /** Valida identidade determinística antes de qualquer DELETE remoto. */
    suspend fun validateDeleteIdentity(
        operation: HealthSyncOperation,
    ): RepositoryResult<Unit> = RepositoryResult.UnexpectedState

    /** Consulta em lote; data civil continua pertencendo ao aggregate, nunca à operação. */
    fun observeForAggregates(aggregates: Set<HealthSyncAggregate>): Flow<List<HealthSyncOperation>> =
        flowOf(emptyList())
    /**
     * Variante aditiva que preserva operações válidas e sinaliza degradação da
     * fila sem atribuir operação órfã a uma data sintética.
     */
    fun observeForAggregatesWithIntegrity(
        aggregates: Set<HealthSyncAggregate>,
    ): Flow<HealthSyncQueueObservation> = observeForAggregates(aggregates).map { operations ->
        HealthSyncQueueObservation(operations = operations)
    }
    suspend fun markStarted(operationId: HealthSyncOperationId, at: Instant): RepositoryResult<Unit>
    /** Inicia somente se a versão ainda corresponder à operação observada. */
    suspend fun markStartedIfCurrent(
        operationId: HealthSyncOperationId,
        expectedClientRecordVersion: Long?,
        at: Instant,
    ): RepositoryResult<Unit> = markStarted(operationId, at)
    suspend fun markSynced(operationId: HealthSyncOperationId, at: Instant): RepositoryResult<Unit>
    suspend fun markFailed(operationId: HealthSyncOperationId, error: SafeSyncError, at: Instant): RepositoryResult<Unit>
    suspend fun resetInterrupted(at: Instant): RepositoryResult<Int>
    suspend fun reconcileBacklog(): RepositoryResult<HealthSyncBacklogReconciliationResult>
}

interface CalendarRepository {
    /** [query] congela mês e ZoneId durante formação de cada emissão. */
    fun observeMonth(query: CalendarMonthQuery): Flow<CalendarMonthSnapshot>

    /** Forma janela do início deste dia ao início do próximo, no ZoneId informado. */
    fun observeDay(localDate: LocalDate, zoneId: ZoneId): Flow<CalendarDaySummary>

    fun observeStepsHistory(query: CalendarHistoryQuery): Flow<CalendarStepsHistorySnapshot>

    fun observeDistanceHistory(query: CalendarHistoryQuery): Flow<CalendarDistanceHistorySnapshot>
}

interface DiagnosticPayloadRepository {
    suspend fun store(payload: DiagnosticPayload): RepositoryResult<Unit>
    suspend fun reprocess(payloadId: DiagnosticPayloadId): RepositoryResult<Unit>
    suspend fun clearExpired(now: Instant): RepositoryResult<Int>
}

interface AppPreferencesRepository {
    fun observe(): Flow<AppPreferences>
    fun observeDailyStepsGoal(): Flow<Int>
    fun observeWorkoutCountdownSeconds(): Flow<Int> = observe().map { it.workoutCountdownSeconds }
    suspend fun update(preferences: AppPreferences): RepositoryResult<Unit>
    suspend fun updateDailyStepsGoal(goal: Int): RepositoryResult<Unit>
    suspend fun updateWorkoutCountdownSeconds(seconds: Int): RepositoryResult<Unit> =
        update(observe().first().copy(workoutCountdownSeconds = seconds))
}
