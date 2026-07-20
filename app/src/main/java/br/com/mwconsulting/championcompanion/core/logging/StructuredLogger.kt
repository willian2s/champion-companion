package br.com.mwconsulting.championcompanion.core.logging

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import br.com.mwconsulting.championcompanion.core.error.InfrastructureError
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class LogEvent(val value: String) {
    ErrorPresented("error_presented"),
}

/** Debug logger restricted to a fixed, non-sensitive event schema. */
interface StructuredLogger {
    fun debug(event: LogEvent, error: InfrastructureError)
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
