# Svara_X: Physical Android Device Validation Checklist

**Document Version:** 1.0  
**Target Platform:** Android 10+ (API Level 29–34)  
**Target Package:** `com.svarax.debug` / `com.svarax`  
**Execution Environment:** Physical Android Smartphone (Target Phone B) + Test Caller (Phone A)

---

## Critical Android Platform Restrictions & Architectural Reality Checks

Before executing this test suite, the tester must account for the following documented Android OS security and hardware constraints:

1. **In-Call Remote Audio Capture Restriction (`VOICE_CALL` / `VOICE_DOWNLINK`):**  
   Since Android 10 (API 29), Google strictly prohibits third-party non-system apps from recording the remote party's cellular audio stream directly (`AudioSource.VOICE_CALL` and `AudioSource.VOICE_DOWNLINK` require privileged `android.permission.CAPTURE_AUDIO_OUTPUT`, reserved exclusively for system apps and system dialers).  
   *Operational Requirement:* For live cellular calls on third-party non-OEM apps, the user must turn **Speakerphone ON**. The app's `MicrophoneAudioInput` captures acoustic output via `AudioSource.MIC`.

2. **Telephony Audio Focus Lock:**  
   During an active cellular call, the Android OS puts `AudioManager` into `MODE_IN_CALL` or `MODE_IN_COMMUNICATION`. On certain OEM chipsets (e.g., MediaTek or aggressive OEM ROMs), the hardware mic is locked exclusively by the telephony modem. If the device does not support concurrent microphone reading, `AudioRecord` will output silence (all zeros) or fail initialization.

3. **Concurrency Limitations of `android.speech.SpeechRecognizer`:**  
   Android's native `SpeechRecognizer` often halts or throws `ERROR_AUDIO` (error code 3) if `AudioManager` is in `MODE_IN_CALL` without continuous audio focus management. Svara_X provides both `AndroidSpeechToText` (for real STT) and `DemoSpeechToText` (for deterministic simulation and engine validation).

4. **Background Activity Launch Restrictions:**  
   Android 10+ blocks background services from calling `startActivity(intent)`. Svara_X uses a two-pronged mechanism: a high-priority Heads-Up Notification with a full-screen `PendingIntent`, and requires the user to grant `SYSTEM_ALERT_WINDOW` ("Display over other apps") to display the in-call warning screen.

---

## Section 1: Standard 20-Point Component & Lifecycle Test Suite

### TEST 1: Application Installation
* **TEST NUMBER:** TEST-01
* **ACTION:** Connect physical Android device via USB with ADB debugging enabled. Run `./gradlew installDebug` or install the compiled APK via `adb install -r app-debug.apk`.
* **EXPECTED RESULT:** Package `com.svarax.debug` installs successfully without `INSTALL_FAILED_CONFLICTING_PROVIDER`, manifest syntax errors, or duplicate class failures. App icon appears in the Android application launcher.
* **ACTUAL RESULT TO RECORD:** `[Record ADB terminal output and whether app icon appears on phone screen]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 2: Required Permissions
* **TEST NUMBER:** TEST-02
* **ACTION:** Launch Svara_X. On the dashboard, observe initial permission status, then tap **"Review & Grant Permissions"**. In the sequential Android OS dialogs, grant: Phone State, Audio Recording, and Notifications.
* **EXPECTED RESULT:** The system dialogs appear cleanly in order. After granting, `PermissionHelper.isAllRequiredPermissionsGranted()` returns `true`. The UI badge updates to green `Granted ✓` for all items.
* **ACTUAL RESULT TO RECORD:** `[Record which permissions prompted, if any were denied, and final badge state in MainActivity]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 3: Call-Screening Role Acquisition
* **TEST NUMBER:** TEST-03
* **ACTION:** On the dashboard, navigate to the **Call Screening Service Role** card and tap **"Enable"**. When the system `RoleManager` dialog appears (*"Set Svara_X as your default Caller ID & spam app"*), tap **Svara_X** and confirm.
* **EXPECTED RESULT:** Android's `RoleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)` opens the system selector. After user confirmation, `roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)` returns `true`. Button changes to `Active ✓` and is disabled.
* **ACTUAL RESULT TO RECORD:** `[Record whether system dialog appeared and whether role status flipped to Active in UI and logcat]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 4: Incoming Call Interception
* **TEST NUMBER:** TEST-04
* **ACTION:** With the app running or in the background and the Call Screening Role granted, place an incoming cellular phone call from Phone A to Target Phone B.
* **EXPECTED RESULT:** `SvaraCallScreeningService.onScreenCall(callDetails)` is invoked by the Android Telecom subsystem. The caller phone number is extracted. Logcat displays: `[SvaraCallScreening] Incoming call detected from: <Phone A Number>`. Call is allowed to ring without being dropped.
* **ACTUAL RESULT TO RECORD:** `[Record logcat output tagged SvaraCallScreening with incoming phone number]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 5: Telephony Call Lifecycle (RINGING → ACTIVE → DISCONNECTED)
* **TEST NUMBER:** TEST-05
* **ACTION:** 
  1. Receive incoming call from Phone A (Phone B rings).
  2. Answer the call on Phone B.
  3. Speak for 15 seconds, then hang up on Phone A.
* **EXPECTED RESULT:** 
  1. `CallStateReceiver` or `TelephonyCallback` registers `TelephonyManager.CALL_STATE_RINGING`.
  2. Answering registers `TelephonyManager.CALL_STATE_OFFHOOK` (ACTIVE).
  3. Hang-up registers `TelephonyManager.CALL_STATE_IDLE` (DISCONNECTED).
  `CallStateManager` transitions state predictably without dropping lifecycle events.
* **ACTUAL RESULT TO RECORD:** `[Record logcat tags CallStateReceiver / CallStateManager showing the exact 3-state transition]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 6: CallMonitoringService Startup
* **TEST NUMBER:** TEST-06
* **ACTION:** When the call enters `CALL_STATE_OFFHOOK`, inspect the notification drawer and running services via `adb shell dumpsys activity services com.svarax`.
* **EXPECTED RESULT:** `CallMonitoringService` starts as a foreground service via `ContextCompat.startForegroundService()`. A persistent notification titled *"Svara_X Active Call Protection"* appears in the notification drawer displaying `"Monitoring call in real-time"`.
* **ACTUAL RESULT TO RECORD:** `[Record notification appearance and dumpsys output confirming foreground service state]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 7: CallMonitoringService Shutdown
* **TEST NUMBER:** TEST-07
* **ACTION:** When the call terminates (`CALL_STATE_IDLE`), observe the notification drawer and service state.
* **EXPECTED RESULT:** `CallMonitoringService.onDestroy()` or `stopSelf()` is executed. The persistent foreground notification is immediately removed from the notification shade. Audio resources and wake-locks are released.
* **ACTUAL RESULT TO RECORD:** `[Record timestamp when notification dismissed and logcat entry confirming service teardown]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 8: Microphone AudioRecord Initialization
* **TEST NUMBER:** TEST-08
* **ACTION:** Trigger active call monitoring with Speakerphone ON. Inspect logcat for `MicrophoneAudioInput` initialization.
* **EXPECTED RESULT:** `AudioRecord` initializes with `AudioSource.MIC`, sample rate `16000Hz`, `CHANNEL_IN_MONO`, `ENCODING_PCM_16BIT`. `audioRecord.state == AudioRecord.STATE_INITIALIZED`. Recording starts without `IllegalStateException`.
* **ACTUAL RESULT TO RECORD:** `[Record logcat confirming AudioRecord state and buffer size]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 9: AudioRecord Receiving Non-Zero Audio Samples
* **TEST NUMBER:** TEST-09
* **ACTION:** While the call is active on Speakerphone, speak continuously into Phone A. Monitor logcat output from `MicrophoneAudioInput` and `VoiceAnalyzer`.
* **EXPECTED RESULT:** `AudioRecord.read(buffer, 0, buffer.size)` returns positive byte counts (`> 0`). RMS energy calculation shows amplitude above ambient noise floor (`RMS > 50`), confirming buffer contains genuine non-zero PCM voice waveforms rather than flatline zeros.
* **ACTUAL RESULT TO RECORD:** `[Record sample RMS energy values and confirmed non-zero byte array counts from logcat]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 10: Speech-To-Text Receiving Usable Speech
* **TEST NUMBER:** TEST-10
* **ACTION:** With active audio capture, speak clear English into Phone A: *"Hello this is a test of voice transmission"*. Monitor `AndroidSpeechToText` or STT pipeline callbacks.
* **EXPECTED RESULT:** Speech recognition engine captures acoustic stream, detects voice activity (`onBeginningOfSpeech`), and triggers `onResults` or `onPartialResults` with non-empty text strings.
* **ACTUAL RESULT TO RECORD:** `[Record actual transcribed string returned by SpeechRecognizer callbacks in logcat]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 11: TranscriptChunk Generation
* **TEST NUMBER:** TEST-11
* **ACTION:** Continue speaking phrases through the call. Observe `TranscriptChunk` emissions.
* **EXPECTED RESULT:** Transcribed text is wrapped into a structured `TranscriptChunk(text, timestamp, isFinal)` data object and emitted to listeners via callback/flow.
* **ACTUAL RESULT TO RECORD:** `[Record timestamped TranscriptChunk objects emitted in logcat]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 12: FraudDetector Receiving Transcript Chunks
* **TEST NUMBER:** TEST-12
* **ACTION:** Speak a phrase containing banking terminology: *"This is SBI Bank security department calling about your debit card"*.
* **EXPECTED RESULT:** `FraudDetector.analyze(transcript)` is called with the exact transcript string. Logcat confirms `FraudDetector` evaluating the incoming text against the 14 regex patterns.
* **ACTUAL RESULT TO RECORD:** `[Record logcat entry showing FraudDetector.analyze called with the input transcript]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 13: Fraud Indicators Generation
* **TEST NUMBER:** TEST-13
* **ACTION:** Transmit the phrase: *"Please give me your 6 digit OTP immediately or your account will be blocked"*.
* **EXPECTED RESULT:** `FraudDetector.analyze()` outputs a list containing at least:
  - `FraudCategory.OTP_REQUEST`
  - `FraudCategory.ACCOUNT_BLOCK_THREAT`
  - `FraudCategory.URGENCY_PRESSURE`
  Each with confidence scores (`> 0.85`) and specific evidence descriptions.
* **ACTUAL RESULT TO RECORD:** `[Record exact list of FraudIndicator objects generated with confidence and evidence]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 14: RiskEngine Receiving Indicators
* **TEST NUMBER:** TEST-14
* **ACTION:** Pass the generated `List<FraudIndicator>` into `RiskEngine.evaluate(indicators, voiceAnalysis)`.
* **EXPECTED RESULT:** `RiskEngine` accepts the indicator list, matches each category to its calibrated weight, and applies the scoring algorithm without throwing exceptions.
* **ACTUAL RESULT TO RECORD:** `[Record logcat showing RiskEngine input indicators]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 15: Risk Score Dynamic Escalation
* **TEST NUMBER:** TEST-15
* **ACTION:** Feed progressive conversational stages:
  1. Greeting: *"Hello good morning."*
  2. Claim: *"I am calling from your bank branch."*
  3. Threat: *"Your account is blocked today."*
  4. Credential theft: *"Share your OTP right now."*
* **EXPECTED RESULT:** Risk score dynamically increases:
  1. Greeting ➔ `< 20% (LOW)`
  2. Impersonation ➔ `30% - 40% (MEDIUM)`
  3. Threat ➔ `60% - 75% (HIGH)`
  4. OTP Request ➔ `90% - 98% (CRITICAL)`
* **ACTUAL RESULT TO RECORD:** `[Record numeric risk score progression: Step 1 %, Step 2 %, Step 3 %, Step 4 %]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 16: AlertManager Generating Android Warning
* **TEST NUMBER:** TEST-16
* **ACTION:** When risk score crosses 80% (`CRITICAL`), inspect device behavior and notification drawer.
* **EXPECTED RESULT:** `AlertManager` issues high-priority notification with `NotificationCompat.PRIORITY_MAX`, alarm sound, and vibration pattern `[0, 400, 200, 400]`. Phone physically vibrates. Red warning banner appears at the top of the screen.
* **ACTUAL RESULT TO RECORD:** `[Record whether phone physically vibrated, alert sound fired, and heads-up banner appeared]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 17: LiveCallActivity Displaying Actual RiskResult
* **TEST NUMBER:** TEST-17
* **ACTION:** Open or view `LiveCallActivity` during active analysis.
* **EXPECTED RESULT:** UI displays:
  - Big percentage text matching calculated score (e.g. `94%`).
  - Colored risk badge (`CRITICAL RISK` in red `#EF4444`).
  - Dynamic progress bar filled to the exact percentage.
  - List of detected indicators with evidence descriptions.
  - Clear directive: *"DO NOT SHARE OTP OR PIN! HANG UP IMMEDIATELY"*.
* **ACTUAL RESULT TO RECORD:** `[Record UI elements rendered on physical device screen: score, badge text, color, recommendation]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 18: Call History Saved After Disconnection
* **TEST NUMBER:** TEST-18
* **ACTION:** Terminate the call. Open `CallHistoryActivity` from `MainActivity`.
* **EXPECTED RESULT:** A new `CallRecord` is saved to `CallHistoryRepository` via `SharedPreferences`. The top list item displays:
  - Phone number
  - Timestamp
  - Call duration in seconds
  - Risk badge (e.g., `CRITICAL 94%`)
  - List of detected indicators
* **ACTUAL RESULT TO RECORD:** `[Record details of the persisted record shown in CallHistoryActivity]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 19: Demo Mode Execution
* **TEST NUMBER:** TEST-19
* **ACTION:** On `MainActivity`, tap **"▶ Run Multi-Stage Scam Simulation"** without making an actual cellular call.
* **EXPECTED RESULT:** `DemoSpeechToText` runs an automated 4-stage scripted bank scam. Simulated call transitions `RINGING` ➔ `OFFHOOK` ➔ streams progressive chunks ➔ triggers `CRITICAL` alert ➔ saves demo record tagged `[DEMO]` to history upon completion.
* **ACTUAL RESULT TO RECORD:** `[Record end-to-end simulation behavior, timing between stages, and final recorded score]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### TEST 20: Failure Behavior (Mic / STT Unavailable)
* **TEST NUMBER:** TEST-20
* **ACTION:** 
  1. Revoke Microphone permission in Android App Settings (`pm revoke com.svarax.debug android.permission.RECORD_AUDIO`).
  2. Launch app and attempt to start active call monitoring.
* **EXPECTED RESULT:** App does NOT crash. `CallMonitoringService` checks permission before initializing `AudioRecord`. Graceful warning is logged or displayed via Toast/Notification informing user that audio protection is suspended until permission is granted.
* **ACTUAL RESULT TO RECORD:** `[Record app behavior, logcat error handling, and verify absence of fatal crash (SIGSEGV / IllegalStateException)]`
* **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

## Section 2: Isolated Subsystem Validation Suites

### SUITE A: Call Detection Only (No Audio / STT Dependency)
*Goal: Prove native Telecom and TelephonyManager integration works independently.*

* **TEST A-1: RoleManager Registration**
  * **ACTION:** Verify `RoleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)` via ADB: `adb shell dumpsys role | grep -A 5 android.app.role.CALL_SCREENING`.
  * **EXPECTED RESULT:** `com.svarax.debug` is listed as the role holder.
  * **ACTUAL RESULT TO RECORD:** `[Dumpsys role output]`
  * **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

* **TEST A-2: SvaraCallScreeningService Execution**
  * **ACTION:** Call Phone B from Phone A.
  * **EXPECTED RESULT:** `onScreenCall` is called. Logcat shows: `[SvaraCallScreeningService] Intercepted call from <Phone A Number>`. Call is allowed to ring without call blocking interference.
  * **ACTUAL RESULT TO RECORD:** `[Logcat output]`
  * **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

* **TEST A-3: BroadcastReceiver Telephony Transitions**
  * **ACTION:** Answer then hang up.
  * **EXPECTED RESULT:** `CallStateReceiver` receives `EXTRA_STATE_RINGING`, `EXTRA_STATE_OFFHOOK`, and `EXTRA_STATE_IDLE`.
  * **ACTUAL RESULT TO RECORD:** `[Logcat state transitions]`
  * **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### SUITE B: Audio Capture Only (No STT / Fraud Engine Dependency)
*Goal: Prove physical microphone hardware captures valid PCM audio on the device.*

* **TEST B-1: AudioRecord Buffer Allocation**
  * **ACTION:** Launch audio recording test harness with `AudioSource.MIC`, 16000Hz, Mono, 16-bit PCM.
  * **EXPECTED RESULT:** Buffer size calculated via `AudioRecord.getMinBufferSize()` is valid (typically 1280 to 4096 bytes). `AudioRecord.getState() == STATE_INITIALIZED`.
  * **ACTUAL RESULT TO RECORD:** `[Calculated buffer size in bytes and state]`
  * **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

* **TEST B-2: Silence vs. Voice Dynamic Range Test**
  * **ACTION:** Run `MicrophoneAudioInput` for 10 seconds in a quiet room, then speak loudly for 10 seconds.
  * **EXPECTED RESULT:** Quiet period returns RMS values `< 20`. Speaking period returns RMS values `> 120`. Confirms mic is active and dynamic range is functional.
  * **ACTUAL RESULT TO RECORD:** `[Recorded baseline RMS vs speaking RMS]`
  * **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

* **TEST B-3: In-Call Speakerphone Mic Acquisition**
  * **ACTION:** Place real call, enable speakerphone, read `AudioRecord` buffer.
  * **EXPECTED RESULT:** `AudioRecord.read()` does NOT return `ERROR_INVALID_OPERATION` or all-zero arrays while phone is off-hook.
  * **ACTUAL RESULT TO RECORD:** `[Read return code and non-zero byte verification]`
  * **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### SUITE C: Speech Recognition Only (Isolated Speech Engine)
*Goal: Prove Android SpeechRecognizer engine converts audio to text independently.*

* **TEST C-1: SpeechRecognizer Availability Check**
  * **ACTION:** Execute `SpeechRecognizer.isRecognitionAvailable(context)`.
  * **EXPECTED RESULT:** Returns `true`. (Google Speech Services / On-Device Speech Recognition is installed and active).
  * **ACTUAL RESULT TO RECORD:** `[Boolean return value and default recognizer package name]`
  * **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

* **TEST C-2: Standalone Speech Transcription**
  * **ACTION:** With no active phone call, invoke `AndroidSpeechToText.startListening()` and speak: *"Testing fraud prevention speech engine"*.
  * **EXPECTED RESULT:** `onResults` callback delivers bundle containing String array with *"testing fraud prevention speech engine"*.
  * **ACTUAL RESULT TO RECORD:** `[Transcribed text returned]`
  * **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

* **TEST C-3: Ambient Noise Rejection**
  * **ACTION:** Play background music or office noise without speaking.
  * **EXPECTED RESULT:** Recognizer does not emit spurious fraud keywords; timeouts or empty results handled cleanly without crashing.
  * **ACTUAL RESULT TO RECORD:** `[Behavior and recognizer log]`
  * **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### SUITE D: Fraud Detection Using Scripted Transcripts (Pure Logic)
*Goal: Prove pattern matching, category classification, and risk scoring accuracy without hardware variables.*

* **TEST D-1: Bank Impersonation + Account Threat + OTP**
  * **ACTION:** Feed transcript: *"This is Axis Bank support. Your account is blocked immediately. Share your 6 digit OTP to verify."*
  * **EXPECTED RESULT:** 
    - Detected: `BANK_IMPERSONATION`, `ACCOUNT_BLOCK_THREAT`, `URGENCY_PRESSURE`, `OTP_REQUEST`
    - Risk Score: `>= 90% (CRITICAL)`
    - Recommendation: `"DO NOT SHARE OTP OR PIN! HANG UP IMMEDIATELY. OFFICIAL BANKS NEVER ASK FOR PASSWORDS."`
  * **ACTUAL RESULT TO RECORD:** `[Computed score and recommendation]`
  * **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

* **TEST D-2: Remote Access Scam**
  * **ACTION:** Feed transcript: *"Your KYC has expired. Download AnyDesk or TeamViewer immediately so I can verify your documents."*
  * **EXPECTED RESULT:**
    - Detected: `REMOTE_ACCESS_APP_DEMAND`, `KYC_UPDATE_DEADLINE`, `URGENCY_PRESSURE`
    - Risk Score: `>= 80% (CRITICAL)`
    - Recommendation: `"DO NOT INSTALL ANY SCREEN-SHARING APP! DISCONNECT THE CALL NOW."`
  * **ACTUAL RESULT TO RECORD:** `[Computed score and recommendation]`
  * **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

* **TEST D-3: Digital Arrest Scam**
  * **ACTION:** Feed transcript: *"This is Mumbai Police Cyber Cell. A digital arrest warrant is issued against your Aadhaar card for money laundering. Do not disconnect this call."*
  * **EXPECTED RESULT:**
    - Detected: `POLICE_GOVERNMENT_IMPERSONATION`, `DIGITAL_ARREST_THREAT`, `SECRECY_ISOLATION_DEMAND`
    - Risk Score: `>= 85% (CRITICAL)`
    - Recommendation: `"DIGITAL ARREST IS A SCAM. POLICE NEVER CONDUCT ARRESTS VIA PHONE OR VIDEO CALL. HANG UP."`
  * **ACTUAL RESULT TO RECORD:** `[Computed score and recommendation]`
  * **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

* **TEST D-4: Benign Normal Conversation**
  * **ACTION:** Feed transcript: *"Hey, are we still meeting for lunch at 1 PM today? Let me know if you want Italian or Mexican."*
  * **EXPECTED RESULT:**
    - Detected: `None`
    - Risk Score: `<= 10% (LOW)`
    - Recommendation: `"No immediate fraud pattern detected. Maintain standard vigilance."`
  * **ACTUAL RESULT TO RECORD:** `[Computed score and recommendation]`
  * **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`

---

### SUITE E: Complete Live End-to-End Pipeline
*Goal: Validate complete integration across cellular network, telecom interception, live speakerphone audio, speech recognition, fraud scoring, heads-up alerting, and history storage.*

* **TEST E-1: Real Call Bank Scam Progression**
  * **SETUP:** Target Phone B has Svara_X running with all permissions and Call Screening Role granted. Caller Phone A is ready to place cellular call.
  * **STEP 1:** Phone A calls Phone B.
    * *Verification:* Phone B rings. `SvaraCallScreeningService` logs incoming call.
  * **STEP 2:** Phone B answers and toggles **Speakerphone ON**.
    * *Verification:* `CallStateReceiver` detects `OFFHOOK`. `CallMonitoringService` starts in foreground. In-call warning UI launches.
  * **STEP 3:** Speaker on Phone A says: *"Hello, I am calling from State Bank of India fraud prevention desk."*
    * *Verification:* In-call UI shows transcript chunk. Risk updates to ~35% (`MEDIUM`).
  * **STEP 4:** Speaker on Phone A says: *"Your debit card has been suspended due to suspicious activity. Your account will be permanently blocked within 10 minutes."*
    * *Verification:* Risk escalates to ~68% (`HIGH`). Warning amber border appears.
  * **STEP 5:** Speaker on Phone A says: *"I have sent a 6-digit verification code to your mobile. Read me that OTP right now to unblock your account."*
    * *Verification:* Risk jumps to `>= 94% (CRITICAL)`. Screen border turns bright red. Recommendation displays: *"DO NOT SHARE OTP OR PIN!"*. Device vibrates with warning notification.
  * **STEP 6:** Phone A disconnects call.
    * *Verification:* `CALL_STATE_IDLE` triggered. Monitoring service stops cleanly. Notification dismissed. `CallRecord` saved.
  * **STEP 7:** Open History tab on Phone B.
    * *Verification:* Call from Phone A is listed with duration, `94% CRITICAL`, and all 4 indicators.
  * **ACTUAL RESULT TO RECORD:** `[Record full end-to-end outcome, timestamps of state changes, and photos/screenshots of the alert screen]`
  * **PASS/FAIL:** `[PENDING PHYSICAL EXECUTION]`
