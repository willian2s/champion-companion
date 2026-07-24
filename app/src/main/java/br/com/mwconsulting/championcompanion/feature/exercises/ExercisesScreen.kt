package br.com.mwconsulting.championcompanion.feature.exercises

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportMetadata
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportProgress
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportResult
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportStatus
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityFilter
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import br.com.mwconsulting.championcompanion.core.model.WorkoutDistanceMetrics
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

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
                text = "Importe treinos gravados no relógio e mantenha a revisão para depois.",
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
private fun WorkoutActivityCard(
    activity: WorkoutActivity,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeMedium,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
            ) {
                AssistChip(onClick = {}, enabled = false, label = { Text(activity.displayType().toLabel()) })
                AssistChip(onClick = {}, enabled = false, label = { Text(activity.workflowState.toLabel()) })
            }
        }
    }
}

@Composable
private fun ExerciseDetailShell(
    state: ExercisesUiState,
    activityId: WorkoutActivityId,
    onNavigateBack: (() -> Unit)?,
) {
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                ) {
                    Text("Detalhe do treino", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Navegação manual por activityId. Conteúdo completo entra em 011-10.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onNavigateBack != null) {
                    AssistChip(onClick = onNavigateBack, label = { Text("Voltar") })
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
                        WorkoutActivityDetailSummary(activity)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutActivityDetailSummary(activity: WorkoutActivity) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        Text(activity.displayTitle(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            text = "${activity.metrics.startedAt.formatDateTime()} • ${activity.metrics.duration.toDurationLabel()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            AssistChip(onClick = {}, enabled = false, label = { Text(activity.workflowState.toLabel()) })
            AssistChip(onClick = {}, enabled = false, label = { Text(activity.displayType().toLabel()) })
        }
        Card(
            shape = ChampionShapeMedium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(ChampionSpacing.large),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
            ) {
                Text("Resumo inicial", style = MaterialTheme.typography.titleMedium)
                Text("Distância final: ${activity.metrics.distances.toDistanceLabel()}", style = MaterialTheme.typography.bodyMedium)
                Text("Validade do parser: ${activity.parserValidity.toLabel()}", style = MaterialTheme.typography.bodyMedium)
                Text("Tipo sugerido: ${activity.suggestedType.toLabel()}", style = MaterialTheme.typography.bodyMedium)
                activity.confirmedType?.let {
                    Text("Tipo confirmado: ${it.toLabel()}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
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
