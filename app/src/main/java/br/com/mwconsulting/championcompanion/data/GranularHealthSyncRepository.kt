package br.com.mwconsulting.championcompanion.data

import androidx.room3.withWriteTransaction
import br.com.mwconsulting.championcompanion.BuildConfig
import br.com.mwconsulting.championcompanion.core.common.IoDispatcher
import br.com.mwconsulting.championcompanion.core.database.ChampionDatabase
import br.com.mwconsulting.championcompanion.core.database.HealthSyncOperationEntity
import br.com.mwconsulting.championcompanion.core.database.WatchDailyD1SampleEntity
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.DataValidity
import br.com.mwconsulting.championcompanion.core.model.HEALTH_SYNC_FROZEN_DAILY_D1_PAYLOAD_SCHEMA_VERSION
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionChildKey
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionOrigin
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregate
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregateType
import br.com.mwconsulting.championcompanion.core.model.HealthSyncBacklogReconciliationResult
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
 * somente STEPS D1 comprovados, reserva WorkoutSteps para a linha diária granular
 * pós-cutover e remove apenas bloqueios terminalmente cobertos da contagem
 * acionável.
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
        val prepared = when (val result = prepareGranularD1Steps()) {
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
        when (prepareGranularD1Steps()) {
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
                HealthSyncSummary(
                    pendingCount = entities.count {
                        it.state == SyncOperationState.PENDING.name && it.id !in locallyResolvedCoveredIds
                    },
                    syncingCount = entities.count {
                        it.state == SyncOperationState.SYNCING.name && it.id !in locallyResolvedCoveredIds
                    },
                    failedCount = entities.count {
                        it.state == SyncOperationState.FAILED.name && it.id !in locallyResolvedCoveredIds
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

    private suspend fun prepareGranularD1Steps(): RepositoryResult<Int> = withContext(ioDispatcher) {
        try {
            database.withWriteTransaction {
                val samples = database.watchDailyD1SampleDao().observeAll().first()
                var created = 0
                for ((naturalDay, daySamples) in samples.groupBy { it.deviceId to it.resolvedLocalDate }) {
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
                    val d1Steps = daySamples.sumStepsOrNull()
                        ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                    if (d1Steps > summary.steps) continue

                    val dailyStepOperations = database.healthSyncOperationDao()
                        .findByAggregate(summary.id, HealthSyncAggregateType.DAILY_SUMMARY.name)
                        .filter { it.recordType == HealthSyncRecordType.STEPS.name }
                    val existingGranular = daySamples.mapNotNull { sample ->
                        database.healthSyncOperationDao().findByNaturalKey(
                            aggregateId = sample.id,
                            aggregateType = HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name,
                            recordType = HealthSyncRecordType.STEPS.name,
                        )
                    }
                    if (dailyStepOperations.isNotEmpty()) {
                        if (existingGranular.isNotEmpty()) {
                            return@withWriteTransaction RepositoryResult.UnexpectedState
                        }
                        continue
                    }

                    for (sample in daySamples.sortedBy { it.resolvedAtEpochMillis }) {
                        if (sample.steps <= 0L) continue
                        val payload = sample.toFrozenD1StepsPayload()
                            ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                        val existing = database.healthSyncOperationDao().findByNaturalKey(
                            aggregateId = sample.id,
                            aggregateType = HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name,
                            recordType = HealthSyncRecordType.STEPS.name,
                        )
                        if (existing != null) {
                            if (!existing.matchesFrozenD1Sample(sample, payload)) {
                                return@withWriteTransaction RepositoryResult.UnexpectedState
                            }
                            continue
                        }
                        database.healthSyncOperationDao().insert(
                            HealthSyncOperationEntity(
                                id = ids.next().toString(),
                                aggregateId = sample.id,
                                aggregateType = HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name,
                                recordType = HealthSyncRecordType.STEPS.name,
                                state = SyncOperationState.PENDING.name,
                                attemptCount = 0,
                                lastAttemptAtEpochMillis = null,
                                lastError = null,
                                clientRecordId = stableD1ClientRecordId(sample.id),
                                clientRecordVersion = sample.resolvedAtEpochMillis,
                                syncedAtEpochMillis = null,
                                timeZoneId = sample.zoneId,
                                createdAtEpochMillis = sample.resolvedAtEpochMillis,
                                updatedAtEpochMillis = sample.resolvedAtEpochMillis,
                                frozenPayload = frozenJson.encodeToString(payload),
                                frozenPayloadVersion = HEALTH_SYNC_FROZEN_DAILY_D1_PAYLOAD_SCHEMA_VERSION,
                            ),
                        )
                        created += 1
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

    private suspend fun findGranularD1Processable(): RepositoryResult<List<HealthSyncOperation>> = withContext(ioDispatcher) {
        val entities = database.healthSyncOperationDao().observeAll().first()
            .filter { entity ->
                entity.aggregateType == HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name &&
                    entity.recordType == HealthSyncRecordType.STEPS.name &&
                    entity.state in setOf(SyncOperationState.PENDING.name, SyncOperationState.FAILED.name)
            }
        val operations = entities.map { entity ->
            entity.toDomainStrict().valueOrNull() ?: return@withContext RepositoryResult.UnexpectedState
        }
        RepositoryResult.Success(operations)
    }

    private suspend fun reserveWorkoutStepsForGranularDailyLine(
        operation: HealthSyncOperation,
        context: WorkoutStepsProjectionContext,
    ): RepositoryResult<Unit> = withContext(ioDispatcher) {
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
                val blocked = credit.block(WorkoutStepsCreditBlockReason.ALREADY_COVERED, Instant.now()).valueOrNull()
                    ?: return@withWriteTransaction RepositoryResult.UnexpectedState
                if (database.workoutStepsCreditDao().update(blocked.toEntity()) != 1) {
                    return@withWriteTransaction RepositoryResult.Conflict
                }
                RepositoryResult.Success(Unit)
            }
        } catch (_: IllegalArgumentException) {
            RepositoryResult.UnexpectedState
        } catch (_: IllegalStateException) {
            RepositoryResult.UnexpectedState
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

    private fun WatchDailyD1SampleEntity.toFrozenD1StepsPayload(): HealthSyncFrozenDailyD1Payload? {
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

    private fun HealthSyncOperationEntity.matchesFrozenD1Sample(
        sample: WatchDailyD1SampleEntity,
        expectedPayload: HealthSyncFrozenDailyD1Payload,
    ): Boolean {
        if (aggregateId != sample.id ||
            aggregateType != HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name ||
            recordType != HealthSyncRecordType.STEPS.name ||
            action != HealthSyncOperationAction.INSERT.name ||
            clientRecordId != stableD1ClientRecordId(sample.id) ||
            clientRecordVersion != sample.resolvedAtEpochMillis ||
            timeZoneId != sample.zoneId ||
            frozenPayloadVersion != HEALTH_SYNC_FROZEN_DAILY_D1_PAYLOAD_SCHEMA_VERSION
        ) return false
        val payload = frozenPayload?.let {
            runCatching { frozenJson.decodeFromString<HealthSyncFrozenDailyD1Payload>(it) }.getOrNull()
        } ?: return false
        return payload == expectedPayload
    }

    private fun stableD1ClientRecordId(sampleId: String): String =
        "${BuildConfig.APPLICATION_ID}:${HealthSyncAggregateType.WATCH_DAILY_D1_SAMPLE.name}:$sampleId:${HealthSyncRecordType.STEPS.name}"

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
    }
}
