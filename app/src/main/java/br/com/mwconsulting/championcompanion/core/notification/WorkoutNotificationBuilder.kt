package br.com.mwconsulting.championcompanion.core.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import br.com.mwconsulting.championcompanion.MainActivity
import br.com.mwconsulting.championcompanion.R
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSession
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionState
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.feature.exercises.WorkoutRecordingService
import java.time.Duration
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutNotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun buildNotification(
        session: AppWorkoutSession,
        overrideActiveDuration: Duration? = null,
        overridePausedDuration: Duration? = null,
        overrideState: AppWorkoutSessionState? = null,
    ): Notification {
        val effectiveState = overrideState ?: session.state
        val effectiveActiveDuration = overrideActiveDuration ?: session.activeDuration
        val effectivePausedDuration = overridePausedDuration ?: session.pausedDuration
        val typeLabel = session.type.toNotificationLabel()
        val durationText = formatDuration(effectiveActiveDuration)
        val isPaused = effectiveState == AppWorkoutSessionState.PAUSED
        val isFinishing = effectiveState == AppWorkoutSessionState.FINISHING

        val title = if (isPaused) "$typeLabel pausado" else typeLabel
        val body = if (isPaused) "Duração: $durationText · Pausado" else "Duração: $durationText"

        val sessionId = session.id.value.toString()

        return NotificationCompat.Builder(context, WorkoutNotificationChannel.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_champion)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openAppIntent(sessionId))
            .apply {
                if (!isFinishing) {
                    if (isPaused) {
                        addAction(
                            0,
                            "Continuar",
                            serviceActionIntent(WorkoutRecordingService.ACTION_RESUME, sessionId),
                        )
                    } else {
                        addAction(
                            0,
                            "Pausar",
                            serviceActionIntent(WorkoutRecordingService.ACTION_PAUSE, sessionId),
                        )
                    }
                    addAction(
                        0,
                        "Finalizar",
                        serviceActionIntent(WorkoutRecordingService.ACTION_FINISH, sessionId),
                    )
                }
            }
            .build()
    }

    private fun openAppIntent(sessionId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_FROM_NOTIFICATION, true)
            putExtra(EXTRA_SESSION_ID, sessionId)
        }
        return PendingIntent.getActivity(
            context,
            OPEN_APP_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun serviceActionIntent(action: String, sessionId: String): PendingIntent {
        val intent = Intent(context, WorkoutRecordingService::class.java).apply {
            this.action = action
            putExtra(WorkoutRecordingService.EXTRA_SESSION_ID, sessionId)
        }
        val requestCode = actionRequestCode(action, sessionId)
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun actionRequestCode(action: String, sessionId: String): Int {
        val base = when (action) {
            WorkoutRecordingService.ACTION_RESUME -> 1002
            WorkoutRecordingService.ACTION_PAUSE -> 1003
            WorkoutRecordingService.ACTION_FINISH -> 1004
            else -> 1005
        }
        return base + sessionId.hashCode()
    }

    private fun WorkoutType.toNotificationLabel(): String = when (this) {
        WorkoutType.WALKING -> "Caminhada"
        WorkoutType.RUNNING -> "Corrida"
        WorkoutType.FREE_WORKOUT -> "Treino livre"
        WorkoutType.TREADMILL -> "Esteira"
        WorkoutType.UNKNOWN -> "Treino"
    }

    private fun formatDuration(duration: Duration): String {
        val totalSeconds = duration.seconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    companion object {
        const val EXTRA_OPEN_FROM_NOTIFICATION = "open_from_notification"
        const val EXTRA_SESSION_ID = "notification_session_id"
        private const val OPEN_APP_REQUEST_CODE = 1001
    }
}
