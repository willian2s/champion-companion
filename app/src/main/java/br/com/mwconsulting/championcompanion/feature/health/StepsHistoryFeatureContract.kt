package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import br.com.mwconsulting.championcompanion.core.model.CalendarHistoryPeriod
import br.com.mwconsulting.championcompanion.core.model.CalendarStepsHistorySnapshot

interface StepsHistoryFeatureContract : UiStateStore<StepsHistoryUiState> {
    fun onIntent(intent: StepsHistoryIntent)
}

data class StepsHistoryUiState(
    val selectedPeriod: CalendarHistoryPeriod = CalendarHistoryPeriod.LAST_7_DAYS,
    val snapshot: CalendarStepsHistorySnapshot? = null,
    val isLoading: Boolean = false,
    val error: StepsHistoryUiError? = null,
)

sealed interface StepsHistoryIntent {
    data object Load : StepsHistoryIntent
    data class SelectPeriod(val period: CalendarHistoryPeriod) : StepsHistoryIntent
    data object Retry : StepsHistoryIntent
    data object DismissError : StepsHistoryIntent
}

data class StepsHistoryUiError(
    val category: StepsHistoryUiErrorCategory,
    val isRetryable: Boolean = true,
)

enum class StepsHistoryUiErrorCategory {
    SOURCE_UNAVAILABLE,
    INVALID_LOCAL_DATA,
    INTERNAL,
}
