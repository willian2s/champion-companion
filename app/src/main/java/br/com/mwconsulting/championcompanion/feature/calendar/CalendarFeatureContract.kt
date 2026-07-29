package br.com.mwconsulting.championcompanion.feature.calendar

import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import br.com.mwconsulting.championcompanion.core.model.CalendarDaySummary
import br.com.mwconsulting.championcompanion.core.model.CalendarMonthSnapshot
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

interface CalendarFeatureContract : UiStateStore<CalendarUiState> {
    fun onIntent(intent: CalendarIntent)
}

data class CalendarUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = null,
    /** Zona congelada usada para consultas e snapshots desta instância de estado. */
    val zoneId: ZoneId = ZoneId.systemDefault(),
    val monthSnapshot: CalendarMonthSnapshot? = null,
    val dayDetail: CalendarDaySummary? = null,
    val monthLoading: Boolean = false,
    val dayLoading: Boolean = false,
    val error: CalendarUiError? = null,
) {
    init {
        require(selectedDate == null || YearMonth.from(selectedDate) == selectedMonth)
        monthSnapshot?.let {
            require(it.query.month == selectedMonth)
            require(it.query.zoneId == zoneId)
        }
        dayDetail?.let {
            require(selectedDate == it.localDate)
            require(it.zoneId == zoneId)
        }
    }
}

sealed interface CalendarIntent {
    data object Load : CalendarIntent
    data object PreviousMonth : CalendarIntent
    data object NextMonth : CalendarIntent
    data object GoToToday : CalendarIntent
    data class SelectDate(val localDate: LocalDate) : CalendarIntent
    data class OpenDate(val localDate: LocalDate) : CalendarIntent
    data object Retry : CalendarIntent
    data object DismissError : CalendarIntent
}

data class CalendarUiError(
    val category: CalendarUiErrorCategory,
    val scope: CalendarUiErrorScope,
    val isRetryable: Boolean = true,
)

enum class CalendarUiErrorCategory {
    SOURCE_UNAVAILABLE,
    INVALID_LOCAL_DATA,
    INVALID_DATE,
    INTERNAL,
}

enum class CalendarUiErrorScope {
    MONTH,
    DAY,
}
