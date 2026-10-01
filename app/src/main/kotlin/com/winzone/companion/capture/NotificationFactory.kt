package com.winzone.companion.capture

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.winzone.companion.MainActivity
import com.winzone.companion.R
import com.winzone.companion.util.Constants

object NotificationFactory {

    fun buildCaptureNotification(context: Context, shortMatchId: String): Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                action = Constants.ACTION_OPEN_FROM_NOTIFICATION
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, Constants.CAPTURE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_capture_notification)
            .setContentTitle(context.getString(R.string.notification_capture_title))
            .setContentText(context.getString(R.string.notification_capture_text, shortMatchId))
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
