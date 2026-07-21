package br.com.mwconsulting.championcompanion.core.bluetooth.android

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBlePrerequisites
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleScanner
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCancellationReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDeviceFilter
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionMissingPrerequisite
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionOperationType
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dagger.hilt.android.qualifiers.ApplicationContext

class AndroidChampionBleScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val environment: AndroidBleEnvironment,
) : ChampionBleScanner {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val mutableIsScanning = MutableStateFlow(false)
    private var activeScan: ActiveScan? = null

    override val isScanning: StateFlow<Boolean> = mutableIsScanning.asStateFlow()

    override suspend fun scan(request: br.com.mwconsulting.championcompanion.core.bluetooth.ChampionScanRequest): ChampionBleResult<List<ChampionDiscoveredDevice>> {
        val readiness = ChampionBlePrerequisites.forScan(environment.snapshot())
        if (!readiness.isReady) {
            return ChampionBleResult.Failure(readiness.missing.firstErrorOr(ChampionBleError.OperationUnavailable))
        }

        val scanSetup = mutex.withLock {
            if (activeScan != null) {
                return@withLock ChampionBleResult.Failure(ChampionBleError.ScanAlreadyInProgress)
            }

            val platformScanner = createPlatformScanner() ?: return@withLock ChampionBleResult.Failure(ChampionBleError.ScanFailed())
            val completion = CompletableDeferred<ChampionBleResult<List<ChampionDiscoveredDevice>>>()
            val state = ActiveScan(
                scanner = platformScanner.scanner,
                callback = platformScanner.callback,
                completion = completion,
                timeoutJob = scope.launch {
                    kotlinx.coroutines.delay(request.timeout)
                    finishActiveScanSafely(
                        result = ChampionBleResult.Failure(
                            ChampionBleError.Timeout(
                                operation = ChampionOperationType.SCAN,
                                after = request.timeout,
                            ),
                        ),
                    )
                },
            )
            activeScan = state
            mutableIsScanning.value = true
            platformScanner.start()
            state
        }

        if (scanSetup is ChampionBleResult.Failure) return scanSetup

        val active = scanSetup as ActiveScan
        return try {
            active.completion.await()
        } catch (_: CancellationException) {
            finishActiveScanSafely(
                result = ChampionBleResult.Failure(
                    ChampionBleError.OperationCancelled(ChampionCancellationReason.USER_REQUESTED),
                ),
            )
            throw CancellationException()
        }
    }

    override suspend fun cancelScan(
        reason: ChampionCancellationReason,
    ): ChampionBleResult<Unit> {
        val result = mutex.withLock {
            if (activeScan == null) {
                return@withLock ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
            }
            ChampionBleResult.Success(Unit)
        }

        if (result is ChampionBleResult.Failure) return result
        return finishActiveScanSafely(
            result = ChampionBleResult.Failure(ChampionBleError.OperationCancelled(reason)),
        )
    }

    @SuppressLint("MissingPermission")
    private fun createPlatformScanner(): PlatformScanner? {
        val snapshot = environment.snapshot()
        if (!snapshot.isBleSupported || !snapshot.hasBluetoothAdapter || !snapshot.isBluetoothEnabled) {
            return null
        }
        val scanner = context.getSystemService(BluetoothManager::class.java)
            ?.adapter
            ?.bluetoothLeScanner
            ?: return null

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                handleScanResult(result.toView())
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { handleScanResult(it.toView()) }
            }

            override fun onScanFailed(errorCode: Int) {
                scope.launch {
                    finishActiveScanSafely(
                        result = ChampionBleResult.Failure(
                            ChampionBleError.ScanFailed(
                                detail = br.com.mwconsulting.championcompanion.core.bluetooth.SafeGattDetail(
                                    statusCode = errorCode,
                                    statusLabel = "scan_error_$errorCode",
                                ),
                            ),
                        ),
                    )
                }
            }
        }

        return PlatformScanner(
            scanner = scanner,
            callback = callback,
            start = {
                scanner.startScan(
                    null,
                    ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                    callback,
                )
            },
        )
    }

    private fun handleScanResult(view: AndroidScanResultView) {
        val device = ChampionDiscoveredDevice(
            transportKey = view.transportKey,
            name = view.name,
            localName = view.localName,
            observedAt = view.observedAt,
        )
        if (!ChampionDeviceFilter.matches(device)) return

        scope.launch {
            mutex.withLock {
                val current = activeScan ?: return@withLock
                current.discovered[ChampionDeviceFilter.deduplicationKey(device)] = device
                finishActiveScanUnlocked(
                    result = ChampionBleResult.Success(current.discovered.values.toList()),
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun finishActiveScanSafely(result: ChampionBleResult<List<ChampionDiscoveredDevice>>): ChampionBleResult<Unit> =
        mutex.withLock {
            if (activeScan == null) return@withLock ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
            finishActiveScanUnlocked(result)
        }

    @SuppressLint("MissingPermission")
    private fun finishActiveScanUnlocked(result: ChampionBleResult<List<ChampionDiscoveredDevice>>): ChampionBleResult<Unit> {
        val current = activeScan ?: return ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        activeScan = null
        mutableIsScanning.value = false
        current.timeoutJob.cancel()
        runCatching { current.scanner.stopScan(current.callback) }
        current.completion.complete(result)
        return ChampionBleResult.Success(Unit)
    }

    private fun List<ChampionMissingPrerequisite>.firstErrorOr(fallback: ChampionBleError): ChampionBleError =
        firstOrNull()?.error ?: fallback

    private fun ScanResult.toView(): AndroidScanResultView = AndroidScanResultView(
        transportKey = device.address,
        name = safeDeviceName(),
        localName = scanRecord?.deviceName,
        observedAt = Instant.now(),
    )

    @SuppressLint("MissingPermission")
    private fun ScanResult.safeDeviceName(): String? =
        if (canReadDeviceName()) device.name else null

    private fun canReadDeviceName(): Boolean {
        val permissions = environment.snapshot().permissions
        return permissions.sdkInt < Build.VERSION_CODES.S || permissions.bluetoothConnectGranted
    }

    private data class ActiveScan(
        val scanner: BluetoothLeScanner,
        val callback: ScanCallback,
        val completion: CompletableDeferred<ChampionBleResult<List<ChampionDiscoveredDevice>>>,
        val timeoutJob: Job,
        val discovered: LinkedHashMap<String, ChampionDiscoveredDevice> = linkedMapOf(),
    )

    private data class PlatformScanner(
        val scanner: BluetoothLeScanner,
        val callback: ScanCallback,
        val start: () -> Unit,
    )
}
