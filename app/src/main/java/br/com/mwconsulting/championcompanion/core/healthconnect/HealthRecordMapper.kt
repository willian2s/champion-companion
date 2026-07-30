package br.com.mwconsulting.championcompanion.core.healthconnect

import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device as HealthConnectDevice
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import br.com.mwconsulting.championcompanion.core.model.ActivityWorkflowState
import br.com.mwconsulting.championcompanion.core.model.DailySummary
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.DomainValue
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperation
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.HeartRateSource
import br.com.mwconsulting.championcompanion.core.model.ParserValidity
import br.com.mwconsulting.championcompanion.core.model.SafeSyncError
import br.com.mwconsulting.championcompanion.core.model.UserProfile
import br.com.mwconsulting.championcompanion.core.model.WeightMeasurement
import br.com.mwconsulting.championcompanion.core.model.WeightUnit
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import br.com.mwconsulting.championcompanion.core.model.WorkoutHeartRateSample
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

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

interface HealthRecordMapper {
    fun mapDailySummary(operation: HealthSyncOperation, summary: DailySummary): HealthRecordMappingResult

    fun mapHeartRateMeasurement(operation: HealthSyncOperation, measurement: HeartRateMeasurement): HealthRecordMappingResult

    fun mapWeightMeasurement(operation: HealthSyncOperation, measurement: WeightMeasurement): HealthRecordMappingResult

    fun mapUserProfileHeight(operation: HealthSyncOperation, profile: UserProfile): HealthRecordMappingResult

    fun mapWorkoutActivity(operation: HealthSyncOperation, activity: WorkoutActivity): HealthRecordMappingResult

    fun findEligibleWorkoutRecordTypes(activity: WorkoutActivity): Set<HealthSyncRecordType>
}

@Singleton
class DefaultHealthRecordMapper @Inject constructor() : HealthRecordMapper {
    override fun mapDailySummary(
        operation: HealthSyncOperation,
        summary: DailySummary,
    ): HealthRecordMappingResult {
        if (operation.key.recordType != HealthSyncRecordType.STEPS) {
            return invalid("Operação de passos esperada para resumo diário.")
        }
        val identity = operation.identityOrNull() ?: return invalid("Operação sem identidade estável para passos.")
        if (summary.key.origin != DataOrigin.WATCH_DAILY_SUMMARY || summary.steps < 0) {
            return invalid("Resumo diário incompatível com `StepsRecord`.")
        }
        val zoneId = operation.timeZoneId.toZoneIdOrSystemDefault()
        val start = summary.key.localDate.atStartOfDay(zoneId).toInstant()
        val end = resolveStepsEnd(summary.key.localDate, summary.collectedAt, zoneId)
        if (end.isBefore(start)) {
            return invalid("Intervalo temporal inválido para `StepsRecord`.")
        }
        val record = StepsRecord(
            start,
            zoneId.offsetAt(start),
            end,
            zoneId.offsetAt(end),
            summary.steps,
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

    override fun findEligibleWorkoutRecordTypes(activity: WorkoutActivity): Set<HealthSyncRecordType> {
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

    private fun resolveStepsEnd(
        localDate: LocalDate,
        collectedAt: Instant,
        zoneId: ZoneId,
    ): Instant {
        val collectedDate = collectedAt.atZone(zoneId).toLocalDate()
        return if (localDate == collectedDate) {
            collectedAt
        } else {
            localDate.plusDays(1).atStartOfDay(zoneId).toInstant()
        }
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

private fun ZoneId.offsetAt(instant: Instant): ZoneOffset = rules.getOffset(instant)

private fun DomainValue<Double>.presentOrNull(): Double? = (this as? DomainValue.Present)?.value
