package br.com.mwconsulting.championcompanion.data

import br.com.mwconsulting.championcompanion.core.model.EvolutionActivitySnapshot
import br.com.mwconsulting.championcompanion.core.model.EvolutionMetric
import br.com.mwconsulting.championcompanion.core.model.EvolutionMetricSeries
import br.com.mwconsulting.championcompanion.core.model.EvolutionReadSnapshot
import br.com.mwconsulting.championcompanion.core.model.EvolutionSeriesGranularity
import br.com.mwconsulting.championcompanion.core.model.EvolutionSeriesPoint
import br.com.mwconsulting.championcompanion.core.model.EvolutionTrendsSnapshot
import br.com.mwconsulting.championcompanion.core.model.EvolutionWorkoutFilter
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.model.toLocalDateIn
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val BASE_TREND_METRICS = listOf(
    EvolutionMetric.DISTANCE,
    EvolutionMetric.DURATION,
    EvolutionMetric.AVERAGE_BPM,
    EvolutionMetric.CADENCE,
)

private val COMPARABLE_PACE_SPEED_TYPES = setOf(
    WorkoutType.RUNNING,
    WorkoutType.WALKING,
    WorkoutType.TREADMILL,
)

/** Agregação diária pura das métricas de evolução, sem rederivar fatos locais. */
fun EvolutionReadSnapshot.toTrendsSnapshot(): EvolutionTrendsSnapshot {
    val activitiesByDate = activities.groupBy { it.startedAt.toLocalDateIn(query.zoneId) }
    val metrics = BASE_TREND_METRICS + comparablePaceSpeedMetricsOrEmpty()

    return EvolutionTrendsSnapshot(
        series = metrics.map { metric ->
            EvolutionMetricSeries(
                metric = metric,
                granularity = EvolutionSeriesGranularity.DAY,
                points = query.currentWindow.dateRange.dates.toTrendPoints(
                    activitiesByDate = activitiesByDate,
                    metric = metric,
                    zoneId = query.zoneId,
                ),
            )
        },
    )
}

private fun EvolutionReadSnapshot.comparablePaceSpeedMetricsOrEmpty(): List<EvolutionMetric> =
    when (val filter = query.filter) {
        is EvolutionWorkoutFilter.ByType -> if (filter.workoutType in COMPARABLE_PACE_SPEED_TYPES) {
            listOf(EvolutionMetric.PACE, EvolutionMetric.SPEED)
        } else {
            emptyList()
        }

        EvolutionWorkoutFilter.Consolidated -> emptyList()
    }

private fun List<LocalDate>.toTrendPoints(
    activitiesByDate: Map<LocalDate, List<EvolutionActivitySnapshot>>,
    metric: EvolutionMetric,
    zoneId: ZoneId,
): List<EvolutionSeriesPoint> = map { date ->
    val startedAt = date.atStartOfDay(zoneId).toInstant()
    val activities = activitiesByDate[date].orEmpty()
    if (activities.isEmpty()) {
        EvolutionSeriesPoint.gap(startedAt)
    } else {
        activities.toTrendPoint(metric, startedAt)
    }
}

private fun List<EvolutionActivitySnapshot>.toTrendPoint(
    metric: EvolutionMetric,
    startedAt: Instant,
): EvolutionSeriesPoint = when (metric) {
    EvolutionMetric.DISTANCE -> mapNotNull { it.distanceMeters }
        .toPositiveSumPoint(startedAt)

    EvolutionMetric.DURATION -> mapNotNull { it.duration }
        .toDurationPoint(startedAt)

    EvolutionMetric.AVERAGE_BPM -> toWeightedAverageBpmPoint(startedAt)
    EvolutionMetric.CADENCE -> toCadencePoint(startedAt)
    EvolutionMetric.PACE -> toPacePoint(startedAt)
    EvolutionMetric.SPEED -> toSpeedPoint(startedAt)
    EvolutionMetric.ACTIVITY_COUNT,
    EvolutionMetric.STEPS,
    EvolutionMetric.ACTIVE_DAYS,
    -> error("Métrica não pertence às tendências")
}

private fun List<Double>.toPositiveSumPoint(startedAt: Instant): EvolutionSeriesPoint {
    if (isEmpty()) return EvolutionSeriesPoint.absent(startedAt)
    return EvolutionSeriesPoint.present(startedAt, checkedFinitePositiveSum())
}

private fun List<Duration>.toDurationPoint(startedAt: Instant): EvolutionSeriesPoint {
    if (isEmpty()) return EvolutionSeriesPoint.absent(startedAt)
    val total = checkedDurationSum()
    val seconds = total.secondsAsDouble()
    return if (seconds.isFinite() && seconds > 0.0) {
        EvolutionSeriesPoint.present(startedAt, seconds)
    } else {
        EvolutionSeriesPoint.absent(startedAt)
    }
}

private fun List<EvolutionActivitySnapshot>.toWeightedAverageBpmPoint(
    startedAt: Instant,
): EvolutionSeriesPoint {
    val weightedActivities = filter { it.averageBpm != null && it.averageBpmSampleCount != null }
    if (weightedActivities.isEmpty()) return EvolutionSeriesPoint.absent(startedAt)

    var sampleCount = 0L
    var weightedBpm = 0.0
    for (activity in weightedActivities) {
        val count = activity.averageBpmSampleCount!!.toLong()
        sampleCount = try {
            Math.addExact(sampleCount, count)
        } catch (_: ArithmeticException) {
            throw IllegalArgumentException("Agregado de amostras de BPM excede limite numérico")
        }
        val contribution = (activity.averageBpmSampleSum?.toDouble()
            ?: activity.averageBpm!!.toDouble() * count.toDouble())
        require(contribution.isFinite()) { "Agregado ponderado de BPM excede limite numérico" }
        weightedBpm += contribution
        require(weightedBpm.isFinite()) { "Agregado ponderado de BPM excede limite numérico" }
    }

    if (sampleCount <= 0L) return EvolutionSeriesPoint.absent(startedAt)
    val average = weightedBpm / sampleCount.toDouble()
    return if (average.isFinite() && average > 0.0) {
        EvolutionSeriesPoint.present(startedAt, average)
    } else {
        EvolutionSeriesPoint.absent(startedAt)
    }
}

private fun List<EvolutionActivitySnapshot>.toCadencePoint(
    startedAt: Instant,
): EvolutionSeriesPoint {
    val pairedActivities = filter {
        it.workoutType != WorkoutType.CYCLING &&
            it.cadenceSteps != null &&
            it.cadenceDuration != null
    }
    if (pairedActivities.isEmpty()) {
        return if (isNotEmpty() && all { it.workoutType == WorkoutType.CYCLING }) {
            EvolutionSeriesPoint.notApplicable(startedAt)
        } else {
            EvolutionSeriesPoint.absent(startedAt)
        }
    }

    val totalSteps = pairedActivities.map { it.cadenceSteps!! }.checkedLongSum()
    val totalDurationSeconds = pairedActivities.map { it.cadenceDuration!! }
        .checkedDurationSum()
        .secondsAsDouble()
    return totalSteps.toRatePoint(
        denominator = totalDurationSeconds,
        multiplier = 60.0,
        startedAt = startedAt,
    )
}

private fun List<EvolutionActivitySnapshot>.toPacePoint(
    startedAt: Instant,
): EvolutionSeriesPoint = pairedDistanceDuration().toRatePoint(
    numerator = { durationSeconds },
    denominator = { distanceMeters / 1_000.0 },
    startedAt = startedAt,
)

private fun List<EvolutionActivitySnapshot>.toSpeedPoint(
    startedAt: Instant,
): EvolutionSeriesPoint = pairedDistanceDuration().toRatePoint(
    numerator = { distanceMeters },
    denominator = { durationSeconds },
    startedAt = startedAt,
)

private data class DistanceDurationAggregate(
    val distanceMeters: Double,
    val durationSeconds: Double,
)

private fun List<EvolutionActivitySnapshot>.pairedDistanceDuration(): DistanceDurationAggregate? {
    val pairedActivities = filter { it.distanceMeters != null && it.paceSpeedDuration != null }
    if (pairedActivities.isEmpty()) return null

    val distanceMeters = pairedActivities.map { it.distanceMeters!! }.checkedFinitePositiveSum()
    val durationSeconds = pairedActivities.map { it.paceSpeedDuration!! }
        .checkedDurationSum()
        .secondsAsDouble()
    if (!durationSeconds.isFinite() || durationSeconds <= 0.0) return null
    return DistanceDurationAggregate(distanceMeters, durationSeconds)
}

private fun DistanceDurationAggregate?.toRatePoint(
    numerator: DistanceDurationAggregate.() -> Double,
    denominator: DistanceDurationAggregate.() -> Double,
    startedAt: Instant,
): EvolutionSeriesPoint {
    if (this == null) return EvolutionSeriesPoint.absent(startedAt)
    val numeratorValue = numerator()
    val denominatorValue = denominator()
    if (!numeratorValue.isFinite() || numeratorValue <= 0.0 ||
        !denominatorValue.isFinite() || denominatorValue <= 0.0
    ) {
        return EvolutionSeriesPoint.absent(startedAt)
    }
    val value = numeratorValue / denominatorValue
    return if (value.isFinite() && value > 0.0) {
        EvolutionSeriesPoint.present(startedAt, value)
    } else {
        EvolutionSeriesPoint.absent(startedAt)
    }
}

private fun Long.toRatePoint(
    denominator: Double,
    multiplier: Double,
    startedAt: Instant,
): EvolutionSeriesPoint {
    if (this < 0L || !denominator.isFinite() || denominator <= 0.0) {
        return EvolutionSeriesPoint.absent(startedAt)
    }
    val value = toDouble() * multiplier / denominator
    return if (value.isFinite() && value >= 0.0) {
        EvolutionSeriesPoint.present(startedAt, value)
    } else {
        EvolutionSeriesPoint.absent(startedAt)
    }
}

private fun List<Double>.checkedFinitePositiveSum(): Double {
    var total = 0.0
    for (value in this) {
        total += value
        require(total.isFinite()) { "Agregado de tendência excede limite numérico" }
    }
    require(total > 0.0) { "Agregado de tendência deve ser positivo" }
    return total
}

private fun List<Duration>.checkedDurationSum(): Duration {
    var total = Duration.ZERO
    for (duration in this) {
        total = try {
            total.plus(duration)
        } catch (_: ArithmeticException) {
            throw IllegalArgumentException("Agregado de duração excede limite temporal")
        }
    }
    require(total.isPositive()) { "Agregado de duração deve ser positivo" }
    return total
}

private fun List<Long>.checkedLongSum(): Long {
    var total = 0L
    for (value in this) {
        total = try {
            Math.addExact(total, value)
        } catch (_: ArithmeticException) {
            throw IllegalArgumentException("Agregado de passos excede limite numérico")
        }
    }
    require(total >= 0L) { "Agregado de passos não pode ser negativo" }
    return total
}

private fun Duration.secondsAsDouble(): Double = seconds.toDouble() + nano.toDouble() / 1_000_000_000.0
