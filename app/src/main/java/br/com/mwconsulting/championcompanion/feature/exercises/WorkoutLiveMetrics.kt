package br.com.mwconsulting.championcompanion.feature.exercises

import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class WorkoutGpsCaptureStatus {
    STARTING,
    READY,
    SEARCHING,
    LOST,
    UNAVAILABLE,
    PERMISSION_REQUIRED,
    LOCATION_DISABLED,
    PAUSED,
    ERROR,
}

data class WorkoutLiveMetricsSnapshot(
    val sessionId: AppWorkoutSessionId,
    val distanceMeters: Double,
    val currentPaceSecondsPerKilometer: Double?,
    val averagePaceSecondsPerKilometer: Double?,
    val currentSpeedMetersPerSecond: Double?,
    val averageSpeedMetersPerSecond: Double?,
    val acceptedPointCount: Long,
    val segmentCount: Int,
    val gpsStatus: WorkoutGpsCaptureStatus,
    val captureError: String? = null,
) {
    init {
        require(distanceMeters.isFinite() && distanceMeters >= 0.0)
        require(currentPaceSecondsPerKilometer.isValidMetric())
        require(averagePaceSecondsPerKilometer.isValidMetric())
        require(currentSpeedMetersPerSecond.isValidMetric())
        require(averageSpeedMetersPerSecond.isValidMetric())
        require(acceptedPointCount >= 0)
        require(segmentCount >= 0)
    }
}

@Singleton
class WorkoutLiveMetricsStore @Inject constructor() {
    private val mutableSnapshot = MutableStateFlow<WorkoutLiveMetricsSnapshot?>(null)
    val snapshot = mutableSnapshot.asStateFlow()

    fun observe(sessionId: AppWorkoutSessionId): Flow<WorkoutLiveMetricsSnapshot?> =
        snapshot
            .map { it?.takeIf { current -> current.sessionId == sessionId } }
            .distinctUntilChanged()

    fun update(value: WorkoutLiveMetricsSnapshot) {
        mutableSnapshot.value = value
    }

    fun clear(sessionId: AppWorkoutSessionId) {
        if (mutableSnapshot.value?.sessionId == sessionId) {
            mutableSnapshot.value = null
        }
    }
}

private fun Double?.isValidMetric(): Boolean = this == null || isFinite() && this >= 0.0
