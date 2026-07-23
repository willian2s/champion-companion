package br.com.mwconsulting.championcompanion.core.bluetooth

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class ChampionHeartRateSignalQuality {
    GOOD,
    MEDIUM,
    LOW,
}

enum class ChampionHeartRateFinalSource {
    D0_CURRENT,
    F002_ESTIMATE,
    UNAVAILABLE,
}

object ChampionF002BpmEstimator {
    private const val minPositiveSamples = 250
    private const val minValidIntervals = 3
    private const val minBpm = 35
    private const val maxBpm = 220
    private const val thresholdStdMultiplier = 0.35
    private const val minPeakGapSeconds = 0.4
    private const val minIntervalSeconds = 0.4
    private const val maxIntervalSeconds = 1.6
    private const val baselineWindowSeconds = 0.75
    private const val smoothingWindowSeconds = 0.18
    private const val initialDiscardSeconds = 6.0

    data class Estimate(
        val bpm: Int,
        val quality: ChampionHeartRateSignalQuality,
        val qualityScore: Int,
        val sampleRateHz: Double,
        val peakCount: Int,
        val validIntervalCount: Int,
        val dynamicRange: Int,
        val intervalCv: Double,
    )

    data class Inspection(
        val estimate: Estimate?,
        val positiveSampleCount: Int,
        val trimmedSampleCount: Int,
        val sampleRateHz: Double?,
        val peakCount: Int,
        val validIntervalCount: Int,
        val dynamicRange: Int,
        val intervalCv: Double,
        val quality: ChampionHeartRateSignalQuality,
        val qualityScore: Int,
    )

    data class FinalSelection(
        val bpm: Int?,
        val source: ChampionHeartRateFinalSource,
    )

    fun inspect(
        samples: List<Int>,
        durationSeconds: Double,
    ): Inspection {
        if (durationSeconds <= 0.0) {
            return Inspection(
                estimate = null,
                positiveSampleCount = samples.count { it > 0 },
                trimmedSampleCount = 0,
                sampleRateHz = null,
                peakCount = 0,
                validIntervalCount = 0,
                dynamicRange = 0,
                intervalCv = 0.0,
                quality = ChampionHeartRateSignalQuality.LOW,
                qualityScore = 50,
            )
        }

        val positiveSamples = samples.filter { it > 0 }
        if (positiveSamples.size < minPositiveSamples) {
            return qualityOnlyInspection(
                positiveSampleCount = positiveSamples.size,
                durationSeconds = durationSeconds,
            )
        }

        val sampleRateHz = positiveSamples.size / durationSeconds
        if (sampleRateHz <= 0.0) {
            return qualityOnlyInspection(
                positiveSampleCount = positiveSamples.size,
                durationSeconds = durationSeconds,
            )
        }

        val discardCount = minOf(
            (positiveSamples.size * 0.25).toInt(),
            (sampleRateHz * initialDiscardSeconds).toInt(),
        )
        val trimmedSamples = positiveSamples.drop(discardCount)
        if (trimmedSamples.size < minPositiveSamples) {
            return qualityOnlyInspection(
                positiveSampleCount = positiveSamples.size,
                durationSeconds = durationSeconds,
                sampleRateHz = sampleRateHz,
                trimmedSampleCount = trimmedSamples.size,
            )
        }

        val baselineWindow = windowSize(sampleRateHz, baselineWindowSeconds)
        val smoothingWindow = windowSize(sampleRateHz, smoothingWindowSeconds)
        val normalized = trimmedSamples.mapIndexed { index, sample ->
            sample - movingAverageInt(trimmedSamples, index, baselineWindow)
        }
        val smoothed = normalized.indices.map { index ->
            movingAverageDouble(normalized, index, smoothingWindow)
        }
        val threshold = mean(smoothed) + (standardDeviation(smoothed) * thresholdStdMultiplier)
        val minPeakDistance = max(1, (sampleRateHz * minPeakGapSeconds).roundToInt())
        val peaks = detectPeaks(smoothed, threshold, minPeakDistance)
        val intervals = peaks.zipWithNext { left, right ->
            (right - left) / sampleRateHz
        }.filter { it in minIntervalSeconds..maxIntervalSeconds }

        val bpm = if (intervals.size >= minValidIntervals) {
            val candidate = (60.0 / median(intervals)).roundToInt()
            candidate.takeIf { it in minBpm..maxBpm }
        } else {
            null
        }
        val dynamicRange = if (smoothed.isEmpty()) 0 else (smoothed.max() - smoothed.min()).roundToInt()
        val intervalCv = if (intervals.size >= 2) coefficientOfVariation(intervals) else 0.0
        val qualityScore = qualityScore(
            bpm = bpm,
            positiveSampleCount = positiveSamples.size,
            durationSeconds = durationSeconds,
            peakCount = peaks.size,
            intervalCv = intervalCv,
            dynamicRange = dynamicRange,
        )
        val quality = qualityFor(qualityScore)
        val estimate = bpm?.let {
            Estimate(
                bpm = it,
                quality = quality,
                qualityScore = qualityScore,
                sampleRateHz = sampleRateHz,
                peakCount = peaks.size,
                validIntervalCount = intervals.size,
                dynamicRange = dynamicRange,
                intervalCv = intervalCv,
            )
        }
        return Inspection(
            estimate = estimate,
            positiveSampleCount = positiveSamples.size,
            trimmedSampleCount = trimmedSamples.size,
            sampleRateHz = sampleRateHz,
            peakCount = peaks.size,
            validIntervalCount = intervals.size,
            dynamicRange = dynamicRange,
            intervalCv = intervalCv,
            quality = quality,
            qualityScore = qualityScore,
        )
    }

    fun selectFinalBpm(
        currentD0Bpm: Int?,
        estimate: Estimate?,
    ): FinalSelection = when {
        currentD0Bpm in minBpm..maxBpm -> FinalSelection(currentD0Bpm, ChampionHeartRateFinalSource.D0_CURRENT)
        estimate != null -> FinalSelection(estimate.bpm, ChampionHeartRateFinalSource.F002_ESTIMATE)
        else -> FinalSelection(null, ChampionHeartRateFinalSource.UNAVAILABLE)
    }

    private fun qualityOnlyInspection(
        positiveSampleCount: Int,
        durationSeconds: Double,
        sampleRateHz: Double? = null,
        trimmedSampleCount: Int = 0,
    ): Inspection {
        val qualityScore = qualityScore(
            bpm = null,
            positiveSampleCount = positiveSampleCount,
            durationSeconds = durationSeconds,
            peakCount = 0,
            intervalCv = 0.0,
            dynamicRange = 0,
        )
        return Inspection(
            estimate = null,
            positiveSampleCount = positiveSampleCount,
            trimmedSampleCount = trimmedSampleCount,
            sampleRateHz = sampleRateHz,
            peakCount = 0,
            validIntervalCount = 0,
            dynamicRange = 0,
            intervalCv = 0.0,
            quality = qualityFor(qualityScore),
            qualityScore = qualityScore,
        )
    }

    private fun qualityScore(
        bpm: Int?,
        positiveSampleCount: Int,
        durationSeconds: Double,
        peakCount: Int,
        intervalCv: Double,
        dynamicRange: Int,
    ): Int {
        var score = 100
        if (bpm == null) score -= 50
        if (positiveSampleCount < 2_500) score -= 25
        if (durationSeconds < 20.0) score -= 20
        if (peakCount < 12) score -= 20
        score -= when {
            intervalCv > 0.28 -> 30
            intervalCv > 0.18 -> 15
            else -> 0
        }
        if (dynamicRange < 250) score -= 15
        return score.coerceIn(0, 100)
    }

    private fun qualityFor(score: Int): ChampionHeartRateSignalQuality = when {
        score >= 75 -> ChampionHeartRateSignalQuality.GOOD
        score >= 45 -> ChampionHeartRateSignalQuality.MEDIUM
        else -> ChampionHeartRateSignalQuality.LOW
    }

    private fun windowSize(sampleRateHz: Double, windowSeconds: Double): Int =
        max(3, (sampleRateHz * windowSeconds).roundToInt())

    private fun movingAverageInt(values: List<Int>, index: Int, windowSize: Int): Double {
        val halfWindow = windowSize / 2
        val start = max(0, index - halfWindow)
        val end = minOf(values.lastIndex, index + halfWindow)
        var total = 0.0
        for (currentIndex in start..end) {
            total += values[currentIndex]
        }
        return total / (end - start + 1)
    }

    private fun movingAverageDouble(values: List<Double>, index: Int, windowSize: Int): Double {
        val halfWindow = windowSize / 2
        val start = max(0, index - halfWindow)
        val end = minOf(values.lastIndex, index + halfWindow)
        var total = 0.0
        for (currentIndex in start..end) {
            total += values[currentIndex]
        }
        return total / (end - start + 1)
    }

    private fun detectPeaks(
        signal: List<Double>,
        threshold: Double,
        minDistance: Int,
    ): List<Int> {
        if (signal.size < 3) return emptyList()
        val peaks = mutableListOf<Int>()
        var lastPeakIndex = -minDistance
        for (index in 1 until signal.lastIndex) {
            val current = signal[index]
            if (current <= threshold) continue
            if (current < signal[index - 1] || current < signal[index + 1]) continue
            if (index - lastPeakIndex < minDistance) {
                if (peaks.isNotEmpty() && current > signal[peaks.last()]) {
                    peaks[peaks.lastIndex] = index
                    lastPeakIndex = index
                }
                continue
            }
            peaks += index
            lastPeakIndex = index
        }
        return peaks
    }

    private fun mean(values: List<Double>): Double = values.sum() / values.size

    private fun standardDeviation(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = mean(values)
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return sqrt(variance)
    }

    private fun coefficientOfVariation(values: List<Double>): Double {
        val mean = mean(values)
        if (mean == 0.0) return 0.0
        return standardDeviation(values) / mean
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }
}
