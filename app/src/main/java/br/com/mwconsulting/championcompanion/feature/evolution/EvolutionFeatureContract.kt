package br.com.mwconsulting.championcompanion.feature.evolution

import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import br.com.mwconsulting.championcompanion.core.model.EvolutionPeriod
import br.com.mwconsulting.championcompanion.core.model.EvolutionQuery
import br.com.mwconsulting.championcompanion.core.model.EvolutionReadState
import br.com.mwconsulting.championcompanion.core.model.EvolutionSnapshot
import br.com.mwconsulting.championcompanion.core.model.EvolutionWorkoutFilter
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordsReadState
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordsSnapshot
import java.time.LocalDate
import java.time.ZoneId

interface EvolutionFeatureContract : UiStateStore<EvolutionUiState> {
    fun onIntent(intent: EvolutionIntent)
}

data class EvolutionUiState(
    val query: EvolutionQuery = defaultEvolutionQuery(),
    val selectedPeriod: EvolutionPeriod = query.period,
    val selectedFilter: EvolutionWorkoutFilter = query.filter,
    val readState: EvolutionReadState = EvolutionReadState.Loading(query),
    val previousReadState: EvolutionReadState = EvolutionReadState.Loading(query.previousQuery()),
    val snapshot: EvolutionSnapshot? = null,
    val personalRecordsState: PersonalRecordsReadState = PersonalRecordsReadState.Loading,
    val personalRecords: PersonalRecordsSnapshot? = null,
)

sealed interface EvolutionIntent {
    data object Load : EvolutionIntent
    data class SelectPeriod(val period: EvolutionPeriod) : EvolutionIntent
    data class SelectFilter(val filter: EvolutionWorkoutFilter) : EvolutionIntent
    data object Retry : EvolutionIntent
}

internal fun defaultEvolutionQuery(): EvolutionQuery {
    val zoneId = ZoneId.systemDefault()
    return EvolutionQuery(
        period = EvolutionPeriod.LAST_30_DAYS,
        untilExclusive = LocalDate.now(zoneId).plusDays(1),
        zoneId = zoneId,
        filter = EvolutionWorkoutFilter.Consolidated,
    )
}
