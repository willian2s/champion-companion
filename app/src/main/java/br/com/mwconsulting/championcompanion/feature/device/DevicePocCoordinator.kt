package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDisconnectReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCancellationReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionOperationType
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacket
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionScanRequest
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSessionId
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionTransportSession
import br.com.mwconsulting.championcompanion.core.common.BleDispatcher
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationAcquireResult
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationGate
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationOwner
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationPermit
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationRequest
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration

class DevicePocCoordinator @Inject constructor(
    private val connectionManager: ChampionConnectionManager,
    private val connectionBootstrap: DeviceConnectionBootstrap,
    private val foregroundBleOperationGate: ForegroundBleOperationGate,
    @BleDispatcher bleDispatcher: CoroutineDispatcher,
) : DeviceFeatureContract {
    constructor(
        connectionManager: ChampionConnectionManager,
        foregroundBleOperationGate: ForegroundBleOperationGate,
        @BleDispatcher bleDispatcher: CoroutineDispatcher,
    ) : this(
        connectionManager = connectionManager,
        connectionBootstrap = DeviceConnectionBootstrap(connectionManager),
        foregroundBleOperationGate = foregroundBleOperationGate,
        bleDispatcher = bleDispatcher,
    )

    private val scope = CoroutineScope(SupervisorJob() + bleDispatcher)
    private val mutableState = MutableStateFlow(DeviceUiState())
    private var stepsObservationJob: Job? = null
    private var d0ObservationJob: Job? = null
    private var f002ObservationJob: Job? = null
    private val f002Buffer = mutableListOf<Int>()
    private val terminalCleanupMutex = Mutex()
    private val permitMutex = Mutex()
    private var foregroundBleOperationLease: ForegroundBleOperationLease? = null
    private var foregroundBleOperationSequence = 0L

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
        val acquisition = acquireForegroundBleOperationPermit() ?: run {
            return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        }
        val activeCycleId = acquisition.lease.cycleId
        updateState {
            it.copy(
                pocState = DevicePocState.Scanning,
                candidate = null,
                lastError = null,
            )
        }

        return try {
            when (val result = connectionManager.scan(request)) {
                is ChampionBleResult.Failure -> {
                    if (state.value.cycleId == activeCycleId) {
                        transitionToFailure(
                            cycleId = activeCycleId,
                            sessionId = null,
                            error = result.error,
                            expectedLease = acquisition.lease,
                        )
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
        } finally {
            if (acquisition.acquiredHere) {
                releaseForegroundBleOperationPermit(acquisition.lease)
            }
        }
    }

    suspend fun connect(device: ChampionDiscoveredDevice): ChampionBleResult<ChampionTransportSession> {
        if (state.value.pocState !in setOf(DevicePocState.DeviceFound, DevicePocState.ReadyToScan)) {
            return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        }
        val acquisition = acquireForegroundBleOperationPermit() ?: run {
            return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        }
        val activeCycleId = acquisition.lease.cycleId
        updateState {
            it.copy(
                pocState = DevicePocState.Connecting,
                candidate = device,
                lastError = null,
            )
        }

        var keepPermit = false
        return try {
            when (val result = connectionManager.connect(device)) {
                is ChampionBleResult.Failure -> {
                    if (state.value.cycleId == activeCycleId) {
                        transitionToFailure(
                            cycleId = activeCycleId,
                            sessionId = acquisition.lease.sessionId,
                            error = result.error,
                            expectedLease = acquisition.lease,
                        )
                    }
                    result
                }

                is ChampionBleResult.Success -> {
                    val sessionId = result.value.session.id
                    val lease = bindForegroundBleOperationSession(activeCycleId, sessionId)
                    if (lease == null) {
                        withContext(NonCancellable) {
                            disconnectExpectedTransport(
                                expectedTransport = result.value,
                                expectedCycleId = activeCycleId,
                                expectedSessionId = sessionId,
                            )
                        }
                        return ChampionBleResult.Failure(
                            ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP),
                        )
                    }
                    updateState { current ->
                        if (current.cycleId == activeCycleId &&
                            current.activeSessionId == null &&
                            current.pocState == DevicePocState.Connecting
                        ) {
                            current.copy(activeSessionId = sessionId)
                        } else {
                            current
                        }
                    }
                    keepPermit = true
                    result
                }
            }
        } finally {
            if (!keepPermit) releaseForegroundBleOperationPermit(acquisition.lease)
        }
    }

    suspend fun connectAndBootstrap(device: ChampionDiscoveredDevice): ChampionBleResult<ChampionTransportSession> {
        val activeCycleId = state.value.cycleId
        val result = connect(device)
        val transport = (result as? ChampionBleResult.Success)?.value ?: return result
        val sessionId = transport.session.id
        val operationLease = currentForegroundBleOperationLease(activeCycleId, sessionId) ?: run {
            withContext(NonCancellable) {
                disconnectExpectedTransport(
                    expectedTransport = transport,
                    expectedCycleId = activeCycleId,
                    expectedSessionId = sessionId,
                )
            }
            return ChampionBleResult.Failure(
                ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP),
            )
        }
        var bootstrapSucceeded = false
        var terminalCleanupRequested = false
        try {
            return when (
                val handshake = executeHandshake(
                    dateTime = ZonedDateTime.now(),
                    expectedCycleId = activeCycleId,
                    expectedSessionId = sessionId,
                )
            ) {
                is ChampionBleResult.Success -> {
                    bootstrapSucceeded = true
                    ChampionBleResult.Success(transport)
                }

                is ChampionBleResult.Failure -> {
                    // executeHandshake pode falhar antes de assumir TransportReady
                    // (por exemplo, se o coletor de estado ainda estiver atrasado).
                    // O ciclo conectado não pode reter gate nesse caso.
                    terminalCleanupRequested = true
                    transitionToFailure(
                        cycleId = activeCycleId,
                        sessionId = sessionId,
                        error = handshake.error,
                        expectedLease = operationLease,
                    )
                    ChampionBleResult.Failure(handshake.error)
                }
            }
        } finally {
            if (!bootstrapSucceeded && !terminalCleanupRequested) {
                withContext(NonCancellable) {
                    cleanupTerminalFailure(
                        cycleId = activeCycleId,
                        sessionId = sessionId,
                        error = ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP),
                        expectedLease = operationLease,
                    )
                }
            }
        }
    }

    suspend fun disconnect(
        reason: ChampionDisconnectReason = ChampionDisconnectReason.USER_REQUESTED,
    ): ChampionBleResult<Unit> {
        val acquisition = acquireForegroundBleOperationPermit() ?: run {
            return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        }
        detachCycleObservationJobs()
        updateState {
            it.copy(
                pocState = DevicePocState.Disconnecting,
                lastError = null,
            )
        }
        return try {
            val result = connectionManager.disconnect(reason)
            if (result is ChampionBleResult.Failure) {
                val cycleId = acquisition.lease.cycleId
                val sessionId = acquisition.lease.sessionId
                transitionToFailure(
                    cycleId = cycleId,
                    sessionId = sessionId,
                    error = result.error,
                    expectedLease = acquisition.lease,
                )
            } else {
                clearCycleState(DevicePocState.Idle)
            }
            result
        } finally {
            releaseForegroundBleOperationPermit(acquisition.lease)
        }
    }

    suspend fun resetCycle(): ChampionBleResult<Unit> {
        val acquisition = acquireForegroundBleOperationPermit() ?: run {
            return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        }
        val nextCycleId = DevicePocCycleId.random()
        detachCycleObservationJobs()
        return try {
            val result = connectionManager.disconnect(ChampionDisconnectReason.CLEANUP)
            if (result is ChampionBleResult.Success) {
                mutableState.value = DeviceUiState(cycleId = nextCycleId)
            }
            result
        } finally {
            releaseForegroundBleOperationPermit(acquisition.lease)
        }
    }

    /** Limpa conexão PoC quando superfície técnica deixa composição. */
    fun requestCleanup() {
        scope.launch { resetCycle() }
    }

    suspend fun currentTransportSession(): ChampionTransportSession? = connectionManager.currentSession()

    suspend fun executeHandshake(): ChampionBleResult<Unit> = executeHandshake(ZonedDateTime.now())

    suspend fun startHeartRateSession(
        maxBufferedSamples: Int = DevicePocHeartRateSession.DEFAULT_MAX_BUFFERED_SAMPLES,
    ): ChampionBleResult<Unit> {
        val current = state.value
        val sessionId = current.activeSessionId ?: return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        val operationLease = bindForegroundBleOperationSession(current.cycleId, sessionId)
            ?: return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        var externalCancellation: CancellationException? = null
        try {
            if (current.pocState !in setOf(
                DevicePocState.HandshakeConfirmed,
                DevicePocState.WaitingForSteps,
                DevicePocState.StepsObserved,
                DevicePocState.CycleCompleted,
            )
            ) {
                return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
            }
            val transport = currentTransportSession()
                ?: return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
            if (transport.session.id != sessionId) {
                return ChampionBleResult.Failure(ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP))
            }
            if (!isCurrentForegroundBleOperation(operationLease, current.cycleId, sessionId)) {
                return ChampionBleResult.Failure(ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP))
            }
            if (!transport.capabilities.snapshot.hasChannel(ChampionChannelAlias.F002_NOTIFY)) {
                val error = ChampionBleError.MissingOptionalChannel(ChampionChannelAlias.F002_NOTIFY)
                transitionToProtocolFailure(current.cycleId, sessionId, error, operationLease)
                return ChampionBleResult.Failure(error)
            }
            val ensureF002 = transport.notifications.ensureSubscribed(ChampionChannelAlias.F002_NOTIFY)
            if (ensureF002 is ChampionBleResult.Failure) {
                transitionToProtocolFailure(current.cycleId, sessionId, ensureF002.error, operationLease)
                return ChampionBleResult.Failure(ensureF002.error)
            }
            val f002Subscription = transport.notifications.subscribe(ChampionChannelAlias.F002_NOTIFY)
            if (f002Subscription is ChampionBleResult.Failure) {
                transitionToProtocolFailure(current.cycleId, sessionId, f002Subscription.error, operationLease)
                return ChampionBleResult.Failure(f002Subscription.error)
            }

            val heartRateSessionId = DevicePocHeartRateSessionId.random()
            val startedAt = java.time.Instant.now()
            if (!isCurrentForegroundBleOperation(operationLease, current.cycleId, sessionId)) {
                return ChampionBleResult.Failure(ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP))
            }
            clearHeartRateRuntimeState()
            var heartRateStateStarted = false
            updateState { currentState ->
                if (currentState.cycleId != current.cycleId || currentState.activeSessionId != sessionId) {
                    currentState
                } else {
                    heartRateStateStarted = true
                    currentState.copy(
                        pocState = DevicePocState.StartingHeartRate,
                        isHeartRateCapabilityAvailable = true,
                        isHeartRateCapabilityMissing = false,
                        activeHeartRateSessionId = heartRateSessionId,
                        heartRateStartedAt = startedAt,
                        d0PacketCount = 0,
                        f002PacketCount = 0,
                        f002BufferedSampleCount = 0,
                        estimatedF002Bpm = null,
                        estimatedF002Quality = null,
                        estimatedF002QualityScore = 0,
                        estimatedF002SampleRateHz = null,
                        estimatedF002PeakCount = 0,
                        lastD0Bpm = null,
                        lastD0ObservedAt = null,
                        finalHeartRateBpm = null,
                        finalHeartRateSource = DevicePocHeartRateFinalSource.UNAVAILABLE,
                        lastError = null,
                    )
                }
            }
            if (!heartRateStateStarted) {
                return ChampionBleResult.Failure(ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP))
            }

            d0ObservationJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                transport.streams.observeChannel(sessionId, ChampionChannelAlias.F008_NOTIFY).collect { packet ->
                    when (val parsed = DevicePocHeartRateSession.parseLiveBpm(packet, startedAt)) {
                        DevicePocHeartRateSession.D0Result.Ignored -> Unit
                        is DevicePocHeartRateSession.D0Result.Valid -> updateState {
                            if (it.activeSessionId != sessionId || it.activeHeartRateSessionId != heartRateSessionId) {
                                it
                            } else {
                                val selection = DevicePocF002BpmEstimator.selectFinalBpm(
                                    currentD0Bpm = parsed.bpm,
                                    estimate = it.currentEstimate(),
                                )
                                it.copy(
                                    pocState = DevicePocState.MeasuringHeartRate,
                                    d0PacketCount = it.d0PacketCount + 1,
                                    lastD0Bpm = parsed.bpm,
                                    lastD0ObservedAt = parsed.observedAt,
                                    finalHeartRateBpm = selection.bpm,
                                    finalHeartRateSource = selection.source,
                                    lastError = null,
                                )
                            }
                        }
                    }
                }
            } catch (_: CancellationException) {
                Unit
            } catch (error: RuntimeException) {
                if (!error.isExpectedObserverShutdown()) {
                    transitionToProtocolFailure(
                        cycleId = current.cycleId,
                        sessionId = sessionId,
                        error = ChampionBleError.UnexpectedInternal,
                        expectedLease = operationLease,
                    )
                }
            }
        }
            f002ObservationJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                (f002Subscription as ChampionBleResult.Success).value.collect { packet ->
                    val samples = DevicePocHeartRateSession.parseF002Samples(packet)
                    updateState {
                        if (it.activeSessionId != sessionId || it.activeHeartRateSessionId != heartRateSessionId) {
                            it
                        } else {
                            val bufferedCount = DevicePocHeartRateSession.appendLimitedBuffer(
                                buffer = f002Buffer,
                                samples = samples,
                                maxSamples = maxBufferedSamples,
                            )
                            val estimate = inspectF002(
                                samples = f002Buffer,
                                startedAt = startedAt,
                                observedAt = packet.observedAt,
                            )
                            val selection = DevicePocF002BpmEstimator.selectFinalBpm(
                                currentD0Bpm = it.lastD0Bpm,
                                estimate = estimate.estimate,
                            )
                            it.copy(
                                pocState = DevicePocState.MeasuringHeartRate,
                                f002PacketCount = it.f002PacketCount + 1,
                                f002BufferedSampleCount = bufferedCount,
                                estimatedF002Bpm = estimate.estimate?.bpm,
                                estimatedF002Quality = estimate.estimate?.quality ?: estimate.quality,
                                estimatedF002QualityScore = estimate.estimate?.qualityScore ?: estimate.qualityScore,
                                estimatedF002SampleRateHz = estimate.sampleRateHz,
                                estimatedF002PeakCount = estimate.peakCount,
                                finalHeartRateBpm = selection.bpm,
                                finalHeartRateSource = selection.source,
                                lastError = null,
                            )
                        }
                    }
                }
            } catch (_: CancellationException) {
                Unit
            } catch (error: RuntimeException) {
                if (!error.isExpectedObserverShutdown()) {
                    transitionToProtocolFailure(
                        cycleId = current.cycleId,
                        sessionId = sessionId,
                        error = ChampionBleError.SubscriptionFailed(ChampionChannelAlias.F002_NOTIFY),
                        expectedLease = operationLease,
                    )
                }
            }
        }

            return when (val result = transport.sendCommand(DevicePocHeartRateSession.createStartCommand())) {
                is ChampionBleResult.Success -> {
                    updateState {
                        if (it.activeSessionId != sessionId || it.activeHeartRateSessionId != heartRateSessionId) {
                            it
                        } else {
                            it.copy(
                                pocState = DevicePocState.MeasuringHeartRate,
                                lastError = null,
                            )
                        }
                    }
                    ChampionBleResult.Success(Unit)
                }

                is ChampionBleResult.Failure -> {
                    clearHeartRateRuntimeState()
                    transitionToProtocolFailure(current.cycleId, sessionId, result.error, operationLease)
                    ChampionBleResult.Failure(result.error)
                }
            }
        } catch (cancellation: CancellationException) {
            externalCancellation = cancellation
            throw cancellation
        } finally {
            externalCancellation?.let { cancellation ->
                try {
                    withContext(NonCancellable) {
                        cleanupTerminalFailure(
                            cycleId = current.cycleId,
                            sessionId = sessionId,
                            error = ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP),
                            expectedLease = operationLease,
                        )
                    }
                } catch (cleanupCancellation: CancellationException) {
                    cancellation.addSuppressed(cleanupCancellation)
                }
            }
        }
    }

    suspend fun stopHeartRateSession(): ChampionBleResult<Unit> {
        val current = state.value
        val sessionId = current.activeSessionId ?: return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        val heartRateSessionId = current.activeHeartRateSessionId
            ?: return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        if (current.pocState !in setOf(
                DevicePocState.StartingHeartRate,
                DevicePocState.MeasuringHeartRate,
                DevicePocState.StoppingHeartRate,
            )
        ) {
            return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        }

        updateState {
            if (it.activeSessionId != sessionId || it.activeHeartRateSessionId != heartRateSessionId) {
                it
            } else {
                it.copy(
                    pocState = DevicePocState.StoppingHeartRate,
                    lastError = null,
                )
            }
        }

        val stopResult = try {
            attemptHeartRateStop(sessionId)
        } finally {
            val finalSelection = currentHeartRateSelection()
            clearHeartRateSession(
                sessionId = sessionId,
                heartRateSessionId = heartRateSessionId,
                nextState = DevicePocState.CycleCompleted,
                finalSelection = finalSelection,
            )
        }

        if (stopResult is ChampionBleResult.Failure) {
            updateState {
                if (it.activeSessionId != sessionId) {
                    it
                } else {
                    it.copy(lastError = stopResult.error)
                }
            }
        }
        return stopResult
    }

    suspend fun awaitSteps(): ChampionBleResult<Unit> {
        val current = state.value
        val sessionId = current.activeSessionId ?: return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        val operationLease = bindForegroundBleOperationSession(current.cycleId, sessionId)
            ?: return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        if (current.pocState !in setOf(
                DevicePocState.HandshakeConfirmed,
                DevicePocState.WaitingForSteps,
                DevicePocState.StepsObserved,
            )
        ) {
            return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        }
        val transport = currentTransportSession()
            ?: return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        if (transport.session.id != sessionId) {
            return ChampionBleResult.Failure(ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP))
        }
        if (!isCurrentForegroundBleOperation(operationLease, current.cycleId, sessionId)) {
            return ChampionBleResult.Failure(ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP))
        }
        if (!transport.capabilities.snapshot.hasChannel(ChampionChannelAlias.FEA1_NOTIFY)) {
            val error = ChampionBleError.MissingOptionalChannel(ChampionChannelAlias.FEA1_NOTIFY)
            transitionToProtocolFailure(current.cycleId, sessionId, error, operationLease)
            return ChampionBleResult.Failure(error)
        }
        if (stepsObservationJob?.isActive == true) {
            moveToWaitingForSteps(sessionId)
            return ChampionBleResult.Success(Unit)
        }

        val subscription = transport.notifications.subscribe(ChampionChannelAlias.FEA1_NOTIFY)
        if (subscription is ChampionBleResult.Failure) {
            transitionToProtocolFailure(current.cycleId, sessionId, subscription.error, operationLease)
            return ChampionBleResult.Failure(subscription.error)
        }

        if (!moveToWaitingForSteps(sessionId)) {
            return ChampionBleResult.Failure(ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP))
        }
        stepsObservationJob = scope.launch {
            try {
                (subscription as ChampionBleResult.Success).value.collect { packet ->
                    if (state.value.activeSessionId != sessionId) return@collect
                    when (val parsed = DevicePocFea1Steps.parse(packet)) {
                        is DevicePocFea1Steps.Result.Valid -> updateState {
                            if (it.activeSessionId != sessionId) {
                                it
                            } else {
                                it.copy(
                                    pocState = DevicePocState.StepsObserved,
                                    hasObservedSteps = true,
                                    stepsPacketCount = it.stepsPacketCount + 1,
                                    lastStepCount = parsed.steps,
                                    lastError = null,
                                )
                            }
                        }

                        is DevicePocFea1Steps.Result.Invalid -> {
                            transitionToProtocolFailure(
                                cycleId = current.cycleId,
                                sessionId = sessionId,
                                error = parsed.error,
                                expectedLease = operationLease,
                            )
                            stepsObservationJob?.cancel()
                        }
                    }
                }
            } catch (_: CancellationException) {
                Unit
            } catch (error: RuntimeException) {
                if (!error.isExpectedObserverShutdown()) {
                    transitionToProtocolFailure(
                        cycleId = current.cycleId,
                        sessionId = sessionId,
                        error = ChampionBleError.SubscriptionFailed(ChampionChannelAlias.FEA1_NOTIFY),
                        expectedLease = operationLease,
                    )
                }
            }
        }
        return ChampionBleResult.Success(Unit)
    }

    internal suspend fun executeHandshake(
        dateTime: ZonedDateTime,
        timeout: Duration = DevicePocHandshakeA1.defaultTimeout,
        expectedCycleId: DevicePocCycleId? = null,
        expectedSessionId: ChampionSessionId? = null,
    ): ChampionBleResult<Unit> {
        val current = state.value
        if ((expectedCycleId != null && current.cycleId != expectedCycleId) ||
            (expectedSessionId != null && current.activeSessionId != expectedSessionId)
        ) {
            return ChampionBleResult.Failure(
                ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP),
            )
        }
        val sessionId = current.activeSessionId ?: return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        val operationLease = bindForegroundBleOperationSession(current.cycleId, sessionId)
            ?: return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        if ((expectedCycleId != null && operationLease.cycleId != expectedCycleId) ||
            (expectedSessionId != null && operationLease.sessionId != expectedSessionId)
        ) {
            return ChampionBleResult.Failure(
                ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP),
            )
        }
        if (current.pocState != DevicePocState.TransportReady) {
            val error = ChampionBleError.OperationUnavailable
            transitionToFailure(current.cycleId, sessionId, error, operationLease)
            return ChampionBleResult.Failure(error)
        }
        val transport = currentTransportSession() ?: run {
            val error = ChampionBleError.OperationUnavailable
            transitionToFailure(current.cycleId, sessionId, error, operationLease)
            return ChampionBleResult.Failure(error)
        }
        if (transport.session.id != sessionId) {
            val error = ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP)
            transitionToFailure(current.cycleId, sessionId, error, operationLease)
            return ChampionBleResult.Failure(error)
        }
        if (!isCurrentForegroundBleOperation(operationLease, current.cycleId, sessionId)) {
            return ChampionBleResult.Failure(
                ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP),
            )
        }

        moveToHandshaking(sessionId)
        val result = try {
            connectionBootstrap.executeHandshake(transport, dateTime, timeout)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            transitionToFailure(
                cycleId = current.cycleId,
                sessionId = sessionId,
                error = ChampionBleError.UnexpectedInternal,
                expectedLease = operationLease,
            )
            return ChampionBleResult.Failure(ChampionBleError.UnexpectedInternal)
        }
        return when (result) {
            is ChampionBleResult.Success -> {
                moveToHandshakeConfirmed(
                    sessionId = sessionId,
                    assumed = result.value == DeviceConnectionProtocolState.HANDSHAKE_ASSUMED,
                )
                ChampionBleResult.Success(Unit)
            }

            is ChampionBleResult.Failure -> {
                transitionToFailure(current.cycleId, sessionId, result.error, operationLease)
                ChampionBleResult.Failure(result.error)
            }
        }
    }

    internal suspend fun handleTransportStateUpdate(transportState: ChampionBleState) {
        val current = state.value
        when (transportState) {
            ChampionBleState.Disconnected -> {
                val activeSessionId = current.activeSessionId
                val leaseToRelease = currentForegroundBleOperationLease(
                    cycleId = current.cycleId,
                    sessionId = activeSessionId,
                ) ?: return
                if (activeSessionId == null && current.pocState != DevicePocState.Disconnecting) return
                if (connectionManager.currentSession() != null) return
                val cleared = clearCycleState(
                    nextState = DevicePocState.Idle,
                    expectedCycleId = current.cycleId,
                    expectedSessionId = activeSessionId,
                )
                if (cleared) {
                    releaseForegroundBleOperationPermit(leaseToRelease)
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
                if (bindForegroundBleOperationSession(current.cycleId, transportState.session.id) == null) return
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
                        isHandshakeAssumed = false,
                        isStepsCapabilityAvailable = transportState.capabilities.hasChannel(ChampionChannelAlias.FEA1_NOTIFY),
                        isHeartRateCapabilityAvailable = transportState.capabilities.hasChannel(ChampionChannelAlias.F002_NOTIFY),
                        isStepsCapabilityMissing = false,
                        isHeartRateCapabilityMissing = false,
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
                if (sessionId == null || sessionId != current.activeSessionId) return
                if (connectionManager.currentSession()?.session?.id?.let { it != sessionId } == true) return
                val expectedLease = currentForegroundBleOperationLease(
                    cycleId = current.cycleId,
                    sessionId = sessionId,
                ) ?: return
                transitionToFailure(
                    cycleId = current.cycleId,
                    sessionId = sessionId,
                    error = transportState.error,
                    expectedLease = expectedLease,
                )
            }
        }
    }

    private suspend fun acquireForegroundBleOperationPermit(): ForegroundBleOperationAcquisition? {
        val cycleId = state.value.cycleId
        return permitMutex.withLock {
            foregroundBleOperationLease?.let { lease ->
                if (lease.cycleId != cycleId || lease.permit.owner != ForegroundBleOperationOwner.DEVICE_CONNECTION) {
                    return@withLock null
                }
                return@withLock ForegroundBleOperationAcquisition(
                    lease = lease,
                    acquiredHere = false,
                )
            }
            when (
                val result = foregroundBleOperationGate.tryAcquire(
                    ForegroundBleOperationRequest(
                        owner = ForegroundBleOperationOwner.DEVICE_CONNECTION,
                        operationId = "device-poc-${++foregroundBleOperationSequence}",
                        requestedAt = java.time.Instant.now(),
                    ),
                )
            ) {
                is ForegroundBleOperationAcquireResult.Acquired -> {
                    val lease = ForegroundBleOperationLease(
                        permit = result.permit,
                        cycleId = cycleId,
                        sessionId = null,
                    )
                    foregroundBleOperationLease = lease
                    ForegroundBleOperationAcquisition(
                        lease = lease,
                        acquiredHere = true,
                    )
                }

                is ForegroundBleOperationAcquireResult.Rejected -> {
                    updateState { it.copy(lastError = ChampionBleError.OperationUnavailable) }
                    null
                }
            }
        }
    }

    private data class ForegroundBleOperationAcquisition(
        val lease: ForegroundBleOperationLease,
        val acquiredHere: Boolean,
    )

    private data class ForegroundBleOperationLease(
        val permit: ForegroundBleOperationPermit,
        val cycleId: DevicePocCycleId,
        val sessionId: ChampionSessionId?,
    )

    private suspend fun bindForegroundBleOperationSession(
        cycleId: DevicePocCycleId,
        sessionId: ChampionSessionId,
    ): ForegroundBleOperationLease? = permitMutex.withLock {
        val lease = foregroundBleOperationLease ?: return@withLock null
        if (lease.permit.owner != ForegroundBleOperationOwner.DEVICE_CONNECTION ||
            lease.cycleId != cycleId ||
            (lease.sessionId != null && lease.sessionId != sessionId)
        ) {
            return@withLock null
        }
        lease.copy(sessionId = sessionId).also { foregroundBleOperationLease = it }
    }

    private suspend fun currentForegroundBleOperationLease(
        cycleId: DevicePocCycleId,
        sessionId: ChampionSessionId?,
    ): ForegroundBleOperationLease? = permitMutex.withLock {
        foregroundBleOperationLease?.takeIf {
            it.permit.owner == ForegroundBleOperationOwner.DEVICE_CONNECTION &&
                it.cycleId == cycleId &&
                it.sessionId == sessionId
        }
    }

    private suspend fun isCurrentForegroundBleOperation(
        expectedLease: ForegroundBleOperationLease,
        cycleId: DevicePocCycleId,
        sessionId: ChampionSessionId,
    ): Boolean = state.value.cycleId == cycleId &&
        state.value.activeSessionId == sessionId &&
        permitMutex.withLock { foregroundBleOperationLease == expectedLease }

    private suspend fun releaseForegroundBleOperationPermit(
        expectedLease: ForegroundBleOperationLease? = null,
    ) {
        withContext(NonCancellable) {
            val permitToRelease = permitMutex.withLock {
                val lease = foregroundBleOperationLease ?: return@withLock null
                if (expectedLease != null && lease != expectedLease) return@withLock null
                foregroundBleOperationLease = null
                lease.permit
            }
            permitToRelease?.let { foregroundBleOperationGate.release(it) }
        }
    }

    internal fun moveToHandshaking(sessionId: ChampionSessionId): Boolean = moveProtocolState(
        sessionId = sessionId,
        nextState = DevicePocState.Handshaking,
    )

    internal fun moveToHandshakeConfirmed(
        sessionId: ChampionSessionId,
        assumed: Boolean = false,
    ): Boolean = moveProtocolState(
        sessionId = sessionId,
        nextState = DevicePocState.HandshakeConfirmed,
        update = {
            it.copy(
                pocState = DevicePocState.HandshakeConfirmed,
                isHandshakeAssumed = assumed,
                lastError = null,
            )
        },
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
        update: (DeviceUiState) -> DeviceUiState = {
            it.copy(
                pocState = nextState,
                lastError = null,
            )
        },
    ): Boolean {
        val current = state.value
        if (current.activeSessionId != sessionId) return false
        updateState(update)
        return true
    }

    private fun clearCycleState(
        nextState: DevicePocState,
        expectedCycleId: DevicePocCycleId? = null,
        expectedSessionId: ChampionSessionId? = null,
    ): Boolean {
        var stateCleared = false
        updateState { current ->
            if (expectedCycleId != null &&
                (current.cycleId != expectedCycleId || current.activeSessionId != expectedSessionId)
            ) {
                current
            } else {
                stateCleared = true
                current.copy(
                    transportState = ChampionBleState.Disconnected,
                    pocState = nextState,
                    candidate = null,
                    activeSessionId = null,
                    isHandshakeAssumed = false,
                    isStepsCapabilityAvailable = false,
                    isStepsCapabilityMissing = false,
                    isHeartRateCapabilityAvailable = false,
                    isHeartRateCapabilityMissing = false,
                    hasObservedSteps = false,
                    stepsPacketCount = 0,
                    lastStepCount = null,
                    activeHeartRateSessionId = null,
                    heartRateStartedAt = null,
                    d0PacketCount = 0,
                    f002PacketCount = 0,
                    f002BufferedSampleCount = 0,
                    estimatedF002Bpm = null,
                    estimatedF002Quality = null,
                    estimatedF002QualityScore = 0,
                    estimatedF002SampleRateHz = null,
                    estimatedF002PeakCount = 0,
                    lastD0Bpm = null,
                    lastD0ObservedAt = null,
                    finalHeartRateBpm = null,
                    finalHeartRateSource = DevicePocHeartRateFinalSource.UNAVAILABLE,
                    lastError = null,
                )
            }
        }
        if (stateCleared) detachCycleObservationJobs()
        return stateCleared
    }

    private suspend fun transitionToFailure(
        cycleId: DevicePocCycleId,
        sessionId: ChampionSessionId?,
        error: ChampionBleError,
        expectedLease: ForegroundBleOperationLease,
    ) = cleanupTerminalFailure(cycleId, sessionId, error, expectedLease)

    private suspend fun transitionToProtocolFailure(
        cycleId: DevicePocCycleId,
        sessionId: ChampionSessionId,
        error: ChampionBleError,
        expectedLease: ForegroundBleOperationLease,
    ) = cleanupTerminalFailure(
        cycleId = cycleId,
        sessionId = sessionId,
        error = error,
        expectedLease = expectedLease,
        preserveFailureDetails = { current ->
            current.copy(
                isStepsCapabilityMissing = current.isStepsCapabilityMissing ||
                    (error is ChampionBleError.MissingOptionalChannel &&
                        error.channel == ChampionChannelAlias.FEA1_NOTIFY),
                isHeartRateCapabilityMissing = current.isHeartRateCapabilityMissing ||
                    (error is ChampionBleError.MissingOptionalChannel &&
                        error.channel == ChampionChannelAlias.F002_NOTIFY),
            )
        },
    )

    /**
     * Encerra somente ciclo PoC que ainda pertence a este coordenador.
     *
     * O gate impede que este caminho concorra com APP_WORKOUT_SESSION. A
     * checagem adicional do permit e do ChampionSessionId evita que callback
     * atrasado feche transporte de outra operação.
     */
    private suspend fun cleanupTerminalFailure(
        cycleId: DevicePocCycleId,
        sessionId: ChampionSessionId?,
        error: ChampionBleError,
        expectedLease: ForegroundBleOperationLease,
        preserveFailureDetails: (DeviceUiState) -> DeviceUiState = { it },
    ) {
        withContext(NonCancellable) {
            var releaseLease = false
            try {
                terminalCleanupMutex.withLock {
                    val beforeCleanup = state.value
                    val leaseAtCleanupStart = permitMutex.withLock { foregroundBleOperationLease }
                    if (leaseAtCleanupStart != expectedLease ||
                        expectedLease.permit.owner != ForegroundBleOperationOwner.DEVICE_CONNECTION ||
                        expectedLease.cycleId != cycleId ||
                        beforeCleanup.cycleId != cycleId ||
                        beforeCleanup.activeSessionId != sessionId ||
                        expectedLease.sessionId != sessionId
                    ) {
                        return@withLock
                    }
                    releaseLease = true

                    detachCycleObservationJobs()
                    var stateUpdated = false
                    updateState { current ->
                        if (current.cycleId != cycleId || current.activeSessionId != sessionId) {
                            current
                        } else {
                            stateUpdated = true
                            preserveFailureDetails(
                                current.copy(
                                    transportState = ChampionBleState.Disconnected,
                                    pocState = DevicePocState.Failed(error),
                                    candidate = null,
                                    activeSessionId = null,
                                    isHandshakeAssumed = false,
                                    activeHeartRateSessionId = null,
                                    heartRateStartedAt = null,
                                    lastD0Bpm = null,
                                    lastD0ObservedAt = null,
                                    lastError = error,
                                ),
                            )
                        }
                    }
                    if (!stateUpdated) return@withLock

                    try {
                        val currentTransport = connectionManager.currentSession()
                        val stillOwnsLease = permitMutex.withLock {
                            foregroundBleOperationLease == expectedLease
                        }
                        val canDisconnectTransport = stillOwnsLease &&
                            sessionId != null &&
                            currentTransport?.session?.id == sessionId
                        if (canDisconnectTransport) {
                            connectionManager.disconnect(ChampionDisconnectReason.TRANSPORT_FAILURE)
                        }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        // O estado terminal e a liberação do gate não dependem de
                        // uma segunda tentativa de transporte já considerado falho.
                    }
                }
            } finally {
                // Detach/cancel acontece antes, e fora desta seção de release,
                // para que um collector que se auto-cancele não retenha o gate.
                if (releaseLease) releaseForegroundBleOperationPermit(expectedLease)
            }
        }
    }

    private suspend fun disconnectExpectedTransport(
        expectedTransport: ChampionTransportSession,
        expectedCycleId: DevicePocCycleId,
        expectedSessionId: ChampionSessionId,
    ) {
        val compensationPermit = when (
            val result = foregroundBleOperationGate.tryAcquire(
                ForegroundBleOperationRequest(
                    owner = ForegroundBleOperationOwner.DEVICE_CONNECTION,
                    operationId = "device-poc-compensation-${expectedCycleId.value}-${expectedSessionId.value}",
                    requestedAt = java.time.Instant.now(),
                ),
            )
        ) {
            is ForegroundBleOperationAcquireResult.Acquired -> result.permit
            is ForegroundBleOperationAcquireResult.Rejected -> return
        }
        try {
            terminalCleanupMutex.withLock {
                val currentState = state.value
                val currentLease = permitMutex.withLock { foregroundBleOperationLease }
                val currentTransport = connectionManager.currentSession()
                val stateAllowsExpectedTransportCompensation = currentState.cycleId == expectedCycleId &&
                    currentState.activeSessionId == expectedSessionId &&
                    currentState.pocState in setOf(
                        DevicePocState.Connecting,
                        DevicePocState.TransportReady,
                    )
                if (!stateAllowsExpectedTransportCompensation ||
                    currentLease != null ||
                    currentTransport !== expectedTransport ||
                    currentTransport?.session?.id != expectedSessionId
                ) {
                    return@withLock
                }
                connectionManager.disconnect(ChampionDisconnectReason.CLEANUP)
            }
        } finally {
            foregroundBleOperationGate.release(compensationPermit)
        }
    }

    private suspend fun attemptHeartRateStop(sessionId: ChampionSessionId): ChampionBleResult<Unit> {
        val transport = currentTransportSession() ?: return ChampionBleResult.Success(Unit)
        if (transport.session.id != sessionId) return ChampionBleResult.Success(Unit)
        return transport.sendCommand(DevicePocHeartRateSession.createStopCommand())
    }

    private fun clearHeartRateSession(
        sessionId: ChampionSessionId,
        heartRateSessionId: DevicePocHeartRateSessionId,
        nextState: DevicePocState,
        finalSelection: DevicePocF002BpmEstimator.FinalSelection,
    ) {
        clearHeartRateRuntimeState()
        updateState {
            if (it.activeSessionId != sessionId || it.activeHeartRateSessionId != heartRateSessionId) {
                it
            } else {
                it.copy(
                    pocState = nextState,
                    activeHeartRateSessionId = null,
                    heartRateStartedAt = null,
                    d0PacketCount = 0,
                    f002PacketCount = 0,
                    f002BufferedSampleCount = 0,
                    lastD0Bpm = null,
                    lastD0ObservedAt = null,
                    finalHeartRateBpm = finalSelection.bpm,
                    finalHeartRateSource = finalSelection.source,
                )
            }
        }
    }

    private fun clearHeartRateRuntimeState() {
        val d0Job = d0ObservationJob
        d0ObservationJob = null
        val f002Job = f002ObservationJob
        f002ObservationJob = null
        d0Job?.cancel()
        f002Job?.cancel()
        f002Buffer.clear()
    }

    private fun detachCycleObservationJobs() {
        val stepsJob = stepsObservationJob
        stepsObservationJob = null
        stepsJob?.cancel()
        clearHeartRateRuntimeState()
    }

    private fun Throwable.isExpectedObserverShutdown(): Boolean =
        message?.startsWith("notification_hub_closed:") == true

    private fun inspectF002(
        samples: List<Int>,
        startedAt: java.time.Instant,
        observedAt: java.time.Instant,
    ): DevicePocF002BpmEstimator.Inspection {
        val durationSeconds = java.time.Duration.between(startedAt, observedAt).toMillis() / 1_000.0
        return DevicePocF002BpmEstimator.inspect(
            samples = samples,
            durationSeconds = durationSeconds,
        )
    }

    private fun currentHeartRateSelection(): DevicePocF002BpmEstimator.FinalSelection {
        val current = state.value
        return DevicePocF002BpmEstimator.selectFinalBpm(
            currentD0Bpm = current.lastD0Bpm,
            estimate = current.currentEstimate(),
        )
    }

    private fun DeviceUiState.currentEstimate(): DevicePocF002BpmEstimator.Estimate? {
        val bpm = estimatedF002Bpm ?: return null
        val quality = estimatedF002Quality ?: return null
        val sampleRateHz = estimatedF002SampleRateHz ?: return null
        return DevicePocF002BpmEstimator.Estimate(
            bpm = bpm,
            quality = quality,
            qualityScore = estimatedF002QualityScore,
            sampleRateHz = sampleRateHz,
            peakCount = estimatedF002PeakCount,
            validIntervalCount = 0,
            dynamicRange = 0,
            intervalCv = 0.0,
        )
    }

    private fun updateState(transform: (DeviceUiState) -> DeviceUiState) {
        val previous = mutableState.value
        val updated = transform(previous)
        mutableState.value = if (updated.pocState == previous.pocState) {
            updated
        } else {
            updated.copy(pocStateHistory = previous.pocStateHistory + updated.pocState.reportName)
        }
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

}
