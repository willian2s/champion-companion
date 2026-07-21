package br.com.mwconsulting.championcompanion.core.bluetooth

import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

internal fun testSessionId(seed: Int = 1): ChampionSessionId =
    ChampionSessionId(UUID.fromString("00000000-0000-0000-0000-${seed.toString().padStart(12, '0')}"))

internal fun testSession(seed: Int = 1): ChampionSession = ChampionSession(
    id = testSessionId(seed),
    transportKey = "transport-$seed",
    connectedAt = Instant.parse("2026-01-01T00:00:00Z"),
)

internal fun testDevice(
    transportKey: String = "device-1",
    name: String? = "Champion C001",
    localName: String? = null,
): ChampionDiscoveredDevice = ChampionDiscoveredDevice(
    transportKey = transportKey,
    name = name,
    localName = localName,
    observedAt = Instant.parse("2026-01-01T00:00:00Z"),
)

internal fun testPacket(
    sessionId: ChampionSessionId = testSessionId(),
    channel: ChampionChannelAlias = ChampionChannelAlias.F008_NOTIFY,
    commandCode: Int? = 0xA1,
    bytes: ByteArray = byteArrayOf(0x01, 0x02, 0x03),
): ChampionPacket = ChampionPacket(
    sessionId = sessionId,
    channel = channel,
    direction = ChampionPacketDirection.INBOUND,
    observedAt = Instant.parse("2026-01-01T00:00:00Z"),
    commandCode = commandCode,
    bytes = bytes,
)

internal fun testOperation(
    sessionId: ChampionSessionId = testSessionId(),
    type: ChampionOperationType = ChampionOperationType.WRITE_CHARACTERISTIC,
    target: ChampionGattTarget = ChampionGattTarget.Channel(ChampionChannelAlias.F008_WRITE),
    writeType: ChampionWriteType? = null,
    timeout: Duration = 100.milliseconds,
    retryPolicy: ChampionRetryPolicy = ChampionRetryPolicy.None,
    payload: ByteArray? = null,
): ChampionGattOperation = ChampionGattOperation.create(
    sessionId = sessionId,
    type = type,
    target = target,
    writeType = writeType,
    timeout = timeout,
    retryPolicy = retryPolicy,
    payload = payload,
)

internal fun capabilitySnapshot(vararg channels: ChampionChannelAlias): ChampionCapabilitySnapshot {
    val map = channels.associateWith { channel ->
        ChampionChannelCapability(
            channel = channel,
            properties = ChampionUuidCatalog.definitionOf(channel).properties,
        )
    }
    return ChampionCapabilitySnapshot(
        requiredChannels = ChampionUuidCatalog.requiredTransportChannels.intersect(map.keys),
        availableChannels = map.keys,
        channels = map,
    )
}

internal fun sessionCapabilities(
    sessionId: ChampionSessionId = testSessionId(),
    vararg channels: ChampionChannelAlias,
): ChampionSessionCapabilities {
    val snapshot = capabilitySnapshot(*channels)
    return ChampionSessionCapabilities(
        sessionId = sessionId,
        snapshot = snapshot,
        discoveredServices = channels.mapTo(linkedSetOf()) { it.service },
        optionalChannelsAvailable = snapshot.availableChannels.intersect(ChampionUuidCatalog.optionalChannels),
        defaultSubscribedChannels = ChampionUuidCatalog.defaultSubscribedChannels.intersect(snapshot.availableChannels),
    )
}

internal fun discoverySnapshot(vararg channels: Pair<ChampionChannelAlias, Set<ChampionChannelProperty>>): ChampionDiscoverySnapshot {
    val services = linkedMapOf<UUID, ChampionDiscoveredService>()
    channels.forEach { (channel, properties) ->
        val serviceUuid = ChampionUuidCatalog.serviceUuid(channel.service)
        val service = services.getOrPut(serviceUuid) {
            ChampionDiscoveredService(
                alias = channel.service,
                uuid = serviceUuid,
                characteristics = linkedMapOf(),
            )
        }
        services[serviceUuid] = service.copy(
            characteristics = service.characteristics + (
                ChampionUuidCatalog.channelUuid(channel) to ChampionDiscoveredCharacteristic(
                    uuid = ChampionUuidCatalog.channelUuid(channel),
                    properties = properties,
                )
            ),
        )
    }
    return ChampionDiscoverySnapshot(services = services)
}

internal suspend fun awaitCondition(
    timeout: Duration = 2_000.milliseconds,
    interval: Duration = 10.milliseconds,
    predicate: () -> Boolean,
) {
    val deadline = System.nanoTime() + timeout.inWholeNanoseconds
    while (!predicate()) {
        check(System.nanoTime() < deadline) { "condition not met within $timeout" }
        delay(interval)
    }
}
