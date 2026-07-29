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
    HEART_RATE,
    EXERCISE_SESSION,
    DISTANCE,
    TOTAL_CALORIES_BURNED,
    WEIGHT,
    HEIGHT,
}

enum class HealthSyncAggregateType {
    DAILY_SUMMARY,
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
)

enum class HeartRateHistoryPeriod {
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,

    ;

    val windowDays: Long
        get() = when (this) {
            TODAY -> 1L
            LAST_7_DAYS -> 7L
            LAST_30_DAYS -> 30L
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
}

data class HeartRateChartPoint(
    val timestamp: Instant,
    val bpm: Double,
    val minimumBpm: Int? = null,
    val maximumBpm: Int? = null,
    val sampleCount: Int = 1,
)

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
    val createdAt: Instant?,
    val updatedAt: Instant?,
) {
    val isPending: Boolean
        get() = state == SyncOperationState.PENDING || state == SyncOperationState.FAILED

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
        if (state == SyncOperationState.SYNCING) {
            DomainResult.Success(copy(
                state = SyncOperationState.SYNCED,
                lastError = null,
                syncedAt = at,
                updatedAt = at,
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
        return DomainResult.Success(copy(
            clientRecordId = clientRecordId,
            clientRecordVersion = clientRecordVersion,
            timeZoneId = timeZoneId,
            updatedAt = updatedAt,
        ))
    }

    companion object {
        fun create(
            id: HealthSyncOperationId,
            key: HealthSyncOperationKey,
            clientRecordId: String? = null,
            clientRecordVersion: Long? = null,
            syncedAt: Instant? = null,
            timeZoneId: String? = null,
            createdAt: Instant? = null,
            updatedAt: Instant? = null,
        ): DomainResult<HealthSyncOperation> =
            if (supports(key) && isValidIdentityMetadata(clientRecordId, clientRecordVersion)) {
                DomainResult.Success(
                    HealthSyncOperation(
                        id = id,
                        key = key,
                        state = SyncOperationState.PENDING,
                        attemptCount = 0,
                        lastAttemptAt = null,
                        lastError = null,
                        clientRecordId = clientRecordId,
                        clientRecordVersion = clientRecordVersion,
                        syncedAt = syncedAt,
                        timeZoneId = timeZoneId,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                    ),
                )
            } else {
                DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
            }

        private fun supports(key: HealthSyncOperationKey): Boolean = when (key.aggregate) {
            is HealthSyncAggregate.DailySummary -> key.recordType == HealthSyncRecordType.STEPS
            is HealthSyncAggregate.HeartRateMeasurement -> key.recordType == HealthSyncRecordType.HEART_RATE
            is HealthSyncAggregate.WorkoutActivity -> key.recordType in setOf(
                HealthSyncRecordType.EXERCISE_SESSION,
                HealthSyncRecordType.DISTANCE,
                HealthSyncRecordType.TOTAL_CALORIES_BURNED,
                HealthSyncRecordType.HEART_RATE,
            )

            is HealthSyncAggregate.WeightMeasurement -> key.recordType == HealthSyncRecordType.WEIGHT
            is HealthSyncAggregate.UserProfileHeight -> key.recordType == HealthSyncRecordType.HEIGHT
        }

        private fun isValidIdentityMetadata(
            clientRecordId: String?,
            clientRecordVersion: Long?,
        ): Boolean = (clientRecordId == null || clientRecordId.isNotBlank()) &&
            (clientRecordVersion == null || clientRecordVersion >= 0L)
    }
}
