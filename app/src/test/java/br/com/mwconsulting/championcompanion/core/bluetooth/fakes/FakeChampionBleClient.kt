package br.com.mwconsulting.championcompanion.core.bluetooth.fakes

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleClient
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDisconnectReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattOperation
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattQueue
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattSessionEvent
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSession
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSessionId
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeChampionBleClient : ChampionBleClient {
    private val mutableEvents = MutableSharedFlow<ChampionGattSessionEvent>(extraBufferCapacity = 32)
    private var nextOpenResult: ChampionBleResult<ChampionSession>? = null

    val executedOperations = mutableListOf<ChampionGattOperation>()
    val attachedQueues = mutableMapOf<ChampionSessionId, ChampionGattQueue>()
    val disconnects = mutableListOf<Pair<ChampionSessionId, ChampionDisconnectReason>>()
    val closes = mutableListOf<ChampionSessionId>()

    override val events: Flow<ChampionGattSessionEvent> = mutableEvents.asSharedFlow()

    fun setOpenResult(result: ChampionBleResult<ChampionSession>) {
        nextOpenResult = result
    }

    suspend fun emit(event: ChampionGattSessionEvent) {
        mutableEvents.emit(event)
    }

    override suspend fun openSession(device: ChampionDiscoveredDevice): ChampionBleResult<ChampionSession> =
        nextOpenResult ?: ChampionBleResult.Success(
            ChampionSession(
                id = ChampionSessionId.random(),
                transportKey = device.transportKey,
                connectedAt = Instant.now(),
            ),
        )

    override fun attachQueue(sessionId: ChampionSessionId, queue: ChampionGattQueue) {
        attachedQueues[sessionId] = queue
    }

    override suspend fun execute(operation: ChampionGattOperation) {
        executedOperations += operation
    }

    override suspend fun disconnect(
        sessionId: ChampionSessionId,
        reason: ChampionDisconnectReason,
    ): ChampionBleResult<Unit> {
        disconnects += sessionId to reason
        return ChampionBleResult.Success(Unit)
    }

    override suspend fun close(sessionId: ChampionSessionId): ChampionBleResult<Unit> {
        closes += sessionId
        return ChampionBleResult.Success(Unit)
    }
}
