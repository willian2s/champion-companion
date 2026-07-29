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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.model.CalendarDistanceHistorySnapshot
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.max

@Composable
internal fun DistanceHistoryChart(
    snapshot: CalendarDistanceHistorySnapshot,
    modifier: Modifier = Modifier,
) {
    val entries = snapshot.entries
    val locale = LocalConfiguration.current.locales[0]
    val maxScale = max(
        snapshot.aggregates.maximumDistanceMeters ?: 0.0,
        1.0,
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val gapColor = MaterialTheme.colorScheme.outline
    val chartDescription = entries.joinToString(separator = "; ") { entry ->
        val date = shortDateFormatter(locale).format(entry.localDate)
        val value = entry.estimatedDistanceMeters?.let { "${formatDistance(it)} km, estimada" }
            ?: "sem estimativa"
        "$date: $value"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .semantics {
                    contentDescription = "Gráfico de barras de distância estimada em quilômetros. $chartDescription"
                },
        ) {
            val top = 10.dp.toPx()
            val bottom = size.height - 18.dp.toPx()
            val chartHeight = (bottom - top).coerceAtLeast(1f)
            val slotWidth = size.width / entries.size.coerceAtLeast(1)
            val barWidth = (slotWidth * 0.68f).coerceAtLeast(2.dp.toPx())

            entries.forEachIndexed { index, entry ->
                val centerX = slotWidth * index + slotWidth / 2f
                val left = centerX - barWidth / 2f
                val distanceMeters = entry.estimatedDistanceMeters
                if (distanceMeters == null) {
                    drawRoundRect(
                        color = gapColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                } else {
                    val barHeight = ((distanceMeters / maxScale) * chartHeight)
                        .toFloat()
                        .coerceAtLeast(2.dp.toPx())
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(left, bottom - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ChampionSpacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val labels = listOf(entries.first(), entries[entries.lastIndex / 2], entries.last())
            labels.forEach { entry ->
                Text(
                    text = shortDateFormatter(locale).format(entry.localDate),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Text(
            text = "Barras: distância estimada • contorno: sem estimativa • unidade: km",
            modifier = Modifier.padding(top = ChampionSpacing.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun formatDistance(meters: Double): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }.format(meters / 1000.0)

private fun shortDateFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
