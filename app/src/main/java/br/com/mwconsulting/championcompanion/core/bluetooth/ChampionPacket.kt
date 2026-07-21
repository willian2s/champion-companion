package br.com.mwconsulting.championcompanion.core.bluetooth

import java.time.Instant

enum class ChampionPacketDirection {
    INBOUND,
    OUTBOUND,
}

class ChampionPacket(
    val sessionId: ChampionSessionId,
    val channel: ChampionChannelAlias,
    val direction: ChampionPacketDirection,
    val observedAt: Instant,
    val commandCode: Int?,
    bytes: ByteArray,
) {
    private val copiedBytes: ByteArray = bytes.copyOf()

    val size: Int = copiedBytes.size

    fun bytesCopy(): ByteArray = copiedBytes.copyOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChampionPacket) return false

        return sessionId == other.sessionId &&
            channel == other.channel &&
            direction == other.direction &&
            observedAt == other.observedAt &&
            commandCode == other.commandCode &&
            copiedBytes.contentEquals(other.copiedBytes)
    }

    override fun hashCode(): Int {
        var result = sessionId.hashCode()
        result = 31 * result + channel.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + observedAt.hashCode()
        result = 31 * result + (commandCode ?: 0)
        result = 31 * result + copiedBytes.contentHashCode()
        return result
    }

    override fun toString(): String =
        "ChampionPacket(sessionId=$sessionId, channel=$channel, direction=$direction, observedAt=$observedAt, commandCode=$commandCode, size=$size)"
}
