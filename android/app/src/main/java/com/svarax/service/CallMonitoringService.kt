package com.svarax.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.svarax.MainActivity
import com.svarax.call.CallStateManager

/**
 * Priority 4 & 5: Foreground Service for Active Call Monitoring.
 * Maintains process priority and persistent notification while a call is active.
 * Shuts down automatically when call returns to IDLE.
 */
class CallMonitoringService : Service() {

    companion object {
        private const val TAG = "SvaraX_MonitoringSvc"
        private const val CHANNEL_ID = "svara_call_protection_channel"
        private const val NOTIFICATION_ID = 1001

        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_STATE = "extra_state"
    }

    private var activePhoneNumber: String = "Unknown Caller"

    private val callStateListener: (CallStateManager.CallEvent) -> Unit = { event ->
        when (event.state) {
            CallStateManager.State.IDLE -> {
                Log.i(TAG, "Call returned to IDLE. Stopping foreground monitoring service.")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            CallStateManager.State.OFFHOOK -> {
                updateNotification("Call Active - Monitoring Audio for Fraud Indicators", event.incomingNumber)
            }
            CallStateManager.State.RINGING -> {
                updateNotification("Incoming Call Screened: ${event.incomingNumber ?: "Unknown"}", event.incomingNumber)
            }
            CallStateManager.State.DISCONNECTED -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        CallStateManager.addListener(callStateListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val number = intent?.getStringExtra(EXTRA_PHONE_NUMBER) ?: activePhoneNumber
        activePhoneNumber = number

        val notification = buildNotification("Monitoring Active Call", "Protecting against fraud & scams")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        CallStateManager.removeListener(callStateListener)
        Log.i(TAG, "CallMonitoringService destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Svara_X Call Protection",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows real-time fraud warning and active call protection status."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(title: String, phoneNumber: String?) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val content = if (phoneNumber != null) "Caller: $phoneNumber | Svara_X Shield Active" else "Shield Active"
        manager.notify(NOTIFICATION_ID, buildNotification(title, content))
    }
}
