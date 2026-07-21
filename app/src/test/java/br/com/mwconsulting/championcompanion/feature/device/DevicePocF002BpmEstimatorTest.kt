package br.com.mwconsulting.championcompanion.feature.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePocF002BpmEstimatorTest {
    @Test
    fun `inspect returns reproducible estimate for valid synthetic signal`() {
        val inspection = DevicePocF002BpmEstimator.inspect(
            samples = syntheticPulseSignal(durationSeconds = 60, bpm = 72, sampleRateHz = 50, amplitude = 900),
            durationSeconds = 60.0,
        )

        val estimate = inspection.estimate
        assertNotNull(estimate)
        assertTrue((estimate?.bpm ?: 0) in 71..73)
        assertEquals(DevicePocHeartRateSignalQuality.GOOD, estimate?.quality)
        assertTrue((estimate?.sampleRateHz ?: 0.0) >= 49.0)
        assertTrue((estimate?.peakCount ?: 0) >= 12)
    }

    @Test
    fun `inspect returns null estimate for insufficient positive samples`() {
        val inspection = DevicePocF002BpmEstimator.inspect(
            samples = List(200) { if (it % 2 == 0) 100 else 0 },
            durationSeconds = 12.0,
        )

        assertNull(inspection.estimate)
        assertEquals(DevicePocHeartRateSignalQuality.LOW, inspection.quality)
        assertTrue(inspection.qualityScore <= 30)
    }

    @Test
    fun `inspect can produce low quality estimate when signal is weak but sufficient`() {
        val inspection = DevicePocF002BpmEstimator.inspect(
            samples = syntheticPulseSignal(durationSeconds = 10, bpm = 72, sampleRateHz = 50, amplitude = 120),
            durationSeconds = 10.0,
        )

        val estimate = inspection.estimate
        assertNotNull(estimate)
        assertTrue((estimate?.bpm ?: 0) in 35..220)
        assertEquals(DevicePocHeartRateSignalQuality.LOW, estimate?.quality)
        assertTrue((estimate?.qualityScore ?: 100) < 45)
    }

    @Test
    fun `final selection prefers current d0 over f002 estimate`() {
        val selection = DevicePocF002BpmEstimator.selectFinalBpm(
            currentD0Bpm = 81,
            estimate = DevicePocF002BpmEstimator.Estimate(
                bpm = 72,
                quality = DevicePocHeartRateSignalQuality.GOOD,
                qualityScore = 90,
                sampleRateHz = 50.0,
                peakCount = 50,
                validIntervalCount = 49,
                dynamicRange = 800,
                intervalCv = 0.05,
            ),
        )

        assertEquals(81, selection.bpm)
        assertEquals(DevicePocHeartRateFinalSource.D0_CURRENT, selection.source)
    }

    @Test
    fun `final selection falls back to f002 estimate when current d0 is absent`() {
        val selection = DevicePocF002BpmEstimator.selectFinalBpm(
            currentD0Bpm = null,
            estimate = DevicePocF002BpmEstimator.Estimate(
                bpm = 74,
                quality = DevicePocHeartRateSignalQuality.MEDIUM,
                qualityScore = 60,
                sampleRateHz = 50.0,
                peakCount = 40,
                validIntervalCount = 39,
                dynamicRange = 500,
                intervalCv = 0.09,
            ),
        )

        assertEquals(74, selection.bpm)
        assertEquals(DevicePocHeartRateFinalSource.F002_ESTIMATE, selection.source)
    }

    @Test
    fun `final selection is unavailable when both d0 and estimate are absent`() {
        val selection = DevicePocF002BpmEstimator.selectFinalBpm(
            currentD0Bpm = null,
            estimate = null,
        )

        assertNull(selection.bpm)
        assertEquals(DevicePocHeartRateFinalSource.UNAVAILABLE, selection.source)
    }

    private fun syntheticPulseSignal(
        durationSeconds: Int,
        bpm: Int,
        sampleRateHz: Int,
        amplitude: Int,
    ): List<Int> {
        val totalSamples = durationSeconds * sampleRateHz
        val intervalSamples = ((60.0 / bpm) * sampleRateHz).toInt().coerceAtLeast(1)
        val pulse = intArrayOf(0, amplitude / 4, amplitude / 2, amplitude, amplitude / 2, amplitude / 4, 0)
        return List(totalSamples) { index ->
            var value = 1_000 + ((index % 23) - 11) * 3
            val peakCenter = ((index / intervalSamples) * intervalSamples) + (intervalSamples / 2)
            val pulseOffset = index - (peakCenter - pulse.size / 2)
            if (pulseOffset in pulse.indices) {
                value += pulse[pulseOffset]
            }
            value.coerceAtLeast(1)
        }
    }
}
