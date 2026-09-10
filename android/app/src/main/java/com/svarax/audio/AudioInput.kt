package com.svarax.audio

/**
 * Clean audio input abstraction.
 * Note on Android Limitations: Direct internal cellular call audio recording is restricted
 * on modern Android devices. For the SIH prototype, MicrophoneAudioInput captures controlled
 * speakerphone audio, decoupling audio acquisition from downstream STT & fraud detection.
 */
data class AudioChunk(
    val data: ByteArray,
    val timestamp: Long = System.currentTimeMillis(),
    val sampleRate: Int = 16000
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

interface AudioInput {
    fun start(onChunkReceived: (AudioChunk) -> Unit)
    fun stop()
    fun isCapturing(): Boolean
}
