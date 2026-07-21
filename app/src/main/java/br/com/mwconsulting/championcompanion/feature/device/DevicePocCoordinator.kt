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
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.coroutines.coroutineContext

class DevicePocCoordinator @Inject constructor(
    private val connectionManager: ChampionConnectionManager,
    @BleDispatcher bleDispatcher: CoroutineDispatcher,
) : DeviceFeatureContract {
    private val scope = CoroutineScope(SupervisorJob() + bleDispatcher)
    private val mutableState = MutableStateFlow(DeviceUiState())
    private var handshakeObservationJob: Job? = null
    private var stepsObservationJob: Job? = null
    private var d0ObservationJob: Job? = null
    private var f002ObservationJob: Job? = null
    private val f002Buffer = mutableListOf<Int>()

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

    suspend fun startHeartRateSession(
        maxBufferedSamples: Int = DevicePocHeartRateSession.DEFAULT_MAX_BUFFERED_SAMPLES,
    ): ChampionBleResult<Unit> {
        val current = state.value
        val sessionId = current.activeSessionId ?: return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
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
        if (!transport.capabilities.snapshot.hasChannel(ChampionChannelAlias.F002_NOTIFY)) {
            val error = ChampionBleError.MissingOptionalChannel(ChampionChannelAlias.F002_NOTIFY)
            transitionToProtocolFailure(sessionId, error)
            return ChampionBleResult.Failure(error)
        }
        val ensureF002 = transport.notifications.ensureSubscribed(ChampionChannelAlias.F002_NOTIFY)
        if (ensureF002 is ChampionBleResult.Failure) {
            transitionToProtocolFailure(sessionId, ensureF002.error)
            return ChampionBleResult.Failure(ensureF002.error)
        }
        val f002Subscription = transport.notifications.subscribe(ChampionChannelAlias.F002_NOTIFY)
        if (f002Subscription is ChampionBleResult.Failure) {
            transitionToProtocolFailure(sessionId, f002Subscription.error)
            return ChampionBleResult.Failure(f002Subscription.error)
        }

        val heartRateSessionId = DevicePocHeartRateSessionId.random()
        val startedAt = java.time.Instant.now()
        clearHeartRateRuntimeState()
        updateState {
            it.copy(
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
                transitionToProtocolFailure(sessionId, result.error)
                ChampionBleResult.Failure(result.error)
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
        if (!transport.capabilities.snapshot.hasChannel(ChampionChannelAlias.FEA1_NOTIFY)) {
            val error = ChampionBleError.MissingOptionalChannel(ChampionChannelAlias.FEA1_NOTIFY)
            transitionToProtocolFailure(sessionId, error)
            return ChampionBleResult.Failure(error)
        }
        if (stepsObservationJob?.isActive == true) {
            moveToWaitingForSteps(sessionId)
            return ChampionBleResult.Success(Unit)
        }

        val subscription = transport.notifications.subscribe(ChampionChannelAlias.FEA1_NOTIFY)
        if (subscription is ChampionBleResult.Failure) {
            transitionToProtocolFailure(sessionId, subscription.error)
            return ChampionBleResult.Failure(subscription.error)
        }

        moveToWaitingForSteps(sessionId)
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
                            transitionToProtocolFailure(sessionId, parsed.error)
                            stepsObservationJob?.cancel()
                        }
                    }
                }
            } catch (_: CancellationException) {
                Unit
            }
        }
        return ChampionBleResult.Success(Unit)
    }

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

        val handshakeSubscription = transport.notifications.subscribe(ChampionChannelAlias.F008_NOTIFY)
        if (handshakeSubscription is ChampionBleResult.Failure) {
            transitionToProtocolFailure(sessionId, handshakeSubscription.error)
            return ChampionBleResult.Failure(handshakeSubscription.error)
        }

        moveToHandshaking(sessionId)
        observeHandshakeConfirmation(
            sessionId = sessionId,
            packets = (handshakeSubscription as ChampionBleResult.Success).value,
            timeout = timeout,
        )
        delay(DevicePocHandshakeA1.initialDelay)
        var attempt = 1
        var result: ChampionBleResult<*> = ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        while (true) {
            result = transport.sendCommand(
                DevicePocHandshakeA1.createCommand(
                    dateTime = dateTime,
                    timeout = timeout,
                ),
            )
            val error = (result as? ChampionBleResult.Failure)?.error ?: break
            if (!DevicePocHandshakeA1.shouldRetry(error, attempt)) break
            delay(DevicePocHandshakeA1.retryDelay)
            attempt += 1
        }
        val latest = state.value
        if (latest.activeSessionId != sessionId) {
            return ChampionBleResult.Failure(ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP))
        }

        return when (result) {
            is ChampionBleResult.Success -> ChampionBleResult.Success(Unit)

            is ChampionBleResult.Failure -> {
                handshakeObservationJob?.cancel()
                handshakeObservationJob = null
                transitionToFailure(sessionId, result.error)
                ChampionBleResult.Failure(result.error)
            }
        }
    }

    private fun observeHandshakeConfirmation(
        sessionId: ChampionSessionId,
        packets: Flow<ChampionPacket>,
        timeout: Duration,
    ) {
        handshakeObservationJob?.cancel()
        handshakeObservationJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val currentJob = coroutineContext[Job]
            try {
                withTimeout(timeout) {
                    packets.first { packet ->
                        state.value.activeSessionId == sessionId && DevicePocHandshakeA1.isConfirmationPacket(packet)
                    }
                }
                if (state.value.activeSessionId == sessionId) {
                    moveToHandshakeConfirmed(sessionId)
                }
            } catch (_: CancellationException) {
                Unit
            } catch (_: TimeoutCancellationException) {
                val current = state.value
                if (current.activeSessionId == sessionId && current.pocState == DevicePocState.Handshaking) {
                    transitionToFailure(
                        sessionId,
                        ChampionBleError.Timeout(ChampionOperationType.WAIT_FOR_RESPONSE, timeout),
                    )
                }
            } finally {
                if (handshakeObservationJob === currentJob) {
                    handshakeObservationJob = null
                }
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
        handshakeObservationJob?.cancel()
        handshakeObservationJob = null
        stepsObservationJob?.cancel()
        stepsObservationJob = null
        clearHeartRateRuntimeState()
        updateState {
            it.copy(
                transportState = ChampionBleState.Disconnected,
                pocState = nextState,
                candidate = null,
                activeSessionId = null,
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

    private fun transitionToFailure(
        sessionId: ChampionSessionId?,
        error: ChampionBleError,
    ) {
        handshakeObservationJob?.cancel()
        handshakeObservationJob = null
        stepsObservationJob?.cancel()
        stepsObservationJob = null
        clearHeartRateRuntimeState()
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

    private fun transitionToProtocolFailure(
        sessionId: ChampionSessionId,
        error: ChampionBleError,
    ) {
        if (state.value.activeSessionId != sessionId) return
        handshakeObservationJob?.cancel()
        handshakeObservationJob = null
        stepsObservationJob?.cancel()
        stepsObservationJob = null
        clearHeartRateRuntimeState()
        updateState {
            if (it.activeSessionId != sessionId) {
                it
            } else {
                it.copy(
                    pocState = DevicePocState.Failed(error),
                    isStepsCapabilityMissing = error is ChampionBleError.MissingOptionalChannel &&
                        error.channel == ChampionChannelAlias.FEA1_NOTIFY,
                    isHeartRateCapabilityMissing = error is ChampionBleError.MissingOptionalChannel &&
                        error.channel == ChampionChannelAlias.F002_NOTIFY,
                    lastError = error,
                )
            }
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
        d0ObservationJob?.cancel()
        d0ObservationJob = null
        f002ObservationJob?.cancel()
        f002ObservationJob = null
        f002Buffer.clear()
    }

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

    private fun placeholderSession(sessionId: ChampionSessionId) =
        br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSession(
            id = sessionId,
            transportKey = "poc-${sessionId.value}",
            connectedAt = java.time.Instant.EPOCH,
        )
}
