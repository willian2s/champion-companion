package br.com.mwconsulting.championcompanion.feature.health

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
import br.com.mwconsulting.championcompanion.core.model.CalendarHistoryPeriod
import java.time.YearMonth
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun DistanceHistoryChart(
    displayEntries: List<DistanceHistoryDisplayEntry>,
    period: CalendarHistoryPeriod,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 190.dp,
    chartMinimumHeight: Dp = 192.dp,
) {
    val entries = displayEntries
    val locale = LocalConfiguration.current.locales[0]
    val maximumDistanceKm = maxOf(
        entries.mapNotNull { it.estimatedDistanceMeters }.maxOrNull()?.div(1000.0) ?: 0.0,
        1.0,
    )
    val estimatedEntryCount = entries.count { it.estimatedDistanceMeters != null }
    val missingEntryCount = entries.size - estimatedEntryCount
    val distanceFormatter = ChampionChartFormatters.decimal(maximumFractionDigits = 1)
    val originLabel = entries
        .flatMap { it.origins }
        .distinct()
        .joinToString(", ") { it.toChartOriginLabel() }
        .ifBlank { "não disponível" }
    val periodLabel = when (period) {
        CalendarHistoryPeriod.LAST_7_DAYS -> "semana civil atual"
        CalendarHistoryPeriod.LAST_30_DAYS -> "mês calendário fechado anterior"
        CalendarHistoryPeriod.LAST_90_DAYS -> "três meses calendário fechados anteriores"
        CalendarHistoryPeriod.LAST_365_DAYS -> "doze meses calendário fechados anteriores"
    }
    val chartEntryCountLabel = when (period) {
        CalendarHistoryPeriod.LAST_7_DAYS -> "dias"
        CalendarHistoryPeriod.LAST_30_DAYS -> "dias"
        CalendarHistoryPeriod.LAST_90_DAYS,
        CalendarHistoryPeriod.LAST_365_DAYS,
        -> "meses"
    }
    val chartData = ChampionCartesianChartData(
        series = listOf(
            ChampionChartSeries(
                key = "distance",
                label = "Distância",
                points = entries.mapIndexed { index, entry ->
                    val date = formatDistanceChartLabel(entry, index, period, locale)
                    val distanceKm = entry.estimatedDistanceMeters?.div(1000.0)
                    ChampionChartPoint(
                        x = distanceChartX(entry, index, period),
                        y = distanceKm,
                        xLabel = date,
                        contentDescription = "$date: ${distanceKm?.let { "${distanceFormatter.format(it)} km" } ?: "sem estimativa"}.",
                    )
                },
            ),
        ),
        xAxis = ChampionChartXAxisSpec(
            formatter = ChampionChartXAxisFormatter { value ->
                formatDistanceAxisLabel(value, entries, period, locale)
            },
            title = if (period == CalendarHistoryPeriod.LAST_90_DAYS ||
                period == CalendarHistoryPeriod.LAST_365_DAYS
            ) {
                "Mês"
            } else {
                "Dia"
            },
            minimum = distanceChartXBounds(entries, period).first,
            maximum = distanceChartXBounds(entries, period).second,
            visible = true,
            gridLinesVisible = false,
            labelValues = when (period) {
                CalendarHistoryPeriod.LAST_30_DAYS -> entries
                    .map { it.fromInclusive.dayOfMonth }
                    .filter { it in distanceHistoryMonthAxisDays(entries.first().fromInclusive.let(YearMonth::from)) }
                    .map(Int::toDouble)
                else -> entries.indices.map(Int::toDouble)
            },
        ),
        yAxis = ChampionChartAxisSpec(
            title = "Distância",
            formatter = distanceFormatter,
            unit = ChampionChartUnit("km"),
            minimum = 0.0,
            maximum = maximumDistanceKm,
            visible = false,
            gridLinesVisible = false,
        ),
        interaction = ChampionChartInteractionPolicy(
            scrollEnabled = false,
        ),
        accessibilitySummary = "Gráfico de barras de distância estimada para $periodLabel, com ${entries.size} $chartEntryCountLabel. " +
            "$estimatedEntryCount $chartEntryCountLabel com estimativa e $missingEntryCount $chartEntryCountLabel sem estimativa. " +
            "Origem: $originLabel. " +
            "Lacunas não representam zero quilômetros. Os mesmos dados locais estão disponíveis na lista, com a agregação correspondente ao período.",
    )

    ChampionBarChart(
        data = chartData,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(chartHeight),
        minimumHeight = chartMinimumHeight,
        columnWidth = 4.dp,
        columnCollectionSpacing = 1.dp,
        groupedColumnSpacing = 1.dp,
    )
}

private fun br.com.mwconsulting.championcompanion.core.model.DataOrigin.toChartOriginLabel(): String = when (this) {
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.WATCH_DAILY_SUMMARY -> "relógio"
    else -> "resumo local"
}

private fun formatDistanceChartLabel(
    entry: DistanceHistoryDisplayEntry,
    index: Int,
    period: CalendarHistoryPeriod,
    @Suppress("UNUSED_PARAMETER") locale: Locale,
): String = when (period) {
    CalendarHistoryPeriod.LAST_7_DAYS -> DISTANCE_HISTORY_WEEKDAY_LABELS.getOrNull(index) ?: "--"
        CalendarHistoryPeriod.LAST_30_DAYS -> entry.fromInclusive.dayOfMonth.toString()
    CalendarHistoryPeriod.LAST_90_DAYS -> formatDistanceMonthAbbreviation(entry.fromInclusive)
    CalendarHistoryPeriod.LAST_365_DAYS -> formatDistanceMonthInitial(entry.fromInclusive, locale)
}

private fun distanceChartX(
    entry: DistanceHistoryDisplayEntry,
    index: Int,
    period: CalendarHistoryPeriod,
): Double = when (period) {
    CalendarHistoryPeriod.LAST_7_DAYS,
    CalendarHistoryPeriod.LAST_90_DAYS,
    CalendarHistoryPeriod.LAST_365_DAYS,
    -> index.toDouble()
    CalendarHistoryPeriod.LAST_30_DAYS -> entry.fromInclusive.dayOfMonth.toDouble()
}

private fun formatDistanceAxisLabel(
    value: Double,
    entries: List<DistanceHistoryDisplayEntry>,
    period: CalendarHistoryPeriod,
    @Suppress("UNUSED_PARAMETER") locale: Locale,
): String {
    if (!value.isFinite()) return "--"
    val index = value.roundToInt()
    return when (period) {
        CalendarHistoryPeriod.LAST_7_DAYS ->
            DISTANCE_HISTORY_WEEKDAY_LABELS.getOrNull(index) ?: "--"

        CalendarHistoryPeriod.LAST_30_DAYS -> {
            val month = entries.firstOrNull()?.let { YearMonth.from(it.fromInclusive) }
            val day = index.takeIf { month != null && it in distanceHistoryMonthAxisDays(month) }
            day?.toString() ?: "--"
        }

        CalendarHistoryPeriod.LAST_90_DAYS ->
            entries.getOrNull(index)?.let { formatDistanceMonthAbbreviation(it.fromInclusive) } ?: "--"
        CalendarHistoryPeriod.LAST_365_DAYS ->
            entries.getOrNull(index)?.let { formatDistanceMonthInitial(it.fromInclusive, locale) } ?: "--"
    }
}

private fun distanceChartXBounds(
    entries: List<DistanceHistoryDisplayEntry>,
    period: CalendarHistoryPeriod,
): Pair<Double, Double> = when (period) {
    CalendarHistoryPeriod.LAST_7_DAYS -> -0.5 to 6.5
    CalendarHistoryPeriod.LAST_30_DAYS -> {
        val lastDay = entries.maxOfOrNull { it.fromInclusive.dayOfMonth } ?: 1
        0.5 to lastDay + 0.5
    }
    CalendarHistoryPeriod.LAST_90_DAYS,
    CalendarHistoryPeriod.LAST_365_DAYS,
    -> {
        val count = entries.size.coerceAtLeast(1)
        -0.5 to count - 0.5
    }
}
