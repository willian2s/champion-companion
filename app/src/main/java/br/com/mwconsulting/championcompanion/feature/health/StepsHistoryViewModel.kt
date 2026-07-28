package br.com.mwconsulting.championcompanion.feature.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mwconsulting.championcompanion.core.model.CalendarHistoryPeriod
import br.com.mwconsulting.championcompanion.core.model.CalendarHistoryQuery
import br.com.mwconsulting.championcompanion.core.model.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
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
class StepsHistoryViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
) : ViewModel(), StepsHistoryFeatureContract {
    private val zoneId = ZoneId.systemDefault()
    private val mutableState = MutableStateFlow(StepsHistoryUiState())
    private var observation: Job? = null

    override val state: StateFlow<StepsHistoryUiState> = mutableState.asStateFlow()

    override fun onIntent(intent: StepsHistoryIntent) {
        when (intent) {
            StepsHistoryIntent.Load -> observePeriod(mutableState.value.selectedPeriod)
            is StepsHistoryIntent.SelectPeriod -> observePeriod(intent.period)
            StepsHistoryIntent.Retry -> observePeriod(mutableState.value.selectedPeriod)
            StepsHistoryIntent.DismissError -> mutableState.update { it.copy(error = null) }
        }
    }

    override fun onCleared() {
        observation?.cancel()
        super.onCleared()
    }

    private fun observePeriod(period: CalendarHistoryPeriod) {
        observation?.cancel()
        mutableState.value = StepsHistoryUiState(
            selectedPeriod = period,
            isLoading = true,
        )
        observation = viewModelScope.launch {
            try {
                calendarRepository.observeStepsHistory(
                    CalendarHistoryQuery(
                        period = period,
                        // Include today while preserving [fromInclusive, untilExclusive).
                        untilExclusive = LocalDate.now(zoneId).plusDays(1),
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
                            error = throwable.toStepsHistoryUiError(),
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun Throwable.toStepsHistoryUiError(): StepsHistoryUiError =
        StepsHistoryUiError(
            category = if (this is IllegalArgumentException) {
                StepsHistoryUiErrorCategory.INVALID_LOCAL_DATA
            } else {
                StepsHistoryUiErrorCategory.SOURCE_UNAVAILABLE
            },
        )
}
