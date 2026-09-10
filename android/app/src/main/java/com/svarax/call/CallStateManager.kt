package com.svarax.call

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Priority 4: Central Call State Manager.
 * Tracks telephony state transitions (RINGING -> OFFHOOK -> IDLE) and notifies registered listeners.
 */
object CallStateManager {

    private const val TAG = "SvaraX_CallStateManager"

    enum class State {
        IDLE,
        RINGING,
        OFFHOOK, // Active conversation
        DISCONNECTED
    }

    data class CallEvent(
        val state: State,
        val incomingNumber: String?,
        val timestamp: Long = System.currentTimeMillis()
    )

    private var currentState: State = State.IDLE
    private var lastIncomingNumber: String? = null
    private val listeners = CopyOnWriteArrayList<(CallEvent) -> Unit>()
    private var isListening = false

    fun getCurrentState(): State = currentState
    fun getLastIncomingNumber(): String? = lastIncomingNumber

    fun addListener(listener: (CallEvent) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (CallEvent) -> Unit) {
        listeners.remove(listener)
    }

    /**
     * Dispatch an updated call state to all subscribers (UI, Service, Logs).
     */
    fun updateState(newState: State, incomingNumber: String? = null) {
        currentState = newState
        if (!incomingNumber.isNullOrBlank()) {
            lastIncomingNumber = incomingNumber
        }
        val event = CallEvent(newState, lastIncomingNumber)
        Log.i(TAG, "Call state transitioned to $newState for number: $lastIncomingNumber")
        for (listener in listeners) {
            try {
                listener(event)
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking call state listener", e)
            }
        }
    }

    /**
     * Registers TelephonyManager listener for phone state tracking.
     */
    fun startListening(context: Context) {
        if (isListening) return
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyManager.registerTelephonyCallback(
                    context.mainExecutor,
                    object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                        override fun onCallStateChanged(state: Int) {
                            handleTelephonyState(state)
                        }
                    }
                )
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.listen(object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleTelephonyState(state, phoneNumber)
                    }
                }, PhoneStateListener.LISTEN_CALL_STATE)
            }
            isListening = true
            Log.i(TAG, "TelephonyManager call state listener registered successfully.")
        } catch (e: SecurityException) {
            Log.w(TAG, "READ_PHONE_STATE permission missing when attaching Telephony listener", e)
        }
    }

    private fun handleTelephonyState(state: Int, phoneNumber: String? = null) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                updateState(State.RINGING, phoneNumber)
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                updateState(State.OFFHOOK, phoneNumber)
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                updateState(State.IDLE, phoneNumber)
            }
        }
    }
}
