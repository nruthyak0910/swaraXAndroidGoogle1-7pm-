package com.svarax.call

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.core.content.ContextCompat
import com.svarax.service.CallMonitoringService

/**
 * Priority 4: Native Android CallScreeningService.
 * Intercepts incoming cellular calls as soon as the telecom subsystem receives them.
 * Does not block or reject the call by default; extracts the caller ID and informs CallStateManager.
 */
class SvaraCallScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "SvaraX_ScreeningService"
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: "Unknown Caller"
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            true
        }

        Log.i(TAG, "Incoming call screened: number=$phoneNumber, isIncoming=$isIncoming")

        // 1. Notify CallStateManager of new incoming call
        CallStateManager.updateState(CallStateManager.State.RINGING, phoneNumber)

        // 2. Start foreground monitoring service to maintain background analysis lifecycle
        try {
            val serviceIntent = Intent(this, CallMonitoringService::class.java).apply {
                putExtra(CallMonitoringService.EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(CallMonitoringService.EXTRA_STATE, "RINGING")
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start CallMonitoringService from screening service", e)
        }

        // 3. Immediately respond to telecom to allow the call through (no unwanted latency or false blocking)
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)
    }
}
