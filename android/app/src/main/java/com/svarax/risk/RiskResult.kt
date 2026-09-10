package com.svarax.risk

/**
 * Priority 8 & 10: Risk Engine output format.
 */
data class RiskResult(
    val riskScore: Int, // 0 - 100
    val riskLevel: String, // LOW, MEDIUM, HIGH, CRITICAL
    val indicators: List<String>,
    val recommendation: String,
    val isDemoSimulation: Boolean = false
)
