package br.com.mwconsulting.championcompanion.core.bluetooth

import java.util.UUID

data class ChampionServiceDefinition(
    val alias: ChampionServiceAlias,
    val uuid: UUID,
)

data class ChampionChannelDefinition(
    val alias: ChampionChannelAlias,
    val uuid: UUID,
    val properties: Set<ChampionChannelProperty>,
    val isSubscribedByDefault: Boolean,
)

object ChampionUuidCatalog {
    val cccd: UUID = uuid("00002902-0000-1000-8000-00805f9b34fb")

    val services: Map<ChampionServiceAlias, ChampionServiceDefinition> = mapOf(
        ChampionServiceAlias.F008 to ChampionServiceDefinition(
            alias = ChampionServiceAlias.F008,
            uuid = uuid("f0080001-0451-4000-b000-000000000000"),
        ),
        ChampionServiceAlias.F002 to ChampionServiceDefinition(
            alias = ChampionServiceAlias.F002,
            uuid = uuid("f0020001-0451-4000-b000-000000000000"),
        ),
        ChampionServiceAlias.FEE7 to ChampionServiceDefinition(
            alias = ChampionServiceAlias.FEE7,
            uuid = uuid("0000fee7-0000-1000-8000-00805f9b34fb"),
        ),
    )

    val channels: Map<ChampionChannelAlias, ChampionChannelDefinition> = mapOf(
        ChampionChannelAlias.F008_NOTIFY to ChampionChannelDefinition(
            alias = ChampionChannelAlias.F008_NOTIFY,
            uuid = uuid("f0080002-0451-4000-b000-000000000000"),
            properties = setOf(ChampionChannelProperty.NOTIFY),
            isSubscribedByDefault = true,
        ),
        ChampionChannelAlias.F008_WRITE to ChampionChannelDefinition(
            alias = ChampionChannelAlias.F008_WRITE,
            uuid = uuid("f0080003-0451-4000-b000-000000000000"),
            properties = setOf(ChampionChannelProperty.WRITE),
            isSubscribedByDefault = false,
        ),
        ChampionChannelAlias.F002_NOTIFY to ChampionChannelDefinition(
            alias = ChampionChannelAlias.F002_NOTIFY,
            uuid = uuid("f0020002-0451-4000-b000-000000000000"),
            properties = setOf(ChampionChannelProperty.NOTIFY),
            isSubscribedByDefault = true,
        ),
        ChampionChannelAlias.F002_WRITE to ChampionChannelDefinition(
            alias = ChampionChannelAlias.F002_WRITE,
            uuid = uuid("f0020003-0451-4000-b000-000000000000"),
            properties = setOf(ChampionChannelProperty.WRITE),
            isSubscribedByDefault = false,
        ),
        ChampionChannelAlias.FEA1_NOTIFY to ChampionChannelDefinition(
            alias = ChampionChannelAlias.FEA1_NOTIFY,
            uuid = uuid("0000fea1-0000-1000-8000-00805f9b34fb"),
            properties = setOf(ChampionChannelProperty.NOTIFY),
            isSubscribedByDefault = true,
        ),
        ChampionChannelAlias.FEA2_INDICATE to ChampionChannelDefinition(
            alias = ChampionChannelAlias.FEA2_INDICATE,
            uuid = uuid("0000fea2-0000-1000-8000-00805f9b34fb"),
            properties = setOf(ChampionChannelProperty.INDICATE),
            isSubscribedByDefault = false,
        ),
        ChampionChannelAlias.FEC8_INDICATE to ChampionChannelDefinition(
            alias = ChampionChannelAlias.FEC8_INDICATE,
            uuid = uuid("0000fec8-0000-1000-8000-00805f9b34fb"),
            properties = setOf(ChampionChannelProperty.INDICATE),
            isSubscribedByDefault = false,
        ),
    )

    val requiredTransportChannels: Set<ChampionChannelAlias> = setOf(
        ChampionChannelAlias.F008_NOTIFY,
        ChampionChannelAlias.F008_WRITE,
    )

    val optionalChannels: Set<ChampionChannelAlias> = setOf(
        ChampionChannelAlias.F002_NOTIFY,
        ChampionChannelAlias.F002_WRITE,
        ChampionChannelAlias.FEA1_NOTIFY,
        ChampionChannelAlias.FEA2_INDICATE,
        ChampionChannelAlias.FEC8_INDICATE,
    )

    val defaultSubscribedChannels: Set<ChampionChannelAlias> =
        channels.values.filter { it.isSubscribedByDefault }.mapTo(linkedSetOf()) { it.alias }

    fun serviceUuid(alias: ChampionServiceAlias): UUID = services.getValue(alias).uuid

    fun channelUuid(alias: ChampionChannelAlias): UUID = channels.getValue(alias).uuid

    fun definitionOf(alias: ChampionChannelAlias): ChampionChannelDefinition = channels.getValue(alias)

    fun findChannel(uuid: UUID): ChampionChannelDefinition? = channels.values.firstOrNull { it.uuid == uuid }

    fun requiredCapabilities(): Set<ChampionChannelAlias> = requiredTransportChannels

    private fun uuid(value: String): UUID = UUID.fromString(value)
}
