package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant

data class RunningPerformanceWorkoutInput(
    val activityId: WorkoutActivityId,
    val startedAt: Instant,
    val activityDuration: Duration,
    val hasActiveSnapshot: Boolean,
    val canonicalDistanceMeters: Double?,
    val canonicalCoveredDurationNanos: Long?,
    val derivationMethod: WorkoutDerivationMethod?,
    val derivationVersion: String?,
    val distanceCalibrationFactor: Double?,
    val temporalEligibility: TemporalSeriesEligibility?,
    val temporalEligibilityReasons: Set<TemporalSeriesValidationReason> = emptySet(),
    val plausibilityIndicators: Set<WorkoutPlausibilityIndicator> = emptySet(),
    val heartRateSamples: List<WorkoutHeartRateSample> = emptyList(),
    val heartRateSourceAvailable: Boolean = true,
)

enum class RunningPerformanceHistoryStatus {
    AVAILABLE,
    NO_RUNS,
    NO_COMPARABLE_RUNS,
    UNAVAILABLE,
}

enum class RunningPerformancePaceStatus {
    COMPARABLE_NATIVE,
    AVAILABLE_FALLBACK_NOT_COMPARABLE,
    AVAILABLE_UNSUPPORTED_VERSION_NOT_COMPARABLE,
    NO_ACTIVE_SNAPSHOT,
    TEMPORAL_INELIGIBLE,
    PLAUSIBILITY_BLOCKED,
    INVALID_CANONICAL_FACTS,
}

enum class RunningPerformanceHeartRateStatus {
    AVAILABLE,
    NO_HEART_RATE_SAMPLES,
    INVALID_SAMPLES,
    SOURCE_UNAVAILABLE,
}

enum class RunningPerformanceDistanceProvenance {
    WATCH_NATIVE,
    WATCH_NATIVE_CONTEXT_CALIBRATED,
    STEP_LENGTH_FALLBACK,
    FIXED_PERSONAL_STEP_LENGTH,
}

data class RunningPerformancePoint(
    val startedAt: Instant,
    val canonicalDistanceMeters: Double?,
    val canonicalCoveredDuration: Duration?,
    val paceSecondsPerKilometer: Double?,
    val averageHeartRateBpm: Double?,
    val heartRateCoverage: Double?,
    val paceStatus: RunningPerformancePaceStatus,
    val heartRateStatus: RunningPerformanceHeartRateStatus,
    val derivationVersion: String?,
    val distanceProvenance: RunningPerformanceDistanceProvenance?,
    val partial: Boolean,
)

data class RunningPerformanceHistory(
    val status: RunningPerformanceHistoryStatus,
    val points: List<RunningPerformancePoint>,
)

object RunningPerformanceHistoryPolicy {
    fun evaluate(inputs: List<RunningPerformanceWorkoutInput>): RunningPerformanceHistory {
        if (inputs.isEmpty()) {
            return RunningPerformanceHistory(
                status = RunningPerformanceHistoryStatus.NO_RUNS,
                points = emptyList(),
            )
        }

        val points = inputs
            .sortedWith(compareBy<RunningPerformanceWorkoutInput>({ it.startedAt }, { it.activityId.value.toString() }))
            .map(::evaluatePoint)
        val hasUnavailableHeartRateSource = points.any { point ->
            point.heartRateStatus == RunningPerformanceHeartRateStatus.SOURCE_UNAVAILABLE
        }
        val hasComparableNativePace = points.any { point ->
            point.paceStatus == RunningPerformancePaceStatus.COMPARABLE_NATIVE
        }
        val status = when {
            hasUnavailableHeartRateSource -> RunningPerformanceHistoryStatus.UNAVAILABLE
            hasComparableNativePace -> RunningPerformanceHistoryStatus.AVAILABLE
            else -> RunningPerformanceHistoryStatus.NO_COMPARABLE_RUNS
        }
        return RunningPerformanceHistory(status = status, points = points)
    }

    private fun evaluatePoint(input: RunningPerformanceWorkoutInput): RunningPerformancePoint {
        val pace = evaluatePace(input)
        val heartRate = evaluateHeartRate(input)
        return RunningPerformancePoint(
            startedAt = input.startedAt,
            canonicalDistanceMeters = pace.distanceMeters,
            canonicalCoveredDuration = pace.coveredDuration,
            paceSecondsPerKilometer = pace.paceSecondsPerKilometer,
            averageHeartRateBpm = heartRate.averageBpm,
            heartRateCoverage = heartRate.coverage,
            paceStatus = pace.status,
            heartRateStatus = heartRate.status,
            derivationVersion = input.derivationVersion,
            distanceProvenance = input.derivationMethod.toProvenance(),
            partial = heartRate.averageBpm == null || heartRate.coverage != 1.0,
        )
    }

    private fun evaluateHeartRate(input: RunningPerformanceWorkoutInput): HeartRateEvaluation {
        if (!input.heartRateSourceAvailable) {
            return HeartRateEvaluation(
                status = RunningPerformanceHeartRateStatus.SOURCE_UNAVAILABLE,
                averageBpm = null,
                coverage = null,
            )
        }
        val sessionWindow = runCatching {
            WorkoutIntervalWindow.create(
                input.startedAt,
                input.startedAt.plus(input.activityDuration),
            ).valueOrNull()
        }.getOrNull() ?: return HeartRateEvaluation(
            status = RunningPerformanceHeartRateStatus.INVALID_SAMPLES,
            averageBpm = null,
            coverage = null,
        )
        return when (val timeline = WorkoutHeartRateTimelinePolicy.analyze(sessionWindow, input.heartRateSamples)) {
            WorkoutHeartRateTimelineResult.NoSamples -> HeartRateEvaluation(
                status = RunningPerformanceHeartRateStatus.NO_HEART_RATE_SAMPLES,
                averageBpm = null,
                coverage = null,
            )
            WorkoutHeartRateTimelineResult.InvalidSamples -> HeartRateEvaluation(
                status = RunningPerformanceHeartRateStatus.INVALID_SAMPLES,
                averageBpm = null,
                coverage = null,
            )
            is WorkoutHeartRateTimelineResult.Available -> {
                val heartRateTimeline = timeline.timeline
                val sessionSeconds = input.activityDuration.secondsAsDouble()
                val coverage = (heartRateTimeline.coveredDuration.secondsAsDouble() / sessionSeconds)
                    .coerceIn(0.0, 1.0)
                HeartRateEvaluation(
                    status = RunningPerformanceHeartRateStatus.AVAILABLE,
                    averageBpm = WorkoutHeartRateTimelinePolicy.weightedAverageBpm(heartRateTimeline),
                    coverage = coverage,
                )
            }
        }
    }

    private fun evaluatePace(input: RunningPerformanceWorkoutInput): PaceEvaluation {
        if (!input.hasActiveSnapshot) {
            return PaceEvaluation(
                status = RunningPerformancePaceStatus.NO_ACTIVE_SNAPSHOT,
                distanceMeters = null,
                coveredDuration = null,
                paceSecondsPerKilometer = null,
            )
        }
        val distanceMeters = input.canonicalDistanceMeters
        val coveredDuration = input.canonicalCoveredDurationNanos?.let { nanos ->
            if (nanos <= 0L) null else runCatching { Duration.ofNanos(nanos) }.getOrNull()
        }
        if (distanceMeters == null || !distanceMeters.isFinite() || distanceMeters <= 0.0 ||
            coveredDuration == null || !input.activityDuration.isPositive() ||
            coveredDuration > input.activityDuration
        ) {
            return PaceEvaluation(
                status = RunningPerformancePaceStatus.INVALID_CANONICAL_FACTS,
                distanceMeters = null,
                coveredDuration = null,
                paceSecondsPerKilometer = null,
            )
        }
        val paceSecondsPerKilometer = coveredDuration.secondsAsDouble() / (distanceMeters / 1_000.0)
        if (!paceSecondsPerKilometer.isFinite() || paceSecondsPerKilometer <= 0.0) {
            return PaceEvaluation(
                status = RunningPerformancePaceStatus.INVALID_CANONICAL_FACTS,
                distanceMeters = null,
                coveredDuration = null,
                paceSecondsPerKilometer = null,
            )
        }
        if (input.temporalEligibility != TemporalSeriesEligibility.ELIGIBLE ||
            input.temporalEligibilityReasons.isNotEmpty()
        ) {
            return PaceEvaluation(
                status = RunningPerformancePaceStatus.TEMPORAL_INELIGIBLE,
                distanceMeters = null,
                coveredDuration = null,
                paceSecondsPerKilometer = null,
            )
        }
        val derivationMethod = input.derivationMethod ?: return PaceEvaluation(
            status = RunningPerformancePaceStatus.INVALID_CANONICAL_FACTS,
            distanceMeters = null,
            coveredDuration = null,
            paceSecondsPerKilometer = null,
        )
        when (derivationMethod) {
            WorkoutDerivationMethod.WATCH_NATIVE -> if (input.distanceCalibrationFactor != null) {
                return invalidPace()
            }
            WorkoutDerivationMethod.WATCH_NATIVE_CONTEXT_CALIBRATED -> if (
                input.distanceCalibrationFactor?.isFinite() != true ||
                input.distanceCalibrationFactor <= 0.0
            ) {
                return invalidPace()
            }
            WorkoutDerivationMethod.STEP_LENGTH_FALLBACK,
            WorkoutDerivationMethod.FIXED_PERSONAL_STEP_LENGTH,
            -> Unit
        }
        if (input.plausibilityIndicators.isNotEmpty()) {
            return PaceEvaluation(
                status = RunningPerformancePaceStatus.PLAUSIBILITY_BLOCKED,
                distanceMeters = distanceMeters,
                coveredDuration = coveredDuration,
                paceSecondsPerKilometer = paceSecondsPerKilometer,
            )
        }
        return when (derivationMethod) {
            WorkoutDerivationMethod.WATCH_NATIVE -> if (input.derivationVersion != WATCH_WORKOUT_NATIVE_DERIVATION_VERSION) {
                unsupportedVersionPace(distanceMeters, coveredDuration, paceSecondsPerKilometer)
            } else {
                PaceEvaluation(
                    status = RunningPerformancePaceStatus.COMPARABLE_NATIVE,
                    distanceMeters = distanceMeters,
                    coveredDuration = coveredDuration,
                    paceSecondsPerKilometer = paceSecondsPerKilometer,
                )
            }
            WorkoutDerivationMethod.WATCH_NATIVE_CONTEXT_CALIBRATED -> if (input.derivationVersion != WATCH_WORKOUT_NATIVE_DERIVATION_VERSION) {
                unsupportedVersionPace(distanceMeters, coveredDuration, paceSecondsPerKilometer)
            } else {
                PaceEvaluation(
                    status = RunningPerformancePaceStatus.COMPARABLE_NATIVE,
                    distanceMeters = distanceMeters,
                    coveredDuration = coveredDuration,
                    paceSecondsPerKilometer = paceSecondsPerKilometer,
                )
            }
            WorkoutDerivationMethod.STEP_LENGTH_FALLBACK,
            WorkoutDerivationMethod.FIXED_PERSONAL_STEP_LENGTH,
            -> PaceEvaluation(
                status = RunningPerformancePaceStatus.AVAILABLE_FALLBACK_NOT_COMPARABLE,
                distanceMeters = distanceMeters,
                coveredDuration = coveredDuration,
                paceSecondsPerKilometer = paceSecondsPerKilometer,
            )
        }
    }

    private fun invalidPace(): PaceEvaluation = PaceEvaluation(
        status = RunningPerformancePaceStatus.INVALID_CANONICAL_FACTS,
        distanceMeters = null,
        coveredDuration = null,
        paceSecondsPerKilometer = null,
    )

    private fun unsupportedVersionPace(
        distanceMeters: Double,
        coveredDuration: Duration,
        paceSecondsPerKilometer: Double,
    ): PaceEvaluation = PaceEvaluation(
        status = RunningPerformancePaceStatus.AVAILABLE_UNSUPPORTED_VERSION_NOT_COMPARABLE,
        distanceMeters = distanceMeters,
        coveredDuration = coveredDuration,
        paceSecondsPerKilometer = paceSecondsPerKilometer,
    )

    private fun WorkoutDerivationMethod?.toProvenance(): RunningPerformanceDistanceProvenance? = when (this) {
        WorkoutDerivationMethod.WATCH_NATIVE -> RunningPerformanceDistanceProvenance.WATCH_NATIVE
        WorkoutDerivationMethod.WATCH_NATIVE_CONTEXT_CALIBRATED ->
            RunningPerformanceDistanceProvenance.WATCH_NATIVE_CONTEXT_CALIBRATED
        WorkoutDerivationMethod.STEP_LENGTH_FALLBACK -> RunningPerformanceDistanceProvenance.STEP_LENGTH_FALLBACK
        WorkoutDerivationMethod.FIXED_PERSONAL_STEP_LENGTH ->
            RunningPerformanceDistanceProvenance.FIXED_PERSONAL_STEP_LENGTH
        null -> null
    }

    private data class PaceEvaluation(
        val status: RunningPerformancePaceStatus,
        val distanceMeters: Double?,
        val coveredDuration: Duration?,
        val paceSecondsPerKilometer: Double?,
    )

    private data class HeartRateEvaluation(
        val status: RunningPerformanceHeartRateStatus,
        val averageBpm: Double?,
        val coverage: Double?,
    )
}
