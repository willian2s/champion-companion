package br.com.mwconsulting.championcompanion.core.bluetooth.android

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleTimeouts
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCapabilitySnapshot
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelProperty
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattOperation
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattQueue
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattTarget
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattTerminalResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionOperationType
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSessionId

class AndroidCccdConfigurator(
    private val timeouts: ChampionBleTimeouts = ChampionBleTimeouts(),
) {
    suspend fun enableChannel(
        sessionId: ChampionSessionId,
        channel: ChampionChannelAlias,
        capabilities: ChampionCapabilitySnapshot,
        queue: ChampionGattQueue,
    ): ChampionBleResult<Unit> {
        val capability = capabilities.capabilityOf(channel)
            ?: return ChampionBleResult.Failure(ChampionBleError.MissingRequiredChannel(channel))

        val physicalOperationType = when {
            capability.properties.contains(ChampionChannelProperty.NOTIFY) -> ChampionOperationType.CONFIGURE_NOTIFICATION
            capability.properties.contains(ChampionChannelProperty.INDICATE) -> ChampionOperationType.CONFIGURE_INDICATION
            else -> return ChampionBleResult.Failure(ChampionBleError.IncompatibleChannelProperties(channel))
        }

        val localEnable = queue.enqueue(
            ChampionGattOperation.create(
                sessionId = sessionId,
                type = physicalOperationType,
                target = ChampionGattTarget.Channel(channel),
                timeout = timeouts.forOperation(physicalOperationType),
            ),
        )
        if (localEnable is ChampionGattTerminalResult.Failure) {
            return ChampionBleResult.Failure(localEnable.error)
        }

        val descriptorWrite = queue.enqueue(
            ChampionGattOperation.create(
                sessionId = sessionId,
                type = ChampionOperationType.WRITE_DESCRIPTOR,
                target = ChampionGattTarget.Channel(channel),
                timeout = timeouts.forOperation(ChampionOperationType.WRITE_DESCRIPTOR),
                payload = cccdValueFor(physicalOperationType),
            ),
        )
        return when (descriptorWrite) {
            ChampionGattTerminalResult.Success -> ChampionBleResult.Success(Unit)
            is ChampionGattTerminalResult.Failure -> ChampionBleResult.Failure(descriptorWrite.error)
        }
    }

    private fun cccdValueFor(operationType: ChampionOperationType): ByteArray = when (operationType) {
        ChampionOperationType.CONFIGURE_NOTIFICATION -> byteArrayOf(0x01, 0x00)
        ChampionOperationType.CONFIGURE_INDICATION -> byteArrayOf(0x02, 0x00)
        else -> error("Unsupported CCCD operation type: $operationType")
    }
}
