package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCommand
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacket
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionRetryPolicy
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionWriteType
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal object DevicePocHeartRateSession {
    const val COMMAND_CODE: Int = 0xD0
    const val DEFAULT_MAX_BUFFERED_SAMPLES: Int = 5_000
    val defaultTimeout: Duration = 15.seconds

    fun createStartCommand(timeout: Duration = defaultTimeout): ChampionCommand = ChampionCommand(
        commandCode = COMMAND_CODE,
        channel = ChampionChannelAlias.F008_WRITE,
        writeType = ChampionWriteType.WITH_RESPONSE,
        timeout = timeout,
        retryPolicy = ChampionRetryPolicy.None,
        responseMatcher = null,
        payload = byteArrayOf(COMMAND_CODE.toByte(), 0x01),
    )

    fun createStopCommand(timeout: Duration = defaultTimeout): ChampionCommand = ChampionCommand(
        commandCode = COMMAND_CODE,
        channel = ChampionChannelAlias.F008_WRITE,
        writeType = ChampionWriteType.WITH_RESPONSE,
        timeout = timeout,
        retryPolicy = ChampionRetryPolicy.None,
        responseMatcher = null,
        payload = byteArrayOf(COMMAND_CODE.toByte(), 0x00),
    )

    fun parseLiveBpm(
        packet: ChampionPacket,
        sessionStartedAt: Instant,
    ): D0Result {
        if (packet.channel != ChampionChannelAlias.F008_NOTIFY) return D0Result.Ignored
        if (packet.commandCode != COMMAND_CODE) return D0Result.Ignored
        if (packet.observedAt.isBefore(sessionStartedAt)) return D0Result.Ignored
        val bpm = packet.bytesCopy().drop(1).firstOrNull { byte ->
            val value = byte.toInt().and(0xFF)
            value in 35..220
        }?.toInt()?.and(0xFF) ?: return D0Result.Ignored
        return D0Result.Valid(bpm = bpm, observedAt = packet.observedAt)
    }

    fun parseF002Samples(packet: ChampionPacket): List<Int> {
        if (packet.channel != ChampionChannelAlias.F002_NOTIFY) return emptyList()
        val bytes = packet.bytesCopy()
        val samples = ArrayList<Int>(bytes.size / 2)
        var index = 0
        while (index + 1 < bytes.size) {
            samples += (bytes[index].toInt().and(0xFF)) or ((bytes[index + 1].toInt().and(0xFF)) shl 8)
            index += 2
        }
        return samples
    }

    fun appendLimitedBuffer(
        buffer: MutableList<Int>,
        samples: List<Int>,
        maxSamples: Int,
    ): Int {
        require(maxSamples > 0) { "maxSamples must be positive" }
        if (samples.isEmpty()) return buffer.size
        if (samples.size >= maxSamples) {
            buffer.clear()
            buffer.addAll(samples.takeLast(maxSamples))
            return buffer.size
        }
        buffer.addAll(samples)
        val overflow = buffer.size - maxSamples
        if (overflow > 0) {
            buffer.subList(0, overflow).clear()
        }
        return buffer.size
    }

    sealed interface D0Result {
        data class Valid(
            val bpm: Int,
            val observedAt: Instant,
        ) : D0Result

        data object Ignored : D0Result
    }
}
