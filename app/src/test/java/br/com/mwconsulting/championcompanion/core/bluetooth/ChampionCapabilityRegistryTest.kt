package br.com.mwconsulting.championcompanion.core.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionBleCapabilityRegistryTest {
    @Test
    fun `resolve accepts required and optional channels`() {
        val result = ChampionCapabilityRegistry.resolve(
            sessionId = testSessionId(),
            discovery = discoverySnapshot(
                ChampionChannelAlias.F008_NOTIFY to setOf(ChampionChannelProperty.NOTIFY),
                ChampionChannelAlias.F008_WRITE to setOf(ChampionChannelProperty.WRITE),
                ChampionChannelAlias.FEA1_NOTIFY to setOf(ChampionChannelProperty.NOTIFY),
            ),
        )

        val capabilities = (result as ChampionBleResult.Success).value
        assertTrue(capabilities.snapshot.isTransportReady())
        assertEquals(setOf(ChampionChannelAlias.FEA1_NOTIFY), capabilities.optionalChannelsAvailable)
        assertEquals(setOf(ChampionChannelAlias.F008_NOTIFY, ChampionChannelAlias.FEA1_NOTIFY), capabilities.defaultSubscribedChannels)
    }

    @Test
    fun `resolve fails when required service or properties are incompatible`() {
        val missingRequired = ChampionCapabilityRegistry.resolve(
            sessionId = testSessionId(),
            discovery = discoverySnapshot(
                ChampionChannelAlias.F008_NOTIFY to setOf(ChampionChannelProperty.NOTIFY),
            ),
        )
        val incompatible = ChampionCapabilityRegistry.resolve(
            sessionId = testSessionId(),
            discovery = discoverySnapshot(
                ChampionChannelAlias.F008_NOTIFY to setOf(ChampionChannelProperty.NOTIFY),
                ChampionChannelAlias.F008_WRITE to setOf(ChampionChannelProperty.READ),
            ),
        )

        assertEquals(
            ChampionBleError.MissingRequiredChannel(ChampionChannelAlias.F008_WRITE),
            (missingRequired as ChampionBleResult.Failure).error,
        )
        assertEquals(
            ChampionBleError.IncompatibleChannelProperties(ChampionChannelAlias.F008_WRITE),
            (incompatible as ChampionBleResult.Failure).error,
        )
    }
}
