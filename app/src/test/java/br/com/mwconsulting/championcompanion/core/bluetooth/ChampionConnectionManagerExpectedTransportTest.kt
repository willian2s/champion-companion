package br.com.mwconsulting.championcompanion.core.bluetooth

import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeChampionBleClient
import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeChampionBleScanner
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionConnectionManagerExpectedTransportTest {
    @Test
    fun `stale expected transport cannot disconnect a newer active transport`() = runBlocking {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient()
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
            timeouts = ChampionBleTimeouts(
                connect = 200.milliseconds,
                discovery = 200.milliseconds,
            ),
        )
        yield()

        val firstSession = testSession(1)
        val firstTransport = connect(
            manager = manager,
            client = client,
            session = firstSession,
            device = testDevice(transportKey = "device-1"),
            operationOffset = 0,
        )

        val secondSession = testSession(2)
        val secondTransport = connect(
            manager = manager,
            client = client,
            session = secondSession,
            device = testDevice(transportKey = "device-2"),
            operationOffset = 3,
        )
        val disconnectCountBeforeStaleCleanup = client.disconnects.size

        val staleCleanup = manager.disconnectExpectedTransport(firstTransport)

        assertTrue(staleCleanup is ChampionBleResult.Failure)
        assertTrue(
            (staleCleanup as ChampionBleResult.Failure).error is ChampionBleError.OperationCancelled,
        )
        assertSame(secondTransport, manager.currentSession())
        assertEquals(disconnectCountBeforeStaleCleanup, client.disconnects.size)
        assertTrue(manager.state.value is ChampionBleState.TransportReady)
    }

    private suspend fun connect(
        manager: ChampionConnectionManager,
        client: FakeChampionBleClient,
        session: ChampionSession,
        device: ChampionDiscoveredDevice,
        operationOffset: Int,
    ): ChampionTransportSession {
        client.setOpenResult(ChampionBleResult.Success(session))
        val result = kotlinx.coroutines.coroutineScope {
            val connection = async { manager.connect(device) }
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
            completeNextQueuedOperation(client, operationOffset)
            completeNextQueuedOperation(client, operationOffset + 1)
            completeNextQueuedOperation(client, operationOffset + 2)
            connection.await()
        }
        return (result as ChampionBleResult.Success).value
    }

    private suspend fun completeNextQueuedOperation(client: FakeChampionBleClient, index: Int) {
        awaitCondition { client.executedOperations.size > index }
        val operation = client.executedOperations[index]
        client.attachedQueues.getValue(operation.sessionId).complete(
            sessionId = operation.sessionId,
            operationId = operation.operationId,
        )
    }
}
