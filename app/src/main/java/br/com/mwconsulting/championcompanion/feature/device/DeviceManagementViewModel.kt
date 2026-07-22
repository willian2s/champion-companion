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
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class DeviceManagementViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val bleEnvironment: AndroidBleEnvironment,
    private val scanner: ChampionBleScanner,
    private val connectionManager: ChampionConnectionManager,
    private val connectionBootstrap: DeviceConnectionBootstrap,
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
            }
        }

        viewModelScope.launch {
            connectionManager.state.collect { transportState ->
                currentTransportState = transportState
                handleTransportState(transportState)
            }
        }
    }

    override fun onIntent(intent: DeviceManagementIntent) {
        when (intent) {
            DeviceManagementIntent.Load,
            DeviceManagementIntent.RefreshPrerequisites,
            -> refreshPrerequisites()

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

    private fun refreshPrerequisites(emitResolutionEvents: Boolean = false) {
        val scanReadiness = ChampionBlePrerequisites.forScan(bleEnvironment.snapshot())
        val connectReadiness = ChampionBlePrerequisites.forConnect(bleEnvironment.snapshot())
        val missingPermission = (scanReadiness.missing + connectReadiness.missing).firstOrNull { it.permissions.isNotEmpty() }
        when {
            missingPermission != null -> {
                val permissions = missingPermission.permissions.mapToUiPermissions()
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
            when (val result = connectionManager.scan(ChampionScanRequest(timeout = 12.seconds))) {
                is ChampionBleResult.Success -> {
                    scanCandidates = result.value
                    val selected = scanCandidates.firstOrNull()?.transportKey?.let(::DeviceManagementCandidateId)
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
                            connectionState = DeviceManagementConnectionState.DISCONNECTED,
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

                is ChampionBleResult.Failure -> handleBleFailure(result.error)
            }
        }
    }

    private fun cancelScan() {
        viewModelScope.launch {
            scanner.cancelScan(ChampionCancellationReason.USER_REQUESTED)
            activeJob = null
            refreshIdleSurface(clearError = scanCandidates.isNotEmpty())
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
        val associatedDevice = associatedDeviceDomain ?: return
        lastConnectIntent = DeviceManagementIntent.ConnectAssociatedDevice
        connect(
            ChampionDiscoveredDevice(
                transportKey = associatedDevice.platformIdentifier,
                name = associatedDevice.name,
                localName = associatedDevice.name,
                observedAt = Instant.now(),
            ),
        )
    }

    private fun connect(candidate: ChampionDiscoveredDevice) {
        if (mutableState.value.activeOperation != null) return
        val readiness = ChampionBlePrerequisites.forConnect(bleEnvironment.snapshot())
        if (!readiness.isReady) {
            handlePrerequisiteFailure(readiness.missing.first().error, readiness.missing.flatMap { it.permissions }.toSet())
            return
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            functionalConnectionEstablished = false
            mutableState.update {
                it.copy(
                    screenState = DeviceManagementScreenState.Connecting,
                    activeOperation = DeviceManagementActiveOperation.CONNECTING,
                    connectionState = DeviceManagementConnectionState.CONNECTING,
                    error = null,
                    retryAction = null,
                )
            }

            when (val result = connectionBootstrap.connectAndHandshake(candidate)) {
                is ChampionBleResult.Success -> {
                    functionalConnectionEstablished = true
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

                is ChampionBleResult.Failure -> handleBleFailure(result.error)
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
                associatedAt = now,
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

        val associateResult = deviceRepository.associateDevice(associatedDevice)
        if (associateResult !is RepositoryResult.Success) return associateResult
        return deviceRepository.updateLastSuccessfulConnection(associateResult.value.id, now)
    }

    private fun disconnectDevice() {
        if (mutableState.value.activeOperation != null) return
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            mutableState.update {
                it.copy(
                    screenState = DeviceManagementScreenState.Disconnecting,
                    activeOperation = DeviceManagementActiveOperation.DISCONNECTING,
                    connectionState = DeviceManagementConnectionState.DISCONNECTING,
                    error = null,
                    retryAction = null,
                )
            }
            when (val result = connectionManager.disconnect(ChampionDisconnectReason.USER_REQUESTED)) {
                is ChampionBleResult.Success -> {
                    functionalConnectionEstablished = false
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
        }
    }

    private fun forgetDevice() {
        if (mutableState.value.activeOperation != null) return
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            mutableState.update {
                it.copy(
                    screenState = DeviceManagementScreenState.Forgetting,
                    activeOperation = DeviceManagementActiveOperation.FORGETTING,
                    error = null,
                    retryAction = null,
                )
            }
            connectionManager.disconnect(ChampionDisconnectReason.CLEANUP)
            functionalConnectionEstablished = false
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
                activeJob = null
                if (!functionalConnectionEstablished && mutableState.value.activeOperation == DeviceManagementActiveOperation.CONNECTING) {
                    refreshIdleSurface()
                }
            }

            is ChampionBleState.Scanning -> {
                mutableState.update {
                    it.copy(
                        connectionState = DeviceManagementConnectionState.SCANNING,
                        screenState = DeviceManagementScreenState.Scanning,
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
            is ChampionBleState.TransportReady,
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
                handleBleFailure(transportState.error)
            }
        }
    }

    private fun handlePrerequisiteFailure(
        error: ChampionBleError,
        permissions: Set<ChampionRuntimePermission>,
    ) {
        if (permissions.isNotEmpty()) {
            mutableState.update {
                it.copy(
                    screenState = DeviceManagementScreenState.PermissionsRequired,
                    activeOperation = DeviceManagementActiveOperation.REQUESTING_PERMISSIONS,
                    error = error.toDeviceManagementError(),
                    retryAction = DeviceManagementRetryAction.RequestPermissions,
                )
            }
            emitEvent(DeviceManagementEvent.RequestPermissions(permissions.mapToUiPermissions()))
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
            emitEvent(DeviceManagementEvent.OpenBluetoothSettings)
            return
        }
        handleBleFailure(error)
    }

    private fun handleBleFailure(error: ChampionBleError) {
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
        if (mutableState.value.activeOperation != null) return
        if (currentTransportState !is ChampionBleState.Disconnected && currentTransportState !is ChampionBleState.Failed) return
        val nextScreenState = when {
            scanCandidates.isNotEmpty() -> DeviceManagementScreenState.ScanResults
            associatedDeviceDomain != null -> DeviceManagementScreenState.Disconnected
            else -> DeviceManagementScreenState.NoAssociatedDevice
        }
        mutableState.update {
            it.copy(
                screenState = nextScreenState,
                connectionState = DeviceManagementConnectionState.DISCONNECTED,
                activeOperation = null,
                error = if (clearError) null else it.error,
                retryAction = if (clearError) null else it.retryAction,
                candidates = scanCandidates.toUiCandidates(it.selectedCandidateId),
            )
        }
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
