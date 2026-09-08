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
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.WeightMeasurement
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

internal fun filterWeightMeasurements(
    measurements: List<WeightMeasurement>,
    period: WeightHistoryPeriod,
    today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<WeightMeasurement> {
    val currentMonth = YearMonth.from(today)
    val firstMonth = currentMonth.minusMonths(period.monthCount)
    val from = firstMonth.atDay(1)
    val until = today.plusDays(1)
    return measurements
        .asSequence()
        .filter { measurement ->
            measurement.toKilogramsOrNull() != null
        }
        .filter { measurement ->
            val date = measurement.measuredAt.atZone(zoneId).toLocalDate()
            date >= from && date < until
        }
        .sortedByDescending(WeightMeasurement::measuredAt)
        .toList()
}

@Composable
internal fun WeightHistoryChart(
    measurements: List<WeightMeasurement>,
    period: WeightHistoryPeriod,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 190.dp,
    chartMinimumHeight: Dp = 192.dp,
) {
    val locale = LocalConfiguration.current.locales[0]
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    val visibleMeasurements = filterWeightMeasurements(measurements, period, today, zoneId)
        .sortedBy(WeightMeasurement::measuredAt)
    val firstMonth = YearMonth.from(today).minusMonths(period.monthCount)
    val chartEntries = visibleMeasurements.toWeightHistoryAggregates(
        period = period,
        firstMonth = firstMonth,
        zoneId = zoneId,
    )
    val chartPoints = chartEntries.mapIndexed { index, entry ->
        val label = formatWeightMonthLabel(entry.month, period, locale)
        ChampionChartPoint(
            x = index.toDouble(),
            y = entry.averageKilograms,
            xLabel = label,
            contentDescription = if (entry.averageKilograms == null) {
                "$label: sem dados."
            } else {
                "$label: ${formatWeightValue(entry.averageKilograms, locale)} kg em média, " +
                    "${entry.measurementCount} ${if (entry.measurementCount == 1) "medição" else "medições"}."
            },
        )
    }
    val originLabel = visibleMeasurements
        .map { it.origin }
        .distinct()
        .joinToString(", ") { it.toWeightChartOriginLabel() }
        .ifBlank { "não disponível" }
    val maximumWeightKilograms = maxOf(
        chartEntries.mapNotNull { it.averageKilograms }.maxOrNull() ?: 0.0,
        1.0,
    )
    val formatter = ChampionChartFormatters.decimal(maximumFractionDigits = 1)
    val chartData = ChampionCartesianChartData(
        series = listOf(
            ChampionChartSeries(
                key = "weight",
                label = "Peso • Origem: $originLabel",
                points = chartPoints,
            ),
        ),
        xAxis = ChampionChartXAxisSpec(
            formatter = ChampionChartXAxisFormatter { value ->
                formatWeightAxisLabel(value, chartEntries, period, locale)
            },
            title = "Data",
            minimum = -0.5,
            maximum = chartEntries.size - 0.5,
            visible = true,
            gridLinesVisible = false,
            labelValues = chartEntries.indices.map(Int::toDouble),
        ),
        yAxis = ChampionChartAxisSpec(
            title = "Peso",
            formatter = formatter,
            unit = ChampionChartUnit("kg"),
            minimum = 0.0,
            maximum = maximumWeightKilograms,
            visible = false,
            gridLinesVisible = false,
        ),
        interaction = ChampionChartInteractionPolicy(scrollEnabled = false),
        accessibilitySummary = "Gráfico de barras de peso médio mensal em quilogramas para " +
            "${period.toWeightPeriodDescription()}. ${visibleMeasurements.size} medições válidas " +
            "em ${chartEntries.size} meses. Origem: $originLabel. " +
            "Meses sem medições permanecem sem dados e não são interpolados.",
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

private data class WeightHistoryAggregate(
    val month: YearMonth,
    val averageKilograms: Double?,
    val measurementCount: Int,
)

private fun List<WeightMeasurement>.toWeightHistoryAggregates(
    period: WeightHistoryPeriod,
    firstMonth: YearMonth,
    zoneId: ZoneId,
): List<WeightHistoryAggregate> {
    val measurementsByMonth = groupBy { YearMonth.from(it.measuredAt.atZone(zoneId).toLocalDate()) }
    return (0..period.monthCount).map { monthOffset ->
        val month = firstMonth.plusMonths(monthOffset.toLong())
        val values = measurementsByMonth[month].orEmpty().mapNotNull(WeightMeasurement::toKilogramsOrNull)
        WeightHistoryAggregate(
            month = month,
            averageKilograms = values.takeIf { it.isNotEmpty() }?.average(),
            measurementCount = values.size,
        )
    }
}

private fun formatWeightAxisLabel(
    value: Double,
    entries: List<WeightHistoryAggregate>,
    period: WeightHistoryPeriod,
    locale: Locale,
): String {
    if (!value.isFinite()) return "--"
    val entry = entries.getOrNull(value.roundToInt()) ?: return "--"
    return formatWeightMonthLabel(entry.month, period, locale)
}

private fun formatWeightMonthLabel(
    month: YearMonth,
    period: WeightHistoryPeriod,
    locale: Locale,
): String {
    val date = month.atDay(1)
    val abbreviation = DateTimeFormatter.ofPattern("MMM", locale)
        .format(date)
        .replace(".", "")
        .uppercase(locale)
    return if (period == WeightHistoryPeriod.YEAR) abbreviation.first().toString() else abbreviation
}

private fun formatWeightValue(value: Double, locale: Locale): String =
    java.text.NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 1
        isGroupingUsed = false
    }.format(value)

private fun DataOrigin.toWeightChartOriginLabel(): String = when (this) {
    DataOrigin.MANUAL_ENTRY -> "entrada manual"
    DataOrigin.WATCH_DAILY_SUMMARY -> "relógio"
    DataOrigin.WATCH_D1_HISTORY -> "histórico do relógio"
    DataOrigin.WATCH_D3_D4 -> "relógio"
    DataOrigin.APP_WORKOUT_SESSION -> "sessão do app"
    DataOrigin.DERIVED -> "derivado"
}

private fun WeightHistoryPeriod.toWeightPeriodDescription(): String = when (this) {
    WeightHistoryPeriod.THREE_MONTHS -> "o mês atual até hoje e os dois meses civis anteriores"
    WeightHistoryPeriod.YEAR -> "o mês atual até hoje e os onze meses civis anteriores"
}

internal fun WeightHistoryPeriod.toWeightPeriodLabel(): String = when (this) {
    WeightHistoryPeriod.THREE_MONTHS -> "3M"
    WeightHistoryPeriod.YEAR -> "A"
}

private val WeightHistoryPeriod.monthCount: Long
    get() = when (this) {
        WeightHistoryPeriod.THREE_MONTHS -> 2L
        WeightHistoryPeriod.YEAR -> 11L
    }
