package br.com.mwconsulting.championcompanion.core.bluetooth

import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportResult
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportStatus
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementId
import br.com.mwconsulting.championcompanion.core.model.DomainResult
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionD1HistoryProtocolTest {
    @Test
    fun `creates exact D1 today command`() {
        val command = ChampionD1HistoryProtocol.createHistoryCommand(15.seconds)

        assertEquals(0xD1, command.commandCode)
        assertEquals(ChampionChannelAlias.F008_WRITE, command.channel)
        assertEquals(ChampionWriteType.WITH_RESPONSE, command.writeType)
        assertEquals(byteArrayOf(0xD1.toByte(), 0x01, 0x00, 0x00).toList(), command.payloadCopy().toList())
    }

    @Test
    fun `parses little endian packet indices`() {
        val packet = ChampionD1HistoryProtocol.parsePayload(d1Bytes(packetIndex = 0x0201, totalPackets = 0x0403))!!

        assertEquals(0x0201, packet.packetIndex)
        assertEquals(0x0403, packet.totalPackets)
    }

    @Test
    fun `rejects invalid payload shapes and ranges`() {
        assertEquals(null, ChampionD1HistoryProtocol.parsePayload(ByteArray(19)))
        assertEquals(null, ChampionD1HistoryProtocol.parsePayload(d1Bytes(commandCode = 0xD0)))
        assertEquals(null, ChampionD1HistoryProtocol.parsePayload(d1Bytes(hour = 24)))
        assertEquals(null, ChampionD1HistoryProtocol.parsePayload(d1Bytes(minute = 60)))
        assertEquals(null, ChampionD1HistoryProtocol.parsePayload(d1Bytes(bpm = 29)))
        assertEquals(null, ChampionD1HistoryProtocol.parsePayload(d1Bytes(bpm = 221)))
    }

    @Test
    fun `resolves timestamp from frozen local date and zone`() {
        val context = context(baseLocalDate = LocalDate.of(2026, 7, 23), zoneId = ZoneId.of("America/Sao_Paulo"))
        val packet = ChampionD1HistoryProtocol.parsePayload(d1Bytes(hour = 6, minute = 45))!!

        assertEquals(
            Instant.parse("2026-07-23T09:45:00Z"),
            ChampionD1HistoryProtocol.resolveMeasuredAt(packet, context),
        )
    }

    @Test
    fun `collector accepts out of order packets and sorts by measurement time`() {
        val collector = collector()

        assertTrue(
            collector.onPacket(packet(packetIndex = 2, totalPackets = 3, hour = 10, minute = 15))
                is ChampionD1HistoryProtocol.D1CollectionUpdate.Progress,
        )
        assertTrue(
            collector.onPacket(packet(packetIndex = 1, totalPackets = 3, hour = 10, minute = 5))
                is ChampionD1HistoryProtocol.D1CollectionUpdate.Progress,
        )

        val finished = collector.onPacket(packet(packetIndex = 3, totalPackets = 3, hour = 10, minute = 25))
            as ChampionD1HistoryProtocol.D1CollectionUpdate.Finished

        assertEquals(D1HeartRateImportStatus.COMPLETED, finished.outcome.result.status)
        assertEquals(
            listOf(
                Instant.parse("2026-07-23T13:05:00Z"),
                Instant.parse("2026-07-23T13:15:00Z"),
                Instant.parse("2026-07-23T13:25:00Z"),
            ),
            finished.outcome.measurements.mapNotNull { it.measuredAt },
        )
    }

    @Test
    fun `collector completes with gaps when final packet arrives before all indices`() {
        val collector = collector()

        collector.onPacket(packet(packetIndex = 1, totalPackets = 3, hour = 10, minute = 5))
        val finished = collector.onPacket(packet(packetIndex = 3, totalPackets = 3, hour = 10, minute = 25))
            as ChampionD1HistoryProtocol.D1CollectionUpdate.Finished

        assertEquals(D1HeartRateImportStatus.COMPLETED_WITH_GAPS, finished.outcome.result.status)
        assertEquals(setOf(2), finished.outcome.missingPacketIndices)
    }

    @Test
    fun `collector returns partial on timeout with valid points`() {
        val collector = collector()
        collector.onPacket(packet(packetIndex = 1, totalPackets = 3, hour = 10, minute = 5))

        val result = collector.onTimeout(Instant.parse("2026-07-23T13:00:15Z"))

        assertEquals(D1HeartRateImportStatus.PARTIAL, result.result.status)
        assertEquals(1, result.measurements.size)
        assertEquals(true, result.result.summary?.timedOut)
    }

    @Test
    fun `collector returns failed on empty timeout`() {
        val result = collector().onTimeout(Instant.parse("2026-07-23T13:00:15Z"))

        assertTrue(result.result is D1HeartRateImportResult.Failed)
        assertEquals(0, result.measurements.size)
    }

    @Test
    fun `collector ignores late callbacks from old session or after finish`() {
        val collector = collector()
        val stale = packet(sessionId = testSessionId(2), packetIndex = 1, totalPackets = 1)

        assertEquals(ChampionD1HistoryProtocol.D1CollectionUpdate.Ignored, collector.onPacket(stale))

        val finished = collector.onPacket(packet(packetIndex = 1, totalPackets = 1, hour = 10, minute = 5))
        assertTrue(finished is ChampionD1HistoryProtocol.D1CollectionUpdate.Finished)
        assertEquals(
            ChampionD1HistoryProtocol.D1CollectionUpdate.Ignored,
            collector.onPacket(packet(packetIndex = 1, totalPackets = 1, hour = 10, minute = 6)),
        )
    }

    private fun collector(): ChampionD1HistoryProtocol.MultipartCollector {
        var nextId = 10
        return ChampionD1HistoryProtocol.MultipartCollector(context()) {
            HeartRateMeasurementId(uuid(nextId++))
        }
    }

    private fun context(
        baseLocalDate: LocalDate = LocalDate.of(2026, 7, 23),
        zoneId: ZoneId = ZoneId.of("America/Sao_Paulo"),
    ): ChampionD1HistoryProtocol.D1ImportContext = ChampionD1HistoryProtocol.D1ImportContext(
        importAttemptId = "attempt-1",
        bleSessionId = testSessionId(1),
        deviceId = DeviceId(uuid(1)),
        startedAt = Instant.parse("2026-07-23T12:59:59Z"),
        baseLocalDate = baseLocalDate,
        zoneId = zoneId,
        parserVersion = (ParserVersion.create("d1-v1") as DomainResult.Success).value,
    )

    private fun packet(
        sessionId: ChampionSessionId = testSessionId(1),
        packetIndex: Int,
        totalPackets: Int,
        hour: Int = 10,
        minute: Int = 5,
        bpm: Int = 72,
        observedAt: Instant = Instant.parse("2026-07-23T13:00:00Z"),
    ): ChampionPacket = ChampionPacket(
        sessionId = sessionId,
        channel = ChampionChannelAlias.F008_NOTIFY,
        direction = ChampionPacketDirection.INBOUND,
        observedAt = observedAt,
        commandCode = 0xD1,
        bytes = d1Bytes(packetIndex = packetIndex, totalPackets = totalPackets, hour = hour, minute = minute, bpm = bpm),
    )

    private fun d1Bytes(
        commandCode: Int = 0xD1,
        packetIndex: Int = 1,
        totalPackets: Int = 1,
        hour: Int = 10,
        minute: Int = 5,
        bpm: Int = 72,
    ): ByteArray = ByteArray(20).apply {
        this[0] = commandCode.toByte()
        this[1] = (packetIndex and 0xFF).toByte()
        this[2] = ((packetIndex shr 8) and 0xFF).toByte()
        this[3] = (totalPackets and 0xFF).toByte()
        this[4] = ((totalPackets shr 8) and 0xFF).toByte()
        this[5] = hour.toByte()
        this[14] = bpm.toByte()
        this[19] = minute.toByte()
    }

    private fun uuid(last: Int) = UUID.fromString("0190029f-2f80-7000-8000-0000000000${last.toString().padStart(2, '0')}")
}
