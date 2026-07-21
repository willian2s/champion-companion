package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionOperationType
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePocCycleReportTest {
    @Test
    fun `report contains required sanitized cycle evidence`() {
        val state = DeviceUiState(
            cycleId = DevicePocCycleId(UUID.fromString("00000000-0000-0000-0000-000000000005")),
            cycleStartedAt = Instant.parse("2026-07-21T12:00:00Z"),
            pocState = DevicePocState.CycleCompleted,
            pocStateHistory = listOf("idle", "transport_ready", "handshake_confirmed", "cycle_completed"),
            stepsPacketCount = 3,
            d0PacketCount = 2,
            f002PacketCount = 8,
            f002BufferedSampleCount = 120,
            finalHeartRateBpm = 88,
            finalHeartRateSource = DevicePocHeartRateFinalSource.D0_CURRENT,
        )

        val report = DevicePocCycleReport.fromState(state, Instant.parse("2026-07-21T12:00:09Z"))
        val json = report.asJson()

        assertEquals(9, report.durationSeconds)
        assertTrue(json.contains("\"transport_ready\""))
        assertTrue(json.contains("\"steps_packet_count\": 3"))
        assertTrue(json.contains("\"heart_rate_result\": \"available\""))
        assertFalse(json.contains("\"finalHeartRateBpm\""))
        assertFalse(json.contains("88"))
    }

    @Test
    fun `report preserves repeated state transitions in sequence`() {
        val report = DevicePocCycleReport.fromState(
            DeviceUiState(
                pocState = DevicePocState.Failed(
                    ChampionBleError.Timeout(
                        operation = ChampionOperationType.CONNECT,
                        after = 1.seconds,
                    ),
                ),
                pocStateHistory = listOf("idle", "scanning", "device_found", "scanning", "failed"),
            ),
        )

        assertEquals(
            listOf("idle", "scanning", "device_found", "scanning", "failed"),
            report.stateSequence,
        )
    }

    @Test
    fun `report never emits forbidden technical or health fields`() {
        val json = DevicePocCycleReport.fromState(DeviceUiState()).asJson().lowercase()
        listOf("mac", "payload", "bytes", "hex", "stacktrace", "latitude", "longitude", "bpm").forEach {
            assertFalse("forbidden term: $it", json.contains(it))
        }
    }

    @Test
    fun `fixtures are synthetic and cover poc protocol stages`() {
        val fixtures = listOf(
            DevicePocSanitizedFixtures.a1,
            DevicePocSanitizedFixtures.fea1,
            DevicePocSanitizedFixtures.d0,
            DevicePocSanitizedFixtures.f002,
        )
        assertEquals(listOf("A1", "FEA1", "D0", "F002"), fixtures.map { it.protocol })
        assertTrue(fixtures.all { it.observed && it.count > 0 })
    }
}
