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
        return if (capability.supportsWrite(command.writeType)) {
            null
        } else {
            ChampionBleError.IncompatibleChannelProperties(command.channel)
        }
    }

    private fun ChampionChannelCapability.supportsWrite(writeType: ChampionWriteType): Boolean = when (writeType) {
        ChampionWriteType.WITH_RESPONSE ->
            properties.contains(ChampionChannelProperty.WRITE) ||
                properties.contains(ChampionChannelProperty.WRITE_WITHOUT_RESPONSE)

        ChampionWriteType.WITHOUT_RESPONSE ->
            properties.contains(ChampionChannelProperty.WRITE_WITHOUT_RESPONSE) ||
                properties.contains(ChampionChannelProperty.WRITE)
    }
}
