package br.com.mwconsulting.championcompanion.core.bluetooth

import java.time.Instant
import java.util.UUID

@JvmInline
value class ChampionSessionId(val value: UUID) {
    companion object {
        fun random(): ChampionSessionId = ChampionSessionId(UUID.randomUUID())
    }
}

data class ChampionSession(
    val id: ChampionSessionId,
    val transportKey: String,
    val connectedAt: Instant,
) {
    init {
        require(transportKey.isNotBlank()) { "transportKey must not be blank" }
    }
}

data class ChampionDiscoveredDevice(
    val transportKey: String,
    val name: String?,
    val localName: String?,
    val observedAt: Instant,
) {
    init {
        require(transportKey.isNotBlank()) { "transportKey must not be blank" }
        require(!name.isNullOrBlank() || !localName.isNullOrBlank()) {
            "name or localName must be present"
        }
    }
}

enum class ChampionDisconnectReason {
    USER_REQUESTED,
    TIMEOUT,
    TRANSPORT_FAILURE,
    STALE_SESSION,
    CLEANUP,
}

enum class ChampionCancellationReason {
    USER_REQUESTED,
    TIMEOUT,
    DISCONNECT,
    SESSION_REPLACED,
    CLEANUP,
}
