package br.com.mwconsulting.championcompanion.core.bluetooth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class ChampionTransportStreams(
    packetSource: Flow<ChampionPacket>,
    scope: CoroutineScope,
) {
    private val router = ChampionPacketRouter(packetSource = packetSource, parentScope = scope)

    val packets: Flow<ChampionPacket> = router.packets

    fun observeChannel(
        sessionId: ChampionSessionId,
        channel: ChampionChannelAlias,
    ): Flow<ChampionPacket> = router.observeChannel(sessionId, channel)

    suspend fun awaitPacket(request: ChampionAwaitPacketRequest): ChampionBleResult<ChampionPacket> =
        router.awaitPacket(request)

    suspend fun awaitCommand(
        sessionId: ChampionSessionId,
        command: ChampionCommand,
    ): ChampionBleResult<ChampionPacket> = router.awaitPacket(
        ChampionAwaitPacketRequest(
            sessionId = sessionId,
            timeout = command.timeout,
            matcher = ChampionResponseMatchers.forCommand(sessionId, command),
        ),
    )

    suspend fun close(reason: ChampionDisconnectReason) {
        router.close(reason)
    }
}
