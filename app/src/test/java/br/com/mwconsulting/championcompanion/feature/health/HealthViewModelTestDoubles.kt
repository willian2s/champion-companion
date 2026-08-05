package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectSyncCoordinator
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectSyncOutcome
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectSyncReport
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregate
import br.com.mwconsulting.championcompanion.core.model.HealthSyncBacklogReconciliationResult
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperation
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationId
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationKey
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRepository
import br.com.mwconsulting.championcompanion.core.model.HealthSyncSummary
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.SafeSyncError
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class FakeHealthSyncRepository : HealthSyncRepository {
    override suspend fun ensureOperation(
        key: HealthSyncOperationKey,
    ): RepositoryResult<HealthSyncOperation> = RepositoryResult.UnexpectedState

    override suspend fun findProcessable(
        limit: Int,
    ): RepositoryResult<List<HealthSyncOperation>> = RepositoryResult.Success(emptyList())

    override fun observeSyncSummary(): Flow<HealthSyncSummary> = flowOf(HealthSyncSummary())

    override suspend fun findOperationsForAggregate(
        aggregate: HealthSyncAggregate,
    ): RepositoryResult<List<HealthSyncOperation>> = RepositoryResult.Success(emptyList())

    override suspend fun markStarted(
        operationId: HealthSyncOperationId,
        at: Instant,
    ): RepositoryResult<Unit> = RepositoryResult.Success(Unit)

    override suspend fun markSynced(
        operationId: HealthSyncOperationId,
        at: Instant,
    ): RepositoryResult<Unit> = RepositoryResult.Success(Unit)

    override suspend fun markFailed(
        operationId: HealthSyncOperationId,
        error: SafeSyncError,
        at: Instant,
    ): RepositoryResult<Unit> = RepositoryResult.Success(Unit)

    override suspend fun resetInterrupted(at: Instant): RepositoryResult<Int> = RepositoryResult.Success(0)

    override suspend fun reconcileBacklog(): RepositoryResult<HealthSyncBacklogReconciliationResult> =
        RepositoryResult.Success(HealthSyncBacklogReconciliationResult())
}

internal object NoOpHealthConnectSyncCoordinator : HealthConnectSyncCoordinator {
    override suspend fun syncPending(limit: Int): HealthConnectSyncOutcome = nothingToSync(limit)

    override suspend fun syncActivity(
        activityId: WorkoutActivityId,
        limit: Int,
    ): HealthConnectSyncOutcome = nothingToSync(limit)

    private fun nothingToSync(limit: Int): HealthConnectSyncOutcome = HealthConnectSyncOutcome.NothingToSync(
        HealthConnectSyncReport(
            startedAt = Instant.EPOCH,
            finishedAt = Instant.EPOCH,
            limit = limit,
        ),
    )
}
