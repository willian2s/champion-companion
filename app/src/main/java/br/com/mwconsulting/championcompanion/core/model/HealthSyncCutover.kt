package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Cutover imutável que separa operações diárias legacy da projeção granular. */
data class HealthSyncCutover(
    val deviceId: DeviceId,
    val localDate: LocalDate,
    val zoneId: ZoneId,
    val activatedAt: Instant,
)
