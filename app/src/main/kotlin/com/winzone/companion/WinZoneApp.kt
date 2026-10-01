package com.winzone.companion

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.winzone.companion.util.Constants
import com.winzone.companion.util.RingBufferTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class WinZoneApp : Application() {

    companion object {
        lateinit var ringBufferTree: RingBufferTree
            private set
    }

    override fun onCreate() {
        super.onCreate()

        ringBufferTree = RingBufferTree(500)
        Timber.plant(ringBufferTree)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        createNotificationChannels()
        Timber.i("WinZoneApp initialized (v%s, code %d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Capture Channel: Low importance, ongoing, silent
            val captureChannel = NotificationChannel(
                Constants.CAPTURE_CHANNEL_ID,
                getString(R.string.notification_channel_capture_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_capture_desc)
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }

            // Alerts Channel: High importance, alerts user for action
            val alertsChannel = NotificationChannel(
                Constants.ALERTS_CHANNEL_ID,
                getString(R.string.notification_channel_alerts_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_alerts_desc)
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannel(captureChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }
}
