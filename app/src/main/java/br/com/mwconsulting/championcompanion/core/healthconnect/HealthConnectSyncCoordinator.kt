package br.com.mwconsulting.championcompanion.core.healthconnect

import br.com.mwconsulting.championcompanion.core.common.IoDispatcher
import br.com.mwconsulting.championcompanion.core.model.AppPreferencesRepository
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregate
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperation
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionChildKey
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionManifest
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionOrigin
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationId
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationAction
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationKey
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRepository
import br.com.mwconsulting.championcompanion.core.model.HeartRateRepository
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsRepository
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.SafeSyncError
import br.com.mwconsulting.championcompanion.core.model.SyncOperationState
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityOrigin
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
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
    val requiredPermissions: Set<HealthConnectWritePermission> = emptySet(),
)

data class HealthConnectSyncFailure(
    val operationId: HealthSyncOperationId,
    val recordType: HealthSyncRecordType,
    val error: SafeSyncError,
    val reason: String,
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
                processableOperations = { healthSyncRepository.findProcessable(safeLimit) },
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
            processableOperations = { RepositoryResult.Success(emptyList()) },
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
        processableOperations: suspend () -> RepositoryResult<List<HealthSyncOperation>>,
    ): HealthConnectSyncOutcome {
        val preferences = appPreferencesRepository.observe().first()
        if (!preferences.healthSyncEnabled) {
            return HealthConnectSyncOutcome.Disabled(baseline.report.finish())
        }

        return when (val availability = healthConnectGateway.getAvailability()) {
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
        processableOperations: suspend () -> RepositoryResult<List<HealthSyncOperation>>,
    ): HealthConnectSyncOutcome {
        val grantedPermissions = healthConnectGateway.getGrantedPermissions()
        val operations = when {
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

                is PreparedActivityOperations.Failed -> {
                    return HealthConnectSyncOutcome.Failed(
                        report = baseline.report.finish(),
                        error = prepared.error,
                        reason = prepared.reason,
                    )
                }
            }

            else -> when (val result = processableOperations()) {
                is RepositoryResult.Success -> filterProcessableOperations(
                    operations = result.value,
                    workoutActivityRepository = workoutActivityRepository,
                    healthRecordMapper = healthRecordMapper,
                )
                else -> return HealthConnectSyncOutcome.Failed(
                    report = baseline.report.finish(),
                    error = SafeSyncError.INTERNAL,
                    reason = "Não foi possível carregar operações processáveis da fila.",
                )
            }
        }
        if (operations.isEmpty()) {
            if (explicitActivityId != null) {
                updateWorkoutWorkflowFromOperations(explicitActivityId, grantedPermissions)
            }
            return HealthConnectSyncOutcome.NothingToSync(baseline.report.finish())
        }

        val failures = mutableListOf<HealthConnectSyncFailure>()
        val missingPermissions = linkedSetOf<HealthConnectWritePermission>()
        val touchedActivities = linkedSetOf<WorkoutActivityId>()
        var syncedCount = 0
        val legacyDistanceGateByActivity = mutableMapOf<WorkoutActivityId, Boolean>()

        for (operation in operations) {
            val now = Instant.now()
            if (operation.key.aggregate is HealthSyncAggregate.WorkoutActivity) {
                touchedActivities += operation.key.aggregate.activityId
            }
            if (operation.isBlockedByKnownLegacyDistance(
                    healthSyncRepository = healthSyncRepository,
                    workoutActivityRepository = workoutActivityRepository,
                    cache = legacyDistanceGateByActivity,
                )
            ) {
                failures += operation.failure(
                    SafeSyncError.RETRY_LATER,
                    "Projeções de distância e velocidade aguardam reconciliação do agregado legado.",
                )
                continue
            }
            when (val started = healthSyncRepository.markStarted(operation.id, now)) {
                is RepositoryResult.Success -> Unit
                else -> {
                    failures += operation.failure(SafeSyncError.INTERNAL, "Não foi possível marcar operação como `SYNCING`.")
                    continue
                }
            }

            val mapped = loadAndMap(operation)
            if (mapped is HealthRecordMappingResult.Failure) {
                healthSyncRepository.markFailed(operation.id, mapped.error, Instant.now())
                failures += operation.failure(mapped.error, mapped.reason)
                continue
            }

            val permission = (mapped as HealthRecordMappingResult.Success).value.permission
            if (permission !in grantedPermissions) {
                healthSyncRepository.markFailed(operation.id, SafeSyncError.PERMISSION_REQUIRED, Instant.now())
                missingPermissions += permission
                failures += operation.failure(
                    SafeSyncError.PERMISSION_REQUIRED,
                    "Permissão `${permission.rawValue}` não concedida para `${operation.key.recordType}`.",
                )
                continue
            }

            val insertOutcome = runCatching {
                healthConnectGateway.insert(listOf(mapped.value.record))
            }
            if (insertOutcome.isSuccess) {
                when (healthSyncRepository.markSynced(operation.id, Instant.now())) {
                    is RepositoryResult.Success -> syncedCount += 1
                    else -> failures += operation.failure(
                        SafeSyncError.INTERNAL,
                        "Record enviado, mas estado local não pôde ser marcado como `SYNCED`.",
                    )
                }
            } else {
                val safeError = insertOutcome.exceptionOrNull().toSafeSyncError()
                healthSyncRepository.markFailed(operation.id, safeError, Instant.now())
                failures += operation.failure(
                    safeError,
                    insertOutcome.exceptionOrNull()?.message ?: "Falha ao escrever record no provider.",
                )
                if (safeError == SafeSyncError.PERMISSION_REQUIRED) {
                    missingPermissions += permission
                }
            }
        }

        touchedActivities.forEach { updateWorkoutWorkflowFromOperations(it, grantedPermissions) }
        if (explicitActivityId != null && explicitActivityId !in touchedActivities) {
            updateWorkoutWorkflowFromOperations(explicitActivityId, grantedPermissions)
        }

        val report = baseline.report.copy(
            finishedAt = Instant.now(),
            processedCount = operations.size,
            syncedCount = syncedCount,
            failedCount = failures.size,
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

    private suspend fun prepareWatchActivityOperations(
        activity: br.com.mwconsulting.championcompanion.core.model.WorkoutActivity,
    ): PreparedActivityOperations {
        val existingOperations = when (
            val result = healthSyncRepository.findOperationsForAggregate(
                HealthSyncAggregate.WorkoutActivity(activity.id),
            )
        ) {
            is RepositoryResult.Success -> result.value.associateBy { operation ->
                operation.key.projectionChildKey
            }
            else -> return PreparedActivityOperations.Failed(
                error = SafeSyncError.INTERNAL,
                reason = "Não foi possível carregar operações existentes da atividade.",
            )
        }
        val eligibleChildren = healthRecordMapper.findEligibleWorkoutProjectionChildren(activity)
        val children = (
            eligibleChildren +
                existingOperations.values.mapNotNull { operation -> operation.key.projectionChildKey }
            ).distinct()
        if (children.isEmpty()) {
            return PreparedActivityOperations.Failed(
                error = SafeSyncError.INVALID_LOCAL_DATA,
                reason = "Atividade do relógio sem filhos elegíveis para sincronização.",
            )
        }
        val origin = HealthProjectionOrigin(
            originId = activity.id.value.toString(),
            originVersion = activity.derivationSnapshot?.derivationVersion ?: WATCH_ACTIVITY_PROJECTION_VERSION,
        )
        val manifest = runCatching {
            HealthProjectionManifest(
                aggregate = HealthSyncAggregate.WorkoutActivity(activity.id),
                children = children,
                origin = origin,
            )
        }.getOrNull() ?: return PreparedActivityOperations.Failed(
            error = SafeSyncError.INVALID_LOCAL_DATA,
            reason = "Manifesto local de projeção inválido.",
        )
        val clientRecordVersion = maxOf(
            activity.recordedAt.toEpochMilli(),
            activity.reviewedAt?.toEpochMilli() ?: Long.MIN_VALUE,
        )
        val operations = mutableListOf<HealthSyncOperation>()
        for (child in manifest.children) {
            val key = HealthSyncOperationKey(
                aggregate = manifest.aggregate,
                recordType = recordTypeFor(child),
                projectionChildKey = child,
            )
            val existing = existingOperations[child]
            val frozenCandidate = if (child in eligibleChildren) {
                when (val result = healthRecordMapper.freezeWorkoutProjection(key, activity)) {
                    is HealthSyncFrozenPayloadResult.Success -> result.value
                    is HealthSyncFrozenPayloadResult.Failure -> {
                        return PreparedActivityOperations.Failed(result.error, result.reason)
                    }
                }
            } else {
                // Filhos que deixaram de ser elegíveis permanecem preservados
                // para reconciliação explícita em 014-14.
                null
            }
            if (existing != null && frozenCandidate == null) {
                operations += existing
                continue
            }
            val frozen = frozenCandidate ?: return PreparedActivityOperations.Failed(
                error = SafeSyncError.INVALID_LOCAL_DATA,
                reason = "Filho elegível sem payload de projeção congelável.",
            )
            val canReuseExisting = existing != null &&
                existing.key == key &&
                existing.action == HealthSyncOperationAction.INSERT &&
                existing.frozenPayload == frozen &&
                existing.projectionOrigin == origin
            if (canReuseExisting) {
                operations += existing
                continue
            }
            when (
                val prepared = healthSyncRepository.prepareProjectionChild(
                    key = key,
                    action = HealthSyncOperationAction.INSERT,
                    frozenPayload = frozen,
                    clientRecordVersion = clientRecordVersion,
                    projectionOrigin = origin,
                )
            ) {
                is RepositoryResult.Success -> operations += prepared.value
                else -> return PreparedActivityOperations.Failed(
                    error = SafeSyncError.INTERNAL,
                    reason = "Não foi possível preparar filho congelado da atividade.",
                )
            }
        }

        val expectedChildren = eligibleChildren.toSet()
        val processable = operations.filter { operation ->
            operation.action == HealthSyncOperationAction.INSERT &&
                operation.key.projectionChildKey in expectedChildren &&
                operation.isPending
        }
        if (processable.isNotEmpty()) {
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
            // Projeções filhas são o manifesto efetivo da atividade. Permissão
            // ausente deve aparecer como FAILED, nunca ser removida da decisão.
            storedOperations
        } else {
            // Mantém política legada de complementos opcionais para atividades
            // phone, que só entram na fila quando a permissão foi concedida.
            storedOperations.filter { permissionFor(it.key.recordType) in grantedPermissions }
        }
        if (operations.isEmpty()) {
            workoutActivityRepository.markSyncFailed(activityId)
            return
        }
        val activity = when (val result = workoutActivityRepository.findById(activityId)) {
            is RepositoryResult.Success -> result.value
            else -> return
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
        if (operations.all { it.state == SyncOperationState.SYNCED }) {
            workoutActivityRepository.markSynced(activityId)
        } else {
            workoutActivityRepository.markSyncFailed(activityId)
        }
    }
}

private suspend fun filterProcessableOperations(
    operations: List<HealthSyncOperation>,
    workoutActivityRepository: WorkoutActivityRepository,
    healthRecordMapper: HealthRecordMapper,
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
        val expectedChildren = expectedChildrenByActivity.getOrPut(aggregate.activityId) {
            healthRecordMapper.findEligibleWorkoutProjectionChildren(activity).toSet()
        }
        if (operation.action == HealthSyncOperationAction.INSERT && child in expectedChildren) {
            filtered += operation
        }
    }
    return filtered
}

private sealed interface PreparedActivityOperations {
    data class Success(
        val operations: List<HealthSyncOperation>,
    ) : PreparedActivityOperations

    data class PermissionRequired(
        val missingPermissions: Set<HealthConnectWritePermission>,
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

private fun Throwable?.toSafeSyncError(): SafeSyncError = when (this) {
    is SecurityException -> SafeSyncError.PERMISSION_REQUIRED
    is IllegalArgumentException -> SafeSyncError.INVALID_LOCAL_DATA
    is IllegalStateException -> SafeSyncError.RETRY_LATER
    null -> SafeSyncError.INTERNAL
    else -> SafeSyncError.WRITE_REJECTED
}

private fun HealthSyncOperation.failure(error: SafeSyncError, reason: String) = HealthConnectSyncFailure(
    operationId = id,
    recordType = key.recordType,
    error = error,
    reason = reason,
)

private suspend fun HealthSyncOperation.isBlockedByKnownLegacyDistance(
    healthSyncRepository: HealthSyncRepository,
    workoutActivityRepository: WorkoutActivityRepository,
    cache: MutableMap<WorkoutActivityId, Boolean>,
): Boolean {
    if (key.recordType !in setOf(HealthSyncRecordType.DISTANCE, HealthSyncRecordType.SPEED)) return false
    val aggregate = key.aggregate as? HealthSyncAggregate.WorkoutActivity ?: return false
    val activityId = aggregate.activityId
    cache[activityId]?.let { return it }
    val activityOrigin = when (val result = workoutActivityRepository.findById(activityId)) {
        is RepositoryResult.Success -> result.value.activityOrigin
        else -> return true
    }
    if (activityOrigin != WorkoutActivityOrigin.WATCH_D3_D4) {
        cache[activityId] = false
        return false
    }
    return when (val result = healthSyncRepository.hasKnownLegacyWorkoutDistanceProjection(activityId, activityOrigin)) {
        is RepositoryResult.Success -> {
            cache[activityId] = result.value
            result.value
        }
        else -> true
    }
}

private fun recordTypeFor(child: HealthProjectionChildKey): HealthSyncRecordType =
    HealthSyncRecordType.entries.first { it.name == child.recordTypeName }

private fun <T> RepositoryResult<T>.successOrNull(): T? = (this as? RepositoryResult.Success)?.value

private fun RepositoryResult<Int>.successOrZero(): Int = (this as? RepositoryResult.Success)?.value ?: 0

private fun permissionFor(recordType: HealthSyncRecordType): HealthConnectWritePermission = when (recordType) {
    HealthSyncRecordType.STEPS -> HealthConnectWritePermission.STEPS
    HealthSyncRecordType.HEART_RATE -> HealthConnectWritePermission.HEART_RATE
    HealthSyncRecordType.EXERCISE_SESSION -> HealthConnectWritePermission.EXERCISE
    HealthSyncRecordType.DISTANCE -> HealthConnectWritePermission.DISTANCE
    HealthSyncRecordType.SPEED -> HealthConnectWritePermission.SPEED
    HealthSyncRecordType.TOTAL_CALORIES_BURNED -> HealthConnectWritePermission.TOTAL_CALORIES_BURNED
    HealthSyncRecordType.WEIGHT -> HealthConnectWritePermission.WEIGHT
    HealthSyncRecordType.HEIGHT -> HealthConnectWritePermission.HEIGHT
}

private const val WATCH_ACTIVITY_PROJECTION_VERSION = "watch-activity-v1"
