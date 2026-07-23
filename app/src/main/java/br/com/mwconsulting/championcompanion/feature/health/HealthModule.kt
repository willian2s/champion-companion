package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.common.BleDispatcher
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationGate
import br.com.mwconsulting.championcompanion.core.model.InMemoryForegroundBleOperationGate
import br.com.mwconsulting.championcompanion.core.model.MonotonicClock
import br.com.mwconsulting.championcompanion.feature.device.DeviceConnectionBootstrap
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
object HealthModule {
    @Provides
    @Singleton
    fun provideForegroundBleOperationGate(): ForegroundBleOperationGate = InMemoryForegroundBleOperationGate()

    @Provides
    @Singleton
    fun provideMonotonicClock(): MonotonicClock = AndroidElapsedRealtimeMonotonicClock()

    @Provides
    fun provideManualHeartRateCoordinator(
        connectionManager: ChampionConnectionManager,
        connectionBootstrap: DeviceConnectionBootstrap,
        foregroundBleOperationGate: ForegroundBleOperationGate,
        monotonicClock: MonotonicClock,
        @BleDispatcher bleDispatcher: CoroutineDispatcher,
    ): ManualHeartRateCoordinator = ChampionManualHeartRateCoordinator(
        connectionManager = connectionManager,
        connectionBootstrap = connectionBootstrap,
        foregroundBleOperationGate = foregroundBleOperationGate,
        monotonicClock = monotonicClock,
        bleDispatcher = bleDispatcher,
    )
}
