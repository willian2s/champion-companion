package br.com.mwconsulting.championcompanion.core.bluetooth

import java.time.Instant
import kotlin.time.Duration

object ChampionD3WorkoutIndexProtocol {
    const val commandCode: Int = 0xD3
    private const val minimumPacketSize: Int = 5
    private const val packetIndexOffset: Int = 1
    private const val totalPacketsOffset: Int = 3
    private const val crcOffset: Int = 3
    private val plausiblePacketRange: IntRange = 1..256

    fun createIndexCommand(timeout: Duration = ChampionBleTimeouts().d3WorkoutIndex): ChampionCommand = ChampionCommand(
        commandCode = commandCode,
        channel = ChampionChannelAlias.F008_WRITE,
        writeType = ChampionWriteType.WITH_RESPONSE,
        timeout = timeout,
        retryPolicy = ChampionRetryPolicy.None,
        responseMatcher = null,
        payload = byteArrayOf(commandCode.toByte(), 0x01, 0x00, 0x00),
    )

    fun parsePacket(packet: ChampionPacket): D3IndexPacket? {
        if (packet.channel != ChampionChannelAlias.F008_NOTIFY) return null
        if (packet.commandCode != commandCode) return null
        return parsePayload(packet.bytesCopy())
    }

    fun parsePayload(bytes: ByteArray): D3IndexPacket? {
        if (bytes.size < minimumPacketSize) return null
        if (bytes[0].toUnsignedInt() != commandCode) return null

        val packetIndex = bytes.readUInt16LittleEndian(packetIndexOffset)
        val totalPackets = bytes.readUInt16LittleEndian(totalPacketsOffset)
        val hasPlausibleMultipartHeader =
            totalPackets in plausiblePacketRange && packetIndex in 1..totalPackets
        val availableCount = (bytes.size - crcOffset) / 2
        val crcPairs = buildList {
            repeat(availableCount) { offset ->
                val index = crcOffset + offset * 2
                val low = bytes[index].toUnsignedInt()
                val high = bytes[index + 1].toUnsignedInt()
                add(CrcCandidate(crc = low or (high shl 8), rawLittleEndian = byteArrayOf(low.toByte(), high.toByte())))
            }
        }

        return D3IndexPacket(
            packetIndex = packetIndex,
            totalPackets = totalPackets,
            hasPlausibleMultipartHeader = hasPlausibleMultipartHeader,
            crcCandidates = crcPairs,
        )
    }

    data class D3IndexContext(
        val importAttemptId: String,
        val bleSessionId: ChampionSessionId,
        val startedAt: Instant,
    ) {
        fun accepts(packet: ChampionPacket): Boolean =
            packet.channel == ChampionChannelAlias.F008_NOTIFY &&
                packet.sessionId == bleSessionId &&
                !packet.observedAt.isBefore(startedAt)
    }

    data class D3IndexProgress(
        val receivedPacketCount: Int = 0,
        val highestPacketIndex: Int = 0,
        val totalPackets: Int = 0,
    ) {
        val isIndeterminate: Boolean = true
    }

    data class D3WorkoutBlockEntry(
        val blockIndex: Int,
        val crc: Int,
        private val rawLittleEndian: ByteArray,
    ) {
        fun rawLittleEndianCopy(): ByteArray = rawLittleEndian.copyOf()
    }

    data class D3IndexPacket(
        val packetIndex: Int,
        val totalPackets: Int,
        val hasPlausibleMultipartHeader: Boolean,
        val crcCandidates: List<CrcCandidate>,
    )

    data class CrcCandidate(
        val crc: Int,
        private val rawLittleEndian: ByteArray,
    ) {
        fun rawLittleEndianCopy(): ByteArray = rawLittleEndian.copyOf()
    }

    data class D3IndexCollectionOutcome(
        val entries: List<D3WorkoutBlockEntry>,
        val progress: D3IndexProgress,
        val timedOut: Boolean,
        val receivedPacketCount: Int,
    )

    sealed interface D3IndexCollectionUpdate {
        data object Ignored : D3IndexCollectionUpdate
        data class Progress(val progress: D3IndexProgress) : D3IndexCollectionUpdate
        data class Finished(val outcome: D3IndexCollectionOutcome) : D3IndexCollectionUpdate
    }

    class IndexCollector(
        private val context: D3IndexContext,
    ) {
        private val packetsByIndex = linkedMapOf<Int, D3IndexPacket>()
        private var active = true
        private var receivedPacketCount = 0
        private var highestPacketIndex = 0
        private var highestExpectedPacketCount = 0

        fun onPacket(packet: ChampionPacket): D3IndexCollectionUpdate {
            if (!active) return D3IndexCollectionUpdate.Ignored
            if (!context.accepts(packet)) return D3IndexCollectionUpdate.Ignored

            val parsed = parsePacket(packet) ?: return D3IndexCollectionUpdate.Ignored
            receivedPacketCount += 1
            highestPacketIndex = maxOf(highestPacketIndex, parsed.packetIndex)
            highestExpectedPacketCount = maxOf(highestExpectedPacketCount, parsed.totalPackets)
            packetsByIndex.putIfAbsent(parsed.packetIndex, parsed)

            return if (parsed.hasPlausibleMultipartHeader) {
                if (parsed.totalPackets > 0 && hasReceivedAllPackets(parsed.totalPackets)) {
                    D3IndexCollectionUpdate.Finished(finish(timedOut = false))
                } else {
                    D3IndexCollectionUpdate.Progress(progressSnapshot())
                }
            } else {
                D3IndexCollectionUpdate.Finished(finish(timedOut = false))
            }
        }

        private fun hasReceivedAllPackets(totalPackets: Int): Boolean =
            (1..totalPackets).all(packetsByIndex::containsKey)

        fun onTimeout(): D3IndexCollectionOutcome = finish(timedOut = true)

        private fun finish(timedOut: Boolean): D3IndexCollectionOutcome {
            active = false
            return D3IndexCollectionOutcome(
                entries = collectEntries(),
                progress = progressSnapshot(),
                timedOut = timedOut,
                receivedPacketCount = receivedPacketCount,
            )
        }

        private fun collectEntries(): List<D3WorkoutBlockEntry> {
            val entriesByCrc = linkedMapOf<Int, D3WorkoutBlockEntry>()
            var physicalBlockIndex = 0
            packetsByIndex.toSortedMap().values
                .flatMap { it.crcCandidates }
                .forEach { candidate ->
                    val originalBlockIndex = physicalBlockIndex++
                    if (candidate.crc == 0 || entriesByCrc.containsKey(candidate.crc)) return@forEach
                    entriesByCrc[candidate.crc] = D3WorkoutBlockEntry(
                        blockIndex = originalBlockIndex,
                        crc = candidate.crc,
                        rawLittleEndian = candidate.rawLittleEndianCopy(),
                    )
                }
            return entriesByCrc.values.toList()
        }

        private fun progressSnapshot(): D3IndexProgress = D3IndexProgress(
            receivedPacketCount = receivedPacketCount,
            highestPacketIndex = highestPacketIndex,
            totalPackets = highestExpectedPacketCount,
        )
    }

    private fun ByteArray.readUInt16LittleEndian(offset: Int): Int =
        this[offset].toUnsignedInt() or (this[offset + 1].toUnsignedInt() shl 8)

    private fun Byte.toUnsignedInt(): Int = toInt() and 0xFF
}
