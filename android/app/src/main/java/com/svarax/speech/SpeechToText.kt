package com.svarax.speech

import com.svarax.audio.AudioChunk

/**
 * Priority 6 & 10: Speech-To-Text interface.
 * Modular design allowing Android SpeechRecognizer, backend STT, or demo fallback.
 */
interface SpeechToText {
    fun start(onTranscript: (TranscriptChunk) -> Unit, onError: (String) -> Unit)
    fun processAudioChunk(chunk: AudioChunk)
    fun stop()
    fun isRunning(): Boolean
}
