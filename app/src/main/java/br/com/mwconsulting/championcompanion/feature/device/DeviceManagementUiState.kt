package br.com.mwconsulting.championcompanion.feature.device

import java.time.Instant

@JvmInline
value class DeviceManagementDeviceId(val value: String)

@JvmInline
value class DeviceManagementCandidateId(val value: String)

data class DeviceManagementUiState(
    val screenState: DeviceManagementScreenState = DeviceManagementScreenState.Loading,
    val associatedDevice: DeviceManagementAssociatedDevice? = null,
    val selectedCandidateId: DeviceManagementCandidateId? = null,
    val candidates: List<DeviceManagementScanCandidate> = emptyList(),
    val connectionState: DeviceManagementConnectionState = DeviceManagementConnectionState.DISCONNECTED,
    val activeOperation: DeviceManagementActiveOperation? = null,
    val error: DeviceManagementError? = null,
    val retryAction: DeviceManagementRetryAction? = null,
)

sealed interface DeviceManagementScreenState {
    data object Loading : DeviceManagementScreenState
    data object NoAssociatedDevice : DeviceManagementScreenState
    data object PermissionsRequired : DeviceManagementScreenState
    data object BluetoothDisabled : DeviceManagementScreenState
    data object ReadyToScan : DeviceManagementScreenState
    data object Scanning : DeviceManagementScreenState
    data object ScanResults : DeviceManagementScreenState
    data object Connecting : DeviceManagementScreenState
    data object PreparingConnection : DeviceManagementScreenState
    data object Connected : DeviceManagementScreenState
    data object Disconnecting : DeviceManagementScreenState
    data object Disconnected : DeviceManagementScreenState
    data object DeviceNotFound : DeviceManagementScreenState
    data object ConnectionFailed : DeviceManagementScreenState
    data object Forgetting : DeviceManagementScreenState
    data object Error : DeviceManagementScreenState
}

data class DeviceManagementAssociatedDevice(
    val deviceId: DeviceManagementDeviceId,
    val displayName: String,
    val modelName: String,
    val safeIdentifier: String,
    val firmware: String?,
    val associatedAt: Instant,
    val lastSuccessfulConnectionAt: Instant?,
    val lastImportedAt: Instant?,
)

data class DeviceManagementScanCandidate(
    val candidateId: DeviceManagementCandidateId,
    val displayName: String,
    val safeIdentifier: String,
    val signalLabel: String?,
    val isSelected: Boolean = false,
)

enum class DeviceManagementConnectionState {
    DISCONNECTED,
    SEARCHING_REMEMBERED_DEVICE,
    SCANNING,
    CONNECTING,
    PREPARING_CONNECTION,
    CONNECTED,
    DISCONNECTING,
    FAILED,
}

enum class DeviceManagementActiveOperation {
    REQUESTING_PERMISSIONS,
    SCANNING,
    CONNECTING,
    DISCONNECTING,
    FORGETTING,
    OPENING_DIAGNOSTICS,
    EXPORTING_DIAGNOSTICS,
}

data class DeviceManagementError(
    val category: DeviceManagementErrorCategory,
    val message: String,
    val isRecoverable: Boolean,
)

enum class DeviceManagementErrorCategory {
    PREREQUISITES,
    SCAN,
    CONNECTION,
    HANDSHAKE,
    DISCONNECT,
    FORGET,
    DIAGNOSTICS,
    INTERNAL,
}

sealed interface DeviceManagementRetryAction {
    data object RequestPermissions : DeviceManagementRetryAction
    data object EnableBluetooth : DeviceManagementRetryAction
    data object RestartScan : DeviceManagementRetryAction
    data object ReconnectAssociatedDevice : DeviceManagementRetryAction
    data object RetrySelectedCandidateConnection : DeviceManagementRetryAction
    data object RetryHandshake : DeviceManagementRetryAction
    data object RetryDisconnect : DeviceManagementRetryAction
}
