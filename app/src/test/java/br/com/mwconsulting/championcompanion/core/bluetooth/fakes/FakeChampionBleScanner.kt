package br.com.mwconsulting.championcompanion.core.bluetooth.fakes

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleScanner
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCancellationReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionScanRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeChampionBleScanner : ChampionBleScanner {
    private val mutableIsScanning = MutableStateFlow(false)
    private val mutableScanResults = MutableStateFlow<List<ChampionDiscoveredDevice>>(emptyList())
    private var nextScanResult: ChampionBleResult<List<ChampionDiscoveredDevice>> = ChampionBleResult.Success(emptyList())
    var lastRequest: ChampionScanRequest? = null
        private set
    var cancelReasons: List<ChampionCancellationReason> = emptyList()
        private set

    override val isScanning: StateFlow<Boolean> = mutableIsScanning.asStateFlow()
    override val scanResults: StateFlow<List<ChampionDiscoveredDevice>> = mutableScanResults.asStateFlow()

    fun enqueueResult(result: ChampionBleResult<List<ChampionDiscoveredDevice>>) {
        nextScanResult = result
    }

    override suspend fun scan(request: ChampionScanRequest): ChampionBleResult<List<ChampionDiscoveredDevice>> {
        lastRequest = request
        mutableIsScanning.value = true
        mutableScanResults.value = when (val result = nextScanResult) {
            is ChampionBleResult.Success -> result.value
            is ChampionBleResult.Failure -> emptyList()
        }
        return nextScanResult.also { mutableIsScanning.value = false }
    }

    override suspend fun cancelScan(reason: ChampionCancellationReason): ChampionBleResult<Unit> {
        cancelReasons = cancelReasons + reason
        mutableIsScanning.value = false
        return ChampionBleResult.Success(Unit)
    }
}
