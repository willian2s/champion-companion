package br.com.mwconsulting.championcompanion.feature.health

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.DarkColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.HealthCard
import br.com.mwconsulting.championcompanion.core.designsystem.LightColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.LoadingState
import br.com.mwconsulting.championcompanion.core.model.CalendarDataSource
import br.com.mwconsulting.championcompanion.core.model.CalendarDistanceHistoryEntry
import br.com.mwconsulting.championcompanion.core.model.CalendarDistanceHistorySnapshot
import br.com.mwconsulting.championcompanion.core.model.CalendarHistoryPeriod
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun DistanceDetailScreen(
    onNavigateBack: () -> Unit,
    backLabel: String = "Voltar para Saúde",
    distanceHistoryViewModel: DistanceHistoryViewModel = activityDistanceHistoryViewModel(),
) {
    val historyState by distanceHistoryViewModel.state.collectAsState()
    LaunchedEffect(distanceHistoryViewModel) {
        while (true) {
            distanceHistoryViewModel.onIntent(DistanceHistoryIntent.Load)
            val now = ZonedDateTime.now(ZoneId.systemDefault())
            val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay((nextDay.toEpochSecond() - now.toEpochSecond()).coerceAtLeast(1L) * 1_000L)
        }
    }

    MetricDetailScaffold(
        title = "Distância",
        description = "Acompanhe estimativa diária e tendência dos resumos locais.",
        onNavigateBack = onNavigateBack,
        backLabel = backLabel,
    ) {
        DistanceHistoryShell(
            state = historyState,
            onIntent = distanceHistoryViewModel::onIntent,
        )
    }
}

@Composable
private fun DistanceHistoryShell(
    state: DistanceHistoryUiState,
    onIntent: (DistanceHistoryIntent) -> Unit,
) {
    HealthCard(title = "Histórico consolidado", modifier = Modifier.fillMaxWidth()) {
        DistanceHistoryPeriodSelector(
            selected = state.selectedPeriod,
            onSelect = { onIntent(DistanceHistoryIntent.SelectPeriod(it)) },
        )

        when {
            state.snapshot == null && state.isLoading -> LoadingState(
                label = "Carregando histórico de distância",
                message = "Consultando somente estimativas diárias locais",
                modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.medium),
            )

            state.snapshot == null && state.error != null -> ErrorState(
                title = "Histórico indisponível",
                message = state.error.toUserMessage(),
                actionLabel = "Tentar novamente",
                onAction = { onIntent(DistanceHistoryIntent.Retry) },
                modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.medium),
            )
        }
        state.snapshot?.let { snapshot ->
            val hasDailySummaryIssue = snapshot.issues.any { it.source == CalendarDataSource.DAILY_SUMMARY }
            val hasConfirmedDistance = snapshot.aggregates.daysWithEstimateCount > 0
            if (snapshot.issues.isNotEmpty()) {
                ErrorState(
                    title = if (hasDailySummaryIssue && !hasConfirmedDistance) {
                        "Histórico de distância indisponível"
                    } else {
                        "Dados parciais do histórico"
                    },
                    message = if (hasDailySummaryIssue && !hasConfirmedDistance) {
                        "A fonte de resumos locais não pôde ser consultada. Não é possível confirmar ausência de estimativas."
                    } else {
                        "Parte das fontes locais não pôde ser consultada. Estimativas confirmadas permanecem visíveis e lacunas não viram zero."
                    },
                    actionLabel = "Tentar novamente",
                    onAction = { onIntent(DistanceHistoryIntent.Retry) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (hasConfirmedDistance || !hasDailySummaryIssue) {
                val displayData = snapshot.toDistanceHistoryDisplayData()
                DistanceHistoryChart(
                    displayEntries = displayData.chartEntries,
                    period = snapshot.query.period,
                    modifier = Modifier.fillMaxWidth(),
                    chartHeight = 136.dp,
                    chartMinimumHeight = 136.dp,
                )
                DistanceHistoryList(
                    entries = displayData.listEntries,
                    period = snapshot.query.period,
                )
            }
        }
    }
}

@Composable
private fun DistanceHistoryPeriodSelector(
    selected: CalendarHistoryPeriod,
    onSelect: (CalendarHistoryPeriod) -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = ChampionSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
    ) {
        Text(
            text = "Período",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            listOf(
                CalendarHistoryPeriod.LAST_7_DAYS,
                CalendarHistoryPeriod.LAST_30_DAYS,
                CalendarHistoryPeriod.LAST_90_DAYS,
                CalendarHistoryPeriod.LAST_365_DAYS,
            ).forEach { period ->
                FilterChip(
                    selected = period == selected,
                    onClick = { onSelect(period) },
                    modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics {
                        this.selected = period == selected
                        contentDescription = period.toDistanceAccessibilityLabel()
                        stateDescription = if (period == selected) "Selecionado" else "Não selecionado"
                    },
                    label = {
                        Text(
                            text = period.toDistancePeriodLabel(),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            fontWeight = if (period == selected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    },
                )
            }
        }
    }
}

internal data class DistanceHistoryDisplayData(
    val chartEntries: List<DistanceHistoryDisplayEntry>,
    val listEntries: List<DistanceHistoryDisplayEntry>,
)

internal data class DistanceHistoryDisplayEntry(
    val fromInclusive: LocalDate,
    val untilExclusive: LocalDate,
    val estimatedDistanceMeters: Double?,
    val origins: Set<DataOrigin>,
    val updatedAt: Instant?,
    val bucketKind: DistanceHistoryBucketKind,
) {
    init {
        require(fromInclusive.isBefore(untilExclusive))
        require(
            estimatedDistanceMeters == null ||
                (estimatedDistanceMeters.isFinite() && estimatedDistanceMeters >= 0.0),
        )
    }
}

internal enum class DistanceHistoryBucketKind(
    val countLabel: String,
    val aggregationLabel: String,
) {
    DAY("dias", "por dia"),
    WEEK("semanas", "por semana"),
    MONTH("meses", "por mês"),
}

internal fun CalendarDistanceHistorySnapshot.toDistanceHistoryDisplayData(
    today: LocalDate = LocalDate.now(),
): DistanceHistoryDisplayData {
    val chartEntries = when (query.period) {
        CalendarHistoryPeriod.LAST_7_DAYS,
        CalendarHistoryPeriod.LAST_30_DAYS,
        -> entries.map { it.toDistanceHistoryDisplayEntry(DistanceHistoryBucketKind.DAY) }

        CalendarHistoryPeriod.LAST_90_DAYS,
        CalendarHistoryPeriod.LAST_365_DAYS,
        -> entries
            .groupBy { YearMonth.from(it.localDate) }
            .toSortedMap()
            .values
            .map { monthEntries ->
                monthEntries.toDistanceHistoryDisplayEntry(DistanceHistoryBucketKind.MONTH)
            }
    }
    val listEntries = when (query.period) {
        CalendarHistoryPeriod.LAST_7_DAYS -> chartEntries.filter {
            !it.fromInclusive.isAfter(today)
        }

        CalendarHistoryPeriod.LAST_30_DAYS -> entries
            .splitIntoCalendarWeeksThroughLastEntry()
            .map { weekEntries ->
                weekEntries.toDistanceHistoryDisplayEntry(DistanceHistoryBucketKind.WEEK)
            }

        CalendarHistoryPeriod.LAST_90_DAYS,
        CalendarHistoryPeriod.LAST_365_DAYS,
        -> chartEntries
    }
    return DistanceHistoryDisplayData(
        chartEntries = chartEntries,
        listEntries = listEntries,
    )
}

internal fun CalendarDistanceHistorySnapshot.toDistanceHistoryDisplayEntries(): List<DistanceHistoryDisplayEntry> =
    toDistanceHistoryDisplayData().chartEntries

private fun CalendarDistanceHistoryEntry.toDistanceHistoryDisplayEntry(
    bucketKind: DistanceHistoryBucketKind,
): DistanceHistoryDisplayEntry = DistanceHistoryDisplayEntry(
    fromInclusive = localDate,
    untilExclusive = localDate.plusDays(1),
    estimatedDistanceMeters = estimatedDistanceMeters,
    origins = origin?.let(::setOf).orEmpty(),
    updatedAt = updatedAt,
    bucketKind = bucketKind,
)

private fun List<CalendarDistanceHistoryEntry>.toDistanceHistoryDisplayEntry(
    bucketKind: DistanceHistoryBucketKind,
): DistanceHistoryDisplayEntry {
    require(isNotEmpty())
    val from = first().localDate
    val until = last().localDate.plusDays(1)
    val values = mapNotNull { it.estimatedDistanceMeters }
    val total = values.sumFiniteOrNull()
    val valueEntries = filter { it.estimatedDistanceMeters != null }
    val metadataEntries = valueEntries.ifEmpty { this }
    return DistanceHistoryDisplayEntry(
        fromInclusive = from,
        untilExclusive = until,
        estimatedDistanceMeters = total,
        origins = metadataEntries.mapNotNull { it.origin }.toSet(),
        updatedAt = metadataEntries.mapNotNull { it.updatedAt }.maxOrNull(),
        bucketKind = bucketKind,
    )
}

private fun List<CalendarDistanceHistoryEntry>.splitIntoCalendarWeeksThroughLastEntry(): List<List<CalendarDistanceHistoryEntry>> {
    if (isEmpty()) return emptyList()

    val month = YearMonth.from(first().localDate)
    val monthEnd = minOf(month.atEndOfMonth(), last().localDate)
    val entriesByDate = associateBy { it.localDate }
    val buckets = mutableListOf<List<CalendarDistanceHistoryEntry>>()
    var from = month.atDay(1)
    while (!from.isAfter(monthEnd)) {
        val until = minOf(
            monthEnd,
            from.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)),
        )
        val bucket = buildList {
            var date = from
            while (!date.isAfter(until)) {
                entriesByDate[date]?.let(::add)
                date = date.plusDays(1)
            }
        }
        buckets += bucket
        from = until.plusDays(1)
    }
    return buckets
}

private fun List<Double>.sumFiniteOrNull(): Double? {
    if (isEmpty()) return null

    var total = 0.0
    for (value in this) {
        if (!value.isFinite() || value < 0.0) return null
        total += value
        if (!total.isFinite()) return null
    }
    return total
}

@Composable
private fun DistanceHistoryList(
    entries: List<DistanceHistoryDisplayEntry>,
    period: CalendarHistoryPeriod,
) {
    val locale = LocalConfiguration.current.locales[0]
    val bucketKind = period.toDistanceListBucketKind()
    val visibleEntries = entries.asReversed().filter { it.estimatedDistanceMeters != null }

    Column(
        modifier = Modifier.padding(top = ChampionSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "${visibleEntries.size} ${bucketKind.countLabel} com dados",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        visibleEntries.forEach { entry ->
            val date = formatDistanceDisplayRange(entry, locale)
            val value = entry.estimatedDistanceMeters?.let { "${formatDistance(it)} • estimada" }
                ?: "Sem estimativa"
            val origin = entry.origins.joinToString(", ") { it.toDistanceOriginLabel() }.ifBlank { "indisponível" }
            val updatedAt = entry.updatedAt?.let(::formatDateTime) ?: "indisponível"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "$date. $value. Origem: $origin. Última atualização: $updatedAt."
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
                ) {
                    Text(text = date, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Origem: $origin",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "Última atualização: $updatedAt",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(modifier = Modifier.width(ChampionSpacing.medium))
                Text(
                    text = value,
                    color = entry.estimatedDistanceMeters?.let { distancePrimaryAccent() }
                        ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (visibleEntries.isEmpty()) {
            EmptyState(
                title = "Sem estimativas no período",
                message = "Nenhuma distância estimada foi encontrada. Lacunas permanecem sem valor.",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Suppress("UNUSED_PARAMETER")
internal fun formatDistanceDisplayRange(
    entry: DistanceHistoryDisplayEntry,
    locale: Locale,
    today: LocalDate = LocalDate.now(),
): String {
    return when (entry.bucketKind) {
        DistanceHistoryBucketKind.DAY -> formatDistanceDayLabel(entry.fromInclusive, today)
        DistanceHistoryBucketKind.WEEK -> {
            val formatter = DateTimeFormatter.ofPattern("dd/MM", DISTANCE_HISTORY_PT_BR)
            "${formatter.format(entry.fromInclusive)} - " +
                formatter.format(entry.untilExclusive.minusDays(1))
        }
        DistanceHistoryBucketKind.MONTH -> formatDistanceMonthAbbreviation(entry.fromInclusive)
    }
}

internal val DISTANCE_HISTORY_WEEKDAY_LABELS = listOf("D", "S", "T", "Q", "Q", "S", "S")

private val DISTANCE_HISTORY_MONTH_ABBREVIATIONS = listOf(
    "JAN",
    "FEV",
    "MAR",
    "ABR",
    "MAI",
    "JUN",
    "JUL",
    "AGO",
    "SET",
    "OUT",
    "NOV",
    "DEZ",
)

private val DISTANCE_HISTORY_WEEKDAY_NAMES = listOf(
    "Dom.",
    "Seg.",
    "Ter.",
    "Qua.",
    "Qui.",
    "Sex.",
    "Sáb.",
)

private val DISTANCE_HISTORY_MONTH_NAMES = listOf(
    "jan.",
    "fev.",
    "mar.",
    "abr.",
    "mai.",
    "jun.",
    "jul.",
    "ago.",
    "set.",
    "out.",
    "nov.",
    "dez.",
)

internal val DISTANCE_HISTORY_PT_BR: Locale = Locale.forLanguageTag("pt-BR")

internal fun formatDistanceDayLabel(date: LocalDate, today: LocalDate): String = when {
    date == today -> "Hoje"
    date == today.minusDays(1) -> "Ontem"
    else -> "${DISTANCE_HISTORY_WEEKDAY_NAMES[date.dayOfWeek.value % 7]} " +
        "${date.dayOfMonth} de ${DISTANCE_HISTORY_MONTH_NAMES[date.monthValue - 1]}"
}

internal fun formatDistanceMonthAbbreviation(date: LocalDate): String =
    DISTANCE_HISTORY_MONTH_ABBREVIATIONS[date.monthValue - 1]

@Suppress("UNUSED_PARAMETER")
internal fun formatDistanceMonthInitial(date: LocalDate, locale: Locale): String =
    formatDistanceMonthAbbreviation(date).first().toString()

internal fun distanceHistoryMonthAxisDays(
    month: YearMonth,
    lastAvailableDay: Int = month.lengthOfMonth(),
): Set<Int> = buildSet {
    val lastDay = lastAvailableDay.coerceIn(1, month.lengthOfMonth())
    addAll(listOf(1, 5, 10, 15, 20, 25).filter { it <= lastDay })
    add(lastDay)
}

private fun CalendarHistoryPeriod.toDistancePeriodLabel(): String = when (this) {
    CalendarHistoryPeriod.LAST_7_DAYS -> "S"
    CalendarHistoryPeriod.LAST_30_DAYS -> "M"
    CalendarHistoryPeriod.LAST_90_DAYS -> "3M"
    CalendarHistoryPeriod.LAST_365_DAYS -> "A"
}

private fun CalendarHistoryPeriod.toDistanceAccessibilityLabel(): String = when (this) {
    CalendarHistoryPeriod.LAST_7_DAYS -> "Semana civil atual, de domingo a sábado, com valores diários"
    CalendarHistoryPeriod.LAST_30_DAYS -> "Mês calendário atual até hoje, com gráfico diário e lista semanal"
    CalendarHistoryPeriod.LAST_90_DAYS -> "Mês atual até hoje e dois meses calendário anteriores, com valores mensais"
    CalendarHistoryPeriod.LAST_365_DAYS -> "Mês atual até hoje e onze meses calendário anteriores, com valores mensais"
}

private fun CalendarHistoryPeriod.toDistanceListBucketKind(): DistanceHistoryBucketKind = when (this) {
    CalendarHistoryPeriod.LAST_7_DAYS -> DistanceHistoryBucketKind.DAY
    CalendarHistoryPeriod.LAST_30_DAYS -> DistanceHistoryBucketKind.WEEK
    CalendarHistoryPeriod.LAST_90_DAYS,
    CalendarHistoryPeriod.LAST_365_DAYS,
    -> DistanceHistoryBucketKind.MONTH
}

private fun DistanceHistoryUiError.toUserMessage(): String = when (category) {
    DistanceHistoryUiErrorCategory.INVALID_LOCAL_DATA -> "Os dados locais de distância estão inválidos. Tente novamente."
    DistanceHistoryUiErrorCategory.SOURCE_UNAVAILABLE,
    DistanceHistoryUiErrorCategory.INTERNAL,
    -> "Não foi possível consultar o histórico local de distância. Tente novamente."
}

private fun DataOrigin.toDistanceOriginLabel(): String = when (this) {
    DataOrigin.WATCH_DAILY_SUMMARY -> "relógio"
    else -> "resumo diário local"
}

private fun formatDistance(meters: Double): String =
    "${distanceDecimalFormat().format(meters / 1000.0)} km"

private fun distanceDecimalFormat(): NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 0
}

@Composable
private fun distancePrimaryAccent() = if (isSystemInDarkTheme()) {
    DarkColorScheme.primary
} else {
    LightColorScheme.primary
}

@Composable
private fun activityDistanceHistoryViewModel(): DistanceHistoryViewModel {
    val activity = checkNotNull(LocalActivity.current) as ComponentActivity
    return androidx.compose.runtime.remember(activity) {
        ViewModelProvider(activity)[DistanceHistoryViewModel::class.java]
    }
}
