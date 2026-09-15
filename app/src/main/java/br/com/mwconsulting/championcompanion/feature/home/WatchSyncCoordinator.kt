package br.com.mwconsulting.championcompanion.feature.home

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDisconnectReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionTransportSession
import br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshRequest
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportStatus
import br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshResult
import br.com.mwconsulting.championcompanion.core.model.DailySyncAttempt
import br.com.mwconsulting.championcompanion.core.model.DailySummaryRepository
import br.com.mwconsulting.championcompanion.core.model.DailySummaryPolicy
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationAcquireResult
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationGate
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationOwner
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationPermit
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationRequest
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsRepository
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportStatus
import br.com.mwconsulting.championcompanion.feature.device.DeviceConnectionBootstrap
import br.com.mwconsulting.championcompanion.feature.exercises.WatchSyncWorkoutComponent
import br.com.mwconsulting.championcompanion.feature.exercises.WatchWorkoutImportRequest
import br.com.mwconsulting.championcompanion.feature.health.D1HeartRateSyncRequest
import br.com.mwconsulting.championcompanion.feature.health.WatchSyncD1Component
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex

interface WatchSyncCoordinator {
    val state: StateFlow<WatchSyncState>

    suspend fun sync(request: WatchSyncRequest): WatchSyncResult

    fun reset()
}

data class WatchSyncRequest(
    val attemptId: String,
    val associatedDevice: Device,
    val baseLocalDate: LocalDate,
    val zoneId: ZoneId,
    val startedAt: Instant = Instant.now(),
)

enum class WatchSyncStage {
    Preparing,
    Connecting,
    FEA1,
    D1,
    D3_D4,
}

enum class WatchSyncComponentStatus {
    NOT_STARTED,
    RUNNING,
    COMPLETED,
    PARTIAL,
    FAILED,
    SKIPPED,
}

data class WatchSyncComponentState(
    val status: WatchSyncComponentStatus = WatchSyncComponentStatus.NOT_STARTED,
    val startedAt: Instant? = null,
    val finishedAt: Instant? = null,
    val message: String? = null,
)

data class WatchSyncComponents(
    val fea1: WatchSyncComponentState = WatchSyncComponentState(),
    val d1: WatchSyncComponentState = WatchSyncComponentState(),
    val d3D4: WatchSyncComponentState = WatchSyncComponentState(),
)

enum class WatchSyncResultStatus {
    COMPLETED,
    PARTIAL,
    FAILED,
}

data class WatchSyncError(
    val category: WatchSyncErrorCategory,
    val message: String,
    val isRetryable: Boolean,
)

enum class WatchSyncErrorCategory {
    BLE_OPERATION_IN_PROGRESS,
    CONNECTION_FAILED,
    HANDSHAKE_FAILED,
    TIMEOUT,
    INVALID_PACKET,
    STORAGE,
    PARTIAL,
    CANCELLED,
    INTERNAL,
}

data class WatchSyncState(
    val attemptId: String? = null,
    val sessionId: String? = null,
    val startedAt: Instant? = null,
    val stage: WatchSyncStage? = null,
    val components: WatchSyncComponents = WatchSyncComponents(),
    val result: WatchSyncResultStatus? = null,
    val error: WatchSyncError? = null,
) {
    val isRunning: Boolean
        get() = attemptId != null && result == null
}

data class WatchSyncResult(
    val attemptId: String,
    val sessionId: String?,
    val startedAt: Instant,
    val finishedAt: Instant,
    val status: WatchSyncResultStatus,
    val components: WatchSyncComponents,
    val error: WatchSyncError? = null,
    val fea1: DailyStepsRefreshResult? = null,
)

@Singleton
class ChampionWatchSyncCoordinator @Inject constructor(
    private val connectionManager: ChampionConnectionManager,
    private val connectionBootstrap: DeviceConnectionBootstrap,
    private val dailyStepsSyncCoordinator: ChampionDailyStepsSyncCoordinator,
    private val d1Component: WatchSyncD1Component,
    private val workoutComponent: WatchSyncWorkoutComponent,
    private val dailySummaryRepository: DailySummaryRepository,
    private val personalMetricsRepository: PersonalMetricsRepository,
    private val foregroundBleOperationGate: ForegroundBleOperationGate,
) : WatchSyncCoordinator {
    private val syncMutex = Mutex()
    private val mutableState = MutableStateFlow(WatchSyncState())

    override val state: StateFlow<WatchSyncState> = mutableState.asStateFlow()

    override fun reset() {
        if (!mutableState.value.isRunning) {
            mutableState.value = WatchSyncState()
        }
    }

    override suspend fun sync(request: WatchSyncRequest): WatchSyncResult {
        if (!syncMutex.tryLock()) {
            val now = Instant.now()
            val error = WatchSyncError(
                category = WatchSyncErrorCategory.BLE_OPERATION_IN_PROGRESS,
                message = "Uma sincronização do relógio já está em andamento.",
                isRetryable = true,
            )
            return WatchSyncResult(
                attemptId = request.attemptId,
                sessionId = null,
                startedAt = request.startedAt,
                finishedAt = now,
                status = WatchSyncResultStatus.FAILED,
                components = WatchSyncComponents(),
                error = error,
            )
        }

        var permit: ForegroundBleOperationPermit? = null
        try {
            mutableState.value = WatchSyncState(
                attemptId = request.attemptId,
                startedAt = request.startedAt,
                stage = WatchSyncStage.Preparing,
            )

            val gateResult = foregroundBleOperationGate.tryAcquire(
                ForegroundBleOperationRequest(
                    owner = ForegroundBleOperationOwner.WATCH_SYNC,
                    operationId = request.attemptId,
                    requestedAt = request.startedAt,
                ),
            )
            permit = when (gateResult) {
                is ForegroundBleOperationAcquireResult.Acquired -> gateResult.permit
                is ForegroundBleOperationAcquireResult.Rejected -> {
                    val error = WatchSyncError(
                        category = WatchSyncErrorCategory.BLE_OPERATION_IN_PROGRESS,
                        message = gateResult.activeOperation.owner.toWatchSyncBlockMessage(),
                        isRetryable = true,
                    )
                    return finish(
                        request = request,
                        status = WatchSyncResultStatus.FAILED,
                        error = error,
                    )
                }
            }

            mutableState.update { it.copy(stage = WatchSyncStage.Connecting) }
            val transport = when (val transportResult = resolveTransport(request)) {
                is ChampionBleResult.Success -> transportResult.value
                is ChampionBleResult.Failure -> {
                    val error = transportResult.error.toWatchSyncError()
                    mutableState.update { current ->
                        current.copy(
                            components = current.components.copy(
                                fea1 = skippedComponent(error.message),
                                d1 = skippedComponent(error.message),
                                d3D4 = skippedComponent(error.message),
                            ),
                        )
                    }
                    return finish(
                        request = request,
                        status = WatchSyncResultStatus.FAILED,
                        error = error,
                    )
                }
            }
            mutableState.update { it.copy(sessionId = transport.session.id.value.toString()) }

            val fea1Result = runFea1Safely(request, transport)
            runD1Safely(request, transport)
            runD3D4Safely(request, transport)
            val finalStatus = globalStatus(mutableState.value.components)
            val finalError = globalError(mutableState.value.components, finalStatus)
            return finish(
                request = request,
                status = finalStatus,
                error = finalError,
                fea1 = fea1Result,
            )
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            val safeError = WatchSyncError(
                category = WatchSyncErrorCategory.CANCELLED,
                message = "A sincronização foi cancelada antes de concluir.",
                isRetryable = true,
            )
            mutableState.update { current ->
                current.copy(
                    result = WatchSyncResultStatus.FAILED,
                    error = safeError,
                    components = current.markRunningAsFailed(safeError.message),
                )
            }
            throw cancellation
        } catch (error: Exception) {
            val safeError = WatchSyncError(
                category = WatchSyncErrorCategory.INTERNAL,
                message = "Não foi possível concluir a sincronização do relógio.",
                isRetryable = true,
            )
            mutableState.update { current ->
                current.copy(
                    result = WatchSyncResultStatus.FAILED,
                    error = safeError,
                    components = current.markRunningAsFailed(safeError.message),
                )
            }
            return state.value.toResult(request, safeError)
        } finally {
            permit?.let { foregroundBleOperationGate.release(it) }
            syncMutex.unlock()
        }
    }

    private suspend fun resolveTransport(request: WatchSyncRequest): ChampionBleResult<ChampionTransportSession> {
        val current = connectionManager.currentSession()
        if (current != null && current.session.transportKey == request.associatedDevice.platformIdentifier) {
            return when (val handshake = connectionBootstrap.executeHandshake(current, ZonedDateTime.now())) {
                is ChampionBleResult.Success -> ChampionBleResult.Success(current)
                is ChampionBleResult.Failure -> {
                    connectionManager.disconnectExpectedTransport(
                        expectedTransport = current,
                        reason = ChampionDisconnectReason.CLEANUP,
                    )
                    ChampionBleResult.Failure(handshake.error)
                }
            }
        }
        return when (val result = connectionBootstrap.connectAssociatedDevice(
            device = request.associatedDevice.toDiscoveredDevice(),
            dateTime = ZonedDateTime.now(),
        )) {
            is ChampionBleResult.Success -> ChampionBleResult.Success(result.value.transport)
            is ChampionBleResult.Failure -> ChampionBleResult.Failure(result.error)
        }
    }

    private suspend fun runFea1(
        request: WatchSyncRequest,
        transport: ChampionTransportSession,
    ): DailyStepsRefreshResult? {
        val startedAt = Instant.now()
        startComponent(WatchSyncComponent.FEA1, startedAt)
        mutableState.update { it.copy(stage = WatchSyncStage.FEA1) }
        val dailyRequest = DailyStepsRefreshRequest(
            attempt = DailySyncAttempt(
                refreshAttemptId = request.attemptId,
                startedAt = request.startedAt,
            ),
            associatedDeviceId = request.associatedDevice.id,
            associatedDevicePlatformIdentifier = request.associatedDevice.platformIdentifier,
            zoneId = request.zoneId,
        )
        return when (val outcome = dailyStepsSyncCoordinator.refreshWithTransport(dailyRequest, transport)) {
            is br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshOutcome.Failure -> {
                finishComponent(
                    component = WatchSyncComponent.FEA1,
                    status = WatchSyncComponentStatus.FAILED,
                    finishedAt = Instant.now(),
                    message = outcome.reason.toWatchSyncMessage(),
                )
                null
            }

            is br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshOutcome.Success -> {
                val result = outcome.value
                val metrics = personalMetricsRepository.observeSnapshot().first()
                val estimates = DailySummaryPolicy.estimate(result.steps, metrics)
                when (dailySummaryRepository.mergeWatchDailySummary(
                    br.com.mwconsulting.championcompanion.core.model.MergeWatchDailySummaryInput(
                        deviceId = result.deviceId,
                        localDate = result.localDate,
                        incomingSteps = result.steps,
                        collectedAt = result.collectedAt,
                        updatedAt = Instant.now(),
                        estimatedDistanceMeters = estimates.distanceMeters,
                        estimatedCaloriesKilocalories = estimates.caloriesKilocalories,
                        parserVersion = result.parserVersion,
                        zoneId = request.zoneId,
                    ),
                )) {
                    is RepositoryResult.Success -> {
                        finishComponent(
                            component = WatchSyncComponent.FEA1,
                            status = WatchSyncComponentStatus.COMPLETED,
                            finishedAt = Instant.now(),
                            message = "Resumo diário atualizado.",
                        )
                        result
                    }

                    RepositoryResult.StorageUnavailable,
                    RepositoryResult.Conflict,
                    RepositoryResult.NotFound,
                    RepositoryResult.UnexpectedState,
                    -> {
                        finishComponent(
                            component = WatchSyncComponent.FEA1,
                            status = WatchSyncComponentStatus.FAILED,
                            finishedAt = Instant.now(),
                            message = "Não foi possível salvar o resumo diário.",
                        )
                        null
                    }
                }
            }
        }
    }

    private suspend fun runFea1Safely(
        request: WatchSyncRequest,
        transport: ChampionTransportSession,
    ): DailyStepsRefreshResult? = try {
        runFea1(request, transport)
    } catch (cancellation: kotlinx.coroutines.CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        finishComponent(
            component = WatchSyncComponent.FEA1,
            status = WatchSyncComponentStatus.FAILED,
            finishedAt = Instant.now(),
            message = "Não foi possível concluir o resumo diário.",
        )
        null
    }

    private suspend fun runD1(
        request: WatchSyncRequest,
        transport: ChampionTransportSession,
    ): br.com.mwconsulting.championcompanion.feature.health.WatchSyncD1Result {
        val startedAt = Instant.now()
        startComponent(WatchSyncComponent.D1, startedAt)
        mutableState.update { it.copy(stage = WatchSyncStage.D1) }
        val result = d1Component.importForWatchSync(
            D1HeartRateSyncRequest(
                importAttemptId = request.attemptId,
                associatedDevice = request.associatedDevice,
                baseLocalDate = request.baseLocalDate,
                zoneId = request.zoneId,
            ),
            transport,
        )
        val status = result.toComponentStatus()
        finishComponent(
            component = WatchSyncComponent.D1,
            status = status,
            finishedAt = result.finishedAt,
            message = result.error?.message,
        )
        return result
    }

    private suspend fun runD1Safely(
        request: WatchSyncRequest,
        transport: ChampionTransportSession,
    ): br.com.mwconsulting.championcompanion.feature.health.WatchSyncD1Result = try {
        runD1(request, transport)
    } catch (cancellation: kotlinx.coroutines.CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        val finishedAt = Instant.now()
        finishComponent(
            component = WatchSyncComponent.D1,
            status = WatchSyncComponentStatus.FAILED,
            finishedAt = finishedAt,
            message = "Não foi possível concluir os dados diários.",
        )
        br.com.mwconsulting.championcompanion.feature.health.WatchSyncD1Result(
            result = br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportResult.Failed(),
            startedAt = request.startedAt,
            finishedAt = finishedAt,
        )
    }

    private suspend fun runD3D4(
        request: WatchSyncRequest,
        transport: ChampionTransportSession,
    ): br.com.mwconsulting.championcompanion.feature.exercises.WatchSyncWorkoutResult {
        val startedAt = Instant.now()
        startComponent(WatchSyncComponent.D3_D4, startedAt)
        mutableState.update { it.copy(stage = WatchSyncStage.D3_D4) }
        val result = workoutComponent.importForWatchSync(
            WatchWorkoutImportRequest(
                importAttemptId = request.attemptId,
                associatedDevice = request.associatedDevice,
            ),
            transport,
        )
        finishComponent(
            component = WatchSyncComponent.D3_D4,
            status = result.toComponentStatus(),
            finishedAt = result.finishedAt,
            message = result.error?.message,
        )
        return result
    }

    private suspend fun runD3D4Safely(
        request: WatchSyncRequest,
        transport: ChampionTransportSession,
    ): br.com.mwconsulting.championcompanion.feature.exercises.WatchSyncWorkoutResult = try {
        runD3D4(request, transport)
    } catch (cancellation: kotlinx.coroutines.CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        val finishedAt = Instant.now()
        finishComponent(
            component = WatchSyncComponent.D3_D4,
            status = WatchSyncComponentStatus.FAILED,
            finishedAt = finishedAt,
            message = "Não foi possível concluir os treinos do relógio.",
        )
        br.com.mwconsulting.championcompanion.feature.exercises.WatchSyncWorkoutResult(
            result = br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportResult.Failed(),
            startedAt = request.startedAt,
            finishedAt = finishedAt,
        )
    }

    private fun startComponent(component: WatchSyncComponent, startedAt: Instant) {
        mutableState.update { current ->
            current.copy(components = current.components.update(component) {
                WatchSyncComponentState(
                    status = WatchSyncComponentStatus.RUNNING,
                    startedAt = startedAt,
                )
            })
        }
    }

    private fun finishComponent(
        component: WatchSyncComponent,
        status: WatchSyncComponentStatus,
        finishedAt: Instant,
        message: String?,
    ) {
        mutableState.update { current ->
            current.copy(components = current.components.update(component) {
                it.copy(status = status, finishedAt = finishedAt, message = message)
            })
        }
    }

    private fun finish(
        request: WatchSyncRequest,
        status: WatchSyncResultStatus,
        error: WatchSyncError?,
        fea1: DailyStepsRefreshResult? = null,
    ): WatchSyncResult {
        val finishedAt = Instant.now()
        mutableState.update { current ->
            current.copy(result = status, error = error)
        }
        return state.value.toResult(request, error, finishedAt, fea1)
    }

    private fun WatchSyncState.toResult(
        request: WatchSyncRequest,
        error: WatchSyncError?,
        finishedAt: Instant = Instant.now(),
        fea1: DailyStepsRefreshResult? = null,
    ): WatchSyncResult = WatchSyncResult(
        attemptId = request.attemptId,
        sessionId = sessionId,
        startedAt = startedAt ?: request.startedAt,
        finishedAt = finishedAt,
        status = result ?: WatchSyncResultStatus.FAILED,
        components = components,
        error = error,
        fea1 = fea1,
    )

    private fun globalStatus(components: WatchSyncComponents): WatchSyncResultStatus {
        val statuses = listOf(components.fea1.status, components.d1.status, components.d3D4.status)
        val hasFailed = statuses.any { it == WatchSyncComponentStatus.FAILED || it == WatchSyncComponentStatus.SKIPPED }
        val hasPartial = statuses.any { it == WatchSyncComponentStatus.PARTIAL }
        return when {
            hasFailed && statuses.any { it == WatchSyncComponentStatus.COMPLETED || it == WatchSyncComponentStatus.PARTIAL } ->
                WatchSyncResultStatus.PARTIAL
            hasFailed -> WatchSyncResultStatus.FAILED
            hasPartial -> WatchSyncResultStatus.PARTIAL
            else -> WatchSyncResultStatus.COMPLETED
        }
    }

    private fun globalError(
        components: WatchSyncComponents,
        status: WatchSyncResultStatus,
    ): WatchSyncError? {
        val message = listOf(components.fea1, components.d1, components.d3D4)
            .firstOrNull { it.status == WatchSyncComponentStatus.FAILED || it.status == WatchSyncComponentStatus.SKIPPED }
            ?.message
        return when {
            status == WatchSyncResultStatus.COMPLETED -> null
            status == WatchSyncResultStatus.PARTIAL -> WatchSyncError(
                category = WatchSyncErrorCategory.PARTIAL,
                message = message?.let { "Sincronização parcial. $it" }
                    ?: "Sincronização parcial. Tente novamente para recuperar dados pendentes.",
                isRetryable = true,
            )
            else -> WatchSyncError(
                category = WatchSyncErrorCategory.CONNECTION_FAILED,
                message = message ?: "Não foi possível concluir a sincronização do relógio.",
                isRetryable = true,
            )
        }
    }

    private fun skippedComponent(message: String) = WatchSyncComponentState(
        status = WatchSyncComponentStatus.SKIPPED,
        startedAt = null,
        finishedAt = Instant.now(),
        message = message,
    )

    private fun WatchSyncComponents.update(
        component: WatchSyncComponent,
        transform: (WatchSyncComponentState) -> WatchSyncComponentState,
    ): WatchSyncComponents = when (component) {
        WatchSyncComponent.FEA1 -> copy(fea1 = transform(fea1))
        WatchSyncComponent.D1 -> copy(d1 = transform(d1))
        WatchSyncComponent.D3_D4 -> copy(d3D4 = transform(d3D4))
    }

    private fun WatchSyncState.markRunningAsFailed(message: String): WatchSyncComponents = components.copy(
        fea1 = components.fea1.markRunningAsFailed(message),
        d1 = components.d1.markRunningAsFailed(message),
        d3D4 = components.d3D4.markRunningAsFailed(message),
    )

    private fun br.com.mwconsulting.championcompanion.feature.health.WatchSyncD1Result.toComponentStatus(): WatchSyncComponentStatus = when (result.status) {
        D1HeartRateImportStatus.COMPLETED -> WatchSyncComponentStatus.COMPLETED
        D1HeartRateImportStatus.COMPLETED_WITH_GAPS -> WatchSyncComponentStatus.PARTIAL
        D1HeartRateImportStatus.PARTIAL -> WatchSyncComponentStatus.PARTIAL
        D1HeartRateImportStatus.FAILED,
        D1HeartRateImportStatus.CANCELLED,
        -> WatchSyncComponentStatus.FAILED
    }

    private fun br.com.mwconsulting.championcompanion.feature.exercises.WatchSyncWorkoutResult.toComponentStatus(): WatchSyncComponentStatus = when (result.status) {
        WatchWorkoutImportStatus.COMPLETED,
        WatchWorkoutImportStatus.EMPTY,
        -> WatchSyncComponentStatus.COMPLETED
        WatchWorkoutImportStatus.PARTIAL -> WatchSyncComponentStatus.PARTIAL
        WatchWorkoutImportStatus.FAILED,
        WatchWorkoutImportStatus.CANCELLED,
        -> WatchSyncComponentStatus.FAILED
    }

    private fun WatchSyncComponentState.markRunningAsFailed(message: String): WatchSyncComponentState =
        if (status == WatchSyncComponentStatus.RUNNING) {
            copy(status = WatchSyncComponentStatus.FAILED, finishedAt = Instant.now(), message = message)
        } else {
            this
        }

    private enum class WatchSyncComponent {
        FEA1,
        D1,
        D3_D4,
    }

    private fun ForegroundBleOperationOwner.toWatchSyncBlockMessage(): String = when (this) {
        ForegroundBleOperationOwner.DAILY_STEPS_REFRESH -> "Outra sincronização do relógio já está em andamento."
        ForegroundBleOperationOwner.MANUAL_HEART_RATE -> "Outra operação do relógio está em andamento."
        ForegroundBleOperationOwner.WATCH_D1_HISTORY -> "Outra sincronização do relógio está em andamento."
        ForegroundBleOperationOwner.WATCH_WORKOUT_IMPORT -> "Outra sincronização do relógio está em andamento."
        ForegroundBleOperationOwner.APP_WORKOUT_SESSION -> "Uma sessão de treino está em andamento."
        ForegroundBleOperationOwner.DEVICE_CONNECTION -> "Uma conexão do dispositivo está em andamento."
        ForegroundBleOperationOwner.WATCH_SYNC -> "Outra sincronização do relógio já está em andamento."
    }

    private fun ChampionBleError.toWatchSyncError(): WatchSyncError = when (this) {
        ChampionBleError.BluetoothDisabled -> WatchSyncError(
            category = WatchSyncErrorCategory.CONNECTION_FAILED,
            message = "Ative o Bluetooth para atualizar os dados do relógio.",
            isRetryable = true,
        )

        ChampionBleError.ScanPermissionMissing,
        ChampionBleError.ConnectPermissionMissing,
        ChampionBleError.LegacyPermissionMissing,
        ChampionBleError.LocationPermissionRequired,
        -> WatchSyncError(
            category = WatchSyncErrorCategory.CONNECTION_FAILED,
            message = "Permissões de Bluetooth são necessárias para atualizar os dados.",
            isRetryable = true,
        )

        ChampionBleError.DeviceNotFound,
        is ChampionBleError.ConnectionFailed,
        is ChampionBleError.UnexpectedDisconnect,
        is ChampionBleError.DiscoveryFailed,
        is ChampionBleError.SubscriptionFailed,
        is ChampionBleError.MissingRequiredChannel,
        is ChampionBleError.MissingRequiredService,
        is ChampionBleError.MissingOptionalChannel,
        is ChampionBleError.IncompatibleChannelProperties,
        is ChampionBleError.CallbackFailure,
        is ChampionBleError.OperationRejected,
        ChampionBleError.OperationUnavailable,
        ChampionBleError.BleNotSupported,
        ChampionBleError.ScanAlreadyInProgress,
        is ChampionBleError.ScanFailed,
        is ChampionBleError.StaleSession,
        -> WatchSyncError(
            category = WatchSyncErrorCategory.CONNECTION_FAILED,
            message = "Não foi possível conectar nem manter a sessão com o relógio.",
            isRetryable = true,
        )

        is ChampionBleError.Timeout -> WatchSyncError(
            category = WatchSyncErrorCategory.TIMEOUT,
            message = "O tempo limite foi atingido enquanto aguardávamos os dados do relógio.",
            isRetryable = true,
        )

        is ChampionBleError.InvalidPayload -> WatchSyncError(
            category = WatchSyncErrorCategory.INVALID_PACKET,
            message = "O relógio respondeu com um pacote inválido.",
            isRetryable = true,
        )

        is ChampionBleError.OperationCancelled,
        ChampionBleError.CleanupInProgress,
        is ChampionBleError.QueueClosed,
        -> WatchSyncError(
            category = WatchSyncErrorCategory.CANCELLED,
            message = "A sincronização foi cancelada antes de concluir.",
            isRetryable = true,
        )

        ChampionBleError.UnexpectedInternal -> WatchSyncError(
            category = WatchSyncErrorCategory.INTERNAL,
            message = "Não foi possível concluir a sincronização do relógio.",
            isRetryable = true,
        )
    }

    private fun br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshFailure.toWatchSyncMessage(): String = when (this) {
        br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshFailure.TIMEOUT ->
            "Tempo limite atingido na leitura FEA1."
        br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshFailure.INVALID_PACKET ->
            "O relógio respondeu com um pacote FEA1 inválido."
        br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshFailure.DEVICE_NOT_FOUND,
        br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshFailure.CONNECTION_FAILED,
        -> "Não foi possível ler o resumo diário do relógio."
        br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshFailure.CANCELLED ->
            "A leitura FEA1 foi cancelada."
        br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshFailure.INTERNAL ->
            "Não foi possível ler o resumo diário do relógio."
    }

    private fun Device.toDiscoveredDevice() = br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice(
        transportKey = platformIdentifier,
        name = name,
        localName = name,
        observedAt = Instant.now(),
    )
}
