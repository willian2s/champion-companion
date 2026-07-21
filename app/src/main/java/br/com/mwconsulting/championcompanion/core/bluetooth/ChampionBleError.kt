package br.com.mwconsulting.championcompanion.core.bluetooth

import kotlin.time.Duration

sealed interface ChampionBleError {
    val isRecoverable: Boolean

    data object BleNotSupported : ChampionBleError {
        override val isRecoverable: Boolean = false
    }

    data object BluetoothDisabled : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data object ScanPermissionMissing : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data object ConnectPermissionMissing : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data object LegacyPermissionMissing : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data object LocationPermissionRequired : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data object ScanAlreadyInProgress : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data object OperationUnavailable : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data object DeviceNotFound : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data class ScanFailed(val detail: SafeGattDetail? = null) : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data class ConnectionFailed(val detail: SafeGattDetail? = null) : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data class UnexpectedDisconnect(val detail: SafeGattDetail? = null) : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data class DiscoveryFailed(val detail: SafeGattDetail? = null) : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data class MissingRequiredService(val service: ChampionServiceAlias) : ChampionBleError {
        override val isRecoverable: Boolean = false
    }

    data class MissingRequiredChannel(val channel: ChampionChannelAlias) : ChampionBleError {
        override val isRecoverable: Boolean = false
    }

    data class MissingOptionalChannel(val channel: ChampionChannelAlias) : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data class IncompatibleChannelProperties(val channel: ChampionChannelAlias) : ChampionBleError {
        override val isRecoverable: Boolean = false
    }

    data class OperationRejected(val operation: ChampionOperationType) : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data class CallbackFailure(
        val operation: ChampionOperationType,
        val detail: SafeGattDetail? = null,
    ) : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data class Timeout(
        val operation: ChampionOperationType,
        val after: Duration,
    ) : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data class QueueClosed(val reason: ChampionDisconnectReason) : ChampionBleError {
        override val isRecoverable: Boolean = false
    }

    data class OperationCancelled(val reason: ChampionCancellationReason) : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data class SubscriptionFailed(
        val channel: ChampionChannelAlias,
        val detail: SafeGattDetail? = null,
    ) : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data class InvalidPayload(
        val channel: ChampionChannelAlias,
        val size: Int,
    ) : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data class StaleSession(
        val expected: ChampionSessionId,
        val received: ChampionSessionId,
    ) : ChampionBleError {
        override val isRecoverable: Boolean = false
    }

    data object CleanupInProgress : ChampionBleError {
        override val isRecoverable: Boolean = true
    }

    data object UnexpectedInternal : ChampionBleError {
        override val isRecoverable: Boolean = false
    }
}

data class SafeGattDetail(
    val statusCode: Int? = null,
    val statusLabel: String? = null,
)

fun ChampionBleError.toSafeMessage(): String = when (this) {
    ChampionBleError.BleNotSupported -> "Bluetooth BLE não suportado neste dispositivo."
    ChampionBleError.BluetoothDisabled -> "Ative o Bluetooth para continuar."
    ChampionBleError.ScanPermissionMissing -> "Permissão de Bluetooth para busca não concedida."
    ChampionBleError.ConnectPermissionMissing -> "Permissão de Bluetooth para conexão não concedida."
    ChampionBleError.LegacyPermissionMissing -> "Permissões legadas de Bluetooth não concedidas."
    ChampionBleError.LocationPermissionRequired -> "Permissão de localização necessária para esta operação."
    ChampionBleError.ScanAlreadyInProgress -> "Já existe uma busca por dispositivo em andamento."
    ChampionBleError.OperationUnavailable -> "Operação indisponível no estado atual."
    ChampionBleError.DeviceNotFound -> "Dispositivo Champion não encontrado."
    is ChampionBleError.ScanFailed -> "Não foi possível concluir a busca por dispositivo."
    is ChampionBleError.ConnectionFailed -> "Não foi possível conectar ao dispositivo."
    is ChampionBleError.UnexpectedDisconnect -> "Conexão com dispositivo encerrada inesperadamente."
    is ChampionBleError.DiscoveryFailed -> "Não foi possível descobrir os canais do dispositivo."
    is ChampionBleError.MissingRequiredService -> "Serviço Bluetooth obrigatório ausente."
    is ChampionBleError.MissingRequiredChannel -> "Canal Bluetooth obrigatório ausente."
    is ChampionBleError.MissingOptionalChannel -> "Canal Bluetooth opcional necessário para esta etapa está ausente."
    is ChampionBleError.IncompatibleChannelProperties -> "Canal Bluetooth incompatível com operação necessária."
    is ChampionBleError.OperationRejected -> "Operação Bluetooth rejeitada."
    is ChampionBleError.CallbackFailure -> "Operação Bluetooth falhou durante execução."
    is ChampionBleError.Timeout -> "Operação Bluetooth expirou."
    is ChampionBleError.QueueClosed -> "Sessão Bluetooth já foi encerrada."
    is ChampionBleError.OperationCancelled -> "Operação Bluetooth cancelada."
    is ChampionBleError.SubscriptionFailed -> "Não foi possível ativar notificações do dispositivo."
    is ChampionBleError.InvalidPayload -> "Pacote Bluetooth recebido em formato inválido."
    is ChampionBleError.StaleSession -> "Evento de sessão Bluetooth antiga foi descartado."
    ChampionBleError.CleanupInProgress -> "Limpeza da sessão Bluetooth em andamento."
    ChampionBleError.UnexpectedInternal -> "Falha interna ao executar operação Bluetooth."
}
