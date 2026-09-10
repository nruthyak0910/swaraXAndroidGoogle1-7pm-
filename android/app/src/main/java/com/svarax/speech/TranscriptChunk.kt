package com.svarax.speech

/**
 * Priority 6 & 10: Represents an incremental transcribed speech fragment.
 */
data class TranscriptChunk(
    val text: String,
    val isFinal: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 0.95f
)
