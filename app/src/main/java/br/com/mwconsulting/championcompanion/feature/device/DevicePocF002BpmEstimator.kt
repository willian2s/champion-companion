package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionF002BpmEstimator
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionHeartRateFinalSource
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionHeartRateSignalQuality

internal object DevicePocF002BpmEstimator {
    data class Estimate(
        val bpm: Int,
        val quality: DevicePocHeartRateSignalQuality,
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
        val quality: DevicePocHeartRateSignalQuality,
        val qualityScore: Int,
    )

    data class FinalSelection(
        val bpm: Int?,
        val source: DevicePocHeartRateFinalSource,
    )

    fun inspect(
        samples: List<Int>,
        durationSeconds: Double,
    ): Inspection = ChampionF002BpmEstimator.inspect(samples, durationSeconds).toDeviceInspection()

    fun selectFinalBpm(
        currentD0Bpm: Int?,
        estimate: Estimate?,
    ): FinalSelection = ChampionF002BpmEstimator.selectFinalBpm(
        currentD0Bpm = currentD0Bpm,
        estimate = estimate?.toChampionEstimate(),
    ).toDeviceFinalSelection()

    private fun Estimate.toChampionEstimate(): ChampionF002BpmEstimator.Estimate = ChampionF002BpmEstimator.Estimate(
        bpm = bpm,
        quality = quality.toChampionQuality(),
        qualityScore = qualityScore,
        sampleRateHz = sampleRateHz,
        peakCount = peakCount,
        validIntervalCount = validIntervalCount,
        dynamicRange = dynamicRange,
        intervalCv = intervalCv,
    )

    private fun ChampionF002BpmEstimator.Inspection.toDeviceInspection(): Inspection = Inspection(
        estimate = estimate?.toDeviceEstimate(),
        positiveSampleCount = positiveSampleCount,
        trimmedSampleCount = trimmedSampleCount,
        sampleRateHz = sampleRateHz,
        peakCount = peakCount,
        validIntervalCount = validIntervalCount,
        dynamicRange = dynamicRange,
        intervalCv = intervalCv,
        quality = quality.toDeviceQuality(),
        qualityScore = qualityScore,
    )

    private fun ChampionF002BpmEstimator.Estimate.toDeviceEstimate(): Estimate = Estimate(
        bpm = bpm,
        quality = quality.toDeviceQuality(),
        qualityScore = qualityScore,
        sampleRateHz = sampleRateHz,
        peakCount = peakCount,
        validIntervalCount = validIntervalCount,
        dynamicRange = dynamicRange,
        intervalCv = intervalCv,
    )

    private fun ChampionF002BpmEstimator.FinalSelection.toDeviceFinalSelection(): FinalSelection = FinalSelection(
        bpm = bpm,
        source = source.toDeviceFinalSource(),
    )

    private fun DevicePocHeartRateSignalQuality.toChampionQuality(): ChampionHeartRateSignalQuality = when (this) {
        DevicePocHeartRateSignalQuality.GOOD -> ChampionHeartRateSignalQuality.GOOD
        DevicePocHeartRateSignalQuality.MEDIUM -> ChampionHeartRateSignalQuality.MEDIUM
        DevicePocHeartRateSignalQuality.LOW -> ChampionHeartRateSignalQuality.LOW
    }

    private fun ChampionHeartRateSignalQuality.toDeviceQuality(): DevicePocHeartRateSignalQuality = when (this) {
        ChampionHeartRateSignalQuality.GOOD -> DevicePocHeartRateSignalQuality.GOOD
        ChampionHeartRateSignalQuality.MEDIUM -> DevicePocHeartRateSignalQuality.MEDIUM
        ChampionHeartRateSignalQuality.LOW -> DevicePocHeartRateSignalQuality.LOW
    }

    private fun ChampionHeartRateFinalSource.toDeviceFinalSource(): DevicePocHeartRateFinalSource = when (this) {
        ChampionHeartRateFinalSource.D0_CURRENT -> DevicePocHeartRateFinalSource.D0_CURRENT
        ChampionHeartRateFinalSource.F002_ESTIMATE -> DevicePocHeartRateFinalSource.F002_ESTIMATE
        ChampionHeartRateFinalSource.UNAVAILABLE -> DevicePocHeartRateFinalSource.UNAVAILABLE
    }
}
