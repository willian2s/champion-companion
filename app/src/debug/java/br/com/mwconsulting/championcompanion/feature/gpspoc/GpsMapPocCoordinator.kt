package br.com.mwconsulting.championcompanion.feature.gpspoc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Ephemeral state store for the debug PoC. GPS and route behavior arrive in later subtasks. */
class GpsMapPocCoordinator {
    private val mutableState = MutableStateFlow(GpsMapPocUiState())

    val state: StateFlow<GpsMapPocUiState> = mutableState.asStateFlow()

    fun start() {
        mutableState.update { current ->
            if (current.status == GpsMapPocStatus.Ready &&
                current.mapStatus is GpsMapPocMapStatus.Ready &&
                current.gpsReadiness == GpsMapPocGpsReadiness.Ready
            ) {
                current.copy(status = GpsMapPocStatus.Tracking)
            } else {
                current
            }
        }
    }

    fun stop() {
        mutableState.update { current ->
            if (current.status == GpsMapPocStatus.Tracking ||
                current.status == GpsMapPocStatus.GpsLost
            ) {
                current.copy(status = GpsMapPocStatus.Stopped)
            } else {
                current
            }
        }
    }

    /** Camera movement belongs to 026-06; this action is intentionally a safe no-op for now. */
    fun recenter() {
        val current = state.value
        if (current.mapStatus is GpsMapPocMapStatus.Ready && current.canRecenter()) {
            // Reserved for the camera coordinator. Do not enable MapLibre's location component.
        }
    }

    fun clear() {
        mutableState.update { current ->
            if (!current.status.canClear()) {
                current
            } else {
                current.copy(
                    status = current.statusAfterClear(),
                    gpsReadiness = current.gpsReadiness.afterClear(),
                    horizontalAccuracyMeters = null,
                    distanceMeters = 0.0,
                    acceptedPointCount = 0,
                    rejectedPointCount = 0,
                )
            }
        }
    }

    fun markMapReady() {
        mutableState.update { it.copy(mapStatus = GpsMapPocMapStatus.Ready) }
    }

    /** Deliberately drops provider details so URLs and implementation diagnostics never reach UI. */
    fun markMapError() {
        mutableState.update {
            it.copy(mapStatus = GpsMapPocMapStatus.Error(GpsMapPocError.MapUnavailable))
        }
    }

    fun markPermissionRequired() {
        mutableState.update {
            it.copy(
                status = GpsMapPocStatus.PermissionRequired,
                gpsReadiness = GpsMapPocGpsReadiness.PermissionRequired,
            )
        }
    }

    fun markGpsSearching() {
        mutableState.update {
            it.copy(
                status = if (it.status == GpsMapPocStatus.Tracking) {
                    GpsMapPocStatus.GpsLost
                } else {
                    GpsMapPocStatus.SearchingGps
                },
                gpsReadiness = GpsMapPocGpsReadiness.Searching,
            )
        }
    }

    fun markGpsReady() {
        mutableState.update {
            it.copy(
                status = when (it.status) {
                    GpsMapPocStatus.Initializing,
                    GpsMapPocStatus.PermissionRequired,
                    GpsMapPocStatus.SearchingGps,
                    GpsMapPocStatus.GpsLost,
                    GpsMapPocStatus.Stopped,
                    is GpsMapPocStatus.Error,
                    -> GpsMapPocStatus.Ready

                    GpsMapPocStatus.Ready,
                    GpsMapPocStatus.Tracking,
                    -> it.status
                },
                gpsReadiness = GpsMapPocGpsReadiness.Ready,
            )
        }
    }

    fun markGpsLost() {
        mutableState.update {
            it.copy(
                status = if (it.status == GpsMapPocStatus.Tracking) {
                    GpsMapPocStatus.GpsLost
                } else {
                    it.status
                },
                gpsReadiness = GpsMapPocGpsReadiness.Lost,
            )
        }
    }

    fun markGpsUnavailable() {
        mutableState.update {
            it.copy(
                status = GpsMapPocStatus.Error(GpsMapPocError.GpsUnavailable),
                gpsReadiness = GpsMapPocGpsReadiness.Unavailable,
            )
        }
    }

    fun markError(error: GpsMapPocError = GpsMapPocError.Internal) {
        mutableState.update { it.copy(status = GpsMapPocStatus.Error(error)) }
    }
}

private fun GpsMapPocUiState.canRecenter(): Boolean = status in setOf(
    GpsMapPocStatus.Ready,
    GpsMapPocStatus.Tracking,
    GpsMapPocStatus.GpsLost,
    GpsMapPocStatus.Stopped,
)

private fun GpsMapPocStatus.canClear(): Boolean = this in setOf(
    GpsMapPocStatus.PermissionRequired,
    GpsMapPocStatus.SearchingGps,
    GpsMapPocStatus.Ready,
    GpsMapPocStatus.Stopped,
) || this is GpsMapPocStatus.Error

private fun GpsMapPocUiState.statusAfterClear(): GpsMapPocStatus = when (gpsReadiness) {
    GpsMapPocGpsReadiness.PermissionRequired -> GpsMapPocStatus.PermissionRequired
    GpsMapPocGpsReadiness.Searching -> GpsMapPocStatus.SearchingGps

    GpsMapPocGpsReadiness.Ready -> GpsMapPocStatus.Ready
    GpsMapPocGpsReadiness.Lost,
    GpsMapPocGpsReadiness.Unavailable,
    -> GpsMapPocStatus.SearchingGps
}

private fun GpsMapPocGpsReadiness.afterClear(): GpsMapPocGpsReadiness = when (this) {
    GpsMapPocGpsReadiness.Lost,
    GpsMapPocGpsReadiness.Unavailable,
    -> GpsMapPocGpsReadiness.Searching

    GpsMapPocGpsReadiness.PermissionRequired,
    GpsMapPocGpsReadiness.Searching,
    GpsMapPocGpsReadiness.Ready,
    -> this
}
