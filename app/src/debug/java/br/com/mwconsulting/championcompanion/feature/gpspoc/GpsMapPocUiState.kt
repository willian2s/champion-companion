package br.com.mwconsulting.championcompanion.feature.gpspoc

data class GpsMapPocUiState(
    val status: GpsMapPocStatus = GpsMapPocStatus.Initializing,
    val mapStatus: GpsMapPocMapStatus = GpsMapPocMapStatus.Loading,
    val gpsReadiness: GpsMapPocGpsReadiness = GpsMapPocGpsReadiness.Searching,
    val horizontalAccuracyMeters: Float? = null,
    val distanceMeters: Double = 0.0,
    val acceptedPointCount: Int = 0,
    val rejectedPointCount: Int = 0,
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

    data object Internal : GpsMapPocError {
        override val message: String = "Não foi possível preparar a PoC."
    }
}
