package br.com.mwconsulting.championcompanion.core.bluetooth

sealed interface ChampionBleState {
    data object Disconnected : ChampionBleState

    data class Scanning(
        val request: ChampionScanRequest,
    ) : ChampionBleState

    data class Connecting(
        val session: ChampionSession,
        val device: ChampionDiscoveredDevice,
    ) : ChampionBleState

    data class DiscoveringServices(
        val session: ChampionSession,
    ) : ChampionBleState

    data class ResolvingChannels(
        val session: ChampionSession,
    ) : ChampionBleState

    data class ConfiguringSubscriptions(
        val session: ChampionSession,
        val capabilities: ChampionCapabilitySnapshot,
    ) : ChampionBleState

    data class TransportReady(
        val session: ChampionSession,
        val capabilities: ChampionCapabilitySnapshot,
    ) : ChampionBleState

    data class Disconnecting(
        val session: ChampionSession?,
        val reason: ChampionDisconnectReason,
    ) : ChampionBleState

    data class Failed(
        val session: ChampionSession?,
        val error: ChampionBleError,
    ) : ChampionBleState
}
