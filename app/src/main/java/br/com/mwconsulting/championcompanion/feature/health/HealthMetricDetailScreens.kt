package br.com.mwconsulting.championcompanion.feature.health

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignHeaderNavigationAction
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignSummaryCard
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignSurfaceHeader
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignTheme
import br.com.mwconsulting.championcompanion.core.designsystem.LocalChampionRedesignTokens
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.HealthCard
import br.com.mwconsulting.championcompanion.core.designsystem.DarkColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.LightColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.PrimaryActionButton
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryEntry
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryPeriod
import br.com.mwconsulting.championcompanion.core.model.HeartRateChartPoint
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryAggregates
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.model.WeightMeasurement
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartRateDetailScreen(
    onNavigateBack: () -> Unit,
    backLabel: String = "Voltar para Saúde",
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
    val triggerD1Refresh = {
        if (!isMeasurementInFlight && !isD1ImportInFlight) {
            healthViewModel.onIntent(HealthIntent.StartD1Import)
        }
    }

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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(backLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text(if (isD1ImportInFlight) "Continuar importando" else "Continuar medindo")
                }
            },
        )
    }

    ChampionRedesignTheme {
        val tokens = LocalChampionRedesignTokens.current
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            PullToRefreshBox(
                isRefreshing = isD1ImportInFlight,
                onRefresh = triggerD1Refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics {
                            customActions = listOf(
                                CustomAccessibilityAction("Atualizar histórico do relógio") {
                                    triggerD1Refresh()
                                    true
                                },
                            )
                        },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = tokens.spacing.screen, vertical = tokens.spacing.compact)
                            .heightIn(min = 1.dp),
                        verticalArrangement = Arrangement.spacedBy(tokens.spacing.item),
                    ) {
                        HeartRateDetailHeader(
                            onNavigateBack = onNavigateBack,
                            backLabel = backLabel,
                        )
                        HeartRateHistoryChartSection(
                            state = state,
                            onSelectPeriod = { healthViewModel.onIntent(HealthIntent.SelectHistoryPeriod(it)) },
                        )
                        if (state.history.period == HeartRateHistoryPeriod.TODAY) {
                            HealthMetricHistorySection(
                                title = "Leituras por faixa",
                                emptyMessage = "Nenhuma leitura disponível neste período.",
                            ) {
                                HeartRateReadingsByZone(points = state.history.chart.points)
                            }
                        } else {
                            HealthMetricHistorySection(
                                title = "Histórico completo",
                                emptyMessage = "Nenhum valor disponível neste período; lacunas permanecem sem dados.",
                            ) {
                                HeartRateHistoryList(
                                    entries = state.history.entries,
                                    period = state.history.period,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeartRateReadingsByZone(points: List<HeartRateChartPoint>) {
    val rows = heartRateZoneDurations(points)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ChampionSpacing.medium)
            .semantics(mergeDescendants = true) {
                contentDescription = "Leituras por faixa: ${rows.joinToString { (range, duration) -> "${range.label}, ${formatObservedDuration(duration)}" }}."
            },
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
    ) {
        rows.forEach { (displayRange, duration) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(displayRange.color, CircleShape),
                    )
                    Text(
                        text = displayRange.label,
                        modifier = Modifier.padding(start = ChampionSpacing.small),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = formatObservedDuration(duration),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Text(
            text = "Tempo observado entre leituras válidas; sem reamostragem e sem atribuir duração ao último ponto.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun heartRateZoneDurations(
    points: List<HeartRateChartPoint>,
): List<Pair<HeartRateDisplayRange, Duration>> {
    val durations = HEART_RATE_DISPLAY_RANGES.associateWith { Duration.ZERO }.toMutableMap()
    points
        .sortedBy { it.timestamp }
        .zipWithNext()
        .forEach { (start, end) ->
            if (start.bpm == null || start.sampleCount <= 0 ||
                end.bpm == null || end.sampleCount <= 0
            ) {
                return@forEach
            }
            val elapsed = Duration.between(start.timestamp, end.timestamp)
            if (elapsed.isZero || elapsed.isNegative) return@forEach

            val displayRange = start.bpm?.let { bpm ->
                HEART_RATE_DISPLAY_RANGES.firstOrNull { it.contains(bpm) }
            } ?: return@forEach
            durations[displayRange] = durations.getValue(displayRange).plus(elapsed)
        }
    return HEART_RATE_DISPLAY_RANGES.map { it to durations.getValue(it) }
}

private fun formatObservedDuration(duration: Duration): String {
    val totalMinutes = duration.toMinutes().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours == 0L) {
        "$minutes min"
    } else {
        "$hours h ${minutes.toString().padStart(2, '0')} min"
    }
}

private data class HeartRateDisplayRange(
    val label: String,
    val minimum: Double,
    val maximum: Double,
    val includeMaximum: Boolean,
    val color: androidx.compose.ui.graphics.Color,
) {
    fun contains(value: Double): Boolean = value >= minimum &&
        (value < maximum || includeMaximum && value <= maximum)
}

private val HEART_RATE_DISPLAY_RANGES = listOf(
    HeartRateDisplayRange("Zona 1 · 30–79 bpm", 30.0, 80.0, false, androidx.compose.ui.graphics.Color(0xFF175A62)),
    HeartRateDisplayRange("Zona 2 · 80–129 bpm", 80.0, 130.0, false, androidx.compose.ui.graphics.Color(0xFF267A55)),
    HeartRateDisplayRange("Zona 3 · 130–169 bpm", 130.0, 170.0, false, androidx.compose.ui.graphics.Color(0xFF8B7624)),
    HeartRateDisplayRange("Zona 4 · 170–220 bpm", 170.0, 220.0, true, androidx.compose.ui.graphics.Color(0xFF8E2730)),
)

@Composable
fun WeightDetailScreen(
    onNavigateBack: () -> Unit,
    backLabel: String = "Voltar para Saúde",
    healthViewModel: HealthViewModel = activityHealthViewModel(),
) {
    val state by healthViewModel.state.collectAsState()
    MetricDetailScaffold(
        title = "Peso",
        description = "Acompanhe seu valor atual e registre novas medições em quilogramas.",
        onNavigateBack = onNavigateBack,
        backLabel = backLabel,
        contentModifier = Modifier.imePadding(),
    ) {
        val weightHistory = state.weightHistory
        val weight = state.latestWeight ?: (weightHistory as? HealthWeightHistoryState.Available)
            ?.measurements
            ?.firstNotNullOfOrNull { it.toHealthWeightSnapshotOrNull() }
        val currentValue = weight?.let { "${decimalFormat().format(it.kilograms)} kg" } ?: when (weightHistory) {
            HealthWeightHistoryState.Loading -> "Carregando"
            HealthWeightHistoryState.Unavailable -> "Indisponível"
            is HealthWeightHistoryState.Available -> "Sem dados"
        }
        val currentSupportingText = weight?.let { "Último registro: ${formatDateTime(it.measuredAt)}" }
            ?: when (weightHistory) {
                HealthWeightHistoryState.Loading -> "Carregando histórico local"
                HealthWeightHistoryState.Unavailable -> "Não foi possível carregar o histórico local"
                is HealthWeightHistoryState.Available -> "Nenhum registro válido ainda"
            }
        ChampionRedesignSummaryCard(
            title = "Valor atual",
            value = currentValue,
            supportingText = currentSupportingText,
            modifier = Modifier.fillMaxWidth(),
        ) {
            when {
                weight != null -> {
                    Text(
                        text = weight.origin?.toHealthOriginLabel() ?: "Origem: não disponível",
                        modifier = Modifier.padding(top = ChampionSpacing.small),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                weightHistory is HealthWeightHistoryState.Loading -> Text(
                    text = "Carregando valor atual…",
                    modifier = Modifier.padding(top = ChampionSpacing.small),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                weightHistory is HealthWeightHistoryState.Unavailable -> Text(
                    text = "O valor atual está indisponível porque não foi possível ler os registros locais.",
                    modifier = Modifier.padding(top = ChampionSpacing.small),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                weightHistory is HealthWeightHistoryState.Available && weightHistory.measurements.isNotEmpty() -> Text(
                    text = "Não foi possível determinar um valor atual válido a partir dos registros locais.",
                    modifier = Modifier.padding(top = ChampionSpacing.small),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> HealthMetricEmptyState(message = "Nenhum peso persistido foi encontrado até agora.")
            }
        }
        val filteredMeasurements = (weightHistory as? HealthWeightHistoryState.Available)
            ?.measurements
            ?.let { measurements ->
                filterWeightMeasurements(
                    measurements = measurements,
                    period = state.selectedWeightHistoryPeriod,
                )
            }
        PrimaryActionButton(
            label = "Registrar peso",
            onClick = { healthViewModel.onIntent(HealthIntent.OpenWeightEditor) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSavingWeight,
        )
        if (state.isWeightEditorOpen) {
            WeightEditor(
                state = state,
                onValueChange = { healthViewModel.onIntent(HealthIntent.WeightChanged(it)) },
                onSave = { healthViewModel.onIntent(HealthIntent.SaveWeight) },
                onCancel = { healthViewModel.onIntent(HealthIntent.CancelWeight) },
            )
        }
        state.weightMessage?.let { message ->
            WeightMessageCard(message = message) {
                healthViewModel.onIntent(HealthIntent.DismissWeightMessage)
            }
        }
        WeightHistoryTrendSection(
            history = weightHistory,
            selectedPeriod = state.selectedWeightHistoryPeriod,
            filteredMeasurements = filteredMeasurements.orEmpty(),
            onSelectPeriod = { healthViewModel.onIntent(HealthIntent.SelectWeightHistoryPeriod(it)) },
        )
        HealthMetricHistorySection(
            title = "Histórico de registros",
            emptyMessage = "Ainda não há medições de peso válidas neste período.",
        ) {
            when (val history = state.weightHistory) {
                HealthWeightHistoryState.Loading -> Text(
                    text = "Carregando histórico…",
                    modifier = Modifier.padding(top = ChampionSpacing.medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HealthWeightHistoryState.Unavailable -> ErrorState(
                    title = "Histórico indisponível",
                    message = "Não foi possível ler o histórico local agora.",
                    modifier = Modifier.fillMaxWidth(),
                )
                is HealthWeightHistoryState.Available -> WeightHistoryList(
                    measurements = filteredMeasurements.orEmpty(),
                    period = state.selectedWeightHistoryPeriod,
                    emptyMessage = "Nenhuma medição de peso válida foi encontrada neste período.",
                )
            }
        }
    }
}

@Composable
private fun WeightHistoryTrendSection(
    history: HealthWeightHistoryState,
    selectedPeriod: WeightHistoryPeriod,
    filteredMeasurements: List<WeightMeasurement>,
    onSelectPeriod: (WeightHistoryPeriod) -> Unit,
) {
    val trendValue = filteredMeasurements
        .firstOrNull()
        ?.toKilogramsOrNull()
        ?.let { "${decimalFormat().format(it)} kg" }
        ?: when (history) {
            HealthWeightHistoryState.Loading -> "Carregando"
            HealthWeightHistoryState.Unavailable -> "Indisponível"
            is HealthWeightHistoryState.Available -> "Sem dados"
        }
    ChampionRedesignSummaryCard(
        title = "Tendência",
        value = trendValue,
        supportingText = "${selectedPeriod.toWeightPeriodLabel()} • kg",
        modifier = Modifier.fillMaxWidth(),
    ) {
        WeightHistoryPeriodSelector(
            selected = selectedPeriod,
            onSelect = onSelectPeriod,
        )
        when (history) {
            HealthWeightHistoryState.Loading -> Text(
                text = "Carregando histórico local…",
                modifier = Modifier.padding(top = ChampionSpacing.medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HealthWeightHistoryState.Unavailable -> ErrorState(
                title = "Histórico indisponível",
                message = "Não foi possível ler o histórico local agora.",
                modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.medium),
            )
            is HealthWeightHistoryState.Available -> WeightHistoryChart(
                measurements = filteredMeasurements,
                period = selectedPeriod,
                modifier = Modifier.padding(top = ChampionSpacing.large),
            )
        }
    }
}

@Composable
private fun WeightHistoryPeriodSelector(
    selected: WeightHistoryPeriod,
    onSelect: (WeightHistoryPeriod) -> Unit,
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
            WeightHistoryPeriod.entries.forEach { period ->
                val isSelected = period == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(period) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            this.selected = isSelected
                            contentDescription = when (period) {
                                WeightHistoryPeriod.THREE_MONTHS -> "Mês atual até hoje e dois meses civis anteriores"
                                WeightHistoryPeriod.YEAR -> "Mês atual até hoje e onze meses civis anteriores"
                            }
                            stateDescription = if (isSelected) "Selecionado" else "Não selecionado"
                        },
                    label = {
                        Text(
                            text = period.toWeightPeriodLabel(),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun WeightEditor(
    state: HealthUiState,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    HealthCard(title = "Registrar peso", modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.weightInput,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.medium),
            label = { Text("Valor em quilogramas") },
            suffix = { Text("kg") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Text(
            text = "Data e hora atuais serão usadas automaticamente.",
            modifier = Modifier.padding(top = ChampionSpacing.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        PrimaryActionButton(
            label = "Salvar medição",
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.large),
            enabled = !state.isSavingWeight,
            isLoading = state.isSavingWeight,
        )
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth(), enabled = !state.isSavingWeight) {
            Text("Cancelar")
        }
    }
}

@Composable
private fun WeightMessageCard(message: HealthWeightMessage, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(
            containerColor = when (message) {
                HealthWeightMessage.SAVED -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            },
            contentColor = when (message) {
                HealthWeightMessage.SAVED -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onErrorContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = when (message) {
                    HealthWeightMessage.SAVED -> "Registro salvo"
                    HealthWeightMessage.INVALID_VALUE -> "Valor inválido"
                    HealthWeightMessage.SAVE_FAILED -> "Não foi possível salvar"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = when (message) {
                    HealthWeightMessage.SAVED -> "Medição salva localmente com sucesso."
                    HealthWeightMessage.INVALID_VALUE -> "Informe um peso positivo válido em quilogramas."
                    HealthWeightMessage.SAVE_FAILED -> "Não foi possível salvar medição local. Tente novamente."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Fechar aviso") }
        }
    }
}

@Composable
private fun WeightHistoryList(
    measurements: List<WeightMeasurement>,
    period: WeightHistoryPeriod,
    emptyMessage: String,
) {
    val items = buildWeightHistoryListItems(measurements, period)
    if (items.isEmpty()) {
        HealthMetricEmptyState(message = emptyMessage)
        return
    }
    Column(
        modifier = Modifier.padding(top = ChampionSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${items.size} períodos com valor",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Média kg",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${item.label}. Média ${decimalFormat().format(item.averageKilograms)} kg."
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.label, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.width(ChampionSpacing.medium))
                Text(
                    text = "${decimalFormat().format(item.averageKilograms)} kg",
                    color = weightPrimaryAccent(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private data class WeightHistoryListItem(
    val label: String,
    val averageKilograms: Double,
)

private fun buildWeightHistoryListItems(
    measurements: List<WeightMeasurement>,
    @Suppress("UNUSED_PARAMETER") period: WeightHistoryPeriod,
): List<WeightHistoryListItem> {
    val zoneId = ZoneId.systemDefault()
    return when (period) {
        WeightHistoryPeriod.THREE_MONTHS,
        WeightHistoryPeriod.YEAR,
        -> measurements
            .groupBy { YearMonth.from(it.measuredAt.atZone(zoneId).toLocalDate()) }
            .toList()
            .sortedByDescending { it.first }
            .mapNotNull { (month, values) ->
                val kilograms = values.mapNotNull(WeightMeasurement::toKilogramsOrNull)
                kilograms.takeIf { it.isNotEmpty() }?.let {
                    WeightHistoryListItem(
                        label = formatWeightHistoryMonth(month),
                        averageKilograms = it.average(),
                    )
                }
            }
    }
}

private fun formatWeightHistoryMonth(month: YearMonth): String =
    HEART_RATE_MONTH_NAMES[month.monthValue - 1]

internal fun WeightMeasurement.toKilogramsOrNull(): Double? = when (unit) {
    br.com.mwconsulting.championcompanion.core.model.WeightUnit.KILOGRAMS -> value
    br.com.mwconsulting.championcompanion.core.model.WeightUnit.POUNDS -> value * 0.45359237
}.takeIf { it.isFinite() && it > 0.0 }

private fun WeightMeasurement.toHealthWeightSnapshotOrNull(): HealthWeightSnapshot? =
    toKilogramsOrNull()?.let { kilograms ->
        HealthWeightSnapshot(
            kilograms = kilograms,
            measuredAt = measuredAt,
            origin = origin,
        )
    }

@Composable
internal fun MetricDetailScaffold(
    title: String,
    description: String,
    onNavigateBack: () -> Unit,
    backLabel: String,
    header: @Composable () -> Unit = {
        MetricDetailHeader(
            title = title,
            description = description,
            onNavigateBack = onNavigateBack,
            backLabel = backLabel,
        )
    },
    contentModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ChampionRedesignTheme {
        val tokens = LocalChampionRedesignTokens.current
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(contentModifier)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = tokens.spacing.screen, vertical = tokens.spacing.compact)
                    .heightIn(min = 1.dp),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.item),
            ) {
                header()
                content()
            }
        }
    }
}

@Composable
private fun MetricDetailHeader(
    title: String,
    description: String,
    onNavigateBack: () -> Unit,
    backLabel: String,
) {
    ChampionRedesignSurfaceHeader(
        title = title,
        supportingText = description,
        navigationContent = {
            ChampionRedesignHeaderNavigationAction(
                contentDescription = backLabel,
                onClick = onNavigateBack,
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
    )
}

@Composable
private fun HeartRateDetailHeader(
    onNavigateBack: () -> Unit,
    backLabel: String,
) {
    MetricDetailHeader(
        title = "Frequência cardíaca",
        description = "Acompanhe seu último valor, leituras do relógio e tendência local.",
        onNavigateBack = onNavigateBack,
        backLabel = backLabel,
    )
}

@Composable
private fun HeartRateHistoryChartSection(
    state: HealthUiState,
    onSelectPeriod: (HeartRateHistoryPeriod) -> Unit,
) {
    val period = state.history.period
    val visualEntries = state.history.entries
    val visualAggregates = visualEntries.toHeartRateHistoryAggregates()
    ChampionRedesignSummaryCard(
        title = "Tendência",
        value = heartRateRangeHeadline(visualAggregates),
        supportingText = "${periodHeadline(period)} • BPM",
        modifier = Modifier.fillMaxWidth(),
    ) {
        HistoryPeriodSelector(
            selected = state.selectedHistoryPeriod,
            onSelectPeriod = onSelectPeriod,
        )

        Text(
            text = "Média ${visualAggregates.averageBpm?.roundToInt()?.toString() ?: "--"} bpm • " +
                "${numberFormat().format(visualEntries.size)} leituras válidas",
            modifier = Modifier.padding(top = ChampionSpacing.medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )

        HeartRateHistoryChart(
            series = state.history.chart,
            period = period,
            origins = visualEntries.map { it.source }.toSet(),
            modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.large),
        )

    }
}

@Composable
private fun HistoryPeriodSelector(
    selected: HeartRateHistoryPeriod,
    onSelectPeriod: (HeartRateHistoryPeriod) -> Unit,
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
            HeartRateHistoryPeriod.entries.forEach { period ->
                val isSelected = period == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectPeriod(period) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            this.selected = isSelected
                            contentDescription = periodToAccessibilityLabel(period)
                            stateDescription = if (isSelected) "Selecionado" else "Não selecionado"
                        },
                    label = {
                        Text(
                            text = periodToFilterLabel(period),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun HeartRateHistoryList(
    entries: List<HeartRateHistoryEntry>,
    period: HeartRateHistoryPeriod,
) {
    val items = buildHeartRateHistoryListItems(entries, period)
    if (items.isEmpty()) {
        HealthMetricEmptyState(message = "Nenhum valor disponível neste período; lacunas permanecem sem dados.")
        return
    }

    Column(
        modifier = Modifier.padding(top = ChampionSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${items.size} períodos com valor",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Média BPM",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${item.label}. Média ${item.averageBpm.roundToInt()} BPM."
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.width(ChampionSpacing.medium))
                Text(
                    text = "${item.averageBpm.roundToInt()} BPM",
                    color = heartRatePrimaryAccent(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private data class HeartRateHistoryListItem(
    val label: String,
    val averageBpm: Double,
)

private fun List<HeartRateHistoryEntry>.toHeartRateHistoryAggregates(): HeartRateHistoryAggregates =
    if (isEmpty()) {
        HeartRateHistoryAggregates()
    } else {
        HeartRateHistoryAggregates(
            measurementCount = size,
            averageBpm = map { it.bpm }.average(),
            minimumBpm = minOf { it.bpm },
            maximumBpm = maxOf { it.bpm },
        )
    }

private fun buildHeartRateHistoryListItems(
    entries: List<HeartRateHistoryEntry>,
    period: HeartRateHistoryPeriod,
): List<HeartRateHistoryListItem> {
    val zoneId = ZoneId.systemDefault()
    return when (period) {
        HeartRateHistoryPeriod.LAST_7_DAYS -> entries
            .groupBy { it.measuredAt.atZone(zoneId).toLocalDate() }
            .toList()
            .sortedByDescending { it.first }
            .map { (date, values) ->
                HeartRateHistoryListItem(
                    label = formatHeartRateDayLabel(date),
                    averageBpm = values.map { it.bpm }.average(),
                )
            }

        HeartRateHistoryPeriod.LAST_30_DAYS -> {
            val month = YearMonth.now(zoneId)
            calendarWeeksFor(month, through = LocalDate.now(zoneId)).mapNotNull { (from, until) ->
                val values = entries.filter { entry ->
                    val date = entry.measuredAt.atZone(zoneId).toLocalDate()
                    date >= from && date <= until
                }
                values.takeIf { it.isNotEmpty() }?.let {
                    HeartRateHistoryListItem(
                        label = formatHeartRateDateRange(from, until),
                        averageBpm = it.map { entry -> entry.bpm }.average(),
                    )
                }
            }.reversed()
        }

        HeartRateHistoryPeriod.LAST_90_DAYS,
        HeartRateHistoryPeriod.LAST_365_DAYS,
        -> entries
            .groupBy { it.measuredAt.atZone(zoneId).let(YearMonth::from) }
            .toList()
            .sortedByDescending { it.first }
            .map { (month, values) ->
                HeartRateHistoryListItem(
                    label = formatHeartRateMonth(month),
                    averageBpm = values.map { it.bpm }.average(),
                )
            }

        HeartRateHistoryPeriod.TODAY -> emptyList()
    }
}

private fun calendarWeeksFor(month: YearMonth, through: LocalDate): List<Pair<LocalDate, LocalDate>> {
    val monthEnd = minOf(month.atEndOfMonth(), through)
    val weeks = mutableListOf<Pair<LocalDate, LocalDate>>()
    var from = month.atDay(1)
    while (!from.isAfter(monthEnd)) {
        val until = minOf(monthEnd, from.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SATURDAY)))
        weeks += from to until
        from = until.plusDays(1)
    }
    return weeks
}

private fun formatHeartRateDayLabel(date: LocalDate): String {
    val today = LocalDate.now(ZoneId.systemDefault())
    return when (date) {
        today -> "Hoje"
        today.minusDays(1) -> "Ontem"
        else -> DateTimeFormatter.ofPattern("EEE d 'de' MMM.", HEART_RATE_PT_BR)
            .format(date)
            .replaceFirstChar { it.titlecase(HEART_RATE_PT_BR) }
    }
}

private fun formatHeartRateDateRange(from: LocalDate, until: LocalDate): String =
    "${HEART_RATE_RANGE_FORMATTER.format(from)} - ${HEART_RATE_RANGE_FORMATTER.format(until)}"

private fun formatHeartRateMonth(month: YearMonth): String =
    HEART_RATE_MONTH_NAMES[month.monthValue - 1]

private val HEART_RATE_PT_BR = Locale.forLanguageTag("pt-BR")
private val HEART_RATE_RANGE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM", HEART_RATE_PT_BR)
private val HEART_RATE_MONTH_NAMES = listOf(
    "JAN", "FEV", "MAR", "ABR", "MAI", "JUN",
    "JUL", "AGO", "SET", "OUT", "NOV", "DEZ",
)

@Composable
internal fun HealthMetricChartSection(
    title: String,
    message: String,
) {
    HealthCard(title = title, modifier = Modifier.fillMaxWidth()) {
        HealthMetricEmptyState(message = message)
    }
}

@Composable
internal fun HealthMetricHistorySection(
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
internal fun HealthMetricEmptyState(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(top = ChampionSpacing.medium),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
internal fun MetricValueBlock(
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
internal fun activityHealthViewModel(): HealthViewModel {
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

internal fun formatDateTime(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)

private fun periodToLabel(period: HeartRateHistoryPeriod): String = when (period) {
    HeartRateHistoryPeriod.TODAY -> "Hoje"
    HeartRateHistoryPeriod.LAST_7_DAYS -> "Semana"
    HeartRateHistoryPeriod.LAST_30_DAYS -> "M"
    HeartRateHistoryPeriod.LAST_90_DAYS -> "3M"
    HeartRateHistoryPeriod.LAST_365_DAYS -> "A"
}

private fun periodToFilterLabel(period: HeartRateHistoryPeriod): String = when (period) {
    HeartRateHistoryPeriod.TODAY -> "D"
    HeartRateHistoryPeriod.LAST_7_DAYS -> "S"
    HeartRateHistoryPeriod.LAST_30_DAYS -> "M"
    HeartRateHistoryPeriod.LAST_90_DAYS -> "3M"
    HeartRateHistoryPeriod.LAST_365_DAYS -> "A"
}

private fun periodToAccessibilityLabel(period: HeartRateHistoryPeriod): String = when (period) {
    HeartRateHistoryPeriod.TODAY -> "Dia de hoje"
    HeartRateHistoryPeriod.LAST_7_DAYS -> "Semana civil atual, de domingo a sábado"
    HeartRateHistoryPeriod.LAST_30_DAYS -> "Mês calendário atual até hoje"
    HeartRateHistoryPeriod.LAST_90_DAYS -> "Mês atual até hoje e dois meses calendário anteriores"
    HeartRateHistoryPeriod.LAST_365_DAYS -> "Mês atual até hoje e onze meses calendário anteriores"
}

private fun periodHeadline(period: HeartRateHistoryPeriod): String = when (period) {
    HeartRateHistoryPeriod.TODAY -> "Hoje"
    HeartRateHistoryPeriod.LAST_7_DAYS -> {
        val today = LocalDate.now(ZoneId.systemDefault())
        val sunday = today.minusDays((today.dayOfWeek.value % 7).toLong())
        formatHeartRateDateRange(sunday, sunday.plusDays(6))
    }
    HeartRateHistoryPeriod.LAST_30_DAYS -> {
        val month = YearMonth.now(ZoneId.systemDefault())
        "${formatHeartRateMonth(month)} até hoje"
    }
    HeartRateHistoryPeriod.LAST_90_DAYS -> {
        val currentMonth = YearMonth.now(ZoneId.systemDefault())
        formatHeartRateMonth(currentMonth.minusMonths(2)) + " - " + formatHeartRateMonth(currentMonth)
    }
    HeartRateHistoryPeriod.LAST_365_DAYS -> {
        val currentMonth = YearMonth.now(ZoneId.systemDefault())
        formatHeartRateMonth(currentMonth.minusMonths(11)) + " - " + formatHeartRateMonth(currentMonth)
    }
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

internal fun br.com.mwconsulting.championcompanion.core.model.DataOrigin.toHealthOriginLabel(): String = when (this) {
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.WATCH_DAILY_SUMMARY -> "Origem: relógio"
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.WATCH_D1_HISTORY -> "Origem: histórico do relógio"
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.WATCH_D3_D4 -> "Origem: relógio"
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.MANUAL_ENTRY -> "Origem: entrada manual"
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.APP_WORKOUT_SESSION -> "Origem: sessão do app"
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.DERIVED -> "Origem: derivado"
}

private fun numberFormat(): NumberFormat = NumberFormat.getIntegerInstance(Locale.getDefault())

@Composable
private fun heartRatePrimaryAccent() = if (isSystemInDarkTheme()) {
    DarkColorScheme.primary
} else {
    LightColorScheme.primary
}

@Composable
private fun weightPrimaryAccent() = if (isSystemInDarkTheme()) {
    DarkColorScheme.primary
} else {
    LightColorScheme.primary
}

private fun decimalFormat(): NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    maximumFractionDigits = 1
    minimumFractionDigits = 0
}
