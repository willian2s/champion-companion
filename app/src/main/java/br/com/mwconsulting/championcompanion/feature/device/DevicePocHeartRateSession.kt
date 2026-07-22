package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionManualHeartRateProtocol
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCommand
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacket
import java.time.Instant
import kotlin.time.Duration

internal object DevicePocHeartRateSession {
    const val COMMAND_CODE: Int = ChampionManualHeartRateProtocol.commandCode
    const val DEFAULT_MAX_BUFFERED_SAMPLES: Int = ChampionManualHeartRateProtocol.defaultMaxBufferedSamples
    val defaultTimeout: Duration = ChampionManualHeartRateProtocol.defaultTimeout

    fun createStartCommand(timeout: Duration = defaultTimeout): ChampionCommand =
        ChampionManualHeartRateProtocol.createStartCommand(timeout)

    fun createStopCommand(timeout: Duration = defaultTimeout): ChampionCommand =
        ChampionManualHeartRateProtocol.createStopCommand(timeout)

    fun parseLiveBpm(
        packet: ChampionPacket,
        sessionStartedAt: Instant,
    ): D0Result = when (val result = ChampionManualHeartRateProtocol.parseLiveBpm(packet, sessionStartedAt)) {
        ChampionManualHeartRateProtocol.D0Result.Ignored -> D0Result.Ignored
        is ChampionManualHeartRateProtocol.D0Result.Valid -> D0Result.Valid(result.bpm, result.observedAt)
    }

    fun parseF002Samples(packet: ChampionPacket): List<Int> = ChampionManualHeartRateProtocol.parseF002Samples(packet)

    fun appendLimitedBuffer(
        buffer: MutableList<Int>,
        samples: List<Int>,
        maxSamples: Int,
    ): Int = ChampionManualHeartRateProtocol.appendLimitedBuffer(buffer, samples, maxSamples)

    sealed interface D0Result {
        data class Valid(
            val bpm: Int,
            val observedAt: Instant,
        ) : D0Result

        data object Ignored : D0Result
    }
}
