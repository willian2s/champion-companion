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
}
