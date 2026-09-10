package com.svarax.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Priority 4: Telephony State BroadcastReceiver.
 * Acts as a fallback observer for standard PHONE_STATE broadcasts.
 */
class CallStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SvaraX_CallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            Log.d(TAG, "PHONE_STATE broadcast received: state=$stateStr, number=$incomingNumber")

            when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    CallStateManager.updateState(CallStateManager.State.RINGING, incomingNumber)
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    CallStateManager.updateState(CallStateManager.State.OFFHOOK, incomingNumber)
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    CallStateManager.updateState(CallStateManager.State.IDLE, incomingNumber)
                }
            }
        }
    }
}
