package br.com.mwconsulting.championcompanion.feature.gpspoc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import br.com.mwconsulting.championcompanion.core.location.GpsReadinessEvaluator
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationEvent
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationSource
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationStartResult
import br.com.mwconsulting.championcompanion.core.model.GpsPermissionState
import br.com.mwconsulting.championcompanion.core.model.GpsReadiness
import br.com.mwconsulting.championcompanion.core.model.GpsReadinessInput
import br.com.mwconsulting.championcompanion.core.model.GpsReadinessPolicy
import br.com.mwconsulting.championcompanion.core.model.GpsServiceState
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointCandidate
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.OnCompleteListener
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executor
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Fonte fused independente do binding produtivo e limitada à variante debug. */
@Suppress("MissingPermission")
internal class GpsMapPocLocationSource(
    context: Context,
    private val locationManager: LocationManager =
        checkNotNull(context.getSystemService(LocationManager::class.java)),
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context),
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val clock: () -> Instant = Instant::now,
    private val elapsedRealtimeNanos: () -> Long = SystemClock::elapsedRealtimeNanos,
) : WorkoutLocationSource {
    private val applicationContext = context.applicationContext
    private val mainExecutor = Executor { command -> handler.post(command) }
    private val lock = Any()
    private val mutableEvents = MutableSharedFlow<WorkoutLocationEvent>(
        replay = 1,
        extraBufferCapacity = EVENT_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private var activeRuntime: Runtime? = null
    private var latestGeneration = -1L
    private var isClosed = false

    override val events: SharedFlow<WorkoutLocationEvent> = mutableEvents.asSharedFlow()

    override fun start(runtimeGeneration: Long): WorkoutLocationStartResult {
        require(runtimeGeneration >= 0)
        val previous: Runtime?
        val runtime: Runtime
        synchronized(lock) {
            if (isClosed) return WorkoutLocationStartResult.SOURCE_CLOSED
            if (runtimeGeneration < latestGeneration) {
                return WorkoutLocationStartResult.STALE_RUNTIME_GENERATION
            }
            latestGeneration = runtimeGeneration
            previous = activeRuntime
            runtime = Runtime(runtimeGeneration)
            activeRuntime = runtime
        }
        handler.post {
            previous?.let(::cleanup)
            if (isActive(runtime)) register(runtime)
        }
        return WorkoutLocationStartResult.STARTED
    }

    override fun stop(runtimeGeneration: Long): Boolean {
        val runtime = synchronized(lock) {
            val current = activeRuntime
            if (current?.generation != runtimeGeneration) return false
            activeRuntime = null
            current
        }
        handler.post { cleanup(runtime) }
        return true
    }

    override fun close() {
        val runtime = synchronized(lock) {
            if (isClosed) return
            isClosed = true
            activeRuntime.also { activeRuntime = null }
        }
        handler.post { runtime?.let(::cleanup) }
    }

    private fun register(runtime: Runtime) {
        if (!isActive(runtime) || runtime.cleaned) return
        updateEnvironment(runtime, force = true)
        if (runtime.permission != GpsPermissionState.PRECISE) return
        if (runtime.service != GpsServiceState.ENABLED) {
            scheduleTick(runtime)
            return
        }

        registerFusedUpdates(runtime)
        requestLastLocation(runtime)
        scheduleTick(runtime)
    }

    private fun registerFusedUpdates(runtime: Runtime) {
        if (runtime.fusedRegistered || runtime.fusedRegistrationPending) return

        val callback = locationCallback(runtime)
        runtime.fusedCallback = callback
        runtime.fusedRegistrationPending = true
        try {
            fusedLocationClient.requestLocationUpdates(
                LOCATION_REQUEST,
                callback,
                handler.looper,
            ).addOnCompleteListener(
                mainExecutor,
                OnCompleteListener<Void> { task ->
                    if (!isActive(runtime)) return@OnCompleteListener
                    runtime.fusedRegistrationPending = false
                    if (task.isSuccessful) {
                        runtime.fusedRegistered = true
                        emitReadiness(runtime)
                    } else {
                        handleFusedFailure(runtime)
                    }
                },
            )
        } catch (_: SecurityException) {
            runtime.permission = permissionState()
            emit(WorkoutLocationEvent.Permission(runtime.generation, runtime.permission))
            handleFusedFailure(runtime)
        } catch (_: RuntimeException) {
            handleFusedFailure(runtime)
        }
    }

    private fun requestLastLocation(runtime: Runtime) {
        if (runtime.lastLocationRequested || runtime.lastLocationPending) return

        runtime.lastLocationRequested = true
        runtime.lastLocationPending = true
        try {
            fusedLocationClient.lastLocation.addOnCompleteListener(
                mainExecutor,
                OnCompleteListener<Location> { task ->
                    if (!isActive(runtime)) return@OnCompleteListener
                    runtime.lastLocationPending = false
                    if (!task.isSuccessful) {
                        runtime.lastLocationRequested = false
                        emitReadiness(runtime)
                        return@OnCompleteListener
                    }
                    val location = try {
                        task.result
                    } catch (_: RuntimeException) {
                        runtime.lastLocationRequested = false
                        null
                    }
                    if (location != null && isRecentSeed(location) &&
                        location.toCandidate(runtime.generation).isValidSeed(clock())
                    ) {
                        emitLocation(runtime, location)
                    }
                    emitReadiness(runtime)
                },
            )
        } catch (_: SecurityException) {
            runtime.lastLocationRequested = false
            runtime.lastLocationPending = false
            runtime.permission = permissionState()
            emit(WorkoutLocationEvent.Permission(runtime.generation, runtime.permission))
            emitReadiness(runtime)
        } catch (_: RuntimeException) {
            runtime.lastLocationRequested = false
            runtime.lastLocationPending = false
            emitReadiness(runtime)
        }
    }

    private fun locationCallback(runtime: Runtime) = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (!isActive(runtime)) return
            result.locations.forEach { location -> emitLocation(runtime, location) }
        }
    }

    private fun isRecentSeed(location: Location): Boolean {
        val nowElapsed = elapsedRealtimeNanos()
        val observedAt = location.elapsedRealtimeNanos
        if (observedAt > 0L && nowElapsed >= observedAt) {
            return Duration.ofNanos(nowElapsed - observedAt) <= RECENT_FIX_MAXIMUM_AGE
        }

        val sampledAt = Instant.ofEpochMilli(location.time)
        val now = clock()
        return sampledAt <= now && Duration.between(sampledAt, now) <= RECENT_FIX_MAXIMUM_AGE
    }

    private fun emitLocation(runtime: Runtime, location: Location) {
        val observedAt = location.elapsedRealtimeNanos
        val previousObservedAt = runtime.lastFixElapsedRealtimeNanos
        if (observedAt > 0L && previousObservedAt != null && previousObservedAt > 0L &&
            observedAt < previousObservedAt
        ) return

        val candidate = location.toCandidate(runtime.generation)
        if (!candidate.isValidForReadiness(clock())) {
            runtime.lastCandidate = null
            runtime.lastFixElapsedRealtimeNanos = null
            emitReadiness(runtime)
            return
        }
        runtime.lastCandidate = candidate
        if (observedAt > 0L) runtime.lastFixElapsedRealtimeNanos = observedAt
        emit(WorkoutLocationEvent.Fix(runtime.generation, candidate))
        emitReadiness(runtime)
    }

    private fun handleFusedFailure(runtime: Runtime) {
        runtime.fusedRegistrationPending = false
        runtime.fusedRegistered = false
        runtime.fusedCallback?.let { callback ->
            try {
                fusedLocationClient.removeLocationUpdates(callback)
            } catch (_: SecurityException) {
                // Revogação concorrente não impede retry no próximo tick.
            } catch (_: RuntimeException) {
                // Falha de cleanup não deve derrubar a PoC.
            }
        }
        runtime.fusedCallback = null
        runtime.service = GpsServiceState.UNAVAILABLE
        emit(WorkoutLocationEvent.Service(runtime.generation, runtime.service))
        emitReadiness(runtime)
    }

    private fun scheduleTick(runtime: Runtime) {
        val tick = object : Runnable {
            override fun run() {
                if (!isActive(runtime) || runtime.cleaned) return
                updateEnvironment(runtime, force = false)
                if (runtime.permission != GpsPermissionState.PRECISE) {
                    cleanup(runtime)
                    return
                }
                if (runtime.service == GpsServiceState.ENABLED) {
                    registerFusedUpdates(runtime)
                    requestLastLocation(runtime)
                }
                handler.postDelayed(this, READINESS_INTERVAL_MILLIS)
            }
        }
        runtime.tick = tick
        handler.postDelayed(tick, READINESS_INTERVAL_MILLIS)
    }

    private fun updateEnvironment(runtime: Runtime, force: Boolean) {
        if (!isActive(runtime)) return
        val permission = permissionState()
        val service = serviceState()
        if (force || permission != runtime.permission) {
            runtime.permission = permission
            emit(WorkoutLocationEvent.Permission(runtime.generation, permission))
        }
        if (force || service != runtime.service) {
            runtime.service = service
            emit(WorkoutLocationEvent.Service(runtime.generation, service))
        }
        emitReadiness(runtime)
    }

    private fun emitReadiness(runtime: Runtime) {
        if (!isActive(runtime)) return
        val candidate = runtime.lastCandidate
        val readiness = GpsReadinessEvaluator.evaluate(
            GpsReadinessInput(
                permission = runtime.permission,
                services = runtime.service,
                fixObservedAt = candidate?.sampledAt,
                evaluatedAt = clock(),
                horizontalAccuracyMeters = candidate?.horizontalAccuracyMeters,
                isMock = candidate?.isMock == true &&
                    !GpsMapPocMockGeometryDebugMode.acceptsMockLocations,
                wasPreviouslyReady = runtime.wasPreviouslyReady,
                gnss = null,
                fixAge = runtime.fixAgeAt(elapsedRealtimeNanos()),
            ),
        )
        runtime.wasPreviouslyReady = runtime.wasPreviouslyReady || readiness.canStart
        emit(WorkoutLocationEvent.Readiness(runtime.generation, readiness))
    }

    private fun cleanup(runtime: Runtime?) {
        if (runtime == null || runtime.cleaned) return
        runtime.cleaned = true
        runtime.tick?.let(handler::removeCallbacks)
        runtime.fusedCallback?.let { callback ->
            try {
                fusedLocationClient.removeLocationUpdates(callback)
            } catch (_: SecurityException) {
                // Permission revogada não impede o restante do cleanup.
            } catch (_: RuntimeException) {
                // Falha de cleanup não deve derrubar a PoC.
            }
        }
        runtime.fusedCallback = null
        runtime.fusedRegistered = false
        runtime.fusedRegistrationPending = false
    }

    private fun permissionState(): GpsPermissionState {
        val fine = applicationContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (fine) return GpsPermissionState.PRECISE
        val coarse = applicationContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return if (coarse) GpsPermissionState.APPROXIMATE else GpsPermissionState.DENIED
    }

    private fun serviceState(): GpsServiceState = try {
        if (locationManager.isLocationEnabled) {
            GpsServiceState.ENABLED
        } else {
            GpsServiceState.DISABLED
        }
    } catch (_: SecurityException) {
        GpsServiceState.UNAVAILABLE
    } catch (_: RuntimeException) {
        GpsServiceState.UNAVAILABLE
    }

    private fun isActive(runtime: Runtime): Boolean = synchronized(lock) {
        !isClosed && activeRuntime === runtime && !runtime.cleaned
    }

    private fun emit(event: WorkoutLocationEvent) {
        mutableEvents.tryEmit(event)
    }

    private fun Runtime.fixAgeAt(nowElapsedRealtimeNanos: Long): Duration? {
        val observed = lastFixElapsedRealtimeNanos ?: return null
        if (observed <= 0L || nowElapsedRealtimeNanos < observed) return null
        return Duration.ofNanos(nowElapsedRealtimeNanos - observed)
    }

    private fun Location.toCandidate(runtimeGeneration: Long): WorkoutRoutePointCandidate =
        WorkoutRoutePointCandidate(
            sampledAt = Instant.ofEpochMilli(time),
            latitudeDegrees = latitude,
            longitudeDegrees = longitude,
            altitudeMeters = altitude.takeIf { hasAltitude() },
            horizontalAccuracyMeters = accuracy.toDouble(),
            verticalAccuracyMeters = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasVerticalAccuracy()) {
                verticalAccuracyMeters.toDouble()
            } else null,
            speedMetersPerSecond = speed.toDouble().takeIf { hasSpeed() },
            bearingDegrees = bearing.toDouble().takeIf { hasBearing() },
            isMock = isMockLocation(),
            runtimeGeneration = runtimeGeneration,
        )

    private fun WorkoutRoutePointCandidate.isValidForReadiness(now: Instant): Boolean =
        sampledAt <= now && latitudeDegrees.isFinite() && longitudeDegrees.isFinite() &&
            latitudeDegrees in -90.0..90.0 && longitudeDegrees in -180.0..180.0 &&
            horizontalAccuracyMeters.isFinite() && horizontalAccuracyMeters > 0.0

    private fun WorkoutRoutePointCandidate.isValidSeed(now: Instant): Boolean =
        isValidForReadiness(now) &&
            (!isMock || GpsMapPocMockGeometryDebugMode.acceptsMockLocations) &&
            horizontalAccuracyMeters <= GpsReadinessPolicy().weakMaximumAccuracyMeters

    @Suppress("DEPRECATION")
    private fun Location.isMockLocation(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        isMock
    } else {
        isFromMockProvider
    }

    private class Runtime(val generation: Long) {
        var permission = GpsPermissionState.DENIED
        var service = GpsServiceState.UNAVAILABLE
        var lastCandidate: WorkoutRoutePointCandidate? = null
        var lastFixElapsedRealtimeNanos: Long? = null
        var wasPreviouslyReady = false
        var fusedCallback: LocationCallback? = null
        var tick: Runnable? = null
        var fusedRegistered = false
        var fusedRegistrationPending = false
        var lastLocationRequested = false
        var lastLocationPending = false
        var cleaned = false
    }

    private companion object {
        val LOCATION_REQUEST: LocationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_INTERVAL_MILLIS,
        ).setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MILLIS)
            .setMinUpdateDistanceMeters(MINIMUM_DISTANCE_METERS)
            .build()
        const val LOCATION_INTERVAL_MILLIS = 1_000L
        const val MIN_UPDATE_INTERVAL_MILLIS = 1_000L
        const val READINESS_INTERVAL_MILLIS = 1_000L
        const val MINIMUM_DISTANCE_METERS = 0f
        const val EVENT_BUFFER_CAPACITY = 64
        val RECENT_FIX_MAXIMUM_AGE: Duration = GpsReadinessPolicy().recentFixMaximumAge
    }
}
