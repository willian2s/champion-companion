package br.com.mwconsulting.championcompanion.core.model

import kotlinx.serialization.Serializable

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
        override val stableValue: String = "interval-$intervalIndex"
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

        /**
         * Reconstitui somente chaves produzidas pelo contrato atual. Valores
         * desconhecidos falham fechados para não atribuir operação a filho
         * diferente.
         */
        fun fromStableValue(value: String): HealthProjectionChildKey? = when {
            value == ExerciseSession.stableValue -> ExerciseSession
            value == SpeedSeries.stableValue -> SpeedSeries
            value == HeartRateSeries.stableValue -> HeartRateSeries
            value == TotalCaloriesBurned.stableValue -> TotalCaloriesBurned
            value.startsWith("interval-") -> value
                .removePrefix("interval-")
                .toIntOrNull()
                ?.takeIf { it >= 0 }
                ?.let { DistanceInterval.create(it).valueOrNull() }
                ?.takeIf { it.stableValue == value }

            else -> null
        }
    }
}

val HealthProjectionChildKey.isSpeedSeries: Boolean
    get() = this is HealthProjectionChildKey.SpeedSeries

enum class HealthSyncOperationAction {
    INSERT,
    DELETE,
}

/** Estado do filho dentro do conjunto esperado pela reconciliação local. */
enum class HealthProjectionReconciliationState {
    EXPECTED,
    SENT,
    OBSOLETE,
}

/** Checkpoint local escrito antes e depois de cada efeito remoto futuro. */
enum class HealthProjectionReconciliationCheckpoint {
    NOT_STARTED,
    BEFORE_REMOTE_EFFECT,
    AFTER_REMOTE_EFFECT,
}

/**
 * Payload serializado e congelado para retry. O conteúdo pode conter dados do
 * record, mas nunca participa de identidade, logs ou diagnóstico.
 */
data class HealthSyncFrozenPayload(
    val schemaVersion: Int,
    val serializedPayload: String,
) {
    init {
        require(schemaVersion > 0)
        require(serializedPayload.isNotBlank())
    }
}

const val HEALTH_SYNC_FROZEN_PROJECTION_PAYLOAD_SCHEMA_VERSION: Int = 1

/** Instante lossless usado pelo payload remoto congelado. */
@Serializable
data class HealthSyncFrozenInstant(
    val epochSecond: Long,
    val nano: Int,
) {
    init {
        require(nano in 0..999_999_999)
    }
}

@Serializable
data class HealthSyncFrozenSpeedSample(
    val sampledAt: HealthSyncFrozenInstant,
    val speedMetersPerSecond: Double,
) {
    init {
        require(speedMetersPerSecond.isFinite() && speedMetersPerSecond >= 0.0)
    }
}

@Serializable
data class HealthSyncFrozenHeartRateSample(
    val sampledAt: HealthSyncFrozenInstant,
    val bpm: Int,
) {
    init {
        require(bpm in 30..220)
    }
}

/**
 * Conteúdo imutável de um filho de treino. Identidade fica na operação; este
 * payload contém somente dados necessários para recriar record em retry.
 */
@Serializable
data class HealthSyncFrozenProjectionPayload(
    val recordType: String,
    val projectionChildKey: String,
    val start: HealthSyncFrozenInstant,
    val end: HealthSyncFrozenInstant,
    val startZoneOffsetSeconds: Int,
    val endZoneOffsetSeconds: Int,
    val exerciseType: Int? = null,
    val title: String? = null,
    val notes: String? = null,
    val distanceMeters: Double? = null,
    val caloriesKilocalories: Double? = null,
    val speedSamples: List<HealthSyncFrozenSpeedSample> = emptyList(),
    val heartRateSamples: List<HealthSyncFrozenHeartRateSample> = emptyList(),
) {
    init {
        require(recordType.isNotBlank())
        require(projectionChildKey.isNotBlank())
        require(startZoneOffsetSeconds in -64_800..64_800)
        require(endZoneOffsetSeconds in -64_800..64_800)
        require(distanceMeters?.let { it.isFinite() && it >= 0.0 } != false)
        require(caloriesKilocalories?.let { it.isFinite() && it >= 0.0 } != false)
    }
}

/** Vínculo imutável com snapshot de derivação ou projeção que originou filho. */
data class HealthProjectionOrigin(
    val originId: String,
    val originVersion: String,
) {
    init {
        require(originId.isNotBlank())
        require(originVersion.isNotBlank())
    }
}

/** Manifesto local: cada entrada representa exatamente uma operação filha. */
data class HealthProjectionManifest(
    val aggregate: HealthSyncAggregate,
    val children: List<HealthProjectionChildKey>,
    val origin: HealthProjectionOrigin? = null,
) {
    init {
        require(aggregate is HealthSyncAggregate.WorkoutActivity)
        require(children.isNotEmpty())
        require(children.distinct().size == children.size)
        require(children.all { it.recordTypeName in HealthSyncRecordType.entries.map(Enum<*>::name) })
    }
}
