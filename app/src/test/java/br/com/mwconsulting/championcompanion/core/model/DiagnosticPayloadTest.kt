package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticPayloadTest {
    @Test
    fun `parse result maps validity and keeps technical error separate`() {
        val payload = payload(DiagnosticParseResult.TECHNICAL_ERROR, SafeDiagnosticError.PARSER_FAILURE)

        assertEquals(null, DiagnosticPayload.parserValidityFor(payload.result))
        assertEquals(SafeDiagnosticError.PARSER_FAILURE, payload.safeError)
        assertEquals(ParserValidity.PARTIAL, DiagnosticPayload.parserValidityFor(DiagnosticParseResult.PARTIAL))
    }

    @Test
    fun `derived output key is idempotent for reprocessing`() {
        val payload = payload()
        val key = DiagnosticDerivedOutputKey(payload.id, DiagnosticDerivedOutputType.WATCH_WORKOUT_IMPORT, 0)

        val reprocessed = payload.recordDerivedOutput(key).valueOrNull()!!
            .recordDerivedOutput(key).valueOrNull()!!

        assertEquals(setOf(key), reprocessed.derivedOutputs)
    }

    @Test
    fun `retention deletes expired then oldest payloads over global limit without derived deletion`() {
        val expired = payload(id = 1, capturedAt = now.minusSeconds(15 * 86_400L), bytes = 1)
        val oldest = payload(id = 2, capturedAt = now.minusSeconds(2), bytes = 6 * MIB)
        val newest = payload(id = 3, capturedAt = now.minusSeconds(1), bytes = 6 * MIB)
        val derived = DiagnosticDerivedOutputKey(oldest.id, DiagnosticDerivedOutputType.DAILY_SUMMARY, 0)
        val withDerived = oldest.recordDerivedOutput(derived).valueOrNull()!!

        val deleted = DiagnosticRetention.payloadIdsToDelete(listOf(newest, withDerived, expired), now)

        assertEquals(listOf(expired.id, withDerived.id), deleted)
        assertTrue(withDerived.derivedOutputs.contains(derived))
        assertFalse(deleted.contains(newest.id))
    }

    @Test
    fun `payload copies private bytes and enforces result error pairing`() {
        val source = byteArrayOf(1)
        val capture = DiagnosticPayloadCapture("D1", DiagnosticCaptureFlow.WATCH_HISTORY, source, null, null, ParserVersion.create("v1").valueOrNull()!!, now)
        val stored = DiagnosticPayload.create(DiagnosticPayloadId(uuid(1)), capture, DiagnosticParseResult.COMPLETED).valueOrNull()!!
        source[0] = 2

        assertEquals(1, stored.bytesForPrivateStorage()[0].toInt())
        assertEquals(
            DomainResult.Failure(DomainValidationError.INVALID_STATE),
            DiagnosticPayload.create(DiagnosticPayloadId(uuid(2)), capture, DiagnosticParseResult.TECHNICAL_ERROR),
        )
    }

    @Test
    fun `invalid parser result maps to invalid and retention keeps cutoff boundary`() {
        val boundary = payload(capturedAt = now.minus(DiagnosticRetention.maximumAge))

        assertEquals(ParserValidity.INVALID, DiagnosticPayload.parserValidityFor(DiagnosticParseResult.INVALID))
        assertEquals(emptyList<DiagnosticPayloadId>(), DiagnosticRetention.payloadIdsToDelete(listOf(boundary), now))
    }

    private fun payload(
        result: DiagnosticParseResult = DiagnosticParseResult.COMPLETED,
        error: SafeDiagnosticError? = null,
        id: Int = 1,
        capturedAt: Instant = now,
        bytes: Int = 3,
    ): DiagnosticPayload = DiagnosticPayload.create(
        DiagnosticPayloadId(uuid(id)),
        DiagnosticPayloadCapture(
            command = "D4", flow = DiagnosticCaptureFlow.WATCH_WORKOUT,
            privateBytes = ByteArray(bytes), deviceId = null, firmware = null,
            parserVersion = ParserVersion.create("d4-v1").valueOrNull()!!, capturedAt = capturedAt,
        ),
        result, error,
    ).valueOrNull()!!

    private fun uuid(last: Int) = UUID.fromString("0190029f-2f80-7000-8000-0000000000${last.toString().padStart(2, '0')}")

    private companion object {
        const val MIB = 1024 * 1024
        val now: Instant = Instant.parse("2026-07-21T10:00:00Z")
    }
}
