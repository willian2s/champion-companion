package br.com.mwconsulting.championcompanion.core.healthconnect

import br.com.mwconsulting.championcompanion.core.model.SafeSyncError
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Última boundary do sync foreground. Uma falha inesperada permanece observável
 * como resultado controlado e nunca encerra a coroutine da UI por exceção.
 */
@Singleton
class SafeHealthConnectSyncCoordinator @Inject constructor(
    private val delegate: DefaultHealthConnectSyncCoordinator,
) : HealthConnectSyncCoordinator {
    override suspend fun syncPending(limit: Int): HealthConnectSyncOutcome =
        runSafely(limit) { delegate.syncPending(limit) }

    override suspend fun syncActivity(
        activityId: WorkoutActivityId,
        limit: Int,
    ): HealthConnectSyncOutcome = runSafely(limit) {
        delegate.syncActivity(activityId, limit)
    }

    private suspend fun runSafely(
        limit: Int,
        block: suspend () -> HealthConnectSyncOutcome,
    ): HealthConnectSyncOutcome {
        val startedAt = Instant.now()
        return try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            HealthConnectSyncOutcome.Failed(
                report = HealthConnectSyncReport(
                    startedAt = startedAt,
                    finishedAt = Instant.now(),
                    limit = limit.coerceAtLeast(1),
                    failedCount = 1,
                ),
                error = SafeSyncError.INTERNAL,
                reason = "Falha interna controlada durante a sincronização com o Health Connect.",
            )
        }
    }
}
