package com.svarax.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * MicrophoneAudioInput: Native AudioRecord implementation.
 *
 * Captures 16kHz, 16-bit Mono PCM audio, calculates per-chunk amplitude metrics
 * (sample count, non-zero sample count, RMS energy), and distinguishes genuine
 * acoustic signals from zero-filled silent buffers.
 */
class MicrophoneAudioInput : AudioInput {

    companion object {
        private const val TAG = "SvaraX_MicInput"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val RMS_SIGNAL_THRESHOLD = 25.0
        private const val CONSECUTIVE_SILENCE_LIMIT = 6 // ~1.5s of continuous silence/zeros
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)
    private var bufferSize = 0

    @Volatile
    private var currentState: AudioInputState = AudioInputState.UNAVAILABLE

    @Volatile
    private var totalSamplesRead: Long = 0L

    @Volatile
    private var totalNonZeroSamples: Long = 0L

    @Volatile
    private var currentRmsLevel: Double = 0.0

    @Volatile
    private var lastSignal: AudioSignal = AudioSignal.SILENCE

    @Volatile
    private var lastErrorMessage: String? = null

    private var consecutiveSilenceCount = 0

    init {
        try {
            bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            if (bufferSize < 4096) bufferSize = 4096
        } catch (e: Exception) {
            lastErrorMessage = e.message
            currentState = AudioInputState.ERROR
            Log.e(TAG, "Failed to getMinBufferSize for AudioRecord", e)
        }
    }

    @SuppressLint("MissingPermission")
    override fun start(onChunkReceived: (AudioChunk) -> Unit) {
        if (isRecording.get()) return

        currentState = AudioInputState.INITIALIZING
        totalSamplesRead = 0L
        totalNonZeroSamples = 0L
        currentRmsLevel = 0.0
        lastSignal = AudioSignal.SILENCE
        lastErrorMessage = null
        consecutiveSilenceCount = 0

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                lastErrorMessage = "AudioRecord failed to initialize (state != STATE_INITIALIZED)"
                currentState = AudioInputState.UNAVAILABLE
                Log.e(TAG, "AudioRecord initialized: FAILED ($lastErrorMessage)")
                return
            }

            Log.i(TAG, "AudioRecord initialized: SUCCESS | sampleRate=$SAMPLE_RATE, channels=1, encoding=PCM_16BIT, bufferSize=$bufferSize, audioSource=MIC")

            audioRecord?.startRecording()
            if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                lastErrorMessage = "AudioRecord failed to enter RECORDSTATE_RECORDING"
                currentState = AudioInputState.ERROR
                Log.e(TAG, "AudioRecord started: FAILED")
                return
            }

            isRecording.set(true)
            currentState = AudioInputState.ACTIVE
            Log.i(TAG, "AudioRecord started: SUCCESS")

            recordingThread = Thread({
                val buffer = ByteArray(bufferSize)
                while (isRecording.get()) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (bytesRead > 0) {
                        val sampleCount = bytesRead / 2
                        var nonZeroCount = 0
                        var sumSquares = 0.0

                        for (i in 0 until bytesRead step 2) {
                            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
                            if (sample.toInt() != 0) {
                                nonZeroCount++
                            }
                            val amp = sample.toDouble()
                            sumSquares += amp * amp
                        }

                        val rms = if (sampleCount > 0) Math.sqrt(sumSquares / sampleCount) else 0.0
                        totalSamplesRead += sampleCount
                        totalNonZeroSamples += nonZeroCount
                        currentRmsLevel = rms

                        // A buffer is only classified as SIGNAL_PRESENT if it contains non-zero samples
                        // AND has an RMS energy above the noise floor
                        val signal = if (nonZeroCount > 0 && rms >= RMS_SIGNAL_THRESHOLD) {
                            AudioSignal.SIGNAL_PRESENT
                        } else {
                            AudioSignal.SILENCE
                        }
                        lastSignal = signal

                        if (signal == AudioSignal.SILENCE) {
                            consecutiveSilenceCount++
                            if (consecutiveSilenceCount >= CONSECUTIVE_SILENCE_LIMIT) {
                                currentState = AudioInputState.NO_SIGNAL
                            }
                        } else {
                            consecutiveSilenceCount = 0
                            currentState = AudioInputState.ACTIVE
                        }

                        Log.d(
                            TAG,
                            "AudioRecord chunk: samplesRead=$sampleCount, nonZeroSamples=$nonZeroCount, rms=${String.format("%.2f", rms)}, signal=$signal"
                        )

                        val chunkData = buffer.copyOf(bytesRead)
                        onChunkReceived(
                            AudioChunk(
                                data = chunkData,
                                timestamp = System.currentTimeMillis(),
                                sampleRate = SAMPLE_RATE,
                                sampleCount = sampleCount,
                                nonZeroSamples = nonZeroCount,
                                rmsLevel = rms,
                                signal = signal
                            )
                        )
                    } else if (bytesRead < 0) {
                        lastErrorMessage = "AudioRecord.read returned error code: $bytesRead"
                        currentState = AudioInputState.ERROR
                        Log.w(TAG, "AudioRecord read error: $bytesRead")
                    }
                }
            }, "SvaraX-AudioRecord-Thread").apply {
                priority = Thread.NORM_PRIORITY + 1
                start()
            }
        } catch (e: Exception) {
            lastErrorMessage = e.message
            currentState = AudioInputState.ERROR
            Log.e(TAG, "Exception starting AudioRecord", e)
            isRecording.set(false)
        }
    }

    override fun stop() {
        if (!isRecording.get()) return
        isRecording.set(false)

        try {
            recordingThread?.interrupt()
            recordingThread = null

            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            currentState = AudioInputState.UNAVAILABLE
            Log.i(TAG, "AudioRecord stopped cleanly")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
    }

    override fun isCapturing(): Boolean = isRecording.get()

    override fun getState(): AudioInputState = currentState

    override fun getDiagnostics(): AudioDiagnostics {
        return AudioDiagnostics(
            isInitialized = audioRecord?.state == AudioRecord.STATE_INITIALIZED,
            sampleRate = SAMPLE_RATE,
            channelCount = 1,
            encoding = "PCM_16BIT",
            audioSource = "MediaRecorder.AudioSource.MIC",
            samplesRead = totalSamplesRead,
            nonZeroSamples = totalNonZeroSamples,
            rmsLevel = currentRmsLevel,
            lastSignal = lastSignal,
            lastError = lastErrorMessage
        )
    }
}
