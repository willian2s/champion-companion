package br.com.mwconsulting.championcompanion.core.healthconnect

import br.com.mwconsulting.championcompanion.core.model.HealthProjectionChildKey
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperation
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationKey
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.SafeSyncError
import br.com.mwconsulting.championcompanion.core.model.UserProfile
import br.com.mwconsulting.championcompanion.core.model.WeightMeasurement
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Boundary defensiva entre dados locais e os construtores estritos do SDK
 * Health Connect. Exceções de validação do SDK nunca devem escapar até a UI.
 */
@Singleton
class SafeHealthRecordMapper @Inject constructor(
    private val delegate: DefaultHealthRecordMapper,
) : HealthRecordMapper {
    override fun mapDailySummary(operation: HealthSyncOperation): HealthRecordMappingResult =
        mapSafely { delegate.mapDailySummary(operation) }

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
    ): HealthSyncFrozenPayloadResult {
        when (key.projectionChildKey) {
            HealthProjectionChildKey.WorkoutSteps -> {
                if (activity.metrics.steps !in MIN_STEPS_RECORD_COUNT..MAX_STEPS_RECORD_COUNT) {
                    return HealthSyncFrozenPayloadResult.Failure(
                        SafeSyncError.INVALID_LOCAL_DATA,
                        "Passos da atividade fora do intervalo aceito pelo Health Connect.",
                    )
                }
            }

            HealthProjectionChildKey.StepsCadenceSeries -> {
                val intervals = activity.derivationSnapshot?.canonicalSeries?.intervals.orEmpty()
                if (intervals.isEmpty() || intervals.any { interval ->
                        val cadence = interval.cadenceStepsPerMinute
                        !cadence.isFinite() || cadence !in MIN_STEPS_CADENCE_RATE..MAX_STEPS_CADENCE_RATE
                    }
                ) {
                    return HealthSyncFrozenPayloadResult.Failure(
                        SafeSyncError.INVALID_LOCAL_DATA,
                        "Cadência da atividade fora do intervalo aceito pelo Health Connect.",
                    )
                }
            }

            else -> Unit
        }

        return try {
            delegate.freezeWorkoutProjection(key, activity)
        } catch (_: IllegalArgumentException) {
            invalidFrozenPayload()
        } catch (_: IllegalStateException) {
            invalidFrozenPayload()
        }
    }

    override fun findEligibleWorkoutProjectionChildren(
        activity: WorkoutActivity,
    ): List<HealthProjectionChildKey> = try {
        delegate.findEligibleWorkoutProjectionChildren(activity)
    } catch (_: IllegalArgumentException) {
        emptyList()
    } catch (_: IllegalStateException) {
        emptyList()
    }

    override fun findEligibleWorkoutRecordTypes(
        activity: WorkoutActivity,
    ): Set<HealthSyncRecordType> = try {
        delegate.findEligibleWorkoutRecordTypes(activity)
    } catch (_: IllegalArgumentException) {
        emptySet()
    } catch (_: IllegalStateException) {
        emptySet()
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

    private fun invalidFrozenPayload(): HealthSyncFrozenPayloadResult.Failure =
        HealthSyncFrozenPayloadResult.Failure(
            error = SafeSyncError.INVALID_LOCAL_DATA,
            reason = "Não foi possível preparar a projeção para o Health Connect.",
        )

    private companion object {
        const val MIN_STEPS_RECORD_COUNT = 1L
        const val MAX_STEPS_RECORD_COUNT = 1_000_000L
        const val MIN_STEPS_CADENCE_RATE = 0.0
        const val MAX_STEPS_CADENCE_RATE = 10_000.0
    }
}
