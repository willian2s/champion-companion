package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCapabilityResolver
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattSessionEvent
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacket
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacketDirection
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionTransportSession
import br.com.mwconsulting.championcompanion.core.bluetooth.awaitCondition
import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeChampionBleClient
import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeChampionBleScanner
import br.com.mwconsulting.championcompanion.core.bluetooth.sessionCapabilities
import br.com.mwconsulting.championcompanion.core.bluetooth.testDevice
import br.com.mwconsulting.championcompanion.core.bluetooth.testSession
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportMetadata
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportPersistenceResult
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationOwner
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryQuery
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistorySnapshot
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.HeartRateRepository
import br.com.mwconsulting.championcompanion.core.model.InMemoryForegroundBleOperationGate
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.SaveD1HeartRateMeasurementsInput
import br.com.mwconsulting.championcompanion.feature.device.DeviceConnectionBootstrap
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionD1HeartRateSyncCoordinatorTest {
    private lateinit var dispatcher: kotlinx.coroutines.ExecutorCoroutineDispatcher

    @Test
    fun `gate blocks concurrent manual and daily owners`() = runHealthTest {
        val coordinator = buildCoordinator(buildManager(FakeChampionBleScanner(), FakeChampionBleClient()), InMemoryForegroundBleOperationGate(), FakeHeartRateRepository())
        val gate = InMemoryForegroundBleOperationGate()
        gate.tryAcquire(
            br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationRequest(
                owner = ForegroundBleOperationOwner.MANUAL_HEART_RATE,
                operationId = "manual",
                requestedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        val blockedCoordinator = buildCoordinator(buildManager(FakeChampionBleScanner(), FakeChampionBleClient()), gate, FakeHeartRateRepository())

        val error = blockedCoordinator.startImport(
            D1HeartRateSyncRequest(
                importAttemptId = "d1-1",
                associatedDevice = buildDomainDevice("assoc-1"),
                baseLocalDate = LocalDate.of(2026, 1, 1),
                zoneId = ZoneId.of("UTC"),
            ),
        )

        assertEquals(HealthD1ImportErrorCategory.BLE_OPERATION_IN_PROGRESS, error?.category)
    }

    @Test
    fun `cancel releases gate and late callback is ignored`() = runHealthTest {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient()
        val session = testSession().copy(transportKey = "assoc-1")
        client.setOpenResult(ChampionBleResult.Success(session))
        val manager = buildManager(scanner, client)
        val gate = InMemoryForegroundBleOperationGate()
        val repository = FakeHeartRateRepository()
        val coordinator = buildCoordinator(manager, gate, repository)
        connectToTransportReady(manager, client, session, testDevice(transportKey = "assoc-1"))

        startImport(coordinator, client, buildDomainDevice("assoc-1"))
        coordinator.cancelImport(D1HeartRateImportCancellationReason.USER_REQUESTED)
        waitForCondition("cancelled state") { coordinator.state.value is D1HeartRateImportSurfaceState.Cancelled }

        client.emit(d1Event(session, packetIndex = 1, totalPackets = 1, minute = 10))
        yield()

        assertNull(gate.observeActiveOperation().first())
        assertTrue(repository.savedInputs.isEmpty())
    }

    @Test
    fun `timeout releases gate and returns failed when no valid data`() = runHealthTest {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient()
        val session = testSession().copy(transportKey = "assoc-1")
        client.setOpenResult(ChampionBleResult.Success(session))
        val manager = buildManager(scanner, client)
        val gate = InMemoryForegroundBleOperationGate()
        val repository = FakeHeartRateRepository()
        val coordinator = buildCoordinator(manager, gate, repository, historyTimeout = 1.seconds)
        connectToTransportReady(manager, client, session, testDevice(transportKey = "assoc-1"))

        startImport(coordinator, client, buildDomainDevice("assoc-1"))
        waitForCondition("failed timeout state") { coordinator.state.value is D1HeartRateImportSurfaceState.Failed }

        val failed = coordinator.state.value as D1HeartRateImportSurfaceState.Failed
        assertEquals(HealthD1ImportErrorCategory.TIMEOUT_EMPTY, failed.session.error?.category)
        assertNull(gate.observeActiveOperation().first())
    }

    @Test
    fun `reuses compatible session and persists partial timeout once`() = runHealthTest {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient()
        val session = testSession().copy(transportKey = "assoc-1")
        client.setOpenResult(ChampionBleResult.Success(session))
        val manager = buildManager(scanner, client)
        val gate = InMemoryForegroundBleOperationGate()
        val repository = FakeHeartRateRepository()
        val coordinator = buildCoordinator(manager, gate, repository, historyTimeout = 1.seconds)
        connectToTransportReady(manager, client, session, testDevice(transportKey = "assoc-1"))
        val operationsBefore = client.executedOperations.size

        startImport(coordinator, client, buildDomainDevice("assoc-1"))
        client.emit(d1Event(session, packetIndex = 1, totalPackets = 3, minute = 10))
        waitForCondition("partial state") { coordinator.state.value is D1HeartRateImportSurfaceState.Partial }

        assertTrue(client.executedOperations.size >= operationsBefore + 2)
        assertEquals(1, client.attachedQueues.size)
        assertEquals(2, repository.savedInputs.size)
        assertEquals(1, repository.savedInputs.first().measurements.size)
        assertEquals(ForegroundBleOperationOwner.WATCH_D1_HISTORY, gate.tryAcquireRequestOwnerAfterRelease())
    }

    @Test
    fun `handshake happens before D1 command`() = runHealthTest {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient()
        val session = testSession().copy(transportKey = "assoc-1")
        client.setOpenResult(ChampionBleResult.Success(session))
        val manager = buildManager(scanner, client)
        val coordinator = buildCoordinator(manager, InMemoryForegroundBleOperationGate(), FakeHeartRateRepository())
        connectToTransportReady(manager, client, session, testDevice(transportKey = "assoc-1"))

        val startIndex = startImport(coordinator, client, buildDomainDevice("assoc-1"))
        val commands = client.executedOperations.drop(startIndex).mapNotNull { operation ->
            operation.payloadCopy()?.firstOrNull()?.toInt()?.and(0xFF)
        }

        assertTrue(commands.contains(0xA1))
        assertTrue(commands.contains(0xD1))
        assertTrue(commands.indexOf(0xA1) < commands.indexOf(0xD1))
    }

    private fun runHealthTest(block: suspend CoroutineScope.() -> Unit) {
        dispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()
        try {
            runBlocking { coroutineScope(block) }
        } finally {
            dispatcher.close()
        }
    }

    private fun CoroutineScope.buildManager(
        scanner: FakeChampionBleScanner,
        client: FakeChampionBleClient,
    ): ChampionConnectionManager = ChampionConnectionManager(
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
        parentScope = CoroutineScope(coroutineContext + Job()),
        timeouts = br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleTimeouts(
            connect = 5.seconds,
            discovery = 5.seconds,
        ),
    )

    private fun buildCoordinator(
        manager: ChampionConnectionManager,
        gate: InMemoryForegroundBleOperationGate,
        repository: FakeHeartRateRepository,
        historyTimeout: kotlin.time.Duration = 2.seconds,
    ): ChampionD1HeartRateSyncCoordinator = ChampionD1HeartRateSyncCoordinator(
        connectionManager = manager,
        connectionBootstrap = DeviceConnectionBootstrap(manager),
        foregroundBleOperationGate = gate,
        heartRateRepository = repository,
        bleDispatcher = dispatcher,
        historyTimeout = historyTimeout,
        parserVersion = (ParserVersion.create("d1-v1") as br.com.mwconsulting.championcompanion.core.model.DomainResult.Success).value,
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
                completeOperation(client, nextOperationIndex)
                nextOperationIndex += 1
            }
        }
        val result = connect.await()
        check(result is ChampionBleResult.Success<ChampionTransportSession>)
        waitForCondition("manager transport ready") { manager.state.value is ChampionBleState.TransportReady }
    }

    private suspend fun startImport(
        coordinator: ChampionD1HeartRateSyncCoordinator,
        client: FakeChampionBleClient,
        device: Device,
    ): Int {
        val startIndex = client.executedOperations.size
        val error = coordinator.startImport(
            D1HeartRateSyncRequest(
                importAttemptId = "d1-1",
                associatedDevice = device,
                baseLocalDate = LocalDate.of(2026, 1, 1),
                zoneId = ZoneId.of("UTC"),
            ),
        )
        check(error == null)
        waitForCondition("handshake command queued") { client.executedOperations.size > startIndex }
        completeOperation(client, startIndex)
        delay(1500)
        completeOperationsUntil(client, startIndex + 1) { operation ->
            operation.payloadCopy()?.contentEquals(byteArrayOf(0xD1.toByte(), 0x01, 0x00, 0x00)) == true
        }
        yield()
        return startIndex
    }

    private suspend fun completeOperation(client: FakeChampionBleClient, index: Int) {
        waitForCondition("operation index=$index queued") { client.executedOperations.size > index }
        val operation = client.executedOperations[index]
        client.attachedQueues.getValue(operation.sessionId).complete(operation.sessionId, operation.operationId)
        yield()
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

    private fun d1Event(
        session: br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSession,
        packetIndex: Int,
        totalPackets: Int,
        minute: Int,
    ) = ChampionGattSessionEvent.CharacteristicChanged(
        sessionId = session.id,
        occurredAt = Instant.parse("2026-01-01T00:00:12Z"),
        channel = ChampionChannelAlias.F008_NOTIFY,
        packet = ChampionPacket(
            sessionId = session.id,
            channel = ChampionChannelAlias.F008_NOTIFY,
            direction = ChampionPacketDirection.INBOUND,
            observedAt = Instant.parse("2026-01-01T00:00:12Z"),
            commandCode = 0xD1,
            bytes = ByteArray(20).apply {
                this[0] = 0xD1.toByte()
                this[1] = (packetIndex and 0xFF).toByte()
                this[2] = ((packetIndex shr 8) and 0xFF).toByte()
                this[3] = (totalPackets and 0xFF).toByte()
                this[4] = ((totalPackets shr 8) and 0xFF).toByte()
                this[5] = 10
                this[14] = 72
                this[19] = minute.toByte()
            },
        ),
    )

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
                owner = ForegroundBleOperationOwner.WATCH_D1_HISTORY,
                operationId = "post-release",
                requestedAt = Instant.parse("2026-01-01T00:00:20Z"),
            ),
        )
        check(result is br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationAcquireResult.Acquired)
        return result.permit.owner
    }

    private suspend fun waitForCondition(label: String, predicate: () -> Boolean) {
        runCatching { awaitCondition(predicate = predicate) }
            .getOrElse { error -> throw IllegalStateException("timeout waiting for $label", error) }
    }

    private class FakeHeartRateRepository : HeartRateRepository {
        val savedInputs = mutableListOf<SaveD1HeartRateMeasurementsInput>()

        override suspend fun import(measurements: List<HeartRateMeasurement>): RepositoryResult<Unit> = RepositoryResult.UnexpectedState

        override suspend fun findPendingSync(): RepositoryResult<List<HeartRateMeasurement>> = RepositoryResult.UnexpectedState

        override suspend fun importD1Measurements(
            input: SaveD1HeartRateMeasurementsInput,
        ): RepositoryResult<D1HeartRateImportPersistenceResult> {
            savedInputs += input
            return RepositoryResult.Success(
                D1HeartRateImportPersistenceResult(
                    insertedCount = input.measurements.size,
                    duplicateCount = 0,
                    conflictCount = 0,
                    pendingCreatedCount = input.measurements.size,
                    pendingExistingCount = 0,
                ),
            )
        }

        override fun observeHistory(query: HeartRateHistoryQuery): Flow<HeartRateHistorySnapshot> =
            flowOf(HeartRateHistorySnapshot(period = query.period))

        override fun observeD1ImportMetadata(deviceId: DeviceId): Flow<D1HeartRateImportMetadata?> = flowOf(null)
    }
}
