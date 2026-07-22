package br.com.mwconsulting.championcompanion.core.bluetooth.fakes

import br.com.mwconsulting.championcompanion.core.model.MonotonicClock
import br.com.mwconsulting.championcompanion.core.model.MonotonicTimestamp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class FakeMonotonicClock(
    initialMillis: Long = 0L,
) : MonotonicClock {
    private var currentMillis: Long = initialMillis

    fun nowMillis(): Long = currentMillis

    override fun now(): MonotonicTimestamp = MonotonicTimestamp(currentMillis)

    override fun elapsedSince(startedAt: MonotonicTimestamp): Duration =
        (currentMillis - startedAt.elapsedRealtimeMillis).coerceAtLeast(0L).milliseconds

    fun advanceBy(millis: Long) {
        require(millis >= 0) { "millis must be non-negative" }
        currentMillis += millis
    }
}
