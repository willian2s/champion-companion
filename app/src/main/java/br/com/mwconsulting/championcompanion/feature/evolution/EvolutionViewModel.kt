package br.com.mwconsulting.championcompanion.feature.evolution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mwconsulting.championcompanion.core.model.EvolutionReadFailure
import br.com.mwconsulting.championcompanion.core.model.EvolutionReadModel
import br.com.mwconsulting.championcompanion.core.model.EvolutionReadState
import br.com.mwconsulting.championcompanion.core.model.EvolutionPeriod
import br.com.mwconsulting.championcompanion.core.model.EvolutionQuery
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordsReadState
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordsSnapshot
import br.com.mwconsulting.championcompanion.core.model.EvolutionWorkoutFilter
import br.com.mwconsulting.championcompanion.data.toEvolutionSnapshot
import br.com.mwconsulting.championcompanion.data.toPersonalRecordsSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class EvolutionViewModel @Inject constructor(
    private val evolutionReadModel: EvolutionReadModel,
) : ViewModel(), EvolutionFeatureContract {
    private val zoneId = ZoneId.systemDefault()
    private val mutableState = MutableStateFlow(EvolutionUiState())
    private val selectedPeriod = MutableStateFlow(EvolutionPeriod.LAST_30_DAYS)
    private val selectedFilter = MutableStateFlow<EvolutionWorkoutFilter>(EvolutionWorkoutFilter.Consolidated)
    private val reloadSignal = MutableStateFlow(0)

    override val state: StateFlow<EvolutionUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(selectedPeriod, selectedFilter, reloadSignal) { period, filter, _ ->
                EvolutionQuery(
                    period = period,
                    untilExclusive = LocalDate.now(zoneId).plusDays(1),
                    zoneId = zoneId,
                    filter = filter,
                )
            }
                .flatMapLatest(::observeQuery)
                .collect { mutableState.value = it }
        }
    }

    override fun onIntent(intent: EvolutionIntent) {
        when (intent) {
            EvolutionIntent.Load,
            EvolutionIntent.Retry,
            -> reloadSignal.update { it + 1 }

            is EvolutionIntent.SelectPeriod -> selectedPeriod.value = intent.period
            is EvolutionIntent.SelectFilter -> selectedFilter.value = intent.filter
        }
    }

    private fun observeQuery(query: EvolutionQuery): Flow<EvolutionUiState> {
        val current = observeSnapshotSafely(query)
        val previous = observeSnapshotSafely(query.previousQuery())
        val records = observePersonalRecordsSafely()

        return combine(current, previous, records) { currentState, previousState, recordsState ->
            buildUiState(
                query = query,
                currentState = currentState,
                previousState = previousState,
                recordsState = recordsState,
            )
        }.onStart {
            emit(
                EvolutionUiState(
                    query = query,
                    selectedPeriod = query.period,
                    selectedFilter = query.filter,
                    readState = EvolutionReadState.Loading(query),
                    previousReadState = EvolutionReadState.Loading(query.previousQuery()),
                    personalRecordsState = PersonalRecordsReadState.Loading,
                ),
            )
        }
    }

    private fun observeSnapshotSafely(query: EvolutionQuery): Flow<EvolutionReadState> = flow {
        emitAll(evolutionReadModel.observeSnapshot(query))
    }
        .onStart { emit(EvolutionReadState.Loading(query)) }
        .catch { error ->
            if (error is CancellationException) throw error
            val reason = error.toEvolutionReadFailure()
            emit(
                EvolutionReadState.Failed(
                    query = query,
                    reason = reason,
                    retryable = reason == EvolutionReadFailure.SOURCE_UNAVAILABLE,
                ),
            )
        }

    private fun observePersonalRecordsSafely(): Flow<PersonalRecordsReadState> = flow {
        emitAll(evolutionReadModel.observePersonalRecordsSnapshot())
    }
        .onStart { emit(PersonalRecordsReadState.Loading) }
        .catch { error ->
            if (error is CancellationException) throw error
            val reason = error.toEvolutionReadFailure()
            emit(
                PersonalRecordsReadState.Failed(
                    reason = reason,
                    retryable = reason == EvolutionReadFailure.SOURCE_UNAVAILABLE,
                ),
            )
        }

    private fun buildUiState(
        query: EvolutionQuery,
        currentState: EvolutionReadState,
        previousState: EvolutionReadState,
        recordsState: PersonalRecordsReadState,
    ): EvolutionUiState {
        var effectiveRecordsState = recordsState
        val personalRecords = when (recordsState) {
            is PersonalRecordsReadState.Ready -> runCatching {
                recordsState.snapshot.toPersonalRecordsSnapshot()
            }.getOrElse {
                effectiveRecordsState = PersonalRecordsReadState.Failed(
                    reason = EvolutionReadFailure.INVALID_LOCAL_DATA,
                    retryable = false,
                )
                null
            }

            PersonalRecordsReadState.Loading,
            PersonalRecordsReadState.Empty,
            is PersonalRecordsReadState.Failed,
            -> null
        }

        var effectiveCurrentState = currentState
        val snapshot = if (currentState is EvolutionReadState.Ready) {
            runCatching {
                currentState.snapshot.toEvolutionSnapshot(
                    previous = (previousState as? EvolutionReadState.Ready)?.snapshot,
                ).copy(
                    personalRecords = personalRecords ?: PersonalRecordsSnapshot(),
                )
            }.getOrElse {
                effectiveCurrentState = EvolutionReadState.Failed(
                    query = query,
                    reason = EvolutionReadFailure.INVALID_LOCAL_DATA,
                    retryable = false,
                )
                null
            }
        } else {
            null
        }

        return EvolutionUiState(
            query = query,
            selectedPeriod = query.period,
            selectedFilter = query.filter,
            readState = effectiveCurrentState,
            previousReadState = previousState,
            snapshot = snapshot,
            personalRecordsState = effectiveRecordsState,
            personalRecords = personalRecords,
        )
    }

    private fun Throwable.toEvolutionReadFailure(): EvolutionReadFailure =
        if (this is IllegalArgumentException) {
            EvolutionReadFailure.INVALID_LOCAL_DATA
        } else {
            EvolutionReadFailure.SOURCE_UNAVAILABLE
        }
}
