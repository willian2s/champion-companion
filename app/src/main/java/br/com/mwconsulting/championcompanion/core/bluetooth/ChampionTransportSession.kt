package br.com.mwconsulting.championcompanion.core.bluetooth

import kotlinx.coroutines.Job

data class ChampionTransportSession(
    val session: ChampionSession,
    val capabilities: ChampionSessionCapabilities,
    val queue: ChampionGattQueue,
    val notifications: ChampionNotificationHub,
    val streams: ChampionTransportStreams,
    internal val internalJobs: List<Job>,
) {
    suspend fun sendCommand(command: ChampionCommand): ChampionBleResult<Unit> {
        validateCommand(command)?.let { return ChampionBleResult.Failure(it) }
        return streams.sendCommand(session.id, queue, command)
    }

    suspend fun executeCommand(command: ChampionCommand): ChampionBleResult<ChampionPacket> {
        validateCommand(command)?.let { return ChampionBleResult.Failure(it) }
        return streams.executeCommand(session.id, queue, command)
    }

    private fun validateCommand(command: ChampionCommand): ChampionBleError? {
        val capability = capabilities.snapshot.capabilityOf(command.channel)
            ?: return ChampionBleError.MissingRequiredChannel(command.channel)
        val requiredProperty = when (command.writeType) {
            ChampionWriteType.WITH_RESPONSE -> ChampionChannelProperty.WRITE
            ChampionWriteType.WITHOUT_RESPONSE -> ChampionChannelProperty.WRITE_WITHOUT_RESPONSE
        }
        return if (capability.properties.contains(requiredProperty)) {
            null
        } else {
            ChampionBleError.IncompatibleChannelProperties(command.channel)
        }
    }
}
