package br.com.mwconsulting.championcompanion.core.location

import br.com.mwconsulting.championcompanion.core.logging.LogEvent
import br.com.mwconsulting.championcompanion.core.logging.StructuredLogger
import br.com.mwconsulting.championcompanion.core.model.GnssQualitySupplement
import br.com.mwconsulting.championcompanion.core.model.GpsReadiness
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointRejectionReason

enum class WorkoutLocationRuntimeLogCategory {
    STARTED,
    STOPPED,
}

/** Nunca recebe fix, coordenadas, precisão numérica ou instantes. */
class WorkoutLocationLogger(
    private val logger: StructuredLogger,
) {
    fun runtime(runtimeGeneration: Long, category: WorkoutLocationRuntimeLogCategory) {
        logger.debug(
            LogEvent.GpsRuntimeState,
            mapOf("generation" to runtimeGeneration.toString(), "category" to category.name.lowercase()),
        )
    }

    fun readiness(runtimeGeneration: Long, readiness: GpsReadiness) {
        logger.debug(
            LogEvent.GpsRuntimeState,
            mapOf(
                "generation" to runtimeGeneration.toString(),
                "category" to "readiness",
                "state" to readiness.state.name.lowercase(),
                "quality_band" to readiness.accuracyQuality.name.lowercase(),
            ),
        )
    }

    fun gnss(runtimeGeneration: Long, quality: GnssQualitySupplement?) {
        logger.debug(
            LogEvent.GpsRuntimeState,
            mapOf(
                "generation" to runtimeGeneration.toString(),
                "category" to if (quality == null) "gnss_unavailable" else "gnss_observed",
                "visible_count" to quality?.visibleSatelliteCount?.toString(),
                "used_count" to quality?.usedInFixSatelliteCount?.toString(),
            ),
        )
    }

    fun pointFilterSummary(
        runtimeGeneration: Long,
        acceptedCount: Long,
        rejectedCount: Long,
        lastRejection: WorkoutRoutePointRejectionReason?,
    ) {
        require(acceptedCount >= 0 && rejectedCount >= 0)
        logger.debug(
            LogEvent.WorkoutPointFilterSummary,
            mapOf(
                "generation" to runtimeGeneration.toString(),
                "category" to (lastRejection?.name?.lowercase() ?: "accepted"),
                "accepted_count" to acceptedCount.toString(),
                "rejected_count" to rejectedCount.toString(),
            ),
        )
    }
}
