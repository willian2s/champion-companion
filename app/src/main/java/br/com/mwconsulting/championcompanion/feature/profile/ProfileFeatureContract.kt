package br.com.mwconsulting.championcompanion.feature.profile

import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import br.com.mwconsulting.championcompanion.core.model.StepLengthSource
import java.time.Instant

data class ProfileUiState(
    val isLoading: Boolean = true,
    val heightCentimeters: Double? = null,
    val stepLengthMeters: Double? = null,
    val stepLengthSource: StepLengthSource? = null,
    val stepLengthUpdatedAt: Instant? = null,
    val stepLengthInput: String = "",
    val heightSuggestionMeters: Double? = null,
    val isConfirmingHeightSuggestion: Boolean = false,
    val isSaving: Boolean = false,
    val message: ProfileMessage? = null,
)

enum class ProfileMessage {
    MANUAL_SAVED,
    HEIGHT_SUGGESTION_SAVED,
    INVALID_STEP_LENGTH,
    PROFILE_UNAVAILABLE,
    SAVE_FAILED,
    SUGGESTION_UNAVAILABLE,
}

sealed interface ProfileIntent {
    data object Load : ProfileIntent
    data class StepLengthChanged(val value: String) : ProfileIntent
    data object SaveManualStepLength : ProfileIntent
    data object AcceptHeightSuggestion : ProfileIntent
    data object ConfirmHeightSuggestion : ProfileIntent
    data object CancelHeightSuggestion : ProfileIntent
    data object DismissMessage : ProfileIntent
}

interface ProfileFeatureContract : UiStateStore<ProfileUiState> {
    fun onIntent(intent: ProfileIntent)
}
