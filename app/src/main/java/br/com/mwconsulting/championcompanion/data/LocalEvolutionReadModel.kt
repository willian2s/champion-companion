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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Projeção factual da evolução formada sobre atividades completas já persistidas.
 * Agregações e buckets pertencem às subtarefas seguintes.
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

    private fun List<WorkoutActivity>.toEvolutionReadState(query: EvolutionQuery): EvolutionReadState {
        val activities = asSequence()
            .filter { activity -> activity.isEligibleForEvolution(query) }
            .mapNotNull(WorkoutActivity::toEvolutionActivitySnapshot)
            .sortedWith(
                compareBy<EvolutionActivitySnapshot> { it.startedAt }
                    .thenBy { it.activityId.value.toString() },
            )
            .distinctBy(EvolutionActivitySnapshot::activityId)
            .toList()

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
}

private fun WorkoutActivity.isEligibleForEvolution(query: EvolutionQuery): Boolean {
    val type = confirmedType ?: return false
    return activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4 &&
        type != WorkoutType.UNKNOWN &&
        importParserValidity in setOf(ParserValidity.VALID, ParserValidity.PARTIAL) &&
        workflowState in EVOLUTION_ELIGIBLE_WORKFLOW_STATES &&
        metrics.startedAt.isIn(query.currentRange) &&
        query.filter.accepts(type)
}

private fun WorkoutActivity.toEvolutionActivitySnapshot(): EvolutionActivitySnapshot? {
    val type = confirmedType ?: return null
    val duration = metrics.duration.takeIf { !it.isNegative && !it.isZero }
    return EvolutionActivitySnapshot(
        activityId = id,
        workoutType = type,
        startedAt = metrics.startedAt,
        duration = duration,
        distanceMeters = finalDistanceMeters(type),
        steps = performanceSteps(type),
        averageBpm = (metrics.averageBpm as? DomainValue.Present)?.value
            ?.takeIf { it in 30..220 },
        averageBpmSampleCount = heartRateSamples.size.takeIf { it > 0 },
        cadenceStepsPerMinute = if (type == WorkoutType.CYCLING) {
            null
        } else {
            derivationSnapshot?.canonicalSeries?.cumulativeMetrics?.cadenceStepsPerMinute
                ?.takeIf { it.isFinite() && it > 0.0 }
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

private fun java.time.Instant.isIn(range: br.com.mwconsulting.championcompanion.core.model.CalendarInstantRange): Boolean =
    !isBefore(range.fromInclusive) && isBefore(range.untilExclusive)
