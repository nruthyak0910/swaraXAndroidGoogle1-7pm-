"""
Svara_X Lightweight FastAPI Backend
Designed for local testing on a laptop or local network during SIH Hackathon.
Accepts transcript chunks, checks for fraud indicators, and computes risk scores.
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
import uvicorn
import re

app = FastAPI(
    title="Svara_X Backend API",
    description="Real-Time Fraud Call Analysis and Risk Engine",
    version="1.0.0-sih"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class TranscriptRequest(BaseModel):
    transcript: str
    caller_number: Optional[str] = "Unknown"

class FraudIndicatorResult(BaseModel):
    category: str
    confidence: float
    evidence: str

class RiskAnalysisResponse(BaseModel):
    fraud_probability: float
    risk_level: str  # LOW, MEDIUM, HIGH, CRITICAL
    indicators: List[FraudIndicatorResult]
    recommendation: str

# Rule & Pattern Signatures for SIH Prototype
PATTERNS = [
    {
        "category": "OTP_REQUEST",
        "regex": r"\b(otp|one[-\s]?time[-\s]?password|verification\s+code)\b",
        "weight": 0.50,
        "evidence": "Caller explicitly requested One-Time Password (OTP)"
    },
    {
        "category": "ACCOUNT_BLOCK_THREAT",
        "regex": r"\b(block|blocked|suspend|deactivate|freeze|terminated)\b.*\b(account|card|sim|access)\b|\b(account|card)\b.*\b(block|suspend|freeze)\b",
        "weight": 0.35,
        "evidence": "Threat detected: Account or card blocking ultimatum"
    },
    {
        "category": "BANK_IMPERSONATION",
        "regex": r"\b(bank|sbi|hdfc|icici|rbi|reserve\s+bank|manager|customer\s+care|fraud\s+department)\b",
        "weight": 0.30,
        "evidence": "Caller impersonated bank/regulatory institution representative"
    },
    {
        "category": "URGENT_ACTION",
        "regex": r"\b(immediately|urgent|within\s+\d+\s+minutes|hurry|right\s+now|last\s+chance)\b",
        "weight": 0.25,
        "evidence": "High-pressure urgency signals detected"
    },
    {
        "category": "PIN_OR_PASSWORD_REQUEST",
        "regex": r"\b(pin|password|cvv|expiry\s+date|card\s+details)\b",
        "weight": 0.45,
        "evidence": "Critical financial authentication credential requested"
    }
]

@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "service": "Svara_X Backend",
        "stage": "Priority 1-4 Active",
        "telephony_screening": "Compatible"
    }

@app.post("/analyze/transcript", response_model=RiskAnalysisResponse)
def analyze_transcript(req: TranscriptRequest):
    text = req.transcript.lower()
    matched_indicators: List[FraudIndicatorResult] = []
    cumulative_score = 0.05  # baseline low risk

    for rule in PATTERNS:
        if re.search(rule["regex"], text, re.IGNORECASE):
            matched_indicators.append(
                FraudIndicatorResult(
                    category=rule["category"],
                    confidence=0.92,
                    evidence=rule["evidence"]
                )
            )
            cumulative_score += rule["weight"]

    # Cap score at 0.99
    risk_score = min(0.99, cumulative_score)

    if risk_score >= 0.80:
        level = "CRITICAL"
        rec = "DO NOT SHARE OTP OR PIN! HANG UP IMMEDIATELY."
    elif risk_score >= 0.60:
        level = "HIGH"
        rec = "DO NOT SHARE BANKING CREDENTIALS. HIGH FRAUD LIKELIHOOD."
    elif risk_score >= 0.30:
        level = "MEDIUM"
        rec = "Exercise caution. Verify caller identity with your official branch."
    else:
        level = "LOW"
        rec = "No immediate scam indicators detected."

    return RiskAnalysisResponse(
        fraud_probability=round(risk_score, 2),
        risk_level=level,
        indicators=matched_indicators,
        recommendation=rec
    )

if __name__ == "__main__":
    print("Starting Svara_X local testing backend on port 8000...")
    uvicorn.run(app, host="0.0.0.0", port=8000)
