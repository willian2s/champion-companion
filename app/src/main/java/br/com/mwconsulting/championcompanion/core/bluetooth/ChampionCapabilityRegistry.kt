package br.com.mwconsulting.championcompanion.core.bluetooth

import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidGattDiscoverySnapshot

object ChampionCapabilityRegistry {
    fun resolve(
        sessionId: ChampionSessionId,
        discovery: AndroidGattDiscoverySnapshot,
    ): ChampionBleResult<ChampionSessionCapabilities> = resolve(
        sessionId = sessionId,
        discovery = discovery.toChampionDiscoverySnapshot(),
    )

    fun resolve(
        sessionId: ChampionSessionId,
        discovery: ChampionDiscoverySnapshot,
    ): ChampionBleResult<ChampionSessionCapabilities> {
        val servicesByAlias = discovery.services.values.mapNotNull { service ->
            service.alias?.let { alias -> alias to service }
        }.toMap()

        val requiredChannels = ChampionUuidCatalog.requiredTransportChannels
        val resolvedChannels = linkedMapOf<ChampionChannelAlias, ChampionChannelCapability>()

        for (channelAlias in ChampionUuidCatalog.channels.keys) {
            val definition = ChampionUuidCatalog.definitionOf(channelAlias)
            val service = servicesByAlias[definition.alias.service]

            if (service == null) {
                if (channelAlias in requiredChannels) {
                    return ChampionBleResult.Failure(
                        ChampionBleError.MissingRequiredService(definition.alias.service),
                    )
                }
                continue
            }

            val characteristic = service.characteristics[definition.uuid]
            if (characteristic == null) {
                if (channelAlias in requiredChannels) {
                    return ChampionBleResult.Failure(
                        ChampionBleError.MissingRequiredChannel(channelAlias),
                    )
                }
                continue
            }

            if (!characteristic.properties.containsAll(definition.properties)) {
                return ChampionBleResult.Failure(
                    ChampionBleError.IncompatibleChannelProperties(channelAlias),
                )
            }

            resolvedChannels[channelAlias] = ChampionChannelCapability(
                channel = channelAlias,
                properties = characteristic.properties,
            )
        }

        val availableChannels = resolvedChannels.keys.toSet()
        val discoveredServices = servicesByAlias.keys
        val snapshot = ChampionCapabilitySnapshot(
            requiredChannels = requiredChannels,
            availableChannels = availableChannels,
            channels = resolvedChannels,
        )

        return ChampionBleResult.Success(
            ChampionSessionCapabilities(
                sessionId = sessionId,
                snapshot = snapshot,
                discoveredServices = discoveredServices,
                optionalChannelsAvailable = availableChannels.intersect(ChampionUuidCatalog.optionalChannels),
                defaultSubscribedChannels = ChampionUuidCatalog.defaultSubscribedChannels.intersect(availableChannels),
            ),
        )
    }
}
