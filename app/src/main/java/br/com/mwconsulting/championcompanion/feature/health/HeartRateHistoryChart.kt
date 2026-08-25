package br.com.mwconsulting.championcompanion.feature.health

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionTimeSeriesChart
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionCartesianChartData
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartAxisSpec
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartFormatters
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartInteractionPolicy
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartPoint
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartSeries
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartUnit
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartXAxisFormatter
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartXAxisSpec
import br.com.mwconsulting.championcompanion.core.model.HeartRateChartPoint
import br.com.mwconsulting.championcompanion.core.model.HeartRateChartResolution
import br.com.mwconsulting.championcompanion.core.model.HeartRateChartSeries
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryPeriod
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

private const val MIN_HEART_RATE_BPM = 30.0
private const val MAX_HEART_RATE_BPM = 220.0

@Composable
internal fun HeartRateHistoryChart(
    series: HeartRateChartSeries,
    period: HeartRateHistoryPeriod,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val zoneId = ZoneId.systemDefault()
    val chartSeries = buildChartSeries(series, period, zoneId, locale)
    val chartPoints = chartSeries.flatMap { it.points }
    val intradayStart = series.points.minOfOrNull { it.timestamp }
    val xBounds = chartXBounds(period, chartPoints)
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM", locale)
    val chartPointCount = chartPoints.count { it.y != null }
    val gapCount = chartPoints.count { it.y == null }
    val sampleCount = series.points.sumOf { it.sampleCount }
    val chartData = ChampionCartesianChartData(
        series = chartSeries,
        xAxis = ChampionChartXAxisSpec(
            formatter = ChampionChartXAxisFormatter { value ->
                formatXAxisValue(value, period, intradayStart, zoneId, locale)
            },
            title = if (period == HeartRateHistoryPeriod.TODAY) "Horário" else "Data",
            minimum = xBounds.first,
            maximum = xBounds.second,
        ),
        yAxis = ChampionChartAxisSpec(
            title = "Frequência cardíaca",
            formatter = ChampionChartFormatters.integer(),
            unit = ChampionChartUnit("bpm"),
            minimum = MIN_HEART_RATE_BPM,
            maximum = MAX_HEART_RATE_BPM,
        ),
        interaction = ChampionChartInteractionPolicy(
            scrollEnabled = chartPoints.size > 12,
        ),
        accessibilitySummary = "Gráfico de frequência cardíaca para ${periodToLabel(period)}. " +
            "$chartPointCount ponto(s) com dados e $sampleCount amostra(s) disponíveis. " +
            "$gapCount lacuna(s) sem dados. Valores representam BPM reais; lacunas não representam zero BPM.",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        ChampionTimeSeriesChart(
            data = chartData,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
        )
        Text(
            text = if (chartPointCount == 0) {
                "Use medição manual, importação D1 ou atividades do relógio para ver tendências."
            } else if (period == HeartRateHistoryPeriod.TODAY) {
                "Linha: BPM por amostra • marcador: ponto mais próximo"
            } else {
                "Linha: BPM médio por período • marcador: ponto mais próximo"
            },
            modifier = Modifier.padding(top = ChampionSpacing.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        if (gapCount > 0 && period != HeartRateHistoryPeriod.TODAY) {
            val gapLabels = chartPoints
                .filter { it.y == null }
                .joinToString(limit = 5, truncated = "…") { point ->
                    point.xLabel ?: dateFormatter.format(Instant.ofEpochMilli(point.x.roundToLong()).atZone(zoneId))
                }
            Text(
                text = "Lacunas: $gapLabels (sem dados; não representam zero BPM)",
                modifier = Modifier.padding(top = ChampionSpacing.extraSmall),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun buildChartSeries(
    series: HeartRateChartSeries,
    period: HeartRateHistoryPeriod,
    zoneId: ZoneId,
    locale: Locale,
): List<ChampionChartSeries> = when (series.resolution) {
    HeartRateChartResolution.INTRADAY -> {
        val timeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm", locale)
        val sortedPoints = series.points.sortedBy { it.timestamp }
        val start = sortedPoints.firstOrNull()?.timestamp
        val lanes = mutableListOf<MutableList<ChampionChartPoint>>()
        sortedPoints.forEach { point ->
            val x = start?.let { ChronoUnit.MILLIS.between(it, point.timestamp).toDouble() / 1_000.0 }
                ?: 0.0
            val chartPoint = point.toChampionPoint(
                x = x,
                xLabel = timeFormatter.format(point.timestamp.atZone(zoneId)),
                period = period,
            )
            lanes.firstOrNull { lane -> lane.lastOrNull()?.x?.let { it < x } != false }
                ?.add(chartPoint)
                ?: lanes.add(mutableListOf(chartPoint))
        }
        if (lanes.isEmpty()) {
            listOf(
                ChampionChartSeries(
                    key = "heart-rate",
                    label = "Frequência cardíaca",
                    points = listOf(
                        ChampionChartPoint(
                            x = 0.0,
                            y = null,
                            xLabel = "Hoje",
                            contentDescription = "Hoje: sem dados.",
                        ),
                    ),
                ),
            )
        } else {
            lanes.mapIndexed { index, points ->
                ChampionChartSeries(
                    key = if (index == 0) "heart-rate" else "heart-rate-$index",
                    label = "Frequência cardíaca",
                    points = points,
                )
            }
        }
    }

    HeartRateChartResolution.DAILY -> {
        val points = buildDailyChartPoints(series, period, zoneId, locale)
        listOf(
            ChampionChartSeries(
                key = "heart-rate",
                label = "Frequência cardíaca",
                points = points,
            ),
        )
    }
}

private fun buildDailyChartPoints(
    series: HeartRateChartSeries,
    period: HeartRateHistoryPeriod,
    zoneId: ZoneId,
    locale: Locale,
): List<ChampionChartPoint> {
    val shortDateFormatter = DateTimeFormatter.ofPattern("dd/MM", locale)
    val pointsByDate = series.points.associateBy { point ->
        point.timestamp.atZone(zoneId).toLocalDate()
    }
    val endDate = LocalDate.now(zoneId)
    val startDate = endDate.minusDays(period.windowDays - 1)
    return (0 until period.windowDays.toInt()).map { index ->
        val date = startDate.plusDays(index.toLong())
        val point = pointsByDate[date]
        val x = ChronoUnit.DAYS.between(startDate, date).toDouble()
        point?.toChampionPoint(
            x = x,
            xLabel = shortDateFormatter.format(date),
            period = period,
        ) ?: ChampionChartPoint(
            x = x,
            y = null,
            xLabel = shortDateFormatter.format(date),
            contentDescription = "${shortDateFormatter.format(date)}: sem dados.",
        )
    }
}

private fun HeartRateChartPoint.toChampionPoint(
    x: Double,
    xLabel: String,
    period: HeartRateHistoryPeriod,
): ChampionChartPoint {
    val bpm = bpm.roundToInt()
    val sampleLabel = if (sampleCount == 1) "1 amostra" else "$sampleCount amostras"
    val range = if (minimumBpm != null && maximumBpm != null && minimumBpm != maximumBpm) {
        " mínimo $minimumBpm e máximo $maximumBpm"
    } else {
        ""
    }
    return ChampionChartPoint(
        x = x,
        y = this.bpm,
        xLabel = xLabel,
        contentDescription = "${periodToLabel(period)} — $xLabel: $bpm bpm, $sampleLabel$range.",
    )
}

private fun formatXAxisValue(
    value: Double,
    period: HeartRateHistoryPeriod,
    intradayStart: Instant?,
    zoneId: ZoneId,
    locale: Locale,
): String {
    val formatter = if (period == HeartRateHistoryPeriod.TODAY) {
        DateTimeFormatter.ofPattern("HH:mm", locale)
    } else {
        DateTimeFormatter.ofPattern("dd/MM", locale)
    }

    if (period == HeartRateHistoryPeriod.TODAY) {
        return intradayStart
            ?.plusMillis(value.roundToLong() * 1_000L)
            ?.atZone(zoneId)
            ?.let(formatter::format)
            ?: "--:--"
    }

    val endDate = LocalDate.now(zoneId)
    val instant = endDate.minusDays(period.windowDays - 1)
        .atStartOfDay(zoneId)
        .toInstant()
        .plusMillis(value.roundToLong() * 24L * 60L * 60L * 1_000L)
    return formatter.format(instant.atZone(zoneId))
}

private fun chartXBounds(period: HeartRateHistoryPeriod, points: List<ChampionChartPoint>): Pair<Double, Double> {
    val maximum = if (period == HeartRateHistoryPeriod.TODAY) {
        points.maxOfOrNull { it.x }?.coerceAtLeast(1.0) ?: 1.0
    } else {
        (period.windowDays - 1).toDouble()
    }
    return 0.0 to maximum
}

private fun periodToLabel(period: HeartRateHistoryPeriod): String = when (period) {
    HeartRateHistoryPeriod.TODAY -> "hoje"
    HeartRateHistoryPeriod.LAST_7_DAYS -> "últimos 7 dias"
    HeartRateHistoryPeriod.LAST_30_DAYS -> "últimos 30 dias"
}

private fun Double.roundToLong(): Long = kotlin.math.round(this).toLong()
