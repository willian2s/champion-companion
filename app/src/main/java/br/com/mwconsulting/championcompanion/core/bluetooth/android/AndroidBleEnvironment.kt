package br.com.mwconsulting.championcompanion.core.bluetooth.android

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class AndroidBleEnvironmentSnapshot(
    val sdkInt: Int,
    val isBleSupported: Boolean,
    val hasBluetoothAdapter: Boolean,
    val isBluetoothEnabled: Boolean,
    val permissions: AndroidPermissionSnapshot,
)

fun interface AndroidBleEnvironment {
    fun snapshot(): AndroidBleEnvironmentSnapshot
}

class DefaultAndroidBleEnvironment @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AndroidBleEnvironment {
    override fun snapshot(): AndroidBleEnvironmentSnapshot {
        val packageManager = context.packageManager
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager?.adapter
        val sdkInt = Build.VERSION.SDK_INT
        val locationRequired = sdkInt < Build.VERSION_CODES.S

        return AndroidBleEnvironmentSnapshot(
            sdkInt = sdkInt,
            isBleSupported = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE),
            hasBluetoothAdapter = bluetoothAdapter != null,
            isBluetoothEnabled = bluetoothAdapter?.isEnabled == true,
            permissions = AndroidPermissionSnapshot(
                sdkInt = sdkInt,
                bluetoothGranted = context.isGranted(Manifest.permission.BLUETOOTH),
                bluetoothAdminGranted = context.isGranted(Manifest.permission.BLUETOOTH_ADMIN),
                bluetoothScanGranted = context.isGranted(Manifest.permission.BLUETOOTH_SCAN),
                bluetoothConnectGranted = context.isGranted(Manifest.permission.BLUETOOTH_CONNECT),
                fineLocationGranted = context.isGranted(Manifest.permission.ACCESS_FINE_LOCATION),
                coarseLocationGranted = context.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION),
                isLocationPermissionRequired = locationRequired,
            ),
        )
    }

    private fun Context.isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
