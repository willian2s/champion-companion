package br.com.mwconsulting.championcompanion.core.healthconnect

import br.com.mwconsulting.championcompanion.core.model.HEALTH_SYNC_FROZEN_PROJECTION_PAYLOAD_WORKOUT_STEPS_SCHEMA_VERSION
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionChildKey
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregate
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenInstant
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenPayload
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenProjectionPayload
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenStepsCadenceSample
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperation
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationKey
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.SafeSyncError
import br.com.mwconsulting.championcompanion.core.model.UserProfile
import br.com.mwconsulting.championcompanion.core.model.WeightMeasurement
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Boundary defensiva entre dados locais e os construtores estritos do SDK
 * Health Connect. Exceções de validação do SDK nunca devem escapar até a UI.
 */
@Singleton
class SafeHealthRecordMapper @Inject constructor(
    private val delegate: DefaultHealthRecordMapper,
) : HealthRecordMapper {
    private val frozenProjectionJson = Json {
        encodeDefaults = true
        explicitNulls = true
    }

    override fun mapDailySummary(operation: HealthSyncOperation): HealthRecordMappingResult =
        mapSafely { delegate.mapDailySummary(operation) }

    override fun mapFrozenDailyD1Sample(operation: HealthSyncOperation): HealthRecordMappingResult =
        mapSafely { delegate.mapFrozenDailyD1Sample(operation) }

    override fun mapHeartRateMeasurement(
        operation: HealthSyncOperation,
        measurement: HeartRateMeasurement,
    ): HealthRecordMappingResult = mapSafely {
        delegate.mapHeartRateMeasurement(operation, measurement)
    }

    override fun mapWeightMeasurement(
        operation: HealthSyncOperation,
        measurement: WeightMeasurement,
    ): HealthRecordMappingResult = mapSafely {
        delegate.mapWeightMeasurement(operation, measurement)
    }

    override fun mapUserProfileHeight(
        operation: HealthSyncOperation,
        profile: UserProfile,
    ): HealthRecordMappingResult = mapSafely {
        delegate.mapUserProfileHeight(operation, profile)
    }

    override fun mapWorkoutActivity(
        operation: HealthSyncOperation,
        activity: WorkoutActivity,
    ): HealthRecordMappingResult = mapSafely {
        delegate.mapWorkoutActivity(operation, activity)
    }

    override fun mapFrozenWorkoutActivity(
        operation: HealthSyncOperation,
    ): HealthRecordMappingResult = mapSafely {
        delegate.mapFrozenWorkoutActivity(operation)
    }

    override fun freezeWorkoutProjection(
        key: HealthSyncOperationKey,
        activity: WorkoutActivity,
    ): HealthSyncFrozenPayloadResult = when (key.projectionChildKey) {
        HealthProjectionChildKey.WorkoutSteps -> freezeWorkoutSteps(key, activity)
        HealthProjectionChildKey.StepsCadenceSeries -> freezeStepsCadence(key, activity)
        else -> try {
            delegate.freezeWorkoutProjection(key, activity)
        } catch (_: IllegalArgumentException) {
            invalidFrozenPayload()
        } catch (_: IllegalStateException) {
            invalidFrozenPayload()
        }
    }

    override fun findEligibleWorkoutProjectionChildren(
        activity: WorkoutActivity,
    ): List<HealthProjectionChildKey> = delegate.findEligibleWorkoutProjectionChildren(activity)

    override fun findEligibleWorkoutRecordTypes(
        activity: WorkoutActivity,
    ): Set<HealthSyncRecordType> = delegate.findEligibleWorkoutRecordTypes(activity)

    private fun freezeWorkoutSteps(
        key: HealthSyncOperationKey,
        activity: WorkoutActivity,
    ): HealthSyncFrozenPayloadResult {
        if (key.aggregate != HealthSyncAggregate.WorkoutActivity(activity.id) ||
            key.recordType != HealthSyncRecordType.STEPS
        ) {
            return invalidFrozenPayload("Identidade da projeção de passos inválida.")
        }
        val steps = activity.metrics.steps
        if (!activity.metrics.stepsAvailable || steps !in MIN_STEPS_RECORD_COUNT..MAX_STEPS_RECORD_COUNT) {
            return invalidFrozenPayload("Passos da atividade fora do intervalo aceito pelo Health Connect.")
        }
        val window = activity.healthProjectionWindowOrNull()
            ?: return invalidFrozenPayload("Janela da projeção de passos inválida.")
        val zoneId = ZoneId.systemDefault()
        return encodeWorkoutMetricPayload(
            HealthSyncFrozenProjectionPayload(
                recordType = HealthSyncRecordType.STEPS.name,
                projectionChildKey = HealthProjectionChildKey.WorkoutSteps.stableValue,
                start = window.start.toFrozenInstant(),
                end = window.end.toFrozenInstant(),
                startZoneOffsetSeconds = zoneId.rules.getOffset(window.start).totalSeconds,
                endZoneOffsetSeconds = zoneId.rules.getOffset(window.end).totalSeconds,
                steps = steps,
            ),
        )
    }

    private fun freezeStepsCadence(
        key: HealthSyncOperationKey,
        activity: WorkoutActivity,
    ): HealthSyncFrozenPayloadResult {
        if (key.aggregate != HealthSyncAggregate.WorkoutActivity(activity.id) ||
            key.recordType != HealthSyncRecordType.STEPS_CADENCE
        ) {
            return invalidFrozenPayload("Identidade da projeção de cadência inválida.")
        }
        val window = activity.healthProjectionWindowOrNull()
            ?: return invalidFrozenPayload("Janela da projeção de cadência inválida.")
        val intervals = activity.derivationSnapshot
            ?.takeIf { it.isEligibleForHealthProjection }
            ?.canonicalSeries
            ?.intervals
            .orEmpty()
        if (intervals.isEmpty()) {
            return invalidFrozenPayload("Atividade sem intervalos elegíveis para cadência.")
        }
        val samples = try {
            intervals.map { interval ->
                val cadence = interval.cadenceStepsPerMinute
                if (!cadence.isFinite() || cadence !in MIN_STEPS_CADENCE_RATE..MAX_STEPS_CADENCE_RATE) {
                    return invalidFrozenPayload(
                        "Cadência da atividade fora do intervalo aceito pelo Health Connect.",
                    )
                }
                val sampledAt = interval.effectiveWindow.start
                    .plus(interval.effectiveDuration.dividedBy(2))
                HealthSyncFrozenStepsCadenceSample(
                    sampledAt = sampledAt.toFrozenInstant(),
                    stepsPerMinute = cadence,
                )
            }
        } catch (_: ArithmeticException) {
            return invalidFrozenPayload("Não foi possível calcular os instantes da cadência.")
        }
        val zoneId = ZoneId.systemDefault()
        return try {
            encodeWorkoutMetricPayload(
                HealthSyncFrozenProjectionPayload(
                    recordType = HealthSyncRecordType.STEPS_CADENCE.name,
                    projectionChildKey = HealthProjectionChildKey.StepsCadenceSeries.stableValue,
                    start = window.start.toFrozenInstant(),
                    end = window.end.toFrozenInstant(),
                    startZoneOffsetSeconds = zoneId.rules.getOffset(window.start).totalSeconds,
                    endZoneOffsetSeconds = zoneId.rules.getOffset(window.end).totalSeconds,
                    stepsCadenceSamples = samples,
                ),
            )
        } catch (_: IllegalArgumentException) {
            invalidFrozenPayload("Série de cadência incompatível com a janela da atividade.")
        } catch (_: IllegalStateException) {
            invalidFrozenPayload("Série de cadência incompatível com a janela da atividade.")
        }
    }

    private fun encodeWorkoutMetricPayload(
        payload: HealthSyncFrozenProjectionPayload,
    ): HealthSyncFrozenPayloadResult = try {
        HealthSyncFrozenPayloadResult.Success(
            HealthSyncFrozenPayload(
                schemaVersion = HEALTH_SYNC_FROZEN_PROJECTION_PAYLOAD_WORKOUT_STEPS_SCHEMA_VERSION,
                serializedPayload = frozenProjectionJson.encodeToString(payload),
            ),
        )
    } catch (_: IllegalArgumentException) {
        invalidFrozenPayload()
    } catch (_: IllegalStateException) {
        invalidFrozenPayload()
    }

    private inline fun mapSafely(
        block: () -> HealthRecordMappingResult,
    ): HealthRecordMappingResult = try {
        block()
    } catch (_: IllegalArgumentException) {
        invalidMappedRecord()
    } catch (_: IllegalStateException) {
        invalidMappedRecord()
    }

    private fun invalidMappedRecord(): HealthRecordMappingResult.Failure =
        HealthRecordMappingResult.Failure(
            error = SafeSyncError.INVALID_LOCAL_DATA,
            reason = "Dados locais incompatíveis com o contrato do Health Connect.",
        )

    private fun invalidFrozenPayload(
        reason: String = "Não foi possível preparar a projeção para o Health Connect.",
    ): HealthSyncFrozenPayloadResult.Failure = HealthSyncFrozenPayloadResult.Failure(
        error = SafeSyncError.INVALID_LOCAL_DATA,
        reason = reason,
    )

    private fun WorkoutActivity.healthProjectionWindowOrNull(): HealthProjectionWindow? {
        val duration = metrics.duration
        if (duration.isZero || duration.isNegative) return null
        val start = metrics.startedAt
        val end = try {
            start.plus(duration)
        } catch (_: RuntimeException) {
            return null
        }
        if (!end.isAfter(start)) return null
        return HealthProjectionWindow(start, end)
    }

    private fun Instant.toFrozenInstant(): HealthSyncFrozenInstant = HealthSyncFrozenInstant(
        epochSecond = epochSecond,
        nano = nano,
    )

    private data class HealthProjectionWindow(
        val start: Instant,
        val end: Instant,
    )

    private companion object {
        const val MIN_STEPS_RECORD_COUNT = 1L
        const val MAX_STEPS_RECORD_COUNT = 1_000_000L
        const val MIN_STEPS_CADENCE_RATE = 0.0
        const val MAX_STEPS_CADENCE_RATE = 10_000.0
    }
}
