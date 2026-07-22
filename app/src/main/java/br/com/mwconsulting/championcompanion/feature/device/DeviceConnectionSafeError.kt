package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.toSafeMessage

data class DeviceConnectionSafeError(
    val category: DeviceConnectionSafeErrorCategory,
    val message: String,
    val isRecoverable: Boolean,
)

enum class DeviceConnectionSafeErrorCategory {
    PREREQUISITES,
    SCAN,
    CONNECTION,
    HANDSHAKE,
    INTERNAL,
}

fun ChampionBleError.toDeviceConnectionSafeError(): DeviceConnectionSafeError = DeviceConnectionSafeError(
    category = when (this) {
        ChampionBleError.BleNotSupported,
        ChampionBleError.BluetoothDisabled,
        ChampionBleError.ScanPermissionMissing,
        ChampionBleError.ConnectPermissionMissing,
        ChampionBleError.LegacyPermissionMissing,
        ChampionBleError.LocationPermissionRequired,
        -> DeviceConnectionSafeErrorCategory.PREREQUISITES

        ChampionBleError.ScanAlreadyInProgress,
        ChampionBleError.DeviceNotFound,
        is ChampionBleError.ScanFailed,
        -> DeviceConnectionSafeErrorCategory.SCAN

        is ChampionBleError.MissingOptionalChannel -> DeviceConnectionSafeErrorCategory.HANDSHAKE

        ChampionBleError.UnexpectedInternal -> DeviceConnectionSafeErrorCategory.INTERNAL

        is ChampionBleError.ConnectionFailed,
        is ChampionBleError.UnexpectedDisconnect,
        is ChampionBleError.DiscoveryFailed,
        is ChampionBleError.MissingRequiredService,
        is ChampionBleError.MissingRequiredChannel,
        is ChampionBleError.IncompatibleChannelProperties,
        is ChampionBleError.OperationRejected,
        is ChampionBleError.CallbackFailure,
        is ChampionBleError.Timeout,
        is ChampionBleError.QueueClosed,
        is ChampionBleError.OperationCancelled,
        is ChampionBleError.SubscriptionFailed,
        is ChampionBleError.InvalidPayload,
        is ChampionBleError.StaleSession,
        ChampionBleError.CleanupInProgress,
        ChampionBleError.OperationUnavailable,
        -> DeviceConnectionSafeErrorCategory.CONNECTION
    },
    message = toSafeMessage(),
    isRecoverable = isRecoverable,
)
