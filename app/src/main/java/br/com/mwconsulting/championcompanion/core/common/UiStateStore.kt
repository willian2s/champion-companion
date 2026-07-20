package br.com.mwconsulting.championcompanion.core.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Read-only state contract for presentation layers. */
interface UiStateStore<out State> {
    val state: StateFlow<State>
}

/** Minimal in-memory implementation for foundation-level UI state. */
class MutableUiStateStore<State>(initialState: State) : UiStateStore<State> {
    private val mutableState = MutableStateFlow(initialState)

    override val state: StateFlow<State> = mutableState.asStateFlow()
}
