"""
Svara_X Backend: Risk Engine Module
Calculates transparent risk probability, assigns risk levels, and recommends actions.
"""

from typing import List, Dict, Any

class RiskEngine:
    def __init__(self, base_risk: float = 0.05):
        self.base_risk = base_risk

    def evaluate(self, indicators: List[Dict[str, Any]], synthetic_voice_prob: float = 0.0) -> Dict[str, Any]:
        cumulative = self.base_risk

        for ind in indicators:
            weight = ind.get("weight", 0.20)
            confidence = ind.get("confidence", 0.90)
            cumulative += weight * confidence

        # Acoustic voice penalty if synthetic speech detected
        if synthetic_voice_prob > 0.50:
            cumulative += 0.12

        # Clamp between 0.05 and 0.99
        risk_probability = max(0.05, min(0.99, cumulative))
        score_percent = int(round(risk_probability * 100))

        if score_percent >= 80:
            level = "CRITICAL"
            rec = self._get_critical_recommendation(indicators)
        elif score_percent >= 60:
            level = "HIGH"
            rec = "DO NOT SHARE BANKING CREDENTIALS. HIGH FRAUD LIKELIHOOD."
        elif score_percent >= 30:
            level = "MEDIUM"
            rec = "Exercise caution. Verify caller identity with your official branch."
        else:
            level = "LOW"
            rec = "No immediate scam indicators detected. Maintain vigilance."

        return {
            "fraud_probability": round(risk_probability, 2),
            "risk_score": score_percent,
            "risk_level": level,
            "indicators": [ind["evidence"] for ind in indicators],
            "recommendation": rec
        }

    def _get_critical_recommendation(self, indicators: List[Dict[str, Any]]) -> str:
        categories = {ind["category"] for ind in indicators}
        if any(c in categories for c in ["OTP_REQUEST", "PIN_REQUEST", "PASSWORD_REQUEST"]):
            return "DO NOT SHARE OTP OR PIN! HANG UP IMMEDIATELY. OFFICIAL BANKS NEVER ASK FOR PASSWORDS."
        if "REMOTE_ACCESS_REQUEST" in categories:
            return "DO NOT INSTALL ANY SCREEN-SHARING APP! DISCONNECT THE CALL NOW."
        return "HIGH CONFIDENCE FRAUD DETECTED! TERMINATE THIS CALL IMMEDIATELY."
