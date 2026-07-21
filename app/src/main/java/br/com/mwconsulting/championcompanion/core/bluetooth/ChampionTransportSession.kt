package br.com.mwconsulting.championcompanion.core.bluetooth

import kotlinx.coroutines.Job

data class ChampionTransportSession(
    val session: ChampionSession,
    val capabilities: ChampionSessionCapabilities,
    val queue: ChampionGattQueue,
    val notifications: ChampionNotificationHub,
    val streams: ChampionTransportStreams,
    internal val internalJobs: List<Job>,
)
