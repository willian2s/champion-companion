package br.com.mwconsulting.championcompanion.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBlePrerequisites
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidBleEnvironment
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectSyncCoordinator
import br.com.mwconsulting.championcompanion.core.model.AppPreferencesRepository
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSession
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionRepository
import br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshFailure
import br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshOutcome
import br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshRequest
import br.com.mwconsulting.championcompanion.core.model.DailyStepsSyncCoordinator
import br.com.mwconsulting.championcompanion.core.model.DailySummary
import br.com.mwconsulting.championcompanion.core.model.DailySummaryPolicy
import br.com.mwconsulting.championcompanion.core.model.DailySummaryRepository
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationAcquireResult
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationGate
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationOwner
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationPermit
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationRequest
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRepository
import br.com.mwconsulting.championcompanion.core.model.HealthSyncSummary
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.HeartRateRepository
import br.com.mwconsulting.championcompanion.core.model.MergeWatchDailySummaryInput
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsRepository
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.PhoneWorkoutPolicy
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import br.com.mwconsulting.championcompanion.core.model.WorkoutWorkflowSummary
import br.com.mwconsulting.championcompanion.core.model.toLocalDateIn
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    private val dailySummaryRepository: DailySummaryRepository,
    private val deviceRepository: DeviceRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val personalMetricsRepository: PersonalMetricsRepository,
    private val appWorkoutSessionRepository: AppWorkoutSessionRepository,
    private val workoutActivityRepository: WorkoutActivityRepository,
    private val healthSyncRepository: HealthSyncRepository,
    private val dailyStepsSyncCoordinator: DailyStepsSyncCoordinator,
    private val healthConnectSyncCoordinator: HealthConnectSyncCoordinator,
    private val foregroundBleOperationGate: ForegroundBleOperationGate,
    private val heartRateRepository: HeartRateRepository,
    private val connectionManager: ChampionConnectionManager,
    private val bleEnvironment: AndroidBleEnvironment,
) : ViewModel(), HomeFeatureContract {
    private val ids = UuidV7Generator()
    private val refreshMutex = Mutex()
    private val observedDate = MutableStateFlow(LocalDate.now())
    private val environment = MutableStateFlow(bleEnvironment.snapshot())
    private val refreshState = MutableStateFlow(HomeRefreshState())
    private val mutableState = MutableStateFlow(HomeUiState())

    override val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    init {
        val summaryFlow = observedDate.flatMapLatest { dailySummaryRepository.observeSummaryForDate(it) }
        val contentFlow = combine(
            observedDate,
            summaryFlow,
            heartRateRepository.observeLatestValidMeasurement(),
            deviceRepository.observeAssociatedDevice(),
            appPreferencesRepository.observeDailyStepsGoal(),
        ) { date, summary, latestHeartRate, device, goal ->
            HomeObservedContent(
                observedDate = date,
                summary = summary,
                latestHeartRate = latestHeartRate,
                device = device,
                dailyGoal = goal,
            )
        }

        viewModelScope.launch {
            val statusFlow = combine(
                contentFlow,
                workoutActivityRepository.observeWorkflowSummary(),
                healthSyncRepository.observeSyncSummary(),
            ) { content: HomeObservedContent, workflowSummary, healthSyncSummary ->
                Triple(content, workflowSummary, healthSyncSummary)
            }
            combine(
                statusFlow,
                appWorkoutSessionRepository.observeRecoverable(),
                connectionManager.state,
                refreshState,
            ) { status, pending, bleState, refresh ->
                val (content, workflowSummary, healthSyncSummary) = status
                HomeUiState(
                    surfaceState = deriveSurfaceState(content.summary, content.device, bleState, refresh),
                    observedDate = content.observedDate,
                    summary = content.summary?.toHomeDailySummaryModel(content.device, content.dailyGoal),
                    latestHeartRate = content.latestHeartRate?.toHomeLatestHeartRateModel(),
                    watch = content.device.toHomeWatchSnapshot(bleState),
                    healthConnectSync = healthSyncSummary.toHomeHealthConnectSyncModel(),
                    workoutPendingBanner = workflowSummary.toWorkoutPendingBanner(),
                    pendingActivities = pending.map { it.toPendingActivityModel() },
                    refresh = refresh,
                )
            }.collect { mutableState.value = it }
        }

        viewModelScope.launch {
            while (isActive) {
                delay(millisUntilNextMidnight())
                refreshObservedDate()
            }
        }
    }

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Load -> {
                refreshObservedDate()
            }

            HomeIntent.Refresh,
            HomeIntent.Retry,
            -> viewModelScope.launch { refresh() }

            HomeIntent.DismissError -> refreshState.update { current -> current.copy(error = null, activeOperation = null) }
            HomeIntent.OpenDevice -> Unit
            HomeIntent.OpenExercises -> Unit
        }
    }

    private suspend fun refresh() {
        if (!refreshMutex.tryLock()) return

        val previousSuccess = refreshState.value.lastSuccessfulRefreshAt
        var gatePermit: ForegroundBleOperationPermit? = null
        var shouldTriggerHealthConnectSync = false
        try {
            if (refreshState.value.isRefreshing) return
            refreshObservedDate()

            val associatedDevice = deviceRepository.observeAssociatedDevice().first()
            if (associatedDevice == null) {
                refreshState.value = HomeRefreshState(
                    error = HomeRefreshError(
                        category = HomeRefreshErrorCategory.NO_ASSOCIATED_DEVICE,
                        message = "Nenhum relógio associado foi encontrado para sincronizar.",
                        isRetryable = false,
                    ),
                    lastSuccessfulRefreshAt = previousSuccess,
                )
                return
            }

            val readiness = ChampionBlePrerequisites.forConnect(environment.value)
            if (!readiness.isReady) {
                refreshState.value = HomeRefreshState(
                    error = readiness.missing.first().error.toHomeRefreshError(),
                    lastSuccessfulRefreshAt = previousSuccess,
                )
                return
            }

            val startedAt = Instant.now()
            val attempt = br.com.mwconsulting.championcompanion.core.model.DailySyncAttempt(
                refreshAttemptId = ids.next().toString(),
                startedAt = startedAt,
            )
            when (
                val gateResult = foregroundBleOperationGate.tryAcquire(
                    ForegroundBleOperationRequest(
                        owner = ForegroundBleOperationOwner.DAILY_STEPS_REFRESH,
                        operationId = attempt.refreshAttemptId,
                        requestedAt = startedAt,
                    ),
                )
            ) {
                is ForegroundBleOperationAcquireResult.Acquired -> gatePermit = gateResult.permit
                is ForegroundBleOperationAcquireResult.Rejected -> {
                    refreshState.value = HomeRefreshState(
                        error = HomeRefreshError(
                            category = HomeRefreshErrorCategory.BLE_OPERATION_IN_PROGRESS,
                            message = gateResult.activeOperation.owner.toHomeForegroundBleBlockMessage(),
                            isRetryable = true,
                        ),
                        lastSuccessfulRefreshAt = previousSuccess,
                    )
                    return
                }
            }

            refreshState.value = HomeRefreshState(
                activeOperation = HomeRefreshOperation(
                    attempt = attempt.toHomeRefreshAttempt(),
                    stage = HomeRefreshStage.Preparing,
                ),
                lastSuccessfulRefreshAt = previousSuccess,
            )

            val currentSession = connectionManager.currentSession()
            refreshState.update { current ->
                current.copy(
                    activeOperation = current.activeOperation?.copy(
                        stage = if (currentSession?.session?.transportKey == associatedDevice.platformIdentifier) {
                            HomeRefreshStage.WaitingForSteps
                        } else {
                            HomeRefreshStage.Connecting
                        },
                    ),
                )
            }

            val metrics = personalMetricsRepository.observeSnapshot().first()
            val refreshZoneId = ZoneId.systemDefault()
            val refreshResult = when (val result = dailyStepsSyncCoordinator.refresh(
                DailyStepsRefreshRequest(
                    attempt = attempt,
                    associatedDeviceId = associatedDevice.id,
                    associatedDevicePlatformIdentifier = associatedDevice.platformIdentifier,
                    zoneId = refreshZoneId,
                ),
            )) {
                is DailyStepsRefreshOutcome.Success -> result.value
                is DailyStepsRefreshOutcome.Failure -> {
                    refreshState.value = HomeRefreshState(
                        error = result.reason.toHomeRefreshError(),
                        lastSuccessfulRefreshAt = previousSuccess,
                    )
                    return
                }
            }

            refreshState.update { current ->
                current.copy(
                    activeOperation = current.activeOperation?.copy(
                        attempt = refreshResult.attempt.toHomeRefreshAttempt(),
                        stage = HomeRefreshStage.Persisting,
                    ),
                )
            }

            val estimates = DailySummaryPolicy.estimate(refreshResult.steps, metrics)
            val updatedAt = Instant.now()
            when (
                dailySummaryRepository.mergeWatchDailySummary(
                    MergeWatchDailySummaryInput(
                        deviceId = refreshResult.deviceId,
                        localDate = refreshResult.localDate,
                        incomingSteps = refreshResult.steps,
                        collectedAt = refreshResult.collectedAt,
                        updatedAt = updatedAt,
                        estimatedDistanceMeters = estimates.distanceMeters,
                        estimatedCaloriesKilocalories = estimates.caloriesKilocalories,
                        parserVersion = refreshResult.parserVersion,
                        zoneId = refreshZoneId,
                    ),
                )
            ) {
                is RepositoryResult.Success -> {
                    observedDate.value = refreshResult.collectedAt.toLocalDateIn(ZoneId.systemDefault())
                    refreshState.value = HomeRefreshState(lastSuccessfulRefreshAt = updatedAt)
                    shouldTriggerHealthConnectSync = true
                }

                RepositoryResult.StorageUnavailable -> {
                    refreshState.value = HomeRefreshState(
                        error = HomeRefreshError(
                            category = HomeRefreshErrorCategory.STORAGE,
                            message = "Não foi possível salvar o resumo do dia.",
                            isRetryable = true,
                        ),
                        lastSuccessfulRefreshAt = previousSuccess,
                    )
                }

                RepositoryResult.Conflict,
                RepositoryResult.NotFound,
                RepositoryResult.UnexpectedState,
                -> {
                    refreshState.value = HomeRefreshState(
                        error = HomeRefreshError(
                            category = HomeRefreshErrorCategory.INTERNAL,
                            message = "Não foi possível consolidar o resumo do dia.",
                            isRetryable = true,
                        ),
                        lastSuccessfulRefreshAt = previousSuccess,
                    )
                }
            }
        } finally {
            gatePermit?.let { foregroundBleOperationGate.release(it) }
            if (shouldTriggerHealthConnectSync) {
                viewModelScope.launch { triggerHealthConnectSyncIfEnabled() }
            }
            refreshObservedDate()
            refreshState.update { current -> current.copy(activeOperation = null) }
            refreshMutex.unlock()
        }
    }

    private suspend fun triggerHealthConnectSyncIfEnabled() {
        if (!appPreferencesRepository.observe().first().healthSyncEnabled) return
        healthConnectSyncCoordinator.syncPending()
    }

    private fun deriveSurfaceState(
        summary: DailySummary?,
        device: Device?,
        bleState: ChampionBleState,
        refresh: HomeRefreshState,
    ): HomeSurfaceState {
        if (refresh.error != null) return HomeSurfaceState.RefreshFailed
        if (summary != null) return HomeSurfaceState.Ready

        val readiness = ChampionBlePrerequisites.forConnect(environment.value)
        if (readiness.missing.any { it.error == ChampionBleError.BluetoothDisabled }) {
            return HomeSurfaceState.BluetoothDisabled
        }
        if (readiness.missing.any { it.permissions.isNotEmpty() }) {
            return HomeSurfaceState.PermissionsRequired
        }
        if (device == null) return HomeSurfaceState.DeviceRequired

        return when (bleState) {
            ChampionBleState.Disconnected,
            is ChampionBleState.Failed,
            is ChampionBleState.TransportReady,
            is ChampionBleState.Connecting,
            is ChampionBleState.ConfiguringSubscriptions,
            is ChampionBleState.DiscoveringServices,
            is ChampionBleState.ResolvingChannels,
            is ChampionBleState.Disconnecting,
            is ChampionBleState.Scanning,
            -> HomeSurfaceState.Empty
        }
    }

    private fun Device?.toHomeWatchSnapshot(bleState: ChampionBleState): HomeWatchSnapshot {
        val readiness = ChampionBlePrerequisites.forConnect(environment.value)
        val connectionState = when {
            readiness.missing.any { it.error == ChampionBleError.BluetoothDisabled } -> HomeWatchConnectionState.BluetoothDisabled
            readiness.missing.any { it.permissions.isNotEmpty() } -> HomeWatchConnectionState.PermissionsRequired
            bleState is ChampionBleState.TransportReady -> HomeWatchConnectionState.Connected
            bleState is ChampionBleState.Connecting ||
                bleState is ChampionBleState.DiscoveringServices ||
                bleState is ChampionBleState.ResolvingChannels ||
                bleState is ChampionBleState.ConfiguringSubscriptions ||
                bleState is ChampionBleState.Disconnecting -> HomeWatchConnectionState.Connecting
            bleState is ChampionBleState.Disconnected || bleState is ChampionBleState.Failed -> HomeWatchConnectionState.Disconnected
            else -> HomeWatchConnectionState.Unknown
        }

        return HomeWatchSnapshot(
            connectionState = connectionState,
            deviceName = this?.name,
            lastSuccessfulConnectionAt = this?.lastSuccessfulConnectionAt,
            lastImportedAt = this?.importMetadata?.lastImportedAt,
        )
    }

    private fun DailySummary.toHomeDailySummaryModel(device: Device?, dailyGoal: Int): HomeDailySummaryModel {
        val goalProgress = DailySummaryPolicy.progress(steps, dailyGoal)
        return HomeDailySummaryModel(
            steps = steps,
            dailyGoal = goalProgress.goal,
            progress = goalProgress.progress,
            estimatedDistanceMeters = estimatedDistanceMeters,
            estimatedCaloriesKilocalories = estimatedCaloriesKilocalories,
            sourceLabel = when (key.origin) {
                DataOrigin.WATCH_DAILY_SUMMARY -> device?.name ?: "Relógio"
                else -> "Resumo local"
            },
            updatedAt = updatedAt,
        )
    }

    private fun HeartRateMeasurement.toHomeLatestHeartRateModel(): HomeLatestHeartRateModel? =
        measuredAt?.let { HomeLatestHeartRateModel(bpm = bpm, measuredAt = it) }

    private fun AppWorkoutSession.toPendingActivityModel(): HomePendingActivityModel = HomePendingActivityModel(
        id = id.value.toString(),
        title = when (type) {
            WorkoutType.WALKING -> "Caminhada pendente"
            WorkoutType.RUNNING -> "Corrida pendente"
            WorkoutType.TREADMILL -> "Esteira pendente"
            WorkoutType.FREE_WORKOUT -> "Treino pendente"
            WorkoutType.CYCLING -> "Bicicleta pendente"
            WorkoutType.UNKNOWN -> "Atividade pendente"
        },
        subtitle = PhoneWorkoutPolicy.NON_RESUMABLE_MESSAGE,
    )

    private fun WorkoutWorkflowSummary.toWorkoutPendingBanner(): HomeWorkoutPendingBanner? {
        if (pendingReviewCount <= 0 && pendingCalibrationCount <= 0) return null
        return if (pendingCalibrationCount > 0) {
            HomeWorkoutPendingBanner(
                title = "Calibração pendente",
                message = buildString {
                    append(pendingCalibrationCount)
                    append(" treino(s) aguardando calibração. ")
                    if (pendingReviewCount > 0) {
                        append(pendingReviewCount)
                        append(" também aguardam revisão.")
                    }
                },
                pendingReviewCount = pendingReviewCount,
                pendingCalibrationCount = pendingCalibrationCount,
                isCalibrationPriority = true,
            )
        } else {
            HomeWorkoutPendingBanner(
                title = "Revisão pendente",
                message = "$pendingReviewCount treino(s) aguardando revisão local antes da sincronização futura.",
                pendingReviewCount = pendingReviewCount,
                pendingCalibrationCount = pendingCalibrationCount,
                isCalibrationPriority = false,
            )
        }
    }

    private fun ChampionBleError.toHomeRefreshError(): HomeRefreshError = when (this) {
        ChampionBleError.BluetoothDisabled -> HomeRefreshError(
            category = HomeRefreshErrorCategory.BLUETOOTH_DISABLED,
            message = "Ative o Bluetooth para atualizar os dados do relógio.",
            isRetryable = true,
        )

        ChampionBleError.ScanPermissionMissing,
        ChampionBleError.ConnectPermissionMissing,
        ChampionBleError.LegacyPermissionMissing,
        ChampionBleError.LocationPermissionRequired,
        -> HomeRefreshError(
            category = HomeRefreshErrorCategory.PERMISSIONS_REQUIRED,
            message = "Permissões de Bluetooth são necessárias para atualizar os dados.",
            isRetryable = true,
        )

        else -> HomeRefreshError(
            category = HomeRefreshErrorCategory.INTERNAL,
            message = "Não foi possível preparar a sincronização.",
            isRetryable = true,
        )
    }

    private fun DailyStepsRefreshFailure.toHomeRefreshError(): HomeRefreshError = when (this) {
        DailyStepsRefreshFailure.DEVICE_NOT_FOUND -> HomeRefreshError(
            category = HomeRefreshErrorCategory.CONNECTION_FAILED,
            message = "Não foi possível localizar o relógio associado.",
            isRetryable = true,
        )

        DailyStepsRefreshFailure.CONNECTION_FAILED -> HomeRefreshError(
            category = HomeRefreshErrorCategory.CONNECTION_FAILED,
            message = "Não foi possível conectar nem manter a sessão com o relógio.",
            isRetryable = true,
        )

        DailyStepsRefreshFailure.TIMEOUT -> HomeRefreshError(
            category = HomeRefreshErrorCategory.TIMEOUT,
            message = "O tempo limite foi atingido enquanto aguardávamos os dados do relógio.",
            isRetryable = true,
        )

        DailyStepsRefreshFailure.INVALID_PACKET -> HomeRefreshError(
            category = HomeRefreshErrorCategory.INVALID_PACKET,
            message = "O relógio respondeu com um pacote inválido para esta sincronização.",
            isRetryable = true,
        )

        DailyStepsRefreshFailure.CANCELLED -> HomeRefreshError(
            category = HomeRefreshErrorCategory.CANCELLED,
            message = "A sincronização foi cancelada antes de concluir uma leitura válida.",
            isRetryable = true,
        )

        DailyStepsRefreshFailure.INTERNAL -> HomeRefreshError(
            category = HomeRefreshErrorCategory.INTERNAL,
            message = "Não foi possível concluir a sincronização.",
            isRetryable = true,
        )
    }

    private fun ForegroundBleOperationOwner.toHomeForegroundBleBlockMessage(): String = when (this) {
        ForegroundBleOperationOwner.DAILY_STEPS_REFRESH -> "Outra sincronização do relógio já está em andamento."
        ForegroundBleOperationOwner.MANUAL_HEART_RATE -> "Uma medição manual está em andamento na tela Saúde."
        ForegroundBleOperationOwner.WATCH_D1_HISTORY -> "Uma importação de histórico do relógio está em andamento na tela Saúde."
        ForegroundBleOperationOwner.WATCH_WORKOUT_IMPORT -> "Uma importação de treinos do relógio está em andamento na tela Exercícios."
        ForegroundBleOperationOwner.APP_WORKOUT_SESSION -> "A participação opcional do relógio está em andamento na sessão de treino."
        ForegroundBleOperationOwner.DEVICE_CONNECTION -> "Uma conexão do dispositivo está em andamento na tela Dispositivo."
    }

    private fun br.com.mwconsulting.championcompanion.core.model.DailySyncAttempt.toHomeRefreshAttempt(): HomeRefreshAttempt =
        HomeRefreshAttempt(
            refreshAttemptId = refreshAttemptId,
            sessionId = sessionId,
            startedAt = startedAt,
        )

    private fun refreshObservedDate() {
        observedDate.value = Instant.now().toLocalDateIn(ZoneId.systemDefault())
        environment.value = bleEnvironment.snapshot()
    }

    private fun HealthSyncSummary.toHomeHealthConnectSyncModel() = HomeHealthConnectSyncModel(
        pendingCount = pendingCount,
        failedCount = failedCount,
        syncingCount = syncingCount,
        syncedCount = syncedCount,
        lastSyncedAt = lastSyncedAt,
    )

    private fun millisUntilNextMidnight(now: Instant = Instant.now(), zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val nextMidnight = now
            .atZone(zoneId)
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(zoneId)
            .toInstant()
        return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L)
    }

    private data class HomeObservedContent(
        val observedDate: LocalDate,
        val summary: DailySummary?,
        val latestHeartRate: HeartRateMeasurement?,
        val device: Device?,
        val dailyGoal: Int,
    )
}
