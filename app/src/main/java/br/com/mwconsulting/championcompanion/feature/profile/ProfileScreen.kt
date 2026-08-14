package br.com.mwconsulting.championcompanion.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import br.com.mwconsulting.championcompanion.R
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeLarge
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeMedium
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
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
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.onIntent(ProfileIntent.Load)
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
            Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.profile_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.isLoading) {
                CircularProgressIndicator()
            }
            PersonalMetricsEditorCard(
                state = state,
                onHeightChange = { viewModel.onIntent(ProfileIntent.HeightChanged(it)) },
                onSaveHeight = { viewModel.onIntent(ProfileIntent.SaveHeight) },
                onDailyStepsGoalChange = { viewModel.onIntent(ProfileIntent.DailyStepsGoalChanged(it)) },
                onSaveDailyStepsGoal = { viewModel.onIntent(ProfileIntent.SaveDailyStepsGoal) },
            )
            CurrentStepLengthCard(state)
            LearnedStepLengthProfilesCard(state.learnedStepLengthProfiles)
            CalibrationHistoryCard(
                history = state.calibrationHistory,
                onRemove = { viewModel.onIntent(ProfileIntent.RemoveCalibration(it)) },
                correctionActivityId = state.correctionActivityId,
                correctionDistanceInput = state.correctionDistanceInput,
                onCorrect = { viewModel.onIntent(ProfileIntent.StartCalibrationCorrection(it)) },
                onCorrectionChange = { viewModel.onIntent(ProfileIntent.CalibrationCorrectionChanged(it)) },
                onSaveCorrection = { viewModel.onIntent(ProfileIntent.SaveCalibrationCorrection) },
                onCancelCorrection = { viewModel.onIntent(ProfileIntent.CancelCalibrationCorrection) },
            )
            StepLengthEditorCard(
                state = state,
                onValueChange = { viewModel.onIntent(ProfileIntent.StepLengthChanged(it)) },
                onSave = { viewModel.onIntent(ProfileIntent.SaveManualStepLength) },
            )
            HeightSuggestionCard(
                state = state,
                onAccept = { viewModel.onIntent(ProfileIntent.AcceptHeightSuggestion) },
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

@Composable
private fun CalibrationHistoryCard(
    history: List<br.com.mwconsulting.championcompanion.core.model.StepLengthCalibrationSummary>,
    onRemove: (WorkoutActivityId) -> Unit,
    correctionActivityId: WorkoutActivityId?,
    correctionDistanceInput: String,
    onCorrect: (br.com.mwconsulting.championcompanion.core.model.StepLengthCalibrationSummary) -> Unit,
    onCorrectionChange: (String) -> Unit,
    onSaveCorrection: () -> Unit,
    onCancelCorrection: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Histórico de calibrações", style = MaterialTheme.typography.titleLarge)
            if (history.isEmpty()) {
                Text("Nenhuma evidência de distância de referência.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                history.forEach { calibration ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (calibration.workoutType == WorkoutType.RUNNING) "Corrida" else "Caminhada")
                            Text(
                                "${formatReferenceDistance(calibration.referenceDistanceMeters)} • " +
                                    "${calibration.decodedSteps} passos • " +
                                    formatStepLengthMeters(calibration.observedStepLengthMeters),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row {
                            TextButton(onClick = { onCorrect(calibration) }) { Text("Corrigir") }
                            TextButton(onClick = { onRemove(calibration.activityId) }) { Text("Remover") }
                        }
                    }
                    if (correctionActivityId == calibration.activityId) {
                        OutlinedTextField(
                            value = correctionDistanceInput,
                            onValueChange = onCorrectionChange,
                            label = { Text("Nova distância (km)") },
                            singleLine = true,
                        )
                        Row {
                            Button(onClick = onSaveCorrection) { Text("Salvar correção") }
                            TextButton(onClick = onCancelCorrection) { Text("Cancelar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LearnedStepLengthProfilesCard(
    profilesState: LearnedStepLengthProfilesState,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = stringResource(R.string.profile_learned_profiles_title),
                style = MaterialTheme.typography.titleLarge,
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
    Text(text = label, style = MaterialTheme.typography.titleMedium)
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
        shape = ChampionShapeLarge,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text(
                text = stringResource(R.string.profile_personal_metrics_title),
                style = MaterialTheme.typography.titleLarge,
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
private fun CurrentStepLengthCard(state: ProfileUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = stringResource(R.string.profile_current_step_length),
                style = MaterialTheme.typography.titleLarge,
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
}

@Composable
private fun StepLengthEditorCard(
    state: ProfileUiState,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val isInvalid = state.message == ProfileMessage.INVALID_STEP_LENGTH
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text(
                text = stringResource(R.string.profile_manual_step_length_title),
                style = MaterialTheme.typography.titleLarge,
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
}

@Composable
private fun HeightSuggestionCard(
    state: ProfileUiState,
    onAccept: () -> Unit,
) {
    val suggestion = state.heightSuggestionMeters
    val suggestionAccepted = state.stepLengthSource == StepLengthSource.HEIGHT_SUGGESTION_ACCEPTED &&
        state.stepLengthMeters == suggestion
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text(
                text = stringResource(R.string.profile_height_suggestion_title),
                style = MaterialTheme.typography.titleLarge,
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
        shape = ChampionShapeMedium,
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
                .padding(ChampionSpacing.medium),
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
    StepLengthSource.CONTEXTUAL_LEARNED_PROFILE -> R.string.profile_source_reference_calibration
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

private fun formatReferenceDistance(meters: Double): String =
    String.format(Locale.getDefault(), "%.2f km", meters / 1_000.0)
