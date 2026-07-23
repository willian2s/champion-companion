package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.testPacket
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePocHeartRateSessionTest {
    @Test
    fun `parse live bpm accepts first valid value after session start`() {
        val result = DevicePocHeartRateSession.parseLiveBpm(
            packet = testPacket(
                channel = ChampionChannelAlias.F008_NOTIFY,
                commandCode = 0xD0,
                bytes = byteArrayOf(0xD0.toByte(), 0x00, 0x20, 0x48, 0x60),
            ),
            sessionStartedAt = Instant.parse("2025-01-01T00:00:00Z"),
        )

        assertEquals(
            DevicePocHeartRateSession.D0Result.Valid(
                bpm = 72,
                observedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
            result,
        )
    }

    @Test
    fun `parse live bpm ignores stale packet`() {
        val packet = testPacket(
            channel = ChampionChannelAlias.F008_NOTIFY,
            commandCode = 0xD0,
            bytes = byteArrayOf(0xD0.toByte(), 0x4A),
        )

        val result = DevicePocHeartRateSession.parseLiveBpm(
            packet = packet,
            sessionStartedAt = Instant.parse("2026-06-01T00:00:00Z"),
        )

        assertEquals(DevicePocHeartRateSession.D0Result.Ignored, result)
    }

    @Test
    fun `parse live bpm ignores packet without valid bpm`() {
        val result = DevicePocHeartRateSession.parseLiveBpm(
            packet = testPacket(
                channel = ChampionChannelAlias.F008_NOTIFY,
                commandCode = 0xD0,
                bytes = byteArrayOf(0xD0.toByte(), 0x00, 0x10, 0x21),
            ),
            sessionStartedAt = Instant.parse("2025-01-01T00:00:00Z"),
        )

        assertEquals(DevicePocHeartRateSession.D0Result.Ignored, result)
    }

    @Test
    fun `parse f002 decodes little endian pairs and ignores odd trailing byte`() {
        val result = DevicePocHeartRateSession.parseF002Samples(
            testPacket(
                channel = ChampionChannelAlias.F002_NOTIFY,
                commandCode = 0x34,
                bytes = byteArrayOf(0x34, 0x12, 0x78, 0x56, 0x7F),
            ),
        )

        assertEquals(listOf(0x1234, 0x5678), result)
    }

    @Test
    fun `append limited buffer keeps newest samples only`() {
        val buffer = mutableListOf(10, 20, 30)

        val size = DevicePocHeartRateSession.appendLimitedBuffer(
            buffer = buffer,
            samples = listOf(40, 50, 60),
            maxSamples = 4,
        )

        assertEquals(4, size)
        assertEquals(listOf(30, 40, 50, 60), buffer)
    }

    @Test
    fun `d0 commands use expected payloads`() {
        assertTrue(DevicePocHeartRateSession.createStartCommand().payloadCopy().contentEquals(byteArrayOf(0xD0.toByte(), 0x01)))
        assertTrue(DevicePocHeartRateSession.createStopCommand().payloadCopy().contentEquals(byteArrayOf(0xD0.toByte(), 0x00)))
    }
}
