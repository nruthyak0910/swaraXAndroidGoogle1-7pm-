package com.svarax.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.svarax.R
import com.svarax.call.CallStateManager
import com.svarax.history.CallHistoryRepository
import com.svarax.permission.PermissionHelper
import com.svarax.service.CallMonitoringService

/**
 * SettingsActivity: Allows configuring Scam Protection settings,
 * launching Physical Device Diagnostics, triggering controlled demo scam simulations,
 * inspecting active Android permissions, and managing audit history.
 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        const val PREFS_SETTINGS = "svara_protection_settings"
        const val KEY_DEMO_MODE = "key_demo_mode_enabled"
    }

    private lateinit var btnBackSettings: ImageButton
    private lateinit var btnOpenDiagnostics: Button
    private lateinit var btnOpenCellularCapability: Button
    private lateinit var switchDemoMode: Switch
    private lateinit var btnRunDemoSimulation: Button
    private lateinit var btnStopDemoSimulation: Button
    private lateinit var btnSeedDemoHistory: Button
    private lateinit var tvSettingsPhoneStatus: TextView
    private lateinit var tvSettingsMicStatus: TextView
    private lateinit var tvSettingsNotificationStatus: TextView
    private lateinit var tvSettingsOverlayStatus: TextView
    private lateinit var tvSettingsRoleStatus: TextView
    private lateinit var btnRequestAllPermissions: Button
    private lateinit var btnClearHistory: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        btnBackSettings = findViewById(R.id.btnBackSettings)
        btnOpenDiagnostics = findViewById(R.id.btnOpenDiagnostics)
        btnOpenCellularCapability = findViewById(R.id.btnOpenCellularCapability)
        switchDemoMode = findViewById(R.id.switchDemoMode)
        btnRunDemoSimulation = findViewById(R.id.btnRunDemoSimulation)
        btnStopDemoSimulation = findViewById(R.id.btnStopDemoSimulation)
        btnSeedDemoHistory = findViewById(R.id.btnSeedDemoHistory)
        tvSettingsPhoneStatus = findViewById(R.id.tvSettingsPhoneStatus)
        tvSettingsMicStatus = findViewById(R.id.tvSettingsMicStatus)
        tvSettingsNotificationStatus = findViewById(R.id.tvSettingsNotificationStatus)
        tvSettingsOverlayStatus = findViewById(R.id.tvSettingsOverlayStatus)
        tvSettingsRoleStatus = findViewById(R.id.tvSettingsRoleStatus)
        btnRequestAllPermissions = findViewById(R.id.btnRequestAllPermissions)
        btnClearHistory = findViewById(R.id.btnClearHistory)

        val prefs = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        // Default to false so live calls are treated as real calls by default
        val isDemo = prefs.getBoolean(KEY_DEMO_MODE, false)
        switchDemoMode.isChecked = isDemo

        btnBackSettings.setOnClickListener { finish() }

        btnOpenDiagnostics.setOnClickListener {
            startActivity(Intent(this, DiagnosticsActivity::class.java))
        }

        btnOpenCellularCapability.setOnClickListener {
            startActivity(Intent(this, DiagnosticsActivity::class.java))
        }

        switchDemoMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_DEMO_MODE, isChecked).apply()
            Toast.makeText(
                this,
                if (isChecked) "Demo mode enabled" else "Live production mode enabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnRunDemoSimulation.setOnClickListener {
            val demoNumber = "[DEMO] +91 98230 11942"
            CallStateManager.updateState(CallStateManager.State.RINGING, demoNumber)
            CallStateManager.updateState(CallStateManager.State.OFFHOOK, demoNumber)

            val serviceIntent = Intent(this, CallMonitoringService::class.java).apply {
                putExtra(CallMonitoringService.EXTRA_PHONE_NUMBER, demoNumber)
                putExtra(CallMonitoringService.EXTRA_IS_DEMO, true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            val liveIntent = Intent(this, LiveCallActivity::class.java).apply {
                putExtra("caller_number", demoNumber)
            }
            startActivity(liveIntent)
        }

        btnStopDemoSimulation.setOnClickListener {
            val num = CallStateManager.getLastIncomingNumber()
            CallStateManager.updateState(CallStateManager.State.IDLE, num)
            val serviceIntent = Intent(this, CallMonitoringService::class.java)
            stopService(serviceIntent)
            Toast.makeText(this, "Active simulation terminated", Toast.LENGTH_SHORT).show()
        }

        btnSeedDemoHistory.setOnClickListener {
            CallHistoryRepository(this).seedDemoAuditRecords()
            Toast.makeText(this, "Sample [DEMO] records loaded for presentation", Toast.LENGTH_SHORT).show()
        }

        btnRequestAllPermissions.setOnClickListener {
            if (!PermissionHelper.isAllRequiredPermissionsGranted(this)) {
                requestPermissions(PermissionHelper.REQUIRED_PERMISSIONS, 101)
            } else if (!PermissionHelper.isOverlayGranted(this)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            } else {
                Toast.makeText(this, "All permissions already granted", Toast.LENGTH_SHORT).show()
            }
        }

        btnClearHistory.setOnClickListener {
            CallHistoryRepository(this).clearHistory()
            Toast.makeText(this, "Call audit logs cleared", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val phoneOk = PermissionHelper.isPhoneStateGranted(this)
        val micOk = PermissionHelper.isMicrophoneGranted(this)
        val notifOk = PermissionHelper.isNotificationGranted(this)
        val overlayOk = PermissionHelper.isOverlayGranted(this)
        val roleOk = PermissionHelper.isCallScreeningRoleHeld(this)

        tvSettingsPhoneStatus.text = if (phoneOk) "• Phone State: GRANTED ✓" else "• Phone State: NOT GRANTED ✗"
        tvSettingsPhoneStatus.setTextColor(if (phoneOk) Color.parseColor("#4ADE80") else Color.parseColor("#F87171"))

        tvSettingsMicStatus.text = if (micOk) "• Microphone: GRANTED ✓" else "• Microphone: NOT GRANTED ✗"
        tvSettingsMicStatus.setTextColor(if (micOk) Color.parseColor("#4ADE80") else Color.parseColor("#F87171"))

        tvSettingsNotificationStatus.text = if (notifOk) "• Notifications: GRANTED ✓" else "• Notifications: NOT GRANTED ✗"
        tvSettingsNotificationStatus.setTextColor(if (notifOk) Color.parseColor("#4ADE80") else Color.parseColor("#F87171"))

        tvSettingsOverlayStatus.text = if (overlayOk) "• Display Over Other Apps: GRANTED ✓" else "• Display Over Other Apps: NOT GRANTED ✗"
        tvSettingsOverlayStatus.setTextColor(if (overlayOk) Color.parseColor("#4ADE80") else Color.parseColor("#F87171"))

        tvSettingsRoleStatus.text = if (roleOk) "• Call Screening Role: ACTIVE ✓" else "• Call Screening Role: INACTIVE ✗"
        tvSettingsRoleStatus.setTextColor(if (roleOk) Color.parseColor("#4ADE80") else Color.parseColor("#FBBF24"))
    }
}
