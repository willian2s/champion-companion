package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCapabilityResolver
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDisconnectReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattSessionEvent
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSession
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionTransportSession
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionUuidCatalog
import br.com.mwconsulting.championcompanion.core.bluetooth.SafeGattDetail
import br.com.mwconsulting.championcompanion.core.bluetooth.awaitCondition
import br.com.mwconsulting.championcompanion.core.bluetooth.capabilitySnapshot
import br.com.mwconsulting.championcompanion.core.bluetooth.sessionCapabilities
import br.com.mwconsulting.championcompanion.core.bluetooth.testDevice
import br.com.mwconsulting.championcompanion.core.bluetooth.testPacket
import br.com.mwconsulting.championcompanion.core.bluetooth.testSession
import br.com.mwconsulting.championcompanion.core.bluetooth.testSessionId
import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeChampionBleClient
import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeChampionBleScanner
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePocCoordinatorTest {
    @Test
    fun `reset cycle clears stale state and invalidates handshake`() = runBlocking {
        val coordinator = buildCoordinator()
        val session = testSession()
        val device = testDevice()

        coordinator.markReadyToScan()
        coordinator.handleTransportStateUpdate(ChampionBleState.Connecting(session, device))
        coordinator.handleTransportStateUpdate(
            ChampionBleState.TransportReady(
                session = session,
                capabilities = capabilitySnapshot(
                    ChampionChannelAlias.F008_NOTIFY,
                    ChampionChannelAlias.F008_WRITE,
                ),
            ),
        )
        assertTrue(coordinator.moveToHandshakeConfirmed(session.id))

        val previousCycleId = coordinator.state.value.cycleId
        coordinator.resetCycle()

        val updated = coordinator.state.value
        assertNotEquals(previousCycleId, updated.cycleId)
        assertEquals(DevicePocState.Idle, updated.pocState)
        assertEquals(ChampionBleState.Disconnected, updated.transportState)
        assertNull(updated.activeSessionId)
        assertNull(updated.candidate)
        assertNull(updated.lastError)
    }

    @Test
    fun `stale transport callback does not update new cycle`() = runBlocking {
        val coordinator = buildCoordinator()
        val session = testSession()
        val device = testDevice()

        coordinator.markReadyToScan()
        coordinator.handleTransportStateUpdate(ChampionBleState.Connecting(session, device))
        coordinator.resetCycle()
        val currentCycle = coordinator.state.value.cycleId

        coordinator.handleTransportStateUpdate(
            ChampionBleState.TransportReady(
                session = session,
                capabilities = capabilitySnapshot(
                    ChampionChannelAlias.F008_NOTIFY,
                    ChampionChannelAlias.F008_WRITE,
                ),
            ),
        )

        val updated = coordinator.state.value
        assertEquals(currentCycle, updated.cycleId)
        assertEquals(DevicePocState.Idle, updated.pocState)
        assertNull(updated.activeSessionId)
    }

    @Test
    fun `connect flow reaches transport ready and disconnect returns idle`() = runBlocking {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient()
        val session = testSession()
        val device = testDevice()
        client.setOpenResult(ChampionBleResult.Success(session))
        val manager = buildManager(scanner, client)
        val coordinator = DevicePocCoordinator(manager, Dispatchers.Default)

        coordinator.markReadyToScan()
        scanner.enqueueResult(ChampionBleResult.Success(listOf(device)))
        val scanResult = coordinator.scan(
            br.com.mwconsulting.championcompanion.core.bluetooth.ChampionScanRequest(500.milliseconds),
        )
        assertTrue(scanResult is ChampionBleResult.Success)
        assertEquals(DevicePocState.DeviceFound, coordinator.state.value.pocState)

        val connectResult = async { coordinator.connect(device) }
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
        yield()
        completeNextQueuedOperation(client, 0)
        completeNextQueuedOperation(client, 1)
        completeNextQueuedOperation(client, 2)

        val transport = (connectResult.await() as ChampionBleResult.Success<ChampionTransportSession>).value
        assertEquals(session.id, transport.session.id)
        awaitCondition { coordinator.state.value.pocState == DevicePocState.TransportReady }
        assertEquals(session.id, coordinator.state.value.activeSessionId)

        assertEquals(ChampionBleResult.Success(Unit), coordinator.disconnect(ChampionDisconnectReason.USER_REQUESTED))
        assertEquals(DevicePocState.Idle, coordinator.state.value.pocState)
        assertEquals(ChampionBleState.Disconnected, coordinator.state.value.transportState)
    }

    @Test
    fun `failed current session transitions to failed state`() = runBlocking {
        val coordinator = buildCoordinator()
        val session = testSession()
        val device = testDevice()

        coordinator.markReadyToScan()
        coordinator.handleTransportStateUpdate(ChampionBleState.Connecting(session, device))
        coordinator.handleTransportStateUpdate(
            ChampionBleState.Failed(
                session = session,
                error = ChampionBleError.UnexpectedDisconnect(
                    SafeGattDetail(statusCode = 19, statusLabel = "gone"),
                ),
            ),
        )

        val updated = coordinator.state.value
        assertTrue(updated.pocState is DevicePocState.Failed)
        assertEquals(session.id, updated.activeSessionId)
        assertEquals(
            ChampionBleError.UnexpectedDisconnect(SafeGattDetail(statusCode = 19, statusLabel = "gone")),
            updated.lastError,
        )
    }

    @Test
    fun `handshake succeeds only after matching notify and updates protocol state`() = runBlocking {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient()
        val session = testSession()
        val device = testDevice()
        client.setOpenResult(ChampionBleResult.Success(session))
        val manager = buildManager(scanner, client)
        val coordinator = DevicePocCoordinator(manager, Dispatchers.Default)

        connectToTransportReady(coordinator, scanner, client, session, device)

        val handshake = async {
            coordinator.executeHandshake(
                ZonedDateTime.of(2026, 7, 21, 14, 5, 9, 0, ZoneOffset.UTC),
                timeout = 200.milliseconds,
            )
        }
        completeNextQueuedOperation(client, 3)
        client.emit(
            ChampionGattSessionEvent.CharacteristicChanged(
                sessionId = session.id,
                occurredAt = Instant.now(),
                channel = ChampionChannelAlias.F008_NOTIFY,
                packet = br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacket(
                    sessionId = session.id,
                    channel = ChampionChannelAlias.F008_NOTIFY,
                    direction = br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacketDirection.INBOUND,
                    observedAt = Instant.now(),
                    commandCode = 0xA1,
                    bytes = ByteArray(20).also {
                        it[0] = 0xA1.toByte()
                        it[19] = 0x01
                    },
                ),
            ),
        )

        assertEquals(ChampionBleResult.Success(Unit), handshake.await())
        assertEquals(DevicePocState.HandshakeConfirmed, coordinator.state.value.pocState)
    }

    @Test
    fun `handshake timeout moves current session to failed`() = runBlocking {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient()
        val session = testSession()
        val device = testDevice()
        client.setOpenResult(ChampionBleResult.Success(session))
        val manager = buildManager(scanner, client)
        val coordinator = DevicePocCoordinator(manager, Dispatchers.Default)

        connectToTransportReady(coordinator, scanner, client, session, device)

        val handshake = async {
            coordinator.executeHandshake(
                ZonedDateTime.of(2026, 7, 21, 14, 5, 9, 0, ZoneOffset.UTC),
                timeout = 50.milliseconds,
            )
        }
        completeNextQueuedOperation(client, 3)

        val result = handshake.await()
        assertTrue(result is ChampionBleResult.Failure)
        assertTrue(coordinator.state.value.pocState is DevicePocState.Failed)
    }

    @Test
    fun `disconnect invalidates confirmed handshake`() = runBlocking {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient()
        val session = testSession()
        val device = testDevice()
        client.setOpenResult(ChampionBleResult.Success(session))
        val manager = buildManager(scanner, client)
        val coordinator = DevicePocCoordinator(manager, Dispatchers.Default)

        connectToTransportReady(coordinator, scanner, client, session, device)
        assertTrue(coordinator.moveToHandshakeConfirmed(session.id))

        coordinator.disconnect(ChampionDisconnectReason.USER_REQUESTED)

        assertEquals(DevicePocState.Idle, coordinator.state.value.pocState)
        assertEquals(ChampionBleState.Disconnected, coordinator.state.value.transportState)
        assertNull(coordinator.state.value.activeSessionId)
    }

    @Test
    fun `await steps fails with specific capability error and keeps transport ready`() = runBlocking {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient()
        val session = testSession()
        val device = testDevice()
        client.setOpenResult(ChampionBleResult.Success(session))
        val manager = buildManager(scanner, client)
        val coordinator = DevicePocCoordinator(manager, Dispatchers.Default)

        connectToTransportReady(coordinator, scanner, client, session, device)
        assertTrue(coordinator.moveToHandshakeConfirmed(session.id))

        val result = coordinator.awaitSteps()

        assertEquals(
            ChampionBleResult.Failure(ChampionBleError.MissingOptionalChannel(ChampionChannelAlias.FEA1_NOTIFY)),
            result,
        )
        assertTrue(coordinator.state.value.transportState is ChampionBleState.TransportReady)
        assertTrue(coordinator.state.value.pocState is DevicePocState.Failed)
        assertTrue(coordinator.state.value.isStepsCapabilityMissing)
        assertFalse(coordinator.state.value.hasObservedSteps)
    }

    @Test
    fun `await steps subscribes via hub and stores sanitized evidence in memory`() = runBlocking {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient()
        val session = testSession()
        val device = testDevice()
        client.setOpenResult(ChampionBleResult.Success(session))
        val manager = buildManager(
            scanner = scanner,
            client = client,
            optionalChannels = setOf(ChampionChannelAlias.FEA1_NOTIFY),
        )
        val coordinator = DevicePocCoordinator(manager, Dispatchers.Default)

        connectToTransportReady(coordinator, scanner, client, session, device)
        assertTrue(coordinator.moveToHandshakeConfirmed(session.id))

        assertEquals(ChampionBleResult.Success(Unit), coordinator.awaitSteps())
        completeNextQueuedOperation(client, 3)
        awaitCondition { coordinator.state.value.pocState == DevicePocState.WaitingForSteps }

        client.emit(
            ChampionGattSessionEvent.CharacteristicChanged(
                sessionId = session.id,
                occurredAt = Instant.now(),
                channel = ChampionChannelAlias.FEA1_NOTIFY,
                packet = testPacket(
                    sessionId = session.id,
                    channel = ChampionChannelAlias.FEA1_NOTIFY,
                    commandCode = 0x01,
                    bytes = byteArrayOf(0x01, 0x56, 0x34, 0x12),
                ),
            ),
        )

        awaitCondition { coordinator.state.value.pocState == DevicePocState.StepsObserved }
        val updated = coordinator.state.value
        assertTrue(updated.isStepsCapabilityAvailable)
        assertFalse(updated.isStepsCapabilityMissing)
        assertTrue(updated.hasObservedSteps)
        assertEquals(1, updated.stepsPacketCount)
        assertEquals(0x123456, updated.lastStepCount)
        assertNull(updated.lastError)
    }

    private fun buildCoordinator(): DevicePocCoordinator {
        val manager = buildManager(FakeChampionBleScanner(), FakeChampionBleClient())
        return DevicePocCoordinator(manager, Dispatchers.Default)
    }

    private fun buildManager(
        scanner: FakeChampionBleScanner,
        client: FakeChampionBleClient,
        optionalChannels: Set<ChampionChannelAlias> = emptySet(),
    ): ChampionConnectionManager = ChampionConnectionManager(
        scanner = scanner,
        client = client,
        capabilityResolver = ChampionCapabilityResolver { currentSession, _ ->
            ChampionBleResult.Success(
                sessionCapabilities(
                    currentSession.id,
                    ChampionChannelAlias.F008_NOTIFY,
                    ChampionChannelAlias.F008_WRITE,
                    *optionalChannels.toTypedArray(),
                ),
            )
        },
        parentScope = CoroutineScope(Dispatchers.Default + Job()),
        timeouts = br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleTimeouts(
            connect = 200.milliseconds,
            discovery = 200.milliseconds,
        ),
    )

    private suspend fun completeNextQueuedOperation(client: FakeChampionBleClient, index: Int) {
        awaitCondition { client.executedOperations.size > index }
        val operation = client.executedOperations[index]
        client.attachedQueues.getValue(operation.sessionId).complete(operation.sessionId, operation.operationId)
    }

    private suspend fun connectToTransportReady(
        coordinator: DevicePocCoordinator,
        scanner: FakeChampionBleScanner,
        client: FakeChampionBleClient,
        session: ChampionSession,
        device: br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice,
    ) = coroutineScope {
        coordinator.markReadyToScan()
        scanner.enqueueResult(ChampionBleResult.Success(listOf(device)))
        coordinator.scan(
            br.com.mwconsulting.championcompanion.core.bluetooth.ChampionScanRequest(500.milliseconds),
        )

        val connectResult = async { coordinator.connect(device) }
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
        yield()
        completeNextQueuedOperation(client, 0)
        completeNextQueuedOperation(client, 1)
        completeNextQueuedOperation(client, 2)
        connectResult.await()
        awaitCondition { coordinator.state.value.pocState == DevicePocState.TransportReady }
    }
}
