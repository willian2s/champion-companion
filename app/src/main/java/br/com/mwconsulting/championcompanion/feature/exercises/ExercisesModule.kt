package br.com.mwconsulting.championcompanion.feature.exercises

import android.content.Context
import android.content.pm.ApplicationInfo
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.common.BleDispatcher
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadRepository
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationGate
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import br.com.mwconsulting.championcompanion.feature.device.DeviceConnectionBootstrap
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
object ExercisesModule {
    @Provides
    fun championWatchWorkoutImportCoordinator(
        @ApplicationContext context: Context,
        connectionManager: ChampionConnectionManager,
        connectionBootstrap: DeviceConnectionBootstrap,
        diagnosticPayloadRepository: DiagnosticPayloadRepository,
        foregroundBleOperationGate: ForegroundBleOperationGate,
        workoutActivityRepository: WorkoutActivityRepository,
        @BleDispatcher bleDispatcher: CoroutineDispatcher,
    ): ChampionWatchWorkoutImportCoordinator = ChampionWatchWorkoutImportCoordinator(
        connectionManager = connectionManager,
        connectionBootstrap = connectionBootstrap,
        diagnosticPayloadRepository = diagnosticPayloadRepository,
        foregroundBleOperationGate = foregroundBleOperationGate,
        workoutActivityRepository = workoutActivityRepository,
        bleDispatcher = bleDispatcher,
        isDebugBuild = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
    )

    @Provides
    fun watchWorkoutImportCoordinator(
        source: ChampionWatchWorkoutImportCoordinator,
    ): WatchWorkoutImportCoordinator = source
}
