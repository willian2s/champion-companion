package br.com.mwconsulting.championcompanion.feature.device

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceDiagnosticsSnapshotTest {
    @Test
    fun `snapshot json stays sanitized and machine readable`() {
        val json = DeviceDiagnosticsSnapshot(
            capturedAt = Instant.parse("2026-07-21T12:00:00Z"),
            transportState = "transport_ready",
            protocolState = DeviceConnectionProtocolState.HANDSHAKE_ASSUMED,
            bluetoothEnabled = true,
            missingPermissions = listOf("BLUETOOTH_CONNECT"),
            lastErrorCategory = "timeout",
        ).asJson()

        assertTrue(json.contains("\"protocol_state\": \"handshake_assumed\""))
        assertTrue(json.contains("\"missing_permissions\": [\"BLUETOOTH_CONNECT\"]"))
        assertFalse(json.contains("payload"))
        assertFalse(json.contains("bpm"))
        assertFalse(json.contains("mac"))
    }
}
