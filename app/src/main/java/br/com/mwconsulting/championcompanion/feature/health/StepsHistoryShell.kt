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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.HealthCard
import br.com.mwconsulting.championcompanion.core.designsystem.LoadingState
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignSummaryCard
import br.com.mwconsulting.championcompanion.core.model.CalendarDataSource
import br.com.mwconsulting.championcompanion.core.model.CalendarStepsHistoryEntry
import br.com.mwconsulting.championcompanion.core.model.CalendarHistoryPeriod
import br.com.mwconsulting.championcompanion.core.model.CalendarStepsHistorySnapshot
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val STEPS_HISTORY_PERIODS = listOf(
    CalendarHistoryPeriod.LAST_7_DAYS to "S",
    CalendarHistoryPeriod.LAST_30_DAYS to "M",
    CalendarHistoryPeriod.LAST_90_DAYS to "3M",
    CalendarHistoryPeriod.LAST_365_DAYS to "A",
)

@Composable
fun StepsDetailScreen(
    onNavigateBack: () -> Unit,
    backLabel: String = "Voltar para Saúde",
    stepsHistoryViewModel: StepsHistoryViewModel = activityStepsHistoryViewModel(),
) {
    val historyState by stepsHistoryViewModel.state.collectAsState()

    LaunchedEffect(stepsHistoryViewModel) {
        stepsHistoryViewModel.onIntent(StepsHistoryIntent.Load)
    }

    MetricDetailScaffold(
        title = "Passos",
        description = "Acompanhe passos, meta e tendência dos seus resumos locais.",
        onNavigateBack = onNavigateBack,
        backLabel = backLabel,
    ) {
        StepsHistoryShell(
            state = historyState,
            onIntent = stepsHistoryViewModel::onIntent,
        )
    }
}

@Composable
private fun StepsHistoryShell(
    state: StepsHistoryUiState,
    onIntent: (StepsHistoryIntent) -> Unit,
) {
    HealthCard(title = "Histórico consolidado", modifier = Modifier.fillMaxWidth()) {
        StepsHistoryPeriodSelector(
            selected = state.selectedPeriod,
            onSelect = { onIntent(StepsHistoryIntent.SelectPeriod(it)) },
        )

        when {
            state.snapshot == null && state.isLoading -> LoadingState(
                label = "Carregando histórico de passos",
                message = "Consultando somente resumos diários locais",
                modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.medium),
            )

            state.snapshot == null && state.error != null -> ErrorState(
                title = "Histórico indisponível",
                message = state.error.toUserMessage(),
                actionLabel = "Tentar novamente",
                onAction = { onIntent(StepsHistoryIntent.Retry) },
                modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.medium),
            )
        }
    }

    state.snapshot?.let { snapshot ->
        val hasDailySummaryIssue = snapshot.issues.any { it.source == CalendarDataSource.DAILY_SUMMARY }
        val hasConfirmedSteps = snapshot.aggregates.recordedDayCount > 0
        if (snapshot.issues.isNotEmpty()) {
            ErrorState(
                title = if (hasDailySummaryIssue && !hasConfirmedSteps) {
                    "Histórico de passos indisponível"
                } else {
                    "Dados parciais do histórico"
                },
                message = if (hasDailySummaryIssue && !hasConfirmedSteps) {
                    "A fonte de resumos locais não pôde ser consultada. Não é possível confirmar ausência de registros."
                } else {
                    "Parte das fontes locais não pôde ser consultada. Dados confirmados permanecem visíveis e lacunas não viram zero."
                },
                actionLabel = "Tentar novamente",
                onAction = { onIntent(StepsHistoryIntent.Retry) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (hasConfirmedSteps || !hasDailySummaryIssue) {
            val displayData = snapshot.toStepsHistoryDisplayData()
            val aggregates = snapshot.aggregates
            ChampionRedesignSummaryCard(
                title = "Tendência",
                value = aggregates.totalSteps?.let(::formatSteps) ?: "Sem dados",
                supportingText = "Total no período • ${snapshot.query.dateRange.dates.size} dias",
                modifier = Modifier.fillMaxWidth(),
            ) {
                StepsHistoryChart(
                    displayEntries = displayData.chartEntries,
                    period = snapshot.query.period,
                    stepsGoal = snapshot.stepsGoal,
                    modifier = Modifier.padding(top = ChampionSpacing.medium),
                )
            }
            HealthCard(title = "Lista acessível", modifier = Modifier.fillMaxWidth()) {
                StepsHistoryList(
                    entries = displayData.listEntries,
                    period = snapshot.query.period,
                    stepsGoal = snapshot.stepsGoal,
                )
            }
        }
    }
}

@Composable
private fun StepsHistoryPeriodSelector(
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
            STEPS_HISTORY_PERIODS.forEach { (period, label) ->
                FilterChip(
                    selected = period == selected,
                    onClick = { onSelect(period) },
                    modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics {
                        this.selected = period == selected
                        contentDescription = period.toStepsAccessibilityLabel()
                        stateDescription = if (period == selected) "Selecionado" else "Não selecionado"
                    },
                    label = {
                        Text(
                            text = label,
                            maxLines = 1,
                            fontWeight = if (period == selected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun StepsHistoryList(
    entries: List<StepsHistoryDisplayEntry>,
    period: CalendarHistoryPeriod,
    stepsGoal: Int,
) {
    val locale = LocalConfiguration.current.locales[0]
    val visibleEntries = entries.asReversed()
    val bucketLabel = period.toStepsBucketKind().countLabel

    Column(
        modifier = Modifier.padding(top = ChampionSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        Text(
            text = "Meta atual: ${formatSteps(stepsGoal.toLong())}",
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = "Meta atual: ${formatSteps(stepsGoal.toLong())}"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "${visibleEntries.size} $bucketLabel com dados no período."
                },
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "${stepsNumberFormat().format(visibleEntries.size)} $bucketLabel com dados",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        visibleEntries.forEach { entry ->
            val date = formatStepsDisplayRange(entry, locale)
            val value = formatSteps(checkNotNull(entry.steps))
            val origin = entry.origins.joinToString(", ") { it.toStepsOriginLabel() }
                .ifBlank { "indisponível" }
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (visibleEntries.isEmpty()) {
            EmptyState(
                title = "Sem dados no período",
                message = "Nenhum resumo diário local foi encontrado. Lacunas permanecem sem valor.",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

internal data class StepsHistoryDisplayData(
    val chartEntries: List<StepsHistoryDisplayEntry>,
    val listEntries: List<StepsHistoryDisplayEntry>,
)

internal data class StepsHistoryDisplayEntry(
    val fromInclusive: LocalDate,
    val untilExclusive: LocalDate,
    val steps: Long?,
    val origins: Set<DataOrigin>,
    val updatedAt: Instant?,
    val bucketKind: StepsHistoryBucketKind,
) {
    init {
        require(fromInclusive.isBefore(untilExclusive))
        require(steps == null || steps >= 0)
    }
}

internal enum class StepsHistoryBucketKind(val countLabel: String) {
    DAY("dias"),
    WEEK("semanas"),
    MONTH("meses"),
}

internal fun CalendarStepsHistorySnapshot.toStepsHistoryDisplayData(
    today: LocalDate = LocalDate.now(query.zoneId),
): StepsHistoryDisplayData {
    val chartEntries = when (query.period) {
        CalendarHistoryPeriod.LAST_7_DAYS,
        CalendarHistoryPeriod.LAST_30_DAYS,
        -> entries.map { it.toStepsHistoryDisplayEntry(StepsHistoryBucketKind.DAY) }

        CalendarHistoryPeriod.LAST_90_DAYS,
        CalendarHistoryPeriod.LAST_365_DAYS,
        -> entries
            .groupBy { YearMonth.from(it.localDate) }
            .toSortedMap()
            .values
            .map { it.toStepsHistoryDisplayEntry(StepsHistoryBucketKind.MONTH) }
    }
    val listEntries = when (query.period) {
        CalendarHistoryPeriod.LAST_7_DAYS -> chartEntries.filter {
            !it.fromInclusive.isAfter(today) && it.steps != null
        }

        CalendarHistoryPeriod.LAST_30_DAYS -> entries
            .splitIntoStepsCalendarWeeks()
            .map { it.toStepsHistoryDisplayEntry(StepsHistoryBucketKind.WEEK) }
            .filter { it.steps != null }

        CalendarHistoryPeriod.LAST_90_DAYS,
        CalendarHistoryPeriod.LAST_365_DAYS,
        -> chartEntries.filter { it.steps != null }
    }
    return StepsHistoryDisplayData(chartEntries, listEntries)
}

private fun CalendarStepsHistoryEntry.toStepsHistoryDisplayEntry(
    bucketKind: StepsHistoryBucketKind,
): StepsHistoryDisplayEntry = StepsHistoryDisplayEntry(
    fromInclusive = localDate,
    untilExclusive = localDate.plusDays(1),
    steps = steps,
    origins = origin?.let(::setOf).orEmpty(),
    updatedAt = updatedAt,
    bucketKind = bucketKind,
)

private fun List<CalendarStepsHistoryEntry>.toStepsHistoryDisplayEntry(
    bucketKind: StepsHistoryBucketKind,
): StepsHistoryDisplayEntry {
    require(isNotEmpty())
    val values = mapNotNull { it.steps }
    val valueEntries = filter { it.steps != null }
    val metadataEntries = valueEntries.ifEmpty { this }
    return StepsHistoryDisplayEntry(
        fromInclusive = first().localDate,
        untilExclusive = last().localDate.plusDays(1),
        steps = values.sumStepsOrNull(),
        origins = metadataEntries.mapNotNull { it.origin }.toSet(),
        updatedAt = metadataEntries.mapNotNull { it.updatedAt }.maxOrNull(),
        bucketKind = bucketKind,
    )
}

private fun List<CalendarStepsHistoryEntry>.splitIntoStepsCalendarWeeks(): List<List<CalendarStepsHistoryEntry>> {
    if (isEmpty()) return emptyList()
    val month = YearMonth.from(first().localDate)
    val monthEnd = minOf(month.atEndOfMonth(), last().localDate)
    val entriesByDate = associateBy { it.localDate }
    val buckets = mutableListOf<List<CalendarStepsHistoryEntry>>()
    var from = month.atDay(1)
    while (!from.isAfter(monthEnd)) {
        val until = minOf(monthEnd, from.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)))
        buckets.add(buildList {
            var date = from
            while (!date.isAfter(until)) {
                entriesByDate[date]?.let { add(it) }
                date = date.plusDays(1)
            }
        })
        from = until.plusDays(1)
    }
    return buckets
}

private fun List<Long>.sumStepsOrNull(): Long? {
    if (isEmpty()) return null
    var total = 0L
    for (value in this) {
        total = try {
            Math.addExact(total, value)
        } catch (_: ArithmeticException) {
            return null
        }
    }
    return total
}

private fun formatStepsDisplayRange(entry: StepsHistoryDisplayEntry, locale: Locale): String = when (entry.bucketKind) {
    StepsHistoryBucketKind.DAY -> {
        val today = LocalDate.now()
        when (entry.fromInclusive) {
            today -> "Hoje"
            today.minusDays(1) -> "Ontem"
            else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(entry.fromInclusive)
        }
    }
    StepsHistoryBucketKind.WEEK -> {
        val formatter = DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-BR"))
        "${formatter.format(entry.fromInclusive)} - ${formatter.format(entry.untilExclusive.minusDays(1))}"
    }
    StepsHistoryBucketKind.MONTH -> STEPS_HISTORY_MONTH_LABELS[entry.fromInclusive.monthValue - 1]
}

private val STEPS_HISTORY_MONTH_LABELS = listOf(
    "JAN", "FEV", "MAR", "ABR", "MAI", "JUN",
    "JUL", "AGO", "SET", "OUT", "NOV", "DEZ",
)

private fun CalendarHistoryPeriod.toStepsBucketKind(): StepsHistoryBucketKind = when (this) {
    CalendarHistoryPeriod.LAST_7_DAYS -> StepsHistoryBucketKind.DAY
    CalendarHistoryPeriod.LAST_30_DAYS -> StepsHistoryBucketKind.WEEK
    CalendarHistoryPeriod.LAST_90_DAYS,
    CalendarHistoryPeriod.LAST_365_DAYS,
    -> StepsHistoryBucketKind.MONTH
}

private fun CalendarHistoryPeriod.toStepsAccessibilityLabel(): String = when (this) {
    CalendarHistoryPeriod.LAST_7_DAYS -> "Semana civil atual, de domingo a sábado"
    CalendarHistoryPeriod.LAST_30_DAYS -> "Mês civil atual, do primeiro dia até hoje"
    CalendarHistoryPeriod.LAST_90_DAYS -> "Mês atual até hoje e dois meses civis anteriores"
    CalendarHistoryPeriod.LAST_365_DAYS -> "Mês atual até hoje e onze meses civis anteriores"
}

private fun StepsHistoryUiError.toUserMessage(): String = when (category) {
    StepsHistoryUiErrorCategory.INVALID_LOCAL_DATA -> "Os dados locais de passos estão inválidos. Tente novamente."
    StepsHistoryUiErrorCategory.SOURCE_UNAVAILABLE,
    StepsHistoryUiErrorCategory.INTERNAL,
    -> "Não foi possível consultar o histórico local de passos. Tente novamente."
}

private fun DataOrigin.toStepsOriginLabel(): String = when (this) {
    DataOrigin.WATCH_DAILY_SUMMARY -> "relógio"
    else -> "resumo diário local"
}

private fun formatSteps(value: Long): String =
    "${NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)} passos"

@Composable
private fun activityStepsHistoryViewModel(): StepsHistoryViewModel {
    val activity = checkNotNull(LocalActivity.current) as ComponentActivity
    return androidx.compose.runtime.remember(activity) {
        ViewModelProvider(activity)[StepsHistoryViewModel::class.java]
    }
}

private fun stepsNumberFormat(): NumberFormat = NumberFormat.getIntegerInstance(Locale.getDefault())
