package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

enum class WatchDailyReadCompleteness {
    EMPTY,
    PARTIAL,
    COMPLETE,
    RECENT_TAIL,
    DIVERGENT,
}

enum class WatchDailyCoverageStatus {
    UNAVAILABLE,
    PARTIAL,
    COMPLETE,
    RECENT_TAIL,
    DIVERGENT,
}

enum class WatchDailyReadIssue {
    ASSOCIATED_DEVICE_SOURCE_UNAVAILABLE,
    DAILY_SUMMARY_SOURCE_UNAVAILABLE,
    D1_SOURCE_UNAVAILABLE,
    D1_STRUCTURAL_COVERAGE_INCOMPLETE,
    WORKOUT_SOURCE_UNAVAILABLE,
    WORKOUT_DETAILS_UNAVAILABLE,
    D1_AGGREGATE_UNAVAILABLE,
}

data class WatchDailyMetricProvenance(
    val source: DataOrigin,
    val kind: MetricProvenance,
)

data class WatchDailyMetric<T>(
    val value: DomainValue<T>,
    val provenance: WatchDailyMetricProvenance? = null,
) {
    init {
        require(value is DomainValue.Absent || provenance != null)
    }
}

data class WatchDailyStepCoverage(
    val fea1TotalSteps: Long?,
    val d1TotalSteps: Long?,
    val d1SampleCount: Int,
    val d1StructuralCoverageComplete: Boolean,
    val expectedD1SampleCount: Int?,
    val missingD1Positions: Set<Int> = emptySet(),
    val differenceSteps: Long?,
    val status: WatchDailyCoverageStatus,
    val d1AggregateSteps: Long? = null,
    val fea1CollectedAt: Instant? = null,
    val latestD1At: Instant? = null,
) {
    init {
        require(d1SampleCount >= 0)
        require(fea1TotalSteps == null || fea1TotalSteps >= 0)
        require(d1TotalSteps == null || d1TotalSteps >= 0)
        require(d1AggregateSteps == null || d1AggregateSteps >= 0)
        require(d1AggregateSteps == null || d1StructuralCoverageComplete)
        require(expectedD1SampleCount == null || expectedD1SampleCount > 0)
        require(missingD1Positions.all { it > 0 })
        require(!d1StructuralCoverageComplete || expectedD1SampleCount != null)
        require(!d1StructuralCoverageComplete || missingD1Positions.isEmpty())
        require(
            differenceSteps == null || differenceSteps != 0L ||
                status in setOf(WatchDailyCoverageStatus.COMPLETE, WatchDailyCoverageStatus.PARTIAL),
        )
    }
}

data class WatchDailyHeartRatePoint(
    val measuredAt: Instant,
    val bpm: Int,
    val source: DataOrigin,
    val activityId: WorkoutActivityId? = null,
) {
    init {
        require(bpm in 30..220)
        require((source == DataOrigin.WATCH_D3_D4) == (activityId != null))
    }
}

data class WatchDailyHeartRateCoverage(
    val d1SampleCount: Int,
    val d1ValidSampleCount: Int,
    val d1SuppressedByD4Count: Int,
    val d4ActivityCount: Int,
    val d4CoveredActivityCount: Int,
    val d4ValidSampleCount: Int,
    val status: WatchDailyCoverageStatus,
) {
    init {
        require(d1SampleCount >= 0)
        require(d1ValidSampleCount in 0..d1SampleCount)
        require(d1SuppressedByD4Count in 0..d1ValidSampleCount)
        require(d4ActivityCount >= 0)
        require(d4CoveredActivityCount in 0..d4ActivityCount)
        require(d4ValidSampleCount >= 0)
    }
}

/**
 * Detalhe D4 usado pelo diário. Métricas D4 permanecem no contexto do workout;
 * nunca são somadas às métricas diárias D1.
 */
data class WatchDailyWorkout(
    val summary: CalendarWorkoutSummary,
    val endedAt: Instant,
    val steps: DomainValue<Long>,
    val heartRateSamples: List<WorkoutHeartRateSample>,
    val detailsAvailable: Boolean,
    val provenance: WatchDailyMetricProvenance? = null,
) {
    init {
        require(!endedAt.isBefore(summary.startedAt))
        require(heartRateSamples.zipWithNext().all { (first, second) -> first.sampledAt.isBefore(second.sampledAt) })
        require(heartRateSamples.all {
            !it.sampledAt.isBefore(summary.startedAt) && it.sampledAt.isBefore(endedAt)
        })
    }

    val hasValidHeartRateCoverage: Boolean
        get() = detailsAvailable && heartRateSamples.isNotEmpty()

    companion object {
        fun from(
            summary: CalendarWorkoutSummary,
            activity: WorkoutActivity?,
        ): WatchDailyWorkout {
            val startedAt = summary.startedAt
            val fallbackEndedAt = summary.endedAt ?: runCatching { startedAt.plus(summary.duration) }.getOrElse { startedAt }
            if (activity == null) {
                return WatchDailyWorkout(
                    summary = summary,
                    endedAt = fallbackEndedAt,
                    steps = DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE),
                    heartRateSamples = emptyList(),
                    detailsAvailable = false,
                    provenance = null,
                )
            }

            val endedAt = activity.metrics.endedAt.takeUnless { it.isBefore(startedAt) } ?: fallbackEndedAt
            return WatchDailyWorkout(
                summary = summary,
                endedAt = endedAt,
                steps = if (activity.metrics.stepsAvailable) {
                    DomainValue.Present(activity.metrics.steps)
                } else {
                    DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE)
                },
                heartRateSamples = activity.heartRateSamples
                    .filter { sample ->
                        !sample.sampledAt.isBefore(startedAt) && sample.sampledAt.isBefore(endedAt)
                    }
                    .sortedBy { it.sampledAt },
                detailsAvailable = true,
                provenance = WatchDailyMetricProvenance(
                    source = DataOrigin.WATCH_D3_D4,
                    kind = MetricProvenance.WATCH_REPORTED,
                ),
            )
        }
    }
}

data class WatchDailyProvenance(
    val stepsTotal: WatchDailyMetricProvenance? = null,
    val stepsDistribution: WatchDailyMetricProvenance? = null,
    val distance: WatchDailyMetricProvenance? = null,
    val calories: WatchDailyMetricProvenance? = null,
    val heartRate: Set<WatchDailyMetricProvenance> = emptySet(),
    val workout: WatchDailyMetricProvenance? = null,
)

/**
 * Read model local composto por finalidade. FEA1 fornece total de passos, D1
 * fornece linha granular diária e D4 permanece disponível como detalhe de
 * workout. Distância/calorias diárias nunca incluem valores D4.
 */
data class WatchDailyReadModel(
    val localDate: LocalDate,
    val zoneId: ZoneId,
    val fea1Summary: DailySummary?,
    val steps: WatchDailyMetric<Long>,
    val distanceMeters: WatchDailyMetric<Double>,
    val caloriesKilocalories: WatchDailyMetric<Double>,
    val d1Samples: List<WatchDailyD1Sample>,
    val workouts: List<WatchDailyWorkout>,
    val heartRate: List<WatchDailyHeartRatePoint>,
    val stepCoverage: WatchDailyStepCoverage,
    val heartRateCoverage: WatchDailyHeartRateCoverage,
    val completeness: WatchDailyReadCompleteness,
    val provenance: WatchDailyProvenance,
    val issues: Set<WatchDailyReadIssue> = emptySet(),
) {
    init {
        require(d1Samples.all { it.resolvedAt.toLocalDateIn(zoneId) == localDate })
        require(workouts.all { it.intersects(localDate, zoneId) })
        require(heartRate.all { it.measuredAt.toLocalDateIn(zoneId) == localDate })
        require(heartRate.zipWithNext().all { (first, second) ->
            first.measuredAt.isBefore(second.measuredAt) ||
                first.measuredAt == second.measuredAt && first.source.ordinal <= second.source.ordinal
        })
    }

    companion object {
        fun empty(
            localDate: LocalDate,
            zoneId: ZoneId,
            issues: Set<WatchDailyReadIssue> = emptySet(),
        ): WatchDailyReadModel = WatchDailyReadModel(
            localDate = localDate,
            zoneId = zoneId,
            fea1Summary = null,
            steps = absentMetric(),
            distanceMeters = absentMetric(),
            caloriesKilocalories = absentMetric(),
            d1Samples = emptyList(),
            workouts = emptyList(),
            heartRate = emptyList(),
            stepCoverage = WatchDailyStepCoverage(
                fea1TotalSteps = null,
                d1TotalSteps = null,
                d1AggregateSteps = null,
                d1SampleCount = 0,
                d1StructuralCoverageComplete = false,
                expectedD1SampleCount = null,
                missingD1Positions = emptySet(),
                differenceSteps = null,
                status = WatchDailyCoverageStatus.UNAVAILABLE,
            ),
            heartRateCoverage = WatchDailyHeartRateCoverage(
                d1SampleCount = 0,
                d1ValidSampleCount = 0,
                d1SuppressedByD4Count = 0,
                d4ActivityCount = 0,
                d4CoveredActivityCount = 0,
                d4ValidSampleCount = 0,
                status = WatchDailyCoverageStatus.UNAVAILABLE,
            ),
            completeness = if (issues.isEmpty()) {
                WatchDailyReadCompleteness.EMPTY
            } else {
                WatchDailyReadCompleteness.PARTIAL
            },
            provenance = WatchDailyProvenance(),
            issues = issues,
        )
    }
}

interface WatchDailyReadModelRepository {
    fun observeDay(query: CalendarDayQuery): Flow<WatchDailyReadModel> =
        observeDay(query.localDate, query.zoneId)

    fun observeDay(localDate: LocalDate, zoneId: ZoneId): Flow<WatchDailyReadModel> =
        observePeriod(CalendarDateRange(localDate, localDate.plusDays(1)), zoneId)
            .map { it.singleOrNull() ?: WatchDailyReadModel.empty(localDate, zoneId) }

    fun observePeriod(query: CalendarHistoryQuery): Flow<List<WatchDailyReadModel>> =
        observePeriod(query.dateRange, query.zoneId)

    fun observePeriod(range: CalendarDateRange, zoneId: ZoneId): Flow<List<WatchDailyReadModel>> =
        flowOf(emptyList())
}

private fun <T> absentMetric(): WatchDailyMetric<T> = WatchDailyMetric(
    value = DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE),
)

object WatchDailyReadModelComposer {
    fun compose(
        localDate: LocalDate,
        zoneId: ZoneId,
        associatedDeviceId: DeviceId?,
        summaries: List<DailySummary>,
        d1Samples: List<WatchDailyD1Sample>,
        workouts: List<WatchDailyWorkout>,
        issues: Set<WatchDailyReadIssue> = emptySet(),
    ): WatchDailyReadModel {
        val summaryCandidates = summaries
            .asSequence()
            .filter { summary ->
                summary.validity == DataValidity.VALID &&
                    summary.key.origin == DataOrigin.WATCH_DAILY_SUMMARY &&
                    summary.key.localDate == localDate
            }
            .toList()
        val fea1Summary = summaryCandidates
            .filter { it.key.deviceId == associatedDeviceId }
            .takeIf { associatedDeviceId != null && it.isNotEmpty() }
            .orEmpty()
            .ifEmpty { summaryCandidates }
            .sortedWith(
                compareByDescending<DailySummary> { it.updatedAt }
                    .thenByDescending { it.collectedAt }
                    .thenBy { it.id.value.toString() },
            )
            .firstOrNull()

        val daySamples = d1Samples
            .asSequence()
            .filter { it.resolvedAt.toLocalDateIn(zoneId) == localDate }
            .sortedWith(compareBy<WatchDailyD1Sample> { it.resolvedAt }.thenBy { it.currentPosition })
            .toList()
        val dayWorkouts = workouts
            .asSequence()
            .filter { it.intersects(localDate, zoneId) }
            .sortedWith(compareBy<WatchDailyWorkout> { it.summary.startedAt }.thenBy { it.summary.activityId.value.toString() })
            .toList()

        val d1Steps = daySamples.map { it.steps }.sumExactOrNull()
        val d1StructuralCoverage = d1StructuralCoverage(daySamples)
        val d1AggregateSteps = d1Steps.takeIf { d1StructuralCoverage.complete }
        val d1Distance = daySamples.map { it.distanceMeters }.finiteSumOrNull()
        val d1Calories = daySamples.map { it.caloriesKilocalories }.finiteSumOrNull()
        val aggregateIssues = buildSet {
            addAll(issues)
            if (daySamples.isNotEmpty() && !d1StructuralCoverage.complete) {
                add(WatchDailyReadIssue.D1_STRUCTURAL_COVERAGE_INCOMPLETE)
            }
            if (daySamples.isNotEmpty() &&
                (d1AggregateSteps == null || d1Distance == null || d1Calories == null)
            ) {
                add(WatchDailyReadIssue.D1_AGGREGATE_UNAVAILABLE)
            }
        }

        val steps = fea1Summary?.let {
            WatchDailyMetric(
                value = DomainValue.Present(it.steps),
                provenance = WatchDailyMetricProvenance(
                    source = DataOrigin.WATCH_DAILY_SUMMARY,
                    kind = MetricProvenance.WATCH_REPORTED,
                ),
            )
        } ?: absentMetric()
        val distance = when {
            d1StructuralCoverage.complete && d1Distance != null -> measuredMetric(d1Distance, DataOrigin.WATCH_D1_HISTORY)
            daySamples.isEmpty() && WatchDailyReadIssue.D1_SOURCE_UNAVAILABLE !in issues -> fea1Summary?.estimatedDistanceMeters?.let {
                estimatedMetric(it, DataOrigin.WATCH_DAILY_SUMMARY)
            } ?: absentMetric()
            else -> absentMetric()
        }
        val calories = when {
            d1StructuralCoverage.complete && d1Calories != null -> measuredMetric(d1Calories, DataOrigin.WATCH_D1_HISTORY)
            daySamples.isEmpty() && WatchDailyReadIssue.D1_SOURCE_UNAVAILABLE !in issues -> fea1Summary?.estimatedCaloriesKilocalories?.let {
                estimatedMetric(it, DataOrigin.WATCH_DAILY_SUMMARY)
            } ?: absentMetric()
            else -> absentMetric()
        }

        val stepCoverage = stepCoverage(
            fea1Total = fea1Summary?.steps,
            d1Total = d1Steps,
            d1Aggregate = d1AggregateSteps,
            d1SampleCount = daySamples.size,
            structuralCoverage = d1StructuralCoverage,
            fea1CollectedAt = fea1Summary?.collectedAt,
            latestD1At = daySamples.maxOfOrNull { it.resolvedAt },
        )
        val heartRateResult = composeHeartRate(localDate, zoneId, daySamples, dayWorkouts)
        val completeIssues = aggregateIssues + if (dayWorkouts.any { !it.detailsAvailable }) {
            setOf(WatchDailyReadIssue.WORKOUT_DETAILS_UNAVAILABLE)
        } else {
            emptySet()
        }
        val completeness = when {
            stepCoverage.status == WatchDailyCoverageStatus.DIVERGENT -> WatchDailyReadCompleteness.DIVERGENT
            stepCoverage.status == WatchDailyCoverageStatus.RECENT_TAIL -> WatchDailyReadCompleteness.RECENT_TAIL
            completeIssues.isNotEmpty() -> WatchDailyReadCompleteness.PARTIAL
            stepCoverage.status == WatchDailyCoverageStatus.COMPLETE -> WatchDailyReadCompleteness.COMPLETE
            fea1Summary != null || d1AggregateSteps != null || d1Steps != null || dayWorkouts.isNotEmpty() ->
                WatchDailyReadCompleteness.PARTIAL
            else -> WatchDailyReadCompleteness.EMPTY
        }

        return WatchDailyReadModel(
            localDate = localDate,
            zoneId = zoneId,
            fea1Summary = fea1Summary,
            steps = steps,
            distanceMeters = distance,
            caloriesKilocalories = calories,
            d1Samples = daySamples,
            workouts = dayWorkouts,
            heartRate = heartRateResult.points,
            stepCoverage = stepCoverage,
            heartRateCoverage = heartRateResult.coverage,
            completeness = completeness,
            provenance = WatchDailyProvenance(
                stepsTotal = steps.provenance,
                stepsDistribution = WatchDailyMetricProvenance(
                    source = DataOrigin.WATCH_D1_HISTORY,
                    kind = MetricProvenance.WATCH_REPORTED,
                ).takeIf { daySamples.isNotEmpty() },
                distance = distance.provenance,
                calories = calories.provenance,
                heartRate = buildSet {
                    if (heartRateResult.points.any { it.source == DataOrigin.WATCH_D1_HISTORY }) {
                        add(WatchDailyMetricProvenance(DataOrigin.WATCH_D1_HISTORY, MetricProvenance.WATCH_REPORTED))
                    }
                    if (heartRateResult.points.any { it.source == DataOrigin.WATCH_D3_D4 }) {
                        add(WatchDailyMetricProvenance(DataOrigin.WATCH_D3_D4, MetricProvenance.WATCH_REPORTED))
                    }
                },
                workout = WatchDailyMetricProvenance(
                    source = DataOrigin.WATCH_D3_D4,
                    kind = MetricProvenance.WATCH_REPORTED,
                ).takeIf {
                    dayWorkouts.isNotEmpty() &&
                        dayWorkouts.all { it.detailsAvailable && it.provenance != null }
                },
            ),
            issues = completeIssues,
        )
    }

    private fun stepCoverage(
        fea1Total: Long?,
        d1Total: Long?,
        d1Aggregate: Long?,
        d1SampleCount: Int,
        structuralCoverage: D1StructuralCoverage,
        fea1CollectedAt: Instant?,
        latestD1At: Instant?,
    ): WatchDailyStepCoverage {
        val difference = if (fea1Total != null && d1Total != null) {
            runCatching { Math.subtractExact(fea1Total, d1Total) }.getOrNull()
        } else {
            null
        }
        val hasRecentTail = difference != null && difference > 0L &&
            structuralCoverage.complete &&
            fea1CollectedAt != null && latestD1At != null &&
            !latestD1At.isAfter(fea1CollectedAt) &&
            runCatching { Duration.between(latestD1At, fea1CollectedAt) }
                .getOrNull()
                ?.let { it >= Duration.ZERO && it <= WATCH_DAILY_RECENT_TAIL_MAX_AGE } == true
        val status = when {
            fea1Total == null && d1Total == null -> WatchDailyCoverageStatus.UNAVAILABLE
            fea1Total == null || d1Total == null -> WatchDailyCoverageStatus.PARTIAL
            !structuralCoverage.complete -> WatchDailyCoverageStatus.PARTIAL
            difference == 0L -> WatchDailyCoverageStatus.COMPLETE
            difference != null && difference < 0L -> WatchDailyCoverageStatus.DIVERGENT
            hasRecentTail -> WatchDailyCoverageStatus.RECENT_TAIL
            else -> WatchDailyCoverageStatus.DIVERGENT
        }
        return WatchDailyStepCoverage(
            fea1TotalSteps = fea1Total,
            d1TotalSteps = d1Total,
            d1AggregateSteps = d1Aggregate,
            d1SampleCount = d1SampleCount,
            d1StructuralCoverageComplete = structuralCoverage.complete,
            expectedD1SampleCount = structuralCoverage.expectedSampleCount,
            missingD1Positions = structuralCoverage.missingPositions,
            differenceSteps = difference,
            status = status,
            fea1CollectedAt = fea1CollectedAt,
            latestD1At = latestD1At,
        )
    }

    private val WATCH_DAILY_RECENT_TAIL_MAX_AGE: Duration = Duration.ofMinutes(10)

    private fun d1StructuralCoverage(samples: List<WatchDailyD1Sample>): D1StructuralCoverage {
        if (samples.isEmpty()) return D1StructuralCoverage()

        val totalPositions = samples.map { it.totalPositions }.toSet().singleOrNull()
        val expectedSampleCount = totalPositions?.takeIf { it > 0 }
        val positions = samples.map { it.currentPosition }
        val distinctPositions = positions.toSet()
        val missingPositions = expectedSampleCount?.let { expected ->
            (1..expected).filterNotTo(linkedSetOf()) { it in distinctPositions }
        }.orEmpty()
        val complete = expectedSampleCount != null &&
            positions.size == expectedSampleCount &&
            distinctPositions.size == expectedSampleCount &&
            positions.all { it in 1..expectedSampleCount }

        return D1StructuralCoverage(
            complete = complete,
            expectedSampleCount = expectedSampleCount,
            missingPositions = missingPositions,
        )
    }

    private fun composeHeartRate(
        localDate: LocalDate,
        zoneId: ZoneId,
        d1Samples: List<WatchDailyD1Sample>,
        workouts: List<WatchDailyWorkout>,
    ): HeartRateComposition {
        val d1Points = d1Samples.mapNotNull { sample ->
            if (sample.resolvedAt.toLocalDateIn(zoneId) != localDate) return@mapNotNull null
            sample.bpm?.let {
                WatchDailyHeartRatePoint(
                    measuredAt = sample.resolvedAt,
                    bpm = it,
                    source = DataOrigin.WATCH_D1_HISTORY,
                )
            }
        }
        val d4Points = workouts
            .filter { it.hasValidHeartRateCoverage }
            .flatMap { workout ->
                workout.heartRateSamples.mapNotNull { sample ->
                    if (sample.sampledAt.toLocalDateIn(zoneId) != localDate) return@mapNotNull null
                    WatchDailyHeartRatePoint(
                        measuredAt = sample.sampledAt,
                        bpm = sample.bpm,
                        source = DataOrigin.WATCH_D3_D4,
                        activityId = workout.summary.activityId,
                    )
                }
            }
        val d4CoveredActivityIds = d4Points.mapNotNull { it.activityId }.toSet()
        val coveredWorkouts = workouts.filter {
            it.hasValidHeartRateCoverage && it.summary.activityId in d4CoveredActivityIds
        }
        val includedD1Points = d1Points.filterNot { point ->
            coveredWorkouts.any { workout ->
                !point.measuredAt.isBefore(workout.summary.startedAt) &&
                    point.measuredAt.isBefore(workout.endedAt)
            }
        }
        val points = (includedD1Points + d4Points)
            .distinctBy { Triple(it.measuredAt, it.source, it.activityId) }
            .sortedWith(compareBy<WatchDailyHeartRatePoint> { it.measuredAt }.thenBy { it.source.ordinal })
        val d1SuppressedCount = d1Points.size - includedD1Points.size
        val d4ValidSampleCount = d4Points.size
        val status = when {
            d1Points.isEmpty() && d4Points.isEmpty() -> if (d1Samples.isEmpty()) {
                WatchDailyCoverageStatus.UNAVAILABLE
            } else {
                WatchDailyCoverageStatus.PARTIAL
            }
            d1Points.size < d1Samples.size || workouts.any { !it.detailsAvailable } -> WatchDailyCoverageStatus.PARTIAL
            else -> WatchDailyCoverageStatus.COMPLETE
        }
        return HeartRateComposition(
            points = points,
            coverage = WatchDailyHeartRateCoverage(
                d1SampleCount = d1Samples.size,
                d1ValidSampleCount = d1Points.size,
                d1SuppressedByD4Count = d1SuppressedCount,
                d4ActivityCount = workouts.size,
                d4CoveredActivityCount = coveredWorkouts.size,
                d4ValidSampleCount = d4ValidSampleCount,
                status = status,
            ),
        )
    }

    private fun <T> measuredMetric(value: T, source: DataOrigin): WatchDailyMetric<T> = WatchDailyMetric(
        value = DomainValue.Present(value),
        provenance = WatchDailyMetricProvenance(source, MetricProvenance.WATCH_REPORTED),
    )

    private fun <T> estimatedMetric(value: T, source: DataOrigin): WatchDailyMetric<T> = WatchDailyMetric(
        value = DomainValue.Present(value),
        provenance = WatchDailyMetricProvenance(source, MetricProvenance.ESTIMATED),
    )

    private data class HeartRateComposition(
        val points: List<WatchDailyHeartRatePoint>,
        val coverage: WatchDailyHeartRateCoverage,
    )

    private data class D1StructuralCoverage(
        val complete: Boolean = false,
        val expectedSampleCount: Int? = null,
        val missingPositions: Set<Int> = emptySet(),
    )
}

private fun List<Long>.sumExactOrNull(): Long? {
    if (isEmpty()) return null
    return runCatching {
        fold(0L) { total, value -> Math.addExact(total, value) }
    }.getOrNull()
}

private fun WatchDailyWorkout.intersects(localDate: LocalDate, zoneId: ZoneId): Boolean {
    val dayStart = localDate.atStartOfDay(zoneId).toInstant()
    val dayEnd = localDate.plusDays(1).atStartOfDay(zoneId).toInstant()
    return endedAt.isAfter(summary.startedAt) &&
        summary.startedAt.isBefore(dayEnd) &&
        endedAt.isAfter(dayStart)
}

private fun List<Double>.finiteSumOrNull(): Double? {
    if (isEmpty()) return null
    var total = 0.0
    for (value in this) {
        total += value
        if (!total.isFinite()) return null
    }
    return total
}
