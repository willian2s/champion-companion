package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant

enum class ParserValidity {
    VALID,
    PARTIAL,
    INVALID,
}

enum class WorkoutType {
    WALKING,
    RUNNING,
    FREE_WORKOUT,
    TREADMILL,
    UNKNOWN,
}

enum class ActivityWorkflowState {
    PENDING_REVIEW,
    PENDING_CALIBRATION,
    READY_TO_SYNC,
    SYNCING,
    SYNCED,
    SYNC_FAILED,
    DISCARDED,
}

data class WatchWorkoutImportKey(
    val deviceId: DeviceId,
    val origin: DataOrigin,
    val crc: Int,
)

data class WorkoutDistanceMetrics(
    val originalMeters: DomainValue<Double>,
    val estimatedMeters: DomainValue<Double>,
    val calibratedMeters: DomainValue<Double>,
)

data class WorkoutMetrics(
    val startedAt: Instant,
    val duration: Duration,
    val steps: Long,
    val caloriesKilocalories: DomainValue<Double>,
    val distances: WorkoutDistanceMetrics,
    val averagePaceSecondsPerKilometer: DomainValue<Double>,
    val averageSpeedMetersPerSecond: DomainValue<Double>,
    val averageBpm: DomainValue<Int>,
    val minimumBpm: DomainValue<Int>,
    val maximumBpm: DomainValue<Int>,
    /** Calibration-derived values live beside, never replace, imported metrics. */
    val calibratedMetrics: CalibratedWorkoutMetrics? = null,
)

data class WorkoutInterval(
    val index: Int,
    val duration: Duration,
    val steps: Long,
    val distanceMeters: DomainValue<Double>,
    val caloriesKilocalories: DomainValue<Double>,
)

data class WorkoutHeartRateSample(
    val sampledAt: Instant,
    val bpm: Int,
)

/** Parsed values cross parser boundary without retaining D3/D4 binary packets. */
data class WatchWorkoutParserInput(
    val deviceId: DeviceId,
    val crc: Int,
    val externalBlockIndex: Int,
    val parserVersion: ParserVersion,
    val parserValidity: ParserValidity,
    val diagnosticPayloadId: DiagnosticPayloadId?,
    val metrics: WorkoutMetrics,
    val intervals: List<WorkoutInterval>,
    val heartRateSamples: List<WorkoutHeartRateSample>,
)

@ConsistentCopyVisibility
data class WatchWorkoutImport private constructor(
    val id: WatchWorkoutImportId,
    val activityId: WorkoutActivityId,
    val key: WatchWorkoutImportKey,
    val externalBlockIndex: Int,
    val parserVersion: ParserVersion,
    val parserValidity: ParserValidity,
    val diagnosticPayloadId: DiagnosticPayloadId?,
    val importedAt: Instant,
) {
    companion object {
        fun create(
            id: WatchWorkoutImportId,
            activityId: WorkoutActivityId,
            input: WatchWorkoutParserInput,
            importedAt: Instant,
        ): DomainResult<WatchWorkoutImport> {
            if (input.crc !in VALID_CRC_RANGE || input.externalBlockIndex < 0 ||
                importedAt.isBefore(input.metrics.startedAt)
            ) {
                return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
            return DomainResult.Success(
                WatchWorkoutImport(
                    id = id,
                    activityId = activityId,
                    key = WatchWorkoutImportKey(input.deviceId, DataOrigin.WATCH_D3_D4, input.crc),
                    externalBlockIndex = input.externalBlockIndex,
                    parserVersion = input.parserVersion,
                    parserValidity = input.parserValidity,
                    diagnosticPayloadId = input.diagnosticPayloadId,
                    importedAt = importedAt,
                ),
            )
        }

        private val VALID_CRC_RANGE = 1..0xffff
    }
}

@ConsistentCopyVisibility
data class WorkoutActivity private constructor(
    val id: WorkoutActivityId,
    val origin: DataOrigin,
    val deviceId: DeviceId,
    val importKey: WatchWorkoutImportKey,
    val parserValidity: ParserValidity,
    val importedAt: Instant,
    val parserVersion: ParserVersion,
    val diagnosticPayloadId: DiagnosticPayloadId?,
    val metrics: WorkoutMetrics,
    val intervals: List<WorkoutInterval>,
    val heartRateSamples: List<WorkoutHeartRateSample>,
    val suggestedType: WorkoutType,
    val title: String?,
    val notes: String?,
    val confirmedType: WorkoutType?,
    val workflowState: ActivityWorkflowState,
    val reviewedAt: Instant?,
    val calibrationId: TreadmillCalibrationId?,
) {
    fun confirmReview(type: WorkoutType, reviewedAt: Instant): DomainResult<WorkoutActivity> {
        if (workflowState != ActivityWorkflowState.PENDING_REVIEW ||
            parserValidity != ParserValidity.VALID || reviewedAt.isBefore(importedAt)
        ) {
            return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

        return DomainResult.Success(copy(
            confirmedType = type,
            workflowState = if (type == WorkoutType.TREADMILL) {
                ActivityWorkflowState.PENDING_CALIBRATION
            } else {
                ActivityWorkflowState.READY_TO_SYNC
            },
            reviewedAt = reviewedAt,
        ))
    }

    fun markCalibrated(): DomainResult<WorkoutActivity> =
        if (workflowState == ActivityWorkflowState.PENDING_CALIBRATION &&
            metrics.distances.calibratedMeters is DomainValue.Present
        ) {
            DomainResult.Success(copy(workflowState = ActivityWorkflowState.READY_TO_SYNC))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

    fun applyCalibration(calibration: TreadmillCalibration): DomainResult<WorkoutActivity> {
        if (workflowState != ActivityWorkflowState.PENDING_CALIBRATION ||
            calibration.subject != TreadmillCalibrationSubject.Activity(id) ||
            reviewedAt == null || calibration.appliedAt.isBefore(reviewedAt)
        ) {
            return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

        return DomainResult.Success(copy(
            calibrationId = calibration.id,
            workflowState = ActivityWorkflowState.READY_TO_SYNC,
            metrics = metrics.copy(
                distances = metrics.distances.copy(calibratedMeters = calibration.calibratedMeters),
                calibratedMetrics = calibration.derivedMetrics,
            ),
        ))
    }

    fun startSync(): DomainResult<WorkoutActivity> = transition(
        from = setOf(ActivityWorkflowState.READY_TO_SYNC, ActivityWorkflowState.SYNC_FAILED),
        to = ActivityWorkflowState.SYNCING,
    )

    fun markSynced(): DomainResult<WorkoutActivity> = transition(
        from = setOf(ActivityWorkflowState.SYNCING),
        to = ActivityWorkflowState.SYNCED,
    )

    fun markSyncFailed(): DomainResult<WorkoutActivity> = transition(
        from = setOf(ActivityWorkflowState.SYNCING),
        to = ActivityWorkflowState.SYNC_FAILED,
    )

    fun discard(): DomainResult<WorkoutActivity> = transition(
        from = setOf(
            ActivityWorkflowState.PENDING_REVIEW,
            ActivityWorkflowState.PENDING_CALIBRATION,
            ActivityWorkflowState.READY_TO_SYNC,
        ),
        to = ActivityWorkflowState.DISCARDED,
    )

    fun reimport(
        input: WatchWorkoutParserInput,
        importedAt: Instant,
    ): DomainResult<WorkoutActivity> {
        if (input.deviceId != deviceId || input.crc != importKey.crc ||
            importedAt.isBefore(this.importedAt) || !isValidInput(input)
        ) {
            return DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
        }

        return DomainResult.Success(copy(
            parserValidity = input.parserValidity,
            importedAt = importedAt,
            parserVersion = input.parserVersion,
            diagnosticPayloadId = input.diagnosticPayloadId,
            metrics = input.metrics.copy(
                distances = input.metrics.distances.copy(
                    calibratedMeters = metrics.distances.calibratedMeters,
                ),
                calibratedMetrics = metrics.calibratedMetrics,
            ),
            intervals = input.intervals,
            heartRateSamples = input.heartRateSamples,
            suggestedType = suggestType(input.metrics.steps),
            title = title,
            notes = notes,
        ))
    }

    private fun transition(
        from: Set<ActivityWorkflowState>,
        to: ActivityWorkflowState,
    ): DomainResult<WorkoutActivity> =
        if (workflowState in from) {
            DomainResult.Success(copy(workflowState = to))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

    companion object {
        fun createFromImport(
            id: WorkoutActivityId,
            input: WatchWorkoutParserInput,
            importedAt: Instant,
        ): DomainResult<WorkoutActivity> {
            if (importedAt.isBefore(input.metrics.startedAt) || !isValidInput(input)) {
                return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
            return DomainResult.Success(
                WorkoutActivity(
                    id = id,
                    origin = DataOrigin.WATCH_D3_D4,
                    deviceId = input.deviceId,
                    importKey = WatchWorkoutImportKey(input.deviceId, DataOrigin.WATCH_D3_D4, input.crc),
                    parserValidity = input.parserValidity,
                    importedAt = importedAt,
                    parserVersion = input.parserVersion,
                    diagnosticPayloadId = input.diagnosticPayloadId,
                    metrics = input.metrics,
                    intervals = input.intervals,
                    heartRateSamples = input.heartRateSamples,
                    suggestedType = suggestType(input.metrics.steps),
                    title = null,
                    notes = null,
                    confirmedType = null,
                    workflowState = ActivityWorkflowState.PENDING_REVIEW,
                    reviewedAt = null,
                    calibrationId = null,
                ),
            )
        }

        fun suggestType(steps: Long): WorkoutType =
            if (steps < STEPS_FOR_RUNNING_SUGGESTION) WorkoutType.FREE_WORKOUT else WorkoutType.RUNNING

        private fun isValidInput(input: WatchWorkoutParserInput): Boolean =
            input.crc in 1..0xffff && input.externalBlockIndex >= 0 &&
                input.metrics.duration >= Duration.ZERO && input.metrics.steps >= 0 &&
                areValid(input.metrics) &&
                areValidIntervals(input.intervals) &&
                areValidHeartRateSamples(input.heartRateSamples) &&
                areValidTemporalOrder(input.heartRateSamples)

        private fun areValid(metrics: WorkoutMetrics): Boolean =
            isNonNegative(metrics.caloriesKilocalories) &&
                isNonNegative(metrics.distances.originalMeters) &&
                isNonNegative(metrics.distances.estimatedMeters) &&
                isNonNegative(metrics.distances.calibratedMeters) &&
                isNonNegative(metrics.averagePaceSecondsPerKilometer) &&
                isNonNegative(metrics.averageSpeedMetersPerSecond) &&
                isValidBpm(metrics.averageBpm) && isValidBpm(metrics.minimumBpm) &&
                isValidBpm(metrics.maximumBpm)

        private fun isNonNegative(value: DomainValue<Double>): Boolean =
            value !is DomainValue.Present || (value.value.isFinite() && value.value >= 0.0)

        private fun isValidBpm(value: DomainValue<Int>): Boolean =
            value !is DomainValue.Present || value.value in 30..220

        private fun areValidIntervals(intervals: List<WorkoutInterval>): Boolean {
            var previousIndex = Int.MIN_VALUE
            return intervals.all { interval ->
                val currentIndex = interval.index
                val isOrdered = currentIndex >= 0 && currentIndex > previousIndex
                previousIndex = currentIndex
                isOrdered && interval.duration >= Duration.ZERO && interval.steps >= 0 &&
                    isNonNegative(interval.distanceMeters) && isNonNegative(interval.caloriesKilocalories)
            }
        }

        private fun areValidHeartRateSamples(samples: List<WorkoutHeartRateSample>): Boolean =
            samples.all { it.bpm in 30..220 }

        private fun areValidTemporalOrder(samples: List<WorkoutHeartRateSample>): Boolean {
            var previous: Instant? = null
            return samples.all { sample ->
                val isOrdered = previous == null || sample.sampledAt.isAfter(previous)
                previous = sample.sampledAt
                isOrdered
            }
        }

        private const val STEPS_FOR_RUNNING_SUGGESTION = 1_000L
    }
}
