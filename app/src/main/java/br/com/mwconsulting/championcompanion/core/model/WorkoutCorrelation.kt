package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant

enum class WorkoutMetricKind {
    TYPE,
    START,
    END,
    DURATION,
    ROUTE,
    DISTANCE,
    PACE,
    SPEED,
    HEART_RATE,
    STEPS,
    CALORIES,
    TITLE,
    NOTES,
    IMPORT_METADATA,
}

enum class WorkoutMetricSource {
    PHONE_SESSION,
    WATCH,
    USER_REVIEW,
    WATCH_IMPORT,
}

object WorkoutSourcePriority {
    fun primarySource(metric: WorkoutMetricKind): WorkoutMetricSource = when (metric) {
        WorkoutMetricKind.TYPE,
        WorkoutMetricKind.START,
        WorkoutMetricKind.END,
        WorkoutMetricKind.DURATION,
        WorkoutMetricKind.ROUTE,
        WorkoutMetricKind.DISTANCE,
        WorkoutMetricKind.PACE,
        WorkoutMetricKind.SPEED,
        -> WorkoutMetricSource.PHONE_SESSION

        WorkoutMetricKind.HEART_RATE,
        WorkoutMetricKind.STEPS,
        WorkoutMetricKind.CALORIES,
        -> WorkoutMetricSource.WATCH

        WorkoutMetricKind.TITLE,
        WorkoutMetricKind.NOTES,
        -> WorkoutMetricSource.USER_REVIEW

        WorkoutMetricKind.IMPORT_METADATA -> WorkoutMetricSource.WATCH_IMPORT
    }
}

data class PhoneWorkoutCorrelationCandidate(
    val activityId: WorkoutActivityId,
    val sessionId: AppWorkoutSessionId,
    val origin: WorkoutActivityOrigin,
    val startedAt: Instant,
    val endedAt: Instant,
    val associatedDeviceId: DeviceId?,
) {
    init {
        require(origin in PHONE_WORKOUT_ORIGINS)
        require(endedAt.isAfter(startedAt))
    }

    private companion object {
        val PHONE_WORKOUT_ORIGINS = setOf(
            WorkoutActivityOrigin.PHONE_GPS,
            WorkoutActivityOrigin.PHONE_TREADMILL,
            WorkoutActivityOrigin.PHONE_FREE_WORKOUT,
        )
    }
}

data class WatchWorkoutCorrelationSource(
    val deviceId: DeviceId,
    val startedAt: Instant,
    val endedAt: Instant,
    val crc: Int,
) {
    init {
        require(endedAt.isAfter(startedAt))
        require(crc in 1..0xffff)
    }
}

@ConsistentCopyVisibility
data class WorkoutCorrelationInput private constructor(
    val watch: WatchWorkoutCorrelationSource,
    val phoneCandidates: List<PhoneWorkoutCorrelationCandidate>,
    val maximumStartDifference: Duration,
    val minimumOverlapRatio: Double,
) {
    companion object {
        val maximumAllowedStartDifference: Duration = Duration.ofSeconds(60)

        fun create(
            watch: WatchWorkoutCorrelationSource,
            phoneCandidates: List<PhoneWorkoutCorrelationCandidate>,
            maximumStartDifference: Duration = maximumAllowedStartDifference,
            minimumOverlapRatio: Double = 0.5,
        ): DomainResult<WorkoutCorrelationInput> = if (
            maximumStartDifference.isNegative || maximumStartDifference.isZero ||
            maximumStartDifference > maximumAllowedStartDifference ||
            !minimumOverlapRatio.isFinite() || minimumOverlapRatio <= 0.0 || minimumOverlapRatio > 1.0 ||
            phoneCandidates.map { it.activityId }.distinct().size != phoneCandidates.size ||
            phoneCandidates.map { it.sessionId }.distinct().size != phoneCandidates.size
        ) {
            DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        } else {
            DomainResult.Success(WorkoutCorrelationInput(
                watch, phoneCandidates, maximumStartDifference, minimumOverlapRatio,
            ))
        }
    }
}

data class WorkoutCorrelationEvidence(
    val absoluteStartDifference: Duration,
    val overlap: Duration,
    val overlapRatio: Double,
    val deviceMatched: Boolean,
)

sealed interface WorkoutCorrelationResult {
    data object NoSafeCandidate : WorkoutCorrelationResult

    data class UniqueCandidate(
        val candidate: PhoneWorkoutCorrelationCandidate,
        val evidence: WorkoutCorrelationEvidence,
    ) : WorkoutCorrelationResult

    /** Não possui candidato escolhido por design. */
    data class Ambiguous(
        val candidateActivityIds: List<WorkoutActivityId>,
    ) : WorkoutCorrelationResult {
        init {
            require(candidateActivityIds.size > 1)
            require(candidateActivityIds.distinct().size == candidateActivityIds.size)
        }
    }
}

object WorkoutCorrelation {
    fun evaluate(input: WorkoutCorrelationInput): WorkoutCorrelationResult {
        val eligible = input.phoneCandidates.mapNotNull { candidate ->
            val startDifference = if (candidate.startedAt.isBefore(input.watch.startedAt)) {
                Duration.between(candidate.startedAt, input.watch.startedAt)
            } else {
                Duration.between(input.watch.startedAt, candidate.startedAt)
            }
            if (startDifference > input.maximumStartDifference) return@mapNotNull null
            val deviceMatched = candidate.associatedDeviceId == null || candidate.associatedDeviceId == input.watch.deviceId
            if (!deviceMatched) return@mapNotNull null
            val overlapStart = maxOf(candidate.startedAt, input.watch.startedAt)
            val overlapEnd = minOf(candidate.endedAt, input.watch.endedAt)
            if (!overlapEnd.isAfter(overlapStart)) return@mapNotNull null
            val overlap = Duration.between(overlapStart, overlapEnd)
            val shorterDuration = minOf(
                Duration.between(candidate.startedAt, candidate.endedAt),
                Duration.between(input.watch.startedAt, input.watch.endedAt),
            )
            val ratio = overlap.secondsWithFraction() / shorterDuration.secondsWithFraction()
            if (ratio < input.minimumOverlapRatio) return@mapNotNull null
            candidate to WorkoutCorrelationEvidence(startDifference, overlap, ratio, deviceMatched)
        }
        return when (eligible.size) {
            0 -> WorkoutCorrelationResult.NoSafeCandidate
            1 -> WorkoutCorrelationResult.UniqueCandidate(eligible.single().first, eligible.single().second)
            else -> WorkoutCorrelationResult.Ambiguous(
                eligible.map { it.first.activityId }.sortedBy { it.value.toString() },
            )
        }
    }

    fun merge(input: WorkoutCorrelationMergeInput): WorkoutCorrelationMergeResult {
        val incomingKey = WatchWorkoutImportKey(
            deviceId = input.watchInput.deviceId,
            origin = DataOrigin.WATCH_D3_D4,
            crc = input.watchInput.crc,
        )
        val existingContext = input.phoneActivity.watchImportContext
        if (existingContext != null) {
            return if (existingContext.key == incomingKey) {
                WorkoutCorrelationMergeResult.IdempotentExisting(
                    activity = input.phoneActivity,
                    importKey = incomingKey,
                )
            } else {
                WorkoutCorrelationMergeResult.Conflict(
                    activityId = input.phoneActivity.id,
                    existingImportKey = existingContext.key,
                    incomingImportKey = incomingKey,
                )
            }
        }
        if (input.watchInput.parserValidity == ParserValidity.INVALID) {
            return WorkoutCorrelationMergeResult.Invalid(DomainValidationError.INVALID_STATE)
        }
        val expectedWatch = input.correlationInput.watch
        if (expectedWatch.deviceId != input.watchInput.deviceId ||
            expectedWatch.crc != input.watchInput.crc ||
            expectedWatch.startedAt != input.watchInput.metrics.startedAt ||
            expectedWatch.endedAt != input.watchInput.metrics.endedAt
        ) {
            return WorkoutCorrelationMergeResult.Invalid(DomainValidationError.INVALID_IDENTIFIER)
        }
        val unique = when (val correlation = evaluate(input.correlationInput)) {
            is WorkoutCorrelationResult.UniqueCandidate -> correlation
            else -> return WorkoutCorrelationMergeResult.NotSafe(correlation)
        }
        val candidate = unique.candidate
        if (input.phoneActivity.id != candidate.activityId ||
            input.phoneActivity.appWorkoutSessionId != candidate.sessionId ||
            input.phoneActivity.activityOrigin != candidate.origin ||
            input.phoneActivity.metrics.startedAt != candidate.startedAt ||
            input.phoneActivity.metrics.endedAt != candidate.endedAt
        ) {
            return WorkoutCorrelationMergeResult.Invalid(DomainValidationError.INVALID_IDENTIFIER)
        }
        val importRecord = when (val result = WatchWorkoutImport.create(
            id = input.importId,
            activityId = input.phoneActivity.id,
            input = input.watchInput,
            importedAt = input.importedAt,
        )) {
            is DomainResult.Success -> result.value
            is DomainResult.Failure -> return WorkoutCorrelationMergeResult.Invalid(result.error)
        }
        return when (val merged = input.phoneActivity.mergeCorrelatedWatchImport(input.watchInput, input.importedAt)) {
            is DomainResult.Success -> WorkoutCorrelationMergeResult.Merged(WorkoutCorrelationMergeOutput(
                activity = merged.value,
                importRecord = importRecord,
                evidence = unique.evidence,
                sourceSelection = WorkoutMetricKind.entries.associateWith(WorkoutSourcePriority::primarySource),
            ))
            is DomainResult.Failure -> WorkoutCorrelationMergeResult.Invalid(merged.error)
        }
    }

    private fun Duration.secondsWithFraction(): Double = seconds.toDouble() + nano / 1_000_000_000.0
}

data class WorkoutCorrelationMergeInput(
    val importId: WatchWorkoutImportId,
    val phoneActivity: WorkoutActivity,
    /** Conjunto completo avaliado; merge nunca reduz candidatos antes de decidir unicidade. */
    val correlationInput: WorkoutCorrelationInput,
    val watchInput: WatchWorkoutParserInput,
    val importedAt: Instant,
)

sealed interface WorkoutCorrelationMergeResult {
    data class Merged(val output: WorkoutCorrelationMergeOutput) : WorkoutCorrelationMergeResult

    data class IdempotentExisting(
        val activity: WorkoutActivity,
        val importKey: WatchWorkoutImportKey,
    ) : WorkoutCorrelationMergeResult

    data class Conflict(
        val activityId: WorkoutActivityId,
        val existingImportKey: WatchWorkoutImportKey,
        val incomingImportKey: WatchWorkoutImportKey,
    ) : WorkoutCorrelationMergeResult

    data class NotSafe(val correlation: WorkoutCorrelationResult) : WorkoutCorrelationMergeResult

    data class Invalid(val error: DomainValidationError) : WorkoutCorrelationMergeResult
}

data class WorkoutCorrelationMergeOutput(
    val activity: WorkoutActivity,
    val importRecord: WatchWorkoutImport,
    val evidence: WorkoutCorrelationEvidence,
    val sourceSelection: Map<WorkoutMetricKind, WorkoutMetricSource>,
)
