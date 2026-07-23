package br.com.mwconsulting.championcompanion.core.bluetooth

import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.DomainResult
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportErrorCategory
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutParserResult
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionD4WorkoutParserTest {
    @Test
    fun `parses preferred layout into domain input`() {
        val result = ChampionD4WorkoutParser.parse(request(blockOutcome(preferredPackets()))) as WatchWorkoutParserResult.Valid

        assertEquals(2, result.input.metrics.steps)
        assertEquals(900.0, result.input.metrics.distances.originalMeters.valueOrNull())
        assertEquals(1.5, result.input.metrics.caloriesKilocalories.valueOrNull())
        assertEquals(WorkoutType.FREE_WORKOUT, WorkoutType.FREE_WORKOUT.takeIf { result.input.metrics.steps < 1000 })
        assertEquals(listOf(0, 1), result.input.intervals.map { it.index })
        assertEquals(listOf(Instant.parse("2026-07-23T10:00:00Z"), Instant.parse("2026-07-23T10:01:00Z")), result.input.heartRateSamples.map { it.sampledAt })
    }

    @Test
    fun `falls back to legacy layout when preferred packet one is invalid`() {
        val result = ChampionD4WorkoutParser.parse(request(blockOutcome(legacyPackets()))) as WatchWorkoutParserResult.Valid

        assertEquals(600L, result.input.metrics.steps)
        assertEquals(1200.0, result.input.metrics.distances.originalMeters.valueOrNull())
        assertEquals(2.5, result.input.metrics.caloriesKilocalories.valueOrNull())
        assertEquals(1200L, result.input.metrics.duration.seconds)
        assertEquals(1, result.input.intervals.size)
    }

    @Test
    fun `ignores invalid bpm samples and computes bpm aggregates from valid ones`() {
        val result = ChampionD4WorkoutParser.parse(
            request(
                blockOutcome(
                    preferredPackets(
                        sampleBpms = listOf(0, 180),
                        recordCount = 2,
                    ),
                ),
            ),
        ) as WatchWorkoutParserResult.Valid

        assertEquals(listOf(180), result.input.heartRateSamples.map { it.bpm })
        assertEquals(180, result.input.metrics.averageBpm.valueOrNull())
        assertEquals(180, result.input.metrics.minimumBpm.valueOrNull())
        assertEquals(180, result.input.metrics.maximumBpm.valueOrNull())
    }

    @Test
    fun `suggests running when steps are one thousand or more`() {
        val result = ChampionD4WorkoutParser.parse(
            request(blockOutcome(preferredPackets(totalSteps = 1200))),
        ) as WatchWorkoutParserResult.Valid

        assertEquals(1200L, result.input.metrics.steps)
    }

    @Test
    fun `returns partial for duplicate packets`() {
        val outcome = blockOutcome(preferredPackets(), duplicatePacketIndices = setOf(4))

        val result = ChampionD4WorkoutParser.parse(request(outcome))

        assertTrue(result is WatchWorkoutParserResult.Partial)
        assertEquals(setOf(4), result.summary?.duplicatePacketIndices)
    }

    @Test
    fun `returns partial for missing packet indices`() {
        val outcome = blockOutcome(preferredPackets(), missingPacketIndices = setOf(5))

        val result = ChampionD4WorkoutParser.parse(request(outcome))

        assertTrue(result is WatchWorkoutParserResult.Partial)
        assertEquals(setOf(5), result.summary?.missingPacketIndices)
    }

    @Test
    fun `returns partial for timeout`() {
        val outcome = blockOutcome(preferredPackets(), timedOut = true)

        val result = ChampionD4WorkoutParser.parse(request(outcome))

        assertTrue(result is WatchWorkoutParserResult.Partial)
        assertEquals(true, result.summary?.timedOut)
    }

    @Test
    fun `returns partial for record count divergence`() {
        val result = ChampionD4WorkoutParser.parse(request(blockOutcome(preferredPackets(recordCount = 3))))

        assertTrue(result is WatchWorkoutParserResult.Partial)
        assertEquals(3, result.summary?.recordCount)
        assertEquals(2, result.summary?.parsedIntervalCount)
    }

    @Test
    fun `returns partial for crc mismatch`() {
        val result = ChampionD4WorkoutParser.parse(request(blockOutcome(preferredPackets(reportedCrc = 0x9999)), crc = 0x1234))

        assertTrue(result is WatchWorkoutParserResult.Partial)
        assertEquals(false, result.summary?.crcMatchesD3)
    }

    @Test
    fun `returns invalid when required packet is missing`() {
        val packets = preferredPackets().filterNot { it.packetIndex == 2 }
        val result = ChampionD4WorkoutParser.parse(request(blockOutcome(packets))) as WatchWorkoutParserResult.Invalid

        assertEquals(WatchWorkoutImportErrorCategory.REQUIRED_PACKET_MISSING, result.errorCategory)
    }

    @Test
    fun `returns invalid when packet one dates are irrecoverable`() {
        val packets = preferredPackets(packet1 = invalidPacket1())
        val result = ChampionD4WorkoutParser.parse(request(blockOutcome(packets))) as WatchWorkoutParserResult.Invalid

        assertEquals(WatchWorkoutImportErrorCategory.INVALID_LAYOUT, result.errorCategory)
    }

    private fun request(
        outcome: ChampionD4WorkoutBlockProtocol.D4BlockCollectionOutcome,
        crc: Int = 0x1234,
    ): ChampionD4WorkoutParser.Request = ChampionD4WorkoutParser.Request(
        deviceId = DeviceId(uuid(1)),
        crc = crc,
        externalBlockIndex = 7,
        parserVersion = (ParserVersion.create("d4-v1") as DomainResult.Success).value,
        block = outcome,
    )

    private fun blockOutcome(
        packets: List<ChampionD4WorkoutBlockProtocol.D4CollectedPacket>,
        duplicatePacketIndices: Set<Int> = emptySet(),
        missingPacketIndices: Set<Int> = emptySet(),
        timedOut: Boolean = false,
    ) = ChampionD4WorkoutBlockProtocol.D4BlockCollectionOutcome(
        blockIndex = 7,
        packets = packets,
        progress = ChampionD4WorkoutBlockProtocol.D4BlockProgress(
            receivedPacketCount = packets.size,
            highestPacketIndex = packets.maxOfOrNull { it.packetIndex } ?: 0,
            totalPackets = packets.maxOfOrNull { it.totalPackets } ?: 0,
        ),
        duplicatePacketIndices = duplicatePacketIndices,
        missingPacketIndices = missingPacketIndices,
        timedOut = timedOut,
        receivedPacketCount = packets.size,
    )

    private fun preferredPackets(
        totalSteps: Int = 2,
        totalDistanceRaw: Int = 90000,
        totalCaloriesRaw: Int = 1500,
        recordCount: Int = 2,
        reportedCrc: Int = 0x1234,
        sampleBpms: List<Int> = listOf(100, 110),
        packet1: ByteArray = preferredPacket1(),
    ): List<ChampionD4WorkoutBlockProtocol.D4CollectedPacket> = listOf(
        packet(1, 5, packet1),
        packet(2, 5, preferredPacket2(totalSteps, totalDistanceRaw, totalCaloriesRaw)),
        packet(3, 5, preferredPacket3(recordCount, reportedCrc)),
        packet(4, 5, samplePacket(sampleBpms[0], stepsDelta = 1, distanceRawDelta = 40000, caloriesRawDelta = 700)),
        packet(5, 5, samplePacket(sampleBpms[1], stepsDelta = 1, distanceRawDelta = 50000, caloriesRawDelta = 800)),
    )

    private fun legacyPackets(): List<ChampionD4WorkoutBlockProtocol.D4CollectedPacket> = listOf(
        packet(1, 4, legacyPacket1()),
        packet(2, 4, legacyPacket2(totalSteps = 600, totalDistanceRaw = 120000, totalCaloriesRaw = 2500)),
        packet(3, 4, legacyPacket3(durationSeconds = 1200, recordCount = 1, reportedCrc = 0x1234)),
        packet(4, 4, samplePacket(120, stepsDelta = 600, distanceRawDelta = 120000, caloriesRawDelta = 2500)),
    )

    private fun packet(index: Int, total: Int, bytes: ByteArray) = ChampionD4WorkoutBlockProtocol.D4CollectedPacket(index, total, bytes)

    private fun preferredPacket1(): ByteArray = basePacket(1, 5).apply {
        writeUInt16LE(7, 2026)
        this[9] = 7
        this[10] = 23
        this[11] = 10
        this[12] = 0
        this[13] = 0
        writeUInt16LE(14, 2026)
        this[16] = 7
        this[17] = 23
        this[18] = 10
        this[19] = 2
    }

    private fun invalidPacket1(): ByteArray = basePacket(1, 5).apply {
        writeUInt16LE(7, 0)
        writeUInt16LE(14, 0)
    }

    private fun legacyPacket1(): ByteArray = basePacket(1, 4).apply {
        this[5] = 10
        this[6] = 0
        this[7] = 0
        this[8] = 7
        this[9] = 23
        writeUInt16LE(10, 2026)
        this[12] = 10
        this[13] = 20
        this[14] = 0
        this[15] = 7
        this[16] = 23
        writeUInt16LE(17, 2026)
    }

    private fun preferredPacket2(totalSteps: Int, totalDistanceRaw: Int, totalCaloriesRaw: Int): ByteArray = basePacket(2, 5).apply {
        this[5] = 0xFF.toByte()
        this[6] = 0xFF.toByte()
        writeUInt16LE(7, totalSteps)
        writeUInt16LE(11, totalDistanceRaw / 100)
        writeUInt24LE(15, totalCaloriesRaw)
    }

    private fun legacyPacket2(totalSteps: Int, totalDistanceRaw: Int, totalCaloriesRaw: Int): ByteArray = basePacket(2, 4).apply {
        writeUInt24LE(5, totalSteps)
        writeUInt32LE(8, totalDistanceRaw)
        writeUInt32LE(12, totalCaloriesRaw)
    }

    private fun preferredPacket3(recordCount: Int, reportedCrc: Int): ByteArray = basePacket(3, 5).apply {
        this[9] = recordCount.toByte()
        writeUInt16LE(14, reportedCrc)
    }

    private fun legacyPacket3(durationSeconds: Int, recordCount: Int, reportedCrc: Int): ByteArray = basePacket(3, 4).apply {
        writeUInt32LE(5, durationSeconds)
        writeUInt16LE(9, recordCount)
        writeUInt16LE(11, reportedCrc)
    }

    private fun samplePacket(bpm: Int, stepsDelta: Int, distanceRawDelta: Int, caloriesRawDelta: Int): ByteArray = basePacket(4, 5).apply {
        this[6] = bpm.toByte()
        writeUInt24LE(7, stepsDelta)
        writeUInt24LE(10, distanceRawDelta)
        writeUInt16LE(13, caloriesRawDelta)
    }

    private fun basePacket(packetIndex: Int, totalPackets: Int): ByteArray = ByteArray(20).apply {
        this[0] = 0xD4.toByte()
        writeUInt16LE(1, packetIndex)
        writeUInt16LE(3, totalPackets)
    }

    private fun ByteArray.writeUInt16LE(offset: Int, value: Int) {
        this[offset] = (value and 0xFF).toByte()
        this[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun ByteArray.writeUInt24LE(offset: Int, value: Int) {
        this[offset] = (value and 0xFF).toByte()
        this[offset + 1] = ((value shr 8) and 0xFF).toByte()
        this[offset + 2] = ((value shr 16) and 0xFF).toByte()
    }

    private fun ByteArray.writeUInt32LE(offset: Int, value: Int) {
        this[offset] = (value and 0xFF).toByte()
        this[offset + 1] = ((value shr 8) and 0xFF).toByte()
        this[offset + 2] = ((value shr 16) and 0xFF).toByte()
        this[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun <T> br.com.mwconsulting.championcompanion.core.model.DomainValue<T>.valueOrNull(): T? =
        (this as? br.com.mwconsulting.championcompanion.core.model.DomainValue.Present)?.value

    private fun uuid(last: Int) = UUID.fromString("0190029f-2f80-7000-8000-0000000000${last.toString().padStart(2, '0')}")
}
