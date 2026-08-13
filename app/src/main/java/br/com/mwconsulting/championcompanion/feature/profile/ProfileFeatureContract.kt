package br.com.mwconsulting.championcompanion.feature.profile

import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import br.com.mwconsulting.championcompanion.core.model.DEFAULT_DAILY_STEPS_GOAL
import br.com.mwconsulting.championcompanion.core.model.LearnedStepLengthProfile
import br.com.mwconsulting.championcompanion.core.model.StepLengthCalibrationSummary
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import br.com.mwconsulting.championcompanion.core.model.StepLengthSource
import java.time.Instant

sealed interface LearnedStepLengthProfilesState {
    data object Loading : LearnedStepLengthProfilesState
    data class Available(val profiles: List<LearnedStepLengthProfile>) : LearnedStepLengthProfilesState
    data object Unavailable : LearnedStepLengthProfilesState
}

data class ProfileUiState(
    val isLoading: Boolean = true,
    val heightCentimeters: Double? = null,
    val heightInput: String = "",
    val dailyStepsGoal: Int = DEFAULT_DAILY_STEPS_GOAL,
    val dailyStepsGoalInput: String = DEFAULT_DAILY_STEPS_GOAL.toString(),
    val stepLengthMeters: Double? = null,
    val stepLengthSource: StepLengthSource? = null,
    val stepLengthUpdatedAt: Instant? = null,
    val learnedStepLengthProfiles: LearnedStepLengthProfilesState = LearnedStepLengthProfilesState.Loading,
    val calibrationHistory: List<StepLengthCalibrationSummary> = emptyList(),
    val correctionActivityId: WorkoutActivityId? = null,
    val correctionDistanceInput: String = "",
    val stepLengthInput: String = "",
    val heightSuggestionMeters: Double? = null,
    val isConfirmingHeightSuggestion: Boolean = false,
    val isSaving: Boolean = false,
    val message: ProfileMessage? = null,
)

enum class ProfileMessage {
    HEIGHT_SAVED,
    MANUAL_SAVED,
    HEIGHT_SUGGESTION_SAVED,
    DAILY_STEPS_GOAL_SAVED,
    HEIGHT_SAVE_FAILED,
    DAILY_STEPS_GOAL_SAVE_FAILED,
    INVALID_HEIGHT,
    INVALID_STEP_LENGTH,
    INVALID_DAILY_STEPS_GOAL,
    PROFILE_UNAVAILABLE,
    SAVE_FAILED,
    SUGGESTION_UNAVAILABLE,
}

sealed interface ProfileIntent {
    data object Load : ProfileIntent
    data class HeightChanged(val value: String) : ProfileIntent
    data object SaveHeight : ProfileIntent
    data class DailyStepsGoalChanged(val value: String) : ProfileIntent
    data object SaveDailyStepsGoal : ProfileIntent
    data class StepLengthChanged(val value: String) : ProfileIntent
    data object SaveManualStepLength : ProfileIntent
    data object AcceptHeightSuggestion : ProfileIntent
    data object ConfirmHeightSuggestion : ProfileIntent
    data object CancelHeightSuggestion : ProfileIntent
    data object DismissMessage : ProfileIntent
    data class RemoveCalibration(val activityId: br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId) : ProfileIntent
    data class StartCalibrationCorrection(val calibration: StepLengthCalibrationSummary) : ProfileIntent
    data class CalibrationCorrectionChanged(val value: String) : ProfileIntent
    data object SaveCalibrationCorrection : ProfileIntent
    data object CancelCalibrationCorrection : ProfileIntent
}

interface ProfileFeatureContract : UiStateStore<ProfileUiState> {
    fun onIntent(intent: ProfileIntent)
}
