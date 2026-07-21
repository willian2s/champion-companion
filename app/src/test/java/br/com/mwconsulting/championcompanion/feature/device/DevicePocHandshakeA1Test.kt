package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacket
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacketDirection
import br.com.mwconsulting.championcompanion.core.bluetooth.testSessionId
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePocHandshakeA1Test {
    @Test
    fun `a1 builder writes 20 bytes with big endian year and zero password`() {
        val payload = DevicePocHandshakeA1.buildPayload(
            ZonedDateTime.of(2026, 7, 21, 14, 5, 9, 0, ZoneOffset.ofHours(3)),
        )

        assertEquals(20, payload.size)
        assertEquals(0xA1, payload[0].toInt().and(0xFF))
        assertEquals(0x00, payload[1].toInt().and(0xFF))
        assertEquals(0x00, payload[2].toInt().and(0xFF))
        assertEquals(0x07, payload[4].toInt().and(0xFF))
        assertEquals(0xEA, payload[5].toInt().and(0xFF))
        assertEquals(7, payload[6].toInt().and(0xFF))
        assertEquals(21, payload[7].toInt().and(0xFF))
        assertEquals(14, payload[8].toInt().and(0xFF))
        assertEquals(5, payload[9].toInt().and(0xFF))
        assertEquals(9, payload[10].toInt().and(0xFF))
        assertEquals(0x01, payload[11].toInt().and(0xFF))
        assertEquals(0x01, payload[12].toInt().and(0xFF))
        assertEquals(12, payload[13].toInt().and(0xFF))
    }

    @Test
    fun `a1 builder encodes negative timezone in 15 minute blocks`() {
        val payload = DevicePocHandshakeA1.buildPayload(
            ZonedDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneOffset.ofHoursMinutes(-3, -30)),
        )

        assertEquals((-14).toByte(), payload[13])
    }

    @Test
    fun `a1 command uses f008 write with response and no retry`() {
        val command = DevicePocHandshakeA1.createCommand(
            dateTime = ZonedDateTime.of(2026, 7, 21, 14, 5, 9, 0, ZoneOffset.UTC),
            timeout = 5.seconds,
        )

        assertEquals(DevicePocHandshakeA1.COMMAND_CODE, command.commandCode)
        assertEquals(ChampionChannelAlias.F008_WRITE, command.channel)
        assertEquals(5.seconds, command.timeout)
    }

    @Test
    fun `a1 matcher accepts valid notify and rejects short packet`() {
        val valid = ChampionPacket(
            sessionId = testSessionId(),
            channel = ChampionChannelAlias.F008_NOTIFY,
            direction = ChampionPacketDirection.INBOUND,
            observedAt = Instant.now(),
            commandCode = 0xA1,
            bytes = ByteArray(20).also {
                it[0] = 0xA1.toByte()
                it[19] = 0x01
            },
        )
        val shortPacket = ChampionPacket(
            sessionId = testSessionId(),
            channel = ChampionChannelAlias.F008_NOTIFY,
            direction = ChampionPacketDirection.INBOUND,
            observedAt = Instant.now(),
            commandCode = 0xA1,
            bytes = byteArrayOf(0xA1.toByte(), 0x01),
        )
        val wrongChannel = ChampionPacket(
            sessionId = testSessionId(),
            channel = ChampionChannelAlias.F008_WRITE,
            direction = ChampionPacketDirection.INBOUND,
            observedAt = Instant.now(),
            commandCode = 0xA1,
            bytes = ByteArray(20).also {
                it[0] = 0xA1.toByte()
                it[19] = 0x01
            },
        )

        assertTrue(DevicePocHandshakeA1.confirmationMatcher().matches(valid))
        assertFalse(DevicePocHandshakeA1.confirmationMatcher().matches(shortPacket))
        assertFalse(DevicePocHandshakeA1.confirmationMatcher().matches(wrongChannel))
    }
}
