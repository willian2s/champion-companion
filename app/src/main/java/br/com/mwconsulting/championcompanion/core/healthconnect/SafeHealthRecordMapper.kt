package br.com.mwconsulting.championcompanion.core.healthconnect

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device as HealthConnectDevice
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import br.com.mwconsulting.championcompanion.core.model.HEALTH_SYNC_FROZEN_DAILY_D1_METRIC_PAYLOAD_SCHEMA_VERSION
import br.com.mwconsulting.championcompanion.core.model.HEALTH_SYNC_FROZEN_DAILY_D1_PAYLOAD_SCHEMA_VERSION
import br.com.mwconsulting.championcompanion.core.model.HEALTH_SYNC_FROZEN_PROJECTION_PAYLOAD_WORKOUT_STEPS_SCHEMA_VERSION
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionChildKey
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregate
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenDailyD1MetricPayload
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenDailyD1Payload
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenInstant
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenPayload
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenProjectionPayload
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenStepsCadenceSample
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperation
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationAction
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationKey
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.SafeSyncError
import br.com.mwconsulting.championcompanion.core.model.UserProfile
import br.com.mwconsulting.championcompanion.core.model.WeightMeasurement
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.decodeFromString
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

    override fun mapFrozenDailyD1Sample(operation: HealthSyncOperation): HealthRecordMappingResult = mapSafely {
        val aggregate = operation.key.aggregate as? HealthSyncAggregate.WatchDailyD1Sample
            ?: return@mapSafely invalidMappedRecord("Operação D1 sem agregado de amostra.")
        if (operation.action != HealthSyncOperationAction.INSERT) {
            return@mapSafely invalidMappedRecord("Operação D1 granular inválida.")
        }
        val clientRecordId = operation.clientRecordId
            ?: return@mapSafely invalidMappedRecord("Operação D1 sem identidade estável.")
        val clientRecordVersion = operation.clientRecordVersion
            ?: return@mapSafely invalidMappedRecord("Operação D1 sem versão estável.")
        when (operation.key.recordType) {
            HealthSyncRecordType.STEPS -> mapFrozenD1Steps(
                operation = operation,
                aggregate = aggregate,
                clientRecordId = clientRecordId,
                clientRecordVersion = clientRecordVersion,
            )
            HealthSyncRecordType.DISTANCE,
            HealthSyncRecordType.ACTIVE_CALORIES_BURNED,
            -> mapFrozenD1Metric(
                operation = operation,
                aggregate = aggregate,
                clientRecordId = clientRecordId,
                clientRecordVersion = clientRecordVersion,
            )
            else -> invalidMappedRecord("Tipo D1 granular não suportado.")
        }
    }

    private fun mapFrozenD1Steps(
        operation: HealthSyncOperation,
        aggregate: HealthSyncAggregate.WatchDailyD1Sample,
        clientRecordId: String,
        clientRecordVersion: Long,
    ): HealthRecordMappingResult {
        val payload = operation.frozenPayload
            ?.takeIf { it.schemaVersion == HEALTH_SYNC_FROZEN_DAILY_D1_PAYLOAD_SCHEMA_VERSION }
            ?.let { frozen ->
                runCatching {
                    frozenProjectionJson.decodeFromString<HealthSyncFrozenDailyD1Payload>(frozen.serializedPayload)
                }.getOrNull()
            }
            ?: return invalidMappedRecord("Operação D1 sem payload congelado válido.")
        if (payload.recordType != HealthSyncRecordType.STEPS.name ||
            payload.sampleId != aggregate.sampleId.value.toString() ||
            payload.zoneId != operation.timeZoneId ||
            payload.steps !in MIN_STEPS_RECORD_COUNT..MAX_STEPS_RECORD_COUNT
        ) {
            return invalidMappedRecord("Payload D1 não corresponde ao fato persistido.")
        }
        val window = payload.validatedWindowOrNull()
            ?: return invalidMappedRecord("Janela D1 não corresponde ao bucket físico de cinco minutos.")
        return HealthRecordMappingResult.Success(
            HealthConnectMappedRecord(
                recordType = HealthSyncRecordType.STEPS,
                permission = HealthConnectWritePermission.STEPS,
                record = StepsRecord(
                    window.start,
                    window.startOffset,
                    window.end,
                    window.endOffset,
                    payload.steps,
                    Metadata.autoRecorded(
                        device = watchDevice(),
                        clientRecordId = clientRecordId,
                        clientRecordVersion = clientRecordVersion,
                    ),
                ),
            ),
        )
    }

    private fun mapFrozenD1Metric(
        operation: HealthSyncOperation,
        aggregate: HealthSyncAggregate.WatchDailyD1Sample,
        clientRecordId: String,
        clientRecordVersion: Long,
    ): HealthRecordMappingResult {
        val payload = operation.frozenPayload
            ?.takeIf { it.schemaVersion == HEALTH_SYNC_FROZEN_DAILY_D1_METRIC_PAYLOAD_SCHEMA_VERSION }
            ?.let { frozen ->
                runCatching {
                    frozenProjectionJson.decodeFromString<HealthSyncFrozenDailyD1MetricPayload>(frozen.serializedPayload)
                }.getOrNull()
            }
            ?: return invalidMappedRecord("Operação D1 de métrica sem payload congelado válido.")
        if (payload.recordType != operation.key.recordType.name ||
            payload.sampleId != aggregate.sampleId.value.toString() ||
            payload.zoneId != operation.timeZoneId
        ) {
            return invalidMappedRecord("Payload D1 de métrica não corresponde ao fato persistido.")
        }
        val window = payload.validatedWindowOrNull()
            ?: return invalidMappedRecord("Janela da métrica D1 não corresponde ao bucket físico de cinco minutos.")
        val metadata = Metadata.autoRecorded(
            device = watchDevice(),
            clientRecordId = clientRecordId,
            clientRecordVersion = clientRecordVersion,
        )
        return when (operation.key.recordType) {
            HealthSyncRecordType.DISTANCE -> {
                val distance = payload.distanceMeters
                if (distance == null || !distance.isFinite() || distance <= 0.0 || distance > MAX_D1_DISTANCE_METERS) {
                    invalidMappedRecord("Distância D1 fora do intervalo aceito pelo Health Connect.")
                } else {
                    HealthRecordMappingResult.Success(
                        HealthConnectMappedRecord(
                            recordType = HealthSyncRecordType.DISTANCE,
                            permission = HealthConnectWritePermission.DISTANCE,
                            record = DistanceRecord(
                                window.start,
                                window.startOffset,
                                window.end,
                                window.endOffset,
                                Length.meters(distance),
                                metadata,
                            ),
                        ),
                    )
                }
            }
            HealthSyncRecordType.ACTIVE_CALORIES_BURNED -> {
                val calories = payload.activeCaloriesKilocalories
                if (calories == null || !calories.isFinite() || calories <= 0.0 ||
                    calories > MAX_D1_ACTIVE_CALORIES_KILOCALORIES
                ) {
                    invalidMappedRecord("Calorias ativas D1 fora do intervalo aceito pelo Health Connect.")
                } else {
                    HealthRecordMappingResult.Success(
                        HealthConnectMappedRecord(
                            recordType = HealthSyncRecordType.ACTIVE_CALORIES_BURNED,
                            permission = HealthConnectWritePermission.ACTIVE_CALORIES_BURNED,
                            record = ActiveCaloriesBurnedRecord(
                                window.start,
                                window.startOffset,
                                window.end,
                                window.endOffset,
                                Energy.kilocalories(calories),
                                metadata,
                            ),
                        ),
                    )
                }
            }
            else -> invalidMappedRecord("Tipo D1 de métrica não suportado.")
        }
    }

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

    private fun HealthSyncFrozenDailyD1Payload.validatedWindowOrNull(): FrozenD1Window? =
        validateD1Window(start, end, zoneId, startZoneOffsetSeconds, endZoneOffsetSeconds)

    private fun HealthSyncFrozenDailyD1MetricPayload.validatedWindowOrNull(): FrozenD1Window? =
        validateD1Window(start, end, zoneId, startZoneOffsetSeconds, endZoneOffsetSeconds)

    private fun validateD1Window(
        frozenStart: HealthSyncFrozenInstant,
        frozenEnd: HealthSyncFrozenInstant,
        zoneIdRaw: String,
        startZoneOffsetSeconds: Int,
        endZoneOffsetSeconds: Int,
    ): FrozenD1Window? {
        val start = frozenStart.toInstantOrNull() ?: return null
        val end = frozenEnd.toInstantOrNull() ?: return null
        if (!start.isBefore(end) || Duration.between(start, end) != D1_BUCKET_DURATION) return null
        val zoneId = runCatching { ZoneId.of(zoneIdRaw) }.getOrNull() ?: return null
        val startOffset = startZoneOffsetSeconds.toZoneOffsetOrNull() ?: return null
        val endOffset = endZoneOffsetSeconds.toZoneOffsetOrNull() ?: return null
        if (zoneId.rules.getOffset(start) != startOffset || zoneId.rules.getOffset(end) != endOffset) return null
        return FrozenD1Window(start, end, startOffset, endOffset)
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

    private fun invalidMappedRecord(
        reason: String = "Dados locais incompatíveis com o contrato do Health Connect.",
    ): HealthRecordMappingResult.Failure = HealthRecordMappingResult.Failure(
        error = SafeSyncError.INVALID_LOCAL_DATA,
        reason = reason,
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

    private fun HealthSyncFrozenInstant.toInstantOrNull(): Instant? = runCatching {
        Instant.ofEpochSecond(epochSecond, nano.toLong())
    }.getOrNull()

    private fun Int.toZoneOffsetOrNull(): ZoneOffset? = runCatching {
        ZoneOffset.ofTotalSeconds(this)
    }.getOrNull()

    private fun watchDevice(): HealthConnectDevice = HealthConnectDevice(
        HealthConnectDevice.TYPE_WATCH,
        "Champion",
        "Champion C001",
    )

    private data class HealthProjectionWindow(
        val start: Instant,
        val end: Instant,
    )

    private data class FrozenD1Window(
        val start: Instant,
        val end: Instant,
        val startOffset: ZoneOffset,
        val endOffset: ZoneOffset,
    )

    private companion object {
        val D1_BUCKET_DURATION: Duration = Duration.ofMinutes(5)
        const val MIN_STEPS_RECORD_COUNT = 1L
        const val MAX_STEPS_RECORD_COUNT = 1_000_000L
        const val MAX_D1_DISTANCE_METERS = 1_000_000.0
        const val MAX_D1_ACTIVE_CALORIES_KILOCALORIES = 1_000_000.0
        const val MIN_STEPS_CADENCE_RATE = 0.0
        const val MAX_STEPS_CADENCE_RATE = 10_000.0
    }
}
