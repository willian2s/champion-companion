package br.com.mwconsulting.championcompanion.core.bluetooth

import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidCccdConfigurator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class ChampionNotificationHub(
    private val sessionCapabilities: ChampionSessionCapabilities,
    private val queue: ChampionGattQueue,
    sessionEvents: Flow<ChampionGattSessionEvent>,
    private val scope: CoroutineScope,
    private val registry: ChampionSubscriptionRegistry = ChampionSubscriptionRegistry(),
    private val cccdConfigurator: AndroidCccdConfigurator = AndroidCccdConfigurator(),
) {
    private var closed = false
    private val collectorJob: Job = scope.launch {
        sessionEvents.collect { event ->
            if (event.sessionId != sessionCapabilities.sessionId) return@collect
            when (event) {
                is ChampionGattSessionEvent.CharacteristicChanged -> {
                    val channel = event.channel
                    val packet = event.packet
                    if (channel != null && packet != null) {
                        registry.broadcast(channel, packet)
                    }
                }

                is ChampionGattSessionEvent.ConnectionStateChanged -> {
                    if (!event.isConnected) {
                        close(ChampionDisconnectReason.TRANSPORT_FAILURE)
                    }
                }

                is ChampionGattSessionEvent.Closed -> close(ChampionDisconnectReason.CLEANUP)

                is ChampionGattSessionEvent.ServicesDiscovered,
                is ChampionGattSessionEvent.CharacteristicRead,
                is ChampionGattSessionEvent.CharacteristicWrite,
                is ChampionGattSessionEvent.DescriptorWrite,
                is ChampionGattSessionEvent.MtuChanged,
                -> Unit
            }
        }
    }

    suspend fun subscribe(channel: ChampionChannelAlias): ChampionBleResult<Flow<ChampionPacket>> {
        if (closed) return ChampionBleResult.Failure(ChampionBleError.QueueClosed(ChampionDisconnectReason.CLEANUP))
        if (!sessionCapabilities.snapshot.hasChannel(channel)) {
            return ChampionBleResult.Failure(ChampionBleError.MissingRequiredChannel(channel))
        }

        val flow = callbackFlow {
            val registration = registry.addSubscriber(channel, this)
            if (registration.shouldEnablePhysicalSubscription && !registry.isPhysicallyEnabled(channel)) {
                val enableResult = cccdConfigurator.enableChannel(
                    sessionId = sessionCapabilities.sessionId,
                    channel = channel,
                    capabilities = sessionCapabilities.snapshot,
                    queue = queue,
                )
                if (enableResult is ChampionBleResult.Failure) {
                    registry.removeSubscriber(registration)
                    close(RuntimeException(enableResult.error.toSafeMessage()))
                    return@callbackFlow
                }
                registry.markPhysicallyEnabled(channel)
            }

            awaitClose {
                scope.launch { registry.removeSubscriber(registration) }
            }
        }
        return ChampionBleResult.Success(flow)
    }

    suspend fun close(reason: ChampionDisconnectReason) {
        if (closed) return
        closed = true
        collectorJob.cancel()
        registry.closeAll(RuntimeException("notification_hub_closed:$reason"))
    }
}
