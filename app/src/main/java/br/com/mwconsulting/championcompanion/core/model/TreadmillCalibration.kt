package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant

enum class TreadmillCalibrationOrigin {
    USER_REPORTED,
}

sealed interface TreadmillCalibrationSubject {
    data class Activity(val id: WorkoutActivityId) : TreadmillCalibrationSubject

    data class Session(val id: AppWorkoutSessionId) : TreadmillCalibrationSubject
}

data class TreadmillCalibrationKey(
    val subject: TreadmillCalibrationSubject,
    val revision: Int,
)

data class CalibratedWorkoutMetrics(
    val averagePaceSecondsPerKilometer: DomainValue<Double>,
    val averageSpeedMetersPerSecond: DomainValue<Double>,
    val provenance: MetricProvenance = MetricProvenance.CALIBRATED,
)

/** Immutable audit record. Source distances remain untouched on WorkoutActivity. */
@ConsistentCopyVisibility
data class TreadmillCalibration private constructor(
    val id: TreadmillCalibrationId,
    val key: TreadmillCalibrationKey,
    val subject: TreadmillCalibrationSubject,
    val originalMeters: DomainValue<Double>,
    val estimatedMeters: DomainValue<Double>,
    val reportedMeters: DomainValue<Double>,
    val calibratedMeters: DomainValue<Double>,
    val factor: DomainValue<Double>,
    val appliedAt: Instant,
    val origin: TreadmillCalibrationOrigin,
    val derivedMetrics: CalibratedWorkoutMetrics,
) {
    companion object {
        fun forImportedActivity(
            id: TreadmillCalibrationId,
            activity: WorkoutActivity,
            revision: Int,
            reportedMeters: Double,
            appliedAt: Instant,
        ): DomainResult<TreadmillCalibration> {
            if (revision < 1 || !reportedMeters.isFinite() ||
                reportedMeters <= 0.0 || activity.confirmedType != WorkoutType.TREADMILL ||
                activity.reviewedAt == null || appliedAt.isBefore(activity.reviewedAt)
            ) {
                return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }

            val factor = when (val base = resolveFactorBase(activity)) {
                is DomainValue.Present -> DomainValue.Present(reportedMeters / base.value)
                is DomainValue.Absent -> DomainValue.Absent(base.reason)
            }

            return DomainResult.Success(
                TreadmillCalibration(
                    id = id,
                    key = TreadmillCalibrationKey(TreadmillCalibrationSubject.Activity(activity.id), revision),
                    subject = TreadmillCalibrationSubject.Activity(activity.id),
                    originalMeters = activity.metrics.distances.originalMeters,
                    estimatedMeters = activity.metrics.distances.estimatedMeters,
                    reportedMeters = DomainValue.Present(reportedMeters),
                    calibratedMeters = DomainValue.Present(reportedMeters),
                    factor = factor,
                    appliedAt = appliedAt,
                    origin = TreadmillCalibrationOrigin.USER_REPORTED,
                    derivedMetrics = deriveMetrics(activity.metrics.duration, reportedMeters),
                ),
            )
        }

        fun forSessionWithoutPositiveEstimate(
            id: TreadmillCalibrationId,
            sessionId: AppWorkoutSessionId,
            revision: Int,
            calibratedMeters: Double,
            appliedAt: Instant,
            duration: Duration,
        ): DomainResult<TreadmillCalibration> {
            if (revision < 1 || !calibratedMeters.isFinite() || calibratedMeters <= 0.0 ||
                duration.isNegative
            ) {
                return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
            val subject = TreadmillCalibrationSubject.Session(sessionId)
            return DomainResult.Success(
                TreadmillCalibration(
                    id = id,
                    key = TreadmillCalibrationKey(subject, revision),
                    subject = subject,
                    originalMeters = DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE),
                    estimatedMeters = DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE),
                    reportedMeters = DomainValue.Present(calibratedMeters),
                    calibratedMeters = DomainValue.Present(calibratedMeters),
                    factor = DomainValue.Absent(AbsenceReason.NOT_APPLICABLE),
                    appliedAt = appliedAt,
                    origin = TreadmillCalibrationOrigin.USER_REPORTED,
                    derivedMetrics = deriveMetrics(duration, calibratedMeters),
                ),
            )
        }

        private fun deriveMetrics(duration: Duration, meters: Double): CalibratedWorkoutMetrics {
            if (duration.isZero || meters <= 0.0) {
                return CalibratedWorkoutMetrics(
                    averagePaceSecondsPerKilometer = DomainValue.Absent(AbsenceReason.NOT_APPLICABLE),
                    averageSpeedMetersPerSecond = DomainValue.Absent(AbsenceReason.NOT_APPLICABLE),
                )
            }
            val seconds = duration.toNanos().toDouble() / NANOS_PER_SECOND
            return CalibratedWorkoutMetrics(
                averagePaceSecondsPerKilometer = DomainValue.Present(seconds * METERS_PER_KILOMETER / meters),
                averageSpeedMetersPerSecond = DomainValue.Present(meters / seconds),
            )
        }

        private fun resolveFactorBase(activity: WorkoutActivity): DomainValue<Double> {
            val original = activity.metrics.distances.originalMeters as? DomainValue.Present
            if (original != null && original.value > 0.0) return original

            val estimated = activity.metrics.distances.estimatedMeters as? DomainValue.Present
            if (estimated != null && estimated.value > 0.0) return estimated

            return DomainValue.Absent(AbsenceReason.NOT_APPLICABLE)
        }

        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private const val METERS_PER_KILOMETER = 1_000.0
    }
}
