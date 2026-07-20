package br.com.mwconsulting.championcompanion.feature.profile

import br.com.mwconsulting.championcompanion.core.common.UiStateStore

data object ProfileUiState

interface ProfileFeatureContract : UiStateStore<ProfileUiState>
