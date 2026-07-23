package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import br.com.mwconsulting.championcompanion.core.model.DEFAULT_DAILY_STEPS_GOAL
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportMetadata
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportResult
import br.com.mwconsulting.championcompanion.core.model.D1HeartRatePacketProgress
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryPeriod
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistorySnapshot
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.MonotonicTimestamp
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

const val MANUAL_HEART_RATE_TARGET_DURATION_SECONDS: Long = 60L

val ManualHeartRateTargetDuration: kotlin.time.Duration = MANUAL_HEART_RATE_TARGET_DURATION_SECONDS.seconds

interface HealthFeatureContract : UiStateStore<HealthUiState> {
    fun onIntent(intent: HealthIntent)
}

data class HealthUiState(
    val surfaceState: HealthSurfaceState = HealthSurfaceState.Idle,
    val d1ImportState: D1HeartRateImportSurfaceState = D1HeartRateImportSurfaceState.Idle,
    val selectedHistoryPeriod: HeartRateHistoryPeriod = HeartRateHistoryPeriod.TODAY,
    val history: HeartRateHistorySnapshot = HeartRateHistorySnapshot(period = HeartRateHistoryPeriod.TODAY),
    val latestD1ImportMetadata: D1HeartRateImportMetadata? = null,
    val latestMeasurement: HeartRateMeasurement? = null,
    val recentMeasurements: List<HeartRateMeasurement> = emptyList(),
    val latestWeight: HealthWeightSnapshot? = null,
    val todaySummary: HealthDailySummarySnapshot? = null,
    val dailyStepsGoal: Int = DEFAULT_DAILY_STEPS_GOAL,
    val hasAssociatedDevice: Boolean = false,
    val associatedDeviceName: String? = null,
)

data class HealthWeightSnapshot(
    val kilograms: Double,
    val measuredAt: Instant,
    val origin: DataOrigin?,
)

data class HealthDailySummarySnapshot(
    val steps: Long,
    val estimatedDistanceMeters: Double?,
    val updatedAt: Instant,
    val source: DataOrigin,
    val sourceLabel: String,
)

sealed interface HealthIntent {
    data object Load : HealthIntent
    data object StartMeasurement : HealthIntent
    data object CancelMeasurement : HealthIntent
    data object StartD1Import : HealthIntent
    data object CancelD1Import : HealthIntent
    data object Retry : HealthIntent
    data object RetryD1Import : HealthIntent
    data object DismissError : HealthIntent
    data object DismissD1Import : HealthIntent
    data object ConfirmExitMeasurement : HealthIntent
    data object ConfirmExitD1Import : HealthIntent
    data object AppMovedToBackground : HealthIntent
    data class SelectHistoryPeriod(val period: HeartRateHistoryPeriod) : HealthIntent
    data object OpenDevice : HealthIntent
}

sealed interface HealthSurfaceState {
    data object Idle : HealthSurfaceState

    data class Preparing(
        val session: ManualHeartRateSessionState,
    ) : HealthSurfaceState

    data class Connecting(
        val session: ManualHeartRateSessionState,
    ) : HealthSurfaceState

    data class Starting(
        val session: ManualHeartRateSessionState,
    ) : HealthSurfaceState

    data class Measuring(
        val session: ManualHeartRateSessionState,
    ) : HealthSurfaceState

    data class Stopping(
        val session: ManualHeartRateSessionState,
    ) : HealthSurfaceState

    data class Completed(
        val session: ManualHeartRateSessionState,
    ) : HealthSurfaceState

    data class Cancelled(
        val session: ManualHeartRateSessionState,
    ) : HealthSurfaceState

    data class Failed(
        val session: ManualHeartRateSessionState,
    ) : HealthSurfaceState
}

sealed interface D1HeartRateImportSurfaceState {
    data object Idle : D1HeartRateImportSurfaceState

    data class Preparing(
        val session: D1HeartRateImportSessionState,
    ) : D1HeartRateImportSurfaceState

    data class Connecting(
        val session: D1HeartRateImportSessionState,
    ) : D1HeartRateImportSurfaceState

    data class Reading(
        val session: D1HeartRateImportSessionState,
    ) : D1HeartRateImportSurfaceState

    data class Persisting(
        val session: D1HeartRateImportSessionState,
    ) : D1HeartRateImportSurfaceState

    data class Completed(
        val session: D1HeartRateImportSessionState,
    ) : D1HeartRateImportSurfaceState

    data class CompletedWithGaps(
        val session: D1HeartRateImportSessionState,
    ) : D1HeartRateImportSurfaceState

    data class Partial(
        val session: D1HeartRateImportSessionState,
    ) : D1HeartRateImportSurfaceState

    data class Failed(
        val session: D1HeartRateImportSessionState,
    ) : D1HeartRateImportSurfaceState

    data class Cancelled(
        val session: D1HeartRateImportSessionState,
    ) : D1HeartRateImportSurfaceState
}

data class D1HeartRateImportSessionState(
    val importAttemptId: String,
    val bleSessionId: String? = null,
    val startedAt: Instant? = null,
    val progress: D1HeartRatePacketProgress = D1HeartRatePacketProgress(),
    val result: D1HeartRateImportResult? = null,
    val metadata: D1HeartRateImportMetadata? = null,
    val error: HealthD1ImportError? = null,
)

data class HealthD1ImportError(
    val category: HealthD1ImportErrorCategory,
    val message: String,
    val isRetryable: Boolean,
)

enum class HealthD1ImportErrorCategory {
    NO_ASSOCIATED_DEVICE,
    PERMISSIONS_REQUIRED,
    BLUETOOTH_DISABLED,
    DEVICE_NOT_FOUND,
    CONNECTION_FAILED,
    HANDSHAKE_FAILED,
    F008_UNAVAILABLE,
    SEND_COMMAND_FAILED,
    TIMEOUT_EMPTY,
    TIMEOUT_PARTIAL,
    INVALID_PACKET,
    NO_VALID_RECORDS,
    STORAGE_FAILED,
    BLE_OPERATION_IN_PROGRESS,
    UNEXPECTED_DISCONNECT,
    CANCELLED,
    INTERNAL,
}

data class ManualHeartRateSessionState(
    val manualMeasurementSessionId: String,
    val bleSessionId: String? = null,
    val startedAt: Instant? = null,
    val monotonicStartedAt: MonotonicTimestamp? = null,
    val progress: Float = 0f,
    val remaining: kotlin.time.Duration = ManualHeartRateTargetDuration,
    val currentReading: ManualHeartRateCurrentReading? = null,
    val signalQuality: ManualHeartRateSignalQuality? = null,
    val finalResult: ManualHeartRateResult? = null,
    val error: HealthMeasurementError? = null,
)

data class ManualHeartRateCurrentReading(
    val bpm: Int,
    val source: ManualHeartRateReadingSource,
    val observedAt: Instant,
)

data class ManualHeartRateResult(
    val bpm: Int,
    val source: ManualHeartRateFinalSource,
    val measuredAt: Instant,
)

enum class ManualHeartRateReadingSource {
    D0_LIVE,
    F002_ESTIMATE,
}

enum class ManualHeartRateFinalSource {
    D0_FINAL,
    F002_FINAL,
}

enum class ManualHeartRateSignalQuality {
    GOOD,
    REGULAR,
    LOW,
}

data class HealthMeasurementError(
    val category: HealthMeasurementErrorCategory,
    val message: String,
    val isRetryable: Boolean,
)

enum class HealthMeasurementErrorCategory {
    NO_ASSOCIATED_DEVICE,
    PERMISSIONS_REQUIRED,
    BLUETOOTH_DISABLED,
    DEVICE_NOT_FOUND,
    CONNECTION_FAILED,
    HANDSHAKE_FAILED,
    F002_UNAVAILABLE,
    START_FAILED,
    UNEXPECTED_DISCONNECT,
    NO_VALID_READING,
    STOP_FAILED,
    STORAGE_FAILED,
    MEASUREMENT_IN_PROGRESS,
    BLE_OPERATION_IN_PROGRESS,
    CANCELLED,
    INTERNAL,
}
