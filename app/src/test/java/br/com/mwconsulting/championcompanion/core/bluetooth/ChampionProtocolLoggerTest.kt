package br.com.mwconsulting.championcompanion.core.bluetooth

import br.com.mwconsulting.championcompanion.core.logging.LogEvent
import br.com.mwconsulting.championcompanion.core.logging.StructuredLogger
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChampionBleProtocolLoggerTest {
    @Test
    fun `logger emits only safe metadata and diagnostic sink copies bytes`() {
        val entries = mutableListOf<Pair<LogEvent, Map<String, String?>>>()
        val captures = mutableListOf<ChampionDiagnosticPacketCapture>()
        val logger = ChampionProtocolLogger(
            logger = object : StructuredLogger {
                override fun debug(event: LogEvent, error: br.com.mwconsulting.championcompanion.core.error.InfrastructureError) = Unit

                override fun debug(event: LogEvent, metadata: Map<String, String?>) {
                    entries += event to metadata
                }
            },
            diagnosticPacketSink = ChampionDiagnosticPacketSink(captures::add),
        )
        val bytes = byteArrayOf(0x01, 0x02, 0x03)
        val packet = testPacket(bytes = bytes)
        val operation = testOperation(payload = bytes)

        logger.logPacketObserved(packet, consumedByMatcher = true)
        logger.logTransportState(packet.sessionId, "transport_ready", ChampionBleError.Timeout(operation.type, operation.timeout))
        logger.logOperationResult(packet.sessionId, operation, result = "success")
        bytes[0] = 0x7F

        val packetLog = entries[0]
        assertEquals(LogEvent.BlePacketObserved, packetLog.first)
        assertEquals(setOf("session_id", "channel", "direction", "command_code", "byte_count", "consumed"), packetLog.second.keys)
        assertFalse(packetLog.second.values.filterNotNull().any { value ->
            value.contains("010203") || value.contains("mac", ignoreCase = true) || value.contains("bpm", ignoreCase = true)
        })

        val operationLog = entries[2]
        assertEquals(LogEvent.BleOperationResult, operationLog.first)
        assertEquals(operation.payloadSize?.toString(), operationLog.second["byte_count"])
        assertEquals("success", operationLog.second["result"])

        assertEquals(1, captures.size)
        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03), captures.single().bytesCopy())
    }
}
