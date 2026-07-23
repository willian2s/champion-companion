package br.com.mwconsulting.championcompanion.core.bluetooth

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ChampionBleTimeouts(
    val connect: Duration = 12.seconds,
    val discovery: Duration = 10.seconds,
    val descriptorWrite: Duration = 8.seconds,
    val characteristicRead: Duration = 8.seconds,
    val characteristicWrite: Duration = 8.seconds,
    val mtuRequest: Duration = 8.seconds,
    val notificationConfiguration: Duration = 8.seconds,
    val indicationConfiguration: Duration = 8.seconds,
    val d1History: Duration = 15.seconds,
    val d3WorkoutIndex: Duration = 10.seconds,
    val d4WorkoutBlock: Duration = 15.seconds,
    val responseWait: Duration = 15.seconds,
    val disconnect: Duration = 5.seconds,
) {
    fun forOperation(type: ChampionOperationType): Duration = when (type) {
        ChampionOperationType.SCAN -> throw IllegalArgumentException("SCAN is not handled by GATT queue")
        ChampionOperationType.CONNECT -> connect
        ChampionOperationType.DISCOVER_SERVICES -> discovery
        ChampionOperationType.READ_CHARACTERISTIC -> characteristicRead
        ChampionOperationType.WRITE_CHARACTERISTIC -> characteristicWrite
        ChampionOperationType.WRITE_DESCRIPTOR -> descriptorWrite
        ChampionOperationType.REQUEST_MTU -> mtuRequest
        ChampionOperationType.CONFIGURE_NOTIFICATION -> notificationConfiguration
        ChampionOperationType.CONFIGURE_INDICATION -> indicationConfiguration
        ChampionOperationType.WAIT_FOR_RESPONSE -> responseWait
        ChampionOperationType.DISCONNECT -> disconnect
    }
}
