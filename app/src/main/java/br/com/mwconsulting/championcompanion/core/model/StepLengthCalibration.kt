package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant

/** Evidência externa de comprimento de passo para contexto de treino específico. */
@ConsistentCopyVisibility
data class StepLengthCalibration private constructor(
    val id: StepLengthCalibrationId,
    val profileId: UserProfileId,
    val activityId: WorkoutActivityId,
    val workoutType: WorkoutType,
    val referenceDistanceMeters: Double,
    val decodedSteps: Long,
    val acceptedAt: Instant,
    val updatedAt: Instant,
) {
    val observedStepLengthMeters: Double
        get() = referenceDistanceMeters / decodedSteps.toDouble()

    companion object {
        fun create(
            id: StepLengthCalibrationId,
            profileId: UserProfileId,
            activityId: WorkoutActivityId,
            workoutType: WorkoutType,
            referenceDistanceMeters: Double,
            decodedSteps: Long,
            acceptedAt: Instant,
            updatedAt: Instant,
        ): DomainResult<StepLengthCalibration> = when {
            workoutType !in CALIBRATABLE_WORKOUT_TYPES -> DomainResult.Failure(DomainValidationError.INVALID_STATE)
            !referenceDistanceMeters.isFinite() || referenceDistanceMeters <= 0.0 ->
                DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            decodedSteps <= 0L -> DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            updatedAt.isBefore(acceptedAt) -> DomainResult.Failure(DomainValidationError.INVALID_TEMPORAL_ORDER)
            !observedStepLengthMetersIsValid(referenceDistanceMeters, decodedSteps) ->
                DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            else -> DomainResult.Success(
                StepLengthCalibration(
                    id = id,
                    profileId = profileId,
                    activityId = activityId,
                    workoutType = workoutType,
                    referenceDistanceMeters = referenceDistanceMeters,
                    decodedSteps = decodedSteps,
                    acceptedAt = acceptedAt,
                    updatedAt = updatedAt,
                ),
            )
        }

        private fun observedStepLengthMetersIsValid(distance: Double, steps: Long): Boolean =
            (distance / steps.toDouble()).isFinite() && distance / steps.toDouble() > 0.0
    }
}

@JvmInline
value class StepLengthCalibrationId(val value: java.util.UUID)

data class StepLengthCalibrationSummary(
    val id: StepLengthCalibrationId,
    val profileId: UserProfileId,
    val activityId: WorkoutActivityId,
    val workoutType: WorkoutType,
    val referenceDistanceMeters: Double,
    val decodedSteps: Long,
    val observedStepLengthMeters: Double,
    val acceptedAt: Instant,
    val updatedAt: Instant,
)

data class LearnedStepLengthProfile(
    val workoutType: WorkoutType,
    val stepLengthMeters: Double,
    val sampleCount: Int,
    val totalReferenceDistanceMeters: Double,
    val totalDecodedSteps: Long,
    val updatedAt: Instant,
)

internal val CALIBRATABLE_WORKOUT_TYPES: Set<WorkoutType> = setOf(
    WorkoutType.RUNNING,
    WorkoutType.WALKING,
)
