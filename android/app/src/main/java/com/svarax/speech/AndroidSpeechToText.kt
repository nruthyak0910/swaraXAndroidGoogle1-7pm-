package com.svarax.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.svarax.audio.AudioChunk
import java.util.Locale

/**
 * Priority 10: Native Android Speech-to-Text Implementation.
 * Uses Android SpeechRecognizer to transcribe speakerphone audio during controlled live calls.
 */
class AndroidSpeechToText(private val context: Context) : SpeechToText {

    companion object {
        private const val TAG = "SvaraX_AndroidSTT"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var callback: ((TranscriptChunk) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun start(onTranscript: (TranscriptChunk) -> Unit, onError: (String) -> Unit) {
        if (isListening) return
        this.callback = onTranscript
        this.errorCallback = onError

        mainHandler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.w(TAG, "SpeechRecognizer is not available on this device")
                    errorCallback?.invoke("Native speech recognition service unavailable")
                    return@post
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speechRecognizer?.startListening(intent)
                isListening = true
                Log.i(TAG, "SpeechRecognizer listening started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognition", e)
                errorCallback?.invoke("Failed to start speech recognition: ${e.message}")
            }
        }
    }

    override fun processAudioChunk(chunk: AudioChunk) {
        // Native SpeechRecognizer listens via audio record session;
        // AudioChunk can also be routed to streaming backend if required.
    }

    override fun stop() {
        if (!isListening) return
        isListening = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
                Log.i(TAG, "SpeechRecognizer stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognition", e)
            }
        }
    }

    override fun isRunning(): Boolean = isListening

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                Log.w(TAG, "SpeechRecognizer error code: $error")
                // If recognizer pauses or times out during active call, restart listening
                if (isListening && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                    restartListening()
                } else {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        else -> "Unknown error ($error)"
                    }
                    errorCallback?.invoke(msg)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    callback?.invoke(TranscriptChunk(text = text, isFinal = true))
                }
                if (isListening) {
                    restartListening()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    callback?.invoke(TranscriptChunk(text = text, isFinal = false))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun restartListening() {
        mainHandler.postDelayed({
            if (isListening && speechRecognizer != null) {
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    }
                    speechRecognizer?.startListening(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error restarting speech recognition", e)
                }
            }
        }, 300)
    }
}
