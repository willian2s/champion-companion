package br.com.mwconsulting.championcompanion.core.bluetooth.android

data class AndroidPermissionSnapshot(
    val sdkInt: Int,
    val bluetoothGranted: Boolean,
    val bluetoothAdminGranted: Boolean,
    val bluetoothScanGranted: Boolean,
    val bluetoothConnectGranted: Boolean,
    val fineLocationGranted: Boolean,
    val coarseLocationGranted: Boolean,
    val isLocationPermissionRequired: Boolean,
)
