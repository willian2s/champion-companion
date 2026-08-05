package br.com.mwconsulting.championcompanion.core.location

import br.com.mwconsulting.championcompanion.core.model.DomainResult
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePoint
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointFilterInput
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointFilterResult
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointRejectionReason
import java.time.Duration
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object RoutePointFilter {
    fun filter(input: WorkoutRoutePointFilterInput): WorkoutRoutePointFilterResult {
        val candidate = input.candidate
        val previous = input.previousAcceptedPoint
        if (candidate.runtimeGeneration != input.expectedRuntimeGeneration) {
            return rejected(WorkoutRoutePointRejectionReason.LATE_RUNTIME_GENERATION)
        }
        if (previous != null && previous.routeId != input.routeId) {
            return rejected(WorkoutRoutePointRejectionReason.ROUTE_MISMATCH)
        }
        if (!hasValidCoordinates(candidate.latitudeDegrees, candidate.longitudeDegrees)) {
            return rejected(WorkoutRoutePointRejectionReason.INVALID_COORDINATE)
        }
        if (!hasValidMeasurements(input)) {
            return rejected(WorkoutRoutePointRejectionReason.INVALID_MEASUREMENT)
        }
        if (candidate.sampledAt.isAfter(input.observedAt) ||
            input.sequence < 0 || input.segmentIndex < 0 ||
            previous == null && (input.sequence != 0L || input.segmentIndex != 0) ||
            previous != null && (
                input.sequence != previous.sequence + 1L ||
                    !candidate.sampledAt.isAfter(previous.sampledAt) ||
                    input.segmentIndex !in previous.segmentIndex..(previous.segmentIndex + 1)
                )
        ) {
            return rejected(WorkoutRoutePointRejectionReason.OUT_OF_ORDER)
        }
        if (Duration.between(candidate.sampledAt, input.observedAt) > input.policy.maximumAge) {
            return rejected(WorkoutRoutePointRejectionReason.STALE)
        }
        if (candidate.horizontalAccuracyMeters > input.policy.maximumHorizontalAccuracyMeters) {
            return rejected(WorkoutRoutePointRejectionReason.INSUFFICIENT_ACCURACY)
        }
        if (candidate.isMock) {
            return rejected(WorkoutRoutePointRejectionReason.MOCK)
        }
        if (candidate.speedMetersPerSecond?.let { it > input.policy.maximumHumanSpeedMetersPerSecond } == true) {
            return rejected(WorkoutRoutePointRejectionReason.IMPOSSIBLE_HUMAN_SPEED)
        }

        val distanceFromPreviousMeters = if (previous != null) {
            haversineMeters(
                previous.latitudeDegrees,
                previous.longitudeDegrees,
                candidate.latitudeDegrees,
                candidate.longitudeDegrees,
            )
        } else {
            0.0
        }
        if (previous != null) {
            val elapsed = Duration.between(previous.sampledAt, candidate.sampledAt)
            val elapsedSeconds = elapsed.seconds.toDouble() + elapsed.nano / NANOS_PER_SECOND
            if (distanceFromPreviousMeters / elapsedSeconds > input.policy.maximumHumanSpeedMetersPerSecond) {
                return rejected(WorkoutRoutePointRejectionReason.IMPOSSIBLE_HUMAN_SPEED)
            }
        }

        val connectsToPrevious = previous != null && previous.segmentIndex == input.segmentIndex
        if (connectsToPrevious) {
            if (candidate.latitudeDegrees == previous.latitudeDegrees &&
                candidate.longitudeDegrees == previous.longitudeDegrees
            ) {
                return rejected(WorkoutRoutePointRejectionReason.DUPLICATE)
            }
            if (distanceFromPreviousMeters < input.policy.minimumDisplacementMeters) {
                return rejected(WorkoutRoutePointRejectionReason.BELOW_NOISE_FLOOR)
            }
        }

        val point = when (val result = WorkoutRoutePoint.create(
            id = input.pointId,
            routeId = input.routeId,
            sequence = input.sequence,
            sampledAt = candidate.sampledAt,
            latitudeDegrees = candidate.latitudeDegrees,
            longitudeDegrees = candidate.longitudeDegrees,
            altitudeMeters = candidate.altitudeMeters,
            horizontalAccuracyMeters = candidate.horizontalAccuracyMeters,
            verticalAccuracyMeters = candidate.verticalAccuracyMeters,
            speedMetersPerSecond = candidate.speedMetersPerSecond,
            bearingDegrees = candidate.bearingDegrees,
            segmentIndex = input.segmentIndex,
        )) {
            is DomainResult.Success -> result.value
            is DomainResult.Failure -> return rejected(WorkoutRoutePointRejectionReason.INVALID_MEASUREMENT)
        }
        val accumulatedDistanceMeters = distanceFromPreviousMeters.takeIf { connectsToPrevious } ?: 0.0
        return WorkoutRoutePointFilterResult.Accepted(point, accumulatedDistanceMeters)
    }

    private fun hasValidCoordinates(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0

    private fun hasValidMeasurements(input: WorkoutRoutePointFilterInput): Boolean {
        val candidate = input.candidate
        return candidate.horizontalAccuracyMeters.isFinite() && candidate.horizontalAccuracyMeters > 0.0 &&
            candidate.sampledAt.isExactlyRepresentableAsEpochMillis() &&
            candidate.altitudeMeters.isNullOrFinite() && candidate.verticalAccuracyMeters.isNullOrPositive() &&
            candidate.speedMetersPerSecond.isNullOrNonNegative() &&
            (candidate.bearingDegrees == null ||
                candidate.bearingDegrees.isFinite() && candidate.bearingDegrees in 0.0..<360.0)
    }

    private fun Double?.isNullOrFinite(): Boolean = this == null || isFinite()

    private fun Double?.isNullOrPositive(): Boolean = this == null || isFinite() && this > 0.0

    private fun Double?.isNullOrNonNegative(): Boolean = this == null || isFinite() && this >= 0.0

    private fun java.time.Instant.isExactlyRepresentableAsEpochMillis(): Boolean = try {
        java.time.Instant.ofEpochMilli(toEpochMilli()) == this
    } catch (_: ArithmeticException) {
        false
    }

    private fun rejected(reason: WorkoutRoutePointRejectionReason) = WorkoutRoutePointFilterResult.Rejected(reason)

    private fun haversineMeters(
        firstLatitude: Double,
        firstLongitude: Double,
        secondLatitude: Double,
        secondLongitude: Double,
    ): Double {
        val firstLatitudeRadians = Math.toRadians(firstLatitude)
        val secondLatitudeRadians = Math.toRadians(secondLatitude)
        val latitudeDelta = secondLatitudeRadians - firstLatitudeRadians
        val longitudeDelta = Math.toRadians(secondLongitude - firstLongitude)
        val haversine = sin(latitudeDelta / 2.0).let { it * it } +
            cos(firstLatitudeRadians) * cos(secondLatitudeRadians) *
            sin(longitudeDelta / 2.0).let { it * it }
        val angularDistance = 2.0 * atan2(sqrt(haversine.coerceIn(0.0, 1.0)), sqrt((1.0 - haversine).coerceAtLeast(0.0)))
        return EARTH_MEAN_RADIUS_METERS * angularDistance
    }

    private const val EARTH_MEAN_RADIUS_METERS = 6_371_008.8
    private const val NANOS_PER_SECOND = 1_000_000_000.0
}
