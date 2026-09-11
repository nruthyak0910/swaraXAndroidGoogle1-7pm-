package com.svarax.speech

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * DemoSpeechToText: Isolated Demonstration Pipeline.
 *
 * Emits progressive conversational dialogue strictly when DEMO mode is enabled in Settings.
 * NEVER instantiated or connected during real cellular calls.
 */
class DemoSpeechToText : SpeechToText {

    companion object {
        private const val TAG = "SvaraX_DemoSTT"
    }

    override val engineName: String = "DemoSpeechToText (Simulation Only)"
    override val consumesPcmDirectly: Boolean = false

    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var chunkIndex = 0
    private var callback: ((TranscriptChunk) -> Unit)? = null

    private val demoPhrases = listOf(
        "Hello, I am calling from your bank branch security division.",
        "Your account will be blocked within thirty minutes due to suspicious transactions.",
        "Please read back the 6-digit OTP you received on your mobile phone to prevent cancellation."
    )

    override fun isAvailable(): Boolean = true

    override fun start(onTranscript: (TranscriptChunk) -> Unit, onError: (String) -> Unit) {
        if (isRunning) return
        isRunning = true
        this.callback = onTranscript
        chunkIndex = 0
        Log.i(TAG, "DemoSpeechToText started for controlled test demonstration")
        scheduleNextChunk()
    }

    private fun scheduleNextChunk() {
        if (!isRunning || chunkIndex >= demoPhrases.size) return

        handler.postDelayed({
            if (isRunning && chunkIndex < demoPhrases.size) {
                val phrase = demoPhrases[chunkIndex]
                chunkIndex++
                Log.d(TAG, "Demo chunk emitted ($chunkIndex/${demoPhrases.size}): \"$phrase\"")
                callback?.invoke(
                    TranscriptChunk(
                        text = phrase,
                        isFinal = true,
                        confidence = 0.98f
                    )
                )
                scheduleNextChunk()
            }
        }, 2800)
    }

    override fun stop() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        Log.i(TAG, "DemoSpeechToText stopped")
    }

    override fun isRunning(): Boolean = isRunning
}
