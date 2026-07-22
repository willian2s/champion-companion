package br.com.mwconsulting.championcompanion.data

import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.DailySummary
import br.com.mwconsulting.championcompanion.core.model.DailySummaryId
import br.com.mwconsulting.championcompanion.core.model.DailySummaryKey
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.DataValidity
import br.com.mwconsulting.championcompanion.core.model.DiagnosticCaptureFlow
import br.com.mwconsulting.championcompanion.core.model.DiagnosticParseResult
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayload
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadCapture
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadId
import br.com.mwconsulting.championcompanion.core.model.HeartRateDeduplicationKey
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementId
import br.com.mwconsulting.championcompanion.core.model.HeartRateSource
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import br.com.mwconsulting.championcompanion.core.model.valueOrNull
import br.com.mwconsulting.championcompanion.core.database.DeviceEntity
import br.com.mwconsulting.championcompanion.core.database.StoredDeviceModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseMappersTest {
    @Test
    fun `device mapper preserves optional import metadata absence`() {
        val associatedAt = Instant.parse("2026-07-21T12:00:00Z")
        val device = Device.create(
            id = DeviceId(UuidV7Generator(Clock.fixed(associatedAt, ZoneOffset.UTC), Random(1)).next()),
            platformIdentifier = "platform-id",
            name = "Champion C001",
            associatedAt = associatedAt,
        ).valueOrNull()!!

        val mapped = device.toEntity().toDomain()

        assertEquals(device, mapped)
    }

    @Test
    fun `device mapper accepts reassociation after older historical import`() {
        val firstAssociation = Instant.parse("2026-07-20T12:00:00Z")
        val reassociatedAt = Instant.parse("2026-07-21T12:00:00Z")
        val device = Device.create(
            id = DeviceId(UuidV7Generator(Clock.fixed(reassociatedAt, ZoneOffset.UTC), Random(2)).next()),
            platformIdentifier = "platform-id",
            name = "Champion C001",
            associatedAt = firstAssociation,
            importMetadata = br.com.mwconsulting.championcompanion.core.model.DeviceImportMetadata(
                lastImportedAt = firstAssociation.plusSeconds(30),
                parserVersion = ParserVersion.create("d1-v1").valueOrNull(),
            ),
        ).valueOrNull()!!

        val mapped = device.toEntity().toDomain(associatedAtEpochMillis = reassociatedAt.toEpochMilli())

        assertEquals(reassociatedAt, mapped?.associatedAt)
        assertEquals(firstAssociation.plusSeconds(30), mapped?.importMetadata?.lastImportedAt)
    }

    @Test
    fun `daily summary mapper preserves natural key and optional metrics`() {
        val generator = generator()
        val summary = DailySummary.create(
            DailySummaryId(generator.next()),
            DailySummaryKey(DeviceId(generator.next()), DataOrigin.WATCH_DAILY_SUMMARY, LocalDate.of(2026, 7, 21)),
            1_234, null, 56.0, instant, instant.plusSeconds(1), DataValidity.VALID,
        ).valueOrNull()!!

        assertEquals(summary, summary.toEntity().toDomain())
    }

    @Test
    fun `heart rate mapper preserves D1 deduplication key and unresolved watch time`() {
        val generator = generator()
        val deviceId = DeviceId(generator.next())
        val measuredAt = instant.plusSeconds(5)
        val measurement = HeartRateMeasurement.create(
            HeartRateMeasurementId(generator.next()), HeartRateSource.WATCH_D1_HISTORY, 72, measuredAt,
            validity = DataValidity.VALID, deviceId = deviceId, importedAt = measuredAt.plusSeconds(1),
            parserVersion = ParserVersion.create("d1-v1").valueOrNull()!!,
            deduplicationKey = HeartRateDeduplicationKey.D1History(deviceId, measuredAt, null),
        ).valueOrNull()!!

        assertEquals(measurement, measurement.toEntity().toDomain())
    }

    @Test
    fun `diagnostic mapper copies private bytes without exposing parse details`() {
        val payload = DiagnosticPayload.create(
            DiagnosticPayloadId(generator().next()),
            DiagnosticPayloadCapture(
                command = "D4", flow = DiagnosticCaptureFlow.WATCH_WORKOUT, privateBytes = byteArrayOf(1, 2),
                deviceId = null, firmware = null, parserVersion = ParserVersion.create("d4-v1").valueOrNull()!!,
                capturedAt = instant,
            ),
            DiagnosticParseResult.PARTIAL,
        ).valueOrNull()!!

        val mapped = payload.toEntity().toDomain()!!

        assertEquals(payload.byteCount, mapped.byteCount)
        assertEquals(payload.result, mapped.result)
        assertTrue(payload.bytesForPrivateStorage().contentEquals(mapped.bytesForPrivateStorage()))
    }

    @Test
    fun `device mapper rejects malformed persisted identifier safely`() {
        val invalid = DeviceEntity(
            id = "invalid", platformIdentifier = "platform", name = "Champion", model = StoredDeviceModel.CHAMPION_C001,
            firmware = null, associatedAtEpochMillis = 0L, lastSuccessfulConnectionAtEpochMillis = null,
            lastImportedAtEpochMillis = null, importParserVersion = null,
        )

        assertNull(invalid.toDomain())
    }

    private fun generator() = UuidV7Generator(Clock.fixed(instant, ZoneOffset.UTC), Random(1))

    private companion object {
        val instant: Instant = Instant.parse("2026-07-21T12:00:00Z")
    }
}
