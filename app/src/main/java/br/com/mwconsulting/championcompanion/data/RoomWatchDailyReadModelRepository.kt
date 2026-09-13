package br.com.mwconsulting.championcompanion.data

import br.com.mwconsulting.championcompanion.core.common.IoDispatcher
import br.com.mwconsulting.championcompanion.core.model.CalendarDateRange
import br.com.mwconsulting.championcompanion.core.model.CalendarInstantRange
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.DailySummaryRepository
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.WatchDailyD1Repository
import br.com.mwconsulting.championcompanion.core.model.WatchDailyReadIssue
import br.com.mwconsulting.championcompanion.core.model.WatchDailyReadModel
import br.com.mwconsulting.championcompanion.core.model.WatchDailyReadModelComposer
import br.com.mwconsulting.championcompanion.core.model.WatchDailyReadModelRepository
import br.com.mwconsulting.championcompanion.core.model.WatchDailyWorkout
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Adapta as fontes Room existentes ao read model diário. A política de
 * autoridade fica em [WatchDailyReadModelComposer], não neste adapter.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class RoomWatchDailyReadModelRepository @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val watchDailyD1Repository: WatchDailyD1Repository,
    private val workoutActivityRepository: WorkoutActivityRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : WatchDailyReadModelRepository {

    override fun observeDay(localDate: LocalDate, zoneId: ZoneId): Flow<WatchDailyReadModel> =
        observePeriod(CalendarDateRange(localDate, localDate.plusDays(1)), zoneId)
            .map { it.singleOrNull() ?: WatchDailyReadModel.empty(localDate, zoneId) }

    override fun observePeriod(
        range: CalendarDateRange,
        zoneId: ZoneId,
    ): Flow<List<WatchDailyReadModel>> {
        val instantRange = range.atZone(zoneId)
        val summariesSource = dailySummaryRepository.observeSummariesBetween(range).asReadSource(
            fallback = emptyList(),
            issue = WatchDailyReadIssue.DAILY_SUMMARY_SOURCE_UNAVAILABLE,
        )
        val workoutsSource = observeWorkoutReads(instantRange).asReadSource(
            fallback = emptyList(),
            issue = WatchDailyReadIssue.WORKOUT_SOURCE_UNAVAILABLE,
        )

        return deviceRepository.observeAssociatedDevice()
            .asReadSource(
                fallback = null,
                issue = WatchDailyReadIssue.ASSOCIATED_DEVICE_SOURCE_UNAVAILABLE,
            )
            .flatMapLatest { associatedDeviceSource ->
                val d1Source = associatedDeviceSource.value?.let { device ->
                    watchDailyD1Repository.observeResolvedAtBetween(
                        deviceId = device.id,
                        range = instantRange,
                    ).asReadSource(
                        fallback = emptyList(),
                        issue = WatchDailyReadIssue.D1_SOURCE_UNAVAILABLE,
                    )
                } ?: flowOf(ReadSource(emptyList()))

                combine(summariesSource, d1Source, workoutsSource) { summaries, d1Samples, workouts ->
                    val sourceIssues = associatedDeviceSource.issues +
                        summaries.issues + d1Samples.issues + workouts.issues
                    range.dates.map { localDate ->
                        WatchDailyReadModelComposer.compose(
                            localDate = localDate,
                            zoneId = zoneId,
                            associatedDeviceId = associatedDeviceSource.value?.id,
                            summaries = summaries.value,
                            d1Samples = d1Samples.value,
                            workouts = workouts.value,
                            issues = sourceIssues,
                        )
                    }
                }
            }
            .flowOn(ioDispatcher)
    }

    private fun observeWorkoutReads(range: CalendarInstantRange): Flow<List<WatchDailyWorkout>> =
        workoutActivityRepository.observeActivitiesIntersecting(range).flatMapLatest { summaries ->
            val watchSummaries = summaries
                .filter { it.origin == DataOrigin.WATCH_D3_D4 }
                .distinctBy { it.activityId }
            if (watchSummaries.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(watchSummaries.map { summary ->
                    workoutActivityRepository.observeActivity(summary.activityId)
                        .map { activity -> WatchDailyWorkout.from(summary, activity) }
                        .catch { error ->
                            if (error is CancellationException) throw error
                            emit(WatchDailyWorkout.from(summary, activity = null))
                        }
                }) { reads -> reads.toList() }
            }
        }
}

private data class ReadSource<T>(
    val value: T,
    val issues: Set<WatchDailyReadIssue> = emptySet(),
)

private fun <T> Flow<T>.asReadSource(
    fallback: T,
    issue: WatchDailyReadIssue,
): Flow<ReadSource<T>> = map { value -> ReadSource(value) }
    .catch { error ->
        if (error is CancellationException) throw error
        emit(ReadSource(value = fallback, issues = setOf(issue)))
    }
