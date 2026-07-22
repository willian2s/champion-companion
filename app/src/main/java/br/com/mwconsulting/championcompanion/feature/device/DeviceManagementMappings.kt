package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.toSafeMessage

fun ChampionBleState.toDeviceManagementConnectionState(): DeviceManagementConnectionState = when (this) {
    ChampionBleState.Disconnected -> DeviceManagementConnectionState.DISCONNECTED
    is ChampionBleState.Scanning -> DeviceManagementConnectionState.SCANNING
    is ChampionBleState.Connecting -> DeviceManagementConnectionState.CONNECTING
    is ChampionBleState.DiscoveringServices,
    is ChampionBleState.ResolvingChannels,
    is ChampionBleState.ConfiguringSubscriptions,
    is ChampionBleState.TransportReady,
    -> DeviceManagementConnectionState.PREPARING_CONNECTION

    is ChampionBleState.Disconnecting -> DeviceManagementConnectionState.DISCONNECTING
    is ChampionBleState.Failed -> DeviceManagementConnectionState.FAILED
}

fun ChampionBleError.toDeviceManagementError(): DeviceManagementError = DeviceManagementError(
    category = toDeviceManagementErrorCategory(),
    message = toSafeMessage(),
    isRecoverable = isRecoverable,
)

fun ChampionBleError.toDeviceManagementRetryAction(): DeviceManagementRetryAction? = when (this) {
    ChampionBleError.ScanPermissionMissing,
    ChampionBleError.ConnectPermissionMissing,
    ChampionBleError.LegacyPermissionMissing,
    ChampionBleError.LocationPermissionRequired,
    -> DeviceManagementRetryAction.RequestPermissions

    ChampionBleError.BluetoothDisabled -> DeviceManagementRetryAction.EnableBluetooth
    ChampionBleError.DeviceNotFound,
    ChampionBleError.ScanAlreadyInProgress,
    is ChampionBleError.ScanFailed,
    -> DeviceManagementRetryAction.RestartScan

    is ChampionBleError.ConnectionFailed,
    is ChampionBleError.UnexpectedDisconnect,
    is ChampionBleError.DiscoveryFailed,
    is ChampionBleError.OperationRejected,
    is ChampionBleError.CallbackFailure,
    is ChampionBleError.Timeout,
    is ChampionBleError.SubscriptionFailed,
    is ChampionBleError.InvalidPayload,
    ChampionBleError.OperationUnavailable,
    ChampionBleError.CleanupInProgress,
    -> DeviceManagementRetryAction.ReconnectAssociatedDevice

    is ChampionBleError.MissingOptionalChannel -> DeviceManagementRetryAction.RetryHandshake

    ChampionBleError.BleNotSupported,
    is ChampionBleError.MissingRequiredService,
    is ChampionBleError.MissingRequiredChannel,
    is ChampionBleError.IncompatibleChannelProperties,
    is ChampionBleError.QueueClosed,
    is ChampionBleError.OperationCancelled,
    is ChampionBleError.StaleSession,
    ChampionBleError.UnexpectedInternal,
    -> null
}

private fun ChampionBleError.toDeviceManagementErrorCategory(): DeviceManagementErrorCategory = when (this) {
    ChampionBleError.BleNotSupported,
    ChampionBleError.BluetoothDisabled,
    ChampionBleError.ScanPermissionMissing,
    ChampionBleError.ConnectPermissionMissing,
    ChampionBleError.LegacyPermissionMissing,
    ChampionBleError.LocationPermissionRequired,
    -> DeviceManagementErrorCategory.PREREQUISITES

    ChampionBleError.ScanAlreadyInProgress,
    ChampionBleError.DeviceNotFound,
    is ChampionBleError.ScanFailed,
    -> DeviceManagementErrorCategory.SCAN

    is ChampionBleError.ConnectionFailed,
    is ChampionBleError.UnexpectedDisconnect,
    is ChampionBleError.DiscoveryFailed,
    is ChampionBleError.MissingRequiredService,
    is ChampionBleError.MissingRequiredChannel,
    is ChampionBleError.IncompatibleChannelProperties,
    is ChampionBleError.OperationRejected,
    is ChampionBleError.CallbackFailure,
    is ChampionBleError.Timeout,
    is ChampionBleError.SubscriptionFailed,
    is ChampionBleError.InvalidPayload,
    is ChampionBleError.QueueClosed,
    is ChampionBleError.OperationCancelled,
    is ChampionBleError.StaleSession,
    ChampionBleError.CleanupInProgress,
    ChampionBleError.OperationUnavailable,
    -> DeviceManagementErrorCategory.CONNECTION

    is ChampionBleError.MissingOptionalChannel -> DeviceManagementErrorCategory.HANDSHAKE
    ChampionBleError.UnexpectedInternal -> DeviceManagementErrorCategory.INTERNAL
}
