package br.com.mwconsulting.championcompanion.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBlePrerequisites
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidBleEnvironment
import br.com.mwconsulting.championcompanion.core.model.AppPreferencesRepository
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSession
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionRepository
import br.com.mwconsulting.championcompanion.core.model.DailySummary
import br.com.mwconsulting.championcompanion.core.model.DailySummaryPolicy
import br.com.mwconsulting.championcompanion.core.model.DailySummaryRepository
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRepository
import br.com.mwconsulting.championcompanion.core.model.HealthSyncSummary
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.HeartRateRepository
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
    private val appWorkoutSessionRepository: AppWorkoutSessionRepository,
    private val workoutActivityRepository: WorkoutActivityRepository,
    private val healthSyncRepository: HealthSyncRepository,
    private val watchSyncCoordinator: WatchSyncCoordinator,
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
            watchSyncCoordinator.state.collect { syncState ->
                refreshState.update { current ->
                    current.copy(
                        activeOperation = syncState.toHomeRefreshOperation(),
                        error = syncState.error?.toHomeRefreshError(),
                        resultStatus = syncState.result,
                        components = syncState.components,
                    )
                }
            }
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
            val attemptId = ids.next().toString()
            val result = watchSyncCoordinator.sync(
                WatchSyncRequest(
                    attemptId = attemptId,
                    associatedDevice = associatedDevice,
                    baseLocalDate = observedDate.value,
                    zoneId = ZoneId.systemDefault(),
                    startedAt = startedAt,
                ),
            )
            result.fea1?.let { observedDate.value = it.localDate }
            refreshState.update { current ->
                current.copy(
                    activeOperation = null,
                    error = result.error?.toHomeRefreshError(),
                    resultStatus = result.status,
                    components = result.components,
                    lastSuccessfulRefreshAt = if (result.status == WatchSyncResultStatus.COMPLETED) {
                        result.finishedAt
                    } else {
                        previousSuccess
                    },
                )
            }
        } finally {
            refreshObservedDate()
            refreshMutex.unlock()
        }
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

    private fun WatchSyncState.toHomeRefreshOperation(): HomeRefreshOperation? {
        val attemptId = attemptId ?: return null
        if (!isRunning) return null
        return HomeRefreshOperation(
            attempt = HomeRefreshAttempt(
                refreshAttemptId = attemptId,
                sessionId = sessionId,
                startedAt = startedAt ?: Instant.now(),
            ),
            stage = when (stage) {
                WatchSyncStage.Preparing -> HomeRefreshStage.Preparing
                WatchSyncStage.Connecting -> HomeRefreshStage.Connecting
                WatchSyncStage.FEA1 -> HomeRefreshStage.FEA1
                WatchSyncStage.D1 -> HomeRefreshStage.D1
                WatchSyncStage.D3_D4 -> HomeRefreshStage.D3_D4
                null -> HomeRefreshStage.Preparing
            },
        )
    }

    private fun WatchSyncError.toHomeRefreshError(): HomeRefreshError = HomeRefreshError(
        category = when (category) {
            WatchSyncErrorCategory.BLE_OPERATION_IN_PROGRESS -> HomeRefreshErrorCategory.BLE_OPERATION_IN_PROGRESS
            WatchSyncErrorCategory.TIMEOUT -> HomeRefreshErrorCategory.TIMEOUT
            WatchSyncErrorCategory.INVALID_PACKET -> HomeRefreshErrorCategory.INVALID_PACKET
            WatchSyncErrorCategory.CANCELLED -> HomeRefreshErrorCategory.CANCELLED
            WatchSyncErrorCategory.STORAGE -> HomeRefreshErrorCategory.STORAGE
            WatchSyncErrorCategory.PARTIAL -> HomeRefreshErrorCategory.PARTIAL
            WatchSyncErrorCategory.CONNECTION_FAILED,
            WatchSyncErrorCategory.HANDSHAKE_FAILED,
            -> HomeRefreshErrorCategory.CONNECTION_FAILED
            WatchSyncErrorCategory.INTERNAL -> HomeRefreshErrorCategory.INTERNAL
        },
        message = message,
        isRetryable = isRetryable,
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
