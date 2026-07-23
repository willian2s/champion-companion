package br.com.mwconsulting.championcompanion.core.bluetooth

import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionD4WorkoutBlockProtocolTest {
    @Test
    fun `creates exact D4 block command`() {
        val command = ChampionD4WorkoutBlockProtocol.createBlockCommand(blockIndex = 7, timeout = 15.seconds)

        assertEquals(0xD4, command.commandCode)
        assertEquals(ChampionChannelAlias.F008_WRITE, command.channel)
        assertEquals(byteArrayOf(0xD4.toByte(), 0x07, 0x00, 0x00).toList(), command.payloadCopy().toList())
    }

    @Test
    fun `parses little endian header`() {
        val parsed = ChampionD4WorkoutBlockProtocol.parsePayload(d4Bytes(packetIndex = 0x0201, totalPackets = 0x0403))!!

        assertEquals(0x0201, parsed.packetIndex)
        assertEquals(0x0403, parsed.totalPackets)
        assertArrayEquals(d4Bytes(packetIndex = 0x0201, totalPackets = 0x0403), parsed.rawBytesCopy())
    }

    @Test
    fun `rejects short payload`() {
        assertNull(ChampionD4WorkoutBlockProtocol.parsePayload(ByteArray(4)))
    }

    @Test
    fun `collector accepts out of order packets and sorts for remounting`() {
        val collector = collector()

        assertTrue(
            collector.onPacket(packet(packetIndex = 2, totalPackets = 3))
                is ChampionD4WorkoutBlockProtocol.D4BlockCollectionUpdate.Progress,
        )
        assertTrue(
            collector.onPacket(packet(packetIndex = 1, totalPackets = 3))
                is ChampionD4WorkoutBlockProtocol.D4BlockCollectionUpdate.Progress,
        )

        val finished = collector.onPacket(packet(packetIndex = 3, totalPackets = 3))
            as ChampionD4WorkoutBlockProtocol.D4BlockCollectionUpdate.Finished

        assertEquals(listOf(1, 2, 3), finished.outcome.packets.map { it.packetIndex })
    }

    @Test
    fun `collector reports duplicate packet indices and preserves packets`() {
        val collector = collector()

        collector.onPacket(packet(packetIndex = 1, totalPackets = 2))
        collector.onPacket(packet(packetIndex = 1, totalPackets = 2))
        val finished = collector.onPacket(packet(packetIndex = 2, totalPackets = 2))
            as ChampionD4WorkoutBlockProtocol.D4BlockCollectionUpdate.Finished

        assertEquals(setOf(1), finished.outcome.duplicatePacketIndices)
        assertEquals(3, finished.outcome.packets.size)
    }

    @Test
    fun `collector reports missing packet indices on final packet`() {
        val collector = collector()

        collector.onPacket(packet(packetIndex = 1, totalPackets = 3))
        val finished = collector.onPacket(packet(packetIndex = 3, totalPackets = 3))
            as ChampionD4WorkoutBlockProtocol.D4BlockCollectionUpdate.Finished

        assertEquals(setOf(2), finished.outcome.missingPacketIndices)
    }

    @Test
    fun `timeout returns partial raw packets without retry`() {
        val collector = collector()
        collector.onPacket(packet(packetIndex = 1, totalPackets = 3))

        val outcome = collector.onTimeout()

        assertEquals(true, outcome.timedOut)
        assertEquals(1, outcome.packets.size)
        assertEquals(setOf(2, 3), outcome.missingPacketIndices)
    }

    private fun collector(): ChampionD4WorkoutBlockProtocol.BlockCollector = ChampionD4WorkoutBlockProtocol.BlockCollector(
        ChampionD4WorkoutBlockProtocol.D4BlockContext(
            importAttemptId = "attempt-d4",
            bleSessionId = testSessionId(1),
            startedAt = Instant.parse("2026-07-23T13:00:00Z"),
            blockIndex = 7,
        ),
    )

    private fun packet(
        sessionId: ChampionSessionId = testSessionId(1),
        packetIndex: Int,
        totalPackets: Int,
        observedAt: Instant = Instant.parse("2026-07-23T13:00:01Z"),
    ): ChampionPacket = ChampionPacket(
        sessionId = sessionId,
        channel = ChampionChannelAlias.F008_NOTIFY,
        direction = ChampionPacketDirection.INBOUND,
        observedAt = observedAt,
        commandCode = 0xD4,
        bytes = d4Bytes(packetIndex = packetIndex, totalPackets = totalPackets),
    )

    private fun d4Bytes(packetIndex: Int = 1, totalPackets: Int = 1): ByteArray = ByteArray(20).apply {
        this[0] = 0xD4.toByte()
        this[1] = (packetIndex and 0xFF).toByte()
        this[2] = ((packetIndex shr 8) and 0xFF).toByte()
        this[3] = (totalPackets and 0xFF).toByte()
        this[4] = ((totalPackets shr 8) and 0xFF).toByte()
    }
}
