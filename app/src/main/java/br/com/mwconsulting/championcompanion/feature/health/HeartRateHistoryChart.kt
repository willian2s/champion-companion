package br.com.mwconsulting.championcompanion.feature.health

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.model.HeartRateChartPoint
import br.com.mwconsulting.championcompanion.core.model.HeartRateChartSeries
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryPeriod
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

private val VisualGuideBands = listOf(
    HeartRateGuideBand(166f, Color(0xFFE57373)),
    HeartRateGuideBand(135f, Color(0xFFFFB74D)),
    HeartRateGuideBand(110f, Color(0xFF66BB6A)),
    HeartRateGuideBand(71f, Color(0xFF4FC3F7)),
    HeartRateGuideBand(30f, Color(0xFF29B6F6)),
)

private val HeartRateChartLeftPadding = 12.dp
private val HeartRateChartRightPadding = 40.dp

@Composable
internal fun HeartRateHistoryChart(
    series: HeartRateChartSeries,
    period: HeartRateHistoryPeriod,
    modifier: Modifier = Modifier,
) {
    val points = series.points
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    if (points.isEmpty()) {
        Column(modifier = modifier.fillMaxWidth()) {
            Text(
                text = "Sem dados",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Use medição manual ou importação D1 para ver tendências.",
                modifier = Modifier.padding(top = ChampionSpacing.small),
                color = onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            EmptyXAxis(period = period)
        }
        return
    }

    val valueSet = points.flatMap { point ->
        listOfNotNull(point.minimumBpm?.toFloat(), point.maximumBpm?.toFloat(), point.bpm.toFloat())
    } + VisualGuideBands.map { it.value }
    val minValue = valueSet.minOrNull() ?: 30f
    val maxValue = valueSet.maxOrNull() ?: 166f
    val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
    val chartDescription = points.joinToString(separator = "; ") { point -> chartPointLabel(point, period) }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .semantics {
                    contentDescription = "Gráfico de frequência cardíaca para ${periodToLabel(period)}. $chartDescription"
                },
        ) {
            val leftPadding = HeartRateChartLeftPadding.toPx()
            val rightPadding = HeartRateChartRightPadding.toPx()
            val topPadding = 14.dp.toPx()
            val bottomPadding = 22.dp.toPx()
            val width = size.width - leftPadding - rightPadding
            val height = size.height - topPadding - bottomPadding
            val zoneId = ZoneId.systemDefault()

            VisualGuideBands.forEach { band ->
                val y = yForValue(
                    value = band.value,
                    minValue = minValue,
                    range = range,
                    chartHeight = height,
                    topPadding = topPadding,
                    bottom = size.height - bottomPadding,
                )
                drawDottedHorizontalLine(
                    color = band.color,
                    startX = leftPadding,
                    endX = size.width - rightPadding,
                    y = y,
                    radius = 2.dp.toPx(),
                    gap = 10.dp.toPx(),
                )
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        band.value.roundToInt().toString(),
                        size.width - rightPadding + 8.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = onSurfaceVariant.toArgbCompat()
                            textSize = 12.dp.toPx()
                            isAntiAlias = true
                        },
                    )
                }
            }

            if (period == HeartRateHistoryPeriod.TODAY) {
                points.forEach { point ->
                    val x = xForPoint(
                        point = point,
                        period = period,
                        zoneId = zoneId,
                        chartWidth = width,
                        leftPadding = leftPadding,
                    )
                    val y = yForValue(point.bpm.toFloat(), minValue, range, height, topPadding, size.height - bottomPadding)
                    drawCircle(
                        color = primaryColor,
                        radius = 2.5.dp.toPx(),
                        center = Offset(x, y),
                    )
                    drawCircle(
                        color = surfaceVariant,
                        radius = 4.5.dp.toPx(),
                        center = Offset(x, y),
                        style = Stroke(width = 0.8.dp.toPx()),
                    )
                }
            } else {
                val path = Path()
                points.forEachIndexed { index, point ->
                    val x = xForPoint(
                        point = point,
                        period = period,
                        zoneId = zoneId,
                        chartWidth = width,
                        leftPadding = leftPadding,
                    )
                    val y = yForValue(point.bpm.toFloat(), minValue, range, height, topPadding, size.height - bottomPadding)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    if (point.minimumBpm != null && point.maximumBpm != null) {
                        val minY = yForValue(point.minimumBpm.toFloat(), minValue, range, height, topPadding, size.height - bottomPadding)
                        val maxY = yForValue(point.maximumBpm.toFloat(), minValue, range, height, topPadding, size.height - bottomPadding)
                        drawLine(
                            color = primaryColor.copy(alpha = 0.45f),
                            start = Offset(x, minY),
                            end = Offset(x, maxY),
                            strokeWidth = 3.dp.toPx(),
                        )
                    }
                }
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx()),
                )
                points.forEach { point ->
                    val x = xForPoint(
                        point = point,
                        period = period,
                        zoneId = zoneId,
                        chartWidth = width,
                        leftPadding = leftPadding,
                    )
                    val y = yForValue(point.bpm.toFloat(), minValue, range, height, topPadding, size.height - bottomPadding)
                    drawCircle(
                        color = primaryColor,
                        radius = 2.5.dp.toPx(),
                        center = Offset(x, y),
                    )
                }
            }
        }

        XAxisLabels(points = points, period = period)
    }
}

@Composable
private fun EmptyXAxis(period: HeartRateHistoryPeriod) {
    XAxisLabels(points = emptyList(), period = period)
}

@Composable
private fun XAxisLabels(
    points: List<HeartRateChartPoint>,
    period: HeartRateHistoryPeriod,
) {
    val labels = buildXAxisLabels(points, period)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .absolutePadding(
                left = HeartRateChartLeftPadding,
                top = ChampionSpacing.small,
                right = HeartRateChartRightPadding,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

private fun DrawScope.drawDottedHorizontalLine(
    color: Color,
    startX: Float,
    endX: Float,
    y: Float,
    radius: Float,
    gap: Float,
) {
    var x = startX
    while (x <= endX) {
        drawCircle(color = color, radius = radius, center = Offset(x, y))
        x += gap
    }
}

private fun yForValue(
    value: Float,
    minValue: Float,
    range: Float,
    chartHeight: Float,
    topPadding: Float,
    bottom: Float,
): Float {
    val normalized = ((value - minValue) / range).coerceIn(0f, 1f)
    return bottom - (normalized * chartHeight).coerceIn(0f, chartHeight) + topPadding - topPadding
}

private fun xForPoint(
    point: HeartRateChartPoint,
    period: HeartRateHistoryPeriod,
    zoneId: ZoneId,
    chartWidth: Float,
    leftPadding: Float,
): Float {
    val fraction = when (period) {
        HeartRateHistoryPeriod.TODAY -> {
            val localTime = point.timestamp.atZone(zoneId).toLocalTime()
            localTime.toSecondOfDay().toFloat() / (24f * 60f * 60f - 1f)
        }

        HeartRateHistoryPeriod.LAST_7_DAYS,
        HeartRateHistoryPeriod.LAST_30_DAYS,
        -> {
            val localDate = point.timestamp.atZone(zoneId).toLocalDate()
            val windowDays = period.windowDays
            val end = LocalDate.now(zoneId)
            val start = end.minusDays(windowDays - 1)
            ChronoUnit.DAYS.between(start, localDate)
                .toFloat()
                .div((windowDays - 1).coerceAtLeast(1).toFloat())
        }
    }.coerceIn(0f, 1f)

    return leftPadding + (fraction * chartWidth)
}

private fun buildXAxisLabels(
    points: List<HeartRateChartPoint>,
    period: HeartRateHistoryPeriod,
): List<String> = when (period) {
    HeartRateHistoryPeriod.TODAY -> listOf("0", "4", "8", "12", "16", "20", "23")
    HeartRateHistoryPeriod.LAST_7_DAYS -> {
        val formatter = DateTimeFormatter.ofPattern("EEEEE", Locale.forLanguageTag("pt-BR"))
        if (points.isEmpty()) {
            lastSevenDaysDates().map { formatter.format(it) }
        } else {
            points.map {
                it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate().let(formatter::format)
            }
        }
    }
    HeartRateHistoryPeriod.LAST_30_DAYS -> {
        val formatter = DateTimeFormatter.ofPattern("d", Locale.forLanguageTag("pt-BR"))
        val source = if (points.isEmpty()) lastThirtyDaysDates() else points.map { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() }
        pickEvenlySpaced(source, count = 4).map(formatter::format)
    }
}

private fun lastSevenDaysDates() = List(7) { index ->
    java.time.LocalDate.now(ZoneId.systemDefault()).minusDays((6 - index).toLong())
}

private fun lastThirtyDaysDates() = List(30) { index ->
    java.time.LocalDate.now(ZoneId.systemDefault()).minusDays((29 - index).toLong())
}

private fun <T> pickEvenlySpaced(values: List<T>, count: Int): List<T> {
    if (values.isEmpty()) return emptyList()
    if (values.size <= count) return values
    return (0 until count).map { step ->
        val fraction = step.toFloat() / (count - 1).coerceAtLeast(1)
        val index = (fraction * values.lastIndex).roundToInt().coerceIn(0, values.lastIndex)
        values[index]
    }
}

private fun periodToLabel(period: HeartRateHistoryPeriod): String = when (period) {
    HeartRateHistoryPeriod.TODAY -> "Hoje"
    HeartRateHistoryPeriod.LAST_7_DAYS -> "7 dias"
    HeartRateHistoryPeriod.LAST_30_DAYS -> "30 dias"
}

private fun chartPointLabel(
    point: HeartRateChartPoint,
    period: HeartRateHistoryPeriod,
): String {
    val timestamp = if (period == HeartRateHistoryPeriod.TODAY) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(point.timestamp)
    } else {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(point.timestamp)
    }
    val range = if (point.minimumBpm != null && point.maximumBpm != null) {
        " • mínimo ${point.minimumBpm} • máximo ${point.maximumBpm}"
    } else {
        ""
    }
    return "$timestamp • média ${point.bpm.roundToInt()} BPM • ${point.sampleCount} registro(s)$range"
}

private data class HeartRateGuideBand(
    val value: Float,
    val color: Color,
)

private fun Color.toArgbCompat(): Int = android.graphics.Color.argb(
    (alpha * 255).roundToInt(),
    (red * 255).roundToInt(),
    (green * 255).roundToInt(),
    (blue * 255).roundToInt(),
)
