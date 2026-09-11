package com.svarax.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.svarax.risk.RiskResult
import com.svarax.ui.LiveCallActivity
import java.util.concurrent.CopyOnWriteArrayList

/**
 * AlertManager: Dispatches high-priority heads-up warnings, device vibration, and in-call alerts.
 *
 * Implements Android-compliant heads-up notification with fullScreenIntent fallback.
 * Prevents duplicate alerts and guarantees automatic vibration on physical hardware.
 */
class AlertManager(private val context: Context) {

    companion object {
        private const val TAG = "SvaraX_AlertManager"
        private const val ALERT_CHANNEL_ID = "svara_fraud_alerts_v3"
        private const val ALERT_NOTIFICATION_ID = 2002
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val alertListeners = CopyOnWriteArrayList<(RiskResult) -> Unit>()
    private var lastNotifiedLevel: String? = null
    private var lastNotifiedScore: Int = -1

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
     * Automatically triggers when risk crosses HIGH or CRITICAL.
     */
    fun dispatchAlert(result: RiskResult, phoneNumber: String?) {
        // 1. Notify in-app UI listeners immediately (if LiveCallActivity or UI is open)
        for (listener in alertListeners) {
            try {
                listener(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking alert listener", e)
            }
        }

        // 2. Prevent duplicate alerts if level is unchanged and score hasn't escalated
        val isEscalation = (result.riskLevel != lastNotifiedLevel) || (result.riskScore >= lastNotifiedScore + 10)
        if (!isEscalation) {
            Log.d(TAG, "Suppressing duplicate alert: level=${result.riskLevel}, score=${result.riskScore}%")
            return
        }

        // 3. Fire high-priority alert when risk crosses HIGH or CRITICAL
        if (result.riskLevel == "HIGH" || result.riskLevel == "CRITICAL") {
            Log.w(TAG, "Triggering automatic urgent warning for ${result.riskLevel} fraud risk (${result.riskScore}%)")
            triggerDeviceVibration()
            showHighRiskWarningNotification(result, phoneNumber)
            lastNotifiedLevel = result.riskLevel
            lastNotifiedScore = result.riskScore
        } else if (result.riskLevel == "MEDIUM" && lastNotifiedLevel != "MEDIUM") {
            showMediumRiskNotification(result, phoneNumber)
            lastNotifiedLevel = result.riskLevel
            lastNotifiedScore = result.riskScore
        }
    }

    fun clearAlerts() {
        notificationManager.cancel(ALERT_NOTIFICATION_ID)
        lastNotifiedLevel = null
        lastNotifiedScore = -1
    }

    private fun triggerDeviceVibration() {
        try {
            if (context.checkSelfPermission(android.Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "VIBRATE permission not granted; relying solely on notification channel vibration")
                return
            }

            val pattern = longArrayOf(0, 450, 150, 450)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(pattern, -1)
                }
            }
            Log.i(TAG, "Device vibration pattern triggered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error executing device vibration", e)
        }
    }

    private fun showHighRiskWarningNotification(result: RiskResult, phoneNumber: String?) {
        val callerLabel = phoneNumber ?: "Unknown caller"
        val title = "🚨 ${result.riskLevel} FRAUD RISK DETECTED (${result.riskScore}%)"

        val bulletReasons = if (result.indicators.isNotEmpty()) {
            result.indicators.take(3).joinToString("\n• ", prefix = "• ")
        } else {
            "• Urgent high-risk conversational pattern detected"
        }
        val content = "Caller: $callerLabel\n$bulletReasons\n\nRecommendation: ${result.recommendation}"

        // Full-screen intent target
        val fullScreenIntent = Intent(context, LiveCallActivity::class.java).apply {
            putExtra("caller_number", callerLabel)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            ALERT_NOTIFICATION_ID,
            fullScreenIntent,
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
            .setVibrate(longArrayOf(0, 450, 150, 450))
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setColor(0xFFEF4444.toInt()) // High contrast red
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
        Log.i(TAG, "Heads-up high-risk warning notification dispatched")
    }

    private fun showMediumRiskNotification(result: RiskResult, phoneNumber: String?) {
        val title = "⚠ Suspicious Pattern Detected (${result.riskScore}%)"
        val content = "Caller: ${phoneNumber ?: "Active Call"}\n${result.recommendation}"

        val intent = Intent(context, LiveCallActivity::class.java).apply {
            putExtra("caller_number", phoneNumber ?: "Unknown caller")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            ALERT_NOTIFICATION_ID,
            intent,
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
                description = "Urgent heads-up warnings and advice during high-risk active phone calls."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 450, 150, 450)
                enableLights(true)
                lightColor = 0xFFEF4444.toInt()
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
