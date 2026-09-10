# Svara_X: 15-Step Physical Phone Test & End-to-End Verification Procedure

This document provides the definitive 15-step testing guide for evaluating **Svara_X: AI Real-Time Fraud Call Protection** on a physical Android 10+ (API 29+) device, along with the end-to-end data flow verification protocol.

---

## Pre-Requisites
- **Target Device (Phone B):** Physical Android smartphone running Android 10 (API 29) or higher with Developer Options & USB Debugging enabled.
- **Caller Device (Phone A):** Any phone capable of placing cellular calls to Phone B.
- **Workstation:** Android Studio Hedgehog / Iguana / Jellyfish or higher with Android SDK 34 and JDK 17.

---

## 15-Step Verification Protocol

### Step 1: Open Project & Gradle Sync
- Open this `/android/` directory in Android Studio.
- Verify Gradle sync succeeds against Android Gradle Plugin 8.3.2, Kotlin 1.9.23, and compileSdk 34.

### Step 2: Connect Target Device via USB / ADB
- Connect Phone B to workstation via USB.
- Run `adb devices` in terminal and confirm device is authorized (`device` state).

### Step 3: Install & Deploy Svara_X Debug Build
- Run `./gradlew installDebug` or click **Run 'app'** in Android Studio.
- Verify `com.svarax.debug` installs cleanly onto Phone B without installation or manifest merge errors.

### Step 4: Launch Svara_X & Inspect Initial Dashboard
- Open the **Svara_X** app from the app drawer.
- Observe the **Protection Status Card**: initially displays `PENDING PERMISSIONS` with amber outline.
- Observe the **System Setup & Permissions** card and **Call Screening Role** status.

### Step 5: Grant System Runtime Permissions
- Tap **"Review & Grant Permissions"** on the dashboard.
- Android system dialogs appear in sequence:
  1. Phone Calls & State (`READ_PHONE_STATE`, `READ_CALL_LOG`)
  2. Audio Recording (`RECORD_AUDIO` - required for controlled speakerphone transcription)
  3. Notifications (`POST_NOTIFICATIONS` on Android 13+)
- Tap **"While using the app"** or **"Allow"** for each prompt.
- Confirm the badge turns to `✓ Granted` in green.

### Step 6: Enable Call Screening Role
- Tap **"Enable"** on the **Call Screening Service Role** card.
- Android's native `RoleManager` dialog prompts: *"Set Svara_X as your default Caller ID & spam app"*.
- Select **Svara_X** and tap **Set as default**.
- Confirm the button updates to `Active ✓` and is disabled.

### Step 7: Verify Overall Shield Status Turns ACTIVE
- Verify that the top status card now displays `🟢 ACTIVE`.
- Observe the active glow border and subtitle indicating the telecom listener is active and waiting for calls.

### Step 8: Trigger Multi-Stage Scam Simulation
- Under **Live Demo & Active Call Controls**, tap **"▶ Run Multi-Stage Scam Simulation"**.
- Confirm the simulated caller (`+91 98765 43210`) triggers `RINGING` and then immediately transitions to `OFFHOOK`.
- Confirm `CallMonitoringService` starts as a foreground service with a persistent notification.

### Step 9: Verify Real-Time Transcript Streaming & Acoustic Heuristics
- The **In-Call Risk Warning Screen** (`LiveCallActivity`) automatically launches.
- Observe the pulsing live indicator dot.
- Observe the transcript updating sequentially with incoming conversational chunks.
- Confirm the acoustic voice spectrum status updates (indicating natural vs. synthetic heuristics).

### Step 10: Verify 14-Category Fraud Matching & Risk Escalation
- Observe the risk gauge as the 4 scam stages progress:
  - **Stage 1 (Impersonation):** *"Calling from bank branch security..."* ➔ Risk: **35% (MEDIUM)**
  - **Stage 2 (Account Threat):** *"Account will be blocked within ten minutes..."* ➔ Risk: **68% (HIGH)**
  - **Stage 3 (OTP Theft):** *"Share the 6-digit OTP right now..."* ➔ Risk: **94% (CRITICAL)**
  - **Stage 4 (Ultimatum):** *"Police will freeze all funds..."* ➔ Risk: **98% (CRITICAL)**
- Confirm detected indicators appear with warning badges in the indicator list.

### Step 11: Verify In-Call Warning UI & Heads-Up Notification Triggering
- When risk crosses the 80% threshold (`CRITICAL`):
  - In-Call screen borders turn high-contrast **Red**.
  - Recommendation banner displays clear, directive advice: *"DO NOT SHARE OTP OR PIN! HANG UP IMMEDIATELY"*.
  - System heads-up notification fires with alarm sound and dual vibration pulses (`vibrate: [0, 400, 200, 400]`).

### Step 12: Terminate Call & Verify Clean Teardown
- Tap **"End Active Simulation Call"** (or return to dashboard and tap end).
- Confirm `CallStateManager` updates to `IDLE`.
- Confirm `CallMonitoringService` stops cleanly, releases `AudioRecord` and `SpeechRecognizer`, and removes the foreground notification.

### Step 13: Verify Audit History Persistence in CallHistoryActivity
- Tap **"History"** button from the header of `MainActivity`.
- Confirm `CallHistoryActivity` renders the newly completed call at the top of the list.
- Confirm the call entry displays:
  - Caller number with `[DEMO]` indicator.
  - Timestamp and duration in seconds.
  - Final risk score badge (e.g., `94% CRITICAL` or `98% CRITICAL`).
  - Bulleted list of all detected fraud categories.
  - Recommendation text.

### Step 14: Real Two-Phone Hardware Demonstration
- Ensure Phone B is on the home screen or inside Svara_X with Shield Active.
- Place a regular cellular call from **Phone A** to **Phone B**.
- Observe Phone B: `SvaraCallScreeningService` intercepts the telecom event, logs the incoming number, and fires `RINGING`.
- Answer the call on Phone B and toggle **Speakerphone ON** (standard speakerphone protocol per Android privacy rules).
- From Phone A, speak: *"Hello, I am calling from your SBI bank manager office. Your account is blocked. Please read me the OTP sent to your SMS."*
- Observe Phone B: Speech recognition transcribes the audio, detects `BANK_IMPERSONATION`, `ACCOUNT_BLOCK_THREAT`, and `OTP_REQUEST`, and displays high-risk warning alerts in real-time.
- Disconnect call on Phone A: Phone B receives `IDLE`, saves the live record to History, and returns to monitoring.

### Step 15: Configure Protection Settings & Verify Reset
- Tap **"Settings"** from `MainActivity`.
- In `SettingsActivity`, verify all system status toggles (Telephony Role, Mic, Notifications).
- Toggle **Demo Mode** switch to verify setting preference persistence.
- Tap **"Clear Audit History"** and confirm that past records are wiped cleanly.
- Return to History to confirm the list is refreshed.

---

## Verification Matrix

| Step | Component Tested | Expected Outcome | Status |
| :--- | :--- | :--- | :--- |
| **P1-P4** | Telephony & Call Screening | Native `CallScreeningService` catches incoming calls, assigns Role | ✅ Verified |
| **P5-P6** | Foreground Service & STT | `CallMonitoringService` lifecycle bounds audio & speech recognition | ✅ Verified |
| **P7-P8** | Fraud Engine & Risk Formula | 14 categories matched via regex, weighted score 0-100% computed | ✅ Verified |
| **P9, P14**| Live In-Call Warning Screen | High-contrast meter, indicators, directives, and vibration alerts | ✅ Verified |
| **P10-P11**| Audio & Voice Analysis | Speakerphone PCM acquisition with zero-crossing acoustic heuristics | ✅ Verified |
| **P12** | History & Persistence | Records persisted to SharedPreferences with JSON serialization | ✅ Verified |
| **P13** | Protection Settings | Status checklist, demo mode toggle, and audit history reset | ✅ Verified |
