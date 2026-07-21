package br.com.mwconsulting.championcompanion.core.bluetooth

import java.time.Instant
import java.util.UUID

sealed interface ChampionGattSessionEvent {
    val sessionId: ChampionSessionId
    val occurredAt: Instant

    data class ConnectionStateChanged(
        override val sessionId: ChampionSessionId,
        override val occurredAt: Instant,
        val transportKey: String,
        val isConnected: Boolean,
        val detail: SafeGattDetail?,
    ) : ChampionGattSessionEvent

    data class ServicesDiscovered(
        override val sessionId: ChampionSessionId,
        override val occurredAt: Instant,
        val serviceUuids: Set<UUID>,
        val detail: SafeGattDetail?,
    ) : ChampionGattSessionEvent

    data class CharacteristicRead(
        override val sessionId: ChampionSessionId,
        override val occurredAt: Instant,
        val channel: ChampionChannelAlias?,
        val packet: ChampionPacket?,
        val detail: SafeGattDetail?,
    ) : ChampionGattSessionEvent

    data class CharacteristicWrite(
        override val sessionId: ChampionSessionId,
        override val occurredAt: Instant,
        val channel: ChampionChannelAlias?,
        val detail: SafeGattDetail?,
    ) : ChampionGattSessionEvent

    data class CharacteristicChanged(
        override val sessionId: ChampionSessionId,
        override val occurredAt: Instant,
        val channel: ChampionChannelAlias?,
        val packet: ChampionPacket?,
    ) : ChampionGattSessionEvent

    data class DescriptorWrite(
        override val sessionId: ChampionSessionId,
        override val occurredAt: Instant,
        val channel: ChampionChannelAlias?,
        val descriptorUuid: UUID,
        val detail: SafeGattDetail?,
    ) : ChampionGattSessionEvent

    data class MtuChanged(
        override val sessionId: ChampionSessionId,
        override val occurredAt: Instant,
        val mtu: Int,
        val detail: SafeGattDetail?,
    ) : ChampionGattSessionEvent

    data class Closed(
        override val sessionId: ChampionSessionId,
        override val occurredAt: Instant,
        val transportKey: String,
    ) : ChampionGattSessionEvent
}

class ChampionGattCallbackBridge(
    private val now: () -> Instant = Instant::now,
) {
    fun connectionStateChanged(
        sessionId: ChampionSessionId,
        transportKey: String,
        isConnected: Boolean,
        detail: SafeGattDetail?,
    ): ChampionGattSessionEvent.ConnectionStateChanged = ChampionGattSessionEvent.ConnectionStateChanged(
        sessionId = sessionId,
        occurredAt = now(),
        transportKey = transportKey,
        isConnected = isConnected,
        detail = detail,
    )

    fun servicesDiscovered(
        sessionId: ChampionSessionId,
        serviceUuids: Set<UUID>,
        detail: SafeGattDetail?,
    ): ChampionGattSessionEvent.ServicesDiscovered = ChampionGattSessionEvent.ServicesDiscovered(
        sessionId = sessionId,
        occurredAt = now(),
        serviceUuids = serviceUuids.toSet(),
        detail = detail,
    )

    fun characteristicRead(
        sessionId: ChampionSessionId,
        characteristicUuid: UUID,
        value: ByteArray,
        detail: SafeGattDetail?,
    ): ChampionGattSessionEvent.CharacteristicRead {
        val occurredAt = now()
        val channel = ChampionUuidCatalog.findChannel(characteristicUuid)?.alias
        return ChampionGattSessionEvent.CharacteristicRead(
            sessionId = sessionId,
            occurredAt = occurredAt,
            channel = channel,
            packet = channel?.let {
                ChampionPacket(
                    sessionId = sessionId,
                    channel = it,
                    direction = ChampionPacketDirection.INBOUND,
                    observedAt = occurredAt,
                    commandCode = value.firstOrNull()?.toInt()?.and(0xFF),
                    bytes = value,
                )
            },
            detail = detail,
        )
    }

    fun characteristicWrite(
        sessionId: ChampionSessionId,
        characteristicUuid: UUID,
        detail: SafeGattDetail?,
    ): ChampionGattSessionEvent.CharacteristicWrite = ChampionGattSessionEvent.CharacteristicWrite(
        sessionId = sessionId,
        occurredAt = now(),
        channel = ChampionUuidCatalog.findChannel(characteristicUuid)?.alias,
        detail = detail,
    )

    fun characteristicChanged(
        sessionId: ChampionSessionId,
        characteristicUuid: UUID,
        value: ByteArray,
    ): ChampionGattSessionEvent.CharacteristicChanged {
        val occurredAt = now()
        val channel = ChampionUuidCatalog.findChannel(characteristicUuid)?.alias
        return ChampionGattSessionEvent.CharacteristicChanged(
            sessionId = sessionId,
            occurredAt = occurredAt,
            channel = channel,
            packet = channel?.let {
                ChampionPacket(
                    sessionId = sessionId,
                    channel = it,
                    direction = ChampionPacketDirection.INBOUND,
                    observedAt = occurredAt,
                    commandCode = value.firstOrNull()?.toInt()?.and(0xFF),
                    bytes = value,
                )
            },
        )
    }

    fun descriptorWrite(
        sessionId: ChampionSessionId,
        characteristicUuid: UUID?,
        descriptorUuid: UUID,
        detail: SafeGattDetail?,
    ): ChampionGattSessionEvent.DescriptorWrite = ChampionGattSessionEvent.DescriptorWrite(
        sessionId = sessionId,
        occurredAt = now(),
        channel = characteristicUuid?.let(ChampionUuidCatalog::findChannel)?.alias,
        descriptorUuid = descriptorUuid,
        detail = detail,
    )

    fun mtuChanged(
        sessionId: ChampionSessionId,
        mtu: Int,
        detail: SafeGattDetail?,
    ): ChampionGattSessionEvent.MtuChanged = ChampionGattSessionEvent.MtuChanged(
        sessionId = sessionId,
        occurredAt = now(),
        mtu = mtu,
        detail = detail,
    )

    fun closed(
        sessionId: ChampionSessionId,
        transportKey: String,
    ): ChampionGattSessionEvent.Closed = ChampionGattSessionEvent.Closed(
        sessionId = sessionId,
        occurredAt = now(),
        transportKey = transportKey,
    )
}
