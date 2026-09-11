package com.svarax.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.svarax.R
import com.svarax.call.CallStateManager
import com.svarax.risk.RiskResult
import com.svarax.service.CallMonitoringService

/**
 * LiveCallActivity: Dedicated In-Call Fraud Warning Screen.
 *
 * Provides real-time risk score, acoustic signals, and fraud indicators.
 * If cellular audio capture is blocked by Android security restrictions,
 * accurately states "Live call audio unavailable on this device."
 */
class LiveCallActivity : AppCompatActivity() {

    private lateinit var tvCallerNumber: TextView
    private lateinit var tvRiskPercentage: TextView
    private lateinit var tvRiskLevelBadge: TextView
    private lateinit var pbRiskScore: ProgressBar
    private lateinit var cardRiskMeter: MaterialCardView
    private lateinit var containerIndicatorsList: LinearLayout
    private lateinit var tvNoIndicatorsPlaceholder: TextView
    private lateinit var tvLiveRecommendation: TextView
    private lateinit var tvLiveTranscript: TextView
    private lateinit var btnReturnToDashboard: Button
    private lateinit var pulseLiveDot: View
    private lateinit var imgPhoneIcon: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private var isActivityActive = false

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (isActivityActive) {
                updateLiveUI()
                handler.postDelayed(this, 800)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_call)

        initViews()
        startLivePulse()

        btnReturnToDashboard.setOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        tvCallerNumber = findViewById(R.id.tvLiveCallerNumber)
        tvRiskPercentage = findViewById(R.id.tvLiveRiskPercentage)
        tvRiskLevelBadge = findViewById(R.id.tvLiveRiskLevelBadge)
        pbRiskScore = findViewById(R.id.pbRiskScore)
        cardRiskMeter = findViewById(R.id.cardRiskMeter)
        containerIndicatorsList = findViewById(R.id.containerIndicatorsList)
        tvNoIndicatorsPlaceholder = findViewById(R.id.tvNoIndicatorsPlaceholder)
        tvLiveRecommendation = findViewById(R.id.tvLiveRecommendation)
        tvLiveTranscript = findViewById(R.id.tvLiveTranscript)
        btnReturnToDashboard = findViewById(R.id.btnReturnToDashboard)
        pulseLiveDot = findViewById(R.id.pulseLiveDot)
        imgPhoneIcon = findViewById(R.id.imgLivePhoneIcon)

        val caller = intent.getStringExtra("caller_number")
            ?: CallMonitoringService.activeCallerNumber.takeIf { it.isNotBlank() && it != "Unknown caller" }
            ?: CallStateManager.getLastIncomingNumber()
            ?: "Unknown caller"
        tvCallerNumber.text = caller
    }

    override fun onResume() {
        super.onResume()
        isActivityActive = true
        handler.post(pollRunnable)
    }

    override fun onPause() {
        super.onPause()
        isActivityActive = false
        handler.removeCallbacks(pollRunnable)
    }

    private fun startLivePulse() {
        ObjectAnimator.ofPropertyValuesHolder(
            pulseLiveDot,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 1.4f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 1.4f, 1f),
            PropertyValuesHolder.ofFloat("alpha", 1f, 0.4f, 1f)
        ).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun updateLiveUI() {
        val currentRisk = CallMonitoringService.currentRiskResult
        val transcript = CallMonitoringService.cumulativeTranscript
        val isLiveUnavailable = CallMonitoringService.isLiveAudioUnavailable
        val isDemo = CallMonitoringService.isDemoModeActive

        // Update caller number if service detected one
        if (CallMonitoringService.activeCallerNumber.isNotBlank() && CallMonitoringService.activeCallerNumber != "Unknown caller") {
            tvCallerNumber.text = CallMonitoringService.activeCallerNumber
        }

        if (isLiveUnavailable && !isDemo && transcript.isBlank()) {
            tvLiveTranscript.text = "Live call audio unavailable on this device."
            tvLiveRecommendation.text = "Android OS isolates cellular downlink audio for privacy. Svara_X will not fabricate fake live transcripts."
            tvRiskPercentage.text = "--"
            tvRiskLevelBadge.text = "UNVERIFIED"
            val mutedColor = ContextCompat.getColor(this, R.color.text_secondary)
            tvRiskPercentage.setTextColor(mutedColor)
            tvRiskLevelBadge.setTextColor(mutedColor)
            cardRiskMeter.strokeColor = mutedColor
            pbRiskScore.progress = 0
            containerIndicatorsList.removeAllViews()
            tvNoIndicatorsPlaceholder.visibility = View.VISIBLE
            tvNoIndicatorsPlaceholder.text = "Live call audio capture is blocked by Android cellular isolation."
            containerIndicatorsList.addView(tvNoIndicatorsPlaceholder)
        } else {
            if (transcript.isNotBlank()) {
                tvLiveTranscript.text = transcript
            } else {
                tvLiveTranscript.text = if (isDemo) "Initializing simulated scam dialogue..." else "Listening for speech audio..."
            }

            if (currentRisk != null) {
                renderRiskState(currentRisk)
            } else {
                tvRiskPercentage.text = "5%"
                tvRiskLevelBadge.text = "LOW RISK"
                val greenColor = ContextCompat.getColor(this, R.color.accent_green)
                tvRiskPercentage.setTextColor(greenColor)
                tvRiskLevelBadge.setTextColor(greenColor)
                cardRiskMeter.strokeColor = greenColor
                pbRiskScore.progress = 5
            }
        }
    }

    private fun renderRiskState(risk: RiskResult) {
        tvRiskPercentage.text = "${risk.riskScore}%"
        pbRiskScore.progress = risk.riskScore
        tvLiveRecommendation.text = risk.recommendation

        val (accentColorRes, badgeBgColorRes) = when (risk.riskLevel) {
            "CRITICAL" -> Pair(R.color.accent_red, R.color.surface_dark)
            "HIGH" -> Pair(R.color.accent_red, R.color.surface_dark)
            "MEDIUM" -> Pair(R.color.accent_amber, R.color.surface_dark)
            else -> Pair(R.color.accent_green, R.color.surface_dark)
        }

        val color = ContextCompat.getColor(this, accentColorRes)
        tvRiskPercentage.setTextColor(color)
        tvRiskLevelBadge.text = "${risk.riskLevel} RISK"
        tvRiskLevelBadge.setTextColor(color)
        pbRiskScore.progressTintList = ColorStateList.valueOf(color)
        cardRiskMeter.strokeColor = color

        // Populate Indicators list
        if (risk.indicators.isEmpty()) {
            tvNoIndicatorsPlaceholder.visibility = View.VISIBLE
            tvNoIndicatorsPlaceholder.text = "No suspicious patterns detected yet"
            containerIndicatorsList.removeAllViews()
            containerIndicatorsList.addView(tvNoIndicatorsPlaceholder)
        } else {
            tvNoIndicatorsPlaceholder.visibility = View.GONE
            containerIndicatorsList.removeAllViews()
            for (ind in risk.indicators) {
                val itemLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 6, 0, 6)
                }

                val icon = TextView(this).apply {
                    text = "⚠ "
                    textSize = 14f
                    setTextColor(color)
                }

                val text = TextView(this).apply {
                    this.text = ind
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(this@LiveCallActivity, R.color.text_primary))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                itemLayout.addView(icon)
                itemLayout.addView(text)
                containerIndicatorsList.addView(itemLayout)
            }
        }
    }
}
