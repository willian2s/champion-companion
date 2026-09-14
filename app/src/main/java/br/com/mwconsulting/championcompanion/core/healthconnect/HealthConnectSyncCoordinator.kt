package br.com.mwconsulting.championcompanion.core.healthconnect

import br.com.mwconsulting.championcompanion.core.common.IoDispatcher
import br.com.mwconsulting.championcompanion.core.model.AppPreferencesRepository
import br.com.mwconsulting.championcompanion.core.model.ActivityWorkflowState
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregate
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperation
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionChildKey
import br.com.mwconsulting.championcompanion.core.model.HealthLegacyDistanceProjectionStatus
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionManifest
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionOrigin
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationId
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationAction
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationKey
import br.com.mwconsulting.championcompanion.core.model.HealthSyncProcessableQueue
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRepository
import br.com.mwconsulting.championcompanion.core.model.HeartRateRepository
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsRepository
import br.com.mwconsulting.championcompanion.core.model.ParserValidity
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.SafeSyncError
import br.com.mwconsulting.championcompanion.core.model.SyncOperationState
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityOrigin
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityFilter
import br.com.mwconsulting.championcompanion.core.model.WorkoutPlausibilityIndicator
import br.com.mwconsulting.championcompanion.core.model.WorkoutStepsCreditState
import br.com.mwconsulting.championcompanion.core.model.WorkoutStepsProjectionContext
import br.com.mwconsulting.championcompanion.core.model.WorkoutIntervalWindow
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.model.valueOrNull
import br.com.mwconsulting.championcompanion.core.model.WATCH_WORKOUT_D4_V4_PARSER_VERSION
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

const val DEFAULT_HEALTH_CONNECT_FOREGROUND_LIMIT: Int = 100

data class HealthConnectSyncReport(
    val startedAt: Instant,
    val finishedAt: Instant,
    val limit: Int,
    val resetInterruptedOperationCount: Int = 0,
    val resetInterruptedActivityCount: Int = 0,
    val reconciledOperationCount: Int = 0,
    val reconciledRepairCount: Int = 0,
    val processedCount: Int = 0,
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
    /** Escritas aceitas pelo provider, sem confirmação remota por leitura. */
    val writeAcceptedWithoutReadbackCount: Int = 0,
    val requiredPermissions: Set<HealthConnectWritePermission> = emptySet(),
)

data class HealthConnectSyncFailure(
    val operationId: HealthSyncOperationId,
    val recordType: HealthSyncRecordType,
    val error: SafeSyncError,
    val reason: String,
    val failureKind: HealthConnectFailureKind = error.toHealthConnectFailureKind(),
    val projectionChildKey: HealthProjectionChildKey? = null,
)

sealed interface HealthConnectSyncOutcome {
    val report: HealthConnectSyncReport

    data class Completed(
        override val report: HealthConnectSyncReport,
    ) : HealthConnectSyncOutcome

    data class CompletedWithFailures(
        override val report: HealthConnectSyncReport,
        val failures: List<HealthConnectSyncFailure>,
    ) : HealthConnectSyncOutcome

    data class NothingToSync(
        override val report: HealthConnectSyncReport,
    ) : HealthConnectSyncOutcome

    data class PermissionRequired(
        override val report: HealthConnectSyncReport,
        val missingPermissions: Set<HealthConnectWritePermission>,
    ) : HealthConnectSyncOutcome

    data class PlausibilityAcceptanceRequired(
        override val report: HealthConnectSyncReport,
    ) : HealthConnectSyncOutcome

    data class PlausibilityBlocked(
        override val report: HealthConnectSyncReport,
        val indicators: Set<WorkoutPlausibilityIndicator>,
    ) : HealthConnectSyncOutcome

    data class ProviderUnavailable(
        override val report: HealthConnectSyncReport,
        val updateRequired: Boolean,
    ) : HealthConnectSyncOutcome

    data class Disabled(
        override val report: HealthConnectSyncReport,
    ) : HealthConnectSyncOutcome

    data class Failed(
        override val report: HealthConnectSyncReport,
        val error: SafeSyncError,
        val reason: String,
    ) : HealthConnectSyncOutcome
}

interface HealthConnectSyncCoordinator {
    suspend fun syncPending(limit: Int = DEFAULT_HEALTH_CONNECT_FOREGROUND_LIMIT): HealthConnectSyncOutcome

    suspend fun syncActivity(
        activityId: WorkoutActivityId,
        limit: Int = DEFAULT_HEALTH_CONNECT_FOREGROUND_LIMIT,
    ): HealthConnectSyncOutcome
}

@Singleton
class DefaultHealthConnectSyncCoordinator @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val heartRateRepository: HeartRateRepository,
    private val workoutActivityRepository: WorkoutActivityRepository,
    private val personalMetricsRepository: PersonalMetricsRepository,
    private val healthSyncRepository: HealthSyncRepository,
    private val healthConnectGateway: HealthConnectGateway,
    private val healthRecordMapper: HealthRecordMapper,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : HealthConnectSyncCoordinator {
    private val mutex = Mutex()

    override suspend fun syncPending(limit: Int): HealthConnectSyncOutcome =
        runSerialized(limit) { startedAt, safeLimit ->
            val baseline = prepareForSync(startedAt = startedAt, limit = safeLimit)
            continueSync(
                baseline = baseline,
                explicitActivityId = null,
                processableOperations = { healthSyncRepository.findProcessableWithIntegrity(safeLimit) },
            )
        }

    override suspend fun syncActivity(
        activityId: WorkoutActivityId,
        limit: Int,
    ): HealthConnectSyncOutcome = runSerialized(limit) { startedAt, safeLimit ->
        val baseline = prepareForSync(startedAt = startedAt, limit = safeLimit)
        continueSync(
            baseline = baseline,
            explicitActivityId = activityId,
            processableOperations = { RepositoryResult.Success(HealthSyncProcessableQueue()) },
        )
    }

    private suspend fun runSerialized(
        limit: Int,
        block: suspend (startedAt: Instant, safeLimit: Int) -> HealthConnectSyncOutcome,
    ): HealthConnectSyncOutcome = mutex.withLock {
        withContext(ioDispatcher) {
            val startedAt = Instant.now()
            val safeLimit = limit.coerceAtLeast(1)
            block(startedAt, safeLimit)
        }
    }

    private suspend fun prepareForSync(
        startedAt: Instant,
        limit: Int,
    ): PreparedSync {
        val resetInterruptedOperationCount = healthSyncRepository.resetInterrupted(startedAt).successOrZero()
        val resetInterruptedActivityCount = workoutActivityRepository.resetInterruptedSyncing().successOrZero()
        val reconciledBacklog = healthSyncRepository.reconcileBacklog().successOrNull()
        return PreparedSync(
            report = HealthConnectSyncReport(
                startedAt = startedAt,
                finishedAt = startedAt,
                limit = limit,
                resetInterruptedOperationCount = resetInterruptedOperationCount,
                resetInterruptedActivityCount = resetInterruptedActivityCount,
                reconciledOperationCount = reconciledBacklog?.ensuredOperationCount ?: 0,
                reconciledRepairCount = reconciledBacklog?.repairedOperationCount ?: 0,
            ),
        )
    }

    private suspend fun continueSync(
        baseline: PreparedSync,
        explicitActivityId: WorkoutActivityId?,
        processableOperations: suspend () -> RepositoryResult<HealthSyncProcessableQueue>,
    ): HealthConnectSyncOutcome {
        val preferences = appPreferencesRepository.observe().first()
        if (!preferences.healthSyncEnabled) {
            return HealthConnectSyncOutcome.Disabled(baseline.report.finish())
        }

        val availability = runCatching { healthConnectGateway.getAvailability() }.getOrNull()
            ?: return HealthConnectSyncOutcome.ProviderUnavailable(
                report = baseline.report.finish(),
                updateRequired = false,
            )
        return when (availability) {
            HealthConnectAvailability.Unavailable -> HealthConnectSyncOutcome.ProviderUnavailable(
                report = baseline.report.finish(),
                updateRequired = false,
            )

            HealthConnectAvailability.ProviderUpdateRequired -> HealthConnectSyncOutcome.ProviderUnavailable(
                report = baseline.report.finish(),
                updateRequired = true,
            )

            HealthConnectAvailability.Available -> processAvailableSync(
                baseline = baseline,
                explicitActivityId = explicitActivityId,
                processableOperations = processableOperations,
            )
        }
    }

    private suspend fun processAvailableSync(
        baseline: PreparedSync,
        explicitActivityId: WorkoutActivityId?,
        processableOperations: suspend () -> RepositoryResult<HealthSyncProcessableQueue>,
    ): HealthConnectSyncOutcome {
        val grantedPermissions = when (val permissionResult = healthConnectGateway.getGrantedPermissions()) {
            is HealthConnectPermissionResult.Granted -> permissionResult.permissions
            is HealthConnectPermissionResult.Failed -> {
                return when (permissionResult.failure.kind) {
                    HealthConnectFailureKind.PROVIDER_UPDATE_REQUIRED -> HealthConnectSyncOutcome.ProviderUnavailable(
                        report = baseline.report.finish(),
                        updateRequired = true,
                    )

                    else -> HealthConnectSyncOutcome.ProviderUnavailable(
                        report = baseline.report.finish(),
                        updateRequired = false,
                    )
                }
            }
        }
        val blockedAggregates = linkedSetOf<HealthSyncAggregate>()
        val preparedOperations = when {
            explicitActivityId != null -> when (val prepared = prepareActivityOperations(explicitActivityId, grantedPermissions)) {
                is PreparedActivityOperations.Success -> prepared.operations
                is PreparedActivityOperations.PermissionRequired -> {
                    return HealthConnectSyncOutcome.PermissionRequired(
                        report = baseline.report.copy(
                            finishedAt = Instant.now(),
                            requiredPermissions = prepared.missingPermissions,
                        ),
                        missingPermissions = prepared.missingPermissions,
                    )
                }

                is PreparedActivityOperations.PlausibilityAcceptanceRequired -> {
                    return HealthConnectSyncOutcome.PlausibilityAcceptanceRequired(
                        report = baseline.report.finish(),
                    )
                }

                is PreparedActivityOperations.PlausibilityBlocked -> {
                    return HealthConnectSyncOutcome.PlausibilityBlocked(
                        report = baseline.report.finish(),
                        indicators = prepared.indicators,
                    )
                }

                is PreparedActivityOperations.Failed -> {
                    return HealthConnectSyncOutcome.Failed(
                        report = baseline.report.finish(),
                        error = prepared.error,
                        reason = prepared.reason,
                    )
                }
            }

            else -> {
                when (val prepared = preparePendingWatchActivityOperations(grantedPermissions)) {
                    is RepositoryResult.Success -> blockedAggregates += prepared.value
                    else -> return HealthConnectSyncOutcome.Failed(
                        report = baseline.report.finish(),
                        error = SafeSyncError.INTERNAL,
                        reason = "Não foi possível preparar reconciliações locais do relógio.",
                    )
                }
                when (val result = processableOperations()) {
                    is RepositoryResult.Success -> {
                        blockedAggregates += result.value.blockedAggregates
                        filterProcessableOperations(
                            operations = result.value.operations,
                            workoutActivityRepository = workoutActivityRepository,
                            healthRecordMapper = healthRecordMapper,
                            healthSyncRepository = healthSyncRepository,
                        )
                    }

                    else -> return HealthConnectSyncOutcome.Failed(
                        report = baseline.report.finish(),
                        error = SafeSyncError.INTERNAL,
                        reason = "Não foi possível carregar operações processáveis da fila.",
                    )
                }
            }
        }
        val pendingOperations = preparedOperations
            .filterNot { operation -> operation.key.aggregate in blockedAggregates }
            .filter(HealthSyncOperation::isPending)
        val operations = orderOperationsForRemoteEffects(pendingOperations)
        if (operations.isEmpty()) {
            if (explicitActivityId != null) {
                updateWorkoutWorkflowFromOperations(explicitActivityId, grantedPermissions)
            } else {
                repairCompletedWorkoutWorkflows(grantedPermissions, blockedAggregates)
            }
            return HealthConnectSyncOutcome.NothingToSync(baseline.report.finish())
        }

        val failures = mutableListOf<HealthConnectSyncFailure>()
        val missingPermissions = linkedSetOf<HealthConnectWritePermission>()
        val touchedActivities = linkedSetOf<WorkoutActivityId>()
        var syncedCount = 0
        var writeAcceptedWithoutReadbackCount = 0
        val legacyDistanceGateByActivity = mutableMapOf<WorkoutActivityId, HealthLegacyDistanceProjectionStatus>()

        for (operation in operations) {
            val now = Instant.now()
            if (operation.key.aggregate is HealthSyncAggregate.WorkoutActivity) {
                touchedActivities += operation.key.aggregate.activityId
            }
            if (operation.action == HealthSyncOperationAction.DELETE) {
                when (healthSyncRepository.validateDeleteIdentity(operation)) {
                    is RepositoryResult.Success -> Unit
                    else -> {
                        failures += operation.failure(
                            SafeSyncError.INVALID_LOCAL_DATA,
                            "Identidade determinística do DELETE não pôde ser validada localmente.",
                        )
                        continue
                    }
                }
            }
            val legacyBlock = operation.legacyRemoteEffectBlock(
                healthSyncRepository = healthSyncRepository,
                workoutActivityRepository = workoutActivityRepository,
                cache = legacyDistanceGateByActivity,
            )
            if (legacyBlock != null) {
                failures += operation.failure(legacyBlock.error, legacyBlock.reason)
                continue
            }
            when (val started = healthSyncRepository.markStartedIfCurrent(
                operationId = operation.id,
                expectedClientRecordVersion = operation.clientRecordVersion,
                at = now,
            )) {
                is RepositoryResult.Success -> Unit
                else -> {
                    failures += operation.failure(SafeSyncError.INTERNAL, "Não foi possível marcar operação como `SYNCING`.")
                    continue
                }
            }

            val mappedRecord = if (operation.action == HealthSyncOperationAction.DELETE) {
                null
            } else {
                when (val mapped = loadAndMap(operation)) {
                    is HealthRecordMappingResult.Failure -> {
                        healthSyncRepository.markFailed(operation.id, mapped.error, Instant.now())
                        failures += operation.failure(mapped.error, mapped.reason)
                        continue
                    }

                    is HealthRecordMappingResult.Success -> mapped.value
                }
            }
            val permission = mappedRecord?.permission ?: permissionFor(operation.key.recordType)
            if (permission !in grantedPermissions) {
                healthSyncRepository.markFailed(operation.id, SafeSyncError.PERMISSION_REQUIRED, Instant.now())
                missingPermissions += permission
                failures += operation.failure(
                    SafeSyncError.PERMISSION_REQUIRED,
                    "Permissão `${permission.rawValue}` não concedida para `${operation.key.recordType}`.",
                )
                continue
            }

            val remoteOutcome = try {
                if (operation.action == HealthSyncOperationAction.DELETE) {
                    healthConnectGateway.delete(
                        recordType = operation.key.recordType,
                        clientRecordId = operation.clientRecordId!!,
                    )
                } else {
                    healthConnectGateway.insert(listOf(mappedRecord!!.record))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                HealthConnectWriteResult.Failed(
                    HealthConnectGatewayFailure(
                        kind = HealthConnectFailureKind.INTERNAL,
                        reason = "Não foi possível concluir a escrita local no provider Health Connect.",
                    ),
                )
            }
            when (remoteOutcome) {
                HealthConnectWriteResult.Accepted -> {
                    writeAcceptedWithoutReadbackCount += 1
                    when (healthSyncRepository.markSynced(operation.id, Instant.now())) {
                        is RepositoryResult.Success -> {
                            syncedCount += 1
                            if (operation.isLegacyWorkoutDistanceDelete()) {
                                val aggregate = operation.key.aggregate as? HealthSyncAggregate.WorkoutActivity
                                if (aggregate != null) {
                                    legacyDistanceGateByActivity[aggregate.activityId] =
                                        HealthLegacyDistanceProjectionStatus.RECONCILED
                                }
                            }
                        }

                        else -> {
                            healthSyncRepository.markFailed(operation.id, SafeSyncError.INTERNAL, Instant.now())
                            failures += operation.failure(
                                SafeSyncError.INTERNAL,
                                "Record enviado, mas estado local não pôde ser marcado como `SYNCED`.",
                            )
                        }
                    }
                }

                is HealthConnectWriteResult.Failed -> {
                    val failure = remoteOutcome.failure
                    val safeError = failure.kind.toSafeSyncError()
                    healthSyncRepository.markFailed(operation.id, safeError, Instant.now())
                    failures += operation.failure(
                        safeError,
                        failure.reason,
                        failureKind = failure.kind,
                    )
                    if (failure.kind == HealthConnectFailureKind.PERMISSION_REQUIRED) {
                        missingPermissions += permission
                    }
                }
            }
        }

        touchedActivities.forEach { updateWorkoutWorkflowFromOperations(it, grantedPermissions) }
        if (explicitActivityId != null && explicitActivityId !in touchedActivities) {
            updateWorkoutWorkflowFromOperations(explicitActivityId, grantedPermissions)
        }
        if (explicitActivityId == null) {
            repairCompletedWorkoutWorkflows(grantedPermissions, blockedAggregates)
        }

        val report = baseline.report.copy(
            finishedAt = Instant.now(),
            processedCount = operations.size,
            syncedCount = syncedCount,
            failedCount = failures.size,
            writeAcceptedWithoutReadbackCount = writeAcceptedWithoutReadbackCount,
            requiredPermissions = missingPermissions,
        )
        if (failures.isEmpty()) {
            return HealthConnectSyncOutcome.Completed(report)
        }
        if (syncedCount == 0 && failures.all { it.error == SafeSyncError.PERMISSION_REQUIRED }) {
            return HealthConnectSyncOutcome.PermissionRequired(report, missingPermissions)
        }
        return HealthConnectSyncOutcome.CompletedWithFailures(report, failures)
    }

    private suspend fun prepareActivityOperations(
        activityId: WorkoutActivityId,
        grantedPermissions: Set<HealthConnectWritePermission>,
    ): PreparedActivityOperations {
        val activity = when (val result = workoutActivityRepository.findById(activityId)) {
            is RepositoryResult.Success -> result.value
            else -> return PreparedActivityOperations.Failed(
                error = SafeSyncError.INVALID_LOCAL_DATA,
                reason = "Atividade não encontrada para sync explícito.",
            )
        }
        if (activity.activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4) {
            return prepareWatchActivityOperations(activity)
        }
        val eligibleRecordTypes = healthRecordMapper.findEligibleWorkoutRecordTypes(activity)
        if (eligibleRecordTypes.isEmpty()) {
            return PreparedActivityOperations.Failed(
                error = SafeSyncError.INVALID_LOCAL_DATA,
                reason = "Atividade sem records elegíveis para sincronização.",
            )
        }
        val missingPermissions = eligibleRecordTypes
            .map(::permissionFor)
            .filterNot { it in grantedPermissions }
            .toSet()
        val grantedRecordTypes = eligibleRecordTypes.filter { permissionFor(it) in grantedPermissions }.toSet()
        if (HealthSyncRecordType.EXERCISE_SESSION !in grantedRecordTypes) {
            return PreparedActivityOperations.PermissionRequired(
                missingPermissions = missingPermissions.ifEmpty { setOf(HealthConnectWritePermission.EXERCISE) },
            )
        }
        when (workoutActivityRepository.startSync(activityId)) {
            is RepositoryResult.Success -> Unit
            else -> return PreparedActivityOperations.Failed(
                error = SafeSyncError.INVALID_LOCAL_DATA,
                reason = "Atividade não está em estado válido para iniciar sync explícito.",
            )
        }
        val operations = mutableListOf<HealthSyncOperation>()
        for (recordType in grantedRecordTypes) {
            when (
                val ensured = healthSyncRepository.ensureOperation(
                    HealthSyncOperationKey(
                        aggregate = HealthSyncAggregate.WorkoutActivity(activityId),
                        recordType = recordType,
                    ),
                )
            ) {
                is RepositoryResult.Success -> operations += ensured.value
                else -> {
                    workoutActivityRepository.markSyncFailed(activityId)
                    return PreparedActivityOperations.Failed(
                        error = SafeSyncError.INTERNAL,
                        reason = "Não foi possível garantir operações granulares da atividade.",
                    )
                }
            }
        }
        return PreparedActivityOperations.Success(operations)
    }

    /** Prepara somente atividades que já possuem fila local ou tombstone legado. */
    private suspend fun preparePendingWatchActivityOperations(
        grantedPermissions: Set<HealthConnectWritePermission>,
    ): RepositoryResult<Set<HealthSyncAggregate>> {
        val blockedAggregates = linkedSetOf<HealthSyncAggregate>()
        val activities = workoutActivityRepository.observeActivities(WorkoutActivityFilter.ALL).first()
        for (activity in activities) {
            if (activity.activityOrigin != WorkoutActivityOrigin.WATCH_D3_D4) continue
            val aggregate = HealthSyncAggregate.WorkoutActivity(activity.id)
            val legacyStatus = when (
                val result = healthSyncRepository.inspectLegacyWorkoutDistanceProjection(
                    activityId = activity.id,
                    activityOrigin = activity.activityOrigin,
                )
            ) {
                is RepositoryResult.Success -> result.value
                else -> HealthLegacyDistanceProjectionStatus.UNKNOWN_ID
            }
            if (legacyStatus == HealthLegacyDistanceProjectionStatus.UNKNOWN_ID) {
                blockedAggregates += aggregate
                continue
            }
            val existingOperations = when (val result = healthSyncRepository.findOperationsForAggregate(aggregate)) {
                is RepositoryResult.Success -> result.value
                else -> {
                    blockedAggregates += aggregate
                    continue
                }
            }
            if (legacyStatus == HealthLegacyDistanceProjectionStatus.ABSENT && existingOperations.isEmpty()) {
                continue
            }
            val prepared = prepareWatchActivityOperations(
                activity = activity,
                existingOperationsOverride = existingOperations,
                startActivity = true,
            )
            when (prepared) {
                is PreparedActivityOperations.Success -> {
                    if (prepared.operations.isEmpty()) {
                        updateWorkoutWorkflowFromOperations(activity.id, grantedPermissions)
                    }
                }
                is PreparedActivityOperations.PermissionRequired,
                PreparedActivityOperations.PlausibilityAcceptanceRequired,
                is PreparedActivityOperations.PlausibilityBlocked,
                is PreparedActivityOperations.Failed,
                -> blockedAggregates += aggregate
            }
        }
        return RepositoryResult.Success(blockedAggregates)
    }

    private suspend fun prepareWatchActivityOperations(
        activity: br.com.mwconsulting.championcompanion.core.model.WorkoutActivity,
        existingOperationsOverride: List<HealthSyncOperation>? = null,
        startActivity: Boolean = true,
    ): PreparedActivityOperations {
        val existingOperations = existingOperationsOverride ?: when (
            val result = healthSyncRepository.findOperationsForAggregate(
                HealthSyncAggregate.WorkoutActivity(activity.id),
            )
        ) {
            is RepositoryResult.Success -> result.value
            else -> return PreparedActivityOperations.Failed(
                error = SafeSyncError.INTERNAL,
                reason = "Não foi possível carregar operações existentes da atividade.",
            )
        }
        val snapshot = activity.derivationSnapshot
        val isV4WatchWorkout = activity.watchImportContext?.parserVersion?.value == WATCH_WORKOUT_D4_V4_PARSER_VERSION
        if (isV4WatchWorkout && activity.confirmedType != WorkoutType.CYCLING && snapshot == null) {
            return PreparedActivityOperations.Failed(
                error = SafeSyncError.INVALID_LOCAL_DATA,
                reason = "Atividade v4 do relógio sem snapshot de derivação materializado.",
            )
        }
        val usesCanonicalWorkoutProjection = activity.confirmedType != WorkoutType.CYCLING
        if (usesCanonicalWorkoutProjection && snapshot?.requiresExplicitPlausibilityAcceptance == true) {
            return PreparedActivityOperations.PlausibilityAcceptanceRequired
        }
        if (usesCanonicalWorkoutProjection && snapshot?.hasPlausibilityIndicators == true) {
            return PreparedActivityOperations.PlausibilityBlocked(snapshot.plausibility.indicators)
        }
        val eligibleChildren = healthRecordMapper.findEligibleWorkoutProjectionChildren(activity)
        if (eligibleChildren.isEmpty()) {
            return PreparedActivityOperations.Failed(
                error = SafeSyncError.INVALID_LOCAL_DATA,
                reason = "Atividade do relógio sem filhos elegíveis para sincronização.",
            )
        }
        val origin = HealthProjectionOrigin(
            originId = activity.id.value.toString(),
            originVersion = activity.derivationSnapshot?.derivationVersion ?: WATCH_ACTIVITY_PROJECTION_VERSION,
        )
        val nextManifest = runCatching {
            HealthProjectionManifest(
                aggregate = HealthSyncAggregate.WorkoutActivity(activity.id),
                children = eligibleChildren,
                origin = origin,
            )
        }.getOrNull() ?: return PreparedActivityOperations.Failed(
            error = SafeSyncError.INVALID_LOCAL_DATA,
            reason = "Manifesto local de projeção inválido.",
        )
        val existingChildOperations = existingOperations
            .filter(HealthSyncOperation::isProjectionChild)
            .associateBy { operation -> operation.key.projectionChildKey }
        val previousManifest = existingChildOperations.keys
            .filterNotNull()
            .takeIf { it.isNotEmpty() }
            ?.let { previousChildren ->
                runCatching {
                    HealthProjectionManifest(
                        aggregate = HealthSyncAggregate.WorkoutActivity(activity.id),
                        children = previousChildren,
                        origin = origin,
                    )
                }.getOrNull()
            }
        val obsoleteChildren = previousManifest?.children.orEmpty()
            .filterNot(nextManifest.children.toSet()::contains)
        val clientRecordVersion = maxOf(
            activity.recordedAt.toEpochMilli(),
            activity.reviewedAt?.toEpochMilli() ?: Long.MIN_VALUE,
        )
        val operations = mutableListOf<HealthSyncOperation>()
        val canReconcileLegacyDistance = activity.confirmedType == WorkoutType.CYCLING ||
            nextManifest.children.any { child ->
                child is HealthProjectionChildKey.DistanceInterval ||
                    child == HealthProjectionChildKey.ManualDistance
            }
        val legacyStatus = when (
            val result = healthSyncRepository.inspectLegacyWorkoutDistanceProjection(
                activityId = activity.id,
                activityOrigin = activity.activityOrigin,
            )
        ) {
            is RepositoryResult.Success -> result.value
            else -> HealthLegacyDistanceProjectionStatus.UNKNOWN_ID
        }
        if (legacyStatus == HealthLegacyDistanceProjectionStatus.UNKNOWN_ID) {
            return PreparedActivityOperations.Failed(
                error = SafeSyncError.INVALID_LOCAL_DATA,
                reason = "Identidade local do agregado legado não é conhecida com segurança.",
            )
        }
        if (legacyStatus == HealthLegacyDistanceProjectionStatus.KNOWN_PENDING_DELETE &&
            !canReconcileLegacyDistance
        ) {
            return PreparedActivityOperations.Failed(
                error = SafeSyncError.INVALID_LOCAL_DATA,
                reason = "Agregado legado não pode ser removido sem série de distância substituta elegível.",
            )
        }
        if (legacyStatus == HealthLegacyDistanceProjectionStatus.KNOWN_PENDING_DELETE &&
            canReconcileLegacyDistance
        ) {
            when (val prepared = healthSyncRepository.prepareLegacyWorkoutDistanceDeletion(activity.id)) {
                is RepositoryResult.Success -> operations += prepared.value
                else -> return PreparedActivityOperations.Failed(
                    error = SafeSyncError.INTERNAL,
                    reason = "Não foi possível preparar remoção exata do agregado legado.",
                )
            }
        }

        for (child in nextManifest.children) {
            val key = HealthSyncOperationKey(
                aggregate = nextManifest.aggregate,
                recordType = recordTypeFor(child),
                projectionChildKey = child,
            )
            val frozen = when (val result = healthRecordMapper.freezeWorkoutProjection(key, activity)) {
                is HealthSyncFrozenPayloadResult.Success -> result.value
                is HealthSyncFrozenPayloadResult.Failure -> {
                    return PreparedActivityOperations.Failed(result.error, result.reason)
                }
            }
            val workoutStepsContext = if (child == HealthProjectionChildKey.WorkoutSteps) {
                activity.workoutStepsProjectionContextOrNull()
                    ?: return PreparedActivityOperations.Failed(
                        error = SafeSyncError.INVALID_LOCAL_DATA,
                        reason = "Janela local de passos da atividade inválida.",
                    )
            } else {
                null
            }
            when (
                val prepared = healthSyncRepository.prepareProjectionChild(
                    key = key,
                    action = HealthSyncOperationAction.INSERT,
                    frozenPayload = frozen,
                    clientRecordVersion = clientRecordVersion,
                    projectionOrigin = origin,
                    workoutStepsContext = workoutStepsContext,
                )
            ) {
                is RepositoryResult.Success -> operations += prepared.value
                else -> return PreparedActivityOperations.Failed(
                    error = SafeSyncError.INTERNAL,
                    reason = "Não foi possível preparar filho congelado da atividade.",
                )
            }
        }

        for (child in obsoleteChildren) {
            val existing = existingChildOperations[child]
                ?: return PreparedActivityOperations.Failed(
                    error = SafeSyncError.INVALID_LOCAL_DATA,
                    reason = "Manifesto anterior referencia filho ausente.",
                )
            val frozen = existing.frozenPayload
                ?: return PreparedActivityOperations.Failed(
                    error = SafeSyncError.INVALID_LOCAL_DATA,
                    reason = "Filho obsoleto não possui payload congelado.",
                )
            val projectionOrigin = existing.projectionOrigin
                ?: return PreparedActivityOperations.Failed(
                    error = SafeSyncError.INVALID_LOCAL_DATA,
                    reason = "Filho obsoleto não possui origem local.",
                )
            val version = existing.clientRecordVersion
                ?: return PreparedActivityOperations.Failed(
                    error = SafeSyncError.INVALID_LOCAL_DATA,
                    reason = "Filho obsoleto não possui versão congelada.",
                )
            when (
                val prepared = healthSyncRepository.prepareProjectionChild(
                    key = existing.key,
                    action = HealthSyncOperationAction.DELETE,
                    frozenPayload = frozen,
                    clientRecordVersion = version,
                    projectionOrigin = projectionOrigin,
                )
            ) {
                is RepositoryResult.Success -> operations += prepared.value
                else -> return PreparedActivityOperations.Failed(
                    error = SafeSyncError.INTERNAL,
                    reason = "Não foi possível preparar remoção exata de filho obsoleto.",
                )
            }
        }

        val processable = operations.mapNotNull { operation ->
            if (!operation.isPending || when {
                    operation.isLegacyWorkoutDistanceDelete() -> !canReconcileLegacyDistance
                    operation.isProjectionChild -> false
                    else -> true
                }
            ) {
                return@mapNotNull null
            }
            if (operation.key.projectionChildKey == HealthProjectionChildKey.WorkoutSteps) {
                val credit = when (val result = healthSyncRepository.findWorkoutStepsCredit(operation.id)) {
                    is RepositoryResult.Success -> result.value
                    else -> return@mapNotNull null
                }
                if (credit == null || credit.state == WorkoutStepsCreditState.BLOCKED) {
                    return@mapNotNull null
                }
            }
            operation
        }
        val representedChildren = operations
            .filter { operation -> operation.action == HealthSyncOperationAction.INSERT }
            .mapNotNull { operation -> operation.key.projectionChildKey }
            .toSet()
        if (!nextManifest.children.all(representedChildren::contains)) {
            return PreparedActivityOperations.Failed(
                error = SafeSyncError.INVALID_LOCAL_DATA,
                reason = "Manifesto local não contém todos os filhos esperados da atividade.",
            )
        }
        if (startActivity && processable.isNotEmpty()) {
            when (workoutActivityRepository.startSync(activity.id)) {
                is RepositoryResult.Success -> Unit
                else -> return PreparedActivityOperations.Failed(
                    error = SafeSyncError.INVALID_LOCAL_DATA,
                    reason = "Atividade não está em estado válido para iniciar sync explícito.",
                )
            }
        }
        return PreparedActivityOperations.Success(processable)
    }

    private suspend fun loadAndMap(operation: HealthSyncOperation): HealthRecordMappingResult = when (val aggregate = operation.key.aggregate) {
        is HealthSyncAggregate.DailySummary -> healthRecordMapper.mapDailySummary(operation)

        is HealthSyncAggregate.WatchDailyD1Sample -> healthRecordMapper.mapFrozenDailyD1Sample(operation)

        is HealthSyncAggregate.HeartRateMeasurement -> when (val measurement = heartRateRepository.findById(aggregate.measurementId)) {
            is RepositoryResult.Success -> healthRecordMapper.mapHeartRateMeasurement(operation, measurement.value)
            else -> HealthRecordMappingResult.Failure(SafeSyncError.INVALID_LOCAL_DATA, "Medição de BPM não encontrada para sync.")
        }

        is HealthSyncAggregate.WeightMeasurement -> when (val weight = personalMetricsRepository.findWeightById(aggregate.measurementId)) {
            is RepositoryResult.Success -> healthRecordMapper.mapWeightMeasurement(operation, weight.value)
            else -> HealthRecordMappingResult.Failure(SafeSyncError.INVALID_LOCAL_DATA, "Medição de peso não encontrada para sync.")
        }

        is HealthSyncAggregate.UserProfileHeight -> when (val profile = personalMetricsRepository.findProfileById(aggregate.profileId)) {
            is RepositoryResult.Success -> healthRecordMapper.mapUserProfileHeight(operation, profile.value)
            else -> HealthRecordMappingResult.Failure(SafeSyncError.INVALID_LOCAL_DATA, "Perfil não encontrado para sync de altura.")
        }

        is HealthSyncAggregate.WorkoutActivity -> if (operation.isProjectionChild) {
            healthRecordMapper.mapFrozenWorkoutActivity(operation)
        } else {
            when (val activity = workoutActivityRepository.findById(aggregate.activityId)) {
                is RepositoryResult.Success -> healthRecordMapper.mapWorkoutActivity(operation, activity.value)
                else -> HealthRecordMappingResult.Failure(SafeSyncError.INVALID_LOCAL_DATA, "Atividade não encontrada para sync.")
            }
        }
    }

    private suspend fun updateWorkoutWorkflowFromOperations(
        activityId: WorkoutActivityId,
        grantedPermissions: Set<HealthConnectWritePermission>,
    ) {
        val storedOperations = when (val result = healthSyncRepository.findOperationsForAggregate(HealthSyncAggregate.WorkoutActivity(activityId))) {
            is RepositoryResult.Success -> result.value
            else -> return
        }
        val projectionOperations = storedOperations.filter(HealthSyncOperation::isProjectionChild)
        val operations = if (projectionOperations.isNotEmpty()) {
            storedOperations
        } else {
            storedOperations
        }
        if (operations.isEmpty()) {
            workoutActivityRepository.markSyncFailed(activityId)
            return
        }
        val activity = when (val result = workoutActivityRepository.findById(activityId)) {
            is RepositoryResult.Success -> result.value
            else -> return
        }
        if (activity.activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4 &&
            activity.confirmedType != WorkoutType.CYCLING &&
            activity.derivationSnapshot?.isEligibleForHealthProjection != true
        ) {
            if (activity.workflowState != ActivityWorkflowState.SYNCED) {
                workoutActivityRepository.markSyncFailed(activityId)
            }
            return
        }
        val expectedProjectionChildren = if (activity.activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4) {
            healthRecordMapper.findEligibleWorkoutProjectionChildren(activity).toSet()
        } else {
            emptySet()
        }
        val storedProjectionChildren = projectionOperations.mapNotNull { it.key.projectionChildKey }.toSet()
        val obsoleteInsertOperations = if (activity.activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4) {
            projectionOperations.filter { operation ->
                operation.action == HealthSyncOperationAction.INSERT &&
                    operation.key.projectionChildKey !in expectedProjectionChildren
            }
        } else {
            emptyList()
        }
        if (obsoleteInsertOperations.isNotEmpty() ||
            (activity.activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4 &&
                !expectedProjectionChildren.all(storedProjectionChildren::contains))
        ) {
            workoutActivityRepository.markSyncFailed(activityId)
            return
        }
        if (operations.all { operation -> operation.isLocallyResolved() }) {
            if (activity.workflowState == ActivityWorkflowState.SYNCED) return
            if (activity.workflowState != ActivityWorkflowState.SYNCING) {
                when (workoutActivityRepository.startSync(activityId)) {
                    is RepositoryResult.Success -> Unit
                    else -> return
                }
            }
            workoutActivityRepository.markSynced(activityId)
        } else {
            workoutActivityRepository.markSyncFailed(activityId)
        }
    }

    private suspend fun HealthSyncOperation.isLocallyResolved(): Boolean {
        if (state == SyncOperationState.SYNCED) return true
        if (key.projectionChildKey != HealthProjectionChildKey.WorkoutSteps) return false
        return when (val result = healthSyncRepository.findWorkoutStepsCredit(id)) {
            is RepositoryResult.Success -> result.value?.state == WorkoutStepsCreditState.BLOCKED
            else -> false
        }
    }

    private suspend fun repairCompletedWorkoutWorkflows(
        grantedPermissions: Set<HealthConnectWritePermission>,
        blockedAggregates: Set<HealthSyncAggregate> = emptySet(),
    ) {
        workoutActivityRepository.observeActivities(WorkoutActivityFilter.ALL).first()
            .filter { it.workflowState != ActivityWorkflowState.SYNCED }
            .forEach { activity ->
                val aggregate = HealthSyncAggregate.WorkoutActivity(activity.id)
                if (aggregate in blockedAggregates) return@forEach
                when (val result = healthSyncRepository.findOperationsForAggregate(
                    aggregate,
                )) {
                    is RepositoryResult.Success -> if (
                        result.value.isNotEmpty() &&
                            result.value.all { operation -> operation.isLocallyResolved() }
                    ) {
                        updateWorkoutWorkflowFromOperations(activity.id, grantedPermissions)
                    }

                    else -> Unit
                }
            }
    }
}

private suspend fun filterProcessableOperations(
    operations: List<HealthSyncOperation>,
    workoutActivityRepository: WorkoutActivityRepository,
    healthRecordMapper: HealthRecordMapper,
    healthSyncRepository: HealthSyncRepository,
): List<HealthSyncOperation> {
    val expectedChildrenByActivity = mutableMapOf<WorkoutActivityId, Set<HealthProjectionChildKey>>()
    val filtered = mutableListOf<HealthSyncOperation>()
    for (operation in operations) {
        val aggregate = operation.key.aggregate as? HealthSyncAggregate.WorkoutActivity
        val child = operation.key.projectionChildKey
        if (aggregate == null || child == null) {
            filtered += operation
            continue
        }
        val activity = when (val result = workoutActivityRepository.findById(aggregate.activityId)) {
            is RepositoryResult.Success -> result.value
            else -> continue
        }
        if (activity.activityOrigin != WorkoutActivityOrigin.WATCH_D3_D4) {
            filtered += operation
            continue
        }
        if (operation.action == HealthSyncOperationAction.INSERT &&
            child != HealthProjectionChildKey.WorkoutSteps &&
            activity.confirmedType != WorkoutType.CYCLING &&
            activity.derivationSnapshot?.isEligibleForHealthProjection != true
        ) {
            continue
        }
        if (child == HealthProjectionChildKey.WorkoutSteps) {
            val credit = when (val result = healthSyncRepository.findWorkoutStepsCredit(operation.id)) {
                is RepositoryResult.Success -> result.value
                else -> null
            }
            if (credit == null || credit.state == WorkoutStepsCreditState.BLOCKED) continue
        }
        val expectedChildren = expectedChildrenByActivity.getOrPut(aggregate.activityId) {
            healthRecordMapper.findEligibleWorkoutProjectionChildren(activity).toSet()
        }
        if (
            (operation.action == HealthSyncOperationAction.INSERT && child in expectedChildren) ||
            operation.action == HealthSyncOperationAction.DELETE
        ) {
            filtered += operation
        }
    }
    return filtered
}

private fun orderOperationsForRemoteEffects(
    operations: List<HealthSyncOperation>,
): List<HealthSyncOperation> = operations.sortedWith(
    compareBy<HealthSyncOperation> {
        when {
            it.isLegacyWorkoutDistanceDelete() -> 0
            it.action == HealthSyncOperationAction.DELETE -> 1
            it.key.recordType == HealthSyncRecordType.DISTANCE ||
                it.key.recordType == HealthSyncRecordType.SPEED -> 2
            else -> 3
        }
    }.thenBy { it.updatedAt ?: it.createdAt ?: Instant.EPOCH }
        .thenBy { it.id.value.toString() },
)

private sealed interface PreparedActivityOperations {
    data class Success(
        val operations: List<HealthSyncOperation>,
    ) : PreparedActivityOperations

    data class PermissionRequired(
        val missingPermissions: Set<HealthConnectWritePermission>,
    ) : PreparedActivityOperations

    data object PlausibilityAcceptanceRequired : PreparedActivityOperations

    data class PlausibilityBlocked(
        val indicators: Set<WorkoutPlausibilityIndicator>,
    ) : PreparedActivityOperations

    data class Failed(
        val error: SafeSyncError,
        val reason: String,
    ) : PreparedActivityOperations
}

private data class PreparedSync(
    val report: HealthConnectSyncReport,
)

private fun HealthConnectSyncReport.finish(at: Instant = Instant.now()): HealthConnectSyncReport = copy(finishedAt = at)

private fun HealthConnectFailureKind.toSafeSyncError(): SafeSyncError = when (this) {
    HealthConnectFailureKind.PERMISSION_REQUIRED -> SafeSyncError.PERMISSION_REQUIRED
    HealthConnectFailureKind.PROVIDER_UNAVAILABLE -> SafeSyncError.PROVIDER_UNAVAILABLE
    HealthConnectFailureKind.PROVIDER_UPDATE_REQUIRED -> SafeSyncError.PROVIDER_UPDATE_REQUIRED
    HealthConnectFailureKind.WRITE_REJECTED -> SafeSyncError.WRITE_REJECTED
    HealthConnectFailureKind.INVALID_LOCAL_DATA -> SafeSyncError.INVALID_LOCAL_DATA
    HealthConnectFailureKind.INTERNAL -> SafeSyncError.INTERNAL
}

private fun SafeSyncError.toHealthConnectFailureKind(): HealthConnectFailureKind = when (this) {
    SafeSyncError.PERMISSION_REQUIRED -> HealthConnectFailureKind.PERMISSION_REQUIRED
    SafeSyncError.PROVIDER_UNAVAILABLE -> HealthConnectFailureKind.PROVIDER_UNAVAILABLE
    SafeSyncError.PROVIDER_UPDATE_REQUIRED -> HealthConnectFailureKind.PROVIDER_UPDATE_REQUIRED
    SafeSyncError.WRITE_REJECTED -> HealthConnectFailureKind.WRITE_REJECTED
    SafeSyncError.INVALID_LOCAL_DATA -> HealthConnectFailureKind.INVALID_LOCAL_DATA
    SafeSyncError.RETRY_LATER,
    SafeSyncError.INTERRUPTED,
    SafeSyncError.INTERNAL,
    -> HealthConnectFailureKind.INTERNAL
}

private fun HealthSyncOperation.failure(
    error: SafeSyncError,
    reason: String,
    failureKind: HealthConnectFailureKind = error.toHealthConnectFailureKind(),
) = HealthConnectSyncFailure(
    operationId = id,
    recordType = key.recordType,
    error = error,
    reason = reason,
    failureKind = failureKind,
    projectionChildKey = key.projectionChildKey,
)

private data class LegacyRemoteEffectBlock(
    val error: SafeSyncError,
    val reason: String,
)

private suspend fun HealthSyncOperation.legacyRemoteEffectBlock(
    healthSyncRepository: HealthSyncRepository,
    workoutActivityRepository: WorkoutActivityRepository,
    cache: MutableMap<WorkoutActivityId, HealthLegacyDistanceProjectionStatus>,
): LegacyRemoteEffectBlock? {
    val aggregate = key.aggregate as? HealthSyncAggregate.WorkoutActivity ?: return null
    val activityId = aggregate.activityId
    cache[activityId]?.let { status ->
        return status.toRemoteEffectBlock(operation = this)
    }
    val activityOrigin = when (val result = workoutActivityRepository.findById(activityId)) {
        is RepositoryResult.Success -> result.value.activityOrigin
        else -> return LegacyRemoteEffectBlock(
            error = SafeSyncError.INVALID_LOCAL_DATA,
            reason = "Atividade do relógio não pôde ser validada localmente.",
        )
    }
    if (activityOrigin != WorkoutActivityOrigin.WATCH_D3_D4) {
        cache[activityId] = HealthLegacyDistanceProjectionStatus.ABSENT
        return null
    }
    val status = when (val result = healthSyncRepository.inspectLegacyWorkoutDistanceProjection(activityId, activityOrigin)) {
        is RepositoryResult.Success -> result.value
        else -> HealthLegacyDistanceProjectionStatus.UNKNOWN_ID
    }
    cache[activityId] = status
    return status.toRemoteEffectBlock(operation = this)
}

private fun HealthLegacyDistanceProjectionStatus.toRemoteEffectBlock(
    operation: HealthSyncOperation,
): LegacyRemoteEffectBlock? = when (this) {
    HealthLegacyDistanceProjectionStatus.ABSENT,
    HealthLegacyDistanceProjectionStatus.RECONCILED,
    -> null

    HealthLegacyDistanceProjectionStatus.KNOWN_PENDING_DELETE -> if (
        operation.action == HealthSyncOperationAction.INSERT &&
            operation.key.recordType in setOf(HealthSyncRecordType.DISTANCE, HealthSyncRecordType.SPEED)
    ) {
        LegacyRemoteEffectBlock(
            error = SafeSyncError.RETRY_LATER,
            reason = "Projeções de distância e velocidade aguardam reconciliação do agregado legado.",
        )
    } else {
        null
    }

    HealthLegacyDistanceProjectionStatus.UNKNOWN_ID -> LegacyRemoteEffectBlock(
        error = SafeSyncError.INVALID_LOCAL_DATA,
        reason = "Identidade local do agregado legado não é conhecida com segurança.",
    )
}

private fun HealthSyncOperation.isLegacyWorkoutDistanceDelete(): Boolean =
    action == HealthSyncOperationAction.DELETE &&
        key.recordType == HealthSyncRecordType.DISTANCE &&
        key.projectionChildKey == null &&
        key.aggregate is HealthSyncAggregate.WorkoutActivity

private fun recordTypeFor(child: HealthProjectionChildKey): HealthSyncRecordType =
    HealthSyncRecordType.entries.first { it.name == child.recordTypeName }

private fun <T> RepositoryResult<T>.successOrNull(): T? = (this as? RepositoryResult.Success)?.value

private fun RepositoryResult<Int>.successOrZero(): Int = (this as? RepositoryResult.Success)?.value ?: 0

private fun br.com.mwconsulting.championcompanion.core.model.WorkoutActivity.workoutStepsProjectionContextOrNull(): WorkoutStepsProjectionContext? {
    if (activityOrigin != WorkoutActivityOrigin.WATCH_D3_D4 ||
        parserValidity != ParserValidity.VALID ||
        workflowState == ActivityWorkflowState.DISCARDED ||
        confirmedType == null ||
        confirmedType == WorkoutType.UNKNOWN ||
        confirmedType == WorkoutType.CYCLING ||
        !metrics.stepsAvailable ||
        metrics.steps <= 0L ||
        metrics.duration.isZero ||
        metrics.duration.isNegative
    ) return null
    val end = runCatching { metrics.startedAt.plus(metrics.duration) }.getOrNull() ?: return null
    val window = WorkoutIntervalWindow.create(metrics.startedAt, end).valueOrNull() ?: return null
    val zoneId = ZoneId.systemDefault()
    val endInclusive = runCatching { end.minusNanos(1L) }.getOrNull() ?: return null
    if (metrics.startedAt.atZone(zoneId).toLocalDate() != endInclusive.atZone(zoneId).toLocalDate()) return null
    return WorkoutStepsProjectionContext(steps = metrics.steps, window = window)
}

private fun permissionFor(recordType: HealthSyncRecordType): HealthConnectWritePermission = when (recordType) {
    HealthSyncRecordType.STEPS -> HealthConnectWritePermission.STEPS
    HealthSyncRecordType.STEPS_CADENCE -> HealthConnectWritePermission.STEPS
    HealthSyncRecordType.HEART_RATE -> HealthConnectWritePermission.HEART_RATE
    HealthSyncRecordType.EXERCISE_SESSION -> HealthConnectWritePermission.EXERCISE
    HealthSyncRecordType.DISTANCE -> HealthConnectWritePermission.DISTANCE
    HealthSyncRecordType.ACTIVE_CALORIES_BURNED -> HealthConnectWritePermission.ACTIVE_CALORIES_BURNED
    HealthSyncRecordType.SPEED -> HealthConnectWritePermission.SPEED
    HealthSyncRecordType.TOTAL_CALORIES_BURNED -> HealthConnectWritePermission.TOTAL_CALORIES_BURNED
    HealthSyncRecordType.WEIGHT -> HealthConnectWritePermission.WEIGHT
    HealthSyncRecordType.HEIGHT -> HealthConnectWritePermission.HEIGHT
}

private const val WATCH_ACTIVITY_PROJECTION_VERSION = "watch-activity-v1"
