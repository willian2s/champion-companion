package br.com.mwconsulting.championcompanion.data

import br.com.mwconsulting.championcompanion.core.model.DataValidity
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.HeartRateDeduplicationKey
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryPeriod
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementId
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementSessionId
import br.com.mwconsulting.championcompanion.core.model.HeartRateSource
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import br.com.mwconsulting.championcompanion.core.model.valueOrNull
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomRepositoriesHeartRateHistoryQueriesTest {
    @Test
    fun `today period uses current local day window`() {
        val window = historyWindow(
            period = HeartRateHistoryPeriod.TODAY,
            zoneId = ZoneId.of("America/Sao_Paulo"),
            now = Instant.parse("2026-07-23T12:00:00Z"),
        )

        assertEquals(Instant.parse("2026-07-23T03:00:00Z"), window.fromInclusive)
        assertEquals(Instant.parse("2026-07-24T03:00:00Z"), window.untilExclusive)
    }

    @Test
    fun `last 7 days window includes today plus six prior local days`() {
        val window = historyWindow(
            period = HeartRateHistoryPeriod.LAST_7_DAYS,
            zoneId = ZoneId.of("UTC"),
            now = Instant.parse("2026-07-23T12:00:00Z"),
        )

        assertEquals(Instant.parse("2026-07-17T00:00:00Z"), window.fromInclusive)
        assertEquals(Instant.parse("2026-07-24T00:00:00Z"), window.untilExclusive)
    }

    @Test
    fun `snapshot consolidates only manual and d1 with overall aggregates`() {
        val snapshot = buildHeartRateHistorySnapshot(
            period = HeartRateHistoryPeriod.TODAY,
            zoneId = ZoneId.of("UTC"),
            measurements = listOf(
                manualMeasurement(1, 80, "2026-07-23T10:00:00Z"),
                d1Measurement(2, 70, "2026-07-23T09:00:00Z"),
                workoutMeasurement(3, 150, "2026-07-23T08:00:00Z"),
            ),
        )

        assertEquals(2, snapshot.entries.size)
        assertEquals(2, snapshot.aggregates.measurementCount)
        assertEquals(75.0, snapshot.aggregates.averageBpm)
        assertEquals(70, snapshot.aggregates.minimumBpm)
        assertEquals(80, snapshot.aggregates.maximumBpm)
        assertEquals(2, snapshot.chart.points.size)
    }

    @Test
    fun `today chart uses real points without daily bucketing`() {
        val entries = buildHeartRateHistorySnapshot(
            period = HeartRateHistoryPeriod.TODAY,
            zoneId = ZoneId.of("UTC"),
            measurements = listOf(
                manualMeasurement(1, 80, "2026-07-23T10:00:00Z"),
                d1Measurement(2, 70, "2026-07-23T09:00:00Z"),
            ),
        ).chart

        assertEquals(br.com.mwconsulting.championcompanion.core.model.HeartRateChartResolution.INTRADAY, entries.resolution)
        assertEquals(listOf(70.0, 80.0), entries.points.map { it.bpm })
        assertEquals(listOf(1, 1), entries.points.map { it.sampleCount })
    }

    @Test
    fun `last 30 days chart uses daily buckets without synthetic empty days`() {
        val chart = buildHeartRateHistorySnapshot(
            period = HeartRateHistoryPeriod.LAST_30_DAYS,
            zoneId = ZoneId.of("UTC"),
            measurements = listOf(
                d1Measurement(1, 60, "2026-07-20T10:00:00Z"),
                manualMeasurement(2, 80, "2026-07-20T11:00:00Z"),
                d1Measurement(3, 90, "2026-07-23T09:00:00Z"),
            ),
        ).chart

        assertEquals(br.com.mwconsulting.championcompanion.core.model.HeartRateChartResolution.DAILY, chart.resolution)
        assertEquals(2, chart.points.size)
        assertEquals(listOf(70.0, 90.0), chart.points.map { it.bpm })
        assertEquals(listOf(60, 90), chart.points.map { it.minimumBpm })
        assertEquals(listOf(80, 90), chart.points.map { it.maximumBpm })
        assertEquals(listOf(2, 1), chart.points.map { it.sampleCount })
    }

    @Test
    fun `empty snapshot keeps null aggregates and empty chart`() {
        val snapshot = buildHeartRateHistorySnapshot(
            period = HeartRateHistoryPeriod.LAST_7_DAYS,
            zoneId = ZoneId.of("UTC"),
            measurements = emptyList(),
        )

        assertEquals(0, snapshot.aggregates.measurementCount)
        assertNull(snapshot.aggregates.averageBpm)
        assertEquals(0, snapshot.chart.points.size)
    }

    private fun manualMeasurement(idSeed: Int, bpm: Int, measuredAt: String): HeartRateMeasurement =
        HeartRateMeasurement.createManualMeasurement(
            id = HeartRateMeasurementId(uuid(idSeed)),
            sessionId = HeartRateMeasurementSessionId(uuid(idSeed + 20)),
            deviceId = DeviceId(uuid(1)),
            bpm = bpm,
            measuredAt = Instant.parse(measuredAt),
            parserVersion = ParserVersion.create("manual-v1").valueOrNull()!!,
        ).valueOrNull()!!

    private fun d1Measurement(idSeed: Int, bpm: Int, measuredAt: String): HeartRateMeasurement {
        val instant = Instant.parse(measuredAt)
        val deviceId = DeviceId(uuid(1))
        return HeartRateMeasurement.create(
            id = HeartRateMeasurementId(uuid(idSeed)),
            source = HeartRateSource.WATCH_D1_HISTORY,
            bpm = bpm,
            measuredAt = instant,
            validity = DataValidity.VALID,
            deviceId = deviceId,
            importedAt = instant,
            parserVersion = ParserVersion.create("d1-v1").valueOrNull()!!,
            deduplicationKey = HeartRateDeduplicationKey.D1History(deviceId, instant, null),
        ).valueOrNull()!!
    }

    private fun workoutMeasurement(idSeed: Int, bpm: Int, measuredAt: String): HeartRateMeasurement =
        HeartRateMeasurement.create(
            id = HeartRateMeasurementId(uuid(idSeed)),
            source = HeartRateSource.APP_WORKOUT_SESSION,
            bpm = bpm,
            measuredAt = Instant.parse(measuredAt),
            validity = DataValidity.VALID,
            appWorkoutSessionId = br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionId(uuid(30)),
            deduplicationKey = HeartRateDeduplicationKey.AppWorkoutSample(
                sessionId = br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionId(uuid(30)),
                measuredAt = Instant.parse(measuredAt),
            ),
        ).valueOrNull()!!

    private fun uuid(last: Int) = UUID.fromString("0190029f-2f80-7000-8000-0000000000${last.toString().padStart(2, '0')}")
}
