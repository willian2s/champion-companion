package br.com.mwconsulting.championcompanion.core.logging

import android.content.Context
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiagnosticPacketSink
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionProtocolLogger
import br.com.mwconsulting.championcompanion.core.bluetooth.NoOpChampionDiagnosticPacketSink
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

    @Provides
    @Singleton
    fun provideChampionDiagnosticPacketSink(): ChampionDiagnosticPacketSink =
        NoOpChampionDiagnosticPacketSink

    @Provides
    @Singleton
    fun provideChampionProtocolLogger(
        structuredLogger: StructuredLogger,
        diagnosticPacketSink: ChampionDiagnosticPacketSink,
    ): ChampionProtocolLogger = ChampionProtocolLogger(structuredLogger, diagnosticPacketSink)
}
