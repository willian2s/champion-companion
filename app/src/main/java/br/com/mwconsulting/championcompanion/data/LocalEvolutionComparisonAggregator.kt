package br.com.mwconsulting.championcompanion.data

import br.com.mwconsulting.championcompanion.core.model.EvolutionMetric
import br.com.mwconsulting.championcompanion.core.model.EvolutionMetricComparison
import br.com.mwconsulting.championcompanion.core.model.EvolutionComparisonDirection
import br.com.mwconsulting.championcompanion.core.model.EvolutionPeriodComparison
import br.com.mwconsulting.championcompanion.core.model.EvolutionVolumeSnapshot
import java.time.Duration
import kotlin.math.abs

/** Comparação pura de métricas de volume entre snapshots equivalentes. */
fun EvolutionVolumeSnapshot.toPeriodComparison(
    previous: EvolutionVolumeSnapshot,
): EvolutionPeriodComparison = EvolutionPeriodComparison(
    current = this,
    previous = previous,
    metrics = COMPARABLE_VOLUME_METRICS.map { metric ->
        metric.toComparison(
            currentValue = valueForComparison(metric),
            previousValue = previous.valueForComparison(metric),
        )
    },
)

private val COMPARABLE_VOLUME_METRICS = listOf(
    EvolutionMetric.ACTIVITY_COUNT,
    EvolutionMetric.DISTANCE,
    EvolutionMetric.DURATION,
    EvolutionMetric.STEPS,
    EvolutionMetric.ACTIVE_DAYS,
)

private fun EvolutionVolumeSnapshot.valueForComparison(metric: EvolutionMetric): Double? = when (metric) {
    EvolutionMetric.ACTIVITY_COUNT -> activityCount.toDouble()
    EvolutionMetric.DISTANCE -> totalDistanceMeters
    EvolutionMetric.DURATION -> totalDuration?.secondsAsDouble()
    EvolutionMetric.STEPS -> totalSteps?.toDouble()
    EvolutionMetric.ACTIVE_DAYS -> activeDayCount.toDouble()
    EvolutionMetric.AVERAGE_BPM,
    EvolutionMetric.CADENCE,
    EvolutionMetric.PACE,
    EvolutionMetric.SPEED,
    -> error("Métrica não pertence à comparação de volume")
}

private fun EvolutionMetric.toComparison(
    currentValue: Double?,
    previousValue: Double?,
): EvolutionMetricComparison {
    if (currentValue == null || previousValue == null) {
        return EvolutionMetricComparison(
            metric = this,
            currentValue = currentValue,
            previousValue = previousValue,
        )
    }

    val absoluteDifference = abs(currentValue - previousValue)
    require(absoluteDifference.isFinite()) {
        "Diferença absoluta da comparação excede limite numérico"
    }
    val percentageDifference = if (previousValue != 0.0) {
        ((currentValue - previousValue) / previousValue * 100.0)
            .takeIf(Double::isFinite)
    } else {
        null
    }
    val direction = when {
        currentValue > previousValue -> EvolutionComparisonDirection.INCREASED
        currentValue < previousValue -> EvolutionComparisonDirection.DECREASED
        else -> EvolutionComparisonDirection.STABLE
    }

    return EvolutionMetricComparison(
        metric = this,
        currentValue = currentValue,
        previousValue = previousValue,
        absoluteDifference = absoluteDifference,
        percentageDifference = percentageDifference,
        direction = direction,
    )
}

private fun Duration.secondsAsDouble(): Double = seconds.toDouble() + nano.toDouble() / 1_000_000_000.0
