package br.com.mwconsulting.championcompanion.core.bluetooth

import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeChampionBleClient
import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeChampionBleScanner
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionBleConnectionManagerTest {
    @Test
    fun `connect reaches transport ready with expected state transitions and disconnect cleanup`() = runBlocking {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient()
        val session = testSession()
        client.setOpenResult(ChampionBleResult.Success(session))
        val manager = ChampionConnectionManager(
            scanner = scanner,
            client = client,
            capabilityResolver = ChampionCapabilityResolver { currentSession, _ ->
                ChampionBleResult.Success(
                    sessionCapabilities(
                        currentSession.id,
                        ChampionChannelAlias.F008_NOTIFY,
                        ChampionChannelAlias.F008_WRITE,
                    ),
                )
            },
            parentScope = CoroutineScope(Dispatchers.Default + Job()),
            timeouts = ChampionBleTimeouts(connect = 200.milliseconds, discovery = 200.milliseconds),
        )
        val states = mutableListOf<ChampionBleState>()
        val collector = launch { manager.state.collect(states::add) }
        yield()

        val result = async { manager.connect(testDevice()) }
        awaitCondition { client.attachedQueues.containsKey(session.id) }
        client.emit(
            ChampionGattSessionEvent.ConnectionStateChanged(
                sessionId = session.id,
                occurredAt = Instant.now(),
                transportKey = session.transportKey,
                isConnected = true,
                detail = null,
            ),
        )
        completeNextQueuedOperation(client, 0)
        completeNextQueuedOperation(client, 1)
        completeNextQueuedOperation(client, 2)

        val transport = (result.await() as ChampionBleResult.Success).value
        assertEquals(session.id, transport.session.id)
        assertNotNull(manager.currentSession())
        assertTrue(states.any { it is ChampionBleState.Connecting })
        assertTrue(states.any { it is ChampionBleState.DiscoveringServices })
        assertTrue(manager.state.value is ChampionBleState.TransportReady)

        assertEquals(ChampionBleResult.Success(Unit), manager.disconnect())
        assertTrue(scanner.cancelReasons.contains(ChampionCancellationReason.DISCONNECT))
        assertEquals(session.id to ChampionDisconnectReason.USER_REQUESTED, client.disconnects.last())
        assertEquals(session.id, client.closes.last())
        collector.cancel()
    }

    @Test
    fun `stale callback is ignored and active session disconnect becomes failed cleanup`() = runBlocking {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient()
        val session = testSession()
        client.setOpenResult(ChampionBleResult.Success(session))
        val manager = ChampionConnectionManager(
            scanner = scanner,
            client = client,
            capabilityResolver = ChampionCapabilityResolver { currentSession, _ ->
                ChampionBleResult.Success(
                    sessionCapabilities(
                        currentSession.id,
                        ChampionChannelAlias.F008_NOTIFY,
                        ChampionChannelAlias.F008_WRITE,
                    ),
                )
            },
            parentScope = CoroutineScope(Dispatchers.Default + Job()),
            timeouts = ChampionBleTimeouts(connect = 200.milliseconds, discovery = 200.milliseconds),
        )
        yield()

        val connectResult = async { manager.connect(testDevice()) }
        awaitCondition { client.attachedQueues.containsKey(session.id) }
        client.emit(
            ChampionGattSessionEvent.ConnectionStateChanged(
                sessionId = session.id,
                occurredAt = Instant.now(),
                transportKey = session.transportKey,
                isConnected = true,
                detail = null,
            ),
        )
        completeNextQueuedOperation(client, 0)
        completeNextQueuedOperation(client, 1)
        completeNextQueuedOperation(client, 2)
        connectResult.await()

        client.emit(
            ChampionGattSessionEvent.ConnectionStateChanged(
                sessionId = testSessionId(2),
                occurredAt = Instant.now(),
                transportKey = "stale-device",
                isConnected = false,
                detail = SafeGattDetail(statusCode = 8, statusLabel = "old"),
            ),
        )
        awaitCondition { manager.state.value is ChampionBleState.TransportReady }

        client.emit(
            ChampionGattSessionEvent.ConnectionStateChanged(
                sessionId = session.id,
                occurredAt = Instant.now(),
                transportKey = session.transportKey,
                isConnected = false,
                detail = SafeGattDetail(statusCode = 19, statusLabel = "gone"),
            ),
        )
        awaitCondition { manager.state.value is ChampionBleState.Failed }

        val failed = manager.state.value as ChampionBleState.Failed
        assertEquals(ChampionBleError.UnexpectedDisconnect(SafeGattDetail(statusCode = 19, statusLabel = "gone")), failed.error)
        assertEquals(session.id to ChampionDisconnectReason.TRANSPORT_FAILURE, client.disconnects.last())
        assertEquals(session.id, client.closes.last())
    }

    private suspend fun completeNextQueuedOperation(client: FakeChampionBleClient, index: Int) {
        awaitCondition { client.executedOperations.size > index }
        val operation = client.executedOperations[index]
        client.attachedQueues.getValue(operation.sessionId).complete(operation.sessionId, operation.operationId)
    }
}
