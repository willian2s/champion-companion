package br.com.mwconsulting.championcompanion.core.location

import br.com.mwconsulting.championcompanion.core.model.GnssQualitySupplement
import br.com.mwconsulting.championcompanion.core.model.GpsPermissionState
import br.com.mwconsulting.championcompanion.core.model.GpsReadiness
import br.com.mwconsulting.championcompanion.core.model.GpsServiceState
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointCandidate
import kotlinx.coroutines.flow.SharedFlow

sealed interface WorkoutLocationEvent {
    val runtimeGeneration: Long

    data class Permission(
        override val runtimeGeneration: Long,
        val state: GpsPermissionState,
    ) : WorkoutLocationEvent

    data class Service(
        override val runtimeGeneration: Long,
        val state: GpsServiceState,
    ) : WorkoutLocationEvent

    /** Fix efêmero; consumidores de preparação não devem persistir este valor. */
    data class Fix(
        override val runtimeGeneration: Long,
        val candidate: WorkoutRoutePointCandidate,
    ) : WorkoutLocationEvent

    data class Gnss(
        override val runtimeGeneration: Long,
        val quality: GnssQualitySupplement?,
    ) : WorkoutLocationEvent

    data class Readiness(
        override val runtimeGeneration: Long,
        val value: GpsReadiness,
    ) : WorkoutLocationEvent
}

enum class WorkoutLocationStartResult {
    STARTED,
    STALE_RUNTIME_GENERATION,
    SOURCE_CLOSED,
}

interface WorkoutLocationSource : AutoCloseable {
    /** Inscreva o consumidor antes de iniciar para receber o snapshot inicial completo. */
    val events: SharedFlow<WorkoutLocationEvent>

    fun start(runtimeGeneration: Long): WorkoutLocationStartResult

    /** Retorna falso quando outra geração já é dona do runtime. */
    fun stop(runtimeGeneration: Long): Boolean

    override fun close()
}

fun interface WorkoutLocationSourceFactory {
    /** Cada preparação/sessão recebe runtime isolado. */
    fun create(): WorkoutLocationSource
}
