package br.com.mwconsulting.championcompanion.feature.profile

import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import br.com.mwconsulting.championcompanion.core.model.DEFAULT_DAILY_STEPS_GOAL

data class ProfileUiState(
    val isLoading: Boolean = true,
    val heightCentimeters: Double? = null,
    val heightInput: String = "",
    val dailyStepsGoal: Int = DEFAULT_DAILY_STEPS_GOAL,
    val dailyStepsGoalInput: String = DEFAULT_DAILY_STEPS_GOAL.toString(),
    val isSaving: Boolean = false,
    val message: ProfileMessage? = null,
    val maxHeartRateBpm: Int? = null,
    val maxHeartRateBpmInput: String = "",
)

enum class ProfileMessage {
    HEIGHT_SAVED,
    MAX_HEART_RATE_SAVED,
    DAILY_STEPS_GOAL_SAVED,
    HEIGHT_SAVE_FAILED,
    MAX_HEART_RATE_SAVE_FAILED,
    DAILY_STEPS_GOAL_SAVE_FAILED,
    INVALID_HEIGHT,
    INVALID_MAX_HEART_RATE,
    INVALID_DAILY_STEPS_GOAL,
    PROFILE_UNAVAILABLE,
}

sealed interface ProfileIntent {
    data object Load : ProfileIntent
    data class HeightChanged(val value: String) : ProfileIntent
    data object SaveHeight : ProfileIntent
    data class MaxHeartRateChanged(val value: String) : ProfileIntent
    data object SaveMaxHeartRate : ProfileIntent
    data class DailyStepsGoalChanged(val value: String) : ProfileIntent
    data object SaveDailyStepsGoal : ProfileIntent
    data object DismissMessage : ProfileIntent
}

interface ProfileFeatureContract : UiStateStore<ProfileUiState> {
    fun onIntent(intent: ProfileIntent)
}
