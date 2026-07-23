package br.com.mwconsulting.championcompanion.feature.exercises

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.common.BleDispatcher
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationGate
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import br.com.mwconsulting.championcompanion.feature.device.DeviceConnectionBootstrap
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
object ExercisesModule {
    @Provides
    fun championWatchWorkoutImportCoordinator(
        connectionManager: ChampionConnectionManager,
        connectionBootstrap: DeviceConnectionBootstrap,
        foregroundBleOperationGate: ForegroundBleOperationGate,
        workoutActivityRepository: WorkoutActivityRepository,
        @BleDispatcher bleDispatcher: CoroutineDispatcher,
    ): ChampionWatchWorkoutImportCoordinator = ChampionWatchWorkoutImportCoordinator(
        connectionManager = connectionManager,
        connectionBootstrap = connectionBootstrap,
        foregroundBleOperationGate = foregroundBleOperationGate,
        workoutActivityRepository = workoutActivityRepository,
        bleDispatcher = bleDispatcher,
    )

    @Provides
    fun watchWorkoutImportCoordinator(
        source: ChampionWatchWorkoutImportCoordinator,
    ): WatchWorkoutImportCoordinator = source
}
