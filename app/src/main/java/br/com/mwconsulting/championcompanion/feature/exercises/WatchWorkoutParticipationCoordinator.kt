package br.com.mwconsulting.championcompanion.feature.exercises

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDisconnectReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSessionId
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionTransportSession
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionId
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionRepository
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionState
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationAcquireResult
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationGate
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationOwner
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationPermit
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationRequest
import br.com.mwconsulting.championcompanion.core.common.BleDispatcher
import br.com.mwconsulting.championcompanion.feature.device.DeviceConnectionBootstrap
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class WatchWorkoutParticipationStatus {
    NOT_ENABLED,
    NO_ASSOCIATED_DEVICE,
    ASSOCIATED,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    CONFLICT,
}

enum class WatchWorkoutSportModeStatus {
    NOT_CONFIRMED,
}

data class WatchWorkoutParticipationState(
    val sessionId: AppWorkoutSessionId? = null,
    val status: WatchWorkoutParticipationStatus = WatchWorkoutParticipationStatus.NOT_ENABLED,
    val sportMode: WatchWorkoutSportModeStatus = WatchWorkoutSportModeStatus.NOT_CONFIRMED,
    val conflictOwner: ForegroundBleOperationOwner? = null,
    val participationEnabled: Boolean = false,
)

/**
 * Participação opcional do relógio durante sessão phone.
 *
 * Este coordenador observa transporte e associação, mas não interpreta FEA1/F002
 * nem inicia qualquer comando de esporte. A ausência de protocolo temporal live
 * mantém BPM e passos fora dos agregados da sessão.
 */
@Singleton
class WatchWorkoutParticipationCoordinator @Inject constructor(
    private val connectionManager: ChampionConnectionManager,
    private val connectionBootstrap: DeviceConnectionBootstrap,
    private val deviceRepository: DeviceRepository,
    private val appWorkoutSessionRepository: AppWorkoutSessionRepository,
    private val foregroundBleOperationGate: ForegroundBleOperationGate,
    @BleDispatcher bleDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + bleDispatcher)
    private val operationMutex = Mutex()
    private val mutableState = MutableStateFlow(WatchWorkoutParticipationState())

    private var associatedDevice: Device? = null
    private var boundSessionId: AppWorkoutSessionId? = null

    @Volatile
    private var enabledSessionId: AppWorkoutSessionId? = null
    private var acceptedTransportSessionId: ChampionSessionId? = null
    private var pendingTransportSessionId: ChampionSessionId? = null
    private var permit: ForegroundBleOperationPermit? = null
    private var terminalObserverJob: Job? = null

    @Volatile
    private var foregroundBleSessionId: AppWorkoutSessionId? = null

    val state: StateFlow<WatchWorkoutParticipationState> = mutableState.asStateFlow()

    init {
        scope.launch {
            deviceRepository.observeAssociatedDevice().collect { device ->
                operationMutex.withLock {
                    associatedDevice = device
                    refreshBoundStateLocked()
                }
            }
        }
        scope.launch {
            connectionManager.state.collect { transportState ->
                operationMutex.withLock {
                    updateFromTransportStateLocked(transportState)
                }
            }
        }
    }

    /** Vincula superfície de status à sessão sem adquirir BLE. */
    fun bindSession(sessionId: AppWorkoutSessionId) {
        scope.launch {
            operationMutex.withLock {
                bindSessionLocked(sessionId)
                refreshBoundStateLocked()
            }
        }
    }

    /**
     * Adquire owner somente após ação explícita do usuário. Uma tentativa de
     * conexão é feita no máximo uma vez por chamada; retry exige nova ação.
     */
    suspend fun enable(sessionId: AppWorkoutSessionId): Boolean {
        val device = try {
            deviceRepository.observeAssociatedDevice().first()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }

        var attemptPermit: ForegroundBleOperationPermit? = null
        var permitAcquiredByAttempt: ForegroundBleOperationPermit? = null
        var connectionAttemptStarted = false
        var returnedTransport: ChampionTransportSession? = null

        try {
            val shouldConnect = operationMutex.withLock {
                if (!bindSessionLocked(sessionId)) return@withLock false
                associatedDevice = device
                if (device == null) {
                    mutableState.value = WatchWorkoutParticipationState(
                        sessionId = sessionId,
                        status = WatchWorkoutParticipationStatus.NO_ASSOCIATED_DEVICE,
                    )
                    foregroundBleSessionId = null
                    return@withLock false
                }

                if (enabledSessionId == null) {
                    when (
                        val result = foregroundBleOperationGate.tryAcquire(
                            ForegroundBleOperationRequest(
                                owner = ForegroundBleOperationOwner.APP_WORKOUT_SESSION,
                                operationId = sessionId.value.toString(),
                                requestedAt = Instant.now(),
                            ),
                        )
                    ) {
                        is ForegroundBleOperationAcquireResult.Acquired -> {
                            attemptPermit = result.permit
                            permitAcquiredByAttempt = result.permit
                            permit = result.permit
                            enabledSessionId = sessionId
                        }

                        is ForegroundBleOperationAcquireResult.Rejected -> {
                            mutableState.value = WatchWorkoutParticipationState(
                                sessionId = sessionId,
                                status = WatchWorkoutParticipationStatus.CONFLICT,
                                conflictOwner = result.activeOperation.owner,
                            )
                            foregroundBleSessionId = null
                            return@withLock false
                        }
                    }
                } else if (enabledSessionId != sessionId) {
                    return@withLock false
                } else {
                    attemptPermit = permit ?: return@withLock false
                }

                if (mutableState.value.status == WatchWorkoutParticipationStatus.CONNECTING) {
                    return@withLock false
                }
                true
            }
            if (!shouldConnect) return false
            val connectedDevice = device ?: return false

            val current = connectionManager.currentSession()
            if (current != null && current.session.transportKey == connectedDevice.platformIdentifier) {
                return operationMutex.withLock {
                    if (enabledSessionId != sessionId || permit != attemptPermit) return@withLock false
                    acceptedTransportSessionId = current.session.id
                    pendingTransportSessionId = null
                    setConnectedLocked(sessionId)
                    true
                }
            }

            val started = operationMutex.withLock {
                if (enabledSessionId != sessionId || permit != attemptPermit) return@withLock false
                acceptedTransportSessionId = null
                pendingTransportSessionId = null
                connectionAttemptStarted = true
                setConnectingLocked(sessionId)
                true
            }
            if (!started) return false

            val result = try {
                connectionBootstrap.connectAssociatedDevice(
                    device = connectedDevice.toDiscoveredDevice(),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
            if (result is ChampionBleResult.Success) {
                returnedTransport = result.value.transport
            }
            return operationMutex.withLock {
                if (enabledSessionId != sessionId || permit != attemptPermit) return@withLock false
                when {
                    result is ChampionBleResult.Success &&
                        result.value.transport.session.transportKey == connectedDevice.platformIdentifier -> {
                        acceptedTransportSessionId = result.value.transport.session.id
                        pendingTransportSessionId = null
                        setConnectedLocked(sessionId)
                        true
                    }

                    else -> {
                        setDisconnectedLocked(sessionId)
                        false
                    }
                }
            }
        } catch (error: CancellationException) {
            cleanupCancelledEnable(
                sessionId = sessionId,
                expectedPermit = attemptPermit,
                permitAcquiredByAttempt = permitAcquiredByAttempt,
                expectedTransportKey = device?.platformIdentifier,
                connectionAttemptStarted = connectionAttemptStarted,
                returnedTransport = returnedTransport,
            )
            throw error
        }
    }

    private suspend fun cleanupCancelledEnable(
        sessionId: AppWorkoutSessionId,
        expectedPermit: ForegroundBleOperationPermit?,
        permitAcquiredByAttempt: ForegroundBleOperationPermit?,
        expectedTransportKey: String?,
        connectionAttemptStarted: Boolean,
        returnedTransport: ChampionTransportSession?,
    ) {
        withContext(NonCancellable) {
            var permitToRelease: ForegroundBleOperationPermit? = null
            operationMutex.withLock {
                if (expectedPermit == null ||
                    enabledSessionId != sessionId ||
                    permit != expectedPermit
                ) {
                    return@withLock
                }

                val currentTransport = if (connectionAttemptStarted) {
                    try {
                        connectionManager.currentSession()
                    } catch (_: CancellationException) {
                        null
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    null
                }
                val expectedTransportSessionId = returnedTransport?.session?.id
                    ?: pendingTransportSessionId
                    ?: acceptedTransportSessionId
                    ?: expectedTransportKey?.let {
                        connectionManager.state.value.transportSessionIdFor(it)
                    }
                val currentStateSessionId = expectedTransportKey?.let {
                    connectionManager.state.value.transportSessionIdFor(it)
                }
                val canDisconnectExpectedTransport = connectionAttemptStarted &&
                    expectedTransportKey != null &&
                    expectedTransportSessionId != null &&
                    currentStateSessionId == expectedTransportSessionId &&
                    currentTransport != null &&
                    currentTransport.session.transportKey == expectedTransportKey &&
                    currentTransport.session.id == expectedTransportSessionId &&
                    (returnedTransport == null || currentTransport === returnedTransport)

                try {
                    if (canDisconnectExpectedTransport) {
                        connectionManager.disconnect(ChampionDisconnectReason.CLEANUP)
                    }
                } catch (_: CancellationException) {
                    // O cancelamento original será relançado após cleanup identitário.
                } catch (_: Exception) {
                    // Estado e owner ainda precisam ser restaurados mesmo sem teardown BLE.
                } finally {
                    setDisconnectedLocked(sessionId)
                    if (permitAcquiredByAttempt != null && permit == permitAcquiredByAttempt) {
                        permit = null
                        enabledSessionId = null
                        permitToRelease = permitAcquiredByAttempt
                    }
                }
            }
            permitToRelease?.let { foregroundBleOperationGate.release(it) }
        }
    }

    /** Owner permanece retido em desconexão e sai em recuperação persistida ou estado terminal. */
    suspend fun releaseAtTerminal(sessionId: AppWorkoutSessionId) {
        releaseAtInactiveState(
            sessionId = sessionId,
            allowedStates = TERMINAL_STATES,
        )
    }

    /** Interrupção confirmada abre recuperação; owner não pode vazar nesse estado. */
    suspend fun releaseAtRecovery(sessionId: AppWorkoutSessionId) {
        releaseAtInactiveState(
            sessionId = sessionId,
            allowedStates = setOf(AppWorkoutSessionState.AWAITING_RECOVERY),
        )
    }

    private suspend fun releaseAtInactiveState(
        sessionId: AppWorkoutSessionId,
        allowedStates: Set<AppWorkoutSessionState>,
    ) {
        val canRelease = try {
            appWorkoutSessionRepository.observeSession(sessionId).first()?.state in allowedStates
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            false
        }
        if (!canRelease) return

        val permitToRelease = operationMutex.withLock {
            if (enabledSessionId != sessionId && boundSessionId != sessionId) return@withLock null
            terminalObserverJob?.cancel()
            terminalObserverJob = null
            foregroundBleSessionId = null
            enabledSessionId = null
            acceptedTransportSessionId = null
            pendingTransportSessionId = null
            boundSessionId = boundSessionId?.takeUnless { it == sessionId }
            mutableState.value = WatchWorkoutParticipationState()
            permit.also { permit = null }
        }
        permitToRelease?.let { foregroundBleOperationGate.release(it) }
    }

    fun requiresConnectedDeviceForegroundType(sessionId: AppWorkoutSessionId): Boolean =
        foregroundBleSessionId == sessionId

    private fun bindSessionLocked(sessionId: AppWorkoutSessionId): Boolean {
        val current = boundSessionId
        if (current != null && current != sessionId) return false
        if (current == null) {
            boundSessionId = sessionId
            terminalObserverJob?.cancel()
            terminalObserverJob = scope.launch {
                appWorkoutSessionRepository.observeSession(sessionId).collect { session ->
                    if (session?.state in RELEASE_OWNER_STATES
                    ) {
                        scope.launch {
                            releaseAtInactiveState(
                                sessionId = sessionId,
                                allowedStates = RELEASE_OWNER_STATES,
                            )
                        }
                    }
                }
            }
        }
        return true
    }

    private fun refreshBoundStateLocked() {
        val sessionId = boundSessionId ?: return
        val device = associatedDevice
        if (device == null) {
            mutableState.value = WatchWorkoutParticipationState(
                sessionId = sessionId,
                status = WatchWorkoutParticipationStatus.NO_ASSOCIATED_DEVICE,
            )
            acceptedTransportSessionId = null
            pendingTransportSessionId = null
            foregroundBleSessionId = null
            return
        }

        if (enabledSessionId == sessionId) {
            when (mutableState.value.status) {
                WatchWorkoutParticipationStatus.CONNECTING,
                WatchWorkoutParticipationStatus.DISCONNECTED,
                WatchWorkoutParticipationStatus.CONFLICT,
                -> return

                else -> Unit
            }
        }

        val readyTransport = transportReadyFor(device)
        if (readyTransport != null) {
            if (enabledSessionId == sessionId) {
                if (acceptedTransportSessionId == null) {
                    acceptedTransportSessionId = readyTransport.id
                }
                if (acceptedTransportSessionId == readyTransport.id) {
                    setConnectedLocked(sessionId)
                } else {
                    setDisconnectedLocked(sessionId)
                }
            } else {
                mutableState.value = WatchWorkoutParticipationState(
                    sessionId = sessionId,
                    status = WatchWorkoutParticipationStatus.ASSOCIATED,
                )
            }
        } else {
            mutableState.value = WatchWorkoutParticipationState(
                sessionId = sessionId,
                status = WatchWorkoutParticipationStatus.ASSOCIATED,
            )
            acceptedTransportSessionId = null
            pendingTransportSessionId = null
            foregroundBleSessionId = null
        }
    }

    private fun updateFromTransportStateLocked(state: ChampionBleState) {
        if (connectionManager.state.value != state) return
        val sessionId = enabledSessionId ?: return
        val device = associatedDevice ?: run {
            setNoAssociatedDeviceLocked(sessionId)
            return
        }
        when (state) {
            is ChampionBleState.TransportReady -> {
                if (state.session.transportKey != device.platformIdentifier ||
                    !isTrackedTransportLocked(state.session.id)
                ) {
                    return
                }
                if (state.session.transportKey == device.platformIdentifier) {
                    acceptedTransportSessionId = state.session.id
                    pendingTransportSessionId = null
                    setConnectedLocked(sessionId)
                }
            }

            is ChampionBleState.Connecting,
            is ChampionBleState.DiscoveringServices,
            is ChampionBleState.ResolvingChannels,
            is ChampionBleState.ConfiguringSubscriptions,
            -> {
                if (state.connectionTransportKeyOrNull() != device.platformIdentifier) return
                val transportSessionId = state.connectionSessionIdOrNull() ?: return
                if (acceptedTransportSessionId != null &&
                    acceptedTransportSessionId != transportSessionId
                ) return
                if (pendingTransportSessionId != null &&
                    pendingTransportSessionId != transportSessionId
                ) return
                pendingTransportSessionId = transportSessionId
                setConnectingLocked(sessionId)
            }

            is ChampionBleState.Disconnecting -> {
                val transportSessionId = state.session?.id ?: return
                if (isTrackedTransportLocked(transportSessionId)) {
                    setDisconnectedLocked(sessionId)
                }
            }

            is ChampionBleState.Failed -> {
                val transportSessionId = state.session?.id ?: return
                if (isTrackedTransportLocked(transportSessionId)) {
                    setDisconnectedLocked(sessionId)
                }
            }

            ChampionBleState.Disconnected -> Unit

            is ChampionBleState.Scanning -> Unit
        }
    }

    private fun setNoAssociatedDeviceLocked(sessionId: AppWorkoutSessionId) {
        mutableState.value = WatchWorkoutParticipationState(
            sessionId = sessionId,
            status = WatchWorkoutParticipationStatus.NO_ASSOCIATED_DEVICE,
        )
        acceptedTransportSessionId = null
        pendingTransportSessionId = null
        foregroundBleSessionId = null
    }

    private fun setConnectingLocked(sessionId: AppWorkoutSessionId) {
        mutableState.value = WatchWorkoutParticipationState(
            sessionId = sessionId,
            status = WatchWorkoutParticipationStatus.CONNECTING,
            participationEnabled = true,
        )
        foregroundBleSessionId = null
    }

    private fun setConnectedLocked(sessionId: AppWorkoutSessionId) {
        mutableState.value = WatchWorkoutParticipationState(
            sessionId = sessionId,
            status = WatchWorkoutParticipationStatus.CONNECTED,
            sportMode = WatchWorkoutSportModeStatus.NOT_CONFIRMED,
            participationEnabled = true,
        )
        foregroundBleSessionId = sessionId
    }

    private fun setDisconnectedLocked(sessionId: AppWorkoutSessionId) {
        mutableState.value = WatchWorkoutParticipationState(
            sessionId = sessionId,
            status = WatchWorkoutParticipationStatus.DISCONNECTED,
            participationEnabled = true,
        )
        acceptedTransportSessionId = null
        pendingTransportSessionId = null
        foregroundBleSessionId = null
    }

    private fun transportReadyFor(device: Device): br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSession? =
        (connectionManager.state.value as? ChampionBleState.TransportReady)
            ?.session
            ?.takeIf { it.transportKey == device.platformIdentifier }

    private fun isTrackedTransportLocked(sessionId: ChampionSessionId): Boolean =
        acceptedTransportSessionId == sessionId || pendingTransportSessionId == sessionId

    private fun ChampionBleState.connectionTransportKeyOrNull(): String? = when (this) {
        is ChampionBleState.Connecting -> session.transportKey
        is ChampionBleState.DiscoveringServices -> session.transportKey
        is ChampionBleState.ResolvingChannels -> session.transportKey
        is ChampionBleState.ConfiguringSubscriptions -> session.transportKey
        else -> null
    }

    private fun ChampionBleState.connectionSessionIdOrNull(): ChampionSessionId? = when (this) {
        is ChampionBleState.Connecting -> session.id
        is ChampionBleState.DiscoveringServices -> session.id
        is ChampionBleState.ResolvingChannels -> session.id
        is ChampionBleState.ConfiguringSubscriptions -> session.id
        else -> null
    }

    private fun ChampionBleState.transportSessionIdFor(transportKey: String): ChampionSessionId? = when (this) {
        is ChampionBleState.Connecting -> session.takeIf { it.transportKey == transportKey }?.id
        is ChampionBleState.DiscoveringServices -> session.takeIf { it.transportKey == transportKey }?.id
        is ChampionBleState.ResolvingChannels -> session.takeIf { it.transportKey == transportKey }?.id
        is ChampionBleState.ConfiguringSubscriptions -> session.takeIf { it.transportKey == transportKey }?.id
        is ChampionBleState.TransportReady -> session.takeIf { it.transportKey == transportKey }?.id
        is ChampionBleState.Disconnecting -> session?.takeIf { it.transportKey == transportKey }?.id
        is ChampionBleState.Failed -> session?.takeIf { it.transportKey == transportKey }?.id
        ChampionBleState.Disconnected,
        is ChampionBleState.Scanning,
        -> null
    }

    private fun Device.toDiscoveredDevice() = ChampionDiscoveredDevice(
        transportKey = platformIdentifier,
        name = name,
        localName = name,
        observedAt = Instant.now(),
    )

    private companion object {
        val TERMINAL_STATES = setOf(
            AppWorkoutSessionState.FINISHED,
            AppWorkoutSessionState.DISCARDED,
        )
        val RELEASE_OWNER_STATES = TERMINAL_STATES + AppWorkoutSessionState.AWAITING_RECOVERY
    }
}
