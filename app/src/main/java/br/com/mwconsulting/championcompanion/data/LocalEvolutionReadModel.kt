package br.com.mwconsulting.championcompanion.data

import br.com.mwconsulting.championcompanion.core.model.DomainValue
import br.com.mwconsulting.championcompanion.core.model.EVOLUTION_ELIGIBLE_WORKFLOW_STATES
import br.com.mwconsulting.championcompanion.core.model.EvolutionActivitySnapshot
import br.com.mwconsulting.championcompanion.core.model.EvolutionReadFailure
import br.com.mwconsulting.championcompanion.core.model.EvolutionReadModel
import br.com.mwconsulting.championcompanion.core.model.EvolutionReadSnapshot
import br.com.mwconsulting.championcompanion.core.model.EvolutionReadState
import br.com.mwconsulting.championcompanion.core.model.EvolutionQuery
import br.com.mwconsulting.championcompanion.core.model.ParserValidity
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordsReadSnapshot
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordsReadState
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityFilter
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityOrigin
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Projeção factual da evolução formada sobre atividades completas já persistidas.
 * Agregadores posteriores consomem somente este snapshot normalizado.
 */
@Singleton
class LocalEvolutionReadModel @Inject constructor(
    private val workoutActivityRepository: WorkoutActivityRepository,
) : EvolutionReadModel {

    override fun observeSnapshot(query: EvolutionQuery): Flow<EvolutionReadState> =
        workoutActivityRepository.observeActivities(
            filter = WorkoutActivityFilter.ALL,
        )
            .map { activities ->
                try {
                    activities.toEvolutionReadState(query)
                } catch (_: IllegalArgumentException) {
                    EvolutionReadState.Failed(
                        query = query,
                        reason = EvolutionReadFailure.INVALID_LOCAL_DATA,
                        retryable = false,
                    )
                }
            }
            .onStart { emit(EvolutionReadState.Loading(query)) }
            .catch { error ->
                if (error is CancellationException) throw error
                val reason = if (error is IllegalArgumentException) {
                    EvolutionReadFailure.INVALID_LOCAL_DATA
                } else {
                    EvolutionReadFailure.SOURCE_UNAVAILABLE
                }
                emit(
                    EvolutionReadState.Failed(
                        query = query,
                        reason = reason,
                        retryable = reason == EvolutionReadFailure.SOURCE_UNAVAILABLE,
                    ),
                )
            }

    override fun observePersonalRecordsSnapshot(): Flow<PersonalRecordsReadState> =
        workoutActivityRepository.observeActivitiesStrict(
            filter = WorkoutActivityFilter.ALL,
        )
            .map { result -> result.toPersonalRecordsReadState() }
            .onStart { emit(PersonalRecordsReadState.Loading) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(error.toPersonalRecordsReadFailure())
            }

    override suspend fun readPersonalRecordsSnapshot(): PersonalRecordsReadState = try {
        workoutActivityRepository.observeActivitiesStrict(
            filter = WorkoutActivityFilter.ALL,
        )
            .first()
            .toPersonalRecordsReadState()
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        error.toPersonalRecordsReadFailure()
    }

    private fun List<WorkoutActivity>.toEvolutionReadState(query: EvolutionQuery): EvolutionReadState {
        val activities = selectEvolutionPopulation(query)

        return if (activities.isEmpty()) {
            EvolutionReadState.Empty(query)
        } else {
            EvolutionReadState.Ready(
                EvolutionReadSnapshot(
                    query = query,
                    activities = activities,
                ),
            )
        }
    }

    private fun List<WorkoutActivity>.toPersonalRecordsReadState(): PersonalRecordsReadState {
        val activities = selectPersonalRecordsPopulation()

        return if (activities.isEmpty()) {
            PersonalRecordsReadState.Empty
        } else {
            PersonalRecordsReadState.Ready(
                PersonalRecordsReadSnapshot(activities),
            )
        }
    }

    private fun RepositoryResult<List<WorkoutActivity>>.toPersonalRecordsReadState(): PersonalRecordsReadState = when (this) {
        is RepositoryResult.Success -> try {
            value.toPersonalRecordsReadState()
        } catch (_: IllegalArgumentException) {
            PersonalRecordsReadState.Failed(
                reason = EvolutionReadFailure.INVALID_LOCAL_DATA,
                retryable = false,
            )
        }

        RepositoryResult.StorageUnavailable -> PersonalRecordsReadState.Failed(
            reason = EvolutionReadFailure.SOURCE_UNAVAILABLE,
            retryable = true,
        )

        RepositoryResult.Conflict,
        RepositoryResult.NotFound,
        RepositoryResult.UnexpectedState,
        -> PersonalRecordsReadState.Failed(
            reason = EvolutionReadFailure.INVALID_LOCAL_DATA,
            retryable = false,
        )
    }

    /**
     * Seleciona e ordena população uma vez, antes de qualquer agregação futura.
     * IDs repetidos não são deduplicados: o contrato do snapshot os trata como
     * dado local inválido, evitando descartar silenciosamente uma atividade.
     */
    private fun List<WorkoutActivity>.selectEvolutionPopulation(
        query: EvolutionQuery,
    ): List<EvolutionActivitySnapshot> = filter { activity ->
        activity.isEligibleForEvolution(query)
    }.toEvolutionActivitySnapshots()

    private fun List<WorkoutActivity>.selectPersonalRecordsPopulation(): List<EvolutionActivitySnapshot> =
        filter(WorkoutActivity::isEligibleForEvolution)
            .toEvolutionActivitySnapshots()

    private fun List<WorkoutActivity>.toEvolutionActivitySnapshots(): List<EvolutionActivitySnapshot> = asSequence()
        .mapNotNull(WorkoutActivity::toEvolutionActivitySnapshot)
        .sortedWith(EVOLUTION_ACTIVITY_ORDER)
        .toList()
}

private val EVOLUTION_ACTIVITY_ORDER: Comparator<EvolutionActivitySnapshot> =
    compareBy<EvolutionActivitySnapshot> { it.startedAt }
        .thenBy { it.activityId.value.toString() }

private fun WorkoutActivity.isEligibleForEvolution(): Boolean {
    val type = confirmedType ?: return false
    return activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4 &&
        type != WorkoutType.UNKNOWN &&
        importParserValidity in setOf(ParserValidity.VALID, ParserValidity.PARTIAL) &&
        workflowState in EVOLUTION_ELIGIBLE_WORKFLOW_STATES
}

private fun WorkoutActivity.isEligibleForEvolution(query: EvolutionQuery): Boolean {
    val type = confirmedType ?: return false
    return isEligibleForEvolution() && metrics.startedAt.isIn(query.currentRange) &&
        query.filter.accepts(type)
}

private fun WorkoutActivity.toEvolutionActivitySnapshot(): EvolutionActivitySnapshot? {
    val type = confirmedType ?: return null
    val duration = metrics.duration.takeIf { !it.isNegative && !it.isZero }
    val canonicalSeries = derivationSnapshot
        ?.takeIf { it.canonicalSeriesEligible }
        ?.canonicalSeries
    val validHeartRateValues = heartRateSamples
        .asSequence()
        .filter { sample ->
            !sample.sampledAt.isBefore(metrics.startedAt) && sample.sampledAt.isBefore(metrics.endedAt)
        }
        .map { it.bpm }
        .filter { it in 30..220 }
        .toList()
    val cadenceFacts = if (type == WorkoutType.CYCLING) {
        null
    } else {
        canonicalSeries?.cumulativeMetrics?.let { cumulative ->
            cumulative.cumulativeSteps.takeIf { it >= 0L }?.let { steps ->
                steps to cumulative.coveredDuration.takeIf { it.isPositive() }
            }
        }?.takeIf { it.second != null }
    }
    return EvolutionActivitySnapshot(
        activityId = id,
        workoutType = type,
        startedAt = metrics.startedAt,
        duration = duration,
        distanceMeters = finalDistanceMeters(type),
        steps = performanceSteps(type),
        averageBpm = validHeartRateValues.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        averageBpmSampleCount = validHeartRateValues.size.takeIf { it > 0 },
        averageBpmSampleSum = validHeartRateValues.takeIf { it.isNotEmpty() }
            ?.let { values -> values.checkedBpmSampleSum() },
        cadenceStepsPerMinute = if (type == WorkoutType.CYCLING) {
            null
        } else {
            derivationSnapshot?.canonicalSeries?.cumulativeMetrics?.cadenceStepsPerMinute
                ?.takeIf { it.isFinite() && it >= 0.0 }
        },
        paceSecondsPerKilometer = when (type) {
            WorkoutType.RUNNING,
            WorkoutType.WALKING,
            -> derivationSnapshot?.canonicalSeries?.cumulativeMetrics?.paceSecondsPerKilometer
                ?.takeIf { it.isFinite() && it > 0.0 }

            WorkoutType.TREADMILL -> metrics.calibratedMetrics?.averagePaceSecondsPerKilometer
                ?.positiveDomainValueOrNull()

            WorkoutType.CYCLING,
            WorkoutType.FREE_WORKOUT,
            WorkoutType.UNKNOWN,
            -> null
        },
        speedMetersPerSecond = when (type) {
            WorkoutType.RUNNING,
            WorkoutType.WALKING,
            -> derivationSnapshot?.canonicalSeries?.cumulativeMetrics?.speedMetersPerSecond
                ?.takeIf { it.isFinite() && it > 0.0 }

            WorkoutType.TREADMILL -> metrics.calibratedMetrics?.averageSpeedMetersPerSecond
                ?.positiveDomainValueOrNull()

            WorkoutType.CYCLING,
            WorkoutType.FREE_WORKOUT,
            WorkoutType.UNKNOWN,
            -> null
        },
        cadenceSteps = cadenceFacts?.first,
        cadenceDuration = cadenceFacts?.second,
        paceSpeedDuration = when (type) {
            WorkoutType.RUNNING,
            WorkoutType.WALKING,
            -> canonicalSeries?.coveredDuration?.takeIf { it.isPositive() }

            WorkoutType.TREADMILL -> duration
            WorkoutType.CYCLING,
            WorkoutType.FREE_WORKOUT,
            WorkoutType.UNKNOWN,
            -> null
        },
    )
}

private fun WorkoutActivity.finalDistanceMeters(type: WorkoutType): Double? = when (type) {
    WorkoutType.RUNNING,
    WorkoutType.WALKING,
    -> derivationSnapshot?.canonicalDistanceMeters?.positiveFiniteOrNull()

    WorkoutType.TREADMILL -> metrics.distances.calibratedMeters.positiveDomainValueOrNull()
    WorkoutType.CYCLING -> metrics.distances.manualDistanceMeters.positiveDomainValueOrNull()
    WorkoutType.FREE_WORKOUT -> derivationSnapshot?.canonicalDistanceMeters?.positiveFiniteOrNull()
    WorkoutType.UNKNOWN,
    -> null
}

private fun WorkoutActivity.performanceSteps(type: WorkoutType): Long? =
    if (type != WorkoutType.CYCLING && metrics.stepsAvailable && metrics.steps > 0L) {
        metrics.steps
    } else {
        null
    }

private fun DomainValue<Double>.positiveDomainValueOrNull(): Double? =
    (this as? DomainValue.Present)?.value?.positiveFiniteOrNull()

private fun Double.positiveFiniteOrNull(): Double? = takeIf { isFinite() && it > 0.0 }

private fun List<Int>.checkedBpmSampleSum(): Long {
    var total = 0L
    for (bpm in this) {
        total = try {
            Math.addExact(total, bpm.toLong())
        } catch (_: ArithmeticException) {
            throw IllegalArgumentException("Soma de amostras de BPM excede limite numérico")
        }
    }
    return total
}

private fun Throwable.toPersonalRecordsReadFailure(): PersonalRecordsReadState.Failed {
    val reason = if (this is IllegalArgumentException) {
        EvolutionReadFailure.INVALID_LOCAL_DATA
    } else {
        EvolutionReadFailure.SOURCE_UNAVAILABLE
    }
    return PersonalRecordsReadState.Failed(
        reason = reason,
        retryable = reason == EvolutionReadFailure.SOURCE_UNAVAILABLE,
    )
}

private fun java.time.Instant.isIn(range: br.com.mwconsulting.championcompanion.core.model.CalendarInstantRange): Boolean =
    !isBefore(range.fromInclusive) && isBefore(range.untilExclusive)
