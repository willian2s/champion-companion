package br.com.mwconsulting.championcompanion.core.bluetooth.android

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import br.com.mwconsulting.championcompanion.core.bluetooth.SafeGattDetail
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionWriteType

interface AndroidGattConnection {
    val transportKey: String

    fun discoverServices(): Boolean

    fun readCharacteristic(characteristicUuid: UUID): Boolean

    fun writeCharacteristic(characteristicUuid: UUID, value: ByteArray, writeType: ChampionWriteType? = null): Boolean

    fun writeDescriptor(characteristicUuid: UUID, descriptorUuid: UUID, value: ByteArray): Boolean

    fun setCharacteristicNotification(characteristicUuid: UUID, enabled: Boolean): Boolean

    fun requestMtu(mtu: Int): Boolean

    fun disconnect()

    fun close()
}

interface AndroidGattCallback {
    fun onConnectionStateChanged(isConnected: Boolean, detail: SafeGattDetail?)

    fun onServicesDiscovered(
        serviceUuids: Set<UUID>,
        snapshot: AndroidGattDiscoverySnapshot,
        detail: SafeGattDetail?,
    )

    fun onCharacteristicRead(characteristicUuid: UUID, value: ByteArray, detail: SafeGattDetail?)

    fun onCharacteristicWrite(characteristicUuid: UUID, detail: SafeGattDetail?)

    fun onCharacteristicChanged(characteristicUuid: UUID, value: ByteArray)

    fun onDescriptorWrite(characteristicUuid: UUID?, descriptorUuid: UUID, detail: SafeGattDetail?)

    fun onMtuChanged(mtu: Int, detail: SafeGattDetail?)
}

fun interface AndroidGattFacade {
    fun connect(transportKey: String, callback: AndroidGattCallback): AndroidGattConnection?
}

class DefaultAndroidGattFacade @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val discoveryMapper: AndroidGattDiscoveryMapper,
) : AndroidGattFacade {
    @SuppressLint("MissingPermission")
    override fun connect(transportKey: String, callback: AndroidGattCallback): AndroidGattConnection? {
        if (!hasBluetoothConnectPermission(context)) return null
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java) ?: return null
        val bluetoothAdapter = bluetoothManager.adapter ?: return null
        val device = runCatching { bluetoothAdapter.getRemoteDevice(transportKey) }.getOrNull() ?: return null

        lateinit var connection: RealAndroidGattConnection
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                callback.onConnectionStateChanged(
                    isConnected = status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED,
                    detail = status.toSafeDetail(),
                )
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                callback.onServicesDiscovered(
                    serviceUuids = gatt.services.mapTo(linkedSetOf()) { it.uuid },
                    snapshot = discoveryMapper.map(gatt.services),
                    detail = status.toSafeDetail(),
                )
            }

            @Deprecated("Deprecated in API 33")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                callback.onCharacteristicRead(
                    characteristicUuid = characteristic.uuid,
                    value = characteristic.value?.copyOf() ?: byteArrayOf(),
                    detail = status.toSafeDetail(),
                )
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int,
            ) {
                callback.onCharacteristicRead(
                    characteristicUuid = characteristic.uuid,
                    value = value.copyOf(),
                    detail = status.toSafeDetail(),
                )
            }

            @Deprecated("Deprecated in API 33")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
            ) {
                callback.onCharacteristicChanged(
                    characteristicUuid = characteristic.uuid,
                    value = characteristic.value?.copyOf() ?: byteArrayOf(),
                )
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
            ) {
                callback.onCharacteristicChanged(characteristic.uuid, value.copyOf())
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                callback.onCharacteristicWrite(characteristic.uuid, status.toSafeDetail())
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int,
            ) {
                callback.onDescriptorWrite(
                    characteristicUuid = descriptor.characteristic?.uuid,
                    descriptorUuid = descriptor.uuid,
                    detail = status.toSafeDetail(),
                )
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                callback.onMtuChanged(mtu, status.toSafeDetail())
            }
        }

        val gatt = device.connectGatt(context, false, gattCallback) ?: return null
        connection = RealAndroidGattConnection(transportKey, gatt, context)
        return connection
    }

    private class RealAndroidGattConnection(
        override val transportKey: String,
        private val gatt: BluetoothGatt,
        private val context: Context,
    ) : AndroidGattConnection {
        @SuppressLint("MissingPermission")
        override fun discoverServices(): Boolean {
            if (!hasBluetoothConnectPermission(context)) return false
            return gatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun readCharacteristic(characteristicUuid: UUID): Boolean {
            val characteristic = findCharacteristic(characteristicUuid) ?: return false
            if (!hasBluetoothConnectPermission(context)) return false
            return gatt.readCharacteristic(characteristic)
        }

        @SuppressLint("MissingPermission")
        override fun writeCharacteristic(
            characteristicUuid: UUID,
            value: ByteArray,
            writeType: ChampionWriteType?,
        ): Boolean {
            val characteristic = findCharacteristic(characteristicUuid) ?: return false
            if (!hasBluetoothConnectPermission(context)) return false
            val resolvedWriteType = characteristic.resolveWriteType(writeType) ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    characteristic,
                    value.copyOf(),
                    resolvedWriteType,
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                writeCharacteristicLegacy(characteristic, value, resolvedWriteType)
            }
        }

        @SuppressLint("MissingPermission")
        override fun writeDescriptor(characteristicUuid: UUID, descriptorUuid: UUID, value: ByteArray): Boolean {
            val characteristic = findCharacteristic(characteristicUuid) ?: return false
            val descriptor = characteristic.getDescriptor(descriptorUuid) ?: return false
            if (!hasBluetoothConnectPermission(context)) return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, value.copyOf()) == BluetoothStatusCodes.SUCCESS
            } else {
                writeDescriptorLegacy(descriptor, value)
            }
        }

        @SuppressLint("MissingPermission")
        override fun setCharacteristicNotification(characteristicUuid: UUID, enabled: Boolean): Boolean {
            val characteristic = findCharacteristic(characteristicUuid) ?: return false
            if (!hasBluetoothConnectPermission(context)) return false
            return gatt.setCharacteristicNotification(characteristic, enabled)
        }

        @SuppressLint("MissingPermission")
        override fun requestMtu(mtu: Int): Boolean {
            if (!hasBluetoothConnectPermission(context)) return false
            return gatt.requestMtu(mtu)
        }

        @SuppressLint("MissingPermission")
        override fun disconnect() {
            if (!hasBluetoothConnectPermission(context)) return
            gatt.disconnect()
        }

        @SuppressLint("MissingPermission")
        override fun close() {
            if (!hasBluetoothConnectPermission(context)) return
            gatt.close()
        }

        private fun findCharacteristic(characteristicUuid: UUID): BluetoothGattCharacteristic? =
            gatt.services.firstNotNullOfOrNull { service -> service.getCharacteristic(characteristicUuid) }

        @SuppressLint("MissingPermission")
        @Suppress("DEPRECATION")
        private fun writeCharacteristicLegacy(
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            writeType: Int,
        ): Boolean {
            characteristic.value = value.copyOf()
            characteristic.writeType = writeType
            return gatt.writeCharacteristic(characteristic)
        }

        private fun BluetoothGattCharacteristic.resolveWriteType(requestedWriteType: ChampionWriteType?): Int? {
            val hasWrite = properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
            val hasWriteNoResponse = properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0

            return when (requestedWriteType) {
                ChampionWriteType.WITH_RESPONSE -> when {
                    hasWrite -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    hasWriteNoResponse -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    else -> null
                }

                ChampionWriteType.WITHOUT_RESPONSE -> when {
                    hasWriteNoResponse -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    hasWrite -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    else -> null
                }

                null -> when {
                    hasWrite -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    hasWriteNoResponse -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    else -> null
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Suppress("DEPRECATION")
        private fun writeDescriptorLegacy(
            descriptor: BluetoothGattDescriptor,
            value: ByteArray,
        ): Boolean {
            descriptor.value = value.copyOf()
            return gatt.writeDescriptor(descriptor)
        }
    }

    private fun Int.toSafeDetail(): SafeGattDetail? = SafeGattDetail(
        statusCode = this,
        statusLabel = if (this == BluetoothGatt.GATT_SUCCESS) "gatt_success" else "gatt_status_$this",
    )

}

private fun hasBluetoothConnectPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
