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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
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
    private var invalidTransportSessionId: ChampionSessionId? = null
    private var permit: ForegroundBleOperationPermit? = null
    private var connectionAttemptSessionId: AppWorkoutSessionId? = null
    private var connectionAttemptToken: ConnectionAttemptToken? = null
    private val connectionAttemptTombstones = mutableListOf<ConnectionAttemptToken>()
    private var terminalObserverJob: Job? = null

    @Volatile
    private var foregroundBleSessionId: AppWorkoutSessionId? = null

    private enum class EnablePreparation {
        START,
        ALREADY_CONNECTED,
        IN_PROGRESS,
        REJECTED,
    }

    private enum class RetryPreparation {
        START,
        ENABLE,
        ALREADY_CONNECTED,
        IN_PROGRESS,
        REJECTED,
    }

    private enum class ConnectionAttemptStart {
        STARTED,
        ALREADY_CONNECTED,
        IN_PROGRESS,
        REJECTED,
    }

    /** Identidade da tentativa, independente do owner ainda estar presente. */
    private data class ConnectionAttemptToken(
        val sessionId: AppWorkoutSessionId,
        val expectedPermit: ForegroundBleOperationPermit,
        val operationId: String,
        val transportKey: String,
        val job: Job?,
        var transportSessionId: ChampionSessionId? = null,
        var transport: ChampionTransportSession? = null,
        var transportIdentityInvalid: Boolean = false,
    )

    val state: StateFlow<WatchWorkoutParticipationState> = mutableState.asStateFlow()

    init {
        scope.launch {
            deviceRepository.observeAssociatedDevice().collect { device ->
                operationMutex.withLock {
                    associatedDevice = device
                    reconcileBoundStateLocked()
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
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            operationMutex.withLock {
                if (canBindSessionLocked(sessionId) && bindSessionLocked(sessionId)) {
                    reconcileBoundStateLocked(sessionId)
                }
            }
        }
    }

    /**
     * Adquire owner somente após ação explícita do usuário. Uma tentativa de
     * conexão é feita no máximo uma vez por chamada.
     */
    suspend fun enable(sessionId: AppWorkoutSessionId): Boolean {
        val device = loadAssociatedDevice()
        var attemptPermit: ForegroundBleOperationPermit? = null
        var permitAcquiredByAttempt: ForegroundBleOperationPermit? = null

        try {
            val preparation = operationMutex.withLock {
                if (!canBindSessionLocked(sessionId)) return@withLock EnablePreparation.REJECTED
                if (!bindSessionLocked(sessionId)) return@withLock EnablePreparation.REJECTED
                associatedDevice = device
                reconcileBoundStateLocked(sessionId)
                if (device == null) {
                    setNoAssociatedDeviceLocked(sessionId)
                    return@withLock EnablePreparation.REJECTED
                }

                if (enabledSessionId == sessionId && permitBelongsToSessionLocked(sessionId)) {
                    return@withLock when (mutableState.value.status) {
                        WatchWorkoutParticipationStatus.CONNECTED -> EnablePreparation.ALREADY_CONNECTED
                        WatchWorkoutParticipationStatus.CONNECTING -> EnablePreparation.IN_PROGRESS
                        else -> EnablePreparation.REJECTED
                    }
                }
                connectionAttemptToken?.let { attempt ->
                    return@withLock if (attempt.sessionId == sessionId) {
                        EnablePreparation.IN_PROGRESS
                    } else {
                        EnablePreparation.REJECTED
                    }
                }
                if (enabledSessionId != null) return@withLock EnablePreparation.REJECTED

                if (!canBindSessionLocked(sessionId)) return@withLock EnablePreparation.REJECTED
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
                        return@withLock EnablePreparation.REJECTED
                    }
                }

                reconcileBoundStateLocked(sessionId = sessionId)
                when (mutableState.value.status) {
                    WatchWorkoutParticipationStatus.CONNECTED -> EnablePreparation.ALREADY_CONNECTED
                    WatchWorkoutParticipationStatus.CONNECTING -> EnablePreparation.IN_PROGRESS
                    else -> EnablePreparation.START
                }
            }

            when (preparation) {
                EnablePreparation.ALREADY_CONNECTED -> return true
                EnablePreparation.IN_PROGRESS,
                EnablePreparation.REJECTED,
                -> return false
                EnablePreparation.START -> Unit
            }
            val expectedPermit = attemptPermit ?: return false
            val connectedDevice = device ?: return false
            return attemptConnection(
                sessionId = sessionId,
                device = connectedDevice,
                expectedPermit = expectedPermit,
                permitAcquiredByAttempt = permitAcquiredByAttempt,
                releaseExpectedPermitOnFailure = true,
            )
        } catch (error: CancellationException) {
            cleanupEnableAttempt(
                sessionId = sessionId,
                expectedPermit = attemptPermit,
                permitAcquiredByAttempt = permitAcquiredByAttempt,
                expectedTransportKey = device?.platformIdentifier,
                connectionAttemptStarted = false,
                returnedTransport = null,
                releaseExpectedPermit = false,
            )
            throw error
        } catch (_: Exception) {
            cleanupEnableAttempt(
                sessionId = sessionId,
                expectedPermit = attemptPermit,
                permitAcquiredByAttempt = permitAcquiredByAttempt,
                expectedTransportKey = device?.platformIdentifier,
                connectionAttemptStarted = false,
                returnedTransport = null,
                releaseExpectedPermit = true,
            )
            return false
        }
    }

    /**
     * Reconciles the persisted participation owner before one explicit retry.
     * Retry reuses APP_WORKOUT_SESSION when already enabled; it never acquires
     * DEVICE_CONNECTION and never releases the workout owner on disconnect.
     */
    suspend fun retry(sessionId: AppWorkoutSessionId): Boolean {
        val device = loadAssociatedDevice()
        var expectedPermit: ForegroundBleOperationPermit? = null

        val preparation = operationMutex.withLock {
            if (!canBindSessionLocked(sessionId)) return@withLock RetryPreparation.REJECTED
            if (!bindSessionLocked(sessionId)) return@withLock RetryPreparation.REJECTED
            associatedDevice = device
            reconcileBoundStateLocked(sessionId)

            if (device == null) {
                if (!participationEnabledLocked(sessionId)) setNoAssociatedDeviceLocked(sessionId)
                return@withLock RetryPreparation.REJECTED
            }
            connectionAttemptToken?.let { attempt ->
                return@withLock if (attempt.sessionId == sessionId) {
                    RetryPreparation.IN_PROGRESS
                } else {
                    RetryPreparation.REJECTED
                }
            }
            if (!participationEnabledLocked(sessionId)) {
                return@withLock RetryPreparation.ENABLE
            }
            if (!canBindSessionLocked(sessionId)) return@withLock RetryPreparation.REJECTED

            expectedPermit = permit
            when (mutableState.value.status) {
                WatchWorkoutParticipationStatus.CONNECTED -> RetryPreparation.ALREADY_CONNECTED
                WatchWorkoutParticipationStatus.CONNECTING -> RetryPreparation.IN_PROGRESS
                else -> RetryPreparation.START
            }
        }

        val connectedDevice = device ?: return false
        return when (preparation) {
            RetryPreparation.ALREADY_CONNECTED -> true
            RetryPreparation.ENABLE -> enable(sessionId)
            RetryPreparation.IN_PROGRESS,
            RetryPreparation.REJECTED,
            -> false
            RetryPreparation.START -> {
                val permitForAttempt = expectedPermit ?: return false
                attemptConnection(
                    sessionId = sessionId,
                    device = connectedDevice,
                    expectedPermit = permitForAttempt,
                    permitAcquiredByAttempt = null,
                    releaseExpectedPermitOnFailure = false,
                )
            }
        }
    }

    private suspend fun attemptConnection(
        sessionId: AppWorkoutSessionId,
        device: Device,
        expectedPermit: ForegroundBleOperationPermit,
        permitAcquiredByAttempt: ForegroundBleOperationPermit?,
        releaseExpectedPermitOnFailure: Boolean,
    ): Boolean {
        val attemptJob = currentCoroutineContext()[Job]
        var connectionAttemptStarted = false
        var returnedTransport: ChampionTransportSession? = null
        var attemptToken: ConnectionAttemptToken? = null
        try {
            val start = operationMutex.withLock {
                if (!canBindSessionLocked(sessionId)) {
                    return@withLock ConnectionAttemptStart.REJECTED
                }
                if (!participationEnabledLocked(sessionId) || permit != expectedPermit) {
                    return@withLock ConnectionAttemptStart.REJECTED
                }
                if (connectionAttemptToken != null) {
                    return@withLock ConnectionAttemptStart.REJECTED
                }
                associatedDevice = device
                reconcileBoundStateLocked(sessionId)
                when (mutableState.value.status) {
                    WatchWorkoutParticipationStatus.CONNECTED -> ConnectionAttemptStart.ALREADY_CONNECTED
                    WatchWorkoutParticipationStatus.CONNECTING -> ConnectionAttemptStart.IN_PROGRESS
                    else -> {
                        acceptedTransportSessionId = null
                        pendingTransportSessionId = null
                        val token = ConnectionAttemptToken(
                            sessionId = sessionId,
                            expectedPermit = expectedPermit,
                            operationId = expectedPermit.operationId,
                            transportKey = device.platformIdentifier,
                            job = attemptJob,
                        )
                        attemptToken = token
                        connectionAttemptToken = token
                        connectionAttemptSessionId = sessionId
                        connectionAttemptStarted = true
                        setConnectingLocked(sessionId)
                        ConnectionAttemptStart.STARTED
                    }
                }
            }

            when (start) {
                ConnectionAttemptStart.ALREADY_CONNECTED -> return true
                ConnectionAttemptStart.IN_PROGRESS -> return false
                ConnectionAttemptStart.REJECTED -> {
                    if (permitAcquiredByAttempt != null) {
                        cleanupEnableAttempt(
                            sessionId = sessionId,
                            expectedPermit = expectedPermit,
                            permitAcquiredByAttempt = permitAcquiredByAttempt,
                            expectedTransportKey = device.platformIdentifier,
                            connectionAttemptStarted = false,
                            returnedTransport = null,
                            attemptToken = null,
                            releaseExpectedPermit = true,
                        )
                    }
                    return false
                }
                ConnectionAttemptStart.STARTED -> Unit
            }

            val canStartConnection = operationMutex.withLock {
                canBindSessionLocked(sessionId) &&
                    participationEnabledLocked(sessionId) &&
                    permit == expectedPermit &&
                    connectionAttemptToken === attemptToken
            }
            if (!canStartConnection) {
                cleanupEnableAttempt(
                    sessionId = sessionId,
                    expectedPermit = expectedPermit,
                    permitAcquiredByAttempt = permitAcquiredByAttempt,
                    expectedTransportKey = device.platformIdentifier,
                    connectionAttemptStarted = connectionAttemptStarted,
                    returnedTransport = returnedTransport,
                    attemptToken = attemptToken,
                    releaseExpectedPermit = releaseExpectedPermitOnFailure,
                )
                return false
            }

            val result = try {
                connectionBootstrap.connectAssociatedDevice(
                    device = device.toDiscoveredDevice(),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
            if (result is ChampionBleResult.Success) {
                returnedTransport = result.value.transport
            }
            attemptToken?.let { token ->
                captureAttemptTransport(
                    token = token,
                    returnedTransport = returnedTransport,
                )
            }

            val accepted = operationMutex.withLock {
                if (!participationEnabledLocked(sessionId) || permit != expectedPermit) {
                    return@withLock false
                }
                val bootstrapTransport =
                    (result as? ChampionBleResult.Success)?.value?.transport
                        ?: return@withLock false
                val attemptTransport = attemptToken
                val capturedTransportMatchesBootstrap = connectionAttemptToken === attemptTransport &&
                    attemptTransport != null &&
                    !attemptTransport.transportIdentityInvalid &&
                    attemptTransport.transport === bootstrapTransport &&
                    attemptTransport.transportSessionId == bootstrapTransport.session.id &&
                    bootstrapTransport.session.transportKey == device.platformIdentifier
                val currentState = connectionManager.state.value
                val currentTransport = try {
                    connectionManager.currentSession()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    null
                }
                val currentReadyState = currentState as? ChampionBleState.TransportReady
                val currentTransportIsBootstrap = capturedTransportMatchesBootstrap &&
                    currentReadyState?.session?.id == bootstrapTransport.session.id &&
                    currentReadyState?.session?.transportKey == device.platformIdentifier &&
                    currentTransport === bootstrapTransport
                if (currentTransportIsBootstrap && !canBindSessionLocked(sessionId)) {
                    return@withLock false
                }
                if (currentTransportIsBootstrap) {
                    val acceptedAttempt = attemptTransport ?: return@withLock false
                    acceptedTransportSessionId = bootstrapTransport.session.id
                    pendingTransportSessionId = null
                    invalidTransportSessionId = null
                    forgetConnectionAttemptLocked(acceptedAttempt)
                    connectionAttemptSessionId = null
                    setConnectedLocked(sessionId)
                    true
                } else {
                    false
                }
            }
            if (accepted) return true

            cleanupEnableAttempt(
                sessionId = sessionId,
                expectedPermit = expectedPermit,
                permitAcquiredByAttempt = permitAcquiredByAttempt,
                expectedTransportKey = device.platformIdentifier,
                connectionAttemptStarted = connectionAttemptStarted,
                returnedTransport = returnedTransport,
                attemptToken = attemptToken,
                releaseExpectedPermit = releaseExpectedPermitOnFailure,
            )
            return false
        } catch (error: CancellationException) {
            cleanupEnableAttempt(
                sessionId = sessionId,
                expectedPermit = expectedPermit,
                permitAcquiredByAttempt = permitAcquiredByAttempt,
                expectedTransportKey = device.platformIdentifier,
                connectionAttemptStarted = connectionAttemptStarted,
                returnedTransport = returnedTransport,
                attemptToken = attemptToken,
                releaseExpectedPermit = releaseExpectedPermitOnFailure,
            )
            throw error
        } catch (_: Exception) {
            cleanupEnableAttempt(
                sessionId = sessionId,
                expectedPermit = expectedPermit,
                permitAcquiredByAttempt = permitAcquiredByAttempt,
                expectedTransportKey = device.platformIdentifier,
                connectionAttemptStarted = connectionAttemptStarted,
                returnedTransport = returnedTransport,
                attemptToken = attemptToken,
                releaseExpectedPermit = releaseExpectedPermitOnFailure,
            )
            return false
        }
    }

    private suspend fun cleanupEnableAttempt(
        sessionId: AppWorkoutSessionId,
        expectedPermit: ForegroundBleOperationPermit?,
        permitAcquiredByAttempt: ForegroundBleOperationPermit?,
        expectedTransportKey: String?,
        connectionAttemptStarted: Boolean,
        returnedTransport: ChampionTransportSession?,
        attemptToken: ConnectionAttemptToken? = null,
        releaseExpectedPermit: Boolean,
    ) {
        withContext(NonCancellable) {
            var permitToRelease: ForegroundBleOperationPermit? = null
            operationMutex.withLock {
                val cleanupPermit = attemptToken?.expectedPermit ?: expectedPermit
                if (cleanupPermit == null ||
                    cleanupPermit.owner != ForegroundBleOperationOwner.APP_WORKOUT_SESSION ||
                    cleanupPermit.operationId != sessionId.value.toString() ||
                    (attemptToken != null && attemptToken.sessionId != sessionId) ||
                    (expectedPermit != null && expectedPermit != cleanupPermit) ||
                    (permitAcquiredByAttempt != null && permitAcquiredByAttempt != cleanupPermit)
                ) {
                    return@withLock
                }

                val activeOperation = try {
                    foregroundBleOperationGate.observeActiveOperation().first()
                } catch (_: CancellationException) {
                    null
                } catch (_: Exception) {
                    null
                }
                val currentPermit = permit
                val activeOperationMatches = activeOperation?.let {
                    it.owner == cleanupPermit.owner && it.operationId == cleanupPermit.operationId
                } == true
                val localOwnerMatches = enabledSessionId == sessionId && currentPermit == cleanupPermit
                val ownerReleased = currentPermit == null &&
                    enabledSessionId == null &&
                    (activeOperation == null || activeOperationMatches)
                val canTouchAttempt = (localOwnerMatches &&
                    (activeOperation == null || activeOperationMatches)) || ownerReleased
                val tokenStillTracked = attemptToken == null ||
                    connectionAttemptToken === attemptToken ||
                    connectionAttemptTombstones.any { it === attemptToken }
                if (attemptToken != null && !tokenStillTracked) {
                    return@withLock
                }
                if (!canTouchAttempt) {
                    // A different owner may have replaced this attempt. Only
                    // drop the local token; never clear its state or transport.
                    if (attemptToken != null && tokenStillTracked) {
                        forgetConnectionAttemptLocked(attemptToken)
                        if (connectionAttemptSessionId == sessionId) {
                            connectionAttemptSessionId = null
                        }
                    }
                    return@withLock
                }

                val transportKey = attemptToken?.transportKey ?: expectedTransportKey
                val currentState = connectionManager.state.value
                val currentStateSessionId = transportKey?.let {
                    currentState.transportSessionIdFor(it)
                }
                val currentTransport = if (attemptToken != null) {
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
                val currentTransportMatchesState = currentTransport != null &&
                    transportKey != null &&
                    currentStateSessionId != null &&
                    currentTransport.session.transportKey == transportKey &&
                    currentTransport.session.id == currentStateSessionId
                if (attemptToken != null &&
                    !attemptToken.transportIdentityInvalid &&
                    attemptToken.transportSessionId == null &&
                    localOwnerMatches &&
                    currentStateSessionId != null
                ) {
                    attemptToken.transportSessionId = currentStateSessionId
                }
                val currentTransportForAttempt = currentTransport?.takeIf {
                    !((attemptToken?.transportIdentityInvalid) == true) &&
                        currentTransportMatchesState &&
                        (
                            attemptToken?.transportSessionId == it.session.id ||
                                (attemptToken != null && localOwnerMatches &&
                                    attemptToken.transportSessionId == null)
                            )
                }
                val expectedTransport = if (attemptToken?.transportIdentityInvalid == true) {
                    null
                } else {
                    attemptToken?.transport
                        ?.takeIf { it.session.transportKey == transportKey }
                        ?: returnedTransport?.takeIf { it.session.transportKey == transportKey }
                        ?: currentTransportForAttempt
                }
                val expectedTransportSessionId = attemptToken?.transportSessionId
                    ?: expectedTransport?.session?.id
                val canDisconnectExpectedTransport = connectionAttemptStarted &&
                    attemptToken != null &&
                    transportKey != null &&
                    expectedTransportSessionId != null &&
                    expectedTransport != null &&
                    currentStateSessionId == expectedTransportSessionId &&
                    currentTransport != null &&
                    currentTransport.session.transportKey == transportKey &&
                    currentTransport.session.id == expectedTransportSessionId &&
                    currentTransport === expectedTransport

                try {
                    if (canDisconnectExpectedTransport) {
                        invalidTransportSessionId = expectedTransportSessionId
                        connectionManager.disconnectExpectedTransport(
                            expectedTransport,
                            ChampionDisconnectReason.CLEANUP,
                        )
                    }
                } catch (_: CancellationException) {
                    // O cancelamento original será relançado após cleanup identitário.
                } catch (_: Exception) {
                    // Estado e owner ainda precisam ser restaurados mesmo sem teardown BLE.
                } finally {
                    (attemptToken?.transportSessionId ?: expectedTransportSessionId)?.let { transportSessionId ->
                        invalidTransportSessionId = transportSessionId
                        if (attemptToken != null && tokenStillTracked) {
                            if (acceptedTransportSessionId == transportSessionId) {
                                acceptedTransportSessionId = null
                            }
                            if (pendingTransportSessionId == transportSessionId) {
                                pendingTransportSessionId = null
                            }
                        }
                    }
                    if (attemptToken != null && tokenStillTracked) {
                        forgetConnectionAttemptLocked(attemptToken)
                    }
                    if (connectionAttemptSessionId == sessionId) {
                        connectionAttemptSessionId = null
                    }
                    val participationStillTracked = enabledSessionId == sessionId ||
                        boundSessionId == sessionId
                    if ((localOwnerMatches || ownerReleased) && participationStillTracked) {
                        setDisconnectedLocked(sessionId)
                        val shouldReleasePermit = releaseExpectedPermit || permitAcquiredByAttempt != null
                        if (shouldReleasePermit && localOwnerMatches && permit == cleanupPermit) {
                            permit = null
                            enabledSessionId = null
                            permitToRelease = cleanupPermit
                        }
                    }
                }
            }
            permitToRelease?.let { releasePermitIfCurrent(it) }
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
        if (!isSessionInState(sessionId, allowedStates)) return

        val callerJob = currentCoroutineContext()[Job]
        val attemptToCancel = operationMutex.withLock {
            if (enabledSessionId != sessionId && boundSessionId != sessionId) return@withLock null
            connectionAttemptToken?.takeIf { it.sessionId == sessionId }?.let { attempt ->
                if (connectionAttemptTombstones.none { it === attempt }) {
                    connectionAttemptTombstones += attempt
                }
            }
            connectionAttemptToken?.takeIf { it.sessionId == sessionId }
        }

        withContext(NonCancellable) {
            attemptToCancel?.job?.takeIf { it !== callerJob }?.cancelAndJoin()

            val attemptStillTracked = attemptToCancel?.let { attempt ->
                operationMutex.withLock {
                    connectionAttemptToken === attempt ||
                        connectionAttemptTombstones.any { it === attempt }
                }
            } == true
            if (attemptToCancel != null && attemptStillTracked) {
                cleanupEnableAttempt(
                    sessionId = sessionId,
                    expectedPermit = attemptToCancel.expectedPermit,
                    permitAcquiredByAttempt = null,
                    expectedTransportKey = attemptToCancel.transportKey,
                    connectionAttemptStarted = true,
                    returnedTransport = attemptToCancel.transport,
                    attemptToken = attemptToCancel,
                    releaseExpectedPermit = false,
                )
            }

            disconnectAcceptedTransportAtInactiveState(sessionId)
            if (!isSessionInState(sessionId, allowedStates)) return@withContext

            val permitToRelease = operationMutex.withLock {
                if (enabledSessionId != sessionId && boundSessionId != sessionId) return@withLock null
                terminalObserverJob?.cancel()
                terminalObserverJob = null
                foregroundBleSessionId = null
                enabledSessionId = null
                acceptedTransportSessionId = null
                pendingTransportSessionId = null
                invalidTransportSessionId = null
                connectionAttemptSessionId = null
                boundSessionId = boundSessionId?.takeUnless { it == sessionId }
                mutableState.value = WatchWorkoutParticipationState()
                permit.also { permit = null }
            }
            permitToRelease?.let { foregroundBleOperationGate.release(it) }
        }
    }

    private suspend fun disconnectAcceptedTransportAtInactiveState(sessionId: AppWorkoutSessionId) {
        val expected = operationMutex.withLock {
            val expectedPermit = permit
            val expectedTransportId = acceptedTransportSessionId ?: return@withLock null
            val expectedTransportKey = associatedDevice?.platformIdentifier ?: return@withLock null
            if (enabledSessionId != sessionId ||
                expectedPermit?.owner != ForegroundBleOperationOwner.APP_WORKOUT_SESSION ||
                expectedPermit.operationId != sessionId.value.toString()
            ) {
                return@withLock null
            }
            InactiveTransportCleanup(expectedPermit, expectedTransportId, expectedTransportKey)
        } ?: return

        val currentTransport = try {
            connectionManager.currentSession()
        } catch (_: Exception) {
            null
        } ?: return
        if (currentTransport.session.id != expected.transportSessionId ||
            currentTransport.session.transportKey != expected.transportKey
        ) {
            return
        }

        val stillOwned = operationMutex.withLock {
            enabledSessionId == sessionId &&
                permit == expected.permit &&
                acceptedTransportSessionId == expected.transportSessionId
        }
        if (!stillOwned) return

        try {
            connectionManager.disconnectExpectedTransport(
                currentTransport,
                ChampionDisconnectReason.CLEANUP,
            )
        } catch (_: Exception) {
            // Owner ainda é liberado abaixo; callbacks tardios permanecem identitários.
        }
    }

    private suspend fun isSessionInState(
        sessionId: AppWorkoutSessionId,
        allowedStates: Set<AppWorkoutSessionState>,
    ): Boolean = try {
        appWorkoutSessionRepository.observeSession(sessionId).first()?.state in allowedStates
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        false
    }

    fun requiresConnectedDeviceForegroundType(sessionId: AppWorkoutSessionId): Boolean =
        foregroundBleSessionId == sessionId

    private suspend fun canBindSessionLocked(sessionId: AppWorkoutSessionId): Boolean = try {
        appWorkoutSessionRepository.observeSession(sessionId).first()
            ?.let { it.state !in NON_BINDABLE_SESSION_STATES }
            ?: false
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        false
    }

    /** Captures transport identity while this attempt still owns the gate. */
    private suspend fun captureAttemptTransport(
        token: ConnectionAttemptToken,
        returnedTransport: ChampionTransportSession?,
    ) {
        operationMutex.withLock {
            if (connectionAttemptToken !== token) return@withLock

            val currentTransport = try {
                connectionManager.currentSession()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
            val currentState = connectionManager.state.value
            val activeOperation = try {
                foregroundBleOperationGate.observeActiveOperation().first()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
            val activeOperationMatches = activeOperation?.let {
                it.owner == token.expectedPermit.owner &&
                    it.operationId == token.expectedPermit.operationId
            } == true
            val localOwnerMatches = permit == token.expectedPermit &&
                enabledSessionId == token.sessionId
            val ownerReleased = permit == null && enabledSessionId == null
            val attemptOwnerMatches = token.expectedPermit.owner ==
                ForegroundBleOperationOwner.APP_WORKOUT_SESSION &&
                token.operationId == token.sessionId.value.toString() &&
                (localOwnerMatches || ownerReleased) &&
                (activeOperation == null || activeOperationMatches)

            val readyState = currentState as? ChampionBleState.TransportReady
            val readySession = readyState?.session
            if (readySession?.transportKey == token.transportKey) {
                val currentTransportMatchesState = currentTransport != null &&
                    currentTransport.session.id == readySession.id &&
                    currentTransport.session.transportKey == readySession.transportKey
                val returnedTransportMatchesState = returnedTransport?.let {
                    it.session.id == readySession.id &&
                        it.session.transportKey == readySession.transportKey &&
                        (currentTransport == null || currentTransport === it)
                } ?: true
                if (attemptOwnerMatches && currentTransportMatchesState && returnedTransportMatchesState) {
                    token.transport = currentTransport
                    token.transportSessionId = readySession.id
                    token.transportIdentityInvalid = false
                } else {
                    token.transport = null
                    token.transportSessionId = readySession.id
                    token.transportIdentityInvalid = true
                    invalidTransportSessionId = readySession.id
                }
                return@withLock
            }

            val returned = returnedTransport?.takeIf {
                it.session.transportKey == token.transportKey
            }
            if (returned != null && attemptOwnerMatches) {
                token.transport = returned
                token.transportSessionId = returned.session.id
                token.transportIdentityInvalid = false
            } else if (returned != null) {
                token.transport = null
                token.transportSessionId = returned.session.id
                token.transportIdentityInvalid = true
                invalidTransportSessionId = returned.session.id
            }
        }
    }

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

    /**
     * Reconciles local UI state against the manager instead of trusting the
     * coordinator's previous MutableStateFlow emission. The manager state and
     * transport object must describe the same transport session before a
     * participation owner can expose CONNECTED.
     */
    private suspend fun reconcileBoundStateLocked(
        sessionId: AppWorkoutSessionId? = boundSessionId,
    ): Boolean {
        captureAttemptSessionIdLocked(connectionManager.state.value)
        val boundId = sessionId ?: return false
        if (boundSessionId != boundId) return false

        if (enabledSessionId == null && permitBelongsToSessionLocked(boundId)) {
            enabledSessionId = boundId
        }
        if (enabledSessionId == boundId && !permitBelongsToSessionLocked(boundId)) {
            enabledSessionId = null
            acceptedTransportSessionId = null
            pendingTransportSessionId = null
            connectionAttemptSessionId = null
            foregroundBleSessionId = null
        }

        val participationEnabled = participationEnabledLocked(boundId)
        val device = associatedDevice
        if (device == null) {
            if (participationEnabled) {
                setDisconnectedLocked(boundId)
            } else {
                setNoAssociatedDeviceLocked(boundId)
            }
            return false
        }

        if (!participationEnabled) {
            setAssociatedLocked(boundId)
            return false
        }

        val currentTransport = try {
            connectionManager.currentSession()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        val latestState = connectionManager.state.value
        val readyState = latestState as? ChampionBleState.TransportReady
        val attemptInProgressForBoundSession = connectionAttemptToken?.sessionId == boundId
        val attemptIdentityValid = connectionAttemptToken?.transportIdentityInvalid != true
        val currentTransportMatchesState = readyState != null &&
            currentTransport != null &&
            readyState.session.id == currentTransport.session.id &&
            readyState.session.transportKey == currentTransport.session.transportKey
        val readyForAssociatedDevice = currentTransportMatchesState &&
            readyState.session.transportKey == device.platformIdentifier &&
            readyState.session.id != invalidTransportSessionId
        // O manager publica TransportReady antes de o bootstrap concluir A1.
        // Token ativo mantém tentativa em CONNECTING até a confirmação final.
        val readyTransportAccepted = readyForAssociatedDevice && attemptIdentityValid && (
            !attemptInProgressForBoundSession &&
                (
                    acceptedTransportSessionId == readyState.session.id ||
                        pendingTransportSessionId == readyState.session.id
                    )
            )

        if (readyTransportAccepted) {
            acceptedTransportSessionId = readyState.session.id
            pendingTransportSessionId = null
            invalidTransportSessionId = null
            connectionAttemptSessionId = null
            setConnectedLocked(boundId)
            return true
        }

        val connectingTransportKey = latestState.connectionTransportKeyOrNull()
        val connectingTransportId = latestState.connectionSessionIdOrNull()
        val isConnectingState = latestState is ChampionBleState.Connecting ||
            latestState is ChampionBleState.DiscoveringServices ||
            latestState is ChampionBleState.ResolvingChannels ||
            latestState is ChampionBleState.ConfiguringSubscriptions
        val canTrackConnectingTransport = connectingTransportKey == device.platformIdentifier &&
            connectingTransportId != null &&
            (
                isTrackedTransportLocked(connectingTransportId) ||
                    (
                        isConnectingState &&
                        mutableState.value.status == WatchWorkoutParticipationStatus.CONNECTING &&
                        acceptedTransportSessionId == null &&
                        pendingTransportSessionId == null
                    )
            )
        if (canTrackConnectingTransport) {
            pendingTransportSessionId = connectingTransportId
            setConnectingLocked(boundId)
            return false
        }

        if (attemptInProgressForBoundSession || connectionAttemptSessionId == boundId) {
            setConnectingLocked(boundId)
            return false
        }

        // Any state other than the accepted ready transport means a previously
        // enabled participation is disconnected. Owner remains held for retry.
        setDisconnectedLocked(boundId)
        return false
    }

    private suspend fun updateFromTransportStateLocked(state: ChampionBleState) {
        if (connectionManager.state.value != state) return
        reconcileBoundStateLocked()
    }

    private fun captureAttemptSessionIdLocked(state: ChampionBleState) {
        val token = connectionAttemptToken ?: return
        val transportKey = state.connectionTransportKeyOrNull() ?: return
        val sessionId = state.connectionSessionIdOrNull() ?: return
        if (transportKey != token.transportKey) return
        if (token.transportSessionId == null || participationEnabledLocked(token.sessionId)) {
            if (token.transportSessionId != sessionId) {
                token.transportSessionId = sessionId
                token.transport = null
                token.transportIdentityInvalid = false
            }
        }
    }

    private suspend fun loadAssociatedDevice(): Device? = try {
        deviceRepository.observeAssociatedDevice().first()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null
    }

    private fun participationEnabledLocked(sessionId: AppWorkoutSessionId): Boolean =
        enabledSessionId == sessionId && permitBelongsToSessionLocked(sessionId)

    private fun permitBelongsToSessionLocked(sessionId: AppWorkoutSessionId): Boolean =
        permit?.owner == ForegroundBleOperationOwner.APP_WORKOUT_SESSION &&
            permit?.operationId == sessionId.value.toString()

    private suspend fun releasePermitIfCurrent(expectedPermit: ForegroundBleOperationPermit) {
        val activeOperation = try {
            foregroundBleOperationGate.observeActiveOperation().first()
        } catch (_: CancellationException) {
            null
        } catch (_: Exception) {
            null
        }
        if (activeOperation?.owner == expectedPermit.owner &&
            activeOperation.operationId == expectedPermit.operationId
        ) {
            foregroundBleOperationGate.release(expectedPermit)
        }
    }

    private fun setNoAssociatedDeviceLocked(sessionId: AppWorkoutSessionId) {
        mutableState.value = WatchWorkoutParticipationState(
            sessionId = sessionId,
            status = WatchWorkoutParticipationStatus.NO_ASSOCIATED_DEVICE,
        )
        acceptedTransportSessionId = null
        pendingTransportSessionId = null
        connectionAttemptSessionId = null
        foregroundBleSessionId = null
    }

    private fun setAssociatedLocked(sessionId: AppWorkoutSessionId) {
        mutableState.value = WatchWorkoutParticipationState(
            sessionId = sessionId,
            status = WatchWorkoutParticipationStatus.ASSOCIATED,
        )
        acceptedTransportSessionId = null
        pendingTransportSessionId = null
        connectionAttemptSessionId = null
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

    private fun isTrackedTransportLocked(sessionId: ChampionSessionId): Boolean =
        acceptedTransportSessionId == sessionId || pendingTransportSessionId == sessionId

    private fun forgetConnectionAttemptLocked(token: ConnectionAttemptToken) {
        if (connectionAttemptToken === token) {
            connectionAttemptToken = null
        }
        connectionAttemptTombstones.removeAll { it === token }
    }

    private data class InactiveTransportCleanup(
        val permit: ForegroundBleOperationPermit,
        val transportSessionId: ChampionSessionId,
        val transportKey: String,
    )

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
        val NON_BINDABLE_SESSION_STATES = setOf(
            AppWorkoutSessionState.FINISHING,
            AppWorkoutSessionState.AWAITING_RECOVERY,
        ) + TERMINAL_STATES
        val RELEASE_OWNER_STATES = TERMINAL_STATES + AppWorkoutSessionState.AWAITING_RECOVERY
    }
}
