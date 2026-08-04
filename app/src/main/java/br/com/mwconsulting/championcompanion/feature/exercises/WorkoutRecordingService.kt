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
import br.com.mwconsulting.championcompanion.core.model.DomainValue
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoute
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteId
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePoint
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointId
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteSegmentReason
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteMetrics
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteRepository
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteRecoverySnapshot
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationEvent
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationLogger
import br.com.mwconsulting.championcompanion.core.location.WorkoutRouteAccumulator
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationSource
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationSourceFactory
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationStartResult
import br.com.mwconsulting.championcompanion.core.logging.StructuredLogger
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import br.com.mwconsulting.championcompanion.core.model.valueOrNull
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
    @Inject lateinit var routeRepository: WorkoutRouteRepository
    @Inject lateinit var workoutLocationSourceFactory: WorkoutLocationSourceFactory
    @Inject lateinit var structuredLogger: StructuredLogger
    @Inject lateinit var liveMetricsStore: WorkoutLiveMetricsStore
    @Inject lateinit var notificationBuilder: WorkoutNotificationBuilder
    @Inject lateinit var ownershipCoordinator: WorkoutSessionOwnershipCoordinator
    @Inject lateinit var watchWorkoutParticipationCoordinator: WatchWorkoutParticipationCoordinator

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val commandMutex = Mutex()
    private val timer = WorkoutTimer()
    private val ownerToken = Any()
    private val teardownRequested = AtomicBoolean(false)
    private val routeIds = UuidV7Generator()
    private val routePolicyLogger by lazy { WorkoutLocationLogger(structuredLogger) }

    @Volatile
    private var pendingSessionId: AppWorkoutSessionId? = null
    private var sessionId: AppWorkoutSessionId? = null
    private var latestSession: AppWorkoutSession? = null
    private var appliedRevision: Long? = null
    private var checkpointJob: Job? = null
    private var sessionObserverJob: Job? = null
    private var watchParticipationJob: Job? = null
    private var notificationUpdateJob: Job? = null
    private var locationSource: WorkoutLocationSource? = null
    private var locationEventsJob: Job? = null
    private var routeFlushJob: Job? = null
    private var routeAccumulator: WorkoutRouteAccumulator? = null
    private var confirmedRouteMetrics: WorkoutRouteMetrics? = null
    private var activeLocationRuntimeGeneration: Long? = null
    private var currentGpsStatus = WorkoutGpsCaptureStatus.STARTING
    private var routeCaptureError: String? = null
    private var currentSpeedMetersPerSecond: Double? = null
    private var isForeground = false
    private var isStopping = false

    companion object {
        const val ACTION_START = "br.com.mwconsulting.championcompanion.action.START_SESSION"
        const val ACTION_PAUSE = "br.com.mwconsulting.championcompanion.action.PAUSE_SESSION"
        const val ACTION_RESUME = "br.com.mwconsulting.championcompanion.action.RESUME_SESSION"
        const val ACTION_RETRY_ROUTE = "br.com.mwconsulting.championcompanion.action.RETRY_ROUTE"
        const val ACTION_ENABLE_WATCH = "br.com.mwconsulting.championcompanion.action.ENABLE_WATCH"
        const val ACTION_RETRY_WATCH = "br.com.mwconsulting.championcompanion.action.RETRY_WATCH"
        const val ACTION_FINISH = "br.com.mwconsulting.championcompanion.action.FINISH_SESSION"
        const val EXTRA_SESSION_ID = "session_id"
        const val CHECKPOINT_INTERVAL_MILLIS = 30_000L
        const val NOTIFICATION_UPDATE_INTERVAL_MILLIS = 2_000L
        const val ROUTE_FLUSH_INTERVAL_MILLIS = 5_000L
        // A captura ativa confirma cada ponto antes de expô-lo como métrica
        // aceita. Isso reduz a janela de perda em morte do processo sem criar
        // staging paralelo fora do contrato persistente da rota.
        const val ROUTE_BATCH_POINT_LIMIT = 1
        private const val ROUTE_CAPTURE_ERROR =
            "Não foi possível salvar a rota. A captura foi pausada; tente novamente."
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
            ACTION_RETRY_ROUTE -> id?.let(::handleRetryRoute)
            ACTION_ENABLE_WATCH,
            ACTION_RETRY_WATCH,
            -> id?.let(::handleWatchParticipation)
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
        stopLocationSource()
        checkpointJob?.cancel()
        sessionObserverJob?.cancel()
        watchParticipationJob?.cancel()
        notificationUpdateJob?.cancel()
        routeFlushJob?.cancel()
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
                    stopLocationSource(WorkoutGpsCaptureStatus.PAUSED)
                    if (!flushRouteBatch()) return@launchCommand
                    val snapshot = timer.snapshot()
                    val result = sessionRepository.pause(
                        timedMutation(session, snapshot),
                    )
                    when (result) {
                        is RepositoryResult.Success -> {
                            routeAccumulator = routeAccumulator?.requestNewSegment(
                                WorkoutRouteSegmentReason.RESUMED_AFTER_PAUSE,
                            )
                            applySession(result.value, snapshot.capturedAtNanos)
                            publishLiveMetrics(result.value)
                        }
                        RepositoryResult.Conflict -> reloadSession(id)?.let {
                            applySession(it)
                            if (it.state == AppWorkoutSessionState.ACTIVE) {
                                synchronizeRouteCapture(
                                    it,
                                    restartReason = WorkoutRouteSegmentReason.GPS_RECOVERY,
                                )
                            } else {
                                publishLiveMetrics(it)
                            }
                        }
                        else -> latestSession?.takeIf {
                            it.id == id && it.state == AppWorkoutSessionState.ACTIVE
                        }?.let {
                            synchronizeRouteCapture(
                                it,
                                restartReason = WorkoutRouteSegmentReason.GPS_RECOVERY,
                            )
                        }
                    }
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
                    when (result) {
                        is RepositoryResult.Success -> {
                            applySession(result.value, snapshot.capturedAtNanos)
                            synchronizeRouteCapture(
                                result.value,
                                restartReason = WorkoutRouteSegmentReason.RESUMED_AFTER_PAUSE,
                            )
                        }
                        RepositoryResult.Conflict -> reloadSession(id)?.let {
                            applySession(it)
                            if (it.state == AppWorkoutSessionState.ACTIVE) {
                                synchronizeRouteCapture(
                                    it,
                                    restartReason = WorkoutRouteSegmentReason.RESUMED_AFTER_PAUSE,
                                )
                            }
                        }
                        else -> Unit
                    }
                }
                else -> Unit
            }
        }
    }

    private fun handleRetryRoute(id: AppWorkoutSessionId) {
        launchCommand(id) {
            val session = ensureAttached(id) ?: return@launchCommand
            if (!session.recordsRoute || session.state != AppWorkoutSessionState.ACTIVE) return@launchCommand
            routeCaptureError = null
            currentGpsStatus = WorkoutGpsCaptureStatus.STARTING
            stopLocationSource(WorkoutGpsCaptureStatus.STARTING)
            if (!flushRouteBatch()) return@launchCommand
            synchronizeRouteCapture(
                session,
                restartReason = WorkoutRouteSegmentReason.GPS_RECOVERY,
            )
        }
    }

    private fun handleWatchParticipation(id: AppWorkoutSessionId) {
        launchCommand(id) {
            val session = ensureAttached(id) ?: return@launchCommand
            watchWorkoutParticipationCoordinator.bindSession(id)
            watchWorkoutParticipationCoordinator.enable(id)
            latestSession?.takeIf { it.id == session.id }?.let(::promoteOrUpdateForeground)
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

            stopLocationSource(WorkoutGpsCaptureStatus.PAUSED)
            if (!flushRouteBatch()) return@launchCommand
            val checkpointSnapshot = timer.snapshot()
            val checkpointed = when (
                val checkpointResult = sessionRepository.checkpoint(
                    AppWorkoutSessionCheckpoint(
                        sessionId = id,
                        expectedRevision = attached.revision,
                        runtimeGeneration = attached.runtimeGeneration,
                        at = auditInstant(attached, checkpointSnapshot),
                        aggregates = aggregatesWithRouteDistance(attached),
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
        if (session.state == AppWorkoutSessionState.AWAITING_RECOVERY) {
            watchWorkoutParticipationCoordinator.releaseAtRecovery(id)
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
        if (session.state == AppWorkoutSessionState.ACTIVE) {
            synchronizeRouteCapture(
                session,
                restartReason = WorkoutRouteSegmentReason.PROCESS_RECOVERY,
            )
        }
        return session
    }

    private fun attachSession(session: AppWorkoutSession) {
        if (sessionId != null && sessionId != session.id) return
        sessionId = session.id
        watchWorkoutParticipationCoordinator.bindSession(session.id)
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
        if (watchParticipationJob?.isActive != true) {
            startWatchParticipationObserver(session.id)
        }
    }

    private fun startWatchParticipationObserver(id: AppWorkoutSessionId) {
        watchParticipationJob = scope.launch {
            watchWorkoutParticipationCoordinator.state.collect { participation ->
                if (participation.sessionId != id) return@collect
                runSerializedCommand {
                    val session = latestSession?.takeIf { it.id == id } ?: return@runSerializedCommand
                    if (!isForeground || isStopping) return@runSerializedCommand
                    promoteOrUpdateForeground(session)
                }
            }
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
                        AppWorkoutSessionState.ACTIVE -> {
                            applySession(session)
                            synchronizeRouteCapture(session)
                        }
                        AppWorkoutSessionState.PAUSED -> {
                            stopLocationSource(WorkoutGpsCaptureStatus.PAUSED)
                            applySession(session)
                            publishLiveMetrics(session)
                        }
                        AppWorkoutSessionState.FINISHING -> {
                            stopLocationSource(WorkoutGpsCaptureStatus.PAUSED)
                            applySession(session)
                            cleanupAndStop()
                        }
                        AppWorkoutSessionState.AWAITING_RECOVERY -> {
                            watchWorkoutParticipationCoordinator.releaseAtRecovery(id)
                            cleanupAndStop()
                        }
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

                    if (session.state == AppWorkoutSessionState.ACTIVE && !flushRouteBatch()) {
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
                                aggregates = aggregatesWithRouteDistance(session),
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
                    overrideGpsStatus = notificationGpsStatus(session),
                )
                notificationManager()?.notify(WorkoutNotificationChannel.NOTIFICATION_ID, notification)
            }
        }
    }

    private suspend fun synchronizeRouteCapture(
        session: AppWorkoutSession,
        restartReason: WorkoutRouteSegmentReason? = null,
    ) {
        if (!session.recordsRoute) {
            stopLocationSource()
            liveMetricsStore.clear(session.id)
            return
        }
        if (session.state != AppWorkoutSessionState.ACTIVE) {
            stopLocationSource(WorkoutGpsCaptureStatus.PAUSED)
            publishLiveMetrics(session)
            return
        }
        if (routeCaptureError != null) {
            publishLiveMetrics(session)
            return
        }
        if (locationSource != null &&
            activeLocationRuntimeGeneration == session.runtimeGeneration &&
            routeAccumulator?.sessionId == session.id
        ) {
            publishLiveMetrics(session)
            return
        }

        if (locationSource != null) stopLocationSource(WorkoutGpsCaptureStatus.STARTING)
        val current = routeAccumulator
        val nextAccumulator = when {
            current?.sessionId == session.id && current.runtimeGeneration == session.runtimeGeneration -> {
                restartReason?.let(current::requestNewSegment) ?: current
            }
            else -> loadOrCreateRouteAccumulator(session, restartReason)
        }
        if (nextAccumulator != null &&
            (current == null || current.routeId != nextAccumulator.routeId ||
                current.expectedRevision != nextAccumulator.expectedRevision)
        ) {
            confirmedRouteMetrics = nextAccumulator.metrics
        }
        routeAccumulator = nextAccumulator
        if (routeAccumulator == null) return

        startLocationSource(session)
        publishLiveMetrics(session)
        if (routeAccumulator?.hasPendingBatch == true) scheduleRouteFlush()
    }

    private suspend fun loadOrCreateRouteAccumulator(
        session: AppWorkoutSession,
        restartReason: WorkoutRouteSegmentReason?,
    ): WorkoutRouteAccumulator? {
        when (val loaded = loadRouteSnapshot(session.id)) {
            is RepositoryResult.Success -> {
                val snapshot = loaded.value
                if (snapshot != null) {
                    if (snapshot.route.workoutType != session.type ||
                        snapshot.route.state != br.com.mwconsulting.championcompanion.core.model.WorkoutRouteState.CAPTURING
                    ) {
                        failRouteCapture()
                        return null
                    }
                    return runCatching {
                        WorkoutRouteAccumulator.restore(
                            snapshot = snapshot,
                            runtimeGeneration = session.runtimeGeneration,
                            reason = restartReason ?: WorkoutRouteSegmentReason.PROCESS_RECOVERY,
                        )
                    }.getOrElse {
                        failRouteCapture()
                        null
                    }
                }
            }
            RepositoryResult.NotFound,
            RepositoryResult.Conflict,
            RepositoryResult.StorageUnavailable,
            RepositoryResult.UnexpectedState,
            -> {
                failRouteCapture()
                return null
            }
        }

        val startedAt = session.startedAt ?: run {
            failRouteCapture()
            return null
        }
        val route = WorkoutRoute.create(
            id = WorkoutRouteId(routeIds.next()),
            sessionId = session.id,
            workoutType = session.type,
            startedAt = startedAt,
        ).valueOrNull() ?: run {
            failRouteCapture()
            return null
        }
        return when (val created = createRoute(route)) {
            is RepositoryResult.Success -> WorkoutRouteAccumulator.create(
                route = created.value,
                runtimeGeneration = session.runtimeGeneration,
            )
            RepositoryResult.Conflict -> when (val retry = loadRouteSnapshot(session.id)) {
                is RepositoryResult.Success -> retry.value?.let {
                    runCatching {
                        WorkoutRouteAccumulator.restore(
                            snapshot = it,
                            runtimeGeneration = session.runtimeGeneration,
                            reason = restartReason ?: WorkoutRouteSegmentReason.PROCESS_RECOVERY,
                        )
                    }.getOrNull()
                }
                else -> null
            } ?: run {
                failRouteCapture()
                null
            }
            RepositoryResult.NotFound,
            RepositoryResult.StorageUnavailable,
            RepositoryResult.UnexpectedState,
            -> {
                failRouteCapture()
                null
            }
        }
    }

    private suspend fun loadRouteSnapshot(
        sessionId: AppWorkoutSessionId,
    ): RepositoryResult<WorkoutRouteRecoverySnapshot?> = try {
        routeRepository.loadRecoverySnapshot(sessionId)
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        RepositoryResult.StorageUnavailable
    }

    private suspend fun createRoute(route: WorkoutRoute): RepositoryResult<WorkoutRoute> = try {
        routeRepository.create(route)
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        RepositoryResult.StorageUnavailable
    }

    private suspend fun startLocationSource(session: AppWorkoutSession) {
        if (locationSource != null || !session.recordsRoute || session.state != AppWorkoutSessionState.ACTIVE) return
        val source = try {
            workoutLocationSourceFactory.create()
        } catch (_: Exception) {
            currentGpsStatus = WorkoutGpsCaptureStatus.UNAVAILABLE
            publishLiveMetrics(session)
            return
        }
        val generation = session.runtimeGeneration
        locationSource = source
        activeLocationRuntimeGeneration = generation
        currentGpsStatus = WorkoutGpsCaptureStatus.STARTING
        locationEventsJob = scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            source.events.collect { event ->
                if (source !== locationSource || event.runtimeGeneration != activeLocationRuntimeGeneration) return@collect
                runSerializedCommand {
                    val current = latestSession
                    if (current?.id == session.id &&
                        current.state == AppWorkoutSessionState.ACTIVE &&
                        event.runtimeGeneration == current.runtimeGeneration
                    ) {
                        handleLocationEvent(event)
                    }
                }
            }
        }
        val result = try {
            source.start(generation)
        } catch (_: Exception) {
            WorkoutLocationStartResult.SOURCE_CLOSED
        }
        if (result != WorkoutLocationStartResult.STARTED) {
            stopLocationSource(WorkoutGpsCaptureStatus.UNAVAILABLE)
            publishLiveMetrics(session)
        }
    }

    private suspend fun handleLocationEvent(event: WorkoutLocationEvent) {
        val session = latestSession ?: return
        if (!session.recordsRoute || session.state != AppWorkoutSessionState.ACTIVE ||
            event.runtimeGeneration != activeLocationRuntimeGeneration
        ) return
        when (event) {
            is WorkoutLocationEvent.Permission -> {
                currentGpsStatus = when (event.state) {
                    br.com.mwconsulting.championcompanion.core.model.GpsPermissionState.PRECISE ->
                        WorkoutGpsCaptureStatus.SEARCHING
                    br.com.mwconsulting.championcompanion.core.model.GpsPermissionState.APPROXIMATE,
                    br.com.mwconsulting.championcompanion.core.model.GpsPermissionState.DENIED,
                    br.com.mwconsulting.championcompanion.core.model.GpsPermissionState.NOT_REQUESTED,
                    -> WorkoutGpsCaptureStatus.PERMISSION_REQUIRED
                }
                publishLiveMetrics(session)
            }
            is WorkoutLocationEvent.Service -> {
                currentGpsStatus = when (event.state) {
                    br.com.mwconsulting.championcompanion.core.model.GpsServiceState.ENABLED ->
                        WorkoutGpsCaptureStatus.SEARCHING
                    br.com.mwconsulting.championcompanion.core.model.GpsServiceState.DISABLED ->
                        WorkoutGpsCaptureStatus.LOCATION_DISABLED
                    br.com.mwconsulting.championcompanion.core.model.GpsServiceState.UNAVAILABLE ->
                        WorkoutGpsCaptureStatus.UNAVAILABLE
                }
                publishLiveMetrics(session)
            }
            is WorkoutLocationEvent.Readiness -> {
                val nextStatus = event.value.toCaptureStatus()
                if (nextStatus in setOf(
                        WorkoutGpsCaptureStatus.LOST,
                        WorkoutGpsCaptureStatus.UNAVAILABLE,
                        WorkoutGpsCaptureStatus.LOCATION_DISABLED,
                        WorkoutGpsCaptureStatus.PERMISSION_REQUIRED,
                    )
                ) {
                    routeAccumulator = routeAccumulator?.requestNewSegment(
                        WorkoutRouteSegmentReason.GPS_RECOVERY,
                    )
                    currentSpeedMetersPerSecond = null
                }
                currentGpsStatus = nextStatus
                publishLiveMetrics(session)
            }
            is WorkoutLocationEvent.Fix -> handleLocationFix(event.candidate, session)
            is WorkoutLocationEvent.Gnss -> Unit
        }
    }

    private suspend fun handleLocationFix(
        candidate: br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointCandidate,
        session: AppWorkoutSession,
    ) {
        var accumulator = routeAccumulator ?: return
        if (accumulator.runtimeGeneration != session.runtimeGeneration) return
        if (accumulator.isBatchFull && !flushRouteBatch()) return
        accumulator = routeAccumulator ?: return
        val previous = accumulator.previousAcceptedPoint
        when (
            val result = accumulator.accumulate(
                pointId = WorkoutRoutePointId(routeIds.next()),
                candidate = candidate,
                observedAt = Instant.now(),
            )
        ) {
            is br.com.mwconsulting.championcompanion.core.location.WorkoutRouteAccumulationResult.Accepted -> {
                routeAccumulator = result.accumulator
                currentSpeedMetersPerSecond = currentSpeed(previous, result.point, result.distanceMeters)
                routePolicyLogger.pointFilterSummary(
                    runtimeGeneration = result.accumulator.runtimeGeneration,
                    acceptedCount = result.accumulator.metrics.acceptedPointCount,
                    rejectedCount = result.accumulator.rejectedPointCount,
                    lastRejection = null,
                )
                // Métricas pending não são publicadas até confirmação do append.
                if (result.accumulator.pendingPoints.size >= ROUTE_BATCH_POINT_LIMIT) {
                    flushRouteBatch()
                } else {
                    scheduleRouteFlush()
                }
            }
            is br.com.mwconsulting.championcompanion.core.location.WorkoutRouteAccumulationResult.Rejected -> {
                routeAccumulator = result.accumulator
                currentSpeedMetersPerSecond = null
                routePolicyLogger.pointFilterSummary(
                    runtimeGeneration = result.accumulator.runtimeGeneration,
                    acceptedCount = result.accumulator.metrics.acceptedPointCount,
                    rejectedCount = result.accumulator.rejectedPointCount,
                    lastRejection = result.reason,
                )
                publishLiveMetrics(session)
            }
            is br.com.mwconsulting.championcompanion.core.location.WorkoutRouteAccumulationResult.BatchFull -> {
                routeAccumulator = result.accumulator
                flushRouteBatch()
            }
        }
    }

    private fun scheduleRouteFlush() {
        if (routeFlushJob?.isActive == true) return
        routeFlushJob = scope.launch {
            delay(ROUTE_FLUSH_INTERVAL_MILLIS)
            runSerializedCommand {
                val session = latestSession
                if (session?.state == AppWorkoutSessionState.ACTIVE) flushRouteBatch()
            }
        }
    }

    private suspend fun flushRouteBatch(): Boolean {
        val accumulator = routeAccumulator ?: return true
        if (!accumulator.hasPendingBatch) return persistRouteDistanceCheckpoint()
        if (latestSession?.state != AppWorkoutSessionState.ACTIVE) {
            failRouteCapture()
            return false
        }
        val result = try {
            routeRepository.appendAcceptedPoints(accumulator.buildAppendInput())
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            RepositoryResult.StorageUnavailable
        }
        return when (result) {
            is RepositoryResult.Success -> {
                val confirmed = runCatching { accumulator.confirmPersisted(result.value) }.getOrNull()
                if (confirmed == null) {
                    if (reconcilePendingRouteAppend(accumulator)) {
                        latestSession?.let(::publishLiveMetrics)
                        true
                    } else {
                        failRouteCapture()
                        false
                    }
                } else {
                    routeAccumulator = confirmed
                    confirmedRouteMetrics = confirmed.metrics
                    routePolicyLogger.pointFilterSummary(
                        runtimeGeneration = confirmed.runtimeGeneration,
                        acceptedCount = confirmed.metrics.acceptedPointCount,
                        rejectedCount = confirmed.rejectedPointCount,
                        lastRejection = null,
                    )
                    if (!persistRouteDistanceCheckpoint()) false else {
                        latestSession?.let(::publishLiveMetrics)
                        true
                    }
                }
            }
            RepositoryResult.Conflict,
            RepositoryResult.StorageUnavailable,
            -> if (reconcilePendingRouteAppend(accumulator)) {
                latestSession?.let(::publishLiveMetrics)
                true
            } else {
                failRouteCapture()
                false
            }
            RepositoryResult.NotFound,
            RepositoryResult.UnexpectedState,
            -> {
                failRouteCapture()
                false
            }
        }
    }

    /**
     * Append pode ter sido confirmado no banco antes de a coroutine observar
     * resposta. Reconciliar snapshot antes de falhar evita reenviar lote com
     * revisão obsoleta e mantém retry idempotente.
     */
    private suspend fun reconcilePendingRouteAppend(
        pending: WorkoutRouteAccumulator,
    ): Boolean {
        val snapshot = when (val result = loadRouteSnapshot(pending.sessionId)) {
            is RepositoryResult.Success -> result.value
            else -> null
        } ?: return false
        val lastPending = pending.pendingPoints.lastOrNull() ?: return false
        if (snapshot.route.id != pending.routeId ||
            snapshot.route.sessionId != pending.sessionId ||
            snapshot.route.state != br.com.mwconsulting.championcompanion.core.model.WorkoutRouteState.CAPTURING ||
            snapshot.route.revision != pending.expectedRevision + 1L ||
            snapshot.route.metrics != pending.metrics ||
            snapshot.lastAcceptedPoint != lastPending ||
            snapshot.cursor.lastSequence != lastPending.sequence ||
            snapshot.cursor.lastSampledAt != lastPending.sampledAt ||
            snapshot.cursor.currentSegmentIndex != lastPending.segmentIndex
        ) return false
        return runCatching {
            val confirmed = pending.confirmPersisted(snapshot.route)
            routeAccumulator = confirmed
            confirmedRouteMetrics = confirmed.metrics
            persistRouteDistanceCheckpoint()
        }.getOrDefault(false)
    }

    private suspend fun persistRouteDistanceCheckpoint(): Boolean {
        val route = routeAccumulator ?: return true
        var candidate = latestSession?.takeIf { it.id == route.sessionId } ?: return true
        if (!candidate.recordsRoute || candidate.state !in setOf(
                AppWorkoutSessionState.ACTIVE,
                AppWorkoutSessionState.PAUSED,
            )
        ) return true
        val routeDistance = DomainValue.Present(route.metrics.distanceMeters)
        if (candidate.aggregates.distanceMeters == routeDistance) return true

        repeat(2) {
            val snapshot = timer.snapshot()
            val activeDuration = if (candidate.state == AppWorkoutSessionState.ACTIVE) {
                snapshot.activeDuration
            } else {
                candidate.activeDuration
            }
            val pausedDuration = if (candidate.state == AppWorkoutSessionState.PAUSED) {
                snapshot.pausedDuration
            } else {
                candidate.pausedDuration
            }
            val result = try {
                sessionRepository.checkpoint(
                    AppWorkoutSessionCheckpoint(
                        sessionId = candidate.id,
                        expectedRevision = candidate.revision,
                        runtimeGeneration = candidate.runtimeGeneration,
                        at = auditInstant(candidate, snapshot),
                        aggregates = candidate.aggregates.copy(distanceMeters = routeDistance),
                        activeDuration = activeDuration,
                        pausedDuration = pausedDuration,
                    ),
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                RepositoryResult.StorageUnavailable
            }
            when (result) {
                is RepositoryResult.Success -> {
                    applySession(result.value, snapshot.capturedAtNanos)
                    return true
                }
                RepositoryResult.Conflict -> {
                    candidate = reloadSession(candidate.id) ?: return false
                    if (candidate.aggregates.distanceMeters == routeDistance) return true
                }
                RepositoryResult.NotFound,
                RepositoryResult.StorageUnavailable,
                RepositoryResult.UnexpectedState,
                -> return failRouteCaptureAndReturnFalse()
            }
        }
        return failRouteCaptureAndReturnFalse()
    }

    private fun failRouteCaptureAndReturnFalse(): Boolean {
        failRouteCapture()
        return false
    }

    private fun failRouteCapture() {
        routeCaptureError = ROUTE_CAPTURE_ERROR
        currentGpsStatus = WorkoutGpsCaptureStatus.ERROR
        currentSpeedMetersPerSecond = null
        stopLocationSource(WorkoutGpsCaptureStatus.ERROR)
        latestSession?.let(::publishLiveMetrics)
    }

    private fun stopLocationSource(status: WorkoutGpsCaptureStatus? = null) {
        status?.let { currentGpsStatus = it }
        val source = locationSource
        val sourceGeneration = activeLocationRuntimeGeneration
        locationSource = null
        activeLocationRuntimeGeneration = null
        locationEventsJob?.cancel()
        locationEventsJob = null
        source?.let {
            sourceGeneration?.let(it::stop)
            it.close()
        }
        currentSpeedMetersPerSecond = null
    }

    private fun currentSpeed(
        previous: WorkoutRoutePoint?,
        point: WorkoutRoutePoint,
        distanceMeters: Double,
    ): Double? {
        val candidateSpeed = point.speedMetersPerSecond
        if (candidateSpeed != null) return candidateSpeed
        if (previous == null || previous.segmentIndex != point.segmentIndex || distanceMeters <= 0.0) return null
        val elapsed = Duration.between(previous.sampledAt, point.sampledAt)
        val elapsedSeconds = elapsed.seconds.toDouble() + elapsed.nano / 1_000_000_000.0
        return distanceMeters.takeIf { elapsedSeconds > 0.0 }?.div(elapsedSeconds)
    }

    private fun publishLiveMetrics(session: AppWorkoutSession) {
        if (!session.recordsRoute) {
            liveMetricsStore.clear(session.id)
            return
        }
        val route = routeAccumulator?.takeIf { it.sessionId == session.id }
        val displayMetrics = route?.let { current ->
            if (current.pendingPoints.isEmpty()) current.metrics
            else confirmedRouteMetrics
        }
        val distance = displayMetrics?.distanceMeters
            ?: (session.aggregates.distanceMeters as? DomainValue.Present)?.value
            ?: 0.0
        val acceptedCount = displayMetrics?.acceptedPointCount ?: 0L
        val segmentCount = displayMetrics?.segmentCount ?: 0
        val activeDuration = if (session.state == AppWorkoutSessionState.ACTIVE) {
            timer.snapshot().activeDuration
        } else {
            session.activeDuration
        }
        val activeSeconds = activeDuration.seconds.toDouble() + activeDuration.nano / 1_000_000_000.0
        val averageSpeed = distance.takeIf { it > 0.0 && activeSeconds > 0.0 }
            ?.div(activeSeconds)
        val averagePace = averageSpeed?.takeIf { it > 0.0 }?.let { 1_000.0 / it }
        val currentSpeed = currentSpeedMetersPerSecond?.takeIf { it.isFinite() && it >= 0.0 }
        liveMetricsStore.update(
            WorkoutLiveMetricsSnapshot(
                sessionId = session.id,
                distanceMeters = distance,
                currentPaceSecondsPerKilometer = currentSpeed?.takeIf { it > 0.0 }?.let { 1_000.0 / it },
                averagePaceSecondsPerKilometer = averagePace,
                currentSpeedMetersPerSecond = currentSpeed,
                averageSpeedMetersPerSecond = averageSpeed,
                acceptedPointCount = acceptedCount,
                segmentCount = segmentCount,
                gpsStatus = if (session.state == AppWorkoutSessionState.PAUSED) {
                    WorkoutGpsCaptureStatus.PAUSED
                } else {
                    currentGpsStatus
                },
                captureError = routeCaptureError,
            ),
        )
    }

    private fun aggregatesWithRouteDistance(session: AppWorkoutSession) =
        routeAccumulator?.takeIf { it.sessionId == session.id }?.let { route ->
            session.aggregates.copy(distanceMeters = DomainValue.Present(route.metrics.distanceMeters))
        } ?: session.aggregates

    private fun notificationGpsStatus(session: AppWorkoutSession): String? =
        if (!session.recordsRoute) null else when {
            session.state == AppWorkoutSessionState.PAUSED -> "pausado"
            else -> currentGpsStatus.toNotificationLabel()
        }

    private fun WorkoutLocationEvent.Readiness.toCaptureStatus(): WorkoutGpsCaptureStatus =
        value.toCaptureStatus()

    private fun br.com.mwconsulting.championcompanion.core.model.GpsReadiness.toCaptureStatus(): WorkoutGpsCaptureStatus = when (state) {
        br.com.mwconsulting.championcompanion.core.model.GpsReadinessState.READY -> WorkoutGpsCaptureStatus.READY
        br.com.mwconsulting.championcompanion.core.model.GpsReadinessState.SEARCHING -> WorkoutGpsCaptureStatus.SEARCHING
        br.com.mwconsulting.championcompanion.core.model.GpsReadinessState.LOST -> WorkoutGpsCaptureStatus.LOST
        br.com.mwconsulting.championcompanion.core.model.GpsReadinessState.PERMISSION_REQUIRED,
        br.com.mwconsulting.championcompanion.core.model.GpsReadinessState.PRECISE_PERMISSION_REQUIRED,
        -> WorkoutGpsCaptureStatus.PERMISSION_REQUIRED
        br.com.mwconsulting.championcompanion.core.model.GpsReadinessState.LOCATION_DISABLED ->
            WorkoutGpsCaptureStatus.LOCATION_DISABLED
        br.com.mwconsulting.championcompanion.core.model.GpsReadinessState.UNAVAILABLE ->
            WorkoutGpsCaptureStatus.UNAVAILABLE
    }

    private fun WorkoutGpsCaptureStatus.toNotificationLabel(): String = when (this) {
        WorkoutGpsCaptureStatus.STARTING -> "iniciando"
        WorkoutGpsCaptureStatus.READY -> "pronto"
        WorkoutGpsCaptureStatus.SEARCHING -> "procurando"
        WorkoutGpsCaptureStatus.LOST -> "perdido"
        WorkoutGpsCaptureStatus.UNAVAILABLE -> "indisponível"
        WorkoutGpsCaptureStatus.PERMISSION_REQUIRED -> "permissão necessária"
        WorkoutGpsCaptureStatus.LOCATION_DISABLED -> "localização desativada"
        WorkoutGpsCaptureStatus.PAUSED -> "pausado"
        WorkoutGpsCaptureStatus.ERROR -> "captura pausada"
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

        publishLiveMetrics(session)
        promoteOrUpdateForeground(session)
    }

    private fun promoteOrUpdateForeground(session: AppWorkoutSession) {
        if (!ownershipCoordinator.reserveAttachment(ownerToken, session.id)) {
            cleanupAndStop()
            return
        }
        val notification = notificationBuilder.buildNotification(
            session = session,
            overrideGpsStatus = notificationGpsStatus(session),
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                if (session.state == AppWorkoutSessionState.ACTIVE && session.recordsRoute) {
                    serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }
                if (watchWorkoutParticipationCoordinator.requiresConnectedDeviceForegroundType(session.id)) {
                    serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
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
            stopLocationSource(WorkoutGpsCaptureStatus.PAUSED)
            flushRouteBatch()
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
            is OwnershipReconciliation.RecoveryConfirmed -> {
                watchWorkoutParticipationCoordinator.releaseAtRecovery(id)
                cleanupAndStop()
            }
            OwnershipReconciliation.NoOpenSession -> cleanupAndStop()
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
        stopLocationSource()
        ownershipCoordinator.release(ownerToken)
        checkpointJob?.cancel()
        checkpointJob = null
        sessionObserverJob?.cancel()
        sessionObserverJob = null
        watchParticipationJob?.cancel()
        watchParticipationJob = null
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
