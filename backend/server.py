"""
Svara_X Lightweight FastAPI Backend
Designed for local testing on a laptop or local network during SIH Hackathon.
Accepts transcript chunks, checks for fraud indicators, and computes risk scores.
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
import uvicorn

from fraud_detector import FraudDetector
from risk_engine import RiskEngine

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

fraud_detector = FraudDetector()
risk_engine = RiskEngine()

class TranscriptRequest(BaseModel):
    transcript: str
    caller_number: Optional[str] = "Unknown"
    synthetic_voice_prob: Optional[float] = 0.0

class RiskAnalysisResponse(BaseModel):
    fraud_probability: float
    risk_score: int
    risk_level: str  # LOW, MEDIUM, HIGH, CRITICAL
    indicators: List[str]
    recommendation: str

@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "service": "Svara_X Backend",
        "stage": "Priorities 1-12 Active",
        "telephony_screening": "Compatible",
        "fraud_categories_loaded": len(fraud_detector.rules)
    }

@app.post("/analyze/transcript", response_model=RiskAnalysisResponse)
def analyze_transcript(req: TranscriptRequest):
    detected_indicators = fraud_detector.detect(req.transcript)
    result = risk_engine.evaluate(
        indicators=detected_indicators,
        synthetic_voice_prob=req.synthetic_voice_prob or 0.0
    )
    return RiskAnalysisResponse(
        fraud_probability=result["fraud_probability"],
        risk_score=result["risk_score"],
        risk_level=result["risk_level"],
        indicators=result["indicators"],
        recommendation=result["recommendation"]
    )

if __name__ == "__main__":
    print("Starting Svara_X local testing backend on port 8000...")
    uvicorn.run(app, host="0.0.0.0", port=8000)
