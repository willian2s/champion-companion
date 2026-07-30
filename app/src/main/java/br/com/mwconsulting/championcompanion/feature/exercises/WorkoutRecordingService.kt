package br.com.mwconsulting.championcompanion.feature.exercises

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationSource
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationSourceFactory
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSession
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionAggregates
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

/**
 * Monotonic timer that tracks elapsed active/paused duration.
 * Provides real-time duration between checkpoints for live UI/notification.
 */
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
        val since = activeSinceNanos
        if (since != null) {
            baseActiveNanos += nowNanos - since
            activeSinceNanos = null
        }
        pausedSinceNanos = nowNanos
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
    private val timer = WorkoutTimer()
    private var sessionId: AppWorkoutSessionId? = null
    private var latestSession: AppWorkoutSession? = null
    private var timerJob: Job? = null
    private var checkpointJob: Job? = null
    private var sessionObserverJob: Job? = null
    private var notificationUpdateJob: Job? = null
    private var locationSource: WorkoutLocationSource? = null
    private var locationRuntimeGeneration = 0L

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
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_FINISH -> handleFinish()
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        handleInterrupt()
        stopSelf()
    }

    override fun onDestroy() {
        isAlive = false
        timerJob?.cancel()
        checkpointJob?.cancel()
        sessionObserverJob?.cancel()
        notificationUpdateJob?.cancel()
        locationSource?.stop(locationRuntimeGeneration)
        locationSource?.close()
        locationSource = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleStart(intent: Intent) {
        val rawId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        val uuid = try { UUID.fromString(rawId) } catch (_: IllegalArgumentException) { return }
        val id = AppWorkoutSessionId(uuid)
        sessionId = id

        scope.launch {
            val mutation = AppWorkoutSessionMutation(
                sessionId = id,
                expectedRevision = 0,
                at = Instant.now(),
            )
            when (val result = sessionRepository.start(mutation)) {
                is RepositoryResult.Success -> {
                    val session = result.value
                    latestSession = session
                    timer.setBase(session.activeDuration, session.pausedDuration)
                    timer.markActive()

                    val notification = notificationBuilder.buildNotification(session)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(
                            WorkoutNotificationChannel.NOTIFICATION_ID,
                            notification,
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
                        )
                    } else {
                        startForeground(
                            WorkoutNotificationChannel.NOTIFICATION_ID,
                            notification,
                        )
                    }

                    startSessionTracking(id)
                    startCheckpointJob(id)
                    startNotificationUpdates()
                }
                else -> {
                    stopSelf()
                }
            }
        }
    }

    private suspend fun startSessionTracking(id: AppWorkoutSessionId) {
        sessionObserverJob = scope.launch {
            sessionRepository.observeSession(id).collect { session ->
                latestSession = session
                if (session == null || session.state == AppWorkoutSessionState.FINISHED ||
                    session.state == AppWorkoutSessionState.DISCARDED
                ) {
                    cleanupAndStop()
                    return@collect
                }
                if (session.state == AppWorkoutSessionState.ACTIVE && session.recordsRoute &&
                    locationSource == null
                ) {
                    startWorkoutLocation()
                }
            }
        }
    }

    private fun startCheckpointJob(id: AppWorkoutSessionId) {
        checkpointJob = scope.launch {
            while (true) {
                delay(CHECKPOINT_INTERVAL_MILLIS)
                val session = latestSession ?: continue
                if (session.state != AppWorkoutSessionState.ACTIVE &&
                    session.state != AppWorkoutSessionState.PAUSED
                ) continue

                val aggregates = session.aggregates
                val mutation = AppWorkoutSessionCheckpoint(
                    sessionId = id,
                    expectedRevision = session.revision,
                    runtimeGeneration = session.runtimeGeneration,
                    at = Instant.now(),
                    aggregates = aggregates,
                )
                sessionRepository.checkpoint(mutation)
            }
        }
    }

    private fun startNotificationUpdates() {
        notificationUpdateJob = scope.launch {
            while (true) {
                delay(NOTIFICATION_UPDATE_INTERVAL_MILLIS)
                val session = latestSession ?: continue
                val snap = timer.snapshot()
                val notification = notificationBuilder.buildNotification(
                    session = session,
                    overrideActiveDuration = Duration.ofMillis(snap.activeDurationMillis),
                    overridePausedDuration = Duration.ofMillis(snap.pausedDurationMillis),
                )
                val manager = ContextCompat.getSystemService(
                    this@WorkoutRecordingService,
                    NotificationManager::class.java,
                ) ?: continue
                manager.notify(WorkoutNotificationChannel.NOTIFICATION_ID, notification)
            }
        }
    }

    private fun handlePause() {
        val id = sessionId ?: return
        val session = latestSession ?: return
        timer.markPaused()
        locationSource?.stop(locationRuntimeGeneration)
        locationSource?.close()
        locationSource = null
        scope.launch {
            val mutation = AppWorkoutSessionMutation(
                sessionId = id,
                expectedRevision = session.revision,
                at = Instant.now(),
            )
            sessionRepository.pause(mutation)
        }
    }

    private fun handleResume() {
        val id = sessionId ?: return
        val session = latestSession ?: return
        timer.markActive()
        scope.launch {
            val mutation = AppWorkoutSessionMutation(
                sessionId = id,
                expectedRevision = session.revision,
                at = Instant.now(),
            )
            sessionRepository.resume(mutation)
            val refreshed = sessionRepository.observeSession(id).first { it != null }
            if (refreshed?.recordsRoute == true) {
                startWorkoutLocation()
            }
        }
    }

    private fun handleFinish() {
        val id = sessionId ?: return
        val session = latestSession ?: return
        scope.launch {
            val checkpointMutation = AppWorkoutSessionCheckpoint(
                sessionId = id,
                expectedRevision = session.revision,
                runtimeGeneration = session.runtimeGeneration,
                at = Instant.now(),
                aggregates = session.aggregates,
            )
            when (val checkpointResult = sessionRepository.checkpoint(checkpointMutation)) {
                is RepositoryResult.Success -> {
                    val updated = checkpointResult.value
                    latestSession = updated
                    val finishMutation = AppWorkoutSessionMutation(
                        sessionId = id,
                        expectedRevision = updated.revision,
                        at = Instant.now(),
                    )
                    when (sessionRepository.beginFinishing(finishMutation)) {
                        is RepositoryResult.Success -> {
                            val notification = notificationBuilder.buildNotification(
                                session = updated,
                                overrideState = AppWorkoutSessionState.FINISHING,
                            )
                            val manager = ContextCompat.getSystemService(
                                this@WorkoutRecordingService,
                                NotificationManager::class.java,
                            ) ?: return@launch
                            manager.notify(WorkoutNotificationChannel.NOTIFICATION_ID, notification)
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        }
                        else -> stopSelf()
                    }
                }
                else -> stopSelf()
            }
        }
    }

    private fun handleInterrupt() {
        val id = sessionId ?: return
        val session = latestSession ?: return
        locationSource?.stop(locationRuntimeGeneration)
        locationSource?.close()
        locationSource = null
        scope.launch {
            val mutation = AppWorkoutSessionMutation(
                sessionId = id,
                expectedRevision = session.revision,
                at = Instant.now(),
            )
            sessionRepository.interrupt(mutation)
        }
    }

    private fun startWorkoutLocation() {
        if (locationSource != null) return
        val source = locationSourceFactory.create()
        locationRuntimeGeneration++
        locationSource = source
        if (source.start(locationRuntimeGeneration) !=
            br.com.mwconsulting.championcompanion.core.location.WorkoutLocationStartResult.STARTED
        ) {
            source.close()
            locationSource = null
        }
    }

    private fun cleanupAndStop() {
        timerJob?.cancel()
        timerJob = null
        checkpointJob?.cancel()
        checkpointJob = null
        notificationUpdateJob?.cancel()
        notificationUpdateJob = null
        locationSource?.stop(locationRuntimeGeneration)
        locationSource?.close()
        locationSource = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

}
