package br.com.mwconsulting.championcompanion.core.model

import java.time.Clock
import java.util.Random
import java.util.UUID

@JvmInline
value class DeviceId(val value: UUID)

@JvmInline
value class DailySummaryId(val value: UUID)

@JvmInline
value class HeartRateMeasurementId(val value: UUID)

@JvmInline
value class WorkoutActivityId(val value: UUID)

@JvmInline
value class WatchWorkoutImportId(val value: UUID)

@JvmInline
value class AppWorkoutSessionId(val value: UUID)

@JvmInline
value class TreadmillCalibrationId(val value: UUID)

@JvmInline
value class HealthSyncOperationId(val value: UUID)

@JvmInline
value class DiagnosticPayloadId(val value: UUID)

@JvmInline
value class UserProfileId(val value: UUID)

@JvmInline
value class WeightMeasurementId(val value: UUID)

@JvmInline
value class ParserVersion private constructor(val value: String) {
    companion object {
        fun create(value: String): DomainResult<ParserVersion> = when (
            val validation = DomainValidation.requiredText(value)
        ) {
            is DomainResult.Success -> DomainResult.Success(ParserVersion(validation.value))
            is DomainResult.Failure -> validation
        }
    }
}

object UuidV7 {
    fun parse(value: String): DomainResult<UUID> = try {
        val uuid = UUID.fromString(value)
        if (uuid.version() == 7 && uuid.variant() == 2) {
            DomainResult.Success(uuid)
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
        }
    } catch (_: IllegalArgumentException) {
        DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
    }

    fun timestamp(uuid: UUID): DomainResult<Long> =
        if (uuid.version() == 7 && uuid.variant() == 2) {
            DomainResult.Success(uuid.mostSignificantBits ushr 16)
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
        }
}

class UuidV7Generator(
    private val clock: Clock = Clock.systemUTC(),
    private val random: Random = Random(),
) {
    private var lastTimestampMillis = -1L
    private var randomA = 0
    private var randomB = 0L

    @Synchronized
    fun next(): UUID {
        val currentTimestamp = maxOf(clock.millis(), lastTimestampMillis)
        if (currentTimestamp > lastTimestampMillis) {
            lastTimestampMillis = currentTimestamp
            randomA = random.nextInt(1 shl 12)
            randomB = random.nextLong() ushr 2
        } else {
            incrementRandomness()
        }

        val mostSignificantBits =
            (lastTimestampMillis shl 16) or (7L shl 12) or randomA.toLong()
        val leastSignificantBits = Long.MIN_VALUE or randomB
        return UUID(mostSignificantBits, leastSignificantBits)
    }

    private fun incrementRandomness() {
        if (randomB == MAX_RANDOM_B) {
            randomB = 0
            if (randomA == MAX_RANDOM_A) {
                lastTimestampMillis += 1
                randomA = 0
            } else {
                randomA += 1
            }
        } else {
            randomB += 1
        }
    }

    private companion object {
        const val MAX_RANDOM_A = (1 shl 12) - 1
        const val MAX_RANDOM_B = 0x3fff_ffff_ffff_ffffL
    }
}
