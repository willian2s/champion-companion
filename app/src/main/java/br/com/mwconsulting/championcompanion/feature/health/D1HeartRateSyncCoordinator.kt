package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionTransportSession
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportResult
import br.com.mwconsulting.championcompanion.core.model.Device
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import kotlinx.coroutines.flow.StateFlow

interface D1HeartRateSyncCoordinator {
    val state: StateFlow<D1HeartRateImportSurfaceState>

    suspend fun startImport(request: D1HeartRateSyncRequest): HealthD1ImportError?

    suspend fun cancelImport(reason: D1HeartRateImportCancellationReason = D1HeartRateImportCancellationReason.USER_REQUESTED)

    fun reset()
}

data class D1HeartRateSyncRequest(
    val importAttemptId: String,
    val associatedDevice: Device,
    val baseLocalDate: LocalDate = LocalDate.now(),
    val zoneId: ZoneId = ZoneId.systemDefault(),
)

/** Operação interna usada pelo owner composto da sincronização do relógio. */
interface WatchSyncD1Component {
    suspend fun importForWatchSync(
        request: D1HeartRateSyncRequest,
        transport: ChampionTransportSession,
    ): WatchSyncD1Result
}

data class WatchSyncD1Result(
    val result: D1HeartRateImportResult,
    val startedAt: Instant,
    val finishedAt: Instant,
    val error: HealthD1ImportError? = null,
)

enum class D1HeartRateImportCancellationReason {
    USER_REQUESTED,
    BACKGROUND,
    EXIT_CONFIRMED,
    INTERNAL_ERROR,
}
