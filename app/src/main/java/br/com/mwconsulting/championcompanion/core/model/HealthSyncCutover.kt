package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Cutover imutável solicitado para separar operações diárias legacy da projeção
 * granular.
 *
 * A data solicitada continua preservada no banco. Enquanto a projeção granular
 * D1 de passos permanecer fail-closed/deferred, [localDate] expõe uma data
 * efetiva futura para manter o caminho incremental FEA1/Spec 027 ativo. Quando
 * houver uma projeção remota granular comprovadamente segura, esta política deve
 * ser removida junto da ativação explícita do cutover real.
 */
class HealthSyncCutover(
    val deviceId: DeviceId,
    localDate: LocalDate,
    val zoneId: ZoneId,
    val activatedAt: Instant,
) {
    val requestedLocalDate: LocalDate = localDate

    val localDate: LocalDate = if (GRANULAR_D1_STEPS_REMOTE_PROJECTION_READY) {
        requestedLocalDate
    } else {
        LEGACY_DAILY_STEPS_FALLBACK_CUTOVER_DATE
    }

    val isDeferred: Boolean
        get() = localDate != requestedLocalDate

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
        "HealthSyncCutover(deviceId=$deviceId, requestedLocalDate=$requestedLocalDate, localDate=$localDate, zoneId=$zoneId, activatedAt=$activatedAt)"

    private companion object {
        // D1 granular STEPS ainda não possui intervalo remoto/não sobreposição
        // suficientemente provados. Não desligar o fallback seguro antes disso.
        const val GRANULAR_D1_STEPS_REMOTE_PROJECTION_READY: Boolean = false
        val LEGACY_DAILY_STEPS_FALLBACK_CUTOVER_DATE: LocalDate = LocalDate.of(9999, 12, 31)
    }
}
