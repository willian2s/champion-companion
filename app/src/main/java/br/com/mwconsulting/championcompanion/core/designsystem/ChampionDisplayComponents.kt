package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun HealthCard(
    title: String,
    modifier: Modifier = Modifier,
    status: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = ChampionShapeLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = ChampionElevation.card),
    ) {
        Column(modifier = Modifier.padding(ChampionSpacing.large)) {
            Row {
                Text(
                    text = title,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                )
                status?.invoke()
            }
            content()
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    status: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = listOfNotNull(label, value, supportingText).joinToString(", ")
        },
        shape = ChampionShapeMedium,
        elevation = CardDefaults.cardElevation(defaultElevation = ChampionElevation.card),
    ) {
        Column(modifier = Modifier.padding(ChampionSpacing.large)) {
            Row {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                )
                status?.invoke()
            }
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
            supportingText?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Immutable
data class HealthChartPoint(
    val value: Float,
    val contentDescription: String,
)

@Composable
fun HealthChart(
    title: String,
    points: List<HealthChartPoint>,
    modifier: Modifier = Modifier,
    emptyLabel: String = "Sem dados para exibir",
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        if (points.isEmpty()) {
            Text(
                text = emptyLabel,
                modifier = Modifier
                    .padding(vertical = ChampionSpacing.extraLarge)
                    .semantics { contentDescription = "$title: $emptyLabel" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            val chartDescription = "$title: ${points.joinToString(", ") { it.contentDescription }}"
            val chartColor = MaterialTheme.colorScheme.primary
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(top = ChampionSpacing.medium)
                    .semantics { contentDescription = chartDescription },
            ) {
                val normalizedValues = normalizedChartY(points)
                val horizontalStep = if (points.size > 1) size.width / (points.size - 1) else 0f
                val path = Path()
                normalizedValues.forEachIndexed { index, normalizedY ->
                    val point = Offset(
                        x = horizontalStep * index,
                        y = normalizedY * size.height,
                    )
                    if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                drawPath(
                    path = path,
                    color = chartColor,
                    style = Stroke(width = 3.dp.toPx()),
                )
                normalizedValues.forEachIndexed { index, normalizedY ->
                    drawCircle(
                        color = chartColor,
                        radius = 4.dp.toPx(),
                        center = Offset(horizontalStep * index, normalizedY * size.height),
                    )
                }
            }
        }
    }
}

internal fun normalizedChartY(points: List<HealthChartPoint>): List<Float> {
    if (points.isEmpty()) return emptyList()

    val minimum = points.minOf { it.value }
    val maximum = points.maxOf { it.value }
    if (minimum == maximum) return List(points.size) { 0.5f }

    return points.map { point -> 1f - (point.value - minimum) / (maximum - minimum) }
}
