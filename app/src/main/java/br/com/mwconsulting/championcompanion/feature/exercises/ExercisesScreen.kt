package br.com.mwconsulting.championcompanion.feature.exercises

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeLarge
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeMedium
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.PendingReviewBanner
import br.com.mwconsulting.championcompanion.core.designsystem.PrimaryActionButton
import br.com.mwconsulting.championcompanion.core.model.ActivityWorkflowState
import br.com.mwconsulting.championcompanion.core.model.DomainValue
import br.com.mwconsulting.championcompanion.core.model.ParserValidity
import br.com.mwconsulting.championcompanion.core.model.PhoneWorkoutPolicy
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibrationSnapshot
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportMetadata
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportProgress
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportResult
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportStatus
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityFilter
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import br.com.mwconsulting.championcompanion.core.model.WorkoutDistanceMetrics
import br.com.mwconsulting.championcompanion.core.model.WorkoutHeartRateSample
import br.com.mwconsulting.championcompanion.core.model.WorkoutInterval
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

private val ExerciseHeartRateGuideBands = listOf(
    166f to Color(0xFFE57373),
    135f to Color(0xFFFFB74D),
    110f to Color(0xFF66BB6A),
    71f to Color(0xFF4FC3F7),
    30f to Color(0xFF29B6F6),
)

private val ExerciseHeartRateChartLeftPadding = 12.dp
private val ExerciseHeartRateChartRightPadding = 40.dp

private val PendingWorkflowBorderColor = Color(0xFFF9A825)
private val ReadyWorkflowBorderColor = Color(0xFF1E88E5)
private val SyncedWorkflowBorderColor = Color(0xFF2E7D32)

@Composable
fun ExercisesScreen(
    openedActivityId: WorkoutActivityId? = null,
    onNavigateBack: (() -> Unit)? = null,
    onOpenActivity: (WorkoutActivityId) -> Unit = {},
    viewModel: ExercisesViewModel = run {
        val activity = checkNotNull(LocalActivity.current) as ComponentActivity
        remember(activity) {
            ViewModelProvider(activity)[ExercisesViewModel::class.java]
        }
    },
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()
    val dismissOperationError = remember(viewModel) {
        { viewModel.onIntent(ExercisesIntent.DismissOperationError) }
    }

    LaunchedEffect(viewModel) {
        viewModel.onIntent(ExercisesIntent.Load)
    }

    LaunchedEffect(openedActivityId) {
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
                .verticalScroll(rememberScrollState())
                .padding(ChampionSpacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
        ) {
            if (openedActivityId == null) {
                ExercisesHeader(state = state)
                ExercisesImportCard(
                    state = state,
                    onStartImport = { viewModel.onIntent(ExercisesIntent.StartImport) },
                    onCancelImport = { viewModel.onIntent(ExercisesIntent.CancelImport) },
                    onRetryImport = { viewModel.onIntent(ExercisesIntent.RetryImport) },
                    onDismissImport = { viewModel.onIntent(ExercisesIntent.DismissImport) },
                )
                WorkflowSummarySection(state = state)
                if (!state.importState.isImportInFlight()) {
                    ExerciseSyncFeedbackCard(state.activityList, state.workflowSummary)
                }
                WorkoutFiltersSection(
                    selectedFilter = state.selectedFilter,
                    workflowSummary = state.workflowSummary,
                    onSelectFilter = { viewModel.onIntent(ExercisesIntent.SelectFilter(it)) },
                )
                ExercisesListSection(
                    selectedFilter = state.selectedFilter,
                    activities = state.activityList,
                    onOpenActivity = onOpenActivity,
                )
            } else {
                ExerciseDetailShell(
                    state = state,
                    activityId = openedActivityId,
                    onNavigateBack = onNavigateBack,
                    onDismissError = dismissOperationError,
                    onReviewTypeChange = { viewModel.onIntent(ExercisesIntent.EditReviewType(it)) },
                    onReviewTitleChange = { viewModel.onIntent(ExercisesIntent.EditReviewTitle(it)) },
                    onReviewNotesChange = { viewModel.onIntent(ExercisesIntent.EditReviewNotes(it)) },
                    onSubmitReview = { viewModel.onIntent(ExercisesIntent.SubmitReview) },
                    onCalibrationDistanceChange = { viewModel.onIntent(ExercisesIntent.EditCalibrationDistance(it)) },
                    onSubmitCalibration = { viewModel.onIntent(ExercisesIntent.SubmitCalibration) },
                    onSyncActivity = { viewModel.onIntent(ExercisesIntent.SyncSelectedActivity) },
                    onDiscardActivity = { viewModel.onIntent(ExercisesIntent.DiscardSelectedActivity) },
                )
            }
        }
    }
}

@Composable
private fun ExercisesHeader(state: ExercisesUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = "Exercícios",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = PhoneWorkoutPolicy.DISCONTINUED_MESSAGE,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (state.hasAssociatedDevice) {
                    "Relógio associado: ${state.associatedDeviceName ?: "Champion C001"}"
                } else {
                    "Nenhum relógio associado. A importação manual ficará disponível após associar um dispositivo."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text("Treinos do relógio", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "A leitura só começa por ação explícita e mantém os treinos importados como pendentes de revisão.",
                style = MaterialTheme.typography.bodyMedium,
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
                Text("Última importação", style = MaterialTheme.typography.titleMedium)
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
private fun WorkflowSummarySection(state: ExercisesUiState) {
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
            Text("Resumo do fluxo", style = MaterialTheme.typography.titleLarge)
            Text(
                text = buildString {
                    append(state.workflowSummary.pendingReviewCount)
                    append(" aguardando revisão, ")
                    append(state.workflowSummary.pendingCalibrationCount)
                    append(" aguardando calibração e ")
                    append(state.workflowSummary.readyToSyncCount)
                    append(" pronta(s) para sincronizar.")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
            ) {
                SummaryChip("Todas", workoutFilterOptions(state.workflowSummary).first().count)
                SummaryChip("Pendentes", state.workflowSummary.pendingReviewCount)
                SummaryChip("Calibração", state.workflowSummary.pendingCalibrationCount)
                SummaryChip("Prontas", state.workflowSummary.readyToSyncCount)
                SummaryChip("Sincronizadas", state.workflowSummary.syncedCount)
                SummaryChip("Com erro", state.workflowSummary.syncFailedCount)
                SummaryChip("Descartadas", state.workflowSummary.discardedCount)
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, count: Int) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text("$label: $count") },
    )
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

@Composable
private fun ExercisesListSection(
    selectedFilter: WorkoutActivityFilter,
    activities: List<WorkoutActivity>,
    onOpenActivity: (WorkoutActivityId) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        Text(selectedFilter.toSectionTitle(), style = MaterialTheme.typography.titleLarge)
        if (activities.isEmpty()) {
            EmptyState(
                title = "Nenhum treino neste filtro",
                message = "Troque o filtro ou importe novos treinos do relógio para preencher esta lista.",
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            activities.forEach { activity ->
                WorkoutActivityCard(
                    activity = activity,
                    onClick = { onOpenActivity(activity.id) },
                )
            }
        }
    }
}

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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Health Connect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    syncingCount > 0 -> "Há treino(s) sincronizando com o Health Connect. Os dados locais continuam disponíveis normalmente."
                    workflowSummary.syncFailedCount > 0 -> "Alguns treinos ficaram com erro ou falha parcial no Health Connect. Os detalhes mostram retry seguro por atividade."
                    workflowSummary.readyToSyncCount > 0 -> "Existem treinos prontos para sincronização explícita com o Health Connect."
                    workflowSummary.syncedCount > 0 -> "Treinos já sincronizados continuam visíveis localmente sem depender do provider."
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
    val borderColor = activity.workflowState.toListBorderColor()
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 2.dp, color = borderColor, shape = ChampionShapeMedium),
        shape = ChampionShapeMedium,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = activity.displayType().toListIcon(),
                    contentDescription = activity.displayType().toLabel(),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = activity.displayType().toLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = activity.displayTitle(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = activity.metrics.startedAt.formatDateTime(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Duração ${activity.metrics.duration.toDurationLabel()} • Distância ${activity.metrics.distances.toDistanceLabel()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = activity.workflowState.toLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = borderColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ExerciseDetailShell(
    state: ExercisesUiState,
    activityId: WorkoutActivityId,
    onNavigateBack: (() -> Unit)?,
    onDismissError: () -> Unit,
    onReviewTypeChange: (WorkoutType) -> Unit,
    onReviewTitleChange: (String) -> Unit,
    onReviewNotesChange: (String) -> Unit,
    onSubmitReview: () -> Unit,
    onCalibrationDistanceChange: (String) -> Unit,
    onSubmitCalibration: () -> Unit,
    onSyncActivity: () -> Unit,
    onDiscardActivity: () -> Unit,
) {
    var showDiscardConfirmation by remember(activityId) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                ) {
                    Text("Detalhe do treino", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
            }

            when (val detailState = state.detailState) {
                ExerciseDetailState.Loading -> Text("Carregando atividade $activityId", style = MaterialTheme.typography.bodyMedium)
                ExerciseDetailState.NotFound -> ErrorState(
                    title = "Treino não encontrado",
                    message = "Nenhuma atividade importada foi encontrada para este identificador.",
                    modifier = Modifier.fillMaxWidth(),
                )

                is ExerciseDetailState.Failed -> ErrorState(
                    title = "Não foi possível abrir o treino",
                    message = detailState.error.message,
                    modifier = Modifier.fillMaxWidth(),
                )

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
                            operationError = state.operationError,
                            canDiscardSelectedActivity = state.canDiscardSelectedActivity,
                            onDismissError = onDismissError,
                            onReviewTypeChange = onReviewTypeChange,
                            onReviewTitleChange = onReviewTitleChange,
                            onReviewNotesChange = onReviewNotesChange,
                            onSubmitReview = onSubmitReview,
                            onCalibrationDistanceChange = onCalibrationDistanceChange,
                            onSubmitCalibration = onSubmitCalibration,
                            onSyncActivity = onSyncActivity,
                            onRequestDiscard = { showDiscardConfirmation = true },
                        )
                    }
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
}

@Composable
private fun WorkoutActivityDetailContent(
    activity: WorkoutActivity,
    latestCalibration: TreadmillCalibrationSnapshot?,
    reviewFormState: ExerciseReviewFormState,
    calibrationFormState: ExerciseCalibrationFormState,
    operationError: ExerciseOperationError?,
    canDiscardSelectedActivity: Boolean,
    onDismissError: () -> Unit,
    onReviewTypeChange: (WorkoutType) -> Unit,
    onReviewTitleChange: (String) -> Unit,
    onReviewNotesChange: (String) -> Unit,
    onSubmitReview: () -> Unit,
    onCalibrationDistanceChange: (String) -> Unit,
    onSubmitCalibration: () -> Unit,
    onSyncActivity: () -> Unit,
    onRequestDiscard: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        ActivityHeaderCard(activity)
        if (operationError != null) {
            ErrorState(
                title = "Ação indisponível",
                message = operationError.message,
                actionLabel = "Fechar",
                onAction = onDismissError,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        ActivityMetricsSection(activity, latestCalibration)
        ActivityHeartRateChartSection(activity)
        ActivityIntervalsSection(activity)
        if (activity.parserValidity == ParserValidity.PARTIAL) {
            PartialParserNotice()
        }
        when {
            activity.workflowState == ActivityWorkflowState.PENDING_REVIEW -> ReviewFormSection(
                activity = activity,
                formState = reviewFormState,
                onReviewTypeChange = onReviewTypeChange,
                onReviewTitleChange = onReviewTitleChange,
                onReviewNotesChange = onReviewNotesChange,
                onSubmitReview = onSubmitReview,
            )

            activity.workflowState == ActivityWorkflowState.PENDING_CALIBRATION -> CalibrationFormSection(
                activity = activity,
                latestCalibration = latestCalibration,
                formState = calibrationFormState,
                onCalibrationDistanceChange = onCalibrationDistanceChange,
                onSubmitCalibration = onSubmitCalibration,
            )

            activity.workflowState in setOf(
                ActivityWorkflowState.READY_TO_SYNC,
                ActivityWorkflowState.SYNCING,
                ActivityWorkflowState.SYNCED,
                ActivityWorkflowState.SYNC_FAILED,
                ActivityWorkflowState.DISCARDED,
            ) -> ReadOnlyWorkflowSection(activity, latestCalibration, onSyncActivity)
        }
        if (canDiscardSelectedActivity) {
            TextButton(onClick = onRequestDiscard, modifier = Modifier.align(Alignment.End)) {
                Text("Descartar treino")
            }
        }
    }
}

@Composable
private fun ActivityHeaderCard(activity: WorkoutActivity) {
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
            Text(activity.displayTitle(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${activity.metrics.startedAt.formatDateTime()} • ${activity.metrics.duration.toDurationLabel()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
            ) {
                AssistChip(onClick = {}, enabled = false, label = { Text(activity.workflowState.toChipLabel()) })
                AssistChip(onClick = {}, enabled = false, label = { Text(activity.displayType().toLabel()) })
                AssistChip(onClick = {}, enabled = false, label = { Text(activity.parserValidity.toLabel()) })
                AssistChip(onClick = {}, enabled = false, label = { Text("Origem: relógio") })
            }
        }
    }
}

@Composable
private fun ActivityMetricsSection(
    activity: WorkoutActivity,
    latestCalibration: TreadmillCalibrationSnapshot?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        Text("Resumo e métricas", style = MaterialTheme.typography.titleLarge)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            activity.detailMetricItems().forEach { item ->
                MetricCard(item.label, item.value)
            }
        }
        activity.confirmedType?.let {
            Text(
                text = "Tipo confirmado: ${it.toLabel()} • Tipo sugerido: ${activity.suggestedType.toLabel()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        latestCalibration?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ChampionShapeMedium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(ChampionSpacing.large),
                    verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                ) {
                    Text("Calibração ativa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Revisão ${it.revision} • Aplicada em ${it.appliedAt.formatDateTime()}", style = MaterialTheme.typography.bodyMedium)
                    Text("Distância informada: ${it.reportedDistanceMeters.toDistanceLabel()}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeMedium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ActivityHeartRateChartSection(activity: WorkoutActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text("Frequência cardíaca", style = MaterialTheme.typography.titleLarge)
            if (activity.heartRateSamples.isEmpty()) {
                Text(
                    text = "Sem amostras reais de BPM disponíveis para este treino.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                WorkoutHeartRateChart(activity.heartRateSamples)
                Text(
                    text = heartRateAccessibleSummary(activity.heartRateSamples),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WorkoutHeartRateChart(samples: List<WorkoutHeartRateSample>) {
    val minBpm = (samples.minOf { it.bpm }.toFloat().coerceAtMost(60f)).coerceAtMost(ExerciseHeartRateGuideBands.last().first)
    val maxBpm = (samples.maxOf { it.bpm }.toFloat().coerceAtLeast(180f)).coerceAtLeast(ExerciseHeartRateGuideBands.first().first)
    val range = (maxBpm - minBpm).takeIf { it > 0f } ?: 1f
    val chartDescription = samples.joinToString(separator = "; ") { "${it.bpm} bpm em ${it.sampledAt.formatTime()}" }
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .semantics {
                    contentDescription = "Gráfico de frequência cardíaca do treino. $chartDescription"
                },
        ) {
            val leftPadding = ExerciseHeartRateChartLeftPadding.toPx()
            val topPadding = 14.dp.toPx()
            val rightPadding = ExerciseHeartRateChartRightPadding.toPx()
            val bottomPadding = 22.dp.toPx()
            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding
            val first = samples.first().sampledAt
            val durationMillis = (samples.last().sampledAt.toEpochMilli() - first.toEpochMilli()).coerceAtLeast(1L)
            val path = Path()
            ExerciseHeartRateGuideBands.forEach { (value, bandColor) ->
                val y = yForExerciseChartValue(value, minBpm, range, chartHeight, topPadding, size.height - bottomPadding)
                drawDottedHorizontalLine(
                    color = bandColor,
                    startX = leftPadding,
                    endX = size.width - rightPadding,
                    y = y,
                    radius = 2.dp.toPx(),
                    gap = 10.dp.toPx(),
                )
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        value.roundToInt().toString(),
                        size.width - rightPadding + 8.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(
                                255,
                                (onSurfaceVariant.red * 255).roundToInt(),
                                (onSurfaceVariant.green * 255).roundToInt(),
                                (onSurfaceVariant.blue * 255).roundToInt(),
                            )
                            textSize = 12.dp.toPx()
                            isAntiAlias = true
                        },
                    )
                }
            }
            samples.forEachIndexed { index, sample ->
                val elapsed = sample.sampledAt.toEpochMilli() - first.toEpochMilli()
                val x = leftPadding + (elapsed.toFloat() / durationMillis.toFloat()) * chartWidth
                val y = yForExerciseChartValue(sample.bpm.toFloat(), minBpm, range, chartHeight, topPadding, size.height - bottomPadding)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx()),
            )
            samples.forEach { sample ->
                val elapsed = sample.sampledAt.toEpochMilli() - first.toEpochMilli()
                val x = leftPadding + (elapsed.toFloat() / durationMillis.toFloat()) * chartWidth
                val y = yForExerciseChartValue(sample.bpm.toFloat(), minBpm, range, chartHeight, topPadding, size.height - bottomPadding)
                drawCircle(
                    color = surfaceVariant,
                    radius = 4.5.dp.toPx(),
                    center = Offset(x, y),
                    style = Stroke(width = 0.8.dp.toPx()),
                )
                drawCircle(
                    color = primaryColor,
                    radius = 2.5.dp.toPx(),
                    center = Offset(x, y),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .absolutePadding(
                    left = ExerciseHeartRateChartLeftPadding,
                    right = ExerciseHeartRateChartRightPadding,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(samples.first().sampledAt.formatTime(), style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)
            Text(samples.last().sampledAt.formatTime(), style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivityIntervalsSection(activity: WorkoutActivity) {
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
private fun PartialParserNotice() {
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
                text = "Treino importado com dados parciais, mas elegível para revisão, calibração quando necessária e sincronização futura, alinhado ao comportamento anterior do app.",
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
    onReviewTypeChange: (WorkoutType) -> Unit,
    onReviewTitleChange: (String) -> Unit,
    onReviewNotesChange: (String) -> Unit,
    onSubmitReview: () -> Unit,
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
            WorkoutType.entries.filter { it != WorkoutType.UNKNOWN }.forEach { type ->
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
            formState.error?.let {
                Text(it.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            PrimaryActionButton(
                label = if (formState.isSaving) "Salvando revisão" else reviewActionLabel(formState.selectedType),
                onClick = onSubmitReview,
                enabled = formState.selectedType != null,
                isLoading = formState.isSaving,
                modifier = Modifier.fillMaxWidth(),
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
            MetricCard("Base importada", activity.metrics.distances.toImportedDistanceLabel())
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
                MetricCard("Prévia distância", formState.previewDistanceMeters.toDistanceLabel())
                MetricCard("Prévia ritmo", formState.previewAveragePaceSecondsPerKilometer.toPaceLabel())
                MetricCard("Prévia velocidade", formState.previewAverageSpeedMetersPerSecond.toSpeedLabel())
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

@Composable
private fun ReadOnlyWorkflowSection(
    activity: WorkoutActivity,
    latestCalibration: TreadmillCalibrationSnapshot?,
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
                    ActivityWorkflowState.SYNCED -> "Treino sincronizado com sucesso no Health Connect."
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
                Text("Calibração ativa: revisão ${it.revision}", style = MaterialTheme.typography.bodyMedium)
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

private fun reviewActionLabel(type: WorkoutType?): String =
    if (type == WorkoutType.TREADMILL) "Salvar revisão e ir para calibração" else "Concluir revisão"

private data class DetailMetricItem(
    val label: String,
    val value: String,
)

private fun WorkoutActivity.detailMetricItems(): List<DetailMetricItem> {
    val common = mutableListOf(
        DetailMetricItem("Distância final", metrics.distances.toDistanceLabel()),
        DetailMetricItem("Calorias", metrics.caloriesKilocalories.toKcalLabel()),
        DetailMetricItem("BPM médio", metrics.averageBpm.toBpmLabel()),
        DetailMetricItem("BPM mínimo", metrics.minimumBpm.toBpmLabel()),
        DetailMetricItem("BPM máximo", metrics.maximumBpm.toBpmLabel()),
    )

    return if (displayType() == WorkoutType.FREE_WORKOUT) {
        common
    } else {
        listOf(
            DetailMetricItem("Distância importada", metrics.distances.toImportedDistanceLabel()),
            DetailMetricItem("Passos", numberLabel(metrics.steps)),
            DetailMetricItem("Ritmo médio", displayAveragePaceLabel()),
            DetailMetricItem("Velocidade média", displayAverageSpeedLabel()),
        ) + common
    }
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
    (metrics.calibratedMetrics?.averagePaceSecondsPerKilometer?.valueOrNull()
        ?: metrics.averagePaceSecondsPerKilometer.valueOrNull()).toPaceLabel()

private fun WorkoutActivity.displayAverageSpeedLabel(): String =
    (metrics.calibratedMetrics?.averageSpeedMetersPerSecond?.valueOrNull()
        ?: metrics.averageSpeedMetersPerSecond.valueOrNull()).toSpeedLabel()

private fun <T> DomainValue<T>.valueOrNull(): T? = (this as? DomainValue.Present)?.value

private fun DrawScope.drawDottedHorizontalLine(
    color: Color,
    startX: Float,
    endX: Float,
    y: Float,
    radius: Float,
    gap: Float,
) {
    var x = startX
    while (x <= endX) {
        drawCircle(color = color, radius = radius, center = Offset(x, y))
        x += gap
    }
}

private fun yForExerciseChartValue(
    value: Float,
    minValue: Float,
    range: Float,
    chartHeight: Float,
    topPadding: Float,
    bottom: Float,
): Float {
    val normalized = ((value - minValue) / range).coerceIn(0f, 1f)
    return bottom - (normalized * chartHeight).coerceIn(0f, chartHeight) + topPadding - topPadding
}

private fun WorkoutActivityFilter.toSectionTitle(): String = when (this) {
    WorkoutActivityFilter.ALL -> "Todos os treinos importados"
    WorkoutActivityFilter.PENDING_REVIEW -> "Treinos aguardando revisão"
    WorkoutActivityFilter.PENDING_CALIBRATION -> "Treinos aguardando calibração"
    WorkoutActivityFilter.READY_TO_SYNC -> "Treinos prontos"
    WorkoutActivityFilter.SYNCED -> "Treinos sincronizados"
    WorkoutActivityFilter.SYNC_FAILED -> "Treinos com erro"
    WorkoutActivityFilter.DISCARDED -> "Treinos descartados"
}

private fun WorkoutActivity.displayTitle(): String =
    title?.takeIf { it.isNotBlank() } ?: displayType().toFallbackTitle()

private fun WorkoutActivity.displayType(): WorkoutType = confirmedType ?: suggestedType

private fun WorkoutType.toFallbackTitle(): String = when (this) {
    WorkoutType.WALKING -> "Caminhada"
    WorkoutType.RUNNING -> "Corrida"
    WorkoutType.FREE_WORKOUT -> "Treino livre"
    WorkoutType.TREADMILL -> "Esteira"
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

private fun ActivityWorkflowState.toListBorderColor(): Color = when (this) {
    ActivityWorkflowState.PENDING_REVIEW,
    ActivityWorkflowState.PENDING_CALIBRATION,
    ActivityWorkflowState.DISCARDED,
    -> PendingWorkflowBorderColor

    ActivityWorkflowState.READY_TO_SYNC,
    ActivityWorkflowState.SYNCING,
    ActivityWorkflowState.SYNC_FAILED,
    -> ReadyWorkflowBorderColor

    ActivityWorkflowState.SYNCED -> SyncedWorkflowBorderColor
}

private fun WorkoutType.toListIcon(): ImageVector = when (this) {
    WorkoutType.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
    WorkoutType.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
    WorkoutType.FREE_WORKOUT -> Icons.Filled.FitnessCenter
    WorkoutType.TREADMILL -> Icons.AutoMirrored.Filled.DirectionsWalk
    WorkoutType.UNKNOWN -> Icons.Filled.FitnessCenter
}

private fun ParserValidity.toLabel(): String = when (this) {
    ParserValidity.VALID -> "Válido"
    ParserValidity.PARTIAL -> "Parcial"
    ParserValidity.INVALID -> "Inválido"
}

private fun WorkoutDistanceMetrics.toDistanceLabel(): String {
    val finalMeters = when {
        calibratedMeters is DomainValue.Present -> calibratedMeters.value
        originalMeters is DomainValue.Present -> originalMeters.value
        estimatedMeters is DomainValue.Present -> estimatedMeters.value
        else -> null
    } ?: return "indisponível"

    val kilometers = finalMeters / 1_000.0
    return String.format(Locale.forLanguageTag("pt-BR"), "%.2f km", kilometers)
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
    WorkoutType.UNKNOWN -> "Tipo indefinido"
}

private fun Instant.formatDateTime(): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.forLanguageTag("pt-BR"))
        .withZone(ZoneId.systemDefault())
        .format(this)

private fun Instant.formatTime(): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(Locale.forLanguageTag("pt-BR"))
        .withZone(ZoneId.systemDefault())
        .format(this)

private fun Duration.toDurationLabel(): String {
    val totalMinutes = toMinutes()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours} h ${minutes} min"
        hours > 0 -> "${hours} h"
        else -> "${minutes} min"
    }
}
