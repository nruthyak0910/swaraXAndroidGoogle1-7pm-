package com.svarax.speech

import android.os.Handler
import android.os.Looper
import com.svarax.audio.AudioChunk

/**
 * Priority 6 & 18: Hackathon Demo & Offline Fallback STT pipeline.
 * Simulates progressive real-time speech chunks for the "Bank OTP Scam" scenario.
 */
class DemoSpeechToText : SpeechToText {

    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var chunkIndex = 0
    private var callback: ((TranscriptChunk) -> Unit)? = null

    private val demoPhrases = listOf(
        "Hello, I am calling from your bank.",
        "Your account will be blocked today.",
        "Please tell me the OTP you received."
    )

    override fun start(onTranscript: (TranscriptChunk) -> Unit, onError: (String) -> Unit) {
        if (isRunning) return
        isRunning = true
        this.callback = onTranscript
        chunkIndex = 0
        scheduleNextChunk()
    }

    private fun scheduleNextChunk() {
        if (!isRunning || chunkIndex >= demoPhrases.size) return

        handler.postDelayed({
            if (isRunning && chunkIndex < demoPhrases.size) {
                val phrase = demoPhrases[chunkIndex]
                chunkIndex++
                callback?.invoke(
                    TranscriptChunk(
                        text = phrase,
                        isFinal = true,
                        confidence = 0.98f
                    )
                )
                scheduleNextChunk()
            }
        }, 3000) // Emit a new conversational chunk every 3 seconds
    }

    override fun processAudioChunk(chunk: AudioChunk) {
        // In demo mode, predefined script plays chronologically
    }

    override fun stop() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    override fun isRunning(): Boolean = isRunning
}
