package br.com.mwconsulting.championcompanion.feature.exercises

import br.com.mwconsulting.championcompanion.core.model.Device
import kotlinx.coroutines.flow.StateFlow

interface WatchWorkoutImportCoordinator {
    val state: StateFlow<WatchWorkoutImportSurfaceState>

    suspend fun startImport(request: WatchWorkoutImportRequest): br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportError?

    suspend fun cancelImport(
        reason: WatchWorkoutImportCancellationReason = WatchWorkoutImportCancellationReason.USER_REQUESTED,
    )

    fun reset()
}

data class WatchWorkoutImportRequest(
    val importAttemptId: String,
    val associatedDevice: Device,
)

enum class WatchWorkoutImportCancellationReason {
    USER_REQUESTED,
    BACKGROUND,
    EXIT_CONFIRMED,
    INTERNAL_ERROR,
}
