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
import androidx.compose.ui.unit.Dp
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
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartUnit
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartXAxisFormatter
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartXAxisSpec
import br.com.mwconsulting.championcompanion.core.model.CalendarHistoryPeriod
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun StepsHistoryChart(
    displayEntries: List<StepsHistoryDisplayEntry>,
    period: CalendarHistoryPeriod,
    stepsGoal: Int,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 190.dp,
    chartMinimumHeight: Dp = 192.dp,
) {
    val entries = displayEntries
    val locale = LocalConfiguration.current.locales[0]
    val maximumSteps = maxOf(
        stepsGoal.toDouble(),
        entries.mapNotNull { it.steps }.maxOrNull()?.toDouble() ?: 0.0,
        1.0,
    )
    val numberFormat = java.text.NumberFormat.getIntegerInstance(locale)
    val recordedEntryCount = entries.count { it.steps != null }
    val missingEntryCount = entries.size - recordedEntryCount
    val originLabel = entries
        .flatMap { it.origins }
        .distinct()
        .joinToString(", ") { it.toStepsChartOriginLabel() }
        .ifBlank { "não disponível" }
    val gapLabels = entries
        .filter { it.steps == null }
        .joinToString(limit = 5, truncated = "…") {
            DateTimeFormatter.ofPattern("dd/MM", locale).format(it.fromInclusive)
        }
    val chartData = ChampionCartesianChartData(
        series = listOf(
            ChampionChartSeries(
                key = "steps",
                label = "Passos • Origem: $originLabel",
                points = entries.mapIndexed { index, entry ->
                    val label = formatStepsChartLabel(entry, index, period)
                    val steps = entry.steps
                    ChampionChartPoint(
                        x = stepsChartX(entry, index, period),
                        y = steps?.toDouble(),
                        xLabel = label,
                        contentDescription = "$label: ${steps?.let { "${numberFormat.format(it)} passos registrados" } ?: "sem dados"}.",
                    )
                },
            ),
        ),
        xAxis = ChampionChartXAxisSpec(
            formatter = ChampionChartXAxisFormatter { value ->
                formatStepsAxisLabel(value, entries, period)
            },
            title = if (period == CalendarHistoryPeriod.LAST_90_DAYS ||
                period == CalendarHistoryPeriod.LAST_365_DAYS
            ) "Mês" else "Dia",
            minimum = stepsChartXBounds(entries, period).first,
            maximum = stepsChartXBounds(entries, period).second,
            visible = true,
            gridLinesVisible = false,
            labelValues = when (period) {
                CalendarHistoryPeriod.LAST_30_DAYS -> {
                    val month = entries.firstOrNull()?.fromInclusive?.let(YearMonth::from)
                    val lastDay = entries.maxOfOrNull { it.fromInclusive.dayOfMonth }
                    if (month == null) {
                        emptyList()
                    } else {
                        stepsHistoryMonthAxisDays(month, lastDay ?: month.lengthOfMonth())
                            .map(Int::toDouble)
                    }
                }
                else -> entries.indices.map(Int::toDouble)
            },
        ),
        yAxis = ChampionChartAxisSpec(
            title = "Passos",
            formatter = ChampionChartFormatters.integer(),
            unit = ChampionChartUnit("passos"),
            minimum = 0.0,
            maximum = maximumSteps,
            visible = false,
            gridLinesVisible = false,
        ),
        referenceLines = if (period == CalendarHistoryPeriod.LAST_90_DAYS ||
            period == CalendarHistoryPeriod.LAST_365_DAYS
        ) {
            emptyList()
        } else {
            listOf(
                ChampionChartReferenceLine(
                    value = stepsGoal.toDouble(),
                    label = "Meta atual",
                    contentDescription = "Meta atual: ${numberFormat.format(stepsGoal)} passos",
                ),
            )
        },
        interaction = ChampionChartInteractionPolicy(
            scrollEnabled = false,
        ),
        accessibilitySummary = "Gráfico de barras de passos para ${stepsPeriodDescription(period)}, com " +
            "${entries.size} ${period.toStepsChartEntryLabel()}. Meta atual: " +
            "${numberFormat.format(stepsGoal)} passos. Origem: $originLabel. " +
            "$recordedEntryCount ${period.toStepsChartEntryLabel()} com dados e " +
            "$missingEntryCount sem dados. Lacunas não representam zero passos. " +
            "A linha de meta aparece nos gráficos diários. Valores estão disponíveis " +
            "na lista acessível.",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        ChampionBarChart(
            data = chartData,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(chartHeight),
            minimumHeight = chartMinimumHeight,
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

private fun formatStepsChartLabel(
    entry: StepsHistoryDisplayEntry,
    index: Int,
    period: CalendarHistoryPeriod,
): String = when (period) {
    CalendarHistoryPeriod.LAST_7_DAYS -> STEPS_HISTORY_WEEKDAY_LABELS.getOrNull(index) ?: "--"
    CalendarHistoryPeriod.LAST_30_DAYS -> entry.fromInclusive.dayOfMonth.toString()
    CalendarHistoryPeriod.LAST_90_DAYS -> STEPS_HISTORY_MONTH_LABELS[entry.fromInclusive.monthValue - 1]
    CalendarHistoryPeriod.LAST_365_DAYS -> STEPS_HISTORY_MONTH_LABELS[entry.fromInclusive.monthValue - 1].first().toString()
}

private fun stepsChartX(
    entry: StepsHistoryDisplayEntry,
    index: Int,
    period: CalendarHistoryPeriod,
): Double = if (period == CalendarHistoryPeriod.LAST_30_DAYS) {
    entry.fromInclusive.dayOfMonth.toDouble()
} else {
    index.toDouble()
}

private fun formatStepsAxisLabel(
    value: Double,
    entries: List<StepsHistoryDisplayEntry>,
    period: CalendarHistoryPeriod,
): String {
    if (!value.isFinite()) return "--"
    val index = value.roundToInt()
    return when (period) {
        CalendarHistoryPeriod.LAST_7_DAYS -> STEPS_HISTORY_WEEKDAY_LABELS.getOrNull(index) ?: "--"
        CalendarHistoryPeriod.LAST_30_DAYS -> {
            val month = entries.firstOrNull()?.fromInclusive?.let(YearMonth::from)
            val day = index.takeIf {
                month != null && it in stepsHistoryMonthAxisDays(
                    month,
                    entries.maxOfOrNull { entry -> entry.fromInclusive.dayOfMonth } ?: 1,
                )
            }
            day?.toString() ?: "--"
        }
        CalendarHistoryPeriod.LAST_90_DAYS,
        CalendarHistoryPeriod.LAST_365_DAYS,
        -> entries.getOrNull(index)?.let {
            val abbreviation = STEPS_HISTORY_MONTH_LABELS[it.fromInclusive.monthValue - 1]
            if (period == CalendarHistoryPeriod.LAST_365_DAYS) abbreviation.first().toString() else abbreviation
        } ?: "--"
    }
}

private fun stepsChartXBounds(
    entries: List<StepsHistoryDisplayEntry>,
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

private fun stepsHistoryMonthAxisDays(month: YearMonth, lastAvailableDay: Int): Set<Int> = buildSet {
    val lastDay = lastAvailableDay.coerceIn(1, month.lengthOfMonth())
    addAll(listOf(1, 5, 10, 15, 20, 25).filter { it <= lastDay })
    add(lastDay)
}

private fun stepsPeriodDescription(period: CalendarHistoryPeriod): String = when (period) {
    CalendarHistoryPeriod.LAST_7_DAYS -> "a semana civil atual"
    CalendarHistoryPeriod.LAST_30_DAYS -> "o mês civil atual até hoje"
    CalendarHistoryPeriod.LAST_90_DAYS -> "o mês atual até hoje e os dois meses civis anteriores"
    CalendarHistoryPeriod.LAST_365_DAYS -> "o mês atual até hoje e os onze meses civis anteriores"
}

private fun CalendarHistoryPeriod.toStepsChartEntryLabel(): String = when (this) {
    CalendarHistoryPeriod.LAST_7_DAYS,
    CalendarHistoryPeriod.LAST_30_DAYS,
    -> "dias"
    CalendarHistoryPeriod.LAST_90_DAYS,
    CalendarHistoryPeriod.LAST_365_DAYS,
    -> "meses"
}

private val STEPS_HISTORY_WEEKDAY_LABELS = listOf("D", "S", "T", "Q", "Q", "S", "S")
private val STEPS_HISTORY_MONTH_LABELS = listOf(
    "JAN", "FEV", "MAR", "ABR", "MAI", "JUN",
    "JUL", "AGO", "SET", "OUT", "NOV", "DEZ",
)

private fun br.com.mwconsulting.championcompanion.core.model.DataOrigin.toStepsChartOriginLabel(): String = when (this) {
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.WATCH_DAILY_SUMMARY -> "relógio"
    else -> "resumo local"
}
