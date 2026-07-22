package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionTransportSession

enum class DeviceConnectionProtocolState {
    TRANSPORT_READY,
    HANDSHAKING,
    HANDSHAKE_CONFIRMED,
    HANDSHAKE_ASSUMED,
}

data class DeviceConnectionBootstrapResult(
    val transport: ChampionTransportSession,
    val protocolState: DeviceConnectionProtocolState,
)
