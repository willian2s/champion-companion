package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.common.UiStateStore

interface DeviceManagementFeatureContract : UiStateStore<DeviceManagementUiState> {
    fun onIntent(intent: DeviceManagementIntent)
}

sealed interface DeviceManagementIntent {
    data object Load : DeviceManagementIntent
    data object AddDevice : DeviceManagementIntent
    data object Retry : DeviceManagementIntent
    data object CancelScan : DeviceManagementIntent
    data class SelectCandidate(val candidateId: DeviceManagementCandidateId) : DeviceManagementIntent
    data object ConnectSelectedCandidate : DeviceManagementIntent
    data object ConnectAssociatedDevice : DeviceManagementIntent
    data object DisconnectDevice : DeviceManagementIntent
    data object ForgetDevice : DeviceManagementIntent
    data object ConfirmForgetDevice : DeviceManagementIntent
    data object DismissError : DeviceManagementIntent
    data object RefreshPrerequisites : DeviceManagementIntent
    data object OpenDiagnostics : DeviceManagementIntent
    data object ExportDiagnostics : DeviceManagementIntent
}

sealed interface DeviceManagementEvent {
    data class RequestPermissions(val permissions: Set<DeviceManagementPermission>) : DeviceManagementEvent
    data object OpenBluetoothSettings : DeviceManagementEvent
    data object ShowForgetConfirmation : DeviceManagementEvent
    data object OpenDiagnostics : DeviceManagementEvent
    data object ShareDiagnostics : DeviceManagementEvent
}

enum class DeviceManagementPermission {
    BLUETOOTH_SCAN,
    BLUETOOTH_CONNECT,
    ACCESS_FINE_LOCATION,
}
