package com.svarax.fraud

/**
 * Structured Fraud Indicator detected during live audio analysis.
 */
data class FraudIndicator(
    val category: FraudCategory,
    val confidence: Float,
    val evidence: String
)
