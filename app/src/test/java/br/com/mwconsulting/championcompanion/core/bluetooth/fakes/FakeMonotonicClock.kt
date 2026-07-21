package br.com.mwconsulting.championcompanion.core.bluetooth.fakes

class FakeMonotonicClock(
    initialMillis: Long = 0L,
) {
    private var currentMillis: Long = initialMillis

    fun nowMillis(): Long = currentMillis

    fun advanceBy(millis: Long) {
        require(millis >= 0) { "millis must be non-negative" }
        currentMillis += millis
    }
}
