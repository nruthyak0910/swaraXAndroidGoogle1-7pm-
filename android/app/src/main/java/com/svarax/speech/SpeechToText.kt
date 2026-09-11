package com.svarax.speech

import com.svarax.audio.AudioChunk

/**
 * SpeechToText: Base abstraction for speech recognition engines.
 * Explicitly states whether this engine consumes raw PCM audio chunks directly
 * or manages its own OS-level audio session (e.g. Android SpeechRecognizer).
 */
interface SpeechToText {
    val engineName: String
    val consumesPcmDirectly: Boolean

    fun start(onTranscript: (TranscriptChunk) -> Unit, onError: (String) -> Unit)
    fun stop()
    fun isRunning(): Boolean
    fun isAvailable(): Boolean
}

/**
 * PcmConsumerSpeechToText: Sub-interface for STT models that ingest raw PCM chunks directly
 * (e.g. on-device acoustic models, streaming WebSocket servers, or custom VAD/STT pipelines).
 */
interface PcmConsumerSpeechToText : SpeechToText {
    override val consumesPcmDirectly: Boolean get() = true
    fun processAudioChunk(chunk: AudioChunk)
}
