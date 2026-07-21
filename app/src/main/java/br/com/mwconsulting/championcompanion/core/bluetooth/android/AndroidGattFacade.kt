package br.com.mwconsulting.championcompanion.core.bluetooth.android

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import br.com.mwconsulting.championcompanion.core.bluetooth.SafeGattDetail

interface AndroidGattConnection {
    val transportKey: String

    fun discoverServices(): Boolean

    fun readCharacteristic(characteristicUuid: UUID): Boolean

    fun writeCharacteristic(characteristicUuid: UUID, value: ByteArray): Boolean

    fun writeDescriptor(characteristicUuid: UUID, descriptorUuid: UUID, value: ByteArray): Boolean

    fun setCharacteristicNotification(characteristicUuid: UUID, enabled: Boolean): Boolean

    fun requestMtu(mtu: Int): Boolean

    fun disconnect()

    fun close()
}

interface AndroidGattCallback {
    fun onConnectionStateChanged(isConnected: Boolean, detail: SafeGattDetail?)

    fun onServicesDiscovered(serviceUuids: Set<UUID>, detail: SafeGattDetail?)

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
) : AndroidGattFacade {
    @SuppressLint("MissingPermission")
    override fun connect(transportKey: String, callback: AndroidGattCallback): AndroidGattConnection? {
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
        connection = RealAndroidGattConnection(transportKey, gatt)
        return connection
    }

    private class RealAndroidGattConnection(
        override val transportKey: String,
        private val gatt: BluetoothGatt,
    ) : AndroidGattConnection {
        override fun discoverServices(): Boolean = gatt.discoverServices()

        @SuppressLint("MissingPermission")
        override fun readCharacteristic(characteristicUuid: UUID): Boolean {
            val characteristic = findCharacteristic(characteristicUuid) ?: return false
            return gatt.readCharacteristic(characteristic)
        }

        @SuppressLint("MissingPermission")
        override fun writeCharacteristic(characteristicUuid: UUID, value: ByteArray): Boolean {
            val characteristic = findCharacteristic(characteristicUuid) ?: return false
            return gatt.writeCharacteristic(characteristic, value.copyOf(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothGatt.GATT_SUCCESS
        }

        @SuppressLint("MissingPermission")
        override fun writeDescriptor(characteristicUuid: UUID, descriptorUuid: UUID, value: ByteArray): Boolean {
            val characteristic = findCharacteristic(characteristicUuid) ?: return false
            val descriptor = characteristic.getDescriptor(descriptorUuid) ?: return false
            return gatt.writeDescriptor(descriptor, value.copyOf()) == BluetoothGatt.GATT_SUCCESS
        }

        @SuppressLint("MissingPermission")
        override fun setCharacteristicNotification(characteristicUuid: UUID, enabled: Boolean): Boolean {
            val characteristic = findCharacteristic(characteristicUuid) ?: return false
            return gatt.setCharacteristicNotification(characteristic, enabled)
        }

        @SuppressLint("MissingPermission")
        override fun requestMtu(mtu: Int): Boolean = gatt.requestMtu(mtu)

        @SuppressLint("MissingPermission")
        override fun disconnect() {
            gatt.disconnect()
        }

        override fun close() {
            gatt.close()
        }

        private fun findCharacteristic(characteristicUuid: UUID): BluetoothGattCharacteristic? =
            gatt.services.firstNotNullOfOrNull { service -> service.getCharacteristic(characteristicUuid) }
    }

    private fun Int.toSafeDetail(): SafeGattDetail? = SafeGattDetail(
        statusCode = this,
        statusLabel = if (this == BluetoothGatt.GATT_SUCCESS) "gatt_success" else "gatt_status_$this",
    )
}
