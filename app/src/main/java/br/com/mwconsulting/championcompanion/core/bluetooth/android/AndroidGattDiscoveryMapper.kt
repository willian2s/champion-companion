package br.com.mwconsulting.championcompanion.core.bluetooth.android

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelProperty
import java.util.UUID

data class AndroidGattCharacteristicView(
    val uuid: UUID,
    val properties: Set<ChampionChannelProperty>,
)

data class AndroidGattServiceView(
    val uuid: UUID,
    val characteristics: List<AndroidGattCharacteristicView>,
)

data class AndroidGattDiscoverySnapshot(
    val services: List<AndroidGattServiceView>,
)

class AndroidGattDiscoveryMapper {
    fun map(services: List<BluetoothGattService>): AndroidGattDiscoverySnapshot = AndroidGattDiscoverySnapshot(
        services = services.map { service ->
            AndroidGattServiceView(
                uuid = service.uuid,
                characteristics = service.characteristics.map { characteristic ->
                    AndroidGattCharacteristicView(
                        uuid = characteristic.uuid,
                        properties = characteristic.properties.toChannelProperties(),
                    )
                },
            )
        },
    )

    private fun Int.toChannelProperties(): Set<ChampionChannelProperty> = buildSet {
        if (this@toChannelProperties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
            add(ChampionChannelProperty.READ)
        }
        if (this@toChannelProperties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
            add(ChampionChannelProperty.WRITE)
        }
        if (this@toChannelProperties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
            add(ChampionChannelProperty.WRITE_WITHOUT_RESPONSE)
        }
        if (this@toChannelProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            add(ChampionChannelProperty.NOTIFY)
        }
        if (this@toChannelProperties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
            add(ChampionChannelProperty.INDICATE)
        }
    }
}
