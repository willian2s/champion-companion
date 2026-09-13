package br.com.mwconsulting.championcompanion.data
import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import br.com.mwconsulting.championcompanion.BuildConfig
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionD4WorkoutBlockProtocol
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionD4WorkoutParser
import br.com.mwconsulting.championcompanion.core.common.IoDispatcher
import br.com.mwconsulting.championcompanion.core.database.AppWorkoutSessionDao
import br.com.mwconsulting.championcompanion.core.database.ChampionDatabase
import br.com.mwconsulting.championcompanion.core.database.DeviceEntity
import br.com.mwconsulting.championcompanion.core.database.DiagnosticDerivedOutputEntity
import br.com.mwconsulting.championcompanion.core.database.DailySummaryEntity
import br.com.mwconsulting.championcompanion.core.database.DailyStepsCheckpointEntity
import br.com.mwconsulting.championcompanion.core.database.DailyStepsCreditReservationEntity
import br.com.mwconsulting.championcompanion.core.database.HealthSyncOperationEntity
import br.com.mwconsulting.championcompanion.core.database.HealthSyncCutoverEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutRouteDao
import br.com.mwconsulting.championcompanion.core.database.AppWorkoutSessionEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutRouteEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutRoutePointEntity
import br.com.mwconsulting.championcompanion.core.database.WatchWorkoutImportEntity
import br.com.mwconsulting.championcompanion.core.database.WatchWorkoutD4V4HeaderEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutActivityEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutDerivationRecord
import br.com.mwconsulting.championcompanion.core.database.WorkoutHeartRateSampleEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutIntervalEntity
import br.com.mwconsulting.championcompanion.core.database.HealthSyncWorkoutCandidateRecord
import br.com.mwconsulting.championcompanion.core.model.ActivityWorkflowState
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSession
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionCheckpoint
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionId
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionMutation
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionRepository
import br.com.mwconsulting.championcompanion.core.model.ApplyReferenceDistanceCalibrationInput
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionState
import br.com.mwconsulting.championcompanion.core.model.AppendWorkoutRoutePointsInput
import br.com.mwconsulting.championcompanion.core.model.AssociateWorkoutActivityInput
import br.com.mwconsulting.championcompanion.core.model.CALENDAR_HEART_RATE_SOURCES
import br.com.mwconsulting.championcompanion.core.model.CalendarDateRange
import br.com.mwconsulting.championcompanion.core.model.CalendarInstantRange
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.DataValidity
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportMetadata
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportPersistenceResult
import br.com.mwconsulting.championcompanion.core.model.DistanceCalibrationEvidence
import br.com.mwconsulting.championcompanion.core.model.DistanceCalibrationEvidenceId
import br.com.mwconsulting.championcompanion.core.model.DailySummary
import br.com.mwconsulting.championcompanion.core.model.DailySummaryId
import br.com.mwconsulting.championcompanion.core.model.DailySummaryRepository
import br.com.mwconsulting.championcompanion.core.model.DailyStepsSyncPayload
import br.com.mwconsulting.championcompanion.core.model.DailyStepsSyncPayloadKind
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayload
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadId
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadRepository
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregate
import br.com.mwconsulting.championcompanion.core.model.HealthSyncBacklogReconciliationResult
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperation
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationId
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationKey
import br.com.mwconsulting.championcompanion.core.model.HealthSyncCutover
import br.com.mwconsulting.championcompanion.core.model.HealthSyncProcessableQueue
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRepository
import br.com.mwconsulting.championcompanion.core.model.HealthSyncQueueObservation
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType
import br.com.mwconsulting.championcompanion.core.model.HealthSyncSummary
import br.com.mwconsulting.championcompanion.core.model.HeartRateChartPoint
import br.com.mwconsulting.championcompanion.core.model.HeartRateChartResolution
import br.com.mwconsulting.championcompanion.core.model.HeartRateChartSeries
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryAggregates
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryEntry
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryOrigin
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryPeriod
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryQuery
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistorySnapshot
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementId
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementSessionId
import br.com.mwconsulting.championcompanion.core.model.HeartRateRepository
import br.com.mwconsulting.championcompanion.core.model.HeartRateSource
import br.com.mwconsulting.championcompanion.core.model.MeasuredWorkoutInterval
import br.com.mwconsulting.championcompanion.core.model.MergeWatchDailySummaryInput
import br.com.mwconsulting.championcompanion.core.model.DomainResult
import br.com.mwconsulting.championcompanion.core.model.DomainValue
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregateType
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionChildKey
import br.com.mwconsulting.championcompanion.core.model.HealthLegacyDistanceProjectionStatus
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionReconciliationCheckpoint
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionReconciliationState
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionOrigin
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenPayload
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenInstant
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenProjectionPayload
import br.com.mwconsulting.championcompanion.core.model.HEALTH_SYNC_FROZEN_PROJECTION_PAYLOAD_WORKOUT_STEPS_SCHEMA_VERSION
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationAction
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsRepository
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsSnapshot
import br.com.mwconsulting.championcompanion.core.model.LearnedStepLengthProfile
import br.com.mwconsulting.championcompanion.core.model.LearnedDistanceCalibrationProfile
import br.com.mwconsulting.championcompanion.core.model.StepLengthCalibration
import br.com.mwconsulting.championcompanion.core.model.StepLengthCalibrationId
import br.com.mwconsulting.championcompanion.core.model.StepLengthCalibrationSummary
import br.com.mwconsulting.championcompanion.core.model.PhoneWorkoutPolicy
import br.com.mwconsulting.championcompanion.core.model.ParserValidity
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.SaveD1HeartRateMeasurementsInput
import br.com.mwconsulting.championcompanion.core.model.SaveManualHeartRateMeasurementInput
import br.com.mwconsulting.championcompanion.core.model.SaveManualWeightMeasurementInput
import br.com.mwconsulting.championcompanion.core.model.SafeSyncError
import br.com.mwconsulting.championcompanion.core.model.StepLengthProfile
import br.com.mwconsulting.championcompanion.core.model.StepLengthResolutionPolicy
import br.com.mwconsulting.championcompanion.core.model.StepLengthSource
import br.com.mwconsulting.championcompanion.core.model.SyncOperationState
import br.com.mwconsulting.championcompanion.core.model.TEMPORAL_ORDER_VALIDATED_MARKER
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibration
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibrationRepository
import br.com.mwconsulting.championcompanion.core.model.TemporalSeriesValidationReason
import br.com.mwconsulting.championcompanion.core.model.UpdateHeightInput
import br.com.mwconsulting.championcompanion.core.model.UpdateStepLengthInput
import br.com.mwconsulting.championcompanion.core.model.UpdateStepLengthCalibrationEvidenceInput
import br.com.mwconsulting.championcompanion.core.model.UpdateDistanceCalibrationEvidenceInput
import br.com.mwconsulting.championcompanion.core.model.UserProfileId
import br.com.mwconsulting.championcompanion.core.model.UserProfile
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import br.com.mwconsulting.championcompanion.core.model.UuidV7
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImport
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportLookup
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportMetadata
import br.com.mwconsulting.championcompanion.core.model.PersistWatchDailyD1SamplesInput
import br.com.mwconsulting.championcompanion.core.model.WatchDailyD1PersistenceResult
import br.com.mwconsulting.championcompanion.core.model.WatchDailyD1ImportMetadata
import br.com.mwconsulting.championcompanion.core.model.WatchDailyD1Repository
import br.com.mwconsulting.championcompanion.core.model.WatchDailyD1SampleKey
import br.com.mwconsulting.championcompanion.core.model.WATCH_DAILY_D1_IMPORT_TYPE
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutReprocessFailureReason
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutReprocessResult
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutPlausibilityAcceptanceFailureReason
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutPlausibilityAcceptanceResult
import br.com.mwconsulting.championcompanion.core.model.WATCH_WORKOUT_D4_V1_PARSER_VERSION
import br.com.mwconsulting.championcompanion.core.model.WATCH_WORKOUT_D4_V2_PARSER_VERSION
import br.com.mwconsulting.championcompanion.core.model.WATCH_WORKOUT_D4_V3_PARSER_VERSION
import br.com.mwconsulting.championcompanion.core.model.WATCH_WORKOUT_D4_V4_PARSER_VERSION
import br.com.mwconsulting.championcompanion.core.model.WATCH_WORKOUT_DERIVATION_VERSION
import br.com.mwconsulting.championcompanion.core.model.WATCH_WORKOUT_REFERENCE_CALIBRATION_DERIVATION_VERSION
import br.com.mwconsulting.championcompanion.core.model.WeightMeasurementId
import br.com.mwconsulting.championcompanion.core.model.WeightMeasurement
import br.com.mwconsulting.championcompanion.core.model.WeightUnit
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityFilter
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityOrigin
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import br.com.mwconsulting.championcompanion.core.model.WorkoutCanonicalReadStatus
import br.com.mwconsulting.championcompanion.core.model.WorkoutDerivationMethod
import br.com.mwconsulting.championcompanion.core.model.WorkoutDerivationSnapshot
import br.com.mwconsulting.championcompanion.core.model.WorkoutDurationResolver
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoute
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteAppendCursor
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteId
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteRepository
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteRecoverySnapshot
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteSegment
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteSegmentReason
import br.com.mwconsulting.championcompanion.core.model.WorkoutSeriesPlausibility
import br.com.mwconsulting.championcompanion.core.model.WorkoutSplitCalculator
import br.com.mwconsulting.championcompanion.core.model.WorkoutWorkflowSummary
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutNativeDistancePolicy
import br.com.mwconsulting.championcompanion.core.model.TemporalSeriesValidator
import br.com.mwconsulting.championcompanion.core.model.WorkoutStepsCredit
import br.com.mwconsulting.championcompanion.core.model.WorkoutStepsCreditBlockReason
import br.com.mwconsulting.championcompanion.core.model.WorkoutStepsCreditId
import br.com.mwconsulting.championcompanion.core.model.WorkoutStepsCreditState
import br.com.mwconsulting.championcompanion.core.model.WorkoutStepsProjectionContext
import br.com.mwconsulting.championcompanion.core.model.WorkoutStepsProjectionIdentity
import br.com.mwconsulting.championcompanion.core.model.WorkoutIntervalWindow
import br.com.mwconsulting.championcompanion.core.model.valueOrNull
import java.time.Duration
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val SQLITE_IN_CLAUSE_CHUNK_SIZE = 500
private const val ROUTE_METRIC_EPSILON = 1e-9
private const val INCREMENTAL_STEPS_CLIENT_RECORD_VERSION = 1L

private class WatchWorkoutDerivationPreflightException :
    IllegalStateException("Watch workout derivation preflight failed")

private class HeightUpdateAborted :
    IllegalStateException("Height update could not be reconciled")

private class ManualWeightSaveAborted :
    IllegalStateException("Manual weight save could not be reconciled")

private class ReferenceCalibrationAborted :
    IllegalStateException("Reference calibration could not be applied")

private class ActivityDeletionAborted :
    IllegalStateException("Watch activity deletion could not be completed")

private class WorkoutStepsPreparationAborted :
    IllegalStateException("Workout steps projection could not be prepared")

private class WorkoutStepsCreditCommitAborted :
    IllegalStateException("Workout steps credit could not be committed")

@Singleton
class RoomRepositories @Inject constructor(
    private val database: ChampionDatabase,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DeviceRepository, DailySummaryRepository, HeartRateRepository, WorkoutActivityRepository,
    TreadmillCalibrationRepository, AppWorkoutSessionRepository, HealthSyncRepository, DiagnosticPayloadRepository,
    PersonalMetricsRepository, WorkoutRouteRepository, WatchDailyD1Repository {
    private val ids = UuidV7Generator()
    private val frozenProjectionJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
    }

    override fun observeAssociatedDevice(): Flow<Device?> = database.deviceAssociationDao().observeAssociated()
        .map { it?.toDomain() }
        .flowOn(ioDispatcher)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSnapshot(): Flow<PersonalMetricsSnapshot> = database.userProfileDao().observeCurrent()
        .transformLatest { profile ->
            if (profile == null) {
                emit(PersonalMetricsSnapshot())
                return@transformLatest
            }
            val profileDomain = profile.toDomain() ?: run {
                emit(PersonalMetricsSnapshot())
                return@transformLatest
            }
            database.userProfileDao().observeLatestWeight(profile.id)
                .map { weight ->
                    PersonalMetricsSnapshot(
                        profileId = profileDomain.id,
                        heightCentimeters = profile.heightCentimeters,
                        weightKilograms = weight?.toKilogramsOrNull(),
                        latestWeightMeasuredAt = weight?.measuredAtEpochMillis?.let(Instant::ofEpochMilli),
                        latestWeightOrigin = weight?.origin?.let { enumValueOfOrNull<DataOrigin>(it) },
                        manualStrideLengthMeters = null,
                        stepLengthProfile = profileDomain.stepLengthProfile,
                    )
                }
                .collect { emit(it) }
        }
        .flowOn(ioDispatcher)

    override suspend fun associateDevice(device: Device): RepositoryResult<Device> = write {
        val deviceDao = database.deviceDao()
        val associationDao = database.deviceAssociationDao()
        val existing = deviceDao.findByPlatformIdentifier(device.platformIdentifier)
        val entity = device.toEntity()
        val persisted = if (existing == null) {
            deviceDao.insert(entity)
            entity
        } else {
            val merged = entity.copy(
                id = existing.id,
                lastSuccessfulConnectionAtEpochMillis = entity.lastSuccessfulConnectionAtEpochMillis
                    ?: existing.lastSuccessfulConnectionAtEpochMillis,
                lastImportedAtEpochMillis = entity.lastImportedAtEpochMillis ?: existing.lastImportedAtEpochMillis,
                importParserVersion = entity.importParserVersion ?: existing.importParserVersion,
            )
            deviceDao.update(merged)
            merged
        }
        associationDao.upsert(
            persisted.toAssociationEntity(
                associatedAtEpochMillis = device.associatedAt.toEpochMilli(),
                updatedAtEpochMillis = device.associatedAt.toEpochMilli(),
            ),
        )
        val mapped = persisted.toDomain(associatedAtEpochMillis = device.associatedAt.toEpochMilli())
            ?: return@write RepositoryResult.UnexpectedState
        RepositoryResult.Success(mapped)
    }

    override suspend fun associateDeviceAndUpdateLastSuccessfulConnection(
        device: Device,
        connectedAt: Instant,
    ): RepositoryResult<Device> = write {
        val associatedAtEpochMillis = device.associatedAt.toEpochMilli()
        val persisted = upsertAssociatedDevice(
            device = device,
            associatedAtEpochMillis = associatedAtEpochMillis,
            connectedAtEpochMillis = connectedAt.toEpochMilli(),
        ) ?: return@write RepositoryResult.UnexpectedState
        val mapped = persisted.toDomain(associatedAtEpochMillis = associatedAtEpochMillis)
            ?: return@write RepositoryResult.UnexpectedState
        RepositoryResult.Success(mapped)
    }

    override suspend fun updateLastSuccessfulConnection(
        deviceId: DeviceId,
        connectedAt: Instant,
    ): RepositoryResult<Device> = write {
        val existing = database.deviceDao().findById(deviceId.value.toString()) ?: return@write RepositoryResult.NotFound
        val updated = existing.copy(lastSuccessfulConnectionAtEpochMillis = connectedAt.toEpochMilli())
        database.deviceDao().update(updated)
        val associatedAt = database.deviceAssociationDao().findById(PRIMARY_DEVICE_ASSOCIATION_ID)
            ?.takeIf { it.deviceId == updated.id }
            ?.associatedAtEpochMillis
            ?: updated.associatedAtEpochMillis
        val mapped = updated.toDomain(associatedAtEpochMillis = associatedAt)
            ?: return@write RepositoryResult.UnexpectedState
        RepositoryResult.Success(mapped)
    }

    override suspend fun updateImportMetadata(
        deviceId: DeviceId,
        importedAt: Instant,
        parserVersion: br.com.mwconsulting.championcompanion.core.model.ParserVersion,
    ): RepositoryResult<Device> = write {
        val existing = database.deviceDao().findById(deviceId.value.toString()) ?: return@write RepositoryResult.NotFound
        val updated = existing.copy(
            lastImportedAtEpochMillis = importedAt.toEpochMilli(),
            importParserVersion = parserVersion.value,
        )
        database.deviceDao().update(updated)
        val associatedAt = database.deviceAssociationDao().findById(PRIMARY_DEVICE_ASSOCIATION_ID)
            ?.takeIf { it.deviceId == updated.id }
            ?.associatedAtEpochMillis
            ?: updated.associatedAtEpochMillis
        val mapped = updated.toDomain(associatedAtEpochMillis = associatedAt)
            ?: return@write RepositoryResult.UnexpectedState
        RepositoryResult.Success(mapped)
    }

    override suspend fun updateKnownMetadata(
        deviceId: DeviceId,
        name: String,
        firmware: String?,
    ): RepositoryResult<Device> = write {
        val existing = database.deviceDao().findById(deviceId.value.toString()) ?: return@write RepositoryResult.NotFound
        val updated = existing.copy(name = name, firmware = firmware)
        database.deviceDao().update(updated)
        val associatedAt = database.deviceAssociationDao().findById(PRIMARY_DEVICE_ASSOCIATION_ID)
            ?.takeIf { it.deviceId == updated.id }
            ?.associatedAtEpochMillis
            ?: updated.associatedAtEpochMillis
        val mapped = updated.toDomain(associatedAtEpochMillis = associatedAt)
            ?: return@write RepositoryResult.UnexpectedState
        RepositoryResult.Success(mapped)
    }

    override suspend fun forgetAssociatedDevice(): RepositoryResult<Unit> = write {
        database.deviceAssociationDao().deleteById(PRIMARY_DEVICE_ASSOCIATION_ID)
        RepositoryResult.Success(Unit)
    }

    override suspend fun findByPlatformIdentifier(platformIdentifier: String): RepositoryResult<Device> = withContext(ioDispatcher) {
        val mapped = database.deviceDao().findByPlatformIdentifier(platformIdentifier)
            ?.toDomain()
            ?: return@withContext RepositoryResult.NotFound
        RepositoryResult.Success(mapped)
    }

    override fun observeSummaries(from: LocalDate, to: LocalDate): Flow<List<DailySummary>> =
        database.dailySummaryDao().observeBetween(from.toString(), to.toString())
            .map { entities -> entities.mapNotNull { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeSummariesBetween(range: CalendarDateRange): Flow<List<DailySummary>> =
        database.dailySummaryDao()
            .observeValidBetween(range.fromInclusive.toString(), range.untilExclusive.toString())
            .map { entities -> entities.mapNotNull { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun findById(summaryId: DailySummaryId): RepositoryResult<DailySummary> = withContext(ioDispatcher) {
        val summary = database.dailySummaryDao().findById(summaryId.value.toString())?.toDomain()
            ?: return@withContext RepositoryResult.NotFound
        RepositoryResult.Success(summary)
    }

    override suspend fun save(summary: DailySummary): RepositoryResult<Unit> = write {
        val entity = summary.toEntity()
        val existing = database.dailySummaryDao().findByNaturalKey(entity.deviceId, entity.origin, entity.localDateIso)
        if (existing == null) database.dailySummaryDao().insert(entity)
        else database.dailySummaryDao().update(entity.copy(id = existing.id))
        RepositoryResult.Success(Unit)
    }

    override suspend fun mergeWatchDailySummary(input: MergeWatchDailySummaryInput): RepositoryResult<DailySummary> = try {
        write {
        val deviceId = input.deviceId.value.toString()
        val device = database.deviceDao().findById(deviceId) ?: return@write RepositoryResult.NotFound
        when (ensureDailyStepsCutoverInternal(
            deviceId = input.deviceId,
            activationLocalDate = input.localDate,
            zoneId = input.zoneId,
            activatedAt = input.updatedAt,
        )) {
            is RepositoryResult.Success -> Unit
            RepositoryResult.Conflict,
            RepositoryResult.UnexpectedState,
            RepositoryResult.NotFound,
            RepositoryResult.StorageUnavailable,
            -> return@write RepositoryResult.UnexpectedState
        }
        val existing = database.dailySummaryDao().findByNaturalKey(
            deviceId = deviceId,
            origin = DataOrigin.WATCH_DAILY_SUMMARY.name,
            localDateIso = input.localDate.toString(),
        )

        val persistedEntity = if (existing == null) {
            val created = createWatchDailySummary(input) ?: return@write RepositoryResult.UnexpectedState
            created.toEntity().copy(collectedZoneId = input.zoneId.id).also {
                database.dailySummaryDao().insert(it)
            }
        } else {
            if (input.incomingSteps > existing.steps) {
                val updated = existing.copy(
                    steps = input.incomingSteps,
                    estimatedDistanceMeters = input.estimatedDistanceMeters,
                    estimatedCaloriesKilocalories = input.estimatedCaloriesKilocalories,
                    collectedAtEpochMillis = input.collectedAt.toEpochMilli(),
                    updatedAtEpochMillis = input.updatedAt.toEpochMilli(),
                    collectedZoneId = existing.collectedZoneId.validZoneIdOrNull() ?: input.zoneId.id,
                )
                database.dailySummaryDao().update(updated)
                updated
            } else {
                existing
            }
        }
        val persistedSummary = persistedEntity.toDomain() ?: return@write RepositoryResult.UnexpectedState

        if (input.incomingSteps >= persistedEntity.steps) {
            enqueueDailyStepsInternal(
                summary = persistedEntity,
                collectedAtEpochMillis = input.collectedAt.toEpochMilli(),
                timeZoneId = persistedEntity.collectedZoneId.validZoneIdOrNull() ?: input.zoneId.id,
                updatedAtEpochMillis = input.updatedAt.toEpochMilli(),
            )
        }

        database.deviceDao().update(
            device.copy(
                lastImportedAtEpochMillis = input.updatedAt.toEpochMilli(),
                importParserVersion = input.parserVersion.value,
            ),
        )
            RepositoryResult.Success(persistedSummary)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: WorkoutStepsCreditCommitAborted) {
        RepositoryResult.UnexpectedState
    }

    override suspend fun import(measurements: List<HeartRateMeasurement>): RepositoryResult<Unit> = write {
        for (measurement in measurements) {
            val entity = measurement.toEntity()
            if (database.heartRateDao().findByDeduplicationKey(entity.deduplicationKey) == null) {
                database.heartRateDao().insert(entity)
                if (measurement.isEligibleForSync) {
                    database.healthSyncOperationDao().insert(newPendingOperationEntity(
                        key = HealthSyncOperationKey(
                            aggregate = HealthSyncAggregate.HeartRateMeasurement(HeartRateMeasurementId(parseUuid(entity.id) ?: return@write RepositoryResult.UnexpectedState)),
                            recordType = HealthSyncRecordType.HEART_RATE,
                        ),
                        metadata = (resolveHeartRateSyncMetadata(entity)
                            ?: return@write RepositoryResult.UnexpectedState).copy(
                            clientRecordId = stableClientRecordId(
                                HealthSyncOperationKey(
                                    aggregate = HealthSyncAggregate.HeartRateMeasurement(HeartRateMeasurementId(parseUuid(entity.id) ?: return@write RepositoryResult.UnexpectedState)),
                                    recordType = HealthSyncRecordType.HEART_RATE,
                                ),
                            ),
                        ),
                    ))
                }
            }
        }
        RepositoryResult.Success(Unit)
    }

    override suspend fun findPendingSync(): RepositoryResult<List<HeartRateMeasurement>> = withContext(ioDispatcher) {
        RepositoryResult.Success(database.heartRateDao().findValidPendingForSync().mapNotNull { it.toDomain() })
    }

    override suspend fun findById(
        measurementId: HeartRateMeasurementId,
    ): RepositoryResult<HeartRateMeasurement> = withContext(ioDispatcher) {
        val measurement = database.heartRateDao().findById(measurementId.value.toString())?.toDomain()
            ?: return@withContext RepositoryResult.NotFound
        RepositoryResult.Success(measurement)
    }

    override suspend fun saveManualMeasurement(
        input: SaveManualHeartRateMeasurementInput,
    ): RepositoryResult<HeartRateMeasurement> = write {
        val measurementDao = database.heartRateDao()
        val deduplicationKey = manualMeasurementDeduplicationKey(input.sessionId)
        val existing = measurementDao.findByDeduplicationKey(deduplicationKey)
        if (existing != null) {
            val mapped = existing.toDomain() ?: return@write RepositoryResult.UnexpectedState
            return@write if (mapped.source == HeartRateSource.MANUAL_MEASUREMENT) {
                RepositoryResult.Success(mapped)
            } else {
                RepositoryResult.UnexpectedState
            }
        }

        val measurement = HeartRateMeasurement.createManualMeasurement(
            id = input.measurementId,
            sessionId = input.sessionId,
            deviceId = input.deviceId,
            bpm = input.bpm,
            measuredAt = input.measuredAt,
            parserVersion = input.parserVersion,
        ).valueOrNull() ?: return@write RepositoryResult.UnexpectedState

        measurementDao.insert(measurement.toEntity())
        RepositoryResult.Success(measurement)
    }

    override fun observeLatestValidMeasurement(): Flow<HeartRateMeasurement?> =
        database.heartRateDao().observeLatestValidMeasurement()
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)

    override fun observeRecentManualMeasurements(limit: Int): Flow<List<HeartRateMeasurement>> {
        if (limit <= 0) return flowOf(emptyList())
        return database.heartRateDao().observeRecentValidManualMeasurements(limit)
            .map { entities -> entities.mapNotNull { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun observeHistory(query: HeartRateHistoryQuery): Flow<HeartRateHistorySnapshot> {
        val now = Instant.now()
        val dateRange = heartRateHistoryDateRange(
            period = query.period,
            today = LocalDateTime.ofInstant(now, query.zoneId).toLocalDate(),
        )
        val window = dateRange.atZone(query.zoneId)
        val sources = canonicalHistorySources(query.sources)
        if (sources.isEmpty()) {
            return flowOf(HeartRateHistorySnapshot(
                period = query.period,
                chart = HeartRateChartSeries(resolution = chartResolutionFor(query.period)),
            ))
        }
        val measurementSources = sources.intersect(HISTORY_MEASUREMENT_SOURCES)
        val measurements = if (measurementSources.isEmpty()) {
            flowOf(emptyList())
        } else {
            database.heartRateDao().observeValidHistoryBetween(
                fromEpochMillis = window.fromInclusive.toEpochMilli(),
                untilEpochMillis = window.untilExclusive.toEpochMilli(),
                sources = measurementSources.mapTo(linkedSetOf()) { it.name },
            )
        }
        val workoutEntries = if (HeartRateSource.WATCH_WORKOUT_ACTIVITY in sources) {
            database.workoutDetailDao().observeHeartRateHistoryBetween(
                fromEpochMillis = window.fromInclusive.toEpochMilli(),
                untilEpochMillis = window.untilExclusive.toEpochMilli(),
            ).map { records ->
                records.mapNotNull { record ->
                    if (record.bpm !in 30..220) return@mapNotNull null
                    val activityId = parseUuid(record.activityId)?.let(::WorkoutActivityId)
                        ?: return@mapNotNull null
                    HeartRateHistoryEntry(
                        origin = HeartRateHistoryOrigin.WorkoutActivitySample(
                            activityId = activityId,
                            activityTitle = record.activityTitle,
                            suggestedType = enumValueOfOrNull<WorkoutType>(record.suggestedType)
                                ?: WorkoutType.UNKNOWN,
                            confirmedType = record.confirmedType?.let {
                                enumValueOfOrNull<WorkoutType>(it)
                            },
                        ),
                        measuredAt = Instant.ofEpochMilli(record.sampledAtEpochMillis),
                        bpm = record.bpm,
                        source = HeartRateSource.WATCH_WORKOUT_ACTIVITY,
                    )
                }
            }
        } else {
            flowOf(emptyList())
        }
        return combine(measurements, workoutEntries) { measurementEntities, activityEntries ->
                buildHeartRateHistorySnapshot(
                    period = query.period,
                    zoneId = query.zoneId,
                    measurements = measurementEntities.mapNotNull { it.toDomain() },
                    workoutEntries = activityEntries,
                    dateRange = dateRange,
                )
            }
            .flowOn(ioDispatcher)
    }

    override fun observeMeasurementsBetween(
        range: CalendarInstantRange,
        sources: Set<HeartRateSource>,
    ): Flow<List<HeartRateMeasurement>> {
        val allowedSources = sources.intersect(CALENDAR_HEART_RATE_SOURCES)
        if (allowedSources.isEmpty()) return flowOf(emptyList())
        return database.heartRateDao()
            .observeValidHistoryBetween(
                fromEpochMillis = range.fromInclusive.toEpochMilli(),
                untilEpochMillis = range.untilExclusive.toEpochMilli(),
                sources = allowedSources.mapTo(linkedSetOf()) { it.name },
            )
            .map { entities -> entities.mapNotNull { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun importD1Measurements(
        input: SaveD1HeartRateMeasurementsInput,
    ): RepositoryResult<D1HeartRateImportPersistenceResult> = write {
        val measurementDao = database.heartRateDao()
        val syncDao = database.healthSyncOperationDao()
        val metadataDao = database.deviceImportMetadataDao()
        val incomingEntities = input.measurements.map { measurement ->
            if (measurement.source != HeartRateSource.WATCH_D1_HISTORY || !measurement.isEligibleForSync) {
                return@write RepositoryResult.UnexpectedState
            }
            measurement.toEntity()
        }

        val existingMeasurementsByKey = linkedMapOf<String, br.com.mwconsulting.championcompanion.core.database.HeartRateMeasurementEntity>()
        val existingPendingAggregateIds = linkedSetOf<String>()
        incomingEntities.forEach { entity ->
            if (existingMeasurementsByKey.containsKey(entity.deduplicationKey)) return@forEach
            val existing = measurementDao.findByDeduplicationKey(entity.deduplicationKey) ?: return@forEach
            existingMeasurementsByKey[entity.deduplicationKey] = existing
            if (syncDao.findByNaturalKey(existing.id, "HEART_RATE_MEASUREMENT", "HEART_RATE") != null) {
                existingPendingAggregateIds += existing.id
            }
        }

        val plan = planD1Import(
            existingMeasurementsByKey = existingMeasurementsByKey,
            existingPendingAggregateIds = existingPendingAggregateIds,
            incomingMeasurements = incomingEntities,
        )

        plan.measurementsToInsert.forEach { measurementDao.insert(it) }
        plan.pendingOperationAggregateIdsToCreate.forEach { aggregateId ->
            val measurement = measurementDao.findById(aggregateId) ?: return@write RepositoryResult.NotFound
            syncDao.insert(newPendingOperationEntity(
                key = HealthSyncOperationKey(
                    aggregate = HealthSyncAggregate.HeartRateMeasurement(HeartRateMeasurementId(parseUuid(aggregateId) ?: return@write RepositoryResult.UnexpectedState)),
                    recordType = HealthSyncRecordType.HEART_RATE,
                ),
                metadata = (resolveHeartRateSyncMetadata(measurement)
                    ?: return@write RepositoryResult.UnexpectedState).copy(
                    clientRecordId = stableClientRecordId(
                        HealthSyncOperationKey(
                            aggregate = HealthSyncAggregate.HeartRateMeasurement(HeartRateMeasurementId(parseUuid(aggregateId) ?: return@write RepositoryResult.UnexpectedState)),
                            recordType = HealthSyncRecordType.HEART_RATE,
                        ),
                    ),
                ),
            ))
        }

        val existingMetadata = metadataDao.findByDeviceAndImportType(
            deviceId = input.metadata.deviceId.value.toString(),
            importType = WATCH_D1_HISTORY_IMPORT_TYPE,
        )
        metadataDao.upsert(input.metadata.toEntity(id = existingMetadata?.id ?: ids.next().toString()))

        RepositoryResult.Success(D1HeartRateImportPersistenceResult(
            insertedCount = plan.insertedCount,
            duplicateCount = plan.duplicateCount,
            conflictCount = plan.conflictCount,
            pendingCreatedCount = plan.pendingCreatedCount,
            pendingExistingCount = plan.pendingExistingCount,
        ))
    }

    override fun observeD1ImportMetadata(deviceId: DeviceId): Flow<D1HeartRateImportMetadata?> =
        database.deviceImportMetadataDao()
            .observeByDeviceAndImportType(deviceId.value.toString(), WATCH_D1_HISTORY_IMPORT_TYPE)
            .map { it?.toD1ImportMetadata() }
            .flowOn(ioDispatcher)

    override suspend fun persist(
        input: PersistWatchDailyD1SamplesInput,
    ): RepositoryResult<WatchDailyD1PersistenceResult> = write {
        val sampleDao = database.watchDailyD1SampleDao()
        val deviceIds = input.samples.map { it.deviceId.value.toString() }.toSet()
        if (deviceIds.any { database.deviceDao().findById(it) == null }) {
            return@write RepositoryResult.NotFound
        }

        var insertedCount = 0
        var updatedCount = 0
        var duplicateCount = 0

        input.samples.forEach { incoming ->
            val incomingEntity = incoming.toEntity()
            val existing = sampleDao.findByNaturalKey(
                deviceId = incomingEntity.deviceId,
                resolvedLocalDateIso = incomingEntity.resolvedLocalDate,
                currentPosition = incomingEntity.currentPosition,
            )
            if (existing == null) {
                sampleDao.insert(incomingEntity)
                insertedCount += 1
            } else {
                val sameFacts = incomingEntity.copy(
                    id = existing.id,
                    importAttemptId = existing.importAttemptId,
                    importedAtEpochMillis = existing.importedAtEpochMillis,
                    updatedAtEpochMillis = existing.updatedAtEpochMillis,
                ) == existing
                if (sameFacts) {
                    duplicateCount += 1
                } else {
                    val candidate = incomingEntity.copy(
                        id = existing.id,
                        importedAtEpochMillis = existing.importedAtEpochMillis,
                    )
                    if (sampleDao.update(candidate) != 1) {
                        return@write RepositoryResult.Conflict
                    }
                    updatedCount += 1
                }
            }
        }

        RepositoryResult.Success(
            WatchDailyD1PersistenceResult(
                insertedCount = insertedCount,
                updatedCount = updatedCount,
                duplicateCount = duplicateCount,
            ),
        )
    }

    override suspend fun persistImportMetadata(
        metadata: WatchDailyD1ImportMetadata,
    ): RepositoryResult<Unit> = write {
        val deviceId = metadata.deviceId.value.toString()
        if (database.deviceDao().findById(deviceId) == null) {
            return@write RepositoryResult.NotFound
        }
        val metadataDao = database.deviceImportMetadataDao()
        val existing = metadataDao.findByDeviceAndImportType(deviceId, WATCH_DAILY_D1_IMPORT_TYPE)
        val metadataToPersist = when (metadata.lastResult) {
            br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportStatus.FAILED,
            br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportStatus.CANCELLED,
            -> metadata.copy(
                lastSuccessAt = existing?.lastSuccessAtEpochMillis?.let(Instant::ofEpochMilli),
            )

            else -> metadata
        }
        metadataDao.upsert(metadataToPersist.toEntity(id = existing?.id ?: ids.next().toString()))
        RepositoryResult.Success(Unit)
    }

    override fun observeImportMetadata(
        deviceId: DeviceId,
    ): Flow<WatchDailyD1ImportMetadata?> = database.deviceImportMetadataDao()
        .observeByDeviceAndImportType(deviceId.value.toString(), WATCH_DAILY_D1_IMPORT_TYPE)
        .map { it?.toWatchDailyD1ImportMetadata() }
        .flowOn(ioDispatcher)

    override fun observeBetween(
        deviceId: DeviceId,
        fromInclusive: LocalDate,
        untilExclusive: LocalDate,
    ): Flow<List<br.com.mwconsulting.championcompanion.core.model.WatchDailyD1Sample>> =
        database.watchDailyD1SampleDao()
            .observeBetween(
                deviceId = deviceId.value.toString(),
                fromLocalDateIso = fromInclusive.toString(),
                untilLocalDateIso = untilExclusive.toString(),
            )
            .map { samples -> samples.mapNotNull { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeResolvedAtBetween(
        deviceId: DeviceId,
        range: CalendarInstantRange,
    ): Flow<List<br.com.mwconsulting.championcompanion.core.model.WatchDailyD1Sample>> =
        database.watchDailyD1SampleDao()
            .observeResolvedAtBetween(
                deviceId = deviceId.value.toString(),
                fromEpochMillis = range.fromInclusive.toEpochMilli(),
                untilEpochMillis = range.untilExclusive.toEpochMilli(),
            )
            .map { samples -> samples.mapNotNull { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun findByNaturalKey(
        key: WatchDailyD1SampleKey,
    ): RepositoryResult<br.com.mwconsulting.championcompanion.core.model.WatchDailyD1Sample?> =
        withContext(ioDispatcher) {
            val entity = database.watchDailyD1SampleDao().findByNaturalKey(
                deviceId = key.deviceId.value.toString(),
                resolvedLocalDateIso = key.resolvedLocalDate.toString(),
                currentPosition = key.currentPosition,
            )
            if (entity == null) {
                RepositoryResult.Success(null)
            } else {
                entity.toDomain()?.let { RepositoryResult.Success(it) }
                    ?: RepositoryResult.UnexpectedState
            }
        }

    override suspend fun persistImported(activity: WorkoutActivity, importRecord: WatchWorkoutImport): RepositoryResult<Unit> = try {
        write {
            val activityEntity = activity.toEntity()
            val importEntity = importRecord.toEntity()
            val exactImport = database.watchWorkoutImportDao().findByNaturalKey(importEntity.deviceId, importEntity.origin, importEntity.crc)
            val existingActivity = when {
                exactImport != null -> database.workoutActivityDao().findById(exactImport.activityId)
                else -> database.workoutActivityDao().findByStartedAt(
                    deviceId = activityEntity.deviceId ?: return@write RepositoryResult.UnexpectedState,
                    origin = activityEntity.origin,
                    startedAtEpochMillis = activityEntity.startedAtEpochMillis,
                )
            }
            val existingImport = when {
                exactImport != null -> exactImport
                existingActivity != null -> database.watchWorkoutImportDao().findByActivityId(existingActivity.id)
                else -> null
            }
            val plan = planWatchWorkoutPersistence(
                incomingActivity = activityEntity,
                incomingImport = importEntity,
                existingImport = existingImport,
                existingActivity = existingActivity,
            ) ?: return@write if (existingImport != null && existingActivity == null) RepositoryResult.NotFound else RepositoryResult.UnexpectedState
            val persistedId = plan.persistedActivity.id
            val existingD4V4Header = database.watchWorkoutD4V4HeaderDao()
                .findByActivityId(persistedId)
            val incomingD4V4Header = importRecord.d4V4Block?.let {
                importRecord.toD4V4HeaderEntity(
                    id = ids.next().toString(),
                    activityId = persistedId,
                    importId = plan.persistedImport.id,
                )
            }
            val incomingIntervals = activity
                .toIntervals(
                    activityId = persistedId,
                    ids = generateIds(activity.intervals.size),
                    d4V4Block = importRecord.d4V4Block,
                )
                .markPersistedSourceOrder(activity)
            val incomingHeartRateSamples = activity.toHeartRateSamples(
                persistedId,
                generateIds(activity.heartRateSamples.size),
            )
            val existingIntervals = if (plan.isReimport) {
                database.workoutDetailDao().findIntervalsForActivity(persistedId)
            } else {
                emptyList()
            }
            val existingHeartRateSamples = if (plan.isReimport) {
                database.workoutDetailDao().findHeartRateSamplesForActivity(persistedId)
            } else {
                emptyList()
            }
            val sameInputs = plan.isReimport && existingActivity != null && sameWatchWorkoutInputs(
                existingActivity = existingActivity,
                incomingActivity = plan.persistedActivity,
                existingImport = existingImport,
                incomingImport = plan.persistedImport,
                existingIntervals = existingIntervals,
                incomingIntervals = incomingIntervals,
                existingHeartRateSamples = existingHeartRateSamples,
                incomingHeartRateSamples = incomingHeartRateSamples,
                existingD4V4Header = existingD4V4Header,
                incomingD4V4Header = incomingD4V4Header,
            )
            if (plan.isReimport && existingActivity?.workflowState == ActivityWorkflowState.SYNCED.name && !sameInputs) {
                // Um reimport diferente não pode deixar projeção remota aceita
                // apontando para fatos locais novos sem reconciliação explícita.
                return@write RepositoryResult.Conflict
            }
            if (!plan.isReimport) {
                database.workoutActivityDao().insert(plan.persistedActivity)
                database.watchWorkoutImportDao().insert(plan.persistedImport)
            } else {
                database.workoutActivityDao().update(plan.persistedActivity)
                database.watchWorkoutImportDao().update(plan.persistedImport)
            }
            val activeSnapshot = if (plan.isReimport) {
                database.workoutDerivationDao().findActiveSnapshotForActivity(persistedId)
            } else {
                null
            }
            val preserveActiveDerivation = plan.isReimport &&
                activeSnapshot != null &&
                existingActivity != null &&
                sameInputs
            if (plan.isReimport && activeSnapshot != null && !preserveActiveDerivation) {
                // Reimportação é explícita, mas não é rederivação. Retire snapshot
                // incompatível da leitura ativa e preserve-o como histórico.
                database.workoutDerivationDao().deactivateActiveSnapshots(persistedId)
            }
            if (!preserveActiveDerivation) {
                database.workoutDetailDao().deleteIntervalsForActivity(persistedId)
                database.workoutDetailDao().insertIntervals(incomingIntervals)
            }
            if (incomingD4V4Header != null) {
                val persistedHeader = incomingD4V4Header.copy(id = existingD4V4Header?.id ?: incomingD4V4Header.id)
                if (existingD4V4Header == null) {
                    database.watchWorkoutD4V4HeaderDao().insert(persistedHeader)
                } else if (database.watchWorkoutD4V4HeaderDao().update(persistedHeader) != 1) {
                    throw WatchWorkoutDerivationPreflightException()
                }
            } else if (existingD4V4Header != null) {
                database.watchWorkoutD4V4HeaderDao().deleteByActivityId(persistedId)
            }
            database.workoutDetailDao().deleteHeartRateSamplesForActivity(persistedId)
            database.workoutDetailDao().insertHeartRateSamples(incomingHeartRateSamples)

            if (!plan.isReimport &&
                plan.persistedImport.parserVersion in setOf(
                    WATCH_WORKOUT_D4_V2_PARSER_VERSION,
                    WATCH_WORKOUT_D4_V3_PARSER_VERSION,
                    WATCH_WORKOUT_D4_V4_PARSER_VERSION,
                ) &&
                plan.persistedImport.parserValidity == ParserValidity.VALID.name
            ) {
                // Ausência de perfil ou série inelegível permanece segura. Falha
                // de preflight aborta transação para não ocultar corrupção.
                when (deriveWatchWorkoutInternal(
                    activityId = activity.id,
                    derivedAt = importRecord.importedAt,
                    replaceActive = false,
                )) {
                    is RepositoryResult.Success -> Unit
                    else -> throw WatchWorkoutDerivationPreflightException()
                }
            }
            RepositoryResult.Success(Unit)
        }
    } catch (_: WatchWorkoutDerivationPreflightException) {
        // A exceção atravessa withWriteTransaction para desfazer atividade,
        // importação e intervalos quando preflight detectar corrupção.
        RepositoryResult.UnexpectedState
    }

    override suspend fun findWatchWorkoutImport(deviceId: DeviceId, crc: Int): RepositoryResult<WatchWorkoutImportLookup?> = withContext(ioDispatcher) {
        RepositoryResult.Success(
            database.watchWorkoutImportDao()
                .findLookupByNaturalKey(deviceId.value.toString(), DataOrigin.WATCH_D3_D4.name, crc)
                ?.toDomain(),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePendingReview(): Flow<List<WorkoutActivity>> = observeActivities(WorkoutActivityFilter.PENDING_REVIEW)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActivities(filter: WorkoutActivityFilter): Flow<List<WorkoutActivity>> = combine(
        when (filter) {
        WorkoutActivityFilter.ALL -> database.workoutActivityDao().observeAll()
        else -> database.workoutActivityDao().observeByWorkflowStates(filter.toStoredWorkflowStates())
        },
        database.workoutDerivationDao().observeActiveSnapshots(),
    ) { activities, _ -> activities }.transformLatest { activities ->
        emit(activities.mapNotNull { activity -> loadWorkoutActivity(activity) })
    }.flowOn(ioDispatcher)

    override fun observeActivitiesBetween(range: CalendarInstantRange) =
        database.workoutActivityDao()
            .observeCalendarSummariesBetween(
                fromEpochMillis = range.fromInclusive.toEpochMilli(),
                untilEpochMillis = range.untilExclusive.toEpochMilli(),
            )
            .map { records -> records.mapNotNull { it.toCalendarSummary() } }
            .flowOn(ioDispatcher)

    override fun observeActivitiesIntersecting(range: CalendarInstantRange) =
        database.workoutActivityDao()
            .observeCalendarSummariesIntersecting(
                fromEpochMillis = range.fromInclusive.toEpochMilli(),
                untilEpochMillis = range.untilExclusive.toEpochMilli(),
            )
            .map { records -> records.mapNotNull { it.toCalendarSummary() } }
            .flowOn(ioDispatcher)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActivity(activityId: WorkoutActivityId): Flow<WorkoutActivity?> =
        combine(
            database.workoutActivityDao().observeById(activityId.value.toString()),
            database.workoutDerivationDao().observeActiveSnapshotForActivity(activityId.value.toString()),
        ) { activity, _ -> activity }
            .transformLatest { activity ->
                emit(activity?.let { loadWorkoutActivity(it) })
            }
            .flowOn(ioDispatcher)

    override fun observeWorkflowSummary(): Flow<WorkoutWorkflowSummary> =
        database.workoutActivityDao().observeWorkflowSummary()
            .map { it.toDomain() }
            .flowOn(ioDispatcher)

    override fun observeWatchWorkoutImportMetadata(deviceId: DeviceId): Flow<WatchWorkoutImportMetadata?> =
        database.deviceImportMetadataDao()
            .observeByDeviceAndImportType(deviceId.value.toString(), WATCH_D3_D4_IMPORT_TYPE)
            .map { it?.toWatchWorkoutImportMetadata() }
            .flowOn(ioDispatcher)

    override suspend fun findById(activityId: WorkoutActivityId): RepositoryResult<WorkoutActivity> = withContext(ioDispatcher) {
        val activity = database.workoutActivityDao().findById(activityId.value.toString())
            ?.let { loadWorkoutActivity(it) }
            ?: return@withContext RepositoryResult.NotFound
        RepositoryResult.Success(activity)
    }

    override suspend fun findPhoneCorrelationCandidates(
        fromInclusive: Instant,
        untilExclusive: Instant,
    ): RepositoryResult<List<br.com.mwconsulting.championcompanion.core.model.PhoneWorkoutCorrelationCandidate>> =
        withContext(ioDispatcher) {
            if (!untilExclusive.isAfter(fromInclusive)) {
                return@withContext RepositoryResult.UnexpectedState
            }
            RepositoryResult.Success(
                database.workoutActivityDao().findPhoneCorrelationCandidates(
                    fromEpochMillis = fromInclusive.toEpochMilli(),
                    untilEpochMillis = untilExclusive.toEpochMilli(),
                ).mapNotNull { it.toDomain() },
            )
        }

    override suspend fun updateWatchWorkoutImportMetadata(metadata: WatchWorkoutImportMetadata): RepositoryResult<Unit> = write {
        val metadataDao = database.deviceImportMetadataDao()
        val existing = metadataDao.findByDeviceAndImportType(metadata.deviceId.value.toString(), WATCH_D3_D4_IMPORT_TYPE)
        metadataDao.upsert(metadata.toEntity(id = existing?.id ?: ids.next().toString(), importType = WATCH_D3_D4_IMPORT_TYPE))
        RepositoryResult.Success(Unit)
    }

    override suspend fun findWatchWorkoutDerivation(
        activityId: WorkoutActivityId,
    ): RepositoryResult<WorkoutDerivationSnapshot?> = read {
        val activity = database.workoutActivityDao().findById(activityId.value.toString())
            ?: return@read RepositoryResult.NotFound
        if (activity.origin != br.com.mwconsulting.championcompanion.core.model.WorkoutActivityOrigin.WATCH_D3_D4.name) {
            return@read RepositoryResult.UnexpectedState
        }
        val importRecord = database.watchWorkoutImportDao().findByActivityId(activity.id)
        val intervals = database.workoutDetailDao().findIntervalsForActivity(activity.id)
        val samples = database.workoutDetailDao().findHeartRateSamplesForActivity(activity.id)
        RepositoryResult.Success(
            findActiveDerivation(
                activity = activity,
                importRecord = importRecord,
                intervals = intervals,
                samples = samples,
            ),
        )
    }

    override suspend fun deriveWatchWorkout(
        activityId: WorkoutActivityId,
        derivedAt: Instant,
    ): RepositoryResult<WorkoutDerivationSnapshot?> = try {
        write {
            deriveWatchWorkoutInternal(activityId, derivedAt, replaceActive = false)
        }
    } catch (_: WatchWorkoutDerivationPreflightException) {
        RepositoryResult.UnexpectedState
    }

    override suspend fun rederiveWatchWorkout(
        activityId: WorkoutActivityId,
        derivedAt: Instant,
    ): RepositoryResult<WorkoutDerivationSnapshot?> = try {
        write {
            deriveWatchWorkoutInternal(activityId, derivedAt, replaceActive = true)
        }
    } catch (_: WatchWorkoutDerivationPreflightException) {
        RepositoryResult.UnexpectedState
    }

    override suspend fun reprocessWatchWorkout(
        activityId: WorkoutActivityId,
        derivedAt: Instant,
    ): WatchWorkoutReprocessResult = try {
        writeTransaction {
            reprocessWatchWorkoutInternal(activityId, derivedAt)
        }
    } catch (_: WatchWorkoutDerivationPreflightException) {
        WatchWorkoutReprocessResult.Blocked(WatchWorkoutReprocessFailureReason.DERIVATION_INCONSISTENT)
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        WatchWorkoutReprocessResult.Blocked(WatchWorkoutReprocessFailureReason.STORAGE_UNAVAILABLE)
    }

    override suspend fun submitReview(input: br.com.mwconsulting.championcompanion.core.model.SubmitWorkoutReviewInput): RepositoryResult<WorkoutActivity> = try {
        write {
            val entity = database.workoutActivityDao().findById(input.activityId.value.toString())
                ?: return@write RepositoryResult.NotFound
            val importRecord = database.watchWorkoutImportDao().findByActivityId(entity.id)
            val intervals = database.workoutDetailDao().findIntervalsForActivity(entity.id)
            val samples = database.workoutDetailDao().findHeartRateSamplesForActivity(entity.id)
            val d4V4Header = database.watchWorkoutD4V4HeaderDao().findByActivityId(entity.id)
            val activity = entity.toDomain(
                importRecord = importRecord,
                intervals = intervals,
                samples = samples,
                derivationSnapshot = findActiveDerivation(entity, importRecord, intervals, samples),
                d4V4Header = d4V4Header,
            ) ?: return@write RepositoryResult.UnexpectedState
            val reviewed = activity.submitReview(input).valueOrNull()
                ?: return@write RepositoryResult.UnexpectedState
            if (input.acceptPlausibility) {
                val acceptedSnapshot = reviewed.derivationSnapshot
                    ?: return@write RepositoryResult.UnexpectedState
                if (!updateActiveDerivationSnapshot(entity.id, acceptedSnapshot)) {
                    return@write RepositoryResult.UnexpectedState
                }
            }
            val hasNativeSnapshot = reviewed.derivationSnapshot?.derivationMethod in setOf(
                WorkoutDerivationMethod.WATCH_NATIVE,
                WorkoutDerivationMethod.WATCH_NATIVE_CONTEXT_CALIBRATED,
            )
            val needsNativePolicyReset = hasNativeSnapshot &&
                reviewed.confirmedType !in setOf(WorkoutType.RUNNING, WorkoutType.WALKING)
            database.workoutActivityDao().update(reviewed.toEntity())
            val needsV4ReviewDerivation = reviewed.activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4 &&
                reviewed.watchImportContext?.parserVersion?.value == WATCH_WORKOUT_D4_V4_PARSER_VERSION &&
                (reviewed.confirmedType in setOf(WorkoutType.RUNNING, WorkoutType.WALKING) ||
                    reviewed.confirmedType != WorkoutType.CYCLING &&
                        (reviewed.derivationSnapshot == null || needsNativePolicyReset))
            if (needsNativePolicyReset) {
                database.workoutDerivationDao().deactivateActiveSnapshots(entity.id)
            }
            if (!needsV4ReviewDerivation) {
                return@write loadWorkoutActivity(
                    database.workoutActivityDao().findById(entity.id)
                        ?: return@write RepositoryResult.NotFound,
                )?.let { RepositoryResult.Success(it) }
                    ?: RepositoryResult.UnexpectedState
            }

            val derived = when (
                val result = deriveWatchWorkoutInternal(
                    activityId = input.activityId,
                    derivedAt = input.reviewedAt,
                    replaceActive = true,
                )
            ) {
                is RepositoryResult.Success -> result.value
                else -> throw WatchWorkoutDerivationPreflightException()
            }
            if (derived == null) {
                if (reviewed.confirmedType != WorkoutType.TREADMILL) {
                    throw WatchWorkoutDerivationPreflightException()
                }
                return@write loadWorkoutActivity(
                    database.workoutActivityDao().findById(entity.id)
                        ?: return@write RepositoryResult.NotFound,
                )?.let { RepositoryResult.Success(it) }
                    ?: RepositoryResult.UnexpectedState
            }
            loadWorkoutActivity(
                database.workoutActivityDao().findById(entity.id)
                    ?: return@write RepositoryResult.NotFound,
            )?.let { RepositoryResult.Success(it) }
                ?: RepositoryResult.UnexpectedState
        }
    } catch (_: WatchWorkoutDerivationPreflightException) {
        // A falha de derivação precisa desfazer também o READY_TO_SYNC já escrito.
        RepositoryResult.UnexpectedState
    }

    override suspend fun acceptWatchWorkoutPlausibility(
        activityId: WorkoutActivityId,
    ): WatchWorkoutPlausibilityAcceptanceResult = try {
        writeTransaction {
            val entity = database.workoutActivityDao().findById(activityId.value.toString())
                ?: return@writeTransaction WatchWorkoutPlausibilityAcceptanceResult.Blocked(
                    WatchWorkoutPlausibilityAcceptanceFailureReason.ACTIVITY_NOT_FOUND,
                )
            when (entity.workflowState) {
                ActivityWorkflowState.SYNCED.name -> return@writeTransaction WatchWorkoutPlausibilityAcceptanceResult.Blocked(
                    WatchWorkoutPlausibilityAcceptanceFailureReason.SYNCED_BLOCKED,
                )

                ActivityWorkflowState.SYNCING.name -> return@writeTransaction WatchWorkoutPlausibilityAcceptanceResult.Blocked(
                    WatchWorkoutPlausibilityAcceptanceFailureReason.SYNC_IN_PROGRESS,
                )
            }
            if (entity.origin != WorkoutActivityOrigin.WATCH_D3_D4.name) {
                return@writeTransaction WatchWorkoutPlausibilityAcceptanceResult.Blocked(
                    WatchWorkoutPlausibilityAcceptanceFailureReason.NOT_WATCH_WORKOUT,
                )
            }
            if (entity.workflowState !in setOf(
                    ActivityWorkflowState.PENDING_REVIEW.name,
                    ActivityWorkflowState.READY_TO_SYNC.name,
                    ActivityWorkflowState.SYNC_FAILED.name,
                )
            ) {
                return@writeTransaction WatchWorkoutPlausibilityAcceptanceResult.Blocked(
                    WatchWorkoutPlausibilityAcceptanceFailureReason.INVALID_WORKFLOW_STATE,
                )
            }
            val importRecord = database.watchWorkoutImportDao().findByActivityId(entity.id)
                ?: return@writeTransaction WatchWorkoutPlausibilityAcceptanceResult.Blocked(
                    WatchWorkoutPlausibilityAcceptanceFailureReason.DERIVATION_NOT_MATERIALIZED,
                )
            val intervals = database.workoutDetailDao().findIntervalsForActivity(entity.id)
            val samples = database.workoutDetailDao().findHeartRateSamplesForActivity(entity.id)
            val d4V4Header = database.watchWorkoutD4V4HeaderDao().findByActivityId(entity.id)
            val snapshot = findActiveDerivation(
                activity = entity,
                importRecord = importRecord,
                intervals = intervals,
                samples = samples,
            ) ?: return@writeTransaction WatchWorkoutPlausibilityAcceptanceResult.Blocked(
                WatchWorkoutPlausibilityAcceptanceFailureReason.DERIVATION_NOT_MATERIALIZED,
            )
            if (!snapshot.canonicalSeriesEligible) {
                return@writeTransaction WatchWorkoutPlausibilityAcceptanceResult.Blocked(
                    WatchWorkoutPlausibilityAcceptanceFailureReason.SERIES_INELIGIBLE,
                )
            }
            if (snapshot.hasPlausibilityIndicators) {
                return@writeTransaction WatchWorkoutPlausibilityAcceptanceResult.Blocked(
                    WatchWorkoutPlausibilityAcceptanceFailureReason.INDICATORS_PRESENT,
                )
            }
            val activity = entity.toDomain(
                importRecord = importRecord,
                intervals = intervals,
                samples = samples,
                derivationSnapshot = snapshot,
                d4V4Header = d4V4Header,
            ) ?: return@writeTransaction WatchWorkoutPlausibilityAcceptanceResult.Blocked(
                WatchWorkoutPlausibilityAcceptanceFailureReason.INVALID_WORKFLOW_STATE,
            )
            val accepted = activity.acceptPlausibility().valueOrNull()
                ?: return@writeTransaction WatchWorkoutPlausibilityAcceptanceResult.Blocked(
                    WatchWorkoutPlausibilityAcceptanceFailureReason.INVALID_WORKFLOW_STATE,
                )
            if (!updateActiveDerivationSnapshot(entity.id, accepted.derivationSnapshot ?: return@writeTransaction WatchWorkoutPlausibilityAcceptanceResult.Blocked(
                    WatchWorkoutPlausibilityAcceptanceFailureReason.DERIVATION_NOT_MATERIALIZED,
                ))) {
                return@writeTransaction WatchWorkoutPlausibilityAcceptanceResult.Blocked(
                    WatchWorkoutPlausibilityAcceptanceFailureReason.STORAGE_UNAVAILABLE,
                )
            }
            WatchWorkoutPlausibilityAcceptanceResult.Accepted(accepted)
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        WatchWorkoutPlausibilityAcceptanceResult.Blocked(
            WatchWorkoutPlausibilityAcceptanceFailureReason.STORAGE_UNAVAILABLE,
        )
    }

    override suspend fun review(activityId: WorkoutActivityId, type: WorkoutType, reviewedAt: Instant): RepositoryResult<WorkoutActivity> = write {
        val entity = database.workoutActivityDao().findById(activityId.value.toString()) ?: return@write RepositoryResult.NotFound
        val importRecord = database.watchWorkoutImportDao().findByActivityId(entity.id)
        val existing = entity.toDomain(
            importRecord,
            database.workoutDetailDao().findIntervalsForActivity(entity.id),
            database.workoutDetailDao().findHeartRateSamplesForActivity(entity.id),
            d4V4Header = database.watchWorkoutD4V4HeaderDao().findByActivityId(entity.id),
        ) ?: return@write RepositoryResult.UnexpectedState
        submitReview(
            br.com.mwconsulting.championcompanion.core.model.SubmitWorkoutReviewInput(
                activityId = activityId,
                confirmedType = type,
                title = existing.title,
                notes = existing.notes,
                reviewedAt = reviewedAt,
                manualDistanceMeters = (existing.metrics.distances.manualDistanceMeters as? DomainValue.Present)
                    ?.value
                    ?.takeIf { type == WorkoutType.CYCLING },
            ),
        )
    }

    override suspend fun startSync(activityId: WorkoutActivityId): RepositoryResult<WorkoutActivity> = write {
        transitionWorkoutSync(activityId) { it.startSync() }
    }

    override suspend fun markSynced(activityId: WorkoutActivityId): RepositoryResult<WorkoutActivity> = write {
        transitionWorkoutSync(activityId) { it.markSynced() }
    }

    override suspend fun markSyncFailed(activityId: WorkoutActivityId): RepositoryResult<WorkoutActivity> = write {
        transitionWorkoutSync(activityId) { it.markSyncFailed() }
    }

    override suspend fun resetInterruptedSyncing(): RepositoryResult<Int> = write {
        RepositoryResult.Success(
            database.workoutActivityDao().transitionAllByState(
                expectedState = "SYNCING",
                nextState = "SYNC_FAILED",
            ),
        )
    }

    override suspend fun discard(activityId: WorkoutActivityId): RepositoryResult<Unit> = write {
        when (database.workoutActivityDao().transitionState(activityId.value.toString(), listOf("PENDING_REVIEW", "PENDING_CALIBRATION", "READY_TO_SYNC"), "DISCARDED")) {
            1 -> RepositoryResult.Success(Unit)
            else -> if (database.workoutActivityDao().findById(activityId.value.toString()) == null) RepositoryResult.NotFound else RepositoryResult.UnexpectedState
        }
    }

    override suspend fun isActivityDeletionAllowed(
        activityId: WorkoutActivityId,
    ): RepositoryResult<Boolean> = try {
        read {
            val activity = database.workoutActivityDao().findById(activityId.value.toString())
                ?: return@read RepositoryResult.NotFound
            RepositoryResult.Success(isSafeLocalWatchActivityDeletionCandidate(activity))
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        RepositoryResult.StorageUnavailable
    }

    override suspend fun deleteActivityCascade(
        activityId: WorkoutActivityId,
    ): RepositoryResult<Unit> = try {
        writeTransaction {
            val activityIdValue = activityId.value.toString()
            val activity = database.workoutActivityDao().findById(activityIdValue)
                ?: return@writeTransaction RepositoryResult.NotFound
            if (!isSafeLocalWatchActivityDeletionCandidate(activity)) {
                return@writeTransaction RepositoryResult.UnexpectedState
            }

            val importRecord = database.watchWorkoutImportDao().findByActivityId(activityIdValue)
            if (database.watchWorkoutImportDao().countByActivityId(activityIdValue) > 1) {
                return@writeTransaction RepositoryResult.UnexpectedState
            }
            if (importRecord != null && importRecord.activityId != activityIdValue) {
                return@writeTransaction RepositoryResult.UnexpectedState
            }

            // Diagnostic payload may be shared by reimports/activities. Delete it
            // only when this activity/import is its sole local reference.
            val diagnosticPayloadIds = buildSet {
                activity.diagnosticPayloadId?.let(::add)
                importRecord?.diagnosticPayloadId?.let(::add)
            }
            diagnosticPayloadIds.forEach { payloadId ->
                if (database.diagnosticPayloadDao().countActivityReferencesExcluding(payloadId, activityIdValue) != 0 ||
                    database.diagnosticPayloadDao().countImportReferencesExcluding(payloadId, activityIdValue) != 0
                ) {
                    return@writeTransaction RepositoryResult.UnexpectedState
                }
            }

            // No remote child is allowed here. Delete local queue rows only after
            // the preflight above proves every operation is local and pending-free.
            database.healthSyncOperationDao().deleteByAggregate(
                aggregateId = activityIdValue,
                aggregateType = HealthSyncAggregateType.WORKOUT_ACTIVITY.name,
            )
            database.stepLengthCalibrationDao().deleteByActivityId(activityIdValue)
            database.distanceCalibrationEvidenceDao().deleteByActivityId(activityIdValue)
            database.treadmillCalibrationDao().deleteByActivityId(activityIdValue)
            database.workoutDerivationDao().deleteDerivedIntervalsForActivity(activityIdValue)
            database.workoutDerivationDao().deleteSplitsForActivity(activityIdValue)
            database.workoutDerivationDao().deleteSnapshotsForActivity(activityIdValue)
            database.workoutDetailDao().deleteIntervalsForActivity(activityIdValue)
            database.workoutDetailDao().deleteHeartRateSamplesForActivity(activityIdValue)
            database.heartRateDao().deleteByWorkoutActivityId(activityIdValue)
            database.watchWorkoutImportDao().deleteByActivityId(activityIdValue)
            diagnosticPayloadIds.forEach { payloadId ->
                database.diagnosticPayloadDao().deleteByIds(listOf(payloadId))
            }

            // Activity is parent of most local children and is intentionally last.
            if (database.workoutActivityDao().deleteById(activityIdValue) != 1) {
                throw ActivityDeletionAborted()
            }
            RepositoryResult.Success(Unit)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: ActivityDeletionAborted) {
        RepositoryResult.UnexpectedState
    } catch (_: Exception) {
        RepositoryResult.StorageUnavailable
    }

    override suspend fun register(calibration: TreadmillCalibration): RepositoryResult<Unit> = write {
        val entity = calibration.toEntity() ?: return@write RepositoryResult.UnexpectedState
        val activity = database.workoutActivityDao().findById(entity.activityId) ?: return@write RepositoryResult.NotFound
        if (activity.workflowState != "PENDING_CALIBRATION") return@write RepositoryResult.UnexpectedState
        database.treadmillCalibrationDao().insert(entity)
        database.workoutActivityDao().update(activity.copy(
            calibratedDistanceMeters = entity.calibratedDistanceMeters,
            calibratedAveragePaceSecondsPerKilometer = entity.averagePaceSecondsPerKilometer,
            calibratedAverageSpeedMetersPerSecond = entity.averageSpeedMetersPerSecond,
            calibrationId = entity.id,
            workflowState = "READY_TO_SYNC",
        ))
        RepositoryResult.Success(Unit)
    }

    override suspend fun calibrateImportedActivity(
        input: br.com.mwconsulting.championcompanion.core.model.CalibrateImportedActivityInput,
    ): RepositoryResult<br.com.mwconsulting.championcompanion.core.model.TreadmillCalibrationSnapshot> = write {
        val activityEntity = database.workoutActivityDao().findById(input.activityId.value.toString())
            ?: return@write RepositoryResult.NotFound
        val activity = activityEntity.toDomain(
            database.watchWorkoutImportDao().findByActivityId(activityEntity.id),
            database.workoutDetailDao().findIntervalsForActivity(activityEntity.id),
            database.workoutDetailDao().findHeartRateSamplesForActivity(activityEntity.id),
        ) ?: return@write RepositoryResult.UnexpectedState
        if (activity.workflowState != br.com.mwconsulting.championcompanion.core.model.ActivityWorkflowState.PENDING_CALIBRATION) {
            return@write RepositoryResult.UnexpectedState
        }

        val latest = database.treadmillCalibrationDao().findLatestForActivity(activityEntity.id)
        val revision = (latest?.revision ?: 0) + 1
        val calibration = br.com.mwconsulting.championcompanion.core.model.TreadmillCalibration.forImportedActivity(
            id = br.com.mwconsulting.championcompanion.core.model.TreadmillCalibrationId(ids.next()),
            activity = activity,
            revision = revision,
            reportedMeters = input.reportedMeters,
            appliedAt = input.appliedAt,
        ).valueOrNull() ?: return@write RepositoryResult.UnexpectedState

        val updatedActivity = activity.applyCalibration(calibration).valueOrNull()
            ?: return@write RepositoryResult.UnexpectedState
        val calibrationEntity = calibration.toEntity() ?: return@write RepositoryResult.UnexpectedState

        database.treadmillCalibrationDao().insert(calibrationEntity)
        database.workoutActivityDao().update(updatedActivity.toEntity())

        RepositoryResult.Success(calibrationEntity.toSnapshot() ?: return@write RepositoryResult.UnexpectedState)
    }

    override fun observeLatestCalibration(activityId: WorkoutActivityId) =
        database.treadmillCalibrationDao()
            .observeLatestForActivity(activityId.value.toString())
            .map { it?.toSnapshot() }
            .flowOn(ioDispatcher)

    override suspend fun findProfileById(profileId: UserProfileId): RepositoryResult<br.com.mwconsulting.championcompanion.core.model.UserProfile> = withContext(ioDispatcher) {
        val profile = database.userProfileDao().findById(profileId.value.toString())?.toDomain()
            ?: return@withContext RepositoryResult.NotFound
        RepositoryResult.Success(profile)
    }

    override suspend fun findWeightById(
        measurementId: WeightMeasurementId,
    ): RepositoryResult<br.com.mwconsulting.championcompanion.core.model.WeightMeasurement> = withContext(ioDispatcher) {
        val weight = database.userProfileDao().findWeightById(measurementId.value.toString())?.toDomain()
            ?: return@withContext RepositoryResult.NotFound
        RepositoryResult.Success(weight)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeWeightHistory(): Flow<List<WeightMeasurement>> =
        database.userProfileDao().observeCurrent().transformLatest { profile ->
            if (profile == null) {
                emit(emptyList())
            } else {
                database.userProfileDao()
                    .observeWeights(profile.id)
                    .map { entities -> entities.mapNotNull { it.toDomain() } }
                    .collect { emit(it) }
            }
        }.flowOn(ioDispatcher)

    override suspend fun listWeightHistory(): RepositoryResult<List<WeightMeasurement>> = try {
        read {
            val profile = database.userProfileDao().findCurrent()
                ?: return@read RepositoryResult.Success(emptyList())
            RepositoryResult.Success(
                database.userProfileDao().findWeights(profile.id).mapNotNull { it.toDomain() },
            )
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        RepositoryResult.StorageUnavailable
    }

    override suspend fun saveManualWeightMeasurement(
        input: SaveManualWeightMeasurementInput,
    ): RepositoryResult<WeightMeasurement> = try {
        write {
            if (!input.valueKilograms.isFinite() || input.valueKilograms <= 0.0) {
                return@write RepositoryResult.UnexpectedState
            }
            val profileEntity = database.userProfileDao().findCurrent()
                ?: return@write RepositoryResult.NotFound
            val profile = profileEntity.toDomain() ?: return@write RepositoryResult.UnexpectedState
            val measurement = WeightMeasurement.create(
                id = WeightMeasurementId(ids.next()),
                value = input.valueKilograms,
                unit = WeightUnit.KILOGRAMS,
                measuredAt = input.measuredAt,
                origin = DataOrigin.MANUAL_ENTRY,
            ).valueOrNull() ?: return@write RepositoryResult.UnexpectedState
            database.userProfileDao().insert(measurement.toEntity(profile.id))
            when (ensureOperationInternal(
                HealthSyncOperationKey(
                    aggregate = HealthSyncAggregate.WeightMeasurement(measurement.id),
                    recordType = HealthSyncRecordType.WEIGHT,
                ),
            )) {
                is RepositoryResult.Success -> RepositoryResult.Success(measurement)
                else -> throw ManualWeightSaveAborted()
            }
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: ManualWeightSaveAborted) {
        RepositoryResult.UnexpectedState
    } catch (_: Exception) {
        RepositoryResult.StorageUnavailable
    }

    override fun observeWeightsBetween(range: CalendarInstantRange) =
        database.userProfileDao()
            .observeBetween(
                fromEpochMillis = range.fromInclusive.toEpochMilli(),
                untilEpochMillis = range.untilExclusive.toEpochMilli(),
            )
            .map { entities -> entities.mapNotNull { it.toCalendarSummary() } }
            .flowOn(ioDispatcher)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeStepLengthCalibrations(workoutType: WorkoutType): Flow<List<StepLengthCalibrationSummary>> =
        database.userProfileDao().observeCurrent().transformLatest { profile ->
            if (profile == null) {
                emit(emptyList())
            } else {
                database.stepLengthCalibrationDao()
                    .observeByProfileAndWorkoutType(profile.id, workoutType.name)
                    .map { entities -> entities.mapNotNull { it.toDomain()?.toSummary() } }
                    .collect { emit(it) }
            }
        }.flowOn(ioDispatcher)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLearnedStepLengthProfiles(): Flow<List<LearnedStepLengthProfile>> =
        database.userProfileDao().observeCurrent().transformLatest { profile ->
            if (profile == null) {
                emit(emptyList())
            } else {
                combine(
                    database.stepLengthCalibrationDao()
                        .observeByProfileAndWorkoutType(profile.id, WorkoutType.RUNNING.name)
                        .map { it.toLearnedStepLengthProfile(WorkoutType.RUNNING) },
                    database.stepLengthCalibrationDao()
                        .observeByProfileAndWorkoutType(profile.id, WorkoutType.WALKING.name)
                        .map { it.toLearnedStepLengthProfile(WorkoutType.WALKING) },
                ) { running, walking -> listOfNotNull(running, walking) }
                    .collect { emit(it) }
            }
        }.flowOn(ioDispatcher)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLearnedDistanceCalibrationProfiles(): Flow<List<LearnedDistanceCalibrationProfile>> =
        database.userProfileDao().observeCurrent().transformLatest { profile ->
            if (profile == null) {
                emit(emptyList())
            } else {
                combine(
                    database.distanceCalibrationEvidenceDao()
                        .observeActiveByProfileAndWorkoutType(profile.id, WorkoutType.RUNNING.name)
                        .map { it.toLearnedDistanceCalibrationProfile(WorkoutType.RUNNING) },
                    database.distanceCalibrationEvidenceDao()
                        .observeActiveByProfileAndWorkoutType(profile.id, WorkoutType.WALKING.name)
                        .map { it.toLearnedDistanceCalibrationProfile(WorkoutType.WALKING) },
                ) { running, walking -> listOfNotNull(running, walking) }
                    .collect { emit(it) }
            }
        }.flowOn(ioDispatcher)

    override suspend fun applyReferenceDistanceCalibration(
        input: ApplyReferenceDistanceCalibrationInput,
    ): RepositoryResult<br.com.mwconsulting.championcompanion.core.model.ReferenceDistanceCalibrationResult> = try {
        writeTransaction {
            val activity = database.workoutActivityDao().findById(input.activityId.value.toString())
                ?: return@writeTransaction RepositoryResult.NotFound
            val type = activity.confirmedType?.let { runCatching { WorkoutType.valueOf(it) }.getOrNull() }
                ?: return@writeTransaction RepositoryResult.UnexpectedState
            if (activity.origin != WorkoutActivityOrigin.WATCH_D3_D4.name ||
                type !in setOf(WorkoutType.RUNNING, WorkoutType.WALKING) ||
                activity.workflowState !in setOf(
                    ActivityWorkflowState.PENDING_CALIBRATION.name,
                    ActivityWorkflowState.READY_TO_SYNC.name,
                    ActivityWorkflowState.SYNC_FAILED.name,
                ) || hasUnsafeWorkoutProjectionChild(activity.id)
            ) return@writeTransaction RepositoryResult.UnexpectedState

            val importRecord = database.watchWorkoutImportDao().findByActivityId(activity.id)
                ?: return@writeTransaction RepositoryResult.UnexpectedState
            val rawIntervals = database.workoutDetailDao().findIntervalsForActivity(activity.id)
            val profileEntity = database.userProfileDao().findCurrent()
                ?: return@writeTransaction RepositoryResult.NotFound
            val profile = profileEntity.toDomain() ?: return@writeTransaction RepositoryResult.UnexpectedState

            val nativeWatchDistance = nativeWatchDistanceForCalibration(
                activity = activity,
                importRecord = importRecord,
                rawIntervals = rawIntervals,
            )
            if (nativeWatchDistance != null) {
                val existingEntity = database.distanceCalibrationEvidenceDao().findByActivityId(activity.id)
                val existing = existingEntity?.toDomain()
                    ?: existingEntity?.let { return@writeTransaction RepositoryResult.UnexpectedState }
                if (existing != null && existing.profileId != profile.id) {
                    return@writeTransaction RepositoryResult.UnexpectedState
                }
                val evidence = DistanceCalibrationEvidence.create(
                    id = existing?.id ?: DistanceCalibrationEvidenceId(ids.next()),
                    profileId = profile.id,
                    activityId = input.activityId,
                    workoutType = type,
                    referenceDistanceMeters = input.referenceDistanceMeters,
                    watchDistanceMeters = nativeWatchDistance,
                    acceptedAt = existing?.acceptedAt ?: input.acceptedAt,
                    updatedAt = input.acceptedAt,
                    isActive = true,
                ).valueOrNull() ?: return@writeTransaction RepositoryResult.UnexpectedState
                val snapshot = deriveWatchWorkoutInternal(
                    activityId = input.activityId,
                    derivedAt = input.acceptedAt,
                    replaceActive = true,
                    explicitDistanceCalibrationFactor = evidence.distanceCalibrationFactor,
                ).let { result ->
                    when (result) {
                        is RepositoryResult.Success -> result.value
                        else -> throw ReferenceCalibrationAborted()
                    }
                } ?: throw ReferenceCalibrationAborted()
                database.distanceCalibrationEvidenceDao().upsert(evidence.toEntity())
                if (activity.workflowState == ActivityWorkflowState.PENDING_CALIBRATION.name) {
                    val transitioned = database.workoutActivityDao().transitionState(
                        id = activity.id,
                        expectedStates = listOf(ActivityWorkflowState.PENDING_CALIBRATION.name),
                        nextState = ActivityWorkflowState.READY_TO_SYNC.name,
                    )
                    if (transitioned != 1) throw ReferenceCalibrationAborted()
                }
                return@writeTransaction RepositoryResult.Success(
                    br.com.mwconsulting.championcompanion.core.model.ReferenceDistanceCalibrationResult(
                        calibration = null,
                        snapshot = snapshot,
                        contextualEvidence = evidence,
                    ),
                )
            }

            val decodedSteps = canonicalDecodedStepsForCalibration(activity, importRecord, rawIntervals)
                ?: return@writeTransaction RepositoryResult.UnexpectedState
            val stepLength = input.referenceDistanceMeters / decodedSteps.toDouble()
            val explicitProfile = StepLengthProfile.create(
                stepLength,
                br.com.mwconsulting.championcompanion.core.model.StepLengthSource.REFERENCE_DISTANCE_CALIBRATION,
                input.acceptedAt,
            ).valueOrNull() ?: return@writeTransaction RepositoryResult.UnexpectedState
            val existing = database.stepLengthCalibrationDao().findByActivityId(activity.id)
            val calibration = StepLengthCalibration.create(
                id = existing?.id
                    ?.let { UuidV7.parse(it).valueOrNull() }?.let(::StepLengthCalibrationId)
                    ?: StepLengthCalibrationId(ids.next()),
                profileId = profile.id,
                activityId = input.activityId,
                workoutType = type,
                referenceDistanceMeters = input.referenceDistanceMeters,
                decodedSteps = decodedSteps,
                acceptedAt = existing?.acceptedAtEpochMillis?.let(Instant::ofEpochMilli) ?: input.acceptedAt,
                updatedAt = input.acceptedAt,
            ).valueOrNull() ?: return@writeTransaction RepositoryResult.UnexpectedState

            val snapshot = deriveWatchWorkoutInternal(
                activityId = input.activityId,
                derivedAt = input.acceptedAt,
                replaceActive = true,
                explicitStepLength = explicitProfile,
                explicitDerivationVersion = WATCH_WORKOUT_REFERENCE_CALIBRATION_DERIVATION_VERSION,
            ).let { result ->
                when (result) {
                    is RepositoryResult.Success -> result.value
                    else -> throw ReferenceCalibrationAborted()
                }
            } ?: throw ReferenceCalibrationAborted()
            database.stepLengthCalibrationDao().upsert(calibration.toEntity())
            if (activity.workflowState == ActivityWorkflowState.PENDING_CALIBRATION.name) {
                val transitioned = database.workoutActivityDao().transitionState(
                    id = activity.id,
                    expectedStates = listOf(ActivityWorkflowState.PENDING_CALIBRATION.name),
                    nextState = ActivityWorkflowState.READY_TO_SYNC.name,
                )
                if (transitioned != 1) throw ReferenceCalibrationAborted()
            }
            RepositoryResult.Success(
                br.com.mwconsulting.championcompanion.core.model.ReferenceDistanceCalibrationResult(
                    calibration = calibration,
                    snapshot = snapshot,
                ),
            )
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: ReferenceCalibrationAborted) {
        RepositoryResult.UnexpectedState
    } catch (_: Exception) {
        RepositoryResult.StorageUnavailable
    }

    override suspend fun updateDistanceCalibrationEvidence(
        input: UpdateDistanceCalibrationEvidenceInput,
    ): RepositoryResult<DistanceCalibrationEvidence> = try {
        writeTransaction {
            val activity = database.workoutActivityDao().findById(input.activityId.value.toString())
                ?: return@writeTransaction RepositoryResult.NotFound
            if (!canMutateReferenceDistanceCalibration(activity)) {
                return@writeTransaction RepositoryResult.UnexpectedState
            }
            val existingEntity = database.distanceCalibrationEvidenceDao().findByActivityId(activity.id)
                ?: return@writeTransaction RepositoryResult.NotFound
            val existing = existingEntity.toDomain()
                ?: return@writeTransaction RepositoryResult.UnexpectedState
            val requestedWatchDistance = input.watchDistanceMeters
            if (requestedWatchDistance != null &&
                (!requestedWatchDistance.isFinite() || requestedWatchDistance <= 0.0 ||
                    requestedWatchDistance != existing.watchDistanceMeters)
            ) {
                return@writeTransaction if (requestedWatchDistance != existing.watchDistanceMeters) {
                    RepositoryResult.Conflict
                } else {
                    RepositoryResult.UnexpectedState
                }
            }
            val profile = database.userProfileDao().findCurrent()?.toDomain()
                ?: return@writeTransaction RepositoryResult.NotFound
            if (existing.profileId != profile.id) {
                return@writeTransaction RepositoryResult.UnexpectedState
            }
            val updated = DistanceCalibrationEvidence.create(
                id = existing.id,
                profileId = existing.profileId,
                activityId = existing.activityId,
                workoutType = existing.workoutType,
                referenceDistanceMeters = input.referenceDistanceMeters,
                watchDistanceMeters = existing.watchDistanceMeters,
                acceptedAt = existing.acceptedAt,
                updatedAt = input.updatedAt,
                isActive = true,
            ).valueOrNull() ?: return@writeTransaction RepositoryResult.UnexpectedState
            when (val result = deriveWatchWorkoutInternal(
                activityId = input.activityId,
                derivedAt = input.updatedAt,
                replaceActive = true,
                explicitDistanceCalibrationFactor = updated.distanceCalibrationFactor,
            ).let { result ->
                when (result) {
                    is RepositoryResult.Success -> result.value
                    else -> throw ReferenceCalibrationAborted()
                }
            }) {
                null -> throw ReferenceCalibrationAborted()
                else -> Unit
            }
            if (database.distanceCalibrationEvidenceDao().update(updated.toEntity()) != 1) {
                throw ReferenceCalibrationAborted()
            }
            // Snapshot is intentionally not returned here; its active row was
            // replaced explicitly, while older rows remain immutable history.
            RepositoryResult.Success(updated)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: ReferenceCalibrationAborted) {
        RepositoryResult.UnexpectedState
    } catch (_: Exception) {
        RepositoryResult.StorageUnavailable
    }

    override suspend fun removeDistanceCalibrationEvidence(
        activityId: WorkoutActivityId,
    ): RepositoryResult<Unit> = try {
        write {
            val activity = database.workoutActivityDao().findById(activityId.value.toString())
                ?: return@write RepositoryResult.NotFound
            if (!canMutateReferenceDistanceCalibration(activity)) {
                return@write RepositoryResult.UnexpectedState
            }
            val deactivated = database.distanceCalibrationEvidenceDao().deactivateByActivityId(
                activityId = activityId.value.toString(),
                updatedAtEpochMillis = Instant.now().toEpochMilli(),
            )
            if (deactivated == 1) RepositoryResult.Success(Unit) else RepositoryResult.NotFound
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        RepositoryResult.StorageUnavailable
    }

    override suspend fun removeStepLengthCalibration(activityId: WorkoutActivityId): RepositoryResult<Unit> = try {
        write {
            val activity = database.workoutActivityDao().findById(activityId.value.toString())
                ?: return@write RepositoryResult.NotFound
            if (!canMutateReferenceDistanceCalibration(activity)) return@write RepositoryResult.UnexpectedState
            val deleted = database.stepLengthCalibrationDao().deleteByActivityId(activityId.value.toString())
            if (deleted == 0) RepositoryResult.NotFound else RepositoryResult.Success(Unit)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        RepositoryResult.StorageUnavailable
    }

    override suspend fun updateStepLengthCalibrationEvidence(
        input: UpdateStepLengthCalibrationEvidenceInput,
    ): RepositoryResult<StepLengthCalibration> = try {
        write {
            val activity = database.workoutActivityDao().findById(input.activityId.value.toString())
                ?: return@write RepositoryResult.NotFound
            if (!canMutateReferenceDistanceCalibration(activity)) return@write RepositoryResult.UnexpectedState
            val existingEntity = database.stepLengthCalibrationDao().findByActivityId(activity.id)
                ?: return@write RepositoryResult.NotFound
            val existing = existingEntity.toDomain() ?: return@write RepositoryResult.UnexpectedState
            if (existing.decodedSteps != input.decodedSteps) return@write RepositoryResult.Conflict
            val profile = database.userProfileDao().findCurrent()?.toDomain()
                ?: return@write RepositoryResult.NotFound
            if (existing.profileId != profile.id) return@write RepositoryResult.UnexpectedState
            val updated = StepLengthCalibration.create(
                id = existing.id,
                profileId = existing.profileId,
                activityId = existing.activityId,
                workoutType = existing.workoutType,
                referenceDistanceMeters = input.referenceDistanceMeters,
                decodedSteps = existing.decodedSteps,
                acceptedAt = existing.acceptedAt,
                updatedAt = input.updatedAt,
            ).valueOrNull() ?: return@write RepositoryResult.UnexpectedState
            if (database.stepLengthCalibrationDao().update(updated.toEntity()) != 1) {
                return@write RepositoryResult.Conflict
            }
            RepositoryResult.Success(updated)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        RepositoryResult.StorageUnavailable
    }

    override suspend fun resolveEffectiveStepLength(workoutType: WorkoutType): RepositoryResult<StepLengthProfile> = try {
        read {
            val profile = database.userProfileDao().findCurrent()?.toDomain()
            resolveEffectiveStepLengthInternal(workoutType, profile)
                ?.let { RepositoryResult.Success(it) }
                ?: RepositoryResult.NotFound
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        RepositoryResult.StorageUnavailable
    }

    /** Resolve perfil e evidência no mesmo snapshot transacional da derivação. */
    private suspend fun resolveEffectiveStepLengthInternal(
        workoutType: WorkoutType,
        profile: UserProfile?,
    ): StepLengthProfile? {
        val learnedProfiles = if (profile != null && workoutType in setOf(WorkoutType.RUNNING, WorkoutType.WALKING)) {
            database.stepLengthCalibrationDao()
                .observeByProfileAndWorkoutType(profile.id.value.toString(), workoutType.name)
                .first()
                .toLearnedStepLengthProfile(workoutType)
                ?.let(::listOf)
                .orEmpty()
        } else {
            emptyList()
        }
        return StepLengthResolutionPolicy.resolve(
            workoutType = workoutType,
            baseProfile = profile?.stepLengthProfile,
            learnedProfiles = learnedProfiles,
        )
    }

    /** Resolve somente fator nativo ativo; calibração legacy nunca participa. */
    private suspend fun resolveLearnedDistanceCalibrationFactorInternal(
        workoutType: WorkoutType,
        profile: UserProfile?,
    ): Double? {
        if (profile == null || workoutType !in setOf(WorkoutType.RUNNING, WorkoutType.WALKING)) return null
        return database.distanceCalibrationEvidenceDao()
            .observeActiveByProfileAndWorkoutType(profile.id.value.toString(), workoutType.name)
            .first()
            .toLearnedDistanceCalibrationProfile(workoutType)
            ?.distanceCalibrationFactor
    }

    override suspend fun findReferenceDistanceCalibrationSteps(
        activityId: WorkoutActivityId,
    ): RepositoryResult<Long> = try {
        read {
            val activity = database.workoutActivityDao().findById(activityId.value.toString())
                ?: return@read RepositoryResult.NotFound
            val importRecord = database.watchWorkoutImportDao().findByActivityId(activity.id)
            val intervals = database.workoutDetailDao().findIntervalsForActivity(activity.id)
            canonicalDecodedStepsForCalibration(activity, importRecord, intervals)
                ?.let { RepositoryResult.Success(it) }
                ?: RepositoryResult.UnexpectedState
        }
    } catch (_: Exception) {
        RepositoryResult.StorageUnavailable
    }

    override suspend fun isReferenceDistanceCalibrationAllowed(
        activityId: WorkoutActivityId,
    ): RepositoryResult<Boolean> = try {
        read {
            val activity = database.workoutActivityDao().findById(activityId.value.toString())
                ?: return@read RepositoryResult.NotFound
            RepositoryResult.Success(canMutateReferenceDistanceCalibration(activity))
        }
    } catch (_: Exception) {
        RepositoryResult.StorageUnavailable
    }

    override suspend fun updateStepLength(input: UpdateStepLengthInput): RepositoryResult<UserProfile> = try {
        write {
            val profileEntity = database.userProfileDao().findCurrent()
            val currentProfile = profileEntity?.toDomain()
                ?: if (profileEntity == null) null else return@write RepositoryResult.UnexpectedState
            val profileUpdatedAt = currentProfile?.updatedAt?.let { existingUpdatedAt ->
                if (input.updatedAt.isAfter(existingUpdatedAt)) input.updatedAt else existingUpdatedAt
            } ?: input.updatedAt
            val stepLengthProfile = StepLengthProfile.create(
                stepLengthMeters = input.stepLengthMeters,
                source = input.source,
                updatedAt = input.updatedAt,
            ).valueOrNull() ?: return@write RepositoryResult.UnexpectedState
            val updatedProfile = UserProfile.create(
                id = currentProfile?.id ?: UserProfileId(ids.next()),
                heightCentimeters = currentProfile?.heightCentimeters,
                updatedAt = profileUpdatedAt,
                stepLengthProfile = stepLengthProfile,
            ).valueOrNull() ?: return@write RepositoryResult.UnexpectedState
            val updatedEntity = updatedProfile.toEntity()
            if (currentProfile == null) {
                database.userProfileDao().insert(updatedEntity)
            } else if (database.userProfileDao().update(updatedEntity) != 1) {
                return@write RepositoryResult.Conflict
            }
            val persistedProfile = database.userProfileDao().findById(updatedEntity.id)?.toDomain()
                ?: return@write RepositoryResult.UnexpectedState
            RepositoryResult.Success(persistedProfile)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        RepositoryResult.StorageUnavailable
    }

    override suspend fun updateHeight(input: UpdateHeightInput): RepositoryResult<UserProfile> = try {
        write {
            val profileEntity = database.userProfileDao().findCurrent()
            val currentProfile = profileEntity?.toDomain()
                ?: if (profileEntity == null) null else return@write RepositoryResult.UnexpectedState
            val existingHeightEntity = currentProfile?.let { profile ->
                database.healthSyncOperationDao().findByNaturalKey(
                    aggregateId = profile.id.value.toString(),
                    aggregateType = HealthSyncAggregateType.USER_PROFILE_HEIGHT.name,
                    recordType = HealthSyncRecordType.HEIGHT.name,
                )
            }
            if (existingHeightEntity?.state == SyncOperationState.SYNCING.name) {
                return@write RepositoryResult.Conflict
            }
            if (existingHeightEntity != null && existingHeightEntity.toDomain() == null) {
                return@write RepositoryResult.UnexpectedState
            }
            val updatedProfile = UserProfile.create(
                id = currentProfile?.id ?: UserProfileId(ids.next()),
                heightCentimeters = input.heightCentimeters,
                updatedAt = monotonicProfileUpdatedAt(
                    requested = input.updatedAt,
                    existing = currentProfile?.updatedAt,
                ),
                // Altura é independente do passo base. Preserva perfil, origem,
                // valor e instante existentes sem reconstrução.
                stepLengthProfile = currentProfile?.stepLengthProfile,
            ).valueOrNull() ?: return@write RepositoryResult.UnexpectedState
            val updatedEntity = updatedProfile.toEntity()
            if (currentProfile == null) {
                database.userProfileDao().insert(updatedEntity)
            } else if (database.userProfileDao().update(updatedEntity) != 1) {
                return@write RepositoryResult.Conflict
            }
            val persistedProfile = database.userProfileDao().findById(updatedEntity.id)?.toDomain()
                ?: return@write RepositoryResult.UnexpectedState
            val reconciled = ensureHeightHealthSync(persistedProfile)
            val requiresReconciledOperation = persistedProfile.heightCentimeters != null || existingHeightEntity != null
            if (requiresReconciledOperation && reconciled == null) {
                // Profile and retry metadata commit together. Preparation
                // failure rolls back local mutation instead of reporting success.
                throw HeightUpdateAborted()
            }
            RepositoryResult.Success(persistedProfile)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: HeightUpdateAborted) {
        RepositoryResult.UnexpectedState
    } catch (_: Exception) {
        RepositoryResult.StorageUnavailable
    }

    /**
     * Mantém identidade única de altura também quando valor local é removido.
     * Sem altura, não há INSERT novo; operação existente vira DELETE usando o
     * mesmo clientRecordId, sem criar outra fila.
     */
    private suspend fun ensureHeightHealthSync(profile: UserProfile): ReconciledOperation? {
        val key = HealthSyncOperationKey(
            aggregate = HealthSyncAggregate.UserProfileHeight(profile.id),
            recordType = HealthSyncRecordType.HEIGHT,
        )
        val syncDao = database.healthSyncOperationDao()
        val existingEntity = syncDao.findByNaturalKey(
            aggregateId = profile.id.value.toString(),
            aggregateType = HealthSyncAggregateType.USER_PROFILE_HEIGHT.name,
            recordType = HealthSyncRecordType.HEIGHT.name,
        )
        val existing = existingEntity?.toDomain()
        if (existingEntity != null && existing == null) return null
        if (existing?.state == SyncOperationState.SYNCING) return null

        if (profile.heightCentimeters == null) {
            val requestedVersion = profile.updatedAt.toEpochMilli().coerceAtLeast(0L)
            // Sem altura anterior enfileirada, não há identidade remota própria
            // que possa ser removida com segurança; não criar tombstone cego.
            if (existing == null) return null
            val operationVersion = maxOf(requestedVersion, existing.clientRecordVersion ?: 0L)
            val next = existing.prepareAggregateDeletion(
                clientRecordId = existing.clientRecordId ?: stableClientRecordId(key),
                clientRecordVersion = operationVersion,
                updatedAt = maxInstant(existing.updatedAt, profile.updatedAt),
            ).valueOrNull() ?: return null
            if (next != existing && syncDao.update(next.toEntity()) != 1) return null
            return ReconciledOperation(next, repaired = next != existing)
        }

        if (existing?.action == HealthSyncOperationAction.DELETE) {
            val currentVersion = existing.clientRecordVersion ?: -1L
            if (currentVersion == Long.MAX_VALUE) return null
            val next = existing.freezeProjection(
                action = HealthSyncOperationAction.INSERT,
                frozenPayload = null,
                clientRecordId = existing.clientRecordId ?: stableClientRecordId(key),
                clientRecordVersion = maxOf(profile.updatedAt.toEpochMilli().coerceAtLeast(0L), currentVersion + 1L),
                projectionOrigin = null,
                updatedAt = maxInstant(existing.updatedAt, profile.updatedAt),
            ).valueOrNull() ?: return null
            if (next != existing && syncDao.update(next.toEntity()) != 1) return null
            return ReconciledOperation(next, repaired = next != existing)
        }

        return ensureReconciledOperation(key)
    }

    private fun maxInstant(first: Instant?, second: Instant?): Instant? = listOfNotNull(first, second).maxOrNull()

    private fun monotonicProfileUpdatedAt(requested: Instant, existing: Instant?): Instant {
        val requestedEpochMillis = requested.toEpochMilli()
        val existingEpochMillis = existing?.toEpochMilli()
        val persistedEpochMillis = when {
            existingEpochMillis == null -> requestedEpochMillis
            requestedEpochMillis > existingEpochMillis -> requestedEpochMillis
            existingEpochMillis == Long.MAX_VALUE -> existingEpochMillis
            else -> existingEpochMillis + 1L
        }
        return Instant.ofEpochMilli(persistedEpochMillis)
    }

    override fun observeRecoverable(): Flow<List<AppWorkoutSession>> = database.appWorkoutSessionDao().observeRecoverable()
        .map { it.mapNotNull { entity -> entity.toDomain() } }
        .flowOn(ioDispatcher)

    override fun observeActive(): Flow<AppWorkoutSession?> = database.appWorkoutSessionDao().observeActive()
        .map { it?.toDomain() }
        .flowOn(ioDispatcher)

    override fun observeOpen(): Flow<AppWorkoutSession?> = database.appWorkoutSessionDao().observeOpen()
        .map { it?.toDomain() }
        .flowOn(ioDispatcher)

    override fun observeSession(sessionId: AppWorkoutSessionId): Flow<AppWorkoutSession?> =
        database.appWorkoutSessionDao().observeById(sessionId.value.toString())
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)

    override suspend fun loadOpenForRecovery(
        sessionId: AppWorkoutSessionId,
    ): RepositoryResult<AppWorkoutSession> = read {
        val entity = database.appWorkoutSessionDao().findById(sessionId.value.toString())
            ?: return@read RepositoryResult.NotFound
        val session = entity.toDomain() ?: return@read RepositoryResult.UnexpectedState
        if (session.state !in setOf(
                AppWorkoutSessionState.CREATED,
                AppWorkoutSessionState.ACTIVE,
                AppWorkoutSessionState.PAUSED,
                AppWorkoutSessionState.AWAITING_RECOVERY,
                AppWorkoutSessionState.FINISHING,
            )
        ) {
            return@read RepositoryResult.NotFound
        }
        RepositoryResult.Success(session)
    }

    override suspend fun create(session: AppWorkoutSession): RepositoryResult<AppWorkoutSession> = write {
        if (PhoneWorkoutPolicy.blocks(session)) return@write RepositoryResult.UnexpectedState
        if (session.state != br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionState.CREATED ||
            session.toEntity().toDomain() != session
        ) {
            return@write RepositoryResult.UnexpectedState
        }
        val dao = database.appWorkoutSessionDao()
        if (dao.findById(session.id.value.toString()) != null || dao.findOpenSession() != null) {
            return@write RepositoryResult.Conflict
        }
        val deviceId = session.associatedDeviceId?.value?.toString()
        if (deviceId != null && database.deviceDao().findById(deviceId) == null) {
            return@write RepositoryResult.NotFound
        }
        dao.insert(session.toEntity())
        RepositoryResult.Success(session)
    }

    override suspend fun createStarted(
        session: AppWorkoutSession,
        startedAt: Instant,
    ): RepositoryResult<AppWorkoutSession> = write {
        if (PhoneWorkoutPolicy.blocks(session)) return@write RepositoryResult.UnexpectedState
        if (session.state != br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionState.CREATED ||
            session.toEntity().toDomain() != session
        ) {
            return@write RepositoryResult.UnexpectedState
        }
        val started = session.start(startedAt).valueOrNull()
            ?: return@write RepositoryResult.UnexpectedState
        if (started.toEntity().toDomain() != started) return@write RepositoryResult.UnexpectedState
        val dao = database.appWorkoutSessionDao()
        if (dao.findById(session.id.value.toString()) != null || dao.findOpenSession() != null) {
            return@write RepositoryResult.Conflict
        }
        val deviceId = session.associatedDeviceId?.value?.toString()
        if (deviceId != null && database.deviceDao().findById(deviceId) == null) {
            return@write RepositoryResult.NotFound
        }
        dao.insert(started.toEntity())
        RepositoryResult.Success(started)
    }

    override suspend fun start(input: AppWorkoutSessionMutation): RepositoryResult<AppWorkoutSession> = write {
        if (PhoneWorkoutPolicy.isDiscontinued()) return@write RepositoryResult.UnexpectedState
        mutateSession(input.sessionId, input.expectedRevision) { it.start(input.at) }
    }

    override suspend fun checkpoint(input: AppWorkoutSessionCheckpoint): RepositoryResult<AppWorkoutSession> = write {
        if (PhoneWorkoutPolicy.isDiscontinued()) return@write RepositoryResult.UnexpectedState
        mutateSession(input.sessionId, input.expectedRevision) { session ->
            if (session.runtimeGeneration != input.runtimeGeneration) {
                DomainResult.Failure(br.com.mwconsulting.championcompanion.core.model.DomainValidationError.INVALID_STATE)
            } else if (input.activeDuration != null && input.pausedDuration != null) {
                session.checkpoint(
                    at = input.at,
                    activeDuration = input.activeDuration,
                    pausedDuration = input.pausedDuration,
                    aggregates = input.aggregates,
                )
            } else {
                session.checkpoint(input.at, input.aggregates)
            }
        }
    }

    override suspend fun pause(input: AppWorkoutSessionMutation): RepositoryResult<AppWorkoutSession> = write {
        if (PhoneWorkoutPolicy.isDiscontinued()) return@write RepositoryResult.UnexpectedState
        mutateSession(input.sessionId, input.expectedRevision) {
            if (input.activeDuration != null && input.pausedDuration != null) {
                it.pause(input.at, input.activeDuration, input.pausedDuration)
            } else {
                it.pause(input.at)
            }
        }
    }

    override suspend fun resume(input: AppWorkoutSessionMutation): RepositoryResult<AppWorkoutSession> = write {
        if (PhoneWorkoutPolicy.isDiscontinued()) return@write RepositoryResult.UnexpectedState
        mutateSession(input.sessionId, input.expectedRevision) {
            if (input.activeDuration != null && input.pausedDuration != null) {
                it.resume(input.at, input.activeDuration, input.pausedDuration)
            } else {
                it.resume(input.at)
            }
        }
    }

    override suspend fun interrupt(input: AppWorkoutSessionMutation): RepositoryResult<AppWorkoutSession> = write {
        if (PhoneWorkoutPolicy.isDiscontinued()) return@write RepositoryResult.UnexpectedState
        mutateSession(input.sessionId, input.expectedRevision) {
            if (input.activeDuration != null && input.pausedDuration != null) {
                it.interrupt(input.at, input.activeDuration, input.pausedDuration)
            } else {
                it.interrupt(input.at)
            }
        }
    }

    override suspend fun recover(
        sessionId: AppWorkoutSessionId,
        expectedRevision: Long,
        at: Instant,
    ): RepositoryResult<AppWorkoutSession> = write {
        if (PhoneWorkoutPolicy.isDiscontinued()) return@write RepositoryResult.UnexpectedState
        mutateSession(sessionId, expectedRevision) { it.recover(at) }
    }

    override suspend fun beginFinishing(
        input: AppWorkoutSessionMutation,
    ): RepositoryResult<AppWorkoutSession> = write {
        if (PhoneWorkoutPolicy.isDiscontinued()) return@write RepositoryResult.UnexpectedState
        mutateSession(input.sessionId, input.expectedRevision) {
            if (input.activeDuration != null && input.pausedDuration != null) {
                it.beginFinishing(input.at, input.activeDuration, input.pausedDuration)
            } else {
                it.beginFinishing(input.at)
            }
        }
    }

    override suspend fun associateActivity(
        input: AssociateWorkoutActivityInput,
    ): RepositoryResult<AppWorkoutSession> = write {
        if (PhoneWorkoutPolicy.isDiscontinued()) return@write RepositoryResult.UnexpectedState
        val session = database.appWorkoutSessionDao().findById(input.sessionId.value.toString())
            ?: return@write RepositoryResult.NotFound
        val activity = database.workoutActivityDao().findById(input.activityId.value.toString())
            ?: return@write RepositoryResult.NotFound
        if (!hasConsistentPhoneActivityOwnership(session, activity)) {
            return@write RepositoryResult.Conflict
        }
        mutateSession(input.sessionId, input.expectedRevision) { it.associateActivity(input.activityId) }
    }

    override suspend fun finish(
        sessionId: AppWorkoutSessionId,
        expectedRevision: Long,
        at: Instant,
    ): RepositoryResult<AppWorkoutSession> = write {
        if (PhoneWorkoutPolicy.isDiscontinued()) return@write RepositoryResult.UnexpectedState
        val session = database.appWorkoutSessionDao().findById(sessionId.value.toString())
            ?: return@write RepositoryResult.NotFound
        val activityId = session.workoutActivityId ?: return@write RepositoryResult.UnexpectedState
        val activity = database.workoutActivityDao().findById(activityId)
            ?: return@write RepositoryResult.NotFound
        if (!hasConsistentPhoneActivityOwnership(session, activity)) {
            return@write RepositoryResult.Conflict
        }
        mutateSession(sessionId, expectedRevision) { it.finish(at) }
    }

    override suspend fun discard(input: AppWorkoutSessionMutation): RepositoryResult<AppWorkoutSession> = write {
        val sessionId = input.sessionId.value.toString()
        val session = database.appWorkoutSessionDao().findById(sessionId)
            ?: return@write RepositoryResult.NotFound
        val domainSession = session.toDomain() ?: return@write RepositoryResult.UnexpectedState
        if (PhoneWorkoutPolicy.isDiscontinued() &&
            domainSession.state == AppWorkoutSessionState.FINISHING
        ) {
            return@write RepositoryResult.UnexpectedState
        }
        if (session.workoutActivityId != null || database.workoutActivityDao().findBySessionId(sessionId) != null) {
            return@write RepositoryResult.Conflict
        }
        val result = mutateSession(input.sessionId, input.expectedRevision) { it.discard(input.at) }
        if (result is RepositoryResult.Success) {
            database.heartRateDao().deleteTransientForSession(sessionId)
            database.workoutRouteDao().deleteBySessionId(sessionId)
        }
        result
    }

    @Suppress("DEPRECATION_ERROR")
    override suspend fun recover(sessionId: AppWorkoutSessionId, at: Instant): RepositoryResult<AppWorkoutSession> = write {
        if (PhoneWorkoutPolicy.isDiscontinued()) return@write RepositoryResult.UnexpectedState
        val entity = database.appWorkoutSessionDao().findById(sessionId.value.toString())
            ?: return@write RepositoryResult.NotFound
        mutateSession(sessionId, entity.revision) { it.recover(at) }
    }

    @Suppress("DEPRECATION_ERROR")
    override suspend fun finish(sessionId: AppWorkoutSessionId, activityId: WorkoutActivityId, at: Instant): RepositoryResult<AppWorkoutSession> = write {
        if (PhoneWorkoutPolicy.isDiscontinued()) return@write RepositoryResult.UnexpectedState
        val activity = database.workoutActivityDao().findById(activityId.value.toString())
            ?: return@write RepositoryResult.NotFound
        val entity = database.appWorkoutSessionDao().findById(sessionId.value.toString())
            ?: return@write RepositoryResult.NotFound
        if (!hasConsistentPhoneActivityOwnership(entity, activity)) return@write RepositoryResult.Conflict
        mutateSession(sessionId, entity.revision) { it.finish(activityId, at) }
    }

    override suspend fun create(route: WorkoutRoute): RepositoryResult<WorkoutRoute> = write {
        if (PhoneWorkoutPolicy.isDiscontinued()) return@write RepositoryResult.UnexpectedState
        val canonicalRoute = WorkoutRoute.create(
            id = route.id,
            sessionId = route.sessionId,
            workoutType = route.workoutType,
            startedAt = route.startedAt,
        ).valueOrNull() ?: return@write RepositoryResult.UnexpectedState
        if (route != canonicalRoute || canonicalRoute.toEntity().toDomain() != canonicalRoute) {
            return@write RepositoryResult.UnexpectedState
        }
        val sessionEntity = database.appWorkoutSessionDao().findById(canonicalRoute.sessionId.value.toString())
            ?: return@write RepositoryResult.NotFound
        val session = sessionEntity.toDomain() ?: return@write RepositoryResult.UnexpectedState
        if (session.startedAt == null || session.startedAt != canonicalRoute.startedAt) {
            return@write RepositoryResult.UnexpectedState
        }
        val routeDao = database.workoutRouteDao()
        if (session.type != canonicalRoute.workoutType || !session.recordsRoute || session.workoutActivityId != null ||
            session.state != br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionState.ACTIVE ||
            routeDao.findById(canonicalRoute.id.value.toString()) != null ||
            routeDao.findBySessionId(sessionEntity.id) != null
        ) {
            return@write RepositoryResult.Conflict
        }
        routeDao.insert(canonicalRoute.toEntity())
        RepositoryResult.Success(canonicalRoute)
    }

    override fun observeBySession(sessionId: AppWorkoutSessionId): Flow<WorkoutRoute?> =
        database.workoutRouteDao().observeBySessionId(sessionId.value.toString())
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)

    override suspend fun loadRecoverySnapshot(
        sessionId: AppWorkoutSessionId,
    ): RepositoryResult<WorkoutRouteRecoverySnapshot?> = read {
        val dao = database.workoutRouteDao()
        val routeEntity = dao.findBySessionId(sessionId.value.toString())
            ?: return@read RepositoryResult.Success(null)
        val route = routeEntity.toDomain() ?: return@read RepositoryResult.UnexpectedState
        val snapshot = restoreWorkoutRouteRecoverySnapshot(
            route = route,
            pointEntities = dao.findPoints(routeEntity.id),
        ) ?: return@read RepositoryResult.UnexpectedState
        RepositoryResult.Success(snapshot)
    }

    override suspend fun appendAcceptedPoints(
        input: AppendWorkoutRoutePointsInput,
    ): RepositoryResult<WorkoutRoute> = write {
        if (PhoneWorkoutPolicy.isDiscontinued()) return@write RepositoryResult.UnexpectedState
        val routeDao = database.workoutRouteDao()
        val routeEntity = routeDao.findById(input.routeId.value.toString())
            ?: return@write RepositoryResult.NotFound
        val route = routeEntity.toDomain() ?: return@write RepositoryResult.UnexpectedState
        if (route.sessionId != input.sessionId || route.revision != input.expectedRevision) {
            return@write RepositoryResult.Conflict
        }
        val session = database.appWorkoutSessionDao().findById(input.sessionId.value.toString())?.toDomain()
            ?: return@write RepositoryResult.NotFound
        if (session.runtimeGeneration != input.runtimeGeneration ||
            session.state != br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionState.ACTIVE
        ) {
            return@write RepositoryResult.Conflict
        }
        val sessionStartedAt = session.startedAt ?: return@write RepositoryResult.UnexpectedState
        if (session.type != route.workoutType || !session.recordsRoute || sessionStartedAt != route.startedAt) {
            return@write RepositoryResult.UnexpectedState
        }
        val lastPoint = routeDao.findLastPoint(routeEntity.id)
        val persistedSegmentStart = lastPoint?.let {
            routeDao.findFirstPointInSegment(routeEntity.id, it.segmentIndex)
        }
        val persistedPointCount = routeDao.countPoints(routeEntity.id)
        if (lastPoint?.sequence != input.cursor.lastSequence ||
            lastPoint?.segmentIndex != input.cursor.currentSegmentIndex ||
            !isValidRouteAppend(
                route = route,
                sessionStartedAt = sessionStartedAt,
                input = input,
                lastPoint = lastPoint,
                persistedSegmentStart = persistedSegmentStart,
                persistedPointCount = persistedPointCount,
            )
        ) {
            return@write RepositoryResult.Conflict
        }
        val updated = route.applyCheckpoint(input.expectedRevision, input.metrics).valueOrNull()
            ?: return@write RepositoryResult.UnexpectedState
        if (routeDao.updateCas(updated.toEntity(), input.expectedRevision) != 1) {
            return@write RepositoryResult.Conflict
        }
        val segmentsByIndex = input.segments.associateBy { it.index }
        routeDao.insertPoints(input.points.map { point ->
            val segment = segmentsByIndex.getValue(point.segmentIndex)
            val isSegmentStart = point.sequence == segment.firstSequence
            val isSegmentCheckpoint = point.sequence == segment.lastSequence
            point.toEntity(
                segmentStartReason = segment.reason.takeIf { isSegmentStart },
                segmentStartedAt = segment.startedAt.takeIf { isSegmentStart },
                segmentDistanceMeters = segment.distanceMeters.takeIf { isSegmentCheckpoint },
            )
        })
        RepositoryResult.Success(updated)
    }

    override suspend fun finalize(
        routeId: WorkoutRouteId,
        expectedRevision: Long,
        endedAt: Instant,
    ): RepositoryResult<WorkoutRoute> = write {
        if (PhoneWorkoutPolicy.isDiscontinued()) return@write RepositoryResult.UnexpectedState
        val dao = database.workoutRouteDao()
        val route = dao.findById(routeId.value.toString())?.toDomain()
            ?: return@write RepositoryResult.NotFound
        if (route.revision != expectedRevision) return@write RepositoryResult.Conflict
        val updated = route.finalize(expectedRevision, endedAt).valueOrNull()
            ?: return@write RepositoryResult.UnexpectedState
        if (updated == route) return@write RepositoryResult.Success(route)
        if (dao.updateCas(updated.toEntity(), expectedRevision) != 1) RepositoryResult.Conflict
        else RepositoryResult.Success(updated)
    }

    override suspend fun deleteBeforeActivity(sessionId: AppWorkoutSessionId): RepositoryResult<Unit> = write {
        if (PhoneWorkoutPolicy.isDiscontinued()) return@write RepositoryResult.UnexpectedState
        val session = database.appWorkoutSessionDao().findById(sessionId.value.toString())
            ?: return@write RepositoryResult.NotFound
        if (session.workoutActivityId != null ||
            database.workoutActivityDao().findBySessionId(session.id) != null
        ) return@write RepositoryResult.Conflict
        database.heartRateDao().deleteTransientForSession(session.id)
        database.workoutRouteDao().deleteBySessionId(session.id)
        RepositoryResult.Success(Unit)
    }

    override suspend fun ensureOperation(key: HealthSyncOperationKey): RepositoryResult<HealthSyncOperation> = write {
        ensureOperationInternal(key)
    }

    override suspend fun ensureDailyStepsCutover(
        deviceId: DeviceId,
        activationLocalDate: LocalDate,
        zoneId: ZoneId,
        activatedAt: Instant,
    ): RepositoryResult<HealthSyncCutover> = write {
        when (val result = ensureDailyStepsCutoverInternal(deviceId, activationLocalDate, zoneId, activatedAt)) {
            is RepositoryResult.Success -> result
            RepositoryResult.Conflict -> RepositoryResult.Conflict
            RepositoryResult.UnexpectedState -> RepositoryResult.UnexpectedState
            RepositoryResult.NotFound -> RepositoryResult.NotFound
            RepositoryResult.StorageUnavailable -> RepositoryResult.StorageUnavailable
        }
    }

    override suspend fun findDailyStepsCutover(
        deviceId: DeviceId,
    ): RepositoryResult<HealthSyncCutover?> = read {
        val entity = database.healthSyncCutoverDao().findByDeviceId(deviceId.value.toString())
        if (entity == null) RepositoryResult.Success(null)
        else entity.toDomain()?.let { RepositoryResult.Success(it) } ?: RepositoryResult.UnexpectedState
    }

    private suspend fun ensureDailyStepsCutoverInternal(
        deviceId: DeviceId,
        activationLocalDate: LocalDate,
        zoneId: ZoneId,
        activatedAt: Instant,
    ): RepositoryResult<HealthSyncCutover> {
        val deviceIdValue = deviceId.value.toString()
        if (database.deviceDao().findById(deviceIdValue) == null) {
            return RepositoryResult.NotFound
        }
        val dao = database.healthSyncCutoverDao()
        val existing = dao.findByDeviceId(deviceIdValue)
        if (existing != null) {
            return existing.toDomain()?.let { RepositoryResult.Success(it) }
                ?: RepositoryResult.UnexpectedState
        }

        val hasLegacyDailySummary = database.dailySummaryDao().findValidWatchBacklog()
            .any { it.deviceId == deviceIdValue }
        val cutoverDate = runCatching {
            if (hasLegacyDailySummary) activationLocalDate.plusDays(1L) else activationLocalDate
        }.getOrNull() ?: return RepositoryResult.UnexpectedState
        val entity = HealthSyncCutoverEntity(
            id = ids.next().toString(),
            deviceId = deviceIdValue,
            localDateIso = cutoverDate.toString(),
            zoneId = zoneId.id,
            activatedAtEpochMillis = activatedAt.toEpochMilli(),
        )
        dao.insert(entity)
        return entity.toDomain()?.let { RepositoryResult.Success(it) } ?: RepositoryResult.UnexpectedState
    }

    override suspend fun prepareProjectionChild(
        key: HealthSyncOperationKey,
        action: HealthSyncOperationAction,
        frozenPayload: HealthSyncFrozenPayload,
        clientRecordVersion: Long,
        projectionOrigin: HealthProjectionOrigin,
        workoutStepsContext: WorkoutStepsProjectionContext?,
    ): RepositoryResult<HealthSyncOperation> = try {
        write {
            if (key.projectionChildKey == null || key.aggregate !is HealthSyncAggregate.WorkoutActivity) {
                return@write RepositoryResult.UnexpectedState
            }
            if (key.projectionChildKey.recordTypeName != key.recordType.name) {
                return@write RepositoryResult.UnexpectedState
            }
            if (clientRecordVersion < 0L) return@write RepositoryResult.UnexpectedState
            if (key.projectionChildKey == HealthProjectionChildKey.WorkoutSteps && workoutStepsContext == null) {
                throw WorkoutStepsPreparationAborted()
            }

            val metadata = resolveSyncMetadata(key) ?: return@write RepositoryResult.NotFound
            val preparedMetadata = metadata.copy(clientRecordVersion = clientRecordVersion)
            val syncDao = database.healthSyncOperationDao()
            val existing = syncDao.findByNaturalKey(
                aggregateId = key.aggregate.id.toString(),
                aggregateType = key.aggregate.type.name,
                recordType = key.recordType.name,
                projectionChildKey = key.projectionChildKey.stableValue,
            )

            val next = if (existing == null) {
                val entity = newPendingOperationEntity(key, preparedMetadata).copy(
                    action = action.name,
                    frozenPayload = frozenPayload.serializedPayload,
                    frozenPayloadVersion = frozenPayload.schemaVersion,
                    projectionOriginId = projectionOrigin.originId,
                    projectionOriginVersion = projectionOrigin.originVersion,
                )
                val operation = entity.toDomain() ?: return@write RepositoryResult.UnexpectedState
                syncDao.insert(entity)
                operation
            } else {
                val current = existing.toDomain() ?: return@write RepositoryResult.UnexpectedState
                val projectionChanged = current.action != action ||
                    current.frozenPayload != frozenPayload ||
                    current.projectionOrigin != projectionOrigin
                val nextClientRecordVersion = if (projectionChanged) {
                    val currentVersion = current.clientRecordVersion ?: -1L
                    if (currentVersion == Long.MAX_VALUE) return@write RepositoryResult.UnexpectedState
                    maxOf(clientRecordVersion, currentVersion + 1L)
                } else {
                    current.clientRecordVersion ?: clientRecordVersion
                }
                val prepared = current.freezeProjection(
                    action = action,
                    frozenPayload = frozenPayload,
                    clientRecordId = stableClientRecordId(key),
                    clientRecordVersion = nextClientRecordVersion,
                    projectionOrigin = projectionOrigin,
                    updatedAt = preparedMetadata.updatedAtEpochMillis?.let(Instant::ofEpochMilli),
                ).valueOrNull() ?: return@write RepositoryResult.UnexpectedState
                if (prepared != current && syncDao.update(prepared.toEntity()) != 1) {
                    return@write RepositoryResult.UnexpectedState
                }
                prepared
            }

            if (key.projectionChildKey == HealthProjectionChildKey.WorkoutSteps &&
                !prepareWorkoutStepsCredit(
                    operation = next,
                    context = workoutStepsContext ?: throw WorkoutStepsPreparationAborted(),
                )
            ) {
                throw WorkoutStepsPreparationAborted()
            }
            RepositoryResult.Success(next)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: WorkoutStepsPreparationAborted) {
        RepositoryResult.UnexpectedState
    }

    override suspend fun findWorkoutStepsCredit(
        operationId: HealthSyncOperationId,
    ): RepositoryResult<WorkoutStepsCredit?> = read {
        val entity = database.workoutStepsCreditDao()
            .findByWorkoutStepsOperationId(operationId.value.toString())
            ?: return@read RepositoryResult.Success(null)
        val credit = entity.toDomain() ?: return@read RepositoryResult.UnexpectedState
        RepositoryResult.Success(credit)
    }

    /** Prepara crédito ou bloqueio local antes de qualquer efeito remoto. */
    private suspend fun prepareWorkoutStepsCredit(
        operation: HealthSyncOperation,
        context: WorkoutStepsProjectionContext,
    ): Boolean {
        val aggregate = operation.key.aggregate as? HealthSyncAggregate.WorkoutActivity ?: return false
        if (operation.key != HealthSyncOperationKey(
                aggregate = aggregate,
                recordType = HealthSyncRecordType.STEPS,
                projectionChildKey = HealthProjectionChildKey.WorkoutSteps,
            ) || operation.action != HealthSyncOperationAction.INSERT
        ) return false

        val activity = database.workoutActivityDao().findById(aggregate.activityId.value.toString()) ?: return false
        if (activity.origin != WorkoutActivityOrigin.WATCH_D3_D4.name ||
            activity.deviceId == null ||
            activity.parserValidity != ParserValidity.VALID.name ||
            activity.workflowState == ActivityWorkflowState.DISCARDED.name ||
            activity.confirmedType == null ||
            activity.confirmedType == WorkoutType.UNKNOWN.name ||
            activity.confirmedType == WorkoutType.CYCLING.name ||
            !activity.stepsAvailable ||
            activity.steps != context.steps
        ) return false

        val zoneId = operation.timeZoneId
            .validZoneIdOrNull()
            ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: return false
        val windowEndInclusive = runCatching { context.window.end.minusNanos(1L) }.getOrNull() ?: return false
        val localDate = context.window.start.atZone(zoneId).toLocalDate()
        if (windowEndInclusive.atZone(zoneId).toLocalDate() != localDate ||
            context.window.start.toEpochMilli() >= context.window.end.toEpochMilli()
        ) return false

        val identity = WorkoutStepsProjectionIdentity.fromOperation(operation).valueOrNull() ?: return false
        val creditDao = database.workoutStepsCreditDao()
        val clientRecordId = operation.clientRecordId
        val clientRecordCredit = if (clientRecordId == null) {
            null
        } else {
            creditDao.findByWorkoutStepsClientRecordId(clientRecordId)
        }
        val candidates = listOfNotNull(
            creditDao.findByActivityAndLocalDate(activity.id, localDate.toString()),
            creditDao.findByWorkoutStepsOperationId(operation.id.value.toString()),
            clientRecordCredit,
        ).distinctBy { it.id }
        if (candidates.size > 1) return false
        val existingEntity = candidates.singleOrNull()
        val existing = existingEntity?.toDomain()
        if (existingEntity != null && existing == null) return false
        val now = Instant.now()
        val operationAccepted = operation.state == SyncOperationState.SYNCED &&
            operation.action == HealthSyncOperationAction.INSERT &&
            operation.reconciliationState == HealthProjectionReconciliationState.SENT &&
            operation.reconciliationCheckpoint == HealthProjectionReconciliationCheckpoint.AFTER_REMOTE_EFFECT

        if (operationAccepted) {
            return activateOrRecoverWorkoutStepsCredit(
                existing = existing,
                identity = identity,
                context = context,
                localDate = localDate,
                zoneId = zoneId,
                at = now,
                creditDao = creditDao,
            )
        }

        if (existing?.state == WorkoutStepsCreditState.ACTIVE ||
            existing?.state == WorkoutStepsCreditState.CONSUMED
        ) return false

        val blockReason = workoutStepsDailyCoverageBlockReason(
            activity = activity,
            localDate = localDate,
            window = context.window,
            zoneId = zoneId,
        )
        val prepared = (if (existing == null) {
            createWorkoutStepsCredit(
                id = WorkoutStepsCreditId(ids.next()),
                identity = identity,
                context = context,
                localDate = localDate,
                zoneId = zoneId,
                createdAt = now,
                updatedAt = now,
            )
        } else if (existing.projectionIdentity.clientRecordId != identity.clientRecordId ||
            existing.projectionIdentity.clientRecordVersion != identity.clientRecordVersion ||
            existing.steps != context.steps ||
            existing.window != context.window ||
            existing.timeZoneId != zoneId ||
            existing.localDate != localDate
        ) {
            if (existing.state == WorkoutStepsCreditState.ACTIVE ||
                existing.state == WorkoutStepsCreditState.CONSUMED
            ) return false
            createWorkoutStepsCredit(
                id = existing.id,
                identity = identity,
                context = context,
                localDate = localDate,
                zoneId = zoneId,
                createdAt = existing.createdAt,
                updatedAt = now,
            )
        } else {
            val retry = existing.prepareForRetry(now).valueOrNull()
            if (retry != null) {
                retry
            } else if (existing.state == WorkoutStepsCreditState.BLOCKED &&
                existing.blockedReason == WorkoutStepsCreditBlockReason.ALREADY_COVERED
            ) {
                existing
            } else {
                return false
            }
        }) ?: return false

        val next = if (blockReason == null) {
            prepared
        } else if (prepared.state == WorkoutStepsCreditState.BLOCKED &&
            prepared.blockedReason == blockReason
        ) {
            prepared
        } else {
            prepared.block(blockReason, now).valueOrNull() ?: return false
        }
        return if (existing == null) {
            creditDao.insert(next.toEntity())
            true
        } else {
            creditDao.update(next.toEntity()) == 1
        }
    }

    private suspend fun activateOrRecoverWorkoutStepsCredit(
        existing: WorkoutStepsCredit?,
        identity: WorkoutStepsProjectionIdentity,
        context: WorkoutStepsProjectionContext,
        localDate: LocalDate,
        zoneId: ZoneId,
        at: Instant,
        creditDao: br.com.mwconsulting.championcompanion.core.database.WorkoutStepsCreditDao,
    ): Boolean {
        if (existing?.projectionIdentity?.clientRecordId == identity.clientRecordId &&
            existing.projectionIdentity.clientRecordVersion == identity.clientRecordVersion &&
            existing.steps == context.steps &&
            existing.window == context.window &&
            existing.timeZoneId == zoneId &&
            existing.localDate == localDate
        ) {
            if (existing.state == WorkoutStepsCreditState.ACTIVE ||
                existing.state == WorkoutStepsCreditState.CONSUMED
            ) return true
        } else if (existing?.state == WorkoutStepsCreditState.ACTIVE ||
            existing?.state == WorkoutStepsCreditState.CONSUMED
        ) return false

        val pending = createWorkoutStepsCredit(
            id = existing?.id ?: WorkoutStepsCreditId(ids.next()),
            identity = identity,
            context = context,
            localDate = localDate,
            zoneId = zoneId,
            createdAt = existing?.createdAt ?: at,
            updatedAt = at,
        ) ?: return false
        val active = pending
            .markAccepted(at)
            .valueOrNull()
            ?.markActive(at)
            ?.valueOrNull()
            ?: return false
        return if (existing == null) {
            creditDao.insert(active.toEntity())
            true
        } else {
            creditDao.update(active.toEntity()) == 1
        }
    }

    private fun createWorkoutStepsCredit(
        id: WorkoutStepsCreditId,
        identity: WorkoutStepsProjectionIdentity,
        context: WorkoutStepsProjectionContext,
        localDate: LocalDate,
        zoneId: ZoneId,
        createdAt: Instant,
        updatedAt: Instant,
    ): WorkoutStepsCredit? = WorkoutStepsCredit.create(
        id = id,
        activityId = identity.activityId,
        localDate = localDate,
        steps = context.steps,
        window = context.window,
        timeZoneId = zoneId,
        projectionIdentity = identity,
        createdAt = createdAt,
        updatedAt = updatedAt,
    ).valueOrNull()

    private suspend fun workoutStepsDailyCoverageBlockReason(
        activity: WorkoutActivityEntity,
        localDate: LocalDate,
        window: br.com.mwconsulting.championcompanion.core.model.WorkoutIntervalWindow,
        zoneId: ZoneId,
    ): WorkoutStepsCreditBlockReason? {
        val summary = database.dailySummaryDao().findByNaturalKey(
            deviceId = activity.deviceId,
            origin = DataOrigin.WATCH_DAILY_SUMMARY.name,
            localDateIso = localDate.toString(),
        ) ?: return null
        val operations = database.healthSyncOperationDao().findByAggregate(
            aggregateId = summary.id,
            aggregateType = HealthSyncAggregateType.DAILY_SUMMARY.name,
        ).filter { it.recordType == HealthSyncRecordType.STEPS.name }
        if (operations.isEmpty()) return null
        if (summary.origin != DataOrigin.WATCH_DAILY_SUMMARY.name ||
            summary.validity != DataValidity.VALID.name ||
            summary.deviceId != activity.deviceId
        ) return WorkoutStepsCreditBlockReason.COVERAGE_UNKNOWN

        val summaryDate = runCatching { LocalDate.parse(summary.localDateIso) }.getOrNull()
            ?: return WorkoutStepsCreditBlockReason.COVERAGE_UNKNOWN
        val summaryZone = summary.collectedZoneId.validZoneIdOrNull()
            ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: return WorkoutStepsCreditBlockReason.COVERAGE_UNKNOWN
        if (summaryDate != localDate || summaryZone != zoneId) {
            return WorkoutStepsCreditBlockReason.COVERAGE_UNKNOWN
        }
        if (operations.any { it.action != HealthSyncOperationAction.INSERT.name }) {
            return WorkoutStepsCreditBlockReason.COVERAGE_UNKNOWN
        }
        val payloadOperations = operations
            .sortedBy { it.operationSequence }
            .map { entity ->
                val operation = entity.toDomain() ?: return WorkoutStepsCreditBlockReason.COVERAGE_UNKNOWN
                val payload = operation.dailyStepsPayload
                    ?: return WorkoutStepsCreditBlockReason.COVERAGE_UNKNOWN
                entity to payload
            }
        val checkpoint = activity.deviceId?.let { deviceId ->
            database.dailyStepsCheckpointDao().findByNaturalKey(
                deviceId = deviceId,
                origin = summary.origin,
                localDateIso = summary.localDateIso,
                timeZoneId = summaryZone.id,
            )
        }
        if (
            !payloadOperations.isValidDailyStepsChain(allowPositiveFirstPreviousTotal = checkpoint != null) ||
                ((payloadOperations.firstOrNull()?.second?.previousTotalSteps ?: 0L) > 0L && checkpoint == null) ||
                (checkpoint != null && payloadOperations.isNotEmpty() &&
                    checkpoint.effectiveTotalSteps < payloadOperations.last().second.currentTotalSteps)
        ) {
            return WorkoutStepsCreditBlockReason.COVERAGE_UNKNOWN
        }
        for ((entity, payload) in payloadOperations) {
            if (entity.projectionChildKey != HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY) {
                return WorkoutStepsCreditBlockReason.COVERAGE_UNKNOWN
            }
            val operation = entity.toDomain() ?: return WorkoutStepsCreditBlockReason.COVERAGE_UNKNOWN
            val payloadZone = payload.timeZoneId.validZoneIdOrNull()
                ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
                ?: return WorkoutStepsCreditBlockReason.COVERAGE_UNKNOWN
            val payloadEndInclusive = runCatching { payload.endTime.minusNanos(1L) }.getOrNull()
                ?: return WorkoutStepsCreditBlockReason.COVERAGE_UNKNOWN
            if (payloadZone != summaryZone ||
                operation.timeZoneId.validZoneIdOrNull()?.let { runCatching { ZoneId.of(it) }.getOrNull() } != payloadZone ||
                payload.startTime.atZone(payloadZone).toLocalDate() != summaryDate ||
                payloadEndInclusive.atZone(payloadZone).toLocalDate() != summaryDate
            ) return WorkoutStepsCreditBlockReason.COVERAGE_UNKNOWN
            if (payload.currentTotalSteps > summary.steps) {
                return WorkoutStepsCreditBlockReason.COVERAGE_UNKNOWN
            }
            val operationAccepted = operation.state == SyncOperationState.SYNCED &&
                operation.reconciliationState == HealthProjectionReconciliationState.SENT &&
                operation.reconciliationCheckpoint == HealthProjectionReconciliationCheckpoint.AFTER_REMOTE_EFFECT
            if (!operationAccepted) {
                return WorkoutStepsCreditBlockReason.COVERAGE_UNKNOWN
            }
            if (window.start.isBefore(payload.endTime) && payload.startTime.isBefore(window.end)) {
                return WorkoutStepsCreditBlockReason.ALREADY_COVERED
            }
        }
        return null
    }

    /** Confirma operação e crédito no mesmo commit Room após `Accepted`. */
    private suspend fun activateWorkoutStepsCreditForAcceptedOperation(
        operationEntity: HealthSyncOperationEntity,
        at: Instant,
    ): Boolean {
        val operation = operationEntity.toDomain() ?: return false
        if (operation.key.aggregate !is HealthSyncAggregate.WorkoutActivity) return false
        val identity = WorkoutStepsProjectionIdentity.fromOperation(operation).valueOrNull() ?: return false
        val zoneId = operation.timeZoneId
            .validZoneIdOrNull()
            ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: return false
        val payload = operation.frozenPayload
            ?.takeIf { it.schemaVersion == HEALTH_SYNC_FROZEN_PROJECTION_PAYLOAD_WORKOUT_STEPS_SCHEMA_VERSION }
            ?.let { runCatching { frozenProjectionJson.decodeFromString<HealthSyncFrozenProjectionPayload>(it.serializedPayload) }.getOrNull() }
            ?: return false
        if (payload.projectionChildKey != HealthProjectionChildKey.WorkoutSteps.stableValue ||
            payload.recordType != HealthSyncRecordType.STEPS.name ||
            payload.steps == null ||
            payload.steps <= 0L
        ) return false
        val start = payload.start.toInstantOrNull() ?: return false
        val end = payload.end.toInstantOrNull() ?: return false
        val window = WorkoutIntervalWindow.create(start, end).valueOrNull() ?: return false
        val context = WorkoutStepsProjectionContext(payload.steps, window)
        val localDate = start.atZone(zoneId).toLocalDate()
        val existingEntity = database.workoutStepsCreditDao()
            .findByWorkoutStepsOperationId(operation.id.value.toString())
        val existing = existingEntity?.toDomain() ?: if (existingEntity == null) null else return false
        return activateOrRecoverWorkoutStepsCredit(
            existing = existing,
            identity = identity,
            context = context,
            localDate = localDate,
            zoneId = zoneId,
            at = at,
            creditDao = database.workoutStepsCreditDao(),
        )
    }

    override suspend fun inspectLegacyWorkoutDistanceProjection(
        activityId: WorkoutActivityId,
        activityOrigin: WorkoutActivityOrigin,
    ): RepositoryResult<HealthLegacyDistanceProjectionStatus> = withContext(ioDispatcher) {
        if (activityOrigin != WorkoutActivityOrigin.WATCH_D3_D4) {
            return@withContext RepositoryResult.Success(HealthLegacyDistanceProjectionStatus.ABSENT)
        }
        val entity = database.healthSyncOperationDao().findKnownLegacyWorkoutDistanceProjection(
            activityId = activityId.value.toString(),
            activityOrigin = WorkoutActivityOrigin.WATCH_D3_D4.name,
        ) ?: return@withContext RepositoryResult.Success(HealthLegacyDistanceProjectionStatus.ABSENT)
        val key = HealthSyncOperationKey(
            aggregate = HealthSyncAggregate.WorkoutActivity(activityId),
            recordType = HealthSyncRecordType.DISTANCE,
        )
        val expectedClientRecordId = stableClientRecordId(key)
        if (entity.clientRecordId.isNullOrBlank() || entity.clientRecordId != expectedClientRecordId) {
            return@withContext RepositoryResult.Success(HealthLegacyDistanceProjectionStatus.UNKNOWN_ID)
        }
        val action = enumValueOfOrNull<HealthSyncOperationAction>(entity.action)
        val state = enumValueOfOrNull<SyncOperationState>(entity.state)
        val reconciliationState = enumValueOfOrNull<HealthProjectionReconciliationState>(entity.reconciliationState)
        val checkpoint = enumValueOfOrNull<HealthProjectionReconciliationCheckpoint>(entity.reconciliationCheckpoint)
        if (action == null || state == null || reconciliationState == null || checkpoint == null) {
            return@withContext RepositoryResult.Success(HealthLegacyDistanceProjectionStatus.UNKNOWN_ID)
        }
        when {
            action == HealthSyncOperationAction.DELETE &&
                state == SyncOperationState.SYNCED &&
                reconciliationState == HealthProjectionReconciliationState.OBSOLETE &&
                checkpoint == HealthProjectionReconciliationCheckpoint.AFTER_REMOTE_EFFECT ->
                RepositoryResult.Success(HealthLegacyDistanceProjectionStatus.RECONCILED)

            action == HealthSyncOperationAction.DELETE && state == SyncOperationState.SYNCED ->
                RepositoryResult.Success(HealthLegacyDistanceProjectionStatus.UNKNOWN_ID)

            else -> RepositoryResult.Success(HealthLegacyDistanceProjectionStatus.KNOWN_PENDING_DELETE)
        }
    }

    override suspend fun prepareLegacyWorkoutDistanceDeletion(
        activityId: WorkoutActivityId,
    ): RepositoryResult<HealthSyncOperation> = write {
        val activity = database.workoutActivityDao().findById(activityId.value.toString())
            ?: return@write RepositoryResult.NotFound
        if (activity.origin != WorkoutActivityOrigin.WATCH_D3_D4.name) {
            return@write RepositoryResult.UnexpectedState
        }
        val key = HealthSyncOperationKey(
            aggregate = HealthSyncAggregate.WorkoutActivity(activityId),
            recordType = HealthSyncRecordType.DISTANCE,
        )
        val entity = database.healthSyncOperationDao().findKnownLegacyWorkoutDistanceProjection(
            activityId = activityId.value.toString(),
            activityOrigin = WorkoutActivityOrigin.WATCH_D3_D4.name,
        ) ?: return@write RepositoryResult.NotFound
        val expectedClientRecordId = stableClientRecordId(key)
        if (entity.clientRecordId.isNullOrBlank() || entity.clientRecordId != expectedClientRecordId) {
            return@write RepositoryResult.UnexpectedState
        }
        val current = entity.toDomain() ?: return@write RepositoryResult.UnexpectedState
        val next = current.prepareAggregateDeletion(
            clientRecordId = expectedClientRecordId,
            clientRecordVersion = current.clientRecordVersion ?: 0L,
            updatedAt = Instant.now(),
        ).valueOrNull() ?: return@write RepositoryResult.UnexpectedState
        if (next != current && database.healthSyncOperationDao().update(next.toEntity()) != 1) {
            return@write RepositoryResult.UnexpectedState
        }
        RepositoryResult.Success(next)
    }

    override suspend fun validateDeleteIdentity(
        operation: HealthSyncOperation,
    ): RepositoryResult<Unit> = withContext(ioDispatcher) {
        if (operation.action != HealthSyncOperationAction.DELETE) {
            return@withContext RepositoryResult.UnexpectedState
        }
        val profileAggregate = operation.key.aggregate as? HealthSyncAggregate.UserProfileHeight
        if (profileAggregate != null) {
            if (operation.key.recordType != HealthSyncRecordType.HEIGHT ||
                operation.key.projectionChildKey != null ||
                operation.clientRecordVersion == null ||
                operation.clientRecordVersion < 0L
            ) {
                return@withContext RepositoryResult.UnexpectedState
            }
            val profile = database.userProfileDao().findById(profileAggregate.profileId.value.toString())
                ?: return@withContext RepositoryResult.NotFound
            if (profile.heightCentimeters != null) {
                return@withContext RepositoryResult.UnexpectedState
            }
            val expectedClientRecordId = stableClientRecordId(operation.key)
            if (operation.clientRecordId != expectedClientRecordId) {
                return@withContext RepositoryResult.UnexpectedState
            }
            val entity = database.healthSyncOperationDao().findById(operation.id.value.toString())
                ?: return@withContext RepositoryResult.NotFound
            if (entity.aggregateId != profileAggregate.profileId.value.toString() ||
                entity.aggregateType != HealthSyncAggregateType.USER_PROFILE_HEIGHT.name ||
                entity.recordType != HealthSyncRecordType.HEIGHT.name ||
                entity.projectionChildKey != HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY ||
                entity.action != HealthSyncOperationAction.DELETE.name ||
                entity.clientRecordId != expectedClientRecordId
            ) {
                return@withContext RepositoryResult.UnexpectedState
            }
            val persisted = entity.toDomainStrict().valueOrNull()
                ?: return@withContext RepositoryResult.UnexpectedState
            if (persisted.key != operation.key ||
                persisted.action != HealthSyncOperationAction.DELETE ||
                persisted.clientRecordId != expectedClientRecordId ||
                persisted.clientRecordVersion != operation.clientRecordVersion
            ) {
                return@withContext RepositoryResult.UnexpectedState
            }
            return@withContext RepositoryResult.Success(Unit)
        }
        val aggregate = operation.key.aggregate as? HealthSyncAggregate.WorkoutActivity
            ?: return@withContext RepositoryResult.UnexpectedState
        val activity = database.workoutActivityDao().findById(aggregate.activityId.value.toString())
            ?: return@withContext RepositoryResult.NotFound
        if (activity.origin != WorkoutActivityOrigin.WATCH_D3_D4.name) {
            return@withContext RepositoryResult.UnexpectedState
        }
        val child = operation.key.projectionChildKey
        if (child == null && operation.key.recordType != HealthSyncRecordType.DISTANCE) {
            return@withContext RepositoryResult.UnexpectedState
        }
        if (child != null && child.recordTypeName != operation.key.recordType.name) {
            return@withContext RepositoryResult.UnexpectedState
        }
        val expectedClientRecordId = stableClientRecordId(operation.key)
        if (operation.clientRecordId != expectedClientRecordId ||
            operation.clientRecordVersion == null ||
            operation.clientRecordVersion < 0L
        ) {
            return@withContext RepositoryResult.UnexpectedState
        }

        val entity = database.healthSyncOperationDao().findById(operation.id.value.toString())
            ?: return@withContext RepositoryResult.NotFound
        val expectedProjectionChildKey = child?.stableValue
            ?: HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY
        if (entity.aggregateId != aggregate.activityId.value.toString() ||
            entity.aggregateType != HealthSyncAggregateType.WORKOUT_ACTIVITY.name ||
            entity.recordType != operation.key.recordType.name ||
            entity.projectionChildKey != expectedProjectionChildKey ||
            entity.action != HealthSyncOperationAction.DELETE.name ||
            entity.clientRecordId != expectedClientRecordId
        ) {
            return@withContext RepositoryResult.UnexpectedState
        }
        val persisted = entity.toDomainStrict().valueOrNull()
            ?: return@withContext RepositoryResult.UnexpectedState
        if (persisted.key != operation.key ||
            persisted.action != HealthSyncOperationAction.DELETE ||
            persisted.clientRecordId != expectedClientRecordId ||
            persisted.clientRecordVersion != operation.clientRecordVersion
        ) {
            return@withContext RepositoryResult.UnexpectedState
        }
        RepositoryResult.Success(Unit)
    }

    override suspend fun findProcessable(limit: Int): RepositoryResult<List<HealthSyncOperation>> = write {
        when (val result = findProcessableInternal(limit)) {
            is RepositoryResult.Success -> RepositoryResult.Success(result.value.operations)
            RepositoryResult.Conflict -> RepositoryResult.Conflict
            RepositoryResult.UnexpectedState -> RepositoryResult.UnexpectedState
            RepositoryResult.NotFound -> RepositoryResult.NotFound
            RepositoryResult.StorageUnavailable -> RepositoryResult.StorageUnavailable
        }
    }

    override suspend fun findProcessableWithIntegrity(
        limit: Int,
    ): RepositoryResult<HealthSyncProcessableQueue> = write {
        findProcessableInternal(limit)
    }

    private suspend fun findProcessableInternal(
        limit: Int,
    ): RepositoryResult<HealthSyncProcessableQueue> {
        if (limit <= 0) return RepositoryResult.Success(HealthSyncProcessableQueue())

        val blockedAggregates = linkedSetOf<HealthSyncAggregate>()
        var unreconstructableAggregate = false
        for (entity in database.healthSyncOperationDao().findProjectionOperations()) {
            val aggregate = entity.toAggregateOrNull()
            if (aggregate == null) {
                unreconstructableAggregate = true
                continue
            }
            if (HealthProjectionChildKey.fromStableValue(entity.projectionChildKey) == null ||
                entity.toDomainStrict().valueOrNull() == null
            ) {
                blockedAggregates += aggregate
            }
        }
        if (unreconstructableAggregate) return RepositoryResult.UnexpectedState

        val repaired = database.healthSyncOperationDao().findProcessable(limit)
            .mapNotNull { entity ->
                if (isBlockedWatchWorkoutOperation(entity)) return@mapNotNull null
                if (entity.isWorkoutStepsOperationEntity()) {
                    val credit = database.workoutStepsCreditDao()
                        .findByWorkoutStepsOperationId(entity.id)
                        ?.toDomain()
                        ?: return@mapNotNull null
                    if (credit.state == WorkoutStepsCreditState.BLOCKED) return@mapNotNull null
                }
                val aggregate = entity.toAggregateOrNull()
                    ?: run {
                        unreconstructableAggregate = true
                        return@mapNotNull null
                    }
                val domain = entity.toDomainStrict().valueOrNull()
                    ?: run {
                        blockedAggregates += aggregate
                        return@mapNotNull null
                    }
                if (entity.isDailyStepsOperationEntity()) {
                    return@mapNotNull domain
                }
                if (entity.projectionChildKey != HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY) {
                    return@mapNotNull domain
                }
                if (domain.action == HealthSyncOperationAction.DELETE) {
                    return@mapNotNull domain
                }
                when (val ensured = ensureOperationInternal(domain.key)) {
                    is RepositoryResult.Success -> ensured.value
                    else -> null
                }
            }
        if (unreconstructableAggregate) return RepositoryResult.UnexpectedState
        return RepositoryResult.Success(
            HealthSyncProcessableQueue(
                operations = repaired.filter {
                    it.state != SyncOperationState.SYNCED && it.key.aggregate !in blockedAggregates
                },
                blockedAggregates = blockedAggregates,
            ),
        )
    }

    override fun observeSyncSummary(): Flow<HealthSyncSummary> =
        database.healthSyncOperationDao().observeSummary()
            .map { it.toDomain() }
            .flowOn(ioDispatcher)

    override suspend fun findOperationsForAggregate(
        aggregate: HealthSyncAggregate,
    ): RepositoryResult<List<HealthSyncOperation>> = withContext(ioDispatcher) {
        val entities = database.healthSyncOperationDao()
            .findByAggregate(aggregate.id.toString(), aggregate.type.name)
            .filterNot { entity ->
                entity.projectionChildKey == HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY &&
                    entity.action != HealthSyncOperationAction.DELETE.name &&
                    isBlockedWatchWorkoutOperation(entity)
            }
        val operations = entities.map { entity ->
            entity.toDomainStrict().valueOrNull()
                ?: return@withContext RepositoryResult.UnexpectedState
        }
        RepositoryResult.Success(
            operations,
        )
    }

    override fun observeForAggregates(aggregates: Set<HealthSyncAggregate>): Flow<List<HealthSyncOperation>> {
        if (aggregates.isEmpty()) return flowOf(emptyList())
        return combine(
            observeSyncEntitiesForAggregates(aggregates),
            database.workoutActivityDao().observeAll(),
        ) { entities, activities ->
            val blockedWatchActivityIds = activities.asSequence()
                .filter { it.origin == WorkoutActivityOrigin.WATCH_D3_D4.name }
                .map { it.id }
                .toSet()
            entities.mapNotNull { entity ->
                if (entity.isBlockedWatchWorkoutOperation(blockedWatchActivityIds)) null else entity.toDomain()
            }
        }
            .flowOn(ioDispatcher)
    }

    override fun observeForAggregatesWithIntegrity(
        aggregates: Set<HealthSyncAggregate>,
    ): Flow<HealthSyncQueueObservation> {
        return combine(
            database.healthSyncOperationDao().observeAll(),
            observeHealthSyncSemanticCandidateIndex(),
        ) { allEntities, candidateIndex ->
            val mappedOperations = allEntities.map { entity ->
                MappedHealthSyncOperation(
                    aggregate = entity.toAggregateOrNull(),
                    operation = entity.toDomain(),
                    hasSupportedPair = entity.hasSupportedHealthSyncPair(),
                )
            }
            val scopedOperations = mappedOperations.filter { mapping ->
                mapping.aggregate?.let(aggregates::contains) == true
            }
            val validOperations = scopedOperations.mapNotNull { mapping ->
                mapping.operation?.takeIf {
                    mapping.hasSupportedPair && !it.isBlockedWatchWorkoutOperation(candidateIndex)
                }
            }
            val activeScopedOperations = validOperations.filterNot { operation ->
                val activity = (operation.key.aggregate as? HealthSyncAggregate.WorkoutActivity)
                    ?.let { candidateIndex.workoutActivities[it.activityId.value] }
                activity?.workflowState in setOf(
                    ActivityWorkflowState.PENDING_REVIEW,
                    ActivityWorkflowState.PENDING_CALIBRATION,
                    ActivityWorkflowState.DISCARDED,
                )
            }
            val invalidActiveOperations = scopedOperations.filter { mapping ->
                val activity = (mapping.aggregate as? HealthSyncAggregate.WorkoutActivity)
                    ?.let { candidateIndex.workoutActivities[it.activityId.value] }
                mapping.aggregate !is HealthSyncAggregate.WorkoutActivity ||
                    activity?.workflowState !in setOf(
                        ActivityWorkflowState.PENDING_REVIEW,
                        ActivityWorkflowState.PENDING_CALIBRATION,
                        ActivityWorkflowState.DISCARDED,
                    )
            }
            HealthSyncQueueObservation(
                operations = activeScopedOperations,
                hasInvalidOperation = invalidActiveOperations.any { !it.isValid },
                hasUnresolvedAggregate = activeScopedOperations.any { operation ->
                    val activity = (operation.key.aggregate as? HealthSyncAggregate.WorkoutActivity)
                        ?.let { candidateIndex.workoutActivities[it.activityId.value] }
                    activity?.workflowState != ActivityWorkflowState.SYNCED &&
                        !isSemanticallyResolved(operation, candidateIndex)
                },
            )
        }.flowOn(ioDispatcher)
    }

    private fun observeHealthSyncSemanticCandidateIndex(): Flow<HealthSyncSemanticCandidateIndex> {
        val nonWorkoutCandidates = combine(
            database.dailySummaryDao().observeAll(),
            database.heartRateDao().observeAll(),
            database.userProfileDao().observeAllWeights(),
            database.userProfileDao().observeAllProfiles(),
        ) { dailySummaries, heartRates, weights, profiles ->
            NonWorkoutHealthSyncCandidates(
                dailySummaries = dailySummaries.mapNotNull { entity ->
                    mapAggregateSafely { entity.toDomain() }
                }.associateBy { it.id.value },
                heartRateMeasurements = heartRates.mapNotNull { entity ->
                    mapAggregateSafely { entity.toDomain() }
                }.associateBy { it.id.value },
                weightMeasurements = weights.mapNotNull { it.toDomain() }
                    .associateBy { it.id.value },
                userProfiles = profiles.mapNotNull { it.toDomain() }
                    .associateBy { it.id.value },
            )
        }
        val workoutCandidates = combine(
            combine(
                database.workoutActivityDao().observeAllHealthSyncCandidates(),
                database.workoutDetailDao().observeAllIntervals(),
                database.workoutDetailDao().observeAllHeartRateSamples(),
                database.watchWorkoutD4V4HeaderDao().observeAll(),
            ) { records, intervals, samples, headers ->
                HealthSyncWorkoutCandidateFacts(
                    records = records,
                    intervalsByActivityId = intervals.groupBy { it.activityId },
                    samplesByActivityId = samples.groupBy { it.activityId },
                    headersByActivityId = headers.associateBy { it.activityId },
                )
            },
            database.workoutDerivationDao().observeActiveRecords().map { records ->
                records.groupBy { it.snapshot.activityId }
                    .mapNotNull { (activityId, activityRecords) ->
                        activityRecords.singleOrNull()?.let { activityId to it }
                    }
                    .toMap()
            },
        ) { facts, derivationsByActivityId ->
            facts.records.mapNotNull { record ->
                val rawIntervals = facts.intervalsByActivityId[record.activity.id].orEmpty()
                val derivationIntervals = record.importParserVersion
                    ?.let { parserVersion ->
                        prepareWatchWorkoutIntervalsForDerivation(parserVersion, rawIntervals)
                    }
                    ?: rawIntervals
                record.toHealthSyncCandidateDomain(
                    intervals = rawIntervals,
                    samples = facts.samplesByActivityId[record.activity.id].orEmpty(),
                    derivation = derivationsByActivityId[record.activity.id],
                    derivationIntervals = derivationIntervals,
                    d4V4Header = facts.headersByActivityId[record.activity.id],
                )
            }.associateBy { it.id.value }
        }
        return combine(nonWorkoutCandidates, workoutCandidates) { nonWorkout, workouts ->
            HealthSyncSemanticCandidateIndex(
                dailySummaries = nonWorkout.dailySummaries,
                heartRateMeasurements = nonWorkout.heartRateMeasurements,
                workoutActivities = workouts,
                weightMeasurements = nonWorkout.weightMeasurements,
                userProfiles = nonWorkout.userProfiles,
            )
        }
    }

    private fun observeSyncEntitiesForAggregates(
        aggregates: Set<HealthSyncAggregate>,
    ): Flow<List<HealthSyncOperationEntity>> {
        if (aggregates.isEmpty()) return flowOf(emptyList())

        val queries = aggregates
            .groupBy { it.type }
            .values
            .flatMap { sameTypeAggregates ->
                sameTypeAggregates
                    .map { it.id.toString() }
                    .distinct()
                    .chunked(SQLITE_IN_CLAUSE_CHUNK_SIZE)
                    .map { aggregateIds ->
                        database.healthSyncOperationDao().observeByAggregateIds(
                            aggregateType = sameTypeAggregates.first().type.name,
                            aggregateIds = aggregateIds,
                        )
                    }
            }

        return combine(queries) { batches ->
            batches.asSequence()
                .flatMap { it.asSequence() }
                .toList()
        }
    }

    override suspend fun markStarted(operationId: HealthSyncOperationId, at: Instant): RepositoryResult<Unit> =
        transitionSync(operationId) { operation ->
            if (operation.state != SyncOperationState.PENDING.name && operation.state != SyncOperationState.FAILED.name) null
            else operation.copy(
                state = SyncOperationState.SYNCING.name,
                attemptCount = operation.attemptCount + 1,
                lastAttemptAtEpochMillis = at.toEpochMilli(),
                lastError = null,
                updatedAtEpochMillis = at.toEpochMilli(),
                reconciliationCheckpoint = HealthProjectionReconciliationCheckpoint.BEFORE_REMOTE_EFFECT.name,
            )
        }

    override suspend fun markStartedIfCurrent(
        operationId: HealthSyncOperationId,
        expectedClientRecordVersion: Long?,
        at: Instant,
    ): RepositoryResult<Unit> = transitionSync(operationId) { operation ->
        if (operation.clientRecordVersion != expectedClientRecordVersion) {
            null
        } else if (operation.state != SyncOperationState.PENDING.name &&
            operation.state != SyncOperationState.FAILED.name
        ) {
            null
        } else {
            operation.copy(
                state = SyncOperationState.SYNCING.name,
                attemptCount = operation.attemptCount + 1,
                lastAttemptAtEpochMillis = at.toEpochMilli(),
                lastError = null,
                updatedAtEpochMillis = at.toEpochMilli(),
                reconciliationCheckpoint = HealthProjectionReconciliationCheckpoint.BEFORE_REMOTE_EFFECT.name,
            )
        }
    }

    override suspend fun markSynced(operationId: HealthSyncOperationId, at: Instant): RepositoryResult<Unit> = try {
        write {
            val syncDao = database.healthSyncOperationDao()
            val current = syncDao.findById(operationId.value.toString())
                ?: return@write RepositoryResult.NotFound
            if (current.state != SyncOperationState.SYNCING.name ||
                current.projectionChildKey != HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY &&
                    current.reconciliationCheckpoint !in setOf(
                        HealthProjectionReconciliationCheckpoint.BEFORE_REMOTE_EFFECT.name,
                        HealthProjectionReconciliationCheckpoint.AFTER_REMOTE_EFFECT.name,
                    )
            ) return@write RepositoryResult.UnexpectedState

            val next = current.copy(
                state = SyncOperationState.SYNCED.name,
                lastError = null,
                syncedAtEpochMillis = at.toEpochMilli(),
                updatedAtEpochMillis = at.toEpochMilli(),
                reconciliationState = if (current.action == HealthSyncOperationAction.DELETE.name) {
                    HealthProjectionReconciliationState.OBSOLETE.name
                } else {
                    HealthProjectionReconciliationState.SENT.name
                },
                reconciliationCheckpoint = HealthProjectionReconciliationCheckpoint.AFTER_REMOTE_EFFECT.name,
            )
            if (current.isDailyStepsOperationEntity() &&
                !consumeDailyStepsCreditReservations(next, at)
            ) {
                throw WorkoutStepsCreditCommitAborted()
            }
            if (current.isWorkoutStepsOperationEntity() &&
                !activateWorkoutStepsCreditForAcceptedOperation(next, at)
            ) {
                throw WorkoutStepsCreditCommitAborted()
            }
            if (syncDao.update(next) != 1) throw WorkoutStepsCreditCommitAborted()
            RepositoryResult.Success(Unit)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: WorkoutStepsCreditCommitAborted) {
        RepositoryResult.UnexpectedState
    }

    override suspend fun markFailed(
        operationId: HealthSyncOperationId,
        error: SafeSyncError,
        at: Instant,
    ): RepositoryResult<Unit> = transitionSync(operationId) { operation ->
        if (operation.state != SyncOperationState.SYNCING.name) null
        else operation.copy(
            state = SyncOperationState.FAILED.name,
            lastError = error.name,
            updatedAtEpochMillis = at.toEpochMilli(),
        )
    }

    override suspend fun resetInterrupted(at: Instant): RepositoryResult<Int> = write {
        val syncDao = database.healthSyncOperationDao()
        val syncing = syncDao.findSyncing()
        var resetCount = 0
        syncing.forEach { operation ->
            if (operation.lastAttemptAtEpochMillis != null && operation.lastAttemptAtEpochMillis > at.toEpochMilli()) return@forEach
            if (!aggregateExists(operation)) return@forEach
            syncDao.update(
                operation.copy(
                    state = SyncOperationState.FAILED.name,
                    lastError = SafeSyncError.INTERRUPTED.name,
                    updatedAtEpochMillis = at.toEpochMilli(),
                ),
            )
            resetCount += 1
        }
        RepositoryResult.Success(resetCount)
    }

    override suspend fun reconcileBacklog(): RepositoryResult<HealthSyncBacklogReconciliationResult> = write {
        RepositoryResult.Success(reconcileBacklogInternal())
    }

    override suspend fun store(payload: DiagnosticPayload): RepositoryResult<Unit> = write {
        val payloadDao = database.diagnosticPayloadDao()
        payloadDao.insert(payload.toEntity())
        payload.toDerivedOutputEntities(generateIds(payload.derivedOutputs.size)).forEach { derivedOutput ->
            payloadDao.insertDerivedOutput(derivedOutput)
        }
        RepositoryResult.Success(Unit)
    }

    override suspend fun reprocess(payloadId: DiagnosticPayloadId): RepositoryResult<Unit> = write {
        if (database.diagnosticPayloadDao().findById(payloadId.value.toString()) == null) RepositoryResult.NotFound
        else RepositoryResult.Success(Unit) // Parser execution belongs to future protocol phase; persisted outputs remain idempotent.
    }

    override suspend fun clearExpired(now: Instant): RepositoryResult<Int> = write {
        val payloadDao = database.diagnosticPayloadDao()
        val expired = payloadDao.findExpired(now.minus(br.com.mwconsulting.championcompanion.core.model.DiagnosticRetention.maximumAge).toEpochMilli())
        val retained = payloadDao.findOldestFirst().filterNot { candidate -> expired.any { it.id == candidate.id } }.toMutableList()
        var bytes = retained.sumOf { it.byteCount }
        val overLimit = buildList {
            for (payload in retained) {
                if (bytes <= br.com.mwconsulting.championcompanion.core.model.DiagnosticRetention.maximumBytes) break
                add(payload.id)
                bytes -= payload.byteCount
            }
        }
        val ids = (expired.map { it.id } + overLimit).distinct()
        RepositoryResult.Success(if (ids.isEmpty()) 0 else payloadDao.deleteByIds(ids))
    }

    private suspend fun upsertAssociatedDevice(
        device: Device,
        associatedAtEpochMillis: Long,
        connectedAtEpochMillis: Long? = null,
    ): DeviceEntity = with(database) {
        val deviceDao = deviceDao()
        val associationDao = deviceAssociationDao()
        val existing = deviceDao.findByPlatformIdentifier(device.platformIdentifier)
        val entity = device.toEntity()
        val persisted = if (existing == null) {
            val inserted = entity.copy(lastSuccessfulConnectionAtEpochMillis = connectedAtEpochMillis ?: entity.lastSuccessfulConnectionAtEpochMillis)
            deviceDao.insert(inserted)
            inserted
        } else {
            val merged = entity.copy(
                id = existing.id,
                lastSuccessfulConnectionAtEpochMillis = connectedAtEpochMillis
                    ?: entity.lastSuccessfulConnectionAtEpochMillis
                    ?: existing.lastSuccessfulConnectionAtEpochMillis,
                lastImportedAtEpochMillis = entity.lastImportedAtEpochMillis ?: existing.lastImportedAtEpochMillis,
                importParserVersion = entity.importParserVersion ?: existing.importParserVersion,
            )
            deviceDao.update(merged)
            merged
        }
        associationDao.upsert(
            persisted.toAssociationEntity(
                associatedAtEpochMillis = associatedAtEpochMillis,
                updatedAtEpochMillis = associatedAtEpochMillis,
            ),
        )
        persisted
    }

    private suspend fun loadWorkoutActivity(activity: WorkoutActivityEntity): WorkoutActivity? {
        val importRecord = database.watchWorkoutImportDao().findByActivityId(activity.id)
        val intervals = database.workoutDetailDao().findIntervalsForActivity(activity.id)
        val samples = database.workoutDetailDao().findHeartRateSamplesForActivity(activity.id)
        val d4V4Header = database.watchWorkoutD4V4HeaderDao().findByActivityId(activity.id)
        val derivationSnapshot = findActiveDerivation(
            activity = activity,
            importRecord = importRecord,
            intervals = intervals,
            samples = samples,
            d4V4Header = d4V4Header,
        )
        val canonicalReadStatusOverride = if (
            activity.origin == WorkoutActivityOrigin.WATCH_D3_D4.name && derivationSnapshot == null &&
            database.userProfileDao().findCurrent()?.toDomain()?.stepLengthProfile == null
        ) {
            WorkoutCanonicalReadStatus.MISSING_STEP_LENGTH
        } else {
            null
        }
        return activity.toDomain(
            importRecord = importRecord,
            intervals = intervals,
            samples = samples,
            derivationSnapshot = derivationSnapshot,
            canonicalReadStatusOverride = canonicalReadStatusOverride,
            d4V4Header = d4V4Header,
        )
    }

    /** Atualiza somente política no snapshot ativo; filhos e fatos permanecem intactos. */
    private suspend fun updateActiveDerivationSnapshot(
        activityId: String,
        snapshot: WorkoutDerivationSnapshot,
    ): Boolean {
        val activeRecord = database.workoutDerivationDao().findActiveForActivity(activityId) ?: return false
        val entity = snapshot.toEntity(
            id = activeRecord.snapshot.id,
            activityId = activityId,
            isActive = true,
        ) ?: return false
        return database.workoutDerivationDao().updateSnapshot(entity) == 1
    }

    private suspend fun reprocessWatchWorkoutInternal(
        activityId: WorkoutActivityId,
        derivedAt: Instant,
    ): WatchWorkoutReprocessResult {
        var activity = database.workoutActivityDao().findById(activityId.value.toString())
            ?: return WatchWorkoutReprocessResult.Blocked(WatchWorkoutReprocessFailureReason.ACTIVITY_NOT_FOUND)
        if (activity.origin != WorkoutActivityOrigin.WATCH_D3_D4.name) {
            return WatchWorkoutReprocessResult.Blocked(WatchWorkoutReprocessFailureReason.NOT_WATCH_WORKOUT)
        }
        when (activity.workflowState) {
            ActivityWorkflowState.SYNCING.name -> return WatchWorkoutReprocessResult.Blocked(
                WatchWorkoutReprocessFailureReason.SYNC_IN_PROGRESS,
            )
            ActivityWorkflowState.SYNCED.name -> return WatchWorkoutReprocessResult.Blocked(
                WatchWorkoutReprocessFailureReason.SYNCED_REQUIRES_FUTURE_PROJECTION,
            )
        }
        if (activity.workflowState !in setOf(
                ActivityWorkflowState.PENDING_CALIBRATION.name,
                ActivityWorkflowState.READY_TO_SYNC.name,
                ActivityWorkflowState.SYNC_FAILED.name,
            ) || hasUnsafeWorkoutProjectionChild(activity.id)
        ) {
            return WatchWorkoutReprocessResult.Blocked(
                WatchWorkoutReprocessFailureReason.DERIVATION_INCONSISTENT,
            )
        }

        val importRecord = database.watchWorkoutImportDao().findByActivityId(activity.id)
            ?: return WatchWorkoutReprocessResult.Blocked(WatchWorkoutReprocessFailureReason.IMPORT_METADATA_MISSING)
        val persistedActivity = activity
        val rawIntervals = database.workoutDetailDao().findIntervalsForActivity(activity.id)
        val durationRecovery = recoverAuthoritativeDurationFromDiagnostic(
            activity = activity,
            importRecord = importRecord,
            rawIntervals = rawIntervals,
        )
        if (importRecord.parserVersion == WATCH_WORKOUT_D4_V2_PARSER_VERSION &&
            !durationRecovery.resolved
        ) {
            return WatchWorkoutReprocessResult.Blocked(WatchWorkoutReprocessFailureReason.TEMPORAL_INCONSISTENCY)
        }
        if (durationRecovery.activity != activity) {
            activity = durationRecovery.activity
        }
        val preparedIntervals = prepareWatchWorkoutIntervalsForDerivation(importRecord.parserVersion, rawIntervals)
            ?: return WatchWorkoutReprocessResult.Blocked(
                rawIntervalFailureReason(importRecord.parserVersion, rawIntervals),
            )
        if (rawIntervals.any { it.temporalEligibilityReason != TEMPORAL_ORDER_VALIDATED_MARKER }) {
            return WatchWorkoutReprocessResult.Blocked(WatchWorkoutReprocessFailureReason.TEMPORAL_INCONSISTENCY)
        }
        val indexValidation = TemporalSeriesValidator.validateIndices(rawIntervals.map { it.intervalIndex })
        if (!indexValidation.isEligible) {
            return WatchWorkoutReprocessResult.Blocked(WatchWorkoutReprocessFailureReason.INDEX_INELIGIBLE)
        }

        val totalDuration = runCatching { Duration.ofNanos(activity.durationNanos) }.getOrNull()
            ?.takeIf { it.isPositive() }
            ?: return WatchWorkoutReprocessResult.Blocked(WatchWorkoutReprocessFailureReason.TEMPORAL_INCONSISTENCY)
        val measuredIntervals = preparedIntervals.map { entity ->
            // Reprocessamento recalcula política temporal atual; duração
            // persistida pertence a snapshot anterior e não é fato de origem.
            entity.copy(effectiveDurationNanos = null).toMeasuredWorkoutInterval(
                parserVersion = importRecord.parserVersion,
            )
        }
        if (measuredIntervals.any { it == null }) {
            return WatchWorkoutReprocessResult.Blocked(WatchWorkoutReprocessFailureReason.TEMPORAL_INCONSISTENCY)
        }
        val measured = measuredIntervals.filterNotNull()
        val temporalResolution = WorkoutDurationResolver.resolve(
            totalDuration = totalDuration,
            intervals = measured,
            sessionStartedAt = Instant.ofEpochMilli(activity.startedAtEpochMillis),
        )
        if (!temporalResolution.isEligible) {
            return WatchWorkoutReprocessResult.Blocked(
                if (temporalResolution.reasons.any {
                        it in setOf(
                            TemporalSeriesValidationReason.FIRST_INDEX_NOT_ZERO,
                            TemporalSeriesValidationReason.NON_CONTIGUOUS,
                            TemporalSeriesValidationReason.INDEX_GAP,
                            TemporalSeriesValidationReason.DUPLICATE_INDEX,
                            TemporalSeriesValidationReason.UNIQUE,
                            TemporalSeriesValidationReason.OUT_OF_ORDER,
                        )
                    }
                ) {
                    WatchWorkoutReprocessFailureReason.INDEX_INELIGIBLE
                } else {
                    WatchWorkoutReprocessFailureReason.TEMPORAL_INCONSISTENCY
                },
            )
        }

        if (activity != persistedActivity && database.workoutActivityDao().update(activity) != 1) {
            return WatchWorkoutReprocessResult.Blocked(
                WatchWorkoutReprocessFailureReason.DERIVATION_INCONSISTENT,
            )
        }

        // A duração recuperada só pode ser publicada junto com uma derivação
        // materializada. Como este update já ocorreu dentro de
        // withWriteTransaction, qualquer resultado diferente de sucesso
        // materializado precisa atravessar a transação como exceção para
        // desfazer a alteração da atividade e quaisquer gravações parciais da
        // derivação.
        return when (val result = deriveWatchWorkoutInternal(
            activityId = activityId,
            derivedAt = derivedAt,
            replaceActive = true,
        )) {
            is RepositoryResult.Success -> result.value?.let(WatchWorkoutReprocessResult::Success)
                ?: throw WatchWorkoutDerivationPreflightException()
            RepositoryResult.NotFound,
            RepositoryResult.Conflict,
            RepositoryResult.UnexpectedState,
            RepositoryResult.StorageUnavailable,
            -> throw WatchWorkoutDerivationPreflightException()
        }
    }

    private data class AuthoritativeDurationRecovery(
        val activity: WorkoutActivityEntity,
        val resolved: Boolean,
    )

    private suspend fun recoverAuthoritativeDurationFromDiagnostic(
        activity: WorkoutActivityEntity,
        importRecord: WatchWorkoutImportEntity,
        rawIntervals: List<WorkoutIntervalEntity>,
    ): AuthoritativeDurationRecovery {
        fun unchanged() = AuthoritativeDurationRecovery(activity = activity, resolved = false)
        val payloadId = importRecord.diagnosticPayloadId ?: activity.diagnosticPayloadId ?: return unchanged()
        val payload = database.diagnosticPayloadDao().findById(payloadId) ?: return unchanged()
        val block = ChampionD4WorkoutBlockProtocol.parseDiagnosticBytes(payload.privateBytes) ?: return unchanged()
        if (block.blockIndex != importRecord.externalBlockIndex) return unchanged()
        val deviceId = runCatching { DeviceId(UUID.fromString(importRecord.deviceId)) }.getOrNull() ?: return unchanged()
        val parserVersion = ParserVersion.create(importRecord.parserVersion).valueOrNull() ?: return unchanged()
        val diagnosticPayloadId = importRecord.diagnosticPayloadId?.let { id ->
            runCatching { DiagnosticPayloadId(UUID.fromString(id)) }.getOrNull()
        }
        val parsed = ChampionD4WorkoutParser.parse(
            ChampionD4WorkoutParser.Request(
                deviceId = deviceId,
                crc = importRecord.crc,
                externalBlockIndex = importRecord.externalBlockIndex,
                parserVersion = parserVersion,
                diagnosticPayloadId = diagnosticPayloadId,
                block = block,
            ),
        )
        val input = when (parsed) {
            is br.com.mwconsulting.championcompanion.core.model.WatchWorkoutParserResult.Valid -> parsed.input
            else -> return unchanged()
        }
        if (input.metrics.startedAt.toEpochMilli() != activity.startedAtEpochMillis ||
            input.intervals.size != rawIntervals.size ||
            input.intervals.map { it.index } != rawIntervals.map { it.intervalIndex } ||
            input.intervals.zip(rawIntervals).any { (incoming, persisted) ->
                incoming.steps != persisted.steps ||
                    incoming.rawMotionValue != persisted.rawMotionValue ||
                    incoming.rawDistanceValue != persisted.rawDistanceValue ||
                    incoming.rawCaloriesValue != persisted.rawCaloriesValue ||
                    incoming.decodedSteps != persisted.decodedSteps ||
                    incoming.nativeSportValue != persisted.nativeSportValue ||
                    incoming.nativeSteps != persisted.nativeSteps ||
                    incoming.nativeDistanceMeters != persisted.nativeDistanceMeters ||
                    incoming.nativeCaloriesRaw != persisted.nativeCaloriesRaw ||
                    incoming.nativePauseStateRaw != persisted.nativePauseStateRaw
            }
        ) return unchanged()
        if (importRecord.parserVersion == WATCH_WORKOUT_D4_V4_PARSER_VERSION) {
            val persistedHeader = database.watchWorkoutD4V4HeaderDao()
                .findByActivityId(activity.id)
                ?.toD4V4Header()
                ?: return unchanged()
            if (input.d4V4Block?.header != persistedHeader) return unchanged()
        }
        val durationNanos = runCatching { input.metrics.duration.toNanos() }.getOrNull()
            ?.takeIf { it > 0L } ?: return unchanged()
        return AuthoritativeDurationRecovery(
            activity = activity.copy(
                durationNanos = durationNanos,
                endedAtEpochMillis = input.metrics.endedAt.toEpochMilli(),
            ),
            resolved = true,
        )
    }

    private fun prepareWatchWorkoutIntervalsForDerivation(
        parserVersion: String,
        rawIntervals: List<WorkoutIntervalEntity>,
    ): List<WorkoutIntervalEntity>? {
        if (parserVersion !in setOf(
                WATCH_WORKOUT_D4_V1_PARSER_VERSION,
                WATCH_WORKOUT_D4_V2_PARSER_VERSION,
                WATCH_WORKOUT_D4_V3_PARSER_VERSION,
                WATCH_WORKOUT_D4_V4_PARSER_VERSION,
            )
        ) return null
        if (rawIntervals.isEmpty()) return null
        return rawIntervals.map { entity ->
            when (parserVersion) {
                WATCH_WORKOUT_D4_V1_PARSER_VERSION -> {
                    val rawMotionValue = entity.rawMotionValue ?: return null
                    if (rawMotionValue !in 0L..MeasuredWorkoutInterval.MAX_RAW_MOTION_VALUE) return null
                    entity.copy(
                        // Ponte somente em memória; coluna legacy permanece intacta.
                        decodedSteps = rawMotionValue ushr 16,
                    )
                }
                WATCH_WORKOUT_D4_V2_PARSER_VERSION,
                WATCH_WORKOUT_D4_V3_PARSER_VERSION,
                -> {
                    val rawMotionValue = entity.rawMotionValue ?: return null
                    if (rawMotionValue !in 0L..MeasuredWorkoutInterval.MAX_RAW_MOTION_VALUE ||
                        entity.decodedSteps == null || entity.decodedSteps != (rawMotionValue ushr 16)
                    ) return null
                    entity
                }
                WATCH_WORKOUT_D4_V4_PARSER_VERSION -> if (hasValidD4V4NativeFacts(entity)) {
                    entity
                } else {
                    return null
                }
                else -> return null
            }
        }
    }

    private fun rawIntervalFailureReason(
        parserVersion: String,
        rawIntervals: List<WorkoutIntervalEntity>,
    ): WatchWorkoutReprocessFailureReason = when {
        parserVersion !in setOf(
            WATCH_WORKOUT_D4_V1_PARSER_VERSION,
            WATCH_WORKOUT_D4_V2_PARSER_VERSION,
            WATCH_WORKOUT_D4_V3_PARSER_VERSION,
            WATCH_WORKOUT_D4_V4_PARSER_VERSION,
        ) ->
            WatchWorkoutReprocessFailureReason.UNKNOWN_PARSER_VERSION
        parserVersion == WATCH_WORKOUT_D4_V4_PARSER_VERSION && rawIntervals.isEmpty() ->
            WatchWorkoutReprocessFailureReason.RAW_INTERVALS_MISSING
        parserVersion == WATCH_WORKOUT_D4_V4_PARSER_VERSION && rawIntervals.any {
            !hasValidD4V4NativeFacts(it)
        } ->
            if (rawIntervals.any { it.nativeSportValue == null || it.nativeSteps == null ||
                    it.nativeDistanceMeters == null || it.nativeCaloriesRaw == null ||
                    it.nativePauseStateRaw == null
            }) {
                WatchWorkoutReprocessFailureReason.RAW_INTERVALS_MISSING
            } else {
                WatchWorkoutReprocessFailureReason.RAW_INTERVAL_INVALID
            }
        rawIntervals.isEmpty() || rawIntervals.any { it.rawMotionValue == null } ->
            WatchWorkoutReprocessFailureReason.RAW_INTERVALS_MISSING
        rawIntervals.any {
            it.rawMotionValue !in 0L..MeasuredWorkoutInterval.MAX_RAW_MOTION_VALUE
        } -> WatchWorkoutReprocessFailureReason.RAW_INTERVAL_INVALID
        parserVersion in setOf(
            WATCH_WORKOUT_D4_V2_PARSER_VERSION,
            WATCH_WORKOUT_D4_V3_PARSER_VERSION,
        ) && rawIntervals.any {
            val rawMotionValue = it.rawMotionValue ?: return@any false
            it.decodedSteps == null || it.decodedSteps != (rawMotionValue ushr 16)
        } -> WatchWorkoutReprocessFailureReason.MEASURED_STEPS_MISSING
        else -> WatchWorkoutReprocessFailureReason.RAW_INTERVAL_INVALID
    }

    private fun hasValidD4V4NativeFacts(entity: WorkoutIntervalEntity): Boolean =
        entity.rawMotionValue == null &&
            entity.rawDistanceValue == null &&
            entity.rawCaloriesValue == null &&
            entity.decodedSteps == null &&
            entity.nativeSportValue?.let { it in 0L..0xFFFFL } == true &&
            entity.nativeSteps?.let { it in 0L..0xFFFFL && it == entity.steps } == true &&
            entity.nativeDistanceMeters?.let {
                it.isFinite() && it in 0.0..0xFFFF.toDouble() &&
                    it == it.toLong().toDouble()
            } == true &&
            entity.nativeCaloriesRaw?.let { it in 0L..0xFFFFL } == true &&
            entity.nativePauseStateRaw?.let { it in 0L..0xFFL } == true

    /** Retorna distância nativa somente quando contrato v4 completo fecha. */
    private suspend fun nativeWatchDistanceForCalibration(
        activity: WorkoutActivityEntity,
        importRecord: WatchWorkoutImportEntity,
        rawIntervals: List<WorkoutIntervalEntity>,
    ): Double? {
        if (importRecord.parserVersion != WATCH_WORKOUT_D4_V4_PARSER_VERSION) return null
        val totalDuration = runCatching { Duration.ofNanos(activity.durationNanos) }
            .getOrNull()?.takeIf { it.isPositive() } ?: return null
        val sessionStartedAt = Instant.ofEpochMilli(activity.startedAtEpochMillis)
        val measuredIntervals = measuredIntervalsForDerivation(
            activityStartedAt = sessionStartedAt,
            rawIntervals = rawIntervals,
            parserVersion = importRecord.parserVersion,
        ) ?: return null
        val resolution = WorkoutDurationResolver.resolve(
            totalDuration = totalDuration,
            intervals = measuredIntervals,
            sessionStartedAt = sessionStartedAt,
        )
        val parserValidity = runCatching { ParserValidity.valueOf(importRecord.parserValidity) }.getOrNull()
        val headerEntity = database.watchWorkoutD4V4HeaderDao().findByActivityId(activity.id) ?: return null
        val header = headerEntity.toD4V4Header() ?: return null
        if (!WatchWorkoutNativeDistancePolicy.isEligible(
                workoutType = activity.confirmedType?.let { runCatching { WorkoutType.valueOf(it) }.getOrNull() },
                parserValidity = parserValidity,
                header = header,
                headerConsistency = headerEntity.toD4V4Consistency(),
                intervals = measuredIntervals,
                resolution = resolution,
                sessionStartedAt = sessionStartedAt,
                totalDuration = totalDuration,
                expectedCrc = importRecord.crc,
            )
        ) return null
        val totalDistance = measuredIntervals.sumOf { it.nativeDistanceMeters ?: return null }
        return totalDistance.takeIf { it.isFinite() && it > 0.0 }
    }

    private suspend fun deriveWatchWorkoutInternal(
        activityId: WorkoutActivityId,
        derivedAt: Instant,
        replaceActive: Boolean,
        explicitStepLength: StepLengthProfile? = null,
        explicitDistanceCalibrationFactor: Double? = null,
        explicitDerivationVersion: String = WATCH_WORKOUT_DERIVATION_VERSION,
    ): RepositoryResult<WorkoutDerivationSnapshot?> {
        if (explicitDistanceCalibrationFactor?.let { !it.isFinite() || it <= 0.0 } == true) {
            return RepositoryResult.UnexpectedState
        }
        val activity = database.workoutActivityDao().findById(activityId.value.toString())
            ?: return RepositoryResult.NotFound
        if (activity.origin != WorkoutActivityOrigin.WATCH_D3_D4.name) {
            return RepositoryResult.UnexpectedState
        }
        if (!replaceActive && activity.workflowState in setOf(
                ActivityWorkflowState.SYNCING.name,
                ActivityWorkflowState.SYNCED.name,
                ActivityWorkflowState.DISCARDED.name,
            )
        ) {
            // Derivação sem substituição é reservada à importação local. Não
            // materialize snapshot novo em atividade com ciclo remoto ou descarte.
            return RepositoryResult.UnexpectedState
        }
        if (replaceActive && (activity.workflowState !in setOf(
                ActivityWorkflowState.PENDING_CALIBRATION.name,
                ActivityWorkflowState.READY_TO_SYNC.name,
                ActivityWorkflowState.SYNC_FAILED.name,
            ) || hasUnsafeWorkoutProjectionChild(activity.id))
        ) {
            // Reprocessamento específico de atividade sincronizada pertence a
            // 014-11; não crie snapshot novo mantendo operações antigas.
            return RepositoryResult.UnexpectedState
        }
        val importRecord = database.watchWorkoutImportDao().findByActivityId(activity.id)
            ?: return RepositoryResult.UnexpectedState
        val rawIntervals = database.workoutDetailDao().findIntervalsForActivity(activity.id)
        val samples = database.workoutDetailDao().findHeartRateSamplesForActivity(activity.id)
        val persistedHeartRateSamples = samples.map { it.toDomain() }
        if (persistedHeartRateSamples.any { it == null }) return RepositoryResult.UnexpectedState
        val persistedHeartRateDomains = persistedHeartRateSamples.filterNotNull()
        val d4V4HeaderEntity = if (importRecord.parserVersion == WATCH_WORKOUT_D4_V4_PARSER_VERSION) {
            database.watchWorkoutD4V4HeaderDao().findByActivityId(activity.id)
        } else {
            null
        }
        val d4V4Header = d4V4HeaderEntity?.toD4V4Header()
        val parserValidity = runCatching { ParserValidity.valueOf(importRecord.parserValidity) }.getOrNull()
        val derivationRawIntervals = prepareWatchWorkoutIntervalsForDerivation(
            parserVersion = importRecord.parserVersion,
            rawIntervals = rawIntervals,
        ) ?: return RepositoryResult.UnexpectedState
        val activeRecord = database.workoutDerivationDao().findActiveForActivity(activity.id)
        if (rawIntervals.hasPersistedUnvalidatedSourceOrder()) {
            // Room ordena por índice. Um marcador persistido antes dessa
            // ordenação impede que derivação reconstrua série sintética. Linhas
            // legadas sem marcador também falham fechadas.
            if (activeRecord != null) {
                database.workoutDerivationDao().deactivateActiveSnapshots(activity.id)
            }
            return RepositoryResult.Success(null)
        }
        val existingSnapshot = activeRecord?.toDomain(
            activityOrigin = WorkoutActivityOrigin.WATCH_D3_D4,
            activityStartedAt = Instant.ofEpochMilli(activity.startedAtEpochMillis),
            activityDurationNanos = activity.durationNanos,
            rawIntervals = derivationRawIntervals,
            heartRateSamples = persistedHeartRateDomains,
            parserVersion = importRecord.parserVersion,
            confirmedType = activity.confirmedType?.let { runCatching { WorkoutType.valueOf(it) }.getOrNull() },
            parserValidity = parserValidity,
            d4V4Header = d4V4Header,
            d4V4Consistency = d4V4HeaderEntity?.toD4V4Consistency(),
            expectedCrc = importRecord.crc,
        )
        if (activeRecord != null && !replaceActive) {
            val persistedSnapshot = existingSnapshot ?: return RepositoryResult.UnexpectedState
            promoteCanonicalTreadmillIfNeeded(activity, persistedSnapshot)
            return RepositoryResult.Success(persistedSnapshot)
        }

        val confirmedType = activity.confirmedType
            ?.let { runCatching { WorkoutType.valueOf(it) }.getOrNull() }
            ?: WorkoutType.UNKNOWN
        val totalDuration = runCatching { Duration.ofNanos(activity.durationNanos) }.getOrNull()
            ?: return RepositoryResult.UnexpectedState
        val sessionStartedAt = Instant.ofEpochMilli(activity.startedAtEpochMillis)
        val measuredIntervals = measuredIntervalsForDerivation(
            activityStartedAt = sessionStartedAt,
            rawIntervals = rawIntervals,
            parserVersion = importRecord.parserVersion,
        ) ?: return RepositoryResult.UnexpectedState
        val temporalResolution = WorkoutDurationResolver.resolve(
            totalDuration = totalDuration,
            intervals = measuredIntervals,
            sessionStartedAt = sessionStartedAt,
        )
        val nativeEligible = importRecord.parserVersion == WATCH_WORKOUT_D4_V4_PARSER_VERSION &&
            WatchWorkoutNativeDistancePolicy.isEligible(
                workoutType = confirmedType,
                parserValidity = parserValidity,
                header = d4V4Header,
                headerConsistency = d4V4HeaderEntity?.toD4V4Consistency(),
                intervals = measuredIntervals,
                resolution = temporalResolution,
                sessionStartedAt = sessionStartedAt,
                totalDuration = totalDuration,
                expectedCrc = importRecord.crc,
            )
        val currentProfile = database.userProfileDao().findCurrent()?.toDomain()
        val resolvedDistanceCalibrationFactor = if (nativeEligible) {
            explicitDistanceCalibrationFactor
                ?: resolveLearnedDistanceCalibrationFactorInternal(confirmedType, currentProfile)
        } else {
            null
        }
        val profile = explicitStepLength
            ?: resolveEffectiveStepLengthInternal(confirmedType, currentProfile)
        val snapshot = if (nativeEligible) {
            WorkoutDerivationSnapshot.deriveNative(
                intervals = measuredIntervals,
                totalDuration = totalDuration,
                sessionStartedAt = sessionStartedAt,
                workoutType = confirmedType,
                header = d4V4Header ?: return RepositoryResult.UnexpectedState,
                parserValidity = parserValidity ?: return RepositoryResult.UnexpectedState,
                headerConsistency = d4V4HeaderEntity?.toD4V4Consistency()
                    ?: return RepositoryResult.UnexpectedState,
                expectedCrc = importRecord.crc,
                derivedAt = derivedAt,
                plausibility = existingSnapshot?.plausibility ?: WorkoutSeriesPlausibility(),
                distanceCalibrationFactor = resolvedDistanceCalibrationFactor,
            ).valueOrNull() ?: return RepositoryResult.UnexpectedState
        } else {
            if (profile == null) {
                // Perfil ausente nunca transforma distância diagnóstica em fallback.
                if (existingSnapshot != null) {
                    promoteCanonicalTreadmillIfNeeded(activity, existingSnapshot)
                    return RepositoryResult.Success(existingSnapshot)
                }
                return RepositoryResult.Success(null)
            }
            val derivationVersion = if (
                profile.source == StepLengthSource.CONTEXTUAL_LEARNED_PROFILE ||
                profile.source == StepLengthSource.REFERENCE_DISTANCE_CALIBRATION ||
                confirmedType in setOf(WorkoutType.RUNNING, WorkoutType.WALKING)
            ) {
                WATCH_WORKOUT_REFERENCE_CALIBRATION_DERIVATION_VERSION
            } else {
                explicitDerivationVersion
            }
            WorkoutDerivationSnapshot.derive(
                intervals = measuredIntervals,
                totalDuration = totalDuration,
                sessionStartedAt = sessionStartedAt,
                stepLength = profile,
                derivationVersion = derivationVersion,
                derivedAt = derivedAt,
                totalSteps = activity.steps,
                plausibility = existingSnapshot?.plausibility ?: WorkoutSeriesPlausibility(),
                derivationMethod = if (importRecord.parserVersion == WATCH_WORKOUT_D4_V4_PARSER_VERSION) {
                    WorkoutDerivationMethod.STEP_LENGTH_FALLBACK
                } else {
                    WorkoutDerivationMethod.FIXED_PERSONAL_STEP_LENGTH
                },
            ).valueOrNull() ?: return RepositoryResult.UnexpectedState
        }

        val splits = if (snapshot.isEligibleForFunctionalSplits) {
            val canonicalSeries = snapshot.canonicalSeries ?: return RepositoryResult.UnexpectedState
            WorkoutSplitCalculator.calculateOrNull(canonicalSeries, persistedHeartRateDomains)
                ?: return RepositoryResult.UnexpectedState
        } else {
            emptyList()
        }
        val missingSplitProjection = snapshot.isEligibleForFunctionalSplits &&
            splits.isNotEmpty() && activeRecord?.splits?.isEmpty() == true
        if (existingSnapshot != null &&
            existingSnapshot.stepLengthMetersUsed == snapshot.stepLengthMetersUsed &&
            existingSnapshot.stepLengthSource == snapshot.stepLengthSource &&
            existingSnapshot.derivationMethod == snapshot.derivationMethod &&
            existingSnapshot.distanceCalibrationFactor == snapshot.distanceCalibrationFactor &&
            existingSnapshot.derivationVersion == snapshot.derivationVersion &&
            !missingSplitProjection &&
            true
        ) {
            promoteCanonicalTreadmillIfNeeded(activity, existingSnapshot)
            return RepositoryResult.Success(existingSnapshot)
        }

        val resolvedRawIntervals = try {
            if (rawIntervals.size != snapshot.temporalResolution.intervals.size) {
                return RepositoryResult.UnexpectedState
            }
            rawIntervals.zip(snapshot.temporalResolution.intervals).map { (entity, resolved) ->
                entity.withEffectiveDuration(resolved.effectiveDuration)
            }
        } catch (_: ArithmeticException) {
            return RepositoryResult.UnexpectedState
        }
        val resolvedDerivationIntervals = prepareWatchWorkoutIntervalsForDerivation(
            parserVersion = importRecord.parserVersion,
            rawIntervals = resolvedRawIntervals,
        ) ?: return RepositoryResult.UnexpectedState
        val snapshotId = ids.next().toString()
        val snapshotEntity = snapshot.toEntity(
            id = snapshotId,
            activityId = activity.id,
            isActive = true,
        ) ?: return RepositoryResult.UnexpectedState
        val intervalEntities = snapshot.toDerivedIntervalEntities(snapshotId, generateIds(
            snapshot.canonicalSeries?.intervals?.size ?: 0,
        )) ?: return RepositoryResult.UnexpectedState
        val splitEntities = snapshot.toSplitEntities(
            snapshotId = snapshotId,
            splits = splits,
            ids = generateIds(splits.size),
        ) ?: return RepositoryResult.UnexpectedState
        val validatedSnapshot = snapshotEntity.toDomain(
            activityOrigin = WorkoutActivityOrigin.WATCH_D3_D4,
            activityStartedAt = Instant.ofEpochMilli(activity.startedAtEpochMillis),
            activityDurationNanos = activity.durationNanos,
            rawIntervals = resolvedDerivationIntervals,
            derivedIntervals = intervalEntities,
            splits = splitEntities,
            heartRateSamples = persistedHeartRateDomains,
            parserVersion = importRecord.parserVersion,
            confirmedType = confirmedType,
            parserValidity = parserValidity,
            d4V4Header = d4V4Header,
            d4V4Consistency = d4V4HeaderEntity?.toD4V4Consistency(),
            expectedCrc = importRecord.crc,
        ) ?: return RepositoryResult.UnexpectedState

        // Normalized timestamps are derivation-only. Persisted BPM facts and
        // timestamps remain unchanged by calibration.
        if (resolvedRawIntervals != rawIntervals) {
            database.workoutDetailDao().deleteIntervalsForActivity(activity.id)
            database.workoutDetailDao().insertIntervals(resolvedRawIntervals)
        }
        database.workoutDerivationDao().insertSnapshotWithChildren(
            snapshot = snapshotEntity,
            intervals = intervalEntities,
            splits = splitEntities,
        )
        // A transição ocorre somente após todos os writes do preflight. Se o CAS
        // de workflow falhar, a exceção atravessa a transação e desfaz o lote.
        promoteCanonicalTreadmillIfNeeded(activity, validatedSnapshot)
        return RepositoryResult.Success(validatedSnapshot)
    }

    private suspend fun promoteCanonicalTreadmillIfNeeded(
        activity: WorkoutActivityEntity,
        snapshot: WorkoutDerivationSnapshot,
    ) {
        if (activity.origin != WorkoutActivityOrigin.WATCH_D3_D4.name ||
            activity.confirmedType != WorkoutType.TREADMILL.name ||
            activity.workflowState != ActivityWorkflowState.PENDING_CALIBRATION.name ||
            !snapshot.isEligibleForCanonicalWorkflow
        ) return

        val transitioned = database.workoutActivityDao().transitionState(
            id = activity.id,
            expectedStates = listOf(ActivityWorkflowState.PENDING_CALIBRATION.name),
            nextState = ActivityWorkflowState.READY_TO_SYNC.name,
        )
        if (transitioned != 1) {
            throw WatchWorkoutDerivationPreflightException()
        }
    }

    private suspend fun findActiveDerivation(
        activity: WorkoutActivityEntity,
        importRecord: WatchWorkoutImportEntity?,
        intervals: List<WorkoutIntervalEntity>,
        samples: List<WorkoutHeartRateSampleEntity>,
        d4V4Header: WatchWorkoutD4V4HeaderEntity? = null,
    ): WorkoutDerivationSnapshot? {
        if (activity.origin != WorkoutActivityOrigin.WATCH_D3_D4.name ||
            importRecord == null ||
            intervals.hasPersistedUnvalidatedSourceOrder()
        ) return null
        val derivationIntervals = prepareWatchWorkoutIntervalsForDerivation(
            parserVersion = importRecord.parserVersion,
            rawIntervals = intervals,
        ) ?: return null
        val mappedSamples = samples.map { it.toDomain() }
        if (mappedSamples.any { it == null }) return null
        val confirmedType = activity.confirmedType
            ?.let { runCatching { WorkoutType.valueOf(it) }.getOrNull() }
        val persistedHeader = d4V4Header ?: database.watchWorkoutD4V4HeaderDao()
            .findByActivityId(activity.id)
        return database.workoutDerivationDao().findActiveForActivity(activity.id)?.toDomain(
            activityOrigin = WorkoutActivityOrigin.WATCH_D3_D4,
            activityStartedAt = Instant.ofEpochMilli(activity.startedAtEpochMillis),
            activityDurationNanos = activity.durationNanos,
            rawIntervals = derivationIntervals,
            heartRateSamples = mappedSamples.filterNotNull(),
            parserVersion = importRecord.parserVersion,
            confirmedType = confirmedType,
            parserValidity = runCatching { ParserValidity.valueOf(importRecord.parserValidity) }.getOrNull(),
            d4V4Header = persistedHeader?.toD4V4Header(),
            d4V4Consistency = persistedHeader?.toD4V4Consistency(),
            expectedCrc = importRecord.crc,
        )
    }

    /**
     * Returns denominator from persisted D4 facts only. Active canonical
     * derivation validates temporal eligibility and proves same interval/index
     * sequence used to distribute canonical distance.
     */
    private suspend fun canonicalDecodedStepsForCalibration(
        activity: WorkoutActivityEntity,
        importRecord: WatchWorkoutImportEntity?,
        rawIntervals: List<WorkoutIntervalEntity>,
    ): Long? {
        if (importRecord == null ||
            importRecord.parserValidity != ParserValidity.VALID.name ||
            importRecord.parserVersion !in setOf(
                WATCH_WORKOUT_D4_V2_PARSER_VERSION,
                WATCH_WORKOUT_D4_V3_PARSER_VERSION,
                WATCH_WORKOUT_D4_V4_PARSER_VERSION,
            ) ||
            rawIntervals.isEmpty() ||
            rawIntervals.any {
                if (importRecord.parserVersion == WATCH_WORKOUT_D4_V4_PARSER_VERSION) {
                    !hasValidD4V4NativeFacts(it)
                } else {
                    it.decodedSteps == null
                }
            }
        ) return null

        if (rawIntervals.hasPersistedUnvalidatedSourceOrder()) return null
        val totalDuration = runCatching { Duration.ofNanos(activity.durationNanos) }
            .getOrNull()
            ?.takeIf { it.isPositive() }
            ?: return null
        // Existing canonical rows may retain resolved durations from prior
        // derivation. Recompute eligibility from persisted D4 facts, not from
        // those derived durations.
        val sourceIntervals = rawIntervals.map { it.copy(effectiveDurationNanos = null) }
        val measuredIntervals = measuredIntervalsForDerivation(
            activityStartedAt = Instant.ofEpochMilli(activity.startedAtEpochMillis),
            rawIntervals = sourceIntervals,
            parserVersion = importRecord.parserVersion,
        ) ?: return null
        val canonicalResolution = WorkoutDurationResolver.resolve(
            totalDuration = totalDuration,
            intervals = measuredIntervals,
            sessionStartedAt = Instant.ofEpochMilli(activity.startedAtEpochMillis),
        )
        if (!canonicalResolution.isEligible) return null

        var totalDecodedSteps = 0L
        canonicalResolution.intervals.forEach { interval ->
            val decodedSteps = interval.measuredSteps ?: return null
            if (decodedSteps < 0L) return null
            totalDecodedSteps = try {
                Math.addExact(totalDecodedSteps, decodedSteps)
            } catch (_: ArithmeticException) {
                return null
            }
        }
        return totalDecodedSteps.takeIf { it > 0L }
    }

    /**
     * Evidence may be corrected only while every existing projection remains
     * local. Unknown child metadata also blocks: remote partial effects fail
     * closed instead of being inferred from aggregate activity state.
     */
    private suspend fun canMutateReferenceDistanceCalibration(activity: WorkoutActivityEntity): Boolean {
        val type = activity.confirmedType?.let { runCatching { WorkoutType.valueOf(it) }.getOrNull() }
        return activity.origin == WorkoutActivityOrigin.WATCH_D3_D4.name &&
            type in setOf(WorkoutType.RUNNING, WorkoutType.WALKING) &&
            activity.workflowState in setOf(
                ActivityWorkflowState.PENDING_CALIBRATION.name,
                ActivityWorkflowState.READY_TO_SYNC.name,
                ActivityWorkflowState.SYNC_FAILED.name,
            ) &&
            !hasUnsafeWorkoutProjectionChild(activity.id)
    }

    private suspend fun hasUnsafeWorkoutProjectionChild(activityId: String): Boolean {
        val operations = database.healthSyncOperationDao().findByAggregate(
            aggregateId = activityId,
            aggregateType = HealthSyncAggregateType.WORKOUT_ACTIVITY.name,
        )
        return operations.any { operation ->
            val domain = operation.toDomainStrict().valueOrNull()
                ?: return@any true
            if (domain.isProjectionChild && domain.state in setOf(
                    SyncOperationState.PENDING,
                    SyncOperationState.FAILED,
                )
            ) {
                // Payload congelado pode descrever snapshot anterior mesmo sem
                // efeito remoto observado. Não mutar evidência enquanto retry
                // local ainda puder publicar esse payload.
                return@any true
            }
            val isSameWorkoutAggregate = domain.key.aggregate.type == HealthSyncAggregateType.WORKOUT_ACTIVITY &&
                domain.key.aggregate.id.toString() == activityId
            val isAllowedState = domain.state in setOf(SyncOperationState.PENDING, SyncOperationState.FAILED)
            val hasNoAcceptedRemoteEffect = domain.reconciliationCheckpoint !=
                HealthProjectionReconciliationCheckpoint.AFTER_REMOTE_EFFECT &&
                domain.reconciliationState !in setOf(
                    HealthProjectionReconciliationState.SENT,
                    HealthProjectionReconciliationState.OBSOLETE,
                ) &&
                domain.syncedAt == null
            val child = domain.key.projectionChildKey
            val isLegacyOperation = operation.projectionChildKey == HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY
            val childMetadataIsConsistent = !isLegacyOperation && child != null && (
                domain.isProjectionChild &&
                    child.recordTypeName == domain.key.recordType.name &&
                    domain.clientRecordId == stableClientRecordId(domain.key) &&
                    domain.clientRecordId.isNotBlank() &&
                    domain.clientRecordVersion != null &&
                    domain.clientRecordVersion >= 0L &&
                    domain.frozenPayload != null &&
                    domain.frozenPayload.schemaVersion == operation.frozenPayloadVersion &&
                    domain.frozenPayload.serializedPayload.isNotBlank() &&
                    domain.projectionOrigin != null &&
                    domain.projectionOrigin.originId == activityId &&
                    domain.projectionOrigin.originVersion.isNotBlank()
            )

            !isSameWorkoutAggregate || !isAllowedState || !hasNoAcceptedRemoteEffect || !childMetadataIsConsistent
        }
    }

    /**
     * Destructive deletion is deliberately narrower than logical discard. It
     * accepts only one persisted Watch D3/D4 aggregate in a local state and
     * only queue rows whose remote effect is provably not started.
     */
    private suspend fun isSafeLocalWatchActivityDeletionCandidate(
        activity: WorkoutActivityEntity,
    ): Boolean {
        val activityId = activity.id
        val knownOrigin = runCatching { WorkoutActivityOrigin.valueOf(activity.origin) }.getOrNull()
            ?: return false
        if (knownOrigin !in setOf(
                WorkoutActivityOrigin.WATCH_D3_D4,
                WorkoutActivityOrigin.PHONE_TREADMILL,
                WorkoutActivityOrigin.PHONE_FREE_WORKOUT,
            )
        ) return false
        if (database.workoutActivityDao().countById(activityId) != 1 ||
            activity.workflowState !in setOf(
                ActivityWorkflowState.PENDING_REVIEW.name,
                ActivityWorkflowState.PENDING_CALIBRATION.name,
                ActivityWorkflowState.READY_TO_SYNC.name,
                ActivityWorkflowState.SYNC_FAILED.name,
                ActivityWorkflowState.DISCARDED.name,
            ) ||
            activity.appWorkoutSessionId != null
        ) return false

        val importRecord = database.watchWorkoutImportDao().findByActivityId(activityId)
        if (database.watchWorkoutImportDao().countByActivityId(activityId) > 1 ||
            database.appWorkoutSessionDao().findByWorkoutActivityId(activityId) != null
        ) return false
        if (importRecord != null && importRecord.origin != WorkoutActivityOrigin.WATCH_D3_D4.name) return false

        val diagnosticPayloadIds = buildSet {
            activity.diagnosticPayloadId?.let(::add)
            importRecord?.diagnosticPayloadId?.let(::add)
        }
        if (diagnosticPayloadIds.any { database.diagnosticPayloadDao().findById(it) == null }) return false

        return database.healthSyncOperationDao().findByAggregate(
            aggregateId = activityId,
            aggregateType = HealthSyncAggregateType.WORKOUT_ACTIVITY.name,
        ).all { operationEntity ->
            val operation = operationEntity.toDomainStrict().valueOrNull() ?: return@all false
            operation.key.aggregate.type == HealthSyncAggregateType.WORKOUT_ACTIVITY &&
                operation.key.aggregate.id.toString() == activityId &&
                operation.action == HealthSyncOperationAction.INSERT &&
                operation.state in setOf(SyncOperationState.PENDING, SyncOperationState.FAILED) &&
                operation.syncedAt == null &&
                operation.reconciliationState == HealthProjectionReconciliationState.EXPECTED &&
                operation.reconciliationCheckpoint == HealthProjectionReconciliationCheckpoint.NOT_STARTED
        }
    }

    private fun normalizeWatchHeartRateSamplesForDerivation(
        parserVersion: String,
        activityStartedAt: Instant,
        totalDuration: Duration,
        rawIntervals: List<WorkoutIntervalEntity>,
        samples: List<WorkoutHeartRateSampleEntity>,
    ): List<WorkoutHeartRateSampleEntity>? {
        if (samples.isEmpty()) return emptyList()
        val derivationIntervals = prepareWatchWorkoutIntervalsForDerivation(
            parserVersion = parserVersion,
            rawIntervals = rawIntervals,
        ) ?: return null
        val measuredIntervals = derivationIntervals.map { entity ->
            entity.copy(effectiveDurationNanos = null).toMeasuredWorkoutInterval(
                parserVersion = parserVersion,
            )
                ?: return null
        }
        val resolution = WorkoutDurationResolver.resolve(
            totalDuration = totalDuration,
            intervals = measuredIntervals,
            sessionStartedAt = activityStartedAt,
        )
        if (!resolution.isEligible) return null
        val intervalsByIndex = resolution.intervals.associateBy { it.index }
        val usedIndexes = mutableSetOf<Int>()
        return samples.map { sample ->
            val sourceIndex = sample.sourceSampleIndex ?: recoverLegacyHeartRateSourceIndex(
                sample = sample,
                activityStartedAt = activityStartedAt,
                intervalIndexes = intervalsByIndex.keys,
            ) ?: return null
            if (!usedIndexes.add(sourceIndex)) return null
            val interval = intervalsByIndex[sourceIndex] ?: return null
            val window = interval.effectiveWindow ?: return null
            val duration = interval.effectiveDuration ?: return null
            val normalizedAt = window.start.plus(duration.dividedBy(2))
            sample.copy(
                sampledAtEpochMillis = normalizedAt.toEpochMilli(),
                sourceSampleIndex = sourceIndex,
                sampledAtEpochSecond = normalizedAt.epochSecond,
                sampledAtNano = normalizedAt.nano,
            )
        }
    }

    private fun recoverLegacyHeartRateSourceIndex(
        sample: WorkoutHeartRateSampleEntity,
        activityStartedAt: Instant,
        intervalIndexes: Set<Int>,
    ): Int? {
        val sampledAt = sample.toDomain()?.sampledAt ?: return null
        val elapsedNanos = runCatching {
            Duration.between(activityStartedAt, sampledAt).toNanos()
        }.getOrNull() ?: return null
        val nominalNanos = WorkoutDurationResolver.DEFAULT_INTERVAL_DURATION.toNanos()
        if (elapsedNanos < 0L || elapsedNanos % nominalNanos != 0L) return null
        val index = elapsedNanos / nominalNanos
        if (index > Int.MAX_VALUE.toLong()) return null
        return index.toInt().takeIf(intervalIndexes::contains)
    }

    private fun measuredIntervalsForDerivation(
        activityStartedAt: Instant,
        rawIntervals: List<WorkoutIntervalEntity>,
        parserVersion: String,
    ): List<MeasuredWorkoutInterval>? {
        val derivationIntervals = prepareWatchWorkoutIntervalsForDerivation(
            parserVersion = parserVersion,
            rawIntervals = rawIntervals,
        ) ?: return null
        var nextWindowStart = activityStartedAt
        return derivationIntervals.map { entity ->
            val measured = entity.toMeasuredWorkoutInterval(
                effectiveWindowStart = nextWindowStart,
                parserVersion = parserVersion,
            )
                ?: return null
            entity.effectiveDurationNanos?.let { nanos ->
                nextWindowStart = runCatching { nextWindowStart.plusNanos(nanos) }.getOrNull()
                    ?: return null
            }
            measured
        }
    }

    private fun sameWatchWorkoutInputs(
        existingActivity: WorkoutActivityEntity,
        incomingActivity: WorkoutActivityEntity,
        existingImport: WatchWorkoutImportEntity?,
        incomingImport: WatchWorkoutImportEntity,
        existingIntervals: List<WorkoutIntervalEntity>,
        incomingIntervals: List<WorkoutIntervalEntity>,
        existingHeartRateSamples: List<WorkoutHeartRateSampleEntity>,
        incomingHeartRateSamples: List<WorkoutHeartRateSampleEntity>,
        existingD4V4Header: WatchWorkoutD4V4HeaderEntity?,
        incomingD4V4Header: WatchWorkoutD4V4HeaderEntity?,
    ): Boolean {
        if (existingImport == null ||
            existingImport.deviceId != incomingImport.deviceId ||
            existingImport.origin != incomingImport.origin ||
            existingImport.crc != incomingImport.crc ||
            existingImport.externalBlockIndex != incomingImport.externalBlockIndex ||
            existingImport.parserVersion != incomingImport.parserVersion ||
            existingImport.parserValidity != incomingImport.parserValidity ||
            existingActivity.origin != incomingActivity.origin ||
            existingActivity.deviceId != incomingActivity.deviceId ||
            existingActivity.startedAtEpochMillis != incomingActivity.startedAtEpochMillis ||
            existingActivity.endedAtEpochMillis != incomingActivity.endedAtEpochMillis ||
            existingActivity.durationNanos != incomingActivity.durationNanos ||
            existingActivity.steps != incomingActivity.steps ||
            existingActivity.caloriesKilocalories != incomingActivity.caloriesKilocalories ||
            existingActivity.originalDistanceMeters != incomingActivity.originalDistanceMeters ||
            existingActivity.estimatedDistanceMeters != incomingActivity.estimatedDistanceMeters ||
            existingActivity.importedAveragePaceSecondsPerKilometer != incomingActivity.importedAveragePaceSecondsPerKilometer ||
            existingActivity.importedAverageSpeedMetersPerSecond != incomingActivity.importedAverageSpeedMetersPerSecond ||
            existingActivity.averageBpm != incomingActivity.averageBpm ||
            existingActivity.minimumBpm != incomingActivity.minimumBpm ||
            existingActivity.maximumBpm != incomingActivity.maximumBpm ||
            existingActivity.stepsAvailable != incomingActivity.stepsAvailable ||
            existingIntervals.size != incomingIntervals.size ||
            existingHeartRateSamples.size != incomingHeartRateSamples.size
        ) return false
        if (!existingIntervals.zip(incomingIntervals).all { (existing, incoming) ->
            existing.intervalIndex == incoming.intervalIndex &&
                existing.durationNanos == incoming.durationNanos &&
                existing.steps == incoming.steps &&
                existing.rawMotionValue == incoming.rawMotionValue &&
                existing.rawDistanceValue == incoming.rawDistanceValue &&
                existing.rawCaloriesValue == incoming.rawCaloriesValue &&
                existing.decodedSteps == incoming.decodedSteps &&
                existing.nativeSportValue == incoming.nativeSportValue &&
                existing.nativeSteps == incoming.nativeSteps &&
                existing.nativeDistanceMeters == incoming.nativeDistanceMeters &&
                existing.nativeCaloriesRaw == incoming.nativeCaloriesRaw &&
                existing.nativePauseStateRaw == incoming.nativePauseStateRaw &&
                existing.temporalEligibilityReason == incoming.temporalEligibilityReason
        }) return false
        if (existingD4V4Header?.toD4V4Header() != incomingD4V4Header?.toD4V4Header()) return false
        return existingHeartRateSamples.zip(incomingHeartRateSamples).all { (existing, incoming) ->
            existing.sampledAtEpochMillis == incoming.sampledAtEpochMillis &&
                existing.sampledAtEpochSecond == incoming.sampledAtEpochSecond &&
                existing.sampledAtNano == incoming.sampledAtNano &&
                existing.sourceSampleIndex == incoming.sourceSampleIndex &&
                existing.bpm == incoming.bpm
        }
    }

    private suspend fun isBlockedWatchWorkoutAggregate(aggregate: HealthSyncAggregate): Boolean {
        val workout = aggregate as? HealthSyncAggregate.WorkoutActivity ?: return false
        return database.workoutActivityDao().findById(workout.activityId.value.toString())?.origin ==
            WorkoutActivityOrigin.WATCH_D3_D4.name
    }

    private suspend fun isBlockedWatchWorkoutOperation(entity: HealthSyncOperationEntity): Boolean {
        if (entity.aggregateType != HealthSyncAggregateType.WORKOUT_ACTIVITY.name) return false
        return entity.projectionChildKey == HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY &&
            entity.action != HealthSyncOperationAction.DELETE.name &&
            database.workoutActivityDao().findById(entity.aggregateId)?.origin ==
                WorkoutActivityOrigin.WATCH_D3_D4.name
    }

    private suspend fun mutateSession(
        sessionId: AppWorkoutSessionId,
        expectedRevision: Long,
        transition: (AppWorkoutSession) -> DomainResult<AppWorkoutSession>,
    ): RepositoryResult<AppWorkoutSession> {
        val dao = database.appWorkoutSessionDao()
        val entity = dao.findById(sessionId.value.toString()) ?: return RepositoryResult.NotFound
        if (entity.revision != expectedRevision) return RepositoryResult.Conflict
        val current = entity.toDomain() ?: return RepositoryResult.UnexpectedState
        val updated = transition(current).valueOrNull() ?: return RepositoryResult.UnexpectedState
        if (updated == current) return RepositoryResult.Success(current)
        return if (dao.updateCas(updated.toEntity(), expectedRevision) == 1) {
            RepositoryResult.Success(updated)
        } else {
            RepositoryResult.Conflict
        }
    }

    private suspend fun AppWorkoutSessionDao.updateCas(
        next: AppWorkoutSessionEntity,
        expectedRevision: Long,
    ): Int = updateCasFields(
        id = next.id,
        type = next.type,
        createdAtEpochMillis = next.createdAtEpochMillis,
        startedAtEpochMillis = next.startedAtEpochMillis,
        endedAtEpochMillis = next.endedAtEpochMillis,
        activeDurationNanos = next.activeDurationNanos,
        pausedDurationNanos = next.pausedDurationNanos,
        activeSinceEpochMillis = next.activeSinceEpochMillis,
        pausedSinceEpochMillis = next.pausedSinceEpochMillis,
        state = next.state,
        steps = next.steps,
        distanceMeters = next.distanceMeters,
        distanceAbsenceReason = next.distanceAbsenceReason,
        caloriesKilocalories = next.caloriesKilocalories,
        caloriesAbsenceReason = next.caloriesAbsenceReason,
        averageBpm = next.averageBpm,
        averageBpmAbsenceReason = next.averageBpmAbsenceReason,
        minimumBpm = next.minimumBpm,
        minimumBpmAbsenceReason = next.minimumBpmAbsenceReason,
        maximumBpm = next.maximumBpm,
        maximumBpmAbsenceReason = next.maximumBpmAbsenceReason,
        workoutActivityId = next.workoutActivityId,
        lastTransitionAtEpochMillis = next.lastTransitionAtEpochMillis,
        associatedDeviceId = next.associatedDeviceId,
        revision = next.revision,
        runtimeGeneration = next.runtimeGeneration,
        stepsAvailable = next.stepsAvailable,
        expectedRevision = expectedRevision,
    )

    private suspend fun hasConsistentPhoneActivityOwnership(
        session: AppWorkoutSessionEntity,
        activity: br.com.mwconsulting.championcompanion.core.database.WorkoutActivityEntity,
    ): Boolean {
        val expectedOrigin = when (session.type) {
            WorkoutType.WALKING.name, WorkoutType.RUNNING.name -> "PHONE_GPS"
            WorkoutType.TREADMILL.name -> "PHONE_TREADMILL"
            WorkoutType.FREE_WORKOUT.name -> "PHONE_FREE_WORKOUT"
            else -> return false
        }
        if (activity.origin != expectedOrigin || activity.suggestedType != session.type ||
            activity.appWorkoutSessionId != session.id || activity.deviceId != session.associatedDeviceId ||
            session.workoutActivityId != null && session.workoutActivityId != activity.id
        ) return false
        val reverseOwner = database.appWorkoutSessionDao().findByWorkoutActivityId(activity.id)
        return reverseOwner == null || reverseOwner.id == session.id
    }

    private fun isValidRouteAppend(
        route: WorkoutRoute,
        sessionStartedAt: Instant,
        input: AppendWorkoutRoutePointsInput,
        lastPoint: br.com.mwconsulting.championcompanion.core.database.WorkoutRoutePointEntity?,
        persistedSegmentStart: br.com.mwconsulting.championcompanion.core.database.WorkoutRoutePointEntity?,
        persistedPointCount: Long,
    ): Boolean {
        val previous = route.metrics
        if (persistedPointCount != previous.acceptedPointCount ||
            (persistedPointCount == 0L) != (lastPoint == null) ||
            (persistedPointCount == 0L) != (previous.averageHorizontalAccuracyMeters == null) ||
            previous.segmentCount != (lastPoint?.segmentIndex?.plus(1) ?: 0) ||
            !hasValidPersistedRouteAppendTimes(input, lastPoint?.sampledAtEpochMillis)
        ) return false

        val next = input.metrics
        if (next.acceptedPointCount != persistedPointCount + input.points.size ||
            next.segmentCount != input.points.last().segmentIndex + 1
        ) return false

        val firstPoint = input.points.first()
        if (route.startedAt != sessionStartedAt ||
            firstPoint.sampledAt.isBefore(route.startedAt) ||
            firstPoint.sampledAt.isBefore(sessionStartedAt) ||
            input.segments.any { segment ->
                segment.startedAt.isBefore(route.startedAt) ||
                    segment.startedAt.isBefore(sessionStartedAt)
            }
        ) return false

        val continuedSegment = input.segments.firstOrNull { it.index == lastPoint?.segmentIndex }
        val previousSegmentDistance = when {
            continuedSegment == null -> 0.0
            lastPoint?.segmentDistanceMeters == null -> return false
            continuedSegment.distanceMeters < lastPoint.segmentDistanceMeters -> return false
            else -> lastPoint.segmentDistanceMeters
        }
        val batchDistance = input.segments.sumOf { it.distanceMeters } - previousSegmentDistance
        val expectedDistance = previous.distanceMeters + batchDistance
        if (!approximatelyEqual(next.distanceMeters, expectedDistance)) return false

        if (input.segments.zipWithNext().any { (previous, nextSegment) ->
                nextSegment.startedAt.isBefore(previous.endedAt)
            } ||
            lastPoint != null && input.segments.any { segment ->
                segment.index > lastPoint.segmentIndex && segment.startedAt.isBefore(
                    Instant.ofEpochMilli(lastPoint.sampledAtEpochMillis),
                )
            }
        ) return false

        val accuracyTotal = (previous.averageHorizontalAccuracyMeters ?: 0.0) * persistedPointCount +
            input.points.sumOf { it.horizontalAccuracyMeters }
        val expectedAverageAccuracy = accuracyTotal / next.acceptedPointCount
        if (next.averageHorizontalAccuracyMeters == null ||
            !approximatelyEqual(next.averageHorizontalAccuracyMeters, expectedAverageAccuracy)
        ) return false

        return input.segments.all { segment ->
            val segmentPoints = input.points.filter { it.segmentIndex == segment.index }
            val continuesPersistedSegment = lastPoint?.segmentIndex == segment.index
            val hasValidReason = if (segment.index == 0) {
                segment.reason == WorkoutRouteSegmentReason.SESSION_START
            } else {
                segment.reason != WorkoutRouteSegmentReason.SESSION_START
            }
            hasValidReason && segmentPoints.isNotEmpty() &&
                segment.lastSequence == segmentPoints.last().sequence &&
                segment.endedAt == segmentPoints.last().sampledAt &&
                if (continuesPersistedSegment) {
                    persistedSegmentStart != null &&
                        persistedSegmentStart.segmentStartReason == segment.reason.name &&
                        persistedSegmentStart.segmentStartedAtEpochMillis == segment.startedAt.toEpochMilli() &&
                        segment.firstSequence == persistedSegmentStart.sequence &&
                        segment.firstSequence <= requireNotNull(lastPoint).sequence
                } else {
                    segment.firstSequence == segmentPoints.first().sequence &&
                        !segment.startedAt.isAfter(segmentPoints.first().sampledAt)
                }
        }
    }

    private fun hasValidPersistedRouteAppendTimes(
        input: AppendWorkoutRoutePointsInput,
        lastSampledAtEpochMillis: Long?,
    ): Boolean {
        val cursorEpochMillis = input.cursor.lastSampledAt?.let {
            it.toEpochMilliOrNull() ?: return false
        }
        if (cursorEpochMillis != lastSampledAtEpochMillis ||
            input.segments.any { it.startedAt.toEpochMilliOrNull() == null }
        ) return false

        var previousEpochMillis = lastSampledAtEpochMillis
        for (point in input.points) {
            val currentEpochMillis = point.sampledAt.toEpochMilliOrNull() ?: return false
            val previous = previousEpochMillis
            if (previous != null && currentEpochMillis <= previous) return false
            previousEpochMillis = currentEpochMillis
        }
        return true
    }

    private fun Instant.toEpochMilliOrNull(): Long? = try {
        val epochMillis = toEpochMilli()
        epochMillis.takeIf { Instant.ofEpochMilli(it) == this }
    } catch (_: ArithmeticException) {
        null
    }

    private fun approximatelyEqual(first: Double, second: Double): Boolean {
        if (!first.isFinite() || !second.isFinite()) return false
        val scale = maxOf(1.0, kotlin.math.abs(first), kotlin.math.abs(second))
        return kotlin.math.abs(first - second) <= ROUTE_METRIC_EPSILON * scale
    }

    private fun restoreWorkoutRouteRecoverySnapshot(
        route: WorkoutRoute,
        pointEntities: List<WorkoutRoutePointEntity>,
    ): WorkoutRouteRecoverySnapshot? {
        if (pointEntities.size.toLong() != route.metrics.acceptedPointCount) return null
        if (pointEntities.isEmpty()) {
            if (route.metrics.segmentCount != 0 || route.metrics.distanceMeters != 0.0 ||
                route.metrics.averageHorizontalAccuracyMeters != null
            ) return null
            return WorkoutRouteRecoverySnapshot(
                route = route,
                cursor = WorkoutRouteAppendCursor(route.id, null, null, null),
                segments = emptyList(),
                lastAcceptedPoint = null,
            )
        }

        val points = pointEntities.map { it.toDomain() ?: return null }
        if (points.any { it.routeId != route.id } ||
            points.first().sequence != 0L ||
            points.zipWithNext().any { (first, second) ->
                second.sequence != first.sequence + 1L ||
                    !second.sampledAt.isAfter(first.sampledAt) ||
                    second.segmentIndex !in first.segmentIndex..(first.segmentIndex + 1)
            } ||
            points.first().segmentIndex != 0 ||
            points.first().sampledAt.isBefore(route.startedAt) ||
            route.endedAt?.let(points.last().sampledAt::isAfter) == true
        ) return null

        val expectedSegmentIndices = (0 until route.metrics.segmentCount).toList()
        if (pointEntities.map { it.segmentIndex }.distinct() != expectedSegmentIndices) return null
        val recordsBySegment = pointEntities.zip(points).groupBy { (entity, _) -> entity.segmentIndex }
        val segments = expectedSegmentIndices.map { segmentIndex ->
            val records = recordsBySegment[segmentIndex] ?: return null
            val firstEntity = records.first().first
            val firstPoint = records.first().second
            val lastEntity = records.last().first
            val lastPoint = records.last().second
            val reasonName = firstEntity.segmentStartReason ?: return null
            val reason = enumValueOfOrNull<WorkoutRouteSegmentReason>(reasonName) ?: return null
            val startedAtEpochMillis = firstEntity.segmentStartedAtEpochMillis ?: return null
            val startedAt = Instant.ofEpochMilli(startedAtEpochMillis)
            if (records.drop(1).any { (entity, _) ->
                    entity.segmentStartReason != null || entity.segmentStartedAtEpochMillis != null
                } ||
                (segmentIndex == 0) != (reason == WorkoutRouteSegmentReason.SESSION_START) ||
                startedAt.isAfter(firstPoint.sampledAt)
            ) return null

            val distanceCheckpoints = records.mapNotNull { (entity, _) -> entity.segmentDistanceMeters }
            val distanceMeters = lastEntity.segmentDistanceMeters ?: return null
            if (distanceCheckpoints.any { !it.isFinite() || it < 0.0 } ||
                distanceCheckpoints.zipWithNext().any { (first, second) -> second < first }
            ) return null
            WorkoutRouteSegment(
                routeId = route.id,
                index = segmentIndex,
                reason = reason,
                firstSequence = firstPoint.sequence,
                lastSequence = lastPoint.sequence,
                startedAt = startedAt,
                endedAt = lastPoint.sampledAt,
                distanceMeters = distanceMeters,
            )
        }
        if (segments.first().startedAt.isBefore(route.startedAt) ||
            segments.zipWithNext().any { (previous, next) -> next.startedAt.isBefore(previous.endedAt) }
        ) return null

        val expectedDistance = segments.sumOf { it.distanceMeters }
        val expectedAverageAccuracy = points.sumOf { it.horizontalAccuracyMeters } / points.size
        if (!approximatelyEqual(route.metrics.distanceMeters, expectedDistance) ||
            route.metrics.averageHorizontalAccuracyMeters?.let {
                approximatelyEqual(it, expectedAverageAccuracy)
            } != true
        ) return null

        val lastPoint = points.last()
        return WorkoutRouteRecoverySnapshot(
            route = route,
            cursor = WorkoutRouteAppendCursor(
                routeId = route.id,
                lastSequence = lastPoint.sequence,
                lastSampledAt = lastPoint.sampledAt,
                currentSegmentIndex = lastPoint.segmentIndex,
            ),
            segments = segments,
            lastAcceptedPoint = lastPoint,
        )
    }

    private suspend fun WorkoutRouteDao.updateCas(
        next: WorkoutRouteEntity,
        expectedRevision: Long,
    ): Int = updateCasFields(
        id = next.id,
        sessionId = next.sessionId,
        workoutType = next.workoutType,
        state = next.state,
        startedAtEpochMillis = next.startedAtEpochMillis,
        endedAtEpochMillis = next.endedAtEpochMillis,
        acceptedPointCount = next.acceptedPointCount,
        segmentCount = next.segmentCount,
        distanceMeters = next.distanceMeters,
        averageHorizontalAccuracyMeters = next.averageHorizontalAccuracyMeters,
        revision = next.revision,
        expectedRevision = expectedRevision,
    )

    private suspend fun transitionWorkoutSync(
        activityId: WorkoutActivityId,
        transition: (WorkoutActivity) -> br.com.mwconsulting.championcompanion.core.model.DomainResult<WorkoutActivity>,
    ): RepositoryResult<WorkoutActivity> {
        val entity = database.workoutActivityDao().findById(activityId.value.toString()) ?: return RepositoryResult.NotFound
        if (entity.origin == WorkoutActivityOrigin.WATCH_D3_D4.name) {
            val hasPreparedChild = database.healthSyncOperationDao()
                .findByAggregate(
                    aggregateId = activityId.value.toString(),
                    aggregateType = HealthSyncAggregateType.WORKOUT_ACTIVITY.name,
                )
                .any {
                    it.projectionChildKey != HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY &&
                        it.frozenPayload != null
                }
            if (!hasPreparedChild) return RepositoryResult.UnexpectedState
        }
        val activity = loadWorkoutActivity(entity) ?: return RepositoryResult.UnexpectedState
        val updated = transition(activity).valueOrNull() ?: return RepositoryResult.UnexpectedState
        database.workoutActivityDao().update(updated.toEntity())
        return RepositoryResult.Success(updated)
    }

    private fun createWatchDailySummary(input: MergeWatchDailySummaryInput): DailySummary? = when (
        val created = DailySummary.create(
            id = DailySummaryId(ids.next()),
            key = br.com.mwconsulting.championcompanion.core.model.DailySummaryKey(
                deviceId = input.deviceId,
                origin = DataOrigin.WATCH_DAILY_SUMMARY,
                localDate = input.localDate,
            ),
            steps = input.incomingSteps,
            estimatedDistanceMeters = input.estimatedDistanceMeters,
            estimatedCaloriesKilocalories = input.estimatedCaloriesKilocalories,
            collectedAt = input.collectedAt,
            updatedAt = input.updatedAt,
            validity = br.com.mwconsulting.championcompanion.core.model.DataValidity.VALID,
        )
    ) {
        is DomainResult.Success -> created.value
        is DomainResult.Failure -> null
    }

    private fun br.com.mwconsulting.championcompanion.core.database.WeightMeasurementEntity.toKilogramsOrNull(): Double? = when (unit) {
        "KILOGRAMS" -> value.takeIf { it.isFinite() && it > 0.0 }
        "POUNDS" -> (value * 0.45359237).takeIf { value.isFinite() && value > 0.0 }
        else -> null
    }

    private fun WeightMeasurement.toKilogramsOrNull(): Double? = when (unit) {
        WeightUnit.KILOGRAMS -> value.takeIf { it.isFinite() && it > 0.0 }
        WeightUnit.POUNDS -> (value * 0.45359237).takeIf { it.isFinite() && it > 0.0 }
    }

    private inline fun <reified T : Enum<T>> enumValueOfOrNull(value: String): T? =
        runCatching { enumValueOf<T>(value) }.getOrNull()

    private suspend fun ensureDailyStepsOperationInternal(
        key: HealthSyncOperationKey,
    ): RepositoryResult<HealthSyncOperation> {
        val aggregate = key.aggregate as? HealthSyncAggregate.DailySummary ?: return RepositoryResult.NotFound
        val summary = database.dailySummaryDao().findById(aggregate.summaryId.value.toString())
            ?: return RepositoryResult.NotFound
        if (summary.origin != DataOrigin.WATCH_DAILY_SUMMARY.name ||
            summary.validity != DataValidity.VALID.name ||
            summary.deviceId == null
        ) return RepositoryResult.NotFound

        val operations = findDailyStepsEntities(summary.id)
        val timeZoneId = summary.collectedZoneId.validZoneIdOrNull()
            ?: operations.firstNotNullOfOrNull { it.timeZoneId.validZoneIdOrNull() }
            ?: ZoneId.systemDefault().id
        val result = enqueueDailyStepsInternal(
            summary = summary,
            collectedAtEpochMillis = summary.collectedAtEpochMillis,
            timeZoneId = timeZoneId,
            updatedAtEpochMillis = summary.updatedAtEpochMillis,
            allowNewOperation = false,
        )
        return result.operation?.let { RepositoryResult.Success(it) } ?: RepositoryResult.NotFound
    }

    private suspend fun enqueueDailyStepsInternal(
        summary: DailySummaryEntity,
        collectedAtEpochMillis: Long,
        timeZoneId: String,
        updatedAtEpochMillis: Long,
        allowNewOperation: Boolean = true,
    ): DailyStepsEnqueueResult {
        if (summary.origin != DataOrigin.WATCH_DAILY_SUMMARY.name ||
            summary.validity != DataValidity.VALID.name ||
            summary.deviceId == null ||
            summary.steps <= 0L
        ) return DailyStepsEnqueueResult()
        val syncDao = database.healthSyncOperationDao()
        var entities = findDailyStepsEntities(summary.id)
        val persistedTimeZoneId = entities.asSequence()
            .mapNotNull { it.timeZoneId.validZoneIdOrNull() }
            .firstOrNull()
            ?: timeZoneId.validZoneIdOrNull()
            ?: return DailyStepsEnqueueResult()
        val zoneId = ZoneId.of(persistedTimeZoneId)
        val localDate = runCatching { LocalDate.parse(summary.localDateIso) }.getOrNull()
            ?: return DailyStepsEnqueueResult()
        val cutover = when (val cutoverEntity = database.healthSyncCutoverDao().findByDeviceId(summary.deviceId)) {
            null -> null
            else -> cutoverEntity.toDomain() ?: return DailyStepsEnqueueResult()
        }
        if (cutover != null && localDate >= cutover.localDate) {
            // D1 granular projection remains deferred until its remote interval is
            // provably safe. Never create or mutate DailySummary projection after cutover.
            return DailyStepsEnqueueResult(
                operation = entities.firstOrNull()?.toDomain(),
            )
        }
        val windowEnd = resolveDailyStepsWindowEnd(summary, collectedAtEpochMillis, zoneId)
            ?: return DailyStepsEnqueueResult()
        val dayStart = runCatching {
            LocalDate.parse(summary.localDateIso).atStartOfDay(zoneId).toInstant()
        }.getOrNull() ?: return DailyStepsEnqueueResult()

        val legacyEntities = entities.filter { it.isUnfrozenLegacyDailyStepsOperation() }
        var payloadOperations = entities.mapNotNull { entity ->
            entity.toDomain()?.dailyStepsPayload?.let { payload -> entity to payload }
        }
        if (legacyEntities.size > 1 ||
            (legacyEntities.isNotEmpty() && payloadOperations.isNotEmpty()) ||
            payloadOperations.size + legacyEntities.size != entities.size
        ) return DailyStepsEnqueueResult()

        val legacy = legacyEntities.singleOrNull()
        if (legacy != null) {
            if (legacy.state == SyncOperationState.SYNCING.name || !windowEnd.isAfter(dayStart)) {
                return DailyStepsEnqueueResult(operation = legacy.toDomain())
            }
            val legacyCreditEvaluation = findEligibleDailyWorkoutCredits(summary, zoneId)
                ?: return DailyStepsEnqueueResult()
            if (legacyCreditEvaluation.credits.isNotEmpty()) {
                // A legacy cumulative identity cannot safely be repartitioned
                // around an already accepted activity. Preserve both payloads
                // and wait for explicit reconciliation instead of inflating.
                return DailyStepsEnqueueResult()
            }
            val payload = DailyStepsSyncPayload(
                kind = DailyStepsSyncPayloadKind.LEGACY_BRIDGE,
                sequence = 0L,
                previousTotalSteps = 0L,
                currentTotalSteps = summary.steps,
                stepDelta = summary.steps,
                startTime = dayStart,
                endTime = windowEnd,
                timeZoneId = zoneId.id,
            )
            val bridgeVersion = maxOf(legacy.clientRecordVersion ?: Long.MIN_VALUE, updatedAtEpochMillis)
            val identityOrVersionChanged = legacy.clientRecordId == null ||
                legacy.clientRecordVersion != bridgeVersion
            val bridge = legacy.copy(
                state = if (identityOrVersionChanged) SyncOperationState.PENDING.name else legacy.state,
                attemptCount = if (identityOrVersionChanged) 0 else legacy.attemptCount,
                lastAttemptAtEpochMillis = if (identityOrVersionChanged) null else legacy.lastAttemptAtEpochMillis,
                lastError = if (identityOrVersionChanged) null else legacy.lastError,
                clientRecordId = legacy.clientRecordId ?: stableClientRecordId(
                    HealthSyncOperationKey(
                        aggregate = HealthSyncAggregate.DailySummary(DailySummaryId(parseUuid(summary.id)
                            ?: return DailyStepsEnqueueResult())),
                        recordType = HealthSyncRecordType.STEPS,
                    ),
                ),
                clientRecordVersion = bridgeVersion,
                syncedAtEpochMillis = if (identityOrVersionChanged) null else legacy.syncedAtEpochMillis,
                timeZoneId = payload.timeZoneId,
                updatedAtEpochMillis = maxOf(legacy.updatedAtEpochMillis ?: Long.MIN_VALUE, updatedAtEpochMillis),
                operationSequence = payload.sequence,
                dailyStepsPayloadKind = payload.kind.name,
                previousTotalSteps = payload.previousTotalSteps,
                currentTotalSteps = payload.currentTotalSteps,
                stepDelta = payload.stepDelta,
                stepStartTimeEpochMillis = payload.startTime.toEpochMilli(),
                stepEndTimeEpochMillis = payload.endTime.toEpochMilli(),
            )
            if (syncDao.update(bridge) != 1) return DailyStepsEnqueueResult()
            entities = listOf(bridge)
            payloadOperations = entities.mapNotNull { entity ->
                entity.toDomain()?.dailyStepsPayload?.let { payload -> entity to payload }
            }
        }

        val orderedPayloadOperations = payloadOperations.sortedBy { (_, payload) -> payload.sequence }
        val checkpoint = resolveDailyStepsCheckpoint(
            summary = summary,
            zoneId = zoneId,
            dayStart = dayStart,
            payloadOperations = orderedPayloadOperations,
        ) ?: return DailyStepsEnqueueResult()

        val creditEvaluation = findEligibleDailyWorkoutCredits(summary, zoneId)
            ?: return DailyStepsEnqueueResult()
        val activeCredits = creditEvaluation.credits
        val latest = orderedPayloadOperations.lastOrNull()
        val pendingOperation = orderedPayloadOperations.firstOrNull { (entity, _) ->
            entity.state == SyncOperationState.PENDING.name ||
                entity.state == SyncOperationState.FAILED.name ||
                entity.state == SyncOperationState.SYNCING.name
        }
        if (pendingOperation != null) {
            val (entity, _) = pendingOperation
            val reservations = database.dailyStepsCreditReservationDao()
                .findByDailyOperationId(entity.id)
            if (entity.state == SyncOperationState.SYNCING.name) {
                return DailyStepsEnqueueResult(operation = entity.toDomain())
            }
            if (reservations.isNotEmpty()) {
                if (!isValidDailyStepsReservationSet(
                        operation = entity,
                        reservations = reservations,
                        activeCredits = activeCredits,
                    )
                ) return DailyStepsEnqueueResult()
                return DailyStepsEnqueueResult(operation = entity.toDomain())
            }
            // An unaccepted Daily operation is already the only safe retry
            // candidate. Never create a second interval around active credits.
            if (activeCredits.isNotEmpty()) return DailyStepsEnqueueResult()
            return DailyStepsEnqueueResult(operation = entity.toDomain())
        }

        if (hasUnacceptedWorkoutStepsForDaily(summary, zoneId, dayStart)) {
            // A pending workout StepsRecord may still become the remote effect
            // that Daily must credit. Do not publish Daily before that outcome.
            return DailyStepsEnqueueResult()
        }

        if (hasAcceptedDailyCoverageOverlap(orderedPayloadOperations, activeCredits)) {
            return DailyStepsEnqueueResult()
        }

        val grossDelta = summary.steps.subtractOrNull(checkpoint.effectiveTotalSteps)
            ?: return DailyStepsEnqueueResult()
        if (grossDelta < 0L) return DailyStepsEnqueueResult()
        val creditSteps = activeCredits.sumOfOrNull { it.steps } ?: return DailyStepsEnqueueResult()
        val residualDelta = grossDelta.subtractOrNull(creditSteps) ?: return DailyStepsEnqueueResult()
        if (residualDelta < 0L) return DailyStepsEnqueueResult()
        if (grossDelta == 0L && activeCredits.isEmpty()) {
            persistDailyStepsCheckpoint(checkpoint)
            return DailyStepsEnqueueResult(operation = latest?.first?.toDomain())
        }

        val maximumCreditEnd = activeCredits.maxOfOrNull { it.window.end } ?: dayStart
        val safeWindowStart = maxOf(
            Instant.ofEpochMilli(checkpoint.lastWindowEndEpochMillis),
            maximumCreditEnd,
            dayStart,
        )
        if (residualDelta == 0L) {
            val at = Instant.ofEpochMilli(updatedAtEpochMillis)
            if (checkpoint.nextOperationOrdinal == Long.MAX_VALUE) return DailyStepsEnqueueResult()
            consumeDailyWorkoutCredits(activeCredits, checkpoint.nextOperationOrdinal, at)
            persistDailyStepsCheckpoint(
                checkpoint.copy(
                    effectiveTotalSteps = summary.steps,
                    lastWindowEndEpochMillis = maxOf(
                        checkpoint.lastWindowEndEpochMillis,
                        maximumCreditEnd.toEpochMilli(),
                    ),
                    updatedAtEpochMillis = maxOf(checkpoint.updatedAtEpochMillis, updatedAtEpochMillis),
                ),
            )
            return DailyStepsEnqueueResult(operation = latest?.first?.toDomain())
        }
        if (latest == null && !allowNewOperation) return DailyStepsEnqueueResult()
        if (!windowEnd.isAfter(safeWindowStart)) {
            return DailyStepsEnqueueResult(operation = latest?.first?.toDomain())
        }
        if (checkpoint.nextOperationOrdinal == Long.MAX_VALUE) return DailyStepsEnqueueResult()
        val sequence = checkpoint.nextOperationOrdinal
        val nextOperationOrdinal = sequence + 1L
        val startsChainAfterCheckpointWithoutOperation = orderedPayloadOperations.isEmpty() &&
            checkpoint.effectiveTotalSteps > 0L
        val kind = if (residualDelta == grossDelta && !startsChainAfterCheckpointWithoutOperation) {
            DailyStepsSyncPayloadKind.INCREMENTAL
        } else {
            DailyStepsSyncPayloadKind.RESIDUAL_INCREMENTAL
        }
        val payload = DailyStepsSyncPayload(
            kind = kind,
            sequence = sequence,
            previousTotalSteps = checkpoint.effectiveTotalSteps,
            currentTotalSteps = summary.steps,
            stepDelta = residualDelta,
            startTime = safeWindowStart,
            endTime = windowEnd,
            timeZoneId = zoneId.id,
        )
        val key = HealthSyncOperationKey(
            aggregate = HealthSyncAggregate.DailySummary(
                DailySummaryId(parseUuid(summary.id) ?: return DailyStepsEnqueueResult()),
            ),
            recordType = HealthSyncRecordType.STEPS,
        )
        val operation = newPendingOperationEntity(
            key = key,
            metadata = ResolvedSyncMetadata(
                clientRecordId = "${stableClientRecordId(key)}:increment:$sequence",
                clientRecordVersion = INCREMENTAL_STEPS_CLIENT_RECORD_VERSION,
                timeZoneId = payload.timeZoneId,
                createdAtEpochMillis = updatedAtEpochMillis,
                updatedAtEpochMillis = updatedAtEpochMillis,
            ),
        ).copy(
            operationSequence = payload.sequence,
            dailyStepsPayloadKind = payload.kind.name,
            previousTotalSteps = payload.previousTotalSteps,
            currentTotalSteps = payload.currentTotalSteps,
            stepDelta = payload.stepDelta,
            stepStartTimeEpochMillis = payload.startTime.toEpochMilli(),
            stepEndTimeEpochMillis = payload.endTime.toEpochMilli(),
        )
        syncDao.insert(operation)
        val at = Instant.ofEpochMilli(updatedAtEpochMillis)
        reserveDailyWorkoutCredits(
            operation = operation,
            payload = payload,
            credits = activeCredits,
            at = at,
        )
        persistDailyStepsCheckpoint(
            checkpoint.copy(
                effectiveTotalSteps = summary.steps,
                nextOperationOrdinal = nextOperationOrdinal,
                lastWindowEndEpochMillis = maxOf(
                    checkpoint.lastWindowEndEpochMillis,
                    payload.endTime.toEpochMilli(),
                ),
                updatedAtEpochMillis = maxOf(checkpoint.updatedAtEpochMillis, updatedAtEpochMillis),
            ),
        )
        return DailyStepsEnqueueResult(
            operation = operation.toDomain(),
            created = true,
        )
    }

    private suspend fun resolveDailyStepsCheckpoint(
        summary: DailySummaryEntity,
        zoneId: ZoneId,
        dayStart: Instant,
        payloadOperations: List<Pair<HealthSyncOperationEntity, DailyStepsSyncPayload>>,
    ): DailyStepsCheckpointEntity? {
        val summaryDeviceId = summary.deviceId ?: return null
        val localDate = runCatching { LocalDate.parse(summary.localDateIso) }.getOrNull() ?: return null
        val dayEnd = runCatching { localDate.plusDays(1L).atStartOfDay(zoneId).toInstant() }.getOrNull()
            ?: return null
        val latest = payloadOperations.maxByOrNull { (_, payload) -> payload.sequence }
        val chainEnd = latest?.second?.endTime ?: dayStart
        val checkpointDao = database.dailyStepsCheckpointDao()
        val existing = checkpointDao.findByNaturalKey(
            deviceId = summaryDeviceId,
            origin = summary.origin,
            localDateIso = summary.localDateIso,
            timeZoneId = zoneId.id,
        )
        if (existing != null && (
                existing.effectiveTotalSteps < 0L ||
                    existing.nextOperationOrdinal < 1L ||
                    existing.lastWindowEndEpochMillis < dayStart.toEpochMilli() ||
                    existing.lastWindowEndEpochMillis > dayEnd.toEpochMilli()
            )
        ) return null
        if (!payloadOperations.isValidDailyStepsChain(allowPositiveFirstPreviousTotal = existing != null)) {
            return null
        }
        if (payloadOperations.any { (entity, payload) ->
                entity.action != HealthSyncOperationAction.INSERT.name ||
                    entity.projectionChildKey != HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY ||
                    entity.timeZoneId != zoneId.id ||
                    payload.timeZoneId != zoneId.id ||
                    payload.startTime.atZone(zoneId).toLocalDate() != localDate ||
                    payload.endTime.minusNanos(1L).atZone(zoneId).toLocalDate() != localDate ||
                    payload.endTime.isAfter(dayEnd)
            }
        ) return null
        val chainTotal = latest?.second?.currentTotalSteps ?: 0L
        val chainNextOrdinal = latest?.second?.sequence?.let {
            if (it == Long.MAX_VALUE) return null
            it + 1L
        } ?: 1L
        if (existing != null && latest != null && existing.nextOperationOrdinal != chainNextOrdinal) {
            return null
        }
        if (existing != null && existing.effectiveTotalSteps < chainTotal) return null
        if (existing == null && latest != null && latest.second.previousTotalSteps > 0L) return null
        val now = summary.updatedAtEpochMillis
        return (existing ?: DailyStepsCheckpointEntity(
            id = ids.next().toString(),
            deviceId = summaryDeviceId,
            origin = summary.origin,
            localDateIso = summary.localDateIso,
            timeZoneId = zoneId.id,
            effectiveTotalSteps = chainTotal,
            nextOperationOrdinal = chainNextOrdinal,
            lastWindowEndEpochMillis = chainEnd.toEpochMilli(),
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )).let { checkpoint ->
            checkpoint.copy(
                effectiveTotalSteps = maxOf(checkpoint.effectiveTotalSteps, chainTotal),
                nextOperationOrdinal = maxOf(checkpoint.nextOperationOrdinal, chainNextOrdinal),
                lastWindowEndEpochMillis = maxOf(checkpoint.lastWindowEndEpochMillis, chainEnd.toEpochMilli()),
            )
        }
    }

    private suspend fun persistDailyStepsCheckpoint(checkpoint: DailyStepsCheckpointEntity) {
        val dao = database.dailyStepsCheckpointDao()
        val existing = dao.findByNaturalKey(
            deviceId = checkpoint.deviceId,
            origin = checkpoint.origin,
            localDateIso = checkpoint.localDateIso,
            timeZoneId = checkpoint.timeZoneId,
        )
        if (existing == null) {
            dao.insert(checkpoint)
        } else if (dao.update(checkpoint.copy(id = existing.id, createdAtEpochMillis = existing.createdAtEpochMillis)) != 1) {
            throw WorkoutStepsCreditCommitAborted()
        }
    }

    private suspend fun findEligibleDailyWorkoutCredits(
        summary: DailySummaryEntity,
        zoneId: ZoneId,
    ): DailyWorkoutCreditEvaluation? {
        val summaryDeviceId = summary.deviceId ?: return null
        val localDate = runCatching { LocalDate.parse(summary.localDateIso) }.getOrNull() ?: return null
        val creditEntities = database.workoutStepsCreditDao().findByLocalDateAndState(
            localDateIso = localDate.toString(),
            state = WorkoutStepsCreditState.ACTIVE.name,
        )
        val eligible = buildList {
            for (entity in creditEntities) {
                val credit = entity.toDomain() ?: return null
                val activity = database.workoutActivityDao().findById(credit.activityId.value.toString())
                    ?: return null
                val activityDeviceId = activity.deviceId ?: return null
                if (activityDeviceId != summaryDeviceId) continue

                // Accepted ledger identity is frozen. Mutable activity snapshots
                // are used only to establish same-device ownership, never to
                // recalculate or silently discard accepted steps.
                if (activity.origin != WorkoutActivityOrigin.WATCH_D3_D4.name) return null
                val operationEntity = database.healthSyncOperationDao()
                    .findById(credit.projectionIdentity.operationId.value.toString())
                    ?: return null
                val operation = operationEntity.toDomain() ?: return null
                val identity = WorkoutStepsProjectionIdentity.fromOperation(operation).valueOrNull()
                    ?: return null
                val context = acceptedWorkoutStepsContext(operation) ?: return null
                if (
                    operation.state != SyncOperationState.SYNCED ||
                        operation.action != HealthSyncOperationAction.INSERT ||
                        operation.reconciliationState != HealthProjectionReconciliationState.SENT ||
                        operation.reconciliationCheckpoint != HealthProjectionReconciliationCheckpoint.AFTER_REMOTE_EFFECT ||
                        identity != credit.projectionIdentity ||
                        context.steps != credit.steps ||
                        context.window != credit.window ||
                        operation.timeZoneId.validZoneIdOrNull()?.let(ZoneId::of) != credit.timeZoneId
                ) return null

                val reservation = database.dailyStepsCreditReservationDao()
                    .findByCreditId(credit.id.value.toString())
                if (reservation != null) {
                    val dailyOperation = database.healthSyncOperationDao().findById(reservation.dailyOperationId)
                        ?: return null
                    val dailyPayload = dailyOperation.toDomain()?.dailyStepsPayload ?: return null
                    if (
                        !dailyOperation.isDailyStepsOperationEntity() ||
                        dailyOperation.aggregateId != summary.id ||
                        dailyOperation.projectionChildKey != HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY ||
                        dailyOperation.action != HealthSyncOperationAction.INSERT.name ||
                        dailyPayload.kind != DailyStepsSyncPayloadKind.RESIDUAL_INCREMENTAL ||
                        reservation.dailyOperationOrdinal != dailyPayload.sequence ||
                        reservation.reservedSteps != credit.steps ||
                        dailyOperation.state == SyncOperationState.SYNCED.name ||
                        dailyOperation.state !in setOf(
                            SyncOperationState.PENDING.name,
                            SyncOperationState.FAILED.name,
                            SyncOperationState.SYNCING.name,
                        )
                    ) return null
                }

                if (credit.localDate != localDate || credit.timeZoneId != zoneId) continue
                val endInclusive = credit.window.end.minusNanos(1L)
                if (
                    credit.window.start.atZone(zoneId).toLocalDate() != localDate ||
                        endInclusive.atZone(zoneId).toLocalDate() != localDate
                ) return null
                add(credit)
            }
        }.sortedBy { it.window.start }

        var latestEnd: Instant? = null
        for (credit in eligible) {
            if (latestEnd != null && credit.window.start.isBefore(latestEnd)) return null
            latestEnd = maxOf(latestEnd ?: credit.window.end, credit.window.end)
        }
        return DailyWorkoutCreditEvaluation(eligible)
    }

    private fun acceptedWorkoutStepsContext(operation: HealthSyncOperation): WorkoutStepsProjectionContext? {
        if (
            operation.key.projectionChildKey != HealthProjectionChildKey.WorkoutSteps ||
            operation.key.recordType != HealthSyncRecordType.STEPS ||
            operation.action != HealthSyncOperationAction.INSERT
        ) return null
        val payload = operation.frozenPayload
            ?.takeIf { it.schemaVersion == HEALTH_SYNC_FROZEN_PROJECTION_PAYLOAD_WORKOUT_STEPS_SCHEMA_VERSION }
            ?.let {
                runCatching {
                    frozenProjectionJson.decodeFromString<HealthSyncFrozenProjectionPayload>(it.serializedPayload)
                }.getOrNull()
            }
            ?: return null
        if (
            payload.projectionChildKey != HealthProjectionChildKey.WorkoutSteps.stableValue ||
            payload.recordType != HealthSyncRecordType.STEPS.name ||
            payload.steps == null || payload.steps <= 0L
        ) return null
        val start = payload.start.toInstantOrNull() ?: return null
        val end = payload.end.toInstantOrNull() ?: return null
        return WorkoutIntervalWindow.create(start, end).valueOrNull()?.let { window ->
            WorkoutStepsProjectionContext(payload.steps, window)
        }
    }

    private fun hasAcceptedDailyCoverageOverlap(
        operations: List<Pair<HealthSyncOperationEntity, DailyStepsSyncPayload>>,
        credits: List<WorkoutStepsCredit>,
    ): Boolean = credits.any { credit ->
        operations.any { (entity, payload) ->
            entity.state == SyncOperationState.SYNCED.name &&
                entity.action == HealthSyncOperationAction.INSERT.name &&
                entity.reconciliationState == HealthProjectionReconciliationState.SENT.name &&
                entity.reconciliationCheckpoint == HealthProjectionReconciliationCheckpoint.AFTER_REMOTE_EFFECT.name &&
                credit.window.start.isBefore(payload.endTime) &&
                payload.startTime.isBefore(credit.window.end)
        }
    }

    private suspend fun hasUnacceptedWorkoutStepsForDaily(
        summary: DailySummaryEntity,
        zoneId: ZoneId,
        dayStart: Instant,
    ): Boolean {
        val deviceId = summary.deviceId ?: return true
        val localDate = runCatching { LocalDate.parse(summary.localDateIso) }.getOrNull() ?: return true
        val dayEnd = runCatching { localDate.plusDays(1L).atStartOfDay(zoneId).toInstant() }.getOrNull()
            ?: return true
        val operations = database.healthSyncOperationDao().findUnacceptedWorkoutStepsForDevice(deviceId)
        for (entity in operations) {
            val operation = entity.toDomain() ?: return true
            val activity = database.workoutActivityDao().findById(entity.aggregateId) ?: return true
            if (activity.origin != WorkoutActivityOrigin.WATCH_D3_D4.name) return true
            val operationZone = operation.timeZoneId.validZoneIdOrNull()
                ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
                ?: return true
            val context = acceptedWorkoutStepsContext(operation) ?: return true
            val endInclusive = context.window.end.minusNanos(1L)
            val operationStartDate = context.window.start.atZone(operationZone).toLocalDate()
            val operationEndDate = endInclusive.atZone(operationZone).toLocalDate()
            if (operationStartDate != operationEndDate) return true
            if (operationZone != zoneId || operationStartDate != localDate) continue
            if (context.window.start.isBefore(dayStart) || context.window.end.isAfter(dayEnd)) return true
            return true
        }
        return false
    }

    private suspend fun isValidDailyStepsReservationSet(
        operation: HealthSyncOperationEntity,
        reservations: List<DailyStepsCreditReservationEntity>,
        activeCredits: List<WorkoutStepsCredit>,
    ): Boolean {
        val payload = operation.toDomain()?.dailyStepsPayload ?: return false
        if (
            !operation.isDailyStepsOperationEntity() ||
            operation.action != HealthSyncOperationAction.INSERT.name ||
            payload.kind != DailyStepsSyncPayloadKind.RESIDUAL_INCREMENTAL ||
            payload.sequence != operation.operationSequence ||
            activeCredits.isEmpty() ||
            reservations.map { it.creditId }.toSet() != activeCredits.map { it.id.value.toString() }.toSet()
        ) return false
        for (reservation in reservations) {
            if (
                reservation.dailyOperationId != operation.id ||
                reservation.dailyOperationOrdinal != payload.sequence ||
                reservation.reservedSteps <= 0L
            ) return false
            val credit = database.workoutStepsCreditDao().findById(reservation.creditId)?.toDomain() ?: return false
            if (
                credit.state != WorkoutStepsCreditState.ACTIVE ||
                credit.steps != reservation.reservedSteps ||
                activeCredits.none { it.id == credit.id }
            ) return false
        }
        return true
    }

    private suspend fun reserveDailyWorkoutCredits(
        operation: HealthSyncOperationEntity,
        payload: DailyStepsSyncPayload,
        credits: List<WorkoutStepsCredit>,
        at: Instant,
    ) {
        if (credits.isEmpty()) return
        val reservationDao = database.dailyStepsCreditReservationDao()
        for (credit in credits) {
            if (credit.state != WorkoutStepsCreditState.ACTIVE) {
                throw WorkoutStepsCreditCommitAborted()
            }
            val creditId = credit.id.value.toString()
            val existing = reservationDao.findByCreditId(creditId)
            if (existing != null) {
                if (
                    existing.dailyOperationId != operation.id ||
                    existing.dailyOperationOrdinal != payload.sequence ||
                    existing.reservedSteps != credit.steps
                ) throw WorkoutStepsCreditCommitAborted()
                continue
            }
            reservationDao.insert(
                DailyStepsCreditReservationEntity(
                    id = ids.next().toString(),
                    creditId = creditId,
                    dailyOperationId = operation.id,
                    dailyOperationOrdinal = payload.sequence,
                    reservedSteps = credit.steps,
                    createdAtEpochMillis = at.toEpochMilli(),
                    updatedAtEpochMillis = at.toEpochMilli(),
                ),
            )
        }
    }

    /** Consome reservas somente no commit local posterior ao aceite remoto. */
    private suspend fun consumeDailyStepsCreditReservations(
        operation: HealthSyncOperationEntity,
        at: Instant,
    ): Boolean {
        val reservations = database.dailyStepsCreditReservationDao()
            .findByDailyOperationId(operation.id)
        if (reservations.isEmpty()) return true
        val payload = operation.toDomain()?.dailyStepsPayload ?: return false
        if (
            !operation.isDailyStepsOperationEntity() ||
            operation.state != SyncOperationState.SYNCED.name ||
            operation.action != HealthSyncOperationAction.INSERT.name ||
            payload.kind != DailyStepsSyncPayloadKind.RESIDUAL_INCREMENTAL ||
            operation.reconciliationState != HealthProjectionReconciliationState.SENT.name ||
            operation.reconciliationCheckpoint != HealthProjectionReconciliationCheckpoint.AFTER_REMOTE_EFFECT.name ||
            payload.sequence != operation.operationSequence
        ) return false
        val creditDao = database.workoutStepsCreditDao()
        for (reservation in reservations) {
            if (
                reservation.dailyOperationId != operation.id ||
                reservation.dailyOperationOrdinal != payload.sequence ||
                reservation.reservedSteps <= 0L
            ) return false
            val credit = creditDao.findById(reservation.creditId)?.toDomain() ?: return false
            if (credit.steps != reservation.reservedSteps) return false
            val consumed = when (credit.state) {
                WorkoutStepsCreditState.ACTIVE -> credit.consume(payload.sequence, at).valueOrNull()
                WorkoutStepsCreditState.CONSUMED -> credit.takeIf {
                    it.consumedDailyOperationOrdinal == payload.sequence
                }

                else -> null
            } ?: return false
            if (consumed != credit && creditDao.update(consumed.toEntity()) != 1) return false
        }
        return true
    }

    private suspend fun consumeDailyWorkoutCredits(
        credits: List<WorkoutStepsCredit>,
        dailyOperationOrdinal: Long,
        at: Instant,
    ) {
        for (credit in credits) {
            val consumed = credit.consume(dailyOperationOrdinal, at).valueOrNull()
                ?: throw WorkoutStepsCreditCommitAborted()
            if (database.workoutStepsCreditDao().update(consumed.toEntity()) != 1) {
                throw WorkoutStepsCreditCommitAborted()
            }
        }
    }

    private fun List<WorkoutStepsCredit>.sumOfOrNull(selector: (WorkoutStepsCredit) -> Long): Long? =
        fold(0L) { total, credit ->
            try {
                Math.addExact(total, selector(credit))
            } catch (_: ArithmeticException) {
                return null
            }
        }

    private suspend fun findDailyStepsEntities(summaryId: String): List<HealthSyncOperationEntity> =
        database.healthSyncOperationDao()
            .findByAggregate(summaryId, HealthSyncAggregateType.DAILY_SUMMARY.name)
            .filter { it.recordType == HealthSyncRecordType.STEPS.name }

    private fun resolveDailyStepsWindowEnd(
        summary: DailySummaryEntity,
        collectedAtEpochMillis: Long,
        zoneId: ZoneId,
    ): Instant? = runCatching {
        val localDate = LocalDate.parse(summary.localDateIso)
        Instant.ofEpochMilli(collectedAtEpochMillis).takeIf {
            it.atZone(zoneId).toLocalDate() == localDate
        }
    }.getOrNull()

    private suspend fun ensureOperationInternal(
        key: HealthSyncOperationKey,
    ): RepositoryResult<HealthSyncOperation> {
        if (isBlockedWatchWorkoutAggregate(key.aggregate)) {
            // Operações legadas de WATCH_D3_D4 não podem chegar ao mapper que
            // ainda usa distância original, calibrada ou estimada.
            return RepositoryResult.UnexpectedState
        }
        // Filho só pode nascer com payload congelado e origem por meio de
        // prepareProjectionChild; nunca criar linha parcial nesta rota legada.
        if (key.projectionChildKey != null) return RepositoryResult.UnexpectedState
        if (key.aggregate is HealthSyncAggregate.DailySummary && key.recordType == HealthSyncRecordType.STEPS) {
            return ensureDailyStepsOperationInternal(key)
        }
        val metadata = resolveSyncMetadata(key) ?: return RepositoryResult.NotFound
        val syncDao = database.healthSyncOperationDao()
        val existing = syncDao.findByNaturalKey(
            aggregateId = key.aggregate.id.toString(),
            aggregateType = key.aggregate.type.name,
            recordType = key.recordType.name,
            projectionChildKey = key.projectionChildKey?.stableValue
                ?: HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY,
        )
        val nextEntity = when (existing) {
            null -> newPendingOperationEntity(key, metadata)
            else -> mergeOperationEntity(existing, metadata)
        }
        when (existing) {
            null -> syncDao.insert(nextEntity)
            else -> if (nextEntity != existing) syncDao.update(nextEntity)
        }
        return nextEntity.toDomain()?.let { RepositoryResult.Success(it) } ?: RepositoryResult.UnexpectedState
    }

    private suspend fun ensureReconciledOperation(key: HealthSyncOperationKey): ReconciledOperation? {
        if (isBlockedWatchWorkoutAggregate(key.aggregate)) return null
        if (key.projectionChildKey != null) return null
        val metadata = resolveSyncMetadata(key) ?: return null
        val existing = database.healthSyncOperationDao().findByNaturalKey(
            aggregateId = key.aggregate.id.toString(),
            aggregateType = key.aggregate.type.name,
            recordType = key.recordType.name,
            projectionChildKey = key.projectionChildKey?.stableValue
                ?: HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY,
        )
        val repaired = existing?.needsRepair(metadata) == true
        return when (val ensured = ensureOperationInternal(key)) {
            is RepositoryResult.Success -> ReconciledOperation(ensured.value, repaired)
            else -> null
        }
    }

    private suspend fun reconcileBacklogInternal(): HealthSyncBacklogReconciliationResult {
        var ensuredCount = 0
        var repairedCount = 0
        var dailySummaryCount = 0
        var heartRateCount = 0
        var weightCount = 0
        var heightCount = 0

        database.dailySummaryDao().findValidWatchBacklog().forEach { summary ->
            val existingOperations = findDailyStepsEntities(summary.id)
            val timeZoneId = summary.collectedZoneId.validZoneIdOrNull()
                ?: existingOperations.firstNotNullOfOrNull { it.timeZoneId.validZoneIdOrNull() }
                ?: ZoneId.systemDefault().id
            val result = enqueueDailyStepsInternal(
                summary = summary,
                collectedAtEpochMillis = summary.collectedAtEpochMillis,
                timeZoneId = timeZoneId,
                updatedAtEpochMillis = summary.updatedAtEpochMillis,
            )
            if (result.operation != null) {
                ensuredCount += 1
                dailySummaryCount += 1
                if (result.repaired) repairedCount += 1
            }
        }

        database.heartRateDao().findValidHealthConnectBacklog().forEach { measurement ->
            val result = ensureReconciledOperation(
                HealthSyncOperationKey(
                    aggregate = HealthSyncAggregate.HeartRateMeasurement(HeartRateMeasurementId(parseUuid(measurement.id) ?: return@forEach)),
                    recordType = HealthSyncRecordType.HEART_RATE,
                ),
            )
            if (result != null) {
                ensuredCount += 1
                heartRateCount += 1
                if (result.repaired) repairedCount += 1
            }
        }

        val profile = database.userProfileDao().findCurrent()
        if (profile != null) {
            val profileId = parseUuid(profile.id)
            database.userProfileDao().findWeights(profile.id).forEach { weight ->
                val result = ensureReconciledOperation(
                    HealthSyncOperationKey(
                        aggregate = HealthSyncAggregate.WeightMeasurement(WeightMeasurementId(parseUuid(weight.id) ?: return@forEach)),
                        recordType = HealthSyncRecordType.WEIGHT,
                    ),
                )
                if (result != null) {
                    ensuredCount += 1
                    weightCount += 1
                    if (result.repaired) repairedCount += 1
                }
            }
            if (profileId != null) {
                val result = profile.toDomain()?.let { ensureHeightHealthSync(it) }
                if (result != null) {
                    ensuredCount += 1
                    heightCount += 1
                    if (result.repaired) repairedCount += 1
                }
            }
        }

        return HealthSyncBacklogReconciliationResult(
            ensuredOperationCount = ensuredCount,
            repairedOperationCount = repairedCount,
            dailySummaryCount = dailySummaryCount,
            heartRateCount = heartRateCount,
            weightCount = weightCount,
            heightCount = heightCount,
        )
    }

    private suspend fun transitionSync(
        id: HealthSyncOperationId,
        transform: (HealthSyncOperationEntity) -> HealthSyncOperationEntity?,
    ): RepositoryResult<Unit> = write {
        val syncDao = database.healthSyncOperationDao()
        val current = syncDao.findById(id.value.toString()) ?: return@write RepositoryResult.NotFound
        if (isBlockedWatchWorkoutOperation(current)) return@write RepositoryResult.UnexpectedState
        if (!aggregateExists(current)) return@write RepositoryResult.NotFound
        val next = transform(current) ?: return@write RepositoryResult.UnexpectedState
        if (syncDao.update(next) == 1) RepositoryResult.Success(Unit) else RepositoryResult.UnexpectedState
    }

    private suspend fun aggregateExists(operation: HealthSyncOperationEntity): Boolean = when (operation.aggregateType) {
        "DAILY_SUMMARY" -> database.dailySummaryDao().findById(operation.aggregateId) != null
        "WATCH_DAILY_D1_SAMPLE" -> database.watchDailyD1SampleDao().findById(operation.aggregateId) != null
        "HEART_RATE_MEASUREMENT" -> database.heartRateDao().findById(operation.aggregateId) != null
        "WORKOUT_ACTIVITY" -> database.workoutActivityDao().findById(operation.aggregateId) != null
        "WEIGHT_MEASUREMENT" -> database.userProfileDao().findWeightById(operation.aggregateId) != null
        "USER_PROFILE_HEIGHT" -> database.userProfileDao().findById(operation.aggregateId) != null
        else -> false
    }

    private suspend fun resolveSyncMetadata(key: HealthSyncOperationKey): ResolvedSyncMetadata? {
        return when (val aggregate = key.aggregate) {
            is HealthSyncAggregate.DailySummary -> {
                val entity = database.dailySummaryDao().findById(aggregate.summaryId.value.toString()) ?: return null
                if (entity.origin != DataOrigin.WATCH_DAILY_SUMMARY.name || entity.validity != "VALID" || entity.deviceId == null) return null
                if (key.recordType != HealthSyncRecordType.STEPS) return null
                ResolvedSyncMetadata(
                    clientRecordId = stableClientRecordId(key),
                    clientRecordVersion = entity.updatedAtEpochMillis,
                    timeZoneId = entity.collectedZoneId,
                    createdAtEpochMillis = entity.collectedAtEpochMillis,
                    updatedAtEpochMillis = entity.updatedAtEpochMillis,
                )
            }

            is HealthSyncAggregate.WatchDailyD1Sample -> null

            is HealthSyncAggregate.HeartRateMeasurement -> {
                val entity = database.heartRateDao().findById(aggregate.measurementId.value.toString()) ?: return null
                resolveHeartRateSyncMetadata(entity)?.takeIf { key.recordType == HealthSyncRecordType.HEART_RATE }
                    ?.copy(clientRecordId = stableClientRecordId(key))
            }

            is HealthSyncAggregate.WorkoutActivity -> {
                val entity = database.workoutActivityDao().findById(aggregate.activityId.value.toString()) ?: return null
                val version = maxOf(
                    entity.recordedAtEpochMillis,
                    entity.reviewedAtEpochMillis ?: Long.MIN_VALUE,
                )
                ResolvedSyncMetadata(
                    clientRecordId = stableClientRecordId(key),
                    clientRecordVersion = version,
                    timeZoneId = resolveWorkoutTimeZoneId(key),
                    createdAtEpochMillis = entity.recordedAtEpochMillis,
                    updatedAtEpochMillis = version,
                )
            }

            is HealthSyncAggregate.WeightMeasurement -> {
                val entity = database.userProfileDao().findWeightById(aggregate.measurementId.value.toString()) ?: return null
                val kilograms = entity.toKilogramsOrNull() ?: return null
                if (kilograms <= 0.0) return null
                if (key.recordType != HealthSyncRecordType.WEIGHT) return null
                ResolvedSyncMetadata(
                    clientRecordId = stableClientRecordId(key),
                    clientRecordVersion = entity.measuredAtEpochMillis,
                    timeZoneId = null,
                    createdAtEpochMillis = entity.measuredAtEpochMillis,
                    updatedAtEpochMillis = entity.measuredAtEpochMillis,
                )
            }

            is HealthSyncAggregate.UserProfileHeight -> {
                val entity = database.userProfileDao().findById(aggregate.profileId.value.toString()) ?: return null
                if (entity.heightCentimeters?.takeIf { it.isFinite() && it > 0.0 } == null) return null
                if (key.recordType != HealthSyncRecordType.HEIGHT) return null
                ResolvedSyncMetadata(
                    clientRecordId = stableClientRecordId(key),
                    clientRecordVersion = entity.updatedAtEpochMillis,
                    timeZoneId = null,
                    createdAtEpochMillis = entity.updatedAtEpochMillis,
                    updatedAtEpochMillis = entity.updatedAtEpochMillis,
                )
            }
        }
    }

    private suspend fun resolveWorkoutTimeZoneId(key: HealthSyncOperationKey): String {
        val existingOperations = database.healthSyncOperationDao().findByAggregate(
            aggregateId = key.aggregate.id.toString(),
            aggregateType = key.aggregate.type.name,
        )
        val childStableValue = key.projectionChildKey?.stableValue
        return existingOperations
            .firstOrNull {
                it.recordType == key.recordType.name &&
                    it.projectionChildKey == childStableValue
            }
            ?.timeZoneId
            .validZoneIdOrNull()
            ?: if (childStableValue != null) {
                existingOperations
                    .firstOrNull {
                        it.recordType == key.recordType.name &&
                            it.projectionChildKey == HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY
                    }
                    ?.timeZoneId
                    .validZoneIdOrNull()
            } else {
                null
            }
            ?: existingOperations
                .firstOrNull { it.recordType == key.recordType.name }
                ?.timeZoneId
                .validZoneIdOrNull()
            ?: existingOperations.firstNotNullOfOrNull { operation -> operation.timeZoneId.validZoneIdOrNull() }
            ?: ZoneId.systemDefault().id
    }

    private fun resolveHeartRateSyncMetadata(
        entity: br.com.mwconsulting.championcompanion.core.database.HeartRateMeasurementEntity,
    ): ResolvedSyncMetadata? {
        if (entity.validity != "VALID" || entity.measuredAtEpochMillis == null) return null
        if (entity.source !in setOf(HeartRateSource.MANUAL_MEASUREMENT.name, HeartRateSource.WATCH_D1_HISTORY.name)) return null
        return ResolvedSyncMetadata(
            clientRecordId = null,
            clientRecordVersion = entity.measuredAtEpochMillis,
            timeZoneId = null,
            createdAtEpochMillis = entity.measuredAtEpochMillis,
            updatedAtEpochMillis = entity.measuredAtEpochMillis,
        )
    }

    private fun mergeOperationEntity(
        existing: HealthSyncOperationEntity,
        metadata: ResolvedSyncMetadata,
    ): HealthSyncOperationEntity {
        if (existing.state == SyncOperationState.SYNCING.name) return existing

        val repairedVersion = when {
            existing.clientRecordVersion == null -> metadata.clientRecordVersion
            metadata.clientRecordVersion == null -> existing.clientRecordVersion
            metadata.clientRecordVersion >= existing.clientRecordVersion -> metadata.clientRecordVersion
            else -> existing.clientRecordVersion
        }
        val nextUpdatedAt = listOfNotNull(existing.updatedAtEpochMillis, metadata.updatedAtEpochMillis).maxOrNull()
        val shouldResetForNewVersion =
            existing.clientRecordVersion != null &&
                metadata.clientRecordVersion != null &&
                metadata.clientRecordVersion > existing.clientRecordVersion
        val repairedClientRecordId = when {
            metadata.clientRecordId == null -> existing.clientRecordId
            existing.state == SyncOperationState.SYNCED.name -> existing.clientRecordId
            existing.state == SyncOperationState.PENDING.name ||
                existing.state == SyncOperationState.FAILED.name -> metadata.clientRecordId
            else -> existing.clientRecordId
        }
        return existing.copy(
            state = if (shouldResetForNewVersion) SyncOperationState.PENDING.name else existing.state,
            attemptCount = if (shouldResetForNewVersion) 0 else existing.attemptCount,
            lastAttemptAtEpochMillis = if (shouldResetForNewVersion) null else existing.lastAttemptAtEpochMillis,
            lastError = if (shouldResetForNewVersion) null else existing.lastError,
            clientRecordId = repairedClientRecordId,
            clientRecordVersion = repairedVersion,
            syncedAtEpochMillis = if (shouldResetForNewVersion) null else existing.syncedAtEpochMillis,
            timeZoneId = metadata.timeZoneId ?: existing.timeZoneId,
            createdAtEpochMillis = existing.createdAtEpochMillis ?: metadata.createdAtEpochMillis ?: metadata.updatedAtEpochMillis,
            updatedAtEpochMillis = if (shouldResetForNewVersion) metadata.updatedAtEpochMillis else nextUpdatedAt,
        )
    }

    private fun HealthSyncOperationEntity.needsRepair(metadata: ResolvedSyncMetadata): Boolean =
        (metadata.clientRecordId != null &&
            state in setOf(SyncOperationState.PENDING.name, SyncOperationState.FAILED.name) &&
            clientRecordId != metadata.clientRecordId) ||
            (clientRecordVersion == null && metadata.clientRecordVersion != null) ||
            (timeZoneId == null && metadata.timeZoneId != null) ||
            (createdAtEpochMillis == null && metadata.createdAtEpochMillis != null) ||
            (updatedAtEpochMillis == null && metadata.updatedAtEpochMillis != null)

    private fun newPendingOperationEntity(
        key: HealthSyncOperationKey,
        metadata: ResolvedSyncMetadata,
    ): HealthSyncOperationEntity = HealthSyncOperationEntity(
        id = ids.next().toString(),
        aggregateId = key.aggregate.id.toString(),
        aggregateType = key.aggregate.type.name,
        recordType = key.recordType.name,
        state = SyncOperationState.PENDING.name,
        attemptCount = 0,
        lastAttemptAtEpochMillis = null,
        lastError = null,
        clientRecordId = metadata.clientRecordId,
        clientRecordVersion = metadata.clientRecordVersion,
        syncedAtEpochMillis = null,
        timeZoneId = metadata.timeZoneId,
        createdAtEpochMillis = metadata.createdAtEpochMillis ?: metadata.updatedAtEpochMillis,
        updatedAtEpochMillis = metadata.updatedAtEpochMillis,
        projectionChildKey = key.projectionChildKey?.stableValue
            ?: HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY,
        action = HealthSyncOperationAction.INSERT.name,
        frozenPayload = null,
        frozenPayloadVersion = null,
        reconciliationState = HealthProjectionReconciliationState.EXPECTED.name,
        reconciliationCheckpoint = HealthProjectionReconciliationCheckpoint.NOT_STARTED.name,
        projectionOriginId = null,
        projectionOriginVersion = null,
    )

    private fun stableClientRecordId(key: HealthSyncOperationKey): String {
        val legacyId =
            "${BuildConfig.APPLICATION_ID}:${key.aggregate.type.name}:${key.aggregate.id}:${key.recordType.name}"
        return key.projectionChildKey?.let { "$legacyId:${it.stableValue}" } ?: legacyId
    }

    private fun parseUuid(value: String): UUID? = br.com.mwconsulting.championcompanion.core.model.UuidV7.parse(value).valueOrNull()

    private suspend fun <T> write(block: suspend () -> RepositoryResult<T>): RepositoryResult<T> = withContext(ioDispatcher) {
        database.withWriteTransaction { block() }
    }

    private suspend fun <T> writeTransaction(block: suspend () -> T): T = withContext(ioDispatcher) {
        database.withWriteTransaction { block() }
    }

    private suspend fun <T> read(block: suspend () -> RepositoryResult<T>): RepositoryResult<T> = withContext(ioDispatcher) {
        database.withReadTransaction { block() }
    }

    private fun manualMeasurementDeduplicationKey(sessionId: HeartRateMeasurementSessionId): String =
        br.com.mwconsulting.championcompanion.core.model.HeartRateDeduplicationKey.MeasurementSession(sessionId)
            .encode()

    private fun generateIds(count: Int): Iterator<java.util.UUID> = List(count) { ids.next() }.iterator()
}

private data class MappedHealthSyncOperation(
    val aggregate: HealthSyncAggregate?,
    val operation: HealthSyncOperation?,
    val hasSupportedPair: Boolean,
) {
    val isValid: Boolean
        get() = operation != null && hasSupportedPair
}

private data class NonWorkoutHealthSyncCandidates(
    val dailySummaries: Map<UUID, DailySummary>,
    val heartRateMeasurements: Map<UUID, HeartRateMeasurement>,
    val weightMeasurements: Map<UUID, WeightMeasurement>,
    val userProfiles: Map<UUID, UserProfile>,
)

private data class HealthSyncWorkoutCandidateFacts(
    val records: List<HealthSyncWorkoutCandidateRecord>,
    val intervalsByActivityId: Map<String, List<WorkoutIntervalEntity>>,
    val samplesByActivityId: Map<String, List<WorkoutHeartRateSampleEntity>>,
    val headersByActivityId: Map<String, WatchWorkoutD4V4HeaderEntity>,
)

private data class HealthSyncSemanticCandidateIndex(
    val dailySummaries: Map<UUID, DailySummary>,
    val heartRateMeasurements: Map<UUID, HeartRateMeasurement>,
    val workoutActivities: Map<UUID, WorkoutActivity>,
    val weightMeasurements: Map<UUID, WeightMeasurement>,
    val userProfiles: Map<UUID, UserProfile>,
)

private data class HealthSyncWorkoutSessionWindow(
    val start: Instant,
    val end: Instant,
)

private fun List<WorkoutIntervalEntity>.markPersistedSourceOrder(
    activity: WorkoutActivity,
): List<WorkoutIntervalEntity> {
    if (activity.activityOrigin != WorkoutActivityOrigin.WATCH_D3_D4 || isEmpty()) return this
    val indices = activity.intervals.map { it.index }
    val marker = if (indices.zipWithNext().all { (previous, next) -> next > previous }) {
        TEMPORAL_ORDER_VALIDATED_MARKER
    } else {
        TemporalSeriesValidationReason.OUT_OF_ORDER.name
    }
    // Coluna existente desde v9. O marcador sobrevive à ordenação do DAO e só
    // controla elegibilidade; fatos raw/measured não são alterados.
    return map { it.copy(temporalEligibilityReason = marker) }
}

private fun List<WorkoutIntervalEntity>.hasPersistedUnvalidatedSourceOrder(): Boolean =
    any { it.temporalEligibilityReason != TEMPORAL_ORDER_VALIDATED_MARKER }

private fun HealthSyncOperationEntity.isBlockedWatchWorkoutOperation(
    blockedWatchActivityIds: Set<String>,
): Boolean = aggregateType == HealthSyncAggregateType.WORKOUT_ACTIVITY.name &&
    projectionChildKey == HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY &&
    action != HealthSyncOperationAction.DELETE.name &&
    aggregateId in blockedWatchActivityIds

private fun HealthSyncOperation.isBlockedWatchWorkoutOperation(
    candidates: HealthSyncSemanticCandidateIndex,
): Boolean {
    val aggregate = key.aggregate as? HealthSyncAggregate.WorkoutActivity ?: return false
    return key.projectionChildKey == null &&
        action != HealthSyncOperationAction.DELETE &&
        candidates.workoutActivities[aggregate.activityId.value]?.activityOrigin ==
            WorkoutActivityOrigin.WATCH_D3_D4
}

private fun HealthSyncOperationEntity.hasSupportedHealthSyncPair(): Boolean = when (aggregateType) {
    HealthSyncAggregateType.DAILY_SUMMARY.name -> recordType == HealthSyncRecordType.STEPS.name
    HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name -> false
    HealthSyncAggregateType.HEART_RATE_MEASUREMENT.name -> recordType == HealthSyncRecordType.HEART_RATE.name
    HealthSyncAggregateType.WORKOUT_ACTIVITY.name -> recordType in setOf(
        HealthSyncRecordType.EXERCISE_SESSION.name,
        HealthSyncRecordType.DISTANCE.name,
        HealthSyncRecordType.TOTAL_CALORIES_BURNED.name,
        HealthSyncRecordType.HEART_RATE.name,
    ) || recordType == HealthSyncRecordType.SPEED.name &&
        projectionChildKey == HealthProjectionChildKey.SpeedSeries.stableValue ||
        recordType == HealthSyncRecordType.STEPS.name &&
            projectionChildKey == HealthProjectionChildKey.WorkoutSteps.stableValue ||
        recordType == HealthSyncRecordType.STEPS_CADENCE.name &&
            projectionChildKey == HealthProjectionChildKey.StepsCadenceSeries.stableValue
    HealthSyncAggregateType.WEIGHT_MEASUREMENT.name -> recordType == HealthSyncRecordType.WEIGHT.name
    HealthSyncAggregateType.USER_PROFILE_HEIGHT.name -> recordType == HealthSyncRecordType.HEIGHT.name
    else -> false
}

private fun isSemanticallyResolved(
    operation: HealthSyncOperation,
    candidates: HealthSyncSemanticCandidateIndex,
): Boolean {
    if (operation.action == HealthSyncOperationAction.DELETE) return true
    return when (val aggregate = operation.key.aggregate) {
    is HealthSyncAggregate.DailySummary ->
        operation.key.recordType == HealthSyncRecordType.STEPS &&
            candidates.dailySummaries[aggregate.summaryId.value]?.let { summary ->
                isDailySummaryEligibleForSteps(operation, summary)
            } == true

    is HealthSyncAggregate.WatchDailyD1Sample -> false

    is HealthSyncAggregate.HeartRateMeasurement ->
        operation.key.recordType == HealthSyncRecordType.HEART_RATE &&
            candidates.heartRateMeasurements[aggregate.measurementId.value]?.let { measurement ->
                isHeartRateMeasurementEligibleForHeartRate(measurement)
            } == true

    is HealthSyncAggregate.WorkoutActivity ->
        candidates.workoutActivities[aggregate.activityId.value]?.let { activity ->
            when (operation.key.recordType) {
                HealthSyncRecordType.EXERCISE_SESSION -> isWorkoutActivityEligibleForExerciseSession(activity, operation)
                HealthSyncRecordType.DISTANCE -> isWorkoutActivityEligibleForDistance(activity, operation)
                HealthSyncRecordType.TOTAL_CALORIES_BURNED -> isWorkoutActivityEligibleForCalories(activity, operation)
                HealthSyncRecordType.HEART_RATE -> isWorkoutActivityEligibleForHeartRate(activity, operation)
                HealthSyncRecordType.SPEED -> isWorkoutActivityEligibleForSpeed(activity, operation)
                HealthSyncRecordType.STEPS -> isWorkoutActivityEligibleForSteps(activity, operation)
                HealthSyncRecordType.STEPS_CADENCE -> isWorkoutActivityEligibleForCadence(activity, operation)
                else -> false
            }
        } == true

    is HealthSyncAggregate.WeightMeasurement ->
        operation.key.recordType == HealthSyncRecordType.WEIGHT &&
            candidates.weightMeasurements[aggregate.measurementId.value]?.let { measurement ->
                isWeightMeasurementEligibleForWeight(measurement)
            } == true

    is HealthSyncAggregate.UserProfileHeight ->
        operation.key.recordType == HealthSyncRecordType.HEIGHT &&
            candidates.userProfiles[aggregate.profileId.value]?.let { profile ->
                isUserProfileEligibleForHeight(profile)
            } == true
    }
}

private fun isDailySummaryEligibleForSteps(
    operation: HealthSyncOperation,
    summary: DailySummary,
): Boolean = summary.validity == DataValidity.VALID &&
    summary.key.origin == DataOrigin.WATCH_DAILY_SUMMARY &&
    summary.key.deviceId != null &&
    (operation.dailyStepsPayload == null || hasValidStepsRecordWindow(operation))

private fun hasValidStepsRecordWindow(
    operation: HealthSyncOperation,
): Boolean {
    val payload = operation.dailyStepsPayload ?: return false
    return payload.stepDelta > 0L &&
        payload.currentTotalSteps > payload.previousTotalSteps &&
        payload.endTime.isAfter(payload.startTime) &&
        payload.timeZoneId.validZoneIdOrNull() != null
}

private fun isHeartRateMeasurementEligibleForHeartRate(
    measurement: HeartRateMeasurement,
): Boolean = measurement.validity == DataValidity.VALID &&
    measurement.measuredAt != null &&
    measurement.source in setOf(
        HeartRateSource.MANUAL_MEASUREMENT,
        HeartRateSource.WATCH_D1_HISTORY,
    ) &&
    measurement.bpm in 30..220

private fun isWorkoutActivityEligibleForExerciseSession(
    activity: WorkoutActivity,
    operation: HealthSyncOperation,
): Boolean = isWorkoutActivityBaseEligible(activity, operation)

private fun isWorkoutActivityEligibleForDistance(
    activity: WorkoutActivity,
    operation: HealthSyncOperation,
): Boolean {
    if (!isWorkoutActivityBaseEligible(activity, operation)) return false
    if (activity.activityOrigin != WorkoutActivityOrigin.WATCH_D3_D4) {
        return activity.finalPositiveDistanceMeters() != null
    }
    if (activity.confirmedType == WorkoutType.CYCLING) {
        return operation.key.projectionChildKey == HealthProjectionChildKey.ManualDistance &&
            activity.metrics.distances.manualDistanceMeters.presentDoubleOrNull()
                ?.let { it.isFinite() && it > 0.0 } == true
    }
    val child = operation.key.projectionChildKey as? HealthProjectionChildKey.DistanceInterval
        ?: return false
    return activity.derivationSnapshot?.isEligibleForHealthProjection == true &&
        activity.canonicalSeries?.intervals?.any { interval ->
            interval.index == child.intervalIndex &&
                interval.canonicalIntervalDistanceMeters.isFinite() &&
                interval.canonicalIntervalDistanceMeters > 0.0
        } == true
}

private fun isWorkoutActivityEligibleForCalories(
    activity: WorkoutActivity,
    operation: HealthSyncOperation,
): Boolean = isWorkoutActivityBaseEligible(activity, operation) &&
        activity.metrics.caloriesKilocalories.presentDoubleOrNull()
            ?.takeIf { it.isFinite() && it > 0.0 } != null

private fun isWorkoutActivityEligibleForSpeed(
    activity: WorkoutActivity,
    operation: HealthSyncOperation,
): Boolean = isWorkoutActivityBaseEligible(activity, operation) &&
    activity.activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4 &&
    activity.confirmedType != WorkoutType.CYCLING &&
    activity.derivationSnapshot?.isEligibleForHealthProjection == true &&
    operation.key.projectionChildKey == HealthProjectionChildKey.SpeedSeries &&
    operation.frozenPayload != null

private fun isWorkoutActivityEligibleForSteps(
    activity: WorkoutActivity,
    operation: HealthSyncOperation,
): Boolean = isWorkoutActivityBaseEligible(activity, operation) &&
    activity.activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4 &&
    activity.confirmedType != WorkoutType.CYCLING &&
    activity.metrics.stepsAvailable &&
    activity.metrics.steps > 0L &&
    operation.key.projectionChildKey == HealthProjectionChildKey.WorkoutSteps &&
    operation.frozenPayload != null

private fun isWorkoutActivityEligibleForCadence(
    activity: WorkoutActivity,
    operation: HealthSyncOperation,
): Boolean = isWorkoutActivityBaseEligible(activity, operation) &&
    activity.activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4 &&
    activity.confirmedType != WorkoutType.CYCLING &&
    activity.derivationSnapshot?.isEligibleForHealthProjection == true &&
    activity.canonicalSeries?.intervals?.isNotEmpty() == true &&
    operation.key.projectionChildKey == HealthProjectionChildKey.StepsCadenceSeries &&
    operation.frozenPayload != null

private fun isWorkoutActivityEligibleForHeartRate(
    activity: WorkoutActivity,
    operation: HealthSyncOperation,
): Boolean {
    if (!isWorkoutActivityBaseEligible(activity, operation)) return false
    val sessionWindow = activity.healthSyncSessionWindowOrNull() ?: return false
    val samplesInsideWindow = activity.heartRateSamples.filter { sample ->
        !sample.sampledAt.isBefore(sessionWindow.start) &&
            !sample.sampledAt.isAfter(sessionWindow.end)
    }
    return samplesInsideWindow.isNotEmpty() && samplesInsideWindow.all { sample -> sample.bpm in 30..220 }
}

private fun isWorkoutActivityBaseEligible(
    activity: WorkoutActivity,
    operation: HealthSyncOperation,
): Boolean =
    activity.importParserValidity != ParserValidity.INVALID &&
        activity.workflowState in HEALTH_SYNC_RESOLVABLE_WORKOUT_STATES &&
        isWorkoutWorkflowCompatible(activity.workflowState, operation.state) &&
        activity.confirmedType != null &&
        activity.confirmedType != WorkoutType.UNKNOWN &&
        activity.healthSyncSessionWindowOrNull() != null

private fun isWorkoutWorkflowCompatible(
    workflowState: ActivityWorkflowState,
    operationState: SyncOperationState,
): Boolean = when (workflowState) {
    ActivityWorkflowState.READY_TO_SYNC -> operationState in setOf(
        SyncOperationState.PENDING,
        SyncOperationState.FAILED,
    )

    ActivityWorkflowState.SYNCING -> operationState in setOf(
        SyncOperationState.PENDING,
        SyncOperationState.FAILED,
        SyncOperationState.SYNCING,
        SyncOperationState.SYNCED,
    )

    ActivityWorkflowState.SYNCED -> operationState == SyncOperationState.SYNCED

    ActivityWorkflowState.SYNC_FAILED -> operationState in setOf(
        SyncOperationState.PENDING,
        SyncOperationState.FAILED,
        SyncOperationState.SYNCING,
        SyncOperationState.SYNCED,
    )

    ActivityWorkflowState.PENDING_REVIEW,
    ActivityWorkflowState.PENDING_CALIBRATION,
    ActivityWorkflowState.DISCARDED,
    -> false
}

private fun WorkoutActivity.healthSyncSessionWindowOrNull(): HealthSyncWorkoutSessionWindow? {
    val duration = metrics.duration
    if (duration.isZero || duration.isNegative) return null
    val end = try {
        metrics.startedAt.plus(duration)
    } catch (_: DateTimeException) {
        return null
    } catch (_: ArithmeticException) {
        return null
    }
    if (!end.isAfter(metrics.startedAt)) return null
    return HealthSyncWorkoutSessionWindow(start = metrics.startedAt, end = end)
}

private fun WorkoutActivity.finalPositiveDistanceMeters(): Double? {
    val candidates = listOf(
        metrics.distances.calibratedMeters,
        metrics.distances.originalMeters,
        metrics.distances.estimatedMeters,
    )
    for (candidate in candidates) {
        val value = candidate.presentDoubleOrNull() ?: continue
        if (value.isFinite() && value > 0.0) return value
    }
    return null
}

private fun isWeightMeasurementEligibleForWeight(measurement: WeightMeasurement): Boolean {
    val kilograms = when (measurement.unit) {
        WeightUnit.KILOGRAMS -> measurement.value
        WeightUnit.POUNDS -> measurement.value * 0.45359237
    }
    return kilograms.isFinite() && kilograms > 0.0
}

private fun isUserProfileEligibleForHeight(profile: UserProfile): Boolean =
    profile.heightCentimeters?.takeIf { it.isFinite() && it > 0.0 } != null

private fun DomainValue<Double>.presentDoubleOrNull(): Double? =
    (this as? DomainValue.Present)?.value

private fun String?.toHealthSyncZoneIdOrSystemDefault(): ZoneId = try {
    this?.let(ZoneId::of) ?: ZoneId.systemDefault()
} catch (_: DateTimeException) {
    ZoneId.systemDefault()
}

private fun String?.validZoneIdOrNull(): String? =
    this?.takeIf { runCatching { ZoneId.of(it) }.isSuccess }

private fun HealthSyncFrozenInstant.toInstantOrNull(): Instant? = runCatching {
    Instant.ofEpochSecond(epochSecond, nano.toLong())
}.getOrNull()

private inline fun <T> mapAggregateSafely(mapper: () -> T?): T? = try {
    mapper()
} catch (_: DateTimeException) {
    null
}

private val HEALTH_SYNC_RESOLVABLE_WORKOUT_STATES: Set<ActivityWorkflowState> = setOf(
    ActivityWorkflowState.READY_TO_SYNC,
    ActivityWorkflowState.SYNCING,
    ActivityWorkflowState.SYNCED,
    ActivityWorkflowState.SYNC_FAILED,
)

private data class ResolvedSyncMetadata(
    val clientRecordId: String?,
    val clientRecordVersion: Long?,
    val timeZoneId: String?,
    val createdAtEpochMillis: Long?,
    val updatedAtEpochMillis: Long?,
)

private data class ReconciledOperation(
    val operation: HealthSyncOperation,
    val repaired: Boolean,
)

private data class DailyStepsEnqueueResult(
    val operation: HealthSyncOperation? = null,
    val created: Boolean = false,
    val repaired: Boolean = false,
)

private data class DailyWorkoutCreditEvaluation(
    val credits: List<WorkoutStepsCredit>,
)

private fun HealthSyncOperationEntity.isDailyStepsOperationEntity(): Boolean =
    aggregateType == HealthSyncAggregateType.DAILY_SUMMARY.name &&
        recordType == HealthSyncRecordType.STEPS.name

private fun HealthSyncOperationEntity.isWorkoutStepsOperationEntity(): Boolean =
    aggregateType == HealthSyncAggregateType.WORKOUT_ACTIVITY.name &&
        recordType == HealthSyncRecordType.STEPS.name &&
        projectionChildKey == HealthProjectionChildKey.WorkoutSteps.stableValue &&
        action == HealthSyncOperationAction.INSERT.name

private fun HealthSyncOperationEntity.isUnfrozenLegacyDailyStepsOperation(): Boolean =
    isDailyStepsOperationEntity() &&
        operationSequence == 0L &&
        dailyStepsPayloadKind == null &&
        previousTotalSteps == null &&
        currentTotalSteps == null &&
        stepDelta == null &&
        stepStartTimeEpochMillis == null &&
        stepEndTimeEpochMillis == null

private fun List<Pair<HealthSyncOperationEntity, DailyStepsSyncPayload>>.isValidDailyStepsChain(
    allowPositiveFirstPreviousTotal: Boolean = false,
): Boolean {
    if (isEmpty()) return true
    val payloads = map { it.second }
    val first = payloads.first()
    val chainTimeZoneId = first.timeZoneId
    if (payloads.any { it.timeZoneId != chainTimeZoneId }) return false
    when (first.kind) {
        DailyStepsSyncPayloadKind.LEGACY_BRIDGE -> {
            if (first.sequence != 0L || first.previousTotalSteps != 0L) return false
        }

        DailyStepsSyncPayloadKind.INCREMENTAL -> {
            if (first.sequence != 1L || first.previousTotalSteps != 0L) return false
        }

        DailyStepsSyncPayloadKind.RESIDUAL_INCREMENTAL -> {
            if (first.sequence != 1L ||
                (!allowPositiveFirstPreviousTotal && first.previousTotalSteps != 0L)
            ) return false
        }
    }
    if (payloads.drop(1).any { it.kind == DailyStepsSyncPayloadKind.LEGACY_BRIDGE }) return false
    return payloads.zipWithNext().all { (previous, current) ->
        current.sequence == previous.sequence + 1L &&
            current.previousTotalSteps == previous.currentTotalSteps &&
            !current.startTime.isBefore(previous.endTime)
    }
}

private fun Long.subtractOrNull(other: Long): Long? = try {
    Math.subtractExact(this, other)
} catch (_: ArithmeticException) {
    null
}

internal data class PlannedWatchWorkoutPersistence(
    val persistedActivity: br.com.mwconsulting.championcompanion.core.database.WorkoutActivityEntity,
    val persistedImport: br.com.mwconsulting.championcompanion.core.database.WatchWorkoutImportEntity,
    val isReimport: Boolean,
)

internal fun planWatchWorkoutPersistence(
    incomingActivity: br.com.mwconsulting.championcompanion.core.database.WorkoutActivityEntity,
    incomingImport: br.com.mwconsulting.championcompanion.core.database.WatchWorkoutImportEntity,
    existingImport: br.com.mwconsulting.championcompanion.core.database.WatchWorkoutImportEntity?,
    existingActivity: br.com.mwconsulting.championcompanion.core.database.WorkoutActivityEntity?,
): PlannedWatchWorkoutPersistence? {
    if (incomingImport.parserValidity == "INVALID") {
        return null
    }
    if (existingImport == null) {
        return PlannedWatchWorkoutPersistence(
            persistedActivity = incomingActivity,
            persistedImport = incomingImport,
            isReimport = false,
        )
    }
    val persistedActivity = existingActivity ?: return null
    if (persistedActivity.recordedAtEpochMillis < incomingActivity.startedAtEpochMillis ||
        (persistedActivity.reviewedAtEpochMillis != null &&
            persistedActivity.reviewedAtEpochMillis < persistedActivity.recordedAtEpochMillis)
    ) {
        return null
    }
    return PlannedWatchWorkoutPersistence(
        persistedActivity = incomingActivity.copy(
            id = persistedActivity.id,
            recordedAtEpochMillis = persistedActivity.recordedAtEpochMillis,
            title = persistedActivity.title,
            notes = persistedActivity.notes,
            confirmedType = persistedActivity.confirmedType,
            workflowState = persistedActivity.workflowState,
            reviewedAtEpochMillis = persistedActivity.reviewedAtEpochMillis,
            calibrationId = persistedActivity.calibrationId,
            calibratedDistanceMeters = persistedActivity.calibratedDistanceMeters,
            calibratedAveragePaceSecondsPerKilometer = persistedActivity.calibratedAveragePaceSecondsPerKilometer,
            calibratedAverageSpeedMetersPerSecond = persistedActivity.calibratedAverageSpeedMetersPerSecond,
        ),
        persistedImport = incomingImport.copy(
            id = existingImport.id,
            activityId = persistedActivity.id,
        ),
        isReimport = true,
    )
}

internal data class PlannedD1Import(
    val measurementsToInsert: List<br.com.mwconsulting.championcompanion.core.database.HeartRateMeasurementEntity>,
    val pendingOperationAggregateIdsToCreate: Set<String>,
    val insertedCount: Int,
    val duplicateCount: Int,
    val conflictCount: Int,
    val pendingCreatedCount: Int,
    val pendingExistingCount: Int,
)

internal data class HeartRateHistoryWindow(
    val fromInclusive: Instant,
    val untilExclusive: Instant,
)

/** Calendário civil usado pela superfície de Saúde, sem janela móvel. */
internal fun heartRateHistoryDateRange(
    period: HeartRateHistoryPeriod,
    today: LocalDate,
): CalendarDateRange {
    val currentMonth = YearMonth.from(today)
    return when (period) {
        HeartRateHistoryPeriod.TODAY -> CalendarDateRange(
            fromInclusive = today,
            untilExclusive = today.plusDays(1),
        )

        HeartRateHistoryPeriod.LAST_7_DAYS -> {
            val sunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            CalendarDateRange(
                fromInclusive = sunday,
                untilExclusive = sunday.plusWeeks(1),
            )
        }

        HeartRateHistoryPeriod.LAST_30_DAYS -> CalendarDateRange(
            fromInclusive = currentMonth.atDay(1),
            untilExclusive = today.plusDays(1),
        )

        HeartRateHistoryPeriod.LAST_90_DAYS -> CalendarDateRange(
            fromInclusive = currentMonth.minusMonths(2).atDay(1),
            untilExclusive = today.plusDays(1),
        )

        HeartRateHistoryPeriod.LAST_365_DAYS -> CalendarDateRange(
            fromInclusive = currentMonth.minusMonths(11).atDay(1),
            untilExclusive = today.plusDays(1),
        )
    }
}

/** Janela histórica anterior, mantida para consumidores internos legados. */
internal fun historyWindow(
    period: HeartRateHistoryPeriod,
    zoneId: ZoneId,
    now: Instant,
): HeartRateHistoryWindow {
    val today = LocalDateTime.ofInstant(now, zoneId).toLocalDate()
    val fromDate = today.minusDays(period.windowDays - 1L)
    return HeartRateHistoryWindow(
        fromInclusive = fromDate.atStartOfDay(zoneId).toInstant(),
        untilExclusive = today.plusDays(1).atStartOfDay(zoneId).toInstant(),
    )
}

internal fun canonicalHistorySources(sources: Set<HeartRateSource>): Set<HeartRateSource> =
    sources.intersect(HISTORY_ALLOWED_SOURCES)

internal fun buildHeartRateHistorySnapshot(
    period: HeartRateHistoryPeriod,
    zoneId: ZoneId,
    measurements: List<HeartRateMeasurement>,
    workoutEntries: List<HeartRateHistoryEntry> = emptyList(),
    dateRange: CalendarDateRange? = null,
): HeartRateHistorySnapshot {
    val validMeasurements = measurements.filter { measurement ->
        measurement.validity == br.com.mwconsulting.championcompanion.core.model.DataValidity.VALID &&
            measurement.measuredAt != null &&
            measurement.source in HISTORY_MEASUREMENT_SOURCES
    }
    val measurementEntries = validMeasurements.mapNotNull { measurement ->
        val measuredAt = measurement.measuredAt ?: return@mapNotNull null
        HeartRateHistoryEntry(
            origin = HeartRateHistoryOrigin.PersistedMeasurement(measurement.id),
            measuredAt = measuredAt,
            bpm = measurement.bpm,
            source = measurement.source,
        )
    }
    val entries = measurementEntries + workoutEntries.filter { entry ->
        entry.source == HeartRateSource.WATCH_WORKOUT_ACTIVITY && entry.bpm in 30..220
    }.let { allEntries ->
        allEntries.sortedWith(
            compareByDescending<HeartRateHistoryEntry> { it.measuredAt }
                .thenBy { it.source.name },
        )
    }
    return HeartRateHistorySnapshot(
        period = period,
        entries = entries,
        aggregates = buildHeartRateHistoryAggregates(entries),
        chart = buildHeartRateChartSeries(
            period = period,
            zoneId = zoneId,
            entries = entries,
            dateRange = dateRange,
        ),
    )
}

internal fun buildHeartRateHistoryAggregates(entries: List<HeartRateHistoryEntry>): HeartRateHistoryAggregates =
    if (entries.isEmpty()) {
        HeartRateHistoryAggregates()
    } else {
        HeartRateHistoryAggregates(
            measurementCount = entries.size,
            averageBpm = entries.map { it.bpm }.average(),
            minimumBpm = entries.minOf { it.bpm },
            maximumBpm = entries.maxOf { it.bpm },
        )
    }

internal fun buildHeartRateChartSeries(
    period: HeartRateHistoryPeriod,
    zoneId: ZoneId,
    entries: List<HeartRateHistoryEntry>,
    dateRange: CalendarDateRange? = null,
): HeartRateChartSeries = when (period) {
    HeartRateHistoryPeriod.TODAY -> if (dateRange == null) {
        HeartRateChartSeries(
            resolution = HeartRateChartResolution.INTRADAY,
            points = entries
                .sortedBy { it.measuredAt }
                .map { entry -> entry.toHeartRateChartPoint() },
        )
    } else {
        buildIntradayHeartRateChartSeries(dateRange, zoneId, entries)
    }

    HeartRateHistoryPeriod.LAST_7_DAYS,
    HeartRateHistoryPeriod.LAST_30_DAYS,
    -> if (dateRange == null) {
        HeartRateChartSeries(
            resolution = HeartRateChartResolution.DAILY,
            points = entries
                .groupBy { entry -> LocalDateTime.ofInstant(entry.measuredAt, zoneId).toLocalDate() }
                .toSortedMap()
                .map { (localDate, dayEntries) ->
                    dayEntries.toHeartRateChartPoint(
                        timestamp = localDate.atTime(LocalTime.MIDNIGHT).atZone(zoneId).toInstant(),
                    )
                },
        )
    } else {
        buildDailyHeartRateChartSeries(dateRange, zoneId, entries)
    }

    HeartRateHistoryPeriod.LAST_90_DAYS,
    HeartRateHistoryPeriod.LAST_365_DAYS,
    -> if (dateRange == null) {
        HeartRateChartSeries(
            resolution = HeartRateChartResolution.MONTHLY,
            points = entries
                .groupBy { entry -> YearMonth.from(entry.measuredAt.atZone(zoneId)) }
                .toSortedMap()
                .map { (month, monthEntries) ->
                    monthEntries.toHeartRateChartPoint(
                        timestamp = month.atDay(1).atStartOfDay(zoneId).toInstant(),
                    )
                },
        )
    } else {
        buildMonthlyHeartRateChartSeries(dateRange, zoneId, entries)
    }
}

internal fun chartResolutionFor(period: HeartRateHistoryPeriod): HeartRateChartResolution = when (period) {
    HeartRateHistoryPeriod.TODAY -> HeartRateChartResolution.INTRADAY
    HeartRateHistoryPeriod.LAST_7_DAYS,
    HeartRateHistoryPeriod.LAST_30_DAYS,
    -> HeartRateChartResolution.DAILY
    HeartRateHistoryPeriod.LAST_90_DAYS,
    HeartRateHistoryPeriod.LAST_365_DAYS,
    -> HeartRateChartResolution.MONTHLY
}

private fun buildIntradayHeartRateChartSeries(
    dateRange: CalendarDateRange,
    zoneId: ZoneId,
    entries: List<HeartRateHistoryEntry>,
): HeartRateChartSeries {
    val start = dateRange.fromInclusive.atStartOfDay(zoneId).toInstant()
    val until = dateRange.untilExclusive.atStartOfDay(zoneId).toInstant()
    return HeartRateChartSeries(
        resolution = HeartRateChartResolution.INTRADAY,
        points = entries
            .filter { entry ->
                !entry.measuredAt.isBefore(start) && entry.measuredAt.isBefore(until)
            }
            .sortedBy { it.measuredAt }
            .map { it.toHeartRateChartPoint() },
    )
}

private fun buildDailyHeartRateChartSeries(
    dateRange: CalendarDateRange,
    zoneId: ZoneId,
    entries: List<HeartRateHistoryEntry>,
): HeartRateChartSeries {
    val entriesByDate = entries.groupBy { it.measuredAt.atZone(zoneId).toLocalDate() }
    return HeartRateChartSeries(
        resolution = HeartRateChartResolution.DAILY,
        points = dateRange.dates.map { localDate ->
            entriesByDate[localDate].orEmpty().toHeartRateChartPoint(
                timestamp = localDate.atStartOfDay(zoneId).toInstant(),
            )
        },
    )
}

private fun buildMonthlyHeartRateChartSeries(
    dateRange: CalendarDateRange,
    zoneId: ZoneId,
    entries: List<HeartRateHistoryEntry>,
): HeartRateChartSeries {
    val firstMonth = YearMonth.from(dateRange.fromInclusive)
    val lastMonth = YearMonth.from(dateRange.untilExclusive.minusDays(1))
    val entriesByMonth = entries.groupBy { YearMonth.from(it.measuredAt.atZone(zoneId)) }
    val months = generateSequence(firstMonth) { month ->
        month.plusMonths(1).takeIf { !it.isAfter(lastMonth) }
    }.toList()
    return HeartRateChartSeries(
        resolution = HeartRateChartResolution.MONTHLY,
        points = months.map { month ->
            entriesByMonth[month].orEmpty().toHeartRateChartPoint(
                timestamp = month.atDay(1).atStartOfDay(zoneId).toInstant(),
            )
        },
    )
}

private fun HeartRateHistoryEntry.toHeartRateChartPoint(): HeartRateChartPoint =
    HeartRateChartPoint(
        timestamp = measuredAt,
        bpm = bpm.toDouble(),
        minimumBpm = bpm,
        maximumBpm = bpm,
        sampleCount = 1,
    )

private fun List<HeartRateHistoryEntry>.toHeartRateChartPoint(timestamp: Instant): HeartRateChartPoint =
    if (isEmpty()) {
        HeartRateChartPoint(timestamp = timestamp, bpm = null, sampleCount = 0)
    } else {
        HeartRateChartPoint(
            timestamp = timestamp,
            bpm = map { it.bpm }.average(),
            minimumBpm = minOf { it.bpm },
            maximumBpm = maxOf { it.bpm },
            sampleCount = size,
        )
    }

internal fun planD1Import(
    existingMeasurementsByKey: Map<String, br.com.mwconsulting.championcompanion.core.database.HeartRateMeasurementEntity>,
    existingPendingAggregateIds: Set<String>,
    incomingMeasurements: List<br.com.mwconsulting.championcompanion.core.database.HeartRateMeasurementEntity>,
): PlannedD1Import {
    val measurementsByKey = existingMeasurementsByKey.toMutableMap()
    val pendingAggregateIds = existingPendingAggregateIds.toMutableSet()
    val measurementsToInsert = mutableListOf<br.com.mwconsulting.championcompanion.core.database.HeartRateMeasurementEntity>()
    val pendingToCreate = linkedSetOf<String>()
    var insertedCount = 0
    var duplicateCount = 0
    var conflictCount = 0
    var pendingCreatedCount = 0
    var pendingExistingCount = 0

    incomingMeasurements.forEach { incoming ->
        val existing = measurementsByKey[incoming.deduplicationKey]
        if (existing == null) {
            measurementsByKey[incoming.deduplicationKey] = incoming
            measurementsToInsert += incoming
            insertedCount += 1
            when (planPendingOperation(incoming.id, pendingAggregateIds, pendingToCreate, incoming.isEligibleForD1Pending())) {
                PendingPlan.CREATED -> pendingCreatedCount += 1
                PendingPlan.EXISTING -> pendingExistingCount += 1
                PendingPlan.SKIPPED -> Unit
            }
        } else {
            if (existing.bpm == incoming.bpm) duplicateCount += 1 else conflictCount += 1
            when (planPendingOperation(existing.id, pendingAggregateIds, pendingToCreate, existing.isEligibleForD1Pending())) {
                PendingPlan.CREATED -> pendingCreatedCount += 1
                PendingPlan.EXISTING -> pendingExistingCount += 1
                PendingPlan.SKIPPED -> Unit
            }
        }
    }

    return PlannedD1Import(
        measurementsToInsert = measurementsToInsert,
        pendingOperationAggregateIdsToCreate = pendingToCreate,
        insertedCount = insertedCount,
        duplicateCount = duplicateCount,
        conflictCount = conflictCount,
        pendingCreatedCount = pendingCreatedCount,
        pendingExistingCount = pendingExistingCount,
    )
}

private enum class PendingPlan {
    CREATED,
    EXISTING,
    SKIPPED,
}

private fun planPendingOperation(
    aggregateId: String,
    pendingAggregateIds: MutableSet<String>,
    pendingToCreate: MutableSet<String>,
    eligible: Boolean,
): PendingPlan {
    if (!eligible) return PendingPlan.SKIPPED
    return if (pendingAggregateIds.add(aggregateId)) {
        pendingToCreate += aggregateId
        PendingPlan.CREATED
    } else {
        PendingPlan.EXISTING
    }
}

private fun br.com.mwconsulting.championcompanion.core.database.HeartRateMeasurementEntity.isEligibleForD1Pending(): Boolean =
    source == HeartRateSource.WATCH_D1_HISTORY.name &&
        validity == "VALID" &&
        measuredAtEpochMillis != null

private fun WorkoutActivityFilter.toStoredWorkflowStates(): List<String> = when (this) {
    WorkoutActivityFilter.ALL -> emptyList()
    WorkoutActivityFilter.PENDING_REVIEW -> listOf("PENDING_REVIEW")
    WorkoutActivityFilter.PENDING_CALIBRATION -> listOf("PENDING_CALIBRATION")
    WorkoutActivityFilter.READY_TO_SYNC -> listOf("READY_TO_SYNC")
    WorkoutActivityFilter.SYNCED -> listOf("SYNCED")
    WorkoutActivityFilter.SYNC_FAILED -> listOf("SYNC_FAILED")
    WorkoutActivityFilter.DISCARDED -> listOf("DISCARDED")
}

private val HISTORY_ALLOWED_SOURCES: Set<HeartRateSource> = setOf(
    HeartRateSource.MANUAL_MEASUREMENT,
    HeartRateSource.WATCH_D1_HISTORY,
    HeartRateSource.WATCH_WORKOUT_ACTIVITY,
)

private val HISTORY_MEASUREMENT_SOURCES: Set<HeartRateSource> = setOf(
    HeartRateSource.MANUAL_MEASUREMENT,
    HeartRateSource.WATCH_D1_HISTORY,
)
