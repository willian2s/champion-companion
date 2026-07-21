package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.testPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePocFea1StepsTest {
    @Test
    fun `parse decodes 24 bit little endian steps`() {
        val result = DevicePocFea1Steps.parse(
            testPacket(
                channel = ChampionChannelAlias.FEA1_NOTIFY,
                commandCode = 0x01,
                bytes = byteArrayOf(0x01, 0x78, 0x56, 0x34),
            ),
        )

        assertEquals(DevicePocFea1Steps.Result.Valid(0x345678), result)
    }

    @Test
    fun `parse rejects invalid packet size`() {
        val result = DevicePocFea1Steps.parse(
            testPacket(
                channel = ChampionChannelAlias.FEA1_NOTIFY,
                commandCode = 0x01,
                bytes = byteArrayOf(0x01, 0x02, 0x03),
            ),
        )

        assertEquals(
            DevicePocFea1Steps.Result.Invalid(
                ChampionBleError.InvalidPayload(
                    channel = ChampionChannelAlias.FEA1_NOTIFY,
                    size = 3,
                ),
            ),
            result,
        )
    }

    @Test
    fun `parse rejects invalid header even with valid size`() {
        val result = DevicePocFea1Steps.parse(
            testPacket(
                channel = ChampionChannelAlias.FEA1_NOTIFY,
                commandCode = 0x02,
                bytes = byteArrayOf(0x02, 0x00, 0x00, 0x00),
            ),
        )

        assertTrue(result is DevicePocFea1Steps.Result.Invalid)
    }
}
