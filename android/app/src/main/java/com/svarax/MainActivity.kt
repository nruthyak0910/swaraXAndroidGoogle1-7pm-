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
import com.svarax.service.CallMonitoringService
import com.svarax.ui.CallHistoryActivity
import com.svarax.ui.LiveCallActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MainActivity: Dashboard, Permission Coordinator & Live Call State Monitor.
 * Fulfills Priorities 1 through 12.
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
    private lateinit var badgeAllPerms: TextView
    private lateinit var btnGrantAllPermissions: Button
    private lateinit var btnStartDemoScam: Button
    private lateinit var btnOpenLiveCallUI: Button
    private lateinit var btnStopDemoCall: Button
    private lateinit var btnViewHistory: Button
    private lateinit var btnOpenSettings: Button
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

    // Listener for Call State transitions (Priority 4 & 5)
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
        badgeAllPerms = findViewById(R.id.badgeAllPerms)
        btnGrantAllPermissions = findViewById(R.id.btnGrantAllPermissions)
        btnStartDemoScam = findViewById(R.id.btnStartDemoScam)
        btnOpenLiveCallUI = findViewById(R.id.btnOpenLiveCallUI)
        btnStopDemoCall = findViewById(R.id.btnStopDemoCall)
        btnViewHistory = findViewById(R.id.btnViewHistory)
        btnOpenSettings = findViewById(R.id.btnOpenSettings)
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

        // Priority 6 & 18: Launch Demo Simulation Mode ("Bank OTP Scam")
        btnStartDemoScam.setOnClickListener {
            val demoNumber = "+91 98765 43210"
            logEvent("[DEMO STARTED] Simulating incoming fraudulent call from $demoNumber")

            CallStateManager.updateState(CallStateManager.State.RINGING, demoNumber)
            CallStateManager.updateState(CallStateManager.State.OFFHOOK, demoNumber)

            val serviceIntent = Intent(this, CallMonitoringService::class.java).apply {
                putExtra(CallMonitoringService.EXTRA_PHONE_NUMBER, demoNumber)
                putExtra(CallMonitoringService.EXTRA_IS_DEMO, true)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            // Launch Live In-Call Warning Screen
            val liveIntent = Intent(this, LiveCallActivity::class.java).apply {
                putExtra("caller_number", demoNumber)
            }
            startActivity(liveIntent)
        }

        // Open Live In-Call Risk Warning Screen
        btnOpenLiveCallUI.setOnClickListener {
            val liveIntent = Intent(this, LiveCallActivity::class.java).apply {
                putExtra("caller_number", "+91 98765 43210")
            }
            startActivity(liveIntent)
        }

        // End / Stop Demo Call
        btnStopDemoCall.setOnClickListener {
            logEvent("[DEMO STOPPED] Terminating active call simulation.")
            CallStateManager.updateState(CallStateManager.State.IDLE, null)
            stopService(Intent(this, CallMonitoringService::class.java))
            Toast.makeText(this, "Call simulation ended. Log saved to History.", Toast.LENGTH_SHORT).show()
        }

        // View Call Analysis History (Priority 12)
        btnViewHistory.setOnClickListener {
            startActivity(Intent(this, CallHistoryActivity::class.java))
        }

        // Open Protection Settings Screen
        btnOpenSettings.setOnClickListener {
            startActivity(Intent(this, com.svarax.ui.SettingsActivity::class.java))
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

        val isProtected = phoneGranted && micGranted && notifGranted

        if (isProtected) {
            badgeAllPerms.text = "✓ Granted"
            badgeAllPerms.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
            tvProtectionStatusBadge.text = "🟢 ACTIVE"
            tvProtectionStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
            cardProtectionStatus.strokeColor = ContextCompat.getColor(this, R.color.shield_active_glow)
        } else {
            badgeAllPerms.text = "⚠ Action Needed"
            badgeAllPerms.setTextColor(ContextCompat.getColor(this, R.color.accent_amber))
            tvProtectionStatusBadge.text = "🟡 PENDING PERMISSIONS"
            tvProtectionStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_amber))
            cardProtectionStatus.strokeColor = ContextCompat.getColor(this, R.color.accent_amber)
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
    }

    /**
     * Reacts to real or simulated CallStateManager state updates (Priority 4 & 5).
     */
    private fun handleCallStateChange(event: CallStateManager.CallEvent) {
        val number = event.incomingNumber ?: "Unknown"
        when (event.state) {
            CallStateManager.State.RINGING -> {
                tvActiveCallStatus.text = "📞 INCOMING CALL: $number"
                tvActiveCallStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_amber))
                tvLastScreenedCall.text = "Screening service intercepted incoming telephony call"
                logEvent("Screening Event: Incoming call from $number intercepted.")
            }
            CallStateManager.State.OFFHOOK -> {
                tvActiveCallStatus.text = "🔴 CALL ACTIVE: $number"
                tvActiveCallStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_red))
                tvLastScreenedCall.text = "Call in progress - AI Fraud Protection pipeline active"
                logEvent("Call Connected: $number. Monitoring conversational transcript & acoustic spectrum.")
            }
            CallStateManager.State.IDLE -> {
                tvActiveCallStatus.text = "Idle: Monitoring for incoming telephony calls..."
                tvActiveCallStatus.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                tvLastScreenedCall.text = "Last call ended. Saved to History."
                logEvent("Call Disconnected: IDLE state reached. Analysis finalized.")
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
