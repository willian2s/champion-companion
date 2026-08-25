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
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionBarChart
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionCartesianChartData
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartAxisSpec
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartFormatters
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartInteractionPolicy
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartPoint
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartReferenceLine
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartSeries
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartXAxisFormatter
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartXAxisSpec
import br.com.mwconsulting.championcompanion.core.model.CalendarStepsHistorySnapshot
import br.com.mwconsulting.championcompanion.core.model.CalendarHistoryPeriod
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun StepsHistoryChart(
    snapshot: CalendarStepsHistorySnapshot,
    modifier: Modifier = Modifier,
) {
    val entries = snapshot.entries
    val locale = LocalConfiguration.current.locales[0]
    val maximumSteps = maxOf(
        snapshot.stepsGoal.toDouble(),
        snapshot.aggregates.maximumSteps?.toDouble() ?: 0.0,
    )
    val numberFormat = NumberFormat.getIntegerInstance(locale)
    val recordedDayCount = entries.count { it.steps != null }
    val missingDayCount = entries.size - recordedDayCount
    val shortDateFormatter = shortDateFormatter(locale)
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    val gapCount = entries.count { it.steps == null }
    val gapLabels = entries
        .filter { it.steps == null }
        .joinToString(limit = 5, truncated = "…") { shortDateFormatter.format(it.localDate) }
        .let { labels ->
            if (gapCount > 5) "$labels e mais ${gapCount - 5}" else labels
        }
    val chartData = ChampionCartesianChartData(
        series = listOf(
            ChampionChartSeries(
                key = "steps",
                label = "Passos",
                points = entries.mapIndexed { index, entry ->
                    val date = dateFormatter.format(entry.localDate)
                    val steps = entry.steps
                    ChampionChartPoint(
                        x = index.toDouble(),
                        y = steps?.toDouble(),
                        xLabel = date,
                        contentDescription = "$date: ${steps?.let { "${numberFormat.format(it)} passos registrados" } ?: "sem dados"}.",
                    )
                },
            ),
        ),
        xAxis = ChampionChartXAxisSpec(
            formatter = ChampionChartXAxisFormatter { value ->
                entries[value.roundToInt().coerceIn(entries.indices)].localDate.let(shortDateFormatter::format)
            },
            title = "Data",
            minimum = 0.0,
            maximum = entries.lastIndex.toDouble(),
        ),
        yAxis = ChampionChartAxisSpec(
            title = "Passos",
            formatter = ChampionChartFormatters.integer(),
            minimum = 0.0,
            maximum = maximumSteps,
        ),
        referenceLines = listOf(
            ChampionChartReferenceLine(
                value = snapshot.stepsGoal.toDouble(),
                label = "Meta atual",
                contentDescription = "Meta atual: ${numberFormat.format(snapshot.stepsGoal)} passos",
            ),
        ),
        interaction = ChampionChartInteractionPolicy(
            scrollEnabled = entries.size > CalendarHistoryPeriod.LAST_7_DAYS.windowDays.toInt(),
        ),
        accessibilitySummary = "Gráfico de barras de passos para ${entries.size} dias. " +
            "Meta atual: ${numberFormat.format(snapshot.stepsGoal)} passos. " +
            "$recordedDayCount dias registrados e $missingDayCount dias sem dados. " +
            "Lacunas não representam zero passos. Valores diários estão disponíveis na lista acessível.",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        ChampionBarChart(
            data = chartData,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
        )
        Text(
            text = "Barras: passos registrados • linha: meta atual • lacuna: sem dados",
            modifier = Modifier.padding(top = ChampionSpacing.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        if (gapLabels.isNotEmpty()) {
            Text(
                text = "Lacunas: $gapLabels (sem dados; não representam zero passos)",
                modifier = Modifier.padding(top = ChampionSpacing.extraSmall),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun shortDateFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM", locale)
