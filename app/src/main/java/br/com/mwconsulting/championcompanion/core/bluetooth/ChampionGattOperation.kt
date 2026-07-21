package br.com.mwconsulting.championcompanion.core.bluetooth

import java.util.UUID
import kotlin.time.Duration

@JvmInline
value class ChampionOperationId(val value: UUID) {
    companion object {
        fun random(): ChampionOperationId = ChampionOperationId(UUID.randomUUID())
    }
}

sealed interface ChampionGattTarget {
    data object Session : ChampionGattTarget
    data class Channel(val alias: ChampionChannelAlias) : ChampionGattTarget
}

class ChampionGattOperation(
    val operationId: ChampionOperationId,
    val sessionId: ChampionSessionId,
    val type: ChampionOperationType,
    val target: ChampionGattTarget,
    val writeType: ChampionWriteType? = null,
    val timeout: Duration,
    val retryPolicy: ChampionRetryPolicy,
    val attempt: Int,
    payload: ByteArray? = null,
) {
    private val copiedPayload: ByteArray? = payload?.copyOf()

    init {
        require(attempt > 0) { "attempt must be positive" }
        require(timeout.isPositive()) { "timeout must be positive" }
    }

    val payloadSize: Int? = copiedPayload?.size

    fun payloadCopy(): ByteArray? = copiedPayload?.copyOf()

    fun nextAttempt(nextAttempt: Int): ChampionGattOperation = ChampionGattOperation(
        operationId = operationId,
        sessionId = sessionId,
        type = type,
        target = target,
        writeType = writeType,
        timeout = timeout,
        retryPolicy = retryPolicy,
        attempt = nextAttempt,
        payload = copiedPayload,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChampionGattOperation) return false

        return operationId == other.operationId &&
            sessionId == other.sessionId &&
            type == other.type &&
            target == other.target &&
            writeType == other.writeType &&
            timeout == other.timeout &&
            retryPolicy == other.retryPolicy &&
            attempt == other.attempt &&
            copiedPayload.contentEqualsNullable(other.copiedPayload)
    }

    override fun hashCode(): Int {
        var result = operationId.hashCode()
        result = 31 * result + sessionId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + target.hashCode()
        result = 31 * result + (writeType?.hashCode() ?: 0)
        result = 31 * result + timeout.hashCode()
        result = 31 * result + retryPolicy.hashCode()
        result = 31 * result + attempt
        result = 31 * result + (copiedPayload?.contentHashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "ChampionGattOperation(operationId=$operationId, sessionId=$sessionId, type=$type, target=$target, writeType=$writeType, timeout=$timeout, attempt=$attempt, payloadSize=$payloadSize)"

    companion object {
        fun create(
            sessionId: ChampionSessionId,
            type: ChampionOperationType,
            target: ChampionGattTarget,
            writeType: ChampionWriteType? = null,
            timeout: Duration,
            retryPolicy: ChampionRetryPolicy = ChampionRetryPolicy.None,
            payload: ByteArray? = null,
        ): ChampionGattOperation = ChampionGattOperation(
            operationId = ChampionOperationId.random(),
            sessionId = sessionId,
            type = type,
            target = target,
            writeType = writeType,
            timeout = timeout,
            retryPolicy = retryPolicy,
            attempt = 1,
            payload = payload,
        )
    }
}

sealed interface ChampionGattTerminalResult {
    data object Success : ChampionGattTerminalResult
    data class Failure(val error: ChampionBleError) : ChampionGattTerminalResult
}

private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean = when {
    this == null && other == null -> true
    this == null || other == null -> false
    else -> contentEquals(other)
}
