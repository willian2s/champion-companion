package br.com.mwconsulting.championcompanion.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DomainMetadataTest {
    @Test
    fun `missing value preserves explicit reason`() {
        val distance: DomainValue<Double> = DomainValue.Absent(
            AbsenceReason.NOT_PROVIDED_BY_SOURCE,
        )

        assertEquals(
            DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE),
            distance,
        )
    }

    @Test
    fun `range validation returns safe domain error`() {
        assertEquals(
            DomainResult.Failure(DomainValidationError.INVALID_RANGE),
            DomainValidation.withinRange(29, 30..220),
        )
    }
}
