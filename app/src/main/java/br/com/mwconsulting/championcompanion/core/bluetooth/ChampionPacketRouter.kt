package br.com.mwconsulting.championcompanion.core.bluetooth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChampionPacketRouter(
    packetSource: Flow<ChampionPacket>,
    parentScope: CoroutineScope,
) {
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]))
    private val pendingMutex = Mutex()
    private val pending = linkedMapOf<Long, PendingAwaiter>()
    private val packetsFlow = MutableSharedFlow<ChampionPacket>(extraBufferCapacity = 64)
    private var nextAwaiterId = 1L

    val packets: SharedFlow<ChampionPacket> = packetsFlow

    init {
        scope.launch {
            packetSource.collect { packet ->
                packetsFlow.emit(packet)
                resolvePending(packet)
            }
        }
    }

    suspend fun awaitPacket(request: ChampionAwaitPacketRequest): ChampionBleResult<ChampionPacket> {
        val deferred = CompletableDeferred<ChampionPacket>()
        val awaiterId = pendingMutex.withLock {
            val id = nextAwaiterId++
            val timeoutJob = scope.launch {
                kotlinx.coroutines.delay(request.timeout)
                timeoutAwaiter(id, request)
            }
            pending[id] = PendingAwaiter(
                request = request,
                completion = deferred,
                timeoutJob = timeoutJob,
            )
            id
        }

        deferred.invokeOnCompletion {
            scope.launch { removeAwaiter(awaiterId) }
        }

        return try {
            ChampionBleResult.Success(deferred.await())
        } catch (_: Throwable) {
            val failure = pendingMutex.withLock {
                pending[awaiterId]?.failure ?: ChampionBleError.UnexpectedInternal
            }
            ChampionBleResult.Failure(failure)
        }
    }

    fun observeChannel(
        sessionId: ChampionSessionId,
        channel: ChampionChannelAlias,
    ): Flow<ChampionPacket> = packets.filter { packet ->
        packet.sessionId == sessionId && packet.channel == channel
    }

    suspend fun close(reason: ChampionDisconnectReason) {
        val awaiters = pendingMutex.withLock {
            val current = pending.values.toList()
            pending.clear()
            current
        }
        awaiters.forEach { awaiter ->
            awaiter.timeoutJob.cancel()
            awaiter.failure = ChampionBleError.QueueClosed(reason)
            awaiter.completion.cancel()
        }
    }

    private suspend fun resolvePending(packet: ChampionPacket) {
        val match = pendingMutex.withLock {
            val entry = pending.entries.firstOrNull { (_, awaiter) -> awaiter.request.matcher.matches(packet) }
            entry?.also { pending.remove(it.key) }?.value
        } ?: return

        match.timeoutJob.cancel()
        match.completion.complete(packet)
    }

    private suspend fun timeoutAwaiter(
        awaiterId: Long,
        request: ChampionAwaitPacketRequest,
    ) {
        val awaiter = pendingMutex.withLock { pending.remove(awaiterId) } ?: return
        awaiter.failure = ChampionBleError.Timeout(
            operation = ChampionOperationType.WAIT_FOR_RESPONSE,
            after = request.timeout,
        )
        awaiter.completion.cancel()
    }

    private suspend fun removeAwaiter(awaiterId: Long) {
        val awaiter = pendingMutex.withLock { pending.remove(awaiterId) } ?: return
        awaiter.timeoutJob.cancel()
    }

    private class PendingAwaiter(
        val request: ChampionAwaitPacketRequest,
        val completion: CompletableDeferred<ChampionPacket>,
        val timeoutJob: Job,
    ) {
        var failure: ChampionBleError = ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP)
    }
}
