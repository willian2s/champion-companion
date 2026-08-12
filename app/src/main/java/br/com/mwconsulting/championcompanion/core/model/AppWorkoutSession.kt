package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant

enum class AppWorkoutSessionState {
    CREATED,
    ACTIVE,
    PAUSED,
    AWAITING_RECOVERY,
    FINISHING,
    FINISHED,
    DISCARDED,
}

data class AppWorkoutSessionAggregates(
    val steps: Long,
    val distanceMeters: DomainValue<Double>,
    val caloriesKilocalories: DomainValue<Double>,
    val averageBpm: DomainValue<Int>,
    val minimumBpm: DomainValue<Int>,
    val maximumBpm: DomainValue<Int>,
    val stepsAvailable: Boolean = true,
) {
    companion object {
        fun empty() = AppWorkoutSessionAggregates(
            steps = 0,
            distanceMeters = DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED),
            caloriesKilocalories = DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED),
            averageBpm = DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED),
            minimumBpm = DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED),
            maximumBpm = DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED),
            stepsAvailable = false,
        )
    }
}

/**
 * Produz instante de auditoria representável pelo Room sem usar relógio civil
 * para medir duração. O piso temporal deriva somente dos acumulados monotônicos.
 */
fun AppWorkoutSession.nextAuditInstant(
    now: Instant,
    nextActiveDuration: Duration = activeDuration,
    nextPausedDuration: Duration = pausedDuration,
): Instant {
    val durationFloor = startedAt?.let { started ->
        runCatching { started.plus(nextActiveDuration).plus(nextPausedDuration) }
            .getOrNull()
            ?.ceilToEpochMilliOrNull()
    }
    return listOfNotNull(
        now.ceilToEpochMilliOrNull(),
        lastTransitionAt,
        durationFloor,
    ).maxOrNull() ?: lastTransitionAt
}

private fun Instant.ceilToEpochMilliOrNull(): Instant? = runCatching {
    val epochMillis = toEpochMilli()
    val truncated = Instant.ofEpochMilli(epochMillis)
    if (truncated == this) truncated else Instant.ofEpochMilli(Math.addExact(epochMillis, 1L))
}.getOrNull()

/** Estado local persistível; runtime Android e callbacks permanecem fora do domínio. */
@ConsistentCopyVisibility
data class AppWorkoutSession private constructor(
    val id: AppWorkoutSessionId,
    val type: WorkoutType,
    val createdAt: Instant,
    /** Último instante persistido da máquina de estados/checkpoint. */
    val lastTransitionAt: Instant,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val activeDuration: Duration,
    val pausedDuration: Duration,
    val activeSince: Instant?,
    val pausedSince: Instant?,
    val state: AppWorkoutSessionState,
    val aggregates: AppWorkoutSessionAggregates,
    val workoutActivityId: WorkoutActivityId?,
    val associatedDeviceId: DeviceId?,
    /** Revisão usada como compare-and-set pela persistência. */
    val revision: Long,
    /** Geração muda em cada novo runtime; callbacks de gerações antigas são rejeitados. */
    val runtimeGeneration: Long,
) {
    val recordsRoute: Boolean
        get() = type == WorkoutType.WALKING || type == WorkoutType.RUNNING

    fun start(at: Instant): DomainResult<AppWorkoutSession> =
        if (state == AppWorkoutSessionState.CREATED && isAtOrAfterLastTransition(at)) {
            DomainResult.Success(copy(
                lastTransitionAt = at,
                startedAt = at,
                activeSince = at,
                state = AppWorkoutSessionState.ACTIVE,
                runtimeGeneration = runtimeGeneration + 1,
                revision = revision + 1,
            ))
        } else {
            invalidState()
        }

    fun checkpoint(
        at: Instant,
        aggregates: AppWorkoutSessionAggregates,
    ): DomainResult<AppWorkoutSession> {
        if (state !in CHECKPOINTABLE_STATES || !areValid(aggregates) ||
            !areMonotonic(this.aggregates, aggregates) || !isAtOrAfterLastTransition(at)
        ) {
            return invalidState()
        }
        return when (state) {
            AppWorkoutSessionState.ACTIVE -> if (isAtOrAfter(at, activeSince)) {
                DomainResult.Success(copy(
                    activeDuration = activeDuration + Duration.between(activeSince!!, at),
                    lastTransitionAt = at,
                    activeSince = at,
                    aggregates = aggregates,
                    revision = revision + 1,
                ))
            } else {
                invalidTemporalOrder()
            }

            AppWorkoutSessionState.PAUSED -> if (isAtOrAfter(at, pausedSince)) {
                DomainResult.Success(copy(
                    pausedDuration = pausedDuration + Duration.between(pausedSince!!, at),
                    lastTransitionAt = at,
                    pausedSince = at,
                    aggregates = aggregates,
                    revision = revision + 1,
                ))
            } else {
                invalidTemporalOrder()
            }

            AppWorkoutSessionState.AWAITING_RECOVERY -> if (isAtOrAfter(at, startedAt)) {
                DomainResult.Success(copy(
                    lastTransitionAt = at,
                    aggregates = aggregates,
                    revision = revision + 1,
                ))
            } else {
                invalidTemporalOrder()
            }

            AppWorkoutSessionState.CREATED -> invalidState()

            AppWorkoutSessionState.FINISHING,
            AppWorkoutSessionState.FINISHED,
            AppWorkoutSessionState.DISCARDED,
            -> invalidState()
        }
    }

    /**
     * Checkpoint de runtime. Durações vêm de relógio monotônico; [at] serve
     * somente para auditoria e ordenação persistente.
     */
    fun checkpoint(
        at: Instant,
        activeDuration: Duration,
        pausedDuration: Duration,
        aggregates: AppWorkoutSessionAggregates,
    ): DomainResult<AppWorkoutSession> {
        if (state !in CHECKPOINTABLE_STATES || !areValid(aggregates) ||
            !areMonotonic(this.aggregates, aggregates) || !isAtOrAfterLastTransition(at) ||
            !hasValidRuntimeDurations(activeDuration, pausedDuration) ||
            !auditInstantContainsDurations(at, activeDuration, pausedDuration)
        ) {
            return invalidState()
        }
        return DomainResult.Success(copy(
            activeDuration = activeDuration,
            pausedDuration = pausedDuration,
            lastTransitionAt = at,
            activeSince = at.takeIf { state == AppWorkoutSessionState.ACTIVE },
            pausedSince = at.takeIf { state == AppWorkoutSessionState.PAUSED },
            aggregates = aggregates,
            revision = revision + 1,
        ))
    }

    /** Adapter de leitura legado; mutações runtime usam [checkpoint] com instante e revisão. */
    fun updateAggregates(aggregates: AppWorkoutSessionAggregates): DomainResult<AppWorkoutSession> =
        if ((state !in CHECKPOINTABLE_STATES && state != AppWorkoutSessionState.CREATED) || !areValid(aggregates) ||
            !areMonotonic(this.aggregates, aggregates)
        ) {
            invalidState()
        } else {
            DomainResult.Success(copy(aggregates = aggregates, revision = revision + 1))
        }

    fun pause(at: Instant): DomainResult<AppWorkoutSession> =
        if (state == AppWorkoutSessionState.ACTIVE && isAtOrAfter(at, activeSince) &&
            isAtOrAfterLastTransition(at)
        ) {
            DomainResult.Success(copy(
                lastTransitionAt = at,
                activeDuration = activeDuration + Duration.between(activeSince!!, at),
                activeSince = null,
                pausedSince = at,
                state = AppWorkoutSessionState.PAUSED,
                revision = revision + 1,
            ))
        } else {
            invalidState()
        }

    fun pause(
        at: Instant,
        activeDuration: Duration,
        pausedDuration: Duration,
    ): DomainResult<AppWorkoutSession> =
        if (state == AppWorkoutSessionState.ACTIVE && isAtOrAfterLastTransition(at) &&
            hasValidRuntimeDurations(activeDuration, pausedDuration) &&
            auditInstantContainsDurations(at, activeDuration, pausedDuration)
        ) {
            DomainResult.Success(copy(
                lastTransitionAt = at,
                activeDuration = activeDuration,
                pausedDuration = pausedDuration,
                activeSince = null,
                pausedSince = at,
                state = AppWorkoutSessionState.PAUSED,
                revision = revision + 1,
            ))
        } else {
            invalidState()
        }

    fun resume(at: Instant): DomainResult<AppWorkoutSession> =
        if (state == AppWorkoutSessionState.PAUSED && isAtOrAfter(at, pausedSince) &&
            isAtOrAfterLastTransition(at)
        ) {
            DomainResult.Success(copy(
                lastTransitionAt = at,
                pausedDuration = pausedDuration + Duration.between(pausedSince!!, at),
                pausedSince = null,
                activeSince = at,
                state = AppWorkoutSessionState.ACTIVE,
                runtimeGeneration = runtimeGeneration + 1,
                revision = revision + 1,
            ))
        } else {
            invalidState()
        }

    fun resume(
        at: Instant,
        activeDuration: Duration,
        pausedDuration: Duration,
    ): DomainResult<AppWorkoutSession> =
        if (state == AppWorkoutSessionState.PAUSED && isAtOrAfterLastTransition(at) &&
            hasValidRuntimeDurations(activeDuration, pausedDuration) &&
            auditInstantContainsDurations(at, activeDuration, pausedDuration)
        ) {
            DomainResult.Success(copy(
                lastTransitionAt = at,
                activeDuration = activeDuration,
                pausedDuration = pausedDuration,
                activeSince = at,
                pausedSince = null,
                state = AppWorkoutSessionState.ACTIVE,
                runtimeGeneration = runtimeGeneration + 1,
                revision = revision + 1,
            ))
        } else {
            invalidState()
        }

    fun interrupt(at: Instant): DomainResult<AppWorkoutSession> = when (state) {
        AppWorkoutSessionState.ACTIVE -> if (isAtOrAfter(at, activeSince) && isAtOrAfterLastTransition(at)) {
            DomainResult.Success(copy(
                lastTransitionAt = at,
                activeDuration = activeDuration + Duration.between(activeSince!!, at),
                activeSince = null,
                state = AppWorkoutSessionState.AWAITING_RECOVERY,
                revision = revision + 1,
            ))
        } else {
            invalidTemporalOrder()
        }

        AppWorkoutSessionState.PAUSED -> if (isAtOrAfter(at, pausedSince) && isAtOrAfterLastTransition(at)) {
            DomainResult.Success(copy(
                lastTransitionAt = at,
                pausedDuration = pausedDuration + Duration.between(pausedSince!!, at),
                pausedSince = null,
                state = AppWorkoutSessionState.AWAITING_RECOVERY,
                revision = revision + 1,
            ))
        } else {
            invalidTemporalOrder()
        }

        AppWorkoutSessionState.AWAITING_RECOVERY -> if (isAtOrAfterLastTransition(at)) {
            DomainResult.Success(this)
        } else {
            invalidTemporalOrder()
        }
        else -> invalidState()
    }

    fun interrupt(
        at: Instant,
        activeDuration: Duration,
        pausedDuration: Duration,
    ): DomainResult<AppWorkoutSession> = when {
        state == AppWorkoutSessionState.AWAITING_RECOVERY && isAtOrAfterLastTransition(at) ->
            DomainResult.Success(this)
        state !in setOf(AppWorkoutSessionState.ACTIVE, AppWorkoutSessionState.PAUSED) ||
            !isAtOrAfterLastTransition(at) ||
            !hasValidRuntimeDurations(activeDuration, pausedDuration) ||
            !auditInstantContainsDurations(at, activeDuration, pausedDuration) -> invalidState()
        else -> DomainResult.Success(copy(
            lastTransitionAt = at,
            activeDuration = activeDuration,
            pausedDuration = pausedDuration,
            activeSince = null,
            pausedSince = null,
            state = AppWorkoutSessionState.AWAITING_RECOVERY,
            revision = revision + 1,
        ))
    }

    fun recover(at: Instant): DomainResult<AppWorkoutSession> =
        if (state == AppWorkoutSessionState.AWAITING_RECOVERY && isAtOrAfter(at, startedAt) &&
            isAtOrAfterLastTransition(at)
        ) {
            DomainResult.Success(copy(
                lastTransitionAt = at,
                activeSince = at,
                state = AppWorkoutSessionState.ACTIVE,
                runtimeGeneration = runtimeGeneration + 1,
                revision = revision + 1,
            ))
        } else {
            invalidState()
        }

    fun beginFinishing(at: Instant): DomainResult<AppWorkoutSession> {
        if (state == AppWorkoutSessionState.FINISHING) {
            return if (isAtOrAfterLastTransition(at)) DomainResult.Success(this) else invalidTemporalOrder()
        }
        if (state !in FINISHABLE_STATES || !isAtOrAfter(at, startedAt)) return invalidState()
        if (!isAtOrAfterLastTransition(at)) return invalidTemporalOrder()
        if (state == AppWorkoutSessionState.ACTIVE && !isAtOrAfter(at, activeSince)) {
            return invalidTemporalOrder()
        }
        if (state == AppWorkoutSessionState.PAUSED && !isAtOrAfter(at, pausedSince)) {
            return invalidTemporalOrder()
        }

        val finalActiveDuration = if (state == AppWorkoutSessionState.ACTIVE) {
            activeDuration + Duration.between(activeSince!!, at)
        } else {
            activeDuration
        }
        val finalPausedDuration = if (state == AppWorkoutSessionState.PAUSED) {
            pausedDuration + Duration.between(pausedSince!!, at)
        } else {
            pausedDuration
        }
        return DomainResult.Success(copy(
            lastTransitionAt = at,
            endedAt = at,
            activeDuration = finalActiveDuration,
            pausedDuration = finalPausedDuration,
            activeSince = null,
            pausedSince = null,
            state = AppWorkoutSessionState.FINISHING,
            revision = revision + 1,
        ))
    }

    fun beginFinishing(
        at: Instant,
        activeDuration: Duration,
        pausedDuration: Duration,
    ): DomainResult<AppWorkoutSession> {
        if (state == AppWorkoutSessionState.FINISHING) {
            return if (isAtOrAfterLastTransition(at)) DomainResult.Success(this) else invalidTemporalOrder()
        }
        if (state !in FINISHABLE_STATES || !isAtOrAfterLastTransition(at) ||
            !hasValidRuntimeDurations(activeDuration, pausedDuration) ||
            !auditInstantContainsDurations(at, activeDuration, pausedDuration)
        ) {
            return invalidState()
        }
        return DomainResult.Success(copy(
            lastTransitionAt = at,
            endedAt = at,
            activeDuration = activeDuration,
            pausedDuration = pausedDuration,
            activeSince = null,
            pausedSince = null,
            state = AppWorkoutSessionState.FINISHING,
            revision = revision + 1,
        ))
    }

    fun associateActivity(activityId: WorkoutActivityId): DomainResult<AppWorkoutSession> = when {
        state !in setOf(AppWorkoutSessionState.FINISHING, AppWorkoutSessionState.FINISHED) -> invalidState()
        workoutActivityId == activityId -> DomainResult.Success(this)
        workoutActivityId != null -> DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
        else -> DomainResult.Success(copy(workoutActivityId = activityId, revision = revision + 1))
    }

    fun finish(at: Instant): DomainResult<AppWorkoutSession> = when {
        state == AppWorkoutSessionState.FINISHED && workoutActivityId != null &&
            isAtOrAfter(at, endedAt) && isAtOrAfterLastTransition(at) ->
            DomainResult.Success(this)
        state != AppWorkoutSessionState.FINISHING || workoutActivityId == null ||
            !isAtOrAfter(at, endedAt) -> invalidState()
        !isAtOrAfterLastTransition(at) -> invalidTemporalOrder()
        else -> DomainResult.Success(copy(
            lastTransitionAt = at,
            state = AppWorkoutSessionState.FINISHED,
            revision = revision + 1,
        ))
    }

    /** Operação composta mantida para consumidores existentes; retries com mesma atividade são idempotentes. */
    fun finish(activityId: WorkoutActivityId, at: Instant): DomainResult<AppWorkoutSession> {
        if (state == AppWorkoutSessionState.FINISHED) {
            return if (workoutActivityId == activityId && isAtOrAfterLastTransition(at)) {
                DomainResult.Success(this)
            } else {
                invalidState()
            }
        }
        val finishing = when (val result = beginFinishing(at)) {
            is DomainResult.Success -> result.value
            is DomainResult.Failure -> return result
        }
        val associated = when (val result = finishing.associateActivity(activityId)) {
            is DomainResult.Success -> result.value
            is DomainResult.Failure -> return result
        }
        return associated.finish(at)
    }

    fun discard(at: Instant): DomainResult<AppWorkoutSession> {
        if (state in TERMINAL_STATES || workoutActivityId != null || at.isBefore(createdAt)) return invalidState()
        if (!isAtOrAfterLastTransition(at)) return invalidTemporalOrder()
        if (startedAt != null && at.isBefore(startedAt)) return invalidTemporalOrder()

        val finalActiveDuration = if (state == AppWorkoutSessionState.ACTIVE) {
            if (!isAtOrAfter(at, activeSince)) return invalidTemporalOrder()
            activeDuration + Duration.between(activeSince!!, at)
        } else {
            activeDuration
        }
        val finalPausedDuration = if (state == AppWorkoutSessionState.PAUSED) {
            if (!isAtOrAfter(at, pausedSince)) return invalidTemporalOrder()
            pausedDuration + Duration.between(pausedSince!!, at)
        } else {
            pausedDuration
        }
        return DomainResult.Success(copy(
            lastTransitionAt = at,
            endedAt = at,
            activeDuration = finalActiveDuration,
            pausedDuration = finalPausedDuration,
            activeSince = null,
            pausedSince = null,
            state = AppWorkoutSessionState.DISCARDED,
            revision = revision + 1,
        ))
    }

    /** Compatibilidade com consumidores antigos sem relógio explícito. */
    fun discard(): DomainResult<AppWorkoutSession> = discard(
        at = listOfNotNull(
            activeSince,
            pausedSince,
            lastTransitionAt,
            startedAt?.plus(activeDuration)?.plus(pausedDuration),
        ).maxOrNull() ?: createdAt,
    )

    companion object {
        fun create(
            id: AppWorkoutSessionId,
            type: WorkoutType,
            createdAt: Instant,
            associatedDeviceId: DeviceId? = null,
        ): DomainResult<AppWorkoutSession> =
            if (PhoneWorkoutPolicy.blocksNewSession()) {
                invalidState()
            } else if (type == WorkoutType.UNKNOWN || type == WorkoutType.CYCLING) {
                invalidState()
            } else {
                DomainResult.Success(AppWorkoutSession(
                    id = id,
                    type = type,
                    createdAt = createdAt,
                    lastTransitionAt = createdAt,
                    startedAt = null,
                    endedAt = null,
                    activeDuration = Duration.ZERO,
                    pausedDuration = Duration.ZERO,
                    activeSince = null,
                    pausedSince = null,
                    state = AppWorkoutSessionState.CREATED,
                    aggregates = AppWorkoutSessionAggregates.empty(),
                    workoutActivityId = null,
                    associatedDeviceId = associatedDeviceId,
                    revision = 0,
                    runtimeGeneration = 0,
                ))
            }

        @Suppress("LongParameterList")
        fun restore(
            id: AppWorkoutSessionId,
            type: WorkoutType,
            createdAt: Instant,
            startedAt: Instant?,
            endedAt: Instant?,
            activeDuration: Duration,
            pausedDuration: Duration,
            activeSince: Instant?,
            pausedSince: Instant?,
            state: AppWorkoutSessionState,
            aggregates: AppWorkoutSessionAggregates,
            workoutActivityId: WorkoutActivityId?,
            associatedDeviceId: DeviceId?,
            revision: Long,
            runtimeGeneration: Long,
            lastTransitionAt: Instant? = null,
        ): DomainResult<AppWorkoutSession> {
            val resolvedLastTransitionAt = lastTransitionAt ?: inferLastTransitionAt(
                createdAt = createdAt,
                startedAt = startedAt,
                endedAt = endedAt,
                activeDuration = activeDuration,
                pausedDuration = pausedDuration,
                activeSince = activeSince,
                pausedSince = pausedSince,
            )
            val candidate = AppWorkoutSession(
                id, type, createdAt, resolvedLastTransitionAt, startedAt, endedAt, activeDuration, pausedDuration,
                activeSince, pausedSince, state, aggregates, workoutActivityId,
                associatedDeviceId, revision, runtimeGeneration,
            )
            return if (candidate.hasValidPersistedState()) {
                DomainResult.Success(candidate)
            } else {
                DomainResult.Failure(DomainValidationError.INVALID_STATE)
            }
        }

        private val CHECKPOINTABLE_STATES = setOf(
            AppWorkoutSessionState.ACTIVE,
            AppWorkoutSessionState.PAUSED,
            AppWorkoutSessionState.AWAITING_RECOVERY,
        )
        private val FINISHABLE_STATES = setOf(
            AppWorkoutSessionState.ACTIVE,
            AppWorkoutSessionState.PAUSED,
            AppWorkoutSessionState.AWAITING_RECOVERY,
        )
        private val TERMINAL_STATES = setOf(AppWorkoutSessionState.FINISHED, AppWorkoutSessionState.DISCARDED)

        private fun isAtOrAfter(at: Instant, reference: Instant?): Boolean =
            reference != null && !at.isBefore(reference)

        private fun AppWorkoutSession.isAtOrAfterLastTransition(at: Instant): Boolean =
            !at.isBefore(lastTransitionAt)

        private fun inferLastTransitionAt(
            createdAt: Instant,
            startedAt: Instant?,
            endedAt: Instant?,
            activeDuration: Duration,
            pausedDuration: Duration,
            activeSince: Instant?,
            pausedSince: Instant?,
        ): Instant = listOfNotNull(
            createdAt,
            startedAt,
            endedAt,
            activeSince,
            pausedSince,
            startedAt?.plus(activeDuration)?.plus(pausedDuration),
        ).maxOrNull() ?: createdAt

        private fun areValid(aggregates: AppWorkoutSessionAggregates): Boolean {
            if (aggregates.steps < 0 || (!aggregates.stepsAvailable && aggregates.steps != 0L) ||
                !isNonNegative(aggregates.distanceMeters) ||
                !isNonNegative(aggregates.caloriesKilocalories) ||
                !isValidBpm(aggregates.averageBpm) || !isValidBpm(aggregates.minimumBpm) ||
                !isValidBpm(aggregates.maximumBpm)
            ) return false
            val average = aggregates.averageBpm.presentValue()
            val minimum = aggregates.minimumBpm.presentValue()
            val maximum = aggregates.maximumBpm.presentValue()
            return (minimum == null || average == null || minimum <= average) &&
                (average == null || maximum == null || average <= maximum) &&
                (minimum == null || maximum == null || minimum <= maximum)
        }

        private fun areMonotonic(
            previous: AppWorkoutSessionAggregates,
            next: AppWorkoutSessionAggregates,
        ): Boolean = (!previous.stepsAvailable || next.stepsAvailable && next.steps >= previous.steps) &&
            isMonotonic(previous.distanceMeters, next.distanceMeters) &&
            isMonotonic(previous.caloriesKilocalories, next.caloriesKilocalories)

        private fun isMonotonic(previous: DomainValue<Double>, next: DomainValue<Double>): Boolean {
            val previousValue = previous.presentValue() ?: return true
            val nextValue = next.presentValue() ?: return false
            return nextValue >= previousValue
        }

        private fun isNonNegative(value: DomainValue<Double>): Boolean =
            value !is DomainValue.Present || (value.value.isFinite() && value.value >= 0.0)

        private fun isValidBpm(value: DomainValue<Int>): Boolean =
            value !is DomainValue.Present || value.value in 30..220

        private fun <T> DomainValue<T>.presentValue(): T? = (this as? DomainValue.Present)?.value

        private fun AppWorkoutSession.hasValidPersistedState(): Boolean {
            if (type == WorkoutType.UNKNOWN || type == WorkoutType.CYCLING || revision < 0 || runtimeGeneration < 0 ||
                lastTransitionAt.isBefore(createdAt) ||
                activeDuration.isNegative || pausedDuration.isNegative || !areValid(aggregates) ||
                (startedAt != null && startedAt.isBefore(createdAt)) ||
                (endedAt != null && endedAt.isBefore(startedAt ?: createdAt)) ||
                (startedAt != null && lastTransitionAt.isBefore(startedAt)) ||
                (endedAt != null && lastTransitionAt.isBefore(endedAt)) ||
                (activeSince != null && activeSince != lastTransitionAt) ||
                (pausedSince != null && pausedSince != lastTransitionAt) ||
                !hasTemporallyPossibleDurations()
            ) return false
            return when (state) {
                AppWorkoutSessionState.CREATED -> startedAt == null && endedAt == null &&
                    activeDuration.isZero && pausedDuration.isZero && activeSince == null &&
                    pausedSince == null && workoutActivityId == null
                AppWorkoutSessionState.ACTIVE -> startedAt != null && endedAt == null &&
                    isAtOrAfter(activeSince ?: return false, startedAt) && pausedSince == null && workoutActivityId == null
                AppWorkoutSessionState.PAUSED -> startedAt != null && endedAt == null &&
                    activeSince == null && isAtOrAfter(pausedSince ?: return false, startedAt) && workoutActivityId == null
                AppWorkoutSessionState.AWAITING_RECOVERY -> startedAt != null && endedAt == null &&
                    activeSince == null && pausedSince == null && workoutActivityId == null
                AppWorkoutSessionState.FINISHING -> startedAt != null && endedAt != null &&
                    activeSince == null && pausedSince == null
                AppWorkoutSessionState.FINISHED -> startedAt != null && endedAt != null &&
                    activeSince == null && pausedSince == null && workoutActivityId != null
                AppWorkoutSessionState.DISCARDED -> endedAt != null && activeSince == null &&
                    pausedSince == null && workoutActivityId == null &&
                    (startedAt != null || activeDuration.isZero && pausedDuration.isZero)
            }
        }

        private fun AppWorkoutSession.hasTemporallyPossibleDurations(): Boolean {
            val started = startedAt ?: return activeDuration.isZero && pausedDuration.isZero
            val durationLimit = endedAt ?: lastTransitionAt
            val elapsed = Duration.between(started, durationLimit)
            return !elapsed.isNegative && activeDuration.plus(pausedDuration) <= elapsed
        }

        private fun AppWorkoutSession.hasValidRuntimeDurations(
            nextActiveDuration: Duration,
            nextPausedDuration: Duration,
        ): Boolean {
            if (nextActiveDuration.isNegative || nextPausedDuration.isNegative ||
                nextActiveDuration < activeDuration || nextPausedDuration < pausedDuration
            ) return false
            return when (state) {
                AppWorkoutSessionState.ACTIVE -> nextPausedDuration == pausedDuration
                AppWorkoutSessionState.PAUSED -> nextActiveDuration == activeDuration
                AppWorkoutSessionState.AWAITING_RECOVERY ->
                    nextActiveDuration == activeDuration && nextPausedDuration == pausedDuration
                else -> false
            }
        }

        private fun AppWorkoutSession.auditInstantContainsDurations(
            at: Instant,
            nextActiveDuration: Duration,
            nextPausedDuration: Duration,
        ): Boolean {
            val started = startedAt ?: return false
            val minimumAuditInstant = runCatching {
                started.plus(nextActiveDuration).plus(nextPausedDuration)
            }.getOrNull() ?: return false
            return !at.isBefore(minimumAuditInstant)
        }

        private fun <T> invalidState(): DomainResult<T> = DomainResult.Failure(DomainValidationError.INVALID_STATE)
        private fun <T> invalidTemporalOrder(): DomainResult<T> =
            DomainResult.Failure(DomainValidationError.INVALID_TEMPORAL_ORDER)
    }
}
