package br.com.mwconsulting.championcompanion.feature.health

import androidx.lifecycle.ViewModel
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.DomainResult
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementId
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementSessionId
import br.com.mwconsulting.championcompanion.core.model.HeartRateRepository
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.SaveManualHeartRateMeasurementInput
import br.com.mwconsulting.championcompanion.core.model.UuidV7
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import br.com.mwconsulting.championcompanion.core.model.valueOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val RECENT_MANUAL_MEASUREMENTS_LIMIT = 10
private const val MANUAL_HEART_RATE_PARSER_VERSION = "manual-heart-rate-v1"

@HiltViewModel
class HealthViewModel internal constructor(
    private val heartRateRepository: HeartRateRepository,
    private val deviceRepository: DeviceRepository,
    private val manualHeartRateCoordinator: ManualHeartRateCoordinator,
    private val scope: CoroutineScope,
    private val ids: UuidV7Generator,
    private val parserVersion: ParserVersion,
) : ViewModel(), HealthFeatureContract {
    private val mutableState = MutableStateFlow(HealthUiState())
    private val surfaceState = MutableStateFlow<HealthSurfaceState>(HealthSurfaceState.Idle)
    private val associatedDevice = MutableStateFlow<Device?>(null)
    private val latestMeasurement = MutableStateFlow<HeartRateMeasurement?>(null)
    private val recentMeasurements = MutableStateFlow<List<HeartRateMeasurement>>(emptyList())
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
            manualHeartRateCoordinator.state.collect(::handleCoordinatorState)
        }
        scope.launch {
            combine(
                surfaceState,
                latestMeasurement,
                recentMeasurements,
                associatedDevice,
            ) { currentSurface, latest, recent, device ->
                HealthUiState(
                    surfaceState = currentSurface,
                    latestMeasurement = latest,
                    recentMeasurements = recent,
                    hasAssociatedDevice = device != null,
                    associatedDeviceName = device?.name,
                )
            }.collect { mutableState.value = it }
        }
    }

    @Inject
    constructor(
        heartRateRepository: HeartRateRepository,
        deviceRepository: DeviceRepository,
        manualHeartRateCoordinator: ManualHeartRateCoordinator,
    ) : this(
        heartRateRepository = heartRateRepository,
        deviceRepository = deviceRepository,
        manualHeartRateCoordinator = manualHeartRateCoordinator,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        ids = UuidV7Generator(),
        parserVersion = ParserVersion.create(MANUAL_HEART_RATE_PARSER_VERSION).valueOrNull()!!,
    ) {
        ownsScope = true
    }

    override fun onIntent(intent: HealthIntent) {
        when (intent) {
            HealthIntent.Load -> Unit
            HealthIntent.StartMeasurement -> startMeasurement()
            HealthIntent.CancelMeasurement -> cancelMeasurement(ManualHeartRateCancellationReason.USER_REQUESTED)
            HealthIntent.Retry -> retryMeasurement()
            HealthIntent.DismissError -> dismissTransientSurface()
            HealthIntent.ConfirmExitMeasurement -> cancelMeasurement(ManualHeartRateCancellationReason.EXIT_CONFIRMED)
            HealthIntent.AppMovedToBackground -> cancelMeasurement(ManualHeartRateCancellationReason.BACKGROUND)
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
    }

    private fun cancelMeasurement(reason: ManualHeartRateCancellationReason) {
        if (!isMeasurementInFlight(surfaceState.value)) return
        scope.launch {
            manualHeartRateCoordinator.cancelMeasurement(reason)
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

    private fun noAssociatedDeviceError() = HealthMeasurementError(
        category = HealthMeasurementErrorCategory.NO_ASSOCIATED_DEVICE,
        message = "Nenhum relógio associado foi encontrado para medir a frequência cardíaca.",
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
}
