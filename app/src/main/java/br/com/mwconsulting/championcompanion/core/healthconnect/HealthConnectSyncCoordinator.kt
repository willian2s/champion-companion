package br.com.mwconsulting.championcompanion.core.healthconnect

import br.com.mwconsulting.championcompanion.core.common.IoDispatcher
import br.com.mwconsulting.championcompanion.core.model.AppPreferencesRepository
import br.com.mwconsulting.championcompanion.core.model.DailySummaryRepository
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregate
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperation
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationId
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationKey
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRepository
import br.com.mwconsulting.championcompanion.core.model.HeartRateRepository
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsRepository
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.SafeSyncError
import br.com.mwconsulting.championcompanion.core.model.SyncOperationState
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
    private val dailySummaryRepository: DailySummaryRepository,
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
                is RepositoryResult.Success -> result.value
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

        for (operation in operations) {
            val now = Instant.now()
            if (operation.key.aggregate is HealthSyncAggregate.WorkoutActivity) {
                touchedActivities += operation.key.aggregate.activityId
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

    private suspend fun loadAndMap(operation: HealthSyncOperation): HealthRecordMappingResult = when (val aggregate = operation.key.aggregate) {
        is HealthSyncAggregate.DailySummary -> when (val summary = dailySummaryRepository.findById(aggregate.summaryId)) {
            is RepositoryResult.Success -> healthRecordMapper.mapDailySummary(operation, summary.value)
            else -> HealthRecordMappingResult.Failure(SafeSyncError.INVALID_LOCAL_DATA, "Resumo diário não encontrado para sync.")
        }

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

        is HealthSyncAggregate.WorkoutActivity -> when (val activity = workoutActivityRepository.findById(aggregate.activityId)) {
            is RepositoryResult.Success -> healthRecordMapper.mapWorkoutActivity(operation, activity.value)
            else -> HealthRecordMappingResult.Failure(SafeSyncError.INVALID_LOCAL_DATA, "Atividade não encontrada para sync.")
        }
    }

    private suspend fun updateWorkoutWorkflowFromOperations(
        activityId: WorkoutActivityId,
        grantedPermissions: Set<HealthConnectWritePermission>,
    ) {
        val operations = when (val result = healthSyncRepository.findOperationsForAggregate(HealthSyncAggregate.WorkoutActivity(activityId))) {
            is RepositoryResult.Success -> result.value.filter { permissionFor(it.key.recordType) in grantedPermissions }
            else -> return
        }
        if (operations.isEmpty()) {
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

private fun <T> RepositoryResult<T>.successOrNull(): T? = (this as? RepositoryResult.Success)?.value

private fun RepositoryResult<Int>.successOrZero(): Int = (this as? RepositoryResult.Success)?.value ?: 0

private fun permissionFor(recordType: HealthSyncRecordType): HealthConnectWritePermission = when (recordType) {
    HealthSyncRecordType.STEPS -> HealthConnectWritePermission.STEPS
    HealthSyncRecordType.HEART_RATE -> HealthConnectWritePermission.HEART_RATE
    HealthSyncRecordType.EXERCISE_SESSION -> HealthConnectWritePermission.EXERCISE
    HealthSyncRecordType.DISTANCE -> HealthConnectWritePermission.DISTANCE
    HealthSyncRecordType.TOTAL_CALORIES_BURNED -> HealthConnectWritePermission.TOTAL_CALORIES_BURNED
    HealthSyncRecordType.WEIGHT -> HealthConnectWritePermission.WEIGHT
    HealthSyncRecordType.HEIGHT -> HealthConnectWritePermission.HEIGHT
}
