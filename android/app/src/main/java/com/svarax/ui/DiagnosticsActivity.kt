package com.svarax.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.svarax.R
import com.svarax.audio.AudioChunk
import com.svarax.audio.AudioInputState
import com.svarax.audio.AudioSignal
import com.svarax.audio.MicrophoneAudioInput
import com.svarax.permission.PermissionHelper
import com.svarax.service.CallMonitoringService
import java.util.Locale

/**
 * DiagnosticsActivity: Physical Hardware and Subsystem Diagnostics Screen.
 *
 * Implements two completely decoupled diagnostic tests:
 * 1. AUDIO RECORD TEST: Verifies Microphone -> AudioRecord -> 16kHz 16-bit Mono PCM.
 *    Supports selectable durations (5s, 10s, 30s default, 60s, custom 1-300s),
 *    live real-time RMS, sample counters, peak RMS calculation, manual STOP, and auto-completion.
 * 2. SPEECH RECOGNITION TEST: Verifies Microphone -> Android SpeechRecognizer -> Transcript.
 *    Operates independently without arbitrary duration cutoffs.
 */
class DiagnosticsActivity : AppCompatActivity() {

    companion object {
        private const val TAG_MIC = "SvaraX_MicInput"
        private const val TAG_SPEECH = "SvaraX_SpeechDiag"
        private const val REQ_PERMISSION_MIC_AUDIO = 201
        private const val REQ_PERMISSION_MIC_SPEECH = 202

        // Duration options in seconds
        private val DURATION_LABELS = arrayOf(
            "5 seconds",
            "10 seconds",
            "30 seconds (Default)",
            "60 seconds",
            "Custom duration (1–300s)..."
        )
        private val DURATION_VALUES = intArrayOf(5, 10, 30, 60, -1)
    }

    // Top Bar & Physical Matrix Views
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

    // Audio Record Test Views
    private lateinit var spinnerAudioDuration: Spinner
    private lateinit var layoutCustomDuration: LinearLayout
    private lateinit var etCustomDuration: EditText
    private lateinit var btnStartAudioRecord: Button
    private lateinit var btnStopAudioRecord: Button
    private lateinit var tvLiveTimer: TextView
    private lateinit var tvAudioRecordState: TextView
    private lateinit var tvLiveCurrentRms: TextView
    private lateinit var tvLivePeakRms: TextView
    private lateinit var tvLiveSamplesRead: TextView
    private lateinit var tvLiveNonZeroSamples: TextView
    private lateinit var tvLiveSignalState: TextView
    private lateinit var tvAudioRecordResults: TextView

    // Speech Recognition Test Views
    private lateinit var btnStartSpeechTest: Button
    private lateinit var btnStopSpeechTest: Button
    private lateinit var tvSpeechStatus: TextView
    private lateinit var tvPartialTranscript: TextView
    private lateinit var tvFinalTranscript: TextView

    // Audio Record Test State
    private var probeInput: MicrophoneAudioInput? = null
    private var isAudioRecordActive = false
    private var targetDurationSeconds = 30
    private var testStartRealtimeMs = 0L
    private var testElapsedMs = 0L

    // Incremental metrics (O(1) memory usage — no giant PCM array retained)
    private var totalSamplesRead = 0L
    private var totalNonZeroSamples = 0L
    private var currentRms = 0.0
    private var peakRms = 0.0
    private var sumRms = 0.0
    private var chunksCount = 0L
    private var hasSignalDetected = false

    // Speech Recognition Test State
    private var speechRecognizer: SpeechRecognizer? = null
    private var isSpeechTestActive = false
    private var cumulativeTranscript = ""

    private val mainHandler = Handler(Looper.getMainLooper())

    // Timer updater runnable for smooth live countdown display
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isAudioRecordActive) {
                val now = SystemClock.elapsedRealtime()
                testElapsedMs = now - testStartRealtimeMs
                val targetMs = targetDurationSeconds * 1000L

                val currentSec = (testElapsedMs / 1000L).coerceAtMost(targetDurationSeconds.toLong())
                val targetSec = targetDurationSeconds.toLong()

                val timeStr = String.format(
                    Locale.US,
                    "Recording\n%02d:%02d / %02d:%02d",
                    currentSec / 60, currentSec % 60,
                    targetSec / 60, targetSec % 60
                )
                tvLiveTimer.text = timeStr

                if (testElapsedMs >= targetMs) {
                    completeAudioRecordTest(isUserInitiated = false)
                    return
                }

                mainHandler.postDelayed(this, 100)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostics)

        initViews()
        setupDurationSpinner()
        setupAudioRecordListeners()
        setupSpeechTestListeners()

        renderDiagnostics()
    }

    override fun onResume() {
        super.onResume()
        renderDiagnostics()
    }

    override fun onPause() {
        super.onPause()
        // Never leave AudioRecord or SpeechRecognizer active when backgrounded/navigating away
        if (isAudioRecordActive) {
            stopAudioRecordTest(isUserInitiated = false, isCancelled = true)
        }
        if (isSpeechTestActive) {
            stopSpeechTest()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioRecordTest(isUserInitiated = false, isCancelled = true)
        stopSpeechTest()
    }

    private fun initViews() {
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

        spinnerAudioDuration = findViewById(R.id.spinnerAudioDuration)
        layoutCustomDuration = findViewById(R.id.layoutCustomDuration)
        etCustomDuration = findViewById(R.id.etCustomDuration)
        btnStartAudioRecord = findViewById(R.id.btnStartAudioRecord)
        btnStopAudioRecord = findViewById(R.id.btnStopAudioRecord)
        tvLiveTimer = findViewById(R.id.tvLiveTimer)
        tvAudioRecordState = findViewById(R.id.tvAudioRecordState)
        tvLiveCurrentRms = findViewById(R.id.tvLiveCurrentRms)
        tvLivePeakRms = findViewById(R.id.tvLivePeakRms)
        tvLiveSamplesRead = findViewById(R.id.tvLiveSamplesRead)
        tvLiveNonZeroSamples = findViewById(R.id.tvLiveNonZeroSamples)
        tvLiveSignalState = findViewById(R.id.tvLiveSignalState)
        tvAudioRecordResults = findViewById(R.id.tvAudioRecordResults)

        btnStartSpeechTest = findViewById(R.id.btnStartSpeechTest)
        btnStopSpeechTest = findViewById(R.id.btnStopSpeechTest)
        tvSpeechStatus = findViewById(R.id.tvSpeechStatus)
        tvPartialTranscript = findViewById(R.id.tvPartialTranscript)
        tvFinalTranscript = findViewById(R.id.tvFinalTranscript)

        btnBack.setOnClickListener { finish() }
        btnRefresh.setOnClickListener { renderDiagnostics() }
    }

    private fun setupDurationSpinner() {
        val adapter = ArrayAdapter(
            this,
            R.layout.item_spinner_duration,
            R.id.tvSpinnerItem,
            DURATION_LABELS
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_duration_dropdown)
        }

        spinnerAudioDuration.adapter = adapter
        // Default to index 2 ("30 seconds (Default)")
        spinnerAudioDuration.setSelection(2)

        spinnerAudioDuration.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 4) { // Custom duration
                    layoutCustomDuration.visibility = View.VISIBLE
                    targetDurationSeconds = parseCustomDuration()
                } else {
                    layoutCustomDuration.visibility = View.GONE
                    targetDurationSeconds = DURATION_VALUES[position]
                }
                tvLiveTimer.text = String.format(
                    Locale.US,
                    "Ready: 00:00 / %02d:%02d",
                    targetDurationSeconds / 60, targetDurationSeconds % 60
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun parseCustomDuration(): Int {
        val input = etCustomDuration.text.toString().trim()
        val parsed = input.toIntOrNull() ?: 30
        return parsed.coerceIn(1, 300)
    }

    private fun setupAudioRecordListeners() {
        btnStartAudioRecord.setOnClickListener {
            if (isAudioRecordActive) return@setOnClickListener

            // Check if Speech Test is running to avoid mic session contention
            if (isSpeechTestActive) {
                Toast.makeText(
                    this,
                    "Speech Recognition test is active. Please stop it before starting AudioRecord test.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Check permission
            if (!PermissionHelper.isMicrophoneGranted(this)) {
                Toast.makeText(this, "Microphone permission is required for this test.", Toast.LENGTH_SHORT).show()
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_PERMISSION_MIC_AUDIO)
                return@setOnClickListener
            }

            // If custom duration was selected, validate and parse it now
            if (spinnerAudioDuration.selectedItemPosition == 4) {
                targetDurationSeconds = parseCustomDuration()
                etCustomDuration.setText(targetDurationSeconds.toString())
            }

            startAudioRecordTest()
        }

        btnStopAudioRecord.setOnClickListener {
            if (isAudioRecordActive) {
                completeAudioRecordTest(isUserInitiated = true)
            }
        }
    }

    private fun startAudioRecordTest() {
        isAudioRecordActive = true
        testStartRealtimeMs = SystemClock.elapsedRealtime()
        testElapsedMs = 0L

        // Reset metrics
        totalSamplesRead = 0L
        totalNonZeroSamples = 0L
        currentRms = 0.0
        peakRms = 0.0
        sumRms = 0.0
        chunksCount = 0L
        hasSignalDetected = false

        // Update UI for active recording
        btnStartAudioRecord.visibility = View.GONE
        btnStopAudioRecord.visibility = View.VISIBLE
        spinnerAudioDuration.isEnabled = false
        etCustomDuration.isEnabled = false

        tvAudioRecordState.text = "● ACTIVE"
        tvAudioRecordState.setTextColor(ContextCompat.getColor(this, R.color.accent_green))

        tvLiveCurrentRms.text = "0"
        tvLivePeakRms.text = "0"
        tvLiveSamplesRead.text = "0"
        tvLiveNonZeroSamples.text = "0"
        tvLiveSignalState.text = "● INITIALIZING"
        tvLiveSignalState.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

        tvAudioRecordResults.text = "Recording in progress... Speak into microphone to test acoustic capture."

        // Initialize and start AudioRecord
        probeInput = MicrophoneAudioInput()
        probeInput?.start { chunk: AudioChunk ->
            // Incrementally compute metrics without buffering raw PCM data (O(1) memory)
            totalSamplesRead += chunk.sampleCount
            totalNonZeroSamples += chunk.nonZeroSamples
            currentRms = chunk.rmsLevel
            if (chunk.rmsLevel > peakRms) {
                peakRms = chunk.rmsLevel
            }
            sumRms += chunk.rmsLevel
            chunksCount++

            if (chunk.signal == AudioSignal.SIGNAL_PRESENT && chunk.rmsLevel >= 25.0) {
                hasSignalDetected = true
            }

            // Log individual chunk according to Requirement 11
            Log.d(
                TAG_MIC,
                "samplesRead=${chunk.sampleCount}, nonZeroSamples=${chunk.nonZeroSamples}, rms=${String.format(Locale.US, "%.1f", chunk.rmsLevel)}, signal=${chunk.signal}"
            )

            mainHandler.post {
                if (isAudioRecordActive) {
                    tvLiveCurrentRms.text = String.format(Locale.US, "%,d", currentRms.toInt())
                    tvLivePeakRms.text = String.format(Locale.US, "%,d", peakRms.toInt())
                    tvLiveSamplesRead.text = String.format(Locale.US, "%,d", totalSamplesRead)

                    val nonZeroPercent = if (totalSamplesRead > 0) {
                        (totalNonZeroSamples.toDouble() / totalSamplesRead) * 100.0
                    } else 0.0
                    tvLiveNonZeroSamples.text = String.format(Locale.US, "%,d (%.1f%%)", totalNonZeroSamples, nonZeroPercent)

                    if (chunk.signal == AudioSignal.SIGNAL_PRESENT) {
                        tvLiveSignalState.text = "● SIGNAL PRESENT"
                        tvLiveSignalState.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
                    } else {
                        tvLiveSignalState.text = "● SILENCE"
                        tvLiveSignalState.setTextColor(ContextCompat.getColor(this, R.color.accent_amber))
                    }
                }
            }
        }

        // Start UI countdown timer
        mainHandler.post(timerRunnable)
        renderDiagnostics()
    }

    private fun completeAudioRecordTest(isUserInitiated: Boolean) {
        if (!isAudioRecordActive) return
        mainHandler.removeCallbacks(timerRunnable)

        val durationMs = SystemClock.elapsedRealtime() - testStartRealtimeMs
        val actualSeconds = durationMs / 1000.0

        // Safely stop AudioRecord and release all native resources
        probeInput?.stop()
        val diag = probeInput?.getDiagnostics()
        probeInput = null
        isAudioRecordActive = false

        // Update UI controls
        btnStartAudioRecord.visibility = View.VISIBLE
        btnStopAudioRecord.visibility = View.GONE
        spinnerAudioDuration.isEnabled = true
        etCustomDuration.isEnabled = true

        tvAudioRecordState.text = if (isUserInitiated) "● STOPPED" else "● COMPLETE"
        tvAudioRecordState.setTextColor(
            ContextCompat.getColor(
                this,
                if (isUserInitiated) R.color.accent_amber else R.color.accent_green
            )
        )

        val avgRms = if (chunksCount > 0) sumRms / chunksCount else 0.0
        val isWorking = (diag?.isInitialized == true) && (totalSamplesRead > 0)

        // Log completion in exact requested format (Requirement 11)
        Log.i(
            TAG_MIC,
            """
            Recording complete
            durationMs=$durationMs
            totalSamples=$totalSamplesRead
            nonZeroSamples=$totalNonZeroSamples
            peakRms=${String.format(Locale.US, "%.1f", peakRms)}
            averageRms=${String.format(Locale.US, "%.1f", avgRms)}
            signalDetected=$hasSignalDetected
            """.trimIndent()
        )

        // Display formatted final results (Requirement 4 & 5)
        tvAudioRecordResults.text = buildString {
            if (isUserInitiated) {
                append("TEST STOPPED BY USER\n\n")
            } else {
                append("TEST COMPLETE ✓\n\n")
            }
            append("Duration:\n${String.format(Locale.US, "%.1f", actualSeconds)} sec\n\n")
            append("Samples:\n${String.format(Locale.US, "%,d", totalSamplesRead)}\n\n")
            val nonZeroPercent = if (totalSamplesRead > 0) {
                (totalNonZeroSamples.toDouble() / totalSamplesRead) * 100.0
            } else 0.0
            append("Non-zero samples:\n${String.format(Locale.US, "%,d (%.1f%%)", totalNonZeroSamples, nonZeroPercent)}\n\n")
            append("Peak RMS:\n${String.format(Locale.US, "%,d", peakRms.toInt())}\n\n")
            append("Signal detected:\n${if (hasSignalDetected) "YES" else "NO"}\n\n")
            append("AudioRecord:\n${if (isWorking) "WORKING" else "UNAVAILABLE"}")
        }

        renderDiagnostics()
    }

    private fun stopAudioRecordTest(isUserInitiated: Boolean, isCancelled: Boolean) {
        if (!isAudioRecordActive) return
        mainHandler.removeCallbacks(timerRunnable)
        probeInput?.stop()
        probeInput = null
        isAudioRecordActive = false

        btnStartAudioRecord.visibility = View.VISIBLE
        btnStopAudioRecord.visibility = View.GONE
        spinnerAudioDuration.isEnabled = true
        etCustomDuration.isEnabled = true

        tvAudioRecordState.text = "● IDLE"
        tvAudioRecordState.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

        if (!isCancelled) {
            tvAudioRecordResults.text = "Diagnostic session terminated."
        }
    }

    // ================= SPEECH RECOGNITION TEST IMPLEMENTATION =================

    private fun setupSpeechTestListeners() {
        btnStartSpeechTest.setOnClickListener {
            if (isSpeechTestActive) return@setOnClickListener

            // Check if AudioRecord test is running to prevent hardware conflict
            if (isAudioRecordActive) {
                Toast.makeText(
                    this,
                    "AudioRecord test is active. Please stop it before starting Speech Recognition test.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Check permission
            if (!PermissionHelper.isMicrophoneGranted(this)) {
                Toast.makeText(this, "Microphone permission is required for this test.", Toast.LENGTH_SHORT).show()
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_PERMISSION_MIC_SPEECH)
                return@setOnClickListener
            }

            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                Toast.makeText(this, "Speech recognition service is not available on this device.", Toast.LENGTH_LONG).show()
                tvSpeechStatus.text = "SERVICE UNAVAILABLE"
                tvSpeechStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_red))
                return@setOnClickListener
            }

            startSpeechTest()
        }

        btnStopSpeechTest.setOnClickListener {
            if (isSpeechTestActive) {
                stopSpeechTest()
            }
        }
    }

    private fun startSpeechTest() {
        isSpeechTestActive = true
        cumulativeTranscript = ""

        btnStartSpeechTest.visibility = View.GONE
        btnStopSpeechTest.visibility = View.VISIBLE

        tvSpeechStatus.text = "INITIALIZING..."
        tvSpeechStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_amber))
        tvPartialTranscript.text = "Listening for speech..."
        tvFinalTranscript.text = "Speak naturally: \"Hello, this is a Svara X microphone test...\""

        initAndStartSpeechRecognizer()
    }

    private fun initAndStartSpeechRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG_SPEECH, "SpeechRecognizer onReadyForSpeech")
                        if (isSpeechTestActive) {
                            tvSpeechStatus.text = "LISTENING..."
                            tvSpeechStatus.setTextColor(ContextCompat.getColor(this@DiagnosticsActivity, R.color.accent_green))
                        }
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG_SPEECH, "SpeechRecognizer onBeginningOfSpeech")
                        if (isSpeechTestActive) {
                            tvSpeechStatus.text = "SPEECH DETECTED..."
                            tvSpeechStatus.setTextColor(ContextCompat.getColor(this@DiagnosticsActivity, R.color.accent_green))
                        }
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        Log.d(TAG_SPEECH, "SpeechRecognizer onEndOfSpeech")
                        if (isSpeechTestActive) {
                            tvSpeechStatus.text = "PROCESSING SPEECH..."
                        }
                    }

                    override fun onError(error: Int) {
                        Log.w(TAG_SPEECH, "SpeechRecognizer onError: $error")
                        if (isSpeechTestActive) {
                            // If timeout or no match occurred while still listening, automatically loop so user can keep testing
                            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                                restartSpeechRecognition()
                            } else {
                                val errorMsg = when (error) {
                                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording conflict"
                                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
                                    SpeechRecognizer.ERROR_NETWORK -> "Network required for speech"
                                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                                    else -> "SpeechRecognizer code $error"
                                }
                                tvSpeechStatus.text = errorMsg
                                tvSpeechStatus.setTextColor(ContextCompat.getColor(this@DiagnosticsActivity, R.color.accent_amber))
                                // Try recovering if not permanently broken
                                mainHandler.postDelayed({
                                    if (isSpeechTestActive) restartSpeechRecognition()
                                }, 1500)
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognized = matches[0]
                            Log.i(TAG_SPEECH, "Speech recognized: \"$recognized\"")
                            cumulativeTranscript = if (cumulativeTranscript.isBlank()) {
                                recognized
                            } else {
                                "$cumulativeTranscript $recognized"
                            }
                            tvFinalTranscript.text = "\"$cumulativeTranscript\""
                            tvPartialTranscript.text = "—"
                        }

                        // Continue listening across multiple phrases until user stops
                        if (isSpeechTestActive) {
                            restartSpeechRecognition()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!partials.isNullOrEmpty()) {
                            tvPartialTranscript.text = "\"${partials[0]}\""
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
            Log.i(TAG_SPEECH, "SpeechRecognizer startListening invoked")
        } catch (e: Exception) {
            Log.e(TAG_SPEECH, "Exception launching SpeechRecognizer", e)
            tvSpeechStatus.text = "FAILED: ${e.message}"
            tvSpeechStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_red))
        }
    }

    private fun restartSpeechRecognition() {
        if (!isSpeechTestActive) return
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG_SPEECH, "Error restarting SpeechRecognizer", e)
            }
        }
    }

    private fun stopSpeechTest() {
        isSpeechTestActive = false
        btnStartSpeechTest.visibility = View.VISIBLE
        btnStopSpeechTest.visibility = View.GONE

        tvSpeechStatus.text = "TEST COMPLETE"
        tvSpeechStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        tvPartialTranscript.text = "—"

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            Log.i(TAG_SPEECH, "SpeechRecognizer destroyed cleanly")
        } catch (e: Exception) {
            Log.e(TAG_SPEECH, "Error destroying SpeechRecognizer", e)
        }
    }

    // ================= PHYSICAL MATRIX STATUS RENDERING =================

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

        // 4. AudioRecord capability
        val minBuf = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val isAudioRecordCapable = isMicGranted && minBuf > 0
        val isServiceAudioActive = CallMonitoringService.isServiceRunning && CallMonitoringService.latestAudioState == AudioInputState.ACTIVE
        tvAudioRecord.text = when {
            isAudioRecordActive || isServiceAudioActive -> "ACTIVE"
            isAudioRecordCapable -> "AVAILABLE"
            else -> "UNAVAILABLE"
        }
        tvAudioRecord.setTextColor(if (isAudioRecordCapable) colorActive else colorInactive)

        // 5. Audio signal
        val diag = CallMonitoringService.latestAudioDiagnostics
        val signalStatus = when {
            isAudioRecordActive && hasSignalDetected -> "DETECTED"
            diag != null && diag.lastSignal == AudioSignal.SIGNAL_PRESENT -> "DETECTED"
            diag != null && diag.lastSignal == AudioSignal.SILENCE -> "SILENCE"
            else -> if (isMicGranted) "READY" else "UNAVAILABLE"
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
        tvSpeechRecognition.text = when {
            isSpeechTestActive -> "ACTIVE"
            isSttAvailable -> "AVAILABLE"
            else -> "UNAVAILABLE"
        }
        tvSpeechRecognition.setTextColor(if (isSttAvailable) colorActive else colorWarning)

        // 7. Live call audio (Platform limitation: cellular downlink isolated by OS)
        tvLiveCallAudio.text = "PLATFORM LIMITED"
        tvLiveCallAudio.setTextColor(colorWarning)

        // 8. Analysis mode
        val mode = when {
            isAudioRecordActive -> "AUDIO_TEST"
            isSpeechTestActive -> "SPEECH_TEST"
            CallMonitoringService.isServiceRunning && CallMonitoringService.isDemoModeActive -> "DEMO"
            CallMonitoringService.isServiceRunning && !CallMonitoringService.isDemoModeActive -> "LIVE"
            else -> "IDLE"
        }
        tvAnalysisMode.text = mode
        tvAnalysisMode.setTextColor(
            when (mode) {
                "LIVE", "AUDIO_TEST", "SPEECH_TEST" -> colorActive
                "DEMO" -> colorWarning
                else -> colorMuted
            }
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        renderDiagnostics()

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission granted.", Toast.LENGTH_SHORT).show()
            if (requestCode == REQ_PERMISSION_MIC_AUDIO) {
                startAudioRecordTest()
            } else if (requestCode == REQ_PERMISSION_MIC_SPEECH) {
                startSpeechTest()
            }
        } else {
            Toast.makeText(this, "Microphone permission is required to run this diagnostic test.", Toast.LENGTH_LONG).show()
        }
    }
}
