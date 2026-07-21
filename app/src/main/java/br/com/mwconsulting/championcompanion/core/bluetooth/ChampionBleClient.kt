package br.com.mwconsulting.championcompanion.core.bluetooth

import kotlinx.coroutines.flow.Flow

interface ChampionBleClient {
    val events: Flow<ChampionGattSessionEvent>

    suspend fun openSession(device: ChampionDiscoveredDevice): ChampionBleResult<ChampionSession>

    fun attachQueue(sessionId: ChampionSessionId, queue: ChampionGattQueue)

    suspend fun execute(operation: ChampionGattOperation)

    suspend fun disconnect(
        sessionId: ChampionSessionId,
        reason: ChampionDisconnectReason = ChampionDisconnectReason.USER_REQUESTED,
    ): ChampionBleResult<Unit>

    suspend fun close(sessionId: ChampionSessionId): ChampionBleResult<Unit>
}
