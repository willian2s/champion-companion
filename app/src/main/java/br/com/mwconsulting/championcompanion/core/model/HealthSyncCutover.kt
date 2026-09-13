package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Intenção imutável de cutover diário persistida por dispositivo.
 *
 * [requestedLocalDate] preserva a data registrada no Room v19. A projeção D1 de
 * passos ainda não possui intervalo remoto/não sobreposição suficientemente
 * provados; por isso [localDate] permanece em uma data efetiva futura e o caminho
 * incremental FEA1/Specs 016 e 027 continua ativo.
 *
 * Ativar o cutover real no futuro exige uma decisão/migração explícita. Não há
 * flag de build que possa reinterpretar silenciosamente datas antigas.
 */
class HealthSyncCutover(
    val deviceId: DeviceId,
    localDate: LocalDate,
    val zoneId: ZoneId,
    val activatedAt: Instant,
) {
    val requestedLocalDate: LocalDate = localDate

    /** Data efetiva enquanto a projeção granular remota continuar deferred. */
    val localDate: LocalDate = DEFERRED_EFFECTIVE_CUTOVER_DATE

    val isDeferred: Boolean = true

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

    private companion object {
        val DEFERRED_EFFECTIVE_CUTOVER_DATE: LocalDate = LocalDate.of(9999, 12, 31)
    }
}
