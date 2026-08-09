package br.com.mwconsulting.championcompanion.core.bluetooth

import java.time.Instant
import kotlin.time.Duration

object ChampionD4WorkoutBlockProtocol {
    const val commandCode: Int = 0xD4
    private const val minimumHeaderSize: Int = 5
    private const val packetIndexOffset: Int = 1
    private const val totalPacketsOffset: Int = 3

    fun createBlockCommand(
        blockIndex: Int,
        timeout: Duration = ChampionBleTimeouts().d4WorkoutBlock,
    ): ChampionCommand {
        require(blockIndex in 0..0xFF) { "blockIndex must be between 0 and 255" }
        return ChampionCommand(
            commandCode = commandCode,
            channel = ChampionChannelAlias.F008_WRITE,
            writeType = ChampionWriteType.WITH_RESPONSE,
            timeout = timeout,
            retryPolicy = ChampionRetryPolicy.None,
            responseMatcher = null,
            payload = byteArrayOf(commandCode.toByte(), blockIndex.toByte(), 0x00, 0x00),
        )
    }

    fun parsePacket(packet: ChampionPacket): D4BlockPacket? {
        if (packet.channel != ChampionChannelAlias.F008_NOTIFY) return null
        if (packet.commandCode != commandCode) return null
        return parsePayload(packet.bytesCopy())
    }

    fun parsePayload(bytes: ByteArray): D4BlockPacket? {
        if (bytes.size < minimumHeaderSize) return null
        if (bytes[0].toUnsignedInt() != commandCode) return null
        return D4BlockPacket(
            packetIndex = bytes.readUInt16LittleEndian(packetIndexOffset),
            totalPackets = bytes.readUInt16LittleEndian(totalPacketsOffset),
            rawBytes = bytes,
        )
    }

    /** Recupera envelope diagnóstico local sem executar nova operação BLE. */
    fun parseDiagnosticBytes(bytes: ByteArray): D4BlockCollectionOutcome? {
        if (bytes.size < DIAGNOSTIC_HEADER_SIZE ||
            bytes[0].toUnsignedInt() != 'D'.code ||
            bytes[1].toUnsignedInt() != '4'.code ||
            bytes[2].toUnsignedInt() != 'B'.code ||
            bytes[3].toUnsignedInt() != '1'.code
        ) return null

        var offset = 4
        val blockIndex = bytes.readUInt16LittleEndian(offset).also { offset += 2 }
        val packetCount = bytes.readUInt16LittleEndian(offset).also { offset += 2 }
        if (packetCount !in 1..MAX_DIAGNOSTIC_PACKET_COUNT) return null
        var expectedTotalPackets: Int? = null
        val packets = buildList {
            repeat(packetCount) {
                if (offset + DIAGNOSTIC_PACKET_HEADER_SIZE > bytes.size) return null
                val packetIndex = bytes.readUInt16LittleEndian(offset).also { offset += 2 }
                val totalPackets = bytes.readUInt16LittleEndian(offset).also { offset += 2 }
                val byteCount = bytes.readUInt16LittleEndian(offset).also { offset += 2 }
                if (totalPackets !in 1..MAX_DIAGNOSTIC_PACKET_COUNT ||
                    packetIndex !in 1..totalPackets ||
                    (expectedTotalPackets != null && expectedTotalPackets != totalPackets) ||
                    byteCount < minimumHeaderSize ||
                    byteCount > bytes.size - offset
                ) return null
                expectedTotalPackets = totalPackets
                val rawBytes = bytes.copyOfRange(offset, offset + byteCount)
                offset += byteCount
                val parsed = parsePayload(rawBytes) ?: return null
                if (parsed.packetIndex != packetIndex || parsed.totalPackets != totalPackets) return null
                add(D4CollectedPacket(packetIndex, totalPackets, rawBytes))
            }
        }
        if (offset != bytes.size) return null

        val distinctIndices = packets.map { it.packetIndex }.toSet()
        val duplicateIndices = packets.map { it.packetIndex }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        val totalPackets = packets.maxOfOrNull { it.totalPackets } ?: 0
        val missingIndices = if (totalPackets > 0) {
            (1..totalPackets).filterNot(distinctIndices::contains).toSet()
        } else {
            emptySet()
        }
        return D4BlockCollectionOutcome(
            blockIndex = blockIndex,
            packets = packets,
            progress = D4BlockProgress(
                receivedPacketCount = packets.size,
                highestPacketIndex = packets.maxOfOrNull { it.packetIndex } ?: 0,
                totalPackets = totalPackets,
            ),
            duplicatePacketIndices = duplicateIndices,
            missingPacketIndices = missingIndices,
            timedOut = false,
            receivedPacketCount = packets.size,
        )
    }

    data class D4BlockContext(
        val importAttemptId: String,
        val bleSessionId: ChampionSessionId,
        val startedAt: Instant,
        val blockIndex: Int,
    ) {
        fun accepts(packet: ChampionPacket): Boolean =
            packet.channel == ChampionChannelAlias.F008_NOTIFY &&
                packet.sessionId == bleSessionId &&
                !packet.observedAt.isBefore(startedAt)
    }

    data class D4BlockProgress(
        val receivedPacketCount: Int = 0,
        val highestPacketIndex: Int = 0,
        val totalPackets: Int = 0,
    ) {
        val progressFraction: Float?
            get() = if (totalPackets > 0) {
                (highestPacketIndex.toFloat() / totalPackets.toFloat()).coerceIn(0f, 1f)
            } else {
                null
            }
    }

    class D4CollectedPacket(
        val packetIndex: Int,
        val totalPackets: Int,
        rawBytes: ByteArray,
    ) {
        private val copiedRawBytes = rawBytes.copyOf()

        fun rawBytesCopy(): ByteArray = copiedRawBytes.copyOf()
    }

    data class D4BlockPacket(
        val packetIndex: Int,
        val totalPackets: Int,
        private val rawBytes: ByteArray,
    ) {
        fun rawBytesCopy(): ByteArray = rawBytes.copyOf()
    }

    data class D4BlockCollectionOutcome(
        val blockIndex: Int,
        val packets: List<D4CollectedPacket>,
        val progress: D4BlockProgress,
        val duplicatePacketIndices: Set<Int>,
        val missingPacketIndices: Set<Int>,
        val timedOut: Boolean,
        val receivedPacketCount: Int,
    )

    sealed interface D4BlockCollectionUpdate {
        data object Ignored : D4BlockCollectionUpdate
        data class Progress(val progress: D4BlockProgress) : D4BlockCollectionUpdate
        data class Finished(val outcome: D4BlockCollectionOutcome) : D4BlockCollectionUpdate
    }

    class BlockCollector(
        private val context: D4BlockContext,
    ) {
        private val packets = mutableListOf<D4CollectedPacket>()
        private val distinctPacketIndices = linkedSetOf<Int>()
        private val duplicatePacketIndices = linkedSetOf<Int>()
        private var active = true
        private var receivedPacketCount = 0
        private var highestPacketIndex = 0
        private var highestExpectedPacketCount = 0

        fun onPacket(packet: ChampionPacket): D4BlockCollectionUpdate {
            if (!active) return D4BlockCollectionUpdate.Ignored
            if (!context.accepts(packet)) return D4BlockCollectionUpdate.Ignored

            val parsed = parsePacket(packet) ?: return D4BlockCollectionUpdate.Ignored
            receivedPacketCount += 1
            if (!distinctPacketIndices.add(parsed.packetIndex)) {
                duplicatePacketIndices += parsed.packetIndex
            }
            highestPacketIndex = maxOf(highestPacketIndex, parsed.packetIndex)
            highestExpectedPacketCount = maxOf(highestExpectedPacketCount, parsed.totalPackets)
            packets += D4CollectedPacket(
                packetIndex = parsed.packetIndex,
                totalPackets = parsed.totalPackets,
                rawBytes = parsed.rawBytesCopy(),
            )

            return if (parsed.totalPackets > 0 && parsed.packetIndex >= parsed.totalPackets) {
                D4BlockCollectionUpdate.Finished(finish(timedOut = false))
            } else {
                D4BlockCollectionUpdate.Progress(progressSnapshot())
            }
        }

        fun onTimeout(): D4BlockCollectionOutcome = finish(timedOut = true)

        private fun finish(timedOut: Boolean): D4BlockCollectionOutcome {
            active = false
            return D4BlockCollectionOutcome(
                blockIndex = context.blockIndex,
                packets = packets.sortedBy { it.packetIndex },
                progress = progressSnapshot(),
                duplicatePacketIndices = duplicatePacketIndices.toSet(),
                missingPacketIndices = missingPacketIndices(),
                timedOut = timedOut,
                receivedPacketCount = receivedPacketCount,
            )
        }

        private fun missingPacketIndices(): Set<Int> {
            val expectedPacketCount = highestExpectedPacketCount
            if (expectedPacketCount <= 0) return emptySet()
            return (1..expectedPacketCount).filterNot(distinctPacketIndices::contains).toSet()
        }

        private fun progressSnapshot(): D4BlockProgress = D4BlockProgress(
            receivedPacketCount = receivedPacketCount,
            highestPacketIndex = highestPacketIndex,
            totalPackets = highestExpectedPacketCount,
        )
    }

    private fun ByteArray.readUInt16LittleEndian(offset: Int): Int =
        this[offset].toUnsignedInt() or (this[offset + 1].toUnsignedInt() shl 8)

    private fun Byte.toUnsignedInt(): Int = toInt() and 0xFF

    private const val DIAGNOSTIC_HEADER_SIZE = 8
    private const val DIAGNOSTIC_PACKET_HEADER_SIZE = 6
    private const val MAX_DIAGNOSTIC_PACKET_COUNT = 4_096
}
