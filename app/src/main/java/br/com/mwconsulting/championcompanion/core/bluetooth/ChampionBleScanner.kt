package br.com.mwconsulting.championcompanion.core.bluetooth

import kotlinx.coroutines.flow.StateFlow

interface ChampionBleScanner {
    val isScanning: StateFlow<Boolean>
    val scanResults: StateFlow<List<ChampionDiscoveredDevice>>

    suspend fun scan(request: ChampionScanRequest): ChampionBleResult<List<ChampionDiscoveredDevice>>

    suspend fun cancelScan(
        reason: ChampionCancellationReason = ChampionCancellationReason.USER_REQUESTED,
    ): ChampionBleResult<Unit>
}
