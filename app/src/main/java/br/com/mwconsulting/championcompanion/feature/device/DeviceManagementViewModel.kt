package br.com.mwconsulting.championcompanion.feature.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBlePrerequisites
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleScanner
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCancellationReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDisconnectReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionRuntimePermission
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionScanRequest
import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidBleEnvironment
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationAcquireResult
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationGate
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationOwner
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationPermit
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationRequest
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationSnapshot
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class DeviceManagementViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val bleEnvironment: AndroidBleEnvironment,
    private val scanner: ChampionBleScanner,
    private val connectionManager: ChampionConnectionManager,
    private val connectionBootstrap: DeviceConnectionBootstrap,
    private val foregroundBleOperationGate: ForegroundBleOperationGate,
) : ViewModel(), DeviceManagementFeatureContract {
    private val mutableState = MutableStateFlow(DeviceManagementUiState())
    private val mutableEvents = MutableSharedFlow<DeviceManagementEvent>(extraBufferCapacity = 8)
    private val deviceIdGenerator = UuidV7Generator()

    private var associatedDeviceDomain: Device? = null
    private var scanCandidates: List<ChampionDiscoveredDevice> = emptyList()
    private var activeJob: Job? = null
    private var currentTransportState: ChampionBleState = ChampionBleState.Disconnected
    private var lastConnectIntent: DeviceManagementIntent? = null
    private var functionalConnectionEstablished = false
    private var autoConnectInFlight = false
    private var currentEntryId = 0L
    private var connectionOperationSequence = 0L
    private var lastAutoConnectAttemptEntryId: Long? = null
    private var isFeatureVisible = false
    private var foregroundBleOperation: ForegroundBleOperationSnapshot? = null

    override val state: StateFlow<DeviceManagementUiState> = mutableState.asStateFlow()
    val events: SharedFlow<DeviceManagementEvent> = mutableEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            deviceRepository.observeAssociatedDevice().collect { device ->
                associatedDeviceDomain = device
                mutableState.update { current ->
                    current.copy(associatedDevice = device?.toUiModel())
                }
                refreshIdleSurface()
                attemptAutoConnectForCurrentEntry()
            }
        }

        viewModelScope.launch {
            connectionManager.state.collect { transportState ->
                currentTransportState = transportState
                handleTransportState(transportState)
            }
        }

        viewModelScope.launch {
            scanner.scanResults.collect { candidates ->
                scanCandidates = candidates
                updateCandidatesState(candidates)
            }
        }

        viewModelScope.launch {
            foregroundBleOperationGate.observeActiveOperation().collect { operation ->
                foregroundBleOperation = operation
                if (currentTransportState is ChampionBleState.TransportReady) {
                    refreshIdleSurface()
                }
                attemptAutoConnectForCurrentEntry()
            }
        }
    }

    override fun onIntent(intent: DeviceManagementIntent) {
        when (intent) {
            DeviceManagementIntent.Load -> onFeatureEntry()
            DeviceManagementIntent.ExitVisibleScreen -> onFeatureExit()
            DeviceManagementIntent.RefreshPrerequisites -> refreshPrerequisites()

            DeviceManagementIntent.AddDevice -> startScan()
            DeviceManagementIntent.Retry -> retry()
            DeviceManagementIntent.CancelScan -> cancelScan()
            is DeviceManagementIntent.SelectCandidate -> selectCandidate(intent.candidateId)
            DeviceManagementIntent.ConnectSelectedCandidate -> connectSelectedCandidate()
            DeviceManagementIntent.ConnectAssociatedDevice -> connectAssociatedDevice()
            DeviceManagementIntent.DisconnectDevice -> disconnectDevice()
            DeviceManagementIntent.ForgetDevice -> emitEvent(DeviceManagementEvent.ShowForgetConfirmation)
            DeviceManagementIntent.ConfirmForgetDevice -> forgetDevice()
            DeviceManagementIntent.DismissError -> dismissError()
            DeviceManagementIntent.OpenDiagnostics -> emitTransientEvent(DeviceManagementEvent.OpenDiagnostics)
            DeviceManagementIntent.ExportDiagnostics -> emitTransientEvent(DeviceManagementEvent.ShareDiagnostics)
        }
    }

    private fun onFeatureEntry() {
        isFeatureVisible = true
        currentEntryId += 1
        if (associatedDeviceDomain == null) {
            refreshIdleSurface(clearError = true)
            return
        }
        refreshPrerequisites()
        attemptAutoConnectForCurrentEntry()
    }

    private fun onFeatureExit() {
        isFeatureVisible = false

        if (mutableState.value.activeOperation in setOf(
                DeviceManagementActiveOperation.REQUESTING_PERMISSIONS,
                DeviceManagementActiveOperation.SCANNING,
            )
        ) {
            activeJob?.cancel()
            activeJob = null
            autoConnectInFlight = false
        }
    }

    private fun refreshPrerequisites(emitResolutionEvents: Boolean = false) {
        val scanReadiness = ChampionBlePrerequisites.forScan(bleEnvironment.snapshot())
        val connectReadiness = ChampionBlePrerequisites.forConnect(bleEnvironment.snapshot())
        val missingPermission = (scanReadiness.missing + connectReadiness.missing).firstOrNull { it.permissions.isNotEmpty() }
        val allMissingPermissions = (scanReadiness.missing + connectReadiness.missing)
            .flatMap { it.permissions }
            .toSet()
        when {
            missingPermission != null -> {
                val permissions = allMissingPermissions.mapToUiPermissions()
                mutableState.update {
                    it.copy(
                        screenState = DeviceManagementScreenState.PermissionsRequired,
                        activeOperation = null,
                        error = missingPermission.error.toDeviceManagementError(),
                        retryAction = DeviceManagementRetryAction.RequestPermissions,
                    )
                }
                if (emitResolutionEvents) {
                    emitEvent(DeviceManagementEvent.RequestPermissions(permissions))
                }
            }

            scanReadiness.missing.any { it.error == ChampionBleError.BluetoothDisabled } ||
                connectReadiness.missing.any { it.error == ChampionBleError.BluetoothDisabled } -> {
                mutableState.update {
                    it.copy(
                        screenState = DeviceManagementScreenState.BluetoothDisabled,
                        activeOperation = null,
                        error = ChampionBleError.BluetoothDisabled.toDeviceManagementError(),
                        retryAction = DeviceManagementRetryAction.EnableBluetooth,
                    )
                }
            }

            else -> refreshIdleSurface(clearError = true)
        }
    }

    private fun startScan() {
        if (mutableState.value.activeOperation != null) return
        val readiness = ChampionBlePrerequisites.forScan(bleEnvironment.snapshot())
        if (!readiness.isReady) {
            handlePrerequisiteFailure(readiness.missing.first().error, readiness.missing.flatMap { it.permissions }.toSet())
            return
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            functionalConnectionEstablished = false
            mutableState.update {
                it.copy(
                    screenState = DeviceManagementScreenState.Scanning,
                    selectedCandidateId = null,
                    candidates = emptyList(),
                    activeOperation = DeviceManagementActiveOperation.SCANNING,
                    connectionState = DeviceManagementConnectionState.SCANNING,
                    error = null,
                    retryAction = null,
                )
            }
            val scanResult = when (val gateResult = acquireDeviceConnectionPermit()) {
                is ForegroundBleOperationAcquireResult.Acquired -> try {
                    connectionManager.scan(ChampionScanRequest(timeout = 12.seconds))
                } finally {
                    withContext(NonCancellable) {
                        foregroundBleOperationGate.release(gateResult.permit)
                    }
                }

                is ForegroundBleOperationAcquireResult.Rejected -> {
                    presentForegroundBleBlockError(
                        owner = gateResult.activeOperation.owner,
                        retryAction = DeviceManagementRetryAction.RestartScan,
                    )
                    return@launch
                }
            }
            when (val result = scanResult) {
                is ChampionBleResult.Success -> {
                    activeJob = null
                    scanCandidates = result.value
                    val selected = resolveSelectedCandidateId(scanCandidates)
                    mutableState.update { current ->
                        current.copy(
                            screenState = if (scanCandidates.isEmpty()) {
                                DeviceManagementScreenState.DeviceNotFound
                            } else {
                                DeviceManagementScreenState.ScanResults
                            },
                            selectedCandidateId = selected,
                            candidates = scanCandidates.toUiCandidates(selected),
                            activeOperation = null,
                            connectionState = currentConnectionStateForIdleSurface(),
                            error = if (scanCandidates.isEmpty()) {
                                ChampionBleError.DeviceNotFound.toDeviceManagementError()
                            } else {
                                null
                            },
                            retryAction = if (scanCandidates.isEmpty()) {
                                DeviceManagementRetryAction.RestartScan
                            } else {
                                null
                            },
                        )
                    }
                }

                is ChampionBleResult.Failure -> {
                    if (result.error is ChampionBleError.OperationCancelled) {
                        activeJob = null
                        restoreSurfaceAfterScanStop(clearError = true)
                    } else {
                        handleBleFailure(result.error)
                    }
                }
            }
        }
    }

    private fun cancelScan() {
        viewModelScope.launch {
            scanner.cancelScan(ChampionCancellationReason.USER_REQUESTED)
        }
    }

    private fun selectCandidate(candidateId: DeviceManagementCandidateId) {
        if (scanCandidates.none { it.transportKey == candidateId.value }) return
        mutableState.update {
            it.copy(
                selectedCandidateId = candidateId,
                candidates = scanCandidates.toUiCandidates(candidateId),
                error = null,
                retryAction = null,
            )
        }
    }

    private fun connectSelectedCandidate() {
        val selectedId = mutableState.value.selectedCandidateId ?: return
        val candidate = scanCandidates.firstOrNull { it.transportKey == selectedId.value } ?: return
        lastConnectIntent = DeviceManagementIntent.ConnectSelectedCandidate
        connect(candidate)
    }

    private fun connectAssociatedDevice() {
        connectAssociatedDevice(automatic = false)
    }

    private fun connectAssociatedDevice(automatic: Boolean) {
        val associatedDevice = associatedDeviceDomain ?: return
        lastConnectIntent = DeviceManagementIntent.ConnectAssociatedDevice
        connect(
            ChampionDiscoveredDevice(
                transportKey = associatedDevice.platformIdentifier,
                name = associatedDevice.name,
                localName = associatedDevice.name,
                observedAt = Instant.now(),
            ),
            automatic = automatic,
            isAssociatedDevice = true,
        )
    }

    private fun connect(
        candidate: ChampionDiscoveredDevice,
        automatic: Boolean = false,
        isAssociatedDevice: Boolean = false,
    ) {
        if (mutableState.value.activeOperation != null) return
        val activeForegroundBleOperation = foregroundBleOperation?.owner
        if (activeForegroundBleOperation != null) {
            if (!automatic) {
                presentForegroundBleBlockError(
                    owner = activeForegroundBleOperation,
                    retryAction = connectionRetryAction(),
                )
            }
            return
        }
        val readiness = ChampionBlePrerequisites.forConnect(bleEnvironment.snapshot())
        if (!readiness.isReady) {
            handlePrerequisiteFailure(
                error = readiness.missing.first().error,
                permissions = readiness.missing.flatMap { it.permissions }.toSet(),
                emitResolutionEvents = !automatic,
            )
            return
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            var gatePermit: ForegroundBleOperationPermit? = null
            functionalConnectionEstablished = false
            autoConnectInFlight = automatic
            try {
                mutableState.update {
                    it.copy(
                        screenState = DeviceManagementScreenState.Connecting,
                        activeOperation = DeviceManagementActiveOperation.CONNECTING,
                        connectionState = if (automatic && isAssociatedDevice) {
                            DeviceManagementConnectionState.SEARCHING_REMEMBERED_DEVICE
                        } else {
                            DeviceManagementConnectionState.CONNECTING
                        },
                        error = null,
                        retryAction = null,
                    )
                }

                when (val gateResult = acquireDeviceConnectionPermit()) {
                    is ForegroundBleOperationAcquireResult.Acquired -> gatePermit = gateResult.permit
                    is ForegroundBleOperationAcquireResult.Rejected -> {
                        autoConnectInFlight = false
                        if (automatic) {
                            mutableState.update { it.copy(activeOperation = null) }
                            refreshIdleSurface(clearError = true)
                        } else {
                            presentForegroundBleBlockError(
                                owner = gateResult.activeOperation.owner,
                                retryAction = connectionRetryAction(),
                            )
                        }
                        return@launch
                    }
                }

                when (val result = if (isAssociatedDevice) {
                    connectionBootstrap.connectAssociatedDevice(candidate)
                } else {
                    connectionBootstrap.connectAndHandshake(candidate)
                }) {
                    is ChampionBleResult.Success -> {
                        functionalConnectionEstablished = true
                        autoConnectInFlight = false
                        when (persistAssociation(candidate)) {
                            is RepositoryResult.Success -> {
                                mutableState.update {
                                    it.copy(
                                        screenState = DeviceManagementScreenState.Connected,
                                        activeOperation = null,
                                        connectionState = DeviceManagementConnectionState.CONNECTED,
                                        error = null,
                                        retryAction = null,
                                    )
                                }
                            }

                            RepositoryResult.Conflict,
                            RepositoryResult.NotFound,
                            RepositoryResult.StorageUnavailable,
                            RepositoryResult.UnexpectedState,
                            -> handleRepositoryFailure(DeviceManagementErrorCategory.INTERNAL, DeviceManagementRetryAction.ReconnectAssociatedDevice)
                        }
                    }

                    is ChampionBleResult.Failure -> {
                        autoConnectInFlight = false
                        handleBleFailure(result.error)
                    }
                }
            } finally {
                gatePermit?.let { foregroundBleOperationGate.release(it) }
            }
        }
    }

    private suspend fun persistAssociation(candidate: ChampionDiscoveredDevice): RepositoryResult<Device> {
        val existing = when (val lookup = deviceRepository.findByPlatformIdentifier(candidate.transportKey)) {
            is RepositoryResult.Success -> lookup.value
            RepositoryResult.NotFound,
            RepositoryResult.Conflict,
            RepositoryResult.StorageUnavailable,
            RepositoryResult.UnexpectedState,
            -> null
        }

        val now = Instant.now()
        val domainDevice = existing ?: when (
            val created = Device.create(
                id = DeviceId(deviceIdGenerator.next()),
                platformIdentifier = candidate.transportKey,
                name = candidate.displayName(),
                associatedAt = now,
                lastSuccessfulConnectionAt = now,
            )
        ) {
            is br.com.mwconsulting.championcompanion.core.model.DomainResult.Success -> created.value
            is br.com.mwconsulting.championcompanion.core.model.DomainResult.Failure -> {
                return RepositoryResult.UnexpectedState
            }
        }

        val associatedDevice = if (existing == null) {
            domainDevice
        } else {
            Device.create(
                id = existing.id,
                platformIdentifier = existing.platformIdentifier,
                name = candidate.displayName(),
                associatedAt = existing.associatedAt,
                model = existing.model,
                firmware = existing.firmware,
                lastSuccessfulConnectionAt = now,
                importMetadata = existing.importMetadata,
            ).let { created ->
                when (created) {
                    is br.com.mwconsulting.championcompanion.core.model.DomainResult.Success -> created.value
                    is br.com.mwconsulting.championcompanion.core.model.DomainResult.Failure -> return RepositoryResult.UnexpectedState
                }
            }
        }

        return deviceRepository.associateDeviceAndUpdateLastSuccessfulConnection(
            device = associatedDevice,
            connectedAt = now,
        )
    }

    private fun disconnectDevice() {
        if (mutableState.value.activeOperation != null) return
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            var gatePermit: ForegroundBleOperationPermit? = null
            try {
                mutableState.update {
                    it.copy(
                        screenState = DeviceManagementScreenState.Disconnecting,
                        activeOperation = DeviceManagementActiveOperation.DISCONNECTING,
                        connectionState = DeviceManagementConnectionState.DISCONNECTING,
                        error = null,
                        retryAction = null,
                    )
                }
                when (val gateResult = acquireDeviceConnectionPermit()) {
                    is ForegroundBleOperationAcquireResult.Acquired -> gatePermit = gateResult.permit
                    is ForegroundBleOperationAcquireResult.Rejected -> {
                        presentForegroundBleBlockError(
                            owner = gateResult.activeOperation.owner,
                            retryAction = DeviceManagementRetryAction.RetryDisconnect,
                        )
                        return@launch
                    }
                }

                when (val result = connectionManager.disconnect(ChampionDisconnectReason.USER_REQUESTED)) {
                    is ChampionBleResult.Success -> {
                        functionalConnectionEstablished = false
                        autoConnectInFlight = false
                        mutableState.update {
                            it.copy(
                                screenState = if (associatedDeviceDomain != null) {
                                    DeviceManagementScreenState.Disconnected
                                } else {
                                    DeviceManagementScreenState.NoAssociatedDevice
                                },
                                activeOperation = null,
                                connectionState = DeviceManagementConnectionState.DISCONNECTED,
                            )
                        }
                    }

                    is ChampionBleResult.Failure -> handleBleFailure(result.error)
                }
            } finally {
                gatePermit?.let { foregroundBleOperationGate.release(it) }
            }
        }
    }

    private fun forgetDevice() {
        if (mutableState.value.activeOperation != null) return
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            var gatePermit: ForegroundBleOperationPermit? = null
            try {
                mutableState.update {
                    it.copy(
                        screenState = DeviceManagementScreenState.Forgetting,
                        activeOperation = DeviceManagementActiveOperation.FORGETTING,
                        error = null,
                        retryAction = null,
                    )
                }
                when (val gateResult = acquireDeviceConnectionPermit()) {
                    is ForegroundBleOperationAcquireResult.Acquired -> gatePermit = gateResult.permit
                    is ForegroundBleOperationAcquireResult.Rejected -> {
                        presentForegroundBleBlockError(
                            owner = gateResult.activeOperation.owner,
                            retryAction = null,
                        )
                        return@launch
                    }
                }

                connectionManager.disconnect(ChampionDisconnectReason.CLEANUP)
                functionalConnectionEstablished = false
                autoConnectInFlight = false
                when (deviceRepository.forgetAssociatedDevice()) {
                    is RepositoryResult.Success -> {
                        associatedDeviceDomain = null
                        scanCandidates = emptyList()
                        mutableState.update {
                            it.copy(
                                screenState = DeviceManagementScreenState.NoAssociatedDevice,
                                associatedDevice = null,
                                selectedCandidateId = null,
                                candidates = emptyList(),
                                activeOperation = null,
                                connectionState = DeviceManagementConnectionState.DISCONNECTED,
                                error = null,
                                retryAction = null,
                            )
                        }
                    }

                    RepositoryResult.Conflict,
                    RepositoryResult.NotFound,
                    RepositoryResult.StorageUnavailable,
                    RepositoryResult.UnexpectedState,
                    -> handleRepositoryFailure(DeviceManagementErrorCategory.FORGET, null)
                }
            } finally {
                gatePermit?.let { foregroundBleOperationGate.release(it) }
            }
        }
    }

    private fun retry() {
        when (mutableState.value.retryAction) {
            DeviceManagementRetryAction.RequestPermissions -> refreshPrerequisites(emitResolutionEvents = true)
            DeviceManagementRetryAction.EnableBluetooth -> {
                emitEvent(DeviceManagementEvent.OpenBluetoothSettings)
                refreshPrerequisites()
            }

            DeviceManagementRetryAction.RestartScan -> startScan()
            DeviceManagementRetryAction.ReconnectAssociatedDevice,
            DeviceManagementRetryAction.RetryHandshake,
            -> when (lastConnectIntent) {
                DeviceManagementIntent.ConnectAssociatedDevice -> connectAssociatedDevice()
                DeviceManagementIntent.ConnectSelectedCandidate -> connectSelectedCandidate()
                else -> connectAssociatedDevice()
            }

            DeviceManagementRetryAction.RetrySelectedCandidateConnection -> connectSelectedCandidate()
            DeviceManagementRetryAction.RetryDisconnect -> disconnectDevice()
            null -> Unit
        }
    }

    private fun dismissError() {
        mutableState.update {
            it.copy(
                error = null,
                retryAction = null,
            )
        }
        refreshIdleSurface(clearError = true)
    }

    private fun handleTransportState(transportState: ChampionBleState) {
        when (transportState) {
            ChampionBleState.Disconnected -> {
                functionalConnectionEstablished = false
                activeJob = null
                autoConnectInFlight = false
                refreshIdleSurface()
                attemptAutoConnectForCurrentEntry()
            }

            is ChampionBleState.Scanning -> {
                mutableState.update {
                    it.copy(
                        connectionState = if (autoConnectInFlight) {
                            DeviceManagementConnectionState.SEARCHING_REMEMBERED_DEVICE
                        } else {
                            DeviceManagementConnectionState.SCANNING
                        },
                        screenState = if (autoConnectInFlight) {
                            DeviceManagementScreenState.Connecting
                        } else {
                            DeviceManagementScreenState.Scanning
                        },
                    )
                }
            }

            is ChampionBleState.Connecting -> {
                mutableState.update {
                    it.copy(
                        connectionState = DeviceManagementConnectionState.CONNECTING,
                        screenState = DeviceManagementScreenState.Connecting,
                    )
                }
            }

            is ChampionBleState.DiscoveringServices,
            is ChampionBleState.ResolvingChannels,
            is ChampionBleState.ConfiguringSubscriptions,
            -> {
                if (!functionalConnectionEstablished) {
                    mutableState.update {
                        it.copy(
                            connectionState = DeviceManagementConnectionState.PREPARING_CONNECTION,
                            screenState = DeviceManagementScreenState.PreparingConnection,
                        )
                    }
                }
            }

            is ChampionBleState.TransportReady -> {
                if (hasFunctionalTransport()) {
                    refreshIdleSurface(clearError = true)
                } else if (!functionalConnectionEstablished) {
                    mutableState.update {
                        it.copy(
                            connectionState = DeviceManagementConnectionState.PREPARING_CONNECTION,
                            screenState = DeviceManagementScreenState.PreparingConnection,
                        )
                    }
                }
            }

            is ChampionBleState.Disconnecting -> {
                mutableState.update {
                    it.copy(
                        connectionState = DeviceManagementConnectionState.DISCONNECTING,
                        screenState = DeviceManagementScreenState.Disconnecting,
                    )
                }
            }

            is ChampionBleState.Failed -> {
                functionalConnectionEstablished = false
                autoConnectInFlight = false
                handleBleFailure(transportState.error)
            }
        }
    }

    private fun handlePrerequisiteFailure(
        error: ChampionBleError,
        permissions: Set<ChampionRuntimePermission>,
        emitResolutionEvents: Boolean = true,
    ) {
        if (permissions.isNotEmpty()) {
            mutableState.update {
                it.copy(
                    screenState = DeviceManagementScreenState.PermissionsRequired,
                    activeOperation = if (emitResolutionEvents) DeviceManagementActiveOperation.REQUESTING_PERMISSIONS else null,
                    connectionState = DeviceManagementConnectionState.DISCONNECTED,
                    error = error.toDeviceManagementError(),
                    retryAction = DeviceManagementRetryAction.RequestPermissions,
                )
            }
            if (emitResolutionEvents) {
                emitEvent(DeviceManagementEvent.RequestPermissions(permissions.mapToUiPermissions()))
            }
            return
        }
        if (error == ChampionBleError.BluetoothDisabled) {
            mutableState.update {
                it.copy(
                    screenState = DeviceManagementScreenState.BluetoothDisabled,
                    activeOperation = null,
                    error = error.toDeviceManagementError(),
                    retryAction = DeviceManagementRetryAction.EnableBluetooth,
                )
            }
            if (emitResolutionEvents) {
                emitEvent(DeviceManagementEvent.OpenBluetoothSettings)
            }
            return
        }
        handleBleFailure(error)
    }

    private fun handleBleFailure(error: ChampionBleError) {
        if (error is ChampionBleError.OperationCancelled) {
            activeJob = null
            autoConnectInFlight = false
            refreshIdleSurface(clearError = true)
            return
        }
        activeJob = null
        mutableState.update {
            it.copy(
                screenState = when (error) {
                    ChampionBleError.DeviceNotFound -> DeviceManagementScreenState.DeviceNotFound
                    else -> DeviceManagementScreenState.ConnectionFailed
                },
                activeOperation = null,
                connectionState = DeviceManagementConnectionState.FAILED,
                error = error.toDeviceManagementError(),
                retryAction = when (lastConnectIntent) {
                    DeviceManagementIntent.ConnectSelectedCandidate -> DeviceManagementRetryAction.RetrySelectedCandidateConnection
                    DeviceManagementIntent.ConnectAssociatedDevice -> DeviceManagementRetryAction.ReconnectAssociatedDevice
                    else -> error.toDeviceManagementRetryAction()
                },
            )
        }
    }

    private fun attemptAutoConnectForCurrentEntry() {
        if (currentEntryId == 0L) return
        if (!isFeatureVisible) return
        val associatedDevice = associatedDeviceDomain ?: return
        if (mutableState.value.activeOperation != null) return
        if (hasFunctionalTransport()) return
        if (currentTransportState !is ChampionBleState.Disconnected && currentTransportState !is ChampionBleState.Failed) return
        if (lastAutoConnectAttemptEntryId == currentEntryId) return
        if (foregroundBleOperation?.owner != null) return

        val readiness = ChampionBlePrerequisites.forConnect(bleEnvironment.snapshot())
        if (!readiness.isReady) {
            handlePrerequisiteFailure(
                error = readiness.missing.first().error,
                permissions = readiness.missing.flatMap { it.permissions }.toSet(),
                emitResolutionEvents = false,
            )
            return
        }

        lastAutoConnectAttemptEntryId = currentEntryId
        connectAssociatedDevice(automatic = true)
    }

    private suspend fun acquireDeviceConnectionPermit(): ForegroundBleOperationAcquireResult =
        foregroundBleOperationGate.tryAcquire(
            ForegroundBleOperationRequest(
                owner = ForegroundBleOperationOwner.DEVICE_CONNECTION,
                operationId = "device-connection-${++connectionOperationSequence}",
                requestedAt = Instant.now(),
            ),
        )

    private fun connectionRetryAction(): DeviceManagementRetryAction = when (lastConnectIntent) {
        DeviceManagementIntent.ConnectSelectedCandidate -> DeviceManagementRetryAction.RetrySelectedCandidateConnection
        else -> DeviceManagementRetryAction.ReconnectAssociatedDevice
    }

    private fun presentForegroundBleBlockError(
        owner: ForegroundBleOperationOwner,
        retryAction: DeviceManagementRetryAction?,
    ) {
        activeJob = null
        mutableState.update {
            it.copy(
                screenState = DeviceManagementScreenState.Error,
                activeOperation = null,
                connectionState = currentTransportState.toDeviceManagementConnectionState(),
                error = DeviceManagementError(
                    category = DeviceManagementErrorCategory.INTERNAL,
                    message = owner.toDeviceManagementForegroundBleBlockMessage(),
                    isRecoverable = retryAction != null,
                ),
                retryAction = retryAction,
            )
        }
    }

    private fun ForegroundBleOperationOwner.toDeviceManagementForegroundBleBlockMessage(): String = when (this) {
        ForegroundBleOperationOwner.DAILY_STEPS_REFRESH -> "Há uma sincronização do relógio em andamento na tela inicial."
        ForegroundBleOperationOwner.MANUAL_HEART_RATE -> "Há uma medição manual em andamento na tela Saúde."
        ForegroundBleOperationOwner.WATCH_D1_HISTORY -> "Há uma importação de histórico em andamento na tela Saúde."
        ForegroundBleOperationOwner.WATCH_WORKOUT_IMPORT -> "Há uma importação de treinos do relógio em andamento na tela Exercícios."
        ForegroundBleOperationOwner.APP_WORKOUT_SESSION -> "Há uma participação opcional do relógio em andamento na sessão de treino."
        ForegroundBleOperationOwner.DEVICE_CONNECTION -> "Outra conexão do dispositivo está em andamento."
    }

    private fun handleRepositoryFailure(
        category: DeviceManagementErrorCategory,
        retryAction: DeviceManagementRetryAction?,
    ) {
        activeJob = null
        mutableState.update {
            it.copy(
                screenState = DeviceManagementScreenState.Error,
                activeOperation = null,
                connectionState = currentTransportState.toDeviceManagementConnectionState(),
                error = DeviceManagementError(
                    category = category,
                    message = "Nao foi possivel concluir operacao do dispositivo.",
                    isRecoverable = retryAction != null,
                ),
                retryAction = retryAction,
            )
        }
    }

    private fun refreshIdleSurface(clearError: Boolean = false) {
        if (
            mutableState.value.activeOperation != null &&
            mutableState.value.activeOperation != DeviceManagementActiveOperation.REQUESTING_PERMISSIONS
        ) {
            return
        }
        if (
            currentTransportState !is ChampionBleState.Disconnected &&
            currentTransportState !is ChampionBleState.Failed &&
            currentTransportState !is ChampionBleState.TransportReady
        ) {
            return
        }
        val nextScreenState = when {
            hasFunctionalTransport() -> DeviceManagementScreenState.Connected
            scanCandidates.isNotEmpty() -> DeviceManagementScreenState.ScanResults
            associatedDeviceDomain != null -> DeviceManagementScreenState.Disconnected
            else -> DeviceManagementScreenState.NoAssociatedDevice
        }
        mutableState.update {
            val selectedCandidateId = resolveSelectedCandidateId(scanCandidates, it.selectedCandidateId)
            it.copy(
                screenState = nextScreenState,
                connectionState = currentConnectionStateForIdleSurface(),
                activeOperation = null,
                error = if (clearError) null else it.error,
                retryAction = if (clearError) null else it.retryAction,
                selectedCandidateId = selectedCandidateId,
                candidates = scanCandidates.toUiCandidates(selectedCandidateId),
            )
        }
    }

    private fun restoreSurfaceAfterScanStop(clearError: Boolean) {
        val nextScreenState = when {
            hasFunctionalTransport() -> DeviceManagementScreenState.Connected
            scanCandidates.isNotEmpty() -> DeviceManagementScreenState.ScanResults
            associatedDeviceDomain != null -> DeviceManagementScreenState.Disconnected
            else -> DeviceManagementScreenState.NoAssociatedDevice
        }
        val selectedCandidateId = resolveSelectedCandidateId(scanCandidates)
        mutableState.update {
            it.copy(
                screenState = nextScreenState,
                connectionState = currentConnectionStateForIdleSurface(),
                activeOperation = null,
                error = if (clearError) null else it.error,
                retryAction = if (clearError) null else it.retryAction,
                selectedCandidateId = selectedCandidateId,
                candidates = scanCandidates.toUiCandidates(selectedCandidateId),
            )
        }
    }

    private fun hasFunctionalTransport(): Boolean =
        currentTransportState is ChampionBleState.TransportReady &&
            (
                functionalConnectionEstablished ||
                    foregroundBleOperation?.owner == ForegroundBleOperationOwner.APP_WORKOUT_SESSION
                )

    private fun currentConnectionStateForIdleSurface(): DeviceManagementConnectionState = when {
        hasFunctionalTransport() -> DeviceManagementConnectionState.CONNECTED
        else -> DeviceManagementConnectionState.DISCONNECTED
    }

    private fun updateCandidatesState(candidates: List<ChampionDiscoveredDevice>) {
        val current = mutableState.value
        val selectedCandidateId = resolveSelectedCandidateId(candidates, current.selectedCandidateId)
        val shouldShowResults = current.activeOperation == DeviceManagementActiveOperation.SCANNING ||
            current.screenState == DeviceManagementScreenState.ScanResults ||
            current.screenState == DeviceManagementScreenState.DeviceNotFound

        if (!shouldShowResults && candidates.isEmpty()) return

        mutableState.update {
            it.copy(
                selectedCandidateId = selectedCandidateId,
                candidates = candidates.toUiCandidates(selectedCandidateId),
            )
        }
    }

    private fun resolveSelectedCandidateId(
        candidates: List<ChampionDiscoveredDevice>,
        selectedCandidateId: DeviceManagementCandidateId? = mutableState.value.selectedCandidateId,
    ): DeviceManagementCandidateId? {
        if (selectedCandidateId != null && candidates.any { it.transportKey == selectedCandidateId.value }) {
            return selectedCandidateId
        }
        return candidates.firstOrNull()?.transportKey?.let(::DeviceManagementCandidateId)
    }

    private fun emitEvent(event: DeviceManagementEvent) {
        mutableEvents.tryEmit(event)
    }

    private fun emitTransientEvent(event: DeviceManagementEvent) {
        mutableState.update {
            it.copy(
                activeOperation = when (event) {
                    DeviceManagementEvent.OpenDiagnostics -> DeviceManagementActiveOperation.OPENING_DIAGNOSTICS
                    DeviceManagementEvent.ShareDiagnostics -> DeviceManagementActiveOperation.EXPORTING_DIAGNOSTICS
                    else -> it.activeOperation
                },
            )
        }
        emitEvent(event)
        mutableState.update { it.copy(activeOperation = null) }
    }

    private fun Device.toUiModel(): DeviceManagementAssociatedDevice = DeviceManagementAssociatedDevice(
        deviceId = DeviceManagementDeviceId(id.value.toString()),
        displayName = name,
        modelName = model.name,
        safeIdentifier = platformIdentifier.toSafeIdentifier(),
        firmware = firmware,
        associatedAt = associatedAt,
        lastSuccessfulConnectionAt = lastSuccessfulConnectionAt,
        lastImportedAt = importMetadata.lastImportedAt,
    )

    private fun List<ChampionDiscoveredDevice>.toUiCandidates(
        selectedId: DeviceManagementCandidateId?,
    ): List<DeviceManagementScanCandidate> = map { candidate ->
        DeviceManagementScanCandidate(
            candidateId = DeviceManagementCandidateId(candidate.transportKey),
            displayName = candidate.displayName(),
            safeIdentifier = candidate.transportKey.toSafeIdentifier(),
            signalLabel = null,
            isSelected = selectedId?.value == candidate.transportKey,
        )
    }

    private fun ChampionDiscoveredDevice.displayName(): String = localName ?: name ?: "Champion C001"

    private fun String.toSafeIdentifier(): String {
        val suffix = takeLast(4).uppercase()
        return if (suffix.isBlank()) "DISPOSITIVO" else "**** $suffix"
    }

    private fun Set<ChampionRuntimePermission>.mapToUiPermissions(): Set<DeviceManagementPermission> = mapNotNullTo(linkedSetOf()) {
        when (it) {
            ChampionRuntimePermission.BLUETOOTH_SCAN -> DeviceManagementPermission.BLUETOOTH_SCAN
            ChampionRuntimePermission.BLUETOOTH_CONNECT -> DeviceManagementPermission.BLUETOOTH_CONNECT
            ChampionRuntimePermission.ACCESS_FINE_LOCATION,
            ChampionRuntimePermission.ACCESS_COARSE_LOCATION,
            -> DeviceManagementPermission.ACCESS_FINE_LOCATION

            ChampionRuntimePermission.BLUETOOTH,
            ChampionRuntimePermission.BLUETOOTH_ADMIN,
            -> null
        }
    }
}
