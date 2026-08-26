package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.Flow

/** Períodos suportados pela primeira versão da leitura de evolução. */
enum class EvolutionPeriod(val windowDays: Long) {
    LAST_7_DAYS(7),
    LAST_30_DAYS(30),
    LAST_90_DAYS(90),
    ;

    val days: Long
        get() = windowDays

    fun windowEndingAt(untilExclusive: LocalDate, zoneId: ZoneId): EvolutionPeriodWindow =
        EvolutionPeriodWindow(
            period = this,
            fromInclusive = untilExclusive.minusDays(windowDays),
            untilExclusive = untilExclusive,
            zoneId = zoneId,
        )
}

val EVOLUTION_ELIGIBLE_WORKFLOW_STATES: Set<ActivityWorkflowState> = setOf(
    ActivityWorkflowState.READY_TO_SYNC,
    ActivityWorkflowState.SYNCING,
    ActivityWorkflowState.SYNCED,
    ActivityWorkflowState.SYNC_FAILED,
)

/** Janela civil e instantânea congelada para uma consulta de evolução. */
data class EvolutionPeriodWindow(
    val period: EvolutionPeriod,
    val fromInclusive: LocalDate,
    val untilExclusive: LocalDate,
    val zoneId: ZoneId,
) {
    init {
        require(fromInclusive.isBefore(untilExclusive))
        require(ChronoUnit.DAYS.between(fromInclusive, untilExclusive) == period.windowDays)
    }

    val dateRange: CalendarDateRange
        get() = CalendarDateRange(fromInclusive, untilExclusive)

    val instantRange: CalendarInstantRange
        get() = dateRange.atZone(zoneId)
}

/** Consulta determinística; [untilExclusive] é data civil, não data de importação. */
data class EvolutionQuery(
    val period: EvolutionPeriod,
    val untilExclusive: LocalDate,
    val zoneId: ZoneId,
    val filter: EvolutionWorkoutFilter = EvolutionWorkoutFilter.Consolidated,
) {
    val currentWindow: EvolutionPeriodWindow
        get() = period.windowEndingAt(untilExclusive, zoneId)

    val previousWindow: EvolutionPeriodWindow
        get() = period.windowEndingAt(currentWindow.fromInclusive, zoneId)

    val currentRange: CalendarInstantRange
        get() = currentWindow.instantRange

    val previousRange: CalendarInstantRange
        get() = previousWindow.instantRange

    fun previousQuery(): EvolutionQuery = copy(untilExclusive = currentWindow.fromInclusive)
}

/** Filtro compartilhado por volume, tendências, comparação e recordes. */
sealed interface EvolutionWorkoutFilter {
    val isConsolidated: Boolean

    fun accepts(workoutType: WorkoutType): Boolean

    data object Consolidated : EvolutionWorkoutFilter {
        override val isConsolidated: Boolean = true

        override fun accepts(workoutType: WorkoutType): Boolean = workoutType != WorkoutType.UNKNOWN
    }

    data class ByType(val workoutType: WorkoutType) : EvolutionWorkoutFilter {
        init {
            require(workoutType != WorkoutType.UNKNOWN)
        }

        override val isConsolidated: Boolean = false

        override fun accepts(workoutType: WorkoutType): Boolean = this.workoutType == workoutType
    }

    companion object {
        fun consolidated(): EvolutionWorkoutFilter = Consolidated

        fun forType(workoutType: WorkoutType): EvolutionWorkoutFilter = ByType(workoutType)
    }
}

/** Métricas que podem aparecer em série ou comparação. Unidades ficam documentadas aqui. */
enum class EvolutionMetric {
    ACTIVITY_COUNT,
    DISTANCE, // metros
    DURATION, // segundos
    STEPS,
    ACTIVE_DAYS,
    AVERAGE_BPM,
    CADENCE, // passos por minuto
    PACE, // segundos por quilômetro
    SPEED, // metros por segundo
}

enum class EvolutionSeriesGranularity {
    ACTIVITY,
    DAY,
    PERIOD,
}

/** Estado explícito evita converter ausência, inaplicabilidade ou gap para zero. */
enum class EvolutionSeriesPointState {
    PRESENT,
    ABSENT,
    NOT_APPLICABLE,
    GAP,
}

/** Ponto neutro de domínio; adaptação para [ChampionChartPoint] fica na apresentação. */
data class EvolutionSeriesPoint(
    val startedAt: Instant,
    val value: Double?,
    val state: EvolutionSeriesPointState = if (value == null) {
        EvolutionSeriesPointState.ABSENT
    } else {
        EvolutionSeriesPointState.PRESENT
    },
    val activityId: WorkoutActivityId? = null,
) {
    init {
        require(value?.isFinite() != false)
        require((value != null) == (state == EvolutionSeriesPointState.PRESENT))
        require(state != EvolutionSeriesPointState.GAP || activityId == null)
    }

    companion object {
        fun present(
            startedAt: Instant,
            value: Double,
            activityId: WorkoutActivityId? = null,
        ): EvolutionSeriesPoint = EvolutionSeriesPoint(
            startedAt = startedAt,
            value = value,
            state = EvolutionSeriesPointState.PRESENT,
            activityId = activityId,
        )

        fun absent(
            startedAt: Instant,
            activityId: WorkoutActivityId? = null,
        ): EvolutionSeriesPoint = EvolutionSeriesPoint(
            startedAt = startedAt,
            value = null,
            state = EvolutionSeriesPointState.ABSENT,
            activityId = activityId,
        )

        fun notApplicable(
            startedAt: Instant,
            activityId: WorkoutActivityId? = null,
        ): EvolutionSeriesPoint = EvolutionSeriesPoint(
            startedAt = startedAt,
            value = null,
            state = EvolutionSeriesPointState.NOT_APPLICABLE,
            activityId = activityId,
        )

        fun gap(startedAt: Instant): EvolutionSeriesPoint = EvolutionSeriesPoint(
            startedAt = startedAt,
            value = null,
            state = EvolutionSeriesPointState.GAP,
        )
    }
}

data class EvolutionMetricSeries(
    val metric: EvolutionMetric,
    val granularity: EvolutionSeriesGranularity,
    val points: List<EvolutionSeriesPoint> = emptyList(),
) {
    init {
        require(points.zipWithNext().all { (first, second) ->
            when {
                second.startedAt.isAfter(first.startedAt) -> true
                second.startedAt.isBefore(first.startedAt) -> false
                granularity != EvolutionSeriesGranularity.ACTIVITY -> true
                else -> first.activityId != null && second.activityId != null &&
                    first.activityId.value.toString() <= second.activityId.value.toString()
            }
        })
        require(
            granularity != EvolutionSeriesGranularity.ACTIVITY || points.all { it.activityId != null },
        )
    }

    val recordedPointCount: Int
        get() = points.count { it.state == EvolutionSeriesPointState.PRESENT }

    val gapCount: Int
        get() = points.count { it.state == EvolutionSeriesPointState.GAP }

    val absenceCount: Int
        get() = points.count {
            it.state == EvolutionSeriesPointState.ABSENT ||
                it.state == EvolutionSeriesPointState.NOT_APPLICABLE
        }
}

data class EvolutionTrendsSnapshot(
    val series: List<EvolutionMetricSeries> = emptyList(),
) {
    init {
        require(series.map(EvolutionMetricSeries::metric).distinct().size == series.size)
    }

    fun forMetric(metric: EvolutionMetric): EvolutionMetricSeries? = series.firstOrNull { it.metric == metric }
}

/** Fato local normalizado, sem entities Room e sem rederivação de distância. */
data class EvolutionActivitySnapshot(
    val activityId: WorkoutActivityId,
    val workoutType: WorkoutType,
    val startedAt: Instant,
    val duration: Duration?,
    /** Distância final persistida; nulo mantém ausência ou inaplicabilidade. */
    val distanceMeters: Double? = null,
    /** Passos positivos disponíveis; bicicleta permanece nula por semântica. */
    val steps: Long? = null,
    val averageBpm: Int? = null,
    /** Quantidade de amostras que sustenta [averageBpm], quando disponível. */
    val averageBpmSampleCount: Int? = null,
    val cadenceStepsPerMinute: Double? = null,
    val paceSecondsPerKilometer: Double? = null,
    val speedMetersPerSecond: Double? = null,
) {
    init {
        require(workoutType != WorkoutType.UNKNOWN)
        require(duration == null || !duration.isNegative && !duration.isZero)
        require(distanceMeters == null || (distanceMeters.isFinite() && distanceMeters > 0.0))
        require(steps == null || steps > 0L)
        require(averageBpm == null || averageBpm in 30..220)
        require(averageBpmSampleCount == null || averageBpmSampleCount > 0)
        require(averageBpm == null || averageBpmSampleCount != 0)
        require(
            cadenceStepsPerMinute == null ||
                (cadenceStepsPerMinute.isFinite() && cadenceStepsPerMinute > 0.0),
        )
        require(
            paceSecondsPerKilometer == null ||
                (paceSecondsPerKilometer.isFinite() && paceSecondsPerKilometer > 0.0),
        )
        require(
            speedMetersPerSecond == null ||
                (speedMetersPerSecond.isFinite() && speedMetersPerSecond > 0.0),
        )
        if (workoutType == WorkoutType.CYCLING) {
            require(steps == null)
            require(cadenceStepsPerMinute == null)
            require(paceSecondsPerKilometer == null)
        }
    }

    fun valueFor(metric: EvolutionMetric): Double? = when (metric) {
        EvolutionMetric.ACTIVITY_COUNT -> 1.0
        EvolutionMetric.DISTANCE -> distanceMeters
        EvolutionMetric.DURATION -> duration?.let {
            it.seconds.toDouble() + it.nano.toDouble() / 1_000_000_000.0
        }
        EvolutionMetric.STEPS -> steps?.toDouble()
        EvolutionMetric.ACTIVE_DAYS -> null
        EvolutionMetric.AVERAGE_BPM -> averageBpm?.toDouble()
        EvolutionMetric.CADENCE -> cadenceStepsPerMinute
        EvolutionMetric.PACE -> paceSecondsPerKilometer
        EvolutionMetric.SPEED -> speedMetersPerSecond
    }
}

/** Snapshot factual que alimenta agregadores posteriores; sempre recalculável. */
data class EvolutionReadSnapshot(
    val query: EvolutionQuery,
    val activities: List<EvolutionActivitySnapshot>,
) {
    init {
        require(activities.distinctBy(EvolutionActivitySnapshot::activityId).size == activities.size)
        require(activities.all { activity ->
            activity.startedAt.isIn(query.currentRange) && query.filter.accepts(activity.workoutType)
        })
        require(activities.zipWithNext().all { (first, second) ->
            first.startedAt.isBefore(second.startedAt) ||
                first.startedAt == second.startedAt &&
                first.activityId.value.toString() <= second.activityId.value.toString()
        })
    }

    val activityCount: Int
        get() = activities.size
}

data class EvolutionVolumeSnapshot(
    val query: EvolutionQuery,
    val activityCount: Int,
    val totalDistanceMeters: Double? = null,
    val totalDuration: Duration? = null,
    val totalSteps: Long? = null,
    val activeDayCount: Int = 0,
    val distanceActivityCount: Int = 0,
    val durationActivityCount: Int = 0,
    val stepsActivityCount: Int = 0,
    /** Séries diárias de volume; lacunas não representam zero. */
    val series: List<EvolutionMetricSeries> = emptyList(),
) {
    init {
        require(activityCount >= 0)
        require(activeDayCount in 0..activityCount)
        require(distanceActivityCount in 0..activityCount)
        require(durationActivityCount in 0..activityCount)
        require(stepsActivityCount in 0..activityCount)
        require((totalDistanceMeters == null) == (distanceActivityCount == 0))
        require(totalDistanceMeters == null || (totalDistanceMeters.isFinite() && totalDistanceMeters > 0.0))
        require((totalDuration == null) == (durationActivityCount == 0))
        require(totalDuration == null || (!totalDuration.isNegative && !totalDuration.isZero))
        require((totalSteps == null) == (stepsActivityCount == 0))
        require(totalSteps == null || totalSteps > 0L)
        require(series.map(EvolutionMetricSeries::metric).distinct().size == series.size)
        require(series.all { it.metric in EVOLUTION_VOLUME_METRICS })
        require(series.all { it.granularity == EvolutionSeriesGranularity.DAY })
        require(series.all { it.points.size == query.period.windowDays.toInt() })
        require(series.all { points ->
            points.points.map { it.startedAt } == query.currentWindow.dateRange.dates.map {
                it.atStartOfDay(query.zoneId).toInstant()
            }
        })
    }

    val volumeSeries: List<EvolutionMetricSeries>
        get() = series
}

private val EVOLUTION_VOLUME_METRICS: Set<EvolutionMetric> = setOf(
    EvolutionMetric.ACTIVITY_COUNT,
    EvolutionMetric.DISTANCE,
    EvolutionMetric.DURATION,
    EvolutionMetric.STEPS,
    EvolutionMetric.ACTIVE_DAYS,
)

enum class EvolutionComparisonDirection {
    INCREASED,
    DECREASED,
    STABLE,
    UNAVAILABLE,
}

data class EvolutionMetricComparison(
    val metric: EvolutionMetric,
    val currentValue: Double?,
    val previousValue: Double?,
    val absoluteDifference: Double? = null,
    val percentageDifference: Double? = null,
    val direction: EvolutionComparisonDirection = EvolutionComparisonDirection.UNAVAILABLE,
) {
    private companion object {
        const val COMPARISON_TOLERANCE = 1e-9
    }

    init {
        require(currentValue?.isFinite() != false)
        require(previousValue?.isFinite() != false)
        require(absoluteDifference?.isFinite() != false)
        require(percentageDifference?.isFinite() != false)
        require(
            absoluteDifference == null || currentValue != null && previousValue != null,
        )
        require(
            percentageDifference == null || currentValue != null && previousValue != null && previousValue != 0.0,
        )
        require(
            direction == EvolutionComparisonDirection.UNAVAILABLE ||
                currentValue != null && previousValue != null,
        )
        if (currentValue != null && previousValue != null) {
            absoluteDifference?.let {
                require(it.isCloseTo(kotlin.math.abs(currentValue - previousValue)))
            }
            percentageDifference?.let {
                require(it.isCloseTo((currentValue - previousValue) / previousValue * 100.0))
            }
            val expectedDirection = when {
                currentValue > previousValue -> EvolutionComparisonDirection.INCREASED
                currentValue < previousValue -> EvolutionComparisonDirection.DECREASED
                else -> EvolutionComparisonDirection.STABLE
            }
            require(direction == EvolutionComparisonDirection.UNAVAILABLE || direction == expectedDirection)
        }
    }

    private fun Double.isCloseTo(expected: Double): Boolean {
        val scale = maxOf(1.0, kotlin.math.abs(this), kotlin.math.abs(expected))
        return kotlin.math.abs(this - expected) <= COMPARISON_TOLERANCE * scale
    }
}

data class EvolutionPeriodComparison(
    val current: EvolutionVolumeSnapshot,
    val previous: EvolutionVolumeSnapshot,
    val metrics: List<EvolutionMetricComparison> = emptyList(),
) {
    init {
        require(current.query.period == previous.query.period)
        require(current.query.zoneId == previous.query.zoneId)
        require(current.query.filter == previous.query.filter)
        require(previous.query == current.query.previousQuery())
        require(metrics.map(EvolutionMetricComparison::metric).distinct().size == metrics.size)
    }
}

/** Recorde sempre carrega modalidade; o valor usa unidade da métrica. */
enum class PersonalRecordMetric {
    LONGEST_DISTANCE,
    LONGEST_DURATION,
    MOST_STEPS,
}

data class PersonalRecord(
    val metric: PersonalRecordMetric,
    val workoutType: WorkoutType,
    val activityId: WorkoutActivityId,
    val value: Double,
    val achievedAt: Instant,
) {
    init {
        require(workoutType != WorkoutType.UNKNOWN)
        require(value.isFinite() && value > 0.0)
        val typeIsValidForMetric = when (metric) {
            PersonalRecordMetric.LONGEST_DISTANCE -> workoutType in setOf(
                WorkoutType.RUNNING,
                WorkoutType.WALKING,
                WorkoutType.TREADMILL,
                WorkoutType.CYCLING,
            )

            PersonalRecordMetric.LONGEST_DURATION -> true
            PersonalRecordMetric.MOST_STEPS -> workoutType in setOf(
                WorkoutType.RUNNING,
                WorkoutType.WALKING,
                WorkoutType.TREADMILL,
                WorkoutType.FREE_WORKOUT,
            )
        }
        require(typeIsValidForMetric)
    }
}

data class PersonalRecordsSnapshot(
    val records: List<PersonalRecord> = emptyList(),
) {
    init {
        require(records.distinctBy { it.metric to it.workoutType }.size == records.size)
    }

    fun find(metric: PersonalRecordMetric, workoutType: WorkoutType): PersonalRecord? =
        records.firstOrNull { it.metric == metric && it.workoutType == workoutType }
}

/** Snapshot completo que será montado pelos agregadores das subtarefas seguintes. */
data class EvolutionSnapshot(
    val query: EvolutionQuery,
    val volume: EvolutionVolumeSnapshot,
    val trends: EvolutionTrendsSnapshot = EvolutionTrendsSnapshot(),
    val comparison: EvolutionPeriodComparison? = null,
    val personalRecords: PersonalRecordsSnapshot = PersonalRecordsSnapshot(),
) {
    init {
        require(volume.query == query)
        comparison?.let { require(it.current.query == query) }
    }
}

enum class EvolutionEmptyReason {
    NO_ELIGIBLE_ACTIVITIES,
}

enum class EvolutionReadFailure {
    SOURCE_UNAVAILABLE,
    INVALID_LOCAL_DATA,
}

sealed interface EvolutionReadState {
    val query: EvolutionQuery

    data class Loading(override val query: EvolutionQuery) : EvolutionReadState

    data class Empty(
        override val query: EvolutionQuery,
        val reason: EvolutionEmptyReason = EvolutionEmptyReason.NO_ELIGIBLE_ACTIVITIES,
    ) : EvolutionReadState

    data class Ready(val snapshot: EvolutionReadSnapshot) : EvolutionReadState {
        override val query: EvolutionQuery
            get() = snapshot.query
    }

    data class Failed(
        override val query: EvolutionQuery,
        val reason: EvolutionReadFailure,
        val retryable: Boolean = true,
    ) : EvolutionReadState
}

/** Leitura local read-only; não inicia importação, sync ou leitura do Health Connect. */
interface EvolutionReadModel {
    fun observeSnapshot(query: EvolutionQuery): Flow<EvolutionReadState>
}

private fun Instant.isIn(range: CalendarInstantRange): Boolean =
    !isBefore(range.fromInclusive) && isBefore(range.untilExclusive)
