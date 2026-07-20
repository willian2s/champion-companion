package br.com.mwconsulting.championcompanion.feature.settings

import br.com.mwconsulting.championcompanion.core.common.UiStateStore

data object SettingsUiState

interface SettingsFeatureContract : UiStateStore<SettingsUiState>
