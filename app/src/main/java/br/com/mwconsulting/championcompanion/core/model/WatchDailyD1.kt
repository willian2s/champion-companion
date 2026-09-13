package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

const val WATCH_DAILY_D1_IMPORT_TYPE: String = "WATCH_DAILY_D1"

@JvmInline
value class WatchDailyD1SampleId(val value: java.util.UUID)

data class WatchDailyD1SampleKey(
    val deviceId: DeviceId,
    val resolvedLocalDate: LocalDate,
    val currentPosition: Int,
)

@ConsistentCopyVisibility
data class WatchDailyD1Sample private constructor(
    val id: WatchDailyD1SampleId,
    val deviceId: DeviceId,
    val importAttemptId: String,
    val parserVersion: ParserVersion,
    val currentPosition: Int,
    val totalPositions: Int,
    val resolvedLocalDate: LocalDate,
    val dayOffset: Int,
    val hour: Int,
    val minute: Int,
    val resolvedAt: Instant,
    val zoneId: ZoneId,
    val steps: Long,
    val distanceRaw: Long,
    val distanceMeters: Double,
    val caloriesRaw: Long,
    val caloriesKilocalories: Double,
    val sportValueRaw: Long,
    val bpmRaw: Int,
    val bpm: Int?,
    val pressureHighRaw: Int,
    val pressureLowRaw: Int,
    val wearRaw: Int,
    val tempOneRaw: Int,
    val importedAt: Instant,
    val updatedAt: Instant,
) {
    val key: WatchDailyD1SampleKey
        get() = WatchDailyD1SampleKey(deviceId, resolvedLocalDate, currentPosition)

    /** Alias usado por consumidores que tratam o instante como medição resolvida. */
    val resolvedMeasuredAt: Instant
        get() = resolvedAt

    companion object {
        fun create(
            id: WatchDailyD1SampleId,
            deviceId: DeviceId,
            importAttemptId: String,
            parserVersion: ParserVersion,
            currentPosition: Int,
            totalPositions: Int,
            resolvedLocalDate: LocalDate,
            dayOffset: Int,
            hour: Int,
            minute: Int,
            resolvedAt: Instant,
            zoneId: ZoneId,
            steps: Long,
            distanceRaw: Long,
            distanceMeters: Double,
            caloriesRaw: Long,
            caloriesKilocalories: Double,
            sportValueRaw: Long,
            bpmRaw: Int,
            bpm: Int?,
            pressureHighRaw: Int,
            pressureLowRaw: Int,
            wearRaw: Int,
            tempOneRaw: Int,
            importedAt: Instant,
            updatedAt: Instant,
        ): DomainResult<WatchDailyD1Sample> {
            val expectedResolvedAt = runCatching {
                resolvedLocalDate.atTime(hour, minute).atZone(zoneId).toInstant()
            }.getOrNull() ?: return DomainResult.Failure(DomainValidationError.INVALID_RANGE)

            if (importAttemptId.isBlank() || parserVersion.value.isBlank() ||
                currentPosition < 0 || totalPositions < 0 || dayOffset < 0 ||
                hour !in 0..23 || minute !in 0..59 ||
                steps !in 0L..MAX_U16 || distanceRaw !in 0L..MAX_U16 ||
                caloriesRaw !in 0L..MAX_U16 || sportValueRaw !in 0L..MAX_U16 ||
                !isRawByte(bpmRaw) || !isRawByte(pressureHighRaw) ||
                !isRawByte(pressureLowRaw) || !isRawByte(wearRaw) ||
                !isRawByte(tempOneRaw) ||
                (bpm != null && (bpm != bpmRaw || bpm !in VALID_BPM_RANGE)) ||
                (bpm == null && bpmRaw in VALID_BPM_RANGE) ||
                !distanceMeters.isFinite() || distanceMeters != distanceRaw.toDouble() ||
                !caloriesKilocalories.isFinite() ||
                caloriesKilocalories != caloriesRaw / CALORIES_SCALE ||
                resolvedAt != expectedResolvedAt || updatedAt.isBefore(importedAt)
            ) {
                return DomainResult.Failure(
                    if (updatedAt.isBefore(importedAt)) {
                        DomainValidationError.INVALID_TEMPORAL_ORDER
                    } else {
                        DomainValidationError.INVALID_RANGE
                    },
                )
            }

            return DomainResult.Success(
                WatchDailyD1Sample(
                    id = id,
                    deviceId = deviceId,
                    importAttemptId = importAttemptId,
                    parserVersion = parserVersion,
                    currentPosition = currentPosition,
                    totalPositions = totalPositions,
                    resolvedLocalDate = resolvedLocalDate,
                    dayOffset = dayOffset,
                    hour = hour,
                    minute = minute,
                    resolvedAt = resolvedAt,
                    zoneId = zoneId,
                    steps = steps,
                    distanceRaw = distanceRaw,
                    distanceMeters = distanceMeters,
                    caloriesRaw = caloriesRaw,
                    caloriesKilocalories = caloriesKilocalories,
                    sportValueRaw = sportValueRaw,
                    bpmRaw = bpmRaw,
                    bpm = bpm,
                    pressureHighRaw = pressureHighRaw,
                    pressureLowRaw = pressureLowRaw,
                    wearRaw = wearRaw,
                    tempOneRaw = tempOneRaw,
                    importedAt = importedAt,
                    updatedAt = updatedAt,
                ),
            )
        }

        private const val MAX_U16 = 65_535L
        private const val CALORIES_SCALE = 10.0
        private val VALID_BPM_RANGE = 30..200

        private fun isRawByte(value: Int): Boolean = value in 0..255
    }
}

data class PersistWatchDailyD1SamplesInput(
    val samples: List<WatchDailyD1Sample>,
)

data class WatchDailyD1PersistenceResult(
    val insertedCount: Int = 0,
    val updatedCount: Int = 0,
    val duplicateCount: Int = 0,
    val conflictCount: Int = 0,
) {
    val persistedCount: Int
        get() = insertedCount + updatedCount + duplicateCount
}

data class WatchDailyD1ImportMetadata(
    val deviceId: DeviceId,
    val lastAttemptAt: Instant,
    val lastSuccessAt: Instant? = null,
    val lastResult: D1HeartRateImportStatus,
    val parserVersion: ParserVersion? = null,
    val insertedCount: Int = 0,
    val updatedCount: Int = 0,
    val duplicateCount: Int = 0,
    val conflictCount: Int = 0,
    val invalidCount: Int = 0,
    val missingIndexCount: Int = 0,
    val timedOut: Boolean = false,
    val updatedAt: Instant,
)

interface WatchDailyD1Repository {
    suspend fun persist(
        input: PersistWatchDailyD1SamplesInput,
    ): RepositoryResult<WatchDailyD1PersistenceResult>

    suspend fun persist(
        samples: List<WatchDailyD1Sample>,
    ): RepositoryResult<WatchDailyD1PersistenceResult> = persist(
        PersistWatchDailyD1SamplesInput(samples),
    )

    suspend fun persistImportMetadata(
        metadata: WatchDailyD1ImportMetadata,
    ): RepositoryResult<Unit> = RepositoryResult.UnexpectedState

    fun observeImportMetadata(
        deviceId: DeviceId,
    ): Flow<WatchDailyD1ImportMetadata?> = flowOf(null)

    fun observeBetween(
        deviceId: DeviceId,
        fromInclusive: LocalDate,
        untilExclusive: LocalDate,
    ): Flow<List<WatchDailyD1Sample>> = flowOf(emptyList())

    suspend fun findByNaturalKey(
        key: WatchDailyD1SampleKey,
    ): RepositoryResult<WatchDailyD1Sample?> = RepositoryResult.UnexpectedState
}
