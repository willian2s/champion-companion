package br.com.mwconsulting.championcompanion.core.bluetooth

import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionD3WorkoutIndexProtocolTest {
    @Test
    fun `creates exact D3 command`() {
        val command = ChampionD3WorkoutIndexProtocol.createIndexCommand(10.seconds)

        assertEquals(0xD3, command.commandCode)
        assertEquals(ChampionChannelAlias.F008_WRITE, command.channel)
        assertEquals(ChampionWriteType.WITH_RESPONSE, command.writeType)
        assertEquals(byteArrayOf(0xD3.toByte(), 0x01, 0x00, 0x00).toList(), command.payloadCopy().toList())
    }

    @Test
    fun `parses little endian crc candidates`() {
        val parsed = ChampionD3WorkoutIndexProtocol.parsePayload(d3Bytes(crcPairs = listOf(0x3412, 0x7856)))!!

        assertEquals(listOf(0x3412, 0x7856), parsed.crcCandidates.map { it.crc })
        assertArrayEquals(byteArrayOf(0x12, 0x34), parsed.crcCandidates.first().rawLittleEndianCopy())
    }

    @Test
    fun `collector ignores zero crc and deduplicates repeated values with sequential block index`() {
        val collector = collector()

        val finished = collector.onPacket(packet(crcPairs = listOf(0x0000, 0x1234, 0x1234, 0x4567)))
            as ChampionD3WorkoutIndexProtocol.D3IndexCollectionUpdate.Finished

        assertEquals(listOf(0x1234, 0x4567), finished.outcome.entries.map { it.crc })
        assertEquals(listOf(0, 1), finished.outcome.entries.map { it.blockIndex })
    }

    @Test
    fun `single response without plausible header finishes immediately`() {
        val finished = collector().onPacket(packet(crcPairs = listOf(0x1111, 0x2222)))
            as ChampionD3WorkoutIndexProtocol.D3IndexCollectionUpdate.Finished

        assertEquals(false, finished.outcome.timedOut)
        assertEquals(listOf(0x1111, 0x2222), finished.outcome.entries.map { it.crc })
    }

    @Test
    fun `multipart waits for plausible final packet`() {
        val collector = collector()

        assertTrue(
            collector.onPacket(packet(packetIndex = 1, totalPackets = 2, crcPairs = listOf(0x0002, 0x1111)))
                is ChampionD3WorkoutIndexProtocol.D3IndexCollectionUpdate.Progress,
        )

        val finished = collector.onPacket(packet(packetIndex = 2, totalPackets = 2, crcPairs = listOf(0x2222)))
            as ChampionD3WorkoutIndexProtocol.D3IndexCollectionUpdate.Finished

        assertEquals(2, finished.outcome.receivedPacketCount)
        assertEquals(listOf(0x0002, 0x1111, 0x2222), finished.outcome.entries.map { it.crc })
    }

    @Test
    fun `timeout preserves collected crcs`() {
        val collector = collector()
        collector.onPacket(packet(crcPairs = listOf(0x1234)))

        val outcome = collector.onTimeout()

        assertEquals(true, outcome.timedOut)
        assertEquals(listOf(0x1234), outcome.entries.map { it.crc })
    }

    private fun collector(): ChampionD3WorkoutIndexProtocol.IndexCollector = ChampionD3WorkoutIndexProtocol.IndexCollector(
        ChampionD3WorkoutIndexProtocol.D3IndexContext(
            importAttemptId = "attempt-d3",
            bleSessionId = testSessionId(1),
            startedAt = Instant.parse("2026-07-23T13:00:00Z"),
        ),
    )

    private fun packet(
        sessionId: ChampionSessionId = testSessionId(1),
        packetIndex: Int = 0,
        totalPackets: Int = 0,
        crcPairs: List<Int>,
        observedAt: Instant = Instant.parse("2026-07-23T13:00:01Z"),
    ): ChampionPacket = ChampionPacket(
        sessionId = sessionId,
        channel = ChampionChannelAlias.F008_NOTIFY,
        direction = ChampionPacketDirection.INBOUND,
        observedAt = observedAt,
        commandCode = 0xD3,
        bytes = d3Bytes(packetIndex = packetIndex, totalPackets = totalPackets, crcPairs = crcPairs),
    )

    private fun d3Bytes(
        packetIndex: Int = 0,
        totalPackets: Int = 0,
        crcPairs: List<Int>,
    ): ByteArray = buildList<Byte> {
        add(0xD3.toByte())
        add((packetIndex and 0xFF).toByte())
        add(((packetIndex shr 8) and 0xFF).toByte())
        if (crcPairs.isEmpty()) {
            add((totalPackets and 0xFF).toByte())
            add(((totalPackets shr 8) and 0xFF).toByte())
        } else {
            crcPairs.forEachIndexed { index, crc ->
                if (index == 0) {
                    add((crc and 0xFF).toByte())
                    add(((crc shr 8) and 0xFF).toByte())
                } else {
                    add((crc and 0xFF).toByte())
                    add(((crc shr 8) and 0xFF).toByte())
                }
            }
        }
        if (size < 5) {
            while (size < 5) add(0x00)
        }
    }.toByteArray()
}
