package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.common.UiStateStore

data object DeviceUiState

interface DeviceFeatureContract : UiStateStore<DeviceUiState>
