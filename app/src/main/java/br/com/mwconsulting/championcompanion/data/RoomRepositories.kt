package br.com.mwconsulting.championcompanion.data
import androidx.room3.withWriteTransaction
import br.com.mwconsulting.championcompanion.BuildConfig
import br.com.mwconsulting.championcompanion.core.common.IoDispatcher
import br.com.mwconsulting.championcompanion.core.database.ChampionDatabase
import br.com.mwconsulting.championcompanion.core.database.DeviceEntity
import br.com.mwconsulting.championcompanion.core.database.DiagnosticDerivedOutputEntity
import br.com.mwconsulting.championcompanion.core.database.HealthSyncOperationEntity
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSession
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionId
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionRepository
import br.com.mwconsulting.championcompanion.core.model.CALENDAR_HEART_RATE_SOURCES
import br.com.mwconsulting.championcompanion.core.model.CalendarDateRange
import br.com.mwconsulting.championcompanion.core.model.CalendarInstantRange
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportMetadata
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportPersistenceResult
import br.com.mwconsulting.championcompanion.core.model.DailySummary
import br.com.mwconsulting.championcompanion.core.model.DailySummaryId
import br.com.mwconsulting.championcompanion.core.model.DailySummaryRepository
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
import br.com.mwconsulting.championcompanion.core.model.MergeWatchDailySummaryInput
import br.com.mwconsulting.championcompanion.core.model.DomainResult
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsRepository
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsSnapshot
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.SaveD1HeartRateMeasurementsInput
import br.com.mwconsulting.championcompanion.core.model.SaveManualHeartRateMeasurementInput
import br.com.mwconsulting.championcompanion.core.model.SafeSyncError
import br.com.mwconsulting.championcompanion.core.model.SyncOperationState
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibration
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibrationRepository
import br.com.mwconsulting.championcompanion.core.model.UserProfileId
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImport
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportLookup
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportMetadata
import br.com.mwconsulting.championcompanion.core.model.WeightMeasurementId
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityFilter
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import br.com.mwconsulting.championcompanion.core.model.WorkoutWorkflowSummary
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.model.valueOrNull
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext

private const val SQLITE_IN_CLAUSE_CHUNK_SIZE = 500

@Singleton
class RoomRepositories @Inject constructor(
    private val database: ChampionDatabase,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DeviceRepository, DailySummaryRepository, HeartRateRepository, WorkoutActivityRepository,
    TreadmillCalibrationRepository, AppWorkoutSessionRepository, HealthSyncRepository, DiagnosticPayloadRepository,
    PersonalMetricsRepository {
    private val ids = UuidV7Generator()

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
            database.userProfileDao().observeLatestWeight(profile.id)
                .map { weight ->
                    PersonalMetricsSnapshot(
                        heightCentimeters = profile.heightCentimeters,
                        weightKilograms = weight?.toKilogramsOrNull(),
                        latestWeightMeasuredAt = weight?.measuredAtEpochMillis?.let(Instant::ofEpochMilli),
                        latestWeightOrigin = weight?.origin?.let { enumValueOfOrNull<DataOrigin>(it) },
                        manualStrideLengthMeters = null,
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

    override suspend fun mergeWatchDailySummary(input: MergeWatchDailySummaryInput): RepositoryResult<DailySummary> = write {
        val deviceId = input.deviceId.value.toString()
        val device = database.deviceDao().findById(deviceId) ?: return@write RepositoryResult.NotFound
        val existing = database.dailySummaryDao().findByNaturalKey(
            deviceId = deviceId,
            origin = DataOrigin.WATCH_DAILY_SUMMARY.name,
            localDateIso = input.localDate.toString(),
        )

        val persistedSummary = if (existing == null) {
            val created = createWatchDailySummary(input) ?: return@write RepositoryResult.UnexpectedState
            database.dailySummaryDao().insert(created.toEntity())
            created
        } else {
            val nextSummary = if (input.incomingSteps > existing.steps) {
                val updated = existing.copy(
                    steps = input.incomingSteps,
                    estimatedDistanceMeters = input.estimatedDistanceMeters,
                    estimatedCaloriesKilocalories = input.estimatedCaloriesKilocalories,
                    collectedAtEpochMillis = input.collectedAt.toEpochMilli(),
                    updatedAtEpochMillis = input.updatedAt.toEpochMilli(),
                )
                database.dailySummaryDao().update(updated)
                updated.toDomain()
            } else {
                existing.toDomain()
            }
            nextSummary ?: return@write RepositoryResult.UnexpectedState
        }

        database.deviceDao().update(
            device.copy(
                lastImportedAtEpochMillis = input.updatedAt.toEpochMilli(),
                importParserVersion = input.parserVersion.value,
            ),
        )
        RepositoryResult.Success(persistedSummary)
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
        val window = historyWindow(
            period = query.period,
            zoneId = query.zoneId,
            now = Instant.now(),
        )
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

    override suspend fun persistImported(activity: WorkoutActivity, importRecord: WatchWorkoutImport): RepositoryResult<Unit> = write {
        val activityEntity = activity.toEntity()
        val importEntity = importRecord.toEntity()
        val exactImport = database.watchWorkoutImportDao().findByNaturalKey(importEntity.deviceId, importEntity.origin, importEntity.crc)
        val existingActivity = when {
            exactImport != null -> database.workoutActivityDao().findById(exactImport.activityId)
            else -> database.workoutActivityDao().findByStartedAt(
                deviceId = activityEntity.deviceId,
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
        if (!plan.isReimport) {
            database.workoutActivityDao().insert(plan.persistedActivity)
            database.watchWorkoutImportDao().insert(plan.persistedImport)
        } else {
            database.workoutActivityDao().update(plan.persistedActivity)
            database.watchWorkoutImportDao().update(plan.persistedImport)
        }
        val persistedId = plan.persistedActivity.id
        database.workoutDetailDao().deleteIntervalsForActivity(persistedId)
        database.workoutDetailDao().deleteHeartRateSamplesForActivity(persistedId)
        database.workoutDetailDao().insertIntervals(activity.toIntervals(persistedId, generateIds(activity.intervals.size)))
        database.workoutDetailDao().insertHeartRateSamples(activity.toHeartRateSamples(persistedId, generateIds(activity.heartRateSamples.size)))
        RepositoryResult.Success(Unit)
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
    override fun observeActivities(filter: WorkoutActivityFilter): Flow<List<WorkoutActivity>> = when (filter) {
        WorkoutActivityFilter.ALL -> database.workoutActivityDao().observeAll()
        else -> database.workoutActivityDao().observeByWorkflowStates(filter.toStoredWorkflowStates())
    }.transformLatest { activities ->
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActivity(activityId: WorkoutActivityId): Flow<WorkoutActivity?> =
        database.workoutActivityDao().observeById(activityId.value.toString())
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

    override suspend fun updateWatchWorkoutImportMetadata(metadata: WatchWorkoutImportMetadata): RepositoryResult<Unit> = write {
        val metadataDao = database.deviceImportMetadataDao()
        val existing = metadataDao.findByDeviceAndImportType(metadata.deviceId.value.toString(), WATCH_D3_D4_IMPORT_TYPE)
        metadataDao.upsert(metadata.toEntity(id = existing?.id ?: ids.next().toString(), importType = WATCH_D3_D4_IMPORT_TYPE))
        RepositoryResult.Success(Unit)
    }

    override suspend fun submitReview(input: br.com.mwconsulting.championcompanion.core.model.SubmitWorkoutReviewInput): RepositoryResult<WorkoutActivity> = write {
        val entity = database.workoutActivityDao().findById(input.activityId.value.toString()) ?: return@write RepositoryResult.NotFound
        val importRecord = database.watchWorkoutImportDao().findByActivityId(entity.id) ?: return@write RepositoryResult.NotFound
        val activity = entity.toDomain(
            importRecord,
            database.workoutDetailDao().findIntervalsForActivity(entity.id),
            database.workoutDetailDao().findHeartRateSamplesForActivity(entity.id),
        ) ?: return@write RepositoryResult.UnexpectedState
        val reviewed = activity.submitReview(input).valueOrNull() ?: return@write RepositoryResult.UnexpectedState
        database.workoutActivityDao().update(reviewed.toEntity())
        RepositoryResult.Success(reviewed)
    }

    override suspend fun review(activityId: WorkoutActivityId, type: WorkoutType, reviewedAt: Instant): RepositoryResult<WorkoutActivity> = write {
        val entity = database.workoutActivityDao().findById(activityId.value.toString()) ?: return@write RepositoryResult.NotFound
        val importRecord = database.watchWorkoutImportDao().findByActivityId(entity.id) ?: return@write RepositoryResult.NotFound
        val existing = entity.toDomain(
            importRecord,
            database.workoutDetailDao().findIntervalsForActivity(entity.id),
            database.workoutDetailDao().findHeartRateSamplesForActivity(entity.id),
        ) ?: return@write RepositoryResult.UnexpectedState
        submitReview(
            br.com.mwconsulting.championcompanion.core.model.SubmitWorkoutReviewInput(
                activityId = activityId,
                confirmedType = type,
                title = existing.title,
                notes = existing.notes,
                reviewedAt = reviewedAt,
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
        val importRecord = database.watchWorkoutImportDao().findByActivityId(activityEntity.id)
            ?: return@write RepositoryResult.NotFound
        val activity = activityEntity.toDomain(
            importRecord,
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

    override fun observeWeightsBetween(range: CalendarInstantRange) =
        database.userProfileDao()
            .observeBetween(
                fromEpochMillis = range.fromInclusive.toEpochMilli(),
                untilEpochMillis = range.untilExclusive.toEpochMilli(),
            )
            .map { entities -> entities.mapNotNull { it.toCalendarSummary() } }
            .flowOn(ioDispatcher)

    override fun observeRecoverable(): Flow<List<AppWorkoutSession>> = database.appWorkoutSessionDao().observeRecoverable()
        .map { it.mapNotNull { entity -> entity.toDomain() } }
        .flowOn(ioDispatcher)

    override suspend fun recover(sessionId: AppWorkoutSessionId, at: Instant): RepositoryResult<AppWorkoutSession> = write {
        val entity = database.appWorkoutSessionDao().findById(sessionId.value.toString()) ?: return@write RepositoryResult.NotFound
        if (entity.state != "AWAITING_RECOVERY") return@write RepositoryResult.UnexpectedState
        val updated = entity.copy(state = "ACTIVE", activeSinceEpochMillis = at.toEpochMilli())
        database.appWorkoutSessionDao().update(updated)
        updated.toDomain()?.let { RepositoryResult.Success(it) } ?: RepositoryResult.UnexpectedState
    }

    override suspend fun finish(sessionId: AppWorkoutSessionId, activityId: WorkoutActivityId, at: Instant): RepositoryResult<AppWorkoutSession> = write {
        val entity = database.appWorkoutSessionDao().findById(sessionId.value.toString()) ?: return@write RepositoryResult.NotFound
        if (database.workoutActivityDao().findById(activityId.value.toString()) == null) return@write RepositoryResult.NotFound
        if (entity.state !in setOf("ACTIVE", "PAUSED", "AWAITING_RECOVERY")) return@write RepositoryResult.UnexpectedState
        val updated = entity.copy(state = "FINISHED", endedAtEpochMillis = at.toEpochMilli(), activeSinceEpochMillis = null, pausedSinceEpochMillis = null, workoutActivityId = activityId.value.toString())
        database.appWorkoutSessionDao().update(updated)
        updated.toDomain()?.let { RepositoryResult.Success(it) } ?: RepositoryResult.UnexpectedState
    }

    override suspend fun ensureOperation(key: HealthSyncOperationKey): RepositoryResult<HealthSyncOperation> = write {
        ensureOperationInternal(key)
    }

    override suspend fun findProcessable(limit: Int): RepositoryResult<List<HealthSyncOperation>> = write {
        if (limit <= 0) return@write RepositoryResult.Success(emptyList())
        val repaired = database.healthSyncOperationDao().findProcessable(limit)
            .mapNotNull { entity ->
                val key = entity.toDomain()?.key ?: return@mapNotNull null
                when (val ensured = ensureOperationInternal(key)) {
                    is RepositoryResult.Success -> ensured.value
                    else -> null
                }
            }
        RepositoryResult.Success(repaired.filter { it.state != SyncOperationState.SYNCED })
    }

    override fun observeSyncSummary(): Flow<HealthSyncSummary> =
        database.healthSyncOperationDao().observeSummary()
            .map { it.toDomain() }
            .flowOn(ioDispatcher)

    override suspend fun findOperationsForAggregate(
        aggregate: HealthSyncAggregate,
    ): RepositoryResult<List<HealthSyncOperation>> = withContext(ioDispatcher) {
        RepositoryResult.Success(
            database.healthSyncOperationDao()
                .findByAggregate(aggregate.id.toString(), aggregate.type.name)
                .mapNotNull { it.toDomain() },
        )
    }

    override fun observeForAggregates(aggregates: Set<HealthSyncAggregate>): Flow<List<HealthSyncOperation>> {
        return observeSyncEntitiesForAggregates(aggregates)
            .map { entities -> entities.mapNotNull { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun observeForAggregatesWithIntegrity(
        aggregates: Set<HealthSyncAggregate>,
    ): Flow<HealthSyncQueueObservation> {
        return combine(
            database.healthSyncOperationDao().observeAll(),
            observeStructurallyValidHealthSyncAggregates(),
        ) { allEntities, validAggregates ->
            val validOperations = allEntities.mapNotNull { it.toDomain() }
            HealthSyncQueueObservation(
                operations = validOperations.filter { it.key.aggregate in aggregates },
                hasInvalidOperation = validOperations.size != allEntities.size,
                hasUnresolvedAggregate = validOperations.any { it.key.aggregate !in validAggregates },
            )
        }.flowOn(ioDispatcher)
    }

    private fun observeStructurallyValidHealthSyncAggregates(): Flow<Set<HealthSyncAggregate>> = combine(
        database.dailySummaryDao().observeAll(),
        database.heartRateDao().observeAll(),
        database.workoutActivityDao().observeAllCalendarSummaries(),
        database.userProfileDao().observeAllWeights(),
        database.userProfileDao().observeAllProfiles(),
    ) { dailySummaries, heartRates, workouts, weights, profiles ->
        buildSet {
            dailySummaries.mapNotNull { entity ->
                try {
                    entity.toDomain()
                } catch (_: DateTimeException) {
                    null
                }
            }.forEach { add(HealthSyncAggregate.DailySummary(it.id)) }
            heartRates.mapNotNull { entity ->
                try {
                    entity.toDomain()
                } catch (_: DateTimeException) {
                    null
                }
            }.forEach { add(HealthSyncAggregate.HeartRateMeasurement(it.id)) }
            workouts.mapNotNull { it.toCalendarSummary() }
                .forEach { add(HealthSyncAggregate.WorkoutActivity(it.activityId)) }
            weights.mapNotNull { it.toCalendarSummary() }
                .forEach { add(HealthSyncAggregate.WeightMeasurement(it.measurementId)) }
            profiles.mapNotNull { it.toDomain() }
                .filter { it.heightCentimeters != null }
                .forEach { add(HealthSyncAggregate.UserProfileHeight(it.id)) }
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
            )
        }

    override suspend fun markSynced(operationId: HealthSyncOperationId, at: Instant): RepositoryResult<Unit> =
        transitionSync(operationId) { operation ->
            if (operation.state != SyncOperationState.SYNCING.name) null
            else operation.copy(
                state = SyncOperationState.SYNCED.name,
                lastError = null,
                syncedAtEpochMillis = at.toEpochMilli(),
                updatedAtEpochMillis = at.toEpochMilli(),
            )
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

    private suspend fun loadWorkoutActivity(activity: br.com.mwconsulting.championcompanion.core.database.WorkoutActivityEntity): WorkoutActivity? {
        val importRecord = database.watchWorkoutImportDao().findByActivityId(activity.id) ?: return null
        return activity.toDomain(
            importRecord = importRecord,
            intervals = database.workoutDetailDao().findIntervalsForActivity(activity.id),
            samples = database.workoutDetailDao().findHeartRateSamplesForActivity(activity.id),
        )
    }

    private suspend fun transitionWorkoutSync(
        activityId: WorkoutActivityId,
        transition: (WorkoutActivity) -> br.com.mwconsulting.championcompanion.core.model.DomainResult<WorkoutActivity>,
    ): RepositoryResult<WorkoutActivity> {
        val entity = database.workoutActivityDao().findById(activityId.value.toString()) ?: return RepositoryResult.NotFound
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

    private inline fun <reified T : Enum<T>> enumValueOfOrNull(value: String): T? =
        runCatching { enumValueOf<T>(value) }.getOrNull()

    private suspend fun ensureOperationInternal(
        key: HealthSyncOperationKey,
    ): RepositoryResult<HealthSyncOperation> {
        val metadata = resolveSyncMetadata(key) ?: return RepositoryResult.NotFound
        val syncDao = database.healthSyncOperationDao()
        val existing = syncDao.findByNaturalKey(
            aggregateId = key.aggregate.id.toString(),
            aggregateType = key.aggregate.type.name,
            recordType = key.recordType.name,
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
        val metadata = resolveSyncMetadata(key) ?: return null
        val existing = database.healthSyncOperationDao().findByNaturalKey(
            aggregateId = key.aggregate.id.toString(),
            aggregateType = key.aggregate.type.name,
            recordType = key.recordType.name,
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
            val result = ensureReconciledOperation(
                HealthSyncOperationKey(
                    aggregate = HealthSyncAggregate.DailySummary(DailySummaryId(parseUuid(summary.id) ?: return@forEach)),
                    recordType = HealthSyncRecordType.STEPS,
                ),
            )
            if (result != null) {
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
            if (profile.heightCentimeters?.takeIf { it.isFinite() && it > 0.0 } != null && profileId != null) {
                val result = ensureReconciledOperation(
                    HealthSyncOperationKey(
                        aggregate = HealthSyncAggregate.UserProfileHeight(UserProfileId(profileId)),
                        recordType = HealthSyncRecordType.HEIGHT,
                    ),
                )
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
        if (!aggregateExists(current)) return@write RepositoryResult.NotFound
        val next = transform(current) ?: return@write RepositoryResult.UnexpectedState
        if (syncDao.update(next) == 1) RepositoryResult.Success(Unit) else RepositoryResult.UnexpectedState
    }

    private suspend fun aggregateExists(operation: HealthSyncOperationEntity): Boolean = when (operation.aggregateType) {
        "DAILY_SUMMARY" -> database.dailySummaryDao().findById(operation.aggregateId) != null
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

            is HealthSyncAggregate.HeartRateMeasurement -> {
                val entity = database.heartRateDao().findById(aggregate.measurementId.value.toString()) ?: return null
                resolveHeartRateSyncMetadata(entity)?.takeIf { key.recordType == HealthSyncRecordType.HEART_RATE }
                    ?.copy(clientRecordId = stableClientRecordId(key))
            }

            is HealthSyncAggregate.WorkoutActivity -> {
                val entity = database.workoutActivityDao().findById(aggregate.activityId.value.toString()) ?: return null
                val version = maxOf(
                    entity.importedAtEpochMillis,
                    entity.reviewedAtEpochMillis ?: Long.MIN_VALUE,
                )
                ResolvedSyncMetadata(
                    clientRecordId = stableClientRecordId(key),
                    clientRecordVersion = version,
                    timeZoneId = null,
                    createdAtEpochMillis = entity.importedAtEpochMillis,
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
                metadata.clientRecordVersion > existing.clientRecordVersion &&
                existing.state != SyncOperationState.SYNCING.name
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
    )

    private fun stableClientRecordId(key: HealthSyncOperationKey): String =
        "${BuildConfig.APPLICATION_ID}:${key.aggregate.type.name}:${key.aggregate.id}:${key.recordType.name}"

    private fun parseUuid(value: String): UUID? = br.com.mwconsulting.championcompanion.core.model.UuidV7.parse(value).valueOrNull()

    private suspend fun <T> write(block: suspend () -> RepositoryResult<T>): RepositoryResult<T> = withContext(ioDispatcher) {
        database.withWriteTransaction { block() }
    }

    private fun manualMeasurementDeduplicationKey(sessionId: HeartRateMeasurementSessionId): String =
        br.com.mwconsulting.championcompanion.core.model.HeartRateDeduplicationKey.MeasurementSession(sessionId)
            .encode()

    private fun generateIds(count: Int): Iterator<java.util.UUID> = List(count) { ids.next() }.iterator()
}

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
    if (incomingActivity.parserValidity == "INVALID" || incomingImport.parserValidity == "INVALID") {
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
    return PlannedWatchWorkoutPersistence(
        persistedActivity = incomingActivity.copy(
            id = persistedActivity.id,
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
    }
    return HeartRateHistorySnapshot(
        period = period,
        entries = entries.sortedWith(
            compareByDescending<HeartRateHistoryEntry> { it.measuredAt }
                .thenBy { it.source.name },
        ),
        aggregates = buildHeartRateHistoryAggregates(entries),
        chart = buildHeartRateChartSeries(period, zoneId, entries),
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
): HeartRateChartSeries = when (period) {
    HeartRateHistoryPeriod.TODAY -> HeartRateChartSeries(
        resolution = HeartRateChartResolution.INTRADAY,
        points = entries
            .sortedBy { it.measuredAt }
            .map { entry ->
                HeartRateChartPoint(
                    timestamp = entry.measuredAt,
                    bpm = entry.bpm.toDouble(),
                    minimumBpm = entry.bpm,
                    maximumBpm = entry.bpm,
                    sampleCount = 1,
                )
            },
    )

    HeartRateHistoryPeriod.LAST_7_DAYS,
    HeartRateHistoryPeriod.LAST_30_DAYS,
    -> HeartRateChartSeries(
        resolution = HeartRateChartResolution.DAILY,
        points = entries
            .groupBy { entry -> LocalDateTime.ofInstant(entry.measuredAt, zoneId).toLocalDate() }
            .toSortedMap()
            .map { (localDate, dayEntries) ->
                HeartRateChartPoint(
                    timestamp = localDate.atTime(LocalTime.MIDNIGHT).atZone(zoneId).toInstant(),
                    bpm = dayEntries.map { it.bpm }.average(),
                    minimumBpm = dayEntries.minOf { it.bpm },
                    maximumBpm = dayEntries.maxOf { it.bpm },
                    sampleCount = dayEntries.size,
                )
            },
    )
}

internal fun chartResolutionFor(period: HeartRateHistoryPeriod): HeartRateChartResolution = when (period) {
    HeartRateHistoryPeriod.TODAY -> HeartRateChartResolution.INTRADAY
    HeartRateHistoryPeriod.LAST_7_DAYS,
    HeartRateHistoryPeriod.LAST_30_DAYS,
    -> HeartRateChartResolution.DAILY
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
