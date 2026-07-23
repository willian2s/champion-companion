package br.com.mwconsulting.championcompanion.feature.health

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.model.HeartRateChartPoint
import br.com.mwconsulting.championcompanion.core.model.HeartRateChartSeries
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryPeriod
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun HeartRateHistoryChart(
    series: HeartRateChartSeries,
    period: HeartRateHistoryPeriod,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val points = series.points
    if (points.isEmpty()) {
        Text(
            text = "Sem pontos disponíveis para ${periodToLabel(period).lowercase(Locale.getDefault())}.",
            modifier = modifier.padding(top = ChampionSpacing.medium),
            color = onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }

    val values = points.flatMap { point ->
        listOfNotNull(point.minimumBpm?.toFloat(), point.maximumBpm?.toFloat(), point.bpm.toFloat())
    }
    val minValue = values.minOrNull() ?: 0f
    val maxValue = values.maxOrNull() ?: minValue
    val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
    val chartDescription = points.joinToString(separator = "; ") { point ->
        chartPointLabel(point, period)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = ChampionSpacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${maxValue.roundToInt()} BPM",
                style = MaterialTheme.typography.labelMedium,
                color = onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = if (series.points.size == 1) "1 ponto" else "${series.points.size} pontos",
                style = MaterialTheme.typography.labelMedium,
                color = onSurfaceVariant,
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .semantics {
                    contentDescription = "Gráfico de frequência cardíaca para ${periodToLabel(period)}. $chartDescription"
                },
        ) {
            val horizontalPadding = 16.dp.toPx()
            val verticalPadding = 20.dp.toPx()
            val width = size.width - (horizontalPadding * 2)
            val height = size.height - (verticalPadding * 2)
            val stepX = if (points.size == 1) 0f else width / (points.lastIndex.coerceAtLeast(1))

            drawLine(
                color = outlineColor,
                start = Offset(horizontalPadding, size.height - verticalPadding),
                end = Offset(size.width - horizontalPadding, size.height - verticalPadding),
                strokeWidth = 2.dp.toPx(),
            )
            drawLine(
                color = outlineColor,
                start = Offset(horizontalPadding, verticalPadding),
                end = Offset(horizontalPadding, size.height - verticalPadding),
                strokeWidth = 2.dp.toPx(),
            )

            val path = Path()
            points.forEachIndexed { index, point ->
                val x = horizontalPadding + (index * stepX)
                val normalized = ((point.bpm.toFloat() - minValue) / range).coerceIn(0f, 1f)
                val y = size.height - verticalPadding - (normalized * height)
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }

                if (point.minimumBpm != null && point.maximumBpm != null) {
                    val minY = size.height - verticalPadding - (((point.minimumBpm.toFloat() - minValue) / range).coerceIn(0f, 1f) * height)
                    val maxY = size.height - verticalPadding - (((point.maximumBpm.toFloat() - minValue) / range).coerceIn(0f, 1f) * height)
                    drawLine(
                        color = secondaryColor,
                        start = Offset(x, minY),
                        end = Offset(x, maxY),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx()),
            )

            points.forEachIndexed { index, point ->
                val x = horizontalPadding + (index * stepX)
                val normalized = ((point.bpm.toFloat() - minValue) / range).coerceIn(0f, 1f)
                val y = size.height - verticalPadding - (normalized * height)
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y),
                )
            }
        }
        Text(
            text = "${minValue.roundToInt()} BPM",
            modifier = Modifier.padding(top = ChampionSpacing.small),
            style = MaterialTheme.typography.labelMedium,
            color = onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
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
