package br.com.mwconsulting.championcompanion.feature.exercises

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeLarge
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeMedium
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.DarkColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.LightColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.PendingReviewBanner
import br.com.mwconsulting.championcompanion.core.designsystem.PermissionBanner
import br.com.mwconsulting.championcompanion.core.designsystem.PrimaryActionButton
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionTimeSeriesChart
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionCartesianChartData
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartAxisSpec
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartFormatters
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartInteractionPolicy
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartPoint
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartReferenceLine
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartSeries
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartUnit
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartXAxisFormatter
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartXAxisSpec
import br.com.mwconsulting.championcompanion.core.model.ActivityWorkflowState
import br.com.mwconsulting.championcompanion.core.model.CanonicalWorkoutSeries
import br.com.mwconsulting.championcompanion.core.model.DerivedWorkoutInterval
import br.com.mwconsulting.championcompanion.core.model.DomainValue
import br.com.mwconsulting.championcompanion.core.model.ParserValidity
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordAchievement
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordMetric
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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val ExerciseHeartRateGuideBands = listOf(166.0, 135.0, 110.0, 71.0, 30.0)
private val ExerciseCadenceChartValueFormatter =
    ChampionChartFormatters.decimal(maximumFractionDigits = 1)

@Composable
fun ExercisesScreen(
    openedActivityId: WorkoutActivityId? = null,
    onNavigateBack: (() -> Unit)? = null,
    onOpenActivity: (WorkoutActivityId) -> Unit = {},
    onOpenEvolution: () -> Unit = {},
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
    val dismissOperationError = remember(viewModel) {
        { viewModel.onIntent(ExercisesIntent.DismissOperationError) }
    }
    val scrollState = rememberScrollState()

    LaunchedEffect(viewModel) {
        viewModel.onIntent(ExercisesIntent.Load)
    }

    LaunchedEffect(viewModel, onNavigateBack) {
        viewModel.activityDeleted.collect {
            onNavigateBack?.invoke()
        }
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
                    .verticalScroll(scrollState)
                .padding(ChampionSpacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
        ) {
            if (state.personalRecordAchievements.isNotEmpty()) {
                PersonalRecordAchievementBanner(
                    achievements = state.personalRecordAchievements,
                    onDismiss = { viewModel.onIntent(ExercisesIntent.DismissPersonalRecordAchievements) },
                    onOpenEvolution = {
                        viewModel.onIntent(ExercisesIntent.DismissPersonalRecordAchievements)
                        onOpenEvolution()
                    },
                )
            }
            if (openedActivityId == null) {
                ExercisesHeader(state = state)
                ExercisesEvolutionCard(onOpenEvolution = onOpenEvolution)
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
private fun PersonalRecordAchievementBanner(
    achievements: List<PersonalRecordAchievement>,
    onDismiss: () -> Unit,
    onOpenEvolution: () -> Unit,
) {
    val achievementLines = achievements.map(PersonalRecordAchievement::toFeedbackLabel)
    val accessibleMessage = achievementLines.joinToString(separator = ". ")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Novo recorde pessoal. $accessibleMessage"
                liveRegion = LiveRegionMode.Polite
            },
        shape = ChampionShapeLarge,
        border = BorderStroke(1.dp, exerciseSecondaryAccent()),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = "Novo recorde pessoal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Conquista registrada após salvar a atividade.",
                style = MaterialTheme.typography.bodyMedium,
            )
            achievementLines.forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small)) {
                TextButton(onClick = onDismiss) {
                    Text("Fechar")
                }
                TextButton(onClick = onOpenEvolution) {
                    Text("Ver recordes")
                }
            }
        }
    }
}

@Composable
private fun ExercisesEvolutionCard(
    onOpenEvolution: () -> Unit,
) {
    Card(
        onClick = onOpenEvolution,
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        border = BorderStroke(1.dp, exerciseSecondaryAccent()),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = "Evolução esportiva",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Acompanhe volume, tendências e recordes das atividades importadas do relógio.",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Abrir evolução",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ExercisesHeader(state: ExercisesUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = LightColorScheme.primary,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = "Exercícios",
                modifier = Modifier.semantics { heading() },
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Importe atividades gravadas no relógio e revise-as localmente.",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = PhoneWorkoutPolicy.DISCONTINUED_MESSAGE,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
            Text(
                text = if (state.hasAssociatedDevice) {
                    "Relógio associado: ${state.associatedDeviceName ?: "Champion C001"}"
                } else {
                    "Nenhum relógio associado. A importação manual ficará disponível após associar um dispositivo."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.82f),
            )
        }
    }
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

@Composable
private fun ExercisesListSection(
    selectedFilter: WorkoutActivityFilter,
    activities: List<WorkoutActivity>,
    onOpenActivity: (WorkoutActivityId) -> Unit,
) {
    val pageSize = 5
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var selectedPage by remember(selectedFilter) { mutableStateOf(0) }
    var pageNavigationRequested by remember(selectedFilter) { mutableStateOf(false) }
    val pageCount = (activities.size + pageSize - 1) / pageSize
    val currentPage = selectedPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))

    LaunchedEffect(selectedFilter, activities.size) {
        selectedPage = 0
    }

    LaunchedEffect(pageNavigationRequested) {
        if (pageNavigationRequested) {
            bringIntoViewRequester.bringIntoView()
            pageNavigationRequested = false
        }
    }

    fun selectPage(page: Int) {
        val nextPage = page.coerceIn(0, pageCount - 1)
        if (nextPage != currentPage) {
            selectedPage = nextPage
            pageNavigationRequested = true
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        Text(
            text = "${selectedFilter.toSectionTitle()} • ${activities.size}",
            modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester),
            style = MaterialTheme.typography.titleLarge,
        )
        if (activities.isEmpty()) {
            EmptyState(
                title = "Nenhum treino neste filtro",
                message = "Troque o filtro ou importe novos treinos do relógio para preencher esta lista.",
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            activities
                .drop(currentPage * pageSize)
                .take(pageSize)
                .forEach { activity ->
                WorkoutActivityCard(
                    activity = activity,
                    onClick = { onOpenActivity(activity.id) },
                )
            }
            if (pageCount > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { selectPage(currentPage - 1) },
                        enabled = currentPage > 0,
                    ) {
                        Text("Anterior")
                    }
                    Text(
                        text = "Página ${currentPage + 1} de $pageCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = { selectPage(currentPage + 1) },
                        enabled = currentPage < pageCount - 1,
                    ) {
                        Text("Próxima")
                    }
                }
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
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp),
        shape = ChampionShapeMedium,
        border = BorderStroke(1.dp, accentColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(ChampionSpacing.medium),
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
                Text(
                    text = activity.displayListTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = activity.displayListContextLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${activity.metrics.duration.toDurationLabel()} • ${activity.displayDistanceLabel()}",
                    style = MaterialTheme.typography.bodyMedium,
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ChampionShapeLarge,
            colors = CardDefaults.cardColors(containerColor = LightColorScheme.primary),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ChampionSpacing.medium),
                horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                onNavigateBack?.let { navigateBack ->
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar para exercícios",
                            tint = Color.White,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
                ) {
                    Text(
                        "Detalhe do treino",
                        modifier = Modifier.semantics { heading() },
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Atividade importada",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
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
) {
    val hasReferenceDistanceCalibration = activity.isReferenceDistanceCalibrationEligible() &&
        referenceDistanceCalibrationAllowed

    Column(
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        ActivityHeaderCard(activity)
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
        ActivityStatusOriginSection(activity)
        if (activity.isWatchWorkout && !activity.isCycling && activity.requiresWatchWorkoutReprocessing) {
            WatchWorkoutReprocessSection(
                activity = activity,
                isReprocessing = isReprocessing,
                onRequestReprocess = onRequestReprocess,
            )
        }
        DetailSectionHeading("Resumo")
        ActivityMetricsSection(activity, latestCalibration)
        DetailSectionHeading("Tendências")
        ActivityCanonicalChartSection(activity)
        ActivityCadenceChartSection(activity)
        ActivityHeartRateChartSection(activity)
        if (!activity.isCycling) {
            DetailSectionHeading("Detalhamento")
            ActivityIntervalsSection(activity)
            ActivitySplitsSection(activity)
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
        if (canDiscardSelectedActivity || canDeleteSelectedActivity) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ChampionShapeMedium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier.padding(ChampionSpacing.large),
                    verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                ) {
                    DetailSectionHeading("Ações locais")
                    Text(
                        "Estas ações não iniciam sincronização e exigem confirmação.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (canDiscardSelectedActivity) {
                        TextButton(onClick = onRequestDiscard, modifier = Modifier.align(Alignment.End)) {
                            Text("Descartar treino", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (canDeleteSelectedActivity) {
                        TextButton(
                            onClick = onRequestDelete,
                            enabled = !isDeletingSelectedActivity,
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text("Excluir treino permanentemente", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
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
private fun ActivityHeaderCard(activity: WorkoutActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(1.dp, exercisePrimaryAccent()),
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
        }
    }
}

@Composable
private fun ActivityStatusOriginSection(activity: WorkoutActivity) {
    val isCanonicalWarning = activity.isWatchWorkout && !activity.isCycling &&
        activity.canonicalReadStatus in setOf(
            WorkoutCanonicalReadStatus.PLAUSIBILITY_BLOCKED,
            WorkoutCanonicalReadStatus.SERIES_INELIGIBLE,
        )
    val statusAccent = if (isCanonicalWarning) {
        MaterialTheme.colorScheme.error
    } else {
        activity.workflowState.toListAccentColor()
    }
    val snapshot = activity.derivationSnapshot

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                stateDescription = activity.workflowState.toLabel()
                contentDescription = activity.statusOriginAccessibleDescription()
            },
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, statusAccent),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text(
                "Status e origem",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
            ) {
                ActivityBadge(activity.workflowState.toChipLabel(), statusAccent)
                ActivityBadge(activity.displayType().toLabel(), exerciseSecondaryAccent())
                ActivityBadge(activity.parserValidity.toLabel(), exerciseTertiaryAccent())
                ActivityBadge(
                    activity.originLabel(),
                    exercisePrimaryAccent(),
                )
            }
            if (activity.isWatchWorkout && !activity.isCycling) {
                Text(
                    "Derivação canônica",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = activity.canonicalReadStatus.toShortLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusAccent,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = activity.canonicalStatusMessage(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                snapshot?.let {
                    Text(
                        text = "Comprimento do passo: ${formatStepLength(it.stepLengthMetersUsed)} (${it.stepLengthSource.toLabel()}) • Derivação ${it.derivationVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (snapshot?.plausibility?.indicators?.isNotEmpty() == true) {
                    Text(
                        text = "Indicadores: ${snapshot.plausibility.indicators.joinToString { it.toLabel() }}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityBadge(label: String, accentColor: Color) {
    Surface(
        shape = ChampionShapeMedium,
        color = accentColor.copy(alpha = if (isSystemInDarkTheme()) 0.2f else 0.1f),
        contentColor = accentColor,
        border = BorderStroke(1.dp, accentColor),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = ChampionSpacing.medium, vertical = ChampionSpacing.small),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
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
private fun ActivityCanonicalChartSection(activity: WorkoutActivity) {
    if (!activity.isWatchWorkout || activity.isCycling) return
    val series = activity.canonicalSeries
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text("Série canônica", style = MaterialTheme.typography.titleLarge)
            if (series == null) {
                Text(
                    text = "Gráfico indisponível: ${activity.canonicalStatusMessage()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (series.intervals.isEmpty()) {
                Text(
                    text = "Gráfico indisponível: série sem intervalos derivados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = if (activity.canonicalReadStatus == WorkoutCanonicalReadStatus.PLAUSIBILITY_BLOCKED) {
                        "Distância estimada acumulada para diagnóstico; não é uma série confiável."
                    } else {
                        "Distância estimada acumulada, sem suavização."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WorkoutCanonicalDistanceChart(series)
                Text(
                    text = canonicalChartAccessibleSummary(series),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WorkoutCanonicalDistanceChart(series: CanonicalWorkoutSeries) {
    val intervals = series.intervals
    val first = intervals.first().effectiveWindow.start
    val last = intervals.last().effectiveWindow.end
    val durationSeconds = elapsedSeconds(first, last).coerceAtLeast(1.0)
    val maximumDistanceKm = (intervals.last().cumulativeDistanceMeters / 1_000.0).let {
        if (it == 0.0) 0.01 else it
    }
    val points = intervals.map { interval ->
        val elapsedSeconds = elapsedSeconds(first, interval.effectiveWindow.end)
        ChampionChartPoint(
            x = elapsedSeconds,
            y = interval.cumulativeDistanceMeters / 1_000.0,
            xLabel = interval.effectiveWindow.end.formatChartTime(),
            contentDescription = "Intervalo ${interval.index + 1}, " +
                "${interval.cumulativeDistanceMeters.toDistanceLabel()} acumulados em " +
                interval.effectiveWindow.end.formatDateTime() + ".",
        )
    }
    val axisPadding = (chartAxisPadding(points) / 2.0).coerceAtLeast(0.5)
    val axisMaximum = durationSeconds + axisPadding
    val chartData = ChampionCartesianChartData(
        series = listOf(
            ChampionChartSeries(
                key = "canonical-distance",
                label = "Distância acumulada",
                points = points,
            ),
        ),
        xAxis = ChampionChartXAxisSpec(
            formatter = ChampionChartXAxisFormatter { value ->
                first.plusNanos((value * 1_000_000_000.0).roundToLong()).formatChartAxisTime()
            },
            title = "Tempo",
            minimum = -axisPadding,
            maximum = axisMaximum,
        ),
        yAxis = ChampionChartAxisSpec(
            title = "Distância acumulada",
            formatter = ChampionChartFormatters.decimal(maximumFractionDigits = 2),
            unit = ChampionChartUnit("km"),
            minimum = 0.0,
            maximum = maximumDistanceKm,
        ),
        interaction = ChampionChartInteractionPolicy(
            scrollEnabled = intervals.size > 6,
        ),
        accessibilitySummary = canonicalChartAccessibleSummary(series) +
            " Valores em quilômetros; pontos representam o término efetivo de cada intervalo.",
    )
    ChampionTimeSeriesChart(
        data = chartData,
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
    )
}

private fun canonicalChartAccessibleSummary(series: CanonicalWorkoutSeries): String =
    "Resumo acessível: ${series.intervals.size} intervalos, término em ${series.intervals.last().cumulativeDistanceMeters.toDistanceLabel()} acumulados."

@Composable
private fun ActivityCadenceChartSection(activity: WorkoutActivity) {
    if (!activity.isWatchWorkout || activity.isCycling) return
    val snapshot = activity.derivationSnapshot
    val series = activity.canonicalSeries
    val intervals = if (snapshot?.canonicalSeriesEligible == true) {
        series?.intervals.orEmpty()
    } else {
        emptyList()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text("Cadência D4", style = MaterialTheme.typography.titleLarge)
            when {
                snapshot?.canonicalSeriesEligible != true -> Text(
                    text = "Gráfico indisponível: ${activity.canonicalStatusMessage()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                series == null -> Text(
                    text = "Gráfico indisponível: série canônica ausente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                intervals.isEmpty() -> Text(
                    text = "Gráfico indisponível: não há intervalos D4 elegíveis para cadência.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> {
                    Text(
                        text = if (activity.canonicalReadStatus == WorkoutCanonicalReadStatus.PLAUSIBILITY_BLOCKED) {
                            "Cadência D4 por intervalo real para diagnóstico; a série contém valores implausíveis e não é confiável."
                        } else {
                            "Cadência D4 por intervalo real, sem interpolação, suavização ou reamostragem."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WorkoutCadenceChart(series)
                    Text(
                        text = cadenceChartAccessibleSummary(intervals),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutCadenceChart(series: CanonicalWorkoutSeries) {
    val intervals = series.intervals
    if (intervals.isEmpty()) return
    val first = intervals.first().effectiveWindow.start
    val points = intervals.map { interval ->
        val midpoint = interval.effectiveWindow.start.plus(interval.effectiveDuration.dividedBy(2))
        ChampionChartPoint(
            x = elapsedSeconds(first, midpoint),
            y = interval.cadenceStepsPerMinute,
            xLabel = cadenceIntervalChartLabel(interval),
            contentDescription = cadenceIntervalAccessibleDescription(interval),
        )
    }
    val durationSeconds = elapsedSeconds(first, intervals.last().effectiveWindow.end).coerceAtLeast(1.0)
    val maximumCadence = points.maxOf { it.y ?: 0.0 }.coerceAtLeast(1.0)
    val axisPadding = (chartAxisPadding(points) / 2.0).coerceAtLeast(0.5)
    val chartData = ChampionCartesianChartData(
        series = listOf(
            ChampionChartSeries(
                key = "workout-cadence",
                label = "Cadência D4",
                points = points,
            ),
        ),
        xAxis = ChampionChartXAxisSpec(
            formatter = ChampionChartXAxisFormatter { value ->
                Duration.ofMillis((value * 1_000.0).roundToLong()).toDurationLabel()
            },
            title = "Tempo",
            minimum = 0.0,
            maximum = durationSeconds + axisPadding,
        ),
        yAxis = ChampionChartAxisSpec(
            title = "Cadência",
            formatter = ExerciseCadenceChartValueFormatter,
            unit = ChampionChartUnit("spm"),
            minimum = 0.0,
            maximum = maximumCadence,
        ),
        interaction = ChampionChartInteractionPolicy(
            scrollEnabled = intervals.size > 6,
        ),
        accessibilitySummary = cadenceChartAccessibleSummary(intervals) +
            " Cada ponto representa o ponto médio da janela efetiva do intervalo; não há pontos sintéticos.",
    )
    ChampionTimeSeriesChart(
        data = chartData,
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
    )
}

private fun cadenceIntervalChartLabel(interval: DerivedWorkoutInterval): String {
    val measuredSteps = interval.decodedSteps?.let { " · ${numberLabel(it)} passos" }.orEmpty()
    return "${interval.effectiveWindow.start.formatChartTime()}–" +
        "${interval.effectiveWindow.end.formatChartTime()}$measuredSteps"
}

private fun cadenceIntervalAccessibleDescription(interval: DerivedWorkoutInterval): String =
    "Intervalo ${interval.index + 1}, período ${interval.effectiveWindow.start.formatDateTime()} a " +
        "${interval.effectiveWindow.end.formatDateTime()}, cadência " +
        "${ExerciseCadenceChartValueFormatter.format(interval.cadenceStepsPerMinute)} spm, " +
        (interval.decodedSteps?.let { "${numberLabel(it)} passos medidos" } ?: "passos medidos indisponíveis") + "."

private fun cadenceChartAccessibleSummary(intervals: List<DerivedWorkoutInterval>): String {
    if (intervals.isEmpty()) return "Resumo acessível: nenhum intervalo D4 elegível para cadência."
    val cadenceValues = intervals.map(DerivedWorkoutInterval::cadenceStepsPerMinute)
    return "Resumo acessível: ${intervals.size} intervalos D4 reais, de " +
        "${intervals.first().effectiveWindow.start.formatDateTime()} a " +
        "${intervals.last().effectiveWindow.end.formatDateTime()}, cadência entre " +
        "${ExerciseCadenceChartValueFormatter.format(cadenceValues.min())} e " +
        "${ExerciseCadenceChartValueFormatter.format(cadenceValues.max())} spm."
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
            activity.detailMetricItems().forEachIndexed { index, item ->
                MetricCard(item.label, item.value, exerciseMetricAccent(index))
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
                    Text(
                        text = if (activity.isWatchWorkout) "Calibração histórica (diagnóstico)" else "Calibração ativa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("Revisão ${it.revision} • Aplicada em ${it.appliedAt.formatDateTime()}", style = MaterialTheme.typography.bodyMedium)
                    Text("Distância informada: ${it.reportedDistanceMeters.toDistanceLabel()}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
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
    val sortedSamples = samples.sortedBy { it.sampledAt }
    val first = sortedSamples.first().sampledAt
    val lanes = mutableListOf<MutableList<ChampionChartPoint>>()
    sortedSamples.forEach { sample ->
        val x = elapsedSeconds(first, sample.sampledAt)
        val point = ChampionChartPoint(
            x = x,
            y = sample.bpm.toDouble(),
            xLabel = sample.sampledAt.formatChartTime(),
            contentDescription = "${sample.bpm} bpm em ${sample.sampledAt.formatDateTime()}.",
        )
        lanes.firstOrNull { lane -> lane.lastOrNull()?.x?.let { it < x } != false }
            ?.add(point)
            ?: lanes.add(mutableListOf(point))
    }
    val points = lanes.flatMap { it }
    val durationSeconds = points.maxOfOrNull { it.x }?.coerceAtLeast(1.0) ?: 1.0
    val axisPadding = (chartAxisPadding(points) / 2.0).coerceAtLeast(0.5)
    val axisMaximum = durationSeconds + axisPadding
    val chartData = ChampionCartesianChartData(
        series = lanes.mapIndexed { index, pointsInLane ->
            ChampionChartSeries(
                key = if (index == 0) "workout-heart-rate" else "workout-heart-rate-$index",
                label = "Frequência cardíaca",
                points = pointsInLane,
            )
        },
        xAxis = ChampionChartXAxisSpec(
            formatter = ChampionChartXAxisFormatter { value ->
                first.plusNanos((value * 1_000_000_000.0).roundToLong()).formatChartAxisTime()
            },
            title = "Tempo",
            minimum = -axisPadding,
            maximum = axisMaximum,
        ),
        yAxis = ChampionChartAxisSpec(
            title = "Frequência cardíaca",
            formatter = ChampionChartFormatters.integer(),
            unit = ChampionChartUnit("bpm"),
            minimum = 30.0,
            maximum = 220.0,
        ),
        referenceLines = ExerciseHeartRateGuideBands.map { value ->
            ChampionChartReferenceLine(
                value = value,
                label = "${value.roundToInt()} bpm",
                contentDescription = "Guideline de ${value.roundToInt()} bpm.",
            )
        },
        interaction = ChampionChartInteractionPolicy(
            scrollEnabled = points.size > 6,
        ),
        accessibilitySummary = "Gráfico de frequência cardíaca do treino. " +
            "${points.size} amostra(s) reais disponíveis. Valores entre 30 e 220 bpm.",
    )
    ChampionTimeSeriesChart(
        data = chartData,
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
                            text = "Nenhum split materializado para exibir.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
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
    if (isWatchWorkout) {
        if (isCycling) {
            return listOf(
                DetailMetricItem("Distância", cyclingManualDistanceMetersOrNull().toDistanceLabel()),
                DetailMetricItem("Velocidade média", cyclingAverageSpeedMetersPerSecond().toSpeedLabel()),
                DetailMetricItem("Calorias", metrics.caloriesKilocalories.toKcalLabel()),
                DetailMetricItem("BPM médio", metrics.averageBpm.toBpmLabel()),
                DetailMetricItem("BPM mínimo", metrics.minimumBpm.toBpmLabel()),
                DetailMetricItem("BPM máximo", metrics.maximumBpm.toBpmLabel()),
            )
        }
        val canonical = canonicalMetricReadModel()
        return listOf(
            DetailMetricItem("Distância estimada acumulada", canonical?.totalDistanceMeters.toDistanceLabel()),
            DetailMetricItem("Passos medidos", canonical?.cumulativeSteps?.let(::numberLabel) ?: "indisponível"),
            DetailMetricItem("Ritmo estimado", canonical?.paceSecondsPerKilometer.toPaceLabel()),
            DetailMetricItem("Velocidade estimada", canonical?.speedMetersPerSecond.toSpeedLabel()),
            DetailMetricItem("Calorias", metrics.caloriesKilocalories.toKcalLabel()),
            DetailMetricItem("BPM médio", metrics.averageBpm.toBpmLabel()),
            DetailMetricItem("BPM mínimo", metrics.minimumBpm.toBpmLabel()),
            DetailMetricItem("BPM máximo", metrics.maximumBpm.toBpmLabel()),
        )
    }
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

private val WorkoutActivity.isWatchWorkout: Boolean
    get() = activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4

private val WorkoutActivity.isCycling: Boolean
    get() = confirmedType == WorkoutType.CYCLING

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

private fun WorkoutActivity.displayDistanceLabel(): String = when {
    isCycling -> "Distância ${cyclingManualDistanceMetersOrNull().toDistanceLabel()}"
    isWatchWorkout -> "Distância estimada ${canonicalMetricReadModel()?.totalDistanceMeters.toDistanceLabel()}"
    else -> "Distância ${metrics.distances.toDistanceLabel()}"
}

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

private fun elapsedSeconds(start: Instant, end: Instant): Double {
    val duration = Duration.between(start, end)
    return duration.seconds.toDouble() + duration.nano.toDouble() / 1_000_000_000.0
}

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

private fun PersonalRecordAchievement.toFeedbackLabel(): String {
    val differenceLabel = difference?.let {
        " (diferença de ${record.metric.formatAchievementValue(it)})"
    }.orEmpty()
    return "${record.metric.toAchievementLabel()} · ${record.workoutType.toLabel()}: " +
        record.metric.formatAchievementValue(record.value) + differenceLabel
}

private fun PersonalRecordMetric.toAchievementLabel(): String = when (this) {
    PersonalRecordMetric.LONGEST_DISTANCE -> "Maior distância"
    PersonalRecordMetric.LONGEST_DURATION -> "Maior duração"
    PersonalRecordMetric.MOST_STEPS -> "Mais passos"
}

private fun PersonalRecordMetric.formatAchievementValue(value: Double): String = when (this) {
    PersonalRecordMetric.LONGEST_DISTANCE ->
        String.format(Locale.forLanguageTag("pt-BR"), "%.2f km", value / 1_000.0)

    PersonalRecordMetric.LONGEST_DURATION ->
        Duration.ofSeconds(value.roundToLong().coerceAtLeast(1L)).toCanonicalDurationLabel()

    PersonalRecordMetric.MOST_STEPS -> "${numberLabel(value.roundToLong())} passos"
}

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
