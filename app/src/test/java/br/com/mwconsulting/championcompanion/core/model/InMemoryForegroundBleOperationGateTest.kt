package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryForegroundBleOperationGateTest {
    @Test
    fun `gate rejects second owner while first permit is active`() = runBlocking {
        val gate = InMemoryForegroundBleOperationGate()
        val first = gate.tryAcquire(
            ForegroundBleOperationRequest(
                owner = ForegroundBleOperationOwner.DAILY_STEPS_REFRESH,
                operationId = "refresh-1",
                requestedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        val second = gate.tryAcquire(
            ForegroundBleOperationRequest(
                owner = ForegroundBleOperationOwner.MANUAL_HEART_RATE,
                operationId = "manual-1",
                requestedAt = Instant.parse("2026-01-01T00:00:01Z"),
            ),
        )

        assertTrue(first is ForegroundBleOperationAcquireResult.Acquired)
        assertTrue(second is ForegroundBleOperationAcquireResult.Rejected)
        assertEquals(
            ForegroundBleOperationOwner.DAILY_STEPS_REFRESH,
            (second as ForegroundBleOperationAcquireResult.Rejected).activeOperation.owner,
        )
    }

    @Test
    fun `release clears active owner only for matching permit`() = runBlocking {
        val gate = InMemoryForegroundBleOperationGate()
        val acquired = gate.tryAcquire(
            ForegroundBleOperationRequest(
                owner = ForegroundBleOperationOwner.MANUAL_HEART_RATE,
                operationId = "manual-1",
                requestedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        ) as ForegroundBleOperationAcquireResult.Acquired

        gate.release(
            ForegroundBleOperationPermit(
                owner = ForegroundBleOperationOwner.MANUAL_HEART_RATE,
                operationId = "other",
                acquiredAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        assertEquals(
            ForegroundBleOperationOwner.MANUAL_HEART_RATE,
            gate.observeActiveOperation().first()?.owner,
        )

        gate.release(acquired.permit)

        assertNull(gate.observeActiveOperation().first())
    }
}
