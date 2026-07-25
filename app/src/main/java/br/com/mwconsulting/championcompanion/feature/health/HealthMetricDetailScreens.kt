package br.com.mwconsulting.championcompanion.feature.health

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.HealthCard
import br.com.mwconsulting.championcompanion.core.designsystem.MetricCard
import br.com.mwconsulting.championcompanion.core.designsystem.PrimaryActionButton
import br.com.mwconsulting.championcompanion.core.model.DailySummaryPolicy
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportMetadata
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportStatus
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportSummary
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryEntry
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryPeriod
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration

private const val HEART_RATE_HISTORY_PAGE_SIZE = 10

@Composable
fun HeartRateDetailScreen(
    onNavigateBack: () -> Unit,
    healthViewModel: HealthViewModel = activityHealthViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val state by healthViewModel.state.collectAsState()
    var showExitConfirmation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(healthViewModel) {
        healthViewModel.onIntent(HealthIntent.Load)
    }

    DisposableEffect(lifecycleOwner, healthViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME,
                -> healthViewModel.onIntent(HealthIntent.Load)

                Lifecycle.Event.ON_STOP -> healthViewModel.onIntent(HealthIntent.AppMovedToBackground)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isMeasurementInFlight = state.surfaceState.isMeasurementInFlight()
    val isD1ImportInFlight = state.d1ImportState.isImportInFlight()
    val isBlockingExit = isMeasurementInFlight || isD1ImportInFlight

    BackHandler(enabled = isBlockingExit) {
        showExitConfirmation = true
    }

    DisposableEffect(view, isMeasurementInFlight) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = isMeasurementInFlight
        onDispose { view.keepScreenOn = previousKeepScreenOn }
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text(if (isD1ImportInFlight) "Cancelar importação?" else "Cancelar medição?") },
            text = {
                Text(
                    if (isD1ImportInFlight) {
                        "A importação D1 em andamento será cancelada antes da conclusão."
                    } else {
                        "A medição em andamento será cancelada e nenhum resultado parcial será salvo."
                    },
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmation = false
                        if (isD1ImportInFlight) {
                            healthViewModel.onIntent(HealthIntent.ConfirmExitD1Import)
                        } else {
                            healthViewModel.onIntent(HealthIntent.ConfirmExitMeasurement)
                        }
                        onNavigateBack()
                    },
                ) {
                    Text("Voltar para Saúde")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text(if (isD1ImportInFlight) "Continuar importando" else "Continuar medindo")
                }
            },
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ChampionSpacing.extraLarge)
                .heightIn(min = 1.dp),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
        ) {
            MetricDetailHeader(
                title = "Frequência cardíaca",
                description = "Esta tela concentra medição manual, importação D1 local, gráfico funcional e histórico consolidado sem iniciar BLE automaticamente.",
            )
            HeartRateOverviewCard(
                state = state,
                onStartMeasurement = { healthViewModel.onIntent(HealthIntent.StartMeasurement) },
                onStartD1Import = { healthViewModel.onIntent(HealthIntent.StartD1Import) },
                onCancelD1Import = { healthViewModel.onIntent(HealthIntent.CancelD1Import) },
                onRetryD1Import = { healthViewModel.onIntent(HealthIntent.RetryD1Import) },
                onDismissD1Import = { healthViewModel.onIntent(HealthIntent.DismissD1Import) },
            )
            HeartRateSessionSurface(
                state = state,
                onStart = { healthViewModel.onIntent(HealthIntent.StartMeasurement) },
                onCancel = { healthViewModel.onIntent(HealthIntent.CancelMeasurement) },
                onRetry = { healthViewModel.onIntent(HealthIntent.Retry) },
                onDismiss = { healthViewModel.onIntent(HealthIntent.DismissError) },
            )
            HeartRateHistoryChartSection(
                state = state,
                onSelectPeriod = { healthViewModel.onIntent(HealthIntent.SelectHistoryPeriod(it)) },
            )
            HealthMetricHistorySection(
                title = "Histórico completo",
                emptyMessage = "Ainda não há medições consolidadas para o período selecionado.",
            ) {
                HeartRateHistoryList(
                    entries = state.history.entries,
                    associatedDeviceName = state.associatedDeviceName,
                )
            }
            TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar para Saúde")
            }
        }
    }
}

@Composable
fun WeightDetailScreen(
    onNavigateBack: () -> Unit,
    healthViewModel: HealthViewModel = activityHealthViewModel(),
) {
    val state by healthViewModel.state.collectAsState()
    MetricDetailScaffold(
        title = "Peso",
        description = "Esta tela usa apenas último peso persistido já disponível. Cadastro, edição e histórico completo ficam para fase futura.",
        onNavigateBack = onNavigateBack,
    ) {
        val weight = state.latestWeight
        HealthCard(title = "Resumo atual", modifier = Modifier.fillMaxWidth()) {
            if (weight == null) {
                HealthMetricEmptyState(message = "Nenhum peso persistido foi encontrado até agora.")
            } else {
                MetricValueBlock(
                    value = "${decimalFormat().format(weight.kilograms)} kg",
                    supporting = "Última atualização: ${formatDateTime(weight.measuredAt)}",
                    extra = weight.origin?.toHealthOriginLabel(),
                )
            }
        }
        HealthMetricChartSection(
            title = "Gráfico",
            message = "Gráfico de peso fica para fase futura de histórico e perfil.",
        )
        HealthMetricHistorySection(
            title = "Histórico",
            emptyMessage = "Histórico completo de peso ainda não faz parte desta fase.",
        )
    }
}

@Composable
fun StepsDetailScreen(
    onNavigateBack: () -> Unit,
    healthViewModel: HealthViewModel = activityHealthViewModel(),
) {
    val state by healthViewModel.state.collectAsState()
    val summary = state.todaySummary
    MetricDetailScaffold(
        title = "Passos",
        description = "Esta tela reutiliza resumo diário persistido da Fase 07. Não cria nova sincronização nem inicia BLE ao abrir.",
        onNavigateBack = onNavigateBack,
    ) {
        HealthCard(title = "Resumo atual", modifier = Modifier.fillMaxWidth()) {
            if (summary == null) {
                HealthMetricEmptyState(message = "Nenhum resumo diário persistido foi encontrado para hoje.")
            } else {
                val progress = DailySummaryPolicy.progress(summary.steps, state.dailyStepsGoal)
                MetricValueBlock(
                    value = "${numberFormat().format(summary.steps)} passos",
                    supporting = "Meta ${numberFormat().format(progress.goal)} • ${(progress.progress * 100f).roundToInt()}%",
                    extra = "Atualizado em ${formatDateTime(summary.updatedAt)} • ${summary.sourceLabel}",
                )
                LinearProgressIndicator(
                    progress = { progress.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.medium),
                )
            }
        }
        HealthMetricChartSection(
            title = "Gráfico",
            message = "Gráficos de passos ficam para a fase de calendário e histórico consolidado.",
        )
        HealthMetricHistorySection(
            title = "Histórico",
            emptyMessage = "Histórico completo de passos ainda não faz parte desta fase.",
        )
    }
}

@Composable
fun DistanceDetailScreen(
    onNavigateBack: () -> Unit,
    healthViewModel: HealthViewModel = activityHealthViewModel(),
) {
    val state by healthViewModel.state.collectAsState()
    val summary = state.todaySummary
    MetricDetailScaffold(
        title = "Distância",
        description = "Esta tela reutiliza distância estimada já persistida no resumo diário. Quando faltarem dados pessoais, o valor permanece indisponível.",
        onNavigateBack = onNavigateBack,
    ) {
        HealthCard(title = "Resumo atual", modifier = Modifier.fillMaxWidth()) {
            if (summary == null || summary.estimatedDistanceMeters == null) {
                HealthMetricEmptyState(
                    message = if (summary == null) {
                        "Nenhum resumo diário persistido foi encontrado para hoje."
                    } else {
                        "A distância de hoje está indisponível porque ainda faltam dados pessoais suficientes para a estimativa."
                    },
                )
            } else {
                MetricValueBlock(
                    value = "${decimalFormat().format(summary.estimatedDistanceMeters / 1000.0)} km",
                    supporting = "Estimativa de hoje",
                    extra = "Atualizado em ${formatDateTime(summary.updatedAt)} • ${summary.sourceLabel}",
                )
            }
        }
        HealthMetricChartSection(
            title = "Gráfico",
            message = "Gráficos de distância ficam para a fase de calendário e histórico consolidado.",
        )
        HealthMetricHistorySection(
            title = "Histórico",
            emptyMessage = "Histórico completo de distância ainda não faz parte desta fase.",
        )
    }
}

@Composable
private fun MetricDetailScaffold(
    title: String,
    description: String,
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ChampionSpacing.extraLarge)
                .heightIn(min = 1.dp),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
        ) {
            MetricDetailHeader(title = title, description = description)
            content()
            TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar para Saúde")
            }
        }
    }
}

@Composable
private fun MetricDetailHeader(
    title: String,
    description: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun HeartRateOverviewCard(
    state: HealthUiState,
    onStartMeasurement: () -> Unit,
    onStartD1Import: () -> Unit,
    onCancelD1Import: () -> Unit,
    onRetryD1Import: () -> Unit,
    onDismissD1Import: () -> Unit,
) {
    val isMeasurementInFlight = state.surfaceState.isMeasurementInFlight()
    val isD1ImportInFlight = state.d1ImportState.isImportInFlight()
    val latestMeasurement = state.latestMeasurement

    HealthCard(title = "Resumo atual e ações", modifier = Modifier.fillMaxWidth()) {
        if (latestMeasurement == null) {
            Text(
                text = if (state.hasAssociatedDevice) {
                    "Ainda não há frequência cardíaca persistida. Use a medição manual ou toque em Buscar dados do relógio para importar o histórico local."
                } else {
                    "Associe um relógio para medir ou importar o histórico local de frequência cardíaca."
                },
                modifier = Modifier.padding(top = ChampionSpacing.medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(
                text = "${latestMeasurement.bpm} BPM",
                modifier = Modifier.padding(top = ChampionSpacing.medium),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = latestMeasurement.measuredAt?.let(::formatMeasurementMoment) ?: "Horário indisponível",
                modifier = Modifier.padding(top = ChampionSpacing.small),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Origem: ${latestMeasurement.source.toFriendlySourceLabel(state.associatedDeviceName)}",
                modifier = Modifier.padding(top = ChampionSpacing.small),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        state.associatedDeviceName?.let { name ->
            Text(
                text = "Relógio associado: $name",
                modifier = Modifier.padding(top = ChampionSpacing.medium),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            PrimaryActionButton(
                label = "Medir frequência cardíaca",
                onClick = onStartMeasurement,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.hasAssociatedDevice && !isD1ImportInFlight,
            )
            OutlinedButton(
                onClick = when {
                    isD1ImportInFlight -> onCancelD1Import
                    state.d1ImportState.isRetryableFailure() -> onRetryD1Import
                    else -> onStartD1Import
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.hasAssociatedDevice && !isMeasurementInFlight,
            ) {
                Text(
                    when {
                        isD1ImportInFlight -> "Cancelar importação"
                        state.d1ImportState.isRetryableFailure() -> "Tentar novamente"
                        else -> "Buscar dados do relógio"
                    },
                )
            }
        }

        HeartRateD1ImportPanel(
            state = state.d1ImportState,
            latestMetadata = state.latestD1ImportMetadata,
            associatedDeviceName = state.associatedDeviceName,
            onRetry = onRetryD1Import,
            onDismiss = onDismissD1Import,
        )

        if (state.healthConnectSync.hasVisibleState) {
            Text(
                text = when {
                    state.healthConnectSync.syncingCount > 0 -> "Health Connect sincronizando em segundo plano. Medição manual e importação D1 locais já foram preservadas."
                    state.healthConnectSync.failedCount > 0 -> "Health Connect com falhas pendentes. O sucesso local de medição ou D1 continua preservado."
                    state.healthConnectSync.pendingCount > 0 -> "Há dados de Saúde aguardando envio ao Health Connect."
                    else -> "Sincronização secundária do Health Connect sem pendências visíveis agora."
                },
                modifier = Modifier.padding(top = ChampionSpacing.medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun HeartRateD1ImportPanel(
    state: D1HeartRateImportSurfaceState,
    latestMetadata: D1HeartRateImportMetadata?,
    associatedDeviceName: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val session = state.sessionOrNull()
    val progress = session?.progress?.progressFraction
    val importMessage = state.toUiMessage()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ChampionSpacing.large)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
    ) {
        Text(
            text = importMessage,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        if (state.isImportInFlight()) {
            if (progress == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Aguardando primeiros pacotes válidos do D1.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = "Progresso da importação D1"
                        stateDescription = "${(progress * 100f).roundToInt()} por cento concluído"
                        progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(progress, 0f..1f)
                    },
                )
                Text(
                    text = "${session.progress.receivedPacketCount} pacotes recebidos • ${(progress * 100f).roundToInt()}% concluído",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        session?.result?.summary?.let { summary ->
            HeartRateD1Summary(summary = summary)
        }

        latestMetadata?.let {
            Text(
                text = buildLatestImportLabel(it, associatedDeviceName),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (state is D1HeartRateImportSurfaceState.Failed && state.session.error?.isRetryable == true) {
            TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text("Tentar novamente")
            }
        }
        if (state.isTerminalSurface()) {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Fechar aviso")
            }
        }
    }
}

@Composable
private fun HeartRateD1Summary(summary: D1HeartRateImportSummary) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        MetricCard(
            label = "Registros válidos",
            value = numberFormat().format(summary.validPointCount),
            supportingText = "${summary.insertedCount} novos • ${summary.duplicateCount} duplicados",
            modifier = Modifier.fillMaxWidth(),
        )
        MetricCard(
            label = "Pacotes e lacunas",
            value = numberFormat().format(summary.receivedPacketCount),
            supportingText = "${summary.invalidCount} inválidos • ${summary.missingIndexCount} lacunas",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HeartRateHistoryChartSection(
    state: HealthUiState,
    onSelectPeriod: (HeartRateHistoryPeriod) -> Unit,
) {
    HealthCard(title = "Gráfico e resumo", modifier = Modifier.fillMaxWidth()) {
        HistoryPeriodSelector(
            selected = state.selectedHistoryPeriod,
            onSelectPeriod = onSelectPeriod,
        )

        val aggregates = state.history.aggregates
        HeartRateChartHeadline(
            period = state.selectedHistoryPeriod,
            aggregates = aggregates,
            entryCount = state.history.entries.size,
            pointsCount = state.history.chart.points.size,
        )

        HeartRateHistoryChart(
            series = state.history.chart,
            period = state.selectedHistoryPeriod,
            modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.large),
        )

        HeartRateChartAccessibleSummary(
            measurementCount = state.history.entries.size,
            pointCount = state.history.chart.points.size,
            period = state.selectedHistoryPeriod,
        )
    }
}

@Composable
private fun HeartRateChartHeadline(
    period: HeartRateHistoryPeriod,
    aggregates: br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryAggregates,
    entryCount: Int,
    pointsCount: Int,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.large),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
    ) {
        Text(
            text = periodHeadline(period),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = heartRateRangeHeadline(aggregates),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = heartRateRangeSupportingText(aggregates, entryCount, pointsCount),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "Média ${aggregates.averageBpm?.roundToInt()?.toString() ?: "--"} bpm • ${numberFormat().format(entryCount)} medições válidas",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun HistoryPeriodSelector(
    selected: HeartRateHistoryPeriod,
    onSelectPeriod: (HeartRateHistoryPeriod) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.medium),
        horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
    ) {
        HeartRateHistoryPeriod.entries.forEach { period ->
            val isSelected = period == selected
            OutlinedButton(
                onClick = { onSelectPeriod(period) },
                modifier = Modifier.weight(1f),
            )
            {
                Text(
                    text = compactPeriodToLabel(period),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun HeartRateChartAccessibleSummary(
    measurementCount: Int,
    pointCount: Int,
    period: HeartRateHistoryPeriod,
) {
    val locale = LocalConfiguration.current.locales[0]

    Text(
        text = if (pointCount == 0) {
            "Resumo acessível: nenhum ponto disponível para ${periodToLabel(period).lowercase(locale)}."
        } else {
            "Resumo acessível: ${numberFormat().format(measurementCount)} medições válidas distribuídas em ${numberFormat().format(pointCount)} ponto(s) no gráfico de ${periodToLabel(period).lowercase(locale)}."
        },
        modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.large),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun HeartRateHistoryList(
    entries: List<HeartRateHistoryEntry>,
    associatedDeviceName: String?,
) {
    if (entries.isEmpty()) {
        HealthMetricEmptyState(message = "Ainda não há medições consolidadas para exibir neste período.")
        return
    }

    val totalPages = ceil(entries.size / HEART_RATE_HISTORY_PAGE_SIZE.toDouble()).toInt().coerceAtLeast(1)
    var currentPage by rememberSaveable(entries.size) { mutableStateOf(0) }
    currentPage = currentPage.coerceIn(0, totalPages - 1)

    val pageEntries = entries
        .drop(currentPage * HEART_RATE_HISTORY_PAGE_SIZE)
        .take(HEART_RATE_HISTORY_PAGE_SIZE)
    val groupedEntries = pageEntries.groupBy { entry ->
        entry.measuredAt.atZone(ZoneId.systemDefault()).toLocalDate()
    }

    Column(
        modifier = Modifier.padding(top = ChampionSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Página ${currentPage + 1} de $totalPages",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "${numberFormat().format(entries.size)} medições",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        groupedEntries.forEach { (date, grouped) ->
            Text(
                text = formatHistoryDay(date),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            grouped.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
                    ) {
                        Text(
                            text = formatMeasurementMoment(entry.measuredAt),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = entry.source.toFriendlySourceLabel(associatedDeviceName),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(modifier = Modifier.width(ChampionSpacing.medium))
                    Text(
                        text = "${entry.bpm} BPM",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
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
                    Text("Anterior", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = { currentPage = (currentPage + 1).coerceAtMost(totalPages - 1) },
                    modifier = Modifier.weight(1f),
                    enabled = currentPage < totalPages - 1,
                ) {
                    Text("Próxima", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun HeartRateSessionSurface(
    state: HealthUiState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (val surface = state.surfaceState) {
        HealthSurfaceState.Idle -> IdleMeasurementCard(state = state, onStart = onStart)
        is HealthSurfaceState.Preparing -> MeasurementProgressCard("Preparando medição", "Validando dispositivo associado e preparando a sessão manual.", surface.session, false, onCancel)
        is HealthSurfaceState.Connecting -> MeasurementProgressCard("Conectando ao relógio", "Aguarde enquanto a conexão é preparada para a medição manual.", surface.session, false, onCancel)
        is HealthSurfaceState.Starting -> MeasurementProgressCard("Iniciando medição", "A medição será iniciada em instantes.", surface.session, false, onCancel)
        is HealthSurfaceState.Measuring -> MeasurementProgressCard("Medindo frequência cardíaca", "Permaneça com o relógio ajustado ao pulso até a conclusão.", surface.session, true, onCancel)
        is HealthSurfaceState.Stopping -> MeasurementProgressCard("Finalizando medição", "Estamos consolidando o resultado final da sessão.", surface.session, true, null)
        is HealthSurfaceState.Completed -> CompletedMeasurementCard(surface.session, onStart, onDismiss)
        is HealthSurfaceState.Cancelled -> CancelledMeasurementCard(surface.session, onStart, onDismiss)
        is HealthSurfaceState.Failed -> FailedMeasurementCard(state, surface.session, onRetry, onDismiss, onStart)
    }
}

@Composable
private fun IdleMeasurementCard(
    state: HealthUiState,
    onStart: () -> Unit,
) {
    HealthCard(title = "Medição manual", modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (state.hasAssociatedDevice) {
                "Toque para iniciar uma medição de 60 segundos no relógio associado."
            } else {
                "Associe um relógio antes de iniciar a medição manual."
            },
            modifier = Modifier.padding(top = ChampionSpacing.medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        state.associatedDeviceName?.let { name ->
            Text(
                text = "Relógio associado: $name",
                modifier = Modifier.padding(top = ChampionSpacing.medium),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        PrimaryActionButton(
            label = "Medir frequência cardíaca",
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.large),
            enabled = state.hasAssociatedDevice,
        )
    }
}

@Composable
private fun MeasurementProgressCard(
    title: String,
    message: String,
    session: ManualHeartRateSessionState,
    showProgress: Boolean,
    onCancel: (() -> Unit)?,
) {
    HealthCard(title = title, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            modifier = Modifier.padding(top = ChampionSpacing.medium).semantics { liveRegion = LiveRegionMode.Polite },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        CurrentHeartRateHero(
            session = session,
            modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.large),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.large),
            horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            MetricCard(
                label = "Tempo restante",
                value = formatRemaining(session.remaining),
                supportingText = "Sessão de 60 segundos",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Qualidade do sinal",
                value = session.signalQuality.toUiLabel(),
                supportingText = session.signalQuality.toGuidance(),
                modifier = Modifier.weight(1f),
            )
        }
        if (showProgress) {
            val progress = session.progress.coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ChampionSpacing.large)
                    .semantics {
                        contentDescription = "Progresso da medição"
                        stateDescription = "${(progress * 100).roundToInt()} por cento concluído"
                        progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(progress, 0f..1f)
                    },
            )
        }
        onCancel?.let {
            PrimaryActionButton(
                label = "Cancelar medição",
                onClick = it,
                modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.large),
            )
        }
    }
}

@Composable
private fun CurrentHeartRateHero(
    session: ManualHeartRateSessionState,
    modifier: Modifier = Modifier,
) {
    val reading = session.currentReading
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ChampionSpacing.large)
                .semantics(mergeDescendants = true) {
                    contentDescription = when (reading) {
                        null -> "Aguardando a primeira leitura da frequência cardíaca"
                        else -> "Frequência cardíaca atual: ${reading.bpm} batimentos por minuto"
                    }
                    liveRegion = LiveRegionMode.Polite
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = reading?.bpm?.let { "$it BPM" } ?: "-- BPM",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = when (reading?.source) {
                    ManualHeartRateReadingSource.D0_LIVE -> "Leitura confirmada pelo relógio"
                    ManualHeartRateReadingSource.F002_ESTIMATE -> "Valor estimado a partir do sinal"
                    null -> "Aguardando a primeira leitura"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CompletedMeasurementCard(
    session: ManualHeartRateSessionState,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    val result = session.finalResult
    HealthCard(title = "Medição concluída", modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "A medição foi concluída e o resultado final foi salvo com sucesso.",
            modifier = Modifier.padding(top = ChampionSpacing.medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        CurrentHeartRateHero(
            session = session.copy(currentReading = result?.toCurrentReading()),
            modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.large),
        )
        result?.let {
            Text(
                text = "Horário: ${formatDateTime(it.measuredAt)}",
                modifier = Modifier.padding(top = ChampionSpacing.medium),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = when (it.source) {
                    ManualHeartRateFinalSource.D0_FINAL -> "Resultado final confirmado pelo relógio"
                    ManualHeartRateFinalSource.F002_FINAL -> "Resultado final estimado a partir do sinal"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            PrimaryActionButton(
                label = "Nova medição",
                onClick = {
                    onDismiss()
                    onStart()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Concluir")
            }
        }
    }
}

@Composable
private fun CancelledMeasurementCard(
    session: ManualHeartRateSessionState,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text(text = "Medição cancelada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = session.error?.message ?: "A medição foi cancelada antes da conclusão e nenhum dado parcial foi salvo.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
            ) {
                PrimaryActionButton(
                    label = "Nova medição",
                    onClick = {
                        onDismiss()
                        onStart()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Fechar")
                }
            }
        }
    }
}

@Composable
private fun FailedMeasurementCard(
    state: HealthUiState,
    session: ManualHeartRateSessionState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
) {
    val error = session.error
    if (error == null) {
        EmptyState(
            title = "Falha ao concluir a medição",
            message = "Não foi possível determinar um estado seguro para a sessão manual.",
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    ErrorState(
        title = "Não foi possível concluir a medição",
        message = error.message,
        actionLabel = when {
            error.isRetryable -> "Tentar novamente"
            state.hasAssociatedDevice -> "Nova medição"
            else -> null
        },
        onAction = when {
            error.isRetryable -> onRetry
            state.hasAssociatedDevice -> onStart
            else -> null
        },
        modifier = Modifier.fillMaxWidth(),
    )

    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
        Text("Fechar aviso")
    }
}

@Composable
private fun LatestMeasurementCard(
    measurement: HeartRateMeasurement?,
    hasAssociatedDevice: Boolean,
    associatedDeviceName: String?,
) {
    if (measurement == null) {
        EmptyState(
            title = "Sem medições salvas",
            message = if (hasAssociatedDevice) {
                "Quando você concluir a primeira medição manual, o último resultado aparecerá aqui."
            } else {
                "Associe um relógio e conclua uma medição manual para começar o histórico de frequência cardíaca."
            },
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    HealthCard(title = "Última medição", modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${measurement.bpm} BPM",
            modifier = Modifier.padding(top = ChampionSpacing.medium),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = measurement.measuredAt?.let(::formatMeasurementMoment) ?: "Horário indisponível",
            modifier = Modifier.padding(top = ChampionSpacing.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = associatedDeviceName?.let { "Origem: $it" } ?: "Origem: relógio associado",
            modifier = Modifier.padding(top = ChampionSpacing.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun RecentMeasurementsList(measurements: List<HeartRateMeasurement>) {
    if (measurements.isEmpty()) {
        HealthMetricEmptyState(message = "Ainda não há medições manuais recentes para exibir.")
        return
    }

    Column(
        modifier = Modifier.padding(top = ChampionSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        measurements.forEach { measurement ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
                ) {
                    Text(
                        text = measurement.measuredAt?.let(::formatMeasurementMoment) ?: "Horário indisponível",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = measurement.source.toMeasurementSourceLabel(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = "${measurement.bpm} BPM",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun HealthMetricChartSection(
    title: String,
    message: String,
) {
    HealthCard(title = title, modifier = Modifier.fillMaxWidth()) {
        HealthMetricEmptyState(message = message)
    }
}

@Composable
private fun HealthMetricHistorySection(
    title: String,
    emptyMessage: String,
    content: (@Composable () -> Unit)? = null,
) {
    HealthCard(title = title, modifier = Modifier.fillMaxWidth()) {
        if (content == null) {
            HealthMetricEmptyState(message = emptyMessage)
        } else {
            content()
        }
    }
}

@Composable
private fun HealthMetricEmptyState(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(top = ChampionSpacing.medium),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun MetricValueBlock(
    value: String,
    supporting: String,
    extra: String? = null,
) {
    Text(
        text = value,
        modifier = Modifier.padding(top = ChampionSpacing.medium),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = supporting,
        modifier = Modifier.padding(top = ChampionSpacing.small),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
    extra?.let {
        Text(
            text = it,
            modifier = Modifier.padding(top = ChampionSpacing.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun activityHealthViewModel(): HealthViewModel {
    val activity = checkNotNull(LocalActivity.current) as ComponentActivity
    return remember(activity) {
        ViewModelProvider(activity)[HealthViewModel::class.java]
    }
}

private fun HealthSurfaceState.isMeasurementInFlight(): Boolean = when (this) {
    is HealthSurfaceState.Preparing,
    is HealthSurfaceState.Connecting,
    is HealthSurfaceState.Starting,
    is HealthSurfaceState.Measuring,
    is HealthSurfaceState.Stopping,
    -> true

    HealthSurfaceState.Idle,
    is HealthSurfaceState.Completed,
    is HealthSurfaceState.Cancelled,
    is HealthSurfaceState.Failed,
    -> false
}

private fun D1HeartRateImportSurfaceState.isImportInFlight(): Boolean = when (this) {
    is D1HeartRateImportSurfaceState.Preparing,
    is D1HeartRateImportSurfaceState.Connecting,
    is D1HeartRateImportSurfaceState.Reading,
    is D1HeartRateImportSurfaceState.Persisting,
    -> true

    D1HeartRateImportSurfaceState.Idle,
    is D1HeartRateImportSurfaceState.Completed,
    is D1HeartRateImportSurfaceState.CompletedWithGaps,
    is D1HeartRateImportSurfaceState.Partial,
    is D1HeartRateImportSurfaceState.Failed,
    is D1HeartRateImportSurfaceState.Cancelled,
    -> false
}

private fun D1HeartRateImportSurfaceState.isRetryableFailure(): Boolean =
    (this as? D1HeartRateImportSurfaceState.Failed)?.session?.error?.isRetryable == true

private fun D1HeartRateImportSurfaceState.isTerminalSurface(): Boolean = when (this) {
    is D1HeartRateImportSurfaceState.Completed,
    is D1HeartRateImportSurfaceState.CompletedWithGaps,
    is D1HeartRateImportSurfaceState.Partial,
    is D1HeartRateImportSurfaceState.Failed,
    is D1HeartRateImportSurfaceState.Cancelled,
    -> true

    D1HeartRateImportSurfaceState.Idle,
    is D1HeartRateImportSurfaceState.Preparing,
    is D1HeartRateImportSurfaceState.Connecting,
    is D1HeartRateImportSurfaceState.Reading,
    is D1HeartRateImportSurfaceState.Persisting,
    -> false
}

private fun D1HeartRateImportSurfaceState.sessionOrNull(): D1HeartRateImportSessionState? = when (this) {
    D1HeartRateImportSurfaceState.Idle -> null
    is D1HeartRateImportSurfaceState.Preparing -> session
    is D1HeartRateImportSurfaceState.Connecting -> session
    is D1HeartRateImportSurfaceState.Reading -> session
    is D1HeartRateImportSurfaceState.Persisting -> session
    is D1HeartRateImportSurfaceState.Completed -> session
    is D1HeartRateImportSurfaceState.CompletedWithGaps -> session
    is D1HeartRateImportSurfaceState.Partial -> session
    is D1HeartRateImportSurfaceState.Failed -> session
    is D1HeartRateImportSurfaceState.Cancelled -> session
}

private fun D1HeartRateImportSurfaceState.toUiTitle(): String = when (this) {
    D1HeartRateImportSurfaceState.Idle -> "Importação D1"
    is D1HeartRateImportSurfaceState.Preparing -> "Preparando importação D1"
    is D1HeartRateImportSurfaceState.Connecting -> "Conectando ao relógio"
    is D1HeartRateImportSurfaceState.Reading -> "Lendo histórico do relógio"
    is D1HeartRateImportSurfaceState.Persisting -> "Salvando histórico importado"
    is D1HeartRateImportSurfaceState.Completed -> "Dados do relógio importados"
    is D1HeartRateImportSurfaceState.CompletedWithGaps -> "Dados importados com lacunas"
    is D1HeartRateImportSurfaceState.Partial -> "Importação parcial concluída"
    is D1HeartRateImportSurfaceState.Failed -> "Não foi possível buscar os dados"
    is D1HeartRateImportSurfaceState.Cancelled -> "Importação cancelada"
}

private fun D1HeartRateImportSurfaceState.toUiMessage(): String = when (this) {
    D1HeartRateImportSurfaceState.Idle -> "A importação D1 acontece somente por toque explícito em Buscar dados do relógio."
    is D1HeartRateImportSurfaceState.Preparing -> "Validando relógio associado e reservando a operação Bluetooth de foreground."
    is D1HeartRateImportSurfaceState.Connecting -> "Aguardando conexão e handshake A1 antes do comando D1."
    is D1HeartRateImportSurfaceState.Reading -> "Recebendo pacotes D1 e preservando dados válidos mesmo em timeout parcial."
    is D1HeartRateImportSurfaceState.Persisting -> "Persistindo histórico local, deduplicando registros e atualizando metadata da última importação."
    is D1HeartRateImportSurfaceState.Completed -> "Histórico local atualizado com sucesso a partir do relógio."
    is D1HeartRateImportSurfaceState.CompletedWithGaps -> "Importação concluída, mas o relógio informou lacunas entre pacotes recebidos."
    is D1HeartRateImportSurfaceState.Partial -> "Parte dos dados válidos foi salva antes do encerramento da tentativa."
    is D1HeartRateImportSurfaceState.Failed -> session.error?.message
        ?: "Não foi possível concluir a importação D1 com segurança."
    is D1HeartRateImportSurfaceState.Cancelled -> session.error?.message
        ?: "A tentativa foi cancelada antes da conclusão."
}

private fun ManualHeartRateSignalQuality?.toUiLabel(): String = when (this) {
    ManualHeartRateSignalQuality.GOOD -> "Boa"
    ManualHeartRateSignalQuality.REGULAR -> "Regular"
    ManualHeartRateSignalQuality.LOW -> "Baixa"
    null -> "Aguardando"
}

private fun ManualHeartRateSignalQuality?.toGuidance(): String = when (this) {
    ManualHeartRateSignalQuality.GOOD -> "Sinal estável para a sessão"
    ManualHeartRateSignalQuality.REGULAR -> "Mantenha o pulso parado para melhorar a leitura"
    ManualHeartRateSignalQuality.LOW -> "Ajuste o relógio ao pulso para melhorar o sinal"
    null -> "A qualidade aparecerá quando houver sinal suficiente"
}

private fun ManualHeartRateResult.toCurrentReading(): ManualHeartRateCurrentReading =
    ManualHeartRateCurrentReading(
        bpm = bpm,
        source = when (source) {
            ManualHeartRateFinalSource.D0_FINAL -> ManualHeartRateReadingSource.D0_LIVE
            ManualHeartRateFinalSource.F002_FINAL -> ManualHeartRateReadingSource.F002_ESTIMATE
        },
        observedAt = measuredAt,
    )

private fun formatRemaining(duration: Duration): String {
    val totalSeconds = duration.inWholeSeconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatMeasurementMoment(instant: Instant): String {
    val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now(ZoneId.systemDefault())
    val prefix = when (date) {
        today -> "Hoje"
        today.minusDays(1) -> "Ontem"
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)
    return "$prefix, $time"
}

private fun formatHistoryDay(date: LocalDate): String {
    val today = LocalDate.now(ZoneId.systemDefault())
    return when (date) {
        today -> "Hoje"
        today.minusDays(1) -> "Ontem"
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .format(date)
    }
}

private fun formatDateTime(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)

private fun br.com.mwconsulting.championcompanion.core.model.HeartRateSource.toMeasurementSourceLabel(): String = when (this) {
    br.com.mwconsulting.championcompanion.core.model.HeartRateSource.MANUAL_MEASUREMENT -> "Medição manual"
    br.com.mwconsulting.championcompanion.core.model.HeartRateSource.MEASUREMENT_STREAM -> "Fluxo de medição"
    br.com.mwconsulting.championcompanion.core.model.HeartRateSource.WATCH_D1_HISTORY -> "Histórico do relógio"
    br.com.mwconsulting.championcompanion.core.model.HeartRateSource.APP_WORKOUT_SESSION -> "Sessão iniciada no app"
    br.com.mwconsulting.championcompanion.core.model.HeartRateSource.WATCH_WORKOUT_ACTIVITY -> "Atividade do relógio"
}

private fun br.com.mwconsulting.championcompanion.core.model.HeartRateSource.toFriendlySourceLabel(
    associatedDeviceName: String?,
): String = when (this) {
    br.com.mwconsulting.championcompanion.core.model.HeartRateSource.MANUAL_MEASUREMENT -> "Medição manual"
    br.com.mwconsulting.championcompanion.core.model.HeartRateSource.WATCH_D1_HISTORY -> associatedDeviceName ?: "Champion C001"
    else -> toMeasurementSourceLabel()
}

private fun buildLatestImportLabel(
    metadata: D1HeartRateImportMetadata,
    associatedDeviceName: String?,
): String {
    val source = associatedDeviceName ?: "relógio associado"
    val attemptLabel = formatDateTime(metadata.lastAttemptAt)
    val successLabel = metadata.lastSuccessAt?.let(::formatDateTime)
    val resultLabel = when (metadata.lastResult) {
        D1HeartRateImportStatus.COMPLETED -> "sucesso"
        D1HeartRateImportStatus.COMPLETED_WITH_GAPS -> "sucesso com lacunas"
        D1HeartRateImportStatus.PARTIAL -> "parcial"
        D1HeartRateImportStatus.FAILED -> "falha"
        D1HeartRateImportStatus.CANCELLED -> "cancelada"
    }
    return buildString {
        append("Última importação D1 de ")
        append(source)
        append(": tentativa em ")
        append(attemptLabel)
        append(" • resultado ")
        append(resultLabel)
        successLabel?.let {
            append(" • concluída em ")
            append(it)
        }
    }
}

private fun periodToLabel(period: HeartRateHistoryPeriod): String = when (period) {
    HeartRateHistoryPeriod.TODAY -> "Hoje"
    HeartRateHistoryPeriod.LAST_7_DAYS -> "7 dias"
    HeartRateHistoryPeriod.LAST_30_DAYS -> "30 dias"
}

private fun compactPeriodToLabel(period: HeartRateHistoryPeriod): String = when (period) {
    HeartRateHistoryPeriod.TODAY -> "D"
    HeartRateHistoryPeriod.LAST_7_DAYS -> "S"
    HeartRateHistoryPeriod.LAST_30_DAYS -> "M"
}

private fun periodHeadline(period: HeartRateHistoryPeriod): String = when (period) {
    HeartRateHistoryPeriod.TODAY -> "Hoje"
    HeartRateHistoryPeriod.LAST_7_DAYS -> buildRelativeDateRangeLabel(days = 7)
    HeartRateHistoryPeriod.LAST_30_DAYS -> buildRelativeDateRangeLabel(days = 30)
}

private fun heartRateRangeHeadline(
    aggregates: br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryAggregates,
): String = if (aggregates.measurementCount == 0) {
    "Sem dados"
} else {
    val min = aggregates.minimumBpm?.toString() ?: "--"
    val max = aggregates.maximumBpm?.toString() ?: "--"
    "$min a $max bpm"
}

private fun heartRateRangeSupportingText(
    aggregates: br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryAggregates,
    entryCount: Int,
    pointsCount: Int,
): String = if (aggregates.measurementCount == 0) {
    "Use medição manual ou importação D1 para ver tendências no período selecionado."
} else {
    "${numberFormat().format(entryCount)} medições válidas distribuídas em ${numberFormat().format(pointsCount)} ponto(s) visuais."
}

private fun buildRelativeDateRangeLabel(days: Long): String {
    val zoneId = ZoneId.systemDefault()
    val end = LocalDate.now(zoneId)
    val start = end.minusDays(days - 1)
    val formatter = DateTimeFormatter.ofPattern("d 'de' MMM.", Locale.forLanguageTag("pt-BR"))
    return "${start.format(formatter)}-${end.format(formatter)}"
}

internal fun br.com.mwconsulting.championcompanion.core.model.DataOrigin.toHealthOriginLabel(): String = when (this) {
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.WATCH_DAILY_SUMMARY -> "Origem: relógio"
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.WATCH_D1_HISTORY -> "Origem: histórico do relógio"
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.WATCH_D3_D4 -> "Origem: relógio"
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.MANUAL_ENTRY -> "Origem: entrada manual"
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.APP_WORKOUT_SESSION -> "Origem: sessão do app"
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.DERIVED -> "Origem: derivado"
}

private fun numberFormat(): NumberFormat = NumberFormat.getIntegerInstance(Locale.getDefault())

private fun decimalFormat(): NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    maximumFractionDigits = 1
    minimumFractionDigits = 0
}
