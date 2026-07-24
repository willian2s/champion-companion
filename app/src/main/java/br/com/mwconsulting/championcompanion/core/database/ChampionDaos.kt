package br.com.mwconsulting.championcompanion.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun findById(id: String): DeviceEntity?

    @Query("SELECT * FROM devices WHERE platformIdentifier = :platformIdentifier")
    suspend fun findByPlatformIdentifier(platformIdentifier: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(device: DeviceEntity)

    @Update
    suspend fun update(device: DeviceEntity): Int
}

@Dao
interface DeviceImportMetadataDao {
    @Query("SELECT * FROM device_import_metadata WHERE deviceId = :deviceId AND importType = :importType LIMIT 1")
    suspend fun findByDeviceAndImportType(deviceId: String, importType: String): DeviceImportMetadataEntity?

    @Query("SELECT * FROM device_import_metadata WHERE deviceId = :deviceId AND importType = :importType LIMIT 1")
    fun observeByDeviceAndImportType(deviceId: String, importType: String): Flow<DeviceImportMetadataEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: DeviceImportMetadataEntity)
}

data class AssociatedDeviceRecord(
    val id: String,
    val platformIdentifier: String,
    val name: String,
    val model: StoredDeviceModel,
    val firmware: String?,
    val associatedAtEpochMillis: Long,
    val lastSuccessfulConnectionAtEpochMillis: Long?,
    val lastImportedAtEpochMillis: Long?,
    val importParserVersion: String?,
)

@Dao
interface DeviceAssociationDao {
    @Query("SELECT d.id, d.platformIdentifier, d.name, d.model, d.firmware, a.associatedAtEpochMillis, d.lastSuccessfulConnectionAtEpochMillis, d.lastImportedAtEpochMillis, d.importParserVersion FROM device_associations a INNER JOIN devices d ON d.id = a.deviceId WHERE a.id = 'primary' LIMIT 1")
    fun observeAssociated(): Flow<AssociatedDeviceRecord?>

    @Query("SELECT * FROM device_associations WHERE id = :id")
    suspend fun findById(id: String): DeviceAssociationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(association: DeviceAssociationEntity)

    @Query("DELETE FROM device_associations WHERE id = :id")
    suspend fun deleteById(id: String): Int
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    fun observeCurrent(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM weight_measurements WHERE profileId = :profileId ORDER BY measuredAtEpochMillis DESC LIMIT 1")
    fun observeLatestWeight(profileId: String): Flow<WeightMeasurementEntity?>

    @Query("SELECT * FROM weight_measurements WHERE profileId = :profileId ORDER BY measuredAtEpochMillis DESC")
    fun observeWeights(profileId: String): Flow<List<WeightMeasurementEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: UserProfileEntity)

    @Update
    suspend fun update(profile: UserProfileEntity): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(weight: WeightMeasurementEntity)
}

@Dao
interface DailySummaryDao {
    @Query("SELECT * FROM daily_summaries WHERE deviceId IS :deviceId AND origin = :origin AND localDateIso = :localDateIso")
    suspend fun findByNaturalKey(deviceId: String?, origin: String, localDateIso: String): DailySummaryEntity?

    @Query("SELECT * FROM daily_summaries WHERE localDateIso BETWEEN :fromLocalDateIso AND :toLocalDateIso ORDER BY localDateIso")
    fun observeBetween(fromLocalDateIso: String, toLocalDateIso: String): Flow<List<DailySummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(summary: DailySummaryEntity)

    @Update
    suspend fun update(summary: DailySummaryEntity): Int
}

@Dao
interface HeartRateDao {
    @Query("SELECT * FROM heart_rate_measurements WHERE id = :id")
    suspend fun findById(id: String): HeartRateMeasurementEntity?

    @Query("SELECT * FROM heart_rate_measurements WHERE deduplicationKey = :deduplicationKey")
    suspend fun findByDeduplicationKey(deduplicationKey: String): HeartRateMeasurementEntity?

    @Query("SELECT measurement.* FROM heart_rate_measurements measurement LEFT JOIN health_sync_operations sync ON sync.aggregateId = measurement.id AND sync.recordType = 'HEART_RATE' WHERE measurement.source = 'WATCH_D1_HISTORY' AND measurement.validity = 'VALID' AND measurement.measuredAtEpochMillis IS NOT NULL AND (sync.id IS NULL OR sync.state IN ('PENDING', 'FAILED')) ORDER BY measurement.measuredAtEpochMillis")
    suspend fun findValidPendingForSync(): List<HeartRateMeasurementEntity>

    @Query("SELECT * FROM heart_rate_measurements WHERE validity = 'VALID' AND measuredAtEpochMillis IS NOT NULL ORDER BY measuredAtEpochMillis DESC, id DESC LIMIT 1")
    fun observeLatestValidMeasurement(): Flow<HeartRateMeasurementEntity?>

    @Query("SELECT * FROM heart_rate_measurements WHERE source = 'MANUAL_MEASUREMENT' AND validity = 'VALID' AND measuredAtEpochMillis IS NOT NULL ORDER BY measuredAtEpochMillis DESC, id DESC LIMIT :limit")
    fun observeRecentValidManualMeasurements(limit: Int): Flow<List<HeartRateMeasurementEntity>>

    @Query("SELECT * FROM heart_rate_measurements WHERE validity = 'VALID' AND measuredAtEpochMillis IS NOT NULL AND measuredAtEpochMillis >= :fromEpochMillis AND measuredAtEpochMillis < :untilEpochMillis AND source IN (:sources) ORDER BY measuredAtEpochMillis DESC, id DESC")
    fun observeValidHistoryBetween(
        fromEpochMillis: Long,
        untilEpochMillis: Long,
        sources: Set<String>,
    ): Flow<List<HeartRateMeasurementEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(measurement: HeartRateMeasurementEntity)
}

@Dao
interface WorkoutActivityDao {
    @Query(
        "SELECT activity.* FROM workout_activities activity " +
            "INNER JOIN watch_workout_imports importRecord ON importRecord.activityId = activity.id " +
            "ORDER BY activity.startedAtEpochMillis DESC, activity.importedAtEpochMillis DESC",
    )
    fun observeAll(): Flow<List<WorkoutActivityEntity>>

    @Query(
        "SELECT activity.* FROM workout_activities activity " +
            "INNER JOIN watch_workout_imports importRecord ON importRecord.activityId = activity.id " +
            "WHERE activity.workflowState = 'PENDING_REVIEW' " +
            "ORDER BY activity.importedAtEpochMillis",
    )
    fun observePendingReview(): Flow<List<WorkoutActivityEntity>>

    @Query(
        "SELECT activity.* FROM workout_activities activity " +
            "INNER JOIN watch_workout_imports importRecord ON importRecord.activityId = activity.id " +
            "WHERE activity.workflowState IN (:workflowStates) " +
            "ORDER BY activity.startedAtEpochMillis DESC, activity.importedAtEpochMillis DESC",
    )
    fun observeByWorkflowStates(workflowStates: List<String>): Flow<List<WorkoutActivityEntity>>

    @Query("SELECT * FROM workout_activities WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<WorkoutActivityEntity?>

    @Query(
        "SELECT " +
            "COALESCE(SUM(CASE WHEN activity.workflowState = 'PENDING_REVIEW' THEN 1 ELSE 0 END), 0) AS pendingReviewCount, " +
            "COALESCE(SUM(CASE WHEN activity.workflowState = 'PENDING_CALIBRATION' THEN 1 ELSE 0 END), 0) AS pendingCalibrationCount, " +
            "COALESCE(SUM(CASE WHEN activity.workflowState = 'READY_TO_SYNC' THEN 1 ELSE 0 END), 0) AS readyToSyncCount, " +
            "COALESCE(SUM(CASE WHEN activity.workflowState = 'SYNCED' THEN 1 ELSE 0 END), 0) AS syncedCount, " +
            "COALESCE(SUM(CASE WHEN activity.workflowState = 'SYNC_FAILED' THEN 1 ELSE 0 END), 0) AS syncFailedCount, " +
            "COALESCE(SUM(CASE WHEN activity.workflowState = 'DISCARDED' THEN 1 ELSE 0 END), 0) AS discardedCount " +
            "FROM workout_activities activity " +
            "INNER JOIN watch_workout_imports importRecord ON importRecord.activityId = activity.id",
    )
    fun observeWorkflowSummary(): Flow<WorkoutWorkflowSummaryRecord>

    @Query("SELECT * FROM workout_activities WHERE id = :id")
    suspend fun findById(id: String): WorkoutActivityEntity?

    @Query("SELECT activity.* FROM workout_activities activity INNER JOIN watch_workout_imports importRecord ON importRecord.activityId = activity.id WHERE importRecord.deviceId = :deviceId AND importRecord.origin = :origin AND importRecord.crc = :crc")
    suspend fun findByImportKey(deviceId: String, origin: String, crc: Int): WorkoutActivityEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(activity: WorkoutActivityEntity)

    @Update
    suspend fun update(activity: WorkoutActivityEntity): Int

    @Query("UPDATE workout_activities SET workflowState = :nextState WHERE id = :id AND workflowState IN (:expectedStates)")
    suspend fun transitionState(id: String, expectedStates: List<String>, nextState: String): Int
}

data class WorkoutWorkflowSummaryRecord(
    val pendingReviewCount: Int,
    val pendingCalibrationCount: Int,
    val readyToSyncCount: Int,
    val syncedCount: Int,
    val syncFailedCount: Int,
    val discardedCount: Int,
)

@Dao
interface WatchWorkoutImportDao {
    @Query("SELECT * FROM watch_workout_imports WHERE activityId = :activityId")
    suspend fun findByActivityId(activityId: String): WatchWorkoutImportEntity?

    @Query("SELECT * FROM watch_workout_imports WHERE deviceId = :deviceId AND origin = :origin AND crc = :crc")
    suspend fun findByNaturalKey(deviceId: String, origin: String, crc: Int): WatchWorkoutImportEntity?

    @Query("SELECT importRecord.id AS importId, importRecord.activityId AS activityId, importRecord.deviceId AS deviceId, importRecord.crc AS crc, importRecord.parserValidity AS parserValidity, importRecord.importedAtEpochMillis AS importedAtEpochMillis, activity.workflowState AS workflowState FROM watch_workout_imports importRecord INNER JOIN workout_activities activity ON activity.id = importRecord.activityId WHERE importRecord.deviceId = :deviceId AND importRecord.origin = :origin AND importRecord.crc = :crc LIMIT 1")
    suspend fun findLookupByNaturalKey(deviceId: String, origin: String, crc: Int): WatchWorkoutImportLookupRecord?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(importRecord: WatchWorkoutImportEntity)

    @Update
    suspend fun update(importRecord: WatchWorkoutImportEntity): Int
}

data class WatchWorkoutImportLookupRecord(
    val importId: String,
    val activityId: String,
    val deviceId: String,
    val crc: Int,
    val parserValidity: String,
    val importedAtEpochMillis: Long,
    val workflowState: String,
)

@Dao
interface WorkoutDetailDao {
    @Query("SELECT * FROM workout_intervals WHERE activityId = :activityId ORDER BY intervalIndex")
    suspend fun findIntervalsForActivity(activityId: String): List<WorkoutIntervalEntity>

    @Query("DELETE FROM workout_intervals WHERE activityId = :activityId")
    suspend fun deleteIntervalsForActivity(activityId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIntervals(intervals: List<WorkoutIntervalEntity>)

    @Query("DELETE FROM workout_heart_rate_samples WHERE activityId = :activityId")
    suspend fun deleteHeartRateSamplesForActivity(activityId: String): Int

    @Query("SELECT * FROM workout_heart_rate_samples WHERE activityId = :activityId ORDER BY sampledAtEpochMillis")
    suspend fun findHeartRateSamplesForActivity(activityId: String): List<WorkoutHeartRateSampleEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHeartRateSamples(samples: List<WorkoutHeartRateSampleEntity>)
}

@Dao
interface TreadmillCalibrationDao {
    @Query("SELECT * FROM treadmill_calibrations WHERE activityId = :activityId ORDER BY revision DESC LIMIT 1")
    suspend fun findLatestForActivity(activityId: String): TreadmillCalibrationEntity?

    @Query("SELECT * FROM treadmill_calibrations WHERE activityId = :activityId ORDER BY revision DESC LIMIT 1")
    fun observeLatestForActivity(activityId: String): Flow<TreadmillCalibrationEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(calibration: TreadmillCalibrationEntity)
}

@Dao
interface AppWorkoutSessionDao {
    @Query("SELECT * FROM app_workout_sessions WHERE state = 'AWAITING_RECOVERY' ORDER BY createdAtEpochMillis")
    fun observeRecoverable(): Flow<List<AppWorkoutSessionEntity>>

    @Query("SELECT * FROM app_workout_sessions WHERE id = :id")
    suspend fun findById(id: String): AppWorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: AppWorkoutSessionEntity)

    @Update
    suspend fun update(session: AppWorkoutSessionEntity): Int

    @Query("UPDATE app_workout_sessions SET state = :nextState WHERE id = :id AND state IN (:expectedStates)")
    suspend fun transitionState(id: String, expectedStates: List<String>, nextState: String): Int
}

@Dao
interface HealthSyncOperationDao {
    @Query("SELECT * FROM health_sync_operations WHERE id = :id")
    suspend fun findById(id: String): HealthSyncOperationEntity?

    @Query("SELECT * FROM health_sync_operations WHERE aggregateId = :aggregateId AND recordType = :recordType")
    suspend fun findByNaturalKey(aggregateId: String, recordType: String): HealthSyncOperationEntity?

    @Query("SELECT * FROM health_sync_operations WHERE state = 'FAILED' ORDER BY lastAttemptAtEpochMillis")
    suspend fun findFailed(): List<HealthSyncOperationEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(operation: HealthSyncOperationEntity)

    @Query("UPDATE health_sync_operations SET state = :nextState, lastAttemptAtEpochMillis = :attemptedAtEpochMillis, lastError = :lastError, attemptCount = attemptCount + :attemptIncrement WHERE id = :id AND state IN (:expectedStates)")
    suspend fun transitionState(id: String, expectedStates: List<String>, nextState: String, attemptedAtEpochMillis: Long?, lastError: String?, attemptIncrement: Int = 0): Int
}

@Dao
interface DiagnosticPayloadDao {
    @Query("SELECT * FROM diagnostic_payloads WHERE id = :id")
    suspend fun findById(id: String): DiagnosticPayloadEntity?

    @Query("SELECT * FROM diagnostic_payloads WHERE capturedAtEpochMillis < :expiresBeforeEpochMillis ORDER BY capturedAtEpochMillis")
    suspend fun findExpired(expiresBeforeEpochMillis: Long): List<DiagnosticPayloadEntity>

    @Query("SELECT * FROM diagnostic_payloads ORDER BY capturedAtEpochMillis")
    suspend fun findOldestFirst(): List<DiagnosticPayloadEntity>

    @Query("SELECT * FROM diagnostic_derived_outputs WHERE payloadId = :payloadId ORDER BY outputType, ordinal")
    suspend fun findDerivedOutputs(payloadId: String): List<DiagnosticDerivedOutputEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(payload: DiagnosticPayloadEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDerivedOutput(output: DiagnosticDerivedOutputEntity)

    @Query("DELETE FROM diagnostic_payloads WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>): Int
}
