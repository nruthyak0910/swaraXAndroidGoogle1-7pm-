package com.svarax.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Priority 10: Live Microphone Audio Acquisition.
 * Used for the controlled speakerphone demonstration during SIH.
 * Captures audio in chunks (16kHz, 16-bit PCM mono) and delivers them to the pipeline.
 */
class MicrophoneAudioInput : AudioInput {

    companion object {
        private const val TAG = "SvaraX_MicInput"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)
    private var bufferSize = 0

    init {
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize < 4096) bufferSize = 4096
    }

    @SuppressLint("MissingPermission")
    override fun start(onChunkReceived: (AudioChunk) -> Unit) {
        if (isRecording.get()) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                return
            }

            audioRecord?.startRecording()
            isRecording.set(true)
            Log.i(TAG, "Microphone recording started for speakerphone pipeline")

            recordingThread = Thread({
                val buffer = ByteArray(bufferSize)
                while (isRecording.get()) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        val chunkData = buffer.copyOf(read)
                        onChunkReceived(
                            AudioChunk(
                                data = chunkData,
                                sampleRate = SAMPLE_RATE
                            )
                        )
                    }
                }
            }, "SvaraX-AudioRecord-Thread").apply {
                priority = Thread.NORM_PRIORITY + 1
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AudioRecord", e)
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
            Log.i(TAG, "Microphone recording stopped cleanly")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
    }

    override fun isCapturing(): Boolean = isRecording.get()
}
