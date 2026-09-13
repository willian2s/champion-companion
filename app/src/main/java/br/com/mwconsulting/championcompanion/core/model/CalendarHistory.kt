package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Intervalo de datas civis com semântica obrigatória [fromInclusive, untilExclusive). */
data class CalendarDateRange(
    val fromInclusive: LocalDate,
    val untilExclusive: LocalDate,
) {
    init {
        require(fromInclusive.isBefore(untilExclusive))
    }

    fun atZone(zoneId: ZoneId): CalendarInstantRange = CalendarInstantRange(
        fromInclusive = fromInclusive.atStartOfDay(zoneId).toInstant(),
        untilExclusive = untilExclusive.atStartOfDay(zoneId).toInstant(),
        zoneId = zoneId,
    )

    val dates: List<LocalDate>
        get() = generateSequence(fromInclusive) { it.plusDays(1) }
            .takeWhile { it.isBefore(untilExclusive) }
            .toList()

    operator fun contains(localDate: LocalDate): Boolean =
        !localDate.isBefore(fromInclusive) && localDate.isBefore(untilExclusive)
}

/** Intervalo de instantes com semântica obrigatória [fromInclusive, untilExclusive). */
data class CalendarInstantRange(
    val fromInclusive: Instant,
    val untilExclusive: Instant,
    val zoneId: ZoneId,
) {
    init {
        require(fromInclusive.isBefore(untilExclusive))
    }
}

data class CalendarMonthQuery(
    val month: YearMonth,
    val zoneId: ZoneId,
) {
    val dateRange: CalendarDateRange
        get() = CalendarDateRange(
            fromInclusive = month.atDay(1),
            untilExclusive = month.plusMonths(1).atDay(1),
        )
}

data class CalendarDayQuery(
    val localDate: LocalDate,
    val zoneId: ZoneId,
) {
    val dateRange: CalendarDateRange
        get() = CalendarDateRange(localDate, localDate.plusDays(1))

    val instantRange: CalendarInstantRange
        get() = dateRange.atZone(zoneId)
}

enum class CalendarHistoryPeriod(val windowDays: Long) {
    LAST_7_DAYS(7),
    LAST_30_DAYS(30),
    LAST_90_DAYS(90),
    LAST_365_DAYS(365),
}

/**
 * Consulta histórica terminada em [untilExclusive]. [zoneId] permanece congelado
 * durante toda formação do snapshot. Quando informado, [dateRangeOverride]
 * substitui a janela derivada de [period] por um intervalo civil explícito.
 */
data class CalendarHistoryQuery(
    val period: CalendarHistoryPeriod,
    val untilExclusive: LocalDate,
    val zoneId: ZoneId,
    val dateRangeOverride: CalendarDateRange? = null,
) {
    val fromInclusive: LocalDate
        get() = dateRange.fromInclusive

    val dateRange: CalendarDateRange
        get() = dateRangeOverride ?: CalendarDateRange(
            fromInclusive = untilExclusive.minusDays(period.windowDays),
            untilExclusive = untilExclusive,
        )

    val instantRange: CalendarInstantRange
        get() = dateRange.atZone(zoneId)
}

enum class CalendarDataSource {
    ASSOCIATED_DEVICE,
    DAILY_SUMMARY,
    HEART_RATE,
    WORKOUT_ACTIVITY,
    WEIGHT_MEASUREMENT,
    HEALTH_SYNC_QUEUE,
    PREFERENCES,
}

enum class CalendarDataIssueReason {
    SOURCE_UNAVAILABLE,
    INVALID_LOCAL_DATA,
    UNRESOLVED_AGGREGATE,
}

/** Falha segura de fonte; não carrega payload, identificador ou valor de saúde. */
data class CalendarDataIssue(
    val source: CalendarDataSource,
    val reason: CalendarDataIssueReason,
)

data class CalendarDayPresence(
    val localDate: LocalDate,
    /** `true` indica resumo válido presente, inclusive quando passos são zero. */
    val hasSteps: Boolean = false,
    val hasHeartRate: Boolean = false,
    val workoutCount: Int = 0,
    val hasWeight: Boolean = false,
    val hasSyncPending: Boolean = false,
    val hasSyncFailure: Boolean = false,
) {
    init {
        require(workoutCount >= 0)
    }
}

data class CalendarMonthSnapshot(
    val query: CalendarMonthQuery,
    /** Presença é esparsa: datas sem qualquer dado não precisam ser emitidas. */
    val days: List<CalendarDayPresence> = emptyList(),
    val issues: Set<CalendarDataIssue> = emptySet(),
) {
    init {
        require(days.isSortedAndUniqueBy { it.localDate })
        require(days.all { it.localDate in query.dateRange })
    }
}

/** Resumo já selecionado pela política de dispositivo; nunca soma dispositivos. */
data class CalendarDailySummary(
    val summaryId: DailySummaryId,
    val deviceId: DeviceId?,
    val origin: DataOrigin,
    /** Data estável vinda de DailySummary.key.localDate; não deriva de timestamps. */
    val localDate: LocalDate,
    val isFromAssociatedDevice: Boolean,
    val steps: Long,
    /** `null` significa ausência de estimativa; zero persistido permanece zero. */
    val estimatedDistanceMeters: Double?,
    val estimatedCaloriesKilocalories: Double?,
    val collectedAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(!isFromAssociatedDevice || deviceId != null)
        require(steps >= 0)
        require(estimatedDistanceMeters == null || (estimatedDistanceMeters.isFinite() && estimatedDistanceMeters >= 0.0))
        require(
            estimatedCaloriesKilocalories == null ||
                (estimatedCaloriesKilocalories.isFinite() && estimatedCaloriesKilocalories >= 0.0),
        )
        require(!updatedAt.isBefore(collectedAt))
    }
}

val CALENDAR_HEART_RATE_SOURCES: Set<HeartRateSource> = setOf(
    HeartRateSource.MANUAL_MEASUREMENT,
    HeartRateSource.WATCH_D1_HISTORY,
)

data class CalendarHeartRateMeasurement(
    val measurementId: HeartRateMeasurementId,
    val bpm: Int,
    /** Data civil deriva deste instante no ZoneId congelado do snapshot. */
    val measuredAt: Instant,
    val source: HeartRateSource,
) {
    init {
        require(source in CALENDAR_HEART_RATE_SOURCES)
        require(bpm in 30..220)
    }
}

/** Agregado exclusivo de BPM manual e histórico D1 válido. */
data class CalendarHeartRateSummary(
    val measurementCount: Int,
    val averageBpm: Double,
    val minimumBpm: Int,
    val maximumBpm: Int,
    val latestMeasurement: CalendarHeartRateMeasurement,
) {
    init {
        require(measurementCount > 0)
        require(averageBpm.isFinite() && averageBpm in minimumBpm.toDouble()..maximumBpm.toDouble())
        require(minimumBpm in 30..220)
        require(maximumBpm in minimumBpm..220)
        require(latestMeasurement.bpm in minimumBpm..maximumBpm)
    }
}

enum class CalendarSyncState {
    NO_OPERATIONS,
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
    PARTIAL,
}

/**
 * Estado derivado somente de operações locais. Horários de sync nunca definem
 * data do agregado.
 */
data class CalendarSyncSummary(
    val pendingCount: Int = 0,
    val syncingCount: Int = 0,
    val failedCount: Int = 0,
    val syncedCount: Int = 0,
) {
    init {
        require(pendingCount >= 0 && syncingCount >= 0 && failedCount >= 0 && syncedCount >= 0)
    }

    val state: CalendarSyncState
        get() = when {
            operationCount == 0 -> CalendarSyncState.NO_OPERATIONS
            syncedCount > 0 && pendingCount + syncingCount + failedCount > 0 -> CalendarSyncState.PARTIAL
            failedCount > 0 -> CalendarSyncState.FAILED
            syncingCount > 0 -> CalendarSyncState.SYNCING
            pendingCount > 0 -> CalendarSyncState.PENDING
            else -> CalendarSyncState.SYNCED
        }

    val operationCount: Int
        get() = pendingCount + syncingCount + failedCount + syncedCount
}

/**
 * Projeção leve da atividade. [startedAt] define dia civil. Passos não são
 * expostos, e [distanceMeters] nunca participa dos totais diários. Para
 * `WATCH_D3_D4`, distância vem da série canônica, exceto para `CYCLING`, que
 * usa somente entrada manual válida.
 */
data class CalendarWorkoutSummary(
    val activityId: WorkoutActivityId,
    val origin: DataOrigin,
    val title: String?,
    val suggestedType: WorkoutType,
    val confirmedType: WorkoutType?,
    val startedAt: Instant,
    val duration: Duration,
    /** Distância funcional: manual para ciclismo, canônica para outros treinos de relógio. */
    val distanceMeters: DomainValue<Double>,
    val caloriesKilocalories: DomainValue<Double>,
    val averageBpm: DomainValue<Int>,
    val workflowState: ActivityWorkflowState,
    val syncSummary: CalendarSyncSummary = CalendarSyncSummary(),
    val canonicalReadStatus: WorkoutCanonicalReadStatus = WorkoutCanonicalReadStatus.NOT_APPLICABLE,
    val derivationVersion: String? = null,
    val stepLengthSource: StepLengthSource? = null,
    val plausibilityIndicators: Set<WorkoutPlausibilityIndicator> = emptySet(),
    /** Instante de término congelado na atividade, quando disponível. */
    val endedAt: Instant? = null,
) {
    init {
        require(!duration.isNegative)
        require(endedAt == null || !endedAt.isBefore(startedAt))
        require(workflowState != ActivityWorkflowState.DISCARDED)
    }
}

data class CalendarWeightMeasurementSummary(
    val measurementId: WeightMeasurementId,
    val value: Double,
    val unit: WeightUnit,
    /** Data civil deriva deste instante no ZoneId congelado do snapshot. */
    val measuredAt: Instant,
    val origin: DataOrigin,
    val syncSummary: CalendarSyncSummary = CalendarSyncSummary(),
) {
    init {
        require(value.isFinite() && value > 0.0)
    }
}

data class CalendarDaySummary(
    val localDate: LocalDate,
    val zoneId: ZoneId,
    /** `null` significa ausência; resumo presente com `steps == 0` é dado real. */
    val dailySummary: CalendarDailySummary? = null,
    /** Meta vigente no momento do snapshot, exibida como "Meta atual". */
    val stepsGoal: Int,
    val heartRateSummary: CalendarHeartRateSummary? = null,
    /** Atividades não descartadas pertencentes ao dia de startedAt. */
    val workouts: List<CalendarWorkoutSummary> = emptyList(),
    /** Medidas ordenadas da mais recente para a mais antiga. */
    val weightMeasurements: List<CalendarWeightMeasurementSummary> = emptyList(),
    val syncSummary: CalendarSyncSummary = CalendarSyncSummary(),
    val issues: Set<CalendarDataIssue> = emptySet(),
) {
    init {
        require(stepsGoal > 0)
        require(dailySummary == null || dailySummary.localDate == localDate)
        require(
            heartRateSummary == null ||
                heartRateSummary.latestMeasurement.measuredAt.toLocalDateIn(zoneId) == localDate,
        )
        require(workouts.all { it.startedAt.toLocalDateIn(zoneId) == localDate })
        require(weightMeasurements.all { it.measuredAt.toLocalDateIn(zoneId) == localDate })
        require(
            weightMeasurements.zipWithNext().all { (newer, older) ->
                !newer.measuredAt.isBefore(older.measuredAt)
            },
        )
    }
}

data class CalendarStepsHistoryEntry(
    val localDate: LocalDate,
    val summaryId: DailySummaryId? = null,
    /** `null` representa dia sem resumo; zero representa registro real. */
    val steps: Long? = null,
    val origin: DataOrigin? = null,
    val updatedAt: Instant? = null,
) {
    init {
        val hasSummary = summaryId != null
        require(hasSummary == (steps != null))
        require(hasSummary == (origin != null))
        require(hasSummary == (updatedAt != null))
        require(steps == null || steps >= 0)
    }
}

data class CalendarStepsHistoryAggregates(
    /** `null` preserva ausência total; zero exige pelo menos um registro real. */
    val totalSteps: Long? = null,
    val averageStepsPerRecordedDay: Double? = null,
    val maximumSteps: Long? = null,
    val recordedDayCount: Int = 0,
    val missingDayCount: Int = 0,
    val currentGoalReachedDayCount: Int = 0,
) {
    init {
        require(recordedDayCount >= 0 && missingDayCount >= 0 && currentGoalReachedDayCount >= 0)
        require(currentGoalReachedDayCount <= recordedDayCount)
        require((recordedDayCount == 0) == (totalSteps == null))
        require((recordedDayCount == 0) == (averageStepsPerRecordedDay == null))
        require((recordedDayCount == 0) == (maximumSteps == null))
        require(totalSteps == null || totalSteps >= 0)
        require(
            averageStepsPerRecordedDay == null ||
                (averageStepsPerRecordedDay.isFinite() && averageStepsPerRecordedDay >= 0.0),
        )
        require(maximumSteps == null || maximumSteps >= 0)
    }
}

data class CalendarStepsHistorySnapshot(
    val query: CalendarHistoryQuery,
    /** Meta vigente no momento do snapshot, nunca meta histórica. */
    val stepsGoal: Int,
    val entries: List<CalendarStepsHistoryEntry>,
    val issues: Set<CalendarDataIssue> = emptySet(),
) {
    init {
        require(stepsGoal > 0)
        require(entries.isSortedAndUniqueBy { it.localDate })
        require(entries.size == query.dateRange.dates.size)
        require(entries.map { it.localDate } == query.dateRange.dates)
        aggregates
    }

    val aggregates: CalendarStepsHistoryAggregates
        get() {
            val values = entries.mapNotNull { it.steps }
            return CalendarStepsHistoryAggregates(
                totalSteps = values.checkedSum().takeIf { values.isNotEmpty() },
                averageStepsPerRecordedDay = values
                    .average()
                    .takeIf { values.isNotEmpty() },
                maximumSteps = values.maxOrNull(),
                recordedDayCount = values.size,
                missingDayCount = entries.size - values.size,
                currentGoalReachedDayCount = values.count { it >= stepsGoal },
            )
        }
}

data class CalendarDistanceHistoryEntry(
    val localDate: LocalDate,
    val summaryId: DailySummaryId? = null,
    /** `null` preserva distância indisponível; zero persistido permanece zero. */
    val estimatedDistanceMeters: Double? = null,
    val origin: DataOrigin? = null,
    val updatedAt: Instant? = null,
) {
    init {
        val hasSummary = summaryId != null
        require(hasSummary == (origin != null))
        require(hasSummary == (updatedAt != null))
        require(estimatedDistanceMeters == null || hasSummary)
        require(
            estimatedDistanceMeters == null ||
                (estimatedDistanceMeters.isFinite() && estimatedDistanceMeters >= 0.0),
        )
    }
}

data class CalendarDistanceHistoryAggregates(
    /** `null` preserva ausência total de estimativas. */
    val totalDistanceMeters: Double? = null,
    val averageDistanceMetersPerDayWithEstimate: Double? = null,
    val maximumDistanceMeters: Double? = null,
    val daysWithEstimateCount: Int = 0,
    val daysWithoutEstimateCount: Int = 0,
) {
    init {
        require(daysWithEstimateCount >= 0 && daysWithoutEstimateCount >= 0)
        require((daysWithEstimateCount == 0) == (totalDistanceMeters == null))
        require((daysWithEstimateCount == 0) == (averageDistanceMetersPerDayWithEstimate == null))
        require((daysWithEstimateCount == 0) == (maximumDistanceMeters == null))
        require(totalDistanceMeters == null || (totalDistanceMeters.isFinite() && totalDistanceMeters >= 0.0))
        require(
            averageDistanceMetersPerDayWithEstimate == null ||
                (averageDistanceMetersPerDayWithEstimate.isFinite() && averageDistanceMetersPerDayWithEstimate >= 0.0),
        )
        require(maximumDistanceMeters == null || (maximumDistanceMeters.isFinite() && maximumDistanceMeters >= 0.0))
    }
}

data class CalendarDistanceHistorySnapshot(
    val query: CalendarHistoryQuery,
    val entries: List<CalendarDistanceHistoryEntry>,
    val issues: Set<CalendarDataIssue> = emptySet(),
) {
    init {
        require(entries.isSortedAndUniqueBy { it.localDate })
        require(entries.size == query.dateRange.dates.size)
        require(entries.map { it.localDate } == query.dateRange.dates)
        aggregates
    }

    val aggregates: CalendarDistanceHistoryAggregates
        get() {
            val values = entries.mapNotNull { it.estimatedDistanceMeters }
            return CalendarDistanceHistoryAggregates(
                totalDistanceMeters = values.checkedFiniteSum().takeIf { values.isNotEmpty() },
                averageDistanceMetersPerDayWithEstimate = values.average().takeIf { values.isNotEmpty() },
                maximumDistanceMeters = values.maxOrNull(),
                daysWithEstimateCount = values.size,
                daysWithoutEstimateCount = query.dateRange.dates.size - values.size,
            )
        }
}

private fun <T> List<T>.isSortedAndUniqueBy(selector: (T) -> LocalDate): Boolean =
    zipWithNext().all { (first, second) -> selector(first).isBefore(selector(second)) }

private fun List<Long>.checkedSum(): Long {
    var total = 0L
    for (value in this) {
        total = try {
            Math.addExact(total, value)
        } catch (_: ArithmeticException) {
            throw IllegalArgumentException("Histórico de passos excede limite numérico")
        }
    }
    return total
}

private fun List<Double>.checkedFiniteSum(): Double {
    var total = 0.0
    for (value in this) {
        total += value
        require(total.isFinite()) { "Histórico de distância excede limite numérico" }
    }
    return total
}
