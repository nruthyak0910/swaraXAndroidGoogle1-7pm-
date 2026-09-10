package com.svarax.fraud

/**
 * 14 distinct scam indicators supported by the Svara_X Native Android Detection Engine.
 */
enum class FraudCategory(val label: String, val baseWeight: Double) {
    BANK_IMPERSONATION("Bank impersonation", 0.35),
    OTP_REQUEST("OTP request detected", 0.40),
    ACCOUNT_BLOCK_THREAT("Account threat detected", 0.30),
    URGENCY_PRESSURE("Urgency detected", 0.25),
    POLICE_GOVERNMENT_IMPERSONATION("Police/Law enforcement impersonation", 0.40),
    DIGITAL_ARREST_THREAT("Digital arrest threat", 0.45),
    REMOTE_ACCESS_APP_DEMAND("Remote access app request", 0.45),
    KYC_UPDATE_DEADLINE("Urgent KYC update demand", 0.30),
    CREDIT_CARD_POINTS_REFUND("Fake refund / reward points", 0.25),
    LOTTERY_PRIZE_ADVANCE_FEE("Lottery / Prize claim fee", 0.30),
    INVESTMENT_GUARANTEED_RETURNS("Guaranteed return investment", 0.35),
    PART_TIME_JOB_OFFER("Task / Part-time job deposit", 0.30),
    SECRECY_ISOLATION_DEMAND("Secrecy / Do not hang up demand", 0.35),
    CALL_MERGING_DEMAND("Call forwarding / merge request", 0.40);
}
