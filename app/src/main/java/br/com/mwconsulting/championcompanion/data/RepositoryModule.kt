package br.com.mwconsulting.championcompanion.data

import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionRepository
import br.com.mwconsulting.championcompanion.core.model.CalendarRepository
import br.com.mwconsulting.championcompanion.core.model.DailySummaryRepository
import br.com.mwconsulting.championcompanion.core.model.DailyStepsSyncCoordinator
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadRepository
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRepository
import br.com.mwconsulting.championcompanion.core.model.HeartRateRepository
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsRepository
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibrationRepository
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteRepository
import br.com.mwconsulting.championcompanion.core.model.WatchDailyD1Repository
import br.com.mwconsulting.championcompanion.core.model.WatchDailyReadModelRepository
import br.com.mwconsulting.championcompanion.feature.home.ChampionDailyStepsSyncCoordinator
import br.com.mwconsulting.championcompanion.feature.home.ChampionWatchSyncCoordinator
import br.com.mwconsulting.championcompanion.feature.home.WatchSyncCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides fun deviceRepository(source: RoomRepositories): DeviceRepository = source
    @Provides fun dailySummaryRepository(source: RoomRepositories): DailySummaryRepository = source
    @Provides fun dailyStepsSyncCoordinator(source: ChampionDailyStepsSyncCoordinator): DailyStepsSyncCoordinator = source
    @Provides fun watchSyncCoordinator(source: ChampionWatchSyncCoordinator): WatchSyncCoordinator = source
    @Provides fun heartRateRepository(source: RoomRepositories): HeartRateRepository = source
    @Provides fun personalMetricsRepository(source: RoomRepositories): PersonalMetricsRepository = source
    @Provides fun workoutActivityRepository(source: RoomRepositories): WorkoutActivityRepository = source
    @Provides fun treadmillCalibrationRepository(source: RoomRepositories): TreadmillCalibrationRepository = source
    @Provides fun appWorkoutSessionRepository(source: RoomRepositories): AppWorkoutSessionRepository = source
    @Provides fun workoutRouteRepository(source: RoomRepositories): WorkoutRouteRepository = source
    @Provides fun healthSyncRepository(source: RoomRepositories): HealthSyncRepository = source
    @Provides fun calendarRepository(source: LocalCalendarRepository): CalendarRepository = source
    @Provides fun diagnosticPayloadRepository(source: RoomRepositories): DiagnosticPayloadRepository = source
    @Provides fun watchDailyD1Repository(source: RoomRepositories): WatchDailyD1Repository = source
    @Provides fun watchDailyReadModelRepository(source: RoomWatchDailyReadModelRepository): WatchDailyReadModelRepository = source
}
