package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant

enum class AppWorkoutSessionState {
    CREATED,
    ACTIVE,
    PAUSED,
    AWAITING_RECOVERY,
    FINISHED,
    DISCARDED,
}

data class AppWorkoutSessionAggregates(
    val steps: Long,
    val distanceMeters: DomainValue<Double>,
    val caloriesKilocalories: DomainValue<Double>,
    val averageBpm: DomainValue<Int>,
    val minimumBpm: DomainValue<Int>,
    val maximumBpm: DomainValue<Int>,
)

/** Persisted session state only. Location samples and service state never enter domain. */
@ConsistentCopyVisibility
data class AppWorkoutSession private constructor(
    val id: AppWorkoutSessionId,
    val type: WorkoutType,
    val createdAt: Instant,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val activeDuration: Duration,
    val pausedDuration: Duration,
    val activeSince: Instant?,
    val pausedSince: Instant?,
    val state: AppWorkoutSessionState,
    val aggregates: AppWorkoutSessionAggregates,
    val workoutActivityId: WorkoutActivityId?,
) {
    fun start(at: Instant): DomainResult<AppWorkoutSession> =
        if (state == AppWorkoutSessionState.CREATED && !at.isBefore(createdAt)) {
            DomainResult.Success(copy(
                startedAt = at,
                activeSince = at,
                state = AppWorkoutSessionState.ACTIVE,
            ))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

    fun pause(at: Instant): DomainResult<AppWorkoutSession> =
        if (state == AppWorkoutSessionState.ACTIVE && isAtOrAfter(at, activeSince)) {
            DomainResult.Success(copy(
                activeDuration = activeDuration + Duration.between(activeSince!!, at),
                activeSince = null,
                pausedSince = at,
                state = AppWorkoutSessionState.PAUSED,
            ))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

    fun resume(at: Instant): DomainResult<AppWorkoutSession> =
        if (state == AppWorkoutSessionState.PAUSED && isAtOrAfter(at, pausedSince)) {
            DomainResult.Success(copy(
                pausedDuration = pausedDuration + Duration.between(pausedSince!!, at),
                pausedSince = null,
                activeSince = at,
                state = AppWorkoutSessionState.ACTIVE,
            ))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

    fun interrupt(at: Instant): DomainResult<AppWorkoutSession> = when (state) {
        AppWorkoutSessionState.ACTIVE -> if (isAtOrAfter(at, activeSince)) {
            DomainResult.Success(copy(
                activeDuration = activeDuration + Duration.between(activeSince!!, at),
                activeSince = null,
                state = AppWorkoutSessionState.AWAITING_RECOVERY,
            ))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_TEMPORAL_ORDER)
        }

        AppWorkoutSessionState.PAUSED -> if (isAtOrAfter(at, pausedSince)) {
            DomainResult.Success(copy(
                pausedDuration = pausedDuration + Duration.between(pausedSince!!, at),
                pausedSince = null,
                state = AppWorkoutSessionState.AWAITING_RECOVERY,
            ))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_TEMPORAL_ORDER)
        }

        else -> DomainResult.Failure(DomainValidationError.INVALID_STATE)
    }

    fun recover(at: Instant): DomainResult<AppWorkoutSession> =
        if (state == AppWorkoutSessionState.AWAITING_RECOVERY && !at.isBefore(createdAt)) {
            DomainResult.Success(copy(activeSince = at, state = AppWorkoutSessionState.ACTIVE))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

    fun updateAggregates(aggregates: AppWorkoutSessionAggregates): DomainResult<AppWorkoutSession> =
        if (state in setOf(AppWorkoutSessionState.FINISHED, AppWorkoutSessionState.DISCARDED) ||
            !areValid(aggregates)
        ) {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        } else {
            DomainResult.Success(copy(aggregates = aggregates))
        }

    fun finish(activityId: WorkoutActivityId, at: Instant): DomainResult<AppWorkoutSession> {
        if (state !in setOf(
                AppWorkoutSessionState.ACTIVE,
                AppWorkoutSessionState.PAUSED,
                AppWorkoutSessionState.AWAITING_RECOVERY,
            ) || !isAtOrAfter(at, startedAt)
        ) {
            return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }
        val finalActiveDuration = if (state == AppWorkoutSessionState.ACTIVE) {
            activeDuration + Duration.between(activeSince!!, at)
        } else {
            activeDuration
        }
        val finalPausedDuration = if (state == AppWorkoutSessionState.PAUSED) {
            pausedDuration + Duration.between(pausedSince!!, at)
        } else {
            pausedDuration
        }
        return DomainResult.Success(copy(
            endedAt = at,
            activeDuration = finalActiveDuration,
            pausedDuration = finalPausedDuration,
            activeSince = null,
            pausedSince = null,
            state = AppWorkoutSessionState.FINISHED,
            workoutActivityId = activityId,
        ))
    }

    fun discard(): DomainResult<AppWorkoutSession> =
        if (state !in setOf(AppWorkoutSessionState.FINISHED, AppWorkoutSessionState.DISCARDED)) {
            DomainResult.Success(copy(activeSince = null, pausedSince = null, state = AppWorkoutSessionState.DISCARDED))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

    companion object {
        fun create(
            id: AppWorkoutSessionId,
            type: WorkoutType,
            createdAt: Instant,
        ): DomainResult<AppWorkoutSession> =
            if (type == WorkoutType.UNKNOWN) {
                DomainResult.Failure(DomainValidationError.INVALID_STATE)
            } else {
                DomainResult.Success(AppWorkoutSession(
                    id = id,
                    type = type,
                    createdAt = createdAt,
                    startedAt = null,
                    endedAt = null,
                    activeDuration = Duration.ZERO,
                    pausedDuration = Duration.ZERO,
                    activeSince = null,
                    pausedSince = null,
                    state = AppWorkoutSessionState.CREATED,
                    aggregates = AppWorkoutSessionAggregates(
                        steps = 0,
                        distanceMeters = DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED),
                        caloriesKilocalories = DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED),
                        averageBpm = DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED),
                        minimumBpm = DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED),
                        maximumBpm = DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED),
                    ),
                    workoutActivityId = null,
                ))
            }

        private fun isAtOrAfter(at: Instant, reference: Instant?): Boolean =
            reference != null && !at.isBefore(reference)

        private fun areValid(aggregates: AppWorkoutSessionAggregates): Boolean =
            aggregates.steps >= 0 && isNonNegative(aggregates.distanceMeters) &&
                isNonNegative(aggregates.caloriesKilocalories) &&
                isValidBpm(aggregates.averageBpm) && isValidBpm(aggregates.minimumBpm) &&
                isValidBpm(aggregates.maximumBpm)

        private fun isNonNegative(value: DomainValue<Double>): Boolean =
            value !is DomainValue.Present || (value.value.isFinite() && value.value >= 0.0)

        private fun isValidBpm(value: DomainValue<Int>): Boolean =
            value !is DomainValue.Present || value.value in 30..220
    }
}
