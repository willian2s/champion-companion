package br.com.mwconsulting.championcompanion.feature.gpspoc

import br.com.mwconsulting.championcompanion.core.model.GpsAccuracyQuality
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePoint
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointRejectionReason
import java.time.Instant
import java.util.UUID

data class GpsMapPocUiState(
    val status: GpsMapPocStatus = GpsMapPocStatus.PermissionRequired,
    val mapStatus: GpsMapPocMapStatus = GpsMapPocMapStatus.Loading,
    val mapFailureIsSimulated: Boolean = false,
    val gpsReadiness: GpsMapPocGpsReadiness = GpsMapPocGpsReadiness.PermissionRequired,
    val gpsAccuracyQuality: GpsAccuracyQuality = GpsAccuracyQuality.UNKNOWN,
    val horizontalAccuracyMeters: Float? = null,
    val receivedFixCount: Int = 0,
    val distanceMeters: Double = 0.0,
    val acceptedPointCount: Int = 0,
    val rejectedPointCount: Int = 0,
    val rejectionCounts: Map<WorkoutRoutePointRejectionReason, Int> = emptyMap(),
    val accuracyBucketCounts: Map<GpsMapPocAccuracyBucket, Int> = emptyMap(),
    val routePoints: List<WorkoutRoutePoint> = emptyList(),
    val currentPoint: WorkoutRoutePoint? = null,
    val currentPosition: GpsMapPocCurrentPosition? = null,
    val followUser: Boolean = false,
    val recenterRequestId: Long = 0L,
)

enum class GpsMapPocAccuracyBucket(val label: String) {
    UP_TO_3_METERS("<=3 m"),
    OVER_3_TO_5_METERS(">3–5 m"),
    OVER_5_TO_10_METERS(">5–10 m"),
    OVER_10_TO_20_METERS(">10–20 m"),
    OVER_20_METERS(">20 m"),
    INVALID("inválida"),
}

data class GpsMapPocCurrentPosition(
    val id: UUID,
    val sampledAt: Instant,
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
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
