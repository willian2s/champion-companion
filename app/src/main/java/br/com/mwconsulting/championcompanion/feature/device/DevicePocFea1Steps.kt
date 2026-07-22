package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionFea1StepsParser
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacket

internal object DevicePocFea1Steps {
    fun parse(packet: ChampionPacket): Result = when (val result = ChampionFea1StepsParser.parse(packet)) {
        is ChampionFea1StepsParser.Result.Valid -> Result.Valid(result.steps)
        is ChampionFea1StepsParser.Result.Invalid -> Result.Invalid(result.error)
    }

    sealed interface Result {
        data class Valid(val steps: Int) : Result

        data class Invalid(val error: ChampionBleError) : Result
    }
}
