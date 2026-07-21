package br.com.mwconsulting.championcompanion.core.bluetooth

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionBleGattQueueTest {
    @Test
    fun `queue processes fifo with one active operation at time`() = runBlocking {
        val sessionId = testSessionId()
        val executed = mutableListOf<ChampionGattOperation>()
        val queue = newQueue(sessionId, executed)
        val first = testOperation(sessionId = sessionId)
        val second = testOperation(sessionId = sessionId, type = ChampionOperationType.READ_CHARACTERISTIC)

        val firstResult = async { queue.enqueue(first) }
        val secondResult = async { queue.enqueue(second) }

        awaitCondition { executed.size == 1 }
        assertEquals(first, queue.activeOperation.value)
        queue.complete(sessionId, first.operationId)

        awaitCondition { executed.size == 2 }
        assertEquals(second, queue.activeOperation.value)
        queue.complete(sessionId, second.operationId)

        assertEquals(ChampionGattTerminalResult.Success, firstResult.await())
        assertEquals(ChampionGattTerminalResult.Success, secondResult.await())
        assertEquals(listOf(first, second), executed)
        assertNull(queue.activeOperation.value)
    }

    @Test
    fun `queue times out and ignores stale completion from old session`() = runBlocking {
        val sessionId = testSessionId()
        val queue = newQueue(sessionId)
        val operation = testOperation(sessionId = sessionId, timeout = 30.milliseconds)

        val result = async { queue.enqueue(operation) }

        assertEquals(
            ChampionGattTerminalResult.Failure(
                ChampionBleError.Timeout(operation.type, operation.timeout),
            ),
            result.await(),
        )

        queue.complete(testSessionId(2), operation.operationId)
        queue.complete(sessionId, operation.operationId)
        assertNull(queue.activeOperation.value)
    }

    @Test
    fun `queue supports cancellation retries and duplicate callback protection`() = runBlocking {
        val sessionId = testSessionId()
        val executed = mutableListOf<ChampionGattOperation>()
        val queue = newQueue(sessionId, executed)
        val cancellable = testOperation(sessionId = sessionId)

        val cancelled = async { queue.enqueue(cancellable) }
        awaitCondition { executed.isNotEmpty() }
        queue.cancel(sessionId, cancellable.operationId, ChampionCancellationReason.USER_REQUESTED)
        queue.complete(sessionId, cancellable.operationId)
        assertEquals(
            ChampionGattTerminalResult.Failure(
                ChampionBleError.OperationCancelled(ChampionCancellationReason.USER_REQUESTED),
            ),
            cancelled.await(),
        )

        val retrying = testOperation(
            sessionId = sessionId,
            retryPolicy = ChampionRetryPolicy.Limited(maxAttempts = 2),
        )
        val retryResult = async { queue.enqueue(retrying) }
        awaitCondition { executed.size == 2 }
        queue.fail(
            sessionId,
            retrying.operationId,
            ChampionBleError.CallbackFailure(retrying.type, null),
        )
        awaitCondition { executed.size == 3 }
        assertEquals(2, executed.last().attempt)
        queue.complete(sessionId, retrying.operationId)
        queue.fail(sessionId, retrying.operationId, ChampionBleError.UnexpectedInternal)
        assertEquals(ChampionGattTerminalResult.Success, retryResult.await())

        val nonRetrying = testOperation(sessionId = sessionId)
        val nonRetryResult = async { queue.enqueue(nonRetrying) }
        awaitCondition { executed.size == 4 }
        queue.fail(
            sessionId,
            nonRetrying.operationId,
            ChampionBleError.CallbackFailure(nonRetrying.type, null),
        )
        assertEquals(
            ChampionGattTerminalResult.Failure(
                ChampionBleError.CallbackFailure(nonRetrying.type, null),
            ),
            nonRetryResult.await(),
        )
        assertEquals(4, executed.size)
    }

    @Test
    fun `disconnect cancels active fails pending and keeps queue closed`() = runBlocking {
        val sessionId = testSessionId()
        val executed = mutableListOf<ChampionGattOperation>()
        val queue = newQueue(sessionId, executed)
        val active = testOperation(sessionId = sessionId, timeout = 500.milliseconds)
        val pending = testOperation(sessionId = sessionId, type = ChampionOperationType.REQUEST_MTU)

        val activeResult = async { queue.enqueue(active) }
        val pendingResult = async { queue.enqueue(pending) }
        awaitCondition { executed.size == 1 }

        queue.disconnect(ChampionDisconnectReason.USER_REQUESTED)

        assertEquals(
            ChampionGattTerminalResult.Failure(
                ChampionBleError.OperationCancelled(ChampionCancellationReason.DISCONNECT),
            ),
            activeResult.await(),
        )
        assertEquals(
            ChampionGattTerminalResult.Failure(
                ChampionBleError.QueueClosed(ChampionDisconnectReason.USER_REQUESTED),
            ),
            pendingResult.await(),
        )

        assertTrue(executed.size == 1)
    }

    @Test
    fun `disconnect is safe when called again after queue closes`() = runBlocking {
        val sessionId = testSessionId()
        val queue = newQueue(sessionId)

        queue.disconnect(ChampionDisconnectReason.USER_REQUESTED)
        queue.disconnect(ChampionDisconnectReason.CLEANUP)

        val result = queue.enqueue(testOperation(sessionId = sessionId))
        assertEquals(
            ChampionGattTerminalResult.Failure(
                ChampionBleError.QueueClosed(ChampionDisconnectReason.USER_REQUESTED),
            ),
            result,
        )
    }

    private fun newQueue(
        sessionId: ChampionSessionId,
        executed: MutableList<ChampionGattOperation> = mutableListOf(),
    ): ChampionGattQueue = ChampionGattQueue(
        sessionId = sessionId,
        parentScope = CoroutineScope(Dispatchers.Default),
    ) { operation ->
        executed += operation
    }
}
