package br.com.mwconsulting.championcompanion.core.healthconnect

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthConnectModule {
    @Binds
    @Singleton
    abstract fun bindHealthConnectPlatform(
        platform: AndroidHealthConnectPlatform,
    ): HealthConnectPlatform

    @Binds
    @Singleton
    abstract fun bindHealthConnectGateway(
        gateway: AndroidHealthConnectGateway,
    ): HealthConnectGateway

    @Binds
    @Singleton
    abstract fun bindHealthRecordMapper(
        mapper: DefaultHealthRecordMapper,
    ): HealthRecordMapper

    @Binds
    @Singleton
    abstract fun bindHealthConnectSyncCoordinator(
        coordinator: DefaultHealthConnectSyncCoordinator,
    ): HealthConnectSyncCoordinator
}
