package br.com.mwconsulting.championcompanion.feature.exercises

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCapabilityResolver
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.awaitCondition
import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeChampionBleClient
import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.FakeChampionBleScanner
import br.com.mwconsulting.championcompanion.core.bluetooth.sessionCapabilities
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationOwner
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationRequest
import br.com.mwconsulting.championcompanion.core.model.InMemoryForegroundBleOperationGate
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImport
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportErrorCategory
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportLookup
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportMetadata
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import br.com.mwconsulting.championcompanion.feature.device.DeviceConnectionBootstrap
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChampionWatchWorkoutImportCoordinatorTest {
    private lateinit var dispatcher: kotlinx.coroutines.ExecutorCoroutineDispatcher

    @Test
    fun `gate blocks concurrent owners`() = runExercisesTest {
        val gate = InMemoryForegroundBleOperationGate()
        gate.tryAcquire(
            ForegroundBleOperationRequest(
                owner = ForegroundBleOperationOwner.MANUAL_HEART_RATE,
                operationId = "manual",
                requestedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        val coordinator = buildCoordinator(
            manager = buildManager(FakeChampionBleScanner(), FakeChampionBleClient()),
            gate = gate,
            repository = FakeWorkoutActivityRepository(),
        )

        val error = coordinator.startImport(
            WatchWorkoutImportRequest(
                importAttemptId = "workout-1",
                associatedDevice = buildDomainDevice("assoc-1"),
            ),
        )

        assertEquals(WatchWorkoutImportErrorCategory.BLE_OPERATION_IN_PROGRESS, error?.category)
    }

    @Test
    fun `connection failure releases gate and exposes failed state`() = runExercisesTest {
        val client = FakeChampionBleClient().apply {
            setOpenResult(ChampionBleResult.Failure(ChampionBleError.DeviceNotFound))
        }
        val gate = InMemoryForegroundBleOperationGate()
        val coordinator = buildCoordinator(
            manager = buildManager(FakeChampionBleScanner(), client),
            gate = gate,
            repository = FakeWorkoutActivityRepository(),
        )

        val error = coordinator.startImport(
            WatchWorkoutImportRequest(
                importAttemptId = "workout-1",
                associatedDevice = buildDomainDevice("assoc-1"),
            ),
        )

        assertNull(error)
        waitForCondition("failed state") { coordinator.state.value is WatchWorkoutImportSurfaceState.Failed }
        val failed = coordinator.state.value as WatchWorkoutImportSurfaceState.Failed
        assertEquals(true, failed.session.error != null)
        assertNull(gate.observeActiveOperation().first())
    }

    private fun runExercisesTest(block: suspend CoroutineScope.() -> Unit) {
        dispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
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
        repository: FakeWorkoutActivityRepository,
    ): ChampionWatchWorkoutImportCoordinator = ChampionWatchWorkoutImportCoordinator(
        connectionManager = manager,
        connectionBootstrap = DeviceConnectionBootstrap(manager),
        foregroundBleOperationGate = gate,
        workoutActivityRepository = repository,
        bleDispatcher = dispatcher,
        civilNow = { Instant.parse("2026-01-01T00:00:10Z") },
    )

    private fun buildDomainDevice(transportKey: String): Device =
        (Device.create(
            id = DeviceId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            platformIdentifier = transportKey,
            name = "Champion C001",
            associatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        ) as br.com.mwconsulting.championcompanion.core.model.DomainResult.Success).value

    private suspend fun waitForCondition(label: String, predicate: () -> Boolean) {
        runCatching { awaitCondition(predicate = predicate) }
            .getOrElse { error -> throw IllegalStateException("timeout waiting for $label", error) }
    }

    private class FakeWorkoutActivityRepository : WorkoutActivityRepository {
        override suspend fun persistImported(
            activity: WorkoutActivity,
            importRecord: WatchWorkoutImport,
        ): RepositoryResult<Unit> = RepositoryResult.Success(Unit)

        override fun observePendingReview(): Flow<List<WorkoutActivity>> = flowOf(emptyList())

        override suspend fun findWatchWorkoutImport(
            deviceId: DeviceId,
            crc: Int,
        ): RepositoryResult<WatchWorkoutImportLookup?> = RepositoryResult.Success(null)

        override fun observeWatchWorkoutImportMetadata(deviceId: DeviceId): Flow<WatchWorkoutImportMetadata?> = flowOf(null)

        override suspend fun updateWatchWorkoutImportMetadata(metadata: WatchWorkoutImportMetadata): RepositoryResult<Unit> =
            RepositoryResult.Success(Unit)

        override suspend fun review(
            activityId: WorkoutActivityId,
            type: br.com.mwconsulting.championcompanion.core.model.WorkoutType,
            reviewedAt: Instant,
        ): RepositoryResult<WorkoutActivity> = RepositoryResult.UnexpectedState

        override suspend fun discard(activityId: WorkoutActivityId): RepositoryResult<Unit> = RepositoryResult.UnexpectedState
    }
}
