package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.model.AppPreferences
import br.com.mwconsulting.championcompanion.core.model.AppPreferencesRepository
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportMetadata
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportStatus
import br.com.mwconsulting.championcompanion.core.model.DailySummary
import br.com.mwconsulting.championcompanion.core.model.DailySummaryRepository
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.DomainResult
import br.com.mwconsulting.championcompanion.core.model.HeartRateChartSeries
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryPeriod
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryQuery
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistorySnapshot
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementId
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementSessionId
import br.com.mwconsulting.championcompanion.core.model.HeartRateRepository
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsRepository
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsSnapshot
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.SaveD1HeartRateMeasurementsInput
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthViewModelD1IntegrationTest {
    @Test
    fun `load does not auto start d1 import`() = runBlocking {
        val fixture = fixture(associatedDevice = device())

        fixture.viewModel.onIntent(HealthIntent.Load)
        fixture.awaitSettled()

        assertTrue(fixture.d1Coordinator.startRequests.isEmpty())
        fixture.close()
    }

    @Test
    fun `select history period updates snapshot without starting ble`() = runBlocking {
        val fixture = fixture(associatedDevice = device())

        fixture.viewModel.onIntent(HealthIntent.SelectHistoryPeriod(HeartRateHistoryPeriod.LAST_7_DAYS))
        fixture.awaitSettled()

        assertEquals(HeartRateHistoryPeriod.LAST_7_DAYS, fixture.viewModel.state.value.selectedHistoryPeriod)
        assertEquals(HeartRateHistoryPeriod.LAST_7_DAYS, fixture.viewModel.state.value.history.period)
        assertEquals(listOf(HeartRateHistoryPeriod.TODAY, HeartRateHistoryPeriod.LAST_7_DAYS), fixture.heartRateRepository.observedPeriods)
        assertTrue(fixture.d1Coordinator.startRequests.isEmpty())
        fixture.close()
    }

    @Test
    fun `background cancels d1 import`() = runBlocking {
        val fixture = fixture(associatedDevice = device())

        fixture.viewModel.onIntent(HealthIntent.StartD1Import)
        fixture.awaitSettled()
        fixture.viewModel.onIntent(HealthIntent.AppMovedToBackground)
        fixture.awaitSettled()

        assertEquals(listOf(D1HeartRateImportCancellationReason.BACKGROUND), fixture.d1Coordinator.cancelReasons)
        assertTrue(fixture.viewModel.state.value.d1ImportState is D1HeartRateImportSurfaceState.Cancelled)
        fixture.close()
    }

    @Test
    fun `retry d1 import creates new attempt`() = runBlocking {
        val fixture = fixture(associatedDevice = device())

        fixture.viewModel.onIntent(HealthIntent.StartD1Import)
        fixture.awaitSettled()
        val firstAttemptId = fixture.d1Coordinator.startRequests.single().importAttemptId

        fixture.d1Coordinator.emitFailed(firstAttemptId)
        fixture.awaitSettled()
        fixture.viewModel.onIntent(HealthIntent.RetryD1Import)
        fixture.awaitSettled()

        assertEquals(2, fixture.d1Coordinator.startRequests.size)
        assertEquals(1, fixture.d1Coordinator.resetCount)
        assertNotEquals(firstAttemptId, fixture.d1Coordinator.startRequests.last().importAttemptId)
        fixture.close()
    }

    @Test
    fun `manual and d1 states stay independent`() = runBlocking {
        val fixture = fixture(associatedDevice = device())

        fixture.viewModel.onIntent(HealthIntent.StartMeasurement)
        fixture.awaitSettled()
        val d1AttemptId = "0190029f-2f80-7000-8000-000000000099"
        fixture.d1Coordinator.emitFailed(d1AttemptId)
        fixture.awaitSettled()

        assertTrue(fixture.viewModel.state.value.surfaceState is HealthSurfaceState.Preparing)
        assertTrue(fixture.viewModel.state.value.d1ImportState is D1HeartRateImportSurfaceState.Failed)
        fixture.close()
    }

    private fun fixture(associatedDevice: Device? = null): Fixture {
        val deviceRepository = FakeDeviceRepository(associatedDevice)
        val heartRateRepository = FakeHeartRateRepository(associatedDevice?.id)
        val dailySummaryRepository = FakeDailySummaryRepository()
        val personalMetricsRepository = FakePersonalMetricsRepository()
        val appPreferencesRepository = FakeAppPreferencesRepository()
        val manualCoordinator = FakeManualHeartRateCoordinator()
        val d1Coordinator = FakeD1HeartRateSyncCoordinator()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        return Fixture(
            viewModel = HealthViewModel(
                heartRateRepository = heartRateRepository,
                deviceRepository = deviceRepository,
                dailySummaryRepository = dailySummaryRepository,
                personalMetricsRepository = personalMetricsRepository,
                appPreferencesRepository = appPreferencesRepository,
                healthSyncRepository = FakeHealthSyncRepository(),
                healthConnectSyncCoordinator = NoOpHealthConnectSyncCoordinator,
                manualHeartRateCoordinator = manualCoordinator,
                d1HeartRateSyncCoordinator = d1Coordinator,
                scope = scope,
                ids = UuidV7Generator(),
                parserVersion = ParserVersion.create("manual-heart-rate-v1").valueOrNull()!!,
            ),
            heartRateRepository = heartRateRepository,
            manualCoordinator = manualCoordinator,
            d1Coordinator = d1Coordinator,
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
        val manualCoordinator: FakeManualHeartRateCoordinator,
        val d1Coordinator: FakeD1HeartRateSyncCoordinator,
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

    private class FakeHeartRateRepository(
        deviceId: DeviceId?,
    ) : HeartRateRepository {
        private val latest = MutableStateFlow<HeartRateMeasurement?>(null)
        private val recent = MutableStateFlow<List<HeartRateMeasurement>>(emptyList())
        private val metadata = MutableStateFlow(
            deviceId?.let {
                D1HeartRateImportMetadata(
                    deviceId = it,
                    lastAttemptAt = Instant.parse("2026-07-23T10:00:00Z"),
                    lastSuccessAt = Instant.parse("2026-07-23T10:00:10Z"),
                    lastResult = D1HeartRateImportStatus.COMPLETED,
                    updatedAt = Instant.parse("2026-07-23T10:00:10Z"),
                )
            },
        )

        val observedPeriods = mutableListOf<HeartRateHistoryPeriod>()

        override suspend fun import(measurements: List<HeartRateMeasurement>): RepositoryResult<Unit> = RepositoryResult.Success(Unit)

        override suspend fun findPendingSync(): RepositoryResult<List<HeartRateMeasurement>> = RepositoryResult.Success(emptyList())

        override suspend fun saveManualMeasurement(input: SaveManualHeartRateMeasurementInput): RepositoryResult<HeartRateMeasurement> =
            RepositoryResult.UnexpectedState

        override fun observeLatestValidMeasurement(): Flow<HeartRateMeasurement?> = latest.asStateFlow()

        override fun observeRecentManualMeasurements(limit: Int): Flow<List<HeartRateMeasurement>> =
            recent.asStateFlow().map { it.take(limit) }

        override suspend fun importD1Measurements(
            input: SaveD1HeartRateMeasurementsInput,
        ) = RepositoryResult.UnexpectedState

        override fun observeHistory(query: HeartRateHistoryQuery): Flow<HeartRateHistorySnapshot> {
            observedPeriods += query.period
            return MutableStateFlow(
                HeartRateHistorySnapshot(
                    period = query.period,
                    chart = HeartRateChartSeries(),
                ),
            )
        }

        override fun observeD1ImportMetadata(deviceId: DeviceId): Flow<D1HeartRateImportMetadata?> = metadata.asStateFlow()
    }

    private class FakeManualHeartRateCoordinator : ManualHeartRateCoordinator {
        private val mutableState = MutableStateFlow<HealthSurfaceState>(HealthSurfaceState.Idle)

        override val state: StateFlow<HealthSurfaceState> = mutableState.asStateFlow()

        override suspend fun startMeasurement(request: ManualHeartRateMeasurementRequest): HealthMeasurementError? {
            mutableState.value = HealthSurfaceState.Preparing(
                session = ManualHeartRateSessionState(
                    manualMeasurementSessionId = request.manualMeasurementSessionId,
                ),
            )
            return null
        }

        override suspend fun cancelMeasurement(reason: ManualHeartRateCancellationReason) {
            mutableState.value = HealthSurfaceState.Cancelled(
                session = ManualHeartRateSessionState(
                    manualMeasurementSessionId = "manual-cancelled",
                    error = HealthMeasurementError(
                        category = HealthMeasurementErrorCategory.CANCELLED,
                        message = "cancelled",
                        isRetryable = true,
                    ),
                ),
            )
        }

        override fun reset() {
            mutableState.value = HealthSurfaceState.Idle
        }
    }

    private class FakeD1HeartRateSyncCoordinator : D1HeartRateSyncCoordinator {
        private val mutableState = MutableStateFlow<D1HeartRateImportSurfaceState>(D1HeartRateImportSurfaceState.Idle)

        val startRequests = mutableListOf<D1HeartRateSyncRequest>()
        val cancelReasons = mutableListOf<D1HeartRateImportCancellationReason>()
        var resetCount: Int = 0

        override val state: StateFlow<D1HeartRateImportSurfaceState> = mutableState.asStateFlow()

        override suspend fun startImport(request: D1HeartRateSyncRequest): HealthD1ImportError? {
            startRequests += request
            mutableState.value = D1HeartRateImportSurfaceState.Preparing(
                session = D1HeartRateImportSessionState(importAttemptId = request.importAttemptId),
            )
            return null
        }

        override suspend fun cancelImport(reason: D1HeartRateImportCancellationReason) {
            cancelReasons += reason
            val current = (mutableState.value as? D1HeartRateImportSurfaceState.Preparing)?.session
                ?: (mutableState.value as? D1HeartRateImportSurfaceState.Connecting)?.session
                ?: (mutableState.value as? D1HeartRateImportSurfaceState.Reading)?.session
                ?: D1HeartRateImportSessionState(importAttemptId = "cancelled")
            mutableState.value = D1HeartRateImportSurfaceState.Cancelled(current)
        }

        override fun reset() {
            resetCount += 1
            mutableState.value = D1HeartRateImportSurfaceState.Idle
        }

        fun emitFailed(importAttemptId: String) {
            mutableState.value = D1HeartRateImportSurfaceState.Failed(
                D1HeartRateImportSessionState(
                    importAttemptId = importAttemptId,
                    error = HealthD1ImportError(
                        category = HealthD1ImportErrorCategory.INTERNAL,
                        message = "failed",
                        isRetryable = true,
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
}
