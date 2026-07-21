package br.com.mwconsulting.championcompanion.core.logging

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import br.com.mwconsulting.championcompanion.core.error.InfrastructureError
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class LogEvent(val value: String) {
    ErrorPresented("error_presented"),
    BlePacketObserved("ble_packet_observed"),
    BleTransportState("ble_transport_state"),
    BleOperationResult("ble_operation_result"),
}

/** Debug logger restricted to a fixed, non-sensitive event schema. */
interface StructuredLogger {
    fun debug(event: LogEvent, error: InfrastructureError)

    fun debug(event: LogEvent, metadata: Map<String, String?>)
}

internal fun interface DebugLogSink {
    fun write(message: String)
}

internal object AndroidDebugLogSink : DebugLogSink {
    override fun write(message: String) {
        Log.d("ChampionCompanion", message)
    }
}

internal class DebugStructuredLogger(
    private val isDebugBuild: Boolean,
    private val sink: DebugLogSink,
) : StructuredLogger {
    override fun debug(event: LogEvent, error: InfrastructureError) {
        if (!isDebugBuild) return

        sink.write(
            buildJsonObject {
                put("event", event.value)
                put("error_code", error.logCode)
            }.toString(),
        )
    }

    override fun debug(event: LogEvent, metadata: Map<String, String?>) {
        if (!isDebugBuild) return

        val approvedFields = event.approvedFields() ?: return
        sink.write(
            buildJsonObject {
                put("event", event.value)
                approvedFields.forEach { field ->
                    metadata[field]?.let { value -> put(field, value) }
                }
            }.toString(),
        )
    }
}

private fun LogEvent.approvedFields(): Set<String>? = when (this) {
    LogEvent.ErrorPresented -> null
    LogEvent.BlePacketObserved -> setOf(
        "session_id",
        "channel",
        "direction",
        "command_code",
        "byte_count",
        "consumed",
    )

    LogEvent.BleTransportState -> setOf(
        "session_id",
        "state",
        "error_code",
    )

    LogEvent.BleOperationResult -> setOf(
        "session_id",
        "operation_id",
        "operation_type",
        "channel",
        "byte_count",
        "result",
        "error_code",
    )
}

internal val InfrastructureError.logCode: String
    get() = when (this) {
        InfrastructureError.NetworkUnavailable -> "network_unavailable"
        InfrastructureError.ServiceUnavailable -> "service_unavailable"
        InfrastructureError.Unexpected -> "unexpected"
    }

internal fun debugStructuredLogger(context: Context): StructuredLogger = DebugStructuredLogger(
    isDebugBuild = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
    sink = AndroidDebugLogSink,
)
