package br.com.mwconsulting.championcompanion.feature.health

import androidx.lifecycle.ViewModel
import br.com.mwconsulting.championcompanion.core.model.AppPreferencesRepository
import br.com.mwconsulting.championcompanion.core.model.DailySummary
import br.com.mwconsulting.championcompanion.core.model.DailySummaryRepository
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportMetadata
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.DomainResult
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
import br.com.mwconsulting.championcompanion.core.model.SaveManualHeartRateMeasurementInput
import br.com.mwconsulting.championcompanion.core.model.UuidV7
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import br.com.mwconsulting.championcompanion.core.model.valueOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private const val RECENT_MANUAL_MEASUREMENTS_LIMIT = 10
private const val MANUAL_HEART_RATE_PARSER_VERSION = "manual-heart-rate-v1"

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModel internal constructor(
    private val heartRateRepository: HeartRateRepository,
    private val deviceRepository: DeviceRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val personalMetricsRepository: PersonalMetricsRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val manualHeartRateCoordinator: ManualHeartRateCoordinator,
    private val d1HeartRateSyncCoordinator: D1HeartRateSyncCoordinator,
    private val scope: CoroutineScope,
    private val ids: UuidV7Generator,
    private val parserVersion: ParserVersion,
) : ViewModel(), HealthFeatureContract {
    private val mutableState = MutableStateFlow(HealthUiState())
    private val surfaceState = MutableStateFlow<HealthSurfaceState>(HealthSurfaceState.Idle)
    private val d1ImportState = MutableStateFlow<D1HeartRateImportSurfaceState>(D1HeartRateImportSurfaceState.Idle)
    private val associatedDevice = MutableStateFlow<Device?>(null)
    private val latestMeasurement = MutableStateFlow<HeartRateMeasurement?>(null)
    private val recentMeasurements = MutableStateFlow<List<HeartRateMeasurement>>(emptyList())
    private val selectedHistoryPeriod = MutableStateFlow(HeartRateHistoryPeriod.TODAY)
    private val history = MutableStateFlow(HeartRateHistorySnapshot(period = HeartRateHistoryPeriod.TODAY))
    private val latestD1ImportMetadata = MutableStateFlow<D1HeartRateImportMetadata?>(null)
    private val personalMetrics = MutableStateFlow(PersonalMetricsSnapshot())
    private val observedDate = MutableStateFlow(LocalDate.now())
    private val todaySummary = MutableStateFlow<DailySummary?>(null)
    private val dailyStepsGoal = MutableStateFlow(br.com.mwconsulting.championcompanion.core.model.DEFAULT_DAILY_STEPS_GOAL)
    private val persistedCompletedSessions = linkedSetOf<String>()
    private val persistingCompletedSessions = linkedSetOf<String>()

    internal var ownsScope: Boolean = false

    override val state: StateFlow<HealthUiState> = mutableState.asStateFlow()

    init {
        scope.launch {
            deviceRepository.observeAssociatedDevice().collect { device ->
                associatedDevice.value = device
            }
        }
        scope.launch {
            heartRateRepository.observeLatestValidMeasurement().collect { measurement ->
                latestMeasurement.value = measurement
            }
        }
        scope.launch {
            heartRateRepository.observeRecentManualMeasurements(RECENT_MANUAL_MEASUREMENTS_LIMIT).collect { measurements ->
                recentMeasurements.value = measurements
            }
        }
        scope.launch {
            selectedHistoryPeriod.flatMapLatest { period ->
                heartRateRepository.observeHistory(
                    HeartRateHistoryQuery(
                        period = period,
                        zoneId = java.time.ZoneId.systemDefault(),
                    ),
                )
            }.collect { snapshot ->
                history.value = snapshot
            }
        }
        scope.launch {
            associatedDevice.flatMapLatest { device ->
                if (device == null) {
                    flowOf(null)
                } else {
                    heartRateRepository.observeD1ImportMetadata(device.id)
                }
            }.collect { metadata ->
                latestD1ImportMetadata.value = metadata
            }
        }
        scope.launch {
            personalMetricsRepository.observeSnapshot().collect { snapshot ->
                personalMetrics.value = snapshot
            }
        }
        scope.launch {
            observedDate.flatMapLatest { date ->
                dailySummaryRepository.observeSummaryForDate(date)
            }.collect { summary ->
                todaySummary.value = summary
            }
        }
        scope.launch {
            appPreferencesRepository.observeDailyStepsGoal().collect { goal ->
                dailyStepsGoal.value = goal
            }
        }
        scope.launch {
            manualHeartRateCoordinator.state.collect(::handleCoordinatorState)
        }
        scope.launch {
            d1HeartRateSyncCoordinator.state.collect(::handleD1CoordinatorState)
        }
        scope.launch {
            val stateFlows = combine(
                surfaceState,
                d1ImportState,
                selectedHistoryPeriod,
                history,
            ) { currentSurface, currentD1ImportState, period, currentHistory ->
                PartialHealthState(
                    surfaceState = currentSurface,
                    d1ImportState = currentD1ImportState,
                    selectedHistoryPeriod = period,
                    history = currentHistory,
                )
            }
            val dataFlows = combine(
                latestD1ImportMetadata,
                latestMeasurement,
                recentMeasurements,
                associatedDevice,
            ) { metadata, latest, recent, device ->
                PartialHealthData(
                    latestD1ImportMetadata = metadata,
                    latestMeasurement = latest,
                    recentMeasurements = recent,
                    associatedDevice = device,
                )
            }
            val coreState = combine(stateFlows, dataFlows) { partialState, partialData ->
                CoreHealthState(
                    surfaceState = partialState.surfaceState,
                    d1ImportState = partialState.d1ImportState,
                    selectedHistoryPeriod = partialState.selectedHistoryPeriod,
                    history = partialState.history,
                    latestD1ImportMetadata = partialData.latestD1ImportMetadata,
                    latestMeasurement = partialData.latestMeasurement,
                    recentMeasurements = partialData.recentMeasurements,
                    associatedDevice = partialData.associatedDevice,
                )
            }
            combine(
                coreState,
                personalMetrics,
                todaySummary,
                dailyStepsGoal,
            ) { core, metrics, summary, goal ->
                HealthUiState(
                    surfaceState = core.surfaceState,
                    d1ImportState = core.d1ImportState,
                    selectedHistoryPeriod = core.selectedHistoryPeriod,
                    history = core.history,
                    latestD1ImportMetadata = core.latestD1ImportMetadata,
                    latestMeasurement = core.latestMeasurement,
                    recentMeasurements = core.recentMeasurements,
                    latestWeight = metrics.toWeightSnapshot(),
                    todaySummary = summary?.toHealthDailySummarySnapshot(core.associatedDevice),
                    dailyStepsGoal = goal,
                    hasAssociatedDevice = core.associatedDevice != null,
                    associatedDeviceName = core.associatedDevice?.name,
                )
            }.collect { mutableState.value = it }
        }
    }

    @Inject
    constructor(
        heartRateRepository: HeartRateRepository,
        deviceRepository: DeviceRepository,
        dailySummaryRepository: DailySummaryRepository,
        personalMetricsRepository: PersonalMetricsRepository,
        appPreferencesRepository: AppPreferencesRepository,
        manualHeartRateCoordinator: ManualHeartRateCoordinator,
        d1HeartRateSyncCoordinator: D1HeartRateSyncCoordinator,
    ) : this(
        heartRateRepository = heartRateRepository,
        deviceRepository = deviceRepository,
        dailySummaryRepository = dailySummaryRepository,
        personalMetricsRepository = personalMetricsRepository,
        appPreferencesRepository = appPreferencesRepository,
        manualHeartRateCoordinator = manualHeartRateCoordinator,
        d1HeartRateSyncCoordinator = d1HeartRateSyncCoordinator,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        ids = UuidV7Generator(),
        parserVersion = ParserVersion.create(MANUAL_HEART_RATE_PARSER_VERSION).valueOrNull()!!,
    ) {
        ownsScope = true
    }

    override fun onIntent(intent: HealthIntent) {
        when (intent) {
            HealthIntent.Load -> observedDate.value = LocalDate.now()
            HealthIntent.StartMeasurement -> startMeasurement()
            HealthIntent.CancelMeasurement -> cancelMeasurement(ManualHeartRateCancellationReason.USER_REQUESTED)
            HealthIntent.StartD1Import -> startD1Import()
            HealthIntent.CancelD1Import -> cancelD1Import(D1HeartRateImportCancellationReason.USER_REQUESTED)
            HealthIntent.Retry -> retryMeasurement()
            HealthIntent.RetryD1Import -> retryD1Import()
            HealthIntent.DismissError -> dismissTransientSurface()
            HealthIntent.DismissD1Import -> dismissTransientD1Import()
            HealthIntent.ConfirmExitMeasurement -> cancelMeasurement(ManualHeartRateCancellationReason.EXIT_CONFIRMED)
            HealthIntent.ConfirmExitD1Import -> cancelD1Import(D1HeartRateImportCancellationReason.EXIT_CONFIRMED)
            HealthIntent.AppMovedToBackground -> {
                cancelMeasurement(ManualHeartRateCancellationReason.BACKGROUND)
                cancelD1Import(D1HeartRateImportCancellationReason.BACKGROUND)
            }
            is HealthIntent.SelectHistoryPeriod -> selectHistoryPeriod(intent.period)
            HealthIntent.OpenDevice -> Unit
        }
    }

    override fun onCleared() {
        if (ownsScope) {
            scope.cancel()
        }
        super.onCleared()
    }

    private fun startMeasurement() {
        val device = associatedDevice.value
        val sessionId = ids.next().toString()
        if (device == null) {
            surfaceState.value = HealthSurfaceState.Failed(
                session = ManualHeartRateSessionState(
                    manualMeasurementSessionId = sessionId,
                    error = noAssociatedDeviceError(),
                ),
            )
            return
        }

        scope.launch {
            val error = manualHeartRateCoordinator.startMeasurement(
                ManualHeartRateMeasurementRequest(
                    manualMeasurementSessionId = sessionId,
                    associatedDevice = device,
                ),
            )
            if (error != null && shouldPresentImmediateError(error)) {
                surfaceState.value = HealthSurfaceState.Failed(
                    session = ManualHeartRateSessionState(
                        manualMeasurementSessionId = sessionId,
                        error = error,
                    ),
                )
            }
        }
    }

    private fun retryMeasurement() {
        if (isMeasurementInFlight(surfaceState.value)) return
        dismissTransientSurface()
        startMeasurement()
    }

    private fun dismissTransientSurface() {
        if (!isMeasurementInFlight(surfaceState.value)) {
            manualHeartRateCoordinator.reset()
            surfaceState.value = HealthSurfaceState.Idle
        }
        dismissTransientD1Import()
    }

    private fun cancelMeasurement(reason: ManualHeartRateCancellationReason) {
        if (!isMeasurementInFlight(surfaceState.value)) return
        scope.launch {
            manualHeartRateCoordinator.cancelMeasurement(reason)
        }
    }

    private fun selectHistoryPeriod(period: HeartRateHistoryPeriod) {
        selectedHistoryPeriod.value = period
    }

    private fun startD1Import() {
        val device = associatedDevice.value
        val attemptId = ids.next().toString()
        if (device == null) {
            d1ImportState.value = D1HeartRateImportSurfaceState.Failed(
                session = D1HeartRateImportSessionState(
                    importAttemptId = attemptId,
                    error = noAssociatedDeviceD1Error(),
                ),
            )
            return
        }

        scope.launch {
            val error = d1HeartRateSyncCoordinator.startImport(
                D1HeartRateSyncRequest(
                    importAttemptId = attemptId,
                    associatedDevice = device,
                ),
            )
            if (error != null && shouldPresentImmediateD1Error(error)) {
                d1ImportState.value = D1HeartRateImportSurfaceState.Failed(
                    session = D1HeartRateImportSessionState(
                        importAttemptId = attemptId,
                        error = error,
                    ),
                )
            }
        }
    }

    private fun retryD1Import() {
        if (isD1ImportInFlight(d1ImportState.value)) return
        dismissTransientD1Import()
        startD1Import()
    }

    private fun dismissTransientD1Import() {
        if (!isD1ImportInFlight(d1ImportState.value)) {
            d1HeartRateSyncCoordinator.reset()
            d1ImportState.value = D1HeartRateImportSurfaceState.Idle
        }
    }

    private fun cancelD1Import(reason: D1HeartRateImportCancellationReason) {
        if (!isD1ImportInFlight(d1ImportState.value)) return
        scope.launch {
            d1HeartRateSyncCoordinator.cancelImport(reason)
        }
    }

    private fun handleCoordinatorState(next: HealthSurfaceState) {
        when (next) {
            HealthSurfaceState.Idle,
            is HealthSurfaceState.Preparing,
            is HealthSurfaceState.Connecting,
            is HealthSurfaceState.Starting,
            is HealthSurfaceState.Measuring,
            is HealthSurfaceState.Stopping,
            is HealthSurfaceState.Cancelled,
            is HealthSurfaceState.Failed,
            -> surfaceState.value = next

            is HealthSurfaceState.Completed -> persistCompletedSession(next.session)
        }
    }

    private fun handleD1CoordinatorState(next: D1HeartRateImportSurfaceState) {
        d1ImportState.value = next
    }

    private fun persistCompletedSession(session: ManualHeartRateSessionState) {
        if (!persistingCompletedSessions.add(session.manualMeasurementSessionId)) return
        if (!persistedCompletedSessions.add(session.manualMeasurementSessionId)) {
            persistingCompletedSessions.remove(session.manualMeasurementSessionId)
            surfaceState.value = HealthSurfaceState.Completed(session)
            return
        }

        val finalResult = session.finalResult
        if (finalResult == null) {
            persistingCompletedSessions.remove(session.manualMeasurementSessionId)
            persistedCompletedSessions.remove(session.manualMeasurementSessionId)
            surfaceState.value = HealthSurfaceState.Failed(
                session = session.copy(error = invalidPersistenceStateError()),
            )
            return
        }

        surfaceState.value = HealthSurfaceState.Stopping(session)
        scope.launch {
            val input = session.toSaveManualMeasurementInput(finalResult)
            if (input == null) {
                persistedCompletedSessions.remove(session.manualMeasurementSessionId)
                persistingCompletedSessions.remove(session.manualMeasurementSessionId)
                surfaceState.value = HealthSurfaceState.Failed(
                    session = session.copy(error = invalidPersistenceStateError()),
                )
                return@launch
            }

            when (heartRateRepository.saveManualMeasurement(input)) {
                is RepositoryResult.Success -> {
                    persistingCompletedSessions.remove(session.manualMeasurementSessionId)
                    surfaceState.value = HealthSurfaceState.Completed(session)
                }

                RepositoryResult.StorageUnavailable,
                RepositoryResult.Conflict,
                RepositoryResult.NotFound,
                RepositoryResult.UnexpectedState,
                -> {
                    persistedCompletedSessions.remove(session.manualMeasurementSessionId)
                    persistingCompletedSessions.remove(session.manualMeasurementSessionId)
                    surfaceState.value = HealthSurfaceState.Failed(
                        session = session.copy(error = storageError()),
                    )
                }
            }
        }
    }

    private fun ManualHeartRateSessionState.toSaveManualMeasurementInput(
        finalResult: ManualHeartRateResult,
    ): SaveManualHeartRateMeasurementInput? {
        val sessionUuid = when (val parsed = UuidV7.parse(manualMeasurementSessionId)) {
            is DomainResult.Success -> parsed.value
            is DomainResult.Failure -> return null
        }
        return SaveManualHeartRateMeasurementInput(
            measurementId = HeartRateMeasurementId(ids.next()),
            sessionId = HeartRateMeasurementSessionId(sessionUuid),
            deviceId = associatedDevice.value?.id,
            bpm = finalResult.bpm,
            measuredAt = finalResult.measuredAt,
            parserVersion = parserVersion,
        )
    }

    private fun shouldPresentImmediateError(error: HealthMeasurementError): Boolean =
        error.category != HealthMeasurementErrorCategory.MEASUREMENT_IN_PROGRESS &&
            !isFailureForCurrentSession(error)

    private fun isFailureForCurrentSession(error: HealthMeasurementError): Boolean {
        val current = surfaceState.value as? HealthSurfaceState.Failed ?: return false
        return current.session.error == error
    }

    private fun shouldPresentImmediateD1Error(error: HealthD1ImportError): Boolean =
        error.category != HealthD1ImportErrorCategory.BLE_OPERATION_IN_PROGRESS &&
            !isD1FailureForCurrentSession(error)

    private fun isD1FailureForCurrentSession(error: HealthD1ImportError): Boolean {
        val current = d1ImportState.value as? D1HeartRateImportSurfaceState.Failed ?: return false
        return current.session.error == error
    }

    private fun isMeasurementInFlight(state: HealthSurfaceState): Boolean = when (state) {
        is HealthSurfaceState.Preparing,
        is HealthSurfaceState.Connecting,
        is HealthSurfaceState.Starting,
        is HealthSurfaceState.Measuring,
        is HealthSurfaceState.Stopping,
        -> true

        HealthSurfaceState.Idle,
        is HealthSurfaceState.Completed,
        is HealthSurfaceState.Cancelled,
        is HealthSurfaceState.Failed,
        -> false
    }

    private fun isD1ImportInFlight(state: D1HeartRateImportSurfaceState): Boolean = when (state) {
        is D1HeartRateImportSurfaceState.Preparing,
        is D1HeartRateImportSurfaceState.Connecting,
        is D1HeartRateImportSurfaceState.Reading,
        is D1HeartRateImportSurfaceState.Persisting,
        -> true

        D1HeartRateImportSurfaceState.Idle,
        is D1HeartRateImportSurfaceState.Completed,
        is D1HeartRateImportSurfaceState.CompletedWithGaps,
        is D1HeartRateImportSurfaceState.Partial,
        is D1HeartRateImportSurfaceState.Failed,
        is D1HeartRateImportSurfaceState.Cancelled,
        -> false
    }

    private fun noAssociatedDeviceError() = HealthMeasurementError(
        category = HealthMeasurementErrorCategory.NO_ASSOCIATED_DEVICE,
        message = "Nenhum relógio associado foi encontrado para medir a frequência cardíaca.",
        isRetryable = false,
    )

    private fun noAssociatedDeviceD1Error() = HealthD1ImportError(
        category = HealthD1ImportErrorCategory.NO_ASSOCIATED_DEVICE,
        message = "Nenhum relógio associado foi encontrado para buscar o histórico de frequência cardíaca.",
        isRetryable = false,
    )

    private fun invalidPersistenceStateError() = HealthMeasurementError(
        category = HealthMeasurementErrorCategory.INTERNAL,
        message = "Não foi possível concluir o salvamento da medição manual.",
        isRetryable = true,
    )

    private fun storageError() = HealthMeasurementError(
        category = HealthMeasurementErrorCategory.STORAGE_FAILED,
        message = "Não foi possível salvar a medição manual concluída.",
        isRetryable = true,
    )

    private fun PersonalMetricsSnapshot.toWeightSnapshot(): HealthWeightSnapshot? {
        val kilograms = weightKilograms ?: return null
        val measuredAt = latestWeightMeasuredAt ?: return null
        return HealthWeightSnapshot(
            kilograms = kilograms,
            measuredAt = measuredAt,
            origin = latestWeightOrigin,
        )
    }

    private fun DailySummary.toHealthDailySummarySnapshot(device: Device?): HealthDailySummarySnapshot =
        HealthDailySummarySnapshot(
            steps = steps,
            estimatedDistanceMeters = estimatedDistanceMeters,
            updatedAt = updatedAt,
            source = key.origin,
            sourceLabel = when (key.origin) {
                br.com.mwconsulting.championcompanion.core.model.DataOrigin.WATCH_DAILY_SUMMARY -> device?.name ?: "Relógio"
                else -> "Resumo local"
            },
        )

    private data class CoreHealthState(
        val surfaceState: HealthSurfaceState,
        val d1ImportState: D1HeartRateImportSurfaceState,
        val selectedHistoryPeriod: HeartRateHistoryPeriod,
        val history: HeartRateHistorySnapshot,
        val latestD1ImportMetadata: D1HeartRateImportMetadata?,
        val latestMeasurement: HeartRateMeasurement?,
        val recentMeasurements: List<HeartRateMeasurement>,
        val associatedDevice: Device?,
    )

    private data class PartialHealthState(
        val surfaceState: HealthSurfaceState,
        val d1ImportState: D1HeartRateImportSurfaceState,
        val selectedHistoryPeriod: HeartRateHistoryPeriod,
        val history: HeartRateHistorySnapshot,
    )

    private data class PartialHealthData(
        val latestD1ImportMetadata: D1HeartRateImportMetadata?,
        val latestMeasurement: HeartRateMeasurement?,
        val recentMeasurements: List<HeartRateMeasurement>,
        val associatedDevice: Device?,
    )
}
