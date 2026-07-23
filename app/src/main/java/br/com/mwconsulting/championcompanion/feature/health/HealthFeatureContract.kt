package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import br.com.mwconsulting.championcompanion.core.model.DEFAULT_DAILY_STEPS_GOAL
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
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
    data object Retry : HealthIntent
    data object DismissError : HealthIntent
    data object ConfirmExitMeasurement : HealthIntent
    data object AppMovedToBackground : HealthIntent
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
