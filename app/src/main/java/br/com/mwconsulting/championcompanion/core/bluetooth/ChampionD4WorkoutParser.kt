package br.com.mwconsulting.championcompanion.core.bluetooth

import br.com.mwconsulting.championcompanion.core.model.AbsenceReason
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadId
import br.com.mwconsulting.championcompanion.core.model.DomainValue
import br.com.mwconsulting.championcompanion.core.model.ParserValidity
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutD4Layout
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportErrorCategory
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutParserInput
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutParserResult
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutParserSummary
import br.com.mwconsulting.championcompanion.core.model.WorkoutDistanceMetrics
import br.com.mwconsulting.championcompanion.core.model.WorkoutHeartRateSample
import br.com.mwconsulting.championcompanion.core.model.WorkoutInterval
import br.com.mwconsulting.championcompanion.core.model.WorkoutMetrics
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ChampionD4WorkoutParser {
    private const val minimumPacketSize: Int = 20
    private val validBpmRange: IntRange = 30..220
    private const val maxObservedRecordCount: Int = 24 * 60

    fun parse(request: Request): WatchWorkoutParserResult {
        val packetsByIndex = request.block.packets.groupBy { it.packetIndex }
        val packet1 = packetsByIndex[1]?.firstOrNull() ?: return invalid(WatchWorkoutImportErrorCategory.REQUIRED_PACKET_MISSING, request)
        val packet2 = packetsByIndex[2]?.firstOrNull() ?: return invalid(WatchWorkoutImportErrorCategory.REQUIRED_PACKET_MISSING, request)
        val packet3 = packetsByIndex[3]?.firstOrNull() ?: return invalid(WatchWorkoutImportErrorCategory.REQUIRED_PACKET_MISSING, request)

        val startEnd = parsePacket1(packet1.rawBytesCopy())
            ?: return invalid(WatchWorkoutImportErrorCategory.INVALID_LAYOUT, request)
        val totals = parsePacket2(packet2.rawBytesCopy())
            ?: return invalid(WatchWorkoutImportErrorCategory.INVALID_LAYOUT, request)
        val durationData = parsePacket3(packet3.rawBytesCopy())
            ?: return invalid(WatchWorkoutImportErrorCategory.INVALID_LAYOUT, request)

        val durationFromPacket1 = Duration.between(startEnd.startedAt, startEnd.endedAt).seconds
        val resolvedDurationSeconds = when {
            durationFromPacket1 > 0 -> durationFromPacket1
            durationData.durationFallbackSeconds > 0 -> durationData.durationFallbackSeconds
            else -> 0L
        }
        val resolvedEndedAt = if (startEnd.endedAt.isAfter(startEnd.startedAt)) {
            startEnd.endedAt
        } else {
            startEnd.startedAt.plusSeconds(resolvedDurationSeconds)
        }

        val distinctSamplePackets = linkedMapOf<Int, ChampionD4WorkoutBlockProtocol.D4CollectedPacket>()
        request.block.packets.filter { it.packetIndex >= 4 }.forEach { packet ->
            if (!distinctSamplePackets.containsKey(packet.packetIndex)) {
                distinctSamplePackets[packet.packetIndex] = packet
            }
        }

        val parsedSamples = distinctSamplePackets.values.mapNotNull { parseSamplePacket(it.rawBytesCopy(), it.packetIndex) }
            .sortedBy { it.minuteIndex }
        val intervals = parsedSamples.map {
            WorkoutInterval(
                index = it.minuteIndex,
                duration = Duration.ofSeconds(60),
                steps = it.stepsDelta.toLong(),
                distanceMeters = DomainValue.Present(distanceRawToMeters(it.distanceRawDelta)),
                caloriesKilocalories = DomainValue.Present(caloriesRawToKilocalories(it.caloriesRawDelta)),
            )
        }
        val heartRateSamples = parsedSamples.mapNotNull { sample ->
            sample.bpm?.let { bpm ->
                WorkoutHeartRateSample(
                    sampledAt = startEnd.startedAt.plusSeconds(sample.minuteIndex.toLong() * 60L),
                    bpm = bpm,
                )
            }
        }.sortedBy { it.sampledAt }

        val validBpms = heartRateSamples.map { it.bpm }
        val averageBpm = if (validBpms.isEmpty()) DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE) else DomainValue.Present(validBpms.average().toInt())
        val minimumBpm = if (validBpms.isEmpty()) DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE) else DomainValue.Present(validBpms.min())
        val maximumBpm = if (validBpms.isEmpty()) DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE) else DomainValue.Present(validBpms.max())
        val originalDistanceMeters = distanceRawToMeters(totals.totalDistanceRaw)
        val caloriesKilocalories = caloriesRawToKilocalories(totals.totalCaloriesRaw)
        val duration = Duration.ofSeconds(resolvedDurationSeconds)
        val pace = if (originalDistanceMeters > 0.0 && resolvedDurationSeconds > 0) {
            DomainValue.Present(resolvedDurationSeconds.toDouble() * 1000.0 / originalDistanceMeters)
        } else {
            DomainValue.Absent(AbsenceReason.NOT_APPLICABLE)
        }
        val speed = if (originalDistanceMeters > 0.0 && resolvedDurationSeconds > 0) {
            DomainValue.Present(originalDistanceMeters / resolvedDurationSeconds.toDouble())
        } else {
            DomainValue.Absent(AbsenceReason.NOT_APPLICABLE)
        }

        val metrics = WorkoutMetrics(
            startedAt = startEnd.startedAt,
            duration = duration,
            steps = totals.totalSteps.toLong(),
            caloriesKilocalories = DomainValue.Present(caloriesKilocalories),
            distances = WorkoutDistanceMetrics(
                originalMeters = DomainValue.Present(originalDistanceMeters),
                estimatedMeters = DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE),
                calibratedMeters = DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED),
            ),
            averagePaceSecondsPerKilometer = pace,
            averageSpeedMetersPerSecond = speed,
            averageBpm = averageBpm,
            minimumBpm = minimumBpm,
            maximumBpm = maximumBpm,
        )

        val outOfDurationSampleCount = heartRateSamples.count { it.sampledAt.isAfter(resolvedEndedAt) }
        val ignoredHeartRateSampleCount = parsedSamples.count { it.bpm == null }
        val crcMatchesD3 = durationData.reportedCrc == request.crc
        val hasPartialCondition = request.block.timedOut ||
            request.block.missingPacketIndices.isNotEmpty() ||
            request.block.duplicatePacketIndices.isNotEmpty() ||
            (durationData.recordCount > 0 && parsedSamples.size != durationData.recordCount) ||
            !crcMatchesD3 ||
            outOfDurationSampleCount > 0
        val layout = resolveOverallLayout(startEnd.layout, totals.layout, durationData.layout)
        val parserValidity = if (hasPartialCondition) ParserValidity.PARTIAL else ParserValidity.VALID
        val input = WatchWorkoutParserInput(
            deviceId = request.deviceId,
            crc = request.crc,
            externalBlockIndex = request.externalBlockIndex,
            parserVersion = request.parserVersion,
            parserValidity = parserValidity,
            diagnosticPayloadId = request.diagnosticPayloadId,
            metrics = metrics,
            intervals = intervals,
            heartRateSamples = heartRateSamples,
        )
        val summary = WatchWorkoutParserSummary(
            layout = layout,
            timedOut = request.block.timedOut,
            missingPacketIndices = request.block.missingPacketIndices,
            duplicatePacketIndices = request.block.duplicatePacketIndices,
            recordCount = durationData.recordCount,
            parsedIntervalCount = intervals.size,
            validHeartRateSampleCount = heartRateSamples.size,
            ignoredHeartRateSampleCount = ignoredHeartRateSampleCount,
            outOfDurationSampleCount = outOfDurationSampleCount,
            reportedBlockCrc = durationData.reportedCrc,
            crcMatchesD3 = crcMatchesD3,
        )
        return if (parserValidity == ParserValidity.VALID) {
            WatchWorkoutParserResult.Valid(input = input, summary = summary)
        } else {
            WatchWorkoutParserResult.Partial(input = input, summary = summary)
        }
    }

    data class Request(
        val deviceId: DeviceId,
        val crc: Int,
        val externalBlockIndex: Int,
        val parserVersion: ParserVersion,
        val diagnosticPayloadId: DiagnosticPayloadId? = null,
        val block: ChampionD4WorkoutBlockProtocol.D4BlockCollectionOutcome,
    )

    private data class Packet1Data(
        val startedAt: Instant,
        val endedAt: Instant,
        val layout: WatchWorkoutD4Layout,
    )

    private data class Packet2Data(
        val totalSteps: Int,
        val totalDistanceRaw: Int,
        val totalCaloriesRaw: Int,
        val layout: WatchWorkoutD4Layout,
    )

    private data class Packet3Data(
        val durationFallbackSeconds: Long,
        val recordCount: Int,
        val reportedCrc: Int,
        val layout: WatchWorkoutD4Layout,
    )

    private data class SampleData(
        val minuteIndex: Int,
        val bpm: Int?,
        val stepsDelta: Int,
        val distanceRawDelta: Int,
        val caloriesRawDelta: Int,
    )

    private fun invalid(
        category: WatchWorkoutImportErrorCategory,
        request: Request,
        layout: WatchWorkoutD4Layout? = null,
        recordCount: Int? = null,
        reportedBlockCrc: Int? = null,
    ): WatchWorkoutParserResult.Invalid = WatchWorkoutParserResult.Invalid(
        errorCategory = category,
        summary = WatchWorkoutParserSummary(
            layout = layout,
            timedOut = request.block.timedOut,
            missingPacketIndices = request.block.missingPacketIndices,
            duplicatePacketIndices = request.block.duplicatePacketIndices,
            recordCount = recordCount,
            reportedBlockCrc = reportedBlockCrc,
            crcMatchesD3 = reportedBlockCrc?.let { it == request.crc },
        ),
    )

    private fun parsePacket1(bytes: ByteArray): Packet1Data? {
        if (bytes.size < minimumPacketSize || bytes[0].toUnsignedInt() != ChampionD4WorkoutBlockProtocol.commandCode) return null
        val observedStart = buildTimestamp(
            year = bytes.readUInt16LittleEndian(7),
            month = bytes[9].toUnsignedInt(),
            day = bytes[10].toUnsignedInt(),
            hour = bytes[11].toUnsignedInt(),
            minute = bytes[12].toUnsignedInt(),
            second = bytes[13].toUnsignedInt(),
        )
        val observedEnd = buildTimestamp(
            year = bytes.readUInt16LittleEndian(14),
            month = bytes[16].toUnsignedInt(),
            day = bytes[17].toUnsignedInt(),
            hour = bytes[18].toUnsignedInt(),
            minute = bytes[19].toUnsignedInt(),
            second = 0,
        )
        if (observedStart != null && observedEnd != null) {
            return Packet1Data(observedStart, observedEnd, WatchWorkoutD4Layout.PREFERRED)
        }

        val legacyStart = buildTimestamp(
            year = bytes.readUInt16LittleEndian(10),
            month = bytes[8].toUnsignedInt(),
            day = bytes[9].toUnsignedInt(),
            hour = bytes[5].toUnsignedInt(),
            minute = bytes[6].toUnsignedInt(),
            second = bytes[7].toUnsignedInt(),
        )
        val legacyEnd = buildTimestamp(
            year = bytes.readUInt16LittleEndian(17),
            month = bytes[15].toUnsignedInt(),
            day = bytes[16].toUnsignedInt(),
            hour = bytes[12].toUnsignedInt(),
            minute = bytes[13].toUnsignedInt(),
            second = bytes[14].toUnsignedInt(),
        )
        return if (legacyStart != null && legacyEnd != null) {
            Packet1Data(legacyStart, legacyEnd, WatchWorkoutD4Layout.LEGACY)
        } else {
            null
        }
    }

    private fun parsePacket2(bytes: ByteArray): Packet2Data? {
        if (bytes.size < minimumPacketSize || bytes[0].toUnsignedInt() != ChampionD4WorkoutBlockProtocol.commandCode) return null
        val observed = Packet2Data(
            totalSteps = bytes.readUInt16LittleEndian(7),
            totalDistanceRaw = bytes.readUInt16LittleEndian(11) * 100,
            totalCaloriesRaw = bytes.readUInt24LittleEndian(15),
            layout = WatchWorkoutD4Layout.PREFERRED,
        )
        val legacy = Packet2Data(
            totalSteps = bytes.readUInt24LittleEndian(5),
            totalDistanceRaw = bytes.readUInt32LittleEndian(8),
            totalCaloriesRaw = bytes.readUInt32LittleEndian(12),
            layout = WatchWorkoutD4Layout.LEGACY,
        )
        val observedLooksPlausible = observed.totalSteps in 1..200_000 &&
            observed.totalDistanceRaw in 0..500_000_000 &&
            observed.totalCaloriesRaw in 0..10_000_000
        val legacyLooksPlausible = legacy.totalSteps in 1..200_000 &&
            legacy.totalDistanceRaw in 0..500_000_000 &&
            legacy.totalCaloriesRaw in 0..10_000_000
        return if (observedLooksPlausible && !legacyLooksPlausible) observed else legacy
    }

    private fun parsePacket3(bytes: ByteArray): Packet3Data? {
        if (bytes.size < minimumPacketSize || bytes[0].toUnsignedInt() != ChampionD4WorkoutBlockProtocol.commandCode) return null
        val observedRecordCount = bytes[9].toUnsignedInt()
        val observedCrc = bytes.readUInt16LittleEndian(14)
        if (observedRecordCount in 1 until maxObservedRecordCount && observedCrc != 0) {
            return Packet3Data(
                durationFallbackSeconds = 0L,
                recordCount = observedRecordCount,
                reportedCrc = observedCrc,
                layout = WatchWorkoutD4Layout.PREFERRED,
            )
        }

        return Packet3Data(
            durationFallbackSeconds = bytes.readUInt32LittleEndian(5).toLong(),
            recordCount = bytes.readUInt16LittleEndian(9),
            reportedCrc = bytes.readUInt16LittleEndian(11),
            layout = WatchWorkoutD4Layout.LEGACY,
        )
    }

    private fun parseSamplePacket(bytes: ByteArray, packetIndex: Int): SampleData? {
        if (bytes.size < minimumPacketSize || bytes[0].toUnsignedInt() != ChampionD4WorkoutBlockProtocol.commandCode) return null
        if (packetIndex < 4) return null
        val rawBpm = bytes[6].toUnsignedInt()
        return SampleData(
            minuteIndex = packetIndex - 4,
            bpm = rawBpm.takeIf { it in validBpmRange },
            stepsDelta = bytes.readUInt24LittleEndian(7),
            distanceRawDelta = bytes.readUInt24LittleEndian(10),
            caloriesRawDelta = bytes.readUInt16LittleEndian(13),
        )
    }

    private fun buildTimestamp(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int,
    ): Instant? {
        if (year !in 2020..2099 || month !in 1..12 || day !in 1..31 || hour !in 0..23 || minute !in 0..59 || second !in 0..59) {
            return null
        }
        return try {
            LocalDate.of(year, 1, 1)
                .plusMonths((month - 1).toLong())
                .plusDays((day - 1).toLong())
                .atTime(hour, minute, second)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveOverallLayout(vararg layouts: WatchWorkoutD4Layout): WatchWorkoutD4Layout =
        if (layouts.any { it == WatchWorkoutD4Layout.LEGACY }) WatchWorkoutD4Layout.LEGACY else WatchWorkoutD4Layout.PREFERRED

    private fun distanceRawToMeters(raw: Int): Double = kotlin.math.round(raw / 100.0)

    private fun caloriesRawToKilocalories(raw: Int): Double = kotlin.math.round(raw / 100.0) / 10.0

    private fun ByteArray.readUInt16LittleEndian(offset: Int): Int =
        this[offset].toUnsignedInt() or (this[offset + 1].toUnsignedInt() shl 8)

    private fun ByteArray.readUInt24LittleEndian(offset: Int): Int =
        this[offset].toUnsignedInt() or
            (this[offset + 1].toUnsignedInt() shl 8) or
            (this[offset + 2].toUnsignedInt() shl 16)

    private fun ByteArray.readUInt32LittleEndian(offset: Int): Int =
        this[offset].toUnsignedInt() or
            (this[offset + 1].toUnsignedInt() shl 8) or
            (this[offset + 2].toUnsignedInt() shl 16) or
            (this[offset + 3].toUnsignedInt() shl 24)

    private fun Byte.toUnsignedInt(): Int = toInt() and 0xFF
}
