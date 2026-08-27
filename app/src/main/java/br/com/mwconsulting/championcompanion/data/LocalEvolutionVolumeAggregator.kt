package br.com.mwconsulting.championcompanion.data

import br.com.mwconsulting.championcompanion.core.model.EvolutionActivitySnapshot
import br.com.mwconsulting.championcompanion.core.model.EvolutionMetric
import br.com.mwconsulting.championcompanion.core.model.EvolutionMetricSeries
import br.com.mwconsulting.championcompanion.core.model.EvolutionReadSnapshot
import br.com.mwconsulting.championcompanion.core.model.EvolutionSeriesGranularity
import br.com.mwconsulting.championcompanion.core.model.EvolutionSeriesPoint
import br.com.mwconsulting.championcompanion.core.model.EvolutionVolumeSnapshot
import br.com.mwconsulting.championcompanion.core.model.EvolutionSnapshot
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.model.toLocalDateIn
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Agregação pura dos fatos já selecionados pelo read model de evolução. */
fun EvolutionReadSnapshot.toVolumeSnapshot(): EvolutionVolumeSnapshot {
    val activitiesByDate = activities.groupBy { it.startedAt.toLocalDateIn(query.zoneId) }
    val distanceActivities = activities.filter { it.distanceMeters != null }
    val durationActivities = activities.filter { it.duration != null }
    val stepActivities = activities.filter { it.steps != null }

    return EvolutionVolumeSnapshot(
        query = query,
        activityCount = activities.size,
        totalDistanceMeters = distanceActivities.map { it.distanceMeters!! }.checkedFiniteSumOrNull(),
        totalDuration = durationActivities.map { it.duration!! }.checkedDurationSumOrNull(),
        totalSteps = stepActivities.map { it.steps!! }.checkedLongSumOrNull(),
        activeDayCount = activitiesByDate.size,
        distanceActivityCount = distanceActivities.size,
        durationActivityCount = durationActivities.size,
        stepsActivityCount = stepActivities.size,
        series = query.currentWindow.dateRange.dates.toVolumeSeries(activitiesByDate, query.zoneId),
    )
}

/** Monta snapshot completo sem inventar tendências, comparações ou recordes. */
fun EvolutionReadSnapshot.toEvolutionSnapshot(): EvolutionSnapshot = EvolutionSnapshot(
    query = query,
    volume = toVolumeSnapshot(),
    trends = toTrendsSnapshot(),
)

private val VOLUME_METRICS = listOf(
    EvolutionMetric.ACTIVITY_COUNT,
    EvolutionMetric.DISTANCE,
    EvolutionMetric.DURATION,
    EvolutionMetric.STEPS,
    EvolutionMetric.ACTIVE_DAYS,
)

private fun List<LocalDate>.toVolumeSeries(
    activitiesByDate: Map<LocalDate, List<EvolutionActivitySnapshot>>,
    zoneId: ZoneId,
): List<EvolutionMetricSeries> = VOLUME_METRICS.map { metric ->
    EvolutionMetricSeries(
        metric = metric,
        granularity = EvolutionSeriesGranularity.DAY,
        points = map { date ->
            val startedAt = date.atStartOfDay(zoneId).toInstant()
            val activities = activitiesByDate[date].orEmpty()
            activities.toVolumePoint(metric, startedAt)
        },
    )
}

private fun List<EvolutionActivitySnapshot>.toVolumePoint(
    metric: EvolutionMetric,
    startedAt: Instant,
): EvolutionSeriesPoint {
    if (isEmpty()) return EvolutionSeriesPoint.gap(startedAt)

    return when (metric) {
        EvolutionMetric.ACTIVITY_COUNT -> EvolutionSeriesPoint.present(startedAt, size.toDouble())
        EvolutionMetric.ACTIVE_DAYS -> EvolutionSeriesPoint.present(startedAt, 1.0)
        EvolutionMetric.DISTANCE -> mapNotNull { it.distanceMeters }
            .toSeriesPoint(startedAt)
        EvolutionMetric.DURATION -> mapNotNull { it.duration?.secondsAsDouble() }
            .toSeriesPoint(startedAt)
        EvolutionMetric.STEPS -> when {
            all { it.workoutType == WorkoutType.CYCLING } -> EvolutionSeriesPoint.notApplicable(startedAt)
            else -> mapNotNull { it.steps?.toDouble() }.toSeriesPoint(startedAt)
        }
        EvolutionMetric.AVERAGE_BPM,
        EvolutionMetric.CADENCE,
        EvolutionMetric.PACE,
        EvolutionMetric.SPEED,
        -> error("Métrica não pertence ao volume")
    }
}

private fun List<Double>.toSeriesPoint(startedAt: Instant): EvolutionSeriesPoint = when {
    isEmpty() -> EvolutionSeriesPoint.absent(startedAt)
    else -> EvolutionSeriesPoint.present(startedAt, checkedFiniteSum())
}

private fun List<Double>.checkedFiniteSumOrNull(): Double? = takeIf { it.isNotEmpty() }?.checkedFiniteSum()

private fun List<Double>.checkedFiniteSum(): Double {
    var total = 0.0
    for (value in this) {
        total += value
        require(total.isFinite()) { "Agregado de distância excede limite numérico" }
    }
    return total.takeIf { it > 0.0 }
        ?: throw IllegalArgumentException("Agregado de distância deve ser positivo")
}

private fun List<Duration>.checkedDurationSumOrNull(): Duration? {
    if (isEmpty()) return null
    var total = Duration.ZERO
    for (duration in this) {
        total = try {
            total.plus(duration)
        } catch (_: ArithmeticException) {
            throw IllegalArgumentException("Agregado de duração excede limite temporal")
        }
    }
    return total.takeIf { !it.isZero && !it.isNegative }
        ?: throw IllegalArgumentException("Agregado de duração deve ser positivo")
}

private fun List<Long>.checkedLongSumOrNull(): Long? {
    if (isEmpty()) return null
    var total = 0L
    for (steps in this) {
        total = try {
            Math.addExact(total, steps)
        } catch (_: ArithmeticException) {
            throw IllegalArgumentException("Agregado de passos excede limite numérico")
        }
    }
    return total.takeIf { it > 0L }
        ?: throw IllegalArgumentException("Agregado de passos deve ser positivo")
}

private fun Duration.secondsAsDouble(): Double = seconds.toDouble() + nano.toDouble() / 1_000_000_000.0
