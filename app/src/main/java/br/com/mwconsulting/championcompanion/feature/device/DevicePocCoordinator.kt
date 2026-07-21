package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDisconnectReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCancellationReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionScanRequest
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSessionId
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionTransportSession
import br.com.mwconsulting.championcompanion.core.common.BleDispatcher
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration

class DevicePocCoordinator @Inject constructor(
    private val connectionManager: ChampionConnectionManager,
    @BleDispatcher bleDispatcher: CoroutineDispatcher,
) : DeviceFeatureContract {
    private val scope = CoroutineScope(SupervisorJob() + bleDispatcher)
    private val mutableState = MutableStateFlow(DeviceUiState())

    override val state: StateFlow<DeviceUiState> = mutableState.asStateFlow()

    init {
        scope.launch {
            connectionManager.state.collect(::handleTransportStateUpdate)
        }
    }

    fun markPermissionsRequired() {
        updateState {
            it.copy(
                pocState = DevicePocState.PermissionsRequired,
                lastError = null,
            )
        }
    }

    fun markReadyToScan() {
        updateState {
            it.copy(
                pocState = DevicePocState.ReadyToScan,
                lastError = null,
            )
        }
    }

    suspend fun scan(request: ChampionScanRequest): ChampionBleResult<List<ChampionDiscoveredDevice>> {
        if (state.value.pocState !in setOf(DevicePocState.ReadyToScan, DevicePocState.DeviceFound)) {
            return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        }
        val activeCycleId = state.value.cycleId
        updateState {
            it.copy(
                pocState = DevicePocState.Scanning,
                candidate = null,
                lastError = null,
            )
        }

        return when (val result = connectionManager.scan(request)) {
            is ChampionBleResult.Failure -> {
                if (state.value.cycleId == activeCycleId) {
                    transitionToFailure(null, result.error)
                }
                result
            }

            is ChampionBleResult.Success -> {
                if (state.value.cycleId != activeCycleId) return result
                val candidate = result.value.firstOrNull()
                updateState {
                    it.copy(
                        pocState = if (candidate != null) DevicePocState.DeviceFound else DevicePocState.ReadyToScan,
                        candidate = candidate,
                        lastError = null,
                    )
                }
                result
            }
        }
    }

    suspend fun connect(device: ChampionDiscoveredDevice): ChampionBleResult<ChampionTransportSession> {
        if (state.value.pocState !in setOf(DevicePocState.DeviceFound, DevicePocState.ReadyToScan)) {
            return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        }
        val activeCycleId = state.value.cycleId
        updateState {
            it.copy(
                pocState = DevicePocState.Connecting,
                candidate = device,
                lastError = null,
            )
        }

        return when (val result = connectionManager.connect(device)) {
            is ChampionBleResult.Failure -> {
                if (state.value.cycleId == activeCycleId) {
                    transitionToFailure(null, result.error)
                }
                result
            }

            is ChampionBleResult.Success -> result
        }
    }

    suspend fun disconnect(
        reason: ChampionDisconnectReason = ChampionDisconnectReason.USER_REQUESTED,
    ): ChampionBleResult<Unit> {
        updateState {
            it.copy(
                pocState = DevicePocState.Disconnecting,
                lastError = null,
            )
        }
        val result = connectionManager.disconnect(reason)
        if (result is ChampionBleResult.Failure) {
            transitionToFailure(state.value.activeSessionId, result.error)
        } else {
            clearCycleState(DevicePocState.Idle)
        }
        return result
    }

    suspend fun resetCycle(): ChampionBleResult<Unit> {
        val nextCycleId = DevicePocCycleId.random()
        mutableState.value = DeviceUiState(cycleId = nextCycleId)
        return connectionManager.disconnect(ChampionDisconnectReason.CLEANUP)
    }

    suspend fun currentTransportSession(): ChampionTransportSession? = connectionManager.currentSession()

    suspend fun executeHandshake(): ChampionBleResult<Unit> = executeHandshake(ZonedDateTime.now())

    internal suspend fun executeHandshake(
        dateTime: ZonedDateTime,
        timeout: Duration = DevicePocHandshakeA1.defaultTimeout,
    ): ChampionBleResult<Unit> {
        val current = state.value
        val sessionId = current.activeSessionId ?: return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        if (current.pocState != DevicePocState.TransportReady) {
            return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        }
        val transport = currentTransportSession()
            ?: return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        if (transport.session.id != sessionId) {
            return ChampionBleResult.Failure(ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP))
        }

        moveToHandshaking(sessionId)
        val result = transport.executeCommand(
            DevicePocHandshakeA1.createCommand(
                dateTime = dateTime,
                timeout = timeout,
            ),
        )
        val latest = state.value
        if (latest.activeSessionId != sessionId) {
            return ChampionBleResult.Failure(ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP))
        }

        return when (result) {
            is ChampionBleResult.Success -> {
                moveToHandshakeConfirmed(sessionId)
                ChampionBleResult.Success(Unit)
            }

            is ChampionBleResult.Failure -> {
                transitionToFailure(sessionId, result.error)
                ChampionBleResult.Failure(result.error)
            }
        }
    }

    internal suspend fun handleTransportStateUpdate(transportState: ChampionBleState) {
        val current = state.value
        when (transportState) {
            ChampionBleState.Disconnected -> {
                if (current.pocState == DevicePocState.Disconnecting || current.activeSessionId != null) {
                    clearCycleState(DevicePocState.Idle)
                } else {
                    updateState { it.copy(transportState = transportState) }
                }
            }

            is ChampionBleState.Scanning -> {
                updateState {
                    it.copy(
                        transportState = transportState,
                        pocState = DevicePocState.Scanning,
                    )
                }
            }

            is ChampionBleState.Connecting -> {
                if (current.pocState != DevicePocState.Connecting) return
                val currentCandidate = current.candidate ?: return
                if (currentCandidate.transportKey != transportState.device.transportKey) return
                updateState {
                    it.copy(
                        transportState = transportState,
                        activeSessionId = transportState.session.id,
                    )
                }
            }

            is ChampionBleState.DiscoveringServices,
            is ChampionBleState.ResolvingChannels,
            is ChampionBleState.ConfiguringSubscriptions,
            -> {
                val sessionId = transportState.sessionIdOrNull()
                if (sessionId == null || sessionId != current.activeSessionId) return
                updateState { it.copy(transportState = transportState) }
            }

            is ChampionBleState.TransportReady -> {
                if (transportState.session.id != current.activeSessionId) return
                updateState {
                    it.copy(
                        transportState = transportState,
                        pocState = DevicePocState.TransportReady,
                        lastError = null,
                    )
                }
            }

            is ChampionBleState.Disconnecting -> {
                val sessionId = transportState.session?.id
                if (sessionId != null && sessionId != current.activeSessionId) return
                updateState {
                    it.copy(
                        transportState = transportState,
                        pocState = DevicePocState.Disconnecting,
                    )
                }
            }

            is ChampionBleState.Failed -> {
                val sessionId = transportState.session?.id
                if (sessionId != null && sessionId != current.activeSessionId) return
                transitionToFailure(sessionId, transportState.error)
            }
        }
    }

    internal fun moveToHandshaking(sessionId: ChampionSessionId): Boolean = moveProtocolState(
        sessionId = sessionId,
        nextState = DevicePocState.Handshaking,
    )

    internal fun moveToHandshakeConfirmed(sessionId: ChampionSessionId): Boolean = moveProtocolState(
        sessionId = sessionId,
        nextState = DevicePocState.HandshakeConfirmed,
    )

    internal fun moveToWaitingForSteps(sessionId: ChampionSessionId): Boolean = moveProtocolState(
        sessionId = sessionId,
        nextState = DevicePocState.WaitingForSteps,
    )

    internal fun moveToStepsObserved(sessionId: ChampionSessionId): Boolean = moveProtocolState(
        sessionId = sessionId,
        nextState = DevicePocState.StepsObserved,
    )

    private fun moveProtocolState(
        sessionId: ChampionSessionId,
        nextState: DevicePocState,
    ): Boolean {
        val current = state.value
        if (current.activeSessionId != sessionId) return false
        updateState {
            it.copy(
                pocState = nextState,
                lastError = null,
            )
        }
        return true
    }

    private fun clearCycleState(nextState: DevicePocState) {
        updateState {
            it.copy(
                transportState = ChampionBleState.Disconnected,
                pocState = nextState,
                candidate = null,
                activeSessionId = null,
                lastError = null,
            )
        }
    }

    private fun transitionToFailure(
        sessionId: ChampionSessionId?,
        error: ChampionBleError,
    ) {
        updateState {
            it.copy(
                transportState = ChampionBleState.Failed(
                    session = sessionId?.let(::placeholderSession),
                    error = error,
                ),
                pocState = DevicePocState.Failed(error),
                lastError = error,
            )
        }
    }

    private fun updateState(transform: (DeviceUiState) -> DeviceUiState) {
        mutableState.value = transform(mutableState.value)
    }

    private fun ChampionBleState.sessionIdOrNull(): ChampionSessionId? = when (this) {
        is ChampionBleState.Connecting -> session.id
        is ChampionBleState.DiscoveringServices -> session.id
        is ChampionBleState.ResolvingChannels -> session.id
        is ChampionBleState.ConfiguringSubscriptions -> session.id
        is ChampionBleState.TransportReady -> session.id
        is ChampionBleState.Disconnecting -> session?.id
        is ChampionBleState.Failed -> session?.id
        ChampionBleState.Disconnected,
        is ChampionBleState.Scanning,
        -> null
    }

    private fun placeholderSession(sessionId: ChampionSessionId) =
        br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSession(
            id = sessionId,
            transportKey = "poc-${sessionId.value}",
            connectedAt = java.time.Instant.EPOCH,
        )
}
