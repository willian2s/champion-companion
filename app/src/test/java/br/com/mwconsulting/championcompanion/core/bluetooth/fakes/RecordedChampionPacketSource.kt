package br.com.mwconsulting.championcompanion.core.bluetooth.fakes

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class RecordedChampionPacketSource {
    private val mutablePackets = MutableSharedFlow<ChampionPacket>(extraBufferCapacity = 32)
    private val recordedPackets = mutableListOf<ChampionPacket>()

    val packets: Flow<ChampionPacket> = mutablePackets.asSharedFlow()

    suspend fun emit(packet: ChampionPacket) {
        recordedPackets += packet
        mutablePackets.emit(packet)
    }

    fun recorded(): List<ChampionPacket> = recordedPackets.toList()
}
