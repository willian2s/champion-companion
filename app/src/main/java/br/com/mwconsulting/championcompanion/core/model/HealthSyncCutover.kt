package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Intenção imutável de cutover diário persistida por dispositivo.
 *
 * [requestedLocalDate] preserva a data registrada no Room v19. A ativação física
 * do STEPS granular fica congelada em 2026-09-14 para instalações que já tinham
 * uma intenção anterior, evitando misturar no mesmo dia a família DailySummary
 * já aceita com buckets D1. Instalações ativadas depois dessa data usam a própria
 * data solicitada.
 */
class HealthSyncCutover(
    val deviceId: DeviceId,
    localDate: LocalDate,
    val zoneId: ZoneId,
    val activatedAt: Instant,
) {
    val requestedLocalDate: LocalDate = localDate

    /** Primeiro dia que pode usar D1 granular quando não existir STEPS Daily legado. */
    val localDate: LocalDate = maxOf(requestedLocalDate, GRANULAR_STEPS_ACTIVATION_DATE)

    val isDeferred: Boolean = localDate.isAfter(requestedLocalDate)

    override fun equals(other: Any?): Boolean =
        this === other || other is HealthSyncCutover &&
            deviceId == other.deviceId &&
            requestedLocalDate == other.requestedLocalDate &&
            zoneId == other.zoneId &&
            activatedAt == other.activatedAt

    override fun hashCode(): Int {
        var result = deviceId.hashCode()
        result = 31 * result + requestedLocalDate.hashCode()
        result = 31 * result + zoneId.hashCode()
        result = 31 * result + activatedAt.hashCode()
        return result
    }

    override fun toString(): String =
        "HealthSyncCutover(deviceId=$deviceId, requestedLocalDate=$requestedLocalDate, localDate=$localDate, zoneId=$zoneId, activatedAt=$activatedAt, isDeferred=$isDeferred)"

    companion object {
        val GRANULAR_STEPS_ACTIVATION_DATE: LocalDate = LocalDate.of(2026, 9, 14)
    }
}
