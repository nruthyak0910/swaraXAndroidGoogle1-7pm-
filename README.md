# Svara_X: AI Real-Time Fraud Call Protection

An end-to-end native Android fraud call detection system engineered for **Smart India Hackathon (SIH)**. Svara_X intercepts incoming telephony calls, processes speech via a real-time streaming pipeline, matches against 14 financial scam indicator patterns, computes cumulative risk scores, and alerts users during high-risk conversations.

---

## 1. System Architecture

```
[ Incoming Cellular Call (Phone A) ]
                 │
                 ▼
[ Android Telecom Subsystem (API 29+) ]
                 │
                 ▼
[ SvaraCallScreeningService ] (Holds RoleManager.ROLE_CALL_SCREENING)
    ├── Extracts Caller Number (+91 XXXXX XXXXX)
    ├── Emits non-blocking CallResponse
    └── Triggers CallStateManager (RINGING)
                 │
                 ▼
[ CallMonitoringService ] (Foreground Service with Mic & PhoneCall types)
    ├── Triggers on OFFHOOK (Call Answered)
    ├── Starts Audio & STT Pipeline:
    │     ├── Live Mode: MicrophoneAudioInput (16kHz PCM mono via Speakerphone) ➔ AndroidSpeechToText
    │     └── Demo Mode: DemoSpeechToText (4-Stage Escalating Bank OTP Scam)
    ├── VoiceAnalyzer: Acoustic zero-crossing & harmonic heuristics (deepfake/synthetic audio checks)
    ├── FraudDetector: Evaluates transcript across 14 scam categories
    ├── RiskEngine: Weighted risk formula (0-100%) + recommendation synthesis
    ├── AlertManager: Dispatches heads-up warning notifications (Sound + Vibrate)
    └── LiveCallActivity: Dedicated high-contrast live warning screen
                 │
                 ▼
[ Call Termination (IDLE) ]
    ├── CallHistoryRepository: Persists call metadata, final risk score & indicators to SQLite/SharedPreferences
    └── CallHistoryActivity: Audit log & forensic breakdown for user review
```

---

## 2. Complete List of Added & Changed Files

### A. Android Native Code (`/android/app/src/main/`)
| Module / Directory | File | Purpose | Priority |
| :--- | :--- | :--- | :--- |
| **Manifest & Setup** | `AndroidManifest.xml` | Declares permissions, foreground service types, and activities | P1, P3, P4, P14 |
| **Telephony** | `call/SvaraCallScreeningService.kt` | Intercepts incoming calls at system telecom layer | P4 |
| **Telephony** | `call/CallStateManager.kt` | Manages transitions (`IDLE` ➔ `RINGING` ➔ `OFFHOOK`) | P4 |
| **Telephony** | `call/CallStateReceiver.kt` | BroadcastReceiver fallback for legacy devices | P4 |
| **Permissions** | `permission/PermissionHelper.kt` | Runtime permission prompts & `RoleManager` binding | P3 |
| **Service** | `service/CallMonitoringService.kt` | Full-lifecycle active foreground service orchestrating pipeline | P5 |
| **Audio** | `audio/AudioInput.kt` | Core interface & `AudioChunk` data definition | P5, P10 |
| **Audio** | `audio/MicrophoneAudioInput.kt` | 16kHz, 16-bit PCM mono speakerphone audio recorder | P10 |
| **Speech** | `speech/SpeechToText.kt` | STT contract & callback interfaces | P6 |
| **Speech** | `speech/TranscriptChunk.kt` | Data model for real-time recognized phrases | P6 |
| **Speech** | `speech/AndroidSpeechToText.kt` | Native Android `SpeechRecognizer` integration | P6, P10 |
| **Speech** | `speech/DemoSpeechToText.kt` | Scripted multi-stage Bank OTP scam simulator for hackathon demos | P6, P18 |
| **Fraud Detection** | `fraud/FraudCategory.kt` | Enum defining all 14 fraud categories | P7 |
| **Fraud Detection** | `fraud/FraudIndicator.kt` | Match result container with evidence & confidence | P7 |
| **Fraud Detection** | `fraud/FraudDetector.kt` | Regex & keyword pattern engine for all 14 scam types | P7 |
| **Risk Engine** | `risk/RiskResult.kt` | Risk evaluation model (score, level, recommendations) | P8 |
| **Risk Engine** | `risk/RiskEngine.kt` | Weighted risk scoring model & action advice generator | P8 |
| **Alerting** | `alert/AlertManager.kt` | High-priority heads-up warning notification dispatcher | P9 |
| **Voice Analysis** | `voice/VoiceAnalyzer.kt` | Interface & heuristics for synthetic/deepfake voice cues | P11 |
| **History** | `history/CallRecord.kt` | Data model for audited call records | P12 |
| **History** | `history/CallHistoryRepository.kt` | Persistence repository for past call assessments | P12 |
| **UI** | `MainActivity.kt` | Main dashboard with permissions, simulator, demo trigger | P2, P14 |
| **UI** | `ui/LiveCallActivity.kt` | Dedicated in-call high-contrast fraud alert screen | P14 |
| **UI** | `ui/CallHistoryActivity.kt` | History audit activity with recyclerview | P12 |
| **UI** | `ui/CallHistoryAdapter.kt` | Recycler adapter with color-coded risk badges | P12 |
| **Layouts** | `res/layout/activity_main.xml` | Dashboard UI layout with demo & history buttons | P2 |
| **Layouts** | `res/layout/activity_live_call.xml` | Live warning UI layout with meter & transcript | P14 |
| **Layouts** | `res/layout/activity_history.xml` | Call history list layout | P12 |
| **Layouts** | `res/layout/item_call_history.xml` | Single call record card layout | P12 |

### B. Python Backend (`/backend/`)
| File | Description |
| :--- | :--- |
| `server.py` | Modular FastAPI service exposing `/health` and `/analyze/transcript` |
| `fraud_detector.py` | Python implementation of the 14-category fraud regex engine |
| `risk_engine.py` | Python risk engine with confidence weights and action advice |
| `requirements.txt` | Dependencies (`fastapi`, `uvicorn`, `pydantic`) |
| `README.md` | Backend instructions and curl testing examples |

### C. Web / React Live Simulator (`/src/`)
| File | Description |
| :--- | :--- |
| `src/App.tsx` | Comprehensive web simulator supporting all 12 priorities, live in-call warning screen, multi-stage scam demo, history audit log, and Android Studio codebase viewer |

---

## 3. The 14 Supported Fraud Categories

Svara_X detects:
1. `OTP_REQUEST`: Direct requests for One-Time Passwords / 6-digit codes.
2. `PIN_REQUEST`: Requests for ATM PIN, UPI MPIN, or security codes.
3. `PASSWORD_REQUEST`: Net banking passwords, login credentials.
4. `CARD_INFORMATION_REQUEST`: CVV, card numbers, expiry dates.
5. `ACCOUNT_BLOCK_THREAT`: Threats to freeze/suspend bank accounts or SIM cards.
6. `BANK_IMPERSONATION`: Claiming to be SBI, HDFC, ICICI, or RBI managers.
7. `URGENT_ACTION`: High-pressure deadlines ("immediately", "within 10 minutes").
8. `GOVERNMENT_IMPERSONATION`: Income Tax, Customs, Telecom Department (DoT).
9. `POLICE_IMPERSONATION`: Cyber Cell, Police Station, arrest warrant threats.
10. `PAYMENT_REQUEST`: Demands for processing fees, penalties, or immediate transfers.
11. `REMOTE_ACCESS_REQUEST`: Prompts to install AnyDesk, TeamViewer, or QuickSupport.
12. `PRIZE_SCAM`: Lottery wins, unexpected cashbacks, lucky draw prizes.
13. `INVESTMENT_SCAM`: Guaranteed returns, doubling money, crypto tips.
14. `SOCIAL_ENGINEERING`: Pressure to maintain secrecy ("do not tell anyone", "don't disconnect").

---

## 4. How to Run the Hackathon Demonstration

### Demonstration Mode (Recommended for Zero-Risk Live Demos)
1. Launch **Svara_X** on the target device (or in the web previewer).
2. Ensure **"Shield Status"** is **ACTIVE**.
3. Tap **"Run Multi-Stage Scam Simulation"** (or **"Run Bank OTP Scam Demo"**).
4. Watch the pipeline automatically progress across 4 realistic conversational stages:
   * **Stage 1 (Greeting):** *"Calling from bank security..."* ➔ Risk: **35% (MEDIUM)**
   * **Stage 2 (Threat):** *"Account will be blocked within 10 minutes..."* ➔ Risk: **68% (HIGH)**
   * **Stage 3 (Theft):** *"Share the 6-digit OTP right now..."* ➔ Risk: **94% (CRITICAL)** ➔ Heads-Up Warning Dispatched!
   * **Stage 4 (Isolation):** *"Hurry up or police will freeze your funds..."* ➔ Risk: **98% (CRITICAL)**
5. Tap **"End Call"**: The call record is automatically audited and saved to **Call History**.

### Real Two-Phone Hardware Demonstration
1. **Phone B (Target):** Install Svara_X, grant permissions, and select Svara_X as the default Call Screening app.
2. **Phone A (Caller):** Dials Phone B.
3. **Phone B Answers:** Turn on **Speakerphone** (standard controlled demo protocol per Android privacy rules).
4. Phone A reads a sample fraud script. Svara_X's speakerphone audio pipeline transcribes the speech and displays live warnings on screen.
