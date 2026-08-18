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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.DarkColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.HealthCard
import br.com.mwconsulting.championcompanion.core.designsystem.LoadingState
import br.com.mwconsulting.championcompanion.core.designsystem.LightColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.MetricCard
import br.com.mwconsulting.championcompanion.core.model.CalendarDataSource
import br.com.mwconsulting.championcompanion.core.model.CalendarDistanceHistorySnapshot
import br.com.mwconsulting.championcompanion.core.model.CalendarHistoryPeriod
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private const val DISTANCE_HISTORY_PAGE_SIZE = 10

@Composable
fun DistanceDetailScreen(
    onNavigateBack: () -> Unit,
    backLabel: String = "Voltar para Saúde",
    healthViewModel: HealthViewModel = activityHealthViewModel(),
    distanceHistoryViewModel: DistanceHistoryViewModel = activityDistanceHistoryViewModel(),
) {
    val state by healthViewModel.state.collectAsState()
    val historyState by distanceHistoryViewModel.state.collectAsState()
    val summary = state.todaySummary

    LaunchedEffect(distanceHistoryViewModel) {
        distanceHistoryViewModel.onIntent(DistanceHistoryIntent.Load)
    }

    MetricDetailScaffold(
        title = "Distância",
        description = "Acompanhe estimativa diária e tendência dos resumos locais.",
        onNavigateBack = onNavigateBack,
        backLabel = backLabel,
        header = { DistanceDetailHeader() },
    ) {
        HealthCard(title = "Resumo atual", modifier = Modifier.fillMaxWidth()) {
            if (summary == null || summary.estimatedDistanceMeters == null) {
                HealthMetricEmptyState(
                    message = if (summary == null) {
                        "Nenhum resumo diário local foi encontrado para hoje."
                    } else {
                        "Sem estimativa de distância hoje: ainda faltam dados pessoais suficientes."
                    },
                )
            } else {
                DistanceDailyValue(
                    value = "${distanceDecimalFormat().format(summary.estimatedDistanceMeters / 1000.0)} km",
                    supporting = "Estimativa local de hoje",
                    extra = "Origem: ${summary.sourceLabel} • Atualizado em ${formatDateTime(summary.updatedAt)}",
                )
            }
        }
        DistanceHistoryShell(
            state = historyState,
            onIntent = distanceHistoryViewModel::onIntent,
        )
    }
}

@Composable
private fun DistanceDetailHeader() {
    val headerColor = LightColorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = headerColor,
            contentColor = White,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = "Distância",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Veja a estimativa diária e a tendência dos resumos locais.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun DistanceDailyValue(
    value: String,
    supporting: String,
    extra: String,
) {
    Text(
        text = value,
        modifier = Modifier.padding(top = ChampionSpacing.medium),
        color = distancePrimaryAccent(),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = supporting,
        modifier = Modifier.padding(top = ChampionSpacing.small),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = extra,
        modifier = Modifier.padding(top = ChampionSpacing.small),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
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
            DistanceHistorySummary(snapshot)
            HealthCard(title = "Gráfico", modifier = Modifier.fillMaxWidth()) {
                DistanceHistoryChart(
                    snapshot = snapshot,
                    modifier = Modifier.padding(top = ChampionSpacing.medium),
                )
            }
            HealthCard(title = "Lista acessível", modifier = Modifier.fillMaxWidth()) {
                DistanceHistoryList(snapshot)
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
            CalendarHistoryPeriod.entries.forEach { period ->
                val modifier = Modifier
                    .weight(1f)
                    .semantics {
                        this.selected = period == selected
                        contentDescription = "Últimos ${period.windowDays} dias"
                        stateDescription = if (period == selected) "Selecionado" else "Não selecionado"
                    }
                OutlinedButton(
                    onClick = { onSelect(period) },
                    modifier = modifier,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (period == selected) distancePrimaryAccent() else MaterialTheme.colorScheme.outline,
                    ),
                ) {
                    Text(
                        text = period.toDistancePeriodLabel(),
                        maxLines = 1,
                        color = if (period == selected) {
                            distancePrimaryAccent()
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
private fun DistanceHistorySummary(snapshot: CalendarDistanceHistorySnapshot) {
    val aggregates = snapshot.aggregates
    HealthCard(title = "Resumo do período", modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(top = ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            MetricCard(
                label = "Total",
                value = aggregates.totalDistanceMeters?.let(::formatDistance) ?: "Sem dados",
                supportingText = "Somente dias com estimativa",
                modifier = Modifier.fillMaxWidth(),
            )
            MetricCard(
                label = "Média por dia com estimativa",
                value = aggregates.averageDistanceMetersPerDayWithEstimate?.let(::formatDistance) ?: "Sem dados",
                supportingText = "Dias sem estimativa não entram no cálculo",
                modifier = Modifier.fillMaxWidth(),
            )
            MetricCard(
                label = "Máximo",
                value = aggregates.maximumDistanceMeters?.let(::formatDistance) ?: "Sem dados",
                modifier = Modifier.fillMaxWidth(),
            )
            MetricCard(
                label = "Dias com estimativa",
                value = "${aggregates.daysWithEstimateCount}/${snapshot.query.period.windowDays}",
                supportingText = "${aggregates.daysWithoutEstimateCount} dia(s) sem estimativa",
                modifier = Modifier.fillMaxWidth(),
            )
            if (aggregates.daysWithEstimateCount == 0) {
                EmptyState(
                    title = "Sem estimativas no período",
                    message = "Nenhuma distância diária estimada foi encontrada. Dias sem estimativa não são tratados como 0 km.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DistanceHistoryList(snapshot: CalendarDistanceHistorySnapshot) {
    val locale = LocalConfiguration.current.locales[0]
    val entries = snapshot.entries.asReversed()
    val totalPages = (entries.size + DISTANCE_HISTORY_PAGE_SIZE - 1) / DISTANCE_HISTORY_PAGE_SIZE
    var currentPage by rememberSaveable(snapshot.query.period) { mutableStateOf(0) }
    currentPage = currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    val pageEntries = entries
        .drop(currentPage * DISTANCE_HISTORY_PAGE_SIZE)
        .take(DISTANCE_HISTORY_PAGE_SIZE)

    Column(
        modifier = Modifier.padding(top = ChampionSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        Text(
            text = "Valores em quilômetros. Dias sem estimativa permanecem indisponíveis.",
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = "Valores em quilômetros. Dias sem estimativa permanecem indisponíveis."
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
                text = "${entries.size} dias",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        pageEntries.forEach { entry ->
            val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale)
                .format(entry.localDate)
            val value = entry.estimatedDistanceMeters?.let { "${formatDistance(it)} • estimada" }
                ?: "Sem estimativa"
            val origin = entry.origin?.toDistanceOriginLabel() ?: "indisponível"
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

private fun CalendarHistoryPeriod.toDistancePeriodLabel(): String = when (this) {
    CalendarHistoryPeriod.LAST_7_DAYS -> "7D"
    CalendarHistoryPeriod.LAST_30_DAYS -> "30D"
    CalendarHistoryPeriod.LAST_90_DAYS -> "90D"
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
    maximumFractionDigits = 1
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
