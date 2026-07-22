package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

data class CanonicalTime(
    val instant: Instant,
)

data class LocalCalendarDate(
    val value: LocalDate,
)

data class ObservedWatchTime(
    val localDateTime: LocalDateTime,
    val resolution: WatchTimeResolution = WatchTimeResolution.Unresolved,
) {
    fun resolveIn(zoneId: ZoneId): ObservedWatchTime = copy(
        resolution = WatchTimeResolver.resolve(localDateTime, zoneId),
    )
}

sealed interface WatchTimeResolution {
    data object Unresolved : WatchTimeResolution

    data class Resolved(
        val instant: Instant,
        val zoneId: ZoneId,
        val offset: ZoneOffset,
    ) : WatchTimeResolution

    data class Ambiguous(
        val zoneId: ZoneId,
        val offsets: List<ZoneOffset>,
    ) : WatchTimeResolution

    data class Nonexistent(
        val zoneId: ZoneId,
    ) : WatchTimeResolution
}

object WatchTimeResolver {
    fun resolve(localDateTime: LocalDateTime, zoneId: ZoneId): WatchTimeResolution {
        val offsets = zoneId.rules.getValidOffsets(localDateTime)
        return when (offsets.size) {
            0 -> WatchTimeResolution.Nonexistent(zoneId)
            1 -> WatchTimeResolution.Resolved(
                instant = localDateTime.toInstant(offsets.single()),
                zoneId = zoneId,
                offset = offsets.single(),
            )
            else -> WatchTimeResolution.Ambiguous(zoneId, offsets)
        }
    }
}

object LocalDateResolver {
    fun resolve(instant: Instant, zoneId: ZoneId): LocalDate = instant.atZone(zoneId).toLocalDate()
}

fun Instant.toLocalDateIn(zoneId: ZoneId): LocalDate = LocalDateResolver.resolve(this, zoneId)
