package br.com.mwconsulting.championcompanion.core.bluetooth

import br.com.mwconsulting.championcompanion.core.bluetooth.internal.ChampionSessionActor
import java.util.ArrayDeque
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChampionGattQueue(
    private val sessionId: ChampionSessionId,
    parentScope: CoroutineScope,
    private val operationExecutor: suspend (ChampionGattOperation) -> Unit,
) {
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]))
    private val actor = ChampionSessionActor<QueueMessage>(scope, ::handleMessage)
    private val pending = ArrayDeque<QueuedOperation>()
    private val mutableActiveOperation = MutableStateFlow<ChampionGattOperation?>(null)
    private var active: ActiveOperation? = null
    private var closedReason: ChampionDisconnectReason? = null

    val activeOperation: StateFlow<ChampionGattOperation?> = mutableActiveOperation.asStateFlow()

    suspend fun enqueue(operation: ChampionGattOperation): ChampionGattTerminalResult {
        require(operation.sessionId == sessionId) { "operation belongs to another session" }
        val completion = CompletableDeferred<ChampionGattTerminalResult>()
        actor.send(QueueMessage.Enqueue(QueuedOperation(operation, completion)))
        return completion.await()
    }

    suspend fun complete(sessionId: ChampionSessionId, operationId: ChampionOperationId) {
        actor.send(QueueMessage.Resolve(sessionId, operationId, ChampionGattTerminalResult.Success))
    }

    suspend fun fail(
        sessionId: ChampionSessionId,
        operationId: ChampionOperationId,
        error: ChampionBleError,
    ) {
        actor.send(QueueMessage.Resolve(sessionId, operationId, ChampionGattTerminalResult.Failure(error)))
    }

    suspend fun cancel(
        sessionId: ChampionSessionId,
        operationId: ChampionOperationId,
        reason: ChampionCancellationReason,
    ) {
        actor.send(
            QueueMessage.Resolve(
                sessionId = sessionId,
                operationId = operationId,
                result = ChampionGattTerminalResult.Failure(ChampionBleError.OperationCancelled(reason)),
            ),
        )
    }

    suspend fun disconnect(reason: ChampionDisconnectReason) {
        actor.send(QueueMessage.Disconnect(reason))
    }

    private suspend fun handleMessage(message: QueueMessage) {
        when (message) {
            is QueueMessage.Enqueue -> handleEnqueue(message.operation)
            is QueueMessage.Resolve -> handleResolve(message)
            is QueueMessage.Timeout -> handleTimeout(message.operationId)
            is QueueMessage.Disconnect -> handleDisconnect(message.reason)
        }
    }

    private suspend fun handleEnqueue(queued: QueuedOperation) {
        val closeReason = closedReason
        if (closeReason != null) {
            queued.completion.complete(ChampionGattTerminalResult.Failure(ChampionBleError.QueueClosed(closeReason)))
            return
        }
        pending.addLast(queued)
        startNextIfIdle()
    }

    private suspend fun handleResolve(message: QueueMessage.Resolve) {
        if (message.sessionId != sessionId) return
        val current = active ?: return
        if (current.operation.operationId != message.operationId) return

        current.timeoutJob.cancel()
        active = null
        mutableActiveOperation.value = null

        when (val result = message.result) {
            ChampionGattTerminalResult.Success -> {
                current.completion.complete(ChampionGattTerminalResult.Success)
                startNextIfIdle()
            }

            is ChampionGattTerminalResult.Failure -> {
                val retryDecision = current.operation.retryPolicy.retryDecisionFor(
                    currentAttempt = current.operation.attempt,
                    error = result.error,
                )
                if (retryDecision.shouldRetry) {
                    pending.addFirst(
                        QueuedOperation(
                            operation = current.operation.nextAttempt(retryDecision.nextAttempt),
                            completion = current.completion,
                        ),
                    )
                } else {
                    current.completion.complete(result)
                }
                startNextIfIdle()
            }
        }
    }

    private suspend fun handleTimeout(operationId: ChampionOperationId) {
        val current = active ?: return
        if (current.operation.operationId != operationId) return
        handleResolve(
            QueueMessage.Resolve(
                sessionId = sessionId,
                operationId = operationId,
                result = ChampionGattTerminalResult.Failure(
                    ChampionBleError.Timeout(
                        operation = current.operation.type,
                        after = current.operation.timeout,
                    ),
                ),
            ),
        )
    }

    private suspend fun handleDisconnect(reason: ChampionDisconnectReason) {
        if (closedReason != null) return
        closedReason = reason

        active?.let { current ->
            current.timeoutJob.cancel()
            current.completion.complete(
                ChampionGattTerminalResult.Failure(
                    ChampionBleError.OperationCancelled(ChampionCancellationReason.DISCONNECT),
                ),
            )
        }
        active = null
        mutableActiveOperation.value = null

        while (pending.isNotEmpty()) {
            pending.removeFirst().completion.complete(
                ChampionGattTerminalResult.Failure(ChampionBleError.QueueClosed(reason)),
            )
        }

        actor.close()
    }

    private suspend fun startNextIfIdle() {
        if (active != null || closedReason != null) return
        if (pending.isEmpty()) return
        val next = pending.removeFirst()
        val timeoutJob = scope.launch {
            withContext(Dispatchers.Default) {
                kotlinx.coroutines.delay(next.operation.timeout)
            }
            actor.send(QueueMessage.Timeout(next.operation.operationId))
        }
        active = ActiveOperation(next.operation, next.completion, timeoutJob)
        mutableActiveOperation.value = next.operation
        try {
            operationExecutor(next.operation)
        } catch (_: Exception) {
            handleResolve(
                QueueMessage.Resolve(
                    sessionId = sessionId,
                    operationId = next.operation.operationId,
                    result = ChampionGattTerminalResult.Failure(ChampionBleError.UnexpectedInternal),
                ),
            )
        }
    }

    private sealed interface QueueMessage {
        data class Enqueue(val operation: QueuedOperation) : QueueMessage
        data class Resolve(
            val sessionId: ChampionSessionId,
            val operationId: ChampionOperationId,
            val result: ChampionGattTerminalResult,
        ) : QueueMessage

        data class Timeout(val operationId: ChampionOperationId) : QueueMessage
        data class Disconnect(val reason: ChampionDisconnectReason) : QueueMessage
    }

    private data class QueuedOperation(
        val operation: ChampionGattOperation,
        val completion: CompletableDeferred<ChampionGattTerminalResult>,
    )

    private data class ActiveOperation(
        val operation: ChampionGattOperation,
        val completion: CompletableDeferred<ChampionGattTerminalResult>,
        val timeoutJob: Job,
    )
}
