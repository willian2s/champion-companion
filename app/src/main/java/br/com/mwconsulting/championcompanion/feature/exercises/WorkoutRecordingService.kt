package br.com.mwconsulting.championcompanion.feature.exercises

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationSource
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationSourceFactory
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationStartResult
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSession
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionCheckpoint
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionId
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionMutation
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionRepository
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionState
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.notification.WorkoutNotificationBuilder
import br.com.mwconsulting.championcompanion.core.notification.WorkoutNotificationChannel
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
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

/** Monotonic timer used only for live rendering between persisted checkpoints. */
private class WorkoutTimer {
    private var baseActiveNanos: Long = 0L
    private var basePausedNanos: Long = 0L
    private var activeSinceNanos: Long? = null
    private var pausedSinceNanos: Long? = null

    fun setBase(activeDuration: Duration, pausedDuration: Duration) {
        baseActiveNanos = activeDuration.toNanos()
        basePausedNanos = pausedDuration.toNanos()
    }

    fun markActive(nowNanos: Long = System.nanoTime()) {
        activeSinceNanos = nowNanos
        pausedSinceNanos = null
    }

    fun markPaused(nowNanos: Long = System.nanoTime()) {
        activeSinceNanos = null
        pausedSinceNanos = nowNanos
    }

    fun markStopped() {
        activeSinceNanos = null
        pausedSinceNanos = null
    }

    fun snapshot(): DurationSnapshot {
        val now = System.nanoTime()
        val active = baseActiveNanos + (activeSinceNanos?.let { now - it } ?: 0L)
        val paused = basePausedNanos + (pausedSinceNanos?.let { now - it } ?: 0L)
        return DurationSnapshot(
            activeDurationMillis = active / 1_000_000,
            pausedDurationMillis = paused / 1_000_000,
        )
    }
}

private data class DurationSnapshot(
    val activeDurationMillis: Long,
    val pausedDurationMillis: Long,
)

@AndroidEntryPoint
class WorkoutRecordingService : Service() {
    @Inject lateinit var sessionRepository: AppWorkoutSessionRepository
    @Inject lateinit var locationSourceFactory: WorkoutLocationSourceFactory
    @Inject lateinit var notificationBuilder: WorkoutNotificationBuilder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val commandMutex = Mutex()
    private val timer = WorkoutTimer()

    private var sessionId: AppWorkoutSessionId? = null
    private var latestSession: AppWorkoutSession? = null
    private var appliedRevision: Long? = null
    private var checkpointJob: Job? = null
    private var sessionObserverJob: Job? = null
    private var notificationUpdateJob: Job? = null
    private var locationSource: WorkoutLocationSource? = null
    private var locationRuntimeGeneration: Long? = null
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

        @Volatile
        var isAlive = false
            private set

        fun startIntent(context: android.content.Context, sessionId: String): Intent =
            Intent(context, WorkoutRecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
    }

    override fun onCreate() {
        super.onCreate()
        isAlive = true
        WorkoutNotificationChannel.create(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.sessionIdOrNull()
        when (intent?.action) {
            ACTION_START -> id?.let(::handleStart)
            ACTION_PAUSE -> id?.let(::handlePause)
            ACTION_RESUME -> id?.let(::handleResume)
            ACTION_FINISH -> id?.let(::handleFinish)
            else -> if (!isForeground) stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        scope.launch { interruptAndStop() }
    }

    override fun onDestroy() {
        isAlive = false
        checkpointJob?.cancel()
        sessionObserverJob?.cancel()
        notificationUpdateJob?.cancel()
        stopWorkoutLocation()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun Intent.sessionIdOrNull(): AppWorkoutSessionId? {
        val rawId = getStringExtra(EXTRA_SESSION_ID) ?: return null
        return runCatching { AppWorkoutSessionId(UUID.fromString(rawId)) }.getOrNull()
    }

    private fun handleStart(id: AppWorkoutSessionId) {
        scope.launch {
            commandMutex.withLock {
                ensureAttached(id, allowCreatedStart = true)
            }
        }
    }

    private fun handlePause(id: AppWorkoutSessionId) {
        scope.launch {
            commandMutex.withLock {
                val session = ensureAttached(id, allowCreatedStart = true) ?: return@withLock
                when (session.state) {
                    AppWorkoutSessionState.PAUSED -> Unit
                    AppWorkoutSessionState.ACTIVE -> {
                        val result = sessionRepository.pause(
                            AppWorkoutSessionMutation(
                                sessionId = id,
                                expectedRevision = session.revision,
                                at = Instant.now(),
                            ),
                        )
                        applyMutationResult(id, result)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun handleResume(id: AppWorkoutSessionId) {
        scope.launch {
            commandMutex.withLock {
                val session = ensureAttached(id, allowCreatedStart = true) ?: return@withLock
                when (session.state) {
                    AppWorkoutSessionState.ACTIVE -> Unit
                    AppWorkoutSessionState.PAUSED -> {
                        val result = sessionRepository.resume(
                            AppWorkoutSessionMutation(
                                sessionId = id,
                                expectedRevision = session.revision,
                                at = Instant.now(),
                            ),
                        )
                        applyMutationResult(id, result)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun handleFinish(id: AppWorkoutSessionId) {
        scope.launch {
            commandMutex.withLock {
                val attached = ensureAttached(id, allowCreatedStart = true) ?: return@withLock
                if (attached.state == AppWorkoutSessionState.FINISHING) {
                    cleanupAndStop()
                    return@withLock
                }
                if (attached.state != AppWorkoutSessionState.ACTIVE &&
                    attached.state != AppWorkoutSessionState.PAUSED
                ) {
                    return@withLock
                }

                val checkpointAt = Instant.now()
                val checkpointed = when (
                    val checkpointResult = sessionRepository.checkpoint(
                        AppWorkoutSessionCheckpoint(
                            sessionId = id,
                            expectedRevision = attached.revision,
                            runtimeGeneration = attached.runtimeGeneration,
                            at = checkpointAt,
                            aggregates = attached.aggregates,
                        ),
                    )
                ) {
                    is RepositoryResult.Success -> checkpointResult.value.also(::applySession)
                    RepositoryResult.Conflict -> reloadSession(id)?.also(::applySession)
                    else -> null
                } ?: return@withLock

                if (checkpointed.state == AppWorkoutSessionState.FINISHING) {
                    cleanupAndStop()
                    return@withLock
                }
                if (checkpointed.state != AppWorkoutSessionState.ACTIVE &&
                    checkpointed.state != AppWorkoutSessionState.PAUSED
                ) {
                    return@withLock
                }

                when (
                    val finishResult = sessionRepository.beginFinishing(
                        AppWorkoutSessionMutation(
                            sessionId = id,
                            expectedRevision = checkpointed.revision,
                            at = Instant.now(),
                        ),
                    )
                ) {
                    is RepositoryResult.Success -> {
                        applySession(finishResult.value)
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
    }

    private suspend fun ensureAttached(
        id: AppWorkoutSessionId,
        allowCreatedStart: Boolean,
    ): AppWorkoutSession? {
        latestSession?.takeIf { it.id == id && isForeground }?.let { return it }

        var session = reloadSession(id) ?: run {
            cleanupAndStop()
            return null
        }

        if (session.state == AppWorkoutSessionState.CREATED) {
            if (!allowCreatedStart) return null
            session = when (
                val result = sessionRepository.start(
                    AppWorkoutSessionMutation(
                        sessionId = id,
                        expectedRevision = session.revision,
                        at = Instant.now(),
                    ),
                )
            ) {
                is RepositoryResult.Success -> result.value
                RepositoryResult.Conflict -> reloadSession(id) ?: return null
                else -> {
                    cleanupAndStop()
                    return null
                }
            }
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
                if (session == null) {
                    cleanupAndStop()
                    return@collect
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

    private fun startCheckpointJob(id: AppWorkoutSessionId) {
        checkpointJob = scope.launch {
            while (true) {
                delay(CHECKPOINT_INTERVAL_MILLIS)
                commandMutex.withLock {
                    val session = latestSession ?: return@withLock
                    if (session.id != id ||
                        (session.state != AppWorkoutSessionState.ACTIVE &&
                            session.state != AppWorkoutSessionState.PAUSED)
                    ) {
                        return@withLock
                    }

                    when (
                        val result = sessionRepository.checkpoint(
                            AppWorkoutSessionCheckpoint(
                                sessionId = id,
                                expectedRevision = session.revision,
                                runtimeGeneration = session.runtimeGeneration,
                                at = Instant.now(),
                                aggregates = session.aggregates,
                            ),
                        )
                    ) {
                        is RepositoryResult.Success -> applySession(result.value)
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
                    overrideActiveDuration = Duration.ofMillis(snapshot.activeDurationMillis),
                    overridePausedDuration = Duration.ofMillis(snapshot.pausedDurationMillis),
                )
                notificationManager()?.notify(WorkoutNotificationChannel.NOTIFICATION_ID, notification)
            }
        }
    }

    private fun applyMutationResult(
        id: AppWorkoutSessionId,
        result: RepositoryResult<AppWorkoutSession>,
    ) {
        when (result) {
            is RepositoryResult.Success -> applySession(result.value)
            RepositoryResult.Conflict -> scope.launch {
                commandMutex.withLock {
                    reloadSession(id)?.let(::applySession)
                }
            }
            else -> Unit
        }
    }

    private fun applySession(session: AppWorkoutSession) {
        if (sessionId != null && sessionId != session.id) return
        sessionId = session.id

        val shouldResetRuntime = appliedRevision != session.revision
        latestSession = session
        if (shouldResetRuntime) {
            appliedRevision = session.revision
            timer.setBase(session.activeDuration, session.pausedDuration)
            when (session.state) {
                AppWorkoutSessionState.ACTIVE -> timer.markActive()
                AppWorkoutSessionState.PAUSED -> timer.markPaused()
                else -> timer.markStopped()
            }
        }

        promoteOrUpdateForeground(session)
        if (session.state == AppWorkoutSessionState.ACTIVE && session.recordsRoute) {
            startWorkoutLocation(session.runtimeGeneration)
        } else {
            stopWorkoutLocation()
        }
    }

    private fun promoteOrUpdateForeground(session: AppWorkoutSession) {
        val notification = notificationBuilder.buildNotification(session)
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
        isForeground = true
    }

    private fun startWorkoutLocation(runtimeGeneration: Long) {
        if (locationSource != null && locationRuntimeGeneration == runtimeGeneration) return
        stopWorkoutLocation()

        val source = locationSourceFactory.create()
        locationRuntimeGeneration = runtimeGeneration
        locationSource = source
        if (source.start(runtimeGeneration) != WorkoutLocationStartResult.STARTED) {
            source.close()
            locationSource = null
            locationRuntimeGeneration = null
        }
    }

    private fun stopWorkoutLocation() {
        val source = locationSource ?: return
        source.stop(locationRuntimeGeneration ?: 0L)
        source.close()
        locationSource = null
        locationRuntimeGeneration = null
    }

    private suspend fun reloadSession(id: AppWorkoutSessionId): AppWorkoutSession? =
        sessionRepository.observeSession(id).first()

    private suspend fun interruptAndStop() {
        commandMutex.withLock {
            val id = sessionId
            val session = latestSession ?: id?.let { reloadSession(it) }
            if (id != null && session != null &&
                (session.state == AppWorkoutSessionState.ACTIVE ||
                    session.state == AppWorkoutSessionState.PAUSED)
            ) {
                when (
                    val result = sessionRepository.interrupt(
                        AppWorkoutSessionMutation(
                            sessionId = id,
                            expectedRevision = session.revision,
                            at = Instant.now(),
                        ),
                    )
                ) {
                    is RepositoryResult.Success -> latestSession = result.value
                    RepositoryResult.Conflict -> latestSession = reloadSession(id)
                    else -> Unit
                }
            }
        }
        cleanupAndStop()
    }

    private fun notificationManager(): NotificationManager? = ContextCompat.getSystemService(
        this,
        NotificationManager::class.java,
    )

    private fun cleanupAndStop() {
        if (isStopping) return
        isStopping = true
        checkpointJob?.cancel()
        checkpointJob = null
        sessionObserverJob?.cancel()
        sessionObserverJob = null
        notificationUpdateJob?.cancel()
        notificationUpdateJob = null
        stopWorkoutLocation()
        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }
        stopSelf()
    }
}
