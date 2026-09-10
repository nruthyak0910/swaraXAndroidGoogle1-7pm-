# Svara_X: AI Real-Time Fraud Call Protection

An Android prototype designed for SIH (Smart India Hackathon) to intercept incoming telephony calls and analyze conversations in real time for fraud, scam impersonation, and OTP theft indicators.

---

## 1. Project Architecture (Priority 1 to 4)

```
[ Incoming Call from Phone A ]
              │
              ▼
    [ Android Telecom Subsystem ]
              │
              ▼
[ SvaraCallScreeningService (Priority 4) ]
   ├── Extracts Caller ID (e.g. +91 98765 43210)
   ├── Non-blocking CallResponse.Builder().build()
   └── Dispatches to CallStateManager
              │
              ▼
     [ CallStateManager ]
   ├── Tracks State: IDLE ➔ RINGING ➔ OFFHOOK ➔ DISCONNECTED
   ├── Notifies MainActivity UI in real time
   └── Starts CallMonitoringService (Foreground Service)
              │
              ▼
    [ MainActivity Dashboard ]
   ├── Protection Active / Inactive status
   ├── RoleManager.ROLE_CALL_SCREENING binding
   ├── Runtime Permission checklist (Phone State, Mic, Notifications)
   └── Real-time Telephony Event Logs
```

---

## 2. Android Limitations & Engineering Truth

* **Cellular Audio Restriction:** Modern Android (Android 10 through 15) strictly forbids third-party apps from recording internal in-call 2-way cellular audio streams for privacy reasons.
* **Controlled Prototype Demonstration Method:** For the SIH demonstration, **Phone B** (running Svara_X) receives the call and enables **speakerphone**. Svara_X's microphone pipeline captures the audio from the speakerphone, converts speech to text, and streams it to the fraud risk engine.
* **Call Screening Service:** By implementing `android.telecom.CallScreeningService` and holding `RoleManager.ROLE_CALL_SCREENING`, Svara_X legally intercepts call events the instant they ring.

---

## 3. Project Directory Structure

```
Svara_X/
├── android/
│   ├── build.gradle.kts                     # Root build configuration
│   ├── settings.gradle.kts                  # Project modules definition
│   ├── gradle.properties                    # AndroidX & JVM configuration
│   └── app/
│       ├── build.gradle.kts                 # Application build script (SDK 34, Min 29)
│       ├── proguard-rules.pro
│       └── src/main/
│           ├── AndroidManifest.xml          # Permissions, Screening Service, Receiver
│           ├── java/com/svarax/
│           │   ├── MainActivity.kt          # Dashboard, Permissions, RoleManager, Event Log
│           │   ├── call/
│           │   │   ├── SvaraCallScreeningService.kt   # Intercepts incoming calls
│           │   │   ├── CallStateManager.kt            # Centralized telephony lifecycle
│           │   │   └── CallStateReceiver.kt           # BroadcastReceiver fallback
│           │   ├── permission/
│           │   │   └── PermissionHelper.kt            # Permission & RoleManager utilities
│           │   ├── service/
│           │   │   └── CallMonitoringService.kt       # Foreground active call service
│           │   ├── audio/
│           │   │   └── AudioInput.kt                  # Audio chunk abstraction
│           │   ├── fraud/
│           │   │   └── FraudIndicator.kt              # Categories & indicators
│           │   └── risk/
│           │       └── RiskResult.kt                  # Risk engine output model
│           └── res/
│               ├── layout/activity_main.xml
│               ├── values/colors.xml
│               ├── values/strings.xml
│               └── values/themes.xml
│
├── backend/
│   ├── server.py                            # FastAPI test backend
│   ├── requirements.txt
│   └── README.md
│
└── README.md
```

---

## 4. How to Build & Run (Priority 1 & 2)

### Step 1: Open in Android Studio
1. Open **Android Studio** (Hedgehog, Iguana, or Jellyfish recommended).
2. Select **File > Open** and choose the `android/` directory.
3. Allow Gradle to sync dependencies.
4. Target Device: Physical Android phone running Android 10+ (API 29 to 34) connected via USB Debugging.

### Step 2: Compile & Install
```bash
cd android
./gradlew assembleDebug
# Install to connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. Setup & Permissions (Priority 3)

When Svara_X opens on your device:
1. Tap **"Request All Permissions"**:
   - `READ_PHONE_STATE`: To detect call state transitions.
   - `RECORD_AUDIO`: For speakerphone audio acquisition.
   - `POST_NOTIFICATIONS`: For ongoing call status and high-risk alerts.
2. Tap **"Enable"** on the **Call Screening Service Role** card:
   - Android will display a system prompt asking to set **Svara_X** as the default Call Screening app.
   - Select **Svara_X** and confirm.
3. Verification: The top banner updates to **🟢 ACTIVE**.

---

## 6. Call Screening & State Detection (Priority 4)

### Method A: Live Two-Phone Test (Real Telephony)
1. **Phone B** has Svara_X installed with Protection: ACTIVE.
2. **Phone A** dials Phone B's mobile number.
3. Observe:
   - Svara_X's `SvaraCallScreeningService` triggers immediately on incoming ring.
   - The event log outputs: `[Screening Event] Incoming call from +91 XXXXX XXXXX detected.`
   - Answer the call: State transitions to `OFFHOOK (Call Active)`.
   - End the call: State returns to `IDLE`.

### Method B: In-App Simulation (Zero Hardware)
- Tap the **"Test Priority 4: Trigger Simulated Call Event"** button on the dashboard:
  - Click 1: Simulates incoming `RINGING` from `+91 98765 43210`.
  - Click 2: Simulates call answered `OFFHOOK`.
  - Click 3: Simulates call ended `IDLE`.

### Method C: ADB Terminal Verification
```bash
# Verify Call Screening role is bound to Svara_X
adb shell telecom get-call-screening-app

# Force test incoming telephony state
adb shell am broadcast -a android.intent.action.PHONE_STATE --es state RINGING --es incoming_number "+919876543210"
```

---

## 7. Troubleshooting

| Issue | Root Cause | Solution |
| :--- | :--- | :--- |
| Call Screening role dialog doesn't appear | Device is Android 9 or below | Android 10+ (API 29+) is required for `RoleManager.ROLE_CALL_SCREENING`. On older devices, Svara_X uses `CallStateReceiver` fallback. |
| Caller Number is null or "Unknown" | Carrier restricts caller ID before pickup | Grant `READ_CALL_LOG` permission in app settings or test with a SIM carrier that forwards caller ID in the telephony header. |
| Background service killed by OEM battery optimizer | Aggressive background killing (MIUI, ColorOS) | In phone settings, set Battery Optimization for Svara_X to **"Unrestricted"**. |
