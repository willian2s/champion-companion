package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.model.Device
import kotlinx.coroutines.flow.StateFlow

interface ManualHeartRateCoordinator {
    val state: StateFlow<HealthSurfaceState>

    suspend fun startMeasurement(request: ManualHeartRateMeasurementRequest): HealthMeasurementError?

    suspend fun cancelMeasurement(reason: ManualHeartRateCancellationReason = ManualHeartRateCancellationReason.USER_REQUESTED)

    fun reset()
}

data class ManualHeartRateMeasurementRequest(
    val manualMeasurementSessionId: String,
    val associatedDevice: Device,
)

enum class ManualHeartRateCancellationReason {
    USER_REQUESTED,
    TIMEOUT,
    BACKGROUND,
    EXIT_CONFIRMED,
    INTERNAL_ERROR,
}
