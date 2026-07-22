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
import br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshRequest
import br.com.mwconsulting.championcompanion.core.model.DailyStepsSyncCoordinator
import br.com.mwconsulting.championcompanion.core.model.DailySummary
import br.com.mwconsulting.championcompanion.core.model.DailySummaryPolicy
import br.com.mwconsulting.championcompanion.core.model.DailySummaryRepository
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.MergeWatchDailySummaryInput
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsRepository
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsSnapshot
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
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
    private val dailyStepsSyncCoordinator: DailyStepsSyncCoordinator,
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
            deviceRepository.observeAssociatedDevice(),
            appPreferencesRepository.observeDailyStepsGoal(),
        ) { date, summary, device, goal ->
            HomeObservedContent(
                observedDate = date,
                summary = summary,
                device = device,
                dailyGoal = goal,
            )
        }

        viewModelScope.launch {
            combine(
                contentFlow,
                appWorkoutSessionRepository.observeRecoverable(),
                connectionManager.state,
                refreshState,
            ) { content, pending, bleState, refresh ->
                HomeUiState(
                    surfaceState = deriveSurfaceState(content.summary, content.device, bleState, refresh),
                    observedDate = content.observedDate,
                    summary = content.summary?.toHomeDailySummaryModel(content.device, content.dailyGoal),
                    watch = content.device.toHomeWatchSnapshot(bleState),
                    pendingActivities = pending.map { it.toPendingActivityModel() },
                    refresh = refresh,
                )
            }.collect { mutableState.value = it }
        }
    }

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Load -> {
                observedDate.value = LocalDate.now()
                environment.value = bleEnvironment.snapshot()
            }

            HomeIntent.Refresh,
            HomeIntent.Retry,
            -> viewModelScope.launch { refresh() }

            HomeIntent.DismissError -> refreshState.update { current -> current.copy(error = null, activeOperation = null) }
            HomeIntent.OpenDevice -> Unit
        }
    }

    private suspend fun refresh() {
        if (refreshState.value.isRefreshing) return
        refreshMutex.lock()

        val previousSuccess = refreshState.value.lastSuccessfulRefreshAt
        try {
            observedDate.value = LocalDate.now()
            environment.value = bleEnvironment.snapshot()

            val associatedDevice = deviceRepository.observeAssociatedDevice().first()
            if (associatedDevice == null) {
                refreshState.value = HomeRefreshState(
                    error = HomeRefreshError(
                        category = HomeRefreshErrorCategory.NO_ASSOCIATED_DEVICE,
                        message = "Nenhum relogio associado para sincronizar.",
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
            val refreshResult = when (val result = dailyStepsSyncCoordinator.refresh(
                DailyStepsRefreshRequest(
                    attempt = attempt,
                    associatedDeviceId = associatedDevice.id,
                    associatedDevicePlatformIdentifier = associatedDevice.platformIdentifier,
                    zoneId = ZoneId.systemDefault(),
                ),
            )) {
                is RepositoryResult.Success -> result.value
                is RepositoryResult.NotFound -> {
                    refreshState.value = HomeRefreshState(
                        error = HomeRefreshError(
                            category = HomeRefreshErrorCategory.CONNECTION_FAILED,
                            message = "Nao foi possivel localizar relogio associado.",
                            isRetryable = true,
                        ),
                        lastSuccessfulRefreshAt = previousSuccess,
                    )
                    return
                }

                RepositoryResult.StorageUnavailable -> {
                    refreshState.value = HomeRefreshState(
                        error = HomeRefreshError(
                            category = HomeRefreshErrorCategory.STORAGE,
                            message = "Nao foi possivel atualizar resumo local.",
                            isRetryable = true,
                        ),
                        lastSuccessfulRefreshAt = previousSuccess,
                    )
                    return
                }

                RepositoryResult.Conflict,
                RepositoryResult.UnexpectedState,
                -> {
                    refreshState.value = HomeRefreshState(
                        error = HomeRefreshError(
                            category = HomeRefreshErrorCategory.INTERNAL,
                            message = "Nao foi possivel concluir sincronizacao.",
                            isRetryable = true,
                        ),
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
                    ),
                )
            ) {
                is RepositoryResult.Success -> {
                    refreshState.value = HomeRefreshState(lastSuccessfulRefreshAt = updatedAt)
                }

                RepositoryResult.StorageUnavailable -> {
                    refreshState.value = HomeRefreshState(
                        error = HomeRefreshError(
                            category = HomeRefreshErrorCategory.STORAGE,
                            message = "Nao foi possivel persistir resumo do dia.",
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
                            message = "Nao foi possivel consolidar resumo do dia.",
                            isRetryable = true,
                        ),
                        lastSuccessfulRefreshAt = previousSuccess,
                    )
                }
            }
        } finally {
            refreshState.update { current -> current.copy(activeOperation = null) }
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
                DataOrigin.WATCH_DAILY_SUMMARY -> device?.name ?: "Relogio"
                else -> "Resumo local"
            },
            updatedAt = updatedAt,
        )
    }

    private fun AppWorkoutSession.toPendingActivityModel(): HomePendingActivityModel = HomePendingActivityModel(
        id = id.value.toString(),
        title = when (type) {
            WorkoutType.WALKING -> "Caminhada pendente"
            WorkoutType.RUNNING -> "Corrida pendente"
            WorkoutType.TREADMILL -> "Esteira pendente"
            WorkoutType.FREE_WORKOUT -> "Treino pendente"
            WorkoutType.UNKNOWN -> "Atividade pendente"
        },
        subtitle = when (state) {
            br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionState.AWAITING_RECOVERY -> "Sessao interrompida aguardando retomada"
            else -> "Sessao pendente"
        },
    )

    private fun ChampionBleError.toHomeRefreshError(): HomeRefreshError = when (this) {
        ChampionBleError.BluetoothDisabled -> HomeRefreshError(
            category = HomeRefreshErrorCategory.BLUETOOTH_DISABLED,
            message = "Ative o Bluetooth para atualizar dados do relogio.",
            isRetryable = true,
        )

        ChampionBleError.ScanPermissionMissing,
        ChampionBleError.ConnectPermissionMissing,
        ChampionBleError.LegacyPermissionMissing,
        ChampionBleError.LocationPermissionRequired,
        -> HomeRefreshError(
            category = HomeRefreshErrorCategory.PERMISSIONS_REQUIRED,
            message = "Permissoes de Bluetooth sao necessarias para atualizar dados.",
            isRetryable = true,
        )

        else -> HomeRefreshError(
            category = HomeRefreshErrorCategory.INTERNAL,
            message = "Nao foi possivel preparar sincronizacao.",
            isRetryable = true,
        )
    }

    private fun br.com.mwconsulting.championcompanion.core.model.DailySyncAttempt.toHomeRefreshAttempt(): HomeRefreshAttempt =
        HomeRefreshAttempt(
            refreshAttemptId = refreshAttemptId,
            sessionId = sessionId,
            startedAt = startedAt,
        )

    private data class HomeObservedContent(
        val observedDate: LocalDate,
        val summary: DailySummary?,
        val device: Device?,
        val dailyGoal: Int,
    )
}
