package com.svarax

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.svarax.call.CallStateManager
import com.svarax.permission.PermissionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MainActivity: Dashboard, Permission Coordinator & Live Call State Monitor.
 * Fulfills Priority 1 (compilation), Priority 2 (launch), Priority 3 (permissions & RoleManager),
 * and Priority 4 (Call screening & Telephony state detection).
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SvaraX_MainActivity"
    }

    // UI Elements
    private lateinit var tvProtectionStatusBadge: TextView
    private lateinit var tvActiveCallStatus: TextView
    private lateinit var tvLastScreenedCall: TextView
    private lateinit var tvScreeningDesc: TextView
    private lateinit var btnSetScreeningRole: Button
    private lateinit var badgeTelephonyPerm: TextView
    private lateinit var badgeMicPerm: TextView
    private lateinit var badgeNotificationPerm: TextView
    private lateinit var btnGrantAllPermissions: Button
    private lateinit var btnSimulateCallTest: Button
    private lateinit var tvTelephonyLogs: TextView
    private lateinit var cardProtectionStatus: MaterialCardView

    private val logHistory = StringBuilder()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // Activity Result Launcher for Runtime Permissions (Priority 3)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        logEvent(if (allGranted) "All requested runtime permissions granted." else "Some permissions were denied.")
        updatePermissionStatusViews()
    }

    // Activity Result Launcher for Call Screening Role Dialog (Priority 3 & 4)
    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            logEvent("RoleManager: Svara_X set as default Call Screening App.")
            Toast.makeText(this, "Call Screening Role Granted!", Toast.LENGTH_SHORT).show()
        } else {
            logEvent("RoleManager: Call Screening role dialog was dismissed or rejected.")
        }
        updatePermissionStatusViews()
    }

    // Listener for Call State transitions (Priority 4)
    private val callEventListener: (CallStateManager.CallEvent) -> Unit = { event ->
        runOnUiThread {
            handleCallStateChange(event)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()

        // Attach to CallStateManager (Priority 4)
        CallStateManager.addListener(callEventListener)
        CallStateManager.startListening(this)

        logEvent("Svara_X initialized. Telephony and Screening listener active.")
        updatePermissionStatusViews()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatusViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        CallStateManager.removeListener(callEventListener)
    }

    private fun initViews() {
        tvProtectionStatusBadge = findViewById(R.id.tvProtectionStatusBadge)
        tvActiveCallStatus = findViewById(R.id.tvActiveCallStatus)
        tvLastScreenedCall = findViewById(R.id.tvLastScreenedCall)
        tvScreeningDesc = findViewById(R.id.tvScreeningDesc)
        btnSetScreeningRole = findViewById(R.id.btnSetScreeningRole)
        badgeTelephonyPerm = findViewById(R.id.badgeTelephonyPerm)
        badgeMicPerm = findViewById(R.id.badgeMicPerm)
        badgeNotificationPerm = findViewById(R.id.badgeNotificationPerm)
        btnGrantAllPermissions = findViewById(R.id.btnGrantAllPermissions)
        btnSimulateCallTest = findViewById(R.id.btnSimulateCallTest)
        tvTelephonyLogs = findViewById(R.id.tvTelephonyLogs)
        cardProtectionStatus = findViewById(R.id.cardProtectionStatus)
    }

    private fun setupListeners() {
        // Request All System Permissions
        btnGrantAllPermissions.setOnClickListener {
            permissionLauncher.launch(PermissionHelper.REQUIRED_PERMISSIONS)
        }

        // Request Call Screening Role (Priority 4)
        btnSetScreeningRole.setOnClickListener {
            val roleIntent = PermissionHelper.getCallScreeningRoleIntent(this)
            if (roleIntent != null) {
                roleLauncher.launch(roleIntent)
            } else {
                Toast.makeText(
                    this,
                    "Call Screening Role is not available on this device version (Requires Android 10+).",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Priority 4 Simulation Button: Trigger Simulated Call Event
        var simStep = 0
        btnSimulateCallTest.setOnClickListener {
            val testNumber = "+91 98765 43210"
            when (simStep % 3) {
                0 -> {
                    logEvent("[SIMULATION] Inbound call event: RINGING from $testNumber")
                    CallStateManager.updateState(CallStateManager.State.RINGING, testNumber)
                    btnSimulateCallTest.text = "Simulate Call Answered (OFFHOOK)"
                }
                1 -> {
                    logEvent("[SIMULATION] Call answered: OFFHOOK (Active Call)")
                    CallStateManager.updateState(CallStateManager.State.OFFHOOK, testNumber)
                    btnSimulateCallTest.text = "Simulate Call Ended (IDLE)"
                }
                2 -> {
                    logEvent("[SIMULATION] Call ended: IDLE")
                    CallStateManager.updateState(CallStateManager.State.IDLE, testNumber)
                    btnSimulateCallTest.text = "Test Priority 4: Trigger Simulated Call Event"
                }
            }
            simStep++
        }
    }

    /**
     * Updates all status indicators and permission badges in the UI.
     */
    private fun updatePermissionStatusViews() {
        val phoneGranted = PermissionHelper.isPhoneStateGranted(this)
        val micGranted = PermissionHelper.isMicrophoneGranted(this)
        val notifGranted = PermissionHelper.isNotificationGranted(this)
        val roleHeld = PermissionHelper.isCallScreeningRoleHeld(this)

        // Telephony Permission Badge
        if (phoneGranted) {
            badgeTelephonyPerm.text = "✓ Granted"
            badgeTelephonyPerm.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
        } else {
            badgeTelephonyPerm.text = "⚠ Needed"
            badgeTelephonyPerm.setTextColor(ContextCompat.getColor(this, R.color.accent_amber))
        }

        // Microphone Permission Badge
        if (micGranted) {
            badgeMicPerm.text = "✓ Granted"
            badgeMicPerm.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
        } else {
            badgeMicPerm.text = "⚠ Needed"
            badgeMicPerm.setTextColor(ContextCompat.getColor(this, R.color.accent_amber))
        }

        // Notifications Permission Badge
        if (notifGranted) {
            badgeNotificationPerm.text = "✓ Granted"
            badgeNotificationPerm.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
        } else {
            badgeNotificationPerm.text = "⚠ Needed"
            badgeNotificationPerm.setTextColor(ContextCompat.getColor(this, R.color.accent_amber))
        }

        // Call Screening Role Status
        if (roleHeld) {
            tvScreeningDesc.text = "Active: Svara_X handles incoming call screening"
            btnSetScreeningRole.text = "Active ✓"
            btnSetScreeningRole.isEnabled = false
        } else {
            tvScreeningDesc.text = "Tap to set Svara_X as default Call Screening app"
            btnSetScreeningRole.text = "Enable"
            btnSetScreeningRole.isEnabled = true
        }

        // Overall Protection Status
        val isProtected = phoneGranted && micGranted && notifGranted
        if (isProtected) {
            tvProtectionStatusBadge.text = "🟢 ACTIVE"
            tvProtectionStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
            cardProtectionStatus.strokeColor = ContextCompat.getColor(this, R.color.shield_active_glow)
        } else {
            tvProtectionStatusBadge.text = "🟡 PENDING PERMISSIONS"
            tvProtectionStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_amber))
            cardProtectionStatus.strokeColor = ContextCompat.getColor(this, R.color.accent_amber)
        }
    }

    /**
     * Reacts to real or simulated CallStateManager state updates (Priority 4).
     */
    private fun handleCallStateChange(event: CallStateManager.CallEvent) {
        val number = event.incomingNumber ?: "Unknown"
        when (event.state) {
            CallStateManager.State.RINGING -> {
                tvActiveCallStatus.text = "📞 INCOMING CALL: $number"
                tvActiveCallStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_amber))
                tvLastScreenedCall.text = "Screening service intercepted incoming telephony call"
                logEvent("Screening Event: Incoming call from $number detected.")
            }
            CallStateManager.State.OFFHOOK -> {
                tvActiveCallStatus.text = "🔴 CALL ACTIVE: $number"
                tvActiveCallStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_red))
                tvLastScreenedCall.text = "Call in progress - Foreground monitoring engaged"
                logEvent("Call Offhook: Call connected with $number. Ready for audio pipeline.")
            }
            CallStateManager.State.IDLE -> {
                tvActiveCallStatus.text = "Idle: Monitoring for incoming telephony calls..."
                tvActiveCallStatus.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                tvLastScreenedCall.text = "Last screened call: $number (Ended)"
                logEvent("Call Idle: Telephony channel disconnected.")
            }
            CallStateManager.State.DISCONNECTED -> {
                tvActiveCallStatus.text = "Call Disconnected"
                logEvent("Call Disconnected.")
            }
        }
    }

    private fun logEvent(message: String) {
        val timestamp = timeFormat.format(Date())
        val logLine = "[$timestamp] $message\n"
        logHistory.insert(0, logLine)
        if (logHistory.length > 2000) {
            logHistory.setLength(2000)
        }
        tvTelephonyLogs.text = logHistory.toString()
        Log.d(TAG, message)
    }
}
