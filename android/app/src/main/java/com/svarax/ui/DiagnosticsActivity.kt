package com.svarax.ui

import android.content.Context
import android.graphics.Color
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.svarax.R
import com.svarax.audio.AudioChunk
import com.svarax.audio.AudioInputState
import com.svarax.audio.AudioSignal
import com.svarax.audio.MicrophoneAudioInput
import com.svarax.call.CallStateManager
import com.svarax.permission.PermissionHelper
import com.svarax.service.CallMonitoringService

/**
 * DiagnosticsActivity: Physical Hardware and Subsystem Diagnostics Screen.
 *
 * Exposes live status for:
 * 1. Call screening (ACTIVE / INACTIVE)
 * 2. Microphone permission (GRANTED / DENIED)
 * 3. Notifications (GRANTED / DENIED)
 * 4. AudioRecord (ACTIVE / UNAVAILABLE)
 * 5. Audio signal (DETECTED / SILENCE / UNAVAILABLE)
 * 6. Speech recognition (AVAILABLE / UNAVAILABLE)
 * 7. Live call audio (AVAILABLE / PLATFORM LIMITED / UNAVAILABLE)
 * 8. Analysis mode (LIVE / DEMO / IDLE)
 *
 * Includes an on-device 3-second hardware PCM sampler to verify real microphone read capabilities.
 */
class DiagnosticsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnRefresh: Button
    private lateinit var tvCallScreening: TextView
    private lateinit var tvMicPermission: TextView
    private lateinit var tvNotifications: TextView
    private lateinit var tvAudioRecord: TextView
    private lateinit var tvAudioSignal: TextView
    private lateinit var tvSpeechRecognition: TextView
    private lateinit var tvLiveCallAudio: TextView
    private lateinit var tvAnalysisMode: TextView
    private lateinit var btnProbe: Button
    private lateinit var tvProbeResults: TextView

    private val mainHandler = Handler(Looper.getMainLooper())
    private var probeInput: MicrophoneAudioInput? = null
    private var isProbing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostics)

        btnBack = findViewById(R.id.btnBackDiagnostics)
        btnRefresh = findViewById(R.id.btnRefreshDiagnostics)
        tvCallScreening = findViewById(R.id.tvDiagCallScreening)
        tvMicPermission = findViewById(R.id.tvDiagMicPermission)
        tvNotifications = findViewById(R.id.tvDiagNotifications)
        tvAudioRecord = findViewById(R.id.tvDiagAudioRecord)
        tvAudioSignal = findViewById(R.id.tvDiagAudioSignal)
        tvSpeechRecognition = findViewById(R.id.tvDiagSpeechRecognition)
        tvLiveCallAudio = findViewById(R.id.tvDiagLiveCallAudio)
        tvAnalysisMode = findViewById(R.id.tvDiagAnalysisMode)
        btnProbe = findViewById(R.id.btnProbeMicrophone)
        tvProbeResults = findViewById(R.id.tvProbeResults)

        btnBack.setOnClickListener { finish() }
        btnRefresh.setOnClickListener { renderDiagnostics() }

        btnProbe.setOnClickListener {
            if (!isProbing) {
                runMicrophoneProbe()
            }
        }

        renderDiagnostics()
    }

    override fun onResume() {
        super.onResume()
        renderDiagnostics()
    }

    override fun onDestroy() {
        super.onDestroy()
        probeInput?.stop()
    }

    private fun renderDiagnostics() {
        val colorActive = ContextCompat.getColor(this, R.color.accent_green)
        val colorInactive = ContextCompat.getColor(this, R.color.accent_red)
        val colorWarning = ContextCompat.getColor(this, R.color.accent_amber)
        val colorMuted = ContextCompat.getColor(this, R.color.text_secondary)

        // 1. Call screening
        val isScreeningActive = PermissionHelper.isCallScreeningRoleHeld(this)
        tvCallScreening.text = if (isScreeningActive) "ACTIVE" else "INACTIVE"
        tvCallScreening.setTextColor(if (isScreeningActive) colorActive else colorWarning)

        // 2. Microphone permission
        val isMicGranted = PermissionHelper.isMicrophoneGranted(this)
        tvMicPermission.text = if (isMicGranted) "GRANTED" else "DENIED"
        tvMicPermission.setTextColor(if (isMicGranted) colorActive else colorInactive)

        // 3. Notifications
        val isNotifGranted = PermissionHelper.isNotificationGranted(this)
        tvNotifications.text = if (isNotifGranted) "GRANTED" else "DENIED"
        tvNotifications.setTextColor(if (isNotifGranted) colorActive else colorInactive)

        // 4. AudioRecord initialization capability
        val minBuf = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val isAudioRecordCapable = isMicGranted && minBuf > 0
        val isServiceAudioActive = CallMonitoringService.isServiceRunning && CallMonitoringService.latestAudioState == AudioInputState.ACTIVE
        tvAudioRecord.text = when {
            isServiceAudioActive -> "ACTIVE"
            isAudioRecordCapable -> "AVAILABLE"
            else -> "UNAVAILABLE"
        }
        tvAudioRecord.setTextColor(if (isAudioRecordCapable) colorActive else colorInactive)

        // 5. Audio signal
        val diag = CallMonitoringService.latestAudioDiagnostics
        val signalStatus = when {
            diag != null && diag.lastSignal == AudioSignal.SIGNAL_PRESENT -> "DETECTED"
            diag != null && diag.lastSignal == AudioSignal.SILENCE -> "SILENCE"
            else -> "UNAVAILABLE"
        }
        tvAudioSignal.text = signalStatus
        tvAudioSignal.setTextColor(
            when (signalStatus) {
                "DETECTED" -> colorActive
                "SILENCE" -> colorWarning
                else -> colorMuted
            }
        )

        // 6. Speech recognition
        val isSttAvailable = SpeechRecognizer.isRecognitionAvailable(this)
        tvSpeechRecognition.text = if (isSttAvailable) "AVAILABLE" else "UNAVAILABLE"
        tvSpeechRecognition.setTextColor(if (isSttAvailable) colorActive else colorWarning)

        // 7. Live call audio
        // On standard Android devices, cellular downlink is isolated by security policy
        tvLiveCallAudio.text = "PLATFORM LIMITED"
        tvLiveCallAudio.setTextColor(colorWarning)

        // 8. Analysis mode
        val mode = when {
            CallMonitoringService.isServiceRunning && CallMonitoringService.isDemoModeActive -> "DEMO"
            CallMonitoringService.isServiceRunning && !CallMonitoringService.isDemoModeActive -> "LIVE"
            else -> "IDLE"
        }
        tvAnalysisMode.text = mode
        tvAnalysisMode.setTextColor(
            when (mode) {
                "LIVE" -> colorActive
                "DEMO" -> colorWarning
                else -> colorMuted
            }
        )
    }

    private fun runMicrophoneProbe() {
        if (!PermissionHelper.isMicrophoneGranted(this)) {
            tvProbeResults.text = "Probe Error: RECORD_AUDIO permission is not granted. Please grant permission in Settings."
            return
        }

        isProbing = true
        btnProbe.isEnabled = false
        btnProbe.text = "Probing Physical Microphone (3s)..."
        tvProbeResults.text = "Initializing AudioRecord (16kHz 16-bit Mono PCM)...\nListening for physical acoustic vibrations..."

        probeInput = MicrophoneAudioInput()
        var totalSamples = 0L
        var nonZeroSamples = 0L
        var maxRms = 0.0
        var chunksCount = 0

        probeInput?.start { chunk: AudioChunk ->
            totalSamples += chunk.sampleCount
            nonZeroSamples += chunk.nonZeroSamples
            if (chunk.rmsLevel > maxRms) maxRms = chunk.rmsLevel
            chunksCount++

            mainHandler.post {
                tvProbeResults.text = buildString {
                    append("Physical Audio Probe In Progress:\n")
                    append("• Chunks Received: $chunksCount\n")
                    append("• Total Samples Read: $totalSamples\n")
                    append("• Non-Zero Samples: $nonZeroSamples\n")
                    append("• Current RMS Level: ${String.format("%.2f", chunk.rmsLevel)}\n")
                    append("• Peak RMS Level: ${String.format("%.2f", maxRms)}\n")
                    append("• Real-Time Signal: ${chunk.signal}")
                }
            }
        }

        // Stop after 3 seconds
        mainHandler.postDelayed({
            probeInput?.stop()
            val diag = probeInput?.getDiagnostics()
            isProbing = false
            btnProbe.isEnabled = true
            btnProbe.text = "Record 3-Second Physical Audio Sample"

            tvProbeResults.text = buildString {
                append("✓ Hardware Probe Complete (3000ms):\n")
                append("• Samples Read: $totalSamples\n")
                append("• Non-Zero Samples: $nonZeroSamples\n")
                append("• Peak RMS Level: ${String.format("%.2f", maxRms)}\n")
                val signalConclusion = if (nonZeroSamples > 0 && maxRms >= 25.0) {
                    "SIGNAL_PRESENT (Valid Acoustic Amplitude)"
                } else if (nonZeroSamples > 0) {
                    "LOW_AMPLITUDE (Near Noise Floor)"
                } else {
                    "SILENCE (Zero-Filled Buffers)"
                }
                append("• Final Signal Classification: $signalConclusion\n")
                append("• AudioRecord Hardware State: ${if (diag?.isInitialized == true) "INITIALIZED_OK" else "FAIL"}\n")
                append("• Source: MediaRecorder.AudioSource.MIC")
            }
            renderDiagnostics()
        }, 3000)
    }
}
