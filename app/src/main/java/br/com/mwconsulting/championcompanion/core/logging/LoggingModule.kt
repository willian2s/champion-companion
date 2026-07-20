package br.com.mwconsulting.championcompanion.core.logging

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoggingModule {
    @Provides
    @Singleton
    fun provideStructuredLogger(@ApplicationContext context: Context): StructuredLogger =
        debugStructuredLogger(context)
}
