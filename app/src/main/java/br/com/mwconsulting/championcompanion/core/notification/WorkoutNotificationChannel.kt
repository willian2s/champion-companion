package br.com.mwconsulting.championcompanion.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import br.com.mwconsulting.championcompanion.R

object WorkoutNotificationChannel {
    const val CHANNEL_ID = "workout_session_active"
    const val NOTIFICATION_ID = 1001

    fun create(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_workout_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_workout_desc)
            setShowBadge(false)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
