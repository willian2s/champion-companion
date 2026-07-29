package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant
import java.util.UUID

@JvmInline
value class WorkoutRouteId(val value: UUID)

@JvmInline
value class WorkoutRoutePointId(val value: UUID)

enum class WorkoutRouteState {
    CAPTURING,
    FINALIZED,
}

enum class WorkoutRouteSegmentReason {
    SESSION_START,
    RESUMED_AFTER_PAUSE,
    PROLONGED_GAP,
    GPS_RECOVERY,
    PROCESS_RECOVERY,
}

data class WorkoutRouteMetrics(
    val acceptedPointCount: Long,
    val segmentCount: Int,
    val distanceMeters: Double,
    val averageHorizontalAccuracyMeters: Double?,
) {
    init {
        require(acceptedPointCount >= 0)
        require(segmentCount >= 0)
        require(distanceMeters.isFinite() && distanceMeters >= 0.0)
        require(averageHorizontalAccuracyMeters == null ||
            averageHorizontalAccuracyMeters.isFinite() && averageHorizontalAccuracyMeters > 0.0)
    }

    companion object {
        val EMPTY = WorkoutRouteMetrics(0, 0, 0.0, null)
    }
}

data class WorkoutRouteSegment(
    val routeId: WorkoutRouteId,
    val index: Int,
    val reason: WorkoutRouteSegmentReason,
    val firstSequence: Long,
    val lastSequence: Long,
    val startedAt: Instant,
    val endedAt: Instant,
    val distanceMeters: Double,
) {
    init {
        require(index >= 0)
        require(firstSequence >= 0 && lastSequence >= firstSequence)
        require(!endedAt.isBefore(startedAt))
        require(distanceMeters.isFinite() && distanceMeters >= 0.0)
    }
}

@ConsistentCopyVisibility
data class WorkoutRoutePoint private constructor(
    val id: WorkoutRoutePointId,
    val routeId: WorkoutRouteId,
    val sequence: Long,
    val sampledAt: Instant,
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val altitudeMeters: Double?,
    val horizontalAccuracyMeters: Double,
    val verticalAccuracyMeters: Double?,
    val speedMetersPerSecond: Double?,
    val bearingDegrees: Double?,
    val segmentIndex: Int,
) {
    companion object {
        @Suppress("LongParameterList")
        fun create(
            id: WorkoutRoutePointId,
            routeId: WorkoutRouteId,
            sequence: Long,
            sampledAt: Instant,
            latitudeDegrees: Double,
            longitudeDegrees: Double,
            altitudeMeters: Double?,
            horizontalAccuracyMeters: Double,
            verticalAccuracyMeters: Double?,
            speedMetersPerSecond: Double?,
            bearingDegrees: Double?,
            segmentIndex: Int,
        ): DomainResult<WorkoutRoutePoint> = if (
            sequence < 0 || segmentIndex < 0 || latitudeDegrees !in -90.0..90.0 ||
            longitudeDegrees !in -180.0..180.0 || !latitudeDegrees.isFinite() ||
            !longitudeDegrees.isFinite() || !horizontalAccuracyMeters.isFinite() ||
            horizontalAccuracyMeters <= 0.0 || !isOptionalFinite(altitudeMeters) ||
            !isOptionalPositive(verticalAccuracyMeters) || !isOptionalNonNegative(speedMetersPerSecond) ||
            (bearingDegrees != null && (!bearingDegrees.isFinite() || bearingDegrees < 0.0 || bearingDegrees >= 360.0))
        ) {
            DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        } else {
            DomainResult.Success(WorkoutRoutePoint(
                id, routeId, sequence, sampledAt, latitudeDegrees, longitudeDegrees,
                altitudeMeters, horizontalAccuracyMeters, verticalAccuracyMeters,
                speedMetersPerSecond, bearingDegrees, segmentIndex,
            ))
        }

        private fun isOptionalFinite(value: Double?): Boolean = value == null || value.isFinite()
        private fun isOptionalPositive(value: Double?): Boolean = value == null || value.isFinite() && value > 0.0
        private fun isOptionalNonNegative(value: Double?): Boolean = value == null || value.isFinite() && value >= 0.0
    }
}

@ConsistentCopyVisibility
data class WorkoutRoute private constructor(
    val id: WorkoutRouteId,
    val sessionId: AppWorkoutSessionId,
    val workoutType: WorkoutType,
    val state: WorkoutRouteState,
    val startedAt: Instant,
    val endedAt: Instant?,
    val metrics: WorkoutRouteMetrics,
    /** Revisão cresce em todo lote aceito ou consolidação. */
    val revision: Long,
) {
    fun applyCheckpoint(
        expectedRevision: Long,
        nextMetrics: WorkoutRouteMetrics,
    ): DomainResult<WorkoutRoute> = if (
        state != WorkoutRouteState.CAPTURING || expectedRevision != revision ||
        nextMetrics.acceptedPointCount < metrics.acceptedPointCount ||
        nextMetrics.segmentCount < metrics.segmentCount ||
        nextMetrics.distanceMeters < metrics.distanceMeters ||
        nextMetrics.segmentCount > nextMetrics.acceptedPointCount
    ) {
        DomainResult.Failure(DomainValidationError.INVALID_STATE)
    } else {
        DomainResult.Success(copy(metrics = nextMetrics, revision = revision + 1))
    }

    fun finalize(expectedRevision: Long, at: Instant): DomainResult<WorkoutRoute> = when {
        state == WorkoutRouteState.FINALIZED && endedAt == at -> DomainResult.Success(this)
        state != WorkoutRouteState.CAPTURING || expectedRevision != revision || at.isBefore(startedAt) ->
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        else -> DomainResult.Success(copy(
            state = WorkoutRouteState.FINALIZED,
            endedAt = at,
            revision = revision + 1,
        ))
    }

    companion object {
        fun create(
            id: WorkoutRouteId,
            sessionId: AppWorkoutSessionId,
            workoutType: WorkoutType,
            startedAt: Instant,
        ): DomainResult<WorkoutRoute> = if (!workoutType.supportsRoute()) {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        } else {
            DomainResult.Success(WorkoutRoute(
                id = id,
                sessionId = sessionId,
                workoutType = workoutType,
                state = WorkoutRouteState.CAPTURING,
                startedAt = startedAt,
                endedAt = null,
                metrics = WorkoutRouteMetrics.EMPTY,
                revision = 0,
            ))
        }

        fun restore(
            id: WorkoutRouteId,
            sessionId: AppWorkoutSessionId,
            workoutType: WorkoutType,
            state: WorkoutRouteState,
            startedAt: Instant,
            endedAt: Instant?,
            metrics: WorkoutRouteMetrics,
            revision: Long,
        ): DomainResult<WorkoutRoute> = if (
            !workoutType.supportsRoute() || revision < 0 ||
            (endedAt != null && endedAt.isBefore(startedAt)) ||
            (state == WorkoutRouteState.CAPTURING && endedAt != null) ||
            (state == WorkoutRouteState.FINALIZED && endedAt == null) ||
            metrics.segmentCount > metrics.acceptedPointCount
        ) {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        } else {
            DomainResult.Success(WorkoutRoute(id, sessionId, workoutType, state, startedAt, endedAt, metrics, revision))
        }

        private fun WorkoutType.supportsRoute(): Boolean = this == WorkoutType.WALKING || this == WorkoutType.RUNNING
    }
}

enum class GpsPermissionState {
    NOT_REQUESTED,
    DENIED,
    APPROXIMATE,
    PRECISE,
}

enum class GpsServiceState {
    ENABLED,
    DISABLED,
    UNAVAILABLE,
}

enum class GpsReadinessState {
    PERMISSION_REQUIRED,
    PRECISE_PERMISSION_REQUIRED,
    LOCATION_DISABLED,
    SEARCHING,
    READY,
    LOST,
    UNAVAILABLE,
}

enum class GpsAccuracyQuality {
    UNKNOWN,
    UNUSABLE,
    WEAK,
    GOOD,
    STRONG,
}

data class GnssQualitySupplement(
    val visibleSatelliteCount: Int,
    val usedInFixSatelliteCount: Int,
    /** C/N0 é complemento informativo; ausência não impede readiness. */
    val averageCn0DbHz: Double?,
) {
    init {
        require(visibleSatelliteCount >= 0)
        require(usedInFixSatelliteCount in 0..visibleSatelliteCount)
        require(averageCn0DbHz == null || averageCn0DbHz.isFinite() && averageCn0DbHz >= 0.0)
    }
}

data class GpsReadinessInput(
    val permission: GpsPermissionState,
    val services: GpsServiceState,
    val fixObservedAt: Instant?,
    val evaluatedAt: Instant,
    val horizontalAccuracyMeters: Double?,
    val isMock: Boolean,
    val wasPreviouslyReady: Boolean,
    val gnss: GnssQualitySupplement? = null,
)

@ConsistentCopyVisibility
data class GpsReadiness private constructor(
    val state: GpsReadinessState,
    val accuracyQuality: GpsAccuracyQuality,
    val fixAge: Duration?,
    val gnss: GnssQualitySupplement?,
) {
    val canStart: Boolean
        get() = state == GpsReadinessState.READY &&
            accuracyQuality in setOf(GpsAccuracyQuality.GOOD, GpsAccuracyQuality.STRONG)

    companion object {
        fun evaluate(
            input: GpsReadinessInput,
            policy: GpsReadinessPolicy = GpsReadinessPolicy(),
        ): GpsReadiness {
            if (input.permission == GpsPermissionState.APPROXIMATE) {
                return safe(GpsReadinessState.PRECISE_PERMISSION_REQUIRED, input = input)
            }
            if (input.permission != GpsPermissionState.PRECISE) {
                return safe(GpsReadinessState.PERMISSION_REQUIRED, input = input)
            }
            if (input.services == GpsServiceState.UNAVAILABLE) {
                return safe(GpsReadinessState.UNAVAILABLE, input = input)
            }
            if (input.services == GpsServiceState.DISABLED) {
                return safe(GpsReadinessState.LOCATION_DISABLED, input = input)
            }

            val observedAt = input.fixObservedAt
            if (observedAt == null) {
                return if (input.horizontalAccuracyMeters != null || input.isMock) {
                    safe(GpsReadinessState.UNAVAILABLE, GpsAccuracyQuality.UNUSABLE, input = input)
                } else {
                    safe(
                        if (input.wasPreviouslyReady) GpsReadinessState.LOST else GpsReadinessState.SEARCHING,
                        input = input,
                    )
                }
            }
            if (observedAt.isAfter(input.evaluatedAt)) {
                return safe(GpsReadinessState.UNAVAILABLE, GpsAccuracyQuality.UNUSABLE, input = input)
            }

            val age = Duration.between(observedAt, input.evaluatedAt)
            val accuracy = input.horizontalAccuracyMeters
            if (input.isMock || accuracy == null || !accuracy.isFinite() || accuracy <= 0.0) {
                return safe(
                    GpsReadinessState.UNAVAILABLE,
                    GpsAccuracyQuality.UNUSABLE,
                    age,
                    input,
                )
            }
            val quality = when {
                accuracy <= policy.strongMaximumAccuracyMeters -> GpsAccuracyQuality.STRONG
                accuracy <= policy.goodMaximumAccuracyMeters -> GpsAccuracyQuality.GOOD
                accuracy <= policy.weakMaximumAccuracyMeters -> GpsAccuracyQuality.WEAK
                else -> GpsAccuracyQuality.UNUSABLE
            }
            if (age > policy.recentFixMaximumAge || quality == GpsAccuracyQuality.UNUSABLE) {
                return safe(
                    if (input.wasPreviouslyReady) GpsReadinessState.LOST else GpsReadinessState.SEARCHING,
                    quality,
                    age,
                    input,
                )
            }
            return if (quality in setOf(GpsAccuracyQuality.GOOD, GpsAccuracyQuality.STRONG)) {
                safe(GpsReadinessState.READY, quality, age, input)
            } else {
                safe(GpsReadinessState.SEARCHING, quality, age, input)
            }
        }

        private fun safe(
            state: GpsReadinessState,
            quality: GpsAccuracyQuality = GpsAccuracyQuality.UNKNOWN,
            age: Duration? = null,
            input: GpsReadinessInput,
        ) = GpsReadiness(state, quality, age, input.gnss)
    }
}

data class GpsReadinessPolicy(
    val recentFixMaximumAge: Duration = Duration.ofSeconds(10),
    val strongMaximumAccuracyMeters: Double = 10.0,
    val goodMaximumAccuracyMeters: Double = 20.0,
    val weakMaximumAccuracyMeters: Double = 50.0,
) {
    init {
        require(!recentFixMaximumAge.isNegative && !recentFixMaximumAge.isZero)
        require(strongMaximumAccuracyMeters.isFinite() && strongMaximumAccuracyMeters > 0.0)
        require(goodMaximumAccuracyMeters.isFinite() && goodMaximumAccuracyMeters > strongMaximumAccuracyMeters)
        require(weakMaximumAccuracyMeters.isFinite() && weakMaximumAccuracyMeters > goodMaximumAccuracyMeters)
    }
}

data class WorkoutRoutePointCandidate(
    val sampledAt: Instant,
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val altitudeMeters: Double?,
    val horizontalAccuracyMeters: Double,
    val verticalAccuracyMeters: Double?,
    val speedMetersPerSecond: Double?,
    val bearingDegrees: Double?,
    val isMock: Boolean,
    val runtimeGeneration: Long,
)

enum class WorkoutRoutePointRejectionReason {
    LATE_RUNTIME_GENERATION,
    INVALID_COORDINATE,
    INVALID_MEASUREMENT,
    ROUTE_MISMATCH,
    OUT_OF_ORDER,
    STALE,
    INSUFFICIENT_ACCURACY,
    IMPOSSIBLE_HUMAN_SPEED,
    DUPLICATE,
    BELOW_NOISE_FLOOR,
    MOCK,
}

data class WorkoutRoutePointFilterPolicy(
    val maximumAge: Duration = Duration.ofSeconds(10),
    val maximumHorizontalAccuracyMeters: Double = 50.0,
    val maximumHumanSpeedMetersPerSecond: Double = 12.0,
    val minimumDisplacementMeters: Double = 3.0,
) {
    init {
        require(!maximumAge.isNegative && !maximumAge.isZero)
        require(maximumHorizontalAccuracyMeters.isFinite() && maximumHorizontalAccuracyMeters > 0.0)
        require(maximumHumanSpeedMetersPerSecond.isFinite() && maximumHumanSpeedMetersPerSecond > 0.0)
        require(minimumDisplacementMeters.isFinite() && minimumDisplacementMeters >= 0.0)
    }
}

data class WorkoutRoutePointFilterInput(
    val pointId: WorkoutRoutePointId,
    val routeId: WorkoutRouteId,
    val sequence: Long,
    val segmentIndex: Int,
    val observedAt: Instant,
    val expectedRuntimeGeneration: Long,
    val candidate: WorkoutRoutePointCandidate,
    val previousAcceptedPoint: WorkoutRoutePoint?,
    val policy: WorkoutRoutePointFilterPolicy = WorkoutRoutePointFilterPolicy(),
)

sealed interface WorkoutRoutePointFilterResult {
    data class Accepted(
        val point: WorkoutRoutePoint,
        val distanceFromPreviousMeters: Double,
    ) : WorkoutRoutePointFilterResult

    data class Rejected(val reason: WorkoutRoutePointRejectionReason) : WorkoutRoutePointFilterResult
}
