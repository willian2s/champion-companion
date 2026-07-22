package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCapabilityResolver
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDisconnectReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattSessionEvent
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacket
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacketDirection
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionTransportSession
import br.com.mwconsulting.championcompanion.core.bluetooth.awaitCondition
import br.com.mwconsulting.championcompanion.core.bluetooth.capabilitySnapshot
import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeChampionBleClient
import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeChampionBleScanner
import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeMonotonicClock
import br.com.mwconsulting.championcompanion.core.bluetooth.sessionCapabilities
import br.com.mwconsulting.championcompanion.core.bluetooth.testDevice
import br.com.mwconsulting.championcompanion.core.bluetooth.testSession
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationOwner
import br.com.mwconsulting.championcompanion.core.model.InMemoryForegroundBleOperationGate
import br.com.mwconsulting.championcompanion.feature.device.DeviceConnectionBootstrap
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionManualHeartRateCoordinatorTest {
    private lateinit var dispatcher: kotlinx.coroutines.ExecutorCoroutineDispatcher

    @Test
    fun `late callback after cancel is ignored`() = runHealthTest {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient()
        val device = testDevice(transportKey = "assoc-1")
        val session = testSession().copy(transportKey = device.transportKey)
        client.setOpenResult(ChampionBleResult.Success(session))
        val manager = buildManager(scanner, client, setOf(ChampionChannelAlias.F002_NOTIFY))
        val gate = InMemoryForegroundBleOperationGate()
        val clock = FakeMonotonicClock()
        val coordinator = buildCoordinator(manager, gate, clock)

        connectToTransportReady(manager, client, session, device)
        val nextOperationIndex = startMeasurement(coordinator, client, session, buildDomainDevice(device.transportKey))

        coordinator.cancelMeasurement(ManualHeartRateCancellationReason.USER_REQUESTED)
        resolveStopOperation(client, nextOperationIndex, complete = true)
        waitForCondition("cancelled state after cancel") { coordinator.state.value is HealthSurfaceState.Cancelled }

        client.emit(
            ChampionGattSessionEvent.CharacteristicChanged(
                sessionId = session.id,
                occurredAt = Instant.parse("2026-01-01T00:01:00Z"),
                channel = ChampionChannelAlias.F008_NOTIFY,
                packet = ChampionPacket(
                    sessionId = session.id,
                    channel = ChampionChannelAlias.F008_NOTIFY,
                    direction = ChampionPacketDirection.INBOUND,
                    observedAt = Instant.parse("2026-01-01T00:01:00Z"),
                    commandCode = 0xD0,
                    bytes = byteArrayOf(0xD0.toByte(), 0x48),
                ),
            ),
        )
        yield()

        val sessionState = (coordinator.state.value as HealthSurfaceState.Cancelled).session
        assertNull(sessionState.currentReading)
        assertEquals(ForegroundBleOperationOwner.MANUAL_HEART_RATE, gate.tryAcquireRequestOwnerAfterRelease())
    }

    @Test
    fun `stop failure does not block cleanup`() = runHealthTest {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient()
        val device = testDevice(transportKey = "assoc-1")
        val session = testSession().copy(transportKey = device.transportKey)
        client.setOpenResult(ChampionBleResult.Success(session))
        val manager = buildManager(scanner, client, setOf(ChampionChannelAlias.F002_NOTIFY))
        val gate = InMemoryForegroundBleOperationGate()
        val clock = FakeMonotonicClock()
        val coordinator = buildCoordinator(manager, gate, clock)

        connectToTransportReady(manager, client, session, device)
        val nextOperationIndex = startMeasurement(coordinator, client, session, buildDomainDevice(device.transportKey))

        coordinator.cancelMeasurement(ManualHeartRateCancellationReason.USER_REQUESTED)
        resolveStopOperation(client, nextOperationIndex, complete = false)
        waitForCondition("cancelled state after failed stop") { coordinator.state.value is HealthSurfaceState.Cancelled }

        assertNull(gate.observeActiveOperation().first())
    }

    @Test
    fun `timeout ignores stale value and fails with no valid reading`() = runHealthTest {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient()
        val device = testDevice(transportKey = "assoc-1")
        val session = testSession().copy(transportKey = device.transportKey)
        client.setOpenResult(ChampionBleResult.Success(session))
        val manager = buildManager(scanner, client, setOf(ChampionChannelAlias.F002_NOTIFY))
        val gate = InMemoryForegroundBleOperationGate()
        val clock = FakeMonotonicClock()
        val coordinator = buildCoordinator(manager, gate, clock, targetDuration = 1.seconds)

        connectToTransportReady(manager, client, session, device)
        val nextOperationIndex = startMeasurement(coordinator, client, session, buildDomainDevice(device.transportKey))

        val startedAt = (coordinator.state.value as HealthSurfaceState.Measuring).session.startedAt!!
        client.emit(
            ChampionGattSessionEvent.CharacteristicChanged(
                sessionId = session.id,
                occurredAt = startedAt.minusSeconds(5),
                channel = ChampionChannelAlias.F008_NOTIFY,
                packet = ChampionPacket(
                    sessionId = session.id,
                    channel = ChampionChannelAlias.F008_NOTIFY,
                    direction = ChampionPacketDirection.INBOUND,
                    observedAt = startedAt.minusSeconds(5),
                    commandCode = 0xD0,
                    bytes = byteArrayOf(0xD0.toByte(), 0x4A),
                ),
            ),
        )
        yield()

        clock.advanceBy(1_500)
        resolveStopOperation(client, nextOperationIndex, complete = true)
        waitForCondition("failed state after timeout") { coordinator.state.value is HealthSurfaceState.Failed }

        val failed = coordinator.state.value as HealthSurfaceState.Failed
        assertEquals(HealthMeasurementErrorCategory.NO_VALID_READING, failed.session.error?.category)
        assertNull(failed.session.finalResult)
    }

    private fun runHealthTest(block: suspend CoroutineScope.() -> Unit) {
        dispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()
        try {
            runBlocking {
                coroutineScope(block)
            }
        } finally {
            dispatcher.close()
        }
    }

    private fun CoroutineScope.buildManager(
        scanner: FakeChampionBleScanner,
        client: FakeChampionBleClient,
        optionalChannels: Set<ChampionChannelAlias>,
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
        parentScope = CoroutineScope(coroutineContext + Job()),
        timeouts = br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleTimeouts(
            connect = 5.seconds,
            discovery = 5.seconds,
        ),
    )

    private fun CoroutineScope.buildCoordinator(
        manager: ChampionConnectionManager,
        gate: InMemoryForegroundBleOperationGate,
        clock: FakeMonotonicClock,
        targetDuration: kotlin.time.Duration = ManualHeartRateTargetDuration,
    ): ChampionManualHeartRateCoordinator = ChampionManualHeartRateCoordinator(
        connectionManager = manager,
        connectionBootstrap = DeviceConnectionBootstrap(manager),
        foregroundBleOperationGate = gate,
        monotonicClock = clock,
        bleDispatcher = dispatcher,
        targetDuration = targetDuration,
        civilNow = { Instant.parse("2026-01-01T00:00:10Z") },
    )

    private suspend fun CoroutineScope.connectToTransportReady(
        manager: ChampionConnectionManager,
        client: FakeChampionBleClient,
        session: br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSession,
        device: br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice,
    ) {
        waitForCondition("client event subscriber") { client.eventSubscriptionCount.value >= 1 }
        val connect = async(start = CoroutineStart.UNDISPATCHED) { manager.connect(device) }
        waitForCondition("attached queue for connect") { client.attachedQueues.containsKey(session.id) }
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
        var nextOperationIndex = 0
        while (!connect.isCompleted) {
            waitForCondition("connect completion or operation index=$nextOperationIndex") {
                connect.isCompleted || client.executedOperations.size > nextOperationIndex
            }
            while (client.executedOperations.size > nextOperationIndex) {
                failOrCompleteNextOperation(client, nextOperationIndex, complete = true)
                nextOperationIndex += 1
            }
        }
        val result = connect.await()
        check(result is ChampionBleResult.Success<ChampionTransportSession>)
        waitForCondition("manager transport ready") { manager.state.value is ChampionBleState.TransportReady }
    }

    private suspend fun startMeasurement(
        coordinator: ChampionManualHeartRateCoordinator,
        client: FakeChampionBleClient,
        session: br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSession,
        device: Device,
    ): Int {
        val error = coordinator.startMeasurement(
            ManualHeartRateMeasurementRequest(
                manualMeasurementSessionId = "manual-1",
                associatedDevice = device,
            ),
        )
        check(error == null)

        val startIndex = client.executedOperations.size

        waitForCondition("handshake command queued") { client.executedOperations.size > startIndex }
        failOrCompleteNextOperation(client, startIndex, complete = true)
        delay(1500)
        check(coordinator.state.value !is HealthSurfaceState.Failed) {
            "coordinator failed before d0 start: ${coordinator.state.value} operations=${client.executedOperations.size}"
        }
        completeOperationsUntil(client, startIndex + 1) { operation ->
            operation.payloadCopy()?.contentEquals(byteArrayOf(0xD0.toByte(), 0x01)) == true
        }
        waitForCondition("measuring state after start") { coordinator.state.value is HealthSurfaceState.Measuring }
        return client.executedOperations.size
    }

    private suspend fun resolveStopOperation(
        client: FakeChampionBleClient,
        startIndex: Int,
        complete: Boolean,
    ) {
        val stopOperation = awaitOperation(client, startIndex) { operation ->
            operation.payloadCopy()?.contentEquals(byteArrayOf(0xD0.toByte(), 0x00)) == true
        }
        val queue = client.attachedQueues.getValue(stopOperation.sessionId)
        if (complete) {
            queue.complete(stopOperation.sessionId, stopOperation.operationId)
        } else {
            queue.fail(
                stopOperation.sessionId,
                stopOperation.operationId,
                br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError.OperationRejected(stopOperation.type),
            )
        }
        yield()
    }

    private suspend fun failOrCompleteNextOperation(
        client: FakeChampionBleClient,
        index: Int,
        complete: Boolean,
    ) {
        waitForCondition("operation index=$index queued") { client.executedOperations.size > index }
        val operation = client.executedOperations[index]
        val queue = client.attachedQueues.getValue(operation.sessionId)
        if (complete) {
            queue.complete(operation.sessionId, operation.operationId)
        } else {
            queue.fail(operation.sessionId, operation.operationId, br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError.OperationRejected(operation.type))
        }
        yield()
    }

    private suspend fun awaitOperation(
        client: FakeChampionBleClient,
        startIndex: Int,
        predicate: (br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattOperation) -> Boolean,
    ): br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattOperation {
        waitForCondition("operation matching predicate from index=$startIndex") {
            client.executedOperations.drop(startIndex).any(predicate)
        }
        return client.executedOperations.drop(startIndex).first(predicate)
    }

    private suspend fun completeOperationsUntil(
        client: FakeChampionBleClient,
        startIndex: Int,
        stopWhen: (br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattOperation) -> Boolean,
    ) {
        var index = startIndex
        while (true) {
            val operation = awaitOperation(client, index) { true }
            client.attachedQueues.getValue(operation.sessionId).complete(operation.sessionId, operation.operationId)
            yield()
            if (stopWhen(operation)) return
            index = client.executedOperations.indexOf(operation) + 1
        }
    }

    private fun buildDomainDevice(transportKey: String): Device =
        (Device.create(
            id = DeviceId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            platformIdentifier = transportKey,
            name = "Champion C001",
            associatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        ) as br.com.mwconsulting.championcompanion.core.model.DomainResult.Success).value

    private suspend fun InMemoryForegroundBleOperationGate.tryAcquireRequestOwnerAfterRelease(): ForegroundBleOperationOwner {
        val result = tryAcquire(
            br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationRequest(
                owner = ForegroundBleOperationOwner.MANUAL_HEART_RATE,
                operationId = "post-release",
                requestedAt = Instant.parse("2026-01-01T00:00:20Z"),
            ),
        )
        check(result is br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationAcquireResult.Acquired)
        return result.permit.owner
    }

    private suspend fun waitForCondition(
        label: String,
        predicate: () -> Boolean,
    ) {
        runCatching {
            awaitCondition(predicate = predicate)
        }.getOrElse { error ->
            throw IllegalStateException("timeout waiting for $label", error)
        }
    }
}
