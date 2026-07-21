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

    @Query("SELECT * FROM devices ORDER BY associatedAtEpochMillis DESC LIMIT 1")
    fun observeAssociated(): Flow<DeviceEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(device: DeviceEntity)

    @Update
    suspend fun update(device: DeviceEntity): Int
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    fun observeCurrent(): Flow<UserProfileEntity?>

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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(measurement: HeartRateMeasurementEntity)
}

@Dao
interface WorkoutActivityDao {
    @Query("SELECT * FROM workout_activities WHERE workflowState = 'PENDING_REVIEW' ORDER BY importedAtEpochMillis")
    fun observePendingReview(): Flow<List<WorkoutActivityEntity>>

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

@Dao
interface WatchWorkoutImportDao {
    @Query("SELECT * FROM watch_workout_imports WHERE activityId = :activityId")
    suspend fun findByActivityId(activityId: String): WatchWorkoutImportEntity?

    @Query("SELECT * FROM watch_workout_imports WHERE deviceId = :deviceId AND origin = :origin AND crc = :crc")
    suspend fun findByNaturalKey(deviceId: String, origin: String, crc: Int): WatchWorkoutImportEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(importRecord: WatchWorkoutImportEntity)

    @Update
    suspend fun update(importRecord: WatchWorkoutImportEntity): Int
}

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
