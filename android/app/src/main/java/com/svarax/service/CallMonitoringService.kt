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
import com.svarax.audio.AudioDiagnostics
import com.svarax.audio.AudioInput
import com.svarax.audio.AudioInputState
import com.svarax.audio.AudioSignal
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
import com.svarax.speech.PcmConsumerSpeechToText
import com.svarax.speech.SpeechToText
import com.svarax.voice.VoiceAnalysisResult
import com.svarax.voice.VoiceAnalyzer
import com.svarax.voice.VoiceAnalyzerImpl
import java.util.UUID

/**
 * CallMonitoringService: Background Foreground Service managing call lifecycle and analysis pipeline.
 *
 * Enforces strict separation between LIVE mode (genuine AudioRecord PCM + platform STT)
 * and DEMO mode (isolated DemoSpeechToText script).
 *
 * If cellular audio capture is blocked by Android OS, accurately reports
 * "Live call audio unavailable on this device" and never synthesizes fake scores or transcripts.
 */
class CallMonitoringService : Service() {

    companion object {
        private const val TAG = "SvaraX_MonitoringSvc"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_IS_DEMO = "extra_is_demo"
        const val EXTRA_STATE = "extra_state"
        private const val CHANNEL_ID = "svara_call_monitoring_v2"
        private const val NOTIFICATION_ID = 1001

        // Expose state observable by UI
        @Volatile
        var isServiceRunning: Boolean = false
            private set

        @Volatile
        var isDemoModeActive: Boolean = false
            private set

        @Volatile
        var isLiveAudioUnavailable: Boolean = false
            private set

        @Volatile
        var cumulativeTranscript: String = ""
            private set

        @Volatile
        var currentRiskResult: RiskResult? = null
            private set

        @Volatile
        var activeCallerNumber: String = "Unknown caller"
            private set

        @Volatile
        var latestAudioDiagnostics: AudioDiagnostics? = null
            private set

        @Volatile
        var latestAudioState: AudioInputState = AudioInputState.UNAVAILABLE
            private set
    }

    private var activePhoneNumber: String = "Unknown caller"
    private var isDemoMode: Boolean = false
    private var callStartTime: Long = 0L
    private var isAnalyzing = false
    private var isCallSaved = false

    // Pipeline Components
    private lateinit var audioInput: AudioInput
    private lateinit var speechToText: SpeechToText
    private lateinit var fraudDetector: FraudDetector
    private lateinit var voiceAnalyzer: VoiceAnalyzer
    private lateinit var riskEngine: RiskEngine
    private lateinit var alertManager: AlertManager
    private lateinit var historyRepo: CallHistoryRepository

    private val detectedIndicators = mutableListOf<FraudIndicator>()
    private var latestVoiceResult: VoiceAnalysisResult? = null

    private val callStateListener: (CallStateManager.CallEvent) -> Unit = { event ->
        Log.d(TAG, "CallStateListener received event: ${event.state} for number: ${event.incomingNumber}")
        when (event.state) {
            CallStateManager.State.IDLE, CallStateManager.State.DISCONNECTED -> {
                Log.i(TAG, "Call ended (${event.state}). Finalizing analysis and stopping service.")
                finalizeAndSaveCall()
                stopAnalysisPipeline()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            CallStateManager.State.OFFHOOK -> {
                Log.i(TAG, "Call answered (OFFHOOK). Commencing real-time monitoring pipeline.")
                startAnalysisPipeline()
                updateNotification(
                    title = "Call Active: Monitoring",
                    phoneNumber = activePhoneNumber,
                    result = currentRiskResult,
                    statusSubtitle = if (isLiveAudioUnavailable) "Live call audio unavailable on this device." else "Scanning for fraud patterns"
                )
            }
            CallStateManager.State.RINGING -> {
                val num = event.incomingNumber ?: activePhoneNumber
                activePhoneNumber = num
                activeCallerNumber = num
                updateNotification("Incoming Call Screened", activePhoneNumber, null, "Shield Active")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()

        audioInput = MicrophoneAudioInput()
        fraudDetector = FraudDetector()
        voiceAnalyzer = VoiceAnalyzerImpl()
        riskEngine = RiskEngine()
        alertManager = AlertManager(this)
        historyRepo = CallHistoryRepository(this)

        CallStateManager.addListener(callStateListener)
        Log.i(TAG, "CallMonitoringService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val number = intent?.getStringExtra(EXTRA_PHONE_NUMBER)
        if (!number.isNullOrBlank()) {
            activePhoneNumber = number
            activeCallerNumber = number
        }

        isDemoMode = intent?.getBooleanExtra(EXTRA_IS_DEMO, false) ?: false
        isDemoModeActive = isDemoMode
        callStartTime = System.currentTimeMillis()
        isCallSaved = false
        isLiveAudioUnavailable = false

        // STRICT ISOLATION: LIVE mode never instantiates DemoSpeechToText
        speechToText = if (isDemoMode) {
            DemoSpeechToText()
        } else {
            AndroidSpeechToText(this)
        }

        Log.i(TAG, "Service started with mode: ${if (isDemoMode) "DEMO" else "LIVE"}, STT engine: ${speechToText.engineName}")

        val notification = buildNotification(
            title = if (isDemoMode) "Svara_X Call Protection [DEMO]" else "Svara_X Call Protection",
            content = "Screening call: $activePhoneNumber | Shield Active"
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

        // Start analysis immediately if call is already in active conversation or in demo mode
        if (CallStateManager.getCurrentState() == CallStateManager.State.OFFHOOK || isDemoMode) {
            startAnalysisPipeline()
        }

        return START_NOT_STICKY
    }

    @Synchronized
    private fun startAnalysisPipeline() {
        if (isAnalyzing) {
            Log.d(TAG, "startAnalysisPipeline: already analyzing, skipping duplicate start")
            return
        }
        isAnalyzing = true

        if (callStartTime == 0L) callStartTime = System.currentTimeMillis()
        cumulativeTranscript = ""
        currentRiskResult = null
        detectedIndicators.clear()
        voiceAnalyzer.reset()
        isLiveAudioUnavailable = false

        // 1. Start audio input (MicrophoneAudioInput)
        if (!isDemoMode) {
            audioInput.start { chunk: AudioChunk ->
                latestAudioDiagnostics = audioInput.getDiagnostics()
                latestAudioState = audioInput.getState()

                // Feed genuine acoustic analyzer
                latestVoiceResult = voiceAnalyzer.processChunk(chunk)

                // Track signal availability
                if (chunk.signal == AudioSignal.SILENCE && audioInput.getState() == AudioInputState.NO_SIGNAL) {
                    if (!isLiveAudioUnavailable) {
                        isLiveAudioUnavailable = true
                        Log.w(TAG, "Live audio unavailable: AudioRecord receiving continuous silence/zero samples during call")
                        updateNotification(
                            title = "Call Active",
                            phoneNumber = activePhoneNumber,
                            result = null,
                            statusSubtitle = "Live call audio unavailable on this device."
                        )
                    }
                } else if (chunk.signal == AudioSignal.SIGNAL_PRESENT) {
                    if (isLiveAudioUnavailable) {
                        isLiveAudioUnavailable = false
                    }
                }

                // Explicit STT consumption: Only feed raw PCM if STT explicitly supports PCM consumption
                if (speechToText is PcmConsumerSpeechToText) {
                    (speechToText as PcmConsumerSpeechToText).processAudioChunk(chunk)
                }
            }
        }

        // 2. Start Speech-to-Text
        speechToText.start(
            onTranscript = { chunk ->
                handleNewTranscriptChunk(chunk.text)
            },
            onError = { errorMsg ->
                Log.w(TAG, "Speech-to-Text reporting status: $errorMsg")
                if (!isDemoMode) {
                    // Cellular isolation prevents STT from hearing caller
                    isLiveAudioUnavailable = true
                }
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

        Log.d(TAG, "Transcript chunk received [${if (isDemoMode) "DEMO" else "LIVE"}]: \"$newText\"")

        // 3. Fraud Detection
        val newIndicators = fraudDetector.analyze(cumulativeTranscript)
        for (ind in newIndicators) {
            if (!detectedIndicators.any { it.category == ind.category }) {
                detectedIndicators.add(ind)
                Log.i(TAG, "New Fraud Indicator identified: ${ind.category} (Confidence: ${ind.confidence})")
            }
        }

        // 4. Dynamic Risk Calculation
        val riskResult = riskEngine.evaluate(
            indicators = detectedIndicators,
            voiceResult = latestVoiceResult,
            isDemo = isDemoMode
        )
        currentRiskResult = riskResult

        // 5. Alert Dispatch
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

    @Synchronized
    private fun stopAnalysisPipeline() {
        if (!isAnalyzing) return
        isAnalyzing = false

        try {
            audioInput.stop()
            speechToText.stop()
            latestAudioDiagnostics = audioInput.getDiagnostics()
            latestAudioState = audioInput.getState()
            Log.i(TAG, "Audio and STT pipelines stopped cleanly")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio/STT pipeline", e)
        }
    }

    @Synchronized
    private fun finalizeAndSaveCall() {
        if (isCallSaved) {
            Log.d(TAG, "finalizeAndSaveCall: already saved for this session, skipping")
            return
        }
        isCallSaved = true

        val duration = ((System.currentTimeMillis() - callStartTime) / 1000).toInt().coerceAtLeast(1)

        val record = if (isDemoMode) {
            val finalResult = currentRiskResult ?: riskEngine.evaluate(detectedIndicators, latestVoiceResult, true)
            CallRecord(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                callerNumber = activePhoneNumber,
                durationSeconds = duration,
                finalRiskScore = finalResult.riskScore,
                riskLevel = finalResult.riskLevel,
                detectedIndicators = finalResult.indicators,
                recommendation = finalResult.recommendation,
                fullTranscriptSnippet = cumulativeTranscript.take(500),
                isDemoSimulation = true
            )
        } else {
            // LIVE CALL: Accurately report if live audio was unavailable or if real analysis occurred
            if (isLiveAudioUnavailable || cumulativeTranscript.isBlank()) {
                CallRecord(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    callerNumber = activePhoneNumber,
                    durationSeconds = duration,
                    finalRiskScore = 0,
                    riskLevel = "UNVERIFIED",
                    detectedIndicators = emptyList(),
                    recommendation = "Live call audio was unavailable on this device during the call.",
                    fullTranscriptSnippet = "[Audio unavailable due to Android OS cellular isolation]",
                    isDemoSimulation = false
                )
            } else {
                val finalResult = currentRiskResult ?: riskEngine.evaluate(detectedIndicators, latestVoiceResult, false)
                CallRecord(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    callerNumber = activePhoneNumber,
                    durationSeconds = duration,
                    finalRiskScore = finalResult.riskScore,
                    riskLevel = finalResult.riskLevel,
                    detectedIndicators = finalResult.indicators,
                    recommendation = finalResult.recommendation,
                    fullTranscriptSnippet = cumulativeTranscript.take(500),
                    isDemoSimulation = false
                )
            }
        }

        historyRepo.saveRecord(record)
        Log.i(TAG, "Call record persisted: caller=${record.callerNumber}, score=${record.finalRiskScore}%, level=${record.riskLevel}, isDemo=${record.isDemoSimulation}")
        currentRiskResult = null
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
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

    private fun updateNotification(
        title: String,
        phoneNumber: String?,
        result: RiskResult?,
        statusSubtitle: String? = null
    ) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val content = when {
            statusSubtitle != null -> statusSubtitle
            result != null && result.riskLevel != "LOW" -> "${result.riskLevel} (${result.riskScore}%): ${result.recommendation}"
            else -> "Caller: ${phoneNumber ?: "Active Call"} | Svara_X Shield Active"
        }
        manager.notify(NOTIFICATION_ID, buildNotification(title, content))
    }
}
