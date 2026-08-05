package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCapabilityResolver
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDisconnectReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattOperation
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattSessionEvent
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSession
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleTimeouts
import br.com.mwconsulting.championcompanion.core.bluetooth.awaitCondition
import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeChampionBleClient
import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeChampionBleScanner
import br.com.mwconsulting.championcompanion.core.bluetooth.sessionCapabilities
import br.com.mwconsulting.championcompanion.core.bluetooth.testDevice
import br.com.mwconsulting.championcompanion.core.bluetooth.testSession
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceConnectionBootstrapTest {
    @Test
    fun `failed handshake disconnects the transport created by bootstrap`() = runBlocking {
        val fixture = createFixture()
        val result = async {
            fixture.bootstrap.connectAndHandshake(
                device = testDevice(),
                timeout = 200.milliseconds,
            )
        }

        completeConnection(fixture)
        awaitCondition { fixture.client.executedOperations.size > HANDSHAKE_OPERATION_INDEX }
        val handshakeOperation = fixture.client.executedOperations[HANDSHAKE_OPERATION_INDEX]
        fixture.client.attachedQueues.getValue(handshakeOperation.sessionId).fail(
            sessionId = handshakeOperation.sessionId,
            operationId = handshakeOperation.operationId,
            error = ChampionBleError.OperationUnavailable,
        )

        val failure = result.await()
        assertTrue(failure is ChampionBleResult.Failure)
        assertEquals(
            ChampionBleError.OperationUnavailable,
            (failure as ChampionBleResult.Failure).error,
        )
        assertNull(fixture.manager.currentSession())
        assertTrue(fixture.manager.state.value is ChampionBleState.Disconnected)
        assertEquals(
            fixture.session.id to ChampionDisconnectReason.CLEANUP,
            fixture.client.disconnects.last(),
        )
        assertEquals(fixture.session.id, fixture.client.closes.last())
    }

    @Test
    fun `cancelling handshake disconnects the transport created by bootstrap`() = runBlocking {
        val fixture = createFixture()
        val attempt = async {
            fixture.bootstrap.connectAndHandshake(
                device = testDevice(),
                timeout = 200.milliseconds,
            )
        }

        completeConnection(fixture)
        awaitCondition { fixture.manager.state.value is ChampionBleState.TransportReady }

        attempt.cancelAndJoin()

        assertNull(fixture.manager.currentSession())
        assertTrue(fixture.manager.state.value is ChampionBleState.Disconnected)
        assertEquals(
            fixture.session.id to ChampionDisconnectReason.CLEANUP,
            fixture.client.disconnects.last(),
        )
        assertEquals(fixture.session.id, fixture.client.closes.last())
    }

    private suspend fun completeConnection(fixture: Fixture) {
        awaitCondition { fixture.client.attachedQueues.containsKey(fixture.session.id) }
        fixture.client.emit(
            ChampionGattSessionEvent.ConnectionStateChanged(
                sessionId = fixture.session.id,
                occurredAt = Instant.now(),
                transportKey = fixture.session.transportKey,
                isConnected = true,
                detail = null,
            ),
        )
        completeNextQueuedOperation(fixture.client, 0)
        completeNextQueuedOperation(fixture.client, 1)
        completeNextQueuedOperation(fixture.client, 2)
    }

    private suspend fun completeNextQueuedOperation(client: FakeChampionBleClient, index: Int) {
        awaitCondition { client.executedOperations.size > index }
        val operation: ChampionGattOperation = client.executedOperations[index]
        client.attachedQueues.getValue(operation.sessionId).complete(
            sessionId = operation.sessionId,
            operationId = operation.operationId,
        )
    }

    private suspend fun createFixture(): Fixture {
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
            timeouts = ChampionBleTimeouts(
                connect = 200.milliseconds,
                discovery = 200.milliseconds,
            ),
        )
        yield()
        return Fixture(
            session = session,
            client = client,
            manager = manager,
            bootstrap = DeviceConnectionBootstrap(manager),
        )
    }

    private data class Fixture(
        val session: ChampionSession,
        val client: FakeChampionBleClient,
        val manager: ChampionConnectionManager,
        val bootstrap: DeviceConnectionBootstrap,
    )

    private companion object {
        const val HANDSHAKE_OPERATION_INDEX = 3
    }
}
