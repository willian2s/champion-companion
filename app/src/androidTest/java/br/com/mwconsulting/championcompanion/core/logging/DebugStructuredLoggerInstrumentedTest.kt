package br.com.mwconsulting.championcompanion.core.logging

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.mwconsulting.championcompanion.core.error.InfrastructureError
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugStructuredLoggerInstrumentedTest {
    @Test
    fun debugLoggerWritesControlledErrorEvent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        debugStructuredLogger(context).debug(
            event = LogEvent.ErrorPresented,
            error = InfrastructureError.NetworkUnavailable,
        )
    }
}
