package br.com.mwconsulting.championcompanion.core.bluetooth

import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidChampionBleClient
import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidChampionBleScanner
import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidGattDiscoveryMapper
import br.com.mwconsulting.championcompanion.core.common.BleDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object ChampionBleModule {
    @Provides
    @Singleton
    fun provideChampionBleScanner(
        impl: AndroidChampionBleScanner,
    ): ChampionBleScanner = impl

    @Provides
    @Singleton
    fun provideChampionBleClient(
        impl: AndroidChampionBleClient,
    ): ChampionBleClient = impl

    @Provides
    @Singleton
    fun provideAndroidGattDiscoveryMapper(): AndroidGattDiscoveryMapper = AndroidGattDiscoveryMapper()

    @Provides
    @Singleton
    fun provideChampionCapabilityResolver(
        discoveryMapper: AndroidGattDiscoveryMapper,
    ): ChampionCapabilityResolver = ChampionCapabilityResolver { _, _ ->
        // Current wrappers expose service UUID callbacks but not full service/characteristic snapshots yet.
        // Keep resolver injectable and fail safely until richer discovery wiring lands.
        val unused = discoveryMapper
        ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
    }

    @Provides
    @Singleton
    fun provideChampionConnectionManager(
        scanner: ChampionBleScanner,
        client: ChampionBleClient,
        capabilityResolver: ChampionCapabilityResolver,
        protocolLogger: ChampionProtocolLogger,
        @BleDispatcher bleDispatcher: CoroutineDispatcher,
    ): ChampionConnectionManager = ChampionConnectionManager(
        scanner = scanner,
        client = client,
        capabilityResolver = capabilityResolver,
        parentScope = CoroutineScope(SupervisorJob() + bleDispatcher),
        protocolLogger = protocolLogger,
    )
}
