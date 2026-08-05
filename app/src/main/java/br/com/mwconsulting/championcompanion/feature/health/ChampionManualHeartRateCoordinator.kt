package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionF002BpmEstimator
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionHeartRateFinalSource
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionHeartRateSignalQuality
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionManualHeartRateProtocol
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionTransportSession
import br.com.mwconsulting.championcompanion.core.bluetooth.toSafeMessage
import br.com.mwconsulting.championcompanion.core.common.BleDispatcher
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationAcquireResult
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationGate
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationOwner
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationPermit
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationRequest
import br.com.mwconsulting.championcompanion.core.model.MonotonicClock
import br.com.mwconsulting.championcompanion.core.model.MonotonicTimestamp
import br.com.mwconsulting.championcompanion.core.model.progressSince
import br.com.mwconsulting.championcompanion.core.model.remainingSince
import br.com.mwconsulting.championcompanion.feature.device.DeviceConnectionBootstrap
import java.time.Instant
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class ChampionManualHeartRateCoordinator(
    private val connectionManager: ChampionConnectionManager,
    private val connectionBootstrap: DeviceConnectionBootstrap,
    private val foregroundBleOperationGate: ForegroundBleOperationGate,
    private val monotonicClock: MonotonicClock,
    @BleDispatcher bleDispatcher: CoroutineDispatcher,
    private val targetDuration: Duration = ManualHeartRateTargetDuration,
    private val civilNow: () -> Instant = Instant::now,
) : ManualHeartRateCoordinator {
    private val scope = CoroutineScope(SupervisorJob() + bleDispatcher)
    private val mutableState = MutableStateFlow<HealthSurfaceState>(HealthSurfaceState.Idle)
    private val lifecycleMutex = Mutex()

    private var activeMeasurement: ActiveMeasurement? = null
    private var activeJob: Job? = null

    override val state: StateFlow<HealthSurfaceState> = mutableState.asStateFlow()

    init {
        scope.launch {
            connectionManager.state.collect(::handleBleStateChanged)
        }
    }

    override suspend fun startMeasurement(request: ManualHeartRateMeasurementRequest): HealthMeasurementError? {
        if (activeMeasurement != null) {
            return HealthMeasurementError(
                category = HealthMeasurementErrorCategory.MEASUREMENT_IN_PROGRESS,
                message = "Ja existe uma medicao manual em andamento.",
                isRetryable = false,
            )
        }

        val gateResult = foregroundBleOperationGate.tryAcquire(
            ForegroundBleOperationRequest(
                owner = ForegroundBleOperationOwner.MANUAL_HEART_RATE,
                operationId = request.manualMeasurementSessionId,
                requestedAt = civilNow(),
            ),
        )
        val permit = when (gateResult) {
            is ForegroundBleOperationAcquireResult.Acquired -> gateResult.permit
            is ForegroundBleOperationAcquireResult.Rejected -> {
                return HealthMeasurementError(
                    category = HealthMeasurementErrorCategory.BLE_OPERATION_IN_PROGRESS,
                    message = gateResult.activeOperation.owner.toHealthForegroundBleBlockMessage(),
                    isRetryable = true,
                ).also { error ->
                    mutableState.value = HealthSurfaceState.Failed(
                        session = ManualHeartRateSessionState(
                            manualMeasurementSessionId = request.manualMeasurementSessionId,
                            error = error,
                        ),
                    )
                }
            }
        }

        val baseSession = ManualHeartRateSessionState(
            manualMeasurementSessionId = request.manualMeasurementSessionId,
        )
        mutableState.value = HealthSurfaceState.Preparing(baseSession)

        activeJob?.cancel()
        activeJob = scope.launch {
            runMeasurement(request, permit, baseSession)
        }
        return null
    }

    override suspend fun cancelMeasurement(reason: ManualHeartRateCancellationReason) {
        val active = activeMeasurement ?: return
        requestTermination(
            active = active,
            termination = when (reason) {
                ManualHeartRateCancellationReason.USER_REQUESTED,
                ManualHeartRateCancellationReason.BACKGROUND,
                ManualHeartRateCancellationReason.EXIT_CONFIRMED,
                -> Termination.Cancelled(reason)

                ManualHeartRateCancellationReason.TIMEOUT -> Termination.Completed
                ManualHeartRateCancellationReason.INTERNAL_ERROR -> Termination.Failed(
                    HealthMeasurementError(
                        category = HealthMeasurementErrorCategory.INTERNAL,
                        message = "Nao foi possivel concluir a medicao manual.",
                        isRetryable = true,
                    ),
                )
            },
        )
    }

    override fun reset() {
        if (activeMeasurement == null) {
            mutableState.value = HealthSurfaceState.Idle
        }
    }

    private suspend fun runMeasurement(
        request: ManualHeartRateMeasurementRequest,
        permit: ForegroundBleOperationPermit,
        baseSession: ManualHeartRateSessionState,
    ) {
        var transport: ChampionTransportSession? = null
        var active: ActiveMeasurement? = null
        try {
            mutableState.value = HealthSurfaceState.Connecting(baseSession)
            transport = when (val result = resolveTransport(request)) {
                is ChampionBleResult.Success -> result.value
                is ChampionBleResult.Failure -> {
                    val error = result.error.toHealthError(HealthMeasurementErrorCategory.CONNECTION_FAILED)
                    mutableState.value = HealthSurfaceState.Failed(baseSession.copy(error = error))
                    return
                }
            }

            val bleSessionId = transport.session.id.value.toString()
            val handshakeResult = connectionBootstrap.executeHandshake(transport, ZonedDateTime.now())
            if (handshakeResult is ChampionBleResult.Failure) {
                val error = handshakeResult.error.toHealthError(HealthMeasurementErrorCategory.HANDSHAKE_FAILED)
                mutableState.value = HealthSurfaceState.Failed(baseSession.copy(bleSessionId = bleSessionId, error = error))
                return
            }

            val f002Ready = transport.notifications.ensureSubscribed(ChampionChannelAlias.F002_NOTIFY)
            if (f002Ready is ChampionBleResult.Failure) {
                val error = f002Ready.error.toHealthError(HealthMeasurementErrorCategory.F002_UNAVAILABLE)
                mutableState.value = HealthSurfaceState.Failed(baseSession.copy(bleSessionId = bleSessionId, error = error))
                return
            }

            val f002Subscription = transport.notifications.subscribe(ChampionChannelAlias.F002_NOTIFY)
            if (f002Subscription is ChampionBleResult.Failure) {
                val error = f002Subscription.error.toHealthError(HealthMeasurementErrorCategory.F002_UNAVAILABLE)
                mutableState.value = HealthSurfaceState.Failed(baseSession.copy(bleSessionId = bleSessionId, error = error))
                return
            }

            val startedAt = civilNow()
            val monotonicStartedAt = monotonicClock.now()
            val startedSession = baseSession.copy(
                bleSessionId = bleSessionId,
                startedAt = startedAt,
                monotonicStartedAt = monotonicStartedAt,
                remaining = targetDuration,
                progress = 0f,
            )
            mutableState.value = HealthSurfaceState.Starting(startedSession)

            active = ActiveMeasurement(
                permit = permit,
                manualMeasurementSessionId = request.manualMeasurementSessionId,
                bleSessionId = bleSessionId,
                transport = transport,
                startedAt = startedAt,
                monotonicStartedAt = monotonicStartedAt,
                completion = CompletableDeferred(),
            )
            lifecycleMutex.withLock {
                activeMeasurement = active
            }

            active.d0Job = scope.launch {
                try {
                    transport.streams.observeChannel(transport.session.id, ChampionChannelAlias.F008_NOTIFY).collect { packet ->
                        if (!isActiveMeasurement(active, packet.sessionId.value.toString())) return@collect
                        when (val parsed = ChampionManualHeartRateProtocol.parseLiveBpm(packet, startedAt)) {
                            ChampionManualHeartRateProtocol.D0Result.Ignored -> Unit
                            is ChampionManualHeartRateProtocol.D0Result.Valid -> {
                                active.lastD0Reading = parsed
                                updateMeasuringState(active)
                            }
                        }
                    }
                } catch (_: CancellationException) {
                    Unit
                } catch (error: RuntimeException) {
                    if (!error.isExpectedObserverShutdown()) throw error
                }
            }

            active.f002Job = scope.launch {
                try {
                    (f002Subscription as ChampionBleResult.Success).value.collect { packet ->
                        if (!isActiveMeasurement(active, packet.sessionId.value.toString())) return@collect
                        val samples = ChampionManualHeartRateProtocol.parseF002Samples(packet)
                        ChampionManualHeartRateProtocol.appendLimitedBuffer(
                            buffer = active.f002Buffer,
                            samples = samples,
                            maxSamples = ChampionManualHeartRateProtocol.defaultMaxBufferedSamples,
                        )
                        val durationSeconds = java.time.Duration.between(startedAt, packet.observedAt).toMillis() / 1_000.0
                        val inspection = ChampionF002BpmEstimator.inspect(
                            samples = active.f002Buffer,
                            durationSeconds = durationSeconds,
                        )
                        active.latestInspection = inspection
                        updateMeasuringState(active)
                    }
                } catch (_: CancellationException) {
                    Unit
                } catch (error: RuntimeException) {
                    if (!error.isExpectedObserverShutdown()) throw error
                }
            }

            val startResult = transport.sendCommand(ChampionManualHeartRateProtocol.createStartCommand())
            if (startResult is ChampionBleResult.Failure) {
                requestTermination(
                    active = active,
                    termination = Termination.Failed(
                        startResult.error.toHealthError(HealthMeasurementErrorCategory.START_FAILED),
                    ),
                )
            } else {
                active.didSendStartCommand = true
                mutableState.value = HealthSurfaceState.Measuring(startedSession)
                active.tickerJob = scope.launch {
                    while (!active.completion.isCompleted) {
                        val remaining = monotonicClock.remainingSince(monotonicStartedAt, targetDuration)
                        val progress = monotonicClock.progressSince(monotonicStartedAt, targetDuration)
                        updateMeasuringState(active, progress = progress, remaining = remaining)
                        if (remaining == Duration.ZERO) {
                            requestTermination(active, Termination.Completed)
                            break
                        }
                        delay(250.milliseconds)
                    }
                }
            }

            val termination = active.completion.await()
            finalizeMeasurement(active, termination)
        } finally {
            active?.let { cleanupMeasurement(it) }
            foregroundBleOperationGate.release(permit)
            lifecycleMutex.withLock {
                if (activeMeasurement?.manualMeasurementSessionId == request.manualMeasurementSessionId) {
                    activeMeasurement = null
                }
            }
        }
    }

    private suspend fun finalizeMeasurement(
        active: ActiveMeasurement,
        termination: Termination,
    ) {
        val currentState = currentSessionState(active)
        val stopError = attemptStopIfSafe(active)
        val finalSelection = ChampionF002BpmEstimator.selectFinalBpm(
            currentD0Bpm = active.lastD0Reading?.bpm,
            estimate = active.latestInspection?.estimate,
        )
        val currentReading = currentReadingFor(active)
        val quality = active.latestInspection?.quality?.toHealthQuality()
        val finalSession = currentState.copy(
            progress = 1f.coerceAtLeast(currentState.progress),
            remaining = Duration.ZERO,
            currentReading = currentReading,
            signalQuality = quality,
        )

        mutableState.value = when (termination) {
            is Termination.Cancelled -> HealthSurfaceState.Cancelled(
                session = finalSession.copy(error = stopError),
            )

            is Termination.Failed -> HealthSurfaceState.Failed(
                session = finalSession.copy(error = stopError ?: termination.error),
            )

            Termination.Completed -> {
                val result = finalSelection.bpm?.let { bpm ->
                    ManualHeartRateResult(
                        bpm = bpm,
                        source = finalSelection.source.toHealthFinalSource(),
                        measuredAt = active.lastD0Reading?.observedAt ?: civilNow(),
                    )
                }
                if (result == null) {
                    HealthSurfaceState.Failed(
                        session = finalSession.copy(
                            error = stopError ?: HealthMeasurementError(
                                category = HealthMeasurementErrorCategory.NO_VALID_READING,
                                message = "Nenhum BPM valido foi encontrado para concluir a medicao.",
                                isRetryable = true,
                            ),
                        ),
                    )
                } else {
                    HealthSurfaceState.Completed(
                        session = finalSession.copy(
                            finalResult = result,
                            error = stopError,
                        ),
                    )
                }
            }
        }
    }

    private suspend fun cleanupMeasurement(active: ActiveMeasurement) {
        active.tickerJob?.cancel()
        active.d0Job?.cancel()
        active.f002Job?.cancel()
        active.f002Buffer.clear()
    }

    private suspend fun resolveTransport(
        request: ManualHeartRateMeasurementRequest,
    ): ChampionBleResult<ChampionTransportSession> {
        val currentSession = connectionManager.currentSession()
        if (currentSession != null && currentSession.session.transportKey == request.associatedDevice.platformIdentifier) {
            return ChampionBleResult.Success(currentSession)
        }
        return when (val result = connectionBootstrap.connectAssociatedDevice(request.associatedDevice.toDiscoveredDevice())) {
            is ChampionBleResult.Success -> ChampionBleResult.Success(result.value.transport)
            is ChampionBleResult.Failure -> ChampionBleResult.Failure(result.error)
        }
    }

    private suspend fun attemptStopIfSafe(active: ActiveMeasurement): HealthMeasurementError? {
        if (!active.didSendStartCommand) return null
        val currentTransport = connectionManager.currentSession()
        if (currentTransport?.session?.id != active.transport.session.id) return null
        mutableState.value = HealthSurfaceState.Stopping(currentSessionState(active))
        return when (val result = active.transport.sendCommand(ChampionManualHeartRateProtocol.createStopCommand())) {
            is ChampionBleResult.Success -> null
            is ChampionBleResult.Failure -> result.error.toHealthError(HealthMeasurementErrorCategory.STOP_FAILED)
        }
    }

    private fun updateMeasuringState(
        active: ActiveMeasurement,
        progress: Float = monotonicClock.progressSince(active.monotonicStartedAt, targetDuration),
        remaining: Duration = monotonicClock.remainingSince(active.monotonicStartedAt, targetDuration),
    ) {
        if (!isActiveMeasurement(active, active.bleSessionId)) return
        val session = currentSessionState(active).copy(
            progress = progress,
            remaining = remaining,
            currentReading = currentReadingFor(active),
            signalQuality = active.latestInspection?.quality?.toHealthQuality(),
        )
        mutableState.value = HealthSurfaceState.Measuring(session)
    }

    private fun currentReadingFor(active: ActiveMeasurement): ManualHeartRateCurrentReading? {
        val d0 = active.lastD0Reading
        if (d0 != null) {
            return ManualHeartRateCurrentReading(
                bpm = d0.bpm,
                source = ManualHeartRateReadingSource.D0_LIVE,
                observedAt = d0.observedAt,
            )
        }
        val estimate = active.latestInspection?.estimate ?: return null
        return ManualHeartRateCurrentReading(
            bpm = estimate.bpm,
            source = ManualHeartRateReadingSource.F002_ESTIMATE,
            observedAt = civilNow(),
        )
    }

    private fun currentSessionState(active: ActiveMeasurement): ManualHeartRateSessionState = when (val state = mutableState.value) {
        is HealthSurfaceState.Preparing -> state.session
        is HealthSurfaceState.Connecting -> state.session
        is HealthSurfaceState.Starting -> state.session
        is HealthSurfaceState.Measuring -> state.session
        is HealthSurfaceState.Stopping -> state.session
        is HealthSurfaceState.Completed -> state.session
        is HealthSurfaceState.Cancelled -> state.session
        is HealthSurfaceState.Failed -> state.session
        HealthSurfaceState.Idle -> ManualHeartRateSessionState(
            manualMeasurementSessionId = active.manualMeasurementSessionId,
            bleSessionId = active.bleSessionId,
            startedAt = active.startedAt,
            monotonicStartedAt = active.monotonicStartedAt,
            remaining = targetDuration,
        )
    }

    private suspend fun requestTermination(
        active: ActiveMeasurement,
        termination: Termination,
    ) {
        lifecycleMutex.withLock {
            if (activeMeasurement !== active || active.isFinishing || active.completion.isCompleted) return
            active.isFinishing = true
            active.completion.complete(termination)
        }
    }

    private suspend fun handleBleStateChanged(state: ChampionBleState) {
        val active = activeMeasurement ?: return
        when (state) {
            ChampionBleState.Disconnected -> {
                requestTermination(
                    active,
                    Termination.Failed(
                        HealthMeasurementError(
                            category = HealthMeasurementErrorCategory.UNEXPECTED_DISCONNECT,
                            message = "Conexao com relogio foi encerrada durante a medicao.",
                            isRetryable = true,
                        ),
                    ),
                )
            }

            is ChampionBleState.Disconnecting -> {
                if (state.session?.id?.value?.toString() == active.bleSessionId) {
                    requestTermination(
                        active,
                        Termination.Failed(
                            HealthMeasurementError(
                                category = HealthMeasurementErrorCategory.UNEXPECTED_DISCONNECT,
                                message = "Conexao com relogio foi encerrada durante a medicao.",
                                isRetryable = true,
                            ),
                        ),
                    )
                }
            }

            is ChampionBleState.Failed -> {
                if (state.session?.id?.value?.toString() == active.bleSessionId) {
                    requestTermination(active, Termination.Failed(state.error.toHealthError()))
                }
            }

            is ChampionBleState.Connecting,
            is ChampionBleState.DiscoveringServices,
            is ChampionBleState.ResolvingChannels,
            is ChampionBleState.ConfiguringSubscriptions,
            is ChampionBleState.TransportReady,
            is ChampionBleState.Scanning,
            -> Unit
        }
    }

    private fun isActiveMeasurement(active: ActiveMeasurement, bleSessionId: String): Boolean =
        activeMeasurement === active && active.bleSessionId == bleSessionId && !active.completion.isCompleted

    private fun br.com.mwconsulting.championcompanion.core.model.Device.toDiscoveredDevice() =
        br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice(
            transportKey = platformIdentifier,
            name = name,
            localName = name,
            observedAt = civilNow(),
        )

    private fun ChampionBleError.toHealthError(
        defaultCategory: HealthMeasurementErrorCategory = HealthMeasurementErrorCategory.CONNECTION_FAILED,
    ): HealthMeasurementError = when (this) {
        ChampionBleError.BluetoothDisabled -> HealthMeasurementError(
            category = HealthMeasurementErrorCategory.BLUETOOTH_DISABLED,
            message = "Ative o Bluetooth para medir frequencia cardiaca.",
            isRetryable = true,
        )

        ChampionBleError.ScanPermissionMissing,
        ChampionBleError.ConnectPermissionMissing,
        ChampionBleError.LegacyPermissionMissing,
        ChampionBleError.LocationPermissionRequired,
        -> HealthMeasurementError(
            category = HealthMeasurementErrorCategory.PERMISSIONS_REQUIRED,
            message = "Permissoes de Bluetooth sao necessarias para medir frequencia cardiaca.",
            isRetryable = true,
        )

        ChampionBleError.DeviceNotFound -> HealthMeasurementError(
            category = HealthMeasurementErrorCategory.DEVICE_NOT_FOUND,
            message = "Nao foi possivel localizar relogio associado.",
            isRetryable = true,
        )

        is ChampionBleError.MissingOptionalChannel,
        is ChampionBleError.MissingRequiredChannel,
        is ChampionBleError.SubscriptionFailed,
        -> HealthMeasurementError(
            category = if (
                (this is ChampionBleError.MissingOptionalChannel && channel == ChampionChannelAlias.F002_NOTIFY) ||
                (this is ChampionBleError.MissingRequiredChannel && channel == ChampionChannelAlias.F002_NOTIFY) ||
                (this is ChampionBleError.SubscriptionFailed && channel == ChampionChannelAlias.F002_NOTIFY)
            ) {
                HealthMeasurementErrorCategory.F002_UNAVAILABLE
            } else {
                defaultCategory
            },
            message = toSafeMessage(),
            isRetryable = isRecoverable,
        )

        is ChampionBleError.OperationCancelled,
        is ChampionBleError.QueueClosed,
        ChampionBleError.CleanupInProgress,
        -> HealthMeasurementError(
            category = HealthMeasurementErrorCategory.CANCELLED,
            message = "Medicao cancelada antes da conclusao.",
            isRetryable = true,
        )

        is ChampionBleError.Timeout,
        is ChampionBleError.ConnectionFailed,
        is ChampionBleError.DiscoveryFailed,
        is ChampionBleError.UnexpectedDisconnect,
        is ChampionBleError.InvalidPayload,
        is ChampionBleError.OperationRejected,
        is ChampionBleError.CallbackFailure,
        is ChampionBleError.IncompatibleChannelProperties,
        is ChampionBleError.MissingRequiredService,
        ChampionBleError.OperationUnavailable,
        ChampionBleError.UnexpectedInternal,
        ChampionBleError.ScanAlreadyInProgress,
        ChampionBleError.BleNotSupported,
        is ChampionBleError.StaleSession,
        is ChampionBleError.ScanFailed,
        -> HealthMeasurementError(
            category = defaultCategory,
            message = toSafeMessage(),
            isRetryable = isRecoverable,
        )
    }

    private fun ChampionHeartRateSignalQuality.toHealthQuality(): ManualHeartRateSignalQuality = when (this) {
        ChampionHeartRateSignalQuality.GOOD -> ManualHeartRateSignalQuality.GOOD
        ChampionHeartRateSignalQuality.MEDIUM -> ManualHeartRateSignalQuality.REGULAR
        ChampionHeartRateSignalQuality.LOW -> ManualHeartRateSignalQuality.LOW
    }

    private fun ChampionHeartRateFinalSource.toHealthFinalSource(): ManualHeartRateFinalSource = when (this) {
        ChampionHeartRateFinalSource.D0_CURRENT -> ManualHeartRateFinalSource.D0_FINAL
        ChampionHeartRateFinalSource.F002_ESTIMATE -> ManualHeartRateFinalSource.F002_FINAL
        ChampionHeartRateFinalSource.UNAVAILABLE -> ManualHeartRateFinalSource.F002_FINAL
    }

    private fun Throwable.isExpectedObserverShutdown(): Boolean =
        message?.startsWith("notification_hub_closed:") == true

    private fun ForegroundBleOperationOwner.toHealthForegroundBleBlockMessage(): String = when (this) {
        ForegroundBleOperationOwner.DAILY_STEPS_REFRESH -> "Uma sincronização do relógio está em andamento na tela Início."
        ForegroundBleOperationOwner.MANUAL_HEART_RATE -> "Uma medição manual já está em andamento na tela Saúde."
        ForegroundBleOperationOwner.WATCH_D1_HISTORY -> "Uma importação de histórico do relógio está em andamento na tela Saúde."
        ForegroundBleOperationOwner.WATCH_WORKOUT_IMPORT -> "Uma importação de treinos do relógio está em andamento na tela Exercícios."
        ForegroundBleOperationOwner.APP_WORKOUT_SESSION -> "A participação opcional do relógio está em andamento na sessão de treino."
        ForegroundBleOperationOwner.DEVICE_CONNECTION -> "Uma conexão do dispositivo está em andamento na tela Dispositivo."
    }

    private data class ActiveMeasurement(
        val permit: ForegroundBleOperationPermit,
        val manualMeasurementSessionId: String,
        val bleSessionId: String,
        val transport: ChampionTransportSession,
        val startedAt: Instant,
        val monotonicStartedAt: MonotonicTimestamp,
        val completion: CompletableDeferred<Termination>,
        val f002Buffer: MutableList<Int> = mutableListOf(),
        var latestInspection: ChampionF002BpmEstimator.Inspection? = null,
        var lastD0Reading: ChampionManualHeartRateProtocol.D0Result.Valid? = null,
        var didSendStartCommand: Boolean = false,
        var isFinishing: Boolean = false,
        var tickerJob: Job? = null,
        var d0Job: Job? = null,
        var f002Job: Job? = null,
    )

    private sealed interface Termination {
        data object Completed : Termination
        data class Cancelled(val reason: ManualHeartRateCancellationReason) : Termination
        data class Failed(val error: HealthMeasurementError) : Termination
    }
}
