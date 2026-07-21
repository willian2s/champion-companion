package br.com.mwconsulting.championcompanion.core.bluetooth

import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidGattCharacteristicView
import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidGattDiscoverySnapshot
import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidGattServiceView
import java.util.UUID

data class ChampionDiscoveredCharacteristic(
    val uuid: UUID,
    val properties: Set<ChampionChannelProperty>,
)

data class ChampionDiscoveredService(
    val alias: ChampionServiceAlias?,
    val uuid: UUID,
    val characteristics: Map<UUID, ChampionDiscoveredCharacteristic>,
)

data class ChampionDiscoverySnapshot(
    val services: Map<UUID, ChampionDiscoveredService>,
)

data class ChampionSessionCapabilities(
    val sessionId: ChampionSessionId,
    val snapshot: ChampionCapabilitySnapshot,
    val discoveredServices: Set<ChampionServiceAlias>,
    val optionalChannelsAvailable: Set<ChampionChannelAlias>,
    val defaultSubscribedChannels: Set<ChampionChannelAlias>,
)

internal fun AndroidGattDiscoverySnapshot.toChampionDiscoverySnapshot(): ChampionDiscoverySnapshot = ChampionDiscoverySnapshot(
    services = services.associateBy(
        keySelector = AndroidGattServiceView::uuid,
        valueTransform = { service ->
            ChampionDiscoveredService(
                alias = ChampionUuidCatalog.services.values.firstOrNull { it.uuid == service.uuid }?.alias,
                uuid = service.uuid,
                characteristics = service.characteristics.associateBy(
                    keySelector = AndroidGattCharacteristicView::uuid,
                    valueTransform = { characteristic ->
                        ChampionDiscoveredCharacteristic(
                            uuid = characteristic.uuid,
                            properties = characteristic.properties,
                        )
                    },
                ),
            )
        },
    ),
)
