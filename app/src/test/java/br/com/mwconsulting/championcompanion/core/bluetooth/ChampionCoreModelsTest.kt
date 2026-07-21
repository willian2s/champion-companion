package br.com.mwconsulting.championcompanion.core.bluetooth

import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionBleCoreModelsTest {
    @Test
    fun `uuid catalog exposes required optional and default subscription channels`() {
        assertEquals(
            setOf(ChampionChannelAlias.F008_NOTIFY, ChampionChannelAlias.F008_WRITE),
            ChampionUuidCatalog.requiredCapabilities(),
        )
        assertTrue(ChampionUuidCatalog.optionalChannels.contains(ChampionChannelAlias.FEA1_NOTIFY))
        assertEquals(
            setOf(
                ChampionChannelAlias.F008_NOTIFY,
                ChampionChannelAlias.F002_NOTIFY,
                ChampionChannelAlias.FEA1_NOTIFY,
            ),
            ChampionUuidCatalog.defaultSubscribedChannels,
        )
    }

    @Test
    fun `command operation and packet copy payload defensively`() {
        val payload = byteArrayOf(0x01, 0x02, 0x03)
        val command = ChampionCommand(
            commandCode = 0xA1,
            channel = ChampionChannelAlias.F008_WRITE,
            writeType = ChampionWriteType.WITH_RESPONSE,
            timeout = 1.seconds,
            retryPolicy = ChampionRetryPolicy.None,
            responseMatcher = null,
            payload = payload,
        )
        val operation = testOperation(
            payload = payload,
            writeType = ChampionWriteType.WITH_RESPONSE,
        )
        val packet = testPacket(bytes = payload)

        payload[0] = 0x7F
        val commandCopy = command.payloadCopy()
        val operationCopy = operation.payloadCopy()!!
        val packetCopy = packet.bytesCopy()
        commandCopy[1] = 0x55
        operationCopy[1] = 0x66
        packetCopy[1] = 0x77

        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03), command.payloadCopy())
        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03), operation.payloadCopy())
        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03), packet.bytesCopy())
        assertEquals(ChampionWriteType.WITH_RESPONSE, operation.writeType)
    }

    @Test
    fun `response matchers and snapshot transport readiness stay scoped`() {
        val sessionId = testSessionId()
        val command = ChampionCommand(
            commandCode = 0xA1,
            channel = ChampionChannelAlias.F008_NOTIFY,
            writeType = ChampionWriteType.WITH_RESPONSE,
            timeout = 1.seconds,
            retryPolicy = ChampionRetryPolicy.None,
            responseMatcher = ChampionResponseMatcher {
                it.channel == ChampionChannelAlias.F008_NOTIFY && it.size == 3
            },
            payload = byteArrayOf(0x11),
        )
        val matchingPacket = testPacket(sessionId = sessionId, channel = ChampionChannelAlias.F008_NOTIFY)
        val wrongSession = testPacket(sessionId = testSessionId(2), channel = ChampionChannelAlias.F008_NOTIFY)
        val wrongChannel = testPacket(sessionId = sessionId, channel = ChampionChannelAlias.F002_NOTIFY)

        assertTrue(ChampionResponseMatchers.forCommand(sessionId, command).matches(matchingPacket))
        assertFalse(ChampionResponseMatchers.forCommand(sessionId, command).matches(wrongSession))
        assertFalse(ChampionResponseMatchers.forCommand(sessionId, command).matches(wrongChannel))

        val snapshot = capabilitySnapshot(ChampionChannelAlias.F008_NOTIFY, ChampionChannelAlias.F008_WRITE)
        assertTrue(snapshot.isTransportReady())
        assertFalse(snapshot.hasChannel(ChampionChannelAlias.FEA1_NOTIFY))
    }
}
