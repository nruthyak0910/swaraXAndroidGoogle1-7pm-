"""
Svara_X Backend: Fraud Detector Module
Implements pattern-matching rules for all 14 fraud categories specified in Section 9.
"""

import re
from typing import List, Dict, Any

FRAUD_PATTERNS = [
    {
        "category": "OTP_REQUEST",
        "regex": r"\b(otp|one[-\s]?time[-\s]?password|verification\s+code|6[-\s]?digit\s+code)\b",
        "weight": 0.45,
        "evidence": "Direct request for One-Time Password (OTP)"
    },
    {
        "category": "PIN_REQUEST",
        "regex": r"\b(atm\s+pin|upi\s+pin|mpin|security\s+pin)\b",
        "weight": 0.45,
        "evidence": "ATM or UPI PIN credential requested"
    },
    {
        "category": "PASSWORD_REQUEST",
        "regex": r"\b(net\s*banking\s+password|login\s+password|account\s+password)\b",
        "weight": 0.45,
        "evidence": "Banking login password requested"
    },
    {
        "category": "CARD_INFORMATION_REQUEST",
        "regex": r"\b(cvv|cvv2|card\s+number|expiry\s+date|16[-\s]?digit)\b",
        "weight": 0.35,
        "evidence": "Debit/credit card details or CVV requested"
    },
    {
        "category": "ACCOUNT_BLOCK_THREAT",
        "regex": r"(?i)(\b(account|card|sim|access|service)\b.*\b(block|blocked|suspend|deactivate|freeze)\b)|(\b(block|blocked|freeze)\b.*\b(account|card|sim)\b)",
        "weight": 0.30,
        "evidence": "Threat ultimatum to freeze or block bank account"
    },
    {
        "category": "BANK_IMPERSONATION",
        "regex": r"\b(sbi|hdfc|icici|axis|rbi|reserve\s+bank|bank\s+manager|head\s+office|fraud\s+prevention)\b",
        "weight": 0.25,
        "evidence": "Impersonation of banking institution or financial regulator"
    },
    {
        "category": "URGENT_ACTION",
        "regex": r"\b(immediately|within\s+\d+\s+minutes|right\s+now|hurry\s+up|last\s+chance|before\s+midnight)\b",
        "weight": 0.20,
        "evidence": "Psychological urgency and high-pressure deadline detected"
    },
    {
        "category": "GOVERNMENT_IMPERSONATION",
        "regex": r"\b(income\s+tax|tax\s+department|customs\s+department|telecom\s+department|dot|enforcement\s+directorate|ed)\b",
        "weight": 0.25,
        "evidence": "Government authority / department impersonation detected"
    },
    {
        "category": "POLICE_IMPERSONATION",
        "regex": r"\b(police|cyber\s+cell|cbi|inspector|police\s+station|arrest\s+warrant)\b",
        "weight": 0.30,
        "evidence": "Law enforcement / police threat impersonation"
    },
    {
        "category": "PAYMENT_REQUEST",
        "regex": r"\b(send\s+money|transfer\s+amount|processing\s+fee|pay\s+penalty|google\s*pay|phonepe)\b",
        "weight": 0.25,
        "evidence": "Suspicious money transfer or penalty payment demand"
    },
    {
        "category": "REMOTE_ACCESS_REQUEST",
        "regex": r"\b(anydesk|teamviewer|quicksupport|screen\s+share|install\s+app|rustdesk)\b",
        "weight": 0.45,
        "evidence": "Request to install remote screen-sharing software (AnyDesk/TeamViewer)"
    },
    {
        "category": "PRIZE_SCAM",
        "regex": r"\b(won\s+a\s+prize|lottery\s+winner|lucky\s+draw|cashback\s+offer|gift\s+voucher)\b",
        "weight": 0.20,
        "evidence": "Lottery / unprompted prize or cashback scam pattern"
    },
    {
        "category": "INVESTMENT_SCAM",
        "regex": r"\b(guaranteed\s+returns|double\s+your\s+money|crypto\s+profit|stock\s+tip|daily\s+profit)\b",
        "weight": 0.25,
        "evidence": "High-yield investment / quick money scam pattern"
    },
    {
        "category": "SOCIAL_ENGINEERING",
        "regex": r"\b(do\s+not\s+tell\s+anyone|keep\s+this\s+confidential|don't\s+disconnect|remain\s+on\s+call)\b",
        "weight": 0.20,
        "evidence": "Isolation tactic: Requesting secrecy and preventing call disconnection"
    }
]

class FraudDetector:
    def __init__(self):
        self.rules = [
            {
                "category": r["category"],
                "compiled": re.compile(r["regex"], re.IGNORECASE),
                "weight": r["weight"],
                "evidence": r["evidence"]
            }
            for r in FRAUD_PATTERNS
        ]

    def detect(self, transcript: str) -> List[Dict[str, Any]]:
        matched = []
        seen = set()
        for rule in self.rules:
            if rule["compiled"].search(transcript) and rule["category"] not in seen:
                matched.append({
                    "category": rule["category"],
                    "confidence": 0.94,
                    "weight": rule["weight"],
                    "evidence": rule["evidence"]
                })
                seen.add(rule["category"])
        return matched
