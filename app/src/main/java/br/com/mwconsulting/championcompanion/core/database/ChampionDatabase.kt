package br.com.mwconsulting.championcompanion.core.database

import androidx.room3.Database
import androidx.room3.Dao
import androidx.room3.ColumnTypeConverters
import androidx.room3.ColumnTypeConverter
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.AutoMigration
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase

enum class StoredDeviceModel {
    CHAMPION_C001,
}

@Entity(
    tableName = "devices",
    indices = [Index(value = ["platformIdentifier"], unique = true)],
)
data class DeviceEntity(
    @PrimaryKey val id: String,
    val platformIdentifier: String,
    val name: String,
    val model: StoredDeviceModel,
    val firmware: String?,
    val associatedAtEpochMillis: Long,
    val lastSuccessfulConnectionAtEpochMillis: Long?,
    val lastImportedAtEpochMillis: Long?,
    val importParserVersion: String?,
)

@Entity(
    tableName = "device_import_metadata",
    foreignKeys = [ForeignKey(
        entity = DeviceEntity::class,
        parentColumns = ["id"],
        childColumns = ["deviceId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["deviceId", "importType"], unique = true)],
)
data class DeviceImportMetadataEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val importType: String,
    val lastAttemptAtEpochMillis: Long,
    val lastSuccessAtEpochMillis: Long?,
    val lastResult: String,
    val parserVersion: String?,
    val insertedCount: Int,
    val duplicateCount: Int,
    val conflictCount: Int,
    val invalidCount: Int,
    val missingIndexCount: Int,
    val timedOut: Boolean,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "device_associations",
    foreignKeys = [ForeignKey(
        entity = DeviceEntity::class,
        parentColumns = ["id"],
        childColumns = ["deviceId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index(value = ["deviceId"], unique = true)],
)
data class DeviceAssociationEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val associatedAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val heightCentimeters: Double?,
    val stepLengthMeters: Double? = null,
    val stepLengthSource: String? = null,
    val stepLengthUpdatedAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "weight_measurements",
    foreignKeys = [ForeignKey(
        entity = UserProfileEntity::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["profileId"]), Index(value = ["measuredAtEpochMillis"])],
)
data class WeightMeasurementEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val value: Double,
    val unit: String,
    val measuredAtEpochMillis: Long,
    val origin: String,
)

@Entity(
    tableName = "step_length_calibrations",
    foreignKeys = [ForeignKey(
        entity = UserProfileEntity::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [
        Index(value = ["activityId"], unique = true),
        Index(value = ["profileId", "workoutType"]),
    ],
)
data class StepLengthCalibrationEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val activityId: String,
    val workoutType: String,
    val referenceDistanceMeters: Double,
    val decodedSteps: Long,
    val acceptedAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "daily_summaries",
    foreignKeys = [ForeignKey(
        entity = DeviceEntity::class,
        parentColumns = ["id"],
        childColumns = ["deviceId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["deviceId", "origin", "localDateIso"], unique = true),
    ],
)
data class DailySummaryEntity(
    @PrimaryKey val id: String,
    val deviceId: String?,
    val origin: String,
    val localDateIso: String,
    val steps: Long,
    val estimatedDistanceMeters: Double?,
    val estimatedCaloriesKilocalories: Double?,
    val collectedAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val validity: String,
    val collectedZoneId: String? = null,
)

@Entity(
    tableName = "workout_activities",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = DiagnosticPayloadEntity::class,
            parentColumns = ["id"],
            childColumns = ["diagnosticPayloadId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["diagnosticPayloadId"]),
        Index(value = ["workflowState"]),
        Index(value = ["appWorkoutSessionId"], unique = true),
    ],
)
data class WorkoutActivityEntity(
    @PrimaryKey val id: String,
    val origin: String,
    val deviceId: String?,
    /** Snapshots legados v5; a origem autoritativa desses campos é watch_workout_imports. */
    val parserValidity: String?,
    val importedAtEpochMillis: Long?,
    val parserVersion: String?,
    val diagnosticPayloadId: String?,
    val startedAtEpochMillis: Long,
    val durationNanos: Long,
    val steps: Long,
    val caloriesKilocalories: Double?,
    val originalDistanceMeters: Double?,
    val estimatedDistanceMeters: Double?,
    val calibratedDistanceMeters: Double?,
    val importedAveragePaceSecondsPerKilometer: Double?,
    val importedAverageSpeedMetersPerSecond: Double?,
    val calibratedAveragePaceSecondsPerKilometer: Double?,
    val calibratedAverageSpeedMetersPerSecond: Double?,
    val averageBpm: Int?,
    val minimumBpm: Int?,
    val maximumBpm: Int?,
    val suggestedType: String,
    val title: String?,
    val notes: String?,
    val confirmedType: String?,
    val workflowState: String,
    val reviewedAtEpochMillis: Long?,
    val calibrationId: String?,
    val appWorkoutSessionId: String? = null,
    val recordedAtEpochMillis: Long = importedAtEpochMillis ?: startedAtEpochMillis,
    val endedAtEpochMillis: Long = startedAtEpochMillis + durationNanos / 1_000_000L,
    val stepsAvailable: Boolean = true,
    /** Entrada manual opcional de distância de bicicleta, em metros. */
    val manualDistanceMeters: Double? = null,
)

@Entity(
    tableName = "diagnostic_payloads",
    foreignKeys = [ForeignKey(
        entity = DeviceEntity::class,
        parentColumns = ["id"],
        childColumns = ["deviceId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index(value = ["deviceId"]), Index(value = ["capturedAtEpochMillis"])],
)
data class DiagnosticPayloadEntity(
    @PrimaryKey val id: String,
    val command: String,
    val flow: String,
    val privateBytes: ByteArray,
    val byteCount: Long,
    val deviceId: String?,
    val firmware: String?,
    val parserVersion: String,
    val result: String,
    val safeError: String?,
    val capturedAtEpochMillis: Long,
)

@Entity(
    tableName = "watch_workout_imports",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = DiagnosticPayloadEntity::class,
            parentColumns = ["id"],
            childColumns = ["diagnosticPayloadId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["activityId"], unique = true),
        Index(value = ["deviceId", "origin", "crc"], unique = true),
        Index(value = ["diagnosticPayloadId"]),
    ],
)
data class WatchWorkoutImportEntity(
    @PrimaryKey val id: String,
    val activityId: String,
    val deviceId: String,
    val origin: String,
    val crc: Int,
    val externalBlockIndex: Int,
    val parserVersion: String,
    val parserValidity: String,
    val diagnosticPayloadId: String?,
    val importedAtEpochMillis: Long,
)

@Entity(
    tableName = "workout_intervals",
    foreignKeys = [ForeignKey(
        entity = WorkoutActivityEntity::class,
        parentColumns = ["id"],
        childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["activityId", "intervalIndex"], unique = true)],
)
data class WorkoutIntervalEntity(
    @PrimaryKey val id: String,
    val activityId: String,
    val intervalIndex: Int,
    val durationNanos: Long,
    val steps: Long,
    val distanceMeters: Double?,
    val caloriesKilocalories: Double?,
    val rawMotionValue: Long? = null,
    val rawDistanceValue: Long? = null,
    val rawCaloriesValue: Long? = null,
    val decodedSteps: Long? = null,
    val effectiveDurationNanos: Long? = null,
    val temporalEligibilityReason: String? = null,
    val plausibilityStatus: String? = null,
)

@Entity(
    tableName = "workout_derivation_snapshots",
    foreignKeys = [ForeignKey(
        entity = WorkoutActivityEntity::class,
        parentColumns = ["id"],
        childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [
        Index(value = ["activityId"]),
        Index(value = ["activityId", "derivedAtEpochMillis"]),
        Index(value = ["activeActivityId"], unique = true),
    ],
)
data class WorkoutDerivationSnapshotEntity(
    @PrimaryKey val id: String,
    val activityId: String,
    val stepLengthMetersUsed: Double,
    val stepLengthSource: String,
    val derivationMethod: String,
    val derivationVersion: String,
    val derivedAtEpochMillis: Long,
    val durationCase: String,
    val totalDurationNanos: Long,
    val nominalIntervalDurationNanos: Long,
    val tailUncoveredNanos: Long,
    val temporalEligibility: String,
    val temporalEligibilityReasons: String?,
    val plausibilityPolicy: String,
    val plausibilityIndicators: String?,
    val canonicalDistanceMeters: Double?,
    val canonicalCoveredDurationNanos: Long?,
    val diagnosticAggregateTotalSteps: Long?,
    val diagnosticAggregateStepLengthMeters: Double?,
    val diagnosticAggregateDistanceMeters: Double?,
    val derivedAtEpochSecond: Long,
    val derivedAtNano: Int,
    val activeActivityId: String? = null,
    @ColumnInfo(defaultValue = "0") val isActive: Boolean = false,
)

@Entity(
    tableName = "workout_derived_intervals",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDerivationSnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["snapshotId", "intervalIndex"], unique = true),
    ],
)
data class DerivedWorkoutIntervalEntity(
    @PrimaryKey val id: String,
    val snapshotId: String,
    val intervalIndex: Int,
    val effectiveStartEpochMillis: Long,
    val effectiveStartEpochSecond: Long,
    val effectiveStartNano: Int,
    val effectiveDurationNanos: Long,
    val decodedSteps: Long,
    val canonicalIntervalDistanceMeters: Double,
    val cadenceStepsPerMinute: Double,
    val speedMetersPerSecond: Double,
    val paceSecondsPerKilometer: Double?,
    val cumulativeSteps: Long,
    val cumulativeDistanceMeters: Double,
    val cumulativeDurationNanos: Long,
    val cumulativeCadenceStepsPerMinute: Double,
    val cumulativeSpeedMetersPerSecond: Double,
    val cumulativePaceSecondsPerKilometer: Double?,
    val speedSampledAtEpochMillis: Long,
    val speedSampledAtEpochSecond: Long,
    val speedSampledAtNano: Int,
)

@Entity(
    tableName = "workout_splits",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDerivationSnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["snapshotId", "splitNumber"], unique = true),
    ],
)
data class WorkoutSplitEntity(
    @PrimaryKey val id: String,
    val snapshotId: String,
    val splitNumber: Int,
    val targetDistanceMeters: Double,
    val windowStartEpochMillis: Long,
    val windowStartEpochSecond: Long,
    val windowStartNano: Int,
    val windowEndEpochMillis: Long,
    val windowEndEpochSecond: Long,
    val windowEndNano: Int,
    val distanceMeters: Double,
    val durationNanos: Long,
    val averageCadenceStepsPerMinute: Double,
    val averageSpeedMetersPerSecond: Double,
    val averagePaceSecondsPerKilometer: Double?,
    val averageBpm: Double?,
    val bpmCoveredDurationNanos: Long,
)

@Entity(
    tableName = "workout_heart_rate_samples",
    foreignKeys = [ForeignKey(
        entity = WorkoutActivityEntity::class,
        parentColumns = ["id"],
        childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["activityId", "sampledAtEpochMillis"])],
)
data class WorkoutHeartRateSampleEntity(
    @PrimaryKey val id: String,
    val activityId: String,
    val sampledAtEpochMillis: Long,
    val bpm: Int,
    val sourceSampleIndex: Int? = null,
    val sampledAtEpochSecond: Long? = null,
    val sampledAtNano: Int? = null,
)

@Entity(
    tableName = "app_workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutActivityId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["associatedDeviceId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["workoutActivityId"], unique = true),
        Index(value = ["state"]),
    ],
)
data class AppWorkoutSessionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val createdAtEpochMillis: Long,
    val startedAtEpochMillis: Long?,
    val endedAtEpochMillis: Long?,
    val activeDurationNanos: Long,
    val pausedDurationNanos: Long,
    val activeSinceEpochMillis: Long?,
    val pausedSinceEpochMillis: Long?,
    val state: String,
    val steps: Long,
    val distanceMeters: Double?,
    val distanceAbsenceReason: String?,
    val caloriesKilocalories: Double?,
    val caloriesAbsenceReason: String?,
    val averageBpm: Int?,
    val averageBpmAbsenceReason: String?,
    val minimumBpm: Int?,
    val minimumBpmAbsenceReason: String?,
    val maximumBpm: Int?,
    val maximumBpmAbsenceReason: String?,
    val workoutActivityId: String?,
    val lastTransitionAtEpochMillis: Long = createdAtEpochMillis,
    val associatedDeviceId: String? = null,
    val revision: Long = 0,
    val runtimeGeneration: Long = 0,
    val stepsAvailable: Boolean = true,
)

@Entity(
    tableName = "workout_routes",
    foreignKeys = [ForeignKey(
        entity = AppWorkoutSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["sessionId"], unique = true)],
)
data class WorkoutRouteEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val workoutType: String,
    val state: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val acceptedPointCount: Long,
    val segmentCount: Int,
    val distanceMeters: Double,
    val averageHorizontalAccuracyMeters: Double?,
    val revision: Long,
)

@Entity(
    tableName = "workout_route_points",
    foreignKeys = [ForeignKey(
        entity = WorkoutRouteEntity::class,
        parentColumns = ["id"],
        childColumns = ["routeId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["routeId", "sequence"], unique = true)],
)
data class WorkoutRoutePointEntity(
    @PrimaryKey val id: String,
    val routeId: String,
    val sequence: Long,
    val sampledAtEpochMillis: Long,
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val altitudeMeters: Double?,
    val horizontalAccuracyMeters: Double,
    val verticalAccuracyMeters: Double?,
    val speedMetersPerSecond: Double?,
    val bearingDegrees: Double?,
    val segmentIndex: Int,
    /** Preenchido somente no primeiro ponto para preservar motivo e início do segmento sem tabela paralela. */
    val segmentStartReason: String?,
    val segmentStartedAtEpochMillis: Long?,
    /** Distância acumulada do segmento neste checkpoint; preenchida no último ponto do lote. */
    val segmentDistanceMeters: Double?,
)

@Entity(
    tableName = "treadmill_calibrations",
    foreignKeys = [ForeignKey(
        entity = WorkoutActivityEntity::class,
        parentColumns = ["id"],
        childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["activityId", "revision"], unique = true)],
)
data class TreadmillCalibrationEntity(
    @PrimaryKey val id: String,
    val activityId: String,
    val revision: Int,
    val originalDistanceMeters: Double?,
    val estimatedDistanceMeters: Double?,
    val reportedDistanceMeters: Double?,
    val calibratedDistanceMeters: Double?,
    val factor: Double?,
    val factorAbsenceReason: String?,
    val appliedAtEpochMillis: Long,
    val origin: String,
    val averagePaceSecondsPerKilometer: Double?,
    val averageSpeedMetersPerSecond: Double?,
    val metricsProvenance: String,
)

@Entity(
    tableName = "heart_rate_measurements",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AppWorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["appWorkoutSessionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = WorkoutActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutActivityId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["appWorkoutSessionId"]),
        Index(value = ["workoutActivityId"]),
        Index(value = ["deduplicationKey"], unique = true),
    ],
)
data class HeartRateMeasurementEntity(
    @PrimaryKey val id: String,
    val source: String,
    val bpm: Int,
    val measuredAtEpochMillis: Long?,
    val observedLocalDateTime: String?,
    val validity: String,
    val deviceId: String?,
    val importedAtEpochMillis: Long?,
    val parserVersion: String?,
    val appWorkoutSessionId: String?,
    val workoutActivityId: String?,
    val deduplicationKey: String,
)

/** No foreign key: aggregate is polymorphic and validated by repository transaction. */
@Entity(
    tableName = "health_sync_operations",
    indices = [
        Index(
            value = [
                "aggregateId",
                "aggregateType",
                "recordType",
                "projectionChildKey",
                "operationSequence",
            ],
            unique = true,
        ),
        Index(value = ["state", "updatedAtEpochMillis"]),
        Index(value = ["clientRecordId"]),
    ],
)
data class HealthSyncOperationEntity(
    @PrimaryKey val id: String,
    val aggregateId: String,
    val aggregateType: String,
    val recordType: String,
    val state: String,
    val attemptCount: Int,
    val lastAttemptAtEpochMillis: Long?,
    val lastError: String?,
    val clientRecordId: String? = null,
    val clientRecordVersion: Long? = null,
    val syncedAtEpochMillis: Long? = null,
    val timeZoneId: String? = null,
    val createdAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long? = null,
    @ColumnInfo(defaultValue = "0") val operationSequence: Long = 0L,
    val dailyStepsPayloadKind: String? = null,
    val previousTotalSteps: Long? = null,
    val currentTotalSteps: Long? = null,
    val stepDelta: Long? = null,
    val stepStartTimeEpochMillis: Long? = null,
    val stepEndTimeEpochMillis: Long? = null,
    @ColumnInfo(defaultValue = "'__legacy__'")
    val projectionChildKey: String = LEGACY_PROJECTION_CHILD_KEY,
    @ColumnInfo(defaultValue = "'INSERT'") val action: String = "INSERT",
    val frozenPayload: String? = null,
    val frozenPayloadVersion: Int? = null,
    @ColumnInfo(defaultValue = "'EXPECTED'") val reconciliationState: String = "EXPECTED",
    @ColumnInfo(defaultValue = "'NOT_STARTED'") val reconciliationCheckpoint: String = "NOT_STARTED",
    val projectionOriginId: String? = null,
    val projectionOriginVersion: String? = null,
) {
    companion object {
        const val LEGACY_PROJECTION_CHILD_KEY = "__legacy__"
    }
}

@Entity(
    tableName = "diagnostic_derived_outputs",
    foreignKeys = [ForeignKey(
        entity = DiagnosticPayloadEntity::class,
        parentColumns = ["id"],
        childColumns = ["payloadId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["payloadId", "outputType", "ordinal"], unique = true)],
)
data class DiagnosticDerivedOutputEntity(
    @PrimaryKey val id: String,
    val payloadId: String,
    val outputType: String,
    val ordinal: Int,
)

class ChampionDatabaseConverters {
    @ColumnTypeConverter
    fun deviceModelToStorage(value: StoredDeviceModel?): String? = value?.name

    @ColumnTypeConverter
    fun storageToDeviceModel(value: String?): StoredDeviceModel? = value?.let(StoredDeviceModel::valueOf)
}

@Database(
    entities = [
        DeviceEntity::class,
        DeviceImportMetadataEntity::class,
        DeviceAssociationEntity::class,
        UserProfileEntity::class,
        WeightMeasurementEntity::class,
        StepLengthCalibrationEntity::class,
        DailySummaryEntity::class,
        HeartRateMeasurementEntity::class,
        WorkoutActivityEntity::class,
        WatchWorkoutImportEntity::class,
        WorkoutIntervalEntity::class,
        WorkoutDerivationSnapshotEntity::class,
        DerivedWorkoutIntervalEntity::class,
        WorkoutSplitEntity::class,
        WorkoutHeartRateSampleEntity::class,
        TreadmillCalibrationEntity::class,
        AppWorkoutSessionEntity::class,
        WorkoutRouteEntity::class,
        WorkoutRoutePointEntity::class,
        HealthSyncOperationEntity::class,
        DiagnosticPayloadEntity::class,
        DiagnosticDerivedOutputEntity::class,
    ],
    version = 13,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
    exportSchema = true,
)
@ColumnTypeConverters(ChampionDatabaseConverters::class)
abstract class ChampionDatabase : RoomDatabase() {
    abstract fun schemaDao(): SchemaDao
    abstract fun deviceDao(): DeviceDao
    abstract fun deviceImportMetadataDao(): DeviceImportMetadataDao
    abstract fun deviceAssociationDao(): DeviceAssociationDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun stepLengthCalibrationDao(): StepLengthCalibrationDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun workoutActivityDao(): WorkoutActivityDao
    abstract fun watchWorkoutImportDao(): WatchWorkoutImportDao
    abstract fun workoutDetailDao(): WorkoutDetailDao
    abstract fun workoutDerivationDao(): WorkoutDerivationDao
    abstract fun treadmillCalibrationDao(): TreadmillCalibrationDao
    abstract fun appWorkoutSessionDao(): AppWorkoutSessionDao
    abstract fun workoutRouteDao(): WorkoutRouteDao
    abstract fun healthSyncOperationDao(): HealthSyncOperationDao
    abstract fun diagnosticPayloadDao(): DiagnosticPayloadDao
}

@Dao
interface SchemaDao {
    @Query("SELECT 1")
    suspend fun verifyConnection(): Int
}
