package br.com.mwconsulting.championcompanion.data

import br.com.mwconsulting.championcompanion.core.model.AppPreferencesRepository
import br.com.mwconsulting.championcompanion.core.model.CALENDAR_HEART_RATE_SOURCES
import br.com.mwconsulting.championcompanion.core.model.CalendarDailySummary
import br.com.mwconsulting.championcompanion.core.model.CalendarDataIssue
import br.com.mwconsulting.championcompanion.core.model.CalendarDataIssueReason
import br.com.mwconsulting.championcompanion.core.model.CalendarDataSource
import br.com.mwconsulting.championcompanion.core.model.CalendarDateRange
import br.com.mwconsulting.championcompanion.core.model.CalendarDayPresence
import br.com.mwconsulting.championcompanion.core.model.CalendarDayQuery
import br.com.mwconsulting.championcompanion.core.model.CalendarDaySummary
import br.com.mwconsulting.championcompanion.core.model.CalendarDistanceHistoryEntry
import br.com.mwconsulting.championcompanion.core.model.CalendarDistanceHistorySnapshot
import br.com.mwconsulting.championcompanion.core.model.CalendarHeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.CalendarHeartRateSummary
import br.com.mwconsulting.championcompanion.core.model.CalendarHistoryQuery
import br.com.mwconsulting.championcompanion.core.model.CalendarMonthQuery
import br.com.mwconsulting.championcompanion.core.model.CalendarMonthSnapshot
import br.com.mwconsulting.championcompanion.core.model.CalendarRepository
import br.com.mwconsulting.championcompanion.core.model.CalendarStepsHistoryEntry
import br.com.mwconsulting.championcompanion.core.model.CalendarStepsHistorySnapshot
import br.com.mwconsulting.championcompanion.core.model.CalendarSyncSummary
import br.com.mwconsulting.championcompanion.core.model.CalendarWeightMeasurementSummary
import br.com.mwconsulting.championcompanion.core.model.CalendarWorkoutSummary
import br.com.mwconsulting.championcompanion.core.model.DEFAULT_DAILY_STEPS_GOAL
import br.com.mwconsulting.championcompanion.core.model.DailySummary
import br.com.mwconsulting.championcompanion.core.model.DailySummaryPolicy
import br.com.mwconsulting.championcompanion.core.model.DailySummaryRepository
import br.com.mwconsulting.championcompanion.core.model.DataValidity
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregate
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperation
import br.com.mwconsulting.championcompanion.core.model.HealthSyncQueueObservation
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRepository
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.HeartRateRepository
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsRepository
import br.com.mwconsulting.championcompanion.core.model.SyncOperationState
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import br.com.mwconsulting.championcompanion.core.model.toLocalDateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class LocalCalendarRepository @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val heartRateRepository: HeartRateRepository,
    private val workoutActivityRepository: WorkoutActivityRepository,
    private val personalMetricsRepository: PersonalMetricsRepository,
    private val healthSyncRepository: HealthSyncRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
) : CalendarRepository {

    override fun observeMonth(query: CalendarMonthQuery): Flow<CalendarMonthSnapshot> =
        observeWindow(query.dateRange, query.zoneId).map { window ->
            val summariesByDate = window.selectedSummaries.associateBy { it.key.localDate }
            val heartRatesByDate = window.heartRates.groupBy { it.measuredAt!!.toLocalDateIn(query.zoneId) }
            val workoutsByDate = window.workouts.groupBy { it.startedAt.toLocalDateIn(query.zoneId) }
            val weightsByDate = window.weights.groupBy { it.measuredAt.toLocalDateIn(query.zoneId) }
            val operationsByDate = window.operations.groupBy { operation ->
                window.aggregateDates.getValue(operation.key.aggregate)
            }

            val daysWithData = buildSet {
                addAll(summariesByDate.keys)
                addAll(heartRatesByDate.keys)
                addAll(workoutsByDate.keys)
                addAll(weightsByDate.keys)
            }

            CalendarMonthSnapshot(
                query = query,
                days = daysWithData.sorted().map { localDate ->
                    val sync = operationsByDate[localDate].toCalendarSyncSummary()
                    CalendarDayPresence(
                        localDate = localDate,
                        hasSteps = summariesByDate.containsKey(localDate),
                        hasHeartRate = heartRatesByDate[localDate].orEmpty().isNotEmpty(),
                        workoutCount = workoutsByDate[localDate].orEmpty().size,
                        hasWeight = weightsByDate[localDate].orEmpty().isNotEmpty(),
                        hasSyncPending = sync.pendingCount > 0 || sync.syncingCount > 0,
                        hasSyncFailure = sync.failedCount > 0,
                    )
                },
                issues = window.issues,
            )
        }

    override fun observeDay(localDate: LocalDate, zoneId: ZoneId): Flow<CalendarDaySummary> {
        val query = CalendarDayQuery(localDate, zoneId)
        return combine(
            observeWindow(query.dateRange, zoneId),
            appPreferencesRepository.observeDailyStepsGoal().asSource(
                source = CalendarDataSource.PREFERENCES,
                fallback = DEFAULT_DAILY_STEPS_GOAL,
            ),
        ) { window, goalSource ->
            val dailySummary = window.selectedSummaries.singleOrNull()?.toCalendarDailySummary(
                associatedDeviceId = window.associatedDeviceId,
            )
            val workouts = window.workouts.map { workout ->
                workout.copy(
                    syncSummary = window.operations
                        .forAggregate(HealthSyncAggregate.WorkoutActivity(workout.activityId))
                        .toCalendarSyncSummary(),
                )
            }
            val weights = window.weights.map { weight ->
                weight.copy(
                    syncSummary = window.operations
                        .forAggregate(HealthSyncAggregate.WeightMeasurement(weight.measurementId))
                        .toCalendarSyncSummary(),
                )
            }
            val goal = DailySummaryPolicy.sanitizeDailyStepsGoal(goalSource.value)
            val preferenceIssues = if (goalSource.value > 0) {
                goalSource.issues
            } else {
                goalSource.issues + invalidDataIssue(CalendarDataSource.PREFERENCES)
            }

            CalendarDaySummary(
                localDate = localDate,
                zoneId = zoneId,
                dailySummary = dailySummary,
                stepsGoal = goal,
                heartRateSummary = window.heartRates.toCalendarHeartRateSummary(),
                workouts = workouts,
                weightMeasurements = weights,
                syncSummary = window.operations.toCalendarSyncSummary(),
                issues = window.issues + preferenceIssues,
            )
        }
    }

    override fun observeStepsHistory(query: CalendarHistoryQuery): Flow<CalendarStepsHistorySnapshot> =
        combine(
            observeSelectedSummaries(query.dateRange),
            appPreferencesRepository.observeDailyStepsGoal().asSource(
                source = CalendarDataSource.PREFERENCES,
                fallback = DEFAULT_DAILY_STEPS_GOAL,
            ),
        ) { selection, goalSource ->
            val summariesByDate = selection.summaries.associateBy { it.key.localDate }
            val goal = DailySummaryPolicy.sanitizeDailyStepsGoal(goalSource.value)
            val preferenceIssues = if (goalSource.value > 0) {
                goalSource.issues
            } else {
                goalSource.issues + invalidDataIssue(CalendarDataSource.PREFERENCES)
            }
            val entries = query.dateRange.dates.map { localDate ->
                val summary = summariesByDate[localDate]
                CalendarStepsHistoryEntry(
                    localDate = localDate,
                    summaryId = summary?.id,
                    steps = summary?.steps,
                    origin = summary?.key?.origin,
                    updatedAt = summary?.updatedAt,
                )
            }

            try {
                CalendarStepsHistorySnapshot(
                    query = query,
                    stepsGoal = goal,
                    entries = entries,
                    issues = selection.issues + preferenceIssues,
                )
            } catch (_: IllegalArgumentException) {
                CalendarStepsHistorySnapshot(
                    query = query,
                    stepsGoal = goal,
                    entries = query.dateRange.dates.map(::CalendarStepsHistoryEntry),
                    issues = selection.issues + preferenceIssues +
                        invalidDataIssue(CalendarDataSource.DAILY_SUMMARY),
                )
            }
        }

    override fun observeDistanceHistory(query: CalendarHistoryQuery): Flow<CalendarDistanceHistorySnapshot> =
        observeSelectedSummaries(query.dateRange).map { selection ->
            val summariesByDate = selection.summaries.associateBy { it.key.localDate }
            val entries = query.dateRange.dates.map { localDate ->
                val summary = summariesByDate[localDate]
                CalendarDistanceHistoryEntry(
                    localDate = localDate,
                    summaryId = summary?.id,
                    estimatedDistanceMeters = summary?.estimatedDistanceMeters,
                    origin = summary?.key?.origin,
                    updatedAt = summary?.updatedAt,
                )
            }

            try {
                CalendarDistanceHistorySnapshot(
                    query = query,
                    entries = entries,
                    issues = selection.issues,
                )
            } catch (_: IllegalArgumentException) {
                CalendarDistanceHistorySnapshot(
                    query = query,
                    entries = query.dateRange.dates.map(::CalendarDistanceHistoryEntry),
                    issues = selection.issues + invalidDataIssue(CalendarDataSource.DAILY_SUMMARY),
                )
            }
        }

    private fun observeSelectedSummaries(range: CalendarDateRange): Flow<SummarySelection> =
        combine(
            deviceRepository.observeAssociatedDevice().map { it?.id }.asSource(
                source = CalendarDataSource.ASSOCIATED_DEVICE,
                fallback = null,
            ),
            dailySummaryRepository.observeSummariesBetween(range).asSource(
                source = CalendarDataSource.DAILY_SUMMARY,
                fallback = emptyList(),
            ),
        ) { associatedSource, summariesSource ->
            val (summaries, validationIssues) = summariesSource.value.validSummariesIn(range)
            SummarySelection(
                summaries = selectSummariesByDate(summaries, associatedSource.value),
                issues = associatedSource.issues + summariesSource.issues + validationIssues,
            )
        }

    private fun observeWindow(range: CalendarDateRange, zoneId: ZoneId): Flow<CalendarWindow> {
        val instantRange = range.atZone(zoneId)
        val localSources = combine(
            deviceRepository.observeAssociatedDevice().map { it?.id }.asSource(
                source = CalendarDataSource.ASSOCIATED_DEVICE,
                fallback = null,
            ),
            dailySummaryRepository.observeSummariesBetween(range).asSource(
                source = CalendarDataSource.DAILY_SUMMARY,
                fallback = emptyList(),
            ),
            heartRateRepository.observeMeasurementsBetween(instantRange).asSource(
                source = CalendarDataSource.HEART_RATE,
                fallback = emptyList(),
            ),
            workoutActivityRepository.observeActivitiesBetween(instantRange).asSource(
                source = CalendarDataSource.WORKOUT_ACTIVITY,
                fallback = emptyList(),
            ),
            personalMetricsRepository.observeWeightsBetween(instantRange).asSource(
                source = CalendarDataSource.WEIGHT_MEASUREMENT,
                fallback = emptyList(),
            ),
        ) { associatedSource, summariesSource, heartRatesSource, workoutsSource, weightsSource ->
            val (summaries, summaryIssues) = summariesSource.value.validSummariesIn(range)
            val (heartRates, heartRateIssues) = heartRatesSource.value.validHeartRatesIn(instantRange)
            val (workouts, workoutIssues) = workoutsSource.value.validWorkoutsIn(instantRange)
            val (weights, weightIssues) = weightsSource.value.validWeightsIn(instantRange)
            CalendarWindow(
                zoneId = zoneId,
                associatedDeviceId = associatedSource.value,
                selectedSummaries = selectSummariesByDate(summaries, associatedSource.value),
                heartRates = heartRates,
                workouts = workouts,
                weights = weights,
                issues = associatedSource.issues +
                    summariesSource.issues +
                    heartRatesSource.issues +
                    workoutsSource.issues +
                    weightsSource.issues +
                    summaryIssues +
                    heartRateIssues +
                    workoutIssues +
                    weightIssues,
            )
        }

        return localSources.flatMapLatest { localWindow ->
            healthSyncRepository.observeForAggregatesWithIntegrity(localWindow.aggregateDates.keys).asSource(
                source = CalendarDataSource.HEALTH_SYNC_QUEUE,
                fallback = HealthSyncQueueObservation(),
            ).map { operationsSource ->
                val expectedAggregates = localWindow.aggregateDates.keys
                val matchingOperations = operationsSource.value.operations
                    .filter { it.key.aggregate in expectedAggregates }
                val operations = matchingOperations
                    .distinctBy { it.id }
                val hasUnresolvedAggregate = operationsSource.value.hasUnresolvedAggregate ||
                    matchingOperations.size != operationsSource.value.operations.size
                val hasInvalidOperation = operationsSource.value.hasInvalidOperation ||
                    operations.size != matchingOperations.size
                localWindow.copy(
                    operations = operations,
                    issues = localWindow.issues +
                        operationsSource.issues +
                        if (hasUnresolvedAggregate) {
                            setOf(
                                CalendarDataIssue(
                                    source = CalendarDataSource.HEALTH_SYNC_QUEUE,
                                    reason = CalendarDataIssueReason.UNRESOLVED_AGGREGATE,
                                ),
                            )
                        } else {
                            emptySet()
                        } +
                        invalidIssueIf(
                            invalid = hasInvalidOperation,
                            source = CalendarDataSource.HEALTH_SYNC_QUEUE,
                        ),
                )
            }
        }
    }
}

private data class ObservedSource<T>(
    val value: T,
    val issues: Set<CalendarDataIssue> = emptySet(),
)

private data class SummarySelection(
    val summaries: List<DailySummary>,
    val issues: Set<CalendarDataIssue>,
)

private data class CalendarWindow(
    val zoneId: ZoneId,
    val associatedDeviceId: DeviceId?,
    val selectedSummaries: List<DailySummary>,
    val heartRates: List<HeartRateMeasurement>,
    val workouts: List<CalendarWorkoutSummary>,
    val weights: List<CalendarWeightMeasurementSummary>,
    val operations: List<HealthSyncOperation> = emptyList(),
    val issues: Set<CalendarDataIssue>,
) {
    val aggregateDates: Map<HealthSyncAggregate, LocalDate>
        get() = buildMap {
            selectedSummaries.forEach { summary ->
                put(HealthSyncAggregate.DailySummary(summary.id), summary.key.localDate)
            }
            heartRates.forEach { measurement ->
                put(
                    HealthSyncAggregate.HeartRateMeasurement(measurement.id),
                    measurement.measuredAt!!.toLocalDateIn(zoneId),
                )
            }
            workouts.forEach { workout ->
                put(
                    HealthSyncAggregate.WorkoutActivity(workout.activityId),
                    workout.startedAt.toLocalDateIn(zoneId),
                )
            }
            weights.forEach { weight ->
                put(
                    HealthSyncAggregate.WeightMeasurement(weight.measurementId),
                    weight.measuredAt.toLocalDateIn(zoneId),
                )
            }
        }
}

private fun <T> Flow<T>.asSource(
    source: CalendarDataSource,
    fallback: T,
): Flow<ObservedSource<T>> = map<T, ObservedSource<T>> { ObservedSource(it) }
    .catch { error ->
        if (error is CancellationException) throw error
        emit(
            ObservedSource(
                value = fallback,
                issues = setOf(
                    CalendarDataIssue(
                        source = source,
                        reason = CalendarDataIssueReason.SOURCE_UNAVAILABLE,
                    ),
                ),
            ),
        )
    }

private fun List<DailySummary>.validSummariesIn(
    range: CalendarDateRange,
): Pair<List<DailySummary>, Set<CalendarDataIssue>> {
    val matching = filter { summary ->
        summary.validity == DataValidity.VALID && summary.key.localDate in range
    }
    val valid = matching
        .sortedWith(
            compareBy<DailySummary> { it.key.localDate }
                .then(dailySummarySelectionComparator),
        )
        .distinctBy { it.id }
    return valid to invalidIssueIf(
        invalid = matching.size != size || valid.size != matching.size,
        source = CalendarDataSource.DAILY_SUMMARY,
    )
}

private fun List<HeartRateMeasurement>.validHeartRatesIn(
    range: br.com.mwconsulting.championcompanion.core.model.CalendarInstantRange,
): Pair<List<HeartRateMeasurement>, Set<CalendarDataIssue>> {
    val valid = filter { measurement ->
        measurement.validity == DataValidity.VALID &&
            measurement.source in CALENDAR_HEART_RATE_SOURCES &&
            measurement.measuredAt?.isIn(range.fromInclusive, range.untilExclusive) == true
    }.sortedWith(
        compareBy<HeartRateMeasurement> { it.measuredAt }
            .thenBy { it.id.value.toString() },
    ).distinctBy { it.id }
    return valid to invalidIssueIf(
        invalid = valid.size != size,
        source = CalendarDataSource.HEART_RATE,
    )
}

private fun List<CalendarWorkoutSummary>.validWorkoutsIn(
    range: br.com.mwconsulting.championcompanion.core.model.CalendarInstantRange,
): Pair<List<CalendarWorkoutSummary>, Set<CalendarDataIssue>> {
    val valid = filter { workout ->
        workout.startedAt.isIn(range.fromInclusive, range.untilExclusive)
    }.sortedWith(
        compareByDescending<CalendarWorkoutSummary> { it.startedAt }
            .thenBy { it.activityId.value.toString() },
    ).distinctBy { it.activityId }
    return valid to invalidIssueIf(
        invalid = valid.size != size,
        source = CalendarDataSource.WORKOUT_ACTIVITY,
    )
}

private fun List<CalendarWeightMeasurementSummary>.validWeightsIn(
    range: br.com.mwconsulting.championcompanion.core.model.CalendarInstantRange,
): Pair<List<CalendarWeightMeasurementSummary>, Set<CalendarDataIssue>> {
    val valid = filter { weight ->
        weight.measuredAt.isIn(range.fromInclusive, range.untilExclusive)
    }.sortedWith(
        compareByDescending<CalendarWeightMeasurementSummary> { it.measuredAt }
            .thenBy { it.measurementId.value.toString() },
    ).distinctBy { it.measurementId }
    return valid to invalidIssueIf(
        invalid = valid.size != size,
        source = CalendarDataSource.WEIGHT_MEASUREMENT,
    )
}

private fun selectSummariesByDate(
    summaries: List<DailySummary>,
    associatedDeviceId: DeviceId?,
): List<DailySummary> = summaries
    .groupBy { it.key.localDate }
    .toSortedMap()
    .mapNotNull { (_, candidates) ->
        val associatedCandidates = associatedDeviceId?.let { deviceId ->
            candidates.filter { it.key.deviceId == deviceId }
        }.orEmpty()
        (associatedCandidates.ifEmpty { candidates }).minWithOrNull(dailySummarySelectionComparator)
    }

private val dailySummarySelectionComparator =
    compareByDescending<DailySummary> { it.updatedAt }
        .thenByDescending { it.collectedAt }
        .thenBy { it.id.value.toString() }

private fun DailySummary.toCalendarDailySummary(
    associatedDeviceId: DeviceId?,
): CalendarDailySummary = CalendarDailySummary(
    summaryId = id,
    deviceId = key.deviceId,
    origin = key.origin,
    localDate = key.localDate,
    isFromAssociatedDevice = associatedDeviceId != null && key.deviceId == associatedDeviceId,
    steps = steps,
    estimatedDistanceMeters = estimatedDistanceMeters,
    estimatedCaloriesKilocalories = estimatedCaloriesKilocalories,
    collectedAt = collectedAt,
    updatedAt = updatedAt,
)

private fun List<HeartRateMeasurement>.toCalendarHeartRateSummary(): CalendarHeartRateSummary? {
    if (isEmpty()) return null
    val measurements = map { measurement ->
        CalendarHeartRateMeasurement(
            measurementId = measurement.id,
            bpm = measurement.bpm,
            measuredAt = requireNotNull(measurement.measuredAt),
            source = measurement.source,
        )
    }
    val latest = measurements.maxWith(
        compareBy<CalendarHeartRateMeasurement> { it.measuredAt }
            .thenBy { it.measurementId.value.toString() },
    )
    return CalendarHeartRateSummary(
        measurementCount = measurements.size,
        averageBpm = measurements.map { it.bpm }.average(),
        minimumBpm = measurements.minOf { it.bpm },
        maximumBpm = measurements.maxOf { it.bpm },
        latestMeasurement = latest,
    )
}

private fun List<HealthSyncOperation>?.toCalendarSyncSummary(): CalendarSyncSummary {
    val operations = orEmpty().distinctBy { it.id }
    return CalendarSyncSummary(
        pendingCount = operations.count { it.state == SyncOperationState.PENDING },
        syncingCount = operations.count { it.state == SyncOperationState.SYNCING },
        failedCount = operations.count { it.state == SyncOperationState.FAILED },
        syncedCount = operations.count { it.state == SyncOperationState.SYNCED },
    )
}

private fun List<HealthSyncOperation>.forAggregate(
    aggregate: HealthSyncAggregate,
): List<HealthSyncOperation> = filter { it.key.aggregate == aggregate }

private fun Instant.isIn(fromInclusive: Instant, untilExclusive: Instant): Boolean =
    !isBefore(fromInclusive) && isBefore(untilExclusive)

private fun invalidIssueIf(
    invalid: Boolean,
    source: CalendarDataSource,
): Set<CalendarDataIssue> =
    if (invalid) setOf(invalidDataIssue(source)) else emptySet()

private fun invalidDataIssue(source: CalendarDataSource): CalendarDataIssue =
    CalendarDataIssue(
        source = source,
        reason = CalendarDataIssueReason.INVALID_LOCAL_DATA,
    )
