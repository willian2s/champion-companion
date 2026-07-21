package br.com.mwconsulting.championcompanion.core.bluetooth

import br.com.mwconsulting.championcompanion.core.logging.LogEvent
import br.com.mwconsulting.championcompanion.core.logging.StructuredLogger

class ChampionProtocolLogger(
    private val logger: StructuredLogger,
    private val diagnosticPacketSink: ChampionDiagnosticPacketSink = NoOpChampionDiagnosticPacketSink,
) {
    fun logPacketObserved(
        packet: ChampionPacket,
        consumedByMatcher: Boolean,
    ) {
        logger.debug(
            event = LogEvent.BlePacketObserved,
            metadata = mapOf(
                "session_id" to packet.sessionId.value.toString(),
                "channel" to packet.channel.name.lowercase(),
                "direction" to packet.direction.name.lowercase(),
                "command_code" to packet.commandCode?.toString(),
                "byte_count" to packet.size.toString(),
                "consumed" to consumedByMatcher.toString(),
            ),
        )

        diagnosticPacketSink.capture(
            ChampionDiagnosticPacketCapture(
                sessionId = packet.sessionId.value.toString(),
                channel = packet.channel,
                direction = packet.direction,
                observedAt = packet.observedAt,
                commandCode = packet.commandCode,
                bytes = packet.bytesCopy(),
            ),
        )
    }

    fun logTransportState(
        sessionId: ChampionSessionId,
        state: String,
        error: ChampionBleError?,
    ) {
        logger.debug(
            event = LogEvent.BleTransportState,
            metadata = mapOf(
                "session_id" to sessionId.value.toString(),
                "state" to state,
                "error_code" to error?.logCode,
            ),
        )
    }

    fun logOperationResult(
        sessionId: ChampionSessionId,
        operation: ChampionGattOperation,
        result: String,
        error: ChampionBleError? = null,
    ) {
        logger.debug(
            event = LogEvent.BleOperationResult,
            metadata = mapOf(
                "session_id" to sessionId.value.toString(),
                "operation_id" to operation.operationId.value.toString(),
                "operation_type" to operation.type.name.lowercase(),
                "channel" to operation.target.channelAliasOrNull()?.name?.lowercase(),
                "byte_count" to operation.payloadSize?.toString(),
                "result" to result,
                "error_code" to error?.logCode,
            ),
        )
    }

    private val ChampionBleError.logCode: String
        get() = when (this) {
            ChampionBleError.BleNotSupported -> "ble_not_supported"
            ChampionBleError.BluetoothDisabled -> "bluetooth_disabled"
            ChampionBleError.ScanPermissionMissing -> "scan_permission_missing"
            ChampionBleError.ConnectPermissionMissing -> "connect_permission_missing"
            ChampionBleError.LegacyPermissionMissing -> "legacy_permission_missing"
            ChampionBleError.LocationPermissionRequired -> "location_permission_required"
            ChampionBleError.ScanAlreadyInProgress -> "scan_already_in_progress"
            ChampionBleError.OperationUnavailable -> "operation_unavailable"
            ChampionBleError.DeviceNotFound -> "device_not_found"
            is ChampionBleError.ScanFailed -> "scan_failed"
            is ChampionBleError.ConnectionFailed -> "connection_failed"
            is ChampionBleError.UnexpectedDisconnect -> "unexpected_disconnect"
            is ChampionBleError.DiscoveryFailed -> "discovery_failed"
            is ChampionBleError.MissingRequiredService -> "missing_required_service"
            is ChampionBleError.MissingRequiredChannel -> "missing_required_channel"
            is ChampionBleError.IncompatibleChannelProperties -> "incompatible_channel_properties"
            is ChampionBleError.OperationRejected -> "operation_rejected"
            is ChampionBleError.CallbackFailure -> "callback_failure"
            is ChampionBleError.Timeout -> "timeout"
            is ChampionBleError.QueueClosed -> "queue_closed"
            is ChampionBleError.OperationCancelled -> "operation_cancelled"
            is ChampionBleError.SubscriptionFailed -> "subscription_failed"
            is ChampionBleError.InvalidPayload -> "invalid_payload"
            is ChampionBleError.StaleSession -> "stale_session"
            ChampionBleError.CleanupInProgress -> "cleanup_in_progress"
            ChampionBleError.UnexpectedInternal -> "unexpected_internal"
        }

    private fun ChampionGattTarget.channelAliasOrNull(): ChampionChannelAlias? = when (this) {
        is ChampionGattTarget.Channel -> alias
        ChampionGattTarget.Session -> null
    }
}
