package br.com.mwconsulting.championcompanion.core.model

enum class WorkoutRouteHealthSyncState {
    NOT_APPLICABLE,
    PENDING_PERMISSION,
    READY,
    SYNCED,
    FAILED,
}

/**
 * Revisão local da mesma ExerciseSessionRecord. Rota nunca cria novo record type
 * nem nova identidade canônica.
 */
@ConsistentCopyVisibility
data class WorkoutExerciseSessionProjection private constructor(
    val activityId: WorkoutActivityId,
    val sessionId: AppWorkoutSessionId,
    val operationKey: HealthSyncOperationKey,
    val clientRecordId: String,
    val clientRecordVersion: Long,
    val routeRevision: Long?,
    val routeSyncState: WorkoutRouteHealthSyncState,
) {
    val includesRoute: Boolean get() = routeRevision != null

    fun nextUpsert(
        localRoute: WorkoutRoute?,
        nextClientRecordVersion: Long,
        routePermissionGranted: Boolean,
    ): DomainResult<WorkoutExerciseSessionProjection> {
        if (nextClientRecordVersion < clientRecordVersion) {
            return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        }
        if (routeRevision != null && localRoute == null) {
            return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }
        if (localRoute != null && (localRoute.sessionId != sessionId ||
                localRoute.state != WorkoutRouteState.FINALIZED ||
                localRoute.revision < (routeRevision ?: 0L))
        ) {
            return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }
        val localRouteRevision = localRoute?.revision
        val nextRouteRevision = localRouteRevision ?: routeRevision
        val routeChanged = localRouteRevision != null && localRouteRevision != routeRevision
        val routeBecomesVisible = routePermissionGranted && routeSyncState == WorkoutRouteHealthSyncState.PENDING_PERMISSION
        if (nextRouteRevision != null && !routePermissionGranted &&
            nextClientRecordVersion != clientRecordVersion
        ) {
            return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        }
        if ((routeChanged && routePermissionGranted || routeBecomesVisible) &&
            nextClientRecordVersion <= clientRecordVersion
        ) {
            return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        }
        val nextRouteState = when {
            nextRouteRevision == null -> WorkoutRouteHealthSyncState.NOT_APPLICABLE
            routeSyncState == WorkoutRouteHealthSyncState.SYNCED && !routeChanged &&
                nextClientRecordVersion == clientRecordVersion -> WorkoutRouteHealthSyncState.SYNCED
            !routePermissionGranted -> WorkoutRouteHealthSyncState.PENDING_PERMISSION
            routeSyncState == WorkoutRouteHealthSyncState.FAILED && !routeChanged &&
                nextClientRecordVersion == clientRecordVersion -> WorkoutRouteHealthSyncState.FAILED
            else -> WorkoutRouteHealthSyncState.READY
        }
        return DomainResult.Success(copy(
            clientRecordVersion = nextClientRecordVersion,
            routeRevision = nextRouteRevision,
            routeSyncState = nextRouteState,
        ))
    }

    fun markRouteSynced(): DomainResult<WorkoutExerciseSessionProjection> =
        if (includesRoute && routeSyncState == WorkoutRouteHealthSyncState.READY) {
            DomainResult.Success(copy(routeSyncState = WorkoutRouteHealthSyncState.SYNCED))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

    fun markRouteFailed(): DomainResult<WorkoutExerciseSessionProjection> =
        if (includesRoute && routeSyncState == WorkoutRouteHealthSyncState.READY) {
            DomainResult.Success(copy(routeSyncState = WorkoutRouteHealthSyncState.FAILED))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

    /** Retry remoto mantém identidade e versão quando conteúdo local não mudou. */
    fun retryRoute(routePermissionGranted: Boolean): DomainResult<WorkoutExerciseSessionProjection> = when {
        !includesRoute -> DomainResult.Failure(DomainValidationError.INVALID_STATE)
        routeSyncState == WorkoutRouteHealthSyncState.FAILED && routePermissionGranted ->
            DomainResult.Success(copy(routeSyncState = WorkoutRouteHealthSyncState.READY))
        routeSyncState == WorkoutRouteHealthSyncState.PENDING_PERMISSION && routePermissionGranted ->
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        else -> DomainResult.Failure(DomainValidationError.INVALID_STATE)
    }

    companion object {
        fun base(
            activityId: WorkoutActivityId,
            sessionId: AppWorkoutSessionId,
            clientRecordId: String,
            clientRecordVersion: Long,
        ): DomainResult<WorkoutExerciseSessionProjection> {
            val key = HealthSyncOperationKey(
                aggregate = HealthSyncAggregate.WorkoutActivity(activityId),
                recordType = HealthSyncRecordType.EXERCISE_SESSION,
            )
            return if (clientRecordId.isBlank() || clientRecordVersion < 0L) {
                DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
            } else {
                DomainResult.Success(WorkoutExerciseSessionProjection(
                    activityId = activityId,
                    sessionId = sessionId,
                    operationKey = key,
                    clientRecordId = clientRecordId,
                    clientRecordVersion = clientRecordVersion,
                    routeRevision = null,
                    routeSyncState = WorkoutRouteHealthSyncState.NOT_APPLICABLE,
                ))
            }
        }

        fun restore(
            activityId: WorkoutActivityId,
            sessionId: AppWorkoutSessionId,
            operationKey: HealthSyncOperationKey,
            clientRecordId: String,
            clientRecordVersion: Long,
            routeRevision: Long?,
            routeSyncState: WorkoutRouteHealthSyncState,
        ): DomainResult<WorkoutExerciseSessionProjection> = if (
            operationKey != HealthSyncOperationKey(
                HealthSyncAggregate.WorkoutActivity(activityId),
                HealthSyncRecordType.EXERCISE_SESSION,
            ) || clientRecordId.isBlank() || clientRecordVersion < 0L ||
            routeRevision?.let { it < 0L } == true ||
            (routeRevision == null) != (routeSyncState == WorkoutRouteHealthSyncState.NOT_APPLICABLE)
        ) {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        } else {
            DomainResult.Success(WorkoutExerciseSessionProjection(
                activityId, sessionId, operationKey, clientRecordId, clientRecordVersion,
                routeRevision, routeSyncState,
            ))
        }
    }
}
