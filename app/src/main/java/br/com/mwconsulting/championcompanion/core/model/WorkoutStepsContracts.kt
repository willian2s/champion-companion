package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** Identidade local do ledger de crédito de passos da atividade. */
@JvmInline
value class WorkoutStepsCreditId(val value: UUID)

/** Identidade remota congelada do filho [HealthProjectionChildKey.WorkoutSteps]. */
@ConsistentCopyVisibility
data class WorkoutStepsProjectionIdentity private constructor(
    val activityId: WorkoutActivityId,
    val operationId: HealthSyncOperationId,
    val clientRecordId: String,
    val clientRecordVersion: Long,
) {
    init {
        require(clientRecordId.isNotBlank())
        require(clientRecordVersion >= 0L)
    }

    val operationKey: HealthSyncOperationKey
        get() = HealthSyncOperationKey(
            aggregate = HealthSyncAggregate.WorkoutActivity(activityId),
            recordType = HealthSyncRecordType.STEPS,
            projectionChildKey = HealthProjectionChildKey.WorkoutSteps,
        )

    companion object {
        fun fromPersisted(
            activityId: WorkoutActivityId,
            operationId: HealthSyncOperationId,
            clientRecordId: String,
            clientRecordVersion: Long,
        ): DomainResult<WorkoutStepsProjectionIdentity> = if (
            clientRecordId.isNotBlank() && clientRecordVersion >= 0L
        ) {
            DomainResult.Success(
                WorkoutStepsProjectionIdentity(
                    activityId = activityId,
                    operationId = operationId,
                    clientRecordId = clientRecordId,
                    clientRecordVersion = clientRecordVersion,
                ),
            )
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

        fun fromOperation(operation: HealthSyncOperation): DomainResult<WorkoutStepsProjectionIdentity> {
            val aggregate = operation.key.aggregate as? HealthSyncAggregate.WorkoutActivity
                ?: return DomainResult.Failure(DomainValidationError.INVALID_STATE)
            if (
                operation.key != HealthSyncOperationKey(
                    aggregate = aggregate,
                    recordType = HealthSyncRecordType.STEPS,
                    projectionChildKey = HealthProjectionChildKey.WorkoutSteps,
                ) ||
                operation.clientRecordId.isNullOrBlank() ||
                operation.clientRecordVersion == null
            ) {
                return DomainResult.Failure(DomainValidationError.INVALID_STATE)
            }
            return DomainResult.Success(
                WorkoutStepsProjectionIdentity(
                    activityId = aggregate.activityId,
                    operationId = operation.id,
                    clientRecordId = operation.clientRecordId,
                    clientRecordVersion = operation.clientRecordVersion,
                ),
            )
        }
    }
}

/** Dados locais necessários para preparar o filho de passos sem efeito remoto. */
data class WorkoutStepsProjectionContext(
    val steps: Long,
    val window: WorkoutIntervalWindow,
) {
    init {
        require(steps > 0L)
    }
}

/** Estado único do ledger, cobrindo publicação e consumo sem contador global. */
enum class WorkoutStepsCreditState {
    PENDING,
    ACTIVE,
    CONSUMED,
    BLOCKED,
}

enum class WorkoutStepsPublicationState {
    NOT_PUBLISHED,
    PENDING,
    FAILED,
    ACCEPTED,
}

/** Motivo explícito para um crédito que não pode participar do Daily. */
enum class WorkoutStepsCreditBlockReason {
    ALREADY_COVERED,
    COVERAGE_UNKNOWN,
    INVALID_WINDOW,
    CROSSES_LOCAL_DATE,
    INVALID_SOURCE,
}

/** Crédito identificável por atividade/data e consumível no máximo uma vez. */
@ConsistentCopyVisibility
data class WorkoutStepsCredit private constructor(
    val id: WorkoutStepsCreditId,
    val activityId: WorkoutActivityId,
    val localDate: LocalDate,
    val steps: Long,
    val window: WorkoutIntervalWindow,
    val timeZoneId: ZoneId,
    val projectionIdentity: WorkoutStepsProjectionIdentity,
    val publicationState: WorkoutStepsPublicationState,
    val state: WorkoutStepsCreditState,
    val blockedReason: WorkoutStepsCreditBlockReason?,
    val consumedDailyOperationOrdinal: Long?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isActive: Boolean
        get() = state == WorkoutStepsCreditState.ACTIVE

    val operationKey: HealthSyncOperationKey
        get() = projectionIdentity.operationKey

    fun markActive(at: Instant): DomainResult<WorkoutStepsCredit> = when {
        state == WorkoutStepsCreditState.ACTIVE &&
            publicationState == WorkoutStepsPublicationState.ACCEPTED -> DomainResult.Success(this)
        state != WorkoutStepsCreditState.PENDING ||
            publicationState != WorkoutStepsPublicationState.ACCEPTED ||
            at.isBefore(updatedAt) ->
            DomainResult.Failure(DomainValidationError.INVALID_STATE)

        else -> DomainResult.Success(copy(
            publicationState = WorkoutStepsPublicationState.ACCEPTED,
            state = WorkoutStepsCreditState.ACTIVE,
            updatedAt = at,
        ))
    }

    /** Prepara retry sem transformar operação pendente em crédito diário. */
    fun prepareForRetry(at: Instant): DomainResult<WorkoutStepsCredit> = when {
        state == WorkoutStepsCreditState.PENDING && !at.isBefore(updatedAt) ->
            DomainResult.Success(copy(
                publicationState = WorkoutStepsPublicationState.NOT_PUBLISHED,
                updatedAt = at,
            ))

        state == WorkoutStepsCreditState.BLOCKED &&
            blockedReason != WorkoutStepsCreditBlockReason.ALREADY_COVERED &&
            !at.isBefore(updatedAt) ->
            DomainResult.Success(copy(
                publicationState = WorkoutStepsPublicationState.NOT_PUBLISHED,
                state = WorkoutStepsCreditState.PENDING,
                blockedReason = null,
                updatedAt = at,
            ))

        else -> DomainResult.Failure(DomainValidationError.INVALID_STATE)
    }

    /** Registra aceite local antes de ativar crédito na mesma transação. */
    fun markAccepted(at: Instant): DomainResult<WorkoutStepsCredit> = when {
        state == WorkoutStepsCreditState.PENDING && !at.isBefore(updatedAt) ->
            DomainResult.Success(copy(
                publicationState = WorkoutStepsPublicationState.ACCEPTED,
                updatedAt = at,
            ))

        state == WorkoutStepsCreditState.ACTIVE &&
            publicationState == WorkoutStepsPublicationState.ACCEPTED -> DomainResult.Success(this)

        state == WorkoutStepsCreditState.CONSUMED &&
            publicationState == WorkoutStepsPublicationState.ACCEPTED -> DomainResult.Success(this)

        else -> DomainResult.Failure(DomainValidationError.INVALID_STATE)
    }

    fun consume(dailyOperationOrdinal: Long, at: Instant): DomainResult<WorkoutStepsCredit> = when {
        state == WorkoutStepsCreditState.CONSUMED &&
            consumedDailyOperationOrdinal == dailyOperationOrdinal -> DomainResult.Success(this)

        state != WorkoutStepsCreditState.ACTIVE || dailyOperationOrdinal < 0L || at.isBefore(updatedAt) ->
            DomainResult.Failure(DomainValidationError.INVALID_STATE)

        else -> DomainResult.Success(copy(
            state = WorkoutStepsCreditState.CONSUMED,
            consumedDailyOperationOrdinal = dailyOperationOrdinal,
            updatedAt = at,
        ))
    }

    fun block(
        reason: WorkoutStepsCreditBlockReason,
        at: Instant,
    ): DomainResult<WorkoutStepsCredit> = when {
        state == WorkoutStepsCreditState.BLOCKED && blockedReason == reason -> DomainResult.Success(this)
        state != WorkoutStepsCreditState.PENDING || at.isBefore(updatedAt) ->
            DomainResult.Failure(DomainValidationError.INVALID_STATE)

        else -> DomainResult.Success(copy(
            state = WorkoutStepsCreditState.BLOCKED,
            blockedReason = reason,
            updatedAt = at,
        ))
    }

    companion object {
        fun create(
            id: WorkoutStepsCreditId,
            activityId: WorkoutActivityId,
            localDate: LocalDate,
            steps: Long,
            window: WorkoutIntervalWindow,
            timeZoneId: ZoneId,
            projectionIdentity: WorkoutStepsProjectionIdentity,
            state: WorkoutStepsCreditState = WorkoutStepsCreditState.PENDING,
            blockedReason: WorkoutStepsCreditBlockReason? = null,
            consumedDailyOperationOrdinal: Long? = null,
            createdAt: Instant,
            updatedAt: Instant = createdAt,
            publicationState: WorkoutStepsPublicationState = WorkoutStepsPublicationState.NOT_PUBLISHED,
        ): DomainResult<WorkoutStepsCredit> {
            val windowEndInclusive = window.end.minusNanos(1L)
            val isContainedInLocalDate = window.start.atZone(timeZoneId).toLocalDate() == localDate &&
                windowEndInclusive.atZone(timeZoneId).toLocalDate() == localDate
            val validState = when (state) {
                WorkoutStepsCreditState.PENDING,
                WorkoutStepsCreditState.ACTIVE,
                -> blockedReason == null && consumedDailyOperationOrdinal == null

                WorkoutStepsCreditState.CONSUMED ->
                    blockedReason == null && consumedDailyOperationOrdinal != null &&
                        consumedDailyOperationOrdinal >= 0L

                WorkoutStepsCreditState.BLOCKED ->
                    blockedReason != null && consumedDailyOperationOrdinal == null
            }
            val validPublicationState = when (state) {
                WorkoutStepsCreditState.PENDING -> true
                WorkoutStepsCreditState.ACTIVE,
                WorkoutStepsCreditState.CONSUMED,
                -> publicationState == WorkoutStepsPublicationState.ACCEPTED

                WorkoutStepsCreditState.BLOCKED -> true
            }
            return if (
                projectionIdentity.activityId == activityId &&
                steps > 0L &&
                    isContainedInLocalDate &&
                    !updatedAt.isBefore(createdAt) &&
                    validState &&
                    validPublicationState
            ) {
                DomainResult.Success(
                    WorkoutStepsCredit(
                        id = id,
                        activityId = activityId,
                        localDate = localDate,
                        steps = steps,
                        window = window,
                        timeZoneId = timeZoneId,
                        projectionIdentity = projectionIdentity,
                        publicationState = publicationState,
                        state = state,
                        blockedReason = blockedReason,
                        consumedDailyOperationOrdinal = consumedDailyOperationOrdinal,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                    ),
                )
            } else {
                DomainResult.Failure(DomainValidationError.INVALID_STATE)
            }
        }
    }
}
