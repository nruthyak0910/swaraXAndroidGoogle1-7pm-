package com.svarax.audio

/**
 * AudioInputState: Explicit lifecycle and signal states of the audio input subsystem.
 */
enum class AudioInputState {
    UNAVAILABLE,
    INITIALIZING,
    ACTIVE,
    NO_SIGNAL,
    ERROR
}

/**
 * AudioSignal: Explicit signal presence classification based on real sample amplitudes.
 */
enum class AudioSignal {
    SILENCE,
    SIGNAL_PRESENT
}

/**
 * AudioDiagnostics: Real-time diagnostics from the audio capture hardware/subsystem.
 */
data class AudioDiagnostics(
    val isInitialized: Boolean = false,
    val sampleRate: Int = 16000,
    val channelCount: Int = 1,
    val encoding: String = "PCM_16BIT",
    val audioSource: String = "MediaRecorder.AudioSource.MIC",
    val samplesRead: Long = 0L,
    val nonZeroSamples: Long = 0L,
    val rmsLevel: Double = 0.0,
    val lastSignal: AudioSignal = AudioSignal.SILENCE,
    val lastError: String? = null
)

/**
 * AudioChunk: Carries raw PCM data along with sample metrics.
 */
data class AudioChunk(
    val data: ByteArray,
    val timestamp: Long = System.currentTimeMillis(),
    val sampleRate: Int = 16000,
    val sampleCount: Int = 0,
    val nonZeroSamples: Int = 0,
    val rmsLevel: Double = 0.0,
    val signal: AudioSignal = AudioSignal.SILENCE
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioChunk
        return data.contentEquals(other.data) && timestamp == other.timestamp && sampleRate == other.sampleRate
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + sampleRate
        return result
    }
}

/**
 * AudioInput: Hardware abstraction interface with state and diagnostics.
 */
interface AudioInput {
    fun start(onChunkReceived: (AudioChunk) -> Unit)
    fun stop()
    fun isCapturing(): Boolean
    fun getState(): AudioInputState
    fun getDiagnostics(): AudioDiagnostics
}
