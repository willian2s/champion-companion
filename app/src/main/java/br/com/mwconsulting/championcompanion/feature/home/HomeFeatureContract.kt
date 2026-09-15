package br.com.mwconsulting.championcompanion.feature.home

import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import br.com.mwconsulting.championcompanion.core.model.WatchDailyReadCompleteness
import java.time.Instant
import java.time.LocalDate

interface HomeFeatureContract : UiStateStore<HomeUiState> {
    fun onIntent(intent: HomeIntent)
}

data class HomeUiState(
    val surfaceState: HomeSurfaceState = HomeSurfaceState.Loading,
    val observedDate: LocalDate = LocalDate.now(),
    val summary: HomeDailySummaryModel? = null,
    val latestHeartRate: HomeLatestHeartRateModel? = null,
    val watch: HomeWatchSnapshot = HomeWatchSnapshot(),
    val healthConnectSync: HomeHealthConnectSyncModel = HomeHealthConnectSyncModel(),
    val workoutPendingBanner: HomeWorkoutPendingBanner? = null,
    val pendingActivities: List<HomePendingActivityModel> = emptyList(),
    val refresh: HomeRefreshState = HomeRefreshState(),
)

sealed interface HomeIntent {
    data object Load : HomeIntent
    data object Refresh : HomeIntent
    data object Retry : HomeIntent
    data object DismissError : HomeIntent
    data object OpenDevice : HomeIntent
    data object OpenExercises : HomeIntent
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
    val resultStatus: WatchSyncResultStatus? = null,
    val components: WatchSyncComponents = WatchSyncComponents(),
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
    FEA1,
    D1,
    D3_D4,
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
    BLE_OPERATION_IN_PROGRESS,
    CONNECTION_FAILED,
    TIMEOUT,
    INVALID_PACKET,
    CANCELLED,
    STORAGE,
    PARTIAL,
    INTERNAL,
}

data class HomeWatchSnapshot(
    val connectionState: HomeWatchConnectionState = HomeWatchConnectionState.Unknown,
    val deviceName: String? = null,
    val lastSuccessfulConnectionAt: Instant? = null,
    val lastImportedAt: Instant? = null,
)

data class HomeHealthConnectSyncModel(
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val syncingCount: Int = 0,
    val syncedCount: Int = 0,
    val lastSyncedAt: Instant? = null,
) {
    val hasVisibleState: Boolean
        get() = pendingCount > 0 || failedCount > 0 || syncingCount > 0 || syncedCount > 0 || lastSyncedAt != null
}

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
    /** Valor composto: D1 medido quando elegível, legacy estimado como fallback. */
    val dailyDistanceMeters: Double? = null,
    val dailyCaloriesKilocalories: Double? = null,
    val dailyDistanceIsMeasured: Boolean = false,
    val dailyCaloriesAreMeasured: Boolean = false,
    val completeness: WatchDailyReadCompleteness? = null,
)

data class HomeLatestHeartRateModel(
    val bpm: Int,
    val measuredAt: Instant,
)

data class HomePendingActivityModel(
    val id: String,
    val title: String,
    val subtitle: String?,
)

data class HomeWorkoutPendingBanner(
    val title: String,
    val message: String,
    val pendingReviewCount: Int,
    val pendingCalibrationCount: Int,
    val isCalibrationPriority: Boolean,
)
