package br.com.mwconsulting.championcompanion.feature.evolution

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeLarge
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeMedium
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.HealthCard
import br.com.mwconsulting.championcompanion.core.designsystem.LoadingState
import br.com.mwconsulting.championcompanion.core.designsystem.LightColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.MetricCard
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionCartesianChartData
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartAxisSpec
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartFormatters
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartInteractionPolicy
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartPoint
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartSeries
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartUnit
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartValueFormatter
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartXAxisFormatter
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartXAxisSpec
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionTimeSeriesChart
import br.com.mwconsulting.championcompanion.core.model.EvolutionComparisonDirection
import br.com.mwconsulting.championcompanion.core.model.EvolutionEmptyReason
import br.com.mwconsulting.championcompanion.core.model.EvolutionMetric
import br.com.mwconsulting.championcompanion.core.model.EvolutionMetricComparison
import br.com.mwconsulting.championcompanion.core.model.EvolutionMetricSeries
import br.com.mwconsulting.championcompanion.core.model.EvolutionPeriod
import br.com.mwconsulting.championcompanion.core.model.EvolutionPeriodComparison
import br.com.mwconsulting.championcompanion.core.model.EvolutionReadFailure
import br.com.mwconsulting.championcompanion.core.model.EvolutionReadSnapshot
import br.com.mwconsulting.championcompanion.core.model.EvolutionReadState
import br.com.mwconsulting.championcompanion.core.model.EvolutionSeriesPointState
import br.com.mwconsulting.championcompanion.core.model.EvolutionSnapshot
import br.com.mwconsulting.championcompanion.core.model.EvolutionWorkoutFilter
import br.com.mwconsulting.championcompanion.core.model.PersonalRecord
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordMetric
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordsReadState
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordsSnapshot
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val EVOLUTION_WORKOUT_TYPES = listOf(
    WorkoutType.RUNNING,
    WorkoutType.WALKING,
    WorkoutType.TREADMILL,
    WorkoutType.CYCLING,
    WorkoutType.FREE_WORKOUT,
)

@Composable
fun EvolutionScreen(
    onNavigateBack: () -> Unit,
    onOpenActivity: (br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId) -> Unit = {},
    viewModel: EvolutionViewModel = run {
        val activity = checkNotNull(LocalActivity.current) as ComponentActivity
        remember(activity) {
            ViewModelProvider(activity)[EvolutionViewModel::class.java]
        }
    },
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.onIntent(EvolutionIntent.Load)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ChampionSpacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
        ) {
            EvolutionHeader(onNavigateBack = onNavigateBack)
            EvolutionPeriodSelector(
                selected = state.selectedPeriod,
                onSelect = { viewModel.onIntent(EvolutionIntent.SelectPeriod(it)) },
            )
            EvolutionWorkoutFilterSelector(
                selected = state.selectedFilter,
                onSelect = { viewModel.onIntent(EvolutionIntent.SelectFilter(it)) },
            )

            when (val readState = state.readState) {
                is EvolutionReadState.Loading -> LoadingState(
                    label = "Carregando evolução",
                    message = "Consultando somente atividades locais elegíveis",
                    modifier = Modifier.fillMaxWidth(),
                )

                is EvolutionReadState.Empty -> EvolutionEmptyState(readState.reason)

                is EvolutionReadState.Failed -> ErrorState(
                    title = "Evolução indisponível",
                    message = readState.reason.toEvolutionUserMessage(),
                    actionLabel = if (readState.retryable) "Tentar novamente" else null,
                    onAction = if (readState.retryable) {
                        { viewModel.onIntent(EvolutionIntent.Retry) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                is EvolutionReadState.Ready -> {
                    val snapshot = state.snapshot
                    if (snapshot == null) {
                        ErrorState(
                            title = "Evolução indisponível",
                            message = "Não foi possível formar um resumo seguro dos dados locais.",
                            actionLabel = "Tentar novamente",
                            onAction = { viewModel.onIntent(EvolutionIntent.Retry) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        EvolutionReadyContent(
                            readSnapshot = readState.snapshot,
                            snapshot = snapshot,
                            previousReadState = state.previousReadState,
                            onRetry = { viewModel.onIntent(EvolutionIntent.Retry) },
                        )
                    }
                }
            }

            EvolutionPersonalRecordsSection(
                state = state.personalRecordsState,
                records = state.personalRecords,
                onOpenActivity = onOpenActivity,
                onRetry = { viewModel.onIntent(EvolutionIntent.Retry) },
            )
        }
    }
}

@Composable
private fun EvolutionHeader(onNavigateBack: () -> Unit) {
    val headerColor = LightColorScheme.primary
    val headerContentColor = Color.White

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = headerColor,
            contentColor = headerContentColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ChampionSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar para exercícios",
                    tint = headerContentColor,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
            ) {
                Text(
                    text = "Evolução",
                    modifier = Modifier.semantics { heading() },
                    color = headerContentColor,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Volume, tendências e recordes do histórico local.",
                    color = headerContentColor,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun EvolutionPeriodSelector(
    selected: EvolutionPeriod,
    onSelect: (EvolutionPeriod) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
    ) {
        Text(
            text = "Período",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            EvolutionPeriod.entries.forEach { period ->
                OutlinedButton(
                    onClick = { onSelect(period) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            this.selected = period == selected
                            contentDescription = period.toEvolutionPeriodLabel()
                            stateDescription = if (period == selected) "Selecionado" else "Não selecionado"
                        },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (period == selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    ),
                ) {
                    Text(
                        text = period.toEvolutionPeriodLabel(),
                        maxLines = 1,
                        fontWeight = if (period == selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun EvolutionWorkoutFilterSelector(
    selected: EvolutionWorkoutFilter,
    onSelect: (EvolutionWorkoutFilter) -> Unit,
) {
    val options = buildList {
        add(EvolutionWorkoutFilter.Consolidated)
        EVOLUTION_WORKOUT_TYPES.forEach { add(EvolutionWorkoutFilter.ByType(it)) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
    ) {
        Text(
            text = "Modalidade",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            options.forEach { option ->
                OutlinedButton(
                    onClick = { onSelect(option) },
                    modifier = Modifier.semantics {
                        this.selected = option == selected
                        contentDescription = option.toEvolutionFilterLabel()
                        stateDescription = if (option == selected) "Selecionada" else "Não selecionada"
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (option == selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    ),
                ) {
                    Text(
                        text = option.toEvolutionFilterLabel(),
                        maxLines = 1,
                        fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun EvolutionReadyContent(
    readSnapshot: EvolutionReadSnapshot,
    snapshot: EvolutionSnapshot,
    previousReadState: EvolutionReadState,
    onRetry: () -> Unit,
) {
    EvolutionSummarySection(
        readSnapshot = readSnapshot,
        snapshot = snapshot,
    )
    EvolutionTrendsSection(
        query = snapshot.query,
        series = snapshot.trends.series,
    )
    EvolutionComparisonSection(
        comparison = snapshot.comparison,
        previousReadState = previousReadState,
        onRetry = onRetry,
    )
}

@Composable
private fun EvolutionSummarySection(
    readSnapshot: EvolutionReadSnapshot,
    snapshot: EvolutionSnapshot,
) {
    val volume = snapshot.volume
    val onlyCycling = readSnapshot.activities.isNotEmpty() &&
        readSnapshot.activities.all { it.workoutType == WorkoutType.CYCLING }
    HealthCard(title = "Resumo do período", modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(top = ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            MetricCard(
                label = "Atividades",
                value = numberFormat().format(volume.activityCount),
                supportingText = "Atividades elegíveis no período",
                modifier = Modifier.fillMaxWidth(),
            )
            MetricCard(
                label = "Distância total",
                value = volume.totalDistanceMeters?.let(::formatDistanceMeters) ?: "Sem dados",
                supportingText = "Somente distâncias finais persistidas",
                modifier = Modifier.fillMaxWidth(),
            )
            MetricCard(
                label = "Duração total",
                value = volume.totalDuration?.let(::formatDuration) ?: "Sem dados",
                supportingText = "Somente atividades com duração válida",
                modifier = Modifier.fillMaxWidth(),
            )
            MetricCard(
                label = "Passos em treinos",
                value = volume.totalSteps?.let { "${numberFormat().format(it)} passos" }
                    ?: if (onlyCycling) "Não aplicável" else "Sem dados",
                supportingText = if (onlyCycling) {
                    "Bicicleta não usa passos como métrica de performance"
                } else {
                    "Ausência de passos não representa zero"
                },
                modifier = Modifier.fillMaxWidth(),
            )
            MetricCard(
                label = "Dias com atividade",
                value = "${volume.activeDayCount} de ${snapshot.query.period.windowDays}",
                supportingText = "Indicador factual de consistência no período",
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Dados ausentes permanecem indisponíveis; não são convertidos em zero.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun EvolutionTrendsSection(
    query: br.com.mwconsulting.championcompanion.core.model.EvolutionQuery,
    series: List<EvolutionMetricSeries>,
) {
    Text(
        text = "Tendências",
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
    if (series.isEmpty()) {
        EmptyState(
            title = "Sem tendências elegíveis",
            message = "Não há métricas comparáveis disponíveis para este filtro.",
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        series.forEach { metricSeries ->
            EvolutionTrendCard(
                query = query,
                series = metricSeries,
            )
        }
    }
}

@Composable
private fun EvolutionTrendCard(
    query: br.com.mwconsulting.championcompanion.core.model.EvolutionQuery,
    series: EvolutionMetricSeries,
) {
    val presentation = series.metric.toTrendPresentation() ?: return
    val locale = LocalConfiguration.current.locales[0]
    val shortDateFormatter = DateTimeFormatter.ofPattern("dd/MM", locale)
    val points = series.points.mapIndexed { index, point ->
        val dateLabel = shortDateFormatter.format(point.startedAt.atZone(query.zoneId))
        val chartValue = point.value?.let(presentation.toChartValue)?.takeIf { it.isFinite() }
        ChampionChartPoint(
            x = index.toDouble(),
            y = chartValue,
            xLabel = dateLabel,
            contentDescription = point.toChartDescription(
                dateLabel = dateLabel,
                chartValue = chartValue,
                presentation = presentation,
            ),
        )
    }
    val maximumX = points.lastIndex.toDouble().coerceAtLeast(1.0)
    // Keep extreme points and their date labels inside plot bounds, including
    // enlarged-font layouts where the chart renderer cannot infer label width.
    val axisPadding = (points.size * 0.08).coerceIn(1.75, 4.0)
    val chartData = ChampionCartesianChartData(
        series = listOf(
            ChampionChartSeries(
                key = "evolution-${series.metric.name.lowercase(Locale.ROOT)}",
                label = presentation.label,
                points = points,
            ),
        ),
        xAxis = ChampionChartXAxisSpec(
            formatter = ChampionChartXAxisFormatter { value ->
                val index = value.roundToInt()
                points.getOrNull(index)?.xLabel ?: "—"
            },
            title = "Data",
            minimum = -axisPadding,
            maximum = maximumX + axisPadding,
        ),
        yAxis = ChampionChartAxisSpec(
            title = presentation.label,
            formatter = presentation.formatter,
            unit = presentation.unit,
        ),
        interaction = ChampionChartInteractionPolicy(
            scrollEnabled = points.size > 6,
        ),
        accessibilitySummary = "${presentation.label} por dia. " +
            "${series.recordedPointCount} ponto(s) com dados, ${series.gapCount} lacuna(s) " +
            "e ${series.absenceCount} ponto(s) sem valor. Lacunas não representam zero.",
    )
    val absentCount = series.points.count { it.state == EvolutionSeriesPointState.ABSENT }
    val notApplicableCount = series.points.count { it.state == EvolutionSeriesPointState.NOT_APPLICABLE }

    HealthCard(title = presentation.label, modifier = Modifier.fillMaxWidth()) {
        ChampionTimeSeriesChart(
            data = chartData,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .padding(top = ChampionSpacing.medium),
        )
        if (series.gapCount > 0) {
            Text(
                text = "${series.gapCount} lacuna(s) sem atividade; não representam zero.",
                modifier = Modifier.padding(top = ChampionSpacing.small),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (absentCount > 0) {
            Text(
                text = "$absentCount ponto(s) sem valor confirmado.",
                modifier = Modifier.padding(top = ChampionSpacing.extraSmall),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (notApplicableCount > 0) {
            Text(
                text = "$notApplicableCount ponto(s) não aplicável(eis) a esta métrica.",
                modifier = Modifier.padding(top = ChampionSpacing.extraSmall),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun EvolutionComparisonSection(
    comparison: EvolutionPeriodComparison?,
    previousReadState: EvolutionReadState,
    onRetry: () -> Unit,
) {
    HealthCard(title = "Comparação com período anterior", modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(top = ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            when {
                comparison != null -> {
                    Text(
                        text = "Período atual comparado com janela imediatamente anterior de mesmo tamanho.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    comparison.metrics.forEach { metric ->
                        EvolutionMetricComparisonCard(metric)
                    }
                }

                previousReadState is EvolutionReadState.Loading -> LoadingState(
                    label = "Carregando comparação",
                    message = "Consultando período anterior",
                    modifier = Modifier.fillMaxWidth(),
                )

                previousReadState is EvolutionReadState.Empty -> EmptyState(
                    title = "Sem base para comparar",
                    message = "Não há atividades elegíveis no período anterior. Nenhum zero foi inventado.",
                    modifier = Modifier.fillMaxWidth(),
                )

                previousReadState is EvolutionReadState.Failed -> ErrorState(
                    title = "Comparação indisponível",
                    message = "Não foi possível consultar o período anterior sem perder segurança dos dados.",
                    actionLabel = if (previousReadState.retryable) "Tentar novamente" else null,
                    onAction = if (previousReadState.retryable) onRetry else null,
                    modifier = Modifier.fillMaxWidth(),
                )

                else -> EmptyState(
                    title = "Comparação indisponível",
                    message = "Não há dados suficientes para formar uma comparação segura.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun EvolutionMetricComparisonCard(comparison: EvolutionMetricComparison) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeMedium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
        ) {
            Text(
                text = comparison.metric.toEvolutionMetricLabel(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            val current = comparison.currentValue
            val previous = comparison.previousValue
            if (current == null || previous == null) {
                Text(
                    text = "Sem dados suficientes nos dois períodos para comparar esta métrica.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = "Período atual: ${comparison.metric.formatEvolutionValue(current)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Período anterior: ${comparison.metric.formatEvolutionValue(previous)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                comparison.absoluteDifference?.let { difference ->
                    Text(
                        text = comparison.toDifferenceLabel(difference),
                        color = comparison.direction.toComparisonColor(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                comparison.percentageDifference?.let { percentage ->
                    Text(
                        text = "Variação percentual: ${signedPercentage(percentage)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun EvolutionPersonalRecordsSection(
    state: PersonalRecordsReadState,
    records: PersonalRecordsSnapshot?,
    onOpenActivity: (br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId) -> Unit,
    onRetry: () -> Unit,
) {
    HealthCard(title = "Recordes pessoais atuais", modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(top = ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text(
                text = "Recordes factuais, separados por modalidade. A data é a do início real do treino.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            when (state) {
                PersonalRecordsReadState.Loading -> LoadingState(
                    label = "Carregando recordes",
                    message = "Consultando histórico local completo",
                    modifier = Modifier.fillMaxWidth(),
                )

                PersonalRecordsReadState.Empty -> EmptyState(
                    title = "Nenhum recorde disponível",
                    message = "Ainda não há atividade elegível com métrica final válida.",
                    modifier = Modifier.fillMaxWidth(),
                )

                is PersonalRecordsReadState.Failed -> ErrorState(
                    title = "Recordes indisponíveis",
                    message = state.reason.toRecordsUserMessage(),
                    actionLabel = if (state.retryable) "Tentar novamente" else null,
                    onAction = if (state.retryable) onRetry else null,
                    modifier = Modifier.fillMaxWidth(),
                )

                is PersonalRecordsReadState.Ready -> {
                    when {
                        records == null -> ErrorState(
                            title = "Recordes indisponíveis",
                            message = "Não foi possível formar recordistas sem inventar valores.",
                            actionLabel = "Tentar novamente",
                            onAction = onRetry,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        records.records.isEmpty() -> EmptyState(
                            title = "Nenhum recorde disponível",
                            message = "As atividades lidas não possuem métrica de recorde válida.",
                            modifier = Modifier.fillMaxWidth(),
                        )

                        else -> records.records.forEach { record ->
                            PersonalRecordCard(
                                record = record,
                                onOpenActivity = { onOpenActivity(record.activityId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalRecordCard(
    record: PersonalRecord,
    onOpenActivity: () -> Unit,
) {
    val dateLabel = formatMoment(record.achievedAt)
    Card(
        onClick = onOpenActivity,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${record.metric.toRecordMetricLabel()}, ${record.workoutType.toWorkoutTypeLabel()}, " +
                    "${record.toRecordValueLabel()}, treino iniciado em $dateLabel. Abrir atividade."
            },
        shape = ChampionShapeMedium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
        ) {
            Text(
                text = record.metric.toRecordMetricLabel(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = record.toRecordValueLabel(),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = record.workoutType.toWorkoutTypeLabel(),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Treino iniciado em $dateLabel",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Abrir atividade",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private data class EvolutionTrendPresentation(
    val label: String,
    val unit: ChampionChartUnit?,
    val toChartValue: (Double) -> Double,
    val formatter: ChampionChartValueFormatter,
)

private fun EvolutionMetric.toTrendPresentation(): EvolutionTrendPresentation? = when (this) {
    EvolutionMetric.DISTANCE -> EvolutionTrendPresentation(
        label = "Distância",
        unit = ChampionChartUnit("km"),
        toChartValue = { it / 1_000.0 },
        formatter = ChampionChartFormatters.decimal(maximumFractionDigits = 1),
    )

    EvolutionMetric.DURATION -> EvolutionTrendPresentation(
        label = "Duração",
        unit = ChampionChartUnit("min"),
        toChartValue = { it / 60.0 },
        formatter = ChampionChartFormatters.decimal(maximumFractionDigits = 1),
    )

    EvolutionMetric.AVERAGE_BPM -> EvolutionTrendPresentation(
        label = "Frequência cardíaca média",
        unit = ChampionChartUnit("BPM"),
        toChartValue = { it },
        formatter = ChampionChartFormatters.integer(),
    )

    EvolutionMetric.CADENCE -> EvolutionTrendPresentation(
        label = "Cadência",
        unit = ChampionChartUnit("passos/min"),
        toChartValue = { it },
        formatter = ChampionChartFormatters.decimal(maximumFractionDigits = 1),
    )

    EvolutionMetric.PACE -> EvolutionTrendPresentation(
        label = "Ritmo",
        unit = ChampionChartUnit("min/km"),
        toChartValue = { it / 60.0 },
        formatter = ChampionChartFormatters.decimal(maximumFractionDigits = 1),
    )

    EvolutionMetric.SPEED -> EvolutionTrendPresentation(
        label = "Velocidade",
        unit = ChampionChartUnit("km/h"),
        toChartValue = { it * 3.6 },
        formatter = ChampionChartFormatters.decimal(maximumFractionDigits = 1),
    )

    EvolutionMetric.ACTIVITY_COUNT,
    EvolutionMetric.STEPS,
    EvolutionMetric.ACTIVE_DAYS,
    -> null
}

private fun br.com.mwconsulting.championcompanion.core.model.EvolutionSeriesPoint.toChartDescription(
    dateLabel: String,
    chartValue: Double?,
    presentation: EvolutionTrendPresentation,
): String = when (state) {
    EvolutionSeriesPointState.PRESENT -> if (chartValue != null) {
        "$dateLabel: ${presentation.formatter.format(chartValue)} ${presentation.unit?.symbol.orEmpty()}."
    } else {
        "$dateLabel: valor fora da escala de exibição."
    }

    EvolutionSeriesPointState.ABSENT -> "$dateLabel: sem dado confirmado."
    EvolutionSeriesPointState.NOT_APPLICABLE -> "$dateLabel: métrica não aplicável."
    EvolutionSeriesPointState.GAP -> "$dateLabel: lacuna sem atividade; não representa zero."
}

private fun EvolutionMetric.toEvolutionMetricLabel(): String = when (this) {
    EvolutionMetric.ACTIVITY_COUNT -> "Atividades"
    EvolutionMetric.DISTANCE -> "Distância"
    EvolutionMetric.DURATION -> "Duração"
    EvolutionMetric.STEPS -> "Passos"
    EvolutionMetric.ACTIVE_DAYS -> "Dias com atividade"
    EvolutionMetric.AVERAGE_BPM -> "Frequência cardíaca média"
    EvolutionMetric.CADENCE -> "Cadência"
    EvolutionMetric.PACE -> "Ritmo"
    EvolutionMetric.SPEED -> "Velocidade"
}

private fun EvolutionMetric.formatEvolutionValue(value: Double): String = when (this) {
    EvolutionMetric.ACTIVITY_COUNT -> numberFormat().format(value.toLong())
    EvolutionMetric.DISTANCE -> "${decimalFormat().format(value / 1_000.0)} km"
    EvolutionMetric.DURATION -> formatDurationValue(value)
    EvolutionMetric.STEPS -> "${numberFormat().format(value.toLong())} passos"
    EvolutionMetric.ACTIVE_DAYS -> "${numberFormat().format(value.toLong())} dias"
    EvolutionMetric.AVERAGE_BPM -> "${decimalFormat(1).format(value)} BPM"
    EvolutionMetric.CADENCE -> "${decimalFormat(1).format(value)} passos/min"
    EvolutionMetric.PACE -> "${decimalFormat(1).format(value / 60.0)} min/km"
    EvolutionMetric.SPEED -> "${decimalFormat(1).format(value * 3.6)} km/h"
}

private fun EvolutionMetricComparison.toDifferenceLabel(difference: Double): String {
    val directionLabel = when (direction) {
        EvolutionComparisonDirection.INCREASED -> "Aumentou"
        EvolutionComparisonDirection.DECREASED -> "Diminuiu"
        EvolutionComparisonDirection.STABLE -> "Estável"
        EvolutionComparisonDirection.UNAVAILABLE -> "Variação indisponível"
    }
    return if (direction == EvolutionComparisonDirection.STABLE) {
        "$directionLabel: ${metric.formatEvolutionValue(difference)}"
    } else {
        "$directionLabel: ${metric.formatEvolutionValue(difference)}"
    }
}

@Composable
private fun EvolutionComparisonDirection.toComparisonColor(): Color = when (this) {
    EvolutionComparisonDirection.INCREASED -> MaterialTheme.colorScheme.primary
    EvolutionComparisonDirection.DECREASED -> MaterialTheme.colorScheme.error
    EvolutionComparisonDirection.STABLE,
    EvolutionComparisonDirection.UNAVAILABLE,
    -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun EvolutionPeriod.toEvolutionPeriodLabel(): String = "$windowDays dias"

private fun EvolutionWorkoutFilter.toEvolutionFilterLabel(): String = when (this) {
    EvolutionWorkoutFilter.Consolidated -> "Consolidado"
    is EvolutionWorkoutFilter.ByType -> workoutType.toWorkoutTypeLabel()
}

private fun WorkoutType.toWorkoutTypeLabel(): String = when (this) {
    WorkoutType.WALKING -> "Caminhada"
    WorkoutType.RUNNING -> "Corrida"
    WorkoutType.FREE_WORKOUT -> "Treino livre"
    WorkoutType.TREADMILL -> "Esteira"
    WorkoutType.CYCLING -> "Bicicleta"
    WorkoutType.UNKNOWN -> "Tipo desconhecido"
}

private fun PersonalRecordMetric.toRecordMetricLabel(): String = when (this) {
    PersonalRecordMetric.LONGEST_DISTANCE -> "Maior distância"
    PersonalRecordMetric.LONGEST_DURATION -> "Maior duração"
    PersonalRecordMetric.MOST_STEPS -> "Mais passos"
}

private fun PersonalRecord.toRecordValueLabel(): String = when (metric) {
    PersonalRecordMetric.LONGEST_DISTANCE -> "${decimalFormat().format(value / 1_000.0)} km"
    PersonalRecordMetric.LONGEST_DURATION -> formatDurationValue(value)
    PersonalRecordMetric.MOST_STEPS -> "${numberFormat().format(value.toLong())} passos"
}

private fun EvolutionEmptyReason.toEvolutionEmptyMessage(): String = when (this) {
    EvolutionEmptyReason.NO_ELIGIBLE_ACTIVITIES ->
        "Não há atividades do relógio com tipo confirmado e estado elegível neste período."
}

@Composable
private fun EvolutionEmptyState(reason: EvolutionEmptyReason) {
    EmptyState(
        title = "Sem atividades elegíveis",
        message = reason.toEvolutionEmptyMessage(),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun EvolutionReadFailure.toEvolutionUserMessage(): String = when (this) {
    EvolutionReadFailure.SOURCE_UNAVAILABLE ->
        "Não foi possível consultar as atividades locais. Tente novamente."

    EvolutionReadFailure.INVALID_LOCAL_DATA ->
        "Os dados locais de evolução não puderam ser confirmados com segurança."
}

private fun EvolutionReadFailure.toRecordsUserMessage(): String = when (this) {
    EvolutionReadFailure.SOURCE_UNAVAILABLE ->
        "Não foi possível consultar o histórico local de recordes. Tente novamente."

    EvolutionReadFailure.INVALID_LOCAL_DATA ->
        "Os dados locais de recordes não puderam ser confirmados com segurança."
}

private fun formatDistanceMeters(meters: Double): String =
    "${decimalFormat().format(meters / 1_000.0)} km"

private fun formatDuration(duration: Duration): String = formatDurationSeconds(
    duration.seconds.toDouble() + duration.nano.toDouble() / 1_000_000_000.0,
)

private fun formatDurationValue(seconds: Double): String = formatDurationSeconds(seconds)

private fun formatDurationSeconds(seconds: Double): String {
    if (!seconds.isFinite() || seconds < 0.0) return "Indisponível"
    if (seconds > 0.0 && seconds < 1.0) return "< 1 s"
    val totalSeconds = seconds.toLong()
    if (totalSeconds < 60L) return "$totalSeconds s"
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val remainingSeconds = totalSeconds % 60L
    return when {
        hours > 0L && minutes > 0L -> "$hours h ${minutes} min"
        hours > 0L -> "$hours h"
        remainingSeconds > 0L -> "$minutes min ${remainingSeconds} s"
        else -> "$minutes min"
    }
}

private fun signedPercentage(value: Double): String {
    val sign = if (value > 0.0) "+" else if (value < 0.0) "−" else ""
    return "$sign${decimalFormat(1).format(abs(value))}%"
}

private fun formatMoment(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.forLanguageTag("pt-BR"))
        .withZone(ZoneId.systemDefault())
        .format(instant)

private fun numberFormat(): NumberFormat = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"))

private fun decimalFormat(maximumFractionDigits: Int = 1): NumberFormat =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
        this.maximumFractionDigits = maximumFractionDigits
        minimumFractionDigits = 0
    }
