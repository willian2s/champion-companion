package br.com.mwconsulting.championcompanion.core.bluetooth

object ChampionFea1StepsParser {
    const val parserVersion: String = "champion_fea1_steps_v1"

    private const val packetSize = 4
    private const val header = 0x01

    fun parse(packet: ChampionPacket): Result {
        val bytes = packet.bytesCopy()
        if (packet.size != packetSize || bytes[0].toInt() != header) {
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
