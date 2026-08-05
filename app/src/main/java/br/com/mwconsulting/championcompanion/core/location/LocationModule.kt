package br.com.mwconsulting.championcompanion.core.location

import android.content.Context
import android.location.LocationManager
import br.com.mwconsulting.championcompanion.core.location.android.AndroidWorkoutLocationSource
import br.com.mwconsulting.championcompanion.core.logging.StructuredLogger
import br.com.mwconsulting.championcompanion.core.model.PhoneWorkoutPolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    @Provides
    @Singleton
    fun provideWorkoutLocationSourceFactory(
        @ApplicationContext context: Context,
        structuredLogger: StructuredLogger,
    ): WorkoutLocationSourceFactory {
        val applicationContext = context.applicationContext
        val locationManager = applicationContext.getSystemService(LocationManager::class.java)
        val logger = WorkoutLocationLogger(structuredLogger)
        return WorkoutLocationSourceFactory {
            if (PhoneWorkoutPolicy.isDiscontinued()) {
                DiscontinuedWorkoutLocationSource()
            } else {
                AndroidWorkoutLocationSource(applicationContext, locationManager, logger)
            }
        }
    }
}
