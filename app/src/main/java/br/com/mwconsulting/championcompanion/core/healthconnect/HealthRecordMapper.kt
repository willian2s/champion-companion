package br.com.mwconsulting.championcompanion.core.healthconnect

import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device as HealthConnectDevice
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Velocity
import br.com.mwconsulting.championcompanion.core.model.ActivityWorkflowState
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.DomainValue
import br.com.mwconsulting.championcompanion.core.model.HEALTH_SYNC_FROZEN_PROJECTION_PAYLOAD_SCHEMA_VERSION
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionChildKey
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregate
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperation
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationAction
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationKey
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenPayload
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenHeartRateSample
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenInstant
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenProjectionPayload
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenSpeedSample
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.HeartRateSource
import br.com.mwconsulting.championcompanion.core.model.ParserValidity
import br.com.mwconsulting.championcompanion.core.model.SafeSyncError
import br.com.mwconsulting.championcompanion.core.model.UserProfile
import br.com.mwconsulting.championcompanion.core.model.WeightMeasurement
import br.com.mwconsulting.championcompanion.core.model.WeightUnit
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityOrigin
import br.com.mwconsulting.championcompanion.core.model.WorkoutHeartRateSample
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.model.valueOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

data class HealthConnectMappedRecord(
    val recordType: HealthSyncRecordType,
    val permission: HealthConnectWritePermission,
    val record: Record,
)

sealed interface HealthRecordMappingResult {
    data class Success(
        val value: HealthConnectMappedRecord,
    ) : HealthRecordMappingResult

    data class Failure(
        val error: SafeSyncError,
        val reason: String,
    ) : HealthRecordMappingResult
}

sealed interface HealthSyncFrozenPayloadResult {
    data class Success(
        val value: HealthSyncFrozenPayload,
    ) : HealthSyncFrozenPayloadResult

    data class Failure(
        val error: SafeSyncError,
        val reason: String,
    ) : HealthSyncFrozenPayloadResult
}

interface HealthRecordMapper {
    fun mapDailySummary(operation: HealthSyncOperation): HealthRecordMappingResult

    fun mapHeartRateMeasurement(operation: HealthSyncOperation, measurement: HeartRateMeasurement): HealthRecordMappingResult

    fun mapWeightMeasurement(operation: HealthSyncOperation, measurement: WeightMeasurement): HealthRecordMappingResult

    fun mapUserProfileHeight(operation: HealthSyncOperation, profile: UserProfile): HealthRecordMappingResult

    fun mapWorkoutActivity(operation: HealthSyncOperation, activity: WorkoutActivity): HealthRecordMappingResult

    fun mapFrozenWorkoutActivity(operation: HealthSyncOperation): HealthRecordMappingResult

    fun freezeWorkoutProjection(
        key: br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationKey,
        activity: WorkoutActivity,
    ): HealthSyncFrozenPayloadResult

    fun findEligibleWorkoutProjectionChildren(activity: WorkoutActivity): List<HealthProjectionChildKey>

    fun findEligibleWorkoutRecordTypes(activity: WorkoutActivity): Set<HealthSyncRecordType>
}

@Singleton
class DefaultHealthRecordMapper @Inject constructor() : HealthRecordMapper {
    private val frozenProjectionJson = Json {
        encodeDefaults = true
        explicitNulls = true
    }

    override fun mapDailySummary(
        operation: HealthSyncOperation,
    ): HealthRecordMappingResult {
        if (operation.key.recordType != HealthSyncRecordType.STEPS) {
            return invalid("Operação de passos esperada para resumo diário.")
        }
        val identity = operation.identityOrNull() ?: return invalid("Operação sem identidade estável para passos.")
        val payload = operation.dailyStepsPayload
            ?: return invalid("Operação de passos sem payload persistido.")
        val zoneId = payload.timeZoneId.toZoneIdOrNull()
            ?: return invalid("Operação de passos sem fuso válido persistido.")
        val record = StepsRecord(
            payload.startTime,
            zoneId.offsetAt(payload.startTime),
            payload.endTime,
            zoneId.offsetAt(payload.endTime),
            payload.stepDelta,
            Metadata.autoRecorded(
                device = watchDevice(),
                clientRecordId = identity.clientRecordId,
                clientRecordVersion = identity.clientRecordVersion,
            ),
        )
        return HealthRecordMappingResult.Success(
            HealthConnectMappedRecord(
                recordType = HealthSyncRecordType.STEPS,
                permission = HealthConnectWritePermission.STEPS,
                record = record,
            ),
        )
    }

    override fun mapHeartRateMeasurement(
        operation: HealthSyncOperation,
        measurement: HeartRateMeasurement,
    ): HealthRecordMappingResult {
        if (operation.key.recordType != HealthSyncRecordType.HEART_RATE) {
            return invalid("Operação de frequência cardíaca esperada.")
        }
        val measuredAt = measurement.measuredAt ?: return invalid("Medição sem instante persistido.")
        val identity = operation.identityOrNull() ?: return invalid("Operação sem identidade estável para BPM.")
        val zoneOffset = ZoneOffset.UTC
        val metadata = when (measurement.source) {
            HeartRateSource.MANUAL_MEASUREMENT -> Metadata.manualEntry(
                device = phoneDevice(),
                clientRecordId = identity.clientRecordId,
                clientRecordVersion = identity.clientRecordVersion,
            )

            HeartRateSource.WATCH_D1_HISTORY -> Metadata.autoRecorded(
                device = watchDevice(),
                clientRecordId = identity.clientRecordId,
                clientRecordVersion = identity.clientRecordVersion,
            )
            else -> return invalid("Fonte de BPM não elegível para Health Connect nesta fase.")
        }
        val record = HeartRateRecord(
            measuredAt,
            zoneOffset,
            measuredAt,
            zoneOffset,
            listOf(HeartRateRecord.Sample(measuredAt, measurement.bpm.toLong())),
            metadata,
        )
        return HealthRecordMappingResult.Success(
            HealthConnectMappedRecord(
                recordType = HealthSyncRecordType.HEART_RATE,
                permission = HealthConnectWritePermission.HEART_RATE,
                record = record,
            ),
        )
    }

    override fun mapWeightMeasurement(
        operation: HealthSyncOperation,
        measurement: WeightMeasurement,
    ): HealthRecordMappingResult {
        if (operation.key.recordType != HealthSyncRecordType.WEIGHT) {
            return invalid("Operação de peso esperada.")
        }
        val identity = operation.identityOrNull() ?: return invalid("Operação sem identidade estável para peso.")
        val kilograms = when (measurement.unit) {
            WeightUnit.KILOGRAMS -> measurement.value
            WeightUnit.POUNDS -> measurement.value * 0.45359237
        }
        if (!kilograms.isFinite() || kilograms <= 0.0) {
            return invalid("Peso sem valor positivo e finito.")
        }
        val zoneId = ZoneId.systemDefault()
        val instant = measurement.measuredAt
        val metadata = when (measurement.origin) {
            DataOrigin.MANUAL_ENTRY -> Metadata.manualEntry(
                device = scaleDevice(),
                clientRecordId = identity.clientRecordId,
                clientRecordVersion = identity.clientRecordVersion,
            )

            else -> Metadata.unknownRecordingMethod(
                device = scaleDevice(),
                clientRecordId = identity.clientRecordId,
                clientRecordVersion = identity.clientRecordVersion,
            )
        }
        val record = WeightRecord(
            instant,
            zoneId.offsetAt(instant),
            Mass.kilograms(kilograms),
            metadata,
        )
        return HealthRecordMappingResult.Success(
            HealthConnectMappedRecord(
                recordType = HealthSyncRecordType.WEIGHT,
                permission = HealthConnectWritePermission.WEIGHT,
                record = record,
            ),
        )
    }

    override fun mapUserProfileHeight(
        operation: HealthSyncOperation,
        profile: UserProfile,
    ): HealthRecordMappingResult {
        if (operation.key.recordType != HealthSyncRecordType.HEIGHT) {
            return invalid("Operação de altura esperada.")
        }
        val identity = operation.identityOrNull() ?: return invalid("Operação sem identidade estável para altura.")
        val centimeters = profile.heightCentimeters
        if (centimeters == null || !centimeters.isFinite() || centimeters <= 0.0) {
            return invalid("Perfil sem altura elegível.")
        }
        val instant = profile.updatedAt
        val zoneId = ZoneId.systemDefault()
        val record = HeightRecord(
            instant,
            zoneId.offsetAt(instant),
            Length.meters(centimeters / 100.0),
            Metadata.manualEntry(
                device = phoneDevice(),
                clientRecordId = identity.clientRecordId,
                clientRecordVersion = identity.clientRecordVersion,
            ),
        )
        return HealthRecordMappingResult.Success(
            HealthConnectMappedRecord(
                recordType = HealthSyncRecordType.HEIGHT,
                permission = HealthConnectWritePermission.HEIGHT,
                record = record,
            ),
        )
    }

    override fun mapWorkoutActivity(
        operation: HealthSyncOperation,
        activity: WorkoutActivity,
    ): HealthRecordMappingResult {
        if (operation.key.projectionChildKey != null) {
            return mapFrozenWorkoutActivity(operation)
        }
        if (activity.activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4) {
            return invalid("Atividade do relógio exige projeção filha congelada.")
        }
        val identity = operation.identityOrNull() ?: return invalid("Operação sem identidade estável para atividade.")
        val sessionWindow = activity.sessionWindowOrNull() ?: return invalid("Atividade sem janela temporal válida.")
        val zoneId = operation.timeZoneId.toZoneIdOrSystemDefault()
        return when (operation.key.recordType) {
            HealthSyncRecordType.EXERCISE_SESSION -> mapWorkoutSession(identity, activity, sessionWindow.start, sessionWindow.end, zoneId)
            HealthSyncRecordType.DISTANCE -> mapWorkoutDistance(identity, activity, sessionWindow.start, sessionWindow.end, zoneId)
            HealthSyncRecordType.TOTAL_CALORIES_BURNED -> mapWorkoutCalories(identity, activity, sessionWindow.start, sessionWindow.end, zoneId)
            HealthSyncRecordType.HEART_RATE -> mapWorkoutHeartRate(identity, activity, sessionWindow.start, sessionWindow.end, zoneId)
            else -> invalid("Tipo de record não suportado para atividade.")
        }
    }

    override fun mapFrozenWorkoutActivity(
        operation: HealthSyncOperation,
    ): HealthRecordMappingResult {
        if (operation.key.aggregate !is HealthSyncAggregate.WorkoutActivity) {
            return invalid("Operação filha sem agregado de atividade.")
        }
        if (operation.action != HealthSyncOperationAction.INSERT) {
            return invalid("Ação de projeção não suportada nesta etapa.")
        }
        val child = operation.key.projectionChildKey
            ?: return invalid("Operação filha sem chave estável.")
        val identity = operation.identityOrNull()
            ?: return invalid("Operação sem identidade estável para atividade.")
        val payload = operation.frozenPayload?.let(::decodeFrozenProjection)
            ?: return invalid("Operação filha sem payload congelado válido.")
        if (payload.recordType != operation.key.recordType.name ||
            payload.projectionChildKey != child.stableValue
        ) {
            return invalid("Payload congelado não corresponde ao filho persistido.")
        }
        val start = payload.start.toInstantOrNull()
            ?: return invalid("Payload congelado sem início temporal válido.")
        val end = payload.end.toInstantOrNull()
            ?: return invalid("Payload congelado sem fim temporal válido.")
        val startOffset = payload.startZoneOffsetSeconds.toZoneOffsetOrNull()
            ?: return invalid("Payload congelado sem offset inicial válido.")
        val endOffset = payload.endZoneOffsetSeconds.toZoneOffsetOrNull()
            ?: return invalid("Payload congelado sem offset final válido.")
        val metadata = Metadata.autoRecorded(
            device = watchDevice(),
            clientRecordId = identity.clientRecordId,
            clientRecordVersion = identity.clientRecordVersion,
        )

        return when (child) {
            HealthProjectionChildKey.ExerciseSession -> {
                if (operation.key.recordType != HealthSyncRecordType.EXERCISE_SESSION ||
                    payload.exerciseType == null || !start.isBefore(end)
                ) {
                    invalid("Payload de sessão inválido.")
                } else {
                    HealthRecordMappingResult.Success(
                        HealthConnectMappedRecord(
                            recordType = HealthSyncRecordType.EXERCISE_SESSION,
                            permission = HealthConnectWritePermission.EXERCISE,
                            record = ExerciseSessionRecord(
                                start,
                                startOffset,
                                end,
                                endOffset,
                                metadata,
                                payload.exerciseType,
                                payload.title,
                                payload.notes,
                            ),
                        ),
                    )
                }
            }

            is HealthProjectionChildKey.DistanceInterval -> {
                val distance = payload.distanceMeters
                if (operation.key.recordType != HealthSyncRecordType.DISTANCE ||
                    !start.isBefore(end) || distance == null || !distance.isFinite() ||
                    distance <= 0.0 || distance > MAX_HEALTH_CONNECT_DISTANCE_METERS
                ) {
                    invalid("Payload de distância granular inválido.")
                } else {
                    HealthRecordMappingResult.Success(
                        HealthConnectMappedRecord(
                            recordType = HealthSyncRecordType.DISTANCE,
                            permission = HealthConnectWritePermission.DISTANCE,
                            record = DistanceRecord(
                                start,
                                startOffset,
                                end,
                                endOffset,
                                Length.meters(distance),
                                metadata,
                            ),
                        ),
                    )
                }
            }

            HealthProjectionChildKey.SpeedSeries -> {
                if (operation.key.recordType != HealthSyncRecordType.SPEED ||
                    !start.isBefore(end) || payload.speedSamples.isEmpty() ||
                    payload.speedSamples.any { it.speedMetersPerSecond > MAX_HEALTH_CONNECT_SPEED_METERS_PER_SECOND }
                ) {
                    invalid("Payload de velocidade inválido.")
                } else {
                    val samples = payload.speedSamples.mapNotNull { sample ->
                        val sampledAt = sample.sampledAt.toInstantOrNull() ?: return@mapNotNull null
                        SpeedRecord.Sample(
                            time = sampledAt,
                            speed = Velocity.metersPerSecond(sample.speedMetersPerSecond),
                        )
                    }
                    if (samples.size != payload.speedSamples.size ||
                        samples.zipWithNext().any { (first, second) -> !first.time.isBefore(second.time) } ||
                        samples.any { it.time.isBefore(start) || !it.time.isBefore(end) } ||
                        samples.any { it.speed.inMetersPerSecond > MAX_HEALTH_CONNECT_SPEED_METERS_PER_SECOND }
                    ) {
                        invalid("Amostras de velocidade fora da janela congelada.")
                    } else {
                        HealthRecordMappingResult.Success(
                            HealthConnectMappedRecord(
                                recordType = HealthSyncRecordType.SPEED,
                                permission = HealthConnectWritePermission.SPEED,
                                record = SpeedRecord(
                                    start,
                                    startOffset,
                                    end,
                                    endOffset,
                                    samples,
                                    metadata,
                                ),
                            ),
                        )
                    }
                }
            }

            HealthProjectionChildKey.HeartRateSeries -> {
                if (operation.key.recordType != HealthSyncRecordType.HEART_RATE ||
                    payload.heartRateSamples.isEmpty() || start.isAfter(end)
                ) {
                    invalid("Payload de BPM granular inválido.")
                } else {
                    val samples = payload.heartRateSamples.mapNotNull { sample ->
                        val sampledAt = sample.sampledAt.toInstantOrNull() ?: return@mapNotNull null
                        HeartRateRecord.Sample(sampledAt, sample.bpm.toLong())
                    }
                    if (samples.size != payload.heartRateSamples.size ||
                        samples.zipWithNext().any { (first, second) -> !first.time.isBefore(second.time) } ||
                        samples.any { it.time.isBefore(start) || it.time.isAfter(end) }
                    ) {
                        invalid("Amostras de BPM fora da janela congelada.")
                    } else {
                        HealthRecordMappingResult.Success(
                            HealthConnectMappedRecord(
                                recordType = HealthSyncRecordType.HEART_RATE,
                                permission = HealthConnectWritePermission.HEART_RATE,
                                record = HeartRateRecord(
                                    start,
                                    startOffset,
                                    end,
                                    endOffset,
                                    samples,
                                    metadata,
                                ),
                            ),
                        )
                    }
                }
            }

            HealthProjectionChildKey.TotalCaloriesBurned -> {
                val calories = payload.caloriesKilocalories
                if (operation.key.recordType != HealthSyncRecordType.TOTAL_CALORIES_BURNED ||
                    !start.isBefore(end) || calories == null || !calories.isFinite() ||
                    calories <= 0.0 || calories > MAX_HEALTH_CONNECT_CALORIES_KILOCALORIES
                ) {
                    invalid("Payload de calorias inválido.")
                } else {
                    HealthRecordMappingResult.Success(
                        HealthConnectMappedRecord(
                            recordType = HealthSyncRecordType.TOTAL_CALORIES_BURNED,
                            permission = HealthConnectWritePermission.TOTAL_CALORIES_BURNED,
                            record = TotalCaloriesBurnedRecord(
                                start,
                                startOffset,
                                end,
                                endOffset,
                                Energy.kilocalories(calories),
                                metadata,
                            ),
                        ),
                    )
                }
            }
        }
    }

    override fun freezeWorkoutProjection(
        key: HealthSyncOperationKey,
        activity: WorkoutActivity,
    ): HealthSyncFrozenPayloadResult {
        if (key.aggregate != HealthSyncAggregate.WorkoutActivity(activity.id) ||
            activity.activityOrigin != WorkoutActivityOrigin.WATCH_D3_D4
        ) {
            return frozenPayloadInvalid("Atividade não elegível para projeção granular.")
        }
        val child = key.projectionChildKey
            ?: return frozenPayloadInvalid("Projeção sem filho estável.")
        val payload = createFrozenProjectionPayload(key, child, activity)
            ?: return frozenPayloadInvalid("Não foi possível congelar payload da projeção.")
        return runCatching {
            HealthSyncFrozenPayload(
                schemaVersion = HEALTH_SYNC_FROZEN_PROJECTION_PAYLOAD_SCHEMA_VERSION,
                serializedPayload = frozenProjectionJson.encodeToString(payload),
            )
        }.fold(
            onSuccess = HealthSyncFrozenPayloadResult::Success,
            onFailure = { frozenPayloadInvalid("Payload de projeção inválido.") },
        )
    }

    override fun findEligibleWorkoutProjectionChildren(
        activity: WorkoutActivity,
    ): List<HealthProjectionChildKey> {
        if (activity.activityOrigin != WorkoutActivityOrigin.WATCH_D3_D4 ||
            activity.workflowState !in setOf(
                ActivityWorkflowState.READY_TO_SYNC,
                ActivityWorkflowState.SYNC_FAILED,
                ActivityWorkflowState.SYNCING,
            ) || activity.confirmedType == null || activity.confirmedType == WorkoutType.UNKNOWN ||
            activity.sessionWindowOrNull() == null
        ) {
            return emptyList()
        }
        val children = mutableListOf<HealthProjectionChildKey>(HealthProjectionChildKey.ExerciseSession)
        val snapshot = activity.derivationSnapshot
        if (snapshot?.isEligibleForHealthProjection == true) {
            val series = snapshot.canonicalSeries
            if (series != null) {
                series.intervals
                    .filter { interval ->
                        interval.canonicalIntervalDistanceMeters.isFinite() &&
                            interval.canonicalIntervalDistanceMeters > 0.0
                    }
                    .forEach { interval ->
                        HealthProjectionChildKey.DistanceInterval.create(interval.index)
                            .valueOrNull()
                            ?.let(children::add)
                    }
                if (series.speedSamples.isNotEmpty()) {
                    children += HealthProjectionChildKey.SpeedSeries
                }
            }
        }
        if (activity.samplesInsideSession().isNotEmpty()) {
            children += HealthProjectionChildKey.HeartRateSeries
        }
        if (activity.positiveCaloriesKilocalories() != null) {
            children += HealthProjectionChildKey.TotalCaloriesBurned
        }
        return children
    }

    override fun findEligibleWorkoutRecordTypes(activity: WorkoutActivity): Set<HealthSyncRecordType> {
        if (activity.activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4) {
            return findEligibleWorkoutProjectionChildren(activity)
                .mapTo(linkedSetOf()) { child ->
                    HealthSyncRecordType.entries.first { it.name == child.recordTypeName }
                }
        }
        val sessionWindow = activity.sessionWindowOrNull() ?: return emptySet()
        if (activity.workflowState !in setOf(ActivityWorkflowState.READY_TO_SYNC, ActivityWorkflowState.SYNC_FAILED)) {
            return emptySet()
        }
        if (activity.confirmedType == null || activity.confirmedType == WorkoutType.UNKNOWN) {
            return emptySet()
        }
        val eligible = linkedSetOf(HealthSyncRecordType.EXERCISE_SESSION)
        if (activity.finalDistanceMeters() != null) eligible += HealthSyncRecordType.DISTANCE
        if (activity.positiveCaloriesKilocalories() != null) eligible += HealthSyncRecordType.TOTAL_CALORIES_BURNED
        if (activity.samplesInside(sessionWindow.start, sessionWindow.end).isNotEmpty()) eligible += HealthSyncRecordType.HEART_RATE
        return eligible
    }

    private fun createFrozenProjectionPayload(
        key: HealthSyncOperationKey,
        child: HealthProjectionChildKey,
        activity: WorkoutActivity,
    ): HealthSyncFrozenProjectionPayload? {
        if (child.recordTypeName != key.recordType.name) return null
        val session = activity.sessionWindowOrNull() ?: return null
        val zoneId = ZoneId.systemDefault()
        return when (child) {
            HealthProjectionChildKey.ExerciseSession -> {
                val exerciseType = activity.confirmedType
                    ?.takeUnless { it == WorkoutType.UNKNOWN }
                    ?.toHealthConnectExerciseType()
                    ?: return null
                frozenProjectionPayload(
                    key = key,
                    child = child,
                    start = session.start,
                    end = session.end,
                    zoneId = zoneId,
                ).copy(
                    exerciseType = exerciseType,
                    title = activity.title,
                    notes = activity.notes,
                )
            }

            is HealthProjectionChildKey.DistanceInterval -> {
                val interval = activity.canonicalSeries?.intervals
                    ?.firstOrNull { it.index == child.intervalIndex }
                    ?: return null
                val distance = interval.canonicalIntervalDistanceMeters
                if (!distance.isFinite() || distance <= 0.0 || distance > MAX_HEALTH_CONNECT_DISTANCE_METERS) {
                    return null
                }
                frozenProjectionPayload(
                    key = key,
                    child = child,
                    start = interval.effectiveWindow.start,
                    end = interval.effectiveWindow.end,
                    zoneId = zoneId,
                ).copy(distanceMeters = distance)
            }

            HealthProjectionChildKey.SpeedSeries -> {
                val snapshot = activity.derivationSnapshot
                    ?.takeIf { it.isEligibleForHealthProjection }
                    ?: return null
                val samples = snapshot.canonicalSeries?.speedSamples?.map { sample ->
                    if (!sample.speedMetersPerSecond.isFinite() ||
                        sample.speedMetersPerSecond < 0.0 ||
                        sample.speedMetersPerSecond > MAX_HEALTH_CONNECT_SPEED_METERS_PER_SECOND
                    ) {
                        return null
                    }
                    HealthSyncFrozenSpeedSample(
                        sampledAt = sample.sampledAt.toFrozenInstant(),
                        speedMetersPerSecond = sample.speedMetersPerSecond,
                    )
                } ?: return null
                if (samples.isEmpty()) return null
                frozenProjectionPayload(
                    key = key,
                    child = child,
                    start = session.start,
                    end = session.end,
                    zoneId = zoneId,
                ).copy(speedSamples = samples)
            }

            HealthProjectionChildKey.HeartRateSeries -> {
                val samples = activity.samplesInside(session.start, session.end)
                if (samples.isEmpty()) return null
                frozenProjectionPayload(
                    key = key,
                    child = child,
                    start = samples.first().sampledAt,
                    end = samples.last().sampledAt,
                    zoneId = zoneId,
                ).copy(
                    heartRateSamples = samples.map { sample ->
                        HealthSyncFrozenHeartRateSample(
                            sampledAt = sample.sampledAt.toFrozenInstant(),
                            bpm = sample.bpm,
                        )
                    },
                )
            }

            HealthProjectionChildKey.TotalCaloriesBurned -> {
                val calories = activity.positiveCaloriesKilocalories()
                    ?.takeIf { it <= MAX_HEALTH_CONNECT_CALORIES_KILOCALORIES }
                    ?: return null
                frozenProjectionPayload(
                    key = key,
                    child = child,
                    start = session.start,
                    end = session.end,
                    zoneId = zoneId,
                ).copy(caloriesKilocalories = calories)
            }
        }
    }

    private fun frozenProjectionPayload(
        key: HealthSyncOperationKey,
        child: HealthProjectionChildKey,
        start: Instant,
        end: Instant,
        zoneId: ZoneId,
    ): HealthSyncFrozenProjectionPayload = HealthSyncFrozenProjectionPayload(
        recordType = key.recordType.name,
        projectionChildKey = child.stableValue,
        start = start.toFrozenInstant(),
        end = end.toFrozenInstant(),
        startZoneOffsetSeconds = zoneId.offsetAt(start).totalSeconds,
        endZoneOffsetSeconds = zoneId.offsetAt(end).totalSeconds,
    )

    private fun decodeFrozenProjection(
        payload: HealthSyncFrozenPayload,
    ): HealthSyncFrozenProjectionPayload? = if (
        payload.schemaVersion == HEALTH_SYNC_FROZEN_PROJECTION_PAYLOAD_SCHEMA_VERSION
    ) {
        runCatching {
            frozenProjectionJson.decodeFromString<HealthSyncFrozenProjectionPayload>(payload.serializedPayload)
        }.getOrNull()
    } else {
        null
    }

    private fun WorkoutActivity.samplesInsideSession(): List<WorkoutHeartRateSample> =
        sessionWindowOrNull()?.let { session -> samplesInside(session.start, session.end) }.orEmpty()

    private fun frozenPayloadInvalid(reason: String): HealthSyncFrozenPayloadResult.Failure =
        HealthSyncFrozenPayloadResult.Failure(SafeSyncError.INVALID_LOCAL_DATA, reason)

    private companion object {
        const val MAX_HEALTH_CONNECT_DISTANCE_METERS = 1_000_000.0
        const val MAX_HEALTH_CONNECT_SPEED_METERS_PER_SECOND = 1_000_000.0
        const val MAX_HEALTH_CONNECT_CALORIES_KILOCALORIES = 1_000_000.0
    }

    private fun mapWorkoutSession(
        identity: RecordIdentity,
        activity: WorkoutActivity,
        start: Instant,
        end: Instant,
        zoneId: ZoneId,
    ): HealthRecordMappingResult {
        if (activity.workflowState !in setOf(ActivityWorkflowState.READY_TO_SYNC, ActivityWorkflowState.SYNC_FAILED, ActivityWorkflowState.SYNCING)) {
            return invalid("Atividade fora de estado elegível para sessão.")
        }
        val confirmedType = activity.confirmedType ?: return invalid("Atividade sem tipo confirmado.")
        val record = ExerciseSessionRecord(
            start,
            zoneId.offsetAt(start),
            end,
            zoneId.offsetAt(end),
            Metadata.autoRecorded(
                device = watchDevice(),
                clientRecordId = identity.clientRecordId,
                clientRecordVersion = identity.clientRecordVersion,
            ),
            confirmedType.toHealthConnectExerciseType(),
            activity.title,
            activity.notes,
        )
        return HealthRecordMappingResult.Success(
            HealthConnectMappedRecord(
                recordType = HealthSyncRecordType.EXERCISE_SESSION,
                permission = HealthConnectWritePermission.EXERCISE,
                record = record,
            ),
        )
    }

    private fun mapWorkoutDistance(
        identity: RecordIdentity,
        activity: WorkoutActivity,
        start: Instant,
        end: Instant,
        zoneId: ZoneId,
    ): HealthRecordMappingResult {
        val distanceMeters = activity.finalDistanceMeters() ?: return invalid("Atividade sem distância elegível.")
        val record = DistanceRecord(
            start,
            zoneId.offsetAt(start),
            end,
            zoneId.offsetAt(end),
            Length.meters(distanceMeters),
            Metadata.autoRecorded(
                device = watchDevice(),
                clientRecordId = identity.clientRecordId,
                clientRecordVersion = identity.clientRecordVersion,
            ),
        )
        return HealthRecordMappingResult.Success(
            HealthConnectMappedRecord(
                recordType = HealthSyncRecordType.DISTANCE,
                permission = HealthConnectWritePermission.DISTANCE,
                record = record,
            ),
        )
    }

    private fun mapWorkoutCalories(
        identity: RecordIdentity,
        activity: WorkoutActivity,
        start: Instant,
        end: Instant,
        zoneId: ZoneId,
    ): HealthRecordMappingResult {
        val calories = activity.positiveCaloriesKilocalories() ?: return invalid("Atividade sem calorias elegíveis.")
        val record = TotalCaloriesBurnedRecord(
            start,
            zoneId.offsetAt(start),
            end,
            zoneId.offsetAt(end),
            Energy.kilocalories(calories),
            Metadata.autoRecorded(
                device = watchDevice(),
                clientRecordId = identity.clientRecordId,
                clientRecordVersion = identity.clientRecordVersion,
            ),
        )
        return HealthRecordMappingResult.Success(
            HealthConnectMappedRecord(
                recordType = HealthSyncRecordType.TOTAL_CALORIES_BURNED,
                permission = HealthConnectWritePermission.TOTAL_CALORIES_BURNED,
                record = record,
            ),
        )
    }

    private fun mapWorkoutHeartRate(
        identity: RecordIdentity,
        activity: WorkoutActivity,
        start: Instant,
        end: Instant,
        zoneId: ZoneId,
    ): HealthRecordMappingResult {
        val samples = activity.samplesInside(start, end)
        if (samples.isEmpty()) {
            return invalid("Atividade sem amostras reais de BPM elegíveis.")
        }
        val first = samples.first().sampledAt
        val last = samples.last().sampledAt
        val record = HeartRateRecord(
            first,
            zoneId.offsetAt(first),
            last,
            zoneId.offsetAt(last),
            samples.map { HeartRateRecord.Sample(it.sampledAt, it.bpm.toLong()) },
            Metadata.autoRecorded(
                device = watchDevice(),
                clientRecordId = identity.clientRecordId,
                clientRecordVersion = identity.clientRecordVersion,
            ),
        )
        return HealthRecordMappingResult.Success(
            HealthConnectMappedRecord(
                recordType = HealthSyncRecordType.HEART_RATE,
                permission = HealthConnectWritePermission.HEART_RATE,
                record = record,
            ),
        )
    }

    private fun WorkoutActivity.sessionWindowOrNull(): SessionWindow? {
        if (parserValidity == ParserValidity.INVALID) return null
        val duration = metrics.duration
        if (duration.isZero || duration.isNegative) return null
        val start = metrics.startedAt
        val end = start.plus(duration)
        if (!end.isAfter(start)) return null
        return SessionWindow(start = start, end = end)
    }

    private fun WorkoutActivity.finalDistanceMeters(): Double? = listOf(
        metrics.distances.calibratedMeters.presentOrNull(),
        metrics.distances.originalMeters.presentOrNull(),
        metrics.distances.estimatedMeters.presentOrNull(),
    ).firstOrNull { it != null && it.isFinite() && it > 0.0 }

    private fun WorkoutActivity.positiveCaloriesKilocalories(): Double? =
        metrics.caloriesKilocalories.presentOrNull()?.takeIf { it.isFinite() && it > 0.0 }

    private fun WorkoutActivity.samplesInside(
        start: Instant,
        end: Instant,
    ): List<WorkoutHeartRateSample> = heartRateSamples.filter { sample ->
        !sample.sampledAt.isBefore(start) && !sample.sampledAt.isAfter(end)
    }

    private fun WorkoutType.toHealthConnectExerciseType(): Int = when (this) {
        WorkoutType.WALKING -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
        WorkoutType.RUNNING -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
        WorkoutType.TREADMILL -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL
        WorkoutType.FREE_WORKOUT -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        WorkoutType.UNKNOWN -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
    }

    private fun invalid(reason: String): HealthRecordMappingResult.Failure =
        HealthRecordMappingResult.Failure(SafeSyncError.INVALID_LOCAL_DATA, reason)

    private fun watchDevice(): HealthConnectDevice = HealthConnectDevice(
        HealthConnectDevice.TYPE_WATCH,
        "Champion",
        "Champion C001",
    )

    private fun phoneDevice(): HealthConnectDevice = HealthConnectDevice(
        HealthConnectDevice.TYPE_PHONE,
        "Champion Companion",
        "Local Profile",
    )

    private fun scaleDevice(): HealthConnectDevice = HealthConnectDevice(
        HealthConnectDevice.TYPE_SCALE,
        "Champion Companion",
        "Local Profile",
    )
}

private data class RecordIdentity(
    val clientRecordId: String,
    val clientRecordVersion: Long,
)

private data class SessionWindow(
    val start: Instant,
    val end: Instant,
)

private fun HealthSyncOperation.identityOrNull(): RecordIdentity? {
    val clientRecordId = clientRecordId?.takeIf(String::isNotBlank) ?: return null
    val clientRecordVersion = clientRecordVersion ?: return null
    return RecordIdentity(clientRecordId, clientRecordVersion)
}

private fun String?.toZoneIdOrSystemDefault(): ZoneId =
    runCatching { if (this == null) ZoneId.systemDefault() else ZoneId.of(this) }.getOrDefault(ZoneId.systemDefault())

private fun String.toZoneIdOrNull(): ZoneId? = runCatching { ZoneId.of(this) }.getOrNull()

private fun ZoneId.offsetAt(instant: Instant): ZoneOffset = rules.getOffset(instant)

private fun DomainValue<Double>.presentOrNull(): Double? = (this as? DomainValue.Present)?.value

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
