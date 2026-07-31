package br.com.mwconsulting.championcompanion.feature.exercises

import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSession
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionId
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionMutation
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionRepository
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionState
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.nextAuditInstant
import java.time.Instant
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

internal sealed interface StaleWorkoutReconciliation {
    data class ForegroundOwned(val session: AppWorkoutSession) : StaleWorkoutReconciliation
    data class RecoverySurface(val session: AppWorkoutSession) : StaleWorkoutReconciliation
    data object NoOpenSession : StaleWorkoutReconciliation
    data object Unavailable : StaleWorkoutReconciliation
}

private enum class OperationalOwnerState {
    ATTACHING,
    FOREGROUND,
    TEARING_DOWN,
}

private data class OperationalOwner(
    val token: Any,
    val sessionId: AppWorkoutSessionId,
    val state: OperationalOwnerState,
)

private sealed interface OpenSessionRead {
    data class Available(val session: AppWorkoutSession?) : OpenSessionRead
    data object Unavailable : OpenSessionRead
}

/**
 * Serializa ownership operacional e reconciliação stale no processo atual.
 * Mantém somente tokens opacos e IDs; nenhuma referência Android é retida.
 */
@Singleton
class WorkoutSessionOwnershipCoordinator @Inject constructor(
    private val sessionRepository: AppWorkoutSessionRepository,
) {
    /**
     * Semáforo não vinculado à thread: operações suspensas podem manter a
     * exclusão após trocar de thread. Callbacks síncronos nunca aguardam o
     * permit: invalidam o owner atomicamente e enfileiram reconciliação.
     */
    private val ownershipPermit = Semaphore(1, true)
    private val operationalOwner = AtomicReference<OperationalOwner?>(null)
    private val processScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    internal suspend fun <T> withSerializedServiceOperation(
        operation: suspend () -> T,
    ): T = withOwnershipExclusion(operation)

    /** Chamado pelo serviço somente dentro de [withSerializedServiceOperation]. */
    internal fun reserveAttachment(token: Any, sessionId: AppWorkoutSessionId): Boolean {
        while (true) {
            val current = operationalOwner.get()
            if (current != null && (current.token !== token || current.sessionId != sessionId)) {
                return false
            }
            if (current?.state == OperationalOwnerState.TEARING_DOWN) return false
            if (current?.state == OperationalOwnerState.FOREGROUND) return true
            val reserved = OperationalOwner(token, sessionId, OperationalOwnerState.ATTACHING)
            if (operationalOwner.compareAndSet(current, reserved)) return true
        }
    }

    internal fun confirmForeground(token: Any, sessionId: AppWorkoutSessionId): Boolean {
        while (true) {
            val current = operationalOwner.get() ?: return false
            if (current.token !== token || current.sessionId != sessionId) return false
            if (current.state == OperationalOwnerState.TEARING_DOWN) return false
            if (current.state == OperationalOwnerState.FOREGROUND) return true
            val foreground = current.copy(state = OperationalOwnerState.FOREGROUND)
            if (operationalOwner.compareAndSet(current, foreground)) return true
        }
    }

    internal fun release(token: Any) {
        while (true) {
            val current = operationalOwner.get() ?: return
            if (current.token !== token) return
            // Somente o fechamento process-scoped pode concluir TEARING_DOWN.
            if (current.state == OperationalOwnerState.TEARING_DOWN) return
            if (operationalOwner.compareAndSet(current, null)) return
        }
    }

    /**
     * Invalida ownership de forma síncrona no callback e conclui reconciliação
     * no escopo do processo, sem depender do escopo do serviço destruído.
     */
    internal fun releaseAfterServiceTeardown(
        token: Any,
        lastKnownSession: AppWorkoutSession?,
        pendingSessionId: AppWorkoutSessionId? = null,
    ) {
        val matchingOwner = operationalOwner.get()?.takeIf { it.token === token }
        val teardownSessionId = matchingOwner?.sessionId
            ?: pendingSessionId
            ?: lastKnownSession?.id
            ?: return
        // onDestroy pode vencer antes da coroutine de comando reservar ATTACHING.
        // Snapshot sintético instala tombstone somente se ainda não houver owner.
        val ownerSnapshot = matchingOwner ?: OperationalOwner(
            token = token,
            sessionId = teardownSessionId,
            state = OperationalOwnerState.ATTACHING,
        )
        val permitAcquired = ownershipPermit.tryAcquire()
        val owner = try {
            // Se outra operação detém o permit, a troca atômica ainda invalida
            // o owner imediatamente. Operações em curso revalidam este estado,
            // novo attach é bloqueado e a reconciliação abaixo fica enfileirada.
            markTearingDown(token, ownerSnapshot)
        } finally {
            if (permitAcquired) ownershipPermit.release()
        }
        processScope.launch {
            withOwnershipExclusion {
                try {
                    val candidate = when (val loaded = loadOpenSessionWithRetry(owner.sessionId)) {
                        is RepositoryResult.Success -> loaded.value
                        RepositoryResult.NotFound -> null
                        RepositoryResult.Conflict,
                        RepositoryResult.StorageUnavailable,
                        RepositoryResult.UnexpectedState,
                        -> lastKnownSession?.takeIf { it.id == owner.sessionId }
                    }
                    if (candidate != null && candidate.state in RUNNING_STATES) {
                        reconcileLocked(candidate)
                    }
                } finally {
                    completeTeardown(owner)
                }
            }
        }
    }

    private fun markTearingDown(
        token: Any,
        ownerSnapshot: OperationalOwner,
    ): OperationalOwner {
        while (true) {
            val current = operationalOwner.get()
            if (current == null) {
                // release comum pode competir com onDestroy. Reinstalar o
                // tombstone impede novo attach até a reconciliação enfileirada.
                val tearingDown = ownerSnapshot.copy(state = OperationalOwnerState.TEARING_DOWN)
                if (operationalOwner.compareAndSet(null, tearingDown)) return tearingDown
                continue
            }
            if (current.token !== token) {
                // Outro owner venceu após release seguro. Reconciliação do
                // snapshot ainda ocorrerá e preservará owner foreground válido.
                return ownerSnapshot.copy(state = OperationalOwnerState.TEARING_DOWN)
            }
            if (current.state == OperationalOwnerState.TEARING_DOWN) return current
            val tearingDown = current.copy(state = OperationalOwnerState.TEARING_DOWN)
            if (operationalOwner.compareAndSet(current, tearingDown)) return tearingDown
        }
    }

    internal suspend fun isForegroundOwner(sessionId: AppWorkoutSessionId): Boolean =
        withOwnershipExclusion { isForegroundOwnerLocked(sessionId) }

    internal suspend fun reconcileOpenSession(): StaleWorkoutReconciliation = withOwnershipExclusion {
        when (val openRead = readOpenSessionWithRetry()) {
            is OpenSessionRead.Available -> openRead.session
                ?.let { reconcileLocked(it) }
                ?: StaleWorkoutReconciliation.NoOpenSession
            OpenSessionRead.Unavailable -> StaleWorkoutReconciliation.Unavailable
        }
    }

    internal suspend fun reconcileSession(
        observedSession: AppWorkoutSession,
    ): StaleWorkoutReconciliation = withOwnershipExclusion {
        val session = when (val loaded = loadOpenSession(observedSession.id)) {
            is RepositoryResult.Success -> loaded.value
            RepositoryResult.NotFound -> return@withOwnershipExclusion StaleWorkoutReconciliation.NoOpenSession
            RepositoryResult.Conflict,
            RepositoryResult.StorageUnavailable,
            RepositoryResult.UnexpectedState,
            -> return@withOwnershipExclusion StaleWorkoutReconciliation.RecoverySurface(observedSession)
        }
        reconcileLocked(session)
    }

    private suspend fun reconcileLocked(
        initial: AppWorkoutSession,
    ): StaleWorkoutReconciliation {
        var candidate = initial
        repeat(MAX_RECONCILIATION_ATTEMPTS) { attempt ->
            if (candidate.state !in RUNNING_STATES) {
                return StaleWorkoutReconciliation.RecoverySurface(candidate)
            }

            // Revalidação ocorre sob mesma exclusão usada por attach/startForeground.
            if (isForegroundOwnerLocked(candidate.id)) {
                return StaleWorkoutReconciliation.ForegroundOwned(candidate)
            }

            when (val interrupted = interrupt(candidate)) {
                is RepositoryResult.Success ->
                    return StaleWorkoutReconciliation.RecoverySurface(interrupted.value)
                RepositoryResult.NotFound -> return StaleWorkoutReconciliation.NoOpenSession
                RepositoryResult.Conflict -> {
                    candidate = when (val reloaded = loadOpenSession(candidate.id)) {
                        is RepositoryResult.Success -> reloaded.value
                        RepositoryResult.NotFound -> return StaleWorkoutReconciliation.NoOpenSession
                        RepositoryResult.Conflict,
                        RepositoryResult.StorageUnavailable,
                        RepositoryResult.UnexpectedState,
                        -> return StaleWorkoutReconciliation.RecoverySurface(candidate)
                    }
                }
                RepositoryResult.StorageUnavailable,
                RepositoryResult.UnexpectedState,
                -> {
                    if (attempt == MAX_RECONCILIATION_ATTEMPTS - 1) {
                        return StaleWorkoutReconciliation.RecoverySurface(candidate)
                    }
                    delay(RECONCILIATION_RETRY_DELAY_MILLIS)
                    candidate = when (val reloaded = loadOpenSession(candidate.id)) {
                        is RepositoryResult.Success -> reloaded.value
                        RepositoryResult.NotFound -> return StaleWorkoutReconciliation.NoOpenSession
                        RepositoryResult.Conflict,
                        RepositoryResult.StorageUnavailable,
                        RepositoryResult.UnexpectedState,
                        -> candidate
                    }
                }
            }
        }
        return StaleWorkoutReconciliation.RecoverySurface(candidate)
    }

    private suspend fun interrupt(
        session: AppWorkoutSession,
    ): RepositoryResult<AppWorkoutSession> = try {
        sessionRepository.interrupt(
            AppWorkoutSessionMutation(
                sessionId = session.id,
                expectedRevision = session.revision,
                at = session.nextAuditInstant(Instant.now()),
                activeDuration = session.activeDuration,
                pausedDuration = session.pausedDuration,
            ),
        )
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        RepositoryResult.StorageUnavailable
    }

    private fun isForegroundOwnerLocked(sessionId: AppWorkoutSessionId): Boolean =
        operationalOwner.get()?.let {
            it.sessionId == sessionId && it.state == OperationalOwnerState.FOREGROUND
        } == true

    private fun completeTeardown(owner: OperationalOwner) {
        operationalOwner.compareAndSet(owner, null)
    }

    private suspend fun <T> withOwnershipExclusion(
        operation: suspend () -> T,
    ): T = withContext(Dispatchers.Default) {
        val acquired = AtomicBoolean(false)
        try {
            runInterruptible(Dispatchers.IO) {
                ownershipPermit.acquire()
                acquired.set(true)
            }
            operation()
        } finally {
            // runInterruptible pode propagar cancelamento logo após acquire.
            // Flag atômica garante devolução mesmo antes de iniciar operation.
            if (acquired.compareAndSet(true, false)) ownershipPermit.release()
        }
    }

    private suspend fun readOpenSessionWithRetry(): OpenSessionRead {
        repeat(MAX_OPEN_READ_ATTEMPTS) { attempt ->
            try {
                return OpenSessionRead.Available(sessionRepository.observeOpen().first())
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (attempt < MAX_OPEN_READ_ATTEMPTS - 1) {
                    delay(RECONCILIATION_RETRY_DELAY_MILLIS)
                }
            }
        }
        return OpenSessionRead.Unavailable
    }

    private suspend fun loadOpenSession(
        sessionId: AppWorkoutSessionId,
    ): RepositoryResult<AppWorkoutSession> = try {
        sessionRepository.loadOpenForRecovery(sessionId)
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        RepositoryResult.StorageUnavailable
    }

    private suspend fun loadOpenSessionWithRetry(
        sessionId: AppWorkoutSessionId,
    ): RepositoryResult<AppWorkoutSession> {
        var lastResult: RepositoryResult<AppWorkoutSession> = RepositoryResult.StorageUnavailable
        repeat(MAX_OPEN_READ_ATTEMPTS) { attempt ->
            val result = loadOpenSession(sessionId)
            if (result is RepositoryResult.Success || result == RepositoryResult.NotFound) {
                return result
            }
            lastResult = result
            if (attempt < MAX_OPEN_READ_ATTEMPTS - 1) {
                delay(RECONCILIATION_RETRY_DELAY_MILLIS)
            }
        }
        return lastResult
    }

    private companion object {
        const val MAX_RECONCILIATION_ATTEMPTS = 2
        const val MAX_OPEN_READ_ATTEMPTS = 2
        const val RECONCILIATION_RETRY_DELAY_MILLIS = 200L
        val RUNNING_STATES = setOf(
            AppWorkoutSessionState.ACTIVE,
            AppWorkoutSessionState.PAUSED,
        )
    }
}
