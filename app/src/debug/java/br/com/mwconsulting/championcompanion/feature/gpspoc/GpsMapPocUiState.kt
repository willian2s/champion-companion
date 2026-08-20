package br.com.mwconsulting.championcompanion.feature.gpspoc

import br.com.mwconsulting.championcompanion.core.model.GpsAccuracyQuality
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePoint
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointRejectionReason

data class GpsMapPocUiState(
    val status: GpsMapPocStatus = GpsMapPocStatus.PermissionRequired,
    val mapStatus: GpsMapPocMapStatus = GpsMapPocMapStatus.Loading,
    val gpsReadiness: GpsMapPocGpsReadiness = GpsMapPocGpsReadiness.PermissionRequired,
    val gpsAccuracyQuality: GpsAccuracyQuality = GpsAccuracyQuality.UNKNOWN,
    val horizontalAccuracyMeters: Float? = null,
    val receivedFixCount: Int = 0,
    val distanceMeters: Double = 0.0,
    val acceptedPointCount: Int = 0,
    val rejectedPointCount: Int = 0,
    val rejectionCounts: Map<WorkoutRoutePointRejectionReason, Int> = emptyMap(),
    val routePoints: List<WorkoutRoutePoint> = emptyList(),
    val currentPoint: WorkoutRoutePoint? = null,
)

sealed interface GpsMapPocStatus {
    data object Initializing : GpsMapPocStatus
    data object PermissionRequired : GpsMapPocStatus
    data object SearchingGps : GpsMapPocStatus
    data object Ready : GpsMapPocStatus
    data object Tracking : GpsMapPocStatus
    data object GpsLost : GpsMapPocStatus
    data object Stopped : GpsMapPocStatus
    data class Error(val details: GpsMapPocError) : GpsMapPocStatus
}

sealed interface GpsMapPocMapStatus {
    data object Loading : GpsMapPocMapStatus
    data object Ready : GpsMapPocMapStatus
    data class Error(val details: GpsMapPocError) : GpsMapPocMapStatus
}

enum class GpsMapPocGpsReadiness {
    PermissionRequired,
    PrecisePermissionRequired,
    LocationDisabled,
    Searching,
    Ready,
    Lost,
    Unavailable,
}

sealed interface GpsMapPocError {
    val message: String

    data object MapUnavailable : GpsMapPocError {
        override val message: String = "Mapa-base indisponível no momento."
    }

    data object PermissionDenied : GpsMapPocError {
        override val message: String = "Permissão de localização precisa é necessária."
    }

    data object GpsUnavailable : GpsMapPocError {
        override val message: String = "GPS indisponível no momento."
    }

    data object LocationDisabled : GpsMapPocError {
        override val message: String = "Ative a localização do aparelho para continuar."
    }

    data object Internal : GpsMapPocError {
        override val message: String = "Não foi possível preparar a PoC."
    }
}
