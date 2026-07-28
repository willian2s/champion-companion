package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import br.com.mwconsulting.championcompanion.core.model.CalendarDistanceHistorySnapshot
import br.com.mwconsulting.championcompanion.core.model.CalendarHistoryPeriod

interface DistanceHistoryFeatureContract : UiStateStore<DistanceHistoryUiState> {
    fun onIntent(intent: DistanceHistoryIntent)
}

data class DistanceHistoryUiState(
    val selectedPeriod: CalendarHistoryPeriod = CalendarHistoryPeriod.LAST_7_DAYS,
    val snapshot: CalendarDistanceHistorySnapshot? = null,
    val isLoading: Boolean = false,
    val error: DistanceHistoryUiError? = null,
)

sealed interface DistanceHistoryIntent {
    data object Load : DistanceHistoryIntent
    data class SelectPeriod(val period: CalendarHistoryPeriod) : DistanceHistoryIntent
    data object Retry : DistanceHistoryIntent
    data object DismissError : DistanceHistoryIntent
}

data class DistanceHistoryUiError(
    val category: DistanceHistoryUiErrorCategory,
    val isRetryable: Boolean = true,
)

enum class DistanceHistoryUiErrorCategory {
    SOURCE_UNAVAILABLE,
    INVALID_LOCAL_DATA,
    INTERNAL,
}
