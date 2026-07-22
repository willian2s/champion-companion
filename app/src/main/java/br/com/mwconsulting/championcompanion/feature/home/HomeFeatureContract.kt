package br.com.mwconsulting.championcompanion.feature.home

import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import java.time.Instant
import java.time.LocalDate

interface HomeFeatureContract : UiStateStore<HomeUiState> {
    fun onIntent(intent: HomeIntent)
}

data class HomeUiState(
    val surfaceState: HomeSurfaceState = HomeSurfaceState.Loading,
    val observedDate: LocalDate = LocalDate.now(),
    val summary: HomeDailySummaryModel? = null,
    val watch: HomeWatchSnapshot = HomeWatchSnapshot(),
    val pendingActivities: List<HomePendingActivityModel> = emptyList(),
    val refresh: HomeRefreshState = HomeRefreshState(),
)

sealed interface HomeIntent {
    data object Load : HomeIntent
    data object Refresh : HomeIntent
    data object Retry : HomeIntent
    data object DismissError : HomeIntent
    data object OpenDevice : HomeIntent
}

enum class HomeSurfaceState {
    Loading,
    Empty,
    Ready,
    RefreshFailed,
    DeviceRequired,
    PermissionsRequired,
    BluetoothDisabled,
}

data class HomeRefreshState(
    val activeOperation: HomeRefreshOperation? = null,
    val error: HomeRefreshError? = null,
    val lastSuccessfulRefreshAt: Instant? = null,
) {
    val isRefreshing: Boolean get() = activeOperation != null
    val refreshStage: HomeRefreshStage? get() = activeOperation?.stage
}

data class HomeRefreshOperation(
    val attempt: HomeRefreshAttempt,
    val stage: HomeRefreshStage,
)

enum class HomeRefreshStage {
    Preparing,
    Connecting,
    WaitingForSteps,
    Persisting,
}

data class HomeRefreshAttempt(
    val refreshAttemptId: String,
    val sessionId: String? = null,
    val startedAt: Instant,
)

data class HomeRefreshError(
    val category: HomeRefreshErrorCategory,
    val message: String,
    val isRetryable: Boolean,
)

enum class HomeRefreshErrorCategory {
    NO_ASSOCIATED_DEVICE,
    PERMISSIONS_REQUIRED,
    BLUETOOTH_DISABLED,
    CONNECTION_FAILED,
    TIMEOUT,
    INVALID_PACKET,
    CANCELLED,
    STORAGE,
    INTERNAL,
}

data class HomeWatchSnapshot(
    val connectionState: HomeWatchConnectionState = HomeWatchConnectionState.Unknown,
    val deviceName: String? = null,
    val lastSuccessfulConnectionAt: Instant? = null,
    val lastImportedAt: Instant? = null,
)

enum class HomeWatchConnectionState {
    Unknown,
    Disconnected,
    Connecting,
    Connected,
    BluetoothDisabled,
    PermissionsRequired,
}

data class HomeDailySummaryModel(
    val steps: Long,
    val dailyGoal: Int,
    val progress: Float,
    val estimatedDistanceMeters: Double?,
    val estimatedCaloriesKilocalories: Double?,
    val sourceLabel: String,
    val updatedAt: Instant,
)

data class HomePendingActivityModel(
    val id: String,
    val title: String,
    val subtitle: String?,
)
