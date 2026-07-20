package br.com.mwconsulting.championcompanion.core.model

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Random
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainIdsTest {
    @Test
    fun `uuidv7 carries generator timestamp and uses version seven`() {
        val instant = Instant.parse("2026-07-20T12:34:56.789Z")
        val generator = UuidV7Generator(
            clock = Clock.fixed(instant, ZoneOffset.UTC),
            random = Random(10),
        )

        val identifier = generator.next()

        assertEquals(7, identifier.version())
        assertEquals(2, identifier.variant())
        assertEquals(instant.toEpochMilli(), UuidV7.timestamp(identifier).valueOrNull())
    }

    @Test
    fun `uuidv7 values remain ordered inside same millisecond`() {
        val generator = UuidV7Generator(
            clock = Clock.fixed(Instant.parse("2026-07-20T12:34:56Z"), ZoneOffset.UTC),
            random = Random(42),
        )

        val first = generator.next()
        val second = generator.next()
        val third = generator.next()

        assertTrue(first < second)
        assertTrue(second < third)
    }

    @Test
    fun `uuidv7 parser rejects identifiers from another version`() {
        val versionFour = UUID.fromString("123e4567-e89b-42d3-a456-426614174000")

        assertEquals(
            DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER),
            UuidV7.parse(versionFour.toString()),
        )
    }

    @Test
    fun `parser version rejects empty value without exposing input`() {
        assertEquals(
            DomainResult.Failure(DomainValidationError.EMPTY_VALUE),
            ParserVersion.create(" "),
        )
    }
}
