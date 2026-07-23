package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.model.Device
import java.time.LocalDate
import java.time.ZoneId
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

enum class D1HeartRateImportCancellationReason {
    USER_REQUESTED,
    BACKGROUND,
    EXIT_CONFIRMED,
    INTERNAL_ERROR,
}
