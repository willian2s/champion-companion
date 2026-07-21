package br.com.mwconsulting.championcompanion.core.bluetooth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    suspend fun sendCommand(
        sessionId: ChampionSessionId,
        queue: ChampionGattQueue,
        command: ChampionCommand,
    ): ChampionBleResult<Unit> {
        val terminal = queue.enqueue(
            ChampionGattOperation.create(
                sessionId = sessionId,
                type = ChampionOperationType.WRITE_CHARACTERISTIC,
                target = ChampionGattTarget.Channel(command.channel),
                writeType = command.writeType,
                timeout = command.timeout,
                retryPolicy = command.retryPolicy,
                payload = command.payloadCopy(),
            ),
        )
        return when (terminal) {
            ChampionGattTerminalResult.Success -> ChampionBleResult.Success(Unit)
            is ChampionGattTerminalResult.Failure -> ChampionBleResult.Failure(terminal.error)
        }
    }

    suspend fun executeCommand(
        sessionId: ChampionSessionId,
        queue: ChampionGattQueue,
        command: ChampionCommand,
    ): ChampionBleResult<ChampionPacket> = coroutineScope {
        val response = async(start = CoroutineStart.UNDISPATCHED) {
            awaitCommand(sessionId, command)
        }
        when (val sent = sendCommand(sessionId, queue, command)) {
            is ChampionBleResult.Failure -> {
                response.cancel()
                ChampionBleResult.Failure(sent.error)
            }

            is ChampionBleResult.Success -> response.await()
        }
    }

    suspend fun close(reason: ChampionDisconnectReason) {
        router.close(reason)
    }
}
