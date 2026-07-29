package br.com.mwconsulting.championcompanion.core.database

import androidx.room3.Dao
import androidx.room3.Embedded
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
    @Query("SELECT * FROM user_profiles ORDER BY id")
    fun observeAllProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM weight_measurements ORDER BY id")
    fun observeAllWeights(): Flow<List<WeightMeasurementEntity>>

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    fun observeCurrent(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    suspend fun findCurrent(): UserProfileEntity?

    @Query("SELECT * FROM weight_measurements WHERE profileId = :profileId ORDER BY measuredAtEpochMillis DESC LIMIT 1")
    fun observeLatestWeight(profileId: String): Flow<WeightMeasurementEntity?>

    @Query("SELECT * FROM weight_measurements WHERE profileId = :profileId ORDER BY measuredAtEpochMillis DESC LIMIT 1")
    suspend fun findLatestWeight(profileId: String): WeightMeasurementEntity?

    @Query("SELECT * FROM weight_measurements WHERE id = :id LIMIT 1")
    suspend fun findWeightById(id: String): WeightMeasurementEntity?

    @Query("SELECT * FROM weight_measurements WHERE profileId = :profileId ORDER BY measuredAtEpochMillis DESC")
    fun observeWeights(profileId: String): Flow<List<WeightMeasurementEntity>>

    @Query("SELECT * FROM weight_measurements WHERE profileId = :profileId ORDER BY measuredAtEpochMillis")
    suspend fun findWeights(profileId: String): List<WeightMeasurementEntity>

    @Query("SELECT * FROM weight_measurements WHERE measuredAtEpochMillis >= :fromEpochMillis AND measuredAtEpochMillis < :untilEpochMillis ORDER BY measuredAtEpochMillis DESC, id DESC")
    fun observeBetween(fromEpochMillis: Long, untilEpochMillis: Long): Flow<List<WeightMeasurementEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: UserProfileEntity)

    @Update
    suspend fun update(profile: UserProfileEntity): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(weight: WeightMeasurementEntity)
}

@Dao
interface DailySummaryDao {
    @Query("SELECT * FROM daily_summaries ORDER BY id")
    fun observeAll(): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summaries WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): DailySummaryEntity?

    @Query("SELECT * FROM daily_summaries WHERE deviceId IS :deviceId AND origin = :origin AND localDateIso = :localDateIso")
    suspend fun findByNaturalKey(deviceId: String?, origin: String, localDateIso: String): DailySummaryEntity?

    @Query("SELECT * FROM daily_summaries WHERE origin = 'WATCH_DAILY_SUMMARY' AND validity = 'VALID' AND deviceId IS NOT NULL ORDER BY updatedAtEpochMillis, collectedAtEpochMillis, id")
    suspend fun findValidWatchBacklog(): List<DailySummaryEntity>

    @Query("SELECT * FROM daily_summaries WHERE localDateIso BETWEEN :fromLocalDateIso AND :toLocalDateIso ORDER BY localDateIso")
    fun observeBetween(fromLocalDateIso: String, toLocalDateIso: String): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summaries WHERE localDateIso >= :fromLocalDateIso AND localDateIso < :untilLocalDateIso AND validity = 'VALID' ORDER BY localDateIso, updatedAtEpochMillis DESC, collectedAtEpochMillis DESC, id")
    fun observeValidBetween(fromLocalDateIso: String, untilLocalDateIso: String): Flow<List<DailySummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(summary: DailySummaryEntity)

    @Update
    suspend fun update(summary: DailySummaryEntity): Int
}

@Dao
interface HeartRateDao {
    @Query("SELECT * FROM heart_rate_measurements ORDER BY id")
    fun observeAll(): Flow<List<HeartRateMeasurementEntity>>

    @Query("SELECT * FROM heart_rate_measurements WHERE id = :id")
    suspend fun findById(id: String): HeartRateMeasurementEntity?

    @Query("SELECT * FROM heart_rate_measurements WHERE deduplicationKey = :deduplicationKey")
    suspend fun findByDeduplicationKey(deduplicationKey: String): HeartRateMeasurementEntity?

    @Query("SELECT measurement.* FROM heart_rate_measurements measurement LEFT JOIN health_sync_operations sync ON sync.aggregateId = measurement.id AND sync.recordType = 'HEART_RATE' WHERE measurement.source = 'WATCH_D1_HISTORY' AND measurement.validity = 'VALID' AND measurement.measuredAtEpochMillis IS NOT NULL AND (sync.id IS NULL OR sync.state IN ('PENDING', 'FAILED')) ORDER BY measurement.measuredAtEpochMillis")
    suspend fun findValidPendingForSync(): List<HeartRateMeasurementEntity>

    @Query("SELECT * FROM heart_rate_measurements WHERE validity = 'VALID' AND measuredAtEpochMillis IS NOT NULL AND source IN ('MANUAL_MEASUREMENT', 'WATCH_D1_HISTORY') ORDER BY measuredAtEpochMillis, id")
    suspend fun findValidHealthConnectBacklog(): List<HeartRateMeasurementEntity>

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

    @Query("DELETE FROM heart_rate_measurements WHERE appWorkoutSessionId = :sessionId AND workoutActivityId IS NULL")
    suspend fun deleteTransientForSession(sessionId: String): Int
}

@Dao
interface WorkoutActivityDao {
    @Query(
        "SELECT activity.* FROM workout_activities activity " +
            "ORDER BY activity.startedAtEpochMillis DESC, activity.recordedAtEpochMillis DESC",
    )
    fun observeAll(): Flow<List<WorkoutActivityEntity>>

    @Query(
        "SELECT activity.*, " +
            "importRecord.id AS importId, importRecord.activityId AS importActivityId, " +
            "importRecord.deviceId AS importDeviceId, importRecord.origin AS importOrigin, " +
            "importRecord.crc AS importCrc, importRecord.externalBlockIndex AS importExternalBlockIndex, " +
            "importRecord.parserVersion AS importParserVersion, importRecord.parserValidity AS importParserValidity, " +
            "importRecord.diagnosticPayloadId AS importDiagnosticPayloadId, " +
            "importRecord.importedAtEpochMillis AS importImportedAtEpochMillis " +
            "FROM workout_activities activity " +
            "LEFT JOIN watch_workout_imports importRecord ON importRecord.activityId = activity.id " +
            "ORDER BY activity.id",
    )
    fun observeAllHealthSyncCandidates(): Flow<List<HealthSyncWorkoutCandidateRecord>>

    @Query(
        "SELECT activity.* FROM workout_activities activity " +
            "WHERE activity.workflowState = 'PENDING_REVIEW' " +
            "ORDER BY activity.recordedAtEpochMillis",
    )
    fun observePendingReview(): Flow<List<WorkoutActivityEntity>>

    @Query(
        "SELECT activity.* FROM workout_activities activity " +
            "WHERE activity.workflowState IN (:workflowStates) " +
            "ORDER BY activity.startedAtEpochMillis DESC, activity.recordedAtEpochMillis DESC",
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
            "FROM workout_activities activity",
    )
    fun observeWorkflowSummary(): Flow<WorkoutWorkflowSummaryRecord>

    @Query("SELECT * FROM workout_activities WHERE id = :id")
    suspend fun findById(id: String): WorkoutActivityEntity?

    @Query("SELECT * FROM workout_activities WHERE appWorkoutSessionId = :sessionId LIMIT 1")
    suspend fun findBySessionId(sessionId: String): WorkoutActivityEntity?

    @Query(
        "SELECT activity.id AS activityId, activity.origin, activity.title, activity.suggestedType, " +
            "activity.confirmedType, activity.startedAtEpochMillis, activity.durationNanos, " +
            "activity.originalDistanceMeters, activity.estimatedDistanceMeters, activity.calibratedDistanceMeters, " +
            "activity.caloriesKilocalories, activity.averageBpm, activity.workflowState " +
            "FROM workout_activities activity " +
            "WHERE activity.startedAtEpochMillis >= :fromEpochMillis " +
            "AND activity.startedAtEpochMillis < :untilEpochMillis " +
            "AND activity.workflowState != 'DISCARDED' " +
            "ORDER BY activity.startedAtEpochMillis, activity.id",
    )
    fun observeCalendarSummariesBetween(
        fromEpochMillis: Long,
        untilEpochMillis: Long,
    ): Flow<List<CalendarWorkoutSummaryRecord>>

    @Query(
        "SELECT activity.id AS activityId, activity.origin, activity.title, activity.suggestedType, " +
            "activity.confirmedType, activity.startedAtEpochMillis, activity.durationNanos, " +
            "activity.originalDistanceMeters, activity.estimatedDistanceMeters, activity.calibratedDistanceMeters, " +
            "activity.caloriesKilocalories, activity.averageBpm, activity.workflowState " +
            "FROM workout_activities activity " +
            "WHERE activity.workflowState != 'DISCARDED' " +
            "ORDER BY activity.startedAtEpochMillis, activity.id",
    )
    fun observeAllCalendarSummaries(): Flow<List<CalendarWorkoutSummaryRecord>>

    @Query("SELECT activity.* FROM workout_activities activity INNER JOIN watch_workout_imports importRecord ON importRecord.activityId = activity.id WHERE importRecord.deviceId = :deviceId AND importRecord.origin = :origin AND importRecord.crc = :crc")
    suspend fun findByImportKey(deviceId: String, origin: String, crc: Int): WorkoutActivityEntity?

    @Query("SELECT * FROM workout_activities WHERE deviceId = :deviceId AND origin = :origin AND startedAtEpochMillis = :startedAtEpochMillis ORDER BY recordedAtEpochMillis DESC LIMIT 1")
    suspend fun findByStartedAt(deviceId: String, origin: String, startedAtEpochMillis: Long): WorkoutActivityEntity?

    @Query(
        "SELECT id AS activityId, appWorkoutSessionId AS sessionId, origin, startedAtEpochMillis, " +
            "endedAtEpochMillis, deviceId AS associatedDeviceId FROM workout_activities " +
            "WHERE origin IN ('PHONE_GPS', 'PHONE_TREADMILL', 'PHONE_FREE_WORKOUT') " +
            "AND appWorkoutSessionId IS NOT NULL AND workflowState != 'DISCARDED' " +
            "AND startedAtEpochMillis >= :fromEpochMillis AND startedAtEpochMillis < :untilEpochMillis " +
            "ORDER BY startedAtEpochMillis, id",
    )
    suspend fun findPhoneCorrelationCandidates(
        fromEpochMillis: Long,
        untilEpochMillis: Long,
    ): List<PhoneWorkoutCorrelationCandidateRecord>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(activity: WorkoutActivityEntity)

    @Update
    suspend fun update(activity: WorkoutActivityEntity): Int

    @Query("UPDATE workout_activities SET workflowState = :nextState WHERE id = :id AND workflowState IN (:expectedStates)")
    suspend fun transitionState(id: String, expectedStates: List<String>, nextState: String): Int

    @Query("UPDATE workout_activities SET workflowState = :nextState WHERE workflowState = :expectedState")
    suspend fun transitionAllByState(expectedState: String, nextState: String): Int
}

data class WorkoutWorkflowSummaryRecord(
    val pendingReviewCount: Int,
    val pendingCalibrationCount: Int,
    val readyToSyncCount: Int,
    val syncedCount: Int,
    val syncFailedCount: Int,
    val discardedCount: Int,
)

data class CalendarWorkoutSummaryRecord(
    val activityId: String,
    val origin: String,
    val title: String?,
    val suggestedType: String,
    val confirmedType: String?,
    val startedAtEpochMillis: Long,
    val durationNanos: Long,
    val originalDistanceMeters: Double?,
    val estimatedDistanceMeters: Double?,
    val calibratedDistanceMeters: Double?,
    val caloriesKilocalories: Double?,
    val averageBpm: Int?,
    val workflowState: String,
)

data class HealthSyncWorkoutCandidateRecord(
    @Embedded val activity: WorkoutActivityEntity,
    val importId: String?,
    val importActivityId: String?,
    val importDeviceId: String?,
    val importOrigin: String?,
    val importCrc: Int?,
    val importExternalBlockIndex: Int?,
    val importParserVersion: String?,
    val importParserValidity: String?,
    val importDiagnosticPayloadId: String?,
    val importImportedAtEpochMillis: Long?,
)

data class PhoneWorkoutCorrelationCandidateRecord(
    val activityId: String,
    val sessionId: String,
    val origin: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val associatedDeviceId: String?,
)

data class HealthSyncSummaryRecord(
    val pendingCount: Int,
    val syncingCount: Int,
    val failedCount: Int,
    val syncedCount: Int,
    val lastSyncedAtEpochMillis: Long?,
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

    @Query("SELECT * FROM workout_heart_rate_samples ORDER BY activityId, sampledAtEpochMillis, id")
    fun observeAllHeartRateSamples(): Flow<List<WorkoutHeartRateSampleEntity>>

    @Query(
        "SELECT sample.activityId AS activityId, sample.sampledAtEpochMillis AS sampledAtEpochMillis, " +
            "sample.bpm AS bpm, activity.title AS activityTitle, activity.suggestedType AS suggestedType, " +
            "activity.confirmedType AS confirmedType " +
            "FROM workout_heart_rate_samples sample " +
            "INNER JOIN workout_activities activity ON activity.id = sample.activityId " +
            "WHERE sample.sampledAtEpochMillis >= :fromEpochMillis " +
            "AND sample.sampledAtEpochMillis < :untilEpochMillis " +
            "AND activity.workflowState != 'DISCARDED' " +
            "ORDER BY sample.sampledAtEpochMillis DESC, sample.id DESC",
    )
    fun observeHeartRateHistoryBetween(
        fromEpochMillis: Long,
        untilEpochMillis: Long,
    ): Flow<List<WorkoutHeartRateHistoryRecord>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHeartRateSamples(samples: List<WorkoutHeartRateSampleEntity>)
}

data class WorkoutHeartRateHistoryRecord(
    val activityId: String,
    val sampledAtEpochMillis: Long,
    val bpm: Int,
    val activityTitle: String?,
    val suggestedType: String,
    val confirmedType: String?,
)

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

    @Query("SELECT * FROM app_workout_sessions WHERE state IN ('ACTIVE', 'PAUSED', 'FINISHING') ORDER BY lastTransitionAtEpochMillis DESC, id DESC LIMIT 1")
    fun observeActive(): Flow<AppWorkoutSessionEntity?>

    @Query("SELECT * FROM app_workout_sessions WHERE id = :id")
    fun observeById(id: String): Flow<AppWorkoutSessionEntity?>

    @Query("SELECT * FROM app_workout_sessions WHERE state IN ('CREATED', 'ACTIVE', 'PAUSED', 'AWAITING_RECOVERY', 'FINISHING') ORDER BY createdAtEpochMillis LIMIT 1")
    suspend fun findOpenSession(): AppWorkoutSessionEntity?

    @Query("SELECT * FROM app_workout_sessions WHERE id = :id")
    suspend fun findById(id: String): AppWorkoutSessionEntity?

    @Query("SELECT * FROM app_workout_sessions WHERE workoutActivityId = :activityId LIMIT 1")
    suspend fun findByWorkoutActivityId(activityId: String): AppWorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: AppWorkoutSessionEntity)

    @Update
    suspend fun update(session: AppWorkoutSessionEntity): Int

    @Query(
        "UPDATE app_workout_sessions SET type = :type, createdAtEpochMillis = :createdAtEpochMillis, " +
            "startedAtEpochMillis = :startedAtEpochMillis, endedAtEpochMillis = :endedAtEpochMillis, " +
            "activeDurationNanos = :activeDurationNanos, pausedDurationNanos = :pausedDurationNanos, " +
            "activeSinceEpochMillis = :activeSinceEpochMillis, pausedSinceEpochMillis = :pausedSinceEpochMillis, " +
            "state = :state, steps = :steps, distanceMeters = :distanceMeters, " +
            "distanceAbsenceReason = :distanceAbsenceReason, " +
            "caloriesKilocalories = :caloriesKilocalories, caloriesAbsenceReason = :caloriesAbsenceReason, " +
            "averageBpm = :averageBpm, averageBpmAbsenceReason = :averageBpmAbsenceReason, " +
            "minimumBpm = :minimumBpm, minimumBpmAbsenceReason = :minimumBpmAbsenceReason, " +
            "maximumBpm = :maximumBpm, maximumBpmAbsenceReason = :maximumBpmAbsenceReason, " +
            "workoutActivityId = :workoutActivityId, lastTransitionAtEpochMillis = :lastTransitionAtEpochMillis, " +
            "associatedDeviceId = :associatedDeviceId, revision = :revision, " +
            "runtimeGeneration = :runtimeGeneration, stepsAvailable = :stepsAvailable " +
            "WHERE id = :id AND revision = :expectedRevision",
    )
    suspend fun updateCasFields(
        id: String,
        type: String,
        createdAtEpochMillis: Long,
        startedAtEpochMillis: Long?,
        endedAtEpochMillis: Long?,
        activeDurationNanos: Long,
        pausedDurationNanos: Long,
        activeSinceEpochMillis: Long?,
        pausedSinceEpochMillis: Long?,
        state: String,
        steps: Long,
        distanceMeters: Double?,
        distanceAbsenceReason: String?,
        caloriesKilocalories: Double?,
        caloriesAbsenceReason: String?,
        averageBpm: Int?,
        averageBpmAbsenceReason: String?,
        minimumBpm: Int?,
        minimumBpmAbsenceReason: String?,
        maximumBpm: Int?,
        maximumBpmAbsenceReason: String?,
        workoutActivityId: String?,
        lastTransitionAtEpochMillis: Long,
        associatedDeviceId: String?,
        revision: Long,
        runtimeGeneration: Long,
        stepsAvailable: Boolean,
        expectedRevision: Long,
    ): Int

    @Query("UPDATE app_workout_sessions SET state = :nextState WHERE id = :id AND state IN (:expectedStates)")
    suspend fun transitionState(id: String, expectedStates: List<String>, nextState: String): Int
}

@Dao
interface WorkoutRouteDao {
    @Query("SELECT * FROM workout_routes WHERE id = :id")
    suspend fun findById(id: String): WorkoutRouteEntity?

    @Query("SELECT * FROM workout_routes WHERE sessionId = :sessionId LIMIT 1")
    suspend fun findBySessionId(sessionId: String): WorkoutRouteEntity?

    @Query("SELECT * FROM workout_routes WHERE sessionId = :sessionId LIMIT 1")
    fun observeBySessionId(sessionId: String): Flow<WorkoutRouteEntity?>

    @Query("SELECT * FROM workout_route_points WHERE routeId = :routeId ORDER BY sequence, id")
    suspend fun findPoints(routeId: String): List<WorkoutRoutePointEntity>

    @Query("SELECT * FROM workout_route_points WHERE routeId = :routeId AND segmentIndex = :segmentIndex ORDER BY sequence, id")
    suspend fun findSegmentPoints(routeId: String, segmentIndex: Int): List<WorkoutRoutePointEntity>

    @Query("SELECT * FROM workout_route_points WHERE routeId = :routeId AND segmentIndex = :segmentIndex ORDER BY sequence, id LIMIT 1")
    suspend fun findFirstPointInSegment(routeId: String, segmentIndex: Int): WorkoutRoutePointEntity?

    @Query("SELECT * FROM workout_route_points WHERE routeId = :routeId ORDER BY sequence DESC, id DESC LIMIT 1")
    suspend fun findLastPoint(routeId: String): WorkoutRoutePointEntity?

    @Query("SELECT COUNT(*) FROM workout_route_points WHERE routeId = :routeId")
    suspend fun countPoints(routeId: String): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(route: WorkoutRouteEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPoints(points: List<WorkoutRoutePointEntity>)

    @Query(
        "UPDATE workout_routes SET sessionId = :sessionId, workoutType = :workoutType, " +
            "state = :state, startedAtEpochMillis = :startedAtEpochMillis, " +
            "endedAtEpochMillis = :endedAtEpochMillis, acceptedPointCount = :acceptedPointCount, " +
            "segmentCount = :segmentCount, distanceMeters = :distanceMeters, " +
            "averageHorizontalAccuracyMeters = :averageHorizontalAccuracyMeters, revision = :revision " +
            "WHERE id = :id AND revision = :expectedRevision",
    )
    suspend fun updateCasFields(
        id: String,
        sessionId: String,
        workoutType: String,
        state: String,
        startedAtEpochMillis: Long,
        endedAtEpochMillis: Long?,
        acceptedPointCount: Long,
        segmentCount: Int,
        distanceMeters: Double,
        averageHorizontalAccuracyMeters: Double?,
        revision: Long,
        expectedRevision: Long,
    ): Int

    @Query("DELETE FROM workout_routes WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String): Int
}

@Dao
interface HealthSyncOperationDao {
    @Query("SELECT * FROM health_sync_operations ORDER BY aggregateType, aggregateId, recordType, id")
    fun observeAll(): Flow<List<HealthSyncOperationEntity>>

    @Query("SELECT * FROM health_sync_operations WHERE id = :id")
    suspend fun findById(id: String): HealthSyncOperationEntity?

    @Query("SELECT * FROM health_sync_operations WHERE aggregateId = :aggregateId AND aggregateType = :aggregateType AND recordType = :recordType")
    suspend fun findByNaturalKey(aggregateId: String, aggregateType: String, recordType: String): HealthSyncOperationEntity?

    @Query("SELECT * FROM health_sync_operations WHERE aggregateId = :aggregateId AND aggregateType = :aggregateType ORDER BY recordType")
    suspend fun findByAggregate(aggregateId: String, aggregateType: String): List<HealthSyncOperationEntity>

    @Query("SELECT * FROM health_sync_operations WHERE aggregateType = :aggregateType AND aggregateId IN (:aggregateIds) ORDER BY aggregateId, recordType")
    fun observeByAggregateIds(aggregateType: String, aggregateIds: List<String>): Flow<List<HealthSyncOperationEntity>>

    @Query("SELECT * FROM health_sync_operations WHERE state = 'FAILED' ORDER BY updatedAtEpochMillis, lastAttemptAtEpochMillis")
    suspend fun findFailed(): List<HealthSyncOperationEntity>

    @Query("SELECT * FROM health_sync_operations WHERE state IN ('PENDING', 'FAILED') ORDER BY COALESCE(updatedAtEpochMillis, createdAtEpochMillis, lastAttemptAtEpochMillis, 0), id LIMIT :limit")
    suspend fun findProcessable(limit: Int): List<HealthSyncOperationEntity>

    @Query("SELECT * FROM health_sync_operations WHERE state = 'SYNCING' ORDER BY COALESCE(updatedAtEpochMillis, createdAtEpochMillis, lastAttemptAtEpochMillis, 0), id")
    suspend fun findSyncing(): List<HealthSyncOperationEntity>

    @Query(
        "SELECT " +
            "COALESCE(SUM(CASE WHEN state = 'PENDING' THEN 1 ELSE 0 END), 0) AS pendingCount, " +
            "COALESCE(SUM(CASE WHEN state = 'SYNCING' THEN 1 ELSE 0 END), 0) AS syncingCount, " +
            "COALESCE(SUM(CASE WHEN state = 'FAILED' THEN 1 ELSE 0 END), 0) AS failedCount, " +
            "COALESCE(SUM(CASE WHEN state = 'SYNCED' THEN 1 ELSE 0 END), 0) AS syncedCount, " +
            "MAX(syncedAtEpochMillis) AS lastSyncedAtEpochMillis " +
            "FROM health_sync_operations",
    )
    fun observeSummary(): Flow<HealthSyncSummaryRecord>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(operation: HealthSyncOperationEntity)

    @Update
    suspend fun update(operation: HealthSyncOperationEntity): Int

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
