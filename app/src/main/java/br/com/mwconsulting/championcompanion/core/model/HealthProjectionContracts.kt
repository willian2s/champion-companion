package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import java.time.ZoneId
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

    data object ManualDistance : HealthProjectionChildKey {
        override val stableValue: String = "manual-distance"
        override val recordTypeName: String = HealthSyncRecordType.DISTANCE.name
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

    /** Passos da atividade, separado da operação cumulativa do Daily Summary. */
    data object WorkoutSteps : HealthProjectionChildKey {
        override val stableValue: String = "workout-steps"
        override val recordTypeName: String = HealthSyncRecordType.STEPS.name
    }

    /** Série de cadência da atividade, sem participação no crédito diário. */
    data object StepsCadenceSeries : HealthProjectionChildKey {
        override val stableValue: String = "steps-cadence-series"
        override val recordTypeName: String = HealthSyncRecordType.STEPS_CADENCE.name
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
            value == ManualDistance.stableValue -> ManualDistance
            value == SpeedSeries.stableValue -> SpeedSeries
            value == HeartRateSeries.stableValue -> HeartRateSeries
            value == TotalCaloriesBurned.stableValue -> TotalCaloriesBurned
            value == WorkoutSteps.stableValue -> WorkoutSteps
            value == StepsCadenceSeries.stableValue -> StepsCadenceSeries
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

/** Estado local da identidade do agregado legado de distância. */
enum class HealthLegacyDistanceProjectionStatus {
    ABSENT,
    KNOWN_PENDING_DELETE,
    RECONCILED,
    UNKNOWN_ID,
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
        require(
            schemaVersion == HEALTH_SYNC_FROZEN_PROJECTION_PAYLOAD_LEGACY_SCHEMA_VERSION ||
                schemaVersion == HEALTH_SYNC_FROZEN_PROJECTION_PAYLOAD_SCHEMA_VERSION ||
                schemaVersion == HEALTH_SYNC_FROZEN_PROJECTION_PAYLOAD_WORKOUT_STEPS_SCHEMA_VERSION ||
                schemaVersion == HEALTH_SYNC_FROZEN_DAILY_D1_PAYLOAD_SCHEMA_VERSION,
        )
        require(serializedPayload.isNotBlank())
    }
}

const val HEALTH_SYNC_FROZEN_PROJECTION_PAYLOAD_LEGACY_SCHEMA_VERSION: Int = 1
const val HEALTH_SYNC_FROZEN_PROJECTION_PAYLOAD_SCHEMA_VERSION: Int = 2
/** Schema reservado para payloads de passos e cadência da atividade. */
const val HEALTH_SYNC_FROZEN_PROJECTION_PAYLOAD_WORKOUT_STEPS_SCHEMA_VERSION: Int = 3
/**
 * Bucket D1 de passos. A evidência física 033-06 promoveu o schema reservado v4
 * para uma janela fixa de cinco minutos terminando no timestamp persistido.
 */
const val HEALTH_SYNC_FROZEN_DAILY_D1_PAYLOAD_SCHEMA_VERSION: Int = 4

const val HEALTH_SYNC_METRIC_PROVENANCE_WATCH_DERIVED: String = "WATCH_DERIVED"
const val HEALTH_SYNC_METRIC_PROVENANCE_EXTERNAL_REFERENCE_CALIBRATION: String =
    "EXTERNAL_REFERENCE_CALIBRATION"

private val HEALTH_SYNC_ALLOWED_METRIC_PROVENANCES = setOf(
    HEALTH_SYNC_METRIC_PROVENANCE_WATCH_DERIVED,
    HEALTH_SYNC_METRIC_PROVENANCE_EXTERNAL_REFERENCE_CALIBRATION,
)

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

/** Amostra de cadência congelada para retry de StepsCadenceRecord. */
@Serializable
data class HealthSyncFrozenStepsCadenceSample(
    val sampledAt: HealthSyncFrozenInstant,
    val stepsPerMinute: Double,
) {
    init {
        require(stepsPerMinute.isFinite() && stepsPerMinute >= 0.0)
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
    /** Ausente em payloads legados e em filhos sem métrica de distância/velocidade. */
    val metricProvenance: String? = null,
    /** Preenchido somente pelo filho [HealthProjectionChildKey.WorkoutSteps]. */
    val steps: Long? = null,
    /** Preenchido somente pelo filho [HealthProjectionChildKey.StepsCadenceSeries]. */
    val stepsCadenceSamples: List<HealthSyncFrozenStepsCadenceSample> = emptyList(),
) {
    init {
        require(recordType.isNotBlank())
        require(projectionChildKey.isNotBlank())
        require(startZoneOffsetSeconds in -64_800..64_800)
        require(endZoneOffsetSeconds in -64_800..64_800)
        require(distanceMeters?.let { it.isFinite() && it >= 0.0 } != false)
        require(caloriesKilocalories?.let { it.isFinite() && it >= 0.0 } != false)
        require(metricProvenance == null || metricProvenance in HEALTH_SYNC_ALLOWED_METRIC_PROVENANCES)
        require(
            when (projectionChildKey) {
                HealthProjectionChildKey.WorkoutSteps.stableValue ->
                    recordType == HealthSyncRecordType.STEPS.name &&
                        steps != null && steps > 0L && stepsCadenceSamples.isEmpty()

                HealthProjectionChildKey.StepsCadenceSeries.stableValue ->
                    recordType == HealthSyncRecordType.STEPS_CADENCE.name &&
                        steps == null && stepsCadenceSamples.isNotEmpty()

                else -> steps == null && stepsCadenceSamples.isEmpty()
            },
        )
        require(
            stepsCadenceSamples.zipWithNext().all { (first, second) ->
                first.sampledAt.toInstantOrNull()?.isBefore(second.sampledAt.toInstantOrNull()) == true
            },
        )
        if (stepsCadenceSamples.isNotEmpty()) {
            val startInstant = start.toInstantOrNull()
            val endInstant = end.toInstantOrNull()
            require(startInstant != null && endInstant != null && startInstant.isBefore(endInstant))
            require(stepsCadenceSamples.all { sample ->
                sample.sampledAt.toInstantOrNull()?.let { sampledAt ->
                    !sampledAt.isBefore(startInstant) && sampledAt.isBefore(endInstant)
                } == true
            })
        }
    }
}

/**
 * Bucket D1 congelado para retry. O timestamp D1 observado é a borda final do
 * bucket; [start] é exatamente cinco minutos antes de [end]. Nenhum residual
 * FEA1-D1 participa deste payload.
 */
@Serializable
data class HealthSyncFrozenDailyD1Payload(
    val recordType: String,
    val sampleId: String,
    val start: HealthSyncFrozenInstant,
    val end: HealthSyncFrozenInstant,
    val zoneId: String,
    val startZoneOffsetSeconds: Int,
    val endZoneOffsetSeconds: Int,
    val steps: Long,
) {
    init {
        require(recordType == HealthSyncRecordType.STEPS.name)
        require(sampleId.isNotBlank())
        require(runCatching { ZoneId.of(zoneId) }.isSuccess)
        require(startZoneOffsetSeconds in -64_800..64_800)
        require(endZoneOffsetSeconds in -64_800..64_800)
        require(steps > 0L)
        val startInstant = start.toInstantOrNull()
        val endInstant = end.toInstantOrNull()
        require(startInstant != null && endInstant != null && startInstant.isBefore(endInstant))
        require(java.time.Duration.between(startInstant, endInstant) == java.time.Duration.ofMinutes(5))
    }
}

private fun HealthSyncFrozenInstant.toInstantOrNull(): Instant? = runCatching {
    Instant.ofEpochSecond(epochSecond, nano.toLong())
}.getOrNull()

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
