package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class AppWorkoutSessionTest {
    @Test
    fun `interrupted session recovers and accumulates active and paused durations`() {
        val paused = session().start(at(0)).valueOrNull()!!.pause(at(600)).valueOrNull()!!
        val interrupted = paused.interrupt(at(900)).valueOrNull()!!
        val recovered = interrupted.recover(at(1_200)).valueOrNull()!!
        val finished = recovered.finish(WorkoutActivityId(uuid(2)), at(1_800)).valueOrNull()!!

        assertEquals(Duration.ofMinutes(20), finished.activeDuration)
        assertEquals(Duration.ofMinutes(5), finished.pausedDuration)
        assertEquals(AppWorkoutSessionState.FINISHED, finished.state)
        assertEquals(WorkoutActivityId(uuid(2)), finished.workoutActivityId)
    }

    @Test
    fun `terminal sessions reject invalid transitions`() {
        val finished = session().start(at(0)).valueOrNull()!!
            .finish(WorkoutActivityId(uuid(2)), at(60)).valueOrNull()!!

        assertEquals(DomainResult.Failure(DomainValidationError.INVALID_STATE), finished.resume(at(61)))
        assertEquals(DomainResult.Failure(DomainValidationError.INVALID_STATE), finished.discard())
    }

    @Test
    fun `interrupted session can be discarded`() {
        val discarded = session().start(at(0)).valueOrNull()!!
            .interrupt(at(30)).valueOrNull()!!
            .discard().valueOrNull()!!

        assertEquals(AppWorkoutSessionState.DISCARDED, discarded.state)
    }

    private fun session() = AppWorkoutSession.create(
        AppWorkoutSessionId(uuid(1)), WorkoutType.WALKING, at(0),
    ).valueOrNull()!!

    private fun at(seconds: Long) = Instant.parse("2026-07-21T10:00:00Z").plusSeconds(seconds)

    private fun uuid(last: Int) = UUID.fromString("0190029f-2f80-7000-8000-0000000000${last.toString().padStart(2, '0')}")
}
