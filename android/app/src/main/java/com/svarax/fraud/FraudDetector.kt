package com.svarax.fraud

import java.util.regex.Pattern

/**
 * Priority 7: Contextual Hybrid Rule & Pattern Fraud Detection Engine.
 * Analyzes conversational transcript chunks in real-time for suspicious scam signatures.
 */
class FraudDetector {

    private data class RulePattern(
        val category: FraudCategory,
        val pattern: Pattern,
        val confidence: Float,
        val evidenceDescription: String
    )

    private val rules: List<RulePattern> = listOf(
        // 1. Bank Impersonation
        RulePattern(
            category = FraudCategory.BANK_IMPERSONATION,
            pattern = Pattern.compile("\\b(sbi|hdfc|icici|axis|rbi|reserve\\s+bank|bank\\s+manager|calling\\s+from\\s+(your\\s+)?bank|bank\\s+branch|fraud\\s+prevention\\s+department)\\b", Pattern.CASE_INSENSITIVE),
            confidence = 0.90f,
            evidenceDescription = "Bank impersonation"
        ),
        // 2. OTP Request (Critical)
        RulePattern(
            category = FraudCategory.OTP_REQUEST,
            pattern = Pattern.compile("\\b(otp|one[-\\s]?time[-\\s]?password|verification\\s+code|6[-\\s]?digit\\s+code|sms\\s+code|tell\\s+me\\s+the\\s+code)\\b", Pattern.CASE_INSENSITIVE),
            confidence = 0.98f,
            evidenceDescription = "OTP request detected"
        ),
        // 3. Account Block Threat
        RulePattern(
            category = FraudCategory.ACCOUNT_BLOCK_THREAT,
            pattern = Pattern.compile("(?i)(\\b(account|card|sim|access|service)\\b.*\\b(block|blocked|suspend|deactivate|freeze|terminated)\\b)|(\\b(block|blocked|freeze)\\b.*\\b(account|card|sim)\\b)", Pattern.CASE_INSENSITIVE),
            confidence = 0.92f,
            evidenceDescription = "Account threat detected"
        ),
        // 4. Urgency Pressure
        RulePattern(
            category = FraudCategory.URGENCY_PRESSURE,
            pattern = Pattern.compile("\\b(immediately|today|within\\s+\\d+\\s+minutes|right\\s+now|hurry\\s+up|last\\s+chance|before\\s+midnight|urgent)\\b", Pattern.CASE_INSENSITIVE),
            confidence = 0.88f,
            evidenceDescription = "Urgency detected"
        ),
        // 5. Police / Law Enforcement Impersonation
        RulePattern(
            category = FraudCategory.POLICE_GOVERNMENT_IMPERSONATION,
            pattern = Pattern.compile("\\b(police|cyber\\s+cell|cbi|inspector|police\\s+station|customs|tax\\s+officer)\\b", Pattern.CASE_INSENSITIVE),
            confidence = 0.94f,
            evidenceDescription = "Police/Law enforcement impersonation"
        ),
        // 6. Digital Arrest Threat
        RulePattern(
            category = FraudCategory.DIGITAL_ARREST_THREAT,
            pattern = Pattern.compile("\\b(digital\\s+arrest|arrest\\s+warrant|court\\s+order|case\\s+registered|detained|narcotics)\\b", Pattern.CASE_INSENSITIVE),
            confidence = 0.96f,
            evidenceDescription = "Digital arrest threat"
        ),
        // 7. Remote Access App Demand
        RulePattern(
            category = FraudCategory.REMOTE_ACCESS_APP_DEMAND,
            pattern = Pattern.compile("\\b(anydesk|teamviewer|quicksupport|screen\\s+share|rustdesk|install\\s+app)\\b", Pattern.CASE_INSENSITIVE),
            confidence = 0.96f,
            evidenceDescription = "Remote access app request"
        ),
        // 8. KYC Update Deadline
        RulePattern(
            category = FraudCategory.KYC_UPDATE_DEADLINE,
            pattern = Pattern.compile("\\b(kyc\\s+(update|pending|verification|expired)|pan\\s+card\\s+link|aadhaar\\s+update)\\b", Pattern.CASE_INSENSITIVE),
            confidence = 0.90f,
            evidenceDescription = "Urgent KYC update demand"
        ),
        // 9. Fake Refund / Reward Points
        RulePattern(
            category = FraudCategory.CREDIT_CARD_POINTS_REFUND,
            pattern = Pattern.compile("\\b(reward\\s+points|cashback|refund\\s+amount|claim\\s+voucher|bonus\\s+points)\\b", Pattern.CASE_INSENSITIVE),
            confidence = 0.88f,
            evidenceDescription = "Fake refund / reward points"
        ),
        // 10. Lottery / Prize Scam
        RulePattern(
            category = FraudCategory.LOTTERY_PRIZE_ADVANCE_FEE,
            pattern = Pattern.compile("\\b(lottery\\s+winner|won\\s+a\\s+car|lucky\\s+draw|processing\\s+fee\\s+to\\s+claim)\\b", Pattern.CASE_INSENSITIVE),
            confidence = 0.91f,
            evidenceDescription = "Lottery / Prize claim fee"
        ),
        // 11. Guaranteed Investment Returns
        RulePattern(
            category = FraudCategory.INVESTMENT_GUARANTEED_RETURNS,
            pattern = Pattern.compile("\\b(guaranteed\\s+returns|double\\s+your\\s+money|crypto\\s+profit|stock\\s+tip|daily\\s+profit)\\b", Pattern.CASE_INSENSITIVE),
            confidence = 0.90f,
            evidenceDescription = "Guaranteed return investment"
        ),
        // 12. Task / Part-time Job Offer
        RulePattern(
            category = FraudCategory.PART_TIME_JOB_OFFER,
            pattern = Pattern.compile("\\b(part[-\\s]?time\\s+job|like\\s+youtube\\s+videos|telegram\\s+task|work\\s+from\\s+home\\s+deposit)\\b", Pattern.CASE_INSENSITIVE),
            confidence = 0.91f,
            evidenceDescription = "Task / Part-time job deposit"
        ),
        // 13. Secrecy & Call Isolation
        RulePattern(
            category = FraudCategory.SECRECY_ISOLATION_DEMAND,
            pattern = Pattern.compile("\\b(do\\s+not\\s+tell\\s+anyone|keep\\s+this\\s+confidential|don't\\s+disconnect|remain\\s+on\\s+call|isolated\\s+room)\\b", Pattern.CASE_INSENSITIVE),
            confidence = 0.92f,
            evidenceDescription = "Secrecy / Do not hang up demand"
        ),
        // 14. Call Forwarding / Merging
        RulePattern(
            category = FraudCategory.CALL_MERGING_DEMAND,
            pattern = Pattern.compile("\\b(\\*401\\*|forward\\s+your\\s+call|merge\\s+call|conference\\s+call)\\b", Pattern.CASE_INSENSITIVE),
            confidence = 0.95f,
            evidenceDescription = "Call forwarding / merge request"
        )
    )

    /**
     * Analyzes incoming transcript text and returns all detected fraud indicators.
     */
    fun analyze(transcript: String): List<FraudIndicator> {
        val detectedIndicators = mutableListOf<FraudIndicator>()
        val seenCategories = mutableSetOf<FraudCategory>()

        for (rule in rules) {
            val matcher = rule.pattern.matcher(transcript)
            if (matcher.find() && !seenCategories.contains(rule.category)) {
                detectedIndicators.add(
                    FraudIndicator(
                        category = rule.category,
                        confidence = rule.confidence,
                        evidence = rule.evidenceDescription
                    )
                )
                seenCategories.add(rule.category)
            }
        }

        return detectedIndicators
    }
}
