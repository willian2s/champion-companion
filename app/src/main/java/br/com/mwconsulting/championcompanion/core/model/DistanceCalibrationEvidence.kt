package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant

/** Evidência manual de distância nativa, independente de calibração por passo. */
@ConsistentCopyVisibility
data class DistanceCalibrationEvidence private constructor(
    val id: DistanceCalibrationEvidenceId,
    val profileId: UserProfileId,
    val activityId: WorkoutActivityId,
    val workoutType: WorkoutType,
    val referenceDistanceMeters: Double,
    val watchDistanceMeters: Double,
    val acceptedAt: Instant,
    val updatedAt: Instant,
    val isActive: Boolean,
) {
    val distanceCalibrationFactor: Double
        get() = referenceDistanceMeters / watchDistanceMeters

    companion object {
        fun create(
            id: DistanceCalibrationEvidenceId,
            profileId: UserProfileId,
            activityId: WorkoutActivityId,
            workoutType: WorkoutType,
            referenceDistanceMeters: Double,
            watchDistanceMeters: Double,
            acceptedAt: Instant,
            updatedAt: Instant,
            isActive: Boolean = true,
        ): DomainResult<DistanceCalibrationEvidence> = when {
            workoutType !in CALIBRATABLE_WORKOUT_TYPES ->
                DomainResult.Failure(DomainValidationError.INVALID_STATE)
            !referenceDistanceMeters.isFinite() || referenceDistanceMeters <= 0.0 ||
                !watchDistanceMeters.isFinite() || watchDistanceMeters <= 0.0 ->
                DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            !(referenceDistanceMeters / watchDistanceMeters).isFinite() ||
                referenceDistanceMeters / watchDistanceMeters <= 0.0 ->
                DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            updatedAt.isBefore(acceptedAt) ->
                DomainResult.Failure(DomainValidationError.INVALID_TEMPORAL_ORDER)
            else -> DomainResult.Success(
                DistanceCalibrationEvidence(
                    id = id,
                    profileId = profileId,
                    activityId = activityId,
                    workoutType = workoutType,
                    referenceDistanceMeters = referenceDistanceMeters,
                    watchDistanceMeters = watchDistanceMeters,
                    acceptedAt = acceptedAt,
                    updatedAt = updatedAt,
                    isActive = isActive,
                ),
            )
        }
    }
}

@JvmInline
value class DistanceCalibrationEvidenceId(val value: java.util.UUID)

/** Agregado futuro; somente evidências nativas ativas participam do cálculo. */
data class LearnedDistanceCalibrationProfile(
    val workoutType: WorkoutType,
    val distanceCalibrationFactor: Double,
    val sampleCount: Int,
    val totalReferenceDistanceMeters: Double,
    val totalWatchDistanceMeters: Double,
    val updatedAt: Instant,
)
