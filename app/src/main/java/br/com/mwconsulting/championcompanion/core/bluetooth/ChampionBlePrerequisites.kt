package br.com.mwconsulting.championcompanion.core.bluetooth

import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidBleEnvironmentSnapshot
import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidPermissionSnapshot

enum class ChampionBleOperation {
    SCAN,
    CONNECT,
}

enum class ChampionRuntimePermission {
    BLUETOOTH,
    BLUETOOTH_ADMIN,
    BLUETOOTH_SCAN,
    BLUETOOTH_CONNECT,
    ACCESS_FINE_LOCATION,
    ACCESS_COARSE_LOCATION,
}

data class ChampionMissingPrerequisite(
    val error: ChampionBleError,
    val permissions: Set<ChampionRuntimePermission> = emptySet(),
)

data class ChampionBleReadiness(
    val operation: ChampionBleOperation,
    val prerequisiteSnapshot: ChampionPrerequisiteSnapshot,
    val missing: List<ChampionMissingPrerequisite>,
) {
    val isReady: Boolean = missing.isEmpty()
}

object ChampionBlePrerequisites {
    fun forScan(environment: AndroidBleEnvironmentSnapshot): ChampionBleReadiness =
        evaluate(ChampionBleOperation.SCAN, environment)

    fun forConnect(environment: AndroidBleEnvironmentSnapshot): ChampionBleReadiness =
        evaluate(ChampionBleOperation.CONNECT, environment)

    fun evaluate(
        operation: ChampionBleOperation,
        environment: AndroidBleEnvironmentSnapshot,
    ): ChampionBleReadiness {
        val permissionSnapshot = environment.permissions
        val prerequisiteSnapshot = permissionSnapshot.toContractSnapshot(
            isBleSupported = environment.isBleSupported,
            isBluetoothEnabled = environment.isBluetoothEnabled,
        )

        val missing = buildList {
            if (!environment.isBleSupported || !environment.hasBluetoothAdapter) {
                add(ChampionMissingPrerequisite(ChampionBleError.BleNotSupported))
            }
            if (!environment.isBluetoothEnabled) {
                add(ChampionMissingPrerequisite(ChampionBleError.BluetoothDisabled))
            }

            when (operation) {
                ChampionBleOperation.SCAN -> addScanPermissionBlockers(permissionSnapshot)
                ChampionBleOperation.CONNECT -> addConnectPermissionBlockers(permissionSnapshot)
            }
        }

        return ChampionBleReadiness(
            operation = operation,
            prerequisiteSnapshot = prerequisiteSnapshot,
            missing = missing,
        )
    }

    private fun MutableList<ChampionMissingPrerequisite>.addScanPermissionBlockers(
        permissions: AndroidPermissionSnapshot,
    ) {
        if (permissions.sdkInt >= ANDROID_12) {
            if (!permissions.bluetoothScanGranted) {
                add(
                    ChampionMissingPrerequisite(
                        error = ChampionBleError.ScanPermissionMissing,
                        permissions = setOf(ChampionRuntimePermission.BLUETOOTH_SCAN),
                    ),
                )
            }
            if (!permissions.bluetoothConnectGranted) {
                add(
                    ChampionMissingPrerequisite(
                        error = ChampionBleError.ConnectPermissionMissing,
                        permissions = setOf(ChampionRuntimePermission.BLUETOOTH_CONNECT),
                    ),
                )
            }
            if (permissions.isLocationPermissionRequired && !permissions.fineLocationGranted) {
                add(
                    ChampionMissingPrerequisite(
                        error = ChampionBleError.LocationPermissionRequired,
                        permissions = setOf(ChampionRuntimePermission.ACCESS_FINE_LOCATION),
                    ),
                )
            }
            return
        }

        val missingLegacyPermissions = buildSet {
            if (!permissions.bluetoothGranted) add(ChampionRuntimePermission.BLUETOOTH)
            if (!permissions.bluetoothAdminGranted) add(ChampionRuntimePermission.BLUETOOTH_ADMIN)
        }
        if (missingLegacyPermissions.isNotEmpty()) {
            add(
                ChampionMissingPrerequisite(
                    error = ChampionBleError.LegacyPermissionMissing,
                    permissions = missingLegacyPermissions,
                ),
            )
        }
        if (!permissions.fineLocationGranted) {
            add(
                ChampionMissingPrerequisite(
                    error = ChampionBleError.LocationPermissionRequired,
                    permissions = setOf(ChampionRuntimePermission.ACCESS_FINE_LOCATION),
                ),
            )
        }
    }

    private fun MutableList<ChampionMissingPrerequisite>.addConnectPermissionBlockers(
        permissions: AndroidPermissionSnapshot,
    ) {
        if (permissions.sdkInt >= ANDROID_12) {
            if (!permissions.bluetoothConnectGranted) {
                add(
                    ChampionMissingPrerequisite(
                        error = ChampionBleError.ConnectPermissionMissing,
                        permissions = setOf(ChampionRuntimePermission.BLUETOOTH_CONNECT),
                    ),
                )
            }
            return
        }

        val missingLegacyPermissions = buildSet {
            if (!permissions.bluetoothGranted) add(ChampionRuntimePermission.BLUETOOTH)
            if (!permissions.bluetoothAdminGranted) add(ChampionRuntimePermission.BLUETOOTH_ADMIN)
        }
        if (missingLegacyPermissions.isNotEmpty()) {
            add(
                ChampionMissingPrerequisite(
                    error = ChampionBleError.LegacyPermissionMissing,
                    permissions = missingLegacyPermissions,
                ),
            )
        }
    }

    private fun AndroidPermissionSnapshot.toContractSnapshot(
        isBleSupported: Boolean,
        isBluetoothEnabled: Boolean,
    ) = ChampionPrerequisiteSnapshot(
        isBleSupported = isBleSupported,
        isBluetoothEnabled = isBluetoothEnabled,
        isScanPermissionGranted = isScanGranted(),
        isConnectPermissionGranted = isConnectGranted(),
        isLegacyPermissionGranted = isLegacyGranted(),
        isLocationPermissionGranted = isLocationGranted(),
        isLocationPermissionRequired = isLocationPermissionRequired,
    )

    private fun AndroidPermissionSnapshot.isScanGranted(): Boolean =
        if (sdkInt >= ANDROID_12) {
            bluetoothScanGranted && bluetoothConnectGranted &&
                (!isLocationPermissionRequired || fineLocationGranted)
        } else {
            bluetoothGranted && bluetoothAdminGranted && fineLocationGranted
        }

    private fun AndroidPermissionSnapshot.isConnectGranted(): Boolean =
        if (sdkInt >= ANDROID_12) bluetoothConnectGranted else bluetoothGranted && bluetoothAdminGranted

    private fun AndroidPermissionSnapshot.isLegacyGranted(): Boolean =
        if (sdkInt >= ANDROID_12) true else bluetoothGranted && bluetoothAdminGranted

    private fun AndroidPermissionSnapshot.isLocationGranted(): Boolean =
        !isLocationPermissionRequired || fineLocationGranted

    private const val ANDROID_12 = 31
}
