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
    private const val packetIndexOffset: Int = 1
    private const val totalPacketsOffset: Int = 3
    private const val hourOffset: Int = 5
    private const val bpmOffset: Int = 14
    private const val minuteOffset: Int = 19
    private val validBpmRange: IntRange = 30..220

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

        val packetIndex = bytes.readUInt16LittleEndian(packetIndexOffset)
        val totalPackets = bytes.readUInt16LittleEndian(totalPacketsOffset)
        val hour = bytes[hourOffset].toUnsignedInt()
        val bpm = bytes[bpmOffset].toUnsignedInt()
        val minute = bytes[minuteOffset].toUnsignedInt()

        if (hour !in 0..23) return null
        if (minute !in 0..59) return null
        if (bpm !in validBpmRange) return null

        return D1Packet(
            packetIndex = packetIndex,
            totalPackets = totalPackets,
            hour = hour,
            minute = minute,
            bpm = bpm,
        )
    }

    fun resolveMeasuredAt(packet: D1Packet, context: D1ImportContext): Instant =
        context.baseLocalDate
            .atTime(LocalTime.of(packet.hour, packet.minute))
            .atZone(context.zoneId)
            .toInstant()

    fun toMeasurement(
        packet: D1Packet,
        context: D1ImportContext,
        measurementId: HeartRateMeasurementId,
    ): HeartRateMeasurement? {
        val measuredAt = resolveMeasuredAt(packet, context)
        val created = HeartRateMeasurement.create(
            id = measurementId,
            source = HeartRateSource.WATCH_D1_HISTORY,
            bpm = packet.bpm,
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
        val packetIndex: Int,
        val totalPackets: Int,
        val hour: Int,
        val minute: Int,
        val bpm: Int,
    )

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
    )

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
        private val entries = linkedMapOf<CollectionKey, D1CollectedMeasurement>()
        private val receivedIndices = linkedSetOf<Int>()
        private var active = true
        private var receivedPacketCount = 0
        private var invalidPacketCount = 0
        private var highestPacketIndex = 0
        private var highestExpectedPacketCount = 0

        fun onPacket(packet: ChampionPacket): D1CollectionUpdate {
            if (!active) return D1CollectionUpdate.Ignored
            if (!context.accepts(packet)) return D1CollectionUpdate.Ignored

            receivedPacketCount += 1
            val parsed = parsePacket(packet)
            if (parsed == null) {
                invalidPacketCount += 1
                return D1CollectionUpdate.Progress(progressSnapshot())
            }

            val measurement = toMeasurement(parsed, context, nextMeasurementId())
                ?: return D1CollectionUpdate.Progress(progressSnapshot())

            val measuredAt = measurement.measuredAt ?: return D1CollectionUpdate.Progress(progressSnapshot())
            val key = CollectionKey(
                packetIndex = parsed.packetIndex,
                totalPackets = parsed.totalPackets,
                measuredAt = measuredAt,
            )

            entries[key] = D1CollectedMeasurement(packet = parsed, measurement = measurement)
            receivedIndices += parsed.packetIndex
            highestPacketIndex = maxOf(highestPacketIndex, parsed.packetIndex)
            highestExpectedPacketCount = maxOf(highestExpectedPacketCount, parsed.totalPackets)

            return if (parsed.totalPackets > 0 && parsed.packetIndex >= parsed.totalPackets) {
                D1CollectionUpdate.Finished(finish(packet.observedAt, timedOut = false))
            } else {
                D1CollectionUpdate.Progress(progressSnapshot())
            }
        }

        fun onTimeout(finishedAt: Instant): D1CollectionOutcome = finish(finishedAt, timedOut = true)

        fun cancel(finishedAt: Instant): D1CollectionOutcome {
            active = false
            return D1CollectionOutcome(
                result = D1HeartRateImportResult.Cancelled(summary(finishedAt = finishedAt, timedOut = false)),
                measurements = orderedMeasurements(),
                progress = progressSnapshot(),
                invalidPacketCount = invalidPacketCount,
                missingPacketIndices = missingPacketIndices(),
            )
        }

        private fun finish(finishedAt: Instant, timedOut: Boolean): D1CollectionOutcome {
            active = false
            val measurements = orderedMeasurements()
            val missingIndices = missingPacketIndices()
            val summary = summary(
                finishedAt = finishedAt,
                timedOut = timedOut,
                validPointCount = measurements.size,
                missingIndexCount = missingIndices.size,
            )
            val result = when {
                timedOut && measurements.isEmpty() -> D1HeartRateImportResult.Failed(summary)
                timedOut -> D1HeartRateImportResult.Partial(summary)
                missingIndices.isNotEmpty() -> D1HeartRateImportResult.CompletedWithGaps(summary)
                else -> D1HeartRateImportResult.Completed(summary)
            }
            return D1CollectionOutcome(
                result = result,
                measurements = measurements,
                progress = progressSnapshot(),
                invalidPacketCount = invalidPacketCount,
                missingPacketIndices = missingIndices,
            )
        }

        private fun orderedMeasurements(): List<HeartRateMeasurement> = entries.values
            .sortedWith(compareBy<D1CollectedMeasurement>({ it.measurement.measuredAt }, { it.packet.packetIndex }))
            .map { it.measurement }

        private fun missingPacketIndices(): Set<Int> {
            val expectedPacketCount = highestExpectedPacketCount
            if (expectedPacketCount <= 0) return emptySet()
            return (1..expectedPacketCount)
                .filterNot(receivedIndices::contains)
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
            invalidCount = invalidPacketCount,
            missingIndexCount = missingIndexCount,
            pendingCreatedCount = 0,
            pendingExistingCount = 0,
            startedAt = context.startedAt,
            finishedAt = finishedAt,
            timedOut = timedOut,
        )

        private data class CollectionKey(
            val packetIndex: Int,
            val totalPackets: Int,
            val measuredAt: Instant,
        )
    }

    private fun ByteArray.readUInt16LittleEndian(offset: Int): Int =
        this[offset].toUnsignedInt() or (this[offset + 1].toUnsignedInt() shl 8)

    private fun Byte.toUnsignedInt(): Int = toInt() and 0xFF
}
