package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import java.time.ZoneId
import java.util.UUID

enum class HeartRateSource {
    MANUAL_MEASUREMENT,
    MEASUREMENT_STREAM,
    WATCH_D1_HISTORY,
    APP_WORKOUT_SESSION,
    WATCH_WORKOUT_ACTIVITY,
}

@JvmInline
value class HeartRateMeasurementSessionId(val value: UUID)

@JvmInline
value class D1RecordKey private constructor(val value: String) {
    companion object {
        fun create(value: String): DomainResult<D1RecordKey> = when (
            val validation = DomainValidation.requiredText(value)
        ) {
            is DomainResult.Success -> DomainResult.Success(D1RecordKey(validation.value))
            is DomainResult.Failure -> validation
        }
    }
}

sealed interface HeartRateDeduplicationKey {
    data class D1History(
        val deviceId: DeviceId,
        val resolvedMeasuredAt: Instant?,
        val recordKey: D1RecordKey?,
    ) : HeartRateDeduplicationKey

    data class MeasurementSession(
        val sessionId: HeartRateMeasurementSessionId,
    ) : HeartRateDeduplicationKey

    data class AppWorkoutSample(
        val sessionId: AppWorkoutSessionId,
        val measuredAt: Instant,
    ) : HeartRateDeduplicationKey

    data class WatchWorkoutSample(
        val activityId: WorkoutActivityId,
        val measuredAt: Instant,
    ) : HeartRateDeduplicationKey
}

@ConsistentCopyVisibility
data class HeartRateMeasurement private constructor(
    val id: HeartRateMeasurementId,
    val source: HeartRateSource,
    val bpm: Int,
    val measuredAt: Instant?,
    val observedAt: ObservedWatchTime?,
    val validity: DataValidity,
    val deviceId: DeviceId?,
    val importedAt: Instant?,
    val parserVersion: ParserVersion?,
    val appWorkoutSessionId: AppWorkoutSessionId?,
    val workoutActivityId: WorkoutActivityId?,
    val deduplicationKey: HeartRateDeduplicationKey,
) {
    val isEligibleForSync: Boolean
        get() = source == HeartRateSource.WATCH_D1_HISTORY &&
            validity == DataValidity.VALID &&
            measuredAt != null

    companion object {
        fun createManualMeasurement(
            id: HeartRateMeasurementId,
            sessionId: HeartRateMeasurementSessionId,
            deviceId: DeviceId?,
            bpm: Int,
            measuredAt: Instant,
            parserVersion: ParserVersion?,
        ): DomainResult<HeartRateMeasurement> = create(
            id = id,
            source = HeartRateSource.MANUAL_MEASUREMENT,
            bpm = bpm,
            measuredAt = measuredAt,
            validity = DataValidity.VALID,
            deviceId = deviceId,
            parserVersion = parserVersion,
            deduplicationKey = HeartRateDeduplicationKey.MeasurementSession(sessionId),
        )

        fun create(
            id: HeartRateMeasurementId,
            source: HeartRateSource,
            bpm: Int,
            measuredAt: Instant?,
            observedAt: ObservedWatchTime? = null,
            validity: DataValidity,
            deviceId: DeviceId? = null,
            importedAt: Instant? = null,
            parserVersion: ParserVersion? = null,
            appWorkoutSessionId: AppWorkoutSessionId? = null,
            workoutActivityId: WorkoutActivityId? = null,
            deduplicationKey: HeartRateDeduplicationKey,
        ): DomainResult<HeartRateMeasurement> {
            if (bpm !in VALID_BPM_RANGE || (measuredAt == null && observedAt == null)) {
                return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
            if (importedAt != null && measuredAt != null && importedAt.isBefore(measuredAt)) {
                return DomainResult.Failure(DomainValidationError.INVALID_TEMPORAL_ORDER)
            }
            if (!isValidSourceContext(
                    source = source,
                    deviceId = deviceId,
                    parserVersion = parserVersion,
                    appWorkoutSessionId = appWorkoutSessionId,
                    workoutActivityId = workoutActivityId,
                    deduplicationKey = deduplicationKey,
                    measuredAt = measuredAt,
                )
            ) {
                return DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
            }

            return DomainResult.Success(
                HeartRateMeasurement(
                    id = id,
                    source = source,
                    bpm = bpm,
                    measuredAt = measuredAt,
                    observedAt = observedAt,
                    validity = validity,
                    deviceId = deviceId,
                    importedAt = importedAt,
                    parserVersion = parserVersion,
                    appWorkoutSessionId = appWorkoutSessionId,
                    workoutActivityId = workoutActivityId,
                    deduplicationKey = deduplicationKey,
                ),
            )
        }

        private fun isValidSourceContext(
            source: HeartRateSource,
            deviceId: DeviceId?,
            parserVersion: ParserVersion?,
            appWorkoutSessionId: AppWorkoutSessionId?,
            workoutActivityId: WorkoutActivityId?,
            deduplicationKey: HeartRateDeduplicationKey,
            measuredAt: Instant?,
        ): Boolean = when (source) {
            HeartRateSource.WATCH_D1_HISTORY ->
                deviceId != null && parserVersion != null &&
                    appWorkoutSessionId == null && workoutActivityId == null &&
                    deduplicationKey is HeartRateDeduplicationKey.D1History &&
                    deduplicationKey.deviceId == deviceId &&
                    (deduplicationKey.resolvedMeasuredAt != null || deduplicationKey.recordKey != null) &&
                    (measuredAt == null || deduplicationKey.resolvedMeasuredAt == measuredAt)

            HeartRateSource.MANUAL_MEASUREMENT,
            HeartRateSource.MEASUREMENT_STREAM,
            -> appWorkoutSessionId == null && workoutActivityId == null &&
                deduplicationKey is HeartRateDeduplicationKey.MeasurementSession

            HeartRateSource.APP_WORKOUT_SESSION ->
                appWorkoutSessionId != null && workoutActivityId == null && measuredAt != null &&
                    deduplicationKey == HeartRateDeduplicationKey.AppWorkoutSample(appWorkoutSessionId, measuredAt)

            HeartRateSource.WATCH_WORKOUT_ACTIVITY ->
                workoutActivityId != null && appWorkoutSessionId == null && measuredAt != null &&
                    deduplicationKey == HeartRateDeduplicationKey.WatchWorkoutSample(workoutActivityId, measuredAt)
        }

        private val VALID_BPM_RANGE = 30..220
    }
}

enum class HealthSyncRecordType {
    STEPS,
    /** StepsCadenceRecord; provider permission is the shared WRITE_STEPS grant. */
    STEPS_CADENCE,
    HEART_RATE,
    EXERCISE_SESSION,
    DISTANCE,
    ACTIVE_CALORIES_BURNED,
    TOTAL_CALORIES_BURNED,
    WEIGHT,
    HEIGHT,
    SPEED,
}

enum class HealthSyncAggregateType {
    DAILY_SUMMARY,
    WATCH_DAILY_D1_SAMPLE,
    HEART_RATE_MEASUREMENT,
    WORKOUT_ACTIVITY,
    WEIGHT_MEASUREMENT,
    USER_PROFILE_HEIGHT,
}

sealed interface HealthSyncAggregate {
    val id: UUID
    val type: HealthSyncAggregateType

    data class DailySummary(
        val summaryId: DailySummaryId,
    ) : HealthSyncAggregate {
        override val id: UUID = summaryId.value
        override val type: HealthSyncAggregateType = HealthSyncAggregateType.DAILY_SUMMARY
    }

    data class WatchDailyD1Sample(
        val sampleId: WatchDailyD1SampleId,
    ) : HealthSyncAggregate {
        override val id: UUID = sampleId.value
        override val type: HealthSyncAggregateType = HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE
    }

    data class HeartRateMeasurement(
        val measurementId: HeartRateMeasurementId,
    ) : HealthSyncAggregate {
        override val id: UUID = measurementId.value
        override val type: HealthSyncAggregateType = HealthSyncAggregateType.HEART_RATE_MEASUREMENT
    }

    data class WorkoutActivity(
        val activityId: WorkoutActivityId,
    ) : HealthSyncAggregate {
        override val id: UUID = activityId.value
        override val type: HealthSyncAggregateType = HealthSyncAggregateType.WORKOUT_ACTIVITY
    }

    data class WeightMeasurement(
        val measurementId: WeightMeasurementId,
    ) : HealthSyncAggregate {
        override val id: UUID = measurementId.value
        override val type: HealthSyncAggregateType = HealthSyncAggregateType.WEIGHT_MEASUREMENT
    }

    data class UserProfileHeight(
        val profileId: UserProfileId,
    ) : HealthSyncAggregate {
        override val id: UUID = profileId.value
        override val type: HealthSyncAggregateType = HealthSyncAggregateType.USER_PROFILE_HEIGHT
    }
}

data class HealthSyncOperationKey(
    val aggregate: HealthSyncAggregate,
    val recordType: HealthSyncRecordType,
    val projectionChildKey: HealthProjectionChildKey? = null,
)

{
    /** Alias aditivo para consumidores que usam nome curto. */
    val childKey: HealthProjectionChildKey?
        get() = projectionChildKey

    val hasProjectionChild: Boolean
        get() = projectionChildKey != null

    /** Nome lógico preserva SPEED mesmo enquanto enum antigo permanece fechado. */
    val logicalRecordTypeName: String
        get() = projectionChildKey?.recordTypeName ?: recordType.name
}

enum class HeartRateHistoryPeriod {
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS,
    LAST_365_DAYS,

    ;

    val windowDays: Long
        get() = when (this) {
            TODAY -> 1L
            LAST_7_DAYS -> 7L
            LAST_30_DAYS -> 30L
            LAST_90_DAYS -> 90L
            LAST_365_DAYS -> 365L
    }
}

data class HeartRateHistoryQuery(
    val period: HeartRateHistoryPeriod,
    val zoneId: ZoneId,
    val sources: Set<HeartRateSource> = setOf(
        HeartRateSource.MANUAL_MEASUREMENT,
        HeartRateSource.WATCH_D1_HISTORY,
        HeartRateSource.WATCH_WORKOUT_ACTIVITY,
    ),
)

sealed interface HeartRateHistoryOrigin {
    data class PersistedMeasurement(
        val measurementId: HeartRateMeasurementId,
    ) : HeartRateHistoryOrigin

    /** `measuredAt` da entrada completa identifica a amostra dentro da atividade. */
    data class WorkoutActivitySample(
        val activityId: WorkoutActivityId,
        val activityTitle: String?,
        val suggestedType: WorkoutType,
        val confirmedType: WorkoutType?,
    ) : HeartRateHistoryOrigin
}

data class HeartRateHistoryEntry(
    val origin: HeartRateHistoryOrigin,
    val measuredAt: Instant,
    val bpm: Int,
    val source: HeartRateSource,
) {
    /** Compatibilidade de leitura para consumidores de medições manual/D1. */
    val measurementId: HeartRateMeasurementId?
        get() = (origin as? HeartRateHistoryOrigin.PersistedMeasurement)?.measurementId

    init {
        require(bpm in 30..220)
        require(
            when (source) {
                HeartRateSource.MANUAL_MEASUREMENT,
                HeartRateSource.WATCH_D1_HISTORY,
                -> origin is HeartRateHistoryOrigin.PersistedMeasurement

                HeartRateSource.WATCH_WORKOUT_ACTIVITY -> origin is HeartRateHistoryOrigin.WorkoutActivitySample
                HeartRateSource.MEASUREMENT_STREAM,
                HeartRateSource.APP_WORKOUT_SESSION,
                -> false
            },
        )
    }

    constructor(
        measurementId: HeartRateMeasurementId,
        measuredAt: Instant,
        bpm: Int,
        source: HeartRateSource,
    ) : this(
        origin = HeartRateHistoryOrigin.PersistedMeasurement(measurementId),
        measuredAt = measuredAt,
        bpm = bpm,
        source = source,
    )
}

data class HeartRateHistoryAggregates(
    val measurementCount: Int = 0,
    val averageBpm: Double? = null,
    val minimumBpm: Int? = null,
    val maximumBpm: Int? = null,
)

enum class HeartRateChartResolution {
    INTRADAY,
    DAILY,
    MONTHLY,
}

data class HeartRateChartPoint(
    val timestamp: Instant,
    /** `null` represents an unfilled display bucket, never zero BPM. */
    val bpm: Double?,
    val minimumBpm: Int? = null,
    val maximumBpm: Int? = null,
    val sampleCount: Int = if (bpm == null) 0 else 1,
) {
    init {
        require(bpm?.isFinite() != false)
        require(sampleCount >= 0)
        require((bpm == null) == (sampleCount == 0))
        require(bpm != null || (minimumBpm == null && maximumBpm == null))
    }
}

data class HeartRateChartSeries(
    val resolution: HeartRateChartResolution = HeartRateChartResolution.INTRADAY,
    val points: List<HeartRateChartPoint> = emptyList(),
)

data class HeartRateHistorySnapshot(
    val period: HeartRateHistoryPeriod,
    val entries: List<HeartRateHistoryEntry> = emptyList(),
    val aggregates: HeartRateHistoryAggregates = HeartRateHistoryAggregates(),
    val chart: HeartRateChartSeries = HeartRateChartSeries(),
)

data class D1HeartRatePacketProgress(
    val receivedPacketCount: Int = 0,
    val highestPacketIndex: Int = 0,
    val totalPackets: Int = 0,
) {
    val progressFraction: Float?
        get() = if (totalPackets > 0) {
            (highestPacketIndex.toFloat() / totalPackets.toFloat()).coerceIn(0f, 1f)
        } else {
            null
        }
}

data class D1HeartRateImportSummary(
    val receivedPacketCount: Int = 0,
    val validPointCount: Int = 0,
    val insertedCount: Int = 0,
    val duplicateCount: Int = 0,
    val conflictCount: Int = 0,
    val invalidCount: Int = 0,
    val missingIndexCount: Int = 0,
    val pendingCreatedCount: Int = 0,
    val pendingExistingCount: Int = 0,
    val startedAt: Instant,
    val finishedAt: Instant,
    val timedOut: Boolean = false,
) {
    val hasPersistedData: Boolean
        get() = insertedCount > 0 || duplicateCount > 0 || conflictCount > 0
}

enum class D1HeartRateImportStatus {
    COMPLETED,
    COMPLETED_WITH_GAPS,
    PARTIAL,
    FAILED,
    CANCELLED,
}

sealed interface D1HeartRateImportResult {
    val status: D1HeartRateImportStatus
    val summary: D1HeartRateImportSummary?

    data class Completed(
        override val summary: D1HeartRateImportSummary,
    ) : D1HeartRateImportResult {
        override val status: D1HeartRateImportStatus = D1HeartRateImportStatus.COMPLETED
    }

    data class CompletedWithGaps(
        override val summary: D1HeartRateImportSummary,
    ) : D1HeartRateImportResult {
        override val status: D1HeartRateImportStatus = D1HeartRateImportStatus.COMPLETED_WITH_GAPS
    }

    data class Partial(
        override val summary: D1HeartRateImportSummary,
    ) : D1HeartRateImportResult {
        override val status: D1HeartRateImportStatus = D1HeartRateImportStatus.PARTIAL
    }

    data class Failed(
        override val summary: D1HeartRateImportSummary? = null,
    ) : D1HeartRateImportResult {
        override val status: D1HeartRateImportStatus = D1HeartRateImportStatus.FAILED
    }

    data class Cancelled(
        override val summary: D1HeartRateImportSummary? = null,
    ) : D1HeartRateImportResult {
        override val status: D1HeartRateImportStatus = D1HeartRateImportStatus.CANCELLED
    }
}

data class D1HeartRateImportMetadata(
    val deviceId: DeviceId,
    val lastAttemptAt: Instant,
    val lastSuccessAt: Instant? = null,
    val lastResult: D1HeartRateImportStatus,
    val parserVersion: ParserVersion? = null,
    val insertedCount: Int = 0,
    val duplicateCount: Int = 0,
    val conflictCount: Int = 0,
    val invalidCount: Int = 0,
    val missingIndexCount: Int = 0,
    val timedOut: Boolean = false,
    val updatedAt: Instant,
)

data class D1HeartRateImportPersistenceResult(
    val insertedCount: Int = 0,
    val duplicateCount: Int = 0,
    val conflictCount: Int = 0,
    val pendingCreatedCount: Int = 0,
    val pendingExistingCount: Int = 0,
)

enum class SyncOperationState {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
}

enum class SafeSyncError {
    PERMISSION_REQUIRED,
    PROVIDER_UNAVAILABLE,
    PROVIDER_UPDATE_REQUIRED,
    INVALID_LOCAL_DATA,
    WRITE_REJECTED,
    RETRY_LATER,
    INTERRUPTED,
    INTERNAL,
}

enum class DailyStepsSyncPayloadKind {
    INCREMENTAL,
    RESIDUAL_INCREMENTAL,
    LEGACY_BRIDGE,
}

data class DailyStepsSyncPayload(
    val kind: DailyStepsSyncPayloadKind,
    val sequence: Long,
    val previousTotalSteps: Long,
    val currentTotalSteps: Long,
    val stepDelta: Long,
    val startTime: Instant,
    val endTime: Instant,
    val timeZoneId: String,
) {
    init {
        require(sequence >= 0L)
        require(previousTotalSteps >= 0L)
        require(currentTotalSteps > previousTotalSteps)
        require(
            when (kind) {
                DailyStepsSyncPayloadKind.INCREMENTAL,
                DailyStepsSyncPayloadKind.LEGACY_BRIDGE,
                -> stepDelta == currentTotalSteps - previousTotalSteps

                DailyStepsSyncPayloadKind.RESIDUAL_INCREMENTAL ->
                    stepDelta in 1L..(currentTotalSteps - previousTotalSteps)
            },
        )
        require(endTime.isAfter(startTime))
        require(runCatching { ZoneId.of(timeZoneId) }.isSuccess)
        require(
            when (kind) {
                DailyStepsSyncPayloadKind.INCREMENTAL,
                DailyStepsSyncPayloadKind.RESIDUAL_INCREMENTAL,
                -> sequence > 0L

                DailyStepsSyncPayloadKind.LEGACY_BRIDGE -> sequence == 0L && previousTotalSteps == 0L
            },
        )
    }
}

@ConsistentCopyVisibility
data class HealthSyncOperation private constructor(
    val id: HealthSyncOperationId,
    val key: HealthSyncOperationKey,
    val state: SyncOperationState,
    val attemptCount: Int,
    val lastAttemptAt: Instant?,
    val lastError: SafeSyncError?,
    val clientRecordId: String?,
    val clientRecordVersion: Long?,
    val syncedAt: Instant?,
    val timeZoneId: String?,
    val dailyStepsPayload: DailyStepsSyncPayload?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val action: HealthSyncOperationAction,
    val frozenPayload: HealthSyncFrozenPayload?,
    val reconciliationState: HealthProjectionReconciliationState,
    val reconciliationCheckpoint: HealthProjectionReconciliationCheckpoint,
    val projectionOrigin: HealthProjectionOrigin?,
) {
    val isPending: Boolean
        get() = state == SyncOperationState.PENDING || state == SyncOperationState.FAILED

    val isProjectionChild: Boolean
        get() = key.projectionChildKey != null

    fun start(at: Instant): DomainResult<HealthSyncOperation> {
        if (state != SyncOperationState.PENDING && state != SyncOperationState.FAILED) {
            return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }
        if (lastAttemptAt != null && at.isBefore(lastAttemptAt)) {
            return DomainResult.Failure(DomainValidationError.INVALID_TEMPORAL_ORDER)
        }

        return DomainResult.Success(copy(
            state = SyncOperationState.SYNCING,
            attemptCount = attemptCount + 1,
            lastAttemptAt = at,
            lastError = null,
            updatedAt = at,
        ))
    }

    fun markSynced(at: Instant = lastAttemptAt ?: Instant.EPOCH): DomainResult<HealthSyncOperation> =
        if (state == SyncOperationState.SYNCING &&
            (!isProjectionChild || reconciliationCheckpoint != HealthProjectionReconciliationCheckpoint.NOT_STARTED)
        ) {
            DomainResult.Success(copy(
                state = SyncOperationState.SYNCED,
                lastError = null,
                syncedAt = at,
                updatedAt = at,
                reconciliationState = when (action) {
                    HealthSyncOperationAction.INSERT -> HealthProjectionReconciliationState.SENT
                    HealthSyncOperationAction.DELETE -> HealthProjectionReconciliationState.OBSOLETE
                },
                reconciliationCheckpoint = HealthProjectionReconciliationCheckpoint.AFTER_REMOTE_EFFECT,
            ))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

    fun markFailed(
        error: SafeSyncError,
        at: Instant = lastAttemptAt ?: Instant.EPOCH,
    ): DomainResult<HealthSyncOperation> =
        if (state == SyncOperationState.SYNCING) {
            DomainResult.Success(copy(
                state = SyncOperationState.FAILED,
                lastError = error,
                updatedAt = at,
            ))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

    fun withIdentity(
        clientRecordId: String?,
        clientRecordVersion: Long?,
        timeZoneId: String?,
        updatedAt: Instant?,
    ): DomainResult<HealthSyncOperation> {
        if (clientRecordId != null && clientRecordId.isBlank()) {
            return DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
        }
        if (clientRecordVersion != null && clientRecordVersion < 0L) {
            return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        }
        if (this.clientRecordId != null && clientRecordId != this.clientRecordId) {
            return DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
        }
        if (this.clientRecordVersion != null &&
            (clientRecordVersion == null || clientRecordVersion < this.clientRecordVersion)
        ) {
            return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        }
        if (dailyStepsPayload != null &&
            (clientRecordId != this.clientRecordId ||
                clientRecordVersion != this.clientRecordVersion ||
                timeZoneId != this.timeZoneId)
        ) {
            return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }
        if (frozenPayload != null && clientRecordVersion != this.clientRecordVersion) {
            return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }
        return DomainResult.Success(copy(
            clientRecordId = clientRecordId,
            clientRecordVersion = clientRecordVersion,
            timeZoneId = timeZoneId,
            updatedAt = updatedAt,
        ))
    }

    /**
     * Congela projeção para retry. Mudança de payload, ação ou origem exige
     * versão estritamente maior e reinicia somente filho afetado.
     */
    fun freezeProjection(
        action: HealthSyncOperationAction,
        frozenPayload: HealthSyncFrozenPayload?,
        clientRecordId: String,
        clientRecordVersion: Long,
        projectionOrigin: HealthProjectionOrigin?,
        updatedAt: Instant?,
    ): DomainResult<HealthSyncOperation> {
        if (clientRecordId.isBlank() || clientRecordVersion < 0L) {
            return DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
        }
        if (this.clientRecordId != null && this.clientRecordId != clientRecordId) {
            return DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
        }
        if (this.clientRecordVersion != null && clientRecordVersion < this.clientRecordVersion) {
            return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        }
        if (state == SyncOperationState.SYNCING) {
            return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

        val projectionChanged = this.action != action ||
            this.frozenPayload != frozenPayload ||
            this.projectionOrigin != projectionOrigin
        if (projectionChanged && this.clientRecordVersion != null &&
            clientRecordVersion <= this.clientRecordVersion
        ) {
            return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        }

        return DomainResult.Success(copy(
            state = if (projectionChanged) SyncOperationState.PENDING else state,
            attemptCount = if (projectionChanged) 0 else attemptCount,
            lastAttemptAt = if (projectionChanged) null else lastAttemptAt,
            lastError = if (projectionChanged) null else lastError,
            clientRecordId = clientRecordId,
            clientRecordVersion = if (projectionChanged) {
                clientRecordVersion
            } else {
                this.clientRecordVersion ?: clientRecordVersion
            },
            syncedAt = if (projectionChanged) null else syncedAt,
            updatedAt = updatedAt ?: this.updatedAt,
            action = action,
            frozenPayload = frozenPayload,
            reconciliationState = if (projectionChanged) {
                HealthProjectionReconciliationState.EXPECTED
            } else {
                reconciliationState
            },
            reconciliationCheckpoint = if (projectionChanged) {
                HealthProjectionReconciliationCheckpoint.NOT_STARTED
            } else {
                reconciliationCheckpoint
            },
            projectionOrigin = projectionOrigin,
        ))
    }

    /** Prepara remoção exata de agregado legado sem criar novo payload remoto. */
    fun prepareAggregateDeletion(
        clientRecordId: String,
        clientRecordVersion: Long,
        updatedAt: Instant?,
    ): DomainResult<HealthSyncOperation> {
        if (key.projectionChildKey != null || clientRecordId.isBlank() || clientRecordVersion < 0L) {
            return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }
        if (this.clientRecordId != null && this.clientRecordId != clientRecordId) {
            return DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
        }
        if (this.clientRecordVersion != null && clientRecordVersion < this.clientRecordVersion) {
            return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        }
        if (state == SyncOperationState.SYNCING) {
            return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }
        if (action == HealthSyncOperationAction.DELETE) {
            return DomainResult.Success(
                if (this.clientRecordVersion == null) {
                    copy(clientRecordVersion = clientRecordVersion)
                } else {
                    this
                },
            )
        }

        val nextVersion = when (val currentVersion = this.clientRecordVersion) {
            null -> clientRecordVersion
            Long.MAX_VALUE -> return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            else -> maxOf(clientRecordVersion, currentVersion + 1L)
        }
        return DomainResult.Success(copy(
            state = SyncOperationState.PENDING,
            attemptCount = 0,
            lastAttemptAt = null,
            lastError = null,
            clientRecordId = clientRecordId,
            clientRecordVersion = nextVersion,
            syncedAt = null,
            updatedAt = updatedAt ?: this.updatedAt,
            action = HealthSyncOperationAction.DELETE,
            frozenPayload = null,
            reconciliationState = HealthProjectionReconciliationState.EXPECTED,
            reconciliationCheckpoint = HealthProjectionReconciliationCheckpoint.NOT_STARTED,
            projectionOrigin = null,
        ))
    }

    fun markReconciliationBeforeRemoteEffect(): DomainResult<HealthSyncOperation> =
        if (state == SyncOperationState.SYNCING &&
            reconciliationCheckpoint != HealthProjectionReconciliationCheckpoint.AFTER_REMOTE_EFFECT
        ) {
            DomainResult.Success(copy(
                reconciliationCheckpoint = HealthProjectionReconciliationCheckpoint.BEFORE_REMOTE_EFFECT,
            ))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

    fun markReconciliationAfterRemoteEffect(): DomainResult<HealthSyncOperation> =
        if (state == SyncOperationState.SYNCING &&
            reconciliationCheckpoint == HealthProjectionReconciliationCheckpoint.BEFORE_REMOTE_EFFECT
        ) {
            DomainResult.Success(copy(
                reconciliationCheckpoint = HealthProjectionReconciliationCheckpoint.AFTER_REMOTE_EFFECT,
                reconciliationState = when (action) {
                    HealthSyncOperationAction.INSERT -> HealthProjectionReconciliationState.SENT
                    HealthSyncOperationAction.DELETE -> HealthProjectionReconciliationState.OBSOLETE
                },
            ))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

    companion object {
        fun create(
            id: HealthSyncOperationId,
            key: HealthSyncOperationKey,
            clientRecordId: String? = null,
            clientRecordVersion: Long? = null,
            syncedAt: Instant? = null,
            timeZoneId: String? = null,
            dailyStepsPayload: DailyStepsSyncPayload? = null,
            createdAt: Instant? = null,
            updatedAt: Instant? = null,
            action: HealthSyncOperationAction = HealthSyncOperationAction.INSERT,
            frozenPayload: HealthSyncFrozenPayload? = null,
            reconciliationState: HealthProjectionReconciliationState =
                HealthProjectionReconciliationState.EXPECTED,
            reconciliationCheckpoint: HealthProjectionReconciliationCheckpoint =
                HealthProjectionReconciliationCheckpoint.NOT_STARTED,
            projectionOrigin: HealthProjectionOrigin? = null,
            state: SyncOperationState = SyncOperationState.PENDING,
            attemptCount: Int = 0,
            lastAttemptAt: Instant? = null,
            lastError: SafeSyncError? = null,
        ): DomainResult<HealthSyncOperation> =
            if (supports(key) &&
                isValidIdentityMetadata(clientRecordId, clientRecordVersion) &&
                isValidDailyStepsPayload(key, timeZoneId, dailyStepsPayload) &&
                isValidProjectionPayload(key, clientRecordId, clientRecordVersion, frozenPayload, projectionOrigin) &&
                isValidProjectionState(key, state, reconciliationCheckpoint) &&
                attemptCount >= 0
            ) {
                DomainResult.Success(
                    HealthSyncOperation(
                        id = id,
                        key = key,
                        state = state,
                        attemptCount = attemptCount,
                        lastAttemptAt = lastAttemptAt,
                        lastError = lastError,
                        clientRecordId = clientRecordId,
                        clientRecordVersion = clientRecordVersion,
                        syncedAt = syncedAt,
                        timeZoneId = timeZoneId,
                        dailyStepsPayload = dailyStepsPayload,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        action = action,
                        frozenPayload = frozenPayload,
                        reconciliationState = reconciliationState,
                        reconciliationCheckpoint = reconciliationCheckpoint,
                        projectionOrigin = projectionOrigin,
                    ),
                )
            } else {
                DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
            }

        private fun supports(key: HealthSyncOperationKey): Boolean {
            if (key.projectionChildKey != null && key.aggregate !is HealthSyncAggregate.WorkoutActivity) {
                return false
            }
            val supportedRecordType = when (key.aggregate) {
                is HealthSyncAggregate.DailySummary -> key.recordType == HealthSyncRecordType.STEPS
                is HealthSyncAggregate.WatchDailyD1Sample -> key.recordType in setOf(
                    HealthSyncRecordType.STEPS,
                    HealthSyncRecordType.DISTANCE,
                    HealthSyncRecordType.ACTIVE_CALORIES_BURNED,
                )
                is HealthSyncAggregate.HeartRateMeasurement -> key.recordType == HealthSyncRecordType.HEART_RATE
                is HealthSyncAggregate.WorkoutActivity -> key.recordType in setOf(
                    HealthSyncRecordType.STEPS,
                    HealthSyncRecordType.STEPS_CADENCE,
                    HealthSyncRecordType.EXERCISE_SESSION,
                    HealthSyncRecordType.DISTANCE,
                    HealthSyncRecordType.TOTAL_CALORIES_BURNED,
                    HealthSyncRecordType.HEART_RATE,
                    HealthSyncRecordType.SPEED,
                )

                is HealthSyncAggregate.WeightMeasurement -> key.recordType == HealthSyncRecordType.WEIGHT
                is HealthSyncAggregate.UserProfileHeight -> key.recordType == HealthSyncRecordType.HEIGHT
            }
            return supportedRecordType && supportsProjectionChild(key)
        }

        private fun supportsProjectionChild(key: HealthSyncOperationKey): Boolean = when (
            val child = key.projectionChildKey
        ) {
            null -> key.aggregate !is HealthSyncAggregate.WorkoutActivity ||
                key.recordType !in setOf(
                    HealthSyncRecordType.SPEED,
                    HealthSyncRecordType.STEPS,
                    HealthSyncRecordType.STEPS_CADENCE,
                )
            HealthProjectionChildKey.ExerciseSession -> key.recordType == HealthSyncRecordType.EXERCISE_SESSION
            is HealthProjectionChildKey.DistanceInterval -> key.recordType == HealthSyncRecordType.DISTANCE
            HealthProjectionChildKey.ManualDistance -> key.recordType == HealthSyncRecordType.DISTANCE
            HealthProjectionChildKey.SpeedSeries -> key.recordType == HealthSyncRecordType.SPEED
            HealthProjectionChildKey.HeartRateSeries -> key.recordType == HealthSyncRecordType.HEART_RATE
            HealthProjectionChildKey.TotalCaloriesBurned ->
                key.recordType == HealthSyncRecordType.TOTAL_CALORIES_BURNED
            HealthProjectionChildKey.WorkoutSteps -> key.recordType == HealthSyncRecordType.STEPS
            HealthProjectionChildKey.StepsCadenceSeries -> key.recordType == HealthSyncRecordType.STEPS_CADENCE
        }

        private fun isValidIdentityMetadata(
            clientRecordId: String?,
            clientRecordVersion: Long?,
        ): Boolean = (clientRecordId == null || clientRecordId.isNotBlank()) &&
            (clientRecordVersion == null || clientRecordVersion >= 0L)

        private fun isValidDailyStepsPayload(
            key: HealthSyncOperationKey,
            timeZoneId: String?,
            payload: DailyStepsSyncPayload?,
        ): Boolean = when {
            payload == null -> true
            key.aggregate !is HealthSyncAggregate.DailySummary -> false
            key.recordType != HealthSyncRecordType.STEPS -> false
            else -> payload.timeZoneId == timeZoneId
        }

        private fun isValidProjectionPayload(
            key: HealthSyncOperationKey,
            clientRecordId: String?,
            clientRecordVersion: Long?,
            frozenPayload: HealthSyncFrozenPayload?,
            projectionOrigin: HealthProjectionOrigin?,
        ): Boolean = when {
            key.aggregate is HealthSyncAggregate.WatchDailyD1Sample ->
                key.projectionChildKey == null &&
                    clientRecordId != null && clientRecordVersion != null &&
                    when (key.recordType) {
                        HealthSyncRecordType.STEPS ->
                            frozenPayload?.schemaVersion == HEALTH_SYNC_FROZEN_DAILY_D1_PAYLOAD_SCHEMA_VERSION
                        HealthSyncRecordType.DISTANCE,
                        HealthSyncRecordType.ACTIVE_CALORIES_BURNED,
                        -> frozenPayload?.schemaVersion == HEALTH_SYNC_FROZEN_DAILY_D1_METRIC_PAYLOAD_SCHEMA_VERSION
                        else -> false
                    }

            key.projectionChildKey == null -> true

            else -> {
                clientRecordId != null && clientRecordVersion != null &&
                    frozenPayload != null && projectionOrigin != null
            }
        }

        private fun isValidProjectionState(
            key: HealthSyncOperationKey,
            state: SyncOperationState,
            checkpoint: HealthProjectionReconciliationCheckpoint,
        ): Boolean = if (key.projectionChildKey == null) {
            true
        } else {
            when (state) {
                SyncOperationState.PENDING -> checkpoint != HealthProjectionReconciliationCheckpoint.AFTER_REMOTE_EFFECT
                SyncOperationState.FAILED -> checkpoint != HealthProjectionReconciliationCheckpoint.AFTER_REMOTE_EFFECT
                SyncOperationState.SYNCING -> checkpoint != HealthProjectionReconciliationCheckpoint.NOT_STARTED
                SyncOperationState.SYNCED -> checkpoint == HealthProjectionReconciliationCheckpoint.AFTER_REMOTE_EFFECT
            }
        }
    }
}
