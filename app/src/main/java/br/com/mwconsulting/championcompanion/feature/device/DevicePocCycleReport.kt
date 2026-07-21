package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Evidence deliberately excludes identifiers, raw protocol data, exact steps and exact BPM. */
data class DevicePocCycleReport(
    val schemaVersion: Int,
    val cycleId: String,
    val startedAt: String,
    val finishedAt: String,
    val durationSeconds: Long,
    val stateSequence: List<String>,
    val transportReady: Boolean,
    val handshakeConfirmed: Boolean,
    val stepsCapability: String,
    val heartRateCapability: String,
    val stepsPacketCount: Int,
    val d0PacketCount: Int,
    val f002PacketCount: Int,
    val f002BufferedSampleCount: Int,
    val heartRateResult: String,
    val heartRateSource: String,
    val errorCategory: String?,
) {
    fun asJson(): String = buildString {
        append("{\n")
        field("schema_version", schemaVersion.toString(), false)
        field("cycle_id", cycleId)
        field("started_at", startedAt)
        field("finished_at", finishedAt)
        field("duration_seconds", durationSeconds.toString(), false)
        append("  \"state_sequence\": [")
        stateSequence.joinTo(this, separator = ", ") { quote(it) }
        append("],\n")
        field("transport_ready", transportReady.toString(), false)
        field("handshake_confirmed", handshakeConfirmed.toString(), false)
        field("steps_capability", stepsCapability)
        field("heart_rate_capability", heartRateCapability)
        field("steps_packet_count", stepsPacketCount.toString(), false)
        field("d0_packet_count", d0PacketCount.toString(), false)
        field("f002_packet_count", f002PacketCount.toString(), false)
        field("f002_buffered_sample_count", f002BufferedSampleCount.toString(), false)
        field("heart_rate_result", heartRateResult)
        field("heart_rate_source", heartRateSource)
        field("error_category", errorCategory?.let(::quote) ?: "null", false, last = true)
        append("}\n")
    }

    private fun StringBuilder.field(name: String, value: String, quoteValue: Boolean = true, last: Boolean = false) {
        append("  \"").append(name).append("\": ")
        append(if (quoteValue) quote(value) else value)
        append(if (last) "\n" else ",\n")
    }

    companion object {
        const val SCHEMA_VERSION = 1
        private val formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

        fun fromState(state: DeviceUiState, finishedAt: Instant = Instant.now()): DevicePocCycleReport {
            val duration = Duration.between(state.cycleStartedAt, finishedAt).seconds.coerceAtLeast(0)
            return DevicePocCycleReport(
                schemaVersion = SCHEMA_VERSION,
                cycleId = state.cycleId.value.toString(),
                startedAt = formatter.format(state.cycleStartedAt),
                finishedAt = formatter.format(finishedAt),
                durationSeconds = duration,
                stateSequence = state.pocStateHistory,
                transportReady = state.transportState is br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState.TransportReady ||
                    state.pocStateHistory.contains("transport_ready"),
                handshakeConfirmed = state.pocStateHistory.contains("handshake_confirmed") || state.pocState in setOf(
                    DevicePocState.HandshakeConfirmed, DevicePocState.WaitingForSteps, DevicePocState.StepsObserved,
                    DevicePocState.StartingHeartRate, DevicePocState.MeasuringHeartRate,
                    DevicePocState.StoppingHeartRate, DevicePocState.CycleCompleted,
                ),
                stepsCapability = capabilityLabel(state.isStepsCapabilityAvailable, state.isStepsCapabilityMissing),
                heartRateCapability = capabilityLabel(state.isHeartRateCapabilityAvailable, state.isHeartRateCapabilityMissing),
                stepsPacketCount = state.stepsPacketCount,
                d0PacketCount = state.d0PacketCount,
                f002PacketCount = state.f002PacketCount,
                f002BufferedSampleCount = state.f002BufferedSampleCount,
                heartRateResult = if (state.finalHeartRateBpm != null) "available" else "unavailable",
                heartRateSource = state.finalHeartRateSource.name.lowercase(),
                errorCategory = state.lastError?.reportCategory(),
            )
        }

        private fun capabilityLabel(available: Boolean, missing: Boolean) = when {
            available -> "available"
            missing -> "missing"
            else -> "not_observed"
        }

        private fun quote(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }
}

private fun ChampionBleError.reportCategory(): String = when (this) {
    ChampionBleError.BleNotSupported -> "ble_not_supported"
    ChampionBleError.BluetoothDisabled -> "bluetooth_disabled"
    ChampionBleError.ScanPermissionMissing, ChampionBleError.LegacyPermissionMissing,
    ChampionBleError.ConnectPermissionMissing, ChampionBleError.LocationPermissionRequired -> "permission_missing"
    ChampionBleError.DeviceNotFound -> "device_not_found"
    is ChampionBleError.ScanFailed -> "scan_failed"
    is ChampionBleError.ConnectionFailed -> "connection_failed"
    is ChampionBleError.UnexpectedDisconnect -> "unexpected_disconnect"
    is ChampionBleError.DiscoveryFailed -> "discovery_failed"
    is ChampionBleError.MissingRequiredService, is ChampionBleError.MissingRequiredChannel -> "required_capability_missing"
    is ChampionBleError.MissingOptionalChannel -> "optional_capability_missing"
    is ChampionBleError.IncompatibleChannelProperties -> "incompatible_capability"
    is ChampionBleError.OperationRejected -> "operation_rejected"
    is ChampionBleError.CallbackFailure -> "callback_failed"
    is ChampionBleError.Timeout -> "timeout"
    is ChampionBleError.QueueClosed -> "queue_closed"
    is ChampionBleError.OperationCancelled -> "cancelled"
    is ChampionBleError.SubscriptionFailed -> "subscription_failed"
    is ChampionBleError.InvalidPayload -> "invalid_data"
    is ChampionBleError.StaleSession -> "stale_session"
    ChampionBleError.CleanupInProgress -> "cleanup_in_progress"
    ChampionBleError.ScanAlreadyInProgress -> "scan_in_progress"
    ChampionBleError.OperationUnavailable -> "operation_unavailable"
    ChampionBleError.UnexpectedInternal -> "internal_failure"
}
