package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.DomainResult
import br.com.mwconsulting.championcompanion.core.model.AppPreferences
import br.com.mwconsulting.championcompanion.core.model.AppPreferencesRepository
import br.com.mwconsulting.championcompanion.core.model.DailySummary
import br.com.mwconsulting.championcompanion.core.model.DailySummaryRepository
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementId
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementSessionId
import br.com.mwconsulting.championcompanion.core.model.HeartRateRepository
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsRepository
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsSnapshot
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.SaveManualHeartRateMeasurementInput
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import br.com.mwconsulting.championcompanion.core.model.valueOrNull
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthViewModelTest {
    @Test
    fun `load does not start measurement`() = runBlocking {
        val fixture = fixture()

        fixture.viewModel.onIntent(HealthIntent.Load)
        fixture.awaitSettled()

        assertTrue(fixture.coordinator.startRequests.isEmpty())
        fixture.close()
    }

    @Test
    fun `completed result persists and updates completed surface`() = runBlocking {
        val fixture = fixture(associatedDevice = device())

        fixture.viewModel.onIntent(HealthIntent.StartMeasurement)
        fixture.awaitSettled()
        val sessionId = fixture.coordinator.startRequests.single().manualMeasurementSessionId

        fixture.coordinator.emitCompleted(sessionId, bpm = 78, measuredAt = Instant.parse("2026-07-22T10:00:00Z"))
        fixture.awaitSettled()

        assertEquals(1, fixture.heartRateRepository.savedInputs.size)
        assertEquals(78, fixture.viewModel.state.value.latestMeasurement?.bpm)
        assertEquals(1, fixture.viewModel.state.value.recentMeasurements.size)
        assertTrue(fixture.viewModel.state.value.surfaceState is HealthSurfaceState.Completed)
        fixture.close()
    }

    @Test
    fun `cancel and background do not persist partial measurement`() = runBlocking {
        val fixture = fixture(associatedDevice = device())

        fixture.viewModel.onIntent(HealthIntent.StartMeasurement)
        fixture.awaitSettled()
        fixture.viewModel.onIntent(HealthIntent.AppMovedToBackground)
        fixture.awaitSettled()

        assertEquals(listOf(ManualHeartRateCancellationReason.BACKGROUND), fixture.coordinator.cancelReasons)
        assertTrue(fixture.viewModel.state.value.surfaceState is HealthSurfaceState.Cancelled)
        assertTrue(fixture.heartRateRepository.savedInputs.isEmpty())
        fixture.close()
    }

    @Test
    fun `gate conflict becomes safe failed surface`() = runBlocking {
        val fixture = fixture(
            associatedDevice = device(),
            startError = HealthMeasurementError(
                category = HealthMeasurementErrorCategory.BLE_OPERATION_IN_PROGRESS,
                message = "Outra operação Bluetooth em primeiro plano já está em andamento.",
                isRetryable = true,
            ),
        )

        fixture.viewModel.onIntent(HealthIntent.StartMeasurement)
        fixture.awaitSettled()

        val failed = fixture.viewModel.state.value.surfaceState as HealthSurfaceState.Failed
        assertEquals(HealthMeasurementErrorCategory.BLE_OPERATION_IN_PROGRESS, failed.session.error?.category)
        assertFalse(fixture.coordinator.startRequests.isEmpty())
        assertTrue(fixture.heartRateRepository.savedInputs.isEmpty())
        fixture.close()
    }

    private fun fixture(
        associatedDevice: Device? = null,
        startError: HealthMeasurementError? = null,
    ): Fixture {
        val deviceRepository = FakeDeviceRepository(associatedDevice)
        val heartRateRepository = FakeHeartRateRepository()
        val dailySummaryRepository = FakeDailySummaryRepository()
        val personalMetricsRepository = FakePersonalMetricsRepository()
        val appPreferencesRepository = FakeAppPreferencesRepository()
        val coordinator = FakeManualHeartRateCoordinator(startError)
        val d1Coordinator = FakeD1HeartRateSyncCoordinator()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        return Fixture(
            viewModel = HealthViewModel(
                heartRateRepository = heartRateRepository,
                deviceRepository = deviceRepository,
                dailySummaryRepository = dailySummaryRepository,
                personalMetricsRepository = personalMetricsRepository,
                appPreferencesRepository = appPreferencesRepository,
                manualHeartRateCoordinator = coordinator,
                d1HeartRateSyncCoordinator = d1Coordinator,
                scope = scope,
                ids = UuidV7Generator(),
                parserVersion = ParserVersion.create("manual-heart-rate-v1").valueOrNull()!!,
            ),
            heartRateRepository = heartRateRepository,
            coordinator = coordinator,
            scope = scope,
        )
    }

    private fun device(): Device = Device.create(
        id = DeviceId(uuid(1)),
        platformIdentifier = "platform-1",
        name = "Champion C001",
        associatedAt = Instant.parse("2026-07-22T09:00:00Z"),
    ).valueOrNull()!!

    private fun uuid(last: Int): UUID =
        UUID.fromString("0190029f-2f80-7000-8000-0000000000${last.toString().padStart(2, '0')}")

    private class Fixture(
        val viewModel: HealthViewModel,
        val heartRateRepository: FakeHeartRateRepository,
        val coordinator: FakeManualHeartRateCoordinator,
        private val scope: CoroutineScope,
    ) {
        suspend fun awaitSettled() {
            repeat(5) { delay(1) }
        }

        fun close() {
            scope.cancel()
        }
    }

    private class FakeDeviceRepository(device: Device?) : DeviceRepository {
        private val associated = MutableStateFlow(device)

        override fun observeAssociatedDevice(): Flow<Device?> = associated.asStateFlow()

        override suspend fun associateDevice(device: Device): RepositoryResult<Device> = RepositoryResult.Success(device)

        override suspend fun associateDeviceAndUpdateLastSuccessfulConnection(
            device: Device,
            connectedAt: Instant,
        ): RepositoryResult<Device> = RepositoryResult.Success(device)

        override suspend fun updateLastSuccessfulConnection(deviceId: DeviceId, connectedAt: Instant): RepositoryResult<Device> =
            RepositoryResult.NotFound

        override suspend fun updateKnownMetadata(deviceId: DeviceId, name: String, firmware: String?): RepositoryResult<Device> =
            RepositoryResult.NotFound

        override suspend fun forgetAssociatedDevice(): RepositoryResult<Unit> = RepositoryResult.Success(Unit)

        override suspend fun findByPlatformIdentifier(platformIdentifier: String): RepositoryResult<Device> = RepositoryResult.NotFound
    }

    private class FakeHeartRateRepository : HeartRateRepository {
        private val latest = MutableStateFlow<HeartRateMeasurement?>(null)
        private val recent = MutableStateFlow<List<HeartRateMeasurement>>(emptyList())

        val savedInputs = mutableListOf<SaveManualHeartRateMeasurementInput>()

        override suspend fun import(measurements: List<HeartRateMeasurement>): RepositoryResult<Unit> = RepositoryResult.Success(Unit)

        override suspend fun findPendingSync(): RepositoryResult<List<HeartRateMeasurement>> = RepositoryResult.Success(emptyList())

        override suspend fun saveManualMeasurement(input: SaveManualHeartRateMeasurementInput): RepositoryResult<HeartRateMeasurement> {
            savedInputs += input
            val measurement = HeartRateMeasurement.createManualMeasurement(
                id = input.measurementId,
                sessionId = input.sessionId,
                deviceId = input.deviceId,
                bpm = input.bpm,
                measuredAt = input.measuredAt,
                parserVersion = input.parserVersion,
            ).valueOrNull() ?: return RepositoryResult.UnexpectedState
            latest.value = measurement
            recent.value = listOf(measurement) + recent.value.filterNot {
                it.deduplicationKey == measurement.deduplicationKey
            }
            return RepositoryResult.Success(measurement)
        }

        override fun observeLatestValidMeasurement(): Flow<HeartRateMeasurement?> = latest.asStateFlow()

        override fun observeRecentManualMeasurements(limit: Int): Flow<List<HeartRateMeasurement>> =
            recent.asStateFlow().map { it.take(limit) }
    }

    private class FakeManualHeartRateCoordinator(
        private val startError: HealthMeasurementError?,
    ) : ManualHeartRateCoordinator {
        private val mutableState = MutableStateFlow<HealthSurfaceState>(HealthSurfaceState.Idle)
        private var activeSessionId: String? = null

        val startRequests = mutableListOf<ManualHeartRateMeasurementRequest>()
        val cancelReasons = mutableListOf<ManualHeartRateCancellationReason>()

        override val state: StateFlow<HealthSurfaceState> = mutableState.asStateFlow()

        override suspend fun startMeasurement(request: ManualHeartRateMeasurementRequest): HealthMeasurementError? {
            startRequests += request
            activeSessionId = request.manualMeasurementSessionId
            if (startError == null) {
                mutableState.value = HealthSurfaceState.Preparing(
                    session = ManualHeartRateSessionState(
                        manualMeasurementSessionId = request.manualMeasurementSessionId,
                    ),
                )
            }
            return startError
        }

        override suspend fun cancelMeasurement(reason: ManualHeartRateCancellationReason) {
            cancelReasons += reason
            val sessionId = activeSessionId ?: return
            mutableState.value = HealthSurfaceState.Cancelled(
                session = ManualHeartRateSessionState(
                    manualMeasurementSessionId = sessionId,
                    error = HealthMeasurementError(
                        category = HealthMeasurementErrorCategory.CANCELLED,
                        message = "A medição foi cancelada antes da conclusão.",
                        isRetryable = true,
                    ),
                ),
            )
        }

        override fun reset() {
            activeSessionId = null
            mutableState.value = HealthSurfaceState.Idle
        }

        fun emitCompleted(sessionId: String, bpm: Int, measuredAt: Instant) {
            activeSessionId = sessionId
            mutableState.value = HealthSurfaceState.Completed(
                session = ManualHeartRateSessionState(
                    manualMeasurementSessionId = sessionId,
                    startedAt = measuredAt.minusSeconds(60),
                    finalResult = ManualHeartRateResult(
                        bpm = bpm,
                        source = ManualHeartRateFinalSource.D0_FINAL,
                        measuredAt = measuredAt,
                    ),
                ),
            )
        }
    }

    private class FakeDailySummaryRepository : DailySummaryRepository {
        override fun observeSummaries(from: LocalDate, to: LocalDate): Flow<List<DailySummary>> = MutableStateFlow(emptyList())

        override suspend fun save(summary: DailySummary): RepositoryResult<Unit> = RepositoryResult.Success(Unit)
    }

    private class FakePersonalMetricsRepository : PersonalMetricsRepository {
        override fun observeSnapshot(): Flow<PersonalMetricsSnapshot> = MutableStateFlow(PersonalMetricsSnapshot())
    }

    private class FakeAppPreferencesRepository : AppPreferencesRepository {
        override fun observe(): Flow<AppPreferences> = MutableStateFlow(AppPreferences())

        override fun observeDailyStepsGoal(): Flow<Int> = MutableStateFlow(8_000)

        override suspend fun update(preferences: AppPreferences): RepositoryResult<Unit> = RepositoryResult.Success(Unit)

        override suspend fun updateDailyStepsGoal(goal: Int): RepositoryResult<Unit> = RepositoryResult.Success(Unit)
    }

    private class FakeD1HeartRateSyncCoordinator : D1HeartRateSyncCoordinator {
        private val mutableState = MutableStateFlow<D1HeartRateImportSurfaceState>(D1HeartRateImportSurfaceState.Idle)

        override val state: StateFlow<D1HeartRateImportSurfaceState> = mutableState.asStateFlow()

        override suspend fun startImport(request: D1HeartRateSyncRequest): HealthD1ImportError? = null

        override suspend fun cancelImport(reason: D1HeartRateImportCancellationReason) {
            Unit
        }

        override fun reset() {
            mutableState.value = D1HeartRateImportSurfaceState.Idle
        }
    }
}
