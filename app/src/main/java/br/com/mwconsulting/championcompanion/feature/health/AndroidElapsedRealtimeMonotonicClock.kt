package br.com.mwconsulting.championcompanion.feature.health

import android.os.SystemClock
import br.com.mwconsulting.championcompanion.core.model.MonotonicClock
import br.com.mwconsulting.championcompanion.core.model.MonotonicTimestamp
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class AndroidElapsedRealtimeMonotonicClock @Inject constructor() : MonotonicClock {
    override fun now(): MonotonicTimestamp = MonotonicTimestamp(SystemClock.elapsedRealtime())

    override fun elapsedSince(startedAt: MonotonicTimestamp): Duration =
        (SystemClock.elapsedRealtime() - startedAt.elapsedRealtimeMillis).coerceAtLeast(0L).milliseconds
}
