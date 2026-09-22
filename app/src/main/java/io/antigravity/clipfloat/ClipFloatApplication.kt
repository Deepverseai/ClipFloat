package io.antigravity.clipfloat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class ClipFloatApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ClipFloat Engine Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground notification keeping the ClipFloat HUD active"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "clipfloat_engine_channel"
    }
}
