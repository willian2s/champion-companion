package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.common.UiStateStore

data object HealthUiState

interface HealthFeatureContract : UiStateStore<HealthUiState>
