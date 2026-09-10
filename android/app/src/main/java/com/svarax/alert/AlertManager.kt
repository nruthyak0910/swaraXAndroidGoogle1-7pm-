package com.svarax.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.svarax.MainActivity
import com.svarax.risk.RiskResult
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Priority 9: Real-Time Alert Manager.
 * Dispatches high-priority Android notifications and alerts listeners (e.g. LiveCall UI)
 * based on risk thresholds (LOW, MEDIUM, HIGH, CRITICAL).
 */
class AlertManager(private val context: Context) {

    companion object {
        private const val TAG = "SvaraX_AlertManager"
        private const val ALERT_CHANNEL_ID = "svara_fraud_alerts_v2"
        private const val ALERT_NOTIFICATION_ID = 2002
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val alertListeners = CopyOnWriteArrayList<(RiskResult) -> Unit>()
    private var lastNotifiedLevel: String? = null

    init {
        createAlertNotificationChannel()
    }

    fun addAlertListener(listener: (RiskResult) -> Unit) {
        alertListeners.add(listener)
    }

    fun removeAlertListener(listener: (RiskResult) -> Unit) {
        alertListeners.remove(listener)
    }

    /**
     * Evaluates whether a new alert needs to be broadcast or displayed.
     */
    fun dispatchAlert(result: RiskResult, phoneNumber: String?) {
        // 1. Notify in-app UI listeners immediately
        for (listener in alertListeners) {
            try {
                listener(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking alert listener", e)
            }
        }

        // 2. Only fire sound/heads-up notifications when risk enters HIGH or CRITICAL
        if (result.riskLevel == "HIGH" || result.riskLevel == "CRITICAL") {
            showHighRiskNotification(result, phoneNumber)
            lastNotifiedLevel = result.riskLevel
        } else if (result.riskLevel == "MEDIUM" && lastNotifiedLevel != "MEDIUM") {
            showMediumRiskNotification(result, phoneNumber)
            lastNotifiedLevel = result.riskLevel
        }
    }

    fun clearAlerts() {
        notificationManager.cancel(ALERT_NOTIFICATION_ID)
        lastNotifiedLevel = null
    }

    private fun showHighRiskNotification(result: RiskResult, phoneNumber: String?) {
        val callerLabel = phoneNumber ?: "Unknown Caller"
        val title = "🚨 ${result.riskLevel} FRAUD RISK DETECTED (${result.riskScore}%)"

        val bulletReasons = result.indicators.take(3).joinToString("\n• ", prefix = "• ")
        val content = "Caller: $callerLabel\n$bulletReasons\n\nRecommendation: ${result.recommendation}"

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText("Immediate threat: ${result.recommendation}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFFEF4444.toInt()) // Red accent
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun showMediumRiskNotification(result: RiskResult, phoneNumber: String?) {
        val title = "⚠ Suspicious Pattern Detected (${result.riskScore}%)"
        val content = "Caller: ${phoneNumber ?: "Active Call"}\n${result.recommendation}"

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFFF59E0B.toInt()) // Amber accent
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun createAlertNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Svara_X Real-Time Fraud Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emits urgent warnings and advice during high-risk active phone calls."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
