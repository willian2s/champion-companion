package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacket

internal object DevicePocFea1Steps {
    private const val PACKET_SIZE = 4
    private const val HEADER = 0x01

    fun parse(packet: ChampionPacket): Result {
        val bytes = packet.bytesCopy()
        if (packet.size != PACKET_SIZE || bytes[0].toInt() != HEADER) {
            return Result.Invalid(
                ChampionBleError.InvalidPayload(
                    channel = ChampionChannelAlias.FEA1_NOTIFY,
                    size = packet.size,
                ),
            )
        }
        val steps =
            (bytes[1].toInt() and 0xFF) or
                ((bytes[2].toInt() and 0xFF) shl 8) or
                ((bytes[3].toInt() and 0xFF) shl 16)
        return Result.Valid(steps)
    }

    sealed interface Result {
        data class Valid(val steps: Int) : Result

        data class Invalid(val error: ChampionBleError) : Result
    }
}
