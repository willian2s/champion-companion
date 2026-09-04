package br.com.mwconsulting.championcompanion.feature.health

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
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
import br.com.mwconsulting.championcompanion.core.model.HeartRateSource
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
    origins: Set<HeartRateSource> = emptySet(),
    modifier: Modifier = Modifier,
    chartHeight: Dp = 210.dp,
    chartMinimumHeight: Dp = 192.dp,
    compact: Boolean = false,
) {
    val locale = LocalConfiguration.current.locales[0]
    val zoneId = ZoneId.systemDefault()
    val originLabel = origins
        .map(HeartRateSource::toChartOriginLabel)
        .distinct()
        .joinToString(", ")
        .ifBlank { "não disponível" }
    val chartSeries = buildChartSeries(series, period, zoneId, locale).map { chartSeriesItem ->
        chartSeriesItem.copy(label = "Frequência cardíaca • Origem: $originLabel")
    }
    val chartPoints = chartSeries.flatMap { it.points }
    val intradayStart = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant()
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
            visible = !compact,
            gridLinesVisible = false,
            minimum = xBounds.first,
            maximum = xBounds.second,
        ),
        yAxis = ChampionChartAxisSpec(
            title = "Frequência cardíaca",
            formatter = ChampionChartFormatters.integer(),
            unit = ChampionChartUnit("bpm"),
            minimum = MIN_HEART_RATE_BPM,
            maximum = MAX_HEART_RATE_BPM,
            visible = false,
            gridLinesVisible = false,
        ),
        interaction = ChampionChartInteractionPolicy(
            scrollEnabled = false,
        ),
        accessibilitySummary = "Gráfico de frequência cardíaca para ${periodToLabel(period)}. " +
            "$chartPointCount ponto(s) com dados e $sampleCount amostra(s) disponíveis. " +
            "$gapCount lacuna(s) sem dados. Origem: $originLabel. Valores representam BPM reais; " +
                "lacunas não representam zero BPM.",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        if (compact && period == HeartRateHistoryPeriod.TODAY) {
            GoogleHealthIntradayHeartRateChart(
                points = chartPoints,
                accessibilitySummary = chartData.accessibilitySummary,
                modifier = Modifier.height(chartHeight),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf("0", "12", "0").forEach { label ->
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        } else {
            ChampionTimeSeriesChart(
                data = chartData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
                minimumHeight = chartMinimumHeight,
            )
        }
        if (!compact) {
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
        }
        if (!compact && gapCount > 0 && period != HeartRateHistoryPeriod.TODAY) {
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

@Composable
private fun GoogleHealthIntradayHeartRateChart(
    points: List<ChampionChartPoint>,
    accessibilitySummary: String,
    modifier: Modifier = Modifier,
) {
    val maxSeconds = 24 * 60 * 60.0
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilitySummary },
    ) {
        val top = 0f
        val bottom = size.height
        val range = MAX_HEART_RATE_BPM - MIN_HEART_RATE_BPM
        fun yFor(value: Double): Float =
            (bottom - ((value - MIN_HEART_RATE_BPM) / range * bottom)).toFloat().coerceIn(top, bottom)
        fun drawBand(from: Double, to: Double, color: Color) {
            drawRect(
                color = color,
                topLeft = Offset(0f, yFor(to)),
                size = androidx.compose.ui.geometry.Size(size.width, yFor(from) - yFor(to)),
            )
        }

        drawBand(170.0, MAX_HEART_RATE_BPM, Color(0xFF8E2730).copy(alpha = 0.45f))
        drawBand(130.0, 170.0, Color(0xFF8B7624).copy(alpha = 0.45f))
        drawBand(80.0, 130.0, Color(0xFF267A55).copy(alpha = 0.5f))
        drawBand(MIN_HEART_RATE_BPM, 80.0, Color(0xFF175A62).copy(alpha = 0.42f))

        listOf(170.0 to Color(0xFFE96A6A), 130.0 to Color(0xFFE5BE43), 80.0 to Color(0xFF52C77B))
            .forEach { (value, color) ->
                val y = yFor(value)
                var x = 0f
                while (x < size.width) {
                    drawCircle(color = color, radius = 1.5.dp.toPx(), center = Offset(x, y))
                    x += 8.dp.toPx()
                }
            }

        points.forEach { point ->
            val bpm = point.y ?: return@forEach
            val x = (point.x / maxSeconds * size.width).toFloat().coerceIn(0f, size.width)
            drawCircle(
                color = Color(0xFF2BC7F2),
                radius = 2.5.dp.toPx(),
                center = Offset(x, yFor(bpm)),
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
        val start = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant()
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
        lanes.mapIndexed { index, points ->
            ChampionChartSeries(
                key = if (index == 0) "heart-rate" else "heart-rate-$index",
                label = "Frequência cardíaca",
                points = points,
            )
        }.ifEmpty {
            listOf(
                ChampionChartSeries(
                    key = "heart-rate",
                    label = "Frequência cardíaca",
                    points = emptyList(),
                ),
            )
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
        24.0 * 60.0 * 60.0
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

private fun HeartRateSource.toChartOriginLabel(): String = when (this) {
    HeartRateSource.MANUAL_MEASUREMENT -> "medição manual"
    HeartRateSource.WATCH_D1_HISTORY -> "histórico D1 do relógio"
    HeartRateSource.WATCH_WORKOUT_ACTIVITY -> "atividade do relógio"
    HeartRateSource.MEASUREMENT_STREAM -> "fluxo de medição"
    HeartRateSource.APP_WORKOUT_SESSION -> "sessão do app"
}
