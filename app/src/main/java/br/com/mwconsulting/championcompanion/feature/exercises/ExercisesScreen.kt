package br.com.mwconsulting.championcompanion.feature.exercises

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeLarge
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeMedium
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignHeaderNavigationAction
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignSurfaceHeader
import br.com.mwconsulting.championcompanion.core.designsystem.DarkColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.LightColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.LoadingState
import br.com.mwconsulting.championcompanion.core.designsystem.PendingReviewBanner
import br.com.mwconsulting.championcompanion.core.designsystem.PermissionBanner
import br.com.mwconsulting.championcompanion.core.designsystem.PrimaryActionButton
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionTimeSeriesChart
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
import br.com.mwconsulting.championcompanion.core.model.ActivityWorkflowState
import br.com.mwconsulting.championcompanion.core.model.CalendarDateRange
import br.com.mwconsulting.championcompanion.core.model.CanonicalWorkoutSeries
import br.com.mwconsulting.championcompanion.core.model.DerivedWorkoutInterval
import br.com.mwconsulting.championcompanion.core.model.DomainValue
import br.com.mwconsulting.championcompanion.core.model.ParserValidity
import br.com.mwconsulting.championcompanion.core.model.PhoneWorkoutPolicy
import br.com.mwconsulting.championcompanion.core.model.StepLengthSource
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibrationSnapshot
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportMetadata
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportProgress
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportResult
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportStatus
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityFilter
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityOrigin
import br.com.mwconsulting.championcompanion.core.model.WorkoutCanonicalReadStatus
import br.com.mwconsulting.championcompanion.core.model.WorkoutDistanceMetrics
import br.com.mwconsulting.championcompanion.core.model.WorkoutHeartRateSample
import br.com.mwconsulting.championcompanion.core.model.WorkoutInterval
import br.com.mwconsulting.championcompanion.core.model.WorkoutPlausibilityIndicator
import br.com.mwconsulting.championcompanion.core.model.WorkoutSplit
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.model.TemporalSeriesValidationReason
import br.com.mwconsulting.championcompanion.feature.health.GoogleHealthIntradayHeartRateChart
import java.time.Duration
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val ExerciseCadenceChartValueFormatter =
    ChampionChartFormatters.decimal(maximumFractionDigits = 1)

@Composable
fun ExercisesScreen(
    openedActivityId: WorkoutActivityId? = null,
    onNavigateBack: (() -> Unit)? = null,
    backLabel: String = "Voltar para Exercícios",
    onOpenActivity: (WorkoutActivityId) -> Unit = {},
    viewModel: ExercisesViewModel = run {
        val activity = checkNotNull(LocalActivity.current) as ComponentActivity
        remember(activity) {
            ViewModelProvider(activity)[ExercisesViewModel::class.java]
        }
    },
    onOpenDebugPoc: (() -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()
    val zoneId = remember { ZoneId.systemDefault() }
    val today = LocalDate.now(zoneId)
    val currentMonth = YearMonth.from(today)
    val currentWeekStart = today.with(DAY_START)
    val dismissOperationError = remember(viewModel) {
        { viewModel.onIntent(ExercisesIntent.DismissOperationError) }
    }
    val scrollState = rememberScrollState()
    var selectedHistoryPeriod by rememberSaveable {
        mutableStateOf(ExerciseHistoryPeriod.WEEK)
    }
    var selectedMonthIso by rememberSaveable { mutableStateOf(currentMonth.toString()) }
    var selectedWeekStartIso by rememberSaveable { mutableStateOf(currentWeekStart.toString()) }
    var selectedExerciseDateIso by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedMonth = remember(selectedMonthIso, currentMonth) {
        parseExerciseMonthOrDefault(selectedMonthIso, currentMonth).coerceAtMost(currentMonth)
    }
    val selectedWeekStart = remember(selectedWeekStartIso, currentWeekStart) {
        parseExerciseDateOrDefault(selectedWeekStartIso, currentWeekStart)
            .with(DAY_START)
            .coerceAtMost(currentWeekStart)
    }
    val selectedExerciseDate = remember(selectedExerciseDateIso, selectedMonth) {
        selectedExerciseDateIso
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.takeIf { YearMonth.from(it) == selectedMonth }
    }

    LaunchedEffect(viewModel) {
        viewModel.onIntent(ExercisesIntent.Load)
    }

    LaunchedEffect(selectedHistoryPeriod, state.selectedFilter) {
        if (selectedHistoryPeriod == ExerciseHistoryPeriod.MONTH &&
            state.selectedFilter != WorkoutActivityFilter.ALL
        ) {
            viewModel.onIntent(ExercisesIntent.SelectFilter(WorkoutActivityFilter.ALL))
        }
    }

    LaunchedEffect(viewModel, onNavigateBack) {
        viewModel.activityDeleted.collect {
            onNavigateBack?.invoke()
        }
    }

    LaunchedEffect(openedActivityId) {
        scrollState.scrollTo(0)
        if (openedActivityId == null) {
            viewModel.onIntent(ExercisesIntent.CloseActivity)
        } else if (state.selectedActivity?.id != openedActivityId) {
            viewModel.onIntent(ExercisesIntent.OpenActivity(openedActivityId))
        }
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onIntent(ExercisesIntent.AppMovedToBackground)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                .padding(ChampionSpacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
        ) {
            if (openedActivityId == null) {
                ExercisesHeader(state = state)
                onOpenDebugPoc?.let { openDebugPoc ->
                    ExercisesDebugPocCard(onOpenDebugPoc = openDebugPoc)
                }
                ExercisesImportCard(
                    state = state,
                    onStartImport = { viewModel.onIntent(ExercisesIntent.StartImport) },
                    onCancelImport = { viewModel.onIntent(ExercisesIntent.CancelImport) },
                    onRetryImport = { viewModel.onIntent(ExercisesIntent.RetryImport) },
                    onDismissImport = { viewModel.onIntent(ExercisesIntent.DismissImport) },
                )
                if (!state.importState.isImportInFlight()) {
                    ExerciseSyncFeedbackCard(state.activityList, state.workflowSummary)
                }
                ExerciseHistoryPeriodSelector(
                    selectedPeriod = selectedHistoryPeriod,
                    onSelectPeriod = { period ->
                        selectedHistoryPeriod = period
                        if (period == ExerciseHistoryPeriod.MONTH &&
                            state.selectedFilter != WorkoutActivityFilter.ALL
                        ) {
                            viewModel.onIntent(ExercisesIntent.SelectFilter(WorkoutActivityFilter.ALL))
                        }
                    },
                )
                ExerciseHistorySection(
                    period = selectedHistoryPeriod,
                    activities = state.activityList,
                    selectedFilter = state.selectedFilter,
                    workflowSummary = state.workflowSummary,
                    today = today,
                    zoneId = zoneId,
                    selectedMonth = selectedMonth,
                    selectedWeekStart = selectedWeekStart,
                     onSelectFilter = { viewModel.onIntent(ExercisesIntent.SelectFilter(it)) },
                     onOpenActivity = onOpenActivity,
                     selectedExerciseDate = selectedExerciseDate,
                     onSelectExerciseDate = { selectedExerciseDateIso = it.toString() },
                     onPreviousMonth = {
                         selectedMonthIso = selectedMonth.minusMonths(1).toString()
                         selectedExerciseDateIso = null
                     },
                     onNextMonth = {
                         if (selectedMonth < currentMonth) {
                             selectedMonthIso = selectedMonth.plusMonths(1).toString()
                             selectedExerciseDateIso = null
                         }
                     },
                    onPreviousWeek = { selectedWeekStartIso = selectedWeekStart.minusWeeks(1).toString() },
                     onNextWeek = {
                         if (selectedWeekStart < currentWeekStart) {
                             selectedWeekStartIso = selectedWeekStart.plusWeeks(1).toString()
                         }
                     },
                    onToday = {
                         if (selectedHistoryPeriod == ExerciseHistoryPeriod.MONTH) {
                             selectedMonthIso = currentMonth.toString()
                             selectedExerciseDateIso = null
                         } else {
                            selectedWeekStartIso = currentWeekStart.toString()
                        }
                    },
                )
            } else {
                ExerciseDetailShell(
                    state = state,
                    activityId = openedActivityId,
                    onNavigateBack = onNavigateBack,
                    backLabel = backLabel,
                    onDismissError = dismissOperationError,
                    onReviewTypeChange = { viewModel.onIntent(ExercisesIntent.EditReviewType(it)) },
                    onReviewDistanceChange = { viewModel.onIntent(ExercisesIntent.EditReviewDistance(it)) },
                    onReviewTitleChange = { viewModel.onIntent(ExercisesIntent.EditReviewTitle(it)) },
                    onReviewNotesChange = { viewModel.onIntent(ExercisesIntent.EditReviewNotes(it)) },
                    onReviewPlausibilityAcceptanceChange = {
                        viewModel.onIntent(ExercisesIntent.EditReviewPlausibilityAcceptance(it))
                    },
                    onSubmitReview = { viewModel.onIntent(ExercisesIntent.SubmitReview) },
                    onCalibrationDistanceChange = { viewModel.onIntent(ExercisesIntent.EditCalibrationDistance(it)) },
                    onSubmitCalibration = { viewModel.onIntent(ExercisesIntent.SubmitCalibration) },
                    referenceDistanceCalibrationAllowed = state.referenceDistanceCalibrationAllowed,
                    onReprocessActivity = { viewModel.onIntent(ExercisesIntent.ReprocessSelectedActivity) },
                    onAcceptPlausibility = { viewModel.onIntent(ExercisesIntent.AcceptSelectedPlausibility) },
                    onSyncActivity = { viewModel.onIntent(ExercisesIntent.SyncSelectedActivity) },
                    onDiscardActivity = { viewModel.onIntent(ExercisesIntent.DiscardSelectedActivity) },
                    onDeleteActivity = { viewModel.onIntent(ExercisesIntent.DeleteSelectedActivity) },
                )
            }
        }
    }
}

@Composable
private fun ExercisesHeader(
    state: ExercisesUiState,
) {
    ChampionRedesignSurfaceHeader(
        title = "Exercícios",
        supportingText = if (state.hasAssociatedDevice) {
            "Relógio: ${state.associatedDeviceName ?: "Champion C001"}"
        } else {
            "Relógio: Pendente"
        },
    )
}

@Composable
private fun ExercisesDebugPocCard(
    onOpenDebugPoc: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        border = BorderStroke(1.dp, exerciseTertiaryAccent()),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text(
                text = "PoC técnica de debug",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Acesso técnico para testar GPS + MapLibre. Não inicia treino nem salva dados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onOpenDebugPoc,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Abrir PoC GPS + MapLibre (debug)")
            }
        }
    }
}

@Composable
private fun ExercisesImportCard(
    state: ExercisesUiState,
    onStartImport: () -> Unit,
    onCancelImport: () -> Unit,
    onRetryImport: () -> Unit,
    onDismissImport: () -> Unit,
) {
    val importSession = state.importState.sessionOrNull()
    val isImporting = state.importState.isImportInFlight()
    val metadata = importSession?.metadata ?: state.latestImportMetadata
    val progressMessage = importSession?.progress?.toFriendlyMessage(state.importState)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        border = BorderStroke(1.dp, exercisePrimaryAccent()),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text("Importar do relógio", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "A leitura começa somente por ação explícita. Atividades importadas ficam disponíveis para revisão local.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = PhoneWorkoutPolicy.DISCONTINUED_MESSAGE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.workflowSummary.pendingReviewCount > 0) {
                PendingReviewBanner(
                    message = "Há ${state.workflowSummary.pendingReviewCount} treino(s) aguardando revisão na lista abaixo.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (!state.hasAssociatedDevice) {
                ErrorState(
                    title = "Relógio necessário",
                    message = "Associe um relógio Champion para liberar a importação manual de treinos.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PrimaryActionButton(
                label = if (isImporting) "Importando treinos do relógio" else "Buscar treinos no relógio",
                onClick = onStartImport,
                enabled = state.hasAssociatedDevice,
                isLoading = isImporting,
                modifier = Modifier.fillMaxWidth(),
            )

            if (isImporting) {
                AssistChip(
                    onClick = onCancelImport,
                    label = { Text("Cancelar importação") },
                )
            }

            if (isImporting && progressMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = progressMessage
                            liveRegion = LiveRegionMode.Polite
                        },
                    verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                ) {
                    LinearProgressIndicator(
                        progress = { importSession.progress.overallProgressFraction ?: 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = exercisePrimaryAccent(),
                        trackColor = exercisePrimaryAccent().copy(alpha = 0.18f),
                    )
                    Text(progressMessage, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = importSession.progress.toTechnicalLine(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when (state.importState) {
                WatchWorkoutImportSurfaceState.Idle,
                is WatchWorkoutImportSurfaceState.Preparing,
                is WatchWorkoutImportSurfaceState.Connecting,
                is WatchWorkoutImportSurfaceState.ReadingIndex,
                is WatchWorkoutImportSurfaceState.ReadingBlocks,
                is WatchWorkoutImportSurfaceState.Parsing,
                is WatchWorkoutImportSurfaceState.Persisting,
                -> Unit

                is WatchWorkoutImportSurfaceState.Completed,
                is WatchWorkoutImportSurfaceState.Partial,
                is WatchWorkoutImportSurfaceState.Empty,
                is WatchWorkoutImportSurfaceState.Cancelled,
                -> ImportResultBanner(
                    session = importSession,
                    onPrimaryAction = onRetryImport,
                    primaryActionLabel = if (state.importState is WatchWorkoutImportSurfaceState.Empty) {
                        "Buscar novamente"
                    } else {
                        "Importar de novo"
                    },
                    onSecondaryAction = onDismissImport,
                )

                is WatchWorkoutImportSurfaceState.Failed -> ImportErrorBanner(
                    session = importSession,
                    onRetry = onRetryImport,
                    onDismiss = onDismissImport,
                )
            }

            MetadataSection(metadata)
        }
    }
}

@Composable
private fun ImportResultBanner(
    session: WatchWorkoutImportSessionState?,
    onPrimaryAction: () -> Unit,
    primaryActionLabel: String,
    onSecondaryAction: () -> Unit,
) {
    val result = session?.result ?: return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = result.toAccessibleTitle()
                liveRegion = LiveRegionMode.Polite
            },
        shape = ChampionShapeMedium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(result.toTitle(), style = MaterialTheme.typography.titleMedium)
            Text(result.toMessage(), style = MaterialTheme.typography.bodyMedium)
            result.summary?.let { summary ->
                Text(summary.toSummaryLine(), style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small)) {
                PrimaryActionButton(label = primaryActionLabel, onClick = onPrimaryAction)
                AssistChip(onClick = onSecondaryAction, label = { Text("Fechar") })
            }
        }
    }
}

@Composable
private fun ImportErrorBanner(
    session: WatchWorkoutImportSessionState?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val error = session?.error ?: return
    ErrorState(
        title = "Não foi possível importar os treinos",
        message = error.message,
        actionLabel = if (error.isRetryable) "Tentar novamente" else null,
        onAction = if (error.isRetryable) onRetry else null,
        modifier = Modifier.fillMaxWidth(),
    )
    AssistChip(onClick = onDismiss, label = { Text("Dispensar") })
}

@Composable
private fun MetadataSection(metadata: WatchWorkoutImportMetadata?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeMedium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Última operação", style = MaterialTheme.typography.titleMedium)
            if (metadata == null) {
                Text(
                    text = "Ainda não há importação registrada para este relógio.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                ) {
                    AssistChip(onClick = {}, enabled = false, label = { Text(metadata.lastResult.toLabel()) })
                    AssistChip(onClick = {}, enabled = false, label = { Text(metadata.timedOut.toTimeoutLabel()) })
                }
                Text("Última tentativa: ${metadata.lastAttemptAt.formatDateTime()}", style = MaterialTheme.typography.bodyMedium)
                metadata.lastSuccessAt?.let {
                    Text("Último sucesso: ${it.formatDateTime()}", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = "Inseridos ${metadata.insertedCount}, duplicados ${metadata.duplicateCount}, inválidos ${metadata.invalidCount}, ausentes ${metadata.missingIndexCount}.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ExercisesPendingSection(pendingActivities: List<WorkoutActivity>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        Text("Pendentes de revisão", style = MaterialTheme.typography.titleLarge)
        if (pendingActivities.isEmpty()) {
            EmptyState(
                title = "Nenhum treino pendente",
                message = "Quando um treino for importado do relógio, ele aparecerá aqui como somente leitura até a revisão futura.",
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            pendingActivities.forEach { activity ->
                PendingActivityCard(activity)
            }
        }
    }
}

@Composable
private fun PendingActivityCard(activity: WorkoutActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeMedium,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = activity.metrics.startedAt.formatDateTime(),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Duração ${activity.metrics.duration.toDurationLabel()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
            ) {
                AssistChip(onClick = {}, enabled = false, label = { Text(activity.suggestedType.toLabel()) })
                AssistChip(onClick = {}, enabled = false, label = { Text("Pendente") })
            }
        }
    }
}

@Composable
private fun WorkoutFiltersSection(
    selectedFilter: WorkoutActivityFilter,
    workflowSummary: br.com.mwconsulting.championcompanion.core.model.WorkoutWorkflowSummary,
    onSelectFilter: (WorkoutActivityFilter) -> Unit,
) {
    var isDialogOpen by remember { mutableStateOf(false) }
    val filterOptions = workoutFilterOptions(workflowSummary)
    val selectedOption = filterOptions.first { it.filter == selectedFilter }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
    ) {
        Text("Filtro da lista", style = MaterialTheme.typography.titleLarge)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ChampionShapeMedium,
            border = BorderStroke(1.dp, exerciseTertiaryAccent()),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ChampionSpacing.large),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall)) {
                    Text(selectedOption.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${selectedOption.count} treino(s) neste filtro",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = { isDialogOpen = true }) {
                    Text("Trocar")
                }
            }
        }
    }

    if (isDialogOpen) {
        AlertDialog(
            onDismissRequest = { isDialogOpen = false },
            title = { Text("Selecionar filtro") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small)) {
                    filterOptions.forEach { option ->
                        Card(
                            onClick = {
                                onSelectFilter(option.filter)
                                isDialogOpen = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ChampionShapeMedium,
                            colors = CardDefaults.cardColors(
                                containerColor = if (option.filter == selectedFilter) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLowest
                                },
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(ChampionSpacing.medium),
                                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
                            ) {
                                Text(option.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${option.count} treino(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isDialogOpen = false }) {
                    Text("Fechar")
                }
            },
        )
    }
}

private data class WorkoutFilterOption(
    val filter: WorkoutActivityFilter,
    val label: String,
    val count: Int,
)

private fun workoutFilterOptions(
    workflowSummary: br.com.mwconsulting.championcompanion.core.model.WorkoutWorkflowSummary,
): List<WorkoutFilterOption> = listOf(
    WorkoutFilterOption(
        filter = WorkoutActivityFilter.ALL,
        label = "Todas",
        count = workflowSummary.pendingReviewCount +
            workflowSummary.pendingCalibrationCount +
            workflowSummary.readyToSyncCount +
            workflowSummary.syncedCount +
            workflowSummary.syncFailedCount +
            workflowSummary.discardedCount,
    ),
    WorkoutFilterOption(WorkoutActivityFilter.PENDING_REVIEW, "Aguardando revisão", workflowSummary.pendingReviewCount),
    WorkoutFilterOption(WorkoutActivityFilter.PENDING_CALIBRATION, "Aguardando calibração", workflowSummary.pendingCalibrationCount),
    WorkoutFilterOption(WorkoutActivityFilter.READY_TO_SYNC, "Prontas", workflowSummary.readyToSyncCount),
    WorkoutFilterOption(WorkoutActivityFilter.SYNCED, "Sincronizadas", workflowSummary.syncedCount),
    WorkoutFilterOption(WorkoutActivityFilter.SYNC_FAILED, "Com erro", workflowSummary.syncFailedCount),
    WorkoutFilterOption(WorkoutActivityFilter.DISCARDED, "Descartadas", workflowSummary.discardedCount),
)

private enum class ExerciseHistoryPeriod {
    WEEK,
    MONTH,
}

@Composable
private fun ExerciseHistoryPeriodSelector(
    selectedPeriod: ExerciseHistoryPeriod,
    onSelectPeriod: (ExerciseHistoryPeriod) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeMedium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Período dos exercícios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Alterne entre Semana e Mês; use as setas para navegar no período selecionado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
            ) {
                FilterChip(
                    selected = selectedPeriod == ExerciseHistoryPeriod.WEEK,
                    onClick = { onSelectPeriod(ExerciseHistoryPeriod.WEEK) },
                    label = { Text("Semana") },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                )
                FilterChip(
                    selected = selectedPeriod == ExerciseHistoryPeriod.MONTH,
                    onClick = { onSelectPeriod(ExerciseHistoryPeriod.MONTH) },
                    label = { Text("Mês") },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                )
            }
        }
    }
}

@Composable
private fun ExerciseHistorySection(
    period: ExerciseHistoryPeriod,
    activities: List<WorkoutActivity>,
    selectedFilter: WorkoutActivityFilter,
    workflowSummary: br.com.mwconsulting.championcompanion.core.model.WorkoutWorkflowSummary,
    today: LocalDate,
    zoneId: ZoneId,
    selectedMonth: YearMonth,
    selectedWeekStart: LocalDate,
    onSelectFilter: (WorkoutActivityFilter) -> Unit,
    onOpenActivity: (WorkoutActivityId) -> Unit,
    selectedExerciseDate: LocalDate?,
    onSelectExerciseDate: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onToday: () -> Unit,
) {
    val weekStart = selectedWeekStart.with(DAY_START)
    val range = when (period) {
        ExerciseHistoryPeriod.WEEK -> CalendarDateRange(weekStart, weekStart.plusDays(7))
        ExerciseHistoryPeriod.MONTH -> CalendarDateRange(
            selectedMonth.atDay(1),
            selectedMonth.plusMonths(1).atDay(1),
        )
    }
    val periodActivities = activities
        .filter { it.metrics.startedAt.isIn(range.atZone(zoneId)) }
        .filter { period == ExerciseHistoryPeriod.WEEK || it.isEligibleForExerciseCalendar() }
        .sortedByDescending { it.metrics.startedAt }
    val activitiesByDate = periodActivities.groupBy { it.metrics.startedAt.atZone(zoneId).toLocalDate() }
    val activityListKey = periodActivities.map { it.id.value.toString() }.hashCode()
    val temporalSelectionKey = if (period == ExerciseHistoryPeriod.WEEK) {
        weekStart.toString()
    } else {
        selectedMonth.toString()
    }
    val pageSize = 5
    var selectedPage by rememberSaveable(
        period,
        selectedFilter,
        temporalSelectionKey,
        activityListKey,
    ) { mutableStateOf(0) }
    val pageCount = (periodActivities.size + pageSize - 1) / pageSize
    val currentPage = selectedPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    val visibleActivities = periodActivities.drop(currentPage * pageSize).take(pageSize)
    val activityDays = activitiesByDate.keys.size

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        ExerciseHistoryPeriodNavigation(
            period = period,
            month = selectedMonth,
            weekStart = weekStart,
            currentMonth = YearMonth.from(today),
            currentWeekStart = today.with(DAY_START),
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onPreviousWeek = onPreviousWeek,
            onNextWeek = onNextWeek,
            onToday = onToday,
        )
        Text(
            text = if (period == ExerciseHistoryPeriod.WEEK) "Resumo da semana" else "Resumo do mês",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (period == ExerciseHistoryPeriod.WEEK) {
                "$activityDays de 7 dias com exercício"
            } else {
                "$activityDays de ${selectedMonth.lengthOfMonth()} dias com exercício"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (period == ExerciseHistoryPeriod.WEEK) {
                "Semana iniciada no domingo e terminada no sábado: " +
                    "${range.fromInclusive.formatExerciseDate()} a " +
                    "${range.untilExclusive.minusDays(1).formatExerciseDate()}."
            } else {
                "Você se exercitou um total de ${periodActivities.size} " +
                    "${if (periodActivities.size == 1) "vez" else "vezes"} em " +
                    "$activityDays ${if (activityDays == 1) "dia" else "dias"} com exercícios"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (period == ExerciseHistoryPeriod.WEEK) {
            Text(
                text = "Dias com exercícios",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            ExerciseHistoryChart(
                range = range,
                activitiesByDate = activitiesByDate,
            )
            WorkoutFiltersSection(
                selectedFilter = selectedFilter,
                workflowSummary = workflowSummary,
                onSelectFilter = onSelectFilter,
            )
            ExerciseWeeklyActivityList(
                activities = visibleActivities,
                today = today,
                zoneId = zoneId,
                 pageCount = pageCount,
                 currentPage = currentPage,
                 onOpenActivity = onOpenActivity,
                onPreviousPage = { selectedPage = (currentPage - 1).coerceAtLeast(0) },
                onNextPage = { selectedPage = (currentPage + 1).coerceAtMost(pageCount - 1) },
            )
        } else {
            ExerciseMonthCalendar(
                month = selectedMonth,
                today = today,
                activitiesByDate = activitiesByDate,
                selectedDate = selectedExerciseDate,
                onSelectDate = onSelectExerciseDate,
            )
            ExerciseMonthMetricRow(
                activityDays = activityDays,
                totalDuration = periodActivities.totalDurationOrNull(),
                totalDistanceMeters = periodActivities.totalDisplayDistanceMetersOrNull(),
            )
            ExerciseMonthActivityList(
                month = selectedMonth,
                today = today,
                activities = periodActivities,
            )
        }
    }
}

@Composable
private fun ExerciseWeeklyActivityList(
    activities: List<WorkoutActivity>,
    today: LocalDate,
    zoneId: ZoneId,
    pageCount: Int,
    currentPage: Int,
    onOpenActivity: (WorkoutActivityId) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        Text(
            text = "Atividades feitas",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (activities.isEmpty()) {
            EmptyState(
                title = "Nenhum exercício neste período",
                message = "Atividades reais gravadas no período aparecerão aqui.",
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            activities
                .groupBy { it.metrics.startedAt.atZone(zoneId).toLocalDate() }
                .toSortedMap(compareByDescending { it })
                .forEach { (date, dayActivities) ->
                    Text(
                        text = formatExerciseDayLabel(date, today),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    dayActivities.forEach { activity ->
                        WorkoutActivityCard(
                            activity = activity,
                            onClick = { onOpenActivity(activity.id) },
                        )
                    }
                }
        }
        if (pageCount > 1) {
            ExerciseHistoryPagination(
                currentPage = currentPage,
                pageCount = pageCount,
                onPrevious = onPreviousPage,
                onNext = onNextPage,
            )
        }
    }
}

@Composable
private fun ExerciseHistoryPeriodNavigation(
    period: ExerciseHistoryPeriod,
    month: YearMonth,
    weekStart: LocalDate,
    currentMonth: YearMonth,
    currentWeekStart: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onToday: () -> Unit,
) {
    val isWeek = period == ExerciseHistoryPeriod.WEEK
    val periodLabel = if (isWeek) {
        formatExerciseWeekRange(weekStart, weekStart.plusDays(6))
    } else {
        formatExerciseMonth(month)
    }
    val currentPeriod = if (isWeek) currentWeekStart == weekStart else currentMonth == month
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Período de exercícios selecionado: $periodLabel"
            },
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = periodLabel,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = if (isWeek) onPreviousWeek else onPreviousMonth,
                    modifier = Modifier.semantics {
                        contentDescription = if (isWeek) "Semana anterior" else "Mês anterior"
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = null,
                    )
                }
                Button(
                    onClick = onToday,
                    enabled = !currentPeriod,
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = exercisePrimaryAccent(),
                        contentColor = if (isSystemInDarkTheme()) DarkColorScheme.onPrimary else LightColorScheme.onPrimary,
                    ),
                ) {
                    Text("Hoje")
                }
                IconButton(
                    onClick = if (isWeek) onNextWeek else onNextMonth,
                    enabled = if (isWeek) {
                        weekStart < currentWeekStart
                    } else {
                        month < currentMonth
                    },
                    modifier = Modifier.semantics {
                        contentDescription = if (isWeek) "Próxima semana" else "Próximo mês"
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseMonthCalendar(
    month: YearMonth,
    today: LocalDate,
    activitiesByDate: Map<LocalDate, List<WorkoutActivity>>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
) {
    val leadingEmptyCells = month.atDay(1).dayOfWeek.value % 7
    val cellCount = ((leadingEmptyCells + month.lengthOfMonth() + 6) / 7) * 7
    val dates = (0 until cellCount).map { index ->
        val dayOffset = index - leadingEmptyCells
        month.atDay(1).plusDays(dayOffset.toLong()).takeIf { YearMonth.from(it) == month }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Calendário mensal somente de exercícios" },
        shape = ChampionShapeLarge,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = ChampionSpacing.small,
                vertical = ChampionSpacing.small,
            ),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
            ) {
                EXERCISE_WEEKDAY_LABELS.forEachIndexed { index, label ->
                    Text(
                        text = label,
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = EXERCISE_WEEKDAY_FULL_LABELS[index]
                            },
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            dates.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
                ) {
                    week.forEach { date ->
                        if (date == null) {
                            EmptyExerciseCalendarCell()
                        } else {
                            ExerciseCalendarDayCell(
                                date = date,
                                activities = activitiesByDate[date].orEmpty(),
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                onClick = { onSelectDate(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.EmptyExerciseCalendarCell() {
    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 56.dp),
    )
}

@Composable
private fun RowScope.ExerciseCalendarDayCell(
    date: LocalDate,
    activities: List<WorkoutActivity>,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val statusColors = activities
        .map { it.workflowState }
        .distinct()
        .take(3)
    val count = activities.size
    val dayDescription = buildString {
        append(date.formatExerciseCalendarDate())
        if (isToday) append(", hoje")
        if (isSelected) append(", selecionado")
        if (count == 0) {
            append(", sem exercício")
        } else {
            append(", $count ${if (count == 1) "atividade" else "atividades"}")
            if (statusColors.isNotEmpty()) {
                append(", status: ${statusColors.joinToString { it.toLabel() }}")
            }
        }
    }
    val borderColor = when {
        isSelected -> exercisePrimaryAccent()
        isToday -> MaterialTheme.colorScheme.onSurface
        else -> Color.Transparent
    }
    Surface(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 56.dp)
            .border(
                width = when {
                    isSelected -> 3.dp
                    isToday -> 2.dp
                    else -> 0.dp
                },
                color = borderColor,
                shape = ChampionShapeMedium,
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = dayDescription
                selected = isSelected
                stateDescription = if (isSelected) "Selecionado" else "Não selecionado"
            },
        shape = ChampionShapeMedium,
        color = if (activities.isEmpty()) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ChampionSpacing.extraSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                statusColors.forEach { status ->
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(status.toExerciseCalendarColor(), CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseMonthMetricRow(
    activityDays: Int,
    totalDuration: Duration?,
    totalDistanceMeters: Double?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
    ) {
        ExerciseMonthMetricCard(
            label = "Dias com exercícios",
            value = activityDays.toString(),
            modifier = Modifier.weight(1f),
        )
        ExerciseMonthMetricCard(
            label = "Duração",
            value = totalDuration?.toDurationLabel() ?: "indisponível",
            modifier = Modifier.weight(1f),
        )
        ExerciseMonthMetricCard(
            label = "Distância",
            value = totalDistanceMeters.toDistanceLabel(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ExerciseMonthMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .heightIn(min = 72.dp)
            .semantics { contentDescription = "$label: $value" },
        shape = ChampionShapeMedium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.small),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ExerciseMonthActivityList(
    month: YearMonth,
    today: LocalDate,
    activities: List<WorkoutActivity>,
) {
    val weekGroups = exerciseMonthWeekGroups(month, ZoneId.systemDefault(), activities)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatExerciseMonth(month),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Total por semana",
                color = exercisePrimaryAccent(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (weekGroups.isEmpty()) {
            EmptyState(
                title = "Nenhum exercício neste mês",
                message = "Atividades reais gravadas no mês aparecerão aqui.",
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            weekGroups.forEach { group ->
                val exerciseTypeCount = group.activities.map { it.displayType() }.distinct().size
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            contentDescription = "${group.label(today)}. ${group.activities.size} " +
                                "${if (group.activities.size == 1) "atividade" else "atividades"}. " +
                                "$exerciseTypeCount ${if (exerciseTypeCount == 1) "tipo" else "tipos"} de exercício."
                        },
                    shape = ChampionShapeMedium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(ChampionSpacing.medium),
                        horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = group.label(today),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
                        ) {
                            Text(
                                text = group.activities.size.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "$exerciseTypeCount ${if (exerciseTypeCount == 1) "tipo" else "tipos"} de exercício",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ExerciseMonthWeekGroup(
    val weekStart: LocalDate,
    val activities: List<WorkoutActivity>,
) {
    fun label(today: LocalDate): String = if (weekStart == today.with(DAY_START)) {
        "Esta semana"
    } else {
        formatExerciseWeekRange(weekStart, weekStart.plusDays(6))
    }
}

private fun exerciseMonthWeekGroups(
    month: YearMonth,
    zoneId: ZoneId,
    activities: List<WorkoutActivity>,
): List<ExerciseMonthWeekGroup> {
    val firstDay = month.atDay(1)
    val lastWeekStart = month.atEndOfMonth().with(DAY_START)
    return activities
        .groupBy { it.metrics.startedAt.atZone(zoneId).toLocalDate().with(DAY_START) }
        .filterKeys { weekStart ->
            !weekStart.plusDays(6).isBefore(firstDay) && !weekStart.isAfter(lastWeekStart)
        }
        .map { (weekStart, weekActivities) ->
            ExerciseMonthWeekGroup(
                weekStart = weekStart,
                activities = weekActivities.sortedByDescending { it.metrics.startedAt },
            )
        }
        .sortedByDescending { it.weekStart }
}

private fun formatExerciseWeekRange(from: LocalDate, until: LocalDate): String =
    "${from.dayOfMonth} de ${from.exerciseMonthAbbreviation()} – " +
        "${until.dayOfMonth} de ${until.exerciseMonthAbbreviation()}"

private fun formatExerciseMonth(month: YearMonth): String =
    month.format(
        DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("pt-BR")),
    ).replaceFirstChar { it.uppercase(Locale.forLanguageTag("pt-BR")) }

private fun parseExerciseMonthOrDefault(value: String, default: YearMonth): YearMonth =
    runCatching { YearMonth.parse(value) }.getOrDefault(default)

private fun parseExerciseDateOrDefault(value: String, default: LocalDate): LocalDate =
    runCatching { LocalDate.parse(value) }.getOrDefault(default)

private fun LocalDate.formatExerciseCalendarDate(): String =
    format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.forLanguageTag("pt-BR")))

private fun LocalDate.exerciseMonthAbbreviation(): String = EXERCISE_MONTH_ABBREVIATIONS[monthValue - 1]

private val EXERCISE_MONTH_ABBREVIATIONS = listOf(
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

private fun List<WorkoutActivity>.totalDurationOrNull(): Duration? {
    if (isEmpty()) return null
    var total = Duration.ZERO
    for (activity in this) {
        val duration = activity.metrics.duration
        if (duration.isNegative) return null
        total = runCatching { total.plus(duration) }.getOrNull() ?: return null
    }
    return total
}

private fun List<WorkoutActivity>.totalDisplayDistanceMetersOrNull(): Double? {
    if (isEmpty()) return null
    var total = 0.0
    for (activity in this) {
        val distance = activity.displayDistanceMetersOrNull() ?: return null
        if (!distance.isFinite() || distance < 0.0) return null
        total += distance
        if (!total.isFinite()) return null
    }
    return total
}

@Composable
private fun ExerciseHistoryPagination(
    currentPage: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrevious, enabled = currentPage > 0) {
            Text("Anterior")
        }
        Text(
            text = "Página ${currentPage + 1} de $pageCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onNext, enabled = currentPage < pageCount - 1) {
            Text("Próxima")
        }
    }
}

@Composable
private fun ExerciseHistoryChart(
    range: CalendarDateRange,
    activitiesByDate: Map<LocalDate, List<WorkoutActivity>>,
) {
    val dates = range.dates
    val counts = dates.map { date -> activitiesByDate[date]?.size?.takeIf { it > 0 } }
    val numberFormatter = ChampionChartFormatters.integer()
    val maximum = maxOf(counts.filterNotNull().maxOrNull()?.toDouble() ?: 0.0, 1.0)
    val chartData = ChampionCartesianChartData(
        series = listOf(
            ChampionChartSeries(
                key = "exercises",
                label = "Exercícios",
                points = dates.mapIndexed { index, date ->
                    val count = counts[index]
                    val label = EXERCISE_WEEKDAY_LABELS[index]
                    ChampionChartPoint(
                        x = index.toDouble(),
                        y = count?.toDouble(),
                        xLabel = label,
                        contentDescription = "$label: ${count?.let { "$it ${if (it == 1) "atividade" else "atividades"}" } ?: "sem exercício"}.",
                    )
                },
            ),
        ),
        xAxis = ChampionChartXAxisSpec(
            formatter = ChampionChartXAxisFormatter { value ->
                val index = value.roundToInt()
                if (index in dates.indices) EXERCISE_WEEKDAY_LABELS[index] else "--"
            },
            title = "Dia",
            minimum = -0.5,
            maximum = dates.size - 0.5,
            visible = true,
            gridLinesVisible = false,
            labelValues = dates.indices.map(Int::toDouble),
        ),
        yAxis = ChampionChartAxisSpec(
            title = "Exercícios",
            formatter = numberFormatter,
            unit = ChampionChartUnit("atividades"),
            minimum = 0.0,
            maximum = maximum,
            visible = false,
            gridLinesVisible = false,
        ),
        interaction = ChampionChartInteractionPolicy(scrollEnabled = false),
        accessibilitySummary = "Gráfico de barras de exercícios da semana civil, de domingo a sábado. " +
            "${counts.count { it != null }} dias com exercício e ${counts.count { it == null }} sem exercício.",
    )
    ChampionBarChart(
        data = chartData,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        columnWidth = 16.dp,
        columnCollectionSpacing = 2.dp,
        groupedColumnSpacing = 1.dp,
    )
}

private fun Instant.isIn(range: br.com.mwconsulting.championcompanion.core.model.CalendarInstantRange): Boolean =
    !isBefore(range.fromInclusive) && isBefore(range.untilExclusive)

private fun LocalDate.formatExerciseDate(): String =
    DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-BR")).format(this)

private fun formatExerciseDayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Hoje"
    today.minusDays(1) -> "Ontem"
    else -> DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")).format(date)
}

private val DAY_START = TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)
private val EXERCISE_WEEKDAY_LABELS = listOf("D", "S", "T", "Q", "Q", "S", "S")
private val EXERCISE_WEEKDAY_FULL_LABELS = listOf(
    "Domingo",
    "Segunda-feira",
    "Terça-feira",
    "Quarta-feira",
    "Quinta-feira",
    "Sexta-feira",
    "Sábado",
)

@Composable
private fun ExerciseSyncFeedbackCard(
    activities: List<WorkoutActivity>,
    workflowSummary: br.com.mwconsulting.championcompanion.core.model.WorkoutWorkflowSummary,
) {
    val syncingCount = activities.count { it.workflowState == ActivityWorkflowState.SYNCING }
    if (workflowSummary.readyToSyncCount <= 0 && syncingCount <= 0 && workflowSummary.syncedCount <= 0 && workflowSummary.syncFailedCount <= 0) {
        return
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, exerciseSecondaryAccent()),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                "Health Connect · feedback secundário",
                style = MaterialTheme.typography.titleMedium,
                color = exerciseSecondaryAccent(),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                when {
                    syncingCount > 0 -> "Há treino(s) sincronizando com o Health Connect. Os dados locais continuam disponíveis normalmente."
                    workflowSummary.syncFailedCount > 0 -> "Alguns treinos ficaram com erro ou falha parcial no Health Connect. Os detalhes mostram retry seguro por atividade."
                    workflowSummary.readyToSyncCount > 0 -> "Existem treinos prontos para sincronização explícita com o Health Connect."
                    workflowSummary.syncedCount > 0 -> "Escritas locais aceitas permanecem visíveis; o provider não é lido para confirmar records."
                    else -> "Sem feedback secundário de sincronização no momento."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Prontas ${workflowSummary.readyToSyncCount} • Sincronizando $syncingCount • Sincronizadas ${workflowSummary.syncedCount} • Com erro ${workflowSummary.syncFailedCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WorkoutActivityCard(
    activity: WorkoutActivity,
    onClick: () -> Unit,
) {
    val accentColor = activity.workflowState.toListAccentColor()
    val dateLabel = activity.metrics.startedAt.formatTime()
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .semantics {
                contentDescription = "${activity.displayListTitle()}. $dateLabel. " +
                    "${activity.metrics.duration.toDurationLabel()}. ${activity.displayDistanceLabel()}. " +
                    "${activity.workflowState.toLabel()}. Toque para abrir detalhes."
            },
        shape = ChampionShapeMedium,
        border = BorderStroke(1.dp, accentColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .padding(ChampionSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = accentColor.copy(alpha = 0.14f),
                        shape = ChampionShapeMedium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = activity.displayType().toListIcon(),
                    contentDescription = activity.displayType().toLabel(),
                    tint = accentColor,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = activity.displayListTitle(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = activity.displayDistanceLabel(),
                        modifier = Modifier.padding(start = ChampionSpacing.small),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "$dateLabel • ${activity.metrics.duration.toDurationLabel()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (activity.isWatchWorkout && !activity.isCycling &&
                    activity.canonicalReadStatus != WorkoutCanonicalReadStatus.AVAILABLE &&
                    activity.canonicalReadStatus != WorkoutCanonicalReadStatus.SPLITS_NOT_MATERIALIZED
                ) {
                    Text(
                        text = activity.canonicalReadStatus.toShortLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (activity.isWatchWorkout && !activity.isCycling && activity.requiresWatchWorkoutReprocessing) {
                    Text(
                        "Reprocessamento explícito necessário",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = activity.workflowState.toLabel(),
                    modifier = Modifier.semantics { stateDescription = activity.workflowState.toLabel() },
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ExerciseDetailShell(
    state: ExercisesUiState,
    activityId: WorkoutActivityId,
    onNavigateBack: (() -> Unit)?,
    backLabel: String = "Voltar para Exercícios",
    onDismissError: () -> Unit,
    onReviewTypeChange: (WorkoutType) -> Unit,
    onReviewDistanceChange: (String) -> Unit,
    onReviewTitleChange: (String) -> Unit,
    onReviewNotesChange: (String) -> Unit,
    onReviewPlausibilityAcceptanceChange: (Boolean) -> Unit,
    onSubmitReview: () -> Unit,
    onCalibrationDistanceChange: (String) -> Unit,
    onSubmitCalibration: () -> Unit,
    referenceDistanceCalibrationAllowed: Boolean,
    onReprocessActivity: () -> Unit,
    onAcceptPlausibility: () -> Unit,
    onSyncActivity: () -> Unit,
    onDiscardActivity: () -> Unit,
    onDeleteActivity: () -> Unit,
) {
    var showDiscardConfirmation by remember(activityId) { mutableStateOf(false) }
    var showDeleteConfirmation by remember(activityId) { mutableStateOf(false) }
    var showReprocessConfirmation by remember(activityId) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        when (val detailState = state.detailState) {
            ExerciseDetailState.Loading -> {
                ActivityDetailHeader(
                    activity = null,
                    onNavigateBack = onNavigateBack,
                    backLabel = backLabel,
                )
                Text("Carregando atividade $activityId", style = MaterialTheme.typography.bodyMedium)
            }
            ExerciseDetailState.NotFound -> {
                ActivityDetailHeader(
                    activity = null,
                    onNavigateBack = onNavigateBack,
                    backLabel = backLabel,
                )
                ErrorState(
                    title = "Treino não encontrado",
                    message = "Nenhuma atividade importada foi encontrada para este identificador.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is ExerciseDetailState.Failed -> {
                ActivityDetailHeader(
                    activity = null,
                    onNavigateBack = onNavigateBack,
                    backLabel = backLabel,
                )
                ErrorState(
                    title = "Não foi possível abrir o treino",
                    message = detailState.error.message,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is ExerciseDetailState.Ready -> {
                val activity = state.selectedActivity
                if (activity == null || activity.id != detailState.activityId) {
                    Text("Carregando treino selecionado", style = MaterialTheme.typography.bodyMedium)
                } else {
                    WorkoutActivityDetailContent(
                        activity = activity,
                        latestCalibration = detailState.latestCalibration,
                        reviewFormState = state.reviewFormState,
                        calibrationFormState = state.calibrationFormState,
                        referenceDistanceCalibrationAllowed = state.referenceDistanceCalibrationAllowed,
                        operationError = state.operationError,
                        isReprocessing = state.isReprocessing,
                        canDiscardSelectedActivity = state.canDiscardSelectedActivity,
                        onDismissError = onDismissError,
                        onReviewTypeChange = onReviewTypeChange,
                        onReviewDistanceChange = onReviewDistanceChange,
                        onReviewTitleChange = onReviewTitleChange,
                        onReviewNotesChange = onReviewNotesChange,
                        onReviewPlausibilityAcceptanceChange = onReviewPlausibilityAcceptanceChange,
                        onSubmitReview = onSubmitReview,
                        onCalibrationDistanceChange = onCalibrationDistanceChange,
                        onSubmitCalibration = onSubmitCalibration,
                        onRequestReprocess = { showReprocessConfirmation = true },
                        isAcceptingPlausibility = state.isAcceptingPlausibility,
                        onAcceptPlausibility = onAcceptPlausibility,
                        onSyncActivity = onSyncActivity,
                        onRequestDiscard = { showDiscardConfirmation = true },
                        canDeleteSelectedActivity = state.canDeleteSelectedActivity,
                        isDeletingSelectedActivity = state.isDeletingSelectedActivity,
                        onRequestDelete = { showDeleteConfirmation = true },
                        onNavigateBack = onNavigateBack,
                        backLabel = backLabel,
                    )
                }
            }
        }
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text("Descartar treino?") },
            text = { Text("O treino será marcado como descartado e continuará disponível apenas para auditoria local.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirmation = false
                    onDiscardActivity()
                }) {
                    Text("Descartar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (showDeleteConfirmation && state.canDeleteSelectedActivity) {
        state.selectedActivity?.let { activity ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Excluir treino permanentemente?") },
                text = {
                    Text(
                        "Treino de ${activity.metrics.startedAt.formatDateTime()}, tipo ${activity.displayType().toLabel()}. " +
                            "Esta exclusão é irreversível e removerá o treino e seus dados locais. " +
                            "Disponível somente para atividade não sincronizada, sem sessão de treino e sem efeito remoto iniciado.",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            onDeleteActivity()
                        },
                        enabled = !state.isDeletingSelectedActivity,
                    ) {
                        Text("Excluir treino permanentemente")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancelar")
                    }
                },
            )
        } ?: run { showDeleteConfirmation = false }
    }

    if (showReprocessConfirmation) {
        state.selectedActivity?.let { activity ->
            val isSynced = activity.workflowState == ActivityWorkflowState.SYNCED
            AlertDialog(
                onDismissRequest = { showReprocessConfirmation = false },
                title = {
                    Text(if (isSynced) "Reprocessar treino sincronizado?" else "Reprocessar métricas do treino?")
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small)) {
                        Text(
                            when {
                                activity.canonicalReadStatus == WorkoutCanonicalReadStatus.SPLITS_NOT_MATERIALIZED ->
                                    "A série canônica está disponível, mas os splits ainda não foram materializados."
                                activity.requiresWatchWorkoutReprocessing ->
                                    "A atividade não possui snapshot da versão atual ou possui versão anterior."
                                else ->
                                    "A derivação local será reavaliada com os fatos persistidos."
                            },
                        )
                        Text(
                            "A ação usa raw/measured locais, preserva recordedAt, revisão, tipo, título, observações, calibração e identidade da importação. Nenhuma mudança ocorre sem confirmação.",
                        )
                        Text(
                            if (isSynced) {
                                "A atividade já está sincronizada. Esta confirmação não cria nem prepara operação do Health Connect; o reprocessamento continuará bloqueado até suporte futuro de projeção."
                            } else {
                                "Nenhuma operação do Health Connect será criada ou enviada automaticamente."
                            },
                            color = if (isSynced) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showReprocessConfirmation = false
                        onReprocessActivity()
                    }) {
                        Text(if (isSynced) "Confirmar aviso" else "Confirmar reprocessamento")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReprocessConfirmation = false }) {
                        Text("Cancelar")
                    }
                },
            )
        } ?: run { showReprocessConfirmation = false }
    }
}

@Composable
private fun WorkoutActivityDetailContent(
    activity: WorkoutActivity,
    latestCalibration: TreadmillCalibrationSnapshot?,
    reviewFormState: ExerciseReviewFormState,
    calibrationFormState: ExerciseCalibrationFormState,
    referenceDistanceCalibrationAllowed: Boolean,
    operationError: ExerciseOperationError?,
    isReprocessing: Boolean,
    canDiscardSelectedActivity: Boolean,
    onDismissError: () -> Unit,
    onReviewTypeChange: (WorkoutType) -> Unit,
    onReviewDistanceChange: (String) -> Unit,
    onReviewTitleChange: (String) -> Unit,
    onReviewNotesChange: (String) -> Unit,
    onReviewPlausibilityAcceptanceChange: (Boolean) -> Unit,
    onSubmitReview: () -> Unit,
    onCalibrationDistanceChange: (String) -> Unit,
    onSubmitCalibration: () -> Unit,
    onRequestReprocess: () -> Unit,
    isAcceptingPlausibility: Boolean,
    onAcceptPlausibility: () -> Unit,
    onSyncActivity: () -> Unit,
    onRequestDiscard: () -> Unit,
    canDeleteSelectedActivity: Boolean,
    isDeletingSelectedActivity: Boolean,
    onRequestDelete: () -> Unit,
    onNavigateBack: (() -> Unit)?,
    backLabel: String,
) {
    val hasReferenceDistanceCalibration = activity.isReferenceDistanceCalibrationEligible() &&
        referenceDistanceCalibrationAllowed

    Column(
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        ActivityDetailHeader(
            activity = activity,
            onNavigateBack = onNavigateBack,
            backLabel = backLabel,
            canDiscard = canDiscardSelectedActivity,
            canDelete = canDeleteSelectedActivity,
            isDeleting = isDeletingSelectedActivity,
            onRequestDiscard = onRequestDiscard,
            onRequestDelete = onRequestDelete,
        )
        ActivityDetailMetadata(activity)
        ActivityMetricsSection(activity)
        if (activity.heartRateSamples.isNotEmpty()) {
            DetailSectionHeading("Faixas de frequência cardíaca")
            ActivityHeartRateChartSection(activity)
        }
        if (operationError != null) {
            if (operationError.category == ExerciseOperationErrorCategory.WRITE_ACCEPTED_NO_READBACK) {
                PermissionBanner(
                    title = "Escrita local aceita",
                    message = operationError.message,
                    actionLabel = "Fechar",
                    onAction = onDismissError,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                ErrorState(
                    title = operationError.category.toFeedbackTitle(),
                    message = operationError.message,
                    actionLabel = "Fechar",
                    onAction = onDismissError,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (activity.isWatchWorkout && !activity.isCycling && activity.requiresWatchWorkoutReprocessing) {
            WatchWorkoutReprocessSection(
                activity = activity,
                isReprocessing = isReprocessing,
                onRequestReprocess = onRequestReprocess,
            )
        }
        if (hasReferenceDistanceCalibration) {
            DetailSectionHeading("Revisão e calibração")
            ReferenceDistanceCalibrationSection(
                activity = activity,
                formState = calibrationFormState,
                onDistanceChange = onCalibrationDistanceChange,
                onSubmit = onSubmitCalibration,
            )
        }
        if (activity.parserValidity == ParserValidity.PARTIAL) {
            DetailSectionHeading("Qualidade da importação")
            PartialParserNotice(activity)
        }
        when {
            activity.workflowState == ActivityWorkflowState.PENDING_REVIEW -> {
                DetailSectionHeading("Revisão e calibração")
                ReviewFormSection(
                    activity = activity,
                    formState = reviewFormState,
                    calibrationFormState = calibrationFormState,
                    onReviewTypeChange = onReviewTypeChange,
                    onReviewDistanceChange = onReviewDistanceChange,
                    onReviewTitleChange = onReviewTitleChange,
                    onReviewNotesChange = onReviewNotesChange,
                    onReviewPlausibilityAcceptanceChange = onReviewPlausibilityAcceptanceChange,
                    onSubmitReview = onSubmitReview,
                    onCalibrationDistanceChange = onCalibrationDistanceChange,
                )
            }

            activity.workflowState == ActivityWorkflowState.PENDING_CALIBRATION && activity.isWatchWorkout && !activity.isCycling -> {
                if (!hasReferenceDistanceCalibration) {
                    DetailSectionHeading("Revisão e calibração")
                }
                WatchWorkoutPendingDerivationSection(activity)
            }

            activity.workflowState == ActivityWorkflowState.PENDING_CALIBRATION && activity.isCycling -> {
                DetailSectionHeading("Revisão e calibração")
                CyclingCalibrationBlockedSection()
            }

            activity.workflowState == ActivityWorkflowState.PENDING_CALIBRATION -> {
                DetailSectionHeading("Revisão e calibração")
                CalibrationFormSection(
                    activity = activity,
                    latestCalibration = latestCalibration,
                    formState = calibrationFormState,
                    onCalibrationDistanceChange = onCalibrationDistanceChange,
                    onSubmitCalibration = onSubmitCalibration,
                )
            }

            activity.workflowState in setOf(
                ActivityWorkflowState.READY_TO_SYNC,
                ActivityWorkflowState.SYNCING,
                ActivityWorkflowState.SYNCED,
                ActivityWorkflowState.SYNC_FAILED,
                ActivityWorkflowState.DISCARDED,
            ) -> {
                DetailSectionHeading("Workflow e sincronização")
                ReadOnlyWorkflowSection(
                    activity = activity,
                    latestCalibration = latestCalibration,
                    isAcceptingPlausibility = isAcceptingPlausibility,
                    onAcceptPlausibility = onAcceptPlausibility,
                    onSyncActivity = onSyncActivity,
                )
            }
        }
    }
}

@Composable
private fun DetailSectionHeading(title: String) {
    Text(
        text = title,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ActivityDetailHeader(
    activity: WorkoutActivity?,
    onNavigateBack: (() -> Unit)?,
    backLabel: String,
    canDiscard: Boolean = false,
    canDelete: Boolean = false,
    isDeleting: Boolean = false,
    onRequestDiscard: () -> Unit = {},
    onRequestDelete: () -> Unit = {},
) {
    var isOverflowExpanded by remember(activity?.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onNavigateBack != null) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backLabel,
                )
            }
        } else {
            Box(modifier = Modifier.size(48.dp))
        }
        Text(
            text = activity?.displayType()?.toLabel() ?: "Detalhe do treino",
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Box {
            IconButton(
                onClick = { if (canDiscard || canDelete) isOverflowExpanded = true },
                modifier = Modifier.semantics {
                    contentDescription = "Mais opções"
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = null,
                )
            }
            if (canDiscard || canDelete) {
                DropdownMenu(
                    expanded = isOverflowExpanded,
                    onDismissRequest = { isOverflowExpanded = false },
                ) {
                    if (canDiscard) {
                        DropdownMenuItem(
                            text = { Text("Descartar treino") },
                            onClick = {
                                isOverflowExpanded = false
                                onRequestDiscard()
                            },
                        )
                    }
                    if (canDelete) {
                        DropdownMenuItem(
                            text = { Text("Excluir treino permanentemente") },
                            enabled = !isDeleting,
                            onClick = {
                                isOverflowExpanded = false
                                onRequestDelete()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityDetailMetadata(activity: WorkoutActivity) {
    val localDate = activity.metrics.startedAt.atZone(ZoneId.systemDefault()).toLocalDate()
    Text(
        text = "${formatExerciseDayLabel(localDate, LocalDate.now(ZoneId.systemDefault()))} • " +
            "${activity.metrics.startedAt.formatTime()} • ${activity.originLabel().removePrefix("Origem: ")} • " +
            activity.workflowState.toLabel(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private data class CanonicalActivityMetricReadModel(
    val cumulativeSteps: Long,
    val totalDistanceMeters: Double,
    val paceSecondsPerKilometer: Double?,
    val speedMetersPerSecond: Double,
)

private fun WorkoutActivity.canonicalMetricReadModel(): CanonicalActivityMetricReadModel? {
    if (isCycling) return null
    val series = canonicalSeries ?: return null
    val lastInterval = series.intervals.lastOrNull() ?: return null
    return CanonicalActivityMetricReadModel(
        cumulativeSteps = lastInterval.cumulativeSteps,
        totalDistanceMeters = lastInterval.cumulativeDistanceMeters,
        paceSecondsPerKilometer = lastInterval.cumulativeMetrics.paceSecondsPerKilometer,
        speedMetersPerSecond = lastInterval.cumulativeMetrics.speedMetersPerSecond,
    )
}

@Composable
private fun ActivityMetricsSection(activity: WorkoutActivity) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
    ) {
        activity.referenceDetailMetricItems().forEach { item ->
            ActivityDetailMetricRow(item)
        }
    }
}

private fun WorkoutActivity.referenceDetailMetricItems(): List<DetailMetricItem> = buildList {
    add(DetailMetricItem("Duração", metrics.duration.toReferenceDurationLabel()))
    val canonical = canonicalMetricReadModel()
        ?.takeIf { isWatchWorkout && canonicalReadStatus == WorkoutCanonicalReadStatus.AVAILABLE }
    val distanceMeters = if (isWatchWorkout) canonical?.totalDistanceMeters else displayDistanceMetersOrNull()
    distanceMeters?.let {
        add(DetailMetricItem("Distância", distanceMeters.toDistanceLabel()))
    }
    val measuredSteps = if (isWatchWorkout) {
        canonical?.cumulativeSteps
    } else {
        metrics.steps.takeIf { metrics.stepsAvailable }
    }
    measuredSteps?.let { steps ->
        add(DetailMetricItem("Etapas", numberLabel(steps)))
    }
    referenceAveragePaceSeconds()?.let { paceSeconds ->
        add(DetailMetricItem("Ritmo médio", paceSeconds.toReferencePaceLabel()))
    }
    (metrics.averageBpm as? DomainValue.Present)
        ?.value
        ?.takeIf { heartRateSamples.isNotEmpty() }
        ?.let { averageBpm ->
            add(DetailMetricItem("Frequência cardíaca média", "$averageBpm bpm"))
        }
}

private fun WorkoutActivity.referenceAveragePaceSeconds(): Double? =
    when {
        isCycling -> null
        isWatchWorkout -> canonicalMetricReadModel()
            ?.takeIf { canonicalReadStatus == WorkoutCanonicalReadStatus.AVAILABLE }
            ?.paceSecondsPerKilometer
        else -> metrics.calibratedMetrics?.averagePaceSecondsPerKilometer?.valueOrNull()
            ?: metrics.averagePaceSecondsPerKilometer.valueOrNull()
    }

private fun Double.toReferencePaceLabel(): String {
    if (!isFinite() || this <= 0.0) return "indisponível"
    val totalSeconds = roundToLong()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (seconds == 0L) {
        "$minutes min"
    } else {
        "$minutes min $seconds s"
    }
}

@Composable
private fun ActivityDetailMetricRow(item: DetailMetricItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${item.label}: ${item.value}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = item.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = item.value,
            modifier = Modifier.padding(start = ChampionSpacing.medium),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MetricCard(label: String, value: String, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeMedium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, accentColor),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, color = accentColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ActivityHeartRateChartSection(activity: WorkoutActivity) {
    Text(
        text = activity.referenceAverageHeartRateLabel(),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.SemiBold,
    )
    WorkoutHeartRateChart(
        samples = activity.heartRateSamples,
        activityDuration = activity.metrics.duration,
    )
}

private fun WorkoutActivity.referenceAverageHeartRateLabel(): String =
    when (val average = metrics.averageBpm) {
        is DomainValue.Present -> "${average.value} bpm (méd.)"
        is DomainValue.Absent -> "FC indisponível"
    }

private fun Duration.toDetailDurationLabel(): String {
    if (isNegative) return "indisponível"
    val totalSeconds = seconds
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val remainingSeconds = totalSeconds % 60
    return buildList {
        if (hours > 0) add("$hours ${if (hours == 1L) "hora" else "horas"}")
        if (minutes > 0) add("$minutes ${if (minutes == 1L) "minuto" else "minutos"}")
        if (remainingSeconds > 0 || isZero) {
            add("$remainingSeconds ${if (remainingSeconds == 1L) "segundo" else "segundos"}")
        }
    }.joinToString(" ")
}

private fun Duration.toReferenceDurationLabel(): String {
    if (isNegative) return "indisponível"
    val totalSeconds = seconds
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val remainingSeconds = totalSeconds % 60
    return buildList {
        if (hours > 0) add("$hours h")
        if (minutes > 0) add("$minutes min")
        if (remainingSeconds > 0 || isZero) add("$remainingSeconds s")
    }.joinToString(" ")
}

@Composable
private fun WorkoutHeartRateChart(
    samples: List<WorkoutHeartRateSample>,
    activityDuration: Duration,
) {
    val sortedSamples = samples.sortedBy { it.sampledAt }
    val zoneId = ZoneId.systemDefault()
    val activityStart = sortedSamples.first().sampledAt
        .atZone(zoneId)
        .toInstant()
    val activityDurationSeconds = activityDuration.seconds.toDouble() +
        activityDuration.nano.toDouble() / 1_000_000_000.0
    val chartDurationSeconds = activityDurationSeconds
        .takeIf { it > 0.0 && it.isFinite() }
        ?: Duration.between(activityStart, sortedSamples.last().sampledAt)
            .toMillis()
            .toDouble()
            .div(1_000.0)
            .coerceAtLeast(1.0)
    val chartPoints = sortedSamples.map { sample ->
        ChampionChartPoint(
            x = Duration.between(activityStart, sample.sampledAt).toMillis() / 1_000.0,
            y = sample.bpm.toDouble(),
            xLabel = sample.sampledAt.formatChartTime(),
            contentDescription = "${sample.bpm} bpm em ${sample.sampledAt.formatDateTime()}.",
        )
    }
    val chartDescription = "Gráfico de frequência cardíaca do treino. " +
        "Duração ${activityDuration.toDetailDurationLabel()}. " +
        "${chartPoints.size} amostra(s) real(is) disponível(is)."
    GoogleHealthIntradayHeartRateChart(
        points = chartPoints,
        accessibilitySummary = chartDescription,
        showLine = true,
        timeWindowSeconds = chartDurationSeconds,
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
    )
}

@Composable
private fun ActivityIntervalsSection(activity: WorkoutActivity) {
    if (activity.isCycling) return
    if (activity.isWatchWorkout) {
        WatchCanonicalIntervalsSection(activity)
        return
    }
    var isDialogOpen by remember(activity.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text("Intervalos", style = MaterialTheme.typography.titleLarge)
            if (activity.intervals.isEmpty()) {
                Text(
                    text = "Este treino não possui intervalos estruturados para exibir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Os intervalos ficam disponíveis sob demanda para reduzir ruído visual no detalhe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { isDialogOpen = true }) {
                    Text("Ver intervalos")
                }
            }
        }
    }

    if (isDialogOpen) {
        AlertDialog(
            onDismissRequest = { isDialogOpen = false },
            title = { Text("Intervalos do treino") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
                ) {
                    activity.intervals.forEachIndexed { index, interval ->
                        IntervalCard(
                            interval = interval,
                            bpmSummary = intervalBpmSummary(activity, index),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isDialogOpen = false }) {
                    Text("Fechar")
                }
            },
        )
    }
}

@Composable
private fun WatchCanonicalIntervalsSection(activity: WorkoutActivity) {
    var isDialogOpen by remember(activity.id) { mutableStateOf(false) }
    val intervals = activity.canonicalSeries?.intervals.orEmpty()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text("Intervalos canônicos", style = MaterialTheme.typography.titleLarge)
            when {
                activity.canonicalSeries == null -> Text(
                    text = "Intervalos canônicos indisponíveis: ${activity.canonicalStatusMessage()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                intervals.isEmpty() -> Text(
                    text = "A série canônica não possui intervalos derivados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> {
                    Text(
                        text = "Intervalos exibem somente passos medidos e métricas da série persistida.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { isDialogOpen = true }) {
                        Text("Ver intervalos")
                    }
                }
            }
        }
    }

    if (isDialogOpen) {
        AlertDialog(
            onDismissRequest = { isDialogOpen = false },
            title = { Text("Intervalos canônicos do treino") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
                ) {
                    intervals.forEach { interval -> CanonicalIntervalCard(interval) }
                }
            },
            confirmButton = {
                TextButton(onClick = { isDialogOpen = false }) { Text("Fechar") }
            },
        )
    }
}

@Composable
private fun CanonicalIntervalCard(interval: DerivedWorkoutInterval) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Intervalo ${interval.index + 1}: ${interval.canonicalIntervalDistanceMeters.toDistanceLabel()}, " +
                    "${interval.cumulativeDistanceMeters.toDistanceLabel()} acumulados, " +
                    "ritmo ${interval.paceSecondsPerKilometer.toPaceLabel()} e velocidade ${interval.speedMetersPerSecond.toSpeedLabel()}"
            },
        shape = ChampionShapeMedium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Intervalo ${interval.index + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${interval.effectiveDuration.toCanonicalDurationLabel()} • ${numberLabel(interval.decodedSteps)} passos medidos • ${interval.canonicalIntervalDistanceMeters.toDistanceLabel()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Ritmo estimado ${interval.paceSecondsPerKilometer.toPaceLabel()} • Velocidade estimada ${interval.speedMetersPerSecond.toSpeedLabel()}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Distância acumulada ${interval.cumulativeDistanceMeters.toDistanceLabel()} • Cadência ${formatCadence(interval.cadenceStepsPerMinute)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "BPM por quilômetro é exibido nos splits persistidos abaixo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActivitySplitsSection(activity: WorkoutActivity) {
    if (!activity.isWatchWorkout || activity.isCycling) return
    val snapshot = activity.derivationSnapshot
    val splits = snapshot?.splits.orEmpty()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text("Splits e BPM", style = MaterialTheme.typography.titleLarge)
            when (activity.canonicalReadStatus) {
                WorkoutCanonicalReadStatus.MISSING_STEP_LENGTH,
                WorkoutCanonicalReadStatus.NOT_MATERIALIZED,
                WorkoutCanonicalReadStatus.SERIES_INELIGIBLE,
                WorkoutCanonicalReadStatus.PLAUSIBILITY_BLOCKED,
                -> Text(
                    text = "Splits indisponíveis: ${activity.canonicalStatusMessage()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                WorkoutCanonicalReadStatus.SPLITS_NOT_MATERIALIZED -> Text(
                    text = "A série canônica está disponível, mas os splits ainda não foram materializados. Reprocesse a atividade por ação explícita.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                WorkoutCanonicalReadStatus.AVAILABLE -> {
                    if (splits.isEmpty()) {
                        Text(
                            text = "Nenhuma parcial persistida para exibir.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text("Parciais persistidas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        splits.forEach { split -> WorkoutSplitCard(split) }
                    }
                }

                WorkoutCanonicalReadStatus.NOT_APPLICABLE -> Unit
            }
        }
    }
}

@Composable
private fun WorkoutSplitCard(split: WorkoutSplit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = splitAccessibleDescription(split)
            },
        shape = ChampionShapeMedium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Split ${split.number}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${split.distanceMeters.toDistanceLabel()} • ${split.duration.toCanonicalDurationLabel()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Ritmo estimado ${split.averagePaceSecondsPerKilometer.toPaceLabel()} • Velocidade estimada ${split.averageSpeedMetersPerSecond.toSpeedLabel()}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Cadência média ${formatCadence(split.averageCadenceStepsPerMinute)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = split.averageBpm?.let {
                    "BPM médio ponderado: ${formatBpm(it)} (${split.bpmCoveredDuration.toCanonicalDurationLabel()} de cobertura)"
                } ?: "BPM indisponível: sem cobertura temporal elegível.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IntervalCard(interval: WorkoutInterval, bpmSummary: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeMedium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Intervalo ${interval.index + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${interval.duration.toDurationLabel()} • ${numberLabel(interval.steps)} passos • ${interval.distanceMeters.toDistanceLabel()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Calorias ${interval.caloriesKilocalories.toKcalLabel()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = bpmSummary ?: "BPM do intervalo indisponível sem associação temporal segura.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PartialParserNotice(activity: WorkoutActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Importação parcial", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (activity.isWatchWorkout) {
                    "Treino do relógio importado com dados parciais. Revise os fatos disponíveis; distância canônica ausente não é substituída por calibração histórica."
                } else {
                    "Treino importado com dados parciais, mas elegível para revisão, calibração quando necessária e sincronização futura, alinhado ao comportamento anterior do app."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun ReviewFormSection(
    activity: WorkoutActivity,
    formState: ExerciseReviewFormState,
    calibrationFormState: ExerciseCalibrationFormState,
    onReviewTypeChange: (WorkoutType) -> Unit,
    onReviewDistanceChange: (String) -> Unit,
    onReviewTitleChange: (String) -> Unit,
    onReviewNotesChange: (String) -> Unit,
    onReviewPlausibilityAcceptanceChange: (Boolean) -> Unit,
    onSubmitReview: () -> Unit,
    onCalibrationDistanceChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text("Revisão da atividade", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Selecione tipo final, título opcional e observações locais antes de concluir a revisão.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WorkoutType.entries.filter { type ->
                type != WorkoutType.UNKNOWN && (activity.isWatchWorkout || type != WorkoutType.CYCLING)
            }.forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReviewTypeChange(type) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = formState.selectedType == type,
                        onClick = { onReviewTypeChange(type) },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall)) {
                        Text(type.toLabel(), style = MaterialTheme.typography.bodyLarge)
                        if (type == activity.suggestedType) {
                            Text("Tipo sugerido pelo parser", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (activity.isWatchWorkout && formState.selectedType == WorkoutType.CYCLING) {
                OutlinedTextField(
                    value = formState.manualDistanceInput,
                    onValueChange = onReviewDistanceChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Distância") },
                    supportingText = { Text("Opcional") },
                    suffix = { Text("km") },
                    singleLine = true,
                    isError = formState.error?.category == ExerciseOperationErrorCategory.INVALID_DISTANCE,
                    enabled = !formState.isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            if (activity.isWatchWorkout && formState.selectedType in setOf(WorkoutType.RUNNING, WorkoutType.WALKING)) {
                OutlinedTextField(
                    value = calibrationFormState.reportedDistanceInput,
                    onValueChange = onCalibrationDistanceChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Distância de referência") },
                    supportingText = {
                        Text("Opcional agora; após salvar revisão, será usada para calibrar corrida/caminhada. Informe fonte externa; o app não coleta GPS.")
                    },
                    suffix = { Text("km") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            OutlinedTextField(
                value = formState.titleInput,
                onValueChange = onReviewTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Título opcional") },
                singleLine = true,
                enabled = !formState.isSaving,
            )
            OutlinedTextField(
                value = formState.notesInput,
                onValueChange = onReviewNotesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Observações locais") },
                minLines = 3,
                enabled = !formState.isSaving,
            )
            when {
                activity.derivationSnapshot?.hasPlausibilityIndicators == true -> Text(
                    text = "Indicadores IMPLAUSIBLE_* impedem aceite genérico. A série continua bloqueada para projeção Health Connect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )

                activity.derivationSnapshot?.requiresExplicitPlausibilityAcceptance == true &&
                    formState.selectedType != WorkoutType.CYCLING -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = formState.acceptPlausibility,
                        onCheckedChange = onReviewPlausibilityAcceptanceChange,
                        enabled = !formState.isSaving,
                    )
                    Text(
                        text = "Confirmo explicitamente série sem indicadores para preparar projeção Health Connect. O app não inventa limiar global.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            formState.error?.let {
                Text(it.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            PrimaryActionButton(
                label = if (formState.isSaving) "Salvando revisão" else reviewActionLabel(activity, formState.selectedType),
                onClick = onSubmitReview,
                enabled = formState.selectedType != null,
                isLoading = formState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun WatchWorkoutReprocessSection(
    activity: WorkoutActivity,
    isReprocessing: Boolean,
    onRequestReprocess: () -> Unit,
) {
    val isSynced = activity.workflowState == ActivityWorkflowState.SYNCED
    val isSyncing = activity.workflowState == ActivityWorkflowState.SYNCING
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (isSynced) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Reprocessamento explícito", style = MaterialTheme.typography.titleLarge)
            Text(
                when {
                    activity.canonicalReadStatus == WorkoutCanonicalReadStatus.SPLITS_NOT_MATERIALIZED ->
                        "A série canônica está disponível, mas os splits ainda não foram materializados. A derivação usa fatos raw/measured persistidos e a versão vigente."
                    else ->
                        "Snapshot atual ausente ou versão anterior${activity.derivationSnapshot?.derivationVersion?.let { " ($it)" } ?: ""}. A derivação usa fatos raw/measured persistidos e a versão vigente."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSynced) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                if (isSynced) {
                    "Atividade sincronizada: confirmação exibirá aviso específico e não preparará operação do Health Connect."
                } else {
                    "Revisão, tipo, título, observações, calibração e identidade da importação serão preservados."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isSynced) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
            )
            if (isSyncing) {
                Text(
                    "Sincronização em andamento. Aguarde antes de reprocessar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            } else {
                PrimaryActionButton(
                    label = if (isReprocessing) "Reprocessando derivação" else "Reprocessar derivação local",
                    onClick = onRequestReprocess,
                    enabled = !isReprocessing,
                    isLoading = isReprocessing,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun WatchWorkoutPendingDerivationSection(activity: WorkoutActivity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Ação pendente: ${activity.canonicalStatusMessage()}"
            },
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Calibração histórica não necessária", style = MaterialTheme.typography.titleLarge)
            Text(
                text = when (activity.canonicalReadStatus) {
                    WorkoutCanonicalReadStatus.MISSING_STEP_LENGTH ->
                        "Este treino do relógio não pede calibração de esteira. Configure o comprimento do passo no Perfil e reprocese a atividade por ação explícita."
                    WorkoutCanonicalReadStatus.SERIES_INELIGIBLE ->
                        "A série canônica está inelegível. Corrija a origem por reprocessamento explícito; nenhuma distância histórica será usada como substituta."
                    else -> activity.canonicalStatusMessage()
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun CyclingCalibrationBlockedSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Calibração indisponível para bicicleta", style = MaterialTheme.typography.titleLarge)
            Text(
                "Bicicleta não usa calibração de esteira. A atividade permanece sem distância funcional até revisão válida.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun CalibrationFormSection(
    activity: WorkoutActivity,
    latestCalibration: TreadmillCalibrationSnapshot?,
    formState: ExerciseCalibrationFormState,
    onCalibrationDistanceChange: (String) -> Unit,
    onSubmitCalibration: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text("Calibração de esteira", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Informe distância real em km para derivar distância final, ritmo e velocidade sem alterar métricas importadas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MetricCard("Base importada", activity.metrics.distances.toImportedDistanceLabel(), exercisePrimaryAccent())
            latestCalibration?.let {
                Text(
                    text = "Última calibração ativa: revisão ${it.revision} em ${it.appliedAt.formatDateTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = formState.reportedDistanceInput,
                onValueChange = onCalibrationDistanceChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Distância real em km") },
                singleLine = true,
                enabled = !formState.isSaving,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
            ) {
                MetricCard("Prévia distância", formState.previewDistanceMeters.toDistanceLabel(), exercisePrimaryAccent())
                MetricCard("Prévia ritmo", formState.previewAveragePaceSecondsPerKilometer.toPaceLabel(), exerciseSecondaryAccent())
                MetricCard("Prévia velocidade", formState.previewAverageSpeedMetersPerSecond.toSpeedLabel(), exerciseTertiaryAccent())
            }
            formState.validationError?.let {
                Text(it.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            PrimaryActionButton(
                label = if (formState.isSaving) "Salvando calibração" else "Aplicar calibração",
                onClick = onSubmitCalibration,
                enabled = formState.reportedDistanceInput.isNotBlank(),
                isLoading = formState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun WorkoutActivity.isReferenceDistanceCalibrationEligible(): Boolean =
    activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4 &&
        confirmedType in setOf(WorkoutType.RUNNING, WorkoutType.WALKING) &&
        workflowState in setOf(
            ActivityWorkflowState.PENDING_CALIBRATION,
            ActivityWorkflowState.READY_TO_SYNC,
        ActivityWorkflowState.SYNC_FAILED,
        ) &&
        parserValidity == ParserValidity.VALID &&
        derivationSnapshot?.canonicalSeriesEligible == true &&
        intervals.any { it.decodedSteps != null && it.decodedSteps > 0L }

@Composable
private fun ReferenceDistanceCalibrationSection(
    activity: WorkoutActivity,
    formState: ExerciseCalibrationFormState,
    onDistanceChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = ChampionShapeLarge) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text("Calibrar com distância de referência", style = MaterialTheme.typography.titleLarge)
            Text(
                "Informe distância externa a partir de fonte conhecida. O Champion Companion não coleta GPS.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MetricCard("Distância atual estimada", activity.canonicalDistanceMeters.toDistanceLabel(), exercisePrimaryAccent())
            MetricCard("Distância após calibração", formState.previewDistanceMeters.toDistanceLabel(), exerciseSecondaryAccent())
            MetricCard("Ritmo após calibração", formState.previewAveragePaceSecondsPerKilometer.toPaceLabel(), exerciseTertiaryAccent())
            MetricCard("Velocidade após calibração", formState.previewAverageSpeedMetersPerSecond.toSpeedLabel(), exercisePrimaryAccent())
            OutlinedTextField(
                value = formState.reportedDistanceInput,
                onValueChange = onDistanceChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Distância de referência (km)") },
                singleLine = true,
                enabled = !formState.isSaving,
            )
            MetricCard("Passos usados", formState.eligibleSteps?.let(::numberLabel) ?: "Indisponível", exerciseSecondaryAccent())
            MetricCard("Comprimento observado do passo", formState.observedStepLengthMeters.toStepLengthLabel(), exerciseTertiaryAccent())
            MetricCard("Perfil atual", formState.currentProfileStepLengthMeters.toStepLengthLabel(), exercisePrimaryAccent())
            MetricCard("Perfil após calibração", formState.futureProfileStepLengthMeters.toStepLengthLabel(), exerciseSecondaryAccent())
            Text(
                "Aceite atualiza atividade e perfil futuro localmente. Nenhuma sincronização automática será iniciada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            formState.validationError?.let {
                Text(it.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            PrimaryActionButton(
                label = if (formState.isSaving) "Aplicando calibração" else "Aplicar calibração",
                onClick = onSubmit,
                enabled = formState.reportedDistanceInput.isNotBlank(),
                isLoading = formState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ReadOnlyWorkflowSection(
    activity: WorkoutActivity,
    latestCalibration: TreadmillCalibrationSnapshot?,
    isAcceptingPlausibility: Boolean,
    onAcceptPlausibility: () -> Unit,
    onSyncActivity: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Estado atual", style = MaterialTheme.typography.titleLarge)
            Text(
                text = when (activity.workflowState) {
                    ActivityWorkflowState.READY_TO_SYNC -> "Treino pronto para sincronizar com o Health Connect por ação explícita."
                    ActivityWorkflowState.SYNCING -> "Sincronização em andamento no Health Connect."
                    ActivityWorkflowState.SYNCED -> "Escritas locais aceitas; o Health Connect não foi lido para confirmar records."
                    ActivityWorkflowState.SYNC_FAILED -> "A sincronização anterior falhou ou ficou parcial. Você pode tentar novamente sem duplicar records já enviados."
                    ActivityWorkflowState.DISCARDED -> "Treino descartado localmente. Dados permanecem visíveis apenas para auditoria local."
                    else -> "Sem ações editáveis neste estado."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            activity.title?.let { Text("Título salvo: $it", style = MaterialTheme.typography.bodyMedium) }
            activity.notes?.let { Text("Observações: $it", style = MaterialTheme.typography.bodyMedium) }
            latestCalibration?.let {
                Text(
                    text = if (activity.isWatchWorkout) {
                        "Calibração histórica (diagnóstico): revisão ${it.revision}"
                    } else {
                        "Calibração ativa: revisão ${it.revision}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (activity.activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4 &&
                activity.confirmedType != WorkoutType.CYCLING &&
                activity.workflowState in setOf(
                    ActivityWorkflowState.READY_TO_SYNC,
                    ActivityWorkflowState.SYNC_FAILED,
                ) && activity.derivationSnapshot?.requiresExplicitPlausibilityAcceptance == true
            ) {
                Text(
                    "A série não possui indicadores IMPLAUSIBLE_*. Confirme explicitamente plausibilidade antes de preparar DISTANCE/SPEED. Nenhum limiar global é inventado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryActionButton(
                    label = if (isAcceptingPlausibility) "Salvando aceite" else "Confirmar plausibilidade",
                    onClick = onAcceptPlausibility,
                    enabled = !isAcceptingPlausibility,
                    isLoading = isAcceptingPlausibility,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (activity.workflowState in setOf(ActivityWorkflowState.READY_TO_SYNC, ActivityWorkflowState.SYNC_FAILED, ActivityWorkflowState.SYNCING)) {
                PrimaryActionButton(
                    label = when (activity.workflowState) {
                        ActivityWorkflowState.READY_TO_SYNC -> "Sincronizar com Health Connect"
                        ActivityWorkflowState.SYNC_FAILED -> "Tentar novamente no Health Connect"
                        ActivityWorkflowState.SYNCING -> "Sincronizando com Health Connect"
                        else -> "Sincronizar com Health Connect"
                    },
                    onClick = onSyncActivity,
                    enabled = activity.workflowState != ActivityWorkflowState.SYNCING,
                    isLoading = activity.workflowState == ActivityWorkflowState.SYNCING,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun reviewActionLabel(activity: WorkoutActivity, type: WorkoutType?): String =
    if (type == WorkoutType.TREADMILL && !activity.isWatchWorkout) {
        "Salvar revisão e ir para calibração"
    } else {
        "Concluir revisão"
    }

private data class DetailMetricItem(
    val label: String,
    val value: String,
)

private fun WorkoutActivity.detailMetricItems(): List<DetailMetricItem> {
    return listOf(
        DetailMetricItem("Duração", metrics.duration.toDetailDurationLabel()),
        DetailMetricItem("Distância", displayDistanceLabel()),
        DetailMetricItem("Etapas", displayStepsLabel()),
        DetailMetricItem("Ritmo médio", displayAveragePaceLabel()),
        DetailMetricItem("Frequência cardíaca média", metrics.averageBpm.toBpmLabel()),
    )
}

private fun WorkoutActivity.displayStepsLabel(): String = if (isWatchWorkout) {
    canonicalMetricReadModel()?.cumulativeSteps?.let(::numberLabel) ?: "indisponível"
} else if (metrics.stepsAvailable) {
    numberLabel(metrics.steps)
} else {
    "indisponível"
}

private fun heartRateAccessibleSummary(samples: List<WorkoutHeartRateSample>): String {
    if (samples.isEmpty()) return "Resumo acessível: sem amostras reais de BPM neste treino."
    val average = samples.map { it.bpm }.average().roundToInt()
    val min = samples.minOf { it.bpm }
    val max = samples.maxOf { it.bpm }
    return "Resumo acessível: ${samples.size} amostras reais, média $average bpm, mínimo $min bpm e máximo $max bpm."
}

private fun intervalBpmSummary(activity: WorkoutActivity, index: Int): String? {
    if (activity.heartRateSamples.isEmpty()) return null
    var start = activity.metrics.startedAt
    activity.intervals.forEachIndexed { currentIndex, interval ->
        val end = start.plus(interval.duration)
        if (currentIndex == index) {
            val windowSamples = activity.heartRateSamples.filter { sample ->
                !sample.sampledAt.isBefore(start) && sample.sampledAt.isBefore(end)
            }
            if (windowSamples.isEmpty()) return null
            val average = windowSamples.map { it.bpm }.average().roundToInt()
            val min = windowSamples.minOf { it.bpm }
            val max = windowSamples.maxOf { it.bpm }
            return "BPM do intervalo: média $average, mínimo $min, máximo $max."
        }
        start = end
    }
    return null
}

private fun numberLabel(value: Long): String =
    java.text.NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(value)

private val WorkoutActivity.isWatchWorkout: Boolean
    get() = activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4

private val WorkoutActivity.isCycling: Boolean
    get() = confirmedType == WorkoutType.CYCLING

private fun WorkoutActivity.isEligibleForExerciseCalendar(): Boolean =
    workflowState != ActivityWorkflowState.DISCARDED

private fun WorkoutActivity.cyclingManualDistanceMetersOrNull(): Double? {
    if (!isCycling) return null
    return when (val distance = metrics.distances.manualDistanceMeters) {
        is DomainValue.Present -> distance.value.takeIf { it.isFinite() && it > 0.0 }
        is DomainValue.Absent -> null
    }
}

private fun WorkoutActivity.cyclingAverageSpeedMetersPerSecond(): Double? {
    val distance = cyclingManualDistanceMetersOrNull() ?: return null
    val duration = metrics.duration
    if (duration.isNegative || duration.isZero) return null
    val durationSeconds = duration.seconds.toDouble() + duration.nano.toDouble() / 1_000_000_000.0
    if (!durationSeconds.isFinite() || durationSeconds <= 0.0) return null
    return (distance / durationSeconds).takeIf { it.isFinite() && it > 0.0 }
}

private fun WorkoutActivity.displayDistanceMetersOrNull(): Double? = when {
    isCycling -> cyclingManualDistanceMetersOrNull()
    isWatchWorkout -> canonicalMetricReadModel()
        ?.takeIf { canonicalReadStatus == WorkoutCanonicalReadStatus.AVAILABLE }
        ?.totalDistanceMeters
    else -> metrics.distances.finalDistanceMetersOrNull()
}

private fun WorkoutActivity.displayDistanceLabel(): String =
    displayDistanceMetersOrNull().toDistanceLabel()

private fun WorkoutActivity.canonicalStatusAccessibleDescription(): String =
    "${canonicalReadStatus.toShortLabel()}. ${canonicalStatusMessage()}"

private fun WorkoutActivity.statusOriginAccessibleDescription(): String = buildString {
    append("Status: ${workflowState.toLabel()}. ")
    append("Tipo: ${displayType().toLabel()}. ")
    append("Parser: ${parserValidity.toLabel()}. ")
    append("${originLabel()}.")
    if (isWatchWorkout && !isCycling) {
        append(" ${canonicalStatusAccessibleDescription()}")
    }
}

private fun WorkoutActivity.originLabel(): String = when (activityOrigin) {
    WorkoutActivityOrigin.WATCH_D3_D4 -> "Origem: relógio"
    WorkoutActivityOrigin.PHONE_GPS,
    WorkoutActivityOrigin.PHONE_TREADMILL,
    WorkoutActivityOrigin.PHONE_FREE_WORKOUT,
    -> "Origem: sessão local legada"
}

private fun WorkoutActivity.canonicalStatusMessage(): String = when (canonicalReadStatus) {
    WorkoutCanonicalReadStatus.NOT_APPLICABLE -> "Métricas canônicas não se aplicam a esta origem."
    WorkoutCanonicalReadStatus.MISSING_STEP_LENGTH ->
        "Sem comprimento do passo confirmado. Configure o valor no Perfil e reprocese a atividade por ação explícita."
    WorkoutCanonicalReadStatus.NOT_MATERIALIZED ->
        "A derivação canônica ainda não foi materializada. Configure o comprimento do passo no Perfil e reprocese a atividade por ação explícita."
    WorkoutCanonicalReadStatus.SERIES_INELIGIBLE -> {
        val reasons = derivationSnapshot?.temporalResolution?.reasons.orEmpty()
        if (reasons.isEmpty()) {
            "A série canônica é inelegível. Reprocesse a atividade por ação explícita; nenhuma distância legada substitui este estado."
        } else {
            "A série canônica é inelegível. Motivo: ${reasons.joinToString { it.toLabel() }}."
        }
    }
    WorkoutCanonicalReadStatus.PLAUSIBILITY_BLOCKED ->
        "A série canônica está visível para diagnóstico, mas contém valores implausíveis e não deve ser tratada como confiável."
    WorkoutCanonicalReadStatus.SPLITS_NOT_MATERIALIZED ->
        "A série canônica está disponível; splits de 1 km ainda não foram materializados no read model persistido."
    WorkoutCanonicalReadStatus.AVAILABLE ->
        "Distância, ritmo, velocidade e acumulado usam a série canônica persistida."
}

private fun ExerciseOperationErrorCategory.toFeedbackTitle(): String = when (this) {
    ExerciseOperationErrorCategory.PERMISSION_REQUIRED -> "Permissão pendente"
    ExerciseOperationErrorCategory.PROVIDER_UNAVAILABLE -> "Provider indisponível"
    ExerciseOperationErrorCategory.PARTIAL_BLOCKED -> "Falha parcial por capacidade"
    ExerciseOperationErrorCategory.SYNC_DISABLED -> "Sincronização desativada"
    ExerciseOperationErrorCategory.SYNC_FAILED -> "Falha de sincronização"
    ExerciseOperationErrorCategory.WRITE_ACCEPTED_NO_READBACK -> "Escrita local aceita"
    ExerciseOperationErrorCategory.PLAUSIBILITY_ACCEPTANCE_REQUIRED -> "Aceite explícito necessário"
    ExerciseOperationErrorCategory.PLAUSIBILITY_INDICATORS_BLOCKED -> "Plausibilidade bloqueada"
    else -> "Ação indisponível"
}

private fun WorkoutCanonicalReadStatus.toShortLabel(): String = when (this) {
    WorkoutCanonicalReadStatus.NOT_APPLICABLE -> "Métricas canônicas não aplicáveis"
    WorkoutCanonicalReadStatus.MISSING_STEP_LENGTH -> "Sem comprimento do passo"
    WorkoutCanonicalReadStatus.NOT_MATERIALIZED -> "Derivação não materializada"
    WorkoutCanonicalReadStatus.SERIES_INELIGIBLE -> "Série canônica inelegível"
    WorkoutCanonicalReadStatus.PLAUSIBILITY_BLOCKED -> "Série estimada implausível"
    WorkoutCanonicalReadStatus.SPLITS_NOT_MATERIALIZED -> "Splits ainda não materializados"
    WorkoutCanonicalReadStatus.AVAILABLE -> "Métricas canônicas disponíveis"
}

private fun StepLengthSource.toLabel(): String = when (this) {
    StepLengthSource.MANUAL -> "entrada manual"
    StepLengthSource.HEIGHT_SUGGESTION_ACCEPTED -> "sugestão aceita pela altura"
    StepLengthSource.REFERENCE_DISTANCE_CALIBRATION -> "calibração de distância"
    StepLengthSource.CONTEXTUAL_LEARNED_PROFILE -> "perfil contextual aprendido"
}

private fun WorkoutPlausibilityIndicator.toLabel(): String = when (this) {
    WorkoutPlausibilityIndicator.IMPLAUSIBLE_CADENCE -> "cadência implausível"
    WorkoutPlausibilityIndicator.IMPLAUSIBLE_SPEED -> "velocidade implausível"
}

private fun TemporalSeriesValidationReason.toLabel(): String = when (this) {
    TemporalSeriesValidationReason.UNIQUE,
    TemporalSeriesValidationReason.DUPLICATE_INDEX,
    -> "índice duplicado"
    TemporalSeriesValidationReason.NON_CONTIGUOUS,
    TemporalSeriesValidationReason.INDEX_GAP,
    -> "lacuna nos índices"
    TemporalSeriesValidationReason.FIRST_INDEX_NOT_ZERO -> "primeiro índice diferente de zero"
    TemporalSeriesValidationReason.OUT_OF_ORDER -> "ordem temporal inconsistente"
    TemporalSeriesValidationReason.EMPTY -> "intervalos ausentes"
    TemporalSeriesValidationReason.INVALID -> "dados temporais inválidos"
    TemporalSeriesValidationReason.INSUFFICIENT_TOTAL_DURATION -> "duração total insuficiente"
    TemporalSeriesValidationReason.MISSING_DECODED_STEPS -> "passos decodificados ausentes"
}

private fun formatStepLength(value: Double): String =
    String.format(Locale.forLanguageTag("pt-BR"), "%.3f m", value)

private fun formatCadence(value: Double): String =
    String.format(Locale.forLanguageTag("pt-BR"), "%.0f passos/min", value)

private fun formatBpm(value: Double): String =
    String.format(Locale.forLanguageTag("pt-BR"), "%.1f BPM", value)

private fun splitAccessibleDescription(split: WorkoutSplit): String =
    "Split ${split.number}: ${split.distanceMeters.toDistanceLabel()}, ${split.duration.toCanonicalDurationLabel()}, " +
        "ritmo ${split.averagePaceSecondsPerKilometer.toPaceLabel()}, " +
        "velocidade ${split.averageSpeedMetersPerSecond.toSpeedLabel()}, " +
        (split.averageBpm?.let { "BPM médio ponderado ${formatBpm(it)}" } ?: "BPM sem cobertura temporal")

private fun Duration.toCanonicalDurationLabel(): String {
    val totalSeconds = seconds
    if (totalSeconds < 60L) return "$totalSeconds s"
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L && minutes > 0L -> "$hours h ${minutes} min"
        hours > 0L -> "$hours h"
        seconds > 0L -> "$minutes min ${seconds} s"
        else -> "$minutes min"
    }
}

private fun DomainValue<Double>.toKcalLabel(): String = when (this) {
    is DomainValue.Present -> String.format(Locale.forLanguageTag("pt-BR"), "%.0f kcal", value)
    is DomainValue.Absent -> "indisponível"
}

private fun DomainValue<Int>.toBpmLabel(): String = when (this) {
    is DomainValue.Present -> "$value bpm"
    is DomainValue.Absent -> "indisponível"
}

private fun DomainValue<Double>.toDistanceLabel(): String = when (this) {
    is DomainValue.Present -> String.format(Locale.forLanguageTag("pt-BR"), "%.2f km", value / 1_000.0)
    is DomainValue.Absent -> "indisponível"
}

private fun Double?.toDistanceLabel(): String =
    if (this == null) "indisponível" else String.format(Locale.forLanguageTag("pt-BR"), "%.2f km", this / 1_000.0)

private fun Double?.toStepLengthLabel(): String =
    if (this == null) "indisponível" else String.format(Locale.forLanguageTag("pt-BR"), "%.3f m", this)

private fun Double?.toPaceLabel(): String {
    val seconds = this ?: return "indisponível"
    val totalSeconds = seconds.roundToInt()
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return String.format(Locale.forLanguageTag("pt-BR"), "%d:%02d min/km", minutes, remainingSeconds)
}

private fun Double?.toSpeedLabel(): String =
    if (this == null) "indisponível" else String.format(Locale.forLanguageTag("pt-BR"), "%.1f km/h", this * 3.6)

private fun WorkoutDistanceMetrics.toImportedDistanceLabel(): String = when {
    originalMeters is DomainValue.Present -> originalMeters.toDistanceLabel()
    estimatedMeters is DomainValue.Present -> estimatedMeters.toDistanceLabel()
    else -> "indisponível"
}

private fun WorkoutActivity.displayAveragePaceLabel(): String =
    if (isCycling) {
        "indisponível"
    } else {
        (metrics.calibratedMetrics?.averagePaceSecondsPerKilometer?.valueOrNull()
            ?: metrics.averagePaceSecondsPerKilometer.valueOrNull()).toPaceLabel()
    }

private fun WorkoutActivity.displayAverageSpeedLabel(): String =
    if (isCycling) {
        cyclingAverageSpeedMetersPerSecond().toSpeedLabel()
    } else {
        (metrics.calibratedMetrics?.averageSpeedMetersPerSecond?.valueOrNull()
            ?: metrics.averageSpeedMetersPerSecond.valueOrNull()).toSpeedLabel()
    }

private fun <T> DomainValue<T>.valueOrNull(): T? = (this as? DomainValue.Present)?.value

private fun chartAxisPadding(points: List<ChampionChartPoint>): Double =
    points
        .map { it.x }
        .distinct()
        .sorted()
        .zipWithNext()
        .map { (first, second) -> second - first }
        .filter { it > 0.0 }
        .minOrNull()
        ?.coerceAtLeast(1.0)
        ?: 1.0

private fun WorkoutActivityFilter.toSectionTitle(): String = when (this) {
    WorkoutActivityFilter.ALL -> "Todos os treinos"
    WorkoutActivityFilter.PENDING_REVIEW -> "Treinos aguardando revisão"
    WorkoutActivityFilter.PENDING_CALIBRATION -> "Treinos aguardando calibração"
    WorkoutActivityFilter.READY_TO_SYNC -> "Treinos prontos"
    WorkoutActivityFilter.SYNCED -> "Treinos sincronizados"
    WorkoutActivityFilter.SYNC_FAILED -> "Treinos com erro"
    WorkoutActivityFilter.DISCARDED -> "Treinos descartados"
}

private fun WorkoutActivity.displayTitle(): String =
    title?.takeIf { it.isNotBlank() } ?: displayType().toFallbackTitle()

private fun WorkoutActivity.displayListTitle(): String =
    title
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        ?.takeIf { it.isNotBlank() }
        ?: displayType().toFallbackTitle()

private fun WorkoutActivity.displayListContextLabel(): String =
    if (title?.trim()?.isNotEmpty() == true) {
        "${displayType().toLabel()} • ${metrics.startedAt.formatDateTime()}"
    } else {
        metrics.startedAt.formatDateTime()
    }

private fun WorkoutActivity.displayType(): WorkoutType = confirmedType ?: suggestedType

private fun WorkoutType.toFallbackTitle(): String = when (this) {
    WorkoutType.WALKING -> "Caminhada"
    WorkoutType.RUNNING -> "Corrida"
    WorkoutType.FREE_WORKOUT -> "Treino livre"
    WorkoutType.TREADMILL -> "Esteira"
    WorkoutType.CYCLING -> "Bicicleta"
    WorkoutType.UNKNOWN -> "Treino do relógio"
}

private fun ActivityWorkflowState.toLabel(): String = when (this) {
    ActivityWorkflowState.PENDING_REVIEW -> "Aguardando revisão"
    ActivityWorkflowState.PENDING_CALIBRATION -> "Aguardando calibração"
    ActivityWorkflowState.READY_TO_SYNC -> "Pronta para sincronizar"
    ActivityWorkflowState.SYNCING -> "Sincronizando"
    ActivityWorkflowState.SYNCED -> "Sincronizada"
    ActivityWorkflowState.SYNC_FAILED -> "Com erro"
    ActivityWorkflowState.DISCARDED -> "Descartada"
}

private fun ActivityWorkflowState.toChipLabel(): String = when (this) {
    ActivityWorkflowState.READY_TO_SYNC -> "Pronta p/ sincronizar"
    else -> toLabel()
}

@Composable
private fun ActivityWorkflowState.toListAccentColor(): Color = when (this) {
    ActivityWorkflowState.PENDING_REVIEW,
    -> exerciseSecondaryAccent()

    ActivityWorkflowState.PENDING_CALIBRATION -> exerciseTertiaryAccent()

    ActivityWorkflowState.READY_TO_SYNC,
    ActivityWorkflowState.SYNCING,
    -> exercisePrimaryAccent()

    ActivityWorkflowState.SYNC_FAILED -> MaterialTheme.colorScheme.error
    ActivityWorkflowState.DISCARDED -> MaterialTheme.colorScheme.onSurfaceVariant
    ActivityWorkflowState.SYNCED -> exercisePrimaryAccent()
}

@Composable
private fun ActivityWorkflowState.toExerciseCalendarColor(): Color = when (this) {
    ActivityWorkflowState.PENDING_REVIEW -> exerciseSecondaryAccent()
    ActivityWorkflowState.PENDING_CALIBRATION -> exerciseTertiaryAccent()
    ActivityWorkflowState.READY_TO_SYNC,
    ActivityWorkflowState.SYNCING,
    ActivityWorkflowState.SYNCED,
    -> exercisePrimaryAccent()
    ActivityWorkflowState.SYNC_FAILED -> MaterialTheme.colorScheme.error
    ActivityWorkflowState.DISCARDED -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun exercisePrimaryAccent(): Color = if (isSystemInDarkTheme()) {
    DarkColorScheme.primary
} else {
    LightColorScheme.primary
}

@Composable
private fun exerciseSecondaryAccent(): Color = if (isSystemInDarkTheme()) {
    DarkColorScheme.secondary
} else {
    LightColorScheme.secondary
}

@Composable
private fun exerciseTertiaryAccent(): Color = if (isSystemInDarkTheme()) {
    DarkColorScheme.tertiary
} else {
    LightColorScheme.tertiary
}

@Composable
private fun exerciseMetricAccent(index: Int): Color = when (index % 3) {
    0 -> exercisePrimaryAccent()
    1 -> exerciseSecondaryAccent()
    else -> exerciseTertiaryAccent()
}

private fun WorkoutType.toListIcon(): ImageVector = when (this) {
    WorkoutType.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
    WorkoutType.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
    WorkoutType.FREE_WORKOUT -> Icons.Filled.FitnessCenter
    WorkoutType.TREADMILL -> Icons.AutoMirrored.Filled.DirectionsWalk
    WorkoutType.CYCLING -> Icons.AutoMirrored.Filled.DirectionsBike
    WorkoutType.UNKNOWN -> Icons.Filled.FitnessCenter
}

private fun ParserValidity.toLabel(): String = when (this) {
    ParserValidity.VALID -> "Válido"
    ParserValidity.PARTIAL -> "Parcial"
    ParserValidity.INVALID -> "Inválido"
}

private fun WorkoutDistanceMetrics.toDistanceLabel(): String {
    val finalMeters = finalDistanceMetersOrNull() ?: return "indisponível"

    val kilometers = finalMeters / 1_000.0
    return String.format(Locale.forLanguageTag("pt-BR"), "%.2f km", kilometers)
}

private fun WorkoutDistanceMetrics.finalDistanceMetersOrNull(): Double? = when {
    calibratedMeters is DomainValue.Present -> calibratedMeters.value
    originalMeters is DomainValue.Present -> originalMeters.value
    estimatedMeters is DomainValue.Present -> estimatedMeters.value
    else -> null
}

private fun WatchWorkoutImportSurfaceState.sessionOrNull(): WatchWorkoutImportSessionState? = when (this) {
    WatchWorkoutImportSurfaceState.Idle -> null
    is WatchWorkoutImportSurfaceState.Preparing -> session
    is WatchWorkoutImportSurfaceState.Connecting -> session
    is WatchWorkoutImportSurfaceState.ReadingIndex -> session
    is WatchWorkoutImportSurfaceState.ReadingBlocks -> session
    is WatchWorkoutImportSurfaceState.Parsing -> session
    is WatchWorkoutImportSurfaceState.Persisting -> session
    is WatchWorkoutImportSurfaceState.Completed -> session
    is WatchWorkoutImportSurfaceState.Partial -> session
    is WatchWorkoutImportSurfaceState.Empty -> session
    is WatchWorkoutImportSurfaceState.Failed -> session
    is WatchWorkoutImportSurfaceState.Cancelled -> session
}

private fun WatchWorkoutImportSurfaceState.isImportInFlight(): Boolean = when (this) {
    WatchWorkoutImportSurfaceState.Idle,
    is WatchWorkoutImportSurfaceState.Completed,
    is WatchWorkoutImportSurfaceState.Partial,
    is WatchWorkoutImportSurfaceState.Empty,
    is WatchWorkoutImportSurfaceState.Failed,
    is WatchWorkoutImportSurfaceState.Cancelled,
    -> false

    is WatchWorkoutImportSurfaceState.Preparing,
    is WatchWorkoutImportSurfaceState.Connecting,
    is WatchWorkoutImportSurfaceState.ReadingIndex,
    is WatchWorkoutImportSurfaceState.ReadingBlocks,
    is WatchWorkoutImportSurfaceState.Parsing,
    is WatchWorkoutImportSurfaceState.Persisting,
    -> true
}

private fun WatchWorkoutImportProgress.toFriendlyMessage(state: WatchWorkoutImportSurfaceState): String = when (state) {
    is WatchWorkoutImportSurfaceState.Preparing -> "Preparando importação dos treinos do relógio"
    is WatchWorkoutImportSurfaceState.Connecting -> "Conectando ao relógio associado"
    is WatchWorkoutImportSurfaceState.ReadingIndex -> "Lendo índice D3 dos treinos no relógio"
    is WatchWorkoutImportSurfaceState.ReadingBlocks -> {
        val current = currentBlockNumber ?: 0
        val total = discoveredBlockCount.takeIf { it > 0 } ?: 0
        if (total > 0 && current > 0) {
            "Baixando treino $current de $total no relógio"
        } else {
            "Baixando treinos do relógio"
        }
    }
    is WatchWorkoutImportSurfaceState.Parsing -> "Interpretando treino recebido do relógio"
    is WatchWorkoutImportSurfaceState.Persisting -> "Salvando treino importado no celular"
    else -> "Importação em andamento"
}

private fun WatchWorkoutImportProgress.toTechnicalLine(): String {
    val discovered = discoveredBlockCount.takeIf { it > 0 }?.toString() ?: "?"
    val processed = processedBlockCount.toString()
    val packetPart = if (currentPacketIndex != null && totalPacketsInBlock != null) {
        " Pacote $currentPacketIndex de $totalPacketsInBlock."
    } else {
        ""
    }
    return "Blocos processados $processed de $discovered.$packetPart"
}

private fun WatchWorkoutImportResult.toTitle(): String = when (this) {
    is WatchWorkoutImportResult.Completed -> "Treinos importados"
    is WatchWorkoutImportResult.Partial -> "Importação parcial concluída"
    is WatchWorkoutImportResult.Empty -> "Nenhum treino novo encontrado"
    is WatchWorkoutImportResult.Cancelled -> "Importação cancelada"
    is WatchWorkoutImportResult.Failed -> "Importação encerrada com falha"
}

private fun WatchWorkoutImportResult.toAccessibleTitle(): String = when (this) {
    is WatchWorkoutImportResult.Completed -> "Treino importado"
    is WatchWorkoutImportResult.Partial -> "Importação parcial concluída"
    is WatchWorkoutImportResult.Empty -> "Nenhum treino novo encontrado"
    is WatchWorkoutImportResult.Cancelled -> "Importação cancelada"
    is WatchWorkoutImportResult.Failed -> "Não foi possível importar os treinos"
}

private fun WatchWorkoutImportResult.toMessage(): String = when (this) {
    is WatchWorkoutImportResult.Completed -> "Os treinos importados já estão disponíveis abaixo como pendentes de revisão."
    is WatchWorkoutImportResult.Partial -> "Parte dos treinos foi salva com segurança. Uma nova tentativa poderá retomar o restante."
    is WatchWorkoutImportResult.Empty -> "Não há treino novo elegível no relógio neste momento."
    is WatchWorkoutImportResult.Cancelled -> "A importação foi interrompida, preservando os blocos já persistidos."
    is WatchWorkoutImportResult.Failed -> "A importação não pôde ser concluída com segurança."
}

private fun br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportSummary.toSummaryLine(): String =
    "Blocos $processedBlockCount de $discoveredBlockCount, inseridos $insertedCount, parciais $partialCount, inválidos $invalidCount, pulados $skippedValidCount."

private fun WatchWorkoutImportStatus.toLabel(): String = when (this) {
    WatchWorkoutImportStatus.COMPLETED -> "Concluída"
    WatchWorkoutImportStatus.PARTIAL -> "Parcial"
    WatchWorkoutImportStatus.EMPTY -> "Sem novos"
    WatchWorkoutImportStatus.CANCELLED -> "Cancelada"
    WatchWorkoutImportStatus.FAILED -> "Falha"
}

private fun Boolean.toTimeoutLabel(): String = if (this) "Timeout" else "Sem timeout"

private fun WorkoutType.toLabel(): String = when (this) {
    WorkoutType.WALKING -> "Caminhada"
    WorkoutType.RUNNING -> "Corrida"
    WorkoutType.FREE_WORKOUT -> "Treino livre"
    WorkoutType.TREADMILL -> "Esteira"
    WorkoutType.CYCLING -> "Bicicleta"
    WorkoutType.UNKNOWN -> "Tipo indefinido"
}

private fun Instant.formatDateTime(): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.forLanguageTag("pt-BR"))
        .withZone(ZoneId.systemDefault())
        .format(this)

private fun Instant.formatTime(): String =
    DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("pt-BR"))
        .withZone(ZoneId.systemDefault())
        .format(this)

private fun Instant.formatChartTime(): String =
    DateTimeFormatter.ofPattern("HH:mm:ss", Locale.forLanguageTag("pt-BR"))
        .withZone(ZoneId.systemDefault())
        .format(this)

private fun Instant.formatChartAxisTime(): String =
    DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("pt-BR"))
        .withZone(ZoneId.systemDefault())
        .format(this)

private fun Duration.toDurationLabel(): String {
    val negative = isNegative
    val totalSeconds = if (seconds == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(seconds)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    val label = if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
    return if (negative) "-$label" else label
}
