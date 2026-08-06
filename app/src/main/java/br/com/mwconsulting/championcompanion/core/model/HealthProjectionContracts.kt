package br.com.mwconsulting.championcompanion.core.model

/**
 * Identidade estável de um filho da projeção. Chaves não carregam valores de
 * saúde e cada chave representa uma única operação, nunca uma subfila.
 */
sealed interface HealthProjectionChildKey {
    val stableValue: String
    val recordTypeName: String

    data object ExerciseSession : HealthProjectionChildKey {
        override val stableValue: String = "exercise-session"
        override val recordTypeName: String = HealthSyncRecordType.EXERCISE_SESSION.name
    }

    @ConsistentCopyVisibility
    data class DistanceInterval private constructor(
        val intervalIndex: Int,
    ) : HealthProjectionChildKey {
        override val stableValue: String = "distance-interval-$intervalIndex"
        override val recordTypeName: String = HealthSyncRecordType.DISTANCE.name

        companion object {
            fun create(intervalIndex: Int): DomainResult<DistanceInterval> = if (intervalIndex >= 0) {
                DomainResult.Success(DistanceInterval(intervalIndex))
            } else {
                DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
        }
    }

    data object SpeedSeries : HealthProjectionChildKey {
        override val stableValue: String = "speed-series"
        override val recordTypeName: String = HealthSyncRecordType.SPEED.name
    }

    data object HeartRateSeries : HealthProjectionChildKey {
        override val stableValue: String = "heart-rate-series"
        override val recordTypeName: String = HealthSyncRecordType.HEART_RATE.name
    }

    data object TotalCaloriesBurned : HealthProjectionChildKey {
        override val stableValue: String = "total-calories-burned"
        override val recordTypeName: String = HealthSyncRecordType.TOTAL_CALORIES_BURNED.name
    }

    companion object {
        val Exercise = ExerciseSession
        val Speed = SpeedSeries
        val HeartRate = HeartRateSeries
        val Calories = TotalCaloriesBurned
    }
}

val HealthProjectionChildKey.isSpeedSeries: Boolean
    get() = this is HealthProjectionChildKey.SpeedSeries
