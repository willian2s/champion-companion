package br.com.mwconsulting.championcompanion.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mwconsulting.championcompanion.core.model.CalendarDataIssue
import br.com.mwconsulting.championcompanion.core.model.CalendarDataIssueReason
import br.com.mwconsulting.championcompanion.core.model.CalendarMonthQuery
import br.com.mwconsulting.championcompanion.core.model.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
) : ViewModel(), CalendarFeatureContract {
    private val zoneId = ZoneId.systemDefault()
    private val initialDate = LocalDate.now(zoneId)
    private val mutableState = MutableStateFlow(
        CalendarUiState(
            selectedMonth = YearMonth.from(initialDate),
            selectedDate = initialDate,
            zoneId = zoneId,
        ),
    )
    private var monthObservation: Job? = null
    private var dayObservation: Job? = null

    override val state: StateFlow<CalendarUiState> = mutableState.asStateFlow()

    override fun onIntent(intent: CalendarIntent) {
        when (intent) {
            CalendarIntent.Load -> loadMonth()
            CalendarIntent.PreviousMonth -> changeMonth { minusMonths(1) }
            CalendarIntent.NextMonth -> changeMonth { plusMonths(1) }
            CalendarIntent.GoToToday -> goToToday()
            is CalendarIntent.SelectDate -> selectDate(intent.localDate)
            is CalendarIntent.OpenDate -> openDate(intent.localDate)
            CalendarIntent.Retry -> retry()
            CalendarIntent.DismissError -> mutableState.update { it.copy(error = null) }
        }
    }

    override fun onCleared() {
        monthObservation?.cancel()
        dayObservation?.cancel()
        super.onCleared()
    }

    private fun loadMonth() {
        startMonthObservation(mutableState.value.selectedMonth)
    }

    private fun changeMonth(change: YearMonth.() -> YearMonth) {
        val current = mutableState.value
        val month = current.selectedMonth.change()
        val selectedDate = current.selectedDate?.let { date ->
            month.atDay(date.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
        }
        dayObservation?.cancel()
        mutableState.value = current.copy(
            selectedMonth = month,
            selectedDate = selectedDate,
            monthSnapshot = null,
            dayDetail = null,
            monthLoading = false,
            dayLoading = false,
            error = null,
        )
        startMonthObservation(month)
    }

    private fun goToToday() {
        val today = LocalDate.now(zoneId)
        val current = mutableState.value
        dayObservation?.cancel()
        mutableState.value = current.copy(
            selectedMonth = YearMonth.from(today),
            selectedDate = today,
            monthSnapshot = null,
            dayDetail = null,
            monthLoading = false,
            dayLoading = false,
            error = null,
        )
        startMonthObservation(YearMonth.from(today))
    }

    private fun selectDate(localDate: LocalDate) {
        val current = mutableState.value
        if (YearMonth.from(localDate) != current.selectedMonth) {
            mutableState.update {
                it.copy(
                    error = CalendarUiError(
                        category = CalendarUiErrorCategory.INVALID_DATE,
                        scope = CalendarUiErrorScope.MONTH,
                        isRetryable = false,
                    ),
                )
            }
            return
        }

        dayObservation?.cancel()
        mutableState.value = current.copy(
            selectedDate = localDate,
            dayDetail = null,
            dayLoading = false,
            error = null,
        )
    }

    private fun openDate(localDate: LocalDate) {
        val current = mutableState.value
        if (YearMonth.from(localDate) != current.selectedMonth) {
            mutableState.update {
                it.copy(
                    error = CalendarUiError(
                        category = CalendarUiErrorCategory.INVALID_DATE,
                        scope = CalendarUiErrorScope.DAY,
                        isRetryable = false,
                    ),
                )
            }
            return
        }

        dayObservation?.cancel()
        mutableState.value = current.copy(
            selectedDate = localDate,
            dayDetail = null,
            dayLoading = true,
            error = null,
        )
        dayObservation = viewModelScope.launch {
            try {
                calendarRepository.observeDay(localDate, zoneId).collect { detail ->
                    mutableState.update { state ->
                        if (state.selectedDate == localDate && state.selectedMonth == YearMonth.from(localDate)) {
                            state.copy(
                                dayDetail = detail,
                                dayLoading = false,
                                error = detail.issues.toUiError(CalendarUiErrorScope.DAY)
                                    ?: state.error?.takeUnless { it.scope == CalendarUiErrorScope.DAY },
                            )
                        } else {
                            state
                        }
                    }
                }
                mutableState.update { state ->
                    if (state.selectedDate == localDate) state.copy(dayLoading = false) else state
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                presentError(CalendarUiErrorScope.DAY, throwable)
            }
        }
    }

    private fun retry() {
        if (mutableState.value.error?.isRetryable == false) return
        when (mutableState.value.error?.scope) {
            CalendarUiErrorScope.DAY -> mutableState.value.selectedDate?.let(::openDate) ?: loadMonth()
            CalendarUiErrorScope.MONTH,
            null,
            -> loadMonth()
        }
    }

    private fun startMonthObservation(month: YearMonth) {
        monthObservation?.cancel()
        mutableState.update {
            it.copy(
                monthSnapshot = null,
                monthLoading = true,
                error = null,
            )
        }
        monthObservation = viewModelScope.launch {
            try {
                calendarRepository.observeMonth(CalendarMonthQuery(month, zoneId)).collect { snapshot ->
                    mutableState.update { state ->
                        if (state.selectedMonth == month) {
                            state.copy(
                                monthSnapshot = snapshot,
                                monthLoading = false,
                                error = snapshot.issues.toUiError(CalendarUiErrorScope.MONTH)
                                    ?: state.error?.takeUnless { it.scope == CalendarUiErrorScope.MONTH },
                            )
                        } else {
                            state
                        }
                    }
                }
                mutableState.update { state ->
                    if (state.selectedMonth == month) state.copy(monthLoading = false) else state
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                presentError(CalendarUiErrorScope.MONTH, throwable)
            }
        }
    }

    private fun presentError(scope: CalendarUiErrorScope, throwable: Throwable) {
        val category = if (throwable is IllegalArgumentException) {
            CalendarUiErrorCategory.INVALID_LOCAL_DATA
        } else {
            CalendarUiErrorCategory.SOURCE_UNAVAILABLE
        }
        mutableState.update {
            it.copy(
                monthLoading = if (scope == CalendarUiErrorScope.MONTH) false else it.monthLoading,
                dayLoading = if (scope == CalendarUiErrorScope.DAY) false else it.dayLoading,
                error = CalendarUiError(category = category, scope = scope),
            )
        }
    }

    private fun Set<CalendarDataIssue>.toUiError(scope: CalendarUiErrorScope): CalendarUiError? =
        firstOrNull()?.let { issue ->
            CalendarUiError(
                category = when (issue.reason) {
                    CalendarDataIssueReason.INVALID_LOCAL_DATA -> CalendarUiErrorCategory.INVALID_LOCAL_DATA
                    CalendarDataIssueReason.SOURCE_UNAVAILABLE,
                    CalendarDataIssueReason.UNRESOLVED_AGGREGATE,
                    -> CalendarUiErrorCategory.SOURCE_UNAVAILABLE
                },
                scope = scope,
            )
        }
}
