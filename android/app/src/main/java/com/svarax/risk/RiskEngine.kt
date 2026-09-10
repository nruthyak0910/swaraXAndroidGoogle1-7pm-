package com.svarax.risk

import com.svarax.fraud.FraudCategory
import com.svarax.fraud.FraudIndicator
import com.svarax.voice.VoiceAnalysisResult

/**
 * Priority 8: Centralized Multi-Signal Risk Calculation Engine.
 * Combines NLP fraud indicators, conversational urgency, and optional acoustic voice signals
 * using a transparent, documented weighted model.
 */
class RiskEngine {

    companion object {
        private const val BASE_RISK = 0.05f

        // Documented Transparent Category Weights matching the 14 categories
        private val CATEGORY_WEIGHTS = mapOf(
            FraudCategory.OTP_REQUEST to 0.45f,
            FraudCategory.REMOTE_ACCESS_APP_DEMAND to 0.45f,
            FraudCategory.DIGITAL_ARREST_THREAT to 0.45f,
            FraudCategory.CALL_MERGING_DEMAND to 0.40f,
            FraudCategory.POLICE_GOVERNMENT_IMPERSONATION to 0.40f,
            FraudCategory.BANK_IMPERSONATION to 0.35f,
            FraudCategory.INVESTMENT_GUARANTEED_RETURNS to 0.35f,
            FraudCategory.SECRECY_ISOLATION_DEMAND to 0.35f,
            FraudCategory.ACCOUNT_BLOCK_THREAT to 0.30f,
            FraudCategory.KYC_UPDATE_DEADLINE to 0.30f,
            FraudCategory.LOTTERY_PRIZE_ADVANCE_FEE to 0.30f,
            FraudCategory.PART_TIME_JOB_OFFER to 0.30f,
            FraudCategory.URGENCY_PRESSURE to 0.25f,
            FraudCategory.CREDIT_CARD_POINTS_REFUND to 0.25f
        )
    }

    /**
     * Computes the cumulative risk score and returns a structured RiskResult.
     */
    fun evaluate(
        indicators: List<FraudIndicator>,
        voiceResult: VoiceAnalysisResult? = null,
        isDemo: Boolean = false
    ): RiskResult {
        var cumulativeWeight = BASE_RISK

        val indicatorDescriptions = mutableListOf<String>()

        for (indicator in indicators) {
            val weight = CATEGORY_WEIGHTS[indicator.category] ?: 0.15f
            cumulativeWeight += weight * indicator.confidence
            indicatorDescriptions.add(indicator.evidence)
        }

        // Add acoustic voice factor if elevated synthetic voice detected
        if (voiceResult != null && voiceResult.isAvailable && voiceResult.syntheticProbability > 0.50f) {
            cumulativeWeight += 0.12f
            indicatorDescriptions.add("Acoustic voice analysis indicates elevated synthetic deepfake probability")
        }

        // Bound probability between 0.05 and 0.99
        val normalizedProbability = cumulativeWeight.coerceIn(0.05f, 0.99f)
        val scorePercent = (normalizedProbability * 100).toInt()

        // Categorize into configurable thresholds
        val (level, recommendation) = when {
            scorePercent >= 80 -> {
                Pair(
                    "CRITICAL",
                    generateCriticalRecommendation(indicators)
                )
            }
            scorePercent >= 60 -> {
                Pair(
                    "HIGH",
                    generateHighRecommendation(indicators)
                )
            }
            scorePercent >= 30 -> {
                Pair(
                    "MEDIUM",
                    "Exercise caution. Do not disclose sensitive financial credentials."
                )
            }
            else -> {
                Pair(
                    "LOW",
                    "No immediate fraud pattern detected. Maintain standard vigilance."
                )
            }
        }

        return RiskResult(
            riskScore = scorePercent,
            riskLevel = level,
            indicators = indicatorDescriptions.distinct(),
            recommendation = recommendation,
            isDemoSimulation = isDemo
        )
    }

    private fun generateCriticalRecommendation(indicators: List<FraudIndicator>): String {
        val hasOtp = indicators.any { it.category == FraudCategory.OTP_REQUEST }
        val hasRemoteAccess = indicators.any { it.category == FraudCategory.REMOTE_ACCESS_APP_DEMAND }
        val hasDigitalArrest = indicators.any { it.category == FraudCategory.DIGITAL_ARREST_THREAT }

        return when {
            hasOtp -> "DO NOT SHARE OTP OR PIN! HANG UP IMMEDIATELY. OFFICIAL BANKS NEVER ASK FOR PASSWORDS."
            hasRemoteAccess -> "DO NOT INSTALL ANY SCREEN-SHARING APP! DISCONNECT THE CALL NOW."
            hasDigitalArrest -> "DIGITAL ARREST IS A SCAM. POLICE NEVER CONDUCT ARRESTS VIA PHONE OR VIDEO CALL. HANG UP."
            else -> "HIGH CONFIDENCE SCAM! TERMINATE THIS CALL IMMEDIATELY."
        }
    }

    private fun generateHighRecommendation(indicators: List<FraudIndicator>): String {
        val hasAccountThreat = indicators.any { it.category == FraudCategory.ACCOUNT_BLOCK_THREAT }
        return if (hasAccountThreat) {
            "Threat detected. Verify with your official bank branch in person. Do not make any payments."
        } else {
            "Suspicious intent detected. Never share card digits, OTPs, or transfer money."
        }
    }
}
