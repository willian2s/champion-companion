package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCapabilityResolver
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionOperationType
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionScanRequest
import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeChampionBleClient
import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeChampionBleScanner
import br.com.mwconsulting.championcompanion.core.bluetooth.sessionCapabilities
import br.com.mwconsulting.championcompanion.core.bluetooth.testDevice
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationOwner
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationRequest
import br.com.mwconsulting.championcompanion.core.model.InMemoryForegroundBleOperationGate
import java.time.Instant
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePocCoordinatorTest {
    private lateinit var dispatcher: kotlinx.coroutines.ExecutorCoroutineDispatcher

    @Test
    fun `scan is rejected until surface is ready`() = runCoordinatorTest {
        val scanner = FakeChampionBleScanner()
        val gate = InMemoryForegroundBleOperationGate()
        val coordinator = buildCoordinator(scanner, FakeChampionBleClient(), gate)

        val result = coordinator.scan(ChampionScanRequest(100.milliseconds))

        assertEquals(
            ChampionBleResult.Failure(ChampionBleError.OperationUnavailable),
            result,
        )
        assertNull(scanner.lastRequest)
        assertNull(gate.observeActiveOperation().first())
    }

    @Test
    fun `scan is blocked while another foreground owner is active`() = runCoordinatorTest {
        val scanner = FakeChampionBleScanner()
        val gate = InMemoryForegroundBleOperationGate()
        gate.tryAcquire(
            ForegroundBleOperationRequest(
                owner = ForegroundBleOperationOwner.APP_WORKOUT_SESSION,
                operationId = "workout-session",
                requestedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        val coordinator = buildCoordinator(scanner, FakeChampionBleClient(), gate)
        coordinator.markReadyToScan()

        val result = coordinator.scan(ChampionScanRequest(100.milliseconds))

        assertEquals(
            ChampionBleResult.Failure(ChampionBleError.OperationUnavailable),
            result,
        )
        assertNull(scanner.lastRequest)
        assertEquals(
            ForegroundBleOperationOwner.APP_WORKOUT_SESSION,
            gate.observeActiveOperation().first()?.owner,
        )
    }

    @Test
    fun `successful scan exposes candidate and releases transient permit`() = runCoordinatorTest {
        val scanner = FakeChampionBleScanner()
        val gate = InMemoryForegroundBleOperationGate()
        val device = testDevice()
        scanner.enqueueResult(ChampionBleResult.Success(listOf(device)))
        val coordinator = buildCoordinator(scanner, FakeChampionBleClient(), gate)
        coordinator.markReadyToScan()

        val result = coordinator.scan(ChampionScanRequest(100.milliseconds))

        assertTrue(result is ChampionBleResult.Success)
        assertEquals(DevicePocState.DeviceFound, coordinator.state.value.pocState)
        assertEquals(device, coordinator.state.value.candidate)
        assertNull(gate.observeActiveOperation().first())
    }

    @Test
    fun `connect failure releases permit and exposes sanitized failure`() = runCoordinatorTest {
        val scanner = FakeChampionBleScanner()
        val client = FakeChampionBleClient().apply {
            setOpenResult(ChampionBleResult.Failure(ChampionBleError.DeviceNotFound))
        }
        val gate = InMemoryForegroundBleOperationGate()
        val coordinator = buildCoordinator(scanner, client, gate)
        coordinator.markReadyToScan()

        val result = coordinator.connect(testDevice())

        assertEquals(
            ChampionBleResult.Failure(ChampionBleError.DeviceNotFound),
            result,
        )
        assertTrue(coordinator.state.value.pocState is DevicePocState.Failed)
        assertEquals(ChampionBleError.DeviceNotFound, coordinator.state.value.lastError)
        assertNull(gate.observeActiveOperation().first())
    }

    @Test
    fun `reset cycle returns idle and releases device owner`() = runCoordinatorTest {
        val scanner = FakeChampionBleScanner()
        val gate = InMemoryForegroundBleOperationGate()
        val coordinator = buildCoordinator(scanner, FakeChampionBleClient(), gate)
        val previousCycle = coordinator.state.value.cycleId

        val result = coordinator.resetCycle()

        assertEquals(ChampionBleResult.Success(Unit), result)
        assertTrue(previousCycle != coordinator.state.value.cycleId)
        assertEquals(DevicePocState.Idle, coordinator.state.value.pocState)
        assertNull(gate.observeActiveOperation().first())
    }

    private fun runCoordinatorTest(block: suspend CoroutineScope.() -> Unit) {
        dispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
        try {
            runBlocking { coroutineScope(block) }
        } finally {
            dispatcher.close()
        }
    }

    private fun CoroutineScope.buildCoordinator(
        scanner: FakeChampionBleScanner,
        client: FakeChampionBleClient,
        gate: InMemoryForegroundBleOperationGate,
    ): DevicePocCoordinator = DevicePocCoordinator(
        connectionManager = ChampionConnectionManager(
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
                scan = 100.milliseconds,
                connect = 100.milliseconds,
                discovery = 100.milliseconds,
            ),
        ),
        foregroundBleOperationGate = gate,
        bleDispatcher = dispatcher,
    )
}
