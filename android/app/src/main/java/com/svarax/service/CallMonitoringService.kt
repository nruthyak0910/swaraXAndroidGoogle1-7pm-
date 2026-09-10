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
import com.svarax.alert.AlertManager
import com.svarax.audio.AudioChunk
import com.svarax.audio.MicrophoneAudioInput
import com.svarax.call.CallStateManager
import com.svarax.fraud.FraudDetector
import com.svarax.fraud.FraudIndicator
import com.svarax.history.CallHistoryRepository
import com.svarax.history.CallRecord
import com.svarax.risk.RiskEngine
import com.svarax.risk.RiskResult
import com.svarax.speech.AndroidSpeechToText
import com.svarax.speech.DemoSpeechToText
import com.svarax.speech.SpeechToText
import com.svarax.voice.VoiceAnalysisResult
import com.svarax.voice.VoiceAnalyzer
import com.svarax.voice.VoiceAnalyzerImpl
import java.util.UUID

/**
 * Priority 5: Full-Lifecycle Active Call Monitoring & Analysis Service.
 * Manages the audio capture, speech recognition, fraud detection, voice analysis,
 * risk scoring, alerting, and history recording pipelines while a call is active.
 */
class CallMonitoringService : Service() {

    companion object {
        private const val TAG = "SvaraX_MonitoringSvc"
        private const val CHANNEL_ID = "svara_call_protection_channel"
        private const val NOTIFICATION_ID = 1001

        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_STATE = "extra_state"
        const val EXTRA_IS_DEMO = "extra_is_demo"

        // Global live risk state accessible by LiveCallActivity / MainActivity
        @Volatile
        var currentRiskResult: RiskResult? = null
            private set

        @Volatile
        var cumulativeTranscript: String = ""
            private set
    }

    private var activePhoneNumber: String = "Unknown Caller"
    private var isDemoMode: Boolean = false
    private var callStartTime: Long = 0L

    // Pipeline Modules
    private lateinit var audioInput: MicrophoneAudioInput
    private lateinit var speechToText: SpeechToText
    private lateinit var fraudDetector: FraudDetector
    private lateinit var voiceAnalyzer: VoiceAnalyzer
    private lateinit var riskEngine: RiskEngine
    private lateinit var alertManager: AlertManager
    private lateinit var historyRepo: CallHistoryRepository

    private val detectedIndicators = mutableListOf<FraudIndicator>()
    private var latestVoiceResult: VoiceAnalysisResult? = null

    private val callStateListener: (CallStateManager.CallEvent) -> Unit = { event ->
        when (event.state) {
            CallStateManager.State.IDLE -> {
                Log.i(TAG, "Call ended (IDLE). Finalizing analysis and stopping service.")
                finalizeAndSaveCall()
                stopAnalysisPipeline()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            CallStateManager.State.OFFHOOK -> {
                Log.i(TAG, "Call became active (OFFHOOK). Commencing real-time audio pipeline.")
                startAnalysisPipeline()
                updateNotification("Call Active: Analyzing Conversation for Fraud", activePhoneNumber, null)
            }
            CallStateManager.State.RINGING -> {
                updateNotification("Incoming Call Screened: ${event.incomingNumber ?: activePhoneNumber}", event.incomingNumber, null)
            }
            CallStateManager.State.DISCONNECTED -> {
                finalizeAndSaveCall()
                stopAnalysisPipeline()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Initialize modules
        audioInput = MicrophoneAudioInput()
        fraudDetector = FraudDetector()
        voiceAnalyzer = VoiceAnalyzerImpl()
        riskEngine = RiskEngine()
        alertManager = AlertManager(this)
        historyRepo = CallHistoryRepository(this)

        CallStateManager.addListener(callStateListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val number = intent?.getStringExtra(EXTRA_PHONE_NUMBER) ?: activePhoneNumber
        isDemoMode = intent?.getBooleanExtra(EXTRA_IS_DEMO, false) ?: false
        activePhoneNumber = number
        callStartTime = System.currentTimeMillis()

        speechToText = if (isDemoMode) {
            DemoSpeechToText()
        } else {
            AndroidSpeechToText(this)
        }

        val notification = buildNotification(
            "Svara_X Call Protection",
            "Screening call: $activePhoneNumber | Shield Active"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // If service was launched while call is already OFFHOOK or in Demo mode, start pipeline immediately
        if (CallStateManager.getCurrentState() == CallStateManager.State.OFFHOOK || isDemoMode) {
            startAnalysisPipeline()
        }

        return START_NOT_STICKY
    }

    private fun startAnalysisPipeline() {
        if (callStartTime == 0L) callStartTime = System.currentTimeMillis()
        cumulativeTranscript = ""
        detectedIndicators.clear()
        voiceAnalyzer.reset()

        // 1. Start audio input (Microphone) if not in demo mode
        if (!isDemoMode) {
            audioInput.start { chunk: AudioChunk ->
                // Feed acoustic analyzer
                latestVoiceResult = voiceAnalyzer.processChunk(chunk)
                // Stream to speech recognizer if needed
                speechToText.processAudioChunk(chunk)
            }
        }

        // 2. Start Speech-to-Text
        speechToText.start(
            onTranscript = { chunk ->
                handleNewTranscriptChunk(chunk.text)
            },
            onError = { errorMsg ->
                Log.w(TAG, "STT Warning: $errorMsg")
            }
        )
    }

    private fun handleNewTranscriptChunk(newText: String) {
        if (newText.isBlank()) return

        cumulativeTranscript = if (cumulativeTranscript.isBlank()) {
            newText
        } else {
            "$cumulativeTranscript $newText"
        }

        Log.d(TAG, "Transcript chunk received: $newText")

        // 3. Fraud Detection
        val newIndicators = fraudDetector.analyze(cumulativeTranscript)
        for (ind in newIndicators) {
            if (!detectedIndicators.any { it.category == ind.category }) {
                detectedIndicators.add(ind)
            }
        }

        // 4. Evaluate in Risk Engine
        val riskResult = riskEngine.evaluate(
            indicators = detectedIndicators,
            voiceResult = latestVoiceResult,
            isDemo = isDemoMode
        )
        currentRiskResult = riskResult

        // 5. Dispatch Alerts & Notifications
        alertManager.dispatchAlert(riskResult, activePhoneNumber)

        // 6. Update Foreground Notification
        val notificationTitle = when (riskResult.riskLevel) {
            "CRITICAL" -> "🚨 CRITICAL FRAUD ALERT (${riskResult.riskScore}%)"
            "HIGH" -> "⚠ HIGH FRAUD RISK (${riskResult.riskScore}%)"
            "MEDIUM" -> "Caution: Suspicious Pattern (${riskResult.riskScore}%)"
            else -> "Call Active: Monitoring for Scams"
        }
        updateNotification(notificationTitle, activePhoneNumber, riskResult)
    }

    private fun stopAnalysisPipeline() {
        try {
            audioInput.stop()
            speechToText.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio/STT pipeline", e)
        }
    }

    private fun finalizeAndSaveCall() {
        val duration = ((System.currentTimeMillis() - callStartTime) / 1000).toInt().coerceAtLeast(1)
        val finalResult = currentRiskResult ?: riskEngine.evaluate(emptyList(), latestVoiceResult, isDemoMode)

        val record = CallRecord(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            callerNumber = activePhoneNumber,
            durationSeconds = duration,
            finalRiskScore = finalResult.riskScore,
            riskLevel = finalResult.riskLevel,
            detectedIndicators = finalResult.indicators,
            recommendation = finalResult.recommendation,
            fullTranscriptSnippet = cumulativeTranscript.take(500),
            isDemoSimulation = isDemoMode
        )

        historyRepo.saveRecord(record)
        currentRiskResult = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAnalysisPipeline()
        CallStateManager.removeListener(callStateListener)
        alertManager.clearAlerts()
        Log.i(TAG, "CallMonitoringService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Svara_X Call Protection",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows active call protection status and in-call fraud warnings."
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

    private fun updateNotification(title: String, phoneNumber: String?, result: RiskResult?) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val content = if (result != null && result.riskLevel != "LOW") {
            "${result.riskLevel} (${result.riskScore}%): ${result.recommendation}"
        } else {
            "Caller: ${phoneNumber ?: "Active Call"} | Svara_X Shield Active"
        }
        manager.notify(NOTIFICATION_ID, buildNotification(title, content))
    }
}
