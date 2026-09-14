package br.com.mwconsulting.championcompanion.data

import androidx.room3.withWriteTransaction
import br.com.mwconsulting.championcompanion.BuildConfig
import br.com.mwconsulting.championcompanion.core.common.IoDispatcher
import br.com.mwconsulting.championcompanion.core.database.ChampionDatabase
import br.com.mwconsulting.championcompanion.core.database.HealthSyncOperationEntity
import br.com.mwconsulting.championcompanion.core.database.WatchDailyD1SampleEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutActivityEntity
import br.com.mwconsulting.championcompanion.core.model.ActivityWorkflowState
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.DataValidity
import br.com.mwconsulting.championcompanion.core.model.HEALTH_SYNC_FROZEN_DAILY_D1_METRIC_PAYLOAD_SCHEMA_VERSION
import br.com.mwconsulting.championcompanion.core.model.HEALTH_SYNC_FROZEN_DAILY_D1_PAYLOAD_SCHEMA_VERSION
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionChildKey
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionOrigin
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregate
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregateType
import br.com.mwconsulting.championcompanion.core.model.HealthSyncBacklogReconciliationResult
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenDailyD1MetricPayload
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenDailyD1Payload
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenInstant
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenPayload
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperation
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationAction
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationKey
import br.com.mwconsulting.championcompanion.core.model.HealthSyncProcessableQueue
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRepository
import br.com.mwconsulting.championcompanion.core.model.HealthSyncSummary
import br.com.mwconsulting.championcompanion.core.model.HealthSyncCutover
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.SyncOperationState
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityOrigin
import br.com.mwconsulting.championcompanion.core.model.WorkoutStepsCreditBlockReason
import br.com.mwconsulting.championcompanion.core.model.WorkoutStepsCreditState
import br.com.mwconsulting.championcompanion.core.model.WorkoutStepsProjectionContext
import br.com.mwconsulting.championcompanion.core.model.valueOrNull
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Extensão focal do ledger Health Connect para o cutover granular D1.
 *
 * RoomRepositories continua dono da fila/ledger histórico. Esta camada prepara
 * STEPS, DISTANCE e ACTIVE_CALORIES_BURNED D1 comprovados, preserva a cobertura
 * de WorkoutSteps e evita dupla publicação D1/D4 por métrica.
 */
@Singleton
class GranularHealthSyncRepository @Inject constructor(
    private val delegate: RoomRepositories,
    private val database: ChampionDatabase,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : HealthSyncRepository by delegate {
    private val ids = UuidV7Generator()
    private val frozenJson = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }

    override suspend fun reconcileBacklog(): RepositoryResult<HealthSyncBacklogReconciliationResult> {
        val base = when (val result = delegate.reconcileBacklog()) {
            is RepositoryResult.Success -> result.value
            RepositoryResult.Conflict -> return RepositoryResult.Conflict
            RepositoryResult.NotFound -> return RepositoryResult.NotFound
            RepositoryResult.StorageUnavailable -> return RepositoryResult.StorageUnavailable
            RepositoryResult.UnexpectedState -> return RepositoryResult.UnexpectedState
        }
        val prepared = when (val result = prepareGranularD1Records()) {
            is RepositoryResult.Success -> result.value
            RepositoryResult.Conflict -> return RepositoryResult.Conflict
            RepositoryResult.NotFound -> return RepositoryResult.NotFound
            RepositoryResult.StorageUnavailable -> return RepositoryResult.StorageUnavailable
            RepositoryResult.UnexpectedState -> return RepositoryResult.UnexpectedState
        }
        return RepositoryResult.Success(
            base.copy(ensuredOperationCount = base.ensuredOperationCount + prepared),
        )
    }

    override suspend fun findProcessable(limit: Int): RepositoryResult<List<HealthSyncOperation>> =
        when (val result = findProcessableWithIntegrity(limit)) {
            is RepositoryResult.Success -> RepositoryResult.Success(result.value.operations)
            RepositoryResult.Conflict -> RepositoryResult.Conflict
            RepositoryResult.NotFound -> RepositoryResult.NotFound
            RepositoryResult.StorageUnavailable -> RepositoryResult.StorageUnavailable
            RepositoryResult.UnexpectedState -> RepositoryResult.UnexpectedState
        }

    override suspend fun findProcessableWithIntegrity(
        limit: Int,
    ): RepositoryResult<HealthSyncProcessableQueue> {
        if (limit <= 0) return RepositoryResult.Success(HealthSyncProcessableQueue())
        when (prepareGranularD1Records()) {
            is RepositoryResult.Success -> Unit
            RepositoryResult.Conflict -> return RepositoryResult.Conflict
            RepositoryResult.NotFound -> return RepositoryResult.NotFound
            RepositoryResult.StorageUnavailable -> return RepositoryResult.StorageUnavailable
            RepositoryResult.UnexpectedState -> return RepositoryResult.UnexpectedState
        }
        val base = when (val result = delegate.findProcessableWithIntegrity(Int.MAX_VALUE)) {
            is RepositoryResult.Success -> result.value
            RepositoryResult.Conflict -> return RepositoryResult.Conflict
            RepositoryResult.NotFound -> return RepositoryResult.NotFound
            RepositoryResult.StorageUnavailable -> return RepositoryResult.StorageUnavailable
            RepositoryResult.UnexpectedState -> return RepositoryResult.UnexpectedState
        }
        val granular = when (val result = findGranularD1Processable()) {
            is RepositoryResult.Success -> result.value
            RepositoryResult.Conflict -> return RepositoryResult.Conflict
            RepositoryResult.NotFound -> return RepositoryResult.NotFound
            RepositoryResult.StorageUnavailable -> return RepositoryResult.StorageUnavailable
            RepositoryResult.UnexpectedState -> return RepositoryResult.UnexpectedState
        }
        val operations = (base.operations + granular)
            .distinctBy { it.id }
            .sortedWith(
                compareBy<HealthSyncOperation> { it.updatedAt ?: it.createdAt ?: Instant.EPOCH }
                    .thenBy { it.id.value.toString() },
            )
            .take(limit)
        return RepositoryResult.Success(
            HealthSyncProcessableQueue(
                operations = operations,
                blockedAggregates = base.blockedAggregates,
            ),
        )
    }

    override fun observeSyncSummary(): Flow<HealthSyncSummary> =
        database.healthSyncOperationDao().observeAll()
            .map { entities ->
                val workouts = database.workoutActivityDao().observeAll().first()
                val locallyResolvedCoveredIds = buildSet {
                    for (entity in entities) {
                        if (!entity.isWorkoutStepsInsert()) continue
                        val credit = database.workoutStepsCreditDao()
                            .findByWorkoutStepsOperationId(entity.id)
                            ?.toDomain()
                            ?: continue
                        if (credit.state == WorkoutStepsCreditState.BLOCKED &&
                            credit.blockedReason == WorkoutStepsCreditBlockReason.ALREADY_COVERED
                        ) {
                            add(entity.id)
                        }
                    }
                }
                val locallySuppressedD1MetricIds = buildSet {
                    for (entity in entities) {
                        val recordType = entity.d1OverlapSensitiveRecordTypeOrNull() ?: continue
                        if (entity.state !in setOf(
                                SyncOperationState.PENDING.name,
                                SyncOperationState.FAILED.name,
                            )
                        ) continue
                        val sample = database.watchDailyD1SampleDao().findById(entity.aggregateId) ?: continue
                        if (sample.overlapsPositiveD4Metric(workouts, recordType)) add(entity.id)
                    }
                }
                val locallyResolvedIds = locallyResolvedCoveredIds + locallySuppressedD1MetricIds
                HealthSyncSummary(
                    pendingCount = entities.count {
                        it.state == SyncOperationState.PENDING.name && it.id !in locallyResolvedIds
                    },
                    syncingCount = entities.count {
                        it.state == SyncOperationState.SYNCING.name && it.id !in locallyResolvedIds
                    },
                    failedCount = entities.count {
                        it.state == SyncOperationState.FAILED.name && it.id !in locallyResolvedIds
                    },
                    syncedCount = entities.count { it.state == SyncOperationState.SYNCED.name },
                    lastSyncedAt = entities.mapNotNull { it.syncedAtEpochMillis }.maxOrNull()?.let(Instant::ofEpochMilli),
                )
            }
            .flowOn(ioDispatcher)

    override suspend fun prepareProjectionChild(
        key: HealthSyncOperationKey,
        action: HealthSyncOperationAction,
        frozenPayload: HealthSyncFrozenPayload,
        clientRecordVersion: Long,
        projectionOrigin: HealthProjectionOrigin,
        workoutStepsContext: WorkoutStepsProjectionContext?,
    ): RepositoryResult<HealthSyncOperation> {
        if (action == HealthSyncOperationAction.INSERT && hasSyncedD1MetricOverlap(key)) {
            return RepositoryResult.Conflict
        }
        val result = delegate.prepareProjectionChild(
            key = key,
            action = action,
            frozenPayload = frozenPayload,
            clientRecordVersion = clientRecordVersion,
            projectionOrigin = projectionOrigin,
            workoutStepsContext = workoutStepsContext,
        )
        if (result !is RepositoryResult.Success ||
            action != HealthSyncOperationAction.INSERT ||
            key.projectionChildKey != HealthProjectionChildKey.WorkoutSteps ||
            workoutStepsContext == null
        ) {
            return result
        }
        return when (reserveWorkoutStepsForGranularDailyLine(result.value, workoutStepsContext)) {
            is RepositoryResult.Success -> result
            RepositoryResult.Conflict -> RepositoryResult.Conflict
            RepositoryResult.NotFound -> RepositoryResult.NotFound
            RepositoryResult.StorageUnavailable -> RepositoryResult.StorageUnavailable
            RepositoryResult.UnexpectedState -> RepositoryResult.UnexpectedState
        }
    }

    private suspend fun prepareGranularD1Records(): RepositoryResult<Int> = withContext(ioDispatcher) {
        val samplesSnapshot = database.watchDailyD1SampleDao().observeAll().first()
        val workoutsSnapshot = database.workoutActivityDao().observeAll().first()
        try {
            database.withWriteTransaction {
                var created = 0
                for ((naturalDay, daySamples) in samplesSnapshot.groupBy { it.deviceId to it.resolvedLocalDate }) {
                    val (deviceId, localDateIso) = naturalDay
                    val localDate = runCatching { LocalDate.parse(localDateIso) }.getOrNull()
                        ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                    val cutoverEntity = database.healthSyncCutoverDao().findByDeviceId(deviceId) ?: continue
                    val requestedCutover = runCatching { LocalDate.parse(cutoverEntity.localDateIso) }.getOrNull()
                        ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                    val effectiveCutover = maxOf(requestedCutover, HealthSyncCutover.GRANULAR_STEPS_ACTIVATION_DATE)
                    if (localDate < effectiveCutover) continue
                    if (!daySamples.isStructurallyCompleteD1Day()) continue

                    val summary = database.dailySummaryDao().findByNaturalKey(
                        deviceId = deviceId,
                        origin = DataOrigin.WATCH_DAILY_SUMMARY.name,
                        localDateIso = localDateIso,
                    ) ?: continue
                    if (summary.validity != DataValidity.VALID.name) continue
                    val eligibleDaySamples = daySamples.filter { it.bucketBelongsToResolvedDate() }
                    val d1Steps = eligibleDaySamples.sumStepsOrNull()
                        ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                    if (d1Steps > summary.steps) continue

                    val dailyStepOperations = database.healthSyncOperationDao()
                        .findByAggregate(summary.id, HealthSyncAggregateType.DAILY_SUMMARY.name)
                        .filter { it.recordType == HealthSyncRecordType.STEPS.name }
                    val existingGranularSteps = daySamples.mapNotNull { sample ->
                        database.healthSyncOperationDao().findByNaturalKey(
                            aggregateId = sample.id,
                            aggregateType = HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name,
                            recordType = HealthSyncRecordType.STEPS.name,
                        )
                    }
                    if (dailyStepOperations.isNotEmpty() && existingGranularSteps.isNotEmpty()) {
                        return@withWriteTransaction RepositoryResult.UnexpectedState
                    }

                    for (sample in eligibleDaySamples.sortedBy { it.resolvedAtEpochMillis }) {
                        if (dailyStepOperations.isEmpty() && sample.steps > 0L) {
                            val payload = sample.toFrozenD1StepsPayload()
                                ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                            created += ensureD1StepsOperation(sample, payload)
                                ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                        }

                        for (recordType in D1_GRANULAR_METRIC_TYPES) {
                            if (!sample.hasPositiveMetric(recordType)) continue
                            if (sample.overlapsPositiveD4Metric(workoutsSnapshot, recordType)) continue
                            val payload = sample.toFrozenD1MetricPayload(recordType)
                                ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                            created += ensureD1MetricOperation(sample, recordType, payload)
                                ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                        }
                    }
                }
                RepositoryResult.Success(created)
            }
        } catch (_: IllegalArgumentException) {
            RepositoryResult.UnexpectedState
        } catch (_: IllegalStateException) {
            RepositoryResult.UnexpectedState
        }
    }

    private suspend fun ensureD1StepsOperation(
        sample: WatchDailyD1SampleEntity,
        payload: HealthSyncFrozenDailyD1Payload,
    ): Int? {
        val existing = database.healthSyncOperationDao().findByNaturalKey(
            aggregateId = sample.id,
            aggregateType = HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name,
            recordType = HealthSyncRecordType.STEPS.name,
        )
        if (existing != null) return if (existing.matchesFrozenD1StepsSample(sample, payload)) 0 else null
        database.healthSyncOperationDao().insert(
            newD1Operation(
                sample = sample,
                recordType = HealthSyncRecordType.STEPS,
                frozenPayload = frozenJson.encodeToString(payload),
                frozenPayloadVersion = HEALTH_SYNC_FROZEN_DAILY_D1_PAYLOAD_SCHEMA_VERSION,
            ),
        )
        return 1
    }

    private suspend fun ensureD1MetricOperation(
        sample: WatchDailyD1SampleEntity,
        recordType: HealthSyncRecordType,
        payload: HealthSyncFrozenDailyD1MetricPayload,
    ): Int? {
        val existing = database.healthSyncOperationDao().findByNaturalKey(
            aggregateId = sample.id,
            aggregateType = HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name,
            recordType = recordType.name,
        )
        if (existing != null) return if (existing.matchesFrozenD1MetricSample(sample, recordType, payload)) 0 else null
        database.healthSyncOperationDao().insert(
            newD1Operation(
                sample = sample,
                recordType = recordType,
                frozenPayload = frozenJson.encodeToString(payload),
                frozenPayloadVersion = HEALTH_SYNC_FROZEN_DAILY_D1_METRIC_PAYLOAD_SCHEMA_VERSION,
            ),
        )
        return 1
    }

    private fun newD1Operation(
        sample: WatchDailyD1SampleEntity,
        recordType: HealthSyncRecordType,
        frozenPayload: String,
        frozenPayloadVersion: Int,
    ): HealthSyncOperationEntity = HealthSyncOperationEntity(
        id = ids.next().toString(),
        aggregateId = sample.id,
        aggregateType = HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name,
        recordType = recordType.name,
        state = SyncOperationState.PENDING.name,
        attemptCount = 0,
        lastAttemptAtEpochMillis = null,
        lastError = null,
        clientRecordId = stableD1ClientRecordId(sample.id, recordType),
        clientRecordVersion = sample.resolvedAtEpochMillis,
        syncedAtEpochMillis = null,
        timeZoneId = sample.zoneId,
        createdAtEpochMillis = sample.resolvedAtEpochMillis,
        updatedAtEpochMillis = sample.resolvedAtEpochMillis,
        frozenPayload = frozenPayload,
        frozenPayloadVersion = frozenPayloadVersion,
    )

    private suspend fun findGranularD1Processable(): RepositoryResult<List<HealthSyncOperation>> = withContext(ioDispatcher) {
        val workouts = database.workoutActivityDao().observeAll().first()
        val entities = database.healthSyncOperationDao().observeAll().first()
            .filter { entity ->
                entity.aggregateType == HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name &&
                    entity.recordType in D1_GRANULAR_RECORD_TYPE_NAMES &&
                    entity.state in setOf(SyncOperationState.PENDING.name, SyncOperationState.FAILED.name)
            }
            .filter { entity ->
                val recordType = entity.d1OverlapSensitiveRecordTypeOrNull() ?: return@filter true
                val sample = database.watchDailyD1SampleDao().findById(entity.aggregateId)
                    ?: return@withContext RepositoryResult.UnexpectedState
                !sample.overlapsPositiveD4Metric(workouts, recordType)
            }
        val operations = entities.map { entity ->
            entity.toDomainStrict().valueOrNull() ?: return@withContext RepositoryResult.UnexpectedState
        }
        RepositoryResult.Success(operations)
    }

    private suspend fun hasSyncedD1MetricOverlap(key: HealthSyncOperationKey): Boolean {
        val d1RecordType = when (key.projectionChildKey) {
            is HealthProjectionChildKey.DistanceInterval,
            HealthProjectionChildKey.ManualDistance,
            -> HealthSyncRecordType.DISTANCE
            HealthProjectionChildKey.TotalCaloriesBurned -> HealthSyncRecordType.ACTIVE_CALORIES_BURNED
            else -> return false
        }
        val aggregate = key.aggregate as? HealthSyncAggregate.WorkoutActivity ?: return false
        val activity = database.workoutActivityDao().findById(aggregate.activityId.value.toString()) ?: return false
        val deviceId = activity.deviceId ?: return false
        val operations = database.healthSyncOperationDao().observeAll().first()
            .filter {
                it.aggregateType == HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name &&
                    it.recordType == d1RecordType.name &&
                    it.state == SyncOperationState.SYNCED.name
            }
        for (operation in operations) {
            val sample = database.watchDailyD1SampleDao().findById(operation.aggregateId) ?: continue
            if (sample.deviceId != deviceId) continue
            if (sample.bucketOverlaps(activity.startedAtEpochMillis, activity.endedAtEpochMillis)) return true
        }
        return false
    }

    private suspend fun reserveWorkoutStepsForGranularDailyLine(
        operation: HealthSyncOperation,
        context: WorkoutStepsProjectionContext,
    ): RepositoryResult<Unit> = withContext(ioDispatcher) {
        val d1Snapshot = database.watchDailyD1SampleDao().observeAll().first()
        try {
            database.withWriteTransaction {
                val aggregate = operation.key.aggregate as? HealthSyncAggregate.WorkoutActivity
                    ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                val activity = database.workoutActivityDao().findById(aggregate.activityId.value.toString())
                    ?: return@withWriteTransaction RepositoryResult.NotFound
                val deviceId = activity.deviceId ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                val cutoverEntity = database.healthSyncCutoverDao().findByDeviceId(deviceId)
                    ?: return@withWriteTransaction RepositoryResult.Success(Unit)
                val requestedCutover = runCatching { LocalDate.parse(cutoverEntity.localDateIso) }.getOrNull()
                    ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                val effectiveCutover = maxOf(requestedCutover, HealthSyncCutover.GRANULAR_STEPS_ACTIVATION_DATE)
                val zoneId = runCatching { ZoneId.of(cutoverEntity.zoneId) }.getOrNull()
                    ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                val localDate = context.window.start.atZone(zoneId).toLocalDate()
                if (localDate < effectiveCutover) return@withWriteTransaction RepositoryResult.Success(Unit)

                val summary = database.dailySummaryDao().findByNaturalKey(
                    deviceId = deviceId,
                    origin = DataOrigin.WATCH_DAILY_SUMMARY.name,
                    localDateIso = localDate.toString(),
                )
                if (summary != null) {
                    val legacyDailySteps = database.healthSyncOperationDao()
                        .findByAggregate(summary.id, HealthSyncAggregateType.DAILY_SUMMARY.name)
                        .any { it.recordType == HealthSyncRecordType.STEPS.name }
                    if (legacyDailySteps) return@withWriteTransaction RepositoryResult.Success(Unit)
                }

                if (!hasPreparedGranularCoverage(
                        samplesSnapshot = d1Snapshot,
                        deviceId = deviceId,
                        localDate = localDate,
                        requiredThrough = context.window.end,
                        summarySteps = summary?.steps,
                    )
                ) {
                    return@withWriteTransaction RepositoryResult.UnexpectedState
                }

                val creditEntity = database.workoutStepsCreditDao()
                    .findByWorkoutStepsOperationId(operation.id.value.toString())
                    ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                val credit = creditEntity.toDomain() ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                if (credit.state == WorkoutStepsCreditState.BLOCKED &&
                    credit.blockedReason == WorkoutStepsCreditBlockReason.ALREADY_COVERED
                ) {
                    return@withWriteTransaction RepositoryResult.Success(Unit)
                }
                if (credit.state != WorkoutStepsCreditState.PENDING) {
                    return@withWriteTransaction RepositoryResult.UnexpectedState
                }
                val at = Instant.now()
                val blocked = credit.block(WorkoutStepsCreditBlockReason.ALREADY_COVERED, at).valueOrNull()
                    ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                if (database.workoutStepsCreditDao().update(blocked.toEntity()) != 1) {
                    throw IllegalStateException("WorkoutSteps credit update failed")
                }
                val operationEntity = database.healthSyncOperationDao().findById(operation.id.value.toString())
                    ?: throw IllegalStateException("WorkoutSteps operation disappeared")
                val touched = operationEntity.copy(
                    updatedAtEpochMillis = maxOf(operationEntity.updatedAtEpochMillis ?: Long.MIN_VALUE, at.toEpochMilli()),
                )
                if (database.healthSyncOperationDao().update(touched) != 1) {
                    throw IllegalStateException("WorkoutSteps operation touch failed")
                }
                RepositoryResult.Success(Unit)
            }
        } catch (_: IllegalArgumentException) {
            RepositoryResult.UnexpectedState
        } catch (_: IllegalStateException) {
            RepositoryResult.UnexpectedState
        }
    }

    private suspend fun hasPreparedGranularCoverage(
        samplesSnapshot: List<WatchDailyD1SampleEntity>,
        deviceId: String,
        localDate: LocalDate,
        requiredThrough: Instant,
        summarySteps: Long?,
    ): Boolean {
        val daySamples = samplesSnapshot.filter {
            it.deviceId == deviceId && it.resolvedLocalDate == localDate.toString()
        }
        if (!daySamples.isStructurallyCompleteD1Day()) return false
        val eligible = daySamples.filter { it.bucketBelongsToResolvedDate() }
        val total = eligible.sumStepsOrNull() ?: return false
        if (summarySteps == null || total > summarySteps) return false
        val latestBucketEnd = eligible.maxOfOrNull { it.resolvedAtEpochMillis }
            ?.let(Instant::ofEpochMilli) ?: return false
        if (latestBucketEnd.isBefore(requiredThrough)) return false
        val positive = eligible.filter { it.steps > 0L }
        if (positive.isEmpty()) return false
        return positive.all { sample ->
            database.healthSyncOperationDao().findByNaturalKey(
                aggregateId = sample.id,
                aggregateType = HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name,
                recordType = HealthSyncRecordType.STEPS.name,
            ) != null
        }
    }

    private fun List<WatchDailyD1SampleEntity>.isStructurallyCompleteD1Day(): Boolean {
        if (isEmpty()) return false
        val expected = map { it.totalPositions }.distinct().singleOrNull() ?: return false
        if (expected <= 0 || size != expected) return false
        val positions = map { it.currentPosition }.sorted()
        return positions == (1..expected).toList()
    }

    private fun List<WatchDailyD1SampleEntity>.sumStepsOrNull(): Long? {
        var total = 0L
        for (sample in this) {
            if (sample.steps < 0L) return null
            total = try {
                Math.addExact(total, sample.steps)
            } catch (_: ArithmeticException) {
                return null
            }
        }
        return total
    }

    private fun WatchDailyD1SampleEntity.bucketBelongsToResolvedDate(): Boolean {
        val resolvedDate = runCatching { LocalDate.parse(resolvedLocalDate) }.getOrNull() ?: return false
        val end = runCatching { Instant.ofEpochMilli(resolvedAtEpochMillis) }.getOrNull() ?: return false
        val start = runCatching { end.minus(D1_BUCKET_DURATION) }.getOrNull() ?: return false
        val endInclusive = runCatching { end.minusNanos(1L) }.getOrNull() ?: return false
        val zone = runCatching { ZoneId.of(zoneId) }.getOrNull() ?: return false
        return start.atZone(zone).toLocalDate() == resolvedDate &&
            endInclusive.atZone(zone).toLocalDate() == resolvedDate
    }

    private fun WatchDailyD1SampleEntity.bucketOverlaps(
        windowStartEpochMillis: Long,
        windowEndEpochMillis: Long,
    ): Boolean {
        val end = resolvedAtEpochMillis
        val start = runCatching {
            Instant.ofEpochMilli(end).minus(D1_BUCKET_DURATION).toEpochMilli()
        }.getOrNull() ?: return false
        return windowStartEpochMillis < end && windowEndEpochMillis > start
    }

    private fun WatchDailyD1SampleEntity.overlapsPositiveD4Metric(
        workouts: List<WorkoutActivityEntity>,
        recordType: HealthSyncRecordType,
    ): Boolean = workouts.any { workout ->
        workout.deviceId == deviceId &&
            workout.origin == WorkoutActivityOrigin.WATCH_D3_D4.name &&
            workout.workflowState != ActivityWorkflowState.DISCARDED.name &&
            bucketOverlaps(workout.startedAtEpochMillis, workout.endedAtEpochMillis) &&
            workout.hasPositiveMetric(recordType)
    }

    private fun WorkoutActivityEntity.hasPositiveMetric(recordType: HealthSyncRecordType): Boolean = when (recordType) {
        HealthSyncRecordType.DISTANCE -> listOf(
            manualDistanceMeters,
            calibratedDistanceMeters,
            originalDistanceMeters,
            estimatedDistanceMeters,
        ).any { it?.let { value -> value.isFinite() && value > 0.0 } == true }
        HealthSyncRecordType.ACTIVE_CALORIES_BURNED ->
            caloriesKilocalories?.let { it.isFinite() && it > 0.0 } == true
        else -> false
    }

    private fun WatchDailyD1SampleEntity.hasPositiveMetric(recordType: HealthSyncRecordType): Boolean = when (recordType) {
        HealthSyncRecordType.DISTANCE -> distanceMeters.isFinite() && distanceMeters > 0.0
        HealthSyncRecordType.ACTIVE_CALORIES_BURNED ->
            caloriesKilocalories.isFinite() && caloriesKilocalories > 0.0
        else -> false
    }

    private fun WatchDailyD1SampleEntity.toFrozenD1StepsPayload(): HealthSyncFrozenDailyD1Payload? {
        if (!bucketBelongsToResolvedDate()) return null
        val end = runCatching { Instant.ofEpochMilli(resolvedAtEpochMillis) }.getOrNull() ?: return null
        val start = runCatching { end.minus(D1_BUCKET_DURATION) }.getOrNull() ?: return null
        val zone = runCatching { ZoneId.of(zoneId) }.getOrNull() ?: return null
        if (steps <= 0L) return null
        return runCatching {
            HealthSyncFrozenDailyD1Payload(
                recordType = HealthSyncRecordType.STEPS.name,
                sampleId = id,
                start = start.toFrozenInstant(),
                end = end.toFrozenInstant(),
                zoneId = zone.id,
                startZoneOffsetSeconds = zone.rules.getOffset(start).totalSeconds,
                endZoneOffsetSeconds = zone.rules.getOffset(end).totalSeconds,
                steps = steps,
            )
        }.getOrNull()
    }

    private fun WatchDailyD1SampleEntity.toFrozenD1MetricPayload(
        recordType: HealthSyncRecordType,
    ): HealthSyncFrozenDailyD1MetricPayload? {
        if (!bucketBelongsToResolvedDate() || !hasPositiveMetric(recordType)) return null
        val end = runCatching { Instant.ofEpochMilli(resolvedAtEpochMillis) }.getOrNull() ?: return null
        val start = runCatching { end.minus(D1_BUCKET_DURATION) }.getOrNull() ?: return null
        val zone = runCatching { ZoneId.of(zoneId) }.getOrNull() ?: return null
        return runCatching {
            HealthSyncFrozenDailyD1MetricPayload(
                recordType = recordType.name,
                sampleId = id,
                start = start.toFrozenInstant(),
                end = end.toFrozenInstant(),
                zoneId = zone.id,
                startZoneOffsetSeconds = zone.rules.getOffset(start).totalSeconds,
                endZoneOffsetSeconds = zone.rules.getOffset(end).totalSeconds,
                distanceMeters = distanceMeters.takeIf { recordType == HealthSyncRecordType.DISTANCE },
                activeCaloriesKilocalories = caloriesKilocalories.takeIf {
                    recordType == HealthSyncRecordType.ACTIVE_CALORIES_BURNED
                },
            )
        }.getOrNull()
    }

    private fun HealthSyncOperationEntity.matchesFrozenD1StepsSample(
        sample: WatchDailyD1SampleEntity,
        expectedPayload: HealthSyncFrozenDailyD1Payload,
    ): Boolean {
        if (aggregateId != sample.id ||
            aggregateType != HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name ||
            recordType != HealthSyncRecordType.STEPS.name ||
            action != HealthSyncOperationAction.INSERT.name ||
            clientRecordId != stableD1ClientRecordId(sample.id, HealthSyncRecordType.STEPS) ||
            clientRecordVersion != sample.resolvedAtEpochMillis ||
            timeZoneId != sample.zoneId ||
            frozenPayloadVersion != HEALTH_SYNC_FROZEN_DAILY_D1_PAYLOAD_SCHEMA_VERSION
        ) return false
        val payload = frozenPayload?.let {
            runCatching { frozenJson.decodeFromString<HealthSyncFrozenDailyD1Payload>(it) }.getOrNull()
        } ?: return false
        return payload == expectedPayload
    }

    private fun HealthSyncOperationEntity.matchesFrozenD1MetricSample(
        sample: WatchDailyD1SampleEntity,
        expectedRecordType: HealthSyncRecordType,
        expectedPayload: HealthSyncFrozenDailyD1MetricPayload,
    ): Boolean {
        if (aggregateId != sample.id ||
            aggregateType != HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name ||
            recordType != expectedRecordType.name ||
            action != HealthSyncOperationAction.INSERT.name ||
            clientRecordId != stableD1ClientRecordId(sample.id, expectedRecordType) ||
            clientRecordVersion != sample.resolvedAtEpochMillis ||
            timeZoneId != sample.zoneId ||
            frozenPayloadVersion != HEALTH_SYNC_FROZEN_DAILY_D1_METRIC_PAYLOAD_SCHEMA_VERSION
        ) return false
        val payload = frozenPayload?.let {
            runCatching { frozenJson.decodeFromString<HealthSyncFrozenDailyD1MetricPayload>(it) }.getOrNull()
        } ?: return false
        return payload == expectedPayload
    }

    private fun HealthSyncOperationEntity.d1OverlapSensitiveRecordTypeOrNull(): HealthSyncRecordType? {
        if (aggregateType != HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name) return null
        return when (recordType) {
            HealthSyncRecordType.DISTANCE.name -> HealthSyncRecordType.DISTANCE
            HealthSyncRecordType.ACTIVE_CALORIES_BURNED.name -> HealthSyncRecordType.ACTIVE_CALORIES_BURNED
            else -> null
        }
    }

    private fun stableD1ClientRecordId(sampleId: String, recordType: HealthSyncRecordType): String =
        "${BuildConfig.APPLICATION_ID}:${HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name}:$sampleId:${recordType.name}"

    private fun Instant.toFrozenInstant(): HealthSyncFrozenInstant = HealthSyncFrozenInstant(
        epochSecond = epochSecond,
        nano = nano,
    )

    private fun HealthSyncOperationEntity.isWorkoutStepsInsert(): Boolean =
        aggregateType == HealthSyncAggregateType.WORKOUT_ACTIVITY.name &&
            recordType == HealthSyncRecordType.STEPS.name &&
            projectionChildKey == HealthProjectionChildKey.WorkoutSteps.stableValue &&
            action == HealthSyncOperationAction.INSERT.name

    private companion object {
        val D1_BUCKET_DURATION: Duration = Duration.ofMinutes(5)
        val D1_GRANULAR_METRIC_TYPES = setOf(
            HealthSyncRecordType.DISTANCE,
            HealthSyncRecordType.ACTIVE_CALORIES_BURNED,
        )
        val D1_GRANULAR_RECORD_TYPE_NAMES = setOf(
            HealthSyncRecordType.STEPS.name,
            HealthSyncRecordType.DISTANCE.name,
            HealthSyncRecordType.ACTIVE_CALORIES_BURNED.name,
        )
    }
}
