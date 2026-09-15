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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import br.com.mwconsulting.championcompanion.R
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignHeaderAction
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignHeaderNavigationAction
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignSurfaceHeader
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignTheme
import br.com.mwconsulting.championcompanion.core.designsystem.LocalChampionRedesignTokens

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onOpenSettings: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    backLabel: String = "Voltar",
) {
    val state by viewModel.state.collectAsState()

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
                state.message?.let { message ->
                    ProfileMessageCard(
                        message = message,
                        onDismiss = { viewModel.onIntent(ProfileIntent.DismissMessage) },
                    )
                }
            }
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

private fun ProfileMessage.toMessageResource(): Int = when (this) {
    ProfileMessage.HEIGHT_SAVED -> R.string.profile_message_height_saved
    ProfileMessage.DAILY_STEPS_GOAL_SAVED -> R.string.profile_message_daily_steps_goal_saved
    ProfileMessage.HEIGHT_SAVE_FAILED -> R.string.profile_message_height_save_failed
    ProfileMessage.DAILY_STEPS_GOAL_SAVE_FAILED -> R.string.profile_message_daily_steps_goal_save_failed
    ProfileMessage.INVALID_HEIGHT -> R.string.profile_message_invalid_height
    ProfileMessage.INVALID_DAILY_STEPS_GOAL -> R.string.profile_message_invalid_daily_steps_goal
    ProfileMessage.PROFILE_UNAVAILABLE -> R.string.profile_message_profile_unavailable
}

private fun ProfileMessage.isError(): Boolean = when (this) {
    ProfileMessage.HEIGHT_SAVED,
    ProfileMessage.DAILY_STEPS_GOAL_SAVED,
    -> false

    ProfileMessage.HEIGHT_SAVE_FAILED,
    ProfileMessage.DAILY_STEPS_GOAL_SAVE_FAILED,
    ProfileMessage.INVALID_HEIGHT,
    ProfileMessage.INVALID_DAILY_STEPS_GOAL,
    ProfileMessage.PROFILE_UNAVAILABLE,
    -> true
}
