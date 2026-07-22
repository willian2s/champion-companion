package br.com.mwconsulting.championcompanion.feature.device

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class DeviceDiagnosticsSnapshot(
    val capturedAt: Instant,
    val transportState: String,
    val protocolState: DeviceConnectionProtocolState,
    val bluetoothEnabled: Boolean,
    val missingPermissions: List<String>,
    val lastErrorCategory: String?,
) {
    fun asJson(): String = buildString {
        append("{\n")
        field("captured_at", formatter.format(capturedAt))
        field("transport_state", transportState)
        field("protocol_state", protocolState.name.lowercase())
        field("bluetooth_enabled", bluetoothEnabled.toString(), quoteValue = false)
        append("  \"missing_permissions\": [")
        missingPermissions.joinTo(this, separator = ", ") { quote(it) }
        append("],\n")
        field("last_error_category", lastErrorCategory?.let(::quote) ?: "null", quoteValue = false, last = true)
        append("}\n")
    }

    private fun StringBuilder.field(name: String, value: String, quoteValue: Boolean = true, last: Boolean = false) {
        append("  \"").append(name).append("\": ")
        append(if (quoteValue) quote(value) else value)
        append(if (last) "\n" else ",\n")
    }

    private companion object {
        val formatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

        fun quote(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }
}
