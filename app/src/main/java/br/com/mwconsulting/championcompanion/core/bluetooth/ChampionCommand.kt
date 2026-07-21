package br.com.mwconsulting.championcompanion.core.bluetooth

import kotlin.time.Duration

enum class ChampionWriteType {
    WITH_RESPONSE,
    WITHOUT_RESPONSE,
}

sealed interface ChampionRetryPolicy {
    data object None : ChampionRetryPolicy
    data class Limited(val maxAttempts: Int) : ChampionRetryPolicy {
        init {
            require(maxAttempts > 0) { "maxAttempts must be positive" }
        }
    }
}

fun interface ChampionResponseMatcher {
    fun matches(packet: ChampionPacket): Boolean
}

class ChampionCommand(
    val commandCode: Int,
    val channel: ChampionChannelAlias,
    val writeType: ChampionWriteType,
    val timeout: Duration,
    val retryPolicy: ChampionRetryPolicy,
    val responseMatcher: ChampionResponseMatcher?,
    payload: ByteArray,
) {
    private val copiedPayload: ByteArray = payload.copyOf()

    val size: Int = copiedPayload.size

    init {
        require(commandCode in 0..0xFF) { "commandCode must be between 0 and 255" }
        require(timeout.isPositive()) { "timeout must be positive" }
    }

    fun payloadCopy(): ByteArray = copiedPayload.copyOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChampionCommand) return false

        return commandCode == other.commandCode &&
            channel == other.channel &&
            writeType == other.writeType &&
            timeout == other.timeout &&
            retryPolicy == other.retryPolicy &&
            copiedPayload.contentEquals(other.copiedPayload)
    }

    override fun hashCode(): Int {
        var result = commandCode
        result = 31 * result + channel.hashCode()
        result = 31 * result + writeType.hashCode()
        result = 31 * result + timeout.hashCode()
        result = 31 * result + retryPolicy.hashCode()
        result = 31 * result + copiedPayload.contentHashCode()
        return result
    }

    override fun toString(): String =
        "ChampionCommand(commandCode=$commandCode, channel=$channel, writeType=$writeType, timeout=$timeout, retryPolicy=$retryPolicy, size=$size)"
}
