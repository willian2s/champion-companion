package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCommand
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacket
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionResponseMatcher
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionRetryPolicy
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionWriteType
import java.time.ZonedDateTime
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal object DevicePocHandshakeA1 {
    const val COMMAND_CODE: Int = 0xA1
    private const val PAYLOAD_SIZE = 20
    private const val DEFAULT_PASSWORD = 0
    val defaultTimeout: Duration = 15.seconds

    fun buildPayload(
        dateTime: ZonedDateTime,
        password: Int = DEFAULT_PASSWORD,
    ): ByteArray {
        val payload = ByteArray(PAYLOAD_SIZE)
        val sanitizedPassword = password.coerceAtLeast(0).and(0xFFFF)
        val timezoneBlocks = (dateTime.offset.totalSeconds / 60.0 / 15.0).roundToInt()
        val year = dateTime.year

        payload[0] = COMMAND_CODE.toByte()
        payload[1] = (sanitizedPassword and 0xFF).toByte()
        payload[2] = ((sanitizedPassword shr 8) and 0xFF).toByte()
        payload[3] = 0x00
        payload[4] = ((year shr 8) and 0xFF).toByte()
        payload[5] = (year and 0xFF).toByte()
        payload[6] = dateTime.monthValue.toByte()
        payload[7] = dateTime.dayOfMonth.toByte()
        payload[8] = dateTime.hour.toByte()
        payload[9] = dateTime.minute.toByte()
        payload[10] = dateTime.second.toByte()
        payload[11] = 0x01
        payload[12] = 0x01
        payload[13] = timezoneBlocks.toByte()
        payload[14] = 0x00
        return payload
    }

    fun confirmationMatcher(): ChampionResponseMatcher = ChampionResponseMatcher { packet ->
        isConfirmationPacket(packet)
    }

    fun createCommand(
        dateTime: ZonedDateTime,
        timeout: Duration = defaultTimeout,
    ): ChampionCommand = ChampionCommand(
        commandCode = COMMAND_CODE,
        channel = ChampionChannelAlias.F008_WRITE,
        writeType = ChampionWriteType.WITH_RESPONSE,
        timeout = timeout,
        retryPolicy = ChampionRetryPolicy.None,
        responseMatcher = confirmationMatcher(),
        payload = buildPayload(dateTime),
    )

    fun isConfirmationPacket(packet: ChampionPacket): Boolean {
        if (packet.channel != ChampionChannelAlias.F008_NOTIFY) return false
        if (packet.size < PAYLOAD_SIZE) return false
        val bytes = packet.bytesCopy()
        return bytes[0].toInt().and(0xFF) == COMMAND_CODE && bytes[19] == 0x01.toByte()
    }
}
