package br.com.mwconsulting.championcompanion.core.logging

import br.com.mwconsulting.championcompanion.core.error.InfrastructureError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DebugStructuredLoggerTest {
    @Test
    fun `debug build emits only approved fields`() {
        val messages = mutableListOf<String>()
        val logger = DebugStructuredLogger(isDebugBuild = true, sink = DebugLogSink(messages::add))

        logger.debug(LogEvent.ErrorPresented, InfrastructureError.NetworkUnavailable)

        val entry = Json.parseToJsonElement(messages.single()).jsonObject
        assertEquals(setOf("event", "error_code"), entry.keys)
        assertEquals("error_presented", entry["event"]?.jsonPrimitive?.content)
        assertEquals("network_unavailable", entry["error_code"]?.jsonPrimitive?.content)
        assertFalse(messages.single().contains("token"))
        assertFalse(messages.single().contains("latitude"))
        assertFalse(messages.single().contains("health"))
    }

    @Test
    fun `release build emits no diagnostic log`() {
        val messages = mutableListOf<String>()
        val logger = DebugStructuredLogger(isDebugBuild = false, sink = DebugLogSink(messages::add))

        logger.debug(LogEvent.ErrorPresented, InfrastructureError.Unexpected)

        assertEquals(emptyList<String>(), messages)
    }
}
