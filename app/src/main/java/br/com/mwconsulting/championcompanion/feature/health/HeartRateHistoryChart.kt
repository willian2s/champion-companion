package br.com.mwconsulting.championcompanion.feature.health

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionBarChart
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
private const val HEART_RATE_BAR_BASELINE_BPM = 0.0
private const val DAY_SECONDS = 24.0 * 60.0 * 60.0
private val HEART_RATE_TODAY_AXIS_VALUES = listOf(0, 4, 8, 12, 16, 20, 23)
private val HEART_RATE_WEEKDAY_LABELS = listOf("D", "S", "T", "Q", "Q", "S", "S")
private val HEART_RATE_MONTH_LABELS = listOf(
    "JAN", "FEV", "MAR", "ABR", "MAI", "JUN",
    "JUL", "AGO", "SET", "OUT", "NOV", "DEZ",
)

@Composable
internal fun HeartRateHistoryChart(
    series: HeartRateChartSeries,
    period: HeartRateHistoryPeriod,
    origins: Set<HeartRateSource> = emptySet(),
    modifier: Modifier = Modifier,
    chartHeight: Dp = 210.dp,
    chartMinimumHeight: Dp = 192.dp,
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
    val xBounds = chartXBounds(period, series.resolution, chartPoints)
    val chartPointCount = chartPoints.count { it.y != null }
    val gapCount = chartPoints.count { it.y == null }
    val sampleCount = series.points.sumOf { it.sampleCount }
    val chartData = ChampionCartesianChartData(
        series = chartSeries,
        xAxis = ChampionChartXAxisSpec(
            formatter = ChampionChartXAxisFormatter { value ->
                formatXAxisValue(value, period, series.resolution, intradayStart, zoneId, locale, chartPoints)
            },
            title = if (period == HeartRateHistoryPeriod.TODAY) "Horário" else "Data",
            visible = period != HeartRateHistoryPeriod.TODAY,
            gridLinesVisible = false,
            minimum = xBounds.first,
            maximum = xBounds.second,
            labelValues = chartLabelValues(period, series.resolution, chartPoints),
        ),
        yAxis = ChampionChartAxisSpec(
            title = "Frequência cardíaca",
            formatter = ChampionChartFormatters.integer(),
            unit = ChampionChartUnit("bpm"),
            minimum = HEART_RATE_BAR_BASELINE_BPM,
            maximum = MAX_HEART_RATE_BPM,
            visible = false,
            gridLinesVisible = false,
        ),
        interaction = ChampionChartInteractionPolicy(
            scrollEnabled = false,
        ),
     accessibilitySummary = "Gráfico de frequência cardíaca para ${periodDescription(period)}. " +
            "$chartPointCount ponto(s) com dados e $sampleCount amostra(s) disponíveis. " +
            "$gapCount lacuna(s) sem dados. Origem: $originLabel. Valores representam BPM reais; " +
                "lacunas não representam zero BPM.",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        if (period == HeartRateHistoryPeriod.TODAY) {
            GoogleHealthIntradayHeartRateChart(
                points = chartPoints,
                accessibilitySummary = chartData.accessibilitySummary,
                modifier = Modifier.height(chartHeight),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HEART_RATE_TODAY_AXIS_VALUES.forEach { label ->
                    Text(
                        text = label.toString(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        } else {
            ChampionBarChart(
                data = chartData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
                minimumHeight = chartMinimumHeight,
            )
        }
    }
}

@Composable
internal fun GoogleHealthIntradayHeartRateChart(
    points: List<ChampionChartPoint>,
    accessibilitySummary: String,
    showLine: Boolean = false,
    timeWindowSeconds: Double = DAY_SECONDS,
    modifier: Modifier = Modifier,
) {
    val maxSeconds = timeWindowSeconds.coerceAtLeast(1.0)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$accessibilitySummary Zonas visuais sem cálculo por idade: Z1 30–79, Z2 80–129, Z3 130–169 e Z4 170–220 bpm." },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
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

            val recordedPoints = points.filter { it.y != null }.sortedBy { it.x }
            if (showLine) {
                recordedPoints.zipWithNext().forEach { (start, end) ->
                    val startBpm = start.y ?: return@forEach
                    val endBpm = end.y ?: return@forEach
                    drawLine(
                        color = Color(0xFF2BC7F2),
                        start = Offset(
                            (start.x / maxSeconds * size.width).toFloat().coerceIn(0f, size.width),
                            yFor(startBpm),
                        ),
                        end = Offset(
                            (end.x / maxSeconds * size.width).toFloat().coerceIn(0f, size.width),
                            yFor(endBpm),
                        ),
                        strokeWidth = 3.dp.toPx(),
                    )
                }
            } else {
                recordedPoints.forEach { point ->
                    val bpm = point.y ?: return@forEach
                    val x = (point.x / maxSeconds * size.width).toFloat().coerceIn(0f, size.width)
                    drawCircle(
                        color = Color(0xFF2BC7F2),
                        radius = 2.5.dp.toPx(),
                        center = Offset(x, yFor(bpm)),
                    )
                }
            }

            val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 10.dp.toPx()
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val zoneLabels = listOf(
                195.0 to "Z4",
                150.0 to "Z3",
                105.0 to "Z2",
                55.0 to "Z1",
            )
            drawIntoCanvas { canvas ->
                zoneLabels.forEach { (bpm, label) ->
                    val baseline = yFor(bpm) - (labelPaint.ascent() + labelPaint.descent()) / 2f
                    canvas.nativeCanvas.drawText(
                        label,
                        size.width - labelPaint.measureText(label) - 4.dp.toPx(),
                        baseline,
                        labelPaint,
                    )
                }
            }
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

    HeartRateChartResolution.MONTHLY -> {
        val points = series.points
            .sortedBy { it.timestamp }
            .mapIndexed { index, point ->
                val monthLabel = HEART_RATE_MONTH_LABELS[point.timestamp.atZone(zoneId).monthValue - 1]
                point.toChampionPoint(
                    x = index.toDouble(),
                    xLabel = if (period == HeartRateHistoryPeriod.LAST_365_DAYS) {
                        monthLabel.first().toString()
                    } else {
                        monthLabel
                    },
                    period = period,
                )
            }
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
    val points = series.points.sortedBy { it.timestamp }
    val shortDateFormatter = DateTimeFormatter.ofPattern("dd/MM", locale)
    return points.mapIndexed { index, point ->
        val date = point.timestamp.atZone(zoneId).toLocalDate()
        val x = if (period == HeartRateHistoryPeriod.LAST_7_DAYS) {
            index.toDouble()
        } else {
            date.dayOfMonth.toDouble()
        }
        val label = if (period == HeartRateHistoryPeriod.LAST_7_DAYS) {
            HEART_RATE_WEEKDAY_LABELS.getOrNull(index) ?: "--"
        } else {
            date.dayOfMonth.toString()
        }
        point.toChampionPoint(
            x = x,
            xLabel = if (period == HeartRateHistoryPeriod.LAST_7_DAYS) {
                "$label (${shortDateFormatter.format(date)})"
            } else {
                label
            },
            period = period,
        )
    }
}

private fun HeartRateChartPoint.toChampionPoint(
    x: Double,
    xLabel: String,
    period: HeartRateHistoryPeriod,
): ChampionChartPoint {
    val bpm = bpm ?: return ChampionChartPoint(
        x = x,
        y = null,
        xLabel = xLabel,
        contentDescription = "${periodToLabel(period)} — $xLabel: sem dados.",
    )
    val roundedBpm = bpm.roundToInt()
    val sampleLabel = if (sampleCount == 1) "1 amostra" else "$sampleCount amostras"
    val range = if (minimumBpm != null && maximumBpm != null && minimumBpm != maximumBpm) {
        " mínimo $minimumBpm e máximo $maximumBpm"
    } else {
        ""
    }
    return ChampionChartPoint(
        x = x,
        y = bpm,
        xLabel = xLabel,
        contentDescription = "${periodToLabel(period)} — $xLabel: $roundedBpm bpm, $sampleLabel$range.",
    )
}

private fun formatXAxisValue(
    value: Double,
    period: HeartRateHistoryPeriod,
    resolution: HeartRateChartResolution,
    intradayStart: Instant?,
    zoneId: ZoneId,
    locale: Locale,
    points: List<ChampionChartPoint>,
): String {
    if (resolution == HeartRateChartResolution.MONTHLY) {
        return points.getOrNull(value.roundToInt())?.xLabel ?: "--"
    }

    if (period == HeartRateHistoryPeriod.LAST_7_DAYS) {
        return HEART_RATE_WEEKDAY_LABELS.getOrNull(value.roundToInt()) ?: "--"
    }
    if (period == HeartRateHistoryPeriod.LAST_30_DAYS) {
        return value.roundToInt().takeIf { it in 1..31 }?.toString() ?: "--"
    }

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

private fun chartXBounds(
    period: HeartRateHistoryPeriod,
    resolution: HeartRateChartResolution,
    points: List<ChampionChartPoint>,
): Pair<Double, Double> {
    val maximum = when (resolution) {
        HeartRateChartResolution.INTRADAY -> DAY_SECONDS
        HeartRateChartResolution.DAILY -> if (period == HeartRateHistoryPeriod.LAST_7_DAYS) {
            6.5
        } else {
            val lastDay = points.maxOfOrNull { it.x.roundToInt() } ?: 31
            lastDay + 0.5
        }
        HeartRateChartResolution.MONTHLY -> (points.size - 1).coerceAtLeast(0).toDouble() + 0.5
    }
    return when (resolution) {
        HeartRateChartResolution.INTRADAY -> 0.0 to maximum
        HeartRateChartResolution.DAILY -> if (period == HeartRateHistoryPeriod.LAST_7_DAYS) {
            -0.5 to maximum
        } else {
            0.5 to maximum
        }
        HeartRateChartResolution.MONTHLY -> -0.5 to maximum
    }
}

private fun chartLabelValues(
    period: HeartRateHistoryPeriod,
    resolution: HeartRateChartResolution,
    points: List<ChampionChartPoint>,
): List<Double> = when (resolution) {
    HeartRateChartResolution.INTRADAY -> HEART_RATE_TODAY_AXIS_VALUES.map { it * 60.0 * 60.0 }
    HeartRateChartResolution.DAILY -> if (period == HeartRateHistoryPeriod.LAST_7_DAYS) {
        (0..6).map(Int::toDouble)
    } else {
        val lastDay = points.maxOfOrNull { it.x.roundToInt() } ?: 31
        buildList {
            listOf(1, 5, 10, 15, 20, 25, lastDay).distinct().forEach { add(it.toDouble()) }
        }
    }
    HeartRateChartResolution.MONTHLY -> points.indices.map(Int::toDouble)
}

private fun periodToLabel(period: HeartRateHistoryPeriod): String = when (period) {
    HeartRateHistoryPeriod.TODAY -> "Hoje"
    HeartRateHistoryPeriod.LAST_7_DAYS -> "Semana"
    HeartRateHistoryPeriod.LAST_30_DAYS -> "M"
    HeartRateHistoryPeriod.LAST_90_DAYS -> "3M"
    HeartRateHistoryPeriod.LAST_365_DAYS -> "A"
}

private fun periodDescription(period: HeartRateHistoryPeriod): String = when (period) {
    HeartRateHistoryPeriod.TODAY -> "Hoje"
    HeartRateHistoryPeriod.LAST_7_DAYS -> "a semana civil atual"
    HeartRateHistoryPeriod.LAST_30_DAYS -> "o mês calendário atual até hoje"
    HeartRateHistoryPeriod.LAST_90_DAYS -> "o mês atual até hoje e os dois meses calendário anteriores"
    HeartRateHistoryPeriod.LAST_365_DAYS -> "o mês atual até hoje e os onze meses calendário anteriores"
}

private fun Double.roundToLong(): Long = kotlin.math.round(this).toLong()

private fun HeartRateSource.toChartOriginLabel(): String = when (this) {
    HeartRateSource.MANUAL_MEASUREMENT -> "medição manual"
    HeartRateSource.WATCH_D1_HISTORY -> "histórico D1 do relógio"
    HeartRateSource.WATCH_WORKOUT_ACTIVITY -> "atividade do relógio"
    HeartRateSource.MEASUREMENT_STREAM -> "fluxo de medição"
    HeartRateSource.APP_WORKOUT_SESSION -> "sessão do app"
}
