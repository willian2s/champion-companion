package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Fatos mínimos necessários para recompor carga cardiovascular histórica. */
data class CardiovascularLoadWorkoutInput(
    val activityOrigin: WorkoutActivityOrigin,
    val workflowState: ActivityWorkflowState,
    val startedAt: Instant,
    val duration: Duration,
    val confirmedType: WorkoutType?,
    val suggestedType: WorkoutType?,
    val heartRateSamples: List<WorkoutHeartRateSample>,
)

enum class CardiovascularLoadHistoryStatus {
    AVAILABLE,
    CONFIGURATION_REQUIRED,
    NO_WORKOUTS,
    NO_ANALYZABLE_WORKOUTS,
    UNAVAILABLE,
}

enum class CardiovascularLoadComparisonStatus {
    AVAILABLE,
    NO_PREVIOUS_BASELINE,
    UNAVAILABLE,
}

data class CardiovascularLoadPeriodSummary(
    val status: CardiovascularLoadHistoryStatus,
    val totalLoad: Double,
    val totalWorkoutCount: Int,
    val analyzedWorkoutCount: Int,
    val totalWorkoutDuration: Duration,
    val coveredHeartRateDuration: Duration,
    val coverageRatio: Double,
    val partial: Boolean,
)

data class CardiovascularLoadComparison(
    val status: CardiovascularLoadComparisonStatus,
    val absoluteDelta: Double?,
    val percentDelta: Double?,
)

data class CardiovascularLoadDailyBucket(
    val localDate: LocalDate,
    val totalLoad: Double,
    val workoutCount: Int,
    val analyzedWorkoutCount: Int,
    val coveredHeartRateDuration: Duration,
    val totalWorkoutDuration: Duration,
    val coverageRatio: Double,
    val partial: Boolean,
)

data class CardiovascularLoadTypeSummary(
    val workoutType: WorkoutType,
    val totalLoad: Double,
    val workoutCount: Int,
    val analyzedWorkoutCount: Int,
    val partial: Boolean,
)

data class CardiovascularLoadHistory(
    val asOf: Instant,
    val zoneId: ZoneId,
    val current7: CardiovascularLoadPeriodSummary,
    val previous7: CardiovascularLoadPeriodSummary,
    val current28: CardiovascularLoadPeriodSummary,
    val previous28: CardiovascularLoadPeriodSummary,
    val current7Comparison: CardiovascularLoadComparison,
    val current28Comparison: CardiovascularLoadComparison,
    val dailyBuckets: List<CardiovascularLoadDailyBucket>,
    val typeSummaries: List<CardiovascularLoadTypeSummary>,
)

data class CardiovascularLoadHistoryResult(
    val status: CardiovascularLoadHistoryStatus,
    val history: CardiovascularLoadHistory?,
)

/** Agregação efêmera da análise individual de frequência cardíaca da Spec 034. */
object CardiovascularLoadHistoryPolicy {
    private val SEVEN_DAYS = Duration.ofDays(7)
    private val TWENTY_EIGHT_DAYS = Duration.ofDays(28)
    private val FIFTY_SIX_DAYS = Duration.ofDays(56)

    fun analyze(
        workouts: List<CardiovascularLoadWorkoutInput>,
        maxHeartRateBpm: Int?,
        asOf: Instant,
        zoneId: ZoneId,
    ): CardiovascularLoadHistoryResult {
        val windows = HistoryWindows.from(asOf)
        val recordsByPeriod = HistoryPeriod.entries.associateWith { mutableListOf<AnalyzedWorkout>() }
        val boundedWorkouts = workouts.filter { workout ->
            workout.activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4 &&
                workout.workflowState != ActivityWorkflowState.DISCARDED &&
                !workout.startedAt.isBefore(windows.previous28.start) &&
                workout.startedAt.isBefore(windows.current7.end)
        }

        boundedWorkouts.forEach { workout ->
            val sessionWindow = runCatching {
                WorkoutIntervalWindow.create(
                    workout.startedAt,
                    workout.startedAt.plus(workout.duration),
                ).valueOrNull()
            }.getOrNull()
                ?: return CardiovascularLoadHistoryResult(
                    status = CardiovascularLoadHistoryStatus.UNAVAILABLE,
                    history = null,
                )

            val record = AnalyzedWorkout(
                workout = workout,
                type = workout.confirmedType ?: workout.suggestedType ?: WorkoutType.UNKNOWN,
                result = WorkoutHeartRateAnalysisPolicy.analyze(
                    sessionWindow = sessionWindow,
                    heartRateSamples = workout.heartRateSamples,
                    configuredMaximumBpm = maxHeartRateBpm,
                ),
            )
            HistoryPeriod.entries.forEach { period ->
                if (windows[period].contains(workout.startedAt)) {
                    recordsByPeriod.getValue(period).add(record)
                }
            }
        }

        val current7 = summarize(recordsByPeriod.getValue(HistoryPeriod.CURRENT_7), maxHeartRateBpm)
        val previous7 = summarize(recordsByPeriod.getValue(HistoryPeriod.PREVIOUS_7), maxHeartRateBpm)
        val current28Records = recordsByPeriod.getValue(HistoryPeriod.CURRENT_28)
        val previous28 = summarize(recordsByPeriod.getValue(HistoryPeriod.PREVIOUS_28), maxHeartRateBpm)
        val current28 = summarize(current28Records, maxHeartRateBpm)
        val current28Window = windows[HistoryPeriod.CURRENT_28]

        val history = CardiovascularLoadHistory(
            asOf = asOf,
            zoneId = zoneId,
            current7 = current7,
            previous7 = previous7,
            current28 = current28,
            previous28 = previous28,
            current7Comparison = compare(current7, previous7),
            current28Comparison = compare(current28, previous28),
            dailyBuckets = dailyBuckets(current28Records, current28Window, zoneId),
            typeSummaries = typeSummaries(current28Records),
        )
        return CardiovascularLoadHistoryResult(
            status = current28.status,
            history = history,
        )
    }

    private fun summarize(
        records: List<AnalyzedWorkout>,
        maxHeartRateBpm: Int?,
    ): CardiovascularLoadPeriodSummary {
        val totalWorkoutDuration = records.fold(Duration.ZERO) { total, record ->
            total.plus(record.workout.duration)
        }
        val coveredHeartRateDuration = records.fold(Duration.ZERO) { total, record ->
            total.plus(record.result.analysis?.coveredDuration ?: Duration.ZERO)
        }
        val totalSeconds = totalWorkoutDuration.secondsAsDouble()
        val coverageRatio = if (totalSeconds > 0.0) {
            (coveredHeartRateDuration.secondsAsDouble() / totalSeconds).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
        val availableRecords = records.filter { record ->
            val available = record.result as? WorkoutHeartRateAnalysisResult.Available
            available?.analysis?.cardiovascularLoad?.isFinite() == true
        }
        val totalLoad = availableRecords.mapNotNull { record ->
            (record.result as WorkoutHeartRateAnalysisResult.Available).analysis.cardiovascularLoad
        }.sum()
        val status = when {
            records.isEmpty() -> CardiovascularLoadHistoryStatus.NO_WORKOUTS
            maxHeartRateBpm == null -> CardiovascularLoadHistoryStatus.CONFIGURATION_REQUIRED
            availableRecords.isNotEmpty() -> CardiovascularLoadHistoryStatus.AVAILABLE
            else -> CardiovascularLoadHistoryStatus.NO_ANALYZABLE_WORKOUTS
        }
        val partial = records.any { record ->
            val analysis = (record.result as? WorkoutHeartRateAnalysisResult.Available)?.analysis
            analysis == null || analysis.cardiovascularLoad == null || analysis.coverageRatio < 1.0
        }
        return CardiovascularLoadPeriodSummary(
            status = status,
            totalLoad = totalLoad,
            totalWorkoutCount = records.size,
            analyzedWorkoutCount = availableRecords.size,
            totalWorkoutDuration = totalWorkoutDuration,
            coveredHeartRateDuration = coveredHeartRateDuration,
            coverageRatio = coverageRatio,
            partial = partial,
        )
    }

    private fun dailyBuckets(
        records: List<AnalyzedWorkout>,
        window: HistoryWindow,
        zoneId: ZoneId,
    ): List<CardiovascularLoadDailyBucket> {
        val firstDate = window.start.atZone(zoneId).toLocalDate()
        val lastDate = window.end.minusNanos(1).atZone(zoneId).toLocalDate()
        val recordsByDate = records.groupBy { it.workout.startedAt.atZone(zoneId).toLocalDate() }
        return generateSequence(firstDate) { date ->
            date.plusDays(1).takeIf { !it.isAfter(lastDate) }
        }.map { date ->
            val summary = summarize(recordsByDate[date].orEmpty(), maxHeartRateBpm = null)
            CardiovascularLoadDailyBucket(
                localDate = date,
                totalLoad = summary.totalLoad,
                workoutCount = summary.totalWorkoutCount,
                analyzedWorkoutCount = summary.analyzedWorkoutCount,
                coveredHeartRateDuration = summary.coveredHeartRateDuration,
                totalWorkoutDuration = summary.totalWorkoutDuration,
                coverageRatio = summary.coverageRatio,
                partial = summary.partial,
            )
        }.toList()
    }

    private fun typeSummaries(records: List<AnalyzedWorkout>): List<CardiovascularLoadTypeSummary> =
        records.groupBy(AnalyzedWorkout::type)
            .toSortedMap()
            .map { (type, typeRecords) ->
                val summary = summarize(typeRecords, maxHeartRateBpm = null)
                CardiovascularLoadTypeSummary(
                    workoutType = type,
                    totalLoad = summary.totalLoad,
                    workoutCount = summary.totalWorkoutCount,
                    analyzedWorkoutCount = summary.analyzedWorkoutCount,
                    partial = summary.partial,
                )
            }

    private fun compare(
        current: CardiovascularLoadPeriodSummary,
        previous: CardiovascularLoadPeriodSummary,
    ): CardiovascularLoadComparison {
        val currentHasNumericLoad = current.status == CardiovascularLoadHistoryStatus.AVAILABLE ||
            current.status == CardiovascularLoadHistoryStatus.NO_WORKOUTS
        if (!currentHasNumericLoad) {
            return CardiovascularLoadComparison(
                status = CardiovascularLoadComparisonStatus.UNAVAILABLE,
                absoluteDelta = null,
                percentDelta = null,
            )
        }

        val absoluteDelta = current.totalLoad - previous.totalLoad
        if (previous.status == CardiovascularLoadHistoryStatus.NO_WORKOUTS ||
            previous.status == CardiovascularLoadHistoryStatus.NO_ANALYZABLE_WORKOUTS ||
            previous.status == CardiovascularLoadHistoryStatus.AVAILABLE && previous.totalLoad == 0.0
        ) {
            return CardiovascularLoadComparison(
                status = CardiovascularLoadComparisonStatus.NO_PREVIOUS_BASELINE,
                absoluteDelta = absoluteDelta,
                percentDelta = null,
            )
        }
        if (previous.status != CardiovascularLoadHistoryStatus.AVAILABLE ||
            previous.totalLoad <= 0.0 ||
            !previous.totalLoad.isFinite()
        ) {
            return CardiovascularLoadComparison(
                status = CardiovascularLoadComparisonStatus.UNAVAILABLE,
                absoluteDelta = null,
                percentDelta = null,
            )
        }

        val percentDelta = (current.totalLoad / previous.totalLoad - 1.0) * 100.0
        return if (percentDelta.isFinite()) {
            CardiovascularLoadComparison(
                status = CardiovascularLoadComparisonStatus.AVAILABLE,
                absoluteDelta = absoluteDelta,
                percentDelta = percentDelta,
            )
        } else {
            CardiovascularLoadComparison(
                status = CardiovascularLoadComparisonStatus.UNAVAILABLE,
                absoluteDelta = null,
                percentDelta = null,
            )
        }
    }

    private data class AnalyzedWorkout(
        val workout: CardiovascularLoadWorkoutInput,
        val type: WorkoutType,
        val result: WorkoutHeartRateAnalysisResult,
    )

    private enum class HistoryPeriod {
        CURRENT_7,
        PREVIOUS_7,
        CURRENT_28,
        PREVIOUS_28,
    }

    private data class HistoryWindow(
        val start: Instant,
        val end: Instant,
    ) {
        fun contains(instant: Instant): Boolean = !instant.isBefore(start) && instant.isBefore(end)
    }

    private data class HistoryWindows(
        val current7: HistoryWindow,
        val previous7: HistoryWindow,
        val current28: HistoryWindow,
        val previous28: HistoryWindow,
    ) {
        operator fun get(period: HistoryPeriod): HistoryWindow = when (period) {
            HistoryPeriod.CURRENT_7 -> current7
            HistoryPeriod.PREVIOUS_7 -> previous7
            HistoryPeriod.CURRENT_28 -> current28
            HistoryPeriod.PREVIOUS_28 -> previous28
        }

        companion object {
            fun from(asOf: Instant): HistoryWindows {
                val current7Start = asOf.minus(SEVEN_DAYS)
                val current28Start = asOf.minus(TWENTY_EIGHT_DAYS)
                return HistoryWindows(
                    current7 = HistoryWindow(current7Start, asOf),
                    previous7 = HistoryWindow(asOf.minus(SEVEN_DAYS.multipliedBy(2)), current7Start),
                    current28 = HistoryWindow(current28Start, asOf),
                    previous28 = HistoryWindow(asOf.minus(FIFTY_SIX_DAYS), current28Start),
                )
            }
        }
    }
}
