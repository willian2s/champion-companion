package br.com.mwconsulting.championcompanion.feature.exercises

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSession
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionCheckpoint
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionId
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionMutation
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionRepository
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionState
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.nextAuditInstant
import br.com.mwconsulting.championcompanion.core.notification.WorkoutNotificationBuilder
import br.com.mwconsulting.championcompanion.core.notification.WorkoutNotificationChannel
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Runtime monotônico; cada snapshot também alimenta a duração persistida. */
private class WorkoutTimer {
    private var baseActiveNanos: Long = 0L
    private var basePausedNanos: Long = 0L
    private var activeSinceNanos: Long? = null
    private var pausedSinceNanos: Long? = null

    fun setBase(
        activeDuration: Duration,
        pausedDuration: Duration,
        state: AppWorkoutSessionState,
        anchorNanos: Long = SystemClock.elapsedRealtimeNanos(),
    ) {
        baseActiveNanos = activeDuration.toNanos()
        basePausedNanos = pausedDuration.toNanos()
        activeSinceNanos = anchorNanos.takeIf { state == AppWorkoutSessionState.ACTIVE }
        pausedSinceNanos = anchorNanos.takeIf { state == AppWorkoutSessionState.PAUSED }
    }

    fun snapshot(): DurationSnapshot {
        val now = SystemClock.elapsedRealtimeNanos()
        val active = baseActiveNanos + (activeSinceNanos?.let { now - it } ?: 0L)
        val paused = basePausedNanos + (pausedSinceNanos?.let { now - it } ?: 0L)
        return DurationSnapshot(
            activeDuration = Duration.ofNanos(active),
            pausedDuration = Duration.ofNanos(paused),
            capturedAtNanos = now,
        )
    }
}

private data class DurationSnapshot(
    val activeDuration: Duration,
    val pausedDuration: Duration,
    val capturedAtNanos: Long,
)

private sealed interface OwnershipReconciliation {
    data class RecoveryConfirmed(val session: AppWorkoutSession) : OwnershipReconciliation
    data class OpenSurfaceConfirmed(val session: AppWorkoutSession) : OwnershipReconciliation
    data object NoOpenSession : OwnershipReconciliation
    data object Unconfirmed : OwnershipReconciliation
}

@AndroidEntryPoint
class WorkoutRecordingService : Service() {
    @Inject lateinit var sessionRepository: AppWorkoutSessionRepository
    @Inject lateinit var notificationBuilder: WorkoutNotificationBuilder
    @Inject lateinit var ownershipCoordinator: WorkoutSessionOwnershipCoordinator

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val commandMutex = Mutex()
    private val timer = WorkoutTimer()
    private val ownerToken = Any()
    private val teardownRequested = AtomicBoolean(false)

    @Volatile
    private var pendingSessionId: AppWorkoutSessionId? = null
    private var sessionId: AppWorkoutSessionId? = null
    private var latestSession: AppWorkoutSession? = null
    private var appliedRevision: Long? = null
    private var checkpointJob: Job? = null
    private var sessionObserverJob: Job? = null
    private var notificationUpdateJob: Job? = null
    private var isForeground = false
    private var isStopping = false

    companion object {
        const val ACTION_START = "br.com.mwconsulting.championcompanion.action.START_SESSION"
        const val ACTION_PAUSE = "br.com.mwconsulting.championcompanion.action.PAUSE_SESSION"
        const val ACTION_RESUME = "br.com.mwconsulting.championcompanion.action.RESUME_SESSION"
        const val ACTION_FINISH = "br.com.mwconsulting.championcompanion.action.FINISH_SESSION"
        const val EXTRA_SESSION_ID = "session_id"
        const val CHECKPOINT_INTERVAL_MILLIS = 30_000L
        const val NOTIFICATION_UPDATE_INTERVAL_MILLIS = 2_000L
        private const val MAX_OWNERSHIP_RECONCILIATION_ATTEMPTS = 2
        private val RUNNING_SESSION_STATES = setOf(
            AppWorkoutSessionState.ACTIVE,
            AppWorkoutSessionState.PAUSED,
        )

        fun startIntent(context: android.content.Context, sessionId: String): Intent =
            Intent(context, WorkoutRecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
    }

    override fun onCreate() {
        super.onCreate()
        WorkoutNotificationChannel.create(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.sessionIdOrNull()
        if (id != null && !registerPendingSession(id)) return START_NOT_STICKY
        when (intent?.action) {
            ACTION_START -> id?.let(::handleStart)
            ACTION_PAUSE -> id?.let(::handlePause)
            ACTION_RESUME -> id?.let(::handleResume)
            ACTION_FINISH -> id?.let(::handleFinish)
            else -> if (!isForeground) stopSelf(startId)
        }
        if (id == null && !isForeground) stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        scope.launch { interruptAndStop() }
    }

    override fun onDestroy() {
        requestProcessScopedTeardown()
        checkpointJob?.cancel()
        sessionObserverJob?.cancel()
        notificationUpdateJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun Intent.sessionIdOrNull(): AppWorkoutSessionId? {
        val rawId = getStringExtra(EXTRA_SESSION_ID) ?: return null
        return runCatching { AppWorkoutSessionId(UUID.fromString(rawId)) }.getOrNull()
    }

    @Synchronized
    private fun registerPendingSession(id: AppWorkoutSessionId): Boolean {
        val current = pendingSessionId
        if (current == id) return true
        if (current != null) return false
        pendingSessionId = id
        return true
    }

    private fun handleStart(id: AppWorkoutSessionId) {
        launchCommand(id) {
            if (!hasWorkoutNotificationPermission(this)) {
                compensateFailedOwnership(id)
                return@launchCommand
            }
            ensureAttached(id)
        }
    }

    private fun handlePause(id: AppWorkoutSessionId) {
        launchCommand(id) {
            val session = ensureAttached(id) ?: return@launchCommand
            when (session.state) {
                AppWorkoutSessionState.PAUSED -> Unit
                AppWorkoutSessionState.ACTIVE -> {
                    val snapshot = timer.snapshot()
                    val result = sessionRepository.pause(
                        timedMutation(session, snapshot),
                    )
                    applyMutationResult(id, result, snapshot.capturedAtNanos)
                }
                else -> Unit
            }
        }
    }

    private fun handleResume(id: AppWorkoutSessionId) {
        launchCommand(id) {
            val session = ensureAttached(id) ?: return@launchCommand
            when (session.state) {
                AppWorkoutSessionState.ACTIVE -> Unit
                AppWorkoutSessionState.PAUSED -> {
                    if (!hasWorkoutNotificationPermission(this)) return@launchCommand
                    val snapshot = timer.snapshot()
                    val result = sessionRepository.resume(
                        timedMutation(session, snapshot),
                    )
                    applyMutationResult(id, result, snapshot.capturedAtNanos)
                }
                else -> Unit
            }
        }
    }

    private fun handleFinish(id: AppWorkoutSessionId) {
        launchCommand(id) {
            val attached = ensureAttached(id) ?: return@launchCommand
            if (attached.state == AppWorkoutSessionState.FINISHING) {
                cleanupAndStop()
                return@launchCommand
            }
            if (attached.state != AppWorkoutSessionState.ACTIVE &&
                attached.state != AppWorkoutSessionState.PAUSED
            ) {
                return@launchCommand
            }

            val checkpointSnapshot = timer.snapshot()
            val checkpointed = when (
                val checkpointResult = sessionRepository.checkpoint(
                    AppWorkoutSessionCheckpoint(
                        sessionId = id,
                        expectedRevision = attached.revision,
                        runtimeGeneration = attached.runtimeGeneration,
                        at = auditInstant(attached, checkpointSnapshot),
                        aggregates = attached.aggregates,
                        activeDuration = checkpointSnapshot.activeDuration,
                        pausedDuration = checkpointSnapshot.pausedDuration,
                    ),
                )
            ) {
                is RepositoryResult.Success -> checkpointResult.value.also {
                    applySession(it, checkpointSnapshot.capturedAtNanos)
                }
                RepositoryResult.Conflict -> reloadSession(id)?.also(::applySession)
                else -> null
            } ?: return@launchCommand

            if (checkpointed.state == AppWorkoutSessionState.FINISHING) {
                cleanupAndStop()
                return@launchCommand
            }
            if (checkpointed.state != AppWorkoutSessionState.ACTIVE &&
                checkpointed.state != AppWorkoutSessionState.PAUSED
            ) {
                return@launchCommand
            }

            val finishingSnapshot = timer.snapshot()
            when (
                val finishResult = sessionRepository.beginFinishing(
                    timedMutation(checkpointed, finishingSnapshot),
                )
            ) {
                is RepositoryResult.Success -> {
                    applySession(finishResult.value, finishingSnapshot.capturedAtNanos)
                    cleanupAndStop()
                }
                RepositoryResult.Conflict -> {
                    val refreshed = reloadSession(id)
                    if (refreshed?.state == AppWorkoutSessionState.FINISHING) {
                        applySession(refreshed)
                        cleanupAndStop()
                    } else if (refreshed != null) {
                        applySession(refreshed)
                    }
                }
                else -> Unit
            }
        }
    }

    private fun launchCommand(
        id: AppWorkoutSessionId,
        command: suspend () -> Unit,
    ) {
        scope.launch {
            runSerializedCommand {
                try {
                    command()
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    compensateFailedOwnership(id)
                }
            }
        }
    }

    private suspend fun ensureAttached(id: AppWorkoutSessionId): AppWorkoutSession? {
        if (sessionId != null && sessionId != id) return null
        if (!ownershipCoordinator.reserveAttachment(ownerToken, id)) {
            cleanupAndStop()
            return null
        }
        latestSession?.takeIf { it.id == id && isForeground }?.let { return it }

        val session = try {
            reloadSession(id)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            requestProcessScopedTeardown(id)
            cleanupAndStop()
            return null
        } ?: run {
            cleanupAndStop()
            return null
        }

        if (session.state == AppWorkoutSessionState.CREATED) {
            cleanupAndStop()
            return null
        }

        if (session.state != AppWorkoutSessionState.ACTIVE &&
            session.state != AppWorkoutSessionState.PAUSED &&
            session.state != AppWorkoutSessionState.FINISHING
        ) {
            cleanupAndStop()
            return null
        }

        attachSession(session)
        return session
    }

    private fun attachSession(session: AppWorkoutSession) {
        if (sessionId != null && sessionId != session.id) return
        sessionId = session.id
        applySession(session)

        if (sessionObserverJob?.isActive != true) {
            startSessionObserver(session.id)
        }
        if (checkpointJob?.isActive != true) {
            startCheckpointJob(session.id)
        }
        if (notificationUpdateJob?.isActive != true) {
            startNotificationUpdates()
        }
    }

    private fun startSessionObserver(id: AppWorkoutSessionId) {
        sessionObserverJob = scope.launch {
            sessionRepository.observeSession(id).collect { session ->
                runSerializedCommand {
                    if (session == null) {
                        cleanupAndStop()
                        return@runSerializedCommand
                    }
                    when (session.state) {
                        AppWorkoutSessionState.ACTIVE,
                        AppWorkoutSessionState.PAUSED,
                        -> applySession(session)
                        AppWorkoutSessionState.FINISHING -> {
                            applySession(session)
                            cleanupAndStop()
                        }
                        AppWorkoutSessionState.AWAITING_RECOVERY,
                        AppWorkoutSessionState.FINISHED,
                        AppWorkoutSessionState.DISCARDED,
                        AppWorkoutSessionState.CREATED,
                        -> cleanupAndStop()
                    }
                }
            }
        }
    }

    private fun startCheckpointJob(id: AppWorkoutSessionId) {
        checkpointJob = scope.launch {
            while (true) {
                delay(CHECKPOINT_INTERVAL_MILLIS)
                runSerializedCommand {
                    val session = latestSession ?: return@runSerializedCommand
                    if (session.id != id ||
                        (session.state != AppWorkoutSessionState.ACTIVE &&
                            session.state != AppWorkoutSessionState.PAUSED)
                    ) {
                        return@runSerializedCommand
                    }

                    val snapshot = timer.snapshot()
                    when (
                        val result = sessionRepository.checkpoint(
                            AppWorkoutSessionCheckpoint(
                                sessionId = id,
                                expectedRevision = session.revision,
                                runtimeGeneration = session.runtimeGeneration,
                                at = auditInstant(session, snapshot),
                                aggregates = session.aggregates,
                                activeDuration = snapshot.activeDuration,
                                pausedDuration = snapshot.pausedDuration,
                            ),
                        )
                    ) {
                        is RepositoryResult.Success -> applySession(result.value, snapshot.capturedAtNanos)
                        RepositoryResult.Conflict -> reloadSession(id)?.let(::applySession)
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun startNotificationUpdates() {
        notificationUpdateJob = scope.launch {
            while (true) {
                delay(NOTIFICATION_UPDATE_INTERVAL_MILLIS)
                val session = latestSession ?: continue
                if (!isForeground) continue
                val snapshot = timer.snapshot()
                val notification = notificationBuilder.buildNotification(
                    session = session,
                    overrideActiveDuration = snapshot.activeDuration,
                    overridePausedDuration = snapshot.pausedDuration,
                )
                notificationManager()?.notify(WorkoutNotificationChannel.NOTIFICATION_ID, notification)
            }
        }
    }

    private fun applyMutationResult(
        id: AppWorkoutSessionId,
        result: RepositoryResult<AppWorkoutSession>,
        runtimeAnchorNanos: Long,
    ) {
        when (result) {
            is RepositoryResult.Success -> applySession(result.value, runtimeAnchorNanos)
            RepositoryResult.Conflict -> scope.launch {
                runSerializedCommand {
                    reloadSession(id)?.let(::applySession)
                }
            }
            else -> Unit
        }
    }

    private fun applySession(
        session: AppWorkoutSession,
        runtimeAnchorNanos: Long? = null,
    ) {
        if (sessionId != null && sessionId != session.id) return
        sessionId = session.id

        val shouldResetRuntime = appliedRevision != session.revision
        latestSession = session
        if (shouldResetRuntime) {
            appliedRevision = session.revision
            timer.setBase(
                activeDuration = session.activeDuration,
                pausedDuration = session.pausedDuration,
                state = session.state,
                anchorNanos = runtimeAnchorNanos ?: SystemClock.elapsedRealtimeNanos(),
            )
        }

        promoteOrUpdateForeground(session)
    }

    private fun promoteOrUpdateForeground(session: AppWorkoutSession) {
        if (!ownershipCoordinator.reserveAttachment(ownerToken, session.id)) {
            cleanupAndStop()
            return
        }
        val notification = notificationBuilder.buildNotification(session)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                if (session.state == AppWorkoutSessionState.ACTIVE && session.recordsRoute) {
                    serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }
                startForeground(
                    WorkoutNotificationChannel.NOTIFICATION_ID,
                    notification,
                    serviceType,
                )
            } else {
                startForeground(
                    WorkoutNotificationChannel.NOTIFICATION_ID,
                    notification,
                )
            }
            check(ownershipCoordinator.confirmForeground(ownerToken, session.id))
            isForeground = true
        } catch (error: Exception) {
            ownershipCoordinator.release(ownerToken)
            throw error
        }
    }

    private suspend fun reloadSession(id: AppWorkoutSessionId): AppWorkoutSession? =
        sessionRepository.observeSession(id).first()

    private suspend fun interruptAndStop() {
        runSerializedCommand {
            val id = sessionId
            if (id == null) {
                cleanupAndStop()
            } else {
                compensateFailedOwnership(id)
            }
        }
    }

    private suspend fun compensateFailedOwnership(id: AppWorkoutSessionId) {
        val initial = latestSession?.takeIf { it.id == id } ?: when (
            val surface = loadOpenForRecovery(id)
        ) {
            is RepositoryResult.Success -> surface.value
            RepositoryResult.NotFound -> {
                cleanupAndStop()
                return
            }
            RepositoryResult.Conflict,
            RepositoryResult.StorageUnavailable,
            RepositoryResult.UnexpectedState,
            -> {
                if (!isForeground) {
                    requestProcessScopedTeardown(id)
                    cleanupAndStop()
                }
                return
            }
        }

        when (val reconciliation = reconcileInterruptedOwnership(initial)) {
            is OwnershipReconciliation.RecoveryConfirmed,
            OwnershipReconciliation.NoOpenSession,
            -> cleanupAndStop()
            is OwnershipReconciliation.OpenSurfaceConfirmed -> {
                val session = reconciliation.session
                if (isForeground && session.state in RUNNING_SESSION_STATES) {
                    latestSession = session
                } else {
                    // loadOpenForRecovery confirma superfície persistida que
                    // MainActivity oferece explicitamente no próximo app open.
                    cleanupAndStop()
                }
            }
            OwnershipReconciliation.Unconfirmed -> if (!isForeground) {
                requestProcessScopedTeardown(id)
                cleanupAndStop()
            }
        }
    }

    private suspend fun reconcileInterruptedOwnership(
        initial: AppWorkoutSession,
    ): OwnershipReconciliation {
        var candidate = initial
        var lastSurface: OwnershipReconciliation = OwnershipReconciliation.Unconfirmed
        repeat(MAX_OWNERSHIP_RECONCILIATION_ATTEMPTS) {
            if (candidate.state !in RUNNING_SESSION_STATES) {
                return confirmOpenRecoverySurface(candidate.id)
            }

            when (val interrupt = interruptForRecovery(candidate)) {
                is RepositoryResult.Success -> {
                    if (interrupt.value.state == AppWorkoutSessionState.AWAITING_RECOVERY) {
                        return OwnershipReconciliation.RecoveryConfirmed(interrupt.value)
                    }
                }
                RepositoryResult.Conflict,
                RepositoryResult.NotFound,
                RepositoryResult.StorageUnavailable,
                RepositoryResult.UnexpectedState,
                -> Unit
            }

            when (val surface = confirmOpenRecoverySurface(candidate.id)) {
                is OwnershipReconciliation.RecoveryConfirmed -> return surface
                is OwnershipReconciliation.OpenSurfaceConfirmed -> {
                    lastSurface = surface
                    candidate = surface.session
                    if (candidate.state !in RUNNING_SESSION_STATES) return surface
                }
                OwnershipReconciliation.NoOpenSession -> return surface
                OwnershipReconciliation.Unconfirmed -> return surface
            }
        }
        return lastSurface
    }

    private suspend fun interruptForRecovery(
        session: AppWorkoutSession,
    ): RepositoryResult<AppWorkoutSession> = try {
        sessionRepository.interrupt(timedMutation(session, compensationSnapshot(session)))
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        RepositoryResult.StorageUnavailable
    }

    private suspend fun confirmOpenRecoverySurface(
        id: AppWorkoutSessionId,
    ): OwnershipReconciliation = when (val surface = loadOpenForRecovery(id)) {
        is RepositoryResult.Success -> when (surface.value.state) {
            AppWorkoutSessionState.AWAITING_RECOVERY ->
                OwnershipReconciliation.RecoveryConfirmed(surface.value)
            AppWorkoutSessionState.CREATED,
            AppWorkoutSessionState.ACTIVE,
            AppWorkoutSessionState.PAUSED,
            AppWorkoutSessionState.FINISHING,
            -> OwnershipReconciliation.OpenSurfaceConfirmed(surface.value)
            AppWorkoutSessionState.FINISHED,
            AppWorkoutSessionState.DISCARDED,
            -> OwnershipReconciliation.Unconfirmed
        }
        RepositoryResult.NotFound -> OwnershipReconciliation.NoOpenSession
        RepositoryResult.Conflict,
        RepositoryResult.StorageUnavailable,
        RepositoryResult.UnexpectedState,
        -> OwnershipReconciliation.Unconfirmed
    }

    private suspend fun loadOpenForRecovery(
        id: AppWorkoutSessionId,
    ): RepositoryResult<AppWorkoutSession> = try {
        sessionRepository.loadOpenForRecovery(id)
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        RepositoryResult.StorageUnavailable
    }

    private fun compensationSnapshot(session: AppWorkoutSession): DurationSnapshot {
        val runtimeSnapshot = latestSession
            ?.takeIf { it.id == session.id && it.state == session.state }
            ?.let { timer.snapshot() }
        val capturedAtNanos = runtimeSnapshot?.capturedAtNanos ?: SystemClock.elapsedRealtimeNanos()
        return when (session.state) {
            AppWorkoutSessionState.ACTIVE -> DurationSnapshot(
                activeDuration = maxOf(
                    session.activeDuration,
                    runtimeSnapshot?.activeDuration ?: session.activeDuration,
                ),
                pausedDuration = session.pausedDuration,
                capturedAtNanos = capturedAtNanos,
            )
            AppWorkoutSessionState.PAUSED -> DurationSnapshot(
                activeDuration = session.activeDuration,
                pausedDuration = maxOf(
                    session.pausedDuration,
                    runtimeSnapshot?.pausedDuration ?: session.pausedDuration,
                ),
                capturedAtNanos = capturedAtNanos,
            )
            else -> DurationSnapshot(
                activeDuration = session.activeDuration,
                pausedDuration = session.pausedDuration,
                capturedAtNanos = capturedAtNanos,
            )
        }
    }

    private fun timedMutation(
        session: AppWorkoutSession,
        snapshot: DurationSnapshot,
    ) = AppWorkoutSessionMutation(
        sessionId = session.id,
        expectedRevision = session.revision,
        at = auditInstant(session, snapshot),
        activeDuration = snapshot.activeDuration,
        pausedDuration = snapshot.pausedDuration,
    )

    private fun auditInstant(
        session: AppWorkoutSession,
        snapshot: DurationSnapshot,
    ): Instant {
        return session.nextAuditInstant(
            now = Instant.now(),
            nextActiveDuration = snapshot.activeDuration,
            nextPausedDuration = snapshot.pausedDuration,
        )
    }

    private fun notificationManager(): NotificationManager? = ContextCompat.getSystemService(
        this,
        NotificationManager::class.java,
    )

    private fun requestProcessScopedTeardown(id: AppWorkoutSessionId? = null) {
        if (!teardownRequested.compareAndSet(false, true)) return
        ownershipCoordinator.releaseAfterServiceTeardown(
            token = ownerToken,
            lastKnownSession = latestSession,
            pendingSessionId = id ?: pendingSessionId ?: sessionId,
        )
    }

    private fun cleanupAndStop() {
        if (isStopping) return
        isStopping = true
        ownershipCoordinator.release(ownerToken)
        checkpointJob?.cancel()
        checkpointJob = null
        sessionObserverJob?.cancel()
        sessionObserverJob = null
        notificationUpdateJob?.cancel()
        notificationUpdateJob = null
        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }
        stopSelf()
    }

    private suspend fun runSerializedCommand(command: suspend () -> Unit) {
        commandMutex.withLock {
            ownershipCoordinator.withSerializedServiceOperation(command)
        }
    }

}
