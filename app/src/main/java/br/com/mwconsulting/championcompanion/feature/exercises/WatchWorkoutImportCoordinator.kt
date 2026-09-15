package br.com.mwconsulting.championcompanion.feature.exercises

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionTransportSession
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportResult
import java.time.Instant
import kotlinx.coroutines.flow.StateFlow

interface WatchWorkoutImportCoordinator {
    val state: StateFlow<WatchWorkoutImportSurfaceState>

    suspend fun startImport(request: WatchWorkoutImportRequest): br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportError?

    suspend fun cancelImport(
        reason: WatchWorkoutImportCancellationReason = WatchWorkoutImportCancellationReason.USER_REQUESTED,
    )

    fun reset()
}

/** Operação interna usada pelo owner composto da sincronização do relógio. */
interface WatchSyncWorkoutComponent {
    suspend fun importForWatchSync(
        request: WatchWorkoutImportRequest,
        transport: ChampionTransportSession,
    ): WatchSyncWorkoutResult
}

data class WatchSyncWorkoutResult(
    val result: WatchWorkoutImportResult,
    val startedAt: Instant,
    val finishedAt: Instant,
    val error: br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportError? = null,
)

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
