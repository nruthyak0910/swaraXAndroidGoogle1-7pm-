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
import java.util.Locale

/**
 * AndroidSpeechToText: Native Android SpeechRecognizer STT Implementation.
 *
 * ARCHITECTURAL NOTICE:
 * Android's built-in SpeechRecognizer (Google Speech Services / On-Device Speech Recognizer)
 * manages its own internal OS audio capture session (MediaRecorder.AudioSource.VOICE_RECOGNITION).
 * It DOES NOT accept raw AudioRecord PCM buffers.
 *
 * Therefore, [consumesPcmDirectly] is explicitly FALSE.
 */
class AndroidSpeechToText(private val context: Context) : SpeechToText {

    companion object {
        private const val TAG = "SvaraX_AndroidSTT"
    }

    override val engineName: String = "Android System SpeechRecognizer"
    override val consumesPcmDirectly: Boolean = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var callback: ((TranscriptChunk) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    override fun start(onTranscript: (TranscriptChunk) -> Unit, onError: (String) -> Unit) {
        if (isListening) return
        this.callback = onTranscript
        this.errorCallback = onError

        mainHandler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.w(TAG, "SpeechRecognizer is not available on this Android device")
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
                Log.i(TAG, "SpeechRecognizer listening started via system recognition intent")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognition", e)
                errorCallback?.invoke("Failed to start speech recognition: ${e.message}")
            }
        }
    }

    override fun stop() {
        if (!isListening) return
        isListening = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
                Log.i(TAG, "SpeechRecognizer stopped and resources released")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognition", e)
            }
        }
    }

    override fun isRunning(): Boolean = isListening

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "SpeechRecognizer: onReadyForSpeech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "SpeechRecognizer: onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "SpeechRecognizer: onEndOfSpeech")
            }

            override fun onError(error: Int) {
                Log.w(TAG, "SpeechRecognizer error code: $error")
                // If recognizer pauses or times out during active call, restart listening
                if (isListening && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                    restartListening()
                } else {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording conflict / mic unavailable during call"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient audio permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network connection required for recognition"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        else -> "SpeechRecognizer error ($error)"
                    }
                    errorCallback?.invoke(msg)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    Log.i(TAG, "FINAL_TRANSCRIPT_RECEIVED length=${text.length}")
                    Log.d("SvaraX_TranscriptPipeline", "TRANSCRIPT_CHUNK_CREATED")
                    val chunk = TranscriptChunk(text = text, isFinal = true, timestamp = System.currentTimeMillis())
                    Log.d("SvaraX_TranscriptPipeline", "TRANSCRIPT_CHUNK_DISPATCHED")
                    callback?.invoke(chunk)
                }
                if (isListening) {
                    restartListening()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    Log.d(TAG, "SpeechRecognizer partial text: \"$text\"")
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
