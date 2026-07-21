package br.com.mwconsulting.championcompanion.core.bluetooth

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChampionSubscriptionRegistry {
    private val mutex = Mutex()
    private val nextSubscriberId = AtomicLong(1)
    private val states = linkedMapOf<ChampionChannelAlias, ChannelState>()

    suspend fun addSubscriber(
        channel: ChampionChannelAlias,
        sink: SendChannel<ChampionPacket>,
    ): SubscriptionRegistration = mutex.withLock {
        val state = states.getOrPut(channel) { ChannelState() }
        val subscriberId = nextSubscriberId.getAndIncrement()
        state.subscribers[subscriberId] = sink
        SubscriptionRegistration(
            channel = channel,
            subscriberId = subscriberId,
            shouldEnablePhysicalSubscription = !state.physicallyEnabled,
        )
    }

    suspend fun markPhysicallyEnabled(channel: ChampionChannelAlias) {
        mutex.withLock {
            states.getOrPut(channel) { ChannelState() }.physicallyEnabled = true
        }
    }

    suspend fun removeSubscriber(registration: SubscriptionRegistration): Int = mutex.withLock {
        val state = states[registration.channel] ?: return@withLock 0
        state.subscribers.remove(registration.subscriberId)
        state.subscribers.size
    }

    suspend fun broadcast(channel: ChampionChannelAlias, packet: ChampionPacket) {
        val subscribers = mutex.withLock { states[channel]?.subscribers?.values?.toList().orEmpty() }
        subscribers.forEach { subscriber -> subscriber.trySend(packet).isSuccess }
    }

    suspend fun closeAll(cause: Throwable? = null) {
        val subscribers = mutex.withLock {
            states.values.flatMap { it.subscribers.values }.also { states.clear() }
        }
        subscribers.forEach { subscriber -> subscriber.close(cause) }
    }

    suspend fun isPhysicallyEnabled(channel: ChampionChannelAlias): Boolean = mutex.withLock {
        states[channel]?.physicallyEnabled == true
    }

    data class SubscriptionRegistration(
        val channel: ChampionChannelAlias,
        val subscriberId: Long,
        val shouldEnablePhysicalSubscription: Boolean,
    )

    private data class ChannelState(
        var physicallyEnabled: Boolean = false,
        val subscribers: LinkedHashMap<Long, SendChannel<ChampionPacket>> = linkedMapOf(),
    )
}
