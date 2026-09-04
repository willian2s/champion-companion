package br.com.mwconsulting.championcompanion.feature.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mwconsulting.championcompanion.core.model.CalendarDateRange
import br.com.mwconsulting.championcompanion.core.model.CalendarHistoryPeriod
import br.com.mwconsulting.championcompanion.core.model.CalendarHistoryQuery
import br.com.mwconsulting.championcompanion.core.model.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DistanceHistoryViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
) : ViewModel(), DistanceHistoryFeatureContract {
    private val zoneId = ZoneId.systemDefault()
    private val mutableState = MutableStateFlow(DistanceHistoryUiState())
    private var observation: Job? = null

    override val state: StateFlow<DistanceHistoryUiState> = mutableState.asStateFlow()

    override fun onIntent(intent: DistanceHistoryIntent) {
        when (intent) {
            DistanceHistoryIntent.Load -> observePeriod(mutableState.value.selectedPeriod)
            is DistanceHistoryIntent.SelectPeriod -> observePeriod(intent.period)
            DistanceHistoryIntent.Retry -> observePeriod(mutableState.value.selectedPeriod)
            DistanceHistoryIntent.DismissError -> mutableState.update { it.copy(error = null) }
        }
    }

    override fun onCleared() {
        observation?.cancel()
        super.onCleared()
    }

    private fun observePeriod(period: CalendarHistoryPeriod) {
        observation?.cancel()
        mutableState.value = DistanceHistoryUiState(
            selectedPeriod = period,
            isLoading = true,
        )
        observation = viewModelScope.launch {
            try {
                calendarRepository.observeDistanceHistory(
                    period.toDistanceHistoryQuery(
                        today = LocalDate.now(zoneId),
                        zoneId = zoneId,
                    ),
                ).collect { snapshot ->
                    mutableState.update { current ->
                        if (current.selectedPeriod == period) {
                            current.copy(snapshot = snapshot, isLoading = false, error = null)
                        } else {
                            current
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                mutableState.update { current ->
                    if (current.selectedPeriod == period) {
                        current.copy(
                            snapshot = null,
                            isLoading = false,
                            error = throwable.toDistanceHistoryUiError(),
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun CalendarHistoryPeriod.toDistanceHistoryQuery(
        today: LocalDate,
        zoneId: ZoneId,
    ): CalendarHistoryQuery {
        val civilRange = distanceHistoryDateRange(today)
        return CalendarHistoryQuery(
            period = this,
            untilExclusive = civilRange.untilExclusive,
            zoneId = zoneId,
            dateRangeOverride = civilRange,
        )
    }

    private fun Throwable.toDistanceHistoryUiError(): DistanceHistoryUiError =
        DistanceHistoryUiError(
            category = if (this is IllegalArgumentException) {
                DistanceHistoryUiErrorCategory.INVALID_LOCAL_DATA
            } else {
                DistanceHistoryUiErrorCategory.SOURCE_UNAVAILABLE
            },
        )
}

internal fun CalendarHistoryPeriod.distanceHistoryDateRange(today: LocalDate): CalendarDateRange {
    val currentMonth = YearMonth.from(today)
    return when (this) {
        CalendarHistoryPeriod.LAST_7_DAYS -> currentCivilWeekRange(today)
        CalendarHistoryPeriod.LAST_30_DAYS -> previousClosedMonthRange(currentMonth)
        CalendarHistoryPeriod.LAST_90_DAYS -> CalendarDateRange(
            fromInclusive = currentMonth.minusMonths(3).atDay(1),
            untilExclusive = currentMonth.atDay(1),
        )
        CalendarHistoryPeriod.LAST_365_DAYS -> CalendarDateRange(
            fromInclusive = currentMonth.minusMonths(12).atDay(1),
            untilExclusive = currentMonth.atDay(1),
        )
    }
}

internal fun currentCivilWeekRange(today: LocalDate): CalendarDateRange {
    val sunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    return CalendarDateRange(
        fromInclusive = sunday,
        untilExclusive = sunday.plusWeeks(1),
    )
}

private fun previousClosedMonthRange(currentMonth: YearMonth): CalendarDateRange {
    val previousMonth = currentMonth.minusMonths(1)
    return CalendarDateRange(
        fromInclusive = previousMonth.atDay(1),
        untilExclusive = currentMonth.atDay(1),
    )
}
