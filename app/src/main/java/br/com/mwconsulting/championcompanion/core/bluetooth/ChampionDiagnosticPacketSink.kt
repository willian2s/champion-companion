package br.com.mwconsulting.championcompanion.core.bluetooth

import java.time.Instant

class ChampionDiagnosticPacketCapture(
    val sessionId: String,
    val channel: ChampionChannelAlias,
    val direction: ChampionPacketDirection,
    val observedAt: Instant,
    val commandCode: Int?,
    bytes: ByteArray,
) {
    private val copiedBytes = bytes.copyOf()

    val size: Int = copiedBytes.size

    fun bytesCopy(): ByteArray = copiedBytes.copyOf()
}

fun interface ChampionDiagnosticPacketSink {
    fun capture(packet: ChampionDiagnosticPacketCapture)
}

object NoOpChampionDiagnosticPacketSink : ChampionDiagnosticPacketSink {
    override fun capture(packet: ChampionDiagnosticPacketCapture) = Unit
}
