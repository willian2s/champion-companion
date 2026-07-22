package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
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
    HEART_RATE,
}

sealed interface HealthSyncAggregate {
    val id: UUID

    data class HeartRateMeasurement(
        val measurementId: HeartRateMeasurementId,
    ) : HealthSyncAggregate {
        override val id: UUID = measurementId.value
    }
}

data class HealthSyncOperationKey(
    val aggregate: HealthSyncAggregate,
    val recordType: HealthSyncRecordType,
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
    WRITE_REJECTED,
    RETRY_LATER,
}

@ConsistentCopyVisibility
data class HealthSyncOperation private constructor(
    val id: HealthSyncOperationId,
    val key: HealthSyncOperationKey,
    val state: SyncOperationState,
    val attemptCount: Int,
    val lastAttemptAt: Instant?,
    val lastError: SafeSyncError?,
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
        ))
    }

    fun markSynced(): DomainResult<HealthSyncOperation> =
        if (state == SyncOperationState.SYNCING) {
            DomainResult.Success(copy(state = SyncOperationState.SYNCED, lastError = null))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

    fun markFailed(error: SafeSyncError): DomainResult<HealthSyncOperation> =
        if (state == SyncOperationState.SYNCING) {
            DomainResult.Success(copy(state = SyncOperationState.FAILED, lastError = error))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

    companion object {
        fun create(
            id: HealthSyncOperationId,
            key: HealthSyncOperationKey,
        ): DomainResult<HealthSyncOperation> =
            if (key.aggregate is HealthSyncAggregate.HeartRateMeasurement &&
                key.recordType == HealthSyncRecordType.HEART_RATE
            ) {
                DomainResult.Success(
                    HealthSyncOperation(
                        id = id,
                        key = key,
                        state = SyncOperationState.PENDING,
                        attemptCount = 0,
                        lastAttemptAt = null,
                        lastError = null,
                    ),
                )
            } else {
                DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
            }
    }
}
