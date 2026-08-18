package br.com.mwconsulting.championcompanion.feature.health

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.DarkColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.LightColorScheme
import br.com.mwconsulting.championcompanion.core.model.CalendarStepsHistorySnapshot
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.max

@Composable
internal fun StepsHistoryChart(
    snapshot: CalendarStepsHistorySnapshot,
    modifier: Modifier = Modifier,
) {
    val entries = snapshot.entries
    val locale = LocalConfiguration.current.locales[0]
    val maxScale = max(
        snapshot.stepsGoal.toDouble(),
        snapshot.aggregates.maximumSteps?.toDouble() ?: 0.0,
    ).coerceAtLeast(1.0)
    val stepsColor = if (isSystemInDarkTheme()) DarkColorScheme.tertiary else LightColorScheme.tertiary
    val gapColor = MaterialTheme.colorScheme.outline
    val goalColor = stepsColor.copy(alpha = 0.72f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val numberFormat = NumberFormat.getIntegerInstance(locale)
    val recordedDayCount = entries.count { it.steps != null }
    val missingDayCount = entries.size - recordedDayCount
    val chartDescription = "Gráfico de barras de passos para ${entries.size} dias. " +
        "Meta atual: ${numberFormat.format(snapshot.stepsGoal)} passos. " +
        "$recordedDayCount dias registrados e $missingDayCount dias sem dados. " +
        "Lacunas não representam zero passos. Valores diários estão disponíveis na lista acessível."

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .semantics {
                    contentDescription = chartDescription
                },
        ) {
            val top = 10.dp.toPx()
            val bottom = size.height - 18.dp.toPx()
            val chartHeight = (bottom - top).coerceAtLeast(1f)
            val slotWidth = size.width / entries.size.coerceAtLeast(1)
            val barWidth = (slotWidth * 0.68f).coerceAtLeast(2.dp.toPx())
            val goalY = bottom - ((snapshot.stepsGoal / maxScale).toFloat() * chartHeight)

            listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
                val y = bottom - (fraction * chartHeight)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            drawLine(
                color = goalColor,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 1.dp.toPx(),
            )

            entries.forEachIndexed { index, entry ->
                val centerX = slotWidth * index + slotWidth / 2f
                val left = centerX - barWidth / 2f
                if (entry.steps == null) {
                    drawRoundRect(
                        color = gapColor,
                        topLeft = Offset(left, bottom - 8.dp.toPx()),
                        size = Size(barWidth, 8.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                } else {
                    val barHeight = ((entry.steps.toDouble() / maxScale) * chartHeight)
                        .toFloat()
                        .coerceAtLeast(2.dp.toPx())
                    drawRoundRect(
                        color = stepsColor,
                        topLeft = Offset(left, bottom - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
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
            text = "Barras: passos registrados • linha: meta atual • marca: sem dados",
            modifier = Modifier.padding(top = ChampionSpacing.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun shortDateFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
