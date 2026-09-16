package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import java.time.LocalDate

enum class DeviceModel {
    CHAMPION_C001,
}

data class DeviceImportMetadata(
    val lastImportedAt: Instant? = null,
    val parserVersion: ParserVersion? = null,
)

@ConsistentCopyVisibility
data class Device private constructor(
    val id: DeviceId,
    val platformIdentifier: String,
    val name: String,
    val model: DeviceModel,
    val firmware: String?,
    val associatedAt: Instant,
    val lastSuccessfulConnectionAt: Instant?,
    val importMetadata: DeviceImportMetadata,
) {
    companion object {
        fun create(
            id: DeviceId,
            platformIdentifier: String,
            name: String,
            associatedAt: Instant,
            model: DeviceModel = DeviceModel.CHAMPION_C001,
            firmware: String? = null,
            lastSuccessfulConnectionAt: Instant? = null,
            importMetadata: DeviceImportMetadata = DeviceImportMetadata(),
        ): DomainResult<Device> {
            if (DomainValidation.requiredText(platformIdentifier) is DomainResult.Failure ||
                DomainValidation.requiredText(name) is DomainResult.Failure ||
                (firmware != null && DomainValidation.requiredText(firmware) is DomainResult.Failure)
            ) {
                return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }

            return DomainResult.Success(
                Device(
                    id = id,
                    platformIdentifier = platformIdentifier,
                    name = name,
                    model = model,
                    firmware = firmware,
                    associatedAt = associatedAt,
                    lastSuccessfulConnectionAt = lastSuccessfulConnectionAt,
                    importMetadata = importMetadata,
                ),
            )
        }
    }
}

enum class WeightUnit {
    KILOGRAMS,
    POUNDS,
}

@ConsistentCopyVisibility
data class WeightMeasurement private constructor(
    val id: WeightMeasurementId,
    val value: Double,
    val unit: WeightUnit,
    val measuredAt: Instant,
    val origin: DataOrigin,
) {
    companion object {
        fun create(
            id: WeightMeasurementId,
            value: Double,
            unit: WeightUnit,
            measuredAt: Instant,
            origin: DataOrigin,
        ): DomainResult<WeightMeasurement> =
            if (value.isFinite() && value > 0.0) {
                DomainResult.Success(WeightMeasurement(id, value, unit, measuredAt, origin))
            } else {
                DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
    }
}

@ConsistentCopyVisibility
data class UserProfile private constructor(
    val id: UserProfileId,
    val heightCentimeters: Double?,
    val updatedAt: Instant,
    val stepLengthProfile: StepLengthProfile? = null,
    val maxHeartRateBpm: Int? = null,
) {
    val stepLengthMeters: Double?
        get() = stepLengthProfile?.stepLengthMeters

    companion object {
        fun create(
            id: UserProfileId,
            updatedAt: Instant,
            heightCentimeters: Double? = null,
            stepLengthProfile: StepLengthProfile? = null,
            maxHeartRateBpm: Int? = null,
        ): DomainResult<UserProfile> =
            if ((heightCentimeters == null || (heightCentimeters.isFinite() && heightCentimeters > 0.0)) &&
                (maxHeartRateBpm == null || maxHeartRateBpm in 30..220)
            ) {
                DomainResult.Success(UserProfile(id, heightCentimeters, updatedAt, stepLengthProfile, maxHeartRateBpm))
            } else {
                DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
    }
}

data class DailySummaryKey(
    val deviceId: DeviceId?,
    val origin: DataOrigin,
    val localDate: LocalDate,
)

data class DailyGoalProgress(
    val goal: Int,
    val progress: Float,
)

data class DailySummaryEstimates(
    val strideLengthMeters: Double?,
    val distanceMeters: Double?,
    val caloriesKilocalories: Double?,
)

object DailySummaryPolicy {
    private const val estimatedStrideFromHeightFactor = 0.415
    private const val caloriesPerKilogramPerKilometer = 0.55

    fun sanitizeDailyStepsGoal(goal: Int): Int = if (goal > 0) goal else DEFAULT_DAILY_STEPS_GOAL

    fun progress(steps: Long, goal: Int): DailyGoalProgress {
        val safeGoal = sanitizeDailyStepsGoal(goal)
        val safeSteps = maxOf(0L, steps)
        val ratio = (safeSteps.toDouble() / safeGoal.toDouble()).coerceAtMost(1.0)
        return DailyGoalProgress(
            goal = safeGoal,
            progress = ratio.toFloat(),
        )
    }

    fun estimate(steps: Long, metrics: PersonalMetricsSnapshot): DailySummaryEstimates {
        val safeSteps = maxOf(0L, steps)
        val strideLengthMeters = resolveStrideLengthMeters(metrics)
        val distanceMeters = strideLengthMeters?.let { safeSteps * it }
            ?.takeIf { it.isFinite() && it > 0.0 }
        val caloriesKilocalories = metrics.weightKilograms
            ?.takeIf { it.isFinite() && it > 0.0 }
            ?.let { weightKg ->
                distanceMeters?.let { meters -> weightKg * (meters / 1_000.0) * caloriesPerKilogramPerKilometer }
            }
            ?.takeIf { it.isFinite() && it > 0.0 }

        return DailySummaryEstimates(
            strideLengthMeters = strideLengthMeters,
            distanceMeters = distanceMeters,
            caloriesKilocalories = caloriesKilocalories,
        )
    }

    private fun resolveStrideLengthMeters(metrics: PersonalMetricsSnapshot): Double? =
        metrics.manualStrideLengthMeters
            ?.takeIf { it.isFinite() && it > 0.0 }
            ?: metrics.heightCentimeters
                ?.takeIf { it.isFinite() && it > 0.0 }
                ?.let { (it / 100.0) * estimatedStrideFromHeightFactor }
                ?.takeIf { it.isFinite() && it > 0.0 }
}

@ConsistentCopyVisibility
data class DailySummary private constructor(
    val id: DailySummaryId,
    val key: DailySummaryKey,
    val steps: Long,
    val estimatedDistanceMeters: Double?,
    val estimatedCaloriesKilocalories: Double?,
    val collectedAt: Instant,
    val updatedAt: Instant,
    val validity: DataValidity,
) {
    fun update(
        steps: Long,
        estimatedDistanceMeters: Double?,
        estimatedCaloriesKilocalories: Double?,
        collectedAt: Instant,
        updatedAt: Instant,
        validity: DataValidity,
    ): DomainResult<DailySummary> = create(
        id = id,
        key = key,
        steps = steps,
        estimatedDistanceMeters = estimatedDistanceMeters,
        estimatedCaloriesKilocalories = estimatedCaloriesKilocalories,
        collectedAt = collectedAt,
        updatedAt = updatedAt,
        validity = validity,
    )

    companion object {
        fun create(
            id: DailySummaryId,
            key: DailySummaryKey,
            steps: Long,
            estimatedDistanceMeters: Double?,
            estimatedCaloriesKilocalories: Double?,
            collectedAt: Instant,
            updatedAt: Instant,
            validity: DataValidity,
        ): DomainResult<DailySummary> {
            if (key.origin == DataOrigin.WATCH_DAILY_SUMMARY && key.deviceId == null) {
                return DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
            }
            if (steps < 0 ||
                !isNonNegative(estimatedDistanceMeters) ||
                !isNonNegative(estimatedCaloriesKilocalories)
            ) {
                return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
            if (updatedAt.isBefore(collectedAt)) {
                return DomainResult.Failure(DomainValidationError.INVALID_TEMPORAL_ORDER)
            }

            return DomainResult.Success(
                DailySummary(
                    id = id,
                    key = key,
                    steps = steps,
                    estimatedDistanceMeters = estimatedDistanceMeters,
                    estimatedCaloriesKilocalories = estimatedCaloriesKilocalories,
                    collectedAt = collectedAt,
                    updatedAt = updatedAt,
                    validity = validity,
                ),
            )
        }

        private fun isNonNegative(value: Double?): Boolean =
            value == null || (value.isFinite() && value >= 0.0)
    }
}
