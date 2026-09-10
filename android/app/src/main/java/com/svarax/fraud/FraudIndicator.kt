package com.svarax.fraud

/**
 * Priority 7 & 9 Foundation: Structured Fraud Categories & Indicators.
 */
enum class FraudCategory {
    BANK_IMPERSONATION,
    OTP_REQUEST,
    PIN_REQUEST,
    PASSWORD_REQUEST,
    CARD_INFORMATION_REQUEST,
    URGENT_ACTION,
    ACCOUNT_BLOCK_THREAT,
    PAYMENT_REQUEST,
    GOVERNMENT_IMPERSONATION,
    POLICE_IMPERSONATION,
    PRIZE_SCAM,
    INVESTMENT_SCAM,
    REMOTE_ACCESS_REQUEST,
    SOCIAL_ENGINEERING
}

data class FraudIndicator(
    val category: FraudCategory,
    val confidence: Float,
    val evidence: String
)
