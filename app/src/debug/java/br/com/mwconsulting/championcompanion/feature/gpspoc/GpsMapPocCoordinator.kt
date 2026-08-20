package br.com.mwconsulting.championcompanion.feature.gpspoc

import android.content.Context
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationEvent
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationStartResult
import br.com.mwconsulting.championcompanion.core.location.RoutePointFilter
import br.com.mwconsulting.championcompanion.core.model.GpsAccuracyQuality
import br.com.mwconsulting.championcompanion.core.model.GpsPermissionState
import br.com.mwconsulting.championcompanion.core.model.GpsReadiness
import br.com.mwconsulting.championcompanion.core.model.GpsReadinessState
import br.com.mwconsulting.championcompanion.core.model.GpsServiceState
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteId
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointFilterInput
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointId
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointRejectionReason
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado efêmero da PoC; fonte GPS não participa do binding produtivo. */
class GpsMapPocCoordinator(context: Context) : AutoCloseable {
    private val mutableState = MutableStateFlow(GpsMapPocUiState())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val locationSource = GpsMapPocLocationSource(context.applicationContext)

    private var runtimeGeneration = -1L
    private var routeId: WorkoutRouteId? = null
    private var nextSequence = 0L
    private var previousAcceptedPoint: br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePoint? = null
    private var captureActive = false
    private var isClosed = false

    val state: StateFlow<GpsMapPocUiState> = mutableState.asStateFlow()

    init {
        scope.launch {
            locationSource.events.collect(::onLocationEvent)
        }
    }

    /** Inicia GPS somente após ação explícita da tela ou retorno da permissão. */
    fun prepareLocation() {
        if (isClosed || state.value.status == GpsMapPocStatus.Tracking) return
        startLocationRuntime()
    }

    fun onPermissionResult() = prepareLocation()

    fun start() {
        val current = state.value
        if (isClosed || current.status != GpsMapPocStatus.Ready ||
            current.mapStatus !is GpsMapPocMapStatus.Ready ||
            current.gpsReadiness != GpsMapPocGpsReadiness.Ready ||
            !current.gpsAccuracyQuality.isStartable()
        ) return

        runtimeGeneration += 1
        routeId = WorkoutRouteId(UUID.randomUUID())
        nextSequence = 0L
        previousAcceptedPoint = null
        captureActive = true
        mutableState.update {
            it.copy(
                status = GpsMapPocStatus.Tracking,
                receivedFixCount = 0,
                distanceMeters = 0.0,
                acceptedPointCount = 0,
                rejectedPointCount = 0,
                rejectionCounts = emptyMap(),
                routePoints = emptyList(),
                currentPoint = null,
            )
        }
        restartLocationRuntime()
    }

    fun stop() {
        captureActive = false
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

    /** Câmera pertence à 026-06; a ação mantém contrato seguro até lá. */
    fun recenter() = Unit

    fun clear() {
        val current = state.value
        if (isClosed || !current.status.canClear()) return
        captureActive = false
        routeId = null
        nextSequence = 0L
        previousAcceptedPoint = null
        runtimeGeneration += 1
        mutableState.update {
            it.copy(
                status = it.status.statusAfterClear(),
                gpsReadiness = it.gpsReadiness.afterClear(),
                gpsAccuracyQuality = GpsAccuracyQuality.UNKNOWN,
                horizontalAccuracyMeters = null,
                receivedFixCount = 0,
                distanceMeters = 0.0,
                acceptedPointCount = 0,
                rejectedPointCount = 0,
                rejectionCounts = emptyMap(),
                routePoints = emptyList(),
                currentPoint = null,
            )
        }
        if (current.status != GpsMapPocStatus.PermissionRequired) restartLocationRuntime()
    }

    fun markMapReady() {
        mutableState.update { it.copy(mapStatus = GpsMapPocMapStatus.Ready) }
    }

    /** Remove detalhes do provider e URLs da UI. */
    fun markMapError() {
        mutableState.update {
            it.copy(mapStatus = GpsMapPocMapStatus.Error(GpsMapPocError.MapUnavailable))
        }
    }

    override fun close() {
        if (isClosed) return
        isClosed = true
        locationSource.close()
        scope.cancel()
    }

    private fun startLocationRuntime() {
        runtimeGeneration += 1
        restartLocationRuntime()
    }

    private fun restartLocationRuntime() {
        val result = locationSource.start(runtimeGeneration)
        if (result != WorkoutLocationStartResult.STARTED) {
            mutableState.update {
                it.copy(
                    status = GpsMapPocStatus.Error(GpsMapPocError.GpsUnavailable),
                    gpsReadiness = GpsMapPocGpsReadiness.Unavailable,
                )
            }
        }
    }

    private fun onLocationEvent(event: WorkoutLocationEvent) {
        if (isClosed || event.runtimeGeneration != runtimeGeneration) return
        when (event) {
            is WorkoutLocationEvent.Permission -> onPermission(event.state)
            is WorkoutLocationEvent.Service -> onService(event.state)
            is WorkoutLocationEvent.Fix -> onFix(event)
            is WorkoutLocationEvent.Readiness -> onReadiness(event.value)
            is WorkoutLocationEvent.Gnss -> Unit
        }
    }

    private fun onPermission(permission: GpsPermissionState) {
        if (permission != GpsPermissionState.PRECISE) {
            mutableState.update {
                it.copy(
                    status = if (it.status == GpsMapPocStatus.Tracking) {
                        GpsMapPocStatus.GpsLost
                    } else {
                        GpsMapPocStatus.PermissionRequired
                    },
                    gpsReadiness = permission.readinessState(),
                )
            }
        }
    }

    private fun onService(service: GpsServiceState) {
        if (service == GpsServiceState.UNAVAILABLE) {
            mutableState.update {
                it.copy(
                    status = if (it.status == GpsMapPocStatus.Tracking) {
                        GpsMapPocStatus.GpsLost
                    } else {
                        GpsMapPocStatus.Error(GpsMapPocError.GpsUnavailable)
                    },
                    gpsReadiness = GpsMapPocGpsReadiness.Unavailable,
                )
            }
        }
    }

    private fun onReadiness(readiness: GpsReadiness) {
        mutableState.update { current ->
            val nextReadiness = readiness.toUiReadiness()
            current.copy(
                status = current.statusFor(readiness, captureActive),
                gpsReadiness = nextReadiness,
                gpsAccuracyQuality = readiness.accuracyQuality,
            )
        }
    }

    private fun onFix(event: WorkoutLocationEvent.Fix) {
        val candidate = event.candidate
        val current = state.value
        mutableState.update {
            it.copy(
                horizontalAccuracyMeters = candidate.horizontalAccuracyMeters.toFloat(),
                receivedFixCount = it.receivedFixCount + 1,
            )
        }
        if (!captureActive) return

        val activeRouteId = routeId ?: return
        val result = RoutePointFilter.filter(
            WorkoutRoutePointFilterInput(
                pointId = WorkoutRoutePointId(UUID.randomUUID()),
                routeId = activeRouteId,
                sequence = nextSequence,
                segmentIndex = 0,
                observedAt = Instant.now(),
                expectedRuntimeGeneration = runtimeGeneration,
                candidate = candidate,
                previousAcceptedPoint = previousAcceptedPoint,
            ),
        )
        when (result) {
            is br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointFilterResult.Accepted -> {
                nextSequence += 1
                previousAcceptedPoint = result.point
                mutableState.update {
                    it.copy(
                        distanceMeters = it.distanceMeters + result.distanceFromPreviousMeters,
                        acceptedPointCount = it.acceptedPointCount + 1,
                        routePoints = it.routePoints + result.point,
                        currentPoint = result.point,
                    )
                }
            }

            is br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointFilterResult.Rejected -> {
                mutableState.update {
                    it.copy(
                        rejectedPointCount = it.rejectedPointCount + 1,
                        rejectionCounts = it.rejectionCounts.increment(result.reason),
                    )
                }
            }
        }
    }
}

private fun GpsPermissionState.readinessState(): GpsMapPocGpsReadiness = when (this) {
    GpsPermissionState.APPROXIMATE -> GpsMapPocGpsReadiness.PrecisePermissionRequired
    GpsPermissionState.NOT_REQUESTED,
    GpsPermissionState.DENIED,
    GpsPermissionState.PRECISE,
    -> GpsMapPocGpsReadiness.PermissionRequired
}

private fun GpsReadiness.toUiReadiness(): GpsMapPocGpsReadiness = when (state) {
    GpsReadinessState.PERMISSION_REQUIRED -> GpsMapPocGpsReadiness.PermissionRequired
    GpsReadinessState.PRECISE_PERMISSION_REQUIRED -> GpsMapPocGpsReadiness.PrecisePermissionRequired
    GpsReadinessState.LOCATION_DISABLED -> GpsMapPocGpsReadiness.LocationDisabled
    GpsReadinessState.SEARCHING -> GpsMapPocGpsReadiness.Searching
    GpsReadinessState.READY -> GpsMapPocGpsReadiness.Ready
    GpsReadinessState.LOST -> GpsMapPocGpsReadiness.Lost
    GpsReadinessState.UNAVAILABLE -> GpsMapPocGpsReadiness.Unavailable
}

private fun GpsMapPocUiState.statusFor(
    readiness: GpsReadiness,
    captureActive: Boolean,
): GpsMapPocStatus = when {
    captureActive && readiness.state == GpsReadinessState.READY -> GpsMapPocStatus.Tracking
    captureActive -> GpsMapPocStatus.GpsLost
    status == GpsMapPocStatus.Stopped -> status
    readiness.state == GpsReadinessState.PERMISSION_REQUIRED ||
        readiness.state == GpsReadinessState.PRECISE_PERMISSION_REQUIRED -> GpsMapPocStatus.PermissionRequired
    readiness.state == GpsReadinessState.UNAVAILABLE -> GpsMapPocStatus.Error(GpsMapPocError.GpsUnavailable)
    readiness.state == GpsReadinessState.LOCATION_DISABLED ->
        GpsMapPocStatus.Error(GpsMapPocError.LocationDisabled)
    readiness.state == GpsReadinessState.READY -> GpsMapPocStatus.Ready
    else -> GpsMapPocStatus.SearchingGps
}

private fun GpsMapPocGpsReadiness.afterClear(): GpsMapPocGpsReadiness = when (this) {
    GpsMapPocGpsReadiness.Lost,
    GpsMapPocGpsReadiness.Unavailable,
    -> GpsMapPocGpsReadiness.Searching

    GpsMapPocGpsReadiness.PermissionRequired,
    GpsMapPocGpsReadiness.PrecisePermissionRequired,
    GpsMapPocGpsReadiness.LocationDisabled,
    GpsMapPocGpsReadiness.Searching,
    GpsMapPocGpsReadiness.Ready,
    -> this
}

private fun GpsMapPocStatus.statusAfterClear(): GpsMapPocStatus = when (this) {
    GpsMapPocStatus.PermissionRequired -> GpsMapPocStatus.PermissionRequired
    GpsMapPocStatus.SearchingGps -> GpsMapPocStatus.SearchingGps
    GpsMapPocStatus.Ready,
    GpsMapPocStatus.Stopped,
    is GpsMapPocStatus.Error,
    -> GpsMapPocStatus.SearchingGps

    else -> this
}

private fun GpsMapPocStatus.canClear(): Boolean = this in setOf(
    GpsMapPocStatus.PermissionRequired,
    GpsMapPocStatus.SearchingGps,
    GpsMapPocStatus.Ready,
    GpsMapPocStatus.Stopped,
) || this is GpsMapPocStatus.Error

private fun GpsAccuracyQuality.isStartable(): Boolean = this == GpsAccuracyQuality.GOOD ||
    this == GpsAccuracyQuality.STRONG

private fun Map<WorkoutRoutePointRejectionReason, Int>.increment(
    reason: WorkoutRoutePointRejectionReason,
): Map<WorkoutRoutePointRejectionReason, Int> = toMutableMap().apply {
    this[reason] = (this[reason] ?: 0) + 1
}
