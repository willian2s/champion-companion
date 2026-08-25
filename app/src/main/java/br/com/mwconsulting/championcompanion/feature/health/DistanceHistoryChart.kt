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
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartSeries
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartUnit
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartXAxisFormatter
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartXAxisSpec
import br.com.mwconsulting.championcompanion.core.model.CalendarDistanceHistorySnapshot
import br.com.mwconsulting.championcompanion.core.model.CalendarHistoryPeriod
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun DistanceHistoryChart(
    snapshot: CalendarDistanceHistorySnapshot,
    modifier: Modifier = Modifier,
) {
    val entries = snapshot.entries
    val locale = LocalConfiguration.current.locales[0]
    val maximumDistanceKm = maxOf(
        snapshot.aggregates.maximumDistanceMeters?.div(1000.0) ?: 0.0,
        1.0,
    )
    val estimatedDayCount = entries.count { it.estimatedDistanceMeters != null }
    val missingDayCount = entries.size - estimatedDayCount
    val shortDateFormatter = shortDateFormatter(locale)
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    val distanceFormatter = ChampionChartFormatters.decimal(maximumFractionDigits = 1)
    val gapCount = entries.count { it.estimatedDistanceMeters == null }
    val gapLabels = entries
        .filter { it.estimatedDistanceMeters == null }
        .joinToString(limit = 5, truncated = "…") { shortDateFormatter.format(it.localDate) }
        .let { labels ->
            if (gapCount > 5) "$labels e mais ${gapCount - 5}" else labels
        }
    val chartData = ChampionCartesianChartData(
        series = listOf(
            ChampionChartSeries(
                key = "distance",
                label = "Distância",
                points = entries.mapIndexed { index, entry ->
                    val date = dateFormatter.format(entry.localDate)
                    val distanceKm = entry.estimatedDistanceMeters?.div(1000.0)
                    ChampionChartPoint(
                        x = index.toDouble(),
                        y = distanceKm,
                        xLabel = date,
                        contentDescription = "$date: ${distanceKm?.let { "${distanceFormatter.format(it)} km" } ?: "sem estimativa"}.",
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
            title = "Distância",
            formatter = distanceFormatter,
            unit = ChampionChartUnit("km"),
            minimum = 0.0,
            maximum = maximumDistanceKm,
        ),
        interaction = ChampionChartInteractionPolicy(
            scrollEnabled = entries.size > CalendarHistoryPeriod.LAST_7_DAYS.windowDays.toInt(),
        ),
        accessibilitySummary = "Gráfico de barras de distância estimada para ${entries.size} dias. " +
            "$estimatedDayCount dias com estimativa e $missingDayCount dias sem estimativa. " +
            "Lacunas não representam zero quilômetros. Valores diários estão disponíveis na lista acessível.",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        ChampionBarChart(
            data = chartData,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
        )
        Text(
            text = "Barras: distância estimada • eixo/tooltip: km • lacuna: sem estimativa",
            modifier = Modifier.padding(top = ChampionSpacing.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        if (gapLabels.isNotEmpty()) {
            Text(
                text = "Lacunas: $gapLabels (sem estimativa; não representam zero quilômetros)",
                modifier = Modifier.padding(top = ChampionSpacing.extraSmall),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun shortDateFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM", locale)
