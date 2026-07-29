package br.com.mwconsulting.championcompanion.feature.health

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import br.com.mwconsulting.championcompanion.core.designsystem.MetricCard
import br.com.mwconsulting.championcompanion.core.model.CalendarDataSource
import br.com.mwconsulting.championcompanion.core.model.CalendarHistoryPeriod
import br.com.mwconsulting.championcompanion.core.model.CalendarStepsHistorySnapshot
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.DailySummaryPolicy
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

private const val STEPS_HISTORY_PAGE_SIZE = 10

@Composable
fun StepsDetailScreen(
    onNavigateBack: () -> Unit,
    backLabel: String = "Voltar para Saúde",
    healthViewModel: HealthViewModel = activityHealthViewModel(),
    stepsHistoryViewModel: StepsHistoryViewModel = activityStepsHistoryViewModel(),
) {
    val state by healthViewModel.state.collectAsState()
    val historyState by stepsHistoryViewModel.state.collectAsState()
    val summary = state.todaySummary

    LaunchedEffect(stepsHistoryViewModel) {
        stepsHistoryViewModel.onIntent(StepsHistoryIntent.Load)
    }

    MetricDetailScaffold(
        title = "Passos",
        description = "Esta tela reutiliza resumo diário persistido da Fase 07. Não cria nova sincronização nem inicia BLE ao abrir.",
        onNavigateBack = onNavigateBack,
        backLabel = backLabel,
    ) {
        HealthCard(title = "Resumo atual", modifier = Modifier.fillMaxWidth()) {
            if (summary == null) {
                HealthMetricEmptyState(message = "Nenhum resumo diário persistido foi encontrado para hoje.")
            } else {
                val progress = DailySummaryPolicy.progress(summary.steps, state.dailyStepsGoal)
                MetricValueBlock(
                    value = "${stepsNumberFormat().format(summary.steps)} passos",
                    supporting = "Meta ${stepsNumberFormat().format(progress.goal)} • ${(progress.progress * 100f).roundToInt()}%",
                    extra = "Atualizado em ${formatDateTime(summary.updatedAt)} • ${summary.sourceLabel}",
                )
                LinearProgressIndicator(
                    progress = { progress.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.medium),
                )
            }
        }
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
            StepsHistorySummary(snapshot)
            HealthCard(title = "Gráfico", modifier = Modifier.fillMaxWidth()) {
                StepsHistoryChart(
                    snapshot = snapshot,
                    modifier = Modifier.padding(top = ChampionSpacing.medium),
                )
            }
            HealthCard(title = "Lista acessível", modifier = Modifier.fillMaxWidth()) {
                StepsHistoryList(snapshot)
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
            CalendarHistoryPeriod.entries.forEach { period ->
                val label = period.toStepsPeriodLabel()
                val modifier = Modifier
                    .weight(1f)
                    .semantics {
                        this.selected = period == selected
                        contentDescription = "Últimos ${period.windowDays} dias"
                        stateDescription = if (period == selected) "Selecionado" else "Não selecionado"
                    }
                OutlinedButton(onClick = { onSelect(period) }, modifier = modifier) {
                    Text(
                        text = label,
                        maxLines = 1,
                        color = if (period == selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = if (period == selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepsHistorySummary(snapshot: CalendarStepsHistorySnapshot) {
    val aggregates = snapshot.aggregates
    HealthCard(title = "Resumo do período", modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(top = ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            MetricCard(
                label = "Total",
                value = aggregates.totalSteps?.let(::formatSteps) ?: "Sem dados",
                supportingText = "Somente dias registrados",
                modifier = Modifier.fillMaxWidth(),
            )
            MetricCard(
                label = "Média por dia registrado",
                value = aggregates.averageStepsPerRecordedDay?.let(::formatAverageSteps) ?: "Sem dados",
                supportingText = "Lacunas não entram no cálculo",
                modifier = Modifier.fillMaxWidth(),
            )
            MetricCard(
                label = "Máximo",
                value = aggregates.maximumSteps?.let(::formatSteps) ?: "Sem dados",
                modifier = Modifier.fillMaxWidth(),
            )
            MetricCard(
                label = "Dias registrados",
                value = "${aggregates.recordedDayCount}/${snapshot.query.period.windowDays}",
                supportingText = "${aggregates.missingDayCount} dia(s) sem resumo",
                modifier = Modifier.fillMaxWidth(),
            )
            MetricCard(
                label = "Meta atual",
                value = formatSteps(snapshot.stepsGoal.toLong()),
                supportingText = "${aggregates.currentGoalReachedDayCount} dia(s) na meta",
                modifier = Modifier.fillMaxWidth(),
            )
            if (aggregates.recordedDayCount == 0) {
                EmptyState(
                    title = "Sem dados no período",
                    message = "Nenhum resumo diário local foi encontrado. Dias ausentes não são tratados como 0 passos.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StepsHistoryList(snapshot: CalendarStepsHistorySnapshot) {
    val locale = LocalConfiguration.current.locales[0]
    val entries = snapshot.entries.asReversed()
    val totalPages = (entries.size + STEPS_HISTORY_PAGE_SIZE - 1) / STEPS_HISTORY_PAGE_SIZE
    var currentPage by rememberSaveable(snapshot.query.period) { mutableStateOf(0) }
    currentPage = currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    val pageEntries = entries
        .drop(currentPage * STEPS_HISTORY_PAGE_SIZE)
        .take(STEPS_HISTORY_PAGE_SIZE)

    Column(
        modifier = Modifier.padding(top = ChampionSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        Text(
            text = "Meta atual: ${formatSteps(snapshot.stepsGoal.toLong())}",
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = "Meta atual: ${formatSteps(snapshot.stepsGoal.toLong())}"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "Página ${currentPage + 1} de $totalPages. ${entries.size} dias no período."
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = "Página ${currentPage + 1} de $totalPages",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "${stepsNumberFormat().format(entries.size)} dias",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        pageEntries.forEach { entry ->
            val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale)
                .format(entry.localDate)
            val value = entry.steps?.let { formatSteps(it) } ?: "Sem dados"
            val origin = entry.origin?.toStepsOriginLabel() ?: "indisponível"
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
        if (totalPages > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
            ) {
                OutlinedButton(
                    onClick = { currentPage = (currentPage - 1).coerceAtLeast(0) },
                    modifier = Modifier.weight(1f),
                    enabled = currentPage > 0,
                ) {
                    Text("Anterior", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { currentPage = (currentPage + 1).coerceAtMost(totalPages - 1) },
                    modifier = Modifier.weight(1f),
                    enabled = currentPage < totalPages - 1,
                ) {
                    Text("Próxima", maxLines = 1)
                }
            }
        }
    }
}

private fun CalendarHistoryPeriod.toStepsPeriodLabel(): String = when (this) {
    CalendarHistoryPeriod.LAST_7_DAYS -> "7D"
    CalendarHistoryPeriod.LAST_30_DAYS -> "30D"
    CalendarHistoryPeriod.LAST_90_DAYS -> "90D"
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

private fun formatAverageSteps(value: Double): String =
    "${NumberFormat.getIntegerInstance(Locale.getDefault()).format(value.roundToInt())} passos"

@Composable
private fun activityStepsHistoryViewModel(): StepsHistoryViewModel {
    val activity = checkNotNull(LocalActivity.current) as ComponentActivity
    return androidx.compose.runtime.remember(activity) {
        ViewModelProvider(activity)[StepsHistoryViewModel::class.java]
    }
}

private fun stepsNumberFormat(): NumberFormat = NumberFormat.getIntegerInstance(Locale.getDefault())
