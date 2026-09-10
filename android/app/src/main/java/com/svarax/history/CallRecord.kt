package com.svarax.history

/**
 * Priority 12: Call history record.
 * Stores lightweight metadata and fraud detection summary without saving raw audio.
 */
data class CallRecord(
    val id: String,
    val timestamp: Long,
    val callerNumber: String,
    val durationSeconds: Int,
    val finalRiskScore: Int,
    val riskLevel: String, // LOW, MEDIUM, HIGH, CRITICAL
    val detectedIndicators: List<String>,
    val recommendation: String,
    val fullTranscriptSnippet: String = "",
    val isDemoSimulation: Boolean = false
)
