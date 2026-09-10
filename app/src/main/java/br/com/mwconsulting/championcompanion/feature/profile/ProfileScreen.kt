package br.com.mwconsulting.championcompanion.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.mwconsulting.championcompanion.R
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignHeaderAction
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignHeaderNavigationAction
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignSurfaceHeader
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignTheme
import br.com.mwconsulting.championcompanion.core.designsystem.LocalChampionRedesignTokens
import br.com.mwconsulting.championcompanion.core.model.LearnedStepLengthProfile
import br.com.mwconsulting.championcompanion.core.model.StepLengthSource
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onOpenActivity: (br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    backLabel: String = "Voltar",
) {
    val state by viewModel.state.collectAsState()
    var showLearnedProfilesDialog by remember { mutableStateOf(false) }
    var showCalibrationHistoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.onIntent(ProfileIntent.Load)
    }

    ChampionRedesignTheme {
        val tokens = LocalChampionRedesignTokens.current
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = tokens.spacing.screen, vertical = tokens.spacing.compact),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.item),
            ) {
                ProfileHeader(
                    onOpenSettings = onOpenSettings,
                    onNavigateBack = onNavigateBack,
                    backLabel = backLabel,
                )
                if (state.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                PersonalMetricsEditorCard(
                    state = state,
                    onHeightChange = { viewModel.onIntent(ProfileIntent.HeightChanged(it)) },
                    onSaveHeight = { viewModel.onIntent(ProfileIntent.SaveHeight) },
                    onDailyStepsGoalChange = { viewModel.onIntent(ProfileIntent.DailyStepsGoalChanged(it)) },
                    onSaveDailyStepsGoal = { viewModel.onIntent(ProfileIntent.SaveDailyStepsGoal) },
                )
                StepLengthContextCard(
                    state = state,
                    onAccept = { viewModel.onIntent(ProfileIntent.AcceptHeightSuggestion) },
                    onValueChange = { viewModel.onIntent(ProfileIntent.StepLengthChanged(it)) },
                    onSave = { viewModel.onIntent(ProfileIntent.SaveManualStepLength) },
                )
                ProfileDetailsCard(
                    onOpenLearnedProfiles = { showLearnedProfilesDialog = true },
                    onOpenCalibrationHistory = { showCalibrationHistoryDialog = true },
                )
                if (state.isConfirmingHeightSuggestion) {
                    HeightSuggestionConfirmationDialog(
                        suggestionMeters = state.heightSuggestionMeters,
                        onConfirm = { viewModel.onIntent(ProfileIntent.ConfirmHeightSuggestion) },
                        onCancel = { viewModel.onIntent(ProfileIntent.CancelHeightSuggestion) },
                    )
                }
                state.message?.let { message ->
                    ProfileMessageCard(
                        message = message,
                        onDismiss = { viewModel.onIntent(ProfileIntent.DismissMessage) },
                    )
                }
            }
        }
    }

    ChampionRedesignTheme {
        if (showLearnedProfilesDialog) {
            AlertDialog(
                onDismissRequest = { showLearnedProfilesDialog = false },
                title = { Text(stringResource(R.string.profile_learned_profiles_title)) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        LearnedStepLengthProfilesCard(state.learnedStepLengthProfiles)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLearnedProfilesDialog = false }) {
                        Text(stringResource(R.string.profile_close_details))
                    }
                },
            )
        }

        if (showCalibrationHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showCalibrationHistoryDialog = false },
                title = { Text(stringResource(R.string.profile_calibration_history_title)) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        CalibrationHistoryCard(
                            history = state.calibrationHistory,
                            onRemove = { viewModel.onIntent(ProfileIntent.RemoveCalibration(it)) },
                            onOpenActivity = onOpenActivity,
                            correctionActivityId = state.correctionActivityId,
                            correctionDistanceInput = state.correctionDistanceInput,
                            onCorrect = { viewModel.onIntent(ProfileIntent.StartCalibrationCorrection(it)) },
                            onCorrectionChange = { viewModel.onIntent(ProfileIntent.CalibrationCorrectionChanged(it)) },
                            onSaveCorrection = { viewModel.onIntent(ProfileIntent.SaveCalibrationCorrection) },
                            onCancelCorrection = { viewModel.onIntent(ProfileIntent.CancelCalibrationCorrection) },
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCalibrationHistoryDialog = false }) {
                        Text(stringResource(R.string.profile_close_details))
                    }
                },
            )
        }
    }
}

@Composable
private fun StepLengthContextCard(
    state: ProfileUiState,
    onAccept: () -> Unit,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalChampionRedesignTokens.current.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(LocalChampionRedesignTokens.current.spacing.card),
            verticalArrangement = Arrangement.spacedBy(LocalChampionRedesignTokens.current.spacing.item),
        ) {
            Text(
                text = stringResource(R.string.profile_step_length_context_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.profile_step_length_context_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CurrentStepLengthContent(state)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            HeightSuggestionContent(state, onAccept)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StepLengthEditorContent(state, onValueChange, onSave)
        }
    }
}

@Composable
private fun ProfileHeader(
    onOpenSettings: () -> Unit,
    onNavigateBack: (() -> Unit)?,
    backLabel: String,
) {
    ChampionRedesignSurfaceHeader(
        title = stringResource(R.string.profile_title),
        supportingText = stringResource(R.string.profile_description),
        navigationContent = onNavigateBack?.let { navigateBack ->
            {
                ChampionRedesignHeaderNavigationAction(
                    contentDescription = backLabel,
                    onClick = navigateBack,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                    )
                }
            }
        },
        actionsContent = {
            ChampionRedesignHeaderAction(
                contentDescription = "Abrir Configurações",
                onClick = onOpenSettings,
            ) {
                Icon(imageVector = Icons.Filled.Settings, contentDescription = null)
            }
        },
    )
}

@Composable
private fun ProfileDetailsCard(
    onOpenLearnedProfiles: () -> Unit,
    onOpenCalibrationHistory: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalChampionRedesignTokens.current.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(LocalChampionRedesignTokens.current.spacing.card),
            verticalArrangement = Arrangement.spacedBy(LocalChampionRedesignTokens.current.spacing.compact),
        ) {
            Text(
                text = stringResource(R.string.profile_details_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.profile_details_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onOpenLearnedProfiles,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.profile_open_learned_profiles))
            }
            OutlinedButton(
                onClick = onOpenCalibrationHistory,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.profile_open_calibration_history))
            }
        }
    }
}

@Composable
private fun CalibrationHistoryCard(
    history: List<br.com.mwconsulting.championcompanion.core.model.StepLengthCalibrationSummary>,
    onRemove: (WorkoutActivityId) -> Unit,
    onOpenActivity: (WorkoutActivityId) -> Unit,
    correctionActivityId: WorkoutActivityId?,
    correctionDistanceInput: String,
    onCorrect: (br.com.mwconsulting.championcompanion.core.model.StepLengthCalibrationSummary) -> Unit,
    onCorrectionChange: (String) -> Unit,
    onSaveCorrection: () -> Unit,
    onCancelCorrection: () -> Unit,
) {
    var pendingRemovalActivityId by remember { mutableStateOf<WorkoutActivityId?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalChampionRedesignTokens.current.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(LocalChampionRedesignTokens.current.spacing.card),
            verticalArrangement = Arrangement.spacedBy(LocalChampionRedesignTokens.current.spacing.compact),
        ) {
            Text(
                text = stringResource(R.string.profile_calibration_history_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (history.isEmpty()) {
                Text(
                    text = stringResource(R.string.profile_calibration_history_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                history.forEach { calibration ->
                    val accentColor = profileAccentFor(calibration.workoutType)
                    Column(verticalArrangement = Arrangement.spacedBy(LocalChampionRedesignTokens.current.spacing.compact)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(LocalChampionRedesignTokens.current.spacing.item),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = workoutTypeLabel(calibration.workoutType),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = accentColor,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.profile_calibration_distance_summary,
                                        formatReferenceDistance(calibration.referenceDistanceMeters),
                                        calibration.decodedSteps,
                                        formatStepLengthMeters(calibration.observedStepLengthMeters),
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.profile_calibration_accepted_at,
                                        formatProfileDate(calibration.acceptedAt),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (calibration.updatedAt != calibration.acceptedAt) {
                                    Text(
                                        text = stringResource(
                                            R.string.profile_calibration_updated_at,
                                            formatProfileDate(calibration.updatedAt),
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { onOpenActivity(calibration.activityId) }) {
                                Text(stringResource(R.string.profile_open_activity))
                            }
                            TextButton(onClick = { onCorrect(calibration) }) {
                                Text(stringResource(R.string.profile_correct_calibration))
                            }
                            TextButton(onClick = { pendingRemovalActivityId = calibration.activityId }) {
                                Text(stringResource(R.string.profile_remove_calibration))
                            }
                        }
                    }
                    if (correctionActivityId == calibration.activityId) {
                        OutlinedTextField(
                            value = correctionDistanceInput,
                            onValueChange = onCorrectionChange,
                            label = { Text(stringResource(R.string.profile_calibration_correction_distance_label)) },
                            singleLine = true,
                        )
                        Row {
                            Button(onClick = onSaveCorrection) {
                                Text(stringResource(R.string.profile_save_correction))
                            }
                            TextButton(onClick = onCancelCorrection) {
                                Text(stringResource(R.string.profile_cancel_suggestion))
                            }
                        }
                    }
                }
            }
        }
    }
    pendingRemovalActivityId?.let { activityId ->
        AlertDialog(
            onDismissRequest = { pendingRemovalActivityId = null },
            title = { Text(stringResource(R.string.profile_remove_calibration_title)) },
            text = { Text(stringResource(R.string.profile_remove_calibration_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRemovalActivityId = null
                        onRemove(activityId)
                    },
                ) {
                    Text(stringResource(R.string.profile_remove_calibration_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemovalActivityId = null }) {
                    Text(stringResource(R.string.profile_cancel_suggestion))
                }
            },
        )
    }
}

@Composable
private fun LearnedStepLengthProfilesCard(
    profilesState: LearnedStepLengthProfilesState,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalChampionRedesignTokens.current.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(LocalChampionRedesignTokens.current.spacing.card),
            verticalArrangement = Arrangement.spacedBy(LocalChampionRedesignTokens.current.spacing.compact),
        ) {
            Text(
                text = stringResource(R.string.profile_learned_profiles_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.profile_learned_profiles_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (profilesState) {
                LearnedStepLengthProfilesState.Loading -> Text(
                    text = stringResource(R.string.profile_learned_profiles_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                LearnedStepLengthProfilesState.Unavailable -> Text(
                    text = stringResource(R.string.profile_learned_profiles_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )

                is LearnedStepLengthProfilesState.Available -> {
                    val profilesByType = profilesState.profiles.associateBy { it.workoutType }
                    if (profilesState.profiles.isEmpty()) {
                        Text(
                            text = stringResource(R.string.profile_learned_profiles_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        listOf(WorkoutType.RUNNING, WorkoutType.WALKING).forEach { workoutType ->
                            LearnedStepLengthProfileEntry(profilesByType[workoutType], workoutType)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LearnedStepLengthProfileEntry(
    profile: LearnedStepLengthProfile?,
    workoutType: WorkoutType,
) {
    val label = when (workoutType) {
        WorkoutType.RUNNING -> stringResource(R.string.profile_learned_running)
        WorkoutType.WALKING -> stringResource(R.string.profile_learned_walking)
        else -> return
    }
    val accentColor = profileAccentFor(workoutType)
    Text(text = label, style = MaterialTheme.typography.titleMedium, color = accentColor)
    if (profile == null) {
        Text(
            text = stringResource(R.string.profile_learned_context_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            text = stringResource(
                R.string.profile_learned_step_length_value,
                formatStepLengthMeters(profile.stepLengthMeters),
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.profile_learned_sample_count, profile.sampleCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                R.string.profile_learned_reference_distance,
                 formatReferenceDistance(profile.totalReferenceDistanceMeters),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                R.string.profile_learned_updated_at,
                formatProfileDate(profile.updatedAt),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PersonalMetricsEditorCard(
    state: ProfileUiState,
    onHeightChange: (String) -> Unit,
    onSaveHeight: () -> Unit,
    onDailyStepsGoalChange: (String) -> Unit,
    onSaveDailyStepsGoal: () -> Unit,
) {
    val heightIsInvalid = state.message == ProfileMessage.INVALID_HEIGHT
    val goalIsInvalid = state.message == ProfileMessage.INVALID_DAILY_STEPS_GOAL
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalChampionRedesignTokens.current.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(LocalChampionRedesignTokens.current.spacing.card),
            verticalArrangement = Arrangement.spacedBy(LocalChampionRedesignTokens.current.spacing.item),
        ) {
            Text(
                text = stringResource(R.string.profile_personal_metrics_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedTextField(
                value = state.heightInput,
                onValueChange = onHeightChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profile_height_label)) },
                supportingText = {
                    Text(stringResource(R.string.profile_height_supporting_text))
                },
                suffix = { Text(stringResource(R.string.profile_height_unit)) },
                singleLine = true,
                isError = heightIsInvalid,
                enabled = !state.isLoading && !state.isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Button(
                onClick = onSaveHeight,
                enabled = !state.isLoading && !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.profile_save_height))
            }
            OutlinedTextField(
                value = state.dailyStepsGoalInput,
                onValueChange = onDailyStepsGoalChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profile_daily_steps_goal_label)) },
                supportingText = {
                    Text(stringResource(R.string.profile_daily_steps_goal_supporting_text))
                },
                suffix = { Text(stringResource(R.string.profile_daily_steps_goal_unit)) },
                singleLine = true,
                isError = goalIsInvalid,
                enabled = !state.isLoading && !state.isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Button(
                onClick = onSaveDailyStepsGoal,
                enabled = !state.isLoading && !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.profile_save_daily_steps_goal))
            }
        }
    }
}

@Composable
private fun HeightSuggestionConfirmationDialog(
    suggestionMeters: Double?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.profile_confirm_suggestion_title)) },
        text = {
            Text(
                stringResource(
                    R.string.profile_confirm_suggestion_message,
                    suggestionMeters?.let(::formatStepLengthMeters).orEmpty(),
                ),
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.profile_confirm_suggestion))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.profile_cancel_suggestion))
            }
        },
    )
}

@Composable
private fun CurrentStepLengthContent(state: ProfileUiState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LocalChampionRedesignTokens.current.spacing.compact),
    ) {
        Text(
            text = stringResource(R.string.profile_current_step_length),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        val stepLengthMeters = state.stepLengthMeters
        if (stepLengthMeters == null) {
            Text(
                text = stringResource(R.string.profile_no_step_length),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(
                    R.string.profile_step_length_value,
                    formatStepLengthMeters(stepLengthMeters),
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            state.stepLengthSource?.let { source ->
                Text(
                    text = stringResource(
                        R.string.profile_step_length_source,
                        stringResource(source.toLabelResource()),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.stepLengthUpdatedAt?.let { updatedAt ->
                Text(
                    text = stringResource(
                        R.string.profile_step_length_updated_at,
                        formatProfileInstant(updatedAt),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StepLengthEditorContent(
    state: ProfileUiState,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val isInvalid = state.message == ProfileMessage.INVALID_STEP_LENGTH
    Column(
        verticalArrangement = Arrangement.spacedBy(LocalChampionRedesignTokens.current.spacing.item),
    ) {
        Text(
            text = stringResource(R.string.profile_manual_step_length_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
        OutlinedTextField(
            value = state.stepLengthInput,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.profile_step_length_label)) },
            placeholder = { Text(stringResource(R.string.profile_step_length_placeholder)) },
            supportingText = {
                Text(stringResource(R.string.profile_step_length_supporting_text))
            },
            suffix = { Text(stringResource(R.string.profile_step_length_unit)) },
            singleLine = true,
            isError = isInvalid,
            enabled = !state.isLoading && !state.isSaving,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Button(
            onClick = onSave,
            enabled = !state.isLoading && !state.isSaving && state.stepLengthInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (state.isSaving) {
                    stringResource(R.string.profile_saving)
                } else {
                    stringResource(R.string.profile_save_manual)
                },
            )
        }
    }
}

@Composable
private fun HeightSuggestionContent(
    state: ProfileUiState,
    onAccept: () -> Unit,
) {
    val suggestion = state.heightSuggestionMeters
    val suggestionAccepted = state.stepLengthSource == StepLengthSource.HEIGHT_SUGGESTION_ACCEPTED &&
        state.stepLengthMeters == suggestion
    Surface(
        shape = LocalChampionRedesignTokens.current.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(LocalChampionRedesignTokens.current.spacing.item),
            verticalArrangement = Arrangement.spacedBy(LocalChampionRedesignTokens.current.spacing.item),
        ) {
            Text(
                text = stringResource(R.string.profile_height_suggestion_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            if (suggestion == null) {
                Text(
                    text = stringResource(R.string.profile_height_suggestion_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.profile_height_suggestion_value,
                        formatStepLengthMeters(suggestion),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.profile_height_suggestion_confirmation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Button(
                    onClick = onAccept,
                    enabled = !state.isLoading && !state.isSaving && !suggestionAccepted,
                ) {
                    Text(
                        text = if (suggestionAccepted) {
                            stringResource(R.string.profile_height_suggestion_accepted)
                        } else {
                            stringResource(R.string.profile_accept_height_suggestion)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileMessageCard(
    message: ProfileMessage,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalChampionRedesignTokens.current.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = if (message.isError()) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LocalChampionRedesignTokens.current.spacing.item),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(message.toMessageResource()),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_dismiss_message))
            }
        }
    }
}

private fun StepLengthSource.toLabelResource(): Int = when (this) {
    StepLengthSource.MANUAL -> R.string.profile_source_manual
    StepLengthSource.HEIGHT_SUGGESTION_ACCEPTED -> R.string.profile_source_height_suggestion
    StepLengthSource.REFERENCE_DISTANCE_CALIBRATION -> R.string.profile_source_reference_calibration
    StepLengthSource.CONTEXTUAL_LEARNED_PROFILE -> R.string.profile_source_learned_profile
}

private fun ProfileMessage.toMessageResource(): Int = when (this) {
    ProfileMessage.HEIGHT_SAVED -> R.string.profile_message_height_saved
    ProfileMessage.MANUAL_SAVED -> R.string.profile_message_manual_saved
    ProfileMessage.HEIGHT_SUGGESTION_SAVED -> R.string.profile_message_suggestion_saved
    ProfileMessage.DAILY_STEPS_GOAL_SAVED -> R.string.profile_message_daily_steps_goal_saved
    ProfileMessage.HEIGHT_SAVE_FAILED -> R.string.profile_message_height_save_failed
    ProfileMessage.DAILY_STEPS_GOAL_SAVE_FAILED -> R.string.profile_message_daily_steps_goal_save_failed
    ProfileMessage.INVALID_HEIGHT -> R.string.profile_message_invalid_height
    ProfileMessage.INVALID_STEP_LENGTH -> R.string.profile_message_invalid_step_length
    ProfileMessage.INVALID_DAILY_STEPS_GOAL -> R.string.profile_message_invalid_daily_steps_goal
    ProfileMessage.PROFILE_UNAVAILABLE -> R.string.profile_message_profile_unavailable
    ProfileMessage.SAVE_FAILED -> R.string.profile_message_save_failed
    ProfileMessage.SUGGESTION_UNAVAILABLE -> R.string.profile_message_suggestion_unavailable
}

private fun ProfileMessage.isError(): Boolean = when (this) {
    ProfileMessage.HEIGHT_SAVED,
    ProfileMessage.MANUAL_SAVED,
    ProfileMessage.HEIGHT_SUGGESTION_SAVED,
    ProfileMessage.DAILY_STEPS_GOAL_SAVED,
    -> false

    ProfileMessage.HEIGHT_SAVE_FAILED,
    ProfileMessage.DAILY_STEPS_GOAL_SAVE_FAILED,
    ProfileMessage.INVALID_HEIGHT,
    ProfileMessage.INVALID_STEP_LENGTH,
    ProfileMessage.INVALID_DAILY_STEPS_GOAL,
    ProfileMessage.PROFILE_UNAVAILABLE,
    ProfileMessage.SAVE_FAILED,
    ProfileMessage.SUGGESTION_UNAVAILABLE,
    -> true
}

private fun formatProfileInstant(instant: Instant): String = DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.SHORT)
    .withLocale(Locale.getDefault())
    .format(instant.atZone(ZoneId.systemDefault()))

private fun formatProfileDate(instant: Instant): String = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.MEDIUM)
    .withLocale(Locale.getDefault())
    .format(instant.atZone(ZoneId.systemDefault()))

private fun workoutTypeLabel(workoutType: WorkoutType): String = when (workoutType) {
    WorkoutType.RUNNING -> "Corrida"
    WorkoutType.WALKING -> "Caminhada"
    else -> workoutType.name
}

@Composable
private fun profilePrimaryAccent() = MaterialTheme.colorScheme.primary

@Composable
private fun profileSecondaryAccent() = MaterialTheme.colorScheme.secondary

@Composable
private fun profileTertiaryAccent() = MaterialTheme.colorScheme.tertiary

@Composable
private fun profileAccentFor(workoutType: WorkoutType) = when (workoutType) {
    WorkoutType.RUNNING -> profileSecondaryAccent()
    WorkoutType.WALKING -> profileTertiaryAccent()
    else -> profilePrimaryAccent()
}

private fun formatReferenceDistance(meters: Double): String =
    String.format(Locale.getDefault(), "%.2f km", meters / 1_000.0)
