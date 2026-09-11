package com.svarax

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.svarax.call.CallStateManager
import com.svarax.history.CallHistoryRepository
import com.svarax.history.CallRecord
import com.svarax.permission.PermissionHelper
import com.svarax.service.CallMonitoringService
import com.svarax.ui.CallHistoryActivity
import com.svarax.ui.LiveCallActivity
import com.svarax.ui.SettingsActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MainActivity: Production Cybersecurity Dashboard.
 * Clean, consumer-facing protection status, security features audit,
 * active in-call banner, recent activity feeds, and bottom navigation.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SvaraX_MainActivity"
    }

    // Hero Status Views
    private lateinit var cardProtectionStatus: MaterialCardView
    private lateinit var imgStatusShield: ImageView
    private lateinit var tvProtectionTitle: TextView
    private lateinit var tvProtectionDescription: TextView
    private lateinit var btnResolvePermissions: Button

    // Active Live Call Card
    private lateinit var cardActiveLiveCall: MaterialCardView
    private lateinit var tvActiveCallBannerTitle: TextView
    private lateinit var tvActiveCallLiveScore: TextView
    private lateinit var tvActiveCallNumber: TextView
    private lateinit var btnOpenActiveLiveCallUI: Button

    // Security Status Icons & Rows
    private lateinit var iconStatusCallProtection: TextView
    private lateinit var iconStatusFraudDetection: TextView
    private lateinit var iconStatusAlerts: TextView
    private lateinit var tvSubCallProtection: TextView
    private lateinit var tvSubFraudDetection: TextView
    private lateinit var tvSubAlerts: TextView
    private lateinit var rowCallProtection: LinearLayout
    private lateinit var rowFraudDetection: LinearLayout
    private lateinit var rowAlerts: LinearLayout

    // Recent Activity Views
    private lateinit var btnViewAllRecent: TextView
    private lateinit var containerRecentActivity: LinearLayout
    private lateinit var tvNoRecentActivity: TextView

    // Top & Bottom Navigation
    private lateinit var btnQuickSettings: ImageButton
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var historyRepo: CallHistoryRepository
    private val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

    // Activity Result Launcher for Runtime Permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        updateProtectionStatus()
    }

    // Activity Result Launcher for Call Screening Role Dialog
    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Call Screening Protection Activated!", Toast.LENGTH_SHORT).show()
        }
        updateProtectionStatus()
    }

    // Listener for Call State transitions
    private val callEventListener: (CallStateManager.CallEvent) -> Unit = { event ->
        runOnUiThread {
            handleCallStateChange(event)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        historyRepo = CallHistoryRepository(this)

        initViews()
        setupListeners()

        // Attach to CallStateManager
        CallStateManager.addListener(callEventListener)
        CallStateManager.startListening(this)
    }

    override fun onResume() {
        super.onResume()
        updateProtectionStatus()
        loadRecentActivity()
        checkActiveCallBanner()
        bottomNavigationView.selectedItemId = R.id.nav_home
    }

    override fun onDestroy() {
        super.onDestroy()
        CallStateManager.removeListener(callEventListener)
    }

    private fun initViews() {
        cardProtectionStatus = findViewById(R.id.cardProtectionStatus)
        imgStatusShield = findViewById(R.id.imgStatusShield)
        tvProtectionTitle = findViewById(R.id.tvProtectionTitle)
        tvProtectionDescription = findViewById(R.id.tvProtectionDescription)
        btnResolvePermissions = findViewById(R.id.btnResolvePermissions)

        cardActiveLiveCall = findViewById(R.id.cardActiveLiveCall)
        tvActiveCallBannerTitle = findViewById(R.id.tvActiveCallBannerTitle)
        tvActiveCallLiveScore = findViewById(R.id.tvActiveCallLiveScore)
        tvActiveCallNumber = findViewById(R.id.tvActiveCallNumber)
        btnOpenActiveLiveCallUI = findViewById(R.id.btnOpenActiveLiveCallUI)

        iconStatusCallProtection = findViewById(R.id.iconStatusCallProtection)
        iconStatusFraudDetection = findViewById(R.id.iconStatusFraudDetection)
        iconStatusAlerts = findViewById(R.id.iconStatusAlerts)
        tvSubCallProtection = findViewById(R.id.tvSubCallProtection)
        tvSubFraudDetection = findViewById(R.id.tvSubFraudDetection)
        tvSubAlerts = findViewById(R.id.tvSubAlerts)
        rowCallProtection = findViewById(R.id.rowCallProtection)
        rowFraudDetection = findViewById(R.id.rowFraudDetection)
        rowAlerts = findViewById(R.id.rowAlerts)

        btnViewAllRecent = findViewById(R.id.btnViewAllRecent)
        containerRecentActivity = findViewById(R.id.containerRecentActivity)
        tvNoRecentActivity = findViewById(R.id.tvNoRecentActivity)

        btnQuickSettings = findViewById(R.id.btnQuickSettings)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
    }

    private fun setupListeners() {
        // Quick Settings in header
        btnQuickSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Action button when permissions needed
        btnResolvePermissions.setOnClickListener {
            if (!PermissionHelper.isAllRequiredPermissionsGranted(this)) {
                permissionLauncher.launch(PermissionHelper.REQUIRED_PERMISSIONS)
            } else if (!PermissionHelper.isCallScreeningRoleHeld(this)) {
                val roleIntent = PermissionHelper.getCallScreeningRoleIntent(this)
                if (roleIntent != null) {
                    roleLauncher.launch(roleIntent)
                }
            }
        }

        // Tap on individual security rows
        rowCallProtection.setOnClickListener {
            if (!PermissionHelper.isCallScreeningRoleHeld(this)) {
                val roleIntent = PermissionHelper.getCallScreeningRoleIntent(this)
                if (roleIntent != null) {
                    roleLauncher.launch(roleIntent)
                } else {
                    Toast.makeText(this, "Call screening role requires Android 10+", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Call protection is active", Toast.LENGTH_SHORT).show()
            }
        }

        rowFraudDetection.setOnClickListener {
            if (!PermissionHelper.isMicrophoneGranted(this)) {
                permissionLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO))
            } else {
                Toast.makeText(this, "On-device fraud detection is ready", Toast.LENGTH_SHORT).show()
            }
        }

        rowAlerts.setOnClickListener {
            if (!PermissionHelper.isNotificationGranted(this)) {
                permissionLauncher.launch(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS))
            } else {
                Toast.makeText(this, "Real-time alert notifications are enabled", Toast.LENGTH_SHORT).show()
            }
        }

        // View All Recent opens History
        btnViewAllRecent.setOnClickListener {
            startActivity(Intent(this, CallHistoryActivity::class.java))
        }

        // Open Live Call Screen from active banner
        btnOpenActiveLiveCallUI.setOnClickListener {
            val num = CallStateManager.getLastIncomingNumber() ?: "Unknown caller"
            val liveIntent = Intent(this, LiveCallActivity::class.java).apply {
                putExtra("caller_number", num)
            }
            startActivity(liveIntent)
        }

        // Bottom Navigation Bar
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, CallHistoryActivity::class.java))
                    false
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    /**
     * Updates the main protection hero card and 3 security status indicators.
     */
    private fun updateProtectionStatus() {
        val phoneOk = PermissionHelper.isPhoneStateGranted(this)
        val micOk = PermissionHelper.isMicrophoneGranted(this)
        val notifOk = PermissionHelper.isNotificationGranted(this)
        val roleOk = PermissionHelper.isCallScreeningRoleHeld(this)

        val isFullyProtected = phoneOk && micOk && notifOk && roleOk

        if (isFullyProtected) {
            tvProtectionTitle.text = "PROTECTION ACTIVE"
            tvProtectionTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            tvProtectionDescription.text = "Automatic protection is enabled."
            cardProtectionStatus.strokeColor = ContextCompat.getColor(this, R.color.accent_green)
            btnResolvePermissions.visibility = View.GONE
        } else {
            tvProtectionTitle.text = "SETUP REQUIRED"
            tvProtectionTitle.setTextColor(ContextCompat.getColor(this, R.color.accent_amber))
            tvProtectionDescription.text = "Enable required permissions for complete protection."
            cardProtectionStatus.strokeColor = ContextCompat.getColor(this, R.color.accent_amber)
            btnResolvePermissions.visibility = View.VISIBLE
        }

        // Security Status Item 1: Call Protection
        if (roleOk) {
            iconStatusCallProtection.text = "✓"
            iconStatusCallProtection.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
            tvSubCallProtection.text = "Call screening active • Intercepts incoming callers"
        } else {
            iconStatusCallProtection.text = "⚠"
            iconStatusCallProtection.setTextColor(ContextCompat.getColor(this, R.color.accent_amber))
            tvSubCallProtection.text = "Tap to set Svara_X as default Call Screening app"
        }

        // Security Status Item 2: Fraud Detection
        if (micOk) {
            iconStatusFraudDetection.text = "✓"
            iconStatusFraudDetection.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
            tvSubFraudDetection.text = "On-device AI engine active • 14 scam categories"
        } else {
            iconStatusFraudDetection.text = "⚠"
            iconStatusFraudDetection.setTextColor(ContextCompat.getColor(this, R.color.accent_amber))
            tvSubFraudDetection.text = "Tap to grant audio capture permission"
        }

        // Security Status Item 3: Alerts
        if (notifOk) {
            iconStatusAlerts.text = "✓"
            iconStatusAlerts.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
            tvSubAlerts.text = "Heads-up warnings & sensory alert signals active"
        } else {
            iconStatusAlerts.text = "⚠"
            iconStatusAlerts.setTextColor(ContextCompat.getColor(this, R.color.accent_amber))
            tvSubAlerts.text = "Tap to grant notification warning permission"
        }
    }

    /**
     * Checks if there's currently an ongoing call and displays the active call card.
     */
    private fun checkActiveCallBanner() {
        val currentState = CallStateManager.getCurrentState()
        if (currentState == CallStateManager.State.RINGING || currentState == CallStateManager.State.OFFHOOK) {
            cardActiveLiveCall.visibility = View.VISIBLE
            val num = CallStateManager.getLastIncomingNumber() ?: "Incoming Call"
            tvActiveCallNumber.text = num
            if (currentState == CallStateManager.State.RINGING) {
                tvActiveCallBannerTitle.text = "📞 INCOMING CALL SCREENED"
                tvActiveCallBannerTitle.setTextColor(ContextCompat.getColor(this, R.color.accent_amber))
                tvActiveCallLiveScore.text = "Ringing..."
            } else {
                tvActiveCallBannerTitle.text = "🔴 LIVE CALL IN PROGRESS"
                tvActiveCallBannerTitle.setTextColor(ContextCompat.getColor(this, R.color.accent_red))
                val risk = CallMonitoringService.currentRiskResult
                tvActiveCallLiveScore.text = if (risk != null) "${risk.riskScore}% ${risk.riskLevel}" else "Analyzing..."
            }
        } else {
            cardActiveLiveCall.visibility = View.GONE
        }
    }

    /**
     * Reacts to real-time telephony state events.
     */
    private fun handleCallStateChange(event: CallStateManager.CallEvent) {
        when (event.state) {
            CallStateManager.State.RINGING -> {
                cardActiveLiveCall.visibility = View.VISIBLE
                tvActiveCallBannerTitle.text = "📞 INCOMING CALL SCREENED"
                tvActiveCallBannerTitle.setTextColor(ContextCompat.getColor(this, R.color.accent_amber))
                tvActiveCallNumber.text = event.incomingNumber ?: "Unknown Caller"
                tvActiveCallLiveScore.text = "Screening..."
            }
            CallStateManager.State.OFFHOOK -> {
                cardActiveLiveCall.visibility = View.VISIBLE
                tvActiveCallBannerTitle.text = "🔴 LIVE CALL IN PROGRESS"
                tvActiveCallBannerTitle.setTextColor(ContextCompat.getColor(this, R.color.accent_red))
                tvActiveCallNumber.text = event.incomingNumber ?: "Active Call"
                tvActiveCallLiveScore.text = "Analyzing..."
            }
            CallStateManager.State.IDLE, CallStateManager.State.DISCONNECTED -> {
                cardActiveLiveCall.visibility = View.GONE
                // Refresh recent activity list to show newly ended call
                loadRecentActivity()
            }
        }
    }

    /**
     * Renders recent calls directly from CallHistoryRepository.
     */
    private fun loadRecentActivity() {
        val records = historyRepo.getAllRecords()
        containerRecentActivity.removeAllViews()

        if (records.isEmpty()) {
            tvNoRecentActivity.visibility = View.VISIBLE
            containerRecentActivity.addView(tvNoRecentActivity)
            return
        }

        tvNoRecentActivity.visibility = View.GONE

        // Display up to 3 most recent calls
        val recentItems = records.take(3)
        for (record in recentItems) {
            val cardView = layoutInflater.inflate(R.layout.item_call_history, containerRecentActivity, false) as MaterialCardView

            val tvCaller = cardView.findViewById<TextView>(R.id.tvHistoryCaller)
            val tvDateDuration = cardView.findViewById<TextView>(R.id.tvHistoryDateDuration)
            val tvBadge = cardView.findViewById<TextView>(R.id.tvHistoryRiskScoreBadge)
            val tvIndicators = cardView.findViewById<TextView>(R.id.tvHistoryIndicators)
            val tvRecommendation = cardView.findViewById<TextView>(R.id.tvHistoryRecommendation)

            val demoSuffix = if (record.isDemoSimulation) " [DEMO]" else ""
            tvCaller.text = "${record.callerNumber}$demoSuffix"
            tvDateDuration.text = "${dateFormat.format(Date(record.timestamp))} • ${record.durationSeconds}s"

            val colorRes = when (record.riskLevel) {
                "CRITICAL" -> R.color.accent_red
                "HIGH" -> R.color.accent_red
                "MEDIUM" -> R.color.accent_amber
                else -> R.color.accent_green
            }
            val color = ContextCompat.getColor(this, colorRes)

            tvBadge.text = "${record.finalRiskScore}% ${record.riskLevel}"
            tvBadge.setTextColor(color)
            cardView.strokeColor = color

            if (record.detectedIndicators.isNotEmpty()) {
                tvIndicators.visibility = View.VISIBLE
                tvIndicators.text = "⚠ " + record.detectedIndicators.joinToString(", ")
                tvIndicators.setTextColor(color)
            } else {
                tvIndicators.visibility = View.VISIBLE
                tvIndicators.text = "✓ No suspicious indicators detected"
                tvIndicators.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
            }

            tvRecommendation.text = "Action: ${record.recommendation}"

            cardView.setOnClickListener {
                showCallDetailsDialog(record)
            }

            containerRecentActivity.addView(cardView)
        }
    }

    private fun showCallDetailsDialog(record: CallRecord) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_call_details)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvCaller = dialog.findViewById<TextView>(R.id.tvDetailCaller)
        val tvDemoTag = dialog.findViewById<TextView>(R.id.tvDetailDemoTag)
        val tvTimestampDuration = dialog.findViewById<TextView>(R.id.tvDetailTimestampDuration)
        val tvRiskBadge = dialog.findViewById<TextView>(R.id.tvDetailRiskBadge)
        val containerIndicators = dialog.findViewById<LinearLayout>(R.id.containerDetailIndicators)
        val tvNoIndicators = dialog.findViewById<TextView>(R.id.tvDetailNoIndicators)
        val tvRecommendation = dialog.findViewById<TextView>(R.id.tvDetailRecommendation)
        val tvSnippet = dialog.findViewById<TextView>(R.id.tvDetailSnippet)
        val lblSnippet = dialog.findViewById<TextView>(R.id.lblDetailSnippet)
        val btnDismiss = dialog.findViewById<Button>(R.id.btnDismissDetail)

        tvCaller.text = record.callerNumber
        if (record.isDemoSimulation) {
            tvDemoTag.visibility = View.VISIBLE
        } else {
            tvDemoTag.visibility = View.GONE
        }

        val formattedDate = dateFormat.format(Date(record.timestamp))
        tvTimestampDuration.text = "$formattedDate • ${record.durationSeconds}s duration"

        val colorRes = when (record.riskLevel) {
            "CRITICAL" -> R.color.accent_red
            "HIGH" -> R.color.accent_red
            "MEDIUM" -> R.color.accent_amber
            else -> R.color.accent_green
        }
        val color = ContextCompat.getColor(this, colorRes)

        tvRiskBadge.text = "${record.finalRiskScore}% ${record.riskLevel}"
        tvRiskBadge.setTextColor(color)

        // Populate Indicators
        containerIndicators.removeAllViews()
        if (record.detectedIndicators.isEmpty()) {
            tvNoIndicators.visibility = View.VISIBLE
        } else {
            tvNoIndicators.visibility = View.GONE
            for (ind in record.detectedIndicators) {
                val itemLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 4, 0, 4)
                }

                val bullet = TextView(this).apply {
                    text = "• "
                    textSize = 14f
                    setTextColor(color)
                }

                val text = TextView(this).apply {
                    this.text = ind
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                itemLayout.addView(bullet)
                itemLayout.addView(text)
                containerIndicators.addView(itemLayout)
            }
        }

        tvRecommendation.text = record.recommendation

        if (record.fullTranscriptSnippet.isNotBlank()) {
            lblSnippet.visibility = View.VISIBLE
            tvSnippet.visibility = View.VISIBLE
            tvSnippet.text = "\"${record.fullTranscriptSnippet}\""
        } else {
            lblSnippet.visibility = View.GONE
            tvSnippet.visibility = View.GONE
        }

        btnDismiss.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
