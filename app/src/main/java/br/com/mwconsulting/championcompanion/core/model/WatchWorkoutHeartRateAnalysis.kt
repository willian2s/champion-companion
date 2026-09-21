package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration

enum class HeartRateZone(
    val weight: Int,
) {
    BELOW_ZONE_1(0),
    Z1(1),
    Z2(2),
    Z3(3),
    Z4(4),
    Z5(5),
}

data class HeartRateZoneSummary(
    val zone: HeartRateZone,
    val duration: Duration,
    val percentage: Double,
) {
    val weight: Int
        get() = zone.weight
}

data class WorkoutHeartRateAnalysis(
    val configuredMaximumBpm: Int?,
    val observedMaximumBpm: Int,
    val sessionDuration: Duration,
    val coveredDuration: Duration,
    val coverageRatio: Double,
    val zones: List<HeartRateZoneSummary>?,
    val cardiovascularLoad: Double?,
    val observedAboveConfiguredMaximum: Boolean,
)

enum class WorkoutHeartRateAnalysisStatus {
    AVAILABLE,
    CONFIGURATION_REQUIRED,
    NO_HEART_RATE_SAMPLES,
    UNAVAILABLE,
}

enum class WorkoutHeartRateAnalysisUnavailableReason {
    INVALID_CONFIGURATION,
    INVALID_SAMPLES,
    OUT_OF_SCOPE,
}

sealed interface WorkoutHeartRateAnalysisResult {
    val status: WorkoutHeartRateAnalysisStatus
    val analysis: WorkoutHeartRateAnalysis?

    data class Available(
        override val analysis: WorkoutHeartRateAnalysis,
    ) : WorkoutHeartRateAnalysisResult {
        override val status: WorkoutHeartRateAnalysisStatus = WorkoutHeartRateAnalysisStatus.AVAILABLE
    }

    data class ConfigurationRequired(
        override val analysis: WorkoutHeartRateAnalysis,
    ) : WorkoutHeartRateAnalysisResult {
        override val status: WorkoutHeartRateAnalysisStatus = WorkoutHeartRateAnalysisStatus.CONFIGURATION_REQUIRED
    }

    data object NoHeartRateSamples : WorkoutHeartRateAnalysisResult {
        override val status: WorkoutHeartRateAnalysisStatus = WorkoutHeartRateAnalysisStatus.NO_HEART_RATE_SAMPLES
        override val analysis: WorkoutHeartRateAnalysis? = null
    }

    data class Unavailable(
        val reason: WorkoutHeartRateAnalysisUnavailableReason,
    ) : WorkoutHeartRateAnalysisResult {
        override val status: WorkoutHeartRateAnalysisStatus = WorkoutHeartRateAnalysisStatus.UNAVAILABLE
        override val analysis: WorkoutHeartRateAnalysis? = null
    }
}

internal data class WorkoutHeartRateSampleWindow(
    val sample: WorkoutHeartRateSample,
    val window: WorkoutIntervalWindow,
)

/**
 * Única primitiva de sample-and-hold usada por splits e análise de zonas.
 * Samples anteriores à sessão podem iniciar cobertura em [sessionWindow.start]
 * para preservar o contrato dos splits; consumidores que exigem somente fatos
 * da sessão devem filtrá-los antes de chamar esta função.
 */
internal fun sampleAndHoldWindows(
    samples: List<WorkoutHeartRateSample>,
    sessionWindow: WorkoutIntervalWindow,
): List<WorkoutHeartRateSampleWindow> {
    val ordered = samples.sortedBy(WorkoutHeartRateSample::sampledAt)
    return ordered.mapIndexedNotNull { index, sample ->
        val sampleStart = maxInstant(sample.sampledAt, sessionWindow.start)
        val nextSampleAt = ordered.getOrNull(index + 1)?.sampledAt ?: sessionWindow.end
        val sampleEnd = minInstant(nextSampleAt, sessionWindow.end)
        if (!sampleEnd.isAfter(sampleStart)) return@mapIndexedNotNull null
        WorkoutIntervalWindow.create(sampleStart, sampleEnd).valueOrNull()?.let { window ->
            WorkoutHeartRateSampleWindow(sample, window)
        }
    }
}

object WorkoutHeartRateAnalysisPolicy {
    private const val MINIMUM_HEART_RATE_BPM = 30
    private const val MAXIMUM_HEART_RATE_BPM = 220
    private const val ZONE_1_RATIO = 0.50
    private const val ZONE_2_RATIO = 0.60
    private const val ZONE_3_RATIO = 0.70
    private const val ZONE_4_RATIO = 0.80
    private const val ZONE_5_RATIO = 0.90

    fun analyze(
        sessionWindow: WorkoutIntervalWindow,
        heartRateSamples: List<WorkoutHeartRateSample>,
        configuredMaximumBpm: Int?,
    ): WorkoutHeartRateAnalysisResult {
        if (configuredMaximumBpm != null && configuredMaximumBpm !in MINIMUM_HEART_RATE_BPM..MAXIMUM_HEART_RATE_BPM) {
            return WorkoutHeartRateAnalysisResult.Unavailable(
                WorkoutHeartRateAnalysisUnavailableReason.INVALID_CONFIGURATION,
            )
        }

        val samplesInSession = heartRateSamples
            .filter { sample -> sessionWindow.contains(sample.sampledAt) }
            .sortedBy(WorkoutHeartRateSample::sampledAt)
        if (samplesInSession.isEmpty()) {
            return WorkoutHeartRateAnalysisResult.NoHeartRateSamples
        }
        if (!samplesInSession.all { sample -> sample.bpm in MINIMUM_HEART_RATE_BPM..MAXIMUM_HEART_RATE_BPM } ||
            samplesInSession.zipWithNext().any { (first, second) -> !second.sampledAt.isAfter(first.sampledAt) }
        ) {
            return WorkoutHeartRateAnalysisResult.Unavailable(
                WorkoutHeartRateAnalysisUnavailableReason.INVALID_SAMPLES,
            )
        }

        val sampleWindows = sampleAndHoldWindows(samplesInSession, sessionWindow)
        if (sampleWindows.isEmpty()) {
            return WorkoutHeartRateAnalysisResult.Unavailable(
                WorkoutHeartRateAnalysisUnavailableReason.INVALID_SAMPLES,
            )
        }

        val coveredDuration = sampleWindows.fold(Duration.ZERO) { total, sampleWindow ->
            total.plus(sampleWindow.window.duration)
        }
        val sessionSeconds = sessionWindow.duration.secondsAsDouble()
        val coveredSeconds = coveredDuration.secondsAsDouble()
        val coverageRatio = (coveredSeconds / sessionSeconds).coerceIn(0.0, 1.0)
        val observedMaximumBpm = samplesInSession.maxOf(WorkoutHeartRateSample::bpm)
        val observedAboveConfiguredMaximum = configuredMaximumBpm != null &&
            samplesInSession.any { sample -> sample.bpm > configuredMaximumBpm }
        val baseAnalysis = WorkoutHeartRateAnalysis(
            configuredMaximumBpm = configuredMaximumBpm,
            observedMaximumBpm = observedMaximumBpm,
            sessionDuration = sessionWindow.duration,
            coveredDuration = coveredDuration,
            coverageRatio = coverageRatio,
            zones = null,
            cardiovascularLoad = null,
            observedAboveConfiguredMaximum = observedAboveConfiguredMaximum,
        )
        if (configuredMaximumBpm == null) {
            return WorkoutHeartRateAnalysisResult.ConfigurationRequired(baseAnalysis)
        }

        val durationsByZone = HeartRateZone.entries.associateWith { Duration.ZERO }.toMutableMap()
        sampleWindows.forEach { sampleWindow ->
            val zone = classify(sampleWindow.sample.bpm, configuredMaximumBpm)
            durationsByZone[zone] = durationsByZone.getValue(zone).plus(sampleWindow.window.duration)
        }
        val zones = HeartRateZone.entries.map { zone ->
            val duration = durationsByZone.getValue(zone)
            HeartRateZoneSummary(
                zone = zone,
                duration = duration,
                percentage = duration.secondsAsDouble() / coveredSeconds,
            )
        }
        val cardiovascularLoad = zones.sumOf { summary ->
            summary.duration.secondsAsDouble() / 60.0 * summary.weight.toDouble()
        }
        return WorkoutHeartRateAnalysisResult.Available(
            baseAnalysis.copy(
                zones = zones,
                cardiovascularLoad = cardiovascularLoad,
            ),
        )
    }

    private fun classify(bpm: Int, configuredMaximumBpm: Int): HeartRateZone {
        val ratio = bpm.toDouble() / configuredMaximumBpm.toDouble()
        return when {
            ratio < ZONE_1_RATIO -> HeartRateZone.BELOW_ZONE_1
            ratio < ZONE_2_RATIO -> HeartRateZone.Z1
            ratio < ZONE_3_RATIO -> HeartRateZone.Z2
            ratio < ZONE_4_RATIO -> HeartRateZone.Z3
            ratio < ZONE_5_RATIO -> HeartRateZone.Z4
            else -> HeartRateZone.Z5
        }
    }
}
