package br.com.mwconsulting.championcompanion.core.bluetooth

import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportResult
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportSummary
import br.com.mwconsulting.championcompanion.core.model.D1HeartRatePacketProgress
import br.com.mwconsulting.championcompanion.core.model.DataValidity
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.HeartRateDeduplicationKey
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementId
import br.com.mwconsulting.championcompanion.core.model.HeartRateSource
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.Duration

object ChampionD1HistoryProtocol {
    const val commandCode: Int = 0xD1
    private const val minimumPacketSize: Int = 20
    private const val currentPositionOffset: Int = 1
    private const val totalPositionsOffset: Int = 3
    private const val packedDayOffsetHourOffset: Int = 5
    private const val caloriesOffset: Int = 6
    private const val distanceOffset: Int = 8
    private const val stepsOffset: Int = 10
    private const val sportValueOffset: Int = 12
    private const val bpmOffset: Int = 14
    private const val pressureHighOffset: Int = 15
    private const val pressureLowOffset: Int = 16
    private const val wearOffset: Int = 17
    private const val tempOneOffset: Int = 18
    private const val minuteOffset: Int = 19
    private val validBpmRange: IntRange = 30..200

    fun createHistoryCommand(timeout: Duration = ChampionBleTimeouts().d1History): ChampionCommand = ChampionCommand(
        commandCode = commandCode,
        channel = ChampionChannelAlias.F008_WRITE,
        writeType = ChampionWriteType.WITH_RESPONSE,
        timeout = timeout,
        retryPolicy = ChampionRetryPolicy.None,
        responseMatcher = null,
        payload = byteArrayOf(commandCode.toByte(), 0x01, 0x00, 0x00),
    )

    fun parsePacket(packet: ChampionPacket): D1Packet? {
        if (packet.channel != ChampionChannelAlias.F008_NOTIFY) return null
        if (packet.commandCode != commandCode) return null
        return parsePayload(packet.bytesCopy())
    }

    fun parsePayload(bytes: ByteArray): D1Packet? {
        if (bytes.size < minimumPacketSize) return null
        if (bytes[0].toUnsignedInt() != commandCode) return null

        val currentPosition = bytes.readUInt16LittleEndian(currentPositionOffset)
        val totalPositions = bytes.readUInt16LittleEndian(totalPositionsOffset)
        val packedDayOffsetHour = bytes[packedDayOffsetHourOffset].toUnsignedInt()
        val dayOffset = packedDayOffsetHour / 32
        val hour = packedDayOffsetHour % 32
        val caloriesRaw = bytes.readUInt16BigEndian(caloriesOffset)
        val distanceRaw = bytes.readUInt16BigEndian(distanceOffset)
        val steps = bytes.readUInt16BigEndian(stepsOffset)
        val sportValue = bytes.readUInt16BigEndian(sportValueOffset)
        val bpmRaw = bytes[bpmOffset].toUnsignedInt()
        val minute = bytes[minuteOffset].toUnsignedInt()

        if (hour !in 0..23) return null
        if (minute !in 0..59) return null

        return D1Packet(
            currentPosition = currentPosition,
            totalPositions = totalPositions,
            dayOffset = dayOffset,
            hour = hour,
            caloriesRaw = caloriesRaw,
            distanceRaw = distanceRaw,
            steps = steps,
            sportValue = sportValue,
            bpmRaw = bpmRaw,
            pressureHighRaw = bytes[pressureHighOffset].toUnsignedInt(),
            pressureLowRaw = bytes[pressureLowOffset].toUnsignedInt(),
            wearRaw = bytes[wearOffset].toUnsignedInt(),
            tempOneRaw = bytes[tempOneOffset].toUnsignedInt(),
            minute = minute,
        )
    }

    fun resolveMeasuredAt(packet: D1Packet, context: D1ImportContext): Instant =
        context.baseLocalDate
            .minusDays(packet.dayOffset.toLong())
            .atTime(LocalTime.of(packet.hour, packet.minute))
            .atZone(context.zoneId)
            .toInstant()

    fun toMeasurement(
        packet: D1Packet,
        context: D1ImportContext,
        measurementId: HeartRateMeasurementId,
    ): HeartRateMeasurement? {
        val bpm = packet.bpm ?: return null
        val measuredAt = resolveMeasuredAt(packet, context)
        val created = HeartRateMeasurement.create(
            id = measurementId,
            source = HeartRateSource.WATCH_D1_HISTORY,
            bpm = bpm,
            measuredAt = measuredAt,
            validity = DataValidity.VALID,
            deviceId = context.deviceId,
            importedAt = measuredAt,
            parserVersion = context.parserVersion,
            deduplicationKey = HeartRateDeduplicationKey.D1History(
                deviceId = context.deviceId,
                resolvedMeasuredAt = measuredAt,
                recordKey = null,
            ),
        )
        return (created as? br.com.mwconsulting.championcompanion.core.model.DomainResult.Success)?.value
    }

    data class D1ImportContext(
        val importAttemptId: String,
        val bleSessionId: ChampionSessionId,
        val deviceId: DeviceId,
        val startedAt: Instant,
        val baseLocalDate: LocalDate,
        val zoneId: ZoneId,
        val parserVersion: ParserVersion,
    ) {
        fun accepts(packet: ChampionPacket): Boolean =
            packet.channel == ChampionChannelAlias.F008_NOTIFY &&
                packet.sessionId == bleSessionId &&
                !packet.observedAt.isBefore(startedAt)
    }

    data class D1Packet(
        val currentPosition: Int,
        val totalPositions: Int,
        val dayOffset: Int,
        val hour: Int,
        val caloriesRaw: Int,
        val distanceRaw: Int,
        val steps: Int,
        val sportValue: Int,
        val bpmRaw: Int,
        val pressureHighRaw: Int,
        val pressureLowRaw: Int,
        val wearRaw: Int,
        val tempOneRaw: Int,
        val minute: Int,
    ) {
        val caloriesKilocalories: Double
            get() = caloriesRaw / 10.0

        val distanceMeters: Int
            get() = distanceRaw

        val bpm: Int?
            get() = bpmRaw.takeIf(validBpmRange::contains)

        /** Compatibilidade de leitura com consumidores do contrato d1-v1. */
        val packetIndex: Int
            get() = currentPosition

        /** Compatibilidade de leitura com consumidores do contrato d1-v1. */
        val totalPackets: Int
            get() = totalPositions
    }

    data class D1CollectedMeasurement(
        val packet: D1Packet,
        val measurement: HeartRateMeasurement,
    )

    data class D1CollectionOutcome(
        val result: D1HeartRateImportResult,
        val measurements: List<HeartRateMeasurement>,
        val progress: D1HeartRatePacketProgress,
        val invalidPacketCount: Int,
        val missingPacketIndices: Set<Int>,
        val packets: List<D1Packet> = emptyList(),
        val missingBpmCount: Int = 0,
        val duplicatePacketCount: Int = 0,
        val outOfOrderPacketCount: Int = 0,
    ) {
        /** Fatos estruturais completos, inclusive quando BPM está ausente. */
        val facts: List<D1Packet>
            get() = packets

        val structurallyInvalidPacketCount: Int
            get() = invalidPacketCount
    }

    sealed interface D1CollectionUpdate {
        data object Ignored : D1CollectionUpdate

        data class Progress(
            val progress: D1HeartRatePacketProgress,
        ) : D1CollectionUpdate

        data class Finished(
            val outcome: D1CollectionOutcome,
        ) : D1CollectionUpdate
    }

    class MultipartCollector(
        private val context: D1ImportContext,
        private val nextMeasurementId: () -> HeartRateMeasurementId,
    ) {
        private val entries = linkedMapOf<Int, CollectedEntry>()
        private var active = true
        private var receivedPacketCount = 0
        private var structurallyInvalidPacketCount = 0
        private var missingBpmCount = 0
        private var duplicatePacketCount = 0
        private var outOfOrderPacketCount = 0
        private var highestPacketIndex = 0
        private var highestExpectedPacketCount = 0
        private var terminalOutcome: D1CollectionOutcome? = null

        @Synchronized
        fun onPacket(packet: ChampionPacket): D1CollectionUpdate {
            if (!active) return D1CollectionUpdate.Ignored
            if (!context.accepts(packet)) return D1CollectionUpdate.Ignored

            receivedPacketCount += 1
            val parsed = parsePacket(packet)
            if (parsed == null) {
                structurallyInvalidPacketCount += 1
                return D1CollectionUpdate.Progress(progressSnapshot())
            }

            missingBpmCount += if (parsed.bpm == null) 1 else 0
            val isDuplicate = entries.containsKey(parsed.currentPosition)
            if (isDuplicate) {
                duplicatePacketCount += 1
            } else if (parsed.currentPosition < highestPacketIndex) {
                outOfOrderPacketCount += 1
            }

            val measurement = parsed.bpm?.let {
                toMeasurement(parsed, context, nextMeasurementId())
            }
            entries[parsed.currentPosition] = CollectedEntry(parsed, measurement)
            highestPacketIndex = maxOf(highestPacketIndex, parsed.currentPosition)
            highestExpectedPacketCount = maxOf(highestExpectedPacketCount, parsed.totalPositions)

            return if (parsed.totalPositions > 0 && parsed.currentPosition >= parsed.totalPositions) {
                D1CollectionUpdate.Finished(finish(packet.observedAt, timedOut = false))
            } else {
                D1CollectionUpdate.Progress(progressSnapshot())
            }
        }

        @Synchronized
        fun onTimeout(finishedAt: Instant): D1CollectionOutcome = finish(finishedAt, timedOut = true)

        @Synchronized
        fun cancel(finishedAt: Instant): D1CollectionOutcome {
            terminalOutcome?.let { return it }
            active = false
            return D1CollectionOutcome(
                result = D1HeartRateImportResult.Cancelled(summary(finishedAt = finishedAt, timedOut = false)),
                measurements = orderedMeasurements(),
                progress = progressSnapshot(),
                invalidPacketCount = structurallyInvalidPacketCount,
                missingPacketIndices = missingPacketIndices(),
                packets = orderedPackets(),
                missingBpmCount = missingBpmCount,
                duplicatePacketCount = duplicatePacketCount,
                outOfOrderPacketCount = outOfOrderPacketCount,
            ).also { terminalOutcome = it }
        }

        private fun finish(finishedAt: Instant, timedOut: Boolean): D1CollectionOutcome {
            terminalOutcome?.let { return it }
            active = false
            val packets = orderedPackets()
            val measurements = orderedMeasurements()
            val missingIndices = missingPacketIndices()
            val summary = summary(
                finishedAt = finishedAt,
                timedOut = timedOut,
                validPointCount = packets.size,
                missingIndexCount = missingIndices.size,
            )
            val result = when {
                timedOut && packets.isEmpty() -> D1HeartRateImportResult.Failed(summary)
                timedOut -> D1HeartRateImportResult.Partial(summary)
                missingIndices.isNotEmpty() -> D1HeartRateImportResult.CompletedWithGaps(summary)
                else -> D1HeartRateImportResult.Completed(summary)
            }
            return D1CollectionOutcome(
                result = result,
                measurements = measurements,
                progress = progressSnapshot(),
                invalidPacketCount = structurallyInvalidPacketCount,
                missingPacketIndices = missingIndices,
                packets = packets,
                missingBpmCount = missingBpmCount,
                duplicatePacketCount = duplicatePacketCount,
                outOfOrderPacketCount = outOfOrderPacketCount,
            ).also { terminalOutcome = it }
        }

        private fun orderedEntries(): List<CollectedEntry> = entries.values
            .sortedWith(compareBy<CollectedEntry>({ resolveMeasuredAt(it.packet, context) }, { it.packet.currentPosition }))

        private fun orderedPackets(): List<D1Packet> = orderedEntries().map { it.packet }

        private fun orderedMeasurements(): List<HeartRateMeasurement> = orderedEntries()
            .mapNotNull { it.measurement }

        private fun missingPacketIndices(): Set<Int> {
            val expectedPacketCount = highestExpectedPacketCount
            if (expectedPacketCount <= 0) return emptySet()
            return (1..expectedPacketCount)
                .filterNot(entries.keys::contains)
                .toSet()
        }

        private fun progressSnapshot(): D1HeartRatePacketProgress = D1HeartRatePacketProgress(
            receivedPacketCount = receivedPacketCount,
            highestPacketIndex = highestPacketIndex,
            totalPackets = highestExpectedPacketCount,
        )

        private fun summary(
            finishedAt: Instant,
            timedOut: Boolean,
            validPointCount: Int = entries.size,
            missingIndexCount: Int = missingPacketIndices().size,
        ): D1HeartRateImportSummary = D1HeartRateImportSummary(
            receivedPacketCount = receivedPacketCount,
            validPointCount = validPointCount,
            insertedCount = 0,
            duplicateCount = 0,
            conflictCount = 0,
            invalidCount = structurallyInvalidPacketCount,
            missingIndexCount = missingIndexCount,
            pendingCreatedCount = 0,
            pendingExistingCount = 0,
            startedAt = context.startedAt,
            finishedAt = finishedAt,
            timedOut = timedOut,
        )

        private data class CollectedEntry(
            val packet: D1Packet,
            val measurement: HeartRateMeasurement?,
        )
    }

    private fun ByteArray.readUInt16LittleEndian(offset: Int): Int =
        this[offset].toUnsignedInt() or (this[offset + 1].toUnsignedInt() shl 8)

    private fun ByteArray.readUInt16BigEndian(offset: Int): Int =
        (this[offset].toUnsignedInt() shl 8) or this[offset + 1].toUnsignedInt()

    private fun Byte.toUnsignedInt(): Int = toInt() and 0xFF
}
